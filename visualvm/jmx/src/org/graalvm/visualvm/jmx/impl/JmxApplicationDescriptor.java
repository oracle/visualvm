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

package org.graalvm.visualvm.jmx.impl;

import org.graalvm.visualvm.application.ApplicationDescriptor;
import java.awt.Image;
import java.beans.PropertyChangeEvent;
import java.util.Objects;
import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.application.type.ApplicationType;
import org.graalvm.visualvm.application.type.ApplicationTypeFactory;
import org.graalvm.visualvm.application.type.DefaultApplicationType;
import org.graalvm.visualvm.core.datasource.DataSource;
import org.graalvm.visualvm.core.datasupport.Stateful;
import org.graalvm.visualvm.core.datasupport.Utils;
import org.openide.util.ImageUtilities;

/**
 *
 * @author Jiri Sedlacek
 */
public class JmxApplicationDescriptor extends ApplicationDescriptor {
     
    private static final Image NODE_BADGE = ImageUtilities.loadImage(
            "org/graalvm/visualvm/jmx/resources/jmxBadge.png", true); // NOI18N
     

    protected JmxApplicationDescriptor(JmxApplication application) {
        super(application, resolveApplicationType(application), resolvePosition(application, POSITION_AT_THE_END, true));
        
        application.addPropertyChangeListener(Stateful.PROPERTY_STATE, this);
    }

    public boolean supportsRename() {
        return true;
    }
    
    protected void setIcon(Image newIcon) {
        super.setIcon(newIcon);
        
        String iconString = Utils.imageToString(newIcon, "png");   // NOI18N
        getDataSource().getStorage().setCustomProperties(new String[] { PROPERTY_ICON }, new String[] { iconString });
    }
    
    public Image getIcon() {
        Image originalIcon = super.getIcon();
        return originalIcon == null ? null : ImageUtilities.mergeImages(
                                             originalIcon, NODE_BADGE, 0, 0);
    }
    
    
    private static ApplicationType resolveApplicationType(JmxApplication application) {
        return application.getState() == Stateful.STATE_AVAILABLE ?
               ApplicationTypeFactory.getApplicationTypeFor(application) :
               new DisconnectedJmxApplicationType(application);
    }
    
    
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        super.propertyChange(evt);
        
        if (Stateful.PROPERTY_STATE.equals(evt.getPropertyName()) && Objects.equals(evt.getNewValue(), Stateful.STATE_AVAILABLE)) {
            final Application application = getDataSource();
            DataSource.EVENT_QUEUE.post(new Runnable() {
                public void run() {
                    ApplicationType type = ApplicationTypeFactory.getApplicationTypeFor(application);
                    setApplicationType(type);
                    
                    String customName = application.getStorage().getCustomProperty(PROPERTY_NAME);
                    if (customName != null) {
                        setName(customName); // will reformat PID if needed
                    } else {
                        customName = resolveCustomName(application);
                        if (customName != null) setImplicitName(customName, null);
                        else setImplicitName(createGenericName(application, type.getName()), ApplicationType.PROPERTY_SUGGESTED_NAME);
                    }
                    
                    setDescription(type.getDescription());
                    setIcon(type.getIcon());
                }
            }, 500); // give the models some time to initialize to resolve the ApplicationType correctly
        }
    }
    
    
    private static final class DisconnectedJmxApplicationType extends DefaultApplicationType {
        
        DisconnectedJmxApplicationType(JmxApplication application) {
            super(application);
        }
     
        
        public String getDescription() {
            return "Unavailable application defined by a JMX connection.";
        }
        
    }
    
}
