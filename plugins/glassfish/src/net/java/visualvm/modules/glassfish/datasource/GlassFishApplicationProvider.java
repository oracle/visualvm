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

import com.sun.appserv.management.DomainRoot;
import com.sun.appserv.management.config.WebModuleConfig;
import com.sun.appserv.management.j2ee.J2EETypes;
import com.sun.appserv.management.monitor.ServerRootMonitor;
import com.sun.appserv.management.monitor.WebModuleVirtualServerMonitor;
import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.datasource.DataSourceRepository;
import com.sun.tools.visualvm.core.datasource.DefaultDataSourceProvider;
import com.sun.tools.visualvm.core.datasupport.DataChangeEvent;
import com.sun.tools.visualvm.core.datasupport.DataChangeListener;
import com.sun.tools.visualvm.core.datasupport.DataFinishedListener;
import com.sun.tools.visualvm.core.explorer.ExplorerExpansionListener;
import com.sun.tools.visualvm.core.explorer.ExplorerSupport;
import com.sun.tools.visualvm.core.model.dsdescr.DataSourceDescriptor;
import com.sun.tools.visualvm.core.model.jmx.JmxModel;
import com.sun.tools.visualvm.core.model.jmx.JmxModelFactory;
import com.sun.tools.visualvm.core.scheduler.Quantum;
import com.sun.tools.visualvm.core.scheduler.ScheduledTask;
import com.sun.tools.visualvm.core.scheduler.Scheduler;
import com.sun.tools.visualvm.core.scheduler.SchedulerTask;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import net.java.visualvm.modules.glassfish.jmx.AMXUtil;
import net.java.visualvm.modules.glassfish.jmx.JMXUtil;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;

/**
 *
 * @author Jaroslav Bachorik
 */
public class GlassFishApplicationProvider extends DefaultDataSourceProvider<GlassFishDataSource> implements DataChangeListener<GlassFishModel>, DataFinishedListener<GlassFishModel>, ExplorerExpansionListener {

    private static final GlassFishApplicationProvider INSTANCE = new GlassFishApplicationProvider();
    private final Map<GlassFishModel, ScheduledTask> taskMap = new HashMap<GlassFishModel, ScheduledTask>();

    private static class LazyLoadingSource extends GlassFishDataSource {

        @Override
        public DataSourceDescriptor getDescriptor() {
            return new DataSourceDescriptor(this) {

                @Override
                public int getAutoExpansionPolicy() {
                    return EXPAND_NEVER;
                }

                @Override
                public String getName() {
                    return "Please Wait ...";
                }
            };
        }
    }

    private class DiscoveryTask implements SchedulerTask {

        final private AtomicBoolean isProcessing = new AtomicBoolean(false);
        final private AtomicBoolean beenNotified = new AtomicBoolean(false);
        
        private GlassFishModel model;

        public DiscoveryTask(GlassFishModel model) {
            this.model = model;
        }

        public void onSchedule(long timeStamp) {
            if (!isProcessing.compareAndSet(false, true)) {
                return;
            }
            try {
                JmxModel jmx = JmxModelFactory.getJmxModelFor(model.getApplication());
                if ((jmx == null || jmx.getConnectionState() == JmxModel.ConnectionState.DISCONNECTED) && beenNotified.compareAndSet(false, true)){
                    NotifyDescriptor nd = new NotifyDescriptor.Message("Can not establish JMX connection", NotifyDescriptor.ERROR_MESSAGE);
                    DialogDisplayer.getDefault().notifyLater(nd);
                    model.setVisible(false);
                }
                if (jmx.getConnectionState() != JmxModel.ConnectionState.CONNECTED) {
                    return;
                }
                
                DomainRoot dr = AMXUtil.getDomainRoot(jmx);
                if (dr == null || !dr.getAMXReady()) {
                    return;
                }

                String serverName = JMXUtil.getServerName(jmx);
                if (serverName == null) {
                    return;
                }

                ServerRootMonitor srm = dr.getMonitoringRoot().getServerRootMonitorMap().get(serverName);

                Map<String, WebModuleConfig> map = dr.getDomainConfig().getWebModuleConfigMap();
                Map<String, String> contextRootMap = new HashMap<String, String>();

                for (Map.Entry<String, WebModuleConfig> cfgEntry : map.entrySet()) {
                    String contextRoot = cfgEntry.getValue().getContextRoot();
                    if (!contextRoot.startsWith("/")) {
                        contextRoot = "/" + contextRoot;
                    }
                    contextRootMap.put(contextRoot, cfgEntry.getKey());
                }
                Set<GlassFishApplication> currentApps = new HashSet<GlassFishApplication>();
                for (Map.Entry<String, WebModuleVirtualServerMonitor> virtMonitorEntry : srm.getWebModuleVirtualServerMonitorMap().entrySet()) {
                    String objectName = JMXUtil.getObjectName(J2EETypes.WEB_MODULE, virtMonitorEntry.getKey(), jmx);
                    String moduleName = JMXUtil.getWebModuleName(objectName, jmx, contextRootMap);
                    String appName = JMXUtil.getJ2EEAppName(objectName);

                    if (moduleName == null || moduleName.length() == 0) {
                        continue;
                    }
                    GlassFishWebModule webModule = new GlassFishWebModule(appName != null ? (moduleName + " (in " + appName + ")") : moduleName, objectName, virtMonitorEntry.getValue(), model);

                    currentApps.add(webModule);
                }

                Set<GlassFishApplication> toRemoveApps = new HashSet<GlassFishApplication>(model.getRepository().getDataSources());
                Set<GlassFishApplication> toAdd = new HashSet<GlassFishApplication>(currentApps);
                toRemoveApps.removeAll(currentApps);
                toAdd.removeAll(model.getRepository().getDataSources());

                Set<LazyLoadingSource> lazy = model.getRepository().getDataSources(LazyLoadingSource.class);
                if (toAdd.size() == 0 && lazy.size() > 0) {
                    return;
                }
                unregisterDataSources(lazy);
                Set<GlassFishDataSource> toRemove = new HashSet<GlassFishDataSource>(toRemoveApps);
                toRemove.addAll(lazy);

                unregisterDataSources(toRemoveApps);
                registerDataSources(toAdd);
                model.getRepository().updateDataSources(toAdd, toRemove);
            } finally {
                isProcessing.set(false);
            }
        }
    }

    public void dataChanged(DataChangeEvent<GlassFishModel> event) {
        if (event.getAdded().isEmpty() && event.getRemoved().isEmpty()) {
            addModels(event.getCurrent());
        } else {
            addModels(event.getAdded());
            removeModels(event.getRemoved());
        }
    }

    private void addModels(Set<GlassFishModel> models) {
        for (GlassFishModel model : models) {
            GlassFishDataSource lazyDS = new LazyLoadingSource();
            model.getRepository().addDataSource(lazyDS);
            ScheduledTask task = Scheduler.sharedInstance().schedule(new DiscoveryTask(model), Quantum.SUSPENDED);
            taskMap.put(model, task);
        }
    }

    private void removeModels(Set<GlassFishModel> models) {
        for (GlassFishModel model : models) {
            // removing the reference to the ScheduledTask practically unschedules the task
            Scheduler.sharedInstance().unschedule(taskMap.remove(model));
        }
    }

    public void dataFinished(GlassFishModel model) {
        // removing the reference to the ScheduledTask practically unschedules the task
        Scheduler.sharedInstance().unschedule(taskMap.remove(model));
    }

    public static void initialize() {
        DataSourceRepository.sharedInstance().addDataSourceProvider(INSTANCE);
        DataSourceRepository.sharedInstance().addDataChangeListener(INSTANCE, GlassFishModel.class);
        ExplorerSupport.sharedInstance().addExpansionListener(INSTANCE);
    }

    public static void shutdown() {
        DataSourceRepository.sharedInstance().removeDataSourceProvider(INSTANCE);
        DataSourceRepository.sharedInstance().removeDataChangeListener(INSTANCE);
        ExplorerSupport.sharedInstance().removeExpansionListener(INSTANCE);
    }

    public void dataSourceCollapsed(DataSource source) {
        // do nothing
    }

    public void dataSourceExpanded(DataSource source) {
        if (source instanceof GlassFishModel) {
            if (taskMap.containsKey(source)) {
                taskMap.get(source).setInterval(Quantum.seconds(3));
            }
        }
    }
}
