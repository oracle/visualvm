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
package org.graalvm.visualvm.lib.ui.cpu;

import java.util.ResourceBundle;
import org.graalvm.visualvm.lib.ui.results.DataView;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class CPUView extends DataView {

    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.graalvm.visualvm.lib.ui.cpu.Bundle"); // NOI18N
    static final String EXPORT_TOOLTIP = messages.getString("CPUView_ExportTooltip"); // NOI18N
    static final String EXPORT_METHODS = messages.getString("CPUView_ExportMethods"); // NOI18N
    static final String EXPORT_FORWARD_CALLS = messages.getString("CPUView_ExportForwardCalls"); // NOI18N
    static final String EXPORT_HOTSPOTS = messages.getString("CPUView_ExportHotSpots"); // NOI18N
    static final String EXPORT_REVERSE_CALLS = messages.getString("CPUView_ExportReverseCalls"); // NOI18N
    static final String COLUMN_NAME = messages.getString("CPUView_ColumnName"); // NOI18N
    static final String COLUMN_SELFTIME = messages.getString("CPUView_ColumnSelfTime"); // NOI18N
    static final String COLUMN_SELFTIME_CPU = messages.getString("CPUView_ColumnSelfTimeCpu"); // NOI18N
    static final String COLUMN_TOTALTIME = messages.getString("CPUView_ColumnTotalTime"); // NOI18N
    static final String COLUMN_TOTALTIME_CPU = messages.getString("CPUView_ColumnTotalTimeCpu"); // NOI18N
    static final String COLUMN_HITS = messages.getString("CPUView_ColumnHits"); // NOI18N
    static final String COLUMN_INVOCATIONS = messages.getString("CPUView_ColumnInvocations"); // NOI18N
    static final String COLUMN_SELECTED = messages.getString("CPUView_ColumnSelected"); // NOI18N
    static final String ACTION_GOTOSOURCE = messages.getString("CPUView_ActionGoToSource"); // NOI18N
    static final String ACTION_PROFILE_METHOD = messages.getString("CPUView_ActionProfileMethod"); // NOI18N
    static final String ACTION_PROFILE_CLASS = messages.getString("CPUView_ActionProfileClass"); // NOI18N
    static final String FIND_IN_FORWARDCALLS = messages.getString("CPUView_FindInForwardCalls"); // NOI18N
    static final String FIND_IN_HOTSPOTS = messages.getString("CPUView_FindInHotSpots"); // NOI18N
    static final String FIND_IN_REVERSECALLS = messages.getString("CPUView_FindInReverseCalls"); // NOI18N
    static final String SELECTED_COLUMN_TOOLTIP = messages.getString("CPUView_SelectedColumnTooltip"); // NOI18N
    static final String NAME_COLUMN_TOOLTIP = messages.getString("CPUView_NameColumnTooltip"); // NOI18N
    static final String SELF_TIME_COLUMN_TOOLTIP = messages.getString("CPUView_SelfTimeColumnTooltip"); // NOI18N
    static final String SELF_TIME_CPU_COLUMN_TOOLTIP = messages.getString("CPUView_SelfTimeCpuColumnTooltip"); // NOI18N
    static final String TOTAL_TIME_COLUMN_TOOLTIP = messages.getString("CPUView_TotalTimeColumnTooltip"); // NOI18N
    static final String TOTAL_TIME_CPU_COLUMN_TOOLTIP = messages.getString("CPUView_TotalTimeCpuColumnTooltip"); // NOI18N
    static final String HITS_COLUMN_TOOLTIP = messages.getString("CPUView_HitsColumnTooltip"); // NOI18N
    static final String INVOCATIONS_COLUMN_TOOLTIP = messages.getString("CPUView_InvocationsColumnTooltip"); // NOI18N
    static final String FILTER_CALLEES_SCOPE = messages.getString("CPUView_FilterCalleesScope"); // NOI18N
    static final String FILTER_CALLERS_SCOPE = messages.getString("CPUView_FilterCallersScope"); // NOI18N
    static final String FILTER_SCOPE_TOOLTIP = messages.getString("CPUView_FilterScopeTooltip"); // NOI18N
    static final String SEARCH_CALLEES_SCOPE = messages.getString("CPUView_SearchCalleesScope"); // NOI18N
    static final String SEARCH_CALLERS_SCOPE = messages.getString("CPUView_SearchCallersScope"); // NOI18N
    static final String SEARCH_SCOPE_TOOLTIP = messages.getString("CPUView_SearchScopeTooltip"); // NOI18N
    static final String HIDE_ZERO_SELF_TIME_TOOLTIP = messages.getString("CPUView_HideZeroSelfTimeTooltip"); // NOI18N
    static final String EXPAND_MENU = messages.getString("CPUView_ExpandMenu"); // NOI18N
    static final String EXPAND_PLAIN_ITEM = messages.getString("CPUView_ExpandPlainItem"); // NOI18N
    static final String EXPAND_TOPMOST_ITEM = messages.getString("CPUView_ExpandTopmostItem"); // NOI18N
    static final String COLLAPSE_CHILDREN_ITEM = messages.getString("CPUView_CollapseChildrenItem"); // NOI18N
    static final String COLLAPSE_ALL_ITEM = messages.getString("CPUView_CollapseAllItem"); // NOI18N
    static final String SHOW_MENU = messages.getString("CPUView_ShowMenu"); // NOI18N
    static final String SHOW_THREAD_ITEM = messages.getString("CPUView_ShowThreadItem"); // NOI18N
    static final String HIDE_THREAD_ITEM = messages.getString("CPUView_HideThreadItem"); // NOI18N
    // -----
    
}
