/*
 *  Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.modules.tracer.impl.timeline.items;

import org.graalvm.visualvm.modules.tracer.ItemValueFormatter;
import org.graalvm.visualvm.modules.tracer.ProbeItemDescriptor;

/**
 * 
 * @author Jiri Sedlacek
 */
public abstract class ValueItemDescriptor extends ProbeItemDescriptor {

    private final ItemValueFormatter formatter;
    private final double dataFactor;
    private final long minValue;
    private final long maxValue;


    ValueItemDescriptor(String name, String description,
                        ItemValueFormatter formatter, double dataFactor,
                        long minValue, long maxValue) {

        super(name, description);
        if (formatter == null) {
            throw new IllegalArgumentException("formatter cannot be null"); // NOI18N
        }
        if (dataFactor == 0) {
            throw new IllegalArgumentException("dataFactor cannot be 0"); // NOI18N
        }
        this.formatter = formatter;
        this.dataFactor = dataFactor;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }


    public final String getValueString(long value, int format) {
        return formatter.formatValue(value, format);
    }

    public final String getUnitsString(int format) {
        return formatter.getUnits(format);
    }

    public final double getDataFactor() {
        return dataFactor;
    }

    public final long getMinValue() {
        return minValue;
    }

    public final long getMaxValue() {
        return maxValue;
    }
    
}
