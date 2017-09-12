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

import com.sun.tools.visualvm.truffle.heapwalker.LocalDynamicObjectNode;
import com.sun.tools.visualvm.truffle.heapwalker.DynamicObjectReferenceNode;
import com.sun.tools.visualvm.truffle.heapwalker.DynamicObjectFieldNode;
import com.sun.tools.visualvm.truffle.heapwalker.DynamicObjectsContainer;
import com.sun.tools.visualvm.truffle.heapwalker.DynamicObjectNode;
import java.util.Map;
import javax.swing.Icon;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.modules.profiler.api.icons.LanguageIcons;
import org.netbeans.modules.profiler.heapwalker.v2.HeapContext;
import org.netbeans.modules.profiler.heapwalker.v2.model.HeapWalkerNode;
import com.sun.tools.visualvm.truffle.heapwalker.TruffleNodesRendererProvider;
import org.netbeans.modules.profiler.heapwalker.v2.ui.HeapWalkerRenderer;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@ServiceProvider(service=HeapWalkerRenderer.Provider.class)
public class JavaScriptNodesRendererProvider extends TruffleNodesRendererProvider {

    public void registerRenderers(Map<Class<? extends HeapWalkerNode>, HeapWalkerRenderer> renderers, HeapContext context) {
        Heap heap = context.getFragment().getHeap();
        Icon instanceIcon = JavaScriptSupport.createBadgedIcon(LanguageIcons.INSTANCE);
        Icon packageIcon = JavaScriptSupport.createBadgedIcon(LanguageIcons.PACKAGE);
        
        renderers.put(JavaScriptNodes.JavaScriptDynamicObjectNode.class, new DynamicObjectNode.Renderer(heap, instanceIcon));
        
        renderers.put(JavaScriptNodes.JavaScriptDynamicObjectsContainer.class, new DynamicObjectsContainer.Renderer(packageIcon));
        
        renderers.put(JavaScriptNodes.JavaScriptDynamicObjectFieldNode.class, new DynamicObjectFieldNode.Renderer(heap, instanceIcon));
        
        renderers.put(JavaScriptNodes.JavaScriptDynamicObjectReferenceNode.class, new DynamicObjectReferenceNode.Renderer(heap, instanceIcon));
        
        renderers.put(JavaScriptNodes.JavaScriptLocalDynamicObjectNode.class, new LocalDynamicObjectNode.Renderer(heap, instanceIcon));

        
        if (JavaScriptHeapFragment.isJavaScriptHeap(context)) super.registerRenderers(renderers, context);
    }
    
}
