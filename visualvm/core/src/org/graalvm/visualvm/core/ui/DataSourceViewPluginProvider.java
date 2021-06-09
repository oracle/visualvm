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
import org.graalvm.visualvm.core.snapshot.Snapshot;
import java.util.HashMap;
import java.util.Map;

/**
 * Provider of DataSourceViewPlugin for a DataSource.
 *
 * @author Jiri Sedlacek
 */
public abstract class DataSourceViewPluginProvider<X extends DataSource> {
    
    private final Map<X, DataSourceViewPlugin> pluginsCache = new HashMap();
    
    
    /**
     * Returns true if the plugin provider provides a plugin for the DataSource.
     * 
     * @param dataSource DataSource for which to provide the plugin.
     * @return true if the plugin provider provides a plugin for the DataSource, false otherwise.
     */
    protected abstract boolean supportsPluginFor(X dataSource);

    /**
     * Returns DataSourceViewPlugin instance for the DataSource.
     * 
     * @param dataSource DataSource for which to create the plugin.
     * @return DataSourceViewPlugin instance for the DataSource.
     */
    protected abstract DataSourceViewPlugin createPlugin(X dataSource);
    
    /**
     * Returns true if the plugin provider supports saving DataSourceViewPlugin for the DataSource into
     * the Snapshot type.
     * 
     * @param dataSource DataSource for which to save the plugin.
     * @param snapshotClass snapshot type into which to save the plugin.
     * @return true if the plugin provider supports saving DataSourceViewPlugin for the DataSource, false otherwise.
     */
    protected boolean supportsSavePluginFor(X dataSource, Class<? extends Snapshot> snapshotClass) { return false; };
    
    /**
     * Saves DataSourceViewPlugin for the DataSource into the Snapshot.
     * 
     * @param dataSource DataSource for which to save the plugin.
     * @param snapshot Snapshot into which to save the plugin.
     */
    protected void savePlugin(X dataSource, Snapshot snapshot) {};
    
    
    /**
     * Returns DataSourceViewPlugin for the DataSource if already created (cached).
     * 
     * @param dataSource DataSource of the plugin.
     * @return DataSourceViewPlugin for the DataSource if already created (cached), null otherwise.
     */
    protected final DataSourceViewPlugin getCachedPlugin(X dataSource) {
        synchronized(pluginsCache) {
            return pluginsCache.get(dataSource);
        }
    }
    
    
    void pluginWillBeAdded(DataSourceViewPlugin plugin) {
    }
    
    void pluginAdded(DataSourceViewPlugin plugin) {
    }
    
    void pluginRemoved(DataSourceViewPlugin plugin) {
        synchronized(pluginsCache) {
            pluginsCache.remove((X)plugin.getDataSource());
        }
    }
    
    /**
     * Returns DataSourceViewPlugin for the DataSource. Tries to resolve already
     * created plugin from cache, creates new DataSourceViewPlugin instance using
     * the createPlugin(DataSource) method if needed.
     * 
     * @param dataSource
     * @return DataSourceViewPlugin for the DataSource.
     */
    protected final DataSourceViewPlugin getPlugin(X dataSource) {
        synchronized(pluginsCache) {
            DataSourceViewPlugin plugin = getCachedPlugin(dataSource);
            if (plugin == null) {
                plugin = createPlugin(dataSource);
                if (plugin == null) throw new NullPointerException("DataSourceViewPluginProvider provides null plugin: " + this);   // NOI18N
                plugin.setController(this);
                pluginsCache.put(dataSource, plugin);
            }
            return plugin;
        }
    }

}
