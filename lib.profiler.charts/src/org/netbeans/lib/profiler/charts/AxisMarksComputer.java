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

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class AxisMarksComputer {

    public abstract Iterator<Mark> marksIterator(int start, int end);
    
    
    public static AxisMarksComputer simpleComputer(int step,
                                                   ChartContext chartContext,
                                                   int orientation) {
        return new SimpleComputer(step, chartContext, orientation);
    }

//    public static AxisMarksComputer percentComputer(ChartContext chartContext,
//                                                   int orientation) {
//        return new PercentComputer(chartContext, orientation);
//    }


    private static class SimpleComputer extends AxisMarksComputer {

        private int step;
        private boolean horizontal;
        private boolean reverse;
        private ChartContext chartContext;


        public SimpleComputer(int step, ChartContext chartContext, int orientation) {
            this.step = step;
            this.chartContext = chartContext;
            horizontal = orientation == SwingConstants.HORIZONTAL;
            reverse = horizontal ? chartContext.isRightBased() :
                                   chartContext.isBottomBased();
        }


        public Iterator<Mark> marksIterator(int start, int end) {
            final long dataStart = horizontal ?
                                   ((long)chartContext.getDataX(start) / step) * step :
                                   ((long)chartContext.getDataY(start) / step) * step;
            final long dataEnd = horizontal ?
                                   ((long)chartContext.getDataX(end) / step) * step :
                                   ((long)chartContext.getDataY(end) / step) * step;
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
                    return new Mark(value);
                }

                public void remove() {
                    throw new UnsupportedOperationException(
                              "AxisMarksComputer does not support remove()");
                }

            };
        }

    }


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


        public Mark(long value) { this.value = value; }

        public long getValue() { return value; }

    }

}
