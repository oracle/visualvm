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

package org.graalvm.visualvm.lib.jfluid.results.cpu.cct;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.graalvm.visualvm.lib.jfluid.ProfilerClient;
import org.graalvm.visualvm.lib.jfluid.ProfilerEngineSettings;
import org.graalvm.visualvm.lib.jfluid.filters.InstrumentationFilter;
import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;
import org.graalvm.visualvm.lib.jfluid.global.ProfilingSessionStatus;
import org.graalvm.visualvm.lib.jfluid.results.RuntimeCCTNodeProcessor;
import org.graalvm.visualvm.lib.jfluid.results.cpu.FlatProfileContainer;
import org.graalvm.visualvm.lib.jfluid.results.cpu.FlatProfileContainerFree;
import org.graalvm.visualvm.lib.jfluid.results.cpu.TimingAdjusterOld;
import org.graalvm.visualvm.lib.jfluid.results.cpu.cct.nodes.MethodCPUCCTNode;
import org.graalvm.visualvm.lib.jfluid.results.cpu.cct.nodes.TimedCPUCCTNode;


/**
 *
 * @author Jaroslav Bachorik
 * @author Tomas Hurka
 */
public class CCTFlattener extends RuntimeCCTNodeProcessor.PluginAdapter {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final Logger LOGGER = Logger.getLogger(CCTFlattener.class.getName());

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final Object containerGuard = new Object();

    // @GuardedBy containerGuard
    private FlatProfileContainer container;
    private ProfilerClient client;
    private Stack<TotalTime> parentStack;
    private Set methodsOnStack;
    private int[] invDiff;
    private int[] invPM;
    private int[] nCalleeInvocations;
    private long[] timePM0;
    private long[] timePM1;
    private long[] totalTimePM0;
    private long[] totalTimePM1;
    private int nMethods;

    private CCTResultsFilter currentFilter;
    private InstrumentationFilter instrFilter;
    private int cpuProfilingType;
    private boolean twoTimestamps;
    private TimingAdjusterOld timingAdjuster;
  
    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public CCTFlattener(ProfilerClient client, CCTResultsFilter filter) {
        this.client = client;
        parentStack = new Stack();
        methodsOnStack = new HashSet();
        this.currentFilter = filter;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public FlatProfileContainer getFlatProfile() {
        synchronized (containerGuard) {
            return container;
        }
    }

    @Override
    public void onStart() {
        ProfilingSessionStatus status = client.getStatus();
        ProfilerEngineSettings pes = client.getSettings();
        
        nMethods = getMaxMethodId();
        timePM0 = new long[nMethods];
        timePM1 = new long[status.collectingTwoTimeStamps() ? nMethods : 0];
        totalTimePM0 = new long[nMethods];
        totalTimePM1 = new long[status.collectingTwoTimeStamps() ? nMethods : 0];
        invPM = new int[nMethods];
        invDiff = new int[nMethods];
        nCalleeInvocations = new int[nMethods];
        parentStack.clear();
        methodsOnStack.clear();
        instrFilter = pes.getInstrumentationFilter();
        cpuProfilingType = pes.getCPUProfilingType();
        twoTimestamps = status.collectingTwoTimeStamps();
        timingAdjuster = TimingAdjusterOld.getInstance(status);
        
        synchronized (containerGuard) {
            container = null;
        }
        
        // uncomment the following piece of code when trying to reproduce #205482
//        try {
//            Thread.sleep(120);
//        } catch (InterruptedException interruptedException) {
//        }
    }

    @Override
    public void onStop() {
        // Now convert the data into microseconds
        long wholeGraphTime0 = 0;

        // Now convert the data into microseconds
        long wholeGraphTime1 = 0;
        long totalNInv = 0;

        for (int i = 0; i < nMethods; i++) {
            double time = timingAdjuster.adjustTime(timePM0[i], (invPM[i] + invDiff[i]), (nCalleeInvocations[i] + invDiff[i]),
                                                       false);

            if (time < 0) {
                // in some cases the combination of cleansing the time by calibration and subtracting wait/sleep
                // times can lead to <0 time
                // see http://profiler.netbeans.org/issues/show_bug.cgi?id=64416
                time = 0;
            }

            timePM0[i] = (long) time;

            // don't include the Thread time into wholegraphtime
            if (i > 0) {
                wholeGraphTime0 += time;
            }

            if (twoTimestamps) {
                time = timingAdjuster.adjustTime(timePM1[i], (invPM[i] + invDiff[i]), (nCalleeInvocations[i] + invDiff[i]),
                                                    true);
                
                if (time < 0) {
                    time = 0;
                }
                    
                timePM1[i] = (long) time;

                // don't include the Thread time into wholegraphtime
                if (i > 0) {
                    wholeGraphTime1 += time;
                }
            }

            totalNInv += invPM[i];
        }

        synchronized (containerGuard) {
            container = createContainer(timePM0, timePM1, totalTimePM0, totalTimePM1, invPM, wholeGraphTime0, wholeGraphTime1);
        }

        timePM0 = timePM1 = null;
        totalTimePM0 = totalTimePM1 = null;
        invPM = invDiff = nCalleeInvocations = null;
        parentStack.clear();
        methodsOnStack.clear();
        instrFilter = null;
    }
    
    @Override
    public void onNode(MethodCPUCCTNode node) {
        final int nodeMethodId = node.getMethodId();
        final int nodeFilerStatus = node.getFilteredStatus();
        final MethodCPUCCTNode currentParent = parentStack.isEmpty() ? null : (MethodCPUCCTNode) parentStack.peek().parent;
        boolean filteredOut = (nodeFilerStatus == TimedCPUCCTNode.FILTERED_YES); // filtered out by rootmethod/markermethod rules

        if (!filteredOut && (cpuProfilingType == CommonConstants.CPU_SAMPLED || nodeFilerStatus == TimedCPUCCTNode.FILTERED_MAYBE)) { // filter out all methods not complying to instrumentation filter & secure to remove

            String jvmClassName = getInstrMethodClass(nodeMethodId).replace('.', '/'); // NOI18N

            if (!instrFilter.passes(jvmClassName)) {
                filteredOut = true;
            }
        }

        if (!filteredOut && (currentFilter != null)) {
            filteredOut = !currentFilter.passesFilter(); // finally use the mark filter
        }
        final int parentMethodId = currentParent != null ? currentParent.getMethodId() : -1;
        
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST, "Processing runtime node: {0}.{1}; filtered={2}, time={3}, CPU time={4}", // NOI18N
                       new Object[]{getInstrMethodClass(nodeMethodId), getInstrMethodName(nodeMethodId), 
                       filteredOut, node.getNetTime0(), node.getNetTime1()});

            String parentInfo = (currentParent != null)
                                ? (getInstrMethodClass(parentMethodId) + "."
                                + getInstrMethodName(parentMethodId)) : "none"; // NOI18N
            LOGGER.log(Level.FINEST, "Currently used parent: {0}", parentInfo); // NOI18N
        }

        if (filteredOut) {
            if ((currentParent != null) && !currentParent.isRoot()) {
                invDiff[parentMethodId] += node.getNCalls();

                timePM0[parentMethodId] += node.getNetTime0();

                if (twoTimestamps) {
                    timePM1[parentMethodId] += node.getNetTime1();
                }
            }
        } else {
            timePM0[nodeMethodId] += node.getNetTime0();

            if (twoTimestamps) {
                timePM1[nodeMethodId] += node.getNetTime1();
            }

            invPM[nodeMethodId] += node.getNCalls();

            if ((currentParent != null) && !currentParent.isRoot()) {
                nCalleeInvocations[parentMethodId] += node.getNCalls();
            }
        }
        final MethodCPUCCTNode nextParent = filteredOut ? currentParent : node;
        final TotalTime timeNode = new TotalTime(nextParent,methodsOnStack.contains(nodeMethodId));
        timeNode.totalTimePM0+=node.getNetTime0();
        if (twoTimestamps) timeNode.totalTimePM1+=node.getNetTime1();  
        if (!timeNode.recursive) {
            methodsOnStack.add(nodeMethodId);
        }
        parentStack.push(timeNode);
    }

    @Override
    public void onBackout(MethodCPUCCTNode node) {
        TotalTime current = parentStack.pop();
        if (!current.recursive) {
            int nodeMethodId = node.getMethodId();
            methodsOnStack.remove(nodeMethodId);
            if (nodeMethodId != -1) {
                long time = (long) timingAdjuster.adjustTime(current.totalTimePM0, node.getNCalls()+current.outCalls, current.outCalls,
                                                           false);
                if (time>0) {
                    totalTimePM0[nodeMethodId]+=time;
                }
                if (twoTimestamps) {
                    time = (long) timingAdjuster.adjustTime(current.totalTimePM1, node.getNCalls()+current.outCalls, current.outCalls,
                                                           true);
                    if (time>0) {
                        totalTimePM1[nodeMethodId]+=time;
                    }
                }
            }
        }
        // add self data to parent
        if (!parentStack.isEmpty()) {
            TotalTime parent = parentStack.peek();
            parent.add(current);
            parent.outCalls+=node.getNCalls();
        }
    }

    protected int getMaxMethodId() {
        return client.getStatus().getNInstrMethods();
    }

    protected String getInstrMethodClass(int nodeMethodId) {
        return client.getStatus().getInstrMethodClasses()[nodeMethodId];
    }

    protected String getInstrMethodName(int nodeMethodId) {
        return client.getStatus().getInstrMethodNames()[nodeMethodId];
    }

    protected FlatProfileContainer createContainer(long[] timeInMcs0, long[] timeInMcs1, 
            long[] totalTimeInMcs0, long[] totalTimeInMcs1, int[] nInvocations, 
            double wholeGraphNetTime0, double wholeGraphNetTime1) {
        return new FlatProfileContainerFree(client.getStatus(), timeInMcs0, timeInMcs1, totalTimeInMcs0, totalTimeInMcs1,
                    nInvocations, new char[0], wholeGraphNetTime0, wholeGraphNetTime1, nInvocations.length);
    }

    private static class TotalTime {
        private final MethodCPUCCTNode parent;
        private final boolean recursive;
        private int outCalls;
        private long totalTimePM0;
        private long totalTimePM1;
        
        TotalTime(MethodCPUCCTNode n, boolean r) {
            parent = n;
            recursive = r;
        }

        private void add(TotalTime current) {
            outCalls += current.outCalls;
            totalTimePM0 += current.totalTimePM0;
            totalTimePM1 += current.totalTimePM1;
        }
    }
}
