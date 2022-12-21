/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.sampler.truffle.cpu;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import org.graalvm.visualvm.lib.jfluid.filters.InstrumentationFilter;
import org.graalvm.visualvm.lib.jfluid.results.RuntimeCCTNodeProcessor;
import org.graalvm.visualvm.lib.jfluid.results.cpu.FlatProfileContainer;
import org.graalvm.visualvm.lib.jfluid.results.cpu.MethodInfoMapper;
import org.graalvm.visualvm.lib.jfluid.results.cpu.cct.nodes.MethodCPUCCTNode;
import org.graalvm.visualvm.lib.jfluid.results.cpu.cct.nodes.TimedCPUCCTNode;

/**
 *
 * @author Tomas Hurka
 */
final class CCTFlattener extends RuntimeCCTNodeProcessor.PluginAdapter {

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final Object containerGuard = new Object();

    // @GuardedBy containerGuard
    private FlatProfileContainer container;
    private Stack<TotalTime> parentStack;
    private Set<Integer> methodsOnStack;
    private int[] invDiff;
    private int[] invPM;
    private int[] nCalleeInvocations;
    private long[] timePM0;
    private long[] timePM1;
    private long[] totalTimePM0;
    private long[] totalTimePM1;
    private int nMethods;
    private InstrumentationFilter instrFilter;
    private boolean twoTimestamps;
    private MethodInfoMapper methodInfoMapper;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    CCTFlattener(boolean twoStamps, MethodInfoMapper mapper, InstrumentationFilter f) {
        parentStack = new Stack<>();
        methodsOnStack = new HashSet<>();
        nMethods = mapper.getMaxMethodId();
        methodInfoMapper = mapper;
        twoTimestamps = twoStamps;
        instrFilter = f;
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
                // see https://netbeans.org/bugzilla/show_bug.cgi?id=64416
                time = 0;
            }

            timePM0[i] = (long) time;

            // don't include the Thread time into wholegraphtime
            if (i > 0) {
                wholeGraphTime0 += time;
            }

            if (twoTimestamps) {
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
            container = new FlatProfilerContainer(methodInfoMapper, twoTimestamps, timePM0, timePM1, totalTimePM0, totalTimePM1,
                    invPM, new char[0], wholeGraphTime0, wholeGraphTime1, invPM.length);
        }

        timePM0 = timePM1 = null;
        invPM = invDiff = nCalleeInvocations = null;
        parentStack.clear();
        methodsOnStack.clear();
        instrFilter = null;
    }

    public void onStart() {
        timePM0 = new long[nMethods];
        timePM1 = new long[twoTimestamps ? nMethods : 0];
        totalTimePM0 = new long[nMethods];
        totalTimePM1 = new long[twoTimestamps ? nMethods : 0];
        invPM = new int[nMethods];
        invDiff = new int[nMethods];
        nCalleeInvocations = new int[nMethods];
        parentStack.clear();
        methodsOnStack.clear();

        synchronized (containerGuard) {
            container = null;
        }
    }

    public void onNode(MethodCPUCCTNode node) {
        final int nodeMethodId = node.getMethodId();
        final int nodeFilerStatus = node.getFilteredStatus();
        final MethodCPUCCTNode currentParent = parentStack.isEmpty() ? null : parentStack.peek().parent;
        boolean filteredOut = (nodeFilerStatus == TimedCPUCCTNode.FILTERED_YES); // filtered out by rootmethod/markermethod rules

        if (!filteredOut) {
            String jvmClassName = methodInfoMapper.getInstrMethodClass(nodeMethodId).replace('.', '/'); // NOI18N
            filteredOut = !instrFilter.passes(jvmClassName);
        }

        final int parentMethodId = currentParent != null ? currentParent.getMethodId() : -1;

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

    public void onBackout(MethodCPUCCTNode node) {
        TotalTime current = parentStack.pop();
        if (!current.recursive) {
            int nodeMethodId = node.getMethodId();
            methodsOnStack.remove(nodeMethodId);
            // convert to microseconds
            double time = current.totalTimePM0 / 1000.0;
            if (time>0) {
                totalTimePM0[nodeMethodId]+=time;
            }
            if (twoTimestamps) {
                time = current.totalTimePM1 / 1000.0;
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
