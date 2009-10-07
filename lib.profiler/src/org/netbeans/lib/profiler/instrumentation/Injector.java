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
import org.netbeans.lib.profiler.client.RuntimeProfilingPoint;
import java.util.Stack;


/**
 * Base class for a number of classes performing concrete method instrumentation injections. Contains core functionality
 * for injection of arbitrary bytecodes at a given location in a method and subsequent rewriting of bytecodes. Also
 * supports appending bytecodes to the existing bytecodes, and extending the method's exception table.
 *
 * @author Misha Dmitriev
 */
public abstract class Injector extends SingleMethodScaner {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    //-------------------------------------- Helper classes ------------------------------------------
    private abstract static class ChangeItem {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        int bci;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        ChangeItem(int bci) {
            this.bci = bci;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        abstract void handleCodeChange(Injector r);

        void relocate(int breakBCI, int delta) {
            if (bci > breakBCI) {
                bci += delta;
            }
        }
    }

    private static class ChangeJumpWiden extends ChangeItem {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        int delta; // New length of instruction at bci

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        ChangeJumpWiden(int bci, int delta) {
            super(bci);
            this.delta = delta;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        // Callback to do instruction
        void handleCodeChange(Injector r) {
            r.handleJumpWiden(bci, delta);
        }
    }

    private static class ChangeSwitchPadding extends ChangeItem {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        boolean isLookupSwitch;
        int padding;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        ChangeSwitchPadding(int bci, int padding, boolean isLookupSwitch) {
            super(bci);
            this.padding = padding;
            this.isLookupSwitch = isLookupSwitch;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        // Callback to do instruction
        void handleCodeChange(Injector r) {
            r.handleSwitchPadding(bci, padding, isLookupSwitch);
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final int MAX_SHORT = ((1 << 15) - 1);
    private static final int MIN_SHORT = (-(1 << 15));

    // Increment to possible stack depth -- two instrumentation arguments
    private static final int STACK_INCREMENT = 2;
    private static Stack changes = new Stack(); // Stack of additional changes to bytecodes that may be caused by our main injection change
    private static byte[] _overwrite = new byte[3]; // Stores overwritten bytes for shrunken instructions

    // The following array is re-used,to avoid excessive object allocations - which means that the class is not multi thread safe
    private static byte[] reusableExcTable = new byte[100];

    // ProfilerRuntime.profilePointHit instrumentation
    private static final byte[] injProfilePointHitCode = new byte[] { opc_sipush, 0, 0, (byte) opc_invokestatic, 0, 0, 0, 0 };
    private static final int injProfilePointHitIDCodeIdx = 1;
    private static final int injProfilePointHitMethodIdx = 4;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected byte[] exceptionTable; // Current updateable copy of exception table (what is in MethodInfo Code attribute after exception_table_length)
    protected int baseCPoolCount; // The "base" constant pool index, after which the set of entries containing the calls that
                                  // we inject, plus supporting entries, are located.
    protected int excTableEntryCount;
    protected int maxLocals;
    protected int maxStack;
    protected int origBytecodesLength;
    protected int origExcTableEntryCount;
    private boolean changeTypeIsInjectNewInstr;
    private boolean injectionBindsToFollowingInstruction;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    // ---------------------------------------- Protected methods -------------------------------------------------
    protected Injector() {
    }

    /**
     * Creates a new Injector for the specified method. Subsequently one can call injectCodeAndRewrite() and other
     * methods below as many times as needed.
     */
    protected Injector(ClassInfo clazz, int methodIdx) {
        super(clazz, methodIdx);
        origBytecodesLength = bytecodesLength;
        maxStack = getU2(origMethodInfo, bytecodesStartIdx - 8);
        maxLocals = getU2(origMethodInfo, bytecodesStartIdx - 6);
        initExceptionTable();
        origExcTableEntryCount = excTableEntryCount;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    // ------------------------------------ Public abstract interface ---------------------------------------------

    /** In concrete subclasses, should do all the real work on method instrumentation and return the updated packed methodInfo */
    public abstract byte[] instrumentMethod();

    protected void addExceptionTableEntry(int startPC, int endPC, int handlerPC, int typeCPIndex) {
        int pos = excTableEntryCount * 8;
        putU2(exceptionTable, pos, startPC);
        putU2(exceptionTable, pos + 2, endPC);
        putU2(exceptionTable, pos + 4, handlerPC);
        putU2(exceptionTable, pos + 6, typeCPIndex);
        excTableEntryCount++;
    }

    protected void appendCode(byte[] appendedBytes, int appendedBytesCount) {
        System.arraycopy(appendedBytes, 0, bytecodes, bytecodesLength, appendedBytesCount);
        bytecodesLength += appendedBytesCount;
    }

    protected byte[] createPackedMethodInfo() {
        int diff = (bytecodesLength - origBytecodesLength) + ((excTableEntryCount - origExcTableEntryCount) * 8);
        byte[] ret = new byte[origMethodInfo.length + diff];
        System.arraycopy(origMethodInfo, 0, ret, 0, bytecodesStartIdx);
        System.arraycopy(bytecodes, 0, ret, bytecodesStartIdx, bytecodesLength);

        int attrLength = getU4(origMethodInfo, bytecodesStartIdx - 12);
        attrLength += diff;
        putU4(ret, bytecodesStartIdx - 12, attrLength); // Patch the attribute_length in Code attribute
        putU4(ret, bytecodesStartIdx - 4, bytecodesLength); // Patch the code_length in Code attribute
        System.arraycopy(origMethodInfo, bytecodesStartIdx + origBytecodesLength, ret, bytecodesStartIdx + bytecodesLength,
                         origMethodInfo.length - bytecodesStartIdx - origBytecodesLength);

        int excTableOldStart = clazz.getExceptionTableStartOffsetInMethodInfo(methodIdx);
        int excTableNewStart = excTableOldStart + (bytecodesLength - origBytecodesLength);
        int excTableOldLen = origExcTableEntryCount * 8;
        int excTableNewLen = excTableEntryCount * 8;

        if (excTableOldLen != excTableNewLen) {
            System.arraycopy(ret, excTableNewStart + excTableOldLen, ret, excTableNewStart + excTableNewLen,
                             origMethodInfo.length - excTableOldStart - excTableOldLen);
            putU2(ret, excTableNewStart, excTableEntryCount);
        }

        System.arraycopy(exceptionTable, 0, ret, excTableNewStart + 2, excTableNewLen);

        // FIXME: need to update linenumber table and localvariable table as well
        putU2(ret, bytecodesStartIdx - 8, maxStack + STACK_INCREMENT);
        putU2(ret, bytecodesStartIdx - 6, maxLocals);

        return ret;
    }

    /**
     * Injects the specified raw bytes array at the specified location.
     * The last parameter indicates whether the injected code should be bound to the following (if true) or previous (if false) bytecode
     * instruction. When binding to the following instruction, all jumps that target that instruction will target the injected code after
     * rewriting. When binding to the previous instruction x, all jumps that target the instruction x+1 will still target that instruction
     * after rewriting (i.e. not target our injected code, as in the first case).
     */
    protected void injectCodeAndRewrite(byte[] injectedBytes, int injectedBytesCount, int injectionPos,
                                        boolean injectionBindsToFollowingInstruction) {
        this.injectionBindsToFollowingInstruction = injectionBindsToFollowingInstruction;
        relocateCode(injectionPos, 0, injectedBytesCount, true);
        // Copy the bytecodes to inject into the method
        System.arraycopy(injectedBytes, 0, bytecodes, injectionPos, injectedBytesCount);
        // Now handle the additional changes that could be caused by the injection
        handleCodeChanges();
    }

    /**
     * Profiling points MUST BE already SORTED by BCI in ascending order and only relevant to this method.
     *
     * @param points sorted array of Profiling points
     * @param ppHitCPMethodIdx
     */
    protected void insertProfilingPoints(RuntimeProfilingPoint[] points, int ppHitCPMethodIdx) {
        for (int i = 0; i < points.length; i++) {
            RuntimeProfilingPoint point = points[i];
            assert (i == 0) || (point.getBci() >= points[i - 1].getBci());

            int ppbci = point.getBci() + (i * injProfilePointHitCode.length);
            injectProfilePointHit(point, ppbci, ppHitCPMethodIdx);
            maxStack = Math.max(maxStack, 4);
        }
    }

    /** Called by the ChangeJumpWiden class */
    void handleJumpWiden(int bci, int delta) {
        int ilen = rcInstrLen(bci);

        if (ilen != 3) {
            return; // Request already handled
                    //Above: assert(code_at(bci) == Bytecodes::_goto_w || code_at(bci) == Bytecodes::_jsr_w, "sanity check");
        }

        //assert(ilen == 3, "check length");
        relocateCode(bci, 3, 2, false);

        int bc = bytecodes[bci] & 0xff;

        switch (bc) {
            case opc_goto:
                bytecodes[bci] = (byte) opc_goto_w;

                break;
            case opc_jsr:
                bytecodes[bci] = (byte) opc_jsr_w;

                break;
            default:
                System.err.println("*** Profiler Engine: error - should not reach here in handleJumpWiden!"); // NOI18N
        }

        // If it's a forward jump, add 2 for the widening.
        if (delta > 0) {
            delta += 2;
        }

        putInt(bci + 1, delta);
    }

    /** If a change item is recorded for bci, with the same is_lookup_switch flag value, returns the associated padding, else -1. */
    private int getOrigSwitchPadding(int bci, boolean isLookupSwitch) {
        for (int k = 0; k < changes.size(); k++) {
            ChangeItem ci = (ChangeItem) changes.elementAt(k);

            if (ci instanceof ChangeSwitchPadding) {
                ChangeSwitchPadding csp = (ChangeSwitchPadding) ci;

                if ((csp.isLookupSwitch == isLookupSwitch) && (csp.bci == bci)) {
                    return csp.padding;
                }
            }
        }

        return -1;
    }

    /**
     * The current instruction at bci is a jump; one of its offsets starts at "offset" and is a short if isShort is true,
     * and an integer otherwise.  If the jump crosses breakBCI, change the span of the jump by delta.
     */
    private void changeJump(int bci, int offset, boolean isShort, int breakBCI, int delta) {
        int bciDelta = (isShort) ? getShort(offset) : getInt(offset);
        int targ = bci + bciDelta;

        boolean doRewrite = false;

        // Injection of a completely new instruction and widening of an existing instruction should be handled differently
        if (changeTypeIsInjectNewInstr) {
            if (breakBCI == 0) {
                return; // No jump to the code injected at offset 0 can be made in the rewrited code
                        // The most reliable way to verify the correctness of the statements below, is to draw a picture, something like
                        // 1 a
                        //     <--- breakBCI = 2
                        // 2 b
                        //     <--- breakBCI = 3
                        // 3 goto
                        //     <--- breakBCI = 4
                        // 4 c
                        // 5 d
                        //
                        // and then consider all the combinations of bci = 3 with breakBCI = 2, 3, 4 and targ = 1,2,4,5 - what should happen in each case.
                        // For example:
                        // bci = 3, breakBCI = 2, injectionBindsToFollowingInstruction = true
                        // targ = 1 - should rewrite jump argument
                        // targ = 2 - should rewrite jump argument (because we will need to jump to the injected code, rather than old instruction at b at 2)
                        // targ = 4, 5 - don't need to rewrite jump argument
                        // ... and so on
            }

            if (injectionBindsToFollowingInstruction) {
                if (((bci < breakBCI) && (targ > breakBCI)) || ((bci >= breakBCI) && (targ <= breakBCI))) {
                    doRewrite = true;
                }
            } else { // Injected code binds to previous instruction, e.g. the traceObjAlloc() call that follows a constructor call

                if (((bci < breakBCI) && (targ >= breakBCI)) || ((bci >= breakBCI) && (targ < breakBCI))) {
                    doRewrite = true;
                }
            }
        } else {
            if (((bci <= breakBCI) && (targ > breakBCI)) || ((bci >= breakBCI) && (targ < breakBCI))) {
                doRewrite = true;
            }
        }

        if (doRewrite) {
            int newDelta = (bciDelta > 0) ? (bciDelta + delta) : (bciDelta - delta);

            if (isShort && ((newDelta > MAX_SHORT) || (newDelta < MIN_SHORT))) {
                changes.push(new ChangeJumpWiden(bci, newDelta));
            } else if (isShort) {
                putShort(offset, (short) newDelta);
            } else {
                putInt(offset, newDelta);
            }
        }
    }

    /** Changes all jumps crossing breakBCI by delta.  May enqueue things on the changes stack */
    private void changeJumps(int breakBCI, int delta) {
        int bci = 0;
        int bc;

        // Now, adjust any affected instructions.
        while (bci < bytecodesLength) {
            bc = (bytecodes[bci] & 0xFF);

            if (((bc >= opc_ifeq) && (bc <= opc_if_acmpne)) || (bc == opc_ifnull) || (bc == opc_ifnonnull) || (bc == opc_goto)
                    || (bc == opc_jsr)) {
                changeJump(bci, bci + 1, true, breakBCI, delta);
            } else {
                switch (bc) {
                    case opc_goto_w:
                    case opc_jsr_w:
                        changeJump(bci, bci + 1, false, breakBCI, delta);

                        break;
                    case opc_tableswitch:
                    case opc_lookupswitch: {
                        int recPad = getOrigSwitchPadding(bci, (bc != opc_tableswitch));
                        int oldPad = (recPad != -1) ? recPad : (align(bci + 1) - (bci + 1));

                        if (bci > breakBCI) {
                            int new_bci = bci + delta;
                            int newPad = align(new_bci + 1) - (new_bci + 1);

                            // Do we need to check the padding?
                            if (newPad != oldPad) {
                                if (recPad == -1) {
                                    changes.push(new ChangeSwitchPadding(bci, oldPad, (bc != opc_tableswitch)));
                                }
                            }
                        }

                        // Then the rest, which depends on the kind of switch.
                        if (bc == opc_tableswitch) {
                            changeJump(bci, bci + 1 + oldPad, false, breakBCI, delta);

                            // We cannot use the Bytecode_tableswitch abstraction, since the padding might not be correct.
                            int lo = getInt(bci + 1 + oldPad + (4 * 1));
                            int hi = getInt(bci + 1 + oldPad + (4 * 2));
                            int n = hi - lo + 1;

                            for (int k = 0; k < n; k++) {
                                changeJump(bci, bci + 1 + oldPad + (4 * (k + 3)), false, breakBCI, delta);
                            }

                            // Special next-bci calculation here...
                            bci += (1 + oldPad + ((n + 3) * 4));

                            continue;
                        } else {
                            changeJump(bci, bci + 1 + oldPad, false, breakBCI, delta);

                            // We cannot use the Bytecode_lookupswitch abstraction, since the padding might not be correct.
                            int npairs = getInt(bci + 1 + oldPad + (4 * 1));

                            for (int k = 0; k < npairs; k++) {
                                changeJump(bci, bci + 1 + oldPad + (4 * (2 + (2 * k) + 1)), false, breakBCI, delta);
                            }

                            // Special next-bci calculation here...
                            bci += (1 + oldPad + ((2 + (npairs * 2)) * 4));

                            continue;
                        }
                    }
                    default:
                        break;
                }
            }

            bci += opcodeLength(bci);
        }
    }

    //--------------------------------------- Private implementation -------------------------------------------
    private void handleCodeChanges() {
        while (!changes.empty()) {
            // Inv: everything is aligned.
            ChangeItem ci = (ChangeItem) changes.pop();
            ci.handleCodeChange(this);
        }
    }

    /** Handle lookup/table switch instructions.  Called be ChangeSwitchPad class */
    private void handleSwitchPadding(int bci, int oldPad, boolean isLookupSwitch) {
        int ilen = rcInstrLen(bci);
        int newPad = align(bci + 1) - (bci + 1);
        int padDelta = newPad - oldPad;

        if (padDelta != 0) {
            int len;

            if (!isLookupSwitch) {
                int low = getInt(bci + 1 + oldPad + 4);
                int high = getInt(bci + 1 + oldPad + 8);
                len = high - low + 1 + 3; // 3 for default, hi, lo.
            } else {
                int npairs = getInt(bci + 1 + oldPad + 4);
                len = (npairs * 2) + 2; // 2 for default, npairs.
            }

            // Because "relocateCode" does a "changeJumps" loop, which parses instructions to determine their length,
            // we need to call that before messing with the current instruction.  Since it may also overwrite the current
            // instruction when moving down, remember the possibly overwritten part.
            relocateCode(bci, ilen, padDelta, false); // Relocate the code following the instruction...

            if (padDelta < 0) { // Move the shrunken instruction down.
                System.arraycopy(bytecodes, bci + 1 + oldPad, bytecodes, bci + 1 + newPad, (len * 4) + padDelta);
                System.arraycopy(_overwrite, 0, bytecodes, bci + 1 + newPad + (len * 4) + padDelta, -padDelta);
            } else { // Move the expanded instruction up.
                System.arraycopy(bytecodes, bci + 1 + oldPad, bytecodes, bci + 1 + newPad, len * 4);
            }
        }
    }

    private void initExceptionTable() {
        int startOfs = clazz.getExceptionTableStartOffsetInMethodInfo(methodIdx);
        excTableEntryCount = getU2(origMethodInfo, startOfs);

        int len = excTableEntryCount * 8;

        if (reusableExcTable.length < (len + 40)) {
            reusableExcTable = new byte[(len * 2) + 40]; // Can add at least 8*5 = 40 entries.
        }

        System.arraycopy(origMethodInfo, startOfs + 2, reusableExcTable, 0, len);
        exceptionTable = reusableExcTable;
    }

    private void injectProfilePointHit(RuntimeProfilingPoint point, int bci, int ppHitCPMethodIdx) {
        // Prepare the profilePointHit(char id, char flags) code packet that is to be injected
        putU2(injProfilePointHitCode, injProfilePointHitMethodIdx, ppHitCPMethodIdx + baseCPoolCount);
        putU2(injProfilePointHitCode, injProfilePointHitIDCodeIdx, point.getId());

        injectCodeAndRewrite(injProfilePointHitCode, injProfilePointHitCode.length, bci, true);
    }

    /**
     * We need a special instruction size method, since lookupswitches and tableswitches might not be
     * properly aligned during relocation
     */
    private int rcInstrLen(int bci) {
        int bc = bytecodes[bci] & 0xFF;

        switch (bc) {
            // In the case of switch instructions, see if we have the original padding recorded.
            case opc_tableswitch:
            case opc_lookupswitch: {
                int pad = getOrigSwitchPadding(bci, (bc == opc_lookupswitch));

                if (pad == -1) {
                    return opcodeLength(bci);
                }

                // Otherwise, depends on the switch type.
                switch (bc) {
                    case opc_tableswitch: {
                        int lo = getInt(bci + 1 + pad + (4 * 1));
                        int hi = getInt(bci + 1 + pad + (4 * 2));
                        int n = hi - lo + 1;

                        return 1 + pad + (4 * (3 + n));
                    }
                    case opc_lookupswitch: {
                        int npairs = getInt(bci + 1 + pad + (4 * 1));

                        return 1 + pad + (4 * (2 + (2 * npairs)));
                    }
                }
            }
        }

        return opcodeLength(bci);
    }

    /**
     * The input is interpreted depending on the injectNewInstr flag value.
     * If it's false, instruction at bci, whose size is iLen, is changing size by delta.
     * If it's true, we inject some instructions bytes of length delta at bci.
     * Reallocate, move code, recalculate jumps, and enqueue more change items as necessary.
     * Note that currently it's assumed that delta is a multiple of 4 - in this way we avoid additional problems with changed
     * paddings in 'switch' instructions.
     */
    private void relocateCode(int bci, int iLen, int delta, boolean injectNewInstr) {
        changeTypeIsInjectNewInstr = injectNewInstr; // Other methods need to know whether we are injecting or widening an instruction

        // Change jumps before doing the copying; this routine requires aligned switches. If we inject something in
        // the very beginning of the method, changing jumps is not needed.
        if (bci > 0) {
            changeJumps(bci, delta);
        }

        // In case we have shrunken a tableswitch/lookupswitch statement, we store the last
        // bytes that get overwritten. We have to copy the bytes after the changeJumps method
        // has been called, since it is likely to update last offset in a tableswitch/lookupswitch
        if (delta < 0) {
            //assert(delta>=-3, "we cannot overwrite more than 3 bytes");
            System.arraycopy(bytecodes, bci + iLen + delta, _overwrite, 0, -delta);
        }

        int nextBCI = bci + iLen; // Needed for instruction widening; ok for injection, since iLen == 0 in that case
        System.arraycopy(bytecodes, nextBCI, bytecodes, nextBCI + delta, bytecodesLength - nextBCI);
        bytecodesLength += delta;

        updateExceptionTable(bci, delta);

        // We currently don't support the following updates - they are used only by debuggers.
        // updateLineNumberTable(injectionPos, delta);
        // updateLocalVariableTable(injectionPos, delta);
        // updateLocalVariableTypeTable(injectionPos, delta);

        // Relocate the bcis of changes in the pending change stack
        for (int j = 0; j < changes.size(); j++) {
            ChangeItem ci = (ChangeItem) changes.elementAt(j);
            ci.relocate(bci, delta);
        }
    }

    private void updateExceptionTable(int injectionPos, int injectedBytesCount) {
        int pos = 0;

        for (int i = 0; i < excTableEntryCount; i++) {
            int startPC = getU2(exceptionTable, pos);
            int endPC = getU2(exceptionTable, pos + 2);
            int handlerPC = getU2(exceptionTable, pos + 4);

            if (startPC > injectionPos) {
                startPC += injectedBytesCount;
                endPC += injectedBytesCount;
                putU2(exceptionTable, pos, startPC);
                putU2(exceptionTable, pos + 2, endPC);
            } else if (injectionPos < endPC) {
                endPC += injectedBytesCount;
                putU2(exceptionTable, pos + 2, endPC);
            }

            if (handlerPC > injectionPos) {
                handlerPC += injectedBytesCount;
                putU2(exceptionTable, pos + 4, handlerPC);
            }

            pos += 8;
        }
    }
}
