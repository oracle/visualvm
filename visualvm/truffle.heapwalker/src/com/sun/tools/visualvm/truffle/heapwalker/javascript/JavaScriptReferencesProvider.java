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
package com.sun.tools.visualvm.truffle.heapwalker.javascript;

import com.sun.tools.visualvm.truffle.heapwalker.DynamicObjectFieldNode;
import com.sun.tools.visualvm.truffle.heapwalker.DynamicObjectNode;
import com.sun.tools.visualvm.truffle.heapwalker.DynamicObject;
import java.util.Iterator;
import java.util.List;
import javax.swing.SortOrder;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.modules.profiler.heapwalker.v2.model.DataType;
import org.netbeans.modules.profiler.heapwalker.v2.model.HeapWalkerNode;
import org.netbeans.modules.profiler.heapwalker.v2.model.HeapWalkerNodeFilter;
import org.netbeans.modules.profiler.heapwalker.v2.ui.UIThresholds;
import org.netbeans.modules.profiler.heapwalker.v2.utils.NodesComputer;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@ServiceProvider(service=HeapWalkerNode.Provider.class, position = 400)
public class JavaScriptReferencesProvider extends HeapWalkerNode.Provider {
    
    public String getName() {
        return "references";
    }
    
    public boolean supportsView(Heap heap, String viewID) {
        return viewID.startsWith("javascript_");
    }
    
    public boolean supportsNode(HeapWalkerNode parent, Heap heap, String viewID) {
        return parent instanceof DynamicObjectNode && !(parent instanceof DynamicObjectFieldNode);
//        return parent instanceof DynamicObjectNode /*&& !(parent instanceof DynamicObjectFieldNode)*/ || parent instanceof ReferenceNode;
    }
    
    public HeapWalkerNode[] getNodes(HeapWalkerNode parent, Heap heap, String viewID, HeapWalkerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders) {
        return getNodes(getReferences(parent, heap), parent, heap, viewID, dataTypes, sortOrders);
    }
    
    static HeapWalkerNode[] getNodes(List<FieldValue> references, HeapWalkerNode parent, Heap heap, String viewID, List<DataType> dataTypes, List<SortOrder> sortOrders) {
        if (references == null) return null;
        
        NodesComputer<Integer> computer = new NodesComputer<Integer>(references.size(), UIThresholds.MAX_INSTANCE_REFERENCES) {
            protected boolean sorts(DataType dataType) {
                return !DataType.COUNT.equals(dataType);
            }
            protected HeapWalkerNode createNode(Integer index) {
                FieldValue reference = references.get(index);
                DynamicObject dobject = new DynamicObject(reference.getDefiningInstance());
                return new JavaScriptNodes.JavaScriptDynamicObjectReferenceNode(dobject, dobject.getType(heap), reference);
            }
            protected Iterator<Integer> objectsIterator(int index) {
                return integerIterator(index, references.size());
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

        return computer.computeNodes(parent, heap, viewID, null, dataTypes, sortOrders);
    }
    
    private List<FieldValue> getReferences(HeapWalkerNode parent, Heap heap) {
        DynamicObject dobject = parent == null ? null : HeapWalkerNode.getValue(parent, DynamicObject.DATA_TYPE, heap);
        if (dobject == null) return null;

        return dobject.getReferences();
    }
    
}
