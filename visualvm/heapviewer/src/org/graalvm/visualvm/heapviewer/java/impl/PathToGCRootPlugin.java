/*
 * Copyright (c) 2017, 2023, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.heapviewer.java.impl;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.heapviewer.HeapContext;
import org.graalvm.visualvm.heapviewer.java.ClassNode;
import org.graalvm.visualvm.heapviewer.java.ClassNodeRenderer;
import org.graalvm.visualvm.heapviewer.java.InstanceNode;
import org.graalvm.visualvm.heapviewer.java.InstanceNodeRenderer;
import org.graalvm.visualvm.heapviewer.java.InstanceReferenceNode;
import org.graalvm.visualvm.heapviewer.java.InstancesWrapper;
import org.graalvm.visualvm.heapviewer.java.JavaHeapFragment;
import org.graalvm.visualvm.heapviewer.model.DataType;
import org.graalvm.visualvm.heapviewer.model.ErrorNode;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNode;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNodeFilter;
import org.graalvm.visualvm.heapviewer.model.Progress;
import org.graalvm.visualvm.heapviewer.model.RootNode;
import org.graalvm.visualvm.heapviewer.model.TextNode;
import org.graalvm.visualvm.heapviewer.swing.LinkButton;
import org.graalvm.visualvm.heapviewer.ui.HeapViewPlugin;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerActions;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerRenderer;
import org.graalvm.visualvm.heapviewer.ui.TreeTableView;
import org.graalvm.visualvm.heapviewer.ui.TreeTableViewColumn;
import org.graalvm.visualvm.heapviewer.ui.UIThresholds;
import org.graalvm.visualvm.heapviewer.utils.HeapOperations;
import org.graalvm.visualvm.heapviewer.utils.HeapUtils;
import org.graalvm.visualvm.heapviewer.utils.NodesComputer;
import org.graalvm.visualvm.heapviewer.utils.ProgressIterator;
import org.graalvm.visualvm.lib.jfluid.heap.ArrayItemValue;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.ObjectFieldValue;
import org.graalvm.visualvm.lib.jfluid.heap.Value;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.ProfilerIcons;
import org.graalvm.visualvm.lib.ui.UIUtils;
import org.graalvm.visualvm.lib.ui.swing.renderer.LabelRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.MultiRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.NormalBoldGrayRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.ProfilerRenderer;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "PathToGCRootPlugin_Name=GC Root",
    "PathToGCRootPlugin_Description=GC Root",
    "PathToGCRootPlugin_NoRoot=<no GC root>",
    "PathToGCRootPlugin_IsRoot=<node is GC root>",
    "PathToGCRootPlugin_NoSelection=<no class or instance selected>",
    "PathToGCRootPlugin_MoreNodes=<another {0} GC roots left>",
    "PathToGCRootPlugin_SamplesContainer=<sample {0} GC roots>",
    "PathToGCRootPlugin_NodesContainer=<GC roots {0}-{1}>",
    "PathToGCRootPlugin_ComputeMergedRootsLbl=Compute Merged GC Roots",
    "PathToGCRootPlugin_ComputeMergedRootsTtp=Compute merged GC roots for the selected class",
    "PathToGCRootPlugin_AutoComputeMergedRootsLbl=Compute Merged GC Roots Automatically",
    "PathToGCRootPlugin_AutoComputeMergedRootsTtp=Compute merged GC roots automatically for each selected class"
})
public class PathToGCRootPlugin extends HeapViewPlugin {
    
    private static final TreeTableView.ColumnConfiguration CCONF_CLASS = new TreeTableView.ColumnConfiguration(DataType.COUNT, null, DataType.COUNT, SortOrder.DESCENDING, Boolean.FALSE);
    private static final TreeTableView.ColumnConfiguration CCONF_INSTANCE = new TreeTableView.ColumnConfiguration(null, DataType.COUNT, DataType.NAME, SortOrder.UNSORTED, null);
    
    private final Heap heap;
    private HeapViewerNode selected;
    
    private volatile boolean mergedRequest;
    
    private final TreeTableView objectsView;
    
    private volatile boolean showingClass;
    
    
    public PathToGCRootPlugin(HeapContext context, HeapViewerActions actions) {
        super(Bundle.PathToGCRootPlugin_Name(), Bundle.PathToGCRootPlugin_Description(), Icons.getIcon(ProfilerIcons.RUN_GC));
        
        heap = context.getFragment().getHeap();
        
        TreeTableViewColumn[] columns = new TreeTableViewColumn[] {
            new TreeTableViewColumn.Name(),
            new TreeTableViewColumn.LogicalValue(),
            new TreeTableViewColumn.Count(heap, true, true),
            new TreeTableViewColumn.OwnSize(heap, false, false),
            new TreeTableViewColumn.RetainedSize(heap, false, false),
            new TreeTableViewColumn.ObjectID()
        };
        objectsView = new TreeTableView("java_objects_gcroots", context, actions, columns) { // NOI18N
            protected HeapViewerNode[] computeData(RootNode root, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {
                if (mergedRequest) return HeapViewerNode.NO_NODES;
                
                HeapViewerNode _selected;
                synchronized (objectsView) { _selected = selected; }

                if (_selected == null) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            if (!CCONF_INSTANCE.equals(objectsView.getCurrentColumnConfiguration()))
                                objectsView.configureColumns(CCONF_INSTANCE);
                        }
                    });
                    
                    return new HeapViewerNode[] { new TextNode(Bundle.PathToGCRootPlugin_NoSelection()) };
                }

                Instance instance;
                InstancesWrapper wrapper = HeapViewerNode.getValue(_selected, DataType.INSTANCES_WRAPPER, heap);
                if (wrapper != null) {
                    instance = null;

                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
//                            if (!mergedRoots && !CCONF_INSTANCE.equals(objectsView.getCurrentColumnConfiguration()))
//                                objectsView.configureColumns(CCONF_INSTANCE);
//                            else if (mergedRoots && !CCONF_CLASS.equals(objectsView.getCurrentColumnConfiguration()))
//                                objectsView.configureColumns(CCONF_CLASS);
                            if (!CCONF_CLASS.equals(objectsView.getCurrentColumnConfiguration()))
                                objectsView.configureColumns(CCONF_CLASS);
                        }
                    });

//                    if (!mergedRoots) return new HeapViewerNode[] { new TextNode("<merged GC roots disabled>") };
                } else {
                    instance = HeapViewerNode.getValue(_selected, DataType.INSTANCE, heap);

                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            if (!CCONF_INSTANCE.equals(objectsView.getCurrentColumnConfiguration()))
                                objectsView.configureColumns(CCONF_INSTANCE);
                        }
                    });

                    if (instance == null) return new HeapViewerNode[] { new TextNode(Bundle.PathToGCRootPlugin_NoSelection()) };
                }
                
                HeapOperations.initializeGCRoots(heap);

                Collection<HeapViewerNode> data;
                if (instance != null) {
                    data = computeInstanceRoots(instance, progress);
                    if (data != null) showingClass = false;
                } else {
                    data = computeInstancesRoots(wrapper.getInstancesIterator(), wrapper.getInstancesCount(), progress);
                    if (data != null) showingClass = true;
                }

                if (data == null) return null;
                if (data.size() == 1) return new HeapViewerNode[] { data.iterator().next() };

                final Collection<HeapViewerNode> _data = data;
                NodesComputer<HeapViewerNode> computer = new NodesComputer<HeapViewerNode>(_data.size(), UIThresholds.MAX_MERGED_OBJECTS) {
                    protected boolean sorts(DataType dataType) {
                        return true;
                    }
                    protected HeapViewerNode createNode(HeapViewerNode node) {
                        return node;
                    }
                    protected ProgressIterator<HeapViewerNode> objectsIterator(int index, Progress progress) {
                        Iterator iterator = _data.iterator();
                        return new ProgressIterator(iterator, index, true, progress);
                    }
                    protected String getMoreNodesString(String moreNodesCount)  {
                        return Bundle.PathToGCRootPlugin_MoreNodes(moreNodesCount);
                    }
                    protected String getSamplesContainerString(String objectsCount)  {
                        return Bundle.PathToGCRootPlugin_SamplesContainer(objectsCount);
                    }
                    protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                        return Bundle.PathToGCRootPlugin_NodesContainer(firstNodeIdx, lastNodeIdx);
                    }
                };

                return computer.computeNodes(root, heap, viewID, null, dataTypes, sortOrders, progress);
            }
            @Override
            protected void populatePopup(HeapViewerNode node, JPopupMenu popup) {
                if (popup.getComponentCount() > 0) popup.addSeparator();
                
                popup.add(new JCheckBoxMenuItem(Bundle.PathToGCRootPlugin_AutoComputeMergedRootsLbl(), isAutoMerge()) {
                    @Override
                    protected void fireActionPerformed(ActionEvent event) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                setAutoMerge(isSelected());
                            }
                        });
                    }
                });
            }
            protected void childrenChanged() {
                if (!showingClass) fullyExpandNode(getRoot());
            }
            protected void nodeExpanded(HeapViewerNode node) {
                if (showingClass && node instanceof GCInstanceNode) fullyExpandNode(node);
            }
            private void fullyExpandNode(HeapViewerNode node) {
                while (node != null) {
                    expandNode(node);
                    node = node.getNChildren() > 0 ? node.getChild(0) : null;
                }
            }
        };
    }
    
    
    private JComponent component;
    
    private void showObjectsView() {
        JComponent c = objectsView.getComponent();
        if (c.isVisible()) return;
        
        c.setVisible(true);
        
        component.removeAll();
        component.add(c, BorderLayout.CENTER);
        
        mergedRequest = false;
        
        component.invalidate();
        component.revalidate();
        component.repaint();
    }
    
    private void showMergedView() {
        JComponent c = objectsView.getComponent();
        if (!c.isVisible()) return;
        
        c.setVisible(false);
        
        component.removeAll();
        
        JButton jb = new JButton(Bundle.PathToGCRootPlugin_ComputeMergedRootsLbl(), Icons.getIcon(ProfilerIcons.RUN_GC)) {
            protected void fireActionPerformed(ActionEvent e) {
                showObjectsView();
                objectsView.reloadView();
            }
        };
        jb.setIconTextGap(jb.getIconTextGap() + 2);
        jb.setToolTipText(Bundle.PathToGCRootPlugin_ComputeMergedRootsTtp());
        Insets margin = jb.getMargin();
        if (margin != null) jb.setMargin(new Insets(margin.top + 3, margin.left + 3, margin.bottom + 3, margin.right + 3));
        
        
        LinkButton lb = new LinkButton(Bundle.PathToGCRootPlugin_AutoComputeMergedRootsLbl()) {
            protected void fireActionPerformed(ActionEvent e) {
                setAutoMerge(true);
                showObjectsView();
                objectsView.reloadView();
            }
        };
        lb.setToolTipText(Bundle.PathToGCRootPlugin_AutoComputeMergedRootsTtp());
                
        
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        GridBagConstraints g;
        
        g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridy = 0;
        p.add(jb, g);
        
        g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridy = 1;
        g.insets = new Insets(10, 0, 0, 0);
        p.add(lb, g);
        
        component.add(p);
        
        mergedRequest = true;

        component.invalidate();
        component.revalidate();
        component.repaint();
    }

    protected JComponent createComponent() {
        component = new JPanel(new BorderLayout());
        component.setOpaque(true);
        component.setBackground(UIUtils.getProfilerResultsBackground());
        
        objectsView.getComponent().setVisible(false); // force init in showObjectsView()
        showObjectsView();
        
        return component;
    }
        
    @Override
    protected void nodeSelected(final HeapViewerNode node, boolean adjusting) {
        synchronized (objectsView) {
            if (Objects.equals(selected, node)) return;
            selected = node;
        }
        
        if (selected != null && !isAutoMerge() && HeapViewerNode.getValue(selected, DataType.INSTANCES_WRAPPER, heap) != null) showMergedView();
        else showObjectsView();
        
        objectsView.reloadView();
    }
    
    
    @Override
    protected void closed() {
        synchronized (objectsView) { selected = objectsView.getRoot(); }
        objectsView.closed();
    }
    
    
    private static Collection<HeapViewerNode> computeInstanceRoots(Instance instance, Progress progress) throws InterruptedException {
        Instance nextInstance = instance.getNearestGCRootPointer();
                    
        if (nextInstance == null) {
            return Collections.singleton(new TextNode(Bundle.PathToGCRootPlugin_NoRoot()));
        } else if (nextInstance == instance) {
            return Collections.singleton(new TextNode(Bundle.PathToGCRootPlugin_IsRoot()));
        } else {
            ToRoot node = null;
            ToRoot firstNode = null;
            ToRoot previousNode = null;
            
            try {
                progress.setupUnknownSteps();
                
                Thread current = Thread.currentThread();
                while (!current.isInterrupted() && instance != nextInstance) {
                    List<Value> references = instance.getReferences();
                    for (Value reference : references) {
                        if (nextInstance.equals(reference.getDefiningInstance())) {
                            if (reference instanceof ObjectFieldValue) {
                                ObjectFieldValue ref = (ObjectFieldValue)reference;
                                SkipNode snode = HeapPatterns.processGCRootReference(ref);
                                if (snode != null) {
                                    ref = snode.getValue();
                                    nextInstance = ref.getDefiningInstance();
                                    node = snode;
                                    if (firstNode == null) firstNode = node;
                                    else previousNode.setChildren(new HeapViewerNode[] { (HeapViewerNode)node });
                                    previousNode = node;
                                }
                                node = new FieldToRoot(ref);
                            } else if (reference instanceof ArrayItemValue) {
                                node = new ArrayItemToRoot((ArrayItemValue)reference);
                            }

                            if (firstNode == null) firstNode = node;
                            else previousNode.setChildren(new HeapViewerNode[] { (HeapViewerNode)node });

                            break;
                        }
                    }

                    instance = nextInstance;
                    nextInstance = instance.getNearestGCRootPointer();
                    progress.step();

                    previousNode = node;
                }
                if (current.isInterrupted()) throw new InterruptedException();
                
                if (node != null) node.setChildren(HeapViewerNode.NO_NODES);
            } finally {           
                progress.finish();
            }
            
            return Collections.singleton((HeapViewerNode) firstNode);
        }
    }
    
    private static Collection<HeapViewerNode> computeInstancesRoots(Iterator<Instance> instances, int count, Progress progress) throws InterruptedException {
        Map<Instance, HeapViewerNode> gcRoots = new HashMap();
        Map<Instance,Instance> gcRootCache = new HashMap<>();
        
        try {
            progress.setupKnownSteps(count);
            
            Thread current = Thread.currentThread();
            while (!current.isInterrupted() && instances.hasNext()) {
                Instance instance = instances.next();
                Instance gcRoot = getGCRoot(instance, current, gcRootCache);
                GCRootNode gcRootNode = (GCRootNode)gcRoots.get(gcRoot);
                if (gcRootNode == null) {
                    gcRootNode = new GCRootNode(gcRoot);
                    gcRoots.put(gcRoot, gcRootNode);
                }
                gcRootNode.addInstance(instance);
                progress.step();
            }
            if (current.isInterrupted()) throw new InterruptedException();
        } catch (OutOfMemoryError e) {
            System.err.println("Out of memory in PathToGCRootPlugin: " + e.getMessage()); // NOI18N
            HeapUtils.handleOOME(true, e);
            return Collections.singleton(new ErrorNode.OOME());
        } finally {
            progress.finish();
        }
        
        if (!gcRoots.isEmpty()) return gcRoots.values();
        else return Collections.singleton(new TextNode(Bundle.PathToGCRootPlugin_NoRoot()));
    }
    
    private static final int MARKER_DISTANCE = 1000;
    private static Instance getGCRoot(Instance instance, Thread current, Map<Instance,Instance> cache) {
        List<Instance> markers = new ArrayList<>();
        int i = 1;
        while (!instance.isGCRoot()) {
            // check cache first
            Instance gcRoot = cache.get(instance);
            if (gcRoot != null) {
                instance = gcRoot;
                break;
            }
            if (i++ % MARKER_DISTANCE == 0) markers.add(instance);
            instance = instance.getNearestGCRootPointer();
            if (current.isInterrupted() || instance == null) return null;
        }
        for (Instance m : markers) {
            cache.put(m, instance);
        }
        return instance;
    }
    
    
    private static final String KEY_MERGED_GCROOTS = "HeapViewer.autoMergedGcRoots"; // NOI18N
    
    private boolean isAutoMerge() {
        return NbPreferences.root().getBoolean(KEY_MERGED_GCROOTS, false);
    }

    private void setAutoMerge(boolean value) {
        NbPreferences.root().putBoolean(KEY_MERGED_GCROOTS, value);
    }
    
    
    @NbBundle.Messages({
        "GCRootNode_MoreNodes=<another {0} instances left>",
        "GCRootNode_SamplesContainer=<sample {0} instances>",
        "GCRootNode_NodesContainer=<instances {0}-{1}>"
    })
    static class GCRootNode extends InstanceNode.IncludingNull {
        
//        private final int maxNodes = UIThresholds.MAX_MERGED_OBJECTS;
        
        private final List<Instance> instances = new ArrayList();
        
        
        GCRootNode(Instance gcRoot) {
            super(gcRoot);
        }
        
        
        void addInstance(Instance instance) {
            instances.add(instance);
        }
        
        
        public int getCount() {
            return instances.size();
        }
        
        
//        protected HeapViewerNode[] computeChildren(RootNode root) {
//            int itemsCount = instances.size();
//            if (itemsCount <= maxNodes) {
//                HeapViewerNode[] nodes = new HeapViewerNode[itemsCount];
//                for (int i = 0; i < itemsCount; i++) nodes[i] = createNode(instances.get(i));
//                return nodes;
//            } else {
//                return super.computeChildren(root);
//            }
//        }

        protected HeapViewerNode[] lazilyComputeChildren(Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {
            final Instance gcRoot = getInstance();
            final boolean isArray = gcRoot != null && gcRoot.getJavaClass().isArray();
            NodesComputer<Instance> computer = new NodesComputer<Instance>(instances.size(), UIThresholds.MAX_MERGED_OBJECTS) {
                protected boolean sorts(DataType dataType) {
                    if (DataType.COUNT.equals(dataType) || (DataType.OWN_SIZE.equals(dataType) && !isArray)) return false;
                    return true;
                }
                protected HeapViewerNode createNode(Instance object) {
                    return new GCInstanceNode(object) {
                        public boolean isLeaf() { return gcRoot == null; }
                    };
                }
                protected ProgressIterator<Instance> objectsIterator(int index, Progress progress) {
                    Iterator<Instance> iterator = instances.listIterator(index);
                    return new ProgressIterator(iterator, index, false, progress);
                }
                protected String getMoreNodesString(String moreNodesCount)  {
                    return Bundle.GCRootNode_MoreNodes(moreNodesCount);
                }
                protected String getSamplesContainerString(String objectsCount)  {
                    return Bundle.GCRootNode_SamplesContainer(objectsCount);
                }
                protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                    return Bundle.GCRootNode_NodesContainer(firstNodeIdx, lastNodeIdx);
                }
            };
            return computer.computeNodes(GCRootNode.this, heap, viewID, null, dataTypes, sortOrders, progress);
        }
        
        
        protected Object getValue(DataType type, Heap heap) {
            if (type == DataType.COUNT) return getCount();

            return super.getValue(type, heap);
        }
        
        
        public boolean isLeaf() {
            return false;
        }
        
        
        static class Renderer extends InstanceNodeRenderer {
            
            private static final ImageIcon ICON = Icons.getImageIcon(ProfilerIcons.NODE_FORWARD);
        
            Renderer(Heap heap) {
                super(heap);
            }
            
            @Override
            public void setValue(Object value, int row) {
                if (value != null) {
                    GCRootNode node = (GCRootNode)value;
                    if (node.getInstance() == null) {
                        setNormalValue(Bundle.PathToGCRootPlugin_NoRoot());
                        setBoldValue(""); // NOI18N
                        setGrayValue(""); // NOI18N
                        setIcon(ICON);
                        return;
                    }
                }
                super.setValue(value, row);
                
                setIconTextGap(4);
                ((LabelRenderer)valueRenderers()[0]).setMargin(3, 3, 3, 0);
            }
            
            @Override
            protected ImageIcon getIcon(Instance instance, boolean isGCRoot) {
                return ICON;
            }

        }
        
    }
    
    private static class GCInstanceNode extends InstanceNode {
    
        GCInstanceNode(Instance instance) {
            super(instance);
        }
        
        protected HeapViewerNode[] lazilyComputeChildren(Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {
            Collection<HeapViewerNode> nodes = PathToGCRootPlugin.computeInstanceRoots(getInstance(), progress);
            return nodes == null ? null : nodes.toArray(HeapViewerNode.NO_NODES);
        }
    
    }
    
    private static interface ToRoot {
        
        public void setChildren(HeapViewerNode[] ch);
        
    }
    
    private static class FieldToRoot extends InstanceReferenceNode.Field implements ToRoot {
        
        FieldToRoot(ObjectFieldValue value) {
            super(value, true);
        }
        
        public void setChildren(HeapViewerNode[] ch) {
            super.setChildren(ch);
        }
        
    }
    
    private static class ArrayItemToRoot extends InstanceReferenceNode.ArrayItem implements ToRoot {
        
        ArrayItemToRoot(ArrayItemValue value) {
            super(value, true);
        } 
        
        public void setChildren(HeapViewerNode[] ch) {
            super.setChildren(ch);
        }
        
    }
    
    static class SkipNode extends FieldToRoot {

        private int skipped;

        SkipNode(ObjectFieldValue ref, int sk) {
            super(ref);
            skipped = sk;
        }
    }

    @NbBundle.Messages({
        "# {0} - number of skipped nodes",
        "SkipNodeRenderer_Instances=in {0} collapsed instances of "
    })
    static class SkipNodeRenderer extends MultiRenderer implements HeapViewerRenderer {

        private final NormalBoldGrayRenderer nameRenderer;
        private final LabelRenderer equalsRenderer;
        private final ClassNodeRenderer classRenderer;
        private final ProfilerRenderer[] renderers;
        private final Heap heap;

        SkipNodeRenderer(Heap heap) {
            this.heap = heap;
            nameRenderer = new NormalBoldGrayRenderer() {
                public void setValue(Object value, int row) {
                    SkipNode node = (SkipNode) value;
                    String name = node.getFieldName();
                    if (name.startsWith("static ")) { // NOI18N
                        setNormalValue("static "); // NOI18N
                        setBoldValue(name.substring("static ".length())); // NOI18N
                    } else {
                        setNormalValue(""); // NOI18N
                        setBoldValue(name);
                    }
                    setIcon(Icons.getIcon(ProfilerIcons.NODE_REVERSE));
                }
            };
            equalsRenderer = new LabelRenderer() {
                public void setValue(Object value, int row) {
                    SkipNode node = (SkipNode) value;
                    setText(Bundle.SkipNodeRenderer_Instances(NumberFormat.getInstance().format(node.skipped)));
                    setMargin(3, 2, 3, 0);
                }

                public String toString() {
                    return " " + getText() + " "; // NOI18N
                }
            };
            classRenderer = new ClassNodeRenderer(heap);
            renderers = new ProfilerRenderer[]{nameRenderer, equalsRenderer, classRenderer};
        }

        public Icon getIcon() {
            return nameRenderer.getIcon();
        }

        public String getShortName() {
            return nameRenderer.toString();
        }

        protected ProfilerRenderer[] valueRenderers() {
            return renderers;
        }

        public void setValue(Object value, int row) {
            HeapViewerNode node = (HeapViewerNode) value;

            nameRenderer.setValue(node, row);
            equalsRenderer.setValue(node, row);
            classRenderer.setValue(new ClassNode(((SkipNode) node).getJavaClass()), row);
        }
    }

    @ServiceProvider(service = HeapViewerRenderer.Provider.class)
    public static class PathToGCRootRendererProvider extends HeapViewerRenderer.Provider {

        @Override
        public boolean supportsView(HeapContext context, String viewID) {
            return "java_objects_gcroots".equals(viewID); // NOI18N
        }

        @Override
        public void registerRenderers(Map<Class<? extends HeapViewerNode>, HeapViewerRenderer> renderers, HeapContext context) {
            renderers.put(SkipNode.class, new SkipNodeRenderer(context.getFragment().getHeap()));
        }

    }

    @ServiceProvider(service=HeapViewPlugin.Provider.class, position = 400)
    public static class Provider extends HeapViewPlugin.Provider {

        public HeapViewPlugin createPlugin(HeapContext context, HeapViewerActions actions, String viewID) {
            if (!viewID.startsWith("diff") && JavaHeapFragment.isJavaHeap(context)) return new PathToGCRootPlugin(context, actions); // NOI18N
            return null;
        }
        
    }
    
}
