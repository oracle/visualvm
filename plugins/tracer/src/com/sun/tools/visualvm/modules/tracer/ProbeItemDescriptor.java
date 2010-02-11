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

import com.sun.tools.visualvm.modules.tracer.impl.timeline.TimelinePaintersFactory;
import java.awt.Color;

/**
 * This class describes TracerProbe items appearance in the UI.
 * Work in progress, the API and implementation is subject to change.
 *
 * @author Jiri Sedlacek
 */
public abstract class ProbeItemDescriptor {

    private final String name;
    private final String description;

    private final double dataFactor;
    private final double viewFactor;
    private final String units;

    private final int chartType;
    private final Color[] colors;
    private final Object[] data;


    private ProbeItemDescriptor(String name, String description,
                                double dataFactor, double viewFactor,
                                String units, int chartType,
                                Color[] colors, Object[] data) {
        this.name = name;
        this.description = description;
        this.dataFactor = dataFactor;
        this.viewFactor = viewFactor;
        this.units = units;
        this.chartType = chartType;
        this.colors = colors;
        this.data = data;
    }

    public final String getName() { return name; }

    public final String getDescription() { return description; }

    public final double getDataFactor() { return dataFactor; }

    public final double getViewFactor() { return viewFactor; }

    public final String getUnits() { return units; }

    public final int getChartType() { return chartType; }

    public final Color[] getColors() { return colors; }

    public final Object[] getData() { return data; }


    public static final class LineItem extends ProbeItemDescriptor {

        public LineItem(String name) {
            this(name, 1d);
        }
        
        public LineItem(String name, double factor) {
            this(name, null, null, factor);
        }

        public LineItem(String name, String description, String units, double factor) {
            this(name, description, units, factor, null, -1f);
        }

        public LineItem(String name, String description, String units, double factor, Color color, float width) {
            super(name, description, factor, factor, units, TimelinePaintersFactory.LINE_ITEM,
                  new Color[] { color }, new Object[] { Float.valueOf(width == -1f ? 2 : width) });
        }

    }

}
