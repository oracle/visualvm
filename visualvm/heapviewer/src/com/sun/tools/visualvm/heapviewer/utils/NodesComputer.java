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

package com.sun.tools.visualvm.heapviewer.utils;

import java.text.Format;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.swing.SortOrder;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.ui.Formatters;
import com.sun.tools.visualvm.heapviewer.model.DataType;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNode;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNodeFilter;
import com.sun.tools.visualvm.heapviewer.model.Progress;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "NodesComputer_MoreNodes=<another {0} objects left>",
    "NodesComputer_SamplesContainer=<sample {0} objects>",
    "NodesComputer_NodesContainer=<objects {0}-{1}>"
})
public abstract class NodesComputer<T> {
    
    private static final int EXTRA_ALLOWED_ITEMS = 10;
    
    private final int itemsCount;
    private final int maxItemsCount;
    
    
    public NodesComputer(int maxItemsCount) {
        this(Integer.MAX_VALUE, maxItemsCount);
    }
    
    // TODO: itemsCount may be long, disable random access and provide just <next N objects>
    public NodesComputer(int itemsCount, int maxItemsCount) {
        this.itemsCount = itemsCount;
        this.maxItemsCount = maxItemsCount;
    }
    
    
    protected abstract boolean sorts(DataType dataType);
    
    protected abstract HeapViewerNode createNode(T object);
    
    protected abstract ProgressIterator<T> objectsIterator(int index, Progress progress);
    
    
    protected String getMoreNodesString(String moreNodesCount)  {
        return Bundle.NodesComputer_MoreNodes(moreNodesCount);
    }
    
    protected String getSamplesContainerString(String objectsCount)  {
        return Bundle.NodesComputer_SamplesContainer(objectsCount);
    }
    
    protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
        return Bundle.NodesComputer_NodesContainer(firstNodeIdx, lastNodeIdx);
    }
    
    
    public HeapViewerNode[] computeNodes(HeapViewerNode parent, final Heap heap, String viewID, final HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) {
        if (itemsCount <= (maxItemsCount + EXTRA_ALLOWED_ITEMS)) {
            // All objects unsorted
            HeapViewerNode[] nodes = new HeapViewerNode[itemsCount];
            int i = 0;
            Iterator<HeapViewerNode> nodesIt = nodesIterator(0, viewFilter, heap, progress);
            // Do not count progress, expected to perform fast
            while (nodesIt.hasNext()) nodes[i++] = nodesIt.next();
            if (i < itemsCount) nodes = Arrays.copyOf(nodes, i);
            return nodes;
        } else {
            // First N objects
            DataType dataType = dataTypes == null || dataTypes.isEmpty() ? null : dataTypes.get(0);
            if (dataType != null && !sorts(dataType)) dataType = null;
            
            SortOrder sortOrder = dataType == null || sortOrders == null || sortOrders.isEmpty() ? null : sortOrders.get(0);
            
            if (itemsCount < Integer.MAX_VALUE && viewFilter == null && (dataType == null || sortOrder == null || SortOrder.UNSORTED.equals(sortOrder))) {
                // First N objects unsorted
                NodesIterator nodesIt = nodesIterator(0, viewFilter, heap, progress);
                HeapViewerNode[] nodes = new HeapViewerNode[maxItemsCount + 1];
                // Do not count progress, expected to perform fast
                for (int i = 0; i < maxItemsCount; i++) if (nodesIt.hasNext()) nodes[i] = nodesIt.next();
                
                Format format = Formatters.numberFormat();
                String moreNodesString = getMoreNodesString(format.format(itemsCount - maxItemsCount));
                nodes[maxItemsCount] = new MoreObjectsNode<T>(moreNodesString, itemsCount, itemsCount, nodesIt.nextObject, maxItemsCount - 1) {
                    protected boolean sorts(DataType dataType) {
                        return NodesComputer.this.sorts(dataType);
                    }
                    protected HeapViewerNode createNode(T object) {
                        return NodesComputer.this.createNode(object);
                    }
                    protected Iterator<T> objectsIterator(int index, Progress progress) {
                        return NodesComputer.this.objectsIterator(index, 0, -1, viewFilter, heap, progress);
                    }
                    protected String getSamplesContainerString(String objectsCount)  {
                        return NodesComputer.this.getSamplesContainerString(objectsCount);
                    }
                    protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                        return NodesComputer.this.getNodesContainerString(firstNodeIdx, lastNodeIdx);
                    }
                };
                
                return nodes;
            } else {
                // First N objects according to the provided sorting
                SortedObjectsBuffer<T> buffer = new SortedObjectsBuffer<T>(maxItemsCount, null, dataType, sortOrder, null, heap, parent) {
                    protected boolean sorts(DataType dataType) { return NodesComputer.this.sorts(dataType); }
                    protected HeapViewerNode createNode(T object) { return NodesComputer.this.createNode(object); }
                };
                
                if (itemsCount == Integer.MAX_VALUE) progress.setupUnknownSteps();
                else progress.setupKnownSteps(itemsCount);
                
                ObjectsIterator objectsIt = objectsIterator(0, 0, -1, viewFilter, heap, progress);
                while (objectsIt.hasNext()) buffer.add(objectsIt.next());
                T[] objects = buffer.getObjects();
                
                progress.finish();
                
                int objectsCount = objects.length;
//                final int totalObjectsCount = buffer.getTotalObjects();
                final int totalOwnItems = objectsIt.getTotalOwnItems();
                if (objectsCount == totalOwnItems) {
                    // No MoreNodesNode needed
                    HeapViewerNode[] nodes = new HeapViewerNode[objectsCount];
                    for (int i = 0; i < objectsCount; i++) nodes[i] = createNode(objects[i]);
                    return nodes;
                } else {
                    // ModeNodesNode needed
                    HeapViewerNode[] nodes = new HeapViewerNode[objectsCount + 1];
                    T lastObject = null;
                    for (int i = 0; i < objectsCount; i++) {
                        nodes[i] = createNode(objects[i]);
                        if (i == maxItemsCount - 1) lastObject = objects[i];
                    }
                    
                    final int firstOwnItem = objectsIt.getFirstOwnItem();

                    Format format = Formatters.numberFormat();
                    String moreNodesString = getMoreNodesString(format.format(totalOwnItems - maxItemsCount));
                    nodes[objectsCount] = new MoreObjectsNode<T>(moreNodesString, totalOwnItems, objectsIt.getTotalItems(), lastObject, objectsCount - 1) {
                        protected boolean sorts(DataType dataType) {
                            return NodesComputer.this.sorts(dataType);
                        }
                        protected HeapViewerNode createNode(T object) {
                            return NodesComputer.this.createNode(object);
                        }
                        protected Iterator<T> objectsIterator(int index, Progress progress) {
                            return NodesComputer.this.objectsIterator(index, firstOwnItem, totalOwnItems, viewFilter, heap, progress);
                        }
                        protected String getSamplesContainerString(String objectsCount)  {
                            return NodesComputer.this.getSamplesContainerString(objectsCount);
                        }
                        protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                            return NodesComputer.this.getNodesContainerString(firstNodeIdx, lastNodeIdx);
                        }
                    };

                    return nodes;
                }
            }
        }
    }
    
    
    public static Iterator<Integer> integerIterator(final int start, final int end) {
        return new Iterator<Integer>() {
            private int value = start;
            private final int endValue = end;
            public boolean hasNext() { return value < endValue; }
            public Integer next() { return value++; }
        };
    }
    
    
    private ObjectsIterator objectsIterator(int index, int knownInnerStart, int knownOuterCount, HeapViewerNodeFilter viewFilter, Heap heap, Progress progress) {
        return viewFilter == null ? new PlainObjectsIterator(index, progress) :
               new FilteredObjectsIterator(index, knownInnerStart <= 0 ? 0 : knownInnerStart - 1, knownOuterCount, viewFilter, heap, progress);
    }
    
    private NodesIterator nodesIterator(int index, HeapViewerNodeFilter viewFilter, Heap heap, Progress progress) {
        return viewFilter == null ? new PlainNodesIterator(index, progress) :
               new FilteredNodesIterator(index, viewFilter, heap, progress);
    }
    
    
    private abstract class ObjectsIterator implements Iterator<T> {
        
        int totalItems;
        
        int firstOwnItem;
        int totalOwnItems;
        
        int getTotalItems() { return totalItems; }
        
        int getFirstOwnItem() { return firstOwnItem; }
        int getTotalOwnItems() { return totalOwnItems; }
    }
    
    private class PlainObjectsIterator extends ObjectsIterator {
        
        private final Iterator<T> iterator;
        
        PlainObjectsIterator(int index, Progress progress) {
            this.iterator = objectsIterator(index, progress);
            totalItems = index;
            firstOwnItem = index;
        }
        
        public boolean hasNext() {
            return iterator.hasNext();
        }
        
        public T next() {
            totalItems++;
            totalOwnItems++;
            return iterator.next();
        }
        
    }
    
    private class FilteredObjectsIterator extends ObjectsIterator {
        
        private final Iterator<T> iterator;
        private final int knownTotalOwnItems;
        
        private final HeapViewerNodeFilter viewFilter;
        private final Heap heap;
        
        private T nextObject;
        
        FilteredObjectsIterator(int index, int knownFirstOwnItem, int knownTotalOwnItems, HeapViewerNodeFilter viewFilter, Heap heap, Progress progress) {
            this.iterator = objectsIterator(knownFirstOwnItem, progress);
            this.knownTotalOwnItems = knownTotalOwnItems;
            
            this.viewFilter = viewFilter;
            this.heap = heap;
            
            firstOwnItem = -1;
            while (index-- > 0) hasNext();
        }
        
        // NOTE: must always be called before next() to compute the next value!
        public boolean hasNext() {
            if (knownTotalOwnItems >= 0 && knownTotalOwnItems == totalOwnItems) return false;
            nextObject = nextObject();
            return nextObject != null;
        }
        
        public T next() {
            totalOwnItems++;
            return nextObject;
        }
        
        private T nextObject() {
            while (iterator.hasNext()) {
                totalItems++;
                T object = iterator.next();
                HeapViewerNode node = createNode(object);
                if (viewFilter.passes(node, heap)) {
                    if (firstOwnItem == -1) firstOwnItem = totalItems;
                    return object;
                }
            }
            return null;
        }
        
    }
    
    
    private abstract class NodesIterator implements Iterator<HeapViewerNode> {
        
        T nextObject;
        
        T nextObject() { return nextObject; }
        
    }
    
    private class PlainNodesIterator extends NodesIterator {
        
        private final Iterator<T> iterator;
        
        PlainNodesIterator(int index, Progress progress) {
            this.iterator = objectsIterator(index, progress);
        }
        
        public boolean hasNext() {
            return iterator.hasNext();
        }
        
        public HeapViewerNode next() {
            nextObject = iterator.next();
            return createNode(nextObject);
        }
        
    }
    
    private class FilteredNodesIterator extends NodesIterator {
        
        private final Iterator<T> iterator;
        
        private final HeapViewerNodeFilter viewFilter;
        private final Heap heap;
        
        private HeapViewerNode nextNode;
        
        FilteredNodesIterator(int index, HeapViewerNodeFilter viewFilter, Heap heap, Progress progress) {
            this.iterator = objectsIterator(0, progress);
            
            this.viewFilter = viewFilter;
            this.heap = heap;
            
            while (index-- > 0) hasNext();
        }
        
        // NOTE: must always be called before next() to compute the next value!
        public boolean hasNext() {
            nextNode = nextNode();
            return nextNode != null;
        }
        
        public HeapViewerNode next() {
            return nextNode;
        }
        
        private HeapViewerNode nextNode() {
            while (iterator.hasNext()) {
                nextObject = iterator.next();
                HeapViewerNode node = createNode(nextObject);
                if (viewFilter.passes(node, heap)) return node;
            }
            nextObject = null;
            return null;
        }
        
    }
    
}
