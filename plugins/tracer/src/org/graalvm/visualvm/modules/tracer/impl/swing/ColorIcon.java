/*
 *  Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 */

package org.graalvm.visualvm.modules.tracer.impl.swing;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Jiri Sedlacek
 */
public final class ColorIcon implements javax.swing.Icon {

    private static int WIDTH = 8;
    private static int HEIGHT = 8;
    private static Color BACKGROUND_COLOR = Color.WHITE;
    private static Color FOREGROUND_COLOR = Color.BLACK;

    public static final ColorIcon BOTTOM_SHADOW = new ColorIcon(null, true);

    private final boolean shadow;
    private final Color color;

    private static final Map<Color, ColorIcon> icons = new HashMap();


    private ColorIcon(Color color) {
        this(color, false);
    }

    private ColorIcon(Color color, boolean shadow) {
        this.color = color;
        this.shadow = shadow;
    }


    public static void setup(int width, int height, Color foreground, Color background) {
        WIDTH = width;
        HEIGHT = height;
        BACKGROUND_COLOR = background;
        FOREGROUND_COLOR = foreground;
        icons.clear();
    }

    public static ColorIcon fromColor(Color color) {
        ColorIcon icon = icons.get(color);
        if (icon == null) {
            icon = new ColorIcon(color);
            icons.put(color, icon);
        }
        return icon;
    }


    public int getIconWidth() {
        return WIDTH;
    }

    public int getIconHeight() {
        return HEIGHT;
    }

    public void paintIcon(java.awt.Component c, java.awt.Graphics g, int x, int y) {
        if (shadow) {
            g.setColor(BACKGROUND_COLOR);
            g.drawLine(x, y + HEIGHT + 1, x + WIDTH - 1, y + HEIGHT + 1);
        } else {
            g.setColor(color);
            g.fillRect(x, y, WIDTH, HEIGHT);
            g.setColor(FOREGROUND_COLOR);
            g.drawRect(x, y, WIDTH - 1, HEIGHT - 1);
        }
    }
}
