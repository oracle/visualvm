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
package com.sun.tools.visualvm.truffle.heapwalker.javascript;

import org.netbeans.modules.profiler.heapwalker.v2.HeapContext;
import org.netbeans.modules.profiler.heapwalker.v2.model.HeapWalkerNode;
import org.netbeans.modules.profiler.heapwalker.v2.ui.HeapWalkerActions;
import org.netbeans.modules.profiler.heapwalker.v2.ui.HeapWalkerNodeAction;
import org.netbeans.modules.profiler.heapwalker.v2.ui.NodeObjectsView;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@ServiceProvider(service=HeapWalkerNodeAction.Provider.class)
public class JavaScriptOpenNodeAction extends HeapWalkerNodeAction.Provider {
    
    public boolean supportsView(HeapContext context, String viewID) {
        return JavaScriptHeapFragment.isJavaScriptHeap(context);
    }

    public HeapWalkerNodeAction[] getActions(HeapWalkerNode node, HeapContext context, HeapWalkerActions actions) {
        HeapWalkerNode copy = node instanceof JavaScriptNodes.JavaScriptNode ? node.createCopy() : null;
        return new HeapWalkerNodeAction[] { new OpenNodeAction(copy, context, actions) };
    }
    
    
    private static class OpenNodeAction extends NodeObjectsView.DefaultOpenAction {
        
        private OpenNodeAction(HeapWalkerNode node, HeapContext context, HeapWalkerActions actions) {
            super(node, context, actions);
        }

        public NodeObjectsView createView(HeapWalkerNode node, HeapContext context, HeapWalkerActions actions) {
            return new JavaScriptObjectView(node, context, actions);
        }
        
    }
    
}
