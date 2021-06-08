/*
 *  Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.modules.tracer.dtrace;

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.modules.tracer.ItemValueFormatter;
import org.graalvm.visualvm.modules.tracer.ProbeItemDescriptor;
import org.graalvm.visualvm.modules.tracer.TracerProbe;
import org.graalvm.visualvm.modules.tracer.TracerProbeDescriptor;
import org.graalvm.visualvm.modules.tracer.TracerProgressObject;
import org.graalvm.visualvm.tools.jmx.JmxModel;
import org.graalvm.visualvm.tools.jmx.JmxModelFactory;
import org.graalvm.visualvm.tools.jmx.JvmMXBeans;
import org.graalvm.visualvm.tools.jmx.JvmMXBeansFactory;
import java.lang.management.ThreadMXBean;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
class JVMOverheadProbe extends TracerProbe.SessionAware<Application> {

    private static final String NAME = "JVM overhead";
    private static final String DESCR = "Monitors JVM overhead.";
    private static final int POSITION = 16;
    private static final String AGGREG_VALUE = "threads";
    private static final int probes = 1;
    private final int processorsCount;
    private long lastValues[];
    private DTracePackage dtrace;
    private long lastTime;
    private ThreadMXBean threads;
    private Set<Long> threadIds;

    JVMOverheadProbe(DTracePackage dt) {
        super(createItemDescriptors());
        processorsCount = getItemsCount();
        dtrace = dt;
    }

    static final TracerProbeDescriptor createDescriptor(Icon icon, boolean available) {
        return new TracerProbeDescriptor(NAME, DESCR, icon, POSITION, available);
    }

    private static final ProbeItemDescriptor[] createItemDescriptors() {
        ProbeItemDescriptor[] descs = new ProbeItemDescriptor[probes];
        descs[0] = ProbeItemDescriptor.continuousLineItem("JVM overhead",
                "Monitors relative JVM overhead (%)",
                ItemValueFormatter.DEFAULT_PERCENT, 1d, 0, 1000);
        return descs;
    }

    @Override
    protected TracerProgressObject sessionInitializing(Application dataSource, int refresh) {
        lastTime = 0;
        lastValues = null;
        return super.sessionInitializing(dataSource, refresh);
    }

    @Override
    protected void sessionRunning(Application dataSource) {
        JmxModel model = JmxModelFactory.getJmxModelFor(dataSource);
        JvmMXBeans beans = JvmMXBeansFactory.getJvmMXBeans(model);
        threads = beans.getThreadMXBean();
        threadIds = new HashSet();
        long[] tids = threads.getAllThreadIds();
        for (long tid : tids) {
            threadIds.add(Long.valueOf(tid));
        }
//        System.out.println(threadIds);
        super.sessionRunning(dataSource);
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
                    diffs[i] = (vals[i] - lastValues[i]) / (1000 * delta);
                }
            } else {
                long[] tids = threads.getAllThreadIds();
                for (long tid : tids) {
                    threadIds.add(Long.valueOf(tid));
                }
//                System.out.println(threadIds);
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
            long time = 0;

            for (AggregationRecord record : records) {
                Number tid = (Number) record.getTuple().get(0).getValue();
                Number val = record.getValue().getValue();
                if (!threadIds.contains(Long.valueOf(tid.longValue()))) {
                    time += val.longValue();
//                    System.out.println("VM thread id: " + tid.longValue());
                }
            }
            vals[0] = time / processorsCount;
        }
        return vals;
    }
}
