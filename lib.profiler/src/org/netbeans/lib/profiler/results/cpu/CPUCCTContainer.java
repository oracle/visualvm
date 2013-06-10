/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

import org.netbeans.lib.profiler.ProfilerLogger;
import org.netbeans.lib.profiler.global.InstrumentationFilter;
import org.netbeans.lib.profiler.results.cpu.cct.nodes.MethodCPUCCTNode;
import org.netbeans.lib.profiler.results.cpu.cct.nodes.RuntimeCPUCCTNode;
import org.netbeans.lib.profiler.results.cpu.cct.nodes.TimedCPUCCTNode;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.lib.profiler.results.RuntimeCCTNode;


/**
 * An instance of this class contains a presentation-time CCT for the given thread in the compact, flattened form, that is also fast
 * to generate and save/retrieve.
 * Can represent data only on the method level "view" AKA "aggregation level". The CPUCCTClassContainer subclass provides functionality
 * to create and represent data at class and package aggregation level. The AllThreadsMergedCPUCCTContainer also supports views. A single
 * instance of CPUCCTContainer or its subclass can represent data only on a single aggregation level.
 *
 * @author Tomas Hurka
 * @author Misha Dmitriev
 */
public class CPUCCTContainer {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final Logger LOGGER = Logger.getLogger(CPUCCTContainer.class.getName());

    // -- Data used for the compact representation of the CCT
    /* In the compactData array, data is packed in the following way:
     *  |---------------------------------------------------------------------------------------------------------
     *  | methodID | nCalls  |time0  | self  |time1        | self  |nbr. of subnodes | subnode0 |     | subnodeN |
     *  |          |         |       | time0 |(if 2 timers | time1 |                 | offset   | ... | offset   |
     *  |          |         |       |       | used)       |       |                 |          |     |          |
     *  |---------------------------------------------------------------------------------------------------------
     *    2 bytes   4 bytes   5 bytes 5 bytes  5 bytes      5 bytes  2 bytes           3 or 4 bytes depending on the size of compactData array
     */
    protected static final int OFS_METHODID = 0;
    protected static final int OFS_NCALLS = OFS_METHODID + 2;
    protected static final int OFS_TIME0 = OFS_NCALLS + 4;
    protected static final int OFS_SELFTIME0 = OFS_TIME0 + 5;
    protected static final int OFS_TIME1 = OFS_SELFTIME0 + 5;
    protected static final int OFS_SELFTIME1 = OFS_TIME1 + 5;
    protected static final int OFS_NSUBNODES1 = OFS_SELFTIME0 + 5;
    protected static final int OFS_NSUBNODES2 = OFS_SELFTIME1 + 5;
    protected static final int OFS_SUBNODE01 = OFS_NSUBNODES1 + 2;
    protected static final int OFS_SUBNODE02 = OFS_NSUBNODES2 + 2;
    protected static final int CHILD_OFS_SIZE_3 = 3;
    protected static final int CHILD_OFS_SIZE_4 = 4;

    // These are just the same-named xxxAbsCounts values converted into microseconds. So far used ONLY for informational purposes
    // (in "get internal statistics"), thus static is more or less tolerable (so far...)
    private static double timeInInjectedCodeInMS;
    private static double wholeGraphGrossTimeAbsInMS;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected CPUResultsSnapshot cpuResSnapshot;
    protected FlatProfileContainer cachedFlatProfile;
    protected PrestimeCPUCCTNode rootNode;
    protected String threadName;
    protected byte[] compactData;
    protected int[] invPerMethodId;

    // -- Temporary data used during flat profile generation
    protected long[] timePerMethodId0;
    protected long[] timePerMethodId1;
    protected long[] totalTimePerMethodId0;
    protected long[] totalTimePerMethodId1;
    protected Set methodsOnStack;
    protected boolean collectingTwoTimeStamps; // True if we collect two timestamps, absolute and thread CPU, for each method invocation
    protected boolean displayWholeThreadCPUTime; // True if we can calculate, and thus display, valid whole thread CPU time

    // Time spent in instrumentation, measured in counts
    protected double timeInInjectedCodeInAbsCounts;
    protected double timeInInjectedCodeInThreadCPUCounts;
    protected int childOfsSize = -1;
    protected int nodeSize; // Size of a single node above, not taking possible subnodeOffset fields into account
    protected int threadId;

    // -- Data that is supposed to be used for user information in various parts of the CPU results display
    // Gross time spent in the whole graph, measured in counts. It's measured by "starting the clock" when the root
    // method is entered, and "stopping the clock" when it exits. Time for hotswapping and on-line data processing
    // is factored out, so what is actually contained in wholeGraphGrossTime is (pure time + instrumentation time).
    // Note that independent of method timestamps collected, root method entry and exit events always have both
    // absolute and thread CPU time stamps.
    protected long wholeGraphGrossTimeAbs;
    protected long wholeGraphGrossTimeThreadCPU;

    // This is calculated as a sum of net times spent in all methods
    protected long wholeGraphNetTime0;
    protected long wholeGraphNetTime1;

    // This is calculated as the above gross time minus total time spent in instrumentation
    protected long wholeGraphPureTimeAbs;
    protected long wholeGraphPureTimeThreadCPU;
    private InstrumentationFilter filter;
    private PrestimeCPUCCTNodeFree reverseCCTRootNode;
//    private ProfilingSessionStatus status;
    private int[] nodeStack;
    private int childTotalNCalls;
    private int currentNodeStackSize;
    private int nodeStackPtr;

    // -- Temporary data used during reverse CCT generation
    private int selectedMethodId;

    // -- Temporary data used during above array generation
    private long childTotalTime0InTimerUnits;
    private long childTotalTime1InTimerUnits;
    long totalInvNo;

    private TimingAdjusterOld timingAdjuster;

    private MethodInfoMapper methodInfoMapper = MethodInfoMapper.DEFAULT;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public CPUCCTContainer(TimedCPUCCTNode rtRootNode, CPUResultsSnapshot cpuResSnapshot, MethodInfoMapper methodInfoMapper, TimingAdjusterOld timingAdjuster,
                           InstrumentationFilter usedFilter, int nNodes,
                           double[] threadActiveTimesInCounts, int threadId, String threadName) {
        this(cpuResSnapshot);

        this.threadId = threadId;
        this.threadName = threadName;

        this.methodInfoMapper = methodInfoMapper;
        this.timingAdjuster = timingAdjuster;
        this.filter = usedFilter;

        collectingTwoTimeStamps = cpuResSnapshot.isCollectingTwoTimeStamps();

        generateCompactData(rtRootNode, nNodes);

        calculateThreadActiveTimesInMS(threadActiveTimesInCounts);

        rootNode = new PrestimeCPUCCTNodeBacked(this, null, 0);

        if (rtRootNode.isRoot()) {
            rootNode.setThreadNode();
        }
    }

    protected CPUCCTContainer(CPUResultsSnapshot cpuResSnapshot) {
        this.cpuResSnapshot = cpuResSnapshot;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public CPUResultsSnapshot getCPUResSnapshot() {
        return cpuResSnapshot;
    }

    public int getChildOfsForNodeOfs(int nodeOfs, int childIdx) {
        if (childOfsSize == CHILD_OFS_SIZE_4) {
            return get4Bytes(nodeOfs + (collectingTwoTimeStamps ? OFS_SUBNODE02 : OFS_SUBNODE01) + (childOfsSize * childIdx));
        } else {
            return get3Bytes(nodeOfs + (collectingTwoTimeStamps ? OFS_SUBNODE02 : OFS_SUBNODE01) + (childOfsSize * childIdx));
        }
    }

    public boolean isCollectingTwoTimeStamps() {
        return collectingTwoTimeStamps;
    }

    public FlatProfileContainer getFlatProfile() {
        //    if (cachedFlatProfile == null) {
        //      generateFlatProfile();
        //    }
        //    return cachedFlatProfile;
        return generateFlatProfile();
    }

    public String[] getMethodClassNameAndSig(int methodId) {
        return cpuResSnapshot.getMethodClassNameAndSig(methodId, CPUResultsSnapshot.METHOD_LEVEL_VIEW);
    }

    // -- Methods for retrieving data for individual nodes
    public int getMethodIdForNodeOfs(int nodeOfs) {
        return get2Bytes(nodeOfs + OFS_METHODID);
    }

    public int getNCallsForNodeOfs(int nodeOfs) {
        return get4Bytes(nodeOfs + OFS_NCALLS);
    }

    public int getNChildrenForNodeOfs(int nodeOfs) {
        return get2Bytes(nodeOfs + (collectingTwoTimeStamps ? OFS_NSUBNODES2 : OFS_NSUBNODES1));
    }

    public PrestimeCPUCCTNode getReverseCCT(int methodId) {
        return generateReverseCCT(methodId);
    }

    public PrestimeCPUCCTNode getRootNode() {
        return rootNode;
    }

    public long getSelfTime0ForNodeOfs(int nodeOfs) {
        return get5Bytes(nodeOfs + OFS_SELFTIME0);
    }

    public long getSelfTime1ForNodeOfs(int nodeOfs) {
        return get5Bytes(nodeOfs + OFS_SELFTIME1);
    }

    public long getSleepTime0ForNodeOfs(int nodeOfs) {
        return 0;
    } // TODO [wait]

    public int getThreadId() {
        return threadId;
    }

    public String getThreadName() {
        return threadName;
    }

    // Provided for information purposes (that is, the "get internal statistics" action) only. Since this stuff
    // is not used in any real calculations, it's more or less tolerable so far to have it static.
    public static double getTimeInInjectedCodeForDisplayedThread() {
        return timeInInjectedCodeInMS;
    }

    public long getTotalTime0ForNodeOfs(int nodeOfs) {
        return get5Bytes(nodeOfs + OFS_TIME0);
    }

    public long getTotalTime1ForNodeOfs(int nodeOfs) {
        return get5Bytes(nodeOfs + OFS_TIME1);
    }

    public long getWaitTime0ForNodeOfs(int nodeOfs) {
        return 0;
    } // TODO [wait]

    public static double getWholeGraphGrossTimeAbsForDisplayedThread() {
        return wholeGraphGrossTimeAbsInMS;
    }

    public long getWholeGraphNetTime0() {
        return wholeGraphNetTime0;
    }

    public long getWholeGraphNetTime1() {
        return wholeGraphNetTime1;
    }

    public long getWholeGraphPureTimeAbs() {
        return wholeGraphPureTimeAbs;
    }

    public long getWholeGraphPureTimeThreadCPU() {
        return wholeGraphPureTimeThreadCPU;
    }

    public boolean canDisplayWholeGraphCPUTime() {
        return displayWholeThreadCPUTime;
    }

    public void readFromStream(DataInputStream in) throws IOException {
        threadId = in.readInt();
        threadName = in.readUTF();

        collectingTwoTimeStamps = in.readBoolean();

        int len = in.readInt();
        compactData = new byte[len];

        if (compactData.length > 0xFFFFFF) {
            childOfsSize = CHILD_OFS_SIZE_4;
        } else {
            childOfsSize = CHILD_OFS_SIZE_3;
        }

        in.readFully(compactData);

        nodeSize = in.readInt();

        wholeGraphGrossTimeAbs = in.readLong();
        wholeGraphGrossTimeThreadCPU = in.readLong();
        timeInInjectedCodeInAbsCounts = in.readDouble();
        timeInInjectedCodeInThreadCPUCounts = in.readDouble();
        wholeGraphPureTimeAbs = in.readLong();
        wholeGraphPureTimeThreadCPU = in.readLong();
        wholeGraphNetTime0 = in.readLong();
        wholeGraphNetTime1 = in.readLong();
        totalInvNo = in.readLong();
        displayWholeThreadCPUTime = in.readBoolean();

        rootNode = new PrestimeCPUCCTNodeBacked(this, null, 0);

        if (this.getMethodIdForNodeOfs(0) == 0) {
            rootNode.setThreadNode();
        }
    }

    // -- Serialization support
    public void writeToStream(DataOutputStream out) throws IOException {
        out.writeInt(threadId);
        out.writeUTF(threadName);

        out.writeBoolean(collectingTwoTimeStamps);

        out.writeInt(compactData.length);
        out.write(compactData);
        out.writeInt(nodeSize);

        out.writeLong(wholeGraphGrossTimeAbs);
        out.writeLong(wholeGraphGrossTimeThreadCPU);
        out.writeDouble(timeInInjectedCodeInAbsCounts);
        out.writeDouble(timeInInjectedCodeInThreadCPUCounts);
        out.writeLong(wholeGraphPureTimeAbs);
        out.writeLong(wholeGraphPureTimeThreadCPU);
        out.writeLong(wholeGraphNetTime0);
        out.writeLong(wholeGraphNetTime1);
        out.writeLong(totalInvNo);
        out.writeBoolean(displayWholeThreadCPUTime);
    }

    protected void setChildOfsForNodeOfs(int nodeOfs, int childIdx, int val) {
        if (childOfsSize == CHILD_OFS_SIZE_4) {
            store4Bytes(nodeOfs + (collectingTwoTimeStamps ? OFS_SUBNODE02 : OFS_SUBNODE01) + (childOfsSize * childIdx), val);
        } else {
            store3Bytes(nodeOfs + (collectingTwoTimeStamps ? OFS_SUBNODE02 : OFS_SUBNODE01) + (childOfsSize * childIdx), val);
        }
    }

    // -- Methods for setting data for individual nodes
    protected void setMethodIdForNodeOfs(int nodeOfs, int val) {
        store2Bytes(nodeOfs + OFS_METHODID, val);
    }

    protected void setNCallsForNodeOfs(int nodeOfs, int val) {
        store4Bytes(nodeOfs + OFS_NCALLS, val);
    }

    protected void setNChildrenForNodeOfs(int nodeOfs, int val) {
        store2Bytes(nodeOfs + (collectingTwoTimeStamps ? OFS_NSUBNODES2 : OFS_NSUBNODES1), val);
    }

    protected void setSelfTime0ForNodeOfs(int nodeOfs, long val) {
        store5Bytes(nodeOfs + OFS_SELFTIME0, val);
    }

    protected void setSelfTime1ForNodeOfs(int nodeOfs, long val) {
        store5Bytes(nodeOfs + OFS_SELFTIME1, val);
    }

    protected void setSleepTime0ForNodeOfs(int dataOfs, long waitTime0) {
    } // TODO [sleep should be stored separately in future versions]

    protected void setTotalTime0ForNodeOfs(int nodeOfs, long val) {
        store5Bytes(nodeOfs + OFS_TIME0, val);
    }

    protected void setTotalTime1ForNodeOfs(int nodeOfs, long val) {
        store5Bytes(nodeOfs + OFS_TIME1, val);
    }

    protected void setWaitTime0ForNodeOfs(int dataOfs, long waitTime0) {
    } // TODO [wait should be stored separately in future versions]

    protected void addFlatProfTimeForNode(int dataOfs) {
        int methodId = getMethodIdForNodeOfs(dataOfs);
        Integer methodIdInt = new Integer(methodId);
        boolean isRecursiveCall = methodsOnStack.contains(methodIdInt);
        
        if (methodId >= invPerMethodId.length) {
            LOGGER.log(Level.WARNING, "Method ID ({0}) out of bounds ({1})", new Object[]{methodId, invPerMethodId.length});
            return;
        }
        int nChildren = getNChildrenForNodeOfs(dataOfs);

        if (nChildren > 0) {
            if (!isRecursiveCall) {
                methodsOnStack.add(methodIdInt);
            }
            for (int i = 0; i < nChildren; i++) {
                int childOfs = getChildOfsForNodeOfs(dataOfs, i);
                addFlatProfTimeForNode(childOfs);
            }
            if (!isRecursiveCall) {
                methodsOnStack.remove(methodIdInt);                
            }
        }

        timePerMethodId0[methodId] += getSelfTime0ForNodeOfs(dataOfs);
        if (!isRecursiveCall) {
            totalTimePerMethodId0[methodId] += getTotalTime0ForNodeOfs(dataOfs);
        }
        if (collectingTwoTimeStamps) {
            timePerMethodId1[methodId] += getSelfTime1ForNodeOfs(dataOfs);
            if (!isRecursiveCall) {
                totalTimePerMethodId1[methodId] += getTotalTime1ForNodeOfs(dataOfs);
            }
        }

        invPerMethodId[methodId] += getNCallsForNodeOfs(dataOfs);
    }

    protected void addToReverseCCT(PrestimeCPUCCTNodeFree reverseNode, int methodId) {
        selectedMethodId = methodId;
        reverseCCTRootNode = reverseNode;

        currentNodeStackSize = 320;
        nodeStack = new int[currentNodeStackSize];
        nodeStackPtr = 0;
        checkStraightGraphNode(0);

        nodeStack = null; // Free memory
        reverseCCTRootNode = null; // Ditto
    }

    /**
     * Walk all the elements of the main graph, looking for nodes with selectedMethodId signature.
     * Whenever one is found, add its path, in reversed form, to the rootNode.
     * When path is added, same-named nodes are merged until the first pair of different nodes is found.
     */
    protected void checkStraightGraphNode(int dataOfs) {
        if (nodeStackPtr >= currentNodeStackSize) {
            int[] newNodeStack = new int[currentNodeStackSize * 2];
            System.arraycopy(nodeStack, 0, newNodeStack, 0, currentNodeStackSize);
            nodeStack = newNodeStack;
            currentNodeStackSize = currentNodeStackSize * 2;
        }

        nodeStack[nodeStackPtr++] = dataOfs;

        if (getMethodIdForNodeOfs(dataOfs) == selectedMethodId) {
            addReversePath();
        }

        int nChildren = getNChildrenForNodeOfs(dataOfs);

        for (int i = 0; i < nChildren; i++) {
            checkStraightGraphNode(getChildOfsForNodeOfs(dataOfs, i));
        }

        nodeStackPtr--;
    }

    protected FlatProfileContainer generateFlatProfile() {
        preGenerateFlatProfile();

        addFlatProfTimeForNode(0);

        return postGenerateFlatProfile();
    }

    protected PrestimeCPUCCTNodeFree generateReverseCCT(int methodId) {
        selectedMethodId = methodId;

        currentNodeStackSize = 320;
        nodeStack = new int[currentNodeStackSize];
        nodeStackPtr = 0;
        checkStraightGraphNode(0);

        PrestimeCPUCCTNodeFree ret = reverseCCTRootNode;

        nodeStack = null; // Free memory
        reverseCCTRootNode = null; // Ditto

        return ret;
    }

    protected int get2Bytes(int ofs) {
        return (((int) compactData[ofs] & 0xFF) << 8) | ((int) compactData[ofs + 1] & 0xFF);
    }

    protected int get3Bytes(int ofs) {
        return (((int) compactData[ofs++] & 0xFF) << 16) | (((int) compactData[ofs++] & 0xFF) << 8)
               | ((int) compactData[ofs++] & 0xFF);
    }

    protected int get4Bytes(int ofs) {
        return (((int) compactData[ofs++] & 0xFF) << 24) | (((int) compactData[ofs++] & 0xFF) << 16)
               | (((int) compactData[ofs++] & 0xFF) << 8) | ((int) compactData[ofs++] & 0xFF);
    }

    protected long get5Bytes(int ofs) {
        return (((long) compactData[ofs++] & 0xFF) << 32) | (((long) compactData[ofs++] & 0xFF) << 24)
               | (((long) compactData[ofs++] & 0xFF) << 16) | (((long) compactData[ofs++] & 0xFF) << 8)
               | ((long) compactData[ofs++] & 0xFF);
    }

    protected FlatProfileContainer postGenerateFlatProfile() {
        FlatProfileContainer fpc = new FlatProfileContainerBacked(this, timePerMethodId0, timePerMethodId1, 
                totalTimePerMethodId0, totalTimePerMethodId1, invPerMethodId, timePerMethodId0.length);

        timePerMethodId0 = timePerMethodId1 = null;
        totalTimePerMethodId0 = totalTimePerMethodId1 = null;
        invPerMethodId = null;
        methodsOnStack = null;

        return fpc;
    }

    protected void preGenerateFlatProfile() {
        int totalMethods = cpuResSnapshot.getNInstrMethods();
        timePerMethodId0 = new long[totalMethods];
        totalTimePerMethodId0 = new long[totalMethods];

        if (collectingTwoTimeStamps) {
            timePerMethodId1 = new long[totalMethods];
            totalTimePerMethodId1 = new long[totalMethods];
        }

        invPerMethodId = new int[totalMethods];
        timePerMethodId0[0] = -1; // 0th element is a hidden "Thread" quazi-method. This prevents exposing it in a pathological case when all times are zero.
        totalTimePerMethodId0[0] = -1; // 0th element is a hidden "Thread" quazi-method. This prevents exposing it in a pathological case when all times are zero.
        methodsOnStack = new HashSet();
    }

    // -- Utility methods, not interesting enough to place earlier in the code
    protected void store2Bytes(int ofs, int data) {
        compactData[ofs] = (byte) ((data >> 8) & 0xFF);
        compactData[ofs + 1] = (byte) ((data) & 0xFF);
    }

    protected void store3Bytes(int ofs, int data) {
        int curPos = ofs;
        compactData[curPos++] = (byte) ((data >> 16) & 0xFF);
        compactData[curPos++] = (byte) ((data >> 8) & 0xFF);
        compactData[curPos++] = (byte) ((data) & 0xFF);
    }

    protected void store4Bytes(int ofs, int data) {
        int curPos = ofs;
        compactData[curPos++] = (byte) ((data >> 24) & 0xFF);
        compactData[curPos++] = (byte) ((data >> 16) & 0xFF);
        compactData[curPos++] = (byte) ((data >> 8) & 0xFF);
        compactData[curPos++] = (byte) ((data) & 0xFF);
    }

    protected void store5Bytes(int ofs, long data) {
        int curPos = ofs;
        compactData[curPos++] = (byte) ((data >> 32) & 0xFF);
        compactData[curPos++] = (byte) ((data >> 24) & 0xFF);
        compactData[curPos++] = (byte) ((data >> 16) & 0xFF);
        compactData[curPos++] = (byte) ((data >> 8) & 0xFF);
        compactData[curPos++] = (byte) ((data) & 0xFF);
    }

    private void addChild(AddChildLocalVars locals) {
        if ((locals.node == null) || (locals.parent == null)) {
            return;
        }

        locals.filterStatus = locals.node.getFilteredStatus();

        if (!(locals.node instanceof MethodCPUCCTNode)) {
            locals.filterStatus = TimedCPUCCTNode.FILTERED_YES;
        }

        switch (locals.filterStatus) {
            case TimedCPUCCTNode.FILTERED_YES: {
                locals.compParent = locals.parent;

                break;
            }
            case TimedCPUCCTNode.FILTERED_MAYBE: {
                if (locals.node instanceof MethodCPUCCTNode) {
                    methodInfoMapper.lock(false);
                    try {
                        locals.className = methodInfoMapper.getInstrMethodClass(((MethodCPUCCTNode) locals.node).getMethodId()).replace('.', '/'); // NOI18N
                        if (!filter.passesFilter(locals.className)) {
                            locals.compParent = locals.parent;
                        } else {
                            locals.newChild = (TimedCPUCCTNode) locals.node.clone();
                            locals.compParent = locals.newChild;
                        }
                    } finally {
                        methodInfoMapper.unlock();
                    }
                } else {
                    locals.compParent = locals.parent;
                }

                break;
            }
            case TimedCPUCCTNode.FILTERED_NO: {
                locals.existingChild = MethodCPUCCTNode.Locator.locate(((MethodCPUCCTNode) locals.node).getMethodId(),
                                                                                 locals.parent.getChildren());

                if (locals.existingChild == null) {
                    locals.newChild = (TimedCPUCCTNode) locals.node.clone();
                    locals.compParent = locals.newChild;
                } else {
                    locals.newChild = null;
                    locals.existingChild.addNCalls(locals.node.getNCalls());
                    locals.existingChild.addNCallsDiff(locals.node.getNCallsDiff());
                    locals.existingChild.addNetTime0(locals.node.getNetTime0());
                    locals.existingChild.addNetTime1(locals.node.getNetTime1());
                    locals.existingChild.addSleepTime0(locals.node.getSleepTime0());
                    locals.existingChild.addWaitTime0(locals.node.getWaitTime0());
                    locals.compParent = locals.existingChild;
                }

                break;
            }
            default:ProfilerLogger.warning("Unknown filtered status (" + locals.filterStatus + ") for " + locals.node); // NOI18N
        }

        locals.nChildren = (locals.node.getChildren() != null) ? locals.node.getChildren().length : 0;

        for (int i = 0; i < locals.nChildren; i++) {
            addChild(new AddChildLocalVars((TimedCPUCCTNode) locals.node.getChildren()[i], locals.compParent));
        }

        if (locals.newChild != null) {
            locals.parent.attachNodeAsChild(locals.newChild);
        } else if (locals.compParent == locals.parent) { // filtered-out node
            if (!locals.parent.isRoot()) { // no propagation of filtered-out data to the Thread level node
                locals.parent.addNetTime0(locals.node.getNetTime0());
                locals.parent.addSleepTime0(locals.node.getSleepTime0());
                locals.parent.addWaitTime0(locals.node.getWaitTime0());
                locals.parent.addNCallsDiff(locals.node.getNCalls());

                if (collectingTwoTimeStamps) {
                    locals.parent.addNetTime1(locals.node.getNetTime1());
                }
            } else {
                //        threadTimeCompensation0 += node.getNetTime0();
                //        if (collectingTwoTimeStamps) {
                //          threadTimeCompensation1 += node.getNetTime1();
                //        }
            }
        }
    }
    
    private static final class AddChildLocalVars {
        private final TimedCPUCCTNode node;
        private final TimedCPUCCTNode parent;
        private TimedCPUCCTNode compParent;
        private TimedCPUCCTNode newChild;
        private int filterStatus;
        private int nChildren;
        private MethodCPUCCTNode existingChild;
        private String className;
        
        AddChildLocalVars(TimedCPUCCTNode n, TimedCPUCCTNode p) {
            node = n;
            parent = p;
        }
    }

    /**
     * Add the whole reverse path contained in the nodeStack to the reverse call tree, merging nodes where appropriate.
     * Most of the complexity of the code is due to handling of the intermediate "from" nodes.
     */
    private void addReversePath() {
        PrestimeCPUCCTNodeFree curNode = null; // This is effectively a node above the root node - which is non-existent
        int stackTopIdx = nodeStackPtr - 1;

        for (int i = stackTopIdx; i >= 0; i--) {
            int sourceNodeOfs = nodeStack[i];
            int sourceNodeId = getMethodIdForNodeOfs(sourceNodeOfs);

            if (sourceNodeId == 0) {
                return; // It doesn't make sense to add "Thread" nodes to the reverse tree
            }

            boolean matchingChildFound = false;

            if (i < stackTopIdx) { // sourceNodeOfs is some intermediate node

                PrestimeCPUCCTNodeFree[] curNodeChildren = (PrestimeCPUCCTNodeFree[]) curNode.getChildren();

                if (curNodeChildren != null) {
                    for (int j = 0; j < curNodeChildren.length; j++) {
                        if (curNodeChildren[j].getMethodId() == sourceNodeId) {
                            curNode = curNodeChildren[j];

                            if (curNode.isContextCallsNode()) { // Skip the "context calls" node if it exists

                                int prevSourceNodeOfs = nodeStack[i + 1];
                                mergeBySelfTime(curNode, prevSourceNodeOfs);
                                curNode = (PrestimeCPUCCTNodeFree) curNode.getChildren()[0];
                            }

                            mergeBySelfTime(curNode, sourceNodeOfs);
                            matchingChildFound = true;

                            break;
                        }
                    }
                }
            } else { // sourceNode is the topmost stack node
                curNode = reverseCCTRootNode;

                if (curNode == null) {
                    curNode = createChildlessCopyBySelfTime(sourceNodeOfs);
                    reverseCCTRootNode = curNode;
                } else {
                    mergeBySelfTime(curNode, sourceNodeOfs);
                }

                matchingChildFound = true;
            }

            if (!matchingChildFound) { // sourceNode may only be an intermediate node

                PrestimeCPUCCTNodeFree newNode = createChildlessCopyBySelfTime(sourceNodeOfs);
                PrestimeCPUCCTNodeFree[] curNodeChildren = (PrestimeCPUCCTNodeFree[]) curNode.getChildren();

                if (curNodeChildren != null) {
                    // For the given node, add an intermediate "context calls" node. If previously there was just one child,
                    // insert another "context calls" node for it.
                    int prevSourceNodeOfs = nodeStack[i + 1];

                    if (curNodeChildren.length == 1) { // Insert a context node for the already existing single child

                        PrestimeCPUCCTNodeFree origFirstChild = curNodeChildren[0];
                        PrestimeCPUCCTNodeFree ccNode = curNode.createChildlessCopy();
                        subtractNodeDataBySelfTime(ccNode, prevSourceNodeOfs); // Undo the results of merging with the parent of sourceNode
                        ccNode.setMethodId(origFirstChild.getMethodId());
                        ccNode.setContextCallsNode();
                        curNodeChildren[0] = ccNode;
                        ccNode.parent = curNode;
                        ccNode.addChild(origFirstChild);
                        origFirstChild.parent = ccNode;
                    }

                    PrestimeCPUCCTNodeFree ccNode = createChildlessCopyBySelfTime(prevSourceNodeOfs);
                    ccNode.setMethodId(getMethodIdForNodeOfs(sourceNodeOfs));
                    ccNode.setContextCallsNode();
                    curNode.addChild(ccNode);
                    ccNode.parent = curNode;
                    curNode = ccNode;
                }

                curNode.addChild(newNode);
                newNode.parent = curNode;
                curNode = newNode;
            }
        }
    }

    /**
     * After presentation-time CCT is generated, calculate various special time values stored in this instance
     */
    private void calculateThreadActiveTimesInMS(double[] threadActiveTimesInCounts) {
        //!!! Delete this comment after deciding what to do with the whole issue.
        // In the code below, '+=' is caused by the fact that this method may be called multiple times when in getRootNode() we
        // generate CCTs for all threads. In the default single-thread case the real value is just added to the initial zero value.
        wholeGraphGrossTimeAbs = (long) threadActiveTimesInCounts[0];
        wholeGraphGrossTimeThreadCPU = (long) threadActiveTimesInCounts[1];
        timeInInjectedCodeInAbsCounts = threadActiveTimesInCounts[2];
        timeInInjectedCodeInThreadCPUCounts = threadActiveTimesInCounts[3];

        wholeGraphGrossTimeAbsInMS += ((wholeGraphGrossTimeAbs * 1000.0) / timingAdjuster.getInstrTimingData().timerCountsInSecond0);
        timeInInjectedCodeInMS += ((timeInInjectedCodeInAbsCounts * 1000.0) / timingAdjuster.getInstrTimingData().timerCountsInSecond0);

        // Note that here we have to use status.timerCountsInSecond[x] explicitly instead of timerCountsInSecond0/1 (which may correspond to wrong time type)
        wholeGraphPureTimeAbs += (int) ((((double) wholeGraphGrossTimeAbs - timeInInjectedCodeInAbsCounts) * 1000000) / timingAdjuster.getInstrTimingData().timerCountsInSecond1);

        //System.err.println("*** wholeGraphTimeAbs gross (cnts) = " + wholeGraphGrossTimeAbs + ", pure (mcs) = " + wholeGraphPureTimeAbs);
        if (wholeGraphGrossTimeThreadCPU > 0) { // Otherwise it means we couldn't calculate it and it shouldn't be displayed
            displayWholeThreadCPUTime = true;
            wholeGraphPureTimeThreadCPU += (int) ((((double) wholeGraphGrossTimeThreadCPU - timeInInjectedCodeInThreadCPUCounts) * 1000000) / timingAdjuster.getInstrTimingData().timerCountsInSecond1);

            //System.err.println("*** wholeGraphTimeThreadCPU gross (cnts) = " + wholeGraphGrossTimeThreadCPU + ", pure (mcs) = " + wholeGraphPureTimeThreadCPU);
            //System.err.println("*** timeInInjectedCode (mcs) = " + (timeInInjectedCodeInAbsCounts * 1000000 / status.timerCountsInSecond[0]));
        } else {
            displayWholeThreadCPUTime = false;
        }

        // Take measures in case timer's low resolution has caused funny results
        if (wholeGraphPureTimeAbs < 0) {
            wholeGraphPureTimeAbs = 0;
        }

        if (wholeGraphPureTimeThreadCPU < 0) {
            wholeGraphPureTimeThreadCPU = 0;
        }

        wholeGraphNetTime0 += get5Bytes(0 + OFS_TIME0);

        if (collectingTwoTimeStamps) {
            wholeGraphNetTime1 += get5Bytes(0 + OFS_TIME1);
        }
    }

    private PrestimeCPUCCTNodeFree createChildlessCopyBySelfTime(int sourceNodeDataOfs) {
        PrestimeCPUCCTNodeFree node = new PrestimeCPUCCTNodeFree(this, null, getMethodIdForNodeOfs(sourceNodeDataOfs));
        mergeBySelfTime(node, sourceNodeDataOfs);

        return node;
    }

    //  private long threadTimeCompensation0, threadTimeCompensation1;
    private TimedCPUCCTNode filterCCT(final TimedCPUCCTNode rootNode) {
        TimedCPUCCTNode newRoot = (TimedCPUCCTNode) rootNode.clone();

        //    threadTimeCompensation0 = threadTimeCompensation1 = 0;
        int nChildren = (rootNode.getChildren() != null) ? rootNode.getChildren().length : 0;

        for (int i = 0; i < nChildren; i++) {
            addChild(new AddChildLocalVars((TimedCPUCCTNode) rootNode.getChildren()[i], newRoot));
        }

        //    long time0, time1;
        //    time0 = newRoot.getNetTime0() - threadTimeCompensation0;
        newRoot.setNetTime0(0);

        if (collectingTwoTimeStamps) {
            //      time1 = newRoot.getNetTime1() - threadTimeCompensation1;
            newRoot.setNetTime1(0);
        }

        return newRoot;
    }

    private void generateCompactData(TimedCPUCCTNode rootNode, int nNodes) {
        nodeSize = collectingTwoTimeStamps ? OFS_SUBNODE02 : OFS_SUBNODE01;
        childOfsSize = CHILD_OFS_SIZE_3;

        int arraySize = (nodeSize * nNodes) + (childOfsSize * (nNodes - 1)); // For each node, except the root one, there is a parent node that references it with childOfsSize bytes long offset

        if (arraySize > 0xFFFFFF) { // compactData is to big to use 3 bytes subnode offsets
            childOfsSize = CHILD_OFS_SIZE_4;
            arraySize = (nodeSize * nNodes) + (childOfsSize * (nNodes - 1));
        }

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST, "generateCompact data: nNodes {0}", nNodes); // NOI18N
            LOGGER.log(Level.FINEST, "generateCompact data: node size {0}", nodeSize); // NOI18N
            LOGGER.log(Level.FINEST, "generateCompact data: array size {0}", arraySize); // NOI18N
            LOGGER.log(Level.FINEST, "generateCompact data: child offset {0}", childOfsSize); // NOI18N
        }

        compactData = new byte[arraySize];

        rootNode = filterCCT(rootNode);
        generateMirrorNode(new GenerateMirrorNodeLocalVars(rootNode, 0));
    }

    /**
     * Generates an equivalent of rtNode in the compact data. Returns the offset right after the last generated node, which
     * is this node if it has no children, or the last recursive child of this node.
     */
    private int generateMirrorNode(GenerateMirrorNodeLocalVars locals) {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST, "Generate mirror node for ofs: {0}, node: {1}", new Object[]{locals.dataOfs, locals.rtNode}); // NOI18N
        }

        generateNodeBase(locals.rtNode, locals.dataOfs);

        totalInvNo += locals.rtNode.getNCalls();

        locals.nodeChildren = locals.rtNode.getChildren();

        locals.nChildren = (locals.nodeChildren != null) ? locals.nodeChildren.length : 0;
        locals.nextNodeOfs = locals.dataOfs + nodeSize + (locals.nChildren * childOfsSize);

        locals.nCallsFromThisNode += locals.rtNode.getNCallsDiff();

        if (locals.nChildren > 0) {
            locals.childCounter = 0;

            for (locals.i = 0; locals.i < locals.nChildren; locals.i++) {
                locals.aNode = (RuntimeCPUCCTNode)locals.nodeChildren[locals.i];

                if (locals.aNode instanceof MethodCPUCCTNode) { // TODO replace "instanceof" by a visitor implementation
                    setChildOfsForNodeOfs(locals.dataOfs, locals.childCounter, locals.nextNodeOfs);
                    locals.nextNodeOfs = generateMirrorNode(new GenerateMirrorNodeLocalVars((MethodCPUCCTNode) locals.aNode, locals.nextNodeOfs));

                    locals.thisNodeTotalTime0InTimerUnits += childTotalTime0InTimerUnits; // Completely uncleansed time

                    if (collectingTwoTimeStamps) {
                        locals.thisNodeTotalTime1InTimerUnits += childTotalTime1InTimerUnits; // Ditto
                    }

                    locals.nCallsFromThisNode += ((MethodCPUCCTNode) locals.aNode).getNCalls();
                    locals.totalNCallsFromThisNode += childTotalNCalls;
                    locals.childCounter++;
                }
            }
        }

        // Calculate cleansed self time
        /* PROTOTYPE [wait]
           long time = (long) (((double) rtNode.netTime0 - rtNode.waitTime0 - rtNode.nCalls * timingData.methodEntryExitInnerTime0 - nCallsFromThisNode * timingData.methodEntryExitOuterTime0) * 1000000 / timingData.timerCountsInSecond0);
         */
        locals.time = (long) timingAdjuster.adjustTime(locals.rtNode.getNetTime0(), locals.rtNode.getNCalls()+locals.rtNode.getNCallsDiff(), locals.nCallsFromThisNode, false);

        //    (long) (((double) rtNode.getNetTime0() - rtNode.getNCalls() * timingData.methodEntryExitInnerTime0
        //      - nCallsFromThisNode * timingData.methodEntryExitOuterTime0) * 1000000 / timingData
        //      .timerCountsInSecond0);
        if (locals.time < 0) {
            // It may happen that for some very short methods the resulting time, after subtracting the instrumentation time, gets negative.
            // When I calculated some concrete results using the (now-commented) code below, it appeared that for such methods the net
            // time per call, in timer counts, is in the order of -0.1.. -0.2. In other words, it's a very small error caused by the
            // hi-res timer (on Windows at least) being still too coarse-grain if just a few machine instructions are to be measured; and
            // possibly insufficient precision of our advanced determination of instrumentation time.
            // Setting the result to zero seems reasonable in this situation.
            //if (nCallsFromThisNode == 0) {
            // Net time per call, in counts
            //double ntpc = ((double)cgNode.netTime - thisNode.nCalls * methodEntryExitInnerTime - nCallsFromThisNode * methodEntryExitOuterTime) / thisNode.nCalls;
            //System.out.println("*** N: id= " + thisNode.methodId + ", cls= " + thisNode.nCalls + ", netTime= " + cgNode.netTime + ", nCFrom= " + nCallsFromThisNode + ", res = " + thisNode.netTime + ", ntpc = " + ntpc);
            //}
            locals.time = 0;
        }

        setSelfTime0ForNodeOfs(locals.dataOfs, locals.time);
        setWaitTime0ForNodeOfs(locals.dataOfs, locals.rtNode.getWaitTime0());
        setSleepTime0ForNodeOfs(locals.dataOfs, locals.rtNode.getSleepTime0());

        locals.thisNodeTotalTime0InTimerUnits += locals.rtNode.getNetTime0(); // Uncleansed time for this node and all its children
        childTotalTime0InTimerUnits = locals.thisNodeTotalTime0InTimerUnits; // It will be effectively returned by this method
                                                                      // Calculate cleansed total time

        locals.time = (long) timingAdjuster.adjustTime(locals.thisNodeTotalTime0InTimerUnits, locals.rtNode.getNCalls()+locals.totalNCallsFromThisNode, locals.totalNCallsFromThisNode,
                                                   false);

        //    time = (long) (((double) thisNodeTotalTime0InTimerUnits - rtNode.getNCalls()* timingData.methodEntryExitInnerTime0
        //      - totalNCallsFromThisNode * timingData.methodEntryExitCallTime0) * 1000000 / timingData
        //      .timerCountsInSecond0);
        if (locals.time < 0) {
            //System.out.println("*** Negative: " + thisNode.totalTime0 + ", thisNCalls = " + thisNode.nCalls + ", fromNCalls = " + totalNCallsFromThisNode);
            locals.time = 0;
        }

        setTotalTime0ForNodeOfs(locals.dataOfs, locals.time);

        if (collectingTwoTimeStamps) {
            // Calculate cleansed self time
            locals.time = (long) timingAdjuster.adjustTime(locals.rtNode.getNetTime1(), locals.rtNode.getNCalls()+locals.rtNode.getNCallsDiff(), locals.nCallsFromThisNode, true);

            //      time = (long) (((double) rtNode.getNetTime1()
            //        - rtNode.getNCalls() * timingData.methodEntryExitInnerTime1
            //        - nCallsFromThisNode * timingData.methodEntryExitOuterTime1) * 1000000 / timingData
            //        .timerCountsInSecond1);
            if (locals.time < 0) {
                locals.time = 0;
            }

            setSelfTime1ForNodeOfs(locals.dataOfs, locals.time);
            locals.thisNodeTotalTime1InTimerUnits += locals.rtNode.getNetTime1();
            childTotalTime1InTimerUnits = locals.thisNodeTotalTime1InTimerUnits; // It will be effectively returned by this method
                                                                          // Calculate cleansed total time

            locals.time = (long) timingAdjuster.adjustTime(locals.thisNodeTotalTime1InTimerUnits, locals.rtNode.getNCalls()+locals.totalNCallsFromThisNode,
                                                       locals.totalNCallsFromThisNode, true);

            //      time = (long) (((double) thisNodeTotalTime1InTimerUnits - rtNode.getNCalls() * timingData.methodEntryExitInnerTime0
            //        - totalNCallsFromThisNode * timingData.methodEntryExitCallTime1) * 1000000 / timingData
            //        .timerCountsInSecond1);
            if (locals.time < 0) {
                locals.time = 0;
            }

            setTotalTime1ForNodeOfs(locals.dataOfs, locals.time);
        }

        childTotalNCalls = locals.totalNCallsFromThisNode + locals.rtNode.getNCalls(); // It will be effectively returned by this method

        return locals.nextNodeOfs;
    }

    private static class GenerateMirrorNodeLocalVars {
        private final TimedCPUCCTNode rtNode;
        private final int dataOfs;
        private long thisNodeTotalTime0InTimerUnits;
        private long thisNodeTotalTime1InTimerUnits;
        private int nCallsFromThisNode;
        private int totalNCallsFromThisNode;
        private RuntimeCCTNode[] nodeChildren;
        private int nChildren;
        private int nextNodeOfs;
        private int childCounter;
        private int i;
        private RuntimeCPUCCTNode aNode;
        private long time;
        
        private GenerateMirrorNodeLocalVars(TimedCPUCCTNode node, int off) {
            rtNode = node;
            dataOfs = off;
        } 
    }
    
    private void generateNodeBase(TimedCPUCCTNode rtNode, int nodeDataOfs) {
        int methodId = (rtNode instanceof MethodCPUCCTNode) ? ((MethodCPUCCTNode) rtNode).getMethodId() : 0;
        int nCalls = rtNode.getNCalls();
        int nChildren = (rtNode.getChildren() != null) ? rtNode.getChildren().length : 0;

        setMethodIdForNodeOfs(nodeDataOfs, methodId);
        setNCallsForNodeOfs(nodeDataOfs, nCalls);
        setNChildrenForNodeOfs(nodeDataOfs, nChildren);
    }

    private void mergeBySelfTime(PrestimeCPUCCTNodeFree curNode, int sourceNodeDataOfs) {
        curNode.addNCalls(getNCallsForNodeOfs(sourceNodeDataOfs));
        curNode.addTotalTime0(getSelfTime0ForNodeOfs(sourceNodeDataOfs));

        if (collectingTwoTimeStamps) {
            curNode.addTotalTime1(getSelfTime1ForNodeOfs(sourceNodeDataOfs));
        }

        curNode.addWaitTime0(getWaitTime0ForNodeOfs(sourceNodeDataOfs));
        curNode.addSleepTime0(getSleepTime0ForNodeOfs(sourceNodeDataOfs));
    }

    private void subtractNodeDataBySelfTime(PrestimeCPUCCTNodeFree curNode, int sourceNodeDataOfs) {
        curNode.addNCalls(-getNCallsForNodeOfs(sourceNodeDataOfs));
        curNode.addTotalTime0(-getSelfTime0ForNodeOfs(sourceNodeDataOfs));

        if (collectingTwoTimeStamps) {
            curNode.addTotalTime1(-getSelfTime1ForNodeOfs(sourceNodeDataOfs));
        }

        curNode.addWaitTime0(-getWaitTime0ForNodeOfs(sourceNodeDataOfs)); // TODO: [wait] what is this?
        curNode.addSleepTime0(-getSleepTime0ForNodeOfs(sourceNodeDataOfs)); // TODO: [wait] what is this?
    }
}
