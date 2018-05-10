/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.modules.tracer.impl.timeline;

import org.graalvm.visualvm.modules.tracer.ProbeItemDescriptor;
import org.graalvm.visualvm.modules.tracer.impl.timeline.items.ContinuousXYItemDescriptor;
import org.graalvm.visualvm.modules.tracer.impl.timeline.items.DiscreteXYItemDescriptor;
import org.graalvm.visualvm.modules.tracer.impl.timeline.items.ValueItemDescriptor;
import org.graalvm.visualvm.modules.tracer.impl.timeline.items.XYItemDescriptor;
import java.awt.Color;

/**
 *
 * @author Jiri Sedlacek
 */
final class TimelinePaintersFactory {

    static TimelineXYPainter createPainter(ProbeItemDescriptor descriptor,
                                           int itemIndex, PointsComputer c) {

        // --- ValueItem -------------------------------------------------------
        if (descriptor instanceof ValueItemDescriptor)
            return createValuePainter((ValueItemDescriptor)descriptor, itemIndex, c);

        return null;
    }

    private static TimelineXYPainter createValuePainter(
            ValueItemDescriptor descriptor, int itemIndex, PointsComputer c) {

        // --- XYItem ----------------------------------------------------------
        if (descriptor instanceof ContinuousXYItemDescriptor)
            return createContinuousPainter((ContinuousXYItemDescriptor)descriptor, itemIndex, c);
        
        // --- BarItem ---------------------------------------------------------
        if (descriptor instanceof DiscreteXYItemDescriptor)
            return createDiscretePainter((DiscreteXYItemDescriptor)descriptor, itemIndex, c);

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

}
