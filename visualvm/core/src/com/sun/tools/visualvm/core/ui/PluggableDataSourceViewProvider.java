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
import com.sun.tools.visualvm.core.snapshot.Snapshot;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Extensible DataSourceViewProvider with support for customizations via DataSourceViewPluginProvider.
 *
 * @author Jiri Sedlacek
 */
public abstract class PluggableDataSourceViewProvider<X extends DataSource> extends DataSourceViewProvider<X> {
    
    /**
     * Set defining all pluggable locations.
     */
    public static final Set<Integer> ALL_LOCATIONS;
    static {
        ALL_LOCATIONS = new HashSet();
        ALL_LOCATIONS.add(DataViewComponent.TOP_LEFT);
        ALL_LOCATIONS.add(DataViewComponent.TOP_RIGHT);
        ALL_LOCATIONS.add(DataViewComponent.BOTTOM_LEFT);
        ALL_LOCATIONS.add(DataViewComponent.BOTTOM_RIGHT);
    }
    
    private final Set<DataSourceViewPluginProvider<X>> pluginProviders =
            Collections.synchronizedSet(new HashSet());
    private final Map<X, Set<DataSourceViewPluginProvider<X>>> pluginProvidersCache =
            Collections.synchronizedMap(new HashMap());
    
    
    /**
     * Registers new DataSourceViewPluginProvider.
     * 
     * @param pluginProvider DataSourceViewPluginProvider to register.
     */
    public final void registerPluginProvider(DataSourceViewPluginProvider<X> pluginProvider) {
        pluginProviders.add(pluginProvider);
    }
    
    /**
     * Unregisters DataSourceViewPluginProvider.
     * 
     * @param pluginProvider DataSourceViewPluginProvider to unregister.
     */
    public final void unregisterPluginProvider(DataSourceViewPluginProvider<X> pluginProvider) {
        pluginProviders.remove(pluginProvider);
    }
    
    /**
     * Returns Set of all locations of the DataSourceView which can be customized by DataSourceViewPluginProviders.
     * 
     * @param view DataSourceView to be customized.
     * @return Set of all locations of the DataSourceView which can be customized by DataSourceViewPluginProviders.
     */
    public abstract Set<Integer> getPluggableLocations(DataSourceView view);
    
    
    /**
     * Saves the DataSourceView for the DataSource into the Snapshot.
     * 
     * @param dataSource DataSource for which to save the view.
     * @param snapshot Snapshot into which to save the view.
     */
    protected void saveView(X dataSource, Snapshot snapshot) {
    };
    
    
    void viewSaveView(X dataSource, Snapshot snapshot) {
        super.viewSaveView(dataSource, snapshot);
        Set<DataSourceViewPluginProvider<X>> providers = getProviders(dataSource, false);
        for (DataSourceViewPluginProvider<X> provider : providers)
            if (provider.supportsSavePluginFor(dataSource, snapshot.getClass()))
                provider.savePlugin(dataSource, snapshot);
    }
    
    void processCreatedComponent(DataSourceView view, DataViewComponent component) {
        X dataSource = (X)view.getDataSource();
        Set<DataSourceViewPluginProvider<X>> providers = getProviders(dataSource, true);
        for (DataSourceViewPluginProvider<X> provider : providers) {
            Set<Integer> pluggableLocations = getPluggableLocations(view);
            DataSourceViewPlugin plugin = provider.getPlugin(dataSource);
            for (int pluggableLocation : pluggableLocations) {
                DataViewComponent.DetailsView pluginView = plugin.createView(pluggableLocation);
                if (pluginView != null) component.addDetailsView(pluginView, pluggableLocation);
            }
        }
    }
    
    void viewWillBeAdded(DataSourceView view) {
        X dataSource = (X)view.getDataSource();
        Set<DataSourceViewPluginProvider<X>> providers = getProviders(dataSource, true);
        for (DataSourceViewPluginProvider<X> provider : providers)
            provider.getPlugin(dataSource).pluginWillBeAdded();
    }
    
    void viewAdded(DataSourceView view) {
        X dataSource = (X)view.getDataSource();
        Set<DataSourceViewPluginProvider<X>> providers = getProviders(dataSource, true);
        for (DataSourceViewPluginProvider<X> provider : providers)
            provider.getPlugin(dataSource).pluginAdded();
    }
    
    void viewRemoved(DataSourceView view) {
        X dataSource = (X)view.getDataSource();
        Set<DataSourceViewPluginProvider<X>> providers = getProviders(dataSource, true);
        for (DataSourceViewPluginProvider<X> provider : providers)
            provider.getPlugin(dataSource).pluginRemoved();
        pluginProvidersCache.remove(dataSource);
        super.viewRemoved(view);
    }
    
    
    private Set<DataSourceViewPluginProvider<X>> getProviders(X dataSource, boolean cache) {
        Set<DataSourceViewPluginProvider<X>> providers = pluginProvidersCache.get(dataSource);
        if (providers != null) return providers;
        providers = new HashSet(pluginProviders);
        Set<DataSourceViewPluginProvider<X>> compatibleProviders = new HashSet();
        for (DataSourceViewPluginProvider<X> provider : providers)
            if (provider.supportsPluginFor(dataSource))
                compatibleProviders.add(provider);
        if (cache) pluginProvidersCache.put(dataSource, compatibleProviders);
        return compatibleProviders;
    }

}
