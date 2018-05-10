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

package com.sun.tools.visualvm.heapviewer.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.swing.SortOrder;
import org.netbeans.lib.profiler.heap.Heap;
import com.sun.tools.visualvm.heapviewer.utils.NodesComputer;
import com.sun.tools.visualvm.heapviewer.utils.ProgressIterator;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "ContainerNode_MoreNodes=<another {0} nodes left>",
    "ContainerNode_SamplesContainer=<sample {0} nodes>",
    "ContainerNode_NodesContainer=<nodes {0}-{1}>"
})
public abstract class ContainerNode<T> extends HeapViewerNode {
    
    protected final int maxNodes;
    protected final List<T> items;
    
    protected final String name;
    
    protected int count = DataType.COUNT.getUnsupportedValue();
    protected long ownSize = DataType.OWN_SIZE.getUnsupportedValue();
    protected long retainedSize = DataType.RETAINED_SIZE.getUnsupportedValue();
    
    
    public ContainerNode(String name) {
        this(name, Integer.MAX_VALUE);
    }

    public ContainerNode(String name, int maxNodes) {
        this.name = name;
        this.maxNodes = maxNodes;
        
        items = new ArrayList();
    }

    public String getName() {
        return name;
    }

    public int getCount() {
        return count;
    }

    public long getOwnSize() {
        return ownSize;
    }

    public long getRetainedSize(Heap heap) {
        if (retainedSize == DataType.RETAINED_SIZE.getNotAvailableValue().longValue() &&
            DataType.RETAINED_SIZE.valuesAvailable(heap)) {
            retainedSize = 0;
            for (T item : items) retainedSize += getRetainedSize(item, heap);
        }
        return retainedSize;
    }
    
    
    protected abstract HeapViewerNode createNode(T item);
    
    
    protected int getCount(T item, Heap heap) {
        return DataType.COUNT.getUnsupportedValue();
    }
    
    protected long getOwnSize(T item, Heap heap) {
        return DataType.OWN_SIZE.getUnsupportedValue();
    }
    
    protected long getRetainedSize(T item, Heap heap) {
        return DataType.RETAINED_SIZE.getUnsupportedValue();
    }
    
    
    protected boolean sorts(DataType dataType) {
        return true;
    }
    
    protected String getMoreNodesString(String moreNodesCount)  {
        return Bundle.ContainerNode_MoreNodes(moreNodesCount);
    }
    
    protected String getSamplesContainerString(String objectsCount)  {
        return Bundle.ContainerNode_SamplesContainer(objectsCount);
    }
    
    protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
        return Bundle.ContainerNode_NodesContainer(firstNodeIdx, lastNodeIdx);
    }

    
    public String toString() {
        return name;
    }
    
    
    public void add(T item, Heap heap) {
        items.add(item);
        
        int _count = getCount(item, heap);
        if (_count >= 0) { // NOTE: assumes that any positive value is not a special value
            if (count < 0) count = 0;
            count += _count;
        }

        long _ownSize = getOwnSize(item, heap);
        if (_ownSize >= 0) { // NOTE: assumes that any positive value is not a special value
            if (ownSize < 0) ownSize = 0;
            ownSize += _ownSize;
        }
        
        if (retainedSize != DataType.RETAINED_SIZE.getNotAvailableValue().longValue()) {
            long _retainedSize = getRetainedSize(item, heap);
            if (_retainedSize >= 0) { // NOTE: assumes that any positive value is not a special value
                if (retainedSize < 0) retainedSize = 0;
                retainedSize += _retainedSize;
            } else if (_retainedSize == DataType.RETAINED_SIZE.getNotAvailableValue().longValue()) {
                retainedSize = _retainedSize;
            }
        }
    }
    
    public List<T> getItems() {
        return Collections.unmodifiableList(items);
    }
    
    
    protected HeapViewerNode[] computeChildren(RootNode root) {
        int itemsCount = items.size();
        if (itemsCount <= maxNodes) {
            HeapViewerNode[] nodes = new HeapViewerNode[itemsCount];
            for (int i = 0; i < itemsCount; i++) nodes[i] = createNode(items.get(i));
            return nodes;
        } else {
            return super.computeChildren(root);
        }
    }

    protected HeapViewerNode[] lazilyComputeChildren(Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) {
        NodesComputer<T> computer = new NodesComputer<T>(items.size(), maxNodes) {
            protected boolean sorts(DataType dataType) {
                return ContainerNode.this.sorts(dataType);
            }
            protected HeapViewerNode createNode(T object) {
                return ContainerNode.this.createNode(object);
            }
            protected ProgressIterator<T> objectsIterator(int index, Progress progress) {
                Iterator<T> iterator = items.listIterator(index);
                return new ProgressIterator(iterator, index, false, progress);
            }
            protected String getMoreNodesString(String moreNodesCount)  {
                return ContainerNode.this.getMoreNodesString(moreNodesCount);
            }
            protected String getSamplesContainerString(String objectsCount)  {
                return ContainerNode.this.getSamplesContainerString(objectsCount);
            }
            protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                return ContainerNode.this.getNodesContainerString(firstNodeIdx, lastNodeIdx);
            }
        };
        return computer.computeNodes(ContainerNode.this, heap, viewID, null, dataTypes, sortOrders, progress);
    }
    
    
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof ContainerNode)) return false;
        return name.equals(((ContainerNode)o).name);
    }
    
    public int hashCode() {
        return name.hashCode();
    }
    

    public boolean isLeaf() {
        return items.isEmpty();
    }
    
    
    protected Object getValue(DataType type, Heap heap) {
        if (type == DataType.NAME) return getName();
        if (type == DataType.COUNT) return getCount();
        if (type == DataType.OWN_SIZE) return getOwnSize();
        if (type == DataType.RETAINED_SIZE) return getRetainedSize(heap);
        
        return super.getValue(type, heap);
    }
    
    
    public static class Nodes<T extends HeapViewerNode> extends ContainerNode<T> {
        
        public Nodes(String name) {
            this(name, Integer.MAX_VALUE);
        }
        
        public Nodes(String name, int maxNodes) {
            super(name, maxNodes);
        }
        
        protected int getCount(T item, Heap heap) {
            return HeapViewerNode.getValue(item, DataType.COUNT, heap);
        }

        protected long getOwnSize(T item, Heap heap) {
            return HeapViewerNode.getValue(item, DataType.OWN_SIZE, heap);
        }

        protected long getRetainedSize(T item, Heap heap) {
            return HeapViewerNode.getValue(item, DataType.RETAINED_SIZE, heap);
        }
        
        protected HeapViewerNode[] computeChildren(RootNode root) {
            if (items.size() <= maxNodes) {
                return items.toArray(HeapViewerNode.NO_NODES);
            } else {
                return super.computeChildren(root);
            }
        }

        protected HeapViewerNode[] lazilyComputeChildren(Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) {
            NodesComputer<T> computer = new NodesComputer<T>(items.size(), maxNodes) {
                protected boolean sorts(DataType dataType) {
                    return ContainerNode.Nodes.this.sorts(dataType);
                }
                protected HeapViewerNode createNode(T object) {
                    return object;
                }
                protected ProgressIterator<T> objectsIterator(int index, Progress progress) {
                    Iterator<T> iterator = items.listIterator(index);
                    return new ProgressIterator(iterator, index, false, progress);
                }
                protected String getMoreNodesString(String moreNodesCount)  {
                    return ContainerNode.Nodes.this.getMoreNodesString(moreNodesCount);
                }
                protected String getSamplesContainerString(String objectsCount)  {
                    return ContainerNode.Nodes.this.getSamplesContainerString(objectsCount);
                }
                protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                    return ContainerNode.Nodes.this.getNodesContainerString(firstNodeIdx, lastNodeIdx);
                }
            };
            return computer.computeNodes(ContainerNode.Nodes.this, heap, viewID, null, dataTypes, sortOrders, progress);
        }

        protected HeapViewerNode createNode(HeapViewerNode item) {
            throw new UnsupportedOperationException("Not supported"); // NOI18N
        }
        
    }
    
}
