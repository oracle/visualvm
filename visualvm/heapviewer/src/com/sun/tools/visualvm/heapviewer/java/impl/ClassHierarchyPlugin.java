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
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.heapwalk.ui.icons.HeapWalkerIcons;
import com.sun.tools.visualvm.heapviewer.HeapContext;
import com.sun.tools.visualvm.heapviewer.java.ClassNode;
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
    "ClassHierarchyPlugin_Name=Hierarchy",
    "ClassHierarchyPlugin_Description=Hierarchy",
    "ClassHierarchyPlugin_NoSelection=<no class or instance selected>"
})
public class ClassHierarchyPlugin extends HeapViewPlugin {
    
    private final Heap heap;
    private JavaClass selected;
    
    private final TreeTableView objectsView;
    
    
    public ClassHierarchyPlugin(HeapContext context, HeapViewerActions actions) {
        super(Bundle.ClassHierarchyPlugin_Name(), Bundle.ClassHierarchyPlugin_Description(), Icons.getIcon(HeapWalkerIcons.CLASSES));
        
        heap = context.getFragment().getHeap();
        
        objectsView = new TreeTableView("java_objects_hierarchy", context, actions, TreeTableViewColumn.classesPlain(heap)) { // NOI18N
            protected HeapViewerNode[] computeData(RootNode root, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) {
                if (selected != null) {
                    JavaClass javaClass = selected;
                    
                    if (javaClass.isArray()) {
                        String className = javaClass.getName().replace("[]", ""); // NOI18N
                        JavaClass plainClass = heap.getJavaClassByName(className);
                        if (plainClass != null) javaClass = plainClass;
                    }
                    
                    SuperClassNode node = null;
                    SuperClassNode firstNode = null;
                    SuperClassNode previousNode = null;

                    while (javaClass != null) {
                        node = new SuperClassNode(javaClass);
                        if (firstNode == null) firstNode = node;
                        else previousNode.setChildren(new HeapViewerNode[] { node });

                        javaClass = javaClass.getSuperClass();

                        previousNode = node;
                    }

                    node.setChildren(HeapViewerNode.NO_NODES);
                    return new HeapViewerNode[] { firstNode };
                }

                return new HeapViewerNode[] { new TextNode(Bundle.ClassHierarchyPlugin_NoSelection()) };
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
        selected = node == null ? null : HeapViewerNode.getValue(node, DataType.CLASS, heap);
        
        objectsView.reloadView();
    }
    
    
    private static class SuperClassNode extends ClassNode {
        
        SuperClassNode(JavaClass javaClass) {
            super(javaClass);
        }
        
        protected void setChildren(HeapViewerNode[] ch) {
            super.setChildren(ch);
        }
        
        public boolean isLeaf() {
            return getChildCount() == 0;
        }
        
    }
    
    
    @ServiceProvider(service=HeapViewPlugin.Provider.class, position = 500)
    public static class Provider extends HeapViewPlugin.Provider {

        public HeapViewPlugin createPlugin(HeapContext context, HeapViewerActions actions, String viewID) {
            if (!viewID.startsWith("diff") && JavaHeapFragment.isJavaHeap(context)) return new ClassHierarchyPlugin(context, actions); // NOI18N
            return null;
        }
        
    }
    
}
