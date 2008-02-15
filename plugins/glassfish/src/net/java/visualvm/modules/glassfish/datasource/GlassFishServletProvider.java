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
import com.sun.tools.visualvm.core.datasource.DataSourceRepository;
import com.sun.tools.visualvm.core.datasource.DefaultDataSourceProvider;
import com.sun.tools.visualvm.core.datasupport.DataChangeEvent;
import com.sun.tools.visualvm.core.datasupport.DataChangeListener;
import com.sun.tools.visualvm.core.datasupport.DataFinishedListener;
import java.util.Map;
import java.util.Set;


/**
 *
 * @author Jaroslav Bachorik
 */
public class GlassFishServletProvider extends DefaultDataSourceProvider<GlassFishServlet> implements DataChangeListener<GlassFishWebModule> {
    private final static GlassFishServletProvider INSTANCE = new GlassFishServletProvider();
    
    private GlassFishServletProvider() {}
    
    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void dataChanged(DataChangeEvent<GlassFishWebModule> event) {
        if (event.getAdded().isEmpty() && event.getRemoved().isEmpty()) {
            // Initial event to deliver DataSources already created by the provider before registering to it as a listener
            // NOTE: already existing hosts are treated as new for this provider
            Set<GlassFishWebModule> newModules = event.getCurrent();

            for (GlassFishWebModule module : newModules) {
                processNewWebModule(module);
            }
        } else {
            // Real delta event
            Set<GlassFishWebModule> newModules = event.getAdded();

            for (GlassFishWebModule module : newModules) {
                processNewWebModule(module);
            }
        }
    }

    public static void initialize() {
        DataSourceRepository.sharedInstance().addDataSourceProvider(INSTANCE);
        DataSourceRepository.sharedInstance().addDataChangeListener(INSTANCE, GlassFishWebModule.class);
    }
    
    public static void shutdown() {
        DataSourceRepository.sharedInstance().removeDataSourceProvider(INSTANCE);
        DataSourceRepository.sharedInstance().removeDataChangeListener(INSTANCE);
    }

    private void processFinishedModule(GlassFishWebModule module) {
        // TODO: remove listener!!!
        Set<GlassFishServlet> monitoredServlets = module.getRepository().getDataSources(GlassFishServlet.class);
        module.getRepository().removeDataSources(monitoredServlets);
        unregisterDataSources(monitoredServlets);
    }

    private void processNewWebModule(final GlassFishWebModule module) {
        for (Map.Entry<String, ServletMonitor> monitorEntry : module.getMonitor().getServletMonitorMap().entrySet()) {
            GlassFishServlet servlet = new GlassFishServlet(monitorEntry.getKey(), module, monitorEntry.getValue());
            registerDataSource(servlet);
            module.getRepository().addDataSource(servlet);
        }

        module.notifyWhenFinished(new DataFinishedListener() {
                public void dataFinished(Object dataSource) {
                    processFinishedModule(module);
                }
            });
    }
}
