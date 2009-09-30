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

package org.netbeans.lib.profiler;

import org.netbeans.lib.profiler.client.AppStatusHandler;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.client.ProfilingPointsProcessor;
import org.netbeans.lib.profiler.global.CalibrationDataFileIO;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.global.Platform;
import org.netbeans.lib.profiler.global.ProfilingSessionStatus;
import org.netbeans.lib.profiler.results.EventBufferProcessor;
import org.netbeans.lib.profiler.results.cpu.CPUCCTContainer;
import org.netbeans.lib.profiler.utils.MiscUtils;
import org.netbeans.lib.profiler.wireprotocol.AsyncMessageCommand;
import org.netbeans.lib.profiler.wireprotocol.Command;
import org.netbeans.lib.profiler.wireprotocol.InternalStatsResponse;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.Vector;


/**
 * Functionality for high-level control of the Target Application (TA) execution, plus some utility methods
 * that seemed to fit best here.
 *
 * @author Tomas Hurka
 * @author Misha Dmitriev
 * @author Ian Formanek
 */
public class TargetAppRunner implements CommonConstants {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.netbeans.lib.profiler.Bundle"); // NOI18N
    private static final String CLASSPATH_SETTINGS_IGNORED_MSG = messages.getString("TargetAppRunner_ClasspathSettingsIgnoredMsg"); // NOI18N
    private static final String ERROR_STARTING_JVM_MSG = messages.getString("TargetAppRunner_ErrorStartingJvmMsg"); // NOI18N
    private static final String CALIBRATION_SUMMARY_SHORT_MSG = messages.getString("TargetAppRunner_CalibrationSummaryShortMsg"); // NOI18N
    private static final String CALIBRATION_SUMMARY_DETAILS_MSG = messages.getString("TargetAppRunner_CalibrationSummaryDetailsMsg"); // NOI18N
    private static final String FAILED_ESTABLISH_CONN_MSG = messages.getString("TargetAppRunner_FailedEstablishConnMsg"); // NOI18N
    private static final String UNEXPECTED_PROBLEM_STARTING_APP_MSG = messages.getString("TargetAppRunner_UnexpectedProblemStartingAppMsg"); // NOI18N
    private static final String JVM_TERMINATED_NOTRESPOND_STRING = messages.getString("TargetAppRunner_JvmTerminatedNotRespondString"); // NOI18N
    private static final String INTERNAL_PROBLEM_STRING = messages.getString("TargetAppRunner_InternalProblemString"); // NOI18N
    private static final String FAILED_START_APP_CAUSE_MSG = messages.getString("TargetAppRunner_FailedStartAppCauseMsg"); // NOI18N
    private static final String CALIBRATION_RESULTS_MSG = messages.getString("TargetAppRunner_CalibrationResultsMsg"); // NOI18N
    private static final String CALIBRATION_ERROR_MSG = messages.getString("TargetAppRunner_CalibrationErrorMsg"); // NOI18N
    private static final String INTERNAL_STATISTICS_ONLY_MSG = messages.getString("TargetAppRunner_InternalStatisticsOnlyMsg"); // NOI18N
    private static final String INSTR_METHODS_COUNT_MSG = messages.getString("TargetAppRunner_InstrMethodsCountMsg"); // NOI18N
    private static final String CLASSLOAD_FIRSTINV_COUNT_MSG = messages.getString("TargetAppRunner_ClassLoadFirstInvCountMsg"); // NOI18N
    private static final String NON_EMPTY_IMG_COUNT_MSG = messages.getString("TargetAppRunner_NonEmptyImgCountMsg"); // NOI18N
    private static final String EMPTY_IMG_COUNT_MSG = messages.getString("TargetAppRunner_EmptyImgCountMsg"); // NOI18N
    private static final String SINGLE_IMG_COUNT_MSG = messages.getString("TargetAppRunner_SingleImgCountMsg"); // NOI18N
    private static final String AVG_METHOD_TIME_MSG = messages.getString("TargetAppRunner_AvgMethodTimeMsg"); // NOI18N
    private static final String MIN_METHOD_TIME_MSG = messages.getString("TargetAppRunner_MinMethodTimeMsg"); // NOI18N
    private static final String MAX_METHOD_TIME_MSG = messages.getString("TargetAppRunner_MaxMethodTimeMsg"); // NOI18N
    private static final String TOTAL_RUN_TIME_MSG = messages.getString("TargetAppRunner_TotalRunTimeMsg"); // NOI18N
    private static final String INJ_INSTR_TIME_MSG = messages.getString("TargetAppRunner_InjInstrTimeMsg"); // NOI18N
    private static final String TOTAL_INSTR_HOTSWAP_TIME_MSG = messages.getString("TargetAppRunner_TotalInstrHotSwapTimeMsg"); // NOI18N
    private static final String BYTECODE_COMM_TIME_MSG = messages.getString("TargetAppRunner_ByteCodeCommTimeMsg"); // NOI18N
    private static final String CLIENT_BYTECODE_TIME_MSG = messages.getString("TargetAppRunner_ClientByteCodeTimeMsg"); // NOI18N
    private static final String CLIENT_DISK_PROCESS_MSG = messages.getString("TargetAppRunner_ClientDiskProcessTimeMsg"); // NOI18N
    private static final String CLIENT_RESULTS_PROCESS_MSG = messages.getString("TargetAppRunner_ClientResultsProcessTimeMsg"); // NOI18N
    private static final String PERFORMING_CALIBRATION_MSG = messages.getString("TargetAppRunner_PerformingCalibrationMsg"); // NOI18N
                                                                                                                             // -----
    private static final boolean DEBUG = System.getProperty("org.netbeans.lib.profiler.TargetAppRunner") != null; // NOI18N
    private static TargetAppRunner defaultTAR; // Ok only while we don't have multiple profiling sessions
    private static final int EVENT_STARTED = 0;
    private static final int EVENT_STOPPED = 1;
    private static final int EVENT_SUSPENDED = 2;
    private static final int EVENT_RESUMED = 3;
    private static final int EVENT_ATTACHED = 4;
    private static final int EVENT_TERMINATED = 5;
    private static final int EVENT_DETACHED = 6;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    // Required for dialog shown during calibration
    private AppStatusHandler.AsyncDialog waitDialog;
    private AppStatusHandler appStatusHandler;
    private Process runningAppProcess;
    private ProfilerClient profilerClient;
    private ProfilerEngineSettings settings;
    private ProfilingPointsProcessor profilingPointProcessor;
    private ProfilingSessionStatus status;
    private Vector listeners = new Vector();
    private boolean targetAppIsSuspended;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public TargetAppRunner(ProfilerEngineSettings settings, AppStatusHandler ash, ProfilingPointsProcessor ppp) {
        this.settings = settings;
        status = new ProfilingSessionStatus();
        appStatusHandler = ash;
        profilingPointProcessor = ppp;

        profilerClient = new ProfilerClient(settings, status, appStatusHandler,
                                            new AppStatusHandler.ServerCommandHandler() {
                public void handleServerCommand(Command cmd) {
                    if (cmd != null) {
                        if (cmd.getType() == Command.MESSAGE) {
                            AsyncMessageCommand msg = (AsyncMessageCommand) cmd;

                            if (msg.isPositive()) {
                                appStatusHandler.displayNotification(msg.getMessage());
                            } else {
                                appStatusHandler.displayError(msg.getMessage());

                                //profilerClient.setCurrentInstrType(INSTR_NONE);
                                // It looks like it often makes more sense to ignore the problem and get at least some info...
                            }
                        } else if (cmd.getType() == Command.RESULTS_AVAILABLE) {
                            appStatusHandler.resultsAvailable();
                        } else if (cmd.getType() == Command.TAKE_SNAPSHOT) {
                            appStatusHandler.takeSnapshot();
                        }
                    }
                }
            });
        defaultTAR = this;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static TargetAppRunner getDefault() {
        return defaultTAR;
    }

    public AppStatusHandler getAppStatusHandler() {
        return appStatusHandler;
    }

    public String getInternalStats() throws ClientUtils.TargetAppOrVMTerminated {
        InternalStatsResponse stats = (status.savedInternalStats != null) ? status.savedInternalStats
                                                                          : profilerClient.getInternalStats();

        return getInternalStatsText(stats);
    }

    public ProfilerClient getProfilerClient() {
        return profilerClient;
    }

    public ProfilerEngineSettings getProfilerEngineSettings() {
        return settings;
    }

    public ProfilingPointsProcessor getProfilingPointsProcessor() {
        return profilingPointProcessor;
    }

    public ProfilingSessionStatus getProfilingSessionStatus() {
        return status;
    }

    public Process getRunningAppProcess() {
        return runningAppProcess;
    }

    public void addProfilingEventListener(ProfilingEventListener profilingEventListener) {
        listeners.add(profilingEventListener);
    }

    /**
     * Attaches to a running application. It is expected that prepareForAttach method is called before this one
     * to prepare the target app environment for attaching.
     *
     * @param tvmDir The working directory for the target application
     * @throws IOException in case sending signal to target app failed
     */
    public boolean attachToTargetJVM(File tvmDir) throws IOException {
        if (connectToStartedVMAndStartTA(1, false)) {
            status.runningInAttachedMode = true;
            notifyListeners(EVENT_ATTACHED);

            return true;
        } else {
            return false;
        }
    }

    public boolean attachToTargetVM() {
        if (connectToStartedVMAndStartTA(1, false)) {
            status.runningInAttachedMode = true;
            notifyListeners(EVENT_ATTACHED);

            return true;
        } else {
            return false;
        }
    }

    /**
     * Attach to the started and waiting target JVM using the "attach on startup" method
     */
    public boolean attachToTargetVMOnStartup() {
        if (connectToStartedVMAndStartTA(2, false)) {
            status.runningInAttachedMode = true;
            notifyListeners(EVENT_ATTACHED);

            return true;
        } else {
            return false;
        }
    }

    /**
     * This call runs the target JVM instance just to calibrate (that is, measure the time it takes to execute)
     * the instrumentation code that we then inject into target app code. The results are saved to be reused
     * in subsequent runs.
     */
    public boolean calibrateInstrumentationCode() {
        status.targetJDKVersionString = settings.getTargetJDKVersionString();

        waitDialog = appStatusHandler.getAsyncDialogInstance(PERFORMING_CALIBRATION_MSG, false, false);
        waitDialog.display();

        boolean res = false;

        try {
            if (!runJVMToCalibrateInstrumentation()) {
                return false;
            }

            res = CalibrationDataFileIO.saveCalibrationData(status);

            return true;
        } finally {
            if (waitDialog != null) {
                waitDialog.close();
            }

            if (res) {
                StringBuffer s = new StringBuffer();
                s.append(CALIBRATION_SUMMARY_DETAILS_MSG);
                appendCalibrationData(s);
                appStatusHandler.displayNotificationWithDetailsAndWaitForConfirm(CALIBRATION_SUMMARY_SHORT_MSG, s.toString());
            } else {
                appStatusHandler.displayErrorAndWaitForConfirm(CalibrationDataFileIO.getErrorMessage());
            }
        }
    }

    /**
     * Connects to the target JVM started using startTargetVM(), and starts the target application.
     * Error reporting happens in the same way as in runTargetApp().
     */
    public boolean connectToStartedVMAndStartTA() {
        return connectToStartedVMAndStartTA(false);
    }

    public void detachFromTargetJVM() {
        if (targetAppIsSuspended) {
            try {
                profilerClient.resumeTargetAppThreads();
            } catch (ClientUtils.TargetAppOrVMTerminated ex) {
            }

            targetAppIsSuspended = false;
        }

        try {
            profilerClient.detachFromTargetJVM();
            notifyListeners(EVENT_DETACHED);
        } catch (ClientUtils.TargetAppOrVMTerminated ex) { /* No need to say anything if it's already terminated */
        }

        targetAppIsSuspended = false;
    }

    public boolean hasSupportedJDKForHeapDump() {
        // not supported for JDK other than 1.6 & 1.7 & 1.5.0_12 and up
        String jdkVersion = getProfilerEngineSettings().getTargetJDKVersionString();

        if (CommonConstants.JDK_16_STRING.equals(jdkVersion) || CommonConstants.JDK_17_STRING.equals(jdkVersion)) {
            return true;
        }

        if (CommonConstants.JDK_15_STRING.equals(jdkVersion)) {
            String fullJDKString = getProfilingSessionStatus().fullTargetJDKVersionString;
            int minorVersion = Platform.getJDKMinorNumber(fullJDKString);

            if (minorVersion >= 12) {
                return true;
            }
        }

        return false;
    }

    /**
     *  Initiates profiling session
     * @param attachMode 0 = no attach; 1 = direct; 2 = dynamic
     * @param calibrationOnlyRun
     * @return Returns TRUE if the connection to the profiler agent has been successfuly established
     */
    public boolean initiateSession(int attachMode, boolean calibrationOnlyRun) {
        if (targetJVMIsAlive()) {
            return true;
        }

        return profilerClient.establishConnectionWithServer(attachMode, calibrationOnlyRun);
    }

    /**
     * @return true if the calibration data was read succesfully, false otherwise
     */
    public boolean readSavedCalibrationData() {
        status.targetJDKVersionString = settings.getTargetJDKVersionString();

        int res = CalibrationDataFileIO.readSavedCalibrationData(status);

        if (res < 0) { // Fatal error with reading saved file data - report the details
            appStatusHandler.displayErrorAndWaitForConfirm(CalibrationDataFileIO.getErrorMessage());
        }

        return (res == 0);
    }

    public void removeProfilingEventListener(ProfilingEventListener profilingEventListener) {
        listeners.remove(profilingEventListener);
    }

    public void resetTimers() throws ClientUtils.TargetAppOrVMTerminated {
        profilerClient.resetProfilerCollectors();
    }

    public void resumeTargetAppIfSuspended() throws ClientUtils.TargetAppOrVMTerminated {
        if (targetAppIsSuspended) {
            profilerClient.resumeTargetAppThreads();
            targetAppIsSuspended = false;
            notifyListeners(EVENT_RESUMED);
        }
    }

    public void runGC() throws ClientUtils.TargetAppOrVMTerminated {
        profilerClient.runGC();
    }

    /**
     * Starts the the target JVM, that then waits for the tool to establish the socket connection and start the
     * TA itself). This function returns boolean indicating success or failure, however the actual problem is
     * reported inside it using methods of the AppStatusHandler passed to this TargetAppRunner.
     */
    public boolean startTargetVM() {
        return startTargetVM(settings.getJVMArgs(), settings.getMainClassName(), settings.getMainArgs(),
                             settings.getWorkingDir(), settings.getSeparateConsole());
    }

    public void suspendTargetAppIfRunning() throws ClientUtils.TargetAppOrVMTerminated {
        if (!targetAppIsSuspended) {
            profilerClient.suspendTargetAppThreads();
            targetAppIsSuspended = true;
            notifyListeners(EVENT_SUSPENDED);
        }
    }

    public boolean targetAppIsRunning() {
        return status.targetAppRunning;
    }

    public boolean targetAppSuspended() {
        return targetAppIsSuspended;
    }

    public boolean targetJVMIsAlive() {
        return profilerClient.targetJVMIsAlive();
    }

    public void terminateTargetJVM() {
        if (targetAppIsSuspended) {
            try {
                profilerClient.resumeTargetAppThreads();
            } catch (ClientUtils.TargetAppOrVMTerminated ex) {
            }

            targetAppIsSuspended = false;
        }

        try {
            profilerClient.terminateTargetJVM();
            notifyListeners(EVENT_TERMINATED);
        } catch (ClientUtils.TargetAppOrVMTerminated ex) {
            /* Probably no need to say anything if it's already terminated */
        }

        targetAppIsSuspended = false;
    }

    //---------------------------- Statistics printing routines ----------------------------------

    /**
     * Note that this displayed statistics is really current, i.e. it may be newer than CPU results currently displayed
     */
    private String getInternalStatsText(InternalStatsResponse r) {
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(2);

        StringBuffer s = new StringBuffer(1000);

        s.append(INTERNAL_STATISTICS_ONLY_MSG);

        double wholeGraphGrossTimeAbs = CPUCCTContainer.getWholeGraphGrossTimeAbsForDisplayedThread();
        double timeInInjectedCode = CPUCCTContainer.getTimeInInjectedCodeForDisplayedThread();
        double totalRunTime = wholeGraphGrossTimeAbs + r.totalHotswappingTime + r.clientInstrTime + r.clientDataProcTime;

        s.append(MessageFormat.format(INSTR_METHODS_COUNT_MSG, new Object[] { "" + r.nTotalInstrMethods })); // NOI18N
        s.append("\n"); // NOI18N
        s.append(MessageFormat.format(CLASSLOAD_FIRSTINV_COUNT_MSG,
                                      new Object[] { "" + r.nClassLoads, "" + r.nFirstMethodInvocations })); // NOI18N
        s.append("\n"); // NOI18N
        s.append(MessageFormat.format(NON_EMPTY_IMG_COUNT_MSG, new Object[] { "" + r.nNonEmptyInstrMethodGroupResponses })); // NOI18N
        s.append("\n"); // NOI18N
        s.append(MessageFormat.format(EMPTY_IMG_COUNT_MSG, new Object[] { "" + r.nEmptyInstrMethodGroupResponses })); // NOI18N
        s.append("\n"); // NOI18N
        s.append(MessageFormat.format(SINGLE_IMG_COUNT_MSG, new Object[] { "" + r.nSingleMethodInstrMethodGroupResponses })); // NOI18N
        s.append("\n"); // NOI18N

        if (r.nNonEmptyInstrMethodGroupResponses > 0) {
            s.append(MessageFormat.format(AVG_METHOD_TIME_MSG, new Object[] { nf.format(r.averageHotswappingTime) }));
            s.append("\n"); // NOI18N
            s.append(MessageFormat.format(MIN_METHOD_TIME_MSG, new Object[] { nf.format(r.minHotswappingTime) }));
            s.append("\n"); // NOI18N
            s.append(MessageFormat.format(MAX_METHOD_TIME_MSG, new Object[] { nf.format(r.maxHotswappingTime) }));
            s.append("\n"); // NOI18N
        }

        s.append("\n"); // NOI18N

        s.append(MessageFormat.format(TOTAL_RUN_TIME_MSG, new Object[] { nf.format(totalRunTime) }));
        s.append("\n"); // NOI18N

        if (totalRunTime == 0.0) {
            totalRunTime = 1.0; // Just to avoid funny percentage figures in this case
        }

        s.append(MessageFormat.format(INJ_INSTR_TIME_MSG,
                                      new Object[] {
                                          nf.format(timeInInjectedCode), nf.format(timeInInjectedCode / totalRunTime * 100)
                                      }));
        s.append("\n"); // NOI18N
        s.append(MessageFormat.format(TOTAL_INSTR_HOTSWAP_TIME_MSG,
                                      new Object[] {
                                          nf.format(r.totalHotswappingTime),
                                          nf.format(r.totalHotswappingTime / totalRunTime * 100)
                                      }));
        s.append("\n"); // NOI18N
        s.append(MessageFormat.format(BYTECODE_COMM_TIME_MSG,
                                      new Object[] { nf.format(r.clientInstrTime), nf.format(r.clientInstrTime / totalRunTime * 100) }));
        s.append("\n"); // NOI18N
        s.append(MessageFormat.format(CLIENT_BYTECODE_TIME_MSG, new Object[] { "" + profilerClient.getInstrProcessingTime() })); // NOI18N
        s.append("\n"); // NOI18N
        s.append(MessageFormat.format(CLIENT_DISK_PROCESS_MSG,
                                      new Object[] {
                                          nf.format(r.clientDataProcTime), nf.format(r.clientDataProcTime / totalRunTime * 100)
                                      }));
        s.append("\n"); // NOI18N
                        // no idea what is this supposed to do; put it back if someone has a clue
                        //    EventBufferProcessor eb = profilerClient.getCPUCallGraphBuilder();
                        //    if (eb == null) {
                        //      eb = profilerClient.getMemoryCallGraphBuilder();
                        //    }
                        //    long dataProcessingTime = (eb != null) ? EventBufferProcessor.getDataProcessingTime() : 0;

        long dataProcessingTime = EventBufferProcessor.getDataProcessingTime();
        s.append(MessageFormat.format(CLIENT_RESULTS_PROCESS_MSG, new Object[] { "" + dataProcessingTime })); // NOI18N
        s.append("\n"); // NOI18N
        s.append("\n"); // NOI18N

        appendCalibrationData(s);
        s.append("\n"); // NOI18N

        return s.toString();
    }

    private void appendCalibrationData(StringBuffer s) {
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(4);

        long cntsInSec = status.timerCountsInSecond[0];
        double m0 = (((double) status.methodEntryExitCallTime[0]) * 1000000) / cntsInSec; // Expressed in microseconds
        double m1 = (((double) status.methodEntryExitCallTime[1]) * 1000000) / cntsInSec; // Ditto
        double m2 = (((double) status.methodEntryExitCallTime[2]) * 1000000) / cntsInSec; // Ditto
        double m4 = (((double) status.methodEntryExitCallTime[4]) * 1000000) / cntsInSec; // Ditto

        s.append(MessageFormat.format(CALIBRATION_RESULTS_MSG,
                                      new Object[] { nf.format(m0), nf.format(m1), nf.format(m2), nf.format(m4) }));
    }

    private boolean connectToStartedVMAndStartTA(boolean calibrationOnlyRun) {
        if (!connectToStartedVMAndStartTA(0, calibrationOnlyRun)) {
            return false;
        }

        status.runningInAttachedMode = false;
        notifyListeners(EVENT_STARTED);

        return true;
    }

    //--------------------------------- Private implementation ------------------------------------

    /**
     * attachMode can have the following values:
     * 0 - application started from under the tool; 1 - attach to the running application; 2 - attach on startup.
     * calibrationOnlyRun == true means that we run the target JVM just to obtain instrumentation calibration data.
     */
    private boolean connectToStartedVMAndStartTA(int attachMode, boolean calibrationOnlyRun) {
        //    if (!profilerClient.establishConnectionWithServer(attachMode != 0, calibrationOnlyRun)) {
        //      appStatusHandler.displayError(FAILED_ESTABLISH_CONN_MSG);
        //      return false;
        //    }
        if (calibrationOnlyRun) {
            return true;
        }

        boolean sendExplicitStartCommand = (attachMode != 1);

        try {
            return profilerClient.startTargetApp(sendExplicitStartCommand);
        } catch (ClientUtils.TargetAppOrVMTerminated e1) {
            String message = UNEXPECTED_PROBLEM_STARTING_APP_MSG;

            if (e1.isVMTerminated()) {
                message += JVM_TERMINATED_NOTRESPOND_STRING;
            } else {
                message += INTERNAL_PROBLEM_STRING;
            }

            appStatusHandler.displayError(message);

            return false;
        } catch (ClientUtils.TargetAppFailedToStart e2) {
            appStatusHandler.displayError(MessageFormat.format(FAILED_START_APP_CAUSE_MSG, new Object[] { e2.getOrigCause() }));

            return false;
        }
    }

    private void notifyListeners(int event) {
        Vector targets = null;

        synchronized (this) {
            if (listeners != null) {
                targets = (java.util.Vector) listeners.clone();
            }
        }

        if (targets != null) {
            for (int i = 0; i < targets.size(); i++) {
                ProfilingEventListener target = (ProfilingEventListener) targets.elementAt(i);

                switch (event) {
                    case EVENT_STARTED:
                        target.targetAppStarted();

                        break;
                    case EVENT_STOPPED:
                        target.targetAppStopped();

                        break;
                    case EVENT_SUSPENDED:
                        target.targetAppSuspended();

                        break;
                    case EVENT_RESUMED:
                        target.targetAppResumed();

                        break;
                    case EVENT_TERMINATED:
                        target.targetVMTerminated();

                        break;
                    case EVENT_ATTACHED:
                        target.attachedToTarget();

                        break;
                    case EVENT_DETACHED:
                        target.detachedFromTarget();

                        break;
                }
            }
        }
    }

    //-------------------------- Calibration-related routines ----------------------------------
    private boolean runJVMToCalibrateInstrumentation() {
        boolean result = startTargetVM(new String[] {  }, CALIBRATION_PSEUDO_CLASS_NAME, new String[] {  }, ".",
                                       settings.getSeparateConsole() // NOI18N
        );

        if (!result) {
            return false;
        }

        result = initiateSession(0, true);

        //    result = connectToStartedVMAndStartTA(true);
        //    if (!result) {
        //      return false;
        //    }
        while (targetJVMIsAlive()) {
            try {
                Thread.sleep(100);
            } catch (Exception ex) {
                // ignore
            }
        }

        if (status.timerCountsInSecond[0] == 0) { // Data not received?
            appStatusHandler.displayErrorAndWaitForConfirm(CALIBRATION_ERROR_MSG);

            return false;
        }

        return true;
    }

    private boolean startTargetVM(String[] jvmArgs, String mainClassName, String[] mainArgs, String workingDir,
                                  boolean separateConsole) {
        boolean isWindows = Platform.isWindows();
        status.savedInternalStats = null;

        File dir = new File(workingDir);

        String classPathArg = settings.getMainClassPath(); // Note that this returns user.dir if not set

        // Create the classpath containing the JFluid server-side classes, that is passed to the target VM
        String libPath = settings.getJFluidRootDirName();
        String jdkVer = settings.getTargetJDKVersionString();

        if (jdkVer.equals(JDK_16_STRING) || jdkVer.equals(JDK_17_STRING)) {
            // for now the 1.6 and 1.7 profiling uses the same agent as 1.5
            jdkVer = JDK_15_STRING;
        }

        jdkVer = jdkVer.substring(3); // Convert e.g. "jdk15" into just "15"

        String jFluidCP = libPath + File.separator + "jfluid-server.jar" // NOI18N
                          + File.pathSeparator + libPath + File.separator + "jfluid-server-" // NOI18N
                          + jdkVer + ".jar"; // NOI18N

        String[] newJVMArgs = new String[jvmArgs.length];
        int idx = 0;

        for (int i = 0; i < jvmArgs.length; i++) {
            if ((jvmArgs[i].equals("-classpath") || jvmArgs[i].equals("-cp")) && ((i + 1) < jvmArgs.length)) { // NOI18N
                                                                                                               // The user shouldn't set the classpath here, so let's ignore it.
                appStatusHandler.displayWarning(CLASSPATH_SETTINGS_IGNORED_MSG);
                i++;

                //classPathArg += File.pathSeparator;
                //classPathArg += jvmArgs[++i];
            } else {
                newJVMArgs[idx++] = jvmArgs[i];
            }
        }

        ArrayList commands = new ArrayList(10);

        if (separateConsole) {
            if (isWindows) {
                commands.add("cmd.exe"); // NOI18N
                commands.add("/K"); // NOI18N
                commands.add("start"); // NOI18N
                commands.add("\"Profiled Application Console\""); // NOI18N

                if (settings.getTargetWindowRemains()) {
                    commands.add("cmd"); // This is used to prevent the window from closing if, say, // NOI18N
                    commands.add("/K"); // the target JVM crashes. // NOI18N
                }
            } else { // Solaris
                commands.add("xterm"); // NOI18N
                commands.add("-sb"); // NOI18N
                commands.add("-sl"); // NOI18N
                commands.add("1000"); // NOI18N
                commands.add("-e"); // NOI18N
            }
        }

        commands.add(settings.getTargetJVMExeFile());

        if (settings.getTargetJDKVersionString().equals(Platform.JDK_15_STRING)
                || settings.getTargetJDKVersionString().equals(Platform.JDK_16_STRING)
                || settings.getTargetJDKVersionString().equals(Platform.JDK_17_STRING)) {
            String jfNativeLibFullName = Platform.getAgentNativeLibFullName(settings.getJFluidRootDirName(), false,
                                                                            settings.getTargetJDKVersionString(),
                                                                            settings.getSystemArchitecture());
            commands.add("-agentpath:" + jfNativeLibFullName); // NOI18N
        }

        commands.add("-Xbootclasspath/a:" + jFluidCP); // NOI18N

        if ((classPathArg != null) && !classPathArg.equals("")) { // NOI18N
            commands.add("-classpath"); // NOI18N
            commands.add(classPathArg);
        }

        if (!isWindows && settings.getTargetWindowRemains()) {
            commands.add("-XX:+ShowMessageBoxOnError"); // NOI18N
        }

        for (int i = 0; i < newJVMArgs.length; i++) {
            commands.add(newJVMArgs[i]);
        }

        // debugging property for agent side - wire I/O
        if (System.getProperty("org.netbeans.lib.profiler.wireprotocol.WireIO.agent") != null) { // NOI18N
            commands.add("-Dorg.netbeans.lib.profiler.wireprotocol.WireIO=true"); // NOI18N
        }

        // debugging property for agent side - Class loader hook
        if (System.getProperty("org.netbeans.lib.profiler.server.ProfilerInterface.classLoadHook") != null) { // NOI18N
            commands.add("-Dorg.netbeans.lib.profiler.server.ProfilerInterface.classLoadHook=true"); // NOI18N
        }

        commands.add("org.netbeans.lib.profiler.server.ProfilerServer"); // NOI18N

        // Really needed by ProfilerServer only in JDK 1.4.2, to call System.load() with this param - TODO: check this
        commands.add(Platform.getJFluidNativeLibDirName(settings.getJFluidRootDirName(), settings.getTargetJDKVersionString(),
                                                        settings.getSystemArchitecture()));
        commands.add(Integer.toString(settings.getPortNo()));

        // 10 seconds is the default timeout, can be set via the profiler.agent.connect.timeout property
        String timeOut = System.getProperty("profiler.agent.connect.timeout", "10"); // NOI18N
        commands.add(timeOut);

        if (mainClassName != null) {
            commands.add(mainClassName);
        }

        for (int i = 0; i < mainArgs.length; i++) {
            commands.add(mainArgs[i]);
        }

        String[] cmdArray = new String[commands.size()];
        commands.toArray(cmdArray);

        MiscUtils.printInfoMessage("Starting target application..."); // NOI18N
        MiscUtils.printVerboseInfoMessage(cmdArray);

        if (DEBUG) {
            System.err.println("TargetAppRunner.DEBUG: Starting VM with " + cmdArray.length + " commands."); // NOI18N

            for (int i = 0; i < cmdArray.length; i++) {
                System.err.println("TargetAppRunner.DEBUG: cmd[" + i + "] = >" + cmdArray[i] + "<"); // NOI18N
            }
        }

        try {
            runningAppProcess = Runtime.getRuntime().exec(cmdArray, null, dir);
        } catch (IOException ex) {
            String s = ""; // NOI18N

            for (int i = 0; i < cmdArray.length; i++) {
                s = s + cmdArray[i] + "\n"; // NOI18N
            }

            appStatusHandler.displayError(MessageFormat.format(ERROR_STARTING_JVM_MSG, new Object[] { s, ex }));

            return false;
        }

        return true;
    }
}
