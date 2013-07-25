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

package org.netbeans.lib.profiler.server;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.global.ProfilingSessionStatus;
import org.netbeans.lib.profiler.server.system.Histogram;
import org.netbeans.lib.profiler.server.system.Timers;

/**
 * This is a base class, containing common functionality for classes that contain instrumentation methods.
 *
 * @author Tomas Hurka
 * @author Misha Dmitriev
 */
public class ProfilerRuntime implements CommonConstants {
    //~ Inner Interfaces ---------------------------------------------------------------------------------------------------------

    // ------------- Handling operations that should be performed outside ProfilerRuntime --------------
    public static interface ExternalActionsHandler {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void handleEventBufferDump(byte[] eventBuffer, int startPos, int curPtrPos);

        public void handleFirstTimeMethodInvoke(char methodId);

        public int handleFirstTimeVMObjectAlloc(String className, int classLoaderId);

        public void handleReflectiveInvoke(Method method);
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final boolean DEBUG = false;
    protected static ExternalActionsHandler externalActionsHandler;
    protected static boolean lockContentionMonitoringEnabled;

    // ---------------------------------- Writing profiler events --------------------------------------
    protected static byte[] eventBuffer;
    protected static int globalEvBufPos;
    protected static int globalEvBufPosThreshold;
    protected static volatile boolean sendingBuffer;
    private static boolean printEvents; // For debugging
    private volatile static Set knownMonitors;

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static void createEventBuffer(int bufSize) {
        eventBuffer = new byte[bufSize];
        globalEvBufPosThreshold = bufSize - (3 * ThreadInfo.MAX_EVENT_SIZE) - 1;
        globalEvBufPos = 0;
    }

    // Asynchronous event buffer dump
    public static void dumpEventBuffer() {
        if (eventBuffer == null) {
            return; // Just in case somebody calls it with null eventBuffer
        }

        if (sendingBuffer) {
            return; // No need (and may cause a deadlock in tool) if forced dump is requested while
                    // a routine dump is already in progress
        }

        synchronized (eventBuffer) {
            sendingBuffer = true;

            // Dump the info from all thread-local buffers (if they are not null) into the global buffer
            ThreadInfo[] tis = ThreadInfo.getThreadInfos();

            for (int i = 0; i < tis.length; i++) {
                ThreadInfo ti = tis[i];

                if ((ti == null) || (ti.evBuf == null) || !ti.isInitialized()) {
                    continue;
                }

                int curPos = ti.evBufPos; // Guaranteed to be at event boundary

                if (((globalEvBufPos + curPos) - ti.evBufDumpLastPos) > globalEvBufPosThreshold) {
                    break; // We don't try to perform more than one global buffer dumps yet
                }

                int evBufSize = curPos - ti.evBufDumpLastPos;

                if (evBufSize > 0) {
                    eventBuffer[globalEvBufPos++] = SET_FOLLOWING_EVENTS_THREAD;
                    eventBuffer[globalEvBufPos++] = (byte) ((ti.threadId >> 8) & 0xFF);
                    eventBuffer[globalEvBufPos++] = (byte) (ti.threadId & 0xFF);
                    System.arraycopy(ti.evBuf, ti.evBufDumpLastPos, eventBuffer, globalEvBufPos, evBufSize);
                    globalEvBufPos += evBufSize;
                    ti.evBufDumpLastPos = curPos;
                }
            }

            externalActionsHandler.handleEventBufferDump(eventBuffer, 0, globalEvBufPos);
            globalEvBufPos = 0;
            sendingBuffer = false;
        }
    }

    protected static void copyLocalBuffer(ThreadInfo ti) {
        long absTimeStamp = 0;
        long threadTimeStamp = 0;

        // Copy the local buffer into the main buffer - however avoid doing that if we have already reset profiler collectors
        if (eventBuffer == null) {
            return;
        }

        boolean needToAdjustTime = false;

        if (sendingBuffer) { // Some other thread is already sending the buffer contents
            absTimeStamp = Timers.getCurrentTimeInCounts();
            if (ProfilerRuntimeCPU.threadCPUTimerOn) threadTimeStamp = Timers.getThreadCPUTimeInNanos();

            synchronized (eventBuffer) { // Wait on the lock. When it's free, buffer has been sent and reset

                if (sendingBuffer) {
                    System.err.println("*** Sanity check failed - sendingBuffer where should have been already sent"); // NOI18N
                }

                needToAdjustTime = true;
            }
        }

        synchronized (eventBuffer) {
            if (!ti.isInitialized()) {
                return; // Reset collectors performed when we were already executing instrumentation code
            }

            int curPos = ti.evBufPos;

            // First check if the global buffer itself needs to be dumped
            int evBufDumpLastPos = ti.evBufDumpLastPos;

            if (((globalEvBufPos + curPos) - evBufDumpLastPos) > globalEvBufPosThreshold) {
                sendingBuffer = true;

                if (!needToAdjustTime) {
                    absTimeStamp = Timers.getCurrentTimeInCounts();
                    if (ProfilerRuntimeCPU.threadCPUTimerOn) threadTimeStamp = Timers.getThreadCPUTimeInNanos();
                    needToAdjustTime = true;
                }

                externalActionsHandler.handleEventBufferDump(eventBuffer, 0, globalEvBufPos);
                globalEvBufPos = 0;
                sendingBuffer = false;
            }
            
            // check that we still have valid eventBuffer
            if (eventBuffer != null) {
                // Finally copy the local buffer into the global one
                eventBuffer[globalEvBufPos++] = SET_FOLLOWING_EVENTS_THREAD;
                eventBuffer[globalEvBufPos++] = (byte) ((ti.threadId >> 8) & 0xFF);
                eventBuffer[globalEvBufPos++] = (byte) ((ti.threadId) & 0xFF);
                System.arraycopy(ti.evBuf, evBufDumpLastPos, eventBuffer, globalEvBufPos, curPos - evBufDumpLastPos);
                globalEvBufPos += (curPos - evBufDumpLastPos);
                ti.evBufPos = 0;
                ti.evBufDumpLastPos = 0;

                // Now, if we previously spent time waiting for another thread to dump the global buffer, or doing that
                // ourselves, write the ADJUST_TIME event into the local buffer
                if (needToAdjustTime) {
                    writeAdjustTimeEvent(ti, absTimeStamp, threadTimeStamp);
                }
            }
        }
    }

    public static void init(ExternalActionsHandler h) {
        externalActionsHandler = h;
    }

    public static void setLockContentionMonitoringEnabled(boolean b) {
        lockContentionMonitoringEnabled = b;
        if (DEBUG) {
            System.out.println("ProfilerRuntime.DEBUG: setLockContentionMonitoringEnabled "+b);
        }
    }

    // ------------- Handling wait/sleep/monitors entry/exit -------------------------------------------
    public static void monitorEntry(Thread t, Object monitor) {
        if (ThreadInfo.profilingSuspended() || ThreadInfo.isProfilerServerThread(t)
            || ThreadInfo.isProfilerServerMonitor(monitor)) {
            // nothing done for profiler own threads or if in instrumentation
            return;
        }
        long timeStamp = -1;
        ThreadInfo ti = ThreadInfo.getThreadInfo(t);
        if (ti.inProfilingRuntimeMethod > 0) {
            return;
        }
        ti.inProfilingRuntimeMethod++;

        ProfilingSessionStatus status = ProfilerServer.getProfilingSessionStatus();

        if (status != null) {
            switch (status.currentInstrType) {
                case INSTR_RECURSIVE_FULL:
                case INSTR_RECURSIVE_SAMPLED:
                    timeStamp = ProfilerRuntimeCPU.monitorEntryCPU(ti, monitor);

                    break;
                case INSTR_CODE_REGION:
                    timeStamp = ProfilerRuntimeCPUCodeRegion.monitorEntryRegion(t, monitor);

                    break;
            }
        }
        if (lockContentionMonitoringEnabled && timeStamp == -1) {
            writeWaitTimeEvent(METHOD_ENTRY_MONITOR, ti, monitor);
        }

        Monitors.recordThreadStateChange(ti.thread, THREAD_STATUS_MONITOR, timeStamp, monitor);
        ti.inProfilingRuntimeMethod--;
    }

    public static void monitorExit(Thread t, Object monitor) {
        if (ThreadInfo.profilingSuspended() || ThreadInfo.isProfilerServerThread(t)
            || ThreadInfo.isProfilerServerMonitor(monitor)) {
            // nothing done for profiler own threads or if in instrumentation
            return;
        }
        long timeStamp = -1;
        ThreadInfo ti = ThreadInfo.getThreadInfo(t);
        if (ti.inProfilingRuntimeMethod > 0) {
            return;
        }
        ti.inProfilingRuntimeMethod++;

        ProfilingSessionStatus status = ProfilerServer.getProfilingSessionStatus();

        if (status != null) {
            switch (status.currentInstrType) {
                case INSTR_RECURSIVE_FULL:
                case INSTR_RECURSIVE_SAMPLED:
                    timeStamp = ProfilerRuntimeCPU.monitorExitCPU(ti, monitor);

                    break;
                case INSTR_CODE_REGION:
                    timeStamp = ProfilerRuntimeCPUCodeRegion.monitorExitRegion(t, monitor);

                    break;
            }
        }
        if (lockContentionMonitoringEnabled && timeStamp == -1) {
            writeWaitTimeEvent(METHOD_EXIT_MONITOR, ti, monitor);
        }

        Monitors.recordThreadStateChange(ti.thread, THREAD_STATUS_RUNNING, timeStamp, null);
        ti.inProfilingRuntimeMethod--;
    }

    public static void sleepEntry() {
        if (ThreadInfo.profilingSuspended() || ThreadInfo.isCurrentThreadProfilerServerThread()) {
            // nothing done for profiler own threads or if in instrumentation
            return;
        }
        long timeStamp = -1;
        ThreadInfo ti = ThreadInfo.getThreadInfo();
        if (ti.inProfilingRuntimeMethod > 0) {
            return;
        }
        ti.inProfilingRuntimeMethod++;

        ProfilingSessionStatus status = ProfilerServer.getProfilingSessionStatus();

        if (status != null) {
            switch (status.currentInstrType) {
                case INSTR_RECURSIVE_FULL:
                case INSTR_RECURSIVE_SAMPLED:
                    timeStamp = ProfilerRuntimeCPU.sleepEntryCPU(ti);

                    break;
                case INSTR_CODE_REGION:
                    ProfilerRuntimeCPUCodeRegion.sleepEntryRegion();

                    break;
            }
        }

        Monitors.recordThreadStateChange(ti.thread, THREAD_STATUS_SLEEPING, timeStamp, null);
        ti.inProfilingRuntimeMethod--;
    }

    public static void sleepExit() {
        if (ThreadInfo.profilingSuspended() || ThreadInfo.isCurrentThreadProfilerServerThread()) {
            // nothing done for profiler own threads or if in instrumentation
            return;
        }
        long timeStamp = -1;
        ThreadInfo ti = ThreadInfo.getThreadInfo();
        if (ti.inProfilingRuntimeMethod > 0) {
            return;
        }
        ti.inProfilingRuntimeMethod++;

        ProfilingSessionStatus status = ProfilerServer.getProfilingSessionStatus();

        if (status != null) {
            switch (status.currentInstrType) {
                case INSTR_RECURSIVE_FULL:
                case INSTR_RECURSIVE_SAMPLED:
                    timeStamp = ProfilerRuntimeCPU.sleepExitCPU(ti);

                    break;
                case INSTR_CODE_REGION:
                    ProfilerRuntimeCPUCodeRegion.sleepExitRegion();

                    break;
            }
        }

        Monitors.recordThreadStateChange(ti.thread, THREAD_STATUS_RUNNING, timeStamp, null);
        ti.inProfilingRuntimeMethod--;
    }

    public static void waitEntry() {
        if (ThreadInfo.profilingSuspended() || ThreadInfo.isCurrentThreadProfilerServerThread()) {
            // nothing done for profiler own threads or if in instrumentation
            return;
        }
        long timeStamp = -1;
        ThreadInfo ti = ThreadInfo.getThreadInfo();
        if (ti.inProfilingRuntimeMethod > 0) {
            return;
        }
        ti.inProfilingRuntimeMethod++;

        ProfilingSessionStatus status = ProfilerServer.getProfilingSessionStatus();

        if (status != null) {
            switch (status.currentInstrType) {
                case INSTR_RECURSIVE_FULL:
                case INSTR_RECURSIVE_SAMPLED:
                    timeStamp = ProfilerRuntimeCPU.waitEntryCPU(ti);

                    break;
                case INSTR_CODE_REGION:
                    ProfilerRuntimeCPUCodeRegion.waitEntryRegion();

                    break;
            }
        }

        Monitors.recordThreadStateChange(ti.thread, THREAD_STATUS_WAIT, timeStamp, null);
        ti.inProfilingRuntimeMethod--;
    }

    public static void waitExit() {
        if (ThreadInfo.profilingSuspended() || ThreadInfo.isCurrentThreadProfilerServerThread()) {
            // nothing done for profiler own threads or if in instrumentation
            return;
        }
        long timeStamp = -1;
        ThreadInfo ti = ThreadInfo.getThreadInfo();
        if (ti.inProfilingRuntimeMethod > 0) {
            return;
        }
        ti.inProfilingRuntimeMethod++;

        ProfilingSessionStatus status = ProfilerServer.getProfilingSessionStatus();

        if (status != null) {
            switch (status.currentInstrType) {
                case INSTR_RECURSIVE_FULL:
                case INSTR_RECURSIVE_SAMPLED:
                    timeStamp = ProfilerRuntimeCPU.waitExitCPU(ti);

                    break;
                case INSTR_CODE_REGION:
                    ProfilerRuntimeCPUCodeRegion.waitExitRegion();

                    break;
            }
        }

        Monitors.recordThreadStateChange(ti.thread, THREAD_STATUS_RUNNING, timeStamp, null);
        ti.inProfilingRuntimeMethod--;
    }
    
    public static void parkEntry() {
        if (ThreadInfo.profilingSuspended() || ThreadInfo.isCurrentThreadProfilerServerThread()) {
            // nothing done for profiler own threads or if in instrumentation
            return;
        }
        long timeStamp = -1;
        ThreadInfo ti = ThreadInfo.getThreadInfo();
        if (ti.inProfilingRuntimeMethod > 0) {
            return;
        }
        ti.inProfilingRuntimeMethod++;

        ProfilingSessionStatus status = ProfilerServer.getProfilingSessionStatus();

        if (status != null) {
            switch (status.currentInstrType) {
                case INSTR_RECURSIVE_FULL:
                case INSTR_RECURSIVE_SAMPLED:
                    timeStamp = ProfilerRuntimeCPU.parkEntryCPU(ti);

                    break;
                case INSTR_CODE_REGION:
                    ProfilerRuntimeCPUCodeRegion.parkEntryRegion();

                    break;
            }
        }

        Monitors.recordThreadStateChange(ti.thread, THREAD_STATUS_PARK, timeStamp, null);
        ti.inProfilingRuntimeMethod--;
    }

    public static void parkExit() {
        if (ThreadInfo.profilingSuspended() || ThreadInfo.isCurrentThreadProfilerServerThread()) {
            // nothing done for profiler own threads or if in instrumentation
            return;
        }
        long timeStamp = -1;
        ThreadInfo ti = ThreadInfo.getThreadInfo();
        if (ti.inProfilingRuntimeMethod > 0) {
            return;
        }
        ti.inProfilingRuntimeMethod++;

        ProfilingSessionStatus status = ProfilerServer.getProfilingSessionStatus();

        if (status != null) {
            switch (status.currentInstrType) {
                case INSTR_RECURSIVE_FULL:
                case INSTR_RECURSIVE_SAMPLED:
                    timeStamp = ProfilerRuntimeCPU.parkExitCPU(ti);

                    break;
                case INSTR_CODE_REGION:
                    ProfilerRuntimeCPUCodeRegion.parkExitRegion();

                    break;
            }
        }

        Monitors.recordThreadStateChange(ti.thread, THREAD_STATUS_RUNNING, timeStamp, null);
        ti.inProfilingRuntimeMethod--;
    }
    
    // ------------------------------ Common setup functionality ---------------------------------------
    public static void resetProfilerCollectors(int instrType) {
        if ((instrType != INSTR_CODE_REGION) && (eventBuffer != null)) {
            synchronized (eventBuffer) {
                doResetProfilerCollectors(instrType);
            }
        } else {
            doResetProfilerCollectors(instrType);
        }
    }

    public static void writeProfilingPointHitEvent(int id, long absTimeStamp) {
        ThreadInfo ti = ThreadInfo.getThreadInfo();
        int tid = ti.threadId;

        if (ti.evBuf == null || !ti.isInitialized()) { // memory profiling or ThreadInfo is not initialized -> use global event buffer

            synchronized (eventBuffer) {
                int curPos = globalEvBufPos;

                if (curPos > globalEvBufPosThreshold) { // Dump the buffer
                    dumpEventBuffer();
                    curPos = 0;
                }

                curPos = writePPointHitToBuffer(eventBuffer, absTimeStamp, curPos, id, tid);
                globalEvBufPos = curPos;
            }
        } else { // CPU profiling write to thread event buffer

            int curPos = ti.evBufPos; // It's important to use a local copy for evBufPos, so that evBufPos is at event boundary at any moment

            if (curPos > ThreadInfo.evBufPosThreshold) {
                ProfilerRuntimeCPU.copyLocalBuffer(ti); // ugly I know :-( 
                curPos = ti.evBufPos;
            }

            byte[] evBuf = ti.evBuf;
            ti.evBufPos = writePPointHitToBuffer(evBuf, absTimeStamp, curPos, id, tid);
        }
    }

    public static void profilePointHit(char id) {
        if (ThreadInfo.profilingSuspended() || ThreadInfo.isCurrentThreadProfilerServerThread()) {
            return;
        }

        if (eventBuffer == null) {
            return; // Instrumentation removal happened when we were in instrumentation
        }

        ThreadInfo ti = ThreadInfo.getThreadInfo();

        if (ti.inProfilingRuntimeMethod > 0) {
            return;
        }

        ti.inProfilingRuntimeMethod++;

        ProfilingPointServerHandler method = ProfilingPointServerHandler.getHandler(id);
        if (method != null) {
            try {
                method.profilingPointHit(id);
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }

        ti.inProfilingRuntimeMethod--;
    }

    static void writeThreadCreationEvent(ThreadInfo ti) {
        writeThreadCreationEvent(ti.thread, ti.getThreadId());
    }

    protected static void writeThreadCreationEvent(Thread thread, int threadId) {
        String threadName;
        String threadClassName = thread.getClass().getName();
        int fullInfoLen;
        
        try {
            threadName = thread.getName();
        } catch (NullPointerException e) {
            threadName = "*Unknown thread ("+threadId+")*";  // NOI18N
        }
        fullInfoLen = ((threadName.length() + threadClassName.length()) * 2) + 7;
        synchronized (eventBuffer) {
            if ((globalEvBufPos + fullInfoLen) > globalEvBufPosThreshold) {
                sendingBuffer = true;
                externalActionsHandler.handleEventBufferDump(eventBuffer, 0, globalEvBufPos);
                globalEvBufPos = 0;
                sendingBuffer = false;
            }

            eventBuffer[globalEvBufPos++] = NEW_THREAD;

            eventBuffer[globalEvBufPos++] = (byte) ((threadId >> 8) & 0xFF);
            eventBuffer[globalEvBufPos++] = (byte) ((threadId) & 0xFF);

            byte[] name = threadName.getBytes();
            int len = name.length;
            eventBuffer[globalEvBufPos++] = (byte) ((len >> 8) & 0xFF);
            eventBuffer[globalEvBufPos++] = (byte) ((len) & 0xFF);
            System.arraycopy(name, 0, eventBuffer, globalEvBufPos, len);
            globalEvBufPos += len;
            name = threadClassName.getBytes();
            len = name.length;
            eventBuffer[globalEvBufPos++] = (byte) ((len >> 8) & 0xFF);
            eventBuffer[globalEvBufPos++] = (byte) ((len) & 0xFF);
            System.arraycopy(name, 0, eventBuffer, globalEvBufPos, len);
            globalEvBufPos += len;
        }
    }
    
    static void writeAdjustTimeEvent(ThreadInfo ti, long absTimeStamp, long threadTimeStamp) {
        //if (printEvents) System.out.println("*** Writing ADJUST_TIME event, metodId = " + (int)methodId + ", ts = " + timeStamp);
        byte[] evBuf = ti.evBuf;
        int curPos = ti.evBufPos; // It's important to use a local copy for evBufPos, so that evBufPos is at event boundary at any moment

        // Check if the local buffer is about to overflow. We initially didn't have this code here, assuming that writeAdjustTimeEvent()
        // cannot be called more than 1-2 times in a row. However, later we recognized that actually a large number of this calls can be
        // made sequentially by classLoadHook() if many classes are loaded in a row. So we need to perform all checks for overflow;
        // however we take some advantage of the fact that we don't need to take intermediate time stamps etc.
        if (curPos > ThreadInfo.evBufPosThreshold) {
            // Copy the local buffer into the main buffer - however avoid doing that if we have already reset profiler collectors
            if (eventBuffer == null) {
                return;
            }

            synchronized (eventBuffer) {
                curPos = ti.evBufPos;

                boolean globalBufNeedsDump = false;

                // First check if the global buffer itself needs to be dumped
                int evBufDumpLastPos = ti.evBufDumpLastPos;

                if (((globalEvBufPos + curPos) - evBufDumpLastPos) > globalEvBufPosThreshold) {
                    globalBufNeedsDump = true;
                    sendingBuffer = true;
                    externalActionsHandler.handleEventBufferDump(eventBuffer, 0, globalEvBufPos);
                    globalEvBufPos = 0;
                    sendingBuffer = false;
                }

                // Finally copy the local buffer into the global one
                eventBuffer[globalEvBufPos++] = SET_FOLLOWING_EVENTS_THREAD;
                eventBuffer[globalEvBufPos++] = (byte) ((ti.threadId >> 8) & 0xFF);
                eventBuffer[globalEvBufPos++] = (byte) ((ti.threadId) & 0xFF);
                System.arraycopy(evBuf, evBufDumpLastPos, eventBuffer, globalEvBufPos, curPos - evBufDumpLastPos);
                globalEvBufPos += (curPos - evBufDumpLastPos);
                ti.evBufPos = 0;
                ti.evBufDumpLastPos = 0;
            }
        }

        curPos = ti.evBufPos;
        evBuf[curPos++] = ADJUST_TIME;

        long absInterval = Timers.getCurrentTimeInCounts() - absTimeStamp;
        evBuf[curPos++] = (byte) ((absInterval >> 48) & 0xFF);
        evBuf[curPos++] = (byte) ((absInterval >> 40) & 0xFF);
        evBuf[curPos++] = (byte) ((absInterval >> 32) & 0xFF);
        evBuf[curPos++] = (byte) ((absInterval >> 24) & 0xFF);
        evBuf[curPos++] = (byte) ((absInterval >> 16) & 0xFF);
        evBuf[curPos++] = (byte) ((absInterval >> 8) & 0xFF);
        evBuf[curPos++] = (byte) ((absInterval) & 0xFF);

        if (ProfilerRuntimeCPU.threadCPUTimerOn) {
            long threadInterval = Timers.getThreadCPUTimeInNanos() - threadTimeStamp;
            evBuf[curPos++] = (byte) ((threadInterval >> 48) & 0xFF);
            evBuf[curPos++] = (byte) ((threadInterval >> 40) & 0xFF);
            evBuf[curPos++] = (byte) ((threadInterval >> 32) & 0xFF);
            evBuf[curPos++] = (byte) ((threadInterval >> 24) & 0xFF);
            evBuf[curPos++] = (byte) ((threadInterval >> 16) & 0xFF);
            evBuf[curPos++] = (byte) ((threadInterval >> 8) & 0xFF);
            evBuf[curPos++] = (byte) ((threadInterval) & 0xFF);
        }
        ti.evBufPos = curPos;
    }

    static long writeWaitTimeEvent(byte eventType, ThreadInfo ti, Object id) {
        int hash = writeNewMonitorEvent(ti,id);
        
        // if (printEvents) System.out.println("*** Writing event " + eventType + ", metodId = " + (int)methodId);
        int curPos = ti.evBufPos; // It's important to use a local copy for evBufPos, so that evBufPos is at event boundary at any moment

        if (curPos > ThreadInfo.evBufPosThreshold) {
            copyLocalBuffer(ti);
            curPos = ti.evBufPos;
        }

        byte[] evBuf = ti.evBuf;

        evBuf[curPos++] = eventType;

        // Note that in the code below, we write only the 7 low bytes of the 64-bit value. The justification is that this saves
        // us some performance and memory, and 2^55 == 36028797018963968 ns == 36028797 sec == 10008 hr == 416 days is a sufficent
        // representation range for the foreseeable usages of our tool. (***)
        long absTimeStamp = Timers.getCurrentTimeInCounts();

        if (DEBUG) {
            System.out.println("ProfilerRuntime.DEBUG: Writing waitTime event type = " + eventType + // NOI18N
                    ", timestamp: " + absTimeStamp + // NOI18N
                    (id==null ? "" : ", id: "+Integer.toHexString(System.identityHashCode(id)))); // NOI18N
        }

        evBuf[curPos++] = (byte) ((absTimeStamp >> 48) & 0xFF);
        evBuf[curPos++] = (byte) ((absTimeStamp >> 40) & 0xFF);
        evBuf[curPos++] = (byte) ((absTimeStamp >> 32) & 0xFF);
        evBuf[curPos++] = (byte) ((absTimeStamp >> 24) & 0xFF);
        evBuf[curPos++] = (byte) ((absTimeStamp >> 16) & 0xFF);
        evBuf[curPos++] = (byte) ((absTimeStamp >> 8) & 0xFF);
        evBuf[curPos++] = (byte) ((absTimeStamp) & 0xFF);
        if (id != null) {
            evBuf[curPos++] = (byte) ((hash >> 24) & 0xFF);
            evBuf[curPos++] = (byte) ((hash >> 16) & 0xFF);
            evBuf[curPos++] = (byte) ((hash >> 8) & 0xFF);
            evBuf[curPos++] = (byte) ((hash) & 0xFF);            
        }

        ti.evBufPos = curPos;
        return absTimeStamp;
    }

    private static int writeNewMonitorEvent(ThreadInfo ti, Object id) {
        if (id == null || !lockContentionMonitoringEnabled) return -1;
        if (ti.evBuf == null) {
            if (!ti.isInitialized()) ti.initialize();
            ti.useEventBuffer();
            writeThreadCreationEvent(ti);
        }
        int hash = System.identityHashCode(id);
        Integer hashInt = new Integer(hash);
        if (knownMonitors == null) {
            knownMonitors = new HashSet();
        }
        if (!knownMonitors.contains(hashInt)) {
            knownMonitors.add(hashInt);
            int curPos = ti.evBufPos; // It's important to use a local copy for evBufPos, so that evBufPos is at event boundary at any moment

            if (curPos > ThreadInfo.evBufPosThreshold) {
                copyLocalBuffer(ti);
                curPos = ti.evBufPos;
            }

            byte[] evBuf = ti.evBuf;
            evBuf[curPos++] = NEW_MONITOR;
            evBuf[curPos++] = (byte) ((hash >> 24) & 0xFF);
            evBuf[curPos++] = (byte) ((hash >> 16) & 0xFF);
            evBuf[curPos++] = (byte) ((hash >> 8) & 0xFF);
            evBuf[curPos++] = (byte) ((hash) & 0xFF);            

            byte[] name = id.getClass().getName().getBytes();
            int len = name.length;
            evBuf[curPos++] = (byte) ((len >> 8) & 0xFF);
            evBuf[curPos++] = (byte) ((len) & 0xFF);
            System.arraycopy(name, 0, evBuf, curPos, len);
            curPos += len;
            ti.evBufPos = curPos;
        }
        return hash;
    }

    // -------------------------------- Thread-related stuff ------------------------------------------
    public static boolean profiledTargetAppThreadsExist() {
        return (ThreadInfo.getNProfiledAppThreads() > 0);
    }

    protected static void changeAllThreadsInProfRuntimeMethodStatus(int val) {
        ThreadInfo.changeAllThreadsInProfRuntimeMethodStatus(val);
    }

    protected static void clearDataStructures() {
        eventBuffer = null;
        globalEvBufPos = 0;
        ThreadInfo.resetThreadInfoTable();
        knownMonitors = new HashSet();
    }

    protected static void createNewDataStructures() {
        ThreadInfo.resetThreadInfoTable(); // Despite the name, it effectively creates some data
    }

    private static void doResetProfilerCollectors(int instrType) {
        ThreadInfo.resetThreadInfoTable();
        globalEvBufPos = 0;

        if (eventBuffer != null) {
            eventBuffer[globalEvBufPos++] = RESET_COLLECTORS;
        }

        switch (instrType) {
            case INSTR_RECURSIVE_FULL:
            case INSTR_RECURSIVE_SAMPLED:
                ProfilerRuntimeCPU.resetProfilerCollectors();

                break;
            case INSTR_NONE_SAMPLING:
                ProfilerRuntimeSampler.resetProfilerCollectors();
                
                break;
            case INSTR_CODE_REGION:
                ProfilerRuntimeCPUCodeRegion.resetProfilerCollectors();

                break;
            case INSTR_OBJECT_ALLOCATIONS:
            case INSTR_OBJECT_LIVENESS:
                ProfilerRuntimeMemory.resetProfilerCollectors(instrType);

                break;
            case INSTR_NONE_MEMORY_SAMPLING:
                if (Histogram.isAvailable()) {
                    ProfilerServer.notifyClientOnResultsAvailability();
                }
                break;
        }
    }

    private static int writePPointHitToBuffer(byte[] buf, final long absTimeStamp, int curPos, final int id, final int tid) {
        buf[curPos++] = BUFFEREVENT_PROFILEPOINT_HIT;
        buf[curPos++] = (byte) ((id >> 8) & 0xFF);
        buf[curPos++] = (byte) (id & 0xFF);
        buf[curPos++] = (byte) ((absTimeStamp >> 48) & 0xFF);
        buf[curPos++] = (byte) ((absTimeStamp >> 40) & 0xFF);
        buf[curPos++] = (byte) ((absTimeStamp >> 32) & 0xFF);
        buf[curPos++] = (byte) ((absTimeStamp >> 24) & 0xFF);
        buf[curPos++] = (byte) ((absTimeStamp >> 16) & 0xFF);
        buf[curPos++] = (byte) ((absTimeStamp >> 8) & 0xFF);
        buf[curPos++] = (byte) ((absTimeStamp) & 0xFF);
        buf[curPos++] = (byte) ((tid >> 8) & 0xFF);
        buf[curPos++] = (byte) ((tid) & 0xFF);

        return curPos;
    }
}
