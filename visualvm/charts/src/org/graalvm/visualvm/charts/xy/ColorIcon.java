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

package org.graalvm.visualvm.charts.xy;

import java.awt.*;


/**
 * Copy of org.graalvm.visualvm.lib.ui.components.ColorIcon.
 * 
 * @author Jiri Sedlacek
 */
final class ColorIcon implements javax.swing.Icon {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private Color borderColor = Color.BLACK;
    private Color color = Color.BLACK;
    private int height = 5;
    private int width = 5;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of ColorIcon */
    ColorIcon() {
    }

    ColorIcon(Color color) {
        this();
        setColor(color);
    }

    ColorIcon(Color color, int width, int height) {
        this(color);
        setIconWidth(width);
        setIconHeight(height);
    }

    ColorIcon(Color color, Color borderColor, int width, int height) {
        this(color, width, height);
        setBorderColor(borderColor);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setBorderColor(Color borderColor) {
        this.borderColor = borderColor;
    }

    public Color getBorderColor() {
        return borderColor;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public Color getColor() {
        return color;
    }

    public void setIconHeight(int height) {
        this.height = height;
    }

    public int getIconHeight() {
        return height;
    }

    public void setIconWidth(int width) {
        this.width = width;
    }

    public int getIconWidth() {
        return width;
    }

    public void paintIcon(java.awt.Component c, java.awt.Graphics g, int x, int y) {
        if (color != null) {
            g.setColor(color);
            g.fillRect(x, y, width, height);
        }

        if (borderColor != null) {
            g.setColor(borderColor);
            g.drawRect(x, y, width - 1, height - 1);
        }
    }
}
