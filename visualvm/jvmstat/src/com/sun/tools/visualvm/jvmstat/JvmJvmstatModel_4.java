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

package com.sun.tools.visualvm.jvmstat;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.tools.jvmstat.JvmstatModel;
import com.sun.tools.visualvm.tools.jvmstat.JvmJvmstatModel;
import com.sun.tools.visualvm.tools.jvmstat.MonitoredValue;

/**
 *
 * @author Tomas Hurka
 */
class JvmJvmstatModel_4 extends JvmJvmstatModel {
    private static final String PERM_GEN_PREFIX = "hotspot.gc.generation.2.";

    JvmJvmstatModel_4(Application app,JvmstatModel stat) {
        super(app,stat);
        initMonitoredVales();
    }

    private void initMonitoredVales() {
      loadedClasses = jvmstat.findMonitoredValueByName("hotspot.rt.cl.classes.loaded");
      unloadedClasses = jvmstat.findMonitoredValueByName("hotspot.rt.cl.classes.unloaded");
      applicationTime = jvmstat.findMonitoredValueByName("sun.rt.applicationTime");
      upTime = jvmstat.findMonitoredValueByName("hotspot.rt.hrt.ticks");
      MonitoredValue osFrequencyMon = jvmstat.findMonitoredValueByName("hotspot.rt.hrt.frequency");
      osFrequency = getLongValue(osFrequencyMon);
      genCapacity = jvmstat.findMonitoredValueByPattern("hotspot.gc.generation.[0-9]+.capacity.current");
      genUsed = jvmstat.findMonitoredValueByPattern("hotspot.gc.generation.[0-9]+.space.[0-9]+.used");
      genMaxCapacity = getGenerationSum(jvmstat.findMonitoredValueByPattern("hotspot.gc.generation.[0-9]+.capacity.max"));
    }

    protected String getPermGenPrefix() {
        return PERM_GEN_PREFIX;
    }
    
}
