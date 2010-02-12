/*
 *  Copyright 2007-2010 Sun Microsystems, Inc.  All Rights Reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Sun designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Sun in the LICENSE file that accompanied this code.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *  Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 *  CA 95054 USA or visit www.sun.com if you need additional information or
 *  have any questions.
 */

package com.sun.tools.visualvm.modules.tracer;

import java.awt.Color;

/**
 * ProbeItemDescriptor describes TracerProbe items appearance in the UI.
 * Use the predefined subclasses to create instances of ProbeItemDescriptor.
 *
 * @author Jiri Sedlacek
 */
public abstract class ProbeItemDescriptor {

    public static final Color DEFAULT_COLOR = new Color(0, 0, 0); // use == to identify this instance!

    private final String name;
    private final String description;


    private ProbeItemDescriptor(String name, String description) {
        this.name = name;
        this.description = description;
    }


    public final String getName() { return name; }

    public final String getDescription() { return description; }


    public static abstract class ValueItem extends ProbeItemDescriptor {

        public static final long MIN_VALUE_UNDEFINED = Long.MAX_VALUE;
        public static final long MAX_VALUE_UNDEFINED = Long.MIN_VALUE;

        private final long minValue;
        private final long maxValue;
        private final double dataFactor;
        private final double viewFactor;
        private final String unitsString;


        private ValueItem(String name, String description, long minValue,
                          long maxValue, double dataFactor, double viewFactor,
                          String unitsString) {
            super(name, description);
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.dataFactor = dataFactor;
            this.viewFactor = viewFactor;
            this.unitsString = unitsString;
        }


        public final long getMinValue() { return minValue; }

        public final long getMaxValue() { return maxValue; }

        public final double getDataFactor() { return dataFactor; }

        public final double getViewFactor() { return viewFactor; }

        public final String getUnitsString() { return unitsString; }

    }


    public static abstract class XYItem extends ValueItem {

        public static final float DEFAULT_LINE_WIDTH = -1f;

        private final float lineWidth;
        private final Color lineColor;
        private final Color fillColor1;
        private final Color fillColor2;


        private XYItem(String name, String description, long minValue,
                       long maxValue, double dataFactor, double viewFactor,
                       String unitsString, float lineWidth, Color lineColor,
                       Color fillColor1, Color fillColor2) {

            super(name, description, minValue, maxValue, dataFactor, viewFactor,
                  unitsString);
            this.lineWidth = lineWidth;
            this.lineColor = lineColor;
            this.fillColor1 = fillColor1;
            this.fillColor2 = fillColor2;
        }


        public final float getLineWidth() { return lineWidth; }

        public final Color getLineColor() { return lineColor; }

        public final Color getFillColor1() { return fillColor1; }

        public final Color getFillColor2() { return fillColor2; }

    }


    public static final class LineItem extends XYItem {

        public LineItem(String name, String description) {
            this(name, description, null);
        }

        public LineItem(String name, String description, String unitsString) {
            this(name, description, MIN_VALUE_UNDEFINED, MAX_VALUE_UNDEFINED,
                 unitsString);
        }
        
        public LineItem(String name, String description, long minValue,
                        long maxValue, String unitsString) {
            this(name, description, minValue, maxValue, 1d, 1d, unitsString);
        }

        public LineItem(String name, String description, long minValue,
                        long maxValue, double dataFactor, double viewFactor,
                        String unitsString) {
            this(name, description, minValue, maxValue, dataFactor, viewFactor,
                 unitsString, DEFAULT_LINE_WIDTH, DEFAULT_COLOR);
        }

        public LineItem(String name, String description, long minValue,
                        long maxValue, double dataFactor, double viewFactor,
                        String unitsString, float lineWidth, Color lineColor) {
            super(name, description, minValue, maxValue, dataFactor, viewFactor,
                  unitsString, lineWidth, lineColor, null, null);
        }

    }


    public static final class FillItem extends XYItem {

        public FillItem(String name, String description) {
            this(name, description, null);
        }

        public FillItem(String name, String description, String unitsString) {
            this(name, description, MIN_VALUE_UNDEFINED, MAX_VALUE_UNDEFINED,
                 unitsString);
        }

        public FillItem(String name, String description, long minValue,
                        long maxValue, String unitsString) {
            this(name, description, minValue, maxValue, 1d, 1d, unitsString);
        }

        public FillItem(String name, String description, long minValue,
                        long maxValue, double dataFactor, double viewFactor,
                        String unitsString) {
            this(name, description, minValue, maxValue, dataFactor, viewFactor,
                 unitsString, DEFAULT_COLOR, DEFAULT_COLOR);
        }

        public FillItem(String name, String description, long minValue,
                        long maxValue, double dataFactor, double viewFactor,
                        String unitsString, Color fillColor1, Color fillColor2) {
            super(name, description, minValue, maxValue, dataFactor, viewFactor,
                  unitsString, 0f, null, fillColor1, fillColor2);
        }

    }


    public static final class LineFillItem extends XYItem {

        public LineFillItem(String name, String description) {
            this(name, description, null);
        }

        public LineFillItem(String name, String description, String unitsString) {
            this(name, description, MIN_VALUE_UNDEFINED, MAX_VALUE_UNDEFINED,
                 unitsString);
        }

        public LineFillItem(String name, String description, long minValue,
                            long maxValue, String unitsString) {
            this(name, description, minValue, maxValue, 1d, 1d, unitsString);
        }

        public LineFillItem(String name, String description, long minValue,
                            long maxValue, double dataFactor, double viewFactor,
                            String unitsString) {
            this(name, description, minValue, maxValue, dataFactor, viewFactor,
                 unitsString, DEFAULT_LINE_WIDTH, DEFAULT_COLOR, DEFAULT_COLOR,
                 DEFAULT_COLOR);
        }

        public LineFillItem(String name, String description, long minValue,
                            long maxValue, double dataFactor, double viewFactor,
                            String unitsString, float lineWidth, Color lineColor,
                            Color fillColor1, Color fillColor2) {
            super(name, description, minValue, maxValue, dataFactor, viewFactor,
                  unitsString, lineWidth, lineColor, fillColor1, fillColor2);
        }

    }


//    public abstract class BarItem extends ValueItem {
//
//    }

}
