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

package com.sun.tools.visualvm.core.host;

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.datasource.Host;
import com.sun.tools.visualvm.core.model.AbstractModelProvider;
import com.sun.tools.visualvm.core.model.dsdescr.DataSourceDescriptor;
import com.sun.tools.visualvm.core.model.dsdescr.MutableDataSourceDescriptor;
import java.awt.Image;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import org.openide.util.Utilities;

/**
 *
 * @author Tomas Hurka
 */
class HostDescriptorFactory extends AbstractModelProvider<DataSourceDescriptor,DataSource> {
    
    HostDescriptorFactory() {
    }
    
    public DataSourceDescriptor createModelFor(DataSource ds) {
        if (RemoteHostsContainer.sharedInstance().equals(ds)) {
            return new HostsContainerDescriptor();
        }
        if (ds instanceof Host) {
            Host host = (Host) ds;
            if (Host.LOCALHOST.equals(ds)) {
                return new LocalHostDescriptor();
            }
            return HostDescriptor.newInstance(host);
        }
        return null;
    }
    
    private static class HostDescriptor extends MutableDataSourceDescriptor implements PropertyChangeListener {
        private static final Image NODE_ICON = Utilities.loadImage("com/sun/tools/visualvm/core/ui/resources/remoteHost.png", true);
        
        static HostDescriptor newInstance(Host host) {
            HostDescriptor desc = new HostDescriptor(host);
            host.addPropertyChangeListener(Host.PROPERTY_DISPLAYNAME,desc);
            desc.setName(host.getDisplayName());
            return desc; 
        }
        
        private HostDescriptor(Host host) {
            super(host,host.getDisplayName(),null,NODE_ICON,POSITION_AT_THE_END, EXPAND_ON_FIRST_CHILD);
        }

        public void propertyChange(PropertyChangeEvent evt) {
            setName((String)evt.getNewValue());
        }
        
    }
    
    private static class LocalHostDescriptor extends DataSourceDescriptor {
        private static final Image NODE_ICON = Utilities.loadImage("com/sun/tools/visualvm/core/ui/resources/localHost.png", true);

        LocalHostDescriptor() {
        }
        
        public String getName() {
            return Host.LOCALHOST.getDisplayName();
        }
        
        public Image getIcon() {
            return NODE_ICON;
        }
        
        public int getPreferredPosition() {
            return 0;
        }

        public String getDescription() {
            return null;
        }
        
    }
    
    private static class HostsContainerDescriptor extends DataSourceDescriptor {
        private static final Image NODE_ICON = Utilities.loadImage("com/sun/tools/visualvm/core/ui/resources/remoteHosts.png", true);
        
        public Image getIcon() {
            return NODE_ICON;
        }
        
        public String getName() {
            return "Remote";
        }
        
        public String getDescription() {
            return null;
        }
        
        public int getPreferredPosition() {
            return 10;
        }
        
        public int getAutoExpansionPolicy() {
            return EXPAND_ON_EACH_NEW_CHILD;
        }
        
    }
}
