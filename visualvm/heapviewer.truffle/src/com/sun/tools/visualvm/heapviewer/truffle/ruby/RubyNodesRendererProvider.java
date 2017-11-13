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
package com.sun.tools.visualvm.heapviewer.truffle.ruby;

import com.sun.tools.visualvm.heapviewer.truffle.DynamicObjectArrayItemNode;
import com.sun.tools.visualvm.heapviewer.truffle.LocalDynamicObjectNode;
import com.sun.tools.visualvm.heapviewer.truffle.DynamicObjectReferenceNode;
import com.sun.tools.visualvm.heapviewer.truffle.DynamicObjectFieldNode;
import com.sun.tools.visualvm.heapviewer.truffle.DynamicObjectsContainer;
import com.sun.tools.visualvm.heapviewer.truffle.DynamicObjectNode;
import java.util.Map;
import javax.swing.Icon;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.modules.profiler.api.icons.LanguageIcons;
import com.sun.tools.visualvm.heapviewer.HeapContext;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNode;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerRenderer;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@ServiceProvider(service=HeapViewerRenderer.Provider.class)
public class RubyNodesRendererProvider extends HeapViewerRenderer.Provider {
    
    public boolean supportsView(HeapContext context, String viewID) {
        return true;
    }

    public void registerRenderers(Map<Class<? extends HeapViewerNode>, HeapViewerRenderer> renderers, HeapContext context) {
        Heap heap = context.getFragment().getHeap();
        Icon instanceIcon = RubySupport.createBadgedIcon(LanguageIcons.INSTANCE);
        Icon packageIcon = RubySupport.createBadgedIcon(LanguageIcons.PACKAGE);
        
        renderers.put(RubyNodes.RubyDynamicObjectNode.class, new DynamicObjectNode.Renderer(heap, instanceIcon));
        
        renderers.put(RubyNodes.RubyDynamicObjectsContainer.class, new DynamicObjectsContainer.Renderer(packageIcon));
        
        renderers.put(RubyNodes.RubyDynamicObjectFieldNode.class, new DynamicObjectFieldNode.Renderer(heap, instanceIcon));
        
        renderers.put(RubyNodes.RubyDynamicObjectArrayItemNode.class, new DynamicObjectArrayItemNode.Renderer(heap, instanceIcon));
        
        renderers.put(RubyNodes.RubyDynamicObjectReferenceNode.class, new DynamicObjectReferenceNode.Renderer(heap, instanceIcon));
        
        renderers.put(RubyNodes.RubyLocalDynamicObjectNode.class, new LocalDynamicObjectNode.Renderer(heap, instanceIcon));
    }
    
}
