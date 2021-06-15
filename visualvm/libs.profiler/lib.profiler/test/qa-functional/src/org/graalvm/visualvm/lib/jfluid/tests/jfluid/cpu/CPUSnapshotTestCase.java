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
 * CPUSnapshotTestCase.java
 *
 * Created on July 19, 2005, 5:20 PM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
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
import org.graalvm.visualvm.lib.jfluid.results.cpu.PrestimeCPUCCTNode;
import org.graalvm.visualvm.lib.jfluid.tests.jfluid.*;
import org.graalvm.visualvm.lib.jfluid.tests.jfluid.utils.*;
import org.graalvm.visualvm.lib.jfluid.utils.StringUtils;
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

        public void cctEstablished(RuntimeCCTNode appRootNode, boolean empty) {
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
                    log("Node : " + node.getNodeName());
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
        //addJVMArgs(settings, "-Dorg.graalvm.visualvm.lib.jfluid.wireprotocol.WireIO=true");
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
