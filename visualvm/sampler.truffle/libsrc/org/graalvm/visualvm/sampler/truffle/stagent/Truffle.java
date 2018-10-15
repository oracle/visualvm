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

import com.oracle.truffle.api.nodes.LanguageInfo;
import com.oracle.truffle.tools.profiler.CPUSampler;
import com.oracle.truffle.tools.profiler.HeapMonitor;
import com.oracle.truffle.tools.profiler.HeapSummary;
import com.oracle.truffle.tools.profiler.StackTraceEntry;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.graalvm.polyglot.Engine;

/**
 *
 * @author Tomas Hurka
 */
public class Truffle implements TruffleMBean {

    private ThreadMXBean threadBean;
    private Method Engine_findActiveEngines;

    public Truffle() {
        threadBean = ManagementFactory.getThreadMXBean();
        try {
            Engine_findActiveEngines = Engine.class.getDeclaredMethod("findActiveEngines");
            Engine_findActiveEngines.setAccessible(true);
        } catch (SecurityException ex) {
            Logger.getLogger(Truffle.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(Truffle.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            for (CPUSampler stacks : getAllStackTracesInstances()) {
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
            Collection<CPUSampler> allThreads = getAllStackTracesInstances();
            List<Map<String, Object>> threads = new ArrayList(allThreads.size());

            for (CPUSampler stacks : allThreads) {
                Map<Thread, List<StackTraceEntry>> all = stacks.takeSample();
                if (all != null) {
                    for (Map.Entry<Thread, List<StackTraceEntry>> entry : all.entrySet()) {
                        Thread t = entry.getKey();
                        long tid = t.getId();
                        long threadCpuTime = threadBean.getThreadCpuTime(tid);
                        StackTraceElement[] stack = getStackTraceElements(entry.getValue());
                        String name = t.getName();
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

    private String threadDump(CPUSampler stacks) {
        Map<Thread, List<StackTraceEntry>> all = stacks.takeSample();
        if (all == null) {
            return "Thread dump EMPTY";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Thread dump:\n"); // NOI18N
        for (Map.Entry<Thread, List<StackTraceEntry>> entry : all.entrySet()) {
            sb.append(entry.getKey().getName()).append('\n');
            if (entry.getValue() == null) {
                sb.append("  no information\n"); // NOI18N
                continue;
            }
            for (StackTraceEntry stackTraceEntry : entry.getValue()) {
                StackTraceElement stackTraceElement = stackTraceEntry.toStackTraceElement();

                sb.append("  ");
                sb.append(stackTraceElement.getClassName()).append('.').append(stackTraceElement.getMethodName());
                String fileName = stackTraceElement.getFileName();
                int lastSep = fileName.lastIndexOf('/');

                if (lastSep != -1) {
                    fileName = fileName.substring(lastSep+1);
                }
                sb.append(" (").append(fileName).append(":").append(stackTraceElement.getLineNumber()).append(')');
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    private Collection<Engine> getAllEngineInstances() {
        try {
            return (Collection<Engine>) Engine_findActiveEngines.invoke(null);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(Truffle.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(Truffle.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvocationTargetException ex) {
            Logger.getLogger(Truffle.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Collections.EMPTY_LIST;
    }

    private Collection<CPUSampler> getAllStackTracesInstances() {
        List<CPUSampler> allInstances = new ArrayList();
        Collection<Engine> all = getAllEngineInstances();

        for (Engine engine : all) {
            CPUSampler sampler = CPUSampler.find(engine);

            if (sampler != null) {
                allInstances.add(sampler);
            }
        }
        return allInstances;
    }

    private StackTraceElement[] getStackTraceElements(List<StackTraceEntry> entries) {
        StackTraceElement[] stack = new StackTraceElement[entries.size()];

        for (int i = 0; i < entries.size(); i++) {
            stack[i] = entries.get(i).toStackTraceElement();
        }
        return stack;
    }

    @Override
    public boolean isStackTracesEnabled() {
        return !getAllStackTracesInstances().isEmpty();
    }

    private Collection<HeapMonitor> getAllHeapHistogramInstances() {
        List<HeapMonitor> allInstances = new ArrayList();
        Collection<Engine> all = getAllEngineInstances();

        for (Engine engine : all) {
            HeapMonitor heapHisto = HeapMonitor.find(engine);

            if (heapHisto != null) {
                if (!heapHisto.isCollecting()) {
                    heapHisto.setCollecting(true);
                }
                allInstances.add(heapHisto);
            }
        }
        return allInstances;
    }

    @Override
    public Map<String, Object>[] heapHistogram() {
        Collection<HeapMonitor> all = getAllHeapHistogramInstances();

        for (HeapMonitor histo : all) {
            if (histo.hasData()) {
                Map<LanguageInfo,Map<String,HeapSummary>> info = histo.takeMetaObjectSummary();
                return toMap(info);
            }
        }
        return new Map[0];
    }

    @Override
    public boolean isHeapHistogramEnabled() {
        return !getAllHeapHistogramInstances().isEmpty();
    }

    static Map<String, Object>[] toMap(Map<LanguageInfo, Map<String, HeapSummary>> summaries) {
        List<Map<String, Object>> heapHisto = new ArrayList<>(summaries.size());
        for (Map.Entry<LanguageInfo, Map<String, HeapSummary>> objectsByLanguage : summaries.entrySet()) {
            LanguageInfo language = objectsByLanguage.getKey();
            for (Map.Entry<String, HeapSummary> objectsByMetaObject : objectsByLanguage.getValue().entrySet()) {
                HeapSummary mi = objectsByMetaObject.getValue();
                Map<String, Object> metaObjMap = new HashMap<>();
                metaObjMap.put("language", language.getId());
                metaObjMap.put("name", objectsByMetaObject.getKey());
                metaObjMap.put("allocatedInstancesCount", mi.getTotalInstances());
                metaObjMap.put("bytes", mi.getTotalBytes());
                metaObjMap.put("liveInstancesCount", mi.getAliveInstances());
                metaObjMap.put("liveBytes", mi.getAliveBytes());
                heapHisto.add(metaObjMap);
            }
        }
        return heapHisto.toArray(new Map[0]);
    }
}
