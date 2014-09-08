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
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.TargetAppRunner;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.lib.profiler.common.filters.SimpleFilter;
import org.netbeans.lib.profiler.ui.components.ProfilerToolbar;
import org.netbeans.lib.profiler.ui.memory.MemoryView;
import org.netbeans.lib.profiler.utils.Wildcards;
import org.netbeans.modules.profiler.NetBeansProfiler;
import org.netbeans.modules.profiler.ResultsListener;
import org.netbeans.modules.profiler.ResultsManager;
import org.netbeans.modules.profiler.actions.HeapDumpAction;
import org.netbeans.modules.profiler.actions.ResetResultsAction;
import org.netbeans.modules.profiler.actions.RunGCAction;
import org.netbeans.modules.profiler.actions.TakeSnapshotAction;
import org.netbeans.modules.profiler.actions.TakeThreadDumpAction;
import org.netbeans.modules.profiler.api.GoToSource;
import org.netbeans.modules.profiler.api.icons.GeneralIcons;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import org.netbeans.modules.profiler.api.java.SourceClassInfo;
import org.netbeans.modules.profiler.api.project.ProjectContentsSupport;
import org.netbeans.modules.profiler.utilities.ProfilerUtils;
import org.netbeans.modules.profiler.v2.ProfilerFeature;
import org.netbeans.modules.profiler.v2.ProfilerSession;
import org.netbeans.modules.profiler.v2.impl.ClassMethodList;
import org.netbeans.modules.profiler.v2.impl.ClassMethodSelector;
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
    
    private static enum Mode { SAMPLED_ALL, SAMPLED_PROJECT, INSTR_CLASSES }
    
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
    private ModeSelector modeSelector;
    private SettingsApplier settingsApplier;
    
    private JPanel settingsContent;
    private SampledAllSettings sampledAllSettings;
    private SampledProjectSettings sampledProjectSettings;
    private InstrSelectedSettings instrSelectedSettings;
    
    private MemoryView memoryView;
    
    private boolean popupPause;
    
    private final Set<ClientUtils.SourceCodeSelection> selection;
    
    
    MemoryFeature() {
        super(Icons.getIcon(ProfilerIcons.MEMORY), Bundle.MemoryFeature_name(),
              Bundle.MemoryFeature_description(), 13);
        
        loadSettings();
//        fireChange(); // updates last mode & settings
        confirmSettings();
        
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
        settingsCache.put(Mode.INSTR_CLASSES, new Properties());
    }
    
    
    public boolean supportsConfiguration(Lookup configuration) {
        return configuration.lookup(SourceClassInfo.class) != null;
    }
    
    public void configure(Lookup configuration) {
        // Handle Profile Class action
        SourceClassInfo classInfo = configuration.lookup(SourceClassInfo.class);
        if (classInfo != null) selectClassForProfiling(classInfo);
    }
    
    
    private void selectClassForProfiling(SourceClassInfo classInfo) {
        selectForProfiling(new ClientUtils.SourceCodeSelection(classInfo.getQualifiedName(),
                           Wildcards.ALLWILDCARD, null));
    }
    
    private void selectForProfiling(ClientUtils.SourceCodeSelection sel) {
        getSettingsUI();
        getResultsUI();
        selection.add(sel);
        setMode(Mode.INSTR_CLASSES);
        updateModeUI();
        memoryView.showSelectionColumn();
        getSettingsUI().setVisible(true);
//        settingsChanged();
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
            
            modeSelector = new ModeSelector();
            settingsUI.add(modeSelector);
            
            settingsContent = new JPanel();
            settingsContent.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
            settingsContent.setLayout(new BoxLayout(settingsContent, BoxLayout.LINE_AXIS));
            settingsContent.setOpaque(false);
            settingsUI.add(settingsContent);
            
            sampledAllSettings = new SampledAllSettings();
            sampledProjectSettings = new SampledProjectSettings();
            instrSelectedSettings = new InstrSelectedSettings();
            
            settingsUI.add(Box.createHorizontalGlue());
            
            settingsApplier = new SettingsApplier();
            settingsUI.add(settingsApplier);
            
            ProfilerSession session = getSession();
            int state = session != null ? session.getState() :
                        NetBeansProfiler.PROFILING_INACTIVE;
            
            updateModeUI();
            updateApply(state);
        }
        return settingsUI;
    }
    
    
    private class ModeSelector extends JPanel {
        
        PopupButton modeButton;
        
        ModeSelector() {
            setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
            setOpaque(false);
            
            add(new JLabel("Profile:"));
            
            add(Box.createHorizontalStrut(5));
            
            modeButton = new PopupButton("All classes") {
                protected void populatePopup(JPopupMenu popup) {
                    popup.add(new TitledMenuSeparator("General (sampled)"));
                    popup.add(new JRadioButtonMenuItem(getModeName(Mode.SAMPLED_ALL), currentMode == Mode.SAMPLED_ALL) {
                        protected void fireActionPerformed(ActionEvent e) { setMode(Mode.SAMPLED_ALL); }
                    });
                    if (getSession().getProject() != null) popup.add(new JRadioButtonMenuItem(getModeName(Mode.SAMPLED_PROJECT), currentMode == Mode.SAMPLED_PROJECT) {
                        protected void fireActionPerformed(ActionEvent e) { setMode(Mode.SAMPLED_PROJECT); }
                    });
                    
                    popup.add(new TitledMenuSeparator("Focused (instrumented)"));
                    popup.add(new JRadioButtonMenuItem(getModeName(Mode.INSTR_CLASSES), currentMode == Mode.INSTR_CLASSES) {
                        protected void fireActionPerformed(ActionEvent e) { setMode(Mode.INSTR_CLASSES); }
                    });
                }
            };
            add(modeButton);
        }
        
    }
    
    private class SettingsApplier extends JPanel {
        
        JButton applyButton;
        
        SettingsApplier() {
            setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
            setOpaque(false);
            
            applyButton = new SmallButton("Apply") {
                protected void fireActionPerformed(ActionEvent e) {
                    stopResults();
                    resetResults();
                    fireChange();
                }
            };
            add(applyButton);
        }
        
    }   
    
    private class SampledAllSettings extends JPanel {
        
        SampledAllSettings() {
            setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
            setOpaque(false);
        }
        
    }
    
    private class SampledProjectSettings extends JPanel {
        
//        LazyComboBox<Lookup.Provider> projectSelect;
        
        SampledProjectSettings() {
            setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
            setOpaque(false);
        }
        
//        public Dimension getMaximumSize() {
//            return getPreferredSize();
//        }
        
    }
    
    private class InstrSelectedSettings extends JPanel {
        
        final JCheckBox lifecycleCheckbox;
        final JCheckBox allocationsCheckbox;
        
        private final JPanel selectionContent;
        private final JPanel noSelectionContent;
        
        private final JButton addSelectionButton;
        private final JButton editSelectionLink;
        
        InstrSelectedSettings() {
            setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
            setOpaque(false);
            
            selectionContent = new JPanel();
            selectionContent.setLayout(new BoxLayout(selectionContent, BoxLayout.LINE_AXIS));
            selectionContent.setOpaque(false);
            
            editSelectionLink = new JButton() {
                public void setText(String text) {
                    super.setText("<html><a href='#'>" + text + ", edit</a></html>");
                }
                protected void fireActionPerformed(ActionEvent e) {
                    ClassMethodList.showClasses(getSession().getProject(), selection, editSelectionLink);
                }
                public Dimension getMinimumSize() {
                    return getPreferredSize();
                }
                public Dimension getMaximumSize() {
                    return getPreferredSize();
                }
            };
            editSelectionLink.setContentAreaFilled(false);
            editSelectionLink.setBorderPainted(true);
            editSelectionLink.setMargin(new Insets(0, 0, 0, 0));
            editSelectionLink.setBorder(BorderFactory.createEmptyBorder());
            editSelectionLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            selectionContent.add(editSelectionLink);
            
            selectionContent.add(Box.createHorizontalStrut(8));
            
            Component separator = Box.createHorizontalStrut(1);
            separator.setBackground(Color.GRAY);
            if (separator instanceof JComponent) ((JComponent)separator).setOpaque(true);
            Dimension d = separator.getMaximumSize();
            d.height = 20;
            separator.setMaximumSize(d);
            selectionContent.add(separator);
            
            selectionContent.add(Box.createHorizontalStrut(8));
            
            lifecycleCheckbox = new JCheckBox("Record full lifecycle") {
                protected void fireStateChanged() { settingsChanged(); super.fireStateChanged(); }
            };
            lifecycleCheckbox.setOpaque(false);
            selectionContent.add(lifecycleCheckbox);
            
            allocationsCheckbox = new JCheckBox("Record allocations") {
                protected void fireStateChanged() { settingsChanged(); super.fireStateChanged(); }
            };
            allocationsCheckbox.setOpaque(false);
            selectionContent.add(allocationsCheckbox);
            
            noSelectionContent = new JPanel();
            noSelectionContent.setLayout(new BoxLayout(noSelectionContent, BoxLayout.LINE_AXIS));
            noSelectionContent.setOpaque(false);
            
            GrayLabel noSelectionHint = new GrayLabel("No classes selected, use Profile Class action in editor or results or click the Add button:");
            noSelectionHint.setEnabled(false);
            noSelectionContent.add(noSelectionHint);
            
            noSelectionContent.add(Box.createHorizontalStrut(5));
            
            addSelectionButton = new SmallButton("+") {
                protected void fireActionPerformed(ActionEvent e) {
                    SourceClassInfo classInfo = ClassMethodSelector.selectClass(getSession().getProject());
                    if (classInfo != null) selectClassForProfiling(classInfo);
                }
                public Dimension getMinimumSize() {
                    return getPreferredSize();
                }
                public Dimension getMaximumSize() {
                    return getPreferredSize();
                }
            };
            noSelectionContent.add(addSelectionButton);
        }
        
        void updateSelection(int count) {
            removeAll();
            if (count == 0) {
                add(noSelectionContent);
            } else {
                editSelectionLink.setText(count == 1 ? "Selected 1 class" : "Selected " + count + " classes");
                add(selectionContent);
            }
        }
        
    }
    
//    // --- ProjectNameRenderer -------------------------------------------------
//    
//    private static final class ProjectNameRenderer extends DefaultListCellRenderer {
//
//        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
//                                                      boolean cellHasFocus) {
//            JLabel renderer = (JLabel)super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
//            
//            Lookup.Provider p = (Lookup.Provider) value;
//            renderer.setText(ProjectUtilities.getDisplayName(p));
//            renderer.setIcon(ProjectUtilities.getIcon(p));
//
//            return renderer;
//        }
//        
//    }
    
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
            case INSTR_CLASSES: return "Selected classes";
        }
        return null;
    }
    
    private void updateModeUI() {
        modeSelector.modeButton.setText(getModeName(currentMode));
        
        settingsContent.removeAll();
        
        if (currentMode == Mode.SAMPLED_ALL) {
            settingsContent.add(sampledAllSettings);
        } else if (currentMode == Mode.SAMPLED_PROJECT) {
            settingsContent.add(sampledProjectSettings);
        } else if (currentMode == Mode.INSTR_CLASSES) {
            instrSelectedSettings.updateSelection(selection.size());
            settingsContent.add(instrSelectedSettings);
        }
        
        settingsContent.doLayout();
        settingsContent.repaint();
        
        restoreSettings(settingsCache.get(currentMode));
    }
    
    private void updateApply(int state) {
        if (settingsApplier == null) return;
        
        settingsApplier.setVisible(state != NetBeansProfiler.PROFILING_INACTIVE);
        
        if (settingsApplier.isVisible())
            settingsApplier.applyButton.setEnabled(settingsValid() && pendingChanges());
    }
    
    private void confirmSettings() {
        appliedMode = currentMode;
        appliedSettings = new Properties();
        appliedSettings.putAll(settingsCache.get(appliedMode));
        
//        if (lrPauseButton != null) lrPauseButton.setSelected(false);
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
                
            case INSTR_CLASSES:
                int count = selection.size();
                settings.put("selectionSize", Integer.toString(count));
                Iterator<ClientUtils.SourceCodeSelection> it = selection.iterator();
                for (int i = 0; i < count; i++)
                    settings.put("selection" + Integer.toString(i), ClientUtils.selectionToString(it.next()));
                
                boolean lifecycle = instrSelectedSettings.lifecycleCheckbox.isSelected();
                settings.put("lifecycle", Boolean.toString(lifecycle));
                
                boolean allocations = instrSelectedSettings.allocationsCheckbox.isSelected();
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
                
            case INSTR_CLASSES:
//                String _count = settings.getProperty("selectionSize");
//                int count = _count == null ? 0 : Integer.parseInt(_count);
//                selectedClasses = new ClientUtils.SourceCodeSelection[count];
//                for (int i = 0; i < count; i++) {
//                    String sel = settings.getProperty("selection" + Integer.toString(i));
//                    selectedClasses[i] = ClientUtils.stringToSelection(sel);
//                }
                String _lifecycle = settings.getProperty("lifecycle", "false");
                instrSelectedSettings.lifecycleCheckbox.setSelected(Boolean.parseBoolean(_lifecycle));
                
                String _allocations = settings.getProperty("allocations", "false");
                instrSelectedSettings.allocationsCheckbox.setSelected(Boolean.parseBoolean(_allocations));
                
                break;
        }
    }
    
    public boolean settingsValid() {
        switch (currentMode) {
            case SAMPLED_ALL:
                return true;
                
            case SAMPLED_PROJECT:
                return true;
                
            case INSTR_CLASSES:
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
                settings.setSelectedInstrumentationFilter(null);
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
                
            case INSTR_CLASSES:
                int type = instrSelectedSettings.lifecycleCheckbox.isSelected() ? ProfilingSettings.PROFILE_MEMORY_LIVENESS :
                                                            ProfilingSettings.PROFILE_MEMORY_ALLOCATIONS;
                settings.setProfilingType(type);
                
                int stackLimit = instrSelectedSettings.allocationsCheckbox.isSelected() ? -1 : 0;
                settings.setAllocStackTraceLimit(stackLimit);
                
                StringBuilder b = new StringBuilder();
                ClientUtils.SourceCodeSelection[] classes = selection.toArray(new ClientUtils.SourceCodeSelection[selection.size()]);
                for (int i = 0; i < classes.length; i++) {
                    b.append(classes[i].getClassName());
                    if (i < classes.length - 1) b.append(", "); // NOI18N
                }
                
                SimpleFilter ff = new SimpleFilter("", SimpleFilter.SIMPLE_FILTER_INCLUSIVE_EXACT, b.toString()); // NOI18N
                settings.setSelectedInstrumentationFilter(ff);
                break;
        }
        
        confirmSettings();
        
//        if (settings == null) settings = ProfilingSettingsPresets.createMemoryPreset();
//        return settings;
    }
    
    
    private void initResultsUI() {
        TargetAppRunner runner = getSession().getProfiler().getTargetAppRunner();
        memoryView = new MemoryView(runner.getProfilerClient(), selection, GoToSource.isAvailable()) {
            public void showSource(ClientUtils.SourceCodeSelection value) {
                Lookup.Provider project = getSession().getProject();
                String className = value.getClassName();
                GoToSource.openSource(project, className, "", ""); // NOI18N
            }
            public void selectForProfiling(ClientUtils.SourceCodeSelection value) {
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
    
//    private boolean isInstrumentation() {
//        return currentMode == Mode.INSTR_CLASSES;
//    }
    
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
