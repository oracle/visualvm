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

package org.netbeans.lib.profiler.results.cpu.cct;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.lib.profiler.ProfilerClient;
import org.netbeans.lib.profiler.ProfilerEngineSettings;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.global.InstrumentationFilter;
import org.netbeans.lib.profiler.global.ProfilingSessionStatus;
import org.netbeans.lib.profiler.results.RuntimeCCTNodeProcessor;
import org.netbeans.lib.profiler.results.cpu.FlatProfileContainer;
import org.netbeans.lib.profiler.results.cpu.FlatProfileContainerFree;
import org.netbeans.lib.profiler.results.cpu.TimingAdjusterOld;
import org.netbeans.lib.profiler.results.cpu.cct.nodes.MethodCPUCCTNode;
import org.netbeans.lib.profiler.results.cpu.cct.nodes.TimedCPUCCTNode;


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
    private String[] instrMethodClasses;
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
        
        nMethods = status.getNInstrMethods();
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
        instrMethodClasses = status.getInstrMethodClasses();
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
        ProfilingSessionStatus status = client.getStatus();

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
                timePM1[i] = (long) time;

                // don't include the Thread time into wholegraphtime
                if (i > 0) {
                    wholeGraphTime1 += time;
                }
            }

            totalNInv += invPM[i];
        }

        synchronized (containerGuard) {
            container = new FlatProfileContainerFree(status, timePM0, timePM1, totalTimePM0, totalTimePM1,
                    invPM, new char[0], wholeGraphTime0, wholeGraphTime1, invPM.length);
        }

        timePM0 = timePM1 = null;
        totalTimePM0 = totalTimePM1 = null;
        invPM = invDiff = nCalleeInvocations = null;
        parentStack.clear();
        methodsOnStack.clear();
        instrFilter = null;
        instrMethodClasses = null;
    }
    
    @Override
    public void onNode(MethodCPUCCTNode node) {
        final int nodeMethodId = node.getMethodId();
        final int nodeFilerStatus = node.getFilteredStatus();
        final MethodCPUCCTNode currentParent = parentStack.isEmpty() ? null : (MethodCPUCCTNode) parentStack.peek().parent;
        boolean filteredOut = (nodeFilerStatus == TimedCPUCCTNode.FILTERED_YES); // filtered out by rootmethod/markermethod rules

        if (!filteredOut && (cpuProfilingType == CommonConstants.CPU_SAMPLED || nodeFilerStatus == TimedCPUCCTNode.FILTERED_MAYBE)) { // filter out all methods not complying to instrumentation filter & secure to remove

            String jvmClassName = instrMethodClasses[nodeMethodId].replace('.', '/');

            if (!instrFilter.passesFilter(jvmClassName)) {
                filteredOut = true;
            }
        }

        if (!filteredOut && (currentFilter != null)) {
            filteredOut = !currentFilter.passesFilter(); // finally use the mark filter
        }
        final int parentMethodId = currentParent != null ? currentParent.getMethodId() : -1;
        
        if (LOGGER.isLoggable(Level.FINEST)) {
            ProfilingSessionStatus status = client.getStatus();
            LOGGER.log(Level.FINEST, "Processing runtime node: {0}.{1}; filtered={2}, time={3}, CPU time={4}", // NOI18N
                       new Object[]{status.getInstrMethodClasses()[nodeMethodId], status.getInstrMethodNames()[nodeMethodId], 
                       filteredOut, node.getNetTime0(), node.getNetTime1()});

            String parentInfo = (currentParent != null)
                                ? (status.getInstrMethodClasses()[parentMethodId] + "."
                                + status.getInstrMethodNames()[parentMethodId]) : "none"; // NOI18N
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
        // add self data to parent
        if (!parentStack.isEmpty()) {
            TotalTime parent = parentStack.peek();
            parent.add(current);
            parent.outCalls+=node.getNCalls();
        }
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
