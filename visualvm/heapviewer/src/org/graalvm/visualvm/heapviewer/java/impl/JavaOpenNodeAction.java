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

package org.graalvm.visualvm.heapviewer.java.impl;

import java.util.ArrayList;
import java.util.List;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.heapviewer.HeapContext;
import org.graalvm.visualvm.heapviewer.java.ClassNode;
import org.graalvm.visualvm.heapviewer.java.JavaHeapFragment;
import org.graalvm.visualvm.heapviewer.model.DataType;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNode;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerActions;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerNodeAction;
import org.graalvm.visualvm.heapviewer.ui.NodeObjectsView;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@ServiceProvider(service=HeapViewerNodeAction.Provider.class)
@NbBundle.Messages({
    "JavaOpenNodeAction_OpenClassTab=Open Class in New Tab"
})
public class JavaOpenNodeAction extends HeapViewerNodeAction.Provider {
    
    public boolean supportsView(HeapContext context, String viewID) {
        return (viewID.startsWith("java_") || viewID.startsWith("diff_java_")) && JavaHeapFragment.getJavaContext(context) != null; // NOI18N
    }

    public HeapViewerNodeAction[] getActions(HeapViewerNode node, HeapContext context, HeapViewerActions actions) {
        HeapContext javaContext = JavaHeapFragment.getJavaContext(context);
        
        List<HeapViewerNodeAction> actionsList = new ArrayList(2);
        
        HeapViewerNode nodeCopy = node.createCopy();
        actionsList.add(new OpenNodeAction(nodeCopy, javaContext, actions));
        
        Instance instance = HeapViewerNode.getValue(node, DataType.INSTANCE, javaContext.getFragment().getHeap());
        HeapViewerNode classNode = instance == null ? null : new ClassNode(instance.getJavaClass());
        if (classNode != null) actionsList.add(new OpenClassAction(classNode, javaContext, actions));
        
        return actionsList.toArray(new HeapViewerNodeAction[0]);
    }
    
    
    private static NodeObjectsView createView(HeapViewerNode node, HeapContext context, HeapViewerActions actions) {
        return new JavaObjectView(node, context, actions);
    }
    
    
    private static class OpenNodeAction extends NodeObjectsView.DefaultOpenAction {
        
        private OpenNodeAction(HeapViewerNode node, HeapContext context, HeapViewerActions actions) {
            super(node, context, actions);
        }

        public NodeObjectsView createView(HeapViewerNode node, HeapContext context, HeapViewerActions actions) {
            return JavaOpenNodeAction.createView(node, context, actions);
        }
        
    }
    
    private static class OpenClassAction extends NodeObjectsView.OpenAction {
        
        private OpenClassAction(HeapViewerNode node, HeapContext context, HeapViewerActions actions) {
            super(Bundle.JavaOpenNodeAction_OpenClassTab(), 1, node, context, actions);
        }

        public NodeObjectsView createView(HeapViewerNode node, HeapContext context, HeapViewerActions actions) {
            return JavaOpenNodeAction.createView(node, context, actions);
        }
        
    }
    
}
