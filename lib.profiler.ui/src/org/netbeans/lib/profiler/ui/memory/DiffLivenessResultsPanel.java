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

package org.netbeans.lib.profiler.ui.memory;

import org.netbeans.lib.profiler.results.memory.LivenessMemoryResultsDiff;
import org.netbeans.lib.profiler.results.memory.LivenessMemoryResultsSnapshot;
import org.netbeans.lib.profiler.ui.components.table.ClassNameTableCellRenderer;
import org.netbeans.lib.profiler.ui.components.table.CustomBarCellRenderer;
import org.netbeans.lib.profiler.ui.components.table.DiffBarCellRenderer;
import org.netbeans.lib.profiler.ui.components.table.LabelTableCellRenderer;
import org.netbeans.lib.profiler.utils.StringUtils;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ResourceBundle;
import javax.swing.*;
import javax.swing.table.TableCellRenderer;


/**
 * This panel displays memory liveness diff.
 *
 * @author Jiri Sedlacek
 */
public class DiffLivenessResultsPanel extends SnapshotLivenessResultsPanel {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.netbeans.lib.profiler.ui.memory.Bundle"); // NOI18N
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
        if (e.getSource() == popupShowSource) {
            performDefaultAction(-1);
        }
    }

    protected CustomBarCellRenderer getBarCellRenderer() {
        return new DiffBarCellRenderer(diff.getMinTrackedLiveObjectsSizeDiff(), diff.getMaxTrackedLiveObjectsSizeDiff());
    }

    protected JPopupMenu getPopupMenu() {
        if (popup == null) {
            popup = new JPopupMenu();

            Font boldfont = popup.getFont().deriveFont(Font.BOLD);

            popupShowSource = new JMenuItem();
            popupShowSource.setText(GO_SOURCE_POPUP_ITEM);
            popupShowSource.setFont(boldfont);
            popup.add(popupShowSource);
            popupShowSource.addActionListener(this);
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
