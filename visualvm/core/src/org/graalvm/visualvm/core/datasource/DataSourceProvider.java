/*
 * Copyright (c) 2007, 2022, Oracle and/or its affiliates. All rights reserved.
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

import org.graalvm.visualvm.core.datasupport.DataChangeEvent;
import org.graalvm.visualvm.core.datasupport.DataChangeListener;
import org.graalvm.visualvm.core.datasupport.Utils;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default implementation of DataSourceProvider.
 * DataSourceProviders can benefit from extending this class which implements
 * managing created DataSource instances and firing the events to listeners.
 *
 * @author Jiri Sedlacek
 */
public class DataSourceProvider {

    private static final boolean SUPPRESS_EXCEPTIONS_UI =
            Boolean.getBoolean(DataSourceProvider.class.getName() + ".suppressExceptionsUI"); // NOI18N
    private static final Logger LOGGER = Logger.getLogger(DataSourceProvider.class.getName());

    private final Set<DataSource> dataSources = Collections.synchronizedSet(new HashSet<>());
    private final Map<DataChangeListener<? extends DataSource>, Class<? extends DataSource>> listeners = new HashMap<>();


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
                if (listeners.containsKey(listener)) {
                    String msg = "Listener " + listener + " already registered"; // NOI18N
                    LOGGER.log(Level.SEVERE, msg, new UnsupportedOperationException(msg));
                } else {
                    listeners.put(listener, scope);
                    fireCurrentState(listener);
                }
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
                if (!listeners.containsKey(listener)) {
                    String msg = "Listener " + listener + " not registered"; // NOI18N
                    LOGGER.log(Level.SEVERE, msg, new UnsupportedOperationException(msg));
                } else {
                    listeners.remove(listener);
                }
            }
        });
    }

    /**
     * Returns DataSources managed by this provider.
     * @return DataSources managed by this provider.
     */
    public final Set<DataSource> getDataSources() {
        return new HashSet<>(dataSources);
    }

    /**
     * Returns DataSources of a certain type managed by this provider.
     *
     * @param <Y> any DataSource type.
     * @param scope DataSource types to return.
     * @return DataSources of a certain type managed by this provider.
     */
    public final <Y extends DataSource> Set<Y> getDataSources(Class<Y> scope) {
        return Utils.getFilteredSet(getDataSources(), scope);
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
                if (!added.isEmpty())
                    registerDataSourcesImpl(checkAdded(added));
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
                if (!removed.isEmpty())
                    unregisterDataSourcesImpl(checkRemoved(removed));
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
                if (!removed.isEmpty())
                    unregisterDataSourcesImpl(checkRemoved(removed));
                if (!added.isEmpty())
                    registerDataSourcesImpl(checkAdded(added));
            }
        });
    }

    void registerDataSourcesImpl(Set<? extends DataSource> added) {
        dataSources.addAll(added);
        fireDataAdded(added);
    }

    void unregisterDataSourcesImpl(Set<? extends DataSource> removed) {
        dataSources.removeAll(removed);
        fireDataRemoved(removed);
    }

    private Set<? extends DataSource> checkAdded(Set<? extends DataSource> added) {
        Set<? extends DataSource> uniqueAdded = new HashSet<>(added);
        Iterator<? extends DataSource> it = uniqueAdded.iterator();

        while(it.hasNext()) {
            DataSource ds = it.next();
            if (dataSources.contains(ds)) {
                it.remove();
                logUnsupportedOperation("DataSource already in repository: " + ds); // NOI18N
            }
        }

        return uniqueAdded;
    }

    private Set<? extends DataSource> checkRemoved(Set<? extends DataSource> removed) {
        Set<? extends DataSource> uniqueRemoved = new HashSet<>(removed);
        Iterator<? extends DataSource> it = uniqueRemoved.iterator();

        while(it.hasNext()) {
            DataSource ds = it.next();
            if (!dataSources.contains(ds)) {
                it.remove();
                logUnsupportedOperation("DataSource not in repository: " + ds); // NOI18N
            }
        }

        return uniqueRemoved;
    }


    private static void logUnsupportedOperation(String msg) {
        if (SUPPRESS_EXCEPTIONS_UI) LOGGER.severe(msg);
        else LOGGER.log(Level.SEVERE, msg, new UnsupportedOperationException(msg));
    }


    private void fireCurrentState(DataChangeListener<? extends DataSource> listener) {
        fireDataChanged(listener, null, null);
    }

    private void fireDataAdded(Set<? extends DataSource> added) {
        fireDataChanged(added, Collections.emptySet());
    }

    private void fireDataRemoved(Set<? extends DataSource> removed) {
        fireDataChanged(Collections.emptySet(), removed);
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
            Set<? extends DataSource> filteredAdded = added != null ? Utils.getFilteredSet(added, filter) : Collections.emptySet();
            Set<? extends DataSource> filteredRemoved = removed != null ? Utils.getFilteredSet(removed, filter) : Collections.emptySet();
            if (!filteredAdded.isEmpty() || !filteredRemoved.isEmpty()) {
                DataChangeEvent event = new DataChangeEvent(filteredCurrent, filteredAdded, filteredRemoved);
                listener.dataChanged(event);
            }
        }
    }

}
