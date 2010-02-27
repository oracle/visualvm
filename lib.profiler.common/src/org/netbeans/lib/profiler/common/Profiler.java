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

package org.netbeans.lib.profiler.common;

import java.awt.EventQueue;
import org.netbeans.lib.profiler.ProfilerClient;
import org.netbeans.lib.profiler.TargetAppRunner;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.client.ClientUtils.SourceCodeSelection;
import org.netbeans.lib.profiler.common.event.ProfilingStateEvent;
import org.netbeans.lib.profiler.common.event.ProfilingStateListener;
import org.netbeans.lib.profiler.common.filters.DefinedFilterSets;
import org.netbeans.lib.profiler.common.filters.GlobalFilters;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.instrumentation.BadLocationException;
import org.netbeans.lib.profiler.instrumentation.InstrumentationException;
import org.netbeans.lib.profiler.results.monitor.VMTelemetryDataManager;
import org.netbeans.lib.profiler.results.threads.ThreadsDataManager;
import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;
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
    private static final boolean DEBUG = System.getProperty("org.netbeans.lib.profiler.common.Profiler") != null; // NOI18N
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
     * @see CommonConstants.AGENT_STATE_CONNECTED
     * @see CommonConstants.AGENT_STATE_DIFFERENT_ID
     * @see CommonConstants.AGENT_STATE_NOT_RUNNING
     * @see CommonConstants.AGENT_STATE_OTHER_SESSION_IN_PROGRESS
     * @see CommonConstants.AGENT_STATE_READY_DIRECT
     * @see CommonConstants.AGENT_STATE_READY_DYNAMIC
     */
    public abstract int getAgentState(String host, int port, int agentId);

    public abstract SessionSettings getCurrentSessionSettings();

    public abstract DefinedFilterSets getDefinedFilterSets();

    public abstract GlobalFilters getGlobalFilters();

    public abstract GlobalProfilingSettings getGlobalProfilingSettings();

    public abstract ProfilingSettings getLastProfilingSettings();

    public abstract int getProfilingMode();

    public abstract int getProfilingState();

    public abstract TargetAppRunner getTargetAppRunner();

    public abstract ThreadsDataManager getThreadsManager();

    public abstract void setThreadsMonitoringEnabled(boolean enabled);

    public abstract boolean getThreadsMonitoringEnabled();

    public abstract VMTelemetryDataManager getVMTelemetryManager();

    public abstract boolean attachToApp(ProfilingSettings profilingSettings, AttachSettings as);

    public abstract boolean connectToStartedApp(ProfilingSettings profilingSettings, SessionSettings sessionSettings);

    public abstract void detachFromApp();

    /** Displays a user-level message with error. Can be run from any thread.
     * @param message The error message to display
     */
    public abstract void displayError(String message);

    /** Displays a user-level message with information.  Can be run from any thread.
     * @param message The message to display
     */
    public abstract void displayInfo(String message);

    /** Displays a user-level message with warning.  Can be run from any thread.
     * @param message The warning message to display
     */
    public abstract void displayWarning(String message);

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
     * @param e The exception that occured
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
            profilingStateListener.instrumentationChanged(-1, getTargetAppRunner().getProfilerClient().getCurrentInstrType());
        }
    }

    public boolean prepareInstrumentation(ProfilingSettings profilingSettings) {
        try {
            final ProfilerClient client = getTargetAppRunner().getProfilerClient();
            final int oldInstrType = client.getStatus().currentInstrType;

            switch (profilingSettings.getProfilingType()) {
                case ProfilingSettings.PROFILE_MONITOR:
                    client.removeAllInstrumentation();

                    break;
                case ProfilingSettings.PROFILE_MEMORY_ALLOCATIONS:
                    client.initiateMemoryProfInstrumentation(CommonConstants.INSTR_OBJECT_ALLOCATIONS);

                    break;
                case ProfilingSettings.PROFILE_MEMORY_LIVENESS:
                    client.initiateMemoryProfInstrumentation(CommonConstants.INSTR_OBJECT_LIVENESS);

                    break;
                case ProfilingSettings.PROFILE_CPU_ENTIRE:
                case ProfilingSettings.PROFILE_CPU_PART:
                    instrumentSelectedRoots(profilingSettings.getInstrumentationMethods());

                    break;
                case ProfilingSettings.PROFILE_CPU_STOPWATCH:

                    SourceCodeSelection[] fragment = new SourceCodeSelection[] { profilingSettings.getCodeFragmentSelection() };
                    client.initiateCodeRegionInstrumentation(fragment);

                    break;
            }

            fireInstrumentationChanged(oldInstrType, client.getStatus().currentInstrType);

            return true;
        } catch (ClientUtils.TargetAppOrVMTerminated e) {
            displayError(e.getMessage());
            e.printStackTrace(System.err);
        } catch (InstrumentationException e) {
            displayError(e.getMessage());
            e.printStackTrace(System.err);
        } catch (BadLocationException e) {
            displayError(e.getMessage());
            e.printStackTrace(System.err);
        } catch (ClassNotFoundException e) {
            displayError(e.getMessage());
            e.printStackTrace(System.err);
        } catch (IOException e) {
            displayError(e.getMessage());
        } catch (ClassFormatError e) {
            displayError(e.getMessage());
        }

        return false;
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
}
