/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;
import org.graalvm.visualvm.lib.jfluid.results.ProfilingResultListener;
import org.graalvm.visualvm.lib.jfluid.results.locks.AbstractLockDataFrameProcessor;

import static org.graalvm.visualvm.lib.jfluid.utils.VMUtils.*;

/**
 * This class main purpose is to parse a chunk of CPU related data received from
 * the JFluid server agent and dispatch the resulting events to all interested parties
 * @author Jaroslav Bachorik
 * @author Tomas Hurka
 */
public class CPUDataFrameProcessor extends AbstractLockDataFrameProcessor {

    private boolean hasMonitorInfo;
    private Map methodParameters = new HashMap();
    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void doProcessDataFrame(ByteBuffer buffer) {
        hasMonitorInfo = (client != null) ? client.getSettings().isLockContentionMonitoringEnabled() : false;

        try {
            while (buffer.hasRemaining()) {
                byte eventType = buffer.get();

                if ((eventType & CommonConstants.COMPACT_EVENT_FORMAT_BYTE_MASK) != 0) {
                    char charEvent = (char) ((((int) eventType & 0xFF) << 8) | ((int) buffer.get() & 0xFF));

                    if ((byte) (eventType & CommonConstants.METHOD_EXIT_COMPACT_BYTE_MASK) == CommonConstants.METHOD_EXIT_COMPACT_BYTE_MASK) {
                        fireMethodExitUnstamped(charEvent & CommonConstants.COMPACT_EVENT_METHOD_ID_MASK, currentThreadId,
                                                CPUProfilingResultListener.METHODTYPE_NORMAL);
                    } else {
                        fireMethodEntryUnstamped(charEvent & CommonConstants.COMPACT_EVENT_METHOD_ID_MASK, currentThreadId,
                                                 CPUProfilingResultListener.METHODTYPE_NORMAL, null, null);
                    }

                    continue;
                }

                if (!((eventType == CommonConstants.BUFFEREVENT_PROFILEPOINT_HIT) || (eventType == CommonConstants.SERVLET_DO_METHOD)
                        || (eventType == CommonConstants.SET_FOLLOWING_EVENTS_THREAD) || (eventType == CommonConstants.NEW_THREAD)
                        || (eventType == CommonConstants.RESET_COLLECTORS) || (eventType == CommonConstants.NEW_MONITOR)
                        || (eventType == CommonConstants.MARKER_ENTRY_PARAMETERS))) {
                    int methodId = -1;
                    long timeStamp0 = 0;
                    long timeStamp1 = 0;
                    int hash = -1;
                    int ownerThreadId = -1;
                    
                    if ((eventType != CommonConstants.ADJUST_TIME // those events do not carry methodId
                        ) && (eventType != CommonConstants.METHOD_ENTRY_WAIT) 
                            && (eventType != CommonConstants.METHOD_EXIT_WAIT)
                            && (eventType != CommonConstants.METHOD_ENTRY_PARK) 
                            && (eventType != CommonConstants.METHOD_EXIT_PARK)
                            && (eventType != CommonConstants.METHOD_ENTRY_MONITOR)
                            && (eventType != CommonConstants.METHOD_EXIT_MONITOR)
                            && (eventType != CommonConstants.METHOD_ENTRY_SLEEP) 
                            && (eventType != CommonConstants.METHOD_EXIT_SLEEP)) {
                        methodId = buffer.getChar();
                    }

                    if ((eventType != CommonConstants.METHOD_ENTRY_UNSTAMPED) && (eventType != CommonConstants.METHOD_EXIT_UNSTAMPED)
                            && (eventType != CommonConstants.MARKER_ENTRY_UNSTAMPED)
                            && (eventType != CommonConstants.MARKER_EXIT_UNSTAMPED)) {
                        timeStamp0 = getTimeStamp(buffer);

                        if ((eventType != CommonConstants.METHOD_ENTRY_WAIT)
                                && (eventType != CommonConstants.METHOD_EXIT_WAIT)
                                && (eventType != CommonConstants.METHOD_ENTRY_PARK)
                                && (eventType != CommonConstants.METHOD_EXIT_PARK)
                                && (eventType != CommonConstants.METHOD_ENTRY_MONITOR)
                                && (eventType != CommonConstants.METHOD_EXIT_MONITOR)
                                && (eventType != CommonConstants.METHOD_ENTRY_SLEEP)
                                && (eventType != CommonConstants.METHOD_EXIT_SLEEP)) {
                            if (collectingTwoTimeStamps) {
                                timeStamp1 = getTimeStamp(buffer);
                            }
                        }
                        if (hasMonitorInfo && (eventType == CommonConstants.METHOD_ENTRY_MONITOR || eventType == CommonConstants.METHOD_EXIT_MONITOR)) {
                            hash = buffer.getInt();
                            if (eventType == CommonConstants.METHOD_ENTRY_MONITOR) {
                                ownerThreadId = buffer.getInt();
                            }
                        }
                    }

                    switch (eventType) {
                        case CommonConstants.MARKER_ENTRY_UNSTAMPED: {
                            List parameters = (List) methodParameters.get(Integer.valueOf(currentThreadId));
                            if (LOGGER.isLoggable(Level.FINEST)) {
                                LOGGER.log(Level.FINEST, "Marker entry unstamped, tId={0}, mId={1}, pars={2}", new Object[]{currentThreadId, methodId, parameters.toString()}); // NOI18N
                            }

                            fireMethodEntryUnstamped(methodId, currentThreadId, CPUProfilingResultListener.METHODTYPE_MARKER, parameters, null);
                            methodParameters.remove(Integer.valueOf(currentThreadId));
                            break;
                        }
                        case CommonConstants.METHOD_ENTRY_UNSTAMPED: {
                            if (LOGGER.isLoggable(Level.FINEST)) {
                                LOGGER.log(Level.FINEST, "Method entry unstamped, tId={0}, mId={1}", new Object[]{currentThreadId, methodId}); // NOI18N
                            }

                            fireMethodEntryUnstamped(methodId, currentThreadId, CPUProfilingResultListener.METHODTYPE_NORMAL, null, null);

                            break;
                        }
                        case CommonConstants.MARKER_EXIT_UNSTAMPED: {
                            if (LOGGER.isLoggable(Level.FINEST)) {
                                LOGGER.log(Level.FINEST, "Marker exit unstamped, tId={0}, mId={1}", new Object[]{currentThreadId, methodId}); // NOI18N
                            }

                            fireMethodExitUnstamped(methodId, currentThreadId, CPUProfilingResultListener.METHODTYPE_MARKER);

                            break;
                        }
                        case CommonConstants.METHOD_EXIT_UNSTAMPED: {
                            if (LOGGER.isLoggable(Level.FINEST)) {
                                LOGGER.log(Level.FINEST, "Method exit unstamped, tId={0}, mId={1}", new Object[]{currentThreadId, methodId}); // NOI18N
                            }

                            fireMethodExitUnstamped(methodId, currentThreadId, CPUProfilingResultListener.METHODTYPE_NORMAL);

                            break;
                        }
                        case CommonConstants.MARKER_ENTRY: {
                            List parameters = (List) methodParameters.get(Integer.valueOf(currentThreadId));
                            int depth = getDepth(buffer);
                            int[] methodIds = new int[depth];

                            if (LOGGER.isLoggable(Level.FINEST)) {
                                LOGGER.log(Level.FINEST, "Marker entry , tId={0}, mId={1}, pars={2} depth={3}", new Object[]{currentThreadId, methodId, parameters.toString(), depth}); // NOI18N
                            }

                            for (int i = 0; i < depth; i++) {
                                methodIds[i] = buffer.getInt();
                            }

                            fireMethodEntry(methodId, currentThreadId, CPUProfilingResultListener.METHODTYPE_MARKER, timeStamp0,
                                            timeStamp1, parameters, methodIds);
                            methodParameters.remove(Integer.valueOf(currentThreadId));                            
                            break;
                        }
                        case CommonConstants.ROOT_ENTRY: {
                            if (LOGGER.isLoggable(Level.FINEST)) {
                                LOGGER.log(Level.FINEST, "Root entry , tId={0}, mId={1}", new Object[]{currentThreadId, methodId}); // NOI18N
                            }

                            fireMethodEntry(methodId, currentThreadId, CPUProfilingResultListener.METHODTYPE_ROOT, timeStamp0,
                                            timeStamp1);

                            break;
                        }
                        case CommonConstants.METHOD_ENTRY: {
                            if (LOGGER.isLoggable(Level.FINEST)) {
                                LOGGER.log(Level.FINEST, "Method entry , tId={0}, mId={1}", new Object[]{currentThreadId, methodId}); // NOI18N
                            }

                            fireMethodEntry(methodId, currentThreadId, CPUProfilingResultListener.METHODTYPE_NORMAL, timeStamp0,
                                            timeStamp1);

                            break;
                        }
                        case CommonConstants.MARKER_EXIT: {
                            List parameters = (List) methodParameters.get(Integer.valueOf(currentThreadId));
                            Object retVal = parameters == null ? null : parameters.get(0);
                            
                            if (LOGGER.isLoggable(Level.FINEST)) {
                                if (retVal != null) {
                                    LOGGER.log(Level.FINEST, "Marker exit , tId={0}, mId={1}, retVal={2}", new Object[]{currentThreadId, methodId, retVal}); // NOI18N                                    
                                } else {
                                    LOGGER.log(Level.FINEST, "Marker exit , tId={0}, mId={1}", new Object[]{currentThreadId, methodId}); // NOI18N
                                }
                            }

                            fireMethodExit(methodId, currentThreadId, CPUProfilingResultListener.METHODTYPE_MARKER, timeStamp0,
                                           timeStamp1, retVal);
                            methodParameters.remove(Integer.valueOf(currentThreadId));
                            break;
                        }
                        case CommonConstants.ROOT_EXIT: {
                            if (LOGGER.isLoggable(Level.FINEST)) {
                                LOGGER.log(Level.FINEST, "Root exit , tId={0}, mId={1}", new Object[]{currentThreadId, methodId}); // NOI18N
                            }

                            fireMethodExit(methodId, currentThreadId, CPUProfilingResultListener.METHODTYPE_ROOT, timeStamp0,
                                           timeStamp1, null);

                            break;
                        }
                        case CommonConstants.METHOD_EXIT: {
                            if (LOGGER.isLoggable(Level.FINEST)) {
                                LOGGER.log(Level.FINEST, "Method exit , tId={0}, mId={1}", new Object[]{currentThreadId, methodId}); // NOI18N
                            }

                            fireMethodExit(methodId, currentThreadId, CPUProfilingResultListener.METHODTYPE_NORMAL, timeStamp0,
                                           timeStamp1, null);

                            break;
                        }
                        case CommonConstants.ADJUST_TIME: {
                            if (LOGGER.isLoggable(Level.FINEST)) {
                                LOGGER.log(Level.FINEST, "Adjust time , tId={0}", currentThreadId); // NOI18N
                            }

                            fireAdjustTime(currentThreadId, timeStamp0, timeStamp1);

                            break;
                        }
                        case CommonConstants.METHOD_ENTRY_MONITOR: {
                            if (LOGGER.isLoggable(Level.FINEST)) {
                                LOGGER.log(Level.FINEST, "Monitor entry , tId={0} , monitorId={1}, ownerId={2}", new Object[]{currentThreadId,Integer.toHexString(hash),ownerThreadId}); // NOI18N
                            }

                            fireMonitorEntry(currentThreadId, timeStamp0, timeStamp1, hash, ownerThreadId);

                            break;
                        }
                        case CommonConstants.METHOD_EXIT_MONITOR: {
                            if (LOGGER.isLoggable(Level.FINEST)) {
                                LOGGER.log(Level.FINEST, "Monitor exit , tId={0} , monitorId={1}", new Object[]{currentThreadId,Integer.toHexString(hash)}); // NOI18N
                            }

                            fireMonitorExit(currentThreadId, timeStamp0, timeStamp1, hash);

                            break;
                        }
                        case CommonConstants.METHOD_ENTRY_SLEEP: {
                            if (LOGGER.isLoggable(Level.FINEST)) {
                                LOGGER.log(Level.FINEST, "Sleep entry , tId={0}", currentThreadId); // NOI18N
                            }

                            fireSleepEntry(currentThreadId, timeStamp0, timeStamp1);

                            break;
                        }
                        case CommonConstants.METHOD_EXIT_SLEEP: {
                            if (LOGGER.isLoggable(Level.FINEST)) {
                                LOGGER.log(Level.FINEST, "Sleep exit , tId={0}", currentThreadId); // NOI18N
                            }

                            fireSleepExit(currentThreadId, timeStamp0, timeStamp1);

                            break;
                        }
                        case CommonConstants.METHOD_ENTRY_WAIT: {
                            if (LOGGER.isLoggable(Level.FINEST)) {
                                LOGGER.log(Level.FINEST, "Wait entry , tId={0}", currentThreadId); // NOI18N
                            }

                            fireWaitEntry(currentThreadId, timeStamp0, timeStamp1);

                            break;
                        }
                        case CommonConstants.METHOD_EXIT_WAIT: {
                            if (LOGGER.isLoggable(Level.FINEST)) {
                                LOGGER.log(Level.FINEST, "Wait exit , tId={0}", currentThreadId); // NOI18N
                            }

                            fireWaitExit(currentThreadId, timeStamp0, timeStamp1);

                            break;
                        }
                        case CommonConstants.METHOD_ENTRY_PARK: {
                            if (LOGGER.isLoggable(Level.FINEST)) {
                                LOGGER.log(Level.FINEST, "Park entry , tId={0}", currentThreadId); // NOI18N
                            }

                            fireParkEntry(currentThreadId, timeStamp0, timeStamp1);

                            break;
                        }
                        case CommonConstants.METHOD_EXIT_PARK: {
                            if (LOGGER.isLoggable(Level.FINEST)) {
                                LOGGER.log(Level.FINEST, "Park exit , tId={0}", currentThreadId); // NOI18N
                            }

                            fireParkExit(currentThreadId, timeStamp0, timeStamp1);

                            break;
                        }
                        case CommonConstants.THREADS_SUSPENDED: {
                            if (LOGGER.isLoggable(Level.FINEST)) {
                                LOGGER.finest("Threads suspend"); // NOI18N
                            }

                            fireThreadsSuspend(timeStamp0, timeStamp1);

                            break;
                        }
                        case CommonConstants.THREADS_RESUMED: {
                            if (LOGGER.isLoggable(Level.FINEST)) {
                                LOGGER.finest("Threads resume"); // NOI18N
                            }

                            fireThreadsResumed(timeStamp0, timeStamp1);

                            break;
                        }
                        default: {
                            LOGGER.log(Level.SEVERE, "*** Profiler Engine: internal error: got unknown event type in CPUDataFrameProcessor: {0} at {1}", // NOI18N
                                                    new Object[]{(int) eventType, buffer.position()});
                            StringBuilder sb = new StringBuilder();
                            sb.append("dataframe [");   // NOI18N
                            buffer.rewind();
                            while (buffer.hasRemaining()) {
                                sb.append(buffer.get()).append(",");    // NOI18N
                            }
                            sb.append("]\n");   // NOI18N
                            LOGGER.severe(sb.toString());

                            break;
                        }
                    }
                } else {
                    switch (eventType) {
                        case CommonConstants.BUFFEREVENT_PROFILEPOINT_HIT: {
                            int id = buffer.getChar();
                            long timeStamp = getTimeStamp(buffer);
                            int threadId = buffer.getChar();
                            if (LOGGER.isLoggable(Level.FINEST)) {
                                LOGGER.finest("Profile Point Hit " + id + ", threadId=" + id + ", timeStamp=" + timeStamp); // NOI18N
                            }
                            fireProfilingPoint(threadId, id, timeStamp);

                            break;
                        }
                        case CommonConstants.SET_FOLLOWING_EVENTS_THREAD: {
                            currentThreadId = buffer.getChar();
                            if (LOGGER.isLoggable(Level.FINEST)) {
                                LOGGER.log(Level.FINEST, "Change current thread , tId={0}", currentThreadId); // NOI18N
                            }

                            break;
                        }
                        case CommonConstants.NEW_THREAD: {
                            int threadId = buffer.getChar();
                            String threadName = getString(buffer);
                            String threadClassName = getString(buffer);

                            if (LOGGER.isLoggable(Level.FINEST)) {
                                LOGGER.log(Level.FINEST, "Creating new thread , tId={0}", threadId); // NOI18N
                            }

                            fireNewThread(threadId, threadName, threadClassName);
                            currentThreadId = threadId;

                            break;
                        }
                        case CommonConstants.NEW_MONITOR: {
                            int hash = buffer.getInt();
                            String className = getString(buffer);

                            if (LOGGER.isLoggable(Level.FINEST)) {
                                LOGGER.log(Level.FINEST, "Creating new monitor , monitorId={0} , className={1}", new Object[] {Integer.toHexString(hash), className}); // NOI18N
                            }
                            fireNewMonitor(hash, className);
                            break;
                        }
                        case CommonConstants.SERVLET_DO_METHOD: {
                            if (LOGGER.isLoggable(Level.FINEST)) {
                                LOGGER.log(Level.FINEST, "Servlet track start , tId={0}", currentThreadId); // NOI18N
                            }

                            byte requestType = buffer.get();
                            String servletPath = getString(buffer);
                            int sessionId = buffer.getInt();
                            
                            fireServletRequest(currentThreadId, requestType, servletPath, sessionId);

                            break;
                        }
                        case CommonConstants.MARKER_ENTRY_PARAMETERS: {
                            if (LOGGER.isLoggable(Level.FINEST)) {
                                LOGGER.log(Level.FINEST, "Marker method parameters , tId={0}", currentThreadId); // NOI18N
                            }
                            
                            List parameters = new ArrayList();
                            int pars = buffer.get();
                            
                            for (int i = 0; i < pars; i++) {
                                parameters.add(readParameter(buffer));
                            }
                            methodParameters.put(Integer.valueOf(currentThreadId), parameters);
                            break;
                        }
                        case CommonConstants.RESET_COLLECTORS: {
                            if (LOGGER.isLoggable(Level.FINEST)) {
                                LOGGER.finest("Profiling data reset"); // NOI18N
                            }

                            fireReset();

                            break;
                        }
                        default: {
                            LOGGER.log(Level.SEVERE, "*** Profiler Engine: internal error: got unknown event type in CPUDataFrameProcessor: {0} at {1}", // NOI18N
                                                      new Object[]{(int) eventType, buffer.position()});
                            StringBuilder sb = new StringBuilder();
                            sb.append("dataframe [");   // NOI18N
                            buffer.rewind();
                            while (buffer.hasRemaining()) {
                                sb.append(buffer.get()).append(",");    // NOI18N
                            }
                            sb.append("]\n");   // NOI18N
                            LOGGER.severe(sb.toString());

                            break;
                        }
                    }
                }
            }
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            StringBuilder sb = new StringBuilder();
            sb.append("AIOOBE in dataframe [");
            buffer.rewind();
            while (buffer.hasRemaining()) {
                sb.append(buffer.get()).append(",");
            }
            sb.append("]\n");
            LOGGER.severe(sb.toString());
            throw aioobe;
        }
    }
    
    private Object readParameter(ByteBuffer buffer) {
        char type = (char) buffer.get();
        switch (type) {
            case BOOLEAN: {
                byte val = buffer.get();
                if (val == 0) {
                    return Boolean.FALSE;
                }
                if (val == 1) {
                    return Boolean.TRUE;
                }
                throw new IllegalArgumentException(Byte.toString(val));
            }
            case CHAR:
                return Character.valueOf(buffer.getChar());
            case BYTE:
                return Byte.valueOf(buffer.get());
            case SHORT:
                return Short.valueOf(buffer.getShort());
            case INT:
                return Integer.valueOf(buffer.getInt());
            case LONG:
                return Long.valueOf(buffer.getLong());
            case FLOAT:
                return Float.valueOf(Float.intBitsToFloat(buffer.getInt()));
            case DOUBLE:
                return Double.valueOf(Double.longBitsToDouble(buffer.getLong()));
            case REFERENCE:
                int lengthChars = buffer.getShort()/2;
                StringBuilder string = new StringBuilder(lengthChars);
                for (int i = 0; i < lengthChars; i++) {
                    string.append(buffer.getChar());
                }
                return string.toString();
        }
        throw new IllegalArgumentException(Integer.toString(type));
    }

    private void fireMethodEntry(final int methodId, final int threadId, final int methodType, final long timeStamp0,
                                 final long timeStamp1) {
        fireMethodEntry(methodId, threadId, methodType, timeStamp0, timeStamp1, null, null);
    }

    private void fireMethodEntry(final int methodId, final int threadId, final int methodType, final long timeStamp0,
                                 final long timeStamp1, final List parameters, final int[] methodIds) {
        foreachListener(new ListenerFunctor() {
                public void execute(ProfilingResultListener listener) {
                    ((CPUProfilingResultListener) listener).methodEntry(methodId, threadId, methodType, timeStamp0, timeStamp1, parameters, methodIds);
                }
            });
    }

    private void fireMethodEntryUnstamped(final int methodId, final int threadId, final int methodType, final List parameters, final int[] methodIds) {
        foreachListener(new ListenerFunctor() {
                public void execute(ProfilingResultListener listener) {
                    ((CPUProfilingResultListener) listener).methodEntryUnstamped(methodId, threadId, methodType, parameters, methodIds);
                }
            });
    }

    private void fireMethodExit(final int methodId, final int threadId, final int methodType, final long timeStamp0,
                                final long timeStamp1, final Object retVal) {
        foreachListener(new ListenerFunctor() {
                public void execute(ProfilingResultListener listener) {
                    ((CPUProfilingResultListener) listener).methodExit(methodId, threadId, methodType, timeStamp0, timeStamp1, retVal);
                }
            });
    }

    private void fireMethodExitUnstamped(final int methodId, final int threadId, final int methodType) {
        foreachListener(new ListenerFunctor() {
                public void execute(ProfilingResultListener listener) {
                    ((CPUProfilingResultListener) listener).methodExitUnstamped(methodId, threadId, methodType);
                }
            });
    }

    private void fireServletRequest(final int threadId, final int requestType, final String servletPath, final int sessionId) {
        foreachListener(new ListenerFunctor() {
                public void execute(ProfilingResultListener listener) {
                    ((CPUProfilingResultListener) listener).servletRequest(threadId, requestType, servletPath, sessionId);
                }
            });
    }

    private void fireSleepEntry(final int threadId, final long timeStamp0, final long timeStamp1) {
        foreachListener(new ListenerFunctor() {
                public void execute(ProfilingResultListener listener) {
                    ((CPUProfilingResultListener) listener).sleepEntry(threadId, timeStamp0, timeStamp1);
                }
            });
    }

    private void fireSleepExit(final int threadId, final long timeStamp0, final long timeStamp1) {
        foreachListener(new ListenerFunctor() {
                public void execute(ProfilingResultListener listener) {
                    ((CPUProfilingResultListener) listener).sleepExit(threadId, timeStamp0, timeStamp1);
                }
            });
    }

    private void fireThreadsResumed(final long timeStamp0, final long timeStamp1) {
        foreachListener(new ListenerFunctor() {
                public void execute(ProfilingResultListener listener) {
                    ((CPUProfilingResultListener) listener).threadsSuspend(timeStamp0, timeStamp1);
                }
            });
    }

    private void fireThreadsSuspend(final long timeStamp0, final long timeStamp1) {
        foreachListener(new ListenerFunctor() {
                public void execute(ProfilingResultListener listener) {
                    ((CPUProfilingResultListener) listener).threadsSuspend(timeStamp0, timeStamp1);
                }
            });
    }

    private void fireWaitEntry(final int threadId, final long timeStamp0, final long timeStamp1) {
        foreachListener(new ListenerFunctor() {
                public void execute(ProfilingResultListener listener) {
                    ((CPUProfilingResultListener) listener).waitEntry(threadId, timeStamp0, timeStamp1);
                }
            });
    }

    private void fireWaitExit(final int threadId, final long timeStamp0, final long timeStamp1) {
        foreachListener(new ListenerFunctor() {
                public void execute(ProfilingResultListener listener) {
                    ((CPUProfilingResultListener) listener).waitExit(threadId, timeStamp0, timeStamp1);
                }
            });
    }

    private void fireParkEntry(final int threadId, final long timeStamp0, final long timeStamp1) {
        foreachListener(new ListenerFunctor() {
                public void execute(ProfilingResultListener listener) {
                    ((CPUProfilingResultListener) listener).parkEntry(threadId, timeStamp0, timeStamp1);
                }
            });
    }

    private void fireParkExit(final int threadId, final long timeStamp0, final long timeStamp1) {
        foreachListener(new ListenerFunctor() {
                public void execute(ProfilingResultListener listener) {
                    ((CPUProfilingResultListener) listener).parkExit(threadId, timeStamp0, timeStamp1);
                }
            });
    }

    private static int getDepth(ByteBuffer buffer) {
        int depth = ((((int) buffer.get()) & 0xFF) << 16) | ((((int) buffer.get()) & 0xFF) << 8)
                    | (((int) buffer.get()) & 0xFF);
        return depth;
    }
}
