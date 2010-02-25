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

import org.netbeans.lib.profiler.global.CalibrationDataFileIO;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.global.Platform;
import org.netbeans.lib.profiler.global.ProfilingSessionStatus;
import org.netbeans.lib.profiler.server.system.Classes;
import org.netbeans.lib.profiler.server.system.GC;
import org.netbeans.lib.profiler.server.system.HeapDump;
import org.netbeans.lib.profiler.server.system.Threads;
import org.netbeans.lib.profiler.server.system.Timers;
import org.netbeans.lib.profiler.wireprotocol.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.*;
import java.text.MessageFormat;
import java.util.*;


/**
 * This class contains functionality for starting (attaching to) the Target Application (TA), and for
 * communication between the profiling back end and the tool (server and client).
 *
 * @author Tomas Hurka
 * @author Misha Dmitriev
 * @author Ian Formanek
 */
public class ProfilerServer extends Thread implements CommonConstants {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private static class AttachDynamicThread extends Thread {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private int activateCode;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        AttachDynamicThread(int activateCode) {
            this.setName(PROFILER_SPECIAL_EXEC_THREAD_NAME + " 5"); // NOI18N
            this.activateCode = activateCode;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void run() {
            try {
                doActivate(activateCode);
            } catch (Throwable ex) {
                System.err.println("Profiler dynamic attach initialization failed due to:"); //NOI18N
                ex.printStackTrace();
            }
        }
    }

    // Copied from org.openide.util.NbBundle
    // Does not support branding!
    private static class LocaleIterator extends Object implements Iterator {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        /**
         * current locale, and initial locale
         */
        private Locale initLocale;

        /**
         * current locale, and initial locale
         */
        private Locale locale;

        /**
         * the branding string in use
         */
        private String branding;

        /**
         * current sufix which will be returned in next calling nextElement
         */
        private String current;

        /**
         * this flag means, if default locale is in progress
         */
        private boolean defaultInProgress = false;

        /**
         * this flag means, if empty sufix was exported yet
         */
        private boolean empty = false;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        /**
         * Creates new LocaleIterator for given locale.
         *
         * @param locale given Locale
         */
        public LocaleIterator(Locale locale) {
            this.locale = this.initLocale = locale;

            if (locale.equals(Locale.getDefault())) {
                defaultInProgress = true;
            }

            current = '_' + locale.toString(); // NOI18N

            //            if (brandingToken == null) {
            branding = null;

            //            } else {
            //                branding = "_" + brandingToken; // NOI18N
            //            }

            //System.err.println("Constructed: " + this);
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        /**
         * Tests if there is any sufix.
         */
        public boolean hasNext() {
            return (current != null);
        }

        /**
         * @return next sufix.
         * @throws NoSuchElementException if there is no more locale sufix.
         */
        public Object next() throws NoSuchElementException {
            if (current == null) {
                throw new NoSuchElementException();
            }

            final String ret;

            if (branding == null) {
                ret = current;
            } else {
                ret = branding + current;
            }

            int lastUnderbar = current.lastIndexOf('_'); // NOI18N

            if (lastUnderbar == 0) {
                if (empty) {
                    reset();
                } else {
                    current = ""; // NOI18N
                    empty = true;
                }
            } else {
                if (lastUnderbar == -1) {
                    if (defaultInProgress) {
                        reset();
                    } else {
                        // [PENDING] stuff with trying the default locale
                        // after the real one does not actually seem to work...
                        locale = Locale.getDefault();
                        current = '_' + locale.toString(); // NOI18N
                        defaultInProgress = true;
                    }
                } else {
                    current = current.substring(0, lastUnderbar);
                }
            }

            //System.err.println("Returning: `" + ret + "' from: " + this);
            return ret;
        }

        public void remove() throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        /**
         * Finish a series.
         * If there was a branding prefix, restart without that prefix
         * (or with a shorter prefix); else finish.
         */
        private void reset() {
            if (branding != null) {
                current = '_' + initLocale.toString(); // NOI18N

                int idx = branding.lastIndexOf('_'); // NOI18N

                if (idx == 0) {
                    branding = null;
                } else {
                    branding = branding.substring(0, idx);
                }

                empty = false;
            } else {
                current = null;
            }
        }
    }

    /**
     * A shutdown wait thread
     */
    private static class ShutdownWaitThread extends Thread {
        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public ShutdownWaitThread() {
            setName(PROFILER_SPECIAL_EXEC_THREAD_NAME + " 7"); // NOI18N
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void run() {
            if (preemptExit && connectionOpen) {
                profilerServer.sendSimpleCmdToClient(Command.SHUTDOWN_INITIATED);
                waitForShutdownOK();
                cleanupOnShutdown();

                // ... and proceed with shutdown
            }
        }
    }

    /**
     * A thread to execute certain commands in (see comments to executeInSeparateThread above)
     */
    private class SeparateCmdExecutionThread extends Thread {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private volatile boolean stopped = false;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public SeparateCmdExecutionThread() {
            ThreadInfo.addProfilerServerThread(this);
            setName(PROFILER_SPECIAL_EXEC_THREAD_NAME + " 6"); // NOI18N
            setDaemon(true);
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void run() {
            synchronized (execInSeparateThreadLock) {
                while (true) {
                    try {
                        execInSeparateThreadLock.wait();
                    } catch (InterruptedException ex) {
                        System.err.println(THREAD_WAIT_EXCEPTION_MSG); // NOI18N
                    }

                    if (stopped) {
                        return;
                    }

                    int opCode = execInSeparateThreadOpCode;

                    switch (opCode) {
                        case Command.DUMP_EXISTING_RESULTS:
                        case Command.DUMP_EXISTING_RESULTS_LIVE:

                            long absTimeStamp = ProfilerRuntimeCPU.getAbsTimeStampInCollectedFormat();
                            boolean res = false;

                            if ((ProfilerRuntime.eventBuffer != null) && !ProfilerRuntime.sendingBuffer) {
                                synchronized (ProfilerRuntime.eventBuffer) {
                                    res = ProfilerInterface.serialClientOperationsLock.beginTrans(true, true);

                                    if (res) {
                                        try {
                                            ProfilerInterface.dumpExistingResults(opCode == Command.DUMP_EXISTING_RESULTS_LIVE);
                                        } finally {
                                            ProfilerInterface.serialClientOperationsLock.endTrans();
                                        }
                                    }
                                }
                            }

                            DumpResultsResponse resp = new DumpResultsResponse(res, absTimeStamp);
                            sendComplexResponseToClient(resp);

                            break;
                        case Command.RESET_PROFILER_COLLECTORS:
                            requestClientResetResults();
                            sendSimpleResponseToClient(true, null);

                            break;
                    }
                }
            }
        }

        public void terminate() {
            stopped = true;
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    // !!! Warning - do not use ResourceBundle.getBundle here, won't work in context of direct/dynamic attach !!!
    // Default EN messages initialized here, will be replaced by localized messages in initLocalizedResources()
    private static ResourceBundle messages;
    private static String ENTER_TO_SHUTDOWN_MSG = "Press ENTER to shut down the target JVM..."; // NOI18N
    private static String MAIN_CLASS_NOT_PUBLIC_MSG = "Main class {0} is not public.\nProfiler can not start it"; // NOI18N
    private static String INCORRECT_MAIN_MODIFIERS_MSG = "Method {0}.main(String args[]) has incorrect modifiers"; // NOI18N
    private static String UNEXPECTED_EXCEPTION_MSG = "Target application threw an unexpected exception: {0}"; // NOI18N
    private static String ELAPSED_TIME_MSG = "Main application thread elapsed time: {0} ms."; // NOI18N
    private static String REMOTE_CONNECTION_MSG = "Profiler Agent: Established remote connection with the tool"; // NOI18N
    private static String LOCAL_CONNECTION_MSG = "Profiler Agent: Established local connection with the tool"; // NOI18N
    private static String WAITING_ON_PORT_MSG = "Profiler Agent: Waiting for connection on port {0} (Protocol version: {1})"; // NOI18N
    private static String WAITING_ON_PORT_TIMEOUT_MSG = "Profiler Agent: Waiting for connection on port {0}, timeout {1} seconds (Protocol version: {2})"; // NOI18N
    private static String CONNECTION_EXCEPTION_MSG = "Profiler Agent Error: Exception when trying to establish connection with client:\n{0}"; // NOI18N
    private static String CONNECTION_TIMEOUT_MSG = "Profiler Agent Error: Timed out trying to establish connection with client"; // NOI18N
    private static String AGENT_ERROR_MSG = "Profiler Agent Error: {0}"; // NOI18N
    private static String CONNECTION_INTERRUPTED_MSG = "Profiler Agent Error: Connection with client interrupted"; // NOI18N
    private static String COMMAND_EXCEPTION_MSG = "Profiler Agent Error: Exception when handling command from client:\n{0}"; // NOI18N
    private static String RESPONSE_EXCEPTION_MSG = "Profiler Agent Error: Exception when trying to send response or command to client:\n{0}"; // NOI18N
    private static String CONNECTION_CLOSED_MSG = "Profiler Agent: Connection with agent closed"; // NOI18N
    private static String INCORRECT_AGENT_ID_MSG = "Profiler Agent Warning: Wrong agentId specified: {0}"; // NOI18N
    private static String THREAD_EXCEPTION_MSG = "Profiler Agent Error: Exception in executeInSeparateThread()"; // NOI18N
    private static String THREAD_WAIT_EXCEPTION_MSG = "Profiler Agent Error: Exception in wait in SeparateCmdExecutionThread"; // NOI18N
                                                                                                                               // -----
    public static final int ATTACH_DYNAMIC = 0;
    public static final int ATTACH_DIRECT = 1;
    private static volatile boolean profilerInterfaceInitialized;
    private static volatile boolean connectionOpen;
    private static volatile boolean connectionFailed;
    private static volatile boolean detachCommandReceived;
    private static ProfilerServer profilerServer;
    private static ProfilingSessionStatus status;
    private static volatile boolean startTargetApp;
    private static volatile boolean targetAppMainThreadComplete;
    private static volatile Exception startupException;
    private static Object targetAppRunningLock;
    private static Thread mainThread;

    // Management of execution of some commands in a separate thread
    private static SeparateCmdExecutionThread separateCmdExecutionThread;
    private static ShutdownWaitThread shutdownWaitThread;
    static Object execInSeparateThreadLock;
    static int execInSeparateThreadOpCode;
    private static volatile boolean preemptExit = true;
    private static boolean shutdownOK = false;
    private static final Object shutdownLock = new Object();
    private static final Object resultsNotifiedLock = new Object();

    // @GuardedBy resultsNotifiedLock
    private static boolean resultsNotified = false;
    private static boolean resourcesInitialized = false;

    // This data is needed to avoid passing parameters to doActivate() which may cause problems in attach by pid mode on Windows.
    private static String _fullJFluidPath;
    private static int _portNo;
    private static int _activateCode;
    private static int _timeOut = 0;
    private static Response lastResponse;
    private static Object responseLock = new Object();

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private ObjectInputStream socketIn;
    private ObjectOutputStream socketOut;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private WireIO wireIO;
    private boolean dynamic;
    private int agentId = -1;

    //---------------------------------------------------------------------------------------
    // Communication management
    //---------------------------------------------------------------------------------------
    private int serverPort;
    private int serverTimeout = 0; // no timeout by default

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    private ProfilerServer(int port, boolean dynamic, int timeout) {
        super(PROFILER_SERVER_THREAD_NAME);
        setPriority(Thread.MAX_PRIORITY);
        serverPort = port;
        ThreadInfo.addProfilerServerThread(this);
        this.dynamic = dynamic;

        if (!dynamic) {
            // for dynamic attach, the server should never timeout
            serverTimeout = timeout;
        }

        setDaemon(true);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static synchronized Response getLastResponse() {
        Response res;

        synchronized (responseLock) {
            if (lastResponse == null) {
                // I had to introduce the check below, since for some applications, seemingly the GUI ones that open a FileChooser dialog,
                // we can somehow get an InterruptedException below. This is likely a bug in JDK - maybe AWT just browses and calls
                // Thread.interrupt() that causes this exception, on waiting threads, and can mistake our thread for its own or something.
                boolean gotInterrupted = false;

                do {
                    try {
                        responseLock.wait();
                        gotInterrupted = false;
                    } catch (InterruptedException ex) {
                        //System.err.println("*** JFluid warning: InterruptedException in ProfilerServer.getLastResponse()");
                        gotInterrupted = true;
                    }
                } while (gotInterrupted);

                if (lastResponse == null) {
                    System.out.println("Profiler Agent Error: lastResponse == null - internal error?"); // NOI18N
                }
            }

            res = lastResponse;
            lastResponse = null;
        }

        return res;
    }

    public static Thread getMainThread() {
        return mainThread;
    }

    public static ProfilingSessionStatus getProfilingSessionStatus() {
        return status;
    }

    public static boolean isTargetAppMainThreadComplete() {
        return targetAppMainThreadComplete;
    }

    public static void activate(String fullJFluidPath, int portNo, final int activateCode) {
        activate(fullJFluidPath, portNo, activateCode, 0);
    }

    /**
     * Entrypoint in the usage scenario when the client attaches to the running target app using an OS signal,
     * or the "attach on startup" method. On JDK 1.5,
     * called from ProfilerActivate15.premain().
     * activateCode == 0 : "attach on the fly", activateCode == 1 : "attach on startup"
     *
     * @param fullJFluidPath Full path to the agent libs
     * @param portNo         Port number to use
     * @param activateCode   one of ATTACH_DIRECT or ATTACH_DYNAMIC, determines whether the server is started in dynamic
     *                       attach mode ( JDK 1.6) or Direct attach
     * @param timeOut        Time out in seconds for server socket, or 0 for no timeout
     * @see #ATTACH_DIRECT
     * @see #ATTACH_DYNAMIC
     */
    public static void activate(String fullJFluidPath, int portNo, final int activateCode, int timeOut) {
        try {
            _fullJFluidPath = fullJFluidPath;
            _portNo = portNo;
            _timeOut = timeOut;
            _activateCode = activateCode;

            initLocalizedResources();

            if (activateCode == ATTACH_DYNAMIC) {
                // Creation of the new thread is (hopefully) a temporary workaround to avoid the problem with stack
                // overflow or something else when we attach on Windows "by pid", i.e. using the CreateRemoteThread() call.
                new AttachDynamicThread(activateCode).start();
            } else {
                doActivate(activateCode);
            }
        } catch (Throwable ex) {
            System.err.println("Profiler initialization failed due to:"); //NOI18N
            ex.printStackTrace();
        }
    }

    /**
     * Entrypoint in the usage scenario where the client starts and stops the target application.
     * Start the communication thread and then the target application.
     * args[0] is the full path to the directory where JFluid native libraries are contained.
     * args[1] is the communication port number.
     * args[2] (optional) if it is a number, it is a timeout for the profiler server (in seconds) to wait until the
     * client connects
     * args[2 or 3] is the target app main class name; args[3 or 4..n] are its arguments.
     */
    public static void main(String[] args) {
        mainThread = Thread.currentThread();

        // Fix for Issue 69454 - cannot find path to Profiler libraries (http://www.netbeans.org/issues/show_bug.cgi?id=69454)
        // _fullJFluidPath is needed for lazy initializing localized messages, but it was originally set only by the activate() method
        // Now it has to be set also here for the I18N to work
        try {
            _fullJFluidPath = new File(args[0]).getParentFile().getParentFile().getParentFile().getAbsolutePath();
        } catch (Exception ex) {
            throw new RuntimeException("ProfilerServer: Unable to initialize ResourceBundle for ProfilerServer, cannot resolve library directory\n" // NOI18N
                                       + ex.getMessage());
        }

        initLocalizedResources();
        initInternals();

        // Get the port number
        int portNo = 0;

        try {
            portNo = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            internalError("illegal port number specified: " + args[1]); // NOI18N
        }

        int idx = 2;

        // Get the optional timeout number
        int timeout = 0;

        try {
            timeout = Integer.parseInt(args[2]);
            idx = 3;
        } catch (NumberFormatException e) {
            // timeout not specified (it is optional)
        }

        // Move the target app arguments into special array
        int len = args.length - (idx + 1);
        String[] targetAppArgs = new String[len];
        System.arraycopy(args, idx + 1, targetAppArgs, 0, len);

        // Start the communication thread and wait for it to establish connection with client
        profilerServer = new ProfilerServer(portNo, true, timeout);
        profilerServer.start();

        while (!(connectionOpen || connectionFailed)) {
            delay(50);
        }

        if (connectionFailed) {
            // prevent the console from dying without the user being able to see the error
            // pressEnterToShutDown();
            // no cleanup in this case, as there is no connection established
            preemptExit = false;
            System.exit(-1);
        }

        ProfilerInterface.setProfilerServer(profilerServer);

        initSupportingFunctionality(false, profilerServer.isRemoteProfiling());

        // Accept, or wait for, the client command to start the target app, and then start it.
        while (!startTargetApp) {
            delay(100);
        }

        runTargetApp(args[idx], targetAppArgs);
        targetAppMainThreadComplete = true;

        // If we haven't actually managed to start the app, notify the waiting communication thread.
        if (startupException != null) {
            synchronized (targetAppRunningLock) {
                targetAppRunningLock.notify();
            }
        }

        // Wait for some time in case the target app started some threads and then exited the main thread, while the
        // offspring threads have not yet fully initialized
        delay(300);

        // Now wait until all target app threads (excluding this, main one) terminate.
        while (Threads.targetAppThreadsExist()) {
            delay(300);
        }

        status.targetAppRunning = false;
        ProfilerInterface.disableProfilerHooks(); // So that e.g. System.exit() doesn't cause class loads and command sends
                                                  // DEBUGGING: if it's needed to check how good is the sampling interval when sampled instrumentation is used,
                                                  // decomment the one below to make the sampling thread stop here and report the debug data.
                                                  // ProfilerRuntimeCPUSampledInstr.clearDataStructures();

        profilerServer.sendSimpleCmdToClient(Command.SHUTDOWN_INITIATED);
        waitForShutdownOK();
        forcedShutdown();
    }

    public boolean isRemoteProfiling() {
        String socketAddr = clientSocket.getRemoteSocketAddress().toString();
        boolean res = !(((socketAddr.indexOf("127.0.0.1") != -1) || socketAddr.startsWith("localhost"))); // NOI18N

        if (res) {
            System.out.println(REMOTE_CONNECTION_MSG);
        } else {
            System.out.println(LOCAL_CONNECTION_MSG);
        }

        return res;
    }

    public static void notifyClientOnResultsAvailability() {
        if (!connectionOpen) {
            return;
        }

        if (profilerServer == null) {
            return; // in calibration mode
        }

        synchronized (resultsNotifiedLock) {
            if (resultsNotified) {
                return; // no need to notify again
            }

            resultsNotified = true;
            profilerServer.sendSimpleCmdToClient(Command.RESULTS_AVAILABLE);
        }
    }

    public static void requestClientResetResults() {
        ProfilerInterface.resetProfilerCollectors();
        ProfilerCalibrator.resetInternalStatsCollectors();
    }

    public static void requestClientTakeSnapshot() {
        if (profilerServer == null) {
            return; // in calibration mode
        }

        profilerServer.sendSimpleCmdToClient(Command.TAKE_SNAPSHOT);
    }

    public boolean getAndCheckLastResponse() {
        Response resp = getLastResponse();

        return resp.isOK();
    }

    public void run() {
        if (connectToClient()) {
            while (!profilerInterfaceInitialized) {
                delay(50);
            }

            listenToClient();
        } else {
            preemptExit = false;
        }
    }

    public void sendClassLoaderUnloadingCommand() {
        sendSimpleCmdToClient(Command.CLASS_LOADER_UNLOADING);
        getLastResponse();
    }

    public synchronized void sendComplexCmdToClient(Command cmd) {
        try {
            wireIO.sendComplexCommand(cmd);
        } catch (IOException ex) {
            if (!detachCommandReceived) {
                handleIOExceptionOnSend(ex);
            }
        }
    }

    public synchronized void sendComplexResponseToClient(Response resp) {
        try {
            wireIO.sendComplexResponse(resp);
        } catch (IOException ex) {
            if (!detachCommandReceived) {
                handleIOExceptionOnSend(ex);
            }
        }
    }

    // Several methods to send commands specific for modules that use wireprotocol just occasionally
    public boolean sendEventBufferDumpedCommand(int length, byte[] buffer, int startPos) {
        EventBufferDumpedCommand cmd = new EventBufferDumpedCommand(length,buffer,startPos);
        sendComplexCmdToClient(cmd);

        Response resp = getLastResponse();

        return resp.isOK();
    }

    public synchronized void sendSimpleCmdToClient(int cmdType) {
        try {
            wireIO.sendSimpleCommand(cmdType);
        } catch (IOException ex) {
            if (!detachCommandReceived) {
                handleIOExceptionOnSend(ex);
            }
        }
    }

    public synchronized void sendSimpleResponseToClient(boolean val, String errorMessage) {
        try {
            wireIO.sendSimpleResponse(val, errorMessage);
        } catch (IOException ex) {
            if (!detachCommandReceived) {
                handleIOExceptionOnSend(ex);
            }
        }
    }

    // --- I18N Support ----------------------------------------------------------

    // This method is used for obtaining ResourceBundle from classes that can be
    // used by ProfilerServer in context of direct/dynamic attach.
    //
    // If path to profiler server libraries (.jar) is known, ResourceBundle is obtained
    // using custom classloader (solves problem with bootstrap classloader&dynamic attach)
    //
    // Does not support branding!
    static ResourceBundle getProfilerServerResourceBundle() {
        if (messages != null) {
            return messages;
        }

        // 1. try to get the ResourceBundle using custom classloader
        if (_fullJFluidPath != null) {
            try {
                messages = getProfilerServerResourceBundle(_fullJFluidPath);
            } catch (Exception e) {
                System.err.println("Profiler Server: Problem with customized initializing localized messages...\n"
                                   + e.getMessage()); // NOI18N
            }
        }

        ; // cannot find jfluid-server.jar or Bundle.properties not found

        if (messages != null) {
            return messages; // ResourceBundle successfuly loaded using custom classloader
        }

        // 2. try to get the ResourceBundle in standard way
        try {
            messages = ResourceBundle.getBundle("org.netbeans.lib.profiler.server.Bundle"); // NOI18N
        } catch (Exception e) {
            System.err.println("Profiler Server: Problem with default initializing localized messages...\n" + e.getMessage()); // NOI18N
        }

        ;

        return messages;
    }

    static void initLocalizedResources() {
        if (resourcesInitialized) {
            return;
        }

        messages = getProfilerServerResourceBundle();

        if (messages != null) {
            ENTER_TO_SHUTDOWN_MSG = messages.getString("ProfilerServer_EnterToShutdownMsg"); // NOI18N
            MAIN_CLASS_NOT_PUBLIC_MSG = messages.getString("ProfilerServer_MainClassNotPublicMsg"); // NOI18N
            INCORRECT_MAIN_MODIFIERS_MSG = messages.getString("ProfilerServer_IncorrectMainModifiersMsg"); // NOI18N
            UNEXPECTED_EXCEPTION_MSG = messages.getString("ProfilerServer_UnexpectedExceptionMsg"); // NOI18N
            ELAPSED_TIME_MSG = messages.getString("ProfilerServer_ElapsedTimeMsg"); // NOI18N
            REMOTE_CONNECTION_MSG = messages.getString("ProfilerServer_RemoteConnectionMsg"); // NOI18N
            LOCAL_CONNECTION_MSG = messages.getString("ProfilerServer_LocalConnectionMsg"); // NOI18N
            WAITING_ON_PORT_MSG = messages.getString("ProfilerServer_WaitingOnPortMsg"); // NOI18N
            WAITING_ON_PORT_TIMEOUT_MSG = messages.getString("ProfilerServer_WaitingOnPortTimeoutMsg"); // NOI18N
            CONNECTION_EXCEPTION_MSG = messages.getString("ProfilerServer_ConnectionExceptionMsg"); // NOI18N
            CONNECTION_TIMEOUT_MSG = messages.getString("ProfilerServer_ConnectionTimeoutMsg"); // NOI18N
            AGENT_ERROR_MSG = messages.getString("ProfilerServer_AgentErrorMsg"); // NOI18N
            CONNECTION_INTERRUPTED_MSG = messages.getString("ProfilerServer_ConnectionInterruptedMsg"); // NOI18N
            COMMAND_EXCEPTION_MSG = messages.getString("ProfilerServer_CommandExceptionMsg"); // NOI18N
            RESPONSE_EXCEPTION_MSG = messages.getString("ProfilerServer_ResponseExceptionMsg"); // NOI18N
            CONNECTION_CLOSED_MSG = messages.getString("ProfilerServer_ConnectionClosedMsg"); // NOI18N
            INCORRECT_AGENT_ID_MSG = messages.getString("ProfilerServer_IncorrectAgentIdMsg"); // NOI18N
            THREAD_EXCEPTION_MSG = messages.getString("ProfilerServer_ThreadExceptionMsg"); // NOI18N
            THREAD_WAIT_EXCEPTION_MSG = messages.getString("ProfilerServer_ThreadWaitExceptionMsg"); // NOI18N
            resourcesInitialized = true;
        }
    }

    static void loadNativeLibrary(String fullJFluidPath, boolean fullPathToLibSpecified) {
        String libFullName = Platform.getAgentNativeLibFullName(fullJFluidPath, fullPathToLibSpecified, null, -1);
        System.load(libFullName);
    }

    static boolean startProfilingPointsActive() {
        if (status != null) {
            return status.startProfilingPointsActive;
        }

        return false;
    }

    private static File getInfoFile(int port) throws IOException {
        String dirName = Platform.getProfilerUserDir();

        return new File(dirName + File.separator + port); // NOI18N
    }

    private static void setShutdownOK() {
        synchronized (shutdownLock) {
            shutdownOK = true;
            shutdownLock.notifyAll();
        }
    }

    private static void cleanupOnShutdown() {
        Monitors.shutdown();
        ProfilerInterface.disableProfilerHooks();
        ProfilerRuntimeCPU.enableProfiling(false); 

        // Bugfix for 65947: Profiler blocks a finishing profiled application
        // The following connectionOpen = false is done just to prevent error message from listenToClient(). When the connection
        // is closed either by the client or here by closeConnection(), whoever is faster, listenToClient() waiting for input in socket
        // will get IOException.
        // Be careful with this! sendResponseToClient() currently doesn't check connectionOpen value, but if it does, this should be changed.
        connectionOpen = false;
        profilerServer.sendSimpleCmdToClient(Command.SHUTDOWN_COMPLETED);
        profilerServer.closeConnection();
        profilerServer.stopSeparateCmdExecutionThread();
    }

    private static void delay(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
        }
    }

    /**
     * Note that putting the code of this into the custom thread above and thus executing in "attach and startup"
     * in a separate thread as well, causes the VM to crash. Probably a new thread can't be created in a call from
     * pre-main function.
     *
     * @param activateCode ATTACH_DYNAMIC or ATTACH_DIRECT
     * @see #ATTACH_DYNAMIC
     * @see #ATTACH_DIRECT
     */
    private static void doActivate(int activateCode) {
        ProfilerInterface.disableProfilerHooks(); // Just in case
        initInternals();

        // Start the communication thread and wait for it to establish connection with client
        profilerServer = new ProfilerServer(_portNo, activateCode == ATTACH_DYNAMIC, _timeOut);
        profilerServer.start();

        while (!(connectionOpen || connectionFailed)) {
            delay(100);
        }

        if (connectionFailed) {
            if (activateCode == ATTACH_DIRECT) {
                System.exit(-1);
            } else {
                return; // in dynamic attach we just continue with execution
            }
        }

        ProfilerInterface.setProfilerServer(profilerServer);

        initSupportingFunctionality(true, profilerServer.isRemoteProfiling());

        if (_activateCode == ATTACH_DIRECT) {
            // "Attach on startup", where we normally wait until the initiate instrumentation request arrives and instrumentation starts.
            // However, the user can also choose to resume the target app without any instrumentation
            while ((ProfilerInterface.getCurrentInstrType() == INSTR_NONE) && !status.targetAppRunning) {
                delay(200);
            }

            delay(100); // Wait a bit more to make sure the classLoadHook is really set
        }

        status.targetAppRunning = true;
    }

    private static void forcedShutdown() {
        cleanupOnShutdown();
        preemptExit = false;
        System.exit(-1);
    }

    private static void initInternals() {
        shutdownWaitThread = new ShutdownWaitThread();
        Runtime.getRuntime().addShutdownHook(shutdownWaitThread);
        profilerInterfaceInitialized = false;
        connectionOpen = false;
        connectionFailed = false;
        detachCommandReceived = false;
        profilerServer = null;
        status = null;
        startTargetApp = false;
        startupException = null;
        targetAppRunningLock = new Object();
        execInSeparateThreadLock = new Object();

        // Preload this class, to avoid possible strange problems that may happen in case of wire protocol errors, that in
        // turn may cause loading of this class, that in turn may invoke classLoadHook, etc.
        try {
            Class.forName("java.net.SocketException"); // NOI18N
        } catch (ClassNotFoundException ex) { /* Shouldn't happen */
        }

        // Preload this class, to avoid possible strange problems that happen during Entire App CPU profiling of tomcat,
        // where classLoadHook is invoked during processing GET_DEFINING_CLASSLOADER request
        try {
            Class.forName("java.util.AbstractList$Itr"); // NOI18N
        } catch (ClassNotFoundException ex) { /* Shouldn't happen */
        }

        ThreadInfo.clearProfilerServerThreads();
    }

    /**
     * Called after the connection with the tool is established, i.e. we know that we are connected, in which mode
     * (attached or called directly) and whether it's local or remote connection.
     */
    private static void initSupportingFunctionality(boolean inAttachedMode, boolean remoteProfiling) {
        status = new ProfilingSessionStatus();
        status.runningInAttachedMode = inAttachedMode;
        status.remoteProfiling = remoteProfiling;
        status.targetJDKVersionString = Platform.getJDKVersionString();

        Monitors.initialize(); // Initialize before initProfilerInterface to get monitor thread(s) recorded as system thread(s)
                               // Also initialize before initProfilerInterface, same purpose

        profilerServer.initSeparateCmdExecutionThread();
        ThreadInfo.addProfilerServerThread(shutdownWaitThread);
        // Profiler interface initialization includes recording profiler's own threads (all currently running threads minus the
        // current thread, since it will become the target app's main thread).
        ProfilerInterface.initProfilerInterface(status, inAttachedMode ? profilerServer : Thread.currentThread());

        // This is to preload some classes that can otherwise be loaded at inappropriate time and cause class load hook firing.
        if (remoteProfiling) {
            ClassBytesLoader.preloadClasses();
        }

        profilerInterfaceInitialized = true;
    }

    private static void pressEnterToShutDown() {
        // Make sure any excessive previous input doesn't cause us to shut down immediately
        try {
            while (System.in.available() > 0) {
                System.in.read();
            }
        } catch (IOException ex) {
            // ignore
        }

        System.out.println(ENTER_TO_SHUTDOWN_MSG); // NOI18N

        try {
            System.in.read();
        } catch (IOException ex) {
            // ignore
        }
    }

    private static void runTargetApp(String mainClassName, String[] mainArgs) {
        Class targetMainClass = null;

        try {
            targetMainClass = ClassLoader.getSystemClassLoader().loadClass(mainClassName);
        } catch (ClassNotFoundException ex) {
            startupException = ex;
            System.err.println(ex);

            return;
        }

        // For the reasons I don't quite understand, if the main class is not public, then somewhere (when we attempt to invoke the
        // main method using reflection?) we get the following: "java.lang.IllegalAccessException: Class org.netbeans.lib.profiler.server.ProfilerServer
        // can not access a member of class Test with modifiers "public static"". Thus we have to run the below preemptive check. Hope this is not
        // a problem for the majority of our users...
        if (!Modifier.isPublic(targetMainClass.getModifiers())) {
            startupException = new IllegalAccessException(MessageFormat.format(MAIN_CLASS_NOT_PUBLIC_MSG,
                                                                               new Object[] { targetMainClass })); // NOI18N
            System.err.println(startupException);

            return;
        }

        Method targetMainMethod = null;
        Class[] params = new Class[] { String[].class };

        try {
            targetMainMethod = targetMainClass.getDeclaredMethod("main", params); // NOI18N
        } catch (NoSuchMethodException ex) {
            startupException = ex;
            System.err.println(ex);

            return;
        }

        // Check for correct method modifiers, to (hopefully) avoid IllegalAccessException and IllegalArgumentException
        int mod = targetMainMethod.getModifiers();

        if (!(Modifier.isPublic(mod) && Modifier.isStatic(mod)) || Modifier.isAbstract(mod) || Modifier.isInterface(mod)) {
            startupException = new IllegalAccessException(MessageFormat.format(INCORRECT_MAIN_MODIFIERS_MSG,
                                                                               new Object[] { targetMainClass })); // NOI18N
            System.err.println(startupException);

            return;
        }

        // We hope after our checks the only exceptions that can be thrown by the target app are those that it generates for
        // natural reasons, and which we should not report as "failed to start the application"
        status.targetAppRunning = true;

        synchronized (targetAppRunningLock) {
            targetAppRunningLock.notify();
        }

        long startTime = Timers.getCurrentTimeInCounts();

        try {
            targetMainMethod.invoke(targetMainClass, new Object[] { mainArgs });
        } catch (IllegalAccessException e1) {
            startupException = e1;
            System.err.println(e1);
        } catch (IllegalArgumentException e2) {
            startupException = e2;
            System.err.println(e2);
        } catch (InvocationTargetException e3) {
            Throwable cause = e3.getCause();

            if (cause != null) {
                cause.printStackTrace(System.err);
            } else { // Can this ever happen?
                internalError("Target application threw a null exception?"); // NOI18N
            }
        } catch (Throwable ex) {
            ProfilerInterface.disableProfilerHooks();
            internalError(MessageFormat.format(UNEXPECTED_EXCEPTION_MSG, new Object[] { ex }), false); // NOI18N
            ex.printStackTrace(System.err);
        } finally {
            int elapsedTime = (int) (((Timers.getCurrentTimeInCounts() - startTime) * 1000) / Timers.getNoOfCountsInSecond());
            System.out.println(MessageFormat.format(ELAPSED_TIME_MSG, new Object[] { "" + elapsedTime })); // NOI18N
        }
    }

    private static void waitForShutdownOK() {
        synchronized (shutdownLock) {
            while (!shutdownOK && !Thread.interrupted()) {
                try {
                    shutdownLock.wait(500);
                } catch (InterruptedException e) {
                }

                Thread.yield();
            }

            if (shutdownOK) {
                return;
            }
        }

        System.err.println("ProfilerServer hasn't shut down cleanly. Terminated."); // NOI18N

        //    while (true) {
        //      if (shutdownOK) {
        //        return;
        //      }
        //      delay(100);
        //    }
    }

    private int getAgentId() {
        if (agentId == -1) {
            String id = System.getProperty("nbprofiler.agentid"); // NOI18N

            if (id != null) {
                try {
                    agentId = Integer.parseInt(id);
                } catch (NumberFormatException e) {
                    System.err.println(MessageFormat.format(INCORRECT_AGENT_ID_MSG, new Object[] { id })); // NOI18N
                                                                                                           // ignore, the agentId will be generated randomly
                }
            }

            if (agentId == -1) {
                agentId = (int) (Math.random() * (float) Integer.MAX_VALUE);
            }
        }

        return agentId;
    }

    private static void setLastResponse(Response r) {
        synchronized (responseLock) {
            lastResponse = r;

            try {
                responseLock.notify();
            } catch (IllegalMonitorStateException ex) {
                internalError("IllegalMonitorState in ProfilerServer.setLastResponse()"); // NOI18N
            }
        }
    }

    private static String getLocalizedJFluidServerJar(String jfluidServerDir) {
        String localizedJFluidServerJar = null;

        // normalize provided directory to use forward slashes with slash at the end of path
        String baseDir = jfluidServerDir.replace('\\', '/'); // NOI18N

        if (!baseDir.endsWith("/")) { // NOI18N
            baseDir = baseDir + "/"; // NOI18N
        }

        // check if directory exists
        File baseDirF = new File(baseDir);

        if (!baseDirF.exists() || !baseDirF.isDirectory()) {
            return null;
        }

        // check if locale directory exists
        String localeDir = baseDir + "locale/"; // NOI18N
        File localeDirF = new File(localeDir);

        if (localeDirF.exists() && localeDirF.isDirectory()) {
            // locale directory found, try to find jar inside
            localizedJFluidServerJar = getLocalizedJFluidServerJarInDir(localeDir);

            if (localizedJFluidServerJar != null) {
                return localizedJFluidServerJar;
            }
        }

        // locale directory doesn't exist or jar not found in it, try to find jar directly in jfluid dir
        localizedJFluidServerJar = getLocalizedJFluidServerJarInDir(baseDir);

        return localizedJFluidServerJar;
    }

    private static String getLocalizedJFluidServerJarInDir(String jfluidServerLocaleDir) {
        LocaleIterator localeIterator = new LocaleIterator(Locale.getDefault());
        String jarFile;
        File jarFileF;

        while (localeIterator.hasNext()) {
            jarFile = jfluidServerLocaleDir + "jfluid-server" + localeIterator.next() + ".jar"; // NOI18N
            jarFileF = new File(jarFile);

            if (jarFileF.exists() && jarFileF.isFile()) {
                return jarFile;
            }
        }

        return null;
    }

    // Does not support branding!
    private static ResourceBundle getProfilerServerResourceBundle(String jfluidPath) {
        ResourceBundle bundle = null;

        if (jfluidPath == null) {
            throw new RuntimeException("ProfilerServer: Unable to initialize ResourceBundle for ProfilerServer, " // NOI18N
                                       + "cannot find path to Profiler libraries" // NOI18N
                                       );
        }

        String jfluidServerJar = getLocalizedJFluidServerJar(jfluidPath);

        if (jfluidServerJar == null) {
            throw new RuntimeException("ProfilerServer: Unable to initialize ResourceBundle for ProfilerServer, " // NOI18N
                                       + "cannot find localized jfluid-server.jar" // NOI18N
                                       );
        }

        try {
            if (!jfluidServerJar.startsWith("/")) {
                jfluidServerJar = "/" + jfluidServerJar; // NOI18N
            }

            String bundleJarURLPath = "jar:file:" + jfluidServerJar + "!/"; // NOI18N
            URLClassLoader loader = new URLClassLoader(new URL[] { new URL(bundleJarURLPath) });
            bundle = ResourceBundle.getBundle("org.netbeans.lib.profiler.server.Bundle", Locale.getDefault(), loader); // NOI18N
        } catch (Exception e2) {
            throw new RuntimeException("ProfilerServer: Unable to initialize ResourceBundle for ProfilerServer\n"
                                       + e2.getMessage() // NOI18N
            );
        }

        if (bundle == null) {
            throw new RuntimeException("ProfilerServer: Unable to initialize ResourceBundle for ProfilerServer" // NOI18N
            );
        }

        return bundle;
    }

    private synchronized void closeConnection() {
        connectionOpen = false;
        status.targetAppRunning = false;
        removeInfoFile();

        try {
            socketOut.close();
            socketIn.close();
            clientSocket.close();
            serverSocket.close();
        } catch (IOException ex) {
        }

        if (status.runningInAttachedMode) {
            System.out.println(CONNECTION_CLOSED_MSG);
        }

        preemptExit = false;
    }

    private boolean connectToClient() {
        try {
            if (serverTimeout == 0) {
                System.out.println(MessageFormat.format(WAITING_ON_PORT_MSG,
                                                        new Object[] {
                                                            "" + serverPort, // NOI18N
                "" + CommonConstants.CURRENT_AGENT_VERSION // NOI18N
                                                        }));
            } else {
                System.out.println(MessageFormat.format(WAITING_ON_PORT_TIMEOUT_MSG,
                                                        new Object[] {
                                                            "" + serverPort, // NOI18N
                "" + serverTimeout, // NOI18N
                "" + CommonConstants.CURRENT_AGENT_VERSION
                                                        } // NOI18N
                )); // NOI18N
            }

            serverSocket = new ServerSocket(serverPort);
            serverSocket.setSoTimeout(serverTimeout * 1000); // serverTimeout is in seconds
            createInfoFile();
            clientSocket = serverSocket.accept();
            clientSocket.setTcpNoDelay(true); // Necessary at least on Solaris to avoid delays in e.g. readInt() etc.
            socketIn = new ObjectInputStream(clientSocket.getInputStream());
            socketOut = new ObjectOutputStream(clientSocket.getOutputStream());
            wireIO = new WireIO(socketOut, socketIn);
            connectionOpen = true;

            return true;
        } catch (SocketTimeoutException ex) {
            System.err.println(CONNECTION_TIMEOUT_MSG); // NOI18N
            connectionFailed = true;
        } catch (IOException ex) {
            System.err.println(MessageFormat.format(CONNECTION_EXCEPTION_MSG, new Object[] { ex })); // NOI18N
            connectionFailed = true;
        } finally {
            //removeInfoFile ();
        }

        return false;
    }

    private void createInfoFile() {
        BufferedOutputStream bos = null;

        try {
            File f = getInfoFile(serverPort);
            f.createNewFile();
            f.deleteOnExit();

            Properties props = new Properties();
            props.setProperty("dynamic", Boolean.toString(dynamic)); // NOI18N
            props.setProperty("working.dir", System.getProperty("user.dir")); // NOI18N
            props.setProperty("agent.id", Integer.toString(getAgentId())); // NOI18N
            props.setProperty("java.version", System.getProperty("java.version")); // NOI18N

            FileOutputStream fos = new FileOutputStream(f);
            bos = new BufferedOutputStream(fos);

            props.store(bos, ""); // NOI18N

            bos.close();
        } catch (IOException e) {
            System.err.println(MessageFormat.format(AGENT_ERROR_MSG, new Object[] { e.getMessage() })); // NOI18N
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Some of the commands need to be executed in a separate thread, because they result in the server sending something
     * to the client and awaiting its response. The response, in turn, can only be picked up by the single JFluid communication
     * thread. So we execute these commands in a separate thread to allow the main communication thread to return immediately,
     * and be ready to process client's response.
     */
    private void executeInSeparateThread(int opCode) {
        synchronized (execInSeparateThreadLock) {
            execInSeparateThreadOpCode = opCode;

            try {
                execInSeparateThreadLock.notify();
            } catch (IllegalMonitorStateException ex) {
                System.err.println(THREAD_EXCEPTION_MSG); // NOI18N
            }
        }
    }

    //---------------------------------------------------------------------------------------
    // Command/response handling
    //---------------------------------------------------------------------------------------
    private void handleClientCommand(Command cmd) {
        //System.out.println(">>> Got command " + cmd);
        if (cmd.getType() == Command.START_TARGET_APP) {
            if (status.runningInAttachedMode) {
                // This is a special case - the user has chosen "Attach on startup" and then "resume application without instrumentation"
                status.targetAppRunning = true;
                sendSimpleResponseToClient(true, null);

                return;
            }

            // Start target app is handled by a separate thread, since we want to return to the client a synchronous response telling
            // whether or not the target app was started successfully. To get an answer to this question, we have to wait until the main
            // class is loaded, its main method is found, etc. Only after that the targetAppRunningLock.notify() is called. Until then
            // this thread remains blocked. However, if instrumentation root method == main method, class load hook is invoked immediately
            // when the main class is loaded. Class load hook, in turn, sends a RootClassLoaded command to the server and waits for the
            // response. But responses are read by the same thread that calls handleClientCommand(). So if we do the below operations in
            // the same thread, we deadlock - so, a separate thread is needed to allow the main listener thread to handle incoming
            // commands/responses immediately.
            class MyThread extends Thread {
                MyThread() {
                    ThreadInfo.addProfilerServerThread(this);
                    this.setName(PROFILER_SPECIAL_EXEC_THREAD_NAME + " 4"); // NOI18N
                }

                public void run() {
                    synchronized (targetAppRunningLock) {
                        startTargetApp = true;

                        try {
                            targetAppRunningLock.wait();
                        } catch (InterruptedException ex) {
                            internalError("START_TARGET_APP");
                        } // NOI18N
                    }

                    if (startupException != null) {
                        sendSimpleResponseToClient(false, startupException.toString());
                    } else {
                        sendSimpleResponseToClient(true, null);
                    }

                    ThreadInfo.removeProfilerServerThread(this);
                }
            }
            new MyThread().start();

            return;
        }

        switch (cmd.getType()) {
            case Command.GET_MONITORED_NUMBERS:
                sendComplexResponseToClient(Monitors.getMonitoredNumbers());

                break;
            case Command.INITIATE_INSTRUMENTATION:

                // Bugfix 69645: Take snapshot is not enabled after modifying profiling from CPU to memory
                // http://profiler.netbeans.org/issues/show_bug.cgi?id=69645
                synchronized (resultsNotifiedLock) {
                    resultsNotified = false;
                }

                try {
                    ProfilerInterface.initiateInstrumentation((InitiateInstrumentationCommand) cmd, status.targetAppRunning);
                    sendSimpleResponseToClient(true, null);
                } catch (Exception ex) {
                    sendSimpleResponseToClient(false, ex.getMessage());
                }

                break;
            case Command.INSTRUMENT_METHOD_GROUP:
                class InstrumentMethodGroupThread extends Thread {
                    final InstrumentMethodGroupCommand methodGroupCmd;
                    String exceptionString;

                    InstrumentMethodGroupThread(InstrumentMethodGroupCommand cmd) {
                        ThreadInfo.addProfilerServerThread(this);
                        setName(PROFILER_SPECIAL_EXEC_THREAD_NAME + " 8"); // NOI18N
                        methodGroupCmd = cmd;
                    }

                    public void run() {
                        try {
                            ProfilerInterface.instrumentMethods(methodGroupCmd);
                        } catch (Exception ex) {
                            exceptionString = ex.getLocalizedMessage();
                        }

                        ThreadInfo.removeProfilerServerThread(this);
                    }
                }

                InstrumentMethodGroupThread instrumentMethodGroupThread = new InstrumentMethodGroupThread((InstrumentMethodGroupCommand) cmd);
                instrumentMethodGroupThread.start();

                while(instrumentMethodGroupThread.isAlive()) {
                    delay(2000);
                    sendSimpleCmdToClient(Command.STILL_ALIVE);
                }

                if (instrumentMethodGroupThread.exceptionString != null) {
                    sendSimpleResponseToClient(false, instrumentMethodGroupThread.exceptionString);
                } else {
                    sendSimpleResponseToClient(true, null);
                }

                break;
            case Command.CHECK_CONNECTION:
                sendSimpleResponseToClient(true, null);

                break;
            case Command.SET_CHANGEABLE_INSTR_PARAMS:

                SetChangeableInstrParamsCommand scipCmd = (SetChangeableInstrParamsCommand) cmd;
                ProfilerRuntimeCPU.setNProfiledThreadsLimit(scipCmd.getNProfiledThreadsLimit());
                ProfilerRuntimeCPUSampledInstr.setSamplingInterval(scipCmd.getSamplingInterval());
                ProfilerRuntimeMemory.setSamplingInterval((short) scipCmd.getObjAllocStackSamplingInterval());
                ProfilerRuntimeMemory.setSamplingDepth(scipCmd.getObjAllocStackSamplingDepth());
                ProfilerRuntimeObjLiveness.setRunGCOnGetResults(scipCmd.getRunGCOnGetResultsInMemoryProfiling());
                Classes.setWaitTrackingEnabled(scipCmd.getWaitTrackingEnabled());
                Classes.setSleepTrackingEnabled(scipCmd.getSleepTrackingEnabled());
                sendSimpleResponseToClient(true, null);

                break;
            case Command.SET_UNCHANGEABLE_INSTR_PARAMS:

                SetUnchangeableInstrParamsCommand sucipCmd = (SetUnchangeableInstrParamsCommand) cmd;
                ProfilerRuntimeCPU.setTimerTypes(sucipCmd.getAbsoluteTimerOn(), sucipCmd.getThreadCPUTimerOn());
                status.instrScheme = sucipCmd.getInstrScheme();
                ProfilerRuntimeCPUCodeRegion.setCPUResBufSize(sucipCmd.getCodeRegionCPUResBufSize());
                sendSimpleResponseToClient(true, null);

                break;
            case Command.CPU_RESULTS_EXIST:
                sendSimpleResponseToClient(ProfilerInterface.cpuResultsExist(), null);

                break;
            case Command.DUMP_EXISTING_RESULTS:
            case Command.DUMP_EXISTING_RESULTS_LIVE:
                // We have to execute the dump in a separate thread to make this call (handleClientCommand()) return immediately.
                // Otherwise, it would not allow the server to receive a response from the client, that the client sends when it
                // processes the dumped results. Generally, all commands that may call ProfilerRuntime.dumpEventBuffer() should be
                // executed in a separate thread.
                executeInSeparateThread(cmd.getType());

                break;
            case Command.GET_CODE_REGION_CPU_RESULTS:
                sendComplexResponseToClient(ProfilerInterface.getCodeRegionCPUResults());

                break;
            case Command.GET_OBJECT_ALLOCATION_RESULTS:
                sendComplexResponseToClient(ProfilerInterface.getObjectAllocationResults());

                break;
            case Command.GET_METHOD_NAMES_FOR_JMETHOD_IDS:

                GetMethodNamesForJMethodIdsCommand gmnCmd = (GetMethodNamesForJMethodIdsCommand) cmd;
                sendComplexResponseToClient(ProfilerInterface.getMethodNamesForJMethodIds(gmnCmd.getMethodIds()));

                break;
            case Command.RESET_PROFILER_COLLECTORS:

                synchronized (resultsNotifiedLock) {
                    resultsNotified = false;
                }

                // Since the resetProfilerCollectors() eventually invokes the dump results method, which in turn sends a command to the client
                // and awaits response, we have to execute it in a separate thread. See comments in DUMP_EXISTING_RESULTS above.
                executeInSeparateThread(cmd.getType());

                break;
            case Command.DEACTIVATE_INJECTED_CODE:
                ProfilerInterface.deactivateInjectedCode();
                sendSimpleResponseToClient(true, null);

                break;
            case Command.GET_THREAD_LIVENESS_STATUS:
                sendComplexResponseToClient(ProfilerInterface.getCurrentThreadLivenessStatus());

                break;
            case Command.SUSPEND_TARGET_APP:
                ProfilerInterface.suspendTargetApp();
                sendSimpleResponseToClient(true, null);

                break;
            case Command.RESUME_TARGET_APP:
                ProfilerInterface.resumeTargetApp();
                sendSimpleResponseToClient(true, null);

                break;
            case Command.TERMINATE_TARGET_JVM:

                if (ProfilerInterface.getCurrentInstrType() != INSTR_NONE) {
                    ProfilerInterface.deactivateInjectedCode();
                }

                sendSimpleResponseToClient(true, null);
                closeConnection();
                preemptExit = false;
                doExit();

                break;
            case Command.SHUTDOWN_OK:
                setShutdownOK();

                break;
            case Command.INSTRUMENT_REFLECTION:
                ProfilerInterface.setInstrumentReflection(true);
                sendSimpleResponseToClient(true, null);

                break;
            case Command.DEINSTRUMENT_REFLECTION:
                ProfilerInterface.setInstrumentReflection(false);
                sendSimpleResponseToClient(true, null);

                break;
            case Command.RUN_GC:
                GC.runGC();
                sendSimpleResponseToClient(true, null);

                break;
            case Command.GET_DEFINING_CLASS_LOADER: {
                GetDefiningClassLoaderCommand gdclCmd = (GetDefiningClassLoaderCommand) cmd;
                int loaderId = ClassLoaderManager.getDefiningLoaderForClass(gdclCmd.getClassName(), gdclCmd.getClassLoaderId());
                DefiningLoaderResponse resp = new DefiningLoaderResponse(loaderId);
                sendComplexResponseToClient(resp);

                break;
            }
            case Command.GET_VM_PROPERTIES: {
                status.jvmArguments = Threads.getJVMArguments();
                status.javaCommand = Threads.getJavaCommand();

                VMPropertiesResponse resp = new VMPropertiesResponse(Platform.getJavaVersionString(), 
                                                                     System.getProperty("java.class.path"), // NOI18N
                                                                     System.getProperty("java.ext.dirs"), // NOI18N
                                                                     System.getProperty("sun.boot.class.path"), // NOI18N
                                                                     System.getProperty("user.dir"), // NOI18N
                                                                     status.jvmArguments, status.javaCommand,
                                                                     System.getProperty("os.name"), // NOI18N
                                                                     Runtime.getRuntime().maxMemory(),
                                                                     System.currentTimeMillis(), Timers.getCurrentTimeInCounts(),
                                                                     getAgentId() // NOI18N
                ); // NOI18N
                sendComplexResponseToClient(resp);

                break;
            }
            case Command.GET_STORED_CALIBRATION_DATA: { // called in the beginning of remote CPU profiling

                int ret = CalibrationDataFileIO.readSavedCalibrationData(status);

                if (ret == 0) {
                    CalibrationDataResponse resp = new CalibrationDataResponse(status.methodEntryExitCallTime,
                                                                               status.methodEntryExitInnerTime,
                                                                               status.methodEntryExitOuterTime,
                                                                               status.timerCountsInSecond);
                    profilerServer.sendComplexResponseToClient(resp);
                } else {
                    sendSimpleResponseToClient(false, CalibrationDataFileIO.getErrorMessage());
                }

                break;
            }
            case Command.RUN_CALIBRATION_AND_GET_DATA: {
                ProfilerCalibrator.init(status);
                ProfilerCalibrator.measureBCIOverhead(false);

                CalibrationDataResponse resp = new CalibrationDataResponse(status.methodEntryExitCallTime,
                                                                           status.methodEntryExitInnerTime,
                                                                           status.methodEntryExitOuterTime,
                                                                           status.timerCountsInSecond);
                profilerServer.sendComplexResponseToClient(resp);

                break;
            }
            case Command.GET_INTERNAL_STATS:
                ProfilerCalibrator.init(status);
                sendComplexResponseToClient(ProfilerCalibrator.getInternalStats());

                break;
            case Command.DETACH:
                // Just in case, normally should be deactivated and cleaned up by client
                ProfilerInterface.deactivateInjectedCode();
                ProfilerInterface.disableProfilerHooks();
                ProfilerInterface.clearProfilerDataStructures();

                stopSeparateCmdExecutionThread();
                Monitors.shutdown();
                ThreadInfo.clearProfilerServerThreads();
                detachCommandReceived = true;
                sendSimpleResponseToClient(true, null);

                break;
            case Command.TAKE_HEAP_DUMP:

                TakeHeapDumpCommand dumpCmd = (TakeHeapDumpCommand) cmd;
                String error = HeapDump.takeHeapDump(Platform.getJDKVersionNumber() == Platform.JDK_15, dumpCmd.getOutputFile());

                sendSimpleResponseToClient(error == null, error);

                break;
        }
    }

    private void doExit() {
        // try to call LifecycleManager in NetBeans first
        try {
            Class lookupClz = Thread.currentThread().getContextClassLoader().loadClass("org.openide.util.Lookup"); // NOI18N
            Method instMethod = lookupClz.getMethod("getDefault", new Class[0]); // NOI18N
            Method lookupMethod = lookupClz.getMethod("lookup", new Class[]{Class.class}); // NOI18N

            Object instance = instMethod.invoke(lookupClz, new Object[0]);
            if (instance != null) {
                ClassLoader clInstance = (ClassLoader)lookupMethod.invoke(instance, new Class[]{ClassLoader.class});
                if (clInstance != null) {
                    Class lcmInstanceClz = clInstance.loadClass("org.openide.LifecycleManager"); // NOI18N
                    Method lcmInstMethod = lcmInstanceClz.getMethod("getDefault", new Class[0]); // NOI18N
                    Method lcmExitMethod = lcmInstanceClz.getMethod("exit", new Class[0]); // NOI18N
                    Object lcmInstance = lcmInstMethod.invoke(lcmInstanceClz, new Object[0]);
                    lcmExitMethod.invoke(lcmInstance, new Object[0]);
                    return;
                }
            }
        } catch (Exception ex) {
            // ignore
        }
        // fall through a general system exit
        System.exit(-1);
    }

    private void handleIOExceptionOnSend(IOException ex) {
        System.err.println(MessageFormat.format(RESPONSE_EXCEPTION_MSG, new Object[] { ex })); // NOI18N
        ex.printStackTrace(System.err);
        closeConnection();
    }

    private void initSeparateCmdExecutionThread() {
        separateCmdExecutionThread = new SeparateCmdExecutionThread();
        separateCmdExecutionThread.start();
    }

    private static void internalError(String message) {
        internalError(message, true);
    }

    private static void internalError(String message, boolean exit) {
        System.err.println("Profiler Engine Error: " + message); // NOI18N

        if (exit) {
            preemptExit = false;
            System.exit(-1);
        }
    }

    private void listenToClient() {
        while (connectionOpen && !detachCommandReceived) {
            try {
                Object o = wireIO.receiveCommandOrResponse();

                if (o == null) {
                    System.err.println(CONNECTION_INTERRUPTED_MSG); // NOI18N

                    break; // end of connection
                }

                //System.out.println(">>> Profiler Engine: received command or response " + o);
                if (o instanceof Command) {
                    handleClientCommand((Command) o);
                } else {
                    setLastResponse((Response) o);
                }
            } catch (IOException ex) {
                if (connectionOpen && !detachCommandReceived) { // It is not an asynchronous connection shutdown
                    System.err.println(MessageFormat.format(COMMAND_EXCEPTION_MSG, new Object[] { ex })); // NOI18N
                }

                break;
            }
        }

        closeConnection();
    }

    private void removeInfoFile() {
        try {
            getInfoFile(serverPort).delete();
        } catch (IOException e) {
            System.err.println(MessageFormat.format(AGENT_ERROR_MSG, new Object[] { e.getMessage() })); // NOI18N
        }
    }

    private void stopSeparateCmdExecutionThread() {
        separateCmdExecutionThread.terminate();

        synchronized (execInSeparateThreadLock) {
            try {
                execInSeparateThreadLock.notify();
            } catch (IllegalMonitorStateException ex) {
            }
        }
    }
}
