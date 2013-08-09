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

package org.netbeans.lib.profiler.results.cpu;

import java.nio.ByteBuffer;
import org.netbeans.lib.profiler.ProfilerClient;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.results.ProfilingResultListener;
import org.netbeans.lib.profiler.results.locks.AbstractLockDataFrameProcessor;
import java.util.logging.Level;


/**
 * This class main purpose is to parse a chunk of CPU related data received from
 * the JFluid server agent and dispatch the resulting events to all interested parties
 * @author Jaroslav Bachorik
 * @author Tomas Hurka
 */
public class CPUDataFrameProcessor extends AbstractLockDataFrameProcessor {

    private boolean hasMonitorInfo;

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void doProcessDataFrame(ByteBuffer buffer) {
        boolean collectingTwoTimeStamps = (client != null) ? client.getStatus().collectingTwoTimeStamps() : false;

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
                                                 CPUProfilingResultListener.METHODTYPE_NORMAL);
                    }

                    continue;
                }

                if (!((eventType == CommonConstants.BUFFEREVENT_PROFILEPOINT_HIT) || (eventType == CommonConstants.SERVLET_DO_METHOD)
                        || (eventType == CommonConstants.SET_FOLLOWING_EVENTS_THREAD) || (eventType == CommonConstants.NEW_THREAD)
                        || (eventType == CommonConstants.RESET_COLLECTORS) || (eventType == CommonConstants.NEW_MONITOR))) {
                    int methodId = -1;
                    long timeStamp0 = 0;
                    long timeStamp1 = 0;
                    int hash = -1;
                    
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
                        }
                    }

                    switch (eventType) {
                        case CommonConstants.MARKER_ENTRY_UNSTAMPED: {
                            if (LOGGER.isLoggable(Level.FINEST)) {
                                LOGGER.log(Level.FINEST, "Marker entry unstamped, tId={0}, mId={1}", new Object[]{currentThreadId, methodId}); // NOI18N
                            }

                            fireMethodEntryUnstamped(methodId, currentThreadId, CPUProfilingResultListener.METHODTYPE_MARKER);

                            break;
                        }
                        case CommonConstants.METHOD_ENTRY_UNSTAMPED: {
                            if (LOGGER.isLoggable(Level.FINEST)) {
                                LOGGER.log(Level.FINEST, "Method entry unstamped, tId={0}, mId={1}", new Object[]{currentThreadId, methodId}); // NOI18N
                            }

                            fireMethodEntryUnstamped(methodId, currentThreadId, CPUProfilingResultListener.METHODTYPE_NORMAL);

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
                            if (LOGGER.isLoggable(Level.FINEST)) {
                                LOGGER.log(Level.FINEST, "Marker entry , tId={0}, mId={1}", new Object[]{currentThreadId, methodId}); // NOI18N
                            }

                            fireMethodEntry(methodId, currentThreadId, CPUProfilingResultListener.METHODTYPE_MARKER, timeStamp0,
                                            timeStamp1);

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
                            if (LOGGER.isLoggable(Level.FINEST)) {
                                LOGGER.log(Level.FINEST, "Marker exit , tId={0}, mId={1}", new Object[]{currentThreadId, methodId}); // NOI18N
                            }

                            fireMethodExit(methodId, currentThreadId, CPUProfilingResultListener.METHODTYPE_MARKER, timeStamp0,
                                           timeStamp1);

                            break;
                        }
                        case CommonConstants.ROOT_EXIT: {
                            if (LOGGER.isLoggable(Level.FINEST)) {
                                LOGGER.log(Level.FINEST, "Root exit , tId={0}, mId={1}", new Object[]{currentThreadId, methodId}); // NOI18N
                            }

                            fireMethodExit(methodId, currentThreadId, CPUProfilingResultListener.METHODTYPE_ROOT, timeStamp0,
                                           timeStamp1);

                            break;
                        }
                        case CommonConstants.METHOD_EXIT: {
                            if (LOGGER.isLoggable(Level.FINEST)) {
                                LOGGER.log(Level.FINEST, "Method exit , tId={0}, mId={1}", new Object[]{currentThreadId, methodId}); // NOI18N
                            }

                            fireMethodExit(methodId, currentThreadId, CPUProfilingResultListener.METHODTYPE_NORMAL, timeStamp0,
                                           timeStamp1);

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
                                LOGGER.log(Level.FINEST, "Monitor entry , tId={0} , monitorId={1}", new Object[]{currentThreadId,hash}); // NOI18N
                            }

                            fireMonitorEntry(currentThreadId, timeStamp0, timeStamp1, hash);

                            break;
                        }
                        case CommonConstants.METHOD_EXIT_MONITOR: {
                            if (LOGGER.isLoggable(Level.FINEST)) {
                                LOGGER.log(Level.FINEST, "Monitor exit , tId={0} , monitorId={1}", new Object[]{currentThreadId,hash}); // NOI18N
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
                                LOGGER.log(Level.FINEST, "Creating new monitor , mId={0} , className={1}", new Object[] {hash, className}); // NOI18N
                            }
                            hasMonitorInfo = true;
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
                        case CommonConstants.RESET_COLLECTORS: {
                            if (LOGGER.isLoggable(Level.FINEST)) {
                                LOGGER.finest("Profiling data reset"); // NOI18N
                            }

                            fireReset();

                            break;
                        }
                        default: {
                            LOGGER.log(Level.SEVERE, "*** Profiler Engine: internal error: got unknown event type in CallGraphBuilder: {0} at {1}", // NOI18N
                                                      new Object[]{(int) eventType, buffer.position()});

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

    public void startup(ProfilerClient client) {
        super.startup(client);
        hasMonitorInfo = false;
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
}
