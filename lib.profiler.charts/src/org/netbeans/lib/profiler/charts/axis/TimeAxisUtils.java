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

import java.text.DateFormat;
import java.text.Format;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Jiri Sedlacek
 */
public class TimeAxisUtils {

    public static final int NTHNG_NEEDED = 0;
    public static final int DAY_NEEDED  = 1;
    public static final int MONTH_NEEDED = 2;
    public static final int YEAR_NEEDED  = 4;

    public static final int STEP_MSEC    = 1;
    public static final int STEP_SEC     = 2;
    public static final int STEP_MIN     = 4;
    public static final int STEP_HOUR    = 8;
    public static final int STEP_DAY     = 16;
    public static final int STEP_WEEK    = 32;
    public static final int STEP_MONTH   = 64;
    public static final int STEP_YEAR    = 128;

    public static final String TIME_SEC = createTimeSec();
    public static final String TIME_MSEC = createTimeMSec(TIME_SEC);
    public static final String TIME_MIN = createTimeMin();

    public static final String DATE_YEAR = createDateYear();
    public static final String DATE_MONTH = createDateMonth(DATE_YEAR);

    public static final String DATE_WEEKDAY_SHORT = "EEE"; // NOI18N
    public static final String DATE_WEEKDAY = "EEEE"; // NOI18N
    public static final String DATE_YEARMONTH = "MMMM"; // NOI18N
    public static final String DATE_SINGLEYEAR = "yyyy"; // NOI18N

    // time: {0}, date: {1}, time should be first
    public static final String TIME_DATE_FORMAT = "{0}, {1}";

    private static final String PATTERN_CHARS = "GyMwWDdFEaHkKhmsSzZ"; // NOI18N
    private static final Map<String, Format> FORMATS = new HashMap();

    public static final long[] timeUnitsGrid = new long[] {
        1 /*1*/, 2 /*2*/, 5 /*5*/, 10 /*10*/, 20 /*20*/, 50 /*50*/, 100 /*100*/, 250 /*250*/, 500 /*500*/,  // milliseconds
        1000 /*1*/, 2000 /*2*/, 5000 /*5*/, 10000 /*10*/, 15000 /*15*/, 30000 /*30*/,                       // seconds
        60000 /*1*/, 120000 /*2*/, 300000 /*5*/, 600000 /*10*/, 900000 /*15*/, 1800000 /*30*/,              // minutes
        3600000 /*1*/, 7200000 /*2*/, 10800000 /*3*/, 21600000 /*6*/, 43200000 /*12*/,                      // hours
        86400000 /*1*/, //172800000 /*2*/,                                                                  // days
        604800000 /*1*/, //1209600000 /*2*/,                                                                // weeks
        2628000000l /*1*/, 5256000000l /*2*/, 7884000000l /*3*/, 15768000000l /*6*/,                        // months (NOTE: not exactly!!!)
        31536000000l /*1*/, 63072000000l /*2*/, 157680000000l /*5*/, 315360000000l /*10*/                   // years (NOTE: not exactly!!!)
    };

    public static long getTimeUnits(double scale, int minDistance) {
        if (scale == Double.POSITIVE_INFINITY || scale <= 0) return -1;

        for (int i = 0; i < timeUnitsGrid.length; i++)
            if (timeUnitsGrid[i] * scale >= minDistance)
                return timeUnitsGrid[i];
        return timeUnitsGrid[timeUnitsGrid.length - 1];
    }

    public static int getStepFlag(long step) {
        if (step > 15768000000l) return STEP_YEAR;
        if (step > 1209600000) return STEP_MONTH;
        if (step > 172800000) return STEP_WEEK;
        if (step > 43200000) return STEP_DAY;
        if (step > 1800000) return STEP_HOUR;
        if (step > 30000) return STEP_MIN;
        if (step > 500) return STEP_SEC;
        return STEP_MSEC;
    }

    public static int getRangeFlag(long startTime, long endTime) {
        Calendar c1 = Calendar.getInstance();
        c1.setTimeInMillis(startTime);
        Calendar c2 = Calendar.getInstance();
        c2.setTimeInMillis(endTime);

        if (c1.get(Calendar.YEAR) != c2.get(Calendar.YEAR))
            return YEAR_NEEDED;

        if (c1.get(Calendar.MONTH) != c2.get(Calendar.MONTH))
            return MONTH_NEEDED;

        if (c1.get(Calendar.DAY_OF_YEAR) != c2.get(Calendar.DAY_OF_YEAR))
            return DAY_NEEDED;

        return NTHNG_NEEDED;
    }

    public static String getFormatString(long step, long startTime, long endTime) {
        int stepFlag = getStepFlag(step);
        int rangeFlag = getRangeFlag(startTime, endTime);

        String time = null;
        String date = null;

        // time necessary
        if (stepFlag < STEP_DAY) {
            // 12:34
            if (stepFlag > STEP_SEC) time = TIME_MIN;
            // 12:34:55
            else if (stepFlag > STEP_MSEC) time = TIME_SEC;
            // 12:34:55.666
            else time = TIME_MSEC;
        }
        
        // date necessary
        if (stepFlag > STEP_HOUR || rangeFlag != NTHNG_NEEDED) {
            // 2009
            if (stepFlag == STEP_YEAR) date = DATE_SINGLEYEAR;
            // January
            else if (stepFlag == STEP_MONTH && rangeFlag < YEAR_NEEDED) date = DATE_YEARMONTH;
            // Monday
            else if (stepFlag == STEP_DAY && rangeFlag < MONTH_NEEDED) date = DATE_WEEKDAY;
            // Jan 10, 2009
            else if (rangeFlag == YEAR_NEEDED) date = DATE_YEAR;
            // Jan 10
            else if (rangeFlag == MONTH_NEEDED || stepFlag > STEP_DAY) date = DATE_MONTH;
            // Mon
            else if (rangeFlag == DAY_NEEDED) date = DATE_WEEKDAY_SHORT;
        }

        if (time == null) return date;
        else if (date == null) return time;
        else return MessageFormat.format(TIME_DATE_FORMAT,
                                         new Object[] { time, date});
    }

    public static String formatTime(TimeMark mark) {
        String formatString = mark.getFormat();

        Format format = FORMATS.get(formatString);
        if (format == null) {
            format = new SimpleDateFormat(formatString);
            FORMATS.put(formatString, format);
        }

        return format.format(mark.getValue());
    }


    private static String createTimeSec() {
        return ((SimpleDateFormat)DateFormat.
                getTimeInstance(DateFormat.MEDIUM)).
                toPattern();
    }

    private static String createTimeMin() {
        return ((SimpleDateFormat)DateFormat.
                getTimeInstance(DateFormat.SHORT)).
                toPattern();
    }

    private static String createTimeMSec(String timeSec) {
        return timeSec.replace("ss", "ss.SSS"); // NOI18N
    }

    private static String createDateYear() {
        return ((SimpleDateFormat)DateFormat.
                getDateInstance(DateFormat.MEDIUM)).
                toPattern();
    }

    private static String createDateMonth(String dateYear) {
        try {
            // Remove the year
            String dateDay = dateYear.replace("y", ""); // NOI18N
            if (dateDay.length() == 0) return dateDay;

            // Cleanup any leading formatting
            String firstLetter = dateDay.substring(0, 1);
            while (dateDay.length() > 0 && !isPatternChar(firstLetter)) {
                dateDay = dateDay.substring(1);
                firstLetter = dateDay.substring(0, 1);
            }

            // Cleanup any trailing formatting
            int length = dateDay.length();
            String lastLetter = dateDay.substring(length - 1, length);
            // NOTE: '.' seems to be a valid separator to be left here
            while (length > 0 &&
                   !".".equals(lastLetter) && // NOI18N
                   !isPatternChar(lastLetter)) {
                dateDay = dateDay.substring(0, length-- - 1);
                lastLetter = dateDay.substring(length - 1, length);
            }

            return dateDay;
        } catch (Exception e) {
            // The above is not absolutely failproof
            return "MMM d"; // NOI18N
        }
    }

    private static boolean isPatternChar(String s) {
        return PATTERN_CHARS.indexOf(s) != -1;
    }

}
