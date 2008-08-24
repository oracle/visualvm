/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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

import org.netbeans.lib.profiler.global.CalibrationDataFileIO;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.global.Platform;
import org.netbeans.lib.profiler.global.ProfilingSessionStatus;
import org.netbeans.lib.profiler.server.system.Timers;
import org.netbeans.lib.profiler.wireprotocol.InternalStatsResponse;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ResourceBundle;


/**
 * Functionality that allows one to measure in advance the time that standard instrumentation takes.
 * These values can then be subtracted from the rough profiling results, giving measurements that are
 * much more close to reality.
 *
 * @author Misha Dmitriev
 * @author Ian Formanek
 */
class ProfilerCalibrator extends ProfilerRuntime {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    // !!! Warning - do not use ResourceBundle.getBundle here, won't work in context of direct/dynamic attach !!!
    // Default EN messages initialized here, will be replaced by localized messages in static initializer
    private static String CANNOT_SAVE_CALIBRATION_DATA_MSG = "Performed calibration successfully, but could not save calibration data:\n{0}"; // NOI18N
    private static String CALIBRATION_SUCCESS_MSG = "Calibration performed successfully"; // NOI18N
    private static String CALIBRATION_RESULTS_PREFIX = "For your reference, obtained results are as follows:"; // NOI18N
    private static String CALIBRATION_RESULTS_MSG = "Approximate time in one methodEntry()/methodExit() call pair:\nWhen getting absolute timestamp only: {0} microseconds\nWhen getting thread CPU timestamp only: {1} microseconds\nWhen getting both timestamps: {2} microseconds\n\nApproximate time in one methodEntry()/methodExit() call pair\nin sampled instrumentation mode: {3} microseconds\n"; // NOI18N
    private static String STARTING_CALIBRATION_MSG = "Starting calibration..."; // NOI18N
    private static String TIMER_COUNTS_MSG = "*** timerCountsInSecond = {0}"; // NOI18N
    private static String TIMER_VALUE_MSG = "*** sample value returned by timer = {0}"; // NOI18N
    private static String INJECTION_CALIBRATION_MSG = "----------- Injected profiler code calibration -----------"; // NOI18N
    private static String TIME_getCurrentTimeInCounts_MSG = "Time per each getCurrentTimeInCounts() call"; // NOI18N
    private static String TIME_getThreadCPUTimeInNanos_MSG = "Time per each getThreadCPUTimeInNanos() call"; // NOI18N
    private static String TIME_COUNTS_MCS_MSG = "{0} counts, {1} mcs"; // NOI18N
    private static String TIME_SUCCESS_PAIRS_MSG = "Time per each successful methodEntry()/methodExit() pair of calls ({0}, {1})"; // NOI18N
    private static String MINIMUM_TIME_MSG = "Minimum time: {0} counts, or {1} mcs."; // NOI18N
    private static String INNER_OUTER_TIME_MSG = "Inner/outer time for a successful methodEntry()/methodExit() pair of calls"; // NOI18N
    private static String INNER_TIME_MCS_MSG = "Inner time: {0} mcs."; // NOI18N
    private static String OUTER_TIME_MCS_MSG = "Outer time: {0} mcs."; // NOI18N
    private static String SAMPLED_TIME_MSG = "Time per each sampled instrumentation methodEntry()/methodExit() pair of calls"; // NOI18N
    private static String REGION_TIME_MSG = "Time per each codeRegionEntry()/codeRegionExit() pair of calls"; // NOI18N
                                                                                                              // -----

    static {
        ResourceBundle messages = ProfilerServer.getProfilerServerResourceBundle();

        if (messages != null) {
            CANNOT_SAVE_CALIBRATION_DATA_MSG = messages.getString("ProfilerCalibrator_CannotSaveCalibrationDataMsg"); // NOI18N
            CALIBRATION_SUCCESS_MSG = messages.getString("ProfilerCalibrator_CalibrationSuccessMsg"); // NOI18N
            CALIBRATION_RESULTS_PREFIX = messages.getString("ProfilerCalibrator_CalibrationResultsPrefix"); // NOI18N
            CALIBRATION_RESULTS_MSG = messages.getString("ProfilerCalibrator_CalibrationResultsMsg"); // NOI18N
            STARTING_CALIBRATION_MSG = messages.getString("ProfilerCalibrator_StartingCalibrationMsg"); // NOI18N
            TIMER_COUNTS_MSG = messages.getString("ProfilerCalibrator_TimerCountsMsg"); // NOI18N
            TIMER_VALUE_MSG = messages.getString("ProfilerCalibrator_TimerValueMsg"); // NOI18N
            INJECTION_CALIBRATION_MSG = messages.getString("ProfilerCalibrator_InjectionCalibrationMsg"); // NOI18N
            TIME_getCurrentTimeInCounts_MSG = messages.getString("ProfilerCalibrator_TimeGetCurrentTimeInCountsMsg"); // NOI18N
            TIME_getThreadCPUTimeInNanos_MSG = messages.getString("ProfilerCalibrator_TimeGetThreadCPUTimeInNanosMsg"); // NOI18N
            TIME_COUNTS_MCS_MSG = messages.getString("ProfilerCalibrator_TimeCountsMcsMsg"); // NOI18N
            TIME_SUCCESS_PAIRS_MSG = messages.getString("ProfilerCalibrator_TimeSuccessPairsMsg"); // NOI18N
            MINIMUM_TIME_MSG = messages.getString("ProfilerCalibrator_MinimumTimeMsg"); // NOI18N
            INNER_OUTER_TIME_MSG = messages.getString("ProfilerCalibrator_InnerOuterTimeMsg"); // NOI18N
            INNER_TIME_MCS_MSG = messages.getString("ProfilerCalibrator_InnerTimeMcsMsg"); // NOI18N
            OUTER_TIME_MCS_MSG = messages.getString("ProfilerCalibrator_OuterTimeMcsMsg"); // NOI18N
            SAMPLED_TIME_MSG = messages.getString("ProfilerCalibrator_SampledTimeMsg"); // NOI18N
            REGION_TIME_MSG = messages.getString("ProfilerCalibrator_RegionTimeMsg"); // NOI18N
        }
    }

    private static ProfilingSessionStatus status;
    private static boolean printResults;
    private static byte[] buf;
    private static double minTimePerMethodEntryExitCallInCounts;
    private static double minTimePerMethodEntryExitCallInMCS;
    private static double innerTimeInCounts;
    private static double outerTimeInCounts;
    private static long cntInSecond;
    private static int nCall;
    private static int cycleWhenMinResultDetected;

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    //private static double deviation; private static int nDevSamples;
    public static void init(ProfilingSessionStatus status) {
        ProfilerCalibrator.status = status;
    }

    /**
     * Main method is called in two cases:
     * - when the user wants to perform calibration and save results on the machine where the tool is not installed,
     *   so that later remote profiling can be performed on this machine. No command line args are needed.
     * - a backdoor usage when JF developers want to see calibration data, tune the internals etc. In that case,
     *   a special command line argument -develmode should be provided.
     */
    public static void main(String[] args) {
        boolean inDevelMode;

        if ((args.length > 0) && args[0].equals("-develmode")) { // NOI18N  // Development mode, see above
            inDevelMode = true;
            System.out.println("Loader for this class is " + ProfilerCalibrator.class.getClassLoader()); // NOI18N
        } else {
            inDevelMode = false;

            ProfilingSessionStatus localStatus = new ProfilingSessionStatus();
            localStatus.targetJDKVersionString = Platform.getJDKVersionString();
            localStatus.remoteProfiling = true;
            init(localStatus);
        }

        if (Platform.getJDKVersionString().equals(Platform.JDK_CVM_STRING)) {
            System.loadLibrary("profilerinterface"); // NOI18N
        }
        Timers.initialize();

        measureBCIOverhead(inDevelMode);

        if (!inDevelMode) {
            if (!CalibrationDataFileIO.saveCalibrationData(status)) {
                System.err.println(MessageFormat.format(CANNOT_SAVE_CALIBRATION_DATA_MSG,
                                                        new Object[] { CalibrationDataFileIO.getErrorMessage() })); // NOI18N
                System.exit(-1);
            }

            System.out.println(CALIBRATION_SUCCESS_MSG); // NOI18N
            System.out.println(CALIBRATION_RESULTS_PREFIX); // NOI18N

            NumberFormat nf = NumberFormat.getInstance();
            nf.setMaximumFractionDigits(4);

            long cntsInSec = status.timerCountsInSecond[0];
            double m0 = (((double) status.methodEntryExitCallTime[0]) * 1000000) / cntsInSec; // Expressed in microseconds
            double m1 = (((double) status.methodEntryExitCallTime[1]) * 1000000) / cntsInSec; // Ditto
            double m2 = (((double) status.methodEntryExitCallTime[2]) * 1000000) / cntsInSec; // Ditto
            double m4 = (((double) status.methodEntryExitCallTime[4]) * 1000000) / cntsInSec; // Ditto

            StringBuffer s = new StringBuffer();
            s.append(MessageFormat.format(CALIBRATION_RESULTS_MSG,
                                          new Object[] { nf.format(m0), nf.format(m1), nf.format(m2), nf.format(m4) }));

            System.out.println(s.toString());
        }
    }

    /**
     * In addition to calculating and optionally printing some measurements of the overhead imposed by BCI, this
     * call, I hope,  makes the impact of BCI on the target application more uniform/predictable, since it causes
     * the VM to precompile various profiling calls.
     */
    public static void measureBCIOverhead(boolean printRes) {
        printResults = printRes;

        System.out.println(STARTING_CALIBRATION_MSG);

        // Just wait for some time, to e.g. make caller threads finish their work, etc.
        try {
            Thread.sleep(1000);
        } catch (Exception ex) {
        }

        cntInSecond = Timers.getNoOfCountsInSecond();
        printResults(MessageFormat.format(TIMER_COUNTS_MSG, new Object[] { "" + cntInSecond })); // NOI18N
        printResults(MessageFormat.format(TIMER_VALUE_MSG, new Object[] { "" + Timers.getCurrentTimeInCounts() })); // NOI18N

        if (status != null) {
            status.timerCountsInSecond = new long[2];
            status.timerCountsInSecond[0] = cntInSecond;
            status.timerCountsInSecond[1] = 1000000000;
        }

        ProfilerRuntime.resetProfilerCollectors(INSTR_RECURSIVE_FULL);
        ProfilerRuntimeCPU.setNProfiledThreadsLimit(1);
        ProfilerRuntimeCPU.setInstrMethodsInvoked(new boolean[] { true, true, true, true });
        ProfilerRuntimeCPU.createThreadInfoForCurrentThread();

        printResults(INJECTION_CALIBRATION_MSG + "\n"); // NOI18N

        if (printResults) { // Measure and print the performance of getCurrentTimeInCounts() - for information only
            measureTimerCall(true);
            measureTimerCall(false);
        }

        //deviation = 0; nDevSamples = 0;
        nCall = -1;
        measureMethodEntryExitCalls(); // A warmup run to make the compiler make all necessary inlines etc.

        for (nCall = 0; nCall < 4; nCall++) {
            measureMethodEntryExitCalls(); // This knows what to count depending on nCall

            if (status != null) {
                status.methodEntryExitCallTime[nCall] = minTimePerMethodEntryExitCallInCounts;
                status.methodEntryExitInnerTime[nCall] = innerTimeInCounts;
                status.methodEntryExitOuterTime[nCall] = outerTimeInCounts;
            }
        }

        //deviation /= nDevSamples; System.out.println("**! deviation = " + deviation);

        //if (printResults) {                             // For information purposes only
        //  measureUnsuccessfulMethodEntryExitCalls();  
        //  measureSuccessfulMethodEntryExitCallsCalledFromInterpreted();
        //}
        nCall = -1;
        measureSampledMethodEntryExitCalls(); // Again a warmup run
        nCall = 0;
        measureSampledMethodEntryExitCalls();

        if (status != null) {
            status.methodEntryExitCallTime[4] = minTimePerMethodEntryExitCallInCounts;
            status.methodEntryExitInnerTime[4] = innerTimeInCounts;
            status.methodEntryExitOuterTime[4] = outerTimeInCounts;
        }

        measureCodeRegionCalls();

        printResults("----------------------------------------------------------\n"); // NOI18N

        buf = null;
        ProfilerRuntimeCPUCodeRegion.setCPUResBufSize(0);
        ThreadInfo.setDefaultEvBufParams();
        ProfilerRuntime.resetProfilerCollectors(INSTR_RECURSIVE_FULL);
        ProfilerRuntime.resetProfilerCollectors(INSTR_RECURSIVE_SAMPLED);
    }

    static InternalStatsResponse getInternalStats() {
        InternalStatsResponse r = new InternalStatsResponse();
        r.nTotalInstrMethods = ProfilerInterface.nTotalInstrMethods;
        r.nClassLoads = ProfilerInterface.nClassLoads;
        r.nFirstMethodInvocations = ProfilerInterface.nFirstMethodInvocations;
        r.nNonEmptyInstrMethodGroupResponses = ProfilerInterface.nNonEmptyInstrMethodGroupResponses;
        r.nEmptyInstrMethodGroupResponses = ProfilerInterface.nEmptyInstrMethodGroupResponses;
        r.nSingleMethodInstrMethodGroupResponses = ProfilerInterface.nSingleMethodInstrMethodGroupResponses;

        long cntsInSec = Timers.getNoOfCountsInSecond();
        r.clientInstrTime = (((double) ProfilerInterface.clientInstrTime) * 1000) / cntsInSec;
        r.clientDataProcTime = (((double) ProfilerInterface.clientDataProcTime) * 1000) / cntsInSec;

        if (r.nNonEmptyInstrMethodGroupResponses > 0) {
            r.totalHotswappingTime = (((double) ProfilerInterface.totalHotswappingTime) * 1000) / cntsInSec;
            r.averageHotswappingTime = ((((double) ProfilerInterface.totalHotswappingTime) * 1000) / cntsInSec / ProfilerInterface.nNonEmptyInstrMethodGroupResponses);
            r.minHotswappingTime = (((double) ProfilerInterface.minHotswappingTime) * 1000) / cntsInSec;
            r.maxHotswappingTime = (((double) ProfilerInterface.maxHotswappingTime) * 1000) / cntsInSec;
        }

        r.methodEntryExitCallTime0 = (((double) status.methodEntryExitCallTime[0]) * 1000000) / cntsInSec; // Expressed in microseconds
        r.methodEntryExitCallTime1 = (((double) status.methodEntryExitCallTime[1]) * 1000000) / cntsInSec; // Ditto
        r.methodEntryExitCallTime2 = (((double) status.methodEntryExitCallTime[2]) * 1000000) / cntsInSec; // Expressed in microseconds

        return r;
    }

    static void resetInternalStatsCollectors() {
        ProfilerInterface.totalHotswappingTime = 0;
        ProfilerInterface.clientInstrTime = 0;
        ProfilerInterface.clientDataProcTime = 0;
    }

    private static void measureCodeRegionCalls() {
        printResults("\n" + REGION_TIME_MSG); // NOI18N
        ProfilerRuntimeCPUCodeRegion.setCPUResBufSize(100);
        ProfilerRuntimeCPUCodeRegion.resetProfilerCollectors();

        int noOfInnerIterations = 50;

        for (int i = 0; i < 50; i++) {
            long time = Timers.getCurrentTimeInCounts();

            for (int j = 0; j < noOfInnerIterations; j++) {
                ProfilerRuntimeCPUCodeRegion.codeRegionEntry();
                ProfilerRuntimeCPUCodeRegion.codeRegionExit();
                ProfilerRuntimeCPUCodeRegion.codeRegionEntry();
                ProfilerRuntimeCPUCodeRegion.codeRegionExit();
            }

            time = Timers.getCurrentTimeInCounts() - time;

            if (printResults && ((i % 5) == 0)) {
                double timeInCounts = (double) time / (noOfInnerIterations * 2);
                double timePerMethodInMCS = (((double) time * 1000000) / cntInSecond / (noOfInnerIterations * 2));
                printResults(MessageFormat.format(TIME_COUNTS_MCS_MSG, new Object[] { "" + timeInCounts, "" + timePerMethodInMCS })); // NOI18N
            }
        }
    }

    private static void measureMethodEntryExitCalls() {
        boolean absolute = false;
        boolean threadCPU = false;

        switch (nCall) {
            case -1:
            case 0:
                absolute = true;
                threadCPU = false;

                break;
            case 1:
                absolute = false;
                threadCPU = true;

                break;
            case 2:
            case 3:
                absolute = true;
                threadCPU = true;

                break;
        }

        printResults("\n" + MessageFormat.format(TIME_SUCCESS_PAIRS_MSG, new Object[] { "" + absolute, "" + threadCPU })); // NOI18N

        ProfilerRuntimeCPU.setTimerTypes(absolute, threadCPU);

        // NOTE: this is, unfortunately, simply an experimentally determined number of iterations, that makes HotSpot fully inline
        // the call when the server compiler is used. It would certainly be better to make this algorithm more adaptive, since
        // this way it is likely to be quite sensitive to VM changes... I have also observed that for some reason on the server
        // compiler on Windows, even after the warmup run, i.e. on nCall == 0, the measured call time does not achieve its lowest
        // value until noOfOuterIterations is close to 200. On nCall == 1 etc. there does not seem to be such an issue. I can't
        // explain this phenomenon yet.
        int noOfOuterIterations = 300;
        int noOfInnerIterations = 200;
        int innerIterationBufferSize = (ThreadInfo.MAX_EVENT_SIZE * ((noOfInnerIterations * 4) + 2) * 5) / 4;
        ThreadInfo ti = ThreadInfo.getThreadInfo();

        // Buffer size is calculated as "required" * 5/4, which is a "safety margin"
        if (nCall == -1) {
            buf = new byte[2 * innerIterationBufferSize];
        }

        ti.setEvBuf(buf);
        ProfilerRuntime.eventBuffer = buf;
        ProfilerRuntime.globalEvBufPosThreshold = buf.length; // To prevent event buffer dump triggering

        if (nCall != 3) { // On the last call to this method, we just calculate the new value from the same array contents
            minTimePerMethodEntryExitCallInMCS = 100000.0;
            minTimePerMethodEntryExitCallInCounts = 1000000;
            cycleWhenMinResultDetected = 1;

            for (int i = 0; i < noOfOuterIterations; i++) {
                ti.evBufPos = ((cycleWhenMinResultDetected + 1) % 2) * innerIterationBufferSize;
                ProfilerRuntimeCPUFullInstr.rootMethodEntry((char) 1);

                long time = Timers.getCurrentTimeInCounts();

                for (int j = 0; j < noOfInnerIterations; j++) {
                    ProfilerRuntimeCPUFullInstr.methodEntry((char) 2);
                    ProfilerRuntimeCPUFullInstr.methodExit((char) 2);
                    ProfilerRuntimeCPUFullInstr.methodEntry((char) 3);
                    ProfilerRuntimeCPUFullInstr.methodExit((char) 3);
                }

                time = Timers.getCurrentTimeInCounts() - time;
                ProfilerRuntimeCPUFullInstr.methodExit((char) 1);

                double timeInCounts = (double) time / (noOfInnerIterations * 2);
                double timeInMCS = (((double) time * 1000000) / cntInSecond / (noOfInnerIterations * 2));

                // Calculate the deviation
                //if (i > 10 && !(nCall == 0 && i < (noOfOuterIterations - 100))) {
                //  deviation += (double) Math.abs(timeInCounts - minTimePerMethodEntryExitCallInCounts) / minTimePerMethodEntryExitCallInCounts;
                //  nDevSamples++;
                //}
                if (timeInCounts < minTimePerMethodEntryExitCallInCounts) {
                    minTimePerMethodEntryExitCallInCounts = timeInCounts;
                    minTimePerMethodEntryExitCallInMCS = timeInMCS;
                    cycleWhenMinResultDetected = ((cycleWhenMinResultDetected + 1) % 2);
                }

                if (printResults && ((i % 5) == 0)) {
                    printResults(MessageFormat.format(TIME_COUNTS_MCS_MSG, new Object[] { "" + timeInCounts, "" + timeInMCS })); // NOI18N
                }
            }

            printResults(MessageFormat.format(MINIMUM_TIME_MSG,
                                              new Object[] {
                                                  "" + minTimePerMethodEntryExitCallInCounts,
                                                  "" + minTimePerMethodEntryExitCallInMCS
                                              })); // NOI18N
        }

        // Now calculate the ratio of time spent in the inner part of methodEntry-methodExit pair to the time
        // spent in the outer part of this pair.
        printResults("\n" + INNER_OUTER_TIME_MSG); // NOI18N

        if ((nCall != 1) && (nCall != 3)) {
            // High-precision calculation of inner and outer time when really hi-res timer is used.
            int recordSize = 1 + 2 + 7 + ((nCall >= 2) ? 7 : 0); // Event type, method id, and 1 or 2 7-byte timestamps
            int curPos = (cycleWhenMinResultDetected * innerIterationBufferSize) + (1 * recordSize);

            // Take into account that when only one timestamp per standard event is collected, ROOT_ENTRY and ROOT_EXIT
            // still generate two timestamps
            if (nCall < 2) {
                curPos += 7;
            }

            int totalCalls = noOfInnerIterations * 4;
            long innerTime = 0;
            long outerTime = 0;
            long prevTimeStamp = 0;
            boolean inner = false;
            int prefixEls = 1 + 2 + ((nCall == 3) ? 7 : 0); // Event type, method id, possible second time stamp
            int suffixEls = ((nCall == 2) ? 7 : 0); // Possible second time stamp

            for (int i = 0; i < totalCalls; i++) {
                byte eventType = buf[curPos];

                // Now that the implementation is stable, there is not much need in the following checks
                if (inner && (eventType != CommonConstants.METHOD_EXIT)) {
                    System.out.println("Problem with inner! " + (int) eventType + ", curPos = " + curPos); // NOI18N
                } else if (!inner && (eventType != CommonConstants.METHOD_ENTRY)) {
                    System.out.println("Problem with outer! " + (int) eventType + ", curPos = " + curPos); // NOI18N
                }

                curPos += prefixEls; // Omit event type, method id, and, possibly, non-relevant timestamp

                long timeStamp = (((long) buf[curPos++] & 0xFF) << 48) | (((long) buf[curPos++] & 0xFF) << 40)
                                 | (((long) buf[curPos++] & 0xFF) << 32) | (((long) buf[curPos++] & 0xFF) << 24)
                                 | (((long) buf[curPos++] & 0xFF) << 16) | (((long) buf[curPos++] & 0xFF) << 8)
                                 | ((long) buf[curPos++] & 0xFF);
                long time = (i > 0) ? (timeStamp - prevTimeStamp) : 0;

                if (inner) {
                    innerTime += time;
                } else {
                    outerTime += time;
                }

                inner = !inner;
                prevTimeStamp = timeStamp;
                curPos += suffixEls;
            }

            innerTimeInCounts = ((double) innerTime) / (totalCalls / 2);
            outerTimeInCounts = ((double) outerTime) / ((totalCalls / 2) - 1);
        } else {
            // Both on Windows and Solaris, thread-local time is extremely low-resolution (10 ms and 1 ms),
            // thus our standard high-precision method won't work. Rough calculation is used instead.
            // Can we (and does it make sense to) do better?
            innerTimeInCounts = outerTimeInCounts = minTimePerMethodEntryExitCallInCounts / 2;
        }

        double innerTimeInMCS = ((innerTimeInCounts * 1000000) / cntInSecond);
        double outerTimeInMCS = ((outerTimeInCounts * 1000000) / cntInSecond);
        printResults(MessageFormat.format(INNER_TIME_MCS_MSG, new Object[] { "" + innerTimeInMCS })); // NOI18N
        printResults(MessageFormat.format(OUTER_TIME_MCS_MSG, new Object[] { "" + outerTimeInMCS })); // NOI18N
    }

    private static void measureSampledMethodEntryExitCalls() {
        printResults("\n" + SAMPLED_TIME_MSG); // NOI18N
                                               //ProfilerRuntime.resetProfilerCollectors(INSTR_RECURSIVE_SAMPLED);  THIS IS NOT NEEDED - will delete our current ThreadInfo

        int noOfOuterIterations = (nCall == -1) ? 200 : 80; // Large number to make the server compiler inline everything
        int noOfInnerIterations = 200;
        ThreadInfo ti = ThreadInfo.getThreadInfo();
        ti.setEvBuf(buf);

        minTimePerMethodEntryExitCallInMCS = 100000.0;
        minTimePerMethodEntryExitCallInCounts = 1000000;

        for (int i = 0; i < noOfOuterIterations; i++) {
            ti.evBufPos = 0;
            ProfilerRuntimeCPUSampledInstr.rootMethodEntry((char) 1);

            long time = Timers.getCurrentTimeInCounts();

            for (int j = 0; j < noOfInnerIterations; j++) {
                ProfilerRuntimeCPUSampledInstr.methodEntry((char) 2);
                ProfilerRuntimeCPUSampledInstr.methodExit((char) 2);
                ProfilerRuntimeCPUSampledInstr.methodEntry((char) 3);
                ProfilerRuntimeCPUSampledInstr.methodExit((char) 3);
                ProfilerRuntimeCPUSampledInstr.methodEntry((char) 2);
                ProfilerRuntimeCPUSampledInstr.methodExit((char) 2);
                ProfilerRuntimeCPUSampledInstr.methodEntry((char) 3);
                ProfilerRuntimeCPUSampledInstr.methodExit((char) 3);
            }

            time = Timers.getCurrentTimeInCounts() - time;
            ProfilerRuntimeCPUSampledInstr.methodExit((char) 1);

            double timeInCounts = (double) time / (noOfInnerIterations * 4);
            double timeInMCS = (((double) time * 1000000) / cntInSecond / (noOfInnerIterations * 4));

            if (timeInCounts < minTimePerMethodEntryExitCallInCounts) {
                minTimePerMethodEntryExitCallInCounts = timeInCounts;
                minTimePerMethodEntryExitCallInMCS = timeInMCS;
            }

            if (printResults && ((i % 5) == 0)) {
                System.out.println(MessageFormat.format(TIME_COUNTS_MCS_MSG, new Object[] { "" + timeInCounts, "" + timeInMCS })); // NOI18N
            }
        }

        innerTimeInCounts = outerTimeInCounts = minTimePerMethodEntryExitCallInCounts / 2;
    }

    private static void measureTimerCall(boolean absolute) {
        if (absolute) {
            printResults(TIME_getCurrentTimeInCounts_MSG); // NOI18N
        } else {
            printResults(TIME_getThreadCPUTimeInNanos_MSG); // NOI18N
        }

        int noOfInnerIterations = 1000;
        long res;

        for (int i = 0; i < 50; i++) {
            long time = Timers.getCurrentTimeInCounts();

            for (int j = 0; j < noOfInnerIterations; j++) {
                if (absolute) {
                    res = Timers.getCurrentTimeInCounts();
                } else {
                    res = Timers.getThreadCPUTimeInNanos();
                }
            }

            time = Timers.getCurrentTimeInCounts() - time;

            if (printResults && ((i % 5) == 0)) {
                double timeInCounts = (double) time / (noOfInnerIterations + 2);
                double timePerMethodInMCS = (((double) time * 1000000) / cntInSecond / (noOfInnerIterations + 2));
                printResults(MessageFormat.format(TIME_COUNTS_MCS_MSG, new Object[] { "" + timeInCounts, "" + timePerMethodInMCS })); // NOI18N
            }
        }
    }

    private static void printResults(String str) {
        if (printResults) {
            System.out.println(str);
        }
    }

    /** The following two methods can be used for info purposes if needed */
    /*private static void measureUnsuccessfulMethodEntryExitCalls() {
       printResults("\nTime per each unsuccessful methodEntry()/methodExit() pair of calls");
       ProfilerRuntime.resetProfilerCollectors(INSTR_RECURSIVE);
       int noOfInnerIterations = 50;
       for (int i = 0; i < 15; i++) {
         long time = Timers.getCurrentTimeInCounts();
         for (int j = 0; j < noOfInnerIterations; j++) {
           ProfilerRuntimeCPUFullInstr.methodEntry((char) 2);
           ProfilerRuntimeCPUFullInstr.methodExit((char) 2);
           ProfilerRuntimeCPUFullInstr.methodEntry((char) 3);
           ProfilerRuntimeCPUFullInstr.methodExit((char) 3);
         }
         time = Timers.getCurrentTimeInCounts() - time;
         if (printResults && i % 5 == 0) {
           double timeInCounts = (double) time / (noOfInnerIterations * 2);
           double timePerMethodInMCS = ((double) time * 1000000 / cntInSecond / (noOfInnerIterations * 2));
           System.out.println(timeInCounts + " counts,  " + timePerMethodInMCS + " mcs");
         }
       }
       }
       private static void measureSuccessfulMethodEntryExitCallsCalledFromInterpreted(boolean printResults) {
         printResults("\nTime per successful methodEntry()/methodExit() pair when called from interpreted code");
         ProfilerRuntime.resetProfilerCollectors(INSTR_RECURSIVE);
         ProfilerRuntimeCPU.createThreadInfoForThread(Thread.currentThread());
         int noOfInnerIterations = 50;
         for (int i = 0; i < 15; i++) {
           ProfilerRuntimeCPUFullInstr.rootMethodEntry((char) 1);
           long time = Timers.getCurrentTimeInCounts();
           for (int j = 0; j < noOfInnerIterations; j++) {
             ProfilerRuntimeCPUFullInstr.methodEntry((char) 2);
             ProfilerRuntimeCPUFullInstr.methodExit((char) 2);
             ProfilerRuntimeCPUFullInstr.methodEntry((char) 3);
             ProfilerRuntimeCPUFullInstr.methodExit((char) 3);
           }
           time = Timers.getCurrentTimeInCounts() - time;
           ProfilerRuntimeCPUFullInstr.methodExit((char) 1);
           if (printResults && i % 5 == 0) {
             double timeInCounts = (double) time / (noOfInnerIterations * 2);
             double timePerMethodInMCS = ((double) time * 1000000 / cntInSecond / (noOfInnerIterations * 2));
             System.out.println(timeInCounts + " counts,  " + timePerMethodInMCS + " mcs");
           }
         }
       }  ***/
}
