/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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
import org.graalvm.visualvm.lib.jfluid.results.cpu.DiffFlatProfileContainer;
import org.graalvm.visualvm.lib.jfluid.utils.StringUtils;
import org.graalvm.visualvm.lib.ui.components.table.DiffBarCellRenderer;
import org.graalvm.visualvm.lib.ui.components.table.LabelTableCellRenderer;

/**
 *
 * @author Jiri Sedlacek
 */
public class DiffFlatProfilePanel extends SnapshotFlatProfilePanel {

    public DiffFlatProfilePanel(CPUResUserActionsHandler actionsHandler, Boolean sampling) {
        super(actionsHandler, sampling);
    }


    protected boolean supportsReverseCallGraph() {
        return false;
    }

    protected boolean supportsSubtreeCallGraph() {
        return false;
    }


    protected Object computeValueAt(int row, int col) {
        long value;
        switch (col) {
            case 0:
                return flatProfileContainer.getMethodNameAtRow(row);
            case 1:
                return new Float(flatProfileContainer.getTimeInMcs0AtRow(row));
            case 2:
                value = flatProfileContainer.getTimeInMcs0AtRow(row);
                return (value > 0 ? "+" : "") + StringUtils.mcsTimeToString(value) + " ms"; // NOI18N
            case 3:
                if (collectingTwoTimeStamps) {
                    value = flatProfileContainer.getTimeInMcs1AtRow(row);
                    return (value > 0 ? "+" : "") + StringUtils.mcsTimeToString(value) + " ms"; // NOI18N
                } else {
                    value = flatProfileContainer.getTotalTimeInMcs0AtRow(row);
                    return (value > 0 ? "+" : "") + StringUtils.mcsTimeToString(value) + " ms"; // NOI18N
                }
            case 4:
                if (collectingTwoTimeStamps) {
                    value = flatProfileContainer.getTotalTimeInMcs0AtRow(row);
                    return (value > 0 ? "+" : "") + StringUtils.mcsTimeToString(value) + " ms"; // NOI18N
                } else {
                    value = flatProfileContainer.getNInvocationsAtRow(row);
                    return (value > 0 ? "+" : "") + intFormat.format(value); // NOI18N
                }
            case 5:
                value = flatProfileContainer.getTotalTimeInMcs1AtRow(row);
                return (value > 0 ? "+" : "") + StringUtils.mcsTimeToString(value) + " ms"; // NOI18N
            case 6:
                value = flatProfileContainer.getNInvocationsAtRow(row);
                return (value > 0 ? "+" : "") + intFormat.format(value); // NOI18N
            default:
                return null;
        }
    }
    
    protected void initColumnsData() {
        super.initColumnsData();
        columnRenderers[1] = new DiffBarCellRenderer(0, 0);
        columnRenderers[2] = new LabelTableCellRenderer(JLabel.TRAILING);
    }
    
    protected void obtainResults() {
        super.obtainResults();
        DiffFlatProfileContainer container = (DiffFlatProfileContainer)flatProfileContainer;
        DiffBarCellRenderer renderer = (DiffBarCellRenderer)columnRenderers[1];
        renderer.setMinimum(container.getMinTime());
        renderer.setMaximum(container.getMaxTime());
    }
    
}
