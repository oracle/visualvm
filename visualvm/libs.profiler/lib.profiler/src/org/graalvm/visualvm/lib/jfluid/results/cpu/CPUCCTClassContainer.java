/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

import org.graalvm.visualvm.lib.jfluid.utils.IntVector;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


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

        rootNode = new PrestimeCPUCCTNodeBacked(this, null, 0);

        if (rootNode.getMethodId() == 0) {
            rootNode.setThreadNode();
        }
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
        Set uniqChildrenCache = new HashSet();

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
            int mid = sourceContainer.getMethodIdForNodeOfs(sourceChildren.get(i));
            sourceChildrenClassIds[i] = methodIdMap.getClassOrPackageIdForMethodId(mid);
        }

        Iterator e = uniqChildrenCache.iterator();

        for (int i = 0; e.hasNext(); i++) {
            sameTypeChildren.clear();

            int sourceChildClassOrPackageId = ((Integer) e.next()).intValue();

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
                                   Set uniqChildCache) {
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

                Integer key = Integer.valueOf(sourceChildClassOrPackageId);

                uniqChildCache.add(key);
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
