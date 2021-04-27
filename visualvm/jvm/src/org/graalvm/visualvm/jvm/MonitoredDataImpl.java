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

package org.graalvm.visualvm.jvm;


import org.graalvm.visualvm.application.jvm.MonitoredData;
import org.graalvm.visualvm.tools.jmx.JvmMXBeans;
import org.graalvm.visualvm.tools.jvmstat.JvmJvmstatModel;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.Collection;
import java.util.logging.Logger;
import org.graalvm.visualvm.application.jvm.Jvm;

/**
 *
 * @author Tomas Hurka
 */
public class MonitoredDataImpl extends MonitoredData {
  private final static Logger LOGGER = Logger.getLogger(MonitoredDataImpl.class.getName());

  private MonitoredDataImpl(Jvm jvm, JmxSupport jmxSupport) {
    monitoredVm = jvm;
    try {
        Collection<GarbageCollectorMXBean> gcList = jmxSupport.getGarbageCollectorMXBeans();

        if (jmxSupport.hasProcessCPUTimeAttribute()) {
            processCpuTime = jmxSupport.getProcessCPUTime();
        }
        if (gcList != null && !gcList.isEmpty()) {
            for (GarbageCollectorMXBean gcBean : gcList) {
                collectionTime+=gcBean.getCollectionTime();
            }
        }
    } catch (Exception ex) {
        LOGGER.throwing(MonitoredDataImpl.class.getName(), "MonitoredDataImpl.<init>", ex); // NOI18N
    }
  }
  
  MonitoredDataImpl(Jvm jvm, JvmJvmstatModel jvmstatModel,JmxSupport jmxSupport) {
    this(jvm, jmxSupport);
    loadedClasses = jvmstatModel.getLoadedClasses();
    sharedLoadedClasses = jvmstatModel.getSharedLoadedClasses();
    sharedUnloadedClasses = jvmstatModel.getSharedUnloadedClasses();
    unloadedClasses = jvmstatModel.getUnloadedClasses();
    threadsDaemon = jvmstatModel.getThreadsDaemon();
    threadsLive = jvmstatModel.getThreadsLive();
    threadsLivePeak = jvmstatModel.getThreadsLivePeak();
    threadsStarted = jvmstatModel.getThreadsStarted();
    applicationTime = 1000*jvmstatModel.getApplicationTime()/jvmstatModel.getOsFrequency();
    upTime = 1000*jvmstatModel.getUpTime()/jvmstatModel.getOsFrequency();
    genCapacity = jvmstatModel.getGenCapacity();
    genUsed = jvmstatModel.getGenUsed();
    genMaxCapacity = jvmstatModel.getGenMaxCapacity();
  }

  MonitoredDataImpl(Jvm jvm, JmxSupport jmxSupport,JvmMXBeans jmxModel) {
    this(jvm, jmxSupport);
    RuntimeMXBean runtimeBean = jmxModel.getRuntimeMXBean();
    upTime = runtimeBean.getUptime();
    ClassLoadingMXBean classBean = jmxModel.getClassLoadingMXBean();
    ThreadMXBean threadBean = jmxModel.getThreadMXBean();
    MemoryUsage mem = jmxModel.getMemoryMXBean().getHeapMemoryUsage();
    MemoryPoolMXBean permBean = jmxSupport.getPermGenPool();
    unloadedClasses = classBean.getUnloadedClassCount();
    loadedClasses = classBean.getLoadedClassCount() + unloadedClasses;
    sharedLoadedClasses = 0;
    sharedUnloadedClasses = 0;
    threadsDaemon = threadBean.getDaemonThreadCount();
    threadsLive = threadBean.getThreadCount();
    threadsLivePeak = threadBean.getPeakThreadCount();
    threadsStarted = threadBean.getTotalStartedThreadCount();
    applicationTime = 0;
    genCapacity = new long[2];
    genUsed = new long[2];
    genMaxCapacity = new long[2];
    genCapacity[0] = mem.getCommitted();
    genUsed[0] = mem.getUsed();
    genMaxCapacity[0] = mem.getMax();
    if (permBean != null) {
        MemoryUsage perm = permBean.getUsage();
        genCapacity[1] = perm.getCommitted();
        genUsed[1] = perm.getUsed();
        genMaxCapacity[1] = perm.getMax();
    }
  }
}
