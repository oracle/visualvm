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

package org.netbeans.modules.profiler.v2.ui;

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
import org.netbeans.lib.profiler.ui.UIUtils;

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
