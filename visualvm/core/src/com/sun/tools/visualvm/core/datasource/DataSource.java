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

import com.sun.tools.visualvm.core.datasupport.DataRemovedListener;
import com.sun.tools.visualvm.core.datasupport.ComparableWeakReference;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class DataSource {
    
    /**
     * Named property for DataSource visibility.
     */
    public static final String PROPERTY_VISIBLE = "prop_visible";
    
    public static final RequestProcessor EVENT_QUEUE = new RequestProcessor("DataSource Event Queue");
    
    /**
     * Virtual root of DataSource tree.
     * ROOT corresponds to invisible root of explorer tree.
     */
    public static final DataSource ROOT = new DataSource() {};
    
    
    private DataSource owner;
    private boolean isRemoved = false;
    private DataSource master;
    private boolean visible = true;
    private Storage storage;
    private DataSourceContainer repository;
    private PropertyChangeSupport changeSupport;
    private Set<ComparableWeakReference<DataRemovedListener>> removedListeners;


    /**
     * Creates new instance of AbstractDataSource with no master.
     */
    public DataSource() {
        this(null);
    }

    /**
     * Creates new instance of AbstractDataSource with defined master.
     * 
     * @param master master of the DataSource.
     */
    public DataSource(DataSource master) {
        this.master = master;
    }
    
    
    public final DataSource getOwner() {
        return owner;
    }
    
    public final synchronized void setVisible(boolean newVisible) {
        if (this == DataSource.ROOT && !newVisible) throw new IllegalArgumentException("DataSourceRoot cannot be hidden");
        boolean oldVisible = visible;
        visible = newVisible;
        getChangeSupport().firePropertyChange(PROPERTY_VISIBLE, oldVisible, newVisible);
    }

    public final boolean isVisible() {
        return visible;
    }
    
    public final DataSource getMaster() {
        return master;
    }
    
    public final Storage getStorage() {
        if (storage == null) {
            storage = createStorage();
            if (storage == null) throw new NullPointerException("Storage cannot be null");
        }
        return storage;
    }

    public final DataSourceContainer getRepository() {
        if (repository == null) repository = new DataSourceContainer(this);
        return repository;
    }

    public final void addPropertyChangeListener(PropertyChangeListener listener) {
        getChangeSupport().addPropertyChangeListener(listener);
    }

    public final void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        getChangeSupport().addPropertyChangeListener(propertyName, listener);
    }

    public final void removePropertyChangeListener(PropertyChangeListener listener) {
        getChangeSupport().removePropertyChangeListener(listener);
    }

    public final void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        getChangeSupport().removePropertyChangeListener(propertyName, listener);
    }
    
    // Returns true if this DataSource can be removed using Remove action
    public boolean supportsUserRemove() {
        return false;
    }
    
    // Notifies the listener that the DataSource has been removed from the tree
    public final void notifyWhenRemoved(DataRemovedListener listener) {
        if (listener == null) throw new IllegalArgumentException("Listener cannot be null");
        if (isRemoved()) listener.dataRemoved(this);
        else getRemovedListeners().add(new ComparableWeakReference(listener));
    }
    
    // Checks if the DataSource has been removed from the tree
    public final boolean isRemoved() {
        return isRemoved;
    }
    
    // Performs blocking check if the DataSource can be removed in context of removeRoot
    // Here the DataSource can for example warn user about possible data loss.
    public boolean checkRemove(DataSource removeRoot) {
        return true;
    }
    
    // Implementation of this DataSource removal
    // Persistent DataSources can remove appropriate entries from their storage
    protected void remove() {
        getStorage().deleteCustomPropertiesStorage();
    }
    
    
    final void addImpl(DataSource owner) {
        if (isRemoved) throw new UnsupportedOperationException("DataSource can be added only once");
        this.owner = owner;
    }
    
    final void removeImpl() {
        remove();
        
        this.owner = null;
        isRemoved = true;
        
        if (!hasRemovedListeners()) return;
        Set<ComparableWeakReference<DataRemovedListener>> listeners = getRemovedListeners();
        for (WeakReference<DataRemovedListener> listenerReference : listeners) {
            DataRemovedListener listener = listenerReference.get();
            if (listener != null) listener.dataRemoved(this);
        }
        listeners.clear();
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
     * Returns instance of PropertyChangeSupport used for processing property changes.
     * 
     * @return instance of PropertyChangeSupport used for processing property changes.
     */
    protected final PropertyChangeSupport getChangeSupport() {
        if (changeSupport == null) changeSupport = new PropertyChangeSupport(this);
        return changeSupport;
    }
    
    
    final boolean hasRemovedListeners() {
        return removedListeners != null;
    }
    
    final Set<ComparableWeakReference<DataRemovedListener>> getRemovedListeners() {
        if (!hasRemovedListeners()) removedListeners = Collections.synchronizedSet(new HashSet());
        return removedListeners;
    }

}
