/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.graalvm.svm;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.application.jvm.HeapHistogram;
import org.graalvm.visualvm.application.jvm.Jvm;
import org.graalvm.visualvm.application.jvm.MonitoredData;
import org.graalvm.visualvm.application.jvm.MonitoredDataListener;
import org.graalvm.visualvm.core.datasupport.Stateful;
import org.graalvm.visualvm.tools.jvmstat.JvmJvmstatModel;
import org.graalvm.visualvm.tools.jvmstat.JvmJvmstatModelFactory;
import org.graalvm.visualvm.tools.jvmstat.JvmstatListener;
import org.graalvm.visualvm.tools.jvmstat.JvmstatModel;
import org.graalvm.visualvm.tools.jvmstat.MonitoredValue;

/**
 *
 * @author Tomas Hurka
 */
public class SVMJVMImpl extends Jvm implements JvmstatListener {

    private static final String USER_DIR_COUNTER_NAME = "java.property.user.dir";
    private static final String PROCESSORS_COUNTER_NAME = "com.oracle.svm.processors";
    private static final String SVM_HEAP_DUMP_PREFIX = "svm-heapdump-";
    private static final String SVM_HEAP_DUMP_SUFFIX = ".hprof";
    private static final String SYSTEM_PROPERTY_PREFIX = "java.property.";
    private static final String SYSTEM_PROPERTY_REG_EXPR = SYSTEM_PROPERTY_PREFIX.replace(".", "\\.")+".*"; // NOI18N
    private static final String MEMORY_COUNTER_REG_EXPR = "sun\\.gc\\.generation\\..*";

    Application application;
    JvmstatModel monitoredVm;
    JvmJvmstatModel jvmstatModel;
    Set<MonitoredDataListener> listeners;

    // static JVM data
    private boolean staticDataInitialized;
    private final Object staticDataLock = new Object();
    private String commandLine;
    private String jvmArgs;
    private String jvmFlags;
    private String mainArgs;
    private String mainClass;
    private String vmVersion;
    private String javaVersion;
    private String javaHome;
    private String vmInfo;
    private String vmName;
    private String vmVendor;


    SVMJVMImpl(Application app,JvmstatModel jvms) {
        application = app;
        monitoredVm = jvms;
        jvmstatModel = JvmJvmstatModelFactory.getJvmstatModelFor(app);
        listeners = new HashSet<>();
    }

    public boolean isAttachable() {
        return false;
    }

    public boolean isBasicInfoSupported() {
        return true;
    }

    public String getCommandLine() {
        initStaticData();
        return commandLine;
    }

    public String getJvmArgs() {
        initStaticData();
        return jvmArgs;
    }

    public String getJvmFlags() {
        initStaticData();
        return jvmFlags;
    }

    public String getMainArgs() {
        initStaticData();
        return mainArgs;
    }

    public String getMainClass() {
        initStaticData();
        return mainClass;
    }

    public String getVmVersion() {
        initStaticData();
        return vmVersion;
    }

    public String getJavaVersion() {
        initStaticData();
        if (javaVersion != null) {
            return javaVersion;
        }
        return vmVersion;
    }

    public String getJavaHome() {
        initStaticData();
        return javaHome;
    }

    public String getVmInfo() {
        initStaticData();
        return vmInfo;
    }

    public String getVmName() {
        initStaticData();
        return vmName;
    }

    public String getVmVendor() {
        initStaticData();
        return vmVendor;
    }

    public boolean is14() {
        return false;
    }

    public boolean is15() {
        return false;
    }

    public boolean is16() {
        return false;
    }

    public boolean is17() {
        return false;
    }

    public boolean is18() {
        String ver = getJavaVersion();
        if (ver != null && ver.startsWith("1.8.")) {
            return true;
        }
        return false;
    }

    public boolean is19() {
        String ver = getJavaVersion();
        if (ver != null && javaVersion != null && (ver.startsWith("1.9.") || (ver.equals("9")) || (ver.startsWith("9.")))) {    // NOI18N
            return true;
        }
        return false;
    }

    public boolean is100() {
        String ver = getJavaVersion();
        if (ver != null && javaVersion != null && (ver.equals("10") || ver.startsWith("10."))) {    // NOI18N
            return true;
        }
        return false;
    }

    public boolean is110() {
        String ver = getJavaVersion();
        if (ver != null && javaVersion != null && (ver.equals("11") || ver.equals("11-ea") || ver.startsWith("11."))) {    // NOI18N
            return true;
        }
        return false;
    }

    public boolean isDumpOnOOMEnabled() {
        return false;
    }

    public void addMonitoredDataListener(MonitoredDataListener l) {
        synchronized (listeners) {
            if (listeners.add(l)) {
                if (monitoredVm != null) {
                    monitoredVm.addJvmstatListener(this);
                }
            }
        }
    }

    public void removeMonitoredDataListener(MonitoredDataListener l) {
        synchronized (listeners) {
            if (listeners.remove(l)) {
                if (listeners.isEmpty()) {
                    if (monitoredVm != null) {
                        monitoredVm.removeJvmstatListener(this);
                    }
                }
            }
        }
    }

    public String[] getGenName() {
        if (jvmstatModel != null) {
            return jvmstatModel.getGenName();
        }
        throw new UnsupportedOperationException();
    }

    public boolean isMonitoringSupported() {
        return isClassMonitoringSupported() || isThreadMonitoringSupported() || isMemoryMonitoringSupported();
    }

    public boolean isClassMonitoringSupported() {
        return monitoredVm != null;
    }

    public boolean isThreadMonitoringSupported() {
        return monitoredVm != null;
    }

    public boolean isMemoryMonitoringSupported() {
        if (monitoredVm != null) {
            List<String> vals = monitoredVm.findByPattern(MEMORY_COUNTER_REG_EXPR);

            return vals != null && !vals.isEmpty();
        }
        return false;
    }

    public boolean isGetSystemPropertiesSupported() {
        return true;
    }

    @Override
    public int getAvailableProcessors() {
        MonitoredValue procs = monitoredVm.findMonitoredValueByName(PROCESSORS_COUNTER_NAME);

        if (procs != null) {
            return ((Long)procs.getValue()).intValue();
        }
        // default
        return 1;
    }

    public Properties getSystemProperties() {
        Properties p = new Properties();
        for (MonitoredValue val : monitoredVm.findMonitoredValueByPattern(SYSTEM_PROPERTY_REG_EXPR)) {
            p.put(val.getName().substring(SYSTEM_PROPERTY_PREFIX.length()), val.getValue());
        }
        return p;
    }

    public boolean isDumpOnOOMEnabledSupported() {
        return false;
    }

    public synchronized void setDumpOnOOMEnabled(boolean enabled) {
    }

    public boolean isTakeHeapDumpSupported() {
        if (application.isLocalApplication()) {
            return monitoredVm.findByName(USER_DIR_COUNTER_NAME) != null;
        }
        return false;
    }

    public boolean takeHeapDump(File outputFile) throws IOException {
        if (!isTakeHeapDumpSupported()) {
            throw new UnsupportedOperationException();
        }
        String cwd = monitoredVm.findByName(USER_DIR_COUNTER_NAME);
        Path applicationCwd = Paths.get(cwd);
        WatchService watchService = FileSystems.getDefault().newWatchService();
        WatchKey key = applicationCwd.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
        Runtime.getRuntime().exec(new String[] {"kill", "-USR1", String.valueOf(application.getPid())});
        try {
            Path name = findHeapDumpFile(key);
            if (name == null) {
                key = watchService.poll(20, TimeUnit.SECONDS);
                name = findHeapDumpFile(key);
            }
            watchService.close();
            if (name == null) {
                return false;
            }
            Path dumpPath = applicationCwd.resolve(name);
            Path outputPath = outputFile.toPath();
            waitDumpDone(dumpPath);
            Files.move(dumpPath, outputPath);
            return true;
        } catch (InterruptedException ex) {
            watchService.close();
            return false;
        }
    }

    private Path findHeapDumpFile(WatchKey key) {
        for (WatchEvent<?> event : key.pollEvents()) {
            WatchEvent.Kind<?> kind = event.kind();
            if (kind == StandardWatchEventKinds.OVERFLOW) {
                continue;
            }
            WatchEvent<Path> ev = (WatchEvent<Path>)event;
            Path filename = ev.context();
            String name = filename.toString();
            if (name.endsWith(SVM_HEAP_DUMP_SUFFIX) && name.startsWith(SVM_HEAP_DUMP_PREFIX)) {
                return filename;
            }
        }
        return null;
    }

    public boolean isTakeThreadDumpSupported() {
        return false;
    }

    public String takeThreadDump() {
        throw new UnsupportedOperationException();
    }

    public HeapHistogram takeHeapHistogram() {
        return null;
    }

    public boolean isCpuMonitoringSupported() {
        return true;
    }

    public boolean isCollectionTimeSupported() {
        return false;
    }

    public boolean isJfrAvailable() {
        return false;
    }

    public List<Long> jfrCheck() {
        return Collections.emptyList();
    }

    public String takeJfrDump(long recording, String fileName) {
        throw new UnsupportedOperationException();
    }

    public boolean startJfrRecording(String name, String[] settings, String delay,
            String duration, Boolean disk, String path, String maxAge, String maxSize,
            Boolean dumpOnExit) {
        throw new UnsupportedOperationException();
    }

    public boolean stopJfrRecording() {
        throw new UnsupportedOperationException();
    }

    public MonitoredData getMonitoredData() {
        if (application.getState() == Stateful.STATE_AVAILABLE) {
            if (monitoredVm != null) {
                return new SVMMonitoredDataImpl(this, monitoredVm, jvmstatModel);
            }
        }
        return null;
    }

    protected void initStaticData() {
        synchronized (staticDataLock) {
            if (staticDataInitialized) {
                return;
            }
            if (jvmstatModel != null) {
                commandLine = jvmstatModel.getCommandLine();
                jvmArgs = jvmstatModel.getJvmArgs();
                jvmFlags = jvmstatModel.getJvmFlags();
                mainArgs = jvmstatModel.getMainArgs();
                mainClass = jvmstatModel.getMainClass();
                vmVersion = jvmstatModel.getVmVersion();
                javaVersion = jvmstatModel.getJavaVersion();
                javaHome = jvmstatModel.getJavaHome();
                vmInfo = jvmstatModel.getVmInfo();
                vmName = jvmstatModel.getVmName();
                vmVendor = jvmstatModel.getVmVendor();
            }
            staticDataInitialized = true;
        }
    }

    public void dataChanged(JvmstatModel stat) {
        assert stat == monitoredVm;
        MonitoredData data = new SVMMonitoredDataImpl(this, monitoredVm, jvmstatModel);
        notifyListeners(data);
    }

    void notifyListeners(final MonitoredData data) {
        List<MonitoredDataListener> listenersCopy;
        synchronized (listeners) {
            listenersCopy = new ArrayList<>(listeners);
        }
        for (MonitoredDataListener listener : listenersCopy) {
            listener.monitoredDataEvent(data);
        }
    }

    private void waitDumpDone(Path name) throws IOException {
        long size;
        long newSize = Files.size(name);
        do {
            size = newSize;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                return;
            }
            newSize = Files.size(name);
        } while (size != newSize);
    }

}
