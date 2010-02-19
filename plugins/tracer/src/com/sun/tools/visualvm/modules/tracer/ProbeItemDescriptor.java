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

import com.sun.tools.visualvm.modules.tracer.impl.timeline.items.FillItemDescriptor;
import com.sun.tools.visualvm.modules.tracer.impl.timeline.items.LineFillItemDescriptor;
import com.sun.tools.visualvm.modules.tracer.impl.timeline.items.LineItemDescriptor;
import com.sun.tools.visualvm.modules.tracer.impl.timeline.items.ValueItemDescriptor;
import java.awt.Color;

/**
 * ProbeItemDescriptor describes TracerProbe items appearance in the UI.
 * Use the predefined static methods to create instances of ProbeItemDescriptor.
 *
 * @author Jiri Sedlacek
 */
public abstract class ProbeItemDescriptor {

    // --- Public predefined constants -----------------------------------------

    public static final long MIN_VALUE_UNDEFINED = Long.MAX_VALUE;
    public static final long MAX_VALUE_UNDEFINED = Long.MIN_VALUE;

    public static final Color DEFAULT_COLOR = new Color(0, 0, 0); // use == to identify this instance!

    public static final float DEFAULT_LINE_WIDTH = -1.0F;


    // --- Private instance variables ------------------------------------------

    private final String name;
    private final String description;


    // --- Protected constructor -----------------------------------------------

    protected ProbeItemDescriptor(String name, String description) {
        if (name == null)
            throw new IllegalArgumentException("name cannot be null"); // NOI18N

        // Custom ProbeItemDescriptor subclasses are currently not supported.
        // May be supported in future versions together with custom Painters.
        if (!(this instanceof ValueItemDescriptor))
            throw new UnsupportedOperationException("Custom descriptor not supported. Use the predefined descriptors."); // NOI18N

        this.name = name;
        this.description = description;
    }


    // --- Common implementation -----------------------------------------------

    public final String getName() { return name; }

    public final String getDescription() { return description; }


    // === Public factory methods ==============================================

    // --- LineItem ------------------------------------------------------------

    public static ProbeItemDescriptor lineItem(String name, String description) {
        return new LineItemDescriptor(name, description);
    }
    
    public static ProbeItemDescriptor lineItem(String name, String description,
                                               ItemValueFormatter formatter) {

        return new LineItemDescriptor(name, description, formatter);
    }

    public static ProbeItemDescriptor lineItem(String name, String description,
                                               ItemValueFormatter formatter,
                                               long minValue, long maxValue) {

        return new LineItemDescriptor(name, description, formatter, minValue,
                                      maxValue);
    }

    public static ProbeItemDescriptor lineItem(String name, String description,
                                               ItemValueFormatter formatter,
                                               double dataFactor, long minValue,
                                               long maxValue) {

        return new LineItemDescriptor(name, description, formatter, dataFactor,
                                      minValue, maxValue);
    }

    public static ProbeItemDescriptor lineItem(String name, String description,
                                               ItemValueFormatter formatter,
                                               double dataFactor, long minValue,
                                               long maxValue, float lineWidth,
                                               Color lineColor) {

        return new LineItemDescriptor(name, description, formatter, dataFactor,
                                      minValue, maxValue, lineWidth, lineColor);
    }


    // --- FillItem ------------------------------------------------------------

    public static ProbeItemDescriptor fillItem(String name, String description) {
        return new FillItemDescriptor(name, description);
    }

    public static ProbeItemDescriptor fillItem(String name, String description,
                                               ItemValueFormatter formatter) {

        return new FillItemDescriptor(name, description, formatter);
    }

    public static ProbeItemDescriptor fillItem(String name, String description,
                                               ItemValueFormatter formatter,
                                               long minValue, long maxValue) {

        return new FillItemDescriptor(name, description, formatter, minValue,
                                      maxValue);
    }

    public static ProbeItemDescriptor fillItem(String name, String description,
                                               ItemValueFormatter formatter,
                                               double dataFactor, long minValue,
                                               long maxValue) {

        return new FillItemDescriptor(name, description, formatter, dataFactor,
                                      minValue, maxValue);
    }

    public static ProbeItemDescriptor fillItem(String name, String description,
                                               ItemValueFormatter formatter,
                                               double dataFactor, long minValue,
                                               long maxValue, Color fillColor1,
                                               Color fillColor2) {

        return new FillItemDescriptor(name, description, formatter, dataFactor,
                                      minValue, maxValue, fillColor1, fillColor2);
    }


    // --- LineFillItem --------------------------------------------------------

    public static ProbeItemDescriptor lineFillItem(String name, String description) {
        return new LineFillItemDescriptor(name, description);
    }

    public static ProbeItemDescriptor lineFillItem(String name, String description,
                                                   ItemValueFormatter formatter) {

        return new LineFillItemDescriptor(name, description, formatter);
    }

    public static ProbeItemDescriptor lineFillItem(String name, String description,
                                                   ItemValueFormatter formatter,
                                                   long minValue, long maxValue) {

        return new LineFillItemDescriptor(name, description, formatter, minValue,
                                          maxValue);
    }

    public static ProbeItemDescriptor lineFillItem(String name, String description,
                                                   ItemValueFormatter formatter,
                                                   double dataFactor, long minValue,
                                                   long maxValue) {

        return new LineFillItemDescriptor(name, description, formatter, dataFactor,
                                          minValue, maxValue);
    }

    public static ProbeItemDescriptor lineFillItem(String name, String description,
                                                   ItemValueFormatter formatter,
                                                   double dataFactor, long minValue,
                                                   long maxValue, float lineWidth,
                                                   Color lineColor, Color fillColor1,
                                                   Color fillColor2) {

        return new LineFillItemDescriptor(name, description, formatter, dataFactor,
                                          minValue, maxValue, lineWidth, lineColor,
                                          fillColor1, fillColor2);
    }

}
