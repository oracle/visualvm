/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
package net.java.visualvm.modules.glassfish.datasource;

import com.sun.appserv.management.monitor.ServletMonitor;
import org.graalvm.visualvm.core.datasource.DataSourceRepository;
import org.graalvm.visualvm.core.datasupport.DataChangeEvent;
import org.graalvm.visualvm.core.datasupport.DataChangeListener;
import org.graalvm.visualvm.core.datasupport.DataRemovedListener;
import java.util.Map;
import java.util.Set;


/**
 *
 * @author Jaroslav Bachorik
 */
public class GlassFishServletProvider implements DataChangeListener<GlassFishWebModule>, DataRemovedListener<GlassFishWebModule> {
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
        DataSourceRepository.sharedInstance().addDataChangeListener(INSTANCE, GlassFishWebModule.class);
    }
    
    public static void shutdown() {
        DataSourceRepository.sharedInstance().removeDataChangeListener(INSTANCE);
    }
    
    public void dataRemoved(GlassFishWebModule module) {
        processFinishedModule(module);
    }

    private void processFinishedModule(GlassFishWebModule module) {
        // TODO: remove listener!!!
        Set<GlassFishServlet> monitoredServlets = module.getRepository().getDataSources(GlassFishServlet.class);
        module.getRepository().removeDataSources(monitoredServlets);
    }

    private void processNewWebModule(final GlassFishWebModule module) {
        for (Map.Entry<String, ServletMonitor> monitorEntry : module.getMonitor().getServletMonitorMap().entrySet()) {
            GlassFishServlet servlet = new GlassFishServlet(monitorEntry.getKey(), module, monitorEntry.getValue());
            module.getRepository().addDataSource(servlet);
        }

        module.notifyWhenRemoved(this);
    }
}
