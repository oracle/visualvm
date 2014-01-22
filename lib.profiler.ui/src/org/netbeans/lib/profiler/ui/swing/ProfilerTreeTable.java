/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.SortOrder;
import javax.swing.UIManager;
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
        
        setDefaultRenderer(String.class, new LabelRenderer());
    }
    
    
    public void setRowHeight(int rowHeight) {
        super.setRowHeight(rowHeight);
        if (tree != null) tree.setRowHeight(rowHeight);
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
        if (e != null) {
            Point point = e.getPoint();
            int column = columnAtPoint(point);
            
            if (getColumnClass(column) == JTree.class) {
                int row = rowAtPoint(point);
                Rectangle treeCellRect = tree.getRowBounds(row);
                
                if (treeCellRect != null) {
                    Rectangle tableCellRect = getCellRect(row, column, true);
                    int treeX = point.x - tableCellRect.x;
                    if (treeX > treeCellRect.x) treeX = treeCellRect.x + treeCellRect.width / 2;
                    MouseEvent newEvent = new MouseEvent(tree, e.getID(), e.getWhen(),
                                                         e.getModifiers(), treeX, e.getY(),
                                                         e.getClickCount(), e.isPopupTrigger());
                    tree.dispatchEvent(newEvent);
                }
            }
        }
        
        super.processMouseEvent(e);
    }
    
    
    protected TableRowSorter createRowSorter() {
        ProfilerRowSorter s = new ProfilerTreeTableSorter(getModel());
        s.setDefaultSortOrder(SortOrder.DESCENDING);
        s.setDefaultSortOrder(0, SortOrder.ASCENDING);
        s.setSortColumn(0);
        return s;
    }
    
    private static class ProfilerTreeTableSorter extends ProfilerRowSorter {
        
        private TableModelImpl model;
        private List<SortKey> sortKeys;
        
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
//            fireSortOrderChanged();
        }
        
        public List<? extends SortKey> getSortKeys() {
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
        private final TreeModelImpl treeModel;
        private final ProfilerTreeTableModel treeTableModel;
        
        TableModelImpl(ProfilerTreeTableModel model) {
            this.treeTableModel = model;
            treeModel = new TreeModelImpl(model.getRoot()) {
                protected void fireTreeStructureChanged(Object source, Object[] path,
                                        int[] childIndices,
                                        Object[] children) {
                    UIState uiState = tree == null ? null : getUIState(tree);
                    super.fireTreeStructureChanged(source, path, childIndices, children);
                    if (uiState != null) restoreUIState(tree, uiState);
                }
            };
            tree = new ProfilerTreeTableTree(treeModel);
            tree.setShowsRootHandles(false);
            for (int i = 0; i < tree.getRowCount(); i++) tree.expandRow(i);
        }
        
        
        void sort(Comparator comparator) {
            treeModel.sort(comparator);
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
        
        
        TreeModelImpl(TreeNode root) {
            super(root);
        }
        
        
        void sort(Comparator comp) {
            comparator = comp;
            viewToModel = null;
            reload();
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
            
            int[] indexes = viewToModel.get(parent);
            if (indexes == null) {
                Object[] children = new Object[super.getChildCount(parent)];
                for (int i = 0; i < children.length; i++)
                    children[i] = super.getChild(parent, i);
                Arrays.sort(children, comparator);
                indexes = new int[children.length];
                for (int i = 0; i < indexes.length; i++)
                    indexes[i] = super.getIndexOfChild(parent, children[i]);
                viewToModel.put(parent, indexes);
            }
            
            return indexes;
        }
        
    }
    
    
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
            while (paths.hasMoreElements()) tree.expandPath(paths.nextElement());
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
        
        public void treeExpanded(TreeExpansionEvent event) {
            model.fireTableDataChanged();
        }

        public void treeCollapsed(TreeExpansionEvent event) {
            model.fireTableDataChanged();
        }

        public void treeNodesChanged(TreeModelEvent e) {
            model.fireTableDataChanged();
        }

        public void treeNodesInserted(TreeModelEvent e) {
            model.fireTableDataChanged();
        }

        public void treeNodesRemoved(TreeModelEvent e) {
            model.fireTableDataChanged();
        }

        public void treeStructureChanged(TreeModelEvent e) {
            model.fireTableDataChanged();
        }

        public void valueChanged(TreeSelectionEvent e) {
            if (internal) return;
            
            TreePath selected = e.getPath();
            int row = selected == null ? -1 : tree.getRowForPath(selected);
            try {
                internal = true;
                if (row != -1) selectRow(row, true);
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
        
        private static BasicTreeUI SIMPLE_SYNTH_UI;

        private int currentX;
        private int currentWidth;
        
        private int currentRowOffset;
        private boolean currentFirst;
        private boolean currentSelected;
        
        private boolean customRendering;

        
        ProfilerTreeTableTree(TreeModel model) {
            super(model);
            
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder());
            getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
            
            setCellRenderer(createCellRenderer(new LabelRenderer()));
        }
        
        public void setUI(TreeUI ui) {
            if (ui instanceof SynthTreeUI) {
                if (SIMPLE_SYNTH_UI == null) {
                    super.setUI(ui);
                    SynthTreeUI synthUI = (SynthTreeUI)ui;
                    int left = synthUI.getLeftChildIndent();
                    int right = synthUI.getRightChildIndent();

                    SIMPLE_SYNTH_UI = new SimpleSynthTreeUI();
                    super.setUI(SIMPLE_SYNTH_UI);

                    SIMPLE_SYNTH_UI.setLeftChildIndent(left + 4);
                    SIMPLE_SYNTH_UI.setRightChildIndent(right);
                } else {
                    super.setUI(SIMPLE_SYNTH_UI);
                }
            } else {
                super.setUI(ui);
            }
        }
        
        static TreeCellRenderer createCellRenderer(final ProfilerRenderer renderer) {
            return new TreeCellRenderer() {
                public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                    renderer.setValue(value, row);
                    JComponent comp = renderer.getComponent();
                    comp.setOpaque(false);
                    comp.setForeground(tree.getForeground());
                    ((JLabel)comp).setIcon(UIManager.getIcon("Tree.openIcon"));
                    return comp;
                }
            };
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
            
            return this;
        }
        
        public Dimension getPreferredSize() {
            return new Dimension(currentX + currentWidth, rowHeight);
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
    }
    
    
    private static class SimpleSynthTreeUI extends BasicTreeUI {
            
        private static Icon COLLAPSED;
        private static Icon EXPANDED;
        
        
        protected void paintHorizontalPartOfLeg(Graphics g, Rectangle clipBounds,
                                        Insets insets, Rectangle bounds,
                                        TreePath path, int row,
                                        boolean isExpanded,
                                        boolean hasBeenExpanded, boolean
                                        isLeaf) {}

        protected void paintVerticalPartOfLeg(Graphics g, Rectangle clipBounds,
                              Insets insets, TreePath path) {}
        
        
        public Icon getCollapsedIcon() {
            if (COLLAPSED == null)
                COLLAPSED = simpleSynthIcon(UIManager.getIcon("Tree.collapsedIcon")); // NOI18N
            return COLLAPSED;
        }

        public Icon getExpandedIcon() {
            if (EXPANDED == null)
                EXPANDED = simpleSynthIcon(UIManager.getIcon("Tree.expandedIcon")); // NOI18N
            return EXPANDED;
        }

        private static Icon simpleSynthIcon(Icon icon) {
            JLabel l = new JLabel(icon, JLabel.LEADING);
            l.setBorder(BorderFactory.createEmptyBorder());
            l.setSize(l.getMinimumSize());
            BufferedImage i = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(),
                                                BufferedImage.TYPE_INT_ARGB);
            l.paint(i.getGraphics());
            return new ImageIcon(i);
        }

    }
    
}
