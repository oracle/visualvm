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
public abstract class TriggeredGlobalProfilingPoint extends GlobalProfilingPoint {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    public static class TriggerCondition {
        //~ Static fields/initializers -------------------------------------------------------------------------------------------

        public static final String PROPERTY_TRIGGCOND_METRIC = "p_triggcond_metric"; // NOI18N
        public static final String PROPERTY_TRIGGCOND_VALUE = "p_triggcond_value"; // NOI18N
        public static final String PROPERTY_TRIGGCOND_ONETIME = "p_triggcond_onetime"; // NOI18N
        public static final int METRIC_HEAPUSG = 1;
        public static final int METRIC_HEAPSIZ = 2;
        public static final int METRIC_SURVGEN = 3;
        public static final int METRIC_LDCLASS = 4;

        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private boolean onetime;
        private boolean triggered;
        private int metric;
        private long value; // [bytes] for HEAPSIZ, [percent <0 ~ 100>] for HEAPUSG, [count] for SURVGEN, LDCLASS

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public TriggerCondition() {
            this(METRIC_HEAPUSG, 95);
        }

        public TriggerCondition(int metric, long value) {
            this(metric, value, true);
        }

        public TriggerCondition(int metric, long value, boolean onetime) {
            setMetric(metric);
            setValue(value);
            setOnetime(onetime);
            setTriggered(false);
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void setMetric(int metric) {
            this.metric = metric;
        }

        public int getMetric() {
            return metric;
        }

        public void setOnetime(boolean onetime) {
            this.onetime = onetime;
        }

        public boolean isOnetime() {
            return onetime;
        }

        public void setValue(long value) {
            this.value = value;
        }

        public long getValue() {
            return value;
        }

        public boolean equals(Object object) {
            if (!(object instanceof TriggerCondition)) {
                return false;
            }

            TriggerCondition condition = (TriggerCondition) object;

            return (metric == condition.metric) && (value == condition.value) && (onetime == condition.onetime);
        }

        public static TriggerCondition load(Project project, int index, Properties properties) {
            return load(project, index, null, properties);
        }

        public static TriggerCondition load(Project project, int index, String prefix, Properties properties) {
            String absPrefix = (prefix == null) ? (index + "_") : (index + "_" + prefix); // NOI18N
            String metricStr = properties.getProperty(absPrefix + PROPERTY_TRIGGCOND_METRIC, null);
            String valueStr = properties.getProperty(absPrefix + PROPERTY_TRIGGCOND_VALUE, null);
            String onetimeStr = properties.getProperty(absPrefix + PROPERTY_TRIGGCOND_ONETIME, null);

            if ((metricStr == null) || (valueStr == null) || (onetimeStr == null)) {
                return null;
            }

            TriggerCondition condition = null;

            try {
                condition = new TriggerCondition(Integer.parseInt(metricStr), Long.parseLong(valueStr),
                                                 Boolean.parseBoolean(onetimeStr));
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
            properties.put(absPrefix + PROPERTY_TRIGGCOND_METRIC, Integer.toString(metric));
            properties.put(absPrefix + PROPERTY_TRIGGCOND_VALUE, Long.toString(value));
            properties.put(absPrefix + PROPERTY_TRIGGCOND_ONETIME, Boolean.toString(onetime));
        }

        void setTriggered(boolean triggered) {
            this.triggered = triggered;
        }

        boolean isTriggered() {
            return triggered;
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    static final String PROPERTY_TRIGGER = "p_triggcond"; // NOI18N

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private TriggerCondition condition;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    TriggeredGlobalProfilingPoint(String name, Project project, ProfilingPointFactory factory) {
        super(name, project, factory);
        condition = new TriggerCondition();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setCondition(TriggerCondition condition) {
        if (this.condition.equals(condition)) {
            return;
        }

        TriggerCondition oldCondition = this.condition;
        this.condition = condition;
        getChangeSupport().firePropertyChange(PROPERTY_TRIGGER, oldCondition, condition);
    }

    public TriggerCondition getCondition() {
        return condition;
    }
}
