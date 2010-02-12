/*
 * Copyright 2007-2010 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tools.visualvm.modules.tracer.impl.timeline;

import com.sun.tools.visualvm.modules.tracer.ProbeItemDescriptor;
import java.awt.Color;

/**
 *
 * @author Jiri Sedlacek
 */
final class TimelinePaintersFactory {

    static TimelineXYPainter createPainter(ProbeItemDescriptor descriptor,
                                           int itemIndex) {

        // --- ValueItem -------------------------------------------------------
        if (descriptor instanceof ProbeItemDescriptor.ValueItem)
            return createValuePainter((ProbeItemDescriptor.ValueItem)descriptor,
                                      itemIndex);

        return null;
    }

    private static TimelineXYPainter createValuePainter(
            ProbeItemDescriptor.ValueItem descriptor, int itemIndex) {

        // --- XYItem ----------------------------------------------------------
        if (descriptor instanceof ProbeItemDescriptor.XYItem)
            return createXYPainter((ProbeItemDescriptor.XYItem)descriptor,
                                   itemIndex);
        
//        // --- BarItem ---------------------------------------------------------
//        if (descriptor instanceof ProbeItemDescriptor.BarItem)
//            return createXYPainter((ProbeItemDescriptor.BarItem)descriptor,
//                                   itemIndex);

        return null;
    }

    private static TimelineXYPainter createXYPainter(
            ProbeItemDescriptor.XYItem descriptor, int itemIndex) {

        float lineWidth = descriptor.getLineWidth();
        if (lineWidth == ProbeItemDescriptor.XYItem.DEFAULT_LINE_WIDTH)
            lineWidth = 2f;

        Color lineColor = descriptor.getLineColor();
        if (lineColor == ProbeItemDescriptor.DEFAULT_COLOR)
            lineColor = TimelineColorFactory.getColor(itemIndex);

        Color fillColor1 = descriptor.getFillColor1();
        if (fillColor1 == ProbeItemDescriptor.DEFAULT_COLOR) {
            if (descriptor instanceof ProbeItemDescriptor.FillItem)
                fillColor1 = TimelineColorFactory.getColor(itemIndex);
            else
                fillColor1 = TimelineColorFactory.getGradient(itemIndex)[0];
        }

        Color fillColor2 = descriptor.getFillColor2();
        if (fillColor2 == ProbeItemDescriptor.DEFAULT_COLOR) {
            if (descriptor instanceof ProbeItemDescriptor.FillItem)
                fillColor2 = null;
            else
                fillColor2 = TimelineColorFactory.getGradient(itemIndex)[1];
        }

        return TimelineXYPainter.absolutePainter(lineWidth, lineColor,
                                                 fillColor1, fillColor2);
    }

}
