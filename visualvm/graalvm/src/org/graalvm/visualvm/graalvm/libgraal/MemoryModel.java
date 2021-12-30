/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.graalvm.libgraal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.Attribute;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.application.jvm.Jvm;
import org.graalvm.visualvm.application.jvm.JvmFactory;
import org.graalvm.visualvm.application.jvm.MonitoredData;
import org.graalvm.visualvm.application.jvm.MonitoredDataListener;
import org.graalvm.visualvm.application.snapshot.ApplicationSnapshot;
import org.graalvm.visualvm.charts.SimpleXYChartSupport;
import org.graalvm.visualvm.core.VisualVM;
import org.graalvm.visualvm.core.datasource.DataSource;
import org.graalvm.visualvm.core.datasource.Storage;
import org.graalvm.visualvm.core.options.GlobalPreferences;
import org.graalvm.visualvm.core.snapshot.Snapshot;
import org.graalvm.visualvm.tools.jmx.JmxModel;
import org.graalvm.visualvm.tools.jmx.JmxModelFactory;

/**
 *
 * @author Tomas Hurka
 */
final class MemoryModel {
    private static final Logger LOGGER = Logger.getLogger(MemoryModel.class.getName());

    private static final String PROP_PREFIX = "LibgraalModel_";  // NOI18N
    private static final String USAGE_ATTRIBUTE = "Usage"; // NOI18N
    private static final String PEAK_USAGE_ATTRIBUTE = "PeakUsage"; // NOI18N

    private static final String SNAPSHOT_VERSION = PROP_PREFIX + "version"; // NOI18N
    private static final String SNAPSHOT_VERSION_DIVIDER = "."; // NOI18N
    private static final String CURRENT_SNAPSHOT_VERSION_MAJOR = "1";   // NOI18N
    private static final String CURRENT_SNAPSHOT_VERSION_MINOR = "0";   // NOI18N
    private static final String CURRENT_SNAPSHOT_VERSION = CURRENT_SNAPSHOT_VERSION_MAJOR + SNAPSHOT_VERSION_DIVIDER + CURRENT_SNAPSHOT_VERSION_MINOR;

    private static final String PROP_NOT_DEFINED = "<not defined>"; // NOI18N

    private static final String PROP_CHART_CACHE = PROP_PREFIX + "chart_cache";    // NOI18N
    private static final String PROP_HEAP_NAME = PROP_PREFIX + "heap_name"; // NOI18N
    private static final String PROP_HEAP_CAPACITY = PROP_PREFIX + "heap_capacity"; // NOI18N
    private static final String PROP_HEAP_USED = PROP_PREFIX + "heap_used"; // NOI18N
    private static final String PROP_MAX_HEAP = PROP_PREFIX + "max_heap"; // NOI18N
    private static final String CHART_STORAGE = PROP_PREFIX + "monitor_libgraal.dat"; // NOI18N

    private boolean initialized;
    private DataSource source;
    private boolean live;

    private final List<ChangeListener> listeners;
    private SimpleXYChartSupport heapChartSupport;
    private MonitoredDataListener monitoredDataListener;

    private Jvm jvm;
    private ObjectName libgraalName;
    private MBeanServerConnection connection;
    private boolean isAlreadyRegistered;

    private int chartCache = -1;

    private long timestamp = -1;

    private String heapName;
    private long heapCapacity = -1;
    private long heapUsed = -1;
    private long maxHeap = -1;

    static MemoryModel create(Application application, String name, ObjectName libgraalName) {
        return new MemoryModel(application, name, libgraalName);
    }

    static MemoryModel create(DataSource ds, String name) {
        return new MemoryModel(ds, name);
    }

    DataSource getSource() {
        return source;
    }

    boolean isLive() {
        return live;
    }

    long getTimestamp() {
        return timestamp;
    }

    int getChartCache() {
        return chartCache;
    }

    String getHeapName() {
        return heapName;
    }

    long getHeapCapacity() {
        return heapCapacity;
    }

    long getHeapUsed() {
        return heapUsed;
    }

    long getMaxHeap() {
        return maxHeap;
    }

    synchronized void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        if (source instanceof Application) {
            initialize((Application) source);
        } else {
            initialize((Snapshot) source);
        }
    }

    void registerHeapChartSupport(final SimpleXYChartSupport heapChartSupport) {
        this.heapChartSupport = heapChartSupport;
        if (heapChartSupport != null && source instanceof Snapshot) {
            VisualVM.getInstance().runTask(new Runnable() {
                public void run() {
                    File file = new File(source.getStorage().getDirectory(), CHART_STORAGE);
                    if (file.isFile() && file.canRead()) {
                        loadChartSupport(heapChartSupport, file);
                    }
                }
            });
        }
    }

    synchronized void cleanup() {
        listeners.clear();
        if (!initialized) {
            return;
        }
        if (jvm != null && monitoredDataListener != null) {
            jvm.removeMonitoredDataListener(monitoredDataListener);
        }
        connection = null;
    }

    void addChangeListener(ChangeListener listener) {
        if (live) {
            listeners.add(listener);
        }
    }

    void removeChangeListener(ChangeListener listener) {
        if (live) {
            listeners.remove(listener);
        }
    }

    void save(Snapshot snapshot) {

        initialize();

        Storage storage = snapshot.getStorage();

        setProperty(storage, SNAPSHOT_VERSION, CURRENT_SNAPSHOT_VERSION);

        setProperty(storage, PROP_CHART_CACHE, Integer.toString(chartCache));

        setProperty(storage, PROP_HEAP_NAME, heapName);
        setProperty(storage, PROP_HEAP_CAPACITY, Long.toString(heapCapacity));
        setProperty(storage, PROP_HEAP_USED, Long.toString(heapUsed));
        setProperty(storage, PROP_MAX_HEAP, Long.toString(maxHeap));

        File dir = storage.getDirectory();

        saveChartSupport(heapChartSupport, new File(dir, CHART_STORAGE));
    }

    private static void saveChartSupport(SimpleXYChartSupport chartSupport, File file) {
        if (chartSupport == null) {
            return;
        }

        OutputStream os = null;

        try {
            os = new FileOutputStream(file);
            chartSupport.saveValues(os);
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "saveChartSupport", e);   // NOI18N
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
            } catch (Exception e) {
                LOGGER.log(Level.INFO, "saveChartSupport", e);   // NOI18N
            }
        }
    }

    private static void loadChartSupport(SimpleXYChartSupport chartSupport, File file) {
        InputStream is = null;

        try {
            is = new FileInputStream(file);
            chartSupport.loadValues(is);
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "loadChartSupport", e);   // NOI18N
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (Exception e) {
                LOGGER.log(Level.INFO, "loadChartSupport", e);   // NOI18N
            }
        }
    }

    private void initialize(Snapshot snapshot) {
        // TODO: if some property cannot be loaded for current snapshot version, FAIL initializing the snapshot!
        Storage storage = snapshot.getStorage();

        String version = getProperty(storage, SNAPSHOT_VERSION);
        heapName = getProperty(storage, PROP_HEAP_NAME);
        chartCache = Integer.parseInt(getProperty(storage, PROP_CHART_CACHE));
        heapCapacity = Long.parseLong(getProperty(storage, PROP_HEAP_CAPACITY));
        heapUsed = Long.parseLong(getProperty(storage, PROP_HEAP_USED));
        maxHeap = Long.parseLong(getProperty(storage, PROP_MAX_HEAP));
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
        chartCache = preferences.getMonitoredDataCache() * 60
                / preferences.getMonitoredDataPoll();

        jvm = JvmFactory.getJVMFor(application);
        connection = getConnection(application);
        if (connection != null) {
            updateValues(System.currentTimeMillis(), getData());

            if (live) {
                monitoredDataListener = new MonitoredDataListener() {
                    long lastTimestamp = -1;

                    public void monitoredDataEvent(final MonitoredData data) {
                        long timestamp = System.currentTimeMillis();
                        final long timestampF = lastTimestamp < timestamp
                                ? lastTimestamp = timestamp : ++lastTimestamp;
                        final Object[] values = getData();

                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                updateValues(timestampF, values);
                                fireChange();
                            }
                        });
                    }
                };
                jvm.addMonitoredDataListener(monitoredDataListener);
            }
        }
    }

    private boolean isLibgraalRegistered() throws IOException {
        if (!isAlreadyRegistered) {
            isAlreadyRegistered = connection.isRegistered(libgraalName);
        }
        return isAlreadyRegistered;
    }

    private Object[] getAttributes(String... names) {
        try {
            Object[] values = new Object[names.length];
            if (isLibgraalRegistered()) {
                List<Attribute> attrs = connection.getAttributes(libgraalName, names).asList();

                for (int i = 0; i < values.length; i++) {
                    values[i] = attrs.get(i).getValue();
                }
            }
            return values;
        } catch (Exception ex) {
            LOGGER.log(Level.INFO, "getAttributes", ex);   // NOI18N
        }
        return null;
    }

    private Object[] getData() {
        if (live && connection != null) {
            Object[] values =  getAttributes(USAGE_ATTRIBUTE, PEAK_USAGE_ATTRIBUTE);

            if (values == null) {
                connection = null;
            }
            return values;
        }
        return null;
    }

    private void updateValues(final long time, Object[] values) {
        if (values != null) {
            CompositeData usageData = (CompositeData) values[0];
            CompositeData peakData = (CompositeData) values[1];

            timestamp = time;
            if (usageData != null && peakData != null) {
                MemoryUsage mem = MemoryUsage.from(usageData);
                MemoryUsage peak = MemoryUsage.from(peakData);
                heapUsed = mem.getUsed();
                heapCapacity = peak.getUsed();
                maxHeap = mem.getMax();
            } else {
                heapUsed = 0;
                heapCapacity = 0;
                maxHeap = 0;
            }
        }
    }

    private void fireChange() {
        final List<ChangeListener> list = new ArrayList<>();
        synchronized (listeners) {
            list.addAll(listeners);
        }
        for (ChangeListener l : list) {
            l.stateChanged(new ChangeEvent(this));
        }
    }

    private MemoryModel() {
        initialized = false;
        listeners = Collections.synchronizedList(new ArrayList<>());
    }

    private MemoryModel(DataSource src, String name) {
        this();
        source = src;
        heapName = name;
        live = false;
    }

    private MemoryModel(DataSource src, String name, ObjectName mbeanName) {
        this(src, name);
        live = true;
        libgraalName = mbeanName;
    }

    private static MBeanServerConnection getConnection(Application app) {
        JmxModel jmxModel = JmxModelFactory.getJmxModelFor(app);
        if (jmxModel != null && jmxModel.getConnectionState() == JmxModel.ConnectionState.CONNECTED) {
            return jmxModel.getMBeanServerConnection();
        }
        return null;
    }

    static boolean isInStapshot(ApplicationSnapshot snapshot) {
        String version = getProperty(snapshot.getStorage(), SNAPSHOT_VERSION);
        
        return version != null;
    }
}
