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

/*
 * MonitorTestCase.java
 *
 * Created on July 19, 2005, 5:21 PM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */
package org.netbeans.lib.profiler.tests.jfluid.monitor;

import org.netbeans.lib.profiler.ProfilerEngineSettings;
import org.netbeans.lib.profiler.TargetAppRunner;
import org.netbeans.lib.profiler.client.MonitoredData;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.results.monitor.VMTelemetryDataManager;
import org.netbeans.lib.profiler.results.threads.ThreadData;
import org.netbeans.lib.profiler.results.threads.ThreadsDataManager;
import org.netbeans.lib.profiler.tests.jfluid.*;
import org.netbeans.lib.profiler.tests.jfluid.utils.*;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;


/**
 *
 * @author ehucka
 */
public abstract class MonitorTestCase extends CommonProfilerTestCase {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    static final byte ST_UNKNOWN = 1;
    static final byte ST_ZOMBIE = 2;
    static final byte ST_RUNNING = 4;
    static final byte ST_SLEEPING = 8;
    static final byte ST_MONITOR = 16;
    static final byte ST_WAIT = 32;
    static final int MONITOR_ONLY = 0;
    static final int WITH_CPU = 1;
    static final int WITH_MEMORY = 2;
    static final int WITH_CODEREGION = 3;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Creates a new instance of MonitorTestCase
     */
    public MonitorTestCase(String name) {
        super(name);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    protected char getState(byte state) {
        if (state == ST_UNKNOWN) {
            return ('U');
        } else if (state == ST_ZOMBIE) {
            return ('Z');
        } else if (state == ST_RUNNING) {
            return ('R');
        } else if (state == ST_SLEEPING) {
            return ('S');
        } else if (state == ST_MONITOR) {
            return ('M');
        } else if (state == ST_WAIT) {
            return ('W');
        }

        return '-';
    }

    protected String getStates(byte[] states) {
        StringBuffer sb = new StringBuffer(states.length);

        for (int i = 0; i < states.length; i++) {
            sb.append(getState(states[i]));
        }

        return sb.toString();
    }

    protected void detectStates(byte[] states, byte[] detectStates) {
        int detectionindex = 0;

        for (int i = 0; i < states.length; i++) {
            if ((states[i] & detectStates[detectionindex]) == 0) {
                detectionindex++;

                if ((detectionindex >= detectStates.length) || ((states[i] & detectStates[detectionindex]) == 0)) {
                    log("\n*********NOT MATCHING STATES");
                    log(getStates(states));
                    log("Matching states: " + getStates(detectStates));
                    log("Wrong state " + getState(states[i]) + " index " + i);
                    assertTrue("States do not match with pattern", false);
                }
            }
        }
    }

    protected ProfilerEngineSettings initMonitorTest(String projectName, String className) {
        //System.setProperty("org.netbeans.lib.profiler.wireprotocol.WireIO", "true");
        ProfilerEngineSettings settings = initTest(projectName, className, null);
        //defaults
        settings.setThreadCPUTimerOn(false);

        return settings;
    }

    protected void logLongs(long[] longs, int count) {
        double average = 0.0;
        long max = 0;
        long min = Long.MAX_VALUE;

        for (int i = 0; i < count; i++) {
            average += longs[i];

            if (max < longs[i]) {
                max = longs[i];
            }

            if (min > longs[i]) {
                min = longs[i];
            }
        }

        average /= count;
        log("Average = " + average + " Max = " + max + " Min = " + min + " Values = " + count);
    }

    protected void startMonitorTest(ProfilerEngineSettings settings, int times, long delay, String[] detects,
                                    byte[][] detectstates, int profilingType) {
        TargetAppRunner runner = new TargetAppRunner(settings, new TestProfilerAppHandler(this),
                                                     new TestProfilingPointsProcessor());
        runner.addProfilingEventListener(Utils.createProfilingListener(this));

        try {
            runner.readSavedCalibrationData();

            Process p = startTargetVM(runner);
            assertNotNull("Target JVM is not started", p);
            bindStreams(p);

            runner.connectToStartedVMAndStartTA();

            if (profilingType == WITH_CPU) {
                runner.getProfilerClient().initiateRecursiveCPUProfInstrumentation(settings.getInstrumentationRootMethods());
            } else if (profilingType == WITH_MEMORY) {
                runner.getProfilerClient().initiateMemoryProfInstrumentation(CommonConstants.INSTR_OBJECT_LIVENESS);
            } else if (profilingType == WITH_CODEREGION) {
                runner.getProfilerClient().initiateCodeRegionInstrumentation(settings.getInstrumentationRootMethods());
            }
            assert runner.targetAppIsRunning();
            waitForStatus(STATUS_RUNNING);

            VMTelemetryDataManager dataManager = new VMTelemetryDataManager();
            dataManager.setArrayBufferSize(10);

            ThreadsDataManager threadsManager = new ThreadsDataManager();

            //run monitoring - data are stored into states map
            for (int cntr = 0; cntr < times; cntr++) {
                Thread.sleep(delay);

                if (isStatus(STATUS_APP_FINISHED)) {
                    break;
                }

                if (!runner.targetJVMIsAlive()) {
                    break;
                }

                runner.getProfilerClient().forceObtainedResultsDump();

                MonitoredData data = runner.getProfilerClient().getMonitoredData();

                dataManager.processData(data);
                threadsManager.processData(data);
            }

            setStatus(STATUS_MEASURED);

            //detect stored data - sign defined state and OR them from all matching threads
            HashMap threads = new HashMap(32);
            HashMap timestamps = new HashMap(32);

            assertTrue("Threads manager has not data", threadsManager.hasData());

            int statesNumber = 128;
            long deltat = threadsManager.getEndTime() - threadsManager.getStartTime();
            double tick = (double) deltat / (double) statesNumber;
            ArrayList names = new ArrayList();

            for (int i = 0; i < threadsManager.getThreadsCount(); i++) {
                ThreadData td = threadsManager.getThreadData(i);

                if (!td.getName().equals("process reaper") && !td.getName().equals("DestroyJavaVM")) { //disable system threads

                    byte[] states = new byte[statesNumber];
                    String n = td.getName() + ", class: " + td.getClassName();
                    byte state = ST_UNKNOWN;
                    long time = threadsManager.getStartTime();
                    int tdindex = 0;

                    for (int j = 0; j < states.length; j++) {
                        if ((tdindex < td.size()) && (time >= td.getTimeStampAt(tdindex))) {
                            state = toBinState(td.getStateAt(tdindex));

                            Color color = td.getThreadStateColorAt(tdindex);
                            assertNotNull("Threads state color is null", color);
                            tdindex++;
                        }

                        states[j] = state;
                        time = (long) ((j * tick) + threadsManager.getStartTime());
                    }

                    td.clearStates();
                    assertTrue("Error in threadData.clearStates", (td.size() == 0));
                    names.add(td.getName());
                    threads.put(n, states);
                    timestamps.put(n, new Long(td.getFirstTimeStamp()));
                }
            }

            String[] keys = (String[]) threads.keySet().toArray(new String[threads.size()]);
            Arrays.sort(keys);
            Collections.sort(names);

            for (int i = 0; i < names.size(); i++) {
                ref(names.get(i));
            }

            boolean[] ret = null;
            int maxindex = 0;

            for (int i = 0; i < keys.length; i++) {
                byte[] sts = (byte[]) (threads.get(keys[i]));
                log(keys[i]);
                log(getStates(sts));

                for (int j = 0; j < detects.length; j++) {
                    if (keys[i].startsWith(detects[j])) {
                        detectStates(sts, detectstates[j]);
                    }
                }
            }

            assertEquals("Some threads are multiple - issue 68266", names.size(), keys.length);
            //log datas
            log("\nDataManager item counts " + dataManager.getItemCount());
            log("Free Memory");
            logLongs(dataManager.freeMemory, dataManager.getItemCount());
            log("lastGCPauseInMS");
            logLongs(dataManager.lastGCPauseInMS, dataManager.getItemCount());
            log("nSurvivingGenerations");
            logLongs(dataManager.nSurvivingGenerations, dataManager.getItemCount());
            log("nSystemThreads");
            logLongs(dataManager.nSystemThreads, dataManager.getItemCount());
            log("nTotalThreads");
            logLongs(dataManager.nTotalThreads, dataManager.getItemCount());
            log("nUserThreads");
            logLongs(dataManager.nUserThreads, dataManager.getItemCount());
            log("relativeGCTimeInPerMil");
            logLongs(dataManager.relativeGCTimeInPerMil, dataManager.getItemCount());
            log("timeStamps");
            logLongs(dataManager.timeStamps, dataManager.getItemCount());
            log("totalMemory");
            logLongs(dataManager.totalMemory, dataManager.getItemCount());
            log("usedMemory");
            logLongs(dataManager.usedMemory, dataManager.getItemCount());
        } catch (Exception ex) {
            log(ex);
            assertTrue("Exception thrown: " + ex.getMessage(), false);
        } finally {
            finalizeTest(runner);
        }
    }

    protected byte toBinState(byte state) {
        if (state == CommonConstants.THREAD_STATUS_UNKNOWN) {
            return ST_UNKNOWN;
        } else if (state == CommonConstants.THREAD_STATUS_ZOMBIE) {
            return ST_ZOMBIE;
        } else if (state == CommonConstants.THREAD_STATUS_RUNNING) {
            return ST_RUNNING;
        } else if (state == CommonConstants.THREAD_STATUS_SLEEPING) {
            return ST_SLEEPING;
        } else if (state == CommonConstants.THREAD_STATUS_MONITOR) {
            return ST_MONITOR;
        } else if (state == CommonConstants.THREAD_STATUS_WAIT) {
            return ST_WAIT;
        }

        return 0;
    }
}
