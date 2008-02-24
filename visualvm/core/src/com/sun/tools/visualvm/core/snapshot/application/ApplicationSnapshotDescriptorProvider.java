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

package com.sun.tools.visualvm.core.snapshot.application;

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.model.AbstractModelProvider;
import com.sun.tools.visualvm.core.model.dsdescr.DataSourceDescriptor;
import com.sun.tools.visualvm.core.snapshot.AbstractSnapshotDescriptor;
import com.sun.tools.visualvm.core.snapshot.SnapshotsContainer;
import java.awt.Image;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Properties;
import org.openide.util.Utilities;

/**
 *
 * @author Jiri Sedlacek
 */
class ApplicationSnapshotDescriptorProvider extends AbstractModelProvider<DataSourceDescriptor, DataSource> {
    
    ApplicationSnapshotDescriptorProvider() {
    }
    
    public DataSourceDescriptor createModelFor(DataSource ds) {
        if (SnapshotsContainer.sharedInstance().equals(ds)) {
            return new SnapshotsContainerDescriptor();
        }
        if (ds instanceof ApplicationSnapshot) {
            return ApplicationSnapshotDescriptor.newInstance((ApplicationSnapshot)ds);
        }
        return null;
    }
    
    private static class ApplicationSnapshotDescriptor extends AbstractSnapshotDescriptor implements PropertyChangeListener {
        private static final Image NODE_ICON = Utilities.loadImage("com/sun/tools/visualvm/core/ui/resources/application.png", true);
        private static final Image NODE_BADGE = Utilities.loadImage("com/sun/tools/visualvm/core/ui/resources/snapshotBadge.png", true);
        
        static ApplicationSnapshotDescriptor newInstance(ApplicationSnapshot snapshot) {
            ApplicationSnapshotDescriptor desc = new ApplicationSnapshotDescriptor(snapshot);
            return desc; 
        }
        
        private ApplicationSnapshotDescriptor(ApplicationSnapshot snapshot) {
            super(snapshot, NODE_ICON);
            
            String name = super.getName();
            Image icon = super.getIcon();
            
            Properties properties = ApplicationSnapshotsSupport.loadProperties(snapshot.getFile());
            if (properties != null) {
                // Load display name
                String displayName = properties.getProperty(ApplicationSnapshotsSupport.DISPLAY_NAME);
                if (displayName != null) name = displayName;
                
                // Load icon
                String iconFile = properties.getProperty(ApplicationSnapshotsSupport.DISPLAY_ICON);
                if (iconFile != null) {
                    Image image = ApplicationSnapshotsSupport.loadImage(new File(snapshot.getFile(), iconFile));
                    if (image != null) icon = image;
                }
            }
            
            setName(name);
            setIcon(Utilities.mergeImages(icon, NODE_BADGE, 0, 0));
        }

        public void propertyChange(PropertyChangeEvent evt) {
            setName((String)evt.getNewValue());
        }
        
    }
    
    private static class SnapshotsContainerDescriptor extends DataSourceDescriptor {
        private static final Image NODE_ICON = Utilities.loadImage("com/sun/tools/visualvm/core/ui/resources/snapshots.png", true);
        
        public Image getIcon() {
            return NODE_ICON;
        }
        
        public String getName() {
            return "Snapshots";
        }
        
        public String getDescription() {
            return null;
        }
        
        public int getPreferredPosition() {
            return 30;
        }
        
        public int getAutoExpansionPolicy() {
            return EXPAND_NEVER;
        }
        
    }
}
