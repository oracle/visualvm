/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tools.visualvm.sampler.cpu;

import java.util.Stack;
import org.netbeans.lib.profiler.global.InstrumentationFilter;
import org.netbeans.lib.profiler.results.RuntimeCCTNodeProcessor;
import org.netbeans.lib.profiler.results.cpu.FlatProfileContainer;
import org.netbeans.lib.profiler.results.cpu.MethodInfoMapper;
import org.netbeans.lib.profiler.results.cpu.cct.nodes.MethodCPUCCTNode;
import org.netbeans.lib.profiler.results.cpu.cct.nodes.TimedCPUCCTNode;

/**
 *
 * @author Tomas Hurka
 */
final class CCTFlattener extends RuntimeCCTNodeProcessor.PluginAdapter {

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final Object containerGuard = new Object();

    // @GuardedBy containerGuard
    private FlatProfileContainer container;
    private Stack parentStack;
    private int[] invDiff;
    private int[] invPM;
    private int[] nCalleeInvocations;
    private long[] timePM0;
    private long[] timePM1;
    private int nMethods;
    private boolean collectingTwoTimeStamps;
    private MethodInfoMapper methodInfoMapper;
    private InstrumentationFilter filter;
    
    //~ Constructors -------------------------------------------------------------------------------------------------------------

    CCTFlattener(boolean twoStamps, MethodInfoMapper mapper, InstrumentationFilter f) {
        parentStack = new Stack();
        nMethods = mapper.getMaxMethodId();
        methodInfoMapper = mapper;
        collectingTwoTimeStamps = twoStamps;
        filter = f;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    FlatProfileContainer getFlatProfile() {
        synchronized (containerGuard) {
            return container;
        }
    }

    public void onStop() {
        // Now convert the data into microseconds
        long wholeGraphTime0 = 0;

        // Now convert the data into microseconds
        long wholeGraphTime1 = 0;
        long totalNInv = 0;

        for (int i = 0; i < nMethods; i++) {
            // convert to microseconds
            double time = timePM0[i] / 1000.0;

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

            if (collectingTwoTimeStamps) {
                // convert to microseconds
                time = timePM1[i] / 1000.0;
                timePM1[i] = (long) time;

                // don't include the Thread time into wholegraphtime
                if (i > 0) {
                    wholeGraphTime1 += time;
                }
            }

            totalNInv += invPM[i];
        }

        synchronized (containerGuard) {
            container = new FlatProfilerContainer(methodInfoMapper, collectingTwoTimeStamps, timePM0, timePM1, invPM, new char[0], wholeGraphTime0,
                                                     wholeGraphTime1, invPM.length);
        }

        timePM0 = timePM1 = null;
        invPM = invDiff = nCalleeInvocations = null;
        parentStack.clear();
//        currentFilter = null;
    }

    public void onStart() {
        timePM0 = new long[nMethods];
        timePM1 = new long[collectingTwoTimeStamps ? nMethods : 0];
        invPM = new int[nMethods];
        invDiff = new int[nMethods];
        nCalleeInvocations = new int[nMethods];
        parentStack.clear();

//        currentFilter = (CCTResultsFilter)Lookup.getDefault().lookup(CCTResultsFilter.class);
        
        synchronized (containerGuard) {
            container = null;
        }
    }

    public void onNode(MethodCPUCCTNode node) {
        MethodCPUCCTNode currentParent = parentStack.isEmpty() ? null : (MethodCPUCCTNode) parentStack.peek();
        boolean filteredOut = node.getFilteredStatus() == TimedCPUCCTNode.FILTERED_YES; // filtered out by rootmethod/markermethod rules

        if (!filteredOut) {
            String jvmClassName = methodInfoMapper.getInstrMethodClass(node.getMethodId()).replace('.', '/'); // NOI18N
            filteredOut = !filter.passesFilter(jvmClassName);
        }
        if (filteredOut) {
            if ((currentParent != null) && !currentParent.isRoot()) {
                invDiff[currentParent.getMethodId()] += node.getNCalls();

                timePM0[currentParent.getMethodId()] += node.getNetTime0();

                if (collectingTwoTimeStamps) {
                    timePM1[currentParent.getMethodId()] += node.getNetTime1();
                }
            }
        } else {
            timePM0[node.getMethodId()] += node.getNetTime0();

            if (collectingTwoTimeStamps) {
                timePM1[node.getMethodId()] += node.getNetTime1();
            }

            invPM[node.getMethodId()] += node.getNCalls();

            if ((currentParent != null) && !currentParent.isRoot()) {
                nCalleeInvocations[currentParent.getMethodId()] += node.getNCalls();
            }

            currentParent = node;
        }

        parentStack.push(currentParent);
    }

    public void onBackout(MethodCPUCCTNode node) {
        parentStack.pop();
    }
}