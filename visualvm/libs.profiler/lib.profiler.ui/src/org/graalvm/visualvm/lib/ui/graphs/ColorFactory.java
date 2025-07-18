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

package org.graalvm.visualvm.lib.ui.graphs;

import java.awt.Color;
import org.graalvm.visualvm.lib.ui.UIUtils;

/**
 * Utility class to access colors predefined for VisualVM.
 *
 * @author Jiri Sedlacek
 */
final class ColorFactory {

    private static final Color[] PREDEFINED_COLORS = new Color[] {
                                                new Color(241, 154,  42),
                                                new Color( 32, 171, 217),
                                                new Color(144,  97, 207),
                                                new Color(158, 156,   0)
    };

    private static final Color[][] PREDEFINED_GRADIENTS = !UIUtils.isDarkResultsBackground() ?
    new Color[][] {
        new Color[] { new Color(245, 204, 152), new Color(255, 243, 226) },
        new Color[] { new Color(151, 223, 246), new Color(227, 248, 255) },
        new Color[] { new Color(200, 163, 248), new Color(242, 232, 255) },
        new Color[] { new Color(212, 211, 131), new Color(244, 243, 217) }
    } :
    new Color[][] {
        new Color[] { new Color(145, 104, 52), new Color(155, 143, 126) },
        new Color[] { new Color(51, 123, 146), new Color(127, 148, 155) },
        new Color[] { new Color(100, 63, 148), new Color(142, 132, 155) },
        new Color[] { new Color(112, 111, 31), new Color(144, 143, 117) }
    };


    /**
     * Returns number of colors predefined for VisualVM charts.
     * Always contains at least 4 colors.
     *
     * @return number of colors predefined for VisualVM charts
     */
    public static int getPredefinedColorsCount() {
        return PREDEFINED_COLORS.length;
    }

    /**
     * Returns a color predefined for VisualVM charts.
     *
     * @param index index of the predefined color
     * @return color predefined for VisualVM charts
     */
    public static Color getPredefinedColor(int index) {
        return PREDEFINED_COLORS[index];
    }


    /**
     * Returns number of color pairs predefined for VisualVM charts gradients.
     * Always contains at least 4 color pairs.
     *
     * @return number of color pairs predefined for VisualVM charts gradients
     */
    public static int getPredefinedGradientsCount() {
        return PREDEFINED_GRADIENTS.length;
    }

    /**
     * Returns a color pair predefined for VisualVM charts gradients.
     *
     * @param index index of the predefined color pair
     * @return color pair predefined for VisualVM charts gradients
     */
    public static Color[] getPredefinedGradient(int index) {
        return PREDEFINED_GRADIENTS[index];
    }

}
