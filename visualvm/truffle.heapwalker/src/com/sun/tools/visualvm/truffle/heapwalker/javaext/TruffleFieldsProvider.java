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
import java.util.Iterator;
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
import org.netbeans.modules.profiler.heapwalker.v2.ui.UIThresholds;
import org.netbeans.modules.profiler.heapwalker.v2.utils.NodesComputer;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
abstract class TruffleFieldsProvider extends HeapWalkerNode.Provider {
    
    public HeapWalkerNode[] getNodes(HeapWalkerNode parent, Heap heap, String viewID, HeapWalkerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders) {
        List<FieldValue> fields = getFields(parent, heap);
        return getNodes(fields, parent, heap, viewID, viewFilter, dataTypes, sortOrders);
    }
    
    static HeapWalkerNode[] getNodes(final List<FieldValue> fields, final HeapWalkerNode parent, final Heap heap, String viewID, HeapWalkerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders) {
        if (fields == null) return null;
        
        NodesComputer<Integer> computer = new NodesComputer<Integer>(fields.size(), UIThresholds.MAX_INSTANCE_FIELDS) {
            protected boolean sorts(DataType dataType) {
                return !DataType.COUNT.equals(dataType);
            }
            protected HeapWalkerNode createNode(Integer index) {
                return TruffleFieldsProvider.createNode(fields.get(index), heap);
            }
            protected Iterator<Integer> objectsIterator(int index) {
                return integerIterator(index, fields.size());
            }
            protected String getMoreNodesString(String moreNodesCount)  {
                return "<another " + moreNodesCount + " truffle fields left>";
            }
            protected String getSamplesContainerString(String objectsCount)  {
                return "<sample " + objectsCount + " truffle fields>";
            }
            protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                return "<truffle fields " + firstNodeIdx + "-" + lastNodeIdx + ">";
            }
        };

        return computer.computeNodes(parent, heap, viewID, null, dataTypes, sortOrders);
    }
    
    private static HeapWalkerNode createNode(FieldValue field, Heap heap) {
        if (field instanceof ObjectFieldValue) {
            ObjectFieldValue objectField = (ObjectFieldValue)field;
            Instance fieldInstance = objectField.getInstance();
            if (DynamicObject.isDynamicObject(fieldInstance)){
                return new DynamicObjectFieldNode(new DynamicObject(fieldInstance), field, heap);
            } else {
                return new InstanceReferenceNode.Field(objectField, false);
            }
        } else {
            return new PrimitiveNode.Field(field);
        }
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
