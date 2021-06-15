/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.profiler.snaptracer.impl.details;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import org.graalvm.visualvm.lib.ui.UIUtils;

/**
 *
 * @author Jiri Sedlacek
 */
class DetailsTableCellRenderer implements TableCellRenderer {

    private static final Color BACKGROUND;
    private static final Color DARKER_BACKGROUND;

    static {
        BACKGROUND = UIUtils.getProfilerResultsBackground();

        int darkerR = BACKGROUND.getRed() - 11;
        if (darkerR < 0) darkerR += 26;
        int darkerG = BACKGROUND.getGreen() - 11;
        if (darkerG < 0) darkerG += 26;
        int darkerB = BACKGROUND.getBlue() - 11;
        if (darkerB < 0) darkerB += 26;
        DARKER_BACKGROUND = new Color(darkerR, darkerG, darkerB);
    }

    private TableCellRenderer impl;


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
        if (!isSelected) {
            c.setBackground(row % 2 == 0 ? DARKER_BACKGROUND : BACKGROUND);
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
    
}
