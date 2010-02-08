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
import com.sun.tools.visualvm.application.jvm.JvmFactory;
import com.sun.tools.visualvm.application.jvm.MonitoredData;
import com.sun.tools.visualvm.modules.tracer.TracerPackage;
import com.sun.tools.visualvm.modules.tracer.TracerProbe;
import com.sun.tools.visualvm.modules.tracer.TracerProbeDescriptor;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.openide.util.ImageUtilities;

/**
 *
 * @author Jiri Sedlacek
 */
class MonitorPackage extends TracerPackage<Application> implements MonitorProbe.MonitoredDataResolver {

    static final Icon ICON = new ImageIcon(ImageUtilities.loadImage(
            "com/sun/tools/visualvm/modules/tracer/monitor/resources/monitor.png", true)); // NOI18N
    private static final String NAME = "Basic JVM Metrics";
    private static final String DESCR = "Provides the same basic JVM metrics as the Monitor tab.";
    private static final int POSITION = 50;

    private TracerProbeDescriptor cpuProbeDescriptor;
    private TracerProbeDescriptor heapProbeDescriptor;
    private TracerProbeDescriptor permgenProbeDescriptor;
    private TracerProbeDescriptor classesProbeDescriptor;
    private TracerProbeDescriptor threadsProbeDescriptor;
    private MonitorProbe cpuProbe;
    private MonitorProbe heapProbe;
    private MonitorProbe permgenProbe;
    private MonitorProbe classesProbe;
    private MonitorProbe threadsProbe;

    private final Application application;
    private final Jvm jvm;

    private long lastTimestamp = -1;
    private MonitoredData lastMonitoredData;


    MonitorPackage(Application application) {
        super(NAME, DESCR, ICON, POSITION);
        this.application = application;
        this.jvm = JvmFactory.getJVMFor(application);
    }


    public TracerProbeDescriptor[] getProbeDescriptors() {
        boolean available = jvm != null && jvm.isMonitoringSupported();
        cpuProbeDescriptor = CpuMonitorProbe.createDescriptor(ICON, available, jvm);
        heapProbeDescriptor = HeapMonitorProbe.createDescriptor(ICON, available, jvm);
        permgenProbeDescriptor = PermgenMonitorProbe.createDescriptor(ICON, available, jvm);
        classesProbeDescriptor = ClassesMonitorProbe.createDescriptor(ICON, available, jvm);
        threadsProbeDescriptor = ThreadsMonitorProbe.createDescriptor(ICON, available, jvm);
        return new TracerProbeDescriptor[] { cpuProbeDescriptor,
                                             heapProbeDescriptor,
                                             permgenProbeDescriptor,
                                             classesProbeDescriptor,
                                             threadsProbeDescriptor};
    }

    public TracerProbe<Application> getProbe(TracerProbeDescriptor descriptor) {
        if (descriptor == cpuProbeDescriptor) {
            if (cpuProbe == null)
                cpuProbe = new CpuMonitorProbe(cpuProbeDescriptor, this,
                                               application, jvm);
            return cpuProbe;
        } else if (descriptor == heapProbeDescriptor) {
            if (heapProbe == null)
                heapProbe = new HeapMonitorProbe(heapProbeDescriptor, this);
            return heapProbe;
        } else if (descriptor == permgenProbeDescriptor) {
            if (permgenProbe == null)
                permgenProbe = new PermgenMonitorProbe(permgenProbeDescriptor, this);
            return permgenProbe;
        } else if (descriptor == classesProbeDescriptor) {
            if (classesProbe == null)
                classesProbe = new ClassesMonitorProbe(classesProbeDescriptor, this);
            return classesProbe;
        } else if (descriptor == threadsProbeDescriptor) {
            if (threadsProbe == null)
                threadsProbe = new ThreadsMonitorProbe(threadsProbeDescriptor, this);
            return threadsProbe;
        } else {
            return null;
        }
    }

    public MonitoredData getMonitoredData(long timestamp) {
        // TODO: validity may be extended to some timeslot (~100ms)
        if (lastTimestamp != timestamp)
            lastMonitoredData = jvm.getMonitoredData();
        return lastMonitoredData;
    }

}
