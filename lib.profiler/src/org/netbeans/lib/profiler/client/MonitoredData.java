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

package org.netbeans.lib.profiler.client;

import org.netbeans.lib.profiler.TargetAppRunner;
import org.netbeans.lib.profiler.global.ProfilingSessionStatus;
import org.netbeans.lib.profiler.wireprotocol.MonitoredNumbersResponse;


/**
 * A representation of the monitored data, returned by the server on demand, that is suitable for use by
 * presentation code.
 *
 * @author Tomas Hurka
 * @author  Misha Dmitriev
 */
public class MonitoredData {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private long[] gcFinishs;
    private long[] gcStarts;

    // The following array contains:
    // - the amounts of free and total memory in elements 0 and 1
    // - the number of user and system threads in elements 2 and 3
    // - number of surviving generations in element 4
    // - relative time spent in GC, in per mil (1/10th of per cent), and the duration of the last GC pause (in ms) in elements 5 and 6
    // - timestamp at the moment when this packet was generated (obtained with System.currentTimeMillis()) in element 7
    private long[] generalMNumbers;
    private String[] newThreadClassNames;
    private int[] newThreadIds;
    private String[] newThreadNames;
    private long[] stateTimestamps;
    private int[] threadIds;
    private byte[][] threadStates = new byte[20][20];

    // Data on new threads. Any thread that has been created between the previous and the current use of this object
    // shows up on the list below, but just once. nNewThreads is the real number of threads, which may be shorter than
    // the size of the following arrays.
    private int nNewThreads;
    private int nThreadStates;

    // Data on thread states. nThreads is the real number of threads (dimension 0 of the following arrays), which
    // may be shorter than the actual size of the following arrays. nStates is the number of thread states
    // (dimension 1 of these arrays), and also may be shorter than the actual size.
    // Thread state timestamps are expressed in milliseconds as obtained by System.currentTimeMillis() on server side.
    // threadStates use constants defined in CommonConstants for thread states.
    private int nThreads;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    private MonitoredData(MonitoredNumbersResponse mresp) {
        long[] gn = mresp.getGeneralMonitoredNumbers();
        generalMNumbers = new long[gn.length];
        System.arraycopy(gn, 0, generalMNumbers, 0, gn.length);

        nThreads = mresp.getNThreads();
        nThreadStates = mresp.getNThreadStates();

        int[] ids = mresp.getThreadIds();
        threadIds = new int[nThreads];
        System.arraycopy(ids, 0, threadIds, 0, nThreads);

        long[] ts = mresp.getStateTimestamps();
        stateTimestamps = new long[nThreadStates];
        System.arraycopy(ts, 0, stateTimestamps, 0, nThreadStates);

        setThreadStates(mresp.getThreadStates());

        nNewThreads = mresp.getNNewThreads();

        if (nNewThreads > 0) {
            int[] newIds = mresp.getNewThreadIds();
            newThreadIds = new int[nNewThreads];
            System.arraycopy(newIds, 0, newThreadIds, 0, nNewThreads);
            newThreadNames = new String[nNewThreads];
            System.arraycopy(mresp.getNewThreadNames(), 0, newThreadNames, 0, nNewThreads);
            newThreadClassNames = new String[nNewThreads];
            System.arraycopy(mresp.getNewThreadClassNames(), 0, newThreadClassNames, 0, nNewThreads);
        }

        gcStarts = mresp.getGCStarts();
        convertToTimeInMillis(gcStarts);
        gcFinishs = mresp.getGCFinishs();
        convertToTimeInMillis(gcFinishs);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public long getFreeMemory() {
        return generalMNumbers[MonitoredNumbersResponse.FREE_MEMORY_IDX];
    }

    public long[] getGCFinishs() {
        return gcFinishs;
    }

    public long[] getGCStarts() {
        return gcStarts;
    }

    public long getLastGCPauseInMS() {
        return generalMNumbers[MonitoredNumbersResponse.GC_PAUSE_IDX];
    }

    public long getLoadedClassesCount() {
        return generalMNumbers[MonitoredNumbersResponse.LOADED_CLASSES_IDX];
    }

    /**
     * With mresp, the same instance is reused all the time to save memory. However, with MonitoredData we
     * generally can't afford that, so here we create a new object every time and copy data into it.
     */
    public static MonitoredData getMonitoredData(MonitoredNumbersResponse mresp) {
        return new MonitoredData(mresp);
    }

    public int getNNewThreads() {
        return nNewThreads;
    }

    public long getNSurvivingGenerations() {
        return generalMNumbers[MonitoredNumbersResponse.SURVIVING_GENERATIONS_IDX];
    }

    public long getNSystemThreads() {
        return generalMNumbers[MonitoredNumbersResponse.SYSTEM_THREADS_IDX];
    }

    public int getNThreadStates() {
        return nThreadStates;
    }

    public int getNThreads() {
        return nThreads;
    }

    public long getNUserThreads() {
        return generalMNumbers[MonitoredNumbersResponse.USER_THREADS_IDX];
    }

    public String[] getNewThreadClassNames() {
        return newThreadClassNames;
    }

    public int[] getNewThreadIds() {
        return newThreadIds;
    }

    public String[] getNewThreadNames() {
        return newThreadNames;
    }

    public long getRelativeGCTimeInPerMil() {
        return generalMNumbers[MonitoredNumbersResponse.GC_TIME_IDX];
    }

    public long[] getStateTimestamps() {
        return stateTimestamps;
    }

    public int[] getThreadIds() {
        return threadIds;
    }

    public byte[][] getThreadStates() {
        return threadStates;
    }

    public long getTimestamp() {
        return generalMNumbers[MonitoredNumbersResponse.TIMESTAMP_IDX];
    }

    public long getTotalMemory() {
        return generalMNumbers[MonitoredNumbersResponse.TOTAL_MEMORY_IDX];
    }

    private static void convertToTimeInMillis(final long[] hiResTimeStamp) {
        ProfilingSessionStatus session = TargetAppRunner.getDefault().getProfilingSessionStatus();
        long statupInCounts = session.startupTimeInCounts;
        long startupMillis = session.startupTimeMillis;

        for (int i = 0; i < hiResTimeStamp.length; i++) {
            hiResTimeStamp[i] = startupMillis + ((hiResTimeStamp[i] - statupInCounts) / (1000000000 / 1000L)); // 1 ms has 1000000000/1000 ns
        }
    }

    private void setThreadStates(byte[] packedStates) {
        threadStates = new byte[nThreads][nThreadStates];

        int idx = 0;

        for (int i = 0; i < nThreads; i++) {
            System.arraycopy(packedStates, idx, threadStates[i], 0, nThreadStates);
            idx += nThreadStates;
        }
    }

    /** Debugging support */
    private void print() {
        for (int i = 0; i < nThreads; i++) {
            System.err.print("id = ");
            System.err.print(threadIds[i]); // NOI18N
            System.err.print(", states = "); // NOI18N

            for (int j = 0; j < nThreadStates; j++) {
                System.err.print(threadStates[i][j]);
            }

            System.err.println();
        }

        if (nNewThreads > 0) {
            System.err.println("New threads added: " + nNewThreads); // NOI18N

            for (int i = 0; i < nNewThreads; i++) {
                System.err.println("  id = " + newThreadIds[i] + ", name = " + newThreadNames[i] + ", classname = "
                                   + newThreadClassNames[i]); // NOI18N
            }
        }

        System.err.println();
    }
}
