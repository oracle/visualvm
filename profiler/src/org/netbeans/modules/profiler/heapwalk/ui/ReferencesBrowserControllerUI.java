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
import org.netbeans.lib.profiler.heap.GCRoot;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.JavaFrameGCRoot;
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
import org.netbeans.modules.profiler.NetBeansProfiler;
import org.netbeans.modules.profiler.heapwalk.ReferencesBrowserController;
import org.netbeans.modules.profiler.heapwalk.model.HeapWalkerFieldNode;
import org.netbeans.modules.profiler.heapwalk.model.HeapWalkerInstanceNode;
import org.netbeans.modules.profiler.heapwalk.model.HeapWalkerNode;
import org.netbeans.modules.profiler.heapwalk.model.InstanceNode;
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
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;
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
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.TreePath;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.results.CCTNode;
import org.netbeans.modules.profiler.heapwalk.HeapFragmentWalker;
import org.netbeans.modules.profiler.heapwalk.HeapFragmentWalker.StateEvent;
import org.netbeans.modules.profiler.heapwalk.model.HeapWalkerNodeFactory;
import org.openide.util.RequestProcessor;


/**
 *
 * @author Tomas Hurka
 * @author Jiri Sedlacek
 */
public class ReferencesBrowserControllerUI extends JTitledPanel {
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
            super(ReferencesBrowserController.EMPTY_INSTANCE_NODE);
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

    // -----
    // I18N String constants
    private static final String VIEW_TITLE_REFERENCES = NbBundle.getMessage(ReferencesBrowserControllerUI.class,
                                                                            "ReferencesBrowserControllerUI_ViewTitleReferences"); // NOI18N
    private static final String NO_INSTANCE_SELECTED_MSG = NbBundle.getMessage(ReferencesBrowserControllerUI.class,
                                                                               "ReferencesBrowserControllerUI_NoInstanceSelectedMsg"); // NOI18N
    private static final String SHOW_LOOP_ITEM_TEXT = NbBundle.getMessage(ReferencesBrowserControllerUI.class,
                                                                          "ReferencesBrowserControllerUI_ShowLoopItemText"); // NOI18N
    private static final String SHOW_INSTANCE_ITEM_TEXT = NbBundle.getMessage(ReferencesBrowserControllerUI.class,
                                                                              "ReferencesBrowserControllerUI_ShowInstanceItemText"); // NOI18N
    private static final String SHOW_IN_CLASSES_ITEM_TEXT = NbBundle.getMessage(ReferencesBrowserControllerUI.class,
                                                                                "ReferencesBrowserControllerUI_ShowInClassesItemText"); // NOI18N
    private static final String SHOW_GCROOT_ITEM_TEXT = NbBundle.getMessage(ReferencesBrowserControllerUI.class,
                                                                            "ReferencesBrowserControllerUI_ShowGcRootItemText"); // NOI18N
    private static final String GO_TO_SOURCE_ITEM_TEXT = NbBundle.getMessage(ReferencesBrowserControllerUI.class,
                                                                             "ReferencesBrowserControllerUI_GoToSourceItemText"); // NOI18N
    private static final String SHOW_IN_THREADS_ITEM_TEXT = NbBundle.getMessage(ReferencesBrowserControllerUI.class,
                                                                             "ReferencesBrowserControllerUI_ShowInThreadsItemText"); // NOI18N
    private static final String SHOW_HIDE_COLUMNS_STRING = NbBundle.getMessage(ReferencesBrowserControllerUI.class,
                                                                               "ReferencesBrowserControllerUI_ShowHideColumnsString"); // NOI18N
    private static final String FIELD_COLUMN_NAME = NbBundle.getMessage(ReferencesBrowserControllerUI.class,
                                                                        "ReferencesBrowserControllerUI_FieldColumnName"); // NOI18N
    private static final String FIELD_COLUMN_DESCR = NbBundle.getMessage(ReferencesBrowserControllerUI.class,
                                                                         "ReferencesBrowserControllerUI_FieldColumnDescr"); // NOI18N
    private static final String TYPE_COLUMN_NAME = NbBundle.getMessage(ReferencesBrowserControllerUI.class,
                                                                       "ReferencesBrowserControllerUI_TypeColumnName"); // NOI18N
    private static final String TYPE_COLUMN_DESCR = NbBundle.getMessage(ReferencesBrowserControllerUI.class,
                                                                        "ReferencesBrowserControllerUI_TypeColumnDescr"); // NOI18N
    private static final String FULL_TYPE_COLUMN_NAME = NbBundle.getMessage(ReferencesBrowserControllerUI.class,
                                                                            "ReferencesBrowserControllerUI_FullTypeColumnName"); // NOI18N
    private static final String FULL_TYPE_COLUMN_DESCR = NbBundle.getMessage(ReferencesBrowserControllerUI.class,
                                                                             "ReferencesBrowserControllerUI_FullTypeColumnDescr"); // NOI18N
    private static final String VALUE_COLUMN_NAME = NbBundle.getMessage(ReferencesBrowserControllerUI.class,
                                                                        "ReferencesBrowserControllerUI_ValueColumnName"); // NOI18N
    private static final String VALUE_COLUMN_DESCR = NbBundle.getMessage(ReferencesBrowserControllerUI.class,
                                                                         "ReferencesBrowserControllerUI_ValueColumnDescr"); // NOI18N
    private static final String SIZE_COLUMN_NAME = NbBundle.getMessage(ReferencesBrowserControllerUI.class,
                                                                       "ReferencesBrowserControllerUI_SizeColumnName"); // NOI18N
    private static final String SIZE_COLUMN_DESCR = NbBundle.getMessage(ReferencesBrowserControllerUI.class,
                                                                        "ReferencesBrowserControllerUI_SizeColumnDescr"); // NOI18N
    private static final String RETAINED_SIZE_COLUMN_NAME = NbBundle.getMessage(ReferencesBrowserControllerUI.class,
                                                                                "ReferencesBrowserControllerUI_RetainedSizeColumnName"); // NOI18N
    private static final String RETAINED_SIZE_COLUMN_DESCR = NbBundle.getMessage(ReferencesBrowserControllerUI.class,
                                                                                 "ReferencesBrowserControllerUI_RetainedSizeColumnDescr"); // NOI18N
    // -----
    private static ImageIcon ICON_FIELDS = ImageUtilities.loadImageIcon("org/netbeans/modules/profiler/heapwalk/ui/resources/incomingRef.png", false); // NOI18N

    // --- UI definition ---------------------------------------------------------
    private static final String DATA = "Data"; // NOI18N
    private static final String NO_DATA = "No data"; // NOI18N
    private int columnCount;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private CardLayout contents;
    private FieldTreeCellRenderer treeCellRenderer = new FieldTreeCellRenderer();
    private FieldsListTreeTableModel realFieldsListTableModel;
    private ExtendedTreeTableModel fieldsListTableModel;
//    private JMenuItem showClassItem;
    private JMenuItem showGcRootItem;
    private JMenuItem showInstanceItem;
    private JMenuItem showLoopOriginItem;
    private JMenuItem showSourceItem;
    private JMenuItem showInThreadsItem;
    private JPanel dataPanel;
    private JPanel noDataPanel;
    private JPopupMenu cornerPopup;
    private JPopupMenu tablePopup;
    private JTreeTable fieldsListTable;
    private ReferencesBrowserController referencesBrowserController;
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
    public ReferencesBrowserControllerUI(ReferencesBrowserController referencesBrowserController) {
        super(VIEW_TITLE_REFERENCES, ICON_FIELDS, true);

        this.referencesBrowserController = referencesBrowserController;

        retainedSizeSupported = referencesBrowserController.getReferencesControllerHandler().
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

        referencesBrowserController.getReferencesControllerHandler().
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

    private static final int MAX_STEP = 10;

    public void selectNode(HeapWalkerNode node) {
        CCTNode[] pathArr = fieldsListTable.getPathToRoot(node);
//        System.err.println(">>> About to open path size: " + pathArr.length);

        selectPath(pathArr, Math.min(pathArr.length, MAX_STEP));

//        fieldsListTable.selectNode(node, true);
    }

    private void selectPath(final CCTNode[] path, final int length) {
        if (length >= path.length) {
            fieldsListTable.getTree().setSelectionPath(new TreePath(path));
        } else {
            Object[] shortPath = new Object[length];
            System.arraycopy(path, 0, shortPath, 0, length);
            final TreePath p = new TreePath(shortPath);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
//                    System.err.println(">>> Selecting length " + length);
                    fieldsListTable.getTree().setSelectionPath(p);
                    fieldsListTable.scrollRectToVisible(fieldsListTable.getCellRect(fieldsListTable.getSelectedRow(), 0, true));
                    selectPath(path, length + MAX_STEP);
                }
            });
        }
    }

    // --- Public interface ------------------------------------------------------
    public void update() {
        if (isShowing()) {
            initData();

            if (contents != null) { // ui already initialized

                if ((fieldsListTableModel.getRoot() == null)
                        || (fieldsListTableModel.getRoot() == ReferencesBrowserController.EMPTY_INSTANCE_NODE)) {
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
            menuItem.setActionCommand(new Integer(i).toString());
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
                    RequestProcessor.getDefault().post(new Runnable() {
                        public void run() {
                            final int retainedSizesState = referencesBrowserController.getReferencesControllerHandler().
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

        showInstanceItem = new JMenuItem(SHOW_INSTANCE_ITEM_TEXT);
        showInstanceItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    performDefaultAction();
                }
            });
        showInstanceItem.setFont(popup.getFont().deriveFont(Font.BOLD));

//        showClassItem = new JMenuItem(SHOW_IN_CLASSES_ITEM_TEXT);
//        showClassItem.addActionListener(new ActionListener() {
//                public void actionPerformed(ActionEvent e) {
//                    int row = fieldsListTable.getSelectedRow();
//
//                    if (row != -1) {
//                        HeapWalkerNode node = (HeapWalkerNode) fieldsListTable.getTree().getPathForRow(row).getLastPathComponent();
//
//                        if (node instanceof HeapWalkerInstanceNode) {
//                            HeapWalkerInstanceNode instanceNode = (HeapWalkerInstanceNode) node;
//
//                            if (instanceNode instanceof ObjectFieldNode && ((ObjectFieldNode) instanceNode).isStatic()) {
//                                referencesBrowserController.navigateToClass(((ObjectFieldNode) instanceNode).getFieldValue()
//                                                                             .getField().getDeclaringClass());
//                            } else if (instanceNode.hasInstance()) {
//                                referencesBrowserController.navigateToClass(instanceNode.getInstance().getJavaClass());
//                            }
//                        }
//                    }
//                }
//            });

        showGcRootItem = new JMenuItem(SHOW_GCROOT_ITEM_TEXT);
        showGcRootItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    int row = fieldsListTable.getSelectedRow();

                    if (row != -1) {
                        HeapWalkerNode node = (HeapWalkerNode) fieldsListTable.getTree().getPathForRow(row).getLastPathComponent();

                        if (node instanceof InstanceNode) {
                            InstanceNode instanceNode = (InstanceNode) node;

                            if (instanceNode.hasInstance()) {
                                referencesBrowserController.navigateToNearestGCRoot(instanceNode);
                            }
                        }
                    }
                }
            });

        showLoopOriginItem = new JMenuItem(SHOW_LOOP_ITEM_TEXT);
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

        showSourceItem = new JMenuItem(GO_TO_SOURCE_ITEM_TEXT);
        showSourceItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    int row = fieldsListTable.getSelectedRow();

                    if (row != -1) {
                        HeapWalkerNode node = (HeapWalkerNode) fieldsListTable.getTree().getPathForRow(row).getLastPathComponent();
                        String className = node.getType();

                        while (className.endsWith("[]")) {
                            className = className.substring(0, className.length() - 2); // NOI18N
                        }
                        Project p = referencesBrowserController.getReferencesControllerHandler().getHeapFragmentWalker().getHeapDumpProject();
                        NetBeansProfiler.getDefaultNB().openJavaSource(p, className, null, null);
                    }
                }
            });
            
        showInThreadsItem = new JMenuItem(SHOW_IN_THREADS_ITEM_TEXT);
        showInThreadsItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    int row = fieldsListTable.getSelectedRow();

                    if (row != -1) {
                        HeapWalkerNode node = (HeapWalkerNode) fieldsListTable.getTree().getPathForRow(row).getLastPathComponent();
                        if (node instanceof HeapWalkerInstanceNode) {
                            Instance instance = ((HeapWalkerInstanceNode)node).getInstance();
                            referencesBrowserController.showInThreads(instance);
                        }
                    }
                }
            });
            
        popup.add(showInstanceItem);
//        popup.add(showClassItem);
        popup.add(showGcRootItem);
        popup.add(showInThreadsItem);
        popup.addSeparator();
        popup.add(showLoopOriginItem);
        popup.add(showSourceItem);

        return popup;
    }

    private void initColumnsData() {
        columnWidths = new int[columnCount - 1]; // Width of the first column fits to width
        columnNames = new String[columnCount];
        columnToolTips = new String[columnCount];
        columnRenderers = new TableCellRenderer[columnCount];

        columnNames[0] = FIELD_COLUMN_NAME;
        columnToolTips[0] = FIELD_COLUMN_DESCR;

        columnNames[1] = TYPE_COLUMN_NAME;
        columnToolTips[1] = TYPE_COLUMN_DESCR;

        columnNames[2] = FULL_TYPE_COLUMN_NAME;
        columnToolTips[2] = FULL_TYPE_COLUMN_DESCR;

        columnNames[3] = VALUE_COLUMN_NAME;
        columnToolTips[3] = VALUE_COLUMN_DESCR;

        columnNames[4] = SIZE_COLUMN_NAME;
        columnToolTips[4] = SIZE_COLUMN_DESCR;

        if (retainedSizeSupported) {
            columnNames[5] = RETAINED_SIZE_COLUMN_NAME;
            columnToolTips[5] = RETAINED_SIZE_COLUMN_DESCR;
        }

        int unitWidth = getFontMetrics(getFont()).charWidth('W'); // NOI18N // initial width of data columns

        FieldTreeCellRenderer treeCellRenderer = new FieldTreeCellRenderer();
        treeCellRenderer.setLeafIcon(null);
        treeCellRenderer.setClosedIcon(null);
        treeCellRenderer.setOpenIcon(null);

        LabelTableCellRenderer dataCellRenderer = new LabelTableCellRenderer(JLabel.TRAILING);

        // method / class / package name
        columnRenderers[0] = null;

        columnWidths[1 - 1] = unitWidth * 18;
        columnRenderers[1] = dataCellRenderer;

        columnWidths[2 - 1] = unitWidth * 28;
        columnRenderers[2] = dataCellRenderer;

        columnWidths[3 - 1] = unitWidth * 14;
        columnRenderers[3] = dataCellRenderer;

        columnWidths[4 - 1] = unitWidth * 7;
        columnRenderers[4] = dataCellRenderer;

        if (retainedSizeSupported) {
            columnWidths[5 - 1] = unitWidth * 7;
            columnRenderers[5] = dataCellRenderer;
        }
    }

    private void initComponents() {
        treeCellRenderer.setLeafIcon(null);
        treeCellRenderer.setClosedIcon(null);
        treeCellRenderer.setOpenIcon(null);

        fieldsListTableModel.setRealColumnVisibility(2, false);
        fieldsListTableModel.setRealColumnVisibility(4, false);

        if (retainedSizeSupported)
            fieldsListTableModel.setRealColumnVisibility(5, referencesBrowserController.
                getReferencesControllerHandler().getHeapFragmentWalker().getRetainedSizesStatus()
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

        String hintText = MessageFormat.format(NO_INSTANCE_SELECTED_MSG,
                                               new Object[] {
                                                   "<img border='0' align='bottom' src='nbresloc:/org/netbeans/modules/profiler/heapwalk/ui/resources/instances.png'>" // NOI18N
                                               });
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
        fieldsListTableModel.setRoot(referencesBrowserController.getFilteredSortedReferences(filterValue, sortingColumn, sortingOrder));
        refreshView();
    }

    private void navigateToInstance(Instance instance) {
        referencesBrowserController.navigateToInstance(instance);
    }

    private void performDefaultAction() {
        int row = fieldsListTable.getSelectedRow();

        if (row != -1) {
            HeapWalkerNode node = (HeapWalkerNode) fieldsListTable.getTree().getPathForRow(row).getLastPathComponent();

            if (node instanceof HeapWalkerInstanceNode) {
                referencesBrowserController.createNavigationHistoryPoint();

                HeapWalkerInstanceNode instanceNode = (HeapWalkerInstanceNode) node;
                referencesBrowserController.navigateToInstance(instanceNode.getInstance());
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

        if (node instanceof HeapWalkerInstanceNode && ((HeapWalkerInstanceNode) node).isLoop()) {
            showLoopOriginItem.setVisible(true);
        } else {
            showLoopOriginItem.setVisible(false);
        }

        if (node.isRoot()) {
            showInstanceItem.setEnabled(false);
        } else {
            showInstanceItem.setEnabled(node instanceof HeapWalkerInstanceNode
                                        && !(node instanceof HeapWalkerFieldNode && ((HeapWalkerFieldNode) node).isStatic()));
        }

//        showClassItem.setEnabled(node instanceof HeapWalkerInstanceNode || node instanceof ClassNode);
        showGcRootItem.setEnabled(node instanceof HeapWalkerInstanceNode && (!node.currentlyHasChildren() ||
                (node.getNChildren() != 1 || !HeapWalkerNodeFactory.isMessageNode(node.getChild(0))))); // #124306
        showSourceItem.setEnabled(node instanceof HeapWalkerInstanceNode);
        showInThreadsItem.setEnabled(false);
        if (node instanceof HeapWalkerInstanceNode) {
            Instance rootInstance = ((HeapWalkerInstanceNode)node).getInstance();
            Heap heap = referencesBrowserController.getReferencesControllerHandler().getHeapFragmentWalker().getHeapFragment();
            GCRoot gcRoot = heap.getGCRoot(rootInstance);
            
            if (gcRoot != null && GCRoot.JAVA_FRAME.equals(gcRoot.getKind())) {
                // make sure that thread information is available
                JavaFrameGCRoot frameVar = (JavaFrameGCRoot) gcRoot;
                
                if (frameVar.getFrameNumber() != -1) {
                    showInThreadsItem.setEnabled(true);
                }
            }
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
