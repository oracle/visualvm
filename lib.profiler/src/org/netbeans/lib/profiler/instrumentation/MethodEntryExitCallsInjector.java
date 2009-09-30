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

import org.netbeans.lib.profiler.classfile.ClassInfo;
import org.netbeans.lib.profiler.global.CommonConstants;


/**
 * Specialized subclass of Injector, that provides injection of our standard "recursive" instrumentation -
 * methodEntry(char methodId) (rootEntry(char methodId)) and methodExit(char methodId) calls - in appropriate places
 * in TA methods.
 *
 * @author Tomas Hurka
 *  @author Misha Dmitriev
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

    static {
        initializeInjectedCode();
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected int baseRootCPoolCount; // cpool base for root method type injection cpool fragment
    protected int injType; // INJ_RECURSIVE_NORMAL_METHOD, INJ_RECURSIVE_ROOT_METHOD, or same with _SAMPLED_ added
    protected int methodId; // methodId (char parameter value) that methodEntry(methodId) etc. should be invoked with

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    MethodEntryExitCallsInjector(ClassInfo clazz, int normalInstrBaseCPoolCount, int rootInstrBaseCPoolCount, int methodIdx,
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
        // the length of the injected code if extended to 8, to avoid worrying about switch statement 4-byte alignment
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
    }

    /**
     * Injects code that is effectively try { } catch (Throwable ex) { methodExit (); rethrow ex; }
     * To have methodExit called even in case Errors/Exceptions are thrown.
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
    }

    private void injectMethodExits(int totalReturns) {
        // Prepare the methodExit(char methodId) code packet
        int targetMethodIdx;

        if ((injType == INJ_RECURSIVE_MARKER_METHOD) || (injType == INJ_RECURSIVE_SAMPLED_MARKER_METHOD)) {
            targetMethodIdx = CPExtensionsRepository.rootContents_MarkerExitMethodIdx + baseRootCPoolCount;
        } else {
            targetMethodIdx = CPExtensionsRepository.normalContents_MethodExitMethodIdx + baseCPoolCount;
        }

        putU2(injCode1, injCodeMethodIdxPos1, targetMethodIdx);

        for (int i = 0; i < totalReturns; i++) {
            int retIdx = -1;
            int bci = 0;

            while (bci < bytecodesLength) {
                int bc = bytecodes[bci] & 0xFF;

                if ((bc >= opc_ireturn) && (bc <= opc_return)) {
                    retIdx++;

                    if (retIdx == i) {
                        injectCodeAndRewrite(injCode1, injCodeLen1, bci, true);

                        break;
                    }
                }

                bci += opcodeLength(bci);
            }
        }
    }
}
