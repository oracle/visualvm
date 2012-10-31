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
     * Returns the managed bean for the class loading system of 
     * the Java virtual machine.
     *
     * @return a {@link ClassLoadingMXBean} object for 
     * the Java virtual machine.
     */
    public ClassLoadingMXBean getClassLoadingMXBean();

    /**
     * Returns the managed bean for the compilation system of 
     * the Java virtual machine.  This method returns <tt>null</tt>
     * if the Java virtual machine has no compilation system.
     *
     * @return a {@link CompilationMXBean} object for the Java virtual 
     *   machine or <tt>null</tt> if the Java virtual machine has 
     *   no compilation system. 
     */
    public CompilationMXBean getCompilationMXBean();

    /**
     * Returns <tt>LoggingMXBean</tt> for managing loggers. 
     *
     * @return a {@link LoggingMXBean} object for 
     * the Java virtual machine.
     */
    public LoggingMXBean getLoggingMXBean();

    /**
     * Returns a list of {@link GarbageCollectorMXBean} objects 
     * in the Java virtual machine.
     * The Java virtual machine may have one or more
     * <tt>GarbageCollectorMXBean</tt> objects.
     * It may add or remove <tt>GarbageCollectorMXBean</tt> 
     * during execution.
     *
     * @return a list of <tt>GarbageCollectorMXBean</tt> objects.
     */
    public Collection<GarbageCollectorMXBean> getGarbageCollectorMXBeans();

    /**
     * Returns a list of {@link MemoryManagerMXBean} objects 
     * in the Java virtual machine. 
     * The Java virtual machine can have one or more memory managers.
     * It may add or remove memory managers during execution.
     *
     * @return a list of <tt>MemoryManagerMXBean</tt> objects.
     */
    public Collection<MemoryManagerMXBean> getMemoryManagerMXBeans();

    /**
     * Returns the managed bean for the memory system of 
     * the Java virtual machine.
     *
     * @return a {@link MemoryMXBean} object for the Java virtual machine.
     */
    public MemoryMXBean getMemoryMXBean();

    /**
     * Returns a list of {@link MemoryPoolMXBean} objects in the 
     * Java virtual machine.
     * The Java virtual machine can have one or more memory pools.
     * It may add or remove memory pools during execution.
     *
     * @return a list of <tt>MemoryPoolMXBean</tt> objects.
     */
    public Collection<MemoryPoolMXBean> getMemoryPoolMXBeans();

    /**
     * Returns the managed bean for the operating system on which
     * the Java virtual machine is running.
     *
     * @return an {@link OperatingSystemMXBean} object for 
     * the Java virtual machine.
     */
    public OperatingSystemMXBean getOperatingSystemMXBean();

    /**
     * Returns the managed bean for the runtime system of 
     * the Java virtual machine.
     *
     * @return a {@link RuntimeMXBean} object for the Java virtual machine.
     */
    public RuntimeMXBean getRuntimeMXBean();

    /**
     * Returns the managed bean for the thread system of 
     * the Java virtual machine.
     *
     * @return a {@link ThreadMXBean} object for the Java virtual machine.
     */
    public ThreadMXBean getThreadMXBean();

    /**
     * Generic method that returns an MXBean proxy for the given platform
     * MXBean identified by its ObjectName and which implements the supplied
     * interface.
     * @return a proxy for a platform MXBean interface of a 
     * given MXBean
     * @param objectName {@link ObjectName} which identifies MXBean
     * @param interfaceClass the MXBean interface to be implemented
     * by the proxy.
     */
    public <T> T getMXBean(ObjectName objectName, Class<T> interfaceClass);
}
