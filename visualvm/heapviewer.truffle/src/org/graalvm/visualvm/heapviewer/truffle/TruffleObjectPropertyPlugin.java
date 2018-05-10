/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.heapviewer.truffle;

import java.util.List;
import java.util.Objects;
import javax.swing.JComponent;
import javax.swing.SortOrder;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.heapviewer.HeapContext;
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
import javax.swing.Icon;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "TruffleObjectPropertyPlugin_NoSelection=<no object selected>",
    "TruffleObjectPropertyPlugin_NoItems=<no {0}>" // <no items>
})
public class TruffleObjectPropertyPlugin<O extends TruffleObject, T extends TruffleType<O>, F extends TruffleLanguageHeapFragment<O, T>, L extends TruffleLanguage<O, T, F>> extends HeapViewPlugin {
    
    private final TruffleObjectPropertyProvider<O, T, F, L, ? extends Object> provider;
    
    private final Heap heap;
    
    private O selected;
    
    private final TreeTableView objectsView;
    

    public TruffleObjectPropertyPlugin(String name, String description, Icon icon, String viewID, HeapContext context, HeapViewerActions actions, TruffleObjectPropertyProvider<O, T, F, L, ? extends Object> provider) {
        super(name, description, icon);
        
        this.provider = provider;
        
        heap = context.getFragment().getHeap();
        
        objectsView = new TreeTableView(viewID, context, actions, TreeTableViewColumn.instancesMinimal(heap, false)) {
            protected HeapViewerNode[] computeData(RootNode root, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) {
                if (selected == null) return new HeapViewerNode[] { new TextNode(Bundle.TruffleObjectPropertyPlugin_NoSelection()) };
                    
                HeapViewerNode[] nodes = provider.getNodes(selected, root, heap, viewID, viewFilter, dataTypes, sortOrders, progress);
                return nodes == null || nodes.length == 0 ? new HeapViewerNode[] { new TextNode(Bundle.TruffleObjectPropertyPlugin_NoItems(provider.getName())) } : nodes;
            }
        };
    }

    protected JComponent createComponent() {
        return objectsView.getComponent();
    }
    
    
    protected void nodeSelected(HeapViewerNode node, boolean adjusting) {
        O selectedObject = provider.getObject(node, heap);
        if (Objects.equals(selected, selectedObject)) return;

        selected = selectedObject;
        
        objectsView.reloadView();
    }
    
}
