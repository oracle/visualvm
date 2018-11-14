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

package org.graalvm.visualvm.heapviewer.java.impl;

import java.util.Iterator;
import java.util.List;
import javax.swing.SortOrder;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.heapviewer.java.ClassNode;
import org.graalvm.visualvm.heapviewer.model.DataType;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNode;
import org.graalvm.visualvm.heapviewer.java.InstanceNode;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNodeFilter;
import org.graalvm.visualvm.heapviewer.model.Progress;
import org.graalvm.visualvm.heapviewer.model.TextNode;
import org.graalvm.visualvm.heapviewer.ui.UIThresholds;
import org.graalvm.visualvm.heapviewer.utils.NodesComputer;
import org.graalvm.visualvm.heapviewer.utils.ProgressIterator;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@ServiceProvider(service=HeapViewerNode.Provider.class, position = 100)
@NbBundle.Messages({
    "JavaInstancesProvider_Name=instances",
    "JavaInstancesProvider_MoreNodes=<another {0} instances left>",
    "JavaInstancesProvider_SamplesContainer=<sample {0} instances>",
    "JavaInstancesProvider_NodesContainer=<instances {0}-{1}>",
    "JavaInstancesProvider_NoInstances=<no instances>",
    "JavaInstancesProvider_NoInstancesFilter=<no instances matching the filter>"
})
public class JavaInstancesProvider extends HeapViewerNode.Provider {

    public String getName() {
        return Bundle.JavaInstancesProvider_Name();
    }
    
    public boolean supportsView(Heap heap, String viewID) {
        return !viewID.startsWith("diff"); // NOI18N
    }
    
    public boolean supportsNode(HeapViewerNode parent, Heap heap, String viewID) {
        return parent instanceof ClassNode;
    }

    public HeapViewerNode[] getNodes(final HeapViewerNode parent, final Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {
        JavaClass jclass = HeapViewerNode.getValue(parent, DataType.CLASS, heap);
        if (jclass == null) return null;
        
        NodesComputer<Instance> computer = new NodesComputer<Instance>(jclass.getInstancesCount(), UIThresholds.MAX_CLASS_INSTANCES) {
            protected boolean sorts(DataType dataType) {
                if (DataType.COUNT.equals(dataType) || (DataType.OWN_SIZE.equals(dataType) && !jclass.isArray())) return false;
                return true;
            }
            protected HeapViewerNode createNode(Instance instance) {
                return new InstanceNode(instance);
            }
            protected ProgressIterator<Instance> objectsIterator(int index, Progress progress) {
                Iterator iterator = jclass.getInstancesIterator();
                return new ProgressIterator(iterator, index, true, progress);
            }
            protected String getMoreNodesString(String moreNodesCount)  {
                return Bundle.JavaInstancesProvider_MoreNodes(moreNodesCount);
            }
            protected String getSamplesContainerString(String objectsCount)  {
                return Bundle.JavaInstancesProvider_SamplesContainer(objectsCount);
            }
            protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                return Bundle.JavaInstancesProvider_NodesContainer(firstNodeIdx, lastNodeIdx);
            }
        };
        
        return computer.computeNodes(parent, heap, viewID, null, dataTypes, sortOrders, progress);
    }
    
    
    public static HeapViewerNode[] getHeapInstances(final HeapViewerNode parent, final Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {
        // TODO: might be faster to process just instances of the classes matching viewFilter, if defined
        
        long totalInstancesL = heap.getSummary().getTotalLiveInstances();
        int totalInstancesI = totalInstancesL < 0 || totalInstancesL > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)totalInstancesL;
        
//        int totalInstancesI = Integer.MAX_VALUE;
//        System.err.println(">>> Computing " + heap.getSummary().getTotalLiveInstances() + " instances");
        
        NodesComputer<Instance> computer = new NodesComputer<Instance>(totalInstancesI, UIThresholds.MAX_TOPLEVEL_INSTANCES) {
            protected boolean sorts(DataType dataType) {
                return !DataType.COUNT.equals(dataType);
            }
            protected HeapViewerNode createNode(Instance instance) {
                return new InstanceNode(instance);
            }
            protected ProgressIterator<Instance> objectsIterator(int index, Progress progress) {
                Iterator iterator = heap.getAllInstancesIterator();
                return new ProgressIterator(iterator, index, true, progress);
            }
            protected String getMoreNodesString(String moreNodesCount)  {
                return Bundle.JavaInstancesProvider_MoreNodes(moreNodesCount);
            }
            protected String getSamplesContainerString(String objectsCount)  {
                return Bundle.JavaInstancesProvider_SamplesContainer(objectsCount);
            }
            protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                return Bundle.JavaInstancesProvider_NodesContainer(firstNodeIdx, lastNodeIdx);
            }
        };
        
        HeapViewerNode[] nodes = computer.computeNodes(parent, heap, viewID, viewFilter, dataTypes, sortOrders, progress);
        return nodes.length == 0 ? new HeapViewerNode[] { new TextNode(getNoInstancesString(viewFilter)) } : nodes;
    }
    
    private static String getNoInstancesString(HeapViewerNodeFilter viewFilter) {
        return viewFilter == null ? Bundle.JavaInstancesProvider_NoInstances() :
                                    Bundle.JavaInstancesProvider_NoInstancesFilter();
    }
    
}
