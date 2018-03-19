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
package com.sun.tools.visualvm.heapviewer.truffle.r;

import com.sun.tools.visualvm.heapviewer.truffle.AbstractObjectsProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.SortOrder;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import com.sun.tools.visualvm.heapviewer.HeapContext;
import com.sun.tools.visualvm.heapviewer.model.DataType;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNode;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNodeFilter;
import com.sun.tools.visualvm.heapviewer.model.Progress;
import com.sun.tools.visualvm.heapviewer.model.TextNode;
import com.sun.tools.visualvm.heapviewer.ui.UIThresholds;
import com.sun.tools.visualvm.heapviewer.utils.NodesComputer;
import com.sun.tools.visualvm.heapviewer.utils.ProgressIterator;

/**
 *
 * @author Jiri Sedlacek
 */
public class RObjectsProvider extends AbstractObjectsProvider {
    
    static HeapViewerNode[] getAllObjects(HeapViewerNode parent, HeapContext context, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress, int aggregation) {
        final RHeapFragment fragment = (RHeapFragment)context.getFragment();
        final Heap heap = fragment.getHeap();
        
        if (aggregation == 0) {
            NodesComputer<Instance> computer = new NodesComputer<Instance>(UIThresholds.MAX_TOPLEVEL_INSTANCES) {
                protected boolean sorts(DataType dataType) {
                    return !DataType.COUNT.equals(dataType);
                }
                protected HeapViewerNode createNode(Instance instance) {
                    RObject robject = new RObject(instance);
                    return new RObjectNode(robject);
                }
                protected ProgressIterator<Instance> objectsIterator(int index, Progress progress) {
                    Iterator<Instance> rinstances = fragment.getInstancesIterator();
                    return new ProgressIterator(rinstances, index, true, progress);
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

            return computer.computeNodes(parent, heap, viewID, null, dataTypes, sortOrders, progress);
        } else {
            List<HeapViewerNode> nodes = new ArrayList();
            Map<String, RObjectsContainer> types = new HashMap();
            
            Iterator<Instance> instances = fragment.getInstancesIterator();
            progress.setupUnknownSteps();
            
            while (instances.hasNext()) {
                Instance instance = instances.next();
                progress.step();
                String type = RObject.getType(instance);
                RObjectsContainer typeNode = types.get(type);

                if (typeNode == null) {
                    typeNode = new RObjectsContainer(type);
                    nodes.add(typeNode);
                    types.put(type, typeNode);
                }
                
                typeNode.add(instance, heap);
            }
            
            progress.finish();
            
            return nodes.toArray(HeapViewerNode.NO_NODES);
        }
    }
    
    public static HeapViewerNode[] getDominators(HeapViewerNode parent, HeapContext context, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress, int aggregation) {
        Heap heap = context.getFragment().getHeap();
        
        if (!DataType.RETAINED_SIZE.valuesAvailable(heap))
            return new HeapViewerNode[] { new TextNode("<Retained sizes not computed yet>") };
        
        int maxSearchInstances = 10000;
        
        List<Instance> searchInstances = heap.getBiggestObjectsByRetainedSize(maxSearchInstances);
        Iterator<Instance> searchInstancesIt = searchInstances.iterator();
        progress.setupKnownSteps(searchInstances.size());
        
        while (searchInstancesIt.hasNext()) {
            Instance instance = searchInstancesIt.next();
            progress.step();
            if (!RObject.isRObject(instance))
                searchInstancesIt.remove();
        }
        
        progress.finish();
        
        final List<Instance> dominators = new ArrayList(getDominatorRoots(searchInstances));
        
        if (aggregation == 0) {
            NodesComputer<Instance> computer = new NodesComputer<Instance>(UIThresholds.MAX_TOPLEVEL_INSTANCES) {
                protected boolean sorts(DataType dataType) {
                    return !DataType.COUNT.equals(dataType);
                }
                protected HeapViewerNode createNode(Instance instance) {
                    RObject robject = new RObject(instance);
                    return new RObjectNode(robject);
                }
                protected ProgressIterator<Instance> objectsIterator(int index, Progress progress) {
                    Iterator<Instance> dominatorsIt = dominators.listIterator(index);
                    return new ProgressIterator(dominatorsIt, index, false, progress);
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
            
            HeapViewerNode[] nodes = computer.computeNodes(parent, heap, viewID, null, dataTypes, sortOrders, progress);
            return nodes.length > 0 ? nodes : new HeapViewerNode[] { new TextNode("<No dominators found>") };
        } else {
            List<HeapViewerNode> nodes = new ArrayList();
            Map<String, RObjectsContainer> types = new HashMap();
            
            progress.setupKnownSteps(dominators.size());
            
            for (Instance dominator : dominators) {
                progress.step();
                String type = RObject.getType(dominator);
                RObjectsContainer typeNode = types.get(type);

                if (typeNode == null) {
                    typeNode = new RObjectsContainer(type);
                    nodes.add(typeNode);
                    types.put(type, typeNode);
                }
                
                typeNode.add(dominator, heap);
            }
            
            progress.finish();
            
            if (nodes.isEmpty()) nodes.add(new TextNode("<No dominators found>"));
            return nodes.toArray(HeapViewerNode.NO_NODES);
        }
    }
    
    public static HeapViewerNode[] getGCRoots(HeapViewerNode parent, HeapContext context, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress, int aggregation) {
        RHeapFragment fragment = (RHeapFragment)context.getFragment();
        final Heap heap = fragment.getHeap();
        
        Iterator<Instance> instancesI = fragment.getInstancesIterator();
        
        if (aggregation == 0) {
            progress.setupUnknownSteps();
            
            final List<Instance> gcRoots = new ArrayList();
            while (instancesI.hasNext()) {
                Instance instance = instancesI.next();
                progress.step();
                if (instance.isGCRoot()) gcRoots.add(instance);
            }
            
            progress.finish();
            
            NodesComputer<Instance> computer = new NodesComputer<Instance>(UIThresholds.MAX_TOPLEVEL_INSTANCES) {
                protected boolean sorts(DataType dataType) {
                    return !DataType.COUNT.equals(dataType);
                }
                protected HeapViewerNode createNode(Instance instance) {
                    RObject robject = new RObject(instance);
                    return new RObjectNode(robject);
                }
                protected ProgressIterator<Instance> objectsIterator(int index, Progress progress) {
                    Iterator<Instance> gcRootsIt = gcRoots.listIterator(index);
                    return new ProgressIterator(gcRootsIt, index, false, progress);
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

            HeapViewerNode[] nodes = computer.computeNodes(parent, heap, viewID, null, dataTypes, sortOrders, progress);
            return nodes.length > 0 ? nodes : new HeapViewerNode[] { new TextNode("<No GC roots found>") };
        } else {
            List<HeapViewerNode> nodes = new ArrayList();
            Map<String, RObjectsContainer> types = new HashMap();
            
            progress.setupUnknownSteps();
            
            while (instancesI.hasNext()) {
                Instance instance = instancesI.next();
                progress.step();
                if (!instance.isGCRoot()) continue;
                
                String type = RObject.getType(instance);
//                type = type.substring(type.lastIndexOf('.') + 1);
                RObjectsContainer typeNode = types.get(type);

                if (typeNode == null) {
                    typeNode = new RObjectsContainer(type);
                    nodes.add(typeNode);
                    types.put(type, typeNode);
                }
                
                typeNode.add(instance, heap);
            }
            
            progress.finish();
            
            if (nodes.isEmpty()) nodes.add(new TextNode("<No GC roots found>"));
            return nodes.toArray(HeapViewerNode.NO_NODES);
        }
    }
    
}
