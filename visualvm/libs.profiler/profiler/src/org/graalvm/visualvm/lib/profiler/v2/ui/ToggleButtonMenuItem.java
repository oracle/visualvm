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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.UIManager;
import org.graalvm.visualvm.lib.ui.UIUtils;
import org.graalvm.visualvm.lib.ui.swing.StayOpenPopupMenu;

/**
 *
 * @author Jiri Sedlacek
 */
public class ToggleButtonMenuItem extends StayOpenPopupMenu.Item {

    private final JLabel label;
    private final Icon selectedIcon;
    private final Icon unselectedIcon;

    private boolean pressed;


    public ToggleButtonMenuItem(String text, Icon icon) {
        super(sizeText(text));
        setLayout(null);

        selectedIcon = createSelectedIcon(icon);
        unselectedIcon = createUnselectedIcon(icon);

        label = new JLabel(unselectedIcon, JLabel.LEADING);
        add(label, BorderLayout.WEST);
    }


    public void setPressed(boolean pressed) {
        if (this.pressed == pressed) return;

        this.pressed = pressed;
        label.setIcon(pressed ? selectedIcon : unselectedIcon);
        repaint();
    }

    public boolean isPressed() {
        return pressed;
    }


    public Dimension getPreferredSize() {
        Dimension dim = super.getPreferredSize();
        dim.height = Math.max(dim.height, getComponent(0).getPreferredSize().height);
        return dim;
    }

    public void doLayout() {
        getComponent(0).setBounds(0, 0, getWidth(), getHeight());
    }


    private static Icon createSelectedIcon(Icon icon) {
        JComponent c = new JToggleButton() {
            {
                setSelected(true);
                if (UIUtils.isAquaLookAndFeel())
                    putClientProperty("JButton.buttonType", "textured"); // NOI18N
            }
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (UIUtils.isOracleLookAndFeel()) {
                    Color c = UIManager.getColor("List.selectionBackground"); // NOI18N
                    g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 140));
                    g.fillRect(1, 1, getWidth() - 3, getHeight() - 2);
                }
            }
        };

        if (UIUtils.isWindowsLookAndFeel() || UIUtils.isMetalLookAndFeel() || UIUtils.isOracleLookAndFeel()) {
            JToolBar t = new JToolBar() {
                {
                    setLayout(null);
                    setOpaque(false);
                    setRollover(true);
                    setFloatable(false);
                    setBorderPainted(false);
                }
                public void setSize(int w, int h) {
                    super.setSize(w, h);
                    if (getComponentCount() > 0) getComponent(0).setBounds(0, 0, w, h);
                }
            };
            t.removeAll();
            t.add(c);
            c = t;
        }

        return createMenuIcon(icon, c);
    }
    
    private static Icon createUnselectedIcon(Icon icon) {
        return createMenuIcon(icon, null);
    }

    private static Icon createMenuIcon(Icon icon, Component decorator) {
        int h = menuIconSize();
        int w = UIUtils.isAquaLookAndFeel() ? h + 4 : h;

        BufferedImage i = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics g = i.getGraphics();

        if (decorator != null) {
            decorator.setSize(w, h);
            decorator.paint(g);
        }

        icon.paintIcon(null, g, (w - icon.getIconWidth()) / 2, (h - icon.getIconHeight()) / 2);
        g.dispose();

        return new ImageIcon(i);
    }

    private static int menuIconSize() {
        if (UIUtils.isMetalLookAndFeel()) return 23;
        if (UIUtils.isAquaLookAndFeel()) return 26;
        if (UIUtils.isGTKLookAndFeel()) return 24;
        if (UIUtils.isNimbus()) return 25;
        if (UIUtils.isOracleLookAndFeel()) return 21;
        return 22;
    }

    private static String sizeText(String text) {
        if (UIUtils.isMetalLookAndFeel()) return "  " + text; // NOI18N
        if (UIUtils.isAquaLookAndFeel()) return "   " + text; // NOI18N
        if (UIUtils.isGTKLookAndFeel()) return " " + text; // NOI18N
        if (UIUtils.isWindowsClassicLookAndFeel()) return "  " + text; // NOI18N
        return text;
    }

}
