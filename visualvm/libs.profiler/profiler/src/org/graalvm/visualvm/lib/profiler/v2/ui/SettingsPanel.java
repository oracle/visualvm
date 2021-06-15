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
package org.graalvm.visualvm.lib.profiler.v2.ui;

import java.awt.Component;
import java.awt.Dimension;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import org.graalvm.visualvm.lib.ui.components.JExtendedSpinner;
import org.graalvm.visualvm.lib.ui.swing.PopupButton;

/**
 *
 * @author Jiri Sedlacek
 */
public class SettingsPanel extends JPanel {

    public SettingsPanel() {
        super(null);

        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
        setOpaque(false);

        add(Box.createVerticalStrut(defaultHeight()));
    }


    public void removeAll() {
        super.removeAll();
        add(Box.createVerticalStrut(defaultHeight()));
    }


    private static int DEFAULT_HEIGHT = -1;

    private static int defaultHeight() {
        if (DEFAULT_HEIGHT == -1) {
            JPanel ref = new JPanel(null);
            ref.setLayout(new BoxLayout(ref, BoxLayout.LINE_AXIS));
            ref.setOpaque(false);

            ref.add(new JLabel("XXX")); // NOI18N

            ref.add(new JButton("XXX")); // NOI18N
            ref.add(new PopupButton("XXX")); // NOI18N

            ref.add(new JCheckBox("XXX")); // NOI18N
            ref.add(new JRadioButton("XXX")); // NOI18N

            ref.add(new JTextField("XXX")); // NOI18N

            ref.add(new JExtendedSpinner(new SpinnerNumberModel(1, 1, 655535, 1)));

            Component separator = Box.createHorizontalStrut(1);
            Dimension d = separator.getMaximumSize(); d.height = 20;
            separator.setMaximumSize(d);
            ref.add(separator);
            
            DEFAULT_HEIGHT = ref.getPreferredSize().height;
        }
        return DEFAULT_HEIGHT;
    }
    
}
