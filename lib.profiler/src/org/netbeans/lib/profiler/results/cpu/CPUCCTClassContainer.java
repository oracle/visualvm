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

import org.netbeans.lib.profiler.utils.IntVector;
import java.util.Enumeration;
import java.util.Hashtable;


/**
 * An extension of CPUCCTContainer that has functionality to build a class- or package-level CCT out of the method-level CCT.
 *
 * @author Misha Dmitriev
 */
public class CPUCCTClassContainer extends CPUCCTContainer {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected int view;

    //-- Temporary data used during construction
    private CPUCCTContainer sourceContainer;
    private MethodIdMap methodIdMap;
    private long childTotalTime0;
    private long childTotalTime1;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public CPUCCTClassContainer(CPUCCTContainer sourceContainer, MethodIdMap methodIdMap, int view) {
        super(sourceContainer.cpuResSnapshot);
        this.view = view;
        this.sourceContainer = sourceContainer;
        this.threadId = sourceContainer.threadId;
        this.threadName = sourceContainer.threadName;
        this.wholeGraphNetTime0 = sourceContainer.wholeGraphNetTime0;
        this.wholeGraphNetTime1 = sourceContainer.wholeGraphNetTime1;
        this.childOfsSize = CHILD_OFS_SIZE_3;

        collectingTwoTimeStamps = sourceContainer.collectingTwoTimeStamps;
        nodeSize = sourceContainer.nodeSize;

        compactData = new byte[sourceContainer.compactData.length]; // Initially create a same-sized array - should be more than enough

        this.methodIdMap = methodIdMap;

        IntVector rootMethodVec = new IntVector();
        rootMethodVec.add(0);

        int lastOfs = generateClassNodeFromMethodNodes(rootMethodVec, 0);

        // Create an array of appropriate size
        byte[] oldData = compactData;
        compactData = new byte[lastOfs];
        System.arraycopy(oldData, 0, compactData, 0, lastOfs);
        oldData = null;

        rootNode = new PrestimeCPUCCTNodeBacked(this, null, 0);

        if (rootNode.getMethodId() == 0) {
            rootNode.setThreadNode();
        }

        methodIdMap = null;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public String[] getMethodClassNameAndSig(int methodId) {
        return cpuResSnapshot.getMethodClassNameAndSig(methodId, view);
    }

    /**
     * For a given vector of source (method-level) nodes, where all nodes have the same class, generate
     * a single class-level node. Do the same with all the source node's children.
     * Returns the offset right after the last generated node, which is this node if it has no children,
     * or the last recursive child of this node.
     */
    protected int generateClassNodeFromMethodNodes(IntVector methodNodes, int dataOfs) {
        int nMethodNodes = methodNodes.size();
        int nCalls = 0;
        long time0 = 0;
        long time1 = 0;

        for (int i = 0; i < nMethodNodes; i++) {
            int methodNodeOfs = methodNodes.get(i);
            nCalls += sourceContainer.getNCallsForNodeOfs(methodNodeOfs);
            time0 += sourceContainer.getSelfTime0ForNodeOfs(methodNodeOfs);

            if (collectingTwoTimeStamps) {
                time1 += sourceContainer.getSelfTime1ForNodeOfs(methodNodeOfs);
            }
        }

        int methodId = sourceContainer.getMethodIdForNodeOfs(methodNodes.get(0));

        if (methodId != 0) {
            methodId = methodIdMap.getClassOrPackageIdForMethodId(methodId);
        }

        setMethodIdForNodeOfs(dataOfs, methodId);
        setNCallsForNodeOfs(dataOfs, nCalls);
        setSelfTime0ForNodeOfs(dataOfs, time0);

        if (collectingTwoTimeStamps) {
            setSelfTime1ForNodeOfs(dataOfs, time1);
        }

        // Now add all the children of methodNodes that have the same class, to thisNode, and collect the rest of the
        // children of methodNodes into sourceChildren vector.
        IntVector sourceChildren = new IntVector();
        Hashtable uniqChildrenCache = new Hashtable();

        for (int i = 0; i < nMethodNodes; i++) {
            int methodNodeOfs = methodNodes.get(i);
            int nChildren = sourceContainer.getNChildrenForNodeOfs(methodNodeOfs);

            if (nChildren > 0) {
                processChildren(dataOfs, methodNodeOfs, nChildren, sourceChildren, uniqChildrenCache);
            }
        }

        int thisNodeNChildren = uniqChildrenCache.size();
        int nextNodeOfs = dataOfs + nodeSize + (thisNodeNChildren * childOfsSize);

        if (thisNodeNChildren == 0) {
            childTotalTime0 = getSelfTime0ForNodeOfs(dataOfs); // We are effectively returning these values

            if (collectingTwoTimeStamps) {
                childTotalTime1 = getSelfTime1ForNodeOfs(dataOfs);
            }

            setTotalTime0ForNodeOfs(dataOfs, childTotalTime0);

            if (collectingTwoTimeStamps) {
                setTotalTime1ForNodeOfs(dataOfs, childTotalTime1);
            }

            return nextNodeOfs;
        } else {
            time0 = getSelfTime0ForNodeOfs(dataOfs);

            if (collectingTwoTimeStamps) {
                time1 = getSelfTime1ForNodeOfs(dataOfs);
            }
        }

        setNChildrenForNodeOfs(dataOfs, thisNodeNChildren);

        IntVector sameTypeChildren = new IntVector();
        int nAllChildren = sourceChildren.size();
        int[] sourceChildrenClassIds = new int[nAllChildren];

        for (int i = 0; i < nAllChildren; i++) {
            sourceChildrenClassIds[i] = methodIdMap.getClassOrPackageIdForMethodId(sourceContainer.getMethodIdForNodeOfs(sourceChildren
                                                                                                                         .get(i)));
        }

        Enumeration e = uniqChildrenCache.elements();

        for (int i = 0; e.hasMoreElements(); i++) {
            sameTypeChildren.clear();

            int sourceChildClassOrPackageId = ((Integer) e.nextElement()).intValue();

            for (int j = 0; j < nAllChildren; j++) {
                if (sourceChildrenClassIds[j] == sourceChildClassOrPackageId) {
                    sameTypeChildren.add(sourceChildren.get(j));
                }
            }

            setChildOfsForNodeOfs(dataOfs, i, nextNodeOfs);

            nextNodeOfs = generateClassNodeFromMethodNodes(sameTypeChildren, nextNodeOfs);
            time0 += childTotalTime0;

            if (collectingTwoTimeStamps) {
                time1 += childTotalTime1;
            }
        }

        setTotalTime0ForNodeOfs(dataOfs, time0);

        if (collectingTwoTimeStamps) {
            setTotalTime1ForNodeOfs(dataOfs, time1);
        }

        childTotalTime0 = time0;

        if (collectingTwoTimeStamps) {
            childTotalTime1 = time1;
        }

        return nextNodeOfs;
    }

    /**
     * Given this target node, and the array of its source-level children, treat them as follows:
     * 1. The info for a source child who has the same class as this node, is added to this node.
     * Its own children are processed recursively by calling this same method.
     * 2. The first source child whose class is different and was not observed before (not contained
     * in uniqChildCache) is added to uniqChildCache, and to allSourceChildren.
     * 3. All other source children are added to allSourceChildren, but not to uniqChildCache.
     */
    protected void processChildren(int dataOfs, int methodNodeOfs, int nChildren, IntVector allSourceChildren,
                                   Hashtable uniqChildCache) {
        int thisNodeClassOrPackageId = getMethodIdForNodeOfs(dataOfs);

        int nCalls = 0;
        long time0 = 0;
        long time1 = 0;

        for (int i = 0; i < nChildren; i++) {
            int sourceChildOfs = sourceContainer.getChildOfsForNodeOfs(methodNodeOfs, i);
            int sourceChildClassOrPackageId = methodIdMap.getClassOrPackageIdForMethodId(sourceContainer.getMethodIdForNodeOfs(sourceChildOfs));

            if (sourceChildClassOrPackageId == thisNodeClassOrPackageId) { // A child node has the same class as this node
                nCalls += sourceContainer.getNCallsForNodeOfs(sourceChildOfs);
                time0 += sourceContainer.getSelfTime0ForNodeOfs(sourceChildOfs);

                if (collectingTwoTimeStamps) {
                    time1 += sourceContainer.getSelfTime1ForNodeOfs(sourceChildOfs);
                }

                // sourceChild's children logically become this node's children now.
                int nSourceChildChildren = sourceContainer.getNChildrenForNodeOfs(sourceChildOfs);

                if (nSourceChildChildren > 0) {
                    this.processChildren(dataOfs, sourceChildOfs, nSourceChildChildren, allSourceChildren, uniqChildCache);
                }
            } else { // A child node belongs to a different class

                Integer key = new Integer(sourceChildClassOrPackageId);

                if (!uniqChildCache.containsKey(key)) {
                    uniqChildCache.put(key, key);
                }

                allSourceChildren.add(sourceChildOfs);
            }
        }

        nCalls += getNCallsForNodeOfs(dataOfs);
        time0 += getSelfTime0ForNodeOfs(dataOfs);

        if (collectingTwoTimeStamps) {
            time1 += getSelfTime1ForNodeOfs(dataOfs);
        }

        setNCallsForNodeOfs(dataOfs, nCalls);
        setSelfTime0ForNodeOfs(dataOfs, time0);

        if (collectingTwoTimeStamps) {
            setSelfTime1ForNodeOfs(dataOfs, time1);
        }
    }
}
