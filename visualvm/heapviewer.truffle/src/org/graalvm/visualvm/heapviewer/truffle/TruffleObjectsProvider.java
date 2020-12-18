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
package org.graalvm.visualvm.heapviewer.truffle;

import org.graalvm.visualvm.heapviewer.HeapContext;
import org.graalvm.visualvm.heapviewer.model.DataType;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNode;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNodeFilter;
import org.graalvm.visualvm.heapviewer.model.Progress;
import org.graalvm.visualvm.heapviewer.model.TextNode;
import org.graalvm.visualvm.heapviewer.ui.UIThresholds;
import org.graalvm.visualvm.heapviewer.utils.NodesComputer;
import org.graalvm.visualvm.heapviewer.utils.ProgressIterator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.SortOrder;
import org.graalvm.visualvm.lib.jfluid.heap.GCRoot;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "TruffleObjectsProvider_MoreNodesObjects=<another {0} objects left>",
    "TruffleObjectsProvider_SamplesContainerObjects=<sample {0} objects>",
    "TruffleObjectsProvider_NodesContainerObjects=<objects {0}-{1}>",
    "TruffleObjectsProvider_NoObjects=<no objects found>",
    "TruffleObjectsProvider_MoreNodesTypes=<another {0} types left>",
    "TruffleObjectsProvider_SamplesContainerTypes=<sample {0} types>",
    "TruffleObjectsProvider_NodesContainerTypes=<types {0}-{1}>",
    "TruffleObjectsProvider_NoRetainedSizes=<retained sizes not computed yet>",
    "TruffleObjectsProvider_MoreNodesDominators=<another {0} dominators left>",
    "TruffleObjectsProvider_SamplesContainerDominators=<sample {0} dominators>",
    "TruffleObjectsProvider_NodesContainerDominators=<dominators {0}-{1}>",
    "TruffleObjectsProvider_NoDominators=<no dominators found>",
    "TruffleObjectsProvider_MoreNodesGcRoots=<another {0} GC roots left>",
    "TruffleObjectsProvider_SamplesContainerGcRoots=<sample {0} GC roots>",
    "TruffleObjectsProvider_NodesContainerGcRoots=<GC roots {0}-{1}>",
    "TruffleObjectsProvider_NoGcRoots=<no GC roots found>"
})
public class TruffleObjectsProvider<O extends TruffleObject, T extends TruffleType<O>, F extends TruffleLanguageHeapFragment<O, T>, L extends TruffleLanguage<O, T, F>> {
    
    private final L language;
    
    
    public TruffleObjectsProvider(L language) {
        this.language = language;
    }
    
    
    public HeapViewerNode[] getAllObjects(HeapViewerNode parent, HeapContext context, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress, int aggregation) throws InterruptedException {
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
                    return Bundle.TruffleObjectsProvider_MoreNodesObjects(moreNodesCount);
                }
                protected String getSamplesContainerString(String objectsCount)  {
                    return Bundle.TruffleObjectsProvider_SamplesContainerObjects(objectsCount);
                }
                protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                    return Bundle.TruffleObjectsProvider_NodesContainerObjects(firstNodeIdx, lastNodeIdx);
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
                    return Bundle.TruffleObjectsProvider_MoreNodesTypes(moreNodesCount);
                }
                protected String getSamplesContainerString(String objectsCount)  {
                    return Bundle.TruffleObjectsProvider_SamplesContainerTypes(objectsCount);
                }
                protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                    return Bundle.TruffleObjectsProvider_NodesContainerTypes(firstNodeIdx, lastNodeIdx);
                }
            };

            nodes = computer.computeNodes(parent, heap, viewID, viewFilter, dataTypes, sortOrders, progress);
        }
        
        return nodes == null || nodes.length > 0 ? nodes : new HeapViewerNode[] { new TextNode(Bundle.TruffleObjectsProvider_NoObjects()) };
    }
    
    public HeapViewerNode[] getDominators(HeapViewerNode parent, HeapContext context, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress, int aggregation) throws InterruptedException {
        final Heap heap = context.getFragment().getHeap();
        
        if (!DataType.RETAINED_SIZE.valuesAvailable(heap))
            return new HeapViewerNode[] { new TextNode(Bundle.TruffleObjectsProvider_NoRetainedSizes()) };
        
        int maxSearchInstances = 10000;
        
        List<Instance> searchInstances = heap.getBiggestObjectsByRetainedSize(maxSearchInstances);
        Iterator<Instance> searchInstancesIt = searchInstances.iterator();
        
        try {
            progress.setupKnownSteps(searchInstances.size());
            
            while (searchInstancesIt.hasNext()) {
                Instance instance = searchInstancesIt.next();
                progress.step();
                if (!language.isLanguageObject(instance)) searchInstancesIt.remove();
            }
        } finally {
            progress.finish();
        }
        
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
                    return Bundle.TruffleObjectsProvider_MoreNodesDominators(moreNodesCount);
                }
                protected String getSamplesContainerString(String objectsCount)  {
                    return Bundle.TruffleObjectsProvider_SamplesContainerDominators(objectsCount);
                }
                protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                    return Bundle.TruffleObjectsProvider_NodesContainerDominators(firstNodeIdx, lastNodeIdx);
                }
            };
            
            nodes = computer.computeNodes(parent, heap, viewID, viewFilter, dataTypes, sortOrders, progress);
        } else {
            TruffleType.TypesComputer<O, T> tcomputer = new TruffleType.TypesComputer(language, heap);
            
            try {
                progress.setupKnownSteps(dominators.size());
                
                for (Instance dominator : dominators) {
                    progress.step();
                    tcomputer.addObject(language.createObject(dominator));
                }
            } finally {
                progress.finish();
            }
            
            final List<T> types = tcomputer.getTypes();
            
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
                    return Bundle.TruffleObjectsProvider_MoreNodesDominators(moreNodesCount);
                }
                protected String getSamplesContainerString(String objectsCount)  {
                    return Bundle.TruffleObjectsProvider_SamplesContainerDominators(objectsCount);
                }
                protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                    return Bundle.TruffleObjectsProvider_NodesContainerDominators(firstNodeIdx, lastNodeIdx);
                }
            };
            
            nodes = computer.computeNodes(parent, heap, viewID, viewFilter, dataTypes, sortOrders, progress);
        }
        
        return nodes.length > 0 ? nodes : new HeapViewerNode[] { new TextNode(Bundle.TruffleObjectsProvider_NoDominators()) };
    }
    
    public HeapViewerNode[] getGCRoots(HeapViewerNode parent, HeapContext context, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress, int aggregation) throws InterruptedException {
        final Heap heap = context.getFragment().getHeap();
        final List<GCRoot> gcrootsS = (List<GCRoot>) heap.getGCRoots();
        final List<Instance> gcrootInstances = gcrootsS.stream()
                .map(GCRoot::getInstance)
                .distinct()
                .collect(Collectors.toList());
        try {
            progress.setupUnknownSteps();
            
            Iterator<Instance> gcrootsI = gcrootInstances.iterator();
            while (gcrootsI.hasNext()) {
                Instance instance = gcrootsI.next();
                if (!language.isLanguageObject(instance)) gcrootsI.remove();
                progress.step();
            }
        } finally {
            progress.finish();
        }
        
        HeapViewerNode[] nodes;
        if (aggregation == 0) {
            NodesComputer<Instance> computer = new NodesComputer<Instance>(UIThresholds.MAX_TOPLEVEL_INSTANCES) {
                protected boolean sorts(DataType dataType) {
                    return !DataType.COUNT.equals(dataType);
                }
                protected HeapViewerNode createNode(Instance gcRootInstance) {
                    O object = language.createObject(gcRootInstance);
                    return (HeapViewerNode)language.createObjectNode(object, object.getType(heap));
                }
                protected ProgressIterator<Instance> objectsIterator(int index, Progress progress) {
                    Iterator<Instance> iterator = gcrootInstances.listIterator(index);
                    return new ProgressIterator(iterator, index, false, progress);
                }
                protected String getMoreNodesString(String moreNodesCount)  {
                    return Bundle.TruffleObjectsProvider_MoreNodesGcRoots(moreNodesCount);
                }
                protected String getSamplesContainerString(String objectsCount)  {
                    return Bundle.TruffleObjectsProvider_SamplesContainerGcRoots(objectsCount);
                }
                protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                    return Bundle.TruffleObjectsProvider_NodesContainerGcRoots(firstNodeIdx, lastNodeIdx);
                }
            };

            nodes = computer.computeNodes(parent, heap, viewID, viewFilter, dataTypes, sortOrders, progress);
        } else {
            TruffleType.TypesComputer<O, T> tcomputer = new TruffleType.TypesComputer(language, heap);
            
            try {            
                progress.setupUnknownSteps();
                
                for (Instance gcroot : gcrootInstances) {
                    tcomputer.addObject(language.createObject(gcroot));
                    progress.step();
                }
            } finally {
                progress.finish();
            }
            
            final List<T> types = tcomputer.getTypes();
            
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
                    return Bundle.TruffleObjectsProvider_MoreNodesGcRoots(moreNodesCount);
                }
                protected String getSamplesContainerString(String objectsCount)  {
                    return Bundle.TruffleObjectsProvider_SamplesContainerGcRoots(objectsCount);
                }
                protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                    return Bundle.TruffleObjectsProvider_NodesContainerGcRoots(firstNodeIdx, lastNodeIdx);
                }
            };
            
            nodes = computer.computeNodes(parent, heap, viewID, viewFilter, dataTypes, sortOrders, progress);
        }
        
        return nodes.length > 0 ? nodes : new HeapViewerNode[] { new TextNode(Bundle.TruffleObjectsProvider_NoGcRoots()) };
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
