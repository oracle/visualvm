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
import org.netbeans.lib.profiler.classfile.ClassInfo;
import org.netbeans.lib.profiler.classfile.ClassRepository;
import org.netbeans.lib.profiler.classfile.DynamicClassInfo;
import org.netbeans.lib.profiler.global.ProfilingSessionStatus;
import java.util.Enumeration;


/**
 * High-level access to functionality that instruments TA methods to generate object allocation and liveness data.
 * Also has support for removing instrumentation for a subset of classes that the user deemed
 * "not interesting".
 *
 * @author Misha Dmitriev
 */
public class ObjLivenessMethodInstrumentor extends MemoryProfMethodInstrumentor {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    static class MethodScanerForBannedInstantiations extends SingleMethodScaner {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        protected boolean[] unprofiledClassStatusArray;

        //~ Methods --------------------------------------------------------------------------------------------------------------

        void setUnprofiledClassStatusArray(boolean[] v) {
            unprofiledClassStatusArray = v;
        }

        boolean hasNewOpcodes() {
            int loaderId = clazz.getLoaderId();
            int bc;
            int bci = 0;

            while (bci < bytecodesLength) {
                bc = (bytecodes[bci] & 0xFF);

                if ((bc == opc_new) || (bc == opc_anewarray) || (bc == opc_multianewarray)) {
                    int classCPIdx = getU2(bci + 1);
                    String refClassName = clazz.getRefClassName(classCPIdx);
                    BaseClassInfo refClazz;

                    if (bc == opc_new) {
                        refClazz = ClassManager.javaClassOrPlaceholderForName(refClassName, loaderId);
                    } else if (bc == opc_anewarray) {
                        refClazz = ClassManager.javaClassForObjectArrayType(refClassName);
                    } else {
                        refClazz = ClassRepository.lookupSpecialClass(refClassName);
                    }

                    int classId = refClazz.getInstrClassId();

                    if (classId != -1) {
                        if ((unprofiledClassStatusArray.length > classId) && unprofiledClassStatusArray[classId]) {
                            return true;
                        }
                    }
                } else if (bc == opc_newarray) {
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

    protected MethodScanerForBannedInstantiations msbi;
    protected boolean[] allUnprofiledClassStatusArray;
    protected int operationCode; // Depending on this value, use different methods to mark/determine if a method needs rewriting
    private final ProfilerEngineSettings engineSettings;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public ObjLivenessMethodInstrumentor(ProfilingSessionStatus status, ProfilerEngineSettings engineSettings, boolean isLiveness) {
        super(status, isLiveness ? INJ_OBJECT_LIVENESS : INJ_OBJECT_ALLOCATIONS);
        this.engineSettings = engineSettings;
        msbi = new MethodScanerForBannedInstantiations();
        operationCode = STANDARD_INSTRUMENTATION;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * This is called when object allocation profiling is already active, with the argument where each line
     * corresponds to a class id, and value true at this line means that the class should not be profiled anymore
     * (i.e. allocation info for its instances shouldn't be generated). Returns the methods from which the
     * instrumentation for unprofiled classes is removed (but for others it's still in place).
     */
    public Object[] getMethodsToInstrumentUponClassUnprofiling(boolean[] unprofiledClassStatusArray) {
        operationCode = SELECTIVE_INSTR_REMOVAL;
        initInstrumentationPackData();
        msbi.setUnprofiledClassStatusArray(unprofiledClassStatusArray);
        setAllUnprofiledClassStatusArray(unprofiledClassStatusArray);

        for (Enumeration e = ClassRepository.getClassEnumerationWithAllVersions(); e.hasMoreElements();) {
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

                if (msbi.hasNewOpcodes()) {
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
        return InstrumentationFactory.instrumentForMemoryProfiling(clazz, methodIdx, allUnprofiledClassStatusArray, injType,
                                                                   getRuntimeProfilingPoints(engineSettings.getRuntimeProfilingPoints(),
                                                                                             clazz, methodIdx));
    }

    protected boolean methodNeedsInstrumentation(ClassInfo clazz, int methodIdx) {
        // TODO: hasNewOpcodes must be called in any case, because it has side effects!
        boolean ni = hasNewOpcodes(clazz, methodIdx, true);

        return ni || (getRuntimeProfilingPoints(engineSettings.getRuntimeProfilingPoints(), clazz, methodIdx).length > 0);
    }

    protected boolean methodNeedsRewriting(DynamicClassInfo clazz, int methodIdx) {
        if (operationCode == STANDARD_INSTRUMENTATION) {
            return clazz.isMethodInstrumented(methodIdx);
        } else { // SELECTIVE_INSTR_REMOVAL

            boolean res = clazz.isMethodSpecial(methodIdx);
            clazz.unsetMethodSpecial(methodIdx);

            return res;
        }
    }
}
