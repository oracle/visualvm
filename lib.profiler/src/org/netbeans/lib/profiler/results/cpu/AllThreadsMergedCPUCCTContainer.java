/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
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
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.netbeans.lib.profiler.results.cpu.Bundle"); // NOI18N
    private static final String ALL_THREADS_STRING = messages.getString("AllThreadsMergedCPUCCTContainer_AllThreadsString"); // NOI18N
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

        for (int i = 0; i < children.length; i++) {
            CPUCCTContainer childContainer = children[i].getContainer();
            childContainer.timePerMethodId0 = this.timePerMethodId0;
            childContainer.timePerMethodId1 = this.timePerMethodId1;
            childContainer.invPerMethodId = this.invPerMethodId;

            childContainer.addFlatProfTimeForNode(0);

            childContainer.timePerMethodId0 = childContainer.timePerMethodId1 = null;
            childContainer.invPerMethodId = null;
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
