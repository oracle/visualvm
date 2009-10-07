/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
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

import org.netbeans.lib.profiler.global.Platform;
import org.netbeans.lib.profiler.server.system.Timers;
import java.lang.reflect.Method;


/**
 * This class contains the functionality that is common for all CPU profiling methods available in JFluid.
 *
 * @author Tomas Hurka
 * @author Misha Dmitriev
 */
public class ProfilerRuntimeCPU extends ProfilerRuntime {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final boolean DEBUG = false;
    private static int nProfiledThreadsLimit;
    protected static int nProfiledThreadsAllowed;

    // The following flag is used to prevent deadlock inside getThreadInfo() by forcing immediate return from methodEntry() etc.
    // in case methodEntry() (typically when it's injected in some core class method) is executed on behalf of some profiler server thread,
    // when all target app threads are suspended. In that case, some thread may be suspended within getThreadInfo(), holding the lock.
    // It is also used to disable instrumentation to be on the safe side when we e.g. detach from a running multithreaded application -
    // it looks as if in this case de-instrumentation may not immediately propagate everywhere.
    protected static volatile boolean recursiveInstrumentationDisabled = false;

    // ------------------------------------------ Timers -----------------------------------------------
    protected static boolean absoluteTimerOn;

    // ------------------------------------------ Timers -----------------------------------------------
    protected static boolean threadCPUTimerOn;

    // ---------------------------------- Profile Data Acquisition --------------------------------------
    protected static boolean[] instrMethodInvoked;
    private static boolean javaLangReflectMethodInvokeInterceptEnabled = false;
    private static Method getRequestedSessionIdMethod;
    private static Method getMethodMethod;
    private static Method getServletPathMethod;

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    // See the comment in writeTimeStampedEvent() below, marked with (***)
    // On all OSes except Linux, the upper byte of value returned by Timers.getCurrentTimeInCounts() will be zero - timer
    // calculates time from program start or from machine start. But on Linux it seems to be timeofday or something. We have
    // to take measures here to return time in the same format as used for collected data.
    public static long getAbsTimeStampInCollectedFormat() {
        return Timers.getCurrentTimeInCounts() & 72057594037927935L; //0xFFFFFFFFFFFFFF, i.e. 7 bytes
    }

    public static void setInstrMethodsInvoked(boolean[] methodInvoked) {
        instrMethodInvoked = methodInvoked;
    }

    public static void setJavaLangReflectMethodInvokeInterceptEnabled(boolean v) {
        javaLangReflectMethodInvokeInterceptEnabled = v;
    }

    public static void setNProfiledThreadsLimit(int num) {
        nProfiledThreadsLimit = nProfiledThreadsAllowed = num;
    }

    public static void setTimerTypes(boolean absolute, boolean threadCPU) {
        if (threadCPU != threadCPUTimerOn && Platform.isSolaris()) {
            Timers.enableMicrostateAccounting(threadCPU);
        }

        absoluteTimerOn = absolute;
        threadCPUTimerOn = threadCPU;
    }

    // This is currently used only when calibrating the profiler, to pre-create a ThreadInfo before calling methodEntry/Exit.
    // It is done to prevent the system attempting to send a "new thread created" message to the client.
    public static void createThreadInfoForCurrentThread() {
        ThreadInfo ti = ThreadInfo.getThreadInfo();
        ti.initialize();
        ti.useEventBuffer();
        ti.inCallGraph = false; // Important: this is a correct initial value when ti is used in calibration
    }

    public static void handleJavaLangReflectMethodInvoke(Method method) {
        if (!javaLangReflectMethodInvokeInterceptEnabled) {
            return;
        }

        if (recursiveInstrumentationDisabled) {
            return;
        }

        ThreadInfo ti = ThreadInfo.getThreadInfo();

        if (!ti.isInitialized() || !ti.inCallGraph) {
            return; // ti == null may happen if instrumentation has been removed or data collectors reset
        }

        if (ti.inProfilingRuntimeMethod > 0) {
            return;
        }

        ti.inProfilingRuntimeMethod++;

        externalActionsHandler.handleReflectiveInvoke(method);

        ti.inProfilingRuntimeMethod--;
    }

    public static void handleServletDoMethod(Object request) {
        if (recursiveInstrumentationDisabled) {
            return;
        }

        ThreadInfo ti = ThreadInfo.getThreadInfo();

        if (!ti.isInitialized()) {
            System.out.println("No thread for servlet request"); // NOI18N

            return;
        }

        ti.inProfilingRuntimeMethod++;
        servletDoMethodHook(ti, request);
        ti.inProfilingRuntimeMethod--;
    }

    public static void resetProfilerCollectors() {
        nProfiledThreadsAllowed = nProfiledThreadsLimit;
    }

    public static void resumeCurrentThreadTimer() {
        ThreadInfo ti = ThreadInfo.getThreadInfo();

        if (!ti.isInitialized() || !ti.inCallGraph) {
            return;
        }

        writeAdjustTimeEvent(ti, ti.absEntryTime, ti.threadEntryTime);
        ti.inProfilingRuntimeMethod--;
    }

    // This is currently called in class load hook, to stop counting the time and emitting method entry/exit events while the
    // hook and other Java code that it may call are active.
    public static ThreadInfo suspendCurrentThreadTimer() {
        ThreadInfo ti = ThreadInfo.getThreadInfo();

        if (!ti.isInitialized() || !ti.inCallGraph) {
            return ti;
        }

        ti.inProfilingRuntimeMethod++;

        // those timestamps are taken here, as opposed to earlier in this method, because we need to make sure we do not
        // profile the Timer.get... calls, by increasing the ti.inProfilingRuntimeMethod
        // see issue 65614 for a possible impact of this
        // http://profiler.netbeans.org/issues/show_bug.cgi?id=65614
        long absTimeStamp = Timers.getCurrentTimeInCounts();
        long threadTimeStamp = Timers.getThreadCPUTimeInNanos();
        ti.absEntryTime = absTimeStamp;
        ti.threadEntryTime = threadTimeStamp;

        return ti;
    }

    protected static void clearDataStructures() {
        ProfilerRuntime.clearDataStructures();
        nProfiledThreadsAllowed = nProfiledThreadsLimit;
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
            threadTimeStamp = Timers.getThreadCPUTimeInNanos();

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
                    threadTimeStamp = Timers.getThreadCPUTimeInNanos();
                    needToAdjustTime = true;
                }

                externalActionsHandler.handleEventBufferDump(eventBuffer, 0, globalEvBufPos);
                globalEvBufPos = 0;
                sendingBuffer = false;
            }

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

    protected static long currentTimeInCounts() {
        return Timers.getCurrentTimeInCounts();
    }

    protected static void enableProfiling(boolean v) {
        recursiveInstrumentationDisabled = !v;

        // Doesn't call clearDataStructures() since this is an "abstract" class
    }

    // ---------------------------------- Handling wait/sleep/monitor times ----------------------------
    protected static void monitorEntryCPU(Thread t, Object monitor) {
        if (recursiveInstrumentationDisabled) {
            return; // See the comment at the recursiveInstrumentationDisabled variable declaration
        }

        ThreadInfo ti = ThreadInfo.getThreadInfo(t);

        if (ti.isInitialized() && ti.inCallGraph) {
            if (ti.inProfilingRuntimeMethod > 0) {
                return;
            }

            ti.inProfilingRuntimeMethod++;
            //System.out.println("++++++monitorEntry, depth = " + ti.stackDepth);
            writeWaitTimeEvent(METHOD_ENTRY_MONITOR, ti);
            ti.inProfilingRuntimeMethod--;
        }
    }

    protected static void monitorExitCPU(Thread t, Object monitor) {
        if (recursiveInstrumentationDisabled) {
            return; // See the comment at the recursiveInstrumentationDisabled variable declaration
        }

        ThreadInfo ti = ThreadInfo.getThreadInfo(t);

        if (ti.isInitialized() && ti.inCallGraph) {
            if (ti.inProfilingRuntimeMethod > 0) {
                return;
            }

            ti.inProfilingRuntimeMethod++;
            //System.out.println("++++++monitorExit, depth = " + ti.stackDepth);
            writeWaitTimeEvent(METHOD_EXIT_MONITOR, ti);
            ti.inProfilingRuntimeMethod--;
        }
    }

    protected static void sleepEntryCPU() {
        if (recursiveInstrumentationDisabled) {
            return; // See the comment at the recursiveInstrumentationDisabled variable declaration
        }

        ThreadInfo ti = ThreadInfo.getThreadInfo();

        if (ti.isInitialized() && ti.inCallGraph) {
            if (ti.inProfilingRuntimeMethod > 0) {
                return;
            }

            ti.inProfilingRuntimeMethod++;
            //System.out.println("++++++sleepEntry, depth = " + ti.stackDepth);
            writeWaitTimeEvent(METHOD_ENTRY_SLEEP, ti);
            ti.inProfilingRuntimeMethod--;
        }
    }

    protected static void sleepExitCPU() {
        if (recursiveInstrumentationDisabled) {
            return; // See the comment at the recursiveInstrumentationDisabled variable declaration
        }

        ThreadInfo ti = ThreadInfo.getThreadInfo();

        if (ti.isInitialized() && ti.inCallGraph) {
            if (ti.inProfilingRuntimeMethod > 0) {
                return;
            }

            ti.inProfilingRuntimeMethod++;
            //System.out.println("++++++sleepExit, depth = " + ti.stackDepth);
            writeWaitTimeEvent(METHOD_EXIT_SLEEP, ti);
            ti.inProfilingRuntimeMethod--;
        }
    }

    protected static void waitEntryCPU() {
        if (recursiveInstrumentationDisabled) {
            return; // See the comment at the recursiveInstrumentationDisabled variable declaration
        }

        ThreadInfo ti = ThreadInfo.getThreadInfo();

        if (ti.isInitialized() && ti.inCallGraph) {
            if (ti.inProfilingRuntimeMethod > 0) {
                return;
            }

            ti.inProfilingRuntimeMethod++;
            //System.out.println("++++++waitEntry, depth = " + ti.stackDepth);
            writeWaitTimeEvent(METHOD_ENTRY_WAIT, ti);
            ti.inProfilingRuntimeMethod--;
        }
    }

    protected static void waitExitCPU() {
        if (recursiveInstrumentationDisabled) {
            return; // See the comment at the recursiveInstrumentationDisabled variable declaration
        }

        ThreadInfo ti = ThreadInfo.getThreadInfo();

        if (ti.isInitialized() && ti.inCallGraph) {
            if (ti.inProfilingRuntimeMethod > 0) {
                return;
            }

            ti.inProfilingRuntimeMethod++;
            //System.out.println("++++++waitExit, depth = " + ti.stackDepth);
            writeWaitTimeEvent(METHOD_EXIT_WAIT, ti);
            ti.inProfilingRuntimeMethod--;
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

        long threadInterval = Timers.getThreadCPUTimeInNanos() - threadTimeStamp;
        evBuf[curPos++] = (byte) ((threadInterval >> 48) & 0xFF);
        evBuf[curPos++] = (byte) ((threadInterval >> 40) & 0xFF);
        evBuf[curPos++] = (byte) ((threadInterval >> 32) & 0xFF);
        evBuf[curPos++] = (byte) ((threadInterval >> 24) & 0xFF);
        evBuf[curPos++] = (byte) ((threadInterval >> 16) & 0xFF);
        evBuf[curPos++] = (byte) ((threadInterval >> 8) & 0xFF);
        evBuf[curPos++] = (byte) ((threadInterval) & 0xFF);

        ti.evBufPos = curPos;
    }

    static void writeServletDoMethod(ThreadInfo ti, String method, String servletPath, String sessionId) {
        int fullInfoLen = 1 + 1 + (servletPath.length() * 2) + 4;
        int curPos = ti.evBufPos; // It's important to use a local copy for evBufPos, so that evBufPos is at event boundary at any moment

        if ((curPos + fullInfoLen) > ThreadInfo.evBufPosThreshold) {
            copyLocalBuffer(ti);
            curPos = ti.evBufPos;
        }

        byte[] evBuf = ti.evBuf;
        byte methodId = -1;
        int sessionHash = -1;

        if ("GET".equals(method)) { // NOI18N
            methodId = 1;
        } else if ("POST".equals(method)) { // NOI18N
            methodId = 2;
        } else if ("PUT".equals(method)) { // NOI18N
            methodId = 3;
        } else if ("DELETE".equals(method)) { // NOI18N
            methodId = 4;
        }

        if (sessionId != null) {
            sessionHash = sessionId.hashCode();
        }

        evBuf[curPos++] = SERVLET_DO_METHOD;
        evBuf[curPos++] = methodId;

        byte[] name = servletPath.getBytes();
        int len = name.length;
        evBuf[curPos++] = (byte) ((len >> 8) & 0xFF);
        evBuf[curPos++] = (byte) ((len) & 0xFF);
        System.arraycopy(name, 0, evBuf, curPos, len);
        curPos += len;
        evBuf[curPos++] = (byte) ((sessionHash >> 24) & 0xFF);
        evBuf[curPos++] = (byte) ((sessionHash >> 16) & 0xFF);
        evBuf[curPos++] = (byte) ((sessionHash >> 8) & 0xFF);
        evBuf[curPos++] = (byte) ((sessionHash) & 0xFF);
        ti.evBufPos = curPos;
    }

    static void writeThreadCreationEvent(ThreadInfo ti) {
        Thread thread = ti.thread;
        String threadName = thread.getName();
        String threadClassName = thread.getClass().getName();
        int fullInfoLen = ((threadName.length() + threadClassName.length()) * 2) + 7;

        synchronized (eventBuffer) {
            if ((globalEvBufPos + fullInfoLen) > globalEvBufPosThreshold) {
                sendingBuffer = true;
                externalActionsHandler.handleEventBufferDump(eventBuffer, 0, globalEvBufPos);
                globalEvBufPos = 0;
                sendingBuffer = false;
            }

            eventBuffer[globalEvBufPos++] = NEW_THREAD;

            int threadId = ti.getThreadId();
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

    // ---------------------------------- Writing profiler events --------------------------------------
    static void writeTimeStampedEvent(byte eventType, ThreadInfo ti, char methodId) {
        int curPos = ti.evBufPos; // It's important to use a local copy for evBufPos, so that evBufPos is at event boundary at any moment

        if (curPos > ThreadInfo.evBufPosThreshold) {
            copyLocalBuffer(ti);
            curPos = ti.evBufPos;
        }

        byte[] evBuf = ti.evBuf;
        if (!ti.isInitialized()) return;    // Reset collectors performed when we were already executing instrumentation code 
        evBuf[curPos++] = eventType;
        evBuf[curPos++] = (byte) ((methodId >> 8) & 0xFF);
        evBuf[curPos++] = (byte) ((methodId) & 0xFF);

        // Note that in the code below, we write only the 7 low bytes of the 64-bit timestamp. The justification is that this saves
        // us some performance and memory, and 2^55 == 36028797018963968 ns == 36028797 sec == 10008 hr == 416 days is a sufficent
        // representation range for the foreseeable usages of our tool. (***)
        if (absoluteTimerOn || (eventType < TWO_TIMESTAMP_EVENTS)) {
            long absTimeStamp = Timers.getCurrentTimeInCounts();
            evBuf[curPos++] = (byte) ((absTimeStamp >> 48) & 0xFF);
            evBuf[curPos++] = (byte) ((absTimeStamp >> 40) & 0xFF);
            evBuf[curPos++] = (byte) ((absTimeStamp >> 32) & 0xFF);
            evBuf[curPos++] = (byte) ((absTimeStamp >> 24) & 0xFF);
            evBuf[curPos++] = (byte) ((absTimeStamp >> 16) & 0xFF);
            evBuf[curPos++] = (byte) ((absTimeStamp >> 8) & 0xFF);
            evBuf[curPos++] = (byte) ((absTimeStamp) & 0xFF);

            if (DEBUG) {
                System.out.println("ProfilerRuntimeCPU.DEBUG: Writing event (Abs) type = " + eventType + ", metodId = "
                                   + (int) methodId + ", timestamp: " + absTimeStamp); // NOI18N
            }
        }

        if (threadCPUTimerOn || (eventType < TWO_TIMESTAMP_EVENTS)) {
            long threadTimeStamp = Timers.getThreadCPUTimeInNanos();
            evBuf[curPos++] = (byte) ((threadTimeStamp >> 48) & 0xFF);
            evBuf[curPos++] = (byte) ((threadTimeStamp >> 40) & 0xFF);
            evBuf[curPos++] = (byte) ((threadTimeStamp >> 32) & 0xFF);
            evBuf[curPos++] = (byte) ((threadTimeStamp >> 24) & 0xFF);
            evBuf[curPos++] = (byte) ((threadTimeStamp >> 16) & 0xFF);
            evBuf[curPos++] = (byte) ((threadTimeStamp >> 8) & 0xFF);
            evBuf[curPos++] = (byte) ((threadTimeStamp) & 0xFF);

            if (DEBUG) {
                System.out.println("ProfilerRuntimeCPU.DEBUG: Writing event (CPU) type = " + eventType + ", metodId = "
                                   + (int) methodId + ", timestamp: " + threadTimeStamp); // NOI18N
            }
        }

        ti.evBufPos = curPos;
    }

    static void writeWaitTimeEvent(byte eventType, ThreadInfo ti) {
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
            System.out.println("ProfilerRuntimeCPU.DEBUG: Writing waitTime event type = " + eventType + ", timestamp: "
                               + absTimeStamp); // NOI18N
        }

        evBuf[curPos++] = (byte) ((absTimeStamp >> 48) & 0xFF);
        evBuf[curPos++] = (byte) ((absTimeStamp >> 40) & 0xFF);
        evBuf[curPos++] = (byte) ((absTimeStamp >> 32) & 0xFF);
        evBuf[curPos++] = (byte) ((absTimeStamp >> 24) & 0xFF);
        evBuf[curPos++] = (byte) ((absTimeStamp >> 16) & 0xFF);
        evBuf[curPos++] = (byte) ((absTimeStamp >> 8) & 0xFF);
        evBuf[curPos++] = (byte) ((absTimeStamp) & 0xFF);

        ti.evBufPos = curPos;
    }

    private static void servletDoMethodHook(ThreadInfo ti, Object request) {
        String servletPath = null;
        String method = null;
        String requestedSessionId = null;

        if (getRequestedSessionIdMethod == null) {
            try {
                Class requestClass = request.getClass();
                getRequestedSessionIdMethod = requestClass.getMethod("getRequestedSessionId", null); // NOI18N
                getMethodMethod = requestClass.getMethod("getMethod", null); // NOI18N
                getServletPathMethod = requestClass.getMethod("getServletPath", null); // NOI18N
            } catch (Exception ex) {
                ex.printStackTrace();

                return;
            }
        }

        try {
            requestedSessionId = (String) getRequestedSessionIdMethod.invoke(request, null);
            method = (String) getMethodMethod.invoke(request, null);
            servletPath = (String) getServletPathMethod.invoke(request, null);
        } catch (Exception ex) {
            ex.printStackTrace();

            return;
        }

        writeServletDoMethod(ti, method, servletPath, requestedSessionId);
    }
}
