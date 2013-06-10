/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.netbeans.lib.profiler.results.cpu;

import java.util.*;
import org.netbeans.lib.profiler.results.CCTNode;

/**
 *
 * @author Jiri Sedlacek
 */
class DiffCPUCCTNode extends PrestimeCPUCCTNodeBacked {
    
    final PrestimeCPUCCTNodeBacked node1;
    final PrestimeCPUCCTNodeBacked node2;
    
    
    DiffCPUCCTNode(PrestimeCPUCCTNodeBacked n1, PrestimeCPUCCTNodeBacked n2) {
        node1 = n1;
        node2 = n2;
        container = node1 == null ? node2.container : node1.container;
    }
    
    
    public DiffCPUCCTNode createRootCopy() {
        PrestimeCPUCCTNodeBacked copy1 = node1.createRootCopy();
        PrestimeCPUCCTNodeBacked copy2 = node2.createRootCopy();
        return new DiffCPUCCTNode(copy1, copy2);
    }
    

    @Override
    public CCTNode getChild(int index) {
        getChildren();

        if (index < children.length) {
            return children[index];
        } else {
            return null;
        }
    }

    @Override
    public CCTNode[] getChildren() {
        if (children != null) return children;
        
        PrestimeCPUCCTNode[] children1 = node1 == null ? null : (PrestimeCPUCCTNode[])node1.getChildren();
        PrestimeCPUCCTNode[] children2 = node2 == null ? null : (PrestimeCPUCCTNode[])node2.getChildren();
        
        if (children1 == null) children1 = new PrestimeCPUCCTNode[0];
        if (children2 == null) children2 = new PrestimeCPUCCTNode[0];
        children = computeChildren(children1, children2, this);
        
        if (children == null) children = new PrestimeCPUCCTNode[0];
        nChildren = children.length;
        
        return children;
    }
    
    private static PrestimeCPUCCTNodeBacked[] computeChildren(PrestimeCPUCCTNode[] children1, PrestimeCPUCCTNode[] children2, PrestimeCPUCCTNode parent) {        
        Map<String, PrestimeCPUCCTNode> nodes1 = new HashMap();
        for (PrestimeCPUCCTNode node : children1) {
            String name = node.getNodeName();
            PrestimeCPUCCTNodeBacked sameNode = (PrestimeCPUCCTNodeBacked)nodes1.get(name);
            if (sameNode == null) nodes1.put(name, node);
            else sameNode.merge((PrestimeCPUCCTNodeBacked)node); // Merge same-named items
        }
        
        Map<String, PrestimeCPUCCTNode> nodes2 = new HashMap();
        for (PrestimeCPUCCTNode node : children2) {
            String name = node.getNodeName();
            PrestimeCPUCCTNodeBacked sameNode = (PrestimeCPUCCTNodeBacked)nodes2.get(name);
            if (sameNode == null) nodes2.put(name, node);
            else sameNode.merge((PrestimeCPUCCTNodeBacked)node); // Merge same-named items
        }
        
        List<PrestimeCPUCCTNodeBacked> children = new ArrayList();
        for (PrestimeCPUCCTNode node1 : nodes1.values()) {
            PrestimeCPUCCTNode node2 = nodes2.get(node1.getNodeName());
            if (node2 != null) children.add(new DiffCPUCCTNode((PrestimeCPUCCTNodeBacked)node1, (PrestimeCPUCCTNodeBacked)node2));
            else children.add(new DiffCPUCCTNode((PrestimeCPUCCTNodeBacked)node1, null));
        }
        for (PrestimeCPUCCTNode node2 : nodes2.values()) {
            if (!nodes1.containsKey(node2.getNodeName())) children.add(new DiffCPUCCTNode(null, (PrestimeCPUCCTNodeBacked)node2));
        }
        
        for (PrestimeCPUCCTNodeBacked child : children) child.parent = parent;
        
        return children.toArray(new PrestimeCPUCCTNodeBacked[children.size()]);
    }
    
    protected void resetChildren() {
        if (node1 != null) node1.resetChildren();
        if (node2 != null) node2.resetChildren();
        children = null;
    }

    @Override
    public int getMethodId() {
        return node1 == null ? (-node2.getMethodId()) : node1.getMethodId();
    }

    @Override
    public int getNCalls() {
        int nCalls1 = node1 == null ? 0 : node1.getNCalls();
        int nCalls2 = node2 == null ? 0 : node2.getNCalls();
        return nCalls2 - nCalls1;
    }

    @Override
    public int getNChildren() {
        return getChildren().length;
    }

    @Override
    public long getSleepTime0() {
        long sleepTime0_1 = node1 == null ? 0 : node1.getSleepTime0();
        long sleepTime0_2 = node2 == null ? 0 : node2.getSleepTime0();
        return sleepTime0_2 - sleepTime0_1;
    }

    @Override
    public int getThreadId() {
        return node1 == null ? node2.getThreadId() : node1.getThreadId();
    }

    @Override
    public long getTotalTime0() {
        long totalTime0_1 = node1 == null ? 0 : node1.getTotalTime0();
        long totalTime0_2 = node2 == null ? 0 : node2.getTotalTime0();
        return totalTime0_2 - totalTime0_1;
    }

    @Override
    public float getTotalTime0InPerCent() {
        float totalTime0ipc_1 = node1 == null ? 0 : node1.getTotalTime0InPerCent();
        float totalTime0ipc_2 = node2 == null ? 0 : node2.getTotalTime0InPerCent();
        return totalTime0ipc_2 - totalTime0ipc_1;
    }

    @Override
    public long getTotalTime1() {
        long totalTime1_1 = node1 == null ? 0 : node1.getTotalTime1();
        long totalTime1_2 = node2 == null ? 0 : node2.getTotalTime1();
        return totalTime1_2 - totalTime1_1;
    }

    @Override
    public float getTotalTime1InPerCent() {
        float totalTime1ipc_1 = node1 == null ? 0 : node1.getTotalTime1InPerCent();
        float totalTime1ipc_2 = node2 == null ? 0 : node2.getTotalTime1InPerCent();
        return totalTime1ipc_2 - totalTime1ipc_1;
    }

    @Override
    public long getWaitTime0() {
        long waitTime0_1 = node1 == null ? 0 : node1.getWaitTime0();
        long waitTime0_2 = node2 == null ? 0 : node2.getWaitTime0();
        return waitTime0_2 - waitTime0_1;
    }

    @Override
    public void sortChildren(int sortBy, boolean sortOrder) {
        if (node1 != null) node1.sortChildren(sortBy, sortOrder);
        if (node2 != null) node2.sortChildren(sortBy, sortOrder);
        super.sortChildren(sortBy, sortOrder);
    }    
    
    
    @Override
    public String getNodeName() {
        return node1 == null ? node2.getNodeName() : node1.getNodeName();
    }
    
    public void setSelfTimeNode() {
        if (node1 != null) node1.setSelfTimeNode();
        if (node2 != null) node2.setSelfTimeNode();
    }
    
    @Override
    public boolean isSelfTimeNode() {
        return node1 == null ? node2.isSelfTimeNode() : node1.isSelfTimeNode();
    }
    
    public void setThreadNode() {
        if (node1 != null) node1.setThreadNode();
        if (node2 != null) node2.setThreadNode();
    }

    @Override
    public boolean isThreadNode() {
        return node1 == null ? node2.isThreadNode() : node1.isThreadNode();
    }
    
    public void setFilteredNode() {
        if (node1 != null) node1.setFilteredNode();
        if (node2 != null) node2.setFilteredNode();
    }
    
    public void resetFilteredNode() {
        if (node1 != null) node1.resetFilteredNode();
        if (node2 != null) node2.resetFilteredNode();
    }

    @Override
    public boolean isFilteredNode() {
        return node1 == null ? node2.isFilteredNode() : node1.isFilteredNode();
    }
    
    public boolean equals(Object o) {
        return getNodeName().equals(((DiffCPUCCTNode)o).getNodeName());
    }
    
    public int hashCode() {
        return getNodeName().hashCode();
    }
    
}
