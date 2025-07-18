/*
 * Copyright (c) 1997, 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import org.graalvm.visualvm.lib.profiler.api.icons.GeneralIcons;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;


/**
 * This class provides a superclass, from which Table Models can be derived, that will support
 * sorting by a column on which the user clicks. A subclass should call setTable(table),
 * and should provide an implementation of the sortByColumn(int column) method.
 *
 * @author Misha Dmitriev
 * @author Jiri Sedlacek
 */
public abstract class SortableTableModel extends AbstractTableModel {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    /**
     * This class is used for listening to the table header mouse events.
     */
    private class HeaderListener extends MouseAdapter implements MouseMotionListener {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        /*
         * If the user clicks to the sorting column (column defining the sort criterium and order), the sorting order is reversed.
         * If new sorting column is selected, the appropriate sorting order for column's datatype is set.
         */
        public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                int column = tableHeader.columnAtPoint(e.getPoint());
                int sortingColumn = headerRenderer.getSortingColumn();

                if (column == sortingColumn) {
                    headerRenderer.reverseSortingOrder();
                } else {
                    headerRenderer.setSortingColumn(column);

                    if (getInitialSorting(column)) {
                        headerRenderer.setSortingOrder(SORT_ORDER_ASC); // Default sort order for strings is Ascending
                    } else {
                        headerRenderer.setSortingOrder(SORT_ORDER_DESC); // Default sort order for numbers is Descending
                    }
                }

                tableHeader.repaint();

                sortByColumn(column, headerRenderer.getSortingOrder());
            }
        }

        public void mouseDragged(MouseEvent e) {
        }

        public void mouseMoved(MouseEvent e) {
            int focusedColumn = tableHeader.columnAtPoint(e.getPoint());

            if ((focusedColumn != lastFocusedColumn) && (focusedColumn != -1)) {
                tableHeader.setToolTipText(SortableTableModel.this.getColumnToolTipText(focusedColumn));
                lastFocusedColumn = focusedColumn;
            }
        }

        /*
         * Here the active header button is programmatically pressed
         */
        public void mousePressed(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e) && (tableHeader.getResizingColumn() == null)) {
                headerRenderer.setPressedColumn(tableHeader.columnAtPoint(e.getPoint()));
                tableHeader.repaint();
            }
        }

        /*
         * Here the active header button is programmatically released
         */
        public void mouseReleased(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                headerRenderer.setPressedColumn(-1);
                tableHeader.repaint();
            }
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    public static final boolean SORT_ORDER_DESC = false;
    public static final boolean SORT_ORDER_ASC = true;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private CustomSortableHeaderRenderer headerRenderer;
    private HeaderListener headerListener;
    private ImageIcon sortAscIcon = Icons.getImageIcon(GeneralIcons.SORT_ASCENDING);
    private ImageIcon sortDescIcon = Icons.getImageIcon(GeneralIcons.SORT_DESCENDING);
    private JTableHeader tableHeader;
    private int lastFocusedColumn = -1;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public SortableTableModel() {
        headerListener = new HeaderListener();
        headerRenderer = new CustomSortableHeaderRenderer(sortAscIcon, sortDescIcon);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * After the table to which this model belongs has been set, this method allows to set the initial sorting column and sorting order.
     * @param sortingColumn The initial sorting column
     * @param sortingOrder The initial sorting order
     */
    public void setInitialSorting(int sortingColumn, boolean sortingOrder) {
        if (headerRenderer != null) {
            headerRenderer.setSortingColumn(sortingColumn);
            headerRenderer.setSortingOrder(sortingOrder);
        }
    }

    /**
     * @param column The table column index
     * @return Initial sorting for the specified column - if true, ascending, if false descending
     */
    public abstract boolean getInitialSorting(int column); /* {
       return (getColumnClass(column).equals(String.class));
       }*/

    public int getSortingColumn() {
        return headerRenderer.getSortingColumn();
    }

    public boolean getSortingOrder() {
        return headerRenderer.getSortingOrder();
    }

    /**
     * Assigns this SortableTableModel to the JTable and sets the custom renderer for the selectable table header.
     * @param table The JTable to set this table model to
     */
    public void setTable(JTable table) {
        TableColumnModel tableModel = table.getColumnModel();
        int n = tableModel.getColumnCount();

        for (int i = 0; i < n; i++) {
            tableModel.getColumn(i).setHeaderRenderer(headerRenderer);
        }

        if (tableHeader != table.getTableHeader()) {
            if (tableHeader != null) {
                tableHeader.removeMouseListener(headerListener);
                tableHeader.removeMouseMotionListener(headerListener);
                lastFocusedColumn = -1;
            }

            tableHeader = table.getTableHeader();
            tableHeader.setReorderingAllowed(false);
            tableHeader.addMouseListener(headerListener);
            tableHeader.addMouseMotionListener(headerListener);
        }
    }

    public abstract void sortByColumn(int column, boolean order);

    public String getColumnToolTipText(int column) {
        return null;
    }
}
