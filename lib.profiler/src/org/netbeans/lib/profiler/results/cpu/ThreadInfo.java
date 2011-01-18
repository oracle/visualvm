package org.netbeans.lib.profiler.results.cpu;

import org.netbeans.lib.profiler.results.cpu.cct.nodes.TimedCPUCCTNode;

class ThreadInfo {

    //~ Static fields/initializers -------------------------------------------------------------------------------------------
    // The following variable is used to record the "compensation" value, a difference between the timestamp at the
    // moment user hits "get results" and the timestamp for the method entry into the top stack method. To present
    // results consistenly, we add this value to the TimedCPUCCTNode for the top-stack method. However, when
    // processing of data is resumed, we need to subtract this value back from that node.
    // this is effectively the self time for the last invocation of the top method on stack - if we would not keep
    // it separately, it would not be reported
    // private long diffAtGetResultsMoment; // diff between last methodEntry and current moment timestamp -
    //  we will have to compensate for the processing time

    //~ Instance fields ------------------------------------------------------------------------------------------------------
    final private Object stackLock = new Object();
    TimedCPUCCTNode[] stack;
    // Simulated stack for this thread - stack starting at root method
    // (or a pseudo node if multiple root methods are called within the thread)
    int inRoot;
    // flag indicating this thread is in a root method initiated code
    int stackTopIdx;
    // Index of the stack top element
    final int threadId;
    int totalNNodes;
    // total number of call tree nodes for this thread
    long rootGrossTimeAbs;
    long rootGrossTimeThreadCPU;
    // Accumulated absolute and thread CPU gross time for the root method
    // - blackout data subtracted, calibration data not
    long rootMethodEntryTimeAbs;
    long rootMethodEntryTimeThreadCPU;
    // Absoute and thread CPU entry timestamps for the root method.
    // The xxx0 part is used when only absolute or thread CPU time data is collected.
    // Both xxx0 and xx1 parts are used when both timestamps are collected.
    long topMethodEntryTime0;
    long topMethodEntryTime1;
    // Entry (or "re-entry" upon return from the callee) time for the topmost method
    long totalNInv;

    ThreadInfo(int threadId) {
        super();
        stack = new TimedCPUCCTNode[40];
        stackTopIdx = -1;
        inRoot = 0;
        this.threadId = threadId;
    }

    boolean isInRoot() {
        return inRoot > 0;
    }

    TimedCPUCCTNode peek() {
        synchronized (stackLock) {
            return (stackTopIdx > -1) ? stack[stackTopIdx] : null;
        }
    }

    TimedCPUCCTNode pop() {
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

    void push(TimedCPUCCTNode node) {
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
