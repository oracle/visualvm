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

package org.graalvm.visualvm.lib.ui.cpu;

import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;
import org.graalvm.visualvm.lib.jfluid.results.CCTNode;
import org.graalvm.visualvm.lib.jfluid.results.cpu.CPUResultsSnapshot;


/**
 * Superclass providing common support for results displays displaying CPU results backed by CPUResultsSnapshot.
 *
 * @author Ian Formanek
 */
public abstract class SnapshotCPUResultsPanel extends CPUResultsPanel implements CommonConstants, ScreenshotProvider {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected CPUResultsSnapshot snapshot;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public SnapshotCPUResultsPanel(CPUResUserActionsHandler actionsHandler, Boolean sampling) {
        super(actionsHandler, sampling);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setDataToDisplay(CPUResultsSnapshot snapshot, int view) {
        this.snapshot = snapshot;
        this.currentView = view;
    }

    public CPUResultsSnapshot getSnapshot() {
        return snapshot;
    }

    /**
     * Gives a hint whether a tab panel is explicitly closeable
     * Subclasses should override the default implementation
     */
    protected boolean isCloseable() {
        return false;
    }

    protected String[] getMethodClassNameAndSig(int methodId, int currentView) {
        return snapshot.getMethodClassNameAndSig(methodId, currentView);
    }

    protected void showReverseCallGraph(int threadId, int methodId, int currentView, int sortingColumn, boolean sortingOrder) {
        actionsHandler.showReverseCallGraph(snapshot, threadId, methodId, currentView, sortingColumn, sortingOrder);
    }

    protected void showSubtreeCallGraph(CCTNode node, int currentView, int sortingColumn, boolean sortingOrder) {
        actionsHandler.showSubtreeCallGraph(snapshot, node, currentView, sortingColumn, sortingOrder);
    }

    protected boolean supportsReverseCallGraph() {
        return true;
    }

    protected boolean supportsSubtreeCallGraph() {
        return true;
    }
}
