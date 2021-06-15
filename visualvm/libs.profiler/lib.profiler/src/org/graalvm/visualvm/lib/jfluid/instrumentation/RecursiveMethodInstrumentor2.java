/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.jfluid.instrumentation;

import java.util.ArrayList;
import org.graalvm.visualvm.lib.jfluid.ProfilerEngineSettings;
import org.graalvm.visualvm.lib.jfluid.classfile.BaseClassInfo;
import org.graalvm.visualvm.lib.jfluid.classfile.DynamicClassInfo;
import org.graalvm.visualvm.lib.jfluid.classfile.PlaceholderClassInfo;
import org.graalvm.visualvm.lib.jfluid.global.ProfilingSessionStatus;
import org.graalvm.visualvm.lib.jfluid.instrumentation.RecursiveMethodInstrumentor.ReachableMethodPlaceholder;
import org.graalvm.visualvm.lib.jfluid.utils.Wildcards;
import org.graalvm.visualvm.lib.jfluid.wireprotocol.RootClassLoadedCommand;


/**
 * Recursive method scanner that implements the eager instrumentation scheme ("Scheme A" in the JFluid paper).
 *
 * @author Misha Dmitriev
 * @author Adrian Mos
 */
public class RecursiveMethodInstrumentor2 extends RecursiveMethodInstrumentor {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    /** A placeholder for a class that contains reachable method and in which transferData() is specialized for this instrumentation scheme */
    protected class ReachableMethodPlaceholder2 extends ReachableMethodPlaceholder {
        //~ Constructors ---------------------------------------------------------------------------------------------------------

        ReachableMethodPlaceholder2(String refClassName, int classLoaderId) {
            super(refClassName, classLoaderId);
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void transferDataIntoRealClass(DynamicClassInfo clazz) {
            super.transferDataIntoRealClass(clazz);

            int len = methodNamesAndSigs.size();

            for (int i = 0; i < len; i += 2) {
                checkAndScanMethod(clazz, (String) methodNamesAndSigs.get(i), (String) methodNamesAndSigs.get(i + 1), false,
                                   false, false);
            }
        }
    }

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public RecursiveMethodInstrumentor2(ProfilingSessionStatus status, ProfilerEngineSettings settings) {
        super(status, settings);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    Object[] getInitialMethodsToInstrument(RootClassLoadedCommand rootLoaded, RootMethods roots) {
        DynamicClassInfo[] loadedClassInfos = preGetInitialMethodsToInstrument(rootLoaded);

        rootMethods = roots;

        // Check which root classes have already been loaded, and mark their root methods accordingly
        for (int j = 0; j < loadedClassInfos.length; j++) {
            if (loadedClassInfos[j] == null) {
                continue; // Can this happen?
            }

            markProfilingPonitForInstrumentation(loadedClassInfos[j]);
            tryInstrumentSpawnedThreads(loadedClassInfos[j]); // This only checks for Runnable.run()

            for (int rIdx = 0; rIdx < rootMethods.classNames.length; rIdx++) {
                String rootClassName = rootMethods.classNames[rIdx];
                boolean isMatch = false;

                if (rootMethods.classesWildcard[rIdx]) {
                    if (Wildcards.matchesWildcard(rootClassName, loadedClassInfos[j].getName())) {
                        //            System.out.println("(Instrumentor 2) Matched package wildcard - " + rootClassName);
                        isMatch = true;
                    }
                } else {
                    if (loadedClassInfos[j].getName().equals(rootClassName)) { // precise match
                        isMatch = true;
                    }
                }

                if (isMatch) { // This root class is loaded

                    if (Wildcards.isPackageWildcard(rootClassName) || Wildcards.isMethodWildcard(rootMethods.methodNames[rIdx])) {
                        if (rootMethods.markerMethods[rIdx]) {
                            markAllMethodsMarker(loadedClassInfos[j]);
                        } else {
                            markAllMethodsRoot(loadedClassInfos[j]);
                        }

                        String[] methodNames = loadedClassInfos[j].getMethodNames();
                        String[] signatures = loadedClassInfos[j].getMethodSignatures();

                        for (int methodIdx = 0; methodIdx < methodNames.length; methodIdx++) {
                            checkAndScanMethod(loadedClassInfos[j], methodNames[methodIdx], signatures[methodIdx], false, false,
                                               false);
                        }
                    } else {
                        markMethod(loadedClassInfos[j], rIdx);
                        checkAndScanMethod(loadedClassInfos[j], rootMethods.methodNames[rIdx],
                                           rootMethods.methodSignatures[rIdx], false, false, false);
                    }
                }
            }
        }

        // So that class loading is measured correctly from the beginning
        checkAndScanMethod(javaClassForName("java/lang/ClassLoader", 0), "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;", // NOI18N
                           true, false, true);

        return createInstrumentedMethodPack();
    }

    public Object[] getMethodsToInstrumentUponClassLoad(String classNameDot, int classLoaderId, boolean threadInCallGraph) {
        //System.out.println("*** MS2: instr. upon CL: " + className);
        String className = classNameDot.replace('.', '/').intern(); // NOI18N

        initInstrMethodData();
        markProfilingPointForInstrumentation(classNameDot,className,classLoaderId);
        // If a class doesn't pass the current instrumentation filter, we can't immediately reject it, since there is a chance
        // it contains some root methods. So we have to check that first.
        boolean isRootClass = false;

        for (int rIdx = 0; rIdx < rootMethods.classNames.length; rIdx++) {
            String rootClassName = rootMethods.classNames[rIdx];

            if (rootMethods.classesWildcard[rIdx]) {
                if (Wildcards.matchesWildcard(rootClassName, className)) {
                    //          System.out.println("(Instrumentor 2) Matched package wildcard - " + rootClassName);
                    isRootClass = true;

                    break;
                }
            } else {
                if (className.equals(rootClassName)) {
                    isRootClass = true;

                    break;
                }
            }
        }

        boolean normallyFilteredOut = !instrFilter.passes(className);

        if (!isRootClass) {
            if (normallyFilteredOut) {
                return createInstrumentedMethodPack();  // profile points!
            }
        }

        DynamicClassInfo clazz = javaClassForName(className, classLoaderId);

        if (clazz == null) {
            return null; // Warning already issued
        }

        boolean instrumentClinit = threadInCallGraph;

        if (!clazz.isLoaded()) {
            clazz.setLoaded(true);
            addToSubclassList(clazz, normallyFilteredOut ? null : clazz); // This call may cause scanning of methods of this class, so all initialization should be done before

            // Check to see if this class has been marked as root by the user:
            for (int rIdx = 0; rIdx < rootMethods.classNames.length; rIdx++) {
                String rootClassName = rootMethods.classNames[rIdx];
                boolean isMatch = false;

                if (rootMethods.classesWildcard[rIdx]) {
                    if (Wildcards.matchesWildcard(rootClassName, className)) {
                        //            System.out.println("(Instrumentor 2) Matched package wildcard - " + rootClassName);
                        isMatch = true;
                    }
                } else {
                    if (className.equals(rootClassName)) { // precise match
                        isMatch = true;
                    }
                }

                if (isMatch) { //it is indeed a root class

                    if (Wildcards.isPackageWildcard(rootClassName) || Wildcards.isMethodWildcard(rootMethods.methodNames[rIdx])) {
                        if (rootMethods.markerMethods[rIdx]) {
                            markAllMethodsMarker(clazz);
                        } else {
                            markAllMethodsRoot(clazz);
                        }

                        String[] methodNames = clazz.getMethodNames();
                        String[] signatures = clazz.getMethodSignatures();

                        for (int methodIdx = 0; methodIdx < methodNames.length; methodIdx++) {
                            checkAndScanMethod(clazz, methodNames[methodIdx], signatures[methodIdx], false, false, false);
                        }
                    } else {
                        markMethod(clazz, rIdx);
                        checkAndScanMethod(clazz, rootMethods.methodNames[rIdx], rootMethods.methodSignatures[rIdx], false,
                                           false, false);
                    }
                }
            }

            if (!clazz.isInterface()) {
                String[] methodNames = clazz.getMethodNames();
                String[] methodSignatures = clazz.getMethodSignatures();

                for (int i = 0; i < methodNames.length; i++) {
                    if (clazz.isMethodReachable(i)
                            || (!normallyFilteredOut && instrumentClinit && (methodNames[i] == "<clinit>"))) { // NOI18N
                        checkAndScanMethod(clazz, methodNames[i], methodSignatures[i], false, false, false);
                    }
                }
            }

            tryInstrumentSpawnedThreads(clazz);

            return createInstrumentedMethodPack();
        } else {
            return null;
        }
    }

    public Object[] getMethodsToInstrumentUponMethodInvocation(String className, int classLoaderId, String methodName,
                                                               String methodSignature) {
        return null; // This method is just not used with this flavour of MethodScanner
    }

    public Object[] getMethodsToInstrumentUponReflectInvoke(String className, int classLoaderId, String methodName,
                                                            String methodSignature) {
        //System.out.println("*** MS2: instr. upon reflect MI: " + className + "." + methodName + methodSignature);
        className = className.replace('.', '/').intern(); // NOI18N

        DynamicClassInfo clazz = javaClassForName(className, classLoaderId);

        if (clazz == null) {
            return null;
        }

        initInstrMethodData();

        methodName = methodName.intern();
        methodSignature = methodSignature.intern();

        checkAndScanMethod(clazz, methodName, methodSignature, false, false, false);

        return createInstrumentedMethodPack();
    }

    protected void findAndMarkOverridingMethodsReachable(DynamicClassInfo superClass, DynamicClassInfo subClass) {
        String[] methodNames = superClass.getMethodNames();
        String[] methodSignatures = superClass.getMethodSignatures();

        for (int i = 0; i < methodNames.length; i++) {
            if (!(superClass.isMethodVirtual(i) && superClass.isMethodReachable(i))) {
                continue;
            }

            // int idx = subClass.overridesVirtualMethod(superClass, i); - I once tried this, but with no visible effect. Strict check
            // for whether a method with the same name and signature in subclass really overrrides a method in superclass, given all other
            // conditions that we have already checked (e.g. that the method in superclass is not private), will only detect a pathological
            // case when both method versions are package-private. This is rare, if ever happens at all.
            checkAndScanMethod(subClass, methodNames[i], methodSignatures[i], true, false, false);
        }
    }

    protected void processInvoke(DynamicClassInfo clazz, boolean virtualCall, int index) {
        byte[] savedCodeBytes = codeBytes;
        int savedOffset = offset;

        String[] cms = clazz.getRefMethodsClassNameAndSig(index);

        if (cms == null) {
            return; // That's how coming across our own stuff currently manifests itself, e.g. when scanning instrumented Method.invoke()
        }

        String refClassName = cms[0];

        if (refClassName.startsWith(PROFILER_SERVER_SLASHED_CLASS_PREFIX)) {
            return; // We may come across our own stuff e.g. when scanning instrumented Method.invoke()
        }

        int loaderId = clazz.getLoaderId();

        // Now let's check if the callee class is actually loaded at this time. If not, we just record the fact that the
        // callee method is reachable using a placeholder class, and return
        BaseClassInfo refClazz = loadedJavaClassOrExistingPlaceholderForName(refClassName, loaderId);

        if ((refClazz == null) || (refClazz instanceof PlaceholderClassInfo)) {
            ReachableMethodPlaceholder pci = (refClazz == null) ? new ReachableMethodPlaceholder2(refClassName, loaderId)
                                                                : (ReachableMethodPlaceholder) refClazz;
            pci.registerReachableMethod(cms[1], cms[2]);

            if (refClazz == null) {
                registerPlaceholder(pci);
            }
        } else {
            checkAndScanMethod((DynamicClassInfo) refClazz, cms[1], cms[2], virtualCall, true, true);
        }

        offset = savedOffset;
        codeBytes = savedCodeBytes;
    }

    protected boolean tryInstrumentSpawnedThreads(DynamicClassInfo clazz) {
        if (instrumentSpawnedThreads) {
            if (clazz.implementsInterface("java/lang/Runnable") && (clazz.getName() != "java/lang/Thread")) { // NOI18N

                boolean res = markMethodRoot(clazz, "run", "()V"); // NOI18N
                checkAndScanMethod(clazz, "run", "()V", false, false, false); // NOI18N

                return res;
            }
        }

        return false;
    }

    /**
     * Mark the given method reachable. The boolean parameters affect this function in the following way:
     * - virtualCall = true means that we reached this method via a "call virtual" instruction, or otherwise know that it's
     * virtual, and want to treat it as virtual. So, for example, if checkSubclasses is true, a method with this name and
     * signature should be looked up in subclasses of this class.
     * - lookupInSuperIfNotFoundInThis = true means that if the given method is not found in this class, it should be looked
     * up in its superclasses.
     * - checkSubclasses = true means that if a method is virtual (either because virtualCall == true or because this method is
     * really defined in this class and marked as virtual), methods that override it in subclasses of this class should also
     * be marked reachable.
     */
    private boolean checkAndScanMethod(DynamicClassInfo clazz, String methodName, String methodSignature, boolean virtualCall,
                                       boolean lookupInSuperIfNotFoundInThis, boolean checkSubclasses) {
        boolean constructorNotInstrumented = false;
        String className = clazz.getName();
        int idx = clazz.getMethodIndex(methodName, methodSignature);

        if (idx != -1) {
            byte[] bytecode = null;

            if (!clazz.isMethodReachable(idx)) {
                clazz.setMethodReachable(idx);

                if (!clazz.isMethodStatic(idx) && !clazz.isMethodPrivate(idx) && !clazz.isMethodFinal(idx)
                        && (methodName != "<init>")) { //NOI18N
                    clazz.setMethodVirtual(idx);
                }

                if (clazz.isMethodNative(idx) || clazz.isMethodAbstract(idx)
                        || (!clazz.isMethodRoot(idx) && !clazz.isMethodMarker(idx) && !instrFilter.passes(className))
                        || (className == OBJECT_SLASHED_CLASS_NAME)) {  // Actually, just the Object.<init> method?
                    clazz.setMethodUnscannable(idx);
                } else if (methodName == "<init>" && !status.canInstrumentConstructor && clazz.getMajorVersion()>50) {
                    clazz.setMethodUnscannable(idx);
                    constructorNotInstrumented = true;
                } else {
                    bytecode = clazz.getMethodBytecode(idx);

                    if ((dontInstrumentEmptyMethods && isEmptyMethod(bytecode))
                            || (dontScanGetterSetterMethods && isGetterSetterMethod(bytecode))) {
                        clazz.setMethodUnscannable(idx);
                    } else {
                        if (isLeafMethod(bytecode)) {
                            clazz.setMethodLeaf(idx);
                        }
                    }
                }
            } else { // Method is already marked reachable - could be done for a not yet loaded class

                if (clazz.isMethodScanned(idx) || clazz.isMethodUnscannable(idx)) {
                    return true;
                } else {
                    bytecode = clazz.getMethodBytecode(idx);
                }
            }

            if (!clazz.isLoaded()) {
                return true; // No need to check subclasses because there are no loaded subclasses if this class itself is not loaded
                             // Class is loaded, method is reachable and not unscannable are sufficient conditions for instrumenting method
            }

            if (!clazz.isMethodUnscannable(idx)) {
                markClassAndMethodForInstrumentation(clazz, idx);
                clazz.setMethodScanned(idx);
                //if (!lookupInSuperIfNotFoundInThis && !checkSubclasses) System.out.println("Gonna scan potentially reachable " + className + "." + methodName + methodSignature);
                scanBytecode(clazz, bytecode);
            } else if (constructorNotInstrumented) {
                scanBytecode(clazz, bytecode);
            }
        }

        if (checkSubclasses && (((idx != -1) && clazz.isMethodVirtual(idx)) || ((idx == -1) && virtualCall))) {
            ArrayList subclasses = clazz.getSubclasses();

            if (subclasses != null) {
                for (int i = 0; i < subclasses.size(); i++) {
                    DynamicClassInfo subClass = (DynamicClassInfo) subclasses.get(i);
                    //System.out.println("Gonna scan subclass " + subclassNames.get(i) + " of class " + className + " for method " + methodName);
                    // DynamicClassInfo subClass = javaClassForName((String) subclassNames.get(i));
                    // if ((idx != -1 && subClass.overridesVirtualMethod(clazz, idx) != -1) || idx == -1) - see the comment in findAndMarkOverridingMethods()
                    // on why this seems to be of no use.
                    if (!subClass.isInterface()) {
                        boolean searchSuper = clazz.isInterface() && !subclasses.contains(subClass.getSuperClass());
                        checkAndScanMethod(subClass, methodName, methodSignature, virtualCall, searchSuper,
                                           false);
                    }
                }
            }
        }

        if (idx != -1) {
            return true;
        }

        if (!lookupInSuperIfNotFoundInThis) {
            return false;
        }

        // Method not defined in this class - try superclass, and if not successful - all superinterfaces
        if (!clazz.isInterface()) {
            DynamicClassInfo superClazz = clazz.getSuperClass();

            if ((superClazz != null) && (superClazz.getName() != clazz.getName())) {
                if (checkAndScanMethod(superClazz, methodName, methodSignature, virtualCall, true, false)) {
                    return true;
                }
            }
        }

        return false;
    }

    //----------------------------------- Private implementation ------------------------------------------------
    private DynamicClassInfo[] preGetInitialMethodsToInstrument(RootClassLoadedCommand rootLoaded) {
        //System.out.println("*** MS2: instr. initial");
        resetLoadedClassData();
        storeClassFileBytesForCustomLoaderClasses(rootLoaded);
        initInstrMethodData();

        DynamicClassInfo[] loadedClassInfos;
        String[] loadedClasses = rootLoaded.getAllLoadedClassNames();
        int[] loadedClassLoaderIds = rootLoaded.getAllLoadedClassLoaderIds();
        //if (instrumentSpawnedThreads) tmpClassInfos = new DynamicClassInfo[loadedClasses.length];
        loadedClassInfos = new DynamicClassInfo[loadedClasses.length]; //EJB Work: removed the condition in the above line as we need to return the temp array anyway (used to check for multiple roots, see the overloaded getInitialMethodsToInstrument )

        for (int i = 0; i < loadedClasses.length; i++) {
            DynamicClassInfo clazz = javaClassForName(loadedClasses[i], loadedClassLoaderIds[i]);

            if (clazz == null) {
                continue;
            }

            clazz.setLoaded(true);
            addToSubclassList(clazz, clazz);
            loadedClassInfos[i] = clazz;
        }

        return loadedClassInfos;
    }
}
