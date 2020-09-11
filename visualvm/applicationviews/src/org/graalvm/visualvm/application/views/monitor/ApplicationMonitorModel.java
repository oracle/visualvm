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

package org.graalvm.visualvm.application.views.monitor;

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.application.jvm.Jvm;
import org.graalvm.visualvm.application.jvm.JvmFactory;
import org.graalvm.visualvm.application.jvm.MonitoredData;
import org.graalvm.visualvm.application.jvm.MonitoredDataListener;
import org.graalvm.visualvm.charts.SimpleXYChartSupport;
import org.graalvm.visualvm.core.datasource.DataSource;
import org.graalvm.visualvm.core.datasource.Storage;
import org.graalvm.visualvm.core.options.GlobalPreferences;
import org.graalvm.visualvm.core.snapshot.Snapshot;
import org.graalvm.visualvm.heapdump.HeapDumpSupport;
import org.graalvm.visualvm.tools.jmx.JmxModel;
import org.graalvm.visualvm.tools.jmx.JmxModel.ConnectionState;
import org.graalvm.visualvm.tools.jmx.JmxModelFactory;
import org.graalvm.visualvm.tools.jmx.JvmMXBeans;
import org.graalvm.visualvm.tools.jmx.JvmMXBeansFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.MemoryMXBean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.graalvm.visualvm.core.VisualVM;
import org.openide.util.NbBundle;


/**
 *
 * @author Jiri Sedlacek
 */
final class ApplicationMonitorModel {
    
    private static final String PROP_PREFIX = "ApplicationMonitorModel_";  // NOI18N
    
    static final String SNAPSHOT_VERSION = PROP_PREFIX + "version"; // NOI18N
    private static final String SNAPSHOT_VERSION_DIVIDER = "."; // NOI18N
    private static final String CURRENT_SNAPSHOT_VERSION_MAJOR = "1";   // NOI18N
    private static final String CURRENT_SNAPSHOT_VERSION_MINOR = "1";   // NOI18N
    private static final String CURRENT_SNAPSHOT_VERSION = CURRENT_SNAPSHOT_VERSION_MAJOR + SNAPSHOT_VERSION_DIVIDER + CURRENT_SNAPSHOT_VERSION_MINOR;
    
    private static final String PROP_NOT_DEFINED = "<not defined>"; // NOI18N

    public static final String PROP_CHART_CACHE = PROP_PREFIX + "chart_cache";    // NOI18N
    public static final String PROP_UPTIME = PROP_PREFIX + "uptime";    // NOI18N
    public static final String PROP_PREV_UPTIME = PROP_PREFIX + "prev_uptime";    // NOI18N
    public static final String PROP_INVOKE_GC_SUPPORTED = PROP_PREFIX + "invoke_gc_supported";    // NOI18N
    public static final String PROP_HEAP_DUMP_SUPPORTED = PROP_PREFIX + "heap_dump_supported";    // NOI18N
    public static final String PROP_CPU_MONITORING_SUPPORTED = PROP_PREFIX + "cpu_monitoring_supported";  // NOI18N
    public static final String PROP_GC_MONITORING_SUPPORTED = PROP_PREFIX + "gc_monitoring_supported";  // NOI18N
    public static final String PROP_MEMORY_MONITORING_SUPPORTED = PROP_PREFIX + "memory_monitoring_supported";  // NOI18N
    public static final String PROP_CLASS_MONITORING_SUPPORTED = PROP_PREFIX + "class_monitoring_supported";    // NOI18N
    public static final String PROP_THREADS_MONITORING_SUPPORTED = PROP_PREFIX + "threads_monitoring_supported";  // NOI18N
    public static final String PROP_NUMBER_OF_PROCESSORS = PROP_PREFIX + "number_of_processors";  // NOI18N

    public static final String PROP_PROCESS_CPU_TIME = PROP_PREFIX + "process_cpu_time"; // NOI18N
    public static final String PROP_PROCESS_GC_TIME = PROP_PREFIX + "process_gc_time"; // NOI18N
    public static final String PROP_PREV_PROCESS_CPU_TIME = PROP_PREFIX + "prev_process_cpu_time"; // NOI18N
    public static final String PROP_PREV_PROCESS_GC_TIME = PROP_PREFIX + "prev_process_gc_time"; // NOI18N
    public static final String PROP_HEAP_NAME = PROP_PREFIX + "heap_name"; // NOI18N
    public static final String PROP_HEAP_CAPACITY = PROP_PREFIX + "heap_capacity"; // NOI18N
    public static final String PROP_HEAP_USED = PROP_PREFIX + "heap_used"; // NOI18N
    public static final String PROP_MAX_HEAP = PROP_PREFIX + "max_heap"; // NOI18N
    public static final String PROP_PERMGEN_NAME = PROP_PREFIX + "permgen_name"; // NOI18N
    public static final String PROP_PERMGEN_CAPACITY = PROP_PREFIX + "permgen_capacity"; // NOI18N
    public static final String PROP_PERMGEN_USED = PROP_PREFIX + "permgen_used"; // NOI18N
    public static final String PROP_PERMGEN_MAX = PROP_PREFIX + "permgen_max"; // NOI18N
    public static final String PROP_SHARED_UNLOADED = PROP_PREFIX + "shared_unloaded"; // NOI18N
    public static final String PROP_TOTAL_UNLOADED = PROP_PREFIX + "total_unloaded"; // NOI18N
    public static final String PROP_SHARED_LOADED = PROP_PREFIX + "shared_loaded"; // NOI18N
    public static final String PROP_TOTAL_LOADED = PROP_PREFIX + "total_loaded"; // NOI18N
    public static final String PROP_TOTAL_THREADS = PROP_PREFIX + "total_threads"; // NOI18N
    public static final String PROP_DAEMON_THREADS = PROP_PREFIX + "daemon_threads"; // NOI18N
    public static final String PROP_PEAK_THREADS = PROP_PREFIX + "peak_threads"; // NOI18N
    public static final String PROP_STARTED_THREADS = PROP_PREFIX + "started_threads"; // NOI18N

    private static final String CPU_CHART_STORAGE = "monitor_cpu.dat"; // NOI18N
    private static final String HEAP_CHART_STORAGE = "monitor_heap.dat"; // NOI18N
    private static final String PERMGEN_CHART_STORAGE = "monitor_permgen.dat"; // NOI18N
    private static final String CLASSES_CHART_STORAGE = "monitor_classes.dat"; // NOI18N
    private static final String THREADS_CHART_STORAGE = "monitor_threads.dat"; // NOI18N
    
    private boolean initialized;
    private final DataSource source;
    private final boolean live;

    private final List<ChangeListener> listeners;

    private Jvm jvm;
    private MemoryMXBean memoryMXBean;
    private MonitoredDataListener monitoredDataListener;

    private int chartCache = -1;

    private long timestamp = -1;

    private long uptime = -1;
    private long prevUpTime = -1;
    private boolean takeHeapDumpSupported = false;
    private boolean cpuMonitoringSupported = false;
    private boolean gcMonitoringSupported = false;
    private boolean memoryMonitoringSupported = false;
    private boolean classMonitoringSupported = false;
    private boolean threadsMonitoringSupported = false;
    private int processorsCount = -1;

    private long processCpuTime = -1;
    private long processGcTime = -1;
    private long prevProcessCpuTime = -1;
    private long prevProcessGcTime = -1;
    private String heapName;
    private long heapCapacity = -1;
    private long heapUsed = -1;
    private long maxHeap = -1;
    private String permgenName;
    private long permgenCapacity = -1;
    private long permgenUsed = -1;
    private long permgenMax = -1;
    private long sharedUnloaded = -1;
    private long totalUnloaded = -1;
    private long sharedLoaded = -1;
    private long totalLoaded = -1;
    private long totalThreads = -1;
    private long daemonThreads = -1;
    private long peakThreads = -1;
    private long startedThreads = -1;

    private SimpleXYChartSupport cpuChartSupport;
    private SimpleXYChartSupport heapChartSupport;
    private SimpleXYChartSupport permGenChartSupport;
    private SimpleXYChartSupport classesChartSupport;
    private SimpleXYChartSupport threadsChartSupport;

    
    public static ApplicationMonitorModel create(Application application, boolean live) {
        return new ApplicationMonitorModel(application, live);
    }
    
    public static ApplicationMonitorModel create(Snapshot snapshot) {
        return new ApplicationMonitorModel(snapshot, false);
    }

    
    public DataSource getSource() { return source; }
    public boolean    isLive() { return live; }


    public MemoryMXBean getMemoryMXBean() { return memoryMXBean; }

    public int     getChartCache() { return chartCache; }
    public boolean isInvokeGCSupported() { return memoryMXBean != null; }
    public boolean isTakeHeapDumpSupported() { return takeHeapDumpSupported; }
    public boolean isCpuMonitoringSupported() { return cpuMonitoringSupported; }
    public boolean isGcMonitoringSupported() { return gcMonitoringSupported; }
    public boolean isMemoryMonitoringSupported() { return memoryMonitoringSupported; }
    public boolean isClassMonitoringSupported() { return classMonitoringSupported; }
    public boolean isThreadsMonitoringSupported() { return threadsMonitoringSupported; }
    public int     getProcessorsCount() { return processorsCount; }
    
    public long    getTimestamp() { return timestamp; }
    public long    getUpTime() { return uptime; }
    public long    getPrevUpTime() { return prevUpTime; }

    public long getProcessCpuTime() { return processCpuTime; }
    public long getProcessGcTime() { return processGcTime; }
    public long getPrevProcessCpuTime() { return prevProcessCpuTime; }
    public long getPrevProcessGcTime() { return prevProcessGcTime; }
    public String getHeapName() { return heapName; }
    public long getHeapCapacity() { return heapCapacity; }
    public long getHeapUsed() { return heapUsed; }
    public long getMaxHeap() { return maxHeap; }
    public String getPermgenName() { return permgenName; }
    public long getPermgenCapacity() { return permgenCapacity; }
    public long getPermgenUsed() { return permgenUsed; }
    public long getPermgenMax() { return permgenMax; }
    public long getSharedUnloaded() { return sharedUnloaded; }
    public long getTotalUnloaded() { return totalUnloaded; }
    public long getSharedLoaded() { return sharedLoaded; }
    public long getTotalLoaded() { return totalLoaded; }
    public long getTotalThreads() { return totalThreads; }
    public long getDaemonThreads() { return daemonThreads; }
    public long getPeakThreads() { return peakThreads; }
    public long getStartedThreads() { return startedThreads; }
    
    
    public synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        if (source instanceof Application) initialize((Application)source);
        else initialize((Snapshot)source);
    }

    public void registerCpuChartSupport(final SimpleXYChartSupport cpuChartSupport) {
        this.cpuChartSupport = cpuChartSupport;
        if (cpuChartSupport != null && source instanceof Snapshot)
            VisualVM.getInstance().runTask(new Runnable() {
                public void run() {
                    File file = new File(source.getStorage().getDirectory(), CPU_CHART_STORAGE);
                    if (file.isFile()) loadChartSupport(cpuChartSupport, file);
                }
            });
    }

    public void registerHeapChartSupport(final SimpleXYChartSupport heapChartSupport) {
        this.heapChartSupport = heapChartSupport;
        if (heapChartSupport != null && source instanceof Snapshot)
            VisualVM.getInstance().runTask(new Runnable() {
                public void run() {
                    File file = new File(source.getStorage().getDirectory(), HEAP_CHART_STORAGE);
                    if (file.isFile()) loadChartSupport(heapChartSupport, file);
                }
            });
    }

    public void registerPermGenChartSupport(final SimpleXYChartSupport permgenChartSupport) {
        this.permGenChartSupport = permgenChartSupport;
        if (permGenChartSupport != null && source instanceof Snapshot)
            VisualVM.getInstance().runTask(new Runnable() {
                public void run() {
                    File file = new File(source.getStorage().getDirectory(), PERMGEN_CHART_STORAGE);
                    if (file.isFile()) loadChartSupport(permGenChartSupport, file);
                }
            });
    }

    public void registerClassesChartSupport(final SimpleXYChartSupport classesChartSupport) {
        this.classesChartSupport = classesChartSupport;
        if (classesChartSupport != null && source instanceof Snapshot)
            VisualVM.getInstance().runTask(new Runnable() {
                public void run() {
                    File file = new File(source.getStorage().getDirectory(), CLASSES_CHART_STORAGE);
                    if (file.isFile()) loadChartSupport(classesChartSupport, file);
                }
            });
    }

    public void registerThreadsChartSupport(final SimpleXYChartSupport threadsChartSupport) {
        this.threadsChartSupport = threadsChartSupport;
        if (threadsChartSupport != null && source instanceof Snapshot)
            VisualVM.getInstance().runTask(new Runnable() {
                public void run() {
                    File file = new File(source.getStorage().getDirectory(), THREADS_CHART_STORAGE);
                    if (file.isFile()) loadChartSupport(threadsChartSupport, file);
                }
            });
    }

    public synchronized void cleanup() {
        listeners.clear();
        if (!initialized) return;
        if (jvm != null && monitoredDataListener != null)
            jvm.removeMonitoredDataListener(monitoredDataListener);
    }


    public void addChangeListener(ChangeListener listener) {
        if (live) listeners.add(listener);
    }

    public void removeChangeListener(ChangeListener listener) {
        if (live) listeners.remove(listener);
    }

    
    public void save(Snapshot snapshot) {
        
        initialize();
        
        Storage storage = snapshot.getStorage();
        
        setProperty(storage, SNAPSHOT_VERSION, CURRENT_SNAPSHOT_VERSION);

        setProperty(storage, PROP_CHART_CACHE, Integer.toString(chartCache));
        setProperty(storage, PROP_UPTIME, Long.toString(uptime));
        setProperty(storage, PROP_PREV_UPTIME, Long.toString(prevUpTime));
        setProperty(storage, PROP_CPU_MONITORING_SUPPORTED, Boolean.toString(cpuMonitoringSupported));
        setProperty(storage, PROP_GC_MONITORING_SUPPORTED, Boolean.toString(gcMonitoringSupported));
        setProperty(storage, PROP_MEMORY_MONITORING_SUPPORTED, Boolean.toString(memoryMonitoringSupported));
        setProperty(storage, PROP_CLASS_MONITORING_SUPPORTED, Boolean.toString(classMonitoringSupported));
        setProperty(storage, PROP_THREADS_MONITORING_SUPPORTED, Boolean.toString(threadsMonitoringSupported));
        setProperty(storage, PROP_NUMBER_OF_PROCESSORS, Integer.toString(processorsCount));

        setProperty(storage, PROP_PROCESS_CPU_TIME, Long.toString(processCpuTime));
        setProperty(storage, PROP_PROCESS_GC_TIME, Long.toString(processGcTime));
        setProperty(storage, PROP_PREV_PROCESS_CPU_TIME, Long.toString(prevProcessCpuTime));
        setProperty(storage, PROP_PREV_PROCESS_GC_TIME, Long.toString(prevProcessGcTime));
        setProperty(storage, PROP_HEAP_NAME, heapName);
        setProperty(storage, PROP_HEAP_CAPACITY, Long.toString(heapCapacity));
        setProperty(storage, PROP_HEAP_USED, Long.toString(heapUsed));
        setProperty(storage, PROP_MAX_HEAP, Long.toString(maxHeap));
        setProperty(storage, PROP_PERMGEN_NAME, permgenName);
        setProperty(storage, PROP_PERMGEN_CAPACITY, Long.toString(permgenCapacity));
        setProperty(storage, PROP_PERMGEN_USED, Long.toString(permgenUsed));
        setProperty(storage, PROP_PERMGEN_MAX, Long.toString(permgenMax));
        setProperty(storage, PROP_SHARED_UNLOADED, Long.toString(sharedUnloaded));
        setProperty(storage, PROP_TOTAL_UNLOADED, Long.toString(totalUnloaded));
        setProperty(storage, PROP_SHARED_LOADED, Long.toString(sharedLoaded));
        setProperty(storage, PROP_TOTAL_LOADED, Long.toString(totalLoaded));
        setProperty(storage, PROP_TOTAL_THREADS, Long.toString(totalThreads));
        setProperty(storage, PROP_DAEMON_THREADS, Long.toString(daemonThreads));
        setProperty(storage, PROP_PEAK_THREADS, Long.toString(peakThreads));
        setProperty(storage, PROP_STARTED_THREADS, Long.toString(startedThreads));

        File dir = storage.getDirectory();

        if (cpuMonitoringSupported || gcMonitoringSupported)
            saveChartSupport(cpuChartSupport, new File(dir, CPU_CHART_STORAGE));
        if (memoryMonitoringSupported)
            saveChartSupport(heapChartSupport, new File(dir, HEAP_CHART_STORAGE));
        if (memoryMonitoringSupported)
            saveChartSupport(permGenChartSupport, new File(dir, PERMGEN_CHART_STORAGE));
        if (classMonitoringSupported)
            saveChartSupport(classesChartSupport, new File(dir, CLASSES_CHART_STORAGE));
        if (threadsMonitoringSupported)
            saveChartSupport(threadsChartSupport, new File(dir, THREADS_CHART_STORAGE));
        
    }

    private static void saveChartSupport(SimpleXYChartSupport chartSupport, File file) {
        if (chartSupport == null) return;

        OutputStream os = null;
        
        try {
            os = new FileOutputStream(file);
            chartSupport.saveValues(os);
        } catch (Exception e) {
            // TODO: log it
        } finally {
            try {
                if (os != null) os.close();
            } catch (Exception e) {
                // TODO: log it
            }
        }
    }

    private static void loadChartSupport(SimpleXYChartSupport chartSupport, File file) {
        InputStream is = null;

        try {
            is = new FileInputStream(file);
            chartSupport.loadValues(is);
        } catch (Exception e) {
            // TODO: log it
        } finally {
            try {
                if (is != null) is.close();
            } catch (Exception e) {
                // TODO: log it
            }
        }
    }

    private void initialize(Snapshot snapshot) {
        // TODO: if some property cannot be loaded for current snapshot version, FAIL initializing the snapshot!
        Storage storage = snapshot.getStorage();

        String version = getProperty(storage, SNAPSHOT_VERSION);
        chartCache = Integer.parseInt(getProperty(storage, PROP_CHART_CACHE));
        uptime = Long.parseLong(getProperty(storage, PROP_UPTIME));
        prevUpTime = Long.parseLong(getProperty(storage, PROP_PREV_UPTIME));
        takeHeapDumpSupported = false;
        cpuMonitoringSupported = Boolean.parseBoolean(getProperty(storage, PROP_CPU_MONITORING_SUPPORTED));
        gcMonitoringSupported = Boolean.parseBoolean(getProperty(storage, PROP_GC_MONITORING_SUPPORTED));
        memoryMonitoringSupported = Boolean.parseBoolean(getProperty(storage, PROP_MEMORY_MONITORING_SUPPORTED));
        classMonitoringSupported = Boolean.parseBoolean(getProperty(storage, PROP_CLASS_MONITORING_SUPPORTED));
        threadsMonitoringSupported = Boolean.parseBoolean(getProperty(storage, PROP_THREADS_MONITORING_SUPPORTED));
        processorsCount = Integer.parseInt(getProperty(storage, PROP_NUMBER_OF_PROCESSORS));

        processCpuTime = Long.parseLong(getProperty(storage, PROP_PROCESS_CPU_TIME));
        processGcTime = Long.parseLong(getProperty(storage, PROP_PROCESS_GC_TIME));
        prevProcessCpuTime = Long.parseLong(getProperty(storage, PROP_PREV_PROCESS_CPU_TIME));
        prevProcessGcTime = Long.parseLong(getProperty(storage, PROP_PREV_PROCESS_GC_TIME));
        heapCapacity = Long.parseLong(getProperty(storage, PROP_HEAP_CAPACITY));
        heapUsed = Long.parseLong(getProperty(storage, PROP_HEAP_USED));
        maxHeap = Long.parseLong(getProperty(storage, PROP_MAX_HEAP));
        permgenCapacity = Long.parseLong(getProperty(storage, PROP_PERMGEN_CAPACITY));
        permgenUsed = Long.parseLong(getProperty(storage, PROP_PERMGEN_USED));
        permgenMax = Long.parseLong(getProperty(storage, PROP_PERMGEN_MAX));
        sharedUnloaded = Long.parseLong(getProperty(storage, PROP_SHARED_UNLOADED));
        totalUnloaded = Long.parseLong(getProperty(storage, PROP_TOTAL_UNLOADED));
        sharedLoaded = Long.parseLong(getProperty(storage, PROP_SHARED_LOADED));
        totalLoaded = Long.parseLong(getProperty(storage, PROP_TOTAL_LOADED));
        totalThreads = Long.parseLong(getProperty(storage, PROP_TOTAL_THREADS));
        daemonThreads = Long.parseLong(getProperty(storage, PROP_DAEMON_THREADS));
        peakThreads = Long.parseLong(getProperty(storage, PROP_PEAK_THREADS));
        startedThreads = Long.parseLong(getProperty(storage, PROP_STARTED_THREADS));
        
        if (version.compareTo("1.1") >= 0) {                      // NOI18N
            heapName = getProperty(storage, PROP_HEAP_NAME);
            permgenName = getProperty(storage, PROP_PERMGEN_NAME);
        } else {
            heapName = NbBundle.getMessage(ApplicationMonitorModel.class, "LBL_Heap");  // NOI18N
            permgenName = NbBundle.getMessage(ApplicationMonitorModel.class, "LBL_PermGen");    // NOI18N
        }
    }
    
    private static void setProperty(Storage storage, String property, String value) {
        storage.setCustomProperty(property, value == null ? PROP_NOT_DEFINED : value);
    }
    
    private static String getProperty(Storage storage, String property) {
        String value = storage.getCustomProperty(property);
        return PROP_NOT_DEFINED.equals(value) ? null : value;
    }
    
    private void initialize(Application application) {
        GlobalPreferences preferences = GlobalPreferences.sharedInstance();
        chartCache = preferences.getMonitoredDataCache() * 60 /
                     preferences.getMonitoredDataPoll();

        processorsCount = 1;
        
        jvm = JvmFactory.getJVMFor(application);
        if (jvm != null) {
            HeapDumpSupport hds = HeapDumpSupport.getInstance();
            if (application.isLocalApplication()) {
                takeHeapDumpSupported = hds.supportsHeapDump(application);
            } else {
                takeHeapDumpSupported = hds.supportsRemoteHeapDump(application);
            }
            cpuMonitoringSupported = jvm.isCpuMonitoringSupported();
            gcMonitoringSupported = jvm.isCollectionTimeSupported();
            memoryMonitoringSupported = jvm.isMemoryMonitoringSupported();
            classMonitoringSupported = jvm.isClassMonitoringSupported();
            threadsMonitoringSupported = jvm.isThreadMonitoringSupported();
            if (memoryMonitoringSupported) {
                String[] names = jvm.getGenName();
                heapName = names[0];
                permgenName = names[1];
            }
            processorsCount = jvm.getAvailableProcessors();
        }

        memoryMXBean = null;
        JmxModel jmxModel = JmxModelFactory.getJmxModelFor(application);
        if (jmxModel != null && jmxModel.getConnectionState() == ConnectionState.CONNECTED) {
            JvmMXBeans mxbeans = JvmMXBeansFactory.getJvmMXBeans(jmxModel);
            if (mxbeans != null) {
                memoryMXBean = mxbeans.getMemoryMXBean();
            }
        }

        if (jvm != null) {
            updateValues(System.currentTimeMillis(), jvm.getMonitoredData());

            if (live) {
                monitoredDataListener = new MonitoredDataListener() {
                    long lastTimestamp = -1;
                    public void monitoredDataEvent(final MonitoredData data) {
                        long timestamp = System.currentTimeMillis();
                        final long timestampF = lastTimestamp < timestamp ?
                            lastTimestamp = timestamp : ++lastTimestamp;
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                updateValues(timestampF, data);
                                fireChange();
                            }
                        });
                    }
                };
                jvm.addMonitoredDataListener(monitoredDataListener);
            }
        }

    }

    private void updateValues(final long time, final MonitoredData data) {
        timestamp = time;
        if (data != null) {
            prevUpTime = uptime;
            uptime = data.getUpTime();
            
            if (cpuMonitoringSupported) {
                prevProcessCpuTime = processCpuTime;
                processCpuTime = data.getProcessCpuTime();
            }

            if (gcMonitoringSupported) {
                prevProcessGcTime = processGcTime;
                processGcTime = data.getCollectionTime();
            }

            if (memoryMonitoringSupported) {
                heapCapacity = data.getGenCapacity()[0];
                heapUsed = data.getGenUsed()[0];
                maxHeap = data.getGenMaxCapacity()[0];
                permgenCapacity = data.getGenCapacity()[1];
                permgenUsed = data.getGenUsed()[1];
                permgenMax = data.getGenMaxCapacity()[1];
            }

            if (classMonitoringSupported) {
                sharedUnloaded = data.getSharedUnloadedClasses();
                totalUnloaded  = data.getUnloadedClasses();
                sharedLoaded  = data.getSharedLoadedClasses();
                totalLoaded   = data.getLoadedClasses();
            }

            if (threadsMonitoringSupported) {
                totalThreads   = data.getThreadsLive();
                daemonThreads  = data.getThreadsDaemon();
                peakThreads    = data.getThreadsLivePeak();
                startedThreads = data.getThreadsStarted();
            }
        }
    }

    private void fireChange() {
        final List<ChangeListener> list = new ArrayList();
        synchronized (listeners) { list.addAll(listeners); }
        for (ChangeListener l : list) l.stateChanged(new ChangeEvent(this));
    }

    
    private ApplicationMonitorModel(DataSource source, boolean live) {
        initialized = false;
        
        this.source = source;
        this.live = live;

        listeners = Collections.synchronizedList(new ArrayList());
    }

}
