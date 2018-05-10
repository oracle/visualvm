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
package org.graalvm.visualvm.heapviewer.truffle.javaext;

import org.graalvm.visualvm.heapviewer.truffle.dynamicobject.DynamicObjectFieldNode;
import org.graalvm.visualvm.heapviewer.truffle.dynamicobject.DynamicObject;
import org.graalvm.visualvm.heapviewer.truffle.dynamicobject.DynamicObjectReferenceNode;
import org.graalvm.visualvm.heapviewer.truffle.nodes.TerminalJavaNodes;
import java.util.Iterator;
import java.util.List;
import javax.swing.SortOrder;
import org.graalvm.visualvm.lib.jfluid.heap.FieldValue;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.heapviewer.java.InstanceNode;
import org.graalvm.visualvm.heapviewer.model.DataType;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNode;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNodeFilter;
import org.graalvm.visualvm.heapviewer.model.Progress;
import org.graalvm.visualvm.heapviewer.ui.UIThresholds;
import org.graalvm.visualvm.heapviewer.utils.NodesComputer;
import org.graalvm.visualvm.heapviewer.utils.ProgressIterator;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "TruffleReferencesProvider_Name=truffle references",
    "TruffleReferencesProvider_MoreNodes=<another {0} truffle references left>",
    "TruffleReferencesProvider_SamplesContainer=<sample {0} truffle references>",
    "TTruffleReferencesProvider_NodesContainer=<truffle references {0}-{1}>"
})
@ServiceProvider(service=HeapViewerNode.Provider.class, position = 200)
public class TruffleReferencesProvider extends HeapViewerNode.Provider {
    
    public String getName() {
        return Bundle.TruffleReferencesProvider_Name();
    }
    
    public boolean supportsView(Heap heap, String viewID) {
        return viewID.equals("truffle_objects_javaext"); // NOI18N
    }
    
    public boolean supportsNode(HeapViewerNode parent, Heap heap, String viewID) {
        if (parent instanceof InstanceNode && !(parent instanceof DynamicObjectFieldNode)) {
            InstanceNode node = (InstanceNode)parent;
            if (InstanceNode.Mode.OUTGOING_REFERENCE.equals(node.getMode())) return false;
            
            Instance instance = node.getInstance();
            return DynamicObject.isDynamicObject(instance);
        } else {
            return false;
        }
    }
    
    public HeapViewerNode[] getNodes(HeapViewerNode parent, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) {
        return getNodes(getReferences(parent, heap), parent, heap, viewID, dataTypes, sortOrders, progress);
    }
    
    static HeapViewerNode[] getNodes(List<FieldValue> references, HeapViewerNode parent, Heap heap, String viewID, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) {
        if (references == null) return null;
        
        NodesComputer<Integer> computer = new NodesComputer<Integer>(references.size(), UIThresholds.MAX_INSTANCE_REFERENCES) {
            protected boolean sorts(DataType dataType) {
                return !DataType.COUNT.equals(dataType);
            }
            protected HeapViewerNode createNode(Integer index) {
                return TruffleReferencesProvider.createNode(references.get(index), heap);
            }
            protected ProgressIterator<Integer> objectsIterator(int index, Progress progress) {
                Iterator<Integer> iterator = integerIterator(index, references.size());
                return new ProgressIterator(iterator, index, false, progress);
            }
            protected String getMoreNodesString(String moreNodesCount)  {
                return Bundle.TruffleReferencesProvider_MoreNodes(moreNodesCount);
            }
            protected String getSamplesContainerString(String objectsCount)  {
                return Bundle.TruffleReferencesProvider_SamplesContainer(objectsCount);
            }
            protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                return Bundle.TTruffleReferencesProvider_NodesContainer(firstNodeIdx, lastNodeIdx);
            }
        };

        return computer.computeNodes(parent, heap, viewID, null, dataTypes, sortOrders, progress);
    }
    
    private static HeapViewerNode createNode(FieldValue field, Heap heap) {
        Instance instance = field.getDefiningInstance();
        if (DynamicObject.isDynamicObject(instance)) {
            DynamicObject dobject = new DynamicObject(instance);
            return new DynamicObjectReferenceNode(dobject, dobject.getType(heap), field);
        } else {
            return TerminalJavaNodes.incomingReference(field);
        }
    }
    
    protected List<FieldValue> getReferences(HeapViewerNode parent, Heap heap) {
        Instance instance = HeapViewerNode.getValue(parent, DataType.INSTANCE, heap);

        if (DynamicObject.isDynamicObject(instance)) {
            DynamicObject dobject = new DynamicObject(instance);
            return dobject.getReferences();
        }

        return null;
    }
    
}
