/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.ui.swing;

import java.awt.Component;
import java.awt.Graphics;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;
import org.graalvm.visualvm.lib.ui.UIUtils;

/**
 * JPopupMenu which supports custom background color.
 *
 * @author Jiri Sedlacek
 */
public class ProfilerPopupMenu extends JPopupMenu {

    private boolean forceBackground;


    public ProfilerPopupMenu() {
        super();
    }

    public ProfilerPopupMenu(String label) {
        super(label);
    }


    // --- Tweaking UI ---------------------------------------------------------

    public JMenuItem add(JMenuItem menuItem) {
        if (forceBackground && !UIUtils.isOracleLookAndFeel()) menuItem.setOpaque(false);
        if (forceBackground && !UIUtils.isNimbusLookAndFeel()) menuItem.setForeground(getForeground());
        return super.add(menuItem);
    }

    public void add(Component comp, Object constraints) {
        if (forceBackground && !UIUtils.isOracleLookAndFeel() && comp instanceof JComponent)
            ((JComponent)comp).setOpaque(false);
        if (forceBackground && !UIUtils.isNimbusLookAndFeel()) comp.setForeground(getForeground());
        comp.setMinimumSize(comp.getPreferredSize());
        super.add(comp, constraints);
    }


    public void setForceBackground(boolean force) {
        if (!UIUtils.isNimbus() || !Boolean.TRUE.equals(UIManager.getBoolean("nb.dark.theme"))) // NOI18N
            this.forceBackground = force;
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (forceBackground) {
            g.setColor(getBackground());
            g.fillRect(1, 1, getWidth() - 2, getHeight() - 2);
        }
    }

}
