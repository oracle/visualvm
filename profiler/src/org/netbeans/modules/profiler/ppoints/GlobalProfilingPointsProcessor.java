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

package org.netbeans.modules.profiler.ppoints;

import org.netbeans.api.project.Project;
import org.netbeans.lib.profiler.client.MonitoredData;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.lib.profiler.results.DataManagerListener;
import org.netbeans.modules.profiler.NetBeansProfiler;
import java.util.LinkedList;
import java.util.List;


/**
 *
 * @author Jiri Sedlacek
 */
public class GlobalProfilingPointsProcessor implements DataManagerListener {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static GlobalProfilingPointsProcessor defaultInstance;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private List<TimedGlobalProfilingPoint> scheduledTimedPPs = new LinkedList();
    private List<TriggeredGlobalProfilingPoint> scheduledTriggeredPPs = new LinkedList();
    private ProfilingSettings profilingSettings;
    private Project profiledProject;
    private GlobalProfilingPoint[] gpp;
    private boolean isRunning = false;
    private long currentHeapSize;
    private long currentHeapUsage;
    private long currentLoadedClasses;
    private long currentSurvGen;
    private long currentTime = System.currentTimeMillis(); // local time of one iteration

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    // --- DataManagerListener implementation ------------------------------------
    public void dataChanged() {
        processTelemetryEvent();
    }

    public void dataReset() {
        processTelemetryEvent();
    }

    // --- Internal interface ----------------------------------------------------
    static synchronized GlobalProfilingPointsProcessor getDefault() {
        if (defaultInstance == null) {
            defaultInstance = new GlobalProfilingPointsProcessor();
        }

        return defaultInstance;
    }

    void notifyProfilingStateChanged() {
        synchronized (this) {
            boolean profilingInProgress = ProfilingPointsManager.getDefault().isProfilingInProgress();
            boolean sessionInProgress = ProfilingPointsManager.getDefault().isProfilingSessionInProgress();

            if (sessionInProgress && !profilingInProgress) { // transition between states

                if (isRunning) {
                    stop(); // modify profiling
                }

                init(); // TODO: unnecessarily called when finishing profiling session
            } else if (profilingInProgress) { // profiling in progress

                if (!isRunning) {
                    start();
                }
            } else { // profiling inactive
                stop();
            }
        }
    }

    private boolean anyProfilingPointsScheduled() {
        return (scheduledTimedPPs.size() > 0) || (scheduledTriggeredPPs.size() > 0);
    }

    private void checkForStop() {
        if (!anyProfilingPointsScheduled()) {
            stop();
        }
    }

    private void init() {
        profiledProject = NetBeansProfiler.getDefaultNB().getProfiledProject();
        profilingSettings = NetBeansProfiler.getDefaultNB().getLastProfilingSettings();

        if ((profiledProject != null) && profilingSettings.useProfilingPoints()) {
            gpp = ProfilingPointsManager.getDefault().createGlobalProfilingConfiguration(profiledProject, profilingSettings);

            for (GlobalProfilingPoint pp : gpp) {
                scheduleProfilingPoint(pp);
            }
        }
    }

    private void initListeners() {
        NetBeansProfiler.getDefaultNB().getVMTelemetryManager().addDataListener(this);
    }

    private void processTelemetryEvent() {
        MonitoredData data = NetBeansProfiler.getDefaultNB().getVMTelemetryManager().getLastData();

        if (data != null) {
            // ----------------------
            // Actually this is being called periodically each 1.2 sec from ProfilingMonitor, can be also used as a timer for timed Profiling Points
            // If no MonitoredData available, also other data most likely won't be available => that's why calling it here
            processTimeEvent();

            // ----------------------
            long currentMaxHeap = NetBeansProfiler.getDefaultNB().getVMTelemetryManager().maxHeapSize;
            currentHeapSize = data.getTotalMemory();

            long currentUsedHeap = currentHeapSize - data.getFreeMemory();
            currentHeapUsage = (long) Math.round(((double) currentUsedHeap / (double) currentMaxHeap) * 100);
            currentSurvGen = data.getNSurvivingGenerations();
            currentLoadedClasses = data.getLoadedClassesCount();

            processTriggeredProfilingPoints();
        } else {
            // no telemetry data available yet
        }
    }

    //// - Core functionality ----------------------------------------------------
    private void processTimeEvent() {
        currentTime = System.currentTimeMillis();
        processTimedProfilingPoints();
    }

    private void processTimedProfilingPoint(TimedGlobalProfilingPoint tgpp, List<TimedGlobalProfilingPoint> rescheduledTimedPPs) {
        tgpp.hit(currentTime);
        scheduleTimedProfilingPoint(tgpp, rescheduledTimedPPs);
    }

    private void processTimedProfilingPoints() {
        synchronized (this) {
            checkForStop();

            if (isRunning) {
                List<TimedGlobalProfilingPoint> rescheduledTimedPPs = new LinkedList();

                for (TimedGlobalProfilingPoint tgpp : scheduledTimedPPs) {
                    if (timeConditionMet(tgpp.getCondition())) {
                        processTimedProfilingPoint(tgpp, rescheduledTimedPPs); // Perform PP and eventually reschedule it
                    } else {
                        rescheduledTimedPPs.add(tgpp); // reschedule currently not active PP for next check
                    }
                }

                scheduledTimedPPs.clear();
                scheduledTimedPPs.addAll(rescheduledTimedPPs);
            }
        }
    }

    private void processTriggeredProfilingPoint(TriggeredGlobalProfilingPoint tgpp,
                                                List<TriggeredGlobalProfilingPoint> rescheduledTriggeredPPs) {
        TriggeredGlobalProfilingPoint.TriggerCondition condition = tgpp.getCondition();

        if (!condition.isTriggered()) {
            condition.setTriggered(true);

            long hitValue = -1;

            if (condition.getMetric() == TriggeredGlobalProfilingPoint.TriggerCondition.METRIC_HEAPSIZ) {
                hitValue = currentHeapSize;
            } else if (condition.getMetric() == TriggeredGlobalProfilingPoint.TriggerCondition.METRIC_HEAPUSG) {
                hitValue = currentHeapUsage;
            } else if (condition.getMetric() == TriggeredGlobalProfilingPoint.TriggerCondition.METRIC_SURVGEN) {
                hitValue = currentSurvGen;
            } else if (condition.getMetric() == TriggeredGlobalProfilingPoint.TriggerCondition.METRIC_LDCLASS) {
                hitValue = currentLoadedClasses;
            }

            tgpp.hit(hitValue);
        } else {
            condition.setTriggered(false);
        }

        if (!condition.isOnetime()) {
            scheduleTriggeredProfilingPoint(tgpp, rescheduledTriggeredPPs);
        }
    }

    private void processTriggeredProfilingPoints() {
        synchronized (this) {
            checkForStop();

            if (isRunning) {
                List<TriggeredGlobalProfilingPoint> rescheduledTriggeredPPs = new LinkedList();

                for (TriggeredGlobalProfilingPoint tgpp : scheduledTriggeredPPs) {
                    if (triggerConditionMet(tgpp.getCondition())) {
                        processTriggeredProfilingPoint(tgpp, rescheduledTriggeredPPs); // Perform PP and eventually reschedule it
                    } else {
                        rescheduledTriggeredPPs.add(tgpp); // reschedule currently not active PP for next check
                    }
                }

                scheduledTriggeredPPs.clear();
                scheduledTriggeredPPs.addAll(rescheduledTriggeredPPs);
            }
        }
    }

    private void reset() {
        gpp = null;
        profiledProject = null;
        profilingSettings = null;
        scheduledTimedPPs.clear();
        scheduledTriggeredPPs.clear();
    }

    private void resetListeners() {
        NetBeansProfiler.getDefaultNB().getVMTelemetryManager().removeDataListener(this);
    }

    private void scheduleProfilingPoint(GlobalProfilingPoint gpp) {
        if (gpp instanceof TimedGlobalProfilingPoint) {
            scheduleTimedProfilingPoint((TimedGlobalProfilingPoint) gpp, scheduledTimedPPs);
        } else if (gpp instanceof TriggeredGlobalProfilingPoint) {
            scheduleTriggeredProfilingPoint((TriggeredGlobalProfilingPoint) gpp, scheduledTriggeredPPs);
        }
    }

    private void scheduleTimedProfilingPoint(TimedGlobalProfilingPoint gpp, List<TimedGlobalProfilingPoint> timedPPs) {
        TimedGlobalProfilingPoint.TimeCondition tc = gpp.getCondition();

        long currentTime = System.currentTimeMillis();
        long scheduledTime = tc.getScheduledTime();
        boolean repeats = tc.getRepeats();

        long periodTime = (long) tc.getPeriodTime();

        switch (tc.getPeriodUnits()) {
            case TimedGlobalProfilingPoint.TimeCondition.UNITS_MINUTES:
                periodTime *= (60 * 1000);

                break;
            case TimedGlobalProfilingPoint.TimeCondition.UNITS_HOURS:
                periodTime *= (60 * 60 * 1000);

                break;
            default:
                break;
        }

        if (scheduledTime < currentTime) {
            if (!repeats) {
                return; // old, won't schedule
            }

            long factor = (long) Math.ceil((double) (currentTime - scheduledTime) / (double) periodTime); // some periods missed, compute first following period
            scheduledTime += (factor * periodTime);
        }

        if (scheduledTime >= currentTime) { // should be always true, forced by the above code
            tc.setScheduledTime(scheduledTime);
            timedPPs.add(gpp);
        }
    }

    private void scheduleTriggeredProfilingPoint(TriggeredGlobalProfilingPoint gpp,
                                                 List<TriggeredGlobalProfilingPoint> triggeredPPs) {
        triggeredPPs.add(gpp);
    }

    // --- Private implementation ------------------------------------------------

    //// - Lifecycle management --------------------------------------------------
    private void start() {
        if ((profiledProject == null) || (gpp == null) || (gpp.length == 0) || !anyProfilingPointsScheduled()) {
            reset();

            return;
        }

        isRunning = true;
        initListeners();
    }

    private void stop() {
        isRunning = false;
        resetListeners();
        reset();
    }

    private boolean timeConditionMet(TimedGlobalProfilingPoint.TimeCondition condition) {
        return condition.getScheduledTime() <= currentTime;
    }

    private boolean triggerConditionMet(TriggeredGlobalProfilingPoint.TriggerCondition condition) {
        if (condition.getMetric() == TriggeredGlobalProfilingPoint.TriggerCondition.METRIC_HEAPSIZ) {
            return condition.isTriggered() ? (condition.getValue() >= currentHeapSize) : (condition.getValue() < currentHeapSize);
        } else if (condition.getMetric() == TriggeredGlobalProfilingPoint.TriggerCondition.METRIC_HEAPUSG) {
            return condition.isTriggered() ? (condition.getValue() >= currentHeapUsage) : (condition.getValue() < currentHeapUsage);
        } else if (condition.getMetric() == TriggeredGlobalProfilingPoint.TriggerCondition.METRIC_SURVGEN) {
            return condition.isTriggered() ? (condition.getValue() >= currentSurvGen) : (condition.getValue() < currentSurvGen);
        } else if (condition.getMetric() == TriggeredGlobalProfilingPoint.TriggerCondition.METRIC_LDCLASS) {
            return condition.isTriggered() ? (condition.getValue() >= currentLoadedClasses)
                                           : (condition.getValue() < currentLoadedClasses);
        } else {
            return false;
        }
    }
}
