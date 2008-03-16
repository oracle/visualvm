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

package com.sun.tools.visualvm.tools.jmx;

import com.sun.tools.visualvm.core.model.Model;
import com.sun.tools.visualvm.application.Application;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.CompilationMXBean;
import java.lang.management.GarbageCollectorMXBean;
import static java.lang.management.ManagementFactory.*;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryManagerMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.LogManager;
import java.util.logging.LoggingMXBean;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.openide.ErrorManager;

/**
 * This model relies on the {@link JmxModel} for the given application
 * and returns MXBean proxies for the Java platform MXBeans.
 *
 * @author Luis-Miguel Alventosa
 */
public class JvmJmxModel extends Model {
    
    private Application app;
    protected MBeanServerConnection mbsc;
    private ClassLoadingMXBean classLoadingMXBean = null;
    private CompilationMXBean compilationMXBean = null;
    private LoggingMXBean loggingMXBean = null;
    private MemoryMXBean memoryMXBean = null;
    private OperatingSystemMXBean operatingSystemMXBean = null;
    private RuntimeMXBean runtimeMXBean = null;
    private ThreadMXBean threadMXBean = null;
    private List<GarbageCollectorMXBean> garbageCollectorMXBeans = null;
    private List<MemoryManagerMXBean> memoryManagerMXBeans = null;
    private List<MemoryPoolMXBean> memoryPoolMXBeans = null;
    
    /**
     * Creates an instance of {@code JvmJmxModel} for a given {@link Application}.
     *
     * @param application the {@link Application} instance.
     */
    public JvmJmxModel(Application application) {
        app = application;
        JmxModel jmxModel = JmxModelFactory.getJmxModelFor(app);
        if (jmxModel != null) {
            mbsc = jmxModel.getCachedMBeanServerConnection();
        }
    }
    
    /**
     * Returns an MXBean proxy for the class loading system of the JVM.
     */
    public synchronized ClassLoadingMXBean getClassLoadingMXBean() {
        if (mbsc != null && classLoadingMXBean == null) {
            classLoadingMXBean = getMXBean(CLASS_LOADING_MXBEAN_NAME, ClassLoadingMXBean.class);
        }
        return classLoadingMXBean;
    }
    
    /**
     * Returns an MXBean proxy for the compilation system of the JVM.
     */
    public synchronized CompilationMXBean getCompilationMXBean() {
        if (mbsc != null && compilationMXBean == null) {
            compilationMXBean = getMXBean(COMPILATION_MXBEAN_NAME, CompilationMXBean.class);
        }
        return compilationMXBean;
    }
    
    /**
     * Returns an MXBean proxy for the logging system of the JVM.
     */
    public synchronized LoggingMXBean getLoggingMXBean() {
        if (mbsc != null && loggingMXBean == null) {
            loggingMXBean = getMXBean(LogManager.LOGGING_MXBEAN_NAME, LoggingMXBean.class);
        }
        return loggingMXBean;
    }
    
    /**
     * Returns a collection of MXBean proxies for the garbage collectors of the JVM.
     */
    public synchronized Collection<GarbageCollectorMXBean> getGarbageCollectorMXBeans() {
        // TODO: How to deal with changes to the list?
        if (mbsc != null && garbageCollectorMXBeans == null) {
            ObjectName gcName;
            try {
                gcName = new ObjectName(GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE + ",*");
            } catch (MalformedObjectNameException e) {
                // Should never happen
                ErrorManager.getDefault().notify(ErrorManager.USER, e);
                return null;
            }
            Set<ObjectName> mbeans;
            try {
                mbeans = mbsc.queryNames(gcName, null);
            } catch (Exception e) {
                ErrorManager.getDefault().notify(ErrorManager.USER, e);
                return null;
            }
            if (mbeans != null) {
                garbageCollectorMXBeans = new ArrayList<GarbageCollectorMXBean>();
                for (ObjectName on : mbeans) {
                    String name = GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE + ",name=" + on.getKeyProperty("name");
                    try {
                        GarbageCollectorMXBean mbean = newPlatformMXBeanProxy(mbsc, name, GarbageCollectorMXBean.class);
                        garbageCollectorMXBeans.add(mbean);
                    } catch (Exception e) {
                        ErrorManager.getDefault().notify(ErrorManager.USER, e);
                    }
                }
            }
        }
        return garbageCollectorMXBeans;
    }
    
    /**
     * Returns a collection of MXBean proxies for the memory managers of the JVM.
     */
    public synchronized Collection<MemoryManagerMXBean> getMemoryManagerMXBeans() {
        // TODO: How to deal with changes to the list?
        if (mbsc != null && memoryManagerMXBeans == null) {
            ObjectName managerName;
            try {
                managerName = new ObjectName(MEMORY_MANAGER_MXBEAN_DOMAIN_TYPE + ",*");
            } catch (MalformedObjectNameException e) {
                // Should never happen
                ErrorManager.getDefault().notify(ErrorManager.USER, e);
                return null;
            }
            Set<ObjectName> mbeans;
            try {
                mbeans = mbsc.queryNames(managerName, null);
            } catch (Exception e) {
                ErrorManager.getDefault().notify(ErrorManager.USER, e);
                return null;
            }
            if (mbeans != null) {
                memoryManagerMXBeans = new ArrayList<MemoryManagerMXBean>();
                for (ObjectName on : mbeans) {
                    String name = MEMORY_MANAGER_MXBEAN_DOMAIN_TYPE + ",name=" + on.getKeyProperty("name");
                    try {
                        MemoryManagerMXBean mbean = newPlatformMXBeanProxy(mbsc, name, MemoryManagerMXBean.class);
                        memoryManagerMXBeans.add(mbean);
                    } catch (Exception e) {
                        ErrorManager.getDefault().notify(ErrorManager.USER, e);
                    }
                }
            }
        }
        return memoryManagerMXBeans;
    }
    
    /**
     * Returns an MXBean proxy for the memory system of the JVM.
     */
    public synchronized MemoryMXBean getMemoryMXBean() {
        if (mbsc != null && memoryMXBean == null) {
            memoryMXBean = getMXBean(MEMORY_MXBEAN_NAME, MemoryMXBean.class);
        }
        return memoryMXBean;
    }
    
    /**
     * Returns a collection of MXBean proxies for the memory pools of the JVM.
     */
    public synchronized Collection<MemoryPoolMXBean> getMemoryPoolMXBeans() {
        // TODO: How to deal with changes to the list?
        if (mbsc != null && memoryPoolMXBeans == null) {
            ObjectName poolName;
            try {
                poolName = new ObjectName(MEMORY_POOL_MXBEAN_DOMAIN_TYPE + ",*");
            } catch (MalformedObjectNameException e) {
                // Should never happen
                ErrorManager.getDefault().notify(ErrorManager.USER, e);
                return null;
            }
            Set<ObjectName> mbeans;
            try {
                mbeans = mbsc.queryNames(poolName, null);
            } catch (Exception e) {
                ErrorManager.getDefault().notify(ErrorManager.USER, e);
                return null;
            }
            if (mbeans != null) {
                memoryPoolMXBeans = new ArrayList<MemoryPoolMXBean>();
                for (ObjectName on : mbeans) {
                    String name = MEMORY_POOL_MXBEAN_DOMAIN_TYPE + ",name=" + on.getKeyProperty("name");
                    try {
                        MemoryPoolMXBean mbean = newPlatformMXBeanProxy(mbsc, name, MemoryPoolMXBean.class);
                        memoryPoolMXBeans.add(mbean);
                    } catch (Exception e) {
                        ErrorManager.getDefault().notify(ErrorManager.USER, e);
                    }
                }
            }
        }
        return memoryPoolMXBeans;
    }
    
    /**
     * Returns an MXBean proxy for the operating system of the JVM.
     */
    public synchronized OperatingSystemMXBean getOperatingSystemMXBean() {
        if (mbsc != null && operatingSystemMXBean == null) {
            operatingSystemMXBean = getMXBean(OPERATING_SYSTEM_MXBEAN_NAME, OperatingSystemMXBean.class);
        }
        return operatingSystemMXBean;
    }
    
    /**
     * Returns an MXBean proxy for the runtime system of the JVM.
     */
    public synchronized RuntimeMXBean getRuntimeMXBean() {
        if (mbsc != null && runtimeMXBean == null) {
            runtimeMXBean = getMXBean(RUNTIME_MXBEAN_NAME, RuntimeMXBean.class);
        }
        return runtimeMXBean;
    }
    
    /**
     * Returns an MXBean proxy for the thread system of the JVM.
     */
    public synchronized ThreadMXBean getThreadMXBean() {
        if (mbsc != null && threadMXBean == null) {
            threadMXBean = getMXBean(THREAD_MXBEAN_NAME, ThreadMXBean.class);
        }
        return threadMXBean;
    }
    
    /**
     * Generic method that returns an MXBean proxy for the given platform
     * MXBean identified by its ObjectName and which implements the supplied
     * interface.
     */
    public <T> T getMXBean(ObjectName objectName, Class<T> interfaceClass) {
        return getMXBean(objectName.toString(), interfaceClass);
    }
    
    <T> T getMXBean(String objectNameStr, Class<T> interfaceClass) {
        if (mbsc != null) {
            try {
                return newPlatformMXBeanProxy(mbsc, objectNameStr, interfaceClass);
            } catch (Exception e) {
                ErrorManager.getDefault().notify(ErrorManager.USER, e);
            }
        }
        return null;
    }
}
