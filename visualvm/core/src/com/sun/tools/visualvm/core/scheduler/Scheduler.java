/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */
package com.sun.tools.visualvm.core.scheduler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * An interval based scheduler service
 * Used to execute various tasks at the predefined interval
 * There is supposed to be only one instance of this class accesible vie <code>getSharedInstance()</code>
 * @author Jaroslav Bachorik
 */
public class Scheduler {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------
    private static final Logger LOGGER = Logger.getLogger(Scheduler.class.getName());
    private static final Scheduler INSTANCE = new Scheduler();

    //~ Instance fields ----------------------------------------------------------------------------------------------------------
    private final ExecutorService immediateTaskService = Executors.newCachedThreadPool();
    
    //~ Constructors -------------------------------------------------------------------------------------------------------------
    private Scheduler() {
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------
    /**
     * Singleton accessor
     * @return Returns the shared instance of scheduler service
     */
    public static final Scheduler sharedInstance() {
        return INSTANCE;
    }

    /**
     * Schedules a new task to be executed with the given interval
     * The newly scheduled task is exeucted out-of-order at the moment of scheduling it
     * This operation's result should be cached by the caller;otherwise the task will get unscheduled immediately
     * @param task The task to be executed
     * @param interval The interval to execute the task
     * @return Returns an instance of <code>ScheduledTask</code> that can be used to later modify the interval of execution
     */
    public final ScheduledTask schedule(SchedulerTask task, Quantum interval) {
        return schedule(task, interval, true);
    }

    /**
     * Schedules a new task to be executed with the given interval
     * This operation's result should be cached by the caller;otherwise the task will get unscheduled immediately
     * @param task The task to be executed
     * @param interval The interval to execute the task
     * @param immediate Tells whether the newly scheduled task is exeucted out-of-order at the moment of scheduling it
     * @return Returns an instance of <code>ScheduledTask</code> that can be used to later modify the interval of execution
     */
    public final ScheduledTask schedule(final SchedulerTask task, final Quantum interval, boolean immediate) {
        boolean suspended = interval.equals(Quantum.SUSPENDED);
        if (immediate && !suspended) {
            immediateTaskService.submit(new Runnable() {
                public void run() {
                    task.onSchedule(System.currentTimeMillis());
                }
            });
        }

        DefaultScheduledTask scheduled = new DefaultScheduledTask(interval, task);

        return scheduled;
    }

    /**
     * Unschedules an instance of <code>Scheduled</code> class
     * @param task The task to be unscheduled
     */
    public final void unschedule(final ScheduledTask task) {
        if (task == null) return;
        task.suspend();
    }
}
