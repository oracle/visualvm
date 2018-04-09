/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
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
 * Portions Copyrighted 2012 Sun Microsystems, Inc.
 */
package org.netbeans.lib.profiler.ui;

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
