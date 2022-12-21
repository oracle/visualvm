/*
 * Copyright (c) 2016, 2022, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.jfluid.results.jdbc;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.logging.Level;
import org.graalvm.visualvm.lib.jfluid.ProfilerClient;
import org.graalvm.visualvm.lib.jfluid.client.ClientUtils;
import org.graalvm.visualvm.lib.jfluid.results.FilterSortSupport;
import org.graalvm.visualvm.lib.jfluid.results.ResultsSnapshot;
import org.graalvm.visualvm.lib.jfluid.results.cpu.FlatProfileContainer;
import org.graalvm.visualvm.lib.jfluid.results.memory.JMethodIdTable;
import org.graalvm.visualvm.lib.jfluid.results.memory.PresoObjAllocCCTNode;
import org.graalvm.visualvm.lib.jfluid.results.memory.RuntimeMemoryCCTNode;
import org.graalvm.visualvm.lib.jfluid.results.memory.RuntimeObjAllocTermCCTNode;
import org.graalvm.visualvm.lib.jfluid.results.memory.RuntimeObjLivenessTermCCTNode;

/**
 * Results snapshot for Jdbc (selects) Profiling.
 *
 * @author Tomas Hurka
 */
public class JdbcResultsSnapshot extends ResultsSnapshot {
   /***************************************************************************
    +------------------------------------------------------------------------------+
    | Profiler memory snapshot format description                                  |
    +------------------------------------------------------------------------------+
    int         version
    long        timestamp
    long        duration
    int         # profiled selects
    ===> for(# profiled selects)
    string      select
    long        #number of invocations
    <===
    boolean     contains stacktraces
    int         # stacktraces
    ===> for(# stacktraces)
    :::> load node
    int         type (RuntimeMemoryCCTNode.TYPE_RuntimeMemoryCCTNode,
                      RuntimeMemoryCCTNode.TYPE_RuntimeObjAllocTermCCTNode,
                      RuntimeMemoryCCTNode.RuntimeObjLivenessTermCCTNode)
    int         methodId
    int         # children
    ======> for(# children)
    >load node<
    <=====
    <::: load node
    <===
    ***************************************************************************/
    
    private JMethodIdTable table;
    /** [0-nProfiledSelects] select names */
    String[] selectNames;

    /** [1-nProfiledSelects] total number of invocations for select */
    long[] invocationsPerSelectId;

    /** [1-nProfiledSelects] total time for select */
    long[] timePerSelectId;

    /** [1-nProfiledSelects] select type see JdbcCCTProvider.SQL* constants */
    int[] typeForSelectId;

    /** [1-nProfiledSelects] command type see JdbcCCTProvider.SQL_COMMAND* constants */
    int[] commandTypeForSelectId;

    /** [1-nProfiledSelects] array of  SQL tables affected by selectId */
    String[][] tablesForSelectId;

    /** [1-nProfiledSelects] select Id -> root of its allocation traces tree */
    private RuntimeMemoryCCTNode[] stacksForSelects;

    /** total number of profiled selects */
    int nProfiledSelects;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public JdbcResultsSnapshot() { // No-arg constructor needed for above serialization methods to work
    } 

    public JdbcResultsSnapshot(long beginTime, long timeTaken, JdbcCCTProvider provider, ProfilerClient client)
                          throws ClientUtils.TargetAppOrVMTerminated {
        super(beginTime, timeTaken);

        provider.beginTrans(false);

        try {
            performInit(client, provider);           
        } finally {
            provider.endTrans();

            if (LOGGER.isLoggable(Level.FINEST)) {
                debugValues();
            }
        }
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public String getSelectName(int selectId) {
        return selectNames[selectId];
    }

    public String[] getSelectNames() {
        return selectNames;
    }

    public JMethodIdTable getJMethodIdTable() {
        return table;
    }

    public int getNProfiledSelects() {
        return nProfiledSelects;
    }

    public long[] getInvocationsPerSelectId() {
        return invocationsPerSelectId;
    }

    public long[] getTimePerSelectId() {
        return timePerSelectId;
    }

    public int[] getTypeForSelectId() {
        return typeForSelectId;
    }

    public int[] getCommandTypeForSelectId() {
        return commandTypeForSelectId;
    }

    public String[][] getTablesForSelectId() {
        return tablesForSelectId;
    }

    public boolean containsStacks() {
        return stacksForSelects != null;
    }
        
    public void filterReverse(String filter, int filterType, int sortBy, boolean sortOrder, PresoObjAllocCCTNode root, int selectId, boolean dontShowZeroLiveObjAllocPaths) {
        PresoObjAllocCCTNode rev =
                (PresoObjAllocCCTNode)createPresentationCCT(selectId, dontShowZeroLiveObjAllocPaths);
        filter(filter, filterType, rev);
        root.children = rev.children;
        if (root.children != null) {
            for (PresoObjAllocCCTNode ch : root.children)
                ch.parent = root;
            root.sortChildren(sortBy, sortOrder);
        }
        if (!FilterSortSupport.passesFilter(filter, filterType, root.getNodeName())) {
//            root.setFilteredNode();
//            root.methodId = -1;
        } else {
//            root.resetFilteredNode();
        }
    }
    
    private void filter(String filter, int filterType, PresoObjAllocCCTNode node) {
        if (node.children != null) {
            PresoObjAllocCCTNode filtered = null;
            List<PresoObjAllocCCTNode> ch = new ArrayList();
            for (PresoObjAllocCCTNode n : node.children) {
                PresoObjAllocCCTNode nn = (PresoObjAllocCCTNode)n;
                if (FilterSortSupport.passesFilter(filter, filterType, nn.getNodeName())) {
                    int i = ch.indexOf(nn);
                    if (i == -1) ch.add(nn);
                    else ch.get(i).merge(nn);
                } else {
                    if (filtered == null) {
//                        nn.setFilteredNode();
//                        nn.methodId = -1;
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
                if (node.isFiltered() && filtered != null && ch.size() == 1) {
                    // "naive" approach, collapse simple chain of filtered out nodes
                    PresoObjAllocCCTNode n = ch.get(0);
                    filter(filter, filterType, n);
                    node.children = n.children;
                } else {
                    node.children = ch.toArray(new PresoObjAllocCCTNode[0]);
                }
            }
            
            if (node.children != null)
                for (PresoObjAllocCCTNode n : node.children)
                    filter(filter, filterType, (PresoObjAllocCCTNode)n);
        }
    }
    

    /**
     * Creates a presentation-time allocation stack traces CCT for given selectId.
     *
     * @param selectId                       Select ID of the select whose allocation stack traces we request
     * @param dontShowZeroLiveObjAllocPaths If true, allocation paths with zero live objects will not be included in CCT
     * @return presentation-time CCT with allocation stack traces or null if none are available
     */
    public PresoObjAllocCCTNode createPresentationCCT(int selectId, boolean dontShowZeroLiveObjAllocPaths) {
        if (stacksForSelects == null) {
            return null;
        }

        RuntimeMemoryCCTNode rootNode = stacksForSelects[selectId];

        if (rootNode == null) {
            return null;
        }

        return createPresentationCCT(rootNode, selectId, dontShowZeroLiveObjAllocPaths);
    }

    public JdbcResultsSnapshot createDiff(JdbcResultsSnapshot snapshot) {
        if (!(snapshot instanceof JdbcResultsSnapshot)) return null;
        return new JdbcResultsDiff(this, (JdbcResultsSnapshot)snapshot);
    }

    public void readFromStream(DataInputStream in) throws IOException {
        super.readFromStream(in);
        
        StringCache strings = new StringCache();
        nProfiledSelects = in.readInt();
        selectNames = new String[nProfiledSelects];
        invocationsPerSelectId = new long[nProfiledSelects];
        timePerSelectId = new long[nProfiledSelects];
        typeForSelectId = new int[nProfiledSelects];
        commandTypeForSelectId = new int[nProfiledSelects];
        tablesForSelectId = new String[nProfiledSelects][];

        for (int i = 1; i < nProfiledSelects; i++) {
            selectNames[i] = in.readUTF();
            invocationsPerSelectId[i] = in.readLong();
            timePerSelectId[i] = in.readLong();
            typeForSelectId[i] = in.readInt();
            commandTypeForSelectId[i] = in.readInt();
            tablesForSelectId[i] = new String[in.readInt()];
            for (int j = 0; j < tablesForSelectId[i].length; j++) {
                tablesForSelectId[i][j] = strings.intern(in.readUTF());
            }
        }

        if (in.readBoolean()) {
            int len = in.readInt();
            //System.err.println("Read len: " +len);
            stacksForSelects = new RuntimeMemoryCCTNode[len];

            for (int i = 0; i < len; i++) {
                int type = in.readInt();

                //System.err.println("  [" + i + "] = " + type);
                if (type != 0) {
                    stacksForSelects[i] = RuntimeMemoryCCTNode.create(type);
                    stacksForSelects[i].readFromStream(in);
                }
            }

            if (in.readBoolean()) {
                table = new JMethodIdTable();
                table.readFromStream(in);
            }
        }

        if (LOGGER.isLoggable(Level.FINEST)) {
            debugValues();
        }
    }

    //---- Serialization support
    public void writeToStream(DataOutputStream out) throws IOException {
        super.writeToStream(out);

        out.writeInt(nProfiledSelects);

        for (int i = 1; i < nProfiledSelects; i++) {
            out.writeUTF(selectNames[i]);
            out.writeLong(invocationsPerSelectId[i]);
            out.writeLong(timePerSelectId[i]);
            out.writeInt(typeForSelectId[i]);
            out.writeInt(commandTypeForSelectId[i]);
            out.writeInt(tablesForSelectId[i].length);
            for (String item : tablesForSelectId[i]) {
                out.writeUTF(item);
            }
        }

        out.writeBoolean(stacksForSelects != null);

        if (stacksForSelects != null) {
            out.writeInt(stacksForSelects.length);

            //.err.println("Stored len: " +stacksForSelects.length);
            for (RuntimeMemoryCCTNode stacksForSelect : stacksForSelects) {
                if (stacksForSelect == null) {
                    //System.err.println("  [" + i + "] = 0");
                    out.writeInt(0);
                } else {
                    out.writeInt(stacksForSelect.getType());
                    //System.err.println("  [" + i + "] = " + stacksForSelects[i].getType());
                    stacksForSelect.writeToStream(out);
                }
            }

            out.writeBoolean(table != null);

            if (table != null) {
                table.writeToStream(out);
            }
        }
    }

    /**
     * Will create presentation CCT for call stacks for given root node.
     *
     * @param rootNode                      The root node that contains allocation stack traces data
     * @param selectId                       Id of select whose allocations we are requesting
     * @param dontShowZeroLiveObjAllocPaths if true, allocation paths with zero live objects will not be included
     * @return a non-null instance of the root of presentation-time allocations CCT
     */
    protected PresoObjAllocCCTNode createPresentationCCT(RuntimeMemoryCCTNode rootNode, int selectId,
                                                                  boolean dontShowZeroLiveObjAllocPaths) {
                return PresoObjAllocCCTNode.createPresentationCCTFromSnapshot(getJMethodIdTable(), rootNode, getSelectName(selectId));
    }

    protected void performInit(ProfilerClient client, JdbcCCTProvider provider)
                                 throws ClientUtils.TargetAppOrVMTerminated {
        FlatProfileContainer fpc = provider.createFlatProfile();
        if (fpc != null) {
            nProfiledSelects = fpc.getNRows() + 1;

            invocationsPerSelectId = new long[nProfiledSelects];
            timePerSelectId = new long[nProfiledSelects];
            typeForSelectId = new int[nProfiledSelects];
            commandTypeForSelectId = new int[nProfiledSelects];
            tablesForSelectId = new String[nProfiledSelects][];
            selectNames = new String[nProfiledSelects];
            for (int i=0; i<fpc.getNRows() ; i++) {
                int selectId = fpc.getMethodIdAtRow(i);
                selectNames[selectId] = fpc.getMethodNameAtRow(i);
                invocationsPerSelectId[selectId] = fpc.getNInvocationsAtRow(i);
                timePerSelectId[selectId] = fpc.getTotalTimeInMcs0AtRow(i);
                typeForSelectId[selectId] = provider.getCommandType(selectId);
                commandTypeForSelectId[selectId] = provider.getSQLCommand(selectId);
                tablesForSelectId[selectId] = provider.getTables(selectId);
            }

            RuntimeMemoryCCTNode[] stacks = provider.getStacksForSelects();
            if ((stacks != null) && checkContainsStacks(stacks)) {
                stacksForSelects = new RuntimeMemoryCCTNode[stacks.length];
                PresoObjAllocCCTNode.getNamesForMethodIdsFromVM(client, stacks);

                for (int i = 0; i < nProfiledSelects; i++) {
                    if (stacks[i] != null) {
                        stacksForSelects[i] = (RuntimeMemoryCCTNode) stacks[i].clone();
                        updateTime(stacksForSelects[i], timePerSelectId[i]/invocationsPerSelectId[i]);
                    }
                }
                table = new JMethodIdTable(client.getJMethodIdTable());
            }
        } else {
            selectNames = new String[0];
        }
    }

    private boolean checkContainsStacks(RuntimeMemoryCCTNode[] stacksForSelects) {
        for (RuntimeMemoryCCTNode stacksForSelect : stacksForSelects) {
            if (stacksForSelect == null) {
                continue;
            }

            if (stacksForSelect instanceof RuntimeObjAllocTermCCTNode) {
                continue;
            }

            if (stacksForSelect instanceof RuntimeObjLivenessTermCCTNode) {
                continue;
            }

            return true;
        }

        return false; // no data but term nodes or nulls
    }

    void debugValues() {
        LOGGER.finest("nProfiledSelects: " + nProfiledSelects); // NOI18N
        LOGGER.finest("stacksForSelects.length: " + debugLength(stacksForSelects)); // NOI18N
        LOGGER.finest("invocationsPerSelectId.length: " + debugLength(invocationsPerSelectId));
        LOGGER.finest("timePerSelectId.length: " + debugLength(timePerSelectId));
        LOGGER.finest("typeForSelectId.length: " + debugLength(typeForSelectId));
        LOGGER.finest("commandTypeForSelectId.length: " + debugLength(commandTypeForSelectId));
        LOGGER.finest("tablesForSelectId.length: " + debugLength(tablesForSelectId));
        LOGGER.finest("selectNames.length: " + debugLength(selectNames)); // NOI18N
        LOGGER.finest("table: " + ((table == null) ? "null" : table.debug())); // NOI18N
    }

    private void updateTime(RuntimeMemoryCCTNode stacksForSelect, long l) {
        Stack nodes = new Stack();
        
        nodes.add(stacksForSelect);
        while (!nodes.empty()) {
            RuntimeMemoryCCTNode n = (RuntimeMemoryCCTNode) nodes.pop();
            if (n instanceof RuntimeObjAllocTermCCTNode) {
                RuntimeObjAllocTermCCTNode node = (RuntimeObjAllocTermCCTNode)n;
                node.totalObjSize = l*node.nCalls;
            }
            if (n.children != null) {
                if (n.children instanceof RuntimeMemoryCCTNode) {
                    nodes.push(n.children);
                } else {
                    RuntimeMemoryCCTNode[] ar = (RuntimeMemoryCCTNode[]) n.children;

                    for (RuntimeMemoryCCTNode ar1 : ar) {
                        nodes.push(ar1);
                    }
                }
            }
        }
    }
}
