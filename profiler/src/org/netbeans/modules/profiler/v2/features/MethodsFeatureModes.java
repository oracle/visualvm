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
import java.awt.event.ActionEvent;
import java.util.HashSet;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.lib.profiler.common.filters.SimpleFilter;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.JExtendedSpinner;
import org.netbeans.lib.profiler.ui.swing.GrayLabel;
import org.netbeans.lib.profiler.ui.swing.SmallButton;
import org.netbeans.lib.profiler.ui.swing.TextArea;
import org.netbeans.modules.profiler.api.icons.GeneralIcons;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.LanguageIcons;
import org.netbeans.modules.profiler.api.java.SourceClassInfo;
import org.netbeans.modules.profiler.api.java.SourceMethodInfo;
import org.netbeans.modules.profiler.api.project.ProjectContentsSupport;
import org.netbeans.modules.profiler.v2.ProfilerSession;
import org.netbeans.modules.profiler.v2.impl.ClassMethodList;
import org.netbeans.modules.profiler.v2.impl.ClassMethodSelector;
import org.netbeans.modules.profiler.v2.ui.SettingsPanel;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

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
    "MethodsFeatureModes_outgoingCalls=Limit outgoing calls:",
    "MethodsFeatureModes_skipJavaClasses=Skip Java core classes",
    "MethodsFeatureModes_noClassSelected=No classes selected, use Profile Class action in editor or results or click the Add button:",
    "MethodsFeatureModes_oneClassSelected=Selected 1 class",
    "MethodsFeatureModes_multipleClassesSelected=Selected {0} classes",
    "MethodsFeatureModes_noMethodSelected=No methods selected, use Profile Method action in editor or results or click the Add button:",
    "MethodsFeatureModes_oneMethodSelected=Selected 1 method",
    "MethodsFeatureModes_multipleMethodsSelected=Selected {0} methods",
    "MethodsFeatureModes_addMethod=Select method",
    "MethodsFeatureModes_addClass=Select class",
    "MethodsFeatureModes_limitCallTreeToolTip=Limit depth of results call tree",
    "MethodsFeatureModes_doNotProfileCoreJavaToolTip=Do not profile core Java classes (java.*, sun.*, com.sun.*, etc.)",
    "MethodsFeatureModes_definedClasses=Defined classes",
    "MethodsFeatureModes_classesLbl=Classes:",
    "MethodsFeatureModes_includeCalls=Include outgoing calls:",
    "MethodsFeatureModes_excludeCalls=Exclude outgoing calls:",
    "MethodsFeatureModes_classesHint=org.mypackage.**\norg.mypackage.*\norg.mypackage.MyClass",
    "MethodsFeatureModes_filterHint=<empty>\norg.mypackage.*\norg.mypackage.MyClass"
})
final class MethodsFeatureModes {
    
    private static abstract class MethodsMode extends FeatureMode {
        
        void configureSettings(ProfilingSettings settings) {
        }
        
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
            
            settings.setSelectedInstrumentationFilter(null);
        }
        
    }
    
    static abstract class ProjectClassesMode extends SampledMethodsMode {
        
        // --- External implementation -----------------------------------------
        
        abstract Lookup.Provider getProject();
        
        
        // --- API implementation ----------------------------------------------
        
        String getID() {
            return "ProjectClassesMode"; // NOI18N
        }

        String getName() {
            return Bundle.MethodsFeatureModes_projectClasses();
        }

        void configureSettings(ProfilingSettings settings) {
            super.configureSettings(settings);
                
            ProjectContentsSupport pcs = ProjectContentsSupport.get(getProject());
            String filter = pcs.getInstrumentationFilter(false);
            SimpleFilter f = new SimpleFilter("", SimpleFilter.SIMPLE_FILTER_INCLUSIVE, filter); // NOI18N
            settings.setSelectedInstrumentationFilter(f);
        }
        
    }
    
    private static abstract class InstrMethodsMode extends MethodsMode {
        
        // --- External implementation -----------------------------------------
        
        abstract void selectionChanging();
        
        abstract void selectionChanged();
        
        abstract ProfilerSession getSession();
        
        
        // --- API implementation ----------------------------------------------
        private static final String CORE_JAVA_FILTER = "apple.laf., apple.awt., com.apple., com.sun., java., javax., sun., sunw., org.omg.CORBA, org.omg.CosNaming., COM.rsa."; // NOI18N
        
        private static final String OUTGOING_CALLS_FLAG = "OUTGOING_CALLS_FLAG"; // NOI18N
        private static final String SKIP_JAVA_FLAG = "SKIP_JAVA_FLAG"; // NOI18N
        private static final String SELECTION_FLAG = "SELECTION_FLAG"; // NOI18N
        
        private static final Integer OUTGOING_CALLS_DEFAULT = 5;
        
        private FeatureMode.Selection selection;
        

        void configureSettings(ProfilingSettings settings) {
            assert SwingUtilities.isEventDispatchThread();
            
            super.configureSettings(settings);            
            
            settings.setProfilingType(ProfilingSettings.PROFILE_CPU_PART);
            settings.setCPUProfilingType(settings.getSamplingInterval() <= 0 ?
                                         CommonConstants.CPU_INSTR_FULL :
                                         CommonConstants.CPU_INSTR_SAMPLED);
            
            boolean filter = Boolean.parseBoolean(readFlag(SKIP_JAVA_FLAG, Boolean.TRUE.toString()));
            settings.setSelectedInstrumentationFilter(!filter ? SimpleFilter.NO_FILTER :
                    new SimpleFilter("", SimpleFilter.SIMPLE_FILTER_EXCLUSIVE, CORE_JAVA_FILTER)); // NOI18N
            
            StringBuilder b = new StringBuilder();
            HashSet<ClientUtils.SourceCodeSelection> _sel = getSelection();
            ClientUtils.SourceCodeSelection[] classes = _sel.toArray(
                    new ClientUtils.SourceCodeSelection[_sel.size()]);
            for (int i = 0; i < classes.length; i++) {
                b.append(classes[i].getClassName());
                if (i < classes.length - 1) b.append(", "); // NOI18N
            }
            settings.addRootMethods(classes);
            
            String o = readFlag(OUTGOING_CALLS_FLAG, OUTGOING_CALLS_DEFAULT.toString());
            settings.setStackDepthLimit(Integer.parseInt(o));
        }
        
        void confirmSettings() {
            if (ui != null) {
                assert SwingUtilities.isEventDispatchThread();
                
                String outgoingCalls = outgoingSpinner.getValue().toString();
                storeFlag(OUTGOING_CALLS_FLAG, OUTGOING_CALLS_DEFAULT.toString().equals(outgoingCalls) ? null : outgoingCalls);
                
                storeFlag(SKIP_JAVA_FLAG, filterJava.isSelected() ? null : Boolean.FALSE.toString());
                
                saveSelection();
            }
        }
        
        boolean pendingChanges() {
            if (ui != null) {
                assert SwingUtilities.isEventDispatchThread();
                
                if (!outgoingSpinner.getValue().toString().equals(readFlag(OUTGOING_CALLS_FLAG, OUTGOING_CALLS_DEFAULT.toString())))
                    return true;
                
                if (Boolean.parseBoolean(readFlag(SKIP_JAVA_FLAG, Boolean.TRUE.toString())) != filterJava.isSelected())
                    return true;
                
                if (!initSelection(false).equals(getSelection())) return true;
            }
            return false;
        }

        boolean currentSettingsValid() {
            return !getSelection().isEmpty();
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
        private JSpinner outgoingSpinner;
        private JCheckBox filterJava;
        private JButton addSelectionButton;
        private JButton editSelectionLink;
        
        
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

                selectionContent.add(Box.createHorizontalStrut(8));
                
                JLabel outgoingLabel = new JLabel(Bundle.MethodsFeatureModes_outgoingCalls());
                outgoingLabel.setToolTipText(Bundle.MethodsFeatureModes_limitCallTreeToolTip());
                selectionContent.add(outgoingLabel);

                selectionContent.add(Box.createHorizontalStrut(5));

                int outgoingCalls = Integer.parseInt(readFlag(OUTGOING_CALLS_FLAG, OUTGOING_CALLS_DEFAULT.toString()));
                outgoingSpinner = new JExtendedSpinner(new SpinnerNumberModel(outgoingCalls, 1, getOutgoingCallsMaximum(), 1)) {
                    public Dimension getPreferredSize() { return getMinimumSize(); }
                    public Dimension getMaximumSize() { return getMinimumSize(); }
                    protected void fireStateChanged() { settingsChanged(); super.fireStateChanged(); }
                };
                outgoingLabel.setLabelFor(outgoingSpinner);
                outgoingSpinner.setToolTipText(Bundle.MethodsFeatureModes_limitCallTreeToolTip());
                selectionContent.add(outgoingSpinner);
                
                selectionContent.add(Box.createHorizontalStrut(6));
                if (UIUtils.isOracleLookAndFeel()) selectionContent.add(Box.createHorizontalStrut(4));
                
                boolean filter = Boolean.parseBoolean(readFlag(SKIP_JAVA_FLAG, Boolean.TRUE.toString()));
                filterJava = new JCheckBox(Bundle.MethodsFeatureModes_skipJavaClasses(), filter) {
                    protected void fireActionPerformed(ActionEvent e) {
                        super.fireActionPerformed(e);
                        settingsChanged();
                    }
                };
                filterJava.setToolTipText(Bundle.MethodsFeatureModes_doNotProfileCoreJavaToolTip());
                filterJava.setOpaque(false);
                selectionContent.add(filterJava);

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
        
    }
    
    static abstract class SelectedClassesMode extends InstrMethodsMode {
        
        abstract void selectForProfiling(SourceClassInfo classInfo);
        
        
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
            SourceClassInfo classInfo = ClassMethodSelector.selectClass(getSession());
            if (classInfo != null) selectForProfiling(classInfo);
        }
        
        protected void performEditSelection(Component invoker) {
            ClassMethodList.showClasses(getSession(), getSelection(), invoker);
        }
        
    }
    
    static abstract class SelectedMethodsMode extends InstrMethodsMode {
        
        abstract void selectForProfiling(SourceMethodInfo methodInfo);
        
        
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
            SourceMethodInfo methodInfo = ClassMethodSelector.selectMethod(getSession());
            if (methodInfo != null) selectForProfiling(methodInfo);
        }
        
        protected void performEditSelection(Component invoker) {
            ClassMethodList.showMethods(getSession(), getSelection(), invoker);
        }
        
    }
    
    
    static abstract class CustomClassesMode extends MethodsMode {
        
        private static final String CLASSES_FLAG = "CLASSES_FLAG"; // NOI18N
        private static final String FILTER_FLAG = "FILTER_FLAG"; // NOI18N
        private static final String FILTER_MODE_FLAG = "FILTER_MODE_FLAG"; // NOI18N
        
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
            
            String[] rootValues = readFlag(CLASSES_FLAG, "").split(","); // NOI18N
            ClientUtils.SourceCodeSelection[] roots = (rootValues.length == 1 && rootValues[0].isEmpty()) ?
                new ClientUtils.SourceCodeSelection[0] :
                new ClientUtils.SourceCodeSelection[rootValues.length];
            for (int i = 0; i < roots.length; i++)
                roots[i] = new ClientUtils.SourceCodeSelection(rootValues[i], "*", null); // NOI18N
            settings.addRootMethods(roots);
            
            String filter = readFlag(FILTER_FLAG, ""); // NOI18N
            if (filter.isEmpty() || "*".equals(filter)) { // NOI18N
                settings.setSelectedInstrumentationFilter(SimpleFilter.NO_FILTER);
            } else {
                int filterType = Boolean.parseBoolean(readFlag(FILTER_MODE_FLAG, Boolean.TRUE.toString())) == true ?
                                 SimpleFilter.SIMPLE_FILTER_INCLUSIVE : SimpleFilter.SIMPLE_FILTER_EXCLUSIVE;
                String filterValue = getFlatValues(filterArea.getText().split("\\n")); // NOI18N
                settings.setSelectedInstrumentationFilter(new SimpleFilter("", filterType, filterValue)); // NOI18N
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
            if (ui != null) {
                assert SwingUtilities.isEventDispatchThread();
                
                if (classesArea.showsHint() || classesArea.getText().trim().isEmpty()) return false;
//                if (filterArea.showsHint() || filterArea.getText().trim().isEmpty()) return false;
                
                return true;
            }
            return false;
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
                
                classesArea = new TextArea(readFlag(CLASSES_FLAG, "")) { // NOI18N
                    protected void changed() { settingsChanged(); }
                };
                classesArea.setFont(new Font("Monospaced", Font.PLAIN, classesArea.getFont().getSize())); // NOI18N
                classesArea.setRows(3);
                classesArea.setColumns(40);
                JScrollPane classesScroll = new JScrollPane(classesArea);
                classesScroll.setPreferredSize(classesScroll.getPreferredSize());
                classesScroll.setMinimumSize(classesScroll.getPreferredSize());
                classesArea.setColumns(0);
                classesArea.setHint(Bundle.MethodsFeatureModes_classesHint());
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
                    protected void changed() { settingsChanged(); }
                };
                filterArea.setFont(new Font("Monospaced", Font.PLAIN, classesArea.getFont().getSize())); // NOI18N
                filterArea.setRows(3);
                filterArea.setColumns(40);
                JScrollPane filterScroll = new JScrollPane(filterArea);
                filterScroll.setPreferredSize(filterScroll.getPreferredSize());
                filterScroll.setMinimumSize(filterScroll.getPreferredSize());
                filterArea.setColumns(0);
                filterArea.setHint(Bundle.MethodsFeatureModes_filterHint());
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
                
                ui = p;
                
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() { settingsChanged(); }
                });
            }
            return ui;
        }
        
    }
    
}
