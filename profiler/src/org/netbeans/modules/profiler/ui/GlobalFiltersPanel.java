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

package org.netbeans.modules.profiler.ui;

import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.common.filters.FilterUtils;
import org.netbeans.lib.profiler.common.filters.GlobalFilters;
import org.netbeans.lib.profiler.ui.UIConstants;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import org.netbeans.lib.profiler.ui.components.JExtendedTable;
import org.netbeans.lib.profiler.ui.components.table.BooleanTableCellRenderer;
import org.netbeans.lib.profiler.ui.components.table.LabelTableCellRenderer;
import org.netbeans.modules.profiler.NetBeansProfiler;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;


/**
 *
 * @author Tomas Hurka
 * @author  Jiri Sedlacek
 */
public final class GlobalFiltersPanel extends JPanel implements HelpCtx.Provider {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private final class ButtonsListener implements ActionListener {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void actionPerformed(final ActionEvent e) {
            if (e.getSource() == newButton) {
                addNewFilter();
            } else if (e.getSource() == editButton) {
                editSelectedCell();
            } else if (e.getSource() == deleteButton) {
                deleteSelectedFilters();
            } else if (e.getSource() == moveUpButton) {
                moveSelectedRowUp();
            } else if (e.getSource() == moveDownButton) {
                moveSelectedRowDown();
            }
        }
    }

    private final class CellEditorDocumentListener implements DocumentListener {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void changedUpdate(final DocumentEvent e) {
            if (filterTable.getEditingColumn() == 0) {
                updateFilterName();
            } else if (filterTable.getEditingColumn() == 1) {
                checkFilterValue();
            }
        }

        public void insertUpdate(final DocumentEvent e) {
            if (filterTable.getEditingColumn() == 0) {
                updateFilterName();
            } else if (filterTable.getEditingColumn() == 1) {
                checkFilterValue();
            }
        }

        public void removeUpdate(final DocumentEvent e) {
            if (filterTable.getEditingColumn() == 0) {
                updateFilterName();
            } else if (filterTable.getEditingColumn() == 1) {
                checkFilterValue();
            }
        }

        private void checkFilterValue() {
            if (!isValidFilter(filterTableStringCellEditorComponent.getText())) {
                filterTableStringCellEditorComponent.setForeground(Color.red);
                filterTableStringCellEditorComponent.setSelectedTextColor(Color.red);

                if (filterCellEditorChangesOKButton) {
                    OKButton.setEnabled(false);
                }
            } else {
                filterTableStringCellEditorComponent.setForeground(UIManager.getColor("Label.foreground")); // NOI18N
                filterTableStringCellEditorComponent.setSelectedTextColor(UIManager.getColor("Label.foreground")); // NOI18N

                if (filterCellEditorChangesOKButton) {
                    OKButton.setEnabled(true);
                }
            }
        }

        private void updateFilterName() {
            filterNames[filterTable.getEditingRow()] = filterTableStringCellEditorComponent.getText();
        }
    }

    private final class CellEditorFocusListener extends FocusAdapter {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private int lastEditedColumn;
        private int lastEditedRow;

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void focusGained(final FocusEvent e) {
            lastEditedRow = filterTable.getEditingRow();
            lastEditedColumn = filterTable.getEditingColumn();
            editButton.setEnabled(false);
            cellValueBeforeEditing = (String) filterTable.getValueAt(lastEditedRow, lastEditedColumn);
            filterCellEditorChangesOKButton = areFiltersValidExceptRow(lastEditedRow);

            if (lastEditedColumn == 1) { // only values in column 1 need to be checked

                if (isValidFilter(filterTableStringCellEditorComponent.getText())) {
                    filterTableStringCellEditorComponent.setForeground(UIManager.getColor("Label.foreground")); // NOI18N
                    filterTableStringCellEditorComponent.setSelectedTextColor(UIManager.getColor("Label.foreground")); // NOI18N
                } else {
                    filterTableStringCellEditorComponent.setForeground(Color.red);
                    filterTableStringCellEditorComponent.setSelectedTextColor(Color.red);
                }
            }
        }

        public void focusLost(final FocusEvent e) {
            processUniqueFilterNameCheckAt(lastEditedRow, lastEditedColumn);
            editButton.setEnabled(true);
        }
    }

    private final class CellEditorKeyListener extends KeyAdapter {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void keyPressed(final KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_ENTER:

                    if (filterTable.getEditingColumn() != 0) {
                        break;
                    }

                    e.consume();
                    processUniqueFilterNameCheckAt(filterTable.getEditingRow(), filterTable.getEditingColumn());
                    stopFilterTableEditing();
                    editSelectedRow(1);
                    filterTableStringCellEditorComponent.selectAll();

                    break;
                case KeyEvent.VK_ESCAPE:
                    e.consume();
                    filterTableStringCellEditorComponent.setText(cellValueBeforeEditing);
                    stopFilterTableEditing();

                    break;
            }
        }
    }

    private final class FilterTableKeyListener extends KeyAdapter {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void keyPressed(final KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_DELETE:
                    e.consume();
                    deleteSelectedFilters();

                    break;
                case KeyEvent.VK_N:

                    if (e.getModifiers() == InputEvent.CTRL_MASK) {
                        e.consume();
                        addNewFilter();

                        break;
                    }
                case KeyEvent.VK_UP:

                    if (e.getModifiers() == InputEvent.CTRL_MASK) {
                        e.consume();

                        if ((filterTable.getSelectedRowCount() == 1) && (filterTable.getSelectedRow() > 0)) {
                            moveSelectedRowUp();
                        }

                        break;
                    }
                case KeyEvent.VK_DOWN:

                    if (e.getModifiers() == InputEvent.CTRL_MASK) {
                        e.consume();

                        if ((filterTable.getSelectedRowCount() == 1)
                                && (filterTable.getSelectedRow() < (filterTable.getRowCount() - 1))) {
                            moveSelectedRowDown();
                        }

                        break;
                    }
                case KeyEvent.VK_ESCAPE:
                    GlobalFiltersPanel.this.processKeyEvent(e);
            }
        }
    }

    //--- Private classes -----
    private final class FilterTableModel extends AbstractTableModel {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public boolean isCellEditable(final int rowIndex, final int columnIndex) {
            return true;
        }

        public Class getColumnClass(final int column) {
            return columnClasses[column];
        }

        public int getColumnCount() {
            return columnNames.length;
        }

        public String getColumnName(final int column) {
            return columnNames[column];
        }

        public int getRowCount() {
            return filterNames.length;
        }

        public void setValueAt(final Object aValue, final int rowIndex, final int columnIndex) {
            switch (columnIndex) {
                case 0:
                    filterNames[rowIndex] = ((String) aValue).trim();

                    break;
                case 1:
                    filterValues[rowIndex] = (String) aValue;

                    break;
            }
        }

        public Object getValueAt(final int rowIndex, final int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return filterNames[rowIndex];
                case 1:
                    return filterValues[rowIndex];
            }

            return null;
        }
    }

    private final class FilterTableViewport extends JViewport implements TableColumnModelListener {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private final JTableHeader tableHeader;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public FilterTableViewport(final JTable table) {
            super();

            setView(table);
            setBackground(table.getBackground());

            tableHeader = table.getTableHeader();
            tableHeader.getColumnModel().addColumnModelListener(this);
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void columnAdded(final TableColumnModelEvent e) {
            repaint();
        }

        public void columnMarginChanged(final ChangeEvent e) {
            repaint();
        }

        public void columnMoved(final TableColumnModelEvent e) {
            repaint();
        }

        public void columnRemoved(final TableColumnModelEvent e) {
            repaint();
        }

        public void columnSelectionChanged(final ListSelectionEvent e) {
        }

        public void paint(final Graphics g) {
            super.paint(g);

            if (UIConstants.SHOW_TABLE_VERTICAL_GRID) {
                paintVerticalLines(g);
            }
        }

        private int getEmptySpaceY() {
            if (getView() == null) {
                return 0;
            }

            return getView().getHeight();
        }

        private void paintVerticalLines(final Graphics g) {
            final int emptySpaceY = getEmptySpaceY();
            Rectangle cellRect;
            g.setColor(UIConstants.TABLE_VERTICAL_GRID_COLOR);

            for (int i = 0; i < tableHeader.getColumnModel().getColumnCount(); i++) {
                cellRect = tableHeader.getHeaderRect(i);
                g.drawLine((cellRect.x + cellRect.width) - 1, emptySpaceY, (cellRect.x + cellRect.width) - 1, getHeight() - 1);
            }
        }
    }

    private final class SelectionListener implements ListSelectionListener {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void valueChanged(final ListSelectionEvent e) {
            final int selectedRowCount = filterTable.getSelectedRowCount();

            if (selectedRowCount == 0) {
                editButton.setEnabled(false);
                deleteButton.setEnabled(false);
                moveUpButton.setEnabled(false);
                moveDownButton.setEnabled(false);
            } else {
                editButton.setEnabled(true);
                deleteButton.setEnabled(true);

                if ((selectedRowCount == 1) && (filterTable.getSelectedRow() > 0)) {
                    moveUpButton.setEnabled(true);
                } else {
                    moveUpButton.setEnabled(false);
                }

                if ((selectedRowCount == 1) && (filterTable.getSelectedRow() < (filterTable.getRowCount() - 1))) {
                    moveDownButton.setEnabled(true);
                } else {
                    moveDownButton.setEnabled(false);
                }
            }
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String DEFAULT_FILTER_NAME = NbBundle.getMessage(GlobalFiltersPanel.class,
                                                                          "GlobalFiltersPanel_DefaultFilterName"); //NOI18N
    private static final String COLUMN_NAME_NAME = NbBundle.getMessage(GlobalFiltersPanel.class,
                                                                       "GlobalFiltersPanel_ColumnNameName"); //NOI18N
    private static final String COLUMN_NAME_VALUE = NbBundle.getMessage(GlobalFiltersPanel.class,
                                                                        "GlobalFiltersPanel_ColumnNameValue"); //NOI18N
    private static final String DEFINED_FILTERS_LABEL_TEXT = NbBundle.getMessage(GlobalFiltersPanel.class,
                                                                                 "GlobalFiltersPanel_DefinedFiltersLabelText"); //NOI18N
    private static final String NEW_BUTTON_TEXT = NbBundle.getMessage(GlobalFiltersPanel.class, "GlobalFiltersPanel_NewButtonText"); //NOI18N
    private static final String EDIT_BUTTON_TEXT = NbBundle.getMessage(GlobalFiltersPanel.class,
                                                                       "GlobalFiltersPanel_EditButtonText"); //NOI18N
    private static final String DELETE_BUTTON_TEXT = NbBundle.getMessage(GlobalFiltersPanel.class,
                                                                         "GlobalFiltersPanel_DeleteButtonText"); //NOI18N
    private static final String MOVE_UP_BUTTON_TEXT = NbBundle.getMessage(GlobalFiltersPanel.class,
                                                                          "GlobalFiltersPanel_MoveUpButtonText"); //NOI18N
    private static final String MOVE_DOWN_BUTTON_TEXT = NbBundle.getMessage(GlobalFiltersPanel.class,
                                                                            "GlobalFiltersPanel_MoveDownButtonText"); //NOI18N
    private static final String OK_BUTTON_TEXT = NbBundle.getMessage(GlobalFiltersPanel.class, "GlobalFiltersPanel_OkButtonText"); //NOI18N
    private static final String CANCEL_BUTTON_TEXT = NbBundle.getMessage(GlobalFiltersPanel.class,
                                                                         "GlobalFiltersPanel_CancelButtonText"); //NOI18N
    private static final String HINT_MSG = NbBundle.getMessage(GlobalFiltersPanel.class, "GlobalFiltersPanel_HintMsg"); //NOI18N
    private static final String FILTER_TABLE_ACCESS_NAME = NbBundle.getMessage(GlobalFiltersPanel.class,
                                                                               "GlobalFiltersPanel_FilterTableAccessName"); //NOI18N
    private static final String NEW_BUTTON_ACCESS_DESC = NbBundle.getMessage(GlobalFiltersPanel.class,
                                                                             "GlobalFiltersPanel_NewButtonAccessDesc"); //NOI18N
    private static final String EDIT_BUTTON_ACCESS_DESC = NbBundle.getMessage(GlobalFiltersPanel.class,
                                                                              "GlobalFiltersPanel_EditButtonAccessDesc"); //NOI18N
    private static final String DELETE_BUTTON_ACCESS_DESC = NbBundle.getMessage(GlobalFiltersPanel.class,
                                                                                "GlobalFiltersPanel_DeleteButtonAccessDesc"); //NOI18N
    private static final String MOVE_UP_BUTTON_ACCESS_DESC = NbBundle.getMessage(GlobalFiltersPanel.class,
                                                                                 "GlobalFiltersPanel_MoveUpButtonAccessDesc"); //NOI18N
    private static final String MOVE_DOWN_BUTTON_ACCESS_DESC = NbBundle.getMessage(GlobalFiltersPanel.class,
                                                                                   "GlobalFiltersPanel_MoveDownButtonAccessDesc"); //NOI18N
                                                                                                                                   // -----
    private static final String HELP_CTX_KEY = "GlobalFiltersPanel.HelpCtx"; // NOI18N
    private static final HelpCtx HELP_CTX = new HelpCtx(HELP_CTX_KEY);
    private static GlobalFiltersPanel defaultInstance;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    JTextField filterTableStringCellEditorComponent;
    private ActionListener buttonsListener;
    private HTMLTextArea hintArea;
    private JButton CancelButton;
    private JButton OKButton;
    private JButton deleteButton;
    private JButton editButton;
    private JButton moveDownButton;
    private JButton moveUpButton;
    private JButton newButton;
    private JExtendedTable filterTable;
    private JLabel definedFiltersLabel;
    private final NetBeansProfiler nbProfiler;
    private SelectionListener selectionListener;
    private final Class[] columnClasses;
    private final String[] columnNames;
    private String cellValueBeforeEditing;
    private String[] filterNames;
    private String[] filterValues;
    private boolean filterCellEditorChangesOKButton = false;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public GlobalFiltersPanel(final String[] filterNames, final String[] filterValues) {
        super(new BorderLayout());

        nbProfiler = (NetBeansProfiler) Profiler.getDefault();

        columnNames = new String[] { COLUMN_NAME_NAME, COLUMN_NAME_VALUE };
        columnClasses = new Class[] { String.class, String.class };

        setFilterNamesFrom(filterNames);
        setFilterValuesFrom(filterValues);

        initComponents();
    }

    /** Creates a new instance of FilterListPanel */
    private GlobalFiltersPanel() {
        super(new BorderLayout());

        nbProfiler = (NetBeansProfiler) Profiler.getDefault();

        columnNames = new String[] { COLUMN_NAME_NAME, COLUMN_NAME_VALUE };
        columnClasses = new Class[] { String.class, String.class };

        setFilterNamesFrom(new String[0]);
        setFilterValuesFrom(new String[0]);

        initComponents();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static GlobalFiltersPanel getDefault() {
        if (defaultInstance == null) {
            defaultInstance = new GlobalFiltersPanel();
        }

        return defaultInstance;
    }

    public JButton getCancelButton() {
        return CancelButton;
    }

    public String[] getFilterNamesCopy() {
        final String[] names = new String[filterNames.length];
        System.arraycopy(filterNames, 0, names, 0, filterNames.length);

        return names;
    }

    public void setFilterNamesFrom(final String[] names) {
        filterNames = new String[names.length];
        System.arraycopy(names, 0, filterNames, 0, names.length);

        if (filterTable != null) {
            ((AbstractTableModel) filterTable.getModel()).fireTableDataChanged();
        }
    }

    public String[] getFilterValuesCopy() {
        final String[] values = new String[filterValues.length];
        System.arraycopy(filterValues, 0, values, 0, filterValues.length);

        return filterValues;
    }

    public void setFilterValuesFrom(final String[] values) {
        filterValues = new String[values.length];
        System.arraycopy(values, 0, filterValues, 0, values.length);

        if (filterTable != null) {
            ((AbstractTableModel) filterTable.getModel()).fireTableDataChanged();
        }

        updateOKButton();
    }

    public HelpCtx getHelpCtx() {
        return HELP_CTX;
    }

    //--- Public methods -----
    public JButton getOKButton() {
        return OKButton;
    }

    public void applyChanges() {
        stopFilterTableEditing();

        final GlobalFilters globalFilters = nbProfiler.getGlobalFilters();
        globalFilters.setFilterNames(getFilterNamesCopy());
        globalFilters.setFilterValues(getFilterValuesCopy());
        nbProfiler.saveGlobalFilters();
    }

    public void editFilterValueAtRow(int row) {
        processUniqueFilterNameCheckAt(filterTable.getEditingRow(), filterTable.getEditingColumn());
        stopFilterTableEditing();
        filterTable.clearSelection();
        filterTable.addRowSelectionInterval(row, row);
        editSelectedRow(1);
        filterTableStringCellEditorComponent.selectAll();
    }

    public void init() {
        stopFilterTableEditing();
        filterTable.clearSelection();

        final GlobalFilters globalFilters = nbProfiler.getGlobalFilters();
        setFilterNamesFrom(globalFilters.getFilterNames());
        setFilterValuesFrom(globalFilters.getFilterValues());
    }

    private boolean isValidFilter(final String complexFilter) {
        if (complexFilter == null) {
            return false;
        }

        String[] filterParts = FilterUtils.getSeparateFilters(complexFilter);

        for (int i = 0; i < filterParts.length; i++) {
            if (!FilterUtils.isValidProfilerFilter(filterParts[i])) {
                return false;
            }
        }

        return true;
    }

    private void addNewFilter() {
        final int nCurrentFilters = filterNames.length;

        final String[] newFilterNames = new String[nCurrentFilters + 1];
        final String[] newFilterValues = new String[nCurrentFilters + 1];
        final Boolean[] newFilterEnablers = new Boolean[nCurrentFilters + 1];

        System.arraycopy(filterNames, 0, newFilterNames, 0, nCurrentFilters);
        System.arraycopy(filterValues, 0, newFilterValues, 0, nCurrentFilters);

        newFilterNames[nCurrentFilters] = createUniqueFilterName();
        newFilterValues[nCurrentFilters] = ""; //NOI18N
        newFilterEnablers[nCurrentFilters] = Boolean.TRUE;

        filterNames = newFilterNames;
        filterValues = newFilterValues;

        filterTable.invalidate();
        revalidate();
        repaint();

        filterTable.clearSelection();
        filterTable.addRowSelectionInterval(nCurrentFilters, nCurrentFilters);
        makeSelectedFilterVisible();
        filterTable.editCellAt(nCurrentFilters, 0);
        filterTableStringCellEditorComponent.selectAll();
    }

    private boolean areFiltersValidExceptRow(final int row) {
        for (int i = 0; i < filterValues.length; i++) {
            if ((i != row) && !isValidFilter(filterValues[i])) {
                return false;
            }
        }

        return true;
    }

    private boolean containsFilterName(final String filterName) {
        //filterName = filterName.trim();
        for (int i = 0; i < filterNames.length; i++) {
            if (filterNames[i].equals(filterName)) {
                return true;
            }
        }

        return false;
    }

    private boolean containsFilterNameExceptRow(final String filterName, final int row) {
        //filterName = filterName.trim();
        for (int i = 0; i < filterNames.length; i++) {
            if ((i != row) && (filterNames[i].equals(filterName))) {
                return true;
            }
        }

        return false;
    }

    private String createUniqueFilterName() {
        return createUniqueFilterName(DEFAULT_FILTER_NAME);
    }

    private String createUniqueFilterName(final String baseFilterName) {
        int index = 1;
        String filterNameExt = ""; //NOI18N

        while (containsFilterName(baseFilterName + filterNameExt)) {
            filterNameExt = " (" + ++index + ")"; //NOI18N
        }

        return baseFilterName + filterNameExt;
    }

    private void deleteSelectedFilters() {
        final int nFiltersToDelete = filterTable.getSelectedRowCount();
        final int nNewFilters = filterNames.length - nFiltersToDelete;

        int rowToSelect = 0;

        final String[] newFilterNames = new String[nNewFilters];
        final String[] newFilterValues = new String[nNewFilters];

        int index = 0;

        for (int i = 0; i < filterNames.length; i++) {
            if (!filterTable.isRowSelected(i)) {
                newFilterNames[index] = filterNames[i];
                newFilterValues[index] = filterValues[i];
                index++;
            } else {
                rowToSelect = Math.max(0, index - 1);
                filterTable.removeRowSelectionInterval(i, i);
            }
        }

        stopFilterTableEditing();

        filterNames = newFilterNames;
        filterValues = newFilterValues;

        filterTable.invalidate();
        revalidate();
        repaint();

        if (filterTable.getRowCount() > 0) {
            filterTable.addRowSelectionInterval(rowToSelect, rowToSelect);
            makeSelectedFilterVisible();
        }

        updateOKButton();
    }

    private void editSelectedCell() {
        filterTable.editCellAt(filterTable.getSelectedRow(), filterTable.getSelectedColumn(),
                               new java.util.EventObject(filterTable));
    }

    private void editSelectedRow(final int column) {
        filterTable.editCellAt(filterTable.getSelectedRow(), column, new java.util.EventObject(filterTable));
    }

    private void initComponents() {
        // buttons to export
        OKButton = new JButton();
        org.openide.awt.Mnemonics.setLocalizedText(OKButton, OK_BUTTON_TEXT);
        CancelButton = new JButton();
        org.openide.awt.Mnemonics.setLocalizedText(CancelButton, CANCEL_BUTTON_TEXT);

        // listeners
        buttonsListener = new ButtonsListener();
        selectionListener = new SelectionListener();

        // definedFiltersLabel
        definedFiltersLabel = new JLabel();
        org.openide.awt.Mnemonics.setLocalizedText(definedFiltersLabel, DEFINED_FILTERS_LABEL_TEXT);
        definedFiltersLabel.setOpaque(false);
        definedFiltersLabel.setBorder(BorderFactory.createEmptyBorder(15, 5, 0, 5));
        add(definedFiltersLabel, BorderLayout.NORTH);

        // filterTable renderers
        final LabelTableCellRenderer filterTableCellRenderer = new LabelTableCellRenderer() {
            protected void setState(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                if (column == 1) {
                    if (isValidFilter((String) value)) {
                        label.setForeground(UIManager.getColor("Label.foreground")); // NOI18N
                    } else {
                        label.setForeground(Color.red);
                    }
                }
            }
            ;
        };

        filterTableCellRenderer.setSupportsFocusBorder(true);

        final BooleanTableCellRenderer booleanTableCellRenderer = new BooleanTableCellRenderer();
        booleanTableCellRenderer.setSupportsFocusBorder(true);

        // filterTable editors
        filterTableStringCellEditorComponent = new JTextField();
        filterTableStringCellEditorComponent.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UIUtils
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             .getDarkerLine(UIConstants.TABLE_SELECTION_BACKGROUND_COLOR,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            0.65f)),
                                                                                          BorderFactory.createEmptyBorder(1, 2,
                                                                                                                          1, 3)));
        filterTableStringCellEditorComponent.setSelectionColor(UIConstants.TABLE_SELECTION_BACKGROUND_COLOR);
        filterTableStringCellEditorComponent.setSelectedTextColor(UIConstants.TABLE_SELECTION_FOREGROUND_COLOR);
        filterTableStringCellEditorComponent.addKeyListener(new CellEditorKeyListener());
        filterTableStringCellEditorComponent.addFocusListener(new CellEditorFocusListener());
        filterTableStringCellEditorComponent.getDocument().addDocumentListener(new CellEditorDocumentListener());

        final DefaultCellEditor filterTableStringCellEditor = new DefaultCellEditor(filterTableStringCellEditorComponent);

        // filterTable
        filterTable = new JExtendedTable(new FilterTableModel()) {
                public boolean editCellAt(final int row, final int column, final java.util.EventObject e) {
                    final boolean canEdit = super.editCellAt(row, column, e);

                    if (canEdit) {
                        final Component c = getEditorComponent();

                        if (c != null) {
                            c.requestFocus();
                            c.repaint();
                        }
                    }

                    return canEdit;
                }
            };
        definedFiltersLabel.setLabelFor(filterTable);
        filterTable.getAccessibleContext().setAccessibleName(FILTER_TABLE_ACCESS_NAME);
        filterTable.setSurrendersFocusOnKeystroke(true);
        filterTable.setRowSelectionAllowed(true);
        filterTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        filterTable.setGridColor(UIConstants.TABLE_VERTICAL_GRID_COLOR);
        filterTable.setSelectionBackground(UIConstants.TABLE_SELECTION_BACKGROUND_COLOR);
        filterTable.setSelectionForeground(UIConstants.TABLE_SELECTION_FOREGROUND_COLOR);
        filterTable.setShowHorizontalLines(UIConstants.SHOW_TABLE_HORIZONTAL_GRID);
        filterTable.setShowVerticalLines(UIConstants.SHOW_TABLE_VERTICAL_GRID);
        filterTable.setRowMargin(UIConstants.TABLE_ROW_MARGIN);
        filterTable.getTableHeader().setReorderingAllowed(false);
        filterTable.getSelectionModel().addListSelectionListener(selectionListener);
        filterTable.setDefaultRenderer(String.class, filterTableCellRenderer);
        filterTable.setDefaultRenderer(Boolean.class, booleanTableCellRenderer);
        filterTable.setRowHeight(UIUtils.getDefaultRowHeight() + 2);
        filterTable.setDefaultEditor(String.class, filterTableStringCellEditor);
        filterTable.addKeyListener(new FilterTableKeyListener());

        // filterTableScrollPane
        final JScrollPane filterTableScrollPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                                                  JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        final FilterTableViewport filterTableViewport = new FilterTableViewport(filterTable);
        filterTableScrollPane.setViewport(filterTableViewport);
        filterTableScrollPane.addMouseWheelListener(filterTable);
        filterTableScrollPane.getVerticalScrollBar().getModel().addChangeListener(new ChangeListener() {
                public void stateChanged(final ChangeEvent e) {
                    if (filterTableScrollPane.getVerticalScrollBar().getModel().getExtent() == filterTableScrollPane.getVerticalScrollBar()
                                                                                                                        .getModel()
                                                                                                                        .getMaximum()) {
                        filterTableScrollPane.getVerticalScrollBar().setEnabled(false);
                    } else {
                        filterTableScrollPane.getVerticalScrollBar().setEnabled(true);
                    }
                }
            });
        filterTableScrollPane.setPreferredSize(new Dimension(490, 370));

        // filterTablePanel
        final JPanel filterTablePanel = new JPanel(new BorderLayout());
        filterTablePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 3));
        filterTablePanel.add(filterTableScrollPane, BorderLayout.CENTER);

        // newButton
        newButton = new JButton();
        org.openide.awt.Mnemonics.setLocalizedText(newButton, NEW_BUTTON_TEXT);
        
        newButton.getAccessibleContext().setAccessibleDescription(NEW_BUTTON_ACCESS_DESC);
        newButton.addActionListener(buttonsListener);

        // editButton
        editButton = new JButton();
        org.openide.awt.Mnemonics.setLocalizedText(editButton, EDIT_BUTTON_TEXT); // Actually no mnemonics, see Issue 116514
        editButton.getAccessibleContext().setAccessibleDescription(EDIT_BUTTON_ACCESS_DESC);
        editButton.setEnabled(false);
        editButton.addActionListener(buttonsListener);

        // deleteButton
        deleteButton = new JButton();
        org.openide.awt.Mnemonics.setLocalizedText(deleteButton, DELETE_BUTTON_TEXT);
        deleteButton.getAccessibleContext().setAccessibleDescription(DELETE_BUTTON_ACCESS_DESC);
        deleteButton.setEnabled(false);
        deleteButton.addActionListener(buttonsListener);

        // moveUpButton
        moveUpButton = new JButton();
        org.openide.awt.Mnemonics.setLocalizedText(moveUpButton, MOVE_UP_BUTTON_TEXT);
        moveUpButton.getAccessibleContext().setAccessibleDescription(MOVE_UP_BUTTON_ACCESS_DESC);
        moveUpButton.setEnabled(false);
        moveUpButton.addActionListener(buttonsListener);

        // moveDownButton
        moveDownButton = new JButton();
        org.openide.awt.Mnemonics.setLocalizedText(moveDownButton, MOVE_DOWN_BUTTON_TEXT);
        moveDownButton.getAccessibleContext().setAccessibleDescription(MOVE_DOWN_BUTTON_ACCESS_DESC);
        moveDownButton.setEnabled(false);
        moveDownButton.addActionListener(buttonsListener);

        // buttonsPanel
        final JPanel buttonsPanel1 = new JPanel(new GridLayout(6, 1, 0, 5));
        buttonsPanel1.add(newButton);
        buttonsPanel1.add(editButton);
        buttonsPanel1.add(deleteButton);
        buttonsPanel1.add(new JPanel());
        buttonsPanel1.add(moveUpButton);
        buttonsPanel1.add(moveDownButton);

        final JPanel buttonsPanel = new JPanel(new BorderLayout());
        buttonsPanel.setBorder(BorderFactory.createEmptyBorder(15, 5, 5, 5));
        buttonsPanel.add(buttonsPanel1, BorderLayout.NORTH);

        Color panelBackground = UIManager.getColor("Panel.background"); //NOI18N
        Color hintBackground = UIUtils.getSafeColor(panelBackground.getRed() - 10, panelBackground.getGreen() - 10,
                                                    panelBackground.getBlue() - 10);
        // hintArea
        hintArea = new HTMLTextArea() {
                public Dimension getPreferredSize() { // Workaround to force the text area not to consume horizontal space to fit the contents to just one line

                    return new Dimension(1, super.getPreferredSize().height);
                }
            };
        hintArea.setText(HINT_MSG); // NOI18N
        hintArea.setEnabled(false);
        hintArea.setDisabledTextColor(Color.darkGray);
        hintArea.setBackground(hintBackground);
        hintArea.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(7, 7, 10, 7,
                                                                                              UIManager.getColor("Panel.background")), //NOI18N
                                                              BorderFactory.createMatteBorder(10, 10, 10, 10, hintBackground)));

        // this
        add(filterTablePanel, BorderLayout.CENTER);
        add(buttonsPanel, BorderLayout.EAST);
        add(hintArea, java.awt.BorderLayout.SOUTH);

        updateOKButton();
    }

    //--- Private implementation -----
    private void makeSelectedFilterVisible() {
        filterTable.scrollRectToVisible(filterTable.getCellRect(filterTable.getSelectedRow(), filterTable.getSelectedColumn(),
                                                                true));
    }

    private void moveSelectedRowDown() {
        stopFilterTableEditing();

        final int selectedRow = filterTable.getSelectedRow();

        if (selectedRow < (filterTable.getRowCount() - 1)) {
            final String tmpFilterName = filterNames[selectedRow + 1];
            final String tmpFilterValue = filterValues[selectedRow + 1];

            filterNames[selectedRow + 1] = filterNames[selectedRow];
            filterValues[selectedRow + 1] = filterValues[selectedRow];

            filterNames[selectedRow] = tmpFilterName;
            filterValues[selectedRow] = tmpFilterValue;

            filterTable.repaint();
            filterTable.clearSelection();
            filterTable.addRowSelectionInterval(selectedRow + 1, selectedRow + 1);
            makeSelectedFilterVisible();
        }
    }

    private void moveSelectedRowUp() {
        stopFilterTableEditing();

        final int selectedRow = filterTable.getSelectedRow();

        if (selectedRow > 0) {
            final String tmpFilterName = filterNames[selectedRow - 1];
            final String tmpFilterValue = filterValues[selectedRow - 1];

            filterNames[selectedRow - 1] = filterNames[selectedRow];
            filterValues[selectedRow - 1] = filterValues[selectedRow];

            filterNames[selectedRow] = tmpFilterName;
            filterValues[selectedRow] = tmpFilterValue;

            filterTable.repaint();
            filterTable.clearSelection();
            filterTable.addRowSelectionInterval(selectedRow - 1, selectedRow - 1);
            makeSelectedFilterVisible();
        }
    }

    private void processUniqueFilterNameCheckAt(final int row, final int column) {
        if (column != 0) {
            return;
        }

        filterNames[row] = filterNames[row].trim();

        final boolean noFilterNameDefined = (filterNames[row].length() == 0);

        if ((noFilterNameDefined) || (containsFilterNameExceptRow(filterNames[row], row))) {
            stopFilterTableEditing();

            if (noFilterNameDefined) {
                filterNames[row] = createUniqueFilterName();
            } else {
                filterNames[row] = createUniqueFilterName(filterNames[row]);
            }
        }

        filterTable.repaint();
    }

    private void stopFilterTableEditing() {
        if (filterTable.getCellEditor() == null) {
            return;
        }

        if ((filterNames.length == 0) || (filterValues.length == 0)) {
            return;
        }

        (filterTable.getCellEditor()).stopCellEditing();
    }

    private void updateOKButton() {
        if (OKButton == null) {
            return;
        }

        for (int i = 0; i < filterValues.length; i++) {
            if (!isValidFilter(filterValues[i])) {
                OKButton.setEnabled(false);

                return;
            }
        }

        OKButton.setEnabled(true);
    }

    /**
     * @param args the command line arguments
     */

    /*  public static void main (String[] args) {
       try {
         UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel"); //NOI18N
         //UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel"); //NOI18N
         //UIManager.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel"); //NOI18N
         //UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel"); //NOI18N
       } catch (Exception e){};
       JFrame frame = new JFrame("GlobalFiltersPanel viewer"); //NOI18N
       GlobalFiltersPanel filterListPanel = new GlobalFiltersPanel(new String[]{"Java classes", "Javax classes", "Sun classes"}, //NOI18N
                                                                   new String[]{"java.*", "javax.*", "sun.*"}); //NOI18N
       frame.getContentPane().add(filterListPanel);
       frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
       frame.pack();
       frame.show();
       }
     */
}
