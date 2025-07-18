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
package org.graalvm.visualvm.lib.ui.swing;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.swing.SortOrder;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;

/**
 *
 * @author Jiri Sedlacek
 */
class ProfilerColumnModel extends DefaultTableColumnModel {

    private static final String PROP_COLUMN_WIDTH = "width"; // NOI18N

    // --- Package-private constructor -----------------------------------------

    ProfilerColumnModel() {
        super();
    }

    // --- Column width --------------------------------------------------------

    private int fitWidthColumn = 0;

    void setFitWidthColumn(int column) {
        fitWidthColumn = column;
    }

    int getFitWidthColumn() {
        return fitWidthColumn;
    }

    boolean hasFitWidthColumn() {
        return fitWidthColumn != -1;
    }

    // --- Column resize -------------------------------------------------------

    private int refWidth = -1;

    void setResizingColumn(TableColumn column) {
        refWidth = -1;
    }

    TableColumn createTableColumn(int columnIndex) {
        return new TableColumn(columnIndex) {
            public void setWidth(int width) {
                if (getMaxWidth() == 0 && getWidth() == 0) {
                    TableColumn c = getPreviousVisibleColumn(this);
                    if (refWidth == -1) refWidth = c.getWidth();
                    c.setWidth(refWidth + width);
                } else {
                    super.setWidth(width);
                }
            }                
        };
    }
    
    public void addColumn(final TableColumn column) {
        super.addColumn(column);
        
        final int index = column.getModelIndex();
        column.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if (PROP_COLUMN_WIDTH.equals(evt.getPropertyName())) {
                    int oldWidth = ((Integer)evt.getOldValue()).intValue();
                    int newWidth = ((Integer)evt.getNewValue()).intValue();
                    fireColumnWidthChanged(index, oldWidth, newWidth);
                }
            }
        });
    }
    
    // --- Column offset & width -----------------------------------------------
    
    private Map<Integer, Integer> columnOffsets;
    private Map<Integer, Integer> columnPreferredWidths;
    
    boolean setColumnOffset(int column, int offset) {
        if (columnOffsets == null) columnOffsets = new HashMap<>();
        Integer previousOffset = columnOffsets.put(column, offset);
        int _previousOffset = previousOffset == null ? 0 : previousOffset.intValue();
        boolean change = _previousOffset != offset;
        if (change) fireColumnOffsetChanged(column, _previousOffset, offset);
        return change;
    }
    
    int getColumnOffset(int column) {
        if (columnOffsets == null) return 0;
        Integer offset = columnOffsets.get(column);
        return offset == null ? 0 : offset.intValue();
    }
    
    void clearColumnsPrefferedWidth() {
        if (columnPreferredWidths != null) columnPreferredWidths.clear();
    }
    
    boolean setColumnPreferredWidth(int column, int width) {
        if (columnPreferredWidths == null) columnPreferredWidths = new HashMap<>();
        Integer previousWidth = columnPreferredWidths.put(column, width);
        int _previousWidth = previousWidth == null ? 0 : previousWidth.intValue();
        boolean change = _previousWidth != width;
        if (change) fireColumnPreferredWidthChanged(column, _previousWidth, width);
        return change;
    }
    
    int getColumnPreferredWidth(int column) {
        if (columnPreferredWidths == null) return 0;
        Integer width = columnPreferredWidths.get(column);
        return width == null ? 0 : width.intValue();
    }
    
    // --- Column visibility ---------------------------------------------------
    
    private int minColumnWidth = 20;
    private int defaultColumnWidth = 60;
    private Map<Integer, Integer> defaultColumnWidths;
    private Map<Integer, Integer> hiddenColumnWidths = new HashMap<>();
    
    void setDefaultColumnWidth(int width) {
        defaultColumnWidth = width;
        Enumeration<TableColumn> columns = getColumns();
        while (columns.hasMoreElements()) {
            TableColumn column = columns.nextElement();
            int index = column.getModelIndex();
            if (defaultColumnWidths == null || defaultColumnWidths.get(index) == null)
                if (index != fitWidthColumn) column.setWidth(width);
        }
    }
    
    void setDefaultColumnWidth(int column, int width) {
        if (defaultColumnWidths == null) defaultColumnWidths = new HashMap<>();
        defaultColumnWidths.put(column, width);
        if (isColumnVisible(column)) {
            TableColumn c = getModelColumn(column);
            if (c != null) c.setWidth(width);
        } else {
            hiddenColumnWidths.put(column, width);
        }
    }
    
    int getDefaultColumnWidth(int column) {
        Integer width = defaultColumnWidths == null ? null :
                        defaultColumnWidths.get(column);
        return width == null ? defaultColumnWidth : width.intValue();
    }
    
    void setColumnVisibility(int column, boolean visible, ProfilerTable table) {
        setColumnVisibility(getModelColumn(column), visible, table);
    }
    
    void setColumnVisibility(TableColumn column, boolean visible, ProfilerTable table) {
        if (visible) showColumn(column, table);
        else hideColumn(column, table);
    }
    
    boolean isColumnVisible(int column) {
        return isColumnVisible(getModelColumn(column));
    }
    
    boolean isColumnVisible(TableColumn column) {
        return column.getMaxWidth() > 0;
    }
    
    int getVisibleColumnCount() {
        return getColumnCount() - hiddenColumnWidths.size();
    }
    
    void showColumn(TableColumn column, ProfilerTable table) {
        if (isColumnVisible(column)) return;
        
        column.setMaxWidth(Integer.MAX_VALUE);
        Integer width = hiddenColumnWidths.remove(column.getModelIndex());
        column.setWidth(width != null ? width.intValue() :
                        getDefaultColumnWidth(column.getModelIndex()));
        column.setMinWidth(minColumnWidth);
        
        int toResizeIndex = getFitWidthColumn();
        if (column.getModelIndex() == toResizeIndex) {
            Enumeration<TableColumn> columns = getColumns();
            while (columns.hasMoreElements()) {
                TableColumn col = columns.nextElement();
                int index = col.getModelIndex();
                if (col.getModelIndex() != toResizeIndex && isColumnVisible(col))
                    col.setWidth(getDefaultColumnWidth(index));
            }
            table.doLayout();
        }
    }
    
    void hideColumn(TableColumn column, ProfilerTable table) {
        if (!isColumnVisible(column)) return;
        
        hiddenColumnWidths.put(column.getModelIndex(), column.getWidth());
        column.setMinWidth(0);
        column.setMaxWidth(0);
        
        int selected = table.getSelectedColumn();
        if (selected != -1 && getColumn(selected).equals(column)) {
            int newSelected = getPreviousVisibleColumn(selected);
            getSelectionModel().setSelectionInterval(newSelected, newSelected);
        }
                
        if (table.isSortable()) {
            ProfilerRowSorter sorter = table._getRowSorter();
            int sortColumn = sorter.getSortColumn();
            if (sortColumn == column.getModelIndex()) {
                int newSortColumn = table.convertColumnIndexToView(sortColumn);
                newSortColumn = getPreviousVisibleColumn(newSortColumn);
                int modelIndex = getColumn(newSortColumn).getModelIndex();
                if (!sorter.allowsThreeStateColumns()) sorter.setSortColumn(modelIndex);
                else sorter.setSortColumn(modelIndex, SortOrder.UNSORTED);
            }
        }
    }
    
    int getFirstVisibleColumn() {
        int firstColumn = 0;
        return isColumnVisible(getColumn(firstColumn)) ? firstColumn :
               getNextVisibleColumn(firstColumn);
    }
    
    int getLastVisibleColumn() {
        int lastColumn = getColumnCount() - 1;
        return isColumnVisible(getColumn(lastColumn)) ? lastColumn :
               getPreviousVisibleColumn(lastColumn);
    }
    
    TableColumn getNextVisibleColumn(TableColumn column) {
        int columnIndex = tableColumns.indexOf(column);
        int nextIndex = getNextVisibleColumn(columnIndex);
        return getColumn(nextIndex);
    }
    
    int getNextVisibleColumn(int column) {
        do { column = getNextColumn(column); }
        while (!isColumnVisible(getColumn(column)));
        return column;
    }
    
    TableColumn getPreviousVisibleColumn(TableColumn column) {
        int columnIndex = tableColumns.indexOf(column);
        int previousIndex = getPreviousVisibleColumn(columnIndex);
        return getColumn(previousIndex);
    }
    
    int getPreviousVisibleColumn(int column) {
        do { column = getPreviousColumn(column); }
        while (!isColumnVisible(getColumn(column)));
        return column;
    }
    
    private int getNextColumn(int column) {
        return ++column == getColumnCount() ? 0 : column;
    }
    
    private int getPreviousColumn(int column) {
        return --column == -1 ? getColumnCount() - 1 : column;
    }
    
    private TableColumn getModelColumn(int modelIndex) {
        Enumeration<TableColumn> columns = getColumns();
        while (columns.hasMoreElements()) {
            TableColumn column = columns.nextElement();
            if (column.getModelIndex() == modelIndex) return column;
        }
        return null;
    }
    
    // --- Column tooltip ------------------------------------------------------
    
    private String[] toolTips;
    
    void setColumnToolTips(String[] toolTips) {
        this.toolTips = Arrays.copyOf(toolTips, toolTips.length);
    }
    
    String getColumnToolTip(int column) {
        if (toolTips == null) return null;
        return column < 0 || column >= toolTips.length ? null : toolTips[column];
    }
    
    // --- Listener ------------------------------------------------------------
    
    private Set<Listener> columnListeners;
    
    void addColumnChangeListener(Listener listener) {
        if (columnListeners == null) columnListeners = new HashSet<>();
        columnListeners.add(listener);
    }
    
    void removeColumnChangeListener(Listener listener) {
        if (columnListeners == null) return;
        columnListeners.remove(listener);
        if (columnListeners.isEmpty()) columnListeners = null;
    }
    
    private void fireColumnOffsetChanged(int column, int oldOffset, int newOffset) {
        if (columnListeners == null) return;
        for (Listener listener : columnListeners)
            listener.columnOffsetChanged(column, oldOffset, newOffset);
    }
    
    private void fireColumnWidthChanged(int column, int oldWidth, int newWidth) {
        if (columnListeners == null) return;
        for (Listener listener : columnListeners)
            listener.columnWidthChanged(column, oldWidth, newWidth);
    }
    
    private void fireColumnPreferredWidthChanged(int column, int oldWidth, int newWidth) {
        if (columnListeners == null) return;
        for (Listener listener : columnListeners)
            listener.columnPreferredWidthChanged(column, oldWidth, newWidth);
    }
    
    static interface Listener {
        
        public void columnOffsetChanged(int column, int oldOffset, int newOffset);
        
        public void columnWidthChanged(int column, int oldWidth, int newWidth);
        
        public void columnPreferredWidthChanged(int column, int oldWidth, int newWidth);
        
    }
    
    // --- Persistence ---------------------------------------------------------
    
    private static final String COLUMN_INDEX_KEY = "ProfilerColumnModel.ColumnIndex"; // NOI18N
    private static final String COLUMN_WIDTH_KEY = "ProfilerColumnModel.ColumnWidth"; // NOI18N
    
    void loadFromStorage(Properties properties, ProfilerTable table) {
        for (int i = 0; i < getColumnCount(); i++) {
            String indexS = properties.getProperty(COLUMN_INDEX_KEY + "." + i); // NOI18N
            if (indexS == null) continue;
            try {
                int index = Integer.parseInt(indexS);
                int width = getDefaultColumnWidth(index);
                String widthS = properties.getProperty(COLUMN_WIDTH_KEY + "." + i); // NOI18N
                if (widthS != null) try {
                    width = Integer.parseInt(widthS);
                } catch (NumberFormatException e) {}
                TableColumn column = getModelColumn(index);
                column.setIdentifier(i);
                if (index != getFitWidthColumn()) {
                    if (width == 0) hideColumn(column, table);
                    else column.setWidth(width);
                }
            } catch (NumberFormatException e) {}
        }
        Collections.sort(tableColumns, new Comparator<TableColumn>() {
            public int compare(TableColumn c1, TableColumn c2) {
                Integer index1 = (Integer)c1.getIdentifier();
                Integer index2 = (Integer)c2.getIdentifier();
                return index1.compareTo(index2);
            }
        });
    }
    
    void saveToStorage(Properties properties, ProfilerTable table) {
        for (int i = 0; i < getColumnCount(); i++) {
            TableColumn column = getColumn(i);
            properties.setProperty(COLUMN_INDEX_KEY + "." + i, Integer.toString(column.getModelIndex())); // NOI18N
            properties.setProperty(COLUMN_WIDTH_KEY + "." + i, Integer.toString(column.getWidth())); // NOI18N
        }
    }
    
}
