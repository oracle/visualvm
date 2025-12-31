/*
 * Copyright (c) 1997, 2025, Oracle and/or its affiliates. All rights reserved.
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


/**
 * Table model that extends SortableTableModel and allows to hide columns
 *
 * @author  Jiri Sedlacek
 */
public class ExtendedTableModel extends SortableTableModel {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private SortableTableModel realModel;
    private int[] columnsMapping; // mapping virtual columns -> real columns
    private boolean[] columnsVisibility; // visibility flags of real columns
    private int realColumnsCount;
    private int virtualColumnsCount;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public ExtendedTableModel(SortableTableModel realModel) {
        realColumnsCount = realModel.getColumnCount();
        virtualColumnsCount = realColumnsCount;

        this.realModel = realModel;
        columnsMapping = new int[realColumnsCount];

        boolean[] initialColumnsVisibility = new boolean[realColumnsCount];

        for (int i = 0; i < realColumnsCount; i++) {
            initialColumnsVisibility[i] = true;
        }

        setColumnsVisibility(initialColumnsVisibility);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public Class<?> getColumnClass(int col) {
        return realModel.getColumnClass(getRealColumn(col));
    }

    public int getColumnCount() {
        return virtualColumnsCount;
    }

    //---------------------
    // SortableTableModel interface
    public String getColumnName(int col) {
        return realModel.getColumnName(getRealColumn(col));
    }

    public String getColumnToolTipText(int columnIndex) {
        return realModel.getColumnToolTipText(getRealColumn(columnIndex));
    }

    public void setColumnsVisibility(boolean[] columnsVisibility) {
        this.columnsVisibility = columnsVisibility;
        recomputeColumnsMapping();
    }

    public boolean[] getColumnsVisibility() {
        return columnsVisibility;
    }

    public boolean getInitialSorting(int column) {
        return realModel.getInitialSorting(getRealColumn(column));
    }

    public int getRealColumn(int column) {
        return columnsMapping[column];
    }

    public void setRealColumnVisibility(int column, boolean visible) {
        if (visible) {
            showRealColumn(column);
        } else {
            hideRealColumn(column);
        }
    }

    public boolean isRealColumnVisible(int column) {
        return columnsVisibility[column];
    }

    public int getRowCount() {
        return realModel.getRowCount();
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        return realModel.getValueAt(rowIndex, getRealColumn(columnIndex));
    }

    public int getVirtualColumn(int column) {
        for (int i = 0; i < virtualColumnsCount; i++) {
            if (getRealColumn(i) == column) {
                return i;
            }
        }

        return -1;
    }

    public void hideRealColumn(int column) {
        if (isRealColumnVisible(column)) {
            columnsVisibility[column] = false;
            recomputeColumnsMapping();
        }
    }

    public void showRealColumn(int column) {
        if (!isRealColumnVisible(column)) {
            columnsVisibility[column] = true;
            recomputeColumnsMapping();
        }
    }

    public void sortByColumn(int column, boolean order) {
        realModel.sortByColumn(getRealColumn(column), order);
    }

    private void recomputeColumnsMapping() {
        virtualColumnsCount = 0;

        int virtualColumnIndex = 0;

        // set indexes virtual columns -> real columns
        for (int i = 0; i < realColumnsCount; i++) {
            if (columnsVisibility[i]) {
                columnsMapping[virtualColumnIndex] = i;
                virtualColumnsCount++;
                virtualColumnIndex++;
            }
        }

        // clear mappings of unused real columns
        for (int i = virtualColumnIndex; i < realColumnsCount; i++) {
            columnsMapping[i] = -1;
        }
        
        fireTableStructureChanged();
        realModel.fireTableStructureChanged();
    }
}
