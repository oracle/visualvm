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
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;
import org.openide.windows.TopComponent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.lib.profiler.common.CommonUtils;
import org.netbeans.lib.profiler.common.event.ProfilingStateAdapter;
import org.netbeans.lib.profiler.results.ExportDataDumper;
import org.netbeans.lib.profiler.results.memory.ClassHistoryDataManager;
import org.netbeans.lib.profiler.results.memory.PresoObjAllocCCTNode;
import org.netbeans.lib.profiler.ui.graphs.AllocationsHistoryGraphPanel;
import org.netbeans.lib.profiler.ui.graphs.LivenessHistoryGraphPanel;
import org.netbeans.lib.profiler.ui.memory.ClassHistoryActionsHandler;
import org.netbeans.lib.profiler.ui.memory.ClassHistoryModels;
import org.netbeans.lib.profiler.ui.memory.LiveSampledResultsPanel;
import org.netbeans.lib.profiler.utils.VMUtils;
import org.netbeans.modules.profiler.api.GoToSource;
import org.netbeans.modules.profiler.api.ProfilerDialogs;
import org.netbeans.lib.profiler.ui.LiveResultsWindowContributor;
import org.netbeans.lib.profiler.ui.ResultsView;
import org.netbeans.lib.profiler.ui.components.ProfilerToolbar;
import org.netbeans.modules.profiler.api.icons.GeneralIcons;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import org.netbeans.modules.profiler.utilities.Delegate;
import org.netbeans.modules.profiler.utilities.ProfilerUtils;


/**
 * An IDE TopComponent to display live profiling results.
 *
 * @author Tomas Hurka
 * @author Ian Formanek
 */
@NbBundle.Messages({
    "LiveResultsWindow_NoProfilingResultsLabelText=No profiling results available yet",
    "LiveResultsWindow_UpdateResultsAutomaticallyTooltip=Update Results Automatically",
    "LiveResultsWindow_UpdateResultsNowTooltip=Update Results Now",
    "LiveResultsWindow_RunGCTooltip=Run Garbage Collection in Profiled Application and Update Results",
    "LiveResultsWindow_ErrorDisplayingStackTracesMsg=Allocation stack traces cannot be displayed for live results",
    "LiveResultsWindow_ErrorDisplayingCallGraphMsg=Reverse call graphs can not be displayed for live results",
    "LiveResultsWindow_ErrorInstrumentingRootMethodMsg=Error occured when instrumenting the new root method: {0}",
    "LiveResultsWindow_LiveResultsAccessDescr=Live profiling results",
    "LiveResultsWindow_LiveResultsTabName=Live Results",
    "LiveResultsWindow_HistoryTabName=Class History",
    "LiveResultsWindow_ClassHistoryTabName=History of {0}",
    "History_LiveObjects=Live Objects",
    "History_AllocatedObjects=Allocated Objects",
    "History_AllocatedSize=Allocated Size",
    "History_LoggingConfirmationCaption=Log Class History",
    "History_LoggingResetMsg=This will reset history logging for {0}.\nDo you want to continue?",
    "History_LoggingStopMsg=This will stop history logging for {0}.\nDo you want to continue?",
    "LAB_ResultsWindowName=Live Results"
})
public final class LiveResultsWindow extends ProfilerTopComponent
                                     implements SaveViewAction.ViewProvider,
                                                ExportAction.ExportProvider {

    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    /* 
     * The following code is an externalization of various listeners registered
     * in the global lookup and needing access to an enclosing instance of
     * LiveResultsWindow. 
     * The enclosing instance will use the FQN registration to obtain the shared instance
     * of the listener implementation and inject itself as a delegate into the listener.
     */
    @ServiceProvider(service=ResultsListener.class)
    public static class Listener extends Delegate<LiveResultsWindow> implements ResultsListener {

        private boolean callResultsAvailable;
        
        @Override
        public void setDelegate(LiveResultsWindow delegate) {
            super.setDelegate(delegate);
            if (callResultsAvailable) {
                resultsAvailable();
            }
        }

        @Override
        public void resultsAvailable() {
            LiveResultsWindow win = getDelegate();
            if (win!=null) {
                win.resultsAvailableinTA = true;
                TargetAppRunner runner = Profiler.getDefault().getTargetAppRunner();
                int instrType = runner.getProfilerClient().getCurrentInstrType();
                if (instrType == ProfilerEngineSettings.INSTR_NONE_MEMORY_SAMPLING) {
                    win.resultsAvailable();
                }
            } else {
                callResultsAvailable = true;
            }
        }

        @Override
        public void resultsReset() {
            LiveResultsWindow win = getDelegate();
            if (win != null) {
                win.resultsAvailableinTA = false;
                win.reset();
            } else {
                callResultsAvailable = false;
            }
        }
        
    }
    
    public static final class EmptyLiveResultsPanel extends JPanel implements LiveResultsPanel {
        //~ Methods --------------------------------------------------------------------------------------------------------------
        
        public EmptyLiveResultsPanel() {
            setOpaque(false);
        }

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
    
    @ServiceProviders({@ServiceProvider(service=CPUCCTProvider.Listener.class), @ServiceProvider(service=MemoryCCTProvider.Listener.class)})
    public static final class ResultsMonitor implements CPUCCTProvider.Listener, MemoryCCTProvider.Listener {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void cctEstablished(RuntimeCCTNode runtimeCCTNode, final boolean empty) {
            CommonUtils.runInEventDispatchThread(new Runnable() {
                public void run() {
                    if (!empty) {
                        getDefault().resultsAvailable();
                    } else {
                        resultsDumpForced.set(false); // fix for issue #114638
                    }
                }
            });
        }

        public void cctReset() {
            CommonUtils.runInEventDispatchThread(new Runnable() {
                public void run() {
                   getDefault().resultsAvailable = false;
                }
            });
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
            ProfilerUtils.runInProfilerRequestProcessor(new Runnable() {

                @Override
                public void run() {
                    final Lookup.Provider project = NetBeansProfiler.getDefaultNB().getProfiledProject();
                    final ProfilingSettings[] projectSettings = ProfilingSettingsManager.getProfilingSettings(project).getProfilingSettings();
                    final List<ProfilingSettings> cpuSettings = new ArrayList<ProfilingSettings>();

                    for (ProfilingSettings settings : projectSettings) {
                        if (ProfilingSettings.isCPUSettings(settings.getProfilingType())) {
                            cpuSettings.add(settings);
                        }
                    }

                    final ProfilingSettings[] lastProfilingSettings = new ProfilingSettings[1];
                    String lastProfilingSettingsName = Profiler.getDefault().getLastProfilingSettings().getSettingsName();

                    // Resolve current settings
                    for (ProfilingSettings settings : cpuSettings) {
                        if (settings.getSettingsName().equals(lastProfilingSettingsName)) {
                            lastProfilingSettings[0] = settings;

                            break;
                        }
                    }
                    
                    SwingUtilities.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            final ProfilingSettings settingsToModify = IDEUtils.selectSettings(ProfilingSettings.PROFILE_CPU_PART,
                                                                    cpuSettings.toArray(new ProfilingSettings[cpuSettings                                                                                                                                                .size()]),
                                                                    lastProfilingSettings[0]);

                            if (settingsToModify == null) {
                                return; // cancelled by the user
                            }
                            
                            ProfilerUtils.runInProfilerRequestProcessor(new Runnable() {
                                @Override
                                public void run() {
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
                            });
                        }
                    });
                }
            });
        }

        public void showReverseCallGraph(final CPUResultsSnapshot snapshot, final int threadId, final int methodId, int view,
                                         int sortingColumn, boolean sortingOrder) {
            throw new IllegalStateException(Bundle.LiveResultsWindow_ErrorDisplayingCallGraphMsg());
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
                    if (!ProfilerDialogs.displayConfirmationDNSA(
                            Bundle.History_LoggingResetMsg(currentlyTrackedClassName),
                            Bundle.History_LoggingConfirmationCaption(), null, "History.historylogging.reset", true)) { //NOI18N
                        return;
                    }
                } else {
                    if (!ProfilerDialogs.displayConfirmationDNSA(
                            Bundle.History_LoggingStopMsg(currentlyTrackedClassName),
                            Bundle.History_LoggingConfirmationCaption(), null, "History.historylogging.stop", true)) { //NOI18N
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

                    TargetAppRunner runner = Profiler.getDefault().getTargetAppRunner();
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

                    resultsView.setViewEnabled(historyPanel, true);
                    resultsView.setViewName(historyPanel, Bundle.LiveResultsWindow_ClassHistoryTabName(className));
                    resultsView.selectView(historyPanel);
                }
            });
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final Logger LOGGER = Logger.getLogger("org.netbeans.modules.profiler.LiveResultsWindow"); // NOI18N

    private static final String HELP_CTX_KEY = "LiveResultsWindow.HelpCtx"; // NOI18N
    private static final String HELP_CTX_KEY_CPU = "CpuLiveResultsWindow.HelpCtx"; // NOI18N
    private static final String HELP_CTX_KEY_MEM = "MemoryLiveResultsWindow.HelpCtx"; // NOI18N
    private static final HelpCtx HELP_CTX_DEFAULT = new HelpCtx(HELP_CTX_KEY);
    private static HelpCtx HELP_CTX = HELP_CTX_DEFAULT;
    private static LiveResultsWindow defaultLiveInstance;
    private static final AtomicBoolean resultsDumpForced = new AtomicBoolean(false);

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private CPUResUserActionsHandler cpuActionsHandler;
    private EmptyLiveResultsPanel noResultsPanel;
    private JButton runGCButton;
    private JButton updateNowButton;
    private JPanel currentDisplayComponent;
    private JPanel memoryTabPanel;
    private ResultsView resultsView;
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
    private ProfilerToolbar toolBar;
    private LiveResultsPanel currentDisplay;
    private MemoryResUserActionsHandler memoryActionsHandler;
    private HistoryActionsHandler historyActionsHandler;
    private boolean autoRefresh = true;
    private volatile boolean profilerRunning = false;
    private volatile boolean resultsAvailable = false;
    private volatile boolean resultsAvailableinTA = false;
    private Listener listener;
    final private AtomicLong autoRefreshRequested = new AtomicLong(0);
    
    private static class Singleton {
        final private static LiveResultsWindow INSTANCE = new LiveResultsWindow();
    }
    
    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public LiveResultsWindow() {
        CommonUtils.runInEventDispatchThreadAndWait(new Runnable() {
            public void run() {
                initUI();
                listener = Lookup.getDefault().lookup(Listener.class);
                listener.setDelegate(LiveResultsWindow.this);
            }
        });
    }
    
    private void initUI() {
        setName(Bundle.LAB_ResultsWindowName());
        setIcon(Icons.getImage(ProfilerIcons.WINDOW_LIVE_RESULTS));
        getAccessibleContext().setAccessibleDescription(Bundle.LiveResultsWindow_LiveResultsAccessDescr());
        //        setBorder(new EmptyBorder(5, 5, 5, 5));
        setLayout(new BorderLayout());

        memoryActionsHandler = new MemoryActionsHandler();
        historyActionsHandler = new HistoryActionsHandler();
        cpuActionsHandler = new CPUActionsHandler();

        classHistoryManager = new ClassHistoryDataManager();
        classHistoryModels = new ClassHistoryModels(classHistoryManager);

        toolBar = createToolBar();

        add(toolBar.getComponent(), BorderLayout.NORTH);

        noResultsPanel = new EmptyLiveResultsPanel();
        noResultsPanel.setLayout(new BorderLayout());
        noResultsPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        final JLabel noResultsLabel = new JLabel(Bundle.LiveResultsWindow_NoProfilingResultsLabelText());

        noResultsLabel.setFont(noResultsLabel.getFont().deriveFont(14));
        noResultsLabel.setIcon(Icons.getIcon(ProfilerIcons.MONITORING_32));
        noResultsLabel.setIconTextGap(10);
        noResultsLabel.setEnabled(false);
        noResultsPanel.add(noResultsLabel, BorderLayout.NORTH);

        currentDisplay = null;
        currentDisplayComponent = noResultsPanel;
        add(noResultsPanel, BorderLayout.CENTER);

        //*************
        memoryTabPanel = new JPanel(new BorderLayout());
        memoryTabPanel.setOpaque(false);

        graphButtonsSeparator = new JToolBar.Separator();
        toolBar.add(graphButtonsSeparator);
        final int chartButtonsOffset = toolBar.getComponentCount();
        graphButtonsSeparator.setVisible(false);

        resultsView = new ResultsView();
        memoryTabPanel.add(resultsView, BorderLayout.CENTER);
        resultsView.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                if (currentDisplayComponent == memoryTabPanel) {
                    if (resultsView.getSelectedView() == historyPanel) {
                        for (JButton b : chartActions)
                            toolBar.add(b, chartButtonsOffset);
                        graphButtonsSeparator.setVisible(true);
                    } else {
                        for (JButton b : chartActions)
                            toolBar.remove(b);
                        graphButtonsSeparator.setVisible(false);
                    }
                    toolBar.getComponent().revalidate();
                    toolBar.getComponent().repaint();
                }
            }
        });

        historyPanel = new JPanel(new BorderLayout());

        hideContributors();
        //******************
        setFocusable(true);
        setRequestFocusEnabled(true);

        Profiler.getDefault().addProfilingStateListener(new ProfilingStateAdapter() {
            @Override
            public void instrumentationChanged(int oldInstrType, int currentInstrType) {
                requestProfilingDataUpdate(false);
            }

            @Override
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
        });
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
                        if (defaultLiveInstance.isShowing()) {
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
        if ((currentDisplayComponent == memoryTabPanel) && (resultsView.getSelectedView() == historyPanel)) {
            return UIUtils.createScreenshot(historyPanel);
        }

        if (currentDisplay == null) {
            return null;
        }

        return currentDisplay.getViewImage(onlyVisibleArea);
    }

    public String getViewName() {
        if ((currentDisplayComponent == memoryTabPanel) && (resultsView.getSelectedView() == historyPanel)) {
            return "memory-history-" + classHistoryManager.getTrackedClassName(); // NOI18N
        }

        if (currentDisplay == null) {
            return null;
        }

        return currentDisplay.getViewName();
    }
    
    protected Component defaultFocusOwner() {
        return currentDisplayComponent;
    }

    public boolean fitsVisibleArea() {
        if ((currentDisplayComponent == memoryTabPanel) && (resultsView.getSelectedView() == historyPanel)) {
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
        if ((currentDisplayComponent == memoryTabPanel) && (resultsView.getSelectedView() == historyPanel)) {
            return true;
        }

        return !noResultsPanel.isShowing() && (currentDisplay != null) && currentDisplay.hasView();
    }

    public void ideClosing() {
        hideContributors();
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
            // Temporary workaround to refresh sampling data when LiveResultsWindow is not refreshing
            // Temporary workaround to refresh lock contention data when LiveResultsWindow is not refreshing
            // TODO: move this code to a separate class performing the update if necessary
            Profiler profiler = Profiler.getDefault();
            final ProfilerClient client = profiler.getTargetAppRunner().getProfilerClient();
            if (NetBeansProfiler.getDefaultNB().processesProfilingPoints() 
                || client.getCurrentInstrType() == ProfilerEngineSettings.INSTR_NONE_SAMPLING
                || profiler.getLockContentionMonitoringEnabled()) {
                ProfilerUtils.runInProfilerRequestProcessor(new Runnable() {
                    public void run() {
                        callForceObtainedResultsDump(client, false);
                    }
                });
            }

            // -----------------------------------------------------------------------
            return false;
        }
    }

    public void exportData(int exportedFileType, ExportDataDumper eDD) {
        Component selectedView = resultsView.getSelectedView();
        if (currentDisplayComponent == memoryTabPanel) {
            if (selectedView instanceof LiveSampledResultsPanel) {
                ((LiveSampledResultsPanel) currentDisplay).exportData(exportedFileType, eDD, Bundle.LAB_ResultsWindowName());
            } else if (selectedView instanceof LiveAllocResultsPanel) {
                ((LiveAllocResultsPanel) currentDisplay).exportData(exportedFileType, eDD, Bundle.LAB_ResultsWindowName());
            } else if (selectedView instanceof LiveLivenessResultsPanel) {
                ((LiveLivenessResultsPanel) currentDisplay).exportData(exportedFileType, eDD, Bundle.LAB_ResultsWindowName());
            }
        } else if (currentDisplayComponent instanceof LiveFlatProfilePanel) {
            ((LiveFlatProfilePanel) currentDisplay).exportData(exportedFileType, eDD, Bundle.LAB_ResultsWindowName());
        }
    }

    public boolean hasLoadedSnapshot() {
        return false;
    }

    public boolean hasExportableView() {
        if ((currentDisplayComponent == memoryTabPanel) && (resultsView.getSelectedView() == currentDisplay)) {
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
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                updateResultsDisplay();
            }
        });
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
    private boolean callForceObtainedResultsDump(final ProfilerClient client) {
        return callForceObtainedResultsDump(client, true);
    }

    private boolean callForceObtainedResultsDump(final ProfilerClient client, final boolean refreshDisplay) {
        int instrType = client.getCurrentInstrType();
        if (!resultsAvailableinTA && instrType != ProfilerEngineSettings.INSTR_NONE_SAMPLING) { // if the results are not available in profiled application, return immediatelly
            return false;
        }
        if (refreshDisplay) {
            resultsDumpForced.set(true);
        }

        try {
            if (instrType == ProfilerEngineSettings.INSTR_NONE_MEMORY_SAMPLING) {
                resultsAvailable();
            } else if (instrType != ProfilerEngineSettings.INSTR_CODE_REGION) {
                client.forceObtainedResultsDump(true);
            }

            return true;
        } catch (ClientUtils.TargetAppOrVMTerminated targetAppOrVMTerminated) {
            return false;
        }
    }

    private boolean isProfiling() {
        TargetAppRunner runner = Profiler.getDefault().getTargetAppRunner();
        return runner.getProfilerClient().getCurrentInstrType() != ProfilerEngineSettings.INSTR_NONE;
    }

    private ProfilerToolbar createToolBar() {
        ProfilerToolbar tb = ProfilerToolbar.create(true);

        autoToggle = new JToggleButton(Icons.getIcon(GeneralIcons.UPDATE_AUTO));
        autoToggle.setSelected(true);
        autoToggle.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    autoRefresh = autoToggle.isSelected();
                }
            });
        autoToggle.setToolTipText(Bundle.LiveResultsWindow_UpdateResultsAutomaticallyTooltip());
        autoToggle.getAccessibleContext().setAccessibleName(Bundle.LiveResultsWindow_UpdateResultsAutomaticallyTooltip());

        updateNowButton = new JButton(Icons.getIcon(GeneralIcons.UPDATE_NOW));
        updateNowButton.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    requestProfilingDataUpdate(true);
                }
            });
        updateNowButton.setToolTipText(Bundle.LiveResultsWindow_UpdateResultsNowTooltip());
        updateNowButton.getAccessibleContext().setAccessibleName(Bundle.LiveResultsWindow_UpdateResultsNowTooltip());

        runGCButton = new JButton(Icons.getIcon(ProfilerIcons.RUN_GC));
        runGCButton.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    try {
                        Profiler.getDefault().getTargetAppRunner().runGC();
                    } catch (ClientUtils.TargetAppOrVMTerminated ex) {
                        ProfilerDialogs.displayWarning(ex.getMessage());
                        ProfilerLogger.log(ex.getMessage());
                    }

                    requestProfilingDataUpdate(true);
                }
            });
        runGCButton.setToolTipText(Bundle.LiveResultsWindow_RunGCTooltip());
        runGCButton.getAccessibleContext().setAccessibleName(Bundle.LiveResultsWindow_RunGCTooltip());

        // todo: add profiler listener to enable/disable buttons
        tb.add(autoToggle);
        tb.add(updateNowButton);
        tb.add(runGCButton);
        tb.add(ResetResultsAction.getInstance());
        tb.addSeparator();
        tb.add(TakeSnapshotAction.getInstance().getToolbarPresenter());
        tb.addSeparator();
        tb.add(new ExportAction(this, null));
        tb.add(new SaveViewAction(this));

        return tb;
    }

    private LiveResultsPanel preparePanelForInstrType(int instrumentationType) {
        HELP_CTX = HELP_CTX_DEFAULT;
        LiveResultsPanel aPanel = null;
        
        TargetAppRunner runner = Profiler.getDefault().getTargetAppRunner();
        switch (instrumentationType) {
            case ProfilerEngineSettings.INSTR_NONE_MEMORY_SAMPLING: {
                LiveSampledResultsPanel samplingPanel = new LiveSampledResultsPanel(runner,
                                                    memoryActionsHandler);
                currentDisplayComponent = memoryTabPanel;

                if (resultsView.getViewsCount() > 0) {
                    resultsView.removeViews();
                }

                resultsView.addView(Bundle.LiveResultsWindow_LiveResultsTabName(), null, null, samplingPanel, null);
                aPanel = samplingPanel;
                HELP_CTX = new HelpCtx(HELP_CTX_KEY_MEM);

                break;
            }
            case ProfilerEngineSettings.INSTR_OBJECT_ALLOCATIONS: {
                LiveAllocResultsPanel allocPanel = new LiveAllocResultsPanel(runner,
                                                    memoryActionsHandler,
                                                    historyActionsHandler,
                                                    classHistoryManager);
                currentDisplayComponent = memoryTabPanel;

                if (resultsView.getViewsCount() > 0) {
                    resultsView.removeViews();
                }

                resultsView.addView(Bundle.LiveResultsWindow_LiveResultsTabName(), null, null, allocPanel, null);
                resultsView.addView(Bundle.LiveResultsWindow_HistoryTabName(), null, null, historyPanel, null);
                resultsView.setViewEnabled(historyPanel, false);
                aPanel = allocPanel;
                HELP_CTX = new HelpCtx(HELP_CTX_KEY_MEM);

                break;
            }
            case ProfilerEngineSettings.INSTR_OBJECT_LIVENESS: {
                LiveLivenessResultsPanel livenessPanel = new LiveLivenessResultsPanel(runner,
                                                    memoryActionsHandler,
                                                    historyActionsHandler,
                                                    classHistoryManager);
                currentDisplayComponent = memoryTabPanel;

                if (resultsView.getViewsCount() > 0) {
                    resultsView.removeViews();
                }

                resultsView.addView(Bundle.LiveResultsWindow_LiveResultsTabName(), null, null, livenessPanel, null);
                resultsView.addView(Bundle.LiveResultsWindow_HistoryTabName(), null, null, historyPanel, null);
                resultsView.setViewEnabled(historyPanel, false);
                aPanel = livenessPanel;
                HELP_CTX = new HelpCtx(HELP_CTX_KEY_MEM);

                break;
            }
            case ProfilerEngineSettings.INSTR_RECURSIVE_FULL:
            case ProfilerEngineSettings.INSTR_RECURSIVE_SAMPLED: 
            case ProfilerEngineSettings.INSTR_NONE_SAMPLING: {
                Lookup.Provider project = NetBeansProfiler.getDefaultNB().getProfiledProject();

                boolean sampling = instrumentationType == ProfilerEngineSettings.INSTR_NONE_SAMPLING;
                final LiveFlatProfilePanel cpuPanel = new LiveFlatProfilePanel(runner, cpuActionsHandler, sampling);

                for(LiveResultsWindowContributor c : Lookup.getDefault().lookupAll(LiveResultsWindowContributor.class)) {
                    c.addToCpuResults(cpuPanel, toolBar, runner.getProfilerClient(), project);
                }

                currentDisplayComponent = cpuPanel;
                aPanel = cpuPanel;
                HELP_CTX = new HelpCtx(HELP_CTX_KEY_CPU);

                break;
            }
            case ProfilerEngineSettings.INSTR_CODE_REGION: {
                CodeRegionLivePanel regionPanel = new CodeRegionLivePanel(runner.getProfilerClient());
                currentDisplayComponent = regionPanel;
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

                    if (!isProfiling() || !isShowing()) {
                        return;
                    }

                    ProfilerUtils.runInProfilerRequestProcessor(new Runnable() {
                            public void run() {
                                // send a command to server to generate the newest live data
                                autoRefreshRequested.incrementAndGet();
                                callForceObtainedResultsDump(Profiler.getDefault().
                                        getTargetAppRunner().getProfilerClient());
                            }
                        });
                }
            });
    }
    
    private void resultsAvailable() {
        CommonUtils.runInEventDispatchThread(new Runnable() {

            @Override
            public void run() {
                resultsAvailable = true;
                if (resultsDumpForced.getAndSet(false) && 
                    autoRefreshRequested.getAndDecrement() > 0) {
                    updateResultsDisplay();
                } else {
                    autoRefreshRequested.compareAndSet(-1, 0);
                }
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
        HELP_CTX = HELP_CTX_DEFAULT;
    }

    private void updateActions(int newState) {
        runGCButton.setEnabled(newState == Profiler.PROFILING_RUNNING);
        updateNowButton.setEnabled(newState == Profiler.PROFILING_RUNNING);
    }

    private void refresh() {        
        if (LOGGER.isLoggable(Level.FINE) && (currentDisplayComponent != null)) {
            LOGGER.log(Level.FINE, "refreshing contributors for drilldown: {0}", currentDisplayComponent.getClass().getName()); // NOI18N
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
        if (!isShowing()) {
            return; // do nothing if i'm closed
        }

        if (!resultsAvailable) {
            currentDisplay = null;
            currentDisplayComponent = noResultsPanel;

            return;
        }

        TargetAppRunner runner = Profiler.getDefault().getTargetAppRunner();
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
