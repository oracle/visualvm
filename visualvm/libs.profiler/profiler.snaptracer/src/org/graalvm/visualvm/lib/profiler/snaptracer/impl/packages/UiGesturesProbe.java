/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.profiler.snaptracer.impl.packages;

import java.io.IOException;
import org.graalvm.visualvm.lib.profiler.snaptracer.ItemValueFormatter;
import org.graalvm.visualvm.lib.profiler.snaptracer.ProbeItemDescriptor;
import org.graalvm.visualvm.lib.profiler.snaptracer.TracerProbe;
import org.graalvm.visualvm.lib.profiler.snaptracer.impl.IdeSnapshot;
import org.openide.util.Exceptions;

/**
 *
 * @author Jiri Sedlacek
 */
class UiGesturesProbe extends TracerProbe {

    private IdeSnapshot snapshot;


    UiGesturesProbe(IdeSnapshot snapshot) {
        super(descriptors(1, snapshot));
        this.snapshot = snapshot;
    }

    public long[] getItemValues(int sampleIndex) {
        return values(sampleIndex);
    }


    private static ProbeItemDescriptor[] descriptors(int items, IdeSnapshot snapshot) {
        ProbeItemDescriptor[] descriptors = new ProbeItemDescriptor[items];
        descriptors[0] = ProbeItemDescriptor.iconItem("UI Gesture",
                             "Shows UI actions performed by the user in the IDE",
                             new UiGesturesFormatter(snapshot));
        return descriptors;
    }

    private long[] values(int sampleIndex) {
        long[] values = new long[1];
        try {
            values[0] = snapshot.getValue(sampleIndex, 1);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return values;
    }


    private static class UiGesturesFormatter extends ItemValueFormatter {

        private IdeSnapshot snapshot;

        UiGesturesFormatter(IdeSnapshot snapshot) {
            this.snapshot = snapshot;
        }

        public String formatValue(long value, int format) {
            switch (format) {
                case FORMAT_TOOLTIP:
                case FORMAT_DETAILS:
                case FORMAT_EXPORT:
                    IdeSnapshot.LogRecordInfo info = snapshot.getLogInfoForValue(value);
                    String message = null;
                    if (info != null) {
                        message = info.getDisplayName();
                        if (message == null) message = info.getName();
                        if (message == null) message = "<unknown>";
                    }
                    return message != null ? message : "<none>";
                case FORMAT_UNITS:
                    return "";
                default:
                    return null;
            }
        }

        public String getUnits(int format) {
            return "";
        }

    }

}
