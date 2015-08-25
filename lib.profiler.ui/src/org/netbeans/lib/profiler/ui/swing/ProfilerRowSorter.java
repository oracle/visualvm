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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

/**
 * TableRowSorter for single-column sorting with default SortOrders for each column. 
 *
 * @author Jiri Sedlacek
 */
class ProfilerRowSorter extends TableRowSorter {
    
    // --- Package-private constructor -----------------------------------------
    
    ProfilerRowSorter(TableModel model) {
        super(model);
    }
    
    // --- Sorting support -----------------------------------------------------
    
    private SortOrder defaultSortOrder = SortOrder.ASCENDING;
    private Map<Integer, SortOrder> defaultSortOrders;
    
    private int secondarySortColumn;
    
    public void setSortKeys(List newKeys) {
        if (newKeys == null) {
            setSortKeysImpl(newKeys);
            return;
        }
        
        RowSorter.SortKey oldKey = getSortKey();
        RowSorter.SortKey newKey = (RowSorter.SortKey)newKeys.get(0);
        
        if (oldKey == null || oldKey.getColumn() != newKey.getColumn()) {
            // Use defined initial SortOrder for a newly sorted column
            setSortColumn(newKey.getColumn());
        } else {
            setSortKeysImpl(newKeys);
        }
    }
    
    protected void setSortKeysImpl(List newKeys) {
        super.setSortKeys(newKeys);
    }
    
    void setSortColumn(int column) {
        setSortColumn(column, getDefaultSortOrder(column));
    }
    
    void setSortColumn(int column, SortOrder order) {
        setSortKey(new RowSorter.SortKey(column, order));
    }
    
    void setSortKey(RowSorter.SortKey key) {
        RowSorter.SortKey secondaryKey = secondarySortColumn == key.getColumn() ?
                          null : new RowSorter.SortKey(secondarySortColumn,
                          getDefaultSortOrder(secondarySortColumn));
        setSortKeysImpl(secondaryKey == null ? Arrays.asList(key) :
                          Arrays.asList(key, secondaryKey));
    }
    
    int getSortColumn() {
        RowSorter.SortKey key = getSortKey();
        return key == null ? -1 : key.getColumn();
    }
    
    SortOrder getSortOrder() {
        RowSorter.SortKey key = getSortKey();
        return key == null ? SortOrder.UNSORTED : key.getSortOrder();
    }
    
    RowSorter.SortKey getSortKey() {
        List<? extends RowSorter.SortKey> keys = getSortKeys();
        return keys.isEmpty() ? null : keys.get(0);
    }
    
    
    void setSecondarySortColumn(int column) {
        secondarySortColumn = column;
    }
    
    int getSecondarySortColumn() {
        return secondarySortColumn;
    }
    
    
    void setDefaultSortOrder(SortOrder sortOrder) {
        defaultSortOrder = sortOrder;
    }
    
    void setDefaultSortOrder(int column, SortOrder sortOrder) {
        if (defaultSortOrders == null) defaultSortOrders = new HashMap();
        defaultSortOrders.put(column, sortOrder);
    }
    
    SortOrder getDefaultSortOrder(int column) {
        SortOrder order = defaultSortOrders == null ? null :
                          defaultSortOrders.get(column);
        return order == null ? defaultSortOrder : order;
    }
    
    
    // --- Filtering support ---------------------------------------------------
    
    private boolean filterMode = true; // AND filter by default
    private Collection<RowFilter<Object, Object>> filters;
    
    // false = OR, true = AND
    void setFiltersMode(boolean mode) {
        this.filterMode = mode;
        if (filters != null) refreshRowFilter();
    }
    
    boolean getFiltersMode() {
        return filterMode;
    }
    
    void addRowFilter(RowFilter filter) {
        if (filters == null) filters = new HashSet();
        if (filters.contains(filter)) filters.remove(filter);
        filters.add(filter);
        refreshRowFilter();
    }
    
    void removeRowFilter(RowFilter filter) {
        if (filters == null) return;
        filters.remove(filter);
        refreshRowFilter();
    }
    
    private void refreshRowFilter() {
        if (filters == null || filters.isEmpty()) {
            setRowFilter(null);
        } else if (filters.size() == 1) {
            setRowFilter(filters.iterator().next());
        } else {
            setRowFilter(filterMode ? RowFilter.andFilter(filters) :
                                      RowFilter.orFilter(filters));
        }
    }
    
    // --- Persistence ---------------------------------------------------------
    
    private static final String SORT_COLUMN_KEY = "ProfilerRowSorter.SortColumn"; // NOI18N
    private static final String SORT_ORDER_KEY = "ProfilerRowSorter.SortOrder"; // NOI18N
    
    void loadFromStorage(Properties properties, ProfilerTable table) {
        String columnS = properties.getProperty(SORT_COLUMN_KEY);
        String orderS = properties.getProperty(SORT_ORDER_KEY);
        if (columnS != null) {
            try {
                int column = Integer.parseInt(columnS);
                SortOrder order = getSortOrder(orderS);
                if (SortOrder.UNSORTED.equals(order)) order = getDefaultSortOrder(column);
                setSortColumn(column, order);
            } catch (NumberFormatException e) {
                // Reset sorting? Set default column?
            }
        } else {
            // Reset sorting? Set default column?
        }
    }
    
    void saveToStorage(Properties properties, ProfilerTable table) {
        RowSorter.SortKey key = getSortKey();
        if (key == null) {
            properties.remove(SORT_COLUMN_KEY);
            properties.remove(SORT_ORDER_KEY);
        } else {
            int column = key.getColumn();
            SortOrder order = key.getSortOrder();
            properties.setProperty(SORT_COLUMN_KEY, Integer.toString(column));
            properties.setProperty(SORT_ORDER_KEY, order.toString());
        }
    }
    
    private SortOrder getSortOrder(String sortOrder) {
        if (SortOrder.ASCENDING.toString().equals(sortOrder)) return SortOrder.ASCENDING;
        else if (SortOrder.DESCENDING.toString().equals(sortOrder)) return SortOrder.DESCENDING;
        else return SortOrder.UNSORTED;
    }
    
}
