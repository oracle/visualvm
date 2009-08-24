/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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

package org.netbeans.lib.profiler.ui.cpu;

import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.results.ExportDataDumper;
import org.netbeans.lib.profiler.results.cpu.CPUResultsSnapshot;
import org.netbeans.lib.profiler.results.cpu.PrestimeCPUCCTNode;
import org.netbeans.lib.profiler.ui.UIConstants;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.JTreeTable;
import org.netbeans.lib.profiler.ui.components.table.CustomBarCellRenderer;
import org.netbeans.lib.profiler.ui.components.table.LabelBracketTableCellRenderer;
import org.netbeans.lib.profiler.ui.components.table.LabelTableCellRenderer;
import org.netbeans.lib.profiler.ui.components.table.SortableTableModel;
import org.netbeans.lib.profiler.ui.components.tree.EnhancedTreeCellRenderer;
import org.netbeans.lib.profiler.ui.components.tree.MethodNameTreeCellRenderer;
import org.netbeans.lib.profiler.ui.components.treetable.AbstractTreeTableModel;
import org.netbeans.lib.profiler.ui.components.treetable.ExtendedTreeTableModel;
import org.netbeans.lib.profiler.ui.components.treetable.JTreeTablePanel;
import org.netbeans.lib.profiler.ui.components.treetable.TreeTableModel;
import org.netbeans.lib.profiler.utils.StringUtils;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.FocusListener;
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
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import org.netbeans.lib.profiler.results.cpu.PrestimeCPUCCTNodeBacked;


/**
 * A display containing a CCT (calling context tree). Always appears together with flat profile display.
 *
 * @author Misha Dmitriev
 * @author Jiri Sedlacek
 * @author Ian Formanek
 */
public class CCTDisplay extends SnapshotCPUResultsPanel implements ScreenshotProvider {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.netbeans.lib.profiler.ui.cpu.Bundle"); // NOI18N
    private static final String METHOD_COLUMN_NAME = messages.getString("CCTDisplay_MethodColumnName"); // NOI18N
    private static final String METHOD_COLUMN_TOOLTIP = messages.getString("CCTDisplay_MethodColumnToolTip"); // NOI18N
    private static final String CLASS_COLUMN_NAME = messages.getString("CCTDisplay_ClassColumnName"); // NOI18N
    private static final String CLASS_COLUMN_TOOLTIP = messages.getString("CCTDisplay_ClassColumnToolTip"); // NOI18N
    private static final String PACKAGE_COLUMN_NAME = messages.getString("CCTDisplay_PackageColumnName"); // NOI18N
    private static final String PACKAGE_COLUMN_TOOLTIP = messages.getString("CCTDisplay_PackageColumnToolTip"); // NOI18N
    private static final String TIME_REL_COLUMN_NAME = messages.getString("CCTDisplay_TimeRelColumnName"); // NOI18N
    private static final String TIME_COLUMN_NAME = messages.getString("CCTDisplay_TimeColumnName"); // NOI18N
    private static final String TIME_CPU_COLUMN_NAME = messages.getString("CCTDisplay_TimeCpuColumnName"); // NOI18N
    private static final String INVOCATIONS_COLUMN_NAME = messages.getString("CCTDisplay_InvocationsColumnName"); // NOI18N
    private static final String TIME_REL_COLUMN_TOOLTIP = messages.getString("CCTDisplay_TimeRelColumnToolTip"); // NOI18N
    private static final String TIME_COLUMN_TOOLTIP = messages.getString("CCTDisplay_TimeColumnToolTip"); // NOI18N
    private static final String TIME_CPU_COLUMN_TOOLTIP = messages.getString("CCTDisplay_TimeCpuColumnToolTip"); // NOI18N
    private static final String INVOCATIONS_COLUMN_TOOLTIP = messages.getString("CCTDisplay_InvocationsColumnToolTip"); // NOI18N
    private static final String TREETABLE_ACCESS_NAME = messages.getString("CCTDisplay_TreeTableAccessName"); // NOI18N
                                                                                                              // -----
    private static final boolean DEBUG = System.getProperty("org.netbeans.lib.profiler.ui.cpu.CCTDisplay") != null; // NOI18N

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected JTreeTable treeTable;
    protected JTreeTablePanel treeTablePanel;
    protected boolean sortOrder;
    protected int sortingColumn;
    private AbstractTreeTableModel abstractTreeTableModel;
    private CPUSelectionHandler selectionHandler;
    private EnhancedTreeCellRenderer enhancedTreeCellRenderer = new MethodNameTreeCellRenderer();
    private ExtendedTreeTableModel treeTableModel;
    private ImageIcon leafIcon = new ImageIcon(CCTDisplay.class.getResource("/org/netbeans/lib/profiler/ui/resources/leaf.png")); // NOI18N
    private ImageIcon nodeIcon = new ImageIcon(CCTDisplay.class.getResource("/org/netbeans/lib/profiler/ui/resources/node.png")); // NOI18N
    private JButton cornerButton;
    private int minNamesColumnWidth; // minimal width of classnames columns

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public CCTDisplay(CPUResUserActionsHandler actionsHandler) {
        this(actionsHandler, null);
    }

    public CCTDisplay(CPUResUserActionsHandler actionsHandler, CPUSelectionHandler selectionHandler) {
        super(actionsHandler);

        this.selectionHandler = selectionHandler;

        enhancedTreeCellRenderer.setLeafIcon(leafIcon);
        enhancedTreeCellRenderer.setClosedIcon(nodeIcon);
        enhancedTreeCellRenderer.setOpenIcon(nodeIcon);

        minNamesColumnWidth = getFontMetrics(getFont()).charWidth('W') * 30; // NOI18N

        cornerPopup = new JPopupMenu();
        cornerButton = createHeaderPopupCornerButton(cornerPopup);

        setDefaultSorting();
    }

    public void exportData(int exportedFileType, ExportDataDumper eDD, boolean combine, String viewName) {
        percentFormat.setMaximumFractionDigits(2);
        percentFormat.setMinimumFractionDigits(2);
        PrestimeCPUCCTNodeBacked.setPercentFormat(percentFormat);
        switch (exportedFileType) {
            case 1: eDD.dumpData(getCSVHeader(",")); //NOI18N
                    ((PrestimeCPUCCTNodeBacked)abstractTreeTableModel.getRoot()).exportCSVData(",",exportedFileType, eDD);
                    if (!combine) {
                        eDD.close();
                    }
                    break;
            case 2: eDD.dumpData(getCSVHeader(";")); //NOI18N
                    ((PrestimeCPUCCTNodeBacked)abstractTreeTableModel.getRoot()).exportCSVData(";", exportedFileType, eDD);
                    if (!combine) {
                        eDD.close();
                    }
                    break;
            case 3: eDD.dumpData(getXMLHeader(combine, viewName));
                    ((PrestimeCPUCCTNodeBacked)abstractTreeTableModel.getRoot()).exportXMLData(eDD, "  ");
                    if (!combine) {
                        eDD.dumpDataAndClose(getXMLFooter(combine));
                    } else {
                        eDD.dumpData(getXMLFooter(combine));
                    }
                    break;
            case 4: eDD.dumpData(getHTMLHeader(viewName));
                    ((PrestimeCPUCCTNodeBacked)abstractTreeTableModel.getRoot()).exportHTMLData(eDD, 0);
                    if (!combine) {
                        eDD.dumpDataAndClose(getHTMLFooter(combine));
                    } else {
                        eDD.dumpData(getHTMLFooter(combine));
                    }

                    break;
        }
        percentFormat.setMaximumFractionDigits(1);
        percentFormat.setMinimumFractionDigits(0);
    }

    private StringBuffer getCSVHeader(String separator) {
        String newLine = "\r\n"; // NOI18N
        String quote = "\""; // NOI18N
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < (columnCount); i++) {
            result.append(quote+columnNames[i]+quote+separator);
        }
        result.append(newLine);
        return result;
    }

    private StringBuffer getHTMLHeader(String viewName) {
        StringBuffer result;
        result=new StringBuffer("<HTML><HEAD><meta http-equiv=\"Content-type\" content=\"text/html; charset=utf-8\" /><TITLE>"+viewName+"</TITLE><style type=\"text/css\">pre.method{overflow:auto;width:600;height:30;vertical-align:baseline}pre.parent{overflow:auto;width:400;height:30;vertical-align:baseline}td.method{text-align:left;width:600}td.parent{text-align:left;width:400}td.right{text-align:right;white-space:nowrap}</style></HEAD><BODY><table border=\"1\"><tr>"); // NOI18N
        result.append("<th>"+columnNames[0]+"</th><th>"+columnNames[1]+"</th><th>"+columnNames[2]+"</th><th>"+columnNames[3]+"</th></tr>"); //NOI18N
        return result;
    }

    private StringBuffer getHTMLFooter(boolean combine) {
        if (combine) {
            return new StringBuffer("</TABLE>"); //NOI18N
        } else {
            return new StringBuffer("</TABLE></BODY></HTML>"); //NOI18N
        }
    }

    private StringBuffer getXMLHeader(boolean combine, String viewName) {
        String newline = System.getProperty("line.separator"); // NOI18N
        StringBuffer result;
        result = new StringBuffer("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+newline+"<ExportedView Name=\""+viewName+"\" type=\""+((combine)?("combined"):("tree"))+"\">"+newline+"<tree>"+newline); // NOI18N
        return result;
    }

    private StringBuffer getXMLFooter(boolean combine) {
        String newline = System.getProperty("line.separator"); // NOI18N
        StringBuffer result;
        if (!combine) {
            result = new StringBuffer("</tree>"+newline+"</ExportedView>"); // NOI18N
        } else {
            result = new StringBuffer("</tree>"+newline+newline); // NOI18N
        }
        
        return result;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public int getCurrentThreadId() {
        return -10;
    } // A meaningless value to denote we don't display results for any
      // single thread in this window

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

    // NOTE: this method only sets sortingColumn, sortOrder and sortBy, it doesn't refresh UI!
    public void setDefaultSorting() {
        setSorting(1, SortableTableModel.SORT_ORDER_DESC);
    }

    public String getDefaultViewName() {
        return "cpu-calltree"; // NOI18N
    }

    // --- Find functionality stuff
    public void setFindString(String findString) {
        treeTable.setFindParameters(findString, 0);
    }

    public String getFindString() {
        return treeTable.getFindString();
    }

    public boolean isFindStringDefined() {
        return treeTable.isFindStringDefined();
    }

    public Object getResultsViewReference() {
        return treeTable;
    }

    // NOTE: this method only sets sortingColumn, sortOrder and sortBy, it doesn't refresh UI!
    public void setSorting(int sColumn, boolean sOrder) {
        if (sColumn == CommonConstants.SORTING_COLUMN_DEFAULT) {
            setDefaultSorting();
        } else {
            sortingColumn = sColumn;
            sortOrder = sOrder;

            //sortBy = getSortBy(sortingColumn);
        }
    }

    public int getSortingColumn() {
        if ((treeTable == null) || (treeTableModel == null)) {
            return CommonConstants.SORTING_COLUMN_DEFAULT;
        }

        return treeTableModel.getRealColumn(treeTable.getSortingColumn());
    }

    public boolean getSortingOrder() {
        if (treeTable == null) {
            return false;
        }

        return treeTable.getSortingOrder();
    }

    public void addResultsViewFocusListener(FocusListener listener) {
        treeTable.addFocusListener(listener);
    }

    public boolean findFirst() {
        return treeTable.findFirst();
    }

    public boolean findNext() {
        return treeTable.findNext();
    }

    public boolean findPrevious() {
        return treeTable.findPrevious();
    }

    public boolean fitsVisibleArea() {
        return !treeTablePanel.getScrollPane().getVerticalScrollBar().isEnabled();
    }

    public void prepareResults() {
        int currentColumnCount = snapshot.isCollectingTwoTimeStamps() ? 5 : 4;

        if (DEBUG) {
            columnCount++; // one extra column for jMethodID
        }

        if (columnCount != currentColumnCount) {
            initColumnsData();
        } else {
            if (treeTable != null) {
                saveColumnsData();
            }
        }

        if (treeTable != null) {
            sortingColumn = treeTable.getSortingColumn();
        }

        reset();
        initFirstColumnName();

        abstractTreeTableModel = new AbstractTreeTableModel(snapshot.getRootNode(currentView), sortingColumn, sortOrder) {
                public int getColumnCount() {
                    return columnCount;
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
                    if (!snapshot.isCollectingTwoTimeStamps()) {
                        if (column > 2) {
                            column += 1;
                        }
                    }

                    PrestimeCPUCCTNode pNode = (PrestimeCPUCCTNode) node;

                    switch (column) {
                        case 0:
                            return pNode;
                        case 1:
                            return getNodeTimeRel(pNode);
                        case 2:
                            return getNodeTime(pNode);
                        case 3:
                            return getNodeSecondaryTime(pNode);
                        case 4:
                            return getNodeInvocations(pNode);
                        case 5:
                            return getNodeMethodId(pNode);
                    }

                    return null;
                }

                public String getColumnToolTipText(int col) {
                    return columnToolTips[col];
                }

                private Float getNodeTimeRel(PrestimeCPUCCTNode pNode) {
                    return new Float(pNode.getTotalTime0InPerCent());
                }

                private String getNodeTime(PrestimeCPUCCTNode pNode) {
                    return StringUtils.mcsTimeToString(pNode.getTotalTime0()) + " ms (" // NOI18N
                           + percentFormat.format(pNode.getTotalTime0InPerCent() / 100) + ")"; // NOI18N
                }

                private String getNodeWaitTime(PrestimeCPUCCTNode pNode) {
                    return StringUtils.mcsTimeToString(pNode.getWaitTime0()) + " ms"; // NOI18N
                }

                private String getNodeSleepTime(PrestimeCPUCCTNode pNode) {
                    return StringUtils.mcsTimeToString(pNode.getSleepTime0()) + " ms"; // NOI18N
                }

                private String getNodeSecondaryTime(PrestimeCPUCCTNode pNode) {
                    return StringUtils.mcsTimeToString(pNode.getTotalTime1()) + " ms"; // NOI18N
                }

                private Integer getNodeInvocations(PrestimeCPUCCTNode pNode) {
                    return new Integer(pNode.getNCalls());
                }

                private Integer getNodeMethodId(PrestimeCPUCCTNode pNode) {
                    return new Integer(pNode.getMethodId());
                }

                public void sortByColumn(int column, boolean order) {
                    sortOrder = order;

                    //sortingColumn = column;
                    if (!snapshot.isCollectingTwoTimeStamps()) {
                        if (column > 2) {
                            column += 1;
                        }
                    }

                    PrestimeCPUCCTNode pRoot = (PrestimeCPUCCTNode) root;

                    //System.err.println(">> CCT: " + CCTDisplay.this.hashCode() + " sortByColumn " + column + ", " + order);
                    switch (column) {
                        case 0:
                            pRoot.sortChildren(PrestimeCPUCCTNode.SORT_BY_NAME, order);

                            break;
                        case 1:
                            pRoot.sortChildren(PrestimeCPUCCTNode.SORT_BY_TIME_0, order);

                            break;
                        case 2:
                            pRoot.sortChildren(PrestimeCPUCCTNode.SORT_BY_TIME_0, order);

                            break;
                        case 3:
                            pRoot.sortChildren(PrestimeCPUCCTNode.SORT_BY_TIME_1, order);

                            break;
                        case 4:
                            pRoot.sortChildren(PrestimeCPUCCTNode.SORT_BY_INVOCATIONS, order);

                            break;
                    }
                }
                ;
                public boolean getInitialSorting(int column) {
                    return (column == 0);
                }
            };

        treeTableModel = new ExtendedTreeTableModel(abstractTreeTableModel);

        if (columnsVisibility != null) {
            treeTableModel.setColumnsVisibility(columnsVisibility);
        }

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
        treeTable.getTree().setRootVisible(false);
        treeTable.getTree().setShowsRootHandles(true);
        treeTable.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                 .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "DEFAULT_ACTION"); // NOI18N
        treeTable.getActionMap().put("DEFAULT_ACTION",
                                     new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    performDefaultAction();
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

        UIUtils.autoExpandRoot(treeTable.getTree(), 2);
        UIUtils.makeTreeAutoExpandable(treeTable.getTree(), 2);

        treeTable.addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent e) {
                    if ((e.getKeyCode() == KeyEvent.VK_CONTEXT_MENU)
                            || ((e.getKeyCode() == KeyEvent.VK_F10) && (e.getModifiers() == InputEvent.SHIFT_MASK))) {
                        int selectedRow = treeTable.getSelectedRow();

                        if (selectedRow != -1) {
                            popupPath = treeTable.getTree().getPathForRow(selectedRow);

                            PrestimeCPUCCTNode node = (PrestimeCPUCCTNode) popupPath.getLastPathComponent();
                            enableDisablePopup(node);

                            Rectangle cellRect = treeTable.getCellRect(selectedRow, 0, false);
                            callGraphPopupMenu.show(e.getComponent(), ((cellRect.x + treeTable.getSize().width) > 50) ? 50 : 5,
                                                    cellRect.y);
                        }
                    }
                }
            });

        treeTable.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    if (e.getModifiers() == InputEvent.BUTTON3_MASK) {
                        popupPath = treeTable.getTree().getPathForRow(treeTable.rowAtPoint(e.getPoint()));

                        if (popupPath != null) {
                            treeTable.getTree().setSelectionPath(popupPath);
                        }
                    }
                }

                public void mouseClicked(MouseEvent e) {
                    popupPath = treeTable.getTree().getPathForRow(treeTable.rowAtPoint(e.getPoint()));

                    if (popupPath == null) {
                        if (e.getModifiers() == InputEvent.BUTTON3_MASK) {
                            treeTable.getTree().clearSelection();
                        }
                    } else {
                        PrestimeCPUCCTNode node = (PrestimeCPUCCTNode) popupPath.getLastPathComponent();

                        enableDisablePopup(node);

                        if (e.getModifiers() == InputEvent.BUTTON3_MASK) {
                            callGraphPopupMenu.show(e.getComponent(), e.getX(), e.getY());
                        } else if ((e.getModifiers() == InputEvent.BUTTON1_MASK) && (e.getClickCount() == 2)) {
                            if (treeTableModel.isLeaf(popupPath.getPath()[popupPath.getPath().length - 1])) {
                                showSourceForMethod(popupPath);
                            }
                        }
                    }
                }
            });

        treeTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    int selectedRow = treeTable.getSelectedRow();

                    if (selectedRow == -1) {
                        return;
                    }

                    popupPath = treeTable.getTree().getPathForRow(selectedRow);

                    PrestimeCPUCCTNode node = (PrestimeCPUCCTNode) popupPath.getLastPathComponent();

                    if (selectionHandler != null) {
                        selectionHandler.methodSelected(node.getThreadId(), node.getMethodId(), currentView);
                    }
                }
            });

        treeTablePanel = new JTreeTablePanel(treeTable);
        treeTablePanel.setCorner(JScrollPane.UPPER_RIGHT_CORNER, cornerButton);
        add(treeTablePanel, java.awt.BorderLayout.CENTER);
    }

    public void removeResultsViewFocusListener(FocusListener listener) {
        treeTable.removeFocusListener(listener);
    }

    public void requestFocus() {
        if (treeTable != null) {
            SwingUtilities.invokeLater(new Runnable() { // must be invoked lazily to override default focus of first component (top-right cornerButton)
                    public void run() {
                        treeTable.requestFocus();
                    }
                });
        }
    }

    public void reset() {
        if (treeTablePanel != null) {
            remove(treeTablePanel);
            treeTablePanel = null;
        }

        treeTable = null;
        abstractTreeTableModel = null;
        treeTableModel = null;
    }

    public boolean silentlyFindFirst() {
        return treeTable.silentlyFindFirst();
    }

    protected String getSelectedMethodName() {
        if ((treeTable == null) || (treeTableModel == null)) {
            return null;
        }

        if (treeTable.getSelectedRow() == -1) {
            return null;
        }

        PrestimeCPUCCTNode pNode = (PrestimeCPUCCTNode) treeTable.getTree().getSelectionPath().getLastPathComponent();

        if (pNode.isSelfTimeNode()) {
            // Self time represents the same method as its parent Node, and the parent's display name is
            // the actual method name
            pNode = (PrestimeCPUCCTNode) pNode.getParent();
        }

        if (pNode.isThreadNode()) {
            // For thread node, the method name is display name of its single child
            pNode = (PrestimeCPUCCTNode) pNode.getChildren()[0];
        }

        return treeTableModel.getValueAt(pNode, 0).toString();
    }

    protected void initColumnSelectorItems() {
        cornerPopup.removeAll();

        JCheckBoxMenuItem menuItem;

        for (int i = 0; i < columnCount; i++) {
            menuItem = new JCheckBoxMenuItem(columnNames[i]);
            menuItem.setActionCommand(new Integer(i).toString());
            addMenuItemListener(menuItem);

            if (treeTable != null) {
                menuItem.setState(treeTableModel.isRealColumnVisible(i));

                if (i == 0) {
                    menuItem.setEnabled(false);
                }
            } else {
                menuItem.setState(true);
            }

            cornerPopup.add(menuItem);
        }

        cornerPopup.pack();
    }

    private void setColumnsData() {
        int index;
        TableColumnModel colModel = treeTable.getColumnModel();

        treeTable.setTreeCellRenderer(enhancedTreeCellRenderer);
        colModel.getColumn(0).setPreferredWidth(minNamesColumnWidth);

        for (int i = 0; i < treeTableModel.getColumnCount(); i++) {
            index = treeTableModel.getRealColumn(i);

            if (index != 0) {
                colModel.getColumn(i).setPreferredWidth(columnWidths[index - 1]);
                colModel.getColumn(i).setCellRenderer(columnRenderers[index]);
            }
        }
    }

    private void addMenuItemListener(JCheckBoxMenuItem menuItem) {
        menuItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    boolean sortResults = false;
                    int column = Integer.parseInt(e.getActionCommand());
                    sortingColumn = treeTable.getSortingColumn();

                    int realSortingColumn = treeTableModel.getRealColumn(sortingColumn);
                    boolean isColumnVisible = treeTableModel.isRealColumnVisible(column);

                    // Current sorting column is going to be hidden
                    if ((isColumnVisible) && (column == realSortingColumn)) {
                        // Try to set next column as a sortingColumn. If currentSortingColumn is the last column, set previous
                        // column as a sorting Column (one column is always visible).
                        sortingColumn = ((sortingColumn + 1) == treeTableModel.getColumnCount()) ? (sortingColumn - 1)
                                                                                                 : (sortingColumn + 1);
                        realSortingColumn = treeTableModel.getRealColumn(sortingColumn);
                        sortResults = true;
                    }

                    treeTableModel.setRealColumnVisibility(column, !isColumnVisible);
                    treeTable.createDefaultColumnsFromModel();
                    treeTable.updateTreeTableHeader();
                    sortingColumn = treeTableModel.getVirtualColumn(realSortingColumn);

                    if (sortResults) {
                        sortOrder = treeTableModel.getInitialSorting(sortingColumn);
                        treeTableModel.sortByColumn(sortingColumn, sortOrder);
                        treeTable.updateTreeTable();
                    }

                    treeTable.setSortingColumn(sortingColumn);
                    treeTable.setSortingOrder(sortOrder);
                    treeTable.getTableHeader().repaint();
                    setColumnsData();

                    // TODO [ui-persistence]
                }
            });
    }

    private void enableDisablePopup(PrestimeCPUCCTNode node) {
        popupShowSource.setEnabled(isShowSourceAvailable() && ((node.getThreadId() != -1) && (node.getMethodId() > 0)));
        popupShowSubtree.setEnabled((node.getThreadId() != -1) && (node.getMethodId() > 0));
        popupShowReverse.setEnabled((node.getThreadId() != -1) && (node.getMethodId() > 0));
        popupAddToRoots.setEnabled(isAddToRootsAvailable() && ((node.getThreadId() != -1) && (node.getMethodId() > 0)));
        popupFind.setEnabled((node.getThreadId() != -1) && (node.getMethodId() > 0));
        // Allow the selection handler to change state of popupFind
        if (selectionHandler != null) selectionHandler.methodSelected(node.getThreadId(), node.getMethodId(), currentView);
    }

    private void initColumnsData() {
        columnCount = snapshot.isCollectingTwoTimeStamps() ? 5 : 4;

        if (DEBUG) {
            columnCount++; // one extra column for jMethodID
        }

        columnWidths = new int[columnCount - 1]; // Width of the first column fits to width
        columnNames = new String[columnCount];
        columnRenderers = new TableCellRenderer[columnCount];
        columnsVisibility = null;

        int idx = 0;
        columnNames = new String[columnCount];
        columnNames[idx++] = ""; // NOI18N
        columnNames[idx++] = TIME_REL_COLUMN_NAME;
        columnNames[idx++] = TIME_COLUMN_NAME;

        if (snapshot.isCollectingTwoTimeStamps()) {
            columnNames[idx++] = TIME_CPU_COLUMN_NAME;
        }

        columnNames[idx++] = INVOCATIONS_COLUMN_NAME;

        if (DEBUG) {
            columnNames[idx++] = "JMethodID"; // NOI18N
        }

        idx = 0;
        columnToolTips = new String[columnCount];
        columnToolTips[idx++] = ""; // NOI18N
        columnToolTips[idx++] = TIME_REL_COLUMN_TOOLTIP;
        columnToolTips[idx++] = TIME_COLUMN_TOOLTIP;

        if (snapshot.isCollectingTwoTimeStamps()) {
            columnToolTips[idx++] = TIME_CPU_COLUMN_TOOLTIP;
        }

        columnToolTips[idx++] = INVOCATIONS_COLUMN_TOOLTIP;

        if (DEBUG) {
            columnToolTips[idx++] = "JMethodID for the method"; // NOI18N
        }

        int maxWidth = getFontMetrics(getFont()).charWidth('W') * 12; // NOI18N // initial width of data columns

        CustomBarCellRenderer customBarCellRenderer = new CustomBarCellRenderer(0, 100);
        LabelTableCellRenderer labelTableCellRenderer = new LabelTableCellRenderer(JLabel.TRAILING);
        LabelBracketTableCellRenderer labelBracketTableCellRenderer = new LabelBracketTableCellRenderer(JLabel.TRAILING);

        columnRenderers[0] = null;

        // Inclusive (total) time bar
        columnWidths[1 - 1] = maxWidth;
        columnRenderers[1] = customBarCellRenderer;

        // Inclusive (total) time
        columnWidths[2 - 1] = maxWidth;
        columnRenderers[2] = labelBracketTableCellRenderer;

        for (int i = 3; i < columnCount; i++) {
            columnWidths[i - 1] = maxWidth;
            columnRenderers[i] = labelTableCellRenderer;
        }
    }

    private void initFirstColumnName() {
        switch (currentView) {
            case CPUResultsSnapshot.METHOD_LEVEL_VIEW:
                columnNames[0] = METHOD_COLUMN_NAME;
                columnToolTips[0] = METHOD_COLUMN_TOOLTIP;

                break;
            case CPUResultsSnapshot.CLASS_LEVEL_VIEW:
                columnNames[0] = CLASS_COLUMN_NAME;
                columnToolTips[0] = CLASS_COLUMN_TOOLTIP;

                break;
            case CPUResultsSnapshot.PACKAGE_LEVEL_VIEW:
                columnNames[0] = PACKAGE_COLUMN_NAME;
                columnToolTips[0] = PACKAGE_COLUMN_TOOLTIP;

                break;
        }

        if (treeTable != null) {
            treeTable.getTableHeader().repaint();
        }
    }

    private void saveColumnsData() {
        int index;
        TableColumnModel colModel = treeTable.getColumnModel();

        for (int i = 0; i < treeTableModel.getColumnCount(); i++) {
            index = treeTableModel.getRealColumn(i);

            if (index != 0) {
                columnWidths[index - 1] = colModel.getColumn(i).getPreferredWidth();
            }
        }

        columnsVisibility = null;
        columnsVisibility = treeTableModel.getColumnsVisibility();
    }

    /*
       public StringBuffer getResultsInCSVFormat(int callChainTypeCode, ExportDataDumper dataDumper) {
         return snapshot.getResultsInCSVFormat(callChainTypeCode, dataDumper);
       } */
}
