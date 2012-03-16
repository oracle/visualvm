/*
 *  Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
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
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 */

package com.sun.tools.visualvm.modules.tracer.dtrace;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.modules.tracer.ItemValueFormatter;
import com.sun.tools.visualvm.modules.tracer.ProbeItemDescriptor;
import com.sun.tools.visualvm.modules.tracer.TracerProbe;
import com.sun.tools.visualvm.modules.tracer.TracerProbeDescriptor;
import com.sun.tools.visualvm.modules.tracer.TracerProgressObject;
import java.util.List;
import javax.swing.Icon;
import org.openide.util.Exceptions;
import org.opensolaris.os.dtrace.Aggregate;
import org.opensolaris.os.dtrace.Aggregation;
import org.opensolaris.os.dtrace.AggregationRecord;
import org.opensolaris.os.dtrace.Consumer;
import org.opensolaris.os.dtrace.DTraceException;

/**
 *
 * @author Tomas Hurka
 */
class BytesIOProbe extends TracerProbe.SessionAware<Application> {

    private static final String NAME = "I/O bytes";
    private static final String DESCR = "Monitors I/O bytes per second.";
    private static final int POSITION = 20;
    private static final String AGGREG_VALUE = "bytes";
    private static final int probes = 2;
    private long lastValues[];
    private DTracePackage dtrace;
    private long lastTime;

    BytesIOProbe(DTracePackage dt) {
        super(createItemDescriptors());
        dtrace = dt;
    }

    static final TracerProbeDescriptor createDescriptor(Icon icon, boolean available) {
        return new TracerProbeDescriptor(NAME, DESCR, icon, POSITION, available);
    }

    private static final ProbeItemDescriptor[] createItemDescriptors() {
        ProbeItemDescriptor[] descs = new ProbeItemDescriptor[probes];
        descs[0] = ProbeItemDescriptor.continuousLineItem("I/O reads",
                "Monitors read bytes per second.",
                ItemValueFormatter.DEFAULT_BYTES, 1d, 0, 1000);
        descs[1] = ProbeItemDescriptor.continuousLineItem("I/O writes",
                "Monitors written bytes per second.",
                ItemValueFormatter.DEFAULT_BYTES, 1d, 0, 1000);
        return descs;
    }

    @Override
    protected TracerProgressObject sessionInitializing(Application dataSource, int refresh) {
        lastTime = 0;
        lastValues = null;
        return super.sessionInitializing(dataSource, refresh);
    }

    @Override
    public long[] getItemValues(long timestamp) {
        long time;
        long vals[];
        long diffs[] = new long[probes];
        try {
            time = System.currentTimeMillis();
            vals = getCurrentValues();
            if (lastValues != null) {
                long delta = time - lastTime;
                for (int i = 0; i < vals.length; i++) {
                    diffs[i] = 1000* (vals[i] - lastValues[i]) / delta;
                }
            }
        } catch (DTraceException ex) {
            Exceptions.printStackTrace(ex);
            return diffs;
        }
        lastTime = time;
        lastValues = vals;
        return diffs;
    }

    private long[] getCurrentValues() throws DTraceException {
        long vals[] = new long[probes];
        Consumer c = dtrace.getConsumer();
        Aggregate result = c.getAggregate();
        Aggregation agg = result.getAggregation(AGGREG_VALUE);
        if (agg != null) {
            List<AggregationRecord> records = agg.getRecords();

            for (AggregationRecord record : records) {
                Number index = (Number) record.getTuple().get(0).getValue();
                Number val = record.getValue().getValue();
                vals[index.intValue()] = val.longValue();
            }
        }
        return vals;
    }
}
