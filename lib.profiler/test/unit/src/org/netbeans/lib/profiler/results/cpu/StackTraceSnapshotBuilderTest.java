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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Jaroslav Bachorik
 */
public class StackTraceSnapshotBuilderTest {
    private static class ThreadEx extends Thread {
        private State adjustableState = State.RUNNABLE;

        public ThreadEx(String name) {
            super(name);
        }

        @Override
        public State getState() {
            return adjustableState;
        }

        public void setState(State state) {
            adjustableState = state;
        }
    }
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

    private ThreadEx thread0;
    private ThreadEx thread1;
    private ThreadEx thread2;

    private Map<Thread, StackTraceElement[]> stack0;

    private Map<Thread, StackTraceElement[]> stackPlus;

    private Map<Thread, StackTraceElement[]> stackMinus;

    private Map<Thread, StackTraceElement[]> stackDif;
    

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

        thread0 = new ThreadEx("Test thread 0");
        thread1 = new ThreadEx("Test thread 1");
        thread2 = new ThreadEx("Test thread 2");
        
        stack0 = new HashMap<Thread, StackTraceElement[]>() {
            {
                put(thread0, elements0);
                put(thread1, elements0);
            }
        };

        stackPlus = new HashMap<Thread, StackTraceElement[]>() {
            {
                put(thread0, elementsPlus);
                put(thread1, elements0);
                put(thread2, elements0);
            }
        };

        stackMinus = new HashMap<Thread, StackTraceElement[]>() {
            {
                put(thread0, elementsMinus);
            }
        };

        stackDif = new HashMap<Thread, StackTraceElement[]>() {
            {
                put(thread0, elementsDif);
            }
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
            instance.createSnapshot(System.currentTimeMillis(), 0);
            fail("Attempt to create an empty snapshot should throw NoDataAvailableException");
        } catch (CPUResultsSnapshot.NoDataAvailableException ex) {
        }
    }

    @Test
    public void testCreateSnapshotOneSample() throws CPUResultsSnapshot.NoDataAvailableException {
        System.out.println("create snapshot : one sample");

        instance.addStacktrace(stack0, 0);
        CPUResultsSnapshot snapshot = instance.createSnapshot(System.currentTimeMillis(), 500000);
        assertFalse(snapshot.collectingTwoTimeStamps);
        assertEquals(instance.methodInfos.size(), snapshot.nInstrMethods);
    }

    @Test
    public void testCreateSnapshotNoChanges() throws CPUResultsSnapshot.NoDataAvailableException {
        System.out.println("create snapshot : two samples");

        instance.addStacktrace(stack0, 0);
        instance.addStacktrace(stack0, 500000);

        CPUResultsSnapshot snapshot = instance.createSnapshot(System.currentTimeMillis(), 1000000);
        assertFalse(snapshot.collectingTwoTimeStamps);
        assertEquals(instance.methodInfos.size(), snapshot.nInstrMethods);
    }

    @Test
    public void testCreateSnapshotMinus() throws CPUResultsSnapshot.NoDataAvailableException {
        System.out.println("create snapshot : minus");

        instance.addStacktrace(stack0, 0);
        instance.addStacktrace(stackMinus, 500000);

        CPUResultsSnapshot snapshot = instance.createSnapshot(System.currentTimeMillis(), 1000000);
        assertFalse(snapshot.collectingTwoTimeStamps);
        assertEquals(instance.methodInfos.size(), snapshot.nInstrMethods);
    }

    @Test
    public void testCreateSnapshotPlus() throws CPUResultsSnapshot.NoDataAvailableException {
        System.out.println("create snapshot : plus");

        instance.addStacktrace(stack0, 0);
        instance.addStacktrace(stackPlus, 500000);

        CPUResultsSnapshot snapshot = instance.createSnapshot(System.currentTimeMillis(), 1000000);
        assertFalse(snapshot.collectingTwoTimeStamps);
        assertEquals(instance.methodInfos.size(), snapshot.nInstrMethods);
    }

    @Test
    public void testCreateSnapshotPlusMinus() throws CPUResultsSnapshot.NoDataAvailableException {
        System.out.println("create snapshot : plus->minus");

        instance.addStacktrace(stack0, 0);
        instance.addStacktrace(stackPlus, 500000);
        instance.addStacktrace(stackMinus, 1000000);

        CPUResultsSnapshot snapshot = instance.createSnapshot(System.currentTimeMillis(), 1500000);
        assertFalse(snapshot.collectingTwoTimeStamps);
        assertEquals(instance.methodInfos.size(), snapshot.nInstrMethods);
    }

    @Test
    public void testCreateSnapshotMinusPlus() throws CPUResultsSnapshot.NoDataAvailableException {
        System.out.println("create snapshot : minus->plus");

        instance.addStacktrace(stack0, 0);
        instance.addStacktrace(stackMinus, 500000);
        instance.addStacktrace(stackPlus, 1000000);

        CPUResultsSnapshot snapshot = instance.createSnapshot(System.currentTimeMillis(), 1500000);
        assertFalse(snapshot.collectingTwoTimeStamps);
        assertEquals(instance.methodInfos.size(), snapshot.nInstrMethods);
    }

    @Test
    public void testAddStacktrace() {
        System.out.println("add stacktrace");

        instance.addStacktrace(stack0, 0);
        assertTrue(instance.methodInfos.size() == elements0.length);
        assertTrue(instance.threadIds.size() == stack0.size());
        assertTrue(instance.threadNames.size() == stack0.size());
        assertFalse(-1L == instance.currentDumpTimeStamp);
        //assertFalse(-1L == instance.firstDumpTimeStamp);
    }

    @Test
    public void testAddStacktraceDuplicate() {
        System.out.println("add stacktrace : duplicate");

        long stamp = 0;
        instance.addStacktrace(stack0, stamp);

        int miSize = instance.methodInfos.size();
        int tIdSize = instance.threadIds.size();
        long timestamp = instance.currentDumpTimeStamp;

        instance.addStacktrace(stack0, stamp);

        assertEquals(tIdSize, instance.threadIds.size());
        assertEquals(miSize, instance.methodInfos.size());
        assertEquals(timestamp, instance.currentDumpTimeStamp);
    }

    @Test
    public void testAddStacktracePlus() {
        System.out.println("add stacktrace : plus");

        instance.addStacktrace(stack0, 0);

        long timestamp = 500000;

        instance.addStacktrace(stackPlus, timestamp);

        assertEquals(Math.max(stack0.size(), stackPlus.size()), instance.threadIds.size());
        assertEquals(Math.max(elements0.length, elementsPlus.length), instance.methodInfos.size());
        assertEquals(timestamp, instance.currentDumpTimeStamp);
    }

    @Test
    public void testAddStacktracePlusWaiting() {
        System.out.println("add stacktrace : plus/waiting");

        instance.addStacktrace(stack0, 0);

        long timestamp = 500000;
        thread0.setState(Thread.State.WAITING);

        instance.addStacktrace(stackPlus, timestamp);

        assertEquals(Math.max(stack0.size(), stackPlus.size()), instance.threadIds.size());
        assertEquals(Math.max(elements0.length, elementsPlus.length), instance.methodInfos.size());
        assertEquals(timestamp, instance.currentDumpTimeStamp);
    }

    @Test
    public void testAddStacktracePlusWaitingThread() {
        System.out.println("add stacktrace : plus/waiting; additional thread");

        instance.addStacktrace(stack0, 0);

        long timestamp = 500000;
        thread2.setState(Thread.State.WAITING);

        instance.addStacktrace(stackPlus, timestamp);

        assertEquals(Math.max(stack0.size(), stackPlus.size()), instance.threadIds.size());
        assertEquals(Math.max(elements0.length, elementsPlus.length), instance.methodInfos.size());
        assertEquals(timestamp, instance.currentDumpTimeStamp);
    }

    @Test
    public void testAddStacktraceWaitingPlus() {
        System.out.println("add stacktrace : waiting/plus");

        thread0.setState(Thread.State.WAITING);
        instance.addStacktrace(stack0, 0);

        long timestamp = 500000;
        thread0.setState(Thread.State.RUNNABLE);

        instance.addStacktrace(stackPlus, timestamp);

        assertEquals(Math.max(stack0.size(), stackPlus.size()), instance.threadIds.size());
        assertEquals(Math.max(elements0.length, elementsPlus.length), instance.methodInfos.size());
        assertEquals(timestamp, instance.currentDumpTimeStamp);
    }

    @Test
    public void testAddStacktraceMinus() {
        System.out.println("add stacktrace : minus");

        instance.addStacktrace(stack0, 0);

        long timestamp = 500000;

        instance.addStacktrace(stackMinus, timestamp);

        assertEquals(Math.max(stack0.size(), stackMinus.size()), instance.threadIds.size());
        assertEquals(Math.max(elements0.length, elementsMinus.length), instance.methodInfos.size());
        assertEquals(timestamp, instance.currentDumpTimeStamp);
    }

    @Test
    public void testAddStacktraceMinusWaiting() {
        System.out.println("add stacktrace : minus/waiting");

        instance.addStacktrace(stack0, 0);

        long timestamp = 500000;
        thread0.setState(Thread.State.WAITING);
        instance.addStacktrace(stackMinus, timestamp);

        assertEquals(Math.max(stack0.size(), stackMinus.size()), instance.threadIds.size());
        assertEquals(Math.max(elements0.length, elementsMinus.length), instance.methodInfos.size());
        assertEquals(timestamp, instance.currentDumpTimeStamp);
    }

    @Test
    public void testAddStacktraceMinusWaitingThread() {
        System.out.println("add stacktrace : minus/waiting; additional thread");

        thread1.setState(Thread.State.WAITING);
        instance.addStacktrace(stack0, 0);

        long timestamp = 500000;
        thread0.setState(Thread.State.WAITING);
        instance.addStacktrace(stackMinus, timestamp);

        assertEquals(Math.max(stack0.size(), stackMinus.size()), instance.threadIds.size());
        assertEquals(Math.max(elements0.length, elementsMinus.length), instance.methodInfos.size());
        assertEquals(timestamp, instance.currentDumpTimeStamp);
    }

    @Test
    public void testAddStacktraceWaitingMinus() {
        System.out.println("add stacktrace : waiting/minus");

        thread0.setState(Thread.State.WAITING);
        instance.addStacktrace(stack0, 0);

        long timestamp = 500000;
        thread0.setState(Thread.State.RUNNABLE);
        instance.addStacktrace(stackMinus, timestamp);

        assertEquals(Math.max(stack0.size(), stackMinus.size()), instance.threadIds.size());
        assertEquals(Math.max(elements0.length, elementsMinus.length), instance.methodInfos.size());
        assertEquals(timestamp, instance.currentDumpTimeStamp);
    }

    @Test
    public void testAddStacktraceDif() {
        System.out.println("add stacktrace : diff");

        instance.addStacktrace(stack0, 0);

        long timestamp = 500000;

        instance.addStacktrace(stackDif, timestamp);

        assertEquals(Math.max(stack0.size(), stackDif.size()), instance.threadIds.size());
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

        instance.addStacktrace(stack0, 0);

        long timestamp = 500000;
        thread0.setState(Thread.State.WAITING);
        
        instance.addStacktrace(stackDif, timestamp);

        assertEquals(Thread.State.WAITING, instance.lastThreadStates.get(thread0));

        assertEquals(Math.max(stack0.size(), stackDif.size()), instance.threadIds.size());
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

        instance.addStacktrace(stack0, 0);

        long timestamp = 500000;
        thread0.setState(Thread.State.WAITING);

        instance.addStacktrace(stackDif, timestamp);

        thread0.setState(Thread.State.BLOCKED);

        timestamp += 500000;

        instance.addStacktrace(stack0, timestamp);

        assertEquals(Thread.State.BLOCKED, instance.lastThreadStates.get(thread0));

        assertEquals(Math.max(stack0.size(), stackDif.size()), instance.threadIds.size());
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

        instance.addStacktrace(stack0, 0);

        long timestamp = 500000;
        thread0.setState(Thread.State.BLOCKED);

        instance.addStacktrace(stackDif, timestamp);

        thread0.setState(Thread.State.WAITING);

        timestamp += 500000;

        instance.addStacktrace(stack0, timestamp);

        assertEquals(Thread.State.WAITING, instance.lastThreadStates.get(thread0));

        assertEquals(Math.max(stack0.size(), stackDif.size()), instance.threadIds.size());
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

        thread0.setState(Thread.State.WAITING);
        instance.addStacktrace(stack0, 0);

        long timestamp = 500000;


        instance.addStacktrace(stackDif, timestamp);

        assertEquals(Math.max(stack0.size(), stackDif.size()), instance.threadIds.size());
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

        thread0.setState(Thread.State.WAITING);
        instance.addStacktrace(stack0, 0);

        long timestamp = 500000;

        thread0.setState(Thread.State.RUNNABLE);
        instance.addStacktrace(stackDif, timestamp);

        assertEquals(Math.max(stack0.size(), stackDif.size()), instance.threadIds.size());
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

        thread0.setState(Thread.State.NEW);

        try {
            instance.addStacktrace(stack0, 500000);
            fail();
        } catch (IllegalStateException ex) {}
    }

    @Test
    public void testAddStackTraceWasTerminated() {
        System.out.println("add stacktrace : terminated->runnable");

        thread0.setState(Thread.State.TERMINATED);

        try {
            instance.addStacktrace(stack0, 0);
            thread0.setState(Thread.State.RUNNABLE);
            instance.addStacktrace(stack0, 500000);
            fail();
        } catch (IllegalStateException ex) {}
    }


    @Test
    public void testAddStackTraceRunnable() {
        System.out.println("add stacktrace : runnable");

        thread0.setState(Thread.State.RUNNABLE);

        instance.addStacktrace(stack0, 500000);

        assertEquals(500000, instance.currentDumpTimeStamp);
        assertEquals(Thread.State.RUNNABLE, instance.lastThreadStates.get(thread0));
    }

    @Test
    public void testAddStackTraceWaiting() {
        System.out.println("add stacktrace : waiting");

        thread0.setState(Thread.State.WAITING);

        instance.addStacktrace(stack0, 500000);

        assertEquals(500000, instance.currentDumpTimeStamp);
        assertEquals(Thread.State.WAITING, instance.lastThreadStates.get(thread0));
    }

    @Test
    public void testAddStackTraceTimedWaiting() {
        System.out.println("add stacktrace : timed waiting");

        thread0.setState(Thread.State.TIMED_WAITING);

        instance.addStacktrace(stack0, 500000);

        assertEquals(500000, instance.currentDumpTimeStamp);
        assertEquals(Thread.State.TIMED_WAITING, instance.lastThreadStates.get(thread0));
    }

    @Test
    public void testAddStackTraceBlocked() {
        System.out.println("add stacktrace : blocked");

        thread0.setState(Thread.State.BLOCKED);

        instance.addStacktrace(stack0, 500000);

        assertEquals(500000, instance.currentDumpTimeStamp);
        assertEquals(Thread.State.BLOCKED, instance.lastThreadStates.get(thread0));
    }

    @Test
    public void testAddStackTraceTerminated() {
        System.out.println("add stacktrace : terminated");

        thread0.setState(Thread.State.TERMINATED);

        instance.addStacktrace(stack0, 500000);

        assertEquals(500000, instance.currentDumpTimeStamp);
        assertEquals(Thread.State.TERMINATED, instance.lastThreadStates.get(thread0));
    }

    @Test
    public void testAddStackTraceWaitRun() {
        System.out.println("add stacktrace : wait->run");

        instance.addStacktrace(stack0, 0);
        thread0.setState(Thread.State.WAITING);
        
        instance.addStacktrace(stack0, 500000);

        assertEquals(500000, instance.currentDumpTimeStamp);
        assertEquals(Thread.State.WAITING, instance.lastThreadStates.get(thread0));

        thread0.setState(Thread.State.RUNNABLE);
        instance.addStacktrace(stack0, 1000000);

        assertEquals(1000000, instance.currentDumpTimeStamp);
        assertEquals(Thread.State.RUNNABLE, instance.lastThreadStates.get(thread0));
    }

    @Test
    public void testAddStackTraceWaitWait() {
        System.out.println("add stacktrace : wait->wait");

        instance.addStacktrace(stack0, 0);
        thread0.setState(Thread.State.WAITING);

        instance.addStacktrace(stack0, 500000);

        assertEquals(500000, instance.currentDumpTimeStamp);
        assertEquals(Thread.State.WAITING, instance.lastThreadStates.get(thread0));
        instance.addStacktrace(stack0, 1000000);

        assertEquals(1000000, instance.currentDumpTimeStamp);
        assertEquals(Thread.State.WAITING, instance.lastThreadStates.get(thread0));
    }

    @Test
    public void testAddStackTraceWaitBlocked() {
        System.out.println("add stacktrace : wait->blocked");

        instance.addStacktrace(stack0, 0);
        thread0.setState(Thread.State.WAITING);

        instance.addStacktrace(stack0, 500000);

        assertEquals(500000, instance.currentDumpTimeStamp);
        assertEquals(Thread.State.WAITING, instance.lastThreadStates.get(thread0));
        thread0.setState(Thread.State.BLOCKED);
        instance.addStacktrace(stack0, 1000000);

        assertEquals(1000000, instance.currentDumpTimeStamp);
        assertEquals(Thread.State.BLOCKED, instance.lastThreadStates.get(thread0));
    }



    @Test
    public void testReset() {
        System.out.println("reset");

        instance.addStacktrace(Thread.getAllStackTraces(), System.nanoTime());
        instance.addStacktrace(Thread.getAllStackTraces(), System.nanoTime());

        instance.reset();
        assertTrue(instance.methodInfos.size() == 0);
        assertTrue(instance.threadIds.size() == 0);
        assertTrue(instance.threadNames.size() == 0);
        assertEquals(-1L, instance.currentDumpTimeStamp);
        //assertEquals(-1L, instance.firstDumpTimeStamp);
        assertEquals(0, instance.stackTraceCount);

        try {
            instance.createSnapshot(System.currentTimeMillis(), 0);
            fail();
        } catch (CPUResultsSnapshot.NoDataAvailableException ex) {
        }
    }

    @Test
    public void testIgnoredThreadName() {
        System.out.println("ignored thread name");

        String ignoredThread = "Thread 0";
        instance.setIgnoredThreads(Collections.singleton(ignoredThread));

        instance.addStacktrace(stack0, 0);
        assertFalse(instance.threadNames.contains(ignoredThread));
    }
}