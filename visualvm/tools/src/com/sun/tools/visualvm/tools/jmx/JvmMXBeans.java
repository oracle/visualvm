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

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.CompilationMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryManagerMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.Collection;
import java.util.logging.LoggingMXBean;
import javax.management.ObjectName;

/**
 * MXBean proxies for the Java platform MXBeans.
 *
 * @author Luis-Miguel Alventosa
 */
public interface JvmMXBeans extends MBeanCacheOperations {

    /**
     * Returns an MXBean proxy for the class loading system of the JVM.
     */
    public ClassLoadingMXBean getClassLoadingMXBean();

    /**
     * Returns an MXBean proxy for the compilation system of the JVM.
     */
    public CompilationMXBean getCompilationMXBean();

    /**
     * Returns an MXBean proxy for the logging system of the JVM.
     */
    public LoggingMXBean getLoggingMXBean();

    /**
     * Returns a collection of MXBean proxies for the garbage collectors of the JVM.
     */
    public Collection<GarbageCollectorMXBean> getGarbageCollectorMXBeans();

    /**
     * Returns a collection of MXBean proxies for the memory managers of the JVM.
     */
    public Collection<MemoryManagerMXBean> getMemoryManagerMXBeans();

    /**
     * Returns an MXBean proxy for the memory system of the JVM.
     */
    public MemoryMXBean getMemoryMXBean();

    /**
     * Returns a collection of MXBean proxies for the memory pools of the JVM.
     */
    public Collection<MemoryPoolMXBean> getMemoryPoolMXBeans();

    /**
     * Returns an MXBean proxy for the operating system of the JVM.
     */
    public OperatingSystemMXBean getOperatingSystemMXBean();

    /**
     * Returns an MXBean proxy for the runtime system of the JVM.
     */
    public RuntimeMXBean getRuntimeMXBean();

    /**
     * Returns an MXBean proxy for the thread system of the JVM.
     */
    public ThreadMXBean getThreadMXBean();

    /**
     * Generic method that returns an MXBean proxy for the given platform
     * MXBean identified by its ObjectName and which implements the supplied
     * interface.
     */
    public <T> T getMXBean(ObjectName objectName, Class<T> interfaceClass);
}
