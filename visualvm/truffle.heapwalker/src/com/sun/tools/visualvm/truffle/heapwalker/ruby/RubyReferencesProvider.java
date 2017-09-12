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
package com.sun.tools.visualvm.truffle.heapwalker.ruby;

import com.sun.tools.visualvm.truffle.heapwalker.DynamicObjectFieldNode;
import com.sun.tools.visualvm.truffle.heapwalker.DynamicObjectNode;
import com.sun.tools.visualvm.truffle.heapwalker.DynamicObject;
import java.util.List;
import javax.swing.SortOrder;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.modules.profiler.heapwalker.v2.model.DataType;
import org.netbeans.modules.profiler.heapwalker.v2.model.HeapWalkerNode;
import org.netbeans.modules.profiler.heapwalker.v2.model.HeapWalkerNodeFilter;
import org.netbeans.modules.profiler.heapwalker.v2.model.SortedNodesBuffer;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@ServiceProvider(service=HeapWalkerNode.Provider.class, position = 400)
public class RubyReferencesProvider extends HeapWalkerNode.Provider {
    
    public String getName() {
        return "references";
    }
    
    public boolean supportsView(Heap heap, String viewID) {
        return viewID.startsWith("ruby_");
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
        
        DataType dataType = dataTypes == null || dataTypes.isEmpty() ? null : dataTypes.get(0);
        SortOrder sortOrder = sortOrders == null || sortOrders.isEmpty() ? null : sortOrders.get(0);
        SortedNodesBuffer nodes = new SortedNodesBuffer(100, dataType, sortOrder, heap, parent) {
            protected String getMoreItemsString(String formattedNodesLeft) {
                return "<another " + formattedNodesLeft + " references left>";
            }
        };
        
        for (FieldValue reference : references) {
            DynamicObject rdobject = new DynamicObject(reference.getDefiningInstance());
            nodes.add(new RubyNodes.RubyDynamicObjectReferenceNode(rdobject, reference, heap));
        }

        return nodes.getNodes();
    }
    
    protected List<FieldValue> getReferences(HeapWalkerNode parent, Heap heap) {
        DynamicObject dobject = parent == null ? null : HeapWalkerNode.getValue(parent, DynamicObject.DATA_TYPE, heap);
        if (dobject == null) return null;

        return dobject.getReferences();
    }
    
}
