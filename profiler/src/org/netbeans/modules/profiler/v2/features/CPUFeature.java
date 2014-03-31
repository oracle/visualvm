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
import java.util.HashSet;
import java.util.Set;
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
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.TargetAppRunner;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.lib.profiler.common.ProfilingSettingsPresets;
import org.netbeans.lib.profiler.common.filters.SimpleFilter;
import org.netbeans.lib.profiler.ui.components.JExtendedSpinner;
import org.netbeans.lib.profiler.ui.components.ProfilerToolbar;
import org.netbeans.lib.profiler.ui.cpu.CPUView;
import org.netbeans.lib.profiler.utils.Wildcards;
import org.netbeans.modules.profiler.actions.ResetResultsAction;
import org.netbeans.modules.profiler.actions.TakeSnapshotAction;
import org.netbeans.modules.profiler.api.GoToSource;
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
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

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
    
    private Component instrSettingsSpace;
    private JLabel selectedLabel;
    private Component selectedSpace1;
    private Component selectedSeparator;
    private Component selectedSpace2;
    private JLabel outgoingLabel;
    private Component outgoingSpace;
    private JSpinner outgoingSpinner;
    
    private View view;
    
    private CPUView cpuView;
    
    private Mode mode = Mode.SAMPLED_ALL;
    private PopupButton modeButton;
    
    private boolean popupPause;
    
    private ClientUtils.SourceCodeSelection[] selectedClasses;
    private ClientUtils.SourceCodeSelection[] selectedMethods;
    
    private final Set<ClientUtils.SourceCodeSelection> selection;
    
    
    CPUFeature() {
        super(Bundle.CPUFeature_name(), Icons.getIcon(ProfilerIcons.CPU));
        
        selection = new HashSet() {
            public boolean add(Object value) {
                boolean _add = super.add(value);
                selectionChanged();
                return _add;
            }
            public boolean remove(Object value) {
                boolean _remove = super.remove(value);
                selectionChanged();
                return _remove;
            }
            public void clear() {
                super.clear();
                selectionChanged();
            }
        };
    }
    
    
    private void profileSingle(ClientUtils.SourceCodeSelection selection) {
        if (Wildcards.ALLWILDCARD.equals(selection.getMethodName())) {
            selectedClasses = new ClientUtils.SourceCodeSelection[] { selection };
            setMode(Mode.INSTR_CLASS);
        } else {
            selectedMethods = new ClientUtils.SourceCodeSelection[] { selection };
            setMode(Mode.INSTR_METHOD);
        }
        updateModeUI();
        getSettingsUI().setVisible(true);
    }
    
    private void selectForProfiling(ClientUtils.SourceCodeSelection[] selection) {
        for (ClientUtils.SourceCodeSelection selected : selection)
            this.selection.add(selected);
        setMode(Mode.INSTR_SELECTED);
        updateModeUI();
        getSettingsUI().setVisible(true);
    }
    
    
    private void selectionChanged() {
        cpuView.refreshSelection();
        updateModeUI();
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
                    popup.add(new TitledMenuSeparator("General (sampled)"));
                    popup.add(new JRadioButtonMenuItem(getModeName(Mode.SAMPLED_ALL), mode == Mode.SAMPLED_ALL) {
                        protected void fireActionPerformed(ActionEvent e) { setMode(Mode.SAMPLED_ALL); }
                    });
                    popup.add(new JRadioButtonMenuItem(getModeName(Mode.SAMPLED_PROJECT), mode == Mode.SAMPLED_PROJECT) {
                        protected void fireActionPerformed(ActionEvent e) { setMode(Mode.SAMPLED_PROJECT); }
                    });
                    
                    popup.add(new TitledMenuSeparator("Focused (instrumented)"));
                    popup.add(new JRadioButtonMenuItem(getModeName(Mode.INSTR_CLASS), mode == Mode.INSTR_CLASS) {
                        protected void fireActionPerformed(ActionEvent e) { setMode(Mode.INSTR_CLASS); }
                    });
                    popup.add(new JRadioButtonMenuItem(getModeName(Mode.INSTR_METHOD), mode == Mode.INSTR_METHOD) {
                        protected void fireActionPerformed(ActionEvent e) { setMode(Mode.INSTR_METHOD); }
                    });
                    popup.add(new JRadioButtonMenuItem(getModeName(Mode.INSTR_SELECTED), mode == Mode.INSTR_SELECTED) {
//                        { setEnabled(!selection.isEmpty()); }
                        protected void fireActionPerformed(ActionEvent e) { setMode(Mode.INSTR_SELECTED); }
                    });
                }
            };
            settingsUI.add(modeButton);            
            
            instrSettingsSpace = settingsUI.add(Box.createHorizontalStrut(8));
            
            selectedLabel = new JLabel();
            settingsUI.add(selectedLabel);
            
            selectedSpace1 = settingsUI.add(Box.createHorizontalStrut(8));
            
            selectedSeparator = Box.createHorizontalStrut(1);
            selectedSeparator.setBackground(Color.GRAY);
            if (selectedSeparator instanceof JComponent) ((JComponent)selectedSeparator).setOpaque(true);
            Dimension d = selectedSeparator.getMaximumSize();
            d.height = 20;
            selectedSeparator.setMaximumSize(d);
            settingsUI.add(selectedSeparator);
            
            selectedSpace2 = settingsUI.add(Box.createHorizontalStrut(8));
            
            outgoingLabel = new JLabel("Outgoing calls:");
            settingsUI.add(outgoingLabel);
            
            outgoingSpace = settingsUI.add(Box.createHorizontalStrut(5));
            
            outgoingSpinner = new JExtendedSpinner(new SpinnerNumberModel(5, 1, 10, 1)) {
                public Dimension getPreferredSize() { return getMinimumSize(); }
                public Dimension getMaximumSize() { return getMinimumSize(); }
            };
            settingsUI.add(outgoingSpinner);


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

            settingsUI.add(Box.createHorizontalStrut(8));

            Component sep1 = Box.createHorizontalStrut(1);
            sep1.setBackground(Color.GRAY);
            if (sep1 instanceof JComponent) ((JComponent)sep1).setOpaque(true);
            Dimension dd = sep1.getMaximumSize();
            dd.height = 20;
            sep1.setMaximumSize(dd);
            settingsUI.add(sep1);

            settingsUI.add(Box.createHorizontalStrut(8));

            settingsUI.add(new SmallButton("Apply") {
                protected void fireActionPerformed(ActionEvent e) {
                    stopResults();
                    resetResults();
                    fireChange();
//                    settingsUI.setVisible(false);
                    
                    // Proof of concept, show Call Tree when switching to root methods
                    if (isInstrumentation() && running && refresher != null)
                        setView(View.CALL_TREE);
                }
            });

            settingsUI.add(Box.createHorizontalStrut(5));

            settingsUI.add(new SmallButton("Cancel") {
                protected void fireActionPerformed(ActionEvent e) {
                    // TODO: clear changes
                    settingsUI.setVisible(false);
                }
            });
            
            updateModeUI();
        }
        return settingsUI;
    }
    
    private void setMode(Mode m) {
        if (mode == m) return;
        mode = m;
        updateModeUI();
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
    
    private void updateModeUI() {
        modeButton.setText(getModeName(mode));
        
        boolean instrumentation = isInstrumentation();
        instrSettingsSpace.setVisible(instrumentation);
        selectedLabel.setVisible(instrumentation);
        selectedSpace1.setVisible(instrumentation);
        selectedSeparator.setVisible(instrumentation);
        selectedSpace2.setVisible(instrumentation);
        outgoingLabel.setVisible(instrumentation);
        outgoingSpace.setVisible(instrumentation);
        outgoingSpinner.setVisible(instrumentation);
        
        if (mode == Mode.INSTR_CLASS) {
            int count = selectedClasses == null ? 0 : selectedClasses.length;
            if (count == 0) {
                selectedLabel.setText("No class");
            } else if (count == 1) {
                selectedLabel.setText(selectedClasses[0].getClassName());
            } else {
                selectedLabel.setText(count + " classes");
            }
        } else if (mode == Mode.INSTR_METHOD) {
            int count = selectedMethods == null ? 0 : selectedMethods.length;
            if (count == 0) {
                selectedLabel.setText("No method");
            } else if (count == 1) {
                selectedLabel.setText(selectedMethods[0].getClassName() + "." + selectedMethods[0].getMethodName());
            } else {
                selectedLabel.setText(count + " methods");
            }
        } else if (mode == Mode.INSTR_SELECTED) {
            int count = selection.size();
            if (count == 0) {
                selectedLabel.setText("No method");
            } else if (count == 1) {
                ClientUtils.SourceCodeSelection sel = selection.iterator().next();
                selectedLabel.setText(sel.getClassName() + "." + sel.getMethodName());
            } else {
                selectedLabel.setText(count + " methods");
            }
        }
    }
    
    public ProfilerToolbar getToolbar() {
        if (toolbar == null) {
            lrLabel = new GrayLabel(Bundle.CPUFeature_lrLabel());
            
            lrPauseButton = new JToggleButton(Icons.getIcon(GeneralIcons.PAUSE)) {
                protected void fireItemStateChanged(ItemEvent event) {
                    paused = isSelected();
                    if (!paused) refreshResults();
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
                    resetResults();
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
                SimpleFilter f = new SimpleFilter("Project only classes",
                                 SimpleFilter.SIMPLE_FILTER_INCLUSIVE, filter); // NOI18N
                settings.setSelectedInstrumentationFilter(f);
                break;
                
            case INSTR_CLASS:
            case INSTR_METHOD:
                settings = ProfilingSettingsPresets.createCPUPreset(ProfilingSettings.PROFILE_CPU_PART);
                settings.setThreadCPUTimerOn(true);
                
                ClientUtils.SourceCodeSelection[] sel = mode == Mode.INSTR_CLASS ? selectedClasses :
                                                                                   selectedMethods;
                if (selection != null) settings.addRootMethods(sel);
                settings.setStackDepthLimit(((Number)outgoingSpinner.getValue()).intValue());
                break;
                
            case INSTR_SELECTED:
                settings = ProfilingSettingsPresets.createCPUPreset(ProfilingSettings.PROFILE_CPU_PART);
                settings.setThreadCPUTimerOn(true);
                
                ClientUtils.SourceCodeSelection[] selections = selection.toArray(
                        new ClientUtils.SourceCodeSelection[selection.size()]);
                if (selections != null) settings.addRootMethods(selections);
                settings.setStackDepthLimit(((Number)outgoingSpinner.getValue()).intValue());
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
        
        cpuView = new CPUView(runner.getProfilerClient(), selection, GoToSource.isAvailable()) {
            public void showSource(ClientUtils.SourceCodeSelection value) {
                Lookup.Provider project = getSession().getProject();
                String className = value.getClassName();
                String methodName = value.getMethodName();
                String methodSig = value.getMethodSignature();
                GoToSource.openSource(project, className, methodName, methodSig);
            }
            public void profileSingle(ClientUtils.SourceCodeSelection value) {
                CPUFeature.this.profileSingle(value);
            }
            public void selectForProfiling(ClientUtils.SourceCodeSelection[] value) {
                CPUFeature.this.selectForProfiling(value);
            }
            public void popupShowing() {
                if (lrPauseButton.isEnabled() && !lrRefreshButton.isEnabled()) {
                    popupPause = true;
                    lrPauseButton.setSelected(true);
                }
            }
            public void popupHidden() {
                if (lrPauseButton.isEnabled() && popupPause) {
                    popupPause = false;
                    lrPauseButton.setSelected(false);
                }
            }
        };
        
        setView(View.HOT_SPOTS);
    }
    
    private void refreshToolbar() {
        ProjectSession session = getSession();
        refreshToolbar(session == null ? null : session.getState());
    }
    
    private void refreshToolbar(final ProjectSession.State state) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                boolean running = isRunning(state);
                lrPauseButton.setEnabled(running);
                lrRefreshButton.setEnabled(!popupPause && running && lrPauseButton.isSelected());
                
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
        } else if (isRunning(newState)) {
            startResults();
        } else if (newState == ProjectSession.State.STARTED) {
            resetResults();
        }
        refreshToolbar(newState);
    }
    
    private boolean isInstrumentation() {
        return mode == Mode.INSTR_CLASS ||
               mode == Mode.INSTR_METHOD ||
               mode == Mode.INSTR_SELECTED;
    }
    
    private boolean isRunning(ProjectSession.State state) {
        if (state != ProjectSession.State.RUNNING) return false;
        ProjectSession session = getSession();
        if (session == null) return false;
        return ProfilingSettings.isCPUSettings(session.getProfilingSettings());
    }
   
    private volatile boolean running;
    private volatile boolean paused;
    private volatile boolean forceRefresh;
    
    private Runnable refresher;
    
    private void startResults() {
        if (running) return;
        running = true;
        
        resetResults();
        
        refresher = new Runnable() {
            public void run() {
                if (running) {
                    try {
                        if (!paused || forceRefresh)
                            if (cpuView != null) cpuView.refreshData();
                        
                        if (!forceRefresh) refreshResults(1500);
                        else forceRefresh = false;
                    } catch (ClientUtils.TargetAppOrVMTerminated ex) {
                        stopResults();
                    }
                }
            }
        };
        
        forceRefresh = false;
        refreshResults(1000);
    }
    
    private void refreshResults() {
        forceRefresh = true;
        refreshResults(0);
    }
    
    private void refreshResults(int delay) {
        // TODO: needs synchronization!
        if (running && refresher != null)
            ProfilerUtils.runInProfilerRequestProcessor(refresher, delay);
    }
    
    private void resetResults() {
        if (cpuView != null) cpuView.resetData();
    }
    
    private void stopResults() {
        if (refresher != null) {
            running = false;
            refresher = null;
        }
    }
    
    public void attachedToSession(ProjectSession session) {
        super.attachedToSession(session);
        resetResults();
    }
    
    public void detachedFromSession(ProjectSession session) {
        super.detachedFromSession(session);
        resetResults();
    }
    
}
