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

import com.sun.tools.visualvm.core.datasupport.DataFinishedListener;
import com.sun.tools.visualvm.core.datasupport.Storage;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Jiri Sedlacek
 */
// TODO: synchronize using RequestProcessor??
public abstract class AbstractDataSource implements DataSource {
    private class WeakReferenceX<T> extends WeakReference<T> {
        public WeakReferenceX(T referent, ReferenceQueue<? super T> q) {
            super(referent, q);
        }

        public WeakReferenceX(T referent) {
            super(referent);
        }
        
        public int hashCode() {
            return this.get() != null ? this.get().hashCode() : 0;
        }
        
        public boolean equals(Object o) {
            if (this.get() == null && o == null) return true;
            if (o == null) return false;
            if (!(o instanceof WeakReferenceX)) return false;
            return this.get().equals(((WeakReferenceX)o).get());
        }
    }
    private DataSource owner;
    private DataSource master;
    private int state = STATE_AVAILABLE;
    private boolean visible = true;
    private Storage storage;
    private DataSourceContainer<DataSource> repository;
    private PropertyChangeSupport changeSupport;
    private Set<WeakReferenceX<DataFinishedListener>> removedListeners;


    /**
     * Creates new instance of AbstractDataSource with no master.
     */
    public AbstractDataSource() {
        this(null);
    }

    /**
     * Creates new instance of AbstractDataSource with defined master.
     * 
     * @param master master of the DataSource.
     */
    public AbstractDataSource(DataSource master) {
        this.master = master;
    }
    
    
    public synchronized void setOwner(DataSource newOwner) {
        if (owner == null && newOwner == null) return;
        DataSource oldOwner = owner;
        owner = newOwner;
        getChangeSupport().firePropertyChange(PROPERTY_OWNER, oldOwner, newOwner);
    }

    public DataSource getOwner() {
        return owner;
    }

    public int getState() {
        return state;
    }
    
    public synchronized void setVisible(boolean newVisible) {
        boolean oldVisible = visible;
        visible = newVisible;
        getChangeSupport().firePropertyChange(PROPERTY_VISIBLE, oldVisible, newVisible);
    }

    public boolean isVisible() {
        return visible;
    }
    
    public DataSource getMaster() {
        return master;
    }
    
    public final Storage getStorage() {
        if (storage == null) {
            storage = createStorage();
            if (storage == null) throw new NullPointerException("Storage cannot be null");
            File directory = storage.getDirectory();
            if (!directory.exists() && !directory.mkdir()) throw new IllegalStateException("Cannot create storage directory " + directory);
        }
        return storage;
    }

    public final DataSourceContainer getRepository() {
        if (repository == null) {
            repository = createRepository();
            if (repository == null) throw new NullPointerException("Repository cannot be null");
        }
        return repository;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        getChangeSupport().addPropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        getChangeSupport().addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        getChangeSupport().removePropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        getChangeSupport().removePropertyChangeListener(propertyName, listener);
    }
    
    
    public void notifyWhenFinished(DataFinishedListener listener) {
        if (listener == null) throw new IllegalArgumentException("Listener cannot be null");
        if (isFinished()) listener.dataFinished(this);
        else getRemovedListeners().add(new WeakReferenceX(listener));
    }
    
    public boolean isFinished() {
        return getState() == STATE_FINISHED;
    }
    
    
    /**
     * Sets state of the DataSource.
     * Note that once the state is STATE_FINSHED it cannot be further changed. Attempt to change
     * state from STATE_FINISHED will cause a RuntimeException.
     * 
     * @param newState state of the DataSource.
     */
    protected synchronized void setState(int newState) {
        if (state == STATE_FINISHED) throw new RuntimeException("Cannot change state from STATE_FINISHED");
        int oldState = state;
        state = newState;
        getChangeSupport().firePropertyChange(PROPERTY_STATE, oldState, newState);
        if (newState == STATE_FINISHED) {
            Set<WeakReferenceX<DataFinishedListener>> listeners = getRemovedListeners();
            for (WeakReference<DataFinishedListener> listenerReference : listeners) {
                DataFinishedListener listener = listenerReference.get();
                if (listener != null) listener.dataFinished(this);
            }
            getRemovedListeners().clear();
        }
    }
    
    
    /**
     * Creates Storage instance for this DataSource.
     * This method should never return null.
     * 
     * @return Storage instance for this DataSource.
     */
    protected Storage createStorage() {
        return new Storage(Storage.getTemporaryStorageDirectory());
    }
    
    /**
     * Creates repository for this DataSource.
     * This method should never return null.
     * 
     * @return repository for this DataSource.
     */
    protected DataSourceContainer createRepository() {
        return new DefaultDataSourceContainer(this);
    }
    
    /**
     * Returns instance of PropertyChangeSupport used for processing property changes.
     * 
     * @return instance of PropertyChangeSupport used for processing property changes.
     */
    protected final PropertyChangeSupport getChangeSupport() {
        if (changeSupport == null) changeSupport = new PropertyChangeSupport(this);
        return changeSupport;
    }
    
    
    private Set<WeakReferenceX<DataFinishedListener>> getRemovedListeners() {
        if (removedListeners == null) removedListeners = Collections.synchronizedSet(new HashSet());
        return removedListeners;
    }

}
