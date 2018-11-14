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

import java.text.Format;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.graalvm.visualvm.heapviewer.truffle.dynamicobject.DynamicObject;
import org.graalvm.visualvm.heapviewer.truffle.dynamicobject.DynamicObjectNode;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerRenderer;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerRendererWrapper;
import org.graalvm.visualvm.heapviewer.ui.UIThresholds;
import org.graalvm.visualvm.heapviewer.utils.ExcludingIterator;
import org.graalvm.visualvm.heapviewer.utils.InterruptibleIterator;
import org.graalvm.visualvm.heapviewer.utils.NodesComputer;
import org.graalvm.visualvm.heapviewer.utils.ProgressIterator;
import org.graalvm.visualvm.heapviewer.utils.counters.InstanceCounter;
import org.graalvm.visualvm.lib.jfluid.heap.Field;
import org.graalvm.visualvm.lib.jfluid.heap.FieldValue;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.ObjectFieldValue;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.LanguageIcons;
import org.graalvm.visualvm.lib.profiler.api.icons.ProfilerIcons;
import org.graalvm.visualvm.lib.ui.swing.renderer.NormalBoldGrayRenderer;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
abstract class TruffleObjectMergedFields<O extends TruffleObject> {
    
    private final Heap heap;
    private final TruffleObjectsWrapper<O> objects;
    
    
    TruffleObjectMergedFields(TruffleObjectsWrapper<O> objects, Heap heap) {
        this.objects = objects;
        this.heap = heap;
    }
    
    
    protected abstract String getMoreNodesString(String moreNodesCount);
    protected abstract String getSamplesContainerString(String objectsCount);
    protected abstract String getNodesContainerString(String firstNodeIdx, String lastNodeIdx);
    
    protected abstract TruffleLanguage getLanguage();
    
    protected abstract boolean filtersFields();
    protected abstract boolean includeField(FieldValue field);
    protected abstract Collection<FieldValue> getFields(O object);
    
    
    private int objectsCount() { return objects.getObjectsCount(); }
    private Iterator<O> objectsIterator() { return new InterruptibleIterator(objects.getObjectsIterator()); }
    
    private HeapViewerNode createObjectNode(O object) {
        return (HeapViewerNode)getLanguage().createObjectNode(object, object.getType(heap));
    }
    
    
    HeapViewerNode[] getNodes(HeapViewerNode parent, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {
        final Set<FieldDescriptor> fields = getAllObjectsFields(progress);
        NodesComputer<FieldDescriptor> computer = new NodesComputer<FieldDescriptor>(fields.size(), UIThresholds.MAX_INSTANCE_FIELDS) {
            protected boolean sorts(DataType dataType) {
                return true;
            }
            protected HeapViewerNode createNode(FieldDescriptor field) {
                return new MergedObjectFieldNode(field);
            }
            protected ProgressIterator<FieldDescriptor> objectsIterator(int index, Progress progress) {
                Iterator<FieldDescriptor> iterator = fields.iterator();
                return new ProgressIterator(iterator, index, true, progress);
            }
            protected String getMoreNodesString(String moreNodesCount)  {
                return TruffleObjectMergedFields.this.getMoreNodesString(moreNodesCount);
            }
            protected String getSamplesContainerString(String objectsCount)  {
                return TruffleObjectMergedFields.this.getSamplesContainerString(objectsCount);
            }
            protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                return TruffleObjectMergedFields.this.getNodesContainerString(firstNodeIdx, lastNodeIdx);
            }
        };
        return computer.computeNodes(parent, heap, viewID, null, dataTypes, sortOrders, progress);
    }
    
    
    private Set<FieldDescriptor> getAllObjectsFields(Progress progress) throws InterruptedException {
        boolean filtersProperties = filtersFields();

        Set<FieldDescriptor> allFields = new HashSet();
        Iterator<O> objectsI = objectsIterator();
        
        progress.setupKnownSteps(objects.getObjectsCount());
        
        try {
            while (objectsI.hasNext()) {
                progress.step();
                
                Collection<FieldValue> fields = getFields(objectsI.next());
                if (fields != null) for (FieldValue field : fields) {
                    if (!filtersProperties || includeField(field)) {
                        Field f = field.getField();
                        String fname = f.isStatic() ? "static " + f.getName() : f.getName(); // NOI18N
                        int ftype = field instanceof ObjectFieldValue ? 0 : -1;
                        allFields.add(new FieldDescriptor(fname, ftype));
                    }
                }
            }
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
        } finally {
            progress.finish();
        }

        return allFields;
    }
    
    private FieldValue getValueOfField(O object, String name) {
        Collection<FieldValue> fieldValues = getFields(object);
        if (fieldValues == null) return null;

        ArrayList<FieldValue> fieldValuesArr = fieldValues instanceof ArrayList ?
                              (ArrayList<FieldValue>)fieldValues : new ArrayList(fieldValues);

        for (int i = fieldValuesArr.size() - 1; i >= 0; i--) {
            FieldValue fv = fieldValuesArr.get(i);
            Field field = fv.getField();
            String fieldN = field.getName();
            if (field.isStatic()) fieldN = "static " + fieldN; // NOI18N
            if (fieldN.equals(name)) return fv;
        }

        return null;
    }
    
    
    private class MergedObjectFieldNode extends HeapViewerNode {
        
        private final String fieldName;
        private final int fieldType;
        private int valuesCount = -1;
        
        
        MergedObjectFieldNode(FieldDescriptor fieldDescriptor) {
            this.fieldName = fieldDescriptor.name;
            this.fieldType = fieldDescriptor.type;
        }
        
        
        String getFieldName() {
            return fieldName;
        }
        
        
        int getValuesCount() {
            return valuesCount;
        }
        
        
        protected HeapViewerNode[] lazilyComputeChildren(Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {
            if (fieldType == 0) {
                final InstanceCounter values = new InstanceCounter(objectsCount());

                progress.setupKnownSteps(objectsCount());
                
                Iterator<O> objects = objectsIterator();
                try {
                    while (objects.hasNext()) {
                        O o = objects.next();
                        progress.step();
                        FieldValue value = getValueOfField(o, fieldName);
                        if (value instanceof ObjectFieldValue)
                            values.count(((ObjectFieldValue)value).getInstance());
                    }
                    if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
                } finally {
                    progress.finish();
                }

                valuesCount = values.size();            

                final TruffleLanguage language = getLanguage();

                NodesComputer<InstanceCounter.Record> computer = new NodesComputer<InstanceCounter.Record>(valuesCount, 20) {
                    protected boolean sorts(DataType dataType) {
                        return true;
                    }
                    protected HeapViewerNode createNode(InstanceCounter.Record record) {
                        Instance instance = record.getInstance(heap);
                        HeapViewerNode node;

                        if (language.isLanguageObject(instance)) {
                            O object = (O)language.createObject(instance);
                            node = (HeapViewerNode)language.createObjectNode(object, object.getType(heap));
                        } else {
                            if (DynamicObject.isDynamicObject(instance)) {
                                DynamicObject pbject = new DynamicObject(instance);
                                node = new DynamicObjectNode(pbject, pbject.getType(heap));
                            } else {
                                node = new InstanceNode.IncludingNull(instance);
                            }
                        }

                        return new ObjectFieldValueNode(node, record.getCount()) {
                            @Override
                            String fieldName() { return fieldName; }
                        };
                    }
                    protected ProgressIterator<InstanceCounter.Record> objectsIterator(int index, Progress progress) {
                        Iterator<InstanceCounter.Record> iterator = values.iterator();
                        return new ProgressIterator(iterator, index, true, progress);
                    }
                    protected String getMoreNodesString(String moreNodesCount)  {
                        return Bundle.TruffleObjectPropertyProvider_FieldHistogramMoreNodes(moreNodesCount);
                    }
                    protected String getSamplesContainerString(String objectsCount)  {
                        return Bundle.TruffleObjectPropertyProvider_FieldHistogramSamplesContainer(objectsCount);
                    }
                    protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                        return Bundle.TruffleObjectPropertyProvider_FieldHistogramNodesContainer(firstNodeIdx, lastNodeIdx);
                    }
                };

                return computer.computeNodes(MergedObjectFieldNode.this, heap, viewID, null, dataTypes, sortOrders, progress);
            } else {
                final Map<String, Integer> values = new HashMap();

                progress.setupKnownSteps(objectsCount());
                
                Iterator<O> objects = objectsIterator();
                try {
                    while (objects.hasNext()) {
                        O o = objects.next();
                        progress.step();
                        FieldValue value = getValueOfField(o, fieldName);
                        if (value != null) {
                            String val = value.getValue();
                            Integer count = values.get(val);
                            if (count == null) count = 0;
                            values.put(val, ++count);
                        }
                    }
                    if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
                } finally {
                    progress.finish();
                }

                valuesCount = values.size(); 
                
                NodesComputer<Map.Entry<String, Integer>> computer = new NodesComputer<Map.Entry<String, Integer>>(valuesCount, 20) {
                    protected boolean sorts(DataType dataType) {
                        return true;
                    }
                    protected HeapViewerNode createNode(Map.Entry<String, Integer> record) {
                        return new PrimitiveFieldValueNode(record.getKey(), "object", record.getValue()) { // NOI18N
                            @Override
                            String fieldName() { return fieldName; }
                        };
                    }
                    protected ProgressIterator<Map.Entry<String, Integer>> objectsIterator(int index, Progress progress) {
                        Iterator<Map.Entry<String, Integer>> iterator = values.entrySet().iterator();
                        return new ProgressIterator(iterator, index, true, progress);
                    }
                    protected String getMoreNodesString(String moreNodesCount)  {
                        return Bundle.TruffleObjectPropertyProvider_FieldHistogramMoreNodes(moreNodesCount);
                    }
                    protected String getSamplesContainerString(String objectsCount)  {
                        return Bundle.TruffleObjectPropertyProvider_FieldHistogramSamplesContainer(objectsCount);
                    }
                    protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                        return Bundle.TruffleObjectPropertyProvider_FieldHistogramNodesContainer(firstNodeIdx, lastNodeIdx);
                    }
                };

                return computer.computeNodes(MergedObjectFieldNode.this, heap, viewID, null, dataTypes, sortOrders, progress);
            }
        }
        
        
        public String toString() {
            if (valuesCount == -1) return fieldName;
            else return fieldName + " " + Bundle.TruffleObjectPropertyProvider_ValuesCountHint(valuesCount); // NOI18N
        }
        
    }
    
    
    private abstract class ObjectFieldValueNode extends HeapViewerNodeWrapper {
        
        private final int valuesCount;
        
        
        ObjectFieldValueNode(HeapViewerNode node, int valuesCount) {
            super(node);
            this.valuesCount = valuesCount;
        }
        
        
        public int getValuesCount() {
            return valuesCount;
        }
        
        
        abstract String fieldName();
        
        
        protected Object getValue(DataType type, Heap heap) {
            if (type == DataType.COUNT) return getValuesCount();

            return super.getValue(type, heap);
        }
        
        
        protected HeapViewerNode[] lazilyComputeChildren(Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {
            final String fieldName = fieldName();
            NodesComputer<O> computer = new NodesComputer<O>(valuesCount, 20) {
                protected boolean sorts(DataType dataType) {
                    return true;
                }
                protected HeapViewerNode createNode(O object) {
                    return TruffleObjectMergedFields.this.createObjectNode(object);
                }
                protected ProgressIterator<O> objectsIterator(int index, Progress progress) {
                    final Instance _instance = HeapViewerNode.getValue(ObjectFieldValueNode.this.getNode(), DataType.INSTANCE, heap);
                    progress.setupUnknownSteps();
                    Iterator<O> fieldInstanceIterator = new ExcludingIterator<O>(new InterruptibleIterator(TruffleObjectMergedFields.this.objectsIterator())) {
                        @Override
                        protected boolean exclude(O object) {
                            progress.step();
                            FieldValue value = getValueOfField(object, fieldName);
                            if (!(value instanceof ObjectFieldValue)) return true;
                            return !Objects.equals(_instance, ((ObjectFieldValue)value).getInstance());
                        }
                    };
                    return new ProgressIterator(fieldInstanceIterator, index, true, progress);
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
            
            HeapViewerNode[] result = computer.computeNodes(ObjectFieldValueNode.this, heap, viewID, null, dataTypes, sortOrders, progress);
            
            return result;
            
        }
        
    }
    
    
    private abstract class PrimitiveFieldValueNode extends HeapViewerNode {
        
        private final String fieldValue;
        private final String fieldType;
        private final int valuesCount;
        
        
        PrimitiveFieldValueNode(String fieldValue, String fieldType, int valuesCount) {
            this.fieldValue = fieldValue;
            this.fieldType = fieldType;
            this.valuesCount = valuesCount;
        }
        
        
        public String getType() {
            return fieldType;
        }

        public String getValue() {
            return fieldValue;
        }
        
        public int getValuesCount() {
            return valuesCount;
        }
        
        
        abstract String fieldName();
        
        
        protected Object getValue(DataType type, Heap heap) {
            if (type == DataType.COUNT) return getValuesCount();

            return super.getValue(type, heap);
        }
        
        
        protected HeapViewerNode[] lazilyComputeChildren(Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {
            final String fieldName = fieldName();
            NodesComputer<O> computer = new NodesComputer<O>(valuesCount, 20) {
                protected boolean sorts(DataType dataType) {
                    return true;
                }
                protected HeapViewerNode createNode(O object) {
                    return TruffleObjectMergedFields.this.createObjectNode(object);
                }
                protected ProgressIterator<O> objectsIterator(int index, Progress progress) {
                    progress.setupUnknownSteps();
                    Iterator<O> fieldInstanceIterator = new ExcludingIterator<O>(new InterruptibleIterator(TruffleObjectMergedFields.this.objectsIterator())) {
                        @Override
                        protected boolean exclude(O object) {
                            progress.step();
                            FieldValue value = getValueOfField(object, fieldName);
                            if (value == null || value instanceof ObjectFieldValue) return true;
                            return !Objects.equals(fieldValue, value.getValue());
                        }
                    };
                    return new ProgressIterator(fieldInstanceIterator, index, true, progress);
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
            
            HeapViewerNode[] result = computer.computeNodes(PrimitiveFieldValueNode.this, heap, viewID, null, dataTypes, sortOrders, progress);
            
            return result;
            
        }
        
    }
    
    
    private static class MergedObjectFieldNodeRenderer extends NormalBoldGrayRenderer implements HeapViewerRenderer {
        
        private static final Format VALUES_COUNT_FORMAT = NumberFormat.getInstance();
        
        public void setValue(Object value, int row) {
            TruffleObjectMergedFields.MergedObjectFieldNode n = (TruffleObjectMergedFields.MergedObjectFieldNode)value;
            if (n != null) {
                String name = n.getFieldName();
                if (name.startsWith("static ")) { // NOI18N
                    setNormalValue("static "); // NOI18N
                    setBoldValue(name.substring("static ".length())); // NOI18N
                } else {
                    setNormalValue(""); // NOI18N
                    setBoldValue(name);
                }
                setGrayValue(n.getValuesCount() == -1 ? "" : " " + Bundle.TruffleObjectPropertyProvider_ValuesCountHint(VALUES_COUNT_FORMAT.format(n.getValuesCount()))); // NOI18N
            } else {
                setBoldValue(""); // NOI18N
                setGrayValue(""); // NOI18N
            }
            setIcon(Icons.getIcon(ProfilerIcons.NODE_FORWARD));
        }


        public String getShortName() {
            return getBoldValue();
        }
        
    }
    
    private static class ObjectFieldValueNodeRenderer extends HeapViewerRendererWrapper {
        
        @Override
        protected HeapViewerRenderer getRenderer(Object value, int row) {
            TruffleObjectMergedFields.ObjectFieldValueNode vnode = (TruffleObjectMergedFields.ObjectFieldValueNode)value;
            HeapViewerNode node = vnode.getNode();
            HeapViewerRenderer renderer = RootNode.get(vnode).resolveRenderer(node);
            renderer.setValue(node, row);
            return renderer;
        }
        
    }
    
    private static class PrimitiveFieldValueNodeRenderer extends NormalBoldGrayRenderer implements HeapViewerRenderer {
        
        public void setValue(Object value, int row) {
            TruffleObjectMergedFields.PrimitiveFieldValueNode n = (TruffleObjectMergedFields.PrimitiveFieldValueNode)value;
            if (n != null) {
                setNormalValue(n.getType() + " "); // NOI18N
                setBoldValue(n.getValue());
            } else {
                setNormalValue(""); // NOI18N
                setBoldValue(""); // NOI18N
            }
            setIcon(Icons.getIcon(LanguageIcons.PRIMITIVE));
        }


        public String getShortName() {
            return getBoldValue();
        }
        
    }
    
    
    private static class FieldDescriptor {
            
        final String name;
        final int type; // temporary solution, 0 for ObjectFieldValue, -1 for generic values

        FieldDescriptor(String name, int type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public int hashCode() {
            return 31 * type + name.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof FieldDescriptor)) return false;

            FieldDescriptor fd = (FieldDescriptor)o;
            return type == fd.type && Objects.equals(name, fd.name);
        }

    }
    
    
    @ServiceProvider(service=HeapViewerRenderer.Provider.class)
    public static class MergedFieldsNodeRendererProvider extends HeapViewerRenderer.Provider {

        @Override
        public boolean supportsView(HeapContext context, String viewID) {
            return true;
        }

        @Override
        public void registerRenderers(Map<Class<? extends HeapViewerNode>, HeapViewerRenderer> renderers, HeapContext context) {
            renderers.put(TruffleObjectMergedFields.MergedObjectFieldNode.class, new MergedObjectFieldNodeRenderer());
            renderers.put(TruffleObjectMergedFields.ObjectFieldValueNode.class, new ObjectFieldValueNodeRenderer());
            renderers.put(TruffleObjectMergedFields.PrimitiveFieldValueNode.class, new PrimitiveFieldValueNodeRenderer());
        }
        
    }
    
}
