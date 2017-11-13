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

package com.sun.tools.visualvm.heapviewer.java.impl;

import java.util.Map;
import org.netbeans.lib.profiler.heap.Heap;
import com.sun.tools.visualvm.heapviewer.HeapContext;
import com.sun.tools.visualvm.heapviewer.java.ClassNode;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNode;
import com.sun.tools.visualvm.heapviewer.java.InstanceNode;
import com.sun.tools.visualvm.heapviewer.java.ClassNodeRenderer;
import com.sun.tools.visualvm.heapviewer.java.ClassesContainer;
import com.sun.tools.visualvm.heapviewer.java.InstanceNodeRenderer;
import com.sun.tools.visualvm.heapviewer.java.InstancesContainer;
import com.sun.tools.visualvm.heapviewer.java.PackageNodeRenderer;
import com.sun.tools.visualvm.heapviewer.java.LocalObjectNode;
import com.sun.tools.visualvm.heapviewer.java.LocalObjectNodeRenderer;
import com.sun.tools.visualvm.heapviewer.java.InstanceReferenceNode;
import com.sun.tools.visualvm.heapviewer.java.InstanceReferenceNodeRenderer;
import com.sun.tools.visualvm.heapviewer.java.PrimitiveNode;
import com.sun.tools.visualvm.heapviewer.java.PrimitiveNodeRenderer;
import com.sun.tools.visualvm.heapviewer.java.StackFrameNode;
import com.sun.tools.visualvm.heapviewer.java.StackFrameNodeRenderer;
import com.sun.tools.visualvm.heapviewer.java.ThreadNode;
import com.sun.tools.visualvm.heapviewer.java.ThreadNodeRenderer;
import org.openide.util.lookup.ServiceProvider;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerRenderer;

/**
 *
 * @author Jiri Sedlacek
 */
@ServiceProvider(service=HeapViewerRenderer.Provider.class)
public class JavaNodesRendererProvider extends HeapViewerRenderer.Provider {
    
    public boolean supportsView(HeapContext context, String viewID) {
        return true;
    }

    public void registerRenderers(Map<Class<? extends HeapViewerNode>, HeapViewerRenderer> renderers, HeapContext context) {
        Heap heap = context.getFragment().getHeap();
        
        // packages
        PackageNodeRenderer packageRenderer = new PackageNodeRenderer(heap);
        renderers.put(ClassesContainer.Objects.class, packageRenderer);
        renderers.put(ClassesContainer.Nodes.class, packageRenderer);
        renderers.put(ClassesContainer.ContainerNodes.class, packageRenderer);
        
        // classes
        ClassNodeRenderer classRenderer = new ClassNodeRenderer(heap);
        renderers.put(ClassNode.class, classRenderer);
        renderers.put(InstancesContainer.Objects.class, classRenderer);
        renderers.put(InstancesContainer.Nodes.class, classRenderer);
        
        // instances
        renderers.put(InstanceNode.class, new InstanceNodeRenderer(heap));
        
        // object fields & items
        renderers.put(InstanceReferenceNode.class, new InstanceReferenceNodeRenderer(heap));
        
        // primitive fields & items
        renderers.put(PrimitiveNode.class, new PrimitiveNodeRenderer());
        
        // threads
        renderers.put(ThreadNode.class, new ThreadNodeRenderer(heap));
        
        // stack frames
        renderers.put(StackFrameNode.class, new StackFrameNodeRenderer());
        
        // local variables
        renderers.put(LocalObjectNode.class, new LocalObjectNodeRenderer(heap));
        
        
        // GC types
        renderers.put(GCTypeNode.class, new GCTypeNode.Renderer());
    }
    
}
