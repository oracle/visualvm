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
import java.awt.Image;
import java.io.File;
import org.openide.util.Utilities;

/**
 *
 * @author Jiri Sedlacek
 */
class ApplicationSnapshotDescriptorProvider extends AbstractModelProvider<DataSourceDescriptor, DataSource> {
    
    ApplicationSnapshotDescriptorProvider() {
    }
    
    public DataSourceDescriptor createModelFor(DataSource ds) {
        if (ds instanceof ApplicationSnapshot) {
            return ApplicationSnapshotDescriptor.newInstance((ApplicationSnapshot)ds);
        }
        return null;
    }
    
    private static class ApplicationSnapshotDescriptor extends AbstractSnapshotDescriptor<ApplicationSnapshot> {
        private static final Image NODE_ICON = Utilities.loadImage("com/sun/tools/visualvm/core/ui/resources/application.png", true);
        private static final Image NODE_BADGE = Utilities.loadImage("com/sun/tools/visualvm/core/ui/resources/snapshotBadge.png", true);
        
        static ApplicationSnapshotDescriptor newInstance(ApplicationSnapshot snapshot) {
            ApplicationSnapshotDescriptor desc = new ApplicationSnapshotDescriptor(snapshot);
            
            String name = desc.getName();
            Image icon = desc.getIcon();
            
            Object[] properties = snapshot.getProperties(new String[] { ApplicationSnapshot.DISPLAY_NAME, ApplicationSnapshot.DISPLAY_ICON });
            if (properties[0] != null) name = (String)properties[0];
            if (properties[1] != null) {
                Image image = ApplicationSnapshotsSupport.loadImage(new File(snapshot.getFile(), (String)properties[1]));
                if (image != null) icon = image;
            }
            
            desc.setName(name);
            desc.setIcon(Utilities.mergeImages(icon, NODE_BADGE, 0, 0));
            
            return desc; 
        }
        
        private ApplicationSnapshotDescriptor(ApplicationSnapshot snapshot) {
            super(snapshot, NODE_ICON);
        }
        
        protected void setName(String newName) {
            super.setName(newName);
            getDataSource().setProperties(new String[] { ApplicationSnapshot.DISPLAY_NAME }, new Object[] { newName });
        }
        
        protected boolean supportsRename() {
            return true;
        }
        
    }
}
