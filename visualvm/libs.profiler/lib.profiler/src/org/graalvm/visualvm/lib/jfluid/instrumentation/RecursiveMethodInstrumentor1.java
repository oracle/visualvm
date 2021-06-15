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
import java.util.Collection;
import org.graalvm.visualvm.lib.jfluid.ProfilerEngineSettings;
import org.graalvm.visualvm.lib.jfluid.classfile.BaseClassInfo;
import org.graalvm.visualvm.lib.jfluid.classfile.DynamicClassInfo;
import org.graalvm.visualvm.lib.jfluid.classfile.PlaceholderClassInfo;
import org.graalvm.visualvm.lib.jfluid.global.ProfilingSessionStatus;
import org.graalvm.visualvm.lib.jfluid.utils.MiscUtils;
import org.graalvm.visualvm.lib.jfluid.utils.Wildcards;
import org.graalvm.visualvm.lib.jfluid.wireprotocol.RootClassLoadedCommand;


/**
 * Recursive method scanner that implements the lazy instrumentation scheme ("Scheme B" in the JFluid paper).
 *
 * @author Misha Dmitriev
 * @author Adrian Mos
 */
public class RecursiveMethodInstrumentor1 extends RecursiveMethodInstrumentor {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    /** A placeholder for a class that contains reachable method and in which transferData() is specialized for this instrumentation scheme */
    protected class ReachableMethodPlaceholder1 extends ReachableMethodPlaceholder {
        //~ Constructors ---------------------------------------------------------------------------------------------------------

        ReachableMethodPlaceholder1(String refClassName, int classLoaderId) {
            super(refClassName, classLoaderId);
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void processReachableMethods(DynamicClassInfo clazz) {
            int len = methodNamesAndSigs.size();

            for (int i = 0; i < len; i += 2) {
                locateAndMarkMethodReachable(clazz, (String) methodNamesAndSigs.get(i), (String) methodNamesAndSigs.get(i + 1),
                                             false, true, false, false);
            }
        }
    }

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public RecursiveMethodInstrumentor1(ProfilingSessionStatus status, ProfilerEngineSettings settings) {
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

            String className = loadedClassInfos[j].getName();

            markProfilingPonitForInstrumentation(loadedClassInfos[j]);
            tryInstrumentSpawnedThreads(loadedClassInfos[j]);

            for (int rIdx = 0; rIdx < rootMethods.classNames.length; rIdx++) {
                String rootClassName = rootMethods.classNames[rIdx];

                boolean isMatch = false;

                if (rootMethods.classesWildcard[rIdx]) {
                    if (Wildcards.matchesWildcard(rootClassName, className)) {
                        //            System.out.println("Matched package wildcard - " + rootClassName);
                        isMatch = true;
                    }
                } else {
                    if (className == rootClassName) { // precise match
                        isMatch = true;
                    }
                }

                if (isMatch) { // This root class is loaded
                    boolean isMarkerMethod = rootMethods.markerMethods[rIdx];
                    boolean checkSubClasses = loadedClassInfos[j].isInterface() && isMarkerMethod;
                    
                    if (Wildcards.isPackageWildcard(rootClassName) || Wildcards.isMethodWildcard(rootMethods.methodNames[rIdx])) {
                        if (isMarkerMethod) {
                            markAllMethodsMarker(loadedClassInfos[j]);
                        } else {
                            markAllMethodsRoot(loadedClassInfos[j]);
                        }

                        String[] methodNames = loadedClassInfos[j].getMethodNames();
                        String[] signatures = loadedClassInfos[j].getMethodSignatures();

                        for (int methodIdx = 0; methodIdx < methodNames.length; methodIdx++) {
                            locateAndMarkMethodReachable(loadedClassInfos[j], methodNames[methodIdx], signatures[methodIdx],
                                                         false, false, checkSubClasses, isMarkerMethod);
                        }
                    } else {
                        markMethod(loadedClassInfos[j], rIdx);
                        locateAndMarkMethodReachable(loadedClassInfos[j], rootMethods.methodNames[rIdx],
                                                     rootMethods.methodSignatures[rIdx], false, false, checkSubClasses, isMarkerMethod);
                    }
                }
            }
        }

        locateAndMarkMethodReachable(javaClassForName("java/lang/ClassLoader", 0), "loadClass",   // NOI18N
                                     "(Ljava/lang/String;)Ljava/lang/Class;", true, false, true, false); // NOI18N

        return createInstrumentedMethodPack();
    }

    public Object[] getMethodsToInstrumentUponClassLoad(String classNameDot, int classLoaderId, boolean threadInCallGraph) {
        //System.out.println("*** MS1: instr. upon CL: " + className);
        String className = classNameDot.replace('.', '/').intern(); // NOI18N

        initInstrMethodData();
        markProfilingPointForInstrumentation(classNameDot,className,classLoaderId);
        // If a class doesn't pass the current instrumentation filter, we can't immediately reject it, since there is a chance
        // it contains some root methods. So we have to check that first.
        boolean isRootClass = false;

        for (int rIdx = 0; rIdx < rootMethods.classNames.length; rIdx++) {
            final String rootClassName = rootMethods.classNames[rIdx];

            if (rootMethods.classesWildcard[rIdx]) {
                if (Wildcards.matchesWildcard(rootClassName, className)) {
                    //          System.out.println("Matched package wildcard - " + rootClassName);
                    isRootClass = true;

                    break;
                }
            } else {
                if (className == rootClassName) {
                    isRootClass = true;

                    break;
                }
            }
        }

        boolean normallyFilteredOut = !instrFilter.passes(className);

        if (!isRootClass) {
            if (normallyFilteredOut) {
                return createInstrumentedMethodPack(); // profile points !
            }
        }

        BaseClassInfo placeholder = loadedJavaClassOrExistingPlaceholderForName(className, classLoaderId);
        DynamicClassInfo clazz = javaClassForName(className, classLoaderId);

        if (clazz == null) {
            return null; // Warning already issued
        }

        boolean instrumentClinit = threadInCallGraph;

        if (!clazz.isLoaded()) {
            clazz.setLoaded(true);
            addToSubclassList(clazz, normallyFilteredOut ? null : clazz); // null as a second parameter will result in NOT marking any methods reachable

            // Check to see if this class has been marked as root by the user:
            for (int rIdx = 0; rIdx < rootMethods.classNames.length; rIdx++) {
                final String rootClassName = rootMethods.classNames[rIdx];

                boolean isMatch = false;

                if (rootMethods.classesWildcard[rIdx]) {
                    if (Wildcards.matchesWildcard(rootClassName, className)) {
                        //            System.out.println("Matched package wildcard - " + rootClassName);
                        isMatch = true;
                    }
                } else {
                    if (className == rootClassName) { // precise match
                        isMatch = true;
                    }
                }

                if (isMatch) { // it is indeed a root class
                    boolean checkSubClasses = clazz.isInterface() && rootMethods.markerMethods[rIdx];

                    if (Wildcards.isPackageWildcard(rootClassName) || Wildcards.isMethodWildcard(rootMethods.methodNames[rIdx])) {
                        if (rootMethods.markerMethods[rIdx]) {
                            markAllMethodsMarker(clazz);
                        } else {
                            markAllMethodsRoot(clazz);
                        }

                        String[] methodNames = clazz.getMethodNames();
                        String[] signatures = clazz.getMethodSignatures();

                        for (int methodIdx = 0; methodIdx < methodNames.length; methodIdx++) {
                            locateAndMarkMethodReachable(clazz, methodNames[methodIdx], signatures[methodIdx], false, false, checkSubClasses, false);
                        }
                    } else {
                        markMethod(clazz, rIdx);
                        locateAndMarkMethodReachable(clazz, rootMethods.methodNames[rIdx], rootMethods.methodSignatures[rIdx],
                                                     false, false, checkSubClasses, true);
                    }
                }
            }
            if (instrumentClinit) {
                instrumentClinit(clazz);
            }
            if (placeholder instanceof ReachableMethodPlaceholder1) {
                ((ReachableMethodPlaceholder1)placeholder).processReachableMethods(clazz);
            } else if (placeholder != null) {
//                System.out.println("Class: "+placeholder.getNameAndLoader());
            }
            tryInstrumentSpawnedThreads(clazz);

            return createInstrumentedMethodPack();
        } else {
            return null;
        }
    }

    public Object[] getMethodsToInstrumentUponMethodInvocation(String className, int classLoaderId, String methodName,
                                                               String methodSignature) {
        //System.out.println("*** MS1: instr. upon MI: " + className + "." + methodName + methodSignature);
        className = className.replace('.', '/').intern(); // NOI18N
        methodName = methodName.intern();
        methodSignature = methodSignature.intern();
        initInstrMethodData();

        checkAndScanMethod(className, classLoaderId, methodName, methodSignature);

        return createInstrumentedMethodPack();
    }

    public Object[] getMethodsToInstrumentUponReflectInvoke(String className, int classLoaderId, String methodName,
                                                            String methodSignature) {
        //System.out.println("*** MS1: instr. upon reflect MI: " + className + "." + methodName + methodSignature);
        className = className.replace('.', '/').intern();

        DynamicClassInfo clazz = javaClassForName(className, classLoaderId);

        if (clazz == null) {
            // System.err.println("Warning: could not find class " + className + " loaded by the VM on the class path");
            // warning already issued in ClassRepository.lookupClass method, no need to do it again
            return null;
        }

        if (!clazz.isLoaded()) {
            return null; // Probably impossible
        }

        initInstrMethodData();

        methodName = methodName.intern();
        methodSignature = methodSignature.intern();

        locateAndMarkMethodReachable(clazz, methodName, methodSignature, false, false, false, false);

        //countReachableScannableMethods(clazz);
        return createInstrumentedMethodPack();
    }

    protected void findAndMarkOverridingMethodsReachable(DynamicClassInfo superClass, DynamicClassInfo subClass) {
        if (!superClass.hasMethodReachable()) {
            return;
        }
        
        String[] methodNames = superClass.getMethodNames();
        String[] methodSignatures = superClass.getMethodSignatures();
        boolean lookupInSuper = superClass.isInterface();

        for (int i = 0; i < methodNames.length; i++) {
            if (!(superClass.isMethodVirtual(i) && superClass.isMethodReachable(i))) {
                continue;
            }
            boolean isMarker = superClass.isMethodMarker(i);

            // int idx = subClass.overridesVirtualMethod(superClass, i); - I once tried this, but with no visible effect. Strict check
            // for whether a method with the same name and signature in subclass really overrrides a method in superclass, given all other
            // conditions that we have already checked (e.g. that the method in superclass is not private), will only detect a pathological
            // case when both method versions are package-private. This is rare, if ever happens at all.
            locateAndMarkMethodReachable(subClass, methodNames[i], methodSignatures[i], true, lookupInSuper, false, isMarker);
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
            ReachableMethodPlaceholder pci = (refClazz == null) ? new ReachableMethodPlaceholder1(refClassName, loaderId)
                                                                : (ReachableMethodPlaceholder) refClazz;
            pci.registerReachableMethod(cms[1], cms[2]);

            if (refClazz == null) {
                registerPlaceholder(pci);
            }
        } else {
            locateAndMarkMethodReachable((DynamicClassInfo) refClazz, cms[1], cms[2], virtualCall, true, true, false);
        }

        offset = savedOffset;
        codeBytes = savedCodeBytes;
    }

    /**
     * If instrumenteSpawnedThreads is true and the given class implements Runnable, find  and mark as root its run() method.
     */
    protected boolean tryInstrumentSpawnedThreads(DynamicClassInfo clazz) {
        if (instrumentSpawnedThreads) {
            if (clazz.implementsInterface("java/lang/Runnable") && (clazz.getName() != "java/lang/Thread")) { // NOI18N

                boolean res = markMethodRoot(clazz, "run", "()V"); // NOI18N
                locateAndMarkMethodReachable(clazz, "run", "()V", false, false, false, false); // NOI18N

                return res;
            }
        }

        return false;
    }

    private void instrumentClinit(DynamicClassInfo clazz) {
        locateAndMarkMethodReachable(clazz, "<clinit>", "()V", false, false, false, false); // NOI18N        
    }

    private void checkAndScanMethod(String className, int classLoaderId, String methodName, String methodSignature) {
        DynamicClassInfo clazz = javaClassForName(className, classLoaderId);

        if (clazz == null) {
            return;
        }

        int idx = clazz.getMethodIndex(methodName, methodSignature);

        if (idx == -1) {
            MiscUtils.internalError("can't find method " + methodName + methodSignature + " in class " + className); // NOI18N
        }

        if (clazz.isMethodUnscannable(idx)) {
            MiscUtils.internalError("got to scan unscannable method " + className + "." + methodName + methodSignature); // NOI18N
        }

        clazz.setMethodScanned(idx);
        scanMethod(clazz, idx);
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
     * - setAsMarkerMethod = true means tag reachable method as marker method
     */
    private boolean locateAndMarkMethodReachable(DynamicClassInfo clazz, String methodName, String methodSignature,
                                                 boolean virtualCall, boolean lookupInSuperIfNotFoundInThis,
                                                 boolean checkSubclasses, boolean setAsMarkerMethod) {
        boolean constructorNotInstrumented = false;
        if (clazz == null) {
            return false; // Normally shouldn't happen, it's just development-time facilitation (introduced when working on 1.5 support)
        }

        String className = clazz.getName();
        int idx = clazz.getMethodIndex(methodName, methodSignature);

        if (idx != -1) {
            if (clazz.isMethodReachable(idx)) {
                return true;
            }

            clazz.setMethodReachable(idx);

            if (!clazz.isMethodStatic(idx) && !clazz.isMethodPrivate(idx) && !clazz.isMethodFinal(idx)
                    && (methodName != "<init>")) {  // NOI18N
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
                byte[] bytecode = clazz.getMethodBytecode(idx);

                if ((dontInstrumentEmptyMethods && isEmptyMethod(bytecode))
                        || (dontScanGetterSetterMethods && isGetterSetterMethod(bytecode))) {
                    clazz.setMethodUnscannable(idx);
                } else {
                    if (isLeafMethod(bytecode)) {
                        clazz.setMethodLeaf(idx);
                    }
                }
            }

            if (!clazz.isLoaded()) {
                return true; // No need to check subclasses because there are no loaded subclasses if this class itself is not loaded
            }
            
            // Class is loaded, method is reachable and not unscannable are sufficient conditions for instrumenting method
            if (!clazz.isMethodUnscannable(idx)) {
                markClassAndMethodForInstrumentation(clazz, idx);
                if (setAsMarkerMethod) {
                    clazz.setMethodMarker(idx);
                }
            } else if (constructorNotInstrumented) {
                scanMethod(clazz, idx);
            }
        }

        if (checkSubclasses && (((idx != -1) && clazz.isMethodVirtual(idx)) || ((idx == -1) && virtualCall)) && className != OBJECT_SLASHED_CLASS_NAME) {
            ArrayList subclasses = clazz.getSubclasses();

            if (subclasses != null) {
                preloadBytecodeForAllSubclasses(subclasses);
                for (int i = 0; i < subclasses.size(); i++) {
                    DynamicClassInfo subClass = (DynamicClassInfo) subclasses.get(i);
                    //System.out.println("Gonna scan subclass " + subclassNames.get(i) + " of class " + className + " for method " + methodName);
                    // DynamicClassInfo subClass = javaClassForName((String) subclassNames.get(i));
                    // if ((idx != -1 && subClass.overridesVirtualMethod(clazz, idx) != -1) || idx == -1) - see the comment in findAndMarkOverridingMethods()
                    // on why this seems to be of no use.
                    if (!subClass.isInterface()) {
                        boolean searchSuper = clazz.isInterface() && !subclasses.contains(subClass.getSuperClass());
                        locateAndMarkMethodReachable(subClass, methodName, methodSignature, virtualCall,
                                                      searchSuper, false, setAsMarkerMethod);
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
                if (locateAndMarkMethodReachable(superClazz, methodName, methodSignature, virtualCall, true, false, setAsMarkerMethod)) {
                    return true;
                }
            }
        }
        return false;
    }

    //----------------------------------- Private implementation ------------------------------------------------
    private DynamicClassInfo[] preGetInitialMethodsToInstrument(RootClassLoadedCommand rootLoaded) {
        //System.out.println("*** MS1: instr. initial: " + rootClassName);
        resetLoadedClassData();
        storeClassFileBytesForCustomLoaderClasses(rootLoaded);
        initInstrMethodData();

        String[] loadedClasses = rootLoaded.getAllLoadedClassNames();
        int[] loadedClassLoaderIds = rootLoaded.getAllLoadedClassLoaderIds();
        DynamicClassInfo[] loadedClassInfos = new DynamicClassInfo[loadedClasses.length];

        // preload all classes
        for (int i = 0; i < loadedClasses.length; i++) {
            DynamicClassInfo clazz = javaClassForName(loadedClasses[i], loadedClassLoaderIds[i]);

            if (clazz == null) {
                continue;
            }

            clazz.setLoaded(true);
            loadedClassInfos[i] = clazz;
        }
        for (int i = 0; i < loadedClasses.length; i++) {
            DynamicClassInfo clazz = javaClassForName(loadedClasses[i], loadedClassLoaderIds[i]);

            if (clazz != null) {
                addToSubclassList(clazz, clazz);
            }
        }

        return loadedClassInfos;
    }
    
    void preloadBytecodeForAllSubclasses(Collection classes) {
        for (Object clazz : classes) {
            ((DynamicClassInfo)clazz).preloadBytecode();
        }
    }
}
