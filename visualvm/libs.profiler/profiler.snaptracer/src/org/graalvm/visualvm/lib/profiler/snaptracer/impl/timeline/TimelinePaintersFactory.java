/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.profiler.snaptracer.impl.timeline;

import org.graalvm.visualvm.lib.profiler.snaptracer.impl.timeline.items.ContinuousXYItemDescriptor;
import org.graalvm.visualvm.lib.profiler.snaptracer.impl.timeline.items.DiscreteXYItemDescriptor;
import org.graalvm.visualvm.lib.profiler.snaptracer.impl.timeline.items.ValueItemDescriptor;
import org.graalvm.visualvm.lib.profiler.snaptracer.impl.timeline.items.XYItemDescriptor;
import java.awt.Color;
import org.graalvm.visualvm.lib.profiler.snaptracer.ProbeItemDescriptor;
import org.graalvm.visualvm.lib.profiler.snaptracer.impl.IdeSnapshot;
import org.graalvm.visualvm.lib.profiler.snaptracer.impl.timeline.items.IconItemDescriptor;

/**
 *
 * @author Jiri Sedlacek
 */
final class TimelinePaintersFactory {

    static TimelineXYPainter createPainter(ProbeItemDescriptor descriptor,
                                           int itemIndex, PointsComputer c,
                                           IdeSnapshot snapshot) {

        // --- ValueItem -------------------------------------------------------
        if (descriptor instanceof ValueItemDescriptor)
            return createValuePainter((ValueItemDescriptor)descriptor, itemIndex, c, snapshot);

        return null;
    }

    private static TimelineXYPainter createValuePainter(
            ValueItemDescriptor descriptor, int itemIndex, PointsComputer c,
            IdeSnapshot snapshot) {

        // --- XYItem ----------------------------------------------------------
        if (descriptor instanceof ContinuousXYItemDescriptor)
            return createContinuousPainter((ContinuousXYItemDescriptor)descriptor, itemIndex, c);

        // --- BarItem ---------------------------------------------------------
        if (descriptor instanceof DiscreteXYItemDescriptor)
            return createDiscretePainter((DiscreteXYItemDescriptor)descriptor, itemIndex, c);

        // --- IconItem --------------------------------------------------------
        if (descriptor instanceof IconItemDescriptor)
            return createIconPainter((IconItemDescriptor)descriptor, itemIndex, snapshot);

        return null;
    }

    private static TimelineXYPainter createContinuousPainter(
            XYItemDescriptor descriptor, int itemIndex, PointsComputer c) {

        double dataFactor = descriptor.getDataFactor();

        float lineWidth = descriptor.getLineWidth();
        if (lineWidth == ProbeItemDescriptor.DEFAULT_LINE_WIDTH)
            lineWidth = 2f;

        Color lineColor = descriptor.getLineColor();
        if (lineColor == ProbeItemDescriptor.DEFAULT_COLOR)
            lineColor = TimelineColorFactory.getColor(itemIndex);

        Color fillColor = descriptor.getFillColor();
        if (fillColor == ProbeItemDescriptor.DEFAULT_COLOR) {
            if (lineColor == null)
                fillColor = TimelineColorFactory.getColor(itemIndex);
            else
                fillColor = TimelineColorFactory.getGradient(itemIndex)[0];
        }

        return new ContinuousXYPainter(lineWidth, lineColor, fillColor, dataFactor, c);
    }

    private static DiscreteXYPainter createDiscretePainter(
            DiscreteXYItemDescriptor descriptor, int itemIndex, PointsComputer c) {

        double dataFactor = descriptor.getDataFactor();

        float lineWidth = descriptor.getLineWidth();
        if (lineWidth == ProbeItemDescriptor.DEFAULT_LINE_WIDTH)
            lineWidth = 2f;

        Color lineColor = descriptor.getLineColor();
        if (lineColor == ProbeItemDescriptor.DEFAULT_COLOR)
            lineColor = TimelineColorFactory.getColor(itemIndex);

        Color fillColor = descriptor.getFillColor();
        if (fillColor == ProbeItemDescriptor.DEFAULT_COLOR) {
            if (lineColor == null)
                fillColor = TimelineColorFactory.getColor(itemIndex);
            else
                fillColor = TimelineColorFactory.getGradient(itemIndex)[0];
        }

        return new DiscreteXYPainter(lineWidth, lineColor, fillColor, descriptor.getWidth(),
                                     descriptor.isFixedWidth(), descriptor.isTopLineOnly(),
                                     descriptor.isOutlineOnly(), dataFactor, c);
    }

    private static TimelineIconPainter createIconPainter(
            IconItemDescriptor descriptor, int itemIndex, IdeSnapshot snapshot) {

        Color color = descriptor.getColor();
        if (color == ProbeItemDescriptor.DEFAULT_COLOR)
            color = TimelineColorFactory.getColor(itemIndex);

        return new TimelineIconPainter(color, snapshot);
    }

}
