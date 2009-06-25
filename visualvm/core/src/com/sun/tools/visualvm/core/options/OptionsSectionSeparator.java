/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tools.visualvm.core.options;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

/**
 * Predefined component to be used as a separator for named sections displayed
 * in the same Options panel. Should be used rather than a TitledBorder to keep
 * the UI simple.
 *
 * @author Jiri Sedlacek
 */
public final class OptionsSectionSeparator extends JPanel {

    /**
     * Creates new instance of OptionsSectionSeparator.
     *
     * @param text separator caption
     */
    public OptionsSectionSeparator(String text) {
        initComponents(text);
    }

    private void initComponents(String text) {
        setBorder(BorderFactory.createEmptyBorder());
        setLayout(new GridBagLayout());

        GridBagConstraints c1 = new GridBagConstraints();
        c1.weighty = 1d;
        add(new JLabel(text), c1);

        GridBagConstraints c2 = new GridBagConstraints();
        c2.weightx = 1d;
        c2.fill = GridBagConstraints.HORIZONTAL;
        c2.insets = new Insets(0, 4, 0, 0);
        add(new JSeparator() {
                public Dimension getMinimumSize() { return getPreferredSize(); }
            }, c2);
    }

}
