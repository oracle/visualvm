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

package com.sun.tools.visualvm.charts.xy;

import java.util.Arrays;
import org.netbeans.lib.profiler.charts.Timeline;
import org.netbeans.lib.profiler.charts.xy.synchronous.SynchronousXYItem;

/**
 *
 * @author Jiri Sedlacek
 */
public class XYStorage implements Timeline {

    public static final long NO_VALUE = Long.MIN_VALUE - 1;

    private int valuesLimit;
    private int bufferStep;

    private int valuesCount;
    private long[] timestamps;
    private long[][] values;

    private int cycleIndex;

    
    public XYStorage(int valuesLimit, int bufferStep) {
        this.valuesLimit = valuesLimit;
        this.bufferStep = bufferStep;
        initialize();
    }


    public SynchronousXYItem addItem(String name, long minValue, long maxValue) {
        final int itemIndex = addItemImpl();
        return new XYItem(name, minValue, maxValue) {
            public long getYValue(int valueIndex) {
                return getValue(itemIndex, valueIndex);
            }
        };
    }


    public void addValues(long timestamp, long[] values) {
        updateStorage();

        setTimestamp(Math.min(valuesCount, valuesLimit - 1), timestamp);
        for (int i = 0; i < values.length; i++)
            setValue(i, Math.min(valuesCount, valuesLimit - 1), values[i]);

        if (valuesCount < valuesLimit) valuesCount++;
    }


    private void initialize() {
        reset();
    }

    private void reset() {
        valuesCount = 0;
        cycleIndex = 0;

        timestamps = null;
        if (values != null) {
            if (values.length == 0) values = null;
            else for (int i = 0; i < values.length; i++)
                    values[i] = new long[bufferStep];
        }
    }


    private int addItemImpl() {
        int itemIndex = 0;
        if (timestamps == null) {
            timestamps = new long[bufferStep];
            values = new long[1][];
            values[0] = new long[bufferStep];
        } else {
            values = extendArray(values, 1);
            itemIndex = values.length - 1;
            values[itemIndex] = new long[timestamps.length];
            if (values[itemIndex].length > 0)
                Arrays.fill(values[itemIndex], NO_VALUE);
        }
        return itemIndex;
    }


    private int getIndex(int index) {
        if (cycleIndex != 0) {
            index += cycleIndex;
            if (index >= valuesCount) index -= valuesCount;
        }
        return index;
    }

    public int getTimestampsCount() {
        return valuesCount;
    }

    private void setTimestamp(int index, long value) {
        timestamps[getIndex(index)] = value;
    }

    public long getTimestamp(int index) {
        return timestamps[getIndex(index)];
    }

    private void setValue(int itemIndex, int valueIndex, long value) {
        values[itemIndex][getIndex(valueIndex)] = value;
    }

    private long getValue(int itemIndex, int valueIndex) {
        return values[itemIndex][getIndex(valueIndex)];
    }


    private void updateStorage() {
        int bufferSize = timestamps.length;
        if (valuesCount == bufferSize && bufferSize < valuesLimit) {
            int extent = Math.min(bufferStep, valuesLimit - bufferSize);
            timestamps = extendArray(timestamps, extent);
            for (int i = 0; i < values.length; i++)
                values[i] = extendArray(values[i], extent);
            cycleIndex = 0;
        } else if (valuesCount == valuesLimit) {
            cycleIndex++;
            if (cycleIndex == valuesLimit) cycleIndex = 0;
        }
    }

    private static long[] extendArray(long[] array, int extraLength) {
        int originalLength = array.length;
        long[] newArray = new long[originalLength + extraLength];
        System.arraycopy(array, 0, newArray, 0, originalLength);
        return newArray;
    }

    private static long[][] extendArray(long[][] array, int extraLength) {
        int originalLength = array.length;
        long[][] newArray = new long[originalLength + extraLength][];
        System.arraycopy(array, 0, newArray, 0, originalLength);
        return newArray;
    }

}
