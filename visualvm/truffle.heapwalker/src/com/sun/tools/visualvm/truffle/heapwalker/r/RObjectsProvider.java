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
import javax.swing.SortOrder;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.modules.profiler.heapwalker.v2.HeapContext;
import org.netbeans.modules.profiler.heapwalker.v2.model.DataType;
import org.netbeans.modules.profiler.heapwalker.v2.model.HeapWalkerNode;
import org.netbeans.modules.profiler.heapwalker.v2.model.HeapWalkerNodeFilter;
import org.netbeans.modules.profiler.heapwalker.v2.model.TextNode;
import org.netbeans.modules.profiler.heapwalker.v2.ui.UIThresholds;
import org.netbeans.modules.profiler.heapwalker.v2.utils.NodesComputer;

/**
 *
 * @author Jiri Sedlacek
 */
public class RObjectsProvider extends AbstractObjectsProvider {
    
    static HeapWalkerNode[] getAllObjects(HeapWalkerNode parent, HeapContext context, String viewID, HeapWalkerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, int aggregation) {

        final RHeapFragment fragment = (RHeapFragment)context.getFragment();
        final Heap heap = fragment.getHeap();
        
        if (aggregation == 0) {
            NodesComputer<Instance> computer = new NodesComputer<Instance>(UIThresholds.MAX_TOPLEVEL_INSTANCES) {
                protected boolean sorts(DataType dataType) {
                    return !DataType.COUNT.equals(dataType);
                }
                protected HeapWalkerNode createNode(Instance instance) {
                    RObject robject = new RObject(instance);
                    return new RObjectNode(robject);
                }
                protected Iterator<Instance> objectsIterator(int index) {
                    Iterator<Instance> instances = fragment.getRObjectsIterator();
                    for (int i = 0; i < index; i++) instances.next();
                    return instances;
                }
                protected String getMoreNodesString(String moreNodesCount)  {
                    return "<another " + moreNodesCount + " objects left>";
                }
                protected String getSamplesContainerString(String objectsCount)  {
                    return "<sample " + objectsCount + " objects>";
                }
                protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                    return "<objects " + firstNodeIdx + "-" + lastNodeIdx + ">";
                }
            };

            return computer.computeNodes(parent, heap, viewID, null, dataTypes, sortOrders);
        } else {
            List<HeapWalkerNode> nodes = new ArrayList();
            Map<String, RObjectsContainer> types = new HashMap();
            
            Iterator<Instance> instances = fragment.getRObjectsIterator();
            
            while (instances.hasNext()) {
                RObject robject = new RObject(instances.next());
                String type = robject.getType();
                RObjectsContainer typeNode = types.get(type);

                if (typeNode == null) {
                    typeNode = new RObjectsContainer(type);
                    nodes.add(typeNode);
                    types.put(type, typeNode);
                }
                
                typeNode.add(robject, heap);
            }
            
            return nodes.toArray(HeapWalkerNode.NO_NODES);
        }
    }
    
    
    public static HeapWalkerNode[] getDominators(HeapWalkerNode parent, Heap heap, String viewID, HeapWalkerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, int aggregation) {
        if (!DataType.RETAINED_SIZE.valuesAvailable(heap))
            return new HeapWalkerNode[] { new TextNode("<Retained sizes not computed yet>") };
        
        int maxSearchInstances = 10000;
        
        List<Instance> searchInstances = heap.getBiggestObjectsByRetainedSize(maxSearchInstances);
        Iterator<Instance> searchInstancesIt = searchInstances.iterator();
        while (searchInstancesIt.hasNext())
            if (!RObject.isRObject(searchInstancesIt.next()))
                searchInstancesIt.remove();
        
        final List<Instance> dominators = new ArrayList(getDominatorRoots(searchInstances));
        
        if (aggregation == 0) {
            NodesComputer<Instance> computer = new NodesComputer<Instance>(UIThresholds.MAX_TOPLEVEL_INSTANCES) {
                protected boolean sorts(DataType dataType) {
                    return !DataType.COUNT.equals(dataType);
                }
                protected HeapWalkerNode createNode(Instance instance) {
                    RObject robject = new RObject(instance);
                    return new RObjectNode(robject);
                }
                protected Iterator<Instance> objectsIterator(int index) {
                    return dominators.listIterator(index);
                }
                protected String getMoreNodesString(String moreNodesCount)  {
                    return "<another " + moreNodesCount + " dominators left>";
                }
                protected String getSamplesContainerString(String objectsCount)  {
                    return "<sample " + objectsCount + " dominators>";
                }
                protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                    return "<dominators " + firstNodeIdx + "-" + lastNodeIdx + ">";
                }
            };
            
            HeapWalkerNode[] nodes = computer.computeNodes(parent, heap, viewID, null, dataTypes, sortOrders);
            return nodes.length > 0 ? nodes : new HeapWalkerNode[] { new TextNode("<No dominators found>") };
        } else {
            List<HeapWalkerNode> nodes = new ArrayList();
            Map<String, RObjectsContainer> types = new HashMap();
            
            for (Instance dominator : dominators) {
                RObject robject = new RObject(dominator);
                String type = robject.getType();
                RObjectsContainer typeNode = types.get(type);

                if (typeNode == null) {
                    typeNode = new RObjectsContainer(type);
                    nodes.add(typeNode);
                    types.put(type, typeNode);
                }
                
                typeNode.add(robject, heap);
            }
            
            if (nodes.isEmpty()) nodes.add(new TextNode("<No dominators found>"));
            return nodes.toArray(HeapWalkerNode.NO_NODES);
        }
        
        
//        dominators = getDominatorRoots(searchInstances);
//        
//        Map<String, RObjectsContainer> types = new HashMap();
//
//        
//        List<HeapWalkerNode> nodes = new ArrayList();
//        for (Instance dominator : dominators) {
//            RObject dobject = new RObject(dominator);
//            String type = dobject.getType();
//            type = type.substring(type.lastIndexOf('.') + 1);
//
//            RObjectsContainer typeNode = types.get(type);
//            if (typeNode == null) {
//                if (aggregation == 0) {
//                    typeNode = PLACEHOLDER;
//                } else {
//                    typeNode = new RObjectsContainer(type, Integer.MAX_VALUE);
//                    nodes.add(typeNode);
//                }
//                types.put(type, typeNode);
//            }
//            if (typeNode != null) {
//                if (aggregation == 0) {
//                    nodes.add(new RObjectNode(dobject));
//                } else {
//                    typeNode.add(dobject, heap);
//                }
//            }
//        }
//        
//        if (nodes.isEmpty()) nodes.add(new TextNode("<No dominators found>"));
//        return nodes.toArray(HeapWalkerNode.NO_NODES);
    }
    
    public static HeapWalkerNode[] getGCRoots(HeapWalkerNode parent, HeapContext context, String viewID, HeapWalkerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, int aggregation) {
        RHeapFragment fragment = (RHeapFragment)context.getFragment();
        final Heap heap = fragment.getHeap();
        
        Iterator<Instance> instancesI = fragment.getRObjectsIterator();
        
        if (aggregation == 0) {
            final List<Instance> gcRoots = new ArrayList();
            while (instancesI.hasNext()) {
                Instance instance = instancesI.next();
                if (instance.isGCRoot()) gcRoots.add(instance);
            }
            
            NodesComputer<Instance> computer = new NodesComputer<Instance>(UIThresholds.MAX_TOPLEVEL_INSTANCES) {
                protected boolean sorts(DataType dataType) {
                    return !DataType.COUNT.equals(dataType);
                }
                protected HeapWalkerNode createNode(Instance instance) {
                    RObject robject = new RObject(instance);
                    return new RObjectNode(robject);
                }
                protected Iterator<Instance> objectsIterator(int index) {
                    return gcRoots.listIterator(index);
                }
                protected String getMoreNodesString(String moreNodesCount)  {
                    return "<another " + moreNodesCount + " GC roots left>";
                }
                protected String getSamplesContainerString(String objectsCount)  {
                    return "<sample " + objectsCount + " GC roots>";
                }
                protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                    return "<GC roots " + firstNodeIdx + "-" + lastNodeIdx + ">";
                }
            };

            HeapWalkerNode[] nodes = computer.computeNodes(parent, heap, viewID, null, dataTypes, sortOrders);
            return nodes.length > 0 ? nodes : new HeapWalkerNode[] { new TextNode("<No GC roots found>") };
        } else {
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
                    typeNode = new RObjectsContainer(type);
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
