/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.core.scheduler;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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
public class SchedulerTest {
    private final Collection<ScheduledTask> stasks = new  ArrayList<>();
    
    public SchedulerTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        stasks.clear();
    }

    @After
    public void tearDown() {
        for(ScheduledTask stask : stasks) {
            Scheduler.sharedInstance().unschedule(stask);
        }
    }

    /**
     * Test of sharedInstance method, of class Scheduler.
     */
    @Test
    public void sharedInstance() {
        System.out.println("sharedInstance");

        Scheduler result1 = Scheduler.sharedInstance();
        Scheduler result2 = Scheduler.sharedInstance();
        assertTrue(result1 == result2); // shared instance MUST remain the same across all calls        
    }

    /**
     * Test of schedule method, immediate execution, of class Scheduler.
     */
    @Test
    public void scheduleImmediate() {
        System.out.println("schedule, immediate");
        final CountDownLatch barrier = new CountDownLatch(1);
        SchedulerTask task = new SchedulerTask() {

            public void onSchedule(long timeStamp) {
                barrier.countDown();
            }
        };
        Quantum interval = Quantum.seconds(2000);
        Scheduler instance = Scheduler.sharedInstance();
        ScheduledTask scheduled = instance.schedule(task, interval, true);
        stasks.add(scheduled);
        try {
            boolean executed = barrier.await(1000, TimeUnit.SECONDS);
            assertTrue(executed);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Test of schedule method, immediate execution, blocking in the scheduled task, of class Scheduler.
     */
    @Test
    public void scheduleImmediateBlocking() {
        System.out.println("schedule, immediate, blocking");
        final CountDownLatch barrier = new CountDownLatch(1);
        SchedulerTask task = new SchedulerTask() {

            public void onSchedule(long timeStamp) {
                try {
                    barrier.countDown();
                    Thread.sleep(10000000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        };
        Quantum interval = Quantum.seconds(2000);
        Scheduler instance = Scheduler.sharedInstance();
        ScheduledTask scheduled = instance.schedule(task, interval, true);
        stasks.add(scheduled);
        try {
            boolean executed = barrier.await(1000, TimeUnit.SECONDS);
            assertTrue(executed);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Test of schedule method of class Scheduler.
     */
    @Test
    public void schedule() {
        System.out.println("schedule");
        final CountDownLatch barrier = new CountDownLatch(1);
        SchedulerTask task = new SchedulerTask() {

            public void onSchedule(long timeStamp) {
                barrier.countDown();
            }
        };
        Quantum interval = Quantum.seconds(5);
        Scheduler instance = Scheduler.sharedInstance();
        ScheduledTask scheduled = instance.schedule(task, interval, false);
        stasks.add(scheduled);
        try {
            boolean executed = barrier.await(8, TimeUnit.SECONDS);
            assertTrue(executed);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Test of unschedule method, of class Scheduler.
     */
    @Test
    public void unschedule() {
        System.out.println("unschedule");
        final AtomicBoolean executed = new AtomicBoolean(false);
        SchedulerTask task = new SchedulerTask() {

            public void onSchedule(long timeStamp) {
                executed.set(true);
            }
        };
        Scheduler instance = Scheduler.sharedInstance();
        ScheduledTask scheduled = instance.schedule(task, Quantum.seconds(3), false);
        instance.unschedule(scheduled);

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        assertFalse(executed.get());
    }

    /**
     * Test of dynamic rescheduling capability of class Scheduler.
     */
    @Test
    public void dynamicReschedule() {
        System.out.println("dynamicReschedule");
        final CountDownLatch barrier1 = new CountDownLatch(1);
        final CountDownLatch barrier2 = new CountDownLatch(2);

        SchedulerTask task = new SchedulerTask() {

            public void onSchedule(long timeStamp) {
                System.out.println("dynamicReschedule; executing periodic task");
                barrier1.countDown();
                barrier2.countDown();
            }
        };

        final ScheduledTask scheduled = Scheduler.sharedInstance().schedule(task, Quantum.seconds(7), false);
        stasks.add(scheduled);
        try {
            if (!barrier1.await(10, TimeUnit.SECONDS)) {
                fail();
            }
            scheduled.setInterval(Quantum.seconds(2));
            if (!barrier2.await(5, TimeUnit.SECONDS)) {
                fail();
            }
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void dynamicRescheduleSuspended() {
        System.out.println("dynamicReschedule suspended");
        final CountDownLatch barrier1 = new CountDownLatch(1);
        final CountDownLatch barrier2 = new CountDownLatch(5);

        SchedulerTask task = new SchedulerTask() {

            public void onSchedule(long timeStamp) {
                System.out.println("dynamicReschedule; executing periodic task");
                barrier1.countDown();
                barrier2.countDown();
            }
        };

        final ScheduledTask scheduled = Scheduler.sharedInstance().schedule(task, Quantum.SUSPENDED, false);
        stasks.add(scheduled);
        try {
            if (barrier1.await(5, TimeUnit.SECONDS)) {
                fail();
            }
            scheduled.resume();
            if (barrier2.await(2, TimeUnit.SECONDS)) {
                fail();
            }
            scheduled.setInterval(Quantum.seconds(1));
            if (!barrier2.await(8, TimeUnit.SECONDS)) {
                fail();
            }
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void suspendResume() {
        System.out.println("suspend-resume");
        final CountDownLatch barrier1 = new CountDownLatch(1);
        final CountDownLatch barrier2 = new CountDownLatch(5);

        SchedulerTask task = new SchedulerTask() {

            public void onSchedule(long timeStamp) {
                barrier1.countDown();
                barrier2.countDown();
            }
        };

        final ScheduledTask scheduled = Scheduler.sharedInstance().schedule(task, Quantum.seconds(2), false);
        stasks.add(scheduled);
        try {
            if (!barrier1.await(3, TimeUnit.SECONDS)) {
                fail();
            }
            scheduled.suspend();
            if (barrier2.await(3, TimeUnit.SECONDS)) {
                fail();
            }
            scheduled.resume();
            if (!barrier2.await(10, TimeUnit.SECONDS)) {
                fail();
            }
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void suspendSuspend() {
        System.out.println("suspend-suspend");
        final CountDownLatch barrier1 = new CountDownLatch(1);
        final CountDownLatch barrier2 = new CountDownLatch(2);

        SchedulerTask task = new SchedulerTask() {

            public void onSchedule(long timeStamp) {
                barrier1.countDown();
                barrier2.countDown();
            }
        };

        final ScheduledTask scheduled = Scheduler.sharedInstance().schedule(task, Quantum.seconds(2), false);
        stasks.add(scheduled);
        try {
            if (!barrier1.await(3, TimeUnit.SECONDS)) {
                fail();
            }
            scheduled.suspend();
            if (barrier2.await(3, TimeUnit.SECONDS)) {
                fail();
            }
            scheduled.suspend();
            if (barrier2.await(3, TimeUnit.SECONDS)) {
                fail();
            }
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void suspendSuspendResume() {
        System.out.println("suspend-suspend-resume");
        final CountDownLatch barrier1 = new CountDownLatch(1);
        final CountDownLatch barrier2 = new CountDownLatch(2);

        SchedulerTask task = new SchedulerTask() {

            public void onSchedule(long timeStamp) {
                barrier1.countDown();
                barrier2.countDown();
            }
        };

        final ScheduledTask scheduled = Scheduler.sharedInstance().schedule(task, Quantum.seconds(2), false);
        stasks.add(scheduled);
        try {
            if (!barrier1.await(3, TimeUnit.SECONDS)) {
                fail();
            }
            scheduled.suspend();
            if (barrier2.await(3, TimeUnit.SECONDS)) {
                fail();
            }
            scheduled.suspend();
            if (barrier2.await(3, TimeUnit.SECONDS)) {
                fail();
            }
            scheduled.resume();
            if (!barrier2.await(3, TimeUnit.SECONDS)) {
                fail();
            }
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
    }
}
