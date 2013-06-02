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

import org.netbeans.lib.profiler.results.CCTNode;

/**
 *
 * @author Jiri Sedlacek
 */
public class CPUResultsDiff extends CPUResultsSnapshot {
    
    private final CPUResultsSnapshot snapshot1;
    private final CPUResultsSnapshot snapshot2;
    
    
    public CPUResultsDiff(CPUResultsSnapshot snapshot1, CPUResultsSnapshot snapshot2) {
        this.snapshot1 = new CPUResultsSnapshot();
        this.snapshot1.readFromSnapshot(snapshot1);
        this.snapshot2 = new CPUResultsSnapshot();
        this.snapshot2.readFromSnapshot(snapshot2);
    }
    
    public boolean isCollectingTwoTimeStamps() {
        return snapshot1.collectingTwoTimeStamps &&
               snapshot2.collectingTwoTimeStamps;
    }

    // Not used for diff
    public CPUCCTContainer getContainerForThread(int threadId, int view) {
        return snapshot1.getContainerForThread(threadId, view);
    }

    public DiffFlatProfileContainer getFlatProfile(int threadId, int view) {
        // NOTE: only works correctly for threadId == -1 (All Threads)
        FlatProfileContainer fpc1 = snapshot1.getFlatProfile(threadId, view);
        FlatProfileContainer fpc2 = snapshot2.getFlatProfile(threadId, view);
        return DiffFlatProfileContainer.create(fpc1, fpc2);
    }
    
    // Not used for diff
    public String[] getInstrMethodClasses(int view) {
        return snapshot1.getInstrMethodClasses(view);
    }

    // Used by CPUTestCase.logInstrumented
    // Otherwise not used for diff
    public String[] getInstrMethodNames() {
        return snapshot1.getInstrMethodNames();
    }

    // Not used for diff
    public String[] getInstrMethodSignatures() {
        return snapshot1.getInstrMethodSignatures();
    }

    public String[] getMethodClassNameAndSig(int methodId, int view) {
        return methodId < 0 ? snapshot2.getMethodClassNameAndSig(-methodId, view) :
                              snapshot1.getMethodClassNameAndSig(methodId, view);
    }

    // Not used for diff
    public int getNInstrMethods() {
        return snapshot1.getNInstrMethods();
    }

    // Not used
    public int getNThreads() {
        return snapshot1.getNThreads();
    }

    // Not used for diff
    public PrestimeCPUCCTNode getReverseCCT(int threadId, int methodId, int view) {
        return null;
    }

    public PrestimeCPUCCTNode getRootNode(int view) {
        PrestimeCPUCCTNodeBacked root1 = (PrestimeCPUCCTNodeBacked)snapshot1.getRootNode(view);
        PrestimeCPUCCTNodeBacked root2 = (PrestimeCPUCCTNodeBacked)snapshot2.getRootNode(view);
        return new DiffCPUCCTNode(root1, root2);
    }
    
    public long getBound(int view) {
        long bound = Long.MIN_VALUE;
        
        PrestimeCPUCCTNode root = getRootNode(view);
        CCTNode[] children = root.getChildren();
        for (CCTNode child : children)
            bound = Math.max(bound, Math.abs(((PrestimeCPUCCTNode)child).getTotalTime0()));
        
        return bound;
    }
    
    public void filterForward(final String filter, final int filterType, final PrestimeCPUCCTNodeBacked root) {
        PrestimeCPUCCTNodeBacked node1 = ((DiffCPUCCTNode)root).node1;
        if (node1 != null) snapshot1.filterForward(filter, filterType, node1);
        
        PrestimeCPUCCTNodeBacked node2 = ((DiffCPUCCTNode)root).node2;
        if (node2 != null) snapshot2.filterForward(filter, filterType, node2);
        
        super.filterForward(filter, filterType, root);
    }
    
    public void saveSortParams(int sortBy, boolean sortOrder, CCTNode node) {
        PrestimeCPUCCTNodeBacked node1 = ((DiffCPUCCTNode)node).node1;
        if (node1 != null) snapshot1.saveSortParams(sortBy, sortOrder, node1);
        
        PrestimeCPUCCTNodeBacked node2 = ((DiffCPUCCTNode)node).node2;
        if (node2 != null) snapshot2.saveSortParams(sortBy, sortOrder, node2);
        
        super.saveSortParams(sortBy, sortOrder, node);
    }
    
    // TODO: used by CPUDiffPanel, fix!
    public int[] getThreadIds() {
        return snapshot1.getThreadIds();
    }

    // Not used for diff
    public String getThreadNameForId(int threadId) {
        return snapshot1.getThreadNameForId(threadId);
    }

    // TODO: used by CPUDiffPanel, fix!
    public String[] getThreadNames() {
        return snapshot1.getThreadNames();
    }
    
}
