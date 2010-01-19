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
import org.netbeans.lib.profiler.server.system.Threads;
import org.netbeans.lib.profiler.server.system.Timers;


/**
 * This class contains the actual methods for sampled instrumentation recursive CPU profiling, calls to which are injected
 * into the target application (TA) bytecodes when they are instrumented.
 *
 * methodEntry: if taking timestamp (in new sampling slot), time is charged to method being left
 * methodExit: if taking timestamp (in new sampling slot), time is charged to method being exited
 *
 * @author Tomas Hurka
 * @author Misha Dmitriev
 */
public class ProfilerRuntimeCPUSampledInstr extends ProfilerRuntimeCPU {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    //------------------------------------------- Support classes --------------------------------------------------

    /** A thread that periodically sets the sample flag for worker threads */
    static class SamplingThread extends Thread {
        //~ Static fields/initializers -------------------------------------------------------------------------------------------

        private static final boolean isSolaris = Platform.isSolaris();
        private static final boolean isLinux = Platform.isLinux();
        private static final boolean isUnix = isSolaris || isLinux;
        private static final int VIOLATION_THRESHOLD = 10;
        private static final boolean DEBUG = false;

        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private volatile boolean terminated;
        private int count;

        //~ Methods --------------------------------------------------------------------------------------------------------------

        SamplingThread() {
            Threads.recordAdditionalProfilerOwnThread(this);
            setPriority(Thread.MAX_PRIORITY);
            setDaemon(true);
            setName(PROFILER_SPECIAL_EXEC_THREAD_NAME + " 9"); // NOI18N
        }

        public void run() {
            if (isSolaris) {
                samplingInterval *= 1000000; // Convert into nanos - the Solaris hires timer resolution
            } else if (isLinux) {
                samplingInterval *= 1000; // Convert into microseconds - the Linux hires timer resolution
            }

            int adjustedSamplingInterval = samplingInterval;
            int upperBound = (samplingInterval * 5) / 4;
            int lowerBound = samplingInterval / 10;
            int violationCount = VIOLATION_THRESHOLD;

            long startTime = Timers.getCurrentTimeInCounts();

            while (!terminated) {
                if (!isUnix) {
                    try {
                        Thread.sleep(samplingInterval);
                    } catch (InterruptedException ex) { /* Should not happen */
                    }
                } else { // Solaris and Linux

                    long time = Timers.getCurrentTimeInCounts();
                    // On Solaris, the resolution of Thread.sleep(), which boils down to the select(3C) system call, seems to be
                    // around 20 ms. So we have to use our own call, which eventually calls nanosleep() and takes an argument in nanos.
                    // On Linux (at least version 7.3 + patches, which I tried), nanosleep() seems to have a 20 ms resolution (or even
                    // give 20 ms no matter what?), which is a documented bug (see 'man nanosleep'). Well, maybe it improves in future...
                    Timers.osSleep(adjustedSamplingInterval);
                    time = Timers.getCurrentTimeInCounts() - time;

                    if ((time > upperBound) && (adjustedSamplingInterval > lowerBound)) {
                        if (violationCount > 0) {
                            violationCount--;
                        } else {
                            adjustedSamplingInterval = (adjustedSamplingInterval * 95) / 100;
                            violationCount = VIOLATION_THRESHOLD;
                        }
                    }
                }

                ThreadInfo.setSampleDueForAllThreads();

                if (DEBUG) {
                    count++;
                }
            }

            if (DEBUG && isUnix) {
                long time = ((Timers.getCurrentTimeInCounts() - startTime) * 1000) / Timers.getNoOfCountsInSecond();
                System.out.println("JFluid sampling thread: elapsed time: " + time + " ms, avg interval: "
                                   + (((double) time) / count) + "ms, adjusted interval: " + adjustedSamplingInterval
                                   + " OS units"); // NOI18N
            }
        }

        public void terminate() {
            terminated = true;

            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) { /* Should not happen */
            }
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    protected static int samplingInterval = 10;
    protected static SamplingThread st;

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static void setSamplingInterval(int v) {
        samplingInterval = v;
    }

    public static void enableProfiling(boolean v) {
        if (v) {
            createNewDataStructures();
            ProfilerRuntimeCPU.enableProfiling(v);
        } else {
            ProfilerRuntimeCPU.enableProfiling(v);
            clearDataStructures();
        }
    }

    // ---------------------------------- Profile Data Acquisition --------------------------------------
    /** Called upon entry into a special root method used for */
    public static void markerMethodEntry(char methodId) {
        if (recursiveInstrumentationDisabled) {
            return; // See the comment at the recursiveInstrumentationDisabled variable declaration
        }

        ThreadInfo ti = ThreadInfo.getThreadInfo();

        if (ti.inProfilingRuntimeMethod > 0) {
            return;
        }

        //if (instrMethodClasses != null && methodId < instrMethodClasses.length) System.out.println("++++++Marker methodEntry for " + instrMethodClasses[methodId] + "." + instrMethodNames[methodId] + ", thread = " + Thread.currentThread());
        //else System.out.println("++++++Marker methodEntry for methodId = " + (int)methodId + ", thread = " + Thread.currentThread());
        if (!ti.isInitialized()) {
            if ((nProfiledThreadsAllowed > 0) && !ThreadInfo.isCurrentThreadProfilerServerThread()) {
                ti.initialize();
                ti.useEventBuffer();

                synchronized (eventBuffer) { // Make this happen atomically wrt. other operations on eventBuffer, such as reset collectors
                    nProfiledThreadsAllowed--;
                    ti.inProfilingRuntimeMethod++;
                    ti.inCallGraph = true;
                    writeThreadCreationEvent(ti);
                }
            } else {
                return;
            }
        } else {
            ti.inProfilingRuntimeMethod++;
            ti.inCallGraph = true;
        }

        // when methodId > 64K/2 is passed here using our instrumentation's sipush command at the call site, 
        // it's treated here as a signed integer. Thus without
        // the below fix we can get e.g. an ArrayIndexOutOfBoundsException(-32768) when methodId == 32768 (***)
        int methodIdInt = methodId&0xff;
        methodIdInt |= methodId&0xff00;
            
        if (!instrMethodInvoked[methodIdInt]) {
            if (ti.rootMethodStackDepth > 0) { // marker method under root method - perform instrumentation of nearest callees
                long absTimeStamp = Timers.getCurrentTimeInCounts();
                long threadTimeStamp = Timers.getThreadCPUTimeInNanos();
                externalActionsHandler.handleFirstTimeMethodInvoke(methodId);
                instrMethodInvoked[methodIdInt] = true; // Mark this method as invoked
                writeAdjustTimeEvent(ti, absTimeStamp, threadTimeStamp);
            } else { // DO NOT perform instrumentation of its immediate callees
                instrMethodInvoked[methodIdInt] = true;
            }
        }

        ti.stackDepth++; //= 1;  // This is the logical stack depth

        if (ti.stackDepth > 1) {
            if (!ti.sampleDue) {
                writeUnstampedEvent(MARKER_ENTRY_UNSTAMPED, ti, methodId);
            } else {
                writeTimeStampedEvent(MARKER_ENTRY, ti, methodId);
                ti.sampleDue = false;
            }
        } else {
            writeTimeStampedEvent(MARKER_ENTRY, ti, methodId);
        }

        ti.inProfilingRuntimeMethod--;
    }

    /** Called upon exit from the marker method. */
    public static void markerMethodExit(char methodId) {
        if (recursiveInstrumentationDisabled) {
            return;
        }

        ThreadInfo ti = ThreadInfo.getThreadInfo();

        if (ti.isInitialized() && ti.inCallGraph) { // ti == null may happen if instrumentation has been removed or data collectors reset

            if (ti.inProfilingRuntimeMethod > 0) {
                return;
            }

            ti.inProfilingRuntimeMethod++;

            //System.out.println("------markerMethodExit for " + instrMethodClasses[methodId] + "." + instrMethodNames[methodId] + ", depth = " + ti.stackDepth + ", id = " + (int) methodId);
            ti.stackDepth--;

            if (ti.stackDepth < 1) {
                ti.inCallGraph = false; // We are exiting the marker method of our call subgraph
                writeTimeStampedEvent(MARKER_EXIT, ti, methodId);
            } else {
                if (!ti.sampleDue) {
                    writeUnstampedEvent(MARKER_EXIT_UNSTAMPED, ti, methodId);
                } else {
                    writeTimeStampedEvent(MARKER_EXIT, ti, methodId);
                    ti.sampleDue = false;
                }
            }

            ti.inProfilingRuntimeMethod--;
        }
    }

    /** Called upon entry into a non-root target application method */
    public static void methodEntry(char methodId) {
        if (recursiveInstrumentationDisabled) {
            return; // See the comment at the recursiveInstrumentationDisabled variable declaration
        }

        ThreadInfo ti = ThreadInfo.getThreadInfo();

        if (ti.isInitialized() && ti.inCallGraph && (ti.rootMethodStackDepth > 0)) {
            if (ti.inProfilingRuntimeMethod > 0) {
                return;
            }

            ti.inProfilingRuntimeMethod++;
            //System.out.println("++++++methodEntry, depth = " + ti.stackDepth + ", id = " + (int) methodId);

            // See comment marked with (***)
            int methodIdInt = methodId&0xff;
            methodIdInt |= methodId&0xff00;
            
            // Now check if it's the first invocation of this method, and if so, perform instrumentation of nearest callees
            if (!instrMethodInvoked[methodIdInt]) {
                long absTimeStamp = Timers.getCurrentTimeInCounts();
                long threadTimeStamp = Timers.getThreadCPUTimeInNanos();
                externalActionsHandler.handleFirstTimeMethodInvoke(methodId);
                instrMethodInvoked[methodIdInt] = true; // Mark this method as invoked
                writeAdjustTimeEvent(ti, absTimeStamp, threadTimeStamp);
            }

            ti.stackDepth++;

            if (!ti.sampleDue) {
                if (methodId <= MAX_METHOD_ID_FOR_COMPACT_FORMAT) {
                    writeCompactEvent(ti, (char) (METHOD_ENTRY_COMPACT_MASK | methodId));
                } else {
                    writeUnstampedEvent(METHOD_ENTRY_UNSTAMPED, ti, methodId);
                }
            } else {
                writeTimeStampedEvent(METHOD_ENTRY, ti, methodId);
                ti.sampleDue = false;
            }

            ti.inProfilingRuntimeMethod--;
        }
    }

    /** Called upon exit from the method. */
    public static void methodExit(char methodId) {
        if (recursiveInstrumentationDisabled) {
            return; // See the comment at the recursiveInstrumentationDisabled variable declaration
        }

        ThreadInfo ti = ThreadInfo.getThreadInfo();

        if (ti.isInitialized() && ti.inCallGraph && (ti.rootMethodStackDepth > 0)) { // ti == null may happen if instrumentation has been removed or data collectors reset

            if (ti.inProfilingRuntimeMethod > 0) {
                return;
            }

            ti.inProfilingRuntimeMethod++;

            //System.out.println("------methodExit, depth = " + ti.stackDepth + ", id = " + (int) methodId);
            if (ti.rootMethodStackDepth == ti.stackDepth) {
                ti.rootMethodStackDepth = 0;
            }

            ti.stackDepth--;

            if (ti.stackDepth < 1) {
                ti.inCallGraph = false; // We are exiting the root method of our call subgraph
                writeTimeStampedEvent(ROOT_EXIT, ti, methodId);
            } else if (ti.rootMethodStackDepth == 0) { // We are exiting the root method, which was under marker method
                writeTimeStampedEvent(ROOT_EXIT, ti, methodId);
            } else {
                if (!ti.sampleDue) {
                    // short path: not taking time stamp

                    // See comment marked with (***)
                    methodId = (char) ((int) methodId);

                    if (methodId <= MAX_METHOD_ID_FOR_COMPACT_FORMAT) {
                        writeCompactEvent(ti, (char) (METHOD_EXIT_COMPACT_MASK | methodId));
                    } else {
                        writeUnstampedEvent(METHOD_EXIT_UNSTAMPED, ti, methodId);
                    }
                } else {
                    writeTimeStampedEvent(METHOD_EXIT, ti, methodId);
                    ti.sampleDue = false;
                }
            }

            ti.inProfilingRuntimeMethod--;
        }
    }

    /** Called upon entry into a root target application method */
    public static void rootMethodEntry(char methodId) {
        if (recursiveInstrumentationDisabled) {
            return; // See the comment at the recursiveInstrumentationDisabled variable declaration
        }

        ThreadInfo ti = ThreadInfo.getThreadInfo();

        if (ti.inProfilingRuntimeMethod > 0) {
            return;
        }

        ProfilerServer.notifyClientOnResultsAvailability();

        if (ti.isInitialized() && !ti.inCallGraph && (ti.stackDepth > 0)) {
            ti.inCallGraph = true;
            methodEntry(methodId);
            ti.inCallGraph = false;

            return;
        }

        if (ti.isInitialized() && ti.inCallGraph && (ti.rootMethodStackDepth > 0)) {
            methodEntry(methodId);
        } else { // Entered the root method from outside this call subgraph
                 //if (instrMethodClasses != null && methodId < instrMethodClasses.length) System.out.println("++++++Root methodEntry for " + instrMethodClasses[methodId] + "." + instrMethodNames[methodId] + ", thread = " + Thread.currentThread());
                 //else System.out.println("++++++Root methodEntry for methodId = " + (int)methodId + ", thread = " + Thread.currentThread());

            if (!ti.isInitialized()) {
                if ((nProfiledThreadsAllowed > 0) && !ThreadInfo.isCurrentThreadProfilerServerThread()) {
                    ti.initialize();
                    ti.useEventBuffer();

                    synchronized (eventBuffer) { // Make this happen atomically wrt. other operations on eventBuffer, such as reset collectors
                        nProfiledThreadsAllowed--;
                        ti.inProfilingRuntimeMethod++;

                        if (!ProfilerServer.startProfilingPointsActive()) {
                            ti.inCallGraph = true;
                        }

                        writeThreadCreationEvent(ti);
                    }
                } else {
                    return;
                }
            } else {
                ti.inProfilingRuntimeMethod++;

                if (ti.stackDepth == 0 && !ProfilerServer.startProfilingPointsActive()) {
                    ti.inCallGraph = true;
                }
            }

            // See comment marked with (***)
            int methodIdInt = methodId&0xff;
            methodIdInt |= methodId&0xff00;
            
            // Check if it's the first invocation of this method, and if so, perform instrumentation of its immediate callees
            if (!instrMethodInvoked[methodIdInt]) {
                externalActionsHandler.handleFirstTimeMethodInvoke(methodId);
                instrMethodInvoked[methodIdInt] = true;
            }

            ti.stackDepth++; //= 1;  // This is the logical stack depth
            writeTimeStampedEvent(ROOT_ENTRY, ti, methodId);
            ti.rootMethodStackDepth = ti.stackDepth;
            ti.inProfilingRuntimeMethod--;
        }
    }

    protected static void clearDataStructures() {
        ProfilerRuntimeCPU.clearDataStructures();

        if (st != null) {
            st.terminate();
        }
    }

    protected static void createNewDataStructures() {
        ProfilerRuntimeCPU.createNewDataStructures();
        st = new SamplingThread();
        st.start();
    }

    // ---------------------------------- Writing profiler events --------------------------------------  

    // In order to optimize usage of the event buffer, we exploit the facts that:
    // (1) We have just a handful of different events, and thus their normal codes are small numbers, that need a few bits.
    // (2) We rarely instrument more than a few thousand methods, so out of 16 bits of char methodId the upper two are usually unused
    // (3) Just two events, method entry and method exit, happen ~3 orders of magnitude more often than others.
    // (4) When performing sampled instrumentation profiling, most of method entry/exit events don't have a timestamp.
    // Given all these observations, we can encode unstamped method entry/exit events with method id <= MAX_METHOD_ID_FOR_COMPACT_FORMAT
    // (equal to 0x3FFF, i.e. not using the upper two bits) as just a single char. The uppermost bit determines if the char
    // corresponds to a full event stored in the compact format, or to just an event code. The second bit from the top determines
    // the actual event - method entry or method exit. Subsequent bits are the method id.

    /** Write a two-byte event, such as unstamped method entry/exit in compact format. */
    static void writeCompactEvent(ThreadInfo ti, char event) {
        // if (printEvents) System.out.println("*** Writing compact event " + (int) event);
        ti.evBuf[ti.evBufPos++] = (byte) ((event >> 8) & 0xFF);
        ti.evBuf[ti.evBufPos++] = (byte) ((event) & 0xFF);

        if (ti.evBufPos > ThreadInfo.evBufPosThreshold) {
            copyLocalBuffer(ti);
        }
    }

    /** Write an unstamped event, such as method entry/exit for a method whose id is > MAX_METHOD_ID_FOR_COMPACT_FORMAT */
    static void writeUnstampedEvent(byte eventType, ThreadInfo ti, char methodId) {
        // if (printEvents) System.out.println("*** Writing unstamped event " + (int) eventType + ", metodId = " + (int)methodId);
        byte[] evBuf = ti.evBuf;
        int curPos = ti.evBufPos; // It's important to use a local copy for evBufPos, so that evBufPos is at event boundary at any moment
        evBuf[curPos++] = eventType;
        evBuf[curPos++] = (byte) ((methodId >> 8) & 0xFF);
        evBuf[curPos++] = (byte) ((methodId) & 0xFF);
        ti.evBufPos = curPos;

        if (curPos > ThreadInfo.evBufPosThreshold) {
            copyLocalBuffer(ti);
        }
    }
}
