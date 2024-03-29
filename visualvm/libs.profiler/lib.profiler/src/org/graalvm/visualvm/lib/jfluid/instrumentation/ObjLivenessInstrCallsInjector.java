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

import java.io.IOException;
import org.graalvm.visualvm.lib.jfluid.classfile.BaseClassInfo;
import org.graalvm.visualvm.lib.jfluid.classfile.DynamicClassInfo;
import org.graalvm.visualvm.lib.jfluid.filters.InstrumentationFilter;
import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;
import org.graalvm.visualvm.lib.jfluid.utils.MiscUtils;
import org.graalvm.visualvm.lib.jfluid.utils.StringUtils;
import org.graalvm.visualvm.lib.jfluid.utils.VMUtils;


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
    private final InstrumentationFilter instrFilter;
    private final boolean checkForOpcNew;
    private final boolean checkForOpcNewArray;
    private final ClassManager classManager;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    ObjLivenessInstrCallsInjector(ClassManager manager, DynamicClassInfo clazz, 
                                  int baseCPoolCount, int methodIdx,
                                  boolean[] allUnprofiledClassStatusArray,
                                  InstrumentationFilter instrFilter, boolean checkForOpcNew,
                                  boolean checkForOpcNewArray) {
        super(clazz, methodIdx);
        this.baseCPoolCount = baseCPoolCount;
        this.allUnprofiledClassStatusArray = allUnprofiledClassStatusArray;
        this.instrFilter = instrFilter;
        this.checkForOpcNew = checkForOpcNew;
        this.checkForOpcNewArray = checkForOpcNewArray;
        classManager = manager;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public byte[] instrumentMethod() {
        int bci = 0;
        int nInjections = 0;

        if (ObjLivenessMethodInstrumentor.isObjectConstructor(clazz, methodIdx)) {
            injectTraceObjAllocObjCtor(bci);
            nInjections++;
        } else {
            int loaderId = clazz.getLoaderId();
            int bc;

            // Instrument all opc_new, opc_anewarray and opc_newarray instructions, for which allUnprofiledClassStatusArray[classId] != true
            int opcNewCount = 0;

            // Instrument all opc_new, opc_anewarray and opc_newarray instructions, for which allUnprofiledClassStatusArray[classId] != true
            int opcNewToInstr = 0;

            do {
                opcNewToInstr = opcNewCount + 1;
                bci = 0;

                while (bci < bytecodesLength && bytecodesLength + injectedCodeLen < 65535) {
                    bc = (bytecodes[bci] & 0xFF);

                    if ((bc == opc_new && checkForOpcNew) || (checkForOpcNewArray && (bc == opc_anewarray || bc == opc_newarray || bc == opc_multianewarray))) {
                        opcNewToInstr--;

                        if (opcNewToInstr == 0) {
                            opcNewCount++;

                            BaseClassInfo refClazz;

                            if ((bc == opc_new) || (bc == opc_anewarray) || (bc == opc_multianewarray)) {
                                int classCPIdx = getU2(bci + 1);
                                String refClassName = clazz.getRefClassName(classCPIdx);

                                if (bc == opc_new) {
                                    if (!instrFilter.passes(refClassName)) {
                                        break;
                                    }
                                    refClazz = classManager.javaClassOrPlaceholderForName(refClassName, loaderId);
                                } else if (bc == opc_anewarray) {
                                    if (!instrFilter.passes(refClassName.concat("[]"))) {    // NOI18N
                                        break;
                                    }
                                    refClazz = classManager.javaClassForObjectArrayType(refClassName);
                                } else {
                                    if (!instrFilter.passes(getMultiArrayClassName(refClassName))) {
                                        break;
                                    }
                                    refClazz = classManager.lookupSpecialClass(refClassName);
                                }

                                if (refClazz == null) {
                                    break; // Warning already issued
                                }

                                int classId = refClazz.getInstrClassId();

                                if ((allUnprofiledClassStatusArray != null) && (allUnprofiledClassStatusArray.length > classId)
                                        && allUnprofiledClassStatusArray[classId]) {
                                    break;
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
                                refClazz = classManager.javaClassForPrimitiveArrayType(arrayClassId);

                                int classId = refClazz.getInstrClassId();
                                String className = StringUtils.userFormClassName(refClazz.getName());

                                if (!instrFilter.passes(className)) {
                                    break;
                                }
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
        }
        if (nInjections == 0) {
            ((DynamicClassInfo) clazz).unsetMethodInstrumented(methodIdx);
        } else {
            // Done very conservatively.
            maxStack += 2;
        }

        return createPackedMethodInfo();
    }

    private static String getMultiArrayClassName(String refClassName) {
        int dimension = refClassName.lastIndexOf('[');
        String baseClass = refClassName.substring(dimension+1);

        if (VMUtils.isVMPrimitiveType(baseClass)) {
            return StringUtils.userFormClassName(refClassName);
        } else {
            StringBuilder arrayClass = new StringBuilder(refClassName.length()+dimension+1);
            arrayClass.append(refClassName.substring(dimension+1));
            
            for (int i = 0; i <= dimension; i++) {
                arrayClass.append("[]");        // NOI18N
            }
            return arrayClass.toString();
        }
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

    private void injectTraceObjAllocObjCtor(int bci) {
        injectedCode[0] = (byte) opc_aload_0; // Insert aload_0
        // Prepare the traceObjAlloc(Object obj, 0) code packet that is to be injected

        int targetMethodIdx = CPExtensionsRepository.memoryProfContents_TraceObjAllocMethodIdx + baseCPoolCount;
        putU2(injectedCode, injectedCodeMethodIdxPos, targetMethodIdx);
        putU2(injectedCode, injectedCodeClassIdPos, 0);

        injectCodeAndRewrite(injectedCode, injectedCodeLen, bci, true);
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
