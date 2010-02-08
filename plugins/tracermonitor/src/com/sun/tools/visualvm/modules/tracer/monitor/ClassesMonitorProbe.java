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

import com.sun.tools.visualvm.application.jvm.Jvm;
import com.sun.tools.visualvm.application.jvm.MonitoredData;
import com.sun.tools.visualvm.modules.tracer.ProbeItemDescriptor;
import com.sun.tools.visualvm.modules.tracer.TracerProbeDescriptor;
import javax.swing.Icon;

/**
 *
 * @author Jiri Sedlacek
 */
class ClassesMonitorProbe extends MonitorProbe {
    
    private static final String NAME = "Classes";
    private static final String DESCR = "Monitors number of classes loaded by the JVM.";
    private static final int POSITION = 40;


    ClassesMonitorProbe(TracerProbeDescriptor descriptor, MonitoredDataResolver resolver) {
        super(descriptor, 2, createItemDescriptors(), resolver);
    }


    long[] getValues(MonitoredData data) {
        long sharedUnloaded = data.getSharedUnloadedClasses();
        long totalUnloaded  = data.getUnloadedClasses();
        long sharedClasses  = data.getSharedLoadedClasses() - sharedUnloaded;
        long totalClasses   = data.getLoadedClasses() - totalUnloaded + sharedClasses;
        return new long[] {
            totalClasses,
            sharedClasses
        };
    }


    static final TracerProbeDescriptor createDescriptor(Icon icon, boolean available,
                                                        Jvm jvm) {
        return new TracerProbeDescriptor(NAME, DESCR, icon, POSITION, available &&
                                         jvm.isClassMonitoringSupported());
    }
    
    private static final ProbeItemDescriptor[] createItemDescriptors() {
        return new ProbeItemDescriptor[] {
            new ProbeItemDescriptor.LineItem("Total loaded"),
            new ProbeItemDescriptor.LineItem("Shared loaded")
        };
    }

}
