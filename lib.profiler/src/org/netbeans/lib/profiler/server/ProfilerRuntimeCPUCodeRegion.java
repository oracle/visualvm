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

import org.netbeans.lib.profiler.server.system.Timers;


/**
 * Functionality for single code region profiling.
 *
 * @author Misha Dmitriev
 */
public class ProfilerRuntimeCPUCodeRegion extends ProfilerRuntime {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    protected static long[] codeRegionResults = new long[0];
    protected static int newBufSize;
    protected static int bufSize;
    protected static int curIdx;
    protected static long invCount;
    protected static boolean codeRegionInstrumentationDisabled;

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static void setCPUResBufSize(int size) {
        newBufSize = size; // We don't set bufSize immediately to prevent the collector from crash if it's already active
    }

    public static long[] getProfilingResults() {
        synchronized (codeRegionResults) {
            int size = (invCount < bufSize) ? (int) invCount : bufSize;
            size++; // The very first element of the returned array is the total number of invocations

            long[] res = new long[size];
            res[0] = (int) invCount;

            if (invCount < bufSize) {
                System.arraycopy(codeRegionResults, 0, res, 1, (int) invCount);
            } else {
                System.arraycopy(codeRegionResults, curIdx, res, 1, bufSize - curIdx);
                System.arraycopy(codeRegionResults, 0, res, bufSize - curIdx + 1, curIdx);
            }

            return res;
        }
    }

    public static void codeRegionEntry() {
        //System.out.println("+++++++++ codeRegionEntry " + Thread.currentThread());
        if (codeRegionInstrumentationDisabled) {
            return;
        }

        ThreadInfo ti = ThreadInfo.getThreadInfo();

        if (!ti.isInitialized()) {
            ti.initialize();
        }

        ti.inCallGraph = true;
        ti.absEntryTime = Timers.getCurrentTimeInCounts();
    }

    public static void codeRegionExit() {
        if (codeRegionInstrumentationDisabled) {
            return;
        }

        long time = Timers.getCurrentTimeInCounts();

        //System.out.println("--------- codeRegionExit " + Thread.currentThread());
        ThreadInfo ti = ThreadInfo.getThreadInfo();

        if (!ti.isInitialized() || !ti.inCallGraph) {
            return;
        }

        time = time - ti.absEntryTime;

        synchronized (codeRegionResults) {
            codeRegionResults[curIdx++] = time;
            invCount++;

            if (curIdx == bufSize) {
                curIdx = 0;
            }
        }

        ti.inCallGraph = false;
    }

    public static void enableProfiling(boolean v) {
        if (v) {
            createNewDataStructures();
            codeRegionInstrumentationDisabled = false;
        } else {
            codeRegionInstrumentationDisabled = true;
            clearDataStructures();
        }
    }

    public static void resetProfilerCollectors() {
        synchronized (codeRegionResults) {
            bufSize = newBufSize;
            codeRegionResults = new long[bufSize];
            invCount = 0;
            curIdx = 0;
        }
    }

    protected static void clearDataStructures() {
        ProfilerRuntime.clearDataStructures();

        synchronized (codeRegionResults) {
            codeRegionResults = new long[0];
            invCount = 0;
            curIdx = 0;
        }
    }

    // ---------------------------------- Handling wait/sleep/monitor times ----------------------------
    protected static void monitorEntryRegion(Thread t, Object monitor) {
        if (codeRegionInstrumentationDisabled) {
            return;
        }

        ThreadInfo ti = ThreadInfo.getThreadInfo();

        if (!ti.isInitialized() || !ti.inCallGraph) {
            return;
        }

        // take note of the time we started waiting
        ti.lastWaitStartTime = Timers.getCurrentTimeInCounts();
    }

    protected static void monitorExitRegion(Thread t, Object monitor) {
        if (codeRegionInstrumentationDisabled) {
            return;
        }

        ThreadInfo ti = ThreadInfo.getThreadInfo();

        if ((ti == null) || !ti.inCallGraph) {
            return;
        }

        // adjust the entry time so that the time spent waiting is not accounted for
        ti.absEntryTime += (Timers.getCurrentTimeInCounts() - ti.lastWaitStartTime);
    }

    protected static void sleepEntryRegion() {
        if (codeRegionInstrumentationDisabled) {
            return;
        }

        ThreadInfo ti = ThreadInfo.getThreadInfo();

        if (!ti.isInitialized() || !ti.inCallGraph) {
            return;
        }

        // take note of the time we started waiting
        ti.lastWaitStartTime = Timers.getCurrentTimeInCounts();
    }

    protected static void sleepExitRegion() {
        if (codeRegionInstrumentationDisabled) {
            return;
        }

        ThreadInfo ti = ThreadInfo.getThreadInfo();

        if (!ti.isInitialized() || !ti.inCallGraph) {
            return;
        }

        // adjust the entry time so that the time spent waiting is not accounted for
        ti.absEntryTime += (Timers.getCurrentTimeInCounts() - ti.lastWaitStartTime);
    }

    protected static void waitEntryRegion() {
        if (codeRegionInstrumentationDisabled) {
            return;
        }

        ThreadInfo ti = ThreadInfo.getThreadInfo();

        if (!ti.isInitialized() || !ti.inCallGraph) {
            return;
        }

        // take note of the time we started waiting
        ti.lastWaitStartTime = Timers.getCurrentTimeInCounts();
    }

    protected static void waitExitRegion() {
        if (codeRegionInstrumentationDisabled) {
            return;
        }

        ThreadInfo ti = ThreadInfo.getThreadInfo();

        if (!ti.isInitialized() || !ti.inCallGraph) {
            return;
        }

        // adjust the entry time so that the time spent waiting is not accounted for
        ti.absEntryTime += (Timers.getCurrentTimeInCounts() - ti.lastWaitStartTime);
    }
}
