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

import com.sun.appserv.management.j2ee.J2EETypes;
import com.sun.appserv.management.monitor.ServerRootMonitor;
import com.sun.appserv.management.monitor.WebModuleVirtualServerMonitor;
import com.sun.tools.visualvm.core.datasource.DataSourceRepository;
import com.sun.tools.visualvm.core.datasource.DefaultDataSourceProvider;
import com.sun.tools.visualvm.core.datasupport.DataChangeEvent;
import com.sun.tools.visualvm.core.datasupport.DataChangeListener;
import com.sun.tools.visualvm.core.datasupport.DataFinishedListener;
import com.sun.tools.visualvm.core.model.jmx.JmxModel;
import com.sun.tools.visualvm.core.model.jmx.JmxModelFactory;
import java.util.Map;
import java.util.Set;
import net.java.visualvm.modules.glassfish.jmx.JMXUtil;

/**
 *
 * @author Jaroslav Bachorik
 */
public class GlassFishApplicationProvider extends DefaultDataSourceProvider<GlassFishApplication> implements DataChangeListener<GlassFishModel>, DataFinishedListener<GlassFishModel> {
    private static final GlassFishApplicationProvider INSTANCE = new GlassFishApplicationProvider();
        
    private GlassFishApplicationProvider() {}
    
    //~ Methods ------------------------------------------------------------------------------------------------------------------
    public void dataChanged(DataChangeEvent<GlassFishModel> event) {
        if (event.getAdded().isEmpty() && event.getRemoved().isEmpty()) {
            // Initial event to deliver DataSources already created by the provider before registering to it as a listener
            // NOTE: already existing hosts are treated as new for this provider
            Set<GlassFishModel> newApplications = event.getCurrent();

            for (GlassFishModel app : newApplications) {
                processNewApplication(app);
            }
        } else {
            // Real delta event
            Set<GlassFishModel> newApplications = event.getAdded();

            for (GlassFishModel app : newApplications) {
                processNewApplication(app);
            }
        }
    }

    public static void initialize() {
        DataSourceRepository.sharedInstance().addDataSourceProvider(INSTANCE);
        DataSourceRepository.sharedInstance().addDataChangeListener(INSTANCE, GlassFishModel.class);
    }

    public static void shutdown() {
        DataSourceRepository.sharedInstance().removeDataSourceProvider(INSTANCE);
        DataSourceRepository.sharedInstance().removeDataChangeListener(INSTANCE);
    }
    
    public void dataFinished(GlassFishModel root) {
        processFinishedApplication(root);
    }

    private void processFinishedApplication(GlassFishModel app) {
        // TODO: remove listener!!!
        Set<GlassFishApplication> monitoredDeployables = app.getRepository().getDataSources(GlassFishApplication.class);
        app.getRepository().removeDataSources(monitoredDeployables);
        unregisterDataSources(monitoredDeployables);

    //        for (GlassfishServlet deployable : monitoredDeployables) deployable.removed();
    }

    private void processNewApplication(final GlassFishModel root) {
        JmxModel jmx = JmxModelFactory.getJmxModelFor(root.getApplication());

        String serverName = JMXUtil.getServerName(jmx);
        ServerRootMonitor srm = root.getDomainRoot().getMonitoringRoot().getServerRootMonitorMap().get(serverName);
        
        for (Map.Entry<String, WebModuleVirtualServerMonitor> virtMonitorEntry : srm.getWebModuleVirtualServerMonitorMap().entrySet()) {
            String objectName = JMXUtil.getObjectName(J2EETypes.WEB_MODULE, virtMonitorEntry.getKey(), jmx);
            String moduleName = JMXUtil.getWebModuleName(objectName, jmx);
            String appName = JMXUtil.getJ2EEAppName(objectName);

            if (moduleName == null || moduleName.length() == 0) {
                continue;
            }

            GlassFishWebModule module = new GlassFishWebModule(appName != null ? (moduleName + " (in " + appName + ")") : moduleName, objectName, virtMonitorEntry.getValue(), root);
            registerDataSource(module);
            root.getRepository().addDataSource(module);
        }

        root.notifyWhenFinished(this);
    }
}
