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
package com.sun.tools.visualvm.truffle.heapwalker;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.swing.JComponent;
import javax.swing.SortOrder;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.modules.profiler.api.icons.GeneralIcons;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.heapwalker.v2.HeapContext;
import org.netbeans.modules.profiler.heapwalker.v2.HeapFragment;
import org.netbeans.modules.profiler.heapwalker.v2.java.InstanceNode;
import org.netbeans.modules.profiler.heapwalker.v2.java.InstanceNodeRenderer;
import org.netbeans.modules.profiler.heapwalker.v2.model.DataType;
import org.netbeans.modules.profiler.heapwalker.v2.model.HeapWalkerNode;
import org.netbeans.modules.profiler.heapwalker.v2.model.HeapWalkerNodeFilter;
import org.netbeans.modules.profiler.heapwalker.v2.model.RootNode;
import org.netbeans.modules.profiler.heapwalker.v2.model.TextNode;
import org.netbeans.modules.profiler.heapwalker.v2.ui.HeapViewPlugin;
import org.netbeans.modules.profiler.heapwalker.v2.ui.HeapWalkerActions;
import org.netbeans.modules.profiler.heapwalker.v2.ui.HeapWalkerRenderer;
import org.netbeans.modules.profiler.heapwalker.v2.ui.TreeTableView;
import org.netbeans.modules.profiler.heapwalker.v2.ui.TreeTableViewColumn;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
class JavaViewPlugin extends HeapViewPlugin {
    
    private final Heap heap;
    private Instance selected;
    
    private final TreeTableView objectsView;
    
    
    public JavaViewPlugin(HeapContext context, HeapWalkerActions actions) {
        super("Java Object", "Java Object", Icons.getIcon(GeneralIcons.JAVA_PROCESS));
        
        heap = context.getFragment().getHeap();
        
        objectsView = new TreeTableView("java_objects_truffleext", context, actions, TreeTableViewColumn.instances(heap, false)) {
            protected HeapWalkerNode[] computeData(RootNode root, Heap heap, String viewID, HeapWalkerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders) {
                InstanceNode instanceNode = selected == null ? null : new InstanceNodeWrapper(selected);
                HeapWalkerNode result = instanceNode == null ? new TextNode("<no instance selected>") : instanceNode;
                return new HeapWalkerNode[] { result };
            }
            protected void childrenChanged() {
                HeapWalkerNode[] children = getRoot().getChildren();
                for (HeapWalkerNode child : children) expandNode(child);
            }
        };
    }

    
    protected JComponent createComponent() {
        return objectsView.getComponent();
    }
    
    
    protected void nodeSelected(HeapWalkerNode node, boolean adjusting) {
        Instance selectedInstance = node == null ? null : HeapWalkerNode.getValue(node, DataType.INSTANCE, heap);
        if (Objects.equals(selected, selectedInstance)) return;

        selected = selectedInstance;
        
        objectsView.reloadView();
    }
    
    
    @ServiceProvider(service=HeapViewPlugin.Provider.class, position = 1000)
    public static class Provider extends HeapViewPlugin.Provider {

        public HeapViewPlugin createPlugin(HeapContext context, HeapWalkerActions actions, String viewID) {
            HeapFragment fragment = context.getFragment();
            if (fragment instanceof TruffleLanguageHeapFragment && !fragment.getID().startsWith("r_")) return new JavaViewPlugin(context, actions);
            return null;
        }
        
    }
    
    
    private static class InstanceNodeWrapper extends InstanceNode {
        
        InstanceNodeWrapper(Instance instance) {
            super(instance);
        }
        
    }
    
    
    @ServiceProvider(service=HeapWalkerRenderer.Provider.class)
    public static class InstanceNodeWrapperRendererProvider extends HeapWalkerRenderer.Provider {

        public void registerRenderers(Map<Class<? extends HeapWalkerNode>, HeapWalkerRenderer> renderers, HeapContext context) {
            renderers.put(InstanceNodeWrapper.class, new InstanceNodeRenderer( context.getFragment().getHeap()));
        }

    }
    
}
