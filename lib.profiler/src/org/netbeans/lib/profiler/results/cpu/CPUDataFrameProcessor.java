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

package org.netbeans.lib.profiler.results.cpu;

import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.results.AbstractDataFrameProcessor;
import org.netbeans.lib.profiler.results.ProfilingResultListener;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;


/**
 * This class' main purpose is to parse a chunk of CPU related data received from
 * the JFluid server agent and dispatch the resulting events to all interested parties
 * @author Jaroslav Bachorik
 */
public class CPUDataFrameProcessor extends AbstractDataFrameProcessor {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private volatile int currentThreadId = -1;

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void doProcessDataFrame(byte[] buffer) {
        int position = 0;
        boolean collectingTwoTimeStamps = (client != null) ? client.getStatus().collectingTwoTimeStamps() : false;

        while (position < buffer.length) {
            byte eventType = buffer[position++];

            if ((eventType & CommonConstants.COMPACT_EVENT_FORMAT_BYTE_MASK) != 0) {
                char charEvent = (char) ((((int) eventType & 0xFF) << 8) | ((int) buffer[position++] & 0xFF));

                if ((byte) (eventType & CommonConstants.METHOD_EXIT_COMPACT_BYTE_MASK) == CommonConstants.METHOD_EXIT_COMPACT_BYTE_MASK) {
                    fireMethodExitUnstamped(charEvent & CommonConstants.COMPACT_EVENT_METHOD_ID_MASK, currentThreadId,
                                            CPUProfilingResultListener.METHODTYPE_NORMAL);
                } else {
                    fireMethodEntryUnstamped(charEvent & CommonConstants.COMPACT_EVENT_METHOD_ID_MASK, currentThreadId,
                                             CPUProfilingResultListener.METHODTYPE_NORMAL);
                }

                continue;
            }

            if (!((eventType == CommonConstants.BUFFEREVENT_PROFILEPOINT_HIT) || (eventType == CommonConstants.SERVLET_DO_METHOD)
                    || (eventType == CommonConstants.SET_FOLLOWING_EVENTS_THREAD) || (eventType == CommonConstants.NEW_THREAD)
                    || (eventType == CommonConstants.RESET_COLLECTORS))) {
                int methodId = -1;
                long timeStamp0 = 0;
                long timeStamp1 = 0;

                if ((eventType != CommonConstants.ADJUST_TIME // those events do not carry methodId
                    ) && (eventType != CommonConstants.METHOD_ENTRY_WAIT) && (eventType != CommonConstants.METHOD_EXIT_WAIT)
                        && (eventType != CommonConstants.METHOD_ENTRY_MONITOR)
                        && (eventType != CommonConstants.METHOD_EXIT_MONITOR)
                        && (eventType != CommonConstants.METHOD_ENTRY_SLEEP) && (eventType != CommonConstants.METHOD_EXIT_SLEEP)) {
                    methodId = (((int) buffer[position++] & 0xFF) << 8) | ((int) buffer[position++] & 0xFF);
                }

                if ((eventType != CommonConstants.METHOD_ENTRY_UNSTAMPED) && (eventType != CommonConstants.METHOD_EXIT_UNSTAMPED)
                        && (eventType != CommonConstants.MARKER_ENTRY_UNSTAMPED)
                        && (eventType != CommonConstants.MARKER_EXIT_UNSTAMPED)) {
                    timeStamp0 = (((long) buffer[position++] & 0xFF) << 48) | (((long) buffer[position++] & 0xFF) << 40)
                                 | (((long) buffer[position++] & 0xFF) << 32) | (((long) buffer[position++] & 0xFF) << 24)
                                 | (((long) buffer[position++] & 0xFF) << 16) | (((long) buffer[position++] & 0xFF) << 8)
                                 | ((long) buffer[position++] & 0xFF);

                    if ((eventType != CommonConstants.METHOD_ENTRY_WAIT) && (eventType != CommonConstants.METHOD_EXIT_WAIT)
                            && (eventType != CommonConstants.METHOD_ENTRY_MONITOR)
                            && (eventType != CommonConstants.METHOD_EXIT_MONITOR)
                            && (eventType != CommonConstants.METHOD_ENTRY_SLEEP)
                            && (eventType != CommonConstants.METHOD_EXIT_SLEEP)) {
                        if (collectingTwoTimeStamps || (eventType < CommonConstants.TWO_TIMESTAMP_EVENTS)) {
                            timeStamp1 = (((long) buffer[position++] & 0xFF) << 48) | (((long) buffer[position++] & 0xFF) << 40)
                                         | (((long) buffer[position++] & 0xFF) << 32)
                                         | (((long) buffer[position++] & 0xFF) << 24)
                                         | (((long) buffer[position++] & 0xFF) << 16) | (((long) buffer[position++] & 0xFF) << 8)
                                         | ((long) buffer[position++] & 0xFF);
                        }
                    }
                }

                switch (eventType) {
                    case CommonConstants.MARKER_ENTRY_UNSTAMPED: {
                        if (LOGGER.isLoggable(Level.FINEST)) {
                            LOGGER.finest("Marker entry unstamped, tId=" + currentThreadId + ", mId=" + methodId); // NOI18N
                        }

                        fireMethodEntryUnstamped(methodId, currentThreadId, CPUProfilingResultListener.METHODTYPE_MARKER);

                        break;
                    }
                    case CommonConstants.METHOD_ENTRY_UNSTAMPED: {
                        if (LOGGER.isLoggable(Level.FINEST)) {
                            LOGGER.finest("Method entry unstamped, tId=" + currentThreadId + ", mId=" + methodId); // NOI18N
                        }

                        fireMethodEntryUnstamped(methodId, currentThreadId, CPUProfilingResultListener.METHODTYPE_NORMAL);

                        break;
                    }
                    case CommonConstants.MARKER_EXIT_UNSTAMPED: {
                        if (LOGGER.isLoggable(Level.FINEST)) {
                            LOGGER.finest("Marker exit unstamped, tId=" + currentThreadId + ", mId=" + methodId); // NOI18N
                        }

                        fireMethodExitUnstamped(methodId, currentThreadId, CPUProfilingResultListener.METHODTYPE_MARKER);

                        break;
                    }
                    case CommonConstants.METHOD_EXIT_UNSTAMPED: {
                        if (LOGGER.isLoggable(Level.FINEST)) {
                            LOGGER.finest("Method exit unstamped, tId=" + currentThreadId + ", mId=" + methodId); // NOI18N
                        }

                        fireMethodExitUnstamped(methodId, currentThreadId, CPUProfilingResultListener.METHODTYPE_NORMAL);

                        break;
                    }
                    case CommonConstants.MARKER_ENTRY: {
                        if (LOGGER.isLoggable(Level.FINEST)) {
                            LOGGER.finest("Marker entry , tId=" + currentThreadId + ", mId=" + methodId); // NOI18N
                        }

                        fireMethodEntry(methodId, currentThreadId, CPUProfilingResultListener.METHODTYPE_MARKER, timeStamp0,
                                        timeStamp1);

                        break;
                    }
                    case CommonConstants.ROOT_ENTRY: {
                        if (LOGGER.isLoggable(Level.FINEST)) {
                            LOGGER.finest("Root entry , tId=" + currentThreadId + ", mId=" + methodId); // NOI18N
                        }

                        fireMethodEntry(methodId, currentThreadId, CPUProfilingResultListener.METHODTYPE_ROOT, timeStamp0,
                                        timeStamp1);

                        break;
                    }
                    case CommonConstants.METHOD_ENTRY: {
                        if (LOGGER.isLoggable(Level.FINEST)) {
                            LOGGER.finest("Method entry , tId=" + currentThreadId + ", mId=" + methodId); // NOI18N
                        }

                        fireMethodEntry(methodId, currentThreadId, CPUProfilingResultListener.METHODTYPE_NORMAL, timeStamp0,
                                        timeStamp1);

                        break;
                    }
                    case CommonConstants.MARKER_EXIT: {
                        if (LOGGER.isLoggable(Level.FINEST)) {
                            LOGGER.finest("Marker exit , tId=" + currentThreadId + ", mId=" + methodId); // NOI18N
                        }

                        fireMethodExit(methodId, currentThreadId, CPUProfilingResultListener.METHODTYPE_MARKER, timeStamp0,
                                       timeStamp1);

                        break;
                    }
                    case CommonConstants.ROOT_EXIT: {
                        if (LOGGER.isLoggable(Level.FINEST)) {
                            LOGGER.finest("Root exit , tId=" + currentThreadId + ", mId=" + methodId); // NOI18N
                        }

                        fireMethodExit(methodId, currentThreadId, CPUProfilingResultListener.METHODTYPE_ROOT, timeStamp0,
                                       timeStamp1);

                        break;
                    }
                    case CommonConstants.METHOD_EXIT: {
                        if (LOGGER.isLoggable(Level.FINEST)) {
                            LOGGER.finest("Method exit , tId=" + currentThreadId + ", mId=" + methodId); // NOI18N
                        }

                        fireMethodExit(methodId, currentThreadId, CPUProfilingResultListener.METHODTYPE_NORMAL, timeStamp0,
                                       timeStamp1);

                        break;
                    }
                    case CommonConstants.ADJUST_TIME: {
                        if (LOGGER.isLoggable(Level.FINEST)) {
                            LOGGER.finest("Adjust time , tId=" + currentThreadId); // NOI18N
                        }

                        fireAdjustTime(currentThreadId, timeStamp0, timeStamp1);

                        break;
                    }
                    case CommonConstants.METHOD_ENTRY_MONITOR: {
                        if (LOGGER.isLoggable(Level.FINEST)) {
                            LOGGER.finest("Monitor entry , tId=" + currentThreadId); // NOI18N
                        }

                        fireMonitorEntry(currentThreadId, timeStamp0, timeStamp1);

                        break;
                    }
                    case CommonConstants.METHOD_EXIT_MONITOR: {
                        if (LOGGER.isLoggable(Level.FINEST)) {
                            LOGGER.finest("Monitor exit , tId=" + currentThreadId); // NOI18N
                        }

                        fireMonitorExit(currentThreadId, timeStamp0, timeStamp1);

                        break;
                    }
                    case CommonConstants.METHOD_ENTRY_SLEEP: {
                        if (LOGGER.isLoggable(Level.FINEST)) {
                            LOGGER.finest("Sleep entry , tId=" + currentThreadId); // NOI18N
                        }

                        fireSleepEntry(currentThreadId, timeStamp0, timeStamp1);

                        break;
                    }
                    case CommonConstants.METHOD_EXIT_SLEEP: {
                        if (LOGGER.isLoggable(Level.FINEST)) {
                            LOGGER.finest("Sleep exit , tId=" + currentThreadId); // NOI18N
                        }

                        fireSleepExit(currentThreadId, timeStamp0, timeStamp1);

                        break;
                    }
                    case CommonConstants.METHOD_ENTRY_WAIT: {
                        if (LOGGER.isLoggable(Level.FINEST)) {
                            LOGGER.finest("Wait entry , tId=" + currentThreadId); // NOI18N
                        }

                        fireWaitEntry(currentThreadId, timeStamp0, timeStamp1);

                        break;
                    }
                    case CommonConstants.METHOD_EXIT_WAIT: {
                        if (LOGGER.isLoggable(Level.FINEST)) {
                            LOGGER.finest("Wait exit , tId=" + currentThreadId); // NOI18N
                        }

                        fireWaitExit(currentThreadId, timeStamp0, timeStamp1);

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
                        LOGGER.severe("*** Profiler Engine: internal error: got unknown event type in CPUDataFrameProcessor: "
                                      + (int) eventType // NOI18N
                                      + " at " + position // NOI18N
                                      );

                        break; // NOI18N
                    }
                }
            } else {
                switch (eventType) {
                    case CommonConstants.BUFFEREVENT_PROFILEPOINT_HIT: {
                        int id = (((int) buffer[position++] & 0xFF) << 8) | ((int) buffer[position++] & 0xFF);
                        long timeStamp = (((long) buffer[position++] & 0xFF) << 48) | (((long) buffer[position++] & 0xFF) << 40)
                                         | (((long) buffer[position++] & 0xFF) << 32)
                                         | (((long) buffer[position++] & 0xFF) << 24)
                                         | (((long) buffer[position++] & 0xFF) << 16) | (((long) buffer[position++] & 0xFF) << 8)
                                         | ((long) buffer[position++] & 0xFF);
                        int threadId = (((int) buffer[position++] & 0xFF) << 8) | ((int) buffer[position++] & 0xFF);
                        fireProfilingPoint(threadId, id, timeStamp);

                        break;
                    }
                    case CommonConstants.SET_FOLLOWING_EVENTS_THREAD: {
                        if (LOGGER.isLoggable(Level.FINEST)) {
                            LOGGER.finest("Change current thread , tId=" + currentThreadId); // NOI18N
                        }

                        currentThreadId = (char) ((((int) buffer[position++] & 0xFF) << 8) | ((int) buffer[position++] & 0xFF));

                        break;
                    }
                    case CommonConstants.NEW_THREAD: {
                        int threadId = (char) ((((int) buffer[position++] & 0xFF) << 8) | ((int) buffer[position++] & 0xFF));
                        int strLen = ((((int) buffer[position++] & 0xFF) << 8) | ((int) buffer[position++] & 0xFF));
                        String threadName = new String(buffer, position, strLen);
                        position += strLen;
                        strLen = ((((int) buffer[position++] & 0xFF) << 8) | ((int) buffer[position++] & 0xFF));

                        String threadClassName = new String(buffer, position, strLen);
                        position += strLen;

                        if (LOGGER.isLoggable(Level.FINEST)) {
                            LOGGER.finest("Creating new thread , tId=" + threadId); // NOI18N
                        }

                        fireNewThread(threadId, threadName, threadClassName);
                        currentThreadId = threadId;

                        break;
                    }
                    case CommonConstants.SERVLET_DO_METHOD: {
                        if (LOGGER.isLoggable(Level.FINEST)) {
                            LOGGER.finest("Servlet track start , tId=" + currentThreadId); // NOI18N
                        }

                        byte requestType = buffer[position++];
                        int strLen = ((((int) buffer[position++] & 0xFF) << 8) | ((int) buffer[position++] & 0xFF));
                        String servletPath = new String(buffer, position, strLen);
                        position += strLen;

                        int sessionId = (((int) buffer[position++] & 0xFF) << 24) | (((int) buffer[position++] & 0xFF) << 16)
                                        | (((int) buffer[position++] & 0xFF) << 8) | ((int) buffer[position++] & 0xFF);
                        fireServletRequest(currentThreadId, requestType, servletPath, sessionId);

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
                        LOGGER.severe("*** Profiler Engine: internal error: got unknown event type in CallGraphBuilder: "
                                      + (int) eventType // NOI18N
                                      + " at " + position // NOI18N
                                      );

                        break; // NOI18N
                    }
                }
            }
        }
    }

    private void fireAdjustTime(final int threadId, final long timeStamp0, final long timeStamp1) {
        foreachListener(new ListenerFunctor() {
                public void execute(ProfilingResultListener listener) {
                    ((CPUProfilingResultListener) listener).timeAdjust(threadId, timeStamp0, timeStamp1);
                }
            });
    }

    private void fireMethodEntry(final int methodId, final int threadId, final int methodType, final long timeStamp0,
                                 final long timeStamp1) {
        foreachListener(new ListenerFunctor() {
                public void execute(ProfilingResultListener listener) {
                    ((CPUProfilingResultListener) listener).methodEntry(methodId, threadId, methodType, timeStamp0, timeStamp1);
                }
            });
    }

    private void fireMethodEntryUnstamped(final int methodId, final int threadId, final int methodType) {
        foreachListener(new ListenerFunctor() {
                public void execute(ProfilingResultListener listener) {
                    ((CPUProfilingResultListener) listener).methodEntryUnstamped(methodId, threadId, methodType);
                }
            });
    }

    private void fireMethodExit(final int methodId, final int threadId, final int methodType, final long timeStamp0,
                                final long timeStamp1) {
        foreachListener(new ListenerFunctor() {
                public void execute(ProfilingResultListener listener) {
                    ((CPUProfilingResultListener) listener).methodExit(methodId, threadId, methodType, timeStamp0, timeStamp1);
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

    private void fireMonitorEntry(final int threadId, final long timeStamp0, final long timeStamp1) {
        foreachListener(new ListenerFunctor() {
                public void execute(ProfilingResultListener listener) {
                    ((CPUProfilingResultListener) listener).monitorEntry(threadId, timeStamp0, timeStamp1);
                }
            });
    }

    private void fireMonitorExit(final int threadId, final long timeStamp0, final long timeStamp1) {
        foreachListener(new ListenerFunctor() {
                public void execute(ProfilingResultListener listener) {
                    ((CPUProfilingResultListener) listener).monitorExit(threadId, timeStamp0, timeStamp1);
                }
            });
    }

    private void fireNewThread(final int threadId, final String threadName, final String threadClassName) {
        foreachListener(new ListenerFunctor() {
                public void execute(ProfilingResultListener listener) {
                    ((CPUProfilingResultListener) listener).newThread(threadId, threadName, threadClassName);
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
}
