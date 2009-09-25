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

import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.global.Platform;
import org.netbeans.lib.profiler.server.system.Classes;
import org.netbeans.lib.profiler.server.system.GC;
import org.netbeans.lib.profiler.server.system.Threads;
import org.netbeans.lib.profiler.server.system.Timers;
import org.netbeans.lib.profiler.wireprotocol.MethodNamesResponse;
import org.netbeans.lib.profiler.wireprotocol.MonitoredNumbersResponse;
import org.netbeans.lib.profiler.wireprotocol.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;


/**
 * Implementation of the monitoring functionality, active throughout the TA execution.
 *
 * @author Tomas Hurka
 * @author Misha Dmitriev
 * @author Ian Formanek
 */
public class Monitors implements CommonConstants {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    static class LongList {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        long[] data;
        int size;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        LongList(int size) {
            data = new long[size];
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        long[] getArray() {
            long[] arr = new long[size];
            System.arraycopy(data, 0, arr, 0, size);

            return arr;
        }

        void add(long l) {
            ensureSize();
            data[size++] = l;
        }

        void clear() {
            size = 0;
        }

        void ensureSize() {
            if (size >= data.length) {
                int newCapacity = ((size * 3) / 2) + 1;
                long[] elementData = new long[newCapacity];
                System.arraycopy(data, 0, elementData, 0, size);
                data = elementData;
            }
        }
    }

    static class SurvGenAndThreadsMonitor extends Thread {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        public boolean started;
        public boolean terminated;
        private LongList gcFinishs;
        private LongList gcStarts;
        private ThreadDataTable threadTable;
        private Vector markerObjects;
        private int[] allThreadStatusRough;

        // ---------------------------------- Thread status data management -------------------------------------
        private Thread[] allThreadsRough;

        // -------------------------------- Surviving generations data management ---------------------------------
        private int savedGCEpoch;
        private long lastGCFinish;
        private long lastGCStart;
        private long time; // Used just for estimating the overhead
        private long time0; // Used just for estimating the overhead

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        SurvGenAndThreadsMonitor() {
            super("*** JFluid Monitor thread ***"); // NOI18N
            savedGCEpoch = GC.getCurrentGCEpoch();
            markerObjects = new Vector();
            allThreadsRough = new Thread[20];
            allThreadStatusRough = new int[20];
            threadTable = new ThreadDataTable();
            gcStarts = new LongList(16);
            gcFinishs = new LongList(16);
            setPriority(Thread.MAX_PRIORITY);
            setDaemon(true);
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public int getNSurvGen() {
            return markerObjects.size();
        }

        public synchronized void getThreadsData(MonitoredNumbersResponse mresp) {
            //threadTable.printCurrentStatus();
            threadTable.getThreadsData(mresp);
            threadTable.resetStates();
        }

        public long getTime() {
            long ret = time;
            time = 0;

            return ret;
        }

        public void run() {
            started = true;

            int checkForUnloadedClassesCounter = 3;

            while (!terminated) {
                //time0 = ProfilerRuntime.getCurrentTimeInCounts();
                updateSurvGenData();
                updateThreadsData();
                updateGCStartFinishData();

                // The next call has nothing to do with monitoring - it's "housekeeping". See details in ClassLoaderManager.
                if (--checkForUnloadedClassesCounter == 0) {
                    ClassLoaderManager.checkForUnloadedClasses();
                    checkForUnloadedClassesCounter = 3;
                }

                ThreadInfo.releaseDeadThreads();

                //time += (ProfilerRuntime.getCurrentTimeInCounts() - time0);
                try {
                    Thread.sleep(100);
                } catch (Exception ex) {
                }
            }
        }

        synchronized void updateGCStartFinishData() {
            int i;
            long maxStart = lastGCStart;
            long maxFinish = lastGCFinish;
            GC.getGCStartFinishTimes(gcStartTimes, gcFinishTimes);

            for (i = 0; i < GC.OBSERVED_PERIODS; i++) {
                long start = gcStartTimes[i];
                long finish = gcFinishTimes[i];

                if (start > lastGCStart) {
                    gcStarts.add(start & 0xFFFFFFFFFFFFFFL); // we use only 7 bytes for hi res timer
                    maxStart = start;
                }

                if (finish > lastGCFinish) {
                    gcFinishs.add(finish & 0xFFFFFFFFFFFFFFL); // we use only 7 bytes for hi res timer
                    maxFinish = finish;
                }
            }

            lastGCStart = maxStart;
            lastGCFinish = maxFinish;
        }

        private synchronized void getGCStartFinishData(MonitoredNumbersResponse resp) {
            long[] start = gcStarts.getArray();
            long[] finish = gcFinishs.getArray();

            gcStarts.clear();
            gcFinishs.clear();

            resp.setGCstartFinishData(start, finish);
        }

        private void updateSurvGenData() {
            // Compare the previously saved GC epoch value with the current one, and allocate new marker if it changed
            int currentGCEpoch = GC.getCurrentGCEpoch();

            if (currentGCEpoch != savedGCEpoch) {
                //System.out.println("***              GC epoch changed from " + savedGCEpoch + " to " + currentGCEpoch);
                markerObjects.add(new Object());
                savedGCEpoch = currentGCEpoch;

                // Walk through the markers, determine which ones are adjacent, etc.
                int lenMinusOne = markerObjects.size() - 1;
                int initSize = lenMinusOne + 1;

                for (int i = 0; i < lenMinusOne; i++) {
                    if (GC.objectsAdjacent(markerObjects.get(i), markerObjects.get(i + 1))) {
                        //System.out.println("********* Objects adjacent at i = " + i + ": " + markerObjects.get(i) + " and " + markerObjects.get(i+1));
                        markerObjects.remove(i);
                        i--;
                        lenMinusOne--;
                    }
                }
            }
        }

        private void updateThreadsData() {
            Thread tMain = ProfilerServer.isTargetAppMainThreadComplete() ? ProfilerServer.getMainThread() : null;

            allThreadsRough = Threads.getAllThreads(allThreadsRough);

            if (allThreadStatusRough.length < allThreadsRough.length) {
                allThreadStatusRough = new int[allThreadsRough.length];
            }

            Threads.getThreadsStatus(allThreadsRough, allThreadStatusRough);

            if (DEBUG) {
                for (int i = 0; i < allThreadsRough.length; i++) {
                    if (allThreadsRough[i] == null) {
                        break;
                    }

                    System.err.println("org.netbeans.lib.profiler.server.Monitors.DEBUG: " // NOI18N
                                       + allThreadsRough[i].getName() + ", status: " + allThreadStatusRough[i]); // NOI18N
                }
            }

            synchronized (this) {
                threadTable.prePut();

                for (int i = 0; i < allThreadsRough.length; i++) {
                    Thread thread = allThreadsRough[i];

                    if (thread == null) {
                        break; // No more live threads in this array
                    }

                    // We check if the thread is our own using ThreadInfo.isProfilerServerThread(), not its name
                    // (which looks easier), because it turns out that Thread.getName() creates a new String out of internal
                    // char[] array every time. ThreadInfo check should be just faster.
                    if ((thread == this) || (thread == tMain) || ThreadInfo.isProfilerServerThread(thread)) {
                        //System.err.println("Skipping thread "+i+", tMain:"+tMain +", thread: "+thread);
                        continue;
                    }

                    threadTable.put(thread, allThreadStatusRough[i]);
                }

                threadTable.incStatusIdx();
            }

            if (DEBUG) {
                System.err.println("Final thread table: "); // NOI18N
                threadTable.printCurrentStatus();
            }
        }
    }

    static class ThreadDataTable {
        //~ Static fields/initializers -------------------------------------------------------------------------------------------

        private static final int INITIAL_SIZE = 23;
        private static final int INITIAL_NSTATES = 20;
        private static Object dummyObj = new Object();

        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private String[] newThreadClassNames = new String[INITIAL_SIZE];
        private int[] newThreadIds = new int[INITIAL_SIZE];
        private String[] newThreadNames = new String[INITIAL_SIZE];
        private long[] packedStateTimestamps = new long[INITIAL_NSTATES];
        private int[] packedThreadIds = new int[INITIAL_SIZE];
        private byte[] packedThreadStates = new byte[INITIAL_SIZE * INITIAL_NSTATES];
        private long[] stateSampleTimestamps;
        private int[] threadIds;
        private boolean[] threadNew;
        private byte[][] threadStates;
        private Object[] threads;
        private boolean jvmSupportsThreadSleepingState;
        private int curStateIdx;
        private int curThreadId;
        private int nFilledSlots;
        private int nNewThreads;
        private int nStates;
        private int nThreads;
        private int size;
        private int threshold;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        ThreadDataTable() {
            size = INITIAL_SIZE;
            threshold = (size * 3) / 4;
            curThreadId = 1;
            nStates = INITIAL_NSTATES;
            threads = new Object[size];
            threadIds = new int[size];
            threadStates = new byte[size][nStates];
            stateSampleTimestamps = new long[nStates];
            threadNew = new boolean[size];
            nThreads = 0;
            nFilledSlots = 0;
            curStateIdx = 0;
            jvmSupportsThreadSleepingState = Platform.thisVMSupportsThreadSleepingStateMonitoring();
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void getThreadsData(MonitoredNumbersResponse mresp) {
            if (nThreads > packedThreadIds.length) {
                packedThreadIds = new int[nThreads];
            }

            if (curStateIdx > packedStateTimestamps.length) {
                packedStateTimestamps = new long[curStateIdx];
            }

            int totalStates = nThreads * curStateIdx;

            if (totalStates > packedThreadStates.length) {
                packedThreadStates = new byte[totalStates];
            }

            int idx0 = 0;
            int idx1 = 0;

            for (int i = 0; i < size; i++) {
                if ((threads[i] != null) && (threads[i] != dummyObj)) {
                    packedThreadIds[idx0++] = threadIds[i];
                    System.arraycopy(threadStates[i], 0, packedThreadStates, idx1, curStateIdx);
                    idx1 += curStateIdx;
                }
            }

            System.arraycopy(stateSampleTimestamps, 0, packedStateTimestamps, 0, curStateIdx);

            mresp.setDataOnThreads(nThreads, curStateIdx, packedThreadIds, packedStateTimestamps, packedThreadStates);

            if (nNewThreads > 0) {
                if (nNewThreads > newThreadIds.length) {
                    newThreadIds = new int[nNewThreads];
                    newThreadNames = new String[nNewThreads];
                    newThreadClassNames = new String[nNewThreads];
                }

                int idx = 0;

                for (int i = 0; i < size; i++) {
                    if (threadNew[i]) {
                        newThreadIds[idx] = threadIds[i];
                        newThreadNames[idx] = ((Thread) threads[i]).getName();
                        newThreadClassNames[idx] = ((Thread) threads[i]).getClass().getName();
                        idx++;
                    }
                }

                mresp.setDataOnNewThreads(nNewThreads, newThreadIds, newThreadNames, newThreadClassNames);
            }
        }

        public void incStatusIdx() {
            curStateIdx++;

            if (curStateIdx == nStates) {
                growStatesArrays();
            }
        }

        /**
         * Sets current status for all threads to dead. Subsequent calls to put will change the status of live threads appropriately.
         * The rest of the threads for which status will remain dead, are really dead.
         */
        public void prePut() {
            for (int i = 0; i < size; i++) {
                threadStates[i][curStateIdx] = 0;
            }

            stateSampleTimestamps[curStateIdx] = System.currentTimeMillis();
        }

        /** Debugging support */
        public void printCurrentStatus() {
            for (int i = 0; i < size; i++) {
                if ((threads[i] == null) || (threads[i] == dummyObj)) {
                    continue;
                }

                System.err.print(((Thread) threads[i]).getName() + "  "); // NOI18N

                byte[] states = threadStates[i];

                for (int j = 0; j < curStateIdx; j++) {
                    System.err.print(states[j]);
                }

                System.err.println();
            }

            System.err.println();
        }

        public void put(Thread thread, int status) {
            int pos = (thread.hashCode() & 0x7FFFFFFF) % size;

            while ((threads[pos] != thread) && (threads[pos] != null)) {
                pos = (pos + 1) % size;
            }

            if (threads[pos] == null) {
                threadNew[pos] = true;
                threads[pos] = thread;
                threadIds[pos] = curThreadId++;
                nThreads++;
                nNewThreads++;
                nFilledSlots++;
            }

            threadStates[pos][curStateIdx] = (byte) status;

            if (nFilledSlots > threshold) {
                growTable();
            }
        }

        public void resetStates() {
            nNewThreads = 0;

            if (curStateIdx == 0) {
                return;
            }

            int idx = curStateIdx - 1;

            for (int i = 0; i < size; i++) {
                if ((threads[i] == null) || (threads[i] == dummyObj)) {
                    continue;
                }

                threadNew[i] = false;

                if (threadStates[i][idx] == 0) { // Thread is dead
                    threads[i] = dummyObj;
                    nThreads--;
                }
            }

            curStateIdx = 0;
        }

        private void growStatesArrays() {
            int oldNStates = nStates;
            nStates = nStates * 2;

            for (int i = 0; i < size; i++) {
                byte[] oldStates = threadStates[i];
                threadStates[i] = new byte[nStates];
                System.arraycopy(oldStates, 0, threadStates[i], 0, oldNStates);
            }

            long[] oldTimestamps = stateSampleTimestamps;
            stateSampleTimestamps = new long[nStates];
            System.arraycopy(oldTimestamps, 0, stateSampleTimestamps, 0, oldNStates);
        }

        private void growTable() {
            int oldSize = size;
            size = (((nThreads * 4) / 3) * 2) + 1;

            if (size < oldSize) {
                size = oldSize; // Too many threads are dead; get rid of them without growing the table
                                // Otherwise, if the application generates lots of short-lived threads, we may probably end up growing the table
                                // endlessly without reason
            }

            threshold = (size * 3) / 4;

            Object[] oldThreads = threads;
            int[] oldThreadIds = threadIds;
            byte[][] oldThreadStates = threadStates;
            boolean[] oldThreadNew = threadNew;
            threads = new Object[size];
            threadIds = new int[size];
            threadStates = new byte[size][];
            threadNew = new boolean[size];

            for (int i = 0; i < oldSize; i++) {
                if ((oldThreads[i] == null) || (oldThreads[i] == dummyObj)) {
                    continue;
                }

                Object thread = oldThreads[i];
                int pos = (thread.hashCode() & 0x7FFFFFFF) % size;

                while (threads[pos] != null) {
                    pos = (pos + 1) % size;
                }

                threadNew[pos] = oldThreadNew[i];
                threads[pos] = thread;
                threadIds[pos] = oldThreadIds[i];
                threadStates[pos] = oldThreadStates[i];
            }

            for (int i = 0; i < size; i++) {
                if (threadStates[i] == null) {
                    threadStates[i] = new byte[nStates];
                }
            }

            nFilledSlots = nThreads;
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final boolean DEBUG = false;
    protected static Runtime runtime;
    protected static SurvGenAndThreadsMonitor stMonitor;
    protected static long[] generalMNums;
    protected static long[] gcRelTime;
    protected static long[] gcStartTimes;
    protected static long[] gcFinishTimes;
    protected static long time; // Used just for estimating the overhead

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static MonitoredNumbersResponse getMonitoredNumbers() {
        //time = ProfilerRuntime.getCurrentTimeInCounts();
        int nUserThreads;
        int nTotalThreads = Threads.getTotalNumberOfThreads();
        nTotalThreads--; // ProfilerServer.executeInSeparateThread()
                         // This number includes our Server communication thread, memory monitor thread, and separate command execution thread

        int nSystemThreads = ProfilerInterface.getNPrerecordedSystemThreads();

        if (nSystemThreads != -1) { // "Start from tool" mode, so we have recorded system threads
            nUserThreads = nTotalThreads - nSystemThreads;
            nSystemThreads -= 2; // To hide our own Server and Memory monitor threads

            if (ProfilerServer.isTargetAppMainThreadComplete()) { // It's not really complete, but executes JFluid code, so logically it's done.
                nUserThreads--;
            }
        } else { // Attachment mode, no exact knowledge of the number of system threads
            nUserThreads = nTotalThreads - 3; // At least we know that two threads are JFluid-owned
        }

        // Now compensate for an additonal Java thread used in the following two instrumentation modes
        int instrType = ProfilerInterface.getCurrentInstrType();

        if ((instrType == INSTR_RECURSIVE_SAMPLED) || (instrType == INSTR_OBJECT_LIVENESS)) {
            nUserThreads--;
        }

        // Get the relative GC time metrics
        GC.getGCRelativeTimeMetrics(gcRelTime);

        generalMNums[MonitoredNumbersResponse.FREE_MEMORY_IDX] = runtime.freeMemory();
        generalMNums[MonitoredNumbersResponse.TOTAL_MEMORY_IDX] = runtime.totalMemory();
        generalMNums[MonitoredNumbersResponse.USER_THREADS_IDX] = nUserThreads;
        generalMNums[MonitoredNumbersResponse.SYSTEM_THREADS_IDX] = nSystemThreads;
        generalMNums[MonitoredNumbersResponse.SURVIVING_GENERATIONS_IDX] = stMonitor.getNSurvGen();
        generalMNums[MonitoredNumbersResponse.GC_TIME_IDX] = gcRelTime[0];
        generalMNums[MonitoredNumbersResponse.GC_PAUSE_IDX] = gcRelTime[1];
        generalMNums[MonitoredNumbersResponse.LOADED_CLASSES_IDX] = Classes.getLoadedClassCount();
        generalMNums[MonitoredNumbersResponse.TIMESTAMP_IDX] = System.currentTimeMillis();

        MonitoredNumbersResponse resp = new MonitoredNumbersResponse(generalMNums);
        stMonitor.getThreadsData(resp);
        stMonitor.getGCStartFinishData(resp);

        //showTime();
        return resp;
    }

    public static void initialize() {
        runtime = Runtime.getRuntime();
        gcRelTime = new long[2];
        gcStartTimes = new long[GC.OBSERVED_PERIODS];
        gcFinishTimes = new long[GC.OBSERVED_PERIODS];
        generalMNums = new long[MonitoredNumbersResponse.GENERAL_NUMBERS_SIZE];
        GC.activateGCEpochCounter(true);
        stMonitor = new SurvGenAndThreadsMonitor();
        ThreadInfo.addProfilerServerThread(stMonitor);
        stMonitor.start();
    }

    /** Check if all monitor threads have been started */
    public static boolean monitorThreadsStarted() {
        return stMonitor.started;
    }

    public static void shutdown() {
        stMonitor.terminated = true;
    }

    /** Measures and prints the overhead of the monitoring code */
    private static void showTime() {
        long cnts = Timers.getNoOfCountsInSecond();
        time = ((Timers.getCurrentTimeInCounts() - time) * 1000000) / cnts;

        long time1 = (stMonitor.getTime() * 1000000) / cnts;
        System.out.println("!!! time = " + time + ", time1 = " + time1 + ", sum = " + (time + time1)); // NOI18N
        // Originally (with no thread state information) time returned above would be 60..80 + 600..700 microsec
        // If getMonitoredNumbers() is called roughly once a second, this translates into 750/1000000*100 = 0.075 per cent overhead
        // With thread state information, overhead grows to about 60..80 + 1000 microsec. Still just 0.1 per cent overhead.
        // Transport, and, more importantly, client-side processing, likely take much more time.
    }
}
