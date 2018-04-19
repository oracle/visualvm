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
package com.sun.tools.visualvm.heapviewer.truffle.javaext;

import com.sun.tools.visualvm.heapviewer.java.InstanceNode;
import com.sun.tools.visualvm.heapviewer.java.PrimitiveNode;
import com.sun.tools.visualvm.heapviewer.model.DataType;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNode;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNodeFilter;
import com.sun.tools.visualvm.heapviewer.model.Progress;
import com.sun.tools.visualvm.heapviewer.ui.UIThresholds;
import com.sun.tools.visualvm.heapviewer.utils.NodesComputer;
import com.sun.tools.visualvm.heapviewer.utils.ProgressIterator;
import java.util.Iterator;
import java.util.List;
import javax.swing.SortOrder;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.PrimitiveArrayInstance;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@ServiceProvider(service=HeapViewerNode.Provider.class, position = 300)
public class TrufflePrimitiveArrayItemsProvider extends HeapViewerNode.Provider {
    
    public String getName() {
        return "items";
    }
    
    public boolean supportsView(Heap heap, String viewID) {
        // TODO: fix to cover just the Truffle heaps!
        return !viewID.startsWith("java_");
    }
    
    public boolean supportsNode(HeapViewerNode parent, Heap heap, String viewID) {
        if (parent instanceof InstanceNode && !InstanceNode.Mode.INCOMING_REFERENCE.equals(((InstanceNode)parent).getMode())) {
            Instance instance = ((InstanceNode)parent).getInstance();
            return instance instanceof PrimitiveArrayInstance;
        } else {
            return false;
        }
    }
    
    public HeapViewerNode[] getNodes(final HeapViewerNode parent, final Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) {
        final Instance instance = HeapViewerNode.getValue(parent, DataType.INSTANCE, heap);
//        if (!(instance instanceof PrimitiveArrayInstance)) return null;
        
        final String type = instance.getJavaClass().getName().replace("[]", "");
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
                return new ProgressIterator(iterator, index, false, progress);
            }
            protected String getMoreNodesString(String moreNodesCount)  {
                return "<another " + moreNodesCount + " items left>";
            }
            protected String getSamplesContainerString(String objectsCount)  {
                return "<sample " + objectsCount + " items>";
            }
            protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                return "<items " + firstNodeIdx + "-" + lastNodeIdx + ">";
            }
        };

        return computer.computeNodes(parent, heap, viewID, null, dataTypes, sortOrders, progress);
    }
    
}
