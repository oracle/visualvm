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

package com.sun.tools.visualvm.core.ui;

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.datasupport.Positionable;
import com.sun.tools.visualvm.core.snapshot.Snapshot;
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

    private static DataSourceViewsManager sharedInstance;

    // TODO: implement some better data structure for cheaper providers query
    private final Map<DataSourceViewsProvider, Class<? extends DataSource>> providers = Collections.synchronizedMap(new HashMap());
    
    
    /**
     * Returns singleton instance of DataSourceViewsFactory.
     * 
     * @return singleton instance of DataSourceViewsFactory.
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
    public void addViewProvider(DataSourceViewsProvider provider, Class<? extends DataSource> scope) {
        providers.put(provider, scope);
    }
    
    /**
     * Unregisters DataSourceViewProvider.
     * 
     * @param provider DataSourceViewProvider to be removed.
     */
    public void removeViewProvider(DataSourceViewsProvider provider) {
        providers.remove(provider);
    }
    
    public boolean canSaveViewsFor(DataSource dataSource) {
        Set<DataSourceViewsProvider> compatibleProviders = getCompatibleProviders(dataSource);
        if (compatibleProviders.isEmpty()) return false;
        for (DataSourceViewsProvider compatibleProvider : compatibleProviders)
            if (compatibleProvider.supportsViewsFor(dataSource) && compatibleProvider.supportsSaveViewsFor(dataSource))
                return true;
        return false;
    }
    
    public void saveViewsFor(DataSource dataSource, Snapshot snapshot) {
        Set<DataSourceViewsProvider> compatibleProviders = getCompatibleProviders(dataSource);
        for (DataSourceViewsProvider compatibleProvider : compatibleProviders)
            if (compatibleProvider.supportsViewsFor(dataSource) && compatibleProvider.supportsSaveViewsFor(dataSource))
                compatibleProvider.saveViews(dataSource, snapshot);
    }
    
    boolean hasViewsFor(DataSource dataSource) {
        Set<DataSourceViewsProvider> compatibleProviders = getCompatibleProviders(dataSource);
        if (compatibleProviders.isEmpty()) return false;
        for (DataSourceViewsProvider compatibleProvider : compatibleProviders)
            if (compatibleProvider.supportsViewsFor(dataSource)) return true;
        return false;
    }
    
    List<? extends DataSourceView> getViews(DataSource dataSource) {
        List<DataSourceView> views = new ArrayList();
        Set<DataSourceViewsProvider> compatibleProviders = getCompatibleProviders(dataSource);
        for (DataSourceViewsProvider compatibleProvider : compatibleProviders)
            if (compatibleProvider.supportsViewsFor(dataSource))
                views.addAll(compatibleProvider.getViews(dataSource));
        Collections.sort(views, Positionable.COMPARATOR);
        return views;
    }
    
    private Set<DataSourceViewsProvider> getCompatibleProviders(DataSource dataSource) {
        Set<DataSourceViewsProvider> compatibleProviders = new HashSet();
        Set<DataSourceViewsProvider> providersSet = providers.keySet();
        for (DataSourceViewsProvider provider : providersSet)
            if (providers.get(provider).isInstance(dataSource))
                compatibleProviders.add(provider);
        return compatibleProviders;
    }
    
    
    private DataSourceViewsManager() {
    }

}
