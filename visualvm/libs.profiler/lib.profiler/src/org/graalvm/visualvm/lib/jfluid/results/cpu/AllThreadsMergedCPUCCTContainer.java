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

package org.graalvm.visualvm.lib.jfluid.results.cpu;

import java.util.HashSet;
import java.util.ResourceBundle;


/**
 * A container for all threads merged CPU data. Currently supports/provides only flat profile data.
 *
 * @author Misha Dmitriev
 */
public class AllThreadsMergedCPUCCTContainer extends CPUCCTContainer {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String ALL_THREADS_STRING = ResourceBundle.getBundle("org.graalvm.visualvm.lib.jfluid.results.cpu.Bundle").getString("AllThreadsMergedCPUCCTContainer_AllThreadsString"); // NOI18N
                                                                                                                             // -----

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected int view;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public AllThreadsMergedCPUCCTContainer(CPUResultsSnapshot cpuResSnapshot, PrestimeCPUCCTNode[] rootNodeSubNodes, int view) {
        super(cpuResSnapshot);
        this.threadId = -1;
        this.threadName = ALL_THREADS_STRING;
        this.view = view;
        collectingTwoTimeStamps = cpuResSnapshot.isCollectingTwoTimeStamps();

        compactData = new byte[OFS_SUBNODE02];
        setNCallsForNodeOfs(0, 1); // 1 call for "All threads" node looks more logical than 0 calls
        rootNode = new PrestimeCPUCCTNodeBacked(this, rootNodeSubNodes);

        // Calculate the total execution time for all threads by just summing individual thread total times
        long time = 0;

        for (int i = 0; i < rootNodeSubNodes.length; i++) {
            time += rootNodeSubNodes[i].getTotalTime0();
        }

        wholeGraphNetTime0 = time;
        setTotalTime0ForNodeOfs(0, time);

        if (collectingTwoTimeStamps) {
            time = 0;

            for (int i = 0; i < rootNodeSubNodes.length; i++) {
                time += rootNodeSubNodes[i].getTotalTime1();
            }

            wholeGraphNetTime1 = time;
            setTotalTime1ForNodeOfs(0, time);
        }
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public String[] getMethodClassNameAndSig(int methodId) {
        return cpuResSnapshot.getMethodClassNameAndSig(methodId, view);
    }

    protected FlatProfileContainer generateFlatProfile() {
        preGenerateFlatProfile();

        PrestimeCPUCCTNode[] children = (PrestimeCPUCCTNode[]) rootNode.getChildren();

        if (children != null) for (int i = 0; i < children.length; i++) {
            CPUCCTContainer childContainer = children[i].getContainer();
            childContainer.timePerMethodId0 = this.timePerMethodId0;
            childContainer.timePerMethodId1 = this.timePerMethodId1;
            childContainer.totalTimePerMethodId0 = this.totalTimePerMethodId0;
            childContainer.totalTimePerMethodId1 = this.totalTimePerMethodId1;
            childContainer.invPerMethodId = this.invPerMethodId;
            childContainer.methodsOnStack = new HashSet();
            
            childContainer.addFlatProfTimeForNode(0);

            childContainer.timePerMethodId0 = childContainer.timePerMethodId1 = null;
            childContainer.totalTimePerMethodId0 = childContainer.totalTimePerMethodId1 = null;
            childContainer.invPerMethodId = null;
            childContainer.methodsOnStack = null;
        }

        return postGenerateFlatProfile();
    }

    protected PrestimeCPUCCTNodeFree generateReverseCCT(int methodId) {
        PrestimeCPUCCTNode[] children = (PrestimeCPUCCTNode[]) rootNode.getChildren();

        PrestimeCPUCCTNodeFree rev = null;

        for (int i = 0; i < children.length; i++) {
            CPUCCTContainer childContainer = children[i].getContainer();

            if (rev == null) {
                rev = childContainer.generateReverseCCT(methodId);
            } else {
                childContainer.addToReverseCCT(rev, methodId);
            }
        }

        return rev;
    }
}
