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
import org.netbeans.lib.profiler.TargetAppRunner;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.lib.profiler.common.ProfilingSettingsPresets;
import org.netbeans.lib.profiler.common.filters.SimpleFilter;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.ProfilerToolbar;
import org.netbeans.lib.profiler.ui.memory.MemoryView;
import org.netbeans.modules.profiler.api.icons.GeneralIcons;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import org.netbeans.modules.profiler.api.project.ProjectContentsSupport;
import org.netbeans.modules.profiler.utilities.ProfilerUtils;
import org.netbeans.modules.profiler.v2.session.ProjectSession;
import org.netbeans.modules.profiler.v2.ui.components.PopupButton;
import org.netbeans.modules.profiler.v2.ui.components.SmallButton;
import org.netbeans.modules.profiler.v2.ui.components.TitledMenuSeparator;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "MemoryFeature_name=Memory",
    "MemoryFeature_lrLabel=Live results:",
    "MemoryFeature_pdLabel=Profiling data:", 
    "MemoryFeature_snapshot=Snapshot", 
    "MemoryFeature_apLabel=Application:", 
    "MemoryFeature_heapDump=Heap Dump",
    "MemoryFeature_gc=GC"
})
final class MemoryFeature extends ProfilerFeature.Basic {
    
    private static enum Mode { SAMPLED_ALL, SAMPLED_PROJECT, INSTR_CLASS, INSTR_SELECTED }
    
    private JLabel lrLabel;
    private JButton lrPauseButton;
    private JToggleButton lrRefreshButton;
    
    private JLabel pdLabel;
    private JButton pdSnapshotButton;
    private JButton pdResetResultsButton;
    
    private JLabel apLabel;
    private JButton apHeapDumpButton;
    private JButton apGCButton;
    
    private ProfilerToolbar toolbar;
    private JPanel settingsUI;
    
    private MemoryView tableView;
    
    private Mode mode = Mode.SAMPLED_ALL;
    private PopupButton modeButton;
    
    private Lookup.Provider project;
    
    
    MemoryFeature() {
        super(Bundle.MemoryFeature_name(), Icons.getIcon(ProfilerIcons.MEMORY));
    }

    
    public JPanel getResultsUI() {
        if (tableView == null) initResultsUI();
        return tableView;
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
                    
                    popup.add(new TitledMenuSeparator("Advanced (instrumented)"));
                    popup.add(new JRadioButtonMenuItem(getModeName(Mode.INSTR_CLASS), mode == Mode.INSTR_CLASS) {
                        protected void fireActionPerformed(ActionEvent e) { setMode(Mode.INSTR_CLASS); }
                    });
                    popup.add(new JRadioButtonMenuItem(getModeName(Mode.INSTR_SELECTED), mode == Mode.INSTR_SELECTED) {
                        { setEnabled(tableView.hasSelection()); }
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
                    tableView.resetData();
                    fireChange();
                    settingsUI.setVisible(false);
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
            case INSTR_SELECTED: return "Selected classes";
        }
        return null;
    }
    
    public ProfilerToolbar getToolbar() {
        if (toolbar == null) {
            lrLabel = new JLabel(Bundle.MemoryFeature_lrLabel());
            lrLabel.setForeground(UIUtils.getDisabledLineColor());
            
            lrPauseButton = new JButton(Icons.getIcon(GeneralIcons.PAUSE));
            lrPauseButton.setEnabled(false);
            
            lrRefreshButton = new JToggleButton(Icons.getIcon(GeneralIcons.UPDATE_NOW));
            lrRefreshButton.setEnabled(false);
            
            pdLabel = new JLabel(Bundle.MemoryFeature_pdLabel());
            pdLabel.setForeground(UIUtils.getDisabledLineColor());
            
            pdSnapshotButton = new JButton(Bundle.MemoryFeature_snapshot(), Icons.getIcon(ProfilerIcons.SNAPSHOT_TAKE));
            pdSnapshotButton.setEnabled(false);
            
            pdResetResultsButton = new JButton(Icons.getIcon(ProfilerIcons.RESET_RESULTS));
            pdResetResultsButton.setEnabled(false);
            
            apLabel = new JLabel(Bundle.MemoryFeature_apLabel());
            apLabel.setForeground(UIUtils.getDisabledLineColor());
            
            apHeapDumpButton = new JButton(Bundle.MemoryFeature_heapDump(), Icons.getIcon(ProfilerIcons.HEAP_DUMP));
            apHeapDumpButton.setEnabled(false);
            
            apGCButton = new JButton(Bundle.MemoryFeature_gc(), Icons.getIcon(ProfilerIcons.RUN_GC));
            apGCButton.setEnabled(false);
            
            toolbar = ProfilerToolbar.create(true);
            
            toolbar.addSpace(2);
            toolbar.addSeparator();
            toolbar.addSpace(5);
            
            toolbar.add(lrLabel);
            toolbar.addSpace(2);
            toolbar.add(lrPauseButton);
            toolbar.add(lrRefreshButton);
            
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
            toolbar.add(apHeapDumpButton);
            toolbar.add(apGCButton);
        }
        
        return toolbar;
    }
    
    public ProfilingSettings getSettings() {
        if (project == null) return null;
        
        ProfilingSettings settings = null;
        
        switch (mode)  {
            case SAMPLED_ALL:
                settings = ProfilingSettingsPresets.createMemoryPreset();
                break;
                
            case SAMPLED_PROJECT:
                settings = ProfilingSettingsPresets.createMemoryPreset();
                
                ProjectContentsSupport pcs = ProjectContentsSupport.get(project);
                String filter = pcs.getInstrumentationFilter(false);
                SimpleFilter f = new SimpleFilter("", SimpleFilter.SIMPLE_FILTER_INCLUSIVE, filter); // NOI18N
                settings.setSelectedInstrumentationFilter(f);
                break;
                
            case INSTR_SELECTED:
//                settings = ProfilingSettingsPresets.createMemoryPreset(ProfilingSettings.PROFILE_MEMORY_ALLOCATIONS);
                settings = ProfilingSettingsPresets.createMemoryPreset(ProfilingSettings.PROFILE_MEMORY_LIVENESS);
                settings.setThreadCPUTimerOn(true);
                
                StringBuilder b = new StringBuilder();
                String[] selections = tableView.getSelections();
                for (int i = 0; i < selections.length; i++) {
                    b.append(selections[i]);
                    if (i < selections.length - 1) b.append(", "); // NOI18N
                }
                
                SimpleFilter ff = new SimpleFilter("", SimpleFilter.SIMPLE_FILTER_INCLUSIVE, b.toString()); // NOI18N
                settings.setSelectedInstrumentationFilter(ff);
                break;
        }
        
        if (settings == null) settings = ProfilingSettingsPresets.createMemoryPreset();
        return settings;
    }
    
    
    private void initResultsUI() {
        TargetAppRunner runner = Profiler.getDefault().getTargetAppRunner();
        tableView = new MemoryView(runner.getProfilerClient());
    }
    
    public void stateChanged(ProjectSession.State oldState, ProjectSession.State newState) {
        if (newState == null || newState == ProjectSession.State.INACTIVE) {
            stopResults();
        } else if (newState == ProjectSession.State.RUNNING) {
            startResults();
        }
    }
    
    private RequestProcessor processor;
    
    private void startResults() {
        if (processor != null) return;
        
        if (tableView != null) tableView.resetData();
        
        processor = new RequestProcessor("Memory Data Refresher"); // NOI18N
        
        Runnable refresher = new Runnable() {
            public void run() {
                if (tableView != null) {
                    ProfilerUtils.runInProfilerRequestProcessor(new Runnable() {
                        public void run() {
                            try {
                                tableView.refreshData();
                            } catch (ClientUtils.TargetAppOrVMTerminated ex) {
                                stopResults();
                            }
                        }
                    });
                }
                
                if (processor != null && !processor.isShutdown()) processor.post(this, 1500);
            }
        };
        
        processor.post(refresher, 2000);
    }
    
    private void stopResults() {
        if (processor != null) processor.shutdownNow();
        processor = null;
    }
    
    public void attachedToSession(ProjectSession session) {
        super.attachedToSession(session);
        project = session.getProject();
//        if (tableView != null) tableView.resetData();
    }
    
    public void detachedFromSession(ProjectSession session) {
        super.detachedFromSession(session);
        project = null;
//        if (tableView != null) tableView.resetData();
    }
    
}
