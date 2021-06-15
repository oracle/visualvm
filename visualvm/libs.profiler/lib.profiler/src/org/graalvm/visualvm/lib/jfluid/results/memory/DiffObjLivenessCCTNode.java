/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.jfluid.results.memory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.graalvm.visualvm.lib.jfluid.results.FilterSortSupport;

/**
 *
 * @author Jiri Sedlacek
 */
class DiffObjLivenessCCTNode extends PresoObjLivenessCCTNode {

    private final PresoObjLivenessCCTNode node1;
    private final PresoObjLivenessCCTNode node2;


    DiffObjLivenessCCTNode(PresoObjLivenessCCTNode node1, PresoObjLivenessCCTNode node2) {
        this.node1 = node1;
        this.node2 = node2;

        long nCalls1 = node1 == null ? 0 : node1.nCalls;
        long nCalls2 = node2 == null ? 0 : node2.nCalls;
        nCalls = nCalls2 - nCalls1;

        long totalObjSize1 = node1 == null ? 0 : node1.totalObjSize;
        long totalObjSize2 = node2 == null ? 0 : node2.totalObjSize;
        totalObjSize = totalObjSize2 - totalObjSize1;

        float avgObjectAge1 = node1 == null ? 0 : node1.avgObjectAge;
        float avgObjectAge2 = node2 == null ? 0 : node2.avgObjectAge;
        avgObjectAge = avgObjectAge2 - avgObjectAge1;

        int nLiveObjects1 = node1 == null ? 0 : node1.nLiveObjects;
        int nLiveObjects2 = node2 == null ? 0 : node2.nLiveObjects;
        nLiveObjects = nLiveObjects2 - nLiveObjects1;

        int survGen1 = node1 == null ? 0 : node1.survGen;
        int survGen2 = node2 == null ? 0 : node2.survGen;
        survGen = survGen2 - survGen1;

        PresoObjAllocCCTNode[] children1 = node1 == null ? null : (PresoObjAllocCCTNode[])node1.getChildren();
        if (children1 == null) children1 = new PresoObjAllocCCTNode[0];
        PresoObjAllocCCTNode[] children2 = node2 == null ? null : (PresoObjAllocCCTNode[])node2.getChildren();
        if (children2 == null) children2 = new PresoObjAllocCCTNode[0];
        setChildren(computeChildren(children1, children2, this));
    }
    
    
    public DiffObjLivenessCCTNode createFilteredNode() {
        DiffObjLivenessCCTNode filtered = new DiffObjLivenessCCTNode(node1, node2);
        setupFilteredNode(filtered);
        return filtered;
    }
    
    
    public String getNodeName() {
        if (nodeName == null) {
            if (isFiltered()) nodeName = FilterSortSupport.FILTERED_OUT_LBL;
            else nodeName = getNode().getNodeName();
        }
        return nodeName;
    }
    
    public String[] getMethodClassNameAndSig() {
        return getNode().getMethodClassNameAndSig();
    }
    
    
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof DiffObjLivenessCCTNode)) return false;
        DiffObjLivenessCCTNode other = (DiffObjLivenessCCTNode)o;
        if (isFiltered()) {
            return other.isFiltered();
        }
        if (other.isFiltered()) {
            return false;
        }
        return getNode().equals(other.getNode());
    }

    public int hashCode() {
        return getNode().hashCode();
    }
    
    
    public boolean isLeaf() {
        boolean leaf1 = node1 == null || node1.isLeaf();
        boolean leaf2 = node2 == null || node2.isLeaf();
        return leaf1 && leaf2;
    }
    
    private PresoObjLivenessCCTNode getNode() {
        if (node1 == null) {
            return node2;
        }
        return node1;
    }
    
    private static PresoObjAllocCCTNode[] computeChildren(PresoObjAllocCCTNode[] children1, PresoObjAllocCCTNode[] children2, PresoObjLivenessCCTNode parent) {        
        Map<Handle, PresoObjAllocCCTNode> nodes1 = new HashMap();
        for (PresoObjAllocCCTNode node : children1) {
            Handle name = new Handle(node);
            PresoObjAllocCCTNode sameNode = nodes1.get(name);
            if (sameNode == null) nodes1.put(name, node);
            else sameNode.merge(node);
        }
        
        Map<Handle, PresoObjAllocCCTNode> nodes2 = new HashMap();
        for (PresoObjAllocCCTNode node : children2) {
            Handle name = new Handle(node);
            PresoObjAllocCCTNode sameNode = nodes2.get(name);
            if (sameNode == null) nodes2.put(name, node);
            else sameNode.merge(node); // Merge same-named items
        }
        
        List<PresoObjAllocCCTNode> children = new ArrayList();
        for (PresoObjAllocCCTNode node1 : nodes1.values()) {
            PresoObjAllocCCTNode node2 = nodes2.get(new Handle(node1));
            if (node2 != null) children.add(new DiffObjLivenessCCTNode((PresoObjLivenessCCTNode)node1, (PresoObjLivenessCCTNode)node2));
            else children.add(new DiffObjLivenessCCTNode((PresoObjLivenessCCTNode)node1, null));
        }
        for (PresoObjAllocCCTNode node2 : nodes2.values()) {
            if (!nodes1.containsKey(new Handle(node2))) children.add(new DiffObjLivenessCCTNode(null, (PresoObjLivenessCCTNode)node2));
        }
        
        return children.toArray(new PresoObjAllocCCTNode[0]);
    }
    
}
