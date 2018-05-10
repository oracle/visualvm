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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.JComponent;
import javax.swing.SortOrder;
import org.graalvm.visualvm.lib.jfluid.heap.FieldValue;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.ProfilerIcons;
import org.graalvm.visualvm.heapviewer.HeapContext;
import org.graalvm.visualvm.heapviewer.java.JavaHeapFragment;
import org.graalvm.visualvm.heapviewer.model.DataType;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNode;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNodeFilter;
import org.graalvm.visualvm.heapviewer.model.Progress;
import org.graalvm.visualvm.heapviewer.model.RootNode;
import org.graalvm.visualvm.heapviewer.model.TextNode;
import org.graalvm.visualvm.heapviewer.ui.HeapViewPlugin;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerActions;
import org.graalvm.visualvm.heapviewer.ui.TreeTableView;
import org.graalvm.visualvm.heapviewer.ui.TreeTableViewColumn;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "JavaFieldsPlugin_Name=Fields",
    "JavaFieldsPlugin_Description=Fields",
    "JavaFieldsPlugin_NoFields=<no fields>",
    "JavaFieldsPlugin_NoSelection=<no class or instance selected>"
})
class JavaFieldsPlugin extends HeapViewPlugin {
    
    private final Heap heap;
    private Object selected;
    
    private final TreeTableView objectsView;
    

    public JavaFieldsPlugin(HeapContext context, HeapViewerActions actions) {
        super(Bundle.JavaFieldsPlugin_Name(), Bundle.JavaFieldsPlugin_Description(), Icons.getIcon(ProfilerIcons.NODE_FORWARD));
        
        heap = context.getFragment().getHeap();
        
        objectsView = new TreeTableView("java_objects_fields", context, actions, TreeTableViewColumn.instancesMinimal(heap, false)) { // NOI18N
            protected HeapViewerNode[] computeData(RootNode root, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) {
                Object _selected;
                synchronized (objectsView) { _selected = selected; }
                
                if (_selected != null) {
                    List<FieldValue> fields = null;

                    if (_selected instanceof Instance) {
                        fields = new ArrayList(((Instance)_selected).getFieldValues());
                        fields.addAll(((Instance)_selected).getStaticFieldValues());
                    } else if (_selected instanceof JavaClass) {
                        fields = ((JavaClass)_selected).getStaticFieldValues();
                    }

                    HeapViewerNode[] nodes = JavaFieldsProvider.getNodes(fields, root, heap, viewID, viewFilter, dataTypes, sortOrders, progress);
                    return nodes == null || nodes.length == 0 ? new HeapViewerNode[] { new TextNode(Bundle.JavaFieldsPlugin_NoFields()) } : nodes;
                }
                
                return new HeapViewerNode[] { new TextNode(Bundle.JavaFieldsPlugin_NoSelection()) };
            }
        };
    }

    protected JComponent createComponent() {
        return objectsView.getComponent();
    }
    
    
    protected void nodeSelected(HeapViewerNode node, boolean adjusting) {
        Instance selectedInstance = node == null ? null : HeapViewerNode.getValue(node, DataType.INSTANCE, heap);
        
        synchronized (objectsView) {
            if (selectedInstance != null) {
                if (Objects.equals(selected, selectedInstance)) return;
                selected = selectedInstance;
            } else {
                JavaClass selectedClass = node == null ? null : HeapViewerNode.getValue(node, DataType.CLASS, heap);
                if (Objects.equals(selected, selectedClass)) return;
                selected = selectedClass;
            }
        }
        
        objectsView.reloadView();
    }
    
    
    @ServiceProvider(service=HeapViewPlugin.Provider.class, position = 200)
    public static class Provider extends HeapViewPlugin.Provider {

        public HeapViewPlugin createPlugin(HeapContext context, HeapViewerActions actions, String viewID) {
            if (!viewID.startsWith("diff") && JavaHeapFragment.isJavaHeap(context)) return new JavaFieldsPlugin(context, actions); // NOI18N
            return null;
        }
        
    }
    
}
