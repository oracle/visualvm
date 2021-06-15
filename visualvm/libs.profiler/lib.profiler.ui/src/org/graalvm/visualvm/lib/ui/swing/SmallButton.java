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

package org.graalvm.visualvm.lib.ui.swing;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JToolBar;
import org.graalvm.visualvm.lib.ui.UIUtils;

/**
 *
 * @author Jiri Sedlacek
 */
public class SmallButton extends JButton {

    protected static final Icon NO_ICON = new Icon() {
        public int getIconWidth() { return 0; }
        public int getIconHeight() { return 16; }
        public void paintIcon(Component c, Graphics g, int x, int y) {}
    };


    {
        setDefaultCapable(false);
        if (UIUtils.isWindowsLookAndFeel()) setOpaque(false);
    }


    public SmallButton() { this(null, null);  }

    public SmallButton(Icon icon) { this(null, icon); }

    public SmallButton(String text) { this(text, null); }

    public SmallButton(Action a) { super(a); }

    public SmallButton(String text, Icon icon) { setText(text); setIcon(icon); }


    public void setIcon(Icon defaultIcon) {
        boolean noIcon = defaultIcon == null;
        if (defaultIcon == null) {
            defaultIcon = NO_ICON;
            setIconTextGap(0);
        }
        super.setIcon(defaultIcon);
        if (!noIcon) putClientProperty("JComponent.sizeVariant", "regular"); // NOI18N
    }

    public Insets getMargin() {
        Insets margin = super.getMargin();
        if (margin != null) {
            if (getParent() instanceof JToolBar) {
                if (UIUtils.isNimbus()) {
                    margin.left = margin.top + 3;
                    margin.right = margin.top + 3;
                }
            } else {
                if (UIUtils.isNimbus()) {
                    margin.left = margin.top - 6;
                    margin.right = margin.top - 6;
                } else {
                    margin.left = margin.top + 3;
                    margin.right = margin.top + 3;
                }
            }
        }
        return margin;
    }
    
}
