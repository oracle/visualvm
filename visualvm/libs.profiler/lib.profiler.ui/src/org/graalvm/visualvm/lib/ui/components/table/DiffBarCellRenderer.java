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

package org.graalvm.visualvm.lib.ui.components.table;

import java.awt.*;


/** Custom Table cell renderer that paints a bar based on numerical value within min/max bounds.
 *
 * @author Jiri Sedlacek
 */
public class DiffBarCellRenderer extends CustomBarCellRenderer {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    public static final Color BAR_FOREGROUND2_COLOR = new Color(41, 195, 41);

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public DiffBarCellRenderer(long min, long max) {
        super(min, max);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void paintComponent(Graphics g) {
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());

        Insets insets = getInsets();
        int clientWidth = getWidth() - insets.right - insets.left;
        int horizCenter = insets.left + (clientWidth / 2);
        int barExtent = (int) Math.ceil((Math.abs(relValue) * ((double) clientWidth)) / 2d);

        if (relValue > 0) {
            g.setColor(BAR_FOREGROUND_COLOR);
            g.fillRect(horizCenter, insets.top, barExtent, getHeight() - insets.bottom - insets.top);
        } else if (relValue < 0) {
            g.setColor(BAR_FOREGROUND2_COLOR);
            g.fillRect(horizCenter - barExtent, insets.top, barExtent, getHeight() - insets.bottom - insets.top);
        }
    }

    protected double calculateViewValue(long n) {
        long absMax = Math.max(Math.abs(min), max);

        return (double) (n) / (double) (absMax);
    }

    protected double calculateViewValue(double n) {
        long absMax = Math.max(Math.abs(min), max);

        return (double) (n) / (double) (absMax);
    }
}
