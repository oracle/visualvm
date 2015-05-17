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

package org.netbeans.modules.profiler;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import org.netbeans.lib.profiler.ProfilerClient;
import org.netbeans.lib.profiler.ProfilerEngineSettings;
import org.netbeans.lib.profiler.TargetAppRunner;
import org.netbeans.lib.profiler.client.MonitoredData;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.results.monitor.VMTelemetryDataManager;
import org.netbeans.lib.profiler.results.threads.ThreadsDataManager;
import javax.swing.*;
import org.netbeans.modules.profiler.api.ProfilerDialogs;
import org.netbeans.modules.profiler.utilities.ProfilerUtils;
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
        "ProfilingMonitor_OomeMsg=<html><b>Not enough memory to store profiling data.</b><br><br>To avoid this error, increase the -Xmx value<br>in the etc/netbeans.conf file in NetBeans IDE installation.</html>"
    })
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    final class UpdateThread extends Thread {
        //~ Static fields/initializers -------------------------------------------------------------------------------------------

        private static final int UPDATE_INTERVAL = 1200;

        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private TargetAppRunner runner;
        private ThreadsDataManager threadsDataManager;
        private VMTelemetryDataManager vmTelemetryManager;
        private boolean doUpdateLiveResults;
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
                                            final Profiler profiler = Profiler.getDefault();
                                            final ProfilerClient client = profiler.getTargetAppRunner().getProfilerClient();
                                            final int instrType = client.getCurrentInstrType();
                                            if ((NetBeansProfiler.getDefaultNB().processesProfilingPoints())
                                                && (!doUpdateLiveResults /*|| !LiveResultsWindow.hasDefault()*/)) {
                                                ProfilerUtils.runInProfilerRequestProcessor(new Runnable() {
                                                        public void run() {
                                                            try {
                                                                if (instrType != ProfilerEngineSettings.INSTR_CODE_REGION) {
                                                                    client.forceObtainedResultsDump(true);
                                                                }
                                                            } catch (Exception e /*ClientUtils.TargetAppOrVMTerminated targetAppOrVMTerminated*/) {
                                                            }
                                                        }
                                                    });

                                            }

                                            // ---------------------------------------------------------

                                            // Let results updating happen every other cycle (i.e. every ~2.5 sec) to allow the user to understand something before it disappears :-)
//                                            if (doUpdateLiveResults && LiveResultsWindow.hasDefault()) {
//                                                LiveResultsWindow.getDefault().refreshLiveResults();
//                                            }

                                            doUpdateLiveResults = !doUpdateLiveResults;
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
