/*
 * Copyright (c) 2017, 2022, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.heapviewer.java.impl;

import java.util.Iterator;
import java.util.List;
import javax.swing.SortOrder;
import org.graalvm.visualvm.heapviewer.java.InstanceNode;
import org.graalvm.visualvm.heapviewer.java.InstanceReferenceNode;
import org.graalvm.visualvm.heapviewer.java.PrimitiveNode;
import org.graalvm.visualvm.heapviewer.model.DataType;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNode;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNodeFilter;
import org.graalvm.visualvm.heapviewer.model.Progress;
import org.graalvm.visualvm.heapviewer.ui.UIThresholds;
import org.graalvm.visualvm.heapviewer.utils.NodesComputer;
import org.graalvm.visualvm.heapviewer.utils.ProgressIterator;
import org.graalvm.visualvm.lib.jfluid.heap.ArrayItemValue;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.ObjectArrayInstance;
import org.graalvm.visualvm.lib.jfluid.heap.PrimitiveArrayInstance;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@ServiceProvider(service=HeapViewerNode.Provider.class, position = 300)
@NbBundle.Messages({
    "JavaArrayItemsProvider_Name=items",
    "JavaArrayItemsProvider_MoreNodes=<another {0} items left>",
    "JavaArrayItemsProvider_SamplesContainer=<sample {0} items>",
    "JavaArrayItemsProvider_NodesContainer=<items {0}-{1}>"
})
public class JavaArrayItemsProvider extends HeapViewerNode.Provider {
    
    public String getName() {
        return Bundle.JavaArrayItemsProvider_Name();
    }
    
    public boolean supportsView(Heap heap, String viewID) {
        return viewID.startsWith("java_"); // NOI18N
    }
    
    public boolean supportsNode(HeapViewerNode parent, Heap heap, String viewID) {
        if (parent instanceof InstanceNode && !InstanceNode.Mode.INCOMING_REFERENCE.equals(((InstanceNode)parent).getMode())) {
            Instance instance = ((InstanceNode)parent).getInstance();
            return instance != null && instance.getJavaClass().isArray();
        } else {
            return false;
        }
    }
    
    public HeapViewerNode[] getNodes(final HeapViewerNode parent, final Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {
        final Instance instance = HeapViewerNode.getValue(parent, DataType.INSTANCE, heap);
        if (instance == null) return null;
        
        if (instance instanceof PrimitiveArrayInstance) {
            final String type = instance.getJavaClass().getName().replace("[]", ""); // NOI18N
            final List<String> items = ((PrimitiveArrayInstance)instance).getValues();
            
            NodesComputer<Integer> computer = new NodesComputer<Integer>(items.size(), UIThresholds.MAX_ARRAY_ITEMS) {
                protected boolean sorts(DataType dataType) {
                    if (DataType.COUNT == dataType || DataType.OWN_SIZE == dataType || DataType.RETAINED_SIZE == dataType) return false;
                    return true;
                }
                protected HeapViewerNode createNode(Integer index) {
                    return new PrimitiveNode.ArrayItem(index, type, items.get(index), instance);
                }
                protected ProgressIterator<Integer> objectsIterator(int index, Progress progress) {
                    Iterator<Integer> iterator = integerIterator(index, items.size());
                    return new ProgressIterator<>(iterator, index, false, progress);
                }
                protected String getMoreNodesString(String moreNodesCount)  {
                    return Bundle.JavaArrayItemsProvider_MoreNodes(moreNodesCount);
                }
                protected String getSamplesContainerString(String objectsCount)  {
                    return Bundle.JavaArrayItemsProvider_SamplesContainer(objectsCount);
                }
                protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                    return Bundle.JavaArrayItemsProvider_NodesContainer(firstNodeIdx, lastNodeIdx);
                }
            };
        
            return computer.computeNodes(parent, heap, viewID, null, dataTypes, sortOrders, progress);
        } else if (instance instanceof ObjectArrayInstance) {
            final List<ArrayItemValue> items = ((ObjectArrayInstance)instance).getItems();
            
            NodesComputer<Integer> computer = new NodesComputer<Integer>(items.size(), UIThresholds.MAX_ARRAY_ITEMS) {
                protected boolean sorts(DataType dataType) {
                    return !DataType.COUNT.equals(dataType);
                }
                protected HeapViewerNode createNode(Integer index) {
                    return new InstanceReferenceNode.ArrayItem(items.get(index), false);
                }
                protected ProgressIterator<Integer> objectsIterator(int index, Progress progress) {
                    Iterator<Integer> iterator = integerIterator(index, items.size());
                    return new ProgressIterator<>(iterator, index, false, progress);
                }
                protected String getMoreNodesString(String moreNodesCount)  {
                    return Bundle.JavaArrayItemsProvider_MoreNodes(moreNodesCount);
                }
                protected String getSamplesContainerString(String objectsCount)  {
                    return Bundle.JavaArrayItemsProvider_SamplesContainer(objectsCount);
                }
                protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                    return Bundle.JavaArrayItemsProvider_NodesContainer(firstNodeIdx, lastNodeIdx);
                }
            };
            
            return computer.computeNodes(parent, heap, viewID, null, dataTypes, sortOrders, progress);
        }
        
        return null;
    }
    
}
