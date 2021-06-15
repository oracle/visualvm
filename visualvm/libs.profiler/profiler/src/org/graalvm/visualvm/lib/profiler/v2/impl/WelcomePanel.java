/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.graalvm.visualvm.lib.profiler.v2.impl;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.net.URL;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolTip;
import org.graalvm.visualvm.lib.ui.UIUtils;
import org.graalvm.visualvm.lib.ui.components.HTMLTextArea;
import org.graalvm.visualvm.lib.ui.components.NoCaret;
import org.graalvm.visualvm.lib.profiler.v2.ProfilerFeature;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "WelcomePanel_howtoCaption=Configure and Start Profiling",
    "WelcomePanel_clickForSetings=Click the {0} button in toolbar and select the desired profiling mode:",
    "WelcomePanel_startProfiling=Click the {0} button in toolbar once the session is configured to start profiling.",
    "WelcomePanel_modifyProfiling=Use the {0} <b>dropdown arrow</b> to change profiling settings for the session."
})
public abstract class WelcomePanel extends JPanel {

    public static final String CAPTION = Bundle.WelcomePanel_howtoCaption();

    public WelcomePanel(String configureButton, String profileButton, Set<ProfilerFeature> features) {

        Color background = UIUtils.getProfilerResultsBackground();

        JPanel pp = new JPanel(new GridBagLayout());
        pp.setOpaque(true);
        pp.setBackground(background);

        int y = 0;

        Paragraph header = new Paragraph(null, CAPTION, 3, background);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, header.getForeground()));
        if (UIUtils.isNimbus()) header.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = y++;
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(4, 20, 4, 20);
        pp.add(header, c);
        
        int counter = 0;
        
        Paragraph hint1 = new Paragraph(Bundle.WelcomePanel_clickForSetings("<b><a href='#'>" + configureButton + "</a></b>"), Integer.toString(++counter), 1, background) { // NOI18N
            protected void showURL(URL url) { highlightItem(null); }
        };
        c = new GridBagConstraints();
        c.gridy = y++;
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(4, 28, 0, 20);
        pp.add(hint1, c);
        
        for (ProfilerFeature feature : features) {
        
            JLabel l1 = new JLabel(feature.getName(), feature.getIcon(), JLabel.LEADING);
            l1.setFont(new JToolTip().getFont());
            l1.setIconTextGap(l1.getIconTextGap() + 2);
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = y;
            c.gridwidth = 1;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.NONE;
            c.insets = new Insets(3, 60, 3, 10);
            pp.add(l1, c);

            JLabel l2 = new JLabel(feature.getDescription());
            l2.setFont(l1.getFont());
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
        
        Paragraph hint2 = new Paragraph(Bundle.WelcomePanel_startProfiling("<b>" + profileButton + "</b>"), Integer.toString(++counter), 1, background); // NOI18N
        c = new GridBagConstraints();
        c.gridy = y++;
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(6, 28, 0, 20);
        pp.add(hint2, c);
        
        Paragraph hint3 = new Paragraph(Bundle.WelcomePanel_modifyProfiling(profileButton), Integer.toString(++counter), 1, background); // NOI18N
        c = new GridBagConstraints();
        c.gridy = y++;
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 28, 0, 20);
        pp.add(hint3, c);
        
        int w = pp.getMinimumSize().width;
        
        header.setSize(w, Integer.MAX_VALUE);
        header.setPreferredSize(new Dimension(w, header.getPreferredSize().height));
        
        hint1.setSize(w, Integer.MAX_VALUE);
        hint1.setPreferredSize(new Dimension(w, hint1.getPreferredSize().height));
        
        hint2.setSize(w, Integer.MAX_VALUE);
        hint2.setPreferredSize(new Dimension(w, hint2.getPreferredSize().height));
        
        hint3.setSize(w, Integer.MAX_VALUE);
        hint3.setPreferredSize(new Dimension(w, hint2.getPreferredSize().height));
        
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
    
    
    private static class Paragraph extends HTMLTextArea {
        
        Paragraph(String text, String caption, int captionSizeDiff, Color background) {
            setCaret(new NoCaret());
            setShowPopup(false);
            setBackground(background);
            if (UIUtils.isNimbus()) setOpaque(false);
            
            setFocusable(false);
            
            setFont(new JToolTip().getFont());
            setText(setupText(text, caption, captionSizeDiff));
        }
        
        private String setupText(String text, String caption, int captionSizeDiff) {
            int fsize = getFont().getSize() + captionSizeDiff;
            return caption == null ? text : "<span style='font-size:" + fsize + "px;'>" + caption + // NOI18N
                                            "</span>" + (text == null ? "" : ". " + text); // NOI18N
        }
        
    }
    
}
