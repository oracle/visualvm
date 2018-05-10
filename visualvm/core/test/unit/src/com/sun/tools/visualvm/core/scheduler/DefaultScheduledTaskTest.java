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
import java.beans.PropertyChangeListener;
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
public class DefaultScheduledTaskTest {
    private SchedulerTask dummyTask = new SchedulerTask() {

        public void onSchedule(long timeStamp) {
            // do nothing
        }
    };
    
    private DefaultScheduledTask instance;
    final private static Quantum DEFAULT_INTERVAL = Quantum.seconds(10);
    
    public DefaultScheduledTaskTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        instance = new DefaultScheduledTask(DEFAULT_INTERVAL, dummyTask);
    }

    @After
    public void tearDown() {
        instance = null;
    }

    /**
     * Test of setInterval method, of class DefaultScheduledTask.
     */
    @Test
    public void setInterval() {
        System.out.println("setInterval");
        Quantum interval = Quantum.SUSPENDED;
        assertEquals(DEFAULT_INTERVAL, instance.getInterval());
        instance.setInterval(interval);
        assertEquals(interval, instance.getInterval());
    }

    /**
     * Test of suspend method, of class DefaultScheduledTask.
     */
    @Test
    public void suspend() {
        System.out.println("suspend");
        instance.suspend();
        assertEquals(Quantum.SUSPENDED, instance.getInterval());        
    }

    /**
     * Test of resume method, of class DefaultScheduledTask.
     */
    @Test
    public void resume() {
        System.out.println("resume");
        instance.suspend();
        instance.resume();
        assertEquals(DEFAULT_INTERVAL, instance.getInterval());
    }

    /**
     * Test of isSuspended method, of class DefaultScheduledTask.
     */
    @Test
    public void isSuspended() {
        System.out.println("isSuspended");
        assertFalse(instance.isSuspended());
        instance.suspend();
        assertTrue(instance.isSuspended());
        instance.resume();
        assertFalse(instance.isSuspended());
    }
}