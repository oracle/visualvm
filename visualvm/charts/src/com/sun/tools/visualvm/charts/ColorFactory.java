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

package com.sun.tools.visualvm.charts;

import java.awt.Color;
import java.util.Iterator;
import org.netbeans.lib.profiler.charts.swing.Utils;

/**
 * Utility class to access colors predefined for VisualVM.
 *
 * @author Jiri Sedlacek
 */
public final class ColorFactory {
    
    private static final Color[] PREDEFINED_COLORS = new Color[] {
                                                new Color(241, 154,  42),
                                                new Color( 32, 171, 217),
                                                new Color(144,  97, 207),
                                                new Color(158, 156,   0)
    };

    private static final Color[][] PREDEFINED_GRADIENTS = new Color[][] {
        new Color[] { new Color(245, 204, 152), new Color(255, 243, 226) },
        new Color[] { new Color(151, 223, 246), new Color(227, 248, 255) },
        new Color[] { new Color(200, 163, 248), new Color(242, 232, 255) },
        new Color[] { new Color(212, 211, 131), new Color(244, 243, 217) }
    };


    /**
     * If VisualVM is running in simple-graphics mode (remote X server session)
     * returns a fully opaque color, otherwise returns the provided color.
     *
     * @param color Color to be checked
     * @return fully opaque color for simple-graphics mode, original Color otherwise
     */
    public static Color checkedColor(Color color) {
        if (!Utils.forceSpeed()) return color;
        return new Color(color.getRed(), color.getGreen(), color.getBlue());
    }

    /**
     * Returns iterator for colors predefined for VisualVM.
     *
     * @return iterator for colors predefined for VisualVM
     */
    public static Iterator<Color> predefinedColors() {
        return new Iterator<Color>() {
            private int index = 0;
            public boolean hasNext() {
                return index < PREDEFINED_COLORS.length;
            }
            public Color next() {
                return hasNext() ? PREDEFINED_COLORS[index++] : null;
            }
            public void remove() {
                throw new UnsupportedOperationException(
                        "predefinedColors().remove() not supported."); // NOI18N
            }
        };
    }


    /**
     * Returns iterator for gradient colors predefined for VisualVM. The iterator
     * returns a two-items array of Color (Color[2]): the first color in the array
     * represents the start (top) of the gradient, the second color represents the
     * end (bottom) of the gradient.
     *
     * @return iterator for gradient colors predefined for VisualVM
     */
    public static Iterator<Color[]> predefinedGradients() {
        return new Iterator<Color[]>() {
            private int index = 0;
            public boolean hasNext() {
                return index < PREDEFINED_GRADIENTS.length;
            }
            public Color[] next() {
                return hasNext() ? PREDEFINED_GRADIENTS[index++] : null;
            }
            public void remove() {
                throw new UnsupportedOperationException(
                        "predefinedGradients().remove() not supported."); // NOI18N
            }
        };
    }

}
