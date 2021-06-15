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

package org.graalvm.visualvm.lib.charts.xy;

import java.util.Iterator;
import org.graalvm.visualvm.lib.charts.ChartContext;
import org.graalvm.visualvm.lib.charts.axis.AxisMark;
import org.graalvm.visualvm.lib.charts.axis.AxisMarksComputer;
import org.graalvm.visualvm.lib.charts.axis.BytesAxisUtils;
import org.graalvm.visualvm.lib.charts.axis.BytesMark;
import org.graalvm.visualvm.lib.charts.swing.Utils;

/**
 *
 * @author Jiri Sedlacek
 */
public class BytesXYItemMarksComputer extends XYItemMarksComputer {

    private double scale;
    private long step;
    private int radix;


    public BytesXYItemMarksComputer(XYItem item,
                                    XYItemPainter painter,
                                    ChartContext context,
                                    int orientation) {

        super(item, painter, context, orientation);

        scale = -1;
        step = -1;
        radix = -1;

    }

    protected boolean refreshConfiguration() {
        double oldScale = scale;

        if (context.getViewWidth() == 0) {
            scale = -1;
//        } else if (item.getValuesCount() == 0) {
//            // Initial scale
//            scale = -1;
        } else {
            scale = painter.getItemValueScale(item, context);
        }

        if (oldScale != scale) {
            if (scale == -1) {
                step = -1;
                radix = -1;
            } else {
                long[] units = BytesAxisUtils.getBytesUnits(scale, getMinMarksDistance());
                step = units[0];
                radix = step == -1 ? -1 : (int)units[1];
            }

            oldScale = scale;
            return true;
        } else {
            return false;
        }
    }


    public Iterator<AxisMark> marksIterator(int start, int end) {
            if (step == -1) return EMPTY_ITERATOR;

            final long dataStart = ((long)painter.getItemValue(start, item,
                                          context) / step) * step;
            final long dataEnd = ((long)painter.getItemValue(end, item,
                                          context) / step) * step;
            final long iterCount = Math.abs(dataEnd - dataStart) / step + 2;
            final long[] iterIndex = new long[] { 0 };


            return new AxisMarksComputer.AbstractIterator() {

                public boolean hasNext() {
                    return iterIndex[0] < iterCount;
                }

                public AxisMark next() {
                    long value = reverse ? dataStart - iterIndex[0] * step :
                                           dataStart + iterIndex[0] * step;

                    iterIndex[0]++;
                    int position = Utils.checkedInt(Math.floor(
                                         painter.getItemView(value, item, context)));
                    return new BytesMark(value, position, radix);
                }

            };

        }

}
