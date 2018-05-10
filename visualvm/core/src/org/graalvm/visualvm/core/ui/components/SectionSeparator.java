/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.core.ui.components;

import org.graalvm.visualvm.uisupport.SeparatorLine;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Separator-like component to be used instead of TitledBorder to keep the UI
 * lightweight. Use UISupport.createSectionSeparator() method instead of instantiating
 * this class directly if creating sections for the Options panel.
 *
 * @author Jiri Sedlacek
 */
public final class SectionSeparator extends JPanel {

    /**
     * Creates new instance of SectionSeparator. Uses bold font by default.
     *
     * @param text separator text
     */
    public SectionSeparator(String text) {
        this(text, null);
    }

    /**
     * Creates new instance of SectionSeparator. Uses the provided font or default
     * font if no font is provided.
     *
     * @param text separator text
     * @param font font for the caption text or null for default font
     */
    public SectionSeparator(String text, Font font) {
        if (text == null) throw new IllegalArgumentException("Text cannot be null"); // NOI18N
        initComponents(text, font);
    }

    public void setForeground(Color foreground) {
        if (label == null) super.setForeground(foreground);
        else label.setForeground(foreground);
    }

    public Color getForeground() {
        if (label == null) return super.getForeground();
        else return label.getForeground();
    }

    public void setFont(Font font) {
        if (label == null) super.setFont(font);
        else label.setFont(font);
    }

    public Font getFont() {
        if (label == null) return super.getFont();
        else return label.getFont();
    }

    private void initComponents(String text, Font font) {
        setBorder(BorderFactory.createEmptyBorder());
        setLayout(new GridBagLayout());
        setOpaque(false);

        label = new JLabel(text);
        label.setForeground(getForeground());
        if (font != null) label.setFont(font);
        else label.setFont(label.getFont().deriveFont(Font.BOLD));
        GridBagConstraints c1 = new GridBagConstraints();
        c1.weighty = 1d;
        add(label, c1);

        GridBagConstraints c2 = new GridBagConstraints();
        c2.weightx = 1d;
        c2.fill = GridBagConstraints.HORIZONTAL;
        c2.insets = new Insets(1, 4, 0, 0);
        add(new SeparatorLine(), c2);
    }

    private JLabel label;

}
