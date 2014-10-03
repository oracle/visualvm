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

package org.netbeans.modules.profiler.v2;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.util.Set;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.JExtendedRadioButton;
import org.netbeans.modules.profiler.api.ProfilerDialogs;
import org.netbeans.modules.profiler.api.ProjectUtilities;
import org.netbeans.modules.profiler.v2.ui.ProjectSelector;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "ProfilerSessions_actionNotSupported=Action not supported by the current profiling session.",
    "ProfilerSessions_loadingFeatures=Loading project features...",
    "ProfilerSessions_selectProject=Select the project to profile:",
    "ProfilerSessions_selectFeature=Select Feature",
    "ProfilerSessions_selectHandlingFeature=Select the feature to handle the action:",
    "ProfilerSessions_selectProjectAndFeature=Select Project and Feature"
})
final class ProfilerSessions {
    
    static void configure(final ProfilerSession session, final Lookup context, final String actionName) {
        final ProfilerFeatures _features = session.getFeatures();
        final Set<ProfilerFeature> compatA = ProfilerFeatures.getCompatible(
                                             _features.getAvailable(), context);
        if (compatA.isEmpty()) {
            // TODO: might offer creating a new profiling session if the current is not in progress
            ProfilerDialogs.displayInfo(Bundle.ProfilerSessions_actionNotSupported());
        } else {
            // Resolving selected features in only supported in EDT
            UIUtils.runInEventDispatchThread(new Runnable() {
                public void run() {
                    Set<ProfilerFeature> compatS = ProfilerFeatures.getCompatible(
                                                   _features.getActivated(), context);

                    ProfilerFeature feature;
                    if (compatS.size() == 1) {
                        // Exactly one selected feature handles the action
                        feature = compatS.iterator().next();
                    } else if (!compatS.isEmpty()) {
                        // Multiple selected features handle the action
                        feature = selectFeature(compatS, actionName);
                    } else if (compatA.size() == 1) {
                        // Exactly one available feature handles the action
                        feature = compatA.iterator().next();
                    } else {
                        // Multiple available features handle the action
                        feature = selectFeature(compatA, actionName);
                    }

                    if (feature != null) {
                        _features.activateFeature(feature);
                        feature.configure(context);
                        session.selectFeature(feature);
                        session.requestActive();
                    }
                }
            });
        }
    }
    
    static void createAndConfigure(final Lookup context, final String actionName) {
        Lookup.Provider project = context.lookup(Lookup.Provider.class);
        if (project == null) project = ProjectUtilities.getMainProject();
        final Lookup.Provider _project = project;
        
        UIUtils.runInEventDispatchThread(new Runnable() {
            public void run() {
                UI ui = UI.createAndConfigure(context, _project);

                String caption = actionName == null ? Bundle.ProfilerSessions_selectProjectAndFeature() : actionName;
                DialogDescriptor dd = new DialogDescriptor(ui, caption, true, new Object[]
                                                         { DialogDescriptor.OK_OPTION, DialogDescriptor.CANCEL_OPTION },
                                                           DialogDescriptor.OK_OPTION, DialogDescriptor.BOTTOM_ALIGN,
                                                           null, null);
                Dialog d = DialogDisplayer.getDefault().createDialog(dd);
                d.setVisible(true);

                if (dd.getValue() == DialogDescriptor.OK_OPTION) {
                    final ProfilerSession session = ui.selectedSession();
                    final ProfilerFeature feature = ui.selectedFeature();

                    RequestProcessor.getDefault().post(new Runnable() {
                        public void run() {
                            final ProfilerFeatures features = session.getFeatures();
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    features.activateFeature(feature);
                                    feature.configure(context);
                                    session.selectFeature(feature);
                                    session.requestActive();
                                }
                            });
                        }
                    });
                }
            }
        });
    }
    
    private static ProfilerFeature selectFeature(Set<ProfilerFeature> features, String actionName) {
        UI ui = UI.selectFeature(features);

        String caption = actionName == null ? Bundle.ProfilerSessions_selectFeature() : actionName;
        DialogDescriptor dd = new DialogDescriptor(ui, caption, true, new Object[]
                                                 { DialogDescriptor.OK_OPTION, DialogDescriptor.CANCEL_OPTION },
                                                   DialogDescriptor.OK_OPTION, DialogDescriptor.BOTTOM_ALIGN,
                                                   null, null);
        Dialog d = DialogDisplayer.getDefault().createDialog(dd);
        d.setVisible(true);
        
        return dd.getValue() == DialogDescriptor.OK_OPTION ? ui.selectedFeature() : null;
    }
    
    
    private static class UI extends JPanel {
        
        private ProfilerFeature selectedFeature;
        private ProfilerSession selectedSession;
        
        static UI selectFeature(Set<ProfilerFeature> features) {
            return new UI(features);
        }
        
        static UI createAndConfigure(Lookup context, Lookup.Provider project) {
            return new UI(context, project);
        }
        
        
        ProfilerFeature selectedFeature() {
            return selectedFeature;
        }
        
        ProfilerSession selectedSession() {
            return selectedSession;
        }
        
        
        UI(Set<ProfilerFeature> features) {
            super(new GridBagLayout());
            
            int y = 0;
            GridBagConstraints c;
            
            JLabel l = new JLabel(Bundle.ProfilerSessions_selectHandlingFeature());
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = y++;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(10, 10, 10, 10);
            add(l, c);
            
            ButtonGroup rbg = new ButtonGroup();
            for (final ProfilerFeature f : features) {
                JRadioButton rb = new JExtendedRadioButton(f.getName(), f.getIcon()) {
                    protected void fireItemStateChanged(ItemEvent e) {
                        if (e.getStateChange() == ItemEvent.SELECTED)
                            selectedFeature = f;
                    }
                };
                rbg.add(rb);
                if (rbg.getSelection() == null) rb.setSelected(true);
                c = new GridBagConstraints();
                c.gridx = 0;
                c.gridy = y++;
                c.anchor = GridBagConstraints.WEST;
                c.insets = new Insets(0, 20, 0, 10);
                add(rb, c);
            }
            
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = y++;
            c.weightx = 1;
            c.weighty = 1;
            c.fill = GridBagConstraints.BOTH;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.insets = new Insets(15, 0, 0, 300);
            add(UIUtils.createFillerPanel(), c);
        }
        
        private JPanel contents;
        private void repaintContents() {
            contents.invalidate();
            contents.revalidate();
            contents.repaint();
        }
        
        UI(final Lookup context, final Lookup.Provider project) {
            super(new GridBagLayout());
            
            int y = 0;
            GridBagConstraints c;
            
            JLabel l = new JLabel(Bundle.ProfilerSessions_selectProject());
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = y++;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(10, 10, 10, 10);
            add(l, c);
            
            contents = new JPanel(new GridBagLayout());
            
            // TODO: should also include External Process!
            ProjectSelector.Populator populator = new ProjectSelector.Populator() {
                protected Lookup.Provider initialProject() { return project; }
            };
            ProjectSelector selector = new ProjectSelector(populator) {
                protected void selectionChanged() {
                    refreshFeatures(context, (Lookup.Provider)getSelectedItem());
                }
            };
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = y++;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(0, 20, 10, 10);
            add(selector, c);
            
            JLabel ll = new JLabel(Bundle.ProfilerSessions_selectHandlingFeature());
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = y++;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(10, 10, 10, 10);
            add(ll, c);
            
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = y++;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(0, 0, 0, 0);
            add(contents, c);
            
            JRadioButton ref = new JExtendedRadioButton("XXX"); // NOI18N
            final int refHeight = ref.getPreferredSize().height;
            JPanel filler = new JPanel(null) {
                public Dimension getPreferredSize() {
                    return new Dimension(300, refHeight * 2);
                }
            };
            filler.setOpaque(false);
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = y++;
            c.weightx = 1;
            c.weighty = 1;
            c.fill = GridBagConstraints.BOTH;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.insets = new Insets(15, 0, 0, 0);
            add(filler, c);
            
            refreshFeatures(context, (Lookup.Provider)selector.getSelectedItem());
        }
        
        private void refreshFeatures(final Lookup context, final Lookup.Provider project) {
            contents.removeAll();
            
            JLabel l = new JLabel(Bundle.ProfilerSessions_loadingFeatures(), JLabel.CENTER);
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.weightx = 1;
            c.weighty = 1;
            c.fill = GridBagConstraints.BOTH;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.insets = new Insets(0, 20, 0, 10);
            contents.add(l, c);
            
            repaintContents();
            
            RequestProcessor.getDefault().post(new Runnable() {
                public void run() {
                    
                    Lookup projectContext = Lookups.fixed(project);
                    selectedSession = ProfilerSession.forContext(projectContext);
                    final ProfilerFeatures features = selectedSession.getFeatures();
                    
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            
                            contents.removeAll();
                            
                            int y = 0;
                            GridBagConstraints c;
                            
                            ButtonGroup rbg = new ButtonGroup();
                            for (final ProfilerFeature f : features.getAvailable()) {
                                if (f.supportsConfiguration(context)) {
                                    JRadioButton rb = new JExtendedRadioButton(f.getName(), f.getIcon()) {
                                        protected void fireItemStateChanged(ItemEvent e) {
                                            if (e.getStateChange() == ItemEvent.SELECTED)
                                                selectedFeature = f;
                                        }
                                    };
                                    rbg.add(rb);
                                    if (rbg.getSelection() == null) rb.setSelected(true);
                                    c = new GridBagConstraints();
                                    c.gridx = 0;
                                    c.gridy = y++;
                                    c.anchor = GridBagConstraints.WEST;
                                    c.insets = new Insets(0, 20, 0, 10);
                                    contents.add(rb, c);
                                }
                            }
                            
                            repaintContents();
                            
                        }
                    });
                    
                }
            });
        }
        
    }
    
}
