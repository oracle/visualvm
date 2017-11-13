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

import java.util.List;
import javax.swing.JComponent;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.lib.profiler.heap.ArrayItemValue;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.HeapProgress;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.ObjectFieldValue;
import org.netbeans.lib.profiler.heap.Value;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import com.sun.tools.visualvm.heapviewer.HeapContext;
import com.sun.tools.visualvm.heapviewer.HeapFragment;
import com.sun.tools.visualvm.heapviewer.java.InstanceReferenceNode;
import com.sun.tools.visualvm.heapviewer.java.JavaHeapFragment;
import com.sun.tools.visualvm.heapviewer.model.DataType;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNode;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNodeFilter;
import com.sun.tools.visualvm.heapviewer.model.Progress;
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
    "PathToGCRootPlugin_ProgressMsg=Computing nearest GC root..."
})
public class PathToGCRootPlugin extends HeapViewPlugin {
    
    private final Heap heap;
    private Instance selected;
    
    private final TreeTableView objectsView;
    
    
    public PathToGCRootPlugin(HeapContext context, HeapViewerActions actions) {
        super("GC Root", "GC Root", Icons.getIcon(ProfilerIcons.RUN_GC));
        
        heap = context.getFragment().getHeap();
        
        objectsView = new TreeTableView("java_objects_gcroots", context, actions, TreeTableViewColumn.instancesPlain(heap)) {
            protected HeapViewerNode[] computeData(RootNode root, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) {
                if (selected != null) {
                    Instance instance = selected;
                    Instance nextInstance = getNearestGCRootPointer(instance);
                    
                    if (nextInstance == null) {
                        return new HeapViewerNode[] { new TextNode("<no GC root>") };
                    } else if (nextInstance == instance) {
                        return new HeapViewerNode[] { new TextNode("<node is GC root>") };
                    } else {
                        ToRoot node = null;
                        HeapViewerNode firstNode = null;
                        ToRoot previousNode = null;
                        
                        while (instance != nextInstance) {
                            List<Value> references = instance.getReferences();
                            for (Value reference : references) {
                                if (nextInstance.equals(reference.getDefiningInstance())) {
                                    if (reference instanceof ObjectFieldValue) node = new FieldToRoot((ObjectFieldValue)reference);
                                    else if (reference instanceof ArrayItemValue) node = new ArrayItemToRoot((ArrayItemValue)reference);
                                    
                                    if (firstNode == null) firstNode = (HeapViewerNode)node;
                                    else previousNode.setChildren(new HeapViewerNode[] { (HeapViewerNode)node });
                                    
                                    break;
                                }
                            }
                            
                            instance = nextInstance;
                            nextInstance = instance.getNearestGCRootPointer();
                            
                            previousNode = node;
                        }
                        
                        node.setChildren(HeapViewerNode.NO_NODES);
                        return new HeapViewerNode[] { firstNode };
                    }
                }

                return new HeapViewerNode[] { new TextNode("<no instance selected>") };
            }
            protected void childrenChanged() {
                HeapViewerNode root = (HeapViewerNode)getRoot();
                while (root != null) {
                    expandNode(root);
                    root = root.getNChildren() > 0 ? (HeapViewerNode)root.getChild(0) : null;
                }
            }
        };
    }

    protected JComponent createComponent() {
        return objectsView.getComponent();
    }
    
    
    protected void nodeSelected(HeapViewerNode node, boolean adjusting) {
        selected = node == null ? null : HeapViewerNode.getValue(node, DataType.INSTANCE, heap);
        
        objectsView.reloadView();
    }
    
    private static Instance getNearestGCRootPointer(Instance instance) {
        assert !SwingUtilities.isEventDispatchThread();

        ProgressHandle pHandle = null;

        try {
            pHandle = ProgressHandle.createHandle(Bundle.PathToGCRootPlugin_ProgressMsg());
            pHandle.setInitialDelay(1000);
            pHandle.start(HeapProgress.PROGRESS_MAX);

            HeapFragment.setProgress(pHandle, 0);
            return instance.getNearestGCRootPointer();
        } finally {
            if (pHandle != null) pHandle.finish();
        }
    }
    
    private static interface ToRoot {
        
        public void setChildren(HeapViewerNode[] ch);
        
    }
    
    private static class FieldToRoot extends InstanceReferenceNode.Field implements ToRoot {
        
        public FieldToRoot(ObjectFieldValue value) {
            super(value, true);
        }
        
        public void setChildren(HeapViewerNode[] ch) {
            super.setChildren(ch);
        }
        
    }
    
    private static class ArrayItemToRoot extends InstanceReferenceNode.ArrayItem implements ToRoot {
        
        public ArrayItemToRoot(ArrayItemValue value) {
            super(value, true);
        } 
        
        public void setChildren(HeapViewerNode[] ch) {
            super.setChildren(ch);
        }
        
    }
    
    
    @ServiceProvider(service=HeapViewPlugin.Provider.class, position = 400)
    public static class Provider extends HeapViewPlugin.Provider {

        public HeapViewPlugin createPlugin(HeapContext context, HeapViewerActions actions, String viewID) {
            if (JavaHeapFragment.isJavaHeap(context)) return new PathToGCRootPlugin(context, actions);
            return null;
        }
        
    }
    
}
