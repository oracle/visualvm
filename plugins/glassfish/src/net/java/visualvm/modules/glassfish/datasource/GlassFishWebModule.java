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

package net.java.visualvm.modules.glassfish.datasource;

import com.sun.appserv.management.monitor.ServletMonitor;
import com.sun.appserv.management.monitor.WebModuleVirtualServerMonitor;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import java.awt.Image;
import java.util.Map;
import org.openide.util.Utilities;

/**
 *
 * @author Jaroslav Bachorik
 */
public class GlassFishWebModule extends GlassFishApplication {
    private static final Image NODE_ICON = Utilities.loadImage("net/java/visualvm/modules/glassfish/resources/application.png",
                                                                   true);
    private WebModuleVirtualServerMonitor monitor;
    
    private final DataSourceDescriptor descriptor = new DataSourceDescriptor(this) {

        @Override
        public Image getIcon() {
            return NODE_ICON;
        }

        @Override
        public String getName() {
            return GlassFishWebModule.this.getName();
        }

        @Override
        public String getDescription() {
            return null;
        }

        @Override
        public int getAutoExpansionPolicy() {
            return DataSourceDescriptor.EXPAND_NEVER;
        }
    };
    
    public GlassFishWebModule(String name, String objName, WebModuleVirtualServerMonitor monitor, GlassFishModel gfRoot) {
        super(name, objName, gfRoot);
        this.monitor = monitor;
        
    }

    public WebModuleVirtualServerMonitor getMonitor() {
        return monitor;
    }

    @Override
    public void generateContents() {
        for(Map.Entry<String, ServletMonitor> monitorEntry : monitor.getServletMonitorMap().entrySet()) {
            GlassFishServlet servlet = new GlassFishServlet(monitorEntry.getKey(), this, monitorEntry.getValue());
            getOwner().getRepository().addDataSource(servlet);
        }
    }

    @Override
    public DataSourceDescriptor getDescriptor() {
        return descriptor;
    }
}
