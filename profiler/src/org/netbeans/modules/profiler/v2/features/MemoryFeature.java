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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.lib.profiler.common.filters.SimpleFilter;
import org.netbeans.lib.profiler.ui.components.ProfilerToolbar;
import org.netbeans.lib.profiler.ui.memory.MemoryView;
import org.netbeans.modules.profiler.NetBeansProfiler;
import org.netbeans.modules.profiler.ResultsListener;
import org.netbeans.modules.profiler.ResultsManager;
import org.netbeans.modules.profiler.actions.HeapDumpAction;
import org.netbeans.modules.profiler.actions.ResetResultsAction;
import org.netbeans.modules.profiler.actions.RunGCAction;
import org.netbeans.modules.profiler.actions.TakeSnapshotAction;
import org.netbeans.modules.profiler.actions.TakeThreadDumpAction;
import org.netbeans.modules.profiler.api.GoToSource;
import org.netbeans.modules.profiler.api.ProfilerDialogs;
import org.netbeans.modules.profiler.api.icons.GeneralIcons;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import org.netbeans.modules.profiler.api.java.SourceClassInfo;
import org.netbeans.modules.profiler.api.project.ProjectContentsSupport;
import org.netbeans.modules.profiler.utilities.ProfilerUtils;
import org.netbeans.modules.profiler.v2.ProfilerFeature;
import org.netbeans.modules.profiler.v2.ProfilerSession;
import org.netbeans.modules.profiler.v2.ui.GrayLabel;
import org.netbeans.modules.profiler.v2.ui.PopupButton;
import org.netbeans.modules.profiler.v2.ui.SmallButton;
import org.netbeans.modules.profiler.v2.ui.TitledMenuSeparator;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "MemoryFeature_name=Objects",
    "MemoryFeature_description=Profile size and count of allocated objects, including allocation paths",
    "MemoryFeature_lrLabel=Live results:",
    "MemoryFeature_pdLabel=Profiling data:", 
    "MemoryFeature_snapshot=Snapshot", 
    "MemoryFeature_apLabel=Application:", 
    "MemoryFeature_heapDump=Heap Dump",
    "MemoryFeature_gc=GC"
})
final class MemoryFeature extends ProfilerFeature.Basic {
    
    private static enum Mode { SAMPLED_ALL, SAMPLED_PROJECT, INSTR_CLASS, INSTR_SELECTED }
    
    private final Map<Mode, Properties> settingsCache = new HashMap();
    private Mode appliedMode;
    private Properties appliedSettings;
    private Mode currentMode = Mode.SAMPLED_ALL;
    
    private JLabel lrLabel;
    private JToggleButton lrPauseButton;
    private JButton lrRefreshButton;
    
    private JLabel pdLabel;
    private JButton pdSnapshotButton;
    private JButton pdResetResultsButton;
    
    private JLabel apLabel;
    private JButton apThreadDumpButton;
    private JButton apHeapDumpButton;
    private JButton apGCButton;
    
    private ProfilerToolbar toolbar;
    private JPanel settingsUI;
    
    private JPanel applyPanel;
    private JButton applyButton;
    
    private PopupButton modeButton;
    
    private Component instrSettingsSpace;
    private JButton selectedLabel;
    private Component selectedSpace1;
    private Component selectedSeparator;
    private Component selectedSpace2;
    private JCheckBox lifecycleCheckbox;
    private JCheckBox allocationsCheckbox;
    
    private MemoryView memoryView;
    
    private boolean popupPause;
    
    private String[] selectedClasses;
    
    private final Set<String> selection;
    
    
    MemoryFeature() {
        super(Icons.getIcon(ProfilerIcons.MEMORY), Bundle.MemoryFeature_name(),
              Bundle.MemoryFeature_description(), 13);
        
        loadSettings();
        fireChange(); // updates last mode & settings
        
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
    
    
    private void loadSettings() {
        settingsCache.put(Mode.SAMPLED_ALL, new Properties());
        settingsCache.put(Mode.SAMPLED_PROJECT, new Properties());
        settingsCache.put(Mode.INSTR_CLASS, new Properties());
        settingsCache.put(Mode.INSTR_SELECTED, new Properties());
    }
    
    
    public boolean supportsConfiguration(Lookup configuration) {
        return configuration.lookup(SourceClassInfo.class) != null;
    }
    
    public void configure(Lookup configuration) {
        SourceClassInfo classInfo = configuration.lookup(SourceClassInfo.class);
        if (classInfo != null) profileSingle(classInfo.getQualifiedName());
    }
    
    
    private void profileSingle(String selection) {
        selectedClasses = new String[] { selection };
        setMode(Mode.INSTR_CLASS);
        updateModeUI();
        getSettingsUI().setVisible(true);
        settingsChanged();
    }
    
    private void selectForProfiling(String[] selection) {
        this.selection.addAll(Arrays.asList(selection));
        setMode(Mode.INSTR_SELECTED);
        updateModeUI();
        memoryView.showSelectionColumn();
        getSettingsUI().setVisible(true);
        settingsChanged();
    }
    
    
    private void selectionChanged() {
        memoryView.refreshSelection();
        updateModeUI();
        settingsChanged();
    }

    
    public JPanel getResultsUI() {
        if (memoryView == null) initResultsUI();
        return memoryView;
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
                    popup.add(new JRadioButtonMenuItem(getModeName(Mode.SAMPLED_ALL), currentMode == Mode.SAMPLED_ALL) {
                        protected void fireActionPerformed(ActionEvent e) { setMode(Mode.SAMPLED_ALL); }
                    });
                    popup.add(new JRadioButtonMenuItem(getModeName(Mode.SAMPLED_PROJECT), currentMode == Mode.SAMPLED_PROJECT) {
                        protected void fireActionPerformed(ActionEvent e) { setMode(Mode.SAMPLED_PROJECT); }
                    });
                    
                    popup.add(new TitledMenuSeparator("Focused (instrumented)"));
                    popup.add(new JRadioButtonMenuItem(getModeName(Mode.INSTR_CLASS), currentMode == Mode.INSTR_CLASS) {
                        protected void fireActionPerformed(ActionEvent e) { setMode(Mode.INSTR_CLASS); }
                    });
                    popup.add(new JRadioButtonMenuItem(getModeName(Mode.INSTR_SELECTED), currentMode == Mode.INSTR_SELECTED) {
//                        { setEnabled(memoryView.hasSelection()); }
                        protected void fireActionPerformed(ActionEvent e) { setMode(Mode.INSTR_SELECTED); }
                    });
                }
            };
            settingsUI.add(modeButton);

            instrSettingsSpace = settingsUI.add(Box.createHorizontalStrut(8));
            
            selectedLabel = new JButton() {
                public void setText(String text) {
                    super.setText("<html>" + text + ", <a href='#'>edit</a></html>");
                }
                protected void fireActionPerformed(ActionEvent e) {
                    ProfilerDialogs.displayInfo("\n[TODO]\n\nWill open a dialog for editing classes to profile.");
                }
                public Dimension getMinimumSize() {
                    return getPreferredSize();
                }
                public Dimension getMaximumSize() {
                    return getPreferredSize();
                }
            };
            selectedLabel.setContentAreaFilled(false);
            selectedLabel.setBorderPainted(true);
            selectedLabel.setMargin(new Insets(0, 0, 0, 0));
            selectedLabel.setBorder(BorderFactory.createEmptyBorder());
            selectedLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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
            
            lifecycleCheckbox = new JCheckBox("Record full lifecycle") {
                protected void fireStateChanged() { settingsChanged(); super.fireStateChanged(); }
            };
            lifecycleCheckbox.setOpaque(false);
            settingsUI.add(lifecycleCheckbox);
            
            allocationsCheckbox = new JCheckBox("Record allocations") {
                protected void fireStateChanged() { settingsChanged(); super.fireStateChanged(); }
            };
            allocationsCheckbox.setOpaque(false);
            settingsUI.add(allocationsCheckbox);


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

            applyPanel = new JPanel();
            applyPanel.setOpaque(false);
            applyPanel.setLayout(new BoxLayout(applyPanel, BoxLayout.LINE_AXIS));
            
            applyPanel.add(Box.createHorizontalStrut(8));

            Component sep1 = Box.createHorizontalStrut(1);
            sep1.setBackground(Color.GRAY);
            if (sep1 instanceof JComponent) ((JComponent)sep1).setOpaque(true);
            Dimension dd = sep1.getMaximumSize();
            dd.height = 20;
            sep1.setMaximumSize(dd);
            applyPanel.add(sep1);

            applyPanel.add(Box.createHorizontalStrut(8));
            
            applyButton = new SmallButton("Apply") {
                protected void fireActionPerformed(ActionEvent e) {
                    stopResults();
                    resetResults();
                    fireChange();
                }
            };
            applyPanel.add(applyButton);
            
            settingsUI.add(applyPanel);
            
            ProfilerSession session = getSession();
            int state = session != null ? session.getState() :
                        NetBeansProfiler.PROFILING_INACTIVE;
            
            updateModeUI();
            updateApply(state);
        }
        return settingsUI;
    }
    
    private void setMode(Mode m) {
        if (currentMode == m) return;
        currentMode = m;
        updateModeUI();
        settingsChanged();
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
    
    private void updateModeUI() {
        modeButton.setText(getModeName(currentMode));
        
        boolean instrumentation = isInstrumentation();
        instrSettingsSpace.setVisible(instrumentation);
        selectedLabel.setVisible(instrumentation);
        selectedSpace1.setVisible(instrumentation);
        selectedSeparator.setVisible(instrumentation);
        selectedSpace2.setVisible(instrumentation);
        lifecycleCheckbox.setVisible(instrumentation);
        allocationsCheckbox.setVisible(instrumentation);
        
        if (currentMode == Mode.INSTR_CLASS) {
            int count = selectedClasses == null ? 0 : selectedClasses.length;
            if (count == 0) {
                selectedLabel.setText("No class");
            } else if (count == 1) {
                selectedLabel.setText(selectedClasses[0]);
            } else {
                selectedLabel.setText(count + " classes");
            }
        } else if (currentMode == Mode.INSTR_SELECTED) {
            int count = selection.size();
            if (count == 0) {
                selectedLabel.setText("No class");
            } else if (count == 1) {
                selectedLabel.setText(selection.iterator().next());
            } else {
                selectedLabel.setText(count + " classes");
            }
        }
        
        restoreSettings(settingsCache.get(currentMode));
    }
    
    private void updateApply(int state) {
        if (applyButton == null) return;
        
        applyPanel.setVisible(state != NetBeansProfiler.PROFILING_INACTIVE);
        
        if (applyPanel.isVisible())
            applyButton.setEnabled(settingsValid() && pendingChanges());
    }
    
    protected void fireChange() {
        appliedMode = currentMode;
        appliedSettings = new Properties();
        appliedSettings.putAll(settingsCache.get(appliedMode));
        super.fireChange();
    }
    
    private boolean pendingChanges() {
        if (appliedMode != currentMode) return true;
        return !appliedSettings.equals(settingsCache.get(currentMode));
    }
    
    private void updateCurrentSettings() {
        Properties settings = settingsCache.get(currentMode);
        switch (currentMode)  {
            case SAMPLED_ALL:
                break;
                
            case SAMPLED_PROJECT:
                break;
                
            case INSTR_CLASS:
                int count = selectedClasses == null ? 0 : selectedClasses.length;
                settings.put("selectionSize", Integer.toString(count));
                for (int i = 0; i < count; i++)
                    settings.put("selection" + Integer.toString(i), selectedClasses[i]);
                
                boolean lifecycle = lifecycleCheckbox.isSelected();
                settings.put("lifecycle", Boolean.toString(lifecycle));
                
                boolean allocations = allocationsCheckbox.isSelected();
                settings.put("allocations", Boolean.toString(allocations));
                
                break;
                
            case INSTR_SELECTED:
                count = selection == null ? 0 : selection.size();
                settings.put("selectionSize", Integer.toString(count));
                Iterator<String> it = count == 0 ? null : selection.iterator();
                for (int i = 0; i < count; i++)
                    settings.put("selection" + Integer.toString(i), it.next());
                
                lifecycle = lifecycleCheckbox.isSelected();
                settings.put("lifecycle", Boolean.toString(lifecycle));
                
                allocations = allocationsCheckbox.isSelected();
                settings.put("allocations", Boolean.toString(allocations));
                
                break;
        }
    }
    
    private void restoreSettings(Properties settings) {
//        setMode(m);
        switch (currentMode)  {
            case SAMPLED_ALL:
                break;
                
            case SAMPLED_PROJECT:
                break;
                
            case INSTR_CLASS:
//                String _count = settings.getProperty("selectionSize");
//                int count = _count == null ? 0 : Integer.parseInt(_count);
//                selectedClasses = new ClientUtils.SourceCodeSelection[count];
//                for (int i = 0; i < count; i++) {
//                    String sel = settings.getProperty("selection" + Integer.toString(i));
//                    selectedClasses[i] = ClientUtils.stringToSelection(sel);
//                }
                String _lifecycle = settings.getProperty("lifecycle", "false");
                lifecycleCheckbox.setSelected(Boolean.parseBoolean(_lifecycle));
                
                String _allocations = settings.getProperty("allocations", "false");
                allocationsCheckbox.setSelected(Boolean.parseBoolean(_allocations));
                
                break;
                
            case INSTR_SELECTED:
//                selection.clear();
//                _count = settings.getProperty("selectionSize");
//                count = _count == null ? 0 : Integer.parseInt(_count);
//                for (int i = 0; i < count; i++) {
//                    String sel = settings.getProperty("selection" + Integer.toString(i));
//                    selection.add(ClientUtils.stringToSelection(sel));
//                }
                
                _lifecycle = settings.getProperty("lifecycle", "false");
                lifecycleCheckbox.setSelected(Boolean.parseBoolean(_lifecycle));
                
                _allocations = settings.getProperty("allocations", "false");
                allocationsCheckbox.setSelected(Boolean.parseBoolean(_allocations));
                
                break;
        }
    }
    
    public boolean settingsValid() {
        switch (currentMode) {
            case SAMPLED_ALL:
                return true;
                
            case SAMPLED_PROJECT:
                return true;
                
            case INSTR_CLASS:
                return selectedClasses != null && selectedClasses.length > 0;
                
            case INSTR_SELECTED:
                return selection != null && !selection.isEmpty();
                
            default:
                return false;
        }
    }
    
    public ProfilerToolbar getToolbar() {
        if (toolbar == null) {
            lrLabel = new GrayLabel(Bundle.CPUFeature_lrLabel());
            
            lrPauseButton = new JToggleButton(Icons.getIcon(GeneralIcons.PAUSE)) {
                protected void fireItemStateChanged(ItemEvent event) {
                    boolean paused = isSelected();
                    memoryView.setPaused(paused);
                    refreshToolbar(getSessionState());
                }
            };
            lrPauseButton.setEnabled(false);
            
            lrRefreshButton = new JButton(Icons.getIcon(GeneralIcons.UPDATE_NOW)) {
                protected void fireActionPerformed(ActionEvent e) {
                    refreshResults();
                }
            };
            
            pdLabel = new GrayLabel(Bundle.MemoryFeature_pdLabel());
            
            pdSnapshotButton = new JButton(TakeSnapshotAction.getInstance());
            pdSnapshotButton.setHideActionText(true);
            pdSnapshotButton.setText(Bundle.CPUFeature_snapshot());
            
            pdResetResultsButton = new JButton(ResetResultsAction.getInstance());
            pdResetResultsButton.setHideActionText(true);
            
            apLabel = new GrayLabel(Bundle.MemoryFeature_apLabel());
            
            apThreadDumpButton = new JButton(TakeThreadDumpAction.getInstance());
            apThreadDumpButton.setHideActionText(true);
            apThreadDumpButton.setText(Bundle.CPUFeature_threadDump());
            
            apHeapDumpButton = new JButton(HeapDumpAction.getInstance());
            apHeapDumpButton.setHideActionText(true);
            apHeapDumpButton.setText(Bundle.MemoryFeature_heapDump());
            
            apGCButton = new JButton(RunGCAction.getInstance());
            apGCButton.setHideActionText(true);
            apGCButton.setText(Bundle.MemoryFeature_gc());
            
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
            toolbar.add(apThreadDumpButton);
            toolbar.add(apHeapDumpButton);
            toolbar.add(apGCButton);
            
            refreshToolbar(getSessionState());
        }
        
        return toolbar;
    }
    
    private void settingsChanged() {
        updateCurrentSettings();
        ProfilerSession session = getSession();
        if (session == null || !session.inProgress()) fireChange();
        updateApply(session != null ? session.getState() :
                    NetBeansProfiler.PROFILING_INACTIVE);
    }
    
    public boolean supportsSettings(ProfilingSettings settings) {
        return !ProfilingSettings.isCPUSettings(settings);
    }
    
    public void configureSettings(ProfilingSettings settings) {
        ProfilerSession session = getSession();
//        if (session == null) return ProfilingSettingsPresets.createMemoryPreset();
//        
//        ProfilingSettings settings = null;
        
        switch (currentMode)  {
            case SAMPLED_ALL:
                settings.setProfilingType(ProfilingSettings.PROFILE_MEMORY_SAMPLING);
                break;
                
            case SAMPLED_PROJECT:
                settings.setProfilingType(ProfilingSettings.PROFILE_MEMORY_SAMPLING);
                
                if (session != null) {
                    ProjectContentsSupport pcs = ProjectContentsSupport.get(session.getProject());
                    String filter = pcs.getInstrumentationFilter(false);
                    SimpleFilter f = new SimpleFilter("", SimpleFilter.SIMPLE_FILTER_INCLUSIVE, filter); // NOI18N
                    settings.setSelectedInstrumentationFilter(f);
                }
                break;
                
            case INSTR_CLASS:
            case INSTR_SELECTED:
                int type = lifecycleCheckbox.isSelected() ? ProfilingSettings.PROFILE_MEMORY_LIVENESS :
                                                            ProfilingSettings.PROFILE_MEMORY_ALLOCATIONS;
                settings.setProfilingType(type);
                
                int stackLimit = allocationsCheckbox.isSelected() ? -1 : 0;
                settings.setAllocStackTraceLimit(stackLimit);
                
                StringBuilder b = new StringBuilder();
                String[] selections = currentMode == Mode.INSTR_CLASS ? selectedClasses : selection.toArray(new String[selection.size()]);
                for (int i = 0; i < selections.length; i++) {
                    b.append(selections[i]);
                    if (i < selections.length - 1) b.append(", "); // NOI18N
                }
                
                SimpleFilter ff = new SimpleFilter("", SimpleFilter.SIMPLE_FILTER_INCLUSIVE_EXACT, b.toString()); // NOI18N
                settings.setSelectedInstrumentationFilter(ff);
                break;
        }
        
//        if (settings == null) settings = ProfilingSettingsPresets.createMemoryPreset();
//        return settings;
    }
    
    
    private void initResultsUI() {
        TargetAppRunner runner = getSession().getProfiler().getTargetAppRunner();
        memoryView = new MemoryView(runner.getProfilerClient(), selection, GoToSource.isAvailable()) {
            public void showSource(String value) {
                Lookup.Provider project = getSession().getProject();
                GoToSource.openSource(project, value, "", ""); // NOI18N
            }
            public void profileSingle(String value) {
                MemoryFeature.this.profileSingle(value);
            }
            public void selectForProfiling(String[] value) {
                MemoryFeature.this.selectForProfiling(value);
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
    }
    
    private void refreshToolbar(final int state) {
        if (toolbar != null) SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                boolean running = isRunning(state);
                lrPauseButton.setEnabled(running);
                lrRefreshButton.setEnabled(!popupPause && running && lrPauseButton.isSelected());
                
                boolean inactive = state == NetBeansProfiler.PROFILING_INACTIVE;
                lrLabel.setEnabled(!inactive);
                pdLabel.setEnabled(!inactive);
                apLabel.setEnabled(!inactive);
            }
        });
    }
    
    protected void profilingStateChanged(int oldState, int newState) {
        if (newState == NetBeansProfiler.PROFILING_INACTIVE) {
            stopResults();
        } else if (isRunning(newState)) {
            startResults();
        } else if (newState == NetBeansProfiler.PROFILING_STARTED) {
            resetResults();
        }
        updateApply(newState);
        refreshToolbar(newState);
    }
    
    private boolean isInstrumentation() {
        return currentMode == Mode.INSTR_CLASS ||
               currentMode == Mode.INSTR_SELECTED;
    }
    
    private boolean isRunning(int state) {
        if (state != NetBeansProfiler.PROFILING_RUNNING) return false;
        ProfilerSession session = getSession();
        if (session == null) return false;
        return ProfilingSettings.isMemorySettings(session.getProfilingSettings());
    }
    
    private volatile boolean running;
    
    private Runnable refresher;
    
    private void startResults() {
        if (running) return;
        running = true;
        
        resetResults();
        
        refresher = new Runnable() {
            public void run() {
                if (running) {
                    if (memoryView != null) refreshView();
                    refreshResults(1500);
                }
            }
        };
        
        refreshResults(1000);
    }

    private void refreshView() {
        try {
            if (ResultsManager.getDefault().resultsAvailable()) memoryView.refreshData();
        } catch (ClientUtils.TargetAppOrVMTerminated ex) {
            stopResults();
        }
    }
    
    private void refreshResults() {
        if (running) ProfilerUtils.runInProfilerRequestProcessor(new Runnable() {

            @Override
            public void run() {
                if (memoryView != null) {
                    memoryView.setForceRefresh(true);
                    refreshView();
                }
            }
        });
    }
    
    private void refreshResults(int delay) {
        // TODO: needs synchronization!
        if (running && refresher != null)
            ProfilerUtils.runInProfilerRequestProcessor(refresher, delay);
    }
    
    private void resetResults() {
        if (memoryView != null) memoryView.resetData();
    }
    
    private void stopResults() {
        if (refresher != null) {
            running = false;
            refresher = null;
        }
    }
    
    private MemoryResetter resetter;
    
    public void attachedToSession(ProfilerSession session) {
        super.attachedToSession(session);
        resetResults();
        resetter = Lookup.getDefault().lookup(MemoryResetter.class);
        resetter.controller = this;
    }
    
    public void detachedFromSession(ProfilerSession session) {
        super.detachedFromSession(session);
        resetResults();
        resetter.controller = null;
        resetter = null;
    }
    
    
    @ServiceProvider(service=ResultsListener.class)
    public static final class MemoryResetter implements ResultsListener {
        
        private MemoryFeature controller;

        public void resultsAvailable() {
//            if (controller != null) controller.refreshView();
        }

        public void resultsReset() {
            if (controller != null) controller.resetResults();
        }
        
    }
    
}
