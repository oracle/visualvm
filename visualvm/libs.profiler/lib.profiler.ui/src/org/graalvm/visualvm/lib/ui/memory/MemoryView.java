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
package org.graalvm.visualvm.lib.ui.memory;

import java.util.ResourceBundle;
import org.graalvm.visualvm.lib.jfluid.filters.GenericFilter;
import org.graalvm.visualvm.lib.jfluid.results.memory.MemoryResultsSnapshot;
import org.graalvm.visualvm.lib.ui.results.DataView;
import org.graalvm.visualvm.lib.ui.swing.ExportUtils;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTable;
import org.graalvm.visualvm.lib.jfluid.utils.StringUtils;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class MemoryView extends DataView {

    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.graalvm.visualvm.lib.ui.memory.Bundle"); // NOI18N
    static final String EXPORT_TOOLTIP = messages.getString("MemoryView_ExportTooltip"); // NOI18N
    static final String EXPORT_OBJECTS = messages.getString("MemoryView_ExportObjects"); // NOI18N
    static final String EXPORT_LIVE = messages.getString("MemoryView_ExportLive"); // NOI18N
    static final String EXPORT_ALLOCATED = messages.getString("MemoryView_ExportAllocated"); // NOI18N
    static final String EXPORT_ALLOCATED_LIVE = messages.getString("MemoryView_ExportAllocatedLive"); // NOI18N
    static final String COLUMN_NAME = messages.getString("MemoryView_ColumnName"); // NOI18N
    static final String COLUMN_ALLOCATED_BYTES = messages.getString("MemoryView_ColumnAllocatedBytes"); // NOI18N
    static final String COLUMN_ALLOCATED_OBJECTS = messages.getString("MemoryView_ColumnAllocatedObjects"); // NOI18N
    static final String COLUMN_LIVE_BYTES = messages.getString("MemoryView_ColumnLiveBytes"); // NOI18N
    static final String COLUMN_LIVE_OBJECTS = messages.getString("MemoryView_ColumnLiveObjects"); // NOI18N
    static final String COLUMN_TOTAL_ALLOCATED_OBJECTS = messages.getString("MemoryView_ColumnTotalAllocatedObjects"); // NOI18N
    static final String COLUMN_AVG_AGE = messages.getString("MemoryView_ColumnAvgAge"); // NOI18N
    static final String COLUMN_GENERATIONS = messages.getString("MemoryView_ColumnGenerations"); // NOI18N
    static final String COLUMN_SELECTED = messages.getString("MemoryView_ColumnSelected"); // NOI18N
    static final String ACTION_GOTOSOURCE = messages.getString("MemoryView_ActionGoToSource"); // NOI18N
    static final String ACTION_PROFILE_METHOD = messages.getString("MemoryView_ActionProfileMethod"); // NOI18N
    static final String ACTION_PROFILE_CLASS = messages.getString("MemoryView_ActionProfileClass"); // NOI18N
    static final String SELECTED_COLUMN_TOOLTIP = messages.getString("MemoryView_SelectedColumnTooltip"); // NOI18N
    static final String NAME_COLUMN_TOOLTIP = messages.getString("MemoryView_NameColumnTooltip"); // NOI18N
    static final String LIVE_SIZE_COLUMN_TOOLTIP = messages.getString("MemoryView_LiveSizeColumnTooltip"); // NOI18N
    static final String LIVE_COUNT_COLUMN_TOOLTIP = messages.getString("MemoryView_LiveCountColumnTooltip"); // NOI18N
    static final String ALLOC_SIZE_COLUMN_TOOLTIP = messages.getString("MemoryView_AllocSizeColumnTooltip"); // NOI18N
    static final String ALLOC_COUNT_COLUMN_TOOLTIP = messages.getString("MemoryView_AllocCountColumnTooltip"); // NOI18N
    static final String TOTAL_ALLOC_COUNT_COLUMN_TOOLTIP = messages.getString("MemoryView_TotalAllocCountColumnTooltip"); // NOI18N
    static final String AVG_AGE_COLUMN_TOOLTIP = messages.getString("MemoryView_AvgAgeColumnTooltip"); // NOI18N
    static final String GENERATIONS_COLUMN_TOOLTIP = messages.getString("MemoryView_GenerationsColumnTooltip"); // NOI18N
    static final String FILTER_CLASSES_SCOPE = messages.getString("MemoryView_FilterClassesScope"); // NOI18N
    static final String FILTER_ALLOCATIONS_SCOPE = messages.getString("MemoryView_FilterAllocationsScope"); // NOI18N
    static final String FILTER_SCOPE_TOOLTIP = messages.getString("MemoryView_FilterScopeTooltip"); // NOI18N
    static final String SEARCH_CLASSES_SCOPE = messages.getString("MemoryView_SearchClassesScope"); // NOI18N
    static final String SEARCH_ALLOCATIONS_SCOPE = messages.getString("MemoryView_SearchAllocationsScope"); // NOI18N
    static final String SEARCH_SCOPE_TOOLTIP = messages.getString("MemoryView_SearchScopeTooltip"); // NOI18N
    static final String EXPAND_MENU = messages.getString("MemoryView_ExpandMenu"); // NOI18N
    static final String EXPAND_PLAIN_ITEM = messages.getString("MemoryView_ExpandPlainItem"); // NOI18N
    static final String EXPAND_TOPMOST_ITEM = messages.getString("MemoryView_ExpandTopmostItem"); // NOI18N
    static final String COLLAPSE_CHILDREN_ITEM = messages.getString("MemoryView_CollapseChildrenItem"); // NOI18N
    static final String COLLAPSE_ALL_ITEM = messages.getString("MemoryView_CollapseAllItem"); // NOI18N
    // -----_GenerationsCo
    
    
    public abstract void setData(MemoryResultsSnapshot snapshot, GenericFilter filter, int aggregation);
    
    public abstract void resetData();
    
    
    public abstract void showSelectionColumn();
    
    public abstract void refreshSelection();
    
    
    public abstract ExportUtils.ExportProvider[] getExportProviders();
    
    
    protected abstract ProfilerTable getResultsComponent();
    
    
    static final void userFormClassNames(MemoryResultsSnapshot snapshot) {
        // class names in VM format
        String[] classNames = snapshot == null ? null : snapshot.getClassNames();
        if (classNames != null) for (int i = 0; i < classNames.length; i++)
            classNames[i] = StringUtils.userFormClassName(classNames[i]);
    }
    
}
