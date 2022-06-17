/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.tools.profiler.CPUSampler.Mode;
import com.oracle.truffle.tools.profiler.HeapMonitor;
import com.oracle.truffle.tools.profiler.HeapSummary;
import com.oracle.truffle.tools.profiler.StackTraceEntry;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Field;
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
import sun.misc.Unsafe;

/**
 *
 * @author Tomas Hurka
 */
public class Truffle implements TruffleMBean {

    private static final String POLYGLOTENGINEIMPL_CLASS_NAME = "com.oracle.truffle.polyglot.PolyglotEngineImpl";

    private ThreadMXBean threadBean;
    private Method Engine_findActiveEngines;
    private Map engines;
    private Unsafe unsafe;
    private boolean trackFlags;

    public Truffle(Unsafe u) {
        unsafe = u;
        threadBean = ManagementFactory.getThreadMXBean();
        Engine_findActiveEngines = getFindActiveEngines();
        if (Engine_findActiveEngines == null) {
            engines = getEngines();
            if (engines == null) {
                throw new IllegalStateException();
            }
        }
        try {
            for (CPUSampler stacks : getAllStackTracesInstances()) {
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
                        TruffleStackTrace stack = getStackTraceElements(entry.getValue());
                        String name = t.getName();
                        Map<String, Object> threadInfo = new HashMap();
                        threadInfo.put("stack", stack.stack);
                        if (trackFlags) {
                            threadInfo.put("flags", stack.flags);
                        }
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

    private Method getFindActiveEngines() {
        try {
            Method m = Engine.class.getDeclaredMethod("findActiveEngines");
            m.setAccessible(true);
            return m;
        } catch (SecurityException ex) {
            Logger.getLogger(Truffle.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(Truffle.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            if (TruffleJMX.DEBUG) {
                Logger.getLogger(Truffle.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return null;
    }

    private Map getEngines() {
        Map engines = null;
        try {
            engines = getEnginesFromClass(Engine.class);
            if (engines == null) {
                Class POLY_CLASS = Class.forName(POLYGLOTENGINEIMPL_CLASS_NAME);
                engines = getEnginesFromClass(POLY_CLASS);
            }
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Truffle.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(Truffle.class.getName()).log(Level.SEVERE, null, ex);
        }
        return engines;
    }

    private Map getEnginesFromClass(Class engineClass) {
        try {
            Field f = engineClass.getDeclaredField("ENGINES");
            return getEnginesFromField(f);
        } catch (NoSuchFieldException ex) {
            Logger.getLogger(Truffle.class.getName()).log(TruffleJMX.DEBUG ? Level.INFO : Level.FINE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(Truffle.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private Map getEnginesFromField(Field f) {
        try {
            Object base = unsafe.staticFieldBase(f);
            return (Map) unsafe.getObject(base, unsafe.staticFieldOffset(f));
        } catch (SecurityException ex) {
            Logger.getLogger(Truffle.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private Collection<Engine> getAllEngineInstances() {
        try {
            if (Engine_findActiveEngines == null) {
                Collection<Engine> en = new ArrayList();
                for (Object o : engines.keySet()) {
                    Engine e;

                    if (o instanceof Engine) {
                        e = (Engine) o;
                    } else {
                        Field cf = TruffleJMX.getDeclaredField(o, "creatorApi", "api");
                        e = (Engine) unsafe.getObject(o, unsafe.objectFieldOffset(cf));
                    }
                    en.add(e);
                }
                return en;
            } else {
                return (Collection<Engine>) Engine_findActiveEngines.invoke(null);
            }
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(Truffle.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(Truffle.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvocationTargetException ex) {
            Logger.getLogger(Truffle.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchFieldException ex) {
            Logger.getLogger(Truffle.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(Truffle.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Collections.EMPTY_LIST;
    }

    private Collection<CPUSampler> getAllStackTracesInstances() {
        List<CPUSampler> allInstances = new ArrayList();
        Collection<Engine> all = getAllEngineInstances();

        for (Engine engine : all) {
            if (engine == null) continue;
            CPUSampler sampler = CPUSampler.find(engine);

            if (sampler != null) {
                allInstances.add(sampler);
            }
        }
        return allInstances;
    }

    private static final int COMPILED  = 1;  // 0001
    private static final int INLINED   = 2;  // 0010

    private TruffleStackTrace getStackTraceElements(List<StackTraceEntry> entries) {
        StackTraceElement[] stack = new StackTraceElement[entries.size()];
        byte[] flags = new byte[entries.size()];

        for (int i = 0; i < entries.size(); i++) {
            StackTraceEntry entry = entries.get(i);
            stack[i] = entry.toStackTraceElement();
            flags[i] |= entry.isCompiled() ? COMPILED:0;
            flags[i] |= entry.isInlined() ? INLINED:0;
            if (TruffleJMX.DEBUG && flags[i] != 0 ) {
                System.out.println(stack[i]+" "+Integer.toHexString(flags[i])+" "+entry.isInlined());
            }
        }
        return new TruffleStackTrace(stack, flags);
    }

    @Override
    public boolean isStackTracesEnabled() {
        return !getAllStackTracesInstances().isEmpty();
    }

    private Collection<HeapMonitor> getAllHeapHistogramInstances() {
        List<HeapMonitor> allInstances = new ArrayList();
        Collection<Engine> all = getAllEngineInstances();

        for (Engine engine : all) {
            if (engine == null) continue;
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
                Map<LanguageInfo, Map<String, HeapSummary>> info = histo.takeMetaObjectSummary();
                try {
                    return toMap(info);
                } catch (Throwable ex) {
                    Logger.getLogger(Truffle.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return new Map[0];
    }

    @Override
    public boolean isHeapHistogramEnabled() {
        return !getAllHeapHistogramInstances().isEmpty();
    }

    Map<String, Object>[] toMap(Map<LanguageInfo, Map<String, HeapSummary>> summaries) throws NoSuchFieldException {
        List<Map<String, Object>> heapHisto = new ArrayList<>(summaries.size());
        for (Map.Entry<LanguageInfo, Map<String, HeapSummary>> objectsByLanguage : summaries.entrySet()) {
            String langId = getLanguageId(objectsByLanguage.getKey());
            for (Map.Entry<String, HeapSummary> objectsByMetaObject : objectsByLanguage.getValue().entrySet()) {
                HeapSummary mi = objectsByMetaObject.getValue();
                Map<String, Object> metaObjMap = new HashMap<>();
                metaObjMap.put("language", langId);
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

    private String getLanguageId(Object lang) throws NoSuchFieldException, SecurityException {
        if (Engine_findActiveEngines != null) {
            return ((LanguageInfo)lang).getId();
        }
        Field f = lang.getClass().getDeclaredField("id");
        String lId = (String) unsafe.getObject(lang, unsafe.objectFieldOffset(f));
        return lId;
    }

    @Override
    public void setTrackFlags(boolean trackFlags) {
        this.trackFlags = trackFlags;
    }

    @Override
    public void setMode(String modeStr) {
        if ("ROOTS".equals(modeStr)) {
            setMode(Mode.ROOTS);
        } else if ("EXCLUDE_INLINED_ROOTS".equals(modeStr)) {
            setMode(Mode.STATEMENTS);
        } else if ("EXCLUDE_INLINED_ROOTS".equals(modeStr)) {
            setMode(Mode.STATEMENTS);
        }
    }

    @Override
    public boolean isModeAvailable() {
        try {
            Class.forName("com.oracle.truffle.tools.profiler.CPUSampler$Mode"); // NOI18N
        } catch (ClassNotFoundException ex) {
            return false;
        }
        return true;
    }

    private void setMode(Mode m) {
        Collection<Engine> all = getAllEngineInstances();

        for (Engine engine : all) {
            if (engine == null) continue;
            CPUSampler sampler = CPUSampler.find(engine);

            if (sampler != null) {
                sampler.setMode(m);
            }
        }
    }

    private static class TruffleStackTrace {
        private StackTraceElement[] stack;
        private byte[] flags;

        private TruffleStackTrace(StackTraceElement[] stack, byte[] flags) {
            this.stack = stack;
            this.flags = flags;
        }
    }
}
