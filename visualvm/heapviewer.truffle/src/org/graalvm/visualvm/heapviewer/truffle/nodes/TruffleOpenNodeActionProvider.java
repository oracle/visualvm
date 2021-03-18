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
package org.graalvm.visualvm.heapviewer.truffle.nodes;

import org.graalvm.visualvm.heapviewer.HeapContext;
import org.graalvm.visualvm.heapviewer.model.DataType;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNode;
import org.graalvm.visualvm.heapviewer.truffle.TruffleLanguage;
import org.graalvm.visualvm.heapviewer.truffle.TruffleLanguageHeapFragment;
import org.graalvm.visualvm.heapviewer.truffle.TruffleObject;
import org.graalvm.visualvm.heapviewer.truffle.TruffleType;
import org.graalvm.visualvm.heapviewer.truffle.ui.TruffleObjectView;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerActions;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerNodeAction;
import org.graalvm.visualvm.heapviewer.ui.NodeObjectsView;
import java.util.ArrayList;
import java.util.List;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "TruffleOpenNodeActionProvider_OpenTypeTab=Open Type in New Tab"
})
public abstract class TruffleOpenNodeActionProvider<O extends TruffleObject, T extends TruffleType<O>, F extends TruffleLanguageHeapFragment<O, T>, L extends TruffleLanguage<O, T, F>> extends HeapViewerNodeAction.Provider {
    
    protected abstract boolean supportsNode(HeapViewerNode node);
    
    protected abstract L getLanguage();
    
    
    public HeapViewerNodeAction[] getActions(HeapViewerNode node, HeapContext context, HeapViewerActions actions) {
        if (!supportsNode(node)) return null;
        
        Heap heap = context.getFragment().getHeap();
        List<HeapViewerNodeAction> actionsList = new ArrayList(2);
        
        final L language = getLanguage();
        
        // Open in New Tab action
        actionsList.add(new NodeObjectsView.DefaultOpenAction(node.createCopy(), context, actions) {
            public NodeObjectsView createView(HeapViewerNode node, HeapContext context, HeapViewerActions actions) {
                return TruffleOpenNodeActionProvider.this.createView(language, node, context, actions);
            }
        });
        
        // Open Type in New Tab action
        Instance instance = HeapViewerNode.getValue(node, DataType.INSTANCE, heap);
        if (instance != null && language.isLanguageObject(instance)) {
            O object = language.createObject(instance);
            F fragment = language.fragmentFromHeap(heap);
            T type = fragment.getType(object.getType(), null);
            if (type != null) {
                HeapViewerNode typeNode = language.createTypeNode(type, heap);
                actionsList.add(new NodeObjectsView.OpenAction(Bundle.TruffleOpenNodeActionProvider_OpenTypeTab(), 1, typeNode, context, actions) {
                    public NodeObjectsView createView(HeapViewerNode node, HeapContext context, HeapViewerActions actions) {
                        return TruffleOpenNodeActionProvider.this.createView(language, node, context, actions);
                    }
                });
            }
        }
        
        return actionsList.toArray(new HeapViewerNodeAction[0]);
    }
    
    
    private NodeObjectsView createView(L language, HeapViewerNode node, HeapContext context, HeapViewerActions actions) {
        return new TruffleObjectView(language, node, context, actions);
    }
    
}
