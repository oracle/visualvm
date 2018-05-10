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
package net.java.visualvm.modules.glassfish.datasource;

import com.sun.appserv.management.DomainRoot;
import com.sun.appserv.management.config.WebModuleConfig;
import com.sun.appserv.management.j2ee.J2EETypes;
import com.sun.appserv.management.monitor.ServerRootMonitor;
import com.sun.appserv.management.monitor.WebModuleVirtualServerMonitor;
import org.graalvm.visualvm.core.datasource.DataSource;
import org.graalvm.visualvm.core.datasource.DataSourceRepository;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import org.graalvm.visualvm.core.datasupport.DataChangeEvent;
import org.graalvm.visualvm.core.datasupport.DataChangeListener;
import org.graalvm.visualvm.core.datasupport.DataRemovedListener;
import org.graalvm.visualvm.core.explorer.ExplorerExpansionListener;
import org.graalvm.visualvm.core.explorer.ExplorerSupport;
import org.graalvm.visualvm.core.scheduler.Quantum;
import org.graalvm.visualvm.core.scheduler.ScheduledTask;
import org.graalvm.visualvm.core.scheduler.Scheduler;
import org.graalvm.visualvm.core.scheduler.SchedulerTask;
import org.graalvm.visualvm.tools.jmx.JmxModel;
import org.graalvm.visualvm.tools.jmx.JmxModelFactory;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
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
public class GlassFishApplicationProvider implements DataChangeListener<GlassFishModel>, DataRemovedListener<GlassFishModel>, ExplorerExpansionListener {

    private static final GlassFishApplicationProvider INSTANCE = new GlassFishApplicationProvider();
    private final Map<GlassFishModel, ScheduledTask> taskMap = new HashMap<GlassFishModel, ScheduledTask>();

    private static class LazyLoadingSource extends GlassFishDataSource {
        private String message;
        private GlassFishModel parent;
        public LazyLoadingSource(String message, GlassFishModel parent) {
            this.message = message;
            this.parent = parent;
        }
        
        @Override
        public DataSourceDescriptor getDescriptor() {
            return new DataSourceDescriptor(this) {

                @Override
                public int getAutoExpansionPolicy() {
                    return EXPAND_NEVER;
                }

                @Override
                public String getName() {
                    return message;
                }
            };
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final LazyLoadingSource other = (LazyLoadingSource) obj;
            if (this.message != other.message && (this.message == null || !this.message.equals(other.message))) {
                return false;
            }
            if (this.parent != other.parent && (this.parent == null || !this.parent.equals(other.parent))) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 19 * hash + (this.message != null ? this.message.hashCode() : 0);
            hash = 19 * hash + (this.parent != null ? this.parent.hashCode() : 0);
            return hash;
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
                if ((jmx == null || jmx.getConnectionState() == JmxModel.ConnectionState.DISCONNECTED)){
                    if (beenNotified.compareAndSet(false, true)) {
                        NotifyDescriptor nd = new NotifyDescriptor.Message("Cannot establish JMX connection", NotifyDescriptor.ERROR_MESSAGE);
                        DialogDisplayer.getDefault().notifyLater(nd);
                        model.setVisible(false);
                    }
                    return;
                }
                if (jmx.getConnectionState() != JmxModel.ConnectionState.CONNECTED) {
                    model.setVisible(true);
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

                Set<GlassFishDataSource> toRemoveApps = new HashSet<GlassFishDataSource>(model.getRepository().getDataSources(GlassFishDataSource.class));
                Set<GlassFishDataSource> toAdd = new HashSet<GlassFishDataSource>(currentApps);
                toRemoveApps.removeAll(currentApps);
                toAdd.removeAll(model.getRepository().getDataSources());

                Set<LazyLoadingSource> lazy = model.getRepository().getDataSources(LazyLoadingSource.class);
                Set<GlassFishDataSource> toRemove = new HashSet<GlassFishDataSource>(toRemoveApps);
                toRemove.addAll(lazy);

                if (currentApps.size() == 0) {
                    LazyLoadingSource unavailable = new LazyLoadingSource("Unavailable", model);
                    toAdd.add(unavailable);
                    toRemove.remove(unavailable);
                }
                toAdd.removeAll(lazy);

                if (toAdd.size() > 0 || toRemove.size() > 0) {
                    model.getRepository().addDataSources(toAdd);
                    model.getRepository().removeDataSources(toRemove);
//                    model.getRepository().updateDataSources(toAdd, toRemove);
                }
            } catch (UndeclaredThrowableException e) {
                // this is caused by disappearing of the underlying JMX connection
                // just ignore it
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
            GlassFishDataSource lazyDS = new LazyLoadingSource("Please wait", model);
            model.getRepository().addDataSource(lazyDS);
            ScheduledTask task = Scheduler.sharedInstance().schedule(new DiscoveryTask(model), Quantum.SUSPENDED);
            taskMap.put(model, task);
        }
    }

    private void removeModels(Set<GlassFishModel> models) {
        for (GlassFishModel model : models) {
            // removing the reference to the ScheduledTask practically unschedules the task
            Scheduler.sharedInstance().unschedule(taskMap.remove(model));
            
            Set<GlassFishApplication> roots = model.getRepository().getDataSources(GlassFishApplication.class);
            model.getRepository().removeDataSources(roots);
        }
    }

    public void dataRemoved(GlassFishModel model) {
        // removing the reference to the ScheduledTask practically unschedules the task
        Scheduler.sharedInstance().unschedule(taskMap.remove(model));
        
        Set<GlassFishApplication> roots = model.getRepository().getDataSources(GlassFishApplication.class);
        model.getRepository().removeDataSources(roots);
    }

    public static void initialize() {
        DataSourceRepository.sharedInstance().addDataChangeListener(INSTANCE, GlassFishModel.class);
        ExplorerSupport.sharedInstance().addExpansionListener(INSTANCE);
    }

    public static void shutdown() {
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
