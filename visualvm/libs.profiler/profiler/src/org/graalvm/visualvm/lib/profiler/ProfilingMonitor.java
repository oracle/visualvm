/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.profiler;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import org.graalvm.visualvm.lib.jfluid.TargetAppRunner;
import org.graalvm.visualvm.lib.jfluid.client.MonitoredData;
import org.graalvm.visualvm.lib.common.Profiler;
import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;
import org.graalvm.visualvm.lib.jfluid.results.monitor.VMTelemetryDataManager;
import org.graalvm.visualvm.lib.jfluid.results.threads.ThreadsDataManager;
import javax.swing.*;
import org.graalvm.visualvm.lib.profiler.api.ProfilerDialogs;
import org.openide.util.NbBundle;


/**
 * This class provides thread for periodically processing monitoring data from profiled application (feeding them to
 * telemetry and threads data managers) as well as I/O redirection.
 *
 * @author Tomas Hurka
 * @author Ian Formanek
 */
public final class ProfilingMonitor {

    @NbBundle.Messages({
        "ProfilingMonitor_OomeMsg=<html><b>Not enough memory to store profiling data.</b><br><br>To avoid this error please increase the -Xmx value<br>in the etc/visualvm.conf file in VisualVM directory.</html>"
    })
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    final class UpdateThread extends Thread {
        //~ Static fields/initializers -------------------------------------------------------------------------------------------

        private static final int UPDATE_INTERVAL = 1200;

        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private TargetAppRunner runner;
        private ThreadsDataManager threadsDataManager;
        private VMTelemetryDataManager vmTelemetryManager;
//        private boolean doUpdateLiveResults;
        private boolean keepRunning = true;
        private volatile boolean oomeNotified;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        UpdateThread() {
            super("Profiler Monitor"); // NOI18N
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void monitor(final TargetAppRunner runner) {
            oomeNotified = false;
            this.runner = runner;
            this.threadsDataManager = Profiler.getDefault().getThreadsManager();
            this.vmTelemetryManager = Profiler.getDefault().getVMTelemetryManager();

            if (runner != null) {
                this.vmTelemetryManager.maxHeapSize = runner.getProfilingSessionStatus().maxHeapSize;
            }
        }

        public void run() {
            while (keepRunning) { // Main loop

                try {
                    if (runner != null) {
//                        ProfilerControlPanel2.getDefault().updateStatus(); // TODO: move elsewhere

                        final MonitoredData md = runner.getProfilerClient().getMonitoredData();

                        if (md != null) {
                            SwingUtilities.invokeAndWait(new Runnable() {
                                    public void run() {
                                        try {
                                            setServerState(md.getServerState());
                                            setServerProgress(md.getServerProgress());
                                            
                                            threadsDataManager.processData(md);
                                            vmTelemetryManager.processData(md);

                                            // ---------------------------------------------------------
                                            // Temporary workaround to refresh profiling points when LiveResultsWindow is not refreshing
                                            // TODO: move this code to a separate class performing the update if necessary
//                                            final Profiler profiler = Profiler.getDefault();
//                                            final ProfilerClient client = profiler.getTargetAppRunner().getProfilerClient();
//                                            final int instrType = client.getCurrentInstrType();
//                                            if ((NetBeansProfiler.getDefaultNB().processesProfilingPoints())
//                                                && (!doUpdateLiveResults /*|| !LiveResultsWindow.hasDefault()*/)) {
//                                                ProfilerUtils.runInProfilerRequestProcessor(new Runnable() {
//                                                        public void run() {
//                                                            try {
//                                                                if (instrType != ProfilerEngineSettings.INSTR_CODE_REGION) {
//                                                                    client.forceObtainedResultsDump(true);
//                                                                }
//                                                            } catch (Exception e /*ClientUtils.TargetAppOrVMTerminated targetAppOrVMTerminated*/) {
//                                                            }
//                                                        }
//                                                    });
//
//                                            }
//
//                                            // ---------------------------------------------------------
//
//                                            // Let results updating happen every other cycle (i.e. every ~2.5 sec) to allow the user to understand something before it disappears :-)
////                                            if (doUpdateLiveResults && LiveResultsWindow.hasDefault()) {
////                                                LiveResultsWindow.getDefault().refreshLiveResults();
////                                            }
//
//                                            doUpdateLiveResults = !doUpdateLiveResults;
                                        } catch (Exception e) {
                                            Profiler.getDefault().notifyException(Profiler.EXCEPTION, e);
                                        } catch (OutOfMemoryError e) {
                                            if (!oomeNotified) {
                                                oomeNotified = true;
                                                ProfilerDialogs.displayError(Bundle.ProfilingMonitor_OomeMsg());
                                            }
                                        }
                                    }
                                });
                        } else {
                            NetBeansProfiler.getDefaultNB().checkAndUpdateState();
                            runner = null; // stop monitoring, the TA must have terminated
                        }
                    }
                } catch (Throwable t) {
                    // prevent thread from dying on exceptions from JFluid engine
                    if (t instanceof ThreadDeath) {
                        throw (ThreadDeath) t;
                    }
                }

                try {
                    sleep(UPDATE_INTERVAL);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        private void stopThread() {
            keepRunning = false;
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------
    static final String PROPERTY_SERVER_STATE = "serverState";
    static final String PROPERTY_SERVER_PROGRESS = "serverProgress";

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private UpdateThread monitorThread;
    private boolean updateThreadStarted = false;
    private int serverState = CommonConstants.SERVER_RUNNING;
    private int serverProgress = CommonConstants.SERVER_PROGRESS_INDETERMINATE;
    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Initializes the Form
     */
    public ProfilingMonitor() {
        monitorThread = new UpdateThread();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void monitorVM(final TargetAppRunner runner) {
        //set server state before first MONITORED_NUMBERS response arrives
        setServerState(CommonConstants.SERVER_INITIALIZING);
        setServerProgress(CommonConstants.SERVER_PROGRESS_INDETERMINATE);
        if (!updateThreadStarted) {
            updateThreadStarted = true;
            monitorThread.start();
        }

        monitorThread.monitor(runner);
    }

    public void stopDisplayingVM() {
        if (monitorThread != null) {
            monitorThread.monitor(null);
        }
    }

    public void stopUpdateThread() {
        if (monitorThread != null) {
            monitorThread.stopThread();
            monitorThread = null;
        }
    }

    private void setServerState(int serverState) {
        if(this.serverState != serverState) {
            int oldValue = this.serverState;
            this.serverState = serverState;
            propertyChangeSupport.firePropertyChange(PROPERTY_SERVER_STATE, oldValue, serverState);
        }
    }

    int getServerState() {
        return serverState;
    }

    private void setServerProgress(int serverProgress) {
        if(this.serverProgress != serverProgress)
        {
            int oldValue = this.serverProgress;
            this.serverProgress = serverProgress;
            propertyChangeSupport.firePropertyChange(PROPERTY_SERVER_PROGRESS, oldValue, serverProgress);
        }
    }

    int getServerProgress() {
        return serverProgress;
    }

    void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }
}
