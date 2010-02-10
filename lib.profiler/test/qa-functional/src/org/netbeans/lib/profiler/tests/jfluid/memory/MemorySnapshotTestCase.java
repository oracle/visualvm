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
 * MemoryTestCase.java
 *
 * Created on July 19, 2005, 5:21 PM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */
package org.netbeans.lib.profiler.tests.jfluid.memory;

import org.netbeans.lib.profiler.ProfilerEngineSettings;
import org.netbeans.lib.profiler.TargetAppRunner;
import org.netbeans.lib.profiler.results.CCTNode;
import org.netbeans.lib.profiler.results.EventBufferResultsProvider;
import org.netbeans.lib.profiler.results.ProfilingResultsDispatcher;
import org.netbeans.lib.profiler.results.RuntimeCCTNode;
import org.netbeans.lib.profiler.results.memory.AllocMemoryResultsSnapshot;
import org.netbeans.lib.profiler.results.memory.LivenessMemoryResultsSnapshot;
import org.netbeans.lib.profiler.results.memory.MemoryCCTManager;
import org.netbeans.lib.profiler.results.memory.MemoryCCTProvider;
import org.netbeans.lib.profiler.results.memory.MemoryCallGraphBuilder;
import org.netbeans.lib.profiler.results.memory.MemoryResultsSnapshot;
import org.netbeans.lib.profiler.results.memory.PresoObjAllocCCTNode;
import org.netbeans.lib.profiler.results.memory.RuntimeMemoryCCTNode;
import org.netbeans.lib.profiler.tests.jfluid.*;
import org.netbeans.lib.profiler.tests.jfluid.utils.*;
import org.netbeans.lib.profiler.utils.StringUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;


/**
 *
 * @author ehucka
 */
public abstract class MemorySnapshotTestCase extends CommonProfilerTestCase {
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

        public void cctEstablished(RuntimeCCTNode appRootNode, boolean emtpy) {
            if (!emtpy) {
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
    public MemorySnapshotTestCase(String name) {
        super(name);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    protected void checkClasses(MemoryResultsSnapshot snapshot, String[] prefixes) {
        ArrayList list = new ArrayList(128);

        if (snapshot instanceof AllocMemoryResultsSnapshot) {
            AllocMemoryResultsSnapshot alsnapshot = (AllocMemoryResultsSnapshot) snapshot;

            int[] objcnts = alsnapshot.getObjectsCounts();
            long[] objsizes = alsnapshot.getObjectsSizePerClass();
            String[] classnames = alsnapshot.getClassNames();

            for (int i = 0; i < snapshot.getNProfiledClasses(); i++) {
                boolean match = false;

                for (int j = 0; j < prefixes.length; j++) {
                    if (classnames[i].startsWith(prefixes[j])) {
                        match = true;

                        break;
                    }
                }

                if (match) {
                    StringBuffer out = new StringBuffer();
                    out.append(complete(StringUtils.userFormClassName(classnames[i]), 32));
                    //out.append(complete(StringUtils.nBytesToString(objsizes[i]), 10));
                    out.append(complete(String.valueOf(objcnts[i]), 8));
                    list.add(out.toString());
                }
            }

            ref(complete("Name", 32) /*complete("Bytes", 10)+*/ + complete("Objects", 8));
        } else if (snapshot instanceof LivenessMemoryResultsSnapshot) {
            LivenessMemoryResultsSnapshot lsnapshot = (LivenessMemoryResultsSnapshot) snapshot;

            log("Max Value:        " + lsnapshot.getMaxValue());
            log("Number Alloc:     " + lsnapshot.getNAlloc());
            log("Instr Classes:    " + lsnapshot.getNInstrClasses());
            log("Total tracked:    " + lsnapshot.getNTotalTracked());
            log("Tracked bytes:    " + lsnapshot.getNTotalTrackedBytes());
            log("Tracked items:    " + lsnapshot.getNTrackedItems());

            float[] avgage = lsnapshot.getAvgObjectAge();
            int[] maxSurvGen = lsnapshot.getMaxSurvGen();
            long[] ntrackedallocobjects = lsnapshot.getNTrackedAllocObjects();
            int[] ntrackedliveobjects = lsnapshot.getNTrackedLiveObjects();
            int[] totalAllocObjects = lsnapshot.getnTotalAllocObjects();
            String[] classnames = lsnapshot.getClassNames();
            long[] trackedLiveObjectsSize = lsnapshot.getTrackedLiveObjectsSize();

            for (int i = 0; i < snapshot.getNProfiledClasses(); i++) {
                boolean match = false;

                for (int j = 0; j < prefixes.length; j++) {
                    if (classnames[i].startsWith(prefixes[j])) {
                        match = true;

                        break;
                    }
                }

                if (match) {
                    StringBuffer out = new StringBuffer();
                    out.append(complete(StringUtils.userFormClassName(classnames[i]), 32));
                    //out.append(complete(StringUtils.nBytesToString(trackedLiveObjectsSize[i]), 10));
                    out.append(complete(String.valueOf(ntrackedliveobjects[i]), 10));
                    out.append(complete(String.valueOf(ntrackedallocobjects[i]), 8));
                    //out.append(complete(String.valueOf((int)avgage[i]), 8));
                    //out.append(complete(String.valueOf(maxSurvGen[i]), 8));
                    out.append(complete(String.valueOf(totalAllocObjects[i]), 8));
                    list.add(out.toString());
                }
            }

            ref(complete("Name", 32) /*complete("LiveBytes", 10)+*/ + complete("LiveObjs", 10)
                + complete("Allocs", 8) /*+complete("AvgAge", 8)+complete("MaxSurv", 8)*/ + complete("Total", 8));
        }

        //log results
        Collections.sort(list);

        for (int i = 0; i < list.size(); i++) {
            ref(list.get(i));
        }

        ref("");
    }

    protected void checkMemoryResults(TargetAppRunner targetAppRunner, String[] classPrefixes, String stacktraceClass)
                               throws Exception {
        targetAppRunner.getProfilerClient().forceObtainedResultsDump();

        boolean gotResults = resultListener.wait4results(10000);

        assertTrue("No memory results available after 10s", gotResults);
        log("results obtained: " + System.currentTimeMillis());

        MemoryResultsSnapshot snapshot = targetAppRunner.getProfilerClient().getMemoryProfilingResultsSnapshot(false);
        assertTrue(snapshot != null);
        log("snapshot taken: " + snapshot);

        ref((snapshot.containsStacks()) ? "Contains stacks." : "Does not contain stacks.");
        log("Begin time:       " + new Date(snapshot.getBeginTime()));
        log("Profiled classes: " + snapshot.getNProfiledClasses());
        log("Time Taken:       " + new Date(snapshot.getTimeTaken()));
        checkClasses(snapshot, classPrefixes);

        //stacktrace
        if (snapshot.containsStacks()) {
            int classid = -1;
            String[] classes = snapshot.getClassNames();

            for (int i = 0; i < snapshot.getNProfiledClasses(); i++) {
                if (classes[i].replace('/', '.').equals(stacktraceClass)) {
                    classid = i;
                }
            }

            assertTrue("Stack trace class wasn't find " + stacktraceClass, (classid > -1));

            MemoryCCTManager manager = new MemoryCCTManager(snapshot, classid, false);

            if (!manager.isEmpty()) {
                PresoObjAllocCCTNode root = manager.getRootNode();
                refNodes(root, "");
            }
        }

        testSerialization(snapshot);
    }

    protected boolean equals(String[] a1, String[] a2, int length) {
        for (int i = 0; i < length; i++) {
            if (!a1[i].equals(a2[i])) {
                return false;
            }
        }

        return true;
    }

    protected boolean equals(int[] a1, int[] a2, int length) {
        for (int i = 0; i < length; i++) {
            if (a1[i] != a2[i]) {
                return false;
            }
        }

        return true;
    }

    protected boolean equals(long[] a1, long[] a2, int length) {
        for (int i = 0; i < length; i++) {
            if (a1[i] != a2[i]) {
                return false;
            }
        }

        return true;
    }

    protected boolean equals(float[] a1, float[] a2, int length) {
        for (int i = 0; i < length; i++) {
            if (a1[i] != a2[i]) {
                return false;
            }
        }

        return true;
    }

    protected boolean equals(RuntimeMemoryCCTNode a1, RuntimeMemoryCCTNode a2)
                      throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        a1.writeToStream(dos);
        dos.close();

        byte[] bytes = baos.toByteArray();
        //write to bytes 2
        baos = new ByteArrayOutputStream();
        dos = new DataOutputStream(baos);
        a2.writeToStream(dos);
        dos.close();

        byte[] bytes2 = baos.toByteArray();

        if (bytes.length != bytes2.length) {
            return false;
        }

        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] != bytes2[i]) {
                return false;
            }
        }

        return true;
    }

    protected ProfilerEngineSettings initMemorySnapshotTest(String projectName, String className) {
        ProfilerEngineSettings settings = initTest(projectName, className, null);
        //defaults
        settings.setThreadCPUTimerOn(false);
        settings.setAllocTrackEvery(1); //default is not strict - cannot be measured in test
        settings.setRunGCOnGetResultsInMemoryProfiling(true);

        return settings;
    }

    protected void refNodes(PresoObjAllocCCTNode root, String tab) {
        ref(tab + (PresoObjAllocCCTNode) root);

        if (root.getNChildren() > 0) {
            root.sortChildren(PresoObjAllocCCTNode.SORT_BY_NAME, false);

            CCTNode[] nodes = root.getChildren();

            for (int i = 0; i < nodes.length; i++) {
                refNodes((PresoObjAllocCCTNode) nodes[i], tab + " ");
            }
        }
    }

    protected void startMemorySnapshotTest(ProfilerEngineSettings settings, int instrMode, String[] classPrefixes,
                                           String stacktraceClass) {
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
            checkMemoryResults(runner, classPrefixes, stacktraceClass);
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

    protected void testSerialization(MemoryResultsSnapshot snapshot) {
        try {
            //write to bytes
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            snapshot.writeToStream(dos);
            dos.close();

            byte[] bytes = baos.toByteArray();
            MemoryResultsSnapshot snapshot2;

            if (snapshot instanceof LivenessMemoryResultsSnapshot) {
                snapshot2 = new LivenessMemoryResultsSnapshot();
            } else {
                snapshot2 = new AllocMemoryResultsSnapshot();
            }

            //read from bytes
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            DataInputStream dis = new DataInputStream(bis);
            snapshot2.readFromStream(dis);
            dis.close();

            //compare
            if (snapshot instanceof LivenessMemoryResultsSnapshot) {
                LivenessMemoryResultsSnapshot s1;
                LivenessMemoryResultsSnapshot s2;
                s1 = (LivenessMemoryResultsSnapshot) snapshot;
                s2 = (LivenessMemoryResultsSnapshot) snapshot2;
                assertEquals("Snapshot Serialization: diff containsStacks", s1.containsStacks(), s2.containsStacks());
                assertEquals("Snapshot Serialization: diff beginTime", s1.getBeginTime(), s2.getBeginTime());
                assertEquals("Snapshot Serialization: diff MaxValue", s1.getMaxValue(), s2.getMaxValue());
                assertEquals("Snapshot Serialization: diff NAlloc", s1.getNAlloc(), s2.getNAlloc());
                assertEquals("Snapshot Serialization: diff NInstrClasses", s1.getNInstrClasses(), s2.getNInstrClasses());
                assertEquals("Snapshot Serialization: diff NProfiledClasses", s1.getNProfiledClasses(), s2.getNProfiledClasses());
                assertEquals("Snapshot Serialization: diff NTotalTracked", s1.getNTotalTracked(), s2.getNTotalTracked());
                assertEquals("Snapshot Serialization: diff NTotalTrackedBytes", s1.getNTotalTrackedBytes(),
                             s2.getNTotalTrackedBytes());
                assertEquals("Snapshot Serialization: diff NTrackedItems", s1.getNTrackedItems(), s2.getNTrackedItems());
                assertEquals("Snapshot Serialization: diff TimeTaken", s1.getTimeTaken(), s2.getTimeTaken());

                assertTrue("Snapshot Serialization: diff AvgObjectAge",
                           equals(s1.getAvgObjectAge(), s2.getAvgObjectAge(), s1.getNProfiledClasses()));
                assertTrue("Snapshot Serialization: diff ClassNames",
                           equals(s1.getClassNames(), s2.getClassNames(), s1.getNProfiledClasses()));
                assertTrue("Snapshot Serialization: diff MaxSurvGen",
                           equals(s1.getMaxSurvGen(), s2.getMaxSurvGen(), s1.getNProfiledClasses()));
                assertTrue("Snapshot Serialization: diff NTrackedAllocObjects",
                           equals(s1.getNTrackedAllocObjects(), s2.getNTrackedAllocObjects(), s1.getNProfiledClasses()));
                assertTrue("Snapshot Serialization: diff NTrackedLiveObjects",
                           equals(s1.getNTrackedLiveObjects(), s2.getNTrackedLiveObjects(), s1.getNProfiledClasses()));
                assertTrue("Snapshot Serialization: diff ObjectsSizePerClass",
                           equals(s1.getObjectsSizePerClass(), s2.getObjectsSizePerClass(), s1.getNProfiledClasses()));
                assertTrue("Snapshot Serialization: diff TrackedLiveObjectsSize",
                           equals(s1.getTrackedLiveObjectsSize(), s2.getTrackedLiveObjectsSize(), s1.getNProfiledClasses()));
                assertTrue("Snapshot Serialization: diff nTotalAllocObjects",
                           equals(s1.getnTotalAllocObjects(), s2.getnTotalAllocObjects(), s1.getNProfiledClasses()));
            } else {
                AllocMemoryResultsSnapshot s1;
                AllocMemoryResultsSnapshot s2;
                s1 = (AllocMemoryResultsSnapshot) snapshot;
                s2 = (AllocMemoryResultsSnapshot) snapshot2;
                assertEquals("Snapshot Serialization: diff containsStacks", s1.containsStacks(), s2.containsStacks());
                assertEquals("Snapshot Serialization: diff beginTime", s1.getBeginTime(), s2.getBeginTime());
                assertEquals("Snapshot Serialization: diff NProfiledClasses", s1.getNProfiledClasses(), s2.getNProfiledClasses());
                assertEquals("Snapshot Serialization: diff TimeTaken", s1.getTimeTaken(), s2.getTimeTaken());

                assertTrue("Snapshot Serialization: diff ClassNames",
                           equals(s1.getClassNames(), s2.getClassNames(), s1.getNProfiledClasses()));
                assertTrue("Snapshot Serialization: diff ObjectsSizePerClass",
                           equals(s1.getObjectsSizePerClass(), s2.getObjectsSizePerClass(), s1.getNProfiledClasses()));
            }

            if (snapshot.containsStacks()) {
                Field field = snapshot.getClass().getSuperclass().getDeclaredField("stacksForClasses");
                field.setAccessible(true);

                RuntimeMemoryCCTNode[] stacksForClasses = (RuntimeMemoryCCTNode[]) field.get(snapshot);
                RuntimeMemoryCCTNode[] stacksForClasses2 = (RuntimeMemoryCCTNode[]) field.get(snapshot2);

                for (int i = 0; i < stacksForClasses.length; i++) {
                    if (stacksForClasses[i] != null) {
                        assertTrue("Snapshot Serialization: diff stacktraces " + snapshot.getClassName(i),
                                   equals(stacksForClasses[i], stacksForClasses2[i]));
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            assertTrue("Snapshot Serialization: " + ex.getClass().getName() + ": " + ex.getMessage(), false);
        }
    }
}
