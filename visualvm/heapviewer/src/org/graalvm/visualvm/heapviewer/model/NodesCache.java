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

package org.graalvm.visualvm.heapviewer.model;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import org.graalvm.visualvm.lib.jfluid.results.CCTNode;

/**
 *
 * @author Jiri Sedlacek
 */
public final class NodesCache {
    
    private Reference<Map<NodeKey, HeapViewerNode[]>> cache;
    
    
    public void storeChildren(HeapViewerNode node, HeapViewerNode[] children) {
        getCache().put(new NodeKey(node), children);
    }
    
    public HeapViewerNode[] retrieveChildren(HeapViewerNode node) {
        return getCache().remove(new NodeKey(node));
    }
    
    
    public void clear() {
        cache = null;
    }
    
    
    private Map<NodeKey, HeapViewerNode[]> getCache() {
        Map<NodeKey, HeapViewerNode[]> c = cache == null ? null : cache.get();
        if (c == null) {
            c = new HashMap();
            cache = new WeakReference(c);
        }
        return c;
    }
    
    
    private static final class NodeKey {
        
        private final HeapViewerNode node;
        private int distance;
        private int hashCode;

        NodeKey(HeapViewerNode node) {
            this.node = node;
            
            distance = 0;
            hashCode = 1;
            
            while (node != null) {
                hashCode = 31 * hashCode + node.hashCode();
                distance++;
                node = (HeapViewerNode)node.getParent();
            }
        }

        public final int hashCode() {
            return hashCode;
        }
        
        public final boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof NodeKey)) return false;
            
            NodeKey nk = (NodeKey)o;
            if (distance != nk.distance) return false;
            
            CCTNode n = nk.node;
            CCTNode t = node;
            while (t != null) {
                if (!t.equals(n)) return false;
                t = t.getParent();
                n = n.getParent();
            }
            
            return true;
        }

    }
    
}
