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

import com.sun.tools.visualvm.core.application.JvmstatApplication;
import java.util.HashSet;
import sun.jvmstat.monitor.LongMonitor;
import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.MonitoredVm;

/**
 *
 * @author Tomas Hurka
 */
public class SunJVM_5 extends SunJVM_4 {
  private static final String PERM_GEN_PREFIX = "sun.gc.generation.2.";

  SunJVM_5(JvmstatApplication app,MonitoredVm vm) {
    super(app,vm);
  }

  public boolean is15() {
    return true;
  }

  public boolean is14() {
    return false;
  }

  String getPermGenPrefix() {
    return PERM_GEN_PREFIX;
  }

  public boolean isThreadMonitoringSupported() {
    return true;
  }

  void initListeners() {
    try {
      loadedClasses = (LongMonitor) monitoredVm.findByName("java.cls.loadedClasses");
      sharedLoadedClasses = (LongMonitor) monitoredVm.findByName("java.cls.sharedLoadedClasses");
      sharedUnloadedClasses = (LongMonitor) monitoredVm.findByName("java.cls.sharedUnloadedClasses");
      unloadedClasses = (LongMonitor) monitoredVm.findByName("java.cls.unloadedClasses");
      threadsDaemon = (LongMonitor) monitoredVm.findByName("java.threads.daemon");
      threadsLive = (LongMonitor) monitoredVm.findByName("java.threads.live");
      threadsLivePeak = (LongMonitor) monitoredVm.findByName("java.threads.livePeak");
      threadsStarted = (LongMonitor) monitoredVm.findByName("java.threads.started");
      applicationTime = (LongMonitor) monitoredVm.findByName("sun.rt.applicationTime");
      upTime = (LongMonitor) monitoredVm.findByName("sun.os.hrt.ticks");
      LongMonitor osFrequencyMon = ((LongMonitor)monitoredVm.findByName("sun.os.hrt.frequency"));
      osFrequency = osFrequencyMon.longValue();
      genCapacity = monitoredVm.findByPattern("sun.gc.generation.[0-9]+.capacity");
      genUsed = monitoredVm.findByPattern("sun.gc.generation.[0-9]+.space.[0-9]+.used");
      genMaxCapacity=getGenerationSum(monitoredVm.findByPattern("sun.gc.generation.[0-9]+.maxCapacity"));
      listeners = new HashSet();
      monitoredVm.addVmListener(this);
    } catch (MonitorException ex) {
      ex.printStackTrace();
    }
  }

}
