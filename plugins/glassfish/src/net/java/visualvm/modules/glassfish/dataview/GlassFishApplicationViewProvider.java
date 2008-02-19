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
import com.sun.appserv.management.monitor.HTTPServiceMonitor;
import com.sun.appserv.management.monitor.ServerRootMonitor;
import com.sun.appserv.management.monitor.TransactionServiceMonitor;
import com.sun.tools.visualvm.core.datasource.Application;
import com.sun.tools.visualvm.core.model.apptype.ApplicationTypeFactory;
import com.sun.tools.visualvm.core.model.jmx.JmxModel;
import com.sun.tools.visualvm.core.model.jmx.JmxModelFactory;
import com.sun.tools.visualvm.core.ui.DataSourceWindowFactory;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.DataSourceViewsProvider;
import net.java.visualvm.modules.glassfish.GlassFishApplicationType;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.java.visualvm.modules.glassfish.datasource.GlassFishApplication;
import net.java.visualvm.modules.glassfish.jmx.AMXUtil;
import net.java.visualvm.modules.glassfish.jmx.JMXUtil;

/**
 *
 * @author Jaroslav Bachorik
 */
public class GlassFishApplicationViewProvider implements DataSourceViewsProvider<Application> {

    private final static GlassFishApplicationViewProvider INSTANCE = new GlassFishApplicationViewProvider();

    private GlassFishApplicationViewProvider() {
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------
    @Override
    public Set<? extends DataSourceView> getViews(final Application application) {
        if (!(application instanceof GlassFishApplication)) {
            return Collections.EMPTY_SET;
        }

        JmxModel model = JmxModelFactory.getJmxModelFor(application);
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
                    HTTPServiceMonitor httpMonitor = serverMonitors.get(serverName).getHTTPServiceMonitor();
                    TransactionServiceMonitor transMonitor = serverMonitors.get(serverName).getTransactionServiceMonitor();

                    if (httpMonitor != null) {
                        add(new HTTPServiceView(application, httpMonitor));
                    }

                    if (transMonitor != null) {
                        add(new TransactionServiceView(application, transMonitor));
                    }
                }
                };
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Collections.EMPTY_SET;
    }

    public static void initialize() {
        DataSourceWindowFactory.sharedInstance().addViewProvider(INSTANCE, Application.class);
    }

    public static void shutdown() {
        DataSourceWindowFactory.sharedInstance().removeViewProvider(INSTANCE);
    }

    public boolean supportsViewFor(Application dataSource) {
        return (ApplicationTypeFactory.getApplicationTypeFor(dataSource) instanceof GlassFishApplicationType);
    }
}
