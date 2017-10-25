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
package com.sun.tools.visualvm.truffle.heapwalker.javascript;

import com.sun.tools.visualvm.truffle.heapwalker.DynamicObject;
import com.sun.tools.visualvm.truffle.heapwalker.TerminalJavaNodes;
import java.util.Iterator;
import java.util.List;
import javax.swing.SortOrder;
import org.netbeans.lib.profiler.heap.ArrayItemValue;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.ObjectArrayInstance;
import org.netbeans.lib.profiler.heap.PrimitiveArrayInstance;
import org.netbeans.modules.profiler.heapwalker.v2.java.InstanceNode;
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
@ServiceProvider(service=HeapWalkerNode.Provider.class, position = 300)
public class JavaScriptArrayItemsProvider extends HeapWalkerNode.Provider {
    
    public String getName() {
        return "items";
    }
    
    public boolean supportsView(Heap heap, String viewID) {
        return viewID.startsWith("javascript_");
    }
    
    public boolean supportsNode(HeapWalkerNode parent, Heap heap, String viewID) {
        if (parent instanceof InstanceNode && !InstanceNode.Mode.INCOMING_REFERENCE.equals(((InstanceNode)parent).getMode())) {
            Instance instance = ((InstanceNode)parent).getInstance();
            return instance != null && instance.getJavaClass().isArray();
        } else {
            return false;
        }
    }
    
    public HeapWalkerNode[] getNodes(final HeapWalkerNode parent, final Heap heap, String viewID, HeapWalkerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders) {
        final Instance instance = HeapWalkerNode.getValue(parent, DataType.INSTANCE, heap);
        if (instance == null) return null;
        
        if (instance instanceof PrimitiveArrayInstance) {
            final String type = instance.getJavaClass().getName().replace("[]", "");
            final List<String> items = ((PrimitiveArrayInstance)instance).getValues();
            
            NodesComputer<Integer> computer = new NodesComputer<Integer>(items.size(), UIThresholds.MAX_ARRAY_ITEMS) {
                protected boolean sorts(DataType dataType) {
                    if (DataType.COUNT == dataType || DataType.OWN_SIZE == dataType || DataType.RETAINED_SIZE == dataType) return false;
                    return true;
                }
                protected HeapWalkerNode createNode(Integer index) {
                    return new PrimitiveNode.ArrayItem(index, type, items.get(index), instance);
                }
                protected Iterator<Integer> objectsIterator(int index) {
                    return integerIterator(index, items.size());
                }
                protected String getMoreNodesString(String moreNodesCount)  {
                    return "<another " + moreNodesCount + " items left>";
                }
                protected String getSamplesContainerString(String objectsCount)  {
                    return "<sample " + objectsCount + " items>";
                }
                protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                    return "<items " + firstNodeIdx + "-" + lastNodeIdx + ">";
                }
            };
        
            return computer.computeNodes(parent, heap, viewID, null, dataTypes, sortOrders);
        } else if (instance instanceof ObjectArrayInstance) {
            final List<ArrayItemValue> items = ((ObjectArrayInstance)instance).getItems();
            
            NodesComputer<Integer> computer = new NodesComputer<Integer>(items.size(), UIThresholds.MAX_ARRAY_ITEMS) {
                protected boolean sorts(DataType dataType) {
                    return !DataType.COUNT.equals(dataType);
                }
                protected HeapWalkerNode createNode(Integer index) {
                    ArrayItemValue item = items.get(index);
                    Instance instance = item.getInstance();
                    if (DynamicObject.isDynamicObject(instance)) {
                        DynamicObject dobject = new DynamicObject(instance);
                        return new JavaScriptNodes.JavaScriptDynamicObjectArrayItemNode(dobject, dobject.getType(heap), item);
                    } else {
                        return new TerminalJavaNodes.ArrayItem(item, false);
                    }
                }
                protected Iterator<Integer> objectsIterator(int index) {
                    return integerIterator(index, items.size());
                }
                protected String getMoreNodesString(String moreNodesCount)  {
                    return "<another " + moreNodesCount + " items left>";
                }
                protected String getSamplesContainerString(String objectsCount)  {
                    return "<sample " + objectsCount + " items>";
                }
                protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                    return "<items " + firstNodeIdx + "-" + lastNodeIdx + ">";
                }
            };
            
            return computer.computeNodes(parent, heap, viewID, null, dataTypes, sortOrders);
        }
        
        return null;
    }
    
}
