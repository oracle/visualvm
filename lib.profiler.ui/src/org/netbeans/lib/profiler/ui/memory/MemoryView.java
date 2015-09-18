/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.netbeans.lib.profiler.ui.memory;

import java.util.Collection;
import java.util.ResourceBundle;
import org.netbeans.lib.profiler.results.memory.MemoryResultsSnapshot;
import org.netbeans.lib.profiler.ui.results.DataView;
import org.netbeans.lib.profiler.ui.swing.ExportUtils;
import org.netbeans.lib.profiler.utils.StringUtils;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class MemoryView extends DataView {
    
    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.netbeans.lib.profiler.ui.memory.Bundle"); // NOI18N
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
    // -----
    
    
    public abstract void setData(MemoryResultsSnapshot snapshot, Collection<String> filter, int aggregation);
    
    public abstract void resetData();
    
    
    public abstract void showSelectionColumn();
    
    public abstract void refreshSelection();
    
    
    public abstract ExportUtils.ExportProvider[] getExportProviders();
    
    
    static final void userFormClassNames(MemoryResultsSnapshot snapshot) {
        // class names in VM format
        String[] classNames = snapshot == null ? null : snapshot.getClassNames();
        if (classNames != null) for (int i = 0; i < classNames.length; i++)
            classNames[i] = StringUtils.userFormClassName(classNames[i]);
    }
    
    static final boolean isAll(Collection<String> filter) {
        return filter.contains("*") || filter.contains("**"); // NOI18N
    }
    
    static final boolean isExact(Collection<String> filter) {
        for (String f : filter) if (f.endsWith("*")) return false; // NOI18N
        return true;
    }
    
}
