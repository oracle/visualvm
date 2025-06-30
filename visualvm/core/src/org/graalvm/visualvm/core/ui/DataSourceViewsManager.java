/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.core.ui;

import org.graalvm.visualvm.core.datasource.DataSource;
import org.graalvm.visualvm.core.datasupport.Positionable;
import org.graalvm.visualvm.core.snapshot.Snapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class responsible for resolving DataSourceViews for DataSources.
 *
 * @author Jiri Sedlacek
 */
public final class DataSourceViewsManager {
    
    private static final String APP_OVERVIEW_CLASS_workaround =
            "org.graalvm.visualvm.application.views.overview.ApplicationOverviewViewProvider"; // NOI18N

    private static DataSourceViewsManager sharedInstance;

    // TODO: implement some better data structure for cheaper providers query
    private final Map<DataSourceViewProvider, Class<? extends DataSource>> providers = Collections.synchronizedMap(new HashMap<>());
    
    
    /**
     * Returns singleton instance of DataSourceViewsManager.
     * 
     * @return singleton instance of DataSourceViewsManager.
     */
    public static synchronized DataSourceViewsManager sharedInstance() {
        if (sharedInstance == null) sharedInstance = new DataSourceViewsManager();
        return sharedInstance;
    }
    
    
    /**
     * Registers new DataSourceViewProvider for given DataSource scope.
     * 
     * @param provider DataSourceViewProvider to be added,
     * @param scope scope of DataSource types for which the provider provides views.
     */
    public void addViewProvider(DataSourceViewProvider provider, Class<? extends DataSource> scope) {
        providers.put(provider, scope);
    }
    
    /**
     * Unregisters DataSourceViewProvider.
     * 
     * @param provider DataSourceViewProvider to be removed.
     */
    public void removeViewProvider(DataSourceViewProvider provider) {
        providers.remove(provider);
    }
    
    /**
     * Returns true if there's at least one DataSourceView for the DataSource which can
     * be saved into the Snapshot type.
     * 
     * @param dataSource DataSource for which to save the views.
     * @param snapshotClass Snapshot type into which to save the views.
     * @return true if there's at least one DataSourceView for the DataSource which can
     * be saved into the Snapshot type, false otherwise.
     */
    public boolean canSaveViewsFor(DataSource dataSource, Class<? extends Snapshot> snapshotClass) {
        Set<DataSourceViewProvider> compatibleProviders = getCompatibleProviders(dataSource);
        if (compatibleProviders.isEmpty()) return false;
        // Workaround for #109 to not block synchronous actions updating in EDT
        for (DataSourceViewProvider compatibleProvider : compatibleProviders)
            if (APP_OVERVIEW_CLASS_workaround.equals(compatibleProvider.getClass().getName())) return true;
        // --------------------------------------------------------------------
        for (DataSourceViewProvider compatibleProvider : compatibleProviders)
            if (compatibleProvider.supportsViewFor(dataSource) && compatibleProvider.supportsSaveViewFor(dataSource, snapshotClass))
                return true;
        return false;
    }
    
    /**
     * Saves views for the DataSource into the Snapshot.
     * 
     * @param dataSource DataSource for which to save the views.
     * @param snapshot Snapshot into which to save the views.
     */
    public void saveViewsFor(DataSource dataSource, Snapshot snapshot) {
        Set<DataSourceViewProvider> compatibleProviders = getCompatibleProviders(dataSource);
        for (DataSourceViewProvider compatibleProvider : compatibleProviders)
            if (compatibleProvider.supportsViewFor(dataSource) && compatibleProvider.supportsSaveViewFor(dataSource, snapshot.getClass()))
                compatibleProvider.viewSaveView(dataSource, snapshot);
    }
    
    boolean hasViewsFor(DataSource dataSource) {
        Set<DataSourceViewProvider> compatibleProviders = getCompatibleProviders(dataSource);
        if (compatibleProviders.isEmpty()) return false;
        // Workaround for #109 to not block synchronous actions updating in EDT
        for (DataSourceViewProvider compatibleProvider : compatibleProviders)
            if (APP_OVERVIEW_CLASS_workaround.equals(compatibleProvider.getClass().getName())) return true;
        // --------------------------------------------------------------------
        for (DataSourceViewProvider compatibleProvider : compatibleProviders)
            if (compatibleProvider.supportsViewFor(dataSource)) return true;
        return false;
    }
    
    List<? extends DataSourceView> getViews(DataSource dataSource) {
        List<DataSourceView> views = new ArrayList<>();
        Set<DataSourceViewProvider> compatibleProviders = getCompatibleProviders(dataSource);
        for (DataSourceViewProvider compatibleProvider : compatibleProviders)
            if (compatibleProvider.supportsViewFor(dataSource))
                views.add(compatibleProvider.getView(dataSource));
        views.sort(Positionable.COMPARATOR);
        return views;
    }
    
    private Set<DataSourceViewProvider> getCompatibleProviders(DataSource dataSource) {
        Set<DataSourceViewProvider> compatibleProviders = new HashSet<>();
        Set<DataSourceViewProvider> providersSet = providers.keySet();
        for (DataSourceViewProvider provider : providersSet)
            if (providers.get(provider).isInstance(dataSource))
                compatibleProviders.add(provider);
        return compatibleProviders;
    }
    
    
    private DataSourceViewsManager() {
    }

}
