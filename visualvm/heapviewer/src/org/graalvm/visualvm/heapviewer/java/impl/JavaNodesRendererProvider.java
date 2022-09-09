/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Map;
import org.graalvm.visualvm.heapviewer.HeapContext;
import org.graalvm.visualvm.heapviewer.java.ClassNode;
import org.graalvm.visualvm.heapviewer.java.ClassNodeRenderer;
import org.graalvm.visualvm.heapviewer.java.ClassesContainer;
import org.graalvm.visualvm.heapviewer.java.InstanceNode;
import org.graalvm.visualvm.heapviewer.java.InstanceNodeRenderer;
import org.graalvm.visualvm.heapviewer.java.InstanceReferenceNode;
import org.graalvm.visualvm.heapviewer.java.InstanceReferenceNodeRenderer;
import org.graalvm.visualvm.heapviewer.java.InstancesContainer;
import org.graalvm.visualvm.heapviewer.java.LocalObjectNode;
import org.graalvm.visualvm.heapviewer.java.LocalObjectNodeRenderer;
import org.graalvm.visualvm.heapviewer.java.PackageNodeRenderer;
import org.graalvm.visualvm.heapviewer.java.PrimitiveNode;
import org.graalvm.visualvm.heapviewer.java.PrimitiveNodeRenderer;
import org.graalvm.visualvm.heapviewer.java.StackFrameNode;
import org.graalvm.visualvm.heapviewer.java.StackFrameNodeRenderer;
import org.graalvm.visualvm.heapviewer.java.ThreadNode;
import org.graalvm.visualvm.heapviewer.java.ThreadNodeRenderer;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNode;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerRenderer;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.openide.util.lookup.ServiceProvider;

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
        renderers.put(PathToGCRootPlugin.GCRootNode.class, new PathToGCRootPlugin.GCRootNode.Renderer(heap));
    }
    
}
