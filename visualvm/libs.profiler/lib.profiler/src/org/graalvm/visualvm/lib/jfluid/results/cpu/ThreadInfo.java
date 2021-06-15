/*
 * Copyright (c) 2010, 2019, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.jfluid.results.cpu;

import org.graalvm.visualvm.lib.jfluid.results.cpu.cct.nodes.TimedCPUCCTNode;

public class ThreadInfo {

    //~ Static fields/initializers -------------------------------------------------------------------------------------------
    // The following variable is used to record the "compensation" value, a difference between the timestamp at the
    // moment user hits "get results" and the timestamp for the method entry into the top stack method. To present
    // results consistenly, we add this value to the TimedCPUCCTNode for the top-stack method. However, when
    // processing of data is resumed, we need to subtract this value back from that node.
    // This is effectively the self time for the last invocation of the top method on stack - if we would not keep
    // it separately, it would not be reported
    long diffAtGetResultsMoment0; // diff between last methodEntry and current moment timestamp - we will have to compensate for the processing time
    long diffAtGetResultsMoment1; // as above, but for thread CPU time

    //~ Instance fields ------------------------------------------------------------------------------------------------------
    final private Object stackLock = new Object();
    public TimedCPUCCTNode[] stack;
    // Simulated stack for this thread - stack starting at root method
    // (or a pseudo node if multiple root methods are called within the thread)
    int inRoot;
    // flag indicating this thread is in a root method initiated code
    int stackTopIdx;
    // Index of the stack top element
    public final int threadId;
    public int totalNNodes;
    // total number of call tree nodes for this thread
    long rootGrossTimeAbs;
    long rootGrossTimeThreadCPU;
    // Accumulated absolute and thread CPU gross time for the root method
    // - blackout data subtracted, calibration data not
    public long rootMethodEntryTimeAbs;
    public long rootMethodEntryTimeThreadCPU;
    // Absoute and thread CPU entry timestamps for the root method.
    // The xxx0 part is used when only absolute or thread CPU time data is collected.
    // Both xxx0 and xx1 parts are used when both timestamps are collected.
    public long topMethodEntryTime0;
    public long topMethodEntryTime1;
    // Entry (or "re-entry" upon return from the callee) time for the topmost method
    public long totalNInv;

    ThreadInfo(int threadId) {
        super();
        stack = new TimedCPUCCTNode[40];
        stackTopIdx = -1;
        inRoot = 0;
        this.threadId = threadId;
    }

    public boolean isInRoot() {
        return inRoot > 0;
    }

    public TimedCPUCCTNode peek() {
        synchronized (stackLock) {
            return (stackTopIdx > -1) ? stack[stackTopIdx] : null;
        }
    }

    public TimedCPUCCTNode pop() {
        TimedCPUCCTNode node = null;
        synchronized (stackLock) {
            if (stackTopIdx >= 0) {
                node = stack[stackTopIdx];
                stack[stackTopIdx] = null;
                stackTopIdx--;
            }
            return node;
        }
    }

    public void push(TimedCPUCCTNode node) {
        synchronized (stackLock) {
            stackTopIdx++;
            if (stackTopIdx >= stack.length) {
                increaseStack();
            }
            stack[stackTopIdx] = node;
            node.addNCalls(1);
            totalNInv++;
        }
    }

    private void increaseStack() {
        synchronized(stackLock) {
            TimedCPUCCTNode[] newStack = new TimedCPUCCTNode[stack.length * 2];
            System.arraycopy(stack, 0, newStack, 0, stack.length);
            stack = newStack;
        }
    }
}
