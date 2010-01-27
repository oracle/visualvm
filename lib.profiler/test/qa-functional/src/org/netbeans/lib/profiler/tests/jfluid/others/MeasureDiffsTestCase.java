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
 * MeasureDiffsTestCase.java
 *
 * Created on July 19, 2005, 5:20 PM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */
package org.netbeans.lib.profiler.tests.jfluid.others;

import org.netbeans.lib.profiler.ProfilerEngineSettings;
import org.netbeans.lib.profiler.TargetAppRunner;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.results.ProfilingResultsDispatcher;
import org.netbeans.lib.profiler.results.RuntimeCCTNode;
import org.netbeans.lib.profiler.results.cpu.CPUCCTProvider;
import org.netbeans.lib.profiler.results.cpu.CPUCallGraphBuilder;
import org.netbeans.lib.profiler.results.cpu.FlatProfileBuilder;
import org.netbeans.lib.profiler.results.cpu.FlatProfileContainer;
import org.netbeans.lib.profiler.results.cpu.FlatProfileContainerFree;
import org.netbeans.lib.profiler.tests.jfluid.*;
import org.netbeans.lib.profiler.tests.jfluid.utils.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import org.netbeans.lib.profiler.results.EventBufferResultsProvider;


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

        public void cctEstablished(RuntimeCCTNode appRootNode, boolean emtpy) {
            if (!emtpy) {
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

        String[] cmds = (String[]) commands.toArray(new String[commands.size()]);

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

        //addJVMArgs(settings, "-Dorg.netbeans.lib.profiler.wireprotocol.WireIO=true");
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

        //addJVMArgs(settings, "-Dorg.netbeans.lib.profiler.wireprotocol.WireIO=true");
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
            for (int mets = 0; mets < methodsOrder.length; mets++) {
                if (fpc.getMethodNameAtRow(row).startsWith(methodsOrder[mets])) {
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
            outFile = File.createTempFile("profiler", "test");
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

        for (int i = 0; i < keys.length; i++) {
            Results res = (Results) (results.get(keys[i]));
            log(keys[i] + ";" + res.toString());
        }

        log("\n");
    }
}
