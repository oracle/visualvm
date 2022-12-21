/*
 * Copyright (c) 2017, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import javax.swing.JComponent;
import javax.swing.SortOrder;
import org.graalvm.visualvm.heapviewer.HeapContext;
import org.graalvm.visualvm.heapviewer.java.ClassNode;
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
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.heapwalk.ui.icons.HeapWalkerIcons;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "ClassHierarchyPlugin_Name=Hierarchy",
    "ClassHierarchyPlugin_Description=Hierarchy",
    "ClassHierarchyPlugin_NoSelection=<no class or instance selected>",
    "ClassHierarchyPlugin_NoInformation=<no superclass information>"
})
public class ClassHierarchyPlugin extends HeapViewPlugin {
    
    private static final JavaClass NO_CLASS = new FakeClass();
    private static final JavaClass EMPTY_CLASS = new FakeClass();
    
    private final Heap heap;
    private JavaClass selected;
    
    private final TreeTableView objectsView;
    
    
    public ClassHierarchyPlugin(HeapContext context, HeapViewerActions actions) {
        super(Bundle.ClassHierarchyPlugin_Name(), Bundle.ClassHierarchyPlugin_Description(), Icons.getIcon(HeapWalkerIcons.CLASSES));
        
        heap = context.getFragment().getHeap();
        
        objectsView = new TreeTableView("java_objects_hierarchy", context, actions, TreeTableViewColumn.classesPlain(heap)) { // NOI18N
            protected HeapViewerNode[] computeData(RootNode root, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {
                JavaClass javaClass;
                synchronized (objectsView) { javaClass = selected; }
                
                if (javaClass != null) {
                    
                    if (javaClass == EMPTY_CLASS) return new HeapViewerNode[] { new TextNode(Bundle.ClassHierarchyPlugin_NoInformation()) };
                    
                    if (javaClass.isArray()) {
                        String className = javaClass.getName().replace("[]", ""); // NOI18N
                        JavaClass plainClass = heap.getJavaClassByName(className);
                        if (plainClass != null) javaClass = plainClass;
                    }
                    
                    SuperClassNode node = null;
                    SuperClassNode firstNode = null;
                    SuperClassNode previousNode = null;

                    Thread worker = Thread.currentThread();
                    while (javaClass != null) {
                        node = new SuperClassNode(javaClass);
                        if (firstNode == null) firstNode = node;
                        else previousNode.setChildren(new HeapViewerNode[] { node });

                        javaClass = javaClass.getSuperClass();

                        previousNode = node;
                        
                        if (worker.isInterrupted()) throw new InterruptedException();
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
    
    
    @Override
    protected void closed() {
        synchronized (objectsView) { selected = NO_CLASS; }
        objectsView.closed();
    }
    
    
    protected void nodeSelected(HeapViewerNode node, boolean adjusting) {
        JavaClass sel = node == null ? null : HeapViewerNode.getValue(node, DataType.CLASS, heap);
        
        // Do not handle artificial classes without superclass (Diff view)
        if (sel != null && sel.getSuperClass() == null && !"java.lang.Object".equals(sel.getName())) // NOI18N
            sel = EMPTY_CLASS;
        
        synchronized (objectsView) {
            if (Objects.equals(selected, sel)) return;
            else selected = sel;
        }
        
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
    
    
    private static class FakeClass implements JavaClass {
        @Override public Object getValueOfStaticField(String name)  { throw new UnsupportedOperationException("Not supported."); } // NOI18N
        @Override public long getAllInstancesSize()                 { throw new UnsupportedOperationException("Not supported."); } // NOI18N
        @Override public boolean isArray()                          { throw new UnsupportedOperationException("Not supported."); } // NOI18N
        @Override public Instance getClassLoader()                  { throw new UnsupportedOperationException("Not supported."); } // NOI18N
        @Override public List getFields()                           { throw new UnsupportedOperationException("Not supported."); } // NOI18N
        @Override public int getInstanceSize()                      { throw new UnsupportedOperationException("Not supported."); } // NOI18N
        @Override public List getInstances()                        { throw new UnsupportedOperationException("Not supported."); } // NOI18N
        @Override public Iterator getInstancesIterator()            { throw new UnsupportedOperationException("Not supported."); } // NOI18N
        @Override public int getInstancesCount()                    { throw new UnsupportedOperationException("Not supported."); } // NOI18N
        @Override public long getRetainedSizeByClass()              { throw new UnsupportedOperationException("Not supported."); } // NOI18N
        @Override public long getJavaClassId()                      { throw new UnsupportedOperationException("Not supported."); } // NOI18N
        @Override public String getName()                           { throw new UnsupportedOperationException("Not supported."); } // NOI18N
        @Override public List getStaticFieldValues()                { throw new UnsupportedOperationException("Not supported."); } // NOI18N
        @Override public Collection getSubClasses()                 { throw new UnsupportedOperationException("Not supported."); } // NOI18N
        @Override public JavaClass getSuperClass()                  { throw new UnsupportedOperationException("Not supported."); } // NOI18N
        @Override public Heap getHeap()                             { throw new UnsupportedOperationException("Not supported."); } // NOI18N
        
        @Override public boolean equals(Object o)                   { return o == this; }
        @Override public int hashCode()                             { return -1; }
    }
    
    
    @ServiceProvider(service=HeapViewPlugin.Provider.class, position = 500)
    public static class Provider extends HeapViewPlugin.Provider {

        public HeapViewPlugin createPlugin(HeapContext context, HeapViewerActions actions, String viewID) {
            if (JavaHeapFragment.isJavaHeap(context)) return new ClassHierarchyPlugin(context, actions); // NOI18N
            return null;
        }
        
    }
    
}
