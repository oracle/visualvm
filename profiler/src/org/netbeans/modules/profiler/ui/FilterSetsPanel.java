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
import org.netbeans.lib.profiler.common.filters.DefinedFilterSets;
import org.netbeans.lib.profiler.common.filters.FilterSet;
import org.netbeans.lib.profiler.ui.UIConstants;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import org.netbeans.lib.profiler.ui.components.JExtendedTable;
import org.netbeans.lib.profiler.ui.components.table.BooleanTableCellRenderer;
import org.netbeans.lib.profiler.ui.components.table.LabelTableCellRenderer;
import org.netbeans.modules.profiler.NetBeansProfiler;
import org.openide.DialogDescriptor;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import java.awt.*;
import java.awt.event.*;
import java.util.HashSet;
import java.util.Set;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;


/**
 *
 * @author Tomas Hurka
 * @author  Jiri Sedlacek
 */
public final class FilterSetsPanel extends JPanel implements ActionListener, HelpCtx.Provider {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private final class ActiveFiltersTableKeyListener extends KeyAdapter {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void keyPressed(final KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_SPACE:
                    e.consume();
                    toggleSelectedRowCheck();

                    break;
                case KeyEvent.VK_ENTER:
                    toggleSelectedRowCheck();

                    break;
                case KeyEvent.VK_ESCAPE:
                    FilterSetsPanel.this.processKeyEvent(e);

                    break;
            }
        }
    }

    private final class ActiveFiltersTableModel extends AbstractTableModel {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public boolean isCellEditable(final int rowIndex, final int columnIndex) {
            if (columnIndex == 0) {
                return true;
            } else {
                return false;
            }
        }

        public Class getColumnClass(final int columnIndex) {
            return columnClasses[columnIndex];
        }

        public int getColumnCount() {
            return 3;
        }

        public String getColumnName(final int columnIndex) {
            return columnNames[columnIndex];
        }

        public int getRowCount() {
            return nbProfiler.getGlobalFilters().getFilterNames().length;
        }

        public void setValueAt(final Object aValue, final int rowIndex, final int columnIndex) {
            if (columnIndex == 0) {
                selectedFilterSetChecks[rowIndex] = (Boolean) aValue;

                final boolean selected = ((Boolean) aValue).booleanValue();

                if (selected) {
                    selectedFilterSet.addActiveGlobalFilter(nbProfiler.getGlobalFilters().getFilterNames()[rowIndex]);
                } else {
                    selectedFilterSet.removeActiveGlobalFilter(nbProfiler.getGlobalFilters().getFilterNames()[rowIndex]);
                }
            }
        }

        public Object getValueAt(final int rowIndex, final int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return selectedFilterSetChecks[rowIndex];
                case 1:
                    return nbProfiler.getGlobalFilters().getFilterNames()[rowIndex];
                case 2:
                    return nbProfiler.getGlobalFilters().getFilterValues()[rowIndex];
            }

            return null;
        }
    }

    private final class ActiveFiltersTableMouseListener extends MouseAdapter {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void mouseClicked(final MouseEvent e) {
            if ((e.getModifiers() == InputEvent.BUTTON1_MASK) && ((e.getClickCount() % 2) == 0)) {
                int selectedColumn = activeFiltersTable.getSelectedColumn();

                if (selectedColumn == 0) {
                    return;
                } else if (selectedColumn == 1) {
                    toggleSelectedRowCheck();

                    return;
                } else if (selectedColumn == 2) {
                    SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                editGlobalFilterAtRow(activeFiltersTable.getSelectedRow());
                            }
                        });
                }
            }
        }

        private void editGlobalFilterAtRow(int row) {
            final GlobalFiltersPanel globalFiltersPanel = GlobalFiltersPanel.getDefault();

            // TODO: implement String getDialogCaption() for FilterSetsPanel, GlobalFiltersPanel and QuickFilterPanel
            final DialogDescriptor dd = new DialogDescriptor(globalFiltersPanel, EDIT_GLOBAL_FILTERS_DIALOG_CAPTION, true,
                                                             new Object[] {
                                                                 DialogDescriptor.OK_OPTION, DialogDescriptor.CANCEL_OPTION
                                                             }, DialogDescriptor.OK_OPTION, DialogDescriptor.BOTTOM_ALIGN, null,
                                                             null);
            final Dialog d = ProfilerDialogs.createDialog(dd);
            globalFiltersPanel.init();
            d.pack(); // allows correct resizing of textarea in GlobalFiltersPanel
            globalFiltersPanel.editFilterValueAtRow(row);
            d.setVisible(true);

            if (dd.getValue() == DialogDescriptor.OK_OPTION) {
                globalFiltersPanel.applyChanges();
                FilterSetsPanel.getDefault().processGlobalFiltersChanged();
            }
        }
    }

    private final class ActiveFiltersTableViewport extends JViewport implements TableColumnModelListener {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private final JTableHeader tableHeader;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public ActiveFiltersTableViewport(final JTable table) {
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

    private final class DefinedFilterSetsListKeyListener extends KeyAdapter {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void keyPressed(final KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_DELETE:
                    e.consume();
                    removeSelectedFilterSet();

                    break;
                case KeyEvent.VK_N:

                    if (e.getModifiers() == InputEvent.CTRL_MASK) {
                        e.consume();
                        addNewFilterSet();

                        break;
                    }
                case KeyEvent.VK_UP:

                    if (e.getModifiers() == InputEvent.CTRL_MASK) {
                        e.consume();

                        if (definedFilterSetsList.getSelectedIndex() > 0) {
                            moveSelectedFilterUp();
                        }

                        break;
                    }
                case KeyEvent.VK_DOWN:

                    if (e.getModifiers() == InputEvent.CTRL_MASK) {
                        e.consume();

                        if (definedFilterSetsList.getSelectedIndex() < (definedFilterSetsList.getModel().getSize() - 1)) {
                            moveSelectedFilterDown();
                        }

                        break;
                    }
                case KeyEvent.VK_ESCAPE:
                    FilterSetsPanel.this.processKeyEvent(e);

                    break;
            }
        }
    }

    private final class DefinedFilterSetsListModel extends AbstractListModel {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public Object getElementAt(final int index) {
            return filterSets.getFilterSetAt(index).getFilterSetName();
        }

        public int getSize() {
            return filterSets.getFilterSetsCount();
        }

        public void fireContentsChanged(final int index0, final int index1) {
            fireContentsChanged(this, index0, index1);
        }

        public void fireIntervalAdded(final int index0, final int index1) {
            fireIntervalAdded(this, index0, index1);
        }

        public void fireIntervalRemoved(final int index0, final int index1) {
            fireIntervalRemoved(this, index0, index1);
        }
    }

    private final class DefinedFilterSetsListSelectionListener implements ListSelectionListener {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void valueChanged(final ListSelectionEvent e) {
            updateSelection();

            if (selectedFilterSetIndex == -1) {
                removeFilterSetButton.setEnabled(false);
                moveUpButton.setEnabled(false);
                moveDownButton.setEnabled(false);
            } else {
                removeFilterSetButton.setEnabled(true);

                if (selectedFilterSetIndex > 0) {
                    moveUpButton.setEnabled(true);
                } else {
                    moveUpButton.setEnabled(false);
                }

                if (selectedFilterSetIndex < (definedFilterSetsList.getModel().getSize() - 1)) {
                    moveDownButton.setEnabled(true);
                } else {
                    moveDownButton.setEnabled(false);
                }
            }
        }
    }

    private final class FilterNameTextFieldDocumentListener implements DocumentListener {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void changedUpdate(final DocumentEvent e) {
            updateDefinedFilterSetsList();
        }

        public void insertUpdate(final DocumentEvent e) {
            updateDefinedFilterSetsList();
        }

        public void removeUpdate(final DocumentEvent e) {
            updateDefinedFilterSetsList();
        }

        private void updateDefinedFilterSetsList() {
            if (selectedFilterSet == null) return;

            String selectedFilterSetName = filterNameTextField.getText().trim();

            if (selectedFilterSetName.length() == 0) {
                selectedFilterSet.setFilterSetName(""); //NOI18N
                selectedFilterSetName = createUniqueFilterSetName();
            }

            selectedFilterSet.setFilterSetName(selectedFilterSetName);

            final DefinedFilterSetsListModel listModel = (DefinedFilterSetsListModel) definedFilterSetsList.getModel();
            listModel.fireContentsChanged(definedFilterSetsList.getSelectedIndex(), definedFilterSetsList.getSelectedIndex());
        }
    }

    private final class FilterNameTextFieldFocusListener extends FocusAdapter {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private int lastEditedFilterSetIndex;

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void focusGained(final FocusEvent e) {
            lastEditedFilterSetIndex = definedFilterSetsList.getSelectedIndex();
            filterSetNameBeforeEditing = filterNameTextField.getText();
        }

        public void focusLost(final FocusEvent e) {
            processUniqueFilterSetNameCheckAt(lastEditedFilterSetIndex);
        }
    }

    private final class FilterNameTextFieldKeyListener extends KeyAdapter {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void keyPressed(final KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_ENTER:
                    e.consume();
                    processUniqueFilterSetNameCheckAt(definedFilterSetsList.getSelectedIndex());
                    filterTypeExclusiveRadio.requestFocus();

                    break;
                case KeyEvent.VK_ESCAPE:
                    e.consume();
                    filterNameTextField.setText(filterSetNameBeforeEditing);
                    filterNameTextField.selectAll();

                    break;
            }
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String COLUMN_NAME_ACTIVE = NbBundle.getMessage(FilterSetsPanel.class, "FilterSetsPanel_ColumnNameActive"); //NOI18N
    private static final String COLUMN_NAME_NAME = NbBundle.getMessage(FilterSetsPanel.class, "FilterSetsPanel_ColumnNameName"); //NOI18N
    private static final String COLUMN_NAME_VALUE = NbBundle.getMessage(FilterSetsPanel.class, "FilterSetsPanel_ColumnNameValue"); //NOI18N
    private static final String ADD_FILTER_SET_BUTTON_TEXT = NbBundle.getMessage(FilterSetsPanel.class,
                                                                                 "FilterSetsPanel_AddFilterSetButtonText"); //NOI18N
    private static final String REMOVE_FILTER_SET_BUTTON_TEXT = NbBundle.getMessage(FilterSetsPanel.class,
                                                                                    "FilterSetsPanel_RemoveFilterSetButtonText"); //NOI18N
    private static final String MOVE_UP_BUTTON_TEXT = NbBundle.getMessage(FilterSetsPanel.class,
                                                                          "FilterSetsPanel_MoveUpButtonText"); //NOI18N
    private static final String MOVE_DOWN_BUTTON_TEXT = NbBundle.getMessage(FilterSetsPanel.class,
                                                                            "FilterSetsPanel_MoveDownButtonText"); //NOI18N
    private static final String FILTERSET_NAME_LABEL_TEXT = NbBundle.getMessage(FilterSetsPanel.class,
                                                                                "FilterSetsPanel_FilterSetNameLabelText"); //NOI18N
    private static final String FILTERSET_TYPE_LABEL_TEXT = NbBundle.getMessage(FilterSetsPanel.class,
                                                                                "FilterSetsPanel_FilterSetTypeLabelText"); //NOI18N
    private static final String FILTERSET_TYPE_EXCLUSIVE_RADIO_TEXT = NbBundle.getMessage(FilterSetsPanel.class,
                                                                                          "FilterSetsPanel_FilterSetTypeExclusiveRadioText"); //NOI18N
    private static final String FILTERSET_TYPE_INCLUSIVE_RADIO_TEXT = NbBundle.getMessage(FilterSetsPanel.class,
                                                                                          "FilterSetsPanel_FilterSetTypeInclusiveRadioText"); //NOI18N
    private static final String ACTIVE_FILTERS_LABEL_TEXT = NbBundle.getMessage(FilterSetsPanel.class,
                                                                                "FilterSetsPanel_ActiveFiltersLabelText"); //NOI18N
    private static final String DEFINED_FILTERSETS_BORDER_CAPTION = NbBundle.getMessage(FilterSetsPanel.class,
                                                                                        "FilterSetsPanel_DefinedFilterSetsBorderCaption"); //NOI18N
    private static final String FILTERSET_PROPERTIES_BORDER_CAPTION = NbBundle.getMessage(FilterSetsPanel.class,
                                                                                          "FilterSetsPanel_FilterSetPropertiesBorderCaption"); //NOI18N
    private static final String EDIT_GLOBAL_FILTERS_DIALOG_CAPTION = NbBundle.getMessage(FilterSetsPanel.class,
                                                                                         "FilterSetsPanel_EditGlobalFiltersDialogCaption"); //NOI18N
    private static final String HINT_MSG = NbBundle.getMessage(FilterSetsPanel.class, "FilterSetsPanel_HintMsg"); //NOI18N
    private static final String DEFINED_FILTER_SETS_LIST_ACCESS_NAME = NbBundle.getMessage(FilterSetsPanel.class,
                                                                                           "FilterSetsPanel_DefinedFilterSetsListAccessName"); //NOI18N
    private static final String DEFINED_FILTER_SETS_LIST_ACCESS_DESCR = NbBundle.getMessage(FilterSetsPanel.class,
                                                                                            "FilterSetsPanel_DefinedFilterSetsListAccessDescr"); //NOI18N
    private static final String ADD_FILTER_SET_BUTTON_ACCESS_DESCR = NbBundle.getMessage(FilterSetsPanel.class,
                                                                                         "FilterSetsPanel_AddFilterSetButtonAccessDescr"); //NOI18N
    private static final String REMOVE_FILTER_SET_BUTTON_ACCESS_DESCR = NbBundle.getMessage(FilterSetsPanel.class,
                                                                                            "FilterSetsPanel_RemoveFilterSetButtonAccessDescr"); //NOI18N
    private static final String MOVE_UP_BUTTON_ACCESS_DESCR = NbBundle.getMessage(FilterSetsPanel.class,
                                                                                  "FilterSetsPanel_MoveUpButtonAccessDescr"); //NOI18N
    private static final String MOVE_DOWN_BUTTON_ACCESS_DESCR = NbBundle.getMessage(FilterSetsPanel.class,
                                                                                    "FilterSetsPanel_MoveDownButtonAccessDescr"); //NOI18N
    private static final String ACTIVE_FILTERS_TABLE_ACCESS_NAME = NbBundle.getMessage(FilterSetsPanel.class,
                                                                                       "FilterSetsPanel_ActiveFiltersTableAccessName"); //NOI18N
    private static final String ACTIVE_FILTERS_TABLE_ACCESS_DESCR = NbBundle.getMessage(FilterSetsPanel.class,
                                                                                        "FilterSetsPanel_ActiveFiltersTableAccessDescr"); //NOI18N
    private static final String FILTER_TYPE_EXCLUSIVE_RADIO_ACCESS_DESCR = NbBundle.getMessage(FilterSetsPanel.class,
                                                                                               "FilterSetsPanel_FilterTypeExclusiveRadioAccessDescr"); //NOI18N
    private static final String FILTER_TYPE_INCLUSIVE_RADIO_ACCESS_DESCR = NbBundle.getMessage(FilterSetsPanel.class,
                                                                                               "FilterSetsPanel_FilterTypeInclusiveRadioAccessDescr"); //NOI18N
                                                                                                                                                       // -----
    private static final String HELP_CTX_KEY = "FilterSetsPanel.HelpCtx"; // NOI18N
    private static final HelpCtx HELP_CTX = new HelpCtx(HELP_CTX_KEY);
    private static FilterSetsPanel defaultInstance;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    public final DefinedFilterSets filterSets;
    private ButtonGroup filterTypeButtonGroup;
    private FilterSet selectedFilterSet;
    private HTMLTextArea hintArea;
    private JButton addFilterSetButton;
    private JButton moveDownButton;
    private JButton moveUpButton;
    private JButton removeFilterSetButton;
    private JExtendedTable activeFiltersTable;
    private JLabel activeFiltersLabel;
    private JLabel filterNameLabel;
    private JLabel filterTypeLabel;
    private JList definedFilterSetsList;
    private JPanel buttonsPanel;
    private JPanel filterSetPropertiesPanel;
    private JPanel filterSetSettingsPanel;
    private JPanel filterSetsPreviewPanel;
    private JRadioButton filterTypeExclusiveRadio;
    private JRadioButton filterTypeInclusiveRadio;
    private JScrollPane activeFiltersScrollPane;
    private JScrollPane definedFilterSetsListScrollPane;
    private JTextField filterNameTextField;
    private final NetBeansProfiler nbProfiler;
    private final Class[] columnClasses;
    private final String[] columnNames;
    private String filterSetNameBeforeEditing;
    private Boolean[] selectedFilterSetChecks = new Boolean[0];
    private int selectedFilterSetIndex;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates new form FilterSetsPanel */
    private FilterSetsPanel() {
        nbProfiler = (NetBeansProfiler) Profiler.getDefault();
        filterSets = new DefinedFilterSets();

        columnNames = new String[] { COLUMN_NAME_ACTIVE, COLUMN_NAME_NAME, COLUMN_NAME_VALUE };
        columnClasses = new Class[] { Boolean.class, String.class, String.class };

        initComponents();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static FilterSetsPanel getDefault() {
        if (defaultInstance == null) {
            defaultInstance = new FilterSetsPanel();
        }

        return defaultInstance;
    }

    public HelpCtx getHelpCtx() {
        return HELP_CTX;
    }

    public FilterSet getSelectedFilterSet() {
        int selectedIndex = getSelectedFilterSetIndex();

        if (selectedIndex == -1) {
            return null;
        } else {
            return filterSets.getFilterSetAt(selectedIndex);
        }
    }

    public int getSelectedFilterSetIndex() {
        return definedFilterSetsList.getSelectedIndex();
    }

    public String getSelectedFilterSetName() {
        return (String) definedFilterSetsList.getSelectedValue();
    }

    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == addFilterSetButton) {
            addNewFilterSet();
        } else if (e.getSource() == removeFilterSetButton) {
            removeSelectedFilterSet();
        } else if (e.getSource() == moveUpButton) {
            moveSelectedFilterUp();
        } else if (e.getSource() == moveDownButton) {
            moveSelectedFilterDown();
        } else if (e.getSource() == filterTypeExclusiveRadio) {
            if (filterTypeExclusiveRadio.isSelected()) {
                selectedFilterSet.setExclusive();
            }
        } else if (e.getSource() == filterTypeInclusiveRadio) {
            if (filterTypeInclusiveRadio.isSelected()) {
                selectedFilterSet.setInclusive();
            }
        }
    }

    public void applyChanges() {
        nbProfiler.getDefinedFilterSets().setValuesFrom(filterSets);
        nbProfiler.saveGlobalFilters();
    }

    public void init(final int initialSelectedIndex) {
        filterSets.setValuesFrom(nbProfiler.getDefinedFilterSets());

        if (initialSelectedIndex < filterSets.getFilterSetsCount()) {
            definedFilterSetsList.setSelectedIndex(initialSelectedIndex);
            makeDefinedFilterSetsListSelectionVisible();
        } else {
            definedFilterSetsList.clearSelection();
        }

        updateSelection();
    }

    public void processGlobalFiltersChanged() {
        if (activeFiltersTable != null) {
            updateSelectedFilterSetProperties();
            ((AbstractTableModel) (activeFiltersTable.getModel())).fireTableDataChanged();
            activeFiltersTable.repaint();
        }
    }

    private void addNewFilterSet() {
        if (definedFilterSetsList != null) {
            filterSets.addFilterSet(new FilterSet(createUniqueFilterSetName()));

            final DefinedFilterSetsListModel listModel = (DefinedFilterSetsListModel) definedFilterSetsList.getModel();
            listModel.fireIntervalAdded(listModel.getSize(), listModel.getSize());
            definedFilterSetsList.setSelectedIndex(listModel.getSize() - 1);
            makeDefinedFilterSetsListSelectionVisible();
            filterNameTextField.selectAll();
            filterNameTextField.requestFocus();
        }
    }

    private boolean containsFilterSetName(final String filterName) {
        for (int i = 0; i < filterSets.getFilterSetsCount(); i++) {
            if (filterSets.getFilterSetAt(i).getFilterSetName().equals(filterName)) {
                return true;
            }
        }

        return false;
    }

    private boolean containsFilterSetNameExceptIndex(final String filterName, final int index) {
        for (int i = 0; i < filterSets.getFilterSetsCount(); i++) {
            if ((i != index) && (filterSets.getFilterSetAt(i).getFilterSetName().equals(filterName))) {
                return true;
            }
        }

        return false;
    }

    private String createUniqueFilterSetName() {
        return createUniqueFilterSetName(FilterSet.DEFAULT_FILTERSET_NAME);
    }

    private String createUniqueFilterSetName(final String baseFilterSetName) {
        int index = 1;
        String filterSetNameExt = ""; //NOI18N

        while (containsFilterSetName(baseFilterSetName + filterSetNameExt)) {
            filterSetNameExt = " (" + ++index + ")"; //NOI18N
        }

        return baseFilterSetName + filterSetNameExt;
    }

    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        filterTypeButtonGroup = new ButtonGroup();
        filterSetsPreviewPanel = new JPanel();
        definedFilterSetsList = new JList();
        definedFilterSetsListScrollPane = new JScrollPane();
        buttonsPanel = new JPanel();
        addFilterSetButton = new JButton();
        removeFilterSetButton = new JButton();
        moveUpButton = new JButton();
        moveDownButton = new JButton();
        filterSetPropertiesPanel = new JPanel();
        //activeFiltersTable = new JExtendedTable(); // Defined later
        filterSetSettingsPanel = new JPanel();
        filterNameLabel = new JLabel();
        filterNameTextField = new JTextField();
        filterTypeLabel = new JLabel();
        filterTypeExclusiveRadio = new JRadioButton();
        filterTypeInclusiveRadio = new JRadioButton();
        activeFiltersLabel = new JLabel();
        hintArea = new HTMLTextArea() {
                public Dimension getPreferredSize() { // Workaround to force the text area not to consume horizontal space to fit the contents to just one line

                    return new Dimension(1, super.getPreferredSize().height);
                }
            };

        setLayout(new java.awt.BorderLayout());

        final TitledBorder filterSetsPreviewPanelTitledBorder = BorderFactory.createTitledBorder(DEFINED_FILTERSETS_BORDER_CAPTION);

        filterSetsPreviewPanel.setLayout(new java.awt.BorderLayout());
        filterSetsPreviewPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(7, 5, 5, 5),
                                                                            filterSetsPreviewPanelTitledBorder));

        definedFilterSetsList.getAccessibleContext().setAccessibleName(DEFINED_FILTER_SETS_LIST_ACCESS_NAME);
        definedFilterSetsList.getAccessibleContext().setAccessibleDescription(DEFINED_FILTER_SETS_LIST_ACCESS_DESCR);
        definedFilterSetsList.setModel(new DefinedFilterSetsListModel());
        definedFilterSetsList.setCellRenderer(new DefaultListCellRenderer() {
                public java.awt.Component getListCellRendererComponent(final JList list, final Object value, final int index,
                                                                       final boolean isSelected, final boolean cellHasFocus) {
                    return super.getListCellRendererComponent(list, value, index, isSelected, false);
                }
            });
        definedFilterSetsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        definedFilterSetsList.setSelectionBackground(UIConstants.TABLE_SELECTION_BACKGROUND_COLOR);
        definedFilterSetsList.setSelectionForeground(UIConstants.TABLE_SELECTION_FOREGROUND_COLOR);
        definedFilterSetsList.setVisibleRowCount(5);
        definedFilterSetsList.addListSelectionListener(new DefinedFilterSetsListSelectionListener());
        definedFilterSetsList.addKeyListener(new DefinedFilterSetsListKeyListener());

        definedFilterSetsListScrollPane.setViewportView(definedFilterSetsList);
        definedFilterSetsListScrollPane.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(5, 5, 5, 0),
                                                                                     definedFilterSetsListScrollPane.getBorder()));
        definedFilterSetsListScrollPane.setPreferredSize(new Dimension(470, 1));

        filterSetsPreviewPanel.add(definedFilterSetsListScrollPane, java.awt.BorderLayout.CENTER);

        //buttonsPanel.setLayout(new java.awt.GridLayout(5, 1, 0, 5));
        buttonsPanel.setLayout(new java.awt.GridLayout(4, 1, 0, 5));
        buttonsPanel.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 5));

        org.openide.awt.Mnemonics.setLocalizedText(addFilterSetButton, ADD_FILTER_SET_BUTTON_TEXT);
        addFilterSetButton.getAccessibleContext().setAccessibleDescription(ADD_FILTER_SET_BUTTON_ACCESS_DESCR);
        addFilterSetButton.addActionListener(this);
        buttonsPanel.add(addFilterSetButton);

        org.openide.awt.Mnemonics.setLocalizedText(removeFilterSetButton, REMOVE_FILTER_SET_BUTTON_TEXT);
        removeFilterSetButton.getAccessibleContext().setAccessibleDescription(REMOVE_FILTER_SET_BUTTON_ACCESS_DESCR);
        removeFilterSetButton.setEnabled(false);
        removeFilterSetButton.addActionListener(this);
        buttonsPanel.add(removeFilterSetButton);

        //buttonsPanel.add(new JPanel());
        org.openide.awt.Mnemonics.setLocalizedText(moveUpButton, MOVE_UP_BUTTON_TEXT);
        moveUpButton.getAccessibleContext().setAccessibleDescription(MOVE_UP_BUTTON_ACCESS_DESCR);
        moveUpButton.setEnabled(false);
        moveUpButton.addActionListener(this);
        buttonsPanel.add(moveUpButton);

        org.openide.awt.Mnemonics.setLocalizedText(moveDownButton, MOVE_DOWN_BUTTON_TEXT);
        moveDownButton.getAccessibleContext().setAccessibleDescription(MOVE_DOWN_BUTTON_ACCESS_DESCR);
        moveDownButton.setEnabled(false);
        moveDownButton.addActionListener(this);
        buttonsPanel.add(moveDownButton);

        filterSetsPreviewPanel.add(buttonsPanel, java.awt.BorderLayout.EAST);

        add(filterSetsPreviewPanel, java.awt.BorderLayout.NORTH);

        final TitledBorder filterSetPropertiesPanelTitledBorder = BorderFactory.createTitledBorder(FILTERSET_PROPERTIES_BORDER_CAPTION);

        filterSetPropertiesPanel.setLayout(new java.awt.BorderLayout());
        filterSetPropertiesPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(2, 5, 5, 5),
                                                                              filterSetPropertiesPanelTitledBorder));

        // activeFiltersTable renderers
        final LabelTableCellRenderer filterTableCellRenderer = new LabelTableCellRenderer();
        final BooleanTableCellRenderer booleanTableCellRenderer = new BooleanTableCellRenderer();

        // activeFiltersTable
        activeFiltersTable = new JExtendedTable(new ActiveFiltersTableModel());
        activeFiltersTable.getAccessibleContext().setAccessibleName(ACTIVE_FILTERS_TABLE_ACCESS_NAME);
        activeFiltersTable.getAccessibleContext().setAccessibleDescription(ACTIVE_FILTERS_TABLE_ACCESS_DESCR);
        activeFiltersTable.setRowSelectionAllowed(true);
        activeFiltersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        activeFiltersTable.setGridColor(UIConstants.TABLE_VERTICAL_GRID_COLOR);
        activeFiltersTable.setSelectionBackground(UIConstants.TABLE_SELECTION_BACKGROUND_COLOR);
        activeFiltersTable.setSelectionForeground(UIConstants.TABLE_SELECTION_FOREGROUND_COLOR);
        activeFiltersTable.setShowHorizontalLines(UIConstants.SHOW_TABLE_HORIZONTAL_GRID);
        activeFiltersTable.setShowVerticalLines(UIConstants.SHOW_TABLE_VERTICAL_GRID);
        activeFiltersTable.setRowMargin(UIConstants.TABLE_ROW_MARGIN);
        activeFiltersTable.getTableHeader().setReorderingAllowed(false);
        activeFiltersTable.setDefaultRenderer(Boolean.class, booleanTableCellRenderer);
        activeFiltersTable.setDefaultRenderer(String.class, filterTableCellRenderer);
        activeFiltersTable.setRowHeight(UIUtils.getDefaultRowHeight() + 2);
        activeFiltersTable.addKeyListener(new ActiveFiltersTableKeyListener());
        activeFiltersTable.addMouseListener(new ActiveFiltersTableMouseListener());

        // Disable traversing table cells using TAB and Shift+TAB
        Set keys = new HashSet(activeFiltersTable.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS));
        keys.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0));
        activeFiltersTable.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, keys);

        keys = new HashSet(activeFiltersTable.getFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS));
        keys.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_MASK));
        activeFiltersTable.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, keys);

        // filterTable columns
        final TableColumn firstColumn = activeFiltersTable.getColumnModel().getColumn(0);
        final int firstColumnWidth = new JButton(COLUMN_NAME_ACTIVE).getPreferredSize().width;
        firstColumn.setMinWidth(firstColumnWidth);
        firstColumn.setPreferredWidth(firstColumnWidth);
        firstColumn.setMaxWidth(firstColumnWidth);

        // filterTableScrollPane
        activeFiltersScrollPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        activeFiltersScrollPane.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5),
                                                                             activeFiltersScrollPane.getBorder()));
        activeFiltersScrollPane.setPreferredSize(new Dimension(1, 200));

        final ActiveFiltersTableViewport activeFiltersTableViewport = new ActiveFiltersTableViewport(activeFiltersTable);
        activeFiltersScrollPane.setViewport(activeFiltersTableViewport);
        activeFiltersScrollPane.addMouseWheelListener(activeFiltersTable);
        activeFiltersScrollPane.getVerticalScrollBar().getModel().addChangeListener(new ChangeListener() {
                public void stateChanged(final ChangeEvent e) {
                    if (activeFiltersScrollPane.getVerticalScrollBar().getModel().getExtent() == activeFiltersScrollPane.getVerticalScrollBar()
                                                                                                                            .getModel()
                                                                                                                            .getMaximum()) {
                        activeFiltersScrollPane.getVerticalScrollBar().setEnabled(false);
                    } else {
                        activeFiltersScrollPane.getVerticalScrollBar().setEnabled(true);
                    }
                }
            });

        filterSetPropertiesPanel.add(activeFiltersScrollPane, java.awt.BorderLayout.CENTER);

        filterSetSettingsPanel.setLayout(new java.awt.GridBagLayout());

        org.openide.awt.Mnemonics.setLocalizedText(filterNameLabel, FILTERSET_NAME_LABEL_TEXT);
        filterNameLabel.setLabelFor(filterNameTextField);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 10);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        filterSetSettingsPanel.add(filterNameLabel, gridBagConstraints);

        filterNameTextField.setText(""); //NOI18N
        filterNameTextField.addKeyListener(new FilterNameTextFieldKeyListener());
        filterNameTextField.addFocusListener(new FilterNameTextFieldFocusListener());
        filterNameTextField.getDocument().addDocumentListener(new FilterNameTextFieldDocumentListener());
        filterNameTextField.setSelectionColor(UIConstants.TABLE_SELECTION_BACKGROUND_COLOR);
        filterNameTextField.setSelectedTextColor(UIConstants.TABLE_SELECTION_FOREGROUND_COLOR);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 4, 5, 0);
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        filterSetSettingsPanel.add(filterNameTextField, gridBagConstraints);

        filterTypeLabel.setText(FILTERSET_TYPE_LABEL_TEXT);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 5, 10);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        filterSetSettingsPanel.add(filterTypeLabel, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(filterTypeExclusiveRadio, FILTERSET_TYPE_EXCLUSIVE_RADIO_TEXT);
        filterTypeExclusiveRadio.getAccessibleContext().setAccessibleDescription(FILTER_TYPE_EXCLUSIVE_RADIO_ACCESS_DESCR);
        filterTypeExclusiveRadio.addActionListener(this);
        filterTypeButtonGroup.add(filterTypeExclusiveRadio);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        filterSetSettingsPanel.add(filterTypeExclusiveRadio, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(filterTypeInclusiveRadio, FILTERSET_TYPE_INCLUSIVE_RADIO_TEXT);
        filterTypeInclusiveRadio.getAccessibleContext().setAccessibleDescription(FILTER_TYPE_INCLUSIVE_RADIO_ACCESS_DESCR);
        filterTypeInclusiveRadio.addActionListener(this);
        filterTypeButtonGroup.add(filterTypeInclusiveRadio);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        filterSetSettingsPanel.add(filterTypeInclusiveRadio, gridBagConstraints);

        activeFiltersLabel.setLabelFor(activeFiltersTable);
        //    activeFiltersLabel.setFocusable(false);
        org.openide.awt.Mnemonics.setLocalizedText(activeFiltersLabel, ACTIVE_FILTERS_LABEL_TEXT);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 5);
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        filterSetSettingsPanel.add(activeFiltersLabel, gridBagConstraints);

        final JPanel expandPanel = new JPanel();
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 2.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        filterSetSettingsPanel.add(expandPanel, gridBagConstraints);

        filterSetPropertiesPanel.add(filterSetSettingsPanel, java.awt.BorderLayout.NORTH);
        filterSetPropertiesPanel.setVisible(false);

        add(filterSetPropertiesPanel, java.awt.BorderLayout.CENTER);

        Color panelBackground = UIManager.getColor("Panel.background"); //NOI18N
        Color hintBackground = UIUtils.getSafeColor(panelBackground.getRed() - 10, panelBackground.getGreen() - 10,
                                                    panelBackground.getBlue() - 10);
        // hintArea
        hintArea.setText(HINT_MSG); // NOI18N
        hintArea.setEnabled(false);
        hintArea.setDisabledTextColor(Color.darkGray);
        hintArea.setBackground(hintBackground);
        hintArea.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(7, 7, 10, 7,
                                                                                              UIManager.getColor("Panel.background")), //NOI18N
                                                              BorderFactory.createMatteBorder(10, 10, 10, 10, hintBackground)));
        add(hintArea, java.awt.BorderLayout.SOUTH);
    }

    private void makeDefinedFilterSetsListSelectionVisible() {
        definedFilterSetsList.scrollRectToVisible(definedFilterSetsList.getCellBounds(definedFilterSetsList.getSelectedIndex(),
                                                                                      definedFilterSetsList.getSelectedIndex()));
    }

    private void moveSelectedFilterDown() {
        if (definedFilterSetsList != null) {
            final int selectedIndex = definedFilterSetsList.getSelectedIndex();

            if (selectedIndex < (definedFilterSetsList.getModel().getSize() - 1)) {
                filterSets.moveFilterSetDown(selectedIndex);

                final DefinedFilterSetsListModel listModel = (DefinedFilterSetsListModel) definedFilterSetsList.getModel();
                listModel.fireContentsChanged(selectedIndex, selectedIndex + 1);
                definedFilterSetsList.setSelectedIndex(selectedIndex + 1);
                makeDefinedFilterSetsListSelectionVisible();
            }
        }
    }

    private void moveSelectedFilterUp() {
        if (definedFilterSetsList != null) {
            final int selectedIndex = definedFilterSetsList.getSelectedIndex();

            if (selectedIndex > 0) {
                filterSets.moveFilterSetUp(selectedIndex);

                final DefinedFilterSetsListModel listModel = (DefinedFilterSetsListModel) definedFilterSetsList.getModel();
                listModel.fireContentsChanged(selectedIndex - 1, selectedIndex);
                definedFilterSetsList.setSelectedIndex(selectedIndex - 1);
                makeDefinedFilterSetsListSelectionVisible();
            }
        }
    }

    private void processUniqueFilterSetNameCheckAt(final int index) {
        int selectedIndex = definedFilterSetsList.getSelectedIndex();
        if (selectedIndex == -1) return;

        if ((filterSets.getFilterSetAt(index).getFilterSetName().trim().length() == 0)
                || (containsFilterSetNameExceptIndex(filterSets.getFilterSetAt(index).getFilterSetName(), index))) {
            filterSets.getFilterSetAt(index)
                      .setFilterSetName(createUniqueFilterSetName(filterSets.getFilterSetAt(index).getFilterSetName()));
            ((DefinedFilterSetsListModel) definedFilterSetsList.getModel()).fireContentsChanged(index, index);
        }

        filterNameTextField.setText(filterSets.getFilterSetAt(selectedIndex).getFilterSetName());
    }

    private void removeSelectedFilterSet() {
        if (definedFilterSetsList != null) {
            final int selectedIndex = definedFilterSetsList.getSelectedIndex();

            if (selectedIndex != -1) {
                filterSets.removeFilterSet(selectedIndex);

                final DefinedFilterSetsListModel listModel = (DefinedFilterSetsListModel) definedFilterSetsList.getModel();
                listModel.fireIntervalRemoved(selectedIndex, selectedIndex);

                if (selectedIndex != 0) {
                    definedFilterSetsList.setSelectedIndex(selectedIndex - 1);
                } else if (listModel.getSize() > 0) {
                    definedFilterSetsList.setSelectedIndex(0);
                }
            }
        }
    }

    private void toggleSelectedRowCheck() {
        final int selectedIndex = activeFiltersTable.getSelectedRow();
        final boolean selected = !selectedFilterSetChecks[selectedIndex].booleanValue();
        selectedFilterSetChecks[selectedIndex] = Boolean.valueOf(selected);

        if (selected) {
            selectedFilterSet.addActiveGlobalFilter(nbProfiler.getGlobalFilters().getFilterNames()[selectedIndex]);
        } else {
            selectedFilterSet.removeActiveGlobalFilter(nbProfiler.getGlobalFilters().getFilterNames()[selectedIndex]);
        }

        ((AbstractTableModel) (activeFiltersTable.getModel())).fireTableDataChanged();
        activeFiltersTable.setRowSelectionInterval(selectedIndex, selectedIndex);
        activeFiltersTable.repaint();
    }

    private void updateSelectedFilterSetProperties() {
        if (selectedFilterSet == null) {
            filterSetPropertiesPanel.setVisible(false);
            selectedFilterSetChecks = new Boolean[0];
        } else {
            if (!filterSetPropertiesPanel.isVisible()) {
                filterSetPropertiesPanel.setVisible(true);
            }

            filterNameTextField.setText(selectedFilterSet.getFilterSetName());

            if (selectedFilterSet.isExclusive()) {
                filterTypeExclusiveRadio.setSelected(true);
            } else {
                filterTypeInclusiveRadio.setSelected(true);
            }

            selectedFilterSetChecks = new Boolean[nbProfiler.getGlobalFilters().getFilterNames().length];

            for (int i = 0; i < selectedFilterSetChecks.length; i++) {
                if (selectedFilterSet.containsActiveGlobalFilter(nbProfiler.getGlobalFilters().getFilterNames()[i])) {
                    selectedFilterSetChecks[i] = Boolean.TRUE;
                } else {
                    selectedFilterSetChecks[i] = Boolean.FALSE;
                }
            }
        }

        ((AbstractTableModel) (activeFiltersTable.getModel())).fireTableDataChanged();
        activeFiltersTable.repaint();
    }

    private void updateSelection() {
        selectedFilterSetIndex = definedFilterSetsList.getSelectedIndex();

        if (selectedFilterSetIndex == -1) {
            selectedFilterSet = null;
        } else {
            selectedFilterSet = filterSets.getFilterSetAt(selectedFilterSetIndex);
        }

        updateSelectedFilterSetProperties();
    }

    /*  public static void main(String args[]) {
       try {
         UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel"); //NOI18N
         //UIManager.setLookAndFeel("plaf.metal.MetalLookAndFeel"); //NOI18N
         //UIManager.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel"); //NOI18N
         //UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel"); //NOI18N
       } catch (Exception e){};
       JFrame frame = new JFrame("FilterSetsPanel Viewer"); //NOI18N
       FilterSetsPanel filterSetsPanel = new FilterSetsPanel();
       filterSetsPanel.setPreferredSize(new Dimension(600, 500));
       frame.getContentPane().add(filterSetsPanel);
       frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
       frame.pack();
       frame.show();
       }
     */
}
