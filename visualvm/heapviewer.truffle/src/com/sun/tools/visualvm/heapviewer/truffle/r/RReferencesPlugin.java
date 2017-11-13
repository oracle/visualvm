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
package com.sun.tools.visualvm.heapviewer.truffle.r;

import java.util.List;
import java.util.Objects;
import javax.swing.JComponent;
import javax.swing.SortOrder;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Heap;
import com.sun.tools.visualvm.heapviewer.HeapContext;
import com.sun.tools.visualvm.heapviewer.model.DataType;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNode;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNodeFilter;
import com.sun.tools.visualvm.heapviewer.model.Progress;
import com.sun.tools.visualvm.heapviewer.model.RootNode;
import com.sun.tools.visualvm.heapviewer.model.TextNode;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewPlugin;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerActions;
import com.sun.tools.visualvm.heapviewer.ui.TreeTableView;
import com.sun.tools.visualvm.heapviewer.ui.TreeTableViewColumn;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
class RReferencesPlugin extends HeapViewPlugin {
    
    private final Heap heap;
    private RObject selected;
    
    private final TreeTableView objectsView;
    
    
    public RReferencesPlugin(HeapContext context, HeapViewerActions actions) {
        super("References", "References", Icons.getIcon(ProfilerIcons.NODE_REVERSE));
        
        heap = context.getFragment().getHeap();
        
        objectsView = new TreeTableView("r_objects_references", context, actions, TreeTableViewColumn.instancesMinimal(heap, false)) {
            protected HeapViewerNode[] computeData(RootNode root, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) {
                if (selected != null) {
                    List<FieldValue> references = selected.getReferences();                
                    HeapViewerNode[] nodes = getNodes(references, root, heap, viewID, dataTypes, sortOrders, progress);
                    return nodes == null || nodes.length == 0 ? new HeapViewerNode[] { new TextNode("<no references>") } : nodes;
                }
                
                return new HeapViewerNode[] { new TextNode("<no object selected>") };
            }
        };
    }
    
    protected JComponent createComponent() {
        return objectsView.getComponent();
    }
    
    
    protected HeapViewerNode[] getNodes(List<FieldValue> fields, HeapViewerNode parent, Heap heap, String viewID, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) {
        return RReferencesProvider.getNodes(fields, parent, heap, viewID, dataTypes, sortOrders, progress);
    }
    
    
    protected void nodeSelected(HeapViewerNode node, boolean adjusting) {
        RObject selectedObject = node == null ? null : HeapViewerNode.getValue(node, RObject.DATA_TYPE, heap);
        if (Objects.equals(selected, selectedObject)) return;

        selected = selectedObject;
        
        objectsView.reloadView();
    }
    
    
    @ServiceProvider(service=HeapViewPlugin.Provider.class, position = 300)
    public static class Provider extends HeapViewPlugin.Provider {

        public HeapViewPlugin createPlugin(HeapContext context, HeapViewerActions actions, String viewID) {
            if (RHeapFragment.isRHeap(context)) return new RReferencesPlugin(context, actions);
            return null;
        }
        
    }
    
}
