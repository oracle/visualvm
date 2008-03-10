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
import com.sun.tools.visualvm.core.datasource.Storage;
import java.beans.PropertyChangeListener;
import org.openide.util.RequestProcessor;

/**
 * DataSource is a general object representing a data unit within the VisualVM.
 *
 * @author Jiri Sedlacek
 */
public interface DataSource {
    
    public static final RequestProcessor EVENT_QUEUE = new RequestProcessor("DataSource Event Queue");

    /**
     * Named property for DataSource owner.
     */
    public static final String PROPERTY_OWNER = "prop_owner";
    /**
     * Named property for DataSource state.
     */
    public static final String PROPERTY_STATE = "prop_state";
    /**
     * Named property for DataSource visibility.
     */
    public static final String PROPERTY_VISIBLE = "prop_visible";

    /**
     * State of this DataSource cannot be determined.
     */
    public static final int STATE_UNKNOWN = -1;
    /**
     * DataSource is unavailable.
     * This can mean temporarily unavailable host or definitely terminated application.
     */
    public static final int STATE_UNAVAILABLE = 0;
    /**
     * DataSource is available.
     * This means an online host or a running application.
     */
    public static final int STATE_AVAILABLE = 1;
    /**
     * DataSource is at the end of it's lifecycle.
     * This means that state of the DataSource will always be STATE_FINISHED.
     */
    public static final int STATE_FINISHED = Integer.MIN_VALUE;
    
    /**
     * Virtual root of DataSource tree.
     * ROOT corresponds to invisible root of explorer tree.
     */
    public static final DataSource ROOT = new DataSourceRoot();

    /**
     * Sets owner of this DataSource.
     * Do not use this method directly, owner is set automatically when adding a DataSource
     * into owner's repository.
     * 
     * @param owner owner of this DataSource.
     */
    public void setOwner(DataSource owner);
    /**
     * Returns owner of this DataSource.
     * Owner is the DataSource which repository contains this DataSource.
     * 
     * @return "parent" of this DataSource.
     */
    public DataSource getOwner();

    /**
     * Returns current state of this DataSource.
     * State can be STATE_AVAILABLE, STATE_UNAVAILABLE, STATE_UNKNOWN or STATE_FINISHED, subclasses can
     * define additional states. Can be monitored by a PropertyChangeListener as property PROPERTY_STATE.
     * 
     * @return current state of this DataSource.
     */
    public int getState();
    
    /**
     * Sets visibility of this DataSource in explorer tree.
     * 
     * @param visible
     */
    public void setVisible(boolean visible);
    /**
     * Returns true if this DataSource is visible in explorer tree, false otherwise.
     * 
     * @return true if this DataSource is visible in explorer tree, false otherwise.
     */
    public boolean isVisible();
    
    /**
     * Returns a master DataSource which will display this DataSource as a subtab.
     * This is used for example for thread dumps and heap dumps which are opened as additional views
     * of an application.
     * 
     * @return master DataSource or null when this DataSource should be displayed in its own window.
     */
    public DataSource getMaster();
    
    /**
     * Returns Storage instance for this DataSource.
     * 
     * @return Storage instance for this DataSource.
     */
    public Storage getStorage();

    /**
     * Repository of this DataSource which can contain any other DataSources.
     * 
     * @return repository of this DataSource.
     */
    public DataSourceContainer getRepository();

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
    public void addPropertyChangeListener(PropertyChangeListener listener);
    
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
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener);
    
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
    public void removePropertyChangeListener(PropertyChangeListener listener);
    
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
    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener);
    
    /**
     * Returns true if state of this DataSource is STATE_FINISHED.
     * This means that the DataSource has been unregistered from all repositories
     * and removed from explorer tree.
     * Once the DataSource is in STATE_FINISHED, its state won't change any more.
     * 
     * @return true if state of this DataSource is STATE_FINISHED.
     */
    public boolean isFinished();
    
    /**
     * Registers a DataFinishedListener for a single notification when this DataSource is removed.
     * This event occurs only once during the DataSource lifecycle exactly when the isFinished() method
     * starts to return true. At this time the DataSource should be unregistered from all repositories
     * and removed from explorer tree.
     * Note that this listener is referenced by WeakReference in monitored DataSource -
     * be sure that the listener has at least one strong reference. You should always avoid writing
     * code like this:<pre>
     * DataSource.notifyWhenFinished(new DataFinishedListener() { ... });
     * </pre>
     * 
     * @param listener a DataFinishedListener for a single notification when state of this DataSource becomes STATE_FINISHED.
     */
    public void notifyWhenFinished(DataFinishedListener listener);

}
