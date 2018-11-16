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
import org.graalvm.visualvm.lib.jfluid.heap.Value;
import org.graalvm.visualvm.heapviewer.model.DataType;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNode;
import org.graalvm.visualvm.heapviewer.java.InstanceNode;
import org.graalvm.visualvm.heapviewer.java.InstanceReferenceNode;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNodeFilter;
import org.graalvm.visualvm.heapviewer.model.Progress;
import org.graalvm.visualvm.heapviewer.ui.UIThresholds;
import org.graalvm.visualvm.heapviewer.utils.HeapOperations;
import org.graalvm.visualvm.heapviewer.utils.NodesComputer;
import org.graalvm.visualvm.heapviewer.utils.ProgressIterator;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "JavaReferencesProvider_Name=references",
    "JavaReferencesProvider_MoreNodes=<another {0} references left>",
    "JavaReferencesProvider_SamplesContainer=<sample {0} references>",
    "JavaReferencesProvider_NodesContainer=<references {0}-{1}>"
})
@ServiceProviders(value={
    @ServiceProvider(service=HeapViewerNode.Provider.class, position = 400),
    @ServiceProvider(service=JavaReferencesProvider.class, position = 400)}
)
public class JavaReferencesProvider extends HeapViewerNode.Provider {
    
    public String getName() {
        return Bundle.JavaReferencesProvider_Name();
    }
    
    public boolean supportsView(Heap heap, String viewID) {
        return viewID.startsWith("java_"); // NOI18N
    }
    
    public boolean supportsNode(HeapViewerNode parent, Heap heap, String viewID) {
        return parent instanceof InstanceNode && !InstanceNode.Mode.OUTGOING_REFERENCE.equals(((InstanceNode)parent).getMode());
    }
    
    public HeapViewerNode[] getNodes(HeapViewerNode parent, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {
        return getNodes(((InstanceNode)parent).getInstance(), parent, heap, viewID, viewFilter, dataTypes, sortOrders, progress);
    }
    
    HeapViewerNode[] getNodes(Instance instance, final HeapViewerNode parent, final Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {
        if (instance == null) return null;
        
        HeapOperations.initializeReferences(heap);
        
        final List<Value> references = instance.getReferences();
        
        NodesComputer<Value> computer = new NodesComputer<Value>(references.size(), UIThresholds.MAX_INSTANCE_REFERENCES) {
            protected boolean sorts(DataType dataType) {
                return !DataType.COUNT.equals(dataType);
            }
            protected HeapViewerNode createNode(Value reference) {
                return InstanceReferenceNode.incoming(reference);
            }
            protected ProgressIterator<Value> objectsIterator(int index, Progress progress) {
                Iterator<Value> iterator = references.listIterator(index);
                return new ProgressIterator(iterator, index, false, progress);
            }
            protected String getMoreNodesString(String moreNodesCount)  {
                return Bundle.JavaReferencesProvider_MoreNodes(moreNodesCount);
            }
            protected String getSamplesContainerString(String objectsCount)  {
                return Bundle.JavaReferencesProvider_SamplesContainer(objectsCount);
            }
            protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                return Bundle.JavaReferencesProvider_NodesContainer(firstNodeIdx, lastNodeIdx);
            }
        };
        
        return computer.computeNodes(parent, heap, viewID, null, dataTypes, sortOrders, progress);
    }
    
}
