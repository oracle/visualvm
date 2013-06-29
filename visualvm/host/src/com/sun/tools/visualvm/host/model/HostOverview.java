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

package com.sun.tools.visualvm.host.model;

import com.sun.tools.visualvm.core.model.Model;

/**
 * This class uses available JVM APIs to obtatin various information about operating system and host
 * on which the Java virtual machine is running.
 *
 * @author Tomas Hurka
 */
public abstract class HostOverview extends Model  {
    
    /**
     * Returns the operating system architecture.
     * This method is equivalent to <tt>System.getProperty("os.arch")</tt>.
     *
     * @return the operating system architecture.
     * @see java.lang.System#getProperty
     */
    public abstract String getArch();
    
    /**
     * Returns the number of processors available to the Java virtual machine.
     * This method is equivalent to the {@link Runtime#availableProcessors()}
     * method.
     * <p> This value may change during a particular invocation of
     * the virtual machine.
     *
     * @return  the number of processors available to the virtual
     *          machine; never smaller than one.
     */
    public abstract int getAvailableProcessors();
    
    /**
     * The number of currently available bytes of physical memory.
     * 
     * @return  the number of currently available bytes of physical memory;
     * never smaller than one. 
     */ 
    public abstract long getFreePhysicalMemorySize();
    
    /**
     * The number of currently available bytes of swap space.
     * 
     * @return  the number of currently available bytes of swap space;
     * never smaller than one. 
     */ 
    public abstract long getFreeSwapSpaceSize();
    
    /**
     * Returns the operating system name. 
     * This method is equivalent to <tt>System.getProperty("os.name")</tt>.
     *
     * @return the operating system name.
     *
     * @see java.lang.System#getProperty
     */
    public abstract String getName();
    
    /**
     * Returns the system load average for the last minute.
     * The system load average is the sum of the number of runnable entities
     * queued to the {@linkplain #getAvailableProcessors available processors}
     * and the number of runnable entities running on the available processors
     * averaged over a period of time.
     * The way in which the load average is calculated is operating system
     * specific but is typically a damped time-dependent average.
     * <p>
     * If the load average is not available, a negative value is returned.
     * <p>
     * This method is designed to provide a hint about the system load
     * and may be queried frequently.
     * The load average is not available on Windows platform.
     *
     * @return the system load average; or a negative value if not available.
     */
    public abstract double getSystemLoadAverage();
    
    /**
     * The total number of bytes of physical memory.
     * 
     * @return  the total number of bytes of physical memory;
     * never smaller than one. 
     */ 
    public abstract long getTotalPhysicalMemorySize();
    
    /**
     * The total number of bytes of swap space.
     * 
     * @return  the total number of bytes of swap space;
     * never smaller than one. 
     */ 
    public abstract long getTotalSwapSpaceSize();
    
    /**
     * Returns the operating system version.
     * This method is equivalent to <tt>System.getProperty("os.version")</tt>.
     *
     * @return the operating system version.
     *
     * @see java.lang.System#getProperty
     */
    public abstract String getVersion();
    
    /**
     * Returns the hostname for this host. 
     *
     * @return the hostname.
     */
    public abstract String getHostName();
    
    /**
     * Returns the operating system patch level. 
     *
     * @return the operating system patch level.
     */
    public abstract String getPatchLevel();
    
    /**
     * Returns the textual represenation of the IP address of this host.
     * 
     * @return the textual represenation of the IP address
     */ 
    public abstract String getHostAddress();
    
}
