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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.SortOrder;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import com.sun.tools.visualvm.heapviewer.HeapContext;
import com.sun.tools.visualvm.heapviewer.java.JavaHeapFragment;
import com.sun.tools.visualvm.heapviewer.model.DataType;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNode;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNodeFilter;
import com.sun.tools.visualvm.heapviewer.model.Progress;
import com.sun.tools.visualvm.heapviewer.model.ProgressNode;
import com.sun.tools.visualvm.heapviewer.model.RootNode;
import com.sun.tools.visualvm.heapviewer.model.TextNode;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewPlugin;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerActions;
import com.sun.tools.visualvm.heapviewer.ui.TreeTableView;
import com.sun.tools.visualvm.heapviewer.ui.TreeTableViewColumn;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "JavaReferencesPlugin_Name=References",
    "JavaReferencesPlugin_Description=References",
    "JavaReferencesPlugin_NoReferences=<no references>",
    "JavaReferencesPlugin_NoSelection=<no instance selected>"
})
class JavaReferencesPlugin extends HeapViewPlugin {
    
    private final Heap heap;
    private Instance selected;
    
    private final TreeTableView objectsView;
    
    private final Set<Instance> computingInstances = Collections.synchronizedSet(new HashSet());
    

    public JavaReferencesPlugin(HeapContext context, HeapViewerActions actions) {
        super(Bundle.JavaReferencesPlugin_Name(), Bundle.JavaReferencesPlugin_Description(), Icons.getIcon(ProfilerIcons.NODE_REVERSE));
        
        heap = context.getFragment().getHeap();
        
        objectsView = new TreeTableView("java_objects_references", context, actions, TreeTableViewColumn.instancesMinimal(heap, false)) { // NOI18N
            protected HeapViewerNode[] computeData(RootNode root, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) {
                Instance _selected = null;
                synchronized (this) { _selected = selected; }
                
                if (_selected != null) {
                    if (computingInstances.contains(_selected)) {
                        return new HeapViewerNode[] { new ProgressNode() };
                    } else {
                        computingInstances.add(_selected);
                        try {
                            HeapViewerNode[] nodes = JavaReferencesProvider.getNodes(_selected, root, heap, viewID, null, dataTypes, sortOrders, progress);
                            return nodes == null || nodes.length == 0 ? new HeapViewerNode[] { new TextNode(Bundle.JavaReferencesPlugin_NoReferences()) } : nodes;
                        } finally {
                            computingInstances.remove(_selected);
                        }
                    }
                }
                
                return new HeapViewerNode[] { new TextNode(Bundle.JavaReferencesPlugin_NoSelection()) };
            }
        };
    }

    protected JComponent createComponent() {
        return objectsView.getComponent();
    }
    
    
    protected void nodeSelected(HeapViewerNode node, boolean adjusting) {
        Instance newSelected = node == null ? null : HeapViewerNode.getValue(node, DataType.INSTANCE, heap);
        if (Objects.equals(selected, newSelected)) return;
        
        synchronized (this) { selected = newSelected; }
        objectsView.reloadView();
    }
    
    
    @ServiceProvider(service=HeapViewPlugin.Provider.class, position = 300)
    public static class Provider extends HeapViewPlugin.Provider {

        public HeapViewPlugin createPlugin(HeapContext context, HeapViewerActions actions, String viewID) {
            if (JavaHeapFragment.isJavaHeap(context)) return new JavaReferencesPlugin(context, actions);
            return null;
        }
        
    }
    
}
