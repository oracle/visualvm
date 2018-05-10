package org.visualvm.demoapplicationtype.application;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import org.openide.util.Exceptions;
import org.visualvm.demoapplicationtype.datasource.AnagramDataSource;
import org.visualvm.demoapplicationtype.model.AnagramModel;

public class AnagramApplicationProvider implements DataChangeListener<AnagramModel>, DataRemovedListener<AnagramModel>, ExplorerExpansionListener {
    
    private static final AnagramApplicationProvider INSTANCE = new AnagramApplicationProvider();
    private final Map<AnagramModel, ScheduledTask> taskMap = new HashMap<AnagramModel, ScheduledTask>();
    
    private static class LazyLoadingSource extends AnagramDataSource {
        private String message;
        private AnagramModel parent;
        
        public LazyLoadingSource(String message, AnagramModel parent) {
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
    }
    
    private class DiscoveryTask implements SchedulerTask {
        
        private AnagramModel model;
        private volatile boolean running;
        
        public DiscoveryTask(AnagramModel model) {
            this.model = model;
        }
        
        public void onSchedule(long timeStamp) {
            if (running) return;
            running = true;
            try {
                
                JmxModel jmx = JmxModelFactory.getJmxModelFor(model.getApplication());
                if (jmx == null || jmx.getConnectionState() != JmxModel.ConnectionState.CONNECTED) {
                    return;
                }
                MBeanServerConnection conn = jmx.getMBeanServerConnection();
                ObjectName obj = new ObjectName("com.toy.anagrams.mbeans:type=AnagramsStats");               
                MBeanInfo infos = conn.getMBeanInfo(obj);
                MBeanAttributeInfo[] attrs = infos.getAttributes();             
                Set<AnagramApplication> currentApps = new HashSet<AnagramApplication>();
                
                for (int i = 0; i < attrs.length; i++) {                  
                    MBeanAttributeInfo attr = attrs[i];               
                    AnagramMbeansModule attrModule = new AnagramMbeansModule(attr != null ? (attr.getName() + " (" + attr.getType() + ")") : attr.getName(), attr.getName(), model);
        
                    currentApps.add(attrModule);
                }
                Set<AnagramDataSource> toRemoveApps = new HashSet<AnagramDataSource>(model.getRepository().getDataSources(AnagramDataSource.class));
                Set<AnagramDataSource> toAdd = new HashSet<AnagramDataSource>(currentApps);
                toRemoveApps.removeAll(currentApps);
                toAdd.removeAll(model.getRepository().getDataSources());
                
                Set<LazyLoadingSource> lazy = model.getRepository().getDataSources(LazyLoadingSource.class);
                Set<AnagramDataSource> toRemove = new HashSet<AnagramDataSource>(toRemoveApps);
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
                }
                
            } catch (MalformedObjectNameException ex) {
                Exceptions.printStackTrace(ex);
            } catch (NullPointerException ex) {
                Exceptions.printStackTrace(ex);
            } catch (InstanceNotFoundException ex) {
                Exceptions.printStackTrace(ex);
            } catch (IntrospectionException ex) {
                Exceptions.printStackTrace(ex);
            } catch (ReflectionException ex) {
                Exceptions.printStackTrace(ex);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            } finally {
                running = false;
            }
        }
    }
    
    public void dataChanged(DataChangeEvent<AnagramModel> event) {
        if (event.getAdded().isEmpty() && event.getRemoved().isEmpty()) {
            addModels(event.getCurrent());
        } else {
            addModels(event.getAdded());
            removeModels(event.getRemoved());
        }
    }
    
    private void addModels(Set<AnagramModel> models) {
        for (AnagramModel model : models) {
            AnagramDataSource lazyDS = new LazyLoadingSource("Please Wait ...",model);
            model.getRepository().addDataSource(lazyDS);
            ScheduledTask task = Scheduler.sharedInstance().schedule(new DiscoveryTask(model), Quantum.SUSPENDED);
            taskMap.put(model, task);
        }
    }
    
    private void removeModels(Set<AnagramModel> models) {
        for (AnagramModel model : models) {
            // removing the reference to the ScheduledTask practically unschedules the task
            Scheduler.sharedInstance().unschedule(taskMap.remove(model));
        }
    }
    
    public void dataRemoved(AnagramModel model) {
        // removing the reference to the ScheduledTask practically unschedules the task
        Scheduler.sharedInstance().unschedule(taskMap.remove(model));
        Set<AnagramApplication> roots = model.getRepository().getDataSources(AnagramApplication.class);
        model.getRepository().removeDataSources(roots);
    }
    
    public static void initialize() {
        DataSourceRepository.sharedInstance().addDataChangeListener(INSTANCE, AnagramModel.class);
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
        if (source instanceof AnagramModel) {
            if (taskMap.containsKey(source)) {
                taskMap.get(source).setInterval(Quantum.seconds(3));
            }
        }
    }
}
