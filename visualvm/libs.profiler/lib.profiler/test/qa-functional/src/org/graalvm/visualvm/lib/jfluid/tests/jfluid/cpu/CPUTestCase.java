/*
 * Copyright (c) 1997, 2022, Oracle and/or its affiliates. All rights reserved.
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
 * CPUTestCase.java
 *
 * Created on July 19, 2005, 5:20 PM
 */
package org.graalvm.visualvm.lib.jfluid.tests.jfluid.cpu;

import org.graalvm.visualvm.lib.jfluid.ProfilerEngineSettings;
import org.graalvm.visualvm.lib.jfluid.TargetAppRunner;
import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;
import org.graalvm.visualvm.lib.jfluid.results.EventBufferResultsProvider;
import org.graalvm.visualvm.lib.jfluid.results.ProfilingResultsDispatcher;
import org.graalvm.visualvm.lib.jfluid.results.RuntimeCCTNode;
import org.graalvm.visualvm.lib.jfluid.results.cpu.CPUCCTProvider;
import org.graalvm.visualvm.lib.jfluid.results.cpu.CPUCallGraphBuilder;
import org.graalvm.visualvm.lib.jfluid.results.cpu.CPUResultsSnapshot;
import org.graalvm.visualvm.lib.jfluid.results.cpu.FlatProfileBuilder;
import org.graalvm.visualvm.lib.jfluid.results.cpu.FlatProfileContainer;
import org.graalvm.visualvm.lib.jfluid.results.cpu.FlatProfileContainerFree;
import org.graalvm.visualvm.lib.jfluid.tests.jfluid.*;
import org.graalvm.visualvm.lib.jfluid.tests.jfluid.utils.*;
import org.graalvm.visualvm.lib.jfluid.utils.StringUtils;
import java.text.NumberFormat;
import java.util.*;


/**
 *
 * @author ehucka
 */
public abstract class CPUTestCase extends CommonProfilerTestCase {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    class Measured {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        public int invocations = 0;
        public long time = 0;

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void setInvocations(int invc) {
            this.invocations = invc;
        }

        public void setTime(long time) {
            this.time = time;
        }
    }

    private class CPUResultListener implements CPUCCTProvider.Listener {
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
                log("cctReset "+System.currentTimeMillis());
                resultsLock.notify();
            }
        }

        public boolean wait4results(long timeout) {
            synchronized (resultsLock) {
                if (!hasResults) {
                    try {
                        log("wait4results "+System.currentTimeMillis());
                        resultsLock.wait(timeout);
                    } catch (InterruptedException e) {
                    }
                }

                return hasResults;
            }
        }

        public void cctEstablished(RuntimeCCTNode appRootNode, boolean empty) {
            log("cctEstablished "+empty+" "+System.currentTimeMillis());
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

    NumberFormat percentFormat;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Creates a new instance of CPUTestCase
     */
    public CPUTestCase(String name) {
        super(name);
        percentFormat = NumberFormat.getPercentInstance();
        percentFormat.setMaximumFractionDigits(1);
        percentFormat.setMinimumFractionDigits(0);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public int getALL_INV_ERROR_METHOD() {
        return ALL_INV_ERROR_METHOD;
    }

    protected double getError(int invocations, long mctime, long idealtime) {
        double ideal = idealtime * invocations * 1000.0;

        return Math.abs(ideal - mctime) / 1000;
    }

    protected void checkCPUResults(FlatProfileContainer fpc, HashMap methods, String[] measuredMethodsFilter) {
        double percent = 0.0;

        for (int row = 0; row < fpc.getNRows(); row++) {
            percent += fpc.getPercentAtRow(row);

            for (String measuredMethodsFilter1 : measuredMethodsFilter) {
                if (fpc.getMethodNameAtRow(row).startsWith(measuredMethodsFilter1)) {
                    Measured m = (Measured) (methods.get(fpc.getMethodNameAtRow(row)));

                    if (m == null) {
                        m = new Measured();
                        m.time = fpc.getTimeInMcs0AtRow(row);
                        m.invocations = fpc.getNInvocationsAtRow(row);
                        methods.put(fpc.getMethodNameAtRow(row), m);
                    } else {
                        long tm = m.time;
                        int inv = m.invocations;
                        m.setTime(fpc.getTimeInMcs0AtRow(row));
                        m.setInvocations(fpc.getNInvocationsAtRow(row));

                        if ((tm > m.time) || (inv > m.invocations)) {
                            log("\n!!!Decreasing values: method " + fpc.getMethodNameAtRow(row) + " current time " + m.time
                                    + " invocations " + m.invocations + " but was time=" + tm + " invocations=" + inv + "\n");
                            assertFalse("Unacceptable results - decresing values (issue 65187)", true);
                        }
                    }
                }
            }
        }

        if (Math.abs(percent - 100.0) > 0.1) {
            log("\n!!!Sum of percents is not 100% - " + percent + "\n");

            for (int row = 0; row < fpc.getNRows(); row++) {
                log(fpc.getMethodIdAtRow(row) + " " + percentFormat.format(fpc.getPercentAtRow(row) / 100) + " %");
            }

            assertFalse("Unacceptable results - sum of percents != 100", true);
        }
    }

    protected void checkCPUResults(FlatProfileContainer fpc, String[] methodsNames, long[] idealTimes, double diffMillis,
                                   String[] refMethods, ArrayList refMethodsList, int errorMethod) {
        double[] errors = new double[methodsNames.length];
        int[] nInv = new int[methodsNames.length];
        long[] times = new long[methodsNames.length];

        for (int row = 0; row < fpc.getNRows(); row++) {
            for (int mets = 0; mets < methodsNames.length; mets++) {
                if (fpc.getMethodNameAtRow(row).equals(methodsNames[mets])) {
                    nInv[mets] = fpc.getNInvocationsAtRow(row);
                    times[mets] = fpc.getTimeInMcs0AtRow(row);
                    errors[mets] = getError(nInv[mets], times[mets], idealTimes[mets]);
                }
            }

            if (refMethods != null) {
                for (String refMethod : refMethods) {
                    String mname = fpc.getMethodNameAtRow(row);
                    if (mname.startsWith(refMethod) && !refMethodsList.contains(mname)) {
                        refMethodsList.add(mname);
                    }
                }
            }
        }

        double best = diffMillis / 4.0;
        int bestcount = 0;
        boolean bigdifference = false;

        for (int cntr = 0; cntr < errors.length; cntr++) {
            if (errors[cntr] <= best) {
                bestcount++;
            }

            bigdifference |= (errors[cntr] > diffMillis);
        }

        boolean accepted = !bigdifference || ((bestcount * 1.0) >= (errors.length * 0.5));
        logFractions(errors, nInv, times, idealTimes, methodsNames);
        log("");

        if (!accepted) {
            log("\nRESULTS WITH BIG DIFFERENCES - differences are greater than given tolerance: " + diffMillis + " ms");
            log("Best count " + bestcount + " errors.length " + errors.length);
        }

        //assertTrue("Not acceptable results - big differences", accepted);
    }

    protected ProfilerEngineSettings initCpuTest(String projectName, String mainClass) {
        return initCpuTest(projectName, mainClass, null);
    }

    protected ProfilerEngineSettings initCpuTest(String projectName, String mainClass, String[][] rootMethods) {
        //System.setProperty("org.graalvm.visualvm.lib.jfluid.wireprotocol.WireIO", "true");
        ProfilerEngineSettings settings = initTest(projectName, mainClass, rootMethods);
        //defaults
        settings.setCPUProfilingType(CommonConstants.CPU_INSTR_FULL);
        settings.setInstrScheme(CommonConstants.INSTRSCHEME_LAZY);
        settings.setInstrumentEmptyMethods(false);
        settings.setInstrumentGetterSetterMethods(false);
        settings.setInstrumentMethodInvoke(true);
        settings.setInstrumentSpawnedThreads(rootMethods == null);
        settings.setExcludeWaitTime(true);

        //        addJVMArgs(settings, "-Dorg.graalvm.visualvm.lib.jfluid.wireprotocol.WireIO=true");
        //addJVMArgs(settings, "-Dorg.graalvm.visualvm.lib.jfluid.server.ProfilerServer=true");
        //        if (rootMethods == null) {
        //            addJVMArgs(settings, "-Dorg.graalvm.visualvm.lib.jfluid.server.ProfilerServer=true");
        //        }
        settings.setThreadCPUTimerOn(false);

        return settings;
    }

    protected void logFractions(double[] errors, int[] inv, long[] times, long[] ideals, String[] methods) {
        log(complete("Error[ms]", 10) + complete("Invocs", 10) + complete("Time[ms]", 10) + complete("Ideal[ms]", 10) + "Method");

        for (int i = 0; i < errors.length; i++) {
            log(complete(String.valueOf(errors[i]), 9) + " " + complete(String.valueOf(inv[i]), 9) + " "
                + complete(StringUtils.mcsTimeToString(times[i]), 9) + " " + complete(String.valueOf(ideals[i] * inv[i]), 9)
                + " " + methods[i]);
        }
    }

    protected void logInstrumented(TargetAppRunner runner)
                            throws Exception {
        CPUResultsSnapshot snapshot = runner.getProfilerClient().getCPUProfilingResultsSnapshot();
        String[] mets = snapshot.getInstrMethodNames();
        log("Instrumented methods:");

        for (String met : mets) {
            log(met);
        }
    }

    /**
     * checks results after the profiled app is finished
     */
    protected void startCPUTest(ProfilerEngineSettings settings, String[] measuredMethods, long[] idealTimes, double diffMillis,
                                String[] displayMethodsFilter, int errorMethod) {
        CPUCallGraphBuilder builder = new CPUCallGraphBuilder();

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

            Thread.sleep(1000);

            if (runner.targetJVMIsAlive()) {
                log("Get results: " + System.currentTimeMillis());
                assertTrue("Results do not exist - issue 65185.", runner.getProfilerClient().cpuResultsExist());

                boolean gotResults = false;
                int retryCounter = 8; // was - 4

                do {
                    // just wait for the results to appear - forceObtainedResultsDump() has been alread called by ProfilerClient on shutdown
                    //                    runner.getProfilerClient().forceObtainedResultsDump();
                    gotResults = resultListener.wait4results(2500);
                } while (!gotResults && (--retryCounter > 0));

                assertTrue("Results are not available after 20 seconds.", gotResults); // was - 10 seconds
                log("obtaining results " + String.valueOf(System.currentTimeMillis()));

                //logInstrumented(runner);
                FlatProfileContainerFree fpc = null;
                int retry = 5;

                while ((fpc == null) && (--retry > 0)) {
                    fpc = (FlatProfileContainerFree) flattener.createFlatProfile();
                    Thread.sleep(500);
                }

                fpc.filterOriginalData(new String[] { "" }, CommonConstants.FILTER_CONTAINS, 0.0D);
                checkCPUResults(fpc, measuredMethods, idealTimes, diffMillis, displayMethodsFilter, methods, errorMethod);
            }

            setStatus(STATUS_MEASURED);

            if (!methods.isEmpty()) {
                Collections.sort(methods);

                for (int mets = 0; mets < methods.size(); mets++) {
                    ref(methods.get(mets));
                }
            }
        } catch (Exception ex) {
            log(ex);
            assertTrue("Exception thrown: " + ex.getMessage(), false);
        } finally {
            ProfilingResultsDispatcher.getDefault().pause(true);
            builder.shutdown();
            flattener.setContext(null,null,null);
            builder.removeListener(flattener);
            builder.removeListener(resultListener);
            ProfilingResultsDispatcher.getDefault().removeListener(builder);
            finalizeTest(runner);
        }
    }

    /**
     * check reulsts periodicaly - live results
     */
    protected void startCPUTest(ProfilerEngineSettings settings, String[] measuredMethodsFilter, long checkDelay, long maxDelay) {
        CPUCallGraphBuilder builder = new CPUCallGraphBuilder();

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
            waitForStatus(STATUS_RESULTS_AVAILABLE | STATUS_APP_FINISHED);
            assertTrue("ResultsAvailable was not called - issue 69084", (isStatus(STATUS_RESULTS_AVAILABLE) || isStatus(STATUS_LIVERESULTS_AVAILABLE)));

            HashMap methods = new HashMap(128);
            long alltime = 0;
            long time = System.currentTimeMillis();
            long oldtime = time - checkDelay;

            while (!isStatus(STATUS_APP_FINISHED) && !isStatus(STATUS_ERROR) && (alltime < maxDelay)) {
                if ((time - oldtime) < (2 * checkDelay)) {
                    Thread.sleep((2 * checkDelay) - (time - oldtime));
                }

                if (!isStatus(STATUS_LIVERESULTS_AVAILABLE)) {
                    waitForStatus(STATUS_LIVERESULTS_AVAILABLE, checkDelay / 2);
                }

                if (runner.targetJVMIsAlive() && isStatus(STATUS_LIVERESULTS_AVAILABLE)) {
                    assertTrue("Results do not exist - issue 65185.", runner.getProfilerClient().cpuResultsExist());
                    log("Get Results: " + System.currentTimeMillis());

                    //                    runner.getProfilerClient().forceObtainedResultsDump();
                    //                    assertTrue("Results do not exist on the server - issue 65185.", runner.getProfilerClient().cpuResultsExist());
                    boolean gotResults = false;
                    int retryCounter = 4;

                    do {
                        runner.getProfilerClient().forceObtainedResultsDump();
                        gotResults = resultListener.wait4results(2500);
                    } while (!gotResults && (--retryCounter > 0));

                    assertTrue("CallGraphBuilder: Results do not exist.", gotResults);
                    log("Results obtained " + String.valueOf(System.currentTimeMillis()));

                    //logInstrumented(runner);
                    FlatProfileContainerFree fpc = null;
                    int retry = 5;

                    while ((fpc == null) && (--retry > 0)) {
                        fpc = (FlatProfileContainerFree) flattener.createFlatProfile();
                        Thread.sleep(500);
                    }

                    fpc.filterOriginalData(new String[] { "" }, CommonConstants.FILTER_CONTAINS, 0.0D);
                    fpc.sortBy(FlatProfileContainer.SORT_BY_TIME, true);
                    checkCPUResults(fpc, methods, measuredMethodsFilter);
                }

                alltime += (System.currentTimeMillis() - time);
                oldtime = time;
                time = System.currentTimeMillis();
            }

            if (methods.isEmpty()) {
                assertTrue("Results were not on the server - issue 65185", false);
            }
        } catch (Exception ex) {
            log(ex);
            assertTrue("Exception thrown: " + ex.getMessage(), false);
        } finally {
            ProfilingResultsDispatcher.getDefault().pause(true);
            builder.shutdown();
            flattener.setContext(null,null,null);
            builder.removeListener(flattener);
            builder.removeListener(resultListener);
            ProfilingResultsDispatcher.getDefault().removeListener(builder);
            finalizeTest(runner);
        }
    }
}
