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
package org.graalvm.visualvm.heapviewer.truffle.nodes;

import org.graalvm.visualvm.heapviewer.model.DataType;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNode;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNodeFilter;
import org.graalvm.visualvm.heapviewer.model.Progress;
import org.graalvm.visualvm.heapviewer.model.RootNode;
import org.graalvm.visualvm.heapviewer.truffle.TruffleObject;
import org.graalvm.visualvm.heapviewer.truffle.TruffleType;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerRenderer;
import org.graalvm.visualvm.heapviewer.ui.UIThresholds;
import org.graalvm.visualvm.heapviewer.utils.NodesComputer;
import org.graalvm.visualvm.heapviewer.utils.ProgressIterator;
import java.awt.Font;
import java.util.Iterator;
import java.util.List;
import javax.swing.Icon;
import javax.swing.SortOrder;
import org.graalvm.visualvm.heapviewer.truffle.TruffleObjectsWrapper;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.ui.swing.renderer.LabelRenderer;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "TruffleTypeNode_MoreNodes=<another {0} objects left>", // <another 1234 objects left>
    "TruffleTypeNode_SamplesContainer=<sample {0} objects>", // <sample 1234 objects>
    "TruffleTypeNode_NodesContainer=<objects {0}-{1}>" // <objects 1001 - 2000>
})
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
    
    
    public TruffleObjectsWrapper<O> getObjectsWrapper() {
        return new TruffleObjectsWrapper() {
            @Override
            public String getType() {
                return TruffleTypeNode.this.getName();
            }

            @Override
            public int getObjectsCount() {
                return TruffleTypeNode.this.getObjectsCount();
            }

            @Override
            public Iterator<O> getObjectsIterator() {
                return type.getObjectsIterator();
            }
        };
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
    
    protected HeapViewerNode[] lazilyComputeChildren(Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {
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
                return Bundle.TruffleTypeNode_MoreNodes(moreNodesCount);
            }
            protected String getSamplesContainerString(String objectsCount)  {
                return Bundle.TruffleTypeNode_SamplesContainer(objectsCount);
            }
            protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                return Bundle.TruffleTypeNode_NodesContainer(firstNodeIdx, lastNodeIdx);
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
        
        if (type == TruffleType.TYPE_NAME) return getName();
        if (type == TruffleObjectsWrapper.DATA_TYPE) return getObjectsWrapper();
        
        if (type == DataType.LOGICAL_VALUE) return DataType.LOGICAL_VALUE.getNoValue();
        if (type == DataType.OBJECT_ID) return DataType.OBJECT_ID.getNoValue();
        
        return super.getValue(type, heap);
    }
    
    
    protected void setupCopy(TruffleTypeNode copy) {
        super.setupCopy(copy);
    }
    
    
    public static class Renderer extends LabelRenderer implements HeapViewerRenderer {
        
        public Renderer(Icon icon) {
            setIcon(icon);
            setFont(getFont().deriveFont(Font.BOLD));
        }
        
    }
    
}
