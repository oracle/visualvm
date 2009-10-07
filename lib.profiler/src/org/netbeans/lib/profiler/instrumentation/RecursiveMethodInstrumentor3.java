/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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
import org.netbeans.lib.profiler.classfile.DynamicClassInfo;
import org.netbeans.lib.profiler.global.ProfilingSessionStatus;
import org.netbeans.lib.profiler.utils.StringUtils;
import org.netbeans.lib.profiler.utils.Wildcards;


/**
 * Recursive method scaner that implements the total instrumentation scheme.
 * In fact, it's not even a scaner, since it just instruments absolutely everything -
 * but it uses the same interface.
 *
 * @author Tomas Hurka
 * @author Misha Dmitriev
 */
public class RecursiveMethodInstrumentor3 extends RecursiveMethodInstrumentor {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    // Attributes to hold saved values for root classes, methods and signatures
    private boolean noExplicitRootsSpecified = false, mainMethodInstrumented = false;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public RecursiveMethodInstrumentor3(ProfilingSessionStatus status, ProfilerEngineSettings settings) {
        super(status, settings);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public Object[] getMethodsToInstrumentUponClassLoad(String className, int classLoaderId, boolean threadInCallGraph) {
        boolean DEBUG = false;

        /*    if (className.startsWith("java2d")) {
           DEBUG = true;
           }
         */
        if (DEBUG) {
            System.out.println("*** MS2: instr. upon CL: " + className); // NOI18N
        }

        className = className.replace('.', '/').intern(); // NOI18N

        DynamicClassInfo clazz = javaClassForName(className, classLoaderId);

        if (clazz == null) {
            return null;
        }

        if (DEBUG) {
            System.out.println("*** MS2: instr. upon CL 2: " + clazz.getNameAndLoader()); // NOI18N
        }

        clazz.setLoaded(true);
        addToSubclassList(clazz, clazz); // Have to call this in advance to determine if a class implements Runnable (possibly indirectly)

        if (clazz.isInterface()) {
            return null;
        }

        initInstrMethodData();

        // Mark as roots all methods that the total instrumentation method views as implicit roots
        boolean isRootClass = false;
        int rootIdxForAll = -1;

        isRootClass = tryInstrumentSpawnedThreads(clazz); // This only checks for Runnable.run()

        if (noExplicitRootsSpecified && !mainMethodInstrumented) { // Check if this class has main method. The first loaded class with main method should be main class.

            if (tryMainMethodInstrumentation(clazz)) {
                isRootClass = true;
                mainMethodInstrumented = true;
            }
        }

        // Check to see if this class has been marked as root by the user:
        if (!isRootClass) {
            for (int rIdx = 0; rIdx < rootMethods.classNames.length; rIdx++) {
                String rootClassName = rootMethods.classNames[rIdx];

                if (rootMethods.classesWildcard[rIdx]) {
                    if (Wildcards.matchesWildcard(rootClassName, className)) {
                        //            System.out.println("Matched package wildcard - " + rootClassName);
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
        }

        boolean normallyFilteredOut = !instrFilter.passesFilter(className);

        if (!isRootClass) {
            if (normallyFilteredOut) {
                return null;
            }
        }

        // Check to see if this class has been marked as root by the user:
        for (int rIdx = 0; rIdx < rootMethods.classNames.length; rIdx++) {
            String rootClassName = rootMethods.classNames[rIdx];
            boolean isMatch = false;

            if (rootMethods.classesWildcard[rIdx]) {
                if (Wildcards.matchesWildcard(rootClassName, className)) {
                    //            System.out.println("Matched package wildcard - " + rootClassName);
                    isMatch = true;
                }
            } else {
                if (className.equals(rootClassName)) { // precise match
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
                } else {
                    markMethod(clazz, rIdx);
                    checkAndMarkMethodForInstrumentation(clazz, rootMethods.methodNames[rIdx], rootMethods.methodSignatures[rIdx]);
                }
            }
        }

        if (!normallyFilteredOut || clazz.getAllMethodsMarkers() || clazz.getAllMethodsRoots()) {
            checkAndMarkAllMethodsForInstrumentation(clazz);
        }

        return createInstrumentedMethodPack();
    }

    public Object[] getMethodsToInstrumentUponMethodInvocation(String className, int classLoaderId, String methodName,
                                                               String methodSignature) {
        return null; // This method is just not used with this flavour of MethodScanner
    }

    public Object[] getMethodsToInstrumentUponReflectInvoke(String className, int classLoaderId, String methodName,
                                                            String methodSignature) {
        return null; // Doesn't have to do anything - everything is handled upon class load
    }

    protected void findAndMarkOverridingMethodsReachable(DynamicClassInfo superClass, DynamicClassInfo subClass) {
        // Doesn't do anything (actually not used/called at all) in this scaner
    }

    protected void processInvoke(DynamicClassInfo clazz, boolean virtualCall, int index) {
        // Doesn't do anything (not used) in this scaner
    }

    protected boolean tryInstrumentSpawnedThreads(DynamicClassInfo clazz) {
//        System.err.println("TryInstrumentSpawnedThreads: " + instrumentSpawnedThreads + "/" + noExplicitRootsSpecified);
        if (instrumentSpawnedThreads || noExplicitRootsSpecified) {
            if (clazz.implementsInterface("java/lang/Runnable") && (clazz.getName() != "java/lang/Thread")) { // NOI18N

                boolean res = markMethodRoot(clazz, "run", "()V"); // NOI18N
                checkAndMarkMethodForInstrumentation(clazz, "run", "()V"); // NOI18N

                return res;
            }
        }

        return false;
    }

    protected boolean tryMainMethodInstrumentation(DynamicClassInfo clazz) {
        int idx = clazz.getMethodIndex("main", "([Ljava/lang/String;)V"); // NOI18N

        if (idx == -1) {
            return false;
        }

        if (!(clazz.isMethodStatic(idx) && clazz.isMethodPublic(idx))) {
            return false;
        }

        markMethodRoot(clazz, "main", "([Ljava/lang/String;)V"); // NOI18N
        checkAndMarkMethodForInstrumentation(clazz, idx);

        return true;
    }

    Object[] getInitialMethodsToInstrument(String[] loadedClasses, int[] loadedClassLoaderIds, byte[][] cachedClassFileBytes,
                                           RootMethods roots) {
        DynamicClassInfo[] loadedClassInfos = preGetInitialMethodsToInstrument(loadedClasses, loadedClassLoaderIds,
                                                                               cachedClassFileBytes);

        rootMethods = roots;
        checkForNoRootsSpecified(roots);

        // Check which root classes have already been loaded, and mark their root methods accordingly
        for (int j = 0; j < loadedClassInfos.length; j++) {
            if (loadedClassInfos[j] == null) {
                continue; // Can this happen?
            }

            tryInstrumentSpawnedThreads(loadedClassInfos[j]); // This only checks for Runnable.run()

            for (int rIdx = 0; rIdx < rootMethods.classNames.length; rIdx++) {
                String rootClassName = rootMethods.classNames[rIdx];
                boolean isMatch = false;

                if (rootMethods.classesWildcard[rIdx]) {
                    if (Wildcards.matchesWildcard(rootClassName, loadedClassInfos[j].getName())) {
                        //            System.out.println("Matched package wildcard - " + rootClassName);
                        isMatch = true;
                    }
                } else {
                    if (loadedClassInfos[j].getName().equals(rootClassName)) { // precise match
                        isMatch = true;
                    }
                }

                if (isMatch) {
                    if (Wildcards.isPackageWildcard(rootClassName) || Wildcards.isMethodWildcard(rootMethods.methodNames[rIdx])) {
                        if (rootMethods.markerMethods[rIdx]) {
                            markAllMethodsMarker(loadedClassInfos[j]);
                        } else {
                            markAllMethodsRoot(loadedClassInfos[j]);
                        }
                    } else {
                        markMethod(loadedClassInfos[j], rIdx);
                        checkAndMarkMethodForInstrumentation(loadedClassInfos[j], rootMethods.methodNames[rIdx],
                                                             rootMethods.methodSignatures[rIdx]);
                    }
                }
            }

            checkAndMarkAllMethodsForInstrumentation(loadedClassInfos[j]);
        }

        // So that class loading is measured correctly from the beginning
        checkAndMarkMethodForInstrumentation(javaClassForName("java/lang/ClassLoader", 0), "loadClass",
                                             "(Ljava/lang/String;)Ljava/lang/Class;"); // NOI18N

        return createInstrumentedMethodPack();
    }

    private void checkAndMarkAllMethodsForInstrumentation(DynamicClassInfo clazz) {
        if (clazz.isInterface()) {
            return;
        }

        String[] methods = clazz.getMethodNames();

        for (int i = 0; i < methods.length; i++) {
            checkAndMarkMethodForInstrumentation(clazz, i);
        }
    }

    /** Mark the given method reachable, if there are no barriers for that (like native, empty, etc. method) */
    private void checkAndMarkMethodForInstrumentation(DynamicClassInfo clazz, String methodName, String methodSignature) {
        if (clazz == null) {
            return;
        }

        int idx = clazz.getMethodIndex(methodName, methodSignature);

        if (idx == -1) {
            return;
        }

        checkAndMarkMethodForInstrumentation(clazz, idx);
    }

    private void checkAndMarkMethodForInstrumentation(DynamicClassInfo clazz, int idx) {
        String className = clazz.getName();

        if (!clazz.isMethodReachable(idx)) {
            clazz.setMethodReachable(idx);

            if (clazz.isMethodNative(idx) || clazz.isMethodAbstract(idx)
                    || (!clazz.isMethodRoot(idx) && !clazz.isMethodMarker(idx) && !instrFilter.passesFilter(className))
                    || (className == "java/lang/Object") // NOI18N // Actually, just the Object.<init> method?
            ) {
                clazz.setMethodUnscannable(idx);
            } else {
                byte[] bytecode = clazz.getMethodBytecode(idx);

                if ((dontInstrumentEmptyMethods && isEmptyMethod(bytecode))
                        || (dontScanGetterSetterMethods && isGetterSetterMethod(bytecode))) {
                    clazz.setMethodUnscannable(idx);
                } else {
                    clazz.setMethodLeaf(idx);
                }
            }

            // Class is loaded, method is reachable and not unscannable are sufficient conditions for instrumenting method
            if (!clazz.isMethodUnscannable(idx)) {
                markClassAndMethodForInstrumentation(clazz, idx);
            }
        }
    }

    private void checkForNoRootsSpecified(RootMethods roots) {
//        System.err.println("Checking for no roots specified");
        // It may happen, for example when directly attaching to a remote application and choosing the Entire App CPU
        // profiling, that there are no explicitly specified root methods (because the main method is not known in advance).
        // To get sensible profiling results, we take special measures, by just guessing what the main class is.
        noExplicitRootsSpecified = true;
        
        if ((roots != null) && (roots.classNames.length != 0)) {
            int rootCount = roots.markerMethods.length;

            if (rootCount > 0) {
                for (int i = 0; i < rootCount; i++) {
                    if (!roots.markerMethods[i]) {
                        noExplicitRootsSpecified = false;

                        break;
                    }
                }
            }
        }
//        System.err.println("NoRootsSpecified = " + noExplicitRootsSpecified);
    }

    //----------------------------------- Private implementation ------------------------------------------------
    private DynamicClassInfo[] preGetInitialMethodsToInstrument(String[] loadedClasses, int[] loadedClassLoaderIds,
                                                                byte[][] cachedClassFileBytes) {
        //System.out.println("*** MS2: instr. initial");
        reflectInvokeInstrumented = true; // We don't need to instrument reflection specially in this mode

        resetLoadedClassData();
        storeClassFileBytesForCustomLoaderClasses(loadedClasses, loadedClassLoaderIds, cachedClassFileBytes);
        initInstrMethodData();

        DynamicClassInfo[] loadedClassInfos = null;
        loadedClassInfos = new DynamicClassInfo[loadedClasses.length]; //EJB Work: removed the condition in the above line as we need to return the temp array anyway (used to check for multiple roots, see the overloaded getInitialMethodsToInstrument )

        for (int i = 0; i < loadedClasses.length; i++) {
            String className = loadedClasses[i].replace('.', '/').intern(); // NOI18N
            DynamicClassInfo clazz = javaClassForName(className, loadedClassLoaderIds[i]);

            if (clazz == null) {
                // warning already issued in ClassRepository.lookupClass method, no need to do it again
                continue;
            }

            clazz.setLoaded(true);
            addToSubclassList(clazz, clazz); // Needed basically only for methods like implementsInterface() to work correctly
            loadedClassInfos[i] = clazz;
        }

        return loadedClassInfos;
    }
}
