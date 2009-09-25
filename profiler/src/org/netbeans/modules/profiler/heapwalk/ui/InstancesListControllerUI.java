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

import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.lib.profiler.ui.UIConstants;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.JTitledPanel;
import org.netbeans.lib.profiler.ui.components.JTreeTable;
import org.netbeans.lib.profiler.ui.components.table.LabelTableCellRenderer;
import org.netbeans.lib.profiler.ui.components.treetable.AbstractTreeTableModel;
import org.netbeans.lib.profiler.ui.components.treetable.ExtendedTreeTableModel;
import org.netbeans.lib.profiler.ui.components.treetable.JTreeTablePanel;
import org.netbeans.lib.profiler.ui.components.treetable.TreeTableModel;
import org.netbeans.modules.profiler.heapwalk.HeapFragmentWalker.StateEvent;
import org.netbeans.modules.profiler.heapwalk.InstancesListController;
import org.netbeans.modules.profiler.heapwalk.model.HeapWalkerNode;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.TreePath;
import org.netbeans.modules.profiler.heapwalk.HeapFragmentWalker;
import org.openide.util.RequestProcessor;


/**
 *
 * @author Jiri Sedlacek
 */
public class InstancesListControllerUI extends JTitledPanel {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    // --- TreeTable model -------------------------------------------------------
    private class InstancesListTreeTableModel extends AbstractTreeTableModel {
        //~ Constructors ---------------------------------------------------------------------------------------------------------

        private InstancesListTreeTableModel() {
            super(InstancesListController.EMPTY_INSTANCE_NODE, true, sortingColumn, sortingOrder);
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        public Class getColumnClass(int column) {
            if (column == 0) {
                return TreeTableModel.class;
            } else {
                return Object.class;
            }
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

        public boolean isLeaf(Object node) {
            return ((HeapWalkerNode) node).isLeaf();
        }

        public Object getValueAt(Object object, int columnIndex) {
            if (object instanceof InstancesListController.InstancesListNode) {
                InstancesListController.InstancesListNode node = (InstancesListController.InstancesListNode) object;

                switch (columnIndex) {
                    case 0:
                        return node;
                    case 1:
                        return node.getSize();
                    case 2:
                        return node.getRetainedSize();

                    // TODO: uncomment once reachable size implemented
                    //          case 3: return node.getReachableSize();

                    default:
                        return null;
                }
            } else {
                HeapWalkerNode node = (HeapWalkerNode) object;

                switch (columnIndex) {
                    case 0:
                        return node;
                    default:
                        return ""; // NOI18N
                }
            }
        }

        public void sortByColumn(int column, boolean order) {
            sortingColumn = column;
            sortingOrder = order;

            Instance selectedInstance = instancesListController.getSelectedInstance();

            if (selectedInstance != null) {
                instancesListController.scheduleInstanceSelection(selectedInstance);
            } else if (instancesListTable != null) {
                int selectedRow = instancesListTable.getSelectedRow();

                if (selectedRow != -1) {
                    HeapWalkerNode selectedNode = (HeapWalkerNode) instancesListTable.getTree().getPathForRow(selectedRow)
                                                                                     .getLastPathComponent();

                    if (selectedNode instanceof InstancesListController.InstancesListContainerNode) {
                        instancesListController.scheduleContainerSelection(selectedNode.getParent().getIndexOfChild(selectedNode));
                    }
                }
            }

            if (isShowing()) {
                sorting = true;
            }

            update(true);
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String VIEW_CAPTION = NbBundle.getMessage(InstancesListControllerUI.class,
                                                                   "InstancesListControllerUI_ViewCaption"); // NOI18N
    private static final String SHOW_HIDE_COLUMNS_STRING = NbBundle.getMessage(InstancesListControllerUI.class,
                                                                               "InstancesListControllerUI_ShowHideColumnsString"); // NOI18N
    private static final String INSTANCE_COLUMN_NAME = NbBundle.getMessage(InstancesListControllerUI.class,
                                                                           "InstancesListControllerUI_InstanceColumnName"); // NOI18N
    private static final String INSTANCE_COLUMN_DESCR = NbBundle.getMessage(InstancesListControllerUI.class,
                                                                            "InstancesListControllerUI_InstanceColumnDescr"); // NOI18N
    private static final String SIZE_COLUMN_NAME = NbBundle.getMessage(InstancesListControllerUI.class,
                                                                       "InstancesListControllerUI_SizeColumnName"); // NOI18N
    private static final String SIZE_COLUMN_DESCR = NbBundle.getMessage(InstancesListControllerUI.class,
                                                                        "InstancesListControllerUI_SizeColumnDescr"); // NOI18N
    private static final String RETAINED_SIZE_COLUMN_NAME = NbBundle.getMessage(InstancesListControllerUI.class,
                                                                                "InstancesListControllerUI_RetainedSizeColumnName"); // NOI18N
    private static final String RETAINED_SIZE_COLUMN_DESCR = NbBundle.getMessage(InstancesListControllerUI.class,
                                                                                 "InstancesListControllerUI_RetainedSizeColumnDescr"); // NOI18N
    private static final String REACHABLE_SIZE_COLUMN_NAME = NbBundle.getMessage(InstancesListControllerUI.class,
                                                                                 "InstancesListControllerUI_ReachableSizeColumnName"); // NOI18N
    private static final String REACHABLE_SIZE_COLUMN_DESCR = NbBundle.getMessage(InstancesListControllerUI.class,
                                                                                  "InstancesListControllerUI_ReachableSizeColumnDescr"); // NOI18N
                                                                                                                                         // -----
    private static ImageIcon ICON_INSTANCES = ImageUtilities.loadImageIcon("org/netbeans/modules/profiler/heapwalk/ui/resources/instances.png", false); // NOI18N
    private int columnCount;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private ExtendedTreeTableModel instancesListTableModel;
    private FieldTreeCellRenderer treeCellRenderer = new FieldTreeCellRenderer();
    private Instance instanceToSelect = null;
    private InstancesListController instancesListController;
    private InstancesListTreeTableModel realInstancesListModel;

    private boolean retainedSizeSupported;

    // --- UI definition ---------------------------------------------------------
    private JPanel dataPanel;
    private JPopupMenu cornerPopup;
    private JTreeTable instancesListTable;
    private String filterValue = ""; // NOI18N
    private String selectedRowContents;
    private String[] columnNames;
    private javax.swing.table.TableCellRenderer[] columnRenderers;
    private String[] columnToolTips;
    private int[] columnWidths;
    private boolean internalCornerButtonClick = false; // flag for closing columns popup by pressing cornerButton

    // --- Selection utils -------------------------------------------------------
    private boolean selectionSaved = false;
    private boolean sorting = false;
    private boolean sortingOrder = false;
    private int selectedRow;
    private int sortingColumn = 1;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    // --- Constructors ----------------------------------------------------------
    public InstancesListControllerUI(InstancesListController instancesListController) {
        super(VIEW_CAPTION, ICON_INSTANCES, true);

        this.instancesListController = instancesListController;

        retainedSizeSupported = instancesListController.getInstancesController().
                                getHeapFragmentWalker().getRetainedSizesStatus() !=
                                HeapFragmentWalker.RETAINED_SIZES_UNSUPPORTED;
        columnCount = retainedSizeSupported ? 3 : 2;

        realInstancesListModel = new InstancesListTreeTableModel();
        instancesListTableModel = new ExtendedTreeTableModel(realInstancesListModel);

        initColumnsData();
        initData();
        initComponents();

        instancesListController.getInstancesController().
            getHeapFragmentWalker().addStateListener(
                new HeapFragmentWalker.StateListener() {
                    public void stateChanged(StateEvent e) {
                        if (e.getRetainedSizesStatus() == HeapFragmentWalker.
                            RETAINED_SIZES_COMPUTED && e.isMasterChange()) {
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run() {
                                        instancesListTableModel.
                                                setRealColumnVisibility(2, true);
                                        instancesListTable.createDefaultColumnsFromModel();
                                        instancesListTable.updateTreeTableHeader();
                                        setColumnsData();
                                    }
                                });
                        }
                    }
                }
            );
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void initColumns() {
        if (instancesListTable != null) {
            SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        JavaClass selectedClass = instancesListController.getInstancesController().getSelectedClass();

                        if ((selectedClass != null) && selectedClass.isArray()) {
                            if (!instancesListTableModel.isRealColumnVisible(1)) {
                                toggleColumnVisibility(1, false);
                            }
                        } else {
                            if (instancesListTableModel.isRealColumnVisible(1)) {
                                toggleColumnVisibility(1, false);
                            }
                        }
                    }
                });
        }
    }

    // --- Public interface ------------------------------------------------------
    public void makeVisible() {
        if (!isShowing()) {
            setVisible(true);
        }
    }

    // --- Internal interface ----------------------------------------------------
    public void refreshView() {
        // Used for refreshing treetable after lazy-populating the model
        if (instancesListTable != null) {
            HeapWalkerNode root = (HeapWalkerNode) instancesListTableModel.getRoot();
            instancesListTable.getTree()
                              .setShowsRootHandles(root instanceof InstancesListController.InstancesListClassNode
                                                   && root.getChild(0) instanceof InstancesListController.InstancesListContainerNode);
            instancesListTable.updateTreeTable();
        }
    }

    public void selectInstance(Instance instance) {
        if (displaysFlatInstances()) {
            selectFlatInstance(instance);
        } else if (displaysCollapsedInstances()) {
            selectCollapsedInstance(instance);
        }
    }

    public void selectPath(TreePath pathToSelect) {
        if (instancesListTable == null) {
            return;
        }

        final TreePath pathToSelectFinal = pathToSelect;
        SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    if (!isShowing()) {
                        setVisible(true);
                    }

                    instancesListTable.getTree().setSelectionPath(pathToSelectFinal);

                    Rectangle pathToSelectBounds = instancesListTable.getTree().getPathBounds(pathToSelectFinal);

                    if (pathToSelectBounds != null) {
                        instancesListTable.scrollRectToVisible(pathToSelectBounds); // Fix for Issue 105299, pathToSelectBounds can be null
                    }

                    if (sorting) {
                        sorting = false;
                    }
                }
            });
    }

    public void update() {
        update(false);
    }

    protected void initColumnSelectorItems() {
        cornerPopup.removeAll();

        JCheckBoxMenuItem menuItem;

        for (int i = 0; i < realInstancesListModel.getColumnCount(); i++) {
            menuItem = new JCheckBoxMenuItem(realInstancesListModel.getColumnName(i));
            menuItem.setActionCommand(new Integer(i).toString());
            addMenuItemListener(menuItem);

            if (instancesListTable != null) {
                menuItem.setState(instancesListTableModel.isRealColumnVisible(i));

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
        TableColumnModel colModel = instancesListTable.getColumnModel();

        instancesListTable.setTreeCellRenderer(treeCellRenderer);

        for (int i = 0; i < instancesListTableModel.getColumnCount(); i++) {
            int index = instancesListTableModel.getRealColumn(i);

            if (index != 0) {
                colModel.getColumn(i).setPreferredWidth(columnWidths[index - 1]);
                colModel.getColumn(i).setCellRenderer(columnRenderers[index]);
            }
        }
    }

    private void addMenuItemListener(final JCheckBoxMenuItem menuItem) {
        final boolean[] internalChange = new boolean[1];
        menuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (internalChange[0]) return;
                final int column = Integer.parseInt(e.getActionCommand());
                if (column == 2 && !instancesListTableModel.isRealColumnVisible(column)) {
                    RequestProcessor.getDefault().post(new Runnable() {
                        public void run() {
                            final int retainedSizesState = instancesListController.getInstancesController().
                                    getHeapFragmentWalker().computeRetainedSizes(false);
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    if (retainedSizesState != HeapFragmentWalker.RETAINED_SIZES_COMPUTED) {
                                        internalChange[0] = true;
                                        menuItem.setSelected(!menuItem.isSelected());
                                        internalChange[0] = false;
                                    } else {
                                        toggleColumnVisibility(column, true);
                                    }
                                }
                            });
                        }
                    });
                } else {
                    toggleColumnVisibility(column, true);
                }

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

    private boolean displaysCollapsedInstances() {
        HeapWalkerNode root = (HeapWalkerNode) instancesListTableModel.getRoot();

        return ((root != null) && (root.getNChildren() > 0)
               && root.getChild(0) instanceof InstancesListController.InstancesListContainerNode);
    }

    private boolean displaysFlatInstances() {
        HeapWalkerNode root = (HeapWalkerNode) instancesListTableModel.getRoot();

        return ((root != null) && (root.getNChildren() > 0)
               && root.getChild(0) instanceof InstancesListController.InstancesListInstanceNode);
    }

    private void initColumnsData() {
        columnWidths = new int[columnCount - 1]; // Width of the first column fits to width
        columnNames = new String[columnCount];
        columnToolTips = new String[columnCount];
        columnRenderers = new TableCellRenderer[columnCount];

        columnNames[0] = INSTANCE_COLUMN_NAME;
        columnToolTips[0] = INSTANCE_COLUMN_DESCR;

        columnNames[1] = SIZE_COLUMN_NAME;
        columnToolTips[1] = SIZE_COLUMN_DESCR;

        if (retainedSizeSupported) {
            columnNames[2] = RETAINED_SIZE_COLUMN_NAME;
            columnToolTips[2] = RETAINED_SIZE_COLUMN_DESCR;
        }

        // TODO: uncomment once reachable size implemented
        //    columnNames[3] = REACHABLE_SIZE_COLUMN_NAME;
        //    columnToolTips[3] = REACHABLE_SIZE_COLUMN_DESCR;
        int maxWidth = getFontMetrics(getFont()).charWidth('W') * 7; // NOI18N // initial width of data columns

        FieldTreeCellRenderer treeCellRenderer = new FieldTreeCellRenderer();
        treeCellRenderer.setLeafIcon(null);
        treeCellRenderer.setClosedIcon(null);
        treeCellRenderer.setOpenIcon(null);

        LabelTableCellRenderer dataCellRenderer = new LabelTableCellRenderer(JLabel.TRAILING);

        // method / class / package name
        columnRenderers[0] = null;

        columnWidths[1 - 1] = maxWidth;
        columnRenderers[1] = dataCellRenderer;

        if (retainedSizeSupported) {
            columnWidths[2 - 1] = maxWidth;
            columnRenderers[2] = dataCellRenderer;
        }
        
        // TODO: uncomment once reachable size implemented
        //    columnWidths[3 - 1] = maxWidth;
        //    columnRenderers[3] = dataCellRenderer;
    }

    private void initComponents() {
        treeCellRenderer.setLeafIcon(null);
        treeCellRenderer.setClosedIcon(null);
        treeCellRenderer.setOpenIcon(null);

        if (retainedSizeSupported)
            instancesListTableModel.setRealColumnVisibility(2, instancesListController.
                getInstancesController().getHeapFragmentWalker().getRetainedSizesStatus()
                                          == HeapFragmentWalker.RETAINED_SIZES_COMPUTED);
        
        // TODO: uncomment once retained & reachable size implemented
        //    instancesListTableModel.setRealColumnVisibility(3, false);
        instancesListTable = new JTreeTable(instancesListTableModel) {
                public void doLayout() {
                    int columnsWidthsSum = 0;
                    int realFirstColumn = -1;

                    TableColumnModel colModel = getColumnModel();

                    for (int i = 0; i < instancesListTableModel.getColumnCount(); i++) {
                        if (instancesListTableModel.getRealColumn(i) == 0) {
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
        instancesListTable.getTree().setRootVisible(false);
        instancesListTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        instancesListTable.setGridColor(UIConstants.TABLE_VERTICAL_GRID_COLOR);
        instancesListTable.setSelectionBackground(UIConstants.TABLE_SELECTION_BACKGROUND_COLOR);
        instancesListTable.setSelectionForeground(UIConstants.TABLE_SELECTION_FOREGROUND_COLOR);
        instancesListTable.setShowHorizontalLines(UIConstants.SHOW_TABLE_HORIZONTAL_GRID);
        instancesListTable.setShowVerticalLines(UIConstants.SHOW_TABLE_VERTICAL_GRID);
        instancesListTable.setRowMargin(UIConstants.TABLE_ROW_MARGIN);
        instancesListTable.setRowHeight(UIUtils.getDefaultRowHeight() + 2);
        instancesListTable.getTree().setLargeModel(true);

        // Disable traversing table cells using TAB and Shift+TAB
        Set keys = new HashSet(instancesListTable.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS));
        keys.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0));
        instancesListTable.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, keys);

        keys = new HashSet(instancesListTable.getFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS));
        keys.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_MASK));
        instancesListTable.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, keys);

        setColumnsData();

        //    tablePopup = createTablePopup();
        cornerPopup = new JPopupMenu();

        JTreeTablePanel tablePanel = new JTreeTablePanel(instancesListTable);
        tablePanel.setCorner(JScrollPane.UPPER_RIGHT_CORNER, createHeaderPopupCornerButton(cornerPopup));

        setLayout(new BorderLayout());
        add(tablePanel, BorderLayout.CENTER);

        instancesListTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    if (sorting || e.getValueIsAdjusting()) {
                        return;
                    }

                    Instance selectedInstance = null;
                    int selectedRow = instancesListTable.getSelectedRow();

                    if (selectedRow != -1) {
                        HeapWalkerNode selectedNode = (HeapWalkerNode) instancesListTable.getTree().getPathForRow(selectedRow)
                                                                                         .getLastPathComponent();

                        if (selectedNode instanceof InstancesListController.InstancesListInstanceNode) {
                            selectedInstance = ((InstancesListController.InstancesListInstanceNode) selectedNode).getInstance();
                        }
                    }

                    instancesListController.instanceSelected(selectedInstance);
                }
            });

        setPreferredSize(new Dimension(225, 500));
    }

    // --- Private implementation ------------------------------------------------
    private void initData() {
        instancesListTableModel.setRoot(instancesListController.getFilteredSortedInstances(filterValue, sortingColumn,
                                                                                           sortingOrder));
        refreshView();
    }

    private void restoreSelection() {
        if (selectedRowContents != null) {
            instancesListTable.selectRowByContents(selectedRowContents, 0, true);
        }

        selectionSaved = false;
    }

    private void saveSelection() {
        if (selectionSaved) {
            return;
        }

        selectedRow = (instancesListTable == null) ? (-1) : instancesListTable.getSelectedRow();
        selectedRowContents = null;

        if (selectedRow != -1) {
            selectedRowContents = instancesListTable.getValueAt(selectedRow, 0).toString();
        }

        selectionSaved = true;
    }

    // Selects instance displayed in container node
    private void selectCollapsedInstance(Instance instance) {
        HeapWalkerNode root = (HeapWalkerNode) instancesListTableModel.getRoot();

        if (root instanceof InstancesListController.InstancesListNode) {
            InstancesListController.InstancesListNode instancesListRoot = (InstancesListController.InstancesListNode) root;
            TreePath instancePath = instancesListRoot.getInstancePath(instance);

            if (instancePath != null) {
                // instance node already created
                selectPath(instancePath);
            } else {
                // instance node collapsed and not yet created
                HeapWalkerNode instanceContainer = instancesListController.getInstanceContainer(instance,
                                                                                                (InstancesListController.InstancesListClassNode) root);

                if (instanceContainer != null) {
                    instancesListController.scheduleInstanceSelection(instance);
                    instanceContainer.getChildren(); // lazily computes children and invokes instance selection
                }
            }
        }
    }

    // Selects instance when no containers are displayed
    private void selectFlatInstance(Instance instance) {
        HeapWalkerNode root = (HeapWalkerNode) instancesListTableModel.getRoot();

        if (root instanceof InstancesListController.InstancesListNode) {
            InstancesListController.InstancesListNode instancesListRoot = (InstancesListController.InstancesListNode) root;
            TreePath instancePath = instancesListRoot.getInstancePath(instance);

            if (instancePath != null) {
                selectPath(instancePath);
            }
        }
    }

    private void showColumnSelectionPopup(final JPopupMenu headerPopup, final JButton cornerButton) {
        initColumnSelectorItems();
        headerPopup.show(cornerButton, cornerButton.getWidth() - headerPopup.getPreferredSize().width, cornerButton.getHeight());
    }

    private void toggleColumnVisibility(int column, boolean reSort) {

        boolean sortResults = false;
        int currentSortingColumn = instancesListTable.getSortingColumn();
        int realSortingColumn = instancesListTableModel.getRealColumn(currentSortingColumn);
        boolean isColumnVisible = instancesListTableModel.isRealColumnVisible(column);

        // Current sorting column is going to be hidden
        if ((isColumnVisible) && (column == realSortingColumn)) {
            // Try to set next column as a currentSortingColumn. If currentSortingColumn is the last column,
            // set previous column as a sorting Column (one column is always visible).
            currentSortingColumn = ((currentSortingColumn + 1) == instancesListTableModel.getColumnCount())
                                   ? (currentSortingColumn - 1) : (currentSortingColumn + 1);
            realSortingColumn = instancesListTableModel.getRealColumn(currentSortingColumn);
            sortResults = true;
        }

        instancesListTableModel.setRealColumnVisibility(column, !isColumnVisible);
        instancesListTable.createDefaultColumnsFromModel();
        instancesListTable.updateTreeTableHeader(); // required to restore table header renderer
        currentSortingColumn = instancesListTableModel.getVirtualColumn(realSortingColumn);

        if (sortResults) {
            if (reSort) {
                instancesListTableModel.sortByColumn(currentSortingColumn,
                                                     instancesListTableModel.getInitialSorting(currentSortingColumn));
            } else {
                sortingOrder = instancesListTableModel.getInitialSorting(currentSortingColumn);
                sortingColumn = currentSortingColumn;
            }

            instancesListTable.updateTreeTable();
        }

        instancesListTable.setSortingColumn(currentSortingColumn);
        instancesListTable.setSortingOrder(sortingOrder);
        instancesListTable.getTableHeader().repaint();
        setColumnsData();
    }

    private void update(boolean fromSorting) {
        makeVisible();
        initData();

        if (!fromSorting && (instancesListTable != null)) {
            instancesListTable.resetTreeCellOffsetX(); // Ideally should be invoked directly on the component when root node changes
        }
    }
}
