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
package com.sun.tools.visualvm.truffle.heapwalker.javaext;

import com.sun.tools.visualvm.truffle.heapwalker.DynamicObject;
import com.sun.tools.visualvm.truffle.heapwalker.TruffleFrame;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.SortOrder;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.modules.profiler.heapwalker.v2.HeapContext;
import org.netbeans.modules.profiler.heapwalker.v2.java.JavaHeapFragment;
import org.netbeans.modules.profiler.heapwalker.v2.model.DataType;
import org.netbeans.modules.profiler.heapwalker.v2.model.HeapWalkerNode;
import org.netbeans.modules.profiler.heapwalker.v2.model.HeapWalkerNodeFilter;
import org.netbeans.modules.profiler.heapwalker.v2.model.RootNode;
import org.netbeans.modules.profiler.heapwalker.v2.ui.HeapViewPlugin;
import org.netbeans.modules.profiler.heapwalker.v2.ui.HeapWalkerActions;
import org.netbeans.modules.profiler.heapwalker.v2.ui.TreeTableView;
import org.netbeans.modules.profiler.heapwalker.v2.ui.TreeTableViewColumn;
import org.openide.util.ImageUtilities;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
class TruffleFieldsPlugin extends HeapViewPlugin {
    
    private final Heap heap;
    private Instance selected;
    
    private final TreeTableView objectsView;
    

    public TruffleFieldsPlugin(HeapContext context, HeapWalkerActions actions) {
        super("Truffle Fields", "Truffle Fields", graalIcon());
        
        heap = context.getFragment().getHeap();
        
        objectsView = new TreeTableView("truffle_objects_fields", context, actions, TreeTableViewColumn.instancesMinimal(heap, false)) {
            protected HeapWalkerNode[] computeData(RootNode root, Heap heap, String viewID, HeapWalkerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders) {
                List<FieldValue> fields = new ArrayList();
                
                if (DynamicObject.isDynamicObject(selected)) {
                    DynamicObject dobj = new DynamicObject(selected);
                    fields.addAll(dobj.getFieldValues());
                    fields.addAll(dobj.getStaticFieldValues());
                } else if (TruffleFrame.isTruffleFrame(selected)) {
                    TruffleFrame tframe = new TruffleFrame(selected);
                    fields.addAll(tframe.getFieldValues());
                }
                
                HeapWalkerNode[] nodes = TruffleFieldsProvider.getNodes(fields, root, heap, viewID, dataTypes, sortOrders);
                return nodes == null ? HeapWalkerNode.NO_NODES : nodes;
            }
        };
    }

    protected JComponent createComponent() {
        return objectsView.getComponent();
    }
    
    
    protected void nodeSelected(HeapWalkerNode node, boolean adjusting) {
        Instance instance = node == null ? null : HeapWalkerNode.getValue(node, DataType.INSTANCE, heap);
        
        if (Objects.equals(instance, selected)) return;
        selected = instance;
        
        objectsView.reloadView();
    }
    
    
    private static Icon graalIcon() {
        String path = DynamicObject.class.getPackage().getName().replace('.', '/') + "/GraalVM.png";
        return new ImageIcon(ImageUtilities.loadImage(path, true));
    }
    
    
    @ServiceProvider(service=HeapViewPlugin.Provider.class, position = 220)
    public static class Provider extends HeapViewPlugin.Provider {

        public HeapViewPlugin createPlugin(HeapContext context, HeapWalkerActions actions, String viewID) {
            if (JavaHeapFragment.isJavaHeap(context)) return new TruffleFieldsPlugin(context, actions);
            return null;
        }
        
    }
    
}
