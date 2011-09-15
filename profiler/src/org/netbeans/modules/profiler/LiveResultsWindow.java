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

import org.netbeans.lib.profiler.ProfilerClient;
import org.netbeans.lib.profiler.ProfilerEngineSettings;
import org.netbeans.lib.profiler.ProfilerLogger;
import org.netbeans.lib.profiler.TargetAppRunner;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.lib.profiler.common.event.ProfilingStateEvent;
import org.netbeans.lib.profiler.common.event.ProfilingStateListener;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.results.RuntimeCCTNode;
import org.netbeans.lib.profiler.results.cpu.CPUCCTProvider;
import org.netbeans.lib.profiler.results.cpu.CPUResultsSnapshot;
import org.netbeans.lib.profiler.results.memory.MemoryCCTProvider;
import org.netbeans.lib.profiler.ui.LiveResultsPanel;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.cpu.CPUResUserActionsHandler;
import org.netbeans.lib.profiler.ui.cpu.CodeRegionLivePanel;
import org.netbeans.lib.profiler.ui.cpu.FlatProfilePanel;
import org.netbeans.lib.profiler.ui.cpu.LiveFlatProfilePanel;
import org.netbeans.lib.profiler.ui.memory.LiveAllocResultsPanel;
import org.netbeans.lib.profiler.ui.memory.LiveLivenessResultsPanel;
import org.netbeans.lib.profiler.ui.memory.MemoryResUserActionsHandler;
import org.netbeans.modules.profiler.actions.ResetResultsAction;
import org.netbeans.modules.profiler.actions.TakeSnapshotAction;
import org.netbeans.modules.profiler.api.ProfilingSettingsManager;
import org.netbeans.modules.profiler.utils.IDEUtils;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.actions.Presenter;
import org.openide.util.actions.SystemAction;
import org.openide.windows.TopComponent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Image;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.lib.profiler.common.CommonUtils;
import org.netbeans.lib.profiler.results.ExportDataDumper;
import org.netbeans.lib.profiler.results.memory.ClassHistoryDataManager;
import org.netbeans.lib.profiler.results.memory.PresoObjAllocCCTNode;
import org.netbeans.lib.profiler.ui.graphs.AllocationsHistoryGraphPanel;
import org.netbeans.lib.profiler.ui.graphs.LivenessHistoryGraphPanel;
import org.netbeans.lib.profiler.ui.memory.ClassHistoryActionsHandler;
import org.netbeans.lib.profiler.ui.memory.ClassHistoryModels;
import org.netbeans.lib.profiler.utils.VMUtils;
import org.netbeans.modules.profiler.api.GoToSource;
import org.netbeans.modules.profiler.api.ProfilerDialogs;
import org.netbeans.lib.profiler.ui.LiveResultsWindowContributor;
import org.netbeans.modules.profiler.api.icons.GeneralIcons;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import org.netbeans.modules.profiler.utilities.ProfilerUtils;


/**
 * An IDE TopComponent to display live profiling results.
 *
 * @author Tomas Hurka
 * @author Ian Formanek
 */
public final class LiveResultsWindow extends TopComponent
                                     implements ResultsListener,
                                                ProfilingStateListener,
                                                SaveViewAction.ViewProvider,
                                                ExportAction.ExportProvider {

    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    public static final class EmptyLiveResultsPanel extends JPanel implements LiveResultsPanel {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public int getSortingColumn() {
            return -1;
        }

        public boolean getSortingOrder() {
            return true;
        }

        public BufferedImage getViewImage(boolean b) {
            throw new UnsupportedOperationException();
        }

        public String getViewName() {
            return "Empty live results"; // NOI18N
        }

        public boolean fitsVisibleArea() {
            return false;
        }

        public void handleRemove() {
        }

        public void handleShutdown() {
        }

        public boolean hasView() {
            return false;
        }

        public void reset() {
        }

        public boolean supports(int instrumentationType) {
            return true;
        }

        public void updateLiveResults() {
        }
    }
    
    @org.openide.util.lookup.ServiceProviders({@org.openide.util.lookup.ServiceProvider(service=org.netbeans.lib.profiler.results.cpu.CPUCCTProvider.Listener.class), @org.openide.util.lookup.ServiceProvider(service=org.netbeans.lib.profiler.results.memory.MemoryCCTProvider.Listener.class)})
    public static final class ResultsMonitor implements CPUCCTProvider.Listener, MemoryCCTProvider.Listener {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void cctEstablished(RuntimeCCTNode runtimeCCTNode, boolean empty) {
            if (!empty) {
                getDefault().resultsAvailable = true;
                CommonUtils.runInEventDispatchThread(new Runnable() {
                        public void run() {
                            getDefault().updateResultsDisplay();
                        }
                    });
            } else {
                resultsDumpForced.set(false); // fix for issue #114638
            }
        }

        public void cctReset() {
            getDefault().resultsAvailable = false;
        }
    }

//    public static class ActivateDrillDownAction extends AbstractAction {
//        //~ Methods --------------------------------------------------------------------------------------------------------------
//
//        public void actionPerformed(ActionEvent e) {
//            //      System.out.println("ActivateDrillDownAction Invoked");
//            if (TopComponent.getRegistry().getActivated() == LiveResultsWindow.getDefault()) {
//                // LiveResultsWindow is active
//                TopComponent drillDown = DrillDownWindow.getDefault();
//
//                //        System.out.println(" Drill window: "+drillDown.isOpened());
//                if (drillDown.isOpened()) {
//                    // DrillDown is visible
//                    drillDown.requestActive();
//                }
//            }
//        }
//    }

    private final class CPUActionsHandler extends CPUResUserActionsHandler.Adapter {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void addMethodToRoots(final String className, final String methodName, final String methodSig) {
            Lookup.Provider project = NetBeansProfiler.getDefaultNB().getProfiledProject();

            ProfilingSettings[] projectSettings = ProfilingSettingsManager.getProfilingSettings(project)
                                                                          .getProfilingSettings();
            List<ProfilingSettings> cpuSettings = new ArrayList<ProfilingSettings>();

            for (ProfilingSettings settings : projectSettings) {
                if (ProfilingSettings.isCPUSettings(settings.getProfilingType())) {
                    cpuSettings.add(settings);
                }
            }

            ProfilingSettings lastProfilingSettings = null;
            String lastProfilingSettingsName = Profiler.getDefault().getLastProfilingSettings().getSettingsName();

            // Resolve current settings
            for (ProfilingSettings settings : cpuSettings) {
                if (settings.getSettingsName().equals(lastProfilingSettingsName)) {
                    lastProfilingSettings = settings;

                    break;
                }
            }

            ProfilingSettings settingsToModify = IDEUtils.selectSettings(ProfilingSettings.PROFILE_CPU_PART,
                                                                         cpuSettings.toArray(new ProfilingSettings[cpuSettings
                                                                                                                                                                                                                                                   .size()]),
                                                                         lastProfilingSettings);

            if (settingsToModify == null) {
                return; // cancelled by the user
            }

            settingsToModify.addRootMethod(className, methodName, methodSig);

            if (cpuSettings.contains(settingsToModify)) {
                ProfilingSettingsManager.storeProfilingSettings(projectSettings, settingsToModify, project);
            } else {
                ProfilingSettings[] newProjectSettings = new ProfilingSettings[projectSettings.length + 1];
                System.arraycopy(projectSettings, 0, newProjectSettings, 0, projectSettings.length);
                newProjectSettings[projectSettings.length] = settingsToModify;
                ProfilingSettingsManager.storeProfilingSettings(newProjectSettings, settingsToModify, project);
            }
        }

        public void showReverseCallGraph(final CPUResultsSnapshot snapshot, final int threadId, final int methodId, int view,
                                         int sortingColumn, boolean sortingOrder) {
            throw new IllegalStateException(ERROR_DISPLAYING_CALL_GRAPH_MSG);
        }

        public void showSourceForMethod(final String className, final String methodName, final String methodSig) {
            GoToSource.openSource(NetBeansProfiler.getDefaultNB().getProfiledProject(), className, methodName, methodSig);
        }

        public void viewChanged(int viewType) {
            if (currentDisplay != null) {
                ((FlatProfilePanel) currentDisplay).prepareResults();
            }
        }
    }

    private static final class MemoryActionsHandler implements MemoryResUserActionsHandler {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void showSourceForMethod(final String className, final String methodName, final String methodSig) {
            // Check if primitive type/array
            if ((methodName == null && methodSig == null) && (VMUtils.isVMPrimitiveType(className) ||
                 VMUtils.isPrimitiveType(className))) ProfilerDialogs.displayWarning(CANNOT_SHOW_PRIMITIVE_SRC_MSG);
            // Check if allocated by reflection
            else if (PresoObjAllocCCTNode.VM_ALLOC_CLASS.equals(className) && PresoObjAllocCCTNode.VM_ALLOC_METHOD.equals(methodName))
                     ProfilerDialogs.displayWarning(CANNOT_SHOW_REFLECTION_SRC_MSG);
            // Display source
            else GoToSource.openSource(NetBeansProfiler.getDefaultNB().getProfiledProject(), className, methodName, methodSig);
        }

        public void showStacksForClass(final int selectedClassId, final int sortingColumn, final boolean sortingOrder) {
            ProfilerUtils.runInProfilerRequestProcessor(new Runnable() {
                    public void run() {
                        final LoadedSnapshot ls = ResultsManager.getDefault().takeSnapshot();

                        if (ls != null) {
                            CommonUtils.runInEventDispatchThread(new Runnable() {
                                    public void run() {
                                        SnapshotResultsWindow srw = SnapshotResultsWindow.get(ls, sortingColumn, sortingOrder);

                                        if (srw != null) {
                                            srw.displayStacksForClass(selectedClassId, sortingColumn, sortingOrder);
                                        }
                                    }
                                });
                        }
                    }
                });
        }
    }

    private final class HistoryActionsHandler implements ClassHistoryActionsHandler {
        public void showClassHistory(int classID, final String className) {

            int currentlyTrackedClass = classHistoryManager.getTrackedClassID();
            String currentlyTrackedClassName = classHistoryManager.getTrackedClassName();
            if (currentlyTrackedClass != -1) {
                if (classID == currentlyTrackedClass) {
                    if (!ProfilerDialogs.displayConfirmationDNSA(MessageFormat.format(
                            LOGGING_RESET_MSG, new Object[] { currentlyTrackedClassName }),
                            LOGGING_CONFIRMATION_CAPTION, null, "History.historylogging.reset", true)) { //NOI18N
                        return;
                    }
                } else {
                    if (!ProfilerDialogs.displayConfirmationDNSA(MessageFormat.format(
                            LOGGING_STOP_MSG, new Object[] { currentlyTrackedClassName }),
                            LOGGING_CONFIRMATION_CAPTION, null, "History.historylogging.stop", true)) { //NOI18N
                        return;
                    }
                }
            }

            // Reset current history
            classHistoryManager.setupClass(classID, className);
            
            // Let the graphs update before showing the tab
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    chartActions.clear();
                    Action[] actions = null;

                    if (runner.getProfilerClient().getCurrentInstrType() ==
                        ProfilerEngineSettings.INSTR_OBJECT_ALLOCATIONS) {
                        if (allocationsHistoryPanel == null)
                            allocationsHistoryPanel = AllocationsHistoryGraphPanel.
                                                        createPanel(classHistoryModels);
                        historyPanel.removeAll();
                        historyPanel.add(allocationsHistoryPanel, BorderLayout.CENTER);
                        actions = allocationsHistoryPanel.getActions();
                    } else if (runner.getProfilerClient().getCurrentInstrType() ==
                               ProfilerEngineSettings.INSTR_OBJECT_LIVENESS) {
                        if (livenessHistoryPanel == null)
                                livenessHistoryPanel = LivenessHistoryGraphPanel.
                                                            createPanel(classHistoryModels);
                        historyPanel.removeAll();
                        historyPanel.add(livenessHistoryPanel, BorderLayout.CENTER);
                        actions = livenessHistoryPanel.getActions();
                    }

                    if (actions != null)
                        for (Action action : actions)
                            chartActions.add(new JButton(action));

                    Collections.reverse(chartActions);

                    tabs.setEnabledAt(1, true);
                    tabs.setTitleAt(1, NbBundle.getMessage(LiveResultsWindow.class,
                                                           "LiveResultsWindow_ClassHistoryTabName", //NOI18N
                                                            new Object[] { className }));
                    tabs.setSelectedIndex(1);
                }
            });
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final Logger LOGGER = Logger.getLogger("org.netbeans.modules.profiler.LiveResultsWindow"); // NOI18N
                                                                                                              // -----
                                                                                                              // I18N String constants
    private static final String UPDATE_RESULTS_AUTOMATICALLY_TOOLTIP = NbBundle.getMessage(LiveResultsWindow.class,
                                                                                           "LiveResultsWindow_UpdateResultsAutomaticallyTooltip"); // NOI18N
    private static final String UPDATE_RESULTS_NOW_TOOLTIP = NbBundle.getMessage(LiveResultsWindow.class,
                                                                                 "LiveResultsWindow_UpdateResultsNowTooltip"); // NOI18N
    private static final String RUN_GC_TOOLTIP = NbBundle.getMessage(LiveResultsWindow.class, "LiveResultsWindow_RunGCTooltip"); // NOI18N
    private static final String NO_PROFILING_RESULTS_LABEL_TEXT = NbBundle.getMessage(LiveResultsWindow.class,
                                                                                      "LiveResultsWindow_NoProfilingResultsLabelText"); // NOI18N
    private static final String ERROR_DISPLAYING_STACK_TRACES_MSG = NbBundle.getMessage(LiveResultsWindow.class,
                                                                                        "LiveResultsWindow_ErrorDisplayingStackTracesMsg"); // NOI18N
    private static final String ERROR_DISPLAYING_CALL_GRAPH_MSG = NbBundle.getMessage(LiveResultsWindow.class,
                                                                                      "LiveResultsWindow_ErrorDisplayingCallGraphMsg"); // NOI18N
    private static final String ERROR_INSTRUMENTING_ROOT_METHOD_MSG = NbBundle.getMessage(LiveResultsWindow.class,
                                                                                          "LiveResultsWindow_ErrorInstrumentingRootMethodMsg"); // NOI18N
    private static final String LIVE_RESULTS_TAB_NAME = NbBundle.getMessage(LiveResultsWindow.class,
                                                                            "LiveResultsWindow_LiveResultsTabName"); // NOI18N
    private static final String HISTORY_TAB_NAME = NbBundle.getMessage(LiveResultsWindow.class, "LiveResultsWindow_HistoryTabName"); // NOI18N
    private static final String LIVE_RESULTS_ACCESS_DESCR = NbBundle.getMessage(LiveResultsWindow.class,
                                                                                "LiveResultsWindow_LiveResultsAccessDescr"); // NOI18N
    private static final String LOGGING_CONFIRMATION_CAPTION = NbBundle.getMessage(LiveResultsWindow.class,
                                                                                   "History_LoggingConfirmationCaption"); //NOI18N
    private static final String LOGGING_RESET_MSG = NbBundle.getMessage(LiveResultsWindow.class, "History_LoggingResetMsg"); //NOI18N
    private static final String LOGGING_STOP_MSG = NbBundle.getMessage(LiveResultsWindow.class, "History_LoggingStopMsg"); //NOI18N

    // -----
    private static final String HELP_CTX_KEY = "LiveResultsWindow.HelpCtx"; // NOI18N
    private static final HelpCtx HELP_CTX = new HelpCtx(HELP_CTX_KEY);
    private static LiveResultsWindow defaultLiveInstance;
    private static final TargetAppRunner runner = Profiler.getDefault().getTargetAppRunner();
    private static final Image liveWindowIcon = Icons.getImage(ProfilerIcons.WINDOW_LIVE_RESULTS);
    private static final AtomicBoolean resultsDumpForced = new AtomicBoolean(false);

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private CPUResUserActionsHandler cpuActionsHandler;
    private Component lastFocusOwner;
    private EmptyLiveResultsPanel noResultsPanel;
    private JButton runGCButton;
    private JButton updateNowButton;
    private JPanel currentDisplayComponent;
    private JPanel memoryTabPanel;
    private JTabbedPane tabs;
    private JToggleButton autoToggle;
    private JToolBar.Separator graphButtonsSeparator;

    private List<JButton> chartActions = new ArrayList();

    private JPanel historyPanel;
    private ClassHistoryDataManager classHistoryManager;
    private ClassHistoryModels classHistoryModels;
    private AllocationsHistoryGraphPanel allocationsHistoryPanel;
    private LivenessHistoryGraphPanel livenessHistoryPanel;

    /*  private JComponent valueFilterComponent;
       private JSlider valueSlider; */
    private JToolBar toolBar;
    private LiveResultsPanel currentDisplay;
    private MemoryResUserActionsHandler memoryActionsHandler;
    private HistoryActionsHandler historyActionsHandler;
    private boolean autoRefresh = true;
    private volatile boolean profilerRunning = false;
    private volatile boolean resultsAvailable = false;

    private static class Singleton {
        final private static LiveResultsWindow INSTANCE = new LiveResultsWindow();
    }
    
    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public LiveResultsWindow() {
        CommonUtils.runInEventDispatchThreadAndWait(new Runnable() {
            public void run() {
                initUI();
            }
        });
    }
    
    private void initUI() {
        setName(NbBundle.getMessage(LiveResultsWindow.class, "LAB_ResultsWindowName")); // NOI18N
        setIcon(liveWindowIcon);
        getAccessibleContext().setAccessibleDescription(LIVE_RESULTS_ACCESS_DESCR);
        //        setBorder(new EmptyBorder(5, 5, 5, 5));
        setLayout(new BorderLayout());

        memoryActionsHandler = new MemoryActionsHandler();
        historyActionsHandler = new HistoryActionsHandler();
        cpuActionsHandler = new CPUActionsHandler();

        classHistoryManager = new ClassHistoryDataManager();
        classHistoryModels = new ClassHistoryModels(classHistoryManager);

        toolBar = createToolBar();

        add(toolBar, BorderLayout.NORTH);

        noResultsPanel = new EmptyLiveResultsPanel();
        noResultsPanel.setLayout(new BorderLayout());
        noResultsPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        final JLabel noResultsLabel = new JLabel(NO_PROFILING_RESULTS_LABEL_TEXT);

        noResultsLabel.setFont(noResultsLabel.getFont().deriveFont(14));
        noResultsLabel.setIcon(Icons.getIcon(ProfilerIcons.MONITORING_32));
        noResultsLabel.setIconTextGap(10);
        noResultsLabel.setEnabled(false);
        noResultsPanel.add(noResultsLabel, BorderLayout.NORTH);

        currentDisplay = null;
        currentDisplayComponent = noResultsPanel;
        add(noResultsPanel, BorderLayout.CENTER);

        //*************
        memoryTabPanel = new JPanel(new java.awt.BorderLayout());

        tabs = new JTabbedPane();
        // Fix for Issue 115062 (CTRL-PageUp/PageDown should move between snapshot tabs)
        tabs.getActionMap().getParent().remove("navigatePageUp"); // NOI18N
        tabs.getActionMap().getParent().remove("navigatePageDown"); // NOI18N
                                                                    // TODO: implement PreviousViewAction/NextViewAction handling for Live Memory Results

        graphButtonsSeparator = new JToolBar.Separator();
        toolBar.add(graphButtonsSeparator);
        final int chartButtonsOffset = toolBar.getComponentCount();

        memoryTabPanel.add(tabs, BorderLayout.CENTER);
        tabs.setTabPlacement(JTabbedPane.BOTTOM);
        tabs.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                if (currentDisplayComponent == memoryTabPanel) {
                    if (tabs.getSelectedComponent() == historyPanel) {
                        for (JButton b : chartActions)
                            toolBar.add(b, chartButtonsOffset);
                        graphButtonsSeparator.setVisible(true);
                    } else {
                        for (JButton b : chartActions)
                            toolBar.remove(b);
                        graphButtonsSeparator.setVisible(false);
                    }
                }
            }
        });

        historyPanel = new JPanel(new BorderLayout());

        hideContributors();
        //******************
        setFocusable(true);
        setRequestFocusEnabled(true);

        Profiler.getDefault().addProfilingStateListener(this);
        ResultsManager.getDefault().addResultsListener(this);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static LiveResultsWindow getDefault() {
        if (!hasDefault()) {
            defaultLiveInstance = Singleton.INSTANCE;
        }

        return defaultLiveInstance;
    }

    public static void setPaused(boolean value) {
        //        paused = value;
    }

    public static void closeIfOpened() {
        if (hasDefault()) {
            CommonUtils.runInEventDispatchThread(new Runnable() {
                    public void run() {
                        if (defaultLiveInstance.isOpened()) {
                            defaultLiveInstance.close();
                        }
                    }
                });
        }
    }

    // checks if default instance exists to prevent unnecessary instantiation from getDefault()
    public static boolean hasDefault() {
        return defaultLiveInstance != null;
    }

    public void setAutoRefresh(final boolean value) {
        if (autoRefresh != value) {
            autoRefresh = value;
            autoToggle.setSelected(value);
        }
    }

    public boolean isAutoRefresh() {
        return autoRefresh;
    }

    public HelpCtx getHelpCtx() {
        return HELP_CTX;
    }

    // ------------------------------------------------------------------------------------------------------
    // TopComponent behavior
    public int getPersistenceType() {
        return TopComponent.PERSISTENCE_NEVER;
    }

    public int getSortingColumn() {
        if (currentDisplay == null) {
            return CommonConstants.SORTING_COLUMN_DEFAULT;
        } else {
            return currentDisplay.getSortingColumn();
        }
    }

    public boolean getSortingOrder() {
        if (currentDisplay == null) {
            return false;
        } else {
            return currentDisplay.getSortingOrder();
        }
    }

    public BufferedImage getViewImage(boolean onlyVisibleArea) {
        if ((currentDisplayComponent == memoryTabPanel) && (tabs.getSelectedComponent() == historyPanel)) {
            return UIUtils.createScreenshot(historyPanel);
        }

        if (currentDisplay == null) {
            return null;
        }

        return currentDisplay.getViewImage(onlyVisibleArea);
    }

    public String getViewName() {
        if ((currentDisplayComponent == memoryTabPanel) && (tabs.getSelectedComponent() == historyPanel)) {
            return "memory-history-" + classHistoryManager.getTrackedClassName(); // NOI18N
        }

        if (currentDisplay == null) {
            return null;
        }

        return currentDisplay.getViewName();
    }

    public void componentActivated() {
        super.componentActivated();

        if (lastFocusOwner != null) {
            lastFocusOwner.requestFocus();
        } else if (currentDisplayComponent != null) {
            currentDisplayComponent.requestFocus();
        }
    }

    public void componentDeactivated() {
        super.componentDeactivated();

        lastFocusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    }

    public boolean fitsVisibleArea() {
        if ((currentDisplayComponent == memoryTabPanel) && (tabs.getSelectedComponent() == historyPanel)) {
            return true;
        }

        return (currentDisplay != null) && currentDisplay.fitsVisibleArea();
    }

    /** This method is called before the profiling session ends to enable
     * asynchronous live results update if auto refresh is on.
     */
    public void handleShutdown() {
        profilerRunning = false;

        if (isShowing()) {
            hideContributors();
            requestProfilingDataUpdate(false);
        }

        //    if (currentDisplay != null) {
        //      currentDisplay.handleShutdown();
        //      currentDisplay = null;
        //    }
        //    updateResultsDisplay(false);
        //    resetResultsDisplay();
    }

    public void handleStartup() {
        profilerRunning = true;
    }

    public void handleCleanupBeforeProfiling() {
        classHistoryManager.resetClass();
    }

    // --- Save Current View action support ------------------------------------
    public boolean hasView() {
        if ((currentDisplayComponent == memoryTabPanel) && (tabs.getSelectedComponent() == historyPanel)) {
            return true;
        }

        return !noResultsPanel.isShowing() && (currentDisplay != null) && currentDisplay.hasView();
    }

    public void ideClosing() {
        hideContributors();
    }

    public void instrumentationChanged(int oldInstrType, int currentInstrType) {
        requestProfilingDataUpdate(false);
    }

    public void profilingStateChanged(ProfilingStateEvent e) {
        updateActions(e.getNewState());

        switch (e.getNewState()) {
            case Profiler.PROFILING_INACTIVE:
                handleShutdown();

                break;
            case Profiler.PROFILING_RUNNING:
                handleStartup();

                break;
        }
    }

    /**
     * This method is called periodically every 2400 ms, from the ProfilerMonitor monitor thread, giving the
     * ResultsManager a chance to update live results.
     * Can be called explicitely to force update of the live results outside of the periodic cycle.
     * The results will only be updated (and associated event fired) if the LiveResultsWindow is displayed.
     */
    public boolean refreshLiveResults() {
        if (isAutoRefresh() && isShowing()) {
            requestProfilingDataUpdate(false);

            return true;
        } else {
            // -----------------------------------------------------------------------
            // Temporary workaround to refresh profiling points when LiveResultsWindow is not refreshing
            // TODO: move this code to a separate class performing the update if necessary
            if (NetBeansProfiler.getDefaultNB().processesProfilingPoints()) {
                callForceObtainedResultsDump(runner.getProfilerClient(), false);
            }

            // -----------------------------------------------------------------------
            return false;
        }
    }

    public void resultsAvailable() {
    }

    public void resultsReset() {
        reset();
    }

    public void threadsMonitoringChanged() {
        // ignore
    }

    public void exportData(int exportedFileType, ExportDataDumper eDD) {
        if (currentDisplayComponent == memoryTabPanel) {
            if (tabs.getSelectedComponent() instanceof LiveAllocResultsPanel) {
                ((LiveAllocResultsPanel) currentDisplay).exportData(exportedFileType, eDD, NbBundle.getMessage(LiveResultsWindow.class, "LAB_ResultsWindowName"));
            } else if (tabs.getSelectedComponent() instanceof LiveLivenessResultsPanel) {
                ((LiveLivenessResultsPanel) currentDisplay).exportData(exportedFileType, eDD, NbBundle.getMessage(LiveResultsWindow.class, "LAB_ResultsWindowName"));
            }
        } else if (currentDisplayComponent instanceof LiveFlatProfilePanel) {
            ((LiveFlatProfilePanel) currentDisplay).exportData(exportedFileType, eDD, NbBundle.getMessage(LiveResultsWindow.class, "LAB_ResultsWindowName"));
        }
    }

    public boolean hasLoadedSnapshot() {
        return false;
    }

    public boolean hasExportableView() {
        if ((currentDisplayComponent == memoryTabPanel) && (tabs.getSelectedComponent() == currentDisplay)) {
            return true;
        }

        return !noResultsPanel.isShowing() && (currentDisplay != null) && currentDisplay.hasView();
    }

    protected void componentClosed() {
        super.componentClosed();

        //    Profiler.getDefault().removeProfilingStateListener(this);
    }

    protected void componentHidden() {
        super.componentHidden();

        hideContributors();
    }

    //
    //  private ProfilerClientListener clientListener = new ProfilerClientListener() {
    //    public void instrumentationChanged(int oldInstrType, int newInstrType) {
    //      deconfigureForInstrType(oldInstrType);
    //      configureForInstrType(newInstrType);
    //    }
    //  };

    /**
     * Called when the topcomponent has been just open
     */
    protected void componentOpened() {
        super.componentOpened();

        //    Profiler.getDefault().addProfilingStateListener(this);
    }

    protected void componentShowing() {
        super.componentShowing();
        updateResultsDisplay();
    }

    /**
     * Subclasses are encouraged to override this method to provide preferred value
     * for unique TopComponent Id returned by getID. Returned value is used as starting
     * value for creating unique TopComponent ID.
     * Value should be preferably unique, but need not be.
     */
    protected String preferredID() {
        return this.getClass().getName();
    }

    /**
     * This should be called when the app is restarted or "Reset Collected Results" is invoked (because once this happened,
     * there are all sorts of data that's going to be deleted/changed, and an attempt to do something with old results displayed
     * here can cause big problems). It should also set the results panel invisible (or is it already happening?) etc.
     */
    void reset() {
        resultsAvailable = false;

        if (currentDisplay != null) {
            currentDisplay.reset();
            resetResultsDisplay();
        }

        resetContributors();
    }

    // -- Private implementation -------------------------------------------------------------------------------------------
    private static boolean callForceObtainedResultsDump(final ProfilerClient client) {
        return callForceObtainedResultsDump(client, true);
    }

    private static boolean callForceObtainedResultsDump(final ProfilerClient client, final boolean refreshDisplay) {
        if (refreshDisplay) {
            resultsDumpForced.set(true);
        }

        try {
            if (client.getCurrentInstrType() != ProfilerEngineSettings.INSTR_CODE_REGION) {
                client.forceObtainedResultsDump(true);
            }

            return true;
        } catch (ClientUtils.TargetAppOrVMTerminated targetAppOrVMTerminated) {
            return false;
        }
    }

    private boolean isProfiling() {
        return runner.getProfilerClient().getCurrentInstrType() != ProfilerEngineSettings.INSTR_NONE;
    }

    private static boolean checkIfResultsExist(final ProfilerClient client, final int currentInstrType) {
        switch (currentInstrType) {
            case ProfilerEngineSettings.INSTR_RECURSIVE_FULL:
            case ProfilerEngineSettings.INSTR_RECURSIVE_SAMPLED:
            case ProfilerEngineSettings.INSTR_NONE_SAMPLING:

            //        return client.getCPUCallGraphBuilder().resultsExist(); // TODO
            case ProfilerEngineSettings.INSTR_OBJECT_ALLOCATIONS:
            case ProfilerEngineSettings.INSTR_OBJECT_LIVENESS:
                return getDefault().resultsAvailable;
            case CommonConstants.INSTR_CODE_REGION:

                try {
                    return client.cpuResultsExist();
                } catch (ClientUtils.TargetAppOrVMTerminated targetAppOrVMTerminated) {
                    return false;
                }
        }

        return false;
    }

    private JToolBar createToolBar() {
        JToolBar toolBar = new JToolBar() {
            public Component add(Component comp) {
                if (comp instanceof JButton) {
                    UIUtils.fixButtonUI((JButton) comp);
                }

                return super.add(comp);
            }
        };

        toolBar.setFloatable(false);
        toolBar.putClientProperty("JToolBar.isRollover", Boolean.TRUE); //NOI18N
        toolBar.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

        autoToggle = new JToggleButton(Icons.getIcon(GeneralIcons.UPDATE_AUTO));
        autoToggle.setSelected(true);
        autoToggle.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    autoRefresh = autoToggle.isSelected();
                }
            });
        autoToggle.setToolTipText(UPDATE_RESULTS_AUTOMATICALLY_TOOLTIP);
        autoToggle.getAccessibleContext().setAccessibleName(UPDATE_RESULTS_AUTOMATICALLY_TOOLTIP);

        updateNowButton = new JButton(Icons.getIcon(GeneralIcons.UPDATE_NOW));
        updateNowButton.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    requestProfilingDataUpdate(true);
                }
            });
        updateNowButton.setToolTipText(UPDATE_RESULTS_NOW_TOOLTIP);
        updateNowButton.getAccessibleContext().setAccessibleName(UPDATE_RESULTS_NOW_TOOLTIP);

        runGCButton = new JButton(Icons.getIcon(ProfilerIcons.RUN_GC));
        runGCButton.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    try {
                        runner.runGC();
                    } catch (ClientUtils.TargetAppOrVMTerminated ex) {
                        ProfilerDialogs.displayError(ex.getMessage());
                        ProfilerLogger.log(ex);
                    }

                    requestProfilingDataUpdate(true);
                }
            });
        runGCButton.setToolTipText(RUN_GC_TOOLTIP);
        runGCButton.getAccessibleContext().setAccessibleName(RUN_GC_TOOLTIP);

        // todo: add profiler listener to enable/disable buttons
        toolBar.add(autoToggle);
        toolBar.add(updateNowButton);
        toolBar.add(runGCButton);
        toolBar.add(new ResetResultsAction());
        toolBar.addSeparator();
        toolBar.add(((Presenter.Toolbar) SystemAction.get(TakeSnapshotAction.class)).getToolbarPresenter());
        toolBar.addSeparator();
        toolBar.add(new ExportAction(this, null));
        toolBar.add(new SaveViewAction(this));

        return toolBar;
    }

    private LiveResultsPanel preparePanelForInstrType(int instrumentationType) {
        LiveResultsPanel aPanel = null;

        switch (instrumentationType) {
            case ProfilerEngineSettings.INSTR_OBJECT_ALLOCATIONS: {
                LiveAllocResultsPanel allocPanel = new LiveAllocResultsPanel(runner,
                                                    memoryActionsHandler,
                                                    historyActionsHandler,
                                                    classHistoryManager);
                currentDisplayComponent = memoryTabPanel;
                currentDisplayComponent.setBorder(new EmptyBorder(5, 0, 0, 0));

                if (tabs.getComponentCount() > 0) {
                    tabs.removeAll();
                }

                tabs.addTab(LIVE_RESULTS_TAB_NAME, allocPanel);
                tabs.addTab(HISTORY_TAB_NAME, historyPanel);
                tabs.setEnabledAt(1, false);
                aPanel = allocPanel;

                break;
            }
            case ProfilerEngineSettings.INSTR_OBJECT_LIVENESS: {
                LiveLivenessResultsPanel livenessPanel = new LiveLivenessResultsPanel(runner,
                                                    memoryActionsHandler,
                                                    historyActionsHandler,
                                                    classHistoryManager);
                currentDisplayComponent = memoryTabPanel;
                currentDisplayComponent.setBorder(new EmptyBorder(5, 0, 0, 0));

                if (tabs.getComponentCount() > 0) {
                    tabs.removeAll();
                }

                tabs.addTab(LIVE_RESULTS_TAB_NAME, livenessPanel);
                tabs.addTab(HISTORY_TAB_NAME, historyPanel);
                tabs.setEnabledAt(1, false);
                aPanel = livenessPanel;

                break;
            }
            case ProfilerEngineSettings.INSTR_RECURSIVE_FULL:
            case ProfilerEngineSettings.INSTR_RECURSIVE_SAMPLED: 
            case ProfilerEngineSettings.INSTR_NONE_SAMPLING: {
                Lookup.Provider project = NetBeansProfiler.getDefaultNB().getProfiledProject();

                final LiveFlatProfilePanel cpuPanel = new LiveFlatProfilePanel(runner, cpuActionsHandler);

                for(LiveResultsWindowContributor c : Lookup.getDefault().lookupAll(LiveResultsWindowContributor.class)) {
                    c.addToCpuResults(cpuPanel, toolBar, runner.getProfilerClient(), project);
                }

                currentDisplayComponent = cpuPanel;

                currentDisplayComponent.setBorder(new EmptyBorder(5, 5, 5, 5));
                aPanel = cpuPanel;

                break;
            }
            case ProfilerEngineSettings.INSTR_CODE_REGION: {
                CodeRegionLivePanel regionPanel = new CodeRegionLivePanel(runner.getProfilerClient());
                currentDisplayComponent = regionPanel;
                currentDisplayComponent.setBorder(new EmptyBorder(5, 5, 5, 5));
                aPanel = regionPanel;

                break;
            }
            case ProfilerEngineSettings.INSTR_NONE:
                throw new IllegalStateException(); // this cannot happen
                                                   //        break;
        }

        return aPanel;
    }

    private void requestProfilingDataUpdate(final boolean force) {
        CommonUtils.runInEventDispatchThread(new Runnable() {
                public void run() {
                    if (!isAutoRefresh() && !force) {
                        return;
                    }

                    if (!isProfiling() || !isOpened()) {
                        return;
                    }

                    ProfilerUtils.runInProfilerRequestProcessor(new Runnable() {
                            public void run() {
                                // send a command to server to generate the newest live data
                                callForceObtainedResultsDump(runner.getProfilerClient());
                            }
                        });
                }
            });
    }

    private void resetResultsDisplay() {
        if ((currentDisplayComponent != null) && (currentDisplayComponent != noResultsPanel)) {
            remove(currentDisplayComponent);
            currentDisplay = null;
            currentDisplayComponent = noResultsPanel;
            add(noResultsPanel, BorderLayout.CENTER);
            for (JButton b : chartActions) toolBar.remove(b);
            graphButtonsSeparator.setVisible(false);
            revalidate();
            repaint();
            hideContributors();
        }
    }

    private void updateActions(int newState) {
        runGCButton.setEnabled(newState == Profiler.PROFILING_RUNNING);
        updateNowButton.setEnabled(newState == Profiler.PROFILING_RUNNING);
    }

    private void refresh() {        
        if (LOGGER.isLoggable(Level.FINE) && (currentDisplayComponent != null)) {
            LOGGER.fine("refreshing contributors for drilldown: " + currentDisplayComponent.getClass().getName()); // NOI18N
        }
        
        refreshContributors();

        if (profilerRunning && currentDisplayComponent instanceof LiveFlatProfilePanel) {
            LOGGER.fine("Showing contributors"); // NOI18N

            if (isVisible()) {
                showContributors();
            }
        } else {
            LOGGER.fine("Hiding contributors"); // NOI18N

            hideContributors();
        }
    }

    private void updateResultsDisplay() {
        if (!isOpened()) {
            return; // do nothing if i'm closed
        }

        if (!resultsDumpForced.getAndSet(false) && !isAutoRefresh()) {
            return; // process only forced results if autorefresh is off
        }

        if (!resultsAvailable) {
            currentDisplay = null;
            currentDisplayComponent = noResultsPanel;

            return;
        }

        int instrType = runner.getProfilerClient().getCurrentInstrType();

        if (instrType != CommonConstants.INSTR_NONE) {

            // does the current display support the instrumentation in use?
            boolean instrSupported = (currentDisplayComponent != null)
                                     && ((currentDisplay != null)
                                         ? currentDisplay.supports(instrType) : false);

            if (!instrSupported) {
                if (currentDisplayComponent != null) {
                    remove(currentDisplayComponent);
                }

                if (currentDisplay != null) {
                    currentDisplay.handleRemove();
                }

                currentDisplay = preparePanelForInstrType(instrType);
                add(currentDisplayComponent, BorderLayout.CENTER);
                revalidate();
                repaint();
                CommonUtils.runInEventDispatchThread(new Runnable() {
                    public void run() {
                        currentDisplayComponent.requestFocusInWindow(); // must be invoked lazily to override default focus behavior
                    }
                });
            }

            if (currentDisplay != null) {
                currentDisplay.updateLiveResults();
            }

        }

        refresh();
    }
    
    private void hideContributors() {
        for(LiveResultsWindowContributor c : Lookup.getDefault().lookupAll(LiveResultsWindowContributor.class)) {
            c.hide();
        }
    }
    
    private void showContributors() {
        for(LiveResultsWindowContributor c : Lookup.getDefault().lookupAll(LiveResultsWindowContributor.class)) {
            c.show();
        }
    }
    
    private void refreshContributors() {
        for(LiveResultsWindowContributor c : Lookup.getDefault().lookupAll(LiveResultsWindowContributor.class)) {
            c.refresh();
        }
    }
    
    private void resetContributors() {
        for(LiveResultsWindowContributor c : Lookup.getDefault().lookupAll(LiveResultsWindowContributor.class)) {
            c.reset();
        }
    }
}
