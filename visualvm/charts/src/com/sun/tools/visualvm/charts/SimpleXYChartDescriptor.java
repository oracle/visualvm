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

package com.sun.tools.visualvm.charts;

import com.sun.tools.visualvm.charts.xy.SimpleXYChartUtils;
import java.awt.Color;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Descriptor to define a simple XY chart.
 *
 * @author Jiri Sedlacek
 */
public final class SimpleXYChartDescriptor {

    // --- Predefined constructors ---------------------------------------------

    public static SimpleXYChartDescriptor decimal(long initialYMargin,
                                                  boolean hideableItems,
                                                  int valuesBuffer) {

        return decimal(initialYMargin, 1d, hideableItems, valuesBuffer);
    }

    public static SimpleXYChartDescriptor decimal(long initialYMargin,
                                                  double chartFactor,
                                                  boolean hideableItems,
                                                  int valuesBuffer) {

        return decimal(0, SimpleXYChartSupport.MAX_UNDEFINED, initialYMargin,
                       chartFactor, hideableItems, valuesBuffer);
    }
    
    public static SimpleXYChartDescriptor decimal(long minValue,
                                                  long maxValue,
                                                  long initialYMargin,
                                                  double chartFactor,
                                                  boolean hideableItems,
                                                  int valuesBuffer) {

        return decimal(minValue, maxValue, initialYMargin, chartFactor,
                       null, hideableItems, valuesBuffer);
    }

    public static SimpleXYChartDescriptor decimal(long minValue,
                                                  long maxValue,
                                                  long initialYMargin,
                                                  double chartFactor,
                                                  NumberFormat customFormat,
                                                  boolean hideableItems,
                                                  int valuesBuffer) {

        return new SimpleXYChartDescriptor(SimpleXYChartUtils.TYPE_DECIMAL,
                                           minValue, maxValue,
                                           initialYMargin, hideableItems,
                                           chartFactor, customFormat, valuesBuffer);
    }


    public static SimpleXYChartDescriptor bytes(long initialYMargin,
                                                boolean hideableItems,
                                                int valuesBuffer) {

        return bytes(0, SimpleXYChartSupport.MAX_UNDEFINED, initialYMargin,
                     hideableItems, valuesBuffer);
    }

    public static SimpleXYChartDescriptor bytes(long minValue,
                                                long maxValue,
                                                long initialYMargin,
                                                boolean hideableItems,
                                                int valuesBuffer) {

        return new SimpleXYChartDescriptor(SimpleXYChartUtils.TYPE_BYTES,
                                           minValue, maxValue,
                                           initialYMargin, hideableItems,
                                           1d, null, valuesBuffer);
    }


    public static SimpleXYChartDescriptor percent(boolean hideableItems,
                                                  int valuesBuffer) {

        return percent(hideableItems, 1d, valuesBuffer);
    }

    public static SimpleXYChartDescriptor percent(boolean hideableItems,
                                                  double chartFactor,
                                                  int valuesBuffer) {

        return percent(0, 100, 100, hideableItems, chartFactor, valuesBuffer);
    }

    public static SimpleXYChartDescriptor percent(long minValue,
                                                  long maxValue,
                                                  long initialYMargin,
                                                  boolean hideableItems,
                                                  double chartFactor,
                                                  int valuesBuffer) {

        long max = (long)Math.ceil(maxValue / chartFactor);
        long init = (long)Math.ceil(initialYMargin / chartFactor);
        return new SimpleXYChartDescriptor(SimpleXYChartUtils.TYPE_PERCENT,
                                           minValue, max,
                                           init, hideableItems,
                                           chartFactor, null, valuesBuffer);
    }


    // --- Items definition ----------------------------------------------------

    public void addLineItems(String... itemNames) {
        for (String itemName : itemNames) {
            Color color = getNextItemColor();
            addItem(itemName, color, 2f, color, null, null);
        }
    }

    public void addFillItems(String... itemNames) {
        for (String itemName : itemNames) {
            Color[] gradients = getNextItemGradient();
            addItem(itemName, gradients[0], 2f, null, gradients[0], gradients[1]);
        }
    }

    public void addLineFillItems(String... itemNames) {
        for (String itemName : itemNames) {
            Color color = getNextItemColor();
            Color[] gradients = getNextItemGradient();
            addItem(itemName, color, 2f, color, gradients[0], gradients[1]);
        }
    }


    public void addItem(String itemName, Color itemColor, float lineWidth,
                        Color lineColor, Color fillColor1, Color fillColor2) {
        itemNames.add(itemName);
        itemColors.add(itemColor);
        lineWidths.add(lineWidth);
        lineColors.add(lineColor);
        fillColors1.add(fillColor1);
        fillColors2.add(fillColor2);
    }


    private Color getNextItemColor() {
        int newItemIndex = itemNames.size();
        int colorsCount  = ColorFactory.getPredefinedColorsCount();
        Color color = null;

        if (newItemIndex >= colorsCount) {
            color = ColorFactory.getPredefinedColor(newItemIndex % colorsCount);
            int darkerFactor = newItemIndex / colorsCount;
            while (darkerFactor-- > 0) color = color.darker();
        } else {
            color = ColorFactory.getPredefinedColor(newItemIndex);
        }

        return color;
    }

    private Color[] getNextItemGradient() {
        int newItemIndex = itemNames.size();
        int colorsCount  = ColorFactory.getPredefinedColorsCount();
        Color[] color = null;

        if (newItemIndex >= colorsCount) {
            color = ColorFactory.getPredefinedGradient(newItemIndex % colorsCount);
            int darkerFactor = newItemIndex / colorsCount;
            while (darkerFactor-- > 0) {
                color[0] = color[0].darker();
                color[1] = color[1].darker();
            }
        } else {
            color = ColorFactory.getPredefinedGradient(newItemIndex);
        }

        return color;
    }

    
    // --- Details definition --------------------------------------------------

    public void setDetailsItems(String[] detailNames) {
        this.detailNames = detailNames != null ? detailNames.clone() : null;
    }


    // --- Axes description ----------------------------------------------------

    public void setChartTitle(String chartTitle) {
        this.chartTitle = chartTitle;
    }

    public void setXAxisDescription(String xAxisDescription) {
        this.xAxisDescription = xAxisDescription;
    }

    public void setYAxisDescription(String yAxisDescription) {
        this.yAxisDescription = yAxisDescription;
    }


    // --- Internal interface --------------------------------------------------

    int getChartType() { return chartType; }

    long getMinValue() { return minValue; }

    long getMaxValue() { return maxValue; }

    long getInitialYMargin() { return initialYMargin; }

    boolean areItemsHideable() { return hideableItems; }

    double getChartFactor() { return chartFactor; }
    
    NumberFormat getCustomFormat() { return customFormat; }

    int getValuesBuffer() { return valuesBuffer; }


    String[] getItemNames() {
        return itemNames.toArray(new String[itemNames.size()]);
    }

    Color[] getItemColors() {
        return itemColors.toArray(new Color[itemColors.size()]);
    }

    float[] getLineWidths() {
        if (lineWidths.isEmpty()) return null;
        float[] floats = new float[lineWidths.size()];
        for (int i = 0; i < floats.length; i++) floats[i] = lineWidths.get(i);
        return floats;
    }

    Color[] getLineColors() {
        return lineColors.isEmpty() ? null :
               lineColors.toArray(new Color[lineColors.size()]);
    }

    Color[] getFillColors1() {
        return fillColors1.isEmpty() ? null :
               fillColors1.toArray(new Color[fillColors1.size()]);
    }

    Color[] getFillColors2() {
        return fillColors2.isEmpty() ? null :
               fillColors2.toArray(new Color[fillColors2.size()]);
    }

    String[] getDetailsItems() {
        return detailNames;
    }

    String getChartTitle() {
        return chartTitle;
    }

    String getXAxisDescription() {
        return xAxisDescription;
    }

    String getYAxisDescription() {
        return yAxisDescription;
    }


    // --- Private implementation ----------------------------------------------

    private final int          chartType;
    private final long         minValue;
    private final long         maxValue;
    private final long         initialYMargin;
    private final boolean      hideableItems;
    private final double       chartFactor;
    private final NumberFormat customFormat;
    private final int          valuesBuffer;

    private final List<String> itemNames  = new ArrayList();
    private final List<Color>  itemColors = new ArrayList();
    private final List<Float>  lineWidths = new ArrayList();
    private final List<Color>  lineColors = new ArrayList();
    private final List<Color>  fillColors1 = new ArrayList();
    private final List<Color>  fillColors2 = new ArrayList();
    private       String[]     detailNames;

    private       String       chartTitle;
    private       String       xAxisDescription;
    private       String       yAxisDescription;


    private SimpleXYChartDescriptor(int chartType,
                                    long minValue,
                                    long maxValue,
                                    long initialYMargin,
                                    boolean hideableItems,
                                    double chartFactor,
                                    NumberFormat customFormat,
                                    int valuesBuffer) {

        this.chartType = chartType;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.initialYMargin = initialYMargin;
        this.hideableItems = hideableItems;
        this.chartFactor = chartFactor;
        this.customFormat = customFormat;
        this.valuesBuffer = valuesBuffer;
    }

}
