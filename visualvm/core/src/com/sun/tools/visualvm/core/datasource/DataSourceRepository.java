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

package com.sun.tools.visualvm.core.datasource;

import com.sun.tools.visualvm.core.datasupport.DataChangeEvent;
import com.sun.tools.visualvm.core.datasupport.DataChangeListener;
import java.util.Set;

/**
 * Central repository of all known DataSources.
 * Whenever is a new DataSource added to DataSources tree, DataSourceRepository
 * detects it and emits notifications to listeners.
 *
 * @author Jiri Sedlacek
 */
public final class DataSourceRepository extends DataSourceProvider {

    private static DataSourceRepository sharedInstance;

    private final Listener dataChangeListener = new Listener();

    /**
     * Returns singleton instance of DataSourceRepository.
     * 
     * @return singleton instance of DataSourceRepository.
     */
    public synchronized static DataSourceRepository sharedInstance() {
        if (sharedInstance == null) sharedInstance = new DataSourceRepository();
        return sharedInstance;
    }

    
    void registerDataSourcesImpl(Set<? extends DataSource> added) {
        super.registerDataSourcesImpl(added);
        for (DataSource dataSource : added) dataSource.getRepository().addDataChangeListener(dataChangeListener, DataSource.class);
    }
    
    void unregisterDataSourcesImpl(Set<? extends DataSource> removed) {
        super.unregisterDataSourcesImpl(removed);
        for (DataSource dataSource : removed) dataSource.getRepository().removeDataChangeListener(dataChangeListener);
    }
    
    
    private DataSourceRepository() {
        registerDataSource(DataSource.ROOT);
    }
    
    
    private class Listener implements DataChangeListener<DataSource> {
        
        public void dataChanged(DataChangeEvent<DataSource> event) {
            Set<DataSource> added = event.getAdded();
            Set<DataSource> removed = event.getRemoved();
            if (!added.isEmpty() || !removed.isEmpty()) changeDataSources(added, removed);
        }
        
    }

}
