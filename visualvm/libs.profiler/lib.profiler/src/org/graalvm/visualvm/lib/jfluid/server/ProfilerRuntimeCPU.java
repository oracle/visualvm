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

package org.graalvm.visualvm.lib.jfluid.server;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import org.graalvm.visualvm.lib.jfluid.global.Platform;
import org.graalvm.visualvm.lib.jfluid.server.system.Stacks;
import org.graalvm.visualvm.lib.jfluid.server.system.Timers;

/**
 * This class contains the functionality that is common for all CPU profiling methods available in JFluid.
 *
 * @author Tomas Hurka
 * @author Misha Dmitriev
 */
public class ProfilerRuntimeCPU extends ProfilerRuntime {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final boolean DEBUG = false;
    private static final int MAX_STRING_LENGTH = 2048;
    static final Object NO_RET_VALUE = new Object();
    private static int nProfiledThreadsLimit;
    protected static int nProfiledThreadsAllowed;
    protected static int stackDepthLimit = Integer.MAX_VALUE;
    protected static boolean enableFirstTimeMethodInvoke;

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

    protected static boolean waitTrackingEnabled;
    protected static boolean sleepTrackingEnabled;
    
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

    public static void setStackDepthLimit(int num) {
        stackDepthLimit = num;
    }

    public static void setWaitAndSleepTracking(boolean waitTracking, boolean sleepTracking) {
        waitTrackingEnabled = waitTracking;
        sleepTrackingEnabled = sleepTracking;
    }
    
    public static void setTimerTypes(boolean absolute, boolean threadCPU) {
        if (threadCPU != threadCPUTimerOn && Platform.isSolaris()) {
            Timers.enableMicrostateAccounting(threadCPU);
        }

        absoluteTimerOn = absolute;
        threadCPUTimerOn = threadCPU;
    }
    
    public static void enableFirstTimeMethodInvoke(boolean enabled) {
        enableFirstTimeMethodInvoke = enabled;
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
        ti.absEntryTime = Timers.getCurrentTimeInCounts();
        if (threadCPUTimerOn) {
            ti.threadEntryTime = Timers.getThreadCPUTimeInNanos();
        }
        return ti;
    }

    protected static void clearDataStructures() {
        ProfilerRuntime.clearDataStructures();
        nProfiledThreadsAllowed = nProfiledThreadsLimit;
    }
    
    protected static void createNewDataStructures() {
        ProfilerRuntime.createNewDataStructures();
        // top level Marker method has stacktrace 
        Stacks.createNativeStackFrameBuffer(ProfilerRuntimeMemory.MAX_STACK_FRAMES);
    }

    protected static long currentTimeInCounts() {
        return Timers.getCurrentTimeInCounts();
    }

    protected static void enableProfiling(boolean v) {
        recursiveInstrumentationDisabled = !v;

        // Doesn't call clearDataStructures() since this is an "abstract" class
    }

    // ---------------------------------- Handling wait/sleep/monitor times ----------------------------
    protected static long monitorEntryCPU(ThreadInfo ti, Object monitor, ThreadInfo ownerTi) {
        if (recursiveInstrumentationDisabled || !waitTrackingEnabled) {
            return -1; // See the comment at the recursiveInstrumentationDisabled variable declaration
        }

        if (ti.isInitialized() && ti.inCallGraph) {
            //System.out.println("++++++monitorEntry, depth = " + ti.stackDepth);
            return writeWaitTimeEvent(METHOD_ENTRY_MONITOR, ti, monitor, ownerTi);
        }
        return -1;
    }

    protected static long monitorExitCPU(ThreadInfo ti, Object monitor) {
        if (recursiveInstrumentationDisabled || !waitTrackingEnabled) {
            return -1; // See the comment at the recursiveInstrumentationDisabled variable declaration
        }

        if (ti.isInitialized() && ti.inCallGraph) {
            //System.out.println("++++++monitorExit, depth = " + ti.stackDepth);
            return writeWaitTimeEvent(METHOD_EXIT_MONITOR, ti, monitor);
        }
        return -1;
    }

    protected static long sleepEntryCPU(ThreadInfo ti) {
        if (recursiveInstrumentationDisabled || !sleepTrackingEnabled) {
            return -1; // See the comment at the recursiveInstrumentationDisabled variable declaration
        }
        if (ti.isInitialized() && ti.inCallGraph) {
            //System.out.println("++++++sleepEntry, depth = " + ti.stackDepth);
            return writeWaitTimeEvent(METHOD_ENTRY_SLEEP, ti);
        }
        return -1;
    }

    protected static long sleepExitCPU(ThreadInfo ti) {
        if (recursiveInstrumentationDisabled || !sleepTrackingEnabled) {
            return -1; // See the comment at the recursiveInstrumentationDisabled variable declaration
        }

        if (ti.isInitialized() && ti.inCallGraph) {
            //System.out.println("++++++sleepExit, depth = " + ti.stackDepth);
            return writeWaitTimeEvent(METHOD_EXIT_SLEEP, ti);
        }
        return -1;
    }

    protected static long waitEntryCPU(ThreadInfo ti) {
        if (recursiveInstrumentationDisabled || !waitTrackingEnabled) {
            return -1; // See the comment at the recursiveInstrumentationDisabled variable declaration
        }

        if (ti.isInitialized() && ti.inCallGraph) {
            //System.out.println("++++++waitEntry, depth = " + ti.stackDepth);
            return writeWaitTimeEvent(METHOD_ENTRY_WAIT, ti);
        }
        return -1;
    }

    protected static long waitExitCPU(ThreadInfo ti) {
        if (recursiveInstrumentationDisabled || !waitTrackingEnabled) {
            return -1; // See the comment at the recursiveInstrumentationDisabled variable declaration
        }

        if (ti.isInitialized() && ti.inCallGraph) {
            //System.out.println("++++++waitExit, depth = " + ti.stackDepth);
            return writeWaitTimeEvent(METHOD_EXIT_WAIT, ti);
        }
        return -1;
    }

    protected static long parkEntryCPU(ThreadInfo ti) {
        if (recursiveInstrumentationDisabled || !waitTrackingEnabled) {
            return -1; // See the comment at the recursiveInstrumentationDisabled variable declaration
        }

        if (ti.isInitialized() && ti.inCallGraph) {
            //System.out.println("++++++parkEntry, depth = " + ti.stackDepth);
            return writeWaitTimeEvent(METHOD_ENTRY_PARK, ti);
        }
        return -1;
    }

    protected static long parkExitCPU(ThreadInfo ti) {
        if (recursiveInstrumentationDisabled || !waitTrackingEnabled) {
            return -1; // See the comment at the recursiveInstrumentationDisabled variable declaration
        }

        if (ti.isInitialized() && ti.inCallGraph) {
            //System.out.println("++++++parkExit, depth = " + ti.stackDepth);
            return writeWaitTimeEvent(METHOD_EXIT_PARK, ti);
        }
        return -1;
    }

    protected static void firstTimeMethodInvoke(final ThreadInfo ti, final char methodId) {
        if (enableFirstTimeMethodInvoke) {
            long absTimeStamp = Timers.getCurrentTimeInCounts();
            long threadTimeStamp = threadCPUTimerOn ? Timers.getThreadCPUTimeInNanos() : 0;
            externalActionsHandler.handleFirstTimeMethodInvoke(methodId);
            writeAdjustTimeEvent(ti, absTimeStamp, threadTimeStamp);
        }
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

    // ---------------------------------- Writing profiler events --------------------------------------
    static void writeTimeStampedEvent(byte eventType, ThreadInfo ti, char methodId) {
        int[] stackFrameIds = null;
        int currentStackDepth = 0;
        if (eventType == MARKER_ENTRY) {
            // top-level marker method has stack trace
            if (ti.stackDepth == 0) {
                stackFrameIds = new int[ProfilerRuntimeMemory.MAX_STACK_FRAMES];
                currentStackDepth = Stacks.getCurrentStackFrameIds(ti.getThread(), stackFrameIds.length, stackFrameIds);
                currentStackDepth -= ProfilerRuntimeMemory.NO_OF_PROFILER_FRAMES;
            } else {
                stackFrameIds = new int[0];
            }
        }
        int curPos = ti.evBufPos; // It's important to use a local copy for evBufPos, so that evBufPos is at event boundary at any moment

        if (curPos + currentStackDepth*4 > ThreadInfo.evBufPosThreshold) {
            copyLocalBuffer(ti);
            curPos = ti.evBufPos;
        }

        byte[] evBuf = ti.evBuf;
        if (!ti.isInitialized()) return;    // Reset collectors performed when we were already executing instrumentation code 
        evBuf[curPos++] = eventType;
        evBuf[curPos++] = (byte) ((methodId >> 8) & 0xFF);
        evBuf[curPos++] = (byte) ((methodId) & 0xFF);

        // Note that in the code below, we write only the 7 low bytes of the 64-bit timestamp. The justification is that this saves
        // us some performance and memory, and 2^55 == 36028797018963968 ns == 36028797 sec == 10008 hr == 416 days is a sufficient
        // representation range for the foreseeable usages of our tool. (***)
        if (absoluteTimerOn) {
            long absTimeStamp = Timers.getCurrentTimeInCounts();
            evBuf[curPos++] = (byte) ((absTimeStamp >> 48) & 0xFF);
            evBuf[curPos++] = (byte) ((absTimeStamp >> 40) & 0xFF);
            evBuf[curPos++] = (byte) ((absTimeStamp >> 32) & 0xFF);
            evBuf[curPos++] = (byte) ((absTimeStamp >> 24) & 0xFF);
            evBuf[curPos++] = (byte) ((absTimeStamp >> 16) & 0xFF);
            evBuf[curPos++] = (byte) ((absTimeStamp >> 8) & 0xFF);
            evBuf[curPos++] = (byte) ((absTimeStamp) & 0xFF);

            if (DEBUG) {
                System.out.println("ProfilerRuntimeCPU.DEBUG: Writing event (Abs) type = " + eventType + ", methodId = "
                                   + (int) methodId + ", timestamp: " + absTimeStamp); // NOI18N
            }
        }

        if (threadCPUTimerOn) {
            long threadTimeStamp = Timers.getThreadCPUTimeInNanos();
            evBuf[curPos++] = (byte) ((threadTimeStamp >> 48) & 0xFF);
            evBuf[curPos++] = (byte) ((threadTimeStamp >> 40) & 0xFF);
            evBuf[curPos++] = (byte) ((threadTimeStamp >> 32) & 0xFF);
            evBuf[curPos++] = (byte) ((threadTimeStamp >> 24) & 0xFF);
            evBuf[curPos++] = (byte) ((threadTimeStamp >> 16) & 0xFF);
            evBuf[curPos++] = (byte) ((threadTimeStamp >> 8) & 0xFF);
            evBuf[curPos++] = (byte) ((threadTimeStamp) & 0xFF);

            if (DEBUG) {
                System.out.println("ProfilerRuntimeCPU.DEBUG: Writing event (CPU) type = " + eventType + ", methodId = "
                                   + (int) methodId + ", timestamp: " + threadTimeStamp); // NOI18N
            }
        }
        if (stackFrameIds != null) {
            evBuf[curPos++] = (byte) ((currentStackDepth >> 16) & 0xFF);
            evBuf[curPos++] = (byte) ((currentStackDepth >> 8) & 0xFF);
            evBuf[curPos++] = (byte) ((currentStackDepth) & 0xFF);
            int frameIdx = ProfilerRuntimeMemory.NO_OF_PROFILER_FRAMES;

            for (int i = 0; i < currentStackDepth; i++) {
                evBuf[curPos++] = (byte) ((stackFrameIds[frameIdx] >> 24) & 0xFF);
                evBuf[curPos++] = (byte) ((stackFrameIds[frameIdx] >> 16) & 0xFF);
                evBuf[curPos++] = (byte) ((stackFrameIds[frameIdx] >> 8) & 0xFF);
                evBuf[curPos++] = (byte) ((stackFrameIds[frameIdx]) & 0xFF);
                frameIdx++;
            }
        }

        ti.evBufPos = curPos;
    }
    
    static long writeWaitTimeEvent(byte eventType, ThreadInfo ti) {
        return writeWaitTimeEvent(eventType, ti, null);
    }
    
    static void writeParametersEvent(ThreadInfo ti) {
        List pars = ti.getParameters();
        int parsLength = 0;
        if (pars != null) {
            for (int i = 0; i < pars.size(); i++) {
                parsLength += 1 + getParSize(pars.get(i));
            }
        }
        int fullInfoLen = 1 + 1 + parsLength;
        int curPos = ti.evBufPos; // It's important to use a local copy for evBufPos, so that evBufPos is at event boundary at any moment

        if ((curPos + fullInfoLen) > ThreadInfo.evBufPosThreshold) {
            copyLocalBuffer(ti);
            curPos = ti.evBufPos;
        }

        byte[] evBuf = ti.evBuf;

        evBuf[curPos++] = MARKER_ENTRY_PARAMETERS;
        if (pars != null) {
            evBuf[curPos++] = (byte) pars.size();

            for (int i = 0; i < pars.size(); i++) {
                curPos = writeParameter(evBuf, curPos, pars.get(i));
            }
            ti.clearParameters();
        } else {
            evBuf[curPos++] = 0;            
        }
        ti.evBufPos = curPos;
    }

    static void writeRetValue(Object ret, ThreadInfo ti) {
        if (ret != NO_RET_VALUE) {
            ti.addParameter(ret != null ? convertToString(ret) : "");
            writeParametersEvent(ti);
        }
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
    
    public static void addParameter(boolean b) {
        if (recursiveInstrumentationDisabled) {
            return;
        }

        ThreadInfo ti = ThreadInfo.getThreadInfo();

        if (ti.inProfilingRuntimeMethod > 0) {
            return;
        }
        ti.inProfilingRuntimeMethod++;
        ti.addParameter(Boolean.valueOf(b));
        ti.inProfilingRuntimeMethod--; 
    }

    public static void addParameter(char b) {
        if (recursiveInstrumentationDisabled) {
            return;
        }

        ThreadInfo ti = ThreadInfo.getThreadInfo();

        if (ti.inProfilingRuntimeMethod > 0) {
            return;
        }

        ti.inProfilingRuntimeMethod++;
        ti.addParameter(new Character(b));
        ti.inProfilingRuntimeMethod--; 
    }

    public static void addParameter(byte b) {
        if (recursiveInstrumentationDisabled) {
            return;
        }

        ThreadInfo ti = ThreadInfo.getThreadInfo();

        if (ti.inProfilingRuntimeMethod > 0) {
            return;
        }

        ti.inProfilingRuntimeMethod++;
        ti.addParameter(new Byte(b));
        ti.inProfilingRuntimeMethod--; 
    }

    public static void addParameter(short b) {
        if (recursiveInstrumentationDisabled) {
            return;
        }

        ThreadInfo ti = ThreadInfo.getThreadInfo();

        if (ti.inProfilingRuntimeMethod > 0) {
            return;
        }

        ti.inProfilingRuntimeMethod++;
        ti.addParameter(new Short(b));
        ti.inProfilingRuntimeMethod--; 
    }

    public static void addParameter(int b) {
        if (recursiveInstrumentationDisabled) {
            return;
        }

        ThreadInfo ti = ThreadInfo.getThreadInfo();

        if (ti.inProfilingRuntimeMethod > 0) {
            return;
        }

        ti.inProfilingRuntimeMethod++;
        ti.addParameter(new Integer(b));
        ti.inProfilingRuntimeMethod--; 
    }

    public static void addParameter(long b) {
        if (recursiveInstrumentationDisabled) {
            return;
        }

        ThreadInfo ti = ThreadInfo.getThreadInfo();

        if (ti.inProfilingRuntimeMethod > 0) {
            return;
        }

        ti.inProfilingRuntimeMethod++;
        ti.addParameter(new Long(b));
        ti.inProfilingRuntimeMethod--; 
    }

    public static void addParameter(float b) {
        if (recursiveInstrumentationDisabled) {
            return;
        }

        ThreadInfo ti = ThreadInfo.getThreadInfo();

        if (ti.inProfilingRuntimeMethod > 0) {
            return;
        }

        ti.inProfilingRuntimeMethod++;
        ti.addParameter(new Float(b));
        ti.inProfilingRuntimeMethod--; 
    }

    public static void addParameter(double b) {
        if (recursiveInstrumentationDisabled) {
            return;
        }

        ThreadInfo ti = ThreadInfo.getThreadInfo();

        if (ti.inProfilingRuntimeMethod > 0) {
            return;
        }

        ti.inProfilingRuntimeMethod++;
        ti.addParameter(new Double(b));
        ti.inProfilingRuntimeMethod--; 
    }

    public static void addParameter(Object b) {
        if (recursiveInstrumentationDisabled) {
            return;
        }

        ThreadInfo ti = ThreadInfo.getThreadInfo();

        if (ti.inProfilingRuntimeMethod > 0) {
            return;
        }

        ti.inProfilingRuntimeMethod++;
        ti.addParameter(b != null ? convertToString(b) : "");
        ti.inProfilingRuntimeMethod--; 
    }
    
    private static int getParSize(Object p) {
        Class type = p.getClass();
        if (type == Integer.class) {
            return 4;
        } else if (type == Boolean.class) {
            return 1;
        } else if (type == Byte.class) {
            return 1;
        } else if (type == Short.class) {
            return 2;
        } else if (type == Character.class) {
            return 2;
        } else if (type == Long.class) {
            return 8;
        } else if (type == Float.class) {
            return 4;
        } else if (type == Double.class) {
            return 8;
        } else {
            return 2 + truncatedByteLength((String) p);
        }
    }
    
    private static int writeParameter(byte[] evBuf, int curPos, Object p) {
        Class type = p.getClass();
        if (type == Integer.class) {
            int vp = ((Integer)p).intValue();
            evBuf[curPos++] = ProfilerInterface.INT;
            evBuf[curPos++] = (byte) ((vp >> 24) & 0xFF);
            evBuf[curPos++] = (byte) ((vp >> 16) & 0xFF);
            evBuf[curPos++] = (byte) ((vp >> 8) & 0xFF);
            evBuf[curPos++] = (byte) ((vp) & 0xFF);
        } else if (type == Boolean.class) {
            boolean vp = ((Boolean)p).booleanValue();
            evBuf[curPos++] = ProfilerInterface.BOOLEAN;
            evBuf[curPos++] = (byte) (vp ? 1 : 0);
        } else if (type == Byte.class) {
            byte vp = ((Byte)p).byteValue();
            evBuf[curPos++] = ProfilerInterface.BYTE;
            evBuf[curPos++] = vp;
        } else if (type == Short.class) {
            short vp = ((Short)p).shortValue();
            evBuf[curPos++] = ProfilerInterface.SHORT;
            evBuf[curPos++] = (byte) ((vp >> 8) & 0xFF);
            evBuf[curPos++] = (byte) ((vp) & 0xFF);
        } else if (type == Character.class) {
            char vp = ((Character)p).charValue();
            evBuf[curPos++] = ProfilerInterface.CHAR;
            evBuf[curPos++] = (byte) ((vp >> 8) & 0xFF);
            evBuf[curPos++] = (byte) ((vp) & 0xFF);
        } else if (type == Long.class) {
            long vp = ((Long)p).longValue();
            evBuf[curPos++] = ProfilerInterface.LONG;
            evBuf[curPos++] = (byte) ((vp >> 56) & 0xFF);
            evBuf[curPos++] = (byte) ((vp >> 48) & 0xFF);
            evBuf[curPos++] = (byte) ((vp >> 40) & 0xFF);
            evBuf[curPos++] = (byte) ((vp >> 32) & 0xFF);
            evBuf[curPos++] = (byte) ((vp >> 24) & 0xFF);
            evBuf[curPos++] = (byte) ((vp >> 16) & 0xFF);
            evBuf[curPos++] = (byte) ((vp >> 8) & 0xFF);
            evBuf[curPos++] = (byte) ((vp) & 0xFF); 
        } else if (type == Float.class) {
            int vp = Float.floatToIntBits(((Float)p).floatValue());
            evBuf[curPos++] = ProfilerInterface.FLOAT;
            evBuf[curPos++] = (byte) ((vp >> 24) & 0xFF);
            evBuf[curPos++] = (byte) ((vp >> 16) & 0xFF);
            evBuf[curPos++] = (byte) ((vp >> 8) & 0xFF);
            evBuf[curPos++] = (byte) ((vp) & 0xFF);
        } else if (type == Double.class) {
            long vp = Double.doubleToLongBits(((Double)p).doubleValue());
            evBuf[curPos++] = ProfilerInterface.DOUBLE;
            evBuf[curPos++] = (byte) ((vp >> 56) & 0xFF);
            evBuf[curPos++] = (byte) ((vp >> 48) & 0xFF);
            evBuf[curPos++] = (byte) ((vp >> 40) & 0xFF);
            evBuf[curPos++] = (byte) ((vp >> 32) & 0xFF);
            evBuf[curPos++] = (byte) ((vp >> 24) & 0xFF);
            evBuf[curPos++] = (byte) ((vp >> 16) & 0xFF);
            evBuf[curPos++] = (byte) ((vp >> 8) & 0xFF);
            evBuf[curPos++] = (byte) ((vp) & 0xFF);
        } else {    
            String sp = (String) p;
            int lengthBytes = truncatedByteLength(sp);
            evBuf[curPos++] = ProfilerInterface.REFERENCE;
            evBuf[curPos++] = (byte) ((lengthBytes >> 8) & 0xFF);
            evBuf[curPos++] = (byte) ((lengthBytes) & 0xFF);
            for (int i = 0; i < lengthBytes/2; i++) {
                char ch = sp.charAt(i);
                evBuf[curPos++] = (byte) ((ch >> 8) & 0xFF);
                evBuf[curPos++] = (byte) ((ch) & 0xFF);
            }
        }
        return curPos;
    }
    
    static String convertToString(Object o) {
        String clazz = o.getClass().getName();
        
        if (clazz.startsWith("java.lang.")) {
            return o.toString();
        }
        if (clazz.equals("java.sql.Date")) {
            return String.valueOf(((Date)o).getTime());
        }
        if (clazz.equals("java.sql.Timestamp")) {
            return String.valueOf(((Date)o).getTime());
        }
        if (clazz.equals("java.math.BigDecimal")) {
            return o.toString();
        }
        return getObjectId(o, clazz);
    }
    
    private static int truncatedByteLength(String s) {
        int length = s.length()*2;
        
        if (length < MAX_STRING_LENGTH) {
            return length;
        }
        return MAX_STRING_LENGTH;
    }
    
    private static String getObjectId(Object o, String clazz) {
        return clazz + "@" + Integer.toHexString(System.identityHashCode(o));
    }
}
