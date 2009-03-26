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

package org.netbeans.lib.profiler.charts;

import java.util.Iterator;
import javax.swing.SwingConstants;
import org.netbeans.lib.profiler.charts.xy.XYItem;
import org.netbeans.lib.profiler.charts.xy.XYItemPainter;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class AxisMarksComputer {

    // --- Abstract definition -------------------------------------------------


    public abstract Iterator<Mark> marksIterator(int start, int end);
    
    
    // --- Time computer -------------------------------------------------------

    public static class TimeMarksComputer extends AxisMarksComputer {
        
        private final ChartContext context;
        private final int orientation;
        private final int minMarksDistance;

        private final boolean horizontal;
        private final boolean reverse;


        public TimeMarksComputer(ChartContext context, int orientation,
                                 int minMarksDistance) {
            this.context = context;
            this.orientation = orientation;
            this.minMarksDistance = minMarksDistance;

            horizontal = orientation == SwingConstants.HORIZONTAL;
            reverse = horizontal ? context.isRightBased() :
                                   context.isBottomBased();
        }

        public Iterator<Mark> marksIterator(int start, int end) {
            double scale =  horizontal ? context.getViewWidth(1d) :
                                         context.getViewHeight(1d);
            
            final long step = getTimeUnits(scale, minMarksDistance);
            final long dataStart = horizontal ?
                                   ((long)context.getDataX(start) / step) * step :
                                   ((long)context.getDataY(start) / step) * step;
            final long dataEnd = horizontal ?
                                   ((long)context.getDataX(end) / step) * step :
                                   ((long)context.getDataY(end) / step) * step;
            final long iterCount = Math.abs(dataEnd - dataStart) / step + 2;
            final long[] iterIndex = new long[] { 0 };


            return new Iterator<Mark>() {

                public boolean hasNext() {
                    return iterIndex[0] < iterCount;
                }

                public Mark next() {
                    long value = reverse ? dataStart - iterIndex[0] * step :
                                           dataStart + iterIndex[0] * step;
                    iterIndex[0]++;
                    int position = ChartContext.getCheckedIntValue(
                                        context.getViewX(value));
                    return new Mark(value, position);
                }

                public void remove() {
                    throw new UnsupportedOperationException(
                              "AxisMarksComputer does not support remove()");
                }

            };

        }

    }


    // --- Decimal computer ----------------------------------------------------

    public static class VerticalDecimalComputer extends AxisMarksComputer {

        private final XYItem item;
        private final XYItemPainter painter;
        private final ChartContext context;
        private final int minMarksDistance;

        private final boolean reverse;


        public VerticalDecimalComputer(XYItem item, XYItemPainter painter,
                                       ChartContext context, int minMarksDistance) {
            this.item = item;
            this.painter = painter;
            this.context = context;
            this.minMarksDistance = minMarksDistance;

            reverse = context.isBottomBased();
        }

        public Iterator<Mark> marksIterator(int start, int end) {
            double scale = painter.getItemValueScale(item, context);
            final long step = getDecimalUnits(scale, minMarksDistance);
            final long dataStart = ((long)painter.getItemValue(start, item,
                                          context) / step) * step;
            final long dataEnd = ((long)painter.getItemValue(end, item,
                                          context) / step) * step;
            final long iterCount = Math.abs(dataEnd - dataStart) / step + 2;
//            System.err.println(">>> start: " + dataStart + ", end: " + dataEnd + ", scale: " + 1d/scale + ", step: " + step + ", iterCount: " + iterCount);
            final long[] iterIndex = new long[] { 0 };


            return new Iterator<Mark>() {

                public boolean hasNext() {
                    return iterIndex[0] < iterCount;
                }

                public Mark next() {
                    long value = reverse ? dataStart - iterIndex[0] * step :
                                           dataStart + iterIndex[0] * step;

                    iterIndex[0]++;
                    int position = ChartContext.getCheckedIntValue(
                                        painter.getItemView(value, item, context));
                    return new Mark(value, position);
                }

                public void remove() {
                    throw new UnsupportedOperationException(
                              "AxisMarksComputer does not support remove()");
                }

            };

        }

    }


    // --- Decimal computer ----------------------------------------------------

    public static class VerticalBytesComputer extends AxisMarksComputer {

        private final XYItem item;
        private final XYItemPainter painter;
        private final ChartContext context;
        private final int minMarksDistance;

        private final boolean reverse;


        public VerticalBytesComputer(XYItem item, XYItemPainter painter,
                                     ChartContext context, int minMarksDistance) {
            this.item = item;
            this.painter = painter;
            this.context = context;
            this.minMarksDistance = minMarksDistance;

            reverse = context.isBottomBased();
        }

        public Iterator<Mark> marksIterator(int start, int end) {
            double scale = painter.getItemValueScale(item, context);
            long[] units = getBytesUnits(scale, minMarksDistance);
            final long step = units[0];
            final int radix = (int)units[1];
            final long dataStart = ((long)painter.getItemValue(start, item,
                                          context) / step) * step;
            final long dataEnd = ((long)painter.getItemValue(end, item,
                                          context) / step) * step;
            final long iterCount = Math.abs(dataEnd - dataStart) / step + 2;
            final long[] iterIndex = new long[] { 0 };


            return new Iterator<Mark>() {

                public boolean hasNext() {
                    return iterIndex[0] < iterCount;
                }

                public Mark next() {
                    long value = reverse ? dataStart - iterIndex[0] * step :
                                           dataStart + iterIndex[0] * step;

                    iterIndex[0]++;
                    int position = ChartContext.getCheckedIntValue(
                                        painter.getItemView(value, item, context));
                    return new BytesMark(value, position, radix);
                }

                public void remove() {
                    throw new UnsupportedOperationException(
                              "AxisMarksComputer does not support remove()");
                }

            };

        }

    }


    // --- Percent computer ----------------------------------------------------


    // --- General support -----------------------------------------------------

    private static final long[] decimalUnitsGrid = new long[] { 1, 2, 5 };

    private static final long[] bytesUnitsGrid = new long[] { 1, 2, 5, 10, 25, 50, 100, 250, 500 };

    private static final long[] timeUnitsGrid = new long[] {
        1 /*1*/, 2 /*2*/, 5 /*5*/, 10 /*10*/, 20 /*20*/, 50 /*50*/, 100 /*100*/, 250 /*250*/, 500 /*500*/,  // milliseconds
        1000 /*1*/, 2000 /*2*/, 5000 /*5*/, 10000 /*10*/, 15000 /*15*/, 30000 /*30*/,                       // seconds
        60000 /*1*/, 120000 /*2*/, 300000 /*5*/, 600000 /*10*/, 900000 /*15*/, 1800000 /*30*/,              // minutes
        3600000 /*1*/, 7200000 /*2*/, 10800000 /*3*/, 21600000 /*6*/, 43200000 /*12*/,                      // hours
        86400000 /*1*/, 172800000 /*2*/,                                                                    // days
        604800000 /*1*/, 1209600000 /*2*/,                                                                  // weeks
        2628000000l /*1*/, 5256000000l /*2*/, 7884000000l /*3*/, 15768000000l /*6*/,                        // months (NOTE: not exactly!!!)
        31536000000l /*1*/, 63072000000l /*2*/, 157680000000l /*5*/, 315360000000l /*10*/                   // years (NOTE: not exactly!!!)
    };


    public static long getDecimalUnits(double scale, int minDistance) {
        if (scale == 0) return decimalUnitsGrid[0];

        long decimalFactor = 1;

        while (true) {
            for (int i = 0; i < decimalUnitsGrid.length; i++)
                if ((decimalUnitsGrid[i] * scale * decimalFactor) >= minDistance)
                    return decimalUnitsGrid[i] * decimalFactor;

            decimalFactor *= 10;
        }
    }

    public static long[] getBytesUnits(double scale, int minDistance) {
        if (scale == 0) return new long[] { bytesUnitsGrid[0], 1 };

        long bytesFactor = 1;
        long bytesRadix  = 0;

        while (true) {
            for (int i = 0; i < bytesUnitsGrid.length; i++)
                if ((bytesUnitsGrid[i] * scale * bytesFactor) >= minDistance)
                    return new long[] { bytesUnitsGrid[i] * bytesFactor, bytesRadix };

            bytesFactor *= 1024;
            bytesRadix  += 1;
        }
    }

    public static long getTimeUnits(double scale, int minDistance) {

        if (scale == 0) return timeUnitsGrid[0];

        for (int i = 0; i < timeUnitsGrid.length; i++)
            if (timeUnitsGrid[i] * scale >= minDistance)
                return timeUnitsGrid[i];
        return timeUnitsGrid[timeUnitsGrid.length - 1];
    }

//    public static long getTimeUnits(ChartContext context, boolean horizontal,
//                                    int minDistance) {
//
//        for (int i = 0; i < timeUnitsGrid.length; i++) {
//            long distance = horizontal ?
//                                (long)context.getViewWidth(timeUnitsGrid[i]) :
//                                (long)context.getViewHeight(timeUnitsGrid[i]);
//            if (distance >= minDistance) return timeUnitsGrid[i];
//        }
//
//        return timeUnitsGrid[timeUnitsGrid.length - 1];
//    }


//    private static class PercentComputer extends AxisMarksComputer {
//
//        private boolean horizontal;
//        private boolean reverse;
//        private ChartContext chartContext;
//
//
//        public PercentComputer(ChartContext chartContext, int orientation) {
//            this.chartContext = chartContext;
//            horizontal = orientation == SwingConstants.HORIZONTAL;
//            reverse = horizontal ? chartContext.isRightBased() :
//                                   chartContext.isBottomBased();
//        }
//
//
//        public Iterator<Mark> marksIterator(int start, int end) {
//            final long basis = horizontal ? chartContext.getViewWidth() :
//                                            chartContext.getViewHeight();
//            final long startl = (long)(start / 10) * 10;
//            final long endl = (long)(end / 10) * 10;
//System.err.println(">>> basis: " + basis);
//System.err.println(">>> startl: " + startl);
//System.err.println(">>> endl: " + endl);
////            final long dataStart = horizontal ?
////                                   (chartContext.getViewWidth() / 10) * 10 :
////                                   (chartContext.getDataY(start) / step) * step;
////            final long dataEnd = horizontal ?
////                                   (chartContext.getDataX(end) / step) * step :
////                                   (chartContext.getDataY(end) / step) * step;
//            final long iterCount = Math.abs(endl - startl) / 10 + 2;
//            final long[] iterIndex = new long[] { 0 };
//
//
//            return new Iterator<Mark>() {
//
//                public boolean hasNext() {
//                    return iterIndex[0] < iterCount;
//                }
//
//                public Mark next() {
//                    long value = reverse ? endl - iterIndex[0] * 10 :
//                                           startl + iterIndex[0] * 10;
////                    System.err.println(">>> Basis: " + basis + ", value: " + value);
//                    iterIndex[0]++;
//                    return new Mark(value);
//                }
//
//                public void remove() {
//                    throw new UnsupportedOperationException(
//                              "AxisMarksComputer does not support remove()");
//                }
//
//            };
//        }
//
//    }


    public static class Mark {

        private final long value;
        private final int position;


        public Mark(long value, int position) {
            this.value = value;
            this.position = position;
        }

        public long getValue() { return value; }

        public int getPosition() { return position; }

    }

    public static class BytesMark extends Mark {

        private final int radix;


        public BytesMark(long value, int position, int radix) {
            super(value, position);
            this.radix = radix;
        }

        public int getRadix() { return radix; }

    }

}
