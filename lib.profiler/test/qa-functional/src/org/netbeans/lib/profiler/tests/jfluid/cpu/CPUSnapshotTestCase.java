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
 * CPUSnapshotTestCase.java
 *
 * Created on July 19, 2005, 5:20 PM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */
package org.netbeans.lib.profiler.tests.jfluid.cpu;

import org.netbeans.lib.profiler.ProfilerEngineSettings;
import org.netbeans.lib.profiler.TargetAppRunner;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.results.EventBufferResultsProvider;
import org.netbeans.lib.profiler.results.ProfilingResultsDispatcher;
import org.netbeans.lib.profiler.results.RuntimeCCTNode;
import org.netbeans.lib.profiler.results.cpu.CPUCCTProvider;
import org.netbeans.lib.profiler.results.cpu.CPUCallGraphBuilder;
import org.netbeans.lib.profiler.results.cpu.CPUResultsSnapshot;
import org.netbeans.lib.profiler.results.cpu.FlatProfileBuilder;
import org.netbeans.lib.profiler.results.cpu.FlatProfileContainer;
import org.netbeans.lib.profiler.results.cpu.FlatProfileContainerFree;
import org.netbeans.lib.profiler.results.cpu.PrestimeCPUCCTNode;
import org.netbeans.lib.profiler.tests.jfluid.*;
import org.netbeans.lib.profiler.tests.jfluid.utils.*;
import org.netbeans.lib.profiler.utils.StringUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;


/**
 *
 * @author ehucka
 */
public abstract class CPUSnapshotTestCase extends CommonProfilerTestCase {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private class CPUResultListener implements CPUCCTProvider.Listener {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private final Object resultsLock = new Object();
        private boolean hasResults = false;

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void cctEstablished(RuntimeCCTNode appRootNode) {
            log("CCT Results established");

            synchronized (resultsLock) {
                hasResults = true;
                resultsLock.notify();
            }
        }

        public void cctReset() {
            log("CCT Results reset");

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
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Creates a new instance of CPUSnapshotTestCase
     */
    public CPUSnapshotTestCase(String name) {
        super(name);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    protected boolean checkSerialization(CPUResultsSnapshot snapshot) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            snapshot.writeToStream(dos);
            dos.close();

            byte[] bytes = baos.toByteArray();
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            DataInputStream dis = new DataInputStream(bais);
            CPUResultsSnapshot snapshot2 = new CPUResultsSnapshot();
            snapshot2.readFromStream(dis);
            dis.close();

            baos = new ByteArrayOutputStream();
            dos = new DataOutputStream(baos);
            snapshot2.writeToStream(dos);
            dos.close();

            byte[] bytes2 = baos.toByteArray();

            //compare
            if (bytes.length != bytes2.length) {
                return false;
            }

            for (int i = 0; i < bytes.length; i++) {
                if (bytes[i] != bytes2[i]) {
                    return false;
                }
            }

            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return false;
    }

    protected void checkSumsOfCCTNodes(PrestimeCPUCCTNode node, String pre, double tolerance, String[] filterout, int level) {
        log(complete(pre + node.getNodeName(), 62) + complete(StringUtils.mcsTimeToString(node.getTotalTime0()), 9) + " ms   ("
            + complete(StringUtils.floatPerCentToString(node.getTotalTime0InPerCent()), 7) + " %)  "
            + complete(String.valueOf(node.getNCalls()), 3));

        boolean exclude = false;

        if (filterout != null) {
            for (int i = 0; i < filterout.length; i++) {
                if (node.getNodeName().startsWith(filterout[i])) {
                    exclude = true;

                    break;
                }
            }
        }

        if (!exclude) {
            long time = 0;
            float percent = 0.0f;

            for (int i = 0; i < node.getNChildren(); i++) {
                PrestimeCPUCCTNode pnode = (PrestimeCPUCCTNode) (node.getChild(i));
                checkSumsOfCCTNodes(pnode, pre + " ", tolerance, filterout, level + 1);
                time += pnode.getTotalTime0();
                percent += pnode.getTotalTime0InPerCent();
            }

            if ((level > 1) && (node.getNChildren() > 0)) {
                double timediff = (Math.abs(time - node.getTotalTime0()) * 100.0) / node.getTotalTime0();
                double percentdiff = (Math.abs(percent - node.getTotalTime0InPerCent()) * 100.0) / node.getTotalTime0InPerCent();

                if ((timediff > tolerance) || (percentdiff > tolerance)) {
                    log("Time diff: " + timediff + " %");
                    log("Percent diff: " + percentdiff + " %");
                    assertTrue("Node's and sum of subnodes values differ", false);
                }
            }
        }
    }

    protected int[] findThreadAndMethod(PrestimeCPUCCTNode node, String[] method, String[] filterout) {
        boolean exclude = false;

        if (filterout != null) {
            for (int i = 0; i < filterout.length; i++) {
                if (node.getNodeName().startsWith(filterout[i])) {
                    exclude = true;

                    break;
                }
            }
        }

        if (!exclude) {
            String[] nmethod = node.getMethodClassNameAndSig();
            boolean found = true;

            for (int i = 0; i < nmethod.length; i++) {
                if (!nmethod[i].equals(method[i])) {
                    found = false;
                }
            }

            if (found) {
                return new int[] { node.getThreadId(), node.getMethodId() };
            }

            for (int i = 0; i < node.getNChildren(); i++) {
                PrestimeCPUCCTNode pnode = (PrestimeCPUCCTNode) (node.getChild(i));
                int[] ret = findThreadAndMethod(pnode, method, filterout);

                if (ret != null) {
                    return ret;
                }
            }
        }

        return null;
    }

    protected ProfilerEngineSettings initSnapshotTest(String projectName, String mainClass, String[][] rootMethods) {
        ProfilerEngineSettings settings = initTest(projectName, mainClass, rootMethods);
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
        settings.setCPUProfilingType(CommonConstants.CPU_INSTR_FULL);

        return settings;
    }

    protected void refOfCCTNodes(PrestimeCPUCCTNode node, String pre, boolean time, boolean percent, boolean invocations) {
        ref(complete(pre + node.getNodeName(), 62)
            + ((!time) ? "" : (complete(String.valueOf(node.getTotalTime0() / 1000.0), 9) + " ms   "))
            + ((!percent) ? "" : (complete(String.valueOf(node.getTotalTime0InPerCent()), 7) + " %  "))
            + ((!invocations) ? "" : complete(String.valueOf(node.getNCalls()), 3)));

        for (int i = 0; i < node.getNChildren(); i++) {
            PrestimeCPUCCTNode pnode = (PrestimeCPUCCTNode) (node.getChild(i));
            refOfCCTNodes(pnode, pre + " ", time, percent, invocations);
        }
    }

    protected void startSnapshotTest(ProfilerEngineSettings settings, String[] reverseMethod, long initDelay, double diffPercent,
                                     String[] filterout) {
        CPUCallGraphBuilder builder = new CPUCallGraphBuilder();

        TargetAppRunner runner = new TargetAppRunner(settings, new TestProfilerAppHandler(this),
                                                     new TestProfilingPointsProcessor());
        runner.addProfilingEventListener(Utils.createProfilingListener(this));

        builder.removeAllListeners();
        ProfilingResultsDispatcher.getDefault().removeAllListeners();

        CPUResultListener resultListener = new CPUResultListener();
        builder.addListener(resultListener);

        FlatProfileBuilder flattener = new FlatProfileBuilder();
        builder.addListener(flattener);
        flattener.setContext(runner.getProfilerClient(),null,null);

        ProfilingResultsDispatcher.getDefault().addListener(builder);

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

            if (initDelay == 0) {
                waitForStatus(STATUS_APP_FINISHED);
            } else {
                Thread.sleep(initDelay);
            }

            if (runner.targetJVMIsAlive()) {
                log("obtaining results " + String.valueOf(System.currentTimeMillis()));
                assertTrue("Results do not exist - issue 65185.", runner.getProfilerClient().cpuResultsExist());

                if (initDelay == 0) {
                    Thread.sleep(200); // wait a while so that client can process all CPU results
                }
                CPUResultsSnapshot snapshot = runner.getProfilerClient().getCPUProfilingResultsSnapshot();
                log("\nSnapshot:");

                //test tree values
                PrestimeCPUCCTNode root = snapshot.getRootNode(CPUResultsSnapshot.METHOD_LEVEL_VIEW);
                log("Snapshot taken: " + snapshot.getTimeTaken());
                log("Checking tree ...");
                checkSumsOfCCTNodes(root, "", diffPercent, filterout, 0);

                log("Checking serialization");
                assertTrue("The snapshot is not serialized/deserialized right.", checkSerialization(snapshot));

                log("Checking reverse call");

                if (reverseMethod != null) {
                    int[] sigs = findThreadAndMethod(root, reverseMethod, filterout);

                    if (sigs != null) {
                        PrestimeCPUCCTNode revers = snapshot.getReverseCCT(-1, sigs[1], CPUResultsSnapshot.METHOD_LEVEL_VIEW);

                        if (initDelay > 0) {
                            refOfCCTNodes(revers, "", false, false, false);
                        } else {
                            refOfCCTNodes(revers, "", false, false, true);
                        }
                    }
                }

                log("");
            }

            setStatus(STATUS_MEASURED);
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
