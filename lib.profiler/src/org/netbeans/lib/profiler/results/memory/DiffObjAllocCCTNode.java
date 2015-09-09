/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2015 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */
package org.netbeans.lib.profiler.results.memory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.netbeans.lib.profiler.results.FilterSortSupport;

/**
 *
 * @author Jiri Sedlacek
 */
class DiffObjAllocCCTNode extends PresoObjAllocCCTNode {
    
    private final PresoObjAllocCCTNode node1;
    private final PresoObjAllocCCTNode node2;
    
    
    DiffObjAllocCCTNode(PresoObjAllocCCTNode node1, PresoObjAllocCCTNode node2) {
        this.node1 = node1;
        this.node2 = node2;
        
        long nCalls1 = node1 == null ? 0 : node1.nCalls;
        long nCalls2 = node2 == null ? 0 : node2.nCalls;
        nCalls = nCalls2 - nCalls1;
        
        long totalObjSize1 = node1 == null ? 0 : node1.totalObjSize;
        long totalObjSize2 = node2 == null ? 0 : node2.totalObjSize;
        totalObjSize = totalObjSize2 - totalObjSize1;
        
        PresoObjAllocCCTNode[] children1 = node1 == null ? null : (PresoObjAllocCCTNode[])node1.getChildren();
        if (children1 == null) children1 = new PresoObjAllocCCTNode[0];
        PresoObjAllocCCTNode[] children2 = node2 == null ? null : (PresoObjAllocCCTNode[])node2.getChildren();
        if (children2 == null) children2 = new PresoObjAllocCCTNode[0];
        setChildren(computeChildren(children1, children2, this));
    }
    
    
    public DiffObjAllocCCTNode createFilteredNode() {
        DiffObjAllocCCTNode filtered = new DiffObjAllocCCTNode(node1, node2);
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
        if (!(o instanceof DiffObjAllocCCTNode)) return false;
        DiffObjAllocCCTNode other = (DiffObjAllocCCTNode)o;
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
    
    private PresoObjAllocCCTNode getNode() {
        if (node1 == null) {
            return node2;
        }
        return node1;
    }
    
    private static PresoObjAllocCCTNode[] computeChildren(PresoObjAllocCCTNode[] children1, PresoObjAllocCCTNode[] children2, PresoObjAllocCCTNode parent) {        
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
            if (node2 != null) children.add(new DiffObjAllocCCTNode(node1, node2));
            else children.add(new DiffObjAllocCCTNode(node1, null));
        }
        for (PresoObjAllocCCTNode node2 : nodes2.values()) {
            if (!nodes1.containsKey(new Handle(node2))) children.add(new DiffObjAllocCCTNode(null, node2));
        }
        
        return children.toArray(new PresoObjAllocCCTNode[children.size()]);
    }
    
}
