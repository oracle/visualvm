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

package org.graalvm.visualvm.heapviewer.utils;

import java.util.Arrays;
import java.util.Comparator;
import javax.swing.SortOrder;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.heapviewer.model.DataType;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNode;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNodeFilter;

/**
 *
 * @author Jiri Sedlacek
 */
abstract class SortedObjectsBuffer<T> {
    
    private final int cacheSize;
    private long totalObjects;
    
    private final Wrapper<T>[] elements;
    
    private final boolean sorting;
    private final boolean ascending;
    
    private final DataType dataType;
    private final Heap heap;
    private final HeapViewerNode parent;
    
    private final HeapViewerNodeFilter filter;
    
    private boolean notFull;
    private int currentIndex;
    
    private final T previousObject;
    private boolean previousObjectSeen;
    private final Object previousObjectValue;
    private Object maxObjectValue;
    private int wrapperSerialId;
    
    
    SortedObjectsBuffer(int nodesCount, T previousObject, DataType dataType, SortOrder sortOrder, HeapViewerNodeFilter filter, Heap heap, HeapViewerNode parent) {
        cacheSize = nodesCount;
        
        elements = new Wrapper[cacheSize*2];
        
        sorting = sortOrder != null && !SortOrder.UNSORTED.equals(sortOrder) && dataType != null && sorts(dataType);
        ascending = SortOrder.ASCENDING.equals(sortOrder);
        
        this.dataType = dataType;
        this.heap = heap;
        this.parent = parent;
        
        this.filter = filter;
        
        notFull = true;
        currentIndex = -1;
        
        this.previousObject = previousObject;
        previousObjectSeen = previousObject == null;
        previousObjectValue = previousObjectSeen ? null : HeapViewerNode.getValue(createNode(previousObject), dataType, heap, parent);
    }
    
    
    protected abstract boolean sorts(DataType dataType);
    
    protected abstract HeapViewerNode createNode(T object);
    
    
    void add(T object) {
        HeapViewerNode node = createNode(object);
        
        if (filter != null && !filter.passes(node, heap)) return;
        
        totalObjects++;
        
        // --- not sorting, just adding first N items
        if (!sorting) {
            if (notFull) {
                currentIndex++;
                elements[currentIndex] = createWrapper(object, null);
                if (currentIndex == cacheSize - 1) notFull = false;
            }
            return;
        }
        // ---------------------------------------------------------------------
        
        Object value = HeapViewerNode.getValue(node, dataType, heap, parent);
        
        if (previousObjectValue != null) {
            int comp = compare(value, previousObjectValue);
            if (comp < 0) { /*System.err.println(">>>     skipping " + object + " because lower than " + previousObjectValue);*/ return; }

            if (comp == 0 && !previousObjectSeen) {
                if (previousObject.equals(object)) previousObjectSeen = true;
//                System.err.println(">>>     skipping " + object + " because previous " + previousObject + " just seen " + previousObjectSeen);
                return;
            }
        }
        
        if (maxObjectValue != null) {
            int comp = compare(value, maxObjectValue);
            if (comp >= 0) { /*System.err.println(">>>     skipping " + object + " because higher than " + maxObjectValue);*/ return; }
        }
        currentIndex++;
        elements[currentIndex] = createWrapper(object, value);
        if (currentIndex < elements.length - 1) {
            return;
        }
        Arrays.sort(elements, new WrapperComparator());
        int middleIndex = elements.length/2-1;
        maxObjectValue = elements[middleIndex].value;
        currentIndex = middleIndex;
    }
    
    T[] getObjects() {
        if (sorting) {
            Arrays.fill(elements, currentIndex+1, elements.length-1, null);
            Arrays.sort(elements, new WrapperComparator());
        }
        int size = Math.min(currentIndex+1,elements.length/2);
        T[] objects = (T[]) new Object[size];

        for (int i = 0; i < objects.length; i++) {
            objects[i] = elements[i].object;
        }
        return objects;
    }
    
    long getTotalObjects() {
        return totalObjects;
    }
    
    private Wrapper<T> createWrapper(T obj, Object val) {
        return new Wrapper(wrapperSerialId++, obj,val);
    }
    
    private int compare(Object value1, Object value2) {
        if (value1 == value2) return 0;
        if (value1 == null) return ascending ? -1 : 1;
        if (value2 == null) return ascending ? 1 : -1;
        return ((Comparable)value1).compareTo(value2) * (ascending ? 1 : -1);
    }
    
    private class WrapperComparator implements Comparator<Wrapper<T>> {

        @Override
        public int compare(Wrapper<T> o1, Wrapper<T> o2) {
            if (o1 == o2) return 0;
            if (o1 == null) return  1;
            if (o2 == null) return -1;
            int comp = SortedObjectsBuffer.this.compare(o1.value, o2.value);
            if (comp == 0) {
                if (o1.serialId > o2.serialId) return 1;
                if (o1.serialId < o2.serialId) return -1;
            }
            return comp;
        }
    }

    private static class Wrapper<T> {
        private final T object;
        private final Object value;
        private final int serialId;

        private Wrapper(int id, T obj, Object val) {
            serialId = id;
            object = obj;
            value = val;
        }
    }
}
