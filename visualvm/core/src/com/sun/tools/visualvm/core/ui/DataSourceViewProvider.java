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
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author Jiri Sedlacek
 */
public abstract class DataSourceViewProvider<X extends DataSource> {
    
    private final Map<X, DataSourceView> viewsCache = new HashMap();
    

    protected abstract boolean supportsViewFor(X dataSource);

    protected abstract DataSourceView createView(X dataSource);
    
    protected boolean supportsSaveViewFor(X dataSource, Class<? extends Snapshot> snapshotClass) { return false; };
    
    protected void saveView(X dataSource, Snapshot snapshot) {};
    
    
    protected final DataSourceView getCachedView(X dataSource) {
        synchronized(viewsCache) {
            return viewsCache.get(dataSource);
        }
    }
    
    
    protected final DataSourceView getView(X dataSource) {
        synchronized(viewsCache) {
            DataSourceView view = getCachedView(dataSource);
            if (view == null) {
                view = createView(dataSource);
                if (view == null) throw new NullPointerException("DataSourceViewProvider provides null view: " + this);
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
