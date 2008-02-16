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
import com.sun.tools.visualvm.core.snapshot.SnapshotSupport;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
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

    private DataSource owner;
    private DataSource master;
    private int state = STATE_AVAILABLE;
    private boolean visible = true;
    private final DataSourceContainer<DataSource> repository = new DefaultDataSourceContainer(this);
    private final PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

    private final Set<WeakReference<DataFinishedListener>> removedListeners = Collections.synchronizedSet(new HashSet());


    public AbstractDataSource() {
        this(null);
    }

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
    
    public File getStorage() {
        return SnapshotSupport.getInstance().getDefaultStorageDirectory();
    }

    public DataSourceContainer getRepository() {
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
        else removedListeners.add(new WeakReference(listener));
    }
    
    public boolean isFinished() {
        return getState() == STATE_FINISHED;
    }
    
    
    protected synchronized void setState(int newState) {
        if (state == STATE_FINISHED) throw new RuntimeException("Cannot change state from STATE_FINISHED");
        int oldState = state;
        state = newState;
        getChangeSupport().firePropertyChange(PROPERTY_STATE, oldState, newState);
        if (newState == STATE_FINISHED) {
            for (WeakReference<DataFinishedListener> listenerReference : removedListeners) {
                DataFinishedListener listener = listenerReference.get();
                if (listener != null) listener.dataFinished(this);
            }
            removedListeners.clear();
        }
    }
    
    
    // NOTE: doesn't need to be final
    protected final PropertyChangeSupport getChangeSupport() {
        return changeSupport;
    }

}
