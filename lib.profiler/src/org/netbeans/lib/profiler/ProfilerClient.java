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

package org.netbeans.lib.profiler;

import org.netbeans.lib.profiler.classfile.ClassRepository;
import org.netbeans.lib.profiler.client.AppStatusHandler;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.client.MonitoredData;
import org.netbeans.lib.profiler.client.RuntimeProfilingPoint;
import org.netbeans.lib.profiler.global.CalibrationDataFileIO;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.global.Platform;
import org.netbeans.lib.profiler.global.ProfilingSessionStatus;
import org.netbeans.lib.profiler.instrumentation.BadLocationException;
import org.netbeans.lib.profiler.instrumentation.InstrumentationException;
import org.netbeans.lib.profiler.instrumentation.Instrumentor;
import org.netbeans.lib.profiler.marker.Marker;
import org.netbeans.lib.profiler.results.EventBufferProcessor;
import org.netbeans.lib.profiler.results.EventBufferResultsProvider;
import org.netbeans.lib.profiler.results.ProfilingResultsDispatcher;
import org.netbeans.lib.profiler.results.coderegion.CodeRegionResultsSnapshot;
import org.netbeans.lib.profiler.results.cpu.CPUCCTProvider;
import org.netbeans.lib.profiler.results.cpu.CPUResultsSnapshot;
import org.netbeans.lib.profiler.results.cpu.FlatProfileProvider;
import org.netbeans.lib.profiler.results.memory.*;
import org.netbeans.lib.profiler.utils.MiscUtils;
import org.netbeans.lib.profiler.utils.StringUtils;
import org.netbeans.lib.profiler.wireprotocol.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * The interface between the tool and the profiling back end.
 *
 * @author Tomas Hurka
 * @author Misha Dmitriev
 * @author Adrian Mos
 * @author Ian Formanek
 */
public class ProfilerClient implements CommonConstants {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    /**
     * Thread for execution of commands that, due to limitations of our wire protocol, need to be executed such that
     * the ServerListener thread doesn't stay blocked while these commands are executed. See executeInSeparateThread()
     * above.
     */
    private class SeparateCmdExecutionThread extends Thread {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void run() {
            setName(PROFILER_SEPARATE_EXEC_THREAD_NAME);

            synchronized (execInSeparateThreadLock) {
                while (true) {
                    try {
                        execInSeparateThreadLock.wait();
                    } catch (InterruptedException ex) {
                        MiscUtils.internalError("ProfilerClient.SpecialExecutionThread.run()"); // NOI18N
                    }

                    if (execInSeparateThreadCmd == null) {
                        return; // It was a signal to this thread to terminate
                    }

                    Command cmd = execInSeparateThreadCmd;
                    execInSeparateThreadCmd = null;

                    switch (cmd.getType()) {
                        case Command.ROOT_CLASS_LOADED:
                            instrumentMethodGroupFromRoot((RootClassLoadedCommand) cmd);

                            //instrMethodGroupFromRootComplete = true;
                            break;
                        case Command.CLASS_LOADED:
                        case Command.METHOD_INVOKED_FIRST_TIME:
                        case Command.METHOD_LOADED:
                            instrumentMethodGroupFollowUp(cmd);

                            break;
                        case Command.EVENT_BUFFER_DUMPED:
                            EventBufferDumpedCommand bufferDumpedCmd = ((EventBufferDumpedCommand) cmd);
                            byte[] buf = EventBufferProcessor.readDataAndPrepareForProcessing(bufferDumpedCmd);
                            
                            EventBufferResultsProvider.getDefault().dataReady(buf, getCurrentInstrType());
                            sendSimpleRespToServer(true, null);

                            break;
                        case Command.CLASS_LOADER_UNLOADING:

                            // We have to grab the forceObtainedResultsDumpLock to prevent forceObtainedResultsDump() coming in while
                            // we are processing the data, sending the request for dump to the server that currently awaits the
                            // request for jmethodIds, and thus creating a "distributed deadlock".
                            synchronized (ProfilerClient.this) {
                                synchronized (forceObtainedResultsDumpLock) {
                                    if (memCctProvider != null) {
                                        memCctProvider.updateInternals();
                                    }

                                    sendSimpleRespToServer(true, null);
                                }
                            }

                            break;
                    }
                }
            }
        }
    }

    private class ServerListener extends Thread {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private final Object startedFlagLock = new Object();

        // @GuardedBy startedFlagLock
        private int startedFlag = 0; // 0 = initial state; 1 = started; -1 = cancelled

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public boolean isRunning() {
            synchronized (startedFlagLock) {
                return startedFlag == 1;
            }
        }

        public void cancel() {
            synchronized (startedFlagLock) {
                startedFlag = -1;
                startedFlagLock.notifyAll();
            }
        }

        public void run() {
            // Wait until we know that the connection is open
            synchronized (startedFlagLock) {
                while (startedFlag == 0) { // while the state hasn't been explicitly changed

                    try {
                        startedFlagLock.wait(500);
                    } catch (InterruptedException e) {
                        startedFlag = -1; // thread has been interrupet = effectively cancelled
                    }
                }

                if (startedFlag == -1) { // cancelled

                    return;
                }
            }
            
            startSeparateCmdExecThread();
            try {
                while (targetVMAlive) {
                    try {
                        Object o = wireIO.receiveCommandOrResponse();

                        //System.out.println(">>> Got response or command from server " + o);
                        if (o == null) {
                            closeConnection();
                        } else {
                            if (o instanceof Command) {
                                handleServerCommand((Command) o);
                            } else {
                                setLastResponse((Response) o);
                            }
                        }
                    } catch (IOException ex) {
                        if (targetVMAlive && !terminateOrDetachCommandIssued) { // It wasn't a normal connection shutdown
                            MiscUtils.printErrorMessage("exception while trying to get response from the target JVM:\n" + ex); // NOI18N
                            closeConnection();

                            //            serverCommandHandler.handleServerCommand(null); // does not seem to do anything
                        }
                    }
                }
            } finally {
                stopSeparateCmdExecThread();
            }
        }

        public void shutdown() {
            synchronized (startedFlagLock) {
                startedFlag = 0;
                startedFlagLock.notifyAll();
            }
        }

        public void startRunning() {
            synchronized (startedFlagLock) {
                startedFlag = 1;
                startedFlagLock.notifyAll();
            }
        }

        private void handleServerCommand(final Command cmd) {
            switch (cmd.getType()) {
                case Command.SHUTDOWN_INITIATED:
                    status.targetAppRunning = false;

                    // Get and save the latest results and the internal statistics before the target VM goes away
                    (new Thread() {
                            public void run() {
                                try {
                                    int instrType = getCurrentInstrType();

                                    if (currentInstrTypeIsRecursiveCPUProfiling() || currentInstrTypeIsMemoryProfiling()) {
                                        forceObtainedResultsDump(false, 15);
                                    }

                                    // In case of memory profiling, fetch additional data from the VM - names for all jmethodIDs and
                                    // object count
                                    if (currentInstrTypeIsMemoryProfiling()) {
                                        savedAllocatedObjectsCountResults = getAllocatedObjectsCountResults();
                                        // #204978: methodIds must be loaded from instead of 
                                        // the MemoryCallGraphBuilder'shutdown' method where it is too late
                                        if (memCctProvider instanceof MemoryCallGraphBuilder) {
                                            ((MemoryCallGraphBuilder)memCctProvider).updateInternals();
                                        }
                                    }

                                    status.savedInternalStats = getInternalStats();

                                    appStatusHandler.handleShutdown();

                                    sendSimpleCmdToServer(Command.SHUTDOWN_OK);
                                } catch (ClientUtils.TargetAppOrVMTerminated ex) { /* Ignore silently */
                                }
                            }
                        }).start();

                    break;
                case Command.SHUTDOWN_COMPLETED:
                    targetVMAlive = false;
                    status.targetAppRunning = false;
                    EventBufferProcessor.removeEventBufferFile();

                    break;
                case Command.ROOT_CLASS_LOADED:
                    executeInSeparateThread(cmd);

                    break;
                case Command.CLASS_LOADED:
                case Command.METHOD_INVOKED_FIRST_TIME:
                case Command.METHOD_LOADED:
                    executeInSeparateThread(cmd);

                    break;
                case Command.EVENT_BUFFER_DUMPED:
                    EventBufferDumpedCommand ebdCmd = (EventBufferDumpedCommand) cmd;
                    String bufferName = ebdCmd.getEventBufferFileName();
                    if (bufferName.length() > 0) {
                        if (!EventBufferProcessor.bufFileExists()) {
                            if (!EventBufferProcessor.setEventBufferFile(bufferName)) {
                                appStatusHandler.displayError(MessageFormat.format(CANNOT_OPEN_SERVER_TEMPFILE_MSG,
                                                                                   new Object[] { ebdCmd.getEventBufferFileName() }));
                            }
                        }
                        JMethodIdTable.reset();
                    }
                    readAndProcessProfilingResults(ebdCmd);

                    break;
                case Command.CLASS_LOADER_UNLOADING:
                    executeInSeparateThread(cmd);

                    break;
                case Command.RESULTS_AVAILABLE:
                    resultsStart = System.currentTimeMillis();

                    break;
                case Command.GET_CLASSID:

                    GetClassIdCommand cidCmd = (GetClassIdCommand) cmd;
                    int classId = instrumentor.getClassId(cidCmd.getClassName(), cidCmd.getClassLoaderId());
                    sendComplexRespToServer(new GetClassIdResponse(classId != -1, classId));

                    break;
                case Command.STILL_ALIVE:
                    break;
            }

            if (!targetVMAlive) {
                closeConnection();
            }

            serverCommandHandler.handleServerCommand(cmd);
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String CANNOT_OPEN_SERVER_TEMPFILE_MSG;
    private static final String PERFORMING_INSTRUMENTATION_STRING;
    private static final String INVALID_CODE_REGION_MSG;
    private static final String CLASS_NOT_FOUND_MSG;
    private static final String OUT_OF_MEMORY_MSG;
    private static final String INCORRECT_AGENT_VERSION_MSG;
    private static final String ERROR_GETTING_CALIBRATION_DATA_MSG;
    private static final String MUST_CALIBRATE_FIRST_MSG;
    private static final String MUST_CALIBRATE_FIRST_SHORT_MSG;
    private static final String INSTRUMENTATION_LIMIT_REACHED_MSG;
    private static final String CORRUPTED_TARGET_CALIBRATION_DATA_MSG;
    private static final String CONNECT_VM_MSG;
    private static final String TARGET_JVM_ERROR_MSG;
    private static final String UNSUPPORTED_JVM_MSG;

    static {
        ResourceBundle messages = ResourceBundle.getBundle("org.netbeans.lib.profiler.Bundle"); // NOI18N
        CANNOT_OPEN_SERVER_TEMPFILE_MSG = messages.getString("ProfilerClient_CannotOpenServerTempFileMsg"); // NOI18N
        PERFORMING_INSTRUMENTATION_STRING = messages.getString("ProfilerClient_PerformingInstrumentationString"); // NOI18N
        INVALID_CODE_REGION_MSG = messages.getString("ProfilerClient_InvalidCodeRegionMsg"); // NOI18N
        CLASS_NOT_FOUND_MSG = messages.getString("ProfilerClient_ClassNotFoundMsg"); // NOI18N
        OUT_OF_MEMORY_MSG = messages.getString("ProfilerClient_OutOfMemoryMsg"); // NOI18N
        INCORRECT_AGENT_VERSION_MSG = messages.getString("ProfilerClient_IncorrectAgentVersionMsg"); // NOI18N
        ERROR_GETTING_CALIBRATION_DATA_MSG = messages.getString("ProfilerClient_ErrorGettingCalibrationDataMsg"); // NOI18N
        MUST_CALIBRATE_FIRST_MSG = messages.getString("ProfilerClient_MustCalibrateFirstMsg"); // NOI18N
        MUST_CALIBRATE_FIRST_SHORT_MSG = messages.getString("ProfilerClient_MustCalibrateFirstShortMsg"); // NOI18N
        INSTRUMENTATION_LIMIT_REACHED_MSG = messages.getString("ProfilerClient_InstrumentationLimitReachedMsg"); // NOI18N
        CORRUPTED_TARGET_CALIBRATION_DATA_MSG = messages.getString("ProfilerClient_CorruptedTargetCalibrationDataMsg"); // NOI18N
        CONNECT_VM_MSG = messages.getString("ProfilerClient_ConnectVmMsg"); // NOI18N
        TARGET_JVM_ERROR_MSG = messages.getString("ProfilerClient_TargetJvmErrorMsg"); // NOI18N
        UNSUPPORTED_JVM_MSG = messages.getString("ProfilerClient_UnsupportedJvmMsg"); // NOI18N
    }
    
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private AppStatusHandler.ServerCommandHandler serverCommandHandler;
    private AppStatusHandler appStatusHandler;
    private CPUCCTProvider cpuCctProvider;
    private Command execInSeparateThreadCmd;
    private FlatProfileProvider flatProvider;
    private InitiateProfilingCommand commandOnStartup = null;
    private Instrumentor instrumentor;
    private MemoryCCTProvider memCctProvider;
    private final Object execInSeparateThreadLock = new Object();
    final private Object forceObtainedResultsDumpLock = new Object(); // To make dump processing and other commands mutually
                                                                // exclusive

    /*instrMethodGroupFromRootComplete, */
    private final Object instrumentationLock = new Object(); // To make sure all instrumentation-related operations
                                                       // happen serially
    private final Object responseLock = new Object();
    private ObjectInputStream socketIn;
    private ObjectOutputStream socketOut;
    private ProfilerEngineSettings settings;
    private ProfilingSessionStatus status;
    private volatile Response lastResponse;
    private SeparateCmdExecutionThread separateCmdExecThread;
    private ServerListener serverListener;
    private HeapHistogramManager histogramManager;

    //--------------------- Connection management --------------------
    private Socket clientSocket;
    private WireIO wireIO;

    /**
     * Needed to make memory profiling results available after app/VM shutdown
     *
     * Note that we don't have anything like getMemoryProfilingResult() here - essentially because we don't have memory
     * results snapshots yet. Those, in turn, are not implemented because of performance concerns (reproducing our,
     * potentially huge, hash table containing all tracked object, plus the call trees for these object allocations,
     * every time that the user hits "Get results" seems scary). So instead of snapshots, we give the user various
     * aspects of (constantly updated) memory profiling data on demand. Methods that return it are public ones in
     * ObjAllocCallGraphBuilder, ObjLivenessCallGraphBuilder, and MemoryCallGraphBuilder.
     * The getAllocatedObjectsCountResults() method below provides only one aspect of the memory profiling data.
     */
    private int[] savedAllocatedObjectsCountResults;
    private volatile boolean connectionWithServerOpen; // Used just to prevent double entry into closeConnection()
    private volatile boolean forceObtainedResultsDumpCalled;
    private volatile boolean handlingEventBufferDump;
    private volatile boolean instrMethodsLimitReported;
    private boolean serverClassesInitialized;
    private volatile boolean targetVMAlive;
    private volatile boolean terminateOrDetachCommandIssued;
    private int currentAgentId = -1;
    private long instrProcessingTime;
    private long resultsStart;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public ProfilerClient(ProfilerEngineSettings settings, ProfilingSessionStatus status, AppStatusHandler ash,
                          AppStatusHandler.ServerCommandHandler sch) {
        this.settings = settings;
        this.status = status;
        appStatusHandler = ash;
        serverCommandHandler = sch;
        instrumentor = new Instrumentor(status, settings);
        histogramManager = new HeapHistogramManager();
        EventBufferProcessor.initialize(this);
        EventBufferResultsProvider.getDefault().addDispatcher(ProfilingResultsDispatcher.getDefault());
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the array where element at index I is the total number of allocated objects for the class with I id.
     * The relevant counters are kept at the server side and returned to the tool on demand, here.
     */
    public synchronized int[] getAllocatedObjectsCountResults()
        throws ClientUtils.TargetAppOrVMTerminated {
        if (!targetVMAlive) {
            if (savedAllocatedObjectsCountResults != null) {
                return savedAllocatedObjectsCountResults;
            } else {
                throw new ClientUtils.TargetAppOrVMTerminated(ClientUtils.TargetAppOrVMTerminated.VM);
            }
        }

        savedAllocatedObjectsCountResults = null;
        checkForTargetVMAlive();
        sendSimpleCmdToServer(Command.GET_OBJECT_ALLOCATION_RESULTS);

        ObjectAllocationResultsResponse resp = (ObjectAllocationResultsResponse) getAndCheckLastResponse("Unknown problem when trying to get allocated object count results."); // NOI18N

        return resp.getResults();
    }

    /**
     * Returns the snapshot of current multi-method CPU profiling results
     *
     * @return CPU Results snapshot
     * @throws ClientUtils.TargetAppOrVMTerminated
     *          In case the profiled application has already terminated
     * @throws CPUResultsSnapshot.NoDataAvailableException
     *          If no data are available yet
     */
    public synchronized CPUResultsSnapshot getCPUProfilingResultsSnapshot()
        throws ClientUtils.TargetAppOrVMTerminated, CPUResultsSnapshot.NoDataAvailableException {
        return getCPUProfilingResultsSnapshot(true);
    }

    /**
     * Returns the snapshot of current multi-method CPU profiling results
     *
     * @param dump true to fetch latest events from server, false otherwise (use only available data)
     * @return CPU Results snapshot
     * @throws ClientUtils.TargetAppOrVMTerminated
     *          In case the profiled application has already terminated
     * @throws CPUResultsSnapshot.NoDataAvailableException
     *          If no data are available yet
     */
    public CPUResultsSnapshot getCPUProfilingResultsSnapshot(boolean dump)
        throws ClientUtils.TargetAppOrVMTerminated, CPUResultsSnapshot.NoDataAvailableException {
        checkForTargetVMAlive();

        if (dump) {
            if (!forceObtainedResultsDump(false, 5)) {
                return null;
            }
        }
        synchronized (this) {
            int len = 0;
            boolean twoTimeStamps = false;
            String[] instrClassNames, instrMethodNames, instrMethodSigs;
            try {
                status.beginTrans(false);
                twoTimeStamps = status.collectingTwoTimeStamps();
                len = status.getNInstrMethods();
                instrClassNames = new String[len];
                System.arraycopy(status.getInstrMethodClasses(), 0, instrClassNames, 0, len);
                instrMethodNames = new String[len];
                System.arraycopy(status.getInstrMethodNames(), 0, instrMethodNames, 0, len);
                instrMethodSigs = new String[len];
                System.arraycopy(status.getInstrMethodSignatures(), 0, instrMethodSigs, 0, len);
                return new CPUResultsSnapshot(resultsStart, System.currentTimeMillis(), cpuCctProvider, twoTimeStamps, instrClassNames, instrMethodNames, instrMethodSigs, len);
            } finally {
                status.endTrans();
            }
        }
    }

    /**
     * Returns the snapshot of current code region profiling results
     */
    public synchronized CodeRegionResultsSnapshot getCodeRegionProfilingResultsSnapshot()
        throws ClientUtils.TargetAppOrVMTerminated {
        checkForTargetVMAlive();
        sendSimpleCmdToServer(Command.GET_CODE_REGION_CPU_RESULTS);

        CodeRegionCPUResultsResponse resp = (CodeRegionCPUResultsResponse) getAndCheckLastResponse("Unknown problem when trying to get code region CPU results."); // NOI18N

        return new CodeRegionResultsSnapshot(resultsStart, System.currentTimeMillis(), resp.getResults(),
                                             status.timerCountsInSecond[0]);
    }

    public int getCurrentAgentId() {
        return currentAgentId;
    }

    public void setCurrentInstrType(int type) {
        status.currentInstrType = type;
    }

    public int getCurrentInstrType() {
        return status.currentInstrType;
    }

    /**
     * Determine which of the currently tracked threads are dead or alive. If the VM is not running, just returns null -
     * it's clear that all threads are dead then.
     */
    public synchronized byte[] getCurrentThreadsLivenessStatus() {
        try {
            checkForTargetVMAlive();
            sendSimpleCmdToServer(Command.GET_THREAD_LIVENESS_STATUS);

            ThreadLivenessStatusResponse resp = (ThreadLivenessStatusResponse) getAndCheckLastResponse("Unknown problem when trying to get thread liveness information."); // NOI18N

            return resp.getStatus();
        } catch (ClientUtils.TargetAppOrVMTerminated ex) {
            if (serverListener.isRunning()) { // The possibly problematic situation is not known yet
                ProfilerLogger.log("in getCurrentThreadLivenessStatus(), caught exception: " + ex); // NOI18N
            }

            return null;
        }
    }

    /**
     * For the class with the given name and the initiating class loader (see Java Language/JVM Spec for definitions),
     * find out and return the defining class loader. Both class loaders are internal class loader ids.
     */
    public synchronized int getDefiningClassLoaderId(String className, int initiatingLoaderId)
                                              throws ClientUtils.TargetAppOrVMTerminated {
        checkForTargetVMAlive();

        GetDefiningClassLoaderCommand cmd = new GetDefiningClassLoaderCommand(className, initiatingLoaderId);
        sendComplexCmdToServer(cmd);

        DefiningLoaderResponse resp = (DefiningLoaderResponse) getAndCheckLastResponse("Unknown problem when trying to get a defining loader for class"); // NOI18N

        return resp.getLoaderId();
    }

    public FlatProfileProvider getFlatProfileProvider() {
        return flatProvider;
    }

    public long getInstrProcessingTime() {
        return instrProcessingTime;
    }

    //---------------- Internal statistics and other target VM information obtaining ----------------
    public synchronized InternalStatsResponse getInternalStats()
        throws ClientUtils.TargetAppOrVMTerminated {
        checkForTargetVMAlive();
        sendSimpleCmdToServer(Command.GET_INTERNAL_STATS);

        InternalStatsResponse resp = (InternalStatsResponse) getLastResponse();

        return resp;
    }

    public MemoryCCTProvider getMemoryCCTProvider() {
        return memCctProvider;
    }

    /**
     * Returns the snapshot of current Memory profiling results
     *
     * @return Memory Results snapshot
     * @throws ClientUtils.TargetAppOrVMTerminated
     *          In case the profiled application has already terminated
     */
    public MemoryResultsSnapshot getMemoryProfilingResultsSnapshot()
        throws ClientUtils.TargetAppOrVMTerminated {
        return getMemoryProfilingResultsSnapshot(true);
    }

    /**
     * Returns the snapshot of current Memory profiling results
     *
     * @param dump true to fetch latest events from server, false otherwise (use only available data)
     * @return Memory Results snapshot
     * @throws ClientUtils.TargetAppOrVMTerminated
     *          In case the profiled application has already terminated
     */
    public MemoryResultsSnapshot getMemoryProfilingResultsSnapshot(boolean dump)
        throws ClientUtils.TargetAppOrVMTerminated {
        checkForTargetVMAlive();
        int instrType = getCurrentInstrType();
        
        if (instrType == INSTR_NONE_MEMORY_SAMPLING) {
            if (settings.getRunGCOnGetResultsInMemoryProfiling()) {
                runGC();
            }
            return new SampledMemoryResultsSnapshot(resultsStart, System.currentTimeMillis(), this);
        }
        if (dump) {
            if (!forceObtainedResultsDump(false, 5)) {
                return null;
            }
        }

        memCctProvider.beginTrans(false);

        try {
            memCctProvider.updateInternals();

            if (instrType == INSTR_OBJECT_ALLOCATIONS) {
                return new AllocMemoryResultsSnapshot(resultsStart, System.currentTimeMillis(), memCctProvider, this);
            } else {
                return new LivenessMemoryResultsSnapshot(resultsStart, System.currentTimeMillis(), memCctProvider, this);
            }
        } finally {
            memCctProvider.endTrans();
        }
    }

    public Marker getMethodMarker() {
        return settings.getMethodMarker();
    }

    /**
     * Called to obtain method names for jMethodIds, that we do not know method names of.
     * This method is typically called when results are to be displayed, but also in case some classes are unloaded
     * in the profiled application, as in this case we would lost method names for already accumulated results.
     * <p/>
     * Assumption is that jMethodId is never reused inside the JVM.
     *
     * @param methodIds array of jMethodIds that we do not have names for
     * @return the 3xn array, containing triplets of {class name, method name, method signature} strings for
     *         given jmethodIds
     */
    public synchronized String[][] getMethodNamesForJMethodIds(int[] methodIds)
        throws ClientUtils.TargetAppOrVMTerminated {
        checkForTargetVMAlive();

        GetMethodNamesForJMethodIdsCommand cmd = new GetMethodNamesForJMethodIdsCommand(methodIds);
        sendComplexCmdToServer(cmd);

        MethodNamesResponse resp = (MethodNamesResponse) getAndCheckLastResponse("Unknown problem when trying to get method names for jmethodIds"); // NOI18N

        return StringUtils.convertPackedStringsIntoStringArrays(resp.getPackedData(), resp.getPackedArrayOffsets(), 3);
    }

    public synchronized HeapHistogram getHeapHistogram() throws ClientUtils.TargetAppOrVMTerminated {
        HeapHistogramResponse resp;
        
        checkForTargetVMAlive();
        sendSimpleCmdToServer(Command.GET_HEAP_HISTOGRAM);
        resp = (HeapHistogramResponse) getAndCheckLastResponse("Unknown problem when trying to get heap histogram"); // NOI18N
        return histogramManager.getHistogram(resp);
    }
    
    public synchronized MonitoredData getMonitoredData() {
        try {
            checkForTargetVMAlive();
            sendSimpleCmdToServer(Command.GET_MONITORED_NUMBERS);

            Response resp = getAndCheckLastResponse("Unknown problem when trying to get memory numbers."); // NOI18N

            try {
                MonitoredNumbersResponse mresp = (MonitoredNumbersResponse) resp;

                return MonitoredData.getMonitoredData(mresp);
            } catch (ClassCastException ex) {
                // FIXME: this diagnostics stuff should be ultimately removed once the root cause of the problem is understood
                MiscUtils.printErrorMessage("caught ClassCastException in getMonitoredNumbers. The real class of resp is " // NOI18N
                                            + resp.getClass().getName() + ", resp = " + resp // NOI18N
                                            );
                throw ex;
            }
        } catch (ClientUtils.TargetAppOrVMTerminated ex) {
            if (serverListener.isRunning()) { // The possibly problematic situation is not known yet
                ProfilerLogger.log("in getMonitoredData(), caught exception: " + ex); // NOI18N
            }

            return null;
        }
    }

    /**
     * @return ProfilerEngineSettings current profiler engine settings
     */
    public ProfilerEngineSettings getSettings() {
        return settings;
    }

    /**
     * We are using this essentially to let user know when the longest part of what happens after hitting e.g.
     * "Instrument Object Allocation", is complete. Otherwise it might be difficult to figure out what's going on
     */

    //  public boolean isInstrMethodGroupFromRootComplete()       { return instrMethodGroupFromRootComplete; }
    //  public CPUCallGraphBuilder getCPUCallGraphBuilder() { return ccgb; }

    //  public MemoryCallGraphBuilder getMemoryCallGraphBuilder() { return mcgb; }
    public ObjectInputStream getSocketInputStream() {
        return socketIn;
    }

    public ProfilingSessionStatus getStatus() {
        return status;
    }

    public synchronized boolean cpuResultsExist() throws ClientUtils.TargetAppOrVMTerminated {
        checkForTargetVMAlive();
        sendSimpleCmdToServer(Command.CPU_RESULTS_EXIST);

        Response resp = getAndCheckLastResponse("Unknown problem when trying to check for CPU profiling results."); // NOI18N

        return resp.yes();
    }

    public boolean currentInstrTypeIsMemoryProfiling() {
        return ((status.currentInstrType == INSTR_OBJECT_ALLOCATIONS) || (status.currentInstrType == INSTR_OBJECT_LIVENESS));
    }

    public boolean currentInstrTypeIsRecursiveCPUProfiling() {
        return ((status.currentInstrType == INSTR_RECURSIVE_FULL) || (status.currentInstrType == INSTR_RECURSIVE_SAMPLED));
    }

    /**
     * Removes instrumentation for classes with ids such that unprofiledClassStatusArray[id] == false.
     * For these classes, no memory profiling data will be generated anymore.
     */
    public void deinstrumentMemoryProfiledClasses(boolean[] unprofiledClassStatusArray)
                                           throws InstrumentationException, ClientUtils.TargetAppOrVMTerminated {
        synchronized (instrumentationLock) {
            if (getCurrentInstrType() == INSTR_NONE || getCurrentInstrType() == INSTR_NONE_SAMPLING) {
                return;
            }

            Response resp = null;
            checkForTargetAppRunning();

            long curTime = System.currentTimeMillis();
            InstrumentMethodGroupCommand cmd = instrumentor.getCommandToUnprofileClasses(unprofiledClassStatusArray);

            if (!cmd.isEmpty()) {
                synchronized (this) {
                    // System.out.println("*** Profiler Engine: deinstrumentMemoryProfiledClasses() produced command:"); cmd.dump();
                    sendComplexCmdToServer(cmd);
                    instrProcessingTime += (System.currentTimeMillis() - curTime);
                    resp = getLastResponse();
                }

                if (!resp.isOK()) {
                    throw new InstrumentationException(resp.getErrorMessage());
                }
            }
        }
    }

    public void prepareDetachFromTargetJVM() throws ClientUtils.TargetAppOrVMTerminated {
        while(true) {
            // active waiting with released lock, this prevents deadlock if getDefiningClassLoaderId is
            // called simultanously
            synchronized(this) {
                sendSimpleCmdToServer(Command.PREPARE_DETACH);
                Response resp = getAndCheckLastResponse("prepareDetachFromTargetJVM");
                if(!resp.isOK()) {
                    return;
                }
                if(resp.yes())
                {
                    break;
                }
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                MiscUtils.printWarningMessage("Interrupted while waiting for prepare detach");
            }
        }
    }

    public synchronized void detachFromTargetJVM() throws ClientUtils.TargetAppOrVMTerminated {
        checkForTargetVMAlive();
        terminateOrDetachCommandIssued = true;
        sendSimpleCmdToServer(Command.DETACH);

        try {
            getLastResponse();
        } finally {
            closeConnection();
            EventBufferProcessor.removeEventBufferFile(); // Try again, just in case closeConnection returned without calling it
        }
    }

    /**
     * This is called in all modes, direct invoke or attachment, to establish connection with the target VM
     * @param attachMode 0 = no attach, 1 = direct attach, 2 = dynamic attach
     * @param calibrationOnlyRun connection in calibration mode only
     * @param cancel shared cancel flag
     */
    public boolean establishConnectionWithServer(int attachMode, boolean calibrationOnlyRun, AtomicBoolean cancel) {
        // Make sure we initialize this field early - it may be changed once we connect to the JVM and find out its
        // real version.
        status.targetJDKVersionString = settings.getTargetJDKVersionString();

        return connectToServer(attachMode, calibrationOnlyRun, cancel);
    }

    /**
     * Tells the server to send the contents of its data buffer to the tool immediately, no matter whether it's
     * full or not.
     */
    public boolean forceObtainedResultsDump() throws ClientUtils.TargetAppOrVMTerminated {
        return forceObtainedResultsDump(false, 0);
    }

    public boolean forceObtainedResultsDump(boolean liveResults, int retries)
                                     throws ClientUtils.TargetAppOrVMTerminated {
        boolean dumped = false;
        int retryCounter = retries;

        do {
            dumped = forceObtainedResultsDump(liveResults);

            if (!dumped) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    break;
                }
            }
        } while (!dumped && (--retryCounter > 0));
        // fix for Issue #135532
        if (dumped) {
             try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
            }
        }
        return dumped;
    }

    /**
     * Tells the server to send the contents of its data buffer to the tool immediately, no matter whether it's
     * full or not.
     */
    public boolean forceObtainedResultsDump(boolean liveResults)
                                     throws ClientUtils.TargetAppOrVMTerminated {
        // The locks below are in the special order, to prevent deadlocks
        synchronized (this) {
            synchronized (forceObtainedResultsDumpLock) {
                if (handlingEventBufferDump) {
                    return true; // If dump handling is already in progress, don't force the second dump
                }

                // no reason (and may be dangerous) to send another force dump command
                checkForTargetVMAlive();
                forceObtainedResultsDumpCalled = true;
                sendSimpleCmdToServer(liveResults ? Command.DUMP_EXISTING_RESULTS_LIVE : Command.DUMP_EXISTING_RESULTS);

                DumpResultsResponse resp = (DumpResultsResponse) getLastResponse();

                if (resp.yes()) {
                    status.dumpAbsTimeStamp = resp.getDumpAbsTimeStamp();
                } else {
                    if (ProfilerLogger.isDebug()) {
                        ProfilerLogger.debug("Force Obtained Results - Received Dump Error "); // NOI18N
                    }
                }

                forceObtainedResultsDumpCalled = false;

                return resp.yes();
            }
        }
    }

    /**
     * This should be called to initiate code region instrumentation for specified code region.
     * The data is remembered or sent to the server immediately if TA is already running. The actual instrumentation
     * starts when server informs the tool that the class to be instrumented is loaded.
     */
    public void initiateCodeRegionInstrumentation(ClientUtils.SourceCodeSelection[] s)
                                           throws ClassNotFoundException, BadLocationException, InstrumentationException,
                                                  IOException, ClassFormatError, ClientUtils.TargetAppOrVMTerminated {
        synchronized (instrumentationLock) {
            removeAllInstrumentation();

            if (status.targetAppRunning && status.remoteProfiling) {
                if (!getCalibrationData(true)) {
                    return;
                }
            }

            instrumentor.setStatusInfoFromSourceCodeSelection(s);
            instrumentor.setSavedSourceCodeSelection(s);

            String className = instrumentor.getRootClassNames()[ProfilingSessionStatus.CODE_REGION_CLASS_IDX].replace('/', '.'); // NOI18N
            InitiateProfilingCommand cmd = new InitiateProfilingCommand(INSTR_CODE_REGION, className, false,
                                                                                    status.startProfilingPointsActive);
            commandOnStartup = cmd;

            setCurrentInstrType(INSTR_CODE_REGION);

            if (status.targetAppRunning) {
                sendSetInstrumentationParamsCmd(false);

                String errorMessage = sendCommandAndGetResponse(commandOnStartup);

                if (errorMessage != null) {
                    appStatusHandler.displayWarning(errorMessage);
                }
            }
        }
    }

    public void initiateMonitoring()  throws ClientUtils.TargetAppOrVMTerminated, InstrumentationException {
         synchronized (instrumentationLock) {
            removeAllInstrumentation();
            InitiateProfilingCommand cmd = new InitiateProfilingCommand(INSTR_NONE);
            commandOnStartup = cmd;
            // just to be consistent, since removeAllInstrumentation()
            // sets instrumentation type to INSTR_NONE
            setCurrentInstrType(INSTR_NONE);

            if (status.targetAppRunning) {
                sendSetInstrumentationParamsCmd(false);

                String errorMessage = sendCommandAndGetResponse(commandOnStartup);

                if (errorMessage != null) {
                    appStatusHandler.displayWarning(errorMessage);
                }
            }            
         }
    }

    /**
     * This should be called to initiate memory profiling instrumentation of specified type (object allocation or
     * object liveness).
     * The data is remembered or sent to the server immediately if TA is already running. The actual instrumentation
     * starts when the TA is started and the first class of this app is loaded, or immediately if TA is already running.
     */
    public void initiateMemoryProfInstrumentation(int instrType)
                                           throws ClientUtils.TargetAppOrVMTerminated, InstrumentationException {
        synchronized (instrumentationLock) {
            removeAllInstrumentation();

            if (instrType == INSTR_NONE_MEMORY_SAMPLING) {
                commandOnStartup = new InitiateProfilingCommand(INSTR_NONE_MEMORY_SAMPLING);
            } else {
                // Set this root class name irrespective of whether the target app has been started or not.
                // If it's not yet started, then indeed instrumentation should be triggered by main class load event - otherwise
                // the first loaded class that we register in the server is some reflection class loaded in process of main()
                // invocation. It causes recursive invocations of classLoadHook() (because it also uses some reflection), thus
                // screwing up the instrumentation procedure.
                // If the target app is already running, then instrumentation starts immediately and isn't triggered by a class
                // load event. However, if the same cmd that we build here is then re-used as commandOnStartup, it should again
                // contain rootClassName.
                String[] rootClassNames = new String[]{settings.getMainClassName()};
                commandOnStartup = createInitiateInstrumnetation(instrType, rootClassNames, false, status.startProfilingPointsActive);

                //      switch (instrType) {
                //        case INSTR_OBJECT_ALLOCATIONS:
                //          mcgb = new ObjAllocCallGraphBuilder(this);
                //          break;
                //        case INSTR_OBJECT_LIVENESS:
                //          mcgb = new ObjLivenessCallGraphBuilder(this);
                //          break;
                //      }

                // See initiateRecursiveCPUProfInstrumentation for why it's important to setCurrentInstrType() early
            }
            setCurrentInstrType(instrType);

            if (status.targetAppRunning) {
                sendSetInstrumentationParamsCmd(false);

                String errorMessage = sendCommandAndGetResponse(commandOnStartup);

                if (errorMessage != null) {
                    appStatusHandler.displayWarning(errorMessage);
                }
            }

            //instrMethodGroupFromRootComplete = false;
        }
    }

    /**
     * This should be called to initiate CPU profiling instrumentation starting from specified root method(s).
     * The data is remembered or sent to the server immediately if TA is already running. The actual instrumentation
     * starts when server informs the tool that one of the classes to be instrumented is loaded.
     */
    public void initiateRecursiveCPUProfInstrumentation(ClientUtils.SourceCodeSelection[] s)
                                                 throws ClassNotFoundException, BadLocationException, InstrumentationException,
                                                        IOException, ClassFormatError, ClientUtils.TargetAppOrVMTerminated {
        //    System.out.println("Initiating CPU instrumentation");
        //    for(int i=0;i<s.length;i++) {
        //      System.out.println(s[i]);
        //    }
        synchronized (instrumentationLock) {
            removeAllInstrumentation();

            if (status.targetAppRunning && status.remoteProfiling) {
                if (!getCalibrationData(true)) {
                    return;
                }
            }

            instrumentor.setStatusInfoFromSourceCodeSelection(s);

            boolean instrSpawnedThreads = settings.getInstrumentSpawnedThreads();

            String[] rootClassNames = instrumentor.getRootClassNames();
            int instrType = (settings.getCPUProfilingType() == CPU_INSTR_FULL) ? INSTR_RECURSIVE_FULL : INSTR_RECURSIVE_SAMPLED;
            InitiateProfilingCommand cmd = createInitiateInstrumnetation(instrType, rootClassNames,
                                                                                    instrSpawnedThreads,
                                                                                    status.startProfilingPointsActive);
            commandOnStartup = cmd;
            status.setTimerTypes(settings.getAbsoluteTimerOn(), settings.getThreadCPUTimerOn());

            // the following code is moved to the CPUCallGraphBuilder.startup() method
            //      switch (instrType) {
            //        case INSTR_RECURSIVE_FULL:
            //          ccgb = new FullInstrCPUCallGraphBuilder(this);
            //          break;
            //        case INSTR_RECURSIVE_SAMPLED:
            //          ccgb = new SampledInstrCPUCallGraphBuilder(this);
            //          break;
            //      }

            // It's important that we set current instr type *before* we make the following call. That's because,
            // if targetAppRunning, at the server side all the operations in reaction to the commandOnStartup are performed
            // in a separate thread. It appears that that thread may quickly send back the response with loaded classes etc..,
            // and that may happen *before* we get response below and set currentInstrType. This fixes that
            // (essentially, race condition) bug.
            setCurrentInstrType(instrType);

            if (status.targetAppRunning) {
                sendSetInstrumentationParamsCmd(false);

                String errorMessage = sendCommandAndGetResponse(commandOnStartup);

                if (errorMessage != null) {
                    appStatusHandler.displayWarning(errorMessage);
                }
            }

            //      CPUResultsDispatcher.getInstance().setProfilerClient(this); // initialize CPUResultsDispatcher
            //instrMethodGroupFromRootComplete = false;
        }
    }

    public void initiateCPUSampling() throws ClientUtils.TargetAppOrVMTerminated, InstrumentationException {
         synchronized (instrumentationLock) {
            removeAllInstrumentation();
            InitiateProfilingCommand cmd = new InitiateProfilingCommand(INSTR_NONE_SAMPLING);
            commandOnStartup = cmd;
            status.setTimerTypes(settings.getAbsoluteTimerOn(), settings.getThreadCPUTimerOn());
            setCurrentInstrType(INSTR_NONE_SAMPLING);

            if (status.targetAppRunning) {
                sendSetInstrumentationParamsCmd(false);

                String errorMessage = sendCommandAndGetResponse(commandOnStartup);

                if (errorMessage != null) {
                    appStatusHandler.displayWarning(errorMessage);
                }
            }            
         }
    }

    public synchronized boolean memoryResultsExist() {
        return (getMemoryCCTProvider() != null) && (getMemoryCCTProvider().getStacksForClasses() != null);
    }

    /*
     * A callback method to be executed at the beginning of profiling;
     * will disappear once the snapshot generation routine is rewritten
     */
    public void registerCPUCCTProvider(CPUCCTProvider provider) {
        cpuCctProvider = provider;
    }

    public void registerFlatProfileProvider(FlatProfileProvider provider) {
        flatProvider = provider;
    }

    public void registerMemoryCCTProvider(MemoryCCTProvider provider) {
        memCctProvider = provider;
    }

    public void removeAllInstrumentation(boolean cleanupClient)
                                  throws InstrumentationException {
        synchronized (instrumentationLock) {
            if (getCurrentInstrType() == INSTR_NONE) {
                return;
            }

            commandOnStartup = null;

            if (cleanupClient) {
                status.resetInstrClassAndMethodInfo();
            }

            try {
                clearPreviousInstrumentationInServer();
            } catch (ClientUtils.TargetAppOrVMTerminated ex) { /* So be it */
            }

            setCurrentInstrType(INSTR_NONE);

            //instrMethodGroupFromRootComplete = true;  // False means we are awaiting this event or it's being processed
        }
    }

    public void removeAllInstrumentation() throws InstrumentationException {
        removeAllInstrumentation(true);
    }

    /**
     * If the target VM is terminated, cleans up all locally cached data so that they can be
     * GCd from the memory. If the TA is running, this method does nothing.
     */
    public void resetClientData() {
        if (targetJVMIsAlive()) {
            return; // we should not do any of the following in this case
        }

        status.resetInstrClassAndMethodInfo();
        instrumentor.resetPerVMInstanceData();

        //    CPUResultsDispatcher.getInstance().stop();
        //    mcgb = null;
        //    CPUResultsDispatcher.getInstance().shutdown();
    }

    public synchronized void resetProfilerCollectors()
                                              throws ClientUtils.TargetAppOrVMTerminated {
        checkForTargetVMAlive();
        sendSimpleCmdToServer(Command.RESET_PROFILER_COLLECTORS);
        getAndCheckLastResponse("Unknown problem when trying to reset profiler collectors."); // NOI18N
    }

    public synchronized void resumeTargetAppThreads() throws ClientUtils.TargetAppOrVMTerminated {
        checkForTargetAppRunning();
        sendSimpleCmdToServer(Command.RESUME_TARGET_APP);
        getAndCheckLastResponse("Unknown problem when trying to resume app threads."); // NOI18N
    }

    public synchronized void runGC() throws ClientUtils.TargetAppOrVMTerminated {
        checkForTargetVMAlive();
        sendSimpleCmdToServer(Command.RUN_GC);
        getAndCheckLastResponse("Unknown problem when trying to run GC"); // NOI18N
    }

    public void sendSetInstrumentationParamsCmd(boolean changeableOnly)
                                         throws ClientUtils.TargetAppOrVMTerminated {
        SetChangeableInstrParamsCommand cmd = new SetChangeableInstrParamsCommand(settings.isLockContentionMonitoringEnabled(),
                                                                                  settings.getNProfiledThreadsLimit(),
                                                                                  settings.getSamplingInterval(),
                                                                                  settings.getAllocTrackEvery(),
                                                                                  settings.getAllocStackTraceLimit(),
                                                                                  settings.getRunGCOnGetResultsInMemoryProfiling(),
                                                                                  settings.getExcludeWaitTime(),
                                                                                  settings.getExcludeWaitTime(),
                                                                                  settings.isThreadsSamplingEnabled(),
                                                                                  settings.getSamplingFrequency());

        String errorMessage = sendCommandAndGetResponse(cmd);

        if (errorMessage != null) {
            appStatusHandler.displayWarning(errorMessage);
        }

        if (!changeableOnly) {
            SetUnchangeableInstrParamsCommand cmd1 = new SetUnchangeableInstrParamsCommand(status.remoteProfiling,
                                                                                           settings.getAbsoluteTimerOn(),
                                                                                           settings.getThreadCPUTimerOn(),
                                                                                           settings.getInstrScheme(),
                                                                                           settings.getCodeRegionCPUResBufSize());
            errorMessage = sendCommandAndGetResponse(cmd1);

            if (errorMessage != null) {
                appStatusHandler.displayWarning(errorMessage);
            }
        }
    }

    /**
     * This method is called both when the application is started by the tool, and when the tool attaches to a running
     * application.
     * It's called *after* the establishConnectionWithServer() above.
     * sendExplicitStartCommand actually determines the mode - it's true if we really start the VM as opposed to
     * attaching.
     */
    public boolean startTargetApp(boolean sendExplicitStartCommand)
                           throws ClientUtils.TargetAppOrVMTerminated, ClientUtils.TargetAppFailedToStart {
        status.resetInstrClassAndMethodInfo();
        instrumentor.resetPerVMInstanceData();
        status.setTimerTypes(settings.getAbsoluteTimerOn(), settings.getThreadCPUTimerOn());
        serverCommandHandler.handleServerCommand(null); // To reset the displayed figures
        checkForTargetVMAlive();
        instrProcessingTime = 0;
        instrMethodsLimitReported = false;

        // Special treatment of the case when instrumentation type is changed between runs by simply
        // switching a button in Settings
        if (currentInstrTypeIsRecursiveCPUProfiling()) {
            setCurrentInstrType((settings.getCPUProfilingType() == CPU_INSTR_FULL) ? INSTR_RECURSIVE_FULL : INSTR_RECURSIVE_SAMPLED);

            if (commandOnStartup != null) {
                commandOnStartup.setInstrType(getCurrentInstrType());
            }
        }

        if (commandOnStartup != null) {
            // Always set the current instrumentation parameters first
            sendSetInstrumentationParamsCmd(false);

            switch (getCurrentInstrType()) {
                case INSTR_CODE_REGION:

                    if (status.remoteProfiling && !getCalibrationData(true)) {
                        try {
                            terminateTargetJVM();
                        } catch (ClientUtils.TargetAppOrVMTerminated e) {
                        }

                        return false;
                    }

                    break;
                case INSTR_RECURSIVE_FULL:
                case INSTR_RECURSIVE_SAMPLED:

                    if (status.remoteProfiling && !getCalibrationData(true)) {
                        try {
                            terminateTargetJVM();
                        } catch (ClientUtils.TargetAppOrVMTerminated e) {
                        }

                        return false;
                    }

                    if (settings.getInstrumentMethodInvoke()) {
                        String error = sendSimpleCommandAndGetResponse(Command.INSTRUMENT_REFLECTION);

                        if (error != null) {
                            throw new ClientUtils.TargetAppFailedToStart(error);
                        }
                    }

                    // the following code is moved to the CPUCallGraphBuilder.startup() method
                    //          if (getCurrentInstrType() == INSTR_RECURSIVE_FULL) {
                    //            ccgb = new FullInstrCPUCallGraphBuilder(this);
                    //          } else {
                    //            ccgb = new SampledInstrCPUCallGraphBuilder(this);
                    //          }
                    //          CPUResultsDispatcher.getInstance().setProfilerClient(this);  // initialize CPUResultsDispatcher
                    break;

                //        case INSTR_OBJECT_ALLOCATIONS:
                //          mcgb = new ObjAllocCallGraphBuilder(this);
                //          break;
                //        case INSTR_OBJECT_LIVENESS:
                //          mcgb = new ObjLivenessCallGraphBuilder(this);
                //          break;
            }

            String errorMessage = sendCommandAndGetResponse(commandOnStartup);

            if (errorMessage != null) {
                appStatusHandler.displayWarning("Profiler Agent Error: " + errorMessage); // NOI18N
            }

            commandOnStartup = null;
        } else {
            // Needed to e.g. prevent initiateProfiling() called later from attempting to
            // remove instrumentation from VM.
            setCurrentInstrType(INSTR_NONE);
        }

        if (sendExplicitStartCommand) {
            String error = sendSimpleCommandAndGetResponse(Command.START_TARGET_APP);

            if (error != null) {
                throw new ClientUtils.TargetAppFailedToStart(error);
            }
        }

        status.targetAppRunning = true;
        checkForInstrMethodsLimitReached();
        EventBufferResultsProvider.getDefault().startup(this);

        return true;
    }

    //---------------- Target Application Thread Management ----------------
    public synchronized void suspendTargetAppThreads()
                                              throws ClientUtils.TargetAppOrVMTerminated {
        checkForTargetAppRunning();
        sendSimpleCmdToServer(Command.SUSPEND_TARGET_APP);
        getAndCheckLastResponse("Unknown problem when trying to suspend app threads."); // NOI18N
    }

    public synchronized boolean takeHeapDump(String outputFile)
                                      throws ClientUtils.TargetAppOrVMTerminated {
        checkForTargetVMAlive();
        sendComplexCmdToServer(new TakeHeapDumpCommand(outputFile));

        Response resp = getAndCheckLastResponse("takeHeapDump."); // NOI18N

        return resp.isOK();
    }

    public boolean targetAppIsRunning() {
        return status.targetAppRunning;
    }

    public boolean targetJVMIsAlive() {
        return targetVMAlive;
    }

    //---------------- Target Application/JVM Status Management ----------------
    public synchronized void terminateTargetJVM() throws ClientUtils.TargetAppOrVMTerminated {
        checkForTargetVMAlive();
        terminateOrDetachCommandIssued = true;
        sendSimpleCmdToServer(Command.TERMINATE_TARGET_JVM);

        if (!getLastResponse().isOK()) {
            throw new ClientUtils.TargetAppOrVMTerminated(ClientUtils.TargetAppOrVMTerminated.VM,
                                                          "Target JVM terminated or not responding" // NOI18N
            );
        }

        closeConnection();
    }

    private synchronized Response getAndCheckLastResponse(String errMessage)
                                                   throws ClientUtils.TargetAppOrVMTerminated {
        Response resp = getLastResponse();

        if (!resp.isOK()) {
            MiscUtils.printErrorMessage("error in getAndCheckLastResponse: for " + resp + " got error message: " // NOI18N
                                        + resp.getErrorMessage() + " and context message " + errMessage // NOI18N
                                        );
        }

        return resp;
    }

    private synchronized boolean getCalibrationData(boolean getStoredData)
                                             throws ClientUtils.TargetAppOrVMTerminated {
        int cmdType = getStoredData ? Command.GET_STORED_CALIBRATION_DATA : Command.RUN_CALIBRATION_AND_GET_DATA;
        sendSimpleCmdToServer(cmdType);

        Response resp = getLastResponse();

        if (!resp.isOK()) {
            String msg = resp.getErrorMessage();

            if (getStoredData) {
                msg = MessageFormat.format(CORRUPTED_TARGET_CALIBRATION_DATA_MSG, new Object[] { msg });
            }

            appStatusHandler.displayError(msg);

            return false;
        }

        CalibrationDataResponse cdr = (CalibrationDataResponse) resp;
        status.methodEntryExitCallTime = cdr.getMethodEntryExitCallTime();
        status.methodEntryExitInnerTime = cdr.getMethodEntryExitInnerTime();
        status.methodEntryExitOuterTime = cdr.getMethodEntryExitOuterTime();
        status.timerCountsInSecond = cdr.getTimerCountsInSecond();

        return true;
    }

    private void setLastResponse(Response r) {
        synchronized (responseLock) {
            lastResponse = r;

            try {
                responseLock.notify();
            } catch (IllegalMonitorStateException ex) {
                MiscUtils.internalError("ProfilerClient.setLastResponse()"); // NOI18N
            }
        }
    }

    private synchronized Response getLastResponse() throws ClientUtils.TargetAppOrVMTerminated {
        Response res;
        
        checkForTargetVMAlive();
        synchronized (responseLock) {
            while (lastResponse == null) {
                long start = System.currentTimeMillis();
                
                try {
                    responseLock.wait(60000);
                } catch (InterruptedException ex) {
                    MiscUtils.internalError("InterruptedException in ProfilerClient.getLastResponse()"); // NOI18N
                }
                
                // If we have been waiting for above number of milliseconds and got no response, assume that we timed out
                // and target JVM is dead
                if (!targetVMAlive) {
                    status.targetAppRunning = false;
                    throw new ClientUtils.TargetAppOrVMTerminated(ClientUtils.TargetAppOrVMTerminated.VM);
                } else if (lastResponse == null && wireIO.wasAlive()<start) { // timed out
                    if (!appStatusHandler.confirmWaitForConnectionReply()) {
                        status.targetAppRunning = false;
                        targetVMAlive = false;
                        throw new ClientUtils.TargetAppOrVMTerminated(ClientUtils.TargetAppOrVMTerminated.VM);
                    }
                }
            }
            res = lastResponse;
            lastResponse = null;
        }

        return res;
    }

    /**
     * Set at least some of the properties related to execution of the target JVM.
     * If we attach to the target VM on-the-fly, we need to get everything from it.
     * Otherwise, there are still some properties that we can guess in principle, but which we better ask the VM
     * for, such as Java extension class path dirs and Java boot class path.
     */
    private boolean setVMProperties(VMPropertiesResponse resp, boolean terminateOnError) {
        if (resp.getAgentVersion() != CommonConstants.CURRENT_AGENT_VERSION) {
            appStatusHandler.displayWarning(INCORRECT_AGENT_VERSION_MSG);
        }

        // Check if the VM version is supported by the Profiler
        String jdkVersionString = resp.getJDKVersionString();

        if (!MiscUtils.isSupportedRunningJVMVersion(jdkVersionString)) {
            String message = MessageFormat.format(UNSUPPORTED_JVM_MSG, new Object[] { jdkVersionString });
            appStatusHandler.displayErrorAndWaitForConfirm(message);

            try {
                if (terminateOnError) {
                    terminateTargetJVM();
                } else {
                    detachFromTargetJVM();
                }
            } catch (ClientUtils.TargetAppOrVMTerminated ex) {
            }

            return false;
        }

        // Check the VM version, and if it doesn't match the one set in the tool, check if we have saved calibration data
        // for this VM version
        String jdkVersionName = Platform.getJDKVersionString(jdkVersionString);
        settings.setTargetJDKVersionString(jdkVersionName);
        status.targetJDKVersionString = jdkVersionName;
        status.fullTargetJDKVersionString = jdkVersionString;
        currentAgentId = resp.getAgentId();

        if (!status.remoteProfiling) {
            int res = CalibrationDataFileIO.readSavedCalibrationData(status);

            if (res < 0) { // Fatal error with reading saved file data - report the details

                String message = MessageFormat.format(ERROR_GETTING_CALIBRATION_DATA_MSG,
                                                      new Object[] { CalibrationDataFileIO.getErrorMessage() });
                appStatusHandler.displayErrorAndWaitForConfirm(message);

                return false;
            } else if (res > 0) { // Saved data file doesn't exist - notify the user and stop
                appStatusHandler.displayErrorWithDetailsAndWaitForConfirm(MUST_CALIBRATE_FIRST_SHORT_MSG, MUST_CALIBRATE_FIRST_MSG);

                try {
                    if (terminateOnError) {
                        terminateTargetJVM();
                    } else {
                        detachFromTargetJVM();
                    }
                } catch (ClientUtils.TargetAppOrVMTerminated ex) {
                }

                return false;
            }
        }

        status.jvmArguments = resp.getJVMArguments();
        status.javaCommand = resp.getJavaCommand();
        status.targetMachineOSName = resp.getTargetMachineOSName();
        status.maxHeapSize = resp.getMaxHeapSize();
        status.startupTimeMillis = resp.getStartupTimeMillis();
        status.startupTimeInCounts = resp.getStartupTimeInCounts();

        if (!status.remoteProfiling) {
            settings.setWorkingDir(resp.getWorkingDir());
            settings.setVMClassPaths(resp.getJavaClassPath(), resp.getJavaExtDirs(), resp.getBootClassPath());
            ClassRepository.initClassPaths(settings.getWorkingDir(), settings.getVMClassPaths());
        }

        return true;
    }

    /**
     * Check if we can't instrument more methods because the 64K limit is reached
     */
    private void checkForInstrMethodsLimitReached() {
        if ((status.getStartingMethodId() >= 65535) && !instrMethodsLimitReported && status.targetAppRunning) {
            appStatusHandler.displayWarningAndWaitForConfirm(INSTRUMENTATION_LIMIT_REACHED_MSG);
            instrMethodsLimitReported = true;
        }
    }

    private void checkForTargetAppRunning() throws ClientUtils.TargetAppOrVMTerminated {
        if (!status.targetAppRunning) {
            serverCommandHandler.handleServerCommand(null);
            throw new ClientUtils.TargetAppOrVMTerminated(ClientUtils.TargetAppOrVMTerminated.APP);
        }
    }

    private void checkForTargetVMAlive() throws ClientUtils.TargetAppOrVMTerminated {
        if (!targetVMAlive) {
            serverCommandHandler.handleServerCommand(null);
            throw new ClientUtils.TargetAppOrVMTerminated(ClientUtils.TargetAppOrVMTerminated.VM);
        }
    }

    //-------------------------------- Private implementation ------------------------------------------
    private void clearPreviousInstrumentationInServer()
                                               throws InstrumentationException, ClientUtils.TargetAppOrVMTerminated {
        Response resp = null;
        checkForTargetAppRunning();

        // First send the command that will make the application stop emitting events
        // But avoid doing that while we are processing the data at the client side, since it looks like when these two
        // things happen at the same time, it's likely to cause problems.
        // This is a quick fix. Probably a more solid solution is needed.
        if (handlingEventBufferDump) {
            while (handlingEventBufferDump) {
                try {
                    Thread.sleep(20);
                } catch (Exception ex) {
                }
            }
        }

        String error = sendSimpleCommandAndGetResponse(Command.DEACTIVATE_INJECTED_CODE);

        if (error != null) {
            throw new InstrumentationException(error);
        }

        long curTime = System.currentTimeMillis();

        // Now actually de-instrument the instrumented methods
        InstrumentMethodGroupCommand cmd = instrumentor.createClearAllInstrumentationCommand();

        synchronized (this) {
            sendComplexCmdToServer(cmd);
            instrProcessingTime += (System.currentTimeMillis() - curTime);
            resp = getLastResponse();
        }

        if (!resp.isOK()) {
            throw new InstrumentationException(resp.getErrorMessage());
        }
    }

    private void closeConnection() {
        if (!serverListener.isRunning()) {
            return;
        }

        try {
            status.targetAppRunning = false;
            targetVMAlive = false;
            serverListener.shutdown();
            setLastResponse(null); // This is important, in case smb. is waiting for the response
            socketOut.close();
            socketIn.close();
            clientSocket.close();

            // This is kind of "black magic", that is needed when we hit "Run" without explicitly terminating
            // the previous target JVM. If this pause is not made here, then for some reason we get:
            // "SocketException: Connection reset by peer: JVM_recv in socket input stream read"  in connectToServer().
            // I don't like this way of dealing with this problem - need to investigate why it really happens
            try {
                Thread.sleep(400);
            } catch (InterruptedException e) {
            }

            //serverCommandHandler.handleServerCommand(null); // does not seem to do anything
        } catch (IOException ex) {
            // Don't do anything
        } finally {
            EventBufferResultsProvider.getDefault().shutdown();
            EventBufferProcessor.removeEventBufferFile();
        }
    }

    private boolean connectToServer(int attachMode, boolean calibrationOnlyRun, final AtomicBoolean cancel) {
        status.targetAppRunning = false;
        targetVMAlive = false;
        terminateOrDetachCommandIssued = false;

        String taHost = (attachMode == 1) ? settings.getRemoteHost() : ""; // NOI18N

        if (taHost.isEmpty()) {
            status.remoteProfiling = false;
            taHost = "127.0.0.1"; // NOI18N
        } else {
            status.remoteProfiling = true;
        }

        final String host = taHost;
        final int port = settings.getPortNo();
        
        int noOfCycles = 600; // Timeout is set to 150 sec
        
        Runnable cancelHandler = new Runnable() {
            public void run() {
                cancel.set(true);
                serverListener.cancel(); 
            }
        };
        AppStatusHandler.AsyncDialog waitDialog =
                appStatusHandler.getAsyncDialogInstance(CONNECT_VM_MSG, true, cancelHandler);
        
        try {
            serverListener = new ServerListener();
            waitDialog.display();
            serverListener.start();

            while (!cancel.get()) {
                try {
                    clientSocket = new Socket(host, port);
                    clientSocket.setSoTimeout(0); // ATTENTION: timeout may be found useful eventually...
                    clientSocket.setTcpNoDelay(true); // Necessary at least on Solaris to avoid delays in e.g. readInt() etc.
                    socketOut = new ObjectOutputStream(clientSocket.getOutputStream());
                    socketIn = new ObjectInputStream(clientSocket.getInputStream());
                    wireIO = new WireIO(socketOut, socketIn);

                    targetVMAlive = true; // This is in fact an assumption
                    serverListener.startRunning();
                    break;
                } catch (ConnectException ex) {
                    // ex.printStackTrace (System.err);
                    try {
                        Thread.sleep(250);
                    } catch (InterruptedException iex) {
                    }

                    if (--noOfCycles == 0) {
                        MiscUtils.printWarningMessage("timed out while trying to connect to the target JVM."); // NOI18N
                        serverListener.cancel();
                        break;
                    }
                }
            }
        } catch (Exception ex) { // SocketException, UnknownHostException, IOException
            MiscUtils.printErrorMessage("exception while trying to connect to the target JVM:\n" + ex); // NOI18N
        } finally {
            waitDialog.close();
        }

        if (!serverListener.isRunning()) {
            MiscUtils.printErrorMessage("connection with server not open"); // NOI18N

            return false;
        }

        // Now check the connection and do other preparation work
        try {
            String error = sendSimpleCommandAndGetResponse(Command.CHECK_CONNECTION);

            if (error != null) {
                targetVMAlive = false;
                MiscUtils.printErrorMessage("got error message from agent:" + error); // NOI18N

                return false;
            }

            if (calibrationOnlyRun) {
                //        System.err.println("G1");
                boolean res = getCalibrationData(false);

                //        System.err.println("G2: "+res);
                try {
                    terminateTargetJVM();
                } catch (ClientUtils.TargetAppOrVMTerminated e) {
                    ProfilerLogger.log("terminateTargetJVM failed with TargetAppOrVMTerminated exception:"); // NOI18N
                    ProfilerLogger.log(e.getMessage());

                    // this is OK here
                }

                //        System.err.println("G3");
                return res;
            }

            boolean terminateOnError = attachMode != 2; // in case of direct attach we don't want to have a JVM process hanging around waiting for the client to connect
                                                        // Get VM properties

            synchronized (this) {
                sendSimpleCmdToServer(Command.GET_VM_PROPERTIES);

                Response aResponse = getLastResponse();

                if (!(aResponse instanceof VMPropertiesResponse)) {
                    System.err.println("SEVERE: Received " + aResponse.getClass().getName() + "(" + aResponse.toString() // NOI18N
                                       + ") instead of VMPropertiesResponse"); // NOI18N
                }

                if (!setVMProperties((VMPropertiesResponse) aResponse, terminateOnError)) {
                    return false;
                }
            }

            // Send a command to initiate the fake RootClassLoadedCommand cycle, that forces initialization of some internal
            // server classes
            serverClassesInitialized = false;
            // Note that here we can't use normal getCmd(), since this shared object could already have been initialized with
            // real data.
            error = sendCommandAndGetResponse(new InitiateProfilingCommand(INSTR_RECURSIVE_FULL,
                                                                                 "*FAKE_CLASS_FOR_INTERNAL_TEST*") // NOI18N
            );

            if (error != null) {
                MiscUtils.printErrorMessage("got error message from agent:" + error); // NOI18N
                targetVMAlive = false;

                return false;
            }

            noOfCycles = 20;

            while (!serverClassesInitialized && (--noOfCycles > 0)) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                }
            }

            if (!serverClassesInitialized) {
                MiscUtils.printErrorMessage("timed out while trying to initialize internals in the target JVM."); // NOI18N
                targetVMAlive = false;

                return false;
            }

            try {
                Thread.sleep(100); // To make sure everything has finished on the server side.
            } catch (InterruptedException ex) {
            }
        } catch (ClientUtils.TargetAppOrVMTerminated ex) {
            targetVMAlive = false;
            MiscUtils.printWarningMessage("target app terminated:" + ex.getMessage()); // NOI18N

            return false;
        }

        return true;
    }

    /**
     * Some commands, e.g. those related to instrumentation, are executed in a separate thread, since they may in turn
     * send requests and await response from the server. Thus the listener thread, that calls this method, should be made
     * available quickly so that it can listen for the server again.
     */
    private void executeInSeparateThread(Command cmd) {
        synchronized (execInSeparateThreadLock) {
            execInSeparateThreadCmd = cmd;

            try {
                execInSeparateThreadLock.notify();
            } catch (IllegalMonitorStateException ex) {
                MiscUtils.internalError("ProfilerClient.executeInSeparateThread()"); // NOI18N
            }
        }
    }

    private void startSeparateCmdExecThread() {
        assert separateCmdExecThread == null;
        SeparateCmdExecutionThread t = new SeparateCmdExecutionThread();
        t.setDaemon(true);
        t.start();
        separateCmdExecThread = t;
    }
    
    private void stopSeparateCmdExecThread() {
        assert separateCmdExecThread != null;
        executeInSeparateThread(null); // stop thread
        separateCmdExecThread = null;
    }

    private boolean handleFakeClassLoad(RootClassLoadedCommand cmd) {
        if (cmd.getAllLoadedClassNames()[0].equals("*FAKE_CLASS_1*")) { // NOI18N
            sendComplexRespToServer(new InstrumentMethodGroupResponse(new String[] { "*FAKE_CLASS_1*", "*FAKE_CLASS_2*" },
                                                                      new int[] { 0, 0 }, new byte[][] {
                                                                          { 0 },
                                                                          { 0 }
                                                                      }, null, 0));
            serverClassesInitialized = true;

            return true;
        } else {
            return false;
        }
    }

    private void handleIOExceptionOnSend(IOException ex)
                                  throws ClientUtils.TargetAppOrVMTerminated {
        checkForTargetVMAlive();
        // For now, assume that the server went away. TODO [misha] - can it happen for any other reason?
        appStatusHandler.displayError(MessageFormat.format(TARGET_JVM_ERROR_MSG, new Object[] { ex.getMessage() }));
        closeConnection();
        throw new ClientUtils.TargetAppOrVMTerminated(ClientUtils.TargetAppOrVMTerminated.VM);
    }

    private void instrumentMethodGroupFollowUp(Command cmd) {
        synchronized (instrumentationLock) {
            long curTime = System.currentTimeMillis();
            InstrumentMethodGroupResponse imgr = instrumentor.createFollowUpInstrumentMethodGroupResponse(cmd);
            instrProcessingTime += (System.currentTimeMillis() - curTime);
            //if (imgr != null && ! imgr.isEmpty()) {
            // System.err.println("*** Profiler Engine: instrumentMethodGroupFollowUp() produced response:");
            // imgr.dump();
            // }
            sendComplexRespToServer(imgr);
        }

        checkForInstrMethodsLimitReached();
    }

    private void instrumentMethodGroupFromRoot(final RootClassLoadedCommand cmd) {
        synchronized (instrumentationLock) {
            AppStatusHandler.AsyncDialog waitDialog = null;
            try {
                InstrumentMethodGroupResponse imgr = null;

                if (!serverClassesInitialized) { // Check if it is a fake command from server, used to just pre-initialize
                                                 // some internal server classes

                    if (handleFakeClassLoad(cmd)) {
                        return;
                    }
                }

                appStatusHandler.pauseLiveUpdates();

                if (status.targetAppRunning) {
                    waitDialog = appStatusHandler.getAsyncDialogInstance(PERFORMING_INSTRUMENTATION_STRING, true, null);
                    waitDialog.display();
                }

                // If the application is not running yet, it means that instrumentation is performed on startup. In that case,
                // it typically takes very short time, so there is no real need to display this progress dialog. Additionally,
                // the AWT Event Queue thread may be blocked in the call to startTargetApp, in which case this whole thing will
                // hang (due to getAsyncDialogInstance NB implementation waiting on this thread's lock).
                try {
                    long curTime = System.currentTimeMillis();
                    imgr = instrumentor.createInitialInstrumentMethodGroupResponse(cmd);
                    instrProcessingTime += (System.currentTimeMillis() - curTime);
                } catch (BadLocationException ex) {
                    imgr = new InstrumentMethodGroupResponse(null);
                    // Can currently happen only for INSTR_CODE_REGION
                    appStatusHandler.displayError(INVALID_CODE_REGION_MSG);
                } catch (ClassNotFoundException ex) {
                    imgr = new InstrumentMethodGroupResponse(null);

                    if (getCurrentInstrType() == INSTR_CODE_REGION) {
                        appStatusHandler.displayError(MessageFormat.format(CLASS_NOT_FOUND_MSG, new Object[] { ex.getMessage() }));
                    } else {
                        MiscUtils.printErrorMessage("problem in instrumentMethodGroupFromRoot: " + ex); // NOI18N
                    }
                }

                //if (imgr != null ! imgr.isEmpty()) {
                // System.err.println("*** Profiler Engine: instrumentMethodGroupFromRoot() produced response:");
                // imgr.dump(); }
                // else System.err.println("*** Profiler Engine: instrumentMethodGroupFromRoot() produced empty response");
                sendComplexRespToServer(imgr);
            } finally {
                if (waitDialog != null) {
                    waitDialog.close();
                }

                appStatusHandler.resumeLiveUpdates();
            }
        }
    }

    /**
     * Upon receipt of the BUFFER_FULL command from the server, read and process the buffer contents
     */
    private void readAndProcessProfilingResults(EventBufferDumpedCommand cmd) {
        int bufSize = cmd.getBufSize();

        if (bufSize == 0) { // zero size may happen when dump is forced when there is actually no new information generated
            sendSimpleRespToServer(true, null);

            return;
        }

        handlingEventBufferDump = true;

        // Results of memory/CPU profiling can be processed concurrently to take advantage of a possible multiprocessor machine.
        // Similarly, during remote profiling any results can be processed concurrently, since processing on a different
        // machine will not disturb execution timing on the TA machine. Note also that if this command is
        // received as a result of the forced dump (as opposed to the normal one due to buffer overflow), the data should
        // be processed synchronously to avoid e.g. a "no results" report when there are already some.

        // update [ian] In case of remote profiling we actually can not process the results in concurrently,
        // since there would suddenly be 2 pieces of code that simultaneously read from the socket stream
        // leading to issue 59660: JFluid: error writing collected data to the socket
        // see http://www.netbeans.org/issues/show_bug.cgi?id=59660 for details
        if (!status.remoteProfiling && !forceObtainedResultsDumpCalled) {
            // Note that the call below may block, waiting for separarateCmdExecThread to finish its current job.
            // That means that nothing in readResultsFromBuffer() that this command eventually calls, is allowed to
            // send a command to the server and await a response. If that happens, the communication thread will be
            // unavailable for reading server's response (because it's waiting here), effectively causing a deadlock.
            executeInSeparateThread(cmd);
            handlingEventBufferDump = false;
        } else {
            // Process profiling results synchronously in case of:
            //  - remote profiling
            //  - explicite Get results (forceObtainedResultsDumpCalled)
            byte[] buf = EventBufferProcessor.readDataAndPrepareForProcessing(cmd);
            EventBufferResultsProvider.getDefault().dataReady(buf, getCurrentInstrType());
            handlingEventBufferDump = false;
            sendSimpleRespToServer(true, null);
            forceObtainedResultsDumpCalled = false;
        }
    }

    /**
     * @param cmd Command to send
     * @return null if command was confirmed OK from Agent, Error message otherwise
     * @throws ClientUtils.TargetAppOrVMTerminated
     *
     */
    private synchronized String sendCommandAndGetResponse(Command cmd)
                                                   throws ClientUtils.TargetAppOrVMTerminated {
        sendComplexCmdToServer(cmd);

        Response resp = getLastResponse();

        if (!resp.isOK()) {
            MiscUtils.printErrorMessage("error in sendCommandAndGetResponse: for cmd = " + cmd // NOI18N
                                        + " and resp = " + resp + " got error message: " + resp.getErrorMessage() // NOI18N
            );

            return resp.getErrorMessage();
        } else {
            return null;
        }
    }

    private void sendComplexCmdToServer(Command cmd) throws ClientUtils.TargetAppOrVMTerminated {
        try {
            wireIO.sendComplexCommand(cmd);
        } catch (IOException ex) {
            handleIOExceptionOnSend(ex);
        }
    }

    private void sendComplexRespToServer(Response resp) {
        try {
            wireIO.sendComplexResponse(resp);
        } catch (IOException ex) {
            MiscUtils.printErrorMessage("exception when trying to send a response: " + ex); // NOI18N

            try {
                handleIOExceptionOnSend(ex);
            } catch (ClientUtils.TargetAppOrVMTerminated ex1) { /* All done already */
            }
        }
    }

    private void sendSimpleCmdToServer(int cmdType) throws ClientUtils.TargetAppOrVMTerminated {
        try {
            wireIO.sendSimpleCommand(cmdType);
        } catch (IOException ex) {
            handleIOExceptionOnSend(ex);
        }
    }

    /**
     * @param cmd Command to send
     * @return null if command was confirmed OK from Agent, Error message otherwise
     * @throws ClientUtils.TargetAppOrVMTerminated
     *
     */
    private synchronized String sendSimpleCommandAndGetResponse(int cmdType)
        throws ClientUtils.TargetAppOrVMTerminated {
        sendSimpleCmdToServer(cmdType);

        Response resp = getLastResponse();

        if (!resp.isOK()) {
            MiscUtils.printErrorMessage("error in sendCommandAndGetResponse: for cmdType = " + cmdType // NOI18N
                                        + " and resp = " + resp + " got error message: " + resp.getErrorMessage() // NOI18N
            );

            return resp.getErrorMessage();
        } else {
            return null;
        }
    }

    private void sendSimpleRespToServer(boolean val, String errorMessage) {
        try {
            wireIO.sendSimpleResponse(val, errorMessage);
        } catch (IOException ex) {
            try {
                handleIOExceptionOnSend(ex);
            } catch (ClientUtils.TargetAppOrVMTerminated ex1) { /* All done already */
            }
        }
    }
    
    private InitiateProfilingCommand createInitiateInstrumnetation(int instrType, String[] classNames,
                                          boolean instrSpawnedThreads, boolean startProfilingPointsActive) {
        RuntimeProfilingPoint points[] = settings.getRuntimeProfilingPoints();
        String[] profilingPointHandlers = new String[points.length];
        String[] profilingPointInfos = new String[points.length];
        int[] profilingPointIDs = new int[points.length];
        Arrays.sort(points); // ProfilerRuntime uses Arrays.binarySearch

        for (int i = 0; i < points.length; i++) {
            RuntimeProfilingPoint point = points[i];
            profilingPointIDs[i] = point.getId();
            profilingPointHandlers[i] = point.getServerHandlerClass();
            profilingPointInfos[i] = point.getServerInfo();
        }
        return new InitiateProfilingCommand(instrType,classNames,
                        profilingPointIDs,profilingPointHandlers,profilingPointInfos,
                        instrSpawnedThreads,startProfilingPointsActive);
    }
}
