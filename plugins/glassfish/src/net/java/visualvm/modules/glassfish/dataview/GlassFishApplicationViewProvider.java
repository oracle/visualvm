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
package net.java.visualvm.modules.glassfish.dataview;

import com.sun.appserv.management.DomainRoot;
import com.sun.appserv.management.config.ModuleMonitoringLevelValues;
import com.sun.appserv.management.config.ModuleMonitoringLevelsConfig;
import com.sun.appserv.management.monitor.HTTPServiceMonitor;
import com.sun.appserv.management.monitor.ServerRootMonitor;
import com.sun.appserv.management.monitor.TransactionServiceMonitor;
import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.application.type.ApplicationTypeFactory;
import org.graalvm.visualvm.core.snapshot.Snapshot;
import org.graalvm.visualvm.core.ui.DataSourceView;
import org.graalvm.visualvm.core.ui.DataSourceViewProvider;
import org.graalvm.visualvm.core.ui.DataSourceViewsManager;
import org.graalvm.visualvm.tools.jmx.JmxModel;
import org.graalvm.visualvm.tools.jmx.JmxModelFactory;
import net.java.visualvm.modules.glassfish.GlassFishApplicationType;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import net.java.visualvm.modules.glassfish.jmx.AMXUtil;
import net.java.visualvm.modules.glassfish.jmx.JMXUtil;

/**
 *
 * @author Jaroslav Bachorik
 */
public class GlassFishApplicationViewProvider extends DataSourceViewProvider<Application> {

    private final static GlassFishApplicationViewProvider INSTANCE = new GlassFishApplicationViewProvider();
    private final static Logger LOGGER = Logger.getLogger(GlassFishApplicationViewProvider.class.getName());
    private final Map<Application, HTTPServiceView> httpServiceViewMap = new HashMap<Application, HTTPServiceView>();
    private final Map<Application, TransactionServiceView> transServiceViewMap = new HashMap<Application, TransactionServiceView>();

    private GlassFishApplicationViewProvider() {
    }

    @Override
    protected DataSourceView createView(Application app) {
        final JmxModel model = JmxModelFactory.getJmxModelFor(app);
        if (model == null) {
            return null;
        }

        DomainRoot dr = AMXUtil.getDomainRoot(model);
        if (dr == null) {
            return null;
        }

        final Map<String, ServerRootMonitor> serverMonitors = dr.getMonitoringRoot().getServerRootMonitorMap();
        final String serverName = JMXUtil.getServerName(model);

        if (serverMonitors.get(serverName) == null) {
            return null;
        }

        HTTPServiceMonitor httpMonitor = serverMonitors.get(serverName).getHTTPServiceMonitor();
        ModuleMonitoringLevelsConfig monitorConfig = AMXUtil.getMonitoringConfig(model);
        if (!monitorConfig.getHTTPService().equals(ModuleMonitoringLevelValues.OFF)) {
            if (httpMonitor != null) {
                return getHTTPServiceView(app, httpMonitor);
            }
        }
        return null;
    }

    @Override
    protected boolean supportsViewFor(Application app) {
        if (!(ApplicationTypeFactory.getApplicationTypeFor(app) instanceof GlassFishApplicationType)) return false;
        
        final JmxModel model = JmxModelFactory.getJmxModelFor(app);
        if (model == null) {
            return false;
        }

        DomainRoot dr = AMXUtil.getDomainRoot(model);
        if (dr == null) {
            return false;
        }

        final Map<String, ServerRootMonitor> serverMonitors = dr.getMonitoringRoot().getServerRootMonitorMap();
        final String serverName = JMXUtil.getServerName(model);

        if (serverMonitors.get(serverName) == null) {
            return false;
        }

        HTTPServiceMonitor httpMonitor = serverMonitors.get(serverName).getHTTPServiceMonitor();
        ModuleMonitoringLevelsConfig monitorConfig = AMXUtil.getMonitoringConfig(model);
        if (!monitorConfig.getHTTPService().equals(ModuleMonitoringLevelValues.OFF)) {
            return httpMonitor != null;
        }
        return false;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------
//    @Override
//    public Set<? extends DataSourceView> getViews(final Application application) {
//        ApplicationType at = ApplicationTypeFactory.getApplicationTypeFor(application);
//        if (!(at instanceof GlassFishApplicationType)) {
//            return Collections.EMPTY_SET;
//        }
//
//        final JmxModel model = JmxModelFactory.getJmxModelFor(application);
//        if (model == null) {
//            return Collections.EMPTY_SET;
//        }
//
//        DomainRoot dr = AMXUtil.getDomainRoot(model);
//        if (dr == null) {
//            return Collections.EMPTY_SET;
//        }
//
//        try {
//            final Map<String, ServerRootMonitor> serverMonitors = dr.getMonitoringRoot().getServerRootMonitorMap();
//            final String serverName = JMXUtil.getServerName(model);
//
//            if (serverMonitors.get(serverName) == null) {
//                return Collections.EMPTY_SET;
//            }
//
//            return new HashSet<DataSourceView>() {
//
//                {
//                    ModuleMonitoringLevelsConfig monitorConfig = AMXUtil.getMonitoringConfig(model);
//                    if (!monitorConfig.getHTTPService().equals(ModuleMonitoringLevelValues.OFF)) {
//                        HTTPServiceMonitor httpMonitor = serverMonitors.get(serverName).getHTTPServiceMonitor();
//                        if (httpMonitor != null) {
//                            add(getHTTPServiceView(application, httpMonitor));
//                        }
//                    }
//                    if (!monitorConfig.getHTTPService().equals(ModuleMonitoringLevelValues.OFF)) {
//                        TransactionServiceMonitor transMonitor = serverMonitors.get(serverName).getTransactionServiceMonitor();
//                        if (transMonitor != null) {
//                            add(getTransactionServiceView(application, transMonitor));
//                        }
//                    }
//                }
//            };
//        } catch (Exception e) {
//            LOGGER.throwing(GlassFishApplicationViewProvider.class.getName(), "getViews", e);
//        }
//
//        return Collections.EMPTY_SET;
//    }

    private HTTPServiceView getHTTPServiceView(Application app, HTTPServiceMonitor monitor) {
        synchronized (httpServiceViewMap) {
            if (httpServiceViewMap.containsKey(app)) {
                return httpServiceViewMap.get(app);
            } else {
                HTTPServiceView view = new HTTPServiceView(app, monitor);
                httpServiceViewMap.put(app, view);
                return view;
            }
        }
    }

    private TransactionServiceView getTransactionServiceView(Application app, TransactionServiceMonitor monitor) {
        synchronized (transServiceViewMap) {
            if (transServiceViewMap.containsKey(app)) {
                return transServiceViewMap.get(app);
            } else {
                TransactionServiceView view = new TransactionServiceView(app, monitor);
                transServiceViewMap.put(app, view);
                return view;
            }
        }
    }

    public static void initialize() {
        DataSourceViewsManager.sharedInstance().addViewProvider(INSTANCE, Application.class);
    }

    public static void shutdown() {
        DataSourceViewsManager.sharedInstance().removeViewProvider(INSTANCE);
        INSTANCE.httpServiceViewMap.clear();
        INSTANCE.transServiceViewMap.clear();
    }

    public boolean supportsViewsFor(Application dataSource) {
        return (ApplicationTypeFactory.getApplicationTypeFor(dataSource) instanceof GlassFishApplicationType);
    }

    public void saveViews(Application app, Snapshot snapshot) {
        // TODO implement later
    }

    public boolean supportsSaveViewsFor(Application app) {
        return false;
    }
}
