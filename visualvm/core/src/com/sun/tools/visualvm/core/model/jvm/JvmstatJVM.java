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

import com.sun.tools.visualvm.core.application.JvmstatApplication;
import com.sun.tools.visualvm.core.datasource.Host;
import com.sun.tools.visualvm.core.datasupport.DataFinishedListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import sun.jvmstat.monitor.LongMonitor;
import sun.jvmstat.monitor.Monitor;
import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.MonitoredVm;
import sun.jvmstat.monitor.MonitoredVmUtil;
import sun.jvmstat.monitor.event.MonitorStatusChangeEvent;
import sun.jvmstat.monitor.event.VmEvent;
import sun.jvmstat.monitor.event.VmListener;
import sun.management.counter.Variability;

/**
 *
 * @author Tomas Hurka
 */
public abstract class JvmstatJVM extends DefaultJVM implements VmListener, DataFinishedListener {
    JvmstatApplication application;
    Boolean isDumpOnOOMEnabled;
    MonitoredVm monitoredVm;
    Set<MonitoredDataListener> listeners;
    LongMonitor loadedClasses;
    LongMonitor sharedLoadedClasses;
    LongMonitor sharedUnloadedClasses;
    LongMonitor unloadedClasses;
    LongMonitor threadsDaemon;
    LongMonitor threadsLive;
    LongMonitor threadsLivePeak;
    LongMonitor threadsStarted;
    LongMonitor applicationTime;
    LongMonitor upTime;
    long osFrequency;
    List<LongMonitor> genCapacity;
    List<LongMonitor> genUsed;
    long[] genMaxCapacity;
    private Map<String,String> valueCache;
    
    JvmstatJVM(JvmstatApplication app,MonitoredVm vm) {
        application = app;
        monitoredVm = vm;
        valueCache = new HashMap();
    }
    
    public boolean is14() {
        return true;
    }
    
    public boolean isAttachable() {
        try {
            return MonitoredVmUtil.isAttachable(monitoredVm);
        } catch (MonitorException ex) {
            ex.printStackTrace();
        }
        return false;
    }
    
    public boolean isBasicInfoSupported() {
        return true;
    }
    
    public String getCommandLine() {
        return findByName("sun.rt.javaCommand");
    }
    
    public String getJvmArgs() {
        return findByName("java.rt.vmArgs");
    }
    
    public String getJvmFlags() {
        return findByName("java.rt.vmFlags");
    }
    
    public String getMainArgs() {
        try {
            return MonitoredVmUtil.mainArgs(monitoredVm);
        } catch (MonitorException ex) {
            ex.printStackTrace();
        }
        return null;
    }
    
    public String getMainClass() {
        String mainClassName;
        try {
            mainClassName = MonitoredVmUtil.mainClass(monitoredVm,true);
        } catch (MonitorException ex) {
            ex.printStackTrace();
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
        return mainClassName.replace('/','.');
    }
    
    public String getVmVersion() {
        return findByName("java.property.java.vm.version");
    }
    
    long getOsFrequency() {
        return osFrequency;
    }
    
    abstract String getPermGenPrefix();
    
    public String getJavaHome() {
        return findByName("java.property.java.home");
    }
    
    public String getVMInfo() {
        return findByName("java.property.java.vm.info");
    }
    
    public String getVMName() {
        return findByName("java.property.java.vm.name");
    }
    
    public boolean isDumpOnOOMEnabled() {
        if (isDumpOnOOMEnabled == null) {
            String args = getJvmFlags().concat(getJvmArgs());
            if (args.contains("-XX:+HeapDumpOnOutOfMemoryError")) {
                isDumpOnOOMEnabled = Boolean.TRUE;
            } else {
                isDumpOnOOMEnabled = Boolean.FALSE;
            }
        }
        return isDumpOnOOMEnabled.booleanValue();
    }
    
    public synchronized void addMonitoredDataListener(MonitoredDataListener l) {
        if (listeners == null) {
            initListeners();
        }
        listeners.add(l);
    }
    
    public synchronized void removeMonitoredDataListener(MonitoredDataListener l) {
        if (listeners != null) {
            listeners.remove(l);
            if (listeners.isEmpty()) {
                disableListeners();
            }
        }
    }
    
    public String findByName(String name) {
        String value = valueCache.get(name);
        if (value != null) return value;
        
        try {
            Monitor mon = monitoredVm.findByName(name);
            if (mon != null) {
                value = mon.getValue().toString();
                if (mon.getVariability().equals(Variability.CONSTANT)) {
                    valueCache.put(name,value);
                }
            }
            return value;
        } catch (MonitorException ex) {
            ex.printStackTrace();
        }
        return null;
    }
    
    public List<String> findByPattern(String pattern) {
        try {
            List<Monitor> monitorList = monitoredVm.findByPattern(pattern);
            List<String> monitorStrList = new ArrayList<String>(monitorList.size());
            for (Monitor monitor : monitorList) {
                monitorStrList.add(monitor.getValue().toString());
            }
            return monitorStrList;
        } catch (MonitorException ex) {
            ex.printStackTrace();
        }
        return null;
    }
    
    abstract void initListeners();
    
    void disableListeners() {
        try {
            monitoredVm.removeVmListener(this);
        } catch (MonitorException ex) {
            ex.printStackTrace();
        }
        listeners = null;
    }
    
    long[] getGenerationSum(List<LongMonitor> counters) {
        long[] results=new long[2];
        String prefix = getPermGenPrefix();
        
        for (LongMonitor counter : counters) {
            if (counter != null) {
                long val = counter.longValue();
                if (counter.getName().startsWith(prefix)) {
                    results[1]+= val;
                } else {
                    results[0]+= val;
                }
            }
        }
        return results;
    }
    
    LongMonitor getLoadedClasses() {
        return loadedClasses;
    }
    
    LongMonitor getSharedLoadedClasses() {
        return sharedLoadedClasses;
    }
    
    LongMonitor getSharedUnloadedClasses() {
        return sharedUnloadedClasses;
    }
    
    LongMonitor getUnloadedClasses() {
        return unloadedClasses;
    }
    
    LongMonitor getThreadsDaemon() {
        return threadsDaemon;
    }
    
    LongMonitor getThreadsLive() {
        return threadsLive;
    }
    
    LongMonitor getThreadsLivePeak() {
        return threadsLivePeak;
    }
    
    LongMonitor getThreadsStarted() {
        return threadsStarted;
    }
    
    LongMonitor getApplicationTime() {
        return applicationTime;
    }
    
    LongMonitor getUpTime() {
        return upTime;
    }
    
    List<LongMonitor> getGenCapacity() {
        return genCapacity;
    }
    
    List<LongMonitor> getGenUsed() {
        return genUsed;
    }
    
    long[] getGenMaxCapacity() {
        return genMaxCapacity;
    }
    
    /**
     * Invoked when instrumentation objects are inserted into or removed
     * from the MonitoredVm.
     *
     * @param event the object describing the event.
     */
    public void monitorStatusChanged(MonitorStatusChangeEvent event) {
        
    }
    
    /**
     * Invoked when instrumentation objects are updated. This event is
     * generated at a fixed interval as determined by the polling rate
     * of the MonitoredVm that the VmListener is registered with.
     *
     * @param event the object describing the event.
     */
    public void monitorsUpdated(VmEvent event) {
        assert event.getMonitoredVm().equals(monitoredVm);
        assert !listeners.isEmpty();
        Iterator<MonitoredDataListener> it = listeners.iterator();
        MonitoredData data = new MonitoredData(this);
        while(it.hasNext()) {
            it.next().monitoredDataEvent(data);
        }
    }
    
    /**
     * Invoked when the connection to the MonitoredVm has disconnected
     * due to communication errors.
     *
     * @param event the object describing the event.
     */
    public void disconnected(VmEvent event) {
        System.out.println("Disconnect "+event.getMonitoredVm().getVmIdentifier());
        disableListeners();
    }
    
    public void dataFinished(Object dataSource) {
        disableListeners();
    }
    
}
