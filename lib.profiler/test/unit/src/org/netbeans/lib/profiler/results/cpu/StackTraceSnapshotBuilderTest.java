/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
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
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */
package org.netbeans.lib.profiler.results.cpu;

import java.lang.Thread.State;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.openmbean.CompositeData;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.netbeans.lib.profiler.results.CCTNode;
import sun.management.ThreadInfoCompositeData;
import static org.junit.Assert.*;


/**
 *
 * @author Jaroslav Bachorik, Tomas Hurka
 */
public class StackTraceSnapshotBuilderTest {

    private StackTraceSnapshotBuilder instance;

    private final StackTraceElement[] elements0 = new StackTraceElement[] {
        new StackTraceElement("test.Class1", "method3", "Class1.java", 30),
        new StackTraceElement("test.Class1", "method2", "Class1.java", 20),
        new StackTraceElement("test.Class1", "method1", "Class1.java", 10)
    };

    private final StackTraceElement[] elementsDif = new StackTraceElement[] {
        new StackTraceElement("test.Class1", "method3", "Class1.java", 40),
        new StackTraceElement("test.Class1", "method4", "Class1.java", 30),
        new StackTraceElement("test.Class1", "method2", "Class1.java", 20),
        new StackTraceElement("test.Class1", "method1", "Class1.java", 10)
    };

    private final StackTraceElement[] elementsPlus = new StackTraceElement[] {
        new StackTraceElement("test.Class1", "method4", "Class1.java", 40),
        new StackTraceElement("test.Class1", "method3", "Class1.java", 30),
        new StackTraceElement("test.Class1", "method2", "Class1.java", 20),
        new StackTraceElement("test.Class1", "method1", "Class1.java", 10)
    };

    private final StackTraceElement[] elementsMinus = new StackTraceElement[] {
        new StackTraceElement("test.Class1", "method2", "Class1.java", 20),
        new StackTraceElement("test.Class1", "method1", "Class1.java", 10)
    };

    private final StackTraceElement[] elementsDup = new StackTraceElement[] {
        new StackTraceElement("test.Class1", "method3", "Class1.java", 30),
        new StackTraceElement("test.Class1", "method2", "Class1.java", 21),
        new StackTraceElement("test.Class1", "method1", "Class1.java", 10)
    };

    private Thread thread0;
    private Thread thread1;
    private Thread thread2;

    private java.lang.management.ThreadInfo[] stack0;
    private java.lang.management.ThreadInfo[] stackPlus;
    private java.lang.management.ThreadInfo[] stackMinus;
    private java.lang.management.ThreadInfo[] stackDif;
    private java.lang.management.ThreadInfo[] stackDup;
    

    public StackTraceSnapshotBuilderTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        instance = new StackTraceSnapshotBuilder();

        thread0 = new Thread("Test thread 0");
        thread1 = new Thread("Test thread 1");
        thread2 = new Thread("Test thread 2");
        
        stack0 = new java.lang.management.ThreadInfo[] {
                createThreadInfo(thread0, elements0),
                createThreadInfo(thread1, elements0)
        };

        stackPlus = new java.lang.management.ThreadInfo[] {
                createThreadInfo(thread0, elementsPlus),
                createThreadInfo(thread1, elements0),
                createThreadInfo(thread2, elements0)
        };

        stackMinus = new java.lang.management.ThreadInfo[] {
                createThreadInfo(thread0, elementsMinus)
        };

        stackDif = new java.lang.management.ThreadInfo[] {
                createThreadInfo(thread0, elementsDif)
        };

        stackDup = new java.lang.management.ThreadInfo[] {
                createThreadInfo(thread0, elementsDup),
                createThreadInfo(thread1, elements0)
        };

    }

    @After
    public void tearDown() {
        instance = null;
    }

    /**
     * Test of createSnapshot method, of class StackTraceSnapshotBuilder.
     * Empty data
     */
    @Test
    public void testCreateSnapshotEmpty() {
        System.out.println("create snapshot : empty");

        try {
            instance.createSnapshot(System.currentTimeMillis());
            fail("Attempt to create an empty snapshot should throw NoDataAvailableException");
        } catch (CPUResultsSnapshot.NoDataAvailableException ex) {
        }
    }

    @Test
    public void testCreateSnapshotOneSample() throws CPUResultsSnapshot.NoDataAvailableException {
        System.out.println("create snapshot : one sample");

        addStacktrace(stack0, 0);
        CPUResultsSnapshot snapshot = instance.createSnapshot(System.currentTimeMillis());
        assertFalse(snapshot.collectingTwoTimeStamps);
        assertEquals(instance.methodInfos.size(), snapshot.nInstrMethods);
    }

    @Test
    public void testCreateSnapshotNoChanges() throws CPUResultsSnapshot.NoDataAvailableException {
        System.out.println("create snapshot : two samples");

        addStacktrace(stack0, 0);
        addStacktrace(stack0, 500000);

        CPUResultsSnapshot snapshot = instance.createSnapshot(System.currentTimeMillis());
        assertFalse(snapshot.collectingTwoTimeStamps);
        assertEquals(instance.methodInfos.size(), snapshot.nInstrMethods);
    }

    @Test
    public void testCreateSnapshotMinus() throws CPUResultsSnapshot.NoDataAvailableException {
        System.out.println("create snapshot : minus");

        addStacktrace(stack0, 0);
        addStacktrace(stackMinus, 500000);

        CPUResultsSnapshot snapshot = instance.createSnapshot(System.currentTimeMillis());
        assertFalse(snapshot.collectingTwoTimeStamps);
        assertEquals(instance.methodInfos.size(), snapshot.nInstrMethods);
    }

    @Test
    public void testCreateSnapshotPlus() throws CPUResultsSnapshot.NoDataAvailableException {
        System.out.println("create snapshot : plus");

        addStacktrace(stack0, 0);
        addStacktrace(stackPlus, 500000);

        CPUResultsSnapshot snapshot = instance.createSnapshot(System.currentTimeMillis());
        assertFalse(snapshot.collectingTwoTimeStamps);
        assertEquals(instance.methodInfos.size(), snapshot.nInstrMethods);
    }

    @Test
    public void testCreateSnapshotPlusMinus() throws CPUResultsSnapshot.NoDataAvailableException {
        System.out.println("create snapshot : plus->minus");

        addStacktrace(stack0, 0);
        addStacktrace(stackPlus, 500000);
        addStacktrace(stackMinus, 1000000);

        CPUResultsSnapshot snapshot = instance.createSnapshot(System.currentTimeMillis());
        assertFalse(snapshot.collectingTwoTimeStamps);
        assertEquals(instance.methodInfos.size(), snapshot.nInstrMethods);
    }

    @Test
    public void testCreateSnapshotMinusPlus() throws CPUResultsSnapshot.NoDataAvailableException {
        System.out.println("create snapshot : minus->plus");

        addStacktrace(stack0, 0);
        addStacktrace(stackMinus, 500000);
        addStacktrace(stackPlus, 1000000);

        CPUResultsSnapshot snapshot = instance.createSnapshot(System.currentTimeMillis());
        assertFalse(snapshot.collectingTwoTimeStamps);
        assertEquals(instance.methodInfos.size(), snapshot.nInstrMethods);
    }

    @Test
    public void testCreateSnapshotDup() throws CPUResultsSnapshot.NoDataAvailableException {
        System.out.println("create snapshot : dup");

        addStacktrace(stack0, 0);
        addStacktrace(stackDup, 500000);

        CPUResultsSnapshot snapshot = instance.createSnapshot(System.currentTimeMillis());
        assertFalse(snapshot.collectingTwoTimeStamps);
        assertEquals(instance.methodInfos.size(), snapshot.nInstrMethods);
        CPUCCTContainer container = snapshot.getContainerForThread((int) stack0[0].getThreadId(), CPUResultsSnapshot.METHOD_LEVEL_VIEW);
        assertEquals(container.getThreadName(),thread0.getName());
        PrestimeCPUCCTNode root = container.getRootNode();
        assertEquals(1, root.getNCalls());
        CCTNode[] childrens = root.getChildren();
        assertEquals(1, childrens.length);
        PrestimeCPUCCTNode ch = (PrestimeCPUCCTNode) childrens[0];
        assertEquals("test.Class1.method1()", ch.getNodeName());
        assertEquals(1, ch.getNCalls());
        CCTNode[] childrens1 = ch.getChildren();
        assertEquals(2, childrens1.length);
        PrestimeCPUCCTNode ch1 = (PrestimeCPUCCTNode) childrens1[0];
        if (ch1.isSelfTimeNode()) {
            ch1 = (PrestimeCPUCCTNode) childrens1[1];
        }
        assertEquals("test.Class1.method2()", ch1.getNodeName());
        assertEquals(1, ch1.getNCalls());
        CCTNode[] childrens2 = ch1.getChildren();
        assertEquals(2, childrens2.length);
        PrestimeCPUCCTNode ch2 = (PrestimeCPUCCTNode) childrens2[0];
        if (ch2.isSelfTimeNode()) {
            ch2 = (PrestimeCPUCCTNode) childrens2[1];
        }
        assertEquals("test.Class1.method3()", ch2.getNodeName());
        assertEquals(2, ch2.getNCalls());
    }

    @Test
    public void testAddStacktrace() {
        System.out.println("add stacktrace");

        addStacktrace(stack0, 0);
        assertTrue(instance.methodInfos.size() == elements0.length);
        assertTrue(instance.threadIds.size() == stack0.length);
        assertTrue(instance.threadNames.size() == stack0.length);
        assertFalse(-1L == instance.currentDumpTimeStamp);
    }

    @Test
    public void testAddStacktraceDuplicate() {
        System.out.println("add stacktrace : duplicate");

        long stamp = 0;
        addStacktrace(stack0, stamp);

        int miSize = instance.methodInfos.size();
        int tIdSize = instance.threadIds.size();
        long timestamp = instance.currentDumpTimeStamp;

        try {
            addStacktrace(stack0, stamp);
            fail();
        } catch (IllegalStateException ex) {
            // ok
        }
    }

    @Test
    public void testAddStacktracePlus() {
        System.out.println("add stacktrace : plus");

        addStacktrace(stack0, 0);

        long timestamp = 500000;

        addStacktrace(stackPlus, timestamp);

        assertEquals(Math.max(stack0.length, stackPlus.length), instance.threadIds.size());
        assertEquals(Math.max(elements0.length, elementsPlus.length), instance.methodInfos.size());
        assertEquals(timestamp, instance.currentDumpTimeStamp);
    }

    @Test
    public void testAddStacktracePlusWaiting() {
        System.out.println("add stacktrace : plus/waiting");

        addStacktrace(stack0, 0);

        long timestamp = 500000;
        setState(stackPlus[0],Thread.State.WAITING);

        addStacktrace(stackPlus, timestamp);

        assertEquals(Math.max(stack0.length, stackPlus.length), instance.threadIds.size());
        assertEquals(Math.max(elements0.length, elementsPlus.length), instance.methodInfos.size());
        assertEquals(timestamp, instance.currentDumpTimeStamp);
    }

    @Test
    public void testAddStacktracePlusWaitingThread() {
        System.out.println("add stacktrace : plus/waiting; additional thread");

        addStacktrace(stack0, 0);

        long timestamp = 500000;
        setState(stackPlus[2],Thread.State.WAITING);

        addStacktrace(stackPlus, timestamp);

        assertEquals(Math.max(stack0.length, stackPlus.length), instance.threadIds.size());
        assertEquals(Math.max(elements0.length, elementsPlus.length), instance.methodInfos.size());
        assertEquals(timestamp, instance.currentDumpTimeStamp);
    }

    @Test
    public void testAddStacktraceWaitingPlus() {
        System.out.println("add stacktrace : waiting/plus");

        setState(stack0[0],Thread.State.WAITING);
        addStacktrace(stack0, 0);

        long timestamp = 500000;
        setState(stackPlus[2],Thread.State.RUNNABLE);

        addStacktrace(stackPlus, timestamp);

        assertEquals(Math.max(stack0.length, stackPlus.length), instance.threadIds.size());
        assertEquals(Math.max(elements0.length, elementsPlus.length), instance.methodInfos.size());
        assertEquals(timestamp, instance.currentDumpTimeStamp);
    }

    @Test
    public void testAddStacktraceMinus() {
        System.out.println("add stacktrace : minus");

        addStacktrace(stack0, 0);

        long timestamp = 500000;

        addStacktrace(stackMinus, timestamp);

        assertEquals(Math.max(stack0.length, stackMinus.length), instance.threadIds.size());
        assertEquals(Math.max(elements0.length, elementsMinus.length), instance.methodInfos.size());
        assertEquals(timestamp, instance.currentDumpTimeStamp);
    }

    @Test
    public void testAddStacktraceMinusWaiting() {
        System.out.println("add stacktrace : minus/waiting");

        addStacktrace(stack0, 0);

        long timestamp = 500000;
        setState(stackMinus[0], Thread.State.WAITING);
        addStacktrace(stackMinus, timestamp);

        assertEquals(Math.max(stack0.length, stackMinus.length), instance.threadIds.size());
        assertEquals(Math.max(elements0.length, elementsMinus.length), instance.methodInfos.size());
        assertEquals(timestamp, instance.currentDumpTimeStamp);
    }

    @Test
    public void testAddStacktraceMinusWaitingThread() {
        System.out.println("add stacktrace : minus/waiting; additional thread");

        setState(stack0[1], Thread.State.WAITING);
        addStacktrace(stack0, 0);

        long timestamp = 500000;
        setState(stackMinus[0], Thread.State.WAITING);
        addStacktrace(stackMinus, timestamp);

        assertEquals(Math.max(stack0.length, stackMinus.length), instance.threadIds.size());
        assertEquals(Math.max(elements0.length, elementsMinus.length), instance.methodInfos.size());
        assertEquals(timestamp, instance.currentDumpTimeStamp);
    }

    @Test
    public void testAddStacktraceWaitingMinus() {
        System.out.println("add stacktrace : waiting/minus");

        setState(stack0[0], Thread.State.WAITING);
        addStacktrace(stack0, 0);

        long timestamp = 500000;
        setState(stackMinus[0], Thread.State.RUNNABLE);
        addStacktrace(stackMinus, timestamp);

        assertEquals(Math.max(stack0.length, stackMinus.length), instance.threadIds.size());
        assertEquals(Math.max(elements0.length, elementsMinus.length), instance.methodInfos.size());
        assertEquals(timestamp, instance.currentDumpTimeStamp);
    }

    @Test
    public void testAddStacktraceDif() {
        System.out.println("add stacktrace : diff");

        addStacktrace(stack0, 0);

        long timestamp = 500000;

        addStacktrace(stackDif, timestamp);

        assertEquals(Math.max(stack0.length, stackDif.length), instance.threadIds.size());
        for(StackTraceElement element : elements0) {
            if (!instance.methodInfos.contains(new StackTraceSnapshotBuilder.MethodInfo(element))) {
                fail();
            }
        }
        for(StackTraceElement element : elementsDif) {
            if (!instance.methodInfos.contains(new StackTraceSnapshotBuilder.MethodInfo(element))) {
                fail();
            }
        }
        assertEquals(timestamp, instance.currentDumpTimeStamp);
    }

    @Test
    public void testAddStacktraceDifWaiting() {
        System.out.println("add stacktrace : diff/waiting");

        addStacktrace(stack0, 0);

        long timestamp = 500000;
        setState(stackDif[0], Thread.State.WAITING);
        
        addStacktrace(stackDif, timestamp);

        assertEquals(Thread.State.WAITING, instance.lastStackTrace.get().get(thread0.getId()).getThreadState());

        assertEquals(Math.max(stack0.length, stackDif.length), instance.threadIds.size());
        for(StackTraceElement element : elements0) {
            if (!instance.methodInfos.contains(new StackTraceSnapshotBuilder.MethodInfo(element))) {
                fail();
            }
        }
        for(StackTraceElement element : elementsDif) {
            if (!instance.methodInfos.contains(new StackTraceSnapshotBuilder.MethodInfo(element))) {
                fail();
            }
        }
        assertEquals(timestamp, instance.currentDumpTimeStamp);
    }

    @Test
    public void testAddStacktraceDifWaitingBlocked() {
        System.out.println("add stacktrace : diff/waiting/blocked");

        addStacktrace(stack0, 0);

        long timestamp = 500000;
        setState(stackDif[0], Thread.State.WAITING);

        addStacktrace(stackDif, timestamp);

        setState(stack0[0], Thread.State.BLOCKED);

        timestamp += 500000;

        addStacktrace(stack0, timestamp);

        assertEquals(Thread.State.BLOCKED, instance.lastStackTrace.get().get(thread0.getId()).getThreadState());

        assertEquals(Math.max(stack0.length, stackDif.length), instance.threadIds.size());
        for(StackTraceElement element : elements0) {
            if (!instance.methodInfos.contains(new StackTraceSnapshotBuilder.MethodInfo(element))) {
                fail();
            }
        }
        for(StackTraceElement element : elementsDif) {
            if (!instance.methodInfos.contains(new StackTraceSnapshotBuilder.MethodInfo(element))) {
                fail();
            }
        }
        assertEquals(timestamp, instance.currentDumpTimeStamp);
    }

    @Test
    public void testAddStacktraceDifBlockedWaiting() {
        System.out.println("add stacktrace : diff/blocked/waiting");

        addStacktrace(stack0, 0);

        long timestamp = 500000;
        setState(stackDif[0], Thread.State.BLOCKED);

        addStacktrace(stackDif, timestamp);

        setState(stack0[0], Thread.State.WAITING);

        timestamp += 500000;

        addStacktrace(stack0, timestamp);

        assertEquals(Thread.State.WAITING, instance.lastStackTrace.get().get(thread0.getId()).getThreadState());

        assertEquals(Math.max(stack0.length, stackDif.length), instance.threadIds.size());
        for(StackTraceElement element : elements0) {
            if (!instance.methodInfos.contains(new StackTraceSnapshotBuilder.MethodInfo(element))) {
                fail();
            }
        }
        for(StackTraceElement element : elementsDif) {
            if (!instance.methodInfos.contains(new StackTraceSnapshotBuilder.MethodInfo(element))) {
                fail();
            }
        }
        assertEquals(timestamp, instance.currentDumpTimeStamp);
    }

    @Test
    public void testAddStacktraceWaitingDif() {
        System.out.println("add stacktrace : waiting/diff");

        setState(stack0[0], Thread.State.WAITING);
        addStacktrace(stack0, 0);

        long timestamp = 500000;


        addStacktrace(stackDif, timestamp);

        assertEquals(Math.max(stack0.length, stackDif.length), instance.threadIds.size());
        for(StackTraceElement element : elements0) {
            if (!instance.methodInfos.contains(new StackTraceSnapshotBuilder.MethodInfo(element))) {
                fail();
            }
        }
        for(StackTraceElement element : elementsDif) {
            if (!instance.methodInfos.contains(new StackTraceSnapshotBuilder.MethodInfo(element))) {
                fail();
            }
        }
        assertEquals(timestamp, instance.currentDumpTimeStamp);
    }

    @Test
    public void testAddStacktraceWaitingDifRunnable() {
        System.out.println("add stacktrace : waiting/diff/runnable");

        setState(stack0[0], Thread.State.WAITING);
        addStacktrace(stack0, 0);

        long timestamp = 500000;

        setState(stackDif[0], Thread.State.RUNNABLE);
        addStacktrace(stackDif, timestamp);

        assertEquals(Math.max(stack0.length, stackDif.length), instance.threadIds.size());
        for(StackTraceElement element : elements0) {
            if (!instance.methodInfos.contains(new StackTraceSnapshotBuilder.MethodInfo(element))) {
                fail();
            }
        }
        for(StackTraceElement element : elementsDif) {
            if (!instance.methodInfos.contains(new StackTraceSnapshotBuilder.MethodInfo(element))) {
                fail();
            }
        }
        assertEquals(timestamp, instance.currentDumpTimeStamp);
    }

    @Test
    public void testAddStackTraceNew() {
        System.out.println("add stacktrace : new");

        setState(stack0[0], Thread.State.NEW);

        try {
            addStacktrace(stack0, 500000);
            fail();
        } catch (IllegalStateException ex) {}
    }

    @Test
    public void testAddStackTraceWasTerminated() {
        System.out.println("add stacktrace : terminated->runnable");

        setState(stack0[0], Thread.State.TERMINATED);

        try {
            addStacktrace(stack0, 0);
            setState(stack0[0], Thread.State.RUNNABLE);
            addStacktrace(stack0, 500000);
            fail();
        } catch (IllegalStateException ex) {}
    }


    @Test
    public void testAddStackTraceRunnable() {
        System.out.println("add stacktrace : runnable");

        setState(stack0[0], Thread.State.RUNNABLE);

        addStacktrace(stack0, 500000);

        assertEquals(500000, instance.currentDumpTimeStamp);
        assertEquals(Thread.State.RUNNABLE, instance.lastStackTrace.get().get(thread0.getId()).getThreadState());
    }

    @Test
    public void testAddStackTraceWaiting() {
        System.out.println("add stacktrace : waiting");

        setState(stack0[0], Thread.State.WAITING);

        addStacktrace(stack0, 500000);

        assertEquals(500000, instance.currentDumpTimeStamp);
        assertEquals(Thread.State.WAITING, instance.lastStackTrace.get().get(thread0.getId()).getThreadState());
    }

    @Test
    public void testAddStackTraceTimedWaiting() {
        System.out.println("add stacktrace : timed waiting");

        setState(stack0[0], Thread.State.TIMED_WAITING);

        addStacktrace(stack0, 500000);

        assertEquals(500000, instance.currentDumpTimeStamp);
        assertEquals(Thread.State.TIMED_WAITING, instance.lastStackTrace.get().get(thread0.getId()).getThreadState());
    }

    @Test
    public void testAddStackTraceBlocked() {
        System.out.println("add stacktrace : blocked");

        setState(stack0[0], Thread.State.BLOCKED);

        addStacktrace(stack0, 500000);

        assertEquals(500000, instance.currentDumpTimeStamp);
        assertEquals(Thread.State.BLOCKED, instance.lastStackTrace.get().get(thread0.getId()).getThreadState());
    }

    @Test
    public void testAddStackTraceTerminated() {
        System.out.println("add stacktrace : terminated");

        setState(stack0[0], Thread.State.TERMINATED);

        addStacktrace(stack0, 500000);

        assertEquals(500000, instance.currentDumpTimeStamp);
        assertEquals(Thread.State.TERMINATED, instance.lastStackTrace.get().get(thread0.getId()).getThreadState());
    }

    @Test
    public void testAddStackTraceWaitRun() {
        System.out.println("add stacktrace : wait->run");

        addStacktrace(stack0, 0);
        setState(stack0[0], Thread.State.WAITING);
        
        addStacktrace(stack0, 500000);

        assertEquals(500000, instance.currentDumpTimeStamp);
        assertEquals(Thread.State.WAITING, instance.lastStackTrace.get().get(thread0.getId()).getThreadState());

        setState(stack0[0], Thread.State.RUNNABLE);
        addStacktrace(stack0, 1000000);

        assertEquals(1000000, instance.currentDumpTimeStamp);
        assertEquals(Thread.State.RUNNABLE, instance.lastStackTrace.get().get(thread0.getId()).getThreadState());
    }

    @Test
    public void testAddStackTraceWaitWait() {
        System.out.println("add stacktrace : wait->wait");

        addStacktrace(stack0, 0);
        setState(stack0[0], Thread.State.WAITING);

        addStacktrace(stack0, 500000);

        assertEquals(500000, instance.currentDumpTimeStamp);
        assertEquals(Thread.State.WAITING, instance.lastStackTrace.get().get(thread0.getId()).getThreadState());
        addStacktrace(stack0, 1000000);

        assertEquals(1000000, instance.currentDumpTimeStamp);
        assertEquals(Thread.State.WAITING, instance.lastStackTrace.get().get(thread0.getId()).getThreadState());
    }

    @Test
    public void testAddStackTraceWaitBlocked() {
        System.out.println("add stacktrace : wait->blocked");

        addStacktrace(stack0, 0);
        setState(stack0[0], Thread.State.WAITING);

        addStacktrace(stack0, 500000);

        assertEquals(500000, instance.currentDumpTimeStamp);
        assertEquals(Thread.State.WAITING, instance.lastStackTrace.get().get(thread0.getId()).getThreadState());
        setState(stack0[0], Thread.State.BLOCKED);
        addStacktrace(stack0, 1000000);

        assertEquals(1000000, instance.currentDumpTimeStamp);
        assertEquals(Thread.State.BLOCKED, instance.lastStackTrace.get().get(thread0.getId()).getThreadState());
    }



    @Test
    public void testReset() {
        System.out.println("reset");
        ThreadMXBean tbean = ManagementFactory.getThreadMXBean();
        addStacktrace(tbean.getThreadInfo(tbean.getAllThreadIds(), Integer.MAX_VALUE), System.nanoTime());
        addStacktrace(tbean.getThreadInfo(tbean.getAllThreadIds(), Integer.MAX_VALUE), System.nanoTime());

        instance.reset();
        assertTrue(instance.methodInfos.size() == 0);
        assertTrue(instance.threadIds.size() == 0);
        assertTrue(instance.threadNames.size() == 0);
        assertEquals(-1L, instance.currentDumpTimeStamp);
        //assertEquals(-1L, instance.firstDumpTimeStamp);
        assertEquals(0, instance.stackTraceCount);

        try {
            instance.createSnapshot(System.currentTimeMillis());
            fail();
        } catch (CPUResultsSnapshot.NoDataAvailableException ex) {
        }
    }

    @Test
    public void testIgnoredThreadName() {
        System.out.println("ignored thread name");

        String ignoredThread = "Thread 0";
        instance.setIgnoredThreads(Collections.singleton(ignoredThread));

        addStacktrace(stack0, 0);
        assertFalse(instance.threadNames.contains(ignoredThread));
    }

    private java.lang.management.ThreadInfo createThreadInfo(Thread t, StackTraceElement[] stack) {
        Constructor tinfoConstructor = java.lang.management.ThreadInfo.class.getDeclaredConstructors()[0];
        tinfoConstructor.setAccessible(true);
        try {
            ThreadInfo tinfo =  (ThreadInfo) tinfoConstructor.newInstance(t,0,null,null,0,0,0,0,stack);
            setState(tinfo,State.RUNNABLE);
            return tinfo;
        } catch (InstantiationException ex) {
            Logger.getLogger(StackTraceSnapshotBuilderTest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(StackTraceSnapshotBuilderTest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(StackTraceSnapshotBuilderTest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvocationTargetException ex) {
            Logger.getLogger(StackTraceSnapshotBuilderTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private void setState(java.lang.management.ThreadInfo tinfo, State s) {
        try {
            Field tstateField = tinfo.getClass().getDeclaredField("threadState");
            tstateField.setAccessible(true);
            tstateField.set(tinfo, s);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(StackTraceSnapshotBuilderTest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(StackTraceSnapshotBuilderTest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchFieldException ex) {
            Logger.getLogger(StackTraceSnapshotBuilderTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void addStacktrace(java.lang.management.ThreadInfo[] tinfos, long time) {
        java.lang.management.ThreadInfo[] newInfo = new java.lang.management.ThreadInfo[tinfos.length];
        int i = 0;

        for (java.lang.management.ThreadInfo tinfo : tinfos) {
            CompositeData aaa = ThreadInfoCompositeData.toCompositeData(tinfo);
            newInfo[i++] = ThreadInfo.from(aaa);
        }
        instance.addStacktrace(newInfo, time);
    }
}