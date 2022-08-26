/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
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
package org.graalvm.visualvm.lib.jfluid.tests.jfluid.monitor;

import org.graalvm.visualvm.lib.jfluid.ProfilerEngineSettings;
import org.graalvm.visualvm.lib.jfluid.TargetAppRunner;
import org.graalvm.visualvm.lib.jfluid.client.MonitoredData;
import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;
import org.graalvm.visualvm.lib.jfluid.results.monitor.VMTelemetryDataManager;
import org.graalvm.visualvm.lib.jfluid.results.threads.ThreadData;
import org.graalvm.visualvm.lib.jfluid.results.threads.ThreadsDataManager;
import org.graalvm.visualvm.lib.jfluid.tests.jfluid.*;
import org.graalvm.visualvm.lib.jfluid.tests.jfluid.utils.*;
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
    static final byte ST_PARK = 64;
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
        } else if (state == ST_PARK) {
            return ('P');
        }

        return '-';
    }

    protected String getStates(byte[] states) {
        StringBuilder sb = new StringBuilder(states.length);

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
        //System.setProperty("org.graalvm.visualvm.lib.jfluid.wireprotocol.WireIO", "true");
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

            String[] keys = (String[]) threads.keySet().toArray(new String[0]);
            Arrays.sort(keys);
            Collections.sort(names);

            for (int i = 0; i < names.size(); i++) {
                ref(names.get(i));
            }

            boolean[] ret = null;
            int maxindex = 0;

            for (String key : keys) {
                byte[] sts = (byte[]) (threads.get(key));
                log(key);
                log(getStates(sts));
                for (int j = 0; j < detects.length; j++) {
                    if (key.startsWith(detects[j])) {
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
        } else if (state == CommonConstants.THREAD_STATUS_PARK) {
            return ST_PARK;
        }

        return 0;
    }
}
