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

import com.sun.tools.visualvm.application.jvm.MonitoredData;
import com.sun.tools.visualvm.modules.tracer.ProbeItemDescriptor;
import com.sun.tools.visualvm.modules.tracer.TracerProbe;
import com.sun.tools.visualvm.modules.tracer.TracerProbeDescriptor;
import java.util.Arrays;

/**
 *
 * @author Jiri Sedlacek
 */
abstract class MonitorProbe extends TracerProbe {

    private final MonitoredDataResolver resolver;

    private final int valuesCount;


    MonitorProbe(TracerProbeDescriptor descriptor, int valuesCount,
                ProbeItemDescriptor[] itemDescriptors,
                MonitoredDataResolver resolver) {
        super(descriptor, itemDescriptors);
        this.valuesCount = valuesCount;
        this.resolver = resolver;
    }


    public synchronized final long[] getItemValues(long timestamp) {
        try {
            MonitoredData data = resolver.getMonitoredData(timestamp);
            return getValues(data);
        } catch (Throwable t) {
            System.err.println(">>> Failed to read Monitor data: " + t);
        }

        long[] values = new long[valuesCount];
        Arrays.fill(values, 0);
        return values;
    }

    abstract long[] getValues(MonitoredData data);


    static interface MonitoredDataResolver {
        public MonitoredData getMonitoredData(long timestamp);
    }

}
