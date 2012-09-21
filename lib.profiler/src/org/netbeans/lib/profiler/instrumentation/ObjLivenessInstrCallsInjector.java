/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
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

import java.io.IOException;
import org.netbeans.lib.profiler.classfile.BaseClassInfo;
import org.netbeans.lib.profiler.classfile.ClassRepository;
import org.netbeans.lib.profiler.classfile.DynamicClassInfo;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.utils.MiscUtils;


/**
 * Specialized subclass of Injector, that provides injection of our object allocation and liveness instrumentation -
 * ProfilerRuntimeObjAlloc/ProfilerRuntimeObjLiveness.traceObjAlloc(Object obj, char classId) call
 * after each "new", "anewarray" or "newarray" bytecode.
 *
 * @author Tomas Hurka
 * @author Misha Dmitriev
 */
class ObjLivenessInstrCallsInjector extends Injector implements CommonConstants {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // Stuff used for traceObjAlloc(Object, char)
    protected static byte[] injectedCode;
    protected static int injectedCodeLen;
    protected static int injectedCodeMethodIdxPos;
    protected static int injectedCodeClassIdPos;

    static {
        initializeInjectedCode();
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected boolean[] allUnprofiledClassStatusArray;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public ObjLivenessInstrCallsInjector(DynamicClassInfo clazz, int baseCPoolCount, int methodIdx,
                                         boolean[] allUnprofiledClassStatusArray) {
        super(clazz, methodIdx);
        this.baseCPoolCount = baseCPoolCount;
        this.allUnprofiledClassStatusArray = allUnprofiledClassStatusArray;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public byte[] instrumentMethod() {
        int loaderId = clazz.getLoaderId();
        int bc;
        int bci = 0;
        int nInjections = 0;

        // Instrument all opc_new, opc_anewarray and opc_newarray instructions, for which allUnprofiledClassStatusArray[classId] != true
        int opcNewCount = 0;

        // Instrument all opc_new, opc_anewarray and opc_newarray instructions, for which allUnprofiledClassStatusArray[classId] != true
        int opcNewToInstr = 0;

        do {
            opcNewToInstr = opcNewCount + 1;
            bci = 0;

            while (bci < bytecodesLength && bytecodesLength + injectedCodeLen < 65535) {
                bc = (bytecodes[bci] & 0xFF);

                if ((bc == opc_new) || (bc == opc_anewarray) || (bc == opc_newarray) || (bc == opc_multianewarray)) {
                    opcNewToInstr--;

                    if (opcNewToInstr == 0) {
                        opcNewCount++;

                        BaseClassInfo refClazz;

                        if ((bc == opc_new) || (bc == opc_anewarray) || (bc == opc_multianewarray)) {
                            int classCPIdx = getU2(bci + 1);
                            String refClassName = clazz.getRefClassName(classCPIdx);

                            if (bc == opc_new) {
                                refClazz = ClassManager.javaClassOrPlaceholderForName(refClassName, loaderId);
                            } else if (bc == opc_anewarray) {
                                refClazz = ClassManager.javaClassForObjectArrayType(refClassName);
                            } else {
                                refClazz = ClassRepository.lookupSpecialClass(refClassName);
                            }

                            if (refClazz == null) {
                                continue; // Warning already issued
                            }

                            int classId = refClazz.getInstrClassId();

                            if ((allUnprofiledClassStatusArray != null) && (allUnprofiledClassStatusArray.length > classId)
                                    && allUnprofiledClassStatusArray[classId]) {
                                continue;
                            }

                            if ((bc == opc_anewarray) || (bc == opc_multianewarray)) { // Simply inject the call after the bytecode instruction
                                injectTraceObjAlloc(classId, bci + opcodeLength(bci));
                                nInjections++;
                            } else { // opc_new - we can only inject the call after the corresponding constructor call
                                bci += opcodeLength(bci);
                                bc = (bytecodes[bci] & 0xFF);

                                if ((bc != opc_dup) && (bc != opc_dup_x1) && (bc != opc_dup_x2)) {
                                    // see issue http://www.netbeans.org/issues/show_bug.cgi?id=59085
                                    // the JBoss JSP compiler generates bytecode that uses opc_dup_x1 in some cases,
                                    // something javac would not generate
                                    // in this case, injecting extra dup would corrupt the stack, so we cannot perform it
                                    // same is expected for opc_dup_x2

                                    // No standard 'dup' after 'new'. Can happen if there is a line like 'new Foo()', with no assignment of reference to the new object.
                                    // This seems to be a rare case - javac apparently always adds 'dup' to 'new' (it would add 'pop' after it if the object is not used).
                                    // We assume that if there is no 'dup' directly after 'new', there is also no 'dup' for this same object later.

                                    //System.err.println("*** Gonna inject dup at bci = " + bci + " in method = " + clazz.getName() + "." + clazz.getMethodName(methodIdx) + " , idx = " + methodIdx);
                                    injectDup(bci);

                                    //System.out.println("*** For " + clazz.getName() + "." + clazz.getMethodName(methodIdx) + " gonna locateConstructor from bci = " + bci);
                                    bci = locateConstructorCallForNewOp(bci, bytecodesLength, refClassName);

                                    // [fixme]
                                    //
                                    // unfortunately deinjecting the dump here is not straightforward if bci = -1
                                    // as an indication of failure to figure out the correct constructor call
                                    // So far this is not happening, the issue that happens with Hibernate goes through the other branch
                                    // without injecting dup
                                    // see http://www.netbeans.org/issues/show_bug.cgi?id=67346
                                    injectTraceObjAllocNoDup(classId, bci);
                                    nInjections++;
                                } else {
                                    bci = locateConstructorCallForNewOp(bci, bytecodesLength, refClassName);

                                    if (bci != -1) {
                                        injectTraceObjAlloc(classId, bci);
                                        nInjections++;
                                    }
                                }
                            }
                        } else { // opc_newarray - primitive array allocation

                            int arrayClassId = getByte(bci + 1);
                            refClazz = ClassManager.javaClassForPrimitiveArrayType(arrayClassId);

                            int classId = refClazz.getInstrClassId();

                            if ((allUnprofiledClassStatusArray == null) || !allUnprofiledClassStatusArray[classId]) {
                                injectTraceObjAlloc(classId, bci + 2);
                                nInjections++;
                            }
                        }

                        break;
                    }
                }

                bci += opcodeLength(bci);
            }
        } while (opcNewToInstr == 0);
        if (bci < bytecodesLength) {
            // method was not fully instrumented -> issue warnining
            String methodFQN = clazz.getName()+"."+clazz.getMethodName(methodIdx)+clazz.getMethodSignature(methodIdx);  // NOI18N
            MiscUtils.printWarningMessage("Method "+methodFQN+" is too big to be fully instrumented.");  // NOI18N
        }

        if (nInjections == 0) {
            ((DynamicClassInfo) clazz).unsetMethodInstrumented(methodIdx);
        } else {
            // Done very conservatively.
            maxStack += 2;
        }

        return createPackedMethodInfo();
    }

    private static void initializeInjectedCode() {
        // Code packet for traceObjAlloc(Object obj, char classId)
        injectedCodeLen = 8;
        injectedCode = new byte[injectedCodeLen];
        injectedCode[0] = (byte) opc_dup; // push newly created object to top of stack to pass as the first argument to traceObjAlloc method
        injectedCode[1] = (byte) opc_sipush; // push char - the actual value will be next two bytes, a second parameter passed to traceObjAlloc method
                                             // Positions 2, 3 are occupied by classId

        injectedCodeClassIdPos = 2;
        injectedCode[4] = (byte) opc_invokestatic;
        // Positions 5, 6 are occupied by method index
        injectedCodeMethodIdxPos = 5;
        injectedCode[7] = (byte) opc_nop;
    }

    private void injectDup(int bci) {
        byte[] injCode = new byte[] { (byte) opc_dup, (byte) opc_nop, (byte) opc_nop, (byte) opc_nop };
        injectCodeAndRewrite(injCode, 4, bci, false);
    }

    private void injectTraceObjAlloc(int classId, int bci) {
        // Prepare the traceObjAlloc(Object obj, char classId) code packet that is to be injected
        int targetMethodIdx = CPExtensionsRepository.memoryProfContents_TraceObjAllocMethodIdx + baseCPoolCount;
        putU2(injectedCode, injectedCodeMethodIdxPos, targetMethodIdx);
        putU2(injectedCode, injectedCodeClassIdPos, classId);

        injectCodeAndRewrite(injectedCode, injectedCodeLen, bci, false);
    }

    private void injectTraceObjAllocNoDup(int classId, int bci) {
        injectedCode[0] = (byte) opc_nop; // Remove the dup
                                          // Prepare the traceObjAlloc(Object obj, char classId) code packet that is to be injected

        int targetMethodIdx = CPExtensionsRepository.memoryProfContents_TraceObjAllocMethodIdx + baseCPoolCount;
        putU2(injectedCode, injectedCodeMethodIdxPos, targetMethodIdx);
        putU2(injectedCode, injectedCodeClassIdPos, classId);

        injectCodeAndRewrite(injectedCode, injectedCodeLen, bci, false);
        injectedCode[0] = (byte) opc_dup; // Restore dup
    }

    private int locateConstructorCallForNewOp(int startBCI, int bytecodesLength, String newOpClassName) {
        int bc;
        int bci = startBCI;
        int nestedNewOps = 0;
        
        while (bci < bytecodesLength) {
            bc = bytecodes[bci] & 0xFF;
            if (bc == opc_new) {
                nestedNewOps++;
            } else if (bc == opc_invokespecial) {
                int index = getU2(bci + 1);
                String[] cms = clazz.getRefMethodsClassNameAndSig(index);

                if (cms == null) {
                    System.err.println("Failed to locate constant pool ref in: " + clazz.getName()); // NOI18N
                    System.err.println("new Op class: " + newOpClassName); // NOI18N
                    System.err.println("bci: " + bci + ", startBCI: " + startBCI); // NOI18N
                    System.err.println("constant pool ref index: " + index); // NOI18N
                    dumpClassFile();
                    //debug = true;
                    return -1;
                }

                String refClassName = cms[0];
                String refMethodName = cms[1];

                if (refMethodName == "<init>") { // NOI18N  // It's really a constructor call, not e.g. a call to a private method of 'this'
                    if (nestedNewOps == 0) {
                        bci += opcodeLength(bci);
                        return bci;
                    } else {
                        nestedNewOps--;
                    }
                }
            }
            bci += opcodeLength(bci);
        }

        System.err.println("Profiler Warning: Failed to instrument creation of class " + newOpClassName // NOI18N
                           + " in method " + clazz.getName() + "." + clazz.getMethodName(methodIdx)); // NOI18N
        dumpClassFile();
        return -1; // not instrumentable, there is no call to constructor
    }

    private void dumpClassFile() {
        try {
            ClassRewriter.saveToDisk(clazz.getName(), ((DynamicClassInfo) clazz).getClassFileBytes());
        } catch (IOException e) {
            System.err.println("Caught exception while dumping class: " + clazz.getName() + ", " + e.getMessage()); // NOI18N
            e.printStackTrace(System.err);
        }
    }
}
