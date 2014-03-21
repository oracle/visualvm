/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2014 Oracle and/or its affiliates. All rights reserved.
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

package org.netbeans.modules.profiler.v2.features;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.TargetAppRunner;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.lib.profiler.common.ProfilingSettingsPresets;
import org.netbeans.lib.profiler.common.filters.SimpleFilter;
import org.netbeans.lib.profiler.ui.components.ProfilerToolbar;
import org.netbeans.lib.profiler.ui.cpu.CPUView;
import org.netbeans.modules.profiler.actions.ResetResultsAction;
import org.netbeans.modules.profiler.actions.TakeSnapshotAction;
import org.netbeans.modules.profiler.api.icons.GeneralIcons;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import org.netbeans.modules.profiler.api.project.ProjectContentsSupport;
import org.netbeans.modules.profiler.utilities.ProfilerUtils;
import org.netbeans.modules.profiler.v2.session.ProjectSession;
import org.netbeans.modules.profiler.v2.ui.components.GrayLabel;
import org.netbeans.modules.profiler.v2.ui.components.PopupButton;
import org.netbeans.modules.profiler.v2.ui.components.SmallButton;
import org.netbeans.modules.profiler.v2.ui.components.TitledMenuSeparator;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "CPUFeature_name=CPU",
    "CPUFeature_lrLabel=Live results:",
    "CPUFeature_viewHotSpots=Hot Spots",
    "CPUFeature_viewCallTree=Call Tree",
    "CPUFeature_viewCombined=Combined",
    "CPUFeature_pdLabel=Profiling data:", 
    "CPUFeature_snapshot=Snapshot", 
    "CPUFeature_apLabel=Application:", 
    "CPUFeature_threadDump=Thread Dump"
})
final class CPUFeature extends ProfilerFeature.Basic {
    
    private static enum View { HOT_SPOTS, CALL_TREE, COMBINED }
    private static enum Mode { SAMPLED_ALL, SAMPLED_PROJECT, INSTR_CLASS, INSTR_METHOD, INSTR_SELECTED }
    
    private JLabel lrLabel;
    private JToggleButton lrPauseButton;
    private JButton lrRefreshButton;
    private PopupButton lrView;
    
    private JLabel pdLabel;
    private JButton pdSnapshotButton;
    private JButton pdResetResultsButton;
    
    private JLabel apLabel;
    private JButton apThreadDumpButton;
    
    private ProfilerToolbar toolbar;
    private JPanel settingsUI;
    
    private View view;
    
    private CPUView cpuView;
    
    private Mode mode = Mode.SAMPLED_ALL;
    private PopupButton modeButton;
    
    
    CPUFeature() {
        super(Bundle.CPUFeature_name(), Icons.getIcon(ProfilerIcons.CPU));
    }

    
    public JPanel getResultsUI() {
        if (cpuView == null) initResultsUI();
        return cpuView;
    }
    
    public JPanel getSettingsUI() {
        if (settingsUI == null) {
            settingsUI = new JPanel();
            settingsUI.setOpaque(false);
            settingsUI.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            settingsUI.setLayout(new BoxLayout(settingsUI, BoxLayout.LINE_AXIS));
            
            settingsUI.setVisible(false); // TODO: should restore last state

            settingsUI.add(new JLabel("Profile:"));

            settingsUI.add(Box.createHorizontalStrut(5));

            modeButton = new PopupButton("All classes") {
                protected void populatePopup(JPopupMenu popup) {
                    popup.add(new TitledMenuSeparator("Quick (sampled)"));
                    popup.add(new JRadioButtonMenuItem(getModeName(Mode.SAMPLED_ALL), mode == Mode.SAMPLED_ALL) {
                        protected void fireActionPerformed(ActionEvent e) { setMode(Mode.SAMPLED_ALL); }
                    });
                    popup.add(new JRadioButtonMenuItem(getModeName(Mode.SAMPLED_PROJECT), mode == Mode.SAMPLED_PROJECT) {
                        protected void fireActionPerformed(ActionEvent e) { setMode(Mode.SAMPLED_PROJECT); }
                    });
                    
                    popup.add(new TitledMenuSeparator("Detailed (instrumented)"));
                    popup.add(new JRadioButtonMenuItem(getModeName(Mode.INSTR_CLASS), mode == Mode.INSTR_CLASS) {
                        protected void fireActionPerformed(ActionEvent e) { setMode(Mode.INSTR_CLASS); }
                    });
                    popup.add(new JRadioButtonMenuItem(getModeName(Mode.INSTR_METHOD), mode == Mode.INSTR_METHOD) {
                        protected void fireActionPerformed(ActionEvent e) { setMode(Mode.INSTR_METHOD); }
                    });
                    popup.add(new JRadioButtonMenuItem(getModeName(Mode.INSTR_SELECTED), mode == Mode.INSTR_SELECTED) {
                        { setEnabled(cpuView.hasSelection()); }
                        protected void fireActionPerformed(ActionEvent e) { setMode(Mode.INSTR_SELECTED); }
                    });
                }
            };
            settingsUI.add(modeButton);

            settingsUI.add(Box.createHorizontalStrut(5));


            settingsUI.add(Box.createGlue());


            settingsUI.add(new JLabel("Overhead:"));

            settingsUI.add(Box.createHorizontalStrut(5));

            settingsUI.add(new JProgressBar() {
                public Dimension getPreferredSize() {
                    Dimension d = super.getPreferredSize();
                    d.width = 80;
                    return d;
                }
                public Dimension getMaximumSize() {
                    return getPreferredSize();
                }
            });

            settingsUI.add(Box.createHorizontalStrut(5));

            Component sep1 = Box.createHorizontalStrut(1);
            sep1.setBackground(Color.GRAY);
            if (sep1 instanceof JComponent) ((JComponent)sep1).setOpaque(true);
            Dimension d = sep1.getMaximumSize();
            d.height = 20;
            sep1.setMaximumSize(d);
            settingsUI.add(sep1);

            settingsUI.add(Box.createHorizontalStrut(5));

            settingsUI.add(new SmallButton("Apply") {
                protected void fireActionPerformed(ActionEvent e) {
                    cpuView.resetData();
                    fireChange();
                    settingsUI.setVisible(false);
                    
                    // Proof of concept, show Call Tree when switching to root methods
                    if (mode == Mode.INSTR_SELECTED && view == View.HOT_SPOTS) {
                        if (processor != null && !processor.isShutdown())
                            setView(View.CALL_TREE);
                    }
                }
            });

            settingsUI.add(Box.createHorizontalStrut(5));

            settingsUI.add(new SmallButton("Cancel") {
                protected void fireActionPerformed(ActionEvent e) {
                    // TODO: clear changes
                    settingsUI.setVisible(false);
                }
            });
        }
        return settingsUI;
    }
    
    private void setMode(Mode m) {
        if (mode == m) return;
        mode = m;
        modeButton.setText(getModeName(m));
    }
    
    private String getModeName(Mode m) {
        switch (m) {
            case SAMPLED_ALL: return "All classes";
            case SAMPLED_PROJECT: return "Project classes";
            case INSTR_CLASS: return "Single class";
            case INSTR_METHOD: return "Single method";
            case INSTR_SELECTED: return "Selected methods";
        }
        return null;
    }
    
    public ProfilerToolbar getToolbar() {
        if (toolbar == null) {
            lrLabel = new GrayLabel(Bundle.CPUFeature_lrLabel());
            
            lrPauseButton = new JToggleButton(Icons.getIcon(GeneralIcons.PAUSE)) {
                protected void fireItemStateChanged(ItemEvent event) {
                    if (!isSelected()) refreshResults();
                    else skipRefresh = true;
                    refreshToolbar();
                }
            };
            lrPauseButton.setEnabled(false);
            
            lrRefreshButton = new JButton(Icons.getIcon(GeneralIcons.UPDATE_NOW)) {
                protected void fireActionPerformed(ActionEvent e) {
                    refreshResults();
                }
            };
            
            lrView = new PopupButton(Bundle.CPUFeature_viewHotSpots()) {
                protected void populatePopup(JPopupMenu popup) { populateViews(popup); }
            };
            
            pdLabel = new GrayLabel(Bundle.CPUFeature_pdLabel());
            
            pdSnapshotButton = new JButton(TakeSnapshotAction.getInstance());
            pdSnapshotButton.setHideActionText(true);
            pdSnapshotButton.setText(Bundle.CPUFeature_snapshot());
            
            pdResetResultsButton = new JButton(ResetResultsAction.getInstance()) {
                protected void fireActionPerformed(ActionEvent e) {
                    cpuView.resetData();
                    super.fireActionPerformed(e);
                }
            };
            pdResetResultsButton.setHideActionText(true);
            
            apLabel = new GrayLabel(Bundle.CPUFeature_apLabel());
            
            apThreadDumpButton = new JButton(Bundle.CPUFeature_threadDump(), Icons.getIcon(ProfilerIcons.WINDOW_THREADS));
            apThreadDumpButton.setEnabled(false);
            
            toolbar = ProfilerToolbar.create(true);
            
            toolbar.addSpace(2);
            toolbar.addSeparator();
            toolbar.addSpace(5);
            
            toolbar.add(lrLabel);
            toolbar.addSpace(2);
            toolbar.add(lrPauseButton);
            toolbar.add(lrRefreshButton);
            toolbar.add(lrView);
            
            toolbar.addSpace(2);
            toolbar.addSeparator();
            toolbar.addSpace(5);
            
            toolbar.add(pdLabel);
            toolbar.addSpace(2);
            toolbar.add(pdSnapshotButton);
            toolbar.add(pdResetResultsButton);
            
            toolbar.addSpace(2);
            toolbar.addSeparator();
            toolbar.addSpace(5);
            
            toolbar.add(apLabel);
            toolbar.addSpace(2);
            toolbar.add(apThreadDumpButton);
            
            refreshToolbar();
        }
        
        return toolbar;
    }
    
    public ProfilingSettings getSettings() {
        ProjectSession session = getSession();
        if (session == null) return null;
        
        ProfilingSettings settings = null;
        
        switch (mode)  {
            case SAMPLED_ALL:
                settings = ProfilingSettingsPresets.createCPUPreset();
                break;
                
            case SAMPLED_PROJECT:
                settings = ProfilingSettingsPresets.createCPUPreset();
                
                ProjectContentsSupport pcs = ProjectContentsSupport.get(session.getProject());
                String filter = pcs.getInstrumentationFilter(false);
                SimpleFilter f = new SimpleFilter("", SimpleFilter.SIMPLE_FILTER_INCLUSIVE, filter); // NOI18N
                settings.setSelectedInstrumentationFilter(f);
                break;
                
            case INSTR_SELECTED:
                settings = ProfilingSettingsPresets.createCPUPreset(ProfilingSettings.PROFILE_CPU_PART);
                settings.setThreadCPUTimerOn(true);
                
                ClientUtils.SourceCodeSelection[] selections = cpuView.getSelections();
                if (selections.length > 0) settings.addRootMethods(selections);
                break;
        }
        
        if (settings == null) settings = ProfilingSettingsPresets.createCPUPreset();
        return settings;
    }
    
    private void populateViews(JPopupMenu popup) {
        popup.add(new JRadioButtonMenuItem(Bundle.CPUFeature_viewHotSpots(), getView() == View.HOT_SPOTS) {
            protected void fireActionPerformed(ActionEvent e) { setView(View.HOT_SPOTS); }
        });
        
        popup.add(new JRadioButtonMenuItem(Bundle.CPUFeature_viewCallTree(), getView() == View.CALL_TREE) {
            protected void fireActionPerformed(ActionEvent e) { setView(View.CALL_TREE); }
        });
        
        popup.add(new JRadioButtonMenuItem(Bundle.CPUFeature_viewCombined(), getView() == View.COMBINED) {
            protected void fireActionPerformed(ActionEvent e) { setView(View.COMBINED); }
        });
    }

    private void setView(View view) {
        if (view == this.view) return;
        
        this.view = view;
        
        switch (view) {
            case HOT_SPOTS:
                cpuView.setView(false, true);
                lrView.setText(Bundle.CPUFeature_viewHotSpots());
                break;
            case CALL_TREE:
                cpuView.setView(true, false);
                lrView.setText(Bundle.CPUFeature_viewCallTree());
                break;
            case COMBINED:
                cpuView.setView(true, true);
                lrView.setText(Bundle.CPUFeature_viewCombined());
                break;
        }
        
        refreshResults();
    }
    
    private View getView() {
        return view;
    }
    
    private void initResultsUI() {
        TargetAppRunner runner = Profiler.getDefault().getTargetAppRunner();
        cpuView = new CPUView(runner.getProfilerClient());
        setView(View.HOT_SPOTS);
    }
    
    private void refreshToolbar() {
        ProjectSession session = getSession();
        refreshToolbar(session == null ? null : session.getState());
    }
    
    private void refreshToolbar(final ProjectSession.State state) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                boolean running = state == ProjectSession.State.RUNNING;
                lrPauseButton.setEnabled(running);
                lrRefreshButton.setEnabled(running && lrPauseButton.isSelected());
                
                boolean inactive = state == ProjectSession.State.INACTIVE;
                lrLabel.setEnabled(!inactive);
                pdLabel.setEnabled(!inactive);
                apLabel.setEnabled(!inactive);
            }
        });
    }
    
    public void stateChanged(ProjectSession.State oldState, ProjectSession.State newState) {
        if (newState == null || newState == ProjectSession.State.INACTIVE) {
            stopResults();
        } else if (newState == ProjectSession.State.RUNNING) {
            startResults();
        }
        refreshToolbar(newState);
    }
    
    private RequestProcessor processor;
    private Runnable refresher;
    private boolean forceRefresh;
    private boolean skipRefresh;
    
    private void startResults() {
        if (processor != null) return;
        
        if (cpuView != null) cpuView.resetData();
        
        processor = new RequestProcessor("CPU Data Refresher"); // NOI18N
        
        refresher = new Runnable() {
            public void run() {
                if (cpuView != null) {
                    ProfilerUtils.runInProfilerRequestProcessor(new Runnable() {
                        public void run() {
                            try {
                                if (skipRefresh) skipRefresh = false;
                                else cpuView.refreshData();
                            } catch (ClientUtils.TargetAppOrVMTerminated ex) {
                                stopResults();
                            }
                        }
                    });
                }
                
                refreshResults(1500);
            }
        };
        
        refreshResults();
    }
    
    private void refreshResults() {
        skipRefresh = false;
        forceRefresh = true;
        refreshResults(0);
    }
    
    private void refreshResults(int delay) {
        // TODO: needs synchronization!
        if (processor != null && !processor.isShutdown()) {
            if (forceRefresh || lrPauseButton == null || !lrPauseButton.isSelected()) {
                processor.post(refresher, delay);
                forceRefresh = false;
            }
        }
    }
    
    private void stopResults() {
        if (processor != null) {
            processor.shutdownNow();
            processor = null;
            refresher = null;
        }
    }
    
//    public void attachedToSession(ProjectSession session) {
//        super.attachedToSession(session);
////        if (cpuView != null) tableView.resetData();
//    }
//    
//    public void detachedFromSession(ProjectSession session) {
//        super.detachedFromSession(session);
////        if (cpuView != null) tableView.resetData();
//    }
    
}
