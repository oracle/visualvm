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

import org.netbeans.lib.profiler.ProfilerEngineSettings;
import org.netbeans.lib.profiler.global.ProfilingSessionStatus;
import org.netbeans.lib.profiler.results.memory.MemoryCCTManager;
import org.netbeans.lib.profiler.results.memory.PresoObjAllocCCTNode;
import org.netbeans.lib.profiler.results.memory.PresoObjLivenessCCTNode;
import org.netbeans.lib.profiler.ui.UIConstants;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.*;
import org.netbeans.lib.profiler.ui.components.JTreeTable;
import org.netbeans.lib.profiler.ui.components.table.CustomBarCellRenderer;
import org.netbeans.lib.profiler.ui.components.treetable.AbstractTreeTableModel;
import org.netbeans.lib.profiler.ui.components.treetable.ExtendedTreeTableModel;
import org.netbeans.lib.profiler.ui.components.treetable.JTreeTablePanel;
import org.netbeans.lib.profiler.ui.components.treetable.TreeTableModel;
import org.netbeans.lib.profiler.utils.StringUtils;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ResourceBundle;
import javax.swing.*;
import javax.swing.table.TableColumnModel;


/**
 * A panel containing a reverse call graph for all allocations of instances of a given class
 *
 * No used at the moment!!!
 *
 * @author Misha Dmitriev
 * @author Jiri Sedlacek
 */
public class LiveReverseMemCallGraphPanel extends ReverseMemCallGraphPanel {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.netbeans.lib.profiler.ui.memory.Bundle"); // NOI18N
    private static final String NO_STACKS_MSG = messages.getString("LiveReverseMemCallGraphPanel_NoStacksMsg"); // NOI18N
    private static final String TREETABLE_ACCESS_NAME = messages.getString("LiveReverseMemCallGraphPanel_TreeTableAccessName"); // NOI18N
                                                                                                                                // -----

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected MemoryCCTManager callGraphManager;
    protected ProfilingSessionStatus status;
    protected int classId;
    private AbstractTreeTableModel abstractTreeTableModel;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public LiveReverseMemCallGraphPanel(ProfilingSessionStatus status, MemoryResUserActionsHandler actionsHandler) {
        super(actionsHandler, status.currentInstrType == ProfilerEngineSettings.INSTR_OBJECT_LIVENESS);
        this.status = status;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setCallGraph(MemoryCCTManager callGraphManager, int classId) {
        this.callGraphManager = callGraphManager;
        this.classId = classId;

        if (!callGraphManager.isEmpty()) {
            customBarCellRenderer = new CustomBarCellRenderer(0,
                                                              ((PresoObjAllocCCTNode) callGraphManager.getRootNode()).totalObjSize);
            columnRenderers[1] = customBarCellRenderer;
        }
    }

    public void prepareResults() {
        if (callGraphManager.isEmpty()) {
            removeAll();
            add(new JLabel(NO_STACKS_MSG), BorderLayout.CENTER);
        } else {
            abstractTreeTableModel = new AbstractTreeTableModel(callGraphManager.getRootNode(), 1, false) {
                    public int getColumnCount() {
                        return columnNames.length;
                    }

                    public String getColumnName(int column) {
                        return columnNames[column];
                    }

                    public Class getColumnClass(int column) {
                        if (column == 0) {
                            return TreeTableModel.class;
                        } else {
                            return Object.class;
                        }
                    }

                    public Object getValueAt(Object node, int column) {
                        long value;

                        if (extendedResults) {
                            PresoObjLivenessCCTNode pNode = (PresoObjLivenessCCTNode) node;

                            switch (column) {
                                case 0:
                                    return pNode.toString();
                                case 1:
                                    return new Long(pNode.totalObjSize);
                                case 2:
                                    value = ((PresoObjLivenessCCTNode) root).totalObjSize;

                                    return intFormat.format(pNode.totalObjSize) + " B ("
                                           + ((value == 0) ? "-%" : percentFormat.format((float) pNode.totalObjSize / (float) value))
                                           + ")"; // NOI18N
                                case 3:
                                    value = ((PresoObjLivenessCCTNode) root).nLiveObjects;

                                    return intFormat.format(pNode.nLiveObjects) + " ("
                                           + ((value == 0) ? "-%" : percentFormat.format((float) pNode.nLiveObjects / (float) value))
                                           + ")"; // NOI18N
                                case 4:
                                    return intFormat.format(pNode.nCalls);
                                case 5:
                                    return StringUtils.floatPerCentToString(pNode.avgObjectAge);
                                case 6:
                                    return intFormat.format(pNode.survGen);
                            }
                        } else {
                            PresoObjAllocCCTNode pNode = (PresoObjAllocCCTNode) node;

                            switch (column) {
                                case 0:
                                    return pNode.getNodeName();
                                case 1:
                                    return new Long(pNode.totalObjSize);
                                case 2:
                                    value = ((PresoObjAllocCCTNode) root).totalObjSize;

                                    return intFormat.format(pNode.totalObjSize) + " B ("
                                           + ((value == 0) ? "-%" : percentFormat.format((float) pNode.totalObjSize / (float) value))
                                           + ")"; // NOI18N
                                case 3:
                                    value = ((PresoObjAllocCCTNode) root).nCalls;

                                    return intFormat.format(pNode.nCalls) + " ("
                                           + ((value == 0) ? "-%" : percentFormat.format((float) pNode.nCalls / (float) value))
                                           + ")"; // NOI18N
                            }
                        }

                        return null;
                    }

                    public String getColumnToolTipText(int col) {
                        return columnToolTips[col];
                    }

                    public void sortByColumn(int column, boolean order) {
                        if (extendedResults) {
                            PresoObjLivenessCCTNode pRoot = (PresoObjLivenessCCTNode) root;

                            switch (column) {
                                case 0:
                                    pRoot.sortChildren(PresoObjLivenessCCTNode.SORT_BY_NAME, order);

                                    break;
                                case 1:
                                case 2:
                                    pRoot.sortChildren(PresoObjLivenessCCTNode.SORT_BY_LIVE_OBJ_SIZE, order);

                                    break;
                                case 3:
                                    pRoot.sortChildren(PresoObjLivenessCCTNode.SORT_BY_LIVE_OBJ_NUMBER, order);

                                    break;
                                case 4:
                                    pRoot.sortChildren(PresoObjLivenessCCTNode.SORT_BY_ALLOC_OBJ, order);

                                    break;
                                case 5:
                                    pRoot.sortChildren(PresoObjLivenessCCTNode.SORT_BY_AVG_AGE, order);

                                    break;
                                case 6:
                                    pRoot.sortChildren(PresoObjLivenessCCTNode.SORT_BY_SURV_GEN, order);

                                    break;
                            }
                        } else {
                            PresoObjAllocCCTNode pRoot = (PresoObjAllocCCTNode) root;

                            switch (column) {
                                case 0:
                                    pRoot.sortChildren(PresoObjAllocCCTNode.SORT_BY_NAME, order);

                                    break;
                                case 1:
                                case 2:
                                    pRoot.sortChildren(PresoObjAllocCCTNode.SORT_BY_ALLOC_OBJ_SIZE, order);

                                    break;
                                case 3:
                                    pRoot.sortChildren(PresoObjAllocCCTNode.SORT_BY_ALLOC_OBJ_NUMBER, order);

                                    break;
                            }
                        }
                    }
                    ;
                    public boolean getInitialSorting(int column) {
                        switch (column) {
                            case 0:
                                return true;
                            default:
                                return false;
                        }
                    }
                };

            treeTableModel = new ExtendedTreeTableModel(abstractTreeTableModel);

            treeTable = new JTreeTable(treeTableModel) {
                    public void doLayout() {
                        int columnsWidthsSum = 0;
                        int realFirstColumn = -1;

                        int index;
                        TableColumnModel colModel = getColumnModel();

                        for (int i = 0; i < treeTableModel.getColumnCount(); i++) {
                            index = treeTableModel.getRealColumn(i);

                            if (index == 0) {
                                realFirstColumn = i;
                            } else {
                                columnsWidthsSum += colModel.getColumn(i).getPreferredWidth();
                            }
                        }

                        if (realFirstColumn != -1) {
                            colModel.getColumn(realFirstColumn)
                                    .setPreferredWidth(Math.max(getWidth() - columnsWidthsSum, minNamesColumnWidth));
                        }

                        super.doLayout();
                    }
                    ;
                };
            treeTable.getAccessibleContext().setAccessibleName(TREETABLE_ACCESS_NAME);

            treeTable.setRowSelectionAllowed(true);
            treeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            treeTable.setGridColor(UIConstants.TABLE_VERTICAL_GRID_COLOR);
            treeTable.setSelectionBackground(UIConstants.TABLE_SELECTION_BACKGROUND_COLOR);
            treeTable.setSelectionForeground(UIConstants.TABLE_SELECTION_FOREGROUND_COLOR);
            treeTable.setShowHorizontalLines(UIConstants.SHOW_TABLE_HORIZONTAL_GRID);
            treeTable.setShowVerticalLines(UIConstants.SHOW_TABLE_VERTICAL_GRID);
            treeTable.setRowMargin(UIConstants.TABLE_ROW_MARGIN);
            treeTable.setRowHeight(UIUtils.getDefaultRowHeight() + 2);

            setColumnsData();

            UIUtils.autoExpandRoot(treeTable.getTree());
            UIUtils.makeTreeAutoExpandable(treeTable.getTree());

            treeTable.addMouseListener(new MouseAdapter() {
                    public void mousePressed(MouseEvent e) {
                        if (e.getModifiers() == InputEvent.BUTTON3_MASK) {
                            treePath = treeTable.getTree().getPathForRow(treeTable.rowAtPoint(e.getPoint()));

                            if (treePath != null) {
                                treeTable.getTree().setSelectionPath(treePath);
                            }
                        }
                    }

                    public void mouseClicked(MouseEvent e) {
                        treePath = treeTable.getTree().getPathForRow(treeTable.rowAtPoint(e.getPoint()));

                        if (treePath == null) {
                            if (e.getModifiers() == InputEvent.BUTTON3_MASK) {
                                treeTable.getTree().clearSelection();
                            }
                        } else {
                            if (e.getModifiers() == InputEvent.BUTTON3_MASK) {
                                popupMenu.show(e.getComponent(), e.getX(), e.getY());
                            } else if ((e.getModifiers() == InputEvent.BUTTON1_MASK) && (e.getClickCount() == 2)) {
                                if (treeTableModel.isLeaf(treePath.getPath()[treePath.getPath().length - 1])) {
                                    performDefaultAction(treePath);
                                }
                            }
                        }
                    }
                });

            removeAll();
            treeTablePanel = new JTreeTablePanel(treeTable);
            treeTablePanel.setCorner(JScrollPane.UPPER_RIGHT_CORNER, cornerButton);
            add(treeTablePanel, BorderLayout.CENTER);
        }
    }
}
