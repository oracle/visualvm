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

import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.project.Project;
import org.netbeans.lib.profiler.ProfilerClient;
import org.netbeans.lib.profiler.ProfilerEngineSettings;
import org.netbeans.lib.profiler.ProfilerLogger;
import org.netbeans.lib.profiler.ProfilingEventListener;
import org.netbeans.lib.profiler.TargetAppRunner;
import org.netbeans.lib.profiler.classfile.ClassRepository;
import org.netbeans.lib.profiler.client.AppStatusHandler;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.client.RuntimeProfilingPoint;
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
import org.netbeans.lib.profiler.ui.cpu.statistics.StatisticalModule;
import org.netbeans.lib.profiler.ui.cpu.statistics.StatisticalModuleContainer;
import org.netbeans.lib.profiler.wireprotocol.Command;
import org.netbeans.lib.profiler.wireprotocol.Response;
import org.netbeans.lib.profiler.wireprotocol.WireIO;
import org.netbeans.modules.profiler.actions.RerunAction;
import org.netbeans.modules.profiler.ppoints.ProfilingPointsManager;
import org.netbeans.modules.profiler.ppoints.ui.ProfilingPointsWindow;
import org.netbeans.modules.profiler.spi.LoadGenPlugin;
import org.netbeans.modules.profiler.ui.NBSwingWorker;
import org.netbeans.modules.profiler.ui.ProfilerDialogs;
import org.netbeans.modules.profiler.ui.stats.ProjectAwareStatisticalModule;
import org.netbeans.modules.profiler.utils.IDEUtils;
import org.netbeans.modules.profiler.utils.OutputParameter;
import org.openide.DialogDescriptor;
import org.openide.ErrorManager;
import org.openide.NotifyDescriptor;
import org.openide.awt.StatusDisplayer;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.actions.CallableSystemAction;
import org.openide.windows.WindowManager;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import org.apache.tools.ant.module.api.support.ActionUtils;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.lib.profiler.results.cpu.FlatProfileBuilder;
import org.netbeans.lib.profiler.results.cpu.cct.TimeCollector;
import org.netbeans.lib.profiler.ui.monitor.VMTelemetryModels;
import org.netbeans.modules.profiler.heapwalk.HeapDumpWatch;
import org.netbeans.modules.profiler.utils.GoToSourceHelper;
import org.netbeans.modules.profiler.utils.JavaSourceLocation;
import org.openide.execution.ExecutorTask;


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
@org.openide.util.lookup.ServiceProvider(service=org.netbeans.lib.profiler.common.Profiler.class)
public final class NetBeansProfiler extends Profiler {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    public static final class ProgressPanel implements AppStatusHandler.AsyncDialog {
        //~ Static fields/initializers -------------------------------------------------------------------------------------------

        private static final int DEFAULT_WIDTH = 350;
        private static final int DEFAULT_HEIGHT = 100;
        private static final RequestProcessor commandQueue = new RequestProcessor("Async dialog command queue", 1); // NOI18N

        //~ Enumerations ---------------------------------------------------------------------------------------------------------

        private enum DialogState {//~ Enumeration constant initializers --------------------------------------------------------------------------------

            CLOSED, NOT_OPENED, NOT_OPENED_CLOSED, OPEN;
        }

        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private Dialog dialog;
        private final Object dialogStateLock = new Object();
        private final Object dialogInitLock = new Object();

        //@GuardedBy dialogStatusLock
        private DialogState dialogState = DialogState.NOT_OPENED;
        private String message;
        private boolean cancelAllowed;
        private boolean cancelled = false;
        //@GuardedBy dialogInitLock
        private boolean instantiated;
        private boolean showProgress;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        ProgressPanel(final String message, final boolean showProgress, final boolean cancelAllowed) {
            this.message = message;
            this.showProgress = showProgress;
            this.cancelAllowed = cancelAllowed;
            this.dialogState = DialogState.NOT_OPENED;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public boolean isDisplayed() {
            synchronized (dialogStateLock) {
                return dialogState == DialogState.OPEN;
            }

            //      return dialog.isVisible();
        }

        public boolean cancelPressed() {
            return cancelled;
        }

        public synchronized void close() {
            // run the close command on a separate serializing queue
            commandQueue.post(new Runnable() {
                    public void run() {
                        dialogClose();
                    }
                });
        }

        /**
         * This method is called to display the asynchronous wait dialog. It should block
         * until the user explicitely cancels or method AsyncDialog.close is called
         */
        public synchronized void display() {
            // run the display command on a separate serializing queue
            commandQueue.post(new Runnable() {
                    public void run() {
                        instantiate();
                        dialogShow();
                    }
                });
        }

        private void dialogClose() {
            synchronized (dialogStateLock) {
                if (dialogState == DialogState.OPEN) {
                    LOGGER.finest("Closing async dialog"); // NOI18N

                    if (dialog.isShowing()) {
                        ProfilerDialogs.close(dialog);
                    }

                    dialogState = DialogState.CLOSED;
                } else if (dialogState == DialogState.NOT_OPENED) {
                    LOGGER.fine("Attempting to close async dialog without opening it first"); // NOI18N
                    dialogState = DialogState.NOT_OPENED_CLOSED;
                }
            }
        }

        private void dialogShow() {
            synchronized (dialogStateLock) {
                if ((dialogState == DialogState.NOT_OPENED) || (dialogState == DialogState.CLOSED)) {
                    LOGGER.finest("Showing async dialog"); // NOI18N

                    Level lvl = LOGGER.getLevel();
                    ProfilerDialogs.display(dialog);
                    dialogState = DialogState.OPEN;
                } else if (dialogState == DialogState.NOT_OPENED_CLOSED) {
                    LOGGER.fine("Async dialog has been closed before being opened. Setting to CLOSED"); // NOI18N
                    dialogState = DialogState.CLOSED;
                }
            }
        }

        private void instantiate() {
            synchronized(dialogInitLock) {
                if (instantiated) {
                    return;
                }

                JPanel panel = new JPanel();
                panel.setLayout(new BorderLayout(10, 10));
                panel.setBorder(new EmptyBorder(15, 15, 15, 15));
                panel.add(new JLabel(message), BorderLayout.NORTH);

                final Dimension ps = panel.getPreferredSize();
                ps.setSize(Math.max(ps.getWidth(), DEFAULT_WIDTH),
                           Math.max(ps.getHeight(), showProgress ? DEFAULT_HEIGHT : ps.getHeight()));
                panel.setPreferredSize(ps);

                if (showProgress) {
                    final JProgressBar progress = new JProgressBar();
                    progress.setIndeterminate(true);
                    panel.add(progress, BorderLayout.SOUTH);
                }

                dialog = ProfilerDialogs.createDialog(new DialogDescriptor(panel, PROGRESS_DIALOG_CAPTION, true,
                                                                           cancelAllowed
                                                                           ? new Object[] { DialogDescriptor.CANCEL_OPTION }
                                                                           : new Object[] {  }, DialogDescriptor.CANCEL_OPTION,
                                                                           DialogDescriptor.RIGHT_ALIGN, null,
                                                                           new ActionListener() {
                        public void actionPerformed(final ActionEvent e) {
                            cancelled = true;

                            synchronized (dialogStateLock) {
                                assert dialogState == DialogState.OPEN;
                                LOGGER.finest("Closing async dialog (cancel)"); // NOI18N
                                dialogState = DialogState.CLOSED;
                            }
                        }
                    }));
                instantiated = true;
            }
        }
    }

    // -- NetBeansProfiler-only callback classes ---------------------------------------------------------------------------
    private final class IDEAppStatusHandler implements AppStatusHandler {
        //~ Methods --------------------------------------------------------------------------------------------------------------
        public AppStatusHandler.AsyncDialog getAsyncDialogInstance(final String message, final boolean showProgress, final boolean cancelAllowed) {
            return new ProgressPanel(message, showProgress, cancelAllowed);
        }

        public boolean confirmWaitForConnectionReply() {
            NotifyDescriptor.Confirmation con = new NotifyDescriptor.Confirmation(TARGET_APP_NOT_RESPONDING_MSG,
                                                                                  TARGET_APP_NOT_RESPONDING_DIALOG_TITLE,
                                                                                  ProfilerDialogs.DNSAMessage.YES_NO_OPTION,
                                                                                  ProfilerDialogs.DNSAMessage.WARNING_MESSAGE);

            return (ProfilerDialogs.notify(con) != ProfilerDialogs.DNSAConfirmation.YES_OPTION);
        }

        // The following methods should display messages asynchronously, i.e. they shouldn't block the current
        // thread waiting for the user pressing OK.
        public void displayError(final String msg) {
            printDebugMsg("IDEAppStatusHandler - error: " + msg); //NOI18N
            NetBeansProfiler.this.displayError(msg);
        }

        // These method SHOULD wait for the user to press ok, since they may be used in a sequence of displayed
        // panels, and the next one shouldn't be displayed before the previous one is read and understood.
        public void displayErrorAndWaitForConfirm(final String msg) {
            printDebugMsg("IDEAppStatusHandler - errorAndWaitForConfirm: " + msg); //NOI18N
            NetBeansProfiler.this.displayErrorAndWait(msg);
        }

        public void displayErrorWithDetailsAndWaitForConfirm(String shortMsg, String detailsMsg) {
            NetBeansProfiler.this.displayErrorWithDetailsAndWait(shortMsg, detailsMsg);
        }

        public void displayNotification(final String msg) {
            StatusDisplayer.getDefault().setStatusText(msg);
        }

        public void displayNotificationAndWaitForConfirm(final String msg) {
            NetBeansProfiler.this.displayInfoAndWait(msg);
        }

        public void displayNotificationWithDetailsAndWaitForConfirm(String shortMsg, String detailsMsg) {
            NetBeansProfiler.this.displayInfoWithDetailsAndWait(shortMsg, detailsMsg);
        }

        public void displayWarning(final String msg) {
            printDebugMsg("IDEAppStatusHandler - warning: " + msg); //NOI18N
            NetBeansProfiler.this.displayWarning(msg);
        }

        public void displayWarningAndWaitForConfirm(final String msg) {
            printDebugMsg("IDEAppStatusHandler - warningAndWaitForConfirm: " + msg); //NOI18N
            NetBeansProfiler.this.displayWarningAndWait(msg);
        }

        public void handleShutdown() {
            //      IDEUtils.runInEventDispatchThreadAndWait( // According to Issue 74914 this cannot run in AWT-EventQueue
            //          new Runnable() {
            //            public void run() {

            // Asynchronously update live results if autorefresh is on
            if (LiveResultsWindow.hasDefault()) {
                LiveResultsWindow.getDefault().handleShutdown();
            }

            if ((Profiler.getDefault().getTargetAppRunner().getProfilerClient().getCurrentInstrType() == CommonConstants.INSTR_NONE)
                    || !ResultsManager.getDefault().resultsAvailable()) {
                ProfilerDialogs.DNSAMessage dnsa = new ProfilerDialogs.DNSAMessage("NetBeansProfiler.handleShutdown.noResults", //NOI18N
                                                                                   TERMINATE_VM_ON_EXIT_MSG,
                                                                                   ProfilerDialogs.DNSAMessage.INFORMATION_MESSAGE);
                dnsa.setDNSADefault(false);
                ProfilerDialogs.notify(dnsa);
            } else {
                ProfilerDialogs.DNSAConfirmation dnsa = new ProfilerDialogs.DNSAConfirmation("NetBeansProfiler.handleShutdown", //NOI18N
                                                                                             TAKE_SNAPSHOT_ON_EXIT_MSG,
                                                                                             TAKE_SNAPSHOT_ON_EXIT_DIALOG_TITLE,
                                                                                             ProfilerDialogs.DNSAConfirmation.YES_NO_OPTION);
                dnsa.setDNSADefault(false);

                if (ProfilerDialogs.notify(dnsa).equals(ProfilerDialogs.DNSAConfirmation.YES_OPTION)) {
                    ResultsManager.getDefault().takeSnapshot();
                }
            }

            //            }
            //          }
            //      );
        }

        public void pauseLiveUpdates() {
            LiveResultsWindow.setPaused(true);
        }

        public void resultsAvailable() {
            ResultsManager.getDefault().resultsBecameAvailable();
        }

        public void resumeLiveUpdates() {
            LiveResultsWindow.setPaused(false);
        }

        public void takeSnapshot() {
            IDEUtils.runInProfilerRequestProcessor(new Runnable() {
                    public void run() {
                        ResultsManager.getDefault().takeSnapshot();
                    }
                });
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final Logger LOGGER = Logger.getLogger(NetBeansProfiler.class.getName());

    static {
        if ((LOGGER.getLevel() == null) || (LOGGER.getLevel().intValue() > Level.CONFIG.intValue())) {
            LOGGER.setLevel(Level.CONFIG); // artificialy set the logger level to debugging; should be removed later on
        }
    }

    // -----
    // I18N String constants
    private static final String CALIBRATION_FAILED_MESSAGE = NbBundle.getMessage(ProfilerModule.class,
                                                                                 "ProfilerModule_CalibrationFailedMessage"); //NOI18N
    private static final String CALIBRATION_MISSING_MESSAGE = NbBundle.getMessage(ProfilerModule.class,
                                                                                  "NetBeansProfiler_MustCalibrateFirstMsg"); //NOI18N
    private static final String CALIBRATION_MISSING_SHORT_MESSAGE = NbBundle.getMessage(ProfilerModule.class,
                                                                                        "NetBeansProfiler_MustCalibrateFirstShortMsg"); //NOI18N
    private static final String PROGRESS_DIALOG_CAPTION = NbBundle.getMessage(NetBeansProfiler.class,
                                                                              "NetBeansProfiler_ProgressDialogCaption"); //NOI18N
    private static final String ENTIRE_APPLICATION_PROFILING_WARNING = NbBundle.getMessage(NetBeansProfiler.class,
                                                                                           "NetBeansProfiler_EntireApplicationProfilingWarning"); //NOI18N
    private static final String DIRECTORY_DOES_NOT_EXIST_MESSAGE = NbBundle.getMessage(NetBeansProfiler.class,
                                                                                       "NetBeansProfiler_DirectoryDoesNotExistMessage"); //NOI18N
    private static final String DIRECTORY_IS_WRITE_PROTECTED_MESSAGE = NbBundle.getMessage(NetBeansProfiler.class,
                                                                                           "NetBeansProfiler_DirectoryIsWriteProtectedMessage"); //NOI18N
    private static final String ERROR_LOADING_PROFILING_SETTINGS_MESSAGE = NbBundle.getMessage(NetBeansProfiler.class,
                                                                                               "NetBeansProfiler_ErrorLoadingProfilingSettingsMessage"); //NOI18N
    private static final String ERROR_SAVING_PROFILING_SETTINGS_MESSAGE = NbBundle.getMessage(NetBeansProfiler.class,
                                                                                              "NetBeansProfiler_ErrorSavingProfilingSettingsMessage"); //NOI18N
    private static final String ERROR_SAVING_FILTER_SETS_MESSAGE = NbBundle.getMessage(NetBeansProfiler.class,
                                                                                       "NetBeansProfiler_ErrorSavingFilterSetsMessage"); //NOI18N
    private static final String ERROR_SAVING_ATTACH_SETTINGS_MESSAGE = NbBundle.getMessage(NetBeansProfiler.class,
                                                                                           "NetBeansProfiler_ErrorSavingAttachSettingsMessage"); //NOI18N
    private static final String CANNOT_FIND_LIBS_MSG = NbBundle.getMessage(NetBeansProfiler.class,
                                                                           "NetBeansProfiler_CannotFindLibsMsg"); //NOI18N
    private static final String ENGINE_INIT_FAILED_MSG = NbBundle.getMessage(NetBeansProfiler.class,
                                                                             "NetBeansProfiler_EngineInitFailedMsg"); //NOI18N
    private static final String INITIAL_CALIBRATION_MSG = NbBundle.getMessage(NetBeansProfiler.class,
                                                                              "NetBeansProfiler_InitialCalibrationMsg"); //NOI18N
    private static final String TERMINATE_VM_ON_EXIT_MSG = NbBundle.getMessage(NetBeansProfiler.class,
                                                                               "NetBeansProfiler_TerminateVMOnExitMsg"); //NOI18N
    private static final String TAKE_SNAPSHOT_ON_EXIT_MSG = NbBundle.getMessage(NetBeansProfiler.class,
                                                                                "NetBeansProfiler_TakeSnapshotOnExitMsg"); //NOI18N
    private static final String TAKE_SNAPSHOT_ON_EXIT_DIALOG_TITLE = NbBundle.getMessage(NetBeansProfiler.class,
                                                                                         "NetBeansProfiler_TakeSnapshotOnExitDialogTitle"); //NOI18N
    private static final String TARGET_APP_NOT_RESPONDING_MSG = NbBundle.getMessage(NetBeansProfiler.class,
                                                                                    "NetBeansProfiler_TargetAppNotRespondingMsg"); //NOI18N
    private static final String TARGET_APP_NOT_RESPONDING_DIALOG_TITLE = NbBundle.getMessage(NetBeansProfiler.class,
                                                                                             "NetBeansProfiler_TargetAppNotRespondingDialogTitle"); //NOI18N
    private static final String MODIFYING_INSTRUMENTATION_MSG = NbBundle.getMessage(NetBeansProfiler.class,
                                                                                    "NetBeansProfiler_ModifyingInstrumentationMsg"); //NOI18N
                                                                                                                                     // -----
    static final ErrorManager profilerErrorManager = ErrorManager.getDefault().getInstance("org.netbeans.modules.profiler"); //NOI18N
    private static final String GLOBAL_FILTERS_FILENAME = "filters"; //NOI18N
    private static final String DEFINED_FILTERSETS_FILENAME = "filtersets"; //NOI18N
    private static final String DEFAULT_FILE_SUFFIX = "-default"; //NOI18N
    private static final String ATTACH_SETTINGS_FILENAME = "attach"; //NOI18N
    private static boolean initialized = false;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    AppStatusHandler.AsyncDialog waitDialog = null;

    //--- Modifying instrumentation progress -------------------------------------
    boolean shouldDisplayDialog = true;

    // TODO [release] set to obtain from property
    //  static boolean DEBUG = true; // System.getProperty("org.netbeans.modules.profiler.NetBeansProfiler") != null;
    private final ProfilerIDESettings ideSettings = ProfilerIDESettings.getInstance();
    private final ProfilingMonitor monitor = new ProfilingMonitor();
    private final TargetAppRunner targetAppRunner;
    private DefinedFilterSets definedFilterSets;
    private FileObject profiledSingleFile;

    // remembered values for rerun and modify actions
    private ProfilerControlPanel2Support actionSupport = new ProfilerControlPanel2Support();
    private GlobalFilters globalFilters;
    private final Object setupLock = new Object();
    private ProfilingSettings lastProfilingSettings;
    private Project profiledProject = null;
    private SessionSettings lastSessionSettings;
    private StringBuilder logMsgs = new StringBuilder();
    private ThreadsDataManager threadsManager;
    private VMTelemetryDataManager vmTelemetryManager;
    private VMTelemetryModels vmTelemetryModels;
    private boolean calibrating = false;

    // ---------------------------------------------------------------------------
    // Temporary workaround to refresh profiling points when LiveResultsWindow is not refreshing
    // TODO: implement better approach for refreshing profiling points and remove this code
    private boolean processesProfilingPoints;
    private boolean silent;
    private boolean threadsMonitoringEnabled = false;
    private boolean waitDialogOpen = false;
    private int lastMode = MODE_PROFILE;
    private int profilingMode = MODE_PROFILE;
    private int profilingState = PROFILING_INACTIVE;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public NetBeansProfiler() {
        boolean initFailed = false;

        final ProfilerEngineSettings sharedSettings = new ProfilerEngineSettings();

        try {
            String libsDir = IDEUtils.getLibsDir();

            if (libsDir == null) {
                throw new IOException(CANNOT_FIND_LIBS_MSG);
            }

            sharedSettings.initialize(libsDir);
            sharedSettings.setSeparateConsole(System.getProperty("org.netbeans.profiler.separateConsole") != null //NOI18N
                                              ); // change to true if something misbehaves and the TA VM does not start
            sharedSettings.setTargetWindowRemains(System.getProperty("org.netbeans.profiler.targetWindowRemains") != null //NOI18N
                                                  ); // use for testing when something misbehaves
        } catch (RuntimeException e) {
            ProfilerLogger.log(e);
            ProfilerDialogs.notify(new NotifyDescriptor.Message(e.getMessage(), NotifyDescriptor.ERROR_MESSAGE));
            initFailed = true;
        } catch (IOException e) {
            ErrorManager.getDefault()
                        .annotate(e, MessageFormat.format(ENGINE_INIT_FAILED_MSG, new Object[] { e.getLocalizedMessage() }));
            ErrorManager.getDefault().notify(ErrorManager.ERROR, e);
            initFailed = true;
        }

        // Initialize shared TargetAppRunner instance
        targetAppRunner = new TargetAppRunner(sharedSettings, new IDEAppStatusHandler(), ProfilingPointsManager.getDefault());
        targetAppRunner.addProfilingEventListener(new ProfilingEventListener() {
                public void targetAppStarted() {
                    if (calibrating) {
                        return;
                    }

                    changeStateTo(PROFILING_RUNNING);
                }

                public void targetAppStopped() {
                    if (calibrating) {
                        return;
                    }

                    changeStateTo(PROFILING_STOPPED);
                }

                public void targetAppSuspended() {
                    if (calibrating) {
                        return;
                    }

                    changeStateTo(PROFILING_PAUSED);
                }

                public void targetAppResumed() {
                    if (calibrating) {
                        return;
                    }

                    changeStateTo(PROFILING_RUNNING);
                }

                public void attachedToTarget() {
                    if (calibrating) {
                        return;
                    }

                    changeStateTo(PROFILING_RUNNING);
                }

                public void detachedFromTarget() {
                    if (calibrating) {
                        return;
                    }

                    monitor.stopDisplayingVM();
                    changeStateTo(PROFILING_INACTIVE);
                }

                public void targetVMTerminated() {
                    if (calibrating) {
                        return;
                    }

                    monitor.stopDisplayingVM();
                    changeStateTo(PROFILING_INACTIVE);
                }
            });

        if (!initFailed) {
            initialized = true;
        }
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static NetBeansProfiler getDefaultNB() {
        return (NetBeansProfiler) getDefault();
    }

    // ---------------------------------------------------------------------------
    public static boolean isInitialized() {
        return initialized;
    }

    public int getAgentState(String host, int port, int agentId) {
        if (profilingState /*!= PROFILING_INACTIVE*/ == PROFILING_RUNNING) {
            // profiling currently in progress, check port and id
            if (port == targetAppRunner.getProfilerEngineSettings().getPortNo()) {
                if (targetAppRunner.getProfilerClient().getCurrentAgentId() == agentId) {
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

    public Properties getCurrentProfilingProperties() {
        return actionSupport.getProperties();
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

    public String getLibsDir() {
        return IDEUtils.getLibsDir();
    }

    public int getPlatformArchitecture(String platformName) {
        JavaPlatform platform = IDEUtils.getJavaPlatformByName(platformName);

        if (platform == null) {
            return Platform.ARCH_32;
        } else {
            return IDEUtils.getPlatformArchitecture(platform);
        }
    }

    public String getPlatformJDKVersion(String platformName) {
        JavaPlatform platform = IDEUtils.getJavaPlatformByName(platformName);

        if (platform == null) {
            return null;
        } else {
            return IDEUtils.getPlatformJDKVersion(platform);
        }
    }

    public String getPlatformJavaFile(String platformName) {
        JavaPlatform platform = IDEUtils.getJavaPlatformByName(platformName);

        if (platform == null) {
            return null;
        } else {
            return IDEUtils.getPlatformJavaFile(platform);
        }
    }

    public int getProfilingMode() {
        return profilingMode;
    }

    public int getProfilingState() {
        return profilingState;
    }

    public TargetAppRunner getTargetAppRunner() {
        return targetAppRunner;
    }

    public ThreadsDataManager getThreadsManager() {
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

    public VMTelemetryModels getVMTelemetryModels() {
        if (vmTelemetryModels == null) {
            vmTelemetryModels = new VMTelemetryModels(getVMTelemetryManager());
        }

        return vmTelemetryModels;
    }

    public VMTelemetryDataManager getVMTelemetryManager() {
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
     * @return true if succesfully attached, false otherwise
     */
    public boolean attachToApp(final ProfilingSettings profilingSettings, final AttachSettings attachSettings) {
        profilingMode = MODE_ATTACH;

        final OutputParameter<Boolean> methodResults = new OutputParameter<Boolean>(Boolean.TRUE);

        new NBSwingWorker(false) {
                private ProgressHandle ph = null;

                @Override
                protected void doInBackground() {
                    if (getProfilingState() != PROFILING_INACTIVE) {
                        if (lastMode == MODE_ATTACH) {
                            detachFromApp(); // if attached, detach
                        } else if (targetAppRunner.targetJVMIsAlive()) {
                            targetAppRunner.terminateTargetJVM(); // otherwise kill current app if running
                        }
                    }

                    // remember profiling settings
                    lastProfilingSettings = profilingSettings;
                    lastSessionSettings = null;
                    lastMode = MODE_ATTACH;

                    // clear rerun
                    actionSupport.nullAll();
                    IDEUtils.runInEventDispatchThread(new Runnable() {
                            public void run() {
                                CallableSystemAction.get(RerunAction.class).updateAction();
                            }
                        });

                    final ProfilerEngineSettings sharedSettings = targetAppRunner.getProfilerEngineSettings();
                    profilingSettings.applySettings(sharedSettings); // can override the session settings
                    attachSettings.applySettings(sharedSettings);

                    //getThreadsManager().setSupportsSleepingStateMonitoring(
                    // Platform.supportsThreadSleepingStateMonitoring(sharedSettings.getTargetJDKVersionString()));
                    printDebugMsg("Profiler.attachToApp: ***************************************************", false); //NOI18N
                    printDebugMsg("profiling settings --------------------------------", false); //NOI18N
                    printDebugMsg(profilingSettings.debug(), false);
                    printDebugMsg("attach settings -----------------------------------", false); //NOI18N
                    printDebugMsg(attachSettings.debug(), false);
                    printDebugMsg("instrumentation filter ----------------------------", false); //NOI18N
                    printDebugMsg(sharedSettings.getInstrumentationFilter().debug(), false); //NOI18N
                    printDebugMsg("Profiler.attachToApp: ***************************************************", false); //NOI18N
                    flushDebugMsgs();

                    GestureSubmitter.logAttach(getProfiledProject(), attachSettings);
                    GestureSubmitter.logConfig(profilingSettings);

                    changeStateTo(PROFILING_STARTED);

                    cleanupBeforeProfiling(sharedSettings);

                    setThreadsMonitoringEnabled(profilingSettings.getThreadsMonitoringEnabled());

                    IDEUtils.runInEventDispatchThread(new Runnable() {
                            public void run() {
                                openWindowsOnProfilingStart();
                            }
                        });

                    if (attachSettings.isDirect()) { // Previously known as "attach on startup"
                                                     // The VM is already started with all necessary options and waiting for us to connect.
                                                     // Remote profiling case fits here too - it's distinguished in ProfilerClient using attachSettings.isRemote()
                                                     // perform the selected instrumentation - it will really start right after the target app starts

                        boolean success = false;

                        if (prepareInstrumentation(profilingSettings)) {
                            success = targetAppRunner.initiateSession(1, false) && targetAppRunner.attachToTargetVMOnStartup();
                        }

                        if (!success) {
                            changeStateTo(PROFILING_INACTIVE);
                            // change state back to inactive and fire, return false
                            methodResults.setValue(false);

                            return;
                        }
                    } else if (attachSettings.isDynamic16()) {
                        String jar = getLibsDir() + "/jfluid-server-15.jar"; // NOI18N
                        String pid = String.valueOf(attachSettings.getPid());
                        String options = String.valueOf(attachSettings.getPort());
                        boolean success = false;

                        try {
                            loadAgentIntTargetJVM(jar, options, pid);

                            if (prepareInstrumentation(profilingSettings)) {
                                success = targetAppRunner.initiateSession(2, false) && targetAppRunner.attachToTargetVM();
                            }
                        } catch (Exception ex) {
                            displayError(ex.getMessage());
                            ProfilerLogger.log(ex);
                        }

                        if (!success) {
                            changeStateTo(PROFILING_INACTIVE);
                            // change state back to inactive and fire, return false
                            methodResults.setValue(false);

                            return;
                        }
                    } else {
                        throw new IllegalArgumentException("Invalid settings " + attachSettings); // NOI18N
                    }

                    if (targetAppRunner.targetAppIsRunning()) {
                        getThreadsManager()
                            .setSupportsSleepingStateMonitoring(Platform.supportsThreadSleepingStateMonitoring(sharedSettings.getTargetJDKVersionString()));
                        monitor.monitorVM(targetAppRunner);

                        if (threadsMonitoringEnabled) {
                            IDEUtils.runInEventDispatchThread(new Runnable() {
                                    public void run() {
                                        ThreadsWindow.getDefault().showThreads();
                                    }
                                });
                        }

                        methodResults.setValue(true);

                        return;
                    } else {
                        methodResults.setValue(false);

                        return;
                    }
                }

                private void loadAgentIntTargetJVM(final String jar, final String options, final String pid)
                                            throws SecurityException, IllegalArgumentException, IllegalAccessException,
                                                   NoSuchMethodException, ClassNotFoundException, InvocationTargetException {
                    //VirtualMachine virtualMachine = VirtualMachine attach(String id);
                    Class vmClass = Class.forName("com.sun.tools.attach.VirtualMachine"); // NOI18N
                    Method attachMethod = vmClass.getMethod("attach", String.class); // NOI18N
                    Object virtualMachine = attachMethod.invoke(null, pid);

                    // virtualMachine.loadAgent(jar,options);
                    Method loadAgentMethod = vmClass.getMethod("loadAgent", String.class, String.class); // NOI18N
                    loadAgentMethod.invoke(virtualMachine, jar, options);
                }

                @Override
                protected void nonResponding() {
                    ph = ProgressHandleFactory.createHandle(NbBundle.getMessage(this.getClass(),
                                                                                "NetBeansProfiler_StartingSession")); // NOI18N
                    ph.start();
                }

                @Override
                protected void done() {
                    if (ph != null) {
                        ph.finish();
                        ph = null;
                    }
                }
            }.execute();

        return methodResults.getValue();
    }

    // -- NetBeansProfiler-only public methods -----------------------------------------------------------------------------
    public void checkAndUpdateState() {
        // TODO: check & refactor to remove this
        final boolean targetVMAlive = targetAppRunner.targetJVMIsAlive();

        if (!targetVMAlive) {
            changeStateTo(PROFILING_INACTIVE);

            return;
        }

        final boolean running = targetAppRunner.targetAppIsRunning();

        if (!running) {
            changeStateTo(PROFILING_STOPPED);

            return;
        }

        final boolean suspended = targetAppRunner.targetAppSuspended();

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
     * @return true if connected succesfully, false otherwise
     */
    public boolean connectToStartedApp(final ProfilingSettings profilingSettings, final SessionSettings sessionSettings) {
        profilingMode = MODE_PROFILE;

        lastProfilingSettings = profilingSettings;
        lastSessionSettings = sessionSettings;
        lastMode = MODE_PROFILE;

        final OutputParameter<Boolean> methodResult = new OutputParameter<Boolean>(Boolean.TRUE);

        new NBSwingWorker(false) {
                private ProgressHandle ph = null;

                @Override
                protected void doInBackground() {
                    if (targetAppRunner.targetJVMIsAlive()) {
                        targetAppRunner.terminateTargetJVM();
                    }

                    final ProfilerEngineSettings sharedSettings = targetAppRunner.getProfilerEngineSettings();

                    sessionSettings.applySettings(sharedSettings);
                    profilingSettings.applySettings(sharedSettings); // can override the session settings
                    sharedSettings.setRemoteHost(""); // NOI18N // clear remote profiling host

                    //getThreadsManager().setSupportsSleepingStateMonitoring(
                    // Platform.supportsThreadSleepingStateMonitoring(sharedSettings.getTargetJDKVersionString()));
                    printDebugMsg("Profiler.connectToStartedApp: **************************************************", false); //NOI18N
                    printDebugMsg("profiling settings -------------------------------", false); //NOI18N
                    printDebugMsg(profilingSettings.debug(), false);
                    printDebugMsg("session settings ---------------------------------", false); //NOI18N
                    printDebugMsg(sessionSettings.debug(), false);
                    printDebugMsg("instrumentation filter ---------------------------", false); // NOI18N
                    printDebugMsg(sharedSettings.getInstrumentationFilter().debug(), false); //NOI18N
                    printDebugMsg("Profiler.connectToStartedApp: **************************************************", false); //NOI18N
                    flushDebugMsgs();

                    GestureSubmitter.logProfileApp(getProfiledProject(), sessionSettings);
                    GestureSubmitter.logConfig(profilingSettings);

                    changeStateTo(PROFILING_STARTED);

                    cleanupBeforeProfiling(sharedSettings);

                    setThreadsMonitoringEnabled(profilingSettings.getThreadsMonitoringEnabled());

                    IDEUtils.runInEventDispatchThread(new Runnable() {
                            public void run() {
                                openWindowsOnProfilingStart();
                            }
                        });

                    if (!CalibrationDataFileIO.validateCalibrationInput(sessionSettings.getJavaVersionString(),
                                                                            sessionSettings.getJavaExecutable())) {
                        displayErrorWithDetailsAndWait(CALIBRATION_MISSING_SHORT_MESSAGE, CALIBRATION_MISSING_MESSAGE);
                        changeStateTo(PROFILING_INACTIVE);
                        methodResult.setValue(Boolean.FALSE);

                        return; // failed, cannot proceed
                    }

                    // perform the selected instrumentation
                    if (!prepareInstrumentation(profilingSettings)) {
                        methodResult.setValue(Boolean.FALSE);

                        return; // failed, cannot proceed
                    }

                    if (!targetAppRunner.initiateSession(0, false) || !targetAppRunner.connectToStartedVMAndStartTA()) {
                        changeStateTo(PROFILING_INACTIVE);
                        methodResult.setValue(Boolean.FALSE);

                        return; // failed, cannot proceed
                    }

                    if (targetAppRunner.targetAppIsRunning()) {
                        getThreadsManager()
                            .setSupportsSleepingStateMonitoring(Platform.supportsThreadSleepingStateMonitoring(sharedSettings.getTargetJDKVersionString()));
                        IDEUtils.runInEventDispatchThread(new Runnable() {
                                public void run() {
                                    monitor.monitorVM(targetAppRunner);
                                }
                            });
                        methodResult.setValue(Boolean.TRUE);

                        return;
                    } else {
                        // TODO: notify the user???
                        changeStateTo(PROFILING_INACTIVE);
                        methodResult.setValue(Boolean.FALSE);

                        return; // failed, cannot proceed
                    }
                }

                @Override
                protected void nonResponding() {
                    ph = ProgressHandleFactory.createHandle(NbBundle.getMessage(this.getClass(),
                                                                                "NetBeansProfiler_StartingSession")); // NOI18N
                    ph.start();
                }

                @Override
                protected void done() {
                    if (ph != null) {
                        ph.finish();
                        ph = null;
                    }
                }
            }.execute();

        return methodResult.getValue();
    }

    public void detachFromApp() {
        setTransitionState();

        if (targetAppRunner.getProfilingSessionStatus().currentInstrType != CommonConstants.INSTR_NONE) {
            //      if (LiveResultsWindow.hasDefault()) LiveResultsWindow.getDefault().reset(); // see issue http://www.netbeans.org/issues/show_bug.cgi?id=68213
            try {
                targetAppRunner.getProfilerClient().removeAllInstrumentation(false); // remove only the server side instrumentation
            } catch (InstrumentationException e) {
                displayError(e.getMessage());
            }
        }

        targetAppRunner.detachFromTargetJVM();

        //    targetAppRunner.getProfilerClient().resetClientData();
        // TODO reset all profilingresultslisteners
        //    CPUCallGraphBuilder.resetCollectors();
        //    ResultsManager.getDefault().reset();
    }

    public void displayError(final String message) {
        ProfilerDialogs.notify(new NotifyDescriptor.Message(message, NotifyDescriptor.ERROR_MESSAGE));
    }

    public void displayErrorAndWait(final String message) {
        ProfilerDialogs.notify(new NotifyDescriptor.Message(message, NotifyDescriptor.ERROR_MESSAGE));
    }

    public void displayErrorWithDetailsAndWait(final String shortMsg, final String detailsMsg) {
        ProfilerDialogs.notify(new ProfilerDialogs.MessageWithDetails(shortMsg, detailsMsg, NotifyDescriptor.ERROR_MESSAGE, false));
    }

    public void displayInfo(final String message) {
        ProfilerDialogs.notify(new NotifyDescriptor.Message(message, NotifyDescriptor.INFORMATION_MESSAGE));
    }

    public void displayInfoAndWait(final String message) {
        ProfilerDialogs.notify(new NotifyDescriptor.Message(message, NotifyDescriptor.INFORMATION_MESSAGE));
    }

    public void displayInfoWithDetailsAndWait(final String shortMsg, final String detailsMsg) {
        ProfilerDialogs.notify(new ProfilerDialogs.MessageWithDetails(shortMsg, detailsMsg, NotifyDescriptor.INFORMATION_MESSAGE,
                                                                      false));
    }

    public void displayWarning(final String message) {
        ProfilerDialogs.notify(new NotifyDescriptor.Message(message, NotifyDescriptor.WARNING_MESSAGE));
    }

    public void displayWarningAndWait(final String message) {
        ProfilerDialogs.notify(new NotifyDescriptor.Message(message, NotifyDescriptor.WARNING_MESSAGE));
    }

    public void instrumentSelectedRoots(ClientUtils.SourceCodeSelection[] rootMethods)
                                 throws ClassNotFoundException, InstrumentationException, BadLocationException, IOException,
                                        ClassFormatError, ClientUtils.TargetAppOrVMTerminated {
        final ProfilerClient client = targetAppRunner.getProfilerClient();

        if (rootMethods.length == 0) {
            ClientUtils.SourceCodeSelection selection = new ClientUtils.SourceCodeSelection(1); // spawned threads recursively
            rootMethods = new ClientUtils.SourceCodeSelection[] { selection };
        }

        // Start the recursive code instrumentation
        client.initiateRecursiveCPUProfInstrumentation(rootMethods);
    }

    public static AttachSettings loadAttachSettings(Project project)
                                             throws IOException {
        FileObject folder = IDEUtils.getProjectSettingsFolder(project, false);

        if (folder == null) {
            return null;
        }

        FileObject attachSettingsFile = folder.getFileObject(ATTACH_SETTINGS_FILENAME, "xml"); //NOI18N

        if (attachSettingsFile == null) {
            return null;
        }

        final InputStream fis = attachSettingsFile.getInputStream();
        final BufferedInputStream bis = new BufferedInputStream(fis);

        try {
            final Properties props = new Properties();
            props.loadFromXML(bis);

            AttachSettings as = new AttachSettings();
            as.load(props);

            return as;
        } finally {
            bis.close();
        }
    }

    public void log(int severity, final String message) {
        switch (severity) {
            case Profiler.INFORMATIONAL:
                severity = ErrorManager.INFORMATIONAL;

                break;
            case Profiler.WARNING:
                severity = ErrorManager.WARNING;

                break;
            case Profiler.EXCEPTION:
                severity = ErrorManager.EXCEPTION;

                break;
            case Profiler.ERROR:
                severity = ErrorManager.ERROR;

                break;
            default:
                severity = ErrorManager.UNKNOWN;

                break;
        }

        if (profilerErrorManager.isLoggable(severity)) {
            profilerErrorManager.log(severity, message);
        }
    }

    // ---------------------------------------------------------------------------
    public void modifyCurrentProfiling(final ProfilingSettings profilingSettings) {
        lastProfilingSettings = profilingSettings;

        if (actionSupport.getProperties()!=null) {
            lastProfilingSettings.store(actionSupport.getProperties()); // Fix for http://www.netbeans.org/issues/show_bug.cgi?id=95651, update settings for ReRun
        }

        if (!targetAppRunner.targetJVMIsAlive()) {
            return;
        }

        final ProfilerEngineSettings sharedSettings = targetAppRunner.getProfilerEngineSettings();
        profilingSettings.applySettings(sharedSettings);

        printDebugMsg("Profiler.modifyCurrentProfiling: ***************************************************", false); //NOI18N
        printDebugMsg("profiling settings --------------------------------", false); //NOI18N
        printDebugMsg(profilingSettings.debug(), false);
        printDebugMsg("instrumentation filter ----------------------------", false); // NOI18N
        printDebugMsg(sharedSettings.getInstrumentationFilter().debug(), false); //NOI18N
        printDebugMsg("Profiler.modifyCurrentProfiling: ***************************************************", false); //NOI18N
        flushDebugMsgs();

        GestureSubmitter.logConfig(profilingSettings);

        setThreadsMonitoringEnabled(profilingSettings.getThreadsMonitoringEnabled());

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (LiveResultsWindow.hasDefault())
                    LiveResultsWindow.getDefault().handleCleanupBeforeProfiling();
            }
        });

        IDEUtils.runInProfilerRequestProcessor(new Runnable() {
                public void run() {
                    changeStateTo(PROFILING_IN_TRANSITION);
                    targetAppRunner.getAppStatusHandler().pauseLiveUpdates();
                    ProfilingPointsManager.getDefault().reset();
                    ResultsManager.getDefault().reset();

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                    }

                    waitDialog = targetAppRunner.getAppStatusHandler()
                                                .getAsyncDialogInstance(MODIFYING_INSTRUMENTATION_MSG, true, false);

                    if (waitDialog != null) {
                        final AppStatusHandler.AsyncDialog dialog = waitDialog;

                        if (EventQueue.isDispatchThread()) {
                            dialog.display();
                        } else {
                            EventQueue.invokeLater(new Runnable() {
                                    public void run() {
                                        dialog.display();
                                    }
                                });
                        }
                    }

                    try {
                        prepareInstrumentation(profilingSettings);
                        changeStateTo(PROFILING_RUNNING);
                    } finally {
                        if (waitDialog != null) {
                            final AppStatusHandler.AsyncDialog dialog = waitDialog;

                            if (EventQueue.isDispatchThread()) {
                                dialog.close();
                            } else {
                                EventQueue.invokeLater(new Runnable() {
                                        public void run() {
                                            dialog.close();
                                        }
                                    });
                            }
                        }

                        targetAppRunner.getAppStatusHandler().resumeLiveUpdates();
                    }
                }
            });
    }

    public void notifyException(final int severity, final Exception e) {
        switch (severity) {
            case Profiler.INFORMATIONAL:
                profilerErrorManager.notify(ErrorManager.INFORMATIONAL, e);

                return;
            case Profiler.WARNING:
                profilerErrorManager.notify(ErrorManager.WARNING, e);

                return;
            case Profiler.EXCEPTION:
                profilerErrorManager.notify(ErrorManager.EXCEPTION, e);

                return;
            case Profiler.ERROR:
                profilerErrorManager.notify(ErrorManager.ERROR, e);

                return;
            default:
                profilerErrorManager.notify(ErrorManager.UNKNOWN, e);

                return;
        }
    }

    public void openJavaSource(String className, String methodName, String methodSig) {
        openJavaSource(getProfiledProject(), className, methodName, methodSig);
    }

    public void openJavaSource(final Project project, final String className, final String methodName, final String methodSig) {
        GoToSourceHelper.openSource(project, new JavaSourceLocation(className, methodName, methodSig));
    }

    public boolean processesProfilingPoints() {
        return processesProfilingPoints;
    }

    /**
     * Starts the TA described via sessionSettings, using profiling mode specified in profilingSettings.
     *
     * @param profilingSettings Settings to use for profiling
     * @param sessionSettings   Session settings for profiling
     * @return true if target app succesfully started, false otherwise
     */
    public boolean profileClass(final ProfilingSettings profilingSettings, final SessionSettings sessionSettings) {
        //final long time = System.currentTimeMillis();
        profilingMode = MODE_PROFILE;

        lastProfilingSettings = profilingSettings;
        lastSessionSettings = sessionSettings;
        lastMode = MODE_PROFILE;

        if (targetAppRunner.targetJVMIsAlive()) {
            targetAppRunner.terminateTargetJVM();
        }

        final ProfilerEngineSettings sharedSettings = targetAppRunner.getProfilerEngineSettings();

        sessionSettings.applySettings(sharedSettings);
        profilingSettings.applySettings(sharedSettings); // can override the session settings
        sharedSettings.setRemoteHost(""); // NOI18N // clear remote profiling host

        //getThreadsManager().setSupportsSleepingStateMonitoring(
        // Platform.supportsThreadSleepingStateMonitoring(sharedSettings.getTargetJDKVersionString()));
        printDebugMsg("Profiler.profileClass: **************************************************", false); //NOI18N
        printDebugMsg("Profiler.profileClass: profiling settings -------------------------------", false); //NOI18N
        printDebugMsg(profilingSettings.debug(), false);
        printDebugMsg("Profiler.profileClass: session settings ---------------------------------", false); //NOI18N
        printDebugMsg(sessionSettings.debug(), false);
        printDebugMsg("Profiler.profileClass: **************************************************", false); //NOI18N
        printDebugMsg("Instrumentation filter:\n" + sharedSettings.getInstrumentationFilter().debug(), false); //NOI18N
        flushDebugMsgs();

        Project owningProject = FileOwnerQuery.getOwner(getProfiledSingleFile());
        GestureSubmitter.logProfileClass(owningProject, sessionSettings);
        GestureSubmitter.logConfig(profilingSettings);

        changeStateTo(PROFILING_STARTED);

        //    System.err.println("--------------------------------------------- 2: "+ (System.currentTimeMillis() - time));
        cleanupBeforeProfiling(sharedSettings);

        setThreadsMonitoringEnabled(profilingSettings.getThreadsMonitoringEnabled());
        //    System.err.println("------------------------------------------ 3: "+ (System.currentTimeMillis() - time));
        openWindowsOnProfilingStart();

        //    System.err.println("------------------------------------- 4: "+ (System.currentTimeMillis() - time));
        final Window mainWindow = WindowManager.getDefault().getMainWindow();

        // This call reduces the speedup for class instrumentation on the 2nd and further runs that we could otherwise
        // have, but guarantees that if any classes have been recompiled in between runs, their most up-to-date copies will
        // be used.
        IDEUtils.runInProfilerRequestProcessor(new Runnable() {
                public void run() {
                    final Cursor cursor = mainWindow.getCursor();
                    mainWindow.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                    try {
                        if (!runCalibration(true, sessionSettings.getJavaExecutable(), sessionSettings.getJavaVersionString(),
                                                sessionSettings.getSystemArchitecture())) {
                            displayError(CALIBRATION_FAILED_MESSAGE);
                            changeStateTo(PROFILING_INACTIVE);

                            return; // failed, cannot proceed
                        }

                        // System.err.println("-----------------------------------5: "+ (System.currentTimeMillis() - time));
                        // perform the selected instrumentation
                        boolean success = prepareInstrumentation(profilingSettings);

                        // and run the target application
                        //        System.err.println("---------------------------- 6: "+ (System.currentTimeMillis() - time));
                        success = success && targetAppRunner.startTargetVM() && targetAppRunner.initiateSession(0, false)
                                  && targetAppRunner.connectToStartedVMAndStartTA();

                        if (!success) {
                            changeStateTo(PROFILING_INACTIVE);

                            return;
                        }

                        // System.err.println("---------------------------- 7: "+ (System.currentTimeMillis() - time));
                        if (targetAppRunner.targetAppIsRunning()) {
                            getThreadsManager()
                                .setSupportsSleepingStateMonitoring(Platform.supportsThreadSleepingStateMonitoring(sharedSettings.getTargetJDKVersionString()));
                            IDEUtils.runInEventDispatchThread(new Runnable() {
                                    public void run() {
                                        // System.err.println("------------ 8: "+ (System.currentTimeMillis() - time));
                                        monitor.monitorVM(targetAppRunner);

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

    public boolean rerunAvailable() {
        return actionSupport.isActionAvailable();
    }

    public boolean modifyAvailable() {
        return getProfilingMode()==MODE_ATTACH||actionSupport.isActionAvailable();
    }

    public void rerunLastProfiling() {
        if (actionSupport.getTarget()!=null) {
            doRunTarget(actionSupport.getScript(), actionSupport.getTarget(), actionSupport.getProperties());
        }
    }

    public boolean runCalibration(boolean checkForSaved, String jvmExecutable, String jdkString, int architecture) {
        calibrating = true;

        ProfilerEngineSettings pes = targetAppRunner.getProfilerEngineSettings();

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
            result = targetAppRunner.readSavedCalibrationData();

            if (!result) {
                displayInfoAndWait(INITIAL_CALIBRATION_MSG);
                result = targetAppRunner.calibrateInstrumentationCode();
            }
        } else {
            result = targetAppRunner.calibrateInstrumentationCode();
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

    public static void saveAttachSettings(Project project, AttachSettings as) {
        FileLock lock = null;

        try {
            final FileObject folder = IDEUtils.getProjectSettingsFolder(project, true);
            FileObject fo = folder.getFileObject(ATTACH_SETTINGS_FILENAME, "xml"); //NOI18N

            if (fo == null) {
                fo = folder.createData(ATTACH_SETTINGS_FILENAME, "xml"); //NOI18N
            }

            lock = fo.lock();

            final BufferedOutputStream bos = new BufferedOutputStream(fo.getOutputStream(lock));
            final Properties globalProps = new Properties();
            try {
                as.store(globalProps);
                globalProps.storeToXML(bos, ""); //NOI18N
            } finally {
                if (bos != null) {
                    try {
                        bos.close();
                    } catch (IOException ex) {
                        // ignore
                    }
                }
            }
        } catch (Exception e) {
            ProfilerLogger.log(e);
            ProfilerDialogs.notify(new NotifyDescriptor.Message(MessageFormat.format(ERROR_SAVING_ATTACH_SETTINGS_MESSAGE,
                                                                                     new Object[] { e.getMessage() }),
                                                                NotifyDescriptor.ERROR_MESSAGE));
        } finally {
            if (lock != null) {
                lock.releaseLock();
            }
        }
    }

    public void setProfiledProject(Project project, FileObject singleFile) {
        profiledProject = project;
        profiledSingleFile = singleFile;

        ProfilerControlPanel2.getDefault().setProfiledProject(project);
    }

    public Project getProfiledProject() {
        return profiledProject;
    }

    public FileObject getProfiledSingleFile() {
        return profiledSingleFile;
    }

    public void setSilent(boolean value) {
        silent = value;
    }

    @Override
    public boolean prepareInstrumentation(ProfilingSettings profilingSettings) {
        final boolean retValue;
        teardownDispatcher();
        setupDispatcher(profilingSettings);

        ClientUtils.SourceCodeSelection[] marks = MarkingEngine.getDefault().getMarkerMethods();
        profilingSettings.setInstrumentationMarkerMethods(marks);

        retValue = super.prepareInstrumentation(profilingSettings);

        return retValue;
    }

    public void runTarget(FileObject buildScriptFO, String target, Properties props) {
        actionSupport.setAll(buildScriptFO, target, props);

        SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    ((RerunAction) RerunAction.get(RerunAction.class)).updateAction();
                }
            });

        doRunTarget(buildScriptFO, target, props);
    }

    // TODO [ian] - perform saving of global settings differently
    public void saveGlobalFilters() {
        // 1. save global filters
        FileLock lock = null;

        try {
            final FileObject folder = IDEUtils.getSettingsFolder(true);
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
            ProfilerDialogs.notify(new NotifyDescriptor.Message(MessageFormat.format(ERROR_SAVING_PROFILING_SETTINGS_MESSAGE,
                                                                                     new Object[] { e.getMessage() }),
                                                                NotifyDescriptor.ERROR_MESSAGE));
        } finally {
            if (lock != null) {
                lock.releaseLock();
            }
        }

        // 2. save defined Filter Sets
        lock = null;

        try {
            final FileObject folder = IDEUtils.getSettingsFolder(true);
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
            ProfilerDialogs.notify(new NotifyDescriptor.Message(MessageFormat.format(ERROR_SAVING_FILTER_SETS_MESSAGE,
                                                                                     new Object[] { e.getMessage() }),
                                                                NotifyDescriptor.ERROR_MESSAGE));
        } finally {
            if (lock != null) {
                lock.releaseLock();
            }
        }
    }

    public void shutdown() {
        monitor.stopUpdateThread();
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
        return (profilingState == PROFILING_RUNNING) && (port == targetAppRunner.getProfilerEngineSettings().getPortNo());
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
    }

    private void cleanupAfterProfiling() {
        stopLoadGenerator();
        teardownDispatcher();
        MarkingEngine.getDefault().deconfigure();
        ClassRepository.cleanup();
    }

    private void cleanupBeforeProfiling(ProfilerEngineSettings sharedSettings) {
        final Profiler profiler = Profiler.getDefault();

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                profiler.getThreadsManager().reset();
                profiler.getVMTelemetryManager().reset();
                if (LiveResultsWindow.hasDefault())
                    LiveResultsWindow.getDefault().handleCleanupBeforeProfiling();
            }
        });
        ProfilingPointsManager.getDefault().reset();
        ResultsManager.getDefault().reset();

        ClassRepository.clearCache();
        ClassRepository.initClassPaths(sharedSettings.getWorkingDir(), sharedSettings.getVMClassPaths());
    }

    private void closeWaitDialog() {
        if (waitDialogOpen) {
            waitDialog.close();
            waitDialogOpen = false;
        }
    }

    private void displayWaitDialog() {
        waitDialogOpen = true;
        waitDialog.display();
    }

    private void displayWarningAboutEntireAppProfiling() {
        displayWarning(ENTIRE_APPLICATION_PROFILING_WARNING);
    }

    private void flushDebugMsgs() {
        String msg = logMsgs.toString();

        if (LOGGER.isLoggable(Level.CONFIG) && !silent) {
            LOGGER.config(msg);
        } else { // just log
            profilerErrorManager.log(msg);
        }
    }

    // -- Package-Private stuff --------------------------------------------------------------------------------------------
    private void loadGlobalFilters() {
        try {
            FileObject folder = IDEUtils.getSettingsFolder(false);
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
            ProfilerDialogs.notify(new NotifyDescriptor.Message(MessageFormat.format(ERROR_LOADING_PROFILING_SETTINGS_MESSAGE,
                                                                                     new Object[] { e.getMessage() }),
                                                                NotifyDescriptor.ERROR_MESSAGE));
        }
    }

    // -- Private implementation -------------------------------------------------------------------------------------------
    private void openWindowsOnProfilingStart() {
        int telemetryBehavior = ideSettings.getTelemetryOverviewBehavior();
        int threadsBehavior = ideSettings.getThreadsViewBehavior();

        boolean threadsEnabled = lastProfilingSettings.getThreadsMonitoringEnabled();
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

        // 3. Live Results
        if ((ideSettings.getDisplayLiveResultsCPU()
                && ((type == ProfilingSettings.PROFILE_CPU_ENTIRE) || (type == ProfilingSettings.PROFILE_CPU_PART)))
                || (ideSettings.getDisplayLiveResultsFragment() && (type == ProfilingSettings.PROFILE_CPU_STOPWATCH))
                || (ideSettings.getDisplayLiveResultsMemory()
                       && ((type == ProfilingSettings.PROFILE_MEMORY_ALLOCATIONS)
                              || (type == ProfilingSettings.PROFILE_MEMORY_LIVENESS)))) {
            LiveResultsWindow.getDefault().open();
            LiveResultsWindow.getDefault().requestVisible();
        }

        // 4. Control Panel displayed always, and getting focus
        final ProfilerControlPanel2 controlPanel2 = ProfilerControlPanel2.getDefault();
        controlPanel2.open();
        controlPanel2.requestActive();
    }

    private void printDebugMsg(String msg) {
        printDebugMsg(msg, true);
    }

    private void printDebugMsg(String msg, boolean flush) {
        logMsgs.append(msg).append('\n');

        if (flush) {
            flushDebugMsgs();
        }
    }

    private void setupDispatcher(ProfilingSettings profilingSettings) {
        synchronized (setupLock) {
            final Project project = ((NetBeansProfiler) Profiler.getDefault()).getProfiledProject();

            // configure call-context-tree dispatching infrastructure
            CCTProvider cctProvider = null;
            Collection<?extends CCTProvider.Listener> cctListeners = null;

            switch (profilingSettings.getProfilingType()) {
                case ProfilingSettings.PROFILE_CPU_ENTIRE:
                case ProfilingSettings.PROFILE_CPU_PART:
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
                        LOGGER.finest("Adding listener " + cctListener.getClass().getName() + " to the provider "
                                      + cctProvider.getClass().getName());
                    }

                    cctProvider.addListener(cctListener);
                }
            } else {
                if (LOGGER.isLoggable(Level.WARNING)) {
                    if (cctProvider == null) {
                        LOGGER.warning("Could not find a CCT provider in lookup!");
                    } else if ((cctListeners == null) || (cctListeners.size() == 0)) {
                        LOGGER.warning("Could not find listeners in lookup!");
                    }
                }
            }

            // done
            StatisticalModuleContainer statModulesContainer = Lookup.getDefault().lookup(StatisticalModuleContainer.class);
            Collection<?extends StatisticalModule> modules = Lookup.getDefault().lookupAll(StatisticalModule.class);

            if ((statModulesContainer != null) && (modules != null)) {
                for (StatisticalModule module : modules) {
                    /* Using workaround here
                     * For some reasons when the lookupAll is called the second time it returns ALL subtypes as well
                     * So I must check for the proper type and check for project support eventually
                     */
                    if (module instanceof ProjectAwareStatisticalModule) {
                        if (((ProjectAwareStatisticalModule) module).supportsProject(project)) {
                            statModulesContainer.addModule(module);
                        }
                    } else {
                        statModulesContainer.addModule(module);
                    }
                }
            }

            Collection<?extends ProjectAwareStatisticalModule> pmodules = Lookup.getDefault()
                                                                                .lookupAll(ProjectAwareStatisticalModule.class);

            if (pmodules != null) {
                for (ProjectAwareStatisticalModule module : pmodules) {
                    if (module.supportsProject(project)) {
                        statModulesContainer.addModule(module);
                    }
                }
            }

            ProfilerClient client = getTargetAppRunner().getProfilerClient();

            CCTResultsFilter filter = Lookup.getDefault().lookup(CCTResultsFilter.class);
            filter.setEvaluators(Lookup.getDefault().lookupAll(CCTResultsFilter.EvaluatorProvider.class));

            if (filter != null) {
                filter.reset(); // clean up the filter before reusing it
            }

            // init context aware instances
            FlatProfileBuilder fpb = Lookup.getDefault().lookup(FlatProfileBuilder.class);
            TimeCollector tc = Lookup.getDefault().lookup(TimeCollector.class);
            fpb.setContext(client, tc, filter);
            
//            Collection<?extends ContextAware> contextAwareInstances = Lookup.getDefault().lookupAll(ContextAware.class);
//
//            for (ContextAware instance : contextAwareInstances) {
//                instance.setContext(client);
//            }

//            boolean isMarksEnabled = (profilingSettings.getProfilingType() == ProfilingSettings.PROFILE_CPU_ENTIRE)
//                                     || (profilingSettings.getProfilingType() == ProfilingSettings.PROFILE_CPU_PART);
//
////            ProjectTypeProfiler ptp = org.netbeans.modules.profiler.utils.ProjectUtilities.getProjectTypeProfiler(project);
//            Categorization ctg = project != null ? project.getLookup().lookup(Categorization.class) : null;
//
//            isMarksEnabled &= (ctg != null);
//
//            if (isMarksEnabled) {
//                ctg.reset();
//                MarkingEngine.getDefault().configure(ctg.getMappings());
//            } else {
//                MarkingEngine.getDefault().deconfigure();
//            }

            Collection listeners = null;

            if ((profilingSettings.getProfilingType() == ProfilingSettings.PROFILE_CPU_PART)
                    || (profilingSettings.getProfilingType() == ProfilingSettings.PROFILE_CPU_ENTIRE)) {
                listeners = Lookup.getDefault().lookupAll(CPUProfilingResultListener.class);

                for (Iterator iter = listeners.iterator(); iter.hasNext();) {
                    CPUProfilingResultListener listener = (CPUProfilingResultListener) iter.next();
                    ProfilingResultsDispatcher.getDefault().addListener(listener);
                    listener.startup(targetAppRunner.getProfilerClient());
                }
            } else if ((profilingSettings.getProfilingType() == ProfilingSettings.PROFILE_MEMORY_ALLOCATIONS)
                           || (profilingSettings.getProfilingType() == ProfilingSettings.PROFILE_MEMORY_LIVENESS)) {
                listeners = Lookup.getDefault().lookupAll(MemoryProfilingResultsListener.class);

                for (Iterator iter = listeners.iterator(); iter.hasNext();) {
                    MemoryProfilingResultsListener listener = (MemoryProfilingResultsListener) iter.next();
                    ProfilingResultsDispatcher.getDefault().addListener(listener);
                    listener.startup(targetAppRunner.getProfilerClient());
                }
            }

            if (profilingSettings.useProfilingPoints() && (getProfiledProject() != null)) {
                RuntimeProfilingPoint[] points = ProfilingPointsManager.getDefault()
                                                                       .createCodeProfilingConfiguration(getProfiledProject(),
                                                                                                         profilingSettings);
                processesProfilingPoints = points.length > 0;
                targetAppRunner.getProfilerEngineSettings().setRuntimeProfilingPoints(points);

                //      targetAppRunner.getProfilingSessionStatus().startProfilingPointsActive = profilingSettings.useProfilingPoints();
            } else {
                RuntimeProfilingPoint[] points = new RuntimeProfilingPoint[0];
                processesProfilingPoints = false;
                targetAppRunner.getProfilerEngineSettings().setRuntimeProfilingPoints(points);
            }

            // TODO: should be moved to openWindowsOnProfilingStart()
            if (processesProfilingPoints) {
                SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            if (!ProfilingPointsWindow.getDefault().isOpened()) {
                                ProfilingPointsWindow.getDefault().open();
                                ProfilingPointsWindow.getDefault().requestVisible();
                            }
                        }
                    });
            }

            ProfilingResultsDispatcher.getDefault().startup(client);
        }
    }

    // Used for killing an agent which could cause a collision on port
    // Returns true if TERMINATE_TARGET_JVM was invoked on agent (not necessarily killed!), false if the agent is already profiling (port is used)
    private boolean shutdownAgent(String host, int port) {
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

    private void stopLoadGenerator() {
        Properties profilingProperties = NetBeansProfiler.getDefaultNB().getCurrentProfilingProperties();

        if (profilingProperties != null) {
            LoadGenPlugin plugin = Lookup.getDefault().lookup(LoadGenPlugin.class);

            if (plugin != null) {
                String scriptPath = profilingProperties.getProperty("profiler.loadgen.path"); // TODO factor out the "profiler.loadgen.path" constant; also used ing J2EEProjectTypeProfiler

                if (scriptPath != null) {
                    plugin.stop(scriptPath);
                }
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
            StatisticalModuleContainer statModulesContainer = Lookup.getDefault().lookup(StatisticalModuleContainer.class);

            if (statModulesContainer != null) {
                statModulesContainer.removeAllModules();
            }

            // deconfigure the profiler client
            ProfilerClient client = getTargetAppRunner().getProfilerClient();
            client.registerFlatProfileProvider(null);

//            // deconfigure the marking engine
//            MarkingEngine.getDefault().deconfigure();
        }
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

    /**
     * Runs an target in Ant script with properties context.
     *
     * @param buildScript The build script to run the target from
     * @param target The name of target to run
     * @param props The properties context to run the task in
     * @return ExecutorTask to track the running Ant process
     */
    public static ExecutorTask doRunTarget(final FileObject buildScript, final String target, final Properties props) {
        try {
            String oomeenabled = props.getProperty(HeapDumpWatch.OOME_PROTECTION_ENABLED_KEY);

            if ((oomeenabled != null) && oomeenabled.equals("yes")) { // NOI18N
                HeapDumpWatch.getDefault().monitor(props.getProperty(HeapDumpWatch.OOME_PROTECTION_DUMPPATH_KEY));
            }

            return ActionUtils.runTarget(buildScript, new String[] { target }, props);
        } catch (IOException e) {
            Profiler.getDefault().notifyException(Profiler.EXCEPTION, e);
        }

        return null;
    }
}
