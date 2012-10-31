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

package com.sun.tools.visualvm.application.jvm;

/**
 * This class encapsulates non-static information from JVM. Instances of MonitoredData
 * is periodically fired by (@link Jvm} via {@link MonitoredDataListener}
 * @author Tomas Hurka
 */
public abstract class MonitoredData {
    
    protected long loadedClasses;
    protected long sharedLoadedClasses;
    protected long sharedUnloadedClasses;
    protected long unloadedClasses;
    protected long threadsDaemon;
    protected long threadsLive;
    protected long threadsLivePeak;
    protected long threadsStarted;
    protected long applicationTime;
    protected long upTime;
    protected long[] genCapacity;
    protected long[] genUsed;
    protected long[] genMaxCapacity;
    protected long processCpuTime;
    protected long collectionTime;
    protected Jvm monitoredVm;
    
    
    /**
     * Returns the total number of classes that have been loaded since
     * the Java virtual machine has started execution.
     *
     * @return the total number of classes loaded.
     *
     */
    public long getLoadedClasses() {
        return loadedClasses;
    }
    
    /**
     * Returns the total number of shared classes that have been loaded since
     * the Java virtual machine has started execution.
     *
     * @return the total number of shared classes loaded.
     *
     */
    public long getSharedLoadedClasses() {
        return sharedLoadedClasses;
    }
    
    /**
     * Returns the total number of shared classes unloaded since the Java virtual machine
     * has started execution.
     *
     * @return the total number of unloaded shared classes.
     */
    public long getSharedUnloadedClasses() {
        return sharedUnloadedClasses;
    }
    
    /**
     * Returns the total number of classes unloaded since the Java virtual machine
     * has started execution.
     *
     * @return the total number of unloaded classes.
     */
    public long getUnloadedClasses() {
        return unloadedClasses;
    }
    
    /**
     * Returns the current number of live daemon threads.
     *
     * @return the current number of live daemon threads.
     */
    public long getThreadsDaemon() {
        return threadsDaemon;
    }
    
    /**
     * Returns the current number of live threads including both
     * daemon and non-daemon threads.
     *
     * @return the current number of live threads.
     */
    public long getThreadsLive() {
        return threadsLive;
    }
    
    /**
     * Returns the peak live thread count since the Java virtual machine
     * started
     *
     * @return the peak live thread count.
     */
    public long getThreadsLivePeak() {
        return threadsLivePeak;
    }
    
    /**
     * Returns the total number of threads created and also started
     * since the Java virtual machine started.
     *
     * @return the total number of threads started.
     */
    public long getThreadsStarted() {
        return threadsStarted;
    }
    
    /**
     * Returns the time spent in application code (excluding time in safe points)
     * since the Java virtual machine start.
     *
     * @return the application time in milliseconds. Returns 0 if unsupported.
     */
    public long getApplicationTime() {
        return applicationTime;
    }
    
    /**
     * Returns {@link Jvm} for which this information is valid.
     *
     * @return instance of {@link Jvm}
     */
    public Jvm getMonitoredVm() {
        return monitoredVm;
    }
    
    /**
     * Returns the uptime of the Java virtual machine in milliseconds.
     *
     * @return uptime of the Java virtual machine in milliseconds.
     */
    public long getUpTime() {
        return upTime;
    }
    
    /**
     * Returns the amount of memory in bytes that is available for
     * the Java virtual machine to use.
     *
     * @return the amount of memory in bytes. Index 0 is for heap,
     * index 1 is for Permanent Generation (PermGen)
     *
     */
    public long[] getGenCapacity() {
        return genCapacity.clone();
    }
    
    /**
     * Returns the currently used amount of memory in bytes.
     *
     * @return the amount of currently used memory in bytes. Index 0 is for heap,
     * index 1 is for Permanent Generation (PermGen)
     *
     */
    public long[] getGenUsed() {
        return genUsed.clone();
    }
    
    /**
     * Returns the maximum amount of memory in bytes that is available for
     * the Java virtual machine to use.
     *
     * @return the maximum amount of memory in bytes. Index 0 is for heap,
     * index 1 is for Permanent Generation (PermGen)
     *
     */
    public long[] getGenMaxCapacity() {
        return genMaxCapacity.clone();
    }
    
    /**
     * Returns the approximate accumulated process CPU elapsed time
     * in nanoseconds.  This method returns <tt>-1</tt> if the collection
     * elapsed time is undefined for this collector.
     *
     * @return the approximate accumulated process CPU elapsed time
     * in nanoseconds.
     */
    public long getProcessCpuTime() {
        return processCpuTime;
    }
    
    /**
     * Returns the approximate accumulated (for all collectors) collection elapsed time
     * in milliseconds.  This method returns <tt>-1</tt> if the collection
     * elapsed time is undefined for this collector.
     * <p>
     * The Java virtual machine implementation may use a high resolution
     * timer to measure the elapsed time.  This method may return the
     * same value even if the collection count has been incremented
     * if the collection elapsed time is very short.
     *
     * @return the approximate accumulated collection elapsed time
     * in milliseconds.
     */
    public long getCollectionTime() {
        return collectionTime;
    }
}
