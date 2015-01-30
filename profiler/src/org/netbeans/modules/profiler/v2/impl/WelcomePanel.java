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

package org.netbeans.modules.profiler.v2.impl;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.net.URL;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import org.netbeans.modules.profiler.v2.ProfilerFeature;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "WelcomePanel_howtoCaption=How To Configure Profiling Session",
    "WelcomePanel_profilingSettings=Access the profiling settings:",
    "WelcomePanel_profileButton=Profile",
    "WelcomePanel_clickForSetings=Click the {0} dropdown arrow in the above toolbar to access the profiling settings.",
    "WelcomePanel_configureTarget=Configure the target:",
    "WelcomePanel_setupProjectAttach=Setup attach to project...",
    "WelcomePanel_setupProcessAttach=Setup attach to process...",
    "WelcomePanel_configureProjectAttach=Select the {0} item to configure the target project for profiling.",
    "WelcomePanel_configureProcessAttach=Select the {0} item to configure the external process for profiling.",
    "WelcomePanel_selectFeatures=Select what features will be used to analyze the application:",
    "#HTML-formatted, names of items from Profile dropdown menu should be in bold",
    "WelcomePanel_multipleFeaturesHint=To profile multiple features simultaneously, select the <b>Profile multiple features</b> choice. Note that profiling <b>Methods</b> and <b>Objects</b> is mutually exclusive."
})
public abstract class WelcomePanel extends JPanel {
    
    public WelcomePanel(boolean project, boolean attach, Set<ProfilerFeature> features) {
        
        Color background = UIUtils.getProfilerResultsBackground();
        
        JPanel pp = new JPanel(new GridBagLayout());
        pp.setOpaque(true);
        pp.setBackground(background);
        
        int y = 0;
        
        HTMLTextArea header = new HTMLTextArea("<font size='+1'>" + Bundle.WelcomePanel_howtoCaption() + "</font>"); // NOI18N
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, header.getForeground()));
        header.setBackground(background);
        if (UIUtils.isNimbus()) header.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = y++;
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(5, 20, 0, 20);
        pp.add(header, c);
        
        HTMLTextArea caption1 = new HTMLTextArea("<b>" + Bundle.WelcomePanel_profilingSettings() + "</b>"); // NOI18N
        caption1.setBackground(background);
        if (UIUtils.isNimbus()) caption1.setOpaque(false);
        c = new GridBagConstraints();
        c.gridy = y++;
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(20, 20, 3, 20);
        pp.add(caption1, c);
        
        HTMLTextArea hint1 = new HTMLTextArea(Bundle.WelcomePanel_clickForSetings("<b><a href='#'>" + Bundle.WelcomePanel_profileButton() + "</a></b>")) { // NOI18N
            protected void showURL(URL url) { highlightItem(null); }
        };
        hint1.setBackground(background);
        if (UIUtils.isNimbus()) hint1.setOpaque(false);
        c = new GridBagConstraints();
        c.gridy = y++;
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 40, 0, 20);
        pp.add(hint1, c);
        
        HTMLTextArea caption2 = null;
        HTMLTextArea hint2 = null;
        
        if (attach) {
            
            caption2 = new HTMLTextArea("<b>" + Bundle.WelcomePanel_configureTarget() + "</b>"); // NOI18N
            caption2.setBackground(background);
            if (UIUtils.isNimbus()) caption2.setOpaque(false);
            c = new GridBagConstraints();
            c.gridy = y++;
            c.weightx = 1;
            c.weighty = 1;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.fill = GridBagConstraints.BOTH;
            c.insets = new Insets(20, 20, 3, 20);
            pp.add(caption2, c);
            
            final String hint2h = project ? Bundle.WelcomePanel_setupProjectAttach() :
                                            Bundle.WelcomePanel_setupProcessAttach();
            final String hint2s = project ? Bundle.WelcomePanel_configureProjectAttach("<b><a href='#'>" + hint2h + "</a></b>"): // NOI18N
                                            Bundle.WelcomePanel_configureProcessAttach("<b><a href='#'>" + hint2h + "</a></b>"); // NOI18N
            hint2 = new HTMLTextArea(hint2s) {
                protected void showURL(URL url) { highlightItem(hint2h); }
            };
            hint2.setBackground(background);
            if (UIUtils.isNimbus()) hint2.setOpaque(false);
            c = new GridBagConstraints();
            c.gridy = y++;
            c.weightx = 1;
            c.weighty = 1;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.fill = GridBagConstraints.BOTH;
            c.insets = new Insets(0, 40, 0, 20);
            pp.add(hint2, c);
            
        }
        
        HTMLTextArea caption3 = new HTMLTextArea("<b>" + Bundle.WelcomePanel_selectFeatures() + "</b>"); // NOI18N
        caption3.setBackground(background);
        if (UIUtils.isNimbus()) caption3.setOpaque(false);
        c = new GridBagConstraints();
        c.gridy = y++;
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(20, 20, 3, 20);
        pp.add(caption3, c);        
        
        for (ProfilerFeature feature : features) {
        
            Link l = new Link(feature.getName(), feature.getIcon());
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = y;
            c.gridwidth = 1;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.NONE;
            c.insets = new Insets(0, 40, 0, 10);
            pp.add(l, c);

            JLabel l2 = new JLabel(feature.getDescription());
            l2.setEnabled(false);
            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = y++;
            c.gridwidth = 1;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(3, 0, 3, 20);
            pp.add(l2, c);
        
        }
        
        HTMLTextArea hint3 = new HTMLTextArea(Bundle.WelcomePanel_multipleFeaturesHint());
        hint3.setBackground(background);
        if (UIUtils.isNimbus()) hint3.setOpaque(false);
        c = new GridBagConstraints();
        c.gridy = y++;
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(3, 20, 5, 20);
        pp.add(hint3, c);
        
        int w = pp.getMinimumSize().width;
        
        header.setSize(w, Integer.MAX_VALUE);
        header.setPreferredSize(new Dimension(w, header.getPreferredSize().height));
        
        caption1.setSize(w, Integer.MAX_VALUE);
        caption1.setPreferredSize(new Dimension(w, caption1.getPreferredSize().height));
        
        if (caption2 != null) {
            caption2.setSize(w, Integer.MAX_VALUE);
            caption2.setPreferredSize(new Dimension(w, caption2.getPreferredSize().height));
        }
        
        caption3.setSize(w, Integer.MAX_VALUE);
        caption3.setPreferredSize(new Dimension(w, caption3.getPreferredSize().height));
        
        hint1.setSize(w, Integer.MAX_VALUE);
        hint1.setPreferredSize(new Dimension(w, hint1.getPreferredSize().height));
        
        if (hint2 != null) {
            hint2.setSize(w, Integer.MAX_VALUE);
            hint2.setPreferredSize(new Dimension(w, hint2.getPreferredSize().height));
        }
        
        hint3.setSize(w, Integer.MAX_VALUE);
        hint3.setPreferredSize(new Dimension(w, hint3.getPreferredSize().height));
        
        setLayout(new GridBagLayout());
        setOpaque(true);
        setBackground(UIUtils.getProfilerResultsBackground());
        GridBagConstraints x = new GridBagConstraints();
        x.gridx = 0;
        x.gridy = 0;
        x.weightx = 1;
        x.weighty = 1;
        x.fill = GridBagConstraints.NONE;
        add(pp, x);
        
    }
    
    public abstract void highlightItem(String text);
    
    
    private class Link extends JButton {
        
        private final String text;
        
        Link(String text, Icon icon) {
            super("<html><nobr><b><a href='#'>" + text + "</a></b></nobr></html>"); // NOI18N
            this.text = text;
            
            setIcon(icon);
            
            setOpaque(false);
            setMargin(new Insets(0, 0, 0, 0));
            setBorder(BorderFactory.createEmptyBorder(3, 4, 3, 4));
            setBorderPainted(false);
            setContentAreaFilled(false);
            
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        
        protected void fireActionPerformed(ActionEvent e) {
            highlightItem(text);
        }
        
    }
    
}
