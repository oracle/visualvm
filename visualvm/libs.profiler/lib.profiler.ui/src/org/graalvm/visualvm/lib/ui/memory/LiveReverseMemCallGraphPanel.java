/*
 * Copyright (c) 1997, 2025, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ResourceBundle;
import javax.swing.*;
import javax.swing.table.TableColumnModel;
import org.graalvm.visualvm.lib.jfluid.ProfilerEngineSettings;
import org.graalvm.visualvm.lib.jfluid.global.ProfilingSessionStatus;
import org.graalvm.visualvm.lib.jfluid.results.memory.MemoryCCTManager;
import org.graalvm.visualvm.lib.jfluid.results.memory.PresoObjAllocCCTNode;
import org.graalvm.visualvm.lib.jfluid.results.memory.PresoObjLivenessCCTNode;
import org.graalvm.visualvm.lib.jfluid.utils.StringUtils;
import org.graalvm.visualvm.lib.ui.UIConstants;
import org.graalvm.visualvm.lib.ui.UIUtils;
import org.graalvm.visualvm.lib.ui.components.JTreeTable;
import org.graalvm.visualvm.lib.ui.components.table.CustomBarCellRenderer;
import org.graalvm.visualvm.lib.ui.components.treetable.AbstractTreeTableModel;
import org.graalvm.visualvm.lib.ui.components.treetable.ExtendedTreeTableModel;
import org.graalvm.visualvm.lib.ui.components.treetable.JTreeTablePanel;
import org.graalvm.visualvm.lib.ui.components.treetable.TreeTableModel;


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
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.graalvm.visualvm.lib.ui.memory.Bundle"); // NOI18N
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
            customBarCellRenderer = new CustomBarCellRenderer(0, callGraphManager.getRootNode().totalObjSize);
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

                    public Class<?> getColumnClass(int column) {
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
                    private void showPopupMenu(MouseEvent e) {
                        treePath = treeTable.getTree().getPathForRow(treeTable.rowAtPoint(e.getPoint()));

                        if (treePath == null) {
                                treeTable.getTree().clearSelection();
                        } else {
                            treeTable.getTree().setSelectionPath(treePath);
                            popupMenu.show(e.getComponent(), e.getX(), e.getY());
                        }
                    }
                    public void mousePressed(MouseEvent e) {
                        if (e.isPopupTrigger()) showPopupMenu(e);
                    }

                    public void mouseReleased(MouseEvent e) {
                        if (e.isPopupTrigger()) showPopupMenu(e);
                    }

                    public void mouseClicked(MouseEvent e) {
                        treePath = treeTable.getTree().getPathForRow(treeTable.rowAtPoint(e.getPoint()));

                        if (treePath != null) {
                            treeTable.getTree().setSelectionPath(treePath);
                            if (SwingUtilities.isLeftMouseButton(e) && (e.getClickCount() == 2)) {
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
