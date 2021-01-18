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

package org.graalvm.visualvm.lib.jfluid.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.net.URL;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.WeakHashMap;
import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;
import org.graalvm.visualvm.lib.jfluid.global.Platform;
import org.graalvm.visualvm.lib.jfluid.global.ProfilingSessionStatus;
import org.graalvm.visualvm.lib.jfluid.global.TransactionalSupport;
import org.graalvm.visualvm.lib.jfluid.server.system.*;
import org.graalvm.visualvm.lib.jfluid.wireprotocol.*;


/**
 * Main interface to the target VM side introspection functionality.
 *
 * @author Tomas Hurka
 * @author Misha Dmitriev
 * @author Adrian Mos
 * @author Ian Formanek
 */
public class ProfilerInterface implements CommonConstants {

    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private static class HFIRIThread extends Thread {
        //~ Constructors ---------------------------------------------------------------------------------------------------------

        HFIRIThread() {
            ThreadInfo.addProfilerServerThread(this);
            this.setName(PROFILER_SPECIAL_EXEC_THREAD_NAME + " 1"); // NOI18N
            setDaemon(true);
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void run() {
            RootClassLoadedCommand cmd = new RootClassLoadedCommand(new String[] { "*FAKE_CLASS_1*", "*FAKE_CLASS_2*" }, // NOI18N
                                                                    new int[] { 0, 0 }, null, new int[] { 0, 0 }, new int[2][], 2, new int[] { -1 });
            profilerServer.sendComplexCmdToClient(cmd);

            InstrumentMethodGroupResponse imgr = (InstrumentMethodGroupResponse) profilerServer.getLastResponse();
            ThreadInfo.removeProfilerServerThread(this);
        }
    }

    private static class InitiateProfilingThread extends Thread {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private InitiateProfilingCommand cmd;
        private boolean targetAppRunning;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        InitiateProfilingThread(InitiateProfilingCommand cmd, boolean targetAppRunning) {
            ThreadInfo.addProfilerServerThread(this);
            this.setName(PROFILER_SPECIAL_EXEC_THREAD_NAME + " 2"); // NOI18N
            this.cmd = cmd;
            this.targetAppRunning = targetAppRunning;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void run() {
            Monitors.enterServerState(CommonConstants.SERVER_INITIALIZING);
            // We take a serialClientOperationsLock, then turn class load hook on, to prevent possible class loads, that
            // will neither get into loadedClassesArray nor be intercepted properly and reported to client by classLoadHook.
            // In other words, classes that are loaded before this point get into loadedClassesArray; classes loaded
            // afterwards should be individually intercepted and reported to client.
            serialClientOperationsLock.beginTrans(true);

            try {
                initInstrumentationThread = Thread.currentThread();

                int instrType = cmd.getInstrType();
                setCurrentInstrType(instrType);
                switch (instrType) {
                    case INSTR_NONE:
                        // do nothing
                        break;
                    case INSTR_NONE_SAMPLING:
                        ProfilerRuntimeSampler.initialize();
                        break;
                    case INSTR_CODE_REGION:
                    case INSTR_RECURSIVE_FULL:
                    case INSTR_RECURSIVE_SAMPLED:
                    case INSTR_OBJECT_ALLOCATIONS:
                    case INSTR_OBJECT_LIVENESS:
                        initiateInstrumentation(instrType);
                        break;
                    case INSTR_NONE_MEMORY_SAMPLING:
                        if (Histogram.initialize(Platform.getJDKVersionNumber() >= Platform.JDK_19)) {
                            profilerServer.notifyClientOnResultsAvailability();
                        }
                        break;
                    default:
                        throw new IllegalArgumentException("Instr. type: "+instrType);
                }
                initInstrumentationThread = null;
            } finally {
                serialClientOperationsLock.endTrans();
                Monitors.exitServerState();
            }

            ThreadInfo.removeProfilerServerThread(this);
        }

        private void initiateInstrumentation(final int instrType) {
            Monitors.DeterminateProgress progress = Monitors.enterServerState(CommonConstants.SERVER_INITIALIZING, 2);
            int[] profilingPointIDs = cmd.getProfilingPointIDs();
            String[] handlers = cmd.getProfilingPointHandlers();
            String[] infos = cmd.getProfilingPointInfos();
            
            rootClassNames = cmd.getRootClassNames();
            status.startProfilingPointsActive = cmd.isStartProfilingPointsActive();
            ProfilingPointServerHandler.initInstances(profilingPointIDs, handlers, infos);
            computeRootWildcard();
            rootClassLoaded = false;

            // the following code is needed to avoid issue 59660: Remote profiling can cause the agent to hang if CPU
            // or Code Fragment profiling is used
            // see http://profiler.netbeans.org/issues/show_bug.cgi?id=59660
            // and http://profiler.netbeans.org/issues/show_bug.cgi?id=61968
            new LinkedHashMap().keySet().iterator();
            try {
                // for take heap dump
                Class.forName("java.lang.reflect.InvocationTargetException"); // NOI18N
                Class.forName("java.lang.InterruptedException");    // NOI18N
                Class.forName("java.lang.ClassFormatError"); // NOI18N class caching
            } catch (ClassNotFoundException e) {
                e.printStackTrace(System.err);
            }

            if (Platform.getJDKVersionNumber() >= CommonConstants.JDK_19) {
                try {
                    // preload classes for classLoadHook()
                    Class.forName("java.lang.WeakPairMap$Pair"); // NOI18N
                    Class.forName("java.lang.WeakPairMap$WeakRefPeer");    // NOI18N
                    Class.forName("java.lang.WeakPairMap$Pair$Weak"); // NOI18N
                    Class.forName("java.lang.WeakPairMap$Pair$Weak$1"); // NOI18N
                } catch (ClassNotFoundException e) {
                    e.printStackTrace(System.err);
                }
            }
            
            // The following code is needed to enforce native method bind for Thread.sleep before instrumentation, so
            // that the NativeMethodBind it can be disabled as first thing in instrumentation
            // this is needed as a workaround for JDK bug:
            // CR 6318850 Updated P3 hotspot/jvmti RedefineClasses() and NativeMethodBind event crash
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
            } // ignore

            synchronized (this) {
                try {
                    wait(1);
                } catch (InterruptedException e) {
                } // ignore
            }

            // reset loadedClassesArray so that next time it contains current loaded classes
            loadedClassesArray = null;
            loadedClassesLoaders = null;
            Classes.enableClassLoadHook();

            instrSpawnedThreads = cmd.getInstrSpawnedThreads();

            if (targetAppRunning || hasAnyCoreClassNames(cmd.getRootClassNames()) || instrSpawnedThreads
                    || (instrType == INSTR_OBJECT_ALLOCATIONS) || (instrType == INSTR_OBJECT_LIVENESS)) {
                getLoadedClasses(); // Init loadedClassesArray

                boolean loadedRootClassesExist = false;

                if (!detachStarted) {
                    switch (instrType) {
                        case INSTR_RECURSIVE_FULL:
                        case INSTR_RECURSIVE_SAMPLED:
                            // This will look into loadedClassesArray to check if there are any root classes already loaded
                            loadedRootClassesExist = instrSpawnedThreads ? true : checkForLoadedRootClasses();

                            break;
                        case INSTR_CODE_REGION:
                            loadedRootClassesExist = checkForLoadedRootClasses();

                            break;
                        case INSTR_OBJECT_ALLOCATIONS:
                        case INSTR_OBJECT_LIVENESS:
                            loadedRootClassesExist = true;

                            break;
                    }
                }

                progress.next();

                if (loadedRootClassesExist) { // Root class(es) has been loaded or none is needed - start
                                              // instrumentation-related operations right away
                    sendRootClassLoadedCommand(false);

                    if (!getAndInstrumentClasses(true)) {
                        disableProfilerHooks();
                    }

                    rootClassLoaded = true; // See the comment in classLoadHook why it's worth setting rootClassLoaded
                                            // to true after the first instrumentation, not before
                } else {
                    // if root class is not loaded, reset loadedClassesArray so that next time it contains current loaded classes
                    loadedClassesArray = null;
                    loadedClassesLoaders = null;
                }
            }
            Monitors.exitServerState();
        }

        private static void computeRootWildcard() {
            rootClassNameWildcard = new boolean[rootClassNames.length];
            rootClassNamePackageWildcard = new boolean[rootClassNames.length];

            for (int i = 0; i < rootClassNames.length; i++) {
                int nameLen = rootClassNames[i].length();
                rootClassNameWildcard[i] = (nameLen == 0) // default package wildcard
                                           || (rootClassNames[i].charAt(nameLen - 1) == '.'); // ends with "." // NOI18N
                if (!rootClassNameWildcard[i]) {
                    if (rootClassNames[i].charAt(nameLen - 1) == '*') { // package wild card - instrument all classes including subpackages
                        rootClassNames[i] = rootClassNames[i].substring(0,nameLen - 1); // remove *
                        rootClassNameWildcard[i] = true;
                        rootClassNamePackageWildcard[i] = true;
                    }
//                    System.out.println("Root "+rootClassNames[i]+" wild "+rootClassNameWildcard[i]+" package "+rootClassNamePackageWildcard[i]);
                }
            }
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final int REDEFINE_CHUNK_SIZE = 500;
    
    static final char BOOLEAN = 'Z'; // NOI18N
    static final char CHAR = 'C'; // NOI18N
    static final char BYTE = 'B'; // NOI18N
    static final char SHORT = 'S'; // NOI18N
    static final char INT = 'I'; // NOI18N
    static final char LONG = 'J'; // NOI18N
    static final char FLOAT = 'F'; // NOI18N
    static final char DOUBLE = 'D'; // NOI18N
    static final char VOID = 'V'; // NOI18N
    static final char REFERENCE = 'L'; // NOI18N

    // -----
    // I18N String constants
    // !!! Warning - do not use ResourceBundle.getBundle here, won't work in context of direct/dynamic attach !!!
    // Default EN messages initialized here, will be replaced by localized messages in static initializer
    private static String INTERNAL_ERROR_MSG = "Internal error:\nExpected InstrumentMethodGroupResponse, got response of class {0},\nvalue = {1}\nAll instrumentation will be removed"; // NOI18N
    private static String UNEXPECTED_EXCEPTION_MSG = "Unexpected exception caught when trying to instrument classes.\nOriginal exception:\n{0}\nStack trace:\n\n{1}"; // NOI18N
    private static String INSTRUMENTATION_SUCCESSFUL_MSG = "Deferred instrumentation performed successfully"; // NOI18N
    private static String HISTOGRAM_NOT_AVAILABLE_MSG = "Histogram is not available."; // NOI18N
    // -----

    static {
        ResourceBundle messages = ProfilerServer.getProfilerServerResourceBundle();

        if (messages != null) {
            INTERNAL_ERROR_MSG = messages.getString("ProfilerInterface_InternalErrorMsg"); // NOI18N
            UNEXPECTED_EXCEPTION_MSG = messages.getString("ProfilerInterface_UnexpectedExceptionMsg"); // NOI18N
            INSTRUMENTATION_SUCCESSFUL_MSG = messages.getString("ProfilerInterface_InstrumentationSuccessfulMsg"); // NOI18N
            HISTOGRAM_NOT_AVAILABLE_MSG = messages.getString("ProfilerInterface_HistogramNotAvailableMsg");     // NOI18N
        }
    }

    // TODO [release]: change value to FALSE to remove the print code below entirely by compiler
    private static final boolean DEBUG = Boolean.getBoolean("org.graalvm.visualvm.lib.jfluid.server.ProfilerInterface.classLoadHook"); // NOI18N
    private static final boolean INSTRUMENT_JFLUID_CLASSES = Boolean.getBoolean("org.graalvm.visualvm.lib.jfluid.server.instrumentJFluidClasses"); // NOI18N

    private static final byte[] EMPTY = new byte[0];
    
    // The lock used to serialize requests from server to client. May be used outside this class.
    public static final TransactionalSupport serialClientOperationsLock = new TransactionalSupport();
    private static ProfilerServer profilerServer;
    private static ProfilingSessionStatus status;
    private static EventBufferManager evBufManager;
    private static WeakReference[] loadedClassesArray; // Temporary array, used to send all loaded class names to client
                                               // on instrumentation initiation.
    private static int[] loadedClassesLoaders; // Ditto, for loaders
    private static WeakHashMap reflectMethods; // Cache of methods called using reflection
    private static boolean targetAppSuspended = false;
    private static boolean instrumentReflection = false;
    private static boolean instrSpawnedThreads;
    private static int[] packedArrayOffsets;
    private static int nSystemThreads;
    private static Thread initInstrumentationThread;
    private static String[] rootClassNames;
    private static boolean[] rootClassNameWildcard;
    private static boolean[] rootClassNamePackageWildcard;

    // For statistics
    static int nClassLoads;

    // For statistics
    static int nFirstMethodInvocations;

    // For statistics
    static int nEmptyInstrMethodGroupResponses;
    static int nNonEmptyInstrMethodGroupResponses;
    static int nSingleMethodInstrMethodGroupResponses;
    static int nTotalInstrMethods;
    static long totalHotswappingTime;
    static long minHotswappingTime = 10000000000L;
    static long maxHotswappingTime;
    static long clientInstrStartTime;
    static long clientInstrTime;
    static long clientDataProcStartTime;
    static long clientDataProcTime;

    //----------------------------------------- Private implementation --------------------------------------------------
    private static boolean rootClassLoaded; // has root class been loaded?

    // The following variable addresses the issue of classLoadHook called for a class, that is already registered as
    // loaded, (through getAllLoadedClasses) but is actually initialized only when extendConstantPool() is called on it.
    // Initialization would cause classLoadHook invocation on this class, and subsequent "second class load event"
    // messages in client (which is just confusing). But this may also cause more subtle deadlock bug due to
    // classLoadHook() trying to record adjustTime event, while serialClientOperationsLock is held by an outer invocation
    // of classLoadHook() or methodInvokedFirstTime(). To avoid all these problems we use this simple way to avoid
    // unnecessary classLoadHook() invocations.
    private static volatile Thread instrumentMethodGroupCallThread;

    private static volatile boolean detachStarted;
    
    private static HeapHistogramManager heapHistogramManager;

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static CodeRegionCPUResultsResponse getCodeRegionCPUResults() {
        CodeRegionCPUResultsResponse resp = new CodeRegionCPUResultsResponse(ProfilerRuntimeCPUCodeRegion.getProfilingResults());

        return resp;
    }

    public static void setCurrentInstrType(int type) {
        boolean isMemoryProfiling = (type == INSTR_OBJECT_ALLOCATIONS) || (type == INSTR_OBJECT_LIVENESS);

        status.currentInstrType = type;
        Classes.setVMObjectAllocEnabled(isMemoryProfiling);
    }

    public static int getCurrentInstrType() {
        return status.currentInstrType;
    }

    public static ThreadLivenessStatusResponse getCurrentThreadLivenessStatus() {
        ThreadLivenessStatusResponse resp = new ThreadLivenessStatusResponse(ThreadInfo.getCurrentLivenessStatus());

        return resp;
    }

    public static void setInstrumentReflection(boolean v) {
        if (status.targetAppRunning) {
            ProfilerRuntimeCPU.setJavaLangReflectMethodInvokeInterceptEnabled(v);
        } else {
            instrumentReflection = v;
        }
    }

    public static MethodNamesResponse getMethodNamesForJMethodIds(int[] methodIds) {
        final int PACKEDARR_ITEMS = 4; // must match PACKEDARR_ITEMS in Stacks.c
        int nMethods = methodIds.length;
        int len = nMethods * PACKEDARR_ITEMS;
        packedArrayOffsets = new int[len];

        byte[] packedData = Stacks.getMethodNamesForJMethodIds(nMethods, methodIds, packedArrayOffsets);
        MethodNamesResponse resp = new MethodNamesResponse(packedData, packedArrayOffsets);

        return resp;
    }

    public static int getNPrerecordedSystemThreads() {
        return nSystemThreads;
    }

    public static ObjectAllocationResultsResponse getObjectAllocationResults() {
        status.beginTrans(false);

        try {
            ObjectAllocationResultsResponse resp = new ObjectAllocationResultsResponse(status.getAllocatedInstancesCount(),
                                                                                       status.getNInstrClasses());

            return resp;
        } finally {
            status.endTrans();
        }
    }

    public static void setProfilerServer(ProfilerServer server) {
        profilerServer = server;
    }

    /**
     * This method cleans up the data structures managed by this class, but not by various ProfilerRuntimeXXX classes.
     * The latter are cleaned up separately, by the following deactivateInjectedCode() method.
     */
    public static void clearProfilerDataStructures() {
        //loadedClassesArray = null;
        //loadedClassesCPLengths = null;
        //loadedClassesLoaders = null;
        //ClassLoaderManager.reset();
        //packedArrayOffsets = null;
        reflectMethods = null;

        //evBufManager.freeBufferFile();
    }

    public static boolean cpuResultsExist() {
        return ProfilerRuntime.profiledTargetAppThreadsExist();
    }

    static void disableProfiling() {
        int instrType = getCurrentInstrType();

        switch (instrType) {
            case INSTR_NONE:
            case INSTR_NONE_MEMORY_SAMPLING:
                ProfilerRuntime.clearDataStructures();
                break;
                
            case INSTR_NONE_SAMPLING:
                ProfilerRuntimeSampler.shutdown();
                break;
                
            case INSTR_CODE_REGION:
                ProfilerRuntimeCPUCodeRegion.enableProfiling(false);

                if (rootClassNames != null) {
                    rootClassNames = null;
                }

                break;
            case INSTR_RECURSIVE_FULL:
                ProfilerRuntimeCPUFullInstr.enableProfiling(false);
                ProfilerRuntimeCPU.setTimerTypes(false, false); // Mainly to clean up microstate accounting on Solaris

                break;
            case INSTR_RECURSIVE_SAMPLED:
                ProfilerRuntimeCPUSampledInstr.enableProfiling(false);
                ProfilerRuntimeCPU.setTimerTypes(false, false);

                break;
            case INSTR_OBJECT_ALLOCATIONS:
                ProfilerRuntimeObjAlloc.enableProfiling(false);

                break;
            case INSTR_OBJECT_LIVENESS:
                ProfilerRuntimeObjLiveness.enableProfiling(false);

                break;
        }        
    }

    /**
     * Deactivate the injected code for the current instrumentation type, and clean up all the supporting data structures
     * maintained by the corresponding ProfilerRuntimeXXX class.
     */
    public static void deactivateInjectedCode() {
        disableProfilerHooks();
        disableProfiling();
        status.resetInstrClassAndMethodInfo();
        setCurrentInstrType(INSTR_NONE);
    }

    public static void disableProfilerHooks() {
        Classes.setWaitTrackingEnabled(false);
        Classes.setParkTrackingEnabled(false);
        Classes.setSleepTrackingEnabled(false);
        Classes.setVMObjectAllocEnabled(false);
        Classes.disableClassLoadHook();
        ProfilerRuntimeCPU.setJavaLangReflectMethodInvokeInterceptEnabled(false);
        ClassLoaderManager.setNotifyToolAboutUnloadedClasses(false);
    }

    public static void dumpExistingResults(boolean live) {
        if (!live && (getCurrentInstrType() == INSTR_OBJECT_LIVENESS) && ProfilerRuntimeObjLiveness.getRunGCOnGetResults()) {
            GC.runGC();

            try {
                // Give WeakReference collector thread a chance to register some (hopefully most of) object GCs
                Thread.sleep(500);

            } catch (Exception ex) {
            }
        }

        ProfilerRuntime.dumpEventBuffer();
    }

    /**
     * This method initializes the internal data structures, and also records the profiler's own thread(s), so that
     * they are not affected by our suspend/resume operations. If we run in the normal mode, i.e. the target JVM was
     * started by the client, specialThread is the current thread, which will then become the target app main thread.
     * It should be excluded from the list of the profiler's own threads. If we run in the attached mode, specialThread
     * is the only thread that we can reliably characterize as the profiler's own.
     */
    public static void initProfilerInterface(ProfilingSessionStatus status, Thread specialThread) {
        boolean jdk15 = Platform.getJDKVersionNumber() == Platform.JDK_15;
        Timers.initialize();
        Classes.initialize();
        GC.initialize();
        Stacks.initialize();
        Threads.initialize();
        HeapDump.initialize(jdk15);
        ThreadDump.initialize(jdk15);
        ClassLoaderManager.initialize(profilerServer);
        ClassLoaderManager.addSystemClassLoader();
        reflectMethods = new WeakHashMap();

        evBufManager = new EventBufferManager(profilerServer);
        heapHistogramManager = new HeapHistogramManager();
        ProfilerInterface.status = status;
        detachStarted = false;

        // Check that all profiler's own threads are running, and then record them internally, so that target app threads
        // are accounted for properly.
        while (!Monitors.monitorThreadsStarted()) {
            try {
                Thread.sleep(50);
            } catch (Exception ex) {
            }
        }

        if (status.runningInAttachedMode) {
            Threads.recordProfilerOwnThreads(false, specialThread);
            nSystemThreads = -1; // Indicates that we really don't know how many of these threads
                                 // are VM-own, or system, threads
        } else {
            nSystemThreads = Threads.recordProfilerOwnThreads(true, specialThread);
        }

        ProfilerRuntime.init(new ProfilerRuntime.ExternalActionsHandler() {
                public void handleFirstTimeMethodInvoke(char methodId) {
                    firstTimeMethodInvokeHook(methodId);
                }

                public void handleReflectiveInvoke(Method method) {
                    reflectiveMethodInvokeHook(method);
                }

                public int handleFirstTimeVMObjectAlloc(String className, int definingClassLoaderId) {
                    return firstTimeVMObjectAlloc(className, definingClassLoaderId);
                }

                public void handleEventBufferDump(byte[] eventBuffer, int startPos, int curPtrPos) {
                    serialClientOperationsLock.beginTrans(true);

                    try { // So that this event does not interfere with class
                          // loads / method invocations
                        clientDataProcStartTime = Timers.getCurrentTimeInCounts();
                        evBufManager.eventBufferDumpHook(eventBuffer, startPos, curPtrPos);
                        clientDataProcTime += (Timers.getCurrentTimeInCounts() - clientDataProcStartTime);
                    } finally {
                        serialClientOperationsLock.endTrans();
                    }
                }
            });
    }

    public static void initiateProfiling(final InitiateProfilingCommand cmd, final boolean targetAppRunning)
                                        throws Exception {
        int instrType = cmd.getInstrType();
        String instrClassName = cmd.getRootClassName();

        if (instrClassName.equals("*FAKE_CLASS_FOR_INTERNAL_TEST*")) { // NOI18N
            handleFakeInitRecursiveInstrumentationCommand(); // To initialize certain internal classes, see comments
                                                             // in handleFake... method

            return;
        }

        switch (instrType) {
            case INSTR_NONE:
            case INSTR_NONE_MEMORY_SAMPLING:
                createEventBuffer();
                
                break;
            case INSTR_RECURSIVE_FULL:
            case INSTR_RECURSIVE_SAMPLED:
            case INSTR_OBJECT_ALLOCATIONS:
            case INSTR_OBJECT_LIVENESS:
                createEventBuffer();
                status.resetInstrClassAndMethodInfo();

                if ((instrType == INSTR_OBJECT_ALLOCATIONS) || (instrType == INSTR_OBJECT_LIVENESS)) {
                    ClassLoaderManager.setNotifyToolAboutUnloadedClasses(true);
                } else {
                    ClassLoaderManager.setNotifyToolAboutUnloadedClasses(false);
                }

                break;
            case INSTR_CODE_REGION:
                ProfilerRuntimeCPUCodeRegion.resetProfilerCollectors();

                break;
            case INSTR_NONE_SAMPLING:
                createEventBuffer();
                break;
            default:
                throw new IllegalArgumentException("Instr. type: "+instrType);
        }

        // We have to perform the following operations in a separate thread, since they may involve further dialog with
        // the tool (client), whereas this thread has to return quickly to send the "OK" response to the tool.
        new InitiateProfilingThread(cmd, targetAppRunning).start();
    }

    public static void instrumentMethods(InstrumentMethodGroupCommand cmd)
                                  throws Exception {
        if (!cmd.isEmpty()) {
            try {
                instrumentMethodGroupNow(cmd.getBase());
            } catch (Exception ex) {
                deactivateInjectedCode();
                setCurrentInstrType(INSTR_NONE);
                throw ex;
            }
        }

        setCurrentInstrType(cmd.getInstrType());
    }

    public static void resetProfilerCollectors() {
        ProfilerRuntime.resetProfilerCollectors(getCurrentInstrType());
        reflectMethods = new WeakHashMap(); // So that methods that are possibly holding unreachable classes are
                                            // removed and classes allowed to be GCed
    }

    public static void resumeTargetApp() {
        if (getCurrentInstrType() == INSTR_RECURSIVE_FULL) {
            ProfilerRuntimeCPUFullInstr.resumeActiveTimers();
        }

        Threads.resumeTargetAppThreads(null);
        targetAppSuspended = false;
    }

    public static void suspendTargetApp() {
        Threads.suspendTargetAppThreads(null);

        if (getCurrentInstrType() == INSTR_RECURSIVE_FULL) {
            ProfilerRuntimeCPUFullInstr.suspendActiveTimers();
        }

        targetAppSuspended = true;
    }

    static void createEventBuffer() throws IOException {
        evBufManager.openBufferFile(EVENT_BUFFER_SIZE_IN_BYTES);
        ProfilerRuntime.createEventBuffer(EVENT_BUFFER_SIZE_IN_BYTES);
    }

    static String getBufferFileName() {
        if (evBufManager != null) {
            return evBufManager.getBufferFileName();
        }
        return "";
    }

    static void setDetachStarted(boolean detachStarted) {
        ProfilerInterface.detachStarted = detachStarted;
    }

    static boolean isDetachStarted() {
        return detachStarted;
    }
        
    static Response computeHistogram() {
        Response resp = null;
        
        if (Histogram.isAvailable()) {
            resp = heapHistogramManager.computeHistogram(Histogram.getRawHistogram());
        }
        if (resp == null) {
            resp = new Response(HISTOGRAM_NOT_AVAILABLE_MSG);
            
        }
        return resp;
    }

    static byte[][] getClassFileBytes(String[] classNames, int[] classLoaderIds) {
        int MAX_CLASSES = 1000;
        Class[] nonSystemClasses = new Class[MAX_CLASSES+1]; // classes loaded by classloaders other that bootstrap and system
        int nonSystemIndex = 0;
        byte[][] bytes = new byte[classNames.length][];
        Class[] classes = new Class[classNames.length];

        for (int i = 0; i < loadedClassesArray.length; i++) {
            Class loadedClass = getOrdinaryClass(loadedClassesArray[i]);
            if (loadedClass == null) {
                // class was unloaded or has special name
                continue;
            }
            int classLoaderId = loadedClassesLoaders[i];
            String name = loadedClass.getName();
            
            // At the client side we treat classes loaded by the bootstrap and by the system classloaders in the same way
            if (classLoaderId == -1) classLoaderId = 0;
            for (int j = 0; j < classNames.length; j++) {
                if (classLoaderIds[j] == classLoaderId && classNames[j].equals(name)) {
                    classes[j] = loadedClass;
                    nonSystemClasses[nonSystemIndex++] = loadedClass;
                    break;
                }
            }
            if (nonSystemIndex == MAX_CLASSES) {
                cacheLoadedClasses(nonSystemClasses,nonSystemIndex);
                nonSystemIndex = 0;
                profilerServer.sendSimpleCmdToClient(Command.STILL_ALIVE);
            }
        }
        if (nonSystemIndex > 0) {
            cacheLoadedClasses(nonSystemClasses,nonSystemIndex);
        }
        for (int i = 0; i < classes.length; i++) {
            if (classes[i] != null) {
                bytes[i] = getClassFileBytes(classes[i], classLoaderIds[i]);
            }
        }
        return bytes;
    }
    
    private static boolean getAndInstrumentClasses(boolean rootClassInstrumentation) {
        Response r = profilerServer.getLastResponse();

        if (!(r instanceof InstrumentMethodGroupResponse)) { // This is an internal error which, hopefully, has been fixed.

            String msg = MessageFormat.format(INTERNAL_ERROR_MSG, new Object[] { r.getClass(), r });
            deactivateInjectedCode();
            profilerServer.sendComplexCmdToClient(new AsyncMessageCommand(false, msg));

            return false;
        }

        InstrumentMethodGroupResponse imgr = (InstrumentMethodGroupResponse) r;
        clientInstrTime += (Timers.getCurrentTimeInCounts() - clientInstrStartTime);

        if (!imgr.isOK()) {
            return false;
        }

        if (imgr.isEmpty()) {
            nEmptyInstrMethodGroupResponses++;

            // Don't return immediately, because may have rootClassInstrumentation == true (see above)
        } else {
            // Do the following update before instrumentation, since if we do this after it, chances are some instrumented
            // method in another thread enters e.g. methodEntry() and hits the not-yet-updated invocation array before
            // updating has been completed.
            updateInstrClassAndMethodNames(imgr.getBase(), true);

            if (rootClassInstrumentation && (getCurrentInstrType() == INSTR_OBJECT_LIVENESS)) {
                // Create a ThreadInfo for the current thread immediately to avoid recursion with trace object allocation calls
                ThreadInfo.getThreadInfo();
            }

            try {
                instrumentMethodGroupNow(imgr.getBase());
            } catch (Exception ex) {
                //deactivateInjectedCode();   // It looks like it often makes more sense to proceed and get at least some info
                profilerServer.sendComplexCmdToClient(new AsyncMessageCommand(false, ex.getMessage()));

                return true; // Used to be "return false" (but see comment above).
            }
        }

        if (rootClassInstrumentation) {
            switch (getCurrentInstrType()) {
                case INSTR_RECURSIVE_FULL:

                    if (instrumentReflection) {
                        ProfilerRuntimeCPU.setJavaLangReflectMethodInvokeInterceptEnabled(true);
                    }

                    ProfilerRuntimeCPUFullInstr.enableProfiling(true);

                    break;
                case INSTR_RECURSIVE_SAMPLED:

                    if (instrumentReflection) {
                        ProfilerRuntimeCPU.setJavaLangReflectMethodInvokeInterceptEnabled(true);
                    }

                    ProfilerRuntimeCPUSampledInstr.enableProfiling(true);

                    break;
                case INSTR_CODE_REGION:
                    ProfilerRuntimeCPUCodeRegion.enableProfiling(true);

                    break;
                case INSTR_OBJECT_ALLOCATIONS:
                    ProfilerRuntimeObjAlloc.enableProfiling(true);

                    break;
                case INSTR_OBJECT_LIVENESS:
                    ProfilerRuntimeObjLiveness.enableProfiling(true);

                    break;
            }
        }

        return true;
    }

    private static boolean isCoreClassName(String name) {
        name = name.replace('.', '/'); // NOI18N

        return (name.startsWith("java/") || name.startsWith("sun/") || name.startsWith("javax/")); // NOI18N
    }

    private static void getLoadedClasses() {
        if (loadedClassesArray == null) {
            int nonSystemIndex = 0;
            int MAX_CLASSES = 1000;
            Class[] nonSystemClasses = new Class[MAX_CLASSES+1]; // classes loaded by classloaders other that bootstrap and system
            boolean dynamic = profilerServer.isDynamic();
            Class[] allClasses = Classes.getAllLoadedClasses();
            loadedClassesArray = new WeakReference[allClasses.length];
            loadedClassesLoaders = new int[allClasses.length];
            
            Monitors.DeterminateProgress progress = Monitors.enterServerState(CommonConstants.SERVER_PREPARING, loadedClassesArray.length);
            try {
                for (int i = 0; i < loadedClassesArray.length; i++) {
                    progress.next();
                    if(detachStarted) {
                        return;
                    }
                    Class clazz = allClasses[i];
                    loadedClassesArray[i] = new WeakReference(clazz);
                    loadedClassesLoaders[i] = ClassLoaderManager.registerLoader(clazz);
                }
            } finally {
                Monitors.exitServerState();
            }
        }
    }

    private static boolean isRootClass(String className) {
        for (int i = 0; i < rootClassNames.length; i++) {
            String rootName = rootClassNames[i];

            if (rootClassNameWildcard[i]) {
                if (className.startsWith(rootName)) {
                    if (rootClassNamePackageWildcard[i]) {  // instrument also subpackages
                        return true;
                    }
                    if (className.indexOf('.', rootName.length()) == -1) { // not a subpackage
                        return true;
                    }
                } else if (rootClassNamePackageWildcard[i]){
                    if (className.equals(rootName.substring(0,rootName.length()-1)))
                    {
                        return true;
                    }
                }
            } else if (rootName.equals(className)) {
                return true;
            }
        }

        return false;
    }

    private static void appendTypeName(StringBuffer sb, Class type) {
        if (type.isArray()) {
            do {
                sb.append('['); // NOI18N
                type = type.getComponentType();
            } while (type.isArray());
        }

        if (type == Integer.TYPE) {
            sb.append(INT);
        } else if (type == Boolean.TYPE) {
            sb.append(BOOLEAN);
        } else if (type == Byte.TYPE) {
            sb.append(BYTE);
        } else if (type == Character.TYPE) {
            sb.append(CHAR);
        } else if (type == Long.TYPE) {
            sb.append(LONG);
        } else if (type == Float.TYPE) {
            sb.append(FLOAT);
        } else if (type == Double.TYPE) {
            sb.append(DOUBLE);
        } else if (type == Void.TYPE) {
            sb.append(VOID);
        } else {
            sb.append(REFERENCE);
            sb.append(type.getName().replace('.', '/')); // NOI18N
            sb.append(';'); // NOI18N
        }
    }

    private static boolean checkForLoadedRootClasses() {
        for (int i = 0; i < loadedClassesArray.length; i++) {
            Class loadedClass = (Class) loadedClassesArray[i].get();
            if (loadedClass != null && isRootClass(loadedClass.getName())) {
                return true;
            }
        }

        return false;
    }

    /** Called on CLASS_PREPARE JVMTI event */
    private static void classLoadHook(Class clazz) {
        ThreadInfo threadInfo = ThreadInfo.getThreadInfo();

        threadInfo.inProfilingRuntimeMethod++;

        try {
            String className = clazz.getName();

            if (instrumentMethodGroupCallThread == Thread.currentThread() || internalClassName(className)) { // See comment at inInstrumentMethodGroupCall
                ClassLoaderManager.registerLoader(clazz); // Still register the loader, for reasons related with
                                                          // management of jmethodIds

                return;
            }

            Thread currentThread = Thread.currentThread();

            if (PROFILER_SERVER_THREAD_NAME.equals(currentThread.getName())) {
                System.err.println(ENGINE_WARNING + "class " + className + " loaded by " + PROFILER_SERVER_THREAD_NAME); // NOI18N

                return;
            }

            if ((initInstrumentationThread != null) && (currentThread == initInstrumentationThread)) {
                // Looks like on rare occasions we can get this problem - class load hook called when it shouldn't.
                // If we are already here, we can't (easily at least) fix this problem, but at least we can warn the user.
                System.err.println(ENGINE_WARNING + "class load hook invoked at inappropriate time for " // NOI18N
                                   + className + ", loader = " + clazz.getClassLoader()); // NOI18N
                System.err.println("*** This class will not be instrumented unless you re-run the instrumentation command"); // NOI18N
                System.err.println(PLEASE_REPORT_PROBLEM);
                System.err.println("=============================== Stack trace ====================="); // NOI18N
                Thread.dumpStack();
                System.err.println("=============================== End stack trace ================="); // NOI18N

                return;
            }

            //System.out.println("+++ Class load hook invoked for " + className + ", loader = " + clazz.getClassLoader());
            int classLoaderId = ClassLoaderManager.registerLoader(clazz);
            boolean resumeTimer = false;

            if (DEBUG) {
                System.err.println("ProfilerInterface.classLoadHook.DEBUG: " + className + ", classLoaderId: " + classLoaderId); // NOI18N
            }

            serialClientOperationsLock.beginTrans(true);

            try {
                boolean rootInstrumented = false;
                String excMessage = null;
                int instrType = getCurrentInstrType();

                if (instrType == INSTR_NONE) {
                    return; // Instrumentation was turned off in the mean time
                }

                // bugfix for issue http://profiler.netbeans.org/issues/show_bug.cgi?id=65968
                boolean resumeProfiling = false;

                if (ThreadInfo.profilingSuspended()) {
                    ThreadInfo.suspendProfiling();
                    resumeProfiling = true;
                }

                try {
                    if (rootClassLoaded) { // if yes, it means instrumentation has been started
                                           // [ian] why the following if???

                        if ((instrType != INSTR_RECURSIVE_FULL) && (instrType != INSTR_RECURSIVE_SAMPLED)
                                && (instrType != INSTR_OBJECT_ALLOCATIONS) && (instrType != INSTR_OBJECT_LIVENESS)) {
                            if (!((instrType == INSTR_CODE_REGION)
                                    && className.equals(rootClassNames[ProfilingSessionStatus.CODE_REGION_CLASS_IDX]))) {
                                return; // Nothing to do
                            }
                        }

                        ThreadInfo ti = null;

                        if ((instrType == INSTR_RECURSIVE_FULL) || (instrType == INSTR_RECURSIVE_SAMPLED)) {
                            nClassLoads++;
                            ti = ProfilerRuntimeCPU.suspendCurrentThreadTimer(); // start blackout period
                            clientInstrStartTime = Timers.getCurrentTimeInCounts();
                            // We'll be unable to call resumeCurrentThreadTimer() right here, since here we are holding serialClientOperationsLock.
                            // The same lock is acquired when we dump the event buffer. So if here we call resumeTimer(), which calls writeEvent(),
                            // we can get into a deadlock if some other thread at this time is dumping the event buffer and tries to acquire that lock.
                            resumeTimer = true; // resume blackout period at the end
                        }

                        byte[] classFileBytes = getClassFileBytes(clazz, classLoaderId);
                        // send request to tool to instrument the bytecode
                        ClassLoadedCommand cmd = new ClassLoadedCommand(className,
                                                                        ClassLoaderManager.getThisAndParentLoaderData(classLoaderId),
                                                                        classFileBytes, (ti != null) ? ti.isInCallGraph() : false);
                        profilerServer.sendComplexCmdToClient(cmd);

                        // read response from tool that should contain the instrumented bytecode, and redefine the methods/classes
                        if (!getAndInstrumentClasses(false)) {
                            disableProfilerHooks();

                            return;
                        }
                    } else {
                        // in total inst scheme for CPU profiling we instrument everything
                        boolean rootWasLoaded = ((instrType == INSTR_RECURSIVE_FULL) || (instrType == INSTR_RECURSIVE_SAMPLED))
                                                && (status.instrScheme == INSTRSCHEME_TOTAL);

                        // No root classes have been loaded - check if it's one of them
                        if (!rootWasLoaded && !isRootClass(className)) {
                            return;
                        }

                        // This is a root class - proceed with requesting client for instrumented code.
                        nClassLoads++;
                        clientInstrStartTime = Timers.getCurrentTimeInCounts();
                        sendRootClassLoadedCommand(true);

                        if (!getAndInstrumentClasses(true)) {
                            disableProfilerHooks();

                            return;
                        }

                        // Note: it is important to have 'rootClassLoaded = true' here, i.e. *after* (not before) the call to getAndInstrumentClasses().
                        // It looks like some classes returned by getAllLoadedClasses() may be not completely initialized, and thus when we finally
                        // load them properly in instrumentMethodGroup() before instrumenting, they get initialized and classLoadHook is called for each
                        // of them. If rootClassLoaded is true, then for each such class a request is sent to the client, which wonders why it got a
                        // second class load event for the same class. Having rootClassLoaded not set until all such classes are loaded eliminates this
                        // issue. WARNING: may it happen that some really new class is loaded as a side effect of initializing of the classes described
                        // above? If so, it will be effectively lost. Need to try to come up with a test to confirm or prove this worry wrong.
                        rootInstrumented = true;
                        rootClassLoaded = true;

                        // This is done to avoid counting the time spent in instrumentation etc. upon root class load, but before our app (or actually
                        // data recording) started. That's because we use this internal statistics to calculate/verify the gross run time of the app.
                        ProfilerCalibrator.resetInternalStatsCollectors();
                    }

                    if (rootInstrumented || (excMessage != null)) {
                        AsyncMessageCommand cmd = null;

                        if (excMessage == null) {
                            cmd = new AsyncMessageCommand(true, INSTRUMENTATION_SUCCESSFUL_MSG);
                        } else {
                            cmd = new AsyncMessageCommand(false, excMessage);
                        }

                        profilerServer.sendComplexCmdToClient(cmd);
                    }
                } finally {
                    if (resumeProfiling) {
                        ThreadInfo.resumeProfiling();
                    }
                }
            } finally { // end of synchronized(serialClientOperationsLock)
                serialClientOperationsLock.endTrans();
            }

            if (resumeTimer) {
                int instrType = getCurrentInstrType();

                if ((instrType == INSTR_RECURSIVE_FULL) || (instrType == INSTR_RECURSIVE_SAMPLED)) {
                    ProfilerRuntimeCPU.resumeCurrentThreadTimer();
                }
            }
        } finally {
            threadInfo.inProfilingRuntimeMethod--;
        }
    }

    private static void firstTimeMethodInvokeHook(char methodId) {
        serialClientOperationsLock.beginTrans(true);

        try {
            int instrType = getCurrentInstrType();

            if ((instrType != INSTR_RECURSIVE_FULL) && (instrType != INSTR_RECURSIVE_SAMPLED)) {
                return; // Chances are that instrumentation is already stopped
            }

            clientInstrStartTime = Timers.getCurrentTimeInCounts();

            MethodInvokedFirstTimeCommand cmd = new MethodInvokedFirstTimeCommand(methodId);
            profilerServer.sendComplexCmdToClient(cmd);

            if (!getAndInstrumentClasses(false)) {
                disableProfilerHooks();

                return;
            }

            // The following reset is done to avoid counting the time spent in instrumentation before data recording started.
            if (nFirstMethodInvocations == 0) {
                ProfilerCalibrator.resetInternalStatsCollectors();
            }

            nFirstMethodInvocations++;
        } finally {
            serialClientOperationsLock.endTrans();
        }
    }

    private static int firstTimeVMObjectAlloc(String className, int definingClassLoaderId) {
        GetClassIdResponse resp;

        if (internalClassName(className)) {
            return -1;
        }
        
        serialClientOperationsLock.beginTrans(true);

        try {
            GetClassIdCommand cmd = new GetClassIdCommand(className, definingClassLoaderId);
            profilerServer.sendComplexCmdToClient(cmd);
            resp = (GetClassIdResponse) profilerServer.getLastResponse();
        }  finally {
            serialClientOperationsLock.endTrans();
        }
        if (resp.isOK()) {
            int classId = resp.getClassId();
            int instrClasses = classId+1;
            if (instrClasses > status.getNInstrClasses()) {
                status.updateAllocatedInstancesCountInfoInServer(instrClasses);
                ProfilerRuntimeMemory.setAllocatedInstancesCountArray(status.getAllocatedInstancesCount());
            }
            return classId;
        }
        return -1;
    }

    private static void handleFakeInitRecursiveInstrumentationCommand() {
        // Send a fake RootClassLoadedCommand to the client and get a reply from it. This is done to force initialization
        // of all classes related to this operation. If this happens later, it can cause deadlock due to classLoadHook called upon
        // loading of some of these classes, when classLoadHook is already locked to "serialize" class load events.
        new HFIRIThread().start();
    }

    /**
     *  support for multiple roots needed by EJB work
     *  will check each class to see if it is a candidate to be a core class
     */
    private static boolean hasAnyCoreClassNames(String[] classes) {
        if (!(classes.length > 0)) {
            return false;
        }

        for (int i = 0; i < classes.length; i++) {
            if (isCoreClassName(classes[i])) {
                return true;
            }
        }

        return false; //none found...
    }

    private static void instrumentMethodGroupNow(InstrumentMethodGroupData imgb)
                                          throws Exception {
        //divided into 2 parts: cass loading and class instrumenting
        Monitors.DeterminateProgress wholeProgress = Monitors.enterServerState(CommonConstants.SERVER_INITIALIZING, 2);
        try {
            instrumentMethodGroupCallThread = Thread.currentThread();

            int res = 0;
            long time = Timers.getCurrentTimeInCounts();
            nNonEmptyInstrMethodGroupResponses++;

            int nClasses = imgb.getNClasses();
            String[] instrClassNames = imgb.getMethodClasses();
            int[] instrClassLoaders = imgb.getClassLoaderIds();
            int nMethods = imgb.getNMethods();

            Class[] clazzes = new Class[nClasses];
            byte[][] b = imgb.getReplacementClassFileBytes();
            int k = 0;

            Monitors.DeterminateProgress progress = Monitors.enterServerState(CommonConstants.SERVER_PREPARING, nClasses);
            try {
                for (int i = 0; i < nClasses; i++) {
                    progress.next();
                    clazzes[k] = ClassLoaderManager.getLoadedClass(instrClassNames[i], instrClassLoaders[i]);

                    if (clazzes[k] != null) {
                        if (b[k] == null) {
                            // An optimization to avoid overhead of creating and sending original class file bytes from client
                            // to server
                            if (instrClassLoaders[i] == 0) {
                                b[k] = ClassBytesLoader.getClassFileBytes(instrClassNames[i]);
                            }
                            if (b[k] == null) {
                                b[k] = getCachedClassFileBytes(clazzes[k]);
                            }
                        }

                        k++;
                    } else {
                        reportUnloadedClass(instrClassNames[i]);

                        int classesToMove = nClasses - k - 1;
                        System.arraycopy(clazzes, k + 1, clazzes, k, classesToMove);
                        System.arraycopy(b, k + 1, b, k, classesToMove);
                    }
                }

                if (k < nClasses) {
                    Class[] oldClazzes = clazzes;
                    clazzes = new Class[k];
                    System.arraycopy(oldClazzes, 0, clazzes, 0, k);
                }
            } finally {
                progress = null;
                Monitors.exitServerState();                
            }
            
            wholeProgress.next();

            redefineClasses(clazzes, imgb.getReplacementClassFileBytes());

            time = Timers.getCurrentTimeInCounts() - time;
            totalHotswappingTime += time;

            if (time < minHotswappingTime) {
                minHotswappingTime = time;
            } else if (time > maxHotswappingTime) {
                maxHotswappingTime = time;
            }

            instrumentMethodGroupCallThread = null;
        } catch (Throwable t) {
            if (t instanceof Classes.RedefineException) {
                int nClasses = imgb.getNClasses();
                String[] instrClassNames = imgb.getMethodClasses();
                System.err.println("Profiler Agent Error: Redefinition failed for classes:"); // NOI18N

                for (int i = 0; i < nClasses; i++) {
                    System.err.println(instrClassNames[i]);
                }

                System.err.println("Profiler Agent Error: with message: " + ((Classes.RedefineException) t).getMessage()); // NOI18N

                byte[][] newBytes = imgb.getReplacementClassFileBytes();

                for (int i = 0; i < nClasses; i++) {
                    String name = instrClassNames[i];
                    File outFile = new File(name + ".class"); // NOI18N
                    System.err.println("Debug: writing class file: " + name + ", into file: " + outFile.getPath()); // NOI18N

                    try {
                        FileOutputStream fos = new FileOutputStream(outFile);
                        fos.write(newBytes[i]);
                        fos.close();
                    } catch (IOException exc) {
                        System.err.println("error: " + exc + " writing class file: " + outFile.getPath()); // NOI18N
                    }
                }

                throw ((Classes.RedefineException) t);
            } else {
                java.io.StringWriter sw = new java.io.StringWriter();
                java.io.PrintWriter pw = new java.io.PrintWriter(sw);
                t.printStackTrace(pw);
                throw new Exception(MessageFormat.format(UNEXPECTED_EXCEPTION_MSG, new Object[] { t, sw.toString() }));
            }
        } finally {
            instrumentMethodGroupCallThread = null;            
            Monitors.exitServerState();
        }
    }

    private static void redefineClasses(Class[] classes, byte[][] bytecode) throws Classes.RedefineException {
        Monitors.DeterminateProgress progress = Monitors.enterServerState(CommonConstants.SERVER_INSTRUMENTING, 
                                                        (classes.length+REDEFINE_CHUNK_SIZE-1)/REDEFINE_CHUNK_SIZE);
        try {
            int index = 0;
            Class[] chunkClasses = null;
            byte[][] chunkBytecode = null;
            while(index < classes.length) {
                int size = Math.min(classes.length - index, REDEFINE_CHUNK_SIZE);
                if(chunkClasses == null || chunkClasses.length != size ) {
                    chunkClasses = new Class[size];
                    chunkBytecode = new byte[size][];
                }
                System.arraycopy(classes, index, chunkClasses, 0, size);
                System.arraycopy(bytecode, index, chunkBytecode, 0, size);
                Classes.redefineClasses(chunkClasses, chunkBytecode);
                progress.next();
                index += size;
            }            
        } finally {
            Monitors.exitServerState();
        }
    }

    private static boolean internalClassName(String name) {
        return (serverInternalClassName(name)
               || // WARNING: sun.reflect.* are not really internal classes, but they may create too many problems by being loaded unexpectedly
        // by our internal code and causing classLoadHook to be invoked recursively. At least we need sun.reflect.Generated* to be
        // dismissed. Others could probably be less of a problem if ClassLoaderManager didn't use reflection.
        (name.startsWith("sun.reflect.") && !name.startsWith("sun.reflect.GeneratedSerializationConstructorAccessor")
                  && !name.startsWith("sun.reflect.GeneratedConstructorAccessor")) // NOI18N
               || name.startsWith("sun.instrument.") // NOI18N
                                                     // FIXME: below is a (hopefully temporary) fix to the strange problem showing up as ClassCircularityError when
                                                     // we try to profile PetStore with eager instrumentation scheme on Sun ONE AS 7. This makes the problem go away,
                                                     // but its root cause is still unclear to me.
               || name.equals("com.sun.enterprise.J2EESecurityManager") // NOI18N
               // do not instrument classes created by Unsafe.defineAnonymousClass() 
               || name.contains("/") // NOI18N   
        );
    }

    static boolean serverInternalClassName(String name) {
        if (INSTRUMENT_JFLUID_CLASSES) {
            return name.startsWith("org.graalvm.visualvm.lib.jfluid.server") || // NOI18N
                   name.startsWith("org.graalvm.visualvm.lib.jfluid.global") || // NOI18N
                   name.startsWith("org.graalvm.visualvm.lib.jfluid.wireprotocol"); // NOI18N
        } else {
            return name.startsWith(PROFILER_DOTTED_CLASS_PREFIX);
        }
    }

    private static void reflectiveMethodInvokeHook(Method method) {
        serialClientOperationsLock.beginTrans(true);

        try {
            if (reflectMethods.containsKey(method)) {
                return;
            }

            ProfilerRuntimeCPU.suspendCurrentThreadTimer();

            reflectMethods.put(method, null);

            Class clazz = method.getDeclaringClass();
            String className = clazz.getName();
            String methodName = method.getName();
            Class[] paramTypes = method.getParameterTypes();
            StringBuffer sb = new StringBuffer();
            sb.append('(');

            for (int i = 0; i < paramTypes.length; i++) {
                appendTypeName(sb, paramTypes[i]);
            }

            sb.append(')');
            appendTypeName(sb, method.getReturnType());

            String methodSignature = sb.toString();

            clientInstrStartTime = Timers.getCurrentTimeInCounts();

            MethodLoadedCommand cmd = new MethodLoadedCommand(className, ClassLoaderManager.registerLoader(clazz), methodName,
                                                              methodSignature);
            profilerServer.sendComplexCmdToClient(cmd);

            if (!getAndInstrumentClasses(false)) {
                disableProfilerHooks();

                return;
            }

            ProfilerRuntimeCPU.resumeCurrentThreadTimer();
        } finally {
            serialClientOperationsLock.endTrans();
        }
    }

    private static void reportUnloadedClass(String className) {
        System.err.println(ENGINE_WARNING + "target VM cannot load class to instrument " + className); // NOI18N
        System.err.println("*** probably it has been unloaded recently"); // NOI18N
    }

    private static void reportCacheMiss(final byte[] bytes, final Class clazz) {
        if (bytes == null) {
            System.err.println(ENGINE_WARNING + "Failed to lookup cached class " + clazz.getName()); // NOI18N
        }
    }
    
    private static byte[] getCachedClassFileBytes(Class clazz) {
        byte[] bytes = Classes.getCachedClassFileBytes(clazz);
        reportCacheMiss(bytes, clazz);
        return bytes;
    }

    private static void cacheLoadedClass(Class clazz) {
        Class[] classes = new Class[2];
        classes[0] = clazz;
        cacheLoadedClasses(classes,1);
    }
    
    private static void cacheLoadedClasses(Class[] nonSystemClasses, int nonSystemIndex) {
        if (DEBUG) System.out.println("Caching "+nonSystemIndex+" classes");
        nonSystemClasses[nonSystemIndex++] = ProfilerInterface.InitiateProfilingThread.class;
        Classes.cacheLoadedClasses(nonSystemClasses,nonSystemIndex);
    }

    /* Get class file bytes if they are available, i.e. if the class is loaded by a custom classloader
    * If remote profiling is used, get these class file bytes from system classpath
    * classLoaderId = 0 means that it is a system or bootstrap classloader
    */
    private static byte[] getClassFileBytes(Class clazz, int classLoaderId) {
        return getClassFileBytes(clazz, classLoaderId, true);
    }

    private static byte[] getClassFileBytes(Class clazz, int classLoaderId, boolean force) {
        byte[] classFileBytes = null;
        URL classURL = null;
        
        if (classLoaderId > 0 || (classURL = ClassBytesLoader.getClassFileURL(clazz.getName())) == null) {
            classFileBytes = Classes.getCachedClassFileBytes(clazz);
            if (classFileBytes == null) {
                if (DEBUG) {
                    System.err.println("Cannot get classbytes for "+clazz.getName()+" loader "+classLoaderId);
                }
                if (!force) return EMPTY;
                cacheLoadedClass(clazz);
                classFileBytes = getCachedClassFileBytes(clazz);
                if (classFileBytes == null) {
                    if (Platform.getJDKVersionNumber() != Platform.JDK_CVM) {
                        System.err.println("***Profiler agent warning: could not get .class file for a synthetic class " + clazz.getName()
                                           + " in ProfilerInterface.getClassFileBytes"); // NOI18N
                    }
                }
            }
        } else if (status.remoteProfiling || Platform.getJDKVersionNumber() >= Platform.JDK_19) {
            classFileBytes = ClassBytesLoader.getClassFileBytes(classURL);
        }
        return classFileBytes;
    }
    
    private static void sendRootClassLoadedCommand(boolean doGetLoadedClasses) {
        if (doGetLoadedClasses) {
            getLoadedClasses(); // Otherwise we know loadedClassesArray has already been initialized
        }

        int len = loadedClassesArray.length;
        String[] loadedClassNames = new String[len];
        int[] loaders = new int[len];
        byte[][] cachedClassFileBytes = new byte[len][];
        int[] loadedClassesSuper = new int[len];
        int[][] loadedClassesInterfaces = new int[len][];
        int instrType = getCurrentInstrType();
        boolean isLazyInstrType = profilerServer.isDynamic()
                && (instrType == INSTR_RECURSIVE_FULL || instrType == INSTR_RECURSIVE_SAMPLED)
                && status.instrScheme == INSTRSCHEME_LAZY 
                && !instrSpawnedThreads;
        boolean isMemoryProfiling = profilerServer.isDynamic()
                && (instrType == INSTR_OBJECT_ALLOCATIONS || instrType == INSTR_OBJECT_LIVENESS);
        Map classIndex = new HashMap(loadedClassesArray.length*4/3);
        Class[] classesArray = new Class[len];

        for (int i = 0; i < len; i++) {
            Class clazz = getOrdinaryClass(loadedClassesArray[i]);
            if (clazz == null) {
                // class was unloaded or has special name
                continue;
            }
            int index = classIndex.size();
            classesArray[index] = clazz;
            loaders[index] = loadedClassesLoaders[i];
            classIndex.put(clazz, new Integer(index));
        }
        for (int i = 0; i < classIndex.size(); i++) {
            Class clazz = classesArray[i];
            String name = clazz.getName();
            boolean forceClassFile = (!isLazyInstrType && !isMemoryProfiling) || isRootClass(name);
            loadedClassNames[i] = name;
            cachedClassFileBytes[i] = getClassFileBytes(clazz, loaders[i], forceClassFile);

            if (!forceClassFile) {
                Class superClass = clazz.getSuperclass();
                Class[] interfaces = clazz.getInterfaces();
                
                if (superClass != null) {
                    Integer clsId = (Integer) classIndex.get(superClass);
                    if (clsId != null) {
                        loadedClassesSuper[i] = clsId.intValue();
                    } else {
                        //System.out.println("Super class of class "+name+" not found: "+superClass.getName());
                        loadedClassesSuper[i] = -1;
                    }
                } else {
                    loadedClassesSuper[i] = -1;
                }
                loadedClassesInterfaces[i] = new int[interfaces.length];
                for (int j = 0; j < interfaces.length; j++) {
                    Integer clsId = (Integer)classIndex.get(interfaces[j]);
                    if (clsId != null) {
                        loadedClassesInterfaces[i][j] = clsId.intValue();
                    } else {
                        //System.out.println("Interface of class "+name+" not found: "+interfaces[j].getName());
                        loadedClassesSuper[i] = -1;
                    }
                }
            }
        }
        RootClassLoadedCommand cmd = new RootClassLoadedCommand(loadedClassNames, loaders, cachedClassFileBytes, 
                                                loadedClassesSuper, loadedClassesInterfaces, classIndex.size(),
                                                ClassLoaderManager.getParentLoaderIdTable());
        profilerServer.sendComplexCmdToClient(cmd);
    }

    private static Class getOrdinaryClass(WeakReference classRef) {
        Class clazz = (Class) classRef.get();
        if (clazz != null) {
            String name = clazz.getName();

            if (!name.startsWith("[") && !internalClassName(name)) {
                return clazz;
            }             
        }
        return null;
    }
    
    private static void updateInstrClassAndMethodNames(InstrumentMethodGroupData imgb, boolean firstTime) {
        status.beginTrans(false);

        try {
            switch (getCurrentInstrType()) {
                case INSTR_RECURSIVE_FULL:
                case INSTR_RECURSIVE_SAMPLED:
                    status.updateInstrMethodsInfo(imgb.getNClasses(), imgb.getNMethods(), null, null, null, null, null,
                                                  imgb.getInstrMethodLeaf());
                    ProfilerRuntimeCPU.setInstrMethodsInvoked(status.getInstrMethodInvoked());

                    break;
                case INSTR_OBJECT_ALLOCATIONS:
                case INSTR_OBJECT_LIVENESS:
                    status.updateAllocatedInstancesCountInfoInServer(imgb.getAddInfo());
                    ProfilerRuntimeMemory.setAllocatedInstancesCountArray(status.getAllocatedInstancesCount());

                    break;
            }
        } finally {
            status.endTrans();
        }
    }
}
