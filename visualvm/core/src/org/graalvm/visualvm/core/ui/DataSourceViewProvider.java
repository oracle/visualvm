/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import java.util.HashMap;
import java.util.Map;

/**
 * Provider of DataSourceView for a DataSource.
 * 
 * @author Jiri Sedlacek
 */
public abstract class DataSourceViewProvider<X extends DataSource> {
    
    private final Map<X, DataSourceView> viewsCache = new HashMap();
    

    /**
     * Returns true if the view provider provides a view for the DataSource.
     * 
     * @param dataSource DataSource for which to provide the view.
     * @return true if the view provider provides a view for the DataSource, false otherwise.
     */
    protected abstract boolean supportsViewFor(X dataSource);

    /**
     * Returns DataSourceView instance for the DataSource.
     * 
     * @param dataSource DataSource for which to create the view.
     * @return DataSourceView instance for the DataSource.
     */
    protected abstract DataSourceView createView(X dataSource);
    
    /**
     * Returns true if the view provider supports saving DataSourceView for the DataSource into
     * the Snapshot type.
     * 
     * @param dataSource DataSource for which to save the view.
     * @param snapshotClass snapshot type into which to save the view.
     * @return true if the view provider supports saving DataSourceView for the DataSource, false otherwise.
     */
    protected boolean supportsSaveViewFor(X dataSource, Class<? extends Snapshot> snapshotClass) { return false; };
    
    /**
     * Saves DataSourceView for the DataSource into the Snapshot.
     * 
     * @param dataSource DataSource for which to save the view.
     * @param snapshot Snapshot into which to save the view.
     */
    protected void saveView(X dataSource, Snapshot snapshot) {};
    
    /**
     * Returns DataSourceView for the DataSource if already created (cached).
     * 
     * @param dataSource DataSource of the plugin.
     * @return DataSourceView for the DataSource if already created (cached), null otherwise.
     */
    protected final DataSourceView getCachedView(X dataSource) {
        synchronized(viewsCache) {
            return viewsCache.get(dataSource);
        }
    }
    
    
    /**
     * Returns DataSourceView for the DataSource. Tries to resolve already
     * created view from cache, creates new DataSourceView instance using
     * the createView(DataSource) method if needed.
     * 
     * @param dataSource
     * @return DataSourceView for the DataSource.
     */
    protected final DataSourceView getView(X dataSource) {
        synchronized(viewsCache) {
            DataSourceView view = getCachedView(dataSource);
            if (view == null) {
                view = createView(dataSource);
                if (view == null) throw new NullPointerException("DataSourceViewProvider provides null view: " + this); // NOI18N
                view.setController(this);
                viewsCache.put(dataSource, view);
            }
            return view;
        }
    }
    
    void viewSaveView(X dataSource, Snapshot snapshot) {
        saveView(dataSource, snapshot);
    }
    
    void processCreatedComponent(DataSourceView view, DataViewComponent component) {
    }
        
    void viewWillBeAdded(DataSourceView view) {
    }
    
    void viewAdded(DataSourceView view) {
    }
    
    void viewRemoved(DataSourceView view) {
        viewsCache.remove((X)view.getDataSource());
    }
    
}
