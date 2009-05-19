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

import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ResourceBundle;

/**
 *
 * @author Jiri Sedlacek
 */
public class BytesAxisUtils {

    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.netbeans.lib.profiler.charts.axis.Bundle"); // NOI18N
    public static final String UNITS_B = messages.getString("BytesAxisUtils_AbbrBytes"); // NOI18N
    public static final String UNITS_KB = messages.getString("BytesAxisUtils_AbbrKiloBytes"); // NOI18N
    public static final String UNITS_MB = messages.getString("BytesAxisUtils_AbbrMegaBytes"); // NOI18N
    public static final String UNITS_GB = messages.getString("BytesAxisUtils_AbbrGigaBytes"); // NOI18N
    public static final String UNITS_TB = messages.getString("BytesAxisUtils_AbbrTeraBytes"); // NOI18N
    public static final String UNITS_PB = messages.getString("BytesAxisUtils_AbbrPetaBytes"); // NOI18N
    private static final String SIZE_FORMAT = messages.getString("BytesAxisUtils_SizeFormat"); // NOI18N
    // -----

    public static final long[] bytesUnitsGrid = new long[] { 1, 2, 5, 10, 25, 50, 100, 250, 500 };
    public static final String[] radixUnits = new String[] { UNITS_B, UNITS_KB, UNITS_MB, UNITS_GB, UNITS_TB, UNITS_PB };

    private static final NumberFormat FORMAT = NumberFormat.getInstance();

    public static long[] getBytesUnits(double scale, int minDistance) {
        if (Double.isNaN(scale) || scale == Double.POSITIVE_INFINITY || scale <= 0)
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

        return MessageFormat.format(SIZE_FORMAT, new Object[] { FORMAT.format(value), units });
    }

}
