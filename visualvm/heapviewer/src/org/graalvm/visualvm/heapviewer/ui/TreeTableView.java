/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.graalvm.visualvm.heapviewer.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumn;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTableContainer;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTreeTableModel;
import org.graalvm.visualvm.lib.ui.swing.renderer.LabelRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.NumberRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.ProfilerRenderer;
import org.graalvm.visualvm.lib.profiler.api.ActionsSupport;
import org.graalvm.visualvm.heapviewer.HeapContext;
import org.graalvm.visualvm.heapviewer.model.DataType;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNode;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNodeFilter;
import org.graalvm.visualvm.heapviewer.model.NodesCache;
import org.graalvm.visualvm.heapviewer.model.Progress;
import org.graalvm.visualvm.heapviewer.model.RootNode;
import javax.swing.RowSorter;
import org.graalvm.visualvm.heapviewer.utils.HeapUtils;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "TreeTableView_FilteredFlag={0} (filtered)",
    "TreeTableView_Filter=Filter",
    "TreeTableView_Pin=Pin",
    "TreeTableView_ResetPin=Reset Pin",
    "TreeTableView_SortToGet=sort to get"
})
public class TreeTableView {
    
    private final String viewID;
    
    private final HeapContext context;
    private final HeapViewerActions actions;
    
    private final List<TreeTableViewColumn> columns;
    private final Root root;
    private HeapViewerNode currentRoot;
    
    private Model model;
    private NodesCache nodesCache;
    private HeapViewerTreeTable treeTable;
    
    private List<DataType> dataTypes;
    private List<SortOrder> sortOrders;
    
    private Collection<HeapViewerRenderer.Provider> rendererProviders;
    private TreeTableViewRenderer nodesRenderer;
    
    private Collection<HeapViewerNodeAction.Provider> actionProviders;
    
    private JComponent component;
    private BreadCrumbsNavigator navigator;
    
    private HeapViewerNodeFilter filter;
    private JComponent filterComponent;
    
    private boolean hasSelection;
    
    private DataType initialSortColumn;
    private SortOrder initialSortOrder;
    
    private ColumnConfiguration initialColumnConfiguration;
    private ColumnConfiguration currentColumnConfiguration;
    
    
    public TreeTableView(String viewID, HeapContext context, HeapViewerActions actions, TreeTableViewColumn... columns) {
        this(viewID, context, actions, false, true, columns);
    }
    
    public TreeTableView(String viewID, HeapContext context, HeapViewerActions actions, boolean useBreadCrumbs, boolean pluggableColumns, TreeTableViewColumn... columns) {
        assert(!SwingUtilities.isEventDispatchThread());
        
        if (columns == null || columns.length == 0) throw new IllegalArgumentException("View must have at least one column defined"); // NOI18N
        
        this.viewID = viewID;
        this.context = context;
        this.actions = actions;
        
        root = new Root() {
            public HeapContext getContext() { return TreeTableView.this.context; }
            public String getViewID() { return TreeTableView.this.viewID; }
            public HeapViewerNodeFilter getViewFilter() { return TreeTableView.this.filter; };
            public List<DataType> getDataTypes() { return dataTypes; }
            public List<SortOrder> getSortOrders() { return sortOrders; }
            public void refreshNode(HeapViewerNode node) { if (treeTable != null) treeTable.repaint(); };
            public void updateChildren(HeapViewerNode node) { if (model != null) model.childrenChanged(root); childrenChanged(); /*if (treeTable != null) treeTable.resetExpandedNodes();*/ }
            protected HeapViewerNode[] retrieveChildren(HeapViewerNode node) { return nodesCache.retrieveChildren(node); }
            protected HeapViewerNode[] lazilyComputeChildren(Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException { return TreeTableView.this.computeData(root, heap, viewID, viewFilter, dataTypes, sortOrders, progress); }
            public HeapViewerRenderer resolveRenderer(HeapViewerNode node) { return TreeTableView.this.resolveNodeRenderer(node); }
            protected void handleOOME(OutOfMemoryError e) { super.handleOOME(e); TreeTableView.this.handleOOME(e); }
        };
        currentRoot = root;
        
        this.columns = new ArrayList();
        
        // Add own columns defined in constructor
        if (columns != null) Collections.addAll(this.columns, columns);
        
        // Add additional columns defined by plugins
        if (pluggableColumns) {
            Heap heap = context.getFragment().getHeap();
            for (TreeTableViewColumn.Provider provider : Lookup.getDefault().lookupAll(TreeTableViewColumn.Provider.class))
            Collections.addAll(this.columns, provider.getColumns(heap, TreeTableView.this.viewID));
        }
        
        Collections.sort(this.columns, new Comparator<TreeTableViewColumn>() {
            public int compare(TreeTableViewColumn column1, TreeTableViewColumn column2) {
                return Integer.compare(column1.getPosition(), column2.getPosition());
            }
        });
        
        rendererProviders = new ArrayList();
        for (HeapViewerRenderer.Provider provider : Lookup.getDefault().lookupAll(HeapViewerRenderer.Provider.class))
            if (provider.supportsView(context, viewID)) rendererProviders.add(provider);
        
        actionProviders = new ArrayList();
        for (HeapViewerNodeAction.Provider provider : Lookup.getDefault().lookupAll(HeapViewerNodeAction.Provider.class))
            if (provider.supportsView(context, viewID)) actionProviders.add(provider);
        
        if (useBreadCrumbs) navigator = new BreadCrumbsNavigator() {
            void nodeClicked(HeapViewerNode node) {
                TreeTableView.this.selectExistingNode(node);
            }
            void nodePinned(HeapViewerNode node) {
                TreeTableView.this.pinNode(node);
            }
            void openNode(HeapViewerNode node) {
            }
            HeapViewerRenderer getRenderer(HeapViewerNode node) {
                return getNodeRenderer(node);
            }
            HeapViewerNodeAction.Actions getNodeActions(HeapViewerNode node) {
                return HeapViewerNodeAction.Actions.forNode(node, actionProviders, context, actions);
            }
        };
    }
    
    
    private String viewName;
    
    public void setViewName(String viewName) {
        this.viewName = viewName;
        if (component != null && !hasSelection) nodeSelected(null, false);
    }
    
    public void setViewFilter(HeapViewerNodeFilter filter) {
        this.filter = filter;
        setViewName(viewName); // update (filtered) flag in breadcrumbs if needed
        reloadView();
    }
    
    
    public JComponent getComponent() {
        if (component == null) init();
        return component;
    }
    
//    public void setData(HeapViewerNode[] data) {
//        root.setChildren(data);
//    }
    
    String getViewID() {
        return viewID;
    }
    
    HeapContext getContext() {
        return context;
    }
    
    public RootNode getRoot() {
        if (component == null) init();
        return root;
    }
    
    public void reloadView() {
        if (component != null) {
            Root _root = (Root)getRoot();
            if (_root != currentRoot) pinNode(null);
            _root.resetChildren();
            _root.updateChildren(null);
        }
    }
    
    
    public void closed() {
        root.resetChildren();
    }
    
    
    public void setSortColumn(DataType dataType, SortOrder sortOrder) {
        if (treeTable == null) {
            initialSortColumn = dataType;
            initialSortOrder = sortOrder;
        } else {
            int column = getColumn(dataType);
            if (column == -1) return;
            
            if (sortOrder == null) sortOrder = SortOrder.UNSORTED;
            
            List<? extends RowSorter.SortKey> sortKeys = treeTable.getRowSorter().getSortKeys();
            if (sortKeys != null && sortKeys.size() == 1) {
                RowSorter.SortKey sortKey = sortKeys.get(0);
                if (sortKey.getColumn() == column && sortOrder.equals(sortKey.getSortOrder())) return;
            }

            treeTable.setSorting(column, sortOrder);
        }
    }
    
//    public DataType getSortColumn() {
//        if (treeTable == null) return initialSortColumn;
//        
//        int sortColumn = treeTable.getSortColumn();
//        return sortColumn == -1 ? null : columns.get(sortColumn).getDataType();
//    }
    
    public void setColumnVisible(DataType dataType, boolean visible) {
        int column = getColumn(dataType);
        if (column == -1) return;
        
        treeTable.setColumnVisibility(column, visible);
    }
    
    public boolean isColumnVisible(DataType dataType) {
        int column = getColumn(dataType);
        if (column == -1) return false;
        
        return treeTable.isColumnVisible(column);
    }
    
    private int getColumn(DataType dataType) {
        for (int i = 0; i < columns.size(); i++) {
            TreeTableViewColumn col = columns.get(i);
            if (col.getDataType() == dataType) return i;
        }
        return -1;
    }
    
    
    protected void initializeColumns(DataType initialSortColumn, SortOrder initialSortOrder, ColumnConfiguration initialColumnConfiguration) {
        if (initialSortColumn != null) {
            SortOrder sortOder = initialSortOrder == null ? SortOrder.UNSORTED : initialSortOrder;
            setSortColumn(initialSortColumn, sortOder);
        }
        
        if (initialColumnConfiguration != null)
            configureColumns(initialColumnConfiguration);
    }
    
    public void configureColumns(ColumnConfiguration configuration) {
        if (treeTable == null) {
            initialColumnConfiguration = configuration;
        } else {
            currentColumnConfiguration = configuration;
            
            boolean sortColumnHidden = false;
            
            if (configuration.alwaysShown != null) {
                int column = getColumn(configuration.alwaysShown);
                if (column != -1) treeTable.setColumnVisibility(column, true);
            }
            
            if (configuration.neverShown != null) {
                int column = getColumn(configuration.neverShown);
                
                if (column != -1) {
                    int sortC = treeTable.getSortColumn();
                    if (sortC != -1) sortC = treeTable.convertColumnIndexToModel(sortC);
                    sortColumnHidden = column == sortC;
                    
                    treeTable.setColumnVisibility(column, false);
                }
            }
            
            if (configuration.sortColumn != null) {
                if (configuration.sortMode != null || sortColumnHidden) {
                    int column = getColumn(configuration.sortColumn);
                    
                    if (Boolean.FALSE.equals(configuration.sortMode)) {
                        List<? extends RowSorter.SortKey> sortKeys = treeTable.getRowSorter().getSortKeys();
                        if (sortKeys != null && sortKeys.size() == 1) {
                            RowSorter.SortKey sortKey = sortKeys.get(0);
                            if (!SortOrder.UNSORTED.equals(sortKey.getSortOrder())) return;
                        }
                    }
                    
                    if (configuration.sortOrder == null) {
                        treeTable.setSortColumn(column);
                    } else {
                        treeTable.setSorting(column, configuration.sortOrder);
                    }
                }
            }
        }
    }
    
    public ColumnConfiguration getCurrentColumnConfiguration() {
        return treeTable == null ? initialColumnConfiguration :
                                   currentColumnConfiguration;
    }
    
    
    public void selectNode(HeapViewerNode node) {
        // TODO: implement correctly for lazy model
        treeTable.selectPath(HeapViewerNode.fromNode(node, currentRoot), true);
    }
    
    public void expandNode(HeapViewerNode node) {
        if (treeTable == null) return;
        treeTable.expandPath(HeapViewerNode.fromNode(node));
    }
    
    public void collapseChildren(HeapViewerNode node) {
        if (treeTable == null) return;
        treeTable.collapseChildren(HeapViewerNode.fromNode(node));
    }
    
    
//    protected void willBeSorted(List<RowSorter.SortKey> sortKeys) {}
    
    protected void nodeSelected(HeapViewerNode node, boolean adjusting) {
        hasSelection = node != null;
        if (navigator != null) updateNavigator(node);
    }
    
    protected void nodeExpanding(HeapViewerNode node) {}
    
    protected void nodeExpanded(HeapViewerNode node) {}

    protected void nodeCollapsing(HeapViewerNode node) {}

    protected void nodeCollapsed(HeapViewerNode node) {}
    
    protected HeapViewerNode[] computeData(RootNode root, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {
        return HeapViewerNode.NO_NODES;
    }
    
    protected void childrenChanged() {}
    
    
    private TreeTableViewRenderer getNodesRenderer() {
        if (nodesRenderer == null) {
            Map<Class<? extends HeapViewerNode>, HeapViewerRenderer> map = new HashMap();
            nodesRenderer = new TreeTableViewRenderer();
            for (HeapViewerRenderer.Provider provider : rendererProviders) {
                map.clear();
                provider.registerRenderers(map, context);
                nodesRenderer.registerRenderers(map);
            }
            rendererProviders = null;
        }
        return nodesRenderer;
    }
    
    private HeapViewerRenderer resolveNodeRenderer(HeapViewerNode node) {
        TreeTableViewRenderer viewRenderer = getNodesRenderer();
        return viewRenderer.resolve(node.getClass());
    }
    
    public HeapViewerRenderer getNodeRenderer(HeapViewerNode node) {
        HeapViewerRenderer nodeRenderer = resolveNodeRenderer(node);
        nodeRenderer.setValue(node, -1);
        return nodeRenderer;
    }
    
    // --- OOME handling -------------------------------------------------------
    
    protected void handleOOME(OutOfMemoryError e) {
        HeapUtils.handleOOME(e);
    }
    
    // --- BreadCrumbs prototype -----------------------------------------------
    
    private void setRoot(HeapViewerNode newRoot) {
        nodesCache.clear();
        
        model.setRoot(newRoot);
        currentRoot = newRoot;
        
        treeTable.setRootVisible(currentRoot != root);
    }
    
    void selectExistingNode(HeapViewerNode node) {
        if (node == null) {
            treeTable.clearSelection();
            setRoot(root);
            treeTable.collapseAll();
            treeTable.expandPath(new TreePath(model.getRoot()));
            treeTable.scrollRectToVisible(new Rectangle());
        } else {
            if (!inPinnedView(node)) setRoot(root);
            treeTable.selectPath(HeapViewerNode.fromNode(node, currentRoot), true);
            treeTable.collapseChildren(HeapViewerNode.fromNode(node, currentRoot));
        }
        
        treeTable.requestFocusInWindow();
    }
    
    protected void pinNode(HeapViewerNode node) {
        int row = treeTable.getSelectedRow();
        HeapViewerNode sel = row == -1 ? null : (HeapViewerNode)treeTable.getValueForRow(row);
        
        if (node == null) {
            setRoot(root);
            if (sel != null) treeTable.selectPath(HeapViewerNode.fromNode(sel, currentRoot), true);
        } else {
            setRoot(node);
            if (sel != null) treeTable.selectPath(HeapViewerNode.fromNode(sel, currentRoot), true);
            if (treeTable.getSelectedValue(0) == null) treeTable.selectRow(0, true);
        }
        updateNavigator();
        treeTable.requestFocusInWindow();
    }
    
    private boolean inPinnedView(HeapViewerNode node) {
        while (node != root && node != null) {
            if (node == currentRoot) return true;
            node = (HeapViewerNode)node.getParent();
        }
        return false;
    }
    
    private void updateNavigator() {
        int row = treeTable.getSelectedRow();
        HeapViewerNode sel = row == -1 ? null : (HeapViewerNode)treeTable.getValueForRow(row);
        updateNavigator(sel);
    }
    
    private void updateNavigator(HeapViewerNode node) {
        String _viewName = viewName;
        if (_viewName != null && filter != null) _viewName = Bundle.TreeTableView_FilteredFlag(_viewName);
        navigator.setNode(node, currentRoot == root ? null : currentRoot, getRoot(), _viewName);
    }
    
    // -------------------------------------------------------------------------
    
    
    private void init() {
        model = new Model();
        nodesCache = new NodesCache();
        component = createComponent();
    }
    
    protected JComponent createComponent() {
        int sortingColumn = -1;
        for (int i = 0; i < columns.size(); i++) {
            TreeTableViewColumn column = columns.get(i);
            if (column.initiallyVisible() && column.initiallySorting()) {
                sortingColumn = i;
                break;
            }
        }
        
        List<? extends RowSorter.SortKey> sortKeys;
        if (sortingColumn != -1) {
            RowSorter.SortKey sortKey = new RowSorter.SortKey(sortingColumn, SortOrder.DESCENDING); // TODO: resolve the right SortOrder
//        List<? extends RowSorter.SortKey> sortKeys = Collections.singletonList(sortKey);
            sortKeys = Collections.singletonList(sortKey);
        } else {
            sortKeys = Collections.EMPTY_LIST;
        }
        updateSortInfo(sortKeys);
        
        treeTable = new HeapViewerTreeTable(model, sortKeys) {
            protected void populatePopup(JPopupMenu popup, Object value, Object userValue) {
                HeapViewerNode node = (HeapViewerNode)value;
                HeapViewerNodeAction.Actions nodeActions = navigator == null ?
                        HeapViewerNodeAction.Actions.forNode(node, actionProviders, context, actions) :
                        HeapViewerNodeAction.Actions.forNode(node, actionProviders, context, actions, new PinAction(node), new ResetPinAction());
                nodeActions.populatePopup(popup);
                
                TreeTableView.this.populatePopup(node, popup);
                
                if (popup.getComponentCount() > 0) popup.addSeparator();
                popup.add(treeTable.createCopyMenuItem());
                TreeTableView.this.populatePopupLast(node, popup);
            }
            public void performDefaultAction(ActionEvent e) {
                int row = getSelectedRow();
                if (row == -1) return;

                Object value = getValueForRow(row);
                if (!(value instanceof HeapViewerNode)) return;
                
                HeapViewerNodeAction.Actions nodeActions =
                        HeapViewerNodeAction.Actions.forNode((HeapViewerNode)value, actionProviders, context, actions);
                nodeActions.performDefaultAction(e);
            }
            protected void nodeSelected(HeapViewerNode node, boolean adjusting) {
                TreeTableView.this.nodeSelected(node, adjusting);
            }
            protected void forgetChildren(HeapViewerNode node) {
                node.forgetChildren(nodesCache);
            }
            protected void willBeSorted(List<? extends RowSorter.SortKey> sortKeys) {
                if (!isInitializing() && !sortKeys.isEmpty()) {
                    int col = sortKeys.get(0).getColumn();
                    DataType type = columns.get(col).getDataType();
                    Heap heap = context.getFragment().getHeap();
                    if (!type.valuesAvailable(heap)) type.computeValues(heap, null);
                }
                
                updateSortInfo(sortKeys);
                nodesCache.clear();
                
                super.willBeSorted(sortKeys);
                
//                TreeTableView.this.willBeSorted(sortKeys);
            }
            protected void nodeExpanding(TreeNode node) { super.nodeExpanding(node); if (node instanceof HeapViewerNode) TreeTableView.this.nodeExpanding((HeapViewerNode)node); }
    
            protected void nodeExpanded(TreeNode node) { super.nodeExpanded(node); if (node instanceof HeapViewerNode) TreeTableView.this.nodeExpanded((HeapViewerNode)node); }

            protected void nodeCollapsing(TreeNode node) { super.nodeCollapsing(node); if (node instanceof HeapViewerNode) TreeTableView.this.nodeCollapsing((HeapViewerNode)node); }

            protected void nodeCollapsed(TreeNode node) { super.nodeCollapsed(node); if (node instanceof HeapViewerNode) TreeTableView.this.nodeCollapsed((HeapViewerNode)node); }
        };
        
        treeTable.setTreeCellRenderer(getNodesRenderer());

        for (int i = 0; i < columns.size(); i++) {
            TreeTableViewColumn column = columns.get(i);
            
            if (i > 0) treeTable.setColumnRenderer(i, getRenderer(column));
            
            int width = column.getPreferredWidth();
            if (width > -1) treeTable.setDefaultColumnWidth(i, width);
            
            if (!column.initiallyVisible()) treeTable.setColumnVisibility(i, false);
        }
        
        initializeColumns(initialSortColumn, initialSortOrder, initialColumnConfiguration);
        initialSortColumn = null;
        initialSortOrder = null;
        initialColumnConfiguration = null;
        
        treeTable.providePopupMenu(true);
        
        treeTable.setSelectionOnMiddlePress(true);
        treeTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isMiddleMouseButton(e)) {
                    int row = treeTable.getSelectedRow();
                    if (row == -1) return;
                    
                    Object value = treeTable.getValueForRow(row);
                    if (!(value instanceof HeapViewerNode)) return;
                    
                    HeapViewerNode node = (HeapViewerNode)value;
                    HeapViewerNodeAction.Actions nodeActions = navigator == null ?
                        HeapViewerNodeAction.Actions.forNode(node, actionProviders, context, actions) :
                        HeapViewerNodeAction.Actions.forNode(node, actionProviders, context, actions, new PinAction(node), new ResetPinAction());
                    ActionEvent ae = new ActionEvent(e.getSource(), e.getID(), "middle button", e.getWhen(), e.getModifiers()); // NOI18N
                    nodeActions.performMiddleButtonAction(ae);
                }
            }
        });
        
        JComponent comp = new JPanel(new BorderLayout()) {
            public boolean requestFocusInWindow() {
                return treeTable.requestFocusInWindow();
            }
        };
        comp.add(new ProfilerTableContainer(treeTable, false, null), BorderLayout.CENTER);
        
        JComponent toolsContainer = new JPanel(new GridBagLayout());
        comp.add(toolsContainer, BorderLayout.SOUTH);
        
        GridBagConstraints c;
        
        if (navigator != null) {
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1f;
            toolsContainer.add(navigator.getComponent(), c);
        }
        
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1f;
        filterComponent = new JPanel(new BorderLayout());
        filterComponent.setVisible(false);
        toolsContainer.add(filterComponent, c);
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() { nodeSelected(null, false); }
        });
        
        return comp;
    }
    
    protected void populatePopup(HeapViewerNode node, JPopupMenu popup) {
    }
    
    protected void populatePopupLast(HeapViewerNode node, JPopupMenu popup) {
        if (filterComponent.getComponentCount() > 0) {
            popup.addSeparator();
            popup.add(new JMenuItem(Bundle.TreeTableView_Filter()) {
                protected void fireActionPerformed(ActionEvent e) { activateFilter(); }
            });
        }
    }
    
    protected void setFilterComponent(JComponent filter) {
        filterComponent.add(filter, BorderLayout.CENTER);
        filterComponent.setVisible(true);
        registerActions();
    }
    
    protected JComponent getFilterComponent() {
        return filterComponent.getComponentCount() == 0 ? null :
               (JComponent)filterComponent.getComponent(0);
    }
    
    private void activateFilter() {
        Component filterComp = getFilterComponent();
        if (filterComp != null) {
            filterComp.setVisible(true);
            filterComp.requestFocusInWindow();
        }
    }
    
    private void registerActions() {
        InputMap inputMap = treeTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap actionMap = treeTable.getActionMap();
        
        final String filterKey = org.graalvm.visualvm.lib.ui.swing.FilterUtils.FILTER_ACTION_KEY;
        Action filterAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) { activateFilter(); }
        };
        ActionsSupport.registerAction(filterKey, filterAction, actionMap, inputMap);
    }
    
    private void updateSortInfo(List<? extends RowSorter.SortKey> sortKeys) {
        if (sortKeys == null) {
            dataTypes = null;
            sortOrders = null;
        } else {
            dataTypes = new ArrayList(sortKeys.size());
            sortOrders = new ArrayList(sortKeys.size());

            for (RowSorter.SortKey sortKey : sortKeys) {
                // TODO: find out the root cause - java.lang.IndexOutOfBoundsException: Index: 2, Size: 2
                if (columns.size() > sortKey.getColumn()) {
                    dataTypes.add(columns.get(sortKey.getColumn()).getDataType());
                    sortOrders.add(sortKey.getSortOrder());
                }
            }
        }
    }
    
    
    private static ProfilerRenderer getRenderer(TreeTableViewColumn column) {
        ProfilerRenderer renderer = column.getRenderer();
        if (renderer != null) return renderer;
        
        Class columnClass = column.getDataType().getType();
        if (Number.class.isAssignableFrom(columnClass)) return new NumberRenderer();
        else return new LabelRenderer();
    }
    
    
    private class PinAction extends HeapViewerNodeAction {
        
        private final HeapViewerNode node;
        
        PinAction(HeapViewerNode node) {
            super(Bundle.TreeTableView_Pin(), 110);
            this.node = node;
            setEnabled(node != currentRoot && !node.isLeaf());
        }
        
        public boolean isMiddleButtonDefault(ActionEvent e) {
            int modifiers = e.getModifiers();
            return (modifiers & ActionEvent.CTRL_MASK) == ActionEvent.CTRL_MASK &&
                   (modifiers & ActionEvent.SHIFT_MASK) != ActionEvent.SHIFT_MASK;
        }

        public void actionPerformed(ActionEvent e) {
            TreeTableView.this.pinNode(node);
        }
        
    }
    
    private class ResetPinAction extends HeapViewerNodeAction {
        
        ResetPinAction() {
            super(Bundle.TreeTableView_ResetPin(), 111);
            setEnabled(root != currentRoot);
        }
        
        public boolean isMiddleButtonDefault(ActionEvent e) {
            int modifiers = e.getModifiers();
            return (modifiers & ActionEvent.CTRL_MASK) == ActionEvent.CTRL_MASK &&
                   (modifiers & ActionEvent.SHIFT_MASK) == ActionEvent.SHIFT_MASK;
        }
        
        public void actionPerformed(ActionEvent e) {
            TreeTableView.this.pinNode(null);
        }
        
    }
    
    
    private final class Model extends ProfilerTreeTableModel.Abstract {
        
        private Runnable[] dataTypeListeners;
        
        private Model() {
            super(root);
            
            dataTypeListeners = new Runnable[getColumnCount()];
            for (int i = 0; i < dataTypeListeners.length; i++) {
                final TreeTableViewColumn viewColumn = columns.get(i);
                DataType dataType = viewColumn.getDataType();
                if (dataType.valuesAvailable(context.getFragment().getHeap())) {
                    dataTypeListeners[i] = null;
                } else {
                    final int ii = i;
                    dataTypeListeners[ii] = new Runnable() {
                        public void run() {
                            dataTypeListeners[ii] = null;
                            TableColumn tableColumn = treeTable.getColumnModel().getColumn(ii);
                            tableColumn.setHeaderValue(getColumnName(ii));
                            TreeTableView.this.component.repaint();
                            if (dataTypes != null && dataTypes.contains(dataType)) {
                                TreeTableView.this.reloadView();
                            }
                        }
                    };
                    dataType.notifyWhenAvailable(context.getFragment().getHeap(), dataTypeListeners[ii]);
                }
            }
        }

        public int getColumnCount() {
            return columns.size();
        }

        public Class getColumnClass(int column) {
            if (column == 0) return JTree.class;
            return columns.get(column).getDataType().getType();
        }

        public String getColumnName(int column) {
            String columnName = columns.get(column).getHeaderValue().toString();
            return dataTypeListeners[column] == null ? columnName :
                   "<html><nobr>" + columnName + " <small style='color: gray;'>(" + Bundle.TreeTableView_SortToGet() + ")</small></nobr></html>"; // NOI18N
        }
        
        public Object getValueAt(TreeNode node, int column) {
            return HeapViewerNode.getValue((HeapViewerNode)node, columns.get(column).getDataType(), context.getFragment().getHeap());
        }
        

        public void setValueAt(Object aValue, TreeNode node, int column) {}

        public boolean isCellEditable(TreeNode node, int column) { return false; }
        
    }
    
    
    private abstract class Root extends RootNode {
        
        public abstract void updateChildren(HeapViewerNode node);
        
    }
    
    
    public static final class ColumnConfiguration {
        
        final DataType alwaysShown;
        final DataType neverShown;
        
        final DataType sortColumn;
        final SortOrder sortOrder;
        final Boolean sortMode;
        
        public ColumnConfiguration(DataType alwaysShown, DataType neverShown, DataType sortColumn, SortOrder sortOrder, Boolean sortMode) {
            this.alwaysShown = alwaysShown;
            this.neverShown = neverShown;
            
            this.sortColumn = sortColumn;
            this.sortOrder = sortOrder;
            this.sortMode = sortMode;
        }
        
    }
    
}
