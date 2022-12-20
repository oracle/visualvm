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

package org.graalvm.visualvm.lib.jfluid.results.threads;

import java.util.HashMap;
import java.util.Map;
import org.graalvm.visualvm.lib.jfluid.client.MonitoredData;
import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;
import org.graalvm.visualvm.lib.jfluid.results.DataManager;


/**
 * A class that holds data about threads history (state changes) during a
 * profiling session. It consumes/processes data obtained from the server via the
 * MonitoredData class, but translates them into data structures more efficient for
 * presentation. A listener is provided for those who want to be notified about
 * newly arrived data.
 *
 * @author Jiri Sedlacek
 * @author Ian Formanek
 * @author Misha Dmitriev
 */
public class ThreadsDataManager extends DataManager {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private Map<Integer,Integer> idToIndex = new HashMap(30);
    private ThreadData[] threadData; // Per-thread array of points at which thread's state changes
    private boolean supportsSleepingState = true;
    private boolean threadsMonitoringEnabled = true;
    private long endTime; // Timestamp of threadData end
    private long startTime; // Timestamp of threadData start

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Creates a new instance of ThreadsDataManager
     */
    public ThreadsDataManager() {
        reset();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the timestamp representing end time of collecting threadData (timestamp of last valid threadData record).
     */
    public synchronized long getEndTime() {
        return endTime;
    }

    // --- Public interface ---------------------------------------------------------------

    /**
     * Returns the timestamp representing start time of collecting threadData (timestamp of first threadData record).
     */
    public synchronized long getStartTime() {
        return startTime;
    }

    public synchronized void setSupportsSleepingStateMonitoring(boolean supportsSleepingState) {
        this.supportsSleepingState = supportsSleepingState;
    }

    public synchronized String getThreadClassName(int index) {
        return threadData[index].getClassName();
    }

    public synchronized ThreadData getThreadData(int index) {
        return threadData[index];
    }

    public synchronized String getThreadName(int index) {
        return threadData[index].getName();
    }

    /**
     * Returns the number of currently monitored threads
     */
    public synchronized int getThreadsCount() {
        return threadData.length;
    }

    public synchronized void setThreadsMonitoringEnabled(boolean enabled) {
        if (threadsMonitoringEnabled == enabled) {
            return;
        }

        threadsMonitoringEnabled = enabled;

        if (!threadsMonitoringEnabled) { // clear accumulated data, except thread ids and names
            for (ThreadData threadData1 : threadData) {
                threadData1.clearStates();
            }
        }
    }

    /**
     * Returns <CODE>true</CODE> if there are some monitored threads
     */
    public synchronized boolean hasData() {
        return getThreadsCount() != 0;
    }

    /**
     * Convert the data received from the server on this iteration into the internal compressed format,
     * and notify listeners
     */
    public synchronized void processData(MonitoredData monitoredData) {
        //debugData ();
        int max = threadData.length;
        int newThreadsNum = monitoredData.getNNewThreads();

        // 1. process newly created threads
        if (newThreadsNum > 0) {
            addNewThreads(monitoredData.getNewThreadNames(), monitoredData.getNewThreadClassNames());

            int[] newIds = monitoredData.getNewThreadIds();

            for (int i = 0; i < newThreadsNum; i++) {
                idToIndex.put(Integer.valueOf(newIds[i]), Integer.valueOf(i + max)); // add new threads to the end of the array
            }
        }

        // 2. process all threads data
        if (threadsMonitoringEnabled) {
            if (monitoredData.getThreadsDataMode() == CommonConstants.MODE_THREADS_EXACT) {
                int[] exThreadIds = monitoredData.getExplicitThreadIds();
                long[] exTimestamps = monitoredData.getExplicitStateTimestamps();
                byte[] exStates = monitoredData.getExplicitThreadStates();
                
                if (exTimestamps.length == 0) {
                    return;
                }
                if (startTime == 0) {
                    startTime = exTimestamps[0];
                }
                // precise states timers
                for (int i = 0; i < exThreadIds.length; i++) {
                    Integer intIndex = idToIndex.get(Integer.valueOf(exThreadIds[i]));
                    int index = intIndex.intValue();
                    ThreadData tData = threadData[index];
                    tData.add(exTimestamps[i], exStates[i]);
                }
                
                endTime = exTimestamps[exTimestamps.length - 1];
                fireDataChanged(); // all listeners are notified about threadData change */
            } else if (monitoredData.getThreadsDataMode() == CommonConstants.MODE_THREADS_SAMPLING) {
                int[] threadIds = monitoredData.getThreadIds();
                long[] timestamps = monitoredData.getStateTimestamps();
                byte[][] states = monitoredData.getThreadStates();
                int nThreads = monitoredData.getNThreads();
                int nStates = monitoredData.getNThreadStates();
                
                if (nStates == 0 || nThreads == 0) {
                    return;
                }                
                // Set the timestamp of first data
                if (startTime == 0) {
                    startTime = timestamps[0];
                }
                for (int threadIdx = 0; threadIdx < nThreads; threadIdx++) {
                    Integer intIndex = idToIndex.get(Integer.valueOf(threadIds[threadIdx]));
                    int index = intIndex.intValue();
                    byte[] threadStates = states[threadIdx];
                    ThreadData tData = threadData[index];
                    
                    for (int stampIdx = 0; stampIdx < nStates; stampIdx++) {
                        long timeStamp = timestamps[stampIdx];
                        byte state = threadStates[stampIdx];
                        byte lastState = tData.getLastState();
                        
                        if ((lastState == ThreadData.NO_STATE) || (lastState != state)) {
                            tData.add(timeStamp, state);
                        }
                    }
                }
                
                endTime = timestamps[nStates - 1]; // end timestamp is updated
                fireDataChanged(); // all listeners are notified about threadData change */
            }
        } else {
            // in this mode we are only tracking thread ids and names, not thread states
        }
    }

    /**
     * Resets the threadData - clears timestamps and threadData store.
     */
    public synchronized void reset() {
        startTime = 0;
        endTime = 0;
        threadData = new ThreadData[0];
        idToIndex.clear();
        fireDataReset(); // all listeners are notified about threadData change
    }
    
    /**
     * Resets the collected data during a running profiling session.
     */
    public synchronized void resetStates() {
        if (threadData != null) {
            startTime = 0;
            endTime = 0;
            for (ThreadData data : threadData) data.clearStates();
            fireDataReset(); // all listeners are notified about threadData change
        }
    }

    public synchronized boolean supportsSleepingStateMonitoring() {
        return supportsSleepingState;
    }

    // --- Private implementation ---------------------------------------------------------------

    /**
     * Enlarges internal array of threads' threadData stores according to newly created threads
     */
    private void addNewThreads(String[] newNames, String[] newClassNames) {
        int newSize = newNames.length + threadData.length;
        ThreadData[] tmpData = new ThreadData[newSize];

        if (threadData.length > 0) {
            System.arraycopy(threadData, 0, tmpData, 0, threadData.length);
        }

        for (int i = threadData.length, idx = 0; i < newSize; i++, idx++) {
            tmpData[i] = new ThreadData(newNames[idx], newClassNames[idx]);
        }

        threadData = tmpData;
    }

    //  private void debugData() {
    //    System.err.print("start time: " + startTime); // NOI18N
    //    System.err.print(", end time: " + endTime); // NOI18N
    //    System.err.println(", delta time: " + (endTime - startTime)); // NOI18N
    //
    //    System.err.println("number of threads: " + threadData.length); // NOI18N
    //    //new Exception("Stack trace").printStackTrace(System.err);
    //    for (int i = 0; i < threadData.length; i++) {
    //      System.err.println(
    //          "thread [" + i + "] = " + threadData[i].getName() + " class " + threadData[i].getClassName() // NOI18N
    //      );
    //    }
    //  }
}
