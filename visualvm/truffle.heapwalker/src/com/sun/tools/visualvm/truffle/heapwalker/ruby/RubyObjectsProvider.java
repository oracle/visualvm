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
package com.sun.tools.visualvm.truffle.heapwalker.ruby;

import com.sun.tools.visualvm.truffle.heapwalker.AbstractObjectsProvider;
import com.sun.tools.visualvm.truffle.heapwalker.DynamicObject;
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
import org.netbeans.modules.profiler.heapwalker.v2.model.TextNode;
import org.netbeans.modules.profiler.heapwalker.v2.model.HeapWalkerNodeFilter;
import org.netbeans.modules.profiler.heapwalker.v2.model.Progress;
import org.netbeans.modules.profiler.heapwalker.v2.ui.UIThresholds;
import org.netbeans.modules.profiler.heapwalker.v2.utils.NodesComputer;
import org.netbeans.modules.profiler.heapwalker.v2.utils.ProgressIterator;

/**
 *
 * @author Jiri Sedlacek
 */
public class RubyObjectsProvider extends AbstractObjectsProvider {
    
    static final String RUBY_LANG_ID = "org.truffleruby.language.RubyObjectType"; // NOI18N
    

    public static HeapWalkerNode[] getAllObjects(HeapWalkerNode parent, HeapContext context, String viewID, HeapWalkerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress, int aggregation) {
        final RubyHeapFragment fragment = (RubyHeapFragment)context.getFragment();
        final Heap heap = fragment.getHeap();
        
        if (aggregation == 0) {
            NodesComputer<DynamicObject> computer = new NodesComputer<DynamicObject>(UIThresholds.MAX_TOPLEVEL_INSTANCES) {
                protected boolean sorts(DataType dataType) {
                    return !DataType.COUNT.equals(dataType);
                }
                protected HeapWalkerNode createNode(DynamicObject dobject) {
                    return new RubyNodes.RubyDynamicObjectNode(dobject, dobject.getType(heap));
                }
                protected ProgressIterator<DynamicObject> objectsIterator(int index, Progress progress) {
                    Iterator<DynamicObject> dobjects = fragment.getRubyObjectsIterator();
                    return new ProgressIterator(dobjects, index, true, progress);
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
            List<HeapWalkerNode> nodes = new ArrayList();
            Map<String, RubyNodes.RubyDynamicObjectsContainer> types = new HashMap();
            
            Iterator<DynamicObject> dobjects = fragment.getRubyObjectsIterator();
            progress.setupUnknownSteps();
                        
            while (dobjects.hasNext()) {
                DynamicObject dobject = dobjects.next();
                progress.step();
                String type = dobject.getType(heap);
                RubyNodes.RubyDynamicObjectsContainer typeNode = types.get(type);

                if (typeNode == null) {
                    typeNode = new RubyNodes.RubyDynamicObjectsContainer(type);
                    nodes.add(typeNode);
                    types.put(type, typeNode);
                }
                
                typeNode.add(dobject, heap);
            }
            
            progress.finish();
            
            return nodes.toArray(HeapWalkerNode.NO_NODES);
        }
    }
    
    public static HeapWalkerNode[] getDominators(HeapWalkerNode parent, HeapContext context, String viewID, HeapWalkerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress, int aggregation) {
        final Heap heap = context.getFragment().getHeap();
        
        if (!DataType.RETAINED_SIZE.valuesAvailable(heap))
            return new HeapWalkerNode[] { new TextNode("<Retained sizes not computed yet>") };
        
        int maxSearchInstances = 10000;
        
        List<Instance> searchInstances = heap.getBiggestObjectsByRetainedSize(maxSearchInstances);
        Iterator<Instance> searchInstancesIt = searchInstances.iterator();
        progress.setupKnownSteps(searchInstances.size());
        
        while (searchInstancesIt.hasNext()) {
            Instance instance = searchInstancesIt.next();
            progress.step();
            if (!DynamicObject.isDynamicObject(instance) || !isRubyObject(new DynamicObject(instance)))
                searchInstancesIt.remove();
        }
        
        progress.finish();
        
        final List<Instance> dominators = new ArrayList(getDominatorRoots(searchInstances));
        
        if (aggregation == 0) {
            NodesComputer<Instance> computer = new NodesComputer<Instance>(UIThresholds.MAX_TOPLEVEL_INSTANCES) {
                protected boolean sorts(DataType dataType) {
                    return !DataType.COUNT.equals(dataType);
                }
                protected HeapWalkerNode createNode(Instance instance) {
                    DynamicObject dobject = new DynamicObject(instance);
                    return new RubyNodes.RubyDynamicObjectNode(dobject, dobject.getType(heap));
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
            
            HeapWalkerNode[] nodes = computer.computeNodes(parent, heap, viewID, null, dataTypes, sortOrders, progress);
            return nodes.length > 0 ? nodes : new HeapWalkerNode[] { new TextNode("<No dominators found>") };
        } else {
            List<HeapWalkerNode> nodes = new ArrayList();
            Map<String, RubyNodes.RubyDynamicObjectsContainer> types = new HashMap();
            
            progress.setupKnownSteps(dominators.size());
            
            for (Instance dominator : dominators) {
                DynamicObject dobject = new DynamicObject(dominator);
                progress.step();
                String type = dobject.getType(heap);
                RubyNodes.RubyDynamicObjectsContainer typeNode = types.get(type);

                if (typeNode == null) {
                    typeNode = new RubyNodes.RubyDynamicObjectsContainer(type);
                    nodes.add(typeNode);
                    types.put(type, typeNode);
                }
                
                typeNode.add(dobject, heap);
            }
            
            progress.finish();
            
            if (nodes.isEmpty()) nodes.add(new TextNode("<No dominators found>"));
            return nodes.toArray(HeapWalkerNode.NO_NODES);
        }
    }
    
    public static HeapWalkerNode[] getGCRoots(HeapWalkerNode parent, HeapContext context, String viewID, HeapWalkerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress, int aggregation) {
        RubyHeapFragment fragment = (RubyHeapFragment)context.getFragment();
        final Heap heap = fragment.getHeap();
        
        Iterator<DynamicObject> dobjects = fragment.getRubyObjectsIterator();
        
        if (aggregation == 0) {
            progress.setupUnknownSteps();
            
            final List<Instance> gcRoots = new ArrayList();
            while (dobjects.hasNext()) {
                DynamicObject dobject = dobjects.next();
                progress.step();
                Instance instance = dobject.getInstance();
                if (instance.isGCRoot()) gcRoots.add(instance);
            }
            
            progress.finish();
            
            NodesComputer<Instance> computer = new NodesComputer<Instance>(UIThresholds.MAX_TOPLEVEL_INSTANCES) {
                protected boolean sorts(DataType dataType) {
                    return !DataType.COUNT.equals(dataType);
                }
                protected HeapWalkerNode createNode(Instance instance) {
                    DynamicObject dobject = new DynamicObject(instance);
                    return new RubyNodes.RubyDynamicObjectNode(dobject, dobject.getType(heap));
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

            HeapWalkerNode[] nodes = computer.computeNodes(parent, heap, viewID, null, dataTypes, sortOrders, progress);
            return nodes.length > 0 ? nodes : new HeapWalkerNode[] { new TextNode("<No GC roots found>") };
        } else {
            List<HeapWalkerNode> nodes = new ArrayList();
            Map<String, RubyNodes.RubyDynamicObjectsContainer> types = new HashMap();
            
            progress.setupUnknownSteps();
            
            while (dobjects.hasNext()) {
                DynamicObject dobject = dobjects.next();
                progress.step();
                Instance instance = dobject.getInstance();
                if (!instance.isGCRoot()) continue;
                
                String type = dobject.getType(heap);
                type = type.substring(type.lastIndexOf('.') + 1);

                RubyNodes.RubyDynamicObjectsContainer typeNode = types.get(type);
                if (typeNode == null) {
                    typeNode = new RubyNodes.RubyDynamicObjectsContainer(type);
                    nodes.add(typeNode);
                    types.put(type, typeNode);
                }
                typeNode.add(dobject, heap);
            }
            
            progress.finish();
            
            if (nodes.isEmpty()) nodes.add(new TextNode("<No GC roots found>"));
            return nodes.toArray(HeapWalkerNode.NO_NODES);
        }
    }
    
    private static boolean isRubyObject(DynamicObject dobject) {
        return RUBY_LANG_ID.equals(dobject.getLanguageId().getName());
    }
    
}
