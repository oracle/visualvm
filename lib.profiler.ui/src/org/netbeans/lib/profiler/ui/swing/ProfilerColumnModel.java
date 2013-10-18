/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */
package org.netbeans.lib.profiler.ui.swing;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;

/**
 *
 * @author Jiri Sedlacek
 */
class ProfilerColumnModel extends DefaultTableColumnModel {
    
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
    
    
    // --- Column visibility ---------------------------------------------------
    
    private int minColumnWidth = 20;
    private int defaultColumnWidth = 60;
    private Map<Integer, Integer> defaultColumnWidths;
    private Map<Integer, Integer> hiddenColumnWidths = new HashMap();
    
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
        if (defaultColumnWidths == null) defaultColumnWidths = new HashMap();
        defaultColumnWidths.put(column, width);
        TableColumn c = getModelColumn(column);
        if (c != null) c.setWidth(width);
    }
    
    int getDefaultColumnWidth(int column) {
        Integer width = defaultColumnWidths == null ? null :
                        defaultColumnWidths.get(column);
        return width == null ? defaultColumnWidth : width.intValue();
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
        column.setMaxWidth(Integer.MAX_VALUE);
        column.setMinWidth(minColumnWidth);
        Integer width = hiddenColumnWidths.remove(column.getModelIndex());
        column.setWidth(width != null ? width.intValue() :
                        getDefaultColumnWidth(column.getModelIndex()));
        
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
        hiddenColumnWidths.put(column.getModelIndex(), column.getWidth());
        column.setMinWidth(0);
        column.setMaxWidth(0);
        
        int selected = table.getSelectedColumn();
        if (selected != -1 && getColumn(selected).equals(column)) {
            int newSelected = getPreviousVisibleColumn(selected);
            getSelectionModel().setSelectionInterval(newSelected, newSelected);
        }
                
        ProfilerRowSorter sorter = table._getRowSorter();
        int sortColumn = sorter.getSortColumn();
        if (sortColumn == column.getModelIndex()) {
            int newSortColumn = table.convertColumnIndexToView(sortColumn);
            newSortColumn = getPreviousVisibleColumn(newSortColumn);
            sorter.setSortColumn(getColumn(newSortColumn).getModelIndex());
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
    
    int getNextVisibleColumn(int column) {
        do { column = getNextColumn(column); }
        while (!isColumnVisible(getColumn(column)));
        return column;
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
            } catch (NumberFormatException e) {
                continue;
            }
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
