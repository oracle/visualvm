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

package com.sun.tools.visualvm.heapviewer.java.impl;

import java.util.List;
import javax.swing.JComponent;
import javax.swing.SortOrder;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.lib.profiler.heap.ArrayItemValue;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.HeapProgress;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.ObjectFieldValue;
import org.netbeans.lib.profiler.heap.Value;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import com.sun.tools.visualvm.heapviewer.HeapContext;
import com.sun.tools.visualvm.heapviewer.HeapFragment;
import com.sun.tools.visualvm.heapviewer.java.ClassNode;
import com.sun.tools.visualvm.heapviewer.java.InstanceNode;
import com.sun.tools.visualvm.heapviewer.java.InstanceNodeRenderer;
import com.sun.tools.visualvm.heapviewer.java.InstanceReferenceNode;
import com.sun.tools.visualvm.heapviewer.java.InstancesContainer;
import com.sun.tools.visualvm.heapviewer.java.JavaHeapFragment;
import com.sun.tools.visualvm.heapviewer.model.DataType;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNode;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNodeFilter;
import com.sun.tools.visualvm.heapviewer.model.Progress;
import com.sun.tools.visualvm.heapviewer.model.RootNode;
import com.sun.tools.visualvm.heapviewer.model.TextNode;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewPlugin;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerActions;
import com.sun.tools.visualvm.heapviewer.ui.TreeTableView;
import com.sun.tools.visualvm.heapviewer.ui.TreeTableViewColumn;
import com.sun.tools.visualvm.heapviewer.ui.UIThresholds;
import com.sun.tools.visualvm.heapviewer.utils.NodesComputer;
import com.sun.tools.visualvm.heapviewer.utils.ProgressIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "PathToGCRootPlugin_Name=GC Root",
    "PathToGCRootPlugin_Description=GC Root",
    "PathToGCRootPlugin_ProgressMsg=Computing nearest GC root...",
    "PathToGCRootPlugin_NoRoot=<no GC root>",
    "PathToGCRootPlugin_IsRoot=<node is GC root>",
    "PathToGCRootPlugin_NoSelection=<no class or instance selected>",
    "PathToGCRootPlugin_MoreNodes=<another {0} GC roots left>",
    "PathToGCRootPlugin_SamplesContainer=<sample {0} GC roots>",
    "PathToGCRootPlugin_NodesContainer=<GC roots {0}-{1}>"
})
public class PathToGCRootPlugin extends HeapViewPlugin {
    
    private final Heap heap;
    private HeapViewerNode selected;
    
    private final TreeTableView objectsView;
    
    private volatile boolean showingClass;
    
    private boolean gcRootsInitialized;
    private Thread currentWorker;
    
    private final Object workerLock = new Object();
    
    
    public PathToGCRootPlugin(HeapContext context, HeapViewerActions actions) {
        super(Bundle.PathToGCRootPlugin_Name(), Bundle.PathToGCRootPlugin_Description(), Icons.getIcon(ProfilerIcons.RUN_GC));
        
        heap = context.getFragment().getHeap();
        
        TreeTableViewColumn[] columns = new TreeTableViewColumn[] {
            new TreeTableViewColumn.Name(heap),
            new TreeTableViewColumn.LogicalValue(heap),
            new TreeTableViewColumn.Count(heap, true, true),
            new TreeTableViewColumn.OwnSize(heap, false, false),
            new TreeTableViewColumn.RetainedSize(heap, false, false),
            new TreeTableViewColumn.ObjectID(heap)
        };
        objectsView = new TreeTableView("java_objects_gcroots", context, actions, columns) { // NOI18N
            protected HeapViewerNode[] computeData(RootNode root, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) {
                synchronized (PathToGCRootPlugin.this) {
                    // interrupt previous computation if running
                    if (currentWorker != null) currentWorker.interrupt();
                    currentWorker = Thread.currentThread();
                }
                
                synchronized (workerLock) {
                    HeapViewerNode _selected;
                    synchronized (objectsView) { _selected = selected; }
                    
                    if (_selected == null) {
                        synchronized (PathToGCRootPlugin.this) { if (currentWorker == Thread.currentThread()) currentWorker = null; }
                        return new HeapViewerNode[] { new TextNode(Bundle.PathToGCRootPlugin_NoSelection()) };
                    }
                    
                    Instance instance;
                    if (_selected instanceof ClassNode || _selected instanceof InstancesContainer.Objects) {
                        instance = null;
                    } else {
                        instance = HeapViewerNode.getValue(_selected, DataType.INSTANCE, heap);
                        if (instance == null) {
                            synchronized (PathToGCRootPlugin.this) { if (currentWorker == Thread.currentThread()) currentWorker = null; }
                            return new HeapViewerNode[] { new TextNode(Bundle.PathToGCRootPlugin_NoSelection()) };
                        }
                    }
                    
                    // workaround - initialize GC roots before interrupting the worker thread:
                    //              instance.getNearestGCRootPointer() fails on Thread.interrupt()
                    if (!gcRootsInitialized) {
                        initializeGCRoots(heap);
                        gcRootsInitialized = true;
                    }
                    
                    Collection<HeapViewerNode> data;
                    if (instance != null) {
                        data = computeInstanceRoots(instance, progress);
                        if (data != null) showingClass = false;
                    } else if (_selected instanceof ClassNode) {
                        ClassNode node = (ClassNode)_selected;
                        data = computeInstancesRoots(node.getInstancesIterator(), node.getInstancesCount(), progress);
                        if (data != null) showingClass = true;
                    } else  {
                        InstancesContainer.Objects node = (InstancesContainer.Objects)_selected;
                        data = computeInstancesRoots(node.getInstancesIterator(), node.getCount(), progress);
                        if (data != null) showingClass = true;
                    }
                    
                    synchronized (PathToGCRootPlugin.this) { if (currentWorker == Thread.currentThread()) currentWorker = null; }

                    if (data == null) return null;
                    if (data.size() == 1) return new HeapViewerNode[] { data.iterator().next() };
                    
                    final Collection<HeapViewerNode> _data = data;
                    NodesComputer<HeapViewerNode> computer = new NodesComputer<HeapViewerNode>(_data.size(), UIThresholds.MAX_CLASS_INSTANCES) {
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
            }
            protected void childrenChanged() {
                if (!showingClass) fullyExpandNode((HeapViewerNode)getRoot());
            }
            protected void nodeExpanded(HeapViewerNode node) {
                if (showingClass && node instanceof GCInstanceNode) fullyExpandNode(node);
            }
            private void fullyExpandNode(HeapViewerNode node) {
                while (node != null) {
                    expandNode(node);
                    node = node.getNChildren() > 0 ? (HeapViewerNode)node.getChild(0) : null;
                }
            }
        };
    }
    
    
    @Override
    protected JComponent createComponent() {
        return objectsView.getComponent();
    }
        
    @Override
    protected void nodeSelected(final HeapViewerNode node, boolean adjusting) {
        synchronized (objectsView) { selected = node; }
        
        objectsView.reloadView();
    }
    
    
    private static Collection<HeapViewerNode> computeInstanceRoots(Instance instance, Progress progress) {
        Instance nextInstance = instance.getNearestGCRootPointer();
                    
        if (nextInstance == null) {
            return Collections.singleton((HeapViewerNode)new TextNode(Bundle.PathToGCRootPlugin_NoRoot()));
        } else if (nextInstance == instance) {
            return Collections.singleton((HeapViewerNode)new TextNode(Bundle.PathToGCRootPlugin_IsRoot()));
        } else {
            ToRoot node = null;
            HeapViewerNode firstNode = null;
            ToRoot previousNode = null;
            
            progress.setupUnknownSteps();

            Thread current = Thread.currentThread();
            while (!current.isInterrupted() && instance != nextInstance) {
                List<Value> references = instance.getReferences();
                for (Value reference : references) {
                    if (nextInstance.equals(reference.getDefiningInstance())) {
                        if (reference instanceof ObjectFieldValue) node = new FieldToRoot((ObjectFieldValue)reference);
                        else if (reference instanceof ArrayItemValue) node = new ArrayItemToRoot((ArrayItemValue)reference);

                        if (firstNode == null) firstNode = (HeapViewerNode)node;
                        else previousNode.setChildren(new HeapViewerNode[] { (HeapViewerNode)node });

                        break;
                    }
                }

                instance = nextInstance;
                nextInstance = instance.getNearestGCRootPointer();
                progress.step();

                previousNode = node;
            }
            node.setChildren(HeapViewerNode.NO_NODES);
            
            progress.finish();

            if (current.isInterrupted()) return null;
            else return Collections.singleton(firstNode);
        }
    }
    
    private static Collection<HeapViewerNode> computeInstancesRoots(Iterator<Instance> instances, int count, Progress progress) {
        Map<Instance, HeapViewerNode> gcRoots = new HashMap();
        
        progress.setupKnownSteps(count);
        
        Thread current = Thread.currentThread();
        while (!current.isInterrupted() && instances.hasNext()) {
            Instance instance = instances.next();
            Instance gcRoot = getGCRoot(instance, current);
            if (gcRoot != null) {
                GCRootNode gcRootNode = (GCRootNode)gcRoots.get(gcRoot);
                if (gcRootNode == null) {
                    gcRootNode = new GCRootNode(gcRoot);
                    gcRoots.put(gcRoot, gcRootNode);
                }
                gcRootNode.addInstance(instance);
            }
            progress.step();
        }
        
        progress.finish();
        
        if (current.isInterrupted()) return null;
        else if (!gcRoots.isEmpty()) return gcRoots.values();
        else return Collections.singleton((HeapViewerNode)new TextNode(Bundle.PathToGCRootPlugin_NoRoot()));
    }
    
    private static void initializeGCRoots(Heap heap) {
        assert !SwingUtilities.isEventDispatchThread();
        
        ProgressHandle pHandle = null;

        try {
            pHandle = ProgressHandle.createHandle(Bundle.PathToGCRootPlugin_ProgressMsg());
            pHandle.setInitialDelay(1000);
            pHandle.start(HeapProgress.PROGRESS_MAX);

            HeapFragment.setProgress(pHandle, 0);
            
            Instance dummy = (Instance)heap.getAllInstancesIterator().next();
            dummy.getNearestGCRootPointer();
        } finally {
            if (pHandle != null) pHandle.finish();
        }
    }
    
    private static Instance getGCRoot(Instance instance, Thread current) {
        Instance previousInstance = null;
        while (!current.isInterrupted() && instance != null && instance != previousInstance) {
            previousInstance = instance;
            instance = instance.getNearestGCRootPointer();
        }
        return instance;
    }
    
    @NbBundle.Messages({
        "GCRootNode_MoreNodes=<another {0} instances left>",
        "GCRootNode_SamplesContainer=<sample {0} instances>",
        "GCRootNode_NodesContainer=<instances {0}-{1}>"
    })
    static class GCRootNode extends InstanceNode {
        
        private final int maxNodes = UIThresholds.MAX_CONTAINER_INSTANCES;
        
        private final List<Instance> instances = new ArrayList();
        
        
        public GCRootNode(Instance gcRoot) {
            super(gcRoot);
        }
        
        
        void addInstance(Instance instance) {
            instances.add(instance);
        }
        
        
        public int getCount() {
            return instances.size();
        }
        
        
        protected HeapViewerNode[] computeChildren(RootNode root) {
            int itemsCount = instances.size();
            if (itemsCount <= maxNodes) {
                HeapViewerNode[] nodes = new HeapViewerNode[itemsCount];
                for (int i = 0; i < itemsCount; i++) nodes[i] = createNode(instances.get(i));
                return nodes;
            } else {
                return super.computeChildren(root);
            }
        }

        protected HeapViewerNode[] lazilyComputeChildren(Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) {
            final boolean isArray = getInstance().getJavaClass().isArray();
            NodesComputer<Instance> computer = new NodesComputer<Instance>(instances.size(), maxNodes) {
                protected boolean sorts(DataType dataType) {
                    if (DataType.COUNT.equals(dataType) || (DataType.OWN_SIZE.equals(dataType) && !isArray)) return false;
                    return true;
                }
                protected HeapViewerNode createNode(Instance object) {
                    return GCRootNode.this.createNode(object);
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
        
        private HeapViewerNode createNode(Instance instance) {
            return new GCInstanceNode(instance);
        }
        
        
        protected Object getValue(DataType type, Heap heap) {
            if (type == DataType.COUNT) return getCount();

            return super.getValue(type, heap);
        }
        
        
        static class Renderer extends InstanceNodeRenderer {
            
            private static final ImageIcon ICON = Icons.getImageIcon(ProfilerIcons.NODE_FORWARD);
        
            Renderer(Heap heap) {
                super(heap);
            }
            
            @Override
            protected ImageIcon getIcon(Instance instance, boolean isGCRoot) {
                return ICON;
            }

        }
        
    }
    
    private static class GCInstanceNode extends InstanceNode {
    
        public GCInstanceNode(Instance instance) {
            super(instance);
        }
        
        protected HeapViewerNode[] lazilyComputeChildren(Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) {
            Collection<HeapViewerNode> nodes = PathToGCRootPlugin.computeInstanceRoots(getInstance(), progress);
            return nodes == null ? null : nodes.toArray(HeapViewerNode.NO_NODES);
        }
    
    }
    
    private static interface ToRoot {
        
        public void setChildren(HeapViewerNode[] ch);
        
    }
    
    private static class FieldToRoot extends InstanceReferenceNode.Field implements ToRoot {
        
        public FieldToRoot(ObjectFieldValue value) {
            super(value, true);
        }
        
        public void setChildren(HeapViewerNode[] ch) {
            super.setChildren(ch);
        }
        
    }
    
    private static class ArrayItemToRoot extends InstanceReferenceNode.ArrayItem implements ToRoot {
        
        public ArrayItemToRoot(ArrayItemValue value) {
            super(value, true);
        } 
        
        public void setChildren(HeapViewerNode[] ch) {
            super.setChildren(ch);
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
