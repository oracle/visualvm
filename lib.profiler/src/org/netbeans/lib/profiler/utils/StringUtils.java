/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.netbeans.lib.profiler.utils;

import java.text.DateFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.ResourceBundle;


/**
 * Utility methods for String-related operations.
 *
 * @author Misha Dmitriev
 * @author Ian Formanek
 */
public class StringUtils {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String THIS_WEEK_FORMAT;
    private static final String LAST_WEEK_FORMAT;
    private static final String YESTERDAY_FORMAT;
    private static NumberFormat percentage;
    private static NumberFormat intFormat = NumberFormat.getIntegerInstance();
    private static char SEPARATOR = DecimalFormatSymbols.getInstance().getDecimalSeparator();
    
    static {
        ResourceBundle messages = ResourceBundle.getBundle("org.netbeans.lib.profiler.utils.Bundle"); // NOI18N
        THIS_WEEK_FORMAT = messages.getString("StringUtils_ThisWeekFormat"); // NOI18N
        LAST_WEEK_FORMAT = messages.getString("StringUtils_LastWeekFormat"); // NOI18N
        YESTERDAY_FORMAT = messages.getString("StringUtils_YesterdayFormat"); // NOI18N
        percentage = NumberFormat.getNumberInstance();
        percentage.setMaximumFractionDigits(1);
        percentage.setMinimumFractionDigits(1);
    }
                                                                                            // -----
    private static SimpleDateFormat thisWeekFormat = new SimpleDateFormat(THIS_WEEK_FORMAT);
    private static SimpleDateFormat lastWeekFormat = new SimpleDateFormat(LAST_WEEK_FORMAT);
    private static SimpleDateFormat yesterdayFormat = new SimpleDateFormat(YESTERDAY_FORMAT);
    private static DateFormat todayFormat = DateFormat.getTimeInstance(DateFormat.MEDIUM);
    private static DateFormat otherFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
    private static DateFormat fullFormat = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.MEDIUM);

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static void appendSplittedLongString(StringBuffer sb, String s, int maxLineLen) {
        int nLines = (s.length() / maxLineLen) + (((s.length() % maxLineLen) != 0) ? 1 : 0);
        int idx = 0;

        for (int i = 0; i < nLines; i++) {
            if (i > 0) {
                sb.append('\n'); // NOI18N
            }

            int endIdx = idx + maxLineLen;

            if (endIdx > s.length()) {
                endIdx = s.length();
            }

            sb.append(s.substring(idx, endIdx));
            idx = endIdx;
        }
    }

    public static String[][] convertPackedStringsIntoStringArrays(byte[] packedData, int[] packedArrayOffsets, int dim) {
        String[][] ret = new String[dim][];
        int len = packedArrayOffsets.length / dim;

        for (int i = 0; i < dim; i++) {
            ret[i] = new String[len];
        }

        int idx = 0;
        int lastIdx = packedArrayOffsets.length - 1;

        for (int j = 0; j < len; j++) {
            for (int i = 0; i < dim; i++) {
                int utf8Len = (idx < lastIdx) ? (packedArrayOffsets[idx + 1] - packedArrayOffsets[idx])
                                              : (packedData.length - packedArrayOffsets[idx]);
                ret[i][j] = utf8ToString(packedData, packedArrayOffsets[idx], utf8Len);
                idx++;
            }
        }

        return ret;
    }

    /** Used to print per cent figures with one digit after decimal point */
    public static String floatPerCentToString(float t) {
        return percentage.format(t);
    }

    // ------------------------------------------------------------------------------------------------
    //    Time/Date formatting
    // ------------------------------------------------------------------------------------------------
    public static String formatFullDate(Date date) {
        return fullFormat.format(date);
    }

    /** Formats provided time/date in a form that is tuned for user wrt to space needed and clarity/usefulness.
     * It compareses the time/date passed against current time, and decides from one of 5 formats:
     * - if the time is today, format it just using the time hh:mm:ss AM/PM
     * - if the time is yesterday, format is as Yesterday, hh:mm AM/PM
     * - if the time is this week, format is as [Name of Day], hh:mm AM/PM
     * - if the time is last week, format is as Last [Name of Day], hh:mm AM/PM
     * - otherwise format it as dd MMM yyyy
     *
     * @param date The date to format
     * @return String with formatted time/date
     */
    public static String formatUserDate(Date date) {
        Calendar now = Calendar.getInstance();
        Calendar before = Calendar.getInstance();
        before.setTime(date);

        if (before.getTimeInMillis() <= now.getTimeInMillis()) { // the time is indeed in the past

            int daysDelta = getDaysDifference(before, now);

            if (daysDelta == 0) {
                // today
                return todayFormat.format(date);
            } else if (daysDelta == 1) {
                // yesterday
                return yesterdayFormat.format(date);
            } else {
                int weekDelta = getWeekDifference(before, now);

                if (weekDelta == 0) {
                    // this week
                    return thisWeekFormat.format(date);
                } else if (weekDelta == 1) {
                    //last week
                    return lastWeekFormat.format(date);
                }
            }
        }

        return otherFormat.format(date);
    }

    /** Represent time (given in microsecond) in milliseconds, with roughly the same number of meaningful digits */
    public static String mcsTimeToString(long t) {
        StringBuilder tmpBuf = new StringBuilder();
        
        if (t < 0) tmpBuf.append("-"); // NOI18N
        t = Math.abs(t);

        if (t >= 100000) {
            tmpBuf.append(intFormat.format(t / 1000));
            return tmpBuf.toString();
        } else if (t >= 10000) {
            long x = t / 1000;
            tmpBuf.append(intFormat.format(x));
            tmpBuf.append(SEPARATOR);
            tmpBuf.append(Long.toString((t - (x * 1000)) / 100));

            return tmpBuf.toString();

            //return Long.toString(x) + "." + Long.toString((t - x*1000) / 100);
        } else if (t >= 1000) {
            long x = t / 1000;
            tmpBuf.append(intFormat.format(x));
            tmpBuf.append(SEPARATOR);
            tmpBuf.append(Long.toString((t - (x * 1000)) / 10));

            return tmpBuf.toString();

            //return Long.toString(x) + "." + Long.toString((t - x*1000) / 10);
        } else {
            if (t >= 100) {
                tmpBuf.append("0"); // NOI18N
                tmpBuf.append(SEPARATOR);
            } else if (t >= 10) {
                tmpBuf.append("0"); // NOI18N
                tmpBuf.append(SEPARATOR);
                tmpBuf.append("0"); // NOI18N
            } else {
                tmpBuf.append("0"); // NOI18N
                tmpBuf.append(SEPARATOR);
                tmpBuf.append("00"); // NOI18N
            }

            return (tmpBuf.append(Long.toString(t))).toString();
        }
    }

    /** Represents the given number of bytes as is, or as "xxx K" (if >= 100 KBytes), or as "xxx M" (if >= 100 MBytes) */
    public static String nBytesToString(long b) {
        StringBuilder tmpBuf = new StringBuilder();

        if (b < (100 * 1024)) {
            return intFormat.format(b) + " B"; // NOI18N
        } else if (b < (100 * 1024 * 1024)) {
            long k = b >> 10;
            tmpBuf.append(intFormat.format(k));

            if (b < (100 * 1024 * 1024)) {
                tmpBuf.append(SEPARATOR); // NOI18N
                tmpBuf.append(Long.toString((b - (k << 10)) / 102)); // 102 stands for 1/10th of 1K
            }

            tmpBuf.append(" KB"); // NOI18N

            return tmpBuf.toString();
        } else {
            long m = b >> 20;
            tmpBuf.append(intFormat.format(m));

            if (b < 10737418240L) {
                tmpBuf.append(SEPARATOR);
                tmpBuf.append(Long.toString((b - (m << 20)) / 104858)); // 104858 stands for 1/10th of 1M
            }

            tmpBuf.append(" MB"); // NOI18N

            return tmpBuf.toString();
        }
    }

    // ------------------------------------------------------------------------------------------------
    //    Miscellaneous
    // ------------------------------------------------------------------------------------------------  
    public static String[] parseArgsString(String args) {
        if (args == null) {
            return new String[0];
        }

        ArrayList listRes = new ArrayList();

        int pos0 = 0;
        int len = args.length();

        while (pos0 < len) {
            int pos1 = pos0;

            while ((pos1 < len) && (args.charAt(pos1) != ' ') && (args.charAt(pos1) != 8)) { // NOI18N
                pos1++;
            }

            listRes.add(args.substring(pos0, pos1));
            pos0 = pos1 + 1;

            while ((pos0 < len) && ((args.charAt(pos0) == ' ') || (args.charAt(pos0) == 8))) { // NOI18N
                pos0++;
            }
        }

        return (String[]) listRes.toArray(new String[listRes.size()]);
    }

    public static String userFormClassName(String className) {
        if (className == null) {
            return null;
        }

        className = className.replace('/', '.'); // NOI18N

        if (className.startsWith("[")) { // NOI18N

            String elemType = null;
            int lastBrackPos = className.lastIndexOf('['); // NOI18N

            if (lastBrackPos == (className.length() - 2)) { // It's an array of ultimately primitive type, e.g. [[C

                switch (className.charAt(lastBrackPos + 1)) {
                    case 'C':
                        elemType = "char"; // NOI18N
                        break;
                    case 'B':
                        elemType = "byte"; // NOI18N
                        break;
                    case 'I':
                        elemType = "int"; // NOI18N
                        break;
                    case 'Z':
                        elemType = "boolean"; // NOI18N
                        break;
                    case 'F':
                        elemType = "float"; // NOI18N
                        break;
                    case 'D':
                        elemType = "double"; // NOI18N
                        break;
                    case 'S':
                        elemType = "short"; // NOI18N
                        break;
                    case 'J':
                        elemType = "long"; // NOI18N
                        break;
                }
            } else {
                elemType = className.substring(lastBrackPos + 1);
            }

            int nDims = lastBrackPos + 1;
            StringBuilder tmpBuf = new StringBuilder();
            tmpBuf.append(elemType);

            for (int i = 0; i < nDims; i++) {
                tmpBuf.append("[]"); // NOI18N
            }

            return tmpBuf.toString();
        } else {
            return className;
        }
    }

    public static String utf8ToString(byte[] src, int stPos, int utf8Len) {
        char[] strBuf = new char[utf8Len];
        int i = stPos;
        int j = 0;
        int limit = stPos + utf8Len;

        while (i < limit) {
            int b = src[i++] & 255;

            if (b >= 224) {
                b = (b & 15) << 12;
                b = b | ((src[i++] & 63) << 6);
                b = b | (src[i++] & 63);
            } else if (b >= 192) {
                b = (b & 31) << 6;
                b = b | (src[i++] & 63);
            }

            strBuf[j++] = (char) b;
        }

        return (new String(strBuf, 0, j)).intern();
    }

    private static int getDaysDifference(Calendar before, Calendar after) {
        int diff = after.get(Calendar.DAY_OF_YEAR) - before.get(Calendar.DAY_OF_YEAR);
        diff = diff + (before.getMaximum(Calendar.DAY_OF_YEAR) * (after.get(Calendar.YEAR) - before.get(Calendar.YEAR)));

        return diff;
    }

    private static int getWeekDifference(Calendar before, Calendar after) {
        int diff = after.get(Calendar.WEEK_OF_YEAR) - before.get(Calendar.WEEK_OF_YEAR);
        diff = diff + (before.getMaximum(Calendar.WEEK_OF_YEAR) * (after.get(Calendar.YEAR) - before.get(Calendar.YEAR)));

        return diff;
    }
}
