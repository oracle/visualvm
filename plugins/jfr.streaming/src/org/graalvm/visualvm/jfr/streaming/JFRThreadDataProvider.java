/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.jfr.streaming;

import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedThread;
import jdk.management.jfr.RemoteRecordingStream;
import org.graalvm.visualvm.application.views.ApplicationThreadsResponseProvider;
import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;
import org.graalvm.visualvm.lib.jfluid.wireprotocol.MonitoredNumbersResponse;

/**
 *
 * @author Tomas Hurka
 */
public class JFRThreadDataProvider implements ApplicationThreadsResponseProvider.ThreadMonitoredDataResponseProvider {

    private final RemoteRecordingStream recordingStream;
    private final ThreadMXBean threadMXBean;
    private final List<JFREvent> events;
    private final List<JFRThread> newThreads;
    private final Set<Long> threadIdSet;

    JFRThreadDataProvider(RemoteRecordingStream rs, ThreadMXBean tb) {
        recordingStream = rs;
        threadMXBean = tb;
        events = new ArrayList<>();
        threadIdSet = new HashSet<>();
        newThreads = new ArrayList<>();
    }

    @Override
    public MonitoredNumbersResponse createThreadMonitoredDataResponse() {
        JFRNumbersResponse rp = new JFRNumbersResponse();

        if (threadIdSet.isEmpty()) {
            threadIdSet.addAll(fillInThreadData(rp));
            return rp;
        }
        synchronized (newThreads) {
            int[] newThreadsId = new int[newThreads.size()];
            String[] newThreadsNames = new String[newThreads.size()];
            String[] newThreadsClasses = new String[newThreads.size()];
            int ntc = 0;
            for (JFRThread t : newThreads) {
                if (threadIdSet.add(t.threadId)) {
                    newThreadsId[ntc] = (int) t.threadId;
                    newThreadsNames[ntc] = t.name;
                    newThreadsClasses[ntc] = "";
                    ntc++;
                }
            }
            if (ntc > 0) {
                rp.setDataOnNewThreads(ntc, newThreadsId, newThreadsNames, newThreadsClasses);
            }
            newThreads.clear();
        }
        synchronized (events) {
            byte[] explicitStates = new byte[events.size()];
            int[] explicitThreads = new int[events.size()];
            long[] explicitTimeStamps = new long[events.size()];
            for (int i = 0; i < events.size(); i++) {
                JFREvent te = events.get(i);
                explicitStates[i] = te.status;
                explicitThreads[i] = (int) te.threadId;
                explicitTimeStamps[i] = te.timeStamp;
            }
            events.clear();
            rp.setExplicitDataOnThreads(explicitThreads, explicitStates, explicitTimeStamps);
        }
        return rp;
    }

    Consumer<RecordedEvent> threadStart() {
        return (RecordedEvent e) -> {
            RecordedThread t = e.getThread();
            addThreadStart(e.getStartTime(), t.getJavaThreadId(), t.getJavaName());
        };
    }

    Consumer<RecordedEvent> threadEnd() {
        return (RecordedEvent e) -> {
            addThreadEnd(e.getThread().getJavaThreadId(), e.getStartTime());
        };
    }

    Consumer<RecordedEvent> javaMonitorWait() {
        return (RecordedEvent e) -> {
            addWaitEvent(CommonConstants.THREAD_STATUS_WAIT, e);
        };
    }

    Consumer<RecordedEvent> javaMonitorEnter() {
        return (RecordedEvent e) -> {
            addWaitEvent(CommonConstants.THREAD_STATUS_MONITOR, e);
        };
    }

    Consumer<RecordedEvent> threadPark() {
        return (RecordedEvent e) -> {
            addWaitEvent(CommonConstants.THREAD_STATUS_PARK, e);
        };
    }

    Consumer<RecordedEvent> threadSleep() {
        return (RecordedEvent e) -> {
            addWaitEvent(CommonConstants.THREAD_STATUS_SLEEPING, e);
        };
    }

    public void cleanup() {
        recordingStream.close();
    }

    private void addThreadEnd(long id, Instant startTime) {
        addEvent(id, CommonConstants.THREAD_STATUS_ZOMBIE, startTime.toEpochMilli());
        synchronized (newThreads) {
            threadIdSet.remove(id);
        }
    }

    private void addThreadStart(Instant startTime, long javaThreadId, String javaName) {
        synchronized (newThreads) {
            if (threadIdSet.add(javaThreadId)) {
                newThreads.add(new JFRThread(javaThreadId, javaName));
            }
        }
    }

    private void addWaitEvent(byte status, RecordedEvent e) {
        addWaitEvent(status, e.getThread().getJavaThreadId(), e.getStartTime(), e.getEndTime());
    }

    private void addWaitEvent(byte status, long threadId, Instant startTime, Instant endTime) {
        addEvent(threadId, status, startTime.toEpochMilli());
        addEvent(threadId, CommonConstants.THREAD_STATUS_RUNNING, endTime.toEpochMilli());
    }

    private void addEvent(long threadId, byte status, long toEpochMilli) {
        synchronized (events) {
            events.add(new JFREvent(threadId, status, toEpochMilli));
        }
    }

    private Set<Long> fillInThreadData(JFRNumbersResponse rp) {
        long[] currentThreadIds = threadMXBean.getAllThreadIds();
        ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(currentThreadIds, 1);
        Set<Long> currentIdSet = new HashSet(currentThreadIds.length * 4 / 3);
        long timeStamps[] = {System.currentTimeMillis()};
        int maxThreads = currentThreadIds.length;
        int tids[] = new int[maxThreads];
        byte states[] = new byte[maxThreads];

        int nNewThreads = 0;
        int newThreadsId[] = new int[currentThreadIds.length];
        String[] newThreadsNames = new String[currentThreadIds.length];
        String[] newThreadsClasses = new String[currentThreadIds.length];

        for (int i = 0; i < currentThreadIds.length; i++) {
            ThreadInfo tinfo = threadInfos[i];
            long threadId = currentThreadIds[i];
            Long threadIdLong;

            if (tinfo == null) {
                continue;
            }
            threadIdLong = Long.valueOf(threadId);
            currentIdSet.add(threadIdLong);

            newThreadsId[nNewThreads] = (int) threadId;
            newThreadsNames[nNewThreads] = tinfo.getThreadName();
            newThreadsClasses[nNewThreads] = "";

            tids[nNewThreads] = (int) threadId;
            states[nNewThreads] = getState(tinfo);
            nNewThreads++;
        }
        rp.setDataOnNewThreads(nNewThreads, newThreadsId, newThreadsNames, newThreadsClasses);
        rp.setDataOnThreads(nNewThreads, timeStamps.length, tids, timeStamps, states);
        return currentIdSet;
    }

    byte getState(ThreadInfo threadInfo) {
        Thread.State state = threadInfo.getThreadState();
        switch (state) {
            case BLOCKED:
                return CommonConstants.THREAD_STATUS_MONITOR;
            case RUNNABLE:
                return CommonConstants.THREAD_STATUS_RUNNING;
            case TIMED_WAITING:
            case WAITING:
                StackTraceElement[] stack = threadInfo.getStackTrace();
                if (stack.length > 0) {
                    StackTraceElement el = stack[0];
                    if (isSleeping(el)) {
                        return CommonConstants.THREAD_STATUS_SLEEPING;
                    }
                    if (isParked(el)) {
                        return CommonConstants.THREAD_STATUS_PARK;
                    }
                }
                return CommonConstants.THREAD_STATUS_WAIT;
            case TERMINATED:
            case NEW:
                return CommonConstants.THREAD_STATUS_ZOMBIE;
        }
        return CommonConstants.THREAD_STATUS_UNKNOWN;
    }

    boolean isSleeping(StackTraceElement element) {
        return Thread.class.getName().equals(element.getClassName())
                && "sleep".equals(element.getMethodName());    // NOI18N
    }

    boolean isParked(StackTraceElement element) {
        String className = element.getClassName();

        if ("jdk.internal.misc.Unsafe".equals(className) || "sun.misc.Unsafe".equals(className)) {
            return "park".equals(element.getMethodName());
        }
        return false;
    }

    private class JFREvent {

        private long threadId;
        private byte status;
        private long timeStamp;

        private JFREvent(long id, byte st, long time) {
            threadId = id;
            status = st;
            timeStamp = time;
        }
    }

    private class JFRThread {

        private long threadId;
        private String name;

        private JFRThread(long id, String n) {
            threadId = id;
            name = n;
        }
    }

    class JFRNumbersResponse extends MonitoredNumbersResponse {

        private static final long[] dummyLong = new long[0];

        JFRNumbersResponse() {
            super(dummyLong, CommonConstants.SERVER_RUNNING, CommonConstants.SERVER_PROGRESS_INDETERMINATE);
            setGCstartFinishData(dummyLong, dummyLong);
        }
    }
}
