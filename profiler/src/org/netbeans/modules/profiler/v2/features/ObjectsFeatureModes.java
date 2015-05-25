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
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.lib.profiler.common.filters.SimpleFilter;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.JExtendedSpinner;
import org.netbeans.lib.profiler.ui.swing.GrayLabel;
import org.netbeans.lib.profiler.ui.swing.SmallButton;
import org.netbeans.modules.profiler.api.icons.GeneralIcons;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.LanguageIcons;
import org.netbeans.modules.profiler.api.java.SourceClassInfo;
import org.netbeans.modules.profiler.api.project.ProjectContentsSupport;
import org.netbeans.modules.profiler.v2.ProfilerSession;
import org.netbeans.modules.profiler.v2.impl.ClassMethodList;
import org.netbeans.modules.profiler.v2.impl.ClassMethodSelector;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "ObjectsFeatureModes_allClasses=All classes",
    "ObjectsFeatureModes_projectClasses=Project classes",
    "ObjectsFeatureModes_selectedClasses=Selected classes",
    "ObjectsFeatureModes_editLink=<html><a href='#'>{0}, edit</a></html>",
    "ObjectsFeatureModes_recordLifecycle=Track only live objects",
    "ObjectsFeatureModes_recordAllocations=Record allocations",
    "ObjectsFeatureModes_limitAllocations=Limit allocations depth:",
    "ObjectsFeatureModes_noClassSelected=No classes selected, use Profile Class action in editor or results or click the Add button:",
    "ObjectsFeatureModes_oneClassSelected=Selected 1 class",
    "ObjectsFeatureModes_multipleClassesSelected=Selected {0} classes",
    "ObjectsFeatureModes_addClass=Select class",
    "ObjectsFeatureModes_lblUnlimited=unlimited",
    "ObjectsFeatureModes_lblNoAllocations=(no allocation calls)",
    "ObjectsFeatureModes_profileAllObjectsToolTip=Unselect to profile all created objects (including already released)",
    "ObjectsFeatureModes_collectFullStacksToolTip=Unselect to collect full depth allocations call tree",
    "ObjectsFeatureModes_limitAllocationsDepthToolTip=Limit depth of allocations call tree (select 0 for no allocation calls)"
})
final class ObjectsFeatureModes {
    
    private static abstract class MemoryMode extends FeatureMode {
    }
    
    private static abstract class SampledMemoryMode extends MemoryMode {
        
        void configureSettings(ProfilingSettings settings) {
            settings.setProfilingType(ProfilingSettings.PROFILE_MEMORY_SAMPLING);
        }
        
        void confirmSettings() {}
        
        boolean pendingChanges() { return false; }

        boolean currentSettingsValid() { return true; }
        
        JComponent getUI() { return null; }
        
    }
    
    static abstract class AllClassesMode extends SampledMemoryMode {
        
        String getID() {
            return "AllClassesMode"; // NOI18N
        }

        String getName() {
            return Bundle.ObjectsFeatureModes_allClasses();
        }

        void configureSettings(ProfilingSettings settings) {
            super.configureSettings(settings);
            
            settings.setSelectedInstrumentationFilter(null);
        }
        
    }
    
    static abstract class ProjectClassesMode extends SampledMemoryMode {
        
        // --- External implementation -----------------------------------------
        
        abstract Lookup.Provider getProject();
        
        
        // --- API implementation ----------------------------------------------
        
        String getID() {
            return "ProjectClassesMode"; // NOI18N
        }

        String getName() {
            return Bundle.ObjectsFeatureModes_projectClasses();
        }

        void configureSettings(ProfilingSettings settings) {
            super.configureSettings(settings);
                
            ProjectContentsSupport pcs = ProjectContentsSupport.get(getProject());
            String filter = pcs.getInstrumentationFilter(false);
            SimpleFilter f = new SimpleFilter("", SimpleFilter.SIMPLE_FILTER_INCLUSIVE, filter); // NOI18N
            settings.setSelectedInstrumentationFilter(f);
        }
        
    }
    
    static abstract class SelectedClassesMode extends MemoryMode {
        
        // --- External implementation -----------------------------------------
        
        abstract void selectionChanging();
        
        abstract void selectionChanged();
        
        abstract ProfilerSession getSession();
        
        abstract void selectForProfiling(SourceClassInfo classInfo);
        
        
        // --- API implementation ----------------------------------------------
        
        private static final String LIFECYCLE_FLAG = "LIFECYCLE_FLAG"; // NOI18N
        private static final String ALLOCATIONS_FLAG = "ALLOCATIONS_FLAG"; // NOI18N
        private static final String LIMIT_ALLOCATIONS_FLAG = "LIMIT_ALLOCATIONS_FLAG"; // NOI18N
        private static final String SELECTION_FLAG = "SELECTION_FLAG"; // NOI18N
        
        private static final Integer LIMIT_ALLOCATIONS_DEFAULT = 10;
        
        private Selection selection;
        
        
        String getID() {
            return "SelectedClassesMode"; // NOI18N
        }

        String getName() {
            return Bundle.ObjectsFeatureModes_selectedClasses();
        }

        void configureSettings(ProfilingSettings settings) {
            assert SwingUtilities.isEventDispatchThread();
            
            boolean lifecycle = Boolean.parseBoolean(readFlag(LIFECYCLE_FLAG, Boolean.TRUE.toString()));
            settings.setProfilingType(lifecycle ? ProfilingSettings.PROFILE_MEMORY_LIVENESS :
                                                  ProfilingSettings.PROFILE_MEMORY_ALLOCATIONS);

            boolean alloc = Boolean.parseBoolean(readFlag(ALLOCATIONS_FLAG, Boolean.TRUE.toString()));
            int limit = Integer.parseInt(readFlag(LIMIT_ALLOCATIONS_FLAG, LIMIT_ALLOCATIONS_DEFAULT.toString()));
            settings.setAllocStackTraceLimit(!alloc ? -10 : limit); // TODO: should follow limit from Options
            
            settings.setAllocTrackEvery(1); // TODO: should follow value from Options

            StringBuilder b = new StringBuilder();
            HashSet<ClientUtils.SourceCodeSelection> _sel = getSelection();
            ClientUtils.SourceCodeSelection[] classes = _sel.toArray(
                    new ClientUtils.SourceCodeSelection[_sel.size()]);
            for (int i = 0; i < classes.length; i++) {
                b.append(classes[i].getClassName());
                if (i < classes.length - 1) b.append(", "); // NOI18N
            }

            SimpleFilter ff = new SimpleFilter("", SimpleFilter.SIMPLE_FILTER_INCLUSIVE_EXACT, b.toString()); // NOI18N
            settings.setSelectedInstrumentationFilter(ff);
        }
        
        void confirmSettings() {
            if (ui != null) {
                assert SwingUtilities.isEventDispatchThread();
                
                storeFlag(LIFECYCLE_FLAG,   lifecycleCheckbox.isSelected() ?
                                            null : Boolean.FALSE.toString());
                storeFlag(ALLOCATIONS_FLAG, outgoingCheckbox.isSelected() ?
                                            null : Boolean.FALSE.toString());
                String limit = ((Integer)outgoingSpinner.getValue()).toString();
                boolean deflimit = LIMIT_ALLOCATIONS_DEFAULT.equals(limit);
                storeFlag(LIMIT_ALLOCATIONS_FLAG, deflimit ? null : limit);
                saveSelection();
            }
        }
        
        boolean pendingChanges() {
            if (ui != null) {
                assert SwingUtilities.isEventDispatchThread();
                
                boolean lifecycle = lifecycleCheckbox.isSelected();
                boolean _lifecycle = Boolean.parseBoolean(readFlag(LIFECYCLE_FLAG, Boolean.TRUE.toString()));
                if (lifecycle != _lifecycle) return true;
                
                boolean alloc = outgoingCheckbox.isSelected();
                boolean _alloc = Boolean.parseBoolean(readFlag(ALLOCATIONS_FLAG, Boolean.TRUE.toString()));
                if (alloc != _alloc) return true;
                
                int limit = Integer.parseInt(readFlag(LIMIT_ALLOCATIONS_FLAG, LIMIT_ALLOCATIONS_DEFAULT.toString()));
                int _limit = (Integer)outgoingSpinner.getValue();
                if (limit != _limit) return true;
                
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
        
        private Selection initSelection(final boolean events) {
            Selection sel = new Selection() {
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
        private JCheckBox lifecycleCheckbox;
        private JButton addSelectionButton;
        private JButton editSelectionLink;
        private JCheckBox outgoingCheckbox;
        private JSpinner outgoingSpinner;
        
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
                        super.setText(Bundle.ObjectsFeatureModes_editLink(text));
                    }
                    protected void fireActionPerformed(ActionEvent e) {
                        ClassMethodList.showClasses(getSession(), selection, editSelectionLink);
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

                boolean lifecycle = Boolean.parseBoolean(readFlag(LIFECYCLE_FLAG, Boolean.TRUE.toString()));
                lifecycleCheckbox = new JCheckBox(Bundle.ObjectsFeatureModes_recordLifecycle(), lifecycle) {
                    protected void fireActionPerformed(ActionEvent e) { super.fireActionPerformed(e); settingsChanged(); }
                };
                lifecycleCheckbox.setToolTipText(Bundle.ObjectsFeatureModes_profileAllObjectsToolTip());
                lifecycleCheckbox.setOpaque(false);
                selectionContent.add(lifecycleCheckbox);
                
                selectionContent.add(Box.createHorizontalStrut(3));
                if (UIUtils.isOracleLookAndFeel()) selectionContent.add(Box.createHorizontalStrut(4));
                
                final JLabel unlimited = new GrayLabel(Bundle.ObjectsFeatureModes_lblUnlimited());
                final JLabel noAllocs = new GrayLabel(Bundle.ObjectsFeatureModes_lblNoAllocations());
                
                boolean alloc = Boolean.parseBoolean(readFlag(ALLOCATIONS_FLAG, Boolean.TRUE.toString()));
                outgoingCheckbox = new JCheckBox(Bundle.ObjectsFeatureModes_limitAllocations(), alloc) {
                    protected void fireActionPerformed(ActionEvent e) {
                        super.fireActionPerformed(e);
                        boolean selected = isSelected();
                        unlimited.setVisible(!selected);
                        outgoingSpinner.setVisible(selected);
                        noAllocs.setVisible(selected && (Integer)outgoingSpinner.getValue() == 0);
                        settingsChanged();
                    }
                };
                outgoingCheckbox.setToolTipText(Bundle.ObjectsFeatureModes_collectFullStacksToolTip());
                outgoingCheckbox.setOpaque(false);
                selectionContent.add(outgoingCheckbox);
                
                unlimited.setVisible(!outgoingCheckbox.isSelected());
                selectionContent.add(unlimited);
                
                selectionContent.add(Box.createHorizontalStrut(1));
                if (UIUtils.isOracleLookAndFeel()) selectionContent.add(Box.createHorizontalStrut(4));
                
                int limit = Integer.parseInt(readFlag(LIMIT_ALLOCATIONS_FLAG, LIMIT_ALLOCATIONS_DEFAULT.toString()));
                outgoingSpinner = new JExtendedSpinner(new SpinnerNumberModel(Math.abs(limit), 0, 99, 1)) {
                    public Dimension getPreferredSize() { return getMinimumSize(); }
                    public Dimension getMaximumSize() { return getMinimumSize(); }
                    protected void fireStateChanged() { settingsChanged(); super.fireStateChanged(); }
                };
                outgoingSpinner.setToolTipText(Bundle.ObjectsFeatureModes_limitAllocationsDepthToolTip());
                JComponent editor = outgoingSpinner.getEditor();
                JTextField field = editor instanceof JSpinner.DefaultEditor ?
                        ((JSpinner.DefaultEditor)editor).getTextField() : null;
                if (field != null) field.getDocument().addDocumentListener(new DocumentListener() {
                    public void insertUpdate(DocumentEvent e) { change(); }
                    public void removeUpdate(DocumentEvent e) { change(); }
                    public void changedUpdate(DocumentEvent e) { change(); }
                    private void change() {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                noAllocs.setVisible(outgoingSpinner.isVisible() &&
                                                    (Integer)outgoingSpinner.getValue() == 0);
                            }
                        });
//                            settingsChanged();
                    }
                });
                outgoingSpinner.setVisible(outgoingCheckbox.isSelected());
                selectionContent.add(outgoingSpinner);
                
                selectionContent.add(Box.createHorizontalStrut(5));
                
                noAllocs.setVisible(outgoingSpinner.isVisible() && (Integer)outgoingSpinner.getValue() == 0);
                selectionContent.add(noAllocs);

                noSelectionContent = new JPanel();
                noSelectionContent.setLayout(new BoxLayout(noSelectionContent, BoxLayout.LINE_AXIS));
                noSelectionContent.setOpaque(false);

                GrayLabel noSelectionHint = new GrayLabel(Bundle.ObjectsFeatureModes_noClassSelected());
                noSelectionHint.setEnabled(false);
                noSelectionContent.add(noSelectionHint);

                noSelectionContent.add(Box.createHorizontalStrut(5));

                String iconMask = LanguageIcons.CLASS;
                Image baseIcon = Icons.getImage(iconMask);
                Image addBadge = Icons.getImage(GeneralIcons.BADGE_ADD);
                Image addImage = ImageUtilities.mergeImages(baseIcon, addBadge, 0, 0);
                addSelectionButton = new SmallButton(ImageUtilities.image2Icon(addImage)) {
                    protected void fireActionPerformed(ActionEvent e) {
                        SourceClassInfo classInfo = ClassMethodSelector.selectClass(getSession());
                        if (classInfo != null) selectForProfiling(classInfo);
                    }
                    public Dimension getMinimumSize() {
                        return getPreferredSize();
                    }
                    public Dimension getMaximumSize() {
                        return getPreferredSize();
                    }
                };
                addSelectionButton.setToolTipText(Bundle.ObjectsFeatureModes_addClass());
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
                
                if (count > 0) editSelectionLink.setText(count == 1 ? Bundle.ObjectsFeatureModes_oneClassSelected() :
                                                         Bundle.ObjectsFeatureModes_multipleClassesSelected(count));
                
                if (content != null) {
                    ui.removeAll();
                    ui.add(content);
                    ui.doLayout();
                    ui.repaint();
                }
            }
        }
        
    }
    
}
