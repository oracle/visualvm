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

package org.graalvm.visualvm.heapviewer.java.impl;

import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.ProfilerIcons;
import org.graalvm.visualvm.heapviewer.HeapContext;
import org.graalvm.visualvm.heapviewer.java.ClassNode;
import org.graalvm.visualvm.heapviewer.java.InstanceNode;
import org.graalvm.visualvm.heapviewer.java.InstanceNodeRenderer;
import org.graalvm.visualvm.heapviewer.java.InstanceReferenceNode;
import org.graalvm.visualvm.heapviewer.java.InstancesContainer;
import org.graalvm.visualvm.heapviewer.java.JavaHeapFragment;
import org.graalvm.visualvm.heapviewer.model.DataType;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNode;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNodeFilter;
import org.graalvm.visualvm.heapviewer.model.Progress;
import org.graalvm.visualvm.heapviewer.model.RootNode;
import org.graalvm.visualvm.heapviewer.model.TextNode;
import org.graalvm.visualvm.heapviewer.ui.HeapViewPlugin;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerActions;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerRenderer;
import org.graalvm.visualvm.heapviewer.ui.TreeTableView;
import org.graalvm.visualvm.heapviewer.ui.TreeTableViewColumn;
import org.graalvm.visualvm.heapviewer.ui.UIThresholds;
import org.graalvm.visualvm.heapviewer.utils.ExcludingIterator;
import org.graalvm.visualvm.heapviewer.utils.InterruptibleIterator;
import org.graalvm.visualvm.heapviewer.utils.NodesComputer;
import org.graalvm.visualvm.heapviewer.utils.ProgressIterator;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.lib.jfluid.heap.Value;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "JavaReferencesPlugin_Name=References",
    "JavaReferencesPlugin_Description=References",
    "JavaReferencesPlugin_NoReferences=<no references>",
    "JavaReferencesPlugin_NoReferencesFiltered=<merged references disabled>",
    "JavaReferencesPlugin_NoSelection=<no class or instance selected>",
    "JavaReferencesPlugin_MoreNodes=<another {0} references left>",
    "JavaReferencesPlugin_SamplesContainer=<sample {0} references>",
    "JavaReferencesPlugin_NodesContainer=<references {0}-{1}>",
    "JavaReferencesPlugin_IMoreNodes=<another {0} instances left>",
    "JavaReferencesPlugin_ISamplesContainer=<sample {0} instances>",
    "JavaReferencesPlugin_INodesContainer=<instances {0}-{1}>",
    "JavaReferencesPlugin_MenuShowMergedReferences=Show Merged References",
    "JavaReferencesPlugin_MenuShowLogicalReferences=Show Logical References",
    "JavaReferencesPlugin_OOMEWarning=<too many references - increase heap size!>"
})
class JavaReferencesPlugin extends HeapViewPlugin {
    
    private static final TreeTableView.ColumnConfiguration CCONF_CLASS = new TreeTableView.ColumnConfiguration(DataType.COUNT, null, DataType.COUNT, SortOrder.DESCENDING, Boolean.FALSE);
    private static final TreeTableView.ColumnConfiguration CCONF_INSTANCE = new TreeTableView.ColumnConfiguration(null, DataType.COUNT, DataType.NAME, SortOrder.UNSORTED, null);
    
    private static final String KEY_MERGED_REFERENCES = "mergedReferences"; // NOI18N
    private static final String KEY_LOGICAL_REFERENCES = "logicalkReferences"; // NOI18N
    
    private volatile boolean mergedReferences = readItem(KEY_MERGED_REFERENCES, true);
    private volatile boolean logicalReferences = readItem(KEY_LOGICAL_REFERENCES, true);
    
    private final Heap heap;
    private HeapViewerNode selected;
    
    private final TreeTableView objectsView;
    
//    private volatile boolean referencesInitialized;
    

    public JavaReferencesPlugin(HeapContext context, HeapViewerActions actions, final JavaReferencesProvider provider) {
        super(Bundle.JavaReferencesPlugin_Name(), Bundle.JavaReferencesPlugin_Description(), Icons.getIcon(ProfilerIcons.NODE_REVERSE));
        
        heap = context.getFragment().getHeap();
        
        TreeTableViewColumn[] columns = new TreeTableViewColumn[] {
            new TreeTableViewColumn.Name(heap),
            new TreeTableViewColumn.LogicalValue(heap),
            new TreeTableViewColumn.Count(heap, true, true),
            new TreeTableViewColumn.OwnSize(heap, false, false),
            new TreeTableViewColumn.RetainedSize(heap, false, false),
            new TreeTableViewColumn.ObjectID(heap)
        };
        objectsView = new TreeTableView("java_objects_references", context, actions, columns) { // NOI18N
            protected HeapViewerNode[] computeData(RootNode root, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {
                HeapViewerNode _selected;
                synchronized (objectsView) { _selected = selected; }
                
                if (_selected == null) return new HeapViewerNode[] { new TextNode(Bundle.JavaReferencesPlugin_NoSelection()) };
                
//                // TODO: should be done once per heap, including the main Objects view!
//                if (!referencesInitialized) {
//                    initializeReferences(heap);
//                    referencesInitialized = true;
//                }
                
                if (_selected instanceof ClassNode || _selected instanceof InstancesContainer.Objects) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            if (!mergedReferences && !CCONF_INSTANCE.equals(objectsView.getCurrentColumnConfiguration()))
                                objectsView.configureColumns(CCONF_INSTANCE);
                            else if (mergedReferences && !CCONF_CLASS.equals(objectsView.getCurrentColumnConfiguration()))
                                objectsView.configureColumns(CCONF_CLASS);
                        }
                    });

                    if (!mergedReferences) return new HeapViewerNode[] { new TextNode(Bundle.JavaReferencesPlugin_NoReferencesFiltered()) };
                    
                    return computeInstancesReferences(InstancesWrapper.fromNode(_selected), root, heap, viewID, null, dataTypes, sortOrders, progress);
                } else {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            if (!CCONF_INSTANCE.equals(objectsView.getCurrentColumnConfiguration()))
                                objectsView.configureColumns(CCONF_INSTANCE);
                        }
                    });
                    
                    Instance instance = HeapViewerNode.getValue(_selected, DataType.INSTANCE, heap);

                    if (instance != null) {
                        HeapViewerNode[] nodes = provider.getNodes(instance, root, heap, viewID, null, dataTypes, sortOrders, progress);
                        return nodes == null || nodes.length == 0 ? new HeapViewerNode[] { new TextNode(Bundle.JavaReferencesPlugin_NoReferences()) } : nodes;
                    }

                    return new HeapViewerNode[] { new TextNode(Bundle.JavaReferencesPlugin_NoSelection()) };
                }
            }
            
            @Override
            protected void populatePopup(HeapViewerNode node, JPopupMenu popup) {
                if (popup.getComponentCount() > 0) popup.addSeparator();
                
                popup.add(new JCheckBoxMenuItem(Bundle.JavaReferencesPlugin_MenuShowMergedReferences(), mergedReferences) {
                    @Override
                    protected void fireActionPerformed(ActionEvent event) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                mergedReferences = isSelected();
                                storeItem(KEY_MERGED_REFERENCES, mergedReferences);
                                reloadView();
                            }
                        });
                    }
                });
                
                if (!CCONF_INSTANCE.equals(objectsView.getCurrentColumnConfiguration())) popup.add(new JCheckBoxMenuItem(Bundle.JavaReferencesPlugin_MenuShowLogicalReferences(), logicalReferences) {
                    @Override
                    protected void fireActionPerformed(ActionEvent event) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                logicalReferences = isSelected();
                                storeItem(KEY_LOGICAL_REFERENCES, logicalReferences);
                                reloadView();
                            }
                        });
                    }
                });
            }
        };
    }

    protected JComponent createComponent() {
        return objectsView.getComponent();
    }
    
    
//    private static void initializeReferences(Heap heap) {
//        assert !SwingUtilities.isEventDispatchThread();
//        
//        ProgressHandle pHandle = null;
//
//        try {
//            pHandle = ProgressHandle.createHandle(Bundle.PathToGCRootPlugin_ProgressMsg());
//            pHandle.setInitialDelay(1000);
//            pHandle.start(HeapProgress.PROGRESS_MAX);
//
//            HeapFragment.setProgress(pHandle, 0);
//            
//            Instance dummy = (Instance)heap.getAllInstancesIterator().next();
//            dummy.getReferences();
//        } finally {
//            if (pHandle != null) pHandle.finish();
//        }
//    }
    
    private HeapViewerNode[] computeInstancesReferences(final InstancesWrapper instances, RootNode root, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {
        final Map<Long, Integer> values = new HashMap();
                
        progress.setupKnownSteps(instances.getInstancesCount());

        Iterator<Instance> instancesI = instances.getInstancesIterator();
        try {
            computingChildren = true;
            while (computingChildren && instancesI.hasNext()) {
                Instance instance = instancesI.next();
                progress.step();
                List<Value> references = instance.getReferences();
                Set<Instance> referers = new HashSet();
                for (Value reference : references) {
                    if (!computingChildren) break;
                    referers.add(logicalReferer(reference.getDefiningInstance()));
                }
                for (Instance referer : referers) {
                    if (!computingChildren) break;
                    long refererID = referer.getInstanceId();
                    Integer count = values.get(refererID);
                    if (count == null) count = 0;
                    values.put(refererID, ++count);
                }
            }
            if (!computingChildren) return null;
        } catch (OutOfMemoryError e) {
            return new HeapViewerNode[] { new TextNode(Bundle.JavaReferencesPlugin_OOMEWarning()) };
        } finally {
            computingChildren = false;
            progress.finish();
        }
        
        NodesComputer<Map.Entry<Long, Integer>> computer = new NodesComputer<Map.Entry<Long, Integer>>(values.size(), UIThresholds.MAX_CLASS_INSTANCES) {
            protected boolean sorts(DataType dataType) {
                return true;
            }
            protected HeapViewerNode createNode(final Map.Entry<Long, Integer> node) {
                return new ReferenceNode(heap.getInstanceByID(node.getKey())) {
                    @Override
                    int getCount() { return node.getValue(); }
                    @Override
                    Iterator<Instance> instancesIterator() { return instances.getInstancesIterator(); }
                };
            }
            protected ProgressIterator<Map.Entry<Long, Integer>> objectsIterator(int index, Progress progress) {
                Iterator<Map.Entry<Long, Integer>> iterator = values.entrySet().iterator();
                return new ProgressIterator(iterator, index, true, progress);
            }
            protected String getMoreNodesString(String moreNodesCount)  {
                return Bundle.JavaReferencesPlugin_MoreNodes(moreNodesCount);
            }
            protected String getSamplesContainerString(String objectsCount)  {
                return Bundle.JavaReferencesPlugin_SamplesContainer(objectsCount);
            }
            protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                return Bundle.JavaReferencesPlugin_NodesContainer(firstNodeIdx, lastNodeIdx);
            }
        };

        return computer.computeNodes(root, heap, viewID, null, dataTypes, sortOrders, progress);
    }
    
    
    private volatile boolean computingChildren;
    
    
    protected void nodeSelected(HeapViewerNode node, boolean adjusting) {
        synchronized (objectsView) {
            if (Objects.equals(selected, node)) return;
            
            computingChildren = false;
            selected = node;
        }
        
        objectsView.reloadView();
    }
    
    
    private static boolean readItem(String itemName, boolean initial) {
        return NbPreferences.forModule(JavaFieldsPlugin.class).getBoolean("JavaReferencesPlugin." + itemName, initial); // NOI18N
    }

    private static void storeItem(String itemName, boolean value) {
        NbPreferences.forModule(JavaFieldsPlugin.class).putBoolean("JavaReferencesPlugin." + itemName, value); // NOI18N
    }
    
    
    private static abstract class InstancesWrapper {
        abstract JavaClass getJavaClass();
        abstract int getInstancesCount();
        abstract Iterator<Instance> getInstancesIterator();
        
        private static InstancesWrapper fromClassNode(final ClassNode node) {
            return new InstancesWrapper() {
                @Override
                JavaClass getJavaClass() { return node.getJavaClass(); }
                @Override
                int getInstancesCount() { return node.getInstancesCount(); }
                @Override
                Iterator<Instance> getInstancesIterator() { return node.getInstancesIterator(); }
            };
        }
        private static InstancesWrapper fromInstancesContainer(final InstancesContainer.Objects node) {
            return new InstancesWrapper() {
                @Override
                JavaClass getJavaClass() { return node.getJavaClass(); }
                @Override
                int getInstancesCount() { return node.getCount(); }
                @Override
                Iterator<Instance> getInstancesIterator() { return node.getInstancesIterator(); }
            };
        }
        static InstancesWrapper fromNode(HeapViewerNode node) {
            if (node instanceof ClassNode) return fromClassNode((ClassNode)node);
            else if (node instanceof InstancesContainer.Objects) return fromInstancesContainer((InstancesContainer.Objects)node);
            else return null;
        }
    }
    
    
    @NbBundle.Messages({
        "ReferenceNode_MoreNodes=<another {0} instances left>",
        "ReferenceNode_SamplesContainer=<sample {0} instances>",
        "ReferenceNode_NodesContainer=<instances {0}-{1}>"
    })
    private abstract class ReferenceNode extends InstanceNode {
        
        ReferenceNode(Instance reference) {
            super(reference);
        }
        
        
        abstract int getCount();
        
        abstract Iterator<Instance> instancesIterator();

        protected HeapViewerNode[] lazilyComputeChildren(Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {
            NodesComputer<Instance> computer = new NodesComputer<Instance>(getCount(), 20) {
                protected boolean sorts(DataType dataType) {
                    return !DataType.COUNT.equals(dataType);
                }
                @Override
                protected HeapViewerNode createNode(Instance object) {
                    return new ReferredInstanceNode(object) {
                        @Override
                        Instance getReferer() { return ReferenceNode.this.getInstance(); }
                    };
                }
                protected ProgressIterator<Instance> objectsIterator(int index, Progress _progress) {
                    final Instance _instance = getInstance();
                    Iterator<Instance> fieldInstanceIterator = new ExcludingIterator<Instance>(new InterruptibleIterator(instancesIterator())) {
                        @Override
                        protected boolean exclude(Instance instance) {
                            List<Value> references = instance.getReferences();
                            for (Value reference : references)
                                if (_instance.equals(logicalReferer(reference.getDefiningInstance())))
                                    return false;
                            return true;
                        }
                    };
                    return new ProgressIterator(fieldInstanceIterator, index, true, _progress);
                }
                protected String getMoreNodesString(String moreNodesCount)  {
                    return Bundle.JavaReferencesPlugin_IMoreNodes(moreNodesCount);
                }
                protected String getSamplesContainerString(String objectsCount)  {
                    return Bundle.JavaReferencesPlugin_ISamplesContainer(objectsCount);
                }
                protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                    return Bundle.JavaReferencesPlugin_INodesContainer(firstNodeIdx, lastNodeIdx);
                }
            };
            
            return computer.computeNodes(ReferenceNode.this, heap, viewID, null, dataTypes, sortOrders, progress);
        }
        
        
        protected Object getValue(DataType type, Heap heap) {
            if (type == DataType.COUNT) return getCount();

            return super.getValue(type, heap);
        }
        
    }
    
    private static class ReferenceNodeRenderer extends InstanceNodeRenderer {
            
        private static final ImageIcon ICON = Icons.getImageIcon(ProfilerIcons.NODE_FORWARD);

        ReferenceNodeRenderer(Heap heap) {
            super(heap);
        }

        @Override
        protected ImageIcon getIcon(Instance instance, boolean isGCRoot) {
            return ICON;
        }

    }
    
    private abstract class ReferredInstanceNode extends InstanceNode {
        
        ReferredInstanceNode(Instance instance) {
            super(instance);
        }
        
        abstract Instance getReferer();
        
        protected HeapViewerNode[] lazilyComputeChildren(Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {
            Instance referer = getReferer();
            final List<Value> references = getInstance().getReferences();
            Iterator<Value> referencesI = references.iterator();
            while (referencesI.hasNext())
                if (!referer.equals(logicalReferer(referencesI.next().getDefiningInstance())))
                    referencesI.remove();
            
            NodesComputer<Value> computer = new NodesComputer<Value>(references.size(), 20) {
                protected boolean sorts(DataType dataType) {
                    return !DataType.COUNT.equals(dataType);
                }
                protected HeapViewerNode createNode(Value object) {
                    return InstanceReferenceNode.incoming(object);
                }
                protected ProgressIterator<Value> objectsIterator(int index, Progress progress) {
                    Iterator<Value> iterator = references.listIterator(index);
                    return new ProgressIterator(iterator, index, false, progress);
                }
                protected String getMoreNodesString(String moreNodesCount)  {
                    return Bundle.JavaReferencesPlugin_MoreNodes(moreNodesCount);
                }
                protected String getSamplesContainerString(String objectsCount)  {
                    return Bundle.JavaReferencesPlugin_SamplesContainer(objectsCount);
                }
                protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                    return Bundle.JavaReferencesPlugin_NodesContainer(firstNodeIdx, lastNodeIdx);
                }
            };
            
            return computer.computeNodes(ReferredInstanceNode.this, heap, viewID, null, dataTypes, sortOrders, progress);
        }
        
    }
    
    
    private static final Set<String> COLLAPSED_ITEMS = new HashSet(Arrays.asList(new String[] {
        "java.util.HashMap$Node", // NOI18N
        "java.util.WeakHashMap$Entry" // NOI18N
    }));
    
    private Instance logicalReferer(Instance realReferer) {
        return logicalReferences ? logicalRefererImpl(realReferer) : realReferer;
    }
    
    private Instance logicalRefererImpl(Instance realReferer) {
        JavaClass jclass = realReferer.getJavaClass();
        
        if (jclass.isArray()) {
            Value reference = getDirectReferrer(realReferer);
            if (reference != null) return logicalRefererImpl(reference.getDefiningInstance());
        }
        
        if (COLLAPSED_ITEMS.contains(jclass.getName())) {
            Value reference = getDirectReferrer(realReferer);
            if (reference != null) return logicalRefererImpl(reference.getDefiningInstance());
        }
        
        return realReferer;
    }
    
    private static Value getDirectReferrer(Instance instance) {
        List<Value> references = instance.getReferences();
        return references.size() == 1 ? references.get(0) : null;
    }
    
    
    @ServiceProvider(service=HeapViewerRenderer.Provider.class)
    public static class JavaReferencesRendererProvider extends HeapViewerRenderer.Provider {

        @Override
        public boolean supportsView(HeapContext context, String viewID) {
            return "java_objects_references".equals(viewID); // NOI18N
        }

        @Override
        public void registerRenderers(Map<Class<? extends HeapViewerNode>, HeapViewerRenderer> renderers, HeapContext context) {
            renderers.put(ReferenceNode.class, new ReferenceNodeRenderer(context.getFragment().getHeap()));
        }
        
    }
    
    
    @ServiceProvider(service=HeapViewPlugin.Provider.class, position = 300)
    public static class Provider extends HeapViewPlugin.Provider {

        public HeapViewPlugin createPlugin(HeapContext context, HeapViewerActions actions, String viewID) {
            if (!viewID.startsWith("diff") && JavaHeapFragment.isJavaHeap(context)) { // NOI18N
                JavaReferencesProvider provider = Lookup.getDefault().lookup(JavaReferencesProvider.class);
                return new JavaReferencesPlugin(context, actions, provider);
            }
            return null;
        }
        
    }
    
}
