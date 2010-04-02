/*
 * Copyright 2007-2010 Sun Microsystems, Inc.  All Rights Reserved.
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

package com.sun.tools.visualvm.modules.tracer.impl.details;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/**
 *
 * @author Jiri Sedlacek
 */
class DetailsTableCellRenderer implements TableCellRenderer {

    private TableCellRenderer impl;
    private Color color;
    private Color darkerColor;


    DetailsTableCellRenderer(TableCellRenderer impl) {
        this.impl = impl;
    }


    protected Object formatValue(JTable table, Object value, boolean isSelected,
                                 boolean hasFocus, int row, int column) {
        return value;
    }

    protected void updateRenderer(Component c, JTable table, Object value,
                                  boolean isSelected, boolean hasFocus, int row,
                                  int column) {
        if (color == null) {
            color = table.getBackground();
            if (color == null) color = c.getBackground();
            // Neutralize LaF colors with unpredictable behavior (Nimbus)
            color = color == null ? Color.WHITE : new Color(color.getRGB());
            darkerColor = darker(color);
        }
        if (!isSelected) {
            c.setBackground(row % 2 == 0 ? darkerColor : color);
            // Make sure the renderer paints its background (Nimbus)
            if (c instanceof JComponent) ((JComponent)c).setOpaque(true);
        }
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int column) {

        if (impl == null) impl = table.getDefaultRenderer(table.getColumnClass(column));
        
        value = formatValue(table, value, isSelected, hasFocus, row, column);
        Component c = impl.getTableCellRendererComponent(table, value, isSelected,
                                                         hasFocus, row, column);
        updateRenderer(c, table, value, isSelected, hasFocus, row, column);

        return c;
    }


    private static Color darker(Color c) {
        if (c == null) return null;
        // Unify with Nimbus (-13)
        int r = Math.abs(c.getRed() - 13);
        int g = Math.abs(c.getGreen() - 13);
        int b = Math.abs(c.getBlue() - 13);
        int a = c.getAlpha();
        return new Color(r, g, b, a);
    }
}
