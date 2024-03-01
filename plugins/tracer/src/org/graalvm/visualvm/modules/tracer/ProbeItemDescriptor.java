/*
 *  Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
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
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 */

package org.graalvm.visualvm.modules.tracer;

import org.graalvm.visualvm.modules.tracer.impl.timeline.items.ContinuousXYItemDescriptor;
import org.graalvm.visualvm.modules.tracer.impl.timeline.items.DiscreteXYItemDescriptor;
import org.graalvm.visualvm.modules.tracer.impl.timeline.items.ValueItemDescriptor;
import java.awt.Color;

/**
 * ProbeItemDescriptor describes a TracerProbe item appearance in the UI.
 * <p>
 *
 * Current version supports two general types of items: continuous and discrete.
 * Continuous items consist of non-rectangular polyline or polygon segments
 * connecting the values. The values define vertices of the segments.
 * <p>
 *
 * Discrete items consist of rectangular segments either connected together or
 * divided into bar segments. The values are located in the middle of the segments.
 * <p>
 *
 * Each descriptor requires a common set of mandatory options:
 * <ul>
 * <li><code>name</code>: item name</li>
 * <li><code>description</code>: item description, may be <code>null</code></li>
 * <li><code>formatter</code>: ItemValueFormatter instance which defines how the item values are presented in UI</li>
 * </ul>
 * <p>
 *
 * The other options which may be set are:
 * <ul>
 * <li><code>dataFactor</code>: a multiplication factor for item values, useful when displaying multiple items in one graph</li>
 * <li><code>minValue</code>: minimum (initial) item value, typically set for zero-based metrics (heap size)</li>
 * <li><code>maxValue</code>: maximum (initial) item value, may be used for the initial graph scale</li>
 * </ul>
 * <p>
 *
 * There's no need to define line width and/or line/fill colors, the framework
 * guarantees that each item in a graph will be displayed by a different color.
 * If needed, line width and/or line/fill colors may be customized by setting
 * these options:
 * <ul>
 * <li><code>lineWidth</code>: width of the line, default is <code>2f</code></li>
 * <li><code>lineColor</code>: color of the line, may be <code>null</code></li>
 * <li><code>fillColor</code>: color of the filled area, may be <code>null</code></li>
 * </ul>
 * <p>
 *
 * <b>Note:</b> Use the predefined static methods to create instances of ProbeItemDescriptor.
 * Custom instances of ProbeItemDescriptor are not supported and will cause a
 * <code>RuntimeException</code>.
 *
 * @author Jiri Sedlacek
 */
public abstract class ProbeItemDescriptor {

    // --- Public predefined constants -----------------------------------------

    /**
     * Minimum item value is undefined.
     */
    public static final long MIN_VALUE_UNDEFINED = Long.MAX_VALUE;
    /**
     * Maximum item value is undefined.
     */
    public static final long MAX_VALUE_UNDEFINED = Long.MIN_VALUE;
    /**
     * Value is undefined. For minimum/maximum value use MIN_VALUE_UNDEFINED or
     * MAX_VALUE_UNDEFINED.
     */
    public static final long VALUE_UNDEFINED = Long.MIN_VALUE - 1;

    /**
     * Default color.
     */
    public static final Color DEFAULT_COLOR = new Color(0, 0, 0); // use == to identify this instance!

    /**
     * Default line width.
     */
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

    /**
     * Returns name of the item.
     * @return name of the item
     */
    public final String getName() { return name; }

    /**
     * Returns description of the item.
     * @return description of the item
     */
    public final String getDescription() { return description; }


    // === Public factory methods ==============================================

    // --- Continuous items ----------------------------------------------------

    /**
     * Creates descriptor for a continuous item created by line segments.
     *
     * @param name item name
     * @param description item description or null
     * @param formatter item formatter
     * @return descriptor for a continuous item created by line segments
     */
    public static ProbeItemDescriptor continuousLineItem(String name, String description,
                                                         ItemValueFormatter formatter) {

        return continuousItem(name, description, formatter, 1d, 0, MAX_VALUE_UNDEFINED,
                              DEFAULT_LINE_WIDTH, DEFAULT_COLOR, null);
    }

    /**
     * Creates descriptor for a continuous item created by line segments with custom dataFactor ad min/max values.
     *
     * @param name item name
     * @param description item description or null
     * @param formatter item formatter
     * @param dataFactor multiplication factor
     * @param minValue minimum (initial) item value
     * @param maxValue maximum (initial) item value
     * @return descriptor for a continuous item created by line segments
     */
    public static ProbeItemDescriptor continuousLineItem(String name, String description,
                                                         ItemValueFormatter formatter,
                                                         double dataFactor,
                                                         long minValue, long maxValue) {

        return continuousItem(name, description, formatter, dataFactor, minValue,
                              maxValue, DEFAULT_LINE_WIDTH, DEFAULT_COLOR, null);
    }

    /**
     * Creates descriptor for a continuous item created by filled segments.
     *
     * @param name item name
     * @param description item description or null
     * @param formatter item formatter
     * @return descriptor for a continuous item created by filled segments
     */
    public static ProbeItemDescriptor continuousFillItem(String name, String description,
                                                         ItemValueFormatter formatter) {

        return continuousItem(name, description, formatter, 1d, 0, MAX_VALUE_UNDEFINED,
                              DEFAULT_LINE_WIDTH, null, DEFAULT_COLOR);
    }

    /**
     * Creates descriptor for a continuous item created by filled segments with custom dataFactor ad min/max values.
     *
     * @param name item name
     * @param description item description or null
     * @param formatter item formatter
     * @param dataFactor multiplication factor
     * @param minValue minimum (initial) item value
     * @param maxValue maximum (initial) item value
     * @return descriptor for a continuous item created by filled segments
     */
    public static ProbeItemDescriptor continuousFillItem(String name, String description,
                                                         ItemValueFormatter formatter,
                                                         double dataFactor,
                                                         long minValue, long maxValue) {

        return continuousItem(name, description, formatter, dataFactor, minValue,
                              maxValue, DEFAULT_LINE_WIDTH, null, DEFAULT_COLOR);
    }

    /**
     * Creates descriptor for a continuous item created by line and filled segments.
     *
     * @param name item name
     * @param description item description or null
     * @param formatter item formatter
     * @return descriptor for a continuous item created by line and filled segments
     */
    public static ProbeItemDescriptor continuousLineFillItem(String name, String description,
                                                             ItemValueFormatter formatter) {

        return continuousItem(name, description, formatter, 1d, 0, MAX_VALUE_UNDEFINED,
                              DEFAULT_LINE_WIDTH, DEFAULT_COLOR, DEFAULT_COLOR);
    }

    /**
     * Creates descriptor for a continuous item created by line and filled segments with custom dataFactor ad min/max values.
     *
     * @param name item name
     * @param description item description or null
     * @param formatter item formatter
     * @param dataFactor multiplication factor
     * @param minValue minimum (initial) item value
     * @param maxValue maximum (initial) item value
     * @return descriptor for a continuous item created by line and filled segments
     */
    public static ProbeItemDescriptor continuousLineFillItem(String name, String description,
                                                             ItemValueFormatter formatter,
                                                             double dataFactor,
                                                             long minValue, long maxValue) {

        return continuousItem(name, description, formatter, dataFactor, minValue,
                              maxValue, DEFAULT_LINE_WIDTH, DEFAULT_COLOR, DEFAULT_COLOR);
    }

    /**
     * Creates descriptor for a general continuous item with custom dataFactor ad min/max values, and custom line width and line/fill colors.
     *
     * @param name item name
     * @param description item description or null
     * @param formatter item formatter
     * @param dataFactor multiplication factor
     * @param minValue minimum (initial) item value
     * @param maxValue maximum (initial) item value
     * @param lineWidth line width
     * @param lineColor line color or null
     * @param fillColor fill color or null
     * @return descriptor for a general continuous item
     */
    public static ProbeItemDescriptor continuousItem(String name, String description,
                                                     ItemValueFormatter formatter,
                                                     double dataFactor, long minValue,
                                                     long maxValue, float lineWidth,
                                                     Color lineColor, Color fillColor) {
        
        if (lineColor == null && fillColor == null)
            throw new IllegalArgumentException("Either lineColor or fillColor must be defined"); // NOI18N

        return new ContinuousXYItemDescriptor(name, description, formatter, dataFactor,
                                              minValue, maxValue, lineWidth, lineColor,
                                              fillColor);
    }


    // --- Discrete items ------------------------------------------------------

    /**
     * Creates descriptor for a discrete item created by line segments representing the outline.
     *
     * @param name item name
     * @param description item description or null
     * @param formatter item formatter
     * @return descriptor for a discrete item created by line segments representing the outline
     */
    public static ProbeItemDescriptor discreteLineItem(String name, String description,
                                                       ItemValueFormatter formatter) {

        return discreteOutlineItem(name, description, formatter, 1d, 0, MAX_VALUE_UNDEFINED,
                                   DEFAULT_LINE_WIDTH, DEFAULT_COLOR, null);
    }

    /**
     * Creates descriptor for a discrete item created by line segments representing the outline with custom dataFactor ad min/max values.
     *
     * @param name item name
     * @param description item description or null
     * @param formatter item formatter
     * @param dataFactor multiplication factor
     * @param minValue minimum (initial) item value
     * @param maxValue maximum (initial) item value
     * @return descriptor for a discrete item created by line segments representing the outline
     */
    public static ProbeItemDescriptor discreteLineItem(String name, String description,
                                                       ItemValueFormatter formatter,
                                                       double dataFactor,
                                                       long minValue, long maxValue) {

        return discreteOutlineItem(name, description, formatter, dataFactor, minValue,
                                   maxValue, DEFAULT_LINE_WIDTH, DEFAULT_COLOR, null);
    }

    /**
     * Creates descriptor of a discrete item created by filled segments.
     *
     * @param name item name
     * @param description item description or null
     * @param formatter item formatter
     * @return descriptor of a discrete item created by filled segments
     */
    public static ProbeItemDescriptor discreteFillItem(String name, String description,
                                                       ItemValueFormatter formatter) {

        return discreteOutlineItem(name, description, formatter, 1d, 0, MAX_VALUE_UNDEFINED,
                                   DEFAULT_LINE_WIDTH, null, DEFAULT_COLOR);
    }

    /**
     * Creates descriptor of a discrete item created by filled segments with custom dataFactor ad min/max values.
     *
     * @param name item name
     * @param description item description or null
     * @param formatter item formatter
     * @param dataFactor multiplication factor
     * @param minValue minimum (initial) item value
     * @param maxValue maximum (initial) item value
     * @return descriptor of a discrete item created by filled segments
     */
    public static ProbeItemDescriptor discreteFillItem(String name, String description,
                                                       ItemValueFormatter formatter,
                                                       double dataFactor,
                                                       long minValue, long maxValue) {

        return discreteOutlineItem(name, description, formatter, dataFactor, minValue,
                                   maxValue, DEFAULT_LINE_WIDTH, null, DEFAULT_COLOR);
    }

    /**
     * Creates descriptor for a discrete item created by line segments representing the outline and filled segments.
     *
     * @param name item name
     * @param description item description or null
     * @param formatter item formatter
     * @return descriptor for a discrete item created by line segments representing the outline and filled segments
     */
    public static ProbeItemDescriptor discreteLineFillItem(String name, String description,
                                                           ItemValueFormatter formatter) {

        return discreteOutlineItem(name, description, formatter, 1d, 0, MAX_VALUE_UNDEFINED,
                                  DEFAULT_LINE_WIDTH, DEFAULT_COLOR, DEFAULT_COLOR);
    }

    /**
     * Creates descriptor for a discrete item created by line segments representing the outline and filled segments with custom dataFactor ad min/max values.
     *
     * @param name item name
     * @param description item description or null
     * @param formatter item formatter
     * @param dataFactor multiplication factor
     * @param minValue minimum (initial) item value
     * @param maxValue maximum (initial) item value
     * @return descriptor for a discrete item created by line segments representing the outline and filled segments
     */
    public static ProbeItemDescriptor discreteLineFillItem(String name, String description,
                                                           ItemValueFormatter formatter,
                                                           double dataFactor,
                                                           long minValue, long maxValue) {
        
        return discreteOutlineItem(name, description, formatter, dataFactor, minValue,
                                   maxValue, DEFAULT_LINE_WIDTH, DEFAULT_COLOR, DEFAULT_COLOR);
    }

    /**
     * Creates descriptor for a general discrete outlined item with custom dataFactor ad min/max values, and custom line width and line/fill colors.
     *
     * @param name item name
     * @param description item description or null
     * @param formatter item formatter
     * @param dataFactor multiplication factor
     * @param minValue minimum (initial) item value
     * @param maxValue maximum (initial) item value
     * @param lineWidth line width
     * @param lineColor line color or null
     * @param fillColor fill color or null
     * @return descriptor for a general discrete outlined item
     */
    public static ProbeItemDescriptor discreteOutlineItem(String name, String description,
                                                          ItemValueFormatter formatter,
                                                          double dataFactor, long minValue,
                                                          long maxValue, float lineWidth,
                                                          Color lineColor, Color fillColor) {
        
        if (lineColor == null && fillColor == null)
            throw new IllegalArgumentException("Either lineColor or fillColor must be defined"); // NOI18N

        return discreteItem(name, description, formatter, dataFactor, minValue, maxValue,
                            lineWidth, lineColor, fillColor, 0, false, false, true);
    }

    /**
     * Creates descriptor for a discrete item represented by a horizontal line segment, optionally filled, with custom dataFactor ad min/max values.
     *
     * @param name item name
     * @param description item description or null
     * @param formatter item formatter
     * @param dataFactor multiplication factor
     * @param minValue minimum (initial) item value
     * @param maxValue maximum (initial) item value
     * @param filled true if the segments are filled
     * @param width width of/between the segments
     * @param fixedWidth true if width defines segment width, false if width defines segments spacing
     * @return descriptor for a discrete item represented by a horizontal line segment, optionally filled
     */
    public static ProbeItemDescriptor discreteToplineItem(String name, String description,
                                                          ItemValueFormatter formatter,
                                                          double dataFactor, long minValue,
                                                          long maxValue, boolean filled,
                                                          int width, boolean fixedWidth) {

        return discreteItem(name, description, formatter, dataFactor, minValue, maxValue,
                            DEFAULT_LINE_WIDTH, DEFAULT_COLOR, filled ? DEFAULT_COLOR : null,
                            width, fixedWidth, true, false);
    }

    /**
     * Creates descriptor for a discrete item represented by a horizontal line segment, optionally filled, with custom dataFactor ad min/max values, and custom line width and line/fill colors.
     *
     * @param name item name
     * @param description item description or null
     * @param formatter item formatter
     * @param dataFactor multiplication factor
     * @param minValue minimum (initial) item value
     * @param maxValue maximum (initial) item value
     * @param lineWidth line width
     * @param lineColor line color or null
     * @param fillColor fill color or null
     * @param width width width of/between the segments
     * @param fixedWidth fixedWidth true if width defines segment width, false if width defines segments spacing
     * @return descriptor for a discrete item represented by a horizontal line segment, optionally filled
     */
    public static ProbeItemDescriptor discreteToplineItem(String name, String description,
                                                          ItemValueFormatter formatter,
                                                          double dataFactor, long minValue,
                                                          long maxValue, float lineWidth,
                                                          Color lineColor, Color fillColor,
                                                          int width, boolean fixedWidth) {
        
        if (lineColor == null && fillColor == null)
            throw new IllegalArgumentException("Either lineColor or fillColor must be defined"); // NOI18N

        return discreteItem(name, description, formatter, dataFactor, minValue, maxValue,
                            lineWidth, lineColor, fillColor, width, fixedWidth, true, false);
    }

    /**
     * Creates descriptor for a discrete item represented by vertical bars, with custom dataFactor ad min/max values
     *
     * @param name item name
     * @param description item description or null
     * @param formatter item formatter
     * @param dataFactor multiplication factor
     * @param minValue minimum (initial) item value
     * @param maxValue maximum (initial) item value
     * @param outlined true if the bars are outlined
     * @param filled true if the bars are filled
     * @param width width width of/between the bars
     * @param fixedWidth fixedWidth true if width defines bar width, false if width defines bars spacing
     * @return descriptor for a discrete item represented by vertical bars
     */
    public static ProbeItemDescriptor discreteBarItem(String name, String description,
                                                      ItemValueFormatter formatter,
                                                      double dataFactor, long minValue,
                                                      long maxValue, boolean outlined,
                                                      boolean filled, int width,
                                                      boolean fixedWidth) {

        if (!outlined && !filled)
            throw new IllegalArgumentException("Either outlined or filled must be set"); // NOI18N

        return discreteItem(name, description, formatter, dataFactor, minValue, maxValue,
                            DEFAULT_LINE_WIDTH, outlined ? DEFAULT_COLOR : null,
                            filled ? DEFAULT_COLOR : null, width, fixedWidth, false, !filled);
    }

    /**
     * Creates descriptor for a discrete item represented by vertical bars, with custom dataFactor ad min/max values, and custom line width and line/fill colors.
     *
     * @param name item name
     * @param description item description or null
     * @param formatter item formatter
     * @param dataFactor multiplication factor
     * @param minValue minimum (initial) item value
     * @param maxValue maximum (initial) item value
     * @param lineWidth line width
     * @param lineColor line color or null
     * @param fillColor fill color or null
     * @param width width width of/between the bars
     * @param fixedWidth fixedWidth true if width defines bar width, false if width defines bars spacing
     * @return descriptor for a discrete item represented by vertical bars
     */
    public static ProbeItemDescriptor discreteBarItem(String name, String description,
                                                      ItemValueFormatter formatter,
                                                      double dataFactor, long minValue,
                                                      long maxValue, float lineWidth,
                                                      Color lineColor, Color fillColor,
                                                      int width, boolean fixedWidth) {
        
        if (lineColor == null && fillColor == null)
            throw new IllegalArgumentException("Either lineColor or fillColor must be defined"); // NOI18N

        return discreteItem(name, description, formatter, dataFactor, minValue, maxValue,
                            lineWidth, lineColor, fillColor, width, fixedWidth, false, fillColor == null);
    }

    private static ProbeItemDescriptor discreteItem(String name, String description,
                                                    ItemValueFormatter formatter,
                                                    double dataFactor, long minValue,
                                                    long maxValue, float lineWidth,
                                                    Color lineColor, Color fillColor,
                                                    int width, boolean fixedWidth,
                                                    boolean topLineOnly,
                                                    boolean outlineOnly) {

        return new DiscreteXYItemDescriptor(name, description, formatter, dataFactor,
                                            minValue, maxValue, lineWidth, lineColor,
                                            fillColor, width, fixedWidth, topLineOnly,
                                            outlineOnly);
    }

}
