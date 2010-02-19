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

import java.text.NumberFormat;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class ItemValueFormatter {

    public static final int FORMAT_TOOLTIP = 0;
    public static final int FORMAT_UNITS = 1;
    public static final int FORMAT_DETAILS = 2;

    public static final ItemValueFormatter SIMPLE = new ItemValueFormatter() {
        public String formatValue(long value, int format) {
            return Long.toString(value);
        }
        public String getUnits(int format) {
            return null;
        }
    };

    public static final ItemValueFormatter DEFAULT_DECIMAL = new Decimal();
    public static final ItemValueFormatter DEFAULT_BYTES = new Bytes();
    public static final ItemValueFormatter DEFAULT_PERCENT = new Percent();


    public abstract String formatValue(long value, int format);
    
    public abstract String getUnits(int format);

    
    public static final class Decimal extends ItemValueFormatter {
        
        private static final NumberFormat FORMAT = NumberFormat.getInstance();

        private final String units;


        public Decimal() {
            this(null);
        }

        public Decimal(String units) {
            this.units = units;
        }


        public String formatValue(long value, int format) {
            return FORMAT.format(value);
        }
        
        public String getUnits(int format) {
            return units;
        }
        
    }


    public static final class Bytes extends ItemValueFormatter {

        private static final NumberFormat FORMAT = NumberFormat.getInstance();


        public String formatValue(long value, int format) {
            switch (format) {
                case FORMAT_TOOLTIP:
                    return FORMAT.format(value);
                case FORMAT_UNITS:
                    String est = value == 0 ? "" : "~";
                    return est + FORMAT.format(Math.round(value / 1024 / 1024));
                default:
                    return null;
            }
        }
        
        public String getUnits(int format) {
            switch (format) {
                case FORMAT_TOOLTIP:
                    return "B";
                case FORMAT_UNITS:
                    return "MB";
                default:
                    return null;
            }
        }
        
    }


    public static final class Percent extends ItemValueFormatter {

        private static final NumberFormat FORMAT;

        static {
            FORMAT = NumberFormat.getPercentInstance();
            FORMAT.setMinimumFractionDigits(1);
            FORMAT.setMaximumIntegerDigits(3);
        }
        
        private double factor;


        public Percent() {
            this(3);
        }

        public Percent(int decexp) {
            factor = Math.pow(10, decexp);
        }


        public String formatValue(long value, int format) {
            return FORMAT.format(value / factor);
        }
        public String getUnits(int format) {
            return null; // '%' provided by NumberFormat.getPercentInstance()
        }

    }

}
