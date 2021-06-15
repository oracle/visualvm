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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import org.graalvm.visualvm.lib.ui.UIUtils;

/**
 *
 * @author Jiri Sedlacek
 */
public class TitledMenuSeparator extends JPanel {

    public TitledMenuSeparator(String text) {
        setLayout(new BorderLayout());
        setOpaque(false);

        JLabel l = new JLabel(text);
        l.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        if (UIUtils.isWindowsLookAndFeel()) l.setOpaque(true);
        l.setFont(l.getFont().deriveFont(Font.BOLD, l.getFont().getSize2D() - 1));
        if (UIUtils.isWindowsLookAndFeel()) l.setForeground(UIUtils.getDisabledLineColor());

        add(l, BorderLayout.WEST);

        if (UIUtils.isGTKLookAndFeel()) {
            add(UIUtils.createHorizontalSeparator(), BorderLayout.CENTER);
        } else {
            JComponent sep = new JPopupMenu.Separator();
            add(sep, BorderLayout.CENTER);

            if (UIUtils.isOracleLookAndFeel()) {
                setOpaque(true);
                setBackground(sep.getBackground());
                l.setForeground(sep.getForeground());
            }
        }
    }

    public void doLayout() {
        super.doLayout();
        Component c = getComponent(1);

        int h = c.getPreferredSize().height;
        Rectangle b = c.getBounds();

        b.y = (b.height - h) / 2;
        b.height = h;
        c.setBounds(b);
    }

    public Dimension getPreferredSize() {
        Dimension d = getComponent(0).getPreferredSize();
        d.width += 25;
        return d;
    }

}
