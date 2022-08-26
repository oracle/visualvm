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
 * MeasureDiffsTestCase.java
 *
 * Created on July 19, 2005, 5:20 PM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */
package org.graalvm.visualvm.lib.jfluid.tests.jfluid.others;

import org.graalvm.visualvm.lib.jfluid.ProfilerEngineSettings;
import org.graalvm.visualvm.lib.jfluid.TargetAppRunner;
import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;
import org.graalvm.visualvm.lib.jfluid.results.ProfilingResultsDispatcher;
import org.graalvm.visualvm.lib.jfluid.results.RuntimeCCTNode;
import org.graalvm.visualvm.lib.jfluid.results.cpu.CPUCCTProvider;
import org.graalvm.visualvm.lib.jfluid.results.cpu.CPUCallGraphBuilder;
import org.graalvm.visualvm.lib.jfluid.results.cpu.FlatProfileBuilder;
import org.graalvm.visualvm.lib.jfluid.results.cpu.FlatProfileContainer;
import org.graalvm.visualvm.lib.jfluid.results.cpu.FlatProfileContainerFree;
import org.graalvm.visualvm.lib.jfluid.tests.jfluid.*;
import org.graalvm.visualvm.lib.jfluid.tests.jfluid.utils.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;


/**
 *
 * @author ehucka
 */
public abstract class MeasureDiffsTestCase extends CommonProfilerTestCase {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    class Results {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        double ideal;
        double measalone;
        double measprofiler;
        double profiled;

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void setIdeal(String val) {
            try {
                ideal = Double.parseDouble(val);
            } catch (NumberFormatException ex) {
            }
        }

        public void setMeasuredAlone(String val) {
            try {
                measalone = Double.parseDouble(val);
            } catch (NumberFormatException ex) {
            }
        }

        public void setMeasuredProfiled(String val) {
            try {
                measprofiler = Double.parseDouble(val);
            } catch (NumberFormatException ex) {
            }
        }

        public void setProfiled(double val) {
            profiled = val;
        }

        public String toString() {
            return String.valueOf(ideal) + ";" + String.valueOf(measalone) + ";" + String.valueOf(measprofiler) + ";"
                   + String.valueOf(profiled);
        }
    }

    private static class CPUResultListener implements CPUCCTProvider.Listener {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private final Object resultsLock = new Object();
        private boolean hasResults = false;

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void cctEstablished(RuntimeCCTNode appRootNode) {
            synchronized (resultsLock) {
                hasResults = true;
                resultsLock.notify();
            }
        }

        public void cctReset() {
            synchronized (resultsLock) {
                hasResults = false;
                resultsLock.notify();
            }
        }

        public boolean wait4results(long timeout) {
            synchronized (resultsLock) {
                if (!hasResults) {
                    try {
                        resultsLock.wait(timeout);
                    } catch (InterruptedException e) {
                    }
                }

                return hasResults;
            }
        }

        public void cctEstablished(RuntimeCCTNode appRootNode, boolean empty) {
            if (!empty) {
                cctEstablished(appRootNode);
            }
            //throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    static int ALL_INV_ERROR_METHOD = 0;
    static int LAST_INV_ERROR_METHOD = 1;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    File outFile;
    private CPUCallGraphBuilder builder;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Creates a new instance of MeasureDiffsTestCase
     */
    public MeasureDiffsTestCase(String name) {
        super(name);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void startAppAlone(ProfilerEngineSettings settings) {
        ArrayList commands = new ArrayList(10);
        commands.add(settings.getTargetJVMExeFile());
        commands.add("-classpath");
        commands.add(settings.getMainClassPath());
        commands.add(settings.getMainClassName());
        commands.add(outFile.getAbsolutePath());

        String[] cmds = (String[]) commands.toArray(new String[0]);

        try {
            Runtime.getRuntime().exec(cmds, null, new File(settings.getWorkingDir())).waitFor();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    protected ProfilerEngineSettings initCpuTest(String projectName, String mainClass) {
        ProfilerEngineSettings settings = initTest(projectName, mainClass, null);
        //defaults
        settings.setCPUProfilingType(CommonConstants.CPU_INSTR_FULL);
        settings.setInstrScheme(CommonConstants.INSTRSCHEME_TOTAL);
        settings.setInstrumentEmptyMethods(false);
        settings.setInstrumentGetterSetterMethods(false);
        settings.setInstrumentMethodInvoke(true);
        settings.setInstrumentSpawnedThreads(true);
        settings.setExcludeWaitTime(true);

        //addJVMArgs(settings, "-Dorg.graalvm.visualvm.lib.jfluid.wireprotocol.WireIO=true");
        settings.setThreadCPUTimerOn(false);

        return settings;
    }

    protected ProfilerEngineSettings initCpuTest(String projectName, String mainClass, String[][] rootMethods) {
        ProfilerEngineSettings settings = initTest(projectName, mainClass, rootMethods);
        //defaults
        settings.setCPUProfilingType(CommonConstants.CPU_INSTR_FULL);
        settings.setInstrScheme(CommonConstants.INSTRSCHEME_LAZY);
        settings.setInstrumentEmptyMethods(false);
        settings.setInstrumentGetterSetterMethods(false);
        settings.setInstrumentMethodInvoke(true);
        settings.setInstrumentSpawnedThreads(false);
        settings.setExcludeWaitTime(true);

        //addJVMArgs(settings, "-Dorg.graalvm.visualvm.lib.jfluid.wireprotocol.WireIO=true");
        settings.setThreadCPUTimerOn(false);

        return settings;
    }

    protected void readIdealTimes(HashMap results) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(outFile));
            String line;
            boolean first = true;

            while ((line = br.readLine()) != null) {
                if (first) {
                    first = false;
                } else {
                    StringTokenizer st = new StringTokenizer(line, ";");
                    String met = st.nextToken();
                    Results res = new Results();
                    res.setIdeal(st.nextToken());
                    res.setMeasuredAlone(st.nextToken());
                    results.put(met, res);
                }
            }

            br.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    protected void readProfiledTimes(HashMap results) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(outFile));
            String line;
            boolean first = true;

            while ((line = br.readLine()) != null) {
                if (first) {
                    first = false;
                } else {
                    StringTokenizer st = new StringTokenizer(line, ";");
                    String met = st.nextToken();
                    st.nextToken(); //skip ideal

                    Results res = (Results) (results.get(met));

                    if (res != null) {
                        res.setMeasuredProfiled(st.nextToken());
                    }
                }
            }

            br.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    protected void refCPUResults(FlatProfileContainer fpc, String[] methodsOrder, HashMap results) {
        for (int row = 0; row < fpc.getNRows(); row++) {
            for (String methodsOrder1 : methodsOrder) {
                if (fpc.getMethodNameAtRow(row).startsWith(methodsOrder1)) {
                    double tm = fpc.getTimeInMcs0AtRow(row) / (fpc.getNInvocationsAtRow(row) * 1000.0);
                    String name = fpc.getMethodNameAtRow(row);

                    if (name.indexOf('.') > -1) {
                        name = name.substring(name.lastIndexOf('.') + 1);
                    }

                    name = name.substring(0, name.indexOf('('));

                    Results res = (Results) (results.get(name));

                    if (res != null) {
                        res.setProfiled(tm);
                    }
                }
            }
        }
    }

    protected void startCPUTest(ProfilerEngineSettings settings, String[] methodsOrder) {
        HashMap results = new HashMap(64);
        builder = new CPUCallGraphBuilder();

        //get results with alone run
        try {
            outFile = File.createTempFile("profiler", "test", getWorkDir());
            startAppAlone(settings);
            System.err.println(">>> startAppAlone");
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        readIdealTimes(results);

        settings.setMainArgs(outFile.getAbsolutePath());

        //create runner
        TargetAppRunner runner = new TargetAppRunner(settings, new TestProfilerAppHandler(this),
                                                     new TestProfilingPointsProcessor());
        runner.addProfilingEventListener(Utils.createProfilingListener(this));

        ProfilingResultsDispatcher.getDefault().addListener(builder);

        CPUResultListener resultListener = new CPUResultListener();
        builder.addListener(resultListener);

        FlatProfileBuilder flattener = new FlatProfileBuilder();
        builder.addListener(flattener);
        flattener.setContext(runner.getProfilerClient(),null,null);
        
        builder.startup(runner.getProfilerClient());

        try {
            runner.readSavedCalibrationData();
            runner.getProfilerClient().initiateRecursiveCPUProfInstrumentation(settings.getInstrumentationRootMethods());

            Process p = startTargetVM(runner);
            assertNotNull("Target JVM is not started", p);
            bindStreams(p);
            runner.attachToTargetVMOnStartup();

            waitForStatus(STATUS_RUNNING);
            assertTrue("runner is not running", runner.targetAppIsRunning());

            ArrayList methods = new ArrayList();

            waitForStatus(STATUS_APP_FINISHED);

            if (runner.targetJVMIsAlive()) {
                log("Get results: " + System.currentTimeMillis());
                assertTrue("Results do not exist - issue 65185.", runner.getProfilerClient().cpuResultsExist());

                runner.getProfilerClient().forceObtainedResultsDump();

                boolean gotResults = resultListener.wait4results(10000);
                assertTrue("Results are not available after 10 seconds.", gotResults);
                log("obtaining results " + String.valueOf(System.currentTimeMillis()));

                //logInstrumented(runner);
                FlatProfileContainerFree fpc = (FlatProfileContainerFree) flattener.createFlatProfile();
                fpc.filterOriginalData(new String[0], 0, 0.0D);
                refCPUResults(fpc, methodsOrder, results);
            }

            setStatus(STATUS_MEASURED);
            readProfiledTimes(results);
            writeResults(results);
        } catch (Exception ex) {
            log(ex);
            assertTrue("Exception thrown: " + ex.getMessage(), false);
        } finally {
            finalizeTest(runner);
        }
    }

    protected void writeResults(HashMap results) {
        Object[] keys = results.keySet().toArray();
        Arrays.sort(keys);
        log("\nMethod Name;Ideal Time;Measured Time;Measured during profiling;Measured by profiler");

        for (Object key : keys) {
            Results res = (Results) (results.get(key));
            log(key + ";" + res.toString());
        }

        log("\n");
    }
}
