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

package org.graalvm.visualvm.charts.xy;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import org.graalvm.visualvm.lib.charts.Timeline;
import org.graalvm.visualvm.lib.charts.xy.synchronous.SynchronousXYItem;

/**
 *
 * @author Jiri Sedlacek
 */
public class XYStorage implements Timeline {

    private static final String SNAPSHOT_HEADER = "XYStorageSnapshot"; // NOI18N
    private static final int SNAPSHOT_VERSION = 1;

    public static final long NO_VALUE = Long.MIN_VALUE - 1;

    private final int valuesLimit;
    private final int bufferStep;

    private int valuesCount;
    private long[] timestamps;
    private long[][] values;

    private int cycleIndex;

    
    public XYStorage(int valuesLimit, int bufferStep) {
        this.valuesLimit = valuesLimit;
        this.bufferStep = bufferStep;
        initialize();
    }


    public synchronized SynchronousXYItem addItem(String name, long minValue, long maxValue) {
        final int itemIndex = addItemImpl();
        return new XYItem(name, minValue, maxValue) {
            public long getYValue(int valueIndex) {
                return getValue(itemIndex, valueIndex);
            }
        };
    }


    public synchronized void addValues(long timestamp, long[] values) {
        updateStorage();

        setTimestamp(Math.min(valuesCount, valuesLimit - 1), timestamp);
        for (int i = 0; i < values.length; i++)
            setValue(i, Math.min(valuesCount, valuesLimit - 1), values[i]);

        if (valuesCount < valuesLimit) valuesCount++;
    }

    public synchronized void saveValues(OutputStream os) throws IOException {
        DataOutputStream dos = null;
        try {
            int icount = values.length;
            int vcount = getTimestampsCount();

            dos = new DataOutputStream(os);

            dos.writeUTF(SNAPSHOT_HEADER); // Snapshot format
            dos.writeInt(SNAPSHOT_VERSION); // Snapshot version
            dos.writeInt(icount); // Items count
            dos.writeInt(vcount); // Values count

            for (int vidx = 0; vidx < vcount; vidx++) {
                dos.writeLong(getTimestamp(vidx));
                for (int iidx = 0; iidx < icount; iidx++)
                    dos.writeLong(getValue(iidx, vidx));
            }
        } finally {
            if (dos != null) dos.close();
        }
    }

    public synchronized void loadValues(InputStream is) throws IOException {
        DataInputStream dis = null;
        try {
            dis = new DataInputStream(is);

            if (!SNAPSHOT_HEADER.equals(dis.readUTF()))
                throw new IOException("Unknown snapshot format"); // NOI18N
            if (SNAPSHOT_VERSION != dis.readInt())
                throw new IOException("Unsupported snapshot version"); // NOI18N
            if (values.length != dis.readInt())
                throw new IOException("Snapshot doesn't match number of items"); // NOI18N

            int vcount = dis.readInt();
            long[] vals = new long[values.length];
            
            for (int vidx = 0; vidx < vcount; vidx++) {
                long timestamp = dis.readLong();
                for (int iidx = 0; iidx < vals.length; iidx++)
                    vals[iidx] = dis.readLong();
                addValues(timestamp, vals);
            }
        } finally {
            if (dis != null) dis.close();
        }
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

    
    boolean isFull() {
        return valuesCount == valuesLimit;
    }

    private void updateStorage() {
        int bufferSize = timestamps.length;
        if (valuesCount == bufferSize && bufferSize < valuesLimit) {
            int extent = Math.min(bufferStep, valuesLimit - bufferSize);
            timestamps = extendArray(timestamps, extent);
            for (int i = 0; i < values.length; i++)
                values[i] = extendArray(values[i], extent);
            cycleIndex = 0;
        } else if (isFull()) {
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
