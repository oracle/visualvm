/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.sampler.truffle.stagent;

import com.oracle.truffle.tools.profiler.HeapHistogram;
import com.oracle.truffle.tools.profiler.StackTraces;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Tomas Hurka
 */
public class Truffle implements TruffleMBean {

    private ThreadMXBean threadBean;

    public Truffle() {
        threadBean = ManagementFactory.getThreadMXBean();
        try {
            for (StackTraces stacks : StackTraces.getAllStackTracesInstances()) {
                stacks.setDelaySamplingUntilNonInternalLangInit(false);
                if (TruffleJMX.DEBUG) {
                    System.out.println("Stacks " + stacks + " " + Integer.toHexString(System.identityHashCode(stacks)));
                    System.out.println(threadDump(stacks));
                }
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public Map<String, Object>[] dumpAllThreads() {
        try {
            Set<StackTraces> allThreads = StackTraces.getAllStackTracesInstances();
            List<Map<String, Object>> threads = new ArrayList(allThreads.size());

            for (StackTraces stacks : allThreads) {
                Map<Thread, StackTraceElement[]> all = stacks.getAllStackTraces();
                if (all != null) {
                    for (Map.Entry<Thread, StackTraceElement[]> entry : all.entrySet()) {
                        Thread t = entry.getKey();
                        StackTraceElement[] stack = entry.getValue();
                        String name = t.getName();
                        long tid = t.getId();
                        long threadCpuTime = threadBean.getThreadCpuTime(tid);
                        Map<String, Object> threadInfo = new HashMap();
                        threadInfo.put("stack", stack);
                        threadInfo.put("name", name);
                        threadInfo.put("tid", tid);
                        threadInfo.put("threadCpuTime", threadCpuTime);
                        threads.add(threadInfo);
                    }
                }
            }
            return threads.toArray(new Map[0]);
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
        return new Map[0];
    }

    private String threadDump(StackTraces stacks) {
        Map<Thread, StackTraceElement[]> all = stacks.getAllStackTraces();
        if (all == null) {
            return "Thread dump EMPTY";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Thread dump:\n"); // NOI18N
        for (Map.Entry<Thread, StackTraceElement[]> entry : all.entrySet()) {
            sb.append(entry.getKey().getName()).append('\n');
            if (entry.getValue() == null) {
                sb.append("  no information\n"); // NOI18N
                continue;
            }
            for (StackTraceElement stackTraceElement : entry.getValue()) {
                sb.append("  ");
                sb.append(stackTraceElement.getClassName()).append('.');
                sb.append(stackTraceElement.getMethodName()).append(':');
                sb.append(stackTraceElement.getLineNumber()).append('\n');
            }
        }
        return sb.toString();
    }

    @Override
    public boolean isStackTracesEnabled() {
        return !StackTraces.getAllStackTracesInstances().isEmpty();
    }

    @Override
    public Map<String, Object>[] heapHistogram() {
        Set<HeapHistogram> all = HeapHistogram.getAllHeapHistogramInstances();

        for (HeapHistogram histo : all) {
            return histo.getHeapHistogram();
        }
        return new Map[0];
    }

    @Override
    public boolean isHeapHistogramEnabled() {
        return !HeapHistogram.getAllHeapHistogramInstances().isEmpty();
    }
}
