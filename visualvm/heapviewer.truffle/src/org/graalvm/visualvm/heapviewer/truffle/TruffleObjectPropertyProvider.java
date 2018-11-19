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

import org.graalvm.visualvm.heapviewer.truffle.nodes.TerminalJavaNodes;
import org.graalvm.visualvm.heapviewer.HeapFragment;
import org.graalvm.visualvm.heapviewer.java.PrimitiveNode;
import org.graalvm.visualvm.heapviewer.model.DataType;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNode;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNodeFilter;
import org.graalvm.visualvm.heapviewer.model.Progress;
import org.graalvm.visualvm.heapviewer.truffle.dynamicobject.DynamicObject;
import org.graalvm.visualvm.heapviewer.truffle.dynamicobject.DynamicObjectFieldNode;
import org.graalvm.visualvm.heapviewer.truffle.dynamicobject.DynamicObjectReferenceNode;
import org.graalvm.visualvm.heapviewer.ui.UIThresholds;
import org.graalvm.visualvm.heapviewer.utils.NodesComputer;
import org.graalvm.visualvm.heapviewer.utils.ProgressIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.swing.SortOrder;
import org.netbeans.api.progress.ProgressHandle;
import org.graalvm.visualvm.lib.jfluid.heap.FieldValue;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.HeapProgress;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.ObjectFieldValue;
import org.graalvm.visualvm.lib.jfluid.heap.PrimitiveArrayInstance;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "TruffleObjectPropertyProvider_ComputingNodes=<Computing {0}...>", // <Computing items...>
    "TruffleObjectPropertyProvider_MoreNodes=<another {0} {1} left>", // <another 1234 items left>
    "TruffleObjectPropertyProvider_SamplesContainer=<sample {0} {1}>", // <sample 1234 items>
    "TruffleObjectPropertyProvider_NodesContainer=<{2} {0}-{1}>", // <items 1001 - 2000>
    "TruffleObjectPropertyProvider_OOMEWarning=<too many references - increase heap size!>",
    "TruffleObjectPropertyProvider_IMoreNodes=<another {0} objects left>",
    "TruffleObjectPropertyProvider_ISamplesContainer=<sample {0} objects>",
    "TruffleObjectPropertyProvider_INodesContainer=<objects {0}-{1}>",
    "TruffleObjectPropertyProvider_ValuesCountHint=({0} values)",
    "TruffleObjectPropertyProvider_FieldHistogramMoreNodes=<another {0} values left>",
    "TruffleObjectPropertyProvider_FieldHistogramSamplesContainer=<sample {0} values>",
    "TruffleObjectPropertyProvider_FieldHistogramNodesContainer=<values {0}-{1}>"
})
public abstract class TruffleObjectPropertyProvider<O extends TruffleObject, T extends TruffleType<O>, F extends TruffleLanguageHeapFragment<O, T>, L extends TruffleLanguage<O, T, F>, I> extends HeapViewerNode.Provider {
    
    private final Class<O> objectClass;
    
    private final L language;
    
    private final String propertyName;
    private final int maxPropertyItems;
    
    private final boolean displaysProgress;
    private final boolean filtersProperties;
    
    
    protected TruffleObjectPropertyProvider(String propertyName, L language, boolean displaysProgress, boolean filtersProperties, int maxPropertyItems) {
        this.language = language;
        this.objectClass = language.getLanguageObjectClass();
        this.propertyName = propertyName;
        this.maxPropertyItems = maxPropertyItems;
        this.displaysProgress = displaysProgress;
        this.filtersProperties = filtersProperties;
    }
    

    @Override
    public String getName() {
        return propertyName;
    }
    
    
    protected final L getLanguage() {
        return language;
    }

    
    @Override
    public abstract boolean supportsView(Heap heap, String viewID);

    @Override
    public abstract boolean supportsNode(HeapViewerNode node, Heap heap, String viewID);
    
    
    protected abstract Collection<I> getPropertyItems(O object, Heap heap) throws InterruptedException;
    
    protected boolean includeItem(I item) { return true; }
    
    protected abstract HeapViewerNode createNode(I item, Heap heap);
    
    
    protected boolean supportsAggregation() { return true; }
    
    
    protected final boolean filtersProperties() { return filtersProperties; }
    
    protected final String moreNodesString(String moreNodesCount) { return Bundle.TruffleObjectPropertyProvider_MoreNodes(moreNodesCount, propertyName); }
    protected final String samplesContainerString(String objectsCount) { return Bundle.TruffleObjectPropertyProvider_SamplesContainer(objectsCount, propertyName); }
    protected final String nodesContainerString(String firstNodeIdx, String lastNodeIdx) { return Bundle.TruffleObjectPropertyProvider_NodesContainer(firstNodeIdx, lastNodeIdx, propertyName); }

    
    @Override
    public final HeapViewerNode[] getNodes(HeapViewerNode parent, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {
        O object = getObject(parent, heap);
        return object == null ? null : getNodes(object, parent, heap, viewID, viewFilter, dataTypes, sortOrders, progress);
    }
    
    final HeapViewerNode[] getNodes(O object, HeapViewerNode parent, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {
        Collection<I> itemsC = null;
        
        if (!displaysProgress) {
            itemsC = getPropertyItems(object, heap);
        } else {
            ProgressHandle pHandle = ProgressHandle.createHandle(Bundle.TruffleObjectPropertyProvider_ComputingNodes(propertyName));
            pHandle.setInitialDelay(1000);
            pHandle.start(HeapProgress.PROGRESS_MAX);
            HeapFragment.setProgress(pHandle, 0);

            try { itemsC = getPropertyItems(object, heap); }
            finally { pHandle.finish(); }
        }
        
        if (itemsC == null) return null;
        
        final List<I> items = new ArrayList(itemsC);
        
        if (filtersProperties) {
            Iterator<I> itemsIt = items.iterator();
            while (itemsIt.hasNext()) if (!includeItem(itemsIt.next())) itemsIt.remove();
        }
        
        NodesComputer<Integer> computer = new NodesComputer<Integer>(items.size(), maxPropertyItems) {
            protected boolean sorts(DataType dataType) {
                return !DataType.COUNT.equals(dataType);
            }
            protected HeapViewerNode createNode(Integer index) {
                return TruffleObjectPropertyProvider.this.createNode(items.get(index), heap);
            }
            protected ProgressIterator<Integer> objectsIterator(int index, Progress progress) {
                Iterator<Integer> iterator = integerIterator(index, items.size());
                return new ProgressIterator(iterator, index, false, progress);
            }
            protected String getMoreNodesString(String moreNodesCount)  { return moreNodesString(moreNodesCount); }
            protected String getSamplesContainerString(String objectsCount)  { return samplesContainerString(objectsCount); }
            protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  { return nodesContainerString(firstNodeIdx, lastNodeIdx); }
        };

        return computer.computeNodes(parent, heap, viewID, null, dataTypes, sortOrders, progress);
    }
    
    protected HeapViewerNode[] getNodes(TruffleObjectsWrapper<O> objects, HeapViewerNode parent, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {
        return null;
    }
    
    final O getObject(HeapViewerNode node, Heap heap) {
        if (node == null) return null;
            
        TruffleObject object = HeapViewerNode.getValue(node, TruffleObject.DATA_TYPE, heap);
        if (object == null || !objectClass.isInstance(object)) return null;
        
        return (O)object;
    }
    
    
    public static abstract class Fields<O extends TruffleObject, T extends TruffleType<O>, F extends TruffleLanguageHeapFragment<O, T>, L extends TruffleLanguage<O, T, F>> extends TruffleObjectPropertyProvider<O, T, F, L, FieldValue> {
        
        protected Fields(String propertyName, L language, boolean filtersProperties) {
            super(propertyName, language, false, filtersProperties, UIThresholds.MAX_INSTANCE_FIELDS);
        }
        
        
//        protected abstract boolean isLanguageObject(Instance instance);
//        
//        protected abstract O createObject(Instance instance);
    
        protected abstract HeapViewerNode createObjectFieldNode(O object, String type, FieldValue field);
        
        
        @Override
        protected boolean includeItem(FieldValue field) {
            // display primitive fields
            if (!(field instanceof ObjectFieldValue)) return true;

            Instance instance = ((ObjectFieldValue)field).getInstance();

            // display null fields
            if (instance == null) return true;
            
            // display primitive arrays
            if (instance instanceof PrimitiveArrayInstance) return true;
            
            // display language objects
            if (getLanguage().isLanguageObject(instance)) return true;

            // display DynamicObject fields
            if (DynamicObject.isDynamicObject(instance)) return true;

            // display selected Java fields
            return includeInstance(instance);
        }
        
        protected boolean includeInstance(Instance instance) { return true; }
        
        @Override
        protected final HeapViewerNode createNode(FieldValue field, Heap heap) {
            if (field instanceof ObjectFieldValue) {
                Instance instance = ((ObjectFieldValue)field).getInstance();
                if (getLanguage().isLanguageObject(instance)) {
                    O object = getLanguage().createObject(instance);
                    return createObjectFieldNode(object, object.getType(heap), field);
                } else {
                    return createForeignFieldNode(instance, field, heap);
                }
            } else {
                return new PrimitiveNode.Field(field);
            }
        }
        
        protected HeapViewerNode createForeignFieldNode(Instance instance, FieldValue field, Heap heap) {
            if (DynamicObject.isDynamicObject(instance)) {
                DynamicObject dobj = new DynamicObject(instance);
                return new DynamicObjectFieldNode(dobj, dobj.getType(heap), field);
            } else {
                return new TerminalJavaNodes.Field((ObjectFieldValue)field, false);
            }
        }
        
        @Override
        protected HeapViewerNode[] getNodes(TruffleObjectsWrapper<O> objects, HeapViewerNode parent, final Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {
            if (!supportsAggregation()) return null;
            
            return new TruffleObjectMergedFields<O>(objects, heap) {
                protected String getMoreNodesString(String moreNodesCount) { return moreNodesString(moreNodesCount); }
                protected String getSamplesContainerString(String objectsCount) { return samplesContainerString(objectsCount); }
                protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx) { return nodesContainerString(firstNodeIdx, lastNodeIdx); }
                protected TruffleLanguage getLanguage() { return Fields.this.getLanguage(); }
                protected boolean filtersFields() { return filtersProperties(); }
                protected boolean includeField(FieldValue field) { return includeItem(field); }
                protected Collection<FieldValue> getFields(O object) throws InterruptedException { return getPropertyItems(object, heap); }
            }.getNodes(parent, viewID, viewFilter, dataTypes, sortOrders, progress);
        }
        
    }
    
    
    public static abstract class References<O extends TruffleObject, T extends TruffleType<O>, F extends TruffleLanguageHeapFragment<O, T>, L extends TruffleLanguage<O, T, F>> extends TruffleObjectPropertyProvider<O, T, F, L, FieldValue> {
        
        protected References(String propertyName, L language, boolean filtersProperties) {
            super(propertyName, language, false, filtersProperties, UIThresholds.MAX_INSTANCE_REFERENCES);
        }
        
        
        protected abstract HeapViewerNode createObjectReferenceNode(O object, String type, FieldValue field);
        
        
        @Override
        protected boolean includeItem(FieldValue field) {
            Instance instance = field.getDefiningInstance();

            // should not happen
            if (instance == null) return false;
            
            // display language references
            if (getLanguage().isLanguageObject(instance)) return true;

            // display DynamicObject references
            if (DynamicObject.isDynamicObject(instance)) return true;

            // display selected Java references
            return includeInstance(instance);
        }
        
        protected boolean includeInstance(Instance instance) { return true; }
        
        @Override
        protected final HeapViewerNode createNode(FieldValue field, Heap heap) {
            Instance instance = field.getDefiningInstance();
            if (getLanguage().isLanguageObject(instance)) {
                O object = getLanguage().createObject(instance);
                return createObjectReferenceNode(object, object.getType(heap), field);
            } else {
                return createForeignReferenceNode(instance, field, heap);
            }
        }
        
        protected HeapViewerNode createForeignReferenceNode(Instance instance, FieldValue field, Heap heap) {
            if (DynamicObject.isDynamicObject(instance)) {
                DynamicObject dobj = new DynamicObject(instance);
                return new DynamicObjectReferenceNode(dobj, dobj.getType(heap), field);
            } else {
                return new TerminalJavaNodes.Field((ObjectFieldValue)field, true);
            }
        }
        
        
        @Override
        protected HeapViewerNode[] getNodes(final TruffleObjectsWrapper<O> objects, HeapViewerNode parent, final Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {
            if (!supportsAggregation()) return null;
            
            return new TruffleObjectMergedReferences<O>(objects, heap) {
                protected String getMoreNodesString(String moreNodesCount) { return moreNodesString(moreNodesCount); }
                protected String getSamplesContainerString(String objectsCount) { return samplesContainerString(objectsCount); }
                protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx) { return nodesContainerString(firstNodeIdx, lastNodeIdx); }
                protected TruffleLanguage getLanguage() { return References.this.getLanguage(); }
                protected boolean filtersReferences() { return filtersProperties(); }
                protected boolean includeReference(FieldValue field) { return includeItem(field); }
                protected Collection<FieldValue> getReferences(O object) throws InterruptedException { return getPropertyItems(object, heap); }
                protected HeapViewerNode createForeignReferenceNode(Instance instance, FieldValue field) { return References.this.createForeignReferenceNode(instance, field, heap); }
            }.getNodes(parent, viewID, viewFilter, dataTypes, sortOrders, progress);
        }
        
    }
    
}
