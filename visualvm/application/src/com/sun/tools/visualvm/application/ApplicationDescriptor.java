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

package com.sun.tools.visualvm.application;

import com.sun.tools.visualvm.application.type.ApplicationType;
import com.sun.tools.visualvm.application.type.ApplicationTypeFactory;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import java.awt.Image;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * DataSourceDescriptor for Application.
 *
 * @author Jiri Sedlacek
 */
 public class ApplicationDescriptor extends DataSourceDescriptor<Application> {
     
    private String name;

    protected ApplicationDescriptor(Application application) {
        this(application, ApplicationTypeFactory.getApplicationTypeFor(application));
    }

    private ApplicationDescriptor(final Application application, final ApplicationType type) {
        super(application, resolveName(application, type), type.getDescription(),
                type.getIcon(), POSITION_AT_THE_END, EXPAND_ON_FIRST_CHILD);
        name = super.getName();
        type.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                if (ApplicationType.PROPERTY_NAME.equals(propertyName)) {
                    // Name already customized by the user, do not change it
                    if (getPersistedName(application) != null) return;
                    
                    if (supportsRename()) {
                        // Descriptor supports renaming, use setName(), sync name
                        setName((String)evt.getNewValue());
                        name = ApplicationDescriptor.super.getName();
                    } else {
                        // Descriptor doesn't support renaming, set name for overriden getName()
                        String oldName = name;
                        name = createGenericName(application, type.getName());
                        PropertyChangeSupport pcs = ApplicationDescriptor.this.getChangeSupport();
                        pcs.firePropertyChange(PROPERTY_NAME, oldName, name);
                    }
                } else if (ApplicationType.PROPERTY_ICON.equals(propertyName)) {
                    setIcon((Image)evt.getNewValue());
                } else if (ApplicationType.PROPERTY_DESCRIPTION.equals(propertyName)) {
                    setDescription((String)evt.getNewValue());
                } else if (ApplicationType.PROPERTY_VERSION.equals(propertyName)) {
                    // Not supported by ApplicationDescriptor
                }
            }
        });
    }
    
    public String getName() {
        if (supportsRename()) return super.getName();
        else return name;
    }

    public boolean providesProperties() {
        return true;
    }

    private static String resolveName(Application application, ApplicationType type) {
        // Check for persisted displayname (currently only for JmxApplications)
        String persistedName = getPersistedName(application);
        if (persistedName != null) return persistedName;

        // Provide generic displayname
        return createGenericName(application, type.getName());
    }
    
    private static String createGenericName(Application application, String nameBase) {
        int pid = application.getPid();
        String id = Application.CURRENT_APPLICATION.getPid() == pid ||
        pid == Application.UNKNOWN_PID ? "" : " (pid " + pid + ")"; // NOI18N
        return nameBase + id;
    }
    
    private static String getPersistedName(Application application) {
        return application.getStorage().getCustomProperty(PROPERTY_NAME);
    }
    
}
