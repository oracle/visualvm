/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
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
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2012 Sun Microsystems, Inc.
 */
package org.netbeans.lib.profiler.ui.memory;

import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;
import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.results.memory.PresoObjAllocCCTNode;
import org.netbeans.lib.profiler.ui.UIConstants;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.JExtendedTable;
import org.netbeans.lib.profiler.ui.components.table.ClassNameTableCellRenderer;
import org.netbeans.lib.profiler.ui.components.table.CustomBarCellRenderer;
import org.netbeans.lib.profiler.ui.components.table.ExtendedTableModel;
import org.netbeans.lib.profiler.ui.components.table.LabelBracketTableCellRenderer;
import org.netbeans.lib.profiler.ui.components.table.SortableTableModel;


/**
 * This class implements presentation frames for Object Allocation Profiling.
 *
 * @author Misha Dmitriev
 * @author Ian Formanek
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
public abstract class SampledResultsPanel extends MemoryResultsPanel {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.netbeans.lib.profiler.ui.memory.Bundle"); // NOI18N
    private static final String FILTER_MENU_ITEM_NAME = messages.getString("SampledResultsPanel_FilterMenuItemName"); // NOI18N
    private static final String CLASS_COLUMN_NAME = messages.getString("SampledResultsPanel_ClassColumnName"); // NOI18N
    private static final String BYTES_REL_COLUMN_NAME = messages.getString("SampledResultsPanel_BytesRelColumnName"); // NOI18N
    private static final String BYTES_COLUMN_NAME = messages.getString("SampledResultsPanel_BytesColumnName"); // NOI18N
    private static final String OBJECTS_COLUMN_NAME = messages.getString("SampledResultsPanel_ObjectsColumnName"); // NOI18N
    private static final String CLASS_COLUMN_TOOLTIP = messages.getString("SampledResultsPanel_ClassColumnToolTip"); // NOI18N
    private static final String BYTES_REL_COLUMN_TOOLTIP = messages.getString("SampledResultsPanel_BytesRelColumnToolTip"); // NOI18N
    private static final String BYTES_COLUMN_TOOLTIP = messages.getString("SampledResultsPanel_BytesColumnToolTip"); // NOI18N
    private static final String OBJECTS_COLUMN_TOOLTIP = messages.getString("SampledResultsPanel_ObjectsColumnToolTip"); // NOI18N
    private static final String TABLE_ACCESS_NAME = messages.getString("SampledResultsPanel_TableAccessName"); // NOI18N
                                                                                                             // -----

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected int[] nTotalLiveObjects;
    protected long[] totalLiveObjectsSize;
    protected long nTotalLiveBytes;
    protected long nTotalClasses;
    private int initialSortingColumn;
    private int minNamesColumnWidth; // minimal width of classnames columns

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public SampledResultsPanel(MemoryResUserActionsHandler actionsHandler) {
        super(actionsHandler);

        setDefaultSorting();

        initColumnsData();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    // NOTE: this method only sets sortBy and sortOrder, it doesn't refresh UI!
    public void setDefaultSorting() {
        setSorting(1, SortableTableModel.SORT_ORDER_DESC);
    }

    // NOTE: this method only sets sortBy and sortOrder, it doesn't refresh UI!
    public void setSorting(int sColumn, boolean sOrder) {
        if (sColumn == CommonConstants.SORTING_COLUMN_DEFAULT) {
            setDefaultSorting();
        } else {
            initialSortingColumn = sColumn;
            sortBy = getSortBy(initialSortingColumn);
            sortOrder = sOrder;
        }
    }

    public int getSortingColumn() {
        if (resTableModel == null) {
            return CommonConstants.SORTING_COLUMN_DEFAULT;
        }

        return resTableModel.getRealColumn(resTableModel.getSortingColumn());
    }

    public boolean getSortingOrder() {
        if (resTableModel == null) {
            return false;
        }

        return resTableModel.getSortingOrder();
    }

    protected abstract JPopupMenu getPopupMenu();

    protected CustomBarCellRenderer getBarCellRenderer() {
//        return new CustomBarCellRenderer(0, maxValue);
        return new CustomBarCellRenderer(0, 100);
    }

    protected void getResultsSortedByLiveObjNumber() {
        //
        getResultsSortedByClassName(true); // Added because of lines toggling when switching between columns 1 and 2.
                                           // At first items must be sorted by class names to get defined initial state for
                                           // other sorting.

        int visibleLines = nInfoLines; // Zero or unprofiled classes are filtered, sorting will be applied only to live
                                       // data
                                       //

        nInfoLines = sortResults(nTotalLiveObjects, null, new long[][] { totalLiveObjectsSize }, null, 0, visibleLines, false);

        totalAllocations = 0;

        for (int i = 0; i < nInfoLines; i++) {
            totalAllocations += nTotalLiveObjects[i];
        }
    }

    protected void getResultsSortedByLiveObjSize() {
        //
        getResultsSortedByClassName(true); // Added because of lines toggling when switching between columns 1 and 2.
                                           // At first items must be sorted by class names to get defined initial state
                                           // for other sorting.

        int visibleLines = nInfoLines; // Zero or unprofiled classes are filtered, sorting will be applied only to live
                                       // data
                                       //

        nInfoLines = sortResults(totalLiveObjectsSize, new int[][] { nTotalLiveObjects }, null, null, 0, visibleLines, false);

        totalAllocations = 0;

        for (int i = 0; i < nInfoLines; i++) {
            totalAllocations += nTotalLiveObjects[i];
        }
    }

    protected void getResultsSortedByClassName(boolean presortOnly) {
        nInfoLines = sortResultsByClassName(new int[][] { nTotalLiveObjects }, new long[][] { totalLiveObjectsSize }, null,
                                            nTrackedItems, truncateZeroItems());

        if (!presortOnly) {
            totalAllocations = 0;

            for (int i = 0; i < nInfoLines; i++) {
                totalAllocations += nTotalLiveObjects[i];
            }
        }
    }

    protected JExtendedTable getResultsTable() {
        sortResults();

        if (resTable == null) {
            resTableModel = new ExtendedTableModel(new SortableTableModel() {
                    public String getColumnName(int col) {
                        return columnNames[col];
                    }

                    public int getRowCount() {
                        return nDisplayedItems;
                    }

                    public int getColumnCount() {
                        return columnNames.length;
                    }

                    public Class getColumnClass(int col) {
                        return columnTypes[col];
                    }

                    public Object getValueAt(int row, int col) {
                        return computeValueAt(row, col);
                    }

                    public String getColumnToolTipText(int col) {
                        return columnToolTips[col];
                    }

                    public void sortByColumn(int column, boolean order) {
                        sortBy = getSortBy(column);
                        sortOrder = order;

                        int selectedRow = resTable.getSelectedRow();
                        String selectedRowContents = null;

                        if (selectedRow != -1) {
                            selectedRowContents = (String) resTable.getValueAt(selectedRow, 0);
                        }

                        prepareResults();

                        if (selectedRowContents != null) {
                            resTable.selectRowByContents(selectedRowContents, 0, true);
                        }
                    }

                    /**
                     * @param column The table column index
                     * @return Initial sorting for the specified column - if true, ascending, if false descending
                     */
                    public boolean getInitialSorting(int column) {
                        switch (column) {
                            case 0:
                                return true;
                            default:
                                return false;
                        }
                    }
                });

            resTable = new JExtendedTable(resTableModel) {
                    public void doLayout() {
                        int columnsWidthsSum = 0;
                        int realFirstColumn = -1;

                        int index;

                        for (int i = 0; i < resTableModel.getColumnCount(); i++) {
                            index = resTableModel.getRealColumn(i);

                            if (index == 0) {
                                realFirstColumn = i;
                            } else {
                                columnsWidthsSum += getColumnModel().getColumn(i).getPreferredWidth();
                            }
                        }

                        if (realFirstColumn != -1) {
                            getColumnModel().getColumn(realFirstColumn)
                                .setPreferredWidth(Math.max(getWidth() - columnsWidthsSum, minNamesColumnWidth));
                        }

                        super.doLayout();
                    }
                    ;
                };
            resTable.getAccessibleContext().setAccessibleName(TABLE_ACCESS_NAME);

            resTableModel.setTable(resTable);
            resTableModel.setInitialSorting(initialSortingColumn, sortOrder);
            resTable.setRowSelectionAllowed(true);
            resTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            resTable.setGridColor(UIConstants.TABLE_VERTICAL_GRID_COLOR);
            resTable.setSelectionBackground(UIConstants.TABLE_SELECTION_BACKGROUND_COLOR);
            resTable.setSelectionForeground(UIConstants.TABLE_SELECTION_FOREGROUND_COLOR);
            resTable.setShowHorizontalLines(UIConstants.SHOW_TABLE_HORIZONTAL_GRID);
            resTable.setShowVerticalLines(UIConstants.SHOW_TABLE_VERTICAL_GRID);
            resTable.setRowMargin(UIConstants.TABLE_ROW_MARGIN);
            resTable.setRowHeight(UIUtils.getDefaultRowHeight() + 2);

            // Disable traversing table cells using TAB and Shift+TAB
            Set keys = new HashSet(resTable.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS));
            keys.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0));
            resTable.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, keys);

            keys = new HashSet(resTable.getFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS));
            keys.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_MASK));
            resTable.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, keys);

            setColumnsData();
        }

        return resTable;
    }

    protected Object computeValueAt(int row, int col) {
        int index = ((Integer) filteredToFullIndexes.get(row)).intValue();

        switch (col) {
            case 0:
                return sortedClassNames[index];
            case 1:
//                return new Long(totalAllocObjectsSize[index]);
                return (nTotalLiveBytes == 0) ? 0 : (double)totalLiveObjectsSize[index] / (double)nTotalLiveBytes * 100;
            case 2:
                return intFormat.format(totalLiveObjectsSize[index]) + " B (" // NOI18N
                       + ((nTotalLiveBytes == 0) ? "-%"
                                             : // NOI18N
                percentFormat.format((double) totalLiveObjectsSize[index] / (double) nTotalLiveBytes)) + ")"; // NOI18N
            case 3:
                return intFormat.format(nTotalLiveObjects[index]) + " (" // NOI18N
                       + ((nTotalClasses == 0) ? "-%"
                                               : // NOI18N
                percentFormat.format((double) nTotalLiveObjects[index] / (double) nTotalClasses)) + ")"; // NOI18N
            default:
                return null;
        }
    }

    protected void initColumnSelectorItems() {
        headerPopup.removeAll();

        JCheckBoxMenuItem menuItem;

        for (int i = 0; i < columnNames.length; i++) {
            menuItem = new JCheckBoxMenuItem(columnNames[i]);
            menuItem.setActionCommand(Integer.valueOf(i).toString());
            addMenuItemListener(menuItem);

            if (resTable != null) {
                menuItem.setState(resTableModel.isRealColumnVisible(i));

                if (i == 0) {
                    menuItem.setEnabled(false);
                }
            } else {
                menuItem.setState(true);
            }

            headerPopup.add(menuItem);
        }

        headerPopup.addSeparator();

        JCheckBoxMenuItem filterMenuItem = new JCheckBoxMenuItem(FILTER_MENU_ITEM_NAME);
        filterMenuItem.setActionCommand("Filter"); // NOI18N
        addMenuItemListener(filterMenuItem);

        if (filterComponent == null) {
            filterMenuItem.setState(true);
        } else {
            filterMenuItem.setState(filterComponent.getComponent().isVisible());
        }

        headerPopup.add(filterMenuItem);
        headerPopup.pack();
    }

    protected void initColumnsData() {
        int maxWidth = getFontMetrics(getFont()).charWidth('W') * 13; // NOI18N // initial width of data columns
        minNamesColumnWidth = getFontMetrics(getFont()).charWidth('W') * 30; // NOI18N

        ClassNameTableCellRenderer classNameTableCellRenderer = new ClassNameTableCellRenderer();
        LabelBracketTableCellRenderer labelBracketTableCellRenderer = new LabelBracketTableCellRenderer(JLabel.TRAILING);

        columnNames = new String[] { CLASS_COLUMN_NAME, BYTES_REL_COLUMN_NAME, BYTES_COLUMN_NAME, OBJECTS_COLUMN_NAME };
        columnToolTips = new String[] { CLASS_COLUMN_TOOLTIP, BYTES_REL_COLUMN_TOOLTIP, BYTES_COLUMN_TOOLTIP, OBJECTS_COLUMN_TOOLTIP };
        columnTypes = new Class[] { String.class, Number.class, String.class, String.class };
        columnRenderers = new TableCellRenderer[] {
                              classNameTableCellRenderer, null, labelBracketTableCellRenderer, labelBracketTableCellRenderer
                          };
        columnWidths = new int[] { maxWidth + 15, maxWidth, maxWidth };
    }

    protected boolean passesValueFilter(int i) {
        return ((((double) totalLiveObjectsSize[i] / (double) nTotalLiveBytes) * 100f) >= valueFilterValue);
    }

    protected void performDefaultAction(int classId) {
        showSourceForClass(classId);
    }

    private void setColumnsData() {
        barRenderer = getBarCellRenderer();

        TableColumnModel colModel = resTable.getColumnModel();
        colModel.getColumn(0).setPreferredWidth(minNamesColumnWidth);

        int index;

        for (int i = 0; i < colModel.getColumnCount(); i++) {
            index = resTableModel.getRealColumn(i);

            if (index == 0) {
                colModel.getColumn(i).setPreferredWidth(minNamesColumnWidth);
            } else {
                colModel.getColumn(i).setPreferredWidth(columnWidths[index - 1]);
            }

            if (index == 1) {
                colModel.getColumn(i).setCellRenderer(barRenderer);
            } else {
                colModel.getColumn(i).setCellRenderer(columnRenderers[index]);
            }
        }
    }

    private int getSortBy(int column) {
        switch (column) {
            case 0:
                return PresoObjAllocCCTNode.SORT_BY_NAME;
            case 1:
                return PresoObjAllocCCTNode.SORT_BY_ALLOC_OBJ_SIZE;
            case 2:
                return PresoObjAllocCCTNode.SORT_BY_ALLOC_OBJ_SIZE;
            case 3:
                return PresoObjAllocCCTNode.SORT_BY_ALLOC_OBJ_NUMBER;
        }

        return PresoObjAllocCCTNode.SORT_BY_ALLOC_OBJ_SIZE;
    }

    private void addMenuItemListener(JCheckBoxMenuItem menuItem) {
        menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (e.getActionCommand().equals("Filter")) { // NOI18N
                        filterComponent.getComponent().setVisible(!filterComponent.getComponent().isVisible());

                        // TODO [ui-persistence]
                        return;
                    }

                    saveColumnsData();

                    boolean sortResults = false;
                    int column = Integer.parseInt(e.getActionCommand());
                    int sortingColumn = resTableModel.getSortingColumn();
                    int realSortingColumn = resTableModel.getRealColumn(sortingColumn);
                    boolean isColumnVisible = resTableModel.isRealColumnVisible(column);

                    // Current sorting column is going to be hidden
                    if ((isColumnVisible) && (column == realSortingColumn)) {
                        // Try to set next column as a sortingColumn. If currentSortingColumn is the last column, set previous
                        // column asa sorting Column (one column is always visible).
                        sortingColumn = ((sortingColumn + 1) == resTableModel.getColumnCount()) ? (sortingColumn - 1)
                                                                                                : (sortingColumn + 1);
                        realSortingColumn = resTableModel.getRealColumn(sortingColumn);
                        sortResults = true;
                    }

                    resTableModel.setRealColumnVisibility(column, !isColumnVisible);
                    resTable.createDefaultColumnsFromModel();
                    resTableModel.setTable(resTable);
                    sortingColumn = resTableModel.getVirtualColumn(realSortingColumn);

                    if (sortResults) {
                        sortOrder = resTableModel.getInitialSorting(sortingColumn);
                        sortBy = getSortBy(realSortingColumn);
                        sortResults();
                        resTable.repaint();
                    }

                    resTableModel.setInitialSorting(sortingColumn, sortOrder);
                    resTable.getTableHeader().repaint();

                    setColumnsData();

                    // TODO [ui-persistence]
                }
            });
    }

    private void saveColumnsData() {
        int index;
        TableColumnModel colModel = resTable.getColumnModel();

        for (int i = 0; i < resTableModel.getColumnCount(); i++) {
            index = resTableModel.getRealColumn(i);

            if (index != 0) {
                columnWidths[index - 1] = colModel.getColumn(i).getPreferredWidth();
            }
        }
    }

    private void sortResults() {
        // This will sort results and produce sortedClassNames and sortedClassIds
        switch (sortBy) {
            case PresoObjAllocCCTNode.SORT_BY_ALLOC_OBJ_SIZE:
                getResultsSortedByLiveObjSize();

                break;
            case PresoObjAllocCCTNode.SORT_BY_ALLOC_OBJ_NUMBER:
                getResultsSortedByLiveObjNumber();

                break;
            case PresoObjAllocCCTNode.SORT_BY_NAME:
                getResultsSortedByClassName(false);

                break;
        }

        createFilteredIndexes();
    }
}
