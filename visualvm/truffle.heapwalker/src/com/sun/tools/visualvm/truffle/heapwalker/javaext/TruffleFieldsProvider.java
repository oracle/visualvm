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
package com.sun.tools.visualvm.truffle.heapwalker.javaext;

import com.sun.tools.visualvm.truffle.heapwalker.DynamicObject;
import com.sun.tools.visualvm.truffle.heapwalker.DynamicObjectFieldNode;
import com.sun.tools.visualvm.truffle.heapwalker.TruffleFrame;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SortOrder;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.ObjectFieldValue;
import org.netbeans.modules.profiler.heapwalker.v2.java.InstanceNode;
import org.netbeans.modules.profiler.heapwalker.v2.java.InstanceReferenceNode;
import org.netbeans.modules.profiler.heapwalker.v2.java.PrimitiveNode;
import org.netbeans.modules.profiler.heapwalker.v2.model.DataType;
import org.netbeans.modules.profiler.heapwalker.v2.model.HeapWalkerNode;
import org.netbeans.modules.profiler.heapwalker.v2.model.HeapWalkerNodeFilter;
import org.netbeans.modules.profiler.heapwalker.v2.model.MoreNodesNode;
import org.netbeans.modules.profiler.heapwalker.v2.model.SortedNodesBuffer;
import org.netbeans.modules.profiler.heapwalker.v2.ui.UIThresholds;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
abstract class TruffleFieldsProvider extends HeapWalkerNode.Provider {
    
    public HeapWalkerNode[] getNodes(HeapWalkerNode parent, Heap heap, String viewID, HeapWalkerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders) {
        List<FieldValue> fields = getFields(parent, heap);
        return getNodes(fields, parent, heap, viewID, dataTypes, sortOrders);
    }
    
    static HeapWalkerNode[] getNodes(final List<FieldValue> fields, final HeapWalkerNode parent, final Heap heap, String viewID, List<DataType> dataTypes, List<SortOrder> sortOrders) {
        if (fields == null) return null;
        
        int maxFields = UIThresholds.MAX_INSTANCE_FIELDS;
        final int fieldsCount = fields.size();
        
        if (fieldsCount <= maxFields) {
            // All fields unsorted
            HeapWalkerNode[] nodes = new HeapWalkerNode[fields.size()];
            for (int i = 0; i < nodes.length; i++) {
                FieldValue field = fields.get(i);
                if (field instanceof ObjectFieldValue) {
                     ObjectFieldValue objectField = (ObjectFieldValue)field;
                    Instance fieldInstance = objectField.getInstance();
                    if (DynamicObject.isDynamicObject(fieldInstance)){
                        nodes[i] = new DynamicObjectFieldNode(new DynamicObject(fieldInstance), field, heap);
                    } else {
                        nodes[i] = new InstanceReferenceNode.Field(objectField, false);
                    }
                    
                    nodes[i] = new InstanceReferenceNode.Field((ObjectFieldValue)field, false);
                } else {
                    nodes[i] = new PrimitiveNode.Field(field);
                }
            }
            return nodes;
        } else {
            // First N fields according to the provided sorting
            final DataType dataType = dataTypes == null || dataTypes.isEmpty() ? null : dataTypes.get(0);
            final SortOrder sortOrder = sortOrders == null || sortOrders.isEmpty() ? null : sortOrders.get(0);
            
            MoreNodesNode.RemainingNodesSupport moreNodes = new MoreNodesNode.RemainingNodesSupport() {
                protected int getRemainingNodesOffset() {
                    return maxFields;
                }
                protected HeapWalkerNode[] computeAllNodes() {
                    return getFields(fields, fieldsCount, dataType, sortOrder, heap, parent, null);
                }
            };
            
            return getFields(fields, maxFields, dataType, sortOrder, heap, parent, moreNodes);
        }
    }
    
    
    private static HeapWalkerNode[] getFields(List<FieldValue> fields, int maxReferences, DataType dataType, SortOrder sortOrder, Heap heap, HeapWalkerNode parent, MoreNodesNode.RemainingNodesSupport moreNodes) {
        SortedNodesBuffer result = new SortedNodesBuffer(maxReferences, dataType, sortOrder, heap, parent, moreNodes) {
            protected String getMoreItemsString(String formattedNodesLeft) {
                return "<another " + formattedNodesLeft + " fields left>";
            }
        };
        for (FieldValue field : fields) {
            if (field instanceof ObjectFieldValue) {
                ObjectFieldValue objectField = (ObjectFieldValue)field;
                Instance fieldInstance = objectField.getInstance();
                if (DynamicObject.isDynamicObject(fieldInstance)){
                    result.add(new DynamicObjectFieldNode(new DynamicObject(fieldInstance), field, heap));
                } else {
                    result.add(new InstanceReferenceNode.Field(objectField, false));
                }
            } else {
                result.add(new PrimitiveNode.Field(field));
            }
        }

        return result.getNodes();
    }
    
    
    protected abstract List<FieldValue> getFields(HeapWalkerNode parent, Heap heap);
    
    
    @ServiceProvider(service=HeapWalkerNode.Provider.class, position = 220)
    public static class InstanceFieldsProvider extends TruffleFieldsProvider {
        
        // TODO: will be configurable, ideally by instance
        private boolean includeStaticFields = true;
        private boolean includeInstanceFields = true;
        
        public String getName() {
            return "truffle fields";
        }

        public boolean supportsView(Heap heap, String viewID) {
            return viewID.startsWith("java_");
        }

        public boolean supportsNode(HeapWalkerNode parent, Heap heap, String viewID) {
            if (parent instanceof InstanceNode && !InstanceNode.Mode.INCOMING_REFERENCE.equals(((InstanceNode)parent).getMode())) {
                Instance instance = ((InstanceNode)parent).getInstance();
                return DynamicObject.isDynamicObject(instance) ||
                       TruffleFrame.isTruffleFrame(instance);
            } else {
                return false;
            }
        }

        
        protected List<FieldValue> getFields(HeapWalkerNode parent, Heap heap) {
            Instance instance = HeapWalkerNode.getValue(parent, DataType.INSTANCE, heap);
            
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
    
//    @ServiceProvider(service=HeapWalkerNode.Provider.class, position = 250)
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
//        public boolean supportsNode(HeapWalkerNode parent, Heap heap, String viewID) {
//            if (parent instanceof ClassNode) {
//                JavaClass javaClass = ((ClassNode)parent).getJavaClass();
//                return javaClass != null && !javaClass.isArray();
//            } else {
//                return false;
//            }
//        }
//
//        
//        protected List<FieldValue> getFields(HeapWalkerNode parent, Heap heap) {
//            JavaClass jclass = HeapWalkerNode.getValue(parent, DataType.CLASS, heap);
//            return jclass == null ? null : jclass.getStaticFieldValues();
//        }
//        
//    }
}
