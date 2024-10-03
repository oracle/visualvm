/*
 * Copyright (c) 2007, 2022, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.jvm;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Collection;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.application.jvm.MonitoredData;
import org.graalvm.visualvm.core.datasupport.DataRemovedListener;
import org.graalvm.visualvm.core.datasupport.Stateful;
import org.graalvm.visualvm.core.options.GlobalPreferences;
import org.graalvm.visualvm.tools.jmx.JmxModel;
import org.graalvm.visualvm.tools.jmx.JmxModel.ConnectionState;
import org.graalvm.visualvm.tools.jmx.JmxModelFactory;
import org.graalvm.visualvm.tools.jmx.JvmMXBeans;
import org.openide.util.NbBundle;

/**
 *
 * @author Tomas Hurka
 */
class JmxSupport implements DataRemovedListener<Application> {
    private final static Logger LOGGER = Logger.getLogger(JmxSupport.class.getName());
    private static final String PROCESS_CPU_TIME_ATTR = "ProcessCpuTime"; // NOI18N

    /** IBM/OpenJ9 only. */
    private static final String PROCESS_CPU_TIME_NS_ATTR = "ProcessCpuTimeByNS"; // NOI18N

    /**
     * IBM/OpenJ9 only.
     * Returns the collective processing capacity available to the VM
     * in units of 1% of a physical processor. In environments without
     * some kind of virtual partitioning, this will simply be the number
     * of CPUs * 100.
     */
    private static final String PROCESSING_CAPACITY_ATTR = "ProcessingCapacity"; // NOI18N
    private static final String PERM_GEN = "Perm Gen";  // NOI18N
    private static final String PS_PERM_GEN = "PS Perm Gen";    // NOI18N
    private static final String CMS_PERM_GEN = "CMS Perm Gen";    // NOI18N
    private static final String G1_PERM_GEN = "G1 Perm Gen";    // NOI18N
    private static final String METASPACE = "Metaspace";       // NOI18N
    private static final String IBM_PERM_GEN = "class storage";    // NOI18N
    private static final ObjectName osName = getOSName();
    private static long INITIAL_DELAY = 100;

    private Application application;
    private JVMImpl jvm;
    private final Object processCPUTimeAttributeLock = new Object();
    private Boolean processCPUTimeAttribute;
    private double processingCapacity;
    private Timer timer;
    private MemoryPoolMXBean permGenPool;
    private Collection<GarbageCollectorMXBean> gcList;
    private String[] genName;
    private String processCpuTimeAttr;

    JmxSupport(Application app, JVMImpl vm) {
        jvm = vm;
        application = app;
        app.notifyWhenRemoved(this);
    }

    RuntimeMXBean getRuntime() {
        JvmMXBeans jmx = getJvmMXBeans();
        if (jmx != null) {
            return jmx.getRuntimeMXBean();
        }
        return null;
    }

    boolean hasProcessCPUTimeAttribute() {
        synchronized (processCPUTimeAttributeLock) {
            if (processCPUTimeAttribute != null) {
                return processCPUTimeAttribute.booleanValue();
            }
            processCPUTimeAttribute = Boolean.FALSE;
            JmxModel jmx = JmxModelFactory.getJmxModelFor(application);
           
            if (jmx != null && jmx.getConnectionState().equals(ConnectionState.CONNECTED)) {
                MBeanServerConnection conn = jmx.getMBeanServerConnection();
                
                if (conn != null) {
                    try {
                       MBeanInfo info = conn.getMBeanInfo(osName);
                       MBeanAttributeInfo[] attrs = info.getAttributes();
                       
                       processingCapacity = 1;
                       for (MBeanAttributeInfo attr : attrs) {
                           String name = attr.getName();
                           if (PROCESS_CPU_TIME_ATTR.equals(name) && !processCPUTimeAttribute) {
                               processCPUTimeAttribute = Boolean.TRUE;
                               processCpuTimeAttr = name;
                           }
                           if (PROCESS_CPU_TIME_NS_ATTR.equals(name)) {
                               processCPUTimeAttribute = Boolean.TRUE;
                               processCpuTimeAttr = name;
                           }
                           if (PROCESSING_CAPACITY_ATTR.equals(name)) {
                               Number mul = (Number) conn.getAttribute(osName,PROCESSING_CAPACITY_ATTR);
                               processingCapacity = mul.longValue()/100.0/getAvailableProcessors();
                           }
                        }
                    } catch (Exception ex) {
                       LOGGER.throwing(JmxSupport.class.getName(), "hasProcessCPUTimeAttribute", ex); // NOI18N
                    }
                }
            }
            return processCPUTimeAttribute.booleanValue();
        }
    }
     
    long getProcessCPUTime() {
        if (!hasProcessCPUTimeAttribute()) {
            throw new UnsupportedOperationException();
        }
        JmxModel jmx = JmxModelFactory.getJmxModelFor(application);
        
        if (jmx != null && jmx.getConnectionState().equals(ConnectionState.CONNECTED)) {
           MBeanServerConnection conn = jmx.getMBeanServerConnection();
            
           if (conn != null) {
                try {
                    Long cputime = (Long)conn.getAttribute(osName,processCpuTimeAttr);
                    
                    return (long)(cputime.longValue()/processingCapacity);
                } catch (Exception ex) {
                    LOGGER.throwing(JmxSupport.class.getName(), "hasProcessCPUTimeAttribute", ex); // NOI18N
                }
            }
        }
        return -1;
    }
    
    int getAvailableProcessors() {
        JvmMXBeans jmx = getJvmMXBeans();
        if (jmx != null) {
            OperatingSystemMXBean osMXBean = jmx.getOperatingSystemMXBean();

            if (osMXBean != null) {
                return osMXBean.getAvailableProcessors();
            }
        }
        return -1;
    }

    JvmMXBeans getJvmMXBeans() {
        JmxModel jmxModel = JmxModelFactory.getJmxModelFor(application);
        if (jmxModel != null && jmxModel.getConnectionState() == ConnectionState.CONNECTED) {
            return jmxModel.getJvmMXBeans();
        }
        return null;
    }
    
    synchronized Collection<GarbageCollectorMXBean> getGarbageCollectorMXBeans() {
        if (gcList == null) {
            JvmMXBeans jmx = getJvmMXBeans();
            if (jmx != null) {
                gcList = jmx.getGarbageCollectorMXBeans();
            }           
        }
        return gcList;
    }

    String getJvmArgs() {
        try {
            RuntimeMXBean runtime = getRuntime();
            if (runtime != null) {
                StringBuilder buf = new StringBuilder();
                List<String> args = runtime.getInputArguments();
                for (String arg : args) {
                    buf.append(arg).append(' ');
                }
                return buf.toString().trim();
            }
            return null;
        } catch (Exception e) {
            LOGGER.throwing(JmxSupport.class.getName(), "getJvmArgs", e); // NOI18N
            return null;
        }
    }

    MemoryPoolMXBean getPermGenPool() {
        try {
            if (permGenPool == null) {
                JvmMXBeans jmx = getJvmMXBeans();
                if (jmx != null) {
                    Collection<MemoryPoolMXBean> pools = jmx.getMemoryPoolMXBeans();
                    for (MemoryPoolMXBean pool : pools) {
                        MemoryType type = pool.getType();
                        String name = pool.getName();
                        if (MemoryType.NON_HEAP.equals(type) &&
                                (PERM_GEN.equals(name) ||
                                PS_PERM_GEN.equals(name) ||
                                CMS_PERM_GEN.equals(name) ||
                                G1_PERM_GEN.equals(name) ||
                                METASPACE.equals(name) ||
                                IBM_PERM_GEN.equals(name))) {
                            permGenPool = pool;
                            break;
                        }
                    }
                }
            }
            return permGenPool;
        } catch (Exception e) {
            LOGGER.throwing(JmxSupport.class.getName(), "getPermGenPool", e); // NOI18N
            return null;
        }
    }

    String[] getGenName() {
        if (genName == null) {
            MemoryPoolMXBean permPool = getPermGenPool();
            initGenName();
            if (permPool != null) {
                String label;
                String name = permPool.getName();
                if (METASPACE.equals(name)) {
                    label = NbBundle.getMessage(JmxSupport.class, "LBL_Meta"); // NOI18N                    
                } else {
                    label = NbBundle.getMessage(JmxSupport.class, "LBL_PermGen"); // NOI18N
                }
                genName[1] = label;
            }
        }
        return genName;
    }

    void initGenName() {
        genName = new String[2];
        genName[0] = NbBundle.getMessage(JmxSupport.class, "LBL_Heap");   // NOI18N
        genName[1] = NbBundle.getMessage(JmxSupport.class, "LBL_NA");   // NOI18N        
    }
    
    void initTimer() {
        int interval = GlobalPreferences.sharedInstance().getMonitoredDataPoll() * 1000;
        final JvmMXBeans jmx = getJvmMXBeans();
        if (jmx != null) {
            TimerTask task = new TimerTask() {
                public void run() {
                    if (application.getState() == Stateful.STATE_AVAILABLE) try {
                        MonitoredData data = new MonitoredDataImpl(jvm, JmxSupport.this, jmx);
                        jvm.notifyListeners(data);
                    } catch (UndeclaredThrowableException e) {
                        LOGGER.throwing(JmxSupport.class.getName(), "MonitoredDataImpl<init>", e); // NOI18N
                    }
                }
            };
            disableTimer();
            timer = new Timer("JMX MonitoredData timer for "+application.getId());       // NOI18N
            timer.schedule(task,INITIAL_DELAY,interval);
        }
    }

    void disableTimer() {
        if (timer != null) {
            timer.cancel();
        }
    }

    private static ObjectName getOSName() {
        try {
            return new ObjectName(ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME);
        } catch (MalformedObjectNameException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public void dataRemoved(Application dataSource) {
        disableTimer();
    }
}
