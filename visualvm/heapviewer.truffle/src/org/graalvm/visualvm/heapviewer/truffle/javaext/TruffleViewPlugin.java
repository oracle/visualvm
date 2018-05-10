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
package com.sun.tools.visualvm.heapviewer.truffle.javaext;

import com.sun.tools.visualvm.heapviewer.truffle.dynamicobject.DynamicObject;
import com.sun.tools.visualvm.heapviewer.truffle.dynamicobject.DynamicObjectNode;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleFrame;
import java.util.List;
import java.util.Objects;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.SortOrder;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import com.sun.tools.visualvm.heapviewer.HeapContext;
import com.sun.tools.visualvm.heapviewer.java.InstanceNode;
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
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "TruffleViewPlugin_Name=Truffle Object",
    "TruffleViewPlugin_Description=Truffle Object",
    "TruffleViewPlugin_NoSelection=<no DynamicObject or TruffleFrame selected>"
})
class TruffleViewPlugin extends HeapViewPlugin {
    
    private final Heap heap;
    private Instance selected;
    
    private final TreeTableView objectsView;
    

    public TruffleViewPlugin(HeapContext context, HeapViewerActions actions) {
        super(Bundle.TruffleViewPlugin_Name(), Bundle.TruffleViewPlugin_Description(), graalIcon());
        
        heap = context.getFragment().getHeap();
        
        objectsView = new TreeTableView("truffle_objects_javaext", context, actions, TreeTableViewColumn.instancesMinimal(heap, false)) { // NOI18N
            protected HeapViewerNode[] computeData(RootNode root, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) {
                if (DynamicObject.isDynamicObject(selected)) {
                    DynamicObject dobject = new DynamicObject(selected);
                    return new HeapViewerNode[] { new DynamicObjectNode(dobject, dobject.getType(heap)) };
                } else if (TruffleFrame.isTruffleFrame(selected)) {
                    return new HeapViewerNode[] { new InstanceNode(selected) };
                } else {
                    return new HeapViewerNode[] { new TextNode(Bundle.TruffleViewPlugin_NoSelection()) };
                }
            }
            protected void childrenChanged() {
                HeapViewerNode[] children = getRoot().getChildren();
                for (HeapViewerNode child : children) expandNode(child);
                
                if (children.length > 0) {
                    children = children[0].getChildren();
                    if (children.length > 0 && children[0] instanceof TextNode) expandNode(children[0]);
                }
            }
        };
    }

    protected JComponent createComponent() {
        return objectsView.getComponent();
    }
    
    
    protected void nodeSelected(HeapViewerNode node, boolean adjusting) {
        Instance instance = node == null ? null : HeapViewerNode.getValue(node, DataType.INSTANCE, heap);
        
        if (Objects.equals(instance, selected)) return;
        selected = instance;
        
        objectsView.reloadView();
    }
    
    
    private static Icon graalIcon() {
        String path = TruffleViewPlugin.class.getPackage().getName().replace('.', '/') + "/GraalVM.png"; // NOI18N
        return new ImageIcon(ImageUtilities.loadImage(path, true));
    }
    
    
    @ServiceProvider(service=HeapViewPlugin.Provider.class, position = 1000)
    public static class Provider extends HeapViewPlugin.Provider {

        public HeapViewPlugin createPlugin(HeapContext context, HeapViewerActions actions, String viewID) {
            if (!JavaHeapFragment.isJavaHeap(context)) return null;
            if (!DynamicObject.hasDynamicObject(context.getFragment().getHeap())) return null;
            return new TruffleViewPlugin(context, actions);
        }
        
    }
    
}
