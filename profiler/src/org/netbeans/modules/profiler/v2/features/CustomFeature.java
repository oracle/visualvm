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

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.lib.profiler.ui.components.ProfilerToolbar;
import org.netbeans.modules.profiler.v2.session.ProjectSession;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;

/**
 *
 * @author Jiri Sedlacek
 */
class CustomFeature extends ProfilerFeature.Basic {
    
    private JPanel resultsPanel;
    private MultiView resultsView;
    
    private ProfilerToolbar toolbar;
    
    private final ProfilerFeature[] features;
    
    
    static CustomFeature create(ProfilerFeature... features) {
        UI ui = new UI(features);
        DialogDescriptor dd = new DialogDescriptor(ui, "Create Custom Configuration");
        DialogDisplayer.getDefault().createDialog(dd).setVisible(true);
        if (dd.getValue() != DialogDescriptor.OK_OPTION) return null;
        else return new CustomFeature(ui.getText(), ui.getFeatures());
    }
    
    
    CustomFeature(String name, ProfilerFeature... features) {
        super(name, null);
        
        this.features = features;
    }

    
    public JPanel getResultsUI() {
        if (resultsPanel == null) {
            resultsView = new MultiView() {
                protected void setToolbar(ProfilerToolbar toolbar) {
                    ProfilerToolbar tb = getToolbar();
                    
                    for (int i = 0; i < tb.getComponentCount(); i++) tb.remove(0);
                    
                    tb.add(toolbar);
                }
            };
            for (ProfilerFeature feature : features)
                resultsView.addView(feature.getName(), feature.getIcon(), null,
                                    feature.getResultsUI(), feature.getToolbar());
            
            resultsPanel = new JPanel(new BorderLayout());
            resultsPanel.add(resultsView, BorderLayout.CENTER);
        }
        return resultsPanel;
    }
    
    public ProfilerToolbar getToolbar() {
        if (toolbar == null) {
            toolbar = ProfilerToolbar.create(true);
        }
        return toolbar;
    }

    public boolean supportsSettings(ProfilingSettings settings) {
        for (ProfilerFeature feature : features)
            if (!feature.supportsSettings(settings)) return false;
        return true;
    }
    
    public void configureSettings(ProfilingSettings settings) {
        for (ProfilerFeature feature : features)
            feature.configureSettings(settings);
    }
    
    
    public void attachedToSession(ProjectSession session) {
        for (ProfilerFeature feature : features)
            feature.attachedToSession(session);
    }
    
    public void detachedFromSession(ProjectSession session) {
        for (ProfilerFeature feature : features)
            feature.detachedFromSession(session);
    }
    
    
    public void addChangeListener(ChangeListener listener) {
        for (ProfilerFeature feature : features)
            feature.addChangeListener(listener);
    }
    
    public void removeChangeListener(ChangeListener listener) {
        for (ProfilerFeature feature : features)
            feature.removeChangeListener(listener);
    }
    
    
    private static class UI extends JPanel {
        
        private final ProfilerFeature[] features;
        private final JTextField text;
        private final JCheckBox[] boxes;
        
        UI(ProfilerFeature... features) {
            this.features = features;
            
            setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            setLayout(new GridBagLayout());
            
            int row = 0;
            GridBagConstraints c;
            
            JLabel textL = new JLabel("Name:");
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = row;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(0, 0, 15, 8);
            add(textL, c);
            
            text = new JTextField();
            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = row;
            c.weightx = 1;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(0, 0, 15, 0);
            add(text, c);
            
            JLabel boxesL = new JLabel("Features:");
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = ++row;
            c.anchor = GridBagConstraints.WEST;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.insets = new Insets(0, 0, 3, 0);
            add(boxesL, c);
            
            boxes = new JCheckBox[features.length];
            for (int i = 0; i < boxes.length; i++) {
                boxes[i] = new JCheckBox(features[i].getName()) {
                    protected void fireActionPerformed(ActionEvent e) {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() { updateCheckboxes(); }
                        });                        
                    }
                };
                boxes[i].putClientProperty(ProfilerFeature.class, features[i]);
                
                c = new GridBagConstraints();
                c.gridx = 1;
                c.gridy = ++row;
//                c.gridwidth = GridBagConstraints.REMAINDER;
                c.anchor = GridBagConstraints.WEST;
                c.fill = GridBagConstraints.HORIZONTAL;
                c.insets = new Insets(0, -5, 0, 0);
                add(boxes[i], c);
            }
            
            JPanel filler = new JPanel(null);
            filler.setOpaque(false);
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = ++row;
            c.weightx = 1;
            c.weighty = 1;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.BOTH;
            add(filler, c);
            
            updateCheckboxes();
        }
        
        public String getText() {
            return text.getText().trim();
        }
        
        public ProfilerFeature[] getFeatures() {
            List<JCheckBox> selectedB = selectedCheckboxes();
            ProfilerFeature[] selectedF = new ProfilerFeature[selectedB.size()];
            for (int i = 0; i < selectedF.length; i++)
                selectedF[i] = (ProfilerFeature)selectedB.get(i).getClientProperty(ProfilerFeature.class);
            return selectedF;
        }
        
        private boolean updating = false;
        private void updateCheckboxes() {
            if (updating) return;
            updating = true;
            
            List<JCheckBox> selected = selectedCheckboxes();
            if (selected.isEmpty()) {
                boxes[0].setSelected(true);
                selected.add(boxes[0]);
            }
            
            ProfilingSettings settings = new ProfilingSettings();
            for (JCheckBox box : selected) {
                ProfilerFeature feature = (ProfilerFeature)box.getClientProperty(ProfilerFeature.class);
                feature.configureSettings(settings);
            }
            
            for (JCheckBox box : boxes) {
                if (box.isSelected() && selected.size() == 1) {
                    box.setEnabled(false);
                } else {
                    ProfilerFeature feature = (ProfilerFeature)box.getClientProperty(ProfilerFeature.class);
                    box.setEnabled(feature.supportsSettings(settings));
                }
            }
            
            updating = false;
        }
        
        private List<JCheckBox> selectedCheckboxes() {
            List<JCheckBox> selected = new ArrayList();
            for (JCheckBox box : boxes) if (box.isSelected()) selected.add(box);
            return selected;
        }
        
    }
    
}
