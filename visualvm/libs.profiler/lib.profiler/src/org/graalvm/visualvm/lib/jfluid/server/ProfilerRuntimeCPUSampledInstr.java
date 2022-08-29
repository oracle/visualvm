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

package org.graalvm.visualvm.lib.jfluid.server;

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
    static class TimeSampler extends SamplingThread {

        TimeSampler(int sampilingInterval) {
            super(sampilingInterval);
            setName(PROFILER_SPECIAL_EXEC_THREAD_NAME + " 9"); // NOI18N
       }

        void sample() {
            ThreadInfo.setSampleDueForAllThreads();
        }

    }
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    protected static int samplingInterval = 10;
    private static SamplingThread st;

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

        if (ti.stackDepth <= stackDepthLimit) {
            // when methodId > 64K/2 is passed here using our instrumentation's sipush command at the call site, 
            // it's treated here as a signed integer. Thus without
            // the below fix we can get e.g. an ArrayIndexOutOfBoundsException(-32768) when methodId == 32768 (***)
            int methodIdInt = methodId&0xff;
            methodIdInt |= methodId&0xff00;

            if (!instrMethodInvoked[methodIdInt]) {
                instrMethodInvoked[methodIdInt] = true; // Mark this method as invoked
                if (ti.rootMethodStackDepth > 0) { // marker method under root method - perform instrumentation of nearest callees
                    firstTimeMethodInvoke(ti, methodId);
                }
            }

            ProfilerServer.notifyClientOnResultsAvailability();
            writeParametersEvent(ti);
            if (ti.stackDepth > 0) {
                if (!ti.sampleDue) {
                    writeUnstampedEvent(MARKER_ENTRY_UNSTAMPED, ti, methodId);
                } else {
                    writeTimeStampedEvent(MARKER_ENTRY, ti, methodId);
                    ti.sampleDue = false;
                }
            } else {
                writeTimeStampedEvent(MARKER_ENTRY, ti, methodId);
            }
        }
        ti.stackDepth++; //= 1;  // This is the logical stack depth
        ti.inProfilingRuntimeMethod--;
    }

    /** Called upon exit from the marker method. */
    public static void markerMethodExit(char methodId) {
      markerMethodExit(NO_RET_VALUE, methodId);
    }
    
    /** Called upon exit from the marker method. */
    public static void markerMethodExit(Object ret, char methodId) {
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
                writeRetValue(ret, ti);
                writeTimeStampedEvent(MARKER_EXIT, ti, methodId);
            } else if (ti.stackDepth <= stackDepthLimit) {
                if (!ti.sampleDue) {
                    writeRetValue(ret, ti);
                    writeUnstampedEvent(MARKER_EXIT_UNSTAMPED, ti, methodId);
                } else {
                    writeRetValue(ret, ti);
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

            if (ti.stackDepth <= stackDepthLimit) {
                // See comment marked with (***)
                int methodIdInt = methodId&0xff;
                methodIdInt |= methodId&0xff00;

                // Now check if it's the first invocation of this method, and if so, perform instrumentation of nearest callees
                if (!instrMethodInvoked[methodIdInt]) {
                    instrMethodInvoked[methodIdInt] = true;
                    firstTimeMethodInvoke(ti, methodId);
                }

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
            }
            ti.stackDepth++;
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
            } else if (ti.stackDepth <= stackDepthLimit) {
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
                instrMethodInvoked[methodIdInt] = true;
                if (enableFirstTimeMethodInvoke) externalActionsHandler.handleFirstTimeMethodInvoke(methodId);
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
        st = new TimeSampler(samplingInterval);
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
