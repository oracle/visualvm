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

package com.sun.tools.visualvm.core.model.jvm;

import com.sun.tools.visualvm.core.model.jmx.JvmJmxModel;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import sun.jvmstat.monitor.LongMonitor;

/**
 *
 * @author Tomas Hurka
 */
public class MonitoredData {

  final private long loadedClasses;
  final private long sharedLoadedClasses;
  final private long sharedUnloadedClasses;
  final private long unloadedClasses;
  final private long threadsDaemon;
  final private long threadsLive;
  final private long threadsLivePeak;
  final private long threadsStarted;
  final private long applicationTime;
  final private long upTime;
  final private long[] genCapacity;
  final private long[] genUsed;
  final private long[] genMaxCapacity;
  final private JVM monitoredVm;

  MonitoredData(JvmstatJVM jvm) {
    loadedClasses = getLongValue(jvm.getLoadedClasses());
    sharedLoadedClasses = getLongValue(jvm.getSharedLoadedClasses());
    sharedUnloadedClasses = getLongValue(jvm.getSharedUnloadedClasses());
    unloadedClasses = getLongValue(jvm.getUnloadedClasses());
    threadsDaemon = getLongValue(jvm.getThreadsDaemon());
    threadsLive = getLongValue(jvm.getThreadsLive());
    threadsLivePeak = getLongValue(jvm.getThreadsLivePeak());
    threadsStarted = getLongValue(jvm.getThreadsStarted());
    applicationTime = 1000*getLongValue(jvm.getApplicationTime())/jvm.getOsFrequency();
    upTime = 1000*getLongValue(jvm.getUpTime())/jvm.getOsFrequency();
    genCapacity = jvm.getGenerationSum(jvm.getGenCapacity());
    genUsed = jvm.getGenerationSum(jvm.getGenUsed());
    genMaxCapacity = jvm.getGenMaxCapacity();
    monitoredVm = jvm;
  }

  MonitoredData(JmxJVM jvm) {
    JvmJmxModel jvmModel = jvm.getJmxModel();
    ClassLoadingMXBean classBean = jvmModel.getClassLoadingMXBean();
    ThreadMXBean threadBean = jvmModel.getThreadMXBean();
    RuntimeMXBean runtimeBean = jvmModel.getRuntimeMXBean();

    loadedClasses = classBean.getLoadedClassCount();
    sharedLoadedClasses = 0;
    sharedUnloadedClasses = 0;
    unloadedClasses = classBean.getUnloadedClassCount();
    threadsDaemon = threadBean.getDaemonThreadCount();
    threadsLive = threadBean.getThreadCount();
    threadsLivePeak = threadBean.getPeakThreadCount();
    threadsStarted = threadBean.getTotalStartedThreadCount();
    applicationTime = 0;
    upTime = runtimeBean.getUptime();
    genCapacity = null;
    genUsed = null;
    genMaxCapacity = null;
    monitoredVm = jvm;
  }
  
  private long getLongValue(LongMonitor mon) {
    if (mon!=null) {
      return mon.longValue();
    }
    return 0;
  }
  
  public long getLoadedClasses() {
    return loadedClasses;
  }

  public long getSharedLoadedClasses() {
    return sharedLoadedClasses;
  }

  public long getSharedUnloadedClasses() {
    return sharedUnloadedClasses;
  }

  public long getUnloadedClasses() {
    return unloadedClasses;
  }

  public long getThreadsDaemon() {
    return threadsDaemon;
  }

  public long getThreadsLive() {
    return threadsLive;
  }

  public long getThreadsLivePeak() {
    return threadsLivePeak;
  }

  public long getThreadsStarted() {
    return threadsStarted;
  }

  public long getApplicationTime() {
    return applicationTime;
  }

  public JVM getMonitoredVm() {
    return monitoredVm;
  }

  public long getUpTime() {
    return upTime;
  }

  public long[] getGenCapacity() {
    return genCapacity;
  }

  public long[] getGenUsed() {
    return genUsed;
  }
  
  public long[] getGenMaxCapacity() {
    return genMaxCapacity;
  }
}
