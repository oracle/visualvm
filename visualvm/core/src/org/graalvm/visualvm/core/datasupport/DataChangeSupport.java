/*
 * Copyright (c) 2007, 2025, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.core.datasupport;

import org.graalvm.visualvm.core.datasource.DataSource;
import java.util.HashSet;
import java.util.Set;


/**
 * This is a utility class that can be used by similarly
 * to PropertyChangeSupport. All DataChangeSupport instances 
 * uses dedicated thread to for all the work, so all events 
 * is processed asynchronously. 
 *
 * @author Tomas Hurka
 */
public final class DataChangeSupport<X> {

    private ChangeSupport<X> changeSupport;

    /**
     * Constructs a <code>DataChangeSupport</code> object.
     */
    public DataChangeSupport() {
        changeSupport = new ChangeSupport();
    }

    /**
     * Add a DataChangeListener to the listener list.
     * The same listener object may be added only once.
     *
     * @param listener  The DataChangeListener to be added
     */
    public void addChangeListener(final DataChangeListener<X> listener) {
        DataSource.EVENT_QUEUE.post(new Runnable() {
            public void run() {
                changeSupport.addChangeListener(listener);
            }
        });
    }

    /**
     * Remove a DataChangeListener from the listener list.
     *
     * @param listener  The DataChangeListener to be removed
     */
    public void removeChangeListener(final DataChangeListener<X> listener) {
        DataSource.EVENT_QUEUE.post(new Runnable() {
            public void run() {
                changeSupport.removeChangeListener(listener);
            }
        });
    }
    
    /**
     * Report an update to any registered listeners.
     *
     * @param current  the set of currently available objects.
     * @param added  the set of added objects since last event.
     * @param removed  the set of removed objects since last event.
     */
    public void fireChange(Set<X> current, Set<X> added, Set<X> removed) {
        final Set<X> currentLocal = current != null ? new HashSet<>(current) : null;
        final Set<X> addedLocal = added != null ? new HashSet<>(added) : null;
        final Set<X> removedLocal = removed != null ? new HashSet<>(removed) : null;

        DataSource.EVENT_QUEUE.post(new Runnable() {
            public void run() {
                changeSupport.fireChange(currentLocal, addedLocal, removedLocal);
            }
        });
    }
    
    private static class ChangeSupport<X> {
        
        private Set<DataChangeListener<X>> listeners = new HashSet<>();
        private Set<X> currentSet;
        
        private void addChangeListener(DataChangeListener<X> listener) {
            if (!listeners.add(listener)) {
                throw new IllegalArgumentException("Listener " + listener + " already registered");     // NOI18N
            }
            if (currentSet != null) {
                DataChangeEvent<X> event = new DataChangeEvent<>(currentSet, currentSet, null);
                listener.dataChanged(event);
            }
        }
        
        private void removeChangeListener(DataChangeListener<X> listener) {
            if (!listeners.remove(listener)) {
                throw new IllegalArgumentException("Listener " + listener + " not registered"); // NOI18N
            }            
        }
        
        private void fireChange(Set<X> current, Set<X> added, Set<X> removed) {
            currentSet = current;
            if (listeners.isEmpty()) {
                return;
            }
            DataChangeEvent<X> event = new DataChangeEvent<>(current, added, removed);
            for (DataChangeListener<X> listener : listeners) {
                listener.dataChanged(event);
            }
        }
    }
}
