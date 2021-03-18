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
package org.graalvm.visualvm.heapviewer.truffle.javaext;

import org.graalvm.visualvm.heapviewer.truffle.dynamicobject.DynamicObject;
import org.graalvm.visualvm.heapviewer.truffle.dynamicobject.DynamicObjectFieldNode;
import org.graalvm.visualvm.heapviewer.truffle.dynamicobject.DynamicObjectReferenceNode;
import org.graalvm.visualvm.heapviewer.truffle.nodes.TerminalJavaNodes;
import org.graalvm.visualvm.heapviewer.truffle.TruffleFrame;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.SortOrder;
import org.graalvm.visualvm.lib.jfluid.heap.FieldValue;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.ObjectFieldValue;
import org.graalvm.visualvm.heapviewer.java.InstanceNode;
import org.graalvm.visualvm.heapviewer.java.PrimitiveNode;
import org.graalvm.visualvm.heapviewer.model.DataType;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNode;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNodeFilter;
import org.graalvm.visualvm.heapviewer.model.Progress;
import org.graalvm.visualvm.heapviewer.ui.UIThresholds;
import org.graalvm.visualvm.heapviewer.utils.NodesComputer;
import org.graalvm.visualvm.heapviewer.utils.ProgressIterator;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "TruffleFieldsProvider_Name=truffle fields",
    "TruffleFieldsProvider_MoreNodes=<another {0} truffle fields left>",
    "TruffleFieldsProvider_SamplesContainer=<sample {0} truffle fields>",
    "TruffleFieldsProvider_NodesContainer=<truffle fields {0}-{1}>"
})
abstract class TruffleFieldsProvider extends HeapViewerNode.Provider {
    
    public HeapViewerNode[] getNodes(HeapViewerNode parent, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {
        List<FieldValue> fields = getFields(parent, heap);
        return getNodes(fields, parent, heap, viewID, viewFilter, dataTypes, sortOrders, progress);
    }
    
    static HeapViewerNode[] getNodes(final List<FieldValue> fields, final HeapViewerNode parent, final Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {
        if (fields == null) return null;
        
        NodesComputer<Integer> computer = new NodesComputer<Integer>(fields.size(), UIThresholds.MAX_INSTANCE_FIELDS) {
            protected boolean sorts(DataType dataType) {
                return !DataType.COUNT.equals(dataType);
            }
            protected HeapViewerNode createNode(Integer index) {
                return TruffleFieldsProvider.createNode(fields.get(index), heap);
            }
            protected ProgressIterator<Integer> objectsIterator(int index, Progress progress) {
                Iterator<Integer> iterator = integerIterator(index, fields.size());
                return new ProgressIterator(iterator, index, false, progress);
            }
            protected String getMoreNodesString(String moreNodesCount)  {
                return Bundle.TruffleFieldsProvider_MoreNodes(moreNodesCount);
            }
            protected String getSamplesContainerString(String objectsCount)  {
                return Bundle.TruffleFieldsProvider_SamplesContainer(objectsCount);
            }
            protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                return Bundle.TruffleFieldsProvider_NodesContainer(firstNodeIdx, lastNodeIdx);
            }
        };

        return computer.computeNodes(parent, heap, viewID, null, dataTypes, sortOrders, progress);
    }
    
    private static HeapViewerNode createNode(FieldValue field, Heap heap) {
        if (field instanceof ObjectFieldValue) {
            ObjectFieldValue objectField = (ObjectFieldValue)field;
            Instance fieldInstance = objectField.getInstance();
            if (DynamicObject.isDynamicObject(fieldInstance)) {
                DynamicObject dobject = new DynamicObject(fieldInstance);
                return new DynamicObjectFieldNode(dobject, dobject.getType(), field);
            } else {
                return new TerminalJavaNodes.Field(objectField, false);
            }
        } else {
            return new PrimitiveNode.Field(field);
        }
    }
    
    
    protected abstract List<FieldValue> getFields(HeapViewerNode parent, Heap heap);
    
    
    @ServiceProvider(service=HeapViewerNode.Provider.class, position = 100)
    public static class InstanceFieldsProvider extends TruffleFieldsProvider {
        
        // TODO: will be configurable, ideally by instance
        private boolean includeStaticFields = true;
        private boolean includeInstanceFields = true;
        
        public String getName() {
            return Bundle.TruffleFieldsProvider_Name();
        }

        public boolean supportsView(Heap heap, String viewID) {
            return viewID.equals("truffle_objects_javaext"); // NOI18N
        }

        public boolean supportsNode(HeapViewerNode parent, Heap heap, String viewID) {
            if (parent instanceof InstanceNode && !(parent instanceof DynamicObjectReferenceNode)) {
                InstanceNode node = (InstanceNode)parent;
                if (InstanceNode.Mode.INCOMING_REFERENCE.equals(node.getMode())) return false;
                
                Instance instance = node.getInstance();
                return DynamicObject.isDynamicObject(instance) ||
                       TruffleFrame.isTruffleFrame(instance);
            } else {
                return false;
            }
        }

        
        protected List<FieldValue> getFields(HeapViewerNode parent, Heap heap) {
            Instance instance = HeapViewerNode.getValue(parent, DataType.INSTANCE, heap);
            
            if (DynamicObject.isDynamicObject(instance)) {
                DynamicObject dobj = new DynamicObject(instance);
                if (includeStaticFields == includeInstanceFields) {
                    List<FieldValue> fields = new ArrayList(dobj.getFieldValues());
                    fields.addAll(dobj.getStaticFieldValues());
                    return fields;
                } else if (includeInstanceFields) {
                    return dobj.getFieldValues();
                } else {
                    return dobj.getStaticFieldValues();
                }
            } else if (TruffleFrame.isTruffleFrame(instance)) {
                TruffleFrame tframe = new TruffleFrame(instance);
                return new ArrayList(tframe.getFieldValues());
            }
            
            return null;
        }
        
    }
    
//    @ServiceProvider(service=HeapViewerNode.Provider.class, position = 250)
//    public static class ClassFieldsProvider extends TruffleFieldsProvider {
//        
//        public String getName() {
//            return "static fields";
//        }
//
//        public boolean supportsView(Heap heap, String viewID) {
//            return viewID.startsWith("java_objects");
//        }
//
//        public boolean supportsNode(HeapViewerNode parent, Heap heap, String viewID) {
//            if (parent instanceof ClassNode) {
//                JavaClass javaClass = ((ClassNode)parent).getJavaClass();
//                return javaClass != null && !javaClass.isArray();
//            } else {
//                return false;
//            }
//        }
//
//        
//        protected List<FieldValue> getFields(HeapViewerNode parent, Heap heap) {
//            JavaClass jclass = HeapViewerNode.getValue(parent, DataType.CLASS, heap);
//            return jclass == null ? null : jclass.getStaticFieldValues();
//        }
//        
//    }
}
