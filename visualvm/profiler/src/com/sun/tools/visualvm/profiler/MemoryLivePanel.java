/*
 *  Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tools.visualvm.profiler;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.profiling.actions.ProfilerResultsAction;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.ui.components.ProfilerToolbar;
import org.netbeans.lib.profiler.ui.memory.LiveMemoryView;
import org.netbeans.lib.profiler.ui.memory.LiveMemoryViewUpdater;
import org.netbeans.lib.profiler.ui.swing.GrayLabel;
import org.netbeans.modules.profiler.actions.ResetResultsAction;
import org.netbeans.modules.profiler.actions.TakeSnapshotAction;
import org.netbeans.modules.profiler.api.GoToSource;
import org.netbeans.modules.profiler.api.icons.GeneralIcons;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "ObjectsFeatureUI_liveResults=Results:",
    "ObjectsFeatureUI_pauseResults=Pause live results",
    "ObjectsFeatureUI_updateResults=Update live results",
    "ObjectsFeatureUI_profilingData=Collected data:",
    "ObjectsFeatureUI_snapshot=Snapshot",
    "ObjectsFeatureUI_showAbsolute=Show absolute values",
    "ObjectsFeatureUI_showDeltas=Show delta values"
})
class MemoryLivePanel extends ProfilingResultsSupport.ResultsView {
    
    private ProfilerToolbar toolbar;
    private LiveMemoryView memoryView;
    private LiveMemoryViewUpdater updater;
    private ProfilingResultsSupport.ResultsResetter resetter;
    
    
    MemoryLivePanel(Application application) {
        setLayout(new BorderLayout());
        setOpaque(false);
        
        initUI(application);
        
        add(toolbar.getComponent(), BorderLayout.NORTH);
        add(memoryView, BorderLayout.CENTER);
    }
    
    
    // -------------------------------------------------------------------------
    
    void refreshResults() {
        refreshResults(false);
    }
    
    void resetResults() {
        if (lrDeltasButton != null) {
            lrDeltasButton.setSelected(false);
            lrDeltasButton.setToolTipText(Bundle.ObjectsFeatureUI_showDeltas());
        }
        if (memoryView != null) {
            memoryView.resetData();
            memoryView.setDiffView(false);
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
                    if (updater != null) {
                        updater.cleanup();
                        updater = null;
                    }

                    if (resetter != null) {
                        resetter.unregisterView(MemoryLivePanel.this);
                        resetter = null;
                    }
                }
            }
        });
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
    
    
    private void initUI(Application application) {
        
        assert SwingUtilities.isEventDispatchThread();
        
        // --- Results ---------------------------------------------------------
        
        memoryView = new LiveMemoryView(null) {
            protected boolean showSourceSupported() {
                return GoToSource.isAvailable();
            }
            protected boolean profileClassSupported() {
                return false;
            }
            protected void showSource(ClientUtils.SourceCodeSelection value) {
                Lookup.Provider project = null;
                String className = value.getClassName();
                String methodName = value.getMethodName();
                String methodSig = value.getMethodSignature();
                GoToSource.openSource(project, className, methodName, methodSig);
            }
            protected void selectForProfiling(ClientUtils.SourceCodeSelection value) {
//                ObjectsFeatureUI.this.selectForProfiling(value);
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
        };
        memoryView.putClientProperty("HelpCtx.Key", "ProfileObjects.HelpCtx"); // NOI18N
        memoryView.putClientProperty(ProfilerResultsAction.PROP_APPLICATION, application);
        
        updater = new LiveMemoryViewUpdater(memoryView, Profiler.getDefault().getTargetAppRunner().getProfilerClient());        
        resetter = ProfilingResultsSupport.ResultsResetter.registerView(this);
        
        // --- Toolbar ---------------------------------------------------------
        
        lrLabel = new GrayLabel(Bundle.ObjectsFeatureUI_liveResults());

        lrPauseButton = new JToggleButton(Icons.getIcon(GeneralIcons.PAUSE)) {
            protected void fireItemStateChanged(ItemEvent event) {
                boolean paused = isSelected();
                updater.setPaused(paused);
                lrRefreshButton.setEnabled(paused && !popupPause);
                if (!paused) refreshResults(true);
            }
        };
        lrPauseButton.setToolTipText(Bundle.ObjectsFeatureUI_pauseResults());

        lrRefreshButton = new JButton(Icons.getIcon(GeneralIcons.UPDATE_NOW)) {
            protected void fireActionPerformed(ActionEvent e) {
                refreshResults(true);
            }
        };
        lrRefreshButton.setToolTipText(Bundle.ObjectsFeatureUI_updateResults());
        lrRefreshButton.setEnabled(false);
        
        Icon icon = Icons.getIcon(ProfilerIcons.DELTA_RESULTS);
        lrDeltasButton = new JToggleButton(icon) {
            protected void fireActionPerformed(ActionEvent e) {
                if (!memoryView.setDiffView(isSelected())) setSelected(false);
                setToolTipText(isSelected() ? Bundle.ObjectsFeatureUI_showAbsolute() :
                                              Bundle.ObjectsFeatureUI_showDeltas());
            }
        };
        lrDeltasButton.setToolTipText(Bundle.ObjectsFeatureUI_showDeltas());

        pdLabel = new GrayLabel(Bundle.ObjectsFeatureUI_profilingData());

        pdSnapshotButton = new JButton(TakeSnapshotAction.getInstance());
//        pdSnapshotButton.setHideActionText(true);
        pdSnapshotButton.setText(Bundle.ObjectsFeatureUI_snapshot());

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
        toolbar.addSeparator();
        toolbar.addSpace(5);

        toolbar.add(pdLabel);
        toolbar.addSpace(2);
        toolbar.add(pdSnapshotButton);
        toolbar.addSpace(3);
        toolbar.add(pdResetResultsButton);
        
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
