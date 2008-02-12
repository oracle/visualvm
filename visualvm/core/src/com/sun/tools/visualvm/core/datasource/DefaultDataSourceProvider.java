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
import com.sun.tools.visualvm.core.datasupport.RequestProcessorFactory;
import com.sun.tools.visualvm.core.datasupport.Utils;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
public class DefaultDataSourceProvider<X extends DataSource> implements DataSourceProvider<X> {

    private final RequestProcessor queue = RequestProcessorFactory.getRequestProcessor();
    private final Set<X> dataSources = Collections.synchronizedSet(new HashSet());
    private final Map<DataChangeListener<? extends X>, Class<? extends X>> listeners = new HashMap();
    private final DataSource owner;
    
    
    public DefaultDataSourceProvider() {
        this(null);
    }
    
    DefaultDataSourceProvider(DataSource owner) {
        this.owner = owner;
    }
    

    public <Y extends X> void addDataChangeListener(final DataChangeListener<Y> listener, final Class<Y> scope) {
        queue.post(new Runnable() {
            public void run() {
                if (listeners.containsKey(listener)) throw new IllegalArgumentException("Listener " + listener + " already registered"); // NOI18N
                listeners.put(listener, scope);
                fireCurrentState(listener);
            }
        });
    }

    public <Y extends X> void removeDataChangeListener(final DataChangeListener<Y> listener) {
        queue.post(new Runnable() {
            public void run() {
                if (!listeners.containsKey(listener)) throw new IllegalArgumentException("Listener " + listener + " not registered"); // NOI18N
                listeners.remove(listener);
            }
        });
    }

    public Set<X> getDataSources() {
        return new HashSet(dataSources);
    }

    public <Y extends X> Set<Y> getDataSources(Class<Y> scope) {
        return Utils.getFilteredSet(dataSources, scope);
    }
    
    
    protected <Y extends X> void registerDataSource(Y added) {
        registerDataSources(Collections.singleton(added));
    }
    
    protected <Y extends X> void registerDataSources(final Set<Y> added) {
        queue.post(new Runnable() {
            public void run() {
                dataSources.addAll(added);
                if (owner != null) for (Y dataSource : added) {
                    DataSource dataSourceOwner = dataSource.getOwner();
                    if (dataSourceOwner != owner) {
                        if (dataSourceOwner != null) dataSourceOwner.getRepository().removeDataSource(dataSource);
                        dataSource.setOwner(owner);
                    }
                }
                fireDataAdded(added);
            }
        });
    }
    
    protected <Y extends X> void unregisterDataSource(Y removed) {
        unregisterDataSources(Collections.singleton(removed));
    }
    
    protected <Y extends X> void unregisterDataSources(final Set<Y> removed) {
        queue.post(new Runnable() {
            public void run() {
                dataSources.removeAll(removed);
                if (owner != null) for (Y dataSource : removed) dataSource.setOwner(null);
                fireDataRemoved(removed);
            }
        });
    }
    
    protected <Y extends X> void updateDataSources(final Set<Y> added, final Set<Y> removed) {
        queue.post(new Runnable() {
            public void run() {
                dataSources.addAll(added);
                dataSources.removeAll(removed);
                if (owner != null) {
                    for (Y dataSource : added) {
                        // TODO: group added by owners and use owner.getRepository().removeDataSources()
                        if (dataSource.getOwner() != null) dataSource.getOwner().getRepository().removeDataSource(dataSource);
                        dataSource.setOwner(owner);
                    }
                    for (Y dataSource : removed) dataSource.setOwner(null);
                }
                fireDataAdded(added);
                fireDataRemoved(removed);
            }
        });
    }
    
    
    private <Y extends X> void fireCurrentState(DataChangeListener<Y> listener) {
        fireDataChanged(listener, null, null);
    }
    
    private <Y extends X> void fireDataAdded(Set<Y> added) {
        fireDataChanged(added, Collections.EMPTY_SET);
    }
    
    private <Y extends X> void fireDataRemoved(Set<Y> removed) {
        fireDataChanged(Collections.EMPTY_SET, removed);
    }
    
    private <Y extends X> void fireDataChanged(Set<Y> added, Set<Y> removed) {
        Set<DataChangeListener<? extends X>> listenersSet = listeners.keySet();
        for (DataChangeListener listener : listenersSet) fireDataChanged(listener, added, removed);
    }
    
    private <Y extends X, Z extends X> void fireDataChanged(DataChangeListener<Y> listener, Set<Z> added, Set<Z> removed) {
        Class<Y> filter = (Class<Y>)listeners.get(listener);
        Set<Y> filteredCurrent = Utils.getFilteredSet(dataSources, filter);
        if (added == null && removed == null) {
            DataChangeEvent event = new DataChangeEvent(filteredCurrent, filteredCurrent, null);
            listener.dataChanged(event);
        } else {
            Set<Y> filteredAdded = Utils.getFilteredSet(added, filter);
            Set<Y> filteredRemoved = Utils.getFilteredSet(removed, filter);
            if (!filteredAdded.isEmpty() || !filteredRemoved.isEmpty()) {
                DataChangeEvent event = new DataChangeEvent(filteredCurrent, filteredAdded, filteredRemoved);
                listener.dataChanged(event);
            }
        }
    }

}
