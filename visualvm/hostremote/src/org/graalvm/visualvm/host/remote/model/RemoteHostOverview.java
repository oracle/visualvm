/*
 * Copyright (c) 2009, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.host.remote.model;

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.application.jvm.JvmFactory;
import org.graalvm.visualvm.core.datasupport.Stateful;
import org.graalvm.visualvm.host.Host;
import org.graalvm.visualvm.host.model.HostOverview;
import org.graalvm.visualvm.tools.jmx.JmxModel;
import org.graalvm.visualvm.tools.jmx.JmxModelFactory;
import org.graalvm.visualvm.tools.jmx.JvmMXBeans;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.UndeclaredThrowableException;
import java.rmi.ConnectException;
import java.util.Properties;
import java.util.Set;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

/**
 *
 * @author Tomas Hurka
 */
class RemoteHostOverview extends HostOverview  {
    private static final ObjectName osMXBeanName = getOperatingSystemMXBeanName();
    private static final String TotalPhysicalMemorySizeAttr = "TotalPhysicalMemorySize";    // NOI18N
    private static final String TotalPhysicalMemorySizeAttr1 = "TotalPhysicalMemory";       // NOI18N
    private static final String FreePhysicalMemorySizeAttr = "FreePhysicalMemorySize";      // NOI18N
    private static final String TotalSwapSpaceSizeAttr = "TotalSwapSpaceSize";              // NOI18N
    private static final String FreeSwapSpaceSizeAttr = "FreeSwapSpaceSize";                // NOI18N
    
    private volatile OperatingSystemMXBean osMXBean;
    private volatile MBeanServerConnection connection;
    private volatile boolean loadAverageAvailable;
    private volatile Application jmxApp;
    private String totalPhysicalMemorySizeAttr;
    private Host remoteHost;
    private boolean staticDataInitialized;
    private String name;
    private String version;
    private String patchLevel;
    private String arch;
    
    RemoteHostOverview(Host h) {
        remoteHost = h;
    }
    
    public String getName() {
        initStaticData();
        return name;
    }
    
    public String getVersion() {
        initStaticData();
        return version;
    }
    
    public String getPatchLevel() {
        initStaticData();
        return patchLevel;
    }
    
    public String getArch() {
        initStaticData();
        return arch;
    }
    
    public int getAvailableProcessors() {
        checkJmxApp();
        if (jmxApp == null) {
            return -1;
        }
        try {
            return osMXBean.getAvailableProcessors();
        } catch (UndeclaredThrowableException ex) {
            if (ex.getCause() instanceof ConnectException) {
                jmxApp = null;
                return getAvailableProcessors();
            }
            throw ex;
        }
    }
    
    public String getHostName() {
        return remoteHost.getHostName();
    }
    
    public double getSystemLoadAverage() {
        if (loadAverageAvailable) {
            checkJmxApp();
            if (jmxApp == null) {
                return -1;
            }
            try {
                return osMXBean.getSystemLoadAverage();
            } catch (UndeclaredThrowableException ex) {
                if (ex.getCause() instanceof ConnectException) {
                    jmxApp = null;
                    return getSystemLoadAverage();
                }
                throw ex;
            }
        }
        return -1;
    }
    
    public long getTotalPhysicalMemorySize() {
        checkJmxApp();
        if (jmxApp == null) {
            return -1;
        }
        try {
            return getAttribute(totalPhysicalMemorySizeAttr);
        } catch (IOException ex) {
            jmxApp = null;
            return getTotalPhysicalMemorySize();            
        }
    }
    
    public long getFreePhysicalMemorySize() {
        checkJmxApp();
        if (jmxApp == null) {
            return -1;
        }
        try {
            return getAttribute(FreePhysicalMemorySizeAttr);
        } catch (IOException ex) {
            jmxApp = null;
            return getTotalPhysicalMemorySize();            
        }
    }
    
    public long getTotalSwapSpaceSize() {
        checkJmxApp();
        if (jmxApp == null) {
            return -1;
        }
        try {
            return getAttribute(TotalSwapSpaceSizeAttr);
        } catch (IOException ex) {
            jmxApp = null;
            return getTotalPhysicalMemorySize();            
        }
    }
    
    public long getFreeSwapSpaceSize() {
        checkJmxApp();
        if (jmxApp == null) {
            return -1;
        }
        try {
            return getAttribute(FreeSwapSpaceSizeAttr);
        } catch (IOException ex) {
            jmxApp = null;
            return getTotalPhysicalMemorySize();            
        }
    }
    
    public String getHostAddress() {
        return remoteHost.getInetAddress().getHostAddress();
    }
    
    private synchronized void initStaticData() {
        if (staticDataInitialized) return;
        checkJmxApp();
        if (jmxApp == null) return;
        Properties sysProp = JvmFactory.getJVMFor(jmxApp).getSystemProperties();
        name = osMXBean.getName();
        version = osMXBean.getVersion();
        patchLevel = sysProp.getProperty("sun.os.patch.level", ""); // NOI18N
        arch = osMXBean.getArch();
        String bits = sysProp.getProperty("sun.arch.data.model"); // NOI18N
        if (bits != null) {
            arch += " "+bits+"bit";   // NOI18N
        }
        staticDataInitialized = true;
    }
    
    private Application getJMXApplication() {
        Set<Application> apps = remoteHost.getRepository().getDataSources(Application.class);
        
        for (Application app : apps) {
            if (app.getState() != Stateful.STATE_AVAILABLE) continue;
            JmxModel jmx = JmxModelFactory.getJmxModelFor(app);
            
            if (jmx != null && jmx.getConnectionState().equals(JmxModel.ConnectionState.CONNECTED)) {
                JvmMXBeans mxbeans = jmx.getJvmMXBeans();
                connection = jmx.getMBeanServerConnection();
                
                if (mxbeans != null && connection != null) {
                    osMXBean = mxbeans.getOperatingSystemMXBean();
                    loadAverageAvailable = false;
                    try {
                        loadAverageAvailable = osMXBean.getSystemLoadAverage() >= 0;
                    } catch (UndeclaredThrowableException ex) {
                        Throwable cause = ex.getCause();
                        if (!(cause instanceof AttributeNotFoundException)) {
                            throw ex;
                        }
                    }
                    try {
                        connection.getAttribute(osMXBeanName, TotalPhysicalMemorySizeAttr);
                        totalPhysicalMemorySizeAttr = TotalPhysicalMemorySizeAttr;
                    } catch (AttributeNotFoundException ex) {
                        totalPhysicalMemorySizeAttr = TotalPhysicalMemorySizeAttr1;
                    } catch (InstanceNotFoundException ex) {
                        throw new RuntimeException(ex);
                    } catch (ReflectionException ex) {
                        throw new RuntimeException(ex);
                    } catch (MBeanException ex) {
                        throw new RuntimeException(ex);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                    return app;
                }
            }
        }
        return null;
    }
    
    private synchronized void checkJmxApp() {
        if (jmxApp == null || jmxApp.getState() != Stateful.STATE_AVAILABLE) {
            jmxApp = getJMXApplication();
        }
    }
    
    private long getAttribute(String name) throws IOException {
        Object val = null;
        try {
            val = connection.getAttribute(osMXBeanName, name);
        } catch (AttributeNotFoundException ex) {
            return -1;
        } catch (InstanceNotFoundException ex) {
            throw new RuntimeException(ex);
        } catch (MBeanException ex) {
            throw new RuntimeException(ex);
        } catch (ReflectionException ex) {
            throw new RuntimeException(ex);
        }
        if (val instanceof Number) {
            return ((Number)val).longValue();
        }
        return -1;
    }
    
    private static ObjectName getOperatingSystemMXBeanName() {
        try {
            return new ObjectName(ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME);
        } catch (MalformedObjectNameException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
}
