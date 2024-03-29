/*
 * Copyright (c) 1997, 2023, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.common;

import java.awt.EventQueue;
import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;
import org.graalvm.visualvm.lib.common.event.ProfilingStateEvent;
import org.graalvm.visualvm.lib.common.event.ProfilingStateListener;
import org.graalvm.visualvm.lib.jfluid.ProfilerClient;
import org.graalvm.visualvm.lib.jfluid.TargetAppRunner;
import org.graalvm.visualvm.lib.jfluid.client.ClientUtils;
import org.graalvm.visualvm.lib.jfluid.client.ClientUtils.SourceCodeSelection;
import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;
import org.graalvm.visualvm.lib.jfluid.instrumentation.BadLocationException;
import org.graalvm.visualvm.lib.jfluid.instrumentation.InstrumentationException;
import org.graalvm.visualvm.lib.jfluid.results.monitor.VMTelemetryDataManager;
import org.graalvm.visualvm.lib.jfluid.results.threads.ThreadsDataManager;
import org.openide.util.Lookup;


/** An abstract superclass representing the entire Profiler.  The Profiler class should add a "state" on top of the
 * underlying JFluid engine, providing easier access to its various functions.
 *
 * A concrete subclass would implement the abstract methods in a specific way (integrated into an IDE, standalone GUI
 * tool, non-gui tool, etc.).
 *
 * This class can also be used by any profiling code to obtain the concrete implementation of the Profiler at any given
 * moment by calling the getDefault () method. Such parts, if they only need to call methods of the Profiler class
 * and/or any of the underlying libraries (jfluid.jar, jfluid-ui.jar) would then be independent of a concrete form of
 * the profiler, whether it if GUI/Non-GUI or Integrated/Standalone.
 *
 * @author Tomas Hurka
 * @author Ian Formanek
 */
public abstract class Profiler {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // Profiling states
    public static final int PROFILING_INACTIVE = 1; // no profiling session in progress
    public static final int PROFILING_STARTED = 2; // profiling session started but TA not yet running
    public static final int PROFILING_RUNNING = 4; // profiling session in progress and TA running
    public static final int PROFILING_PAUSED = 8; // profiling session in progress and TA paused (all threads suspended)
    public static final int PROFILING_STOPPED = 16; // profiling session finished, some results still available
    public static final int PROFILING_IN_TRANSITION = 128; // profiling state is in transition between states

    // Profiling modes
    public static final int MODE_ATTACH = 0;
    public static final int MODE_PROFILE = 1;

    // Logging & error management

    /** Message that would be useful for tracing events but which need not be a problem. */
    public static final int INFORMATIONAL = 1;

    /** Something went wrong in the software, but it is continuing and the user need not be bothered. */
    public static final int WARNING = 2;

    /** Something the user should be aware of. */
    public static final int USER = 4;

    /** Something went wrong, though it can be recovered. */
    public static final int EXCEPTION = 8;

    /** Serious problem, application may be crippled. */
    public static final int ERROR = 16;
    private static final boolean DEBUG = System.getProperty("org.graalvm.visualvm.lib.common.Profiler") != null; // NOI18N
    private static Profiler defaultProfiler;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private Vector profilingStateListeners;
    private int currentProfilingState = PROFILING_INACTIVE;

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static synchronized Profiler getDefault() {
        if (defaultProfiler == null) {
            defaultProfiler = (Profiler) Lookup.getDefault().lookup(Profiler.class);
            if (defaultProfiler == null) {
                throw new InternalError("Should never happen"); // NOI18N
            } else if (DEBUG) {
                System.err.println("Default Profiler succesfully installed: " + defaultProfiler); // NOI18N
            }
        }
        return defaultProfiler;
    }

    /** Obtains a state for agent listening on given port, checking whether it has the expected agentId.
     *
     * @param host Ignored for now
     * @param port Port number to be used to communicate with the agent
     * @param agentId Expected agent Id
     * @return the state of the agent
     * @see CommonConstants#AGENT_STATE_CONNECTED
     * @see CommonConstants#AGENT_STATE_DIFFERENT_ID
     * @see CommonConstants#AGENT_STATE_NOT_RUNNING
     * @see CommonConstants#AGENT_STATE_OTHER_SESSION_IN_PROGRESS
     * @see CommonConstants#AGENT_STATE_READY_DIRECT
     * @see CommonConstants#AGENT_STATE_READY_DYNAMIC
     */
    public abstract int getAgentState(String host, int port, int agentId);

    public abstract SessionSettings getCurrentSessionSettings();

    public abstract GlobalProfilingSettings getGlobalProfilingSettings();

    public abstract ProfilingSettings getLastProfilingSettings();

    public abstract int getProfilingMode();

    public abstract int getProfilingState();

    public abstract int getServerState();

    public abstract int getServerProgress();

    public abstract TargetAppRunner getTargetAppRunner();

    public abstract ThreadsDataManager getThreadsManager();

    public abstract void setThreadsMonitoringEnabled(boolean enabled);

    public abstract boolean getThreadsMonitoringEnabled();
    
    public abstract void setLockContentionMonitoringEnabled(boolean enabled);

    public abstract boolean getLockContentionMonitoringEnabled();

    public abstract VMTelemetryDataManager getVMTelemetryManager();

    public abstract boolean attachToApp(ProfilingSettings profilingSettings, AttachSettings as);

    public abstract boolean connectToStartedApp(ProfilingSettings profilingSettings, SessionSettings sessionSettings);

    public abstract void detachFromApp();

    public abstract void instrumentSelectedRoots(ClientUtils.SourceCodeSelection[] rootMethods)
                                          throws ClassNotFoundException, InstrumentationException, BadLocationException,
                                                 IOException, ClassFormatError, ClientUtils.TargetAppOrVMTerminated;

    /** Silently log a message. This is not intended for user-level error notification, but rather for
     * internal messages that would be logged based on the severity level.
     *
     * @param severity The severity of the problem
     * @param message The message to log
     */
    public abstract void log(int severity, String message);

    public abstract void modifyCurrentProfiling(ProfilingSettings profilingSettings);

    /** Notify the user about an internal error. This is not intended for user-level error notification, but rather for
     * internal unexpected problems that usually represent a bug in our code.
     *
     * @param severity The severity of the problem
     * @param e The exception that occurred
     */
    public abstract void notifyException(int severity, Exception e);

    public abstract void openJavaSource(String classname, String methodName, String methodSig);

    public abstract boolean profileClass(ProfilingSettings profilingSettings, SessionSettings sessionSettings);

    public abstract boolean rerunAvailable();

    public abstract boolean modifyAvailable();

    public abstract void rerunLastProfiling();

    public abstract boolean runCalibration(boolean checkForSaved, String jvmExecutable, String jdkString, int architecture);

    public abstract boolean shutdownBlockedAgent(String host, int port, int agentId);

    public abstract void stopApp();

    // ProfilingStateListener stuff
    public final void addProfilingStateListener(final ProfilingStateListener profilingStateListener) {
        if (profilingStateListeners == null) {
            profilingStateListeners = new Vector();
        }

        if (!profilingStateListeners.contains(profilingStateListener)) {
            profilingStateListeners.add(profilingStateListener);
            profilingStateListener.profilingStateChanged(new ProfilingStateEvent(-1, currentProfilingState, defaultProfiler));
            // should not add listeners in the middle of profiling session
            // profilingStateListener.instrumentationChanged(-1, getTargetAppRunner().getProfilerClient().getCurrentInstrType());
        }
    }

    public boolean prepareInstrumentation(ProfilingSettings profilingSettings)
            throws ClientUtils.TargetAppOrVMTerminated, InstrumentationException,
            BadLocationException, ClassNotFoundException, IOException, ClassFormatError {
        final ProfilerClient client = getTargetAppRunner().getProfilerClient();
        final int oldInstrType = client.getStatus().currentInstrType;

        switch (profilingSettings.getProfilingType()) {
            case ProfilingSettings.PROFILE_MONITOR:
                client.initiateMonitoring();

                break;
            case ProfilingSettings.PROFILE_MEMORY_SAMPLING:
                client.initiateMemoryProfInstrumentation(CommonConstants.INSTR_NONE_MEMORY_SAMPLING);

                break;
            case ProfilingSettings.PROFILE_MEMORY_ALLOCATIONS:
                client.initiateMemoryProfInstrumentation(CommonConstants.INSTR_OBJECT_ALLOCATIONS);

                break;
            case ProfilingSettings.PROFILE_MEMORY_LIVENESS:
                client.initiateMemoryProfInstrumentation(CommonConstants.INSTR_OBJECT_LIVENESS);

                break;
            case ProfilingSettings.PROFILE_CPU_ENTIRE:
            case ProfilingSettings.PROFILE_CPU_PART:
            case ProfilingSettings.PROFILE_CPU_JDBC:
                instrumentSelectedRoots(profilingSettings.getInstrumentationMethods());

                break;
            case ProfilingSettings.PROFILE_CPU_SAMPLING:
                client.initiateCPUSampling();
                break;
                
            case ProfilingSettings.PROFILE_CPU_STOPWATCH:

                SourceCodeSelection[] fragment = new SourceCodeSelection[] { profilingSettings.getCodeFragmentSelection() };
                client.initiateCodeRegionInstrumentation(fragment);

                break;
        }

        fireInstrumentationChanged(oldInstrType, client.getStatus().currentInstrType);

        return true;
    }

    public final boolean profilingInProgress() {
        final int state = getProfilingState();

        return ((state == PROFILING_PAUSED) || (state == PROFILING_RUNNING));
    }

    public final void removeProfilingStateListener(final ProfilingStateListener profilingStateListener) {
        if (profilingStateListeners != null) {
            profilingStateListeners.remove(profilingStateListener);
        }
    }

    public static void debug(String s) {
        if (DEBUG) {
            System.err.println("Profiler.DEBUG: " + s); // NOI18N
        }
    }

    public static void debug(Exception e) {
        if (DEBUG) {
            System.err.print("Profiler.DEBUG: "); // NOI18N
            e.printStackTrace(System.err);
        }
    }

    public abstract String getLibsDir();

    public abstract int getPlatformArchitecture(String platformName);

    public abstract String getPlatformJDKVersion(String platformName);

    public abstract String getPlatformJavaFile(String platformName);

    protected final void fireInstrumentationChanged(final int oldInstrType, final int currentInstrType) {
        if (profilingStateListeners == null) {
            return;
        }

        final Vector toNotify;

        synchronized (this) {
            toNotify = (Vector) profilingStateListeners.clone();
        }

        final Iterator iterator = toNotify.iterator();
        final Runnable r = new Runnable() {
            public void run() {
                while (iterator.hasNext()) {
                    ((ProfilingStateListener) iterator.next()).instrumentationChanged(oldInstrType, currentInstrType);
                }
            }
        };

        if (EventQueue.isDispatchThread()) {
            r.run();
        } else {
            EventQueue.invokeLater(r);
        }
    }

    protected final void fireProfilingStateChange(final int oldProfilingState, final int newProfilingState) {
        currentProfilingState = newProfilingState;

        if (profilingStateListeners == null) {
            return;
        }

        if (oldProfilingState == newProfilingState) {
            return;
        }

        final Vector toNotify;

        synchronized (this) {
            toNotify = (Vector) profilingStateListeners.clone();
        }

        final Iterator iterator = toNotify.iterator();
        final ProfilingStateEvent event = new ProfilingStateEvent(oldProfilingState, newProfilingState, this);
        final Runnable r = new Runnable() {
            public void run() {
                while (iterator.hasNext()) {
                    ((ProfilingStateListener) iterator.next()).profilingStateChanged(event);
                }
            }
        };

        if (EventQueue.isDispatchThread()) {
            r.run();
        } else {
            EventQueue.invokeLater(r);
        }
    }

    protected final void fireThreadsMonitoringChange() {
        if (profilingStateListeners == null) {
            return;
        }

        final Vector toNotify;

        synchronized (this) {
            toNotify = (Vector) profilingStateListeners.clone();
        }

        final Iterator iterator = toNotify.iterator();
        final Runnable r = new Runnable() {
            public void run() {
                while (iterator.hasNext()) {
                    ((ProfilingStateListener) iterator.next()).threadsMonitoringChanged();
                }
            }
        };

        if (EventQueue.isDispatchThread()) {
            r.run();
        } else {
            EventQueue.invokeLater(r);
        }
    }
    
    protected final void fireLockContentionMonitoringChange() {
        if (profilingStateListeners == null) {
            return;
        }

        final Vector toNotify;

        synchronized (this) {
            toNotify = (Vector) profilingStateListeners.clone();
        }

        final Iterator iterator = toNotify.iterator();
        final Runnable r = new Runnable() {
            public void run() {
                while (iterator.hasNext()) {
                    ((ProfilingStateListener) iterator.next()).lockContentionMonitoringChanged();
                }
            }
        };

        if (EventQueue.isDispatchThread()) {
            r.run();
        } else {
            EventQueue.invokeLater(r);
        }
    }

    protected final void fireServerStateChanged(final int serverState, final int serverProgress) {
        if (profilingStateListeners == null) {
            return;
        }

        final Vector toNotify;

        synchronized (this) {
            toNotify = (Vector) profilingStateListeners.clone();
        }

        final Iterator iterator = toNotify.iterator();
        final Runnable r = new Runnable() {
            public void run() {
                while (iterator.hasNext()) {
                    ((ProfilingStateListener) iterator.next()).serverStateChanged(serverState, serverProgress);
                }
            }
        };

        if (EventQueue.isDispatchThread()) {
            r.run();
        } else {
            EventQueue.invokeLater(r);
        }
    }
}
