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

package com.sun.tools.visualvm.core.datasource;

import com.sun.tools.visualvm.core.datasupport.DataChangeEvent;
import com.sun.tools.visualvm.core.datasupport.DataChangeListener;

/**
 * Central repository of all known DataSources.
 * Each DataSourceProvider which wants to publish created DataSources
 * should register into DataSourceRepository. This allows other (depending) providers
 * to discover new DataSource and process it. For example Host provider registers
 * new Host instances into DataSourceRepository, the instances are the discovered
 * by Application provider which tries to detect all applications running on the Host.
 *
 * @author Jiri Sedlacek
 */
public final class DataSourceRepository extends DefaultDataSourceProvider<DataSource> implements DataChangeListener<DataSource> {

    private static DataSourceRepository sharedInstance;


    /**
     * Returns singleton instance of DataSourceRepository.
     * 
     * @return singleton instance of DataSourceRepository.
     */
    public synchronized static DataSourceRepository sharedInstance() {
        if (sharedInstance == null) sharedInstance = createSharedInstance();
        return sharedInstance;
    }


    /**
     * Registers new DataSourceProvider to the repository.
     * 
     * @param provider DataSourceProvider to be added.
     */
    public void addDataSourceProvider(DataSourceProvider provider) {
        provider.addDataChangeListener(this, DataSource.class);
    }
    
    /**
     * Unregisters DataSourceProvider from the repository.
     * 
     * @param provider DataSourceProvider to be removed.
     */
    public void removeDataSourceProvider(DataSourceProvider provider) {
        provider.removeDataChangeListener(this);
    }

    public void dataChanged(DataChangeEvent<DataSource> event) {
        updateDataSources(event.getAdded(), event.getRemoved());
    }
    
    
    private DataSourceRepository() {}
    
    private static DataSourceRepository createSharedInstance() {
        return new DataSourceRepository();
    }

}
