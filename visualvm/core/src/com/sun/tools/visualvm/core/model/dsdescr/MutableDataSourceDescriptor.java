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
import java.awt.Image;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
public abstract class MutableDataSourceDescriptor extends DataSourceDescriptor {
    
    private Image icon;
    private String name;
    private String description;
    private int preferredPosition;
    private final PropertyChangeSupport changeSupport;
    
    
    public MutableDataSourceDescriptor(DataSource dataSource) {
        this(dataSource, dataSource.toString(), null, null, POSITION_AT_THE_END);
    }
    
    public MutableDataSourceDescriptor(DataSource dataSource, String n, String desc, Image ic, int pos) {
        super();        
        if (dataSource == null) throw new IllegalArgumentException("DataSource cannot be null");
        changeSupport = new PropertyChangeSupport(dataSource);
        name = n;
        description = desc;
        icon = ic;
        preferredPosition = pos;
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
        String oldName = name;
        name = newName;
        changeSupport.firePropertyChange(PROPERTY_NAME, oldName, newName);
    }
    
    protected void setDescription(String newDescription) {
        String oldDescription = description;
        description = newDescription;
        changeSupport.firePropertyChange(PROPERTY_DESCRIPTION, oldDescription, newDescription);
    }
    
    protected void setIcon(Image newIcon) {
        Image oldIcon = icon;
        icon = newIcon;
        changeSupport.firePropertyChange(PROPERTY_ICON, oldIcon, newIcon);
    }
    
    protected void setPreferredPosition(int newPosition) {
        int oldPosition = preferredPosition;
        preferredPosition = newPosition;
        changeSupport.firePropertyChange(PROPERTY_PREFERRED_POSITION, oldPosition, newPosition);
    }

}
