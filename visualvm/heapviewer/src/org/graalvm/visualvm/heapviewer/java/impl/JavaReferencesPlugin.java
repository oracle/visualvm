/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.ProfilerIcons;
import org.graalvm.visualvm.heapviewer.HeapContext;
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
import org.graalvm.visualvm.heapviewer.utils.ExcludingIterator;
import org.graalvm.visualvm.heapviewer.utils.HeapOperations;
import org.graalvm.visualvm.heapviewer.utils.HeapUtils;
import org.graalvm.visualvm.heapviewer.utils.InterruptibleIterator;
import org.graalvm.visualvm.heapviewer.utils.NodesComputer;
import org.graalvm.visualvm.heapviewer.utils.ProgressIterator;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.lib.jfluid.heap.Value;
import org.graalvm.visualvm.lib.ui.UIUtils;
import org.graalvm.visualvm.lib.ui.swing.renderer.LabelRenderer;
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
//    "JavaReferencesPlugin_NoReferencesFiltered=<merged references disabled>",
    "JavaReferencesPlugin_NoSelection=<no class or instance selected>",
    "JavaReferencesPlugin_MoreNodes=<another {0} references left>",
    "JavaReferencesPlugin_SamplesContainer=<sample {0} references>",
    "JavaReferencesPlugin_NodesContainer=<references {0}-{1}>",
    "JavaReferencesPlugin_IMoreNodes=<another {0} instances left>",
    "JavaReferencesPlugin_ISamplesContainer=<sample {0} instances>",
    "JavaReferencesPlugin_INodesContainer=<instances {0}-{1}>",
    "JavaReferencesPlugin_ComputeMergedReferencesLbl=Compute Merged References",
    "JavaReferencesPlugin_ComputeMergedReferencesTtp=Compute merged references for the selected class",
    "JavaReferencesPlugin_AutoComputeMergedReferencesLbl=Compute Merged References Automatically",
    "JavaReferencesPlugin_AutoComputeMergedReferencesTtp=Compute merged references automatically for each selected class",
    "JavaReferencesPlugin_MenuShowLogicalReferences=Show Logical References"
})
class JavaReferencesPlugin extends HeapViewPlugin {
    
    private static final TreeTableView.ColumnConfiguration CCONF_CLASS = new TreeTableView.ColumnConfiguration(DataType.COUNT, null, DataType.COUNT, SortOrder.DESCENDING, Boolean.FALSE);
    private static final TreeTableView.ColumnConfiguration CCONF_INSTANCE = new TreeTableView.ColumnConfiguration(null, DataType.COUNT, DataType.NAME, SortOrder.UNSORTED, null);
    
    private final Heap heap;
    private HeapViewerNode selected;
    
    private volatile boolean mergedRequest;
    
    private final TreeTableView objectsView;
    

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
                    
                    return new HeapViewerNode[] { new TextNode(Bundle.JavaReferencesPlugin_NoSelection()) };
                }
                
                InstancesWrapper wrapper = HeapViewerNode.getValue(_selected, DataType.INSTANCES_WRAPPER, heap);
                if (wrapper != null) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
//                            if (!mergedReferences && !CCONF_INSTANCE.equals(objectsView.getCurrentColumnConfiguration()))
//                                objectsView.configureColumns(CCONF_INSTANCE);
//                            else if (mergedReferences && !CCONF_CLASS.equals(objectsView.getCurrentColumnConfiguration()))
//                                objectsView.configureColumns(CCONF_CLASS);
                            if (!CCONF_CLASS.equals(objectsView.getCurrentColumnConfiguration()))
                                objectsView.configureColumns(CCONF_CLASS);
                        }
                    });

//                    if (!mergedReferences) return new HeapViewerNode[] { new TextNode(Bundle.JavaReferencesPlugin_NoReferencesFiltered()) };
                    
                    return computeInstancesReferences(wrapper, root, heap, viewID, null, dataTypes, sortOrders, progress);
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
                
                popup.add(new JCheckBoxMenuItem(Bundle.JavaReferencesPlugin_AutoComputeMergedReferencesLbl(), isAutoMerge()) {
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
                
                if (!CCONF_INSTANCE.equals(objectsView.getCurrentColumnConfiguration())) popup.add(new JCheckBoxMenuItem(Bundle.JavaReferencesPlugin_MenuShowLogicalReferences(), isLogicalReferences()) {
                    @Override
                    protected void fireActionPerformed(ActionEvent event) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                setLogicalReferences(isSelected());
                                if (CCONF_CLASS.equals(objectsView.getCurrentColumnConfiguration())) { // only update view for class selection
                                    reloadView();
                                }
                            }
                        });
                    }
                });
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
        
        JButton jb = new JButton(Bundle.JavaReferencesPlugin_ComputeMergedReferencesLbl(), Icons.getIcon(ProfilerIcons.NODE_REVERSE)) {
            protected void fireActionPerformed(ActionEvent e) {
                showObjectsView();
                objectsView.reloadView();
            }
        };
        jb.setIconTextGap(jb.getIconTextGap() + 2);
        jb.setToolTipText(Bundle.JavaReferencesPlugin_ComputeMergedReferencesTtp());
        Insets margin = jb.getMargin();
        if (margin != null) jb.setMargin(new Insets(margin.top + 3, margin.left + 3, margin.bottom + 3, margin.right + 3));
        
        
        LinkButton lb = new LinkButton(Bundle.JavaReferencesPlugin_AutoComputeMergedReferencesLbl()) {
            protected void fireActionPerformed(ActionEvent e) {
                setAutoMerge(true);
                showObjectsView();
                objectsView.reloadView();
            }
        };
        lb.setToolTipText(Bundle.JavaReferencesPlugin_AutoComputeMergedReferencesTtp());
                
        
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
    protected void closed() {
        synchronized (objectsView) { selected = objectsView.getRoot(); }
        objectsView.closed();
    }
    
    
    private static InterruptibleIterator<Instance> instancesIterator(InstancesWrapper instances) {
        return new InterruptibleIterator(instances.getInstancesIterator());
    }
    
    private HeapViewerNode[] computeInstancesReferences(final InstancesWrapper instances, RootNode root, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {
        HeapOperations.initializeReferences(heap);
        
        final Map<Long, Integer> values = new HashMap();
        
        try {        
            progress.setupKnownSteps(instances.getInstancesCount());

            InterruptibleIterator<Instance> instancesI = instancesIterator(instances);
            while (instancesI.hasNext()) {
                Instance instance = instancesI.next();
                progress.step();
                List<Value> references = instance.getReferences();
                Set<Instance> referrers = new HashSet();
                if (references.isEmpty()) {
                    referrers.add(null);
                } else for (Value reference : references) {
                    referrers.add(logicalReferrer(reference.getDefiningInstance()));
                }
                for (Instance referrer : referrers) {
                    long referrerID = referrer == null ? -1 : referrer.getInstanceId();
                    Integer count = values.get(referrerID);
                    if (count == null) count = 0;
                    values.put(referrerID, ++count);
                }
            }
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
        } catch (OutOfMemoryError e) {
            System.err.println("Out of memory in JavaReferencesPlugin: " + e.getMessage()); // NOI18N
            HeapUtils.handleOOME(true, e);
            return new HeapViewerNode[] { new ErrorNode.OOME() };
        } finally {
            progress.finish();
        }
        
        if (values.isEmpty()) return new HeapViewerNode[] { new TextNode(Bundle.JavaReferencesPlugin_NoReferences()) };
        
        NodesComputer<Map.Entry<Long, Integer>> computer = new NodesComputer<Map.Entry<Long, Integer>>(values.size(), UIThresholds.MAX_CLASS_INSTANCES) {
            protected boolean sorts(DataType dataType) {
                return true;
            }
            protected HeapViewerNode createNode(final Map.Entry<Long, Integer> node) {
                long referrerID = node.getKey();
                return new ReferenceNode(referrerID == -1 ? null : heap.getInstanceByID(referrerID)) {
                    @Override
                    int getCount() { return node.getValue(); }
                    @Override
                    InterruptibleIterator<Instance> instancesIterator() { return JavaReferencesPlugin.this.instancesIterator(instances); }
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
    
    
    protected void nodeSelected(HeapViewerNode node, boolean adjusting) {
        synchronized (objectsView) {
            if (Objects.equals(selected, node)) return;
            selected = node;
        }
        
        if (selected != null && !isAutoMerge() && HeapViewerNode.getValue(selected, DataType.INSTANCES_WRAPPER, heap) != null) showMergedView();
        else showObjectsView();
        
        objectsView.reloadView();
    }
    
    
    private static final String KEY_MERGED_REFERENCES = "HeapViewer.autoMergedReferences"; // NOI18N
    private static final String KEY_LOGICAL_REFERENCES = "HeapViewer.logicalReferences"; // NOI18N
    
    private boolean isAutoMerge() {
        return NbPreferences.root().getBoolean(KEY_MERGED_REFERENCES, false);
    }

    private void setAutoMerge(boolean value) {
        NbPreferences.root().putBoolean(KEY_MERGED_REFERENCES, value);
    }
    
    private boolean isLogicalReferences() {
        return NbPreferences.root().getBoolean(KEY_LOGICAL_REFERENCES, false);
    }

    private void setLogicalReferences(boolean value) {
        NbPreferences.root().putBoolean(KEY_LOGICAL_REFERENCES, value);
    }
    
    
    @NbBundle.Messages({
        "ReferenceNode_MoreNodes=<another {0} instances left>",
        "ReferenceNode_SamplesContainer=<sample {0} instances>",
        "ReferenceNode_NodesContainer=<instances {0}-{1}>"
    })
    private abstract class ReferenceNode extends InstanceNode.IncludingNull {
        
        ReferenceNode(Instance reference) {
            super(reference);
        }
        
        
        abstract int getCount();
        
        abstract InterruptibleIterator<Instance> instancesIterator();

        protected HeapViewerNode[] lazilyComputeChildren(Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {
            HeapOperations.initializeReferences(heap);
            
            NodesComputer<Instance> computer = new NodesComputer<Instance>(getCount(), UIThresholds.MAX_MERGED_OBJECTS) {
                protected boolean sorts(DataType dataType) {
                    return !DataType.COUNT.equals(dataType);
                }
                @Override
                protected HeapViewerNode createNode(Instance object) {
                    return new ReferredInstanceNode(object) {
                        @Override
                        Instance getReferrer() { return ReferenceNode.this.getInstance(); }
                    };
                }
                protected ProgressIterator<Instance> objectsIterator(int index, final Progress _progress) {
                    final Instance _instance = getInstance();
                    _progress.setupUnknownSteps();
                    Iterator<Instance> fieldInstanceIterator = new ExcludingIterator<Instance>(instancesIterator()) {
                        @Override
                        protected boolean exclude(Instance instance) {
                            _progress.step();
                            List<Value> references = instance.getReferences();
                            if (_instance == null) return !references.isEmpty();
                            for (Value reference : references)
                                if (_instance.equals(logicalReferrer(reference.getDefiningInstance())))
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
        
        public boolean isLeaf() {
            return false;
        }
        
    }
    
    private static class ReferenceNodeRenderer extends InstanceNodeRenderer {
            
        private static final ImageIcon ICON = Icons.getImageIcon(ProfilerIcons.NODE_FORWARD);

        ReferenceNodeRenderer(Heap heap) {
            super(heap);
        }
        
        @Override
        public void setValue(Object value, int row) {
            if (value != null) {
                ReferenceNode node = (ReferenceNode)value;
                if (node.getInstance() == null) {
                    setNormalValue(Bundle.JavaReferencesPlugin_NoReferences());
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
    
    private abstract class ReferredInstanceNode extends InstanceNode {
        
        ReferredInstanceNode(Instance instance) {
            super(instance);
        }
        
        abstract Instance getReferrer();
        
        protected HeapViewerNode[] lazilyComputeChildren(Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {
            HeapOperations.initializeReferences(heap);
            
            Instance referrer = getReferrer();
            if (referrer == null) return HeapViewerNode.NO_NODES;
            
            final List<Value> references = getInstance().getReferences();
            Iterator<Value> referencesI = references.iterator();
                while (referencesI.hasNext())
                    if (!referrer.equals(logicalReferrer(referencesI.next().getDefiningInstance())))
                        referencesI.remove();
            
            NodesComputer<Value> computer = new NodesComputer<Value>(references.size(), UIThresholds.MAX_MERGED_OBJECTS) {
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
        
        public boolean isLeaf() {
            return getReferrer() == null;
        }
        
    }
    
    
    private static final Set<String> COLLAPSED_ITEMS = new HashSet(Arrays.asList(new String[] {
        "java.util.HashMap$Node", // NOI18N
        "java.util.WeakHashMap$Entry" // NOI18N
    }));
    
    private Instance logicalReferrer(Instance realReferrer) {
        if (realReferrer == null) return null;
        return isLogicalReferences() ? logicalReferrerImpl(realReferrer) : realReferrer;
    }
    
    private Instance logicalReferrerImpl(Instance realReferrer) {
        JavaClass jclass = realReferrer.getJavaClass();
        
        if (jclass.isArray()) {
            Value reference = getDirectReferrer(realReferrer);
            if (reference != null) return logicalReferrerImpl(reference.getDefiningInstance());
        }
        
        if (COLLAPSED_ITEMS.contains(jclass.getName())) {
            Value reference = getDirectReferrer(realReferrer);
            if (reference != null) return logicalReferrerImpl(reference.getDefiningInstance());
        }
        
        return realReferrer;
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
