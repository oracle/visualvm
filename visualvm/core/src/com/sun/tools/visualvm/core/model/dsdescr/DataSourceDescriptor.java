/*
 *  Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Sun designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Sun in the LICENSE file that accompanied this code.
 * 
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 * 
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 *  Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 *  CA 95054 USA or visit www.sun.com if you need additional information or
 *  have any questions.
 */

package com.sun.tools.visualvm.core.model.dsdescr;

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.datasupport.Positionable;
import com.sun.tools.visualvm.core.model.Model;
import java.awt.Image;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
public abstract class DataSourceDescriptor<X extends DataSource> extends Model implements Positionable {
    
    public static final String PROPERTY_ICON = "prop_icon";
    
    public static final String PROPERTY_NAME = "prop_name";
    
    public static final String PROPERTY_DESCRIPTION = "prop_description";
    
    public static final String PROPERTY_PREFERRED_POSITION = "prop_preferred_position";
    
    public static final String PROPERTY_EXPANSION_POLICY = "prop_expansion_policy";
    
    public static final int EXPAND_NEVER = 0;
    
    public static final int EXPAND_ON_FIRST_CHILD = 1;
    
    public static final int EXPAND_ON_EACH_FIRST_CHILD = 2;
    
    public static final int EXPAND_ON_EACH_NEW_CHILD = 3;
    
    public static final int EXPAND_ON_EACH_CHILD_CHANGE = 4;
    
    private X dataSource;
    private Image icon;
    private String name;
    private String description;
    private int preferredPosition;
    private int autoExpansionPolicy;
    private final PropertyChangeSupport changeSupport;
    
    
    public DataSourceDescriptor() {
        this(null);
    }
    
    public DataSourceDescriptor(X dataSource) {
        this(dataSource, dataSource != null ? dataSource.toString() : null, null, null, POSITION_AT_THE_END, EXPAND_ON_FIRST_CHILD);
    }
    
    public DataSourceDescriptor(X ds, String n, String desc, Image ic, int pos, int aep) {   
        dataSource = ds;
        changeSupport = dataSource != null ? new PropertyChangeSupport(dataSource) : null;
        name = n;
        description = desc;
        icon = ic;
        preferredPosition = pos;
        autoExpansionPolicy = aep;
    }
    

    public Image getIcon() {
        return icon;
    }
    
    public boolean supportsRename() {
        return false;
    }
    
    public void setName(String newName) {
        if (!supportsRename()) throw new UnsupportedOperationException("Rename not supported for this descriptor");
        if (newName == null) throw new IllegalArgumentException("Name cannot be null");
        String oldName = name;
        name = newName;
        getDataSource().getStorage().setCustomProperties(new String[] { PROPERTY_NAME }, new String[] { newName });
        if (getChangeSupport() != null) getChangeSupport().firePropertyChange(PROPERTY_NAME, oldName, newName);
    }

    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }

    public int getPreferredPosition() {
        return preferredPosition;
    }
    
    public int getAutoExpansionPolicy() {
        return autoExpansionPolicy;
    }
    
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (getChangeSupport() != null) getChangeSupport().addPropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        if (getChangeSupport() != null) getChangeSupport().addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        if (getChangeSupport() != null) getChangeSupport().removePropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        if (getChangeSupport() != null) getChangeSupport().removePropertyChangeListener(propertyName, listener);
    }
    
    
    protected X getDataSource() {
        return dataSource;
    }
    
    protected void setDescription(String newDescription) {
        if (description == null && newDescription == null) return;
        String oldDescription = description;
        description = newDescription;
        if (getChangeSupport() != null) getChangeSupport().firePropertyChange(PROPERTY_DESCRIPTION, oldDescription, newDescription);
    }
    
    protected void setIcon(Image newIcon) {
        if (icon == null && newIcon == null) return;
        Image oldIcon = icon;
        icon = newIcon;
        if (getChangeSupport() != null) getChangeSupport().firePropertyChange(PROPERTY_ICON, oldIcon, newIcon);
    }
    
    protected void setPreferredPosition(int newPosition) {
        int oldPosition = preferredPosition;
        preferredPosition = newPosition;
        if (getChangeSupport() != null) getChangeSupport().firePropertyChange(PROPERTY_PREFERRED_POSITION, oldPosition, newPosition);
    }
    
    protected void getAutoExpansionPolicy(int newPolicy) {
        int oldPolicy = autoExpansionPolicy;
        autoExpansionPolicy = newPolicy;
        if (getChangeSupport() != null) getChangeSupport().firePropertyChange(PROPERTY_EXPANSION_POLICY, oldPolicy, newPolicy);
    }
    
    protected PropertyChangeSupport getChangeSupport() {
        return changeSupport;
    }

}
