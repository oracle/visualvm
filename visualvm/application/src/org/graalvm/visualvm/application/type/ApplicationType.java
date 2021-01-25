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
package org.graalvm.visualvm.application.type;

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.core.model.Model;
import java.awt.Image;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * This class is used to identify different type of Java
 * applications. Using this class you can obtain 
 * application's name and icon. VisualVM plugins can
 * use instances of this class to identify particular
 * application type (like GlassFish) and provide
 * additional information for that application type.
 * To get instance of ApplicationType for {@link Application}
 * use the following factory method <code>
 * ApplicationTypeFactory.getApplicationTypeFor(Application app)
 * </code>
 * @author Tomas Hurka
 */
public abstract class ApplicationType extends Model {
    public static final String PROPERTY_NAME = "name"; // NOI18N
    /**
     * Named property for suggested name of the application type. This name will
     * eventually be used instead of "Local Application" or "Remote Application"
     * in case the application type is not recognized.
     */
    public static final String PROPERTY_SUGGESTED_NAME = "prop_suggested_name"; // NOI18N
    public static final String PROPERTY_DESCRIPTION = "description"; // NOI18N
    public static final String PROPERTY_VERSION = "version"; // NOI18N
    public static final String PROPERTY_ICON = "icon"; // NOI18N

    private PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    /**
     * Gets the name of the application.
     * @return this application's name
     */
    public abstract String getName();

    /**
     * Gets the version of the application.
     * @return this application's version
     */    
    public abstract String getVersion();

    /**
     * Gets the description of the application.
     * @return this application's description
     */    
    public abstract String getDescription();

    /**
     * Gets the icon of the application.
     * @return this application's icon
     */    
    public abstract Image getIcon();

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
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
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
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    /**
     * Report a bound property update to any registered listeners.
     * No event is fired if old and new are equal and non-null.
     *
     * @param propertyName  The programmatic name of the property
     *		that was changed.
     * @param oldValue  The old value of the property.
     * @param newValue  The new value of the property.
     */
    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        pcs.firePropertyChange(propertyName, oldValue, newValue);
    }
}
