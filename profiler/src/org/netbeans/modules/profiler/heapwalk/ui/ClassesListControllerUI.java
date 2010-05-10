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

package org.netbeans.modules.profiler.heapwalk.ui;

import org.netbeans.api.project.Project;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.lib.profiler.ui.UIConstants;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.FilterComponent;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import org.netbeans.lib.profiler.ui.components.JExtendedTable;
import org.netbeans.lib.profiler.ui.components.JTitledPanel;
import org.netbeans.lib.profiler.ui.components.table.ClassNameTableCellRenderer;
import org.netbeans.lib.profiler.ui.components.table.CustomBarCellRenderer;
import org.netbeans.lib.profiler.ui.components.table.ExtendedTableModel;
import org.netbeans.lib.profiler.ui.components.table.JExtendedTablePanel;
import org.netbeans.lib.profiler.ui.components.table.LabelBracketTableCellRenderer;
import org.netbeans.lib.profiler.ui.components.table.SortableTableModel;
import org.netbeans.modules.profiler.NetBeansProfiler;
import org.netbeans.modules.profiler.heapwalk.ClassesListController;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumnModel;
import org.netbeans.modules.profiler.heapwalk.model.BrowserUtils;
import org.netbeans.modules.profiler.utils.IDEUtils;
import org.openide.util.RequestProcessor;


/**
 *
 * @author Jiri Sedlacek
 */
public class ClassesListControllerUI extends JTitledPanel {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private class ClassesListTableKeyListener extends KeyAdapter {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void keyPressed(KeyEvent e) {
            if ((e.getKeyCode() == KeyEvent.VK_CONTEXT_MENU)
                    || ((e.getKeyCode() == KeyEvent.VK_F10) && (e.getModifiers() == InputEvent.SHIFT_MASK))) {
                int selectedRow = classesListTable.getSelectedRow();

                if (selectedRow != -1) {
                    Rectangle rowBounds = classesListTable.getCellRect(selectedRow, 0, true);
                    tablePopup.show(classesListTable, rowBounds.x + (rowBounds.width / 2), rowBounds.y + (rowBounds.height / 2));
                }
            }
        }
    }

    // --- Table model -----------------------------------------------------------
    private class ClassesListTableModel extends SortableTableModel {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        public Class getColumnClass(int columnIndex) {
            return Object.class;
        }

        public int getColumnCount() {
            return columnCount;
        }

        public String getColumnName(int columnIndex) {
            return columnNames[columnIndex];
        }

        public String getColumnToolTipText(int col) {
            return columnToolTips[col];
        }

        public boolean getInitialSorting(int column) {
            switch (column) {
                case 0:
                    return true;
                default:
                    return false;
            }
        }

        public int getRowCount() {
            return displayCache.length;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            return displayCache[rowIndex][columnIndex];
        }

        public void sortByColumn(int column, boolean order) {
            sortingColumn = column;
            sortingOrder = order;
            initData();
            repaint();
        }
    }

    // --- Listeners -------------------------------------------------------------
    private class ClassesListTableMouseListener extends MouseAdapter {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        private void updateSelection(int row) {
            classesListTable.requestFocusInWindow();
            if (row != -1) classesListTable.setRowSelectionInterval(row, row);
            else classesListTable.clearSelection();
        }

        public void mousePressed(final MouseEvent e) {
            final int row = classesListTable.rowAtPoint(e.getPoint());
            updateSelection(row);
            if (e.isPopupTrigger()) tablePopup.show(e.getComponent(), e.getX(), e.getY());
        }

        public void mouseReleased(MouseEvent e) {
            int row = classesListTable.rowAtPoint(e.getPoint());
            updateSelection(row);
            if (e.isPopupTrigger()) tablePopup.show(e.getComponent(), e.getX(), e.getY());
        }

        public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                int row = classesListTable.rowAtPoint(e.getPoint());
                if (row != -1) showInstancesForClass((JavaClass) displayCache[row][4]);
            }
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String VIEW_TITLE = NbBundle.getMessage(ClassesListControllerUI.class,
                                                                 "ClassesListControllerUI_ViewTitle"); // NOI18N
    private static final String NO_INSTANCES_MSG = NbBundle.getMessage(ClassesListControllerUI.class,
                                                                       "ClassesListControllerUI_NoInstancesMsg"); // NOI18N
    private static final String FILTER_STARTS_WITH = NbBundle.getMessage(ClassesListControllerUI.class,
                                                                         "ClassesListControllerUI_FilterStartsWith"); // NOI18N
    private static final String FILTER_CONTAINS = NbBundle.getMessage(ClassesListControllerUI.class,
                                                                      "ClassesListControllerUI_FilterContains"); // NOI18N
    private static final String FILTER_ENDS_WITH = NbBundle.getMessage(ClassesListControllerUI.class,
                                                                       "ClassesListControllerUI_FilterEndsWith"); // NOI18N
    private static final String FILTER_REGEXP = NbBundle.getMessage(ClassesListControllerUI.class,
                                                                    "ClassesListControllerUI_FilterRegexp"); // NOI18N
    private static final String FILTER_IMPLEMENTATION = NbBundle.getMessage(ClassesListControllerUI.class,
                                                                            "ClassesListControllerUI_FilterImplementation"); // NOI18N
    private static final String FILTER_SUBCLASS = NbBundle.getMessage(ClassesListControllerUI.class,
                                                                      "ClassesListControllerUI_FilterSubclass"); // NOI18N
    private static final String DEFAULT_FILTER_TEXT = NbBundle.getMessage(ClassesListControllerUI.class,
                                                                          "ClassesListControllerUI_DefaultFilterText"); // NOI18N
    private static final String SHOW_IN_INSTANCES_STRING = NbBundle.getMessage(ClassesListControllerUI.class,
                                                                               "ClassesListControllerUI_ShowInInstancesString"); // NOI18N
    private static final String SHOW_IMPLEMENTATIONS_STRING = NbBundle.getMessage(ClassesListControllerUI.class,
                                                                                  "ClassesListControllerUI_ShowImplementationsString"); // NOI18N
    private static final String SHOW_SUBCLASSES_STRING = NbBundle.getMessage(ClassesListControllerUI.class,
                                                                             "ClassesListControllerUI_ShowSubclassesString"); // NOI18N
    private static final String GO_TO_SOURCE_STRING = NbBundle.getMessage(ClassesListControllerUI.class,
                                                                          "ClassesListControllerUI_GoToSourceString"); // NOI18N
    private static final String SHOW_HIDE_COLUMNS_STRING = NbBundle.getMessage(ClassesListControllerUI.class,
                                                                               "ClassesListControllerUI_ShowHideColumnsString"); // NOI18N
    private static final String FILTER_CHECKBOX_TEXT = NbBundle.getMessage(ClassesListControllerUI.class,
                                                                           "ClassesListControllerUI_FilterCheckboxText"); // NOI18N
    private static final String CLASSNAME_COLUMN_TEXT = NbBundle.getMessage(ClassesListControllerUI.class,
                                                                            "ClassesListControllerUI_ClassNameColumnText"); // NOI18N
    private static final String CLASSNAME_COLUMN_DESCR = NbBundle.getMessage(ClassesListControllerUI.class,
                                                                             "ClassesListControllerUI_ClassNameColumnDescr"); // NOI18N
    private static final String INSTANCES_REL_COLUMN_TEXT = NbBundle.getMessage(ClassesListControllerUI.class,
                                                                                "ClassesListControllerUI_InstancesRelColumnText"); // NOI18N
    private static final String INSTANCES_REL_COLUMN_DESCR = NbBundle.getMessage(ClassesListControllerUI.class,
                                                                                 "ClassesListControllerUI_InstancesRelColumnDescr"); // NOI18N
    private static final String INSTANCES_COLUMN_TEXT = NbBundle.getMessage(ClassesListControllerUI.class,
                                                                            "ClassesListControllerUI_InstancesColumnText"); // NOI18N
    private static final String INSTANCES_COLUMN_DESCR = NbBundle.getMessage(ClassesListControllerUI.class,
                                                                             "ClassesListControllerUI_InstancesColumnDescr"); // NOI18N
    private static final String SIZE_COLUMN_TEXT = NbBundle.getMessage(ClassesListControllerUI.class,
                                                                       "ClassesListControllerUI_SizeColumnText"); // NOI18N
    private static final String SIZE_COLUMN_DESCR = NbBundle.getMessage(ClassesListControllerUI.class,
                                                                        "ClassesListControllerUI_SizeColumnDescr"); // NOI18N
    private static final String FITERING_PROGRESS_TEXT = NbBundle.getMessage(ClassesListControllerUI.class,
                                                                             "ClassesListControllerUI_FilteringProgressText"); // NOI18N
    private static final String RESULT_NOT_AVAILABLE_STRING = NbBundle.getMessage(ClassesListControllerUI.class,
                                                                                  "ClassesListControllerUI_ResultNotAvailableString"); // NOI18N
    private static final String CLASSES_TABLE_ACCESS_NAME = NbBundle.getMessage(ClassesListControllerUI.class,
                                                                             "ClassesListControllerUI_ClassesTableAccessName"); // NOI18N
    private static final String CLASSES_TABLE_ACCESS_DESCR = NbBundle.getMessage(ClassesListControllerUI.class,
                                                                                  "ClassesListControllerUI_ClassesTableAccessDescr"); // NOI18N
                                                                                                                                       // -----
    private static ImageIcon ICON_CLASSES = ImageUtilities.loadImageIcon("org/netbeans/modules/profiler/heapwalk/ui/resources/classes.png", false); // NOI18N
    private static String filterValue = ""; // NOI18N
    private static int filterType = CommonConstants.FILTER_CONTAINS;

    // --- UI definition ---------------------------------------------------------
    private static final String DATA = "Data"; // NOI18N
    private static final String NO_DATA = "No data"; // NOI18N
    private static final int columnCount = 4;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private CardLayout contents;
    private ClassesListController classesListController;
    private final NumberFormat percentFormat = NumberFormat.getPercentInstance();
    private ClassesListTableModel realClassesListTableModel = new ClassesListTableModel();
    private ExtendedTableModel classesListTableModel = new ExtendedTableModel(realClassesListTableModel);
    private FilterComponent filterComponent;
    private JExtendedTable classesListTable;
    private JPanel contentsPanel;
    private JPopupMenu cornerPopup;
    private JPopupMenu tablePopup;
    private String selectedRowContents;
    private String[] columnNames;
    private javax.swing.table.TableCellRenderer[] columnRenderers;
    private String[] columnToolTips;
    private int[] columnWidths;
    private Object[][] displayCache;
    private boolean hasProjectContext;
    private boolean internalCornerButtonClick = false; // flag for closing columns popup by pressing cornerButton

    // --- Selection utils -------------------------------------------------------
    private boolean selectionSaved = false;
    private boolean showZeroInstances = true;
    private boolean showZeroSize = true;
    private boolean sortingOrder = false;

    // --- Private implementation ------------------------------------------------
    private int classesCount = -1;
    private int selectedRow;
    private int sortingColumn = 1;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    // --- Constructors ----------------------------------------------------------
    public ClassesListControllerUI(ClassesListController classesListController) {
        super(VIEW_TITLE, ICON_CLASSES, true);

        this.classesListController = classesListController;
        hasProjectContext = classesListController.getClassesController().getHeapFragmentWalker().getHeapDumpProject() != null;

        initColumnsData();
        initData();
        initComponents();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setColumnVisibility(int column, boolean columnVisible) {
        boolean isColumnVisible = classesListTableModel.isRealColumnVisible(column);

        if (isColumnVisible == columnVisible) {
            return;
        }

        saveSelection();

        boolean sortResults = false;
        int currentSortingColumn = classesListTableModel.getSortingColumn();
        int realSortingColumn = classesListTableModel.getRealColumn(currentSortingColumn);

        // Current sorting column is going to be hidden
        if (isColumnVisible && (column == realSortingColumn)) {
            // Try to set next column as a currentSortingColumn. If currentSortingColumn is the last column,
            // set previous column as a sorting Column (one column is always visible).
            currentSortingColumn = ((currentSortingColumn + 1) == classesListTableModel.getColumnCount())
                                   ? (currentSortingColumn - 1) : (currentSortingColumn + 1);
            realSortingColumn = classesListTableModel.getRealColumn(currentSortingColumn);
            sortResults = true;
        }

        classesListTableModel.setRealColumnVisibility(column, columnVisible);
        classesListTable.createDefaultColumnsFromModel();
        classesListTableModel.setTable(classesListTable); // required to restore table header renderer
        currentSortingColumn = classesListTableModel.getVirtualColumn(realSortingColumn);

        if (sortResults) {
            sortingOrder = classesListTableModel.getInitialSorting(currentSortingColumn);
            sortingColumn = realSortingColumn;
            initData();
        }

        sortingColumn = realSortingColumn;
        classesListTableModel.setInitialSorting(currentSortingColumn, sortingOrder);
        classesListTable.getTableHeader().repaint();
        setColumnsData();
        restoreSelection();

        // TODO [ui-persistence]
    }

    public void ensureWillBeVisible(JavaClass javaClass) {
        // TODO: add showZeroSize and showZeroInstances checking
        if (ClassesListController.matchesFilter(javaClass, FilterComponent.getFilterStrings(filterValue), filterType,
                                                    showZeroInstances, showZeroSize)) {
            return;
        }

        //    if (ClassesListController.matchesFilter(javaClass, FilterComponent.getFilterStrings(filterValue + " " + javaClass.getName()), filterType, showZeroInstances, showZeroSize)) { // NOI18N
        //      filterComponent.setFilterString(filterValue + " " + javaClass.getName()); // NOI18N
        //      return;
        //    }
        filterComponent.setFilterString(""); // NOI18N
    }

    // --- Public interface ------------------------------------------------------
    public void selectClass(JavaClass javaClass) {
        //    if (isShowing()) {
        if ((displayCache == null) || (displayCache.length == 0)) {
            return;
        }

        for (int i = 0; i < displayCache.length; i++) {
            if (displayCache[i][4].equals(javaClass)) {
                classesListTable.setRowSelectionInterval(i, i);

                final int rowIndex = i;
                SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            classesListTable.ensureRowVisible(rowIndex);
                        }
                    });

                break;
            }
        }

        //      needsSelectInstance = false;
        //    } else {
        //      needsSelectFirstInstance = false;
        //      instanceToSelect = instance;
        //      needsSelectInstance = true;
        //    }
    }

    public void updateData() {
        // TODO: should be performed lazily, not from AWT!
        initData();
    }

    protected void initColumnSelectorItems() {
        cornerPopup.removeAll();

        JCheckBoxMenuItem menuItem;

        for (int i = 0; i < realClassesListTableModel.getColumnCount(); i++) {
            menuItem = new JCheckBoxMenuItem(realClassesListTableModel.getColumnName(i));
            menuItem.setActionCommand(new Integer(i).toString());
            addMenuItemListener(menuItem);

            if (classesListTable != null) {
                menuItem.setState(classesListTableModel.isRealColumnVisible(i));

                if (i == 0) {
                    menuItem.setEnabled(false);
                }
            } else {
                menuItem.setState(true);
            }

            cornerPopup.add(menuItem);
        }

        cornerPopup.addSeparator();

        JCheckBoxMenuItem filterMenuItem = new JCheckBoxMenuItem(FILTER_CHECKBOX_TEXT);
        filterMenuItem.setActionCommand("Filter"); // NOI18N
        addMenuItemListener(filterMenuItem);

        if (filterComponent == null) {
            filterMenuItem.setState(true);
        } else {
            filterMenuItem.setState(filterComponent.isVisible());
        }

        cornerPopup.add(filterMenuItem);

        cornerPopup.pack();
    }

    protected void saveColumnsData() {
        TableColumnModel colModel = classesListTable.getColumnModel();

        for (int i = 0; i < classesListTableModel.getColumnCount(); i++) {
            int index = classesListTableModel.getRealColumn(i);

            if (index != 0) {
                columnWidths[index - 1] = colModel.getColumn(i).getPreferredWidth();
            }
        }
    }

    private void setColumnsData() {
        TableColumnModel colModel = classesListTable.getColumnModel();

        for (int i = 0; i < classesListTableModel.getColumnCount(); i++) {
            int index = classesListTableModel.getRealColumn(i);

            if (index != 0) {
                colModel.getColumn(i).setPreferredWidth(columnWidths[index - 1]);
            }

            colModel.getColumn(i).setCellRenderer(columnRenderers[index]);
        }
    }

    private void addMenuItemListener(JCheckBoxMenuItem menuItem) {
        menuItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    if (e.getActionCommand().equals("Filter")) { // NOI18N
                        filterComponent.setVisible(!filterComponent.isVisible());

                        return;
                    }

                    int column = Integer.parseInt(e.getActionCommand());
                    setColumnVisibility(column, !classesListTableModel.isRealColumnVisible(column));
                }
            });
    }

    private JButton createHeaderPopupCornerButton(final JPopupMenu headerPopup) {
        final JButton cornerButton = new JButton(ImageUtilities.loadImageIcon("org/netbeans/lib/profiler/ui/resources/hideColumn.png", false)); // NOI18N
        cornerButton.setToolTipText(SHOW_HIDE_COLUMNS_STRING);
        cornerButton.setDefaultCapable(false);

        if (UIUtils.isWindowsClassicLookAndFeel()) {
            cornerButton.setMargin(new Insets(0, 0, 2, 2));
        } else if (UIUtils.isWindowsXPLookAndFeel()) {
            cornerButton.setMargin(new Insets(0, 0, 0, 1));
        } else if (UIUtils.isMetalLookAndFeel()) {
            cornerButton.setMargin(new Insets(0, 0, 2, 1));
        }

        cornerButton.addKeyListener(new KeyAdapter() {
                public void keyPressed(final KeyEvent evt) {
                    if (evt.getKeyCode() == KeyEvent.VK_SPACE) {
                        showColumnSelectionPopup(headerPopup, cornerButton);
                    }
                }
            });

        cornerButton.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent mouseEvent) {
                    if (headerPopup.isVisible()) {
                        internalCornerButtonClick = true;
                        cornerButton.getModel().setArmed(false);
                    } else {
                        internalCornerButtonClick = false;

                        if (mouseEvent.getModifiers() == InputEvent.BUTTON3_MASK) {
                            showColumnSelectionPopup(headerPopup, cornerButton);
                        }
                    }
                }

                public void mouseClicked(MouseEvent mouseEvent) {
                    if ((mouseEvent.getModifiers() == InputEvent.BUTTON1_MASK) && (!internalCornerButtonClick)) {
                        showColumnSelectionPopup(headerPopup, cornerButton);
                    }
                }
            });

        return cornerButton;
    }

    private JPopupMenu createTablePopup() {
        JPopupMenu popup = new JPopupMenu();

        JMenuItem showInstancesItem = new JMenuItem(SHOW_IN_INSTANCES_STRING);
        showInstancesItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    performDefaultAction();
                }
            });
        showInstancesItem.setFont(popup.getFont().deriveFont(Font.BOLD));

        JMenuItem showInstancesOfItem = new JMenuItem(hasProjectContext ? SHOW_IMPLEMENTATIONS_STRING : SHOW_SUBCLASSES_STRING);
        showInstancesOfItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    int row = classesListTable.getSelectedRow();

                    if (row != -1) {
                        showSubclassesForClass((JavaClass) displayCache[row][4]);
                    }
                }
            });

        JMenuItem showSourceItem = new JMenuItem(GO_TO_SOURCE_STRING);
        showSourceItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    int row = classesListTable.getSelectedRow();

                    if (row != -1) {
                        String className = (String) displayCache[row][0];

                        while (className.endsWith("[]")) {
                            className = className.substring(0, className.length() - 2); // NOI18N
                        }
                        Project p = classesListController.getClassesController().getHeapFragmentWalker().getHeapDumpProject();
                        NetBeansProfiler.getDefaultNB().openJavaSource(p, className, null, null);
                    }
                }
            });

        popup.add(showInstancesItem);
        popup.add(showInstancesOfItem);
        popup.addSeparator();
        popup.add(showSourceItem);

        return popup;
    }

    private void initColumnsData() {
        columnWidths = new int[columnCount - 1]; // Width of the first column fits to width
        columnNames = new String[columnCount];
        columnToolTips = new String[columnCount];
        columnRenderers = new javax.swing.table.TableCellRenderer[columnCount];

        columnNames[0] = CLASSNAME_COLUMN_TEXT;
        columnToolTips[0] = CLASSNAME_COLUMN_DESCR;

        columnNames[1] = INSTANCES_REL_COLUMN_TEXT;
        columnToolTips[1] = INSTANCES_REL_COLUMN_DESCR;

        columnNames[2] = INSTANCES_COLUMN_TEXT;
        columnToolTips[2] = INSTANCES_COLUMN_DESCR;

        columnNames[3] = SIZE_COLUMN_TEXT;
        columnToolTips[3] = SIZE_COLUMN_DESCR;

        int maxWidth = getFontMetrics(getFont()).charWidth('W') * 12; // NOI18N // initial width of data columns

        ClassNameTableCellRenderer classNameCellRenderer = new ClassNameTableCellRenderer();
        CustomBarCellRenderer customBarCellRenderer = new CustomBarCellRenderer(0, 100);
        LabelBracketTableCellRenderer dataCellRenderer = new LabelBracketTableCellRenderer(JLabel.TRAILING);

        // method / class / package name
        columnRenderers[0] = classNameCellRenderer;

        columnWidths[1 - 1] = maxWidth;
        columnRenderers[1] = customBarCellRenderer;

        columnWidths[2 - 1] = maxWidth;
        columnRenderers[2] = dataCellRenderer;

        columnWidths[3 - 1] = maxWidth;
        columnRenderers[3] = dataCellRenderer;
    }

    private void initComponents() {
        percentFormat.setMaximumFractionDigits(1);
        percentFormat.setMinimumFractionDigits(0);

        classesListTable = new JExtendedTable(classesListTableModel) {
                public void doLayout() {
                    int columnsWidthsSum = 0;
                    int realFirstColumn = -1;

                    TableColumnModel colModel = getColumnModel();

                    for (int i = 0; i < classesListTableModel.getColumnCount(); i++) {
                        if (classesListTableModel.getRealColumn(i) == 0) {
                            realFirstColumn = i;
                        } else {
                            columnsWidthsSum += colModel.getColumn(i).getPreferredWidth();
                        }
                    }

                    if (realFirstColumn != -1) {
                        colModel.getColumn(realFirstColumn).setPreferredWidth(getWidth() - columnsWidthsSum);
                    }

                    super.doLayout();
                }
                ;
            };
        classesListTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        classesListTable.addMouseListener(new ClassesListTableMouseListener());
        classesListTable.addKeyListener(new ClassesListTableKeyListener());
        classesListTable.setGridColor(UIConstants.TABLE_VERTICAL_GRID_COLOR);
        classesListTable.setSelectionBackground(UIConstants.TABLE_SELECTION_BACKGROUND_COLOR);
        classesListTable.setSelectionForeground(UIConstants.TABLE_SELECTION_FOREGROUND_COLOR);
        classesListTable.setShowHorizontalLines(UIConstants.SHOW_TABLE_HORIZONTAL_GRID);
        classesListTable.setShowVerticalLines(UIConstants.SHOW_TABLE_VERTICAL_GRID);
        classesListTable.setRowMargin(UIConstants.TABLE_ROW_MARGIN);
        classesListTable.setRowHeight(UIUtils.getDefaultRowHeight() + 2);
        classesListTableModel.setTable(classesListTable);
        classesListTableModel.setInitialSorting(sortingColumn, sortingOrder);
        classesListTable.getColumnModel().getColumn(0).setMinWidth(150);
        classesListTable.getAccessibleContext().setAccessibleName(CLASSES_TABLE_ACCESS_NAME);
        classesListTable.getAccessibleContext().setAccessibleDescription(CLASSES_TABLE_ACCESS_DESCR);
        classesListTable.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                        .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "DEFAULT_ACTION"); // NOI18N
        classesListTable.getActionMap().put("DEFAULT_ACTION",
                                            new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    performDefaultAction();
                }
            }); // NOI18N

        // Disable traversing table cells using TAB and Shift+TAB
        Set keys = new HashSet(classesListTable.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS));
        keys.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0));
        classesListTable.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, keys);

        keys = new HashSet(classesListTable.getFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS));
        keys.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_MASK));
        classesListTable.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, keys);

        setColumnsData();

        filterComponent = new FilterComponent();
        filterComponent.addFilterItem(new ImageIcon(filterComponent.getClass()
                                                                   .getResource("/org/netbeans/lib/profiler/ui/resources/filterStartsWith.png") // NOI18N
        ), FILTER_STARTS_WITH, CommonConstants.FILTER_STARTS_WITH);
        filterComponent.addFilterItem(new ImageIcon(filterComponent.getClass()
                                                                   .getResource("/org/netbeans/lib/profiler/ui/resources/filterContains.png") // NOI18N
        ), FILTER_CONTAINS, CommonConstants.FILTER_CONTAINS);
        filterComponent.addFilterItem(new ImageIcon(filterComponent.getClass()
                                                                   .getResource("/org/netbeans/lib/profiler/ui/resources/filterEndsWith.png") // NOI18N
        ), FILTER_ENDS_WITH, CommonConstants.FILTER_ENDS_WITH);
        filterComponent.addFilterItem(new ImageIcon(filterComponent.getClass()
                                                                   .getResource("/org/netbeans/lib/profiler/ui/resources/filterRegExp.png") // NOI18N
        ), FILTER_REGEXP, CommonConstants.FILTER_REGEXP);
        filterComponent.addFilterItem(org.netbeans.modules.profiler.ui.Utils.CLASS_ICON,
                                      hasProjectContext ? FILTER_IMPLEMENTATION : FILTER_SUBCLASS,
                                      classesListController.FILTER_SUBCLASS);
        filterComponent.setEmptyFilterText(DEFAULT_FILTER_TEXT);
        filterComponent.setFilterValues(filterValue, filterType);
        filterComponent.addFilterListener(new FilterComponent.FilterListener() {
                public void filterChanged() {
                    filterValue = filterComponent.getFilterString();
                    filterType = filterComponent.getFilterType();
                    initData();
                }
            });

        tablePopup = createTablePopup();

        cornerPopup = new JPopupMenu();

        JExtendedTablePanel tablePanel = new JExtendedTablePanel(classesListTable);
        tablePanel.setCorner(JScrollPane.UPPER_RIGHT_CORNER, createHeaderPopupCornerButton(cornerPopup));

        setLayout(new BorderLayout());

        JPanel noDataPanel = new JPanel(new BorderLayout());
        noDataPanel.setBorder(BorderFactory.createLoweredBevelBorder());

        HTMLTextArea hintArea = new HTMLTextArea();
        hintArea.setBorder(BorderFactory.createEmptyBorder(10, 8, 8, 8));

        String hintText = "<img border='0' align='bottom' src='nbresloc:/org/netbeans/modules/profiler/heapwalk/ui/resources/progress.png'>&nbsp;&nbsp;"
                          + FITERING_PROGRESS_TEXT; // NOI18N
        hintArea.setText(hintText);
        noDataPanel.add(hintArea, BorderLayout.CENTER);

        contents = new CardLayout();
        contentsPanel = new JPanel(contents);
        contentsPanel.add(tablePanel, DATA);
        contentsPanel.add(noDataPanel, NO_DATA);
        contents.show(contentsPanel, NO_DATA);

        add(contentsPanel, BorderLayout.CENTER);
        add(filterComponent, BorderLayout.SOUTH);

        classesListTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    classesListController.classSelected((classesListTable.getSelectedRow() == -1) ? null
                                                                                                  : (JavaClass) displayCache[classesListTable
                                                                                                                             .getSelectedRow()][4]);
                }
            });
    }

    private void initData() {
        if (displayCache == null) displayCache = new Object[0][columnCount + 1];

        IDEUtils.runInEventDispatchThread(new Runnable() {
            public void run() {
                final AtomicBoolean initInProgress = new AtomicBoolean(false);
                
                RequestProcessor.getDefault().post(new Runnable() {
                    public void run() {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                if (contents != null && initInProgress.get())
                                    contents.show(contentsPanel, NO_DATA);
                            }
                        });
                    }
                }, 100);

                saveSelection();

                BrowserUtils.performTask(new Runnable() {
                    public void run() {
                        initInProgress.set(true);

                        long totalLiveInstances = classesListController.getClassesController().
                                getHeapFragmentWalker().getTotalLiveInstances();
                        long totalLiveBytes = classesListController.getClassesController().
                                getHeapFragmentWalker().getTotalLiveBytes();

                        if (classesCount == -1) classesCount = classesListController.
                                getClassesController().getHeapFragmentWalker().
                                getHeapFragment().getAllClasses().size();

                        List classes = classesListController.getFilteredSortedClasses(
                                FilterComponent.getFilterStrings(filterValue), filterType,
                                showZeroInstances, showZeroSize, sortingColumn, sortingOrder);
                        final Object[][] displayCache2 = new Object[classes.size()][columnCount + 1];

                        for (int i = 0; i < classes.size(); i++) {
                            JavaClass jClass = (JavaClass) classes.get(i);

                            int instancesCount = jClass.getInstancesCount();
                            int instanceSize = jClass.getInstanceSize();
                            long allInstancesSize = jClass.getAllInstancesSize();

                            displayCache2[i][0] = jClass.getName();
                            displayCache2[i][1] = new Double((double) instancesCount /
                                                 (double) totalLiveInstances * 100);
                            displayCache2[i][2] = Integer.toString(instancesCount) + " (" // NOI18N
                                                 + percentFormat.format((double) instancesCount /
                                                 (double) totalLiveInstances) + ")"; // NOI18N
                            displayCache2[i][3] = (allInstancesSize < 0) ? RESULT_NOT_AVAILABLE_STRING
                                                  : (Long.toString(allInstancesSize) + " (" // NOI18N
                                                  + percentFormat.format((double) allInstancesSize /
                                                  (double) totalLiveBytes) + ")"); // NOI18N
                            displayCache2[i][4] = jClass;
                        }

                        initInProgress.set(false);

                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                displayCache = displayCache2;
                                classesListTableModel.fireTableDataChanged();
                                restoreSelection();
                                if (contents != null) contents.show(contentsPanel, DATA);
                            }
                        });

                    }
                });

            }
        });
    }

    private void performDefaultAction() {
        int row = classesListTable.getSelectedRow();

        if (row != -1) {
            showInstancesForClass((JavaClass) displayCache[row][4]);
        }
    }

    private void restoreSelection() {
        if (selectedRowContents != null) {
            classesListTable.selectRowByContents(selectedRowContents, 0, true);
        }

        selectionSaved = false;
    }

    private void saveSelection() {
        if (selectionSaved) {
            return;
        }

        selectedRow = (classesListTable == null) ? (-1) : classesListTable.getSelectedRow();
        selectedRowContents = null;

        if (selectedRow != -1) {
            selectedRowContents = (String) classesListTable.getValueAt(selectedRow, 0);
        }

        selectionSaved = true;
    }

    private void showColumnSelectionPopup(final JPopupMenu headerPopup, final JButton cornerButton) {
        initColumnSelectorItems();
        headerPopup.show(cornerButton, cornerButton.getWidth() - headerPopup.getPreferredSize().width, cornerButton.getHeight());
    }

    private void showInstancesForClass(JavaClass jClass) {
        if (jClass.getInstancesCount() == 0) {
            NetBeansProfiler.getDefaultNB().displayInfo(MessageFormat.format(NO_INSTANCES_MSG, new Object[] { jClass.getName() }));
        } else {
            classesListController.getClassesController().getHeapFragmentWalker().showInstancesForClass(jClass);
        }
    }

    private void showSubclassesForClass(JavaClass jClass) {
        filterComponent.setFilterType(ClassesListController.FILTER_SUBCLASS);
        filterComponent.setFilterString(jClass.getName()); // fires change in filterComponent
    }
}
