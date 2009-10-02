/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
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

package org.netbeans.lib.profiler.ui.threads;

import java.awt.*;
import java.util.HashMap;
import java.util.ResourceBundle;


/**
 *
 * @author  Jiri Sedlacek
 */
public class TimeLineUtils {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.netbeans.lib.profiler.ui.threads.Bundle"); // NOI18N
    private static final String MINUTES_ABBR = messages.getString("TimeLineUtils_MinutesAbbr"); // NOI18N
    private static final String HOURS_ABBR = messages.getString("TimeLineUtils_HoursAbbr"); // NOI18N
    private static final String HOURS_LEGEND_ABBR = messages.getString("TimeLineUtils_HoursLegendAbbr"); // NOI18N
    private static final String MINUTES_LEGEND_ABBR = messages.getString("TimeLineUtils_MinutesLegendAbbr"); // NOI18N
    private static final String SECONDS_LEGEND_ABBR = messages.getString("TimeLineUtils_SecondsLegendAbbr"); // NOI18N
    private static final String MILLISECONDS_LEGEND_ABBR = messages.getString("TimeLineUtils_MillisecondsLegendAbbr"); // NOI18N
                                                                                                                       // -----
    public static final int MIN_TIMEMARK_STEP = 150; // The minimal distance between two time marks
    public static final Color BASE_TIMELINE_COLOR = new Color(0, 0, 0);
    public static final Color MAIN_TIMELINE_COLOR = new Color(150, 150, 150);
    public static final Color TICK_TIMELINE_COLOR = new Color(230, 230, 230);
    private static final int TIME_FORMAT_UNKNOWN = -1;
    private static final int TIME_FORMAT_MILLIS = 10;
    private static final int TIME_FORMAT_SECONDS = 20;
    private static final int TIME_FORMAT_MINUTES = 30;
    private static final int TIME_FORMAT_HOURS = 40;
    private static final int[] timeUnitsGrid = new int[] {
                                                   10, 50, 100, 250, 500, // milliseconds
    1000, 5000, 10000, 30000, // seconds
    60000, 180000, 300000, 600000, // minutes
    3600000, 18000000, 36000000
                                               }; // hours
    private static final int[] ticksCountGrid = new int[] { 10, 5, 10, 5, 5, 10, 5, 10, 6, 6, 6, 5, 10, 6, 5, 10 };
    private static final int[] timeUnitsFormat = new int[] {
                                                     TIME_FORMAT_MILLIS, TIME_FORMAT_MILLIS, TIME_FORMAT_MILLIS,
                                                     TIME_FORMAT_MILLIS, TIME_FORMAT_MILLIS, TIME_FORMAT_SECONDS,
                                                     TIME_FORMAT_SECONDS, TIME_FORMAT_SECONDS, TIME_FORMAT_SECONDS,
                                                     TIME_FORMAT_MINUTES, TIME_FORMAT_MINUTES, TIME_FORMAT_MINUTES,
                                                     TIME_FORMAT_MINUTES, TIME_FORMAT_HOURS, TIME_FORMAT_HOURS, TIME_FORMAT_HOURS
                                                 };
    private static final HashMap timeUnitsToIndex = new HashMap();

    static {
        for (int i = 0; i < timeUnitsGrid.length; i++) {
            timeUnitsToIndex.put(new Integer(timeUnitsGrid[i]), new Integer(i));
        }
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static String getHoursValue(long mark) {
        // Hours
        long hours = mark / 3600000;

        return "" + hours + " " + HOURS_ABBR; // NOI18N
    }

    public static String getMillisValue(long mark) {
        // Hours
        long hours = mark / 3600000;
        String sHours = ((hours == 0) ? "" : ("" + hours + ":")); // NOI18N
        mark = mark % 3600000;

        // Minutes
        long minutes = mark / 60000;
        String sMinutes = (((hours > 0) && (minutes < 10)) ? ("0" + minutes) : ("" + minutes)) + ":"; // NOI18N
        mark = mark % 60000;

        // Seconds
        long seconds = mark / 1000;
        String sSeconds = ((seconds < 10) ? ("0" + seconds) : ("" + seconds)) + "."; // NOI18N
        mark = mark % 1000;

        // Milliseconds
        long millis = mark;
        String sMillis = "" + millis; // NOI18N

        if (millis < 10) {
            sMillis = "0" + sMillis; // NOI18N
        }

        if (millis < 100) {
            sMillis = "0" + sMillis; // NOI18N
        }

        return sHours + sMinutes + sSeconds + sMillis;
    }
    
    public static String getMillisValue2(long mark) {
        // Hours
        long hours = mark / 3600000;
        String sHours = ((hours == 0) ? "" : ("" + hours + ":")); // NOI18N
        mark = mark % 3600000;

        // Minutes
        long minutes = mark / 60000;
        String sMinutes = hours == 0 && minutes == 0 ? "" : (((hours > 0) && (minutes < 10)) ? ("0" + minutes) : ("" + minutes)) + ":"; // NOI18N
        mark = mark % 60000;

        // Seconds
        long seconds = mark / 1000;
        String sSeconds = ((seconds < 10 && sMinutes.length() > 0) ? ("0" + seconds) : ("" + seconds)) + "."; // NOI18N
        mark = mark % 1000;

        // Milliseconds
        long millis = mark;
        String sMillis = "" + millis; // NOI18N

        if (millis < 10) {
            sMillis = "0" + sMillis; // NOI18N
        }

        if (millis < 100) {
            sMillis = "0" + sMillis; // NOI18N
        }

        return sHours + sMinutes + sSeconds + sMillis;
    }

    public static String getMinutesValue(long mark) {
        // Hours
        long hours = mark / 3600000;
        String sHours = ((hours == 0) ? "" : ("" + hours + ":")); // NOI18N
        mark = mark % 3600000;

        // Minutes
        long minutes = mark / 60000;
        String sMinutes = (((hours > 0) && (minutes < 10)) ? ("0" + minutes) : ("" + minutes + " " + MINUTES_ABBR)); // NOI18N

        return sHours + sMinutes;
    }

    public static int getOptimalUnits(float factor) {
        for (int i = 0; i < (timeUnitsGrid.length - 1); i++) {
            if ((timeUnitsGrid[i] * factor) >= MIN_TIMEMARK_STEP) {
                return timeUnitsGrid[i];
            }
        }

        return timeUnitsGrid[timeUnitsGrid.length - 1];
    }

    public static String getSecondsValue(long mark) {
        // Hours
        long hours = mark / 3600000;
        String sHours = ((hours == 0) ? "" : ("" + hours + ":")); // NOI18N
        mark = mark % 3600000;

        // Minutes
        long minutes = mark / 60000;
        String sMinutes = (((hours > 0) && (minutes < 10)) ? ("0" + minutes) : ("" + minutes)) + ":"; // NOI18N
        mark = mark % 60000;

        // Seconds
        long seconds = mark / 1000;
        String sSeconds = ((seconds < 10) ? ("0" + seconds) : ("" + seconds)); // NOI18N

        return sHours + sMinutes + sSeconds;
    }

    public static int getTicksCount(int optimalUnits) {
        int ticksGridIndex = getUnitsIndex(optimalUnits);

        if (ticksGridIndex == -1) {
            return 0;
        }

        return ticksCountGrid[ticksGridIndex];
    }

    public static String getTimeMarkMillisString(int mark, int optimalUnits) {
        int format = getTimeUnitsFormat(optimalUnits);

        if (format != TIME_FORMAT_MILLIS) {
            return ""; // NOI18N
        }

        // Hours
        mark = mark % 3600000;
        // Minutes
        mark = mark % 60000;
        // Seconds
        mark = mark % 1000;

        // Milliseconds
        int millis = mark;
        String sMillis = "" + millis; // NOI18N

        if (millis < 10) {
            sMillis = "0" + sMillis; // NOI18N
        }

        if (millis < 100) {
            sMillis = "0" + sMillis; // NOI18N
        }

        return sMillis;
    }

    public static String getTimeMarkNoMillisString(int mark, int optimalUnits) {
        int format = getTimeUnitsFormat(optimalUnits);

        if (format == TIME_FORMAT_UNKNOWN) {
            return ""; // NOI18N
        }

        if (format == TIME_FORMAT_MILLIS) {
            format = TIME_FORMAT_SECONDS;
        }

        return getTimeMarkStringFromFormat(mark, format);
    }

    public static String getTimeMarkString(int mark, int optimalUnits) {
        int format = getTimeUnitsFormat(optimalUnits);

        if (format == TIME_FORMAT_UNKNOWN) {
            return ""; // NOI18N
        }

        return getTimeMarkStringFromFormat(mark, format);
    }

    public static String getUnitsLegend(int lastMark, int optimalUnits) {
        String timeMarkNoMillis = getTimeMarkNoMillisString(lastMark, optimalUnits);

        if (timeMarkNoMillis.endsWith(MINUTES_ABBR)) {
            return "[" + MINUTES_LEGEND_ABBR + "]"; // NOI18N
        }

        if (timeMarkNoMillis.endsWith(HOURS_ABBR)) {
            return "[" + HOURS_LEGEND_ABBR + "]"; // NOI18N
        }

        String sMillis = ""; // NOI18N

        if (!getTimeMarkMillisString(lastMark, optimalUnits).equals("")) {
            sMillis = "." + MILLISECONDS_LEGEND_ABBR; // NOI18N
        }

        int hours = lastMark / 3600000;

        if (hours != 0) {
            return "[" + HOURS_LEGEND_ABBR + ":" + MINUTES_LEGEND_ABBR + ":" + SECONDS_LEGEND_ABBR + sMillis + "]"; // NOI18N
        }

        return "[" + MINUTES_LEGEND_ABBR + ":" + SECONDS_LEGEND_ABBR + sMillis + "]"; // NOI18N
    }

    private static String getTimeMarkStringFromFormat(int mark, int format) {
        switch (format) {
            case TIME_FORMAT_MILLIS:
                return getMillisValue(mark);
            case TIME_FORMAT_SECONDS:
                return getSecondsValue(mark);
            case TIME_FORMAT_MINUTES:
                return getMinutesValue(mark);
            case TIME_FORMAT_HOURS:
                return getHoursValue(mark);
            default:
                return ""; // NOI18N
        }
    }

    private static int getTimeUnitsFormat(int optimalUnits) {
        int timeUnitsFormatIndex = getUnitsIndex(optimalUnits);

        if (timeUnitsFormatIndex == -1) {
            return TIME_FORMAT_UNKNOWN;
        }

        return timeUnitsFormat[timeUnitsFormatIndex];
    }

    private static int getUnitsIndex(int optimalUnits) {
        Object oTimeUnitsFormatIndex = timeUnitsToIndex.get(new Integer(optimalUnits));

        if (oTimeUnitsFormatIndex == null) {
            return -1;
        }

        return ((Integer) oTimeUnitsFormatIndex).intValue();
    }
}
