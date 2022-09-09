/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.heapviewer.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.swing.SortOrder;
import org.graalvm.visualvm.heapviewer.HeapContext;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerRenderer;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.openide.util.Lookup;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class RootNode extends HeapViewerNode {
    
//    public abstract void repaintView();
        
//    public abstract void refreshView();

    
    public abstract HeapContext getContext();
    
    
    public abstract String getViewID();
    
    public abstract HeapViewerNodeFilter getViewFilter();
    
    
    public abstract List<DataType> getDataTypes();
    
    public abstract List<SortOrder> getSortOrders();
    
    
    public abstract void refreshNode(HeapViewerNode node);
    
    
    public abstract void updateChildren(HeapViewerNode node);
    
    public abstract HeapViewerNode[] retrieveChildren(HeapViewerNode node);
    
    
    public abstract HeapViewerRenderer resolveRenderer(HeapViewerNode node);
    
    
    protected void handleOOME(OutOfMemoryError e) {
        System.err.println("Out of memory in " + getViewID() + ": " + e.getMessage()); // NOI18N
    }
    
    
//    public RootNode() {
//        this(NO_NODES);
//    }
//    
//    public RootNode(HeapViewerNode[] children) {
//        super.setChildren(children);
//    }
        
    
//    public void setChildren(HeapViewerNode[] children) {
//        super.setChildren(children);
////        updateChildren(this);
//    }
    
    
    public void reset(boolean makeEmpty) {
        super.resetChildren();
        if (makeEmpty) super.setChildren(HeapViewerNode.NO_NODES);
        updateChildren(null);
    }
    
    
    public boolean equals(Object o) {
        return o instanceof RootNode && toString().equals(o.toString());
    }
    
    public int hashCode() {
        return toString().hashCode();
    }
    
    
    public String toString() {
        return "root of " + getViewID(); // NOI18N
    }
    
    
    protected abstract HeapViewerNode[] lazilyComputeChildren(Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException;
    
    
    private Collection<? extends HeapViewerNode.Provider> nodeProviders;
        
    synchronized Collection<? extends HeapViewerNode.Provider> getNodeProviders() {
        if (nodeProviders == null) {
            nodeProviders = Lookup.getDefault().lookupAll(HeapViewerNode.Provider.class);
            if (!nodeProviders.isEmpty()) {
                nodeProviders = new ArrayList(nodeProviders);
                Iterator<? extends HeapViewerNode.Provider> providers = nodeProviders.iterator();
                Heap heap = getContext().getFragment().getHeap();
                String viewID = getViewID();
                while (providers.hasNext())
                    if (!providers.next().supportsView(heap, viewID))
                        providers.remove();
            }
        }
        return nodeProviders;
    }
    
    private Collection<? extends DataType.ValueProvider> valueProviders;
        
    synchronized Collection<? extends DataType.ValueProvider> getValueProviders() {
        if (valueProviders == null) {
            valueProviders = Lookup.getDefault().lookupAll(DataType.ValueProvider.class);
            if (!valueProviders.isEmpty()) {
                valueProviders = new ArrayList(valueProviders);
                Iterator<? extends DataType.ValueProvider> providers = valueProviders.iterator();
                Heap heap = getContext().getFragment().getHeap();
                String viewID = getViewID();
                while (providers.hasNext())
                    if (!providers.next().supportsView(heap, viewID))
                        providers.remove();
            }
        }
        return valueProviders;
    }


    public static RootNode get(HeapViewerNode node) {
        while (node != null) {
            if (node instanceof RootNode) return (RootNode)node;
            node = (HeapViewerNode)node.getParent();
        }
        return null;
    }
    
}
