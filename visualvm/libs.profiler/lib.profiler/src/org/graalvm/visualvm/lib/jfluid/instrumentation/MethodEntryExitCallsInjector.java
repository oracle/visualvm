/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayOutputStream;
import org.graalvm.visualvm.lib.jfluid.classfile.DynamicClassInfo;
import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;

import static org.graalvm.visualvm.lib.jfluid.utils.VMUtils.*;

/**
 * Specialized subclass of Injector, that provides injection of our standard
 * "recursive" instrumentation - methodEntry(char methodId) (rootEntry(char
 * methodId)) and methodExit(char methodId) calls - in appropriate places in TA
 * methods.
 *
 * @author Tomas Hurka
 * @author Misha Dmitriev
 */
class MethodEntryExitCallsInjector extends Injector implements CommonConstants {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // Stuff used for rootEntry(char), methodEntry(char), and methodExit(char) injection interchangeably
    protected static byte[] injCode1;
    protected static int injCodeLen1;
    protected static int injCodeMethodIdxPos1;
    protected static int injCodeMethodIdPos1;

    // Stuff used for the equivalent of try { .. } catch (Throwable ex) { methodExit(methodId); throw ex; } around the whole method
    protected static byte[] injCode2;
    protected static int injCodeLen2;
    protected static int injCodeMethodIdxPos2;
    protected static int injCodeMethodIdPos2;

    // Stuff used for markerMethodExit(Object, char) injection
    protected static byte[] injCode3;
    protected static int injCodeLen3;
    protected static int injCodeMethodIdxPos3;
    protected static int injCodeMethodIdPos3;

    static {
        initializeInjectedCode();
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------
    protected int baseRootCPoolCount; // cpool base for root method type injection cpool fragment
    protected int injType; // INJ_RECURSIVE_NORMAL_METHOD, INJ_RECURSIVE_ROOT_METHOD, or same with _SAMPLED_ added
    protected int methodId; // methodId (char parameter value) that methodEntry(methodId) etc. should be invoked with

    //~ Constructors -------------------------------------------------------------------------------------------------------------
    MethodEntryExitCallsInjector(DynamicClassInfo clazz, int normalInstrBaseCPoolCount, int rootInstrBaseCPoolCount, int methodIdx,
            int injType, int methodId) {
        super(clazz, methodIdx);
        this.injType = injType;
        this.methodId = methodId;
        baseCPoolCount = normalInstrBaseCPoolCount;
        baseRootCPoolCount = rootInstrBaseCPoolCount;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------
    public byte[] instrumentMethod() {
        // Determine the number of returns in this method
        int totalReturns = 0;
        int bci = 0;

        while (bci < bytecodesLength) {
            int bc = bytecodes[bci] & 0xFF;

            if ((bc >= opc_ireturn) && (bc <= opc_return)) {
                totalReturns++;
            }

            bci += opcodeLength(bci);
        }

        injectMethodEntry();
        injectMethodExits(totalReturns);
        injectGlobalCatch();

        // Done very conservatively.
        maxStack += 1;

        return createPackedMethodInfo();
    }

    private static void initializeInjectedCode() {
        // Injection for methodEntry(char methodId) (rootMethodEntry(char methodId)) and methodExit(char methodId)
        // the length of the injected code is extended to 8, to avoid worrying about switch statement 4-byte alignment
        injCodeLen1 = 8;
        injCode1 = new byte[injCodeLen1];
        injCode1[0] = (byte) opc_sipush;
        // Positions 1, 2 are occupied by methodId
        injCodeMethodIdPos1 = 1;
        injCode1[3] = (byte) opc_invokestatic;
        // Positions 4, 5 are occupied by method index
        injCodeMethodIdxPos1 = 4;
        injCode1[6] = injCode1[7] = (byte) opc_nop;

        // Injection for the whole-method all-exceptions try - catch
        // We do not need to worry about 4-byte alignment since this always goes to the end of a method
        injCodeLen2 = 9;
        injCode2 = new byte[injCodeLen2];
        injCode2[0] = (byte) opc_astore_1;
        injCode2[1] = (byte) opc_sipush;
        // Positions 2, 3 are occupied by methodId
        injCodeMethodIdPos2 = 2;
        injCode2[4] = (byte) opc_invokestatic;
        // Positions 5, 6 are occpupied by method index for methodExit()
        injCodeMethodIdxPos2 = 5;
        injCode2[7] = (byte) opc_aload_1;
        injCode2[8] = (byte) opc_athrow;

        injCodeLen3 = 8;
        injCode3 = new byte[injCodeLen3];
        injCode3[0] = (byte) opc_dup;
        injCode3[1] = (byte) opc_sipush;
        // Positions 2, 3 are occupied by methodId
        injCodeMethodIdPos3 = 2;
        injCode3[4] = (byte) opc_invokestatic;
        // Positions 5, 6 are occupied by method index
        injCodeMethodIdxPos3 = 5;
        injCode3[7] = (byte) opc_nop;
    }

    /**
     * Injects code that is effectively try { } catch (Throwable ex) {
     * methodExit (); rethrow ex; } To have methodExit called even in case
     * Errors/Exceptions are thrown.
     */
    private void injectGlobalCatch() {
        int targetMethodIdx;

        if ((injType == INJ_RECURSIVE_MARKER_METHOD) || (injType == INJ_RECURSIVE_SAMPLED_MARKER_METHOD)) {
            targetMethodIdx = CPExtensionsRepository.rootContents_MarkerExitMethodIdx + baseRootCPoolCount;
        } else {
            targetMethodIdx = CPExtensionsRepository.normalContents_MethodExitMethodIdx + baseCPoolCount;
        }

        putU2(injCode2, injCodeMethodIdPos2, methodId);
        putU2(injCode2, injCodeMethodIdxPos2, targetMethodIdx);

        int origLen = bytecodesLength;
        int bci = 0;

        //int lastInstrBCI = 0;
        //while (bci < bytecodesLength) {
        //  lastInstrBCI = bci;
        //  bci += opcodeLength(bci);
        //}
        // Above is the "theoretically correct" variant of the code: we should determine the starting offset of the
        // last bytecode of the method, and use it as the end index in the exception table entry that we add. However,
        // for some reason, it appears that at least in case when in the given method the last bytecode is athrow,
        // the above code results in the exception thrown by this athrow and not caught by our global handler. This
        // looks like a bug in the JVM, but anyway, the workaround (?), when we set the last bytecode offset equal
        // to the start of our handler itself, works. Not sure, though, that it will pass verification... but when
        // we have per-method verification working, we will be in a much better position to discuss this possible VM bug :-)
        int lastInstrBCI = bytecodesLength;

        appendCode(injCode2, injCodeLen2);
        addExceptionTableEntry(0, lastInstrBCI, origLen, 0);
        addGlobalCatchStackMapTableEntry(lastInstrBCI);

        if (maxLocals < 2) {
            maxLocals = 2;
        }
    }

    private void injectMethodEntry() {
        int targetMethodIdx = 0;

        // Prepare the methodEntry(char methodId) or rootEntry(char methodId) code packet that is to be injected
        if ((injType == INJ_RECURSIVE_ROOT_METHOD) || (injType == INJ_RECURSIVE_SAMPLED_ROOT_METHOD)) {
            targetMethodIdx = CPExtensionsRepository.rootContents_RootEntryMethodIdx + baseRootCPoolCount;
        } else if ((injType == INJ_RECURSIVE_MARKER_METHOD) || (injType == INJ_RECURSIVE_SAMPLED_MARKER_METHOD)) {
            targetMethodIdx = CPExtensionsRepository.rootContents_MarkerEntryMethodIdx + baseRootCPoolCount;
        } else {
            targetMethodIdx = CPExtensionsRepository.normalContents_MethodEntryMethodIdx + baseCPoolCount;
        }

        putU2(injCode1, injCodeMethodIdxPos1, targetMethodIdx);
        putU2(injCode1, injCodeMethodIdPos1, methodId);

        injectCodeAndRewrite(injCode1, injCodeLen1, 0, true);
        if (injType == INJ_RECURSIVE_MARKER_METHOD || injType == INJ_RECURSIVE_SAMPLED_MARKER_METHOD) {
            // for marker method inject code to get parameters
            ByteArrayOutputStream code = new ByteArrayOutputStream();
            String parTypes = getParTypes();
            int parIndex;
            
            if (clazz.isMethodStatic(methodIdx)) {
                parIndex = 0;
            } else {
                parIndex = 1;
                if ( !"<init>".equals(clazz.getMethodName(methodIdx))) {
                    // 'this' is parameter at index 0
                    getParInvocationCode(REFERENCE, 0, code);
                }
            }
            for (char vmParType : parTypes.toCharArray()) {
                getParInvocationCode(vmParType, parIndex, code);
                switch (vmParType) {
                    case DOUBLE:
                    case LONG:
                        parIndex+=2;
                        break;
                    default:
                        parIndex++;
                }
            }
            int padding = (4 - code.size() % 4) % 4;
            for (int i = 0; i < padding; i++) {
                code.write(opc_nop);
            }
            injectCodeAndRewrite(code.toByteArray(), code.size(), 0, true);        
        }
    }

    private void injectMethodExits(int totalReturns) {
        // Prepare the methodExit(char methodId) code packet
        int targetMethodIdx;
        int targetParMethodIdx = -1;

        if ((injType == INJ_RECURSIVE_MARKER_METHOD) || (injType == INJ_RECURSIVE_SAMPLED_MARKER_METHOD)) {
            targetMethodIdx = CPExtensionsRepository.rootContents_MarkerExitMethodIdx + baseRootCPoolCount;
            targetParMethodIdx = CPExtensionsRepository.rootContents_MarkerExitParMethodIdx + baseRootCPoolCount;
        } else {
            targetMethodIdx = CPExtensionsRepository.normalContents_MethodExitMethodIdx + baseCPoolCount;
        }

        putU2(injCode1, injCodeMethodIdxPos1, targetMethodIdx);
        if (targetParMethodIdx != -1) {
            putU2(injCode3, injCodeMethodIdPos3, methodId);
            putU2(injCode3, injCodeMethodIdxPos3, targetParMethodIdx);            
        }        

        for (int i = 0; i < totalReturns; i++) {
            int retIdx = -1;
            int bci = 0;

            while (bci < bytecodesLength) {
                int bc = bytecodes[bci] & 0xFF;

                if ((bc >= opc_ireturn) && (bc <= opc_return)) {
                    retIdx++;

                    if (retIdx == i) {
                        if (bc == opc_areturn && targetParMethodIdx != -1) {
                            injectCodeAndRewrite(injCode3, injCodeLen3, bci, true);                            
                        } else {
                            injectCodeAndRewrite(injCode1, injCodeLen1, bci, true);
                        }
                        break;
                    }
                }

                bci += opcodeLength(bci);
            }
        }
    }

    private String getParTypes() {
        String sig = clazz.getMethodSignature(methodIdx);
        int idx1 = sig.indexOf('(') + 1; // NOI18N
        int idx2 = sig.lastIndexOf(')'); // NOI18N
        StringBuilder paramsBuf = new StringBuilder();
        boolean arrayIndicator;

        if (idx2 > 0) {
            String paramsString = sig.substring(idx1, idx2);
            arrayIndicator = false;
            int curPos = 0;
            char nextChar;

            while (curPos < paramsString.length()) {
                while (paramsString.charAt(curPos) == '[') { // NOI18N
                    arrayIndicator = true;
                    curPos++;
                }

                nextChar = paramsString.charAt(curPos++);

                if (nextChar == REFERENCE) { // it's a class
                    while (paramsString.charAt(curPos) != ';') { // NOI18N
                        curPos++;
                    }
                    curPos++;
                }

                if (arrayIndicator) {
                    paramsBuf.append(REFERENCE);
                } else {
                    paramsBuf.append(nextChar);
                }
            }
        }
        return paramsBuf.toString();
    }

    private void getParInvocationCode(char vmParType, int i, ByteArrayOutputStream code) {

        switch (vmParType) {
            case BOOLEAN: {
                getIloadCode(i, code);
                getInvokeStatic(CPExtensionsRepository.miContents_AddParBooleanMethodIdx + baseRootCPoolCount, code);
                break;
            }
            case CHAR: {
                getIloadCode(i, code);
                getInvokeStatic(CPExtensionsRepository.miContents_AddParCharMethodIdx + baseRootCPoolCount, code);
                break;
            }
            case BYTE: {
                getIloadCode(i, code);
                getInvokeStatic(CPExtensionsRepository.miContents_AddParByteMethodIdx + baseRootCPoolCount, code);
                break;
            }
            case SHORT: {
                getIloadCode(i, code);
                getInvokeStatic(CPExtensionsRepository.miContents_AddParShortMethodIdx + baseRootCPoolCount, code);
                break;
            }
            case INT: {
                getIloadCode(i, code);
                getInvokeStatic(CPExtensionsRepository.miContents_AddParIntMethodIdx + baseRootCPoolCount, code);
                break;
            }
            case LONG: {
                getLloadCode(i, code);
                getInvokeStatic(CPExtensionsRepository.miContents_AddParLongMethodIdx + baseRootCPoolCount, code);
                break;
            }
            case FLOAT: {
                getFloadCode(i, code);
                getInvokeStatic(CPExtensionsRepository.miContents_AddParFloatMethodIdx + baseRootCPoolCount, code);
                break;
            }
            case DOUBLE: {
                getDloadCode(i, code);
                getInvokeStatic(CPExtensionsRepository.miContents_AddParDoubleMethodIdx + baseRootCPoolCount, code);
                break;
            }
            case REFERENCE: {
                getAloadCode(i, code);
                getInvokeStatic(CPExtensionsRepository.miContents_AddParObjectMethodIdx + baseRootCPoolCount, code);
                break;
            }
        }
    }

    private void getIloadCode(int index, ByteArrayOutputStream code) {
        switch (index) {
            case 0:
                code.write(opc_iload_0);
                break;
            case 1:
                code.write(opc_iload_1);
                break;
            case 2:
                code.write(opc_iload_2);
                break;
            case 3:
                code.write(opc_iload_3);
                break;
            default:
                code.write(opc_iload);
                code.write(index);
        }
    }

    private void getLloadCode(int index, ByteArrayOutputStream code) {
        switch (index) {
            case 0:
                code.write(opc_lload_0);
                break;
            case 1:
                code.write(opc_lload_1);
                break;
            case 2:
                code.write(opc_lload_2);
                break;
            case 3:
                code.write(opc_lload_3);
                break;
            default:
                code.write(opc_lload);
                code.write(index);
        }
    }

    private void getFloadCode(int index, ByteArrayOutputStream code) {
        switch (index) {
            case 0:
                code.write(opc_fload_0);
                break;
            case 1:
                code.write(opc_fload_1);
                break;
            case 2:
                code.write(opc_fload_2);
                break;
            case 3:
                code.write(opc_fload_3);
                break;
            default:
                code.write(opc_fload);
                code.write(index);
        }
    }

    private void getDloadCode(int index, ByteArrayOutputStream code) {
        switch (index) {
            case 0:
                code.write(opc_dload_0);
                break;
            case 1:
                code.write(opc_dload_1);
                break;
            case 2:
                code.write(opc_dload_2);
                break;
            case 3:
                code.write(opc_dload_3);
                break;
            default:
                code.write(opc_dload);
                code.write(index);
        }
    }

    private void getAloadCode(int index, ByteArrayOutputStream code) {
        switch (index) {
            case 0:
                code.write(opc_aload_0);
                break;
            case 1:
                code.write(opc_aload_1);
                break;
            case 2:
                code.write(opc_aload_2);
                break;
            case 3:
                code.write(opc_aload_3);
                break;
            default:
                code.write(opc_aload);
                code.write(index);
        }
    }
    
    private void getInvokeStatic(int cpIndex, ByteArrayOutputStream code) {
        code.write(opc_invokestatic);
        code.write((cpIndex >> 8) & 0xFF);
        code.write(cpIndex & 0xFF);
    }
}
