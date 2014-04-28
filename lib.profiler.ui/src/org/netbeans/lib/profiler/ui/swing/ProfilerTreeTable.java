/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2014 Oracle and/or its affiliates. All rights reserved.
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

package org.netbeans.lib.profiler.ui.swing;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.plaf.TreeUI;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.plaf.synth.SynthTreeUI;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.swing.renderer.LabelRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.ProfilerRenderer;

/**
 *
 * @author Jiri Sedlacek
 */
public class ProfilerTreeTable extends ProfilerTable {
    
    private final TableModelImpl model;
    private final ProfilerTreeTableTree tree;
    
    
    public ProfilerTreeTable(ProfilerTreeTableModel model, boolean sortable,
                             boolean hideableColums, int[] scrollableColumns) {
        super(new TableModelImpl(model), sortable, hideableColums, scrollableColumns);
        
        this.model = (TableModelImpl)getModel();
        tree = this.model.getTree();
        
        
        Adapter adapter = new Adapter();
        tree.addTreeSelectionListener(adapter);
        tree.addTreeExpansionListener(adapter);
        tree.getModel().addTreeModelListener(adapter);
        getSelectionModel().addListSelectionListener(adapter);

        tree.setRowHeight(rowHeight);
        setDefaultRenderer(JTree.class, tree);
    }
    
    
    public TreePath getPathForRow(int row) {
        return tree.getPathForRow(row);
    }
    
    
    public void setRowHeight(int rowHeight) {
        super.setRowHeight(rowHeight);
        if (tree != null) tree.setRowHeight(rowHeight);
    }
    
    
    public void setShowsRootHandles(boolean newValue) {
        if (tree != null) tree.setShowsRootHandles(newValue);
    }
    
    public void setRootVisible(boolean rootVisible) {
        if (tree != null) tree.setRootVisible(rootVisible);
    }
    
    public void makeTreeAutoExpandable(int maxChildToExpand) {
        if (tree != null) UIUtils.makeTreeAutoExpandable(tree, maxChildToExpand);
    }
    
    
    public void setCellRenderer(TreeCellRenderer renderer) {
        if (tree != null) tree.setCellRenderer(renderer);
    }
    
    public void setTreeCellRenderer(ProfilerRenderer renderer) {
        setCellRenderer(createTreeCellRenderer(renderer));
    }
    
    public static TreeCellRenderer createTreeCellRenderer(final ProfilerRenderer renderer) {
        return new TreeCellRenderer() {
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                renderer.setValue(value, row);
                JComponent comp = renderer.getComponent();
                comp.setOpaque(false);
                comp.setForeground(tree.getForeground());
                return comp;
            }
        };
    }
    
    Component getRenderer(TableCellRenderer renderer, int row, int column, boolean sized) {
        Component comp = super.getRenderer(renderer, row, column, sized);
        
        if (sized && JTree.class.equals(getColumnClass(column))) {
            Rectangle bounds = tree.getRowBounds(row);
            comp.setBounds(bounds.x, 0, bounds.width, comp.getHeight());
        }
        
        return comp;
    }
    
    
    protected void processKeyEvent(KeyEvent e) {
        tree.dispatchEvent(e);
        if (!e.isConsumed()) super.processKeyEvent(e);
    }

    protected void processMouseEvent(MouseEvent e) {
        MouseEvent treeEvent = null;
        
        if (e != null) {
            Point point = e.getPoint();
            int column = columnAtPoint(point);
            
            if (getColumnClass(column) == JTree.class) {
                int row = rowAtPoint(point);
                Rectangle treeCellRect = tree.getRowBounds(row);
                
                if (treeCellRect != null) {
                    Rectangle tableCellRect = getCellRect(row, column, true);
                    int _column = convertColumnIndexToModel(column);
                    int treeX = point.x - tableCellRect.x + getColumnOffset(_column);
                    if (treeX > treeCellRect.x) treeX = treeCellRect.x + treeCellRect.width / 2;
                    treeEvent = new MouseEvent(tree, e.getID(), e.getWhen(),
                                               e.getModifiers(), treeX, e.getY(),
                                               e.getClickCount(), e.isPopupTrigger());
                    
                    // Prevent invoking default action on doubleclick
                    e = clearClicks(e);
                }
            }
        }
        
        super.processMouseEvent(e);
        if (treeEvent != null) tree.dispatchEvent(treeEvent);
    }
    
    
    protected TableRowSorter createRowSorter() {
        ProfilerRowSorter s = new ProfilerTreeTableSorter(getModel()) {
            public void allRowsChanged() {
                // Must invoke later, JTree.getRowCount() not ready yet
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() { updateColumnsPreferredWidth(); }
                });
            }
        };
        s.setDefaultSortOrder(SortOrder.DESCENDING);
        s.setDefaultSortOrder(0, SortOrder.ASCENDING);
        s.setSortColumn(0);
        return s;
    }
    
    private static class ProfilerTreeTableSorter extends ProfilerRowSorter {
        
        private final TableModelImpl model;
        private List<RowSorter.SortKey> sortKeys;
        
        ProfilerTreeTableSorter(TableModel model) {
            super(model);
            this.model = (TableModelImpl)model;
        }
        
        public int convertRowIndexToModel(int index) {
            return index;
        }
        
        public int convertRowIndexToView(int index) {
            return index;
        }
        
        public int getViewRowCount() {
            return model.getRowCount();
        }
        
        public int getModelRowCount() {
            return model.getRowCount();
        }
        
        protected void setSortKeysImpl(List newKeys) {
            sortKeys = newKeys == null ? Collections.emptyList() :
                       Collections.unmodifiableList(new ArrayList(newKeys));
            model.sort(newKeys == null ? null : getComparator());
        }
        
        public List<? extends RowSorter.SortKey> getSortKeys() {
            return sortKeys;
        }
        
        private Comparator getComparator() {
            SortOrder sortOrder = getSortOrder();
            if (SortOrder.UNSORTED.equals(sortOrder)) return null;
            
            final boolean ascending = SortOrder.ASCENDING.equals(sortOrder);
            final int sortColumn = getSortColumn();
            boolean sortingTree = JTree.class.equals(model.getColumnClass(sortColumn));
            final Comparator comparator = sortingTree ? null : getComparator(sortColumn);
            
            return new Comparator() {
                public int compare(Object o1, Object o2) {
                    int result;
                    if (comparator == null) {
                        String s1 = o1.toString();
                        String s2 = o2.toString();
                        result = s1.compareTo(s2);
                    } else {
                        Object v1 = model.getValueAt((TreeNode)o1, sortColumn);
                        Object v2 = model.getValueAt((TreeNode)o2, sortColumn);
                        result = comparator.compare(v1, v2);
                    }
                    
                    return ascending ? result : result * -1;
                }
            };
        }
        
    }
    
    
    private static class TableModelImpl extends AbstractTableModel {
        
        private final ProfilerTreeTableTree tree;
        private TreeModelImpl treeModel;
        private final ProfilerTreeTableModel treeTableModel;
        
        TableModelImpl(ProfilerTreeTableModel model) {
            this.treeTableModel = model;
            
            treeModel = treeModelImpl(model.getRoot(), null);
            
            model.addListener(new ProfilerTreeTableModel.Adapter() {
                public void dataChanged() {
                    fireTableDataChanged();
                }
                public void rootChanged(TreeNode oldRoot, TreeNode newRoot) {
                    // NOTE: would be cleaner to change root of existing model,
                    //       wasn't able to easily resolve all related problems.
//                    treeModel.setRoot(newRoot);
                    
                    tree.setChangingModel(true);
                    
                    try {
                        UIState uiState = getUIState(tree);

                        Comparator comparator = treeModel != null ? treeModel.getComparator() : null;
                        treeModel = treeModelImpl(newRoot, comparator);
                        tree.setModel(treeModel);
                        fireTableDataChanged();

                        if (uiState != null) restoreUIState(tree, uiState);
                    } finally {
                        tree.setChangingModel(false);
                    }
                }
            });
                    
            tree = new ProfilerTreeTableTree(treeModel);
        }
        
        private TreeModelImpl treeModelImpl(TreeNode root, Comparator comparator) {
            return new TreeModelImpl(root, comparator) {
                protected void fireTreeStructureChanged(Object source, Object[] path,
                                        int[] childIndices,
                                        Object[] children) {
                    UIState uiState = tree == null ? null : getUIState(tree);
                    super.fireTreeStructureChanged(source, path, childIndices, children);
                    if (uiState != null) restoreUIState(tree, uiState);
                    fireTableDataChanged();
                }
            };
        }
        
        
        void sort(Comparator comparator) {
            treeModel.setComparator(comparator);
        }
        
        
        ProfilerTreeTableTree getTree() {
            return tree;
        }
        
        TreeNode nodeForRow(int rowIndex) {
            TreePath path = tree.getPathForRow(rowIndex);
            return path == null ? null : (TreeNode)path.getLastPathComponent();
        }

        public int getRowCount() {
            return tree.getRowCount();
        }

        public int getColumnCount() {
            return treeTableModel.getColumnCount();
        }
        
        public String getColumnName(int columnIndex) {
            return treeTableModel.getColumnName(columnIndex);
        }
        
        public Class getColumnClass(int columnIndex) {
            return treeTableModel.getColumnClass(columnIndex);
        }
        
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return treeTableModel.isCellEditable(nodeForRow(rowIndex), columnIndex);
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            return treeTableModel.getValueAt(nodeForRow(rowIndex), columnIndex);
        }
        
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            treeTableModel.setValueAt(aValue, nodeForRow(rowIndex), columnIndex);
        }
        
        Object getValueAt(TreeNode node, int column) {
            return treeTableModel.getValueAt(node, column);
        }
        
    }
    
    
    private static class TreeModelImpl extends DefaultTreeModel {
        
        private Comparator comparator;
        private Map<Object, int[]> viewToModel;
        
        
        TreeModelImpl(TreeNode root, Comparator comp) {
            super(root);
            comparator = comp;
        }
        
        
        void setComparator(Comparator comp) {
            comparator = comp;
            viewToModel = null;
            reload();
        }
        
        Comparator getComparator() {
            return comparator;
        }
        
        
        public Object getChild(Object parent, int index) {
            if (comparator == null) return super.getChild(parent, index);
            return super.getChild(parent, viewToModel(parent)[index]);
        }
        
        public int getIndexOfChild(Object parent, Object child) {
            if (comparator == null) return super.getIndexOfChild(parent, child);
            
            int index = super.getIndexOfChild(parent, child);
            int[] indexes = viewToModel(parent);
            for (int i = 0; i < indexes.length; i++)
                if (indexes[i] == index) return i;
            
            return -1;
        }
        
        
        private int[] viewToModel(Object parent) {
            if (viewToModel == null) viewToModel = new HashMap();
            
            TreePath parentPath = new TreePath(getPathToRoot((TreeNode)parent));
            int[] indexes = viewToModel.get(parentPath);
            
            if (indexes == null) {
                Object[] children = new Object[super.getChildCount(parent)];
                for (int i = 0; i < children.length; i++)
                    children[i] = super.getChild(parent, i);
                Arrays.sort(children, comparator);
                indexes = new int[children.length];
                for (int i = 0; i < indexes.length; i++)
                    indexes[i] = super.getIndexOfChild(parent, children[i]);
                viewToModel.put(parentPath, indexes);
            }
            
            return indexes;
        }
        
    }
    
    
    protected void saveSelection() {}
    
    protected void restoreSelection() {}
    
    static UIState getUIState(JTree tree) {
        TreePath[] selectedPaths = tree.getSelectionPaths();
        TreePath rootPath = new TreePath(tree.getModel().getRoot());
        Enumeration<TreePath> expandedPaths = tree.getExpandedDescendants(rootPath);
        return new UIState(selectedPaths, expandedPaths);
    }
    
    static void restoreUIState(JTree tree, UIState uiState) {
        try {
            tree.putClientProperty(UIUtils.PROP_EXPANSION_TRANSACTION, Boolean.TRUE);
            Enumeration<TreePath> paths = uiState.getExpandedPaths();
            if (paths != null) while (paths.hasMoreElements())
                tree.expandPath(paths.nextElement());
        } finally {
            tree.putClientProperty(UIUtils.PROP_EXPANSION_TRANSACTION, null);
        }
        tree.setSelectionPaths(uiState.getSelectedPaths());
    }
    
    
    static class UIState {
        
        private final TreePath[] selectedPaths;
        private final Enumeration<TreePath> expandedPaths;
        
        
        UIState(TreePath[] selectedPaths, Enumeration<TreePath> expandedPaths) {
            this.selectedPaths = selectedPaths;
            this.expandedPaths = expandedPaths;
        }
        
        
        public TreePath[] getSelectedPaths() {
            return selectedPaths;
        }
        
        public Enumeration getExpandedPaths() {
            return expandedPaths;
        }
        
    }
    
    
    private class Adapter implements TreeModelListener, TreeExpansionListener,
                                     TreeSelectionListener, ListSelectionListener {
        
        private boolean internal;
        
        public void treeExpanded(TreeExpansionEvent event) { notifyTable(); }

        public void treeCollapsed(TreeExpansionEvent event) { notifyTable(); }

        public void treeNodesChanged(TreeModelEvent e) { notifyTable(); }

        public void treeNodesInserted(TreeModelEvent e) { notifyTable(); }

        public void treeNodesRemoved(TreeModelEvent e) { notifyTable(); }

        public void treeStructureChanged(TreeModelEvent e) { notifyTable(); }
        
        private void notifyTable() {
            if (tree.isChangingModel()) return;
            if (tree.getClientProperty(UIUtils.PROP_EXPANSION_TRANSACTION) != null) return;
            
            TreePath[] selectedPaths = tree.getSelectionPaths();
            model.fireTableDataChanged();
            tree.setSelectionPaths(selectedPaths);
        }

        public void valueChanged(TreeSelectionEvent e) {
            if (internal) return;
            
            TreePath selected = e.getPath();
            int row = selected == null ? -1 : tree.getRowForPath(selected);
            try {
                internal = true;
                if (row != -1) selectRow(row, !tree.isChangingModel());
                else clearSelection();
            } finally {
                internal = false;
            }
        }

        public void valueChanged(ListSelectionEvent e) {
            if (internal) return;
            
            int row = getSelectedRow();
            try {
                internal = true;
                if (row != -1) tree.setSelectionRow(row);
                else tree.clearSelection();
                repaint(); // TODO: optimize, do not repaint all
            } finally {
                internal = false;
            }
        }
        
    }
    
    
    private static class ProfilerTreeTableTree extends JTree implements TableCellRenderer {
        
        private int currentX;
        private int currentWidth;
        
        private int currentRowOffset;
        private boolean currentFirst;
        private boolean currentSelected;
        
        private boolean customRendering;
        
        private SynthLikeTreeUI synthLikeUI;

        
        ProfilerTreeTableTree(TreeModelImpl model) {
            super(model);
            
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder());
            getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
            
            setCellRenderer(createTreeCellRenderer(new LabelRenderer()));
            
            setLargeModel(true);
        }
        
        public void setUI(TreeUI ui) {
            if (ui instanceof SynthTreeUI) {
                if (synthLikeUI == null) {
                    super.setUI(ui);
                    SynthTreeUI synthUI = (SynthTreeUI)ui;
                    int left = synthUI.getLeftChildIndent();
                    int right = synthUI.getRightChildIndent();

                    synthLikeUI = new SynthLikeTreeUI();
                    super.setUI(synthLikeUI);

                    boolean nimbus = UIUtils.isNimbusLookAndFeel();
                    synthLikeUI.setLeftChildIndent(left + (nimbus ? 4 : 6));
                    synthLikeUI.setRightChildIndent(right);
                } else {
                    super.setUI(synthLikeUI);
                }
            } else {
                super.setUI(ui);
            }
        }

        
        // Overridden for performance reasons.
        public void validate() {}

        // Overridden for performance reasons.
        public void revalidate() {}

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row,
                                                       int column) {
            currentRowOffset = row * rowHeight;
            currentFirst = column == 0 || isFirstColumn(table.getColumnModel(), column);
            currentSelected = isSelected;
            
            Rectangle cellBounds = getRowBounds(row);
            currentX = cellBounds.x;
            currentWidth = cellBounds.width;
            
            customRendering = ((ProfilerTable)table).isCustomRendering();
            if (synthLikeUI != null) synthLikeUI.setSelected(isSelected);
            
            return this;
        }
        
        private final Dimension prefSize = new Dimension();
        public Dimension getPreferredSize() {
            prefSize.setSize(currentX + currentWidth, rowHeight);
            return prefSize;
        }
        
        public void paint(Graphics g) {
            g.setColor(getBackground());
            int rectX = currentSelected || customRendering || !currentFirst ? 0 : currentX;
            g.fillRect(rectX, 0, getWidth() - rectX, rowHeight);
            
            g.translate(customRendering ? -currentX : 0, -currentRowOffset);
            super.paint(g);
        }
        
        private boolean isFirstColumn(TableColumnModel columns, int column) {
            int x = 0;
            for (int i = 0; i < column; i++) x += columns.getColumn(i).getWidth();
            return x == 0;
        }
        
        public void expandPath(TreePath path) {
            if (changingModel) path = getSimilarPath(path);
            super.expandPath(path);
        }
        
        public void setSelectionPath(TreePath path) {
            if (changingModel) path = getSimilarPath(path);
            super.setSelectionPath(path);
        }
        
        public void setSelectionPaths(TreePath[] paths) {
            if (changingModel && paths != null)
                for (int i = 0; i < paths.length; i++)
                    paths[i] = getSimilarPath(paths[i]);
            super.setSelectionPaths(paths);
        }
        
        private TreePath getSimilarPath(TreePath oldPath) {
            if (oldPath == null || oldPath.getPathCount() < 1) return null;

            TreeModel currentModel = getModel();
            Object currentRoot = currentModel.getRoot();
            if (!currentRoot.equals(oldPath.getPathComponent(0))) return null;

            TreePath p = new TreePath(currentRoot);
            Object[] op = oldPath.getPath();
            Object n = currentRoot;

            for (int i = 1; i < op.length; i++) {
                Object nn = null;

                for (int ii = 0; ii < currentModel.getChildCount(n); ii++) {
                    Object c = currentModel.getChild(n, ii);
                    if (c.equals(op[i])) {
                        nn = c;
                        break;
                    }
                }

                if (nn == null) return null;

                n = nn;
                p = p.pathByAddingChild(n);
            }

            return p;
        }
        
        private boolean changingModel;
        
        private void setChangingModel(boolean changing) {
            changingModel = changing;
        }
        
        boolean isChangingModel() {
            return changingModel;
        }
        
    }
    
    
    private static class SynthLikeTreeUI extends BasicTreeUI {
        
        private static final Icon[] ICONS = new Icon[4];
        
        static {
            
            final BufferedImage[] image = new BufferedImage[1];
            BufferedImage tmp = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
            
            DefaultMutableTreeNode root = new DefaultMutableTreeNode();
            root.add(new DefaultMutableTreeNode());
            
            JTree tree = new JTree(root);
            tree.setRootVisible(true);
            tree.setShowsRootHandles(true);
            tree.setSize(50, 50);
            
            tree.setUI(new SynthTreeUI() {
                protected void drawCentered(Component c, Graphics graphics, Icon icon,
                                        int x, int y) {
                    int w = icon.getIconWidth();
                    int h = icon.getIconHeight();
                    image[0] = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    super.drawCentered(c, image[0].getGraphics(), icon, w / 2, h / 2);
                }
            });
            
            // Expanded
            tree.expandRow(0);
            tree.clearSelection();
            tree.paint(tmp.getGraphics());
            ICONS[0] = new ImageIcon(image[0]);
            
            // Expanded selected
            tree.expandRow(0);
            tree.setSelectionRow(0);
            tree.paint(tmp.getGraphics());
            ICONS[1] = new ImageIcon(image[0]);
            
            // Collapsed
            tree.collapseRow(0);
            tree.clearSelection();
            tree.paint(tmp.getGraphics());
            ICONS[2] = new ImageIcon(image[0]);
            
            // Collapsed selected
            tree.collapseRow(0);
            tree.setSelectionRow(0);
            tree.paint(tmp.getGraphics());
            ICONS[3] = new ImageIcon(image[0]);
            
        }
        
            
        private boolean isSelected;
        
        void setSelected(boolean selected) { isSelected = selected; }
        
        public Icon getExpandedIcon() { return isSelected ? ICONS[1] : ICONS[0]; }
        
        public Icon getCollapsedIcon() { return isSelected ? ICONS[3] : ICONS[2]; }
        
        protected void paintHorizontalPartOfLeg(Graphics g, Rectangle clipBounds,
                                        Insets insets, Rectangle bounds,
                                        TreePath path, int row, boolean isExpanded,
                                        boolean hasBeenExpanded, boolean isLeaf) {}

        protected void paintVerticalPartOfLeg(Graphics g, Rectangle clipBounds,
                                        Insets insets, TreePath path) {}

    }
    
}
