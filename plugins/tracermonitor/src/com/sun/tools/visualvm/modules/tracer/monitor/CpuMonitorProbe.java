/*
 *  Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Sun designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Sun in the LICENSE file that accompanied this code.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *  Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 *  CA 95054 USA or visit www.sun.com if you need additional information or
 *  have any questions.
 */

package com.sun.tools.visualvm.modules.tracer.monitor;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.application.jvm.Jvm;
import com.sun.tools.visualvm.application.jvm.MonitoredData;
import com.sun.tools.visualvm.modules.tracer.ProbeItemDescriptor;
import com.sun.tools.visualvm.modules.tracer.TracerProbeDescriptor;
import com.sun.tools.visualvm.tools.jmx.JmxModel;
import com.sun.tools.visualvm.tools.jmx.JmxModel.ConnectionState;
import com.sun.tools.visualvm.tools.jmx.JmxModelFactory;
import com.sun.tools.visualvm.tools.jmx.JvmMXBeans;
import com.sun.tools.visualvm.tools.jmx.JvmMXBeansFactory;
import java.lang.management.OperatingSystemMXBean;
import javax.swing.Icon;

/**
 *
 * @author Jiri Sedlacek
 */
class CpuMonitorProbe extends MonitorProbe {
    
    private static final String NAME = "Cpu & GC";
    private static final String DESCR = "Monitors CPU usage and GC activity.";
    private static final int POSITION = 10;

    private final boolean cpuSupported;
    private final boolean gcSupported;
    private final int processorsCount;

    private long prevUpTime = -1;
    private long prevProcessCpuTime = -1;
    private long prevProcessGcTime = -1;


    CpuMonitorProbe(TracerProbeDescriptor descriptor, MonitoredDataResolver resolver,
                    Application application, Jvm jvm) {
        super(descriptor, 2, createItemDescriptors(), resolver);
        cpuSupported = jvm.isCpuMonitoringSupported();
        gcSupported = jvm.isCollectionTimeSupported();
        int pCount = 1;
        JmxModel jmxModel = JmxModelFactory.getJmxModelFor(application);
        if (jmxModel != null && jmxModel.getConnectionState() == ConnectionState.CONNECTED) {
            JvmMXBeans mxbeans = JvmMXBeansFactory.getJvmMXBeans(jmxModel);
            if (mxbeans != null) {
                OperatingSystemMXBean osbean = mxbeans.getOperatingSystemMXBean();
                if (osbean != null) pCount = osbean.getAvailableProcessors();
            }
        }
        processorsCount = pCount;
    }


    long[] getValues(MonitoredData data) {
        long cpuUsage = -1;
        long gcUsage = -1;
         
        long upTime = data.getUpTime() * 1000000;
        
        long processCpuTime = cpuSupported ?
            data.getProcessCpuTime() / processorsCount : -1;
        long processGcTime  = gcSupported ?
            data.getCollectionTime() * 1000000 / processorsCount : -1;

        if (prevUpTime != -1) {
            long upTimeDiff = upTime - prevUpTime;

            if (cpuSupported && prevProcessCpuTime != -1) {
                long processTimeDiff = processCpuTime - prevProcessCpuTime;
                cpuUsage = upTimeDiff > 0 ? Math.min((long)(1000 * (float)processTimeDiff /
                                                     (float)upTimeDiff), 1000) : 0;
            }

            if (gcSupported && prevProcessGcTime != -1) {
                long processGcTimeDiff = processGcTime - prevProcessGcTime;
                gcUsage = upTimeDiff > 0 ? Math.min((long)(1000 * (float)processGcTimeDiff /
                                                    (float)upTimeDiff), 1000) : 0;
                if (cpuUsage != -1 && cpuUsage < gcUsage) gcUsage = cpuUsage;
            }
        }

        prevUpTime = upTime;
        prevProcessCpuTime = processCpuTime;
        prevProcessGcTime  = processGcTime;

        return new long[] {
            Math.max(cpuUsage, 0),
            Math.max(gcUsage, 0)
        };
    }


    static final TracerProbeDescriptor createDescriptor(Icon icon, boolean available,
                                                        Jvm jvm) {
        return new TracerProbeDescriptor(NAME, DESCR, icon, POSITION, available &&
                                         jvm.isCpuMonitoringSupported() ||
                                         jvm.isCollectionTimeSupported());
    }
    
    private static final ProbeItemDescriptor[] createItemDescriptors() {
        return new ProbeItemDescriptor[] {
            new ProbeItemDescriptor.LineItem("CPU usage"),
            new ProbeItemDescriptor.LineItem("GC activity")
        };
    }

}
