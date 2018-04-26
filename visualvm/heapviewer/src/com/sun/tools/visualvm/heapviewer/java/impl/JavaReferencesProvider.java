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

package com.sun.tools.visualvm.heapviewer.java.impl;

import java.util.Iterator;
import java.util.List;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.HeapProgress;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.Value;
import com.sun.tools.visualvm.heapviewer.HeapFragment;
import com.sun.tools.visualvm.heapviewer.model.DataType;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNode;
import com.sun.tools.visualvm.heapviewer.java.InstanceNode;
import com.sun.tools.visualvm.heapviewer.java.InstanceReferenceNode;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNodeFilter;
import com.sun.tools.visualvm.heapviewer.model.Progress;
import com.sun.tools.visualvm.heapviewer.ui.UIThresholds;
import com.sun.tools.visualvm.heapviewer.utils.NodesComputer;
import com.sun.tools.visualvm.heapviewer.utils.ProgressIterator;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "JavaReferencesProvider_Name=references",
    "JavaReferencesProvider_References=Computing references...",
    "JavaReferencesProvider_MoreNodes=<another {0} references left>",
    "JavaReferencesProvider_SamplesContainer=<sample {0} references>",
    "JavaReferencesProvider_NodesContainer=<references {0}-{1}>"
})
@ServiceProviders(value={
    @ServiceProvider(service=HeapViewerNode.Provider.class, position = 400),
    @ServiceProvider(service=JavaReferencesProvider.class, position = 400)}
)
public class JavaReferencesProvider extends HeapViewerNode.Provider {
    
    private boolean referencesInitialized;
    
    
    public String getName() {
        return Bundle.JavaReferencesProvider_Name();
    }
    
    public boolean supportsView(Heap heap, String viewID) {
        return viewID.startsWith("java_"); // NOI18N
    }
    
    public boolean supportsNode(HeapViewerNode parent, Heap heap, String viewID) {
        return parent instanceof InstanceNode && !InstanceNode.Mode.OUTGOING_REFERENCE.equals(((InstanceNode)parent).getMode());
    }
    
    public HeapViewerNode[] getNodes(HeapViewerNode parent, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) {
        return getNodes(((InstanceNode)parent).getInstance(), parent, heap, viewID, viewFilter, dataTypes, sortOrders, progress);
    }
    
    HeapViewerNode[] getNodes(Instance instance, final HeapViewerNode parent, final Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) {
        if (instance == null) return null;
        
        List<Value> references = null;
        
        synchronized (this) {
            if (!referencesInitialized) {
                references = initializeReferences(instance);
                referencesInitialized = true;
            }
        }
        
        if (references == null) references = instance.getReferences();
        final List<Value> referencesF = references;
        
        NodesComputer<Value> computer = new NodesComputer<Value>(referencesF.size(), UIThresholds.MAX_INSTANCE_REFERENCES) {
            protected boolean sorts(DataType dataType) {
                return !DataType.COUNT.equals(dataType);
            }
            protected HeapViewerNode createNode(Value reference) {
                return InstanceReferenceNode.incoming(reference);
            }
            protected ProgressIterator<Value> objectsIterator(int index, Progress progress) {
                Iterator<Value> iterator = referencesF.listIterator(index);
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
    
    private static List<Value> initializeReferences(Instance instance) {
        assert !SwingUtilities.isEventDispatchThread();

        ProgressHandle pHandle = null;

        try {
            pHandle = ProgressHandle.createHandle(Bundle.JavaReferencesProvider_References());
            pHandle.setInitialDelay(1000);
            pHandle.start(HeapProgress.PROGRESS_MAX);

            HeapFragment.setProgress(pHandle, 0);
            return instance.getReferences();
        } finally {
            if (pHandle != null) pHandle.finish();
        }
    }
    
}
