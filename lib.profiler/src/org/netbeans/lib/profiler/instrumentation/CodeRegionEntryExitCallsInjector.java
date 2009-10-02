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

    CodeRegionEntryExitCallsInjector(ClassInfo clazz, int baseCPoolCount, int methodIdx, int bci0, int bci1) {
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
