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

import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.results.ExportDataDumper;
import org.netbeans.lib.profiler.results.memory.*;
import org.netbeans.lib.profiler.ui.UIConstants;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.JTreeTable;
import org.netbeans.lib.profiler.ui.components.table.CustomBarCellRenderer;
import org.netbeans.lib.profiler.ui.components.table.SortableTableModel;
import org.netbeans.lib.profiler.ui.components.treetable.AbstractTreeTableModel;
import org.netbeans.lib.profiler.ui.components.treetable.ExtendedTreeTableModel;
import org.netbeans.lib.profiler.ui.components.treetable.JTreeTablePanel;
import org.netbeans.lib.profiler.ui.components.treetable.TreeTableModel;
import org.netbeans.lib.profiler.utils.StringUtils;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumnModel;


/**
 * A panel containing a reverse call graph for all allocations of instances of a given class
 *
 * @author Misha Dmitriev
 * @author Jiri Sedlacek
 */
public class SnapshotReverseMemCallGraphPanel extends ReverseMemCallGraphPanel {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.netbeans.lib.profiler.ui.memory.Bundle"); // NOI18N
    private static final String NO_STACK_TRACES_MSG = messages.getString("SnapshotReverseMemCallGraphPanel_NoStackTracesMsg"); // NOI18N
    private static final String TREETABLE_ACCESS_NAME = messages.getString("SnapshotReverseMemCallGraphPanel_TreeTableAccessName"); // NOI18N
                                                                                                                                    // -----

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected int classId;
    private AbstractTreeTableModel abstractTreeTableModel;
    private JPanel noContentPanel;
    private MemoryCCTManager callGraphManager;
    private MemoryResultsSnapshot snapshot;
    private boolean initialSortingOrder;
    private int initialSortingColumn;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public SnapshotReverseMemCallGraphPanel(MemoryResultsSnapshot snapshot, MemoryResUserActionsHandler actionsHandler) {
        super(actionsHandler, snapshot instanceof LivenessMemoryResultsSnapshot);
        this.snapshot = snapshot;

        noContentPanel = new JPanel();
        noContentPanel.setLayout(new BorderLayout());
        noContentPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel noContentIcon = new JLabel(new javax.swing.ImageIcon(getClass()
                                                                        .getResource("/org/netbeans/lib/profiler/ui/resources/takeSnapshotMem.png"))); // NOI18N
        noContentIcon.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 5));
        noContentIcon.setVerticalAlignment(SwingConstants.TOP);
        noContentIcon.setEnabled(false);

        JTextArea noContentText = new JTextArea(NO_STACK_TRACES_MSG);
        noContentText.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        noContentText.setFont(noContentText.getFont().deriveFont(14));
        noContentText.setEditable(false);
        noContentText.setEnabled(false);
        noContentText.setWrapStyleWord(true);
        noContentText.setLineWrap(true);
        noContentText.setBackground(noContentPanel.getBackground());

        JPanel containerPanel = new JPanel(new BorderLayout());
        containerPanel.add(noContentIcon, BorderLayout.WEST);
        containerPanel.add(noContentText, BorderLayout.CENTER);
        noContentPanel.add(containerPanel, BorderLayout.NORTH);

        setDefaultSorting();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setClassId(int classId) {
        this.classId = classId;
        callGraphManager = new MemoryCCTManager(snapshot, classId, true);

        if (!callGraphManager.isEmpty()) {
            customBarCellRenderer = new CustomBarCellRenderer(0, ((MemoryCCTManager) callGraphManager).getRootNode().totalObjSize);
            columnRenderers[1] = customBarCellRenderer;
        }
    }

    public BufferedImage getCurrentViewScreenshot(boolean onlyVisibleArea) {
        if ((treeTablePanel == null) || (treeTable == null)) {
            return null;
        }

        if (onlyVisibleArea) {
            return UIUtils.createScreenshot(treeTablePanel.getScrollPane());
        } else {
            return UIUtils.createScreenshot(treeTable);
        }
    }

    private StringBuffer getCSVHeader(String separator) {
        String newLine = "\r\n"; // NOI18N
        String quote = "\""; // NOI18N
        StringBuffer result = new StringBuffer(quote+columnNames[0]+quote+separator);
        for (int i = 2; i < (columnNames.length); i++) {
            result.append(quote+columnNames[i]+quote+separator);
        }        
        result.append(messages.getString("SnapshotReverseMemCallGraphPanel_ExportAddedColumnName")+newLine);// NOI18N
        return result;
    }

    public void exportData(int exportedFileType, ExportDataDumper eDD, String viewName) {
        if (callGraphManager.getRootNode() instanceof PresoObjLivenessCCTNode) {
            ((PresoObjLivenessCCTNode) callGraphManager.getRootNode()).setDecimalFormat();
            switch (exportedFileType) {
                case 1: eDD.dumpData(getCSVHeader(",")); //NOI18N
                        ((PresoObjLivenessCCTNode) callGraphManager.getRootNode()).exportCSVData(",", 0, eDD); //NOI18N
                        eDD.close();
                        break;
                case 2: eDD.dumpData(getCSVHeader(";")); //NOI18N
                        ((PresoObjLivenessCCTNode) callGraphManager.getRootNode()).exportCSVData(";", 0, eDD); //NOI18N
                        eDD.close();
                        break;
                case 3: eDD.dumpData(getXMLHeader(viewName));
                        ((PresoObjLivenessCCTNode) callGraphManager.getRootNode()).exportXMLData(eDD, " "); //NOI18N
                        eDD.dumpDataAndClose(getXMLFooter());
                        break;
                case 4: eDD.dumpData(getHTMLHeader(viewName));
                        ((PresoObjLivenessCCTNode) callGraphManager.getRootNode()).exportHTMLData(eDD,0);
                        eDD.dumpDataAndClose(getHTMLFooter());
                        break;
            }

        } else if (callGraphManager.getRootNode() instanceof PresoObjAllocCCTNode) {
            switch (exportedFileType) {
                case 1: eDD.dumpData(getCSVHeader(",")); //NOI18N
                        ((PresoObjAllocCCTNode) callGraphManager.getRootNode()).exportCSVData(",", 0, eDD); //NOI18N
                        eDD.close();
                        break;
                case 2: eDD.dumpData(getCSVHeader(";")); //NOI18N
                        ((PresoObjAllocCCTNode) callGraphManager.getRootNode()).exportCSVData(";", 0, eDD); //NOI18N
                        eDD.close();
                        break;
                case 3: eDD.dumpData(getXMLHeader(viewName));
                        ((PresoObjAllocCCTNode) callGraphManager.getRootNode()).exportXMLData(eDD, " "); //NOI18N
                        eDD.dumpDataAndClose(getXMLFooter());
                        break;
                case 4: eDD.dumpData(getHTMLHeader(viewName));
                        ((PresoObjAllocCCTNode) callGraphManager.getRootNode()).exportHTMLData(eDD,0);
                        eDD.dumpDataAndClose(getHTMLFooter());
                        break;
            }
        }
    }

    private StringBuffer getHTMLHeader(String viewName) {
        StringBuffer result = new StringBuffer("<HTML><HEAD><meta http-equiv=\"Content-type\" content=\"text/html; charset=utf-8\" /><TITLE>"+viewName+"</TITLE><style type=\"text/css\">pre.method{overflow:auto;width:600;height:30;vertical-align:baseline}pre.parent{overflow:auto;width:400;height:30;vertical-align:baseline}td.method{text-align:left;width:600}td.parent{text-align:left;width:400}td.right{text-align:right;white-space:nowrap}</style></HEAD><BODY><table border=\"1\"><tr><th>"+columnNames[0]+"</th>"); // NOI18N
        for (int i = 2; i < (columnNames.length); i++) {
            result.append("<th>"+columnNames[i]+"</th>");
        }
        result.append("<th>"+messages.getString("SnapshotReverseMemCallGraphPanel_ExportAddedColumnName")+"</th></tr>"); //NOI18N
        return result;
    }

    private StringBuffer getHTMLFooter() {        
        return new StringBuffer("</TABLE></BODY></HTML>"); //NOI18N
    }

    private StringBuffer getXMLHeader(String viewName) {
        String newline = System.getProperty("line.separator"); // NOI18N
        StringBuffer result = new StringBuffer("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+newline+"<ExportedView Name=\""+viewName+"\" type=\"tree\">"+newline+"<tree>"+newline); // NOI18N
        return result;
    }

    private StringBuffer getXMLFooter() {
        String newline = System.getProperty("line.separator"); // NOI18N
        StringBuffer result = new StringBuffer("</tree>"+newline+"</ExportedView>"); // NOI18N
        return result;
    }
    
    // NOTE: this method only sets initialSortingColumn and initialSortingOrder, it doesn't refresh UI!
    public void setDefaultSorting() {
        setSorting(1, SortableTableModel.SORT_ORDER_DESC);
    }

    public boolean isEmpty() {
        return (callGraphManager == null) || callGraphManager.isEmpty();
    }

    // NOTE: this method only sets initialSortingColumn and initialSortingOrder, it doesn't refresh UI!
    public void setSorting(int sColumn, boolean sOrder) {
        if (sColumn == CommonConstants.SORTING_COLUMN_DEFAULT) {
            setDefaultSorting();
        } else {
            initialSortingColumn = sColumn;
            initialSortingOrder = sOrder;
        }
    }

    public boolean fitsVisibleArea() {
        return !treeTablePanel.getScrollPane().getVerticalScrollBar().isEnabled();
    }

    public boolean hasView() {
        return treeTable != null;
    }

    public void prepareResults() {
        if ((callGraphManager == null) || callGraphManager.isEmpty()) {
            removeAll();
            add(noContentPanel, BorderLayout.CENTER);
        } else {
            abstractTreeTableModel = new AbstractTreeTableModel(callGraphManager.getRootNode(), initialSortingColumn,
                                                                initialSortingOrder) {
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
                                    return pNode.getNodeName();
                                case 1:
                                    return new Long(pNode.totalObjSize);
                                case 2:
                                    value = ((PresoObjLivenessCCTNode) root).totalObjSize;

                                    return intFormat.format(pNode.totalObjSize) + " B ("  //NOI18N
                                           + ((value == 0) ? "-%" : percentFormat.format((float) pNode.totalObjSize / (float) value)) //NOI18N
                                           + ")"; // NOI18N
                                case 3:
                                    value = ((PresoObjLivenessCCTNode) root).nLiveObjects;

                                    return intFormat.format(pNode.nLiveObjects) + " (" //NOI18N
                                           + ((value == 0) ? "-%" : percentFormat.format((float) pNode.nLiveObjects / (float) value)) //NOI18N
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

                                    return intFormat.format(pNode.totalObjSize) + " B (" //NOI18N
                                           + ((value == 0) ? "-%" : percentFormat.format((float) pNode.totalObjSize / (float) value)) //NOI18N
                                           + ")"; // NOI18N
                                case 3:
                                    value = ((PresoObjAllocCCTNode) root).nCalls;

                                    return intFormat.format(pNode.nCalls) + " (" //NOI18N
                                           + ((value == 0) ? "-%" : percentFormat.format((float) pNode.nCalls / (float) value)) //NOI18N
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
            treeTable.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                     .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "DEFAULT_ACTION"); // NOI18N
            treeTable.getActionMap().put("DEFAULT_ACTION", new AbstractAction() { //NOI18N
                    public void actionPerformed(ActionEvent e) {
                        performDefaultAction(treePath);
                    }
                }); // NOI18N

            // Disable traversing table cells using TAB and Shift+TAB
            Set keys = new HashSet(treeTable.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS));
            keys.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0));
            treeTable.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, keys);

            keys = new HashSet(treeTable.getFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS));
            keys.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_MASK));
            treeTable.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, keys);

            setColumnsData();

            UIUtils.autoExpandRoot(treeTable.getTree());
            UIUtils.makeTreeAutoExpandable(treeTable.getTree());

            treeTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                    public void valueChanged(ListSelectionEvent e) {
                        int selectedRow = treeTable.getSelectedRow();

                        if (selectedRow == -1) {
                            return;
                        }

                        treePath = treeTable.getTree().getPathForRow(selectedRow);
                    }
                });

            treeTable.addKeyListener(new KeyAdapter() {
                    public void keyPressed(KeyEvent e) {
                        if ((e.getKeyCode() == KeyEvent.VK_CONTEXT_MENU)
                                || ((e.getKeyCode() == KeyEvent.VK_F10) && (e.getModifiers() == InputEvent.SHIFT_MASK))) {
                            int selectedRow = treeTable.getSelectedRow();

                            if (selectedRow != -1) {
                                treePath = treeTable.getTree().getPathForRow(selectedRow);

                                Rectangle cellRect = treeTable.getCellRect(selectedRow, 0, false);
                                popupMenu.show(e.getComponent(), ((cellRect.x + treeTable.getSize().width) > 50) ? 50 : 5,
                                               cellRect.y);
                            }
                        }
                    }
                });

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
