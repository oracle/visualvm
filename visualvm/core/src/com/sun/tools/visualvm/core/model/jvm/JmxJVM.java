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

package com.sun.tools.visualvm.core.model.jvm;

import com.sun.management.HotSpotDiagnosticMXBean;
import com.sun.management.VMOption;
import com.sun.tools.visualvm.core.datasource.Application;
import com.sun.tools.visualvm.core.datasource.Host;
import com.sun.tools.visualvm.core.datasupport.DataFinishedListener;
import com.sun.tools.visualvm.core.heapdump.HeapDumpSupport;
import com.sun.tools.visualvm.core.model.jmx.JvmJmxModel;
import com.sun.tools.visualvm.core.threaddump.ThreadDumpSupport;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.LockInfo;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MonitorInfo;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import javax.management.ObjectName;
import javax.swing.Timer;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Tomas Hurka
 */
class JmxJVM extends DefaultJVM implements DataFinishedListener<Application> {

    private static final int DEFAULT_REFRESH = 2000;
    private static final String PERM_GEN = "Perm Gen";
    private static final String PS_PERM_GEN = "PS Perm Gen";
    private static final String HOTSPOT_DIAGNOSTIC_MXBEAN_NAME =
            "com.sun.management:type=HotSpotDiagnostic";
    private Application app;
    private JvmJmxModel jmxModel;
    private Properties systemProperties;
    private String jvmArgs;
    private Set<MonitoredDataListener> listeners;
    private Timer timer;
    private MemoryPoolMXBean permGenPool;
    private HotSpotDiagnosticMXBean hotspotDiagnosticMXBean;
    private Object hotspotDiagnosticMXBeanLock = new Object();

    JmxJVM(Application application, JvmJmxModel model) {
        app = application;
        jmxModel = model;
        listeners = new HashSet();
    }

    public boolean is14() {
        return getVmVersion().startsWith("1.4.");
    }

    public boolean is15() {
        return getVmVersion().startsWith("1.5.");
    }

    public boolean is16() {
        return getVmVersion().startsWith("1.6.");
    }

    public boolean is17() {
        return getVmVersion().startsWith("1.7.");
    }

    public boolean isAttachable() {
        return false;
    }

    public String getCommandLine() {
        return "Unknown";
    }

    public synchronized String getJvmArgs() {
        if (jvmArgs == null) {
            StringBuilder buf = new StringBuilder();
            RuntimeMXBean runtimeMXBean = jmxModel.getRuntimeMXBean();
            if (runtimeMXBean == null) {
                return "";
            }
            List<String> args = runtimeMXBean.getInputArguments();
            for (String arg : args) {
                buf.append(arg).append(' ');
            }
            jvmArgs = buf.toString();
        }
        return jvmArgs;
    }

    public String getJvmFlags() {
        return null;
    }

    public String getMainArgs() {
        return null;
    }

    public String getMainClass() {
        return null;
    }

    public String getVmVersion() {
        return findByName("java.vm.version");
    }

    public String getJavaHome() {
        return findByName("java.home");
    }

    public String getVMInfo() {
        return findByName("java.vm.info");
    }

    public String getVMName() {
        return findByName("java.vm.name");
    }

    public synchronized Properties getSystemProperties() {
        if (systemProperties == null) {
            Map propMap = jmxModel.getRuntimeMXBean().getSystemProperties();
            systemProperties = new Properties();
            systemProperties.putAll(propMap);
        }
        return systemProperties;
    }

    public void addMonitoredDataListener(MonitoredDataListener l) {
        synchronized (listeners) {
            if (listeners.isEmpty()) {
                initTimer();
            }
            listeners.add(l);
        }
    }

    public void removeMonitoredDataListener(MonitoredDataListener l) {
        synchronized (listeners) {
            listeners.remove(l);
            if (listeners.isEmpty()) {
                disableTimer();
            }
        }
    }

    private HotSpotDiagnosticMXBean getHotSpotDiagnosticMXBean() {
        synchronized (hotspotDiagnosticMXBeanLock) {
            if (hotspotDiagnosticMXBean == null) {
                try {
                    hotspotDiagnosticMXBean = jmxModel.getMXBean(
                            ObjectName.getInstance(HOTSPOT_DIAGNOSTIC_MXBEAN_NAME),
                            HotSpotDiagnosticMXBean.class);
                } catch (Exception e) {
                    LOGGER.warning("Couldn't find HotSpotDiagnosticMXBean: " +
                            e.getLocalizedMessage());
                }
            }
            return hotspotDiagnosticMXBean;
        }
    }

    public boolean isDumpOnOOMEnabled() {
        HotSpotDiagnosticMXBean hsd = getHotSpotDiagnosticMXBean();
        // Get HeapDumpOnOutOfMemoryError
        VMOption vmOption = hsd.getVMOption("HeapDumpOnOutOfMemoryError");
        return Boolean.valueOf(vmOption.getValue());
    }

    public void setDumpOnOOMEnabled(boolean enabled) {
        HotSpotDiagnosticMXBean hsd = getHotSpotDiagnosticMXBean();
        if (enabled) {
            // Set HeapDumpPath
            hsd.setVMOption("HeapDumpPath",
                    app.getStorage().getDirectory().getAbsolutePath());
        }
        // Set HeapDumpOnOutOfMemoryError
        hsd.setVMOption("HeapDumpOnOutOfMemoryError", enabled ? "true" : "false");
    }

    private String takeHeapDump(File dumpFile) {
        try {
            HotSpotDiagnosticMXBean hsd = getHotSpotDiagnosticMXBean();
            hsd.dumpHeap(dumpFile.getAbsolutePath(), true);
            InputStream in = new FileInputStream(dumpFile);
            StringBuffer buffer = new StringBuffer(1024);
            // read to EOF and just print output
            byte b[] = new byte[256];
            int n;
            do {
                n = in.read(b);
                if (n > 0) {
                    String s = new String(b, 0, n, "UTF-8");
                    buffer.append(s);
                }
            } while (n > 0);
            in.close();
            return buffer.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "Cannot get heap dump " + e.getLocalizedMessage(); // NOI18N
        }
    }

    public File takeHeapDump() throws IOException {
        File snapshotDir = app.getStorage().getDirectory();
        String name = HeapDumpSupport.getInstance().getCategory().createFileName();
        File dumpFile = new File(snapshotDir,name);
        String dump = takeHeapDump(dumpFile);
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Dump " + dump);
        }
        return dumpFile;
    }

    private String runThreadDump() throws Exception {
        ThreadMXBean threadMXBean = jmxModel.getThreadMXBean();
        RuntimeMXBean runtimeMXBean = jmxModel.getRuntimeMXBean();
        StringBuffer sb = new StringBuffer(1024);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sb.append(df.format(new Date()) + "\n");
        sb.append("Full thread dump " + runtimeMXBean.getVmName() +
                " (" + runtimeMXBean.getVmVersion() + " " +
                getSystemProperties().getProperty("java.vm.info") + "):\n");
        if (is15()) {
            long[] threadIds = threadMXBean.getAllThreadIds();
            for (long threadId : threadIds) {
                ThreadInfo thread = threadMXBean.getThreadInfo(threadId, Integer.MAX_VALUE);
                sb.append("\n\"" + thread.getThreadName() +
                        "\" - Thread t@" + thread.getThreadId() + "\n");
                sb.append("   java.lang.Thread.State: " + thread.getThreadState());
                if (thread.getLockName() != null) {
                    sb.append(" on " + thread.getLockName());
                    if (thread.getLockOwnerName() != null) {
                        sb.append(" owned by: " + thread.getLockOwnerName());
                    }
                }
                sb.append("\n");
                for (StackTraceElement st : thread.getStackTrace()) {
                    sb.append("        at " + st.toString() + "\n");
                }
            }
            return sb.toString();
        } else {
            ThreadInfo[] threads = threadMXBean.dumpAllThreads(true, true);
            for (ThreadInfo thread : threads) {
                MonitorInfo[] monitors = null;
                if (threadMXBean.isObjectMonitorUsageSupported()) {
                    monitors = thread.getLockedMonitors();
                }
                sb.append("\n\"" + thread.getThreadName() +
                        "\" - Thread t@" + thread.getThreadId() + "\n");
                sb.append("   java.lang.Thread.State: " + thread.getThreadState());
                if (thread.getLockName() != null) {
                    sb.append(" on " + thread.getLockName());
                    if (thread.getLockOwnerName() != null) {
                        sb.append(" owned by: " + thread.getLockOwnerName());
                    }
                }
                sb.append("\n");
                int index = 0;
                for (StackTraceElement st : thread.getStackTrace()) {
                    sb.append("        at " + st.toString() + "\n");
                    if (monitors != null) {
                        for (MonitorInfo mi : monitors) {
                            if (mi.getLockedStackDepth() == index) {
                                sb.append("        - locked " + mi.toString() + "\n");
                            }
                        }
                    }
                    index++;
                }
                if (threadMXBean.isSynchronizerUsageSupported()) {
                    sb.append("\n   Locked ownable synchronizers:");
                    LockInfo[] synchronizers = thread.getLockedSynchronizers();
                    if (synchronizers != null || synchronizers.length == 0) {
                        sb.append("\n        - None\n");
                    } else {
                        for (LockInfo li : synchronizers) {
                            sb.append("\n        - locked " + li.toString() + "\n");
                        }
                    }
                }
            }
            return sb.toString();
        }
    }

    private String getStackTrace() {
        try {
            return runThreadDump();
        } catch (Exception e) {
            e.printStackTrace();
            return "Cannot get thread dump " + e.getLocalizedMessage(); // NOI18N
        }
    }

    public File takeThreadDump() throws IOException {
        String dump = getStackTrace();
        File snapshotDir = app.getStorage().getDirectory();
        String name = ThreadDumpSupport.getInstance().getCategory().createFileName();
        File dumpFile = new File(snapshotDir, name);
        OutputStream os = new FileOutputStream(dumpFile);
        os.write(dump.getBytes("UTF-8"));
        os.close();
        return dumpFile;
    }

    public boolean isBasicInfoSupported() {
        return true;
    }

    public boolean isMonitoringSupported() {
        return isClassMonitoringSupported() || isThreadMonitoringSupported() || isMemoryMonitoringSupported();
    }

    public boolean isClassMonitoringSupported() {
        return true;
    }

    public boolean isThreadMonitoringSupported() {
        return true;
    }

    public boolean isMemoryMonitoringSupported() {
        return true;
    }

    public boolean isGetSystemPropertiesSupported() {
        return !is14();
    }

    private boolean isHeapDumpSupported() {
        if (is14() || is15()) {
            return false;
        }
        if (!app.getHost().equals(Host.LOCALHOST)) {
            return false;
        }
        if (getHotSpotDiagnosticMXBean() == null) {
            return false;
        }
        return true;
    }

    public boolean isDumpOnOOMEnabledSupported() {
        return isHeapDumpSupported();
    }

    public boolean isTakeHeapDumpSupported() {
        return isHeapDumpSupported();
    }

    public boolean isTakeThreadDumpSupported() {
        return is14() ? false : (jmxModel.getThreadMXBean() != null);
    }

    private String findByName(String key) {
        Properties p = getSystemProperties();
        if (p == null) {
            return null;
        }
        return p.getProperty(key);
    }

    private void initTimer() {
        timer = new Timer(DEFAULT_REFRESH, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                RequestProcessor.getDefault().post(new Runnable() {
                    public void run() {
                        computeMonitoredData();
                    }
                });
            }
        });
        timer.setCoalesce(true);
        timer.start();
    }

    private void disableTimer() {
        if (timer != null) {
            timer.stop();
        }
    }

    private void computeMonitoredData() {
        MonitoredData data = new MonitoredData(this);
        List<MonitoredDataListener> listenersCopy;
        synchronized (listeners) {
            listenersCopy = new ArrayList(listeners);
        }
        for (MonitoredDataListener listener : listenersCopy) {
            listener.monitoredDataEvent(data);
        }
    }

    JvmJmxModel getJmxModel() {
        return jmxModel;
    }

    public void dataFinished(Application dataSource) {
        disableTimer();
    }

    MemoryPoolMXBean getPermGenPool() {
        if (permGenPool == null) {
            Collection<MemoryPoolMXBean> pools = jmxModel.getMemoryPoolMXBeans();
            for (MemoryPoolMXBean pool : pools) {
                if (pool.getType().equals(MemoryType.NON_HEAP) &&
                        (PERM_GEN.equals(pool.getName()) ||
                        PS_PERM_GEN.equals(pool.getName()))) {
                    permGenPool = pool;
                    break;
                }
            }
        }
        return permGenPool;
    }
}
