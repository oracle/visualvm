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

import org.netbeans.lib.profiler.results.ResultsSnapshot;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.WeakHashMap;
import java.util.logging.Level;
import org.netbeans.lib.profiler.results.CCTNode;
import org.netbeans.lib.profiler.results.FilterSortSupport;


/**
 * Management of the CPU profiling results snapshot.
 * A CPU snapshot is created from the runtime CCTs for all profiled threads, that are provided by CPUCallGraphBuilder.
 * Initially it contains only these same (logically) CCTs (in instances of CPUCCTContainer), but in compact "flattened"
 * format that takes less space in memory, and represented in microseconds, cleansed time.
 * When the user starts to open CCT nodes for the given thread, the relevant objects (PrestimeCPUCCTNodes) are generated lazily
 * for touched nodes out of the initial compact representation. This is done via a PrestimeCPUCCTNode object keeping a pointer
 * into the compact tree representation where its data is located, from which the data for its children can be located, etc.
 * When the user requests data in different "view" (switching say from method-level to class- (or package-) level aggregation),
 * the relevant compact representation for all threads is generated out of the initial compact representation.
 * <p/>
 * Has an API for
 * - returning the root of the all-threads CCT. The node objects themselves take care of lazy construction of sub-nodes
 * out of the compact representation, when needed
 * - generation of the accumulated time per method (flat profile) out of the CCT.
 *
 * @author Misha Dmitriev
 */
public class CPUResultsSnapshot extends ResultsSnapshot {
    /**************************************************************************
    +------------------------------------------------------------------------------+
    | Profiler CPU snapshot format description                                     |
    +------------------------------------------------------------------------------+
    int         version
    long        timestamp
    long        duration
    boolean     measure thread time?
    int         #instrumented methods
    ===> (for #instrumented methods)
    string      class name
    string      method name
    string      signature
    <===
    int         #threads
    ===> (for #threads)
    int         thread id
    string      thread name
    boolean     measure thread time?
    int         compact data length
    byte[]      compact data with the given length
    int         node size
    long        wholeGraphGrossTimeAbs
    long        wholeGraphGrossTimeThreadCPU
    double      timeInInjectedCodeInAbsCounts
    double      timeInInjectedCodeInThreadCPUCounts
    long        wholeGraphPureTimeAbs
    long        wholeGraphPureTimeThreadCPU
    long        wholeGraphNetTime0
    long        wholeGraphNetTime1
    long        totalInvNo
    boolean     displayWholeThreadCPUTime
    <===


    +------------------------------------------------------------------------------+
    | CPU compact data format description                                          |
    +------------------------------------------------------------------------------+
    0-1     2 bytes         methodID
    2-5     4 bytes         nCalls
    6-10    5 bytes         time0
    11-15   5 bytes         self time0
    16-20   5 bytes         time1(if measuring thread time)
    21-25   5 bytes         self time1(if measuring thread time)
    26-27   2 bytes         # of subnodes
    28-30   3 bytes         if compact data size <= 16777215
    28-31   4 bytes         if compact data size > 16777215
    ***************************************************************************/

    /**
     * This exception just indicates that snapshot can't be created because no data is available
     */
    public static class NoDataAvailableException extends Exception {
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String CPU_MSG = ResourceBundle.getBundle("org.netbeans.lib.profiler.results.cpu.Bundle").getString("CPUResultsSnapshot_CpuMsg"); // NOI18N
                                                                                           // -----

    // Views-related stuff
    public static final int METHOD_LEVEL_VIEW = 0;
    public static final int CLASS_LEVEL_VIEW = 1;
    public static final int PACKAGE_LEVEL_VIEW = 2;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected Map threadIdMap;
    protected CPUCCTContainer[] allThreadsMergedCCTContainers; // [method|class|package aggregation level] -> CPUCCTContainer Per-view representation of all threads merged CCT containers

    // <class name>,<method name>,<method signature> triplets corresponding to methodIds in the profiling results
    // The 0th entry is reserved for a special "Threads" node
    // instrMethodClasses consists of 3 arrays, each per different "view"
    protected String[][] instrMethodClassesViews;
    protected String[] instrMethodNames;
    protected String[] instrMethodSignatures;
    protected PrestimeCPUCCTNode[] rootNode; // Per-view root nodes
    protected CPUCCTContainer[][] threadCCTContainers; // [method|class|package aggregation level][0-nThreads] -> CPUCCTContainer
    protected boolean collectingTwoTimeStamps;

    // Number of instrumented methods - may be smaller than the size of the above arrays
    protected int nInstrMethods;
    
    private final Map<CCTNode, FilterSortSupport.Configuration> sortInfos = new WeakHashMap();

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public CPUResultsSnapshot() { // No-arg constructor needed for above serialization methods to work
        super();
        threadIdMap = new HashMap();
    }

    public CPUResultsSnapshot(long beginTime, long timeTaken, CPUCCTProvider cctProvider, boolean collectingTwoTimestamps,
                              String[] instrClassNames, String[] instrMethodNames, String[] instrMethodSigs, int nInstrMethods)
                       throws NoDataAvailableException {
        super(beginTime, timeTaken);

        this.collectingTwoTimeStamps = collectingTwoTimestamps;

        this.instrMethodClassesViews = new String[3][];
        this.instrMethodClassesViews[METHOD_LEVEL_VIEW] = instrClassNames;
        this.instrMethodNames = instrMethodNames;
        this.instrMethodSignatures = instrMethodSigs;
        this.nInstrMethods = nInstrMethods;

        // Generate individual CCT containers for all threads in CPUCallGraphBuilder.
        CPUCCTContainer[] methodLevelCCTs = cctProvider.createPresentationCCTs(this);

        if (methodLevelCCTs == null || methodLevelCCTs.length == 0) {
            throw new NoDataAvailableException();
        }

        threadIdMap = new HashMap(methodLevelCCTs.length);

        for (int i = 0; i < methodLevelCCTs.length; i++) {
            threadIdMap.put(Integer.valueOf(methodLevelCCTs[i].threadId), Integer.valueOf(i));
        }

        threadCCTContainers = new CPUCCTContainer[3][];
        threadCCTContainers[METHOD_LEVEL_VIEW] = methodLevelCCTs;

        allThreadsMergedCCTContainers = new CPUCCTContainer[3];
        rootNode = new PrestimeCPUCCTNode[3];
        rootNode[METHOD_LEVEL_VIEW] = createRootNodeForAllThreads(METHOD_LEVEL_VIEW);

        if (LOGGER.isLoggable(Level.FINEST)) {
            debugValues();
        }
    }
    
    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public boolean isCollectingTwoTimeStamps() {
        return collectingTwoTimeStamps;
    }

    public CPUCCTContainer getContainerForThread(int threadId, int view) {
        if (threadCCTContainers[view] == null) {
            generateDataForView(view);
        }

        return threadCCTContainers[view][getContainerIdForThreadId(threadId)];
    }

    public FlatProfileContainer getFlatProfile(int threadId, int view) {
        if (threadCCTContainers[view] == null) {
            generateDataForView(view);
        }

        if (threadId != -1) {
            return threadCCTContainers[view][getContainerIdForThreadId(threadId)].getFlatProfile();
        } else {
            return allThreadsMergedCCTContainers[view].getFlatProfile();
        }
    }

    // -- Views-related code
    public String[] getInstrMethodClasses(int view) {
        return instrMethodClassesViews[view];
    }

    public String[] getInstrMethodNames() {
        return instrMethodNames;
    }

    public String[] getInstrMethodSignatures() {
        return instrMethodSignatures;
    }

    public String[] getMethodClassNameAndSig(int methodId, int view) {
        if (view == METHOD_LEVEL_VIEW) {
            return new String[] {
                       instrMethodClassesViews[METHOD_LEVEL_VIEW][methodId], instrMethodNames[methodId],
                       instrMethodSignatures[methodId]
                   };
        } else {
            return new String[] { instrMethodClassesViews[view][methodId], null, null };
        }
    }

    public int getNInstrMethods() {
        return nInstrMethods;
    }

    public int getNThreads() {
        return threadCCTContainers[METHOD_LEVEL_VIEW].length;
    }

    public PrestimeCPUCCTNode getReverseCCT(int threadId, int methodId, int view) {
        if (threadCCTContainers[view] == null) {
            generateDataForView(view);
        }

        if (threadId >= 0) {
            return threadCCTContainers[view][getContainerIdForThreadId(threadId)].getReverseCCT(methodId);
        } else if (threadId == -1) {
            return allThreadsMergedCCTContainers[view].getReverseCCT(methodId);
        } else {
            throw new IllegalArgumentException("!!! Cannot generate reverse CCT for threadId = " + threadId); // NOI18N
        }
    }

    public PrestimeCPUCCTNode getRootNode(int view) {
        if (threadCCTContainers[view] == null) {
            generateDataForView(view);
        }

        return rootNode[view];
    }
    
    public FilterSortSupport.Configuration getFilterSortInfo(CCTNode node) {
        return sortInfo(node);
    }
    
    private FilterSortSupport.Configuration sortInfo(CCTNode node) {
        while (node.getParent() != null)
            node = node.getParent();
        FilterSortSupport.Configuration config = sortInfos.get(node);
        if (config == null) {
            config = new FilterSortSupport.Configuration();
            sortInfos.put(node, config);
        }
        return config;
    }
    
    public void filterForward(final String filter, final int filterType, final PrestimeCPUCCTNodeBacked root) {
        FilterSortSupport.Configuration config = sortInfo(root);
        config.setFilterInfo(filter, filterType);
        
        if (!FilterSortSupport.passesFilter(config, root.getNodeName())) {
            root.setFilteredNode();
        } else {
            root.resetFilteredNode();
        }
        root.resetChildren();
    }
    
    public void filterReverse(String filter, int filterType, PrestimeCPUCCTNodeFree root, int view) {
        PrestimeCPUCCTNodeFree rev = (PrestimeCPUCCTNodeFree)getReverseCCT(
                root.getContainer().getThreadId(), root.getMethodId(), view);
        FilterSortSupport.Configuration config = sortInfo(root);
        config.setFilterInfo(filter, filterType);
        filter(config, rev);
        root.children = rev.children;
        if (root.children != null) {
            for (PrestimeCPUCCTNode ch : root.children)
                ch.parent = root;
            
            root.sortChildren(config.getSortBy(), config.getSortOrder());
        }
        if (!FilterSortSupport.passesFilter(config, root.getNodeName())) {
            root.setFilteredNode();
        } else {
            root.resetFilteredNode();
        }
    }
    
    private void filter(FilterSortSupport.Configuration config, PrestimeCPUCCTNodeFree node) {
        if (node.children != null) {
            PrestimeCPUCCTNodeFree filtered = null;
            List<PrestimeCPUCCTNodeFree> ch = new ArrayList();
            for (PrestimeCPUCCTNode n : node.children) {
                PrestimeCPUCCTNodeFree nn = (PrestimeCPUCCTNodeFree)n;
                if (FilterSortSupport.passesFilter(config, nn.getNodeName())) {
                    int i = ch.indexOf(nn);
                    if (i == -1) ch.add(nn);
                    else ch.get(i).merge(nn);
                } else {
                    if (filtered == null) {
                        nn.setFilteredNode();
                        filtered = nn;
                        ch.add(nn);
                    } else {
                        filtered.merge(nn);
                    }
                }
            }
            
            if (ch.isEmpty()) {
                node.children = null;
            } else {
                if (node.isFilteredNode() && filtered != null && ch.size() == 1) {
                    // "naive" approach, collapse simple chain of filtered out nodes
                    PrestimeCPUCCTNodeFree n = ch.get(0);
                    filter(config, n);
                    node.children = n.children;
                } else {
                    node.children = ch.toArray(new PrestimeCPUCCTNodeFree[ch.size()]);
                }
            }
            
            if (node.children != null)
                for (PrestimeCPUCCTNode n : node.children)
                    filter(config, (PrestimeCPUCCTNodeFree)n);
        }
    }

    public int[] getThreadIds() {
        int[] ret = new int[threadCCTContainers[METHOD_LEVEL_VIEW].length];

        for (int i = 0; i < threadCCTContainers[METHOD_LEVEL_VIEW].length; i++) {
            ret[i] = threadCCTContainers[METHOD_LEVEL_VIEW][i].threadId;
        }

        return ret;
    }

    public String getThreadNameForId(int threadId) {
        return getThreadNames()[getContainerIdForThreadId(threadId)];
    }

    public String[] getThreadNames() {
        String[] ret = new String[threadCCTContainers[METHOD_LEVEL_VIEW].length];

        for (int i = 0; i < threadCCTContainers[METHOD_LEVEL_VIEW].length; i++) {
            ret[i] = threadCCTContainers[METHOD_LEVEL_VIEW][i].threadName;
        }

        return ret;
    }
    
    void readFromSnapshot(CPUResultsSnapshot s) {
        beginTime = s.beginTime;
        timeTaken = s.timeTaken;
        collectingTwoTimeStamps = s.collectingTwoTimeStamps;
        
        nInstrMethods = s.nInstrMethods;
        instrMethodClassesViews = s.instrMethodClassesViews;
        
        instrMethodNames = s.instrMethodNames;
        instrMethodSignatures = s.instrMethodSignatures;
        
        threadCCTContainers = new CPUCCTContainer[3][];
        CPUCCTContainer[] scontainers = s.threadCCTContainers[METHOD_LEVEL_VIEW];
        int nThreads = scontainers.length;
        CPUCCTContainer[] containers = new CPUCCTContainer[nThreads];
        threadCCTContainers[METHOD_LEVEL_VIEW] = containers;
        
        threadIdMap = s.threadIdMap;
        for (int i = 0; i < nThreads; i++) {
            containers[i] = new CPUCCTContainer(this);
            
            containers[i].threadId = scontainers[i].threadId;
            containers[i].threadName = scontainers[i].threadName;

            containers[i].collectingTwoTimeStamps = scontainers[i].collectingTwoTimeStamps;

            containers[i].compactData = scontainers[i].compactData;

            containers[i].childOfsSize = scontainers[i].childOfsSize;

            containers[i].nodeSize = scontainers[i].nodeSize;

            containers[i].wholeGraphGrossTimeAbs = scontainers[i].wholeGraphGrossTimeAbs;
            containers[i].wholeGraphGrossTimeThreadCPU = scontainers[i].wholeGraphGrossTimeThreadCPU;
            containers[i].timeInInjectedCodeInAbsCounts = scontainers[i].timeInInjectedCodeInAbsCounts;
            containers[i].timeInInjectedCodeInThreadCPUCounts = scontainers[i].timeInInjectedCodeInThreadCPUCounts;
            containers[i].wholeGraphPureTimeAbs = scontainers[i].wholeGraphPureTimeAbs;
            containers[i].wholeGraphPureTimeThreadCPU = scontainers[i].wholeGraphPureTimeThreadCPU;
            containers[i].wholeGraphNetTime0 = scontainers[i].wholeGraphNetTime0;
            containers[i].wholeGraphNetTime1 = scontainers[i].wholeGraphNetTime1;
            containers[i].totalInvNo = scontainers[i].totalInvNo;
            containers[i].displayWholeThreadCPUTime = scontainers[i].displayWholeThreadCPUTime;

            containers[i].rootNode = new PrestimeCPUCCTNodeBacked(containers[i], null, 0);

            if (containers[i].getMethodIdForNodeOfs(0) == 0) {
                containers[i].rootNode.setThreadNode();
            }
        }
        
        allThreadsMergedCCTContainers = new CPUCCTContainer[3];
        rootNode = new PrestimeCPUCCTNode[3];
        rootNode[METHOD_LEVEL_VIEW] = createRootNodeForAllThreads(METHOD_LEVEL_VIEW);
    }

    public void readFromStream(DataInputStream in) throws IOException {
        super.readFromStream(in);
        collectingTwoTimeStamps = in.readBoolean();

        nInstrMethods = in.readInt();
        instrMethodClassesViews = new String[3][];

        String[] classNames = new String[nInstrMethods];
        instrMethodClassesViews[METHOD_LEVEL_VIEW] = classNames;
        instrMethodNames = new String[nInstrMethods];
        instrMethodSignatures = new String[nInstrMethods];

        for (int i = 0; i < nInstrMethods; i++) {
            classNames[i] = in.readUTF();
            instrMethodNames[i] = in.readUTF();
            instrMethodSignatures[i] = in.readUTF();
        }

        int nThreads = in.readInt();
        threadCCTContainers = new CPUCCTContainer[3][];

        CPUCCTContainer[] containers = new CPUCCTContainer[nThreads];
        threadCCTContainers[METHOD_LEVEL_VIEW] = containers;

        for (int i = 0; i < nThreads; i++) {
            containers[i] = new CPUCCTContainer(this);
            containers[i].readFromStream(in);
            threadIdMap.put(Integer.valueOf(containers[i].threadId), Integer.valueOf(i));
        }

        allThreadsMergedCCTContainers = new CPUCCTContainer[3];
        rootNode = new PrestimeCPUCCTNode[3];
        rootNode[METHOD_LEVEL_VIEW] = createRootNodeForAllThreads(METHOD_LEVEL_VIEW);

        if (LOGGER.isLoggable(Level.FINEST)) {
            debugValues();
        }
    }

    // -- Methods for saving/retrieving sorting parameters
    public void saveSortParams(int sortBy, boolean sortOrder, CCTNode node) {
        FilterSortSupport.Configuration config = sortInfo(node);
        config.setSortInfo(sortBy, sortOrder);
    }

    public String toString() {
        return MessageFormat.format(CPU_MSG, new Object[] { super.toString() });
    }

    //---- Serialization support
    public void writeToStream(DataOutputStream out) throws IOException {
        super.writeToStream(out);
        out.writeBoolean(collectingTwoTimeStamps);

        out.writeInt(nInstrMethods);

        String[] classNames = instrMethodClassesViews[METHOD_LEVEL_VIEW];

        for (int i = 0; i < nInstrMethods; i++) {
            out.writeUTF(classNames[i]);
            out.writeUTF(instrMethodNames[i]);
            out.writeUTF(instrMethodSignatures[i]);
        }

        CPUCCTContainer[] containers = threadCCTContainers[METHOD_LEVEL_VIEW];
        int nThreads = containers.length;
        out.writeInt(nThreads);

        for (int i = 0; i < nThreads; i++) {
            containers[i].writeToStream(out);
        }
    }

    private int getContainerIdForThreadId(int threadId) {
        Integer tid = Integer.valueOf(threadId);
        Integer cId = null;

        if (threadIdMap.containsKey(tid)) {
            cId = (Integer) threadIdMap.get(tid);
        }

        return (cId != null) ? cId.intValue() : 0;
    }

    protected PrestimeCPUCCTNode createRootNodeForAllThreads(int view) {
        CPUCCTContainer[] ccts = threadCCTContainers[view];
        int len = ccts.length;
        PrestimeCPUCCTNode[] threadNodes = new PrestimeCPUCCTNode[len];

        for (int i = 0; i < len; i++) {
            PrestimeCPUCCTNode tRootNode = ccts[i].getRootNode();

            if (!tRootNode.isThreadNode()) {
                threadNodes[i] = new PrestimeCPUCCTNodeBacked(ccts[i], new PrestimeCPUCCTNode[] { tRootNode });
            } else {
                threadNodes[i] = tRootNode;
            }
        }

        allThreadsMergedCCTContainers[view] = new AllThreadsMergedCPUCCTContainer(this, threadNodes, view);

        return allThreadsMergedCCTContainers[view].getRootNode();
    }

    private void debugValues() {
        LOGGER.log(Level.FINEST, "collectingTwoTimeStamps: {0}", collectingTwoTimeStamps); // NOI18N
        LOGGER.log(Level.FINEST, "threadCCTContainers.length: {0}", debugLength(threadCCTContainers)); // NOI18N
        LOGGER.log(Level.FINEST, "allThreadsMergedCCTContainers.length: {0}", debugLength(allThreadsMergedCCTContainers));
        LOGGER.log(Level.FINEST, "rootNode.length: {0}", debugLength(rootNode)); // NOI18N
        LOGGER.log(Level.FINEST, "instrMethodClassesViews.length: {0}", debugLength(instrMethodClassesViews));
        LOGGER.log(Level.FINEST, "instrMethodNames.length: {0}", debugLength(instrMethodNames)); // NOI18N
        LOGGER.log(Level.FINEST, "instrMethodSignatures.length: {0}", debugLength(instrMethodSignatures));
        LOGGER.log(Level.FINEST, "nInstrMethods: {0}", nInstrMethods); // NOI18N
//        LOGGER.log(Level.FINEST, "sortNodesBy: {0}", sortNodesBy); // NOI18N
//        LOGGER.log(Level.FINEST, "sortNodesOrder: {0}", sortNodesOrder); // NOI18N
    }

    /**
     * callChainType code can be:
     * 1 - call chains with invocations and timings;
     * 2 - call chains with invocation numbers only;
     * 3 - call chains with no timings and/or invocation numbers.
     * /
     * public StringBuffer getResultsInCSVFormat(int callChainTypeCode, ExportDataDumper dataDumper) {
     * //!!! Need to copy here a whole lot of stuff from CPUCCTManager
     *      
     * return null;
     * }
     */

    //------------------------------------ Private implementation ----------------------------------------------------

    /**
     * If data for the provided view have not yet been initialized, it creates threadCCTContainers[view],
     * rootNode[view] and allThreadsMergedCCTContainers[view].
     *
     * @param view either of METHOD_LEVEL_VIEW, CLASS_LEVEL_VIEW or PACKAGE_LEVEL_VIEW
     */
    private void generateDataForView(int view) {
        if (threadCCTContainers[view] == null) {
            MethodIdMap methodIdMap = new MethodIdMap(instrMethodClassesViews[METHOD_LEVEL_VIEW], nInstrMethods, view);
            int len = threadCCTContainers[METHOD_LEVEL_VIEW].length;
            threadCCTContainers[view] = new CPUCCTContainer[len];

            for (int i = 0; i < len; i++) {
                threadCCTContainers[view][i] = new CPUCCTClassContainer(threadCCTContainers[METHOD_LEVEL_VIEW][i], methodIdMap,
                                                                        view);
            }

            rootNode[view] = createRootNodeForAllThreads(view);
            instrMethodClassesViews[view] = methodIdMap.getInstrClassesOrPackages();
        }
    }
    
}
