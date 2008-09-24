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

package com.sun.tools.visualvm.jvm;


import com.sun.management.OperatingSystemMXBean;
import com.sun.tools.visualvm.application.jvm.Jvm;
import com.sun.tools.visualvm.application.jvm.MonitoredData;
import com.sun.tools.visualvm.tools.jmx.JvmMXBeans;
import com.sun.tools.visualvm.tools.jvmstat.JvmJvmstatModel;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.Collection;
import sun.jvmstat.monitor.LongMonitor;

/**
 *
 * @author Tomas Hurka
 */
public class MonitoredDataImpl extends MonitoredData {

  final private Jvm jvm;

  private MonitoredDataImpl(Jvm vm,JmxSupport jmxSupport) {
    OperatingSystemMXBean osMXBean = jmxSupport.getOperationSystem();
    Collection<GarbageCollectorMXBean> gcList = jmxSupport.getGarbageCollectorMXBeans();
    if (osMXBean != null) {
        processCpuTime = osMXBean.getProcessCpuTime();
    }
    if (gcList != null && !gcList.isEmpty()) {
        for (GarbageCollectorMXBean gcBean : gcList) {
            collectionTime+=gcBean.getCollectionTime();
        }
    }
    jvm = vm;
  }
  
  MonitoredDataImpl(Jvm vm,JvmJvmstatModel jvmstatModel,JmxSupport jmxSupport) {
    this(vm,jmxSupport);
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

  MonitoredDataImpl(Jvm vm,JmxSupport jmxSupport,JvmMXBeans jmxModel) {
    this(vm,jmxSupport);
    RuntimeMXBean runtimeBean = jmxModel.getRuntimeMXBean();
    upTime = runtimeBean.getUptime();
    ClassLoadingMXBean classBean = jmxModel.getClassLoadingMXBean();
    ThreadMXBean threadBean = jmxModel.getThreadMXBean();
    OperatingSystemMXBean osMXBean = jmxSupport.getOperationSystem();
    MemoryUsage mem = jmxModel.getMemoryMXBean().getHeapMemoryUsage();
    MemoryUsage perm = jmxSupport.getPermGenPool().getUsage();
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
    genCapacity[1] = perm.getCommitted();
    genUsed[1] = perm.getUsed();
    genMaxCapacity[1] = perm.getMax();
  }
  
  private long getLongValue(LongMonitor mon) {
    if (mon!=null) {
      return mon.longValue();
    }
    return 0;
  }
  
  public Jvm getJVM() {
    return jvm;
  }

}
