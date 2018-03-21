/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.tools.visualvm.heapviewer.model.DataType;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNode;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNodeFilter;
import com.sun.tools.visualvm.heapviewer.model.Progress;
import com.sun.tools.visualvm.heapviewer.model.RootNode;
import com.sun.tools.visualvm.heapviewer.ui.UIThresholds;
import com.sun.tools.visualvm.heapviewer.utils.NodesComputer;
import com.sun.tools.visualvm.heapviewer.utils.ProgressIterator;
import java.util.Iterator;
import java.util.List;
import javax.swing.SortOrder;
import org.netbeans.lib.profiler.heap.Heap;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class TruffleTypeNode<O extends TruffleObject, T extends TruffleType<O>> extends HeapViewerNode {
    
    private final T type;
    
    
    protected TruffleTypeNode(T type) {
        this.type = type;
    }
    
    
    public abstract HeapViewerNode createNode(O object, Heap heap);
    
    public abstract TruffleTypeNode createCopy();
    
    
    public T getType() {
        return type;
    }
    
    
    public String getName() {
        return type.getName();
    }
    
    public int getObjectsCount() {
        return type.getObjectsCount();
    }
    
    public long getOwnSize() {
        return type.getAllObjectsSize();
    }
    
    public long getRetainedSize(Heap heap) {
        return type.getRetainedSizeByType(heap);
    }
    
    
    public boolean isLeaf() {
        return type.getObjectsCount() == 0 ? true : super.isLeaf();
    }
    
    public String toString() {
        return getName();
    }
    
    
    protected HeapViewerNode[] computeChildren(RootNode root) {
        int itemsCount = type.getObjectsCount();
        if (itemsCount <= UIThresholds.MAX_CLASS_INSTANCES) {
            Heap heap = root.getContext().getFragment().getHeap();
            HeapViewerNode[] nodes = new HeapViewerNode[itemsCount];
            Iterator<O> iterator = type.getObjectsIterator();
            int i = 0;
            while (iterator.hasNext()) {
                O object = iterator.next();
                nodes[i++] = createNode(object, heap);
            }
            return nodes;
        } else {
            return super.computeChildren(root);
        }
    }
    
    protected HeapViewerNode[] lazilyComputeChildren(Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) {
        NodesComputer<O> computer = new NodesComputer<O>(type.getObjectsCount(), UIThresholds.MAX_CLASS_INSTANCES) {
            protected boolean sorts(DataType dataType) {
                return !DataType.COUNT.equals(dataType);
            }
            protected HeapViewerNode createNode(O object) {
                return TruffleTypeNode.this.createNode(object, heap);
            }
            protected ProgressIterator<O> objectsIterator(int index, Progress progress) {
                Iterator<O> iterator = type.getObjectsIterator();
                return new ProgressIterator(iterator, index, true, progress);
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
        return computer.computeNodes(this, heap, viewID, null, dataTypes, sortOrders, progress);
    }
    
    
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof TruffleTypeNode)) return false;
        return getName().equals(((TruffleTypeNode)o).getName());
    }
    
    public int hashCode() {
        return getName().hashCode();
    }
    
    
    protected Object getValue(DataType type, Heap heap) {
        if (type == DataType.NAME) return getName();
        if (type == DataType.COUNT) return getObjectsCount();
        if (type == DataType.OWN_SIZE) return getOwnSize();
        if (type == DataType.RETAINED_SIZE) return getRetainedSize(heap);
        
        if (type == DataType.LOGICAL_VALUE) return DataType.LOGICAL_VALUE.getNoValue();
        if (type == DataType.OBJECT_ID) return DataType.OBJECT_ID.getNoValue();
        
        return super.getValue(type, heap);
    }
    
    
    protected void setupCopy(TruffleTypeNode copy) {
        super.setupCopy(copy);
    }
    
}
