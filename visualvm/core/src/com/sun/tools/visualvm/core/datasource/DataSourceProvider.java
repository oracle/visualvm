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
import com.sun.tools.visualvm.core.datasupport.Utils;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Default implementation of DataSourceProvider.
 * DataSourceProviders can benefit from extending this class which implements
 * managing created DataSource instances and firing the events to listeners.
 *
 * @author Jiri Sedlacek
 */
public class DataSourceProvider {

    private final Set<DataSource> dataSources = Collections.synchronizedSet(new HashSet());
    private final Map<DataChangeListener<? extends DataSource>, Class<? extends DataSource>> listeners = new HashMap();
    
    
    /**
     * Creates new instance of DataSourceProvider.
     */
    DataSourceProvider() {
    }
    

    /**
     * Adds a DataChangeListener to listen for added/removed DataSources.
     * 
     * @param <Y> any DataSource type.
     * @param listener listener to be added.
     * @param scope scope of DataSource types for which to get notifications.
     */
    public final <Y extends DataSource> void addDataChangeListener(final DataChangeListener<Y> listener, final Class<Y> scope) {
        DataSource.EVENT_QUEUE.post(new Runnable() {
            public void run() {
                if (listeners.containsKey(listener)) throw new IllegalArgumentException("Listener " + listener + " already registered"); // NOI18N
                listeners.put(listener, scope);
                fireCurrentState(listener);
            }
        });
    }

    /**
     * Removes a DataChange listener.
     * 
     * @param <Y> any DataSource type.
     * @param listener listener to be removed.
     */
    public final <Y extends DataSource> void removeDataChangeListener(final DataChangeListener<Y> listener) {
        DataSource.EVENT_QUEUE.post(new Runnable() {
            public void run() {
                if (!listeners.containsKey(listener)) throw new IllegalArgumentException("Listener " + listener + " not registered"); // NOI18N
                listeners.remove(listener);
            }
        });
    }

    /**
     * Returns DataSources managed by this provider.
     * @return DataSources managed by this provider.
     */
    public final Set<DataSource> getDataSources() {
        return new HashSet(dataSources);
    }

    /**
     * Returns DataSources of a certain type managed by this provider.
     * 
     * @param <Y> any DataSource type.
     * @param scope DataSource types to return.
     * @return DataSources of a certain type managed by this provider.
     */
    public final <Y extends DataSource> Set<Y> getDataSources(Class<Y> scope) {
        return Utils.getFilteredSet(dataSources, scope);
    }
    
    
    /**
     * Registers added DataSource into this provider.
     * 
     * @param added added DataSource to register.
     */
    protected final void registerDataSource(DataSource added) {
        registerDataSources(Collections.singleton(added));
    }
    
    /**
     * Registers added DataSources into this provider.
     * 
     * @param added added DataSources to register.
     */
    protected final void registerDataSources(final Set<? extends DataSource> added) {
        DataSource.EVENT_QUEUE.post(new Runnable() {
            public void run() {
                if (!added.isEmpty()) registerDataSourcesImpl(added);
            }
        });
    }
    
    /**
     * Unregisters removed DataSource from this provider.
     * 
     * @param removed removed DataSource to unregister.
     */
    protected final void unregisterDataSource(DataSource removed) {
        unregisterDataSources(Collections.singleton(removed));
    }
    
    /**
     * Unregisters removed DataSources from this provider.
     * 
     * @param removed removed DataSources to unregister.
     */
    protected final void unregisterDataSources(final Set<? extends DataSource> removed) {
        DataSource.EVENT_QUEUE.post(new Runnable() {
            public void run() {
                if (!removed.isEmpty()) unregisterDataSourcesImpl(removed);
            }
        });
    }
    
    /**
     * Registers added DataSources into this provider and unregisters removed DataSources from this provider.
     * 
     * @param added added DataSources to register.
     * @param removed removed DataSources to unregister.
     */
    protected final void changeDataSources(final Set<? extends DataSource> added, final Set<? extends DataSource> removed) {
        DataSource.EVENT_QUEUE.post(new Runnable() {
            public void run() {
                if (!removed.isEmpty()) unregisterDataSourcesImpl(removed);
                if (!added.isEmpty()) registerDataSourcesImpl(added);
            }
        });
    }
    
    void registerDataSourcesImpl(Set<? extends DataSource> added) {
        for (DataSource dataSource : added) if (dataSources.contains(dataSource))
            throw new UnsupportedOperationException("DataSource already in repository: " + dataSource); // NOI18N
            
        dataSources.addAll(added);
        fireDataAdded(added);
    }
    
    void unregisterDataSourcesImpl(Set<? extends DataSource> removed) {
        for (DataSource dataSource : removed) if (!dataSources.contains(dataSource))
            throw new UnsupportedOperationException("DataSource not in repository: " + dataSource); // NOI18N
        
        dataSources.removeAll(removed);
        fireDataRemoved(removed);
    }
    
    
    private void fireCurrentState(DataChangeListener<? extends DataSource> listener) {
        fireDataChanged(listener, null, null);
    }
    
    private void fireDataAdded(Set<? extends DataSource> added) {
        fireDataChanged(added, Collections.EMPTY_SET);
    }
    
    private void fireDataRemoved(Set<? extends DataSource> removed) {
        fireDataChanged(Collections.EMPTY_SET, removed);
    }
    
    private void fireDataChanged(Set<? extends DataSource> added, Set<? extends DataSource> removed) {
        Set<DataChangeListener<? extends DataSource>> listenersSet = listeners.keySet();
        for (DataChangeListener listener : listenersSet) fireDataChanged(listener, added, removed);
    }
    
    private void fireDataChanged(DataChangeListener<? extends DataSource> listener, Set<? extends DataSource> added, Set<? extends DataSource> removed) {
        Class<? extends DataSource> filter = listeners.get(listener);
        Set<? extends DataSource> filteredCurrent = Utils.getFilteredSet(dataSources, filter);
        if (added == null && removed == null) {
            DataChangeEvent event = new DataChangeEvent(filteredCurrent, filteredCurrent, null);
            listener.dataChanged(event);
        } else {
            Set<? extends DataSource> filteredAdded = added != null ? Utils.getFilteredSet(added, filter) : Collections.EMPTY_SET;
            Set<? extends DataSource> filteredRemoved = removed != null ? Utils.getFilteredSet(removed, filter) : Collections.EMPTY_SET;
            if (!filteredAdded.isEmpty() || !filteredRemoved.isEmpty()) {
                DataChangeEvent event = new DataChangeEvent(filteredCurrent, filteredAdded, filteredRemoved);
                listener.dataChanged(event);
            }
        }
    }

}
