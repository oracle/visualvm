/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.jfluid.results.cpu.marking;

import java.util.ArrayDeque;
import java.util.Deque;
import org.graalvm.visualvm.lib.jfluid.global.TransactionalSupport;
import org.graalvm.visualvm.lib.jfluid.marker.Mark;
import org.graalvm.visualvm.lib.jfluid.results.RuntimeCCTNodeProcessor;
import org.graalvm.visualvm.lib.jfluid.results.cpu.cct.nodes.MarkedCPUCCTNode;

/**
 *
 * @author Jaroslav Bachorik
 */
public class MarkAwareNodeProcessorPlugin extends RuntimeCCTNodeProcessor.PluginAdapter implements MarkingEngine.StateObserver {
    volatile boolean resetFlag = false;
    private Mark parentMark = null;
    private Deque<Mark> markStack = new ArrayDeque<>();
    private final TransactionalSupport transaction = new TransactionalSupport();

    @Override
    public void onBackout(MarkedCPUCCTNode node) {
        markStack.pop();
        parentMark = (Mark) (markStack.isEmpty() ? null : markStack.peek());
    }

    @Override
    public void onNode(MarkedCPUCCTNode node) {
        parentMark = (Mark) (markStack.isEmpty() ? null : markStack.peek());
        markStack.push(node.getMark());
    }

    @Override
    public void onStart() {
        transaction.beginTrans(true);
        parentMark = null;
        resetFlag = false;
    }

    @Override
    public void onStop() {
        markStack.clear();
        parentMark = null;
        transaction.endTrans();
    }

    public void beginTrans(final boolean mutable) {
        transaction.beginTrans(mutable);
    }

    public void endTrans() {
        if (resetFlag) {
            markStack.clear();
            resetFlag = false;
        }

        transaction.endTrans();
    }

    public synchronized void onReset() {
        resetFlag = true;
        transaction.endTrans();
    }

    @Override
    public void stateChanged(MarkingEngine instance) {
        reset();
    }

    protected final Mark getCurrentMark() {
        return (Mark) (markStack.isEmpty() ? Mark.DEFAULT : markStack.peek());
    }

    protected final Mark getParentMark() {
        return (parentMark != null) ? parentMark : Mark.DEFAULT;
    }

    protected synchronized boolean isReset() {
        return resetFlag;
    }

    private void reset() {
        resetFlag = true;
    }
}
