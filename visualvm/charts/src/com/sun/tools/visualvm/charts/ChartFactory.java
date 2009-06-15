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

package com.sun.tools.visualvm.charts;

import com.sun.tools.visualvm.charts.xy.SimpleXYChartUtils;
import java.awt.Color;

/**
 * Factory class to create custom charts.
 *
 * @author Jiri Sedlacek
 */
public final class ChartFactory {

    public static SimpleXYChartSupport createSimpleDecimalXYChart(
                                                            long initialYMargin,
                                                            String[] itemNames,
                                                            Color[] itemColors,
                                                            float[] lineWidths,
                                                            Color[] lineColors,
                                                            Color[] fillColors1,
                                                            Color[] fillColors2,
                                                            long minValue,
                                                            long maxValue,
                                                            boolean hideItems,
                                                            int valuesBuffer,
                                                            String[] detailsItems) {

        return new SimpleXYChartSupport(SimpleXYChartUtils.TYPE_DECIMAL,
                                        initialYMargin, itemNames, itemColors,
                                        lineWidths, lineColors, fillColors1,
                                        fillColors2, minValue, maxValue,
                                        hideItems, valuesBuffer, detailsItems);
    }

    public static SimpleXYChartSupport createSimpleBytesXYChart(
                                                            long initialYMargin,
                                                            String[] itemNames,
                                                            Color[] itemColors,
                                                            float[] lineWidths,
                                                            Color[] lineColors,
                                                            Color[] fillColors1,
                                                            Color[] fillColors2,
                                                            long minValue,
                                                            long maxValue,
                                                            boolean hideItems,
                                                            int valuesBuffer,
                                                            String[] detailsItems) {

        return new SimpleXYChartSupport(SimpleXYChartUtils.TYPE_BYTES,
                                        initialYMargin, itemNames, itemColors,
                                        lineWidths, lineColors, fillColors1,
                                        fillColors2, minValue, maxValue,
                                        hideItems, valuesBuffer, detailsItems);
    }

    public static SimpleXYChartSupport createSimplePercentXYChart(
                                                            String[] itemNames,
                                                            Color[] itemColors,
                                                            float[] lineWidths,
                                                            Color[] lineColors,
                                                            Color[] fillColors1,
                                                            Color[] fillColors2,
                                                            boolean hideItems,
                                                            int valuesBuffer,
                                                            String[] detailsItems) {
        
        return new SimpleXYChartSupport(SimpleXYChartUtils.TYPE_PERCENT,
                                        1000, itemNames, itemColors,
                                        lineWidths, lineColors, fillColors1,
                                        fillColors2, 0, 1000,
                                        hideItems, valuesBuffer, detailsItems);
    }

}
