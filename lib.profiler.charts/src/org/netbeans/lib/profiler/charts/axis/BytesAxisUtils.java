/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2007-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
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
