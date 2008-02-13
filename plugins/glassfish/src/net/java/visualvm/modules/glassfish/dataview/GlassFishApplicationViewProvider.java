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
import com.sun.appserv.management.monitor.ConnectionQueueMonitor;
import com.sun.appserv.management.monitor.FileCacheMonitor;
import com.sun.appserv.management.monitor.HTTPServiceMonitor;
import com.sun.appserv.management.monitor.KeepAliveMonitor;
import com.sun.appserv.management.monitor.ServerRootMonitor;
import com.sun.appserv.management.monitor.TransactionServiceMonitor;
import com.sun.appserv.management.monitor.statistics.ConnectionQueueStats;
import com.sun.appserv.management.monitor.statistics.FileCacheStats;
import com.sun.appserv.management.monitor.statistics.KeepAliveStats;
import com.sun.appserv.management.monitor.statistics.TransactionServiceStats;
import com.sun.tools.visualvm.core.datasource.Application;
import com.sun.tools.visualvm.core.model.apptype.ApplicationTypeFactory;
import com.sun.tools.visualvm.core.model.jmx.JmxModel;
import com.sun.tools.visualvm.core.model.jmx.JmxModelFactory;
import com.sun.tools.visualvm.core.model.jvm.JVM;
import com.sun.tools.visualvm.core.model.jvm.JVMFactory;
import com.sun.tools.visualvm.core.ui.DataSourceWindowFactory;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.DataSourceViewsProvider;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import com.sun.tools.visualvm.core.scheduler.Quantum;
import com.sun.tools.visualvm.core.scheduler.ScheduledTask;
import com.sun.tools.visualvm.core.scheduler.Scheduler;
import com.sun.tools.visualvm.core.scheduler.SchedulerTask;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import net.java.visualvm.modules.glassfish.GlassFishApplicationType;
import org.openide.util.Utilities;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import net.java.visualvm.modules.glassfish.jmx.AMXUtil;
import net.java.visualvm.modules.glassfish.jmx.JMXUtil;
import net.java.visualvm.modules.glassfish.ui.ConnectionQueuePanel;
import net.java.visualvm.modules.glassfish.ui.FileCachePanel;
import net.java.visualvm.modules.glassfish.ui.KeepAlivePanel;
import net.java.visualvm.modules.glassfish.ui.TransactionsPanel;


/**
 *
 * @author Jaroslav Bachorik
 */
public class GlassFishApplicationViewProvider implements DataSourceViewsProvider<Application> {

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    @Override
    public Set<?extends DataSourceView> getViews(final Application application) {
        JmxModel model = JmxModelFactory.getJmxModelFor(application);

        if (model != null) {
            try {
                DomainRoot dr = AMXUtil.getDomainRoot(model.getMBeanServerConnection());

                if (dr != null) {
                    dr.waitAMXReady();

                    final Map<String, ServerRootMonitor> serverMonitors = dr.getMonitoringRoot().getServerRootMonitorMap();
                    JVM jvm = JVMFactory.getJVMFor(application);
                    final String serverName = JMXUtil.getServerName(model);

                    if (serverMonitors.get(serverName) == null) {
                        return Collections.EMPTY_SET;
                    }

                    return new HashSet<DataSourceView>() {

                            {
                                HTTPServiceMonitor httpMonitor = serverMonitors.get(serverName).getHTTPServiceMonitor();
                                TransactionServiceMonitor transMonitor = serverMonitors.get(serverName)
                                                                                       .getTransactionServiceMonitor();

                                if (httpMonitor != null) {
                                    add(new HTTPServiceView(application, httpMonitor));
                                }

                                if (transMonitor != null) {
                                    add(new TransactionServiceView(application, transMonitor));
                                }
                            }
                        };
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return Collections.EMPTY_SET;
    }

    public void initialize() {
        DataSourceWindowFactory.sharedInstance().addViewProvider(this, Application.class);
    }

    public boolean supportsViewFor(Application dataSource) {
        return (ApplicationTypeFactory.getApplicationTypeFor(dataSource) instanceof GlassFishApplicationType);
    }
}
