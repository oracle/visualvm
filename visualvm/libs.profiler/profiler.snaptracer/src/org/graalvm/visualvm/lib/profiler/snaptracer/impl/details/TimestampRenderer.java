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

import java.awt.Component;
import java.text.Format;
import java.text.SimpleDateFormat;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import org.graalvm.visualvm.lib.charts.axis.TimeAxisUtils;

/**
 *
 * @author Jiri Sedlacek
 */
final class TimestampRenderer extends DetailsTableCellRenderer {

    // Fri Mar 19 11:59:59.999 AM 2010
    static final long REFERENCE_TIMESTAMP = 1268996399999l;

    private String formatString;
    private Format format;

    TimestampRenderer(TableCellRenderer renderer) {
        super(renderer);
    }

    protected Object formatValue(JTable table, Object value, boolean isSelected,
                                 boolean hasFocus, int row, int column) {
        String valueString = format.format(value);
        // Improve spacing of the text
        return " " + valueString + " "; // NOI18N
    }

    protected void updateRenderer(Component c, JTable table, Object value,
                                  boolean isSelected, boolean hasFocus, int row,
                                  int column) {
        super.updateRenderer(c, table, value, isSelected, hasFocus, row, column);
        if (c instanceof JLabel) ((JLabel)c).setHorizontalAlignment(JLabel.TRAILING);
    }

    boolean updateFormat(TableModel model) {
        int rowCount = model.getRowCount();

        long first = rowCount > 0 ? (Long)model.getValueAt(0, 1) : REFERENCE_TIMESTAMP;
        long last  = rowCount > 0 ? (Long)model.getValueAt(rowCount - 1, 1) :
                                    REFERENCE_TIMESTAMP + 1;

        String newFormatString = TimeAxisUtils.getFormatString(1, first, last);
        if (!newFormatString.equals(formatString)) {
            formatString = newFormatString;
            format = new SimpleDateFormat(formatString);
            return true;
        } else {
            return false;
        }
    }

}
