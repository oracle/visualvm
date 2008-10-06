/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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

import org.netbeans.lib.profiler.global.ProfilingSessionStatus;
import org.netbeans.lib.profiler.results.ResultsSnapshot;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;


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
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    /**
     * This exception just indicates that snapshot can't be created because no data is available
     */
    public static class NoDataAvailableException extends Exception {
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.netbeans.lib.profiler.results.cpu.Bundle"); // NOI18N
    private static final String CPU_MSG = messages.getString("CPUResultsSnapshot_CpuMsg"); // NOI18N
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
    protected boolean sortNodesOrder;

    // Number of instrumented methods - may be smaller than the size of the above arrays
    protected int nInstrMethods;

    // Remembered sorting parameters for CCT nodes
    private int sortNodesBy;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public CPUResultsSnapshot() { // No-arg constructor needed for above serialization methods to work
        super();
        threadIdMap = new HashMap();
    }

    public CPUResultsSnapshot(long beginTime, long timeTaken, CPUCCTProvider cctProvider, ProfilingSessionStatus status)
                       throws NoDataAvailableException {
        super(beginTime, timeTaken);
        status.beginTrans(false);

        try {
            collectingTwoTimeStamps = status.collectingTwoTimeStamps();

            instrMethodClassesViews = new String[3][];
            instrMethodClassesViews[METHOD_LEVEL_VIEW] = status.getInstrMethodClasses();
            instrMethodNames = status.getInstrMethodNames();
            instrMethodSignatures = status.getInstrMethodSignatures();
            nInstrMethods = status.getNInstrMethods();
        } finally {
            status.endTrans();
        }

        // Generate individual CCT containers for all threads in CPUCallGraphBuilder.
        CPUCCTContainer[] methodLevelCCTs = cctProvider.createPresentationCCTs(this);

        if (methodLevelCCTs == null) {
            throw new NoDataAvailableException();
        }

        int len = methodLevelCCTs.length;

        if (len == 0) {
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

    public int getSortBy() {
        return sortNodesBy;
    }

    public boolean getSortOrder() {
        return sortNodesOrder;
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
    public void saveSortParams(int sortBy, boolean sortOrder) {
        sortNodesBy = sortBy;
        sortNodesOrder = sortOrder;
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
        LOGGER.finest("collectingTwoTimeStamps: " + collectingTwoTimeStamps); // NOI18N
        LOGGER.finest("threadCCTContainers.length: " + debugLength(threadCCTContainers)); // NOI18N
        LOGGER.finest("allThreadsMergedCCTContainers.length: " + debugLength(allThreadsMergedCCTContainers) // NOI18N
        );
        LOGGER.finest("rootNode.length: " + debugLength(rootNode)); // NOI18N
        LOGGER.finest("instrMethodClassesViews.length: " + debugLength(instrMethodClassesViews) // NOI18N
        );
        LOGGER.finest("instrMethodNames.length: " + debugLength(instrMethodNames)); // NOI18N
        LOGGER.finest("instrMethodSignatures.length: " + debugLength(instrMethodSignatures) // NOI18N
        );
        LOGGER.finest("nInstrMethods: " + nInstrMethods); // NOI18N
        LOGGER.finest("sortNodesBy: " + sortNodesBy); // NOI18N
        LOGGER.finest("sortNodesOrder: " + sortNodesOrder); // NOI18N
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
