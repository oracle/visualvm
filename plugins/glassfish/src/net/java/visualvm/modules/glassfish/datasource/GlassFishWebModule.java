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
import com.sun.tools.visualvm.core.datasource.Application;
import com.sun.tools.visualvm.core.datasource.DataSource;
import java.util.Map;

/**
 *
 * @author Jaroslav Bachorik
 */
public class GlassFishWebModule extends GlassFishApplication {
    private WebModuleVirtualServerMonitor monitor;
    
    public GlassFishWebModule(String name, WebModuleVirtualServerMonitor monitor, GlassFishRoot gfRoot) {
        super(name, monitor.getDomainRoot(), gfRoot);
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
}
