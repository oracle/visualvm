/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.ui;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Jaroslav Bachorik <jaroslav.bachorik@oracle.com>
 */
public class SwingWorkerTest {

    public SwingWorkerTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of execute method, of class SwingWorker.
     */
    @Test
    public void testExecute() throws Exception {
        System.out.println("execute");
        final boolean[] executed = new boolean[]{false};
        final CountDownLatch latch = new CountDownLatch(1);
        SwingWorker instance = new SwingWorkerImpl(0, true, null, new Runnable() {
            @Override
            public void run() {
                executed[0] = true;
            }
        }, new Runnable() {

            @Override
            public void run() {
                latch.countDown();
            }
        }, null, null);
        instance.execute();
        latch.await(1, TimeUnit.SECONDS);
        assertTrue(executed[0]);
    }

    /**
     * Test of cancel method, of class SwingWorker.
     */
    @Test
    public void testCancel() throws Exception {
        System.out.println("cancel");
        final boolean[] canceled = new boolean[]{false};
        final boolean[] done = new boolean[]{false};
        final CountDownLatch latch = new CountDownLatch(1);
        SwingWorker instance = new SwingWorkerImpl(6000, true, null, new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                }
            }
        }, new Runnable() {

            @Override
            public void run() {
                done[0] = true;
            }
        }, new Runnable() {

            @Override
            public void run() {
                canceled[0] = true;
                latch.countDown();
            }
        }, null);
        instance.execute();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
        }
        instance.cancel();
        latch.await(5, TimeUnit.SECONDS);
        assertTrue(canceled[0]);
        assertFalse(done[0]);
    }

    /**
     * Test of nonResponding method, of class SwingWorker.
     */
    @Test
    public void testNonResponding() throws Exception {
        System.out.println("nonResponding");
        final boolean[] waiting = new boolean[]{false};
        final CountDownLatch latch = new CountDownLatch(1);
        SwingWorker instance = new SwingWorkerImpl(500, true, null, new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                }
            }
        }, new Runnable() {

            @Override
            public void run() {
                latch.countDown();
            }
        }, null, 
        new Runnable() {

            @Override
            public void run() {
                waiting[0] = true;
            }
        });
        instance.execute();
        latch.await(4, TimeUnit.SECONDS);
        
        assertTrue(waiting[0]);
    }
    
    @Test
    public void testSharedSemaphore() throws Exception  {
        System.out.println("sharedSemaphore");
        Semaphore s = new Semaphore(1);
        final AtomicInteger counter = new AtomicInteger(0);
        final CountDownLatch latch = new CountDownLatch(2);
        
        final SwingWorker sw1 = new SwingWorkerImpl(0, true, s, new Runnable() {

            @Override
            public void run() {
                counter.incrementAndGet();
                try {
                    Thread.sleep(312);
                } catch (InterruptedException e) {
                }
                counter.decrementAndGet();
            }
        }, new Runnable() {

            @Override
            public void run() {
                latch.countDown();
            }
        }, null, null);
        SwingWorker sw2 = new SwingWorkerImpl(0, true, s, new Runnable() {

            @Override
            public void run() {
                counter.incrementAndGet();
                sw1.execute();                
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }
                counter.decrementAndGet();
            }
        }, new Runnable() {

            @Override
            public void run() {
                latch.countDown();
            }
        }, null, null);
        
        sw2.execute();
        
        latch.await(3, TimeUnit.SECONDS);
        
        assertEquals(0, counter.get());
    }

    public class SwingWorkerImpl extends SwingWorker {
        final private Runnable task, onDone, onCancel, waiting;
        
        final private int warmup;

        public SwingWorkerImpl(int warmup, boolean forceEQ, Semaphore throughputSemaphore, Runnable task, Runnable onDone, Runnable onCancel, Runnable waiting) {
            super(forceEQ, throughputSemaphore);
            this.warmup = warmup;
            this.task = task;
            this.onDone = onDone;
            this.onCancel = onCancel;
            this.waiting = waiting;
        }
        
        public void doInBackground() {
            if (task != null) {
                task.run();
            }
        }

        @Override
        protected int getWarmup() {
            return warmup;
        }

        @Override
        protected void done() {
            if (onDone != null) {
                onDone.run();
            }
        }

        @Override
        protected void cancelled() {
            if (onCancel != null) {
                onCancel.run();
            }
        }

        @Override
        protected void nonResponding() {
            if (waiting != null) {
                waiting.run();
            }
        }
        
        
    }
}
