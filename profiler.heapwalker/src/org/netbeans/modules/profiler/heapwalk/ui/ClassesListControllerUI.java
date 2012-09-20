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

package org.netbeans.modules.profiler.heapwalk.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
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
import org.netbeans.modules.profiler.heapwalk.ClassesListController;
import org.netbeans.modules.profiler.heapwalk.HeapFragmentWalker;
import org.openide.util.NbBundle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import org.netbeans.lib.profiler.common.CommonUtils;
import org.netbeans.lib.profiler.ui.components.HTMLLabel;
import org.netbeans.lib.profiler.ui.components.table.DiffBarCellRenderer;
import org.netbeans.lib.profiler.ui.components.table.LabelTableCellRenderer;
import org.netbeans.modules.profiler.api.icons.GeneralIcons;
import org.netbeans.modules.profiler.api.GoToSource;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.ProfilerDialogs;
import org.netbeans.modules.profiler.heapwalk.model.BrowserUtils;
import org.netbeans.modules.profiler.heapwalk.ui.icons.HeapWalkerIcons;
import org.openide.util.Lookup;


/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "ClassesListControllerUI_ViewTitle=Classes",
    "ClassesListControllerUI_NoInstancesMsg=Class {0} has no instances.",
    "ClassesListControllerUI_NoClassInBaseMsg=Class is not present in base heap dump.",
    "ClassesListControllerUI_FilterStartsWith=Starts with",
    "ClassesListControllerUI_FilterContains=Contains",
    "ClassesListControllerUI_FilterEndsWith=Ends with",
    "ClassesListControllerUI_FilterRegexp=Regular expression",
    "ClassesListControllerUI_FilterImplementation=Subclass/Implementation of",
    "ClassesListControllerUI_FilterSubclass=Subclass of",
    "ClassesListControllerUI_DefaultFilterText=Class Name Filter",
    "ClassesListControllerUI_ShowInInstancesString=Show in Instances View",
    "ClassesListControllerUI_ShowImplementationsString=Show Only Subclasses or Implementations",
    "ClassesListControllerUI_ShowSubclassesString=Show Only Subclasses",
    "ClassesListControllerUI_GoToSourceString=Go To Source",
    "ClassesListControllerUI_ShowHideColumnsString=Show or hide columns",
    "ClassesListControllerUI_FilterCheckboxText=Filter",
    "ClassesListControllerUI_ClassNameColumnText=Class Name",
    "ClassesListControllerUI_ClassNameColumnDescr=Class name",
    "ClassesListControllerUI_InstancesRelColumnText=Instances [%]",
    "ClassesListControllerUI_InstancesRelColumnDescr=Relative number of instances",
    "ClassesListControllerUI_InstancesColumnText=Instances",
    "ClassesListControllerUI_InstancesColumnDescr=Number of instances",
    "ClassesListControllerUI_SizeColumnText=Size",
    "ClassesListControllerUI_SizeColumnDescr=Size of all instances",
    "ClassesListControllerUI_RetainedSizeColumnName=Retained",
    "ClassesListControllerUI_RetainedSizeColumnDescr=Retained size of all instances of particular class",
    "ClassesListControllerUI_FilteringProgressText=Processing classes, wait please...",
    "ClassesListControllerUI_ClassesTableAccessName=Classes",
    "ClassesListControllerUI_ClassesTableAccessDescr=List of classes allocated on the heap",
    "ClassesListControllerUI_CompareWithAnotherText=Compare with another heap dump",
    "ClassesListControllerUI_ComparingMsg=Comparing heap dumps...",
    "ClassesListControllerUI_ShowingDiffText=Showing heap dumps difference, {0}reset view{1}"
})
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
                    showPopupMenu(selectedRow, rowBounds.x + (rowBounds.width / 2), rowBounds.y + (rowBounds.height / 2));
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
            if (e.isPopupTrigger()) showPopupMenu(row, e.getX(), e.getY());
        }

        public void mouseReleased(MouseEvent e) {
            int row = classesListTable.rowAtPoint(e.getPoint());
            updateSelection(row);
            if (e.isPopupTrigger()) showPopupMenu(row, e.getX(), e.getY());
        }

        public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                int row = classesListTable.rowAtPoint(e.getPoint());
                if (row != -1) showInstancesForClass((JavaClass) displayCache[row][columnCount]);
            }
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------
                                                                                                                                       // -----
    private static Icon ICON_CLASSES = Icons.getIcon(HeapWalkerIcons.CLASSES);
    // --- UI definition ---------------------------------------------------------
    private static final String DATA = "Data"; // NOI18N
    private static final String NO_DATA = "No data"; // NOI18N

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private String filterValue = ""; // NOI18N
    private int filterType = CommonConstants.FILTER_CONTAINS;
    private CardLayout contents;
    private ClassesListController classesListController;
    private ClassesListTableModel realClassesListTableModel;
    private ExtendedTableModel classesListTableModel;
    private FilterComponent filterComponent;
    private JExtendedTable classesListTable;
    private JMenuItem showSourceItem;
    private JPanel contentsPanel;
    private JPopupMenu cornerPopup;
    private JPopupMenu tablePopup;
    private String selectedRowContents;
    private final int columnCount;
    private String[] columnNames;
    private javax.swing.table.TableCellRenderer[] columnRenderers;
    private String[] columnToolTips;
    private int[] columnWidths;
    private Object[][] displayCache;
    private boolean hasProjectContext;
    private boolean retainedSizeSupported;
    private boolean internalCornerButtonClick = false; // flag for closing columns popup by pressing cornerButton

    // --- Selection utils -------------------------------------------------------
    private boolean selectionSaved = false;
    private boolean showZeroInstances = true;
    private boolean showZeroSize = true;
    private boolean sortingOrder = false;

    // --- Private implementation ------------------------------------------------
    private int selectedRow;
    private int sortingColumn = 1;
    private boolean isDiff = false;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    // --- Constructors ----------------------------------------------------------
    public ClassesListControllerUI(ClassesListController classesListController) {
        super(Bundle.ClassesListControllerUI_ViewTitle(), ICON_CLASSES, true);

        this.classesListController = classesListController;
        HeapFragmentWalker heap = classesListController.getClassesController().getHeapFragmentWalker();
        hasProjectContext = heap.getHeapDumpProject() != null;
        retainedSizeSupported = heap.getRetainedSizesStatus() != HeapFragmentWalker.RETAINED_SIZES_UNSUPPORTED;
        columnCount = retainedSizeSupported ? 5 : 4;
        realClassesListTableModel = new ClassesListTableModel();
        classesListTableModel = new ExtendedTableModel(realClassesListTableModel);

        initColumnsData();
        initData();
        initComponents();
        heap.addStateListener(
            new HeapFragmentWalker.StateListener() {
                public void stateChanged(HeapFragmentWalker.StateEvent e) {
                    if (e.getRetainedSizesStatus() == HeapFragmentWalker.RETAINED_SIZES_COMPUTED && e.isMasterChange()) {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                initData();
                                setColumnVisibility(4, true);
                            }
                        });
                    }
                }
            }
        );
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
        setColumnsData(true);
        restoreSelection();

        // TODO [ui-persistence]
    }

    public void selectClass(JavaClass javaClass) {
        if (ClassesListController.matchesFilter(javaClass, FilterComponent.getFilterValues(filterValue), filterType,
                                                    showZeroInstances, showZeroSize)) {
            selectClassImpl(javaClass);
        } else {
            filterComponent.setFilterValue(""); // NOI18N
            filterValue = filterComponent.getFilterValue();
            initDataImpl(javaClass);
        }
    }
    
    private void selectClassImpl(JavaClass javaClass) {
        //    if (isShowing()) {
        if ((displayCache == null) || (displayCache.length == 0)) {
            return;
        }

        for (int i = 0; i < displayCache.length; i++) {
            if (displayCache[i][columnCount].equals(javaClass)) {
                classesListTable.setRowSelectionInterval(i, i);

                final int rowIndex = i;
                SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            classesListTable.ensureRowVisible(rowIndex);
                        }
                    });

                break;
            }
        }
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
            menuItem.setActionCommand(Integer.valueOf(i).toString());
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

        JCheckBoxMenuItem filterMenuItem = new JCheckBoxMenuItem(Bundle.ClassesListControllerUI_FilterCheckboxText());
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

    protected void saveColumnsData() {
        TableColumnModel colModel = classesListTable.getColumnModel();

        for (int i = 0; i < classesListTableModel.getColumnCount(); i++) {
            int index = classesListTableModel.getRealColumn(i);

            if (index != 0) {
                columnWidths[index - 1] = colModel.getColumn(i).getPreferredWidth();
            }
        }
    }

    private void setColumnsData(boolean widths) {
        TableColumnModel colModel = classesListTable.getColumnModel();

        for (int i = 0; i < classesListTableModel.getColumnCount(); i++) {
            int index = classesListTableModel.getRealColumn(i);

            if (widths && index != 0) {
                colModel.getColumn(i).setPreferredWidth(columnWidths[index - 1]);
            }

            colModel.getColumn(i).setCellRenderer(columnRenderers[index]);
        }
    }

    private void addMenuItemListener(final JCheckBoxMenuItem menuItem) {
        final boolean[] internalChange = new boolean[1];
        menuItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    if (e.getActionCommand().equals("Filter")) { // NOI18N
                        filterComponent.getComponent().setVisible(!filterComponent.getComponent().isVisible());

                        return;
                    }
                    if (internalChange[0]) return;
                    final int column = Integer.parseInt(e.getActionCommand());
                    if (column == 4 && !classesListTableModel.isRealColumnVisible(column)) {
                        BrowserUtils.performTask(new Runnable() {
                            public void run() {
                                final int retainedSizesState = classesListController.getClassesController().
                                        getHeapFragmentWalker().computeRetainedSizes(true);
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run() {
                                        if (retainedSizesState != HeapFragmentWalker.RETAINED_SIZES_COMPUTED) {
                                            internalChange[0] = true;
                                            menuItem.setSelected(!menuItem.isSelected());
                                            internalChange[0] = false;
                                        } else {
                                            initData();
                                            setColumnVisibility(4,true);
                                        }
                                    }
                                });
                            }
                        });
                    } else {
                        setColumnVisibility(column, !classesListTableModel.isRealColumnVisible(column));
                    }
                }
            });
    }

    private JButton createHeaderPopupCornerButton(final JPopupMenu headerPopup) {
        final JButton cornerButton = new JButton(Icons.getIcon(GeneralIcons.HIDE_COLUMN));
        cornerButton.setToolTipText(Bundle.ClassesListControllerUI_ShowHideColumnsString());
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

        JMenuItem showInstancesItem = new JMenuItem(Bundle.ClassesListControllerUI_ShowInInstancesString());
        showInstancesItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    performDefaultAction();
                }
            });
        showInstancesItem.setFont(popup.getFont().deriveFont(Font.BOLD));

        JMenuItem showInstancesOfItem = new JMenuItem(hasProjectContext ? 
                                            Bundle.ClassesListControllerUI_ShowImplementationsString() : 
                                            Bundle.ClassesListControllerUI_ShowSubclassesString());
        showInstancesOfItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    int row = classesListTable.getSelectedRow();

                    if (row != -1) {
                        showSubclassesForClass((JavaClass) displayCache[row][columnCount]);
                    }
                }
            });

        if (GoToSource.isAvailable()) {
            showSourceItem = new JMenuItem(Bundle.ClassesListControllerUI_GoToSourceString());
            showSourceItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        int row = classesListTable.getSelectedRow();

                        if (row != -1) {
                            String className = BrowserUtils.getArrayBaseType((String)displayCache[row][0]);
                            Lookup.Provider p = classesListController.getClassesController().getHeapFragmentWalker().getHeapDumpProject();
                            GoToSource.openSource(p, className, null, null);
                        }
                    }
                });
        }

        popup.add(showInstancesItem);
        popup.add(showInstancesOfItem);
        if (showSourceItem != null) {
            popup.addSeparator();
            popup.add(showSourceItem);
        }

        return popup;
    }

    private void initColumnsData() {
        columnWidths = new int[columnCount - 1]; // Width of the first column fits to width
        columnNames = new String[columnCount];
        columnToolTips = new String[columnCount];
        columnRenderers = new javax.swing.table.TableCellRenderer[columnCount];

        columnNames[0] = Bundle.ClassesListControllerUI_ClassNameColumnText();
        columnToolTips[0] = Bundle.ClassesListControllerUI_ClassNameColumnDescr();

        columnNames[1] = Bundle.ClassesListControllerUI_InstancesRelColumnText();
        columnToolTips[1] = Bundle.ClassesListControllerUI_InstancesRelColumnDescr();

        columnNames[2] = Bundle.ClassesListControllerUI_InstancesColumnText();
        columnToolTips[2] = Bundle.ClassesListControllerUI_InstancesColumnDescr();

        columnNames[3] = Bundle.ClassesListControllerUI_SizeColumnText();
        columnToolTips[3] = Bundle.ClassesListControllerUI_SizeColumnDescr();

        if (retainedSizeSupported) {
            columnNames[4] = Bundle.ClassesListControllerUI_RetainedSizeColumnName();
            columnToolTips[4] = Bundle.ClassesListControllerUI_RetainedSizeColumnDescr();
        }

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

        if (retainedSizeSupported) {
            columnWidths[4 - 1] = maxWidth;
            columnRenderers[4] = dataCellRenderer;
        }
    }
    
    private HTMLLabel l;
    private JLabel w;
    private JProgressBar p;
    
    protected Component[] getAdditionalControls() {
        if (l == null) {
            l = new HTMLLabel() {
                protected void showURL(URL url) {
                    if (classesListController.isDiff()) {
                        classesListController.resetDiffAction();
                    } else {
                        classesListController.compareAction();
                    }
                }
            };
            l.setBorder(BorderFactory.createEmptyBorder());
            l.setFont(UIManager.getFont("ToolTip.font")); // NOI18N
            l.setText("<nobr><a href='#'>" + Bundle.ClassesListControllerUI_CompareWithAnotherText() + "</a></nobr>"); // NOI18N
        }
        
        if (w == null) {
            w = new JLabel(Bundle.ClassesListControllerUI_ComparingMsg());
            w.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
            w.setFont(UIManager.getFont("ToolTip.font")); // NOI18N
        }
        
        if (p == null) {
            p = new JProgressBar() {
                public Dimension getPreferredSize() {
                    Dimension d = l.getPreferredSize();
                    d.width = 130;
                    return d;
                }
                public Dimension getMinimumSize() {
                    return getPreferredSize();
                }
            };
        }
        
        JPanel indent = new JPanel(null);
        indent.setOpaque(false);
        indent.setPreferredSize(new Dimension(5, 5));
        indent.setMinimumSize(indent.getPreferredSize());
        
        w.setVisible(false);
        p.setVisible(false);
        l.setVisible(true);
        
        return new Component[] { w, p, l, indent };
    }
    
    public void showDiffProgress() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                w.setVisible(true);
                p.setIndeterminate(true);
                p.setVisible(true);
                l.setVisible(false);
            }
        });
    }
    
    public void hideDiffProgress() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                w.setVisible(false);
                p.setVisible(false);
                p.setIndeterminate(false);
                
                if (classesListController.isDiff()) {
                    l.setText("<nobr>" + NbBundle.getMessage(ClassesListControllerUI.class, // NOI18N
                              "ClassesListControllerUI_ShowingDiffText", "<a href='#'>", "</a>") + "</nobr>"); // NOI18N
                } else {
                    l.setText("<nobr><a href='#'>" + Bundle.ClassesListControllerUI_CompareWithAnotherText() + "</a></nobr>"); // NOI18N
                }
                l.setVisible(true);
            }
        });
    }

    private void initComponents() {
        if (retainedSizeSupported) {
            HeapFragmentWalker heap = classesListController.getClassesController().getHeapFragmentWalker();
            classesListTableModel.setRealColumnVisibility(4, heap.getRetainedSizesStatus() == HeapFragmentWalker.RETAINED_SIZES_COMPUTED);
        }
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
        classesListTable.getAccessibleContext().setAccessibleName(Bundle.ClassesListControllerUI_ClassesTableAccessName());
        classesListTable.getAccessibleContext().setAccessibleDescription(Bundle.ClassesListControllerUI_ClassesTableAccessDescr());
        classesListTable.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                        .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "DEFAULT_ACTION"); // NOI18N
        classesListTable.getActionMap().put("DEFAULT_ACTION", // NOI18N
                                            new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    performDefaultAction();
                }
            });

        // Disable traversing table cells using TAB and Shift+TAB
        Set keys = new HashSet(classesListTable.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS));
        keys.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0));
        classesListTable.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, keys);

        keys = new HashSet(classesListTable.getFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS));
        keys.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_MASK));
        classesListTable.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, keys);

        setColumnsData(true);

        filterComponent = FilterComponent.create(true, true);
        filterComponent.addFilterType(hasProjectContext ? 
                    Bundle.ClassesListControllerUI_FilterImplementation() : 
                    Bundle.ClassesListControllerUI_FilterSubclass(),
                ClassesListController.FILTER_SUBCLASS);
        filterComponent.setHint(Bundle.ClassesListControllerUI_DefaultFilterText());
        filterComponent.setFilter(filterValue, filterType);
        filterComponent.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    filterValue = filterComponent.getFilterValue();
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

        String progressRes = Icons.getResource(HeapWalkerIcons.PROGRESS);
        String hintText = "<img border='0' align='bottom' src='nbresloc:/" + progressRes + "'>&nbsp;&nbsp;" // NOI18N
                          + Bundle.ClassesListControllerUI_FilteringProgressText();
        hintArea.setText(hintText);
        noDataPanel.add(hintArea, BorderLayout.CENTER);

        contents = new CardLayout();
        contentsPanel = new JPanel(contents);
        contentsPanel.add(tablePanel, DATA);
        contentsPanel.add(noDataPanel, NO_DATA);
        contents.show(contentsPanel, NO_DATA);

        add(contentsPanel, BorderLayout.CENTER);
        add(filterComponent.getComponent(), BorderLayout.SOUTH);

        classesListTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    classesListController.classSelected((classesListTable.getSelectedRow() == -1) ? null
                                                                                                  : (JavaClass) displayCache[classesListTable
                                                                                                                             .getSelectedRow()][columnCount]);
                }
            });
    }
    
    private void initData() {
        initDataImpl(null);
    }

    private void initDataImpl(final JavaClass classToSelect) {
        if (displayCache == null) displayCache = new Object[0][columnCount + 1];

        CommonUtils.runInEventDispatchThread(new Runnable() {
            public void run() {
                final AtomicBoolean initInProgress = new AtomicBoolean(false);
                
                BrowserUtils.performTask(new Runnable() {
                    public void run() {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                if (contents != null && initInProgress.get())
                                    contents.show(contentsPanel, NO_DATA);
                            }
                        });
                    }
                }, 100);

                if (classToSelect == null) saveSelection();

                BrowserUtils.performTask(new Runnable() {
                    public void run() {
                        initInProgress.set(true);
                        
                        final Object[][] displayCache2 = classesListController.getData(
                                    FilterComponent.getFilterValues(filterValue), filterType,
                                    showZeroInstances, showZeroSize, sortingColumn, sortingOrder, columnCount);

                        initInProgress.set(false);

                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                if (isDiff != classesListController.isDiff()) {
                                    isDiff = !isDiff;
                                    CustomBarCellRenderer customBarCellRenderer = isDiff ?
                                            new DiffBarCellRenderer(classesListController.minDiff, classesListController.maxDiff) :
                                            new CustomBarCellRenderer(0, 100);
                                    columnRenderers[1] = customBarCellRenderer;
                                    
                                    TableCellRenderer dataCellRenderer = isDiff ?
                                            new LabelTableCellRenderer(JLabel.TRAILING) :
                                            new LabelBracketTableCellRenderer(JLabel.TRAILING);
                                    columnRenderers[2] = dataCellRenderer;
                                    columnRenderers[3] = dataCellRenderer;
                                    columnRenderers[4] = dataCellRenderer;
                                    setColumnsData(false);
                                }
                                
                                displayCache = displayCache2;
                                classesListTableModel.fireTableDataChanged();
                                if (classToSelect == null) restoreSelection();
                                else selectClassImpl(classToSelect);
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
            showInstancesForClass((JavaClass) displayCache[row][columnCount]);
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

    private void showPopupMenu(int row, int x, int y) {
        if(showSourceItem != null) {
            String className = BrowserUtils.getArrayBaseType((String)displayCache[row][0]);
            showSourceItem.setEnabled(!BrowserUtils.isPrimitiveType(className));
        }
        tablePopup.show(classesListTable, x, y);
    }

    private void showInstancesForClass(JavaClass jClass) {
        if (classesListController.isDiff() && jClass == null) {
            ProfilerDialogs.displayInfo(Bundle.ClassesListControllerUI_NoClassInBaseMsg());
        } else if (jClass.getInstancesCount() == 0) {
            ProfilerDialogs.displayInfo(Bundle.ClassesListControllerUI_NoInstancesMsg(jClass.getName()));
        } else {
            classesListController.getClassesController().getHeapFragmentWalker().showInstancesForClass(jClass);
        }
    }

    private void showSubclassesForClass(JavaClass jClass) {
        filterComponent.setFilter(jClass.getName(),ClassesListController.FILTER_SUBCLASS);
    }
}
