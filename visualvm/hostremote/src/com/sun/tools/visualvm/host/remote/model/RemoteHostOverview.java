/*
 * Copyright 2009-2010 Sun Microsystems, Inc.  All Rights Reserved.
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

package com.sun.tools.visualvm.host.remote.model;

import com.sun.management.OperatingSystemMXBean;
import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.application.jvm.JvmFactory;
import com.sun.tools.visualvm.core.datasupport.Stateful;
import com.sun.tools.visualvm.host.Host;
import com.sun.tools.visualvm.host.model.*;
import com.sun.tools.visualvm.tools.jmx.JmxModel;
import com.sun.tools.visualvm.tools.jmx.JmxModelFactory;
import com.sun.tools.visualvm.tools.jmx.JvmMXBeans;
import com.sun.tools.visualvm.tools.jmx.JvmMXBeansFactory;
import java.lang.management.ManagementFactory;
import java.lang.reflect.UndeclaredThrowableException;
import java.rmi.ConnectException;
import java.util.Properties;
import java.util.Set;
import javax.management.AttributeNotFoundException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 *
 * @author Tomas Hurka
 */
class RemoteHostOverview extends HostOverview  {
    private static ObjectName osMXBeanName = getOperatingSystemMXBeanName();
    
    private OperatingSystemMXBean osMXBean;
    private boolean loadAverageAvailable;
    private Host remoteHost;
    private volatile Application jmxApp;
    private String name;
    private String version;
    private String patchLevel;
    private String arch;
    
    RemoteHostOverview(Host h) {
        remoteHost = h;
        jmxApp = getJMXApplication();
        initStaticData(jmxApp);
    }
    
    public String getName() {
        return name;
    }
    
    public String getVersion() {
        return version;
    }
    
    public String getPatchLevel() {
        return patchLevel;
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
    
    public String getArch() {
        return arch;
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
            return osMXBean.getTotalPhysicalMemorySize();
        } catch (UndeclaredThrowableException ex) {
            if (ex.getCause() instanceof ConnectException) {
                jmxApp = null;
                return getTotalPhysicalMemorySize();
            }
            throw ex;
        }
    }
    
    public long getFreePhysicalMemorySize() {
        checkJmxApp();
        if (jmxApp == null) {
            return -1;
        }
        try {
            return osMXBean.getFreePhysicalMemorySize();
        } catch (UndeclaredThrowableException ex) {
            if (ex.getCause() instanceof ConnectException) {
                jmxApp = null;
                return getFreePhysicalMemorySize();
            }
            throw ex;
        }
    }
    
    public long getTotalSwapSpaceSize() {
        checkJmxApp();
        if (jmxApp == null) {
            return -1;
        }
        try {
            return osMXBean.getTotalSwapSpaceSize();
        } catch (UndeclaredThrowableException ex) {
            if (ex.getCause() instanceof ConnectException) {
                jmxApp = null;
                return getTotalSwapSpaceSize();
            }
            throw ex;
        }
    }
    
    public long getFreeSwapSpaceSize() {
        checkJmxApp();
        if (jmxApp == null) {
            return -1;
        }
        try {
            return osMXBean.getFreeSwapSpaceSize();
        } catch (UndeclaredThrowableException ex) {
            if (ex.getCause() instanceof ConnectException) {
                jmxApp = null;
                return getFreeSwapSpaceSize();
            }
            throw ex;
        }
    }
    
    public String getHostAddress() {
        return remoteHost.getInetAddress().getHostAddress();
    }
    
    private void initStaticData(Application app) {
        if (app == null) return;
        Properties sysProp = JvmFactory.getJVMFor(app).getSystemProperties();
        name = osMXBean.getName();
        version = osMXBean.getVersion();
        patchLevel = sysProp.getProperty("sun.os.patch.level", ""); // NOI18N
        arch = osMXBean.getArch();
        String bits = sysProp.getProperty("sun.arch.data.model"); // NOI18N
        if (bits != null) {
            arch += " "+bits+"bit";   // NOI18N
        }
        
    }
    
    private Application getJMXApplication() {
        Set<Application> apps = remoteHost.getRepository().getDataSources(Application.class);
        
        for (Application app : apps) {
            if (app.getState() != Stateful.STATE_AVAILABLE) continue;
            JmxModel jmx = JmxModelFactory.getJmxModelFor(app);
            
            if (jmx != null && jmx.getConnectionState().equals(JmxModel.ConnectionState.CONNECTED)) {
                JvmMXBeans mxbeans = JvmMXBeansFactory.getJvmMXBeans(jmx);
                
                if (mxbeans != null) {
                    osMXBean = mxbeans.getMXBean(osMXBeanName, OperatingSystemMXBean.class);
                    loadAverageAvailable = false;
                    try {
                        loadAverageAvailable = osMXBean.getSystemLoadAverage() >= 0;
                    } catch (UndeclaredThrowableException ex) {
                        Throwable cause = ex.getCause();
                        if (!(cause instanceof AttributeNotFoundException)) {
                            throw ex;
                        }
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
    
    private static ObjectName getOperatingSystemMXBeanName() {
        try {
            return new ObjectName(ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME);
        } catch (MalformedObjectNameException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
}
