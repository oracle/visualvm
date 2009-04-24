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

package org.netbeans.lib.profiler.charts.axis;

import java.text.NumberFormat;

/**
 *
 * @author Jiri Sedlacek
 */
public class BytesAxisUtils {

    public static final long[] bytesUnitsGrid = new long[] { 1, 2, 5, 10, 25, 50, 100, 250, 500 };
    public static final String[] radixUnits = new String[] { "B", "KB", "MB", "GB", "TB", "PB" };

    private static final NumberFormat FORMAT = NumberFormat.getInstance();

    public static long[] getBytesUnits(double scale, int minDistance) {
        if (scale == Double.POSITIVE_INFINITY || scale <= 0)
            return new long[] { -1, -1 };

        long bytesFactor = 1;
        long bytesRadix  = 0;

        while (true) {
            for (int i = 0; i < bytesUnitsGrid.length; i++)
                if ((bytesUnitsGrid[i] * scale * bytesFactor) >= minDistance)
                    return new long[] { bytesUnitsGrid[i] * bytesFactor, bytesRadix };

            bytesFactor *= 1024;
            bytesRadix  += 1;
        }
    }

    public static String getRadixUnits(BytesMark mark) {
        int radix = mark.getRadix();
        if (radix < 0 || radix >= radixUnits.length) return ""; // NOI18N
        return radixUnits[radix];
    }

    public static String formatBytes(BytesMark mark) {
        int radix = mark.getRadix();
        long value = mark.getValue() / (long)Math.pow(1024, radix);
        String units = getRadixUnits(mark);

        return FORMAT.format(value) + " " + units;
    }

}
