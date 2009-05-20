/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
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

package org.netbeans.lib.profiler.charts.xy;

import java.util.Iterator;
import org.netbeans.lib.profiler.charts.ChartContext;
import org.netbeans.lib.profiler.charts.axis.AxisMark;
import org.netbeans.lib.profiler.charts.axis.AxisMarksComputer;
import org.netbeans.lib.profiler.charts.axis.DecimalAxisUtils;
import org.netbeans.lib.profiler.charts.axis.LongMark;
import org.netbeans.lib.profiler.charts.swing.Utils;

/**
 *
 * @author Jiri Sedlacek
 */
public class DecimalXYItemMarksComputer extends XYItemMarksComputer {

    private double scale;
    private long step;


    public DecimalXYItemMarksComputer(XYItem item,
                                      XYItemPainter painter,
                                      ChartContext context,
                                      int orientation) {

        super(item, painter, context, orientation);

        scale = -1;
        step = -1;

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
            } else {
                step = DecimalAxisUtils.getDecimalUnits(scale, getMinMarksDistance());
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
                    int position = Utils.checkedInt(
                                         painter.getItemView(value, item, context));
                    return new LongMark(value, position);
                }

            };

        }

}
