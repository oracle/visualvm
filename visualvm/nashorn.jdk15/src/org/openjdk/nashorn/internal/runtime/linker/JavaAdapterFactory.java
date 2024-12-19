/*
 * Copyright (c) 2010, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.openjdk.nashorn.internal.runtime.linker;

import static org.openjdk.nashorn.internal.lookup.Lookup.MH;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Modifier;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.Permissions;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import jdk.dynalink.CallSiteDescriptor;
import jdk.dynalink.StandardOperation;
import jdk.dynalink.beans.StaticClass;
import jdk.dynalink.linker.support.SimpleLinkRequest;
import org.openjdk.nashorn.internal.runtime.Context;
import org.openjdk.nashorn.internal.runtime.ECMAErrors;
import org.openjdk.nashorn.internal.runtime.ECMAException;
import org.openjdk.nashorn.internal.runtime.ScriptFunction;
import org.openjdk.nashorn.internal.runtime.ScriptObject;

/**
 * A factory class that generates adapter classes. Adapter classes allow
 * implementation of Java interfaces and extending of Java classes from
 * JavaScript. For every combination of a superclass to extend and interfaces to
 * implement (collectively: "original types"), exactly one adapter class is
 * generated that extends the specified superclass and implements the specified
 * interfaces. (But see the discussion of class-based overrides for exceptions.)
 * <p>
 * The adapter class is generated in a new secure class loader that inherits
 * Nashorn's protection domain, and has either one of the original types' class
 * loader or the Nashorn's class loader as its parent - the parent class loader
 * is chosen so that all the original types and the Nashorn core classes are
 * visible from it (as the adapter will have constant pool references to
 * ScriptObject and ScriptFunction classes). In case none of the candidate class
 * loaders has visibility of all the required types, an error is thrown. The
 * class uses {@link JavaAdapterBytecodeGenerator} to generate the adapter class
 * itself; see its documentation for details about the generated class.
 * <p>
 * You normally don't use this class directly, but rather either create adapters
 * from script using {@link org.openjdk.nashorn.internal.objects.NativeJava#extend(Object, Object...)},
 * using the {@code new} operator on abstract classes and interfaces (see
 * {@link org.openjdk.nashorn.internal.objects.NativeJava#type(Object, Object)}), or
 * implicitly when passing script functions to Java methods expecting SAM types.
 */

public final class JavaAdapterFactory {
    private static final ProtectionDomain MINIMAL_PERMISSION_DOMAIN = createMinimalPermissionDomain();

    // context with permissions needs for AdapterInfo creation
    private static final AccessControlContext CREATE_ADAPTER_INFO_ACC_CTXT =
        ClassAndLoader.createPermAccCtxt("createClassLoader", "getClassLoader",
            "accessDeclaredMembers", "accessClassInPackage.org.openjdk.nashorn.internal.runtime");

    /**
     * A mapping from an original Class object to AdapterInfo representing the adapter for the class it represents.
     */
    private static final ClassValue<Map<List<Class<?>>, AdapterInfo>> ADAPTER_INFO_MAPS = new ClassValue<>() {
        @Override
        protected Map<List<Class<?>>, AdapterInfo> computeValue(final Class<?> type) {
            return new ConcurrentHashMap<>();
        }
    };

    private static final ClassValue<Boolean> AUTO_CONVERTIBLE_FROM_FUNCTION = new ClassValue<>() {
        @Override
        protected Boolean computeValue(final Class<?> type) {
            try {
                return getAdapterInfo(new Class<?>[] { type }).autoConvertibleFromFunction;
            } catch (Exception e) {
                return false;
            }
        }
    };

    /**
     * Returns an adapter class for the specified original types. The adapter
     * class extends/implements the original class/interfaces.
     *
     * @param types the original types. The caller must pass at least one Java
     *        type representing either a public interface or a non-final public
     *        class with at least one public or protected constructor. If more
     *        than one type is specified, at most one can be a class and the
     *        rest have to be interfaces. The class can be in any position in
     *        the array. Invoking the method twice with exactly the same types
     *        in the same order will return the same adapter class, any
     *        reordering of types or even addition or removal of redundant types
     *        (i.e., interfaces that other types in the list already
     *        implement/extend, or {@code java.lang.Object} in a list of types
     *        consisting purely of interfaces) will result in a different
     *        adapter class, even though those adapter classes are functionally
     *        identical; we deliberately don't want to incur the additional
     *        processing cost of canonicalizing type lists.
     * @param classOverrides a JavaScript object with functions serving as the
     *        class-level overrides and implementations. These overrides are
     *        defined for all instances of the class, and can be further
     *        overridden on a per-instance basis by passing additional objects
     *        in the constructor.
     * @param lookup the lookup object identifying the caller class. The
     *        generated adapter class will have the protection domain of the
     *        caller class iff the lookup object is full-strength, otherwise it
     *        will be completely unprivileged.
     *
     * @return an adapter class. See this class' documentation for details on
     *         the generated adapter class.
     *
     * @throws ECMAException with a TypeError if the adapter class can not be
     *         generated because the original class is final, non-public, or has
     *         no public or protected constructors.
     */
    public static StaticClass getAdapterClassFor(final Class<?>[] types, final ScriptObject classOverrides, final MethodHandles.Lookup lookup) {
        return getAdapterClassFor(types, classOverrides, getProtectionDomain(lookup));
    }

    private static StaticClass getAdapterClassFor(final Class<?>[] types, final ScriptObject classOverrides, final ProtectionDomain protectionDomain) {
        assert types != null && types.length > 0;
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            for (final Class<?> type : types) {
                // check for restricted package access
                Context.checkPackageAccess(type);
                // check for classes, interfaces in reflection
                ReflectionCheckLinker.checkReflectionAccess(type, true);
            }
        }
        return getAdapterInfo(types).getAdapterClass(classOverrides, protectionDomain);
    }

    private static ProtectionDomain getProtectionDomain(final MethodHandles.Lookup lookup) {
        if((lookup.lookupModes() & Lookup.PRIVATE) == 0) {
            return MINIMAL_PERMISSION_DOMAIN;
        }
        return getProtectionDomain(lookup.lookupClass());
    }

    private static ProtectionDomain getProtectionDomain(final Class<?> clazz) {
        return AccessController.doPrivileged((PrivilegedAction<ProtectionDomain>) clazz::getProtectionDomain);
    }

    /**
     * Returns a method handle representing a constructor that takes a single
     * argument of the source type (which, really, should be one of {@link ScriptObject},
     * {@link ScriptFunction}, or {@link Object}, and returns an instance of the
     * adapter for the target type. Used to implement the function autoconverters
     * as well as the Nashorn JSR-223 script engine's {@code getInterface()}
     * method.
     *
     * @param sourceType the source type; should be either {@link ScriptObject},
     *        {@link ScriptFunction}, or {@link Object}. In case of {@code Object},
     *        it will return a method handle that dispatches to either the script
     *        object or function constructor at invocation based on the actual
     *        argument.
     * @param targetType the target type, for which adapter instances will be created
     * @param lookup method handle lookup to use
     *
     * @return the constructor method handle.
     *
     * @throws Exception if anything goes wrong
     */
    public static MethodHandle getConstructor(final Class<?> sourceType, final Class<?> targetType, final MethodHandles.Lookup lookup) throws Exception {
        final StaticClass adapterClass = getAdapterClassFor(new Class<?>[] { targetType }, null, lookup);
        return MH.bindTo(Bootstrap.getLinkerServices().getGuardedInvocation(new SimpleLinkRequest(
                new CallSiteDescriptor(lookup, StandardOperation.NEW,
                        MethodType.methodType(targetType, StaticClass.class, sourceType)), false,
                        adapterClass, null)).getInvocation(), adapterClass);
    }

    /**
     * Returns whether an instance of the specified class/interface can be
     * generated from a ScriptFunction. Returns {@code true} iff: the adapter
     * for the class/interface can be created, it is abstract (this includes
     * interfaces), it has at least one abstract method, all the abstract
     * methods share the same name, and it has a public or protected default
     * constructor. Note that invoking this class will most likely result in the
     * adapter class being defined in the JVM if it hasn't been already.
     *
     * @param clazz the inspected class
     *
     * @return {@code true} iff an instance of the specified class/interface can
     *         be generated from a ScriptFunction.
     */
    static boolean isAutoConvertibleFromFunction(final Class<?> clazz) {
        return AUTO_CONVERTIBLE_FROM_FUNCTION.get(clazz);
    }

    private static AdapterInfo getAdapterInfo(final Class<?>[] types) {
        final ClassAndLoader definingClassAndLoader = ClassAndLoader.getDefiningClassAndLoader(types);

        final Map<List<Class<?>>, AdapterInfo> adapterInfoMap = ADAPTER_INFO_MAPS.get(definingClassAndLoader.getRepresentativeClass());
        return adapterInfoMap.computeIfAbsent(List.of(types), t -> createAdapterInfo(t, definingClassAndLoader));
    }

   /**
     * For a given class, create its adapter class and associated info.
     *
     * @param types the class and interfaces for which the adapter is created
     *
     * @return the adapter info for the class.
     */
    private static AdapterInfo createAdapterInfo(final List<Class<?>> types, final ClassAndLoader definingClassAndLoader) {
        Class<?> superClass = null;
        final List<Class<?>> interfaces = new ArrayList<>(types.size());
        final Set<Class<?>> interfacesDedup = new HashSet<>(Math.max((int) (types.size()/.75f) + 1, 16));
        for(final Class<?> t: types) {
            final int mod = t.getModifiers();
            if(!t.isInterface()) {
                if (superClass == t) {
                    throw adaptationException(ErrorOutcome.DUPLICATE_TYPE, t.getCanonicalName());
                } else if(superClass != null) {
                    throw adaptationException(ErrorOutcome.MULTIPLE_SUPERCLASSES, t.getCanonicalName() + " and " + superClass.getCanonicalName());
                } else if (Modifier.isFinal(mod)) {
                    throw adaptationException(ErrorOutcome.FINAL_CLASS, t.getCanonicalName());
                }
                superClass = t;
            } else {
                if (interfaces.size() > 65535) {
                    throw adaptationException(ErrorOutcome.TOO_MANY_INTERFACES, "65535");
                } else if (!interfacesDedup.add(t)) {
                    throw adaptationException(ErrorOutcome.DUPLICATE_TYPE, t.getCanonicalName());
                }
                interfaces.add(t);
            }

            if(!Modifier.isPublic(mod)) {
                throw adaptationException(ErrorOutcome.NON_PUBLIC_CLASS, t.getCanonicalName());
            }
        }

        final Class<?> effectiveSuperClass = superClass == null ? Object.class : superClass;
        return AccessController.doPrivileged((PrivilegedAction<AdapterInfo>) () ->
            new AdapterInfo(effectiveSuperClass, interfaces, definingClassAndLoader),
            CREATE_ADAPTER_INFO_ACC_CTXT);
    }

    static ECMAException adaptationException(ErrorOutcome outcome, String... messageArgs) {
        return ECMAErrors.typeError("extend." + outcome, messageArgs);
    }

    /**
     * Contains various error outcomes for attempting to generate an adapter class.
     */
    enum ErrorOutcome {
        FINAL_CLASS,
        NON_PUBLIC_CLASS,
        NO_ACCESSIBLE_CONSTRUCTOR,
        MULTIPLE_SUPERCLASSES,
        DUPLICATE_TYPE,
        TOO_MANY_INTERFACES,
        NO_COMMON_LOADER,
        FINAL_FINALIZER
    }

    private static class AdapterInfo {
        private static final ClassAndLoader SCRIPT_OBJECT_LOADER = new ClassAndLoader(ScriptFunction.class, true);
        private static final VarHandle INSTANCE_ADAPTERS;
        static {
            try {
                INSTANCE_ADAPTERS = MethodHandles.lookup().findVarHandle(AdapterInfo.class, "instanceAdapters", Map.class);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }

        private final ClassLoader commonLoader;
        // TODO: soft reference the JavaAdapterClassLoader objects. They can be recreated when needed.
        private final JavaAdapterClassLoader classAdapterGenerator;
        private final JavaAdapterClassLoader instanceAdapterGenerator;
        private Map<CodeSource, StaticClass> instanceAdapters;
        final boolean autoConvertibleFromFunction;

        AdapterInfo(final Class<?> superClass, final List<Class<?>> interfaces, final ClassAndLoader definingLoader) {
            this.commonLoader = findCommonLoader(definingLoader);
            final JavaAdapterBytecodeGenerator gen = new JavaAdapterBytecodeGenerator(superClass, interfaces, commonLoader, false);
            this.autoConvertibleFromFunction = gen.isAutoConvertibleFromFunction();
            instanceAdapterGenerator = gen.createAdapterClassLoader();
            this.classAdapterGenerator = new JavaAdapterBytecodeGenerator(superClass, interfaces, commonLoader, true).createAdapterClassLoader();
        }

        StaticClass getAdapterClass(final ScriptObject classOverrides, final ProtectionDomain protectionDomain) {
            return classOverrides == null ? getInstanceAdapterClass(protectionDomain) :
                getClassAdapterClass(classOverrides, protectionDomain);
        }

        private StaticClass getInstanceAdapterClass(final ProtectionDomain protectionDomain) {
            CodeSource codeSource = protectionDomain.getCodeSource();
            if(codeSource == null) {
                codeSource = MINIMAL_PERMISSION_DOMAIN.getCodeSource();
            }
            var ia = instanceAdapters;
            if (ia == null) {
                var nia = new ConcurrentHashMap<CodeSource, StaticClass>();
                @SuppressWarnings("unchecked")
                var xia = (Map<CodeSource, StaticClass>)INSTANCE_ADAPTERS.compareAndExchange(this, null, nia);
                ia = xia == null ? nia : xia;
            }
            return ia.computeIfAbsent(codeSource, cs -> {
                // Any "unknown source" code source will default to no permission domain.
                final ProtectionDomain effectiveDomain =
                    cs.equals(MINIMAL_PERMISSION_DOMAIN.getCodeSource())
                    ? MINIMAL_PERMISSION_DOMAIN : protectionDomain;

                return instanceAdapterGenerator.generateClass(commonLoader, effectiveDomain);
            });
        }

        private StaticClass getClassAdapterClass(final ScriptObject classOverrides, final ProtectionDomain protectionDomain) {
            JavaAdapterServices.setClassOverrides(classOverrides);
            try {
                return classAdapterGenerator.generateClass(commonLoader, protectionDomain);
            } finally {
                JavaAdapterServices.setClassOverrides(null);
            }
        }

        /**
         * Choose between the passed class loader and the class loader that defines the
         * ScriptObject class, based on which of the two can see the classes in both.
         *
         * @param classAndLoader the loader and a representative class from it that will
         *        be used to add the generated adapter to its ADAPTER_INFO_MAPS.
         *
         * @return the class loader that sees both the specified class and Nashorn classes.
         */
        private static ClassLoader findCommonLoader(final ClassAndLoader classAndLoader) {
            if(classAndLoader.canSee(SCRIPT_OBJECT_LOADER)) {
                return classAndLoader.getLoader();
            }
            if (SCRIPT_OBJECT_LOADER.canSee(classAndLoader)) {
                return SCRIPT_OBJECT_LOADER.getLoader();
            }
            // try context class loader
            if (checkContextCL(classAndLoader) && checkContextCL(SCRIPT_OBJECT_LOADER)) {
                return Thread.currentThread().getContextClassLoader();
            }
            throw adaptationException(ErrorOutcome.NO_COMMON_LOADER, classAndLoader.getRepresentativeClass().getCanonicalName());
        }

        private static boolean checkContextCL(final ClassAndLoader classAndLoader) {
            try {
                ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
                final Class<?> otherClass = classAndLoader.getRepresentativeClass();
                return Class.forName(otherClass.getName(), false, contextClassLoader) == otherClass;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }
    }

    private static ProtectionDomain createMinimalPermissionDomain() {
        // Generated classes need to have at least the permission to access Nashorn runtime and runtime.linker packages.
        final Permissions permissions = new Permissions();
        permissions.add(new RuntimePermission("accessClassInPackage.org.openjdk.nashorn.internal.runtime"));
        permissions.add(new RuntimePermission("accessClassInPackage.org.openjdk.nashorn.internal.runtime.linker"));
        return new ProtectionDomain(new CodeSource(null, (CodeSigner[])null), permissions);
    }
}
