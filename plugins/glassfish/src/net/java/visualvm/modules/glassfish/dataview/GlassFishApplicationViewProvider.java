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
package net.java.visualvm.modules.glassfish.dataview;

import com.sun.appserv.management.DomainRoot;
import com.sun.appserv.management.config.ModuleMonitoringLevelValues;
import com.sun.appserv.management.config.ModuleMonitoringLevelsConfig;
import com.sun.appserv.management.monitor.HTTPServiceMonitor;
import com.sun.appserv.management.monitor.ServerRootMonitor;
import com.sun.appserv.management.monitor.TransactionServiceMonitor;
import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.application.type.ApplicationType;
import com.sun.tools.visualvm.application.type.ApplicationTypeFactory;
import com.sun.tools.visualvm.core.snapshot.Snapshot;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.DataSourceViewsManager;
import com.sun.tools.visualvm.core.ui.DataSourceViewsProvider;
import com.sun.tools.visualvm.tools.jmx.JmxModel;
import com.sun.tools.visualvm.tools.jmx.JmxModelFactory;
import net.java.visualvm.modules.glassfish.GlassFishApplicationType;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import net.java.visualvm.modules.glassfish.jmx.AMXUtil;
import net.java.visualvm.modules.glassfish.jmx.JMXUtil;

/**
 *
 * @author Jaroslav Bachorik
 */
public class GlassFishApplicationViewProvider implements DataSourceViewsProvider<Application> {
    
    private final static GlassFishApplicationViewProvider INSTANCE = new GlassFishApplicationViewProvider();
    private final static Logger LOGGER = Logger.getLogger(GlassFishApplicationViewProvider.class.getName());
    
    private final Map<Application, HTTPServiceView> httpServiceViewMap = new  HashMap<Application, HTTPServiceView>();
    private final Map<Application, TransactionServiceView> transServiceViewMap = new  HashMap<Application, TransactionServiceView>();
    
    private GlassFishApplicationViewProvider() {
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------
    @Override
    public Set<? extends DataSourceView> getViews(final Application application) {
        ApplicationType at = ApplicationTypeFactory.getApplicationTypeFor(application);
        if (!(at instanceof GlassFishApplicationType)) {
            return Collections.EMPTY_SET;
        }

        final JmxModel model = JmxModelFactory.getJmxModelFor(application);
        if (model == null) {
            return Collections.EMPTY_SET;
        }

        DomainRoot dr = AMXUtil.getDomainRoot(model);
        if (dr == null) {
            return Collections.EMPTY_SET;
        }

        try {
            final Map<String, ServerRootMonitor> serverMonitors = dr.getMonitoringRoot().getServerRootMonitorMap();
            final String serverName = JMXUtil.getServerName(model);

            if (serverMonitors.get(serverName) == null) {
                return Collections.EMPTY_SET;
            }

            return new HashSet<DataSourceView>() {

                {
                    ModuleMonitoringLevelsConfig monitorConfig = AMXUtil.getMonitoringConfig(model);
                    if (!monitorConfig.getHTTPService().equals(ModuleMonitoringLevelValues.OFF)) {
                        HTTPServiceMonitor httpMonitor = serverMonitors.get(serverName).getHTTPServiceMonitor();
                        if (httpMonitor != null) {
                            add(getHTTPServiceView(application, httpMonitor));
                        }
                    }
                    if (!monitorConfig.getHTTPService().equals(ModuleMonitoringLevelValues.OFF)) {
                        TransactionServiceMonitor transMonitor = serverMonitors.get(serverName).getTransactionServiceMonitor();
                        if (transMonitor != null) {
                            add(getTransactionServiceView(application, transMonitor));
                        }
                    }
                }
                };
        } catch (Exception e) {
            LOGGER.throwing(GlassFishApplicationViewProvider.class.getName(), "getViews", e);
        }

        return Collections.EMPTY_SET;
    }

    private HTTPServiceView getHTTPServiceView(Application app, HTTPServiceMonitor monitor) {
        synchronized(httpServiceViewMap) {
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
        synchronized(transServiceViewMap) {
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
        DataSourceViewsManager.sharedInstance().addViewsProvider(INSTANCE, Application.class);
    }

    public static void shutdown() {
        DataSourceViewsManager.sharedInstance().removeViewsProvider(INSTANCE);
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
