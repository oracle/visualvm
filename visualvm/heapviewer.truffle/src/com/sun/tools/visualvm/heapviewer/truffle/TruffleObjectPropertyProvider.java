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
package com.sun.tools.visualvm.heapviewer.truffle;

import com.sun.tools.visualvm.heapviewer.truffle.nodes.TerminalJavaNodes;
import com.sun.tools.visualvm.heapviewer.HeapFragment;
import com.sun.tools.visualvm.heapviewer.java.PrimitiveNode;
import com.sun.tools.visualvm.heapviewer.model.DataType;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNode;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNodeFilter;
import com.sun.tools.visualvm.heapviewer.model.Progress;
import com.sun.tools.visualvm.heapviewer.truffle.dynamicobject.DynamicObject;
import com.sun.tools.visualvm.heapviewer.truffle.dynamicobject.DynamicObjectFieldNode;
import com.sun.tools.visualvm.heapviewer.truffle.dynamicobject.DynamicObjectReferenceNode;
import com.sun.tools.visualvm.heapviewer.ui.UIThresholds;
import com.sun.tools.visualvm.heapviewer.utils.NodesComputer;
import com.sun.tools.visualvm.heapviewer.utils.ProgressIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.swing.SortOrder;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.HeapProgress;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.ObjectFieldValue;
import org.netbeans.lib.profiler.heap.PrimitiveArrayInstance;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "TruffleObjectPropertyProvider_ComputingNodes=<Computing {0}...>", // <Computing items...>
    "TruffleObjectPropertyProvider_MoreNodes=<another {0} {1} left>", // <another 1234 items left>
    "TruffleObjectPropertyProvider_SamplesContainer=<sample {0} {1}>", // <sample 1234 items>
    "TruffleObjectPropertyProvider_NodesContainer=<{2} {0}-{1}>" // <items 1001 - 2000>
})
public abstract class TruffleObjectPropertyProvider<O extends TruffleObject, T extends TruffleType<O>, F extends TruffleLanguageHeapFragment<O, T>, L extends TruffleLanguage<O, T, F>, I> extends HeapViewerNode.Provider {
    
    private final Class<O> objectClass;
    
    private final L language;
    
    private final String propertyName;
    private final int maxPropertyItems;
    
    private final boolean displaysProgress;
    private final boolean filtersProperties;
    
    
    protected TruffleObjectPropertyProvider(String propertyName, Class<O> objectClass, L language, boolean displaysProgress, boolean filtersProperties, int maxPropertyItems) {
        this.language = language;
        this.objectClass = objectClass;
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
    
    
    protected abstract Collection<I> getPropertyItems(O object, Heap heap);
    
    protected boolean includeItem(I item) { return true; }
    
    protected abstract HeapViewerNode createNode(I item, Heap heap);

    
    @Override
    public final HeapViewerNode[] getNodes(HeapViewerNode parent, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) {
        O object = getObject(parent, heap);
        return object == null ? null : getNodes(object, parent, heap, viewID, viewFilter, dataTypes, sortOrders, progress);
    }
    
    final HeapViewerNode[] getNodes(O object, HeapViewerNode parent, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) {
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
            protected String getMoreNodesString(String moreNodesCount)  {
                return Bundle.TruffleObjectPropertyProvider_MoreNodes(moreNodesCount, propertyName);
            }
            protected String getSamplesContainerString(String objectsCount)  {
                return Bundle.TruffleObjectPropertyProvider_SamplesContainer(objectsCount, propertyName);
            }
            protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                return Bundle.TruffleObjectPropertyProvider_NodesContainer(firstNodeIdx, lastNodeIdx, propertyName);
            }
        };

        return computer.computeNodes(parent, heap, viewID, null, dataTypes, sortOrders, progress);
    }
    
    final O getObject(HeapViewerNode node, Heap heap) {
        if (node == null) return null;
            
        TruffleObject object = HeapViewerNode.getValue(node, TruffleObject.DATA_TYPE, heap);
        if (object == null || !objectClass.isInstance(object)) return null;
        
        return (O)object;
    }
    
    
    public static abstract class Fields<O extends TruffleObject, T extends TruffleType<O>, F extends TruffleLanguageHeapFragment<O, T>, L extends TruffleLanguage<O, T, F>> extends TruffleObjectPropertyProvider<O, T, F, L, FieldValue> {
        
        protected Fields(String propertyName, Class<O> objectClass, L language, boolean filtersProperties) {
            super(propertyName, objectClass, language, false, filtersProperties, UIThresholds.MAX_INSTANCE_FIELDS);
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
        
    }
    
    
    public static abstract class References<O extends TruffleObject, T extends TruffleType<O>, F extends TruffleLanguageHeapFragment<O, T>, L extends TruffleLanguage<O, T, F>> extends TruffleObjectPropertyProvider<O, T, F, L, FieldValue> {
        
        protected References(String propertyName, Class<O> objectClass, L language, boolean filtersProperties) {
            super(propertyName, objectClass, language, true, filtersProperties, UIThresholds.MAX_INSTANCE_REFERENCES);
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
        
    }
    
}
