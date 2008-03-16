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
import com.sun.tools.visualvm.host.Host;
import com.sun.tools.visualvm.tools.jvmstat.Jvmstat;
import com.sun.tools.visualvm.tools.jvmstat.JvmstatModel;
import com.sun.tools.visualvm.tools.jvmstat.MonitoredValue;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

/**
 *
 * @author Tomas Hurka
 */
class JvmstatModel_5 extends JvmstatModel {
    private static final String PERM_GEN_PREFIX = "sun.gc.generation.2.";

    JvmstatModel_5(Application app,Jvmstat stat) {
        super(app,stat);
        initMonitoredVales();
    }

    private void initMonitoredVales() {
      loadedClasses = jvmstat.findMonitoredValueByName("java.cls.loadedClasses");
      sharedLoadedClasses = jvmstat.findMonitoredValueByName("java.cls.sharedLoadedClasses");
      sharedUnloadedClasses = jvmstat.findMonitoredValueByName("java.cls.sharedUnloadedClasses");
      unloadedClasses = jvmstat.findMonitoredValueByName("java.cls.unloadedClasses");
      threadsDaemon = jvmstat.findMonitoredValueByName("java.threads.daemon");
      threadsLive = jvmstat.findMonitoredValueByName("java.threads.live");
      threadsLivePeak = jvmstat.findMonitoredValueByName("java.threads.livePeak");
      threadsStarted = jvmstat.findMonitoredValueByName("java.threads.started");
      applicationTime = jvmstat.findMonitoredValueByName("sun.rt.applicationTime");
      upTime = jvmstat.findMonitoredValueByName("sun.os.hrt.ticks");
      MonitoredValue osFrequencyMon = jvmstat.findMonitoredValueByName("sun.os.hrt.frequency");
      osFrequency = getLongValue(osFrequencyMon);
      genCapacity = jvmstat.findMonitoredValueByPattern("sun.gc.generation.[0-9]+.capacity");
      genUsed = jvmstat.findMonitoredValueByPattern("sun.gc.generation.[0-9]+.space.[0-9]+.used");
      genMaxCapacity=getGenerationSum(jvmstat.findMonitoredValueByPattern("sun.gc.generation.[0-9]+.maxCapacity"));
    }

    protected String getPermGenPrefix() {
        return PERM_GEN_PREFIX;
    }
    
}
