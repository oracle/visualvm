/*
 *  Copyright (c) 2007, 2023, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
 * 
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 * 
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 */
package org.graalvm.visualvm.profiler;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.lib.common.Profiler;
import org.graalvm.visualvm.lib.jfluid.client.ClientUtils;
import org.graalvm.visualvm.lib.profiler.actions.ResetResultsAction;
import org.graalvm.visualvm.lib.profiler.actions.TakeSnapshotAction;
import org.graalvm.visualvm.lib.profiler.api.ActionsSupport;
import org.graalvm.visualvm.lib.profiler.api.GoToSource;
import org.graalvm.visualvm.lib.profiler.api.icons.GeneralIcons;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.ProfilerIcons;
import org.graalvm.visualvm.lib.ui.components.ProfilerToolbar;
import org.graalvm.visualvm.lib.ui.cpu.LiveCPUView;
import org.graalvm.visualvm.lib.ui.cpu.LiveCPUViewUpdater;
import org.graalvm.visualvm.lib.ui.swing.FilterUtils;
import org.graalvm.visualvm.lib.ui.swing.GrayLabel;
import org.graalvm.visualvm.lib.ui.swing.MultiButtonGroup;
import org.graalvm.visualvm.lib.ui.swing.SearchUtils;
import org.graalvm.visualvm.profiling.actions.ProfilerResultsAction;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "MethodsFeatureUI_selectedMethods=Selected methods",
    "MethodsFeatureUI_liveResults=Results:",
    "MethodsFeatureUI_pauseResults=Pause live results",
    "MethodsFeatureUI_updateResults=Update live results",
    "MethodsFeatureUI_view=View:",
    "MethodsFeatureUI_viewForward=Forward calls",
    "MethodsFeatureUI_viewHotSpots=Hot spots",
    "MethodsFeatureUI_viewReverse=Reverse calls",
    "MethodsFeatureUI_resultsMode=Results mode",
    "MethodsFeatureUI_profilingData=Collected data:",
    "MethodsFeatureUI_snapshot=Snapshot",
    "MethodsFeatureUI_showAbsolute=Show absolute values",
    "MethodsFeatureUI_showDeltas=Show delta values"
})
class CPULivePanel extends ProfilingResultsSupport.ResultsView {
    
    private ProfilerToolbar toolbar;
    private LiveCPUView cpuView;
    private LiveCPUViewUpdater updater;
    private ProfilingResultsSupport.ResultsResetter resetter;
    
    
    CPULivePanel(Application application) {
        setLayout(new BorderLayout());
        setOpaque(false);
        
        initUI(application);
        
        add(toolbar.getComponent(), BorderLayout.NORTH);
        add(cpuView, BorderLayout.CENTER);
    }
    
    
    // -------------------------------------------------------------------------
    
    void refreshResults() {
        refreshResults(false);
    }
    
    void resetResults() {
        if (lrDeltasButton != null) {
            lrDeltasButton.setSelected(false);
            lrDeltasButton.setToolTipText(Bundle.MethodsFeatureUI_showDeltas());
        }
        if (cpuView != null) {
            cpuView.resetData();
            cpuView.setDiffView(false);
        }
    }
    
    void sessionStateChanged(int sessionState) {
        refreshToolbar(sessionState);
    }
    
    
    // -------------------------------------------------------------------------
    
    private void refreshResults(final boolean forceRefresh) {
        RESULTS_PROCESSOR.post(new Runnable() {
            public void run() {
                try {
                    if (updater != null) {
                        if (forceRefresh) updater.setForceRefresh(true);
                        updater.update();
                    }
//                } catch (ClientUtils.TargetAppOrVMTerminated ex) {
                } catch (Throwable t) {
                    cleanup();
                }
            }
        });
    }

    void cleanup() {
        if (updater != null) {
            updater.cleanup();
            updater = null;
        }

        if (resetter != null) {
            resetter.unregisterView(CPULivePanel.this);
            resetter = null;
        }
    }
    
    
    // -------------------------------------------------------------------------
    
    private JLabel lrLabel;
    private JToggleButton lrPauseButton;
    private JButton lrRefreshButton;
    private JToggleButton lrDeltasButton;
    
    private JLabel pdLabel;
    private JButton pdSnapshotButton;
    private JButton pdResetResultsButton;
    
    private boolean popupPause;
    private JToggleButton[] toggles;
    
    
    private void initUI(Application application) {
        
        assert SwingUtilities.isEventDispatchThread();
        
        // --- Results ---------------------------------------------------------
        
        cpuView = new LiveCPUView(null) {
            protected boolean showSourceSupported() {
                return GoToSource.isAvailable();
            }
            protected boolean profileMethodSupported() {
                return false;
            }
            protected boolean profileClassSupported() {
                return false;
            }
            protected void showSource(ClientUtils.SourceCodeSelection value) {
//                Lookup.Provider project = getProject();
                Lookup.Provider project = null;
                String className = value.getClassName();
                String methodName = value.getMethodName();
                String methodSig = value.getMethodSignature();
                GoToSource.openSource(project, className, methodName, methodSig);
            }
            protected void selectForProfiling(ClientUtils.SourceCodeSelection value) {
//                MethodsFeatureUI.this.selectForProfiling(value);
            }
            protected void popupShowing() {
                if (lrPauseButton.isEnabled() && !lrRefreshButton.isEnabled()) {
                    popupPause = true;
                    lrPauseButton.setSelected(true);
                }
            }
            protected void popupHidden() {
                if (lrPauseButton.isEnabled() && popupPause) {
                    popupPause = false;
                    lrPauseButton.setSelected(false);
                }
            }
            protected void foundInForwardCalls() {
                super.foundInForwardCalls();
                toggles[0].setSelected(true);
            }
            protected void foundInHotSpots() {
                super.foundInHotSpots();
                toggles[1].setSelected(true);
            }
            protected void foundInReverseCalls() {
                super.foundInReverseCalls();
                toggles[2].setSelected(true);
            }
        };
        cpuView.putClientProperty("HelpCtx.Key", "ProfileMethods.HelpCtx"); // NOI18N
        cpuView.putClientProperty(ProfilerResultsAction.PROP_APPLICATION, application);
        
        updater = new LiveCPUViewUpdater(cpuView, Profiler.getDefault().getTargetAppRunner().getProfilerClient());
        resetter = ProfilingResultsSupport.ResultsResetter.registerView(this);
        
        InputMap inputMap = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap actionMap = getActionMap();
        
        final String filterKey = FilterUtils.FILTER_ACTION_KEY;
        Action filterAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                Action action = cpuView.getActionMap().get(filterKey);
                if (action != null && action.isEnabled()) action.actionPerformed(e);
            }
        };
        ActionsSupport.registerAction(filterKey, filterAction, actionMap, inputMap);
        
        final String findKey = SearchUtils.FIND_ACTION_KEY;
        Action findAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                Action action = cpuView.getActionMap().get(findKey);
                if (action != null && action.isEnabled()) action.actionPerformed(e);
            }
        };
        ActionsSupport.registerAction(findKey, findAction, actionMap, inputMap);
        
        
        // --- Toolbar ---------------------------------------------------------
        
        lrLabel = new GrayLabel(Bundle.MethodsFeatureUI_liveResults());
            
        lrPauseButton = new JToggleButton(Icons.getIcon(GeneralIcons.PAUSE)) {
            protected void fireItemStateChanged(ItemEvent event) {
                boolean paused = isSelected();
                if (updater != null) updater.setPaused(paused);
                lrRefreshButton.setEnabled(paused && !popupPause);
                if (!paused) refreshResults(true);
            }
        };
        lrPauseButton.setToolTipText(Bundle.MethodsFeatureUI_pauseResults());
        
        lrRefreshButton = new JButton(Icons.getIcon(GeneralIcons.UPDATE_NOW)) {
            protected void fireActionPerformed(ActionEvent e) {
                refreshResults(true);
            }
        };
        lrRefreshButton.setToolTipText(Bundle.MethodsFeatureUI_updateResults());
        lrRefreshButton.setEnabled(false);
        
        Icon icon = Icons.getIcon(ProfilerIcons.DELTA_RESULTS);
        lrDeltasButton = new JToggleButton(icon) {
            protected void fireActionPerformed(ActionEvent e) {
                if (!cpuView.setDiffView(isSelected())) setSelected(false);
                setToolTipText(isSelected() ? Bundle.MethodsFeatureUI_showAbsolute() :
                                              Bundle.MethodsFeatureUI_showDeltas());
            }
        };
        lrDeltasButton.setToolTipText(Bundle.MethodsFeatureUI_showDeltas());
        
        MultiButtonGroup group = new MultiButtonGroup();
        toggles = new JToggleButton[3];
        
        JToggleButton forwardCalls = new JToggleButton(Icons.getIcon(ProfilerIcons.NODE_FORWARD)) {
            protected void fireActionPerformed(ActionEvent e) {
                super.fireActionPerformed(e);
                cpuView.setView(isSelected(), toggles[1].isSelected(), toggles[2].isSelected());
                refreshResults(true);
            }
        };
        forwardCalls.putClientProperty("JButton.buttonType", "segmented"); // NOI18N
        forwardCalls.putClientProperty("JButton.segmentPosition", "first"); // NOI18N
        forwardCalls.setToolTipText(Bundle.MethodsFeatureUI_viewForward());
        group.add(forwardCalls);
        toggles[0] = forwardCalls;
        forwardCalls.setSelected(true);
        
        JToggleButton hotSpots = new JToggleButton(Icons.getIcon(ProfilerIcons.TAB_HOTSPOTS)) {
            protected void fireActionPerformed(ActionEvent e) {
                super.fireActionPerformed(e);
                cpuView.setView(toggles[0].isSelected(), isSelected(), toggles[2].isSelected());
                refreshResults(true);
            }
        };
        hotSpots.putClientProperty("JButton.buttonType", "segmented"); // NOI18N
        hotSpots.putClientProperty("JButton.segmentPosition", "middle"); // NOI18N
        hotSpots.setToolTipText(Bundle.MethodsFeatureUI_viewHotSpots());
        group.add(hotSpots);
        toggles[1] = hotSpots;
        hotSpots.setSelected(false);
        
        JToggleButton reverseCalls = new JToggleButton(Icons.getIcon(ProfilerIcons.NODE_REVERSE)) {
            protected void fireActionPerformed(ActionEvent e) {
                super.fireActionPerformed(e);
                cpuView.setView(toggles[0].isSelected(), toggles[1].isSelected(), isSelected());
                refreshResults(true);
            }
        };
        reverseCalls.putClientProperty("JButton.buttonType", "segmented"); // NOI18N
        reverseCalls.putClientProperty("JButton.segmentPosition", "last"); // NOI18N
        reverseCalls.setToolTipText(Bundle.MethodsFeatureUI_viewReverse());
        group.add(reverseCalls);
        toggles[2] = reverseCalls;
        reverseCalls.setSelected(false);

        pdLabel = new GrayLabel(Bundle.MethodsFeatureUI_profilingData());

        pdSnapshotButton = new JButton(TakeSnapshotAction.getInstance());
//        pdSnapshotButton.setHideActionText(true);
        pdSnapshotButton.setText(Bundle.MethodsFeatureUI_snapshot());
        pdSnapshotButton.putClientProperty("JComponent.sizeVariant", "regular"); // NOI18N

        pdResetResultsButton = new JButton(ResetResultsAction.getInstance());
        pdResetResultsButton.setHideActionText(true);

        toolbar = ProfilerToolbar.create(true);

//        toolbar.addSpace(2);
//        toolbar.addSeparator();
        toolbar.addSpace(5);

        toolbar.add(lrLabel);
        toolbar.addSpace(2);
        toolbar.add(lrPauseButton);
        toolbar.add(lrRefreshButton);
        
        toolbar.addSpace(5);
        toolbar.add(lrDeltasButton);
        
        toolbar.addSpace(2);
//        toolbar.addSeparator();
        toolbar.addSpace(5);
        
        toolbar.add(new GrayLabel(Bundle.MethodsFeatureUI_view()));
        toolbar.addSpace(2);
        toolbar.add(forwardCalls);
        toolbar.add(hotSpots);
        toolbar.add(reverseCalls);
        
        toolbar.addSpace(5);
        toolbar.add(cpuView.createThreadSelector());

        toolbar.addSpace(2);
        toolbar.addSeparator();
        toolbar.addSpace(5);

        toolbar.add(pdLabel);
        toolbar.addSpace(2);
        toolbar.add(pdSnapshotButton);
        toolbar.addSpace(3);
        toolbar.add(pdResetResultsButton);
        
        
        cpuView.setView(true, false, false);
        
    }
    
    private void refreshToolbar(final int state) {
        if (toolbar != null) SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                boolean running = state == Profiler.PROFILING_RUNNING;
                lrPauseButton.setEnabled(running);
                lrRefreshButton.setEnabled(!popupPause && running && lrPauseButton.isSelected());
                lrDeltasButton.setEnabled(running);
            }
        });
    }
    
}
