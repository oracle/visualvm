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
package com.sun.tools.visualvm.heapviewer.truffle;

import com.sun.tools.visualvm.heapviewer.HeapContext;
import com.sun.tools.visualvm.heapviewer.model.DataType;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNode;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNodeFilter;
import com.sun.tools.visualvm.heapviewer.model.Progress;
import com.sun.tools.visualvm.heapviewer.model.TextNode;
import com.sun.tools.visualvm.heapviewer.ui.UIThresholds;
import com.sun.tools.visualvm.heapviewer.utils.NodesComputer;
import com.sun.tools.visualvm.heapviewer.utils.ProgressIterator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.swing.SortOrder;
import org.netbeans.lib.profiler.heap.GCRoot;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;

/**
 *
 * @author Jiri Sedlacek
 */
public class TruffleObjectsProvider<O extends TruffleObject, T extends TruffleType<O>, F extends TruffleLanguageHeapFragment<O, T>, L extends TruffleLanguage<O, T, F>> {
    
    private final L language;
    
    
    public TruffleObjectsProvider(L language) {
        this.language = language;
    }
    
    
    public HeapViewerNode[] getAllObjects(HeapViewerNode parent, HeapContext context, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress, int aggregation) {
        final TruffleLanguageHeapFragment fragment = (TruffleLanguageHeapFragment)context.getFragment();
        final Heap heap = fragment.getHeap();
        
        HeapViewerNode[] nodes;
        if (aggregation == 0) {
            NodesComputer<O> computer = new NodesComputer<O>(UIThresholds.MAX_TOPLEVEL_INSTANCES) {
                protected boolean sorts(DataType dataType) {
                    return !DataType.COUNT.equals(dataType);
                }
                protected HeapViewerNode createNode(O object) {
                    return (HeapViewerNode)language.createObjectNode(object, object.getType(heap));
                }
                protected ProgressIterator<O> objectsIterator(int index, Progress progress) {
                    Iterator<O> objects = fragment.getObjectsIterator();
                    return new ProgressIterator(objects, index, true, progress);
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
            
            nodes = computer.computeNodes(parent, heap, viewID, viewFilter, dataTypes, sortOrders, progress);
        } else {
            NodesComputer<T> computer = new NodesComputer<T>(UIThresholds.MAX_TOPLEVEL_CLASSES) {
                protected boolean sorts(DataType dataType) {
                    return true;
                }
                protected HeapViewerNode createNode(T type) {
                    return language.createTypeNode(type, heap);
                }
                protected ProgressIterator<T> objectsIterator(int index, Progress progress) {
                    List<T> types = fragment.getTypes(progress);
                    Iterator<T> typesI = types.listIterator(index);
                    return new ProgressIterator(typesI, index, false, progress);
                }
                protected String getMoreNodesString(String moreNodesCount)  {
                    return "<another " + moreNodesCount + " types left>";
                }
                protected String getSamplesContainerString(String objectsCount)  {
                    return "<sample " + objectsCount + " types>";
                }
                protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                    return "<types " + firstNodeIdx + "-" + lastNodeIdx + ">";
                }
            };

            nodes = computer.computeNodes(parent, heap, viewID, viewFilter, dataTypes, sortOrders, progress);
        }
        
        return nodes.length > 0 ? nodes : new HeapViewerNode[] { new TextNode("<No objects found>") };
    }
    
    public HeapViewerNode[] getDominators(HeapViewerNode parent, HeapContext context, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress, int aggregation) {
        final Heap heap = context.getFragment().getHeap();
        
        if (!DataType.RETAINED_SIZE.valuesAvailable(heap))
            return new HeapViewerNode[] { new TextNode("<Retained sizes not computed yet>") };
        
        int maxSearchInstances = 10000;
        
        List<Instance> searchInstances = heap.getBiggestObjectsByRetainedSize(maxSearchInstances);
        Iterator<Instance> searchInstancesIt = searchInstances.iterator();
        progress.setupKnownSteps(searchInstances.size());
        
        while (searchInstancesIt.hasNext()) {
            Instance instance = searchInstancesIt.next();
            progress.step();
            if (!language.isLanguageObject(instance)) searchInstancesIt.remove();
        }
        
        progress.finish();
        
        final List<Instance> dominators = new ArrayList(getDominatorRoots(searchInstances));
        
        HeapViewerNode[] nodes;
        if (aggregation == 0) {
            NodesComputer<Instance> computer = new NodesComputer<Instance>(UIThresholds.MAX_TOPLEVEL_INSTANCES) {
                protected boolean sorts(DataType dataType) {
                    return !DataType.COUNT.equals(dataType);
                }
                protected HeapViewerNode createNode(Instance instance) {
                    O object = language.createObject(instance);
                    return (HeapViewerNode)language.createObjectNode(object, object.getType(heap));
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
            
            nodes = computer.computeNodes(parent, heap, viewID, viewFilter, dataTypes, sortOrders, progress);
        } else {
            progress.setupKnownSteps(dominators.size());
            
            TruffleType.TypesComputer<O, T> tcomputer = new TruffleType.TypesComputer(language, heap);
            
            for (Instance dominator : dominators) {
                progress.step();
                tcomputer.addObject(language.createObject(dominator));
            }
            
            final List<T> types = tcomputer.getTypes();
            
            progress.finish();
            
            NodesComputer<T> computer = new NodesComputer<T>(UIThresholds.MAX_TOPLEVEL_INSTANCES) {
                protected boolean sorts(DataType dataType) {
                    return true;
                }
                protected HeapViewerNode createNode(T type) {
                    return language.createTypeNode(type, heap);
                }
                protected ProgressIterator<T> objectsIterator(int index, Progress progress) {
                    Iterator<T> typesIt = types.listIterator(index);
                    return new ProgressIterator(typesIt, index, false, progress);
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
            
            nodes = computer.computeNodes(parent, heap, viewID, viewFilter, dataTypes, sortOrders, progress);
        }
        
        return nodes.length > 0 ? nodes : new HeapViewerNode[] { new TextNode("<No dominators found>") };
    }
    
    public HeapViewerNode[] getGCRoots(HeapViewerNode parent, HeapContext context, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress, int aggregation) {
        final Heap heap = context.getFragment().getHeap();
        
        progress.setupUnknownSteps();
        
        final List<GCRoot> gcroots = new ArrayList(heap.getGCRoots());
        Iterator<GCRoot> gcrootsI = gcroots.iterator();
        while (gcrootsI.hasNext()) {
            Instance instance = gcrootsI.next().getInstance();
            if (!language.isLanguageObject(instance)) gcrootsI.remove();
            progress.step();
        }
        
        progress.finish();
        
        HeapViewerNode[] nodes;
        if (aggregation == 0) {
            NodesComputer<GCRoot> computer = new NodesComputer<GCRoot>(UIThresholds.MAX_TOPLEVEL_INSTANCES) {
                protected boolean sorts(DataType dataType) {
                    return !DataType.COUNT.equals(dataType);
                }
                protected HeapViewerNode createNode(GCRoot gcroot) {
                    O object = language.createObject(gcroot.getInstance());
                    return (HeapViewerNode)language.createObjectNode(object, object.getType(heap));
                }
                protected ProgressIterator<GCRoot> objectsIterator(int index, Progress progress) {
                    Iterator<GCRoot> gcRootsIt = gcroots.listIterator(index);
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

            nodes = computer.computeNodes(parent, heap, viewID, viewFilter, dataTypes, sortOrders, progress);
        } else {
            progress.setupUnknownSteps();
            
            TruffleType.TypesComputer<O, T> tcomputer = new TruffleType.TypesComputer(language, heap);
            
            for (GCRoot gcroot : gcroots) {
                tcomputer.addObject(language.createObject(gcroot.getInstance()));
                progress.step();
            }
            final List<T> types = tcomputer.getTypes();
            
            progress.finish();
            
            NodesComputer<T> computer = new NodesComputer<T>(UIThresholds.MAX_TOPLEVEL_INSTANCES) {
                protected boolean sorts(DataType dataType) {
                    return true;
                }
                protected HeapViewerNode createNode(T type) {
                    return language.createTypeNode(type, heap);
                }
                protected ProgressIterator<T> objectsIterator(int index, Progress progress) {
                    Iterator<T> typesIt = types.listIterator(index);
                    return new ProgressIterator(typesIt, index, false, progress);
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
            
            nodes = computer.computeNodes(parent, heap, viewID, viewFilter, dataTypes, sortOrders, progress);
        }
        
        return nodes.length > 0 ? nodes : new HeapViewerNode[] { new TextNode("<No GC roots found>") };
    }
    
    
    public static Set<Instance> getDominatorRoots(List<Instance> searchInstances) {
        Set<Instance> dominators = new HashSet(searchInstances);
        Set<Instance> removed = new HashSet();

        for (Instance instance : searchInstances) {
            if (dominators.contains(instance)) {
                Instance dom = instance;
                long retainedSize = instance.getRetainedSize();

                while (!instance.isGCRoot()) {
                    instance = instance.getNearestGCRootPointer();
                    if (dominators.contains(instance) && instance.getRetainedSize()>=retainedSize) {
                        dominators.remove(dom);
                        removed.add(dom);
                        dom = instance;
                        retainedSize = instance.getRetainedSize();
                    }
                    if (removed.contains(instance)) {
                        dominators.remove(dom);
                        removed.add(dom);
                        break;
                    }
                }
            }
        }
        
        return dominators;
    }
    
}
