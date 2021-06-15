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

import org.graalvm.visualvm.lib.jfluid.classfile.DynamicClassInfo;
import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;


/**
 * Specialized subclass of Injector, that provides injection of our standard Code Region instrumentation -
 * codeRegionEntry() and codeRegionExit() calls - in appropriate places.
 *
 * @author Misha Dmitriev
 */
class CodeRegionEntryExitCallsInjector extends Injector implements CommonConstants {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // Stuff used for codeRegionEntry() and codeRegionExit() injection
    protected static byte[] injectedCode;
    protected static int injectedCodeLen;
    protected static int injectedCodeMethodIdxPos;

    static {
        initializeInjectedCode();
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected int bci0; // Original code region bounds
    protected int bci1; // Original code region bounds

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    CodeRegionEntryExitCallsInjector(DynamicClassInfo clazz, int baseCPoolCount, int methodIdx, int bci0, int bci1) {
        super(clazz, methodIdx);
        this.baseCPoolCount = baseCPoolCount;
        this.bci0 = bci0;
        this.bci1 = bci1;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public byte[] instrumentMethod() {
        // Determine the index (among return instructions) of first return within the given code region, total number of returns between
        // bci = 0 and bci1, and the original index of the last instruction within this region.
        int firstRetIdx = -1;
        int totalReturns = 0;
        int lastInstrIdx = -1;
        int bci = 0;

        while (bci <= bci1) {
            int bc = bytecodes[bci] & 0xFF;
            lastInstrIdx++;

            if ((bc >= opc_ireturn) && (bc <= opc_return)) {
                if ((bci >= bci0) && (firstRetIdx == -1)) {
                    firstRetIdx = totalReturns;
                }

                totalReturns++;
            }

            bci += opcodeLength(bci);
        }

        injectCodeRegionEntry();
        lastInstrIdx += 2; // Since we added two opcodes in the above operation
        injectCodeRegionExits(firstRetIdx, totalReturns, lastInstrIdx);

        return createPackedMethodInfo();
    }

    private static void initializeInjectedCode() {
        // Code packet for codeRegionEntry()/codeRegionExit()
        injectedCodeLen = 4;
        injectedCode = new byte[injectedCodeLen];
        injectedCode[0] = (byte) opc_invokestatic;
        // Positions 1, 2 are occupied by method index
        injectedCodeMethodIdxPos = 1;
        injectedCode[3] = (byte) opc_nop;
    }

    private void injectCodeRegionEntry() {
        int targetMethodIdx = CPExtensionsRepository.codeRegionContents_CodeRegionEntryMethodIdx + baseCPoolCount;
        putU2(injectedCode, injectedCodeMethodIdxPos, targetMethodIdx);

        injectCodeAndRewrite(injectedCode, injectedCodeLen, bci0, true);
    }

    private void injectCodeRegionExits(int firstRetIdx, int totalReturns, int lastInstrIdx) {
        // Prepare the codeRegionExit() code packet
        int targetMethodIdx = CPExtensionsRepository.codeRegionContents_CodeRegionExitMethodIdx + baseCPoolCount;
        putU2(injectedCode, injectedCodeMethodIdxPos, targetMethodIdx);

        int curInstrIdx = -1;

        if (firstRetIdx != -1) { // There is a corner case when a method has no returns at all - e.g. when it contains just a "while (true)" loop
                                 // Inject codeRegionExit() before each return inside the selected fragment

            for (int inFragmentRetIndex = firstRetIdx; inFragmentRetIndex < totalReturns; inFragmentRetIndex++) {
                int curRetIdx = -1;
                curInstrIdx = -1;

                int bci = 0;

                while (bci < bytecodesLength) {
                    curInstrIdx++;

                    int bc = bytecodes[bci] & 0xFF;

                    if ((bc >= opc_ireturn) && (bc <= opc_return)) {
                        curRetIdx++;

                        if (curRetIdx == inFragmentRetIndex) {
                            injectCodeAndRewrite(injectedCode, injectedCodeLen, bci, true);
                            lastInstrIdx += 2;

                            break;
                        }
                    }

                    bci += opcodeLength(bci);
                }
            }
        }

        // Inject the call at the last bytecode, which may be anything
        if (curInstrIdx == lastInstrIdx) {
            return;
        }

        curInstrIdx = -1;

        int bci = 0;

        while (bci < bytecodesLength) {
            curInstrIdx++;

            if (curInstrIdx >= lastInstrIdx) {
                injectCodeAndRewrite(injectedCode, injectedCodeLen, bci, true);

                break;
            }

            bci += opcodeLength(bci);
        }
    }
}
