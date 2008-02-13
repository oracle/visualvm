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
import com.sun.tools.visualvm.core.explorer.ExplorerActionDescriptor;
import java.awt.Image;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Collections;
import java.util.Set;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class AbstractDataSourceDescriptor implements DataSourceDescriptor {
    
    private final PropertyChangeSupport changeSupport;
    
    private Image icon;
    private String name;
    private String description;
    private int preferredPosition;
    
    
    public AbstractDataSourceDescriptor(DataSource dataSource) {
        this(dataSource, dataSource.toString(), null, null, POSITION_AT_THE_END);
    }
    
    public AbstractDataSourceDescriptor(DataSource dataSource, String name, String description, Image icon, int preferredPosition) {
        if (dataSource == null) throw new IllegalArgumentException("DataSource cannot be null");
        
        changeSupport = new PropertyChangeSupport(dataSource);
        
        setName(name);
        setDescription(description);
        setIcon(icon);
        setPreferredPosition(preferredPosition);
    }
    

    public Image getIcon() {
        return icon;
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

    public ExplorerActionDescriptor getImplicitDefaultAction() {
        return null;
    }

    public Set<ExplorerActionDescriptor> getImplicitActions() {
        return Collections.EMPTY_SET;
    }

    
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(propertyName, listener);
    }
    
    
    protected void setName(String newName) {
        if (newName == null) throw new IllegalArgumentException("Name cannot be null");
        if (newName.equals(name)) return;
        String oldName = name;
        name = newName;
        changeSupport.firePropertyChange(PROPERTY_NAME, oldName, newName);
    }
    
    protected void setDescription(String newDescription) {
        if (description == null && newDescription == null) return;
        if ((description != null && description.equals(newDescription)) ||
                (newDescription != null && newDescription.equals(description))) return;
        String oldDescription = description;
        description = newDescription;
        changeSupport.firePropertyChange(PROPERTY_DESCRIPTION, oldDescription, newDescription);
    }
    
    protected void setIcon(Image newIcon) {
        if (icon == newIcon) return;
        Image oldIcon = icon;
        icon = newIcon;
        changeSupport.firePropertyChange(PROPERTY_ICON, oldIcon, newIcon);
    }
    
    protected void setPreferredPosition(int newPosition) {
        if (preferredPosition == newPosition) return;
        int oldPosition = preferredPosition;
        preferredPosition = newPosition;
        changeSupport.firePropertyChange(PROPERTY_ICON, oldPosition, newPosition);
    }

}
