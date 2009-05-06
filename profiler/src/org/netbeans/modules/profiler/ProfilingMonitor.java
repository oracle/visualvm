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

package org.netbeans.modules.profiler;

import org.netbeans.api.project.Project;
import org.netbeans.lib.profiler.ProfilerClient;
import org.netbeans.lib.profiler.ProfilerEngineSettings;
import org.netbeans.lib.profiler.TargetAppRunner;
import org.netbeans.lib.profiler.client.MonitoredData;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.results.monitor.VMTelemetryDataManager;
import org.netbeans.lib.profiler.results.threads.ThreadsDataManager;
import org.netbeans.modules.profiler.ppoints.ProfilingPointsManager;
import javax.swing.*;


/**
 * This class provides thread for periodically processing monitoring data from profiled application (feeding them to
 * telemetry and threads data managers) as well as I/O redirection.
 *
 * @author Tomas Hurka
 * @author Ian Formanek
 */
public final class ProfilingMonitor {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    static final class UpdateThread extends Thread {
        //~ Static fields/initializers -------------------------------------------------------------------------------------------

        private static final int UPDATE_INTERVAL = 1200;

        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private TargetAppRunner runner;
        private ThreadsDataManager threadsDataManager;
        private VMTelemetryDataManager vmTelemetryManager;
        private boolean doUpdateLiveResults;
        private boolean keepRunning = true;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        UpdateThread() {
            super("Profiler Monitor"); // NOI18N
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void monitor(final TargetAppRunner runner) {
            this.runner = runner;
            this.threadsDataManager = Profiler.getDefault().getThreadsManager();
            this.vmTelemetryManager = Profiler.getDefault().getVMTelemetryManager();

            if (runner != null) {
                this.vmTelemetryManager.maxHeapSize = runner.getProfilingSessionStatus().maxHeapSize;
            }
        }

        public void run() {
            Project project = NetBeansProfiler.getDefaultNB().getProfiledProject();
            final int ppsize = ProfilingPointsManager.getDefault().getProfilingPoints(
                               project, ProfilerIDESettings.getInstance().
                               getIncludeProfilingPointsDependencies(), false).size(); // PPs are not modifiable during runtime!

            while (keepRunning) { // Main loop

                try {
                    if (runner != null) {
                        ProfilerControlPanel2.getDefault().updateStatus(); // TODO: move elsewhere

                        final MonitoredData md = runner.getProfilerClient().getMonitoredData();

                        if (md != null) {
                            SwingUtilities.invokeAndWait(new Runnable() {
                                    public void run() {
                                        try {
                                            threadsDataManager.processData(md);
                                            vmTelemetryManager.processData(md);

                                            // ---------------------------------------------------------
                                            // Temporary workaround to refresh profiling points when LiveResultsWindow is not refreshing
                                            // TODO: move this code to a separate class performing the update if necessary
                                            if (NetBeansProfiler.getDefaultNB().processesProfilingPoints() && (ppsize > 0)
                                                    && (!doUpdateLiveResults || !LiveResultsWindow.hasDefault())) {
                                                org.netbeans.modules.profiler.utils.IDEUtils.runInProfilerRequestProcessor(new Runnable() {
                                                        public void run() {
                                                            try {
                                                                ProfilerClient client = Profiler.getDefault().getTargetAppRunner()
                                                                                                .getProfilerClient();

                                                                if (client.getCurrentInstrType() != ProfilerEngineSettings.INSTR_CODE_REGION) {
                                                                    client.forceObtainedResultsDump(true);
                                                                }
                                                            } catch (Exception e /*ClientUtils.TargetAppOrVMTerminated targetAppOrVMTerminated*/) {
                                                            }
                                                        }
                                                    });

                                            }

                                            // ---------------------------------------------------------

                                            // Let results updating happen every other cycle (i.e. every ~2.5 sec) to allow the user to understand something before it disappears :-)
                                            if (doUpdateLiveResults && LiveResultsWindow.hasDefault()) {
                                                LiveResultsWindow.getDefault().refreshLiveResults();
                                            }

                                            doUpdateLiveResults = !doUpdateLiveResults;
                                        } catch (Exception e) {
                                            Profiler.getDefault().notifyException(Profiler.EXCEPTION, e);
                                        }
                                    }
                                });
                        } else {
                            SwingUtilities.invokeLater(new Runnable() {
                                    public void run() {
                                        ((NetBeansProfiler) Profiler.getDefault()).checkAndUpdateState();
                                    }
                                });
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

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private UpdateThread monitorThread;
    private boolean updateThreadStarted = false;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Initializes the Form
     */
    public ProfilingMonitor() {
        monitorThread = new UpdateThread();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void monitorVM(final TargetAppRunner runner) {
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
}
