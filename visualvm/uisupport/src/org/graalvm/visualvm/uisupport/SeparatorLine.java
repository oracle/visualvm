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
package org.graalvm.visualvm.uisupport;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.UIManager;
import org.graalvm.visualvm.lib.ui.UIUtils;

/**
 * Simple thin JSeparator.
 *
 * @author Jiri Sedlacek
 */
public final class SeparatorLine extends JSeparator {
    
    private final Color separatorColor;


    public SeparatorLine() {
        this(HORIZONTAL);
    }
    
    public SeparatorLine(boolean thin) {
        this(HORIZONTAL, thin);
    }

    public SeparatorLine(int orientation) {
        this(orientation, UISupport.isWindowsLookAndFeel() || UISupport.isAquaLookAndFeel());
    }
    
    public SeparatorLine(int orientation, boolean thin) {
        super(orientation);
        setBorder(BorderFactory.createEmptyBorder());
        separatorColor = thin ? getSeparatorColor() : null;
    }
    
    
    public Dimension getPreferredSize() {
        Dimension dim = super.getPreferredSize();
        if (separatorColor != null) dim.height = 1;
        return dim;
    }

    public Dimension getMinimumSize() {
        return getPreferredSize();
    }
    
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }
    
    
    public void paint(Graphics g) {
        if (separatorColor != null) {
            g.setColor(separatorColor);
            g.drawLine(0, 0, getWidth(), 0);
        } else {
            super.paint(g);
        }
    }
    
    
    private static Color getSeparatorColor() {
        if (UISupport.isWindowsLookAndFeel()) return UIManager.getColor("inactiveCaption"); // NOI18N
        else return UIUtils.getDarkerLine(new JPanel().getBackground(), 0.8f);
    }
    
}
