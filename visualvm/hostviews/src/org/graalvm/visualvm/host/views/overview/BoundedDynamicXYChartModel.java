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

package org.graalvm.visualvm.host.views.overview;

import java.awt.Color;
//import org.graalvm.visualvm.lib.ui.charts.DynamicSynchronousXYChartModel;


/**
 *
 * @author Jiri Sedlacek
 */
public class BoundedDynamicXYChartModel /*extends DynamicSynchronousXYChartModel*/ {
    
//    private int maxItemsCount;
//
//
//    public BoundedDynamicXYChartModel(int maxItemsCount) {
//        this.maxItemsCount = maxItemsCount;
//    }
//
//    public void setupModel(String[] seriesNames, Color[] seriesColors) {
//        this.seriesNames = seriesNames;
//        this.seriesColors = seriesColors;
//
//        if (seriesNames.length != seriesColors.length) {
//            seriesCount = 0;
//            throw new RuntimeException("Counts of series names and series colors don't match."); // NOI18N
//        } else {
//            seriesCount = seriesNames.length;
//        }
//
//        itemCount = 0;
//
//        xValues = new long[maxItemsCount];
//        yValues = new long[maxItemsCount][];
//
//        minXValue = 0;
//        maxXValue = 0;
//
//        minYValues = new long[seriesCount];
//        maxYValues = new long[seriesCount];
//    }
//
//    public void addItemValues(long xValue, long[] yValues) {
//        // first data arrived, initialize min/max values
//        if (itemCount == 0) {
//            for (int i = 0; i < seriesCount; i++) {
//                minYValues[i] = yValues[i];
//                maxYValues[i] = yValues[i];
//            }
//        } else {
//            // check "timeline" consistency
//            if (xValues[itemCount - 1] >= xValue) {
//                throw new RuntimeException("New x-value not greater than previous x-value."); // NOI18N
//            }
//
//            // check min/max for y values
//            for (int i = 0; i < seriesCount; i++) {
//                minYValues[i] = Math.min(minYValues[i], yValues[i]);
//                maxYValues[i] = Math.max(maxYValues[i], yValues[i]);
//            }
//        }
//
//        if (itemCount == maxItemsCount) {
//            System.arraycopy(xValues, 1, xValues, 0, xValues.length - 1);
//            System.arraycopy(this.yValues, 1, this.yValues, 0, this.yValues.length - 1);
//
//            // add new x value
//            xValues[itemCount - 1] = xValue;
//
//            // add new y values
//            this.yValues[itemCount - 1] = yValues;
//        } else {
//            // add new x value
//            xValues[itemCount] = xValue;
//
//            // add new y values
//            this.yValues[itemCount] = yValues;
//        }
//
//        // increment item counter
//        if (itemCount < maxItemsCount) itemCount++;
//
//        minXValue = xValues[0];
//        maxXValue = xValue; // new values are always greater
//
//        fireChartDataChanged();
//    }
}
