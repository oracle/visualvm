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
public class SunJVM_4 extends JvmstatJVM {
  private static final String PERM_GEN_PREFIX = "hotspot.gc.generation.2.";

  SunJVM_4(JvmstatApplication app,MonitoredVm vm) {
    super(app,vm);
  }

  public boolean is14() {
    return true;
  }

  String getPermGenPrefix() {
    return PERM_GEN_PREFIX;
  }

  public boolean isMemoryMonitoringSupported() {
    return true;
  }

  public boolean isClassMonitoringSupported() {
    return true;
  }

  void initListeners() {
    try {
      loadedClasses = (LongMonitor) monitoredVm.findByName("hotspot.rt.cl.classes.loaded");
      unloadedClasses = (LongMonitor) monitoredVm.findByName("hotspot.rt.cl.classes.unloaded");
      applicationTime = (LongMonitor) monitoredVm.findByName("sun.rt.applicationTime");
      upTime = (LongMonitor) monitoredVm.findByName("hotspot.rt.hrt.ticks");
      LongMonitor osFrequencyMon = ((LongMonitor)monitoredVm.findByName("hotspot.rt.hrt.frequency"));
      osFrequency = osFrequencyMon.longValue();
      genCapacity = monitoredVm.findByPattern("hotspot.gc.generation.[0-9]+.capacity.current");
      genUsed = monitoredVm.findByPattern("hotspot.gc.generation.[0-9]+.space.[0-9]+.used");
      genMaxCapacity = getGenerationSum(monitoredVm.findByPattern("hotspot.gc.generation.[0-9]+.capacity.max"));
      listeners = new HashSet();
      monitoredVm.addVmListener(this);
    } catch (MonitorException ex) {
      ex.printStackTrace();
    }
  }
}
