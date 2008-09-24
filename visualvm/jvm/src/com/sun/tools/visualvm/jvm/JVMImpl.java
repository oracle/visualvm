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

package com.sun.tools.visualvm.jvm;

import com.sun.management.HotSpotDiagnosticMXBean;
import com.sun.management.OperatingSystemMXBean;
import com.sun.tools.visualvm.application.jvm.Jvm;
import com.sun.tools.visualvm.application.jvm.MonitoredData;
import com.sun.tools.visualvm.application.jvm.MonitoredDataListener;
import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.heapdump.HeapDumpSupport;
import com.sun.tools.visualvm.host.Host;
import com.sun.tools.visualvm.threaddump.ThreadDumpSupport;
import com.sun.tools.visualvm.tools.attach.AttachModel;
import com.sun.tools.visualvm.tools.attach.AttachModelFactory;
import com.sun.tools.visualvm.tools.jvmstat.JvmstatModel;
import com.sun.tools.visualvm.tools.jvmstat.JvmstatListener;
import com.sun.tools.visualvm.tools.jvmstat.JvmJvmstatModel;
import com.sun.tools.visualvm.tools.jvmstat.JvmJvmstatModelFactory;
import com.sun.tools.visualvm.tools.sa.SaModel;
import com.sun.tools.visualvm.tools.sa.SaModelFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 *
 * @author Tomas Hurka
 */
public class JVMImpl extends Jvm implements JvmstatListener {
    private static final String HEAP_DUMP_ON_OOME = "HeapDumpOnOutOfMemoryError";   // NOI18N
    Application application;
    Boolean isDumpOnOOMEnabled;
    JvmstatModel monitoredVm;
    JvmJvmstatModel jvmstatModel;
    Set<MonitoredDataListener> listeners;
    JmxSupport jmxSupport;
    
    // static JVM data 
    private boolean staticDataInitialized; 
    private Object staticDataLock = new Object();
    private String commandLine;
    private String jvmArgs;
    private String jvmFlags;
    private String mainArgs;
    private String mainClass;
    private String vmVersion;
    private String javaHome;
    private String vmInfo;
    private String vmName;
    private String vmVendor;

 
    JVMImpl(Application app,JvmstatModel jvms) {
        application = app;
        monitoredVm = jvms;
        jvmstatModel = JvmJvmstatModelFactory.getJvmstatModelFor(app);
        jmxSupport = new JmxSupport(app,this);
        listeners = new HashSet();
    }
    
    JVMImpl(Application app) {
        application = app;
        jmxSupport = new JmxSupport(app,this);
        listeners = new HashSet();
    }
    
    public boolean isAttachable() {
        if (jvmstatModel != null) {
            return jvmstatModel.isAttachable();
        }
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
        String ver = getVmVersion();
        if (ver != null && ver.startsWith("1.4.")) {    // NOI18N
            return true;
        }
        return false;
    }
    
    public boolean is15() {
        String ver = getVmVersion();
        if (ver != null && ver.startsWith("1.5.")) {    // NOI18N
            return true;
        }
        return false;
    }
    
    public boolean is16() {
        String ver = getVmVersion();
        if (ver != null && (ver.startsWith("1.6.") || ver.startsWith("10.0") || ver.startsWith("11.0"))) {    // NOI18N
            return true;
        }
        return false;
    }
    
    public boolean is17() {
        String ver = getVmVersion();
        if (ver != null && (ver.startsWith("1.7.") || ver.startsWith("12.0") || ver.startsWith("13.0") || ver.startsWith("14.0"))) {  // NOI18N
            return true;
        }
        return false;
    }
    
    public boolean isDumpOnOOMEnabled() {
        if (isDumpOnOOMEnabled == null) {
            AttachModel attach = getAttach();
            String args = null;
            if (attach != null) {
                args = attach.printFlag(HEAP_DUMP_ON_OOME);
            }
            if (args == null) {
                HotSpotDiagnosticMXBean hsDiagnostic = jmxSupport.getHotSpotDiagnostic();
                if (hsDiagnostic != null) {
                    String value = hsDiagnostic.getVMOption(HEAP_DUMP_ON_OOME).getValue();
                    isDumpOnOOMEnabled = Boolean.valueOf(value);
                    return isDumpOnOOMEnabled.booleanValue();
                }
            }
            if (args == null && monitoredVm != null) {
                args = getJvmFlags().concat(getJvmArgs());
            }
            if (args != null && args.contains("-XX:+"+HEAP_DUMP_ON_OOME)) { // NOI18N
                isDumpOnOOMEnabled = Boolean.TRUE;
            } else {
                isDumpOnOOMEnabled = Boolean.FALSE;
            }
        }
        return isDumpOnOOMEnabled.booleanValue();
    }
    
    public void addMonitoredDataListener(MonitoredDataListener l) {
        synchronized (listeners) {
            if (listeners.add(l)) {
                if (monitoredVm != null) {
                    if (jmxSupport != null) jmxSupport.disableTimer();
                    monitoredVm.addJvmstatListener(this);
                } else {
                    if (jmxSupport != null) jmxSupport.initTimer();
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
                    } else {
                        if (jmxSupport != null) jmxSupport.disableTimer();
                    }
                }
            }
        }
    }
    
    public boolean isMonitoringSupported() {
        return isClassMonitoringSupported() || isThreadMonitoringSupported() || isMemoryMonitoringSupported();
    }
    
    public boolean isClassMonitoringSupported() {
        return monitoredVm != null || jmxSupport.getRuntime() != null;
    }
    
    public boolean isThreadMonitoringSupported() {
        return (!is14() && monitoredVm != null) || jmxSupport.getRuntime() != null;
    }
    
    public boolean isMemoryMonitoringSupported() {
        return monitoredVm != null || jmxSupport.getRuntime() != null;
    }
    
    public boolean isGetSystemPropertiesSupported() {
        return getAttach() != null || jmxSupport.getRuntime() != null || getSAAgent() != null;
    }
    
    public Properties getSystemProperties() {
        AttachModel attach = getAttach();
        Properties prop = null;
        
        if (attach != null) {
            prop = attach.getSystemProperties();
        }
        if (prop != null)
            return prop;
        prop = jmxSupport.getSystemProperties();
        if (prop != null) {
            return prop;
        }
        SaModel saAgent = getSAAgent();
        if (saAgent != null) {
            return saAgent.getSystemProperties();
        }
        if (!isGetSystemPropertiesSupported()) {
            throw new UnsupportedOperationException();
        }
        return null;
    }
    
    public boolean isDumpOnOOMEnabledSupported() {
        return  getAttach() != null || (Host.LOCALHOST.equals(application.getHost()) && jmxSupport.getHotSpotDiagnostic() != null); 
    }
    
    public synchronized void setDumpOnOOMEnabled(boolean enabled) {
        if (!isDumpOnOOMEnabledSupported()) {
            throw new UnsupportedOperationException();
        }
        AttachModel attach = getAttach();
        if (attach!=null) {
            attach.setFlag(HEAP_DUMP_ON_OOME,enabled?"1":"0");  // NOI18N
            if (enabled) {
                attach.setFlag("HeapDumpPath",application.getStorage().getDirectory().getAbsolutePath());   // NOI18N
            }
        } else {
            HotSpotDiagnosticMXBean hsDiagnostic = jmxSupport.getHotSpotDiagnostic();
            hsDiagnostic.setVMOption(HEAP_DUMP_ON_OOME,enabled?"true":"false"); // NOI18N
            if (enabled) {
                hsDiagnostic.setVMOption("HeapDumpPath",application.getStorage().getDirectory().getAbsolutePath()); // NOI18N
            }
        }
        Boolean oldVlue = isDumpOnOOMEnabled;
        isDumpOnOOMEnabled = Boolean.valueOf(enabled);
        firePropertyChange(PROPERTY_DUMP_OOME_ENABLED,oldVlue,isDumpOnOOMEnabled);
    }
    
    public boolean isTakeHeapDumpSupported() {
        return  getAttach() != null || (Host.LOCALHOST.equals(application.getHost()) && jmxSupport.getHotSpotDiagnostic() != null); 
    }
    
    public File takeHeapDump() throws IOException {
        if (!isTakeHeapDumpSupported()) {
            throw new UnsupportedOperationException();
        }
        File snapshotDir = application.getStorage().getDirectory();
        String name = HeapDumpSupport.getInstance().getCategory().createFileName();
        File dumpFile = new File(snapshotDir,name);
        AttachModel attach = getAttach();
        if (attach != null) {
            if (attach.takeHeapDump(dumpFile.getAbsolutePath())) {
                return dumpFile;
            }
            return null;
        }
        HotSpotDiagnosticMXBean hsDiagnostic = jmxSupport.getHotSpotDiagnostic();
        if (hsDiagnostic != null) {
            hsDiagnostic.dumpHeap(dumpFile.getAbsolutePath(),true);
            return dumpFile;
        }
        return null;
    }
    
    public boolean isTakeThreadDumpSupported() {
        return getAttach() != null || jmxSupport.getRuntime() != null || getSAAgent() != null;
    }
    
    public File takeThreadDump() throws IOException {
        AttachModel attach = getAttach();
        String threadDump = null;
        
        if (attach != null) {
            threadDump = attach.takeThreadDump();
        }
        if (threadDump == null) {
            threadDump = jmxSupport.takeThreadDump();
        }
        if (threadDump == null) {
            SaModel sa = getSAAgent();
            if (sa != null) {
                threadDump = sa.takeThreadDump();
            }
        }
        if (threadDump == null) {
            if (!isTakeThreadDumpSupported()) {
            throw new UnsupportedOperationException();
            }
            threadDump = org.openide.util.NbBundle.getMessage(JVMImpl.class, "MSG_ThreadDumpfailed");   // NOI18N
        }
        File snapshotDir = application.getStorage().getDirectory();
        String name = ThreadDumpSupport.getInstance().getCategory().createFileName();
        File dumpFile = new File(snapshotDir,name);
        OutputStream os = new FileOutputStream(dumpFile);
        os.write(threadDump.getBytes("UTF-8")); // NOI18N
        os.close();
        return dumpFile;
    }
    
    public boolean isCpuMonitoringSupported() {
        return jmxSupport.getOperationSystem() instanceof OperatingSystemMXBean;
    }
    
    public boolean isCollectionTimeSupported() {
        Collection gcList = jmxSupport.getGarbageCollectorMXBeans();
        return gcList != null && !gcList.isEmpty();
    }
    
    protected AttachModel getAttach() {
        return AttachModelFactory.getAttachFor(application);
    }
    
    protected SaModel getSAAgent() {
        return SaModelFactory.getSAAgentFor(application);
    }
    
    protected void initStaticData() {
        synchronized (staticDataLock) {
            if (staticDataInitialized) {
                return;
            }
            if (jvmstatModel != null) {
                commandLine = jvmstatModel.getCommandLine();
                jvmArgs = getJvmArgsJvmstat();
                jvmFlags = jvmstatModel.getJvmFlags();
                mainArgs = jvmstatModel.getMainArgs();
                mainClass = jvmstatModel.getMainClass();
                vmVersion = jvmstatModel.getVmVersion();
                javaHome = jvmstatModel.getJavaHome();
                vmInfo = jvmstatModel.getVmInfo();
                vmName = jvmstatModel.getVmName();
                vmVendor = jvmstatModel.getVmVendor();
            } else {
                jvmArgs = jmxSupport.getJvmArgs();
                Properties prop = jmxSupport.getSystemProperties();
                if (prop != null) {
                    vmVersion = prop.getProperty("java.vm.version");    // NOI18N
                    javaHome = prop.getProperty("java.home");   // NOI18N
                    vmInfo = prop.getProperty("java.vm.info");  // NOI18N
                    vmName = prop.getProperty("java.vm.name");  // NOI18N
                    vmVendor = prop.getProperty("java.vm.vendor");  // NOI18N
                }
            }
            staticDataInitialized = true;
        }
    }
    
    private String getJvmArgsJvmstat() {
        String args = jvmstatModel.getJvmArgs();
        if (args.length() == 1024) {
            args = jmxSupport.getJvmArgs();
            if (args == null) {
                SaModel sa = getSAAgent();
                if (sa != null) {
                    args = sa.getJvmArgs();
                }
            }
            if (args == null || args.length() == 0) {
                args = jvmstatModel.getJvmArgs();
            }
        }
        return args;
    }

    public void dataChanged(JvmstatModel stat) {
        assert stat == monitoredVm;
        MonitoredData data = new MonitoredDataImpl(this,jvmstatModel,jmxSupport);
        notifyListeners(data);        
    }

    void notifyListeners(final MonitoredData data) {
        List<MonitoredDataListener> listenersCopy;
        synchronized (listeners) {
            listenersCopy = new ArrayList(listeners);
        }
        for (MonitoredDataListener listener : listenersCopy) {
            listener.monitoredDataEvent(data);
        }        
    }

}
