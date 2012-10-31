/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.visualvm.tools.jmx;

import java.io.IOException;
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
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.LoggingMXBean;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * <p>The {@code JvmMXBeansFactory} class is a factory class that
 * allows to get instances of {@link JvmMXBeans} for a given
 * {@link MBeanServerConnection} or {@link JmxModel}.</p>
 * 
 * <p>The factory methods allow to supply an interval value at which the
 * internal MBean cache will be automatically flushed and interested
 * {@link MBeanCacheListener}s notified.</p>
 * 
 * <p>If the factory methods which do not take an interval value are used
 * then no MBean caching is performed at all.</p>
 *
 * @see CachedMBeanServerConnection
 * @see CachedMBeanServerConnectionFactory
 *
 * @author Luis-Miguel Alventosa
 */
public final class JvmMXBeansFactory {

    private JvmMXBeansFactory() {
    }

    /**
     * <p>Factory method for obtaining the {@link JvmMXBeans} for
     * the given {@link MBeanServerConnection}.</p>
     * 
     * @param mbsc an MBeanServerConnection.
     * 
     * @return a {@link JvmMXBeans} instance containing the MXBean
     * proxies for the Java platform MXBeans backed by the supplied
     * {@link MBeanServerConnection}. No MBean caching is applied on
     * the supplied {@link MBeanServerConnection}.
     */
    public static JvmMXBeans getJvmMXBeans(MBeanServerConnection mbsc) {
        return new JvmMXBeansImpl(mbsc);
    }

    /**
     * <p>Factory method for obtaining the {@link JvmMXBeans} for
     * the given {@link MBeanServerConnection}.</p>
     * 
     * <p>The MBeans in this {@link MBeanServerConnection} will be cached.
     * The MBean cache will be flushed at the given interval and the
     * interested {@link MBeanCacheListener}s will be notified.</p>
     * 
     * @param mbsc an MBeanServerConnection.
     * @param interval the interval (in milliseconds) at which the MBean
     * cache is flushed. An interval equal to zero means no automatic flush
     * of the MBean cache.
     * 
     * @return a {@link JvmMXBeans} instance containing the MXBean
     * proxies for the Java platform MXBeans backed by the supplied
     * {@link MBeanServerConnection}. MBean caching is enabled and
     * the cache is flushed at the end of every interval period.
     * 
     * @throws IllegalArgumentException if the supplied interval is negative.
     */
    public static JvmMXBeans getJvmMXBeans(MBeanServerConnection mbsc, int interval)
            throws IllegalArgumentException {
        if (interval < 0) {
            throw new IllegalArgumentException("interval cannot be negative"); // NOI18N
        }
        return new JvmMXBeansImpl(CachedMBeanServerConnectionFactory.getCachedMBeanServerConnection(mbsc, interval));
    }

    /**
     * <p>Factory method for obtaining the {@link JvmMXBeans} for
     * the given {@link JmxModel}.</p>
     * 
     * @param jmx a JmxModel.
     * 
     * @return a {@link JvmMXBeans} instance containing the MXBean
     * proxies for the Java platform MXBeans backed by the supplied
     * {@link JmxModel}. No MBean caching is applied on the supplied
     * {@link JmxModel}.
     */
    public static JvmMXBeans getJvmMXBeans(JmxModel jmx) {
        return new JvmMXBeansImpl(jmx.getMBeanServerConnection());
    }

    /**
     * <p>Factory method for obtaining the {@link JvmMXBeans} for the given
     * {@link JmxModel}.</p>
     * 
     * <p>The MBeans in this {@link JmxModel} will be cached. The MBean
     * cache will be flushed at the given interval and the interested
     * {@link MBeanCacheListener}s will be notified.</p>
     *
     * @param jmx a JmxModel.
     * @param interval the interval (in milliseconds) at which the MBean
     * cache is flushed. An interval equal to zero means no automatic flush
     * of the MBean cache.
     *
     * @return a {@link JvmMXBeans} instance containing the MXBean
     * proxies for the Java platform MXBeans backed by the supplied
     * {@link JmxModel}. MBean caching is enabled and the cache is
     * flushed at the end of every interval period.
     * 
     * @throws IllegalArgumentException if the supplied interval is negative.
     */
    public static JvmMXBeans getJvmMXBeans(JmxModel jmx, int interval)
            throws IllegalArgumentException {
        if (interval < 0) {
            throw new IllegalArgumentException("interval cannot be negative"); // NOI18N
        }
        return new JvmMXBeansImpl(CachedMBeanServerConnectionFactory.getCachedMBeanServerConnection(jmx.getMBeanServerConnection(), interval));
    }

    /**
     * MXBean proxies for the Java platform MXBeans backed by the
     * supplied {@link MBeanServerConnection} or {@link JmxModel}.
     *
     * @author Luis-Miguel Alventosa
     */
    static class JvmMXBeansImpl implements JvmMXBeans {

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
        private final static Logger LOGGER = Logger.getLogger(JvmMXBeansImpl.class.getName());

        /**
         * Creates an instance of {@code JvmMXBeans} for the given
         * {@link MBeanServerConnection}.
         *
         * @param mbsc the {@link MBeanServerConnection} instance.
         */
        public JvmMXBeansImpl(MBeanServerConnection mbsc) {
            this.mbsc = mbsc;
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
                    LOGGER.throwing(JvmMXBeansImpl.class.getName(), "getGarbageCollectorMXBeans", e); // NOI18N
                    return null;
                }
                Set<ObjectName> mbeans;
                try {
                    mbeans = mbsc.queryNames(gcName, null);
                } catch (Exception e) {
                    LOGGER.throwing(JvmMXBeansImpl.class.getName(), "getGarbageCollectorMXBeans", e); // NOI18N
                    return null;
                }
                if (mbeans != null) {
                    garbageCollectorMXBeans = new ArrayList<GarbageCollectorMXBean>();
                    for (ObjectName on : mbeans) {
                        String name = GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE + ",name=" + on.getKeyProperty("name"); // NOI18N
                        try {
                            GarbageCollectorMXBean mbean = newPlatformMXBeanProxy(mbsc, name, GarbageCollectorMXBean.class);
                            garbageCollectorMXBeans.add(mbean);
                        } catch (Exception e) {
                            LOGGER.throwing(JvmMXBeansImpl.class.getName(), "getGarbageCollectorMXBeans", e); // NOI18N
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
                    LOGGER.throwing(JvmMXBeansImpl.class.getName(), "getMemoryManagerMXBeans", e); // NOI18N
                    return null;
                }
                Set<ObjectName> mbeans;
                try {
                    mbeans = mbsc.queryNames(managerName, null);
                } catch (Exception e) {
                    LOGGER.throwing(JvmMXBeansImpl.class.getName(), "getMemoryManagerMXBeans", e); // NOI18N
                    return null;
                }
                if (mbeans != null) {
                    memoryManagerMXBeans = new ArrayList<MemoryManagerMXBean>();
                    for (ObjectName on : mbeans) {
                        String name = MEMORY_MANAGER_MXBEAN_DOMAIN_TYPE + ",name=" + on.getKeyProperty("name"); // NOI18N
                        try {
                            MemoryManagerMXBean mbean = newPlatformMXBeanProxy(mbsc, name, MemoryManagerMXBean.class);
                            memoryManagerMXBeans.add(mbean);
                        } catch (Exception e) {
                            LOGGER.throwing(JvmMXBeansImpl.class.getName(), "getMemoryManagerMXBeans", e); // NOI18N
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
                    LOGGER.throwing(JvmMXBeansImpl.class.getName(), "getMemoryPoolMXBeans", e); // NOI18N
                    return null;
                }
                Set<ObjectName> mbeans;
                try {
                    mbeans = mbsc.queryNames(poolName, null);
                } catch (Exception e) {
                    LOGGER.throwing(JvmMXBeansImpl.class.getName(), "getMemoryPoolMXBeans", e); // NOI18N
                    return null;
                }
                if (mbeans != null) {
                    memoryPoolMXBeans = new ArrayList<MemoryPoolMXBean>();
                    for (ObjectName on : mbeans) {
                        String name = MEMORY_POOL_MXBEAN_DOMAIN_TYPE + ",name=" + on.getKeyProperty("name"); // NOI18N
                        try {
                            MemoryPoolMXBean mbean = newPlatformMXBeanProxy(mbsc, name, MemoryPoolMXBean.class);
                            memoryPoolMXBeans.add(mbean);
                        } catch (Exception e) {
                            LOGGER.throwing(JvmMXBeansImpl.class.getName(), "getMemoryPoolMXBeans", e); // NOI18N
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
                } catch (IOException e) {
                    LOGGER.throwing(JvmMXBeansImpl.class.getName(), "getMXBean", e); // NOI18N
                } catch (IllegalArgumentException iae) {
                    LOGGER.log(Level.INFO, JvmMXBeansImpl.class.getName()+".getMXBean()", iae); // NOI18N                    
                }
            }
            return null;
        }

        public void addMBeanCacheListener(MBeanCacheListener listener) {
            if (mbsc instanceof CachedMBeanServerConnection) {
                ((CachedMBeanServerConnection) mbsc).addMBeanCacheListener(listener);
            } else {
                throw new UnsupportedOperationException("The underlying MBeanServerConnection does not support caching."); // NOI18N
            }
        }

        public void removeMBeanCacheListener(MBeanCacheListener listener) {
            if (mbsc instanceof CachedMBeanServerConnection) {
                ((CachedMBeanServerConnection) mbsc).removeMBeanCacheListener(listener);
            } else {
                throw new UnsupportedOperationException("The underlying MBeanServerConnection does not support caching."); // NOI18N
            }
        }

        public void flush() {
            if (mbsc instanceof CachedMBeanServerConnection) {
                ((CachedMBeanServerConnection) mbsc).flush();
            } else {
                throw new UnsupportedOperationException("The underlying MBeanServerConnection does not support caching."); // NOI18N
            }
        }

        public int getInterval() {
            if (mbsc instanceof CachedMBeanServerConnection) {
                return ((CachedMBeanServerConnection) mbsc).getInterval();
            } else {
                throw new UnsupportedOperationException("The underlying MBeanServerConnection does not support caching."); // NOI18N
            }
        }
    }
}
