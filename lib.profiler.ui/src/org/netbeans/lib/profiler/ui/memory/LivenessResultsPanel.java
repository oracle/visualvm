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

package org.netbeans.lib.profiler.ui.memory;

import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.results.ExportDataDumper;
import org.netbeans.lib.profiler.results.memory.PresoObjLivenessCCTNode;
import org.netbeans.lib.profiler.ui.UIConstants;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.JExtendedTable;
import org.netbeans.lib.profiler.ui.components.table.ClassNameTableCellRenderer;
import org.netbeans.lib.profiler.ui.components.table.CustomBarCellRenderer;
import org.netbeans.lib.profiler.ui.components.table.ExtendedTableModel;
import org.netbeans.lib.profiler.ui.components.table.LabelBracketTableCellRenderer;
import org.netbeans.lib.profiler.ui.components.table.LabelTableCellRenderer;
import org.netbeans.lib.profiler.ui.components.table.SortableTableModel;
import org.netbeans.lib.profiler.utils.StringUtils;
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


/**
 * This class implements presentation frames for Object Liveness Profiling.
 *
 * @author Misha Dmitriev
 * @author Ian Formanek
 * @author Jiri Sedlacek
 */
public abstract class LivenessResultsPanel extends MemoryResultsPanel {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.netbeans.lib.profiler.ui.memory.Bundle"); // NOI18N
    private static final String FILTER_MENU_ITEM_NAME = messages.getString("LivenessResultsPanel_FilterMenuItemName"); // NOI18N
    private static final String CLASS_COLUMN_NAME = messages.getString("LivenessResultsPanel_ClassColumnName"); // NOI18N
    private static final String LIVE_BYTES_REL_COLUMN_NAME = messages.getString("LivenessResultsPanel_LiveBytesRelColumnName"); // NOI18N
    private static final String LIVE_BYTES_COLUMN_NAME = messages.getString("LivenessResultsPanel_LiveBytesColumnName"); // NOI18N
    private static final String LIVE_OBJECTS_COLUMN_NAME = messages.getString("LivenessResultsPanel_LiveObjectsColumnName"); // NOI18N
    private static final String ALLOC_OBJECTS_COLUMN_NAME = messages.getString("LivenessResultsPanel_AllocObjectsColumnName"); // NOI18N
    private static final String AVG_AGE_COLUMN_NAME = messages.getString("LivenessResultsPanel_AvgAgeColumnName"); // NOI18N
    private static final String SURVGEN_COLUMN_NAME = messages.getString("LivenessResultsPanel_SurvGenColumnName"); // NOI18N
    private static final String TOTAL_ALLOC_OBJECTS_COLUMN_NAME = messages.getString("LivenessResultsPanel_TotalAllocObjectsColumnName"); // NOI18N
    private static final String CLASS_COLUMN_TOOLTIP = messages.getString("LivenessResultsPanel_ClassColumnToolTip"); // NOI18N
    private static final String LIVE_BYTES_REL_COLUMN_TOOLTIP = messages.getString("LivenessResultsPanel_LiveBytesRelColumnToolTip"); // NOI18N
    private static final String LIVE_BYTES_COLUMN_TOOLTIP = messages.getString("LivenessResultsPanel_LiveBytesColumnToolTip"); // NOI18N
    private static final String LIVE_OBJECTS_COLUMN_TOOLTIP = messages.getString("LivenessResultsPanel_LiveObjectsColumnToolTip"); // NOI18N
    private static final String ALLOC_OBJECTS_COLUMN_TOOLTIP = messages.getString("LivenessResultsPanel_AllocObjectsColumnToolTip"); // NOI18N
    private static final String AVG_AGE_COLUMN_TOOLTIP = messages.getString("LivenessResultsPanel_AvgAgeColumnToolTip"); // NOI18N
    private static final String SURVGEN_COLUMN_TOOLTIP = messages.getString("LivenessResultsPanel_SurvGenColumnToolTip"); // NOI18N
    private static final String TOTAL_ALLOC_OBJECTS_COLUMN_TOOLTIP = messages.getString("LivenessResultsPanel_TotalAllocObjectsColumnToolTip"); // NOI18N
    private static final String TABLE_ACCESS_NAME = messages.getString("LivenessResultsPanel_TableAccessName"); // NOI18N    
                                                                                                                // -----

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected float[] avgObjectAge;
    protected int[] maxSurvGen;
    protected int[] nTotalAllocObjects; // # of allocated objects of each class
    protected long[] nTrackedAllocObjects; // # of allocated objects of each class (just tracked)
    protected int[] nTrackedLiveObjects; // # of live objects of each class
    protected long[] trackedLiveObjectsSize; // Byte side of live objects of each class
    protected int nInstrClasses;
    protected int trackedAllocObjects;
    protected int trackedLiveObjects;
    protected long nTotalTracked;
    protected long nTotalTrackedBytes;
    private int initialSortingColumn;
    private int minNamesColumnWidth; // minimal width of classnames columns

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public LivenessResultsPanel(MemoryResUserActionsHandler actionsHandler) {
        super(actionsHandler);

        setDefaultSorting();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /*
     * @return value 1-100 of percent of objects being tracked - to be used for column name rendering.
     */

    //  protected abstract int getPercentsTracked ();

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

    protected CustomBarCellRenderer getBarCellRenderer() {
        return new CustomBarCellRenderer(0, maxValue);
    }

    protected void getResultsSortedByAllocObj() {
        getResultsSortedByClassName(true); // Added because of lines toggling when switching between columns 1 and 2.
                                           // At first items must be sorted by class names to get defined initial state for
                                           // other sorting.

        int visibleLines = nInfoLines; // Zero or unprofiled classes are filtered, sorting will be applied only to live
                                       // data

        nInfoLines = sortResults(nTrackedAllocObjects, new int[][] { nTotalAllocObjects, nTrackedLiveObjects, maxSurvGen },
                                 new long[][] { trackedLiveObjectsSize }, new float[][] { avgObjectAge }, 0, visibleLines, false);

        totalAllocations = 0;
        trackedLiveObjects = trackedAllocObjects = 0;

        for (int i = 0; i < nInfoLines; i++) {
            trackedLiveObjects += nTrackedLiveObjects[i];
            trackedAllocObjects += nTrackedAllocObjects[i];
            totalAllocations += nTotalAllocObjects[i];
        }
    }

    protected void getResultsSortedByAvgAge() {
        getResultsSortedByClassName(true); // Added because of lines toggling when switching between columns 1 and 2.
                                           // At first items must be sorted by class names to get defined initial state for
                                           // other sorting.

        int visibleLines = nInfoLines; // Zero or unprofiled classes are filtered, sorting will be applied only to live
                                       // data

        nInfoLines = sortResults(avgObjectAge, new int[][] { nTotalAllocObjects, nTrackedLiveObjects, maxSurvGen },
                                 new long[][] { trackedLiveObjectsSize, nTrackedAllocObjects }, 0, visibleLines, false);

        totalAllocations = 0;
        trackedLiveObjects = trackedAllocObjects = 0;

        for (int i = 0; i < nInfoLines; i++) {
            trackedLiveObjects += nTrackedLiveObjects[i];
            trackedAllocObjects += nTrackedAllocObjects[i];
            totalAllocations += nTotalAllocObjects[i];
        }
    }

    protected void getResultsSortedByClassName(boolean presortOnly) {
        // Sort classes by name, initially moving to the bottom elements that have zero allocated objects
        nInfoLines = sortResultsByClassName(new int[][] { nTotalAllocObjects, nTrackedLiveObjects, maxSurvGen },
                                            new long[][] { trackedLiveObjectsSize, nTrackedAllocObjects },
                                            new float[][] { avgObjectAge }, //nInstrClasses, true);
        nTrackedItems, truncateZeroItems());

        if (!presortOnly) {
            totalAllocations = 0;
            trackedLiveObjects = trackedAllocObjects = 0;

            for (int i = 0; i < nInfoLines; i++) {
                trackedLiveObjects += nTrackedLiveObjects[i];
                trackedAllocObjects += nTrackedAllocObjects[i];
                totalAllocations += nTotalAllocObjects[i];
            }
        }
    }

    protected void getResultsSortedByLiveObjNumber() {
        getResultsSortedByClassName(true); // Added because of lines toggling when switching between columns 1 and 2.
                                           // At first items must be sorted by class names to get defined initial state for
                                           // other sorting.

        int visibleLines = nInfoLines; // Zero or unprofiled classes are filtered, sorting will be applied only to live
                                       // data

        // This will sort nTrackedLiveObjects, align with it the other arrays, and produce sortedClassNames and
        // sortedClassIds
        nInfoLines = sortResults(nTrackedLiveObjects, new int[][] { nTotalAllocObjects, maxSurvGen },
                                 new long[][] { trackedLiveObjectsSize, nTrackedAllocObjects }, new float[][] { avgObjectAge },
                                 0, visibleLines, false);

        totalAllocations = 0;
        trackedLiveObjects = trackedAllocObjects = 0;

        for (int i = 0; i < nInfoLines; i++) {
            trackedLiveObjects += nTrackedLiveObjects[i];
            trackedAllocObjects += nTrackedAllocObjects[i];
            totalAllocations += nTotalAllocObjects[i];
        }
    }

    protected void getResultsSortedByLiveObjSize() {
        getResultsSortedByClassName(true); // Added because of lines toggling when switching between columns 1 and 2.
                                           // At first items must be sorted by class names to get defined initial state for
                                           // other sorting.

        int visibleLines = nInfoLines; // Zero or unprofiled classes are filtered, sorting will be applied only to live
                                       // data

        // This will sort trackedLiveObjectsSize, align with it nTrackedLiveObjects and nTotalAllocObjects, and produce
        // sortedClassNames and sortedClassIds
        nInfoLines = sortResults(trackedLiveObjectsSize, new int[][] { nTotalAllocObjects, nTrackedLiveObjects, maxSurvGen },
                                 new long[][] { nTrackedAllocObjects }, new float[][] { avgObjectAge }, 0, visibleLines, false);

        totalAllocations = 0;
        trackedLiveObjects = trackedAllocObjects = 0;

        for (int i = 0; i < nInfoLines; i++) {
            trackedLiveObjects += nTrackedLiveObjects[i];
            trackedAllocObjects += nTrackedAllocObjects[i];
            totalAllocations += nTotalAllocObjects[i];
        }
    }

    protected void getResultsSortedBySurvGen() {
        getResultsSortedByClassName(true); // Added because of lines toggling when switching between columns 1 and 2.
                                           // At first items must be sorted by class names to get defined initial state for
                                           // other sorting.

        int visibleLines = nInfoLines; // Zero or unprofiled classes are filtered, sorting will be applied only to live
                                       // data

        nInfoLines = sortResults(maxSurvGen, new int[][] { nTotalAllocObjects, nTrackedLiveObjects },
                                 new long[][] { trackedLiveObjectsSize, nTrackedAllocObjects }, new float[][] { avgObjectAge },
                                 0, visibleLines, false);

        // Now sort each subgroup where the number of surviving generations is the same and > 0, by the total live obj size
        int curSurvGen = maxSurvGen[0];
        int top = 0;

        while (curSurvGen > 0) {
            int bottom = top;

            while ((maxSurvGen[bottom] == curSurvGen) && (bottom < nInfoLines)) {
                bottom++;
            }

            if (bottom > top) {
                sortResults(trackedLiveObjectsSize, new int[][] { nTrackedLiveObjects, nTotalAllocObjects, maxSurvGen },
                            new long[][] { nTrackedAllocObjects }, new float[][] { avgObjectAge }, top, bottom - top, false);
                top = bottom;
                curSurvGen = maxSurvGen[bottom];
            }
        }

        totalAllocations = 0;
        trackedLiveObjects = trackedAllocObjects = 0;

        for (int i = 0; i < nInfoLines; i++) {
            trackedLiveObjects += nTrackedLiveObjects[i];
            trackedAllocObjects += nTrackedAllocObjects[i];
            totalAllocations += nTotalAllocObjects[i];
        }
    }

    protected void getResultsSortedByTotalAllocObj() {
        getResultsSortedByClassName(true); // Added because of lines toggling when switching between columns 1 and 2.
                                           // At first items must be sorted by class names to get defined initial state for
                                           // other sorting.

        int visibleLines = nInfoLines; // Zero or unprofiled classes are filtered, sorting will be applied only to live
                                       // data

        nInfoLines = sortResults(nTotalAllocObjects, new int[][] { nTrackedLiveObjects, maxSurvGen },
                                 new long[][] { trackedLiveObjectsSize, nTrackedAllocObjects }, new float[][] { avgObjectAge },
                                 0, visibleLines, false);

        totalAllocations = 0;
        trackedLiveObjects = trackedAllocObjects = 0;

        for (int i = 0; i < nInfoLines; i++) {
            trackedLiveObjects += nTrackedLiveObjects[i];
            trackedAllocObjects += nTrackedAllocObjects[i];
            totalAllocations += nTotalAllocObjects[i];
        }
    }

    protected JExtendedTable getResultsTable() {
        trackedLiveObjects = trackedAllocObjects = 0;
        totalAllocations = 0;

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
                        // The main purpose of this method is to make numeric values aligned properly inside table cells
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

            resTableModel.setRealColumnVisibility(7, false);

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
                return new Long(trackedLiveObjectsSize[index]);
            case 2:
                return intFormat.format(trackedLiveObjectsSize[index]) + " B (" // NOI18N
                       + ((nTotalTrackedBytes == 0) ? "-%" //NOI18N
                                                    : 
                percentFormat.format((float) trackedLiveObjectsSize[index] / (float) nTotalTrackedBytes)) + ")"; // NOI18N
            case 3:
                return intFormat.format(nTrackedLiveObjects[index]) + " (" // NOI18N
                       + ((nTotalTracked == 0) ? "-%"  //NOI18N
                                               : 
                percentFormat.format((float) nTrackedLiveObjects[index] / (float) nTotalTracked)) + ")"; // NOI18N
            case 4:
                return intFormat.format(nTrackedAllocObjects[index]);
            case 5:
                return StringUtils.floatPerCentToString(avgObjectAge[index]);
            case 6:
                return intFormat.format(maxSurvGen[index]);
            case 7:
                return intFormat.format(nTotalAllocObjects[index]);
            default:
                return null;
        }
    }

    protected void initColumnSelectorItems() {
        headerPopup.removeAll();

        JCheckBoxMenuItem menuItem;

        for (int i = 0; i < columnNames.length; i++) {
            menuItem = new JCheckBoxMenuItem(columnNames[i]);
            menuItem.setActionCommand(new Integer(i).toString());
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
            filterMenuItem.setState(filterComponent.isVisible());
        }

        headerPopup.add(filterMenuItem);

        headerPopup.pack();
    }

    protected void initColumnsData() {
        int maxWidth = getFontMetrics(getFont()).charWidth('W') * 10; // NOI18N // initial width of data columns
        minNamesColumnWidth = getFontMetrics(getFont()).charWidth('W') * 30; // NOI18N

        ClassNameTableCellRenderer classNameTableCellRenderer = new ClassNameTableCellRenderer();
        LabelTableCellRenderer labelTableCellRenderer = new LabelTableCellRenderer(JLabel.TRAILING);
        LabelBracketTableCellRenderer labelBracketTableCellRenderer = new LabelBracketTableCellRenderer(JLabel.TRAILING);

        columnNames = new String[] {
                          CLASS_COLUMN_NAME, // - " + getPercentsTracked() + "% Tracked",
        LIVE_BYTES_REL_COLUMN_NAME, LIVE_BYTES_COLUMN_NAME, LIVE_OBJECTS_COLUMN_NAME, ALLOC_OBJECTS_COLUMN_NAME,
                          AVG_AGE_COLUMN_NAME, SURVGEN_COLUMN_NAME, TOTAL_ALLOC_OBJECTS_COLUMN_NAME
                      };

        columnToolTips = new String[] {
                             CLASS_COLUMN_TOOLTIP, // - "+getPercentsTracked()+"% of all allocated objets are displayed\",",
        LIVE_BYTES_REL_COLUMN_TOOLTIP, // - "+getPercentsTracked()+"% Tracked",
        LIVE_BYTES_COLUMN_TOOLTIP, // - "+getPercentsTracked()+"% Tracked",
        LIVE_OBJECTS_COLUMN_TOOLTIP, // - "+getPercentsTracked()+"% Tracked",
        ALLOC_OBJECTS_COLUMN_TOOLTIP, // - "+getPercentsTracked()+"% Tracked",
        AVG_AGE_COLUMN_TOOLTIP, // - "+getPercentsTracked()+"% Tracked",
        SURVGEN_COLUMN_TOOLTIP, // - "+ getPercentsTracked()+"% Tracked",
        TOTAL_ALLOC_OBJECTS_COLUMN_TOOLTIP
                         };
        columnTypes = new Class[] {
                          String.class, Number.class, String.class, String.class, String.class, Number.class, Number.class,
                          String.class
                      };
        columnRenderers = new TableCellRenderer[] {
                              classNameTableCellRenderer, null, labelBracketTableCellRenderer, labelBracketTableCellRenderer,
                              labelTableCellRenderer, labelTableCellRenderer, labelTableCellRenderer, labelTableCellRenderer
                          };
        columnWidths = new int[] { maxWidth, maxWidth, maxWidth, maxWidth, maxWidth, maxWidth, maxWidth, maxWidth };
    }

    protected boolean passesValueFilter(int i) {
        return ((((double) trackedLiveObjectsSize[i] / (double) nTotalTrackedBytes) * 100f) >= valueFilterValue);
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
                return PresoObjLivenessCCTNode.SORT_BY_NAME;
            case 1:
                return PresoObjLivenessCCTNode.SORT_BY_LIVE_OBJ_SIZE;
            case 2:
                return PresoObjLivenessCCTNode.SORT_BY_LIVE_OBJ_SIZE;
            case 3:
                return PresoObjLivenessCCTNode.SORT_BY_LIVE_OBJ_NUMBER;
            case 4:
                return PresoObjLivenessCCTNode.SORT_BY_ALLOC_OBJ;
            case 5:
                return PresoObjLivenessCCTNode.SORT_BY_AVG_AGE;
            case 6:
                return PresoObjLivenessCCTNode.SORT_BY_SURV_GEN;
            case 7:
                return PresoObjLivenessCCTNode.SORT_BY_TOTAL_ALLOC_OBJ;
        }

        return PresoObjLivenessCCTNode.SORT_BY_LIVE_OBJ_SIZE;
    }

    private void addMenuItemListener(JCheckBoxMenuItem menuItem) {
        menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (e.getActionCommand().equals("Filter")) { // NOI18N
                        filterComponent.setVisible(!filterComponent.isVisible());

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
                        // column as a sorting Column (one column is always visible).
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
        switch (sortBy) {
            case PresoObjLivenessCCTNode.SORT_BY_NAME:
                getResultsSortedByClassName(false);

                break;
            case PresoObjLivenessCCTNode.SORT_BY_LIVE_OBJ_SIZE:
                getResultsSortedByLiveObjSize();

                break;
            case PresoObjLivenessCCTNode.SORT_BY_LIVE_OBJ_NUMBER:
                getResultsSortedByLiveObjNumber();

                break;
            case PresoObjLivenessCCTNode.SORT_BY_ALLOC_OBJ:
                getResultsSortedByAllocObj();

                break;
            case PresoObjLivenessCCTNode.SORT_BY_AVG_AGE:
                getResultsSortedByAvgAge();

                break;
            case PresoObjLivenessCCTNode.SORT_BY_SURV_GEN:
                getResultsSortedBySurvGen();

                break;
            case PresoObjLivenessCCTNode.SORT_BY_TOTAL_ALLOC_OBJ:
                getResultsSortedByTotalAllocObj();

                break;
        }

        createFilteredIndexes();
    }

    public void exportData(int typeOfFile, ExportDataDumper eDD, String viewName) {
        intFormat.setMaximumFractionDigits(2);
        intFormat.setMinimumFractionDigits(2);
        switch (typeOfFile) {
            case 1: exportCSV(",", eDD); break; // NOI18N
            case 2: exportCSV(";", eDD); break; // NOI18N
            case 3: exportXML(eDD, viewName); break;
            case 4: exportHTML(eDD, viewName); break;
        }
        intFormat.setMaximumFractionDigits(1);
        intFormat.setMinimumFractionDigits(0);
    }

    private void exportHTML(ExportDataDumper eDD, String viewName) {
         // Header
        StringBuffer result = new StringBuffer("<HTML><HEAD><meta http-equiv=\"Content-type\" content=\"text/html; charset=utf-8\" /><TITLE>"+viewName+"</TITLE></HEAD><BODY><TABLE border=\"1\"><tr>"); // NOI18N
        for (int i = 0; i < (columnNames.length-1); i++) {
            if (!(columnRenderers[i]==null)) {
                result.append("<th>"+columnNames[i]+"</th>"); // NOI18N
            }
        }
        result.append("</tr>"); // NOI18N
        eDD.dumpData(result);

        for (int i=0; i < nTrackedItems; i++) {

            result = new StringBuffer("<tr><td>"+replaceHTMLCharacters(sortedClassNames[i])+"</td>"); // NOI18N
            result.append("<td align=\"right\">"+trackedLiveObjectsSize[i]+"</td>"); // NOI18N
            result.append("<td align=\"right\">"+nTrackedLiveObjects[i]+"</td>"); // NOI18N
            result.append("<td align=\"right\">"+nTrackedAllocObjects[i]+"</td>"); // NOI18N
            result.append("<td align=\"char\" char=\".\">"+intFormat.format((double)avgObjectAge[i])+"</td>"); // NOI18N
            result.append("<td align=\"right\">"+maxSurvGen[i]+"</td></tr>"); // NOI18N
            eDD.dumpData(result);
        }
        eDD.dumpDataAndClose(new StringBuffer(" </TABLE></BODY></HTML>")); // NOI18N
    }

    private void exportXML(ExportDataDumper eDD, String viewName) {
         // Header
        String newline = System.getProperty("line.separator"); // NOI18N
        StringBuffer result = new StringBuffer("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+newline+"<ExportedView Name=\""+viewName+"\">"+newline); // NOI18N
        result.append(" <TableData NumRows=\""+nTrackedItems+"\" NumColumns=\"6\">"+newline); // NOI18N
        result.append("<TableHeader>"); // NOI18N
        for (int i = 0; i < (columnNames.length-1); i++) {
            if (!(columnRenderers[i]==null)) {
                result.append("  <TableColumn><![CDATA["+columnNames[i]+"]]></TableColumn>"+newline); // NOI18N
            }
        }
        result.append("</TableHeader>"); // NOI18N
        eDD.dumpData(result);

        // Data
        for (int i=0; i < nTrackedItems; i++) {
            result = new StringBuffer("  <TableRow>"+newline);
            result.append("   <TableColumn><![CDATA["+sortedClassNames[i]+"]]></TableColumn>"+newline);
            result.append("   <TableColumn><![CDATA["+trackedLiveObjectsSize[i]+"]]></TableColumn>"+newline);
            result.append("   <TableColumn><![CDATA["+nTrackedLiveObjects[i]+"]]></TableColumn>"+newline);
            result.append("   <TableColumn><![CDATA["+nTrackedAllocObjects[i]+"]]></TableColumn>"+newline);
            result.append("   <TableColumn><![CDATA["+intFormat.format((double)avgObjectAge[i])+"]]></TableColumn>"+newline);
            result.append("   <TableColumn><![CDATA["+maxSurvGen[i]+"]]></TableColumn>"+newline+"  </TableRow>"+newline);
            eDD.dumpData(result);
        }
        eDD.dumpDataAndClose(new StringBuffer(" </TableData>"+newline+"</ExportedView>"));
    }

    private void exportCSV(String separator, ExportDataDumper eDD) {
        // Header
        StringBuffer result = new StringBuffer();
        String newLine = "\r\n"; // NOI18N
        String quote = "\""; // NOI18N

        for (int i = 0; i < (columnNames.length-1); i++) {
            if (!(columnRenderers[i]==null)) {
                result.append(quote+columnNames[i]+quote+separator);
            }
        }
        result.deleteCharAt(result.length()-1);
        result.append(newLine);
        eDD.dumpData(result);

        // Data
        
        for (int i=0; i < nTrackedItems; i++) {
            result = new StringBuffer();
            result.append(quote+sortedClassNames[i]+quote+separator);
            result.append(quote+trackedLiveObjectsSize[i]+quote+separator);
            result.append(quote+nTrackedLiveObjects[i]+quote+separator);
            result.append(quote+nTrackedAllocObjects[i]+quote+separator);
            result.append(quote+intFormat.format((double)avgObjectAge[i])+quote+separator);
            result.append(quote+maxSurvGen[i]+quote+newLine);
            eDD.dumpData(result);
        }
        eDD.close();
    }

    private String replaceHTMLCharacters(String s) {
        StringBuffer sb = new StringBuffer();
        int len = s.length();
        for (int i = 0; i < len; i++) {
          char c = s.charAt(i);
          switch (c) {
              case '<': sb.append("&lt;"); break;
              case '>': sb.append("&gt;"); break;
              case '&': sb.append("&amp;"); break;
              case '"': sb.append("&quot;"); break;
              default: sb.append(c); break;
          }
        }
        return sb.toString();
    }
}
