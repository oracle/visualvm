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

package com.sun.tools.visualvm.heapviewer.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.swing.SortOrder;
import javax.swing.SwingWorker;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.results.CCTNode;
import com.sun.tools.visualvm.heapviewer.ui.UIThresholds;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class HeapViewerNode extends CCTNode {
    
    public static final HeapViewerNode[] NO_NODES = new HeapViewerNode[0];
    
    private int indexInParent;
    private HeapViewerNode parent;
    
    private HeapViewerNode[] children;
    
    
    // --- CCTNode ---------------------------------------------------------
    
    public final HeapViewerNode getParent() {
        return parent;
    }

    public HeapViewerNode getChild(int index) {
        HeapViewerNode[] ch = resolveChildren();
        return ch[index];
    }

    public HeapViewerNode[] getChildren() {
        HeapViewerNode[] ch = resolveChildren();
        return ch;
    }

    public int getIndexOfChild(Object child) {
        HeapViewerNode node = (HeapViewerNode)child;
        return node.getParent() == this ? node.indexInParent : -1;
    }

    public int getNChildren() {
        HeapViewerNode[] ch = resolveChildren();
        return ch.length;
    }
    
    public boolean isLeaf() {
        return children == null ? false : getNChildren() == 0;
    }
    
    
    // --- Children logic --------------------------------------------------
    
    protected void setChildren(HeapViewerNode[] ch) {
        for (int i = 0; i < ch.length; i++) {
            ch[i].parent = this;
            ch[i].indexInParent = i;
        }
        children = ch;
    }
    
    protected void resetChildren() {
        forgetChildren(null);
        children = null;
    }
   
    public void forgetChildren(NodesCache cache) {
        if (children != null && children.length > 0) {
            for (HeapViewerNode node : children) {
                node.forgetChildren(cache);
                if (cache == null) node.parent = null;
            }
            if (cache != null && childrenComputed()) cache.storeChildren(this, children);
            children = null;
        }
    }
    
    private boolean childrenComputed() {
        if (children == null) return false;
        if (children.length == 0 || children.length > 1) return true;
        return !(children[0] instanceof ProgressNode);
    }
    
    
    private HeapViewerNode[] resolveChildren() {
        if (children != null) return children;

        RootNode root = RootNode.get(this);
        if (root == null) return NO_NODES;
        
        children = root.retrieveChildren(this);
        if (children != null) return children;

        HeapViewerNode[] ch = computeChildren(root);
        setChildren(ch == null ? NO_NODES : ch);

        return ch;
    }
    
    
    protected HeapViewerNode[] computeChildren(final RootNode root) {
//        if (this == root) {
//            System.err.println(">>> COMPUTING CHILDREN OF ROOT in " + Thread.currentThread());
//            Thread.dumpStack();
//        }
        final Progress progress = new Progress();
        
        SwingWorker<HeapViewerNode[], HeapViewerNode[]> worker = new SwingWorker<HeapViewerNode[], HeapViewerNode[]>() {
            protected HeapViewerNode[] doInBackground() throws Exception {
                return lazilyComputeChildren(root.getContext().getFragment().getHeap(), root.getViewID(), root.getViewFilter(), root.getDataTypes(), root.getSortOrders(), progress);
            }
            protected void done() {
                if (children != null) try {
                    // TODO: children not valid in case the sorting changed during computation!
                    HeapViewerNode[] newChildren = get();
                    // newChildren may be null, for example if the worker thread has been interrupted
                    if (newChildren != null) {
                        setChildren(newChildren);
                        root.updateChildren(HeapViewerNode.this);
                    }
                } catch (Exception ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        };
        worker.execute();
        try {
            return worker.get(UIThresholds.MODEL_CHILDREN, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ExecutionException ex) {
            Exceptions.printStackTrace(ex);
        } catch (TimeoutException ex) {
            return new HeapViewerNode[] { new ProgressNode(progress) };
        }

        return null;
    }

    protected HeapViewerNode[] lazilyComputeChildren(Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) {

        List<HeapViewerNode> nodes = new ArrayList();
        Collection<? extends Provider> providers;
        
        RootNode root = RootNode.get(this);
        if (root != null) providers = new ArrayList(root.getNodeProviders());
        else providers = new ArrayList(Lookup.getDefault().lookupAll(Provider.class));

        Iterator<? extends Provider> iproviders = providers.iterator();
        while (iproviders.hasNext())
            if (!iproviders.next().supportsNode(HeapViewerNode.this, heap, viewID))
                iproviders.remove();
        
        if (providers.size() == 1) {
            HeapViewerNode[] n = providers.iterator().next().getNodes(this, heap, viewID, viewFilter, dataTypes, sortOrders, progress);
            if (n != null) nodes.addAll(Arrays.asList(checkForLoops(this, n)));
        } else {
            for (Provider provider : providers) nodes.add(new ChildrenContainer(provider));
        }
        
        return nodes.toArray(NO_NODES);
    }
    
    
    private static HeapViewerNode[] checkForLoops(HeapViewerNode parent, HeapViewerNode[] nodes) {
        Map<HeapViewerNode, HeapViewerNode> pathToRoot = new HashMap();
        while (parent != null) {
            pathToRoot.put(parent, parent);
            parent = (HeapViewerNode)parent.getParent();
        }
        
        for (int i = 0; i < nodes.length; i++) {
            HeapViewerNode loopOrigin = pathToRoot.get(nodes[i]);
            if (loopOrigin != null) nodes[i] = new LoopNode(nodes[i], loopOrigin);
        }
        
        return nodes;
    }
    
    
    // --- Values --------------------------------------------------------------
    
    private static final Object NO_VALUE = new Object();
    private Map<DataType, Object> foreignValues;

    protected <T> T getValue(DataType<T> type, Heap heap) {
        return DataType.DEFAULT_TYPES.contains(type) ? type.getUnsupportedValue() : null;
    }
    
    public static <T> T getValue(HeapViewerNode node, DataType<T> type, Heap heap) {
        return getValue(node, type, heap, null);
    }
    
    // To be used for "temporary" nodes out of the tree structure (like providers returning first N nodes)
    public static <T> T getValue(HeapViewerNode node, DataType<T> type, Heap heap, HeapViewerNode parent) {
        Object value = node.getValue(type, heap);
        if (Objects.equals(value, type.getUnsupportedValue())) return (T)value;
        if (value != null || type.getNoValue() == null) return (T)value;
        
        if (!type.valuesAvailable(heap)) return type.getNotAvailableValue();
        
        if (node.foreignValues != null) {
            value = node.foreignValues.get(type);
            if (value != null) return value == NO_VALUE ? null : (T)value;
        }
        
        RootNode root = RootNode.get(parent != null ? parent : node);
        Iterator<? extends DataType.ValueProvider> providers = root != null ?
                root.getValueProviders().iterator() : 
                Lookup.getDefault().lookupAll(DataType.ValueProvider.class).iterator();

        if (providers.hasNext()) {
            while (value == null && providers.hasNext())
                value = providers.next().getValue(node, type, heap);
        }
        
        if (node.foreignValues == null) node.foreignValues = new IdentityHashMap(1);
        node.foreignValues.put(type, value == null ? NO_VALUE : value);
        
        return (T)value;
    }
    
    
    // --- Sorting support -----------------------------------------------------
    
    public final void willBeSorted() {
        if (updateChildrenOnSort()) forgetChildren(null);
    }
    
    private boolean updateChildrenOnSort() {
        return children != null && children.length > 1 && children[children.length - 1] instanceof MoreNodesNode;
    }
    
    
    // --- Cloning support -----------------------------------------------------
    
    public HeapViewerNode createCopy() {
        return null;
    }
    
    protected void setupCopy(HeapViewerNode copy) {
    }
            
            
    // --- Utils ---------------------------------------------------------------
    
    public static TreePath fromNode(TreeNode node) {
        return fromNode(node, null);
    }
    
    public static TreePath fromNode(TreeNode node, TreeNode root) {
        List l = new ArrayList();
        while (node != root) {
            l.add(0, node);
            node = node.getParent();
        }
        if (node != null) l.add(0, node);
        return new TreePath(l.toArray(new Object[0]));
    }
    
    
    private static class ChildrenContainer extends TextNode {
        
        private final Provider provider;
        
        ChildrenContainer(Provider provider) {
            super("<" + provider.getName() + ">"); // NOI18N
            resetChildren();
            
            this.provider = provider;
        }
        
        protected HeapViewerNode[] lazilyComputeChildren(Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) {
            HeapViewerNode parent = (HeapViewerNode)getParent();
            HeapViewerNode[] n = provider.getNodes(parent, heap, viewID, viewFilter, dataTypes, sortOrders, progress);
            return n != null ? checkForLoops(parent, n) : NO_NODES;
        }
        
    }
    
    
    // --- SPI -----------------------------------------------------------------
    
    public static abstract class Provider {
    
        public abstract String getName();

        public abstract boolean supportsView(Heap heap, String viewID);

        public abstract boolean supportsNode(HeapViewerNode parent, Heap heap, String viewID);

        public abstract HeapViewerNode[] getNodes(HeapViewerNode parent, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress);

    }
    
}
