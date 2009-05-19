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
import org.netbeans.lib.profiler.classfile.DynamicClassInfo;
import org.netbeans.lib.profiler.classfile.PlaceholderClassInfo;
import org.netbeans.lib.profiler.client.RuntimeProfilingPoint;
import org.netbeans.lib.profiler.global.InstrumentationFilter;
import org.netbeans.lib.profiler.global.ProfilingSessionStatus;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Base class for two "recursive" method scanners, implementing the "eager" and "lazy" transitive call subgraph revelation and
 * instrumentation schemes. This class contains functionality used by both scaners.
 *
 * @author Tomas Hurka
 * @author Misha Dmitriev
 * @author Adrian Mos
 */
public abstract class RecursiveMethodInstrumentor extends ClassManager {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    protected static class ReachableMethodPlaceholder extends PlaceholderClassInfo {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        protected ArrayList methodNamesAndSigs;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        ReachableMethodPlaceholder(String className, int classLoaderId) {
            super(className, classLoaderId);
            methodNamesAndSigs = new ArrayList();
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void registerReachableMethod(String methodName, String methodSig) {
            int nameIdx = methodNamesAndSigs.indexOf(methodName);

            if (nameIdx != -1) {
                if (methodNamesAndSigs.get(nameIdx + 1).equals(methodSig)) {
                    return;
                }
            }

            methodNamesAndSigs.add(methodName);
            methodNamesAndSigs.add(methodSig);
        }
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected Hashtable instrClasses = new Hashtable();
    protected InstrumentationFilter instrFilter;
    protected byte[] codeBytes;
    protected boolean dontInstrumentEmptyMethods;
    protected boolean dontScanGetterSetterMethods;
    protected boolean instrumentSpawnedThreads;

    // This flag shows whether we have already instrumented the java.lang.reflect.Method.invoke() method to intercept all invocations.
    // The current policy is to instrument it eagerly, in the very first method instrumentation packet. However, the actual
    // interception can be turned on and off at run time on demand.
    protected boolean reflectInvokeInstrumented = false;
    protected int markerInjectionType; // Bytecode injections that are set dependent on the above
    protected int nInstrClasses;
    protected int nInstrMethods;
    protected int normalInjectionType; // Bytecode injections that are set dependent on the above
    protected int offset;
    protected int rootInjectionType; // Bytecode injections that are set dependent on the above
    RootMethods rootMethods;

    // remembered here because of profiling points
    private ProfilerEngineSettings engineSettings;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    RecursiveMethodInstrumentor(ProfilingSessionStatus status, ProfilerEngineSettings settings) {
        super(status);

        switch (status.currentInstrType) {
            case INSTR_RECURSIVE_FULL:
                normalInjectionType = INJ_RECURSIVE_NORMAL_METHOD;
                rootInjectionType = INJ_RECURSIVE_ROOT_METHOD;
                markerInjectionType = INJ_RECURSIVE_MARKER_METHOD;

                break;
            case INSTR_RECURSIVE_SAMPLED:
                normalInjectionType = INJ_RECURSIVE_SAMPLED_NORMAL_METHOD;
                rootInjectionType = INJ_RECURSIVE_SAMPLED_ROOT_METHOD;
                markerInjectionType = INJ_RECURSIVE_SAMPLED_MARKER_METHOD;

                break;
        }

        reflectInvokeInstrumented = false;

        dontScanGetterSetterMethods = !settings.getInstrumentGetterSetterMethods();
        dontInstrumentEmptyMethods = !settings.getInstrumentEmptyMethods();
        instrumentSpawnedThreads = settings.getInstrumentSpawnedThreads();
        instrFilter = settings.getInstrumentationFilter();
        engineSettings = settings;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * This method is called when some class containing an instrumentation root method is loaded by the VM (either has just
     * been loaded, or the JFluid server, upon the user's command to initiate instrumentation, has detected that it has been
     * loaded some time in the past). The JFluid server then sends a RootClassLoadedCommand to the tool. This command contains
     * the information on all classes currently loaded by the VM - see the details in this method's header.
     * This method should initialize instrumentation-related data structures, register given classes as loaded, and return
     * the initial set of methods to instrument in the format given by createInstrumentedMethodPack().
     */
    abstract Object[] getInitialMethodsToInstrument(String[] loadedClasses, int[] loadedClassLoaderIds,
                                                           byte[][] cachedClassFileBytes, RootMethods rootMethods);

    public abstract Object[] getMethodsToInstrumentUponClassLoad(String className, int classLoaderId, boolean threadInCallGraph);

    /** Methods below return method bytecodes to instrument upon specific events reported by the JFluid server. */
    public abstract Object[] getMethodsToInstrumentUponMethodInvocation(String className, int classLoaderId, String methodName,
                                                                        String methodSignature);

    public abstract Object[] getMethodsToInstrumentUponReflectInvoke(String className, int classLoaderId, String methodName,
                                                                     String methodSignature);

    /** Called every time before a new round of instrumentation, caused by class load, method invoke, etc. */
    protected void initInstrMethodData() {
        instrClasses.clear();
        nInstrClasses = nInstrMethods = 0;
    }

    protected static boolean rootClassNameIsReal(String rootClassName) {
        return !rootClassName.equals(NO_CLASS_NAME);
    }

    protected void addToSubclassList(DynamicClassInfo clazz, DynamicClassInfo addedClassInfo) {
        String superClassName = clazz.getSuperclassName();
        int loaderId = clazz.getLoaderId();
        DynamicClassInfo superClass = javaClassForName(superClassName, loaderId);
        clazz.setSuperClass(superClass);

        if ((superClass != null) && !clazz.isInterface()) {
            superClass.addSubclass(addedClassInfo);

            if (addedClassInfo != null) {
                findAndMarkOverridingMethodsReachable(superClass, addedClassInfo);
            }

            if (superClassName != "java/lang/Object") {
                addToSubclassList(superClass, addedClassInfo); // NOI18N
            }
        }

        String[] interfaces = clazz.getInterfaceNames();

        if (interfaces != null) {
            for (int i = 0; i < interfaces.length; i++) {
                DynamicClassInfo superInterface = javaClassForName(interfaces[i], loaderId);
                clazz.setSuperInterface(superInterface, i);

                if (superInterface != null) {
                    superInterface.addSubclass(addedClassInfo);

                    if (addedClassInfo != null) {
                        findAndMarkOverridingMethodsReachable(superInterface, addedClassInfo);
                    }

                    addToSubclassList(superInterface, addedClassInfo);
                }
            }
        }
    }

    protected abstract void findAndMarkOverridingMethodsReachable(DynamicClassInfo superClass, DynamicClassInfo subClass);

    protected abstract void processInvoke(DynamicClassInfo clazz, boolean virtualCall, int index);

    protected final int at(int index) {
        return codeBytes[offset + index] & 0xFF;
    }

    /**
     * Given the table at the specified index, return the specified entry
     */
    protected final long intAt(int tbl, int entry) {
        int base = tbl + (entry << 2);

        return (codeBytes[base] << 24) | ((codeBytes[base + 1] & 0xFF) << 16) | ((codeBytes[base + 2] & 0xFF) << 8)
               | (codeBytes[base + 3] & 0xFF);
    }

    protected void scanMethod(DynamicClassInfo clazz, int index) {
        byte[] bytecode = clazz.getMethodBytecode(index);
        scanBytecode(clazz, bytecode);
    }

    protected final int shortAt(int index) {
        int base = offset + index;

        return ((codeBytes[base] & 0xFF) << 8) | (codeBytes[base + 1] & 0xFF);
    }

    /**
     * This method is used either to normally process the bytecodes of a method, in which case clazz != null and the return
     * result is ignored. If clazz == null, then returns false upon encountering the first invoke bytecode, and true if there
     * are no invokes, i.e. it's a leaf method.
     */
    protected boolean scanBytecode(DynamicClassInfo clazz, byte[] code) {
        codeBytes = code;

        for (offset = 0; offset < codeBytes.length;) {
            int opcode = at(0);

            if (opcode == opc_wide) {
                opcode = at(1);

                if (((opcode >= opc_iload) && (opcode <= opc_aload)) || ((opcode >= opc_istore) && (opcode <= opc_astore))
                        || (opcode == opc_ret)) {
                    offset += 4;
                } else if (opcode == opc_iinc) {
                    offset += 6;
                } else {
                    offset++;
                }
            } else {
                switch (opcode) {
                    case opc_tableswitch: {
                        int tbl = (offset + 1 + 3) & (~3); // four byte boundry
                        long default_skip = intAt(tbl, 0);
                        long low = intAt(tbl, 1);
                        long high = intAt(tbl, 2);
                        tbl += (3 << 2); // three int header
                        offset = tbl + (int) ((high - low + 1) << 2);

                        break;
                    }
                    case opc_lookupswitch: {
                        int tbl = (offset + 1 + 3) & (~3); // four byte boundry
                        long default_skip = intAt(tbl, 0);
                        int npairs = (int) intAt(tbl, 1);
                        int nints = npairs * 2;
                        tbl += (2 << 2); // two int header
                        offset = tbl + (nints << 2);

                        break;
                    }
                    case opc_invokevirtual:
                    case opc_invokespecial:
                    case opc_invokestatic: {
                        if (clazz == null) {
                            return false; // Using scanBytecode() as a leaf-method checker
                        }

                        int index = shortAt(1);
                        processInvoke(clazz, (opcode == opc_invokevirtual), index);
                        offset += 3;

                        break;
                    }
                    case opc_invokeinterface: {
                        if (clazz == null) {
                            return false; // Using scanBytecode() as a leaf-method checker
                        }

                        int index = shortAt(1);
                        processInvoke(clazz, true, index);
                        offset += 5;

                        break;
                    }
                    default:
                        offset += opc_length[opcode];

                        break;
                }
            }
        }

        return true;
    }

    protected abstract boolean tryInstrumentSpawnedThreads(DynamicClassInfo clazz);

    protected static boolean isEmptyMethod(byte[] code) {
        return (code.length == 1); // Can't be anything but "return"
    }

    protected static boolean isGetterSetterMethod(byte[] code) {
        // Getter (accessor) method:
        // 0 aload_0; 1 getfield x ; 4 (i..a)return.  Parameter size = 1
        // Setter method:
        // 0 aload_0; 1 (i..a)load_1; 2 putfield x; 5 return. Parameter size = 2
        if (code.length == 5) {
            if (((code[0] & 0xFF) == opc_aload_0) && ((code[1] & 0xFF) == opc_getfield)
                    && (((code[4] & 0xFF) >= opc_ireturn) && ((code[4] & 0xFF) <= opc_areturn))) {
                return true;
            }
        } else if (code.length == 6) {
            if (((code[0] & 0xFF) == opc_aload_0) && (((code[1] & 0xFF) >= opc_iload_1) && ((code[1] & 0xFF) <= opc_aload_1))
                    && ((code[2] & 0xFF) == opc_putfield) && ((code[5] & 0xFF) == opc_return)) {
                return true;
            }
        }

        return false;
    }

    protected boolean isLeafMethod(byte[] code) {
        return scanBytecode(null, code);
    }

    /** Create a multi-class packet of instrumented methods or classes */
    protected Object[] createInstrumentedMethodPack() {
        if (nInstrMethods == 0) {
            return null;
        }

        return createInstrumentedMethodPack15();
    }

    protected void markAllMethodsMarker(DynamicClassInfo clazz) {
        clazz.setAllMethodsMarkers();
    }

    protected void markAllMethodsRoot(DynamicClassInfo clazz) {
        clazz.setAllMethodsRoots();
    }

    protected void markClassAndMethodForInstrumentation(DynamicClassInfo clazz, int methodIdx) {
        if ((status.getStartingMethodId() + nInstrMethods) < 65535) {
            String classNameAndLoader = clazz.getNameAndLoader();

            if (!instrClasses.containsKey(classNameAndLoader)) {
                instrClasses.put(classNameAndLoader, clazz);
                nInstrClasses++;
            }

            nInstrMethods++;
        } else { // Can't instrument more than 64K methods - mark this method as already instrumented
            clazz.setMethodInstrumented(methodIdx);
        }
    }

    protected boolean markMethod(DynamicClassInfo clazz, int rootMethod) {
        String rootMethodName = rootMethods.methodNames[rootMethod];
        String rootMethodSignature = rootMethods.methodSignatures[rootMethod];
        boolean isMarkerMethod = rootMethods.markerMethods[rootMethod];
        int rootMethodIdx = clazz.getMethodIndex(rootMethodName, rootMethodSignature);

        if (rootMethodIdx == -1) {
            return false;
        }

        if (isMarkerMethod) {
            clazz.setMethodMarker(rootMethodIdx);
        } else {
            clazz.setMethodRoot(rootMethodIdx);
        }

        return true;
    }

    protected boolean markMethodMarker(DynamicClassInfo clazz, String rootMethodName, String rootMethodSignature) {
        int rootMethodIdx = clazz.getMethodIndex(rootMethodName, rootMethodSignature);

        if (rootMethodIdx == -1) {
            return false;
        }

        clazz.setMethodMarker(rootMethodIdx);

        return true;
    }

    protected boolean markMethodRoot(DynamicClassInfo clazz, String rootMethodName, String rootMethodSignature) {
        int rootMethodIdx = clazz.getMethodIndex(rootMethodName, rootMethodSignature);

        if (rootMethodIdx == -1) {
            return false;
        }

        clazz.setMethodRoot(rootMethodIdx);

        return true;
    }

    //---------------------------- Private implementation of instrumentation data packing ---------------------------

    /** Create a multi-class packet of instrumented 1.5-style data */
    private Object[] createInstrumentedMethodPack15() {
        DynamicClassInfo reflectMethodClass = null;
        int reflectMethodClassIdx = -1;

        if (!reflectInvokeInstrumented) {
            // Check if java.lang.reflect.Method is already among classes to instrument
            int idx = 0;

            for (Enumeration e = instrClasses.elements(); e.hasMoreElements(); idx++) {
                DynamicClassInfo clazz = (DynamicClassInfo) e.nextElement();

                if (clazz.getName() == JAVA_LANG_REFLECT_METHOD_SLASHED_CLASS_NAME) {
                    reflectMethodClassIdx = idx;
                    reflectMethodClass = clazz;
                    break;
                }
            }
            if (reflectMethodClassIdx == -1) {
                reflectMethodClass = javaClassForName(JAVA_LANG_REFLECT_METHOD_DOTTED_CLASS_NAME, 0);
                if (reflectMethodClass != null) {
                    nInstrClasses++;
                }
            }
            if (reflectMethodClass == null) {
                reflectInvokeInstrumented = true;
            }
        }

        String[] instrMethodClasses = new String[nInstrClasses];
        int[] instrClassLoaderIds = new int[nInstrClasses];
        boolean[] instrMethodLeaf = new boolean[nInstrMethods];
        byte[][] replacementClassFileBytes = new byte[nInstrClasses][];
        int methodId = status.getStartingMethodId();
        int classIdx = 0;
        int methodIdx = 0;

        for (Enumeration e = instrClasses.elements(); e.hasMoreElements();) {
            DynamicClassInfo clazz = (DynamicClassInfo) e.nextElement();
            int nMethods = clazz.getMethodNames().length;
            instrMethodClasses[classIdx] = clazz.getName().replace('/', '.').intern(); // NOI18N
            instrClassLoaderIds[classIdx] = clazz.getLoaderId();

            boolean hasRootMethods = clazz.hasUninstrumentedRootMethods();
            boolean hasMarkerMethods = clazz.hasUninstrumentedMarkerMethods();
            DynamicConstantPoolExtension.getCPFragment(clazz, normalInjectionType);

            if (hasRootMethods) {
                DynamicConstantPoolExtension.getCPFragment(clazz, rootInjectionType);
            }

            if (hasMarkerMethods) {
                DynamicConstantPoolExtension.getCPFragment(clazz, markerInjectionType);
            }

            int imInClass = 0;
            byte[][] replacementMethodInfos = new byte[nMethods][];
            RuntimeProfilingPoint[] pointsForClass = getRuntimeProfilingPoints(engineSettings.getRuntimeProfilingPoints(), clazz);

            //System.err.println("CLazz: "+clazz.getName());
            for (int i = 0; i < nMethods; i++) {
                // FIXME: issue 68840: An overriden method overriding with subclass of return type is instrumented twice
                // http://profiler.netbeans.org/issues/show_bug.cgi?id=68840
                // a method whose return type is not exact match as the method which it implements/overrides would be listed
                // and processed twice, leading to double instrumentation

                //System.err.println("Method: "+clazz.getMethodName(i)+" " + clazz.getMethodSignature(i));
                RuntimeProfilingPoint[] points = getRuntimeProfilingPoints(pointsForClass, i);

                if (!clazz.isMethodInstrumented(i)) {
                    if (clazz.isMethodReachable(i) && !clazz.isMethodUnscannable(i)) {
                        clazz.setMethodInstrumented(i);
                        instrMethodLeaf[methodIdx] = clazz.isMethodLeaf(i);
                        //System.err.println(">>>1 For method " + clazz.getName() + "." + clazz.getMethodName(i) + clazz.getMethodSignature(i) + " gonna use methodId = " + methodId);
                        replacementMethodInfos[i] = InstrumentationFactory.instrumentMethod(clazz, i, normalInjectionType,
                                                                                            rootInjectionType,
                                                                                            markerInjectionType, methodId++,
                                                                                            points);
                        clazz.saveMethodInfo(i, replacementMethodInfos[i]);

                        status.updateInstrMethodsInfo(instrMethodClasses[classIdx], instrClassLoaderIds[classIdx],
                                                      clazz.getMethodNames()[i], clazz.getMethodSignatures()[i]);
                        imInClass++;
                        methodIdx++;
                    } else if (points.length > 0) {
                        replacementMethodInfos[i] = InstrumentationFactory.instrumentAsProiflePointHitMethod(clazz, i,
                                                                                                             normalInjectionType,
                                                                                                             points);
                        clazz.saveMethodInfo(i, replacementMethodInfos[i]);
                    }
                } else {
                    replacementMethodInfos[i] = clazz.getMethodInfo(i); // Will return the previously instrumented methodInfo
                    imInClass++;
                }
            }

            instrumentServletDoMethods(clazz, replacementMethodInfos);

            if (imInClass > 0) {
                if (hasRootMethods) {
                    clazz.setHasUninstrumentedRootMethods(false);
                }

                if (hasMarkerMethods) {
                    clazz.setHasUninstrumentedMarkerMethods(false);
                }

                DynamicConstantPoolExtension wholeECP = DynamicConstantPoolExtension.getAllAddedCPFragments(clazz);
                int nAddedCPEntries = wholeECP.getNEntries();
                byte[] addedCPContents = wholeECP.getContents();
                replacementClassFileBytes[classIdx] = ClassRewriter.rewriteClassFile(clazz, replacementMethodInfos,
                                                                                     nAddedCPEntries, addedCPContents);
            }
            classIdx++;
        }

        if (!reflectInvokeInstrumented) { // Special instrumentation of java.lang.reflect.Method.invoke()
            int nMethods = reflectMethodClass.getMethodNames().length;
            byte[][] replacementMethodInfos = new byte[nMethods][];

            if (reflectMethodClassIdx == -1) {
                instrMethodClasses[classIdx] = JAVA_LANG_REFLECT_METHOD_DOTTED_CLASS_NAME;
                instrClassLoaderIds[classIdx] = 0;
            } else {
                classIdx = reflectMethodClassIdx;

                for (int i = 0; i < nMethods; i++) {
                    replacementMethodInfos[i] = reflectMethodClass.getMethodInfo(i);
                }
            }

            int idx = reflectMethodClass.getMethodIndex(INVOKE_METHOD_NAME, INVOKE_METHOD_SIGNATURE);
            DynamicConstantPoolExtension.getCPFragment(reflectMethodClass, INJ_REFLECT_METHOD_INVOKE);

            replacementMethodInfos[idx] = InstrumentationFactory.instrumentAsReflectInvokeMethod(reflectMethodClass, idx);

            DynamicConstantPoolExtension wholeECP = DynamicConstantPoolExtension.getAllAddedCPFragments(reflectMethodClass);
            int nAddedCPEntries = wholeECP.getNEntries();
            byte[] addedCPContents = wholeECP.getContents();
            replacementClassFileBytes[classIdx] = ClassRewriter.rewriteClassFile(reflectMethodClass, replacementMethodInfos, nAddedCPEntries,
                                                                                 addedCPContents);

            reflectMethodClass.saveMethodInfo(idx, replacementMethodInfos[idx]);
            reflectInvokeInstrumented = true;
        }

        return new Object[] { instrMethodClasses, instrClassLoaderIds, instrMethodLeaf, replacementClassFileBytes };
    }

    private void instrumentServletDoMethods(DynamicClassInfo clazz, byte[][] replacementMethodInfos) {
        if (!Boolean.getBoolean("org.netbeans.lib.profiler.servletTracking")) { // NOI18N
            return;
        }

        if (clazz.isServletDoMethodScanned()) {
            return;
        }

        clazz.setServletDoMethodScanned();

        if (!clazz.isSubclassOf(HandleServletDoMethodCallInjector.getClassName())) {
            return;
        }

        DynamicConstantPoolExtension.getCPFragment(clazz, INJ_SERVLET_DO_METHOD);

        String[] methods = HandleServletDoMethodCallInjector.getMethodNames();
        String[] sigs = HandleServletDoMethodCallInjector.getMethodSignatures();

        for (int i = 0; i < methods.length; i++) {
            int midx = clazz.getMethodIndex(methods[i], sigs[i]);

            if (midx != -1) {
                replacementMethodInfos[midx] = InstrumentationFactory.instrumentAsServletDoMethod(clazz, midx);
                clazz.saveMethodInfo(midx, replacementMethodInfos[midx]);
            }
        }
    }
}
