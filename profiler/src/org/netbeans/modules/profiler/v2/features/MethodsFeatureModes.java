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
import java.util.HashSet;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.lib.profiler.common.filters.SimpleFilter;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.ui.components.JExtendedSpinner;
import org.netbeans.modules.profiler.api.java.SourceClassInfo;
import org.netbeans.modules.profiler.api.java.SourceMethodInfo;
import org.netbeans.modules.profiler.api.project.ProjectContentsSupport;
import org.netbeans.modules.profiler.v2.ProfilerSession;
import org.netbeans.modules.profiler.v2.impl.ClassMethodList;
import org.netbeans.modules.profiler.v2.impl.ClassMethodSelector;
import org.netbeans.modules.profiler.v2.ui.GrayLabel;
import org.netbeans.modules.profiler.v2.ui.SmallButton;
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
    "MethodsFeatureModes_outgoingCalls=Outgoing calls:",
    "MethodsFeatureModes_noClassSelected=No classes selected, use Profile Class action in editor or results or click the Add button:",
    "MethodsFeatureModes_oneClassSelected=Selected 1 class",
    "MethodsFeatureModes_multipleClassesSelected=Selected {0} classes",
    "MethodsFeatureModes_noMethodSelected=No methods selected, use Profile Method action in editor or results or click the Add button:",
    "MethodsFeatureModes_oneMethodSelected=Selected 1 method",
    "MethodsFeatureModes_multipleMethodsSelected=Selected {0} methods"
})
final class MethodsFeatureModes {
    
    private static abstract class MethodsMode extends FeatureMode {
        
        void configureSettings(ProfilingSettings settings) {
            // TODO: read from global settings (Options)
            settings.setThreadCPUTimerOn(true);
            settings.setInstrumentGetterSetterMethods(false);
            settings.setInstrumentEmptyMethods(false);
            settings.setInstrumentMethodInvoke(true);
            settings.setExcludeWaitTime(true);
        }
        
    }
    
    private static abstract class SampledMethodsMode extends MethodsMode {
        
        void configureSettings(ProfilingSettings settings) {
            super.configureSettings(settings);
            
            settings.setProfilingType(ProfilingSettings.PROFILE_CPU_SAMPLING);
            settings.setCPUProfilingType(CommonConstants.CPU_SAMPLED);
            settings.setSamplingFrequency(10);
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
        
        private static final String OUTGOING_CALLS_FLAG = "OUTGOING_CALLS_FLAG"; // NOI18N
        private static final String SELECTION_FLAG = "SELECTION_FLAG"; // NOI18N
        
        private static final Integer OUTGOING_CALLS_DEFAULT = 5;
        
        private FeatureMode.Selection selection;
        

        void configureSettings(ProfilingSettings settings) {
            assert SwingUtilities.isEventDispatchThread();
            
            super.configureSettings(settings);            
            
            settings.setProfilingType(ProfilingSettings.PROFILE_CPU_PART);
            settings.setCPUProfilingType(CommonConstants.CPU_INSTR_FULL);
            settings.setInstrScheme(CommonConstants.INSTRSCHEME_LAZY);
            settings.setInstrumentSpawnedThreads(false);
            
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

            settings.setSelectedInstrumentationFilter(null);
        }
        
        void confirmSettings() {
            if (ui != null) {
                assert SwingUtilities.isEventDispatchThread();
                
                String outgoingCalls = outgoingSpinner.getValue().toString();
                storeFlag(OUTGOING_CALLS_FLAG, OUTGOING_CALLS_DEFAULT.toString().equals(outgoingCalls) ? null : outgoingCalls);
                
                saveSelection();
            }
        }
        
        boolean pendingChanges() {
            if (ui != null) {
                assert SwingUtilities.isEventDispatchThread();
                
                if (!outgoingSpinner.getValue().toString().equals(readFlag(OUTGOING_CALLS_FLAG, OUTGOING_CALLS_DEFAULT.toString())))
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
        private JButton addSelectionButton;
        private JButton editSelectionLink;
        
        
        protected abstract String noSelectionString();
        
        protected abstract String oneSelectionString();
        
        protected abstract String multipleSelectionsString(int count);
        
        protected abstract void performAddSelection();
        
        protected abstract void performEditSelection(Component invoker);
        
                
        JComponent getUI() {
            if (ui == null) {
                ui = new JPanel(null);
                ui.setLayout(new BoxLayout(ui, BoxLayout.LINE_AXIS));
                ui.setOpaque(false);

                selectionContent = new JPanel(null);
                selectionContent.setLayout(new BoxLayout(selectionContent, BoxLayout.LINE_AXIS));
                selectionContent.setOpaque(false);

                editSelectionLink = new JButton() {
                    public void setText(String text) {
                        super.setText(Bundle.MethodsFeatureModes_editLink(text));
                    }
                    protected void fireActionPerformed(ActionEvent e) {
                        performEditSelection(this);
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
                selectionContent.add(outgoingLabel);

                selectionContent.add(Box.createHorizontalStrut(5));

                int outgoingCalls = Integer.parseInt(readFlag(OUTGOING_CALLS_FLAG, OUTGOING_CALLS_DEFAULT.toString()));
                outgoingSpinner = new JExtendedSpinner(new SpinnerNumberModel(outgoingCalls, 1, 10, 1)) {
                    public Dimension getPreferredSize() { return getMinimumSize(); }
                    public Dimension getMaximumSize() { return getMinimumSize(); }
                    protected void fireStateChanged() { settingsChanged(); super.fireStateChanged(); }
                };
                selectionContent.add(outgoingSpinner);

                noSelectionContent = new JPanel();
                noSelectionContent.setLayout(new BoxLayout(noSelectionContent, BoxLayout.LINE_AXIS));
                noSelectionContent.setOpaque(false);

                GrayLabel noSelectionHint = new GrayLabel(noSelectionString());
                noSelectionHint.setEnabled(false);
                noSelectionContent.add(noSelectionHint);

                noSelectionContent.add(Box.createHorizontalStrut(5));

                addSelectionButton = new SmallButton("+") {
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
        
        
        protected String noSelectionString() {
            return Bundle.MethodsFeatureModes_noClassSelected();
        }
        
        protected String oneSelectionString() {
            return Bundle.MethodsFeatureModes_oneClassSelected();
        }
        
        protected String multipleSelectionsString(int count) {
            return Bundle.MethodsFeatureModes_multipleClassesSelected(count);
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
        
        
        protected String noSelectionString() {
            return Bundle.MethodsFeatureModes_noMethodSelected();
        }
        
        protected String oneSelectionString() {
            return Bundle.MethodsFeatureModes_oneMethodSelected();
        }
        
        protected String multipleSelectionsString(int count) {
            return Bundle.MethodsFeatureModes_multipleMethodsSelected(count);
        }
        
        
        protected void performAddSelection() {
            SourceMethodInfo methodInfo = ClassMethodSelector.selectMethod(getSession());
            if (methodInfo != null) selectForProfiling(methodInfo);
        }
        
        protected void performEditSelection(Component invoker) {
            ClassMethodList.showMethods(getSession(), getSelection(), invoker);
        }
        
    }
    
}
