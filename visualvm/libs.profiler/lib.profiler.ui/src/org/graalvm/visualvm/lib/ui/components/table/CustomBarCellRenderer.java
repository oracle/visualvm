/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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
import javax.swing.*;


/** Custom Table cell renderer that paints a bar based on numerical value within min/max bounds.
 *
 * @author Ian Formanek
 * @author Jiri Sedlacek
 */
public class CustomBarCellRenderer extends EnhancedTableCellRenderer {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    public static final Color BAR_FOREGROUND_COLOR = new Color(195, 41, 41);

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected double relValue; // relative part of max - min, <0, 1>
    protected long max;
    protected long min;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public CustomBarCellRenderer(long min, long max) {
        setMinimum(min);
        setMaximum(max);
        setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setMaximum(long n) {
        max = n;
    }

    public void setMinimum(long n) {
        min = n;
    }

    public void setRelValue(double n) {
        relValue = n;
    }

    public Component getTableCellRendererComponentPersistent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                             int row, int column) {
        return null;
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Insets insets = getInsets();
        g.setColor(BAR_FOREGROUND_COLOR);
        g.fillRect(insets.left, insets.top, (int) Math.round(relValue * (getWidth() - insets.right - insets.left)),
                   getHeight() - insets.bottom - insets.top);
    }

    /**
     * Called each time this renderer is to be used to render a specific value on specified row/column.
     * Subclasses need to implement this method to render the value.
     *
     * @param table  the table in which the rendering occurs
     * @param value  the value to be rendered
     * @param row    the row at which the value is located
     * @param column the column at which the value is located
     */
    protected void setValue(JTable table, Object value, int row, int column) {
        if (value instanceof Long) {
            //multiplying by 10 to allow displaying graphs for values < 1
            // - same done for maxi and min values of progress bar, should be ok
            setRelValue(calculateViewValue(((Long) value).longValue()));
        } else if (value instanceof Number) {
            //multiplying by 10 to allow displaying graphs for values < 1
            // - same done for maxi and min values of progress bar, should be ok
            setRelValue(calculateViewValue(((Number) value).doubleValue()));
        } else if (value instanceof String) {
            //multiplying by 10 to allow displaying graphs for values < 1
            // - same done for maxi and min values of progress bar, should be ok
            setRelValue(calculateViewValue(Double.parseDouble((String) value)));
        } else {
            setRelValue(min);
        }
    }

    protected double calculateViewValue(long n) {
        return (double) (n - min) / (double) (max - min);
    }

    protected double calculateViewValue(double n) {
        return (double) (n - min) / (double) (max - min);
    }
}
