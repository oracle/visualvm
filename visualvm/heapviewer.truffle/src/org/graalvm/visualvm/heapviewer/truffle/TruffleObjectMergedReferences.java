/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.heapviewer.truffle;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.SortOrder;
import org.graalvm.visualvm.heapviewer.HeapContext;
import org.graalvm.visualvm.heapviewer.java.InstanceNode;
import org.graalvm.visualvm.heapviewer.model.DataType;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNode;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNodeFilter;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNodeWrapper;
import org.graalvm.visualvm.heapviewer.model.Progress;
import org.graalvm.visualvm.heapviewer.model.RootNode;
import org.graalvm.visualvm.heapviewer.model.TextNode;
import org.graalvm.visualvm.heapviewer.truffle.nodes.TruffleObjectReferenceNode;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerRenderer;
import org.graalvm.visualvm.heapviewer.ui.UIThresholds;
import org.graalvm.visualvm.heapviewer.utils.ExcludingIterator;
import org.graalvm.visualvm.heapviewer.utils.InterruptibleIterator;
import org.graalvm.visualvm.heapviewer.utils.NodesComputer;
import org.graalvm.visualvm.heapviewer.utils.ProgressIterator;
import org.graalvm.visualvm.lib.jfluid.heap.FieldValue;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.ProfilerIcons;
import org.graalvm.visualvm.lib.ui.swing.renderer.NormalBoldGrayRenderer;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "TruffleObjectMergedReferences_NoReferences=<no references>"
})
abstract class TruffleObjectMergedReferences<O extends TruffleObject> {
    
    private final Heap heap;
    private final TruffleObjectsWrapper<O> objects;
    
    
    TruffleObjectMergedReferences(TruffleObjectsWrapper<O> objects, Heap heap) {
        this.objects = objects;
        this.heap = heap;
    }
    
    
    protected abstract String getMoreNodesString(String moreNodesCount);
    protected abstract String getSamplesContainerString(String objectsCount);
    protected abstract String getNodesContainerString(String firstNodeIdx, String lastNodeIdx);
    
    protected abstract TruffleLanguage getLanguage();
    
    protected abstract boolean filtersReferences();
    protected abstract boolean includeReference(FieldValue field);
    protected abstract Collection<FieldValue> getReferences(O object) throws InterruptedException;
    
    protected abstract HeapViewerNode createForeignReferenceNode(Instance instance, FieldValue field);
    
    
    private int objectsCount() { return objects.getObjectsCount(); }
    private Iterator<O> objectsIterator() { return new InterruptibleIterator(objects.getObjectsIterator()); }
    
    private HeapViewerNode createObjectNode(O object) {
        return (HeapViewerNode)getLanguage().createObjectNode(object, object.getType(heap));
    }
    
    protected HeapViewerNode[] getNodes(HeapViewerNode parent, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {
        boolean filtersReferences = filtersReferences();

        final Map<Long, Integer> values = new HashMap();
        FieldValue refFV = null;
        
        Iterator<O> objectsI = objectsIterator();

        progress.setupKnownSteps(objectsCount());
        try {
            while (objectsI.hasNext()) {
                O object = objectsI.next();
                progress.step();
                Collection<FieldValue> references = getReferences(object);
                Set<Instance> referers = new HashSet();
                if (references.isEmpty()) {
                    referers.add(null);
                } else for (FieldValue reference : references) {
                    if (refFV == null) refFV = reference;
                    if (!filtersReferences || includeReference(reference))
                        referers.add(reference.getDefiningInstance());
                }
                for (Instance referer : referers) {
                    long refererID = referer == null ? -1 : referer.getInstanceId();
                    Integer count = values.get(refererID);
                    if (count == null) count = 0;
                    values.put(refererID, ++count);
                }
            }
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
        } catch (OutOfMemoryError e) {
            return new HeapViewerNode[] { new TextNode(Bundle.TruffleObjectPropertyProvider_OOMEWarning()) };
        } finally {
            progress.finish();
        }

        final TruffleLanguage language = getLanguage();
        final FieldValue refFVF = refFV;

        NodesComputer<Map.Entry<Long, Integer>> computer = new NodesComputer<Map.Entry<Long, Integer>>(values.size(), UIThresholds.MAX_CLASS_INSTANCES) {
            protected boolean sorts(DataType dataType) {
                return true;
            }
            protected HeapViewerNode createNode(final Map.Entry<Long, Integer> node) {
                long refererID = node.getKey();
                final Instance instance = refererID == -1 ? null : heap.getInstanceByID(refererID);
                HeapViewerNode ref;
                if (instance == null) {
                    ref = new InstanceNode.IncludingNull(null);
                } else if (language.isLanguageObject(instance)) {
                    ref = createObjectNode((O)language.createObject(instance));
                } else {
                    // see for example RObjectProperties.ReferencesProvider.createForeignReferenceNode
                    ref = createForeignReferenceNode(instance, refFVF);
                    if (ref instanceof TruffleObjectReferenceNode.InstanceBased) {
                        Instance i = ((TruffleObjectReferenceNode.InstanceBased)ref).getInstance();
                        ref = createObjectNode((O)language.createObject(i));
                    } else {
                        ref = new InstanceNode(instance);
                    }
                }

                return new MergedObjectReferenceNode(ref) {
                    @Override
                    public Instance getInstance() { return instance; }
                    @Override
                    public int getCount() { return node.getValue(); }
                };
            }
            protected ProgressIterator<Map.Entry<Long, Integer>> objectsIterator(int index, Progress progress) {
                Iterator<Map.Entry<Long, Integer>> iterator = values.entrySet().iterator();
                return new ProgressIterator(iterator, index, true, progress);
            }
            protected String getMoreNodesString(String moreNodesCount)  {
                return TruffleObjectMergedReferences.this.getMoreNodesString(moreNodesCount);
            }
            protected String getSamplesContainerString(String objectsCount)  {
                return TruffleObjectMergedReferences.this.getSamplesContainerString(objectsCount);
            }
            protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                return TruffleObjectMergedReferences.this.getNodesContainerString(firstNodeIdx, lastNodeIdx);
            }
        };

        return computer.computeNodes(parent, heap, viewID, null, dataTypes, sortOrders, progress);
    }
    
    
    private abstract class MergedObjectReferenceNode extends HeapViewerNodeWrapper {        
        
        MergedObjectReferenceNode(HeapViewerNode reference) {
            super(reference);
        }
        
        
        public abstract Instance getInstance();
        
        public abstract int getCount();
        
        
        protected HeapViewerNode[] lazilyComputeChildren(Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {
            NodesComputer<O> computer = new NodesComputer<O>(getCount(), UIThresholds.MAX_MERGED_OBJECTS) {
                protected boolean sorts(DataType dataType) {
                    return !DataType.COUNT.equals(dataType);
                }
                @Override
                protected HeapViewerNode createNode(O object) {
                    return createObjectNode(object);
                }
                protected ProgressIterator<O> objectsIterator(int index, Progress _progress) {
                    final Instance referer = getInstance();
                    progress.setupUnknownSteps();
                    Iterator<O> referencesIt = new ExcludingIterator<O>(new InterruptibleIterator(TruffleObjectMergedReferences.this.objectsIterator())) {
                        @Override
                        protected boolean exclude(O object) {
                            progress.step();
                            try {
                                Collection<FieldValue> references = getReferences(object);
                                if (referer == null) return !references.isEmpty();
                                for (FieldValue reference : references) {
                                    if (referer.equals(reference.getDefiningInstance()))
                                        return false;
                                }
                            } catch (InterruptedException e) {}
                            return true;
                        }
                    };
                    return new ProgressIterator(referencesIt, index, true, _progress);
                }
                protected String getMoreNodesString(String moreNodesCount)  {
                    return Bundle.TruffleObjectPropertyProvider_IMoreNodes(moreNodesCount);
                }
                protected String getSamplesContainerString(String objectsCount)  {
                    return Bundle.TruffleObjectPropertyProvider_ISamplesContainer(objectsCount);
                }
                protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                    return Bundle.TruffleObjectPropertyProvider_INodesContainer(firstNodeIdx, lastNodeIdx);
                }
            };
            
            return computer.computeNodes(MergedObjectReferenceNode.this, heap, viewID, null, dataTypes, sortOrders, progress);
        }
        
        protected Object getValue(DataType type, Heap heap) {
            if (type == DataType.COUNT) return getCount();
            
            return super.getValue(type, heap);
        }
        
    }
    
    
    private static class MergedObjectReferenceNodeRenderer extends NormalBoldGrayRenderer implements HeapViewerRenderer {
        
        private HeapViewerRenderer renderer;

        @Override
        public void setValue(Object value, int row) {
            TruffleObjectMergedReferences.MergedObjectReferenceNode vnode = (TruffleObjectMergedReferences.MergedObjectReferenceNode)value;
            HeapViewerNode node = vnode.getNode();
            renderer = RootNode.get(vnode).resolveRenderer(node);
            renderer.setValue(node, row);
            
            if (node instanceof InstanceNode.IncludingNull) {
                setNormalValue(Bundle.TruffleObjectMergedReferences_NoReferences());
                setBoldValue(""); // NOI18N
                setGrayValue(""); // NOI18N
            } else if (renderer instanceof NormalBoldGrayRenderer) {
                NormalBoldGrayRenderer r = (NormalBoldGrayRenderer)renderer;
                setNormalValue(r.getNormalValue());
                setBoldValue(r.getBoldValue());
                setGrayValue(r.getGrayValue());
            } else {
                HeapViewerRenderer r = (HeapViewerRenderer)renderer;
                setNormalValue(r.getShortName());
                setBoldValue(""); // NOI18N
                setGrayValue(""); // NOI18N
            }
            setIcon(Icons.getIcon(ProfilerIcons.NODE_FORWARD));
        }

        @Override
        public int getHorizontalAlignment() {
            return renderer.getHorizontalAlignment();
        }
        
//        @Override
//        public String toString() {
//            return renderer.toString();
//        }
//
//        @Override
//        public String getShortName() {
//            return renderer.getShortName();
//        }
//
//        @Override
//        public AccessibleContext getAccessibleContext() {
//            return renderer.getAccessibleContext();
//        }
        
    }
    
    
    @ServiceProvider(service=HeapViewerRenderer.Provider.class)
    public static class MergedReferencesNodeRendererProvider extends HeapViewerRenderer.Provider {

        @Override
        public boolean supportsView(HeapContext context, String viewID) {
            return true;
        }

        @Override
        public void registerRenderers(Map<Class<? extends HeapViewerNode>, HeapViewerRenderer> renderers, HeapContext context) {
            renderers.put(TruffleObjectMergedReferences.MergedObjectReferenceNode.class, new MergedObjectReferenceNodeRenderer());
        }
        
    }
    
}
