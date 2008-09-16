/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tools.visualvm.core.ui.actions;

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.datasupport.Utils;
import com.sun.tools.visualvm.core.explorer.ExplorerSupport;
import java.util.Collections;
import java.util.Set;

/**
 * Utils class providing useful methods mostly for Actions implementation.
 *
 * @author Jiri Sedlacek
 */
public final class ActionUtils {
    
    /**
     * Returns selected DataSource is exactly one DataSource is selected in Applications window, null otherwise.
     * @return selected DataSource is exactly one DataSource is selected in Applications window, null otherwise.
     */
    public static DataSource getSelectedDataSource() {
        Set<DataSource> selectedDataSources = getSelectedDataSources();
        return (selectedDataSources.size() == 1 ? selectedDataSources.iterator().next() : null);
    }
    
    /**
     * Returns selected DataSource is exactly one DataSource of a certain type is selected in Applications window, null otherwise.
     * 
     * @param <X> any DataSource.
     * @param scope DataSource type.
     * @return selected DataSource is exactly one DataSource of a certain type is selected in Applications window, null otherwise.
     */
    public static <X extends DataSource> X getSelectedDataSource(Class<X> scope) {
        Set<X> selectedDataSources = getSelectedDataSources(scope);
        return (selectedDataSources.size() == 1 ? selectedDataSources.iterator().next() : null);
    }
    
    /**
     * Returns Set of selected DataSources in Applications window or empty Set for no selection.
     * 
     * @return Set of selected DataSources in Applications window or empty Set for no selection.
     */
    public static Set<DataSource> getSelectedDataSources() {
        Set<DataSource> selectedDataSources = ExplorerSupport.sharedInstance().getSelectedDataSources();
        return selectedDataSources;
    }
    
    /**
     * Returns Set of selected DataSources of a certain type in Applications window or empty Set if no DataSource of this type is selected.
     * 
     * @param <X> any DataSource.
     * @param scope DataSource type.
     * @return Set of selected DataSources of a certain type in Applications window or empty Set if no DataSource of this type is selected.
     */
    public static <X extends DataSource> Set<X> getSelectedDataSources(Class<X> scope) {
        Set<DataSource> selectedDataSources = getSelectedDataSources();
        Set<X> filteredSelectedDataSources = Utils.getFilteredSet(selectedDataSources, scope);
        return selectedDataSources.size() == filteredSelectedDataSources.size() ?
            filteredSelectedDataSources : Collections.EMPTY_SET;
    }

}
