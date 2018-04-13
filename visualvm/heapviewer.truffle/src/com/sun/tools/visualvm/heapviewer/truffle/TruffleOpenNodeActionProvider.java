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
package com.sun.tools.visualvm.heapviewer.truffle;

import com.sun.tools.visualvm.heapviewer.HeapContext;
import com.sun.tools.visualvm.heapviewer.model.DataType;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNode;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerActions;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerNodeAction;
import com.sun.tools.visualvm.heapviewer.ui.NodeObjectsView;
import java.util.ArrayList;
import java.util.List;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "TruffleOpenNodeActionProvider_OpenTypeTab=Open Type in New Tab"
})
public abstract class TruffleOpenNodeActionProvider<O extends TruffleObject, T extends TruffleType<O>> extends HeapViewerNodeAction.Provider {
    
    protected abstract boolean supportsNode(HeapViewerNode node);
    
    protected abstract boolean isLanguageObject(Instance instance);
    
    protected abstract O createObject(Instance instance);
    
    protected abstract T getType(String name, HeapContext context);
    
    protected abstract TruffleTypeNode<O, T> createTypeNode(T type);
    
    protected abstract NodeObjectsView createView(HeapViewerNode node, HeapContext context, HeapViewerActions actions);
    
    
    public HeapViewerNodeAction[] getActions(HeapViewerNode node, HeapContext context, HeapViewerActions actions) {
        if (!supportsNode(node)) return null;
        
        Heap heap = context.getFragment().getHeap();
        List<HeapViewerNodeAction> actionsList = new ArrayList(2);
        
        // Open in New Tab action
        actionsList.add(new OpenNodeAction(node.createCopy(), context, actions));
        
        // Open Type in New Tab action
        Instance instance = HeapViewerNode.getValue(node, DataType.INSTANCE, heap);
        if (instance != null && isLanguageObject(instance)) {
            O object = createObject(instance);
            T type = getType(object.getType(heap), context);
            if (type != null) {
                HeapViewerNode typeNode = createTypeNode(type);
                actionsList.add(new OpenClassAction(typeNode, context, actions));
            }
        }
        
        return actionsList.toArray(new HeapViewerNodeAction[0]);
    }
    
    
    private class OpenNodeAction extends NodeObjectsView.DefaultOpenAction {
        
        private OpenNodeAction(HeapViewerNode node, HeapContext context, HeapViewerActions actions) {
            super(node, context, actions);
        }

        public NodeObjectsView createView(HeapViewerNode node, HeapContext context, HeapViewerActions actions) {
            return TruffleOpenNodeActionProvider.this.createView(node, context, actions);
        }
        
    }
    
    private class OpenClassAction extends NodeObjectsView.OpenAction {
        
        private OpenClassAction(HeapViewerNode node, HeapContext context, HeapViewerActions actions) {
            super(Bundle.TruffleOpenNodeActionProvider_OpenTypeTab(), 1, node, context, actions);
        }

        public NodeObjectsView createView(HeapViewerNode node, HeapContext context, HeapViewerActions actions) {
            return TruffleOpenNodeActionProvider.this.createView(node, context, actions);
        }
        
    }
    
}
