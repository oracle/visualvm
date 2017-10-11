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
package com.sun.tools.visualvm.truffle.heapwalker.r;

import com.sun.tools.visualvm.truffle.heapwalker.TruffleFrame;
import java.util.List;
import java.util.Objects;
import javax.swing.JComponent;
import javax.swing.SortOrder;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import org.netbeans.modules.profiler.heapwalker.v2.HeapContext;
import org.netbeans.modules.profiler.heapwalker.v2.model.DataType;
import org.netbeans.modules.profiler.heapwalker.v2.model.HeapWalkerNode;
import org.netbeans.modules.profiler.heapwalker.v2.model.HeapWalkerNodeFilter;
import org.netbeans.modules.profiler.heapwalker.v2.model.RootNode;
import org.netbeans.modules.profiler.heapwalker.v2.ui.HeapViewPlugin;
import org.netbeans.modules.profiler.heapwalker.v2.ui.HeapWalkerActions;
import org.netbeans.modules.profiler.heapwalker.v2.ui.TreeTableView;
import org.netbeans.modules.profiler.heapwalker.v2.ui.TreeTableViewColumn;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tomas Hurka
 */
class RItemsPlugin extends HeapViewPlugin {

    private final Heap heap;
    private RObject selected;

    private final TreeTableView objectsView;

    RItemsPlugin(HeapContext context, HeapWalkerActions actions) {
        super("Items", "Items", Icons.getIcon(ProfilerIcons.NODE_FORWARD));

        heap = context.getFragment().getHeap();

        objectsView = new TreeTableView("r_attributes_fields", context, actions, TreeTableViewColumn.instancesMinimal(heap, false)) {
            protected HeapWalkerNode[] computeData(RootNode root, Heap heap, String viewID, HeapWalkerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders) {
                List<FieldValue> fields = null;

                if (selected != null) {
                    fields = selected.getFieldValues();
                    if (fields.isEmpty()) {
                        TruffleFrame frame = selected.getFrame();
                        if (frame != null) {
                            fields = frame.getLocalFieldValues();
                        }
                    }
                }

                HeapWalkerNode[] nodes = getNodes(fields, root, heap, viewID, viewFilter, dataTypes, sortOrders);
                return nodes == null ? HeapWalkerNode.NO_NODES : nodes;
            }
        };
    }

    protected JComponent createComponent() {
        return objectsView.getComponent();
    }

    protected HeapWalkerNode[] getNodes(List<FieldValue> fields, HeapWalkerNode parent, Heap heap, String viewID, HeapWalkerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders) {
        return RItemsProvider.getNodes(fields, parent, heap, viewID, viewFilter, dataTypes, sortOrders);
    }

    protected void nodeSelected(HeapWalkerNode node, boolean adjusting) {
        RObject selectedObject = node == null ? null : HeapWalkerNode.getValue(node, RObject.DATA_TYPE, heap);
        if (Objects.equals(selected, selectedObject)) {
            return;
        }

        selected = selectedObject;

        objectsView.reloadView();
    }

    @ServiceProvider(service = HeapViewPlugin.Provider.class, position = 500)
    public static class Provider extends HeapViewPlugin.Provider {

        public HeapViewPlugin createPlugin(HeapContext context, HeapWalkerActions actions, String viewID) {
            if (RHeapFragment.isRHeap(context)) {
                return new RItemsPlugin(context, actions);
            }
            return null;
        }
    }
}
