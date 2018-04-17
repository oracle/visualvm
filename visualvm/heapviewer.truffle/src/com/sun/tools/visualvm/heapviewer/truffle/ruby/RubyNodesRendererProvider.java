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

import java.util.Map;
import javax.swing.Icon;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.modules.profiler.api.icons.LanguageIcons;
import com.sun.tools.visualvm.heapviewer.HeapContext;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNode;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleLocalObjectNode;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleObjectArrayItemNode;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleObjectFieldNode;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleObjectNode;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleObjectReferenceNode;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleTypeNode;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerRenderer;
import org.netbeans.modules.profiler.api.icons.Icons;
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
        Icon instanceIcon = RubySupport.createLanguageIcon(Icons.getIcon(LanguageIcons.INSTANCE));
        Icon packageIcon = RubySupport.createLanguageIcon(Icons.getIcon(LanguageIcons.PACKAGE));
        
        renderers.put(RubyNodes.RubyObjectNode.class, new TruffleObjectNode.Renderer(heap, instanceIcon));
        
        renderers.put(RubyNodes.RubyTypeNode.class, new TruffleTypeNode.Renderer(packageIcon));
        
        renderers.put(RubyNodes.RubyObjectFieldNode.class, new TruffleObjectFieldNode.Renderer(heap, instanceIcon));
        
        renderers.put(RubyNodes.RubyObjectArrayItemNode.class, new TruffleObjectArrayItemNode.Renderer(heap, instanceIcon));
        
        renderers.put(RubyNodes.RubyObjectReferenceNode.class, new TruffleObjectReferenceNode.Renderer(heap, instanceIcon));
        
        renderers.put(RubyNodes.RubyLocalObjectNode.class, new TruffleLocalObjectNode.Renderer(heap, instanceIcon));
    }
    
}
