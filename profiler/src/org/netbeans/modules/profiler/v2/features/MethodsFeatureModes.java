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
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.lib.profiler.filters.GenericFilter;
import org.netbeans.lib.profiler.filters.JavaTypeFilter;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.JExtendedSpinner;
import org.netbeans.lib.profiler.ui.swing.GrayLabel;
import org.netbeans.lib.profiler.ui.swing.SmallButton;
import org.netbeans.lib.profiler.ui.swing.TextArea;
import org.netbeans.modules.profiler.api.ProjectUtilities;
import org.netbeans.modules.profiler.api.icons.GeneralIcons;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.LanguageIcons;
import org.netbeans.modules.profiler.api.java.SourceClassInfo;
import org.netbeans.modules.profiler.api.java.SourceMethodInfo;
import org.netbeans.modules.profiler.api.project.ProjectContentsSupport;
import org.netbeans.modules.profiler.v2.ProfilerSession;
import org.netbeans.modules.profiler.v2.impl.ClassMethodList;
import org.netbeans.modules.profiler.v2.impl.ClassMethodSelector;
import org.netbeans.modules.profiler.v2.impl.FilterSelector;
import org.netbeans.modules.profiler.v2.impl.ProjectsSelector;
import org.netbeans.modules.profiler.v2.ui.SettingsPanel;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "MethodsFeatureModes_allClasses=All classes",
    "MethodsFeatureModes_projectClasses=Project classes",
    "MethodsFeatureModes_selectedClasses=Selected classes",
    "MethodsFeatureModes_selectedMethods=Selected methods",
    "MethodsFeatureModes_editLink=<html><a href='#'>{0}, edit</a></html>",
    "MethodsFeatureModes_selectedProject=Selected {0}",
    "MethodsFeatureModes_selectedProjects=Selected {0} projects",
    "MethodsFeatureModes_outgoingCalls=Limit outgoing calls:",
    "MethodsFeatureModes_unlimitedCalls=unlimited",
    "MethodsFeatureModes_filterOutgoingCalls=Filter outgoing calls:",
    "MethodsFeatureModes_filterOutgoingTooltip=Filter calls from the selected classes or methods",
    "MethodsFeatureModes_noFilter=no filter",
    "MethodsFeatureModes_noClassSelected=No classes selected, use Profile Class action in editor or results or click the Add button:",
    "MethodsFeatureModes_oneClassSelected=Selected 1 class",
    "MethodsFeatureModes_multipleClassesSelected=Selected {0} classes",
    "MethodsFeatureModes_noMethodSelected=No methods selected, use Profile Method action in editor or results or click the Add button:",
    "MethodsFeatureModes_oneMethodSelected=Selected 1 method",
    "MethodsFeatureModes_multipleMethodsSelected=Selected {0} methods",
    "MethodsFeatureModes_addMethod=Select method",
    "MethodsFeatureModes_addClass=Select class",
    "MethodsFeatureModes_limitCallTreeToolTip=Limit depth of calls from the selected classes or methods",
    "MethodsFeatureModes_doNotProfileCoreJavaToolTip=Do not profile core Java classes (java.*, sun.*, com.sun.*, etc.)",
    "MethodsFeatureModes_definedClasses=Defined classes",
    "MethodsFeatureModes_classesLbl=Classes:",
    "MethodsFeatureModes_includeCalls=Include outgoing calls:",
    "MethodsFeatureModes_includeTooltip=Profile only outgoing calls of the defined classes or packages",
    "MethodsFeatureModes_excludeCalls=Exclude outgoing calls:",
    "MethodsFeatureModes_excludeTooltip=Do not profile outgoing calls of the defined classes or packages",
    "MethodsFeatureModes_classesHint=org.mypackage.**\norg.mypackage.*\norg.mypackage.MyClass",
    "MethodsFeatureModes_filterHint=org.mypackage.**\norg.mypackage.*\norg.mypackage.MyClass",
    "MethodsFeatureModes_classesTooltip=<html>Profile methods of these classes or packages:<br><br>"
            + "<code>&nbsp;org.mypackage.**&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</code>all classes in package and subpackages<br>"
            + "<code>&nbsp;org.mypackage.*&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</code>all classes in package<br>"
            + "<code>&nbsp;org.mypackage.MyClass&nbsp;&nbsp;</code>single class<br></html>",
    "MethodsFeatureModes_filterTooltip=<html>Include/exclude profiling outgoing calls from these classes or packages:<br><br>"
            + "<code>&nbsp;org.mypackage.**&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</code>all classes in package and subpackages<br>"
            + "<code>&nbsp;org.mypackage.*&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</code>all classes in package<br>"
            + "<code>&nbsp;org.mypackage.MyClass&nbsp;&nbsp;</code>single class<br><br>"
            + "Special case:<br><br>"
            + "<code>&nbsp;&lt;empty&gt;</code> or <code>*&nbsp;&nbsp;</code>include all classes<br></html>"
})
final class MethodsFeatureModes {
    
    private static abstract class MethodsMode extends FeatureMode {
        
        void initialize() {}
        
        void configureSettings(ProfilingSettings settings) {}
        
    }
    
    private static abstract class SampledMethodsMode extends MethodsMode {
        
        void configureSettings(ProfilingSettings settings) {
            super.configureSettings(settings);
            
            settings.setProfilingType(ProfilingSettings.PROFILE_CPU_SAMPLING);
            settings.setCPUProfilingType(CommonConstants.CPU_SAMPLED);
        }
        
        void confirmSettings() {}
        
        boolean pendingChanges() { return false; }

        boolean currentSettingsValid() { return true; }
        
        JComponent getUI() { return null; }
        
    }
    
    static abstract class AllClassesMode extends SampledMethodsMode {
        
        String getID() {
            return "AllClassesMode"; // NOI18N
        }

        String getName() {
            return Bundle.MethodsFeatureModes_allClasses();
        }

        void configureSettings(ProfilingSettings settings) {
            super.configureSettings(settings);
            
            settings.setInstrumentationFilter(new JavaTypeFilter());
        }
        
    }
    
    static abstract class ProjectClassesMode extends SampledMethodsMode {
        
        private final Collection<Lookup.Provider> selectedProjects;
        
        // --- External implementation -----------------------------------------
        
        abstract Lookup.Provider getProject();
        
        
        // --- API implementation ----------------------------------------------
        
        private static final String PROJECTS_FLAG = "PROJECTS_FLAG"; // NOI18N
        
        ProjectClassesMode() {
            selectedProjects = new HashSet();
            
            Collection<File> files = createFilesFromStorage();
            if (files.isEmpty()) {
                selectedProjects.add(getProject());
            } else {
                for (File file : files) if (file.exists()) {
                    FileObject fo = FileUtil.toFileObject(FileUtil.normalizeFile(file));
                    Lookup.Provider project = fo == null ? null : ProjectUtilities.getProject(fo);
                    if (fo != null) selectedProjects.add(project);
                }
                verifySelectedProjects(false);
            }
        }
        
        String getID() {
            return "ProjectClassesMode"; // NOI18N
        }

        String getName() {
            return Bundle.MethodsFeatureModes_projectClasses();
        }

        void configureSettings(ProfilingSettings settings) {
            super.configureSettings(settings);
            
            StringBuilder filter = new StringBuilder();
            
            for (Lookup.Provider project : selectedProjects) {
                ProjectContentsSupport pcs = ProjectContentsSupport.get(project);
                filter.append(pcs.getInstrumentationFilter(false));
                filter.append(" "); // NOI18N
                pcs.reset();
            }
            
            String s  = filter.toString().replace(". ", ".* ").replace(".,", ".*,").trim(); // NOI18N
            JavaTypeFilter f = new JavaTypeFilter(s, JavaTypeFilter.TYPE_INCLUSIVE);
            settings.setInstrumentationFilter(f);
        }
        
        void confirmSettings() {
            if (ui != null) {
                assert SwingUtilities.isEventDispatchThread();
                                
                saveSelection();
            }
        }
        
        boolean pendingChanges() {
            if (ui != null) {
                assert SwingUtilities.isEventDispatchThread();
                
                if (!filesEquals(createFilesFromSelection(), createFilesFromStorage())) return true;
            }
            return false;
        }
        
        boolean currentSettingsValid() {
            assert SwingUtilities.isEventDispatchThread();
            
            if (selectedProjects.isEmpty()) return false;
            
            return true;
        }
        
        
        private void saveSelection() {
            Collection<File> files = createFilesFromSelection();
            if (files.isEmpty()) {
                storeFlag(PROJECTS_FLAG, null);
            } else {
                StringBuilder sb = new StringBuilder();
                for (File file : files) {
                    try {
                        sb.append(file.getCanonicalPath());
                    } catch (IOException ex) {
                        sb.append(file.getAbsolutePath());
                    }
                    sb.append(File.pathSeparatorChar);
                }
                storeFlag(PROJECTS_FLAG, sb.toString());
            }
        }
        
        private Collection<File> createFilesFromStorage() {
            Set<File> files = new HashSet();
            
            String s = readFlag(PROJECTS_FLAG, null);
            if (s != null) {
                String[] sa = s.split(File.pathSeparator);
                for (String _s : sa) files.add(new File(_s));
            }
            
            return files;
        }
        
        private Collection<File> createFilesFromSelection() {
            Set<File> files = new HashSet();
            
            if (selectedProjects.size() > 1 || !selectedProjects.contains(getProject()))
                for (Lookup.Provider project : selectedProjects)
                    files.add(FileUtil.toFile(ProjectUtilities.getProjectDirectory(project)));
            
            return files;
        }
        
        private boolean filesEquals(Collection<File> files1, Collection<File> files2) {
            if (files1.size() != files2.size()) return false;
            for (File file1 : files1) if (!files2.contains(file1)) return false;
            return true;
        }
        
        // NOTE: must be executed in EDT except of calling from constructor (populating selectedProjects)
        private void verifySelectedProjects(boolean refreshLink) {
            if (selectedProjects.size() == 1 && selectedProjects.contains(getProject())) return;
            
            List<Lookup.Provider> projects = Arrays.asList(ProjectUtilities.getOpenedProjects());
            Iterator<Lookup.Provider> iterator = selectedProjects.iterator();
            while (iterator.hasNext()) if (!projects.contains(iterator.next())) iterator.remove();
            
            if (selectedProjects.isEmpty()) selectedProjects.add(getProject());
            
            if (refreshLink) refreshProjectsLink();
        }
        
        
        JComponent getUI() {
            if (ui == null) {
                final ChangeListener projectsListener = new ChangeListener() {
                    public void stateChanged(ChangeEvent e) {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() { verifySelectedProjects(true); }
                        });
                    }
                };
                ui = new SettingsPanel() {
                    public void addNotify() {
                        super.addNotify();
                        ProjectUtilities.addOpenProjectsListener(projectsListener);
                    }
                    public void removeNotify() {
                        ProjectUtilities.removeOpenProjectsListener(projectsListener);
                        super.removeNotify();
                    }
                };
                
                editProjectLink = new JButton() {
                    public void setText(String text) {
                        super.setText(Bundle.MethodsFeatureModes_editLink(text));
                    }
                    protected void fireActionPerformed(ActionEvent e) {
                        performEditProject();
                    }
                    public Dimension getMinimumSize() {
                        return getPreferredSize();
                    }
                    public Dimension getMaximumSize() {
                        return getPreferredSize();
                    }
                };
                editProjectLink.setContentAreaFilled(false);
                editProjectLink.setBorderPainted(true);
                editProjectLink.setMargin(new Insets(0, 0, 0, 0));
                editProjectLink.setBorder(BorderFactory.createEmptyBorder());
                editProjectLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                refreshProjectsLink();
                ui.add(editProjectLink);
            }
            return ui;
        }
        
        
        private void performEditProject() {
            new ProjectsSelector(selectedProjects) {
                protected void selectionChanged(Collection<Lookup.Provider> selected) {
                    selectedProjects.clear();
                    selectedProjects.addAll(selected);
                    refreshProjectsLink();
                    settingsChanged();
                }
            }.show(ui);
        }
        
        private void refreshProjectsLink() {
            if (editProjectLink == null) return;
            if (selectedProjects.size() == 1)
                editProjectLink.setText(Bundle.MethodsFeatureModes_selectedProject(
                                        ProjectUtilities.getDisplayName(selectedProjects.
                                        iterator().next())));
            else editProjectLink.setText(Bundle.MethodsFeatureModes_selectedProjects(
                                        selectedProjects.size()));
        }
        
        
        private JComponent ui;
        private JButton editProjectLink;
        
    }
    
    private static abstract class InstrMethodsMode extends MethodsMode {
        
        // --- External implementation -----------------------------------------
        
        abstract void selectionChanging();
        
        abstract void selectionChanged();
        
        abstract ProfilerSession getSession();
        
        
        // --- API implementation ----------------------------------------------
        private static final String CORE_JAVA_FILTER = "apple.laf.**, apple.awt.**, com.apple.**, com.sun.**, java.**, javax.**, sun.**, sunw.**, org.omg.CORBA.**, org.omg.CosNaming.**, COM.rsa.**"; // NOI18N
        
        private static final String OUTGOING_CALLS_ENABLED_FLAG = "OUTGOING_CALLS_ENABLED_FLAG"; // NOI18N
        private static final String OUTGOING_CALLS_FLAG = "OUTGOING_CALLS_FLAG"; // NOI18N
        private static final String SKIP_JAVA_FLAG = "SKIP_JAVA_FLAG"; // NOI18N // Note: used in 8.1
        private static final String FILTER_CALLS_FLAG = "FILTER_CALLS_FLAG"; // NOI18N
        private static final String FILTER_CALLS_VALUE_FLAG = "FILTER_CALLS_VALUE_FLAG"; // NOI18N
        private static final String SELECTION_FLAG = "SELECTION_FLAG"; // NOI18N
        
        private static final Integer OUTGOING_CALLS_DEFAULT = 5;
        
        private FeatureMode.Selection selection;
        private FilterSelector.FilterName filterName;
        private String filterValue;
        
        private boolean lastOutgoingSelected;
        
        
        void initialize() {
            // Migrate 8.1 settings
            String excludeJava = readFlag(SKIP_JAVA_FLAG, null);
            if (excludeJava != null) {
                boolean _excludeJava = Boolean.TRUE.toString().equals(excludeJava) ?
                                       true : false;
                
                // remove deprecated flag
                storeFlag(SKIP_JAVA_FLAG, null);
                
                if (_excludeJava) {
                    // default value, no need to set anything
                } else {
                    // no filtering, set the appropriate flag
                    storeFlag(FILTER_CALLS_FLAG, FilterSelector.FilterName.NO_FILTER.name());
                }
            }
            
            // Initialize selected filter
            String filter = readFlag(FILTER_CALLS_FLAG, null);
            if (filter != null) filterName = FilterSelector.FilterName.valueOf(filter);
            if (filterName == null) filterName = FilterSelector.FilterName.EXCLUDE_JAVA_FILTER;
            
            // Initialize custom filter
            filterValue = readFlag(FILTER_CALLS_VALUE_FLAG, ""); // NOI18N
        }
        

        void configureSettings(ProfilingSettings settings) {
            assert SwingUtilities.isEventDispatchThread();
            
            super.configureSettings(settings);            
            
            settings.setProfilingType(ProfilingSettings.PROFILE_CPU_PART);
            settings.setCPUProfilingType(settings.getSamplingInterval() <= 0 ?
                                         CommonConstants.CPU_INSTR_FULL :
                                         CommonConstants.CPU_INSTR_SAMPLED);
            
            String filterType = readFlag(FILTER_CALLS_FLAG, FilterSelector.FilterName.EXCLUDE_JAVA_FILTER.name());
            if (FilterSelector.FilterName.NO_FILTER.name().equals(filterType)) {
                settings.setInstrumentationFilter(new JavaTypeFilter());
            } else if (FilterSelector.FilterName.EXCLUDE_JAVA_FILTER.name().equals(filterType)) {
                settings.setInstrumentationFilter(new JavaTypeFilter(CORE_JAVA_FILTER, JavaTypeFilter.TYPE_EXCLUSIVE));
            } else {
                 String filterStrings = readFlag(FILTER_CALLS_VALUE_FLAG, CORE_JAVA_FILTER);
                 if (filterStrings.isEmpty() || "*".equals(filterStrings) || "**".equals(filterStrings)) { // NOI18N
                     settings.setInstrumentationFilter(new JavaTypeFilter());
                 } else {
                     filterStrings = getFlatValues(filterStrings.split("\\n")); // NOI18N
                     if (FilterSelector.FilterName.EXCLUDE_CUSTOM_FILTER.name().equals(filterType)) {
                         settings.setInstrumentationFilter(new JavaTypeFilter(filterStrings, JavaTypeFilter.TYPE_EXCLUSIVE));
                     } else if (FilterSelector.FilterName.INCLUDE_CUSTOM_FILTER.name().equals(filterType)) {
                         settings.setInstrumentationFilter(new JavaTypeFilter(filterStrings, JavaTypeFilter.TYPE_INCLUSIVE)); // NOI18N
                     }
                 }
            }
            
            HashSet<ClientUtils.SourceCodeSelection> _sel = getSelection();
            ClientUtils.SourceCodeSelection[] classes = _sel.toArray(new ClientUtils.SourceCodeSelection[0]);
            settings.addRootMethods(classes);
            
            if (Boolean.parseBoolean(readFlag(OUTGOING_CALLS_ENABLED_FLAG, Boolean.TRUE.toString())))
                settings.setStackDepthLimit(Integer.parseInt(readFlag(OUTGOING_CALLS_FLAG, OUTGOING_CALLS_DEFAULT.toString())));
            else settings.setStackDepthLimit(Integer.MAX_VALUE);
        }
        
        void confirmSettings() {
            if (ui != null) {
                assert SwingUtilities.isEventDispatchThread();
                
                storeFlag(OUTGOING_CALLS_ENABLED_FLAG, lastOutgoingSelected ? null : Boolean.FALSE.toString());
                
                String outgoingCalls = outgoingSpinner.getValue().toString();
                storeFlag(OUTGOING_CALLS_FLAG, OUTGOING_CALLS_DEFAULT.toString().equals(outgoingCalls) ? null : outgoingCalls);
                
                String filter = FilterSelector.FilterName.EXCLUDE_JAVA_FILTER.equals(filterName) ? null : filterName.name();
                storeFlag(FILTER_CALLS_FLAG, filter);
                
                storeFlag(FILTER_CALLS_VALUE_FLAG, filterValue.isEmpty() ? null : filterValue);
                
                saveSelection();
            }
        }
        
        boolean pendingChanges() {
            if (ui != null) {
                assert SwingUtilities.isEventDispatchThread();
                
                if (Boolean.parseBoolean(readFlag(OUTGOING_CALLS_ENABLED_FLAG, Boolean.TRUE.toString())) != lastOutgoingSelected)
                    return true;
                
                if (!outgoingSpinner.getValue().toString().equals(readFlag(OUTGOING_CALLS_FLAG, OUTGOING_CALLS_DEFAULT.toString())))
                    return true;
                
                String filter = readFlag(FILTER_CALLS_FLAG, FilterSelector.FilterName.EXCLUDE_JAVA_FILTER.name());
                if (!filter.equals(filterName.name())) return true;
                
                if (!readFlag(FILTER_CALLS_VALUE_FLAG, "").equals(filterValue)) return true; // NOI18N
                
                if (!initSelection(false).equals(getSelection())) return true;
            }
            return false;
        }
        
        boolean currentSettingsValid() {
            assert SwingUtilities.isEventDispatchThread();
            
            if (ui != null) {
                if (FilterSelector.FilterName.EXCLUDE_CUSTOM_FILTER.equals(filterName) ||
                    FilterSelector.FilterName.INCLUDE_CUSTOM_FILTER.equals(filterName))
                    if (filterValue.isEmpty()) return false;
            } else {
                String filter = readFlag(FILTER_CALLS_FLAG, FilterSelector.FilterName.EXCLUDE_JAVA_FILTER.name());
                if (FilterSelector.FilterName.EXCLUDE_CUSTOM_FILTER.name().equals(filter) ||
                    FilterSelector.FilterName.INCLUDE_CUSTOM_FILTER.name().equals(filter))
                    if (readFlag(FILTER_CALLS_VALUE_FLAG, "").isEmpty()) return false; // NOI18N
            }
            
            if (getSelection().isEmpty()) return false;

            return true;
        }
        
        HashSet<ClientUtils.SourceCodeSelection> getSelection() {
            if (selection == null) selection = initSelection(true);
            return selection;
        }
        
        private FeatureMode.Selection initSelection(final boolean events) {
            FeatureMode.Selection sel = new FeatureMode.Selection() {
                protected void changing() { selectionChanging(); }
                protected void changed() { selectionChanged(); updateSelectionCustomizer(); }
            };
            
            sel.disableEvents();
            
            String _sel = readFlag(SELECTION_FLAG, null);
            if (_sel != null)
                for (String s : _sel.split(" ")) // NOI18N
                    sel.add(ClientUtils.stringToSelection(s));
            
            if (events) sel.enableEvents();
            
            return sel;
        }
        
        private void saveSelection() {
            if (selection != null) {
                StringBuilder b = new StringBuilder();
                for (ClientUtils.SourceCodeSelection sel : selection) {
                    b.append(ClientUtils.selectionToString(sel));
                    b.append(" "); // NOI18N
                }
                String sel = b.toString();
                storeFlag(SELECTION_FLAG, sel.isEmpty() ? null : sel);
            }
        }
        
        
        // --- UI --------------------------------------------------------------
        
        private JComponent ui;
        private JPanel selectionContent;
        private JPanel noSelectionContent;
        private JLabel outgoingLabel;
        private JCheckBox outgoingChoice;
        private JLabel outgoingHint;
        private JSpinner outgoingSpinner;
        private JButton addSelectionButton;
        private JButton editSelectionLink;
        private JButton filterLink;
        
        
        protected abstract int getOutgoingCallsMaximum();
        
        protected abstract String noSelectionString();
        
        protected abstract String oneSelectionString();
        
        protected abstract String multipleSelectionsString(int count);
        
        protected abstract Icon getAddIcon();
        
        protected abstract String getAddTooltip();
        
        protected abstract void performAddSelection();
        
        protected abstract void performEditSelection(Component invoker);
        
        
        JComponent getUI() {
            if (ui == null) {
                ui = new SettingsPanel();
                
                selectionContent = new SettingsPanel();

                editSelectionLink = new JButton() {
                    public void setText(String text) {
                        super.setText(Bundle.MethodsFeatureModes_editLink(text));
                    }
                    protected void fireActionPerformed(ActionEvent e) {
                        performEditSelection(InstrMethodsMode.this.ui);
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
                
                outgoingLabel = new JLabel(Bundle.MethodsFeatureModes_outgoingCalls());

                lastOutgoingSelected = Boolean.parseBoolean(readFlag(OUTGOING_CALLS_ENABLED_FLAG, Boolean.TRUE.toString()));
                outgoingChoice = new JCheckBox(Bundle.MethodsFeatureModes_outgoingCalls(), lastOutgoingSelected) {
                    protected void fireActionPerformed(ActionEvent e) {
                        super.fireActionPerformed(e);
                        lastOutgoingSelected = isSelected();
                        updateControls();
                        settingsChanged();
                    }
                };
                outgoingChoice.setToolTipText(Bundle.MethodsFeatureModes_limitCallTreeToolTip());
                outgoingChoice.setOpaque(false);
                selectionContent.add(createStrut(outgoingChoice, 8, true));
                selectionContent.add(outgoingLabel);
                selectionContent.add(outgoingChoice);
                
                Insets i = outgoingChoice.getBorder().getBorderInsets(outgoingChoice);
                outgoingLabel.setBorder(BorderFactory.createEmptyBorder(0, i.left, 0, i.right));

                selectionContent.add(createStrut(outgoingChoice, 5, false));

                outgoingHint = new GrayLabel(Bundle.MethodsFeatureModes_unlimitedCalls());
                outgoingHint.setEnabled(false);
                outgoingHint.setVisible(!outgoingChoice.isSelected());
                selectionContent.add(outgoingHint);

                int outgoingCalls = Integer.parseInt(readFlag(OUTGOING_CALLS_FLAG, OUTGOING_CALLS_DEFAULT.toString()));
                outgoingSpinner = new JExtendedSpinner(new SpinnerNumberModel(outgoingCalls, 1, getOutgoingCallsMaximum(), 1)) {
                    public Dimension getPreferredSize() { return getMinimumSize(); }
                    public Dimension getMaximumSize() { return getMinimumSize(); }
                    protected void fireStateChanged() { settingsChanged(); super.fireStateChanged(); }
                };
                outgoingSpinner.setToolTipText(Bundle.MethodsFeatureModes_limitCallTreeToolTip());
                outgoingSpinner.setVisible(outgoingChoice.isSelected());
                selectionContent.add(outgoingSpinner);
                
                selectionContent.add(Box.createHorizontalStrut(10));
                if (UIUtils.isOracleLookAndFeel()) selectionContent.add(Box.createHorizontalStrut(4));

                JLabel filterLabel = new JLabel(Bundle.MethodsFeatureModes_filterOutgoingCalls());
                filterLabel.setToolTipText(Bundle.MethodsFeatureModes_filterOutgoingTooltip());
                selectionContent.add(filterLabel);
                
                selectionContent.add(createStrut(filterLabel, 5, false));

                filterLink = new JButton() {
                    public void setText(String text) {
                        super.setText(Bundle.MethodsFeatureModes_editLink(text));
                    }
                    protected void fireActionPerformed(ActionEvent e) {
                        performEditFilter(this);
                    }
                    public Dimension getMinimumSize() {
                        return getPreferredSize();
                    }
                    public Dimension getMaximumSize() {
                        return getPreferredSize();
                    }
                };
                filterLink.setContentAreaFilled(false);
                filterLink.setBorderPainted(true);
                filterLink.setMargin(new Insets(0, 0, 0, 0));
                filterLink.setBorder(BorderFactory.createEmptyBorder());
                filterLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                selectionContent.add(filterLink);
                updateControls();

                noSelectionContent = new SettingsPanel();

                GrayLabel noSelectionHint = new GrayLabel(noSelectionString());
                noSelectionHint.setEnabled(false);
                noSelectionContent.add(noSelectionHint);

                noSelectionContent.add(Box.createHorizontalStrut(5));

                addSelectionButton = new SmallButton(getAddIcon()) {
                    protected void fireActionPerformed(ActionEvent e) {
                        performAddSelection();
                    }
                    public Dimension getMinimumSize() {
                        return getPreferredSize();
                    }
                    public Dimension getMaximumSize() {
                        return getPreferredSize();
                    }
                };
                addSelectionButton.setToolTipText(getAddTooltip());
                noSelectionContent.add(addSelectionButton);
                updateSelectionCustomizer();
            }
            return ui;
        }
        
        private void updateSelectionCustomizer() {
            if (ui != null) {
                int count = getSelection().size();
                
                JPanel content = count == 0 ? noSelectionContent : selectionContent;
                if (ui.getComponentCount() > 0 && content == ui.getComponent(0)) content = null;
                
                if (count > 0) editSelectionLink.setText(count == 1 ? oneSelectionString() :
                                                         multipleSelectionsString(count));
                
                if (content != null) {
                    ui.removeAll();
                    ui.add(content);
                    ui.doLayout();
                    ui.repaint();
                }
            }
        }
        
        private void updateControls() {
            boolean outgoingRequired = FilterSelector.FilterName.NO_FILTER.equals(filterName) ||
                                       FilterSelector.FilterName.EXCLUDE_JAVA_FILTER.equals(filterName);
            outgoingLabel.setVisible(outgoingRequired);
            outgoingChoice.setVisible(!outgoingRequired);
            
            if (outgoingRequired) outgoingChoice.setSelected(true);
            else outgoingChoice.setSelected(lastOutgoingSelected);
            
            boolean outgoingDefined = outgoingChoice.isSelected();
            outgoingSpinner.setVisible(outgoingDefined);
            outgoingHint.setVisible(!outgoingDefined);
            
            filterLink.setText(filterName.toString());
        }
        
        private void performEditFilter(Component invoker) {
            new FilterSelector() {
                protected void filterChanged(FilterSelector.FilterName filterName, String filterValue) {
                    InstrMethodsMode.this.filterName = filterName;
                    InstrMethodsMode.this.filterValue = filterValue;
                    updateControls();
                    settingsChanged();
                }
            }.show(invoker.getParent(), filterName, filterValue);
        }
        
    }
    
    static abstract class SelectedClassesMode extends InstrMethodsMode {
        
        abstract void selectForProfiling(Collection<SourceClassInfo> classInfos);
        
        
        String getID() {
            return "SelectedClassesMode"; // NOI18N
        }

        String getName() {
            return Bundle.MethodsFeatureModes_selectedClasses();
        }
        
        
        protected int getOutgoingCallsMaximum() {
            return 10;
        }
        
        protected String noSelectionString() {
            return Bundle.MethodsFeatureModes_noClassSelected();
        }
        
        protected String oneSelectionString() {
            return Bundle.MethodsFeatureModes_oneClassSelected();
        }
        
        protected String multipleSelectionsString(int count) {
            return Bundle.MethodsFeatureModes_multipleClassesSelected(count);
        }
        
        
        protected Icon getAddIcon() {
            String iconMask = LanguageIcons.CLASS;
            Image baseIcon = Icons.getImage(iconMask);
            Image addBadge = Icons.getImage(GeneralIcons.BADGE_ADD);
            Image addImage = ImageUtilities.mergeImages(baseIcon, addBadge, 0, 0);
            return ImageUtilities.image2Icon(addImage);
        }
        
        protected String getAddTooltip() {
            return Bundle.MethodsFeatureModes_addClass();
        }
        
        
        protected void performAddSelection() {
            selectForProfiling(ClassMethodSelector.selectClasses(getSession()));
        }
        
        protected void performEditSelection(Component invoker) {
            ClassMethodList.showClasses(getSession(), getSelection(), invoker);
        }
        
    }
    
    static abstract class SelectedMethodsMode extends InstrMethodsMode {
        
        abstract void selectForProfiling(Collection<SourceMethodInfo> methodInfos);
        
        
        String getID() {
            return "SelectedMethodsMode"; // NOI18N
        }

        String getName() {
            return Bundle.MethodsFeatureModes_selectedMethods();
        }
        
        
        protected int getOutgoingCallsMaximum() {
            return 99;
        }
        
        protected String noSelectionString() {
            return Bundle.MethodsFeatureModes_noMethodSelected();
        }
        
        protected String oneSelectionString() {
            return Bundle.MethodsFeatureModes_oneMethodSelected();
        }
        
        protected String multipleSelectionsString(int count) {
            return Bundle.MethodsFeatureModes_multipleMethodsSelected(count);
        }
        
        
        protected Icon getAddIcon() {
            String iconMask = LanguageIcons.METHOD;
            Image baseIcon = Icons.getImage(iconMask);
            Image addBadge = Icons.getImage(GeneralIcons.BADGE_ADD);
            Image addImage = ImageUtilities.mergeImages(baseIcon, addBadge, 0, 0);
            return ImageUtilities.image2Icon(addImage);
        }
        
        protected String getAddTooltip() {
            return Bundle.MethodsFeatureModes_addMethod();
        }
        
        
        protected void performAddSelection() {
            selectForProfiling(ClassMethodSelector.selectMethods(getSession()));
        }
        
        protected void performEditSelection(Component invoker) {
            ClassMethodList.showMethods(getSession(), getSelection(), invoker);
        }
        
    }
    
    
    static abstract class CustomClassesMode extends MethodsMode {
        
        private static final String CLASSES_FLAG = "CLASSES_FLAG"; // NOI18N
        private static final String FILTER_FLAG = "FILTER_FLAG"; // NOI18N
        private static final String FILTER_MODE_FLAG = "FILTER_MODE_FLAG"; // NOI18N
        
        private static final int MIN_ROWS = 2;
        private static final int MAX_ROWS = 15;
        private static final int DEFAULT_ROWS = 3;
        private static final int MIN_COLUMNS = 10;
        private static final int MAX_COLUMNS = 100;
        private static final int DEFAULT_COLUMNS = 40;
        
        private JComponent ui;
        private TextArea classesArea;
        private TextArea filterArea;
        private JRadioButton includeChoice;
        private JRadioButton excludeChoice;
        

        String getID() {
            return "CustomMethodsMode"; // NOI18N
        }

        String getName() {
            return Bundle.MethodsFeatureModes_definedClasses();
        }
        
        void configureSettings(ProfilingSettings settings) {
            assert SwingUtilities.isEventDispatchThread();
            
            super.configureSettings(settings);            
            
            settings.setProfilingType(ProfilingSettings.PROFILE_CPU_PART);
            settings.setCPUProfilingType(settings.getSamplingInterval() <= 0 ?
                                         CommonConstants.CPU_INSTR_FULL :
                                         CommonConstants.CPU_INSTR_SAMPLED);
            
            String[] rootsLines = readFlag(CLASSES_FLAG, "").split("\\n"); // NOI18N
            String[] rootsValues = GenericFilter.values(getFlatValues(rootsLines));
            ClientUtils.SourceCodeSelection[] roots = (rootsValues.length == 1 && rootsValues[0].isEmpty()) ?
                new ClientUtils.SourceCodeSelection[0] :
                new ClientUtils.SourceCodeSelection[rootsValues.length];
            for (int i = 0; i < roots.length; i++)
                roots[i] = new ClientUtils.SourceCodeSelection(rootsValues[i], "*", null); // NOI18N
            settings.addRootMethods(roots);
            
            String filter = readFlag(FILTER_FLAG, ""); // NOI18N
            if (filter.isEmpty() || "*".equals(filter) || "**".equals(filter)) { // NOI18N
                settings.setInstrumentationFilter(new JavaTypeFilter());
            } else {
                int filterType = Boolean.parseBoolean(readFlag(FILTER_MODE_FLAG, Boolean.TRUE.toString())) == true ?
                                 JavaTypeFilter.TYPE_INCLUSIVE : JavaTypeFilter.TYPE_EXCLUSIVE;
                String filterValue = getFlatValues(filter.split("\\n")); // NOI18N
                settings.setInstrumentationFilter(new JavaTypeFilter(filterValue, filterType));
            }
            
            settings.setStackDepthLimit(Integer.MAX_VALUE);
        }

        void confirmSettings() {
            if (ui != null) {
                assert SwingUtilities.isEventDispatchThread();
                
                String classes = classesArea.showsHint() ? "" : // NOI18N
                                 classesArea.getText().trim();
                storeFlag(CLASSES_FLAG, classes.isEmpty() ? null : classes);
                
                String filter = filterArea.showsHint() ? "" : // NOI18N
                                filterArea.getText().trim();
                storeFlag(FILTER_FLAG, filter.isEmpty() ? null : filter);
                
                boolean filterMode = includeChoice.isSelected();
                storeFlag(FILTER_MODE_FLAG, filterMode == true ? null : Boolean.FALSE.toString());
            }
        }

        boolean pendingChanges() {
            if (ui != null) {
                assert SwingUtilities.isEventDispatchThread();
                
                String classes = classesArea.showsHint() ? "" : // NOI18N
                                 classesArea.getText().trim();
                if (!classes.equals(readFlag(CLASSES_FLAG, ""))) return true; // NOI18N
                
                String filter = filterArea.showsHint() ? "" : // NOI18N
                                filterArea.getText().trim();
                if (!filter.equals(readFlag(FILTER_FLAG, ""))) return true; // NOI18N
                
                if (Boolean.parseBoolean(readFlag(FILTER_MODE_FLAG, Boolean.TRUE.toString())) != includeChoice.isSelected())
                    return true;
            }
            return false;
        }

        boolean currentSettingsValid() {
            assert SwingUtilities.isEventDispatchThread();
            
            if (ui != null) {
                if (classesArea.showsHint() || classesArea.getText().trim().isEmpty()) return false;
            } else {
                if (readFlag(CLASSES_FLAG, "").isEmpty()) return false; // NOI18N
            }
            
            return true;
        }

        JComponent getUI() {
            if (ui == null) {
                JPanel p = new JPanel(new GridBagLayout());
                p.setOpaque(false);
                
                GridBagConstraints c;
        
                JPanel classesPanel = new SettingsPanel();
                classesPanel.add(new JLabel(Bundle.MethodsFeatureModes_classesLbl()));
                c = new GridBagConstraints();
                c.gridx = 0;
                c.gridy = 0;
                c.fill = GridBagConstraints.NONE;
                c.insets = new Insets(0, 0, 0, 5);
                c.anchor = GridBagConstraints.NORTHWEST;
                p.add(classesPanel, c);
                
                class Resizer {
                    
                    private TextArea area1, area2;
                    private JComponent container1, container2;
                    
                    void setContext(TextArea area1, TextArea area2, JComponent container1, JComponent container2) {
                        this.area1 = area1; this.area2 = area2;
                        this.container1 = container1; this.container2 = container2;
                    }
                    
                    void resize() {
                        area1.setColumns(readColumns1());
                        area2.setColumns(readColumns2());
                        
                        int rows = readRows();
                        area1.setRows(rows);
                        area2.setRows(rows);
                        
                        area1.invalidate();
                        area2.invalidate();
                        
                        container1.setPreferredSize(null);
                        container1.setPreferredSize(container1.getPreferredSize());
                        container1.setMinimumSize(container1.getPreferredSize());
                        
                        container2.setPreferredSize(null);
                        container2.setPreferredSize(container2.getPreferredSize());
                        container2.setMinimumSize(container2.getPreferredSize());
                        
                        JComponent root = SwingUtilities.getRootPane(container1);
                        root.doLayout();
                        root.repaint();
                        
                        area1.setColumns(0);
                        area2.setColumns(0);
                    }
                    
                }
                final Resizer resizer = new Resizer();
                
                classesArea = new TextArea(readFlag(CLASSES_FLAG, "")) { // NOI18N
                    protected void changed() {
                        settingsChanged();
                    }
                    protected boolean changeSize(boolean vertical, boolean direction) {
                        if (vertical) {
                            int rows = readRows();
                            if (direction) rows = Math.min(rows + 1, MAX_ROWS);
                            else rows = Math.max(rows - 1, MIN_ROWS);
                            storeRows(rows);
                        } else {
                            int cols = readColumns1();
                            if (direction) cols = Math.min(cols + 3, MAX_COLUMNS);
                            else cols = Math.max(cols - 3, MIN_COLUMNS);
                            storeColumns1(cols);
                        }
                        
                        resizer.resize();
                        return true;
                    }
                    protected boolean resetSize() {
                        storeRows(DEFAULT_ROWS);
                        storeColumns1(DEFAULT_COLUMNS);
                
                        resizer.resize();
                        return true;
                    }
                    protected void customizePopup(JPopupMenu popup) {
                        popup.addSeparator();
                        popup.add(createResizeMenu());
                    }
                    public Point getToolTipLocation(MouseEvent event) {
                        Component scroll = getParent().getParent();
                        return SwingUtilities.convertPoint(scroll, 0, scroll.getHeight(), this);
                    }
                };
                classesArea.setFont(new Font("Monospaced", Font.PLAIN, classesArea.getFont().getSize())); // NOI18N
                classesArea.setRows(readRows());
                classesArea.setColumns(readColumns1());
                JScrollPane classesScroll = new JScrollPane(classesArea);
                classesScroll.setPreferredSize(classesScroll.getPreferredSize());
                classesScroll.setMinimumSize(classesScroll.getPreferredSize());
                classesArea.setColumns(0);
                classesArea.setHint(Bundle.MethodsFeatureModes_classesHint());
                classesArea.setToolTipText(Bundle.MethodsFeatureModes_classesTooltip());
                c = new GridBagConstraints();
                c.gridx = 1;
                c.gridy = 0;
                c.gridheight = GridBagConstraints.REMAINDER;
                c.weightx = 0.5;
                c.weighty = 1;
                c.fill = GridBagConstraints.VERTICAL;
                c.insets = new Insets(0, 0, 0, 10);
                c.anchor = GridBagConstraints.NORTHWEST;
                p.add(classesScroll, c);
                
                boolean filterMode = Boolean.TRUE.toString().equals(readFlag(FILTER_MODE_FLAG, Boolean.TRUE.toString()));
                ButtonGroup bg = new ButtonGroup();
                JPanel filterPanel = new SettingsPanel();
                includeChoice = new JRadioButton(Bundle.MethodsFeatureModes_includeCalls()) {
                    protected void fireActionPerformed(ActionEvent e) {
                        super.fireActionPerformed(e);
                        settingsChanged();
                    }
                };
                includeChoice.setToolTipText(Bundle.MethodsFeatureModes_includeTooltip());
                Border b = includeChoice.getBorder();
                Insets i = b != null ? b.getBorderInsets(includeChoice) : null;
                includeChoice.setOpaque(false);
                bg.add(includeChoice);
                includeChoice.setSelected(filterMode);
                filterPanel.add(includeChoice);
                c = new GridBagConstraints();
                c.gridx = 2;
                c.gridy = 0;
                c.fill = GridBagConstraints.NONE;
                c.insets = i == null ? new Insets(0, 0, 0, 0) :
                           new Insets(0, 1 - i.left, 0, 1 - i.right);
                c.anchor = GridBagConstraints.NORTHWEST;
                p.add(filterPanel, c);
                
                excludeChoice = new JRadioButton(Bundle.MethodsFeatureModes_excludeCalls()) {
                    protected void fireActionPerformed(ActionEvent e) {
                        super.fireActionPerformed(e);
                        settingsChanged();
                    }
                };
                excludeChoice.setToolTipText(Bundle.MethodsFeatureModes_excludeTooltip());
                b = excludeChoice.getBorder();
                i = b != null ? b.getBorderInsets(excludeChoice) : null;
                excludeChoice.setOpaque(false);
                bg.add(excludeChoice);
                excludeChoice.setSelected(!filterMode);
                c = new GridBagConstraints();
                c.gridx = 2;
                c.gridy = 1;
                c.fill = GridBagConstraints.NONE;
                c.insets = i == null ? new Insets(0, 0, 0, 0) :
                           new Insets(1 - i.top, 1 - i.left, 0, 1 - i.right);
                c.anchor = GridBagConstraints.NORTHWEST;
                p.add(excludeChoice, c);
                
                filterArea = new TextArea(readFlag(FILTER_FLAG, "")) { // NOI18N
                    protected void changed() {
                        settingsChanged();
                    }
                    protected boolean changeSize(boolean vertical, boolean direction) {
                        if (vertical) {
                            int rows = readRows();
                            if (direction) rows = Math.min(rows + 1, MAX_ROWS);
                            else rows = Math.max(rows - 1, MIN_ROWS);
                            storeRows(rows);
                        } else {
                            int cols = readColumns2();
                            if (direction) cols = Math.min(cols + 3, MAX_COLUMNS);
                            else cols = Math.max(cols - 3, MIN_COLUMNS);
                            storeColumns2(cols);
                        }
                        
                        resizer.resize();               
                        return true;
                    }
                    protected boolean resetSize() {
                        storeRows(DEFAULT_ROWS);
                        storeColumns2(DEFAULT_COLUMNS);
                
                        resizer.resize();
                        return true;
                    }
                    protected void customizePopup(JPopupMenu popup) {
                        popup.addSeparator();
                        popup.add(createResizeMenu());
                    }
                    public Point getToolTipLocation(MouseEvent event) {
                        Component scroll = getParent().getParent();
                        return SwingUtilities.convertPoint(scroll, 0, scroll.getHeight(), this);
                    }
                };
                filterArea.setFont(new Font("Monospaced", Font.PLAIN, classesArea.getFont().getSize())); // NOI18N
                filterArea.setRows(readRows());
                filterArea.setColumns(readColumns2());
                JScrollPane filterScroll = new JScrollPane(filterArea);
                filterScroll.setPreferredSize(filterScroll.getPreferredSize());
                filterScroll.setMinimumSize(filterScroll.getPreferredSize());
                filterArea.setColumns(0);
                filterArea.setHint(Bundle.MethodsFeatureModes_filterHint());
                filterArea.setToolTipText(Bundle.MethodsFeatureModes_filterTooltip());
                c = new GridBagConstraints();
                c.gridx = 3;
                c.gridy = 0;
                c.gridheight = GridBagConstraints.REMAINDER;
                c.weightx = 0.5;
                c.weighty = 1;
                c.fill = GridBagConstraints.VERTICAL;
                c.insets = new Insets(0, 4, 0, 1);
                c.anchor = GridBagConstraints.NORTHWEST;
                p.add(filterScroll, c);
                
                resizer.setContext(classesArea, filterArea, classesScroll, filterScroll);
                
                ui = p;
                
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() { settingsChanged(); }
                });
            }
            return ui;
        }
        
        private int readRows() {
            return NbPreferences.forModule(MethodsFeatureModes.class).getInt("MethodsFeatureModes.rows", DEFAULT_ROWS); // NOI18N
        }
        
        private void storeRows(int rows) {
            NbPreferences.forModule(MethodsFeatureModes.class).putInt("MethodsFeatureModes.rows", rows); // NOI18N
        }
        
        private int readColumns1() {
            return NbPreferences.forModule(MethodsFeatureModes.class).getInt("MethodsFeatureModes.columns1", DEFAULT_COLUMNS); // NOI18N
        }
        
        private void storeColumns1(int columns) {
            NbPreferences.forModule(MethodsFeatureModes.class).putInt("MethodsFeatureModes.columns1", columns); // NOI18N
        }
        
        private int readColumns2() {
            return NbPreferences.forModule(MethodsFeatureModes.class).getInt("MethodsFeatureModes.columns2", DEFAULT_COLUMNS); // NOI18N
        }
        
        private void storeColumns2(int columns) {
            NbPreferences.forModule(MethodsFeatureModes.class).putInt("MethodsFeatureModes.columns2", columns); // NOI18N
        }
        
    }
    
    private static String getFlatValues(String[] values) {
        StringBuilder convertedValue = new StringBuilder();

        for (int i = 0; i < values.length; i++) {
            String filterValue = values[i].trim();
            if ((i != (values.length - 1)) && !filterValue.endsWith(",")) // NOI18N
                filterValue = filterValue + ","; // NOI18N
            convertedValue.append(filterValue);
        }

        return convertedValue.toString();
    }
    
    private static Component createStrut(JComponent c, int width, boolean before) {
        Border b = c.getBorder();
        Insets i = b != null ? b.getBorderInsets(c) : null;
        int w = i == null ? width : Math.max(width - (before ? i.left : i.right), 0);
        return Box.createHorizontalStrut(w);
    }
    
}
