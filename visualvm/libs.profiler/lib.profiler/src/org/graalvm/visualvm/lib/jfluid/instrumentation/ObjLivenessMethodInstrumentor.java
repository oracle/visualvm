/*
 * Copyright (c) 1997, 2023, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Enumeration;
import org.graalvm.visualvm.lib.jfluid.ProfilerEngineSettings;
import org.graalvm.visualvm.lib.jfluid.classfile.BaseClassInfo;
import org.graalvm.visualvm.lib.jfluid.classfile.ClassInfo;
import org.graalvm.visualvm.lib.jfluid.classfile.ClassRepository;
import org.graalvm.visualvm.lib.jfluid.classfile.DynamicClassInfo;
import org.graalvm.visualvm.lib.jfluid.filters.InstrumentationFilter;
import org.graalvm.visualvm.lib.jfluid.global.ProfilingSessionStatus;


/**
 * High-level access to functionality that instruments TA methods to generate object allocation and liveness data.
 * Also has support for removing instrumentation for a subset of classes that the user deemed
 * "not interesting".
 *
 * @author Misha Dmitriev
 */
public class ObjLivenessMethodInstrumentor extends MemoryProfMethodInstrumentor {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    class MethodScanerForBannedInstantiations extends SingleMethodScaner {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        protected boolean[] unprofiledClassStatusArray;

        //~ Methods --------------------------------------------------------------------------------------------------------------

        void setUnprofiledClassStatusArray(boolean[] v) {
            unprofiledClassStatusArray = v;
        }

        boolean hasNewOpcodes(boolean checkForOpcNew, boolean checkForOpcNewArray) {
            if (!checkForOpcNew && !checkForOpcNewArray) return false;
            int loaderId = clazz.getLoaderId();
            int bc;
            int bci = 0;

            while (bci < bytecodesLength) {
                bc = (bytecodes[bci] & 0xFF);

                if ((bc == opc_new && checkForOpcNew) || 
                   ((bc == opc_anewarray || (bc == opc_multianewarray) && checkForOpcNewArray))) {
                    int classCPIdx = getU2(bci + 1);
                    String refClassName = clazz.getRefClassName(classCPIdx);
                    BaseClassInfo refClazz;

                    if (bc == opc_new) {
                        refClazz = javaClassOrPlaceholderForName(refClassName, loaderId);
                    } else if (bc == opc_anewarray) {
                        refClazz = javaClassForObjectArrayType(refClassName);
                    } else {
                        refClazz = lookupSpecialClass(refClassName);
                    }

                    int classId = refClazz.getInstrClassId();

                    if (classId != -1) {
                        if ((unprofiledClassStatusArray.length > classId) && unprofiledClassStatusArray[classId]) {
                            return true;
                        }
                    }
                } else if (bc == opc_newarray && checkForOpcNewArray) {
                    int arrayClassId = getByte(bci + 1);
                    BaseClassInfo refClazz = javaClassForPrimitiveArrayType(arrayClassId);
                    int classId = refClazz.getInstrClassId();

                    if (classId != -1) {
                        if (unprofiledClassStatusArray[classId]) {
                            return true;
                        }
                    }
                }

                bci += opcodeLength(bci);
            }

            return false;
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // Values that operationCode can have
    protected static final int STANDARD_INSTRUMENTATION = 1;
    protected static final int SELECTIVE_INSTR_REMOVAL = 2;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected boolean[] allUnprofiledClassStatusArray;
    protected int operationCode; // Depending on this value, use different methods to mark/determine if a method needs rewriting
    private final ProfilerEngineSettings engineSettings;
    private final InstrumentationFilter instrFilter;
    private final boolean instrObjectInit;
    private final boolean instrArr;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public ObjLivenessMethodInstrumentor(ClassRepository repo, ProfilingSessionStatus status, ProfilerEngineSettings engineSettings, boolean isLiveness) {
        super(repo, status, isLiveness ? INJ_OBJECT_LIVENESS : INJ_OBJECT_ALLOCATIONS);
        this.engineSettings = engineSettings;
        instrFilter = engineSettings.getInstrumentationFilter();
        operationCode = STANDARD_INSTRUMENTATION;
        instrObjectInit = engineSettings.isInstrumentObjectInit();
        instrArr = engineSettings.isInstrumentArrayAllocation();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * This is called when object allocation profiling is already active, with the argument where each line
     * corresponds to a class id, and value true at this line means that the class should not be profiled anymore
     * (i.e. allocation info for its instances shouldn't be generated). Returns the methods from which the
     * instrumentation for unprofiled classes is removed (but for others it's still in place).
     */
    public Object[] getMethodsToInstrumentUponClassUnprofiling(boolean[] unprofiledClassStatusArray) {
        MethodScanerForBannedInstantiations msbi;
        
        operationCode = SELECTIVE_INSTR_REMOVAL;
        initInstrumentationPackData();
        msbi = new MethodScanerForBannedInstantiations();
        msbi.setUnprofiledClassStatusArray(unprofiledClassStatusArray);
        setAllUnprofiledClassStatusArray(unprofiledClassStatusArray);

        for (Enumeration e = getClassEnumerationWithAllVersions(); e.hasMoreElements();) {
            Object ci = e.nextElement();

            if (!(ci instanceof DynamicClassInfo)) {
                continue; // It's a BaseClassInfo, created just for e.g. array classes, or a PlaceholderClassInfo
            }

            DynamicClassInfo clazz = (DynamicClassInfo) ci;

            if (!clazz.isLoaded()) {
                continue;
            }

            if (!clazz.hasInstrumentedMethods()) {
                continue;
            }

            String[] methodNames = clazz.getMethodNames();
            boolean found = false;

            for (int i = 0; i < methodNames.length; i++) {
                if (!clazz.isMethodInstrumented(i)) {
                    continue;
                }

                msbi.setClassAndMethod(clazz, i);

                if (msbi.hasNewOpcodes(!instrObjectInit, instrArr)) {
                    found = true;
                    clazz.setMethodSpecial(i);
                    nInstrMethods++;
                }
            }

            if (found) {
                nInstrClasses++;
                instrClasses.add(clazz);
            }
        }

        Object[] res = createInstrumentedMethodPack();
        operationCode = STANDARD_INSTRUMENTATION;

        return res;
    }

    /**
     * Every time the user tells the tool to remove profiling for some class(es), the info about only
     * those classes currently selected for unprofiling, rather than about all classes ever unprofiled,
     * is recorded and then passed here via currentUnprofiledClassStatusArray[]. This is done to simplify
     * scanning instrumented methods for 'new' instructions to unprofile, so that instructions that
     * have already been unprofiled before don't cause false positives. However, when rewriting methods
     * that may already contain some unprofiled instantiations (to unprofile more instantiations), or
     * rewriting fresh uninstrumented methods, we need to use cumulative info about all unprofilings.
     * That info is collected here, in allUnprofiledClassStatusArray.
     */
    protected void setAllUnprofiledClassStatusArray(boolean[] currentUnprofiledClassStatusArray) {
        int len = currentUnprofiledClassStatusArray.length;

        if ((allUnprofiledClassStatusArray == null) || (allUnprofiledClassStatusArray.length < len)) {
            boolean[] old = allUnprofiledClassStatusArray;
            allUnprofiledClassStatusArray = new boolean[len];

            if (old != null) {
                System.arraycopy(old, 0, allUnprofiledClassStatusArray, 0, old.length);
            }
        }

        for (int i = 0; i < len; i++) {
            if (currentUnprofiledClassStatusArray[i]) {
                allUnprofiledClassStatusArray[i] = true;
            }
        }
    }

    protected byte[] instrumentMethod(DynamicClassInfo clazz, int methodIdx) {
        return InstrumentationFactory.instrumentForMemoryProfiling(this, clazz, methodIdx, allUnprofiledClassStatusArray, injType,
                                             getRuntimeProfilingPoints(engineSettings.getRuntimeProfilingPoints(),clazz, methodIdx),
                                             instrFilter, !instrObjectInit, instrArr);
    }

    protected boolean classNeedsInstrumentation(ClassInfo clazz) {
        if (clazz == null) {
            return false;
        }
        if (!instrObjectInit || instrArr) {
            return true;
        }
        if (instrObjectInit && OBJECT_SLASHED_CLASS_NAME.equals(clazz.getName()) && clazz.getLoaderId() <= 0) {
            return true;
        }
        return false;
    }

    protected boolean methodNeedsInstrumentation(ClassInfo clazz, int methodIdx) {
        // TODO: hasNewOpcodes must be called in any case, because it has side effects!
        boolean ni = hasNewOpcodes(clazz, methodIdx, !instrObjectInit, instrArr, instrFilter);
        boolean pp = getRuntimeProfilingPoints(engineSettings.getRuntimeProfilingPoints(), clazz, methodIdx).length > 0;
        boolean oi = instrObjectInit && isObjectConstructor(clazz, methodIdx);
        return ni || pp || oi;
    }

    @Override
    protected boolean methodNeedsRewriting(DynamicClassInfo clazz, int methodIdx) {
        if (operationCode == STANDARD_INSTRUMENTATION) {
            return clazz.isMethodInstrumented(methodIdx);
        } else { // SELECTIVE_INSTR_REMOVAL

            boolean res = clazz.isMethodSpecial(methodIdx);
            clazz.unsetMethodSpecial(methodIdx);

            return res;
        }
    }
    
    static boolean isObjectConstructor(ClassInfo clazz, int methodIdx) {
        if (OBJECT_SLASHED_CLASS_NAME.equals(clazz.getName())
                && clazz.getLoaderId() <= 0
                && "<init>".equals(clazz.getMethodName(methodIdx))) {   // NOI18N
            return true;
        }
        return false;
    }
}
