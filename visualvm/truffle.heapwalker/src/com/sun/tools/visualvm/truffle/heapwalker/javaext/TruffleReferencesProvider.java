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
package com.sun.tools.visualvm.truffle.heapwalker.javaext;

import com.sun.tools.visualvm.truffle.heapwalker.DynamicObjectFieldNode;
import com.sun.tools.visualvm.truffle.heapwalker.DynamicObject;
import com.sun.tools.visualvm.truffle.heapwalker.DynamicObjectReferenceNode;
import com.sun.tools.visualvm.truffle.heapwalker.TerminalJavaNodes;
import java.util.Iterator;
import java.util.List;
import javax.swing.SortOrder;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.modules.profiler.heapwalker.v2.java.InstanceNode;
import org.netbeans.modules.profiler.heapwalker.v2.model.DataType;
import org.netbeans.modules.profiler.heapwalker.v2.model.HeapWalkerNode;
import org.netbeans.modules.profiler.heapwalker.v2.model.HeapWalkerNodeFilter;
import org.netbeans.modules.profiler.heapwalker.v2.model.Progress;
import org.netbeans.modules.profiler.heapwalker.v2.ui.UIThresholds;
import org.netbeans.modules.profiler.heapwalker.v2.utils.NodesComputer;
import static org.netbeans.modules.profiler.heapwalker.v2.utils.NodesComputer.integerIterator;
import org.netbeans.modules.profiler.heapwalker.v2.utils.ProgressIterator;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@ServiceProvider(service=HeapWalkerNode.Provider.class, position = 200)
public class TruffleReferencesProvider extends HeapWalkerNode.Provider {
    
    public String getName() {
        return "truffle references";
    }
    
    public boolean supportsView(Heap heap, String viewID) {
        return viewID.equals("truffle_objects_javaext");
    }
    
    public boolean supportsNode(HeapWalkerNode parent, Heap heap, String viewID) {
        if (parent instanceof InstanceNode && !(parent instanceof DynamicObjectFieldNode)) {
            InstanceNode node = (InstanceNode)parent;
            if (InstanceNode.Mode.OUTGOING_REFERENCE.equals(node.getMode())) return false;
            
            Instance instance = node.getInstance();
            return DynamicObject.isDynamicObject(instance);
        } else {
            return false;
        }
    }
    
    public HeapWalkerNode[] getNodes(HeapWalkerNode parent, Heap heap, String viewID, HeapWalkerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) {
        return getNodes(getReferences(parent, heap), parent, heap, viewID, dataTypes, sortOrders, progress);
    }
    
    static HeapWalkerNode[] getNodes(List<FieldValue> references, HeapWalkerNode parent, Heap heap, String viewID, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) {
        if (references == null) return null;
        
        NodesComputer<Integer> computer = new NodesComputer<Integer>(references.size(), UIThresholds.MAX_INSTANCE_REFERENCES) {
            protected boolean sorts(DataType dataType) {
                return !DataType.COUNT.equals(dataType);
            }
            protected HeapWalkerNode createNode(Integer index) {
                return TruffleReferencesProvider.createNode(references.get(index), heap);
            }
            protected ProgressIterator<Integer> objectsIterator(int index, Progress progress) {
                Iterator<Integer> iterator = integerIterator(index, references.size());
                return new ProgressIterator(iterator, index, false, progress);
            }
            protected String getMoreNodesString(String moreNodesCount)  {
                return "<another " + moreNodesCount + " references left>";
            }
            protected String getSamplesContainerString(String objectsCount)  {
                return "<sample " + objectsCount + " references>";
            }
            protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                return "<references " + firstNodeIdx + "-" + lastNodeIdx + ">";
            }
        };

        return computer.computeNodes(parent, heap, viewID, null, dataTypes, sortOrders, progress);
    }
    
    private static HeapWalkerNode createNode(FieldValue field, Heap heap) {
        Instance instance = field.getDefiningInstance();
        if (DynamicObject.isDynamicObject(instance)) {
            DynamicObject dobject = new DynamicObject(instance);
            return new DynamicObjectReferenceNode(dobject, dobject.getType(heap), field);
        } else {
            return TerminalJavaNodes.incomingReference(field);
        }
    }
    
    protected List<FieldValue> getReferences(HeapWalkerNode parent, Heap heap) {
        Instance instance = HeapWalkerNode.getValue(parent, DataType.INSTANCE, heap);

        if (DynamicObject.isDynamicObject(instance)) {
            DynamicObject dobject = new DynamicObject(instance);
            return dobject.getReferences();
        }

        return null;
    }
    
}
