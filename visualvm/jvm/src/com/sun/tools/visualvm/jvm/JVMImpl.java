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
import com.sun.tools.visualvm.application.JVM;
import com.sun.tools.visualvm.application.MonitoredDataListener;
import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.heapdump.HeapDumpSupport;
import com.sun.tools.visualvm.host.Host;
import com.sun.tools.visualvm.threaddump.ThreadDumpSupport;
import com.sun.tools.visualvm.tools.attach.Attach;
import com.sun.tools.visualvm.tools.attach.AttachFactory;
import com.sun.tools.visualvm.tools.jmx.JvmJmxModel;
import com.sun.tools.visualvm.tools.jmx.JvmJmxModelFactory;
import com.sun.tools.visualvm.tools.jvmstat.Jvmstat;
import com.sun.tools.visualvm.tools.sa.SAAgent;
import com.sun.tools.visualvm.tools.sa.SAAgentFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.RuntimeMXBean;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 *
 * @author Tomas Hurka
 */
public class JVMImpl extends JVM {
    private static final String HOTSPOT_DIAGNOSTIC_MXBEAN_NAME =
            "com.sun.management:type=HotSpotDiagnostic";
    private static final String HEAP_DUMP_ON_OOME = "HeapDumpOnOutOfMemoryError";
    Application application;
    Boolean isDumpOnOOMEnabled;
    Jvmstat monitoredVm;
    Set<MonitoredDataListener> listeners;
    
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

    // HotspotDiagnostic 
    private boolean hotspotDiagnosticInitialized;
    private Object hotspotDiagnosticLock = new Object();
    private HotSpotDiagnosticMXBean hotspotDiagnosticMXBean;
 
    JVMImpl(Application app,Jvmstat jvms) {
        application = app;
        monitoredVm = jvms;
        listeners = new HashSet();
    }
    
    JVMImpl(Application app,JvmJmxModel jmx) {
        application = app;
        listeners = new HashSet();
    }
    
    public boolean isAttachable() {
        if (monitoredVm != null) {
            return monitoredVm.isAttachable();
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
    
    public String getVMInfo() {
        initStaticData();
        return vmInfo;
    }
    
    public String getVMName() {
        initStaticData();
        return vmName;
    }
    
    public boolean is14() {
        String ver = getVmVersion();
        if (ver != null && ver.startsWith("1.4.")) {
            return true;
        }
        return false;
    }
    
    public boolean is15() {
        String ver = getVmVersion();
        if (ver != null && ver.startsWith("1.5.")) {
            return true;
        }
        return false;
    }
    
    public boolean is16() {
        String ver = getVmVersion();
        if (ver != null && (ver.startsWith("1.6.") || ver.startsWith("10.0"))) {
            return true;
        }
        return false;
    }
    
    public boolean is17() {
        String ver = getVmVersion();
        if (ver != null && (ver.startsWith("1.7.") || ver.startsWith("11.0") || ver.startsWith("12.0"))) {
            return true;
        }
        return false;
    }
    
    public boolean isDumpOnOOMEnabled() {
        if (isDumpOnOOMEnabled == null) {
            Attach attach = getAttach();
            String args = null;
            if (attach != null) {
                args = attach.printFlag(HEAP_DUMP_ON_OOME);
            }
            HotSpotDiagnosticMXBean hsDiagnostic = getHotSpotDiagnostic();
            if (args == null && hsDiagnostic != null) {
                String value = hsDiagnostic.getVMOption(HEAP_DUMP_ON_OOME).getValue();
                isDumpOnOOMEnabled = Boolean.valueOf(value);
                return isDumpOnOOMEnabled.booleanValue();
            }
            if (args == null && monitoredVm != null) {
                args = getJvmFlags().concat(getJvmArgs());
            }
            if (args != null && args.contains("-XX:+"+HEAP_DUMP_ON_OOME)) {
                isDumpOnOOMEnabled = Boolean.TRUE;
            } else {
                isDumpOnOOMEnabled = Boolean.FALSE;
            }
        }
        return isDumpOnOOMEnabled.booleanValue();
    }
    
    public void addMonitoredDataListener(MonitoredDataListener l) {
        
    }
    
    public void removeMonitoredDataListener(MonitoredDataListener l) {
    }
    
    public boolean isMonitoringSupported() {
        return false;
    }
    
    public boolean isClassMonitoringSupported() {
        return false;
    }
    
    public boolean isThreadMonitoringSupported() {
        return false;
    }
    
    public boolean isMemoryMonitoringSupported() {
        return false;
    }
    
    public boolean isGetSystemPropertiesSupported() {
        return getAttach() != null || getJXM() != null || getSAAgent() != null;
    }
    
    public Properties getSystemProperties() {
        Attach attach = getAttach();
        Properties prop = null;
        
        if (attach != null) {
            prop = attach.getSystemProperties();
        }
        if (prop != null)
            return prop;
        RuntimeMXBean runtime = getRuntime();
        if (runtime != null) {
            prop = new Properties();
            prop.putAll(runtime.getSystemProperties());
            return prop;
        }
        SAAgent saAgent = getSAAgent();
        if (saAgent != null) {
            return saAgent.getSystemProperties();
        }
        if (!isGetSystemPropertiesSupported()) {
            throw new UnsupportedOperationException();
        }
        return null;
    }
    
    public boolean isDumpOnOOMEnabledSupported() {
        return  getAttach() != null || (Host.LOCALHOST.equals(application.getHost()) && getHotSpotDiagnostic() != null); 
    }
    
    public void setDumpOnOOMEnabled(boolean enabled) {
        if (!isDumpOnOOMEnabledSupported()) {
            throw new UnsupportedOperationException();
        }
        Attach attach = getAttach();
        if (attach!=null) {
            attach.setFlag(HEAP_DUMP_ON_OOME,enabled?"1":"0");
            if (enabled) {
                attach.setFlag("HeapDumpPath",application.getStorage().getDirectory().getAbsolutePath());
            }
        } else {
            HotSpotDiagnosticMXBean hsDiagnostic = getHotSpotDiagnostic();
            hsDiagnostic.setVMOption(HEAP_DUMP_ON_OOME,enabled?"true":"false");
            if (enabled) {
                attach.setFlag("HeapDumpPath",application.getStorage().getDirectory().getAbsolutePath());
            }
        }
        isDumpOnOOMEnabled = Boolean.valueOf(enabled);
    }
    
    public boolean isTakeHeapDumpSupported() {
        return  getAttach() != null || (Host.LOCALHOST.equals(application.getHost()) && getHotSpotDiagnostic() != null); 
    }
    
    public File takeHeapDump() throws IOException {
        if (!isTakeHeapDumpSupported()) {
            throw new UnsupportedOperationException();
        }
        File snapshotDir = application.getStorage().getDirectory();
        String name = HeapDumpSupport.getInstance().getCategory().createFileName();
        File dumpFile = new File(snapshotDir,name);
        Attach attach = getAttach();
        if (attach != null) {
            if (attach.takeHeapDump(dumpFile.getAbsolutePath())) {
                return dumpFile;
            }
            return null;
        }
        HotSpotDiagnosticMXBean hsDiagnostic = getHotSpotDiagnostic();
        if (hsDiagnostic != null) {
//            hsDiagnostic.dumpHeap(dumpFile.getAbsolutePath(),true);
            return dumpFile;
        }
        return null;
    }
    
    public boolean isTakeThreadDumpSupported() {
        return getAttach() != null || getSAAgent() != null;
    }
    
    public File takeThreadDump() throws IOException {
        Attach attach = getAttach();
        String threadDump = null;
        
        if (attach != null) {
            threadDump = attach.takeThreadDump();
        }
        if (threadDump == null) {
            SAAgent sa = getSAAgent();
            if (sa != null) {
                threadDump = sa.takeThreadDump();
            }
        }
        if (threadDump == null) {
            if (!isTakeThreadDumpSupported()) {
            throw new UnsupportedOperationException();
            }
            threadDump = "Take Thread Dump failed";
        }
        File snapshotDir = application.getStorage().getDirectory();
        String name = ThreadDumpSupport.getInstance().getCategory().createFileName();
        File dumpFile = new File(snapshotDir,name);
        OutputStream os = new FileOutputStream(dumpFile);
        os.write(threadDump.getBytes("UTF-8"));
        os.close();
        return dumpFile;
    }
    
    protected JvmJmxModel getJXM() {
        return JvmJmxModelFactory.getJvmJmxModelFor(application);
    }
    
    protected Attach getAttach() {
        return AttachFactory.getAttachFor(application);
    }
    
    protected SAAgent getSAAgent() {
        return SAAgentFactory.getSAAgentFor(application);
    }
    
    protected RuntimeMXBean getRuntime() {
        JvmJmxModel jmx = getJXM();
        if (jmx != null) return jmx.getRuntimeMXBean();
        return null;
    }
    
    protected void initStaticData() {
        synchronized (staticDataLock) {
            if (staticDataInitialized) {
                return;
            }
            if (monitoredVm != null) {
                commandLine = monitoredVm.getCommandLine();
                jvmArgs = getJvmArgsJvmstat();
                jvmFlags = monitoredVm.getJvmFlags();
                mainArgs = monitoredVm.getMainArgs();
                mainClass = getMainClassJvmstat();
                vmVersion = monitoredVm.getVmVersion();
                javaHome = monitoredVm.getJavaHome();
                vmInfo = monitoredVm.getVMInfo();
                vmName = monitoredVm.getVMName();
            } else {
                RuntimeMXBean runtime = getRuntime();
                if (runtime != null) {
                    Map<String,String> propMap = runtime.getSystemProperties();
                    jvmArgs = getJvmArgsJmx();
                    vmVersion = propMap.get("java.vm.version");
                    javaHome = propMap.get("java.home");
                    vmInfo = propMap.get("java.vm.info");
                    vmName = propMap.get("java.vm.name");
                }
            }
            staticDataInitialized = true;
        }
    }
    
    private String getJvmArgsJvmstat() {
        String args = monitoredVm.getJvmArgs();
        if (args.length() == 1024) {
            args = getJvmArgsJmx();
            if (args == null) {
                SAAgent sa = getSAAgent();
                if (sa != null) {
                    args = sa.getJVMArgs();
                }
            }
            if (args == null || args.length() == 0) {
                args = monitoredVm.getJvmArgs();
            }
        }
        return args;
    }
    
    private String getMainClassJvmstat() {
        String mainClassName = monitoredVm.getMainClass();
        if (mainClassName == null) {
            return null;
        }
        if (application.getHost().equals(Host.LOCALHOST)) {
            File jarFile = new File(mainClassName);
            if (jarFile.exists()) {
                try {
                    JarFile jf = new JarFile(jarFile);
                    mainClassName = jf.getManifest().getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
                    assert mainClassName!=null;
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        if (mainClassName.endsWith(".jar")) {
            mainClassName = mainClassName.replace('\\', '/');
            int index = mainClassName.lastIndexOf("/");
            if (index != -1) {
                mainClassName = mainClassName.substring(index + 1);
            }
        }
        mainClassName = mainClassName.replace('\\', '/').replace('/', '.');
        return mainClassName;
    }
    
    private String getJvmArgsJmx() {
        RuntimeMXBean runtime = getRuntime();
        if (runtime != null) {
            StringBuilder buf = new StringBuilder();
            List<String> args = runtime.getInputArguments();
            for (String arg : args) {
                buf.append(arg).append(' ');
            }
            return buf.toString();
        }
        return null;
    }

    HotSpotDiagnosticMXBean getHotSpotDiagnostic() {
        synchronized (hotspotDiagnosticLock) {
            if (hotspotDiagnosticInitialized) {
                return hotspotDiagnosticMXBean;
            }
            JvmJmxModel jmxModel = getJXM();
            if (jmxModel != null) {
                try {
                    hotspotDiagnosticMXBean = jmxModel.getMXBean(
                            ObjectName.getInstance(HOTSPOT_DIAGNOSTIC_MXBEAN_NAME),
                            HotSpotDiagnosticMXBean.class);
                } catch (MalformedObjectNameException e) {
                    LOGGER.warning("Couldn't find HotSpotDiagnosticMXBean: " +
                            e.getLocalizedMessage());
                }
            }
            hotspotDiagnosticInitialized = true;
            return hotspotDiagnosticMXBean;
        }
    }

}
