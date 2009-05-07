/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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

package org.netbeans.modules.profiler.ppoints;

import org.netbeans.api.project.Project;
import org.openide.ErrorManager;
import java.util.Properties;


/**
 *
 * @author Jiri Sedlacek
 */
public abstract class TimedGlobalProfilingPoint extends GlobalProfilingPoint {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    public static class TimeCondition {
        //~ Static fields/initializers -------------------------------------------------------------------------------------------

        public static final String PROPERTY_TIMECOND_STARTTIME = "p_timecond_starttime"; // NOI18N
        public static final String PROPERTY_TIMECOND_REPEATS = "p_timecond_repeats"; // NOI18N
        public static final String PROPERTY_TIMECOND_PERIODTIME = "p_timecond_periodtime"; // NOI18N
        public static final String PROPERTY_TIMECOND_PERIODUNITS = "p_timecond_periodunits"; // NOI18N
        public static final int UNITS_MINUTES = 1;
        public static final int UNITS_HOURS = 2;

        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private boolean repeats;
        private int periodTime;
        private int periodUnits;
        private long scheduledTime;
        private long startTime;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public TimeCondition() {
            this(System.currentTimeMillis());
        }

        public TimeCondition(long startTime) {
            this(startTime, false, 1, UNITS_MINUTES);
        }

        public TimeCondition(long startTime, boolean repeats, int periodTime, int periodUnits) {
            setStartTime(startTime);
            setRepeats(repeats);
            setPeriodTime(periodTime);
            setPeriodUnits(periodUnits);
            setScheduledTime(startTime);
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void setPeriodTime(int periodTime) {
            this.periodTime = periodTime;
        }

        public int getPeriodTime() {
            return periodTime;
        }

        public void setPeriodUnits(int periodUnits) {
            this.periodUnits = periodUnits;
        }

        public int getPeriodUnits() {
            return periodUnits;
        }

        public void setRepeats(boolean repeats) {
            this.repeats = repeats;
        }

        public boolean getRepeats() {
            return repeats;
        }

        public void setStartTime(long startTime) {
            this.startTime = startTime;
            this.scheduledTime = startTime;
        }

        public long getStartTime() {
            return startTime;
        }

        public boolean equals(Object object) {
            if (!(object instanceof TimeCondition)) {
                return false;
            }

            TimeCondition condition = (TimeCondition) object;

            return (startTime == condition.startTime) && (repeats == condition.repeats) && (periodTime == condition.periodTime)
                   && (periodUnits == condition.periodUnits);
        }

        public static TimeCondition load(Project project, int index, Properties properties) {
            return load(project, index, null, properties);
        }

        public static TimeCondition load(Project project, int index, String prefix, Properties properties) {
            String absPrefix = (prefix == null) ? (index + "_") : (index + "_" + prefix); // NOI18N
            String startTimeStr = properties.getProperty(absPrefix + PROPERTY_TIMECOND_STARTTIME, null);
            String repeatsStr = properties.getProperty(absPrefix + PROPERTY_TIMECOND_REPEATS, null);
            String periodTimeStr = properties.getProperty(absPrefix + PROPERTY_TIMECOND_PERIODTIME, null);
            String periodUnitsStr = properties.getProperty(absPrefix + PROPERTY_TIMECOND_PERIODUNITS, null);

            if ((startTimeStr == null) || (repeatsStr == null) || (periodTimeStr == null) || (periodUnitsStr == null)) {
                return null;
            }

            TimeCondition condition = null;

            try {
                condition = new TimeCondition(Long.parseLong(startTimeStr), Boolean.parseBoolean(repeatsStr),
                                              Integer.parseInt(periodTimeStr), Integer.parseInt(periodUnitsStr));
            } catch (Exception e) {
                ErrorManager.getDefault().log(ErrorManager.ERROR, e.getMessage());
            }

            return condition;
        }

        public void store(Project project, int index, Properties properties) {
            store(project, index, null, properties);
        }

        public void store(Project project, int index, String prefix, Properties properties) {
            String absPrefix = (prefix == null) ? (index + "_") : (index + "_" + prefix); // NOI18N
            properties.put(absPrefix + PROPERTY_TIMECOND_STARTTIME, Long.toString(startTime));
            properties.put(absPrefix + PROPERTY_TIMECOND_REPEATS, Boolean.toString(repeats));
            properties.put(absPrefix + PROPERTY_TIMECOND_PERIODTIME, Integer.toString(periodTime));
            properties.put(absPrefix + PROPERTY_TIMECOND_PERIODUNITS, Integer.toString(periodUnits));
        }

        void setScheduledTime(long scheduledTime) {
            this.scheduledTime = scheduledTime;
        }

        long getScheduledTime() {
            return scheduledTime;
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    static final String PROPERTY_TIME = "p_timecond"; // NOI18N

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private TimeCondition condition;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    TimedGlobalProfilingPoint(String name, Project project, ProfilingPointFactory factory) {
        super(name, project, factory);
        condition = new TimeCondition();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setCondition(TimeCondition condition) {
        if (this.condition.equals(condition)) {
            return;
        }

        TimeCondition oldCondition = this.condition;
        this.condition = condition;
        getChangeSupport().firePropertyChange(PROPERTY_TIME, oldCondition, condition);
    }

    public TimeCondition getCondition() {
        return condition;
    }
}
