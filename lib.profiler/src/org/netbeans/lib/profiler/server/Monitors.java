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

import java.util.Stack;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.global.Platform;
import org.netbeans.lib.profiler.server.system.Classes;
import org.netbeans.lib.profiler.server.system.GC;
import org.netbeans.lib.profiler.server.system.Threads;
import org.netbeans.lib.profiler.server.system.Timers;
import org.netbeans.lib.profiler.wireprotocol.MonitoredNumbersResponse;
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

        public volatile boolean started;
        public volatile boolean terminated;
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

        public synchronized void addThreadStateChange(Thread thread, byte state, long timeStamp, Object monitor) {
            if (started && !terminated) threadTable.addExactState (thread, -1, state, timeStamp);
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
                threadTable.findDeathThreads();
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
        private int explPos;
        private byte[] explicitStates;
        private int[] explicitThreads;
        private long[] explicitTimeStamps;

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
            if (explicitThreads != null) {
                // pass the collected explicit thread state changes
                int[] msgExplicitThreads = new int[explPos];
                System.arraycopy(explicitThreads, 0, msgExplicitThreads, 0, explPos);
                byte[] msgExplicitStates = new byte[explPos];
                System.arraycopy(explicitStates, 0, msgExplicitStates, 0, explPos);
                long[] msgExplicitTimeStamps = new long[explPos];
                System.arraycopy(explicitTimeStamps, 0, msgExplicitTimeStamps, 0, explPos);
                
                mresp.setExplicitDataOnThreads(msgExplicitThreads, msgExplicitStates, msgExplicitTimeStamps);
                
                // and reset the arrays/pos
                if (explPos > 0) {
                    explicitStates = new byte[explPos];
                    explicitThreads = new int[explPos];
                    explicitTimeStamps = new long[explPos];
                }
                explPos = 0;
            } else {
                // explicit Threads states not supported
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
            }

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
                        Thread t = (Thread) threads[i];
                        newThreadClassNames[idx] = t.getClass().getName();
                        try {
                            newThreadNames[idx] = t.getName();
                        } catch (NullPointerException e) {
                            newThreadNames[idx] = "*Unknown thread ("+threadIds[i]+")*";  // NOI18N
                        }
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

        void put(Thread thread, int status) {
            int pos = getPosIndex(thread);

            if (threads[pos] == null) {
                threadNew[pos] = true;
                threads[pos] = thread;
                threadIds[pos] = curThreadId++;
                nThreads++;
                nNewThreads++;
                nFilledSlots++;
            }

            if (explicitThreads != null) {
                // we are actually using exact thread states tracking, so make use of zombie state that we otherwise do
                // not get
                if (status == CommonConstants.THREAD_STATUS_ZOMBIE || threadNew[pos]) {
                    addExactState(null, threadIds[pos], (byte)status, stateSampleTimestamps[curStateIdx]);
                }
                
                // just an optimization, if we use exact timings, the sampling data will only contain state running
                status = CommonConstants.THREAD_STATUS_RUNNING;
            }
            
            threadStates[pos][curStateIdx] = (byte) status;

            if (nFilledSlots > threshold) {
                growTable();
            }
        }

        private int getPosIndex(final Thread thread) {
            int pos = (thread.hashCode() & 0x7FFFFFFF) % size;

            while ((threads[pos] != thread) && (threads[pos] != null)) {
                pos = (pos + 1) % size;
            }
            return pos;
        }

        private void findDeathThreads() {
            if (explicitThreads == null) return;
            // we are actually using exact thread states tracking, so make use zombie state that we otherwise do
            for (int i = 0; i < size; i++) {
                if ((threads[i] == null) || (threads[i] == dummyObj)) {
                    continue;
                }

                if (threadStates[i][curStateIdx] == 0) { // Thread is dead
                    addExactState(null,threadIds[i],CommonConstants.THREAD_STATUS_ZOMBIE,stateSampleTimestamps[curStateIdx]);
                }
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

        void addExactState(Thread thread, int threadId, byte state, long timeStamp) {
            int id = threadId;
            if (id == -1) {
                id = findThreadId(thread);
            }
            if (id == -1) {
                // thread not found, forget about it
                return;
            }
            
            if (explicitThreads == null) {
                explicitStates = new byte[20];
                explicitThreads = new int [20];
                explicitTimeStamps = new long[20];
            }
            
            if (explicitStates.length == explPos) {
                byte[] newExplicitStates = new byte[explPos * 2];
                System.arraycopy(explicitStates, 0, newExplicitStates, 0, explicitStates.length);
                explicitStates = newExplicitStates;
                
                int[] newExplicitThreads = new int[explPos * 2];
                System.arraycopy(explicitThreads, 0, newExplicitThreads, 0, explicitThreads.length);
                explicitThreads = newExplicitThreads;
                
                long[] newExplicitTimeStamps = new long[explPos * 2];
                System.arraycopy(explicitTimeStamps, 0, newExplicitTimeStamps, 0, explicitTimeStamps.length);
                explicitTimeStamps = newExplicitTimeStamps;
            }
            
            explicitStates[explPos] = state;
            explicitThreads[explPos] = id;
            explicitTimeStamps[explPos] = timeStamp;
            explPos++;
        }

        private int findThreadId(Thread thread) {
            int pos = getPosIndex(thread);
            
            if (threads[pos] == thread) {
                return threadIds[pos];
            }
            return -1; // not found
        }

    }

    interface DeterminateProgress {
        public void next();        
    }

    private static class ActiveServerState implements DeterminateProgress{
        //~ Instance fields ----------------------------------------------------------------------------------------------------------
        private final ActiveServerState parent;
        private final int serverState;
        private final int stepCount;
        private final double stepSize;
        private final boolean indeterminate;
        private int step;

        private final int id;
        private static int counter = 0;

        //~ Constructors -------------------------------------------------------------------------------------------------------------
        ActiveServerState(int serverState) {
            this(null, serverState, 0);
        }

        ActiveServerState(ActiveServerState parent, int serverState, int stepCount) {
            this.parent = parent;
            this.serverState = serverState;
            this.stepCount = stepCount;
            if (stepCount == 0) {
                this.stepSize = parent == null ? 1.0 : parent.stepSize;
                this.indeterminate = parent == null ? true : parent.indeterminate;
            } else {
                this.stepSize = parent == null ? 1.0/stepCount : parent.stepSize/stepCount;
                this.indeterminate = false;
            }
            this.step = 0;
            this.id = counter ++;
            //System.out.println("ActiveServerState #"+String.valueOf(id)+": init(serverState="+String.valueOf(serverState)+", stepCount="+String.valueOf(stepCount)+", stepSize="+String.valueOf(stepSize)+")");
        }
        
        //~ Methods ------------------------------------------------------------------------------------------------------------------

        private int getServerState() {
            return serverState;
        }

        private int getProgress() {
            if(indeterminate) {
               return CommonConstants.SERVER_PROGRESS_INDETERMINATE; 
            }
            return (int)(getRealProgress()*CommonConstants.SERVER_PROGRESS_WORKUNITS);
        }

        private synchronized double getRealProgress() {
            double result;
            if(indeterminate) {
                result = 0.0;
            } else if(parent == null) {
                assert stepCount == 0: "called for indeterminate state";
                result = 1.0*step/stepCount;
            } else {
                result = parent.getRealProgress() + parent.stepSize*step/stepCount;
            }
            return result;
        }
          
        public synchronized void next() {
            assert stepCount > 0: "called for indeterminate progress state";
            step ++;
            if(step >= stepCount) {
                step = stepCount - 1;
            }
        }        
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final boolean DEBUG = Boolean.getBoolean("org.netbeans.lib.profiler.server.Monitors");
    protected static Runtime runtime;
    protected static SurvGenAndThreadsMonitor stMonitor;
    protected static long[] generalMNums;
    protected static long[] gcRelTime;
    protected static long[] gcStartTimes;
    protected static long[] gcFinishTimes;
    private static long startTimeMilis;
    private static long startTimeCounts;
    private static boolean threadsSamplingEnabled;

    protected static long time; // Used just for estimating the overhead

    private static ActiveServerState activeServerState = new ActiveServerState(CommonConstants.SERVER_RUNNING);
    private static final Object activeServerStateLock = new Object();
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

        int serverState;
        int serverProgress;
        synchronized(activeServerStateLock) {
            serverState = activeServerState.getServerState();
            serverProgress = activeServerState.getProgress();
        }
        MonitoredNumbersResponse resp = new MonitoredNumbersResponse(generalMNums, serverState, serverProgress);
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
        startTimeMilis = System.currentTimeMillis();
        startTimeCounts = Timers.getCurrentTimeInCounts();
        stMonitor.start();
    }

    static void setThreadsSamplingEnabled(boolean b) {
        threadsSamplingEnabled = b;
    }

    static DeterminateProgress enterServerState(int serverState, int stepCount) {
        synchronized(activeServerStateLock) {
            activeServerState = new ActiveServerState(activeServerState, serverState, stepCount);
            return activeServerState;
        }
    }

    static void enterServerState(int serverState) {
        synchronized(activeServerStateLock) {
            activeServerState = new ActiveServerState(activeServerState, serverState, 0);
        }
    }

    static void exitServerState() {
        synchronized(activeServerStateLock) {
            activeServerState = activeServerState.parent;
        }
    }

    static void recordThreadStateChange(Thread thread, byte state, long timeStamp, Object monitor) {
        if (threadsSamplingEnabled) return;
        if (timeStamp == -1) {
            timeStamp = Timers.getCurrentTimeInCounts();
        }
        // convert to 
        long diff = timeStamp - startTimeCounts;
        diff /= Timers.getNoOfCountsInSecond() / 1000;
        timeStamp = startTimeMilis + diff;
        if (DEBUG) {
              switch (state) {
                case CommonConstants.THREAD_STATUS_MONITOR:
                    System.err.println("Thread state change: "+thread.getName()+", Monitor: "+timeStamp+", monitor: "+System.identityHashCode(monitor));
                    break;
                case CommonConstants.THREAD_STATUS_WAIT:
                    System.err.println("Thread state change: "+thread.getName()+", Wait: "+timeStamp);
                    break;
                case CommonConstants.THREAD_STATUS_SLEEPING:
                    System.err.println("Thread state change: "+thread.getName()+", Sleep: "+timeStamp);
                    break;
                case CommonConstants.THREAD_STATUS_RUNNING:
                    System.err.println("Thread state change: "+thread.getName()+", Run: "+timeStamp);
                    break;
            }
        }

        if (stMonitor != null) {
            stMonitor.addThreadStateChange(thread, state, timeStamp, monitor);
        }
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
