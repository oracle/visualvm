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
 * MemoryTestCase.java
 *
 * Created on July 19, 2005, 5:21 PM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */
package org.graalvm.visualvm.lib.jfluid.tests.jfluid.memory;

import org.graalvm.visualvm.lib.jfluid.ProfilerEngineSettings;
import org.graalvm.visualvm.lib.jfluid.TargetAppRunner;
import org.graalvm.visualvm.lib.jfluid.results.EventBufferResultsProvider;
import org.graalvm.visualvm.lib.jfluid.results.ProfilingResultsDispatcher;
import org.graalvm.visualvm.lib.jfluid.results.RuntimeCCTNode;
import org.graalvm.visualvm.lib.jfluid.results.memory.MemoryCCTProvider;
import org.graalvm.visualvm.lib.jfluid.results.memory.MemoryCallGraphBuilder;
import org.graalvm.visualvm.lib.jfluid.tests.jfluid.*;
import org.graalvm.visualvm.lib.jfluid.tests.jfluid.utils.*;
import org.graalvm.visualvm.lib.jfluid.utils.StringUtils;
import java.util.ArrayList;
import java.util.Collections;
import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;

/**
 *
 * @author ehucka
 */
public abstract class MemoryTestCase extends CommonProfilerTestCase {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private static class MemoryResultListener implements MemoryCCTProvider.Listener {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private final Object resultsLock = new Object();
        private boolean hasResults = false;

        //~ Methods --------------------------------------------------------------------------------------------------------------
        public void cctEstablished(RuntimeCCTNode appRootNode) {
            System.out.println("Memory CCT Established");

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

    //~ Instance fields ----------------------------------------------------------------------------------------------------------
    MemoryCallGraphBuilder builder = new MemoryCallGraphBuilder();
    MemoryResultListener resultListener = null;

    //~ Constructors -------------------------------------------------------------------------------------------------------------
    /**
     * Creates a new instance of MemoryTestCase
     */
    public MemoryTestCase(String name) {
        super(name);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------
    protected void checkMemoryResults(TargetAppRunner targetAppRunner, String[] matchingPrefixes,
            MemoryResultListener resultListener, int instrMode)
            throws Exception {
        boolean gotResults = false;
        int retryCounter = 4;

        do {
            targetAppRunner.getProfilerClient().forceObtainedResultsDump();
            gotResults = resultListener.wait4results(2500);
        } while (!gotResults && (--retryCounter > 0));

        assertTrue("CallGraphBuilder: Results do not exist.", gotResults);
        log("results obtained: " + System.currentTimeMillis());

        int[] totalAllocObjects = targetAppRunner.getProfilerClient().getAllocatedObjectsCountResults();
        String[] classnames = targetAppRunner.getProfilerClient().getStatus().getClassNames();
        long[] totalAllocObjectsSize = null;
        ArrayList list = new ArrayList(128);
        totalAllocObjectsSize = builder.getAllocObjectNumbers();
        
        if (instrMode == CommonConstants.INSTR_OBJECT_ALLOCATIONS) {
            for (int i = 0; i < totalAllocObjectsSize.length; i++) {
                boolean match = false;

                for (int j = 0; j < matchingPrefixes.length; j++) {
                    if (classnames[i].startsWith(matchingPrefixes[j])) {
                        match = true;

                        break;
                    }
                }

                if (match) {
                    StringBuilder out = new StringBuilder();
                    out.append(complete(StringUtils.userFormClassName(classnames[i]), 32));
                    out.append(complete(String.valueOf(totalAllocObjects[i]), 8));
                    //out.append(complete(StringUtils.nBytesToString(totalAllocObjectsSize[i]), 10));
                    list.add(out.toString());
                }
            }

            ref(complete("Name", 32) + complete("DCount", 8)); //+complete("DSize", 10));
        }

        if (instrMode == CommonConstants.INSTR_OBJECT_LIVENESS) {
            try {
                MemoryCCTProvider.ObjectNumbersContainer liveness = builder.getLivenessObjectNumbers();

                totalAllocObjectsSize = liveness.trackedLiveObjectsSize;

                float[] avgage = liveness.avgObjectAge;
                int[] maxSurvGen = liveness.maxSurvGen;
                long[] ntrackedallocobjects = liveness.nTrackedAllocObjects;
                int[] ntrackedliveobjects = liveness.nTrackedLiveObjects;

                for (int i = 0; i < totalAllocObjectsSize.length; i++) {
                    boolean match = false;

                    for (int j = 0; j < matchingPrefixes.length; j++) {
                        if (classnames[i].startsWith(matchingPrefixes[j])) {
                            match = true;

                            break;
                        }
                    }

                    if (match) {
                        StringBuilder out = new StringBuilder();
                        out.append(complete(StringUtils.userFormClassName(classnames[i]), 32));
                        //out.append(complete(StringUtils.nBytesToString(totalAllocObjectsSize[i]), 10));
                        out.append(complete(String.valueOf(ntrackedliveobjects[i]), 10));
                        out.append(complete(String.valueOf(ntrackedallocobjects[i]), 8));
                        //out.append(complete(String.valueOf((int)avgage[i]), 8));
                        //out.append(complete(String.valueOf(maxSurvGen[i]), 8));
                        list.add(out.toString());
                    }
                }

                ref(complete("Name", 32) /*complete("LiveBytes", 10)+*/ + complete("LiveObjs", 10) + complete("Allocs", 8)); //+complete("AvgAge", 8)+complete("MaxSurv", 8));
            } catch (IllegalStateException e) {
            }
        }
        //log results
        Collections.sort(list);

        for (int i = 0; i < list.size(); i++) {
            ref(list.get(i));
        }

        ref("");
    }

    protected ProfilerEngineSettings initMemoryTest(String projectName, String className) {
        ProfilerEngineSettings settings = initTest(projectName, className, null);
        //defaults
        settings.setThreadCPUTimerOn(false);
        settings.setAllocTrackEvery(1); //default is not strict - cannot be measured in test
        settings.setRunGCOnGetResultsInMemoryProfiling(true);

        return settings;
    }

    protected void startMemoryTest(ProfilerEngineSettings settings, int instrMode, String[] classPrefixes) {
        //create runner //instrMode CommonConstants.INSTR_OBJECT_ALLOCATIONS
        assertTrue(builder != null);

        TestProfilerAppHandler handler = new TestProfilerAppHandler(this);
        TargetAppRunner runner = new TargetAppRunner(settings, handler, new TestProfilingPointsProcessor());
        runner.addProfilingEventListener(Utils.createProfilingListener(this));

        builder.removeAllListeners();
        ProfilingResultsDispatcher.getDefault().removeAllListeners();

        resultListener = new MemoryResultListener();
        builder.addListener(resultListener);

        ProfilingResultsDispatcher.getDefault().addListener(builder);
        builder.startup(runner.getProfilerClient());

        try {
            assertTrue("not read calibration data", runner.readSavedCalibrationData());
            runner.getProfilerClient().initiateMemoryProfInstrumentation(instrMode);

            Process p = startTargetVM(runner);
            assertNotNull("Target JVM is not started", p);
            bindStreams(p);
            runner.attachToTargetVMOnStartup();

            waitForStatus(STATUS_RUNNING);
            assertTrue("runner is not running", runner.targetAppIsRunning());

            waitForStatus(STATUS_RESULTS_AVAILABLE | STATUS_APP_FINISHED);

            if (!isStatus(STATUS_APP_FINISHED)) {
                waitForStatus(STATUS_APP_FINISHED);
            }
            Thread.sleep(1000);
            checkMemoryResults(runner, classPrefixes, resultListener, instrMode);
            setStatus(STATUS_MEASURED);
        } catch (Exception ex) {
            log(ex);
            assertTrue("Exception thrown: " + ex.getMessage(), false);
        } finally {
            ProfilingResultsDispatcher.getDefault().pause(true);
            builder.shutdown();

            builder.removeListener(resultListener);
            ProfilingResultsDispatcher.getDefault().removeListener(builder);

            finalizeTest(runner);
        }
    }
}
