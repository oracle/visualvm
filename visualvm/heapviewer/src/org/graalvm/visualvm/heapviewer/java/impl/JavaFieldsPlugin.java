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
import java.text.Format;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.lib.jfluid.heap.FieldValue;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.ProfilerIcons;
import org.graalvm.visualvm.heapviewer.HeapContext;
import org.graalvm.visualvm.heapviewer.java.InstanceNode;
import org.graalvm.visualvm.heapviewer.java.InstancesWrapper;
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
import org.graalvm.visualvm.heapviewer.utils.counters.InstanceCounter;
import org.graalvm.visualvm.heapviewer.utils.counters.PrimitiveCounter;
import org.graalvm.visualvm.lib.jfluid.heap.Field;
import org.graalvm.visualvm.lib.jfluid.heap.ObjectFieldValue;
import org.graalvm.visualvm.lib.jfluid.heap.Type;
import org.graalvm.visualvm.lib.profiler.api.icons.LanguageIcons;
import org.graalvm.visualvm.lib.ui.swing.renderer.NormalBoldGrayRenderer;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "JavaFieldsPlugin_Name=Fields",
    "JavaFieldsPlugin_Description=Fields",
    "JavaFieldsPlugin_NoFields=<no fields>",
    "JavaFieldsPlugin_NoFieldsFiltered=<no fields - instance or class fields disabled>",
    "JavaFieldsPlugin_NoSelection=<no class or instance selected>",
    "JavaFieldsPlugin_FieldsContainerMoreNodes=<another {0} fields left>",
    "JavaFieldsPlugin_FieldsContainerSamplesContainer=<sample {0} fields>",
    "JavaFieldsPlugin_FieldsContainerNodesContainer=<fields {0}-{1}>",
    "JavaFieldsPlugin_FieldHistogramMoreNodes=<another {0} values left>",
    "JavaFieldsPlugin_FieldHistogramSamplesContainer=<sample {0} values>",
    "JavaFieldsPlugin_MenuShowInstance=Show (Instance)",
    "JavaFieldsPlugin_MenuShowClass=Show (Class)",
    "JavaFieldsPlugin_MenuFields=Fields",
    "JavaFieldsPlugin_MenuStaticFields=Static Fields",
    "JavaFieldsPlugin_MenuFieldsHisto=Fields Histogram",
    "JavaFieldsPlugin_ValuesCountHint=({0} values)",
    "JavaFieldsPlugin_OOMEWarning=<too many instances - increase heap size!>",
    "JavaFieldsPlugin_FieldHistogramNodesContainer=<values {0}-{1}>"
})
class JavaFieldsPlugin extends HeapViewPlugin {
    
    private static final String KEY_INSTANCE_FIELDS = "iFields"; // NOI18N
    private static final String KEY_INSTANCE_STATIC_FIELDS = "iStaticFields"; // NOI18N
    private static final String KEY_CLASS_FIELDS_HISTOGRAM = "cFieldsHisto"; // NOI18N
    private static final String KEY_CLASS_STATIC_FIELDS = "cStaticFields"; // NOI18N
    
    private volatile boolean iFields = readItem(KEY_INSTANCE_FIELDS, true);
    private volatile boolean iStaticFields = readItem(KEY_INSTANCE_STATIC_FIELDS, true);
    private volatile boolean cFieldsHisto = readItem(KEY_CLASS_FIELDS_HISTOGRAM, true);
    private volatile boolean cStaticFields = readItem(KEY_CLASS_STATIC_FIELDS, true);
    
    
    private static final Format VALUES_COUNT_FORMAT = NumberFormat.getInstance();
    
    
    private static final TreeTableView.ColumnConfiguration CCONF_CLASS = new TreeTableView.ColumnConfiguration(DataType.COUNT, null, DataType.COUNT, SortOrder.DESCENDING, Boolean.FALSE);
    private static final TreeTableView.ColumnConfiguration CCONF_INSTANCE = new TreeTableView.ColumnConfiguration(null, DataType.COUNT, DataType.NAME, SortOrder.UNSORTED, null);
    
    
    private final Heap heap;
    private HeapViewerNode selected;
    
    private final TreeTableView objectsView;
    

    public JavaFieldsPlugin(HeapContext context, HeapViewerActions actions) {
        super(Bundle.JavaFieldsPlugin_Name(), Bundle.JavaFieldsPlugin_Description(), Icons.getIcon(ProfilerIcons.NODE_FORWARD));
        
        heap = context.getFragment().getHeap();
        
        TreeTableViewColumn[] columns = new TreeTableViewColumn[] {
            new TreeTableViewColumn.Name(heap),
            new TreeTableViewColumn.LogicalValue(heap),
            new TreeTableViewColumn.Count(heap, true, true),
            new TreeTableViewColumn.OwnSize(heap, false, false),
            new TreeTableViewColumn.RetainedSize(heap, false, false),
            new TreeTableViewColumn.ObjectID(heap)
        };
        objectsView = new TreeTableView("java_objects_fields", context, actions, columns) { // NOI18N
            @Override
            protected HeapViewerNode[] computeData(RootNode root, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {
                HeapViewerNode _selected;
                synchronized (objectsView) { _selected = selected; }
                
                if (_selected != null) {
                    boolean filtered = false;
                    HeapViewerNode[] nodes = null;
                    
                    InstancesWrapper wrapper = HeapViewerNode.getValue(_selected, DataType.INSTANCES_WRAPPER, heap);
                    if (wrapper != null) {
                        List<HeapViewerNode> fieldNodes = new ArrayList();
                        
                        if (cFieldsHisto) {
                            HeapViewerNode[] histo = getClassFieldsHistogram(wrapper, root, heap, viewID, viewFilter, dataTypes, sortOrders, progress);
                            fieldNodes.addAll(Arrays.asList(histo));
                        } else {
                            filtered = true;
                        }
                        
                        if (cStaticFields) {
                            JavaClass jclass = wrapper.getJavaClass();
                            if (jclass != null) { // Note: GCTypeNode returns null here
                                List<FieldValue> fields = jclass.getStaticFieldValues();
                                fieldNodes.addAll(Arrays.asList(JavaFieldsProvider.getNodes(fields, root, heap, viewID, viewFilter, dataTypes, sortOrders, progress)));
                            }
                        } else {
                            filtered = true;
                        }
                        
                        nodes = fieldNodes.toArray(HeapViewerNode.NO_NODES);
                        
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                if (!cFieldsHisto && !CCONF_INSTANCE.equals(objectsView.getCurrentColumnConfiguration()))
                                        objectsView.configureColumns(CCONF_INSTANCE);
                                else if (cFieldsHisto && !CCONF_CLASS.equals(objectsView.getCurrentColumnConfiguration()))
                                    objectsView.configureColumns(CCONF_CLASS);
                            }
                        });
                    } else {
                        Instance instance = HeapViewerNode.getValue(_selected, DataType.INSTANCE, heap);
                        if (instance != null) {
                            List<FieldValue> fields = new ArrayList();

                            if (iFields) fields.addAll(instance.getFieldValues());
                            else filtered = true;

                            if (iStaticFields) fields.addAll(instance.getStaticFieldValues());
                            else filtered = true;

                            nodes = JavaFieldsProvider.getNodes(fields, root, heap, viewID, viewFilter, dataTypes, sortOrders, progress);
                            
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    if (!CCONF_INSTANCE.equals(objectsView.getCurrentColumnConfiguration()))
                                        objectsView.configureColumns(CCONF_INSTANCE);
                                }
                            });
                        }
                    }

                    return nodes == null || nodes.length == 0 ? new HeapViewerNode[] { new TextNode(filtered ? Bundle.JavaFieldsPlugin_NoFieldsFiltered() : Bundle.JavaFieldsPlugin_NoFields()) } : nodes;
                }
                
                return new HeapViewerNode[] { new TextNode(Bundle.JavaFieldsPlugin_NoSelection()) };
            }
            @Override
            protected void populatePopup(HeapViewerNode node, JPopupMenu popup) {
                if (popup.getComponentCount() > 0) popup.addSeparator();
                
                JMenu mInstance = new JMenu(Bundle.JavaFieldsPlugin_MenuShowInstance());
                mInstance.add(new JCheckBoxMenuItem(Bundle.JavaFieldsPlugin_MenuFields(), iFields) {
                    @Override
                    protected void fireActionPerformed(ActionEvent event) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                iFields = isSelected();
                                storeItem(KEY_INSTANCE_FIELDS, iFields); // NOI18N
                                reloadView();
                            }
                        });
                    }
                });
                mInstance.add(new JCheckBoxMenuItem(Bundle.JavaFieldsPlugin_MenuStaticFields(), iStaticFields) {
                    @Override
                    protected void fireActionPerformed(ActionEvent event) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                iStaticFields = isSelected();
                                storeItem(KEY_INSTANCE_STATIC_FIELDS, iStaticFields);
                                reloadView();
                            }
                        });
                    }
                });
                popup.add(mInstance);
                
                JMenu mClass = new JMenu(Bundle.JavaFieldsPlugin_MenuShowClass());
                mClass.add(new JCheckBoxMenuItem(Bundle.JavaFieldsPlugin_MenuFieldsHisto(), cFieldsHisto) {
                    @Override
                    protected void fireActionPerformed(ActionEvent event) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                cFieldsHisto = isSelected();
                                storeItem(KEY_CLASS_FIELDS_HISTOGRAM, cFieldsHisto);
                                reloadView();
                            }
                        });
                    }
                });
                mClass.add(new JCheckBoxMenuItem(Bundle.JavaFieldsPlugin_MenuStaticFields(), cStaticFields) {
                    @Override
                    protected void fireActionPerformed(ActionEvent event) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                cStaticFields = isSelected();
                                storeItem(KEY_CLASS_STATIC_FIELDS, cStaticFields);
                                reloadView();
                            }
                        });
                    }
                });
                popup.add(mClass);
            }
        };
    }

    protected JComponent createComponent() {
        return objectsView.getComponent();
    }
    
    
    private HeapViewerNode[] getClassFieldsHistogram(final InstancesWrapper instances, HeapViewerNode parent, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {
        final List<Field> fields = getAllInstanceFields(instances.getJavaClass());
        NodesComputer<Field> computer = new NodesComputer<Field>(fields.size(), UIThresholds.MAX_INSTANCE_FIELDS) {
            protected boolean sorts(DataType dataType) {
                return true;
            }
            protected HeapViewerNode createNode(Field field) {
                return new FieldHistogramNode(field) {
                    @Override
                    InterruptibleIterator<Instance> instancesIterator() {
                        return new InterruptibleIterator(instances.getInstancesIterator());
                    }
                    @Override
                    int instancesCount() {
                        return instances.getInstancesCount();
                    }
                };
            }
            protected ProgressIterator<Field> objectsIterator(int index, Progress progress) {
                Iterator<Field> iterator = fields.listIterator(index);
                return new ProgressIterator(iterator, index, false, progress);
            }
            protected String getMoreNodesString(String moreNodesCount)  {
                return Bundle.JavaFieldsPlugin_FieldsContainerMoreNodes(moreNodesCount);
            }
            protected String getSamplesContainerString(String objectsCount)  {
                return Bundle.JavaFieldsPlugin_FieldsContainerSamplesContainer(objectsCount);
            }
            protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                return Bundle.JavaFieldsPlugin_FieldsContainerNodesContainer(firstNodeIdx, lastNodeIdx);
            }
        };
        return computer.computeNodes(parent, heap, viewID, null, dataTypes, sortOrders, progress);
    }
    
    
    static abstract class FieldHistogramNode extends HeapViewerNode {
        
        private final String fieldName;
        private final Type fieldType;
        private int valuesCount = -1;
        
        FieldHistogramNode(Field field) {
            this.fieldName = field.getName();
            this.fieldType = field.getType();
        }
        
        String getFieldName() {
            return fieldName;
        }
        
        Type getFieldType() {
            return fieldType;
        }
        
        int getValuesCount() {
            return valuesCount;
        }
        
        public String toString() {
            if (valuesCount == -1) return fieldName;
            else return fieldName + " " + Bundle.JavaFieldsPlugin_ValuesCountHint(valuesCount); // NOI18N
        }
        
        abstract int instancesCount();
        
        abstract InterruptibleIterator<Instance> instancesIterator();
        
        protected HeapViewerNode[] lazilyComputeChildren(Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {
            String fieldTypeName = fieldType.getName();
            
            if ("object".equals(fieldTypeName)) { // NOI18N
                final InstanceCounter values = new InstanceCounter(instancesCount());
                
                progress.setupKnownSteps(instancesCount());
                
                Iterator<Instance> instances = instancesIterator();
                try {
                    while (instances.hasNext()) {
                        Instance instance = instances.next();
                        progress.step();
                        FieldValue value = getValueOfField(instance, fieldName);
                        if (value instanceof ObjectFieldValue)
                            values.count(((ObjectFieldValue)value).getInstance());
                    }
                    if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
                } finally {
                    progress.finish();
                }
                
                valuesCount = values.size();
                
                NodesComputer<InstanceCounter.Record> computer = new NodesComputer<InstanceCounter.Record>(valuesCount, UIThresholds.MAX_MERGED_OBJECTS) {
                    protected boolean sorts(DataType dataType) {
                        return true;
                    }
                    protected HeapViewerNode createNode(InstanceCounter.Record object) {
                        return new InstanceFieldValueNode(object.getInstance(heap), object.getCount()) {
                            @Override
                            String fieldName() { return fieldName; }
                            @Override
                            InterruptibleIterator<Instance> instancesIterator() { return FieldHistogramNode.this.instancesIterator(); }
                        };
                    }
                    protected ProgressIterator<InstanceCounter.Record> objectsIterator(int index, Progress progress) {
                        Iterator<InstanceCounter.Record> iterator = values.iterator();
                        return new ProgressIterator(iterator, index, true, progress);
                    }
                    protected String getMoreNodesString(String moreNodesCount)  {
                        return Bundle.JavaFieldsPlugin_FieldHistogramMoreNodes(moreNodesCount);
                    }
                    protected String getSamplesContainerString(String objectsCount)  {
                        return Bundle.JavaFieldsPlugin_FieldHistogramSamplesContainer(objectsCount);
                    }
                    protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                        return Bundle.JavaFieldsPlugin_FieldHistogramNodesContainer(firstNodeIdx, lastNodeIdx);
                    }
                };
                
                return computer.computeNodes(FieldHistogramNode.this, heap, viewID, null, dataTypes, sortOrders, progress);
            } else {
                int instancesCount = instancesCount();
                
                try {
                    
                    final PrimitiveCounter counter = PrimitiveCounter.create(fieldTypeName, instancesCount);

                    progress.setupKnownSteps(instancesCount);

                    Iterator<Instance> instances = instancesIterator();
                    try {
                        while (instances.hasNext()) {
                            Instance instance = instances.next();
                            progress.step();
                            FieldValue value = getValueOfField(instance, fieldName);
                            if (value != null) counter.count(getValueOfField(instance, fieldName).getValue());
                        }
                        if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
                    } finally {
                        progress.finish();
                    }

                    valuesCount = counter.size();

                    NodesComputer<PrimitiveCounter.Record> computer = new NodesComputer<PrimitiveCounter.Record>(valuesCount, UIThresholds.MAX_MERGED_OBJECTS) {
                        protected boolean sorts(DataType dataType) {
                            return true;
                        }
                        protected HeapViewerNode createNode(PrimitiveCounter.Record object) {
                            return new PrimitiveFieldValueNode(object.getValue(), fieldType.getName(), object.getCount()) {
                                @Override
                                String fieldName() { return fieldName; }
                                @Override
                                InterruptibleIterator<Instance> instancesIterator() { return FieldHistogramNode.this.instancesIterator(); }
                            };
                        }
                        protected ProgressIterator<PrimitiveCounter.Record> objectsIterator(int index, Progress progress) {
                            Iterator<? extends PrimitiveCounter.Record> iterator = counter.iterator();
                            return new ProgressIterator(iterator, index, true, progress);
                        }
                        protected String getMoreNodesString(String moreNodesCount)  {
                            return Bundle.JavaFieldsPlugin_FieldHistogramMoreNodes(moreNodesCount);
                        }
                        protected String getSamplesContainerString(String objectsCount)  {
                            return Bundle.JavaFieldsPlugin_FieldHistogramSamplesContainer(objectsCount);
                        }
                        protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                            return Bundle.JavaFieldsPlugin_FieldHistogramNodesContainer(firstNodeIdx, lastNodeIdx);
                        }
                    };

                    return computer.computeNodes(FieldHistogramNode.this, heap, viewID, null, dataTypes, sortOrders, progress);
                    
                } catch (OutOfMemoryError e) {
                    
                    return new HeapViewerNode[] { new TextNode(Bundle.JavaFieldsPlugin_OOMEWarning()) };
                    
                }
            }
            
        }
        
    }
    
    
    static abstract class PrimitiveFieldValueNode extends HeapViewerNode {
        
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
        
        abstract InterruptibleIterator<Instance> instancesIterator();
        
        
        protected Object getValue(DataType type, Heap heap) {
            if (type == DataType.COUNT) return getValuesCount();

            return super.getValue(type, heap);
        }
        
        
        protected HeapViewerNode[] lazilyComputeChildren(Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {
            final String fieldName = fieldName();

            NodesComputer<Instance> computer = new NodesComputer<Instance>(valuesCount, UIThresholds.MAX_MERGED_OBJECTS) {
                protected boolean sorts(DataType dataType) {
                    return true;
                }
                protected HeapViewerNode createNode(Instance object) {
                    return new InstanceNode(object) {
                        public boolean isLeaf() {
                            return true;
                        }
                    };
                }
                protected ProgressIterator<Instance> objectsIterator(int index, final Progress _progress) {
                    _progress.setupUnknownSteps();
                    Iterator<Instance> fieldInstanceIterator = new ExcludingIterator<Instance>(instancesIterator()) {
                        @Override
                        protected boolean exclude(Instance instance) {
                            _progress.step();
                            FieldValue value = getValueOfField(instance, fieldName);
                            return value == null || !fieldValue.equals(value.getValue());
                        }
                    };
                    return new ProgressIterator(fieldInstanceIterator, index, true, _progress);
                }
                protected String getMoreNodesString(String moreNodesCount)  {
                    return Bundle.JavaFieldsPlugin_FieldHistogramMoreNodes(moreNodesCount);
                }
                protected String getSamplesContainerString(String objectsCount)  {
                    return Bundle.JavaFieldsPlugin_FieldHistogramSamplesContainer(objectsCount);
                }
                protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                    return Bundle.JavaFieldsPlugin_FieldHistogramNodesContainer(firstNodeIdx, lastNodeIdx);
                }
            };
            
            return computer.computeNodes(PrimitiveFieldValueNode.this, heap, viewID, null, dataTypes, sortOrders, progress);
            
        }
        
    }
    
    static abstract class InstanceFieldValueNode extends InstanceNode {
        
        private final int valuesCount;
        
        
        InstanceFieldValueNode(Instance instance, int valuesCount) {
            super(instance);
            this.valuesCount = valuesCount;
        }
        
        
        public int getValuesCount() {
            return valuesCount;
        }
        
        
        public JavaClass getJavaClass() {
            if (getInstance() == null) return null;
            else return super.getJavaClass();
        }
        
        public String getName(Heap heap) {
            if (getInstance() == null) return "null"; // NOI18N
            else return super.getName(heap);
        }

        public String getLogicalValue(Heap heap) {
            if (getInstance() == null) return DataType.LOGICAL_VALUE.getNoValue();
            else return super.getLogicalValue(heap);
        }
        
        public long getOwnSize() {
            if (getInstance() == null) return DataType.OWN_SIZE.getNoValue();
            else return super.getOwnSize();
        }

        public long getRetainedSize(Heap heap) {
            if (getInstance() == null) return DataType.RETAINED_SIZE.valuesAvailable(heap) ?
                                       DataType.RETAINED_SIZE.getNoValue() : DataType.RETAINED_SIZE.getNotAvailableValue();
            else return super.getRetainedSize(heap);
        }
        
        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof InstanceFieldValueNode)) return false;
            return Objects.equals(getInstance(), ((InstanceFieldValueNode)o).getInstance());
        }

        public int hashCode() {
            return getInstance() == null ? 37 : super.hashCode();
        }
        
        
        abstract String fieldName();
        
        abstract InterruptibleIterator<Instance> instancesIterator();
        
        
        protected Object getValue(DataType type, Heap heap) {
            if (type == DataType.COUNT) return getValuesCount();

            return super.getValue(type, heap);
        }
        
        
        protected HeapViewerNode[] lazilyComputeChildren(Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {
            final String fieldName = fieldName();
            
            NodesComputer<Instance> computer = new NodesComputer<Instance>(valuesCount, UIThresholds.MAX_MERGED_OBJECTS) {
                protected boolean sorts(DataType dataType) {
                    return true;
                }
                protected HeapViewerNode createNode(Instance object) {
                    return new InstanceNode(object) {
                        protected HeapViewerNode[] lazilyComputeChildren(Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {
                            List<FieldValue> fields = JavaFieldsProvider.InstanceFieldsProvider.getFields(this, heap, true, true);
                            return JavaFieldsProvider.getNodes(fields, this, heap, viewID, viewFilter, dataTypes, sortOrders, progress);
                        }
                    };
                }
                protected ProgressIterator<Instance> objectsIterator(int index, final Progress _progress) {
                    final Instance _instance = getInstance();
                    _progress.setupUnknownSteps();
                    Iterator<Instance> fieldInstanceIterator = new ExcludingIterator<Instance>(instancesIterator()) {
                        @Override
                        protected boolean exclude(Instance instance) {
                            _progress.step();
                            FieldValue value = getValueOfField(instance, fieldName);
                            if (!(value instanceof ObjectFieldValue)) return true;
                            return !Objects.equals(_instance, ((ObjectFieldValue)value).getInstance());
                        }
                    };
                    return new ProgressIterator(fieldInstanceIterator, index, true, _progress);
                }
                protected String getMoreNodesString(String moreNodesCount)  {
                    return Bundle.JavaFieldsPlugin_FieldHistogramMoreNodes(moreNodesCount);
                }
                protected String getSamplesContainerString(String objectsCount)  {
                    return Bundle.JavaFieldsPlugin_FieldHistogramSamplesContainer(objectsCount);
                }
                protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                    return Bundle.JavaFieldsPlugin_FieldHistogramNodesContainer(firstNodeIdx, lastNodeIdx);
                }
            };
            
            return computer.computeNodes(InstanceFieldValueNode.this, heap, viewID, null, dataTypes, sortOrders, progress);
            
        }
        
    }
    
    
    // TODO: make JavaClass.getAllInstanceFields() public?
    private static List<Field> getAllInstanceFields(JavaClass jclass) {
        List fields = new ArrayList(50);

        for (JavaClass jcls = jclass; jcls != null; jcls = jcls.getSuperClass()) {
            fields.addAll(jcls.getFields());
        }

        return fields;
    }
    
    private static FieldValue getValueOfField(Instance instance, String name) {
        List<FieldValue> fieldValues = instance.getFieldValues();
        
        for (int i = fieldValues.size() - 1; i >= 0; i--) {
            FieldValue fieldValue = fieldValues.get(i);
            if (fieldValue.getField().getName().equals(name)) {
                return fieldValue;
            }
        }
        
        return null; // happens for java.lang.Class instances in GC Roots preset
    }
    
    
    private static boolean readItem(String itemName, boolean initial) {
        return NbPreferences.forModule(JavaFieldsPlugin.class).getBoolean("JavaFieldsPlugin." + itemName, initial); // NOI18N
    }

    private static void storeItem(String itemName, boolean value) {
        NbPreferences.forModule(JavaFieldsPlugin.class).putBoolean("JavaFieldsPlugin." + itemName, value); // NOI18N
    }
    
    
    private static class FieldHistogramNodeRenderer extends NormalBoldGrayRenderer implements HeapViewerRenderer {
        
        public void setValue(Object value, int row) {
            FieldHistogramNode n = (FieldHistogramNode)value;
            if (n != null) {
//                setNormalValue(n.getFieldType().getName() + " "); // TODO: remove field type!
                setBoldValue(n.getFieldName());
                setGrayValue(n.getValuesCount() == -1 ? "" : " " + Bundle.JavaFieldsPlugin_ValuesCountHint(VALUES_COUNT_FORMAT.format(n.getValuesCount()))); // NOI18N
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
    
    private static class PrimitiveFieldValueNodeRenderer extends NormalBoldGrayRenderer implements HeapViewerRenderer {
        
        public void setValue(Object value, int row) {
            PrimitiveFieldValueNode n = (PrimitiveFieldValueNode)value;
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
    
    
    @ServiceProvider(service=HeapViewerRenderer.Provider.class)
    public static class FieldsHistogramRendererProvider extends HeapViewerRenderer.Provider {

        @Override
        public boolean supportsView(HeapContext context, String viewID) {
            return "java_objects_fields".equals(viewID); // NOI18N
        }

        @Override
        public void registerRenderers(Map<Class<? extends HeapViewerNode>, HeapViewerRenderer> renderers, HeapContext context) {
            renderers.put(FieldHistogramNode.class, new FieldHistogramNodeRenderer());
            renderers.put(PrimitiveFieldValueNode.class, new PrimitiveFieldValueNodeRenderer());
        }
        
    }
    
    
    protected void nodeSelected(HeapViewerNode node, boolean adjusting) {
        synchronized (objectsView) {
            if (Objects.equals(selected, node)) return;
            selected = node;
        }
        
        objectsView.reloadView();
    }
    
    
    @ServiceProvider(service=HeapViewPlugin.Provider.class, position = 200)
    public static class Provider extends HeapViewPlugin.Provider {

        public HeapViewPlugin createPlugin(HeapContext context, HeapViewerActions actions, String viewID) {
            if (!viewID.startsWith("diff") && JavaHeapFragment.isJavaHeap(context)) return new JavaFieldsPlugin(context, actions); // NOI18N
            return null;
        }
        
    }
    
}
