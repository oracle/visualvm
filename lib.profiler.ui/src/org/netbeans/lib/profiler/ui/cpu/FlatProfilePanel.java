/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
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
import org.netbeans.lib.profiler.results.cpu.CPUResultsSnapshot;
import org.netbeans.lib.profiler.results.cpu.FlatProfileContainer;
import org.netbeans.lib.profiler.ui.UIConstants;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.FilterComponent;
import org.netbeans.lib.profiler.ui.components.JExtendedTable;
import org.netbeans.lib.profiler.ui.components.table.ClassNameTableCellRenderer;
import org.netbeans.lib.profiler.ui.components.table.CustomBarCellRenderer;
import org.netbeans.lib.profiler.ui.components.table.ExtendedTableModel;
import org.netbeans.lib.profiler.ui.components.table.LabelBracketTableCellRenderer;
import org.netbeans.lib.profiler.ui.components.table.LabelTableCellRenderer;
import org.netbeans.lib.profiler.ui.components.table.MethodNameTableCellRenderer;
import org.netbeans.lib.profiler.ui.components.table.SortableTableModel;
import org.netbeans.lib.profiler.utils.StringUtils;
import java.awt.*;
import java.awt.event.*;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;


/**
 * A common abstract superclass for Hotspots display containing a flat profile.
 * <p/>
 * The subclasses need to implement these methods:
 * obtainResults () to initialize the data either from snapshot or from live data.
 * getTitle () to provide title for the panel
 * getMethodClassNameAndSig () to map methodId to class/method names
 * supportsReverseCallGraph () to declare if displaying reverse call graph is supported
 * showReverseCallGraph () to display the reverse call graph (utilizing actionsHandler)
 *
 * @author Misha Dmitriev
 * @author Ian Formanek
 * @author Jiri Sedlacek
 */
public abstract class FlatProfilePanel extends CPUResultsPanel {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.netbeans.lib.profiler.ui.cpu.Bundle"); // NOI18N
    private static final String FILTER_ITEM_NAME = messages.getString("FlatProfilePanel_FilterItemName"); // NOI18N
    private static final String METHOD_COLUMN_NAME = messages.getString("FlatProfilePanel_MethodColumnName"); // NOI18N
    private static final String METHOD_COLUMN_TOOLTIP = messages.getString("FlatProfilePanel_MethodColumnToolTip"); // NOI18N
    private static final String METHOD_FILTER_HINT = messages.getString("FlatProfilePanel_MethodFilterHint"); // NOI18N
    private static final String CLASS_COLUMN_NAME = messages.getString("FlatProfilePanel_ClassColumnName"); // NOI18N
    private static final String CLASS_COLUMN_TOOLTIP = messages.getString("FlatProfilePanel_ClassColumnToolTip"); // NOI18N
    private static final String CLASS_FILTER_HINT = messages.getString("FlatProfilePanel_ClassFilterHint"); // NOI18N
    private static final String PACKAGE_COLUMN_NAME = messages.getString("FlatProfilePanel_PackageColumnName"); // NOI18N
    private static final String PACKAGE_COLUMN_TOOLTIP = messages.getString("FlatProfilePanel_PackageColumnToolTip"); // NOI18N
    private static final String PACKAGE_FILTER_HINT = messages.getString("FlatProfilePanel_PackageFilterHint"); // NOI18N
    private static final String SELFTIME_REL_COLUMN_NAME = messages.getString("FlatProfilePanel_SelfTimeRelColumnName"); // NOI18N
    private static final String SELFTIME_REL_COLUMN_TOOLTIP = messages.getString("FlatProfilePanel_SelfTimeRelColumnToolTip"); // NOI18N
    private static final String SELFTIME_COLUMN_NAME = messages.getString("FlatProfilePanel_SelfTimeColumnName"); // NOI18N
    private static final String SELFTIME_COLUMN_TOOLTIP = messages.getString("FlatProfilePanel_SelfTimeColumnToolTip"); // NOI18N
    private static final String SELFTIME_CPU_COLUMN_NAME = messages.getString("FlatProfilePanel_SelfTimeCpuColumnName"); // NOI18N
    private static final String SELFTIME_CPU_COLUMN_TOOLTIP = messages.getString("FlatProfilePanel_SelfTimeCpuColumnToolTip"); // NOI18N
    private static final String INVOCATIONS_COLUMN_NAME = messages.getString("FlatProfilePanel_InvocationsColumnName"); // NOI18N
    private static final String SAMPLES_COLUMN_NAME = messages.getString("FlatProfilePanel_SamplesColumnName"); // NOI18N
    private static final String INVOCATIONS_COLUMN_TOOLTIP = messages.getString("FlatProfilePanel_InvocationsColumnToolTip"); // NOI18N
    private static final String SAMPLES_COLUMN_TOOLTIP = messages.getString("FlatProfilePanel_SamplesColumnToolTip"); // NOI18N
    private static final String TABLE_ACCESS_NAME = messages.getString("FlatProfilePanel_TableAccessName"); // NOI18N
    private static final String NO_RELEVANT_DATA = messages.getString("FlatProfilePanel_NoRelevantData"); // NOI18N
// -----

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    //float maxValue;
    protected CustomBarCellRenderer barRenderer;
    protected ExtendedTableModel resTableModel;
    protected FilterComponent filterComponent;
    protected FlatProfileContainer flatProfileContainer;
    protected JExtendedTable resTable;
    protected JScrollPane jScrollPane;
    protected String filterString = ""; // NOI18N
    protected boolean collectingTwoTimeStamps;
    protected boolean sortOrder;
    protected double valueFilterValue = 0.0d;
    protected int filterType = CommonConstants.FILTER_CONTAINS;
    protected int sortBy;
    protected int threadId;
    private CPUSelectionHandler selectionHandler;
    private JPanel noDataPanel;
    private int minNamesColumnWidth; // minimal width of classnames columns
    private int sortingColumn;
    private boolean sampling;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public FlatProfilePanel(CPUResUserActionsHandler actionsHandler, boolean sampling) {
        this(actionsHandler, null, sampling);
    }

    public FlatProfilePanel(CPUResUserActionsHandler actionsHandler, CPUSelectionHandler selectionHandler, boolean sampling) {
        super(actionsHandler);
        this.selectionHandler = selectionHandler;
        this.sampling = sampling;
        setDefaultSorting();

        minNamesColumnWidth = getFontMetrics(getFont()).charWidth('W') * 30; // NOI18N

        cornerPopup = new JPopupMenu();

        jScrollPane = createScrollPaneVerticalScrollBarAlways();
        jScrollPane.setCorner(JScrollPane.UPPER_RIGHT_CORNER, createHeaderPopupCornerButton(cornerPopup));
        jScrollPane.setBorder(BorderFactory.createEmptyBorder());
        jScrollPane.setViewportBorder(BorderFactory.createEmptyBorder());
        jScrollPane.addMouseWheelListener(new MouseWheelListener() {
                public void mouseWheelMoved(MouseWheelEvent e) {
                    if (resTable != null) {
                        resTable.mouseWheelMoved(e);
                    }
                }
            });
        initFilterPanel();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void clearSelection() {
        resTable.clearSelection();
    }
    
    public void setCPUSelectionHandler(CPUSelectionHandler handler) {
        selectionHandler = handler;
    }

    @Override
    public int getCurrentThreadId() {
        return threadId;
    }

    // NOTE: this method only sets sortingColumn, sortOrder and sortBy, it doesn't refresh UI!
    public void setDefaultSorting() {
        setSorting(1, SortableTableModel.SORT_ORDER_DESC);
    }

    public int getFilterType() {
        return filterComponent.getFilterType();
    }

    public String getFilterValue() {
        return filterComponent.getFilterValue();
    }

    public void setFilterValues(String filterValue, int filterType) {
        filterComponent.setFilter(filterValue, filterType);
    }

    /*  private void printPercents() {
       double sum = 0;
    
       for (int i = 0; i < flatProfileContainer.getNRows(); i++) {
         sum += flatProfileContainer.getPercentAtRow(i);
       }
    
       System.err.println("Sum: "+sum);
       }
     */

    // --- Find functionality stuff
    public void setFindString(String findString) {
        resTable.setFindParameters(findString, 0);
    }

    public String getFindString() {
        return resTable.getFindString();
    }

    public boolean isFindStringDefined() {
        return resTable.isFindStringDefined();
    }

    public void setResultsAvailable(boolean available) {
        JViewport viewport = jScrollPane.getViewport();
        Component viewComponent = available ? resTable : noDataPanel;
        if (viewComponent != viewport.getView()) {
            viewport.setView(viewComponent);
            revalidate();
            repaint();
        }
    }

    public Object getResultsViewReference() {
        return resTable;
    }

    public int getSortBy(int column) {
        switch (column) {
            case 0:
                return FlatProfileContainer.SORT_BY_NAME;
            case 1:
            case 2:
                return FlatProfileContainer.SORT_BY_TIME;
            case 3:
                return collectingTwoTimeStamps ? FlatProfileContainer.SORT_BY_SECONDARY_TIME
                                               : FlatProfileContainer.SORT_BY_INV_NUMBER;
            case 4:
                return FlatProfileContainer.SORT_BY_INV_NUMBER;
        }

        return FlatProfileContainer.SORT_BY_TIME;
    }

    // NOTE: this method only sets sortingColumn, sortOrder and sortBy, it doesn't refresh UI!
    public void setSorting(int sColumn, boolean sOrder) {
        if (sColumn == CommonConstants.SORTING_COLUMN_DEFAULT) {
            setDefaultSorting();
        } else {
            sortingColumn = sColumn;
            sortOrder = sOrder;
            sortBy = getSortBy(sortingColumn);
        }
    }

    @Override
    public int getSortingColumn() {
        if (resTableModel == null) {
            return CommonConstants.SORTING_COLUMN_DEFAULT;
        }

        return resTableModel.getRealColumn(resTableModel.getSortingColumn());
    }

    @Override
    public boolean getSortingOrder() {
        if (resTableModel == null) {
            return false;
        }

        return resTableModel.getSortingOrder();
    }

    public void addFilterListener(ChangeListener listener) {
        filterComponent.addChangeListener(listener);
    }

    public void addResultsViewFocusListener(FocusListener listener) {
        resTable.addFocusListener(listener);
    }

    public boolean findFirst() {
        return resTable.findFirst();
    }

    public boolean findNext() {
        return resTable.findNext();
    }

    public boolean findPrevious() {
        return resTable.findPrevious();
    }

    @Override
    public void prepareResults() {
        prepareResults(true);
    }

    public void removeFilterListener(ChangeListener listener) {
        filterComponent.removeChangeListener(listener);
    }

    public void removeResultsViewFocusListener(FocusListener listener) {
        resTable.removeFocusListener(listener);
    }

    @Override
    public void requestFocus() {
        if (resTable != null) {
            SwingUtilities.invokeLater(new Runnable() { // must be invoked lazily to override default focus of first component (top-right cornerButton)
                    public void run() {
                        resTable.requestFocus();
                    }
                });
        }
    }

    // ---

    // components are discarded between profiling sessions
    @Override
    public void reset() {
        jScrollPane.setViewportView(null);
        flatProfileContainer = null;
        resTable = null;
        resTableModel = null;
    }

    public void selectMethod(int methodId) {
        //    System.err.println("Select method: "+methodId);
        if (methodId == 0) {
            resTable.getSelectionModel().clearSelection();

            return;
        }

        int sel = resTable.getSelectedRow();

        if (sel >= flatProfileContainer.getNRows()) {
            sel = flatProfileContainer.getNRows() - 1; // no idea how can this happen, but it happens - see #100355
        }

        if ((sel != -1) && (getCurrentThreadId() == threadId) && (flatProfileContainer.getMethodIdAtRow(sel) == methodId)) {
            return; // the right method is already selected
        }

        // lookup the row index with the matching methodId
        for (int i = 0; i < flatProfileContainer.getNRows(); i++) {
            //      System.err.println("Checking: "+flatProfileContainer.getMethodIdAtRow(i));
            if (flatProfileContainer.getMethodIdAtRow(i) == methodId) {
                resTable.getSelectionModel().setSelectionInterval(i, i);
                resTable.scrollRectToVisible(resTable.getCellRect(i, 0, true));

                break;
            }
        }
    }

    public void selectMethod(String methodName) {
        for (int i = 0; i < resTable.getRowCount(); i++) {
            if (resTable.getValueAt(i, 0).toString().equals(methodName)) {
                resTable.getSelectionModel().setSelectionInterval(i, i);
                resTable.scrollRectToVisible(resTable.getCellRect(i, 0, true));

                return;
            }
        }

        resTable.getSelectionModel().clearSelection();
    }

    @Override
    protected String getSelectedMethodName() {
        if ((resTable == null) || (resTableModel == null)) {
            return null;
        }

        if (resTable.getSelectedRow() == -1) {
            return null;
        }

        return resTable.getValueAt(resTable.getSelectedRow(), 0).toString();
    }

    @Override
    protected void initColumnSelectorItems() {
        cornerPopup.removeAll();

        JCheckBoxMenuItem menuItem;

        for (int i = 0; i < columnCount; i++) {
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

            cornerPopup.add(menuItem);
        }

        cornerPopup.addSeparator();

        JCheckBoxMenuItem filterMenuItem = new JCheckBoxMenuItem(FILTER_ITEM_NAME);
        filterMenuItem.setActionCommand("Filter"); // NOI18N
        addMenuItemListener(filterMenuItem);

        if (filterComponent == null) {
            filterMenuItem.setState(true);
        } else {
            filterMenuItem.setState(filterComponent.getComponent().isVisible());
        }

        cornerPopup.add(filterMenuItem);

        cornerPopup.pack();
    }

    protected void initColumnsData() {
        columnCount = collectingTwoTimeStamps ? 5 : 4;

        columnsVisibility = new boolean[columnCount];
        for (int i = 0; i < columnCount - 1; i++)
            columnsVisibility[i] = true;
        if (!sampling) columnsVisibility[columnCount - 1] = true;

        columnWidths = new int[columnCount - 1]; // Width of the first column fits to width
        columnNames = new String[columnCount];
        columnToolTips = new String[columnCount];
        columnRenderers = new TableCellRenderer[columnCount];

        columnNames[0] = METHOD_COLUMN_NAME;
        columnToolTips[0] = METHOD_COLUMN_TOOLTIP;

        columnNames[1] = SELFTIME_REL_COLUMN_NAME;
        columnToolTips[1] = SELFTIME_REL_COLUMN_TOOLTIP;

        columnNames[2] = SELFTIME_COLUMN_NAME;
        columnToolTips[2] = SELFTIME_COLUMN_TOOLTIP;

        if (collectingTwoTimeStamps) {
            columnNames[3] = SELFTIME_CPU_COLUMN_NAME;
            columnToolTips[3] = SELFTIME_CPU_COLUMN_TOOLTIP;
            if (sampling) {
                columnNames[4] = SAMPLES_COLUMN_NAME;
                columnToolTips[4] = SAMPLES_COLUMN_TOOLTIP;
            } else {
                columnNames[4] = INVOCATIONS_COLUMN_NAME;
                columnToolTips[4] = INVOCATIONS_COLUMN_TOOLTIP;
            }
        } else { // just absolute
            if (sampling) {
                columnNames[3] = SAMPLES_COLUMN_NAME;
                columnToolTips[3] = SAMPLES_COLUMN_TOOLTIP;
            } else {
                columnNames[3] = INVOCATIONS_COLUMN_NAME;
                columnToolTips[3] = INVOCATIONS_COLUMN_TOOLTIP;
            }
        }

        int maxWidth = getFontMetrics(getFont()).charWidth('W') * 12; // NOI18N // initial width of data columns

        CustomBarCellRenderer customBarCellRenderer = new CustomBarCellRenderer(0, 100);
        LabelTableCellRenderer labelTableCellRenderer = new LabelTableCellRenderer(JLabel.TRAILING);
        LabelBracketTableCellRenderer labelBracketTableCellRenderer = new LabelBracketTableCellRenderer(JLabel.TRAILING);

        // method / class / package name
        columnRenderers[0] = null;

        columnWidths[1 - 1] = maxWidth;
        columnRenderers[1] = customBarCellRenderer;

        columnWidths[2 - 1] = maxWidth;
        columnRenderers[2] = labelBracketTableCellRenderer;

        for (int i = 3; i < columnCount; i++) {
            columnWidths[i - 1] = maxWidth;
            columnRenderers[i] = labelTableCellRenderer;
        }
    }

    /*  public void updateValueFilter(double value) {
       System.err.println("Update value filter to: "+value);
    
       valueFilterValue = value/3f; // maximum 33.3%
    
       flatProfileContainer.filterOriginalData(
       FilterComponent.getFilterStrings(filterString), filterType, valueFilterValue);
       flatProfileContainer.sortBy(sortBy, sortOrder);
       resTable.invalidate();
       jScrollPane.revalidate();
       resTable.repaint();
       } */
    protected abstract void obtainResults();

    /**
     * If firstTime is true, it means we need to go and get results from the CCT, which means walking the
     * nodes of the CCT and doing some calculations, i.e. non-zero cost. Otherwise, we just use the cached
     * results in flatProfileContainer, and sort them by the current sorting criterion.
     */
    protected void prepareResults(boolean firstTime) {
        if (threadId < -1) {
            return; // -1 is reserved for all threads merged flat profile;
        }

        // non-negative numbers are actual thread ids
        int currentColumnCount = collectingTwoTimeStamps ? 5 : 4;

        if (columnCount != currentColumnCount) {
            initColumnsData();
        } else {
            if (resTable != null) {
                saveColumnsData();
            }
        }

        // first create the UI component model
        if ((resTableModel == null) || (resTable == null)) {
            initComponents(); // new components (table & tableModel) are created for each profiling session
        }

        // then try to fetch some data
        if (firstTime) {
            obtainResults(); // This also sorts the results by the appropriate timer

            String firstColumnName = columnNames[0];
            initFirstColumnName();

            if ((resTable != null) && !columnNames[0].equals(firstColumnName)) {
                resTable.getColumnModel().getColumn(0).setHeaderValue(columnNames[0]);
            }
        }

        flatProfileContainer.sortBy(sortBy, sortOrder);

        //    resTable.clearSelection();
        resTable.invalidate();
        jScrollPane.revalidate();
        resTable.repaint();
    }

    protected void saveColumnsData() {
        int index;
        TableColumnModel colModel = resTable.getColumnModel();

        for (int i = 0; i < resTableModel.getColumnCount(); i++) {
            index = resTableModel.getRealColumn(i);

            if (index != 0) {
                columnWidths[index - 1] = colModel.getColumn(i).getPreferredWidth();
            }
        }

        columnsVisibility = null;
        columnsVisibility = resTableModel.getColumnsVisibility();
    }

    protected void updateResults() {
        if (threadId < -1) {
            return; // -1 is reserved for all threads merged flat profile;
        }

        // non-negative numbers are actual thread ids
        int currentColumnCount = collectingTwoTimeStamps ? 5 : 4;

        if (columnCount != currentColumnCount) {
            initColumnsData();
        } else {
            if (resTable != null) {
                saveColumnsData();
            }
        }

        flatProfileContainer.sortBy(sortBy, sortOrder);

        jScrollPane.setViewportView(resTable);
        jScrollPane.getViewport().setBackground(resTable.getBackground());
    }

    private void setColumnsData() {
        switch (currentView) {
            case CPUResultsSnapshot.METHOD_LEVEL_VIEW:
                columnRenderers[0] = new MethodNameTableCellRenderer();

                break;
            case CPUResultsSnapshot.CLASS_LEVEL_VIEW:
                columnRenderers[0] = new ClassNameTableCellRenderer();

                break;
            case CPUResultsSnapshot.PACKAGE_LEVEL_VIEW:
                columnRenderers[0] = new LabelTableCellRenderer();

                break;
        }

        int index;
        TableColumnModel colModel = resTable.getColumnModel();

        for (int i = 0; i < resTableModel.getColumnCount(); i++) {
            index = resTableModel.getRealColumn(i);

            if (index == 0) {
                colModel.getColumn(i).setPreferredWidth(minNamesColumnWidth);
            } else {
                colModel.getColumn(i).setPreferredWidth(columnWidths[index - 1]);
            }

            colModel.getColumn(i).setCellRenderer(columnRenderers[index]);
        }
    }

    private void addMenuItemListener(JCheckBoxMenuItem menuItem) {
        menuItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    if (e.getActionCommand().equals("Filter")) { // NOI18N
                        filterComponent.getComponent().setVisible(!filterComponent.getComponent().isVisible());

                        return;
                    }

                    boolean sortResults = false;
                    int column = Integer.parseInt(e.getActionCommand());
                    int currentSortingColumn = resTableModel.getSortingColumn();
                    int realSortingColumn = resTableModel.getRealColumn(currentSortingColumn);
                    boolean isColumnVisible = resTableModel.isRealColumnVisible(column);

                    // Current sorting column is going to be hidden
                    if ((isColumnVisible) && (column == realSortingColumn)) {
                        // Try to set next column as a currentSortingColumn. If currentSortingColumn is the last column,
                        // set previous column as a sorting Column (one column is always visible).
                        currentSortingColumn = ((currentSortingColumn + 1) == resTableModel.getColumnCount())
                                               ? (currentSortingColumn - 1) : (currentSortingColumn + 1);
                        realSortingColumn = resTableModel.getRealColumn(currentSortingColumn);
                        sortResults = true;
                    }

                    resTableModel.setRealColumnVisibility(column, !isColumnVisible);
                    resTable.createDefaultColumnsFromModel();
                    resTableModel.setTable(resTable);
                    currentSortingColumn = resTableModel.getVirtualColumn(realSortingColumn);

                    if (sortResults) {
                        sortOrder = resTableModel.getInitialSorting(currentSortingColumn);
                        sortBy = getSortBy(realSortingColumn);
                        flatProfileContainer.sortBy(sortBy, sortOrder);
                        resTable.repaint();
                    }

                    sortingColumn = realSortingColumn;
                    resTableModel.setInitialSorting(currentSortingColumn, sortOrder);
                    resTable.getTableHeader().repaint();
                    setColumnsData();

                    // TODO [ui-persistence]
                }
            });
    }

    private void initComponents() {
        resTableModel = new ExtendedTableModel(new SortableTableModel() {
                @Override
                public String getColumnName(int col) {
                    return columnNames[col];
                }

                public int getRowCount() {
                    if (flatProfileContainer == null) {
                        return 0;
                    }

                    return flatProfileContainer.getNRows();
                }

                public int getColumnCount() {
                    return columnCount;
                }

                @Override
                public Class getColumnClass(int col) {
                    if (col == 1) {
                        return Number.class;
                    }

                    return String.class;
                }

                public Object getValueAt(int row, int col) {
                    return computeValueAt(row, col);
                }

                @Override
                public String getColumnToolTipText(int col) {
                    return columnToolTips[col];
                }

                @Override
                public void sortByColumn(int column, boolean order) {
                    sortingColumn = column;
                    sortBy = getSortBy(column);
                    sortOrder = order;

                    int selectedRow = resTable.getSelectedRow();
                    String selectedRowContents = null;

                    if (selectedRow != -1) {
                        selectedRowContents = (String) resTable.getValueAt(selectedRow, 0);
                    }

                    updateResults();

                    if (selectedRowContents != null) {
                        resTable.selectRowByContents(selectedRowContents, 0, true);
                    }
                }

                /**
                 * @param column The table column index
                 * @return Initial sorting for the specified column - if true, ascending, if false descending
                 */
                @Override
                public boolean getInitialSorting(int column) {
                    switch (column) {
                        case 0:
                            return true;
                        default:
                            return false;
                    }
                }
            });

        if (columnsVisibility != null) {
            resTableModel.setColumnsVisibility(columnsVisibility);
        }

        resTable = new JExtendedTable(resTableModel) {
                @Override
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

                {
                }
            };
        resTable.getAccessibleContext().setAccessibleName(TABLE_ACCESS_NAME);

        resTableModel.setTable(resTable);
        resTableModel.setInitialSorting(sortingColumn, sortOrder);
        resTable.setRowSelectionAllowed(true);
        resTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resTable.setGridColor(UIConstants.TABLE_VERTICAL_GRID_COLOR);
        resTable.setSelectionBackground(UIConstants.TABLE_SELECTION_BACKGROUND_COLOR);
        resTable.setSelectionForeground(UIConstants.TABLE_SELECTION_FOREGROUND_COLOR);
        resTable.setShowHorizontalLines(UIConstants.SHOW_TABLE_HORIZONTAL_GRID);
        resTable.setShowVerticalLines(UIConstants.SHOW_TABLE_VERTICAL_GRID);
        resTable.setRowMargin(UIConstants.TABLE_ROW_MARGIN);
        resTable.setRowHeight(UIUtils.getDefaultRowHeight() + 2);
        resTable.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "DEFAULT_ACTION"); // NOI18N
        resTable.getActionMap().put("DEFAULT_ACTION",
                                    new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    performDefaultAction();
                }
            }); // NOI18N

        // Disable traversing table cells using TAB and Shift+TAB
        Set keys = new HashSet(resTable.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS));
        keys.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0));
        resTable.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, keys);

        keys = new HashSet(resTable.getFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS));
        keys.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_MASK));
        resTable.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, keys);

        setColumnsData();

        // -------------------------------------
        resTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                private int lastSelection = -1;

                public void valueChanged(ListSelectionEvent e) {
                    int selectedRow = resTable.getSelectedRow();
                    methodId = (selectedRow != -1) ? flatProfileContainer.getMethodIdAtRow(selectedRow) : (-1);

                    if (selectionHandler != null) {
                        selectionHandler.methodSelected(lastSelection, methodId, currentView);
                    }

                    lastSelection = methodId;
                }
            });

        resTable.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if ((e.getKeyCode() == KeyEvent.VK_CONTEXT_MENU)
                            || ((e.getKeyCode() == KeyEvent.VK_F10) && (e.getModifiers() == InputEvent.SHIFT_MASK))) {
                        int selectedRow = resTable.getSelectedRow();

                        if (selectedRow != -1) {
                            if (popupShowSource != null) popupShowSource.setVisible(true);
                            if (popupShowReverse != null) popupShowReverse.setVisible(true);

                            popupPath = null;
                            methodId = flatProfileContainer.getMethodIdAtRow(selectedRow);
                            popupAddToRoots.setVisible(true);

                            Rectangle cellRect = resTable.getCellRect(selectedRow, 0, false);

                            callGraphPopupMenu.show(e.getComponent(), ((cellRect.x + resTable.getSize().width) > 50) ? 50 : 5,
                                                    cellRect.y);
                        }
                    }
                }
            });

        resTable.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.getModifiers() == InputEvent.BUTTON3_MASK) {
                        int line = resTable.rowAtPoint(e.getPoint());

                        if (line != -1) {
                            resTable.setRowSelectionInterval(line, line);
                        }
                    }
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    int line = resTable.rowAtPoint(e.getPoint());

                    if (line == -1) {
                        if (popupShowSource != null) popupShowSource.setVisible(false);
                        if (popupShowReverse != null) popupShowReverse.setVisible(false);

                        popupAddToRoots.setVisible(false);

                        if (e.getModifiers() == InputEvent.BUTTON3_MASK) {
                            popupPath = null;
                            callGraphPopupMenu.show(e.getComponent(), e.getX(), e.getY());
                        }
                    } else {
                        resTable.getSelectionModel().setSelectionInterval(line, line);
                        
                        if (popupShowSource != null) popupShowSource.setVisible(true);
                        if (popupShowReverse != null) popupShowReverse.setVisible(true);

                        popupAddToRoots.setVisible(true);
                        methodId = flatProfileContainer.getMethodIdAtRow(line);

                        if (e.getModifiers() == InputEvent.BUTTON3_MASK) {
                            popupPath = null;
                            callGraphPopupMenu.show(e.getComponent(), e.getX(), e.getY());
                        } else if ((e.getModifiers() == InputEvent.BUTTON1_MASK) && (e.getClickCount() == 2)) {
                            showSourceForMethod(methodId);
                        }
                    }
                }
            });
        noDataPanel = new JPanel(new BorderLayout());
        noDataPanel.add(new JLabel(NO_RELEVANT_DATA), BorderLayout.NORTH);
        noDataPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        noDataPanel.setBackground(resTable.getBackground());
        jScrollPane.setViewportView(null);
        //    jScrollPane.setViewportView(resTable);
        jScrollPane.getViewport().setBackground(resTable.getBackground());
    }
    
    protected Object computeValueAt(int row, int col) {
        switch (col) {
            case 0:
                return flatProfileContainer.getMethodNameAtRow(row);
            case 1:
                return new Float(flatProfileContainer.getPercentAtRow(row));
            case 2:
                return StringUtils.mcsTimeToString(flatProfileContainer.getTimeInMcs0AtRow(row)) + " ms (" // NOI18N
                        + percentFormat.format(flatProfileContainer.getPercentAtRow(row) / 100) + ")"; // NOI18N
            case 3:
                return collectingTwoTimeStamps
                        ? (StringUtils.mcsTimeToString(flatProfileContainer.getTimeInMcs1AtRow(row)) + " ms" // NOI18N
                ) : intFormat.format(flatProfileContainer.getNInvocationsAtRow(row));
            case 4:
                return intFormat.format(flatProfileContainer.getNInvocationsAtRow(row));
            default:
                return null;
        }
    }

    private void initFilterPanel() {
        filterComponent = FilterComponent.create(true, true);
        filterComponent.setFilter(filterString, filterType);

        filterComponent.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    String selectedRowContents = null;

                    if (resTable != null) {
                        int selectedRow = resTable.getSelectedRow();

                        if (selectedRow != -1) {
                            selectedRowContents = (String) resTable.getValueAt(selectedRow, 0);
                        }
                    }

                    filterString = filterComponent.getFilterValue();
                    filterType = filterComponent.getFilterType();

                    if (flatProfileContainer != null) { // can be null after reset, see Issue 65866
                        flatProfileContainer.filterOriginalData(FilterComponent.getFilterValues(filterString), filterType,
                                                                valueFilterValue);
                        flatProfileContainer.sortBy(sortBy, sortOrder);
                    }

                    if (resTable != null) { // can be null after reset, see Issue 65866
                        resTable.invalidate();
                        jScrollPane.revalidate();
                        resTable.repaint();

                        if (selectedRowContents != null) {
                            resTable.selectRowByContents(selectedRowContents, 0, true);
                        }
                    }
                }
            });

        add(filterComponent.getComponent(), BorderLayout.SOUTH);
    }

    private void initFirstColumnName() {
        switch (currentView) {
            case CPUResultsSnapshot.METHOD_LEVEL_VIEW:
                columnNames[0] = METHOD_COLUMN_NAME;
                columnToolTips[0] = METHOD_COLUMN_TOOLTIP;
                filterComponent.setHint(METHOD_FILTER_HINT);

                break;
            case CPUResultsSnapshot.CLASS_LEVEL_VIEW:
                columnNames[0] = CLASS_COLUMN_NAME;
                columnToolTips[0] = CLASS_COLUMN_TOOLTIP;
                filterComponent.setHint(CLASS_FILTER_HINT);

                break;
            case CPUResultsSnapshot.PACKAGE_LEVEL_VIEW:
                columnNames[0] = PACKAGE_COLUMN_NAME;
                columnToolTips[0] = PACKAGE_COLUMN_TOOLTIP;
                filterComponent.setHint(PACKAGE_FILTER_HINT);

                break;
        }

        if (resTable != null) {
            resTable.getTableHeader().repaint();
        }
    }
}
