/*
 * Copyright (c) 2010, 2023, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.jfluid.results.cpu;

import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.graalvm.visualvm.lib.jfluid.filters.InstrumentationFilter;
import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;
import org.graalvm.visualvm.lib.jfluid.global.ProfilingSessionStatus;
import org.graalvm.visualvm.lib.jfluid.results.RuntimeCCTNode;
import org.graalvm.visualvm.lib.jfluid.results.cpu.cct.nodes.MethodCPUCCTNode;
import org.graalvm.visualvm.lib.jfluid.results.cpu.cct.nodes.TimedCPUCCTNode;

/**
 *
 * @author Jaroslav Bachorik, Tomas Hurka
 */
public class StackTraceSnapshotBuilder {

    static final char NAME_SIG_SPLITTER = '|';
    private static final StackTraceElement[] NO_STACK_TRACE = new StackTraceElement[0];
    private static final boolean COLLECT_TWO_TIMESTAMPS = true;
    private static final Set<MethodInfo> knownBLockingMethods = new HashSet(Arrays.asList(new MethodInfo[] {
        new MethodInfo("java.net.PlainSocketImpl", "socketAccept[native]"), // NOI18N
        new MethodInfo("java.net.PlainSocketImpl", "socketAccept[native](java.net.SocketImpl) : void"), // NOI18N
        new MethodInfo("sun.awt.windows.WToolkit", "eventLoop[native]"), // NOI18N
        new MethodInfo("sun.awt.windows.WToolkit", "eventLoop[native]() : void"), // NOI18N
        new MethodInfo("java.lang.UNIXProcess", "waitForProcessExit[native]"), // NOI18N
        new MethodInfo("java.lang.UNIXProcess", "waitForProcessExit[native](int) : int"), // NOI18N
        new MethodInfo("sun.awt.X11.XToolkit", "waitForEvents[native]"), // NOI18N
        new MethodInfo("sun.awt.X11.XToolkit", "waitForEvents[native](long) : void"), // NOI18N
        new MethodInfo("apple.awt.CToolkit", "doAWTRunLoop[native]"), // NOI18N
        new MethodInfo("apple.awt.CToolkit", "doAWTRunLoop[native](long, boolean, boolean) : void"), // NOI18N
        new MethodInfo("java.lang.Object", "wait[native]"), // NOI18N
        new MethodInfo("java.lang.Object", "wait[native](long) : void"), // NOI18N
        new MethodInfo("java.lang.Thread", "sleep[native]"), // NOI18N
        new MethodInfo("java.lang.Thread", "sleep[native](long) : void"), // NOI18N
        new MethodInfo("sun.net.dns.ResolverConfigurationImpl","notifyAddrChange0[native]"), // NOI18N
        new MethodInfo("sun.net.dns.ResolverConfigurationImpl","notifyAddrChange0[native]() : int"), // NOI18N
        new MethodInfo("java.lang.ProcessImpl","waitFor[native]"), // NOI18N
        new MethodInfo("java.lang.ProcessImpl","waitFor[native]() : int"), // NOI18N
        new MethodInfo("sun.nio.ch.EPollArrayWrapper","epollWait[native]"), // NOI18N
        new MethodInfo("sun.nio.ch.EPollArrayWrapper","epollWait[native](long, int, long, int) : int"), // NOI18N
        new MethodInfo("java.net.DualStackPlainSocketImpl","accept0[native]"), // NOI18N
        new MethodInfo("java.net.DualStackPlainSocketImpl","accept0[native](int, java.net.InetSocketAddress[]) : int"), // NOI18N
        new MethodInfo("java.lang.ProcessImpl","waitForInterruptibly[native]"), // NOI18N
        new MethodInfo("java.lang.ProcessImpl","waitForInterruptibly[native](long) : void"), // NOI18N
        new MethodInfo("sun.print.Win32PrintServiceLookup","notifyPrinterChange[native]"), // NOI18N
        new MethodInfo("sun.print.Win32PrintServiceLookup","notifyPrinterChange[native](long) : int"), // NOI18N
        new MethodInfo("java.net.DualStackPlainSocketImpl","waitForConnect[native]"), // NOI18N
        new MethodInfo("java.net.DualStackPlainSocketImpl","waitForConnect[native](int, int) : void"), // NOI18N
        new MethodInfo("sun.nio.ch.KQueueArrayWrapper","kevent0[native]"), // NOI18N
        new MethodInfo("sun.nio.ch.KQueueArrayWrapper","kevent0[native](int, long, int, long) : int"), // NOI18N
        new MethodInfo("sun.nio.ch.WindowsSelectorImpl$SubSelector","poll0[native]"), // NOI18N
        new MethodInfo("sun.nio.ch.WindowsSelectorImpl$SubSelector","poll0[native](long, int, int[], int[], int[], long) : int"), // NOI18N
        new MethodInfo("java.net.PlainSocketImpl", "socketConnect[native]"), // NOI18N
        new MethodInfo("java.net.PlainSocketImpl", "socketConnect[native](java.net.InetAddress, int, int) : void"), // NOI18N
        new MethodInfo("sun.nio.ch.ServerSocketChannelImpl", "accept0[native]"), // NOI18N
        new MethodInfo("sun.nio.ch.ServerSocketChannelImpl", "accept0[native](java.io.FileDescriptor, java.io.FileDescriptor, java.net.InetSocketAddress[]) : int"), // NOI18N
        new MethodInfo("java.lang.ref.Reference", "waitForReferencePendingList[native]"), // NOI18N
        new MethodInfo("java.lang.ref.Reference", "waitForReferencePendingList[native]() : void"), // NOI18N
        new MethodInfo("sun.nio.fs.LinuxWatchService", "poll[native]"), // NOI18N
        new MethodInfo("sun.nio.fs.LinuxWatchService", "poll[native](int, int) : int"), // NOI18N
        new MethodInfo("sun.nio.ch.Net", "accept[native]"), // NOI18N
        new MethodInfo("sun.nio.ch.Net", "accept[native](java.io.FileDescriptor, java.io.FileDescriptor, java.net.InetSocketAddress[]) : int"), // NOI18N
        new MethodInfo("sun.nio.ch.Net", "poll[native]"), // NOI18N
        new MethodInfo("sun.nio.ch.Net", "poll[native](java.io.FileDescriptor, int, long) : int"), // NOI18N
        new MethodInfo("sun.nio.ch.Net", "connect0[native]"), // NOI18N
        new MethodInfo("sun.nio.ch.Net", "connect0[native](boolean, java.io.FileDescriptor, java.net.InetAddress, int) : int"), // NOI18N
        new MethodInfo("java.lang.ProcessHandleImpl", "waitForProcessExit0[native]"), // NOI18N
        new MethodInfo("java.lang.ProcessHandleImpl", "waitForProcessExit0[native](long, boolean) : int"), // NOI18N
        new MethodInfo("java.net.PlainSocketImpl", "accept0[native]"), // NOI18N
        new MethodInfo("java.net.PlainSocketImpl", "accept0[native](int, java.net.InetSocketAddress[]) : int"), // NOI18N
        new MethodInfo("sun.nio.ch.KQueue", "keventPoll[native]"), // NOI18N
        new MethodInfo("sun.nio.ch.KQueue", "keventPoll[native](int, long, int) : int"), // NOI18N
        new MethodInfo("sun.nio.ch.KQueue", "poll[native]"), // NOI18N
        new MethodInfo("sun.nio.ch.KQueue", "poll[native](int, long, int, long) : int"), // NOI18N
        new MethodInfo("sun.nio.ch.UnixDomainSockets", "accept0[native]"), // NOI18N
        new MethodInfo("sun.nio.ch.UnixDomainSockets", "accept0[native](java.io.FileDescriptor, java.io.FileDescriptor, java.lang.Object[]) : int"), // NOI18N
        new MethodInfo("sun.nio.ch.EPoll", "wait[native]"), // NOI18N
        new MethodInfo("sun.nio.ch.EPoll", "wait[native](int, long, int, int) : int"), // NOI18N
    }));

    private InstrumentationFilter filter;
    
    static class MethodInfo {
        
        final String className;
        final String methodName;
        final String signature;
        final boolean isNative;
        
        MethodInfo(String className, String methodName) {
            this.className = className;
            this.methodName = methodName;
            signature = "";
            isNative = false;
        }
        
        MethodInfo(StackTraceElement element) {
            isNative = element.isNativeMethod();
            final String nativeSuffix = "[native]"; // NOI18N
            String methodAndSigName = element.getMethodName();
            String method;
            int index = methodAndSigName.indexOf(NAME_SIG_SPLITTER);
            if (index > 0) {
                method = methodAndSigName.substring(0, index);
                signature = methodAndSigName.substring(index+1);
            } else {
                method = methodAndSigName;
                signature = "";
            }
            if (isNative) {
                index = method.indexOf('(');    // NOI18N
                if (index > 0) {
                    methodName = new StringBuilder(method).insert(index, nativeSuffix).toString();
                } else {
                    methodName = method + nativeSuffix;
                }
            } else {
                methodName = method;
            }
            className = element.getClassName();
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final MethodInfo other = (MethodInfo) obj;
            if (!Objects.equals(className, other.className)) {
                return false;
            }
            if (!Objects.equals(methodName, other.methodName)) {
                return false;
            }
            if (!Objects.equals(signature, other.signature)) {
                return false;
            }
            return true;
        }
        
        @Override
        public int hashCode() {
            int hash = 5;
            hash = 29 * hash + (this.className != null ? this.className.hashCode() : 0);
            hash = 29 * hash + (this.methodName != null ? this.methodName.hashCode() : 0);
            hash = 29 * hash + (this.signature != null ? this.signature.hashCode() : 0);
            return hash;
        }
        
        @Override
        public String toString() {
            return className + "." + methodName + "()";
        }
    }
    
    static class SampledThreadInfo {
        private StackTraceElement[] stackTrace;
        private Thread.State state;
        private String threadName;
        private long threadId;
        private long threadCpuTime;
 
        SampledThreadInfo(String tn, long tid, Thread.State ts, StackTraceElement[] st, InstrumentationFilter filter) {
            this (tn, tid,ts, st, -1, filter);
        }

        SampledThreadInfo(String tn, long tid, Thread.State ts, StackTraceElement[] st, long tct, InstrumentationFilter filter) {
            threadName = tn;
            threadId = tid;
            state = ts;
            stackTrace = st;
            threadCpuTime = tct;
            if (state == Thread.State.RUNNABLE && containsKnownBlockingMethod(st)) { // known blocking method -> change state to waiting
                state = Thread.State.WAITING;
            }
            if (filter != null) {
                int i;
                
                for (i=0; i<st.length; i++) {
                    StackTraceElement frame = st[i];
                    if (filter.passes(frame.getClassName().replace('.','/'))) { // NOI18N
                        if (i>1) {
                            stackTrace = new StackTraceElement[st.length-i+1];
                            System.arraycopy(st,i-1,stackTrace,0,stackTrace.length);
                        }
                        break;
                    }
                }
                if (i==st.length) {
                    stackTrace = NO_STACK_TRACE;
                }
            }
        }
        
        SampledThreadInfo(java.lang.management.ThreadInfo info, InstrumentationFilter filter) {
            this(info.getThreadName(), info.getThreadId(), info.getThreadState(), info.getStackTrace(), filter);
        }
        
        private static boolean containsKnownBlockingMethod(StackTraceElement[] stackTrace) {
            if (stackTrace.length > 0) {
                MethodInfo firstFrame = new MethodInfo(stackTrace[0]);
                return knownBLockingMethods.contains(firstFrame);
            }
            return false;
        }

        private StackTraceElement[] getStackTrace() {
            return stackTrace;
        }

        State getThreadState() {
            return state;
        }

        private String getThreadName() {
            return threadName;
        }

        private long getThreadId() {
            return threadId;
        }
        
    }
    
    final List<Long> threadIds = new ArrayList<>();
    final List<String> threadNames = new ArrayList<>();
    final List<byte[]> threadCompactData = new ArrayList<>();
    final List<MethodInfo> methodInfos = new ArrayList<>();
    final Map<MethodInfo,Integer> methodInfoMap = new HashMap<>();
    final MethodInfoMapper mapper = new MethodInfoMapper() {
        
        @Override
        public String getInstrMethodClass(int methodId) {
            return methodInfos.get(methodId).className;
        }
        
        @Override
        public String getInstrMethodName(int methodId) {
            return methodInfos.get(methodId).methodName;
        }
        
        @Override
        public String getInstrMethodSignature(int methodId) {
            return methodInfos.get(methodId).signature;
        }
        
        @Override
        public int getMaxMethodId() {
            return methodInfos.size();
        }
        
        @Override
        public int getMinMethodId() {
            return 0;
        }
    };
    final CPUCallGraphBuilder ccgb;
    final ProfilingSessionStatus status;
    final Object lock = new Object();
    final Object stampLock = new Object();
    // @GuardedBy stampLock
    long currentDumpTimeStamp = -1L;
    final AtomicReference<Map<Long, SampledThreadInfo>> lastStackTrace = new AtomicReference<>(Collections.EMPTY_MAP);
    int stackTraceCount = 0;
    //    int builderBatchSize;
    final Set<String> ignoredThreadNames = new HashSet<>();
    final Map<Long,Long> threadtimes = new HashMap();
    
    {
        registerNewMethodInfo(new MethodInfo("Thread","")); // NOI18N
    }
    
    public StackTraceSnapshotBuilder() {
        this(1, null);
    }
    
    StackTraceSnapshotBuilder(CPUCallGraphBuilder b, InstrumentationFilter f, ProfilingSessionStatus s) {
        //        builderBatchSize = batchSize;
        filter = f;
        setDefaultTiming();
        ccgb = b;
        status = s;
    }
    
    public StackTraceSnapshotBuilder(int batchSize, InstrumentationFilter f) {
        //        builderBatchSize = batchSize;
        filter = f;
        setDefaultTiming();
        ccgb = new StackTraceCallGraphBuilder(mapper, f);
        status = null;
    }
    
    final public void setIgnoredThreads(Set<String> ignoredThreadNames) {
        synchronized (lock) {
            this.ignoredThreadNames.clear();
            this.ignoredThreadNames.addAll(ignoredThreadNames);
        }
    }
    
    final void addStacktrace(SampledThreadInfo[] threads, long dumpTimeStamp) throws IllegalStateException {
        long timediff = processDumpTimeStamp(dumpTimeStamp);
        
        if (timediff < 0) return;
        synchronized (lock) {
            Map<Long,SampledThreadInfo> tinfoMap = new HashMap();
            
            for (SampledThreadInfo tinfo : threads) {
                tinfoMap.put(tinfo.getThreadId(),tinfo);
            }
            processThreadDump(timediff, dumpTimeStamp, tinfoMap);
            //            if (stackTraceCount%builderBatchSize == 0) {
            //                ccgb.doBatchStop();
            //            }
        }
    }

    final public void addStacktrace(Map<String, Object>[] infoMap, long dumpTimeStamp) throws IllegalStateException {
        List<SampledThreadInfo> threads = new ArrayList<>(infoMap.length);

        for (Map<String,Object> threadInfo : infoMap) {
            String name = (String) threadInfo.get("name");
            StackTraceElement[] stack = (StackTraceElement[]) threadInfo.get("stack");
            long tid = (Long) threadInfo.get("tid");
            Long threadCpuTime = (Long) threadInfo.get("threadCpuTime");
            State state = (State) threadInfo.get("state");

            if (threadCpuTime == null) {
                threadCpuTime = Long.valueOf(-1);   // no thread cpu time
            }
            if (state == null) {
                state = State.RUNNABLE;
            }
            SampledThreadInfo i = new SampledThreadInfo(name, tid, state, stack, threadCpuTime.longValue(), filter);

            threads.add(i);
        }
        addStacktrace(threads.toArray(new SampledThreadInfo[0]), dumpTimeStamp);
    }
    
    final public void addStacktrace(java.lang.management.ThreadInfo[] threads, long dumpTimeStamp) throws IllegalStateException {
        long timediff = processDumpTimeStamp(dumpTimeStamp);
        
        if (timediff < 0) return;
        synchronized (lock) {
            Map<Long,SampledThreadInfo> tinfoMap = new HashMap();
            
            //            if (stackTraceCount%builderBatchSize == 0) {
            //                ccgb.doBatchStart();
            //            }
            for (java.lang.management.ThreadInfo tinfo : threads) {
                if (tinfo != null) {
                    tinfoMap.put(tinfo.getThreadId(),new SampledThreadInfo(tinfo,filter));
                }
            }
            processThreadDump(timediff, dumpTimeStamp, tinfoMap);
            //            if (stackTraceCount%builderBatchSize == 0) {
            //                ccgb.doBatchStop();
            //            }
        }
    }

    private void processThreadDump(final long timediff, final long dumpTimeStamp, final Map<Long, SampledThreadInfo> tinfoMap) throws IllegalStateException {
        Iterator<Map.Entry<Long,SampledThreadInfo>> tinfoIt = tinfoMap.entrySet().iterator();
        
        while (tinfoIt.hasNext()) {
            Map.Entry<Long,SampledThreadInfo> tinfoEntry = tinfoIt.next();
            SampledThreadInfo tinfo = tinfoEntry.getValue();
            String tname = tinfo.getThreadName();
            
            if (ignoredThreadNames.contains(tname)) {
                tinfoIt.remove();
                continue;
            }
            Thread.State newState = tinfo.getThreadState();
            // ignore threads, which has not yet started.
            if (Thread.State.NEW.equals(newState)) {
                tinfoIt.remove();
                continue;
            }
            
            long threadId = tinfo.getThreadId();
            if (!threadIds.contains(threadId)) {
                long threadCpuTime = tinfo.threadCpuTime;
                threadIds.add(threadId);
                threadNames.add(tname);
                ccgb.newThread((int) threadId, tname, "<none>");
                if (threadCpuTime != -1) {
                    threadtimes.put(threadId,threadCpuTime);
                } else {
                    threadtimes.put(threadId,dumpTimeStamp);
                }
            }
            StackTraceElement[] newElements = tinfo.getStackTrace();
            SampledThreadInfo oldTinfo = lastStackTrace.get().get(threadId);
            StackTraceElement[] oldElements = NO_STACK_TRACE;
            Thread.State oldState = Thread.State.NEW;
            
            if (oldTinfo != null) {
                oldElements = oldTinfo.getStackTrace();
                oldState = oldTinfo.getThreadState();
            }
            processDiffs((int) threadId, oldElements, newElements, dumpTimeStamp, tinfo.threadCpuTime, timediff, oldState, newState);
        }
        
        for (SampledThreadInfo oldTinfo : lastStackTrace.get().values()) {            
            if (!tinfoMap.containsKey(oldTinfo.getThreadId())) {
                Thread.State oldState = oldTinfo.getThreadState();
                Thread.State newState = Thread.State.TERMINATED;
                processDiffs((int) oldTinfo.getThreadId(), oldTinfo.getStackTrace(), NO_STACK_TRACE, dumpTimeStamp, oldTinfo.threadCpuTime, timediff, oldState, newState);
            }
        }
        
        lastStackTrace.set(tinfoMap);
        
        stackTraceCount++;
    }

    private long processDumpTimeStamp(long dumpTimeStamp) {
        long timediff;
        synchronized(stampLock) {
            if (dumpTimeStamp <= currentDumpTimeStamp) {
                // issue #171756 - ignore misplaced samples
                // montonicity of System.nanoTime is not presently guaranteed (CR 6458294)
                // throw new IllegalStateException("Adding stacktrace with timestamp " + dumpTimeStamp + " is not allowed after a stacktrace with timestamp " + currentDumpTimeStamp + " has been added");
                return -1;
            }
            timediff = dumpTimeStamp - currentDumpTimeStamp;
            currentDumpTimeStamp = dumpTimeStamp;
        }
        return timediff;
    }
    
    private void processDiffs(int threadId, StackTraceElement[] oldElements, StackTraceElement[] newElements, long timestamp, long threadCpuTime, long timediff, Thread.State oldState, Thread.State newState) throws IllegalStateException {
        assert newState != Thread.State.NEW : "Invalid thread state " + newState.name() + " for taking a stack trace"; // just to be sure
        if (oldState == Thread.State.TERMINATED && newState != Thread.State.TERMINATED) {
            throw new IllegalStateException("Thread has already been set to " + Thread.State.TERMINATED.name() + " - stack trace can not be taken");
        }
        //        switch (oldState) {
        //            case NEW: {
        //                switch (newState) {
        //                    case RUNNABLE: {
        //                        processDiffs(threadId, oldElements, newElements, timestamp);
        //                        break;
        //                    }
        //                }
        //                break;
        //            }
        //            case RUNNABLE: {
        //                break;
        //            }
        //            case WAITING:
        //            case TIMED_WAITING: {
        //                ccgb.waitExit(threadId, timestamp, threadtime);
        //                break;
        //            }
        //            case BLOCKED: {
        //                ccgb.monitorExit(threadId, timestamp, threadtime);
        //                break;
        //            }
        //        }
        long threadtime;
        if (threadCpuTime == -1) {
            threadtime = threadtimes.get(Long.valueOf(threadId));
            if (oldState == Thread.State.RUNNABLE) {
                threadtime += timediff;
                threadtimes.put(Long.valueOf(threadId),threadtime);
            }
        } else {
            threadtime = threadCpuTime;
        }
        //        if (newState == Thread.State.RUNNABLE && newElements.length > 0) {
        //            StackTraceElement top = newElements[0];
        //            if (top.getClassName().equals("java.lang.Object") && top.isNativeMethod() && top.getMethodName().equals("wait")) {
        //                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!");
        //                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!");
        //            }
        //        }
        processDiffs(threadId, oldElements, newElements, timestamp, threadtime);
        //        switch (newState) {
        //            case RUNNABLE: {
        //                break;
        //            }
        //            case WAITING:
        //            case TIMED_WAITING: {
        //                ccgb.waitEntry(threadId, timestamp, threadtime);
        //                break;
        //            }
        //            case BLOCKED: {
        //                ccgb.monitorEntry(threadId, timestamp, threadtime);
        //                break;
        //            }
        //        }
    }
    
    private void processDiffs(int threadId, StackTraceElement[] oldElements, StackTraceElement[] newElements, long timestamp, long threadtimestamp) throws IllegalStateException {
        if (oldElements.length == 0 && newElements.length == 0) {
            return;
        }
        
        int newMax = newElements.length - 1;
        int oldMax = oldElements.length - 1;
        int globalMax = Math.max(oldMax, newMax);
        
        List<StackTraceElement> newElementsList = Collections.EMPTY_LIST;
        List<StackTraceElement> oldElementsList = Collections.EMPTY_LIST;
        
        for (int iteratorIndex = 0; iteratorIndex <= globalMax; iteratorIndex++) {
            StackTraceElement oldElement = oldMax >= iteratorIndex ? oldElements[oldMax - iteratorIndex] : null;
            StackTraceElement newElement = newMax >= iteratorIndex ? newElements[newMax - iteratorIndex] : null;
            
            if (oldElement != null && newElement != null) {
                if (!oldElement.equals(newElement)) {
                    if (hasSameMethodInfo(oldElement,newElement)) {
                        iteratorIndex++;
                    }
                    newElementsList = Arrays.asList(newElements).subList(0, newMax - iteratorIndex + 1);
                    oldElementsList = Arrays.asList(oldElements).subList(0, oldMax - iteratorIndex + 1);
                    break;
                }
            } else if (oldElement == null && newElement != null) {
                newElementsList = Arrays.asList(newElements).subList(0, newMax - iteratorIndex + 1);
                break;
                
            } else if (oldElement != null && newElement == null) {
                oldElementsList = Arrays.asList(oldElements).subList(0, oldMax - iteratorIndex + 1);
                break;
                
            }
        }
        
        // !!! The order is important - first we need to exit from the
        // already entered methods and only then we can enter the new ones !!!
        addMethodExits(threadId, oldElementsList, timestamp, threadtimestamp, newElements.length == 0);
        addMethodEntries(threadId, newElementsList, timestamp, threadtimestamp, oldElements.length == 0);
    }
    
    private void addMethodEntries(int threadId, List<StackTraceElement> elements, long timestamp, long threadtimestamp, boolean asRoot) throws IllegalStateException {
        boolean inRoot = false;
        ListIterator<StackTraceElement> reverseIt = elements.listIterator(elements.size());
        
        while(reverseIt.hasPrevious()) {
            StackTraceElement element = reverseIt.previous();
            MethodInfo mi = new MethodInfo(element);
            Integer mId = methodInfoMap.get(mi);
            if (mId == null) {
                mId = registerNewMethodInfo(mi);
                if (status != null) {
                    String method = mi.methodName;
                    int index = method.indexOf('(');
                    if (index > 0) {
                        method = method.substring(0,index);
                    }
                    status.updateInstrMethodsInfo(mi.className,0,method,mi.signature);
                }
            }
            
            if (asRoot && !inRoot) {
                inRoot = true;
                ccgb.methodEntry(mId.intValue(), threadId, CPUCallGraphBuilder.METHODTYPE_ROOT, timestamp, threadtimestamp, null, null);
            } else {
                ccgb.methodEntry(mId.intValue(), threadId, CPUCallGraphBuilder.METHODTYPE_NORMAL, timestamp, threadtimestamp, null, null);
            }
            
        }
    }

    private Integer registerNewMethodInfo(final MethodInfo mi) {
        Integer index = Integer.valueOf(methodInfos.size());
        
        methodInfos.add(mi);
        methodInfoMap.put(mi,index);
        return index;
    }
    
    private void addMethodExits(int threadId, List<StackTraceElement> elements, long timestamp, long threadtimestamp, boolean asRoot) throws IllegalStateException {
        int rootIndex = elements.size();
        for (StackTraceElement element : elements) {
            MethodInfo mi = new MethodInfo(element);
            Integer index = methodInfoMap.get(mi);
            if (index == null) {
                System.err.println("*** Not found: " + mi);
                throw new IllegalStateException();
            }
            
            if (asRoot && --rootIndex == 0) {
                ccgb.methodExit(index.intValue(), threadId, CPUCallGraphBuilder.METHODTYPE_ROOT, timestamp, threadtimestamp, null);
            } else {
                ccgb.methodExit(index.intValue(), threadId, CPUCallGraphBuilder.METHODTYPE_NORMAL, timestamp, threadtimestamp, null);
            }
        }
    }
    
    private boolean hasSameMethodInfo(StackTraceElement oldElement, StackTraceElement newElement) {
        MethodInfo oldMethodInfo = new MethodInfo(oldElement);
        MethodInfo newMethodInfo = new MethodInfo(newElement);
        
        return oldMethodInfo.equals(newMethodInfo);
    }
    
    private void setDefaultTiming() {
        // Ugly code to set default CPU calibration data
        ProfilingSessionStatus pss = new ProfilingSessionStatus();
        pss.timerCountsInSecond[0] = InstrTimingData.DEFAULT.timerCountsInSecond0;
        pss.timerCountsInSecond[1] = InstrTimingData.DEFAULT.timerCountsInSecond1;
        pss.currentInstrType = CommonConstants.INSTR_RECURSIVE_FULL;
        pss.absoluteTimerOn = true;
        pss.threadCPUTimerOn = true;
        TimingAdjusterOld.getInstance(pss);        
    }

    /**
     * Creates CPUResultsSnapsot
     * @param since time in milliseconds
     * @return snapshot
     * @throws org.graalvm.visualvm.lib.jfluid.results.cpu.CPUResultsSnapshot.NoDataAvailableException 
     */
    public final CPUResultsSnapshot createSnapshot(
            long since) throws CPUResultsSnapshot.NoDataAvailableException {
        if (stackTraceCount < 1) {
            throw new CPUResultsSnapshot.NoDataAvailableException();
        }
        
        String[] instrMethodClasses;
        String[] instrMethodNames;
        String[] instrMethodSigs;
        int miCount;
        synchronized (lock) {
            miCount = methodInfos.size();
            instrMethodClasses = new String[methodInfos.size()];
            instrMethodNames = new String[methodInfos.size()];
            instrMethodSigs = new String[methodInfos.size()];
            
            int counter = 0;
            for (MethodInfo mi : methodInfos) {
                instrMethodClasses[counter] = mi.className;
                instrMethodNames[counter] = mi.methodName;
                instrMethodSigs[counter] = mi.signature;
                counter++;
            }
            return new CPUResultsSnapshot(since, System.currentTimeMillis(),
                    ccgb, ccgb.isCollectingTwoTimeStamps(), filter,
                    instrMethodClasses, instrMethodNames, instrMethodSigs, miCount);
        }
    }
    
    public final void reset() {
        synchronized (lock) {
            ccgb.reset();
            if (status != null) {
                status.resetInstrClassAndMethodInfo();
            }
            methodInfos.clear();
            methodInfoMap.clear();
            threadIds.clear();
            threadNames.clear();
            stackTraceCount = 0;
            lastStackTrace.set(Collections.EMPTY_MAP);
            registerNewMethodInfo(new MethodInfo("Thread","")); // NOI18N
            synchronized(stampLock) {
                currentDumpTimeStamp = -1L;
            }
        }
    }
    
    public MethodInfoMapper getMapper() {
        return mapper;
    }
    
    public RuntimeCCTNode getAppRootNode() {
        return ccgb.getAppRootNode();
    }
    
    public boolean collectionTwoTimeStamps() {
        return COLLECT_TWO_TIMESTAMPS;
    }
    
    public InstrumentationFilter getFilter() {
        return filter;
    }
    
    private class StackTraceCallGraphBuilder extends CPUCallGraphBuilder {

        StackTraceCallGraphBuilder (MethodInfoMapper mapper, InstrumentationFilter filter) {
            setFilter(filter);
            setMethodInfoMapper(mapper);
        }

        @Override
        protected boolean isCollectingTwoTimeStamps() {
            return COLLECT_TWO_TIMESTAMPS;
        }

        @Override
        protected boolean isReady() {
            return true;
        }

        @Override
        protected long getDumpAbsTimeStamp() {
            synchronized (stampLock) {
                return currentDumpTimeStamp;
            }
        }

        @Override
        protected void applyDiffAtGetResultsMoment(ThreadInfo ti) {
            long time0 = getDumpAbsTimeStamp();
            long diff0 = time0 - ti.topMethodEntryTime0;
            long diff1 = getThreadTime(ti, time0) - ti.topMethodEntryTime1;

            if (diff0<0) diff0=0;
            if (diff1<0) diff1=0;
            if (diff0>0 || diff1>0) {
                applyDiffToTopNode(ti, diff0, diff1);
                ti.diffAtGetResultsMoment0 = diff0;
                ti.diffAtGetResultsMoment1 = diff1;
            }
        }

        @Override
        protected void undoDiffAtGetResultsMoment(ThreadInfo ti) {
            if (ti.diffAtGetResultsMoment0>0) {
                applyDiffToTopNode(ti, -ti.diffAtGetResultsMoment0, -ti.diffAtGetResultsMoment1);
                ti.diffAtGetResultsMoment0 = 0;
            }
        }

        private long getThreadTime(ThreadInfo ti, long time0) {
            if (isCollectingTwoTimeStamps()) {
                SampledThreadInfo sti = lastStackTrace.get().get(Long.valueOf(ti.threadId));

                if (sti!=null) {
                    if (sti.threadCpuTime != -1) {
                        return sti.threadCpuTime;
                    }
                    if (sti.getThreadState() == Thread.State.RUNNABLE) {
                        return threadtimes.get(Long.valueOf(ti.threadId));
                    }
                }
            }
            return 0;
        }

        private void applyDiffToTopNode(ThreadInfo ti, long diff0, long diff1) {
            TimedCPUCCTNode top = ti.peek();

            if (top instanceof MethodCPUCCTNode) {
                top.addNetTime0(diff0);
                if (isCollectingTwoTimeStamps()) {
                    top.addNetTime1(diff1);
                }
            }
        }
    }
}
