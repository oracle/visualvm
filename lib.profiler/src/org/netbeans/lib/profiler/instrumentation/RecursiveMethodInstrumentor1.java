/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.netbeans.lib.profiler.instrumentation;

import org.netbeans.lib.profiler.ProfilerEngineSettings;
import org.netbeans.lib.profiler.classfile.BaseClassInfo;
import org.netbeans.lib.profiler.classfile.DynamicClassInfo;
import org.netbeans.lib.profiler.classfile.PlaceholderClassInfo;
import org.netbeans.lib.profiler.global.ProfilingSessionStatus;
import org.netbeans.lib.profiler.utils.MiscUtils;
import org.netbeans.lib.profiler.utils.Wildcards;
import java.util.ArrayList;


/**
 * Recursive method scaner that implements the lazy instrumentation scheme ("Scheme B" in the JFluid paper).
 *
 * @author Misha Dmitriev
 * @author Adrian Mos
 */
public class RecursiveMethodInstrumentor1 extends RecursiveMethodInstrumentor {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    /** A placeholder for a class that contains reachable method and in which transferData() is specialized for this instrumentation scheme */
    protected class ReachableMethodPlaceholder1 extends ReachableMethodPlaceholder {
        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public ReachableMethodPlaceholder1(String refClassName, int classLoaderId) {
            super(refClassName, classLoaderId);
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void processReachableMethods(DynamicClassInfo clazz) {
            int len = methodNamesAndSigs.size();

            for (int i = 0; i < len; i += 2) {
                locateAndMarkMethodReachable(clazz, (String) methodNamesAndSigs.get(i), (String) methodNamesAndSigs.get(i + 1),
                                             false, true, false);
            }
        }
    }

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public RecursiveMethodInstrumentor1(ProfilingSessionStatus status, ProfilerEngineSettings settings) {
        super(status, settings);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    Object[] getInitialMethodsToInstrument(String[] loadedClasses, int[] loadedClassLoaderIds,
                                                  byte[][] cachedClassFileBytes, RootMethods roots) {
        DynamicClassInfo[] loadedClassInfos = preGetInitialMethodsToInstrument(loadedClasses, loadedClassLoaderIds,
                                                                               cachedClassFileBytes);

        rootMethods = roots;

        // Check which root classes have already been loaded, and mark their root methods accordingly
        for (int j = 0; j < loadedClassInfos.length; j++) {
            if (loadedClassInfos[j] == null) {
                continue; // Can this happen?
            }

            String className = loadedClassInfos[j].getName();

            tryInstrumentSpawnedThreads(loadedClassInfos[j]);

            for (int rIdx = 0; rIdx < rootMethods.classNames.length; rIdx++) {
                String rootClassName = rootMethods.classNames[rIdx].intern();

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

                    if (Wildcards.isPackageWildcard(rootClassName) || Wildcards.isMethodWildcard(rootMethods.methodNames[rIdx])) {
                        if (rootMethods.markerMethods[rIdx]) {
                            markAllMethodsMarker(loadedClassInfos[j]);
                        } else {
                            markAllMethodsRoot(loadedClassInfos[j]);
                        }

                        String[] methodNames = loadedClassInfos[j].getMethodNames();
                        String[] signatures = loadedClassInfos[j].getMethodSignatures();

                        for (int methodIdx = 0; methodIdx < methodNames.length; methodIdx++) {
                            locateAndMarkMethodReachable(loadedClassInfos[j], methodNames[methodIdx], signatures[methodIdx],
                                                         false, false, false);
                        }
                    } else {
                        markMethod(loadedClassInfos[j], rIdx);
                        locateAndMarkMethodReachable(loadedClassInfos[j], rootMethods.methodNames[rIdx],
                                                     rootMethods.methodSignatures[rIdx], false, false, false);
                    }
                }
            }
        }

        locateAndMarkMethodReachable(javaClassForName("java/lang/ClassLoader", 0), "loadClass",
                                     "(Ljava/lang/String;)Ljava/lang/Class;", true, false, true); // NOI18N

        return createInstrumentedMethodPack();
    }

    public Object[] getMethodsToInstrumentUponClassLoad(String className, int classLoaderId, boolean threadInCallGraph) {
        //System.out.println("*** MS1: instr. upon CL: " + className);
        className = className.replace('.', '/').intern(); // NOI18N

        // If a class doesn't pass the current instrumentation filter, we can't immediately reject it, since there is a chance
        // it contains some root methods. So we have to check that first.
        boolean isRootClass = false;

        for (int rIdx = 0; rIdx < rootMethods.classNames.length; rIdx++) {
            final String rootClassName = rootMethods.classNames[rIdx].intern();

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

        boolean normallyFilteredOut = !instrFilter.passesFilter(className);

        if (!isRootClass) {
            if (normallyFilteredOut) {
                return null;
            }
        }

        BaseClassInfo placeholder = loadedJavaClassOrExistingPlaceholderForName(className, classLoaderId);
        DynamicClassInfo clazz = javaClassForName(className, classLoaderId);

        if (clazz == null) {
            return null; // Warning already issued
        }

        initInstrMethodData();
        boolean instrumentClinit = threadInCallGraph;

        if (!clazz.isLoaded()) {
            clazz.setLoaded(true);
            addToSubclassList(clazz, normallyFilteredOut ? null : clazz); // null as a second parameter will result in NOT marking any methods reachable

            // Check to see if this class has been marked as root by the user:
            for (int rIdx = 0; rIdx < rootMethods.classNames.length; rIdx++) {
                final String rootClassName = rootMethods.classNames[rIdx].intern();

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

                    if (Wildcards.isPackageWildcard(rootClassName) || Wildcards.isMethodWildcard(rootMethods.methodNames[rIdx])) {
                        if (rootMethods.markerMethods[rIdx]) {
                            markAllMethodsMarker(clazz);
                        } else {
                            markAllMethodsRoot(clazz);
                        }

                        String[] methodNames = clazz.getMethodNames();
                        String[] signatures = clazz.getMethodSignatures();

                        for (int methodIdx = 0; methodIdx < methodNames.length; methodIdx++) {
                            locateAndMarkMethodReachable(clazz, methodNames[methodIdx], signatures[methodIdx], false, false, false);
                        }
                    } else {
                        markMethod(clazz, rIdx);
                        locateAndMarkMethodReachable(clazz, rootMethods.methodNames[rIdx], rootMethods.methodSignatures[rIdx],
                                                     false, false, false);
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

        locateAndMarkMethodReachable(clazz, methodName, methodSignature, false, false, false);

        //countReachableScannableMethods(clazz);
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
            locateAndMarkMethodReachable(subClass, methodNames[i], methodSignatures[i], true, false, false);
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
            locateAndMarkMethodReachable((DynamicClassInfo) refClazz, cms[1], cms[2], virtualCall, true, true);
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
                locateAndMarkMethodReachable(clazz, "run", "()V", false, false, false); // NOI18N

                return res;
            }
        }

        return false;
    }

    private void instrumentClinit(DynamicClassInfo clazz) {
        locateAndMarkMethodReachable(clazz, "<clinit>", "()V", false, false, false); // NOI18N        
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
     */
    private boolean locateAndMarkMethodReachable(DynamicClassInfo clazz, String methodName, String methodSignature,
                                                 boolean virtualCall, boolean lookupInSuperIfNotFoundInThis,
                                                 boolean checkSubclasses) {
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
                    || (!clazz.isMethodRoot(idx) && !clazz.isMethodMarker(idx) && !instrFilter.passesFilter(className))
                    || (className == "java/lang/Object")) { // NOI18N  // Actually, just the Object.<init> method?
                clazz.setMethodUnscannable(idx);
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
            }
        }

        if (checkSubclasses && (((idx != -1) && clazz.isMethodVirtual(idx)) || ((idx == -1) && virtualCall))) {
            ArrayList subclasses = clazz.getSubclasses();

            if (subclasses != null) {
                for (int i = 0; i < subclasses.size(); i++) {
                    //System.out.println("Gonna scan subclass " + subclassNames.get(i) + " of class " + className + " for method " + methodName);
                    // DynamicClassInfo subClass = javaClassForName((String) subclassNames.get(i));
                    // if ((idx != -1 && subClass.overridesVirtualMethod(clazz, idx) != -1) || idx == -1) - see the comment in findAndMarkOverridingMethods()
                    // on why this seems to be of no use.
                    locateAndMarkMethodReachable((DynamicClassInfo) subclasses.get(i), methodName, methodSignature, virtualCall,
                                                 false, false);
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
                if (locateAndMarkMethodReachable(superClazz, methodName, methodSignature, virtualCall, true, false)) {
                    return true;
                }
            }
        }

        DynamicClassInfo[] interfaces = clazz.getSuperInterfaces();

        if (interfaces == null) {
            return false;
        }

        for (int i = 0; i < interfaces.length; i++) {
            DynamicClassInfo intfClazz = interfaces[i];

            if (intfClazz == null) {
                continue;
            }

            if (locateAndMarkMethodReachable(intfClazz, methodName, methodSignature, virtualCall, true, false)) {
                return true;
            }
        }

        return false;
    }

    //----------------------------------- Private implementation ------------------------------------------------
    private DynamicClassInfo[] preGetInitialMethodsToInstrument(String[] loadedClasses, int[] loadedClassLoaderIds,
                                                                byte[][] cachedClassFileBytes) {
        //System.out.println("*** MS1: instr. initial: " + rootClassName);
        resetLoadedClassData();
        storeClassFileBytesForCustomLoaderClasses(loadedClasses, loadedClassLoaderIds, cachedClassFileBytes);
        initInstrMethodData();

        DynamicClassInfo[] loadedClassInfos = new DynamicClassInfo[loadedClasses.length];

        for (int i = 0; i < loadedClasses.length; i++) {
            String className = loadedClasses[i].replace('.', '/').intern(); // NOI18N
            DynamicClassInfo clazz = javaClassForName(className, loadedClassLoaderIds[i]);

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
