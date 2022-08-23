/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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

import javax.swing.JLabel;
import org.graalvm.visualvm.lib.jfluid.results.cpu.CPUResultsDiff;
import org.graalvm.visualvm.lib.jfluid.utils.StringUtils;
import org.graalvm.visualvm.lib.ui.components.table.DiffBarCellRenderer;
import org.graalvm.visualvm.lib.ui.components.table.LabelTableCellRenderer;

/**
 *
 * @author Jiri Sedlacek
 */
public class DiffCCTDisplay extends CCTDisplay {

    public DiffCCTDisplay(CPUResUserActionsHandler actionsHandler, Boolean sampling) {
        super(actionsHandler, sampling);
    }


    protected boolean supportsReverseCallGraph() {
        return false;
    }

    protected boolean supportsSubtreeCallGraph() {
        return false;
    }


    protected Float getNodeTimeRel(long time, float percent) {
        return new Float(time);
    }

    protected String getNodeTime(long time, float percent) {
        return getNodeSecondaryTime(time);
    }

    protected String getNodeSecondaryTime(long time) {
        return (time > 0 ? "+" : "") + StringUtils.mcsTimeToString(time) + " ms"; // NOI18N
    }

    protected String getNodeInvocations(int nCalls) {
        return (nCalls > 0 ? "+" : "") + Integer.toString(nCalls); // NOI18N
    }

    protected void initColumnsData() {
        super.initColumnsData();
        columnRenderers[1] = new DiffBarCellRenderer(0, 0);
        columnRenderers[2] = new LabelTableCellRenderer(JLabel.TRAILING);
    }

    public void prepareResults() {
        super.prepareResults();
        DiffBarCellRenderer renderer = (DiffBarCellRenderer)columnRenderers[1];
        long bound = ((CPUResultsDiff)snapshot).getBound(currentView);
        renderer.setMinimum(-bound);
        renderer.setMaximum(bound);
    }

}
