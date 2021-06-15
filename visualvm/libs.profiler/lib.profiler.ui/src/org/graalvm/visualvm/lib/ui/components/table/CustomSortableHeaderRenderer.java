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
import javax.swing.*;
import javax.swing.table.TableCellRenderer;


/**
 * This class is used for rendering the JTable header. It also holds information about current sorting column and sorting order.
 * The column header is rendered by the JButton using the appropriate icon.
 * @author Jiri Sedlacek
 */
public class CustomSortableHeaderRenderer implements TableCellRenderer {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private ImageIcon ascIcon;
    private ImageIcon descIcon;
    private boolean sortOrder = SortableTableModel.SORT_ORDER_DESC;

    /**
     * The column which is currently being pressed.
     * The button has to be pressed programatically because the mouse events are not delivered to the JButton from the table header.
     */
    private int pressedColumn = -1;

    /**
     * The column which currently defines the sorting order. Only this column header contains the appropriate icon.
     */
    private int sortingColumn = -1;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Creates a new instance of CustomSortableHeaderRenderer
     * @param asc The icon representing ascending sort order.
     * @param desc The icon representing descending sort order.
     */
    public CustomSortableHeaderRenderer(ImageIcon asc, ImageIcon desc) {
        ascIcon = asc;
        descIcon = desc;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setPressedColumn(int column) {
        pressedColumn = column;
    }

    public int getPressedColumn() {
        return pressedColumn;
    }

    public void setSortingColumn(int column) {
        sortingColumn = column;
    }

    public int getSortingColumn() {
        return sortingColumn;
    }

    public void setSortingOrder(boolean order) {
        sortOrder = order;
    }

    public boolean getSortingOrder() {
        return sortOrder;
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row,
                                                   int column) {
        TableCellRenderer tableCellRenderer = table.getTableHeader().getDefaultRenderer();
        Component c = tableCellRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        if (c instanceof JLabel) {
            JLabel l = (JLabel) c;

            if (column == sortingColumn) { // only for sorting column the icon is displayed
                l.setIcon((sortOrder == SortableTableModel.SORT_ORDER_ASC) ? ascIcon : descIcon);
                l.setFont(l.getFont().deriveFont(Font.BOLD));
            } else {
                l.setIcon(null);
            }

            l.setHorizontalTextPosition(JLabel.LEFT);
        }

        return c;
    }

    public void reverseSortingOrder() {
        sortOrder = !sortOrder;
    }
}
