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

package org.graalvm.visualvm.application;

import org.graalvm.visualvm.application.jvm.Jvm;
import org.graalvm.visualvm.application.jvm.JvmFactory;
import org.graalvm.visualvm.application.type.ApplicationType;
import org.graalvm.visualvm.application.type.ApplicationTypeFactory;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptor;
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
    
    private static final String DISPLAY_NAME_PROPERTY = "-Dvisualvm.display.name="; // NOI18N
     
    private String name;

    /**
     * Creates new instance of Application Descriptor.
     *
     * @param application Application described by the descriptor
     */
    protected ApplicationDescriptor(Application application) {
        this(application, ApplicationTypeFactory.getApplicationTypeFor(
             application), POSITION_AT_THE_END);
    }

    /**
     * Creates new instance of Application Descriptor.
     *
     * @param application Application described by the descriptor
     * @param preferredPosition preferred position of the Application
     *
     * @since VisualVM 1.3
     */
    protected ApplicationDescriptor(Application application, int preferredPosition) {
        this(application, ApplicationTypeFactory.getApplicationTypeFor(application),
             preferredPosition);
    }

    private ApplicationDescriptor(final Application application, final ApplicationType type,
                                  int preferredPosition) {
        super(application, resolveApplicationName(application, type), type.getDescription(),
              type.getIcon(), preferredPosition, EXPAND_ON_FIRST_CHILD);
        name = super.getName();
        type.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                if (ApplicationType.PROPERTY_NAME.equals(propertyName)) {
                    // Name already customized by the user, do not change it
                    if (resolveName(application, null) != null) return;
                    
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

    /**
     * Returns Application name if available in Snapshot Storage as PROPERTY_NAME
     * or user-provided display name defined by JVM argument <code>-Dvisualvm.display.name</code>
     * (since VisualVM 1.3.4) or generates new name using the provided ApplicationType.
     *
     * @param application Application for which to resolve the name
     * @param type ApplicationType to be used for generating Application name
     * @return persisted Application name if available or new generated name
     *
     * @since VisualVM 1.3
     */
    protected static String resolveApplicationName(Application application, ApplicationType type) {
        // Check for persisted displayname (currently only for JmxApplications)
        String persistedName = resolveName(application, null);
        if (persistedName != null) return persistedName;
        
        // Check for custom name defined by -Dvisualvm.display.name
        String customName = resolveCustomName(application);
        if (customName != null) return customName;

        // Provide generic displayname
        return createGenericName(application, type.getName());
    }
    
    private static String resolveCustomName(Application application) {
        Jvm jvm = JvmFactory.getJVMFor(application);
        if (jvm.isBasicInfoSupported()) {
            String args = jvm.getJvmArgs();
            int propIndex = args.indexOf(DISPLAY_NAME_PROPERTY);

            if (propIndex != -1) {  // display name propery detected on commandline
                propIndex += DISPLAY_NAME_PROPERTY.length();
                int endIndex = args.indexOf(" ", propIndex); // NOI18N
                String name;
                if (endIndex == -1) name = args.substring(propIndex);
                else name = args.substring(propIndex, endIndex);
                return buildNameWithPid(application, name);
            }
        }
        return null;
    }
    
    private static String createGenericName(Application application, String nameBase) {
        return buildNameWithPid(application, nameBase);
    }

    private static String buildNameWithPid(Application application, String name) {
        int pid = application.getPid();
        String id = Application.CURRENT_APPLICATION.getPid() == pid ||
        pid == Application.UNKNOWN_PID ? "" : " (pid " + pid + ")"; // NOI18N
        return name + id;
    }
    
}
