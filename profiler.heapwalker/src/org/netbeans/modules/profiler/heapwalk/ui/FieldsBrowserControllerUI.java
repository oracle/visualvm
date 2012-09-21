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


import org.netbeans.lib.profiler.ui.UIConstants;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import org.netbeans.lib.profiler.ui.components.JTitledPanel;
import org.netbeans.lib.profiler.ui.components.JTreeTable;
import org.netbeans.lib.profiler.ui.components.table.LabelTableCellRenderer;
import org.netbeans.lib.profiler.ui.components.treetable.AbstractTreeTableModel;
import org.netbeans.lib.profiler.ui.components.treetable.ExtendedTreeTableModel;
import org.netbeans.lib.profiler.ui.components.treetable.JTreeTablePanel;
import org.netbeans.lib.profiler.ui.components.treetable.TreeTableModel;
import org.netbeans.modules.profiler.heapwalk.FieldsBrowserController;
import org.netbeans.modules.profiler.heapwalk.model.ClassNode;
import org.netbeans.modules.profiler.heapwalk.model.HeapWalkerInstanceNode;
import org.netbeans.modules.profiler.heapwalk.model.HeapWalkerNode;
import org.openide.util.NbBundle;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Icon;
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
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.TreePath;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.modules.profiler.api.icons.GeneralIcons;
import org.netbeans.modules.profiler.api.GoToSource;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.heapwalk.HeapFragmentWalker;
import org.netbeans.modules.profiler.heapwalk.HeapFragmentWalker.StateEvent;
import org.netbeans.modules.profiler.heapwalk.model.BrowserUtils;
import org.netbeans.modules.profiler.heapwalk.ui.icons.HeapWalkerIcons;
import org.openide.util.Lookup;


/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "FieldsBrowserControllerUI_ViewTitleFields=Fields",
    "FieldsBrowserControllerUI_ViewTitleStaticFields=Static Fields",
    "FieldsBrowserControllerUI_NoInstanceSelectedMsg=<b>No instance selected.</b><br><br>To view instance fields, select an instance in {0}&nbsp;Instances list.",
    "FieldsBrowserControllerUI_NoClassSelectedMsg=<b>No class selected.</b><br><br>To view static fields, select a class in {0}&nbsp;Classes list.",
    "FieldsBrowserControllerUI_ShowLoopItemText=Select Loop Origin",
    "FieldsBrowserControllerUI_CopyPathFromRoot=Copy Path From Root",
    "FieldsBrowserControllerUI_ShowInstanceItemText=Show Instance",
    "FieldsBrowserControllerUI_ShowInInstancesItemText=Show in Instances View",
    "FieldsBrowserControllerUI_ShowClassItemText=Show Class",
    "FieldsBrowserControllerUI_ShowInClassesItemText=Show in Classes View",
    "FieldsBrowserControllerUI_GoToSourceItemText=Go To Source",
    "FieldsBrowserControllerUI_ShowHideColumnsString=Show or hide columns",
    "FieldsBrowserControllerUI_FieldColumnName=Field",
    "FieldsBrowserControllerUI_FieldColumnDescr=Name of field",
    "FieldsBrowserControllerUI_TypeColumnName=Type",
    "FieldsBrowserControllerUI_TypeColumnDescr=Type of data or referenced instance",
    "FieldsBrowserControllerUI_FullTypeColumnName=Full Type",
    "FieldsBrowserControllerUI_FullTypeColumnDescr=Fully qualified type of data or referenced instance",
    "FieldsBrowserControllerUI_ValueColumnName=Value",
    "FieldsBrowserControllerUI_ValueColumnDescr=Field value or number of referenced instance",
    "FieldsBrowserControllerUI_SizeColumnName=Size",
    "FieldsBrowserControllerUI_SizeColumnDescr=Size of the field instance",
    "FieldsBrowserControllerUI_RetainedSizeColumnName=Retained",
    "FieldsBrowserControllerUI_RetainedSizeColumnDescr=Retained size of the field instance"
})
public class FieldsBrowserControllerUI extends JTitledPanel {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private class FieldsListTableKeyListener extends KeyAdapter {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void keyPressed(KeyEvent e) {
            if ((e.getKeyCode() == KeyEvent.VK_CONTEXT_MENU)
                    || ((e.getKeyCode() == KeyEvent.VK_F10) && (e.getModifiers() == InputEvent.SHIFT_MASK))) {
                int selectedRow = fieldsListTable.getSelectedRow();

                if (selectedRow != -1) {
                    showPopupMenu(selectedRow, -1, -1);
                }
            }
        }
    }

    // --- Listeners -------------------------------------------------------------
    private class FieldsListTableMouseListener extends MouseAdapter {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        private void updateSelection(int row) {
            fieldsListTable.requestFocusInWindow();
            if (row != -1) fieldsListTable.setRowSelectionInterval(row, row);
            else fieldsListTable.clearSelection();
        }

        public void mousePressed(final MouseEvent e) {
            final int row = fieldsListTable.rowAtPoint(e.getPoint());
            updateSelection(row);
            if (e.isPopupTrigger()) showPopupMenu(row, e.getX(), e.getY());
        }

        public void mouseReleased(MouseEvent e) {
            int row = fieldsListTable.rowAtPoint(e.getPoint());
            updateSelection(row);
            if (e.isPopupTrigger()) showPopupMenu(row, e.getX(), e.getY());
        }

        public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                int row = fieldsListTable.rowAtPoint(e.getPoint());
                if (e.getX() >= fieldsListTable.getTree().getRowBounds(row).x -
                                fieldsListTable.getTreeCellOffsetX() && row != -1) {
                    HeapWalkerNode node = (HeapWalkerNode) fieldsListTable.getTree().
                            getPathForRow(row).getLastPathComponent();
                    if (node instanceof HeapWalkerInstanceNode)
                            performDefaultAction();
                }
            }
        }
    }

    // --- Table model -----------------------------------------------------------
    private class FieldsListTreeTableModel extends AbstractTreeTableModel {
        //~ Constructors ---------------------------------------------------------------------------------------------------------

        private FieldsListTreeTableModel() {
            super((fieldsBrowserController.getRootMode() == FieldsBrowserController.ROOT_INSTANCE)
                  ? FieldsBrowserController.EMPTY_INSTANCE_NODE : FieldsBrowserController.EMPTY_CLASS_NODE);
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

        public boolean isLeaf(Object node) {
            return ((HeapWalkerNode) node).isLeaf();
        }

        public Object getValueAt(Object object, int columnIndex) {
            HeapWalkerNode fieldNode = (HeapWalkerNode) object;

            switch (columnIndex) {
                case 0:
                    return fieldNode;
                case 1:
                    return fieldNode.getSimpleType();
                case 2:
                    return fieldNode.getType();
                case 3:
                    return fieldNode.getValue();
                case 4:
                    return fieldNode.getSize();
                case 5:
                    return fieldNode.getRetainedSize();
                default:
                    return null;
            }
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static Icon ICON_FIELDS = Icons.getIcon(HeapWalkerIcons.DATA);

    // --- UI definition ---------------------------------------------------------
    private static final String DATA = "Data"; // NOI18N
    private static final String NO_DATA = "No data"; // NOI18N
    private int columnCount;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private CardLayout contents;
    private ExtendedTreeTableModel fieldsListTableModel;
    private FieldTreeCellRenderer treeCellRenderer = new FieldTreeCellRenderer();
    private FieldsBrowserController fieldsBrowserController;
    private FieldsListTreeTableModel realFieldsListTableModel;
    private JMenuItem showClassItem;
    private JMenuItem showInstanceItem;
    private JMenuItem showLoopOriginItem;
    private JMenuItem copyPathFromRootItem;
    private JMenuItem showSourceItem;
    private JPanel dataPanel;
    private JPanel noDataPanel;
    private JPopupMenu cornerPopup;
    private JPopupMenu tablePopup;
    private JTreeTable fieldsListTable;
    private String filterValue = ""; // NOI18N
    private String[] columnNames;
    private javax.swing.table.TableCellRenderer[] columnRenderers;
    private String[] columnToolTips;
    private int[] columnWidths;
    private boolean internalCornerButtonClick = false; // flag for closing columns popup by pressing cornerButton
    private boolean needsUpdate = true;
    private boolean sortingOrder = true;
    private int sortingColumn = 0;

    private boolean retainedSizeSupported;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    // --- Constructors ----------------------------------------------------------
    public FieldsBrowserControllerUI(FieldsBrowserController fieldsBrowserController) {
        super((fieldsBrowserController.getRootMode() == FieldsBrowserController.ROOT_INSTANCE) ? 
                Bundle.FieldsBrowserControllerUI_ViewTitleFields()
                : Bundle.FieldsBrowserControllerUI_ViewTitleStaticFields(),
              ICON_FIELDS, true);

        this.fieldsBrowserController = fieldsBrowserController;

        retainedSizeSupported = fieldsBrowserController.getInstancesControllerHandler().
                                getHeapFragmentWalker().getRetainedSizesStatus() !=
                                HeapFragmentWalker.RETAINED_SIZES_UNSUPPORTED;
        columnCount = retainedSizeSupported ? 6 : 5;

        realFieldsListTableModel = new FieldsListTreeTableModel();
        fieldsListTableModel = new ExtendedTreeTableModel(realFieldsListTableModel);

        addHierarchyListener(new HierarchyListener() {
                public void hierarchyChanged(HierarchyEvent e) {
                    if (((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) && isShowing() && needsUpdate) {
                        update();
                    }
                }
            });

        initColumnsData();
        initData();
        initComponents();

        fieldsBrowserController.getInstancesControllerHandler().
            getHeapFragmentWalker().addStateListener(
                new HeapFragmentWalker.StateListener() {
                    public void stateChanged(StateEvent e) {
                        if (e.getRetainedSizesStatus() == HeapFragmentWalker.
                            RETAINED_SIZES_COMPUTED && e.isMasterChange()) {
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run() {
                                        fieldsListTableModel.
                                                setRealColumnVisibility(5, true);
                                        fieldsListTable.createDefaultColumnsFromModel();
                                        fieldsListTable.updateTreeTableHeader();
                                        setColumnsData();
                                    }
                                });
                        }
                    }
                }
            );
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    // --- Internal interface ----------------------------------------------------
    public void refreshView() {
        // Used for refreshing treetable after lazy-populating the model
        if (fieldsListTable != null) {
            fieldsListTable.updateTreeTable();
        }
    }

    public void selectNode(HeapWalkerNode node) {
        fieldsListTable.selectNode(node, true);
    }

    // --- Public interface ------------------------------------------------------
    public void update() {
        if (isShowing()) {
            initData();

            if (contents != null) { // ui already initialized

                if ((fieldsListTableModel.getRoot() == null)
                        || (fieldsListTableModel.getRoot() == FieldsBrowserController.EMPTY_CLASS_NODE)
                        || (fieldsListTableModel.getRoot() == FieldsBrowserController.EMPTY_INSTANCE_NODE)) {
                    contents.show(getContentPanel(), NO_DATA);
                } else {
                    contents.show(getContentPanel(), DATA);
                }

                fieldsListTable.resetTreeCellOffsetX(); // Ideally should be invoked directly on the component when root node changes
            }

            needsUpdate = false;
        } else {
            needsUpdate = true;
        }
    }

    protected void initColumnSelectorItems() {
        cornerPopup.removeAll();

        JCheckBoxMenuItem menuItem;

        for (int i = 0; i < realFieldsListTableModel.getColumnCount(); i++) {
            menuItem = new JCheckBoxMenuItem(realFieldsListTableModel.getColumnName(i));
            menuItem.setActionCommand(Integer.valueOf(i).toString());
            addMenuItemListener(menuItem);

            if (fieldsListTable != null) {
                menuItem.setState(fieldsListTableModel.isRealColumnVisible(i));

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
        TableColumnModel colModel = fieldsListTable.getColumnModel();

        fieldsListTable.setTreeCellRenderer(treeCellRenderer);

        for (int i = 0; i < fieldsListTableModel.getColumnCount(); i++) {
            int index = fieldsListTableModel.getRealColumn(i);

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
                if (column == 5 && !fieldsListTableModel.isRealColumnVisible(column)) {
                    BrowserUtils.performTask(new Runnable() {
                        public void run() {
                            final int retainedSizesState = fieldsBrowserController.getInstancesControllerHandler().
                                    getHeapFragmentWalker().computeRetainedSizes(false);
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    if (retainedSizesState != HeapFragmentWalker.RETAINED_SIZES_COMPUTED) {
                                        internalChange[0] = true;
                                        menuItem.setSelected(!menuItem.isSelected());
                                        internalChange[0] = false;
                                    } else {
                                        fieldsListTableModel.setRealColumnVisibility(column,
                                                !fieldsListTableModel.isRealColumnVisible(column));
                                        fieldsListTable.createDefaultColumnsFromModel();
                                        fieldsListTable.updateTreeTableHeader();
                                        setColumnsData();
                                    }
                                }
                            });
                        }
                    });
                } else {
                    fieldsListTableModel.setRealColumnVisibility(column,
                            !fieldsListTableModel.isRealColumnVisible(column));
                    fieldsListTable.createDefaultColumnsFromModel();
                    fieldsListTable.updateTreeTableHeader();
                    setColumnsData();
                }
            }
        });
    }

    private JButton createHeaderPopupCornerButton(final JPopupMenu headerPopup) {
        final JButton cornerButton = new JButton(Icons.getIcon(GeneralIcons.HIDE_COLUMN));
        cornerButton.setToolTipText(Bundle.FieldsBrowserControllerUI_ShowHideColumnsString());
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

        // Show Instance
        showInstanceItem = new JMenuItem((fieldsBrowserController.getRootMode() == FieldsBrowserController.ROOT_INSTANCE)
                                         ? Bundle.FieldsBrowserControllerUI_ShowInstanceItemText() : 
                                           Bundle.FieldsBrowserControllerUI_ShowInInstancesItemText());
        showInstanceItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    performDefaultAction();
                }
            });
        showInstanceItem.setFont(popup.getFont().deriveFont(Font.BOLD));

        // Show in Classes View
        showClassItem = new JMenuItem((fieldsBrowserController.getRootMode() == FieldsBrowserController.ROOT_INSTANCE)
                                      ? Bundle.FieldsBrowserControllerUI_ShowInClassesItemText() : 
                                        Bundle.FieldsBrowserControllerUI_ShowClassItemText());
        showClassItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    int row = fieldsListTable.getSelectedRow();

                    if (row != -1) {
                        HeapWalkerNode node = (HeapWalkerNode) fieldsListTable.getTree().getPathForRow(row).getLastPathComponent();

                        if (node instanceof HeapWalkerInstanceNode && ((HeapWalkerInstanceNode) node).hasInstance()) {
                            if (fieldsBrowserController.getRootMode() == FieldsBrowserController.ROOT_CLASS) {
                                fieldsBrowserController.createNavigationHistoryPoint();
                            }

                            HeapWalkerInstanceNode instanceNode = (HeapWalkerInstanceNode) node;
                            fieldsBrowserController.navigateToClass(instanceNode.getInstance().getJavaClass());
                        }
                    }
                }
            });
        
        // Copy Path From Root
        copyPathFromRootItem = new JMenuItem(Bundle.FieldsBrowserControllerUI_CopyPathFromRoot());
        copyPathFromRootItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int row = fieldsListTable.getSelectedRow();
                if (row != -1) {
                    TreePath path = fieldsListTable.getTree().getPathForRow(row);
                    BrowserUtils.copyPathFromRoot(path);
                }
            };
        });
        

        // Show Loop Origin
        showLoopOriginItem = new JMenuItem(Bundle.FieldsBrowserControllerUI_ShowLoopItemText());
        showLoopOriginItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    int row = fieldsListTable.getSelectedRow();

                    if (row != -1) {
                        HeapWalkerNode node = (HeapWalkerNode) fieldsListTable.getTree().getPathForRow(row).getLastPathComponent();

                        if (node instanceof HeapWalkerInstanceNode && ((HeapWalkerInstanceNode) node).isLoop()) {
                            selectNode(((HeapWalkerInstanceNode) node).getLoopTo());
                        }
                    }
                }
            });

        if (GoToSource.isAvailable()) {
            showSourceItem = new JMenuItem(Bundle.FieldsBrowserControllerUI_GoToSourceItemText());
            showSourceItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        int row = fieldsListTable.getSelectedRow();

                        if (row != -1) {
                            HeapWalkerNode node = (HeapWalkerNode) fieldsListTable.getTree().getPathForRow(row).getLastPathComponent();
                            String className = BrowserUtils.getArrayBaseType(node.getType());
                            Lookup.Provider p = fieldsBrowserController.getInstancesControllerHandler().getHeapFragmentWalker().getHeapDumpProject();
                            GoToSource.openSource(p, className, null, null);
                        }
                    }
                });
        }

        popup.add(showInstanceItem);
        popup.add(showClassItem);
        popup.addSeparator();
        popup.add(copyPathFromRootItem);
        popup.addSeparator();
        popup.add(showLoopOriginItem);
        if (showSourceItem != null) popup.add(showSourceItem);

        return popup;
    }

    private void initColumnsData() {
        columnWidths = new int[columnCount - 1]; // Width of the first column fits to width
        columnNames = new String[columnCount];
        columnToolTips = new String[columnCount];
        columnRenderers = new TableCellRenderer[columnCount];

        columnNames[0] = Bundle.FieldsBrowserControllerUI_FieldColumnName();
        columnToolTips[0] = Bundle.FieldsBrowserControllerUI_FieldColumnDescr();

        columnNames[1] = Bundle.FieldsBrowserControllerUI_TypeColumnName();
        columnToolTips[1] = Bundle.FieldsBrowserControllerUI_TypeColumnDescr();

        columnNames[2] = Bundle.FieldsBrowserControllerUI_FullTypeColumnName();
        columnToolTips[2] = Bundle.FieldsBrowserControllerUI_FullTypeColumnDescr();

        columnNames[3] = Bundle.FieldsBrowserControllerUI_ValueColumnName();
        columnToolTips[3] = Bundle.FieldsBrowserControllerUI_ValueColumnDescr();

        columnNames[4] = Bundle.FieldsBrowserControllerUI_SizeColumnName();
        columnToolTips[4] = Bundle.FieldsBrowserControllerUI_SizeColumnDescr();

        if (retainedSizeSupported) {
            columnNames[5] = Bundle.FieldsBrowserControllerUI_RetainedSizeColumnName();
            columnToolTips[5] = Bundle.FieldsBrowserControllerUI_RetainedSizeColumnDescr();
        }

        int unitWidth = getFontMetrics(getFont()).charWidth('W'); // NOI18N // initial width of data columns

        FieldTreeCellRenderer treeCellRenderer = new FieldTreeCellRenderer();
        treeCellRenderer.setLeafIcon(null);
        treeCellRenderer.setClosedIcon(null);
        treeCellRenderer.setOpenIcon(null);

        LabelTableCellRenderer dataCellRendererL = new LabelTableCellRenderer(JLabel.LEADING);
        LabelTableCellRenderer dataCellRendererT = new LabelTableCellRenderer(JLabel.TRAILING);

        // method / class / package name
        columnRenderers[0] = null;

        columnWidths[1 - 1] = unitWidth * 18;
        columnRenderers[1] = dataCellRendererL;

        columnWidths[2 - 1] = unitWidth * 28;
        columnRenderers[2] = dataCellRendererL;

        columnWidths[3 - 1] = unitWidth * 14;
        columnRenderers[3] = dataCellRendererT;

        columnWidths[4 - 1] = unitWidth * 7;
        columnRenderers[4] = dataCellRendererT;

        if (retainedSizeSupported) {
            columnWidths[5 - 1] = unitWidth * 7;
            columnRenderers[5] = dataCellRendererT;
        }
    }

    private void initComponents() {
        treeCellRenderer.setLeafIcon(null);
        treeCellRenderer.setClosedIcon(null);
        treeCellRenderer.setOpenIcon(null);

        fieldsListTableModel.setRealColumnVisibility(2, false);
        fieldsListTableModel.setRealColumnVisibility(4, false);

        if (retainedSizeSupported)
            fieldsListTableModel.setRealColumnVisibility(5, fieldsBrowserController.
                getInstancesControllerHandler().getHeapFragmentWalker().getRetainedSizesStatus()
                                          == HeapFragmentWalker.RETAINED_SIZES_COMPUTED);

        fieldsListTable = new JTreeTable(fieldsListTableModel) {
                public void doLayout() {
                    int columnsWidthsSum = 0;
                    int realFirstColumn = -1;

                    TableColumnModel colModel = getColumnModel();

                    for (int i = 0; i < fieldsListTableModel.getColumnCount(); i++) {
                        if (fieldsListTableModel.getRealColumn(i) == 0) {
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
        fieldsListTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fieldsListTable.setGridColor(UIConstants.TABLE_VERTICAL_GRID_COLOR);
        fieldsListTable.setSelectionBackground(UIConstants.TABLE_SELECTION_BACKGROUND_COLOR);
        fieldsListTable.setSelectionForeground(UIConstants.TABLE_SELECTION_FOREGROUND_COLOR);
        fieldsListTable.setShowHorizontalLines(UIConstants.SHOW_TABLE_HORIZONTAL_GRID);
        fieldsListTable.setShowVerticalLines(UIConstants.SHOW_TABLE_VERTICAL_GRID);
        fieldsListTable.setRowMargin(UIConstants.TABLE_ROW_MARGIN);
        fieldsListTable.setRowHeight(UIUtils.getDefaultRowHeight() + 2);
        fieldsListTable.getTree().setLargeModel(true);
        fieldsListTable.getTree().setToggleClickCount(0);
        fieldsListTable.getColumnModel().getColumn(0).setMinWidth(150);
        fieldsListTable.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                       .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "DEFAULT_ACTION"); // NOI18N
        fieldsListTable.getActionMap().put("DEFAULT_ACTION", // NOI18N
                                           new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    performDefaultAction();
                }
            });

        // Disable traversing table cells using TAB and Shift+TAB
        Set keys = new HashSet(fieldsListTable.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS));
        keys.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0));
        fieldsListTable.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, keys);

        keys = new HashSet(fieldsListTable.getFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS));
        keys.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_MASK));
        fieldsListTable.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, keys);

        setColumnsData();

        tablePopup = createTablePopup();

        cornerPopup = new JPopupMenu();

        JTreeTablePanel tablePanel = new JTreeTablePanel(fieldsListTable);
        tablePanel.setCorner(JScrollPane.UPPER_RIGHT_CORNER, createHeaderPopupCornerButton(cornerPopup));

        dataPanel = new JPanel(new BorderLayout());
        dataPanel.add(tablePanel, BorderLayout.CENTER);

        noDataPanel = new JPanel(new BorderLayout());
        noDataPanel.setBorder(BorderFactory.createLoweredBevelBorder());

        HTMLTextArea hintArea = new HTMLTextArea();
        hintArea.setBorder(BorderFactory.createEmptyBorder(10, 8, 8, 8));

        String instancesRes = Icons.getResource(HeapWalkerIcons.INSTANCES);
        String classesRes = Icons.getResource(HeapWalkerIcons.CLASSES);
        String hintText = (fieldsBrowserController.getRootMode() == FieldsBrowserController.ROOT_INSTANCE)
                          ? Bundle.FieldsBrowserControllerUI_NoInstanceSelectedMsg(
                                "<img border='0' align='bottom' src='nbresloc:/" + instancesRes + "'>") // NOI18N
                          : Bundle.FieldsBrowserControllerUI_NoClassSelectedMsg(
                                 "<img border='0' align='bottom' src='nbresloc:/" + classesRes + "'>"); // NOI18N

        hintArea.setText(hintText);
        noDataPanel.add(hintArea, BorderLayout.CENTER);

        contents = new CardLayout();
        setLayout(contents);
        add(noDataPanel, NO_DATA);
        add(dataPanel, DATA);

        fieldsListTable.addMouseListener(new FieldsListTableMouseListener());
        fieldsListTable.addKeyListener(new FieldsListTableKeyListener());
    }

    // --- Private implementation ------------------------------------------------
    private void initData() {
        fieldsListTableModel.setRoot(fieldsBrowserController.getFilteredSortedFields(filterValue, sortingColumn, sortingOrder));
        refreshView();
    }

    private void navigateToInstance(Instance instance) {
        fieldsBrowserController.navigateToInstance(instance);
    }

    private void performDefaultAction() {
        int row = fieldsListTable.getSelectedRow();

        if (row != -1) {
            HeapWalkerNode node = (HeapWalkerNode) fieldsListTable.getTree().getPathForRow(row).getLastPathComponent();

            if (!node.isRoot() && node instanceof HeapWalkerInstanceNode && ((HeapWalkerInstanceNode) node).hasInstance()) {
                if (fieldsBrowserController.getRootMode() == FieldsBrowserController.ROOT_INSTANCE) {
                    fieldsBrowserController.createNavigationHistoryPoint();
                }

                HeapWalkerInstanceNode instanceNode = (HeapWalkerInstanceNode) node;
                fieldsBrowserController.navigateToInstance(instanceNode.getInstance());
            }
        }
    }

    private void saveColumnsData() {
        TableColumnModel colModel = fieldsListTable.getColumnModel();

        for (int i = 0; i < fieldsListTableModel.getColumnCount(); i++) {
            int index = fieldsListTableModel.getRealColumn(i);

            if (index != 0) {
                columnWidths[index - 1] = colModel.getColumn(i).getPreferredWidth();
            }
        }
    }

    private void showColumnSelectionPopup(final JPopupMenu headerPopup, final JButton cornerButton) {
        initColumnSelectorItems();
        headerPopup.show(cornerButton, cornerButton.getWidth() - headerPopup.getPreferredSize().width, cornerButton.getHeight());
    }

    private void showPopupMenu(int row, int x, int y) {
        HeapWalkerNode node = (HeapWalkerNode) fieldsListTable.getTree().getPathForRow(row).getLastPathComponent();

        // Show Loop Origin
        if (node instanceof HeapWalkerInstanceNode && ((HeapWalkerInstanceNode) node).isLoop()) {
            showLoopOriginItem.setVisible(true);
        } else {
            showLoopOriginItem.setVisible(false);
        }

        // Show Instance
        if (node.isRoot()) {
            showInstanceItem.setEnabled(false);
            copyPathFromRootItem.setEnabled(false);
        } else {
            showInstanceItem.setEnabled(node instanceof HeapWalkerInstanceNode && ((HeapWalkerInstanceNode) node).hasInstance());
            copyPathFromRootItem.setEnabled(true);
        }

        // Show in Classes View
        showClassItem.setEnabled(node instanceof HeapWalkerInstanceNode && ((HeapWalkerInstanceNode) node).hasInstance());

        // Go To Source
        if (showSourceItem != null) {
            String className = null;
            if(node instanceof ClassNode) {
                className = ((ClassNode)node).getJavaClass().getName();
            } else if (node instanceof HeapWalkerInstanceNode) {
                HeapWalkerInstanceNode instanceNode = (HeapWalkerInstanceNode)node;
                if(instanceNode.hasInstance()) {
                    className = instanceNode.getInstance().getJavaClass().getName();
                }
            }
            showSourceItem.setEnabled(className != null && !BrowserUtils.isPrimitiveType(BrowserUtils.getArrayBaseType(className)));
        }

        if ((x == -1) || (y == -1)) {
            Rectangle rowBounds = fieldsListTable.getCellRect(row, 0, true);

            if (x == -1) {
                x = rowBounds.x + (rowBounds.width / 2);
            }

            if (y == -1) {
                y = rowBounds.y + (rowBounds.height / 2);
            }
        }

        tablePopup.show(fieldsListTable, x, y);
    }
}
