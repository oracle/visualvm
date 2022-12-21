/*
 * Copyright (c) 1997, 2022, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.ui.memory;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ResourceBundle;
import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import org.graalvm.visualvm.lib.jfluid.results.memory.LivenessMemoryResultsDiff;
import org.graalvm.visualvm.lib.jfluid.results.memory.LivenessMemoryResultsSnapshot;
import org.graalvm.visualvm.lib.jfluid.utils.StringUtils;
import org.graalvm.visualvm.lib.profiler.api.GoToSource;
import org.graalvm.visualvm.lib.ui.components.table.ClassNameTableCellRenderer;
import org.graalvm.visualvm.lib.ui.components.table.CustomBarCellRenderer;
import org.graalvm.visualvm.lib.ui.components.table.DiffBarCellRenderer;
import org.graalvm.visualvm.lib.ui.components.table.LabelTableCellRenderer;


/**
 * This panel displays memory liveness diff.
 *
 * @author Jiri Sedlacek
 */
public class DiffLivenessResultsPanel extends SnapshotLivenessResultsPanel {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.graalvm.visualvm.lib.ui.memory.Bundle"); // NOI18N
    private static final String GO_SOURCE_POPUP_ITEM = messages.getString("SnapshotLivenessResultsPanel_GoSourcePopupItem"); // NOI18N
                                                                                                                             // -----

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private JMenuItem popupShowSource;
    private JPopupMenu popup;
    private LivenessMemoryResultsDiff diff;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public DiffLivenessResultsPanel(LivenessMemoryResultsSnapshot snapshot, MemoryResUserActionsHandler actionsHandler,
                                    int allocTrackEvery) {
        super(snapshot, actionsHandler, allocTrackEvery);
        diff = (LivenessMemoryResultsDiff) snapshot;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == popupShowSource && popupShowSource != null) {
            performDefaultAction(-1);
        }
    }

    protected CustomBarCellRenderer getBarCellRenderer() {
        return new DiffBarCellRenderer(diff.getMinTrackedLiveObjectsSizeDiff(), diff.getMaxTrackedLiveObjectsSizeDiff());
    }

    protected JPopupMenu getPopupMenu() {
        if (popup == null) {
            popup = new JPopupMenu();

            if (GoToSource.isAvailable()) {
                Font boldfont = popup.getFont().deriveFont(Font.BOLD);

                popupShowSource = new JMenuItem();
                popupShowSource.setText(GO_SOURCE_POPUP_ITEM);
                popupShowSource.setFont(boldfont);
                popup.add(popupShowSource);
                popupShowSource.addActionListener(this);
            }
        }

        return popup;
    }

    protected Object computeValueAt(int row, int col) {
        int index = ((Integer) filteredToFullIndexes.get(row)).intValue();

        switch (col) {
            case 0:
                return sortedClassNames[index];
            case 1:
                return new Long(trackedLiveObjectsSize[index]);
            case 2:
                return ((trackedLiveObjectsSize[index] > 0) ? "+" : "") + intFormat.format(trackedLiveObjectsSize[index]) + " B"; // NOI18N
            case 3:
                return ((nTrackedLiveObjects[index] > 0) ? "+" : "") + intFormat.format(nTrackedLiveObjects[index]); // NOI18N
            case 4:
                return ((nTrackedAllocObjects[index] > 0) ? "+" : "") + intFormat.format(nTrackedAllocObjects[index]); // NOI18N
            case 5:
                if (avgObjectAge[index] == 0) return "0.0"; // NOI18N
                // NOTE: StringUtils.floatPerCentToString() doesn't handle correctly negative values!
                else return ((avgObjectAge[index] > 0) ? "+" : "-") + StringUtils.floatPerCentToString(Math.abs(avgObjectAge[index])); // NOI18N
            case 6:
                return ((maxSurvGen[index] > 0) ? "+" : "") + intFormat.format(maxSurvGen[index]); // NOI18N
            case 7:
                return ((nTotalAllocObjects[index] > 0) ? "+" : "") + intFormat.format(nTotalAllocObjects[index]); // NOI18N
            default:
                return null;
        }
    }

    protected void initColumnsData() {
        super.initColumnsData();

        ClassNameTableCellRenderer classNameTableCellRenderer = new ClassNameTableCellRenderer();
        LabelTableCellRenderer labelTableCellRenderer = new LabelTableCellRenderer(JLabel.TRAILING);

        columnRenderers = new TableCellRenderer[] {
                              classNameTableCellRenderer, null, labelTableCellRenderer, labelTableCellRenderer,
                              labelTableCellRenderer, labelTableCellRenderer, labelTableCellRenderer, labelTableCellRenderer
                          };
    }

    protected void initDataUponResultsFetch() {
        super.initDataUponResultsFetch();

        if (barRenderer != null) {
            barRenderer.setMinimum(diff.getMinTrackedLiveObjectsSizeDiff());
            barRenderer.setMaximum(diff.getMaxTrackedLiveObjectsSizeDiff());
        }
    }

    protected boolean passesValueFilter(int i) {
        return true;
    }

    protected void performDefaultAction(int classId) {
        String className = null;
        int selectedRow = resTable.getSelectedRow();

        if (selectedRow != -1) {
            className = (String) resTable.getValueAt(selectedRow, 0).toString().replace("[]", ""); // NOI18N;
        }

        if (className != null) {
            actionsHandler.showSourceForMethod(className, null, null);
        }
    }

    protected boolean truncateZeroItems() {
        return false;
    }
}
