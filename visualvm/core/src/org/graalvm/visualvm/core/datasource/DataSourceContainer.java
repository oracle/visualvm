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

package org.graalvm.visualvm.core.datasource;

import java.util.Set;

/**
 * Default implementation of DataSourceContainer.
 * This class implements all necessary methods to act as a repository of a DataSource (DataSource.getRepository()).
 *
 * @author Jiri Sedlacek
 */
public final class DataSourceContainer extends DataSourceProvider {
    
    private final DataSource owner;
    
    
    /**
     * Default implementation of DataSourceContainer.
     * DataSoures can benefit from using this class which implements
     * managing created DataSource instances and firing the events to listeners
     * as their repositories.
     * 
     * @param owner
     */
    DataSourceContainer(DataSource owner) {
        this.owner = owner;
    }
    

    /**
     * Adds a DataSource to the container.
     * 
     * @param added DataSource to be added.
     */
    public void addDataSource(DataSource added) {
        registerDataSource(added);
    }

    /**
     * Adds several DataSources to the container at once.
     * 
     * @param added DataSources to be added.
     */
    public void addDataSources(Set<? extends DataSource> added) {
        super.registerDataSources(added);
    }

    /**
     * Removes a DataSource from the container.
     * 
     * @param removed DataSource to be removed.
     */
    public void removeDataSource(DataSource removed) {
        unregisterDataSource(removed);
    }

    /**
     * Removes several DataSources from the container at once.
     * 
     * @param removed DataSources to be removed.
     */
    public void removeDataSources(Set<? extends DataSource> removed) {
        super.unregisterDataSources(removed);
    }
    
    /**
     * Adds and removes several DataSources to/from the container in a single operation.
     * 
     * @param added DataSources to be added.
     * @param removed DataSources to be removed.
     */
    public void updateDataSources(Set<? extends DataSource> added, Set<? extends DataSource> removed) {
        super.changeDataSources(added, removed);
    }
    
    
    protected void registerDataSourcesImpl(Set<? extends DataSource> added) {
        for (DataSource dataSource : added) dataSource.addImpl(owner);
        super.registerDataSourcesImpl(added);
    }
    
    protected void unregisterDataSourcesImpl(Set<? extends DataSource> removed) {
        for (DataSource dataSource : removed) {
            DataSourceContainer dataSourceRepository = dataSource.getRepository();
            dataSourceRepository.unregisterDataSourcesImpl(dataSourceRepository.getDataSources());
            dataSource.removeImpl();
        }
        super.unregisterDataSourcesImpl(removed);
    }

}
