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

package org.graalvm.visualvm.core.datasource;

import org.graalvm.visualvm.core.datasupport.DataRemovedListener;
import org.graalvm.visualvm.core.datasupport.ComparableWeakReference;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.openide.util.RequestProcessor;

/**
 * Abstract implementation of DataSource.
 * DataSource is a base element of all data sources in VisualVM like applications,
 * hosts, thread dumps etc.
 *
 * @author Jiri Sedlacek
 */
public abstract class DataSource {
    
    /**
     * Named property for DataSource visibility.
     */
    public static final String PROPERTY_VISIBLE = "prop_visible";   // NOI18N
    
    /**
     * Event dispatch thread for all DataSource events. All operations on DataSources should be invoked
     * in this thread - similar to UI operations performed in AWT event dispatch thread.
     */
    public static final RequestProcessor EVENT_QUEUE = new RequestProcessor("DataSource Event Queue");  // NOI18N
    
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
     * Creates new instance of DataSource.
     */
    public DataSource() {
        this(null);
    }

    /**
     * Creates new instance of DataSource with defined master.
     * 
     * @param master master of the DataSource.
     */
    public DataSource(DataSource master) {
        this.master = master;
    }
    
    
    /**
     * Returns owner (parent) DataSource of this DataSource.
     * @return owner (parent) DataSource of this DataSource.
     */
    public final DataSource getOwner() {
        return owner;
    }
    
    /**
     * Sets visibility of the DataSource.
     * 
     * @param newVisible visibility of the DataSource.
     */
    public final synchronized void setVisible(boolean newVisible) {
        if (this == DataSource.ROOT && !newVisible) throw new IllegalArgumentException("DataSourceRoot cannot be hidden");  // NOI18N
        boolean oldVisible = visible;
        visible = newVisible;
        getChangeSupport().firePropertyChange(PROPERTY_VISIBLE, oldVisible, newVisible);
    }

    /**
     * Returns true if the DataSource is visible, false otherwise.
     * 
     * @return true if the DataSource is visible, false otherwise.
     */
    public final boolean isVisible() {
        return visible;
    }
    
    /**
     * Returns master of the DataSource.
     * 
     * @return master of the DataSource.
     */
    public final DataSource getMaster() {
        return master;
    }
    
    /**
     * Returns storage for the DataSource.
     * 
     * @return storage for the DataSource.
     */
    public final synchronized Storage getStorage() {
        if (storage == null) {
            storage = createStorage();
            if (storage == null) throw new NullPointerException("Storage cannot be null");  // NOI18N
        }
        return storage;
    }

    /**
     * Returns repository of the DataSource.
     * Repository is a container for other DataSources, virtually building a tree
     * structure of DataSources.
     * 
     * @return repository of the DataSource.
     */
    public final synchronized DataSourceContainer getRepository() {
        if (repository == null) repository = new DataSourceContainer(this);
        return repository;
    }

    /**
     * Add a PropertyChangeListener to the listener list.
     * The listener is registered for all properties.
     * The same listener object may be added more than once, and will be called
     * as many times as it is added.
     * If <code>listener</code> is null, no exception is thrown and no action
     * is taken.
     *
     * @param listener  The PropertyChangeListener to be added
     */
    public final void addPropertyChangeListener(PropertyChangeListener listener) {
        getChangeSupport().addPropertyChangeListener(listener);
    }

    /**
     * Add a PropertyChangeListener for a specific property.  The listener
     * will be invoked only when a call on firePropertyChange names that
     * specific property.
     * The same listener object may be added more than once.  For each
     * property,  the listener will be invoked the number of times it was added
     * for that property.
     * If <code>propertyName</code> or <code>listener</code> is null, no
     * exception is thrown and no action is taken.
     *
     * @param propertyName  The name of the property to listen on.
     * @param listener  The PropertyChangeListener to be added
     */
    public final void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        getChangeSupport().addPropertyChangeListener(propertyName, listener);
    }

    /**
     * Remove a PropertyChangeListener from the listener list.
     * This removes a PropertyChangeListener that was registered
     * for all properties.
     * If <code>listener</code> was added more than once to the same event
     * source, it will be notified one less time after being removed.
     * If <code>listener</code> is null, or was never added, no exception is
     * thrown and no action is taken.
     *
     * @param listener  The PropertyChangeListener to be removed
     */
    public final void removePropertyChangeListener(PropertyChangeListener listener) {
        getChangeSupport().removePropertyChangeListener(listener);
    }

    /**
     * Remove a PropertyChangeListener for a specific property.
     * If <code>listener</code> was added more than once to the same event
     * source for the specified property, it will be notified one less time
     * after being removed.
     * If <code>propertyName</code> is null,  no exception is thrown and no
     * action is taken.
     * If <code>listener</code> is null, or was never added for the specified
     * property, no exception is thrown and no action is taken.
     *
     * @param propertyName  The name of the property that was listened on.
     * @param listener  The PropertyChangeListener to be removed
     */
    public final void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        getChangeSupport().removePropertyChangeListener(propertyName, listener);
    }
    
    /**
     * Returns true if the DataSource can be removed using Remove action, false otherwise.
     * 
     * @return true if the DataSource can be removed using Remove action, false otherwise.
     */
    public boolean supportsUserRemove() {
        return false;
    }
    
    /**
     * Adds a DataRemovedListener to be notified when the DataSource is removed from the tree.
     * Note that this listener cannot be explicitly unregistered, it's weakly referenced and will
     * be notified up to once and then unregistered automatically.
     * 
     * @param listener DataRemovedListener to be notified when the DataSource is removed from the tree.
     */
    public final void notifyWhenRemoved(DataRemovedListener listener) {
        if (listener == null) throw new IllegalArgumentException("Listener cannot be null");    // NOI18N
        if (isRemoved()) listener.dataRemoved(this);
        else getRemovedListeners().add(new ComparableWeakReference(listener));
    }
    
    /**
     * Returns true if the DataSource has already been removed from DataSources tree, false otherwise.
     * @return true if the DataSource has already been removed from DataSources tree, false otherwise.
     */
    public final boolean isRemoved() {
        return isRemoved;
    }
    
    /**
     * Returns true if the DataSource can be removed in context of removeRoot.
     * The check is blocking, this is a chance for example to warn the user about
     * possible data loss when removing the DataSource representing an unsaved snapshot.
     * 
     * @param removeRoot DataSource which invoked the removal action (topmost DataSource to be removed).
     * @return true if the DataSource can be removed in context of removeRoot, false otherwise.
     */
    public boolean checkRemove(DataSource removeRoot) {
        return true;
    }
    
    // Implementation of this DataSource removal
    // Persistent DataSources can remove appropriate entries from their storage
    protected void remove() {
        getStorage().deleteCustomPropertiesStorage();
    }
    
    
    final void addImpl(DataSource owner) {
        if (isRemoved) throw new UnsupportedOperationException("DataSource can be added only once");    // NOI18N
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
        return new Storage();
    }
    
    /**
     * Returns instance of PropertyChangeSupport used for processing property changes.
     * 
     * @return instance of PropertyChangeSupport used for processing property changes.
     */
    protected final synchronized PropertyChangeSupport getChangeSupport() {
        if (changeSupport == null) changeSupport = new PropertyChangeSupport(this);
        return changeSupport;
    }
    
    
    final boolean hasRemovedListeners() {
        return removedListeners != null;
    }
    
    final synchronized Set<ComparableWeakReference<DataRemovedListener>> getRemovedListeners() {
        if (!hasRemovedListeners()) removedListeners = Collections.synchronizedSet(new HashSet());
        return removedListeners;
    }

}
