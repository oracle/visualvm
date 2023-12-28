/*
 * Copyright (c) 2007, 2023, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.jvmstat;

import java.util.List;
import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.tools.jvmstat.JvmJvmstatModel;
import org.graalvm.visualvm.tools.jvmstat.JvmstatModel;
import org.graalvm.visualvm.tools.jvmstat.MonitoredValue;
import org.openide.util.NbBundle;

/**
 *
 * @author Tomas Hurka
 */
class JvmJvmstatModel_8 extends JvmJvmstatModel {
    private static final String PERM_GEN_PREFIX_META = "sun.gc.metaspace.";   // NOI18N
    private static final String PERM_GEN_PREFIX_PERM = "sun.gc.generation.2.";   // NOI18N
    private static final String GC_TYPE_COUNTER_NAME = "sun.gc.policy.name";  // NOI18N
    private static final String G1_NAME = "GarbageFirst";           // NOI18N
    private static final String GC_COLLECTOR_COUNTER_NAME = "sun.gc.collector.0.name";  // NOI18N
    private static final String GENERATIONAL_ZGC_NAME = "ZGC minor collection pauses"; // NOI18N
    
    private String permGenPrefix = PERM_GEN_PREFIX_PERM;

    JvmJvmstatModel_8(Application app,JvmstatModel stat) {
        super(app,stat);
        initMonitoredVales();
    }

    private void initMonitoredVales() {
      loadedClasses = jvmstat.findMonitoredValueByName("java.cls.loadedClasses");   // NOI18N
      sharedLoadedClasses = jvmstat.findMonitoredValueByName("java.cls.sharedLoadedClasses");   // NOI18N
      sharedUnloadedClasses = jvmstat.findMonitoredValueByName("java.cls.sharedUnloadedClasses");   // NOI18N
      unloadedClasses = jvmstat.findMonitoredValueByName("java.cls.unloadedClasses");   // NOI18N
      threadsDaemon = jvmstat.findMonitoredValueByName("java.threads.daemon");  // NOI18N
      threadsLive = jvmstat.findMonitoredValueByName("java.threads.live");  // NOI18N
      threadsLivePeak = jvmstat.findMonitoredValueByName("java.threads.livePeak");  // NOI18N
      threadsStarted = jvmstat.findMonitoredValueByName("java.threads.started");    // NOI18N
      applicationTime = jvmstat.findMonitoredValueByName("sun.rt.applicationTime"); // NOI18N
      upTime = jvmstat.findMonitoredValueByName("sun.os.hrt.ticks");    // NOI18N
      MonitoredValue osFrequencyMon = jvmstat.findMonitoredValueByName("sun.os.hrt.frequency"); // NOI18N
      osFrequency = getLongValue(osFrequencyMon);
      genCapacity = jvmstat.findMonitoredValueByPattern("sun.gc.((generation.[0-9]+)|(metaspace)).capacity");   // NOI18N
      initPermGenPrefix(genCapacity);
      genUsed = jvmstat.findMonitoredValueByPattern("sun.gc.((generation.[0-9]+.space.[0-9]+)|(metaspace)).used");  // NOI18N
      genMaxCapacity=computeMaxCapacity();
    }

    private void initPermGenPrefix(List<MonitoredValue> monitors) {
        for (MonitoredValue m : monitors) {
            if (m.getName().startsWith(PERM_GEN_PREFIX_META)) {
                permGenPrefix = PERM_GEN_PREFIX_META;
                genName[1] = NbBundle.getMessage(JvmJvmstatModel_8.class, "LBL_Meta"); // NOI18N
                break;
            }
        }
    }
    
    private long[] computeMaxCapacity() {
        String gcType = jvmstat.findByName(GC_TYPE_COUNTER_NAME);
        String gcColName = jvmstat.findByName(GC_COLLECTOR_COUNTER_NAME);

        if (G1_NAME.equals(gcType) || GENERATIONAL_ZGC_NAME.equals(gcColName)) {
            // Generational ZGC Max Capacity GH-518
            // Generational ZGC sets the max capacity of all spaces to heap_capacity
            //
            // G1 Max Capacity GH-127
            // G1 sets the max capacity of all spaces to heap_capacity,
            // given that G1 don't always have a reasonable upper bound on how big
            // each space can grow.
            long[] maxCapacity = new long[2];
            MonitoredValue maxVal = jvmstat.findMonitoredValueByName("sun.gc.generation.0.maxCapacity");    // NOI18N
            MonitoredValue metaVal = jvmstat.findMonitoredValueByName("sun.gc.metaspace.maxCapacity");    // NOI18N

            maxCapacity[0] = getLongValue(maxVal);
            maxCapacity[1] = getLongValue(metaVal);
            return maxCapacity;
        }
        return getGenerationSum(jvmstat.findMonitoredValueByPattern("sun.gc.((generation.[0-9]+)|(metaspace)).maxCapacity")); // NOI18N
    }

    protected String getPermGenPrefix() {
        return permGenPrefix;
    }
    
}
