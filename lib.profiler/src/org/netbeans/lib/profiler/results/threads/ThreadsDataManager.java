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

package org.netbeans.lib.profiler.results.threads;

import org.netbeans.lib.profiler.client.MonitoredData;
import org.netbeans.lib.profiler.results.DataManager;
import java.util.Hashtable;


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

    private Hashtable idToIndex = new Hashtable(30);
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

            for (int i = 0; i < threadData.length; i++) {
                threadData[i].clearStates();
            }
        }
    }

    /**
     * Returns <CODE>true</CODE> if there are some monitored threads
     */
    public synchronized boolean hasData() {
        return (getThreadsCount() != 0);
    }

    /**
     * Convert the data received from the server on this iteration into the internal compressed format,
     * and notify listeners
     */
    public synchronized void processData(MonitoredData monitoredData) {
        //debugData ();
        if (monitoredData.getNThreads() == 0) {
            return;
        }

        int max = threadData.length;
        int newThreadsNum = monitoredData.getNNewThreads();

        // Set the timestamp of first data
        if (max == 0) {
            startTime = monitoredData.getStateTimestamps()[0];
        }

        // 1. process newly created threads
        if (newThreadsNum > 0) {
            addNewThreads(monitoredData.getNewThreadNames(), monitoredData.getNewThreadClassNames());

            int[] newIds = monitoredData.getNewThreadIds();

            for (int i = 0; i < newThreadsNum; i++) {
                idToIndex.put(new Integer(newIds[i]), new Integer(i + max)); // add new threads to the end of the array
            }
        }

        // 2. process all threads data
        if (threadsMonitoringEnabled) {
            int[] threadIds = monitoredData.getThreadIds();
            long[] timestamps = monitoredData.getStateTimestamps();
            byte[][] states = monitoredData.getThreadStates();
            int nThreads = monitoredData.getNThreads();
            int nStates = monitoredData.getNThreadStates();

            if (nStates == 0) {
                return;
            }

            for (int threadIdx = 0; threadIdx < nThreads; threadIdx++) {
                Integer intIndex = (Integer) idToIndex.get(new Integer(threadIds[threadIdx]));
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
