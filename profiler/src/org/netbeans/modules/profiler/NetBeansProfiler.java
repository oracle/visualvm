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

import org.netbeans.modules.profiler.api.GestureSubmitter;
import org.netbeans.modules.profiler.api.ProfilerIDESettings;
import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.lib.profiler.ProfilerClient;
import org.netbeans.lib.profiler.ProfilerEngineSettings;
import org.netbeans.lib.profiler.ProfilerLogger;
import org.netbeans.lib.profiler.ProfilingEventListener;
import org.netbeans.lib.profiler.TargetAppRunner;
import org.netbeans.lib.profiler.classfile.ClassRepository;
import org.netbeans.lib.profiler.client.AppStatusHandler;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.common.*;
import org.netbeans.lib.profiler.common.filters.DefinedFilterSets;
import org.netbeans.lib.profiler.common.filters.GlobalFilters;
import org.netbeans.lib.profiler.global.CalibrationDataFileIO;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.global.InstrumentationFilter;
import org.netbeans.lib.profiler.global.Platform;
import org.netbeans.lib.profiler.instrumentation.BadLocationException;
import org.netbeans.lib.profiler.instrumentation.InstrumentationException;
import org.netbeans.lib.profiler.results.CCTProvider;
import org.netbeans.lib.profiler.results.ProfilingResultsDispatcher;
import org.netbeans.lib.profiler.results.cpu.CPUCCTProvider;
import org.netbeans.lib.profiler.results.cpu.CPUProfilingResultListener;
import org.netbeans.lib.profiler.results.cpu.cct.CCTResultsFilter;
import org.netbeans.lib.profiler.results.cpu.marking.MarkingEngine;
import org.netbeans.lib.profiler.results.memory.MemoryCCTProvider;
import org.netbeans.lib.profiler.results.memory.MemoryProfilingResultsListener;
import org.netbeans.lib.profiler.results.monitor.VMTelemetryDataManager;
import org.netbeans.lib.profiler.results.threads.ThreadsDataManager;
import org.netbeans.lib.profiler.wireprotocol.Command;
import org.netbeans.lib.profiler.wireprotocol.Response;
import org.netbeans.lib.profiler.wireprotocol.WireIO;
import org.openide.awt.StatusDisplayer;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.windows.WindowManager;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import org.netbeans.lib.profiler.client.ProfilingPointsProcessor;
import org.netbeans.lib.profiler.results.cpu.FlatProfileBuilder;
import org.netbeans.lib.profiler.results.cpu.cct.TimeCollector;
import org.netbeans.lib.profiler.results.locks.LockProfilingResultListener;
import org.netbeans.lib.profiler.ui.SwingWorker;
import org.netbeans.lib.profiler.ui.monitor.VMTelemetryModels;
import org.netbeans.modules.profiler.api.GlobalStorage;
import org.netbeans.modules.profiler.api.JavaPlatform;
import org.netbeans.modules.profiler.api.ProfilerDialogs;
import org.netbeans.modules.profiler.api.ProgressDisplayer;
import org.netbeans.modules.profiler.spi.SessionListener;
import org.netbeans.modules.profiler.ui.ProfilerProgressDisplayer;
import org.netbeans.modules.profiler.utilities.ProfilerUtils;
import org.openide.awt.Mnemonics;


/**
 * The main class representing profiler integrated in the IDE
 * <p/>
 * List of properties that can be used to influence the profiler behavior:
 * \"org.netbeans.lib.profiler.wireprotocol.WireIO\" - set to true to enable wire i/o debugging on profiler side
 * \"org.netbeans.lib.profiler.wireprotocol.WireIO.agent\" - set to true to enable wire i/o debugging on profiled app side
 *
 * @author Tomas Hurka
 * @author Ian Formanek
 * @author Misha Dmitriev
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "NetBeansProfiler_ProgressDialogCaption=Progress...",
    "NetBeansProfiler_EntireApplicationProfilingWarning=In the Entire Application profiling mode,\nprofiling data collection will not start\nuntil your application starts a new Thread,\nand will be done only for new Threads.\n\nConsider switching to \"Part of Application\" mode\nif you want to profile already running threads.",
    "NetBeansProfiler_DirectoryDoesNotExistMessage=The directory does not exist.",
    "NetBeansProfiler_DirectoryIsWriteProtectedMessage=The directory is write-protected.\nPlease make it writeable.",
    "NetBeansProfiler_ErrorLoadingProfilingSettingsMessage=Error encountered while loading profiling settings: {0}",
    "NetBeansProfiler_ErrorSavingProfilingSettingsMessage=Error encountered while saving global filters: {0}",
    "NetBeansProfiler_ErrorSavingFilterSetsMessage=Error encountered while saving defined filter sets: {0}",
    "NetBeansProfiler_ErrorSavingGlobalSettingsMessage=Error encountered while saving global settings: {0}",
    "NetBeansProfiler_ErrorSavingAttachSettingsMessage=Error encountered while saving attach settings: {0}",
    "NetBeansProfiler_CannotFindLibsMsg=Cannot find profiler libs directory",
    "NetBeansProfiler_EngineInitFailedMsg=Failed to initialize the Profiler engine: {0}",
    "NetBeansProfiler_InitialCalibrationMsg=Profiler will now perform an initial calibration of your machine and target JVM.\n\nThis calibration needs to be performed the first time you run the profiler to\nensure that timing results are accurate when profiling your application. To\nensure the calibration data is accurate, please make sure that other\napplications are not placing a noticeable load on your machine at this time.\n\nYou can run the calibration again by choosing\n\"Profile | Advanced Commands | Run Profiler Calibration\"\n\nWarning: If your computer uses dynamic CPU frequency switching, please\ndisable it and do not use it when  profiling.",
    "NetBeansProfiler_MustCalibrateFirstMsg=Profiling will STOP now because the calibration data is missing or is corrupt.\n\nIf this is the first time you are using the profiler or target JVM on this machine,\nyou first need to run the calibration for your target JVM.\nThe obtained calibration data will be saved and re-used\non subsequent runs, so you will not see this message again.\n\nTo perform calibration, choose\n\"Profile | Advanced Commands | Run Profiler Calibration\".\n\n",
    "NetBeansProfiler_MustCalibrateFirstShortMsg=<html><b>Calibration data missing.</b><br><br>Profiling cannot be started on this JDK. Please perform<br>profiler calibration first and start the profiling session again.</html>",
    "NetBeansProfiler_TerminateVMOnExitMsg=<b>The profiled application has finished execution.</b>\n Press OK to terminate the VM.",
    "NetBeansProfiler_TakeSnapshotOnExitMsg=<b>The profiled application has finished execution.</b>\nDo you want to take a snapshot of the collected results?",
    "NetBeansProfiler_TakeSnapshotOnExitDialogTitle=Application Finished",
    "NetBeansProfiler_TargetAppNotRespondingMsg=The profiled application does not respond.\n Do you want to close the connection and stop profiling?",
    "NetBeansProfiler_TargetAppNotRespondingDialogTitle=Question",
    "NetBeansProfiler_ModifyingInstrumentationMsg=Modifying instrumentation...",
    "NetBeansProfiler_StartingSession=Starting profiling session...",
    "NetBeansProfiler_CancelBtn=&Cancel",
    "NetBeansProfiler_MemorySamplingJava5=<html><b>Memory sampling is not supported for Java 5.</b><br><br>Please run the profiled application using Java 6+ or use memory instrumentation.</html>"
})
public abstract class NetBeansProfiler extends Profiler {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------
    
    public static final class ProgressPanel implements AppStatusHandler.AsyncDialog {
        
        private static final int MINIMUM_WIDTH = 350;
        
        private volatile boolean opened;
        private volatile boolean closed;
        private JDialog dialog;
        
        private final String message;
        private final boolean showProgress;
        private final Runnable cancelHandler;
        
        
        private ProgressPanel(String message, boolean showProgress, Runnable cancelHandler) {
            this.message = message;
            this.showProgress = showProgress;
            this.cancelHandler = cancelHandler;
        }

        @Override
        public void close() {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    if (!opened) closed = true;
                    else closeImpl();
                }
            });
        }

        @Override
        public void display() {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    initUI();
//                    RequestProcessor.getDefault().post(new Runnable() {
//                        public void run() {
                            if (!closed) dialog.setVisible(true);
//                        }
//                    });
                }
            });
        }
        
        private void initUI() {
            Frame mainWindow = WindowManager.getDefault().getMainWindow();
            dialog = new JDialog(mainWindow, Bundle.NetBeansProfiler_ProgressDialogCaption(), true);
            
            JPanel panel = new JPanel(new BorderLayout(5, 5));
            panel.setBorder(new EmptyBorder(15, 15, 15, 10));
            panel.add(new JLabel(message), BorderLayout.NORTH);

            if (showProgress) {
                JProgressBar progress = new JProgressBar();
                progress.setIndeterminate(true);
                panel.add(progress, BorderLayout.SOUTH);
            }
            
            if (cancelHandler != null) {
                JButton cancelButton = new JButton() {
                    protected void fireActionPerformed(ActionEvent e) {
                        close();
                        cancelHandler.run();
                    }
                };
                Mnemonics.setLocalizedText(cancelButton, Bundle.NetBeansProfiler_CancelBtn());
                JPanel buttonPanel = new JPanel(new BorderLayout(0, 0));
                buttonPanel.setBorder(new EmptyBorder(5, 15, 10, 10));
                buttonPanel.add(cancelButton, BorderLayout.EAST);
                
                dialog.add(panel, BorderLayout.NORTH);
                dialog.add(buttonPanel, BorderLayout.SOUTH);
            } else {
                dialog.add(panel, BorderLayout.NORTH);
            }
            
            Dimension ps = panel.getPreferredSize();
            panel.setPreferredSize(new Dimension(Math.max(ps.width, MINIMUM_WIDTH), ps.height));
            dialog.pack();
            dialog.setLocationRelativeTo(mainWindow);
            
            dialog.addHierarchyListener(new HierarchyListener() {
                public void hierarchyChanged(HierarchyEvent e) {
                    if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && dialog.isShowing()) {
                        dialog.removeHierarchyListener(this);
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                opened = true;
                                if (closed) closeImpl();
                            }
                        });
                    }
                }
            });
            dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        }
        
        private void closeImpl() {
            dialog.setVisible(false);
            dialog.dispose();
        }
        
    }
    
    // -- NetBeansProfiler-only callback classes ---------------------------------------------------------------------------
    private final class IDEAppStatusHandler implements AppStatusHandler {
        //~ Methods --------------------------------------------------------------------------------------------------------------
        public AppStatusHandler.AsyncDialog getAsyncDialogInstance(String message, boolean showProgress, Runnable cancelHandler) {
            return new ProgressPanel(message, showProgress, cancelHandler);
        }

        public boolean confirmWaitForConnectionReply() {
            // FIXXX: should display a NotifyDescriptor.WARNING_MESSAGE confirmation!
            return (!ProfilerDialogs.displayConfirmation(
                    Bundle.NetBeansProfiler_TargetAppNotRespondingMsg(),
                    Bundle.NetBeansProfiler_TargetAppNotRespondingDialogTitle()));
        }

        // The following methods should display messages asynchronously, i.e. they shouldn't block the current
        // thread waiting for the user pressing OK.
        public void displayError(final String msg) {
            LOGGER.log(Level.WARNING, "IDEAppStatusHandler - error: {)}", msg); //NOI18N
            ProfilerDialogs.displayError(msg);
        }

        // These method SHOULD wait for the user to press ok, since they may be used in a sequence of displayed
        // panels, and the next one shouldn't be displayed before the previous one is read and understood.
        public void displayErrorAndWaitForConfirm(final String msg) {
            LOGGER.log(Level.WARNING, "IDEAppStatusHandler - errorAndWaitForConfirm: {0}", msg); //NOI18N
            ProfilerDialogs.displayError(msg);
        }

        public void displayErrorWithDetailsAndWaitForConfirm(String shortMsg, String detailsMsg) {
            ProfilerDialogs.displayError(shortMsg, null, detailsMsg);
        }

        public void displayNotification(final String msg) {
            StatusDisplayer.getDefault().setStatusText(msg);
        }

        public void displayNotificationAndWaitForConfirm(final String msg) {
            ProfilerDialogs.displayInfo(msg);
        }

        public void displayNotificationWithDetailsAndWaitForConfirm(String shortMsg, String detailsMsg) {
            ProfilerDialogs.displayInfo(shortMsg, null, detailsMsg);
        }

        public void displayWarning(final String msg) {
            LOGGER.log(Level.WARNING, "IDEAppStatusHandler - warning: {0}" ,msg); //NOI18N
            ProfilerDialogs.displayWarning(msg);
        }

        public void displayWarningAndWaitForConfirm(final String msg) {
            LOGGER.log(Level.WARNING, "IDEAppStatusHandler - warningAndWaitForConfirm: {0}", msg); //NOI18N
            ProfilerDialogs.displayWarning(msg);
        }

        public void handleShutdown() {
            //      IDEUtils.runInEventDispatchThreadAndWait( // According to Issue 74914 this cannot run in AWT-EventQueue
            //          new Runnable() {
            //            public void run() {

            // Asynchronously update live results if autorefresh is on
            if (LiveResultsWindow.hasDefault()) {
                LiveResultsWindow.getDefault().handleShutdown();
            }

            if ((getTargetAppRunner().getProfilerClient().getCurrentInstrType() == CommonConstants.INSTR_NONE)
                    || !ResultsManager.getDefault().resultsAvailable()) {
                ProfilerDialogs.displayInfoDNSA(Bundle.NetBeansProfiler_TerminateVMOnExitMsg(), null, null, "NetBeansProfiler.handleShutdown.noResults", false); //NOI18N
            } else if (ProfilerDialogs.displayConfirmationDNSA(Bundle.NetBeansProfiler_TakeSnapshotOnExitMsg(), Bundle.NetBeansProfiler_TakeSnapshotOnExitDialogTitle(), null, "NetBeansProfiler.handleShutdown", false)) { //NOI18N
                ResultsManager.getDefault().takeSnapshot();
            }

            //            }
            //          }
            //      );
        }

        public void pauseLiveUpdates() {
            LiveResultsWindow.setPaused(true);
        }

        public void resultsAvailable() {
            ProfilingSettings ps = getLastProfilingSettings();
            if (ps != null && ps.getProfilingType() == ProfilingSettings.PROFILE_CPU_SAMPLING) {
                return;
            }
            ResultsManager.getDefault().resultsBecameAvailable();
        }

        public void resumeLiveUpdates() {
            LiveResultsWindow.setPaused(false);
        }

        public void takeSnapshot() {
            ProfilerUtils.runInProfilerRequestProcessor(new Runnable() {
                    public void run() {
                        ResultsManager.getDefault().takeSnapshot();
                    }
                });
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final Logger LOGGER = Logger.getLogger(NetBeansProfiler.class.getName());

    private static final String GLOBAL_FILTERS_FILENAME = "filters"; //NOI18N
    private static final String DEFINED_FILTERSETS_FILENAME = "filtersets"; //NOI18N
    private static final String DEFAULT_FILE_SUFFIX = "-default"; //NOI18N
    private static boolean initialized = false;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    //--- Modifying instrumentation progress -------------------------------------
    boolean shouldDisplayDialog = true;

    // TODO [release] set to obtain from property
    //  static boolean DEBUG = true; // System.getProperty("org.netbeans.modules.profiler.NetBeansProfiler") != null;
    private final ProfilerIDESettings ideSettings = ProfilerIDESettings.getInstance();
    
    private ProfilingMonitor monitor = null;
    private TargetAppRunner targetAppRunner;
    private DefinedFilterSets definedFilterSets;
    private FileObject profiledSingleFile;
    final private ProfilerEngineSettings sharedSettings;
    
    private GlobalFilters globalFilters;
    private final Object setupLock = new Object();
    private ProfilingSettings lastProfilingSettings;
    private Lookup.Provider profiledProject = null;
    private SessionSettings lastSessionSettings;
    private StringBuilder logMsgs = new StringBuilder();
    private ThreadsDataManager threadsManager;
    private VMTelemetryDataManager vmTelemetryManager;
    private VMTelemetryModels vmTelemetryModels;
    private boolean calibrating = false;

    // ---------------------------------------------------------------------------
    // Temporary workaround to refresh profiling points when LiveResultsWindow is not refreshing
    // TODO: implement better approach for refreshing profiling points and remove this code
//    private boolean processesProfilingPoints;
    private boolean threadsMonitoringEnabled = false;
    private boolean lockContentionMonitoringEnabled = false;
    private boolean waitDialogOpen = false;
    private int lastMode = MODE_PROFILE;
    private int profilingMode = MODE_PROFILE;
    private int profilingState = PROFILING_INACTIVE;
    
    //~ Constructors -------------------------------------------------------------------------------------------------------------
    
    public NetBeansProfiler() {
        boolean initFailed = false;

        sharedSettings = new ProfilerEngineSettings();

        try {
            String libsDir = getLibsDir();

            if (libsDir == null) {
                throw new IOException(Bundle.NetBeansProfiler_CannotFindLibsMsg());
            }

            sharedSettings.initialize(libsDir);
            sharedSettings.setSeparateConsole(System.getProperty("org.netbeans.profiler.separateConsole") != null //NOI18N
                                              ); // change to true if something misbehaves and the TA VM does not start
            sharedSettings.setTargetWindowRemains(System.getProperty("org.netbeans.profiler.targetWindowRemains") != null //NOI18N
                                                  ); // use for testing when something misbehaves
        } catch (RuntimeException e) {
            ProfilerLogger.log(e);
            ProfilerDialogs.displayError(e.getMessage());
            initFailed = true;
        } catch (IOException e) {
            // #216809 - likely an unsupported system, just silently log the problem
            LOGGER.log(Level.WARNING, Bundle.NetBeansProfiler_EngineInitFailedMsg(e.getLocalizedMessage()));
            initFailed = true;
        }

        
        if (!initFailed) {
            initialized = true;
            new ServerStateMonitor(this);
        }
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------
    public static NetBeansProfiler getDefaultNB() {
        return (NetBeansProfiler) getDefault();
    }

    // ---------------------------------------------------------------------------
    public static boolean isInitialized() {
        // make sure that the profiler is initialized
        getDefaultNB();
        return initialized;
    }

    public int getAgentState(String host, int port, int agentId) {
        if (profilingState /*!= PROFILING_INACTIVE*/ == PROFILING_RUNNING) {
            // profiling currently in progress, check port and id
            if (port == getTargetAppRunner().getProfilerEngineSettings().getPortNo()) {
                if (getTargetAppRunner().getProfilerClient().getCurrentAgentId() == agentId) {
                    return CommonConstants.AGENT_STATE_CONNECTED;
                } else {
                    return CommonConstants.AGENT_STATE_DIFFERENT_ID;
                }
            }
        }

        Properties agentProps = getAgentProperties(port);

        if (agentProps == null) {
            return CommonConstants.AGENT_STATE_NOT_RUNNING;
        } else {
            try {
                int id = Integer.parseInt(agentProps.getProperty("agent.id")); //NOI18N

                if (id == agentId) {
                    String dynamic = agentProps.getProperty("dynamic"); //NOI18N

                    if ((dynamic == null) || "false".equals(dynamic)) { //NOI18N

                        return CommonConstants.AGENT_STATE_READY_DIRECT;
                    } else {
                        return CommonConstants.AGENT_STATE_READY_DYNAMIC;
                    }
                } else {
                    return CommonConstants.AGENT_STATE_DIFFERENT_ID;
                }
            } catch (NumberFormatException e) {
                return CommonConstants.AGENT_STATE_NOT_RUNNING;
            }
        }
    }

    public SessionSettings getCurrentSessionSettings() {
        return lastSessionSettings;
    }

    public synchronized DefinedFilterSets getDefinedFilterSets() {
        if (definedFilterSets == null) {
            loadGlobalFilters();
        }

        return definedFilterSets;
    }

    public synchronized GlobalFilters getGlobalFilters() {
        if (globalFilters == null) {
            loadGlobalFilters();
        }

        return globalFilters;
    }

    public GlobalProfilingSettings getGlobalProfilingSettings() {
        return ideSettings;
    }

    public ProfilingSettings getLastProfilingSettings() {
        return lastProfilingSettings;
    }

    public abstract String getLibsDir();

    public int getPlatformArchitecture(String platformName) {
        JavaPlatform platform = JavaPlatform.getJavaPlatformById(platformName);

        return platform.getPlatformArchitecture();
    }

    public String getPlatformJDKVersion(String platformName) {
        JavaPlatform platform = JavaPlatform.getJavaPlatformById(platformName);

        return platform.getPlatformJDKVersion();
    }

    public String getPlatformJavaFile(String platformName) {
        JavaPlatform platform = JavaPlatform.getJavaPlatformById(platformName);

        return platform.getPlatformJavaFile();
    }

    public int getProfilingMode() {
        return profilingMode;
    }

    public int getProfilingState() {
        return profilingState;
    }

    @Override
    public int getServerState() {
        if (monitor != null) {
            return monitor.getServerState();
        }
        return CommonConstants.SERVER_RUNNING;
    }
    
    @Override
    public int getServerProgress() {
        if (monitor != null) {
            return monitor.getServerProgress();
        }
        return CommonConstants.SERVER_PROGRESS_INDETERMINATE;
    }

    public synchronized TargetAppRunner getTargetAppRunner() {
        if (initialized) {
            if (targetAppRunner == null) {
                // Initialize shared TargetAppRunner instance
                targetAppRunner = new TargetAppRunner(sharedSettings, new IDEAppStatusHandler(), getProfilingPointsManager());
                targetAppRunner.addProfilingEventListener(new ProfilingEventListener() {
                    @Override
                    public void targetAppStarted() {
                        if (calibrating) {
                            return;
                        }

                        changeStateTo(PROFILING_RUNNING);
                    }

                    @Override
                    public void targetAppStopped() {
                        if (calibrating) {
                            return;
                        }

                        changeStateTo(PROFILING_STOPPED);
                    }

                    @Override
                    public void targetAppSuspended() {
                        if (calibrating) {
                            return;
                        }

                        changeStateTo(PROFILING_PAUSED);
                    }

                    @Override
                    public void targetAppResumed() {
                        if (calibrating) {
                            return;
                        }

                        changeStateTo(PROFILING_RUNNING);
                    }

                    @Override
                    public void attachedToTarget() {
                        if (calibrating) {
                            return;
                        }

                        changeStateTo(PROFILING_RUNNING);
                    }

                    @Override
                    public void detachedFromTarget() {
                        if (calibrating) {
                            return;
                        }

                        getMonitor().stopDisplayingVM();
                        changeStateTo(PROFILING_INACTIVE);
                    }

                    @Override
                    public void targetVMTerminated() {
                        if (calibrating) {
                            return;
                        }

                        getMonitor().stopDisplayingVM();
                        changeStateTo(PROFILING_INACTIVE);
                    }
                });
            }
            return targetAppRunner;
        }
        return null;
    }

    public synchronized ThreadsDataManager getThreadsManager() {
        if (threadsManager == null) {
            threadsManager = new ThreadsDataManager();
        }

        return threadsManager;
    }

    public void setThreadsMonitoringEnabled(final boolean enabled) {
        getThreadsManager().setThreadsMonitoringEnabled(enabled);

        if (threadsMonitoringEnabled == enabled) {
            return;
        }

        threadsMonitoringEnabled = enabled;
        fireThreadsMonitoringChange();
    }

    public boolean getThreadsMonitoringEnabled() {
        return threadsMonitoringEnabled;
    }
    
    public void setLockContentionMonitoringEnabled(final boolean enabled) {
        if (lockContentionMonitoringEnabled == enabled) {
            return;
        }

        lockContentionMonitoringEnabled = enabled;
        fireLockContentionMonitoringChange();
    }

    public boolean getLockContentionMonitoringEnabled() {
        return lockContentionMonitoringEnabled;
    }

    public synchronized VMTelemetryModels getVMTelemetryModels() {
        if (vmTelemetryModels == null) {
            vmTelemetryModels = new VMTelemetryModels(getVMTelemetryManager());
        }

        return vmTelemetryModels;
    }

    public synchronized VMTelemetryDataManager getVMTelemetryManager() {
        if (vmTelemetryManager == null) {
            vmTelemetryManager = new VMTelemetryDataManager();
        }

        return vmTelemetryManager;
    }

    /**
     * Attaches to a running application using provided settings
     *
     * @param profilingSettings Settings to use for profiling
     * @param attachSettings    AttachSettings to use
     * @return true if successfully attached, false otherwise
     */
    public boolean attachToApp(final ProfilingSettings profilingSettings, final AttachSettings attachSettings) {
        profilingMode = MODE_ATTACH;

        ProgressHandle ph = ProgressHandleFactory.createHandle(Bundle.NetBeansProfiler_StartingSession());
        ph.setInitialDelay(500);
        
        ph.start();
        
        try {
            if (getProfilingState() != PROFILING_INACTIVE) {
                if (lastMode == MODE_ATTACH) {
                    detachFromApp(); // if attached, detach
                } else if (getTargetAppRunner().targetJVMIsAlive()) {
                    getTargetAppRunner().terminateTargetJVM(); // otherwise kill current app if running
                }
            }

            // remember profiling settings
            lastProfilingSettings = profilingSettings;
            lastSessionSettings = null;
            lastMode = MODE_ATTACH;
            
            final ProfilerEngineSettings sSettings = getTargetAppRunner().getProfilerEngineSettings();
            profilingSettings.applySettings(sSettings); // can override the session settings
            attachSettings.applySettings(sSettings);

            //getThreadsManager().setSupportsSleepingStateMonitoring(
            // Platform.supportsThreadSleepingStateMonitoring(sharedSettings.getTargetJDKVersionString()));
            logActionConfig("attachToApp", profilingSettings, null, attachSettings, sSettings.getInstrumentationFilter()); // NOI18N
            
            GestureSubmitter.logAttach(getProfiledProject(), attachSettings);
            GestureSubmitter.logConfig(profilingSettings, sSettings.getInstrumentationFilter());
            
            changeStateTo(PROFILING_STARTED);
            
            cleanupBeforeProfiling(sSettings);
            
            setThreadsMonitoringEnabled(profilingSettings.getThreadsMonitoringEnabled());
            setLockContentionMonitoringEnabled(profilingSettings.getLockContentionMonitoringEnabled());
            
            if (shouldOpenWindowsOnProfilingStart()) {
                CommonUtils.runInEventDispatchThread(new Runnable() {
                    public void run() {
                        openWindowsOnProfilingStart();
                    }
                });
            }
            
            if (attachSettings.isDirect()) { // Previously known as "attach on startup"
                // The VM is already started with all necessary options and waiting for us to connect.
                // Remote profiling case fits here too - it's distinguished in ProfilerClient using attachSettings.isRemote()
                // perform the selected instrumentation - it will really start right after the target app starts

                boolean success = false;
                
                if (prepareInstrumentation(profilingSettings)) {
                    success = getTargetAppRunner().initiateSession(1, false) && getTargetAppRunner().attachToTargetVMOnStartup();
                }
                
                if (!success) {
                    changeStateTo(PROFILING_INACTIVE);
                    // change state back to inactive and fire, return false

                    return false;
                }
            } else if (attachSettings.isDynamic16()) {
                String jar = getLibsDir() + "/jfluid-server-15.jar"; // NOI18N
                String pid = String.valueOf(attachSettings.getPid());
                String options = String.valueOf(attachSettings.getPort());
                boolean success = false;
                
                try {
                    loadAgentIntoTargetJVM(jar, options, pid);
                    
                    if (prepareInstrumentation(profilingSettings)) {
                        success = getTargetAppRunner().initiateSession(2, false) && getTargetAppRunner().attachToTargetVM();
                    }
                } catch (Exception ex) {
                    ProfilerDialogs.displayError(ex.getMessage());
                    ProfilerLogger.log(ex);
                }
                
                if (!success) {
                    changeStateTo(PROFILING_INACTIVE);
                    // change state back to inactive and fire, return false

                    return false;
                }
            } else {
                throw new IllegalArgumentException("Invalid settings " + attachSettings); // NOI18N
            }
            
            return connectToApp();
        } finally {
            ph.finish();
        }
    }
    
    private static void loadAgentIntoTargetJVM(final String jar, final String options, final String pid)
                                            throws AttachNotSupportedException, IOException, AgentLoadException, AgentInitializationException  {
        VirtualMachine virtualMachine = VirtualMachine.attach(pid);
        virtualMachine.loadAgent(jar,options);
    }

    // -- NetBeansProfiler-only public methods -----------------------------------------------------------------------------
    public void checkAndUpdateState() {
        // TODO: check & refactor to remove this
        final boolean targetVMAlive = getTargetAppRunner().targetJVMIsAlive();

        if (!targetVMAlive) {
            changeStateTo(PROFILING_INACTIVE);

            return;
        }

        final boolean running = getTargetAppRunner().targetAppIsRunning();

        if (!running) {
            changeStateTo(PROFILING_STOPPED);

            return;
        }

        final boolean suspended = getTargetAppRunner().targetAppSuspended();

        if (suspended) {
            changeStateTo(PROFILING_PAUSED);
        }
    }

    // used before starting a server for profiling, kills any agent which would cause collision on port and removes agent file
    // does nothing if the agent is already profiling (port is used, covers both profiling from current or other IDE)
    public boolean cleanForProfilingOnPort(int port) {
        // profiling session communicating over the port currently in progress, nothing to do (will cause collision)
        if (isProfilingRunningOnPort(port)) {
            ProfilerLogger.severe(">>> Profiling session already running on port " + port
                                  + ", will cause collision when starting another agent on the same port."); // NOI18N

            return false;
        }

        // there is an agent alive currently using the port, most likely profiling from within another IDE (will cause collision)
        if (!shutdownAgent("localhost", port)) { // NOI18N
            ProfilerLogger.severe(">>> Profiler agent already profiling on port " + port
                                  + " (communicating with another IDE?), will cause collision when starting another agent on the same port."); // NOI18N

            return false;
        }

        File agentFile = getInfoFile(port);

        // agent file exists, agent is still shutting down or in undefined state (hanging?)
        if (agentFile.exists()) {
            // returns true if agent file successfuly deleted, false otherwise (will cause server startup failure because initial STATE_INACTIVE)
            boolean fileDeleted = waitForDeleteAgentFile(agentFile);

            if (!fileDeleted) {
                ProfilerLogger.severe(">>> Profiler agent identification file cannot be deleted for port " + port
                                      + ", will cause failure starting a server for profiling on the same port."); // NOI18N
            }

            return fileDeleted;
        }

        // agent file doesn't exist, there should be no collision starting new profiling session on port
        return true;
    }

    /**
     * Connects to an application started using the specified sessionSettings, and will start its profiling
     * with the provided profilingSettings.
     *
     * @param profilingSettings Settings to use for profiling
     * @param sessionSettings   Session settings for profiling
     * @return true if connected successfully, false otherwise
     */
    @Override
    public boolean connectToStartedApp(final ProfilingSettings profilingSettings, final SessionSettings sessionSettings) {
        return connectToStartedApp(profilingSettings, sessionSettings, new AtomicBoolean());
    }
    
    /**
     * Connects to an application started using the specified sessionSettings, and will start its profiling
     * with the provided profilingSettings.
     *
     * @param profilingSettings Settings to use for profiling
     * @param sessionSettings   Session settings for profiling
     * @param cancel shared cancel flag
     * @return true if connected successfully, false otherwise
     */
    public boolean connectToStartedApp(final ProfilingSettings profilingSettings, final SessionSettings sessionSettings, final AtomicBoolean cancel) {
        profilingMode = MODE_PROFILE;

        lastProfilingSettings = profilingSettings;
        lastSessionSettings = sessionSettings;
        lastMode = MODE_PROFILE;

        ProgressHandle ph = ProgressHandleFactory.createHandle(Bundle.NetBeansProfiler_StartingSession());
        try {
            ph.setInitialDelay(500);
            ph.start();
            
            if (getTargetAppRunner().targetJVMIsAlive()) {
                getTargetAppRunner().terminateTargetJVM();
            }
            
            final ProfilerEngineSettings sSettings = getTargetAppRunner().getProfilerEngineSettings();
            
            sessionSettings.applySettings(sSettings);
            profilingSettings.applySettings(sSettings); // can override the session settings
            sSettings.setRemoteHost(""); // NOI18N // clear remote profiling host

            //getThreadsManager().setSupportsSleepingStateMonitoring(
            // Platform.supportsThreadSleepingStateMonitoring(sharedSettings.getTargetJDKVersionString()));
            logActionConfig("connectToStartedApp", profilingSettings, sessionSettings, null, sSettings.getInstrumentationFilter()); // NOI18N
            
            GestureSubmitter.logProfileApp(getProfiledProject(), sessionSettings);
            GestureSubmitter.logConfig(profilingSettings, sSettings.getInstrumentationFilter());
            
            if (prepareProfilingSession(profilingSettings, sessionSettings)) {
                RequestProcessor.getDefault().post(new Runnable() {
                    
                    @Override
                    public void run() {
                        // should propagate the result of the following operation somehow; current workflow doesn't allow it
                        if (tryInitiateSession(cancel)) {
                            connectToApp();
                        }
                    }
                });
                
                return true;
            }
            
            return false;
        } finally {
            ph.finish();
        }
    }

    private boolean prepareProfilingSession(ProfilingSettings profilingSettings, SessionSettings sessionSettings) {
        changeStateTo(PROFILING_STARTED);

        cleanupBeforeProfiling(getTargetAppRunner().getProfilerEngineSettings());

        setThreadsMonitoringEnabled(profilingSettings.getThreadsMonitoringEnabled());
        setLockContentionMonitoringEnabled(profilingSettings.getLockContentionMonitoringEnabled());

        if (shouldOpenWindowsOnProfilingStart()) {
            CommonUtils.runInEventDispatchThread(new Runnable() {
                public void run() {
                    openWindowsOnProfilingStart();
                }
            });
        }
        if (!CalibrationDataFileIO.validateCalibrationInput(sessionSettings.getJavaVersionString(),
                                                                sessionSettings.getJavaExecutable())) {
            ProfilerDialogs.displayError(
                Bundle.NetBeansProfiler_MustCalibrateFirstMsg(), null, Bundle.NetBeansProfiler_MustCalibrateFirstShortMsg());
            changeStateTo(PROFILING_INACTIVE);

            return false; // failed, cannot proceed
        }

        // perform the selected instrumentation
        if (!prepareInstrumentation(profilingSettings)) {
            return false; // failed, cannot proceed
        }
        
        return true;
    }
    
    private boolean tryInitiateSession(AtomicBoolean cancel) {
        if (!targetAppRunner.initiateSession(0, false, cancel) || !targetAppRunner.connectToStartedVMAndStartTA()) {
            changeStateTo(PROFILING_INACTIVE);

            return false; // failed, cannot proceed
        }
        return true;
    }
    
    private boolean connectToApp() {
        if (getTargetAppRunner().targetAppIsRunning()) {
            getThreadsManager()
                .setSupportsSleepingStateMonitoring(Platform.supportsThreadSleepingStateMonitoring(getTargetAppRunner().getProfilerEngineSettings().getTargetJDKVersionString()));
            CommonUtils.runInEventDispatchThread(new Runnable() {
                    public void run() {
                        getMonitor().monitorVM(getTargetAppRunner());
                    }
                });

            return true;
        } else {
            // TODO: notify the user???
            changeStateTo(PROFILING_INACTIVE);

            return false; // failed, cannot proceed
        }
    }
    
    public void detachFromApp() {
        setTransitionState();

        getTargetAppRunner().prepareDetachFromTargetJVM();

        if (getTargetAppRunner().getProfilingSessionStatus().currentInstrType != CommonConstants.INSTR_NONE) {
            //      if (LiveResultsWindow.hasDefault()) LiveResultsWindow.getDefault().reset(); // see issue http://www.netbeans.org/issues/show_bug.cgi?id=68213
            try {
                getTargetAppRunner().getProfilerClient().removeAllInstrumentation(false); // remove only the server side instrumentation
            } catch (InstrumentationException e) {
                ProfilerDialogs.displayError(e.getMessage());
            }
        }

        getTargetAppRunner().detachFromTargetJVM();

        //    targetAppRunner.getProfilerClient().resetClientData();
        // TODO reset all profilingresultslisteners
        //    CPUCallGraphBuilder.resetCollectors();
        //    ResultsManager.getDefault().reset();
    }

    public void instrumentSelectedRoots(ClientUtils.SourceCodeSelection[] rootMethods)
                                 throws ClassNotFoundException, InstrumentationException, BadLocationException, IOException,
                                        ClassFormatError, ClientUtils.TargetAppOrVMTerminated {
        final ProfilerClient client = getTargetAppRunner().getProfilerClient();

        if (rootMethods.length == 0) {
            ClientUtils.SourceCodeSelection selection = new ClientUtils.SourceCodeSelection(1); // spawned threads recursively
            rootMethods = new ClientUtils.SourceCodeSelection[] { selection };
        }

        // Start the recursive code instrumentation
        client.initiateRecursiveCPUProfInstrumentation(rootMethods);
    }

    public void log(int severity, final String message) {
        switch (severity) {
            case Profiler.INFORMATIONAL:
                LOGGER.log(Level.INFO, message);

                break;
            case Profiler.WARNING:
                LOGGER.log(Level.WARNING, message);

                break;
            case Profiler.EXCEPTION:
            case Profiler.ERROR:
                LOGGER.log(Level.SEVERE, message);

                break;
            default:
                LOGGER.log(Level.FINEST, message);

                break;
        }
    }

    // ---------------------------------------------------------------------------
    public void modifyCurrentProfiling(final ProfilingSettings profilingSettings) {
        lastProfilingSettings = profilingSettings;

        if (!targetAppRunner.targetJVMIsAlive()) {
            return;
        }

        final ProfilerEngineSettings sharedSettings = getTargetAppRunner().getProfilerEngineSettings();
        profilingSettings.applySettings(sharedSettings);

        logActionConfig("modifyCurrentProfiling", profilingSettings, null, null, sharedSettings.getInstrumentationFilter()); //NOI18N
        
        GestureSubmitter.logConfig(profilingSettings, sharedSettings.getInstrumentationFilter());

        setThreadsMonitoringEnabled(profilingSettings.getThreadsMonitoringEnabled());
        setLockContentionMonitoringEnabled(profilingSettings.getLockContentionMonitoringEnabled());

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (LiveResultsWindow.hasDefault())
                    LiveResultsWindow.getDefault().handleCleanupBeforeProfiling();
            }
        });

        ProfilerUtils.runInProfilerRequestProcessor(new Runnable() {
                @Override
                public void run() {
                    changeStateTo(PROFILING_IN_TRANSITION);
                    getTargetAppRunner().getAppStatusHandler().pauseLiveUpdates();
                    ResultsManager.getDefault().reset();

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                    }

                    AppStatusHandler.AsyncDialog waitDialog = getTargetAppRunner().getAppStatusHandler()
                                                .getAsyncDialogInstance(Bundle.NetBeansProfiler_ModifyingInstrumentationMsg(), true, null);
                    waitDialog.display();

                    try {
                        prepareInstrumentation(profilingSettings);
                        changeStateTo(PROFILING_RUNNING);
                    } finally {
                        waitDialog.close();

                        getTargetAppRunner().getAppStatusHandler().resumeLiveUpdates();
                    }
                }
            });
    }

    public void notifyException(final int severity, final Exception e) {
        switch (severity) {
            case Profiler.INFORMATIONAL:
                LOGGER.log(Level.INFO, null, e);

                break;
            case Profiler.WARNING:
                LOGGER.log(Level.WARNING, null, e);

                break;
            default:
                LOGGER.log(Level.SEVERE, null, e);

                break;
        }
    }

    public void openJavaSource(String className, String methodName, String methodSig) {
//        openJavaSource(getProfiledProject(), className, methodName, methodSig);
    }
    
    public boolean processesProfilingPoints() {
        ProfilingPointsProcessor ppp = getProfilingPointsManager();
        if (ppp != null) {
            return ppp.getSupportedProfilingPoints().length > 0;
        }
        return false;
    }

    /**
     * Starts the TA described via sessionSettings, using profiling mode specified in profilingSettings.
     *
     * @param profilingSettings Settings to use for profiling
     * @param sessionSettings   Session settings for profiling
     * @return true if target app successfully started, false otherwise
     */
    public boolean profileClass(final ProfilingSettings profilingSettings, final SessionSettings sessionSettings) {
        //final long time = System.currentTimeMillis();
        profilingMode = MODE_PROFILE;

        lastProfilingSettings = profilingSettings;
        lastSessionSettings = sessionSettings;
        lastMode = MODE_PROFILE;

        if (getTargetAppRunner().targetJVMIsAlive()) {
            getTargetAppRunner().terminateTargetJVM();
        }

        final ProfilerEngineSettings sSettings = getTargetAppRunner().getProfilerEngineSettings();

        sessionSettings.applySettings(sSettings);
        profilingSettings.applySettings(sSettings); // can override the session settings
        sSettings.setRemoteHost(""); // NOI18N // clear remote profiling host

        //getThreadsManager().setSupportsSleepingStateMonitoring(
        // Platform.supportsThreadSleepingStateMonitoring(sharedSettings.getTargetJDKVersionString()));
        logActionConfig("profileClass", profilingSettings, sessionSettings, null, sSettings.getInstrumentationFilter()); //NOI18N

        GestureSubmitter.logProfileClass(getProfiledProject(), sessionSettings);
        GestureSubmitter.logConfig(profilingSettings, sSettings.getInstrumentationFilter());

        changeStateTo(PROFILING_STARTED);

        //    System.err.println("--------------------------------------------- 2: "+ (System.currentTimeMillis() - time));
        cleanupBeforeProfiling(sSettings);

        setThreadsMonitoringEnabled(profilingSettings.getThreadsMonitoringEnabled());
        setLockContentionMonitoringEnabled(profilingSettings.getLockContentionMonitoringEnabled());
        //    System.err.println("------------------------------------------ 3: "+ (System.currentTimeMillis() - time));
        if (shouldOpenWindowsOnProfilingStart()) {
            openWindowsOnProfilingStart();
        }

        //    System.err.println("------------------------------------- 4: "+ (System.currentTimeMillis() - time));
        final Window mainWindow = WindowManager.getDefault().getMainWindow();

        // This call reduces the speedup for class instrumentation on the 2nd and further runs that we could otherwise
        // have, but guarantees that if any classes have been recompiled in between runs, their most up-to-date copies will
        // be used.
        ProfilerUtils.runInProfilerRequestProcessor(new Runnable() {
                public void run() {
                    final Cursor cursor = mainWindow.getCursor();
                    mainWindow.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                    try {
                        if (!runCalibration(true, sessionSettings.getJavaExecutable(), sessionSettings.getJavaVersionString(),
                                                sessionSettings.getSystemArchitecture())) {
                            ProfilerDialogs.displayError(Bundle.ProfilerModule_CalibrationFailedMessage());
                            changeStateTo(PROFILING_INACTIVE);

                            return; // failed, cannot proceed
                        }

                        // System.err.println("-----------------------------------5: "+ (System.currentTimeMillis() - time));
                        // perform the selected instrumentation
                        boolean success = prepareInstrumentation(profilingSettings);

                        // and run the target application
                        //        System.err.println("---------------------------- 6: "+ (System.currentTimeMillis() - time));
                        success = success && getTargetAppRunner().startTargetVM() && getTargetAppRunner().initiateSession(0, false)
                                  && getTargetAppRunner().connectToStartedVMAndStartTA();

                        if (!success) {
                            changeStateTo(PROFILING_INACTIVE);

                            return;
                        }

                        // System.err.println("---------------------------- 7: "+ (System.currentTimeMillis() - time));
                        if (getTargetAppRunner().targetAppIsRunning()) {
                            getThreadsManager()
                                .setSupportsSleepingStateMonitoring(Platform.supportsThreadSleepingStateMonitoring(sSettings.getTargetJDKVersionString()));
                            CommonUtils.runInEventDispatchThread(new Runnable() {
                                    public void run() {
                                        // System.err.println("------------ 8: "+ (System.currentTimeMillis() - time));
                                        getMonitor().monitorVM(getTargetAppRunner());

                                        // System.err.println("------------------------ 9: "+ (System.currentTimeMillis() - time));
                                        // System.err.println("------------------------ 11: "+ (System.currentTimeMillis() - time));
                                    }
                                });
                        } else {
                            // TODO: notify the user???
                            changeStateTo(PROFILING_INACTIVE);
                        }
                    } finally {
                        mainWindow.setCursor(cursor);
                    }
                }
            });

        //    Syst---------------------------------------------------- Final: "+ (System.currentTimeMillis() - time));
        return true;
    }

    protected boolean shouldOpenWindowsOnProfilingStart() {
        return true;
    }

    // NOTE: used from com.sun.tools.visualvm.profiler.ProfilerSupport.calibrateJVM(),
    //       requires targetAppRunner to be configured correctly. Most likely you want to use
    //       runCalibration(boolean checkForSaved, String jvmExecutable, String jdkString, int architecture) !!!
    public boolean runConfiguredCalibration() {
        calibrating = true;
        boolean result = targetAppRunner.calibrateInstrumentationCode();
        calibrating = false;

        return result;
    }

    public boolean runCalibration(boolean checkForSaved, String jvmExecutable, String jdkString, int architecture) {
        calibrating = true;

        ProfilerEngineSettings pes = getTargetAppRunner().getProfilerEngineSettings();

        int savedPort = pes.getPortNo();
        InstrumentationFilter savedInstrFilter = pes.getInstrumentationFilter();
        String savedJVMExeFile = pes.getTargetJVMExeFile();
        String savedJDKVersionString = pes.getTargetJDKVersionString();
        int savedArch = pes.getSystemArchitecture();
        String savedCP = pes.getMainClassPath();

        if (jvmExecutable != null) {
            pes.setTargetJVMExeFile(jvmExecutable);
            pes.setTargetJDKVersionString(jdkString);
            pes.setSystemArchitecture(architecture);
        }

        pes.setPortNo(ideSettings.getCalibrationPortNo());
        pes.setInstrumentationFilter(new InstrumentationFilter());
        pes.setMainClassPath(""); //NOI18N

        boolean result = false;

        if (checkForSaved) {
            result = getTargetAppRunner().readSavedCalibrationData();

            if (!result) {
                ProfilerDialogs.displayInfo(Bundle.NetBeansProfiler_InitialCalibrationMsg());
                result = getTargetAppRunner().calibrateInstrumentationCode();
            }
            
            // NOTE: use -Dprofiler.disableFTSRecalibration=true to skip fast
            //       timestamp recalibration for each profiling session on old
            //       linux kernels not supporting Time Stamp Counter.
            if (!Boolean.getBoolean("profiler.disableFTSRecalibration")) { // NOI18N
                boolean shouldCalibrate = false;
                getTargetAppRunner().getProfilingSessionStatus().beginTrans(false);
                try {
                    // the calibration was executed without the usage of "-XX:+UseLinuxPosixThreadCPUClocks" flag
                    // ---> recalibrate <---
                    shouldCalibrate = Platform.isLinux() &&
                                      Platform.JDK_16_STRING.equals(pes.getTargetJDKVersionString()) &&
                                      getTargetAppRunner().getProfilingSessionStatus().methodEntryExitCallTime[1] > 20000; // 20us
                } finally {
                    getTargetAppRunner().getProfilingSessionStatus().endTrans();
                }
                if (shouldCalibrate) {
                    result = getTargetAppRunner().calibrateInstrumentationCode();
                }
            }
        } else {
            result = getTargetAppRunner().calibrateInstrumentationCode();
        }

        calibrating = false;

        // restore original values
        pes.setPortNo(savedPort);
        pes.setInstrumentationFilter(savedInstrFilter);
        pes.setTargetJDKVersionString(savedJDKVersionString);
        pes.setSystemArchitecture(savedArch);
        pes.setTargetJVMExeFile(savedJVMExeFile);
        pes.setMainClassPath(savedCP);

        return result;
    }

    public void setProfiledProject(Lookup.Provider project, FileObject singleFile) {
        profiledProject = project;
        profiledSingleFile = singleFile;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (ProfilerControlPanel2.hasDefault())
                    ProfilerControlPanel2.getDefault().setProfiledProject(profiledProject);
            }
        });
    }

    public Lookup.Provider getProfiledProject() {
        return profiledProject;
    }

    public FileObject getProfiledSingleFile() {
        return profiledSingleFile;
    }

    @Override
    public boolean prepareInstrumentation(ProfilingSettings profilingSettings) {
        teardownDispatcher();
        setupDispatcher(profilingSettings);

        ClientUtils.SourceCodeSelection[] marks = MarkingEngine.getDefault().getMarkerMethods();
        profilingSettings.setInstrumentationMarkerMethods(marks);

        return prepareInstrumentationImpl(profilingSettings);
    }
    
    private synchronized ProfilingMonitor getMonitor() {
        if (monitor == null) {
            monitor = new ProfilingMonitor();
            monitor.addPropertyChangeListener(new PropertyChangeListener() {
                        @Override
                        public void propertyChange(PropertyChangeEvent evt) {
                            if (evt.getPropertyName().equals(ProfilingMonitor.PROPERTY_SERVER_STATE)
                                || evt.getPropertyName().equals(ProfilingMonitor.PROPERTY_SERVER_PROGRESS)) {
                                fireServerStateChanged(((ProfilingMonitor)evt.getSource()).getServerState(),
                                                       ((ProfilingMonitor)evt.getSource()).getServerProgress());
                            }
                        }
                    });            
        }
        return monitor;
    }
    
    private boolean prepareInstrumentationImpl(ProfilingSettings profilingSettings) {
        try {
            return super.prepareInstrumentation(profilingSettings);
        } catch (ClientUtils.TargetAppOrVMTerminated e) {
            ProfilerDialogs.displayWarning(e.getMessage());
            e.printStackTrace(System.err);
        } catch (InstrumentationException e) {
            ProfilerDialogs.displayError(e.getMessage());
            e.printStackTrace(System.err);
        } catch (BadLocationException e) {
            ProfilerDialogs.displayError(e.getMessage());
            e.printStackTrace(System.err);
        } catch (ClassNotFoundException e) {
            ProfilerDialogs.displayError(e.getMessage());
            e.printStackTrace(System.err);
        } catch (IOException e) {
            ProfilerDialogs.displayError(e.getMessage());
        } catch (ClassFormatError e) {
            ProfilerDialogs.displayError(e.getMessage());
        }

        return false;
    }

    // TODO [ian] - perform saving of global settings differently
    public void saveFilters() {
        // 1. save global filters
        FileLock lock = null;

        try {
            final FileObject folder = GlobalStorage.getSettingsFolder(true);
            FileObject fo = folder.getFileObject(GLOBAL_FILTERS_FILENAME, "xml"); //NOI18N

            if (fo == null) {
                fo = folder.createData(GLOBAL_FILTERS_FILENAME, "xml"); //NOI18N
            }

            lock = fo.lock();

            final OutputStream os = fo.getOutputStream(lock);
            final BufferedOutputStream bos = new BufferedOutputStream(os);
            final Properties globalFiltersProps = new Properties();
            globalFilters.store(globalFiltersProps);
            globalFiltersProps.storeToXML(bos, ""); //NOI18N
            bos.close();
        } catch (Exception e) {
            ProfilerLogger.log(e);
            ProfilerDialogs.displayError(
                Bundle.NetBeansProfiler_ErrorSavingProfilingSettingsMessage(e.getMessage()));
        } finally {
            if (lock != null) {
                lock.releaseLock();
            }
        }

        // 2. save defined Filter Sets
        lock = null;

        try {
            final FileObject folder = GlobalStorage.getSettingsFolder(true);
            FileObject fo = folder.getFileObject(DEFINED_FILTERSETS_FILENAME, "xml"); //NOI18N

            if (fo == null) {
                fo = folder.createData(DEFINED_FILTERSETS_FILENAME, "xml"); //NOI18N
            }

            lock = fo.lock();

            final OutputStream os = fo.getOutputStream(lock);
            final BufferedOutputStream bos = new BufferedOutputStream(os);
            final Properties definedFilterSetsProps = new Properties();

            definedFilterSets.store(definedFilterSetsProps);

            definedFilterSetsProps.storeToXML(bos, ""); //NOI18N
            bos.close();
        } catch (Exception e) {
            ProfilerLogger.log(e);
            ProfilerDialogs.displayError(
                Bundle.NetBeansProfiler_ErrorSavingFilterSetsMessage(e.getMessage()));
        } finally {
            if (lock != null) {
                lock.releaseLock();
            }
        }
    }

    public void shutdown() {
        getMonitor().stopUpdateThread();
    }

    // (synchronous - blocking) Used for stopping a server from a blocking state ready for direct attach (org.netbeans.modules.j2ee.deployment.profiler.spi.Profiler.shutdown())
    // returns true if the agent was correctly finished from blocked state, false otherwise
    public boolean shutdownBlockedAgent(String host, int port, int agentId) {
        int state = getAgentState(host, port, agentId);

        if (state == CommonConstants.AGENT_STATE_READY_DIRECT) {
            Socket clientSocket = null;
            ObjectOutputStream socketOut = null;
            ObjectInputStream socketIn = null;

            try {
                clientSocket = new Socket(host, port);
                clientSocket.setSoTimeout(100);
                clientSocket.setTcpNoDelay(true); // Necessary at least on Solaris to avoid delays in e.g. readInt() etc.
                socketOut = new ObjectOutputStream(clientSocket.getOutputStream());
                socketIn = new ObjectInputStream(clientSocket.getInputStream());

                WireIO wio = new WireIO(socketOut, socketIn);
                wio.sendSimpleCommand(Command.TERMINATE_TARGET_JVM);

                Object o = wio.receiveCommandOrResponse();

                if (o instanceof Response && ((Response) o).isOK()) {
                    return true;
                } else {
                    return false;
                }
            } catch (IOException e) {
                return false;
            } finally {
                try {
                    if (socketIn != null) {
                        socketIn.close();
                    }

                    if (socketOut != null) {
                        socketOut.close();
                    }

                    if (clientSocket != null) {
                        clientSocket.close();
                    }
                } catch (IOException e) {
                    ProfilerLogger.log(e);
                }
            }
        } else {
            return false;
        }
    }

    public void stopApp() {
        setTransitionState();
        getTargetAppRunner().terminateTargetJVM();
    }

    private Properties getAgentProperties(int port) {
        File f = getInfoFile(port);

        if (!f.exists()) {
            return null; // No agent is running
        }

        BufferedInputStream bis = null;

        try {
            Properties props = new Properties();
            FileInputStream fis = new FileInputStream(f);
            bis = new BufferedInputStream(fis);

            props.load(bis);

            bis.close();

            return props;
        } catch (IOException e) {
            // commented out, the file is sometimes deleted before creating FileInputStream,
            // which results in FileNotFoundException. This actually means that the file doesn't exist
            // and that the Profiler is connected, so silently returning null is correct.
            //e.printStackTrace();
            return null;
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static File getInfoFile(int port) {
        String homeDir = System.getProperty("user.home"); // NOI18N

        return new File(homeDir + File.separator + ".nbprofiler" + File.separator + port); // NOI18N
    }

    // checks if there is a profiling session currently in progress communicating over specified port
    private boolean isProfilingRunningOnPort(int port) {
        return (profilingState == PROFILING_RUNNING) && (port == getTargetAppRunner().getProfilerEngineSettings().getPortNo());
    }

    private void setTransitionState() {
        changeStateTo(PROFILING_IN_TRANSITION);
    }

    private void changeStateTo(int newState) {
        if (profilingState == newState) {
            return;
        }

        final int oldProfilingState = profilingState;
        profilingState = newState;
        fireProfilingStateChange(oldProfilingState, profilingState);

        if ((newState == PROFILING_INACTIVE) || (newState == PROFILING_STOPPED)) {
            cleanupAfterProfiling();
        }
        
        if (newState == PROFILING_RUNNING && CommonConstants.JDK_15_STRING.
                equals(getTargetAppRunner().getProfilerEngineSettings().getTargetJDKVersionString())) {
            if (lastProfilingSettings.getProfilingType() == ProfilingSettings.PROFILE_MEMORY_SAMPLING)
                SwingUtilities.invokeLater(new Runnable() { // Let the underlying dialogs close first
                    public void run() {
                        ProfilerDialogs.displayWarning(Bundle.NetBeansProfiler_MemorySamplingJava5());
                    }
                });                    
        }
    }

    protected void cleanupAfterProfiling() {
        teardownDispatcher();
        MarkingEngine.getDefault().deconfigure();
        ClassRepository.cleanup();
    }

    private void cleanupBeforeProfiling(ProfilerEngineSettings sharedSettings) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                NetBeansProfiler.this.getThreadsManager().reset();
                NetBeansProfiler.this.getVMTelemetryManager().reset();
                if (LiveResultsWindow.hasDefault())
                    LiveResultsWindow.getDefault().handleCleanupBeforeProfiling();
            }
        });
        ResultsManager.getDefault().reset();

        ClassRepository.clearCache();
        ClassRepository.initClassPaths(sharedSettings.getWorkingDir(), sharedSettings.getVMClassPaths());
    }

    private void displayWarningAboutEntireAppProfiling() {
        ProfilerDialogs.displayWarning(Bundle.NetBeansProfiler_EntireApplicationProfilingWarning());
    }

    // -- Package-Private stuff --------------------------------------------------------------------------------------------
    private void loadGlobalFilters() {
        try {
            FileObject folder = GlobalStorage.getSettingsFolder(false);
            FileObject configFolder = FileUtil.getConfigFile("NBProfiler/Config");

            // 1. Deal with global filters
            FileObject filtersFO = null;

            if ((folder != null) && folder.isValid()) {
                filtersFO = folder.getFileObject(GLOBAL_FILTERS_FILENAME, "xml"); //NOI18N
            }

            if ((filtersFO == null) && (configFolder != null) && configFolder.isValid()) {
                filtersFO = configFolder.getFileObject(GLOBAL_FILTERS_FILENAME + DEFAULT_FILE_SUFFIX, "xml"); //NOI18N
            }

            if (filtersFO != null) {
                final InputStream fis = filtersFO.getInputStream();
                final BufferedInputStream bis = new BufferedInputStream(fis);
                final Properties globalFiltersProps = new Properties();
                globalFiltersProps.loadFromXML(bis);
                globalFilters = new GlobalFilters();
                globalFilters.load(globalFiltersProps);
                bis.close();
            }

            // 2. Deal with defined filter sets
            FileObject filterSetsFO = null;

            if ((folder != null) && folder.isValid()) {
                filterSetsFO = folder.getFileObject(DEFINED_FILTERSETS_FILENAME, "xml"); //NOI18N
            }

            if ((filterSetsFO == null) && (configFolder != null) && configFolder.isValid()) {
                filterSetsFO = configFolder.getFileObject(DEFINED_FILTERSETS_FILENAME + DEFAULT_FILE_SUFFIX, "xml"); //NOI18N
            }

            if (filterSetsFO != null) {
                final InputStream fis = filterSetsFO.getInputStream();
                final BufferedInputStream bis = new BufferedInputStream(fis);
                final Properties definedFilterSetsProps = new Properties();
                definedFilterSetsProps.loadFromXML(bis);
                definedFilterSets = new DefinedFilterSets();
                definedFilterSets.load(definedFilterSetsProps);
                bis.close();
            }
        } catch (Exception e) {
            ProfilerLogger.log(e);
            ProfilerDialogs.displayError(
                Bundle.NetBeansProfiler_ErrorLoadingProfilingSettingsMessage(e.getMessage()));
        }
    }

    // -- Private implementation -------------------------------------------------------------------------------------------
    private void openWindowsOnProfilingStart() {
        int telemetryBehavior = ideSettings.getTelemetryOverviewBehavior();
        int threadsBehavior = ideSettings.getThreadsViewBehavior();
        int locksBehavior = ideSettings.getLockContentionViewBehavior();

        boolean threadsEnabled = lastProfilingSettings.getThreadsMonitoringEnabled();
        boolean lockContentionEnabled = lastProfilingSettings.getLockContentionMonitoringEnabled();
        int type = lastProfilingSettings.getProfilingType();

        // 1. Telemetry Overview
        if ((telemetryBehavior == ProfilerIDESettings.OPEN_ALWAYS)
                || ((telemetryBehavior == ProfilerIDESettings.OPEN_MONITORING) && (type == ProfilingSettings.PROFILE_MONITOR))) {
            TelemetryOverviewPanel.getDefault().open();
            TelemetryOverviewPanel.getDefault().requestVisible();
        }

        // 2. Threads view
        if (threadsEnabled) {
            if ((threadsBehavior == ProfilerIDESettings.OPEN_ALWAYS)
                    || ((threadsBehavior == ProfilerIDESettings.OPEN_MONITORING) && (type == ProfilingSettings.PROFILE_MONITOR))) {
                ThreadsWindow.getDefault().open();
                ThreadsWindow.getDefault().requestVisible();
            }
        }
        
        // 3. Lock Contention view
        if (lockContentionEnabled) {
            if ((locksBehavior == ProfilerIDESettings.OPEN_ALWAYS)
                    || ((locksBehavior == ProfilerIDESettings.OPEN_MONITORING) && (type == ProfilingSettings.PROFILE_MONITOR))) {
                LockContentionWindow.getDefault().showView();
            }
        }

        // 4. Live Results
        if ((ideSettings.getDisplayLiveResultsCPU()
                && ((type == ProfilingSettings.PROFILE_CPU_ENTIRE) || (type == ProfilingSettings.PROFILE_CPU_PART)
                    || (type == ProfilingSettings.PROFILE_CPU_SAMPLING)))
                || (ideSettings.getDisplayLiveResultsFragment() && (type == ProfilingSettings.PROFILE_CPU_STOPWATCH))
                || (ideSettings.getDisplayLiveResultsMemory()
                       && ((type == ProfilingSettings.PROFILE_MEMORY_ALLOCATIONS)
                           || (type == ProfilingSettings.PROFILE_MEMORY_LIVENESS)
                           || (type == ProfilingSettings.PROFILE_MEMORY_SAMPLING)))) {
            LiveResultsWindow.getDefault().open();
            LiveResultsWindow.getDefault().requestVisible();
        }

        // 5. Control Panel displayed always, and getting focus
        final ProfilerControlPanel2 controlPanel2 = ProfilerControlPanel2.getDefault();
        controlPanel2.open();
        controlPanel2.requestActive();
    }

    @NbBundle.Messages({
        "MSG_StartingProfilerClient=Starting Profiler Client"
    })
    public boolean startEx(final ProfilingSettings profilingSettings, final SessionSettings sessionSettings, final AtomicBoolean cancel) {
        final boolean[] rslt = new boolean[1];
        final CountDownLatch latch = new CountDownLatch(1);
        
        new SwingWorker(false) {
            volatile private ProgressDisplayer pd;
            @Override
            protected void doInBackground() {
                if (isCancelled()) return;
                
                connectToStartedApp(profilingSettings, sessionSettings, cancel);
            }

            @Override
            protected void done() {
                if (pd != null) {
                    pd.close();
                    pd = null;
                }
                rslt[0] = true;
                latch.countDown();
            }

            @Override
            protected void nonResponding() {
                final SwingWorker thiz = this;
                pd = ProfilerProgressDisplayer.getDefault().showProgress(Bundle.MSG_StartingProfilerClient(), new ProgressDisplayer.ProgressController() {
                    @Override
                    public boolean cancel() {
                        thiz.cancel();
                        return true;
                    }
                });
            }

            @Override
            protected void cancelled() {
                if (pd != null) {
                    pd.close();
                    pd = null;
                }
                rslt[0] = false;
                cancel.set(true);
                latch.countDown();
            }

            @Override
            protected int getWarmup() {
                return 1500;
            }
        }.execute();
        
        try {
            latch.await();
            return rslt[0];
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return false;
    }
    
    public void setupDispatcher(ProfilingSettings profilingSettings) {
        lastProfilingSettings = profilingSettings;
        synchronized (setupLock) {
            final Lookup.Provider project = getProfiledProject();

            // configure call-context-tree dispatching infrastructure
            CCTProvider cctProvider = null;
            Collection<?extends CCTProvider.Listener> cctListeners = null;

            switch (profilingSettings.getProfilingType()) {
                case ProfilingSettings.PROFILE_CPU_ENTIRE:
                case ProfilingSettings.PROFILE_CPU_PART:
                case ProfilingSettings.PROFILE_CPU_SAMPLING:
                case ProfilingSettings.PROFILE_CPU_STOPWATCH: {
                    cctProvider = Lookup.getDefault().lookup(CPUCCTProvider.class);
                    cctListeners = Lookup.getDefault().lookupAll(CPUCCTProvider.Listener.class);

                    break;
                }
                case ProfilingSettings.PROFILE_MEMORY_ALLOCATIONS:
                case ProfilingSettings.PROFILE_MEMORY_LIVENESS: {
                    cctProvider = Lookup.getDefault().lookup(MemoryCCTProvider.class);
                    cctListeners = Lookup.getDefault().lookupAll(MemoryCCTProvider.Listener.class);

                    break;
                }
            }

            if ((cctProvider != null) && (cctListeners != null) && (cctListeners.size() > 0)) {
                for (CCTProvider.Listener cctListener : cctListeners) {
                    if (LOGGER.isLoggable(Level.FINEST)) {
                        LOGGER.log(Level.FINEST, "Adding listener {0} to the provider {1}", new Object[]{cctListener.getClass().getName(), cctProvider.getClass().getName()});
                    }

                    cctProvider.addListener(cctListener);
                }
            } else {
                if (LOGGER.isLoggable(Level.WARNING)) {
                    if (cctProvider == null) {
                        LOGGER.warning("Could not find a CCT provider in lookup!");
                    } else if ((cctListeners == null) || (cctListeners.isEmpty())) {
                        LOGGER.warning("Could not find listeners in lookup!");
                    }
                }
            }

            for(SessionListener sl : Lookup.getDefault().lookupAll(SessionListener.class)) {
                sl.onStartup(profilingSettings, project);
            }

            ProfilerClient client = getTargetAppRunner().getProfilerClient();

            CCTResultsFilter filter = Lookup.getDefault().lookup(CCTResultsFilter.class);

            if (filter != null) {
                filter.reset(); // clean up the filter before reusing it
                filter.setEvaluators(Lookup.getDefault().lookupAll(CCTResultsFilter.EvaluatorProvider.class));
            }

            // init context aware instances
            FlatProfileBuilder fpb = Lookup.getDefault().lookup(FlatProfileBuilder.class);
            TimeCollector tc = Lookup.getDefault().lookup(TimeCollector.class);
            fpb.setContext(client, tc, filter);

            Collection listeners = null;
            switch (profilingSettings.getProfilingType()) {
                case ProfilingSettings.PROFILE_CPU_PART:
                case ProfilingSettings.PROFILE_CPU_ENTIRE:
                case ProfilingSettings.PROFILE_CPU_SAMPLING: {
                    listeners = Lookup.getDefault().lookupAll(CPUProfilingResultListener.class);

                    for (Iterator iter = listeners.iterator(); iter.hasNext();) {
                        CPUProfilingResultListener listener = (CPUProfilingResultListener) iter.next();
                        ProfilingResultsDispatcher.getDefault().addListener(listener);
                        listener.startup(getTargetAppRunner().getProfilerClient());
                    }
                    break;
                }
                case ProfilingSettings.PROFILE_MEMORY_ALLOCATIONS:
                case ProfilingSettings.PROFILE_MEMORY_LIVENESS: {
                    listeners = Lookup.getDefault().lookupAll(MemoryProfilingResultsListener.class);

                    for (Iterator iter = listeners.iterator(); iter.hasNext();) {
                        MemoryProfilingResultsListener listener = (MemoryProfilingResultsListener) iter.next();
                        ProfilingResultsDispatcher.getDefault().addListener(listener);
                        listener.startup(getTargetAppRunner().getProfilerClient());
                    }
                }
                default: {
                    listeners = Lookup.getDefault().lookupAll(LockProfilingResultListener.class);

                    for (Iterator iter = listeners.iterator(); iter.hasNext();) {
                        LockProfilingResultListener listener = (LockProfilingResultListener) iter.next();
                        ProfilingResultsDispatcher.getDefault().addListener(listener);
                        listener.startup(getTargetAppRunner().getProfilerClient());
                    }
                    
                }
            }
            
            ProfilingPointsProcessor ppp = getProfilingPointsManager();
            if (ppp != null) ppp.init(getProfiledProject());

            ProfilingResultsDispatcher.getDefault().startup(client);
        }
    }

    // Used for killing an agent which could cause a collision on port
    // Returns true if TERMINATE_TARGET_JVM was invoked on agent (not necessarily killed!), false if the agent is already profiling (port is used)
    private boolean shutdownAgent(String host, int port) {
        if (port == -1) return false; // invalid port
        
        Socket clientSocket = null;
        ObjectOutputStream socketOut = null;
        ObjectInputStream socketIn = null;

        try {
            clientSocket = new Socket(host, port);
            clientSocket.setSoTimeout(100);
            clientSocket.setTcpNoDelay(true); // Necessary at least on Solaris to avoid delays in e.g. readInt() etc.
            socketOut = new ObjectOutputStream(clientSocket.getOutputStream());
            socketIn = new ObjectInputStream(clientSocket.getInputStream());

            WireIO wio = new WireIO(socketOut, socketIn);
            wio.sendSimpleCommand(Command.TERMINATE_TARGET_JVM);

            try {
                Object o = wio.receiveCommandOrResponse();
            } catch (Exception e) {
            } // Throws SocketTimeoutException!

            ProfilerLogger.warning(">>> An existing Profiler agent listening on port " + port
                                   + " was terminated to allow starting new profiling session on the same port."); // NOI18N

            return true;
        } catch (SocketTimeoutException e) { // port already in use

            return false;
        } catch (IOException e) {
            return true;
        } finally {
            try {
                if (socketIn != null) {
                    socketIn.close();
                }

                if (socketOut != null) {
                    socketOut.close();
                }

                if (clientSocket != null) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                //        e.printStackTrace(System.err);
            }
        }
    }

    private void teardownDispatcher() {
        synchronized (setupLock) {
            ProfilingResultsDispatcher.getDefault().shutdown();

            //      Collection<? extends CCTProvider> cctProviders = Lookup.getDefault().lookupAll(CCTProvider.class);
            //      for(CCTProvider cctProvider : cctProviders) {
            //        cctProvider.removeAllListeners();
            //      }
//            StatisticalModuleContainer statModulesContainer = Lookup.getDefault().lookup(StatisticalModuleContainer.class);
//
//            if (statModulesContainer != null) {
//                statModulesContainer.removeAllModules();
//            }

            // deconfigure the profiler client
            ProfilerClient client = getTargetAppRunner().getProfilerClient();
            client.registerFlatProfileProvider(null);

//            // deconfigure the marking engine
//            MarkingEngine.getDefault().deconfigure();
            for(SessionListener sl : Lookup.getDefault().lookupAll(SessionListener.class)) {
                sl.onShutdown();
            }
        }
    }

    private ProfilingPointsProcessor getProfilingPointsManager() {
        return Lookup.getDefault().lookup(ProfilingPointsProcessor.class);
    }
    
    private boolean waitForDeleteAgentFile(File agentFile) {
        if (agentFile.delete()) {
            return true;
        }

        for (int i = 0; i < 5; i++) {
            if (agentFile.delete()) {
                return true;
            }

            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
            }
        }

        return false;
    }
    
    private static void logActionConfig(String actionTitle, ProfilingSettings ps, SessionSettings ss, AttachSettings as, InstrumentationFilter f) {
        assert actionTitle != null;
        assert ps != null;
        assert f != null;
        
        LOGGER.log(Level.CONFIG, 
            "*** Profiler Action = {0}\n" + //NOI18N
            ">>> Profiling Settings = \n" + // NOI18N
            "{1}\n" +// NOI18N
            ">>> {2} Settings = \n" + // NOI18N
            "{3}\n" + // NOI18N
            ">>> Instrumentation Filter = \n" + //NOI18N
            "{4}", // NOI18N
            new Object[]{
                actionTitle,
                ps.debug(),
                (ss != null ? "Session" : (as != null ? "Attach" : null)),  // NOI18N
                (ss != null ? ss.debug() : (as != null ? as.debug() : null)),                    
                f.debug()
            }
        );
    }
}
