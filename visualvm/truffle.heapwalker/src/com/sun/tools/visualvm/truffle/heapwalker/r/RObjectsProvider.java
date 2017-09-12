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
package com.sun.tools.visualvm.truffle.heapwalker.r;

import com.sun.tools.visualvm.truffle.heapwalker.AbstractObjectsProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.SortOrder;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.modules.profiler.heapwalker.v2.HeapContext;
import org.netbeans.modules.profiler.heapwalker.v2.model.DataType;
import org.netbeans.modules.profiler.heapwalker.v2.model.HeapWalkerNode;
import org.netbeans.modules.profiler.heapwalker.v2.model.SortedNodesBuffer;
import org.netbeans.modules.profiler.heapwalker.v2.model.TextNode;

/**
 *
 * @author Jiri Sedlacek
 */
public class RObjectsProvider extends AbstractObjectsProvider {
    
    private static RObjectsContainer PLACEHOLDER = new RObjectsContainer("", 0);
    
    static HeapWalkerNode[] getAllObjects(HeapWalkerNode parent, HeapContext context, String viewID, List<DataType> dataTypes, List<SortOrder> sortOrders, int aggregation) {

        RHeapFragment fragment = (RHeapFragment)context.getFragment();
        Iterator<Instance> instancesI = fragment.getRObjectsIterator();
        
        if (aggregation == 0) {
            DataType dataType = dataTypes == null || dataTypes.isEmpty() ? null : dataTypes.get(0);
            SortOrder sortOrder = sortOrders == null || sortOrders.isEmpty() ? null : sortOrders.get(0);
            SortedNodesBuffer objects = new SortedNodesBuffer(100, dataType, sortOrder, fragment.getHeap(), parent) {
                protected String getMoreItemsString(String formattedNodesLeft) {
                    return "<another " + formattedNodesLeft + " objects left>";
                }
            };
            
            while (instancesI.hasNext()) {
                RObject robject = new RObject(instancesI.next());
                objects.add(new RObjectNode(robject));
            }
            
            return objects.getNodes();
        } else {
            Heap heap = fragment.getHeap();
            
            List<HeapWalkerNode> nodes = new ArrayList();
            Map<String, RObjectsContainer> types = new HashMap();
            
            while (instancesI.hasNext()) {
                RObject robject = new RObject(instancesI.next());
                
                String type = robject.getType();
                type = type.substring(type.lastIndexOf('.') + 1);

                RObjectsContainer typeNode = types.get(type);
                if (typeNode == null) {
                    typeNode = new RObjectsContainer(type, 100);
                    nodes.add(typeNode);
                    types.put(type, typeNode);
                }
                typeNode.add(robject, heap);
            }
            
            return nodes.toArray(HeapWalkerNode.NO_NODES);
        }
    }
    
    
    public static HeapWalkerNode[] getDominators(HeapWalkerNode parent, Heap heap, String viewID, List<DataType> dataTypes, List<SortOrder> sortOrders, int aggregation) {
        if (!DataType.RETAINED_SIZE.valuesAvailable(heap))
            return new HeapWalkerNode[] { new TextNode("<Retained sizes not computed yet>") };
        
        List<HeapWalkerNode> nodes = new ArrayList();
        
        int maxSearchInstances = 10000;
        
        List<Instance> searchInstances = heap.getBiggestObjectsByRetainedSize(maxSearchInstances);
        Iterator<Instance> searchInstancesIt = searchInstances.iterator();
        Set<Instance> dominators;
        
        while (searchInstancesIt.hasNext()) {
            if (!RObject.isRObject(searchInstancesIt.next())) {
                searchInstancesIt.remove();
            }
        }
        dominators = getDominatorRoots(searchInstances);
        
        Map<String, RObjectsContainer> types = new HashMap();

        for (Instance dominator : dominators) {
            RObject dobject = new RObject(dominator);
            String type = dobject.getType();
            type = type.substring(type.lastIndexOf('.') + 1);

            RObjectsContainer typeNode = types.get(type);
            if (typeNode == null) {
                if (aggregation == 0) {
                    typeNode = PLACEHOLDER;
                } else {
                    typeNode = new RObjectsContainer(type, Integer.MAX_VALUE);
                    nodes.add(typeNode);
                }
                types.put(type, typeNode);
            }
            if (typeNode != null) {
                if (aggregation == 0) {
                    nodes.add(new RObjectNode(dobject));
                } else {
                    typeNode.add(dobject, heap);
                }
            }
        }
        
        if (nodes.isEmpty()) nodes.add(new TextNode("<No dominators found>"));
        return nodes.toArray(HeapWalkerNode.NO_NODES);
    }
    
    public static HeapWalkerNode[] getGCRoots(HeapWalkerNode parent, HeapContext context, String viewID, List<DataType> dataTypes, List<SortOrder> sortOrders, int aggregation) {
        RHeapFragment fragment = (RHeapFragment)context.getFragment();
        Iterator<Instance> instancesI = fragment.getRObjectsIterator();
        
        if (aggregation == 0) {
            DataType dataType = dataTypes == null || dataTypes.isEmpty() ? null : dataTypes.get(0);
            SortOrder sortOrder = sortOrders == null || sortOrders.isEmpty() ? null : sortOrders.get(0);
            SortedNodesBuffer objects = new SortedNodesBuffer(100, dataType, sortOrder, fragment.getHeap(), parent) {
                protected String getMoreItemsString(String formattedNodesLeft) {
                    return "<another " + formattedNodesLeft + " objects left>";
                }
            };
            
            while (instancesI.hasNext()) {
                Instance instance = instancesI.next();
                if (instance.isGCRoot()) {
                    RObject robject = new RObject(instance);
                    objects.add(new RObjectNode(robject));
                }
            }
            
            HeapWalkerNode[] nodes = objects.getNodes();
            return nodes.length > 0 ? nodes : new HeapWalkerNode[] { new TextNode("<No GC roots found>") };
        } else {
            Heap heap = fragment.getHeap();
            
            List<HeapWalkerNode> nodes = new ArrayList();
            Map<String, RObjectsContainer> types = new HashMap();
            
            while (instancesI.hasNext()) {
                Instance instance = instancesI.next();
                if (!instance.isGCRoot()) continue;
                
                RObject robject = new RObject(instance);
                
                String type = robject.getType();
                type = type.substring(type.lastIndexOf('.') + 1);

                RObjectsContainer typeNode = types.get(type);
                if (typeNode == null) {
                    typeNode = new RObjectsContainer(type, 100);
                    nodes.add(typeNode);
                    types.put(type, typeNode);
                }
                typeNode.add(robject, heap);
            }
            
            if (nodes.isEmpty()) nodes.add(new TextNode("<No GC roots found>"));
            return nodes.toArray(HeapWalkerNode.NO_NODES);
        }
    }
}
