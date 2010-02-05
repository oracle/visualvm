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

package com.sun.tools.visualvm.modules.tracer.impl.timeline;

import java.awt.Color;

/**
 * Utility class to access colors predefined for VisualVM.
 *
 * @author Jiri Sedlacek
 */
final class TimelineColorFactory {
    
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
     * Returns number of colors predefined for VisualVM charts.
     * Always contains at least 4 colors.
     *
     * @return number of colors predefined for VisualVM charts
     */
    private static int getPredefinedColorsCount() {
        return PREDEFINED_COLORS.length;
    }

    /**
     * Returns a color predefined for VisualVM charts.
     *
     * @param index index of the predefined color
     * @return color predefined for VisualVM charts
     */
    private static Color getPredefinedColor(int index) {
        return PREDEFINED_COLORS[index];
    }

    public static Color getColor(int index) {
        Color color;

        if (index >= PREDEFINED_COLORS.length) {
            color = getPredefinedColor(index % PREDEFINED_COLORS.length);
            int darkerFactor = index / PREDEFINED_COLORS.length;
            while (darkerFactor-- > 0) color = color.darker();
        } else {
            color = getPredefinedColor(index);
        }

        return color;
    }


    /**
     * Returns number of color pairs predefined for VisualVM charts gradients.
     * Always contains at least 4 color pairs.
     *
     * @return number of color pairs predefined for VisualVM charts gradients
     */
    private static int getPredefinedGradientsCount() {
        return PREDEFINED_GRADIENTS.length;
    }

    /**
     * Returns a color pair predefined for VisualVM charts gradients.
     *
     * @param index index of the predefined color pair
     * @return color pair predefined for VisualVM charts gradients
     */
    private static Color[] getPredefinedGradient(int index) {
        return PREDEFINED_GRADIENTS[index];
    }

    public static Color[] getGradient(int index) {
        Color[] colors = null;

        if (index >= PREDEFINED_GRADIENTS.length) {
            colors = getPredefinedGradient(index % PREDEFINED_GRADIENTS.length);
            int darkerFactor = index / PREDEFINED_GRADIENTS.length;
            while (darkerFactor-- > 0) {
                colors[0] = colors[0].darker();
                colors[1] = colors[1].darker();
            }
        } else {
            colors = getPredefinedGradient(index);
        }

        return colors;

    }

}
