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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An interval based scheduler service
 * Used to execute various tasks at the predefined interval
 * There is supposed to be only one instance of this class accesible vie <code>getSharedInstance()</code>
 * @author Jaroslav Bachorik
 */
public class Scheduler implements PropertyChangeListener {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------
    private static final Logger LOGGER = Logger.getLogger(Scheduler.class.getName());
    private static final Scheduler INSTANCE = new Scheduler();

    //~ Instance fields ----------------------------------------------------------------------------------------------------------
    private final Map<Quantum, Set<WeakReference<DefaultScheduledTask>>> interval2recevier = new HashMap<Quantum, Set<WeakReference<DefaultScheduledTask>>>();
    private final ScheduledExecutorService schedulerService = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
    private final ExecutorService intermediateTaskService = Executors.newCachedThreadPool();
    private final ExecutorService dispatcher = Executors.newCachedThreadPool();
    
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
            intermediateTaskService.submit(new Runnable() {
                public void run() {
                    task.onSchedule(System.currentTimeMillis());
                }
            });
        }

        DefaultScheduledTask scheduled = new DefaultScheduledTask(interval, task);
        add(scheduled, interval);

        return scheduled;
    }

    /**
     * Unschedules an instance of <code>Scheduled</code> class
     * @param task The task to be unscheduled
     */
    public final void unschedule(final ScheduledTask task) {
        if (task == null) return;
        remove((DefaultScheduledTask) task, task.getInterval());
        task.suspend();
    }

    /**
     * @see PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
     */
    public void propertyChange(PropertyChangeEvent evt) {
        DefaultScheduledTask task = (DefaultScheduledTask) evt.getSource();
        reschedule(task, (Quantum) evt.getOldValue(), (Quantum) evt.getNewValue());
    }

    private void add(final DefaultScheduledTask task, final Quantum interval) {
        if (task == null || interval == null) return;
        
        synchronized (interval2recevier) {
            Set<WeakReference<DefaultScheduledTask>> receivers = interval2recevier.get(interval);

            if (receivers == null) {
                Set<WeakReference<DefaultScheduledTask>> newReceivers = new HashSet<WeakReference<DefaultScheduledTask>>();
                newReceivers.add(new WeakReference<DefaultScheduledTask>(task));
                interval2recevier.put(interval, newReceivers);
                if (!interval.equals(Quantum.SUSPENDED)) {
                    schedulerService.scheduleAtFixedRate(new Runnable() {

                        public void run() {
                            try {
                                if (LOGGER.isLoggable(Level.FINEST)) {
                                    LOGGER.finest("Notifying scheduled tasks at interval " + interval); // NOI18N
                                }

                                long timeStamp = System.currentTimeMillis();
                                Set<WeakReference<DefaultScheduledTask>> myReceivers = Collections.EMPTY_SET;

                                synchronized (interval2recevier) {
                                    if (!interval2recevier.containsKey(interval)) {
                                        interval2recevier.put(interval, Collections.EMPTY_SET); // sanitize the dead interval
                                    }
                                    myReceivers = new HashSet<WeakReference<DefaultScheduledTask>>(interval2recevier.get(interval));
                                    if (LOGGER.isLoggable(Level.FINEST)) {
                                        LOGGER.finest(((myReceivers != null) ? myReceivers.size() : "0") + " scheduled tasks for interval " + interval);    // NOI18N
                                    }
                                }

                                int deadRefCounter = notifyReceivers(timeStamp, myReceivers);

                                if (deadRefCounter > 0) {
                                    Set<WeakReference<DefaultScheduledTask>> cleansed = cleanDeadRefs(myReceivers);

                                    synchronized (interval2recevier) {
                                        interval2recevier.remove(interval);
                                        interval2recevier.put(interval, cleansed);
                                    }
                                }
                                if (LOGGER.isLoggable(Level.FINEST)) {
                                    LOGGER.finest("Finished");  // NOI18N
                                }
                            } catch (Exception e) {
                                if (LOGGER.isLoggable(Level.WARNING)) {
                                    LOGGER.log(Level.WARNING, "Exception in scheduler", e); // NOI18N
                                }
                            }
                        }
                    }, interval.interval, interval.interval, interval.unit);
                }
            } else {
                receivers.add(new WeakReference<DefaultScheduledTask>(task));
            }
        }

        task.addPropertyChangeListener(ScheduledTask.INTERVAL_PROPERTY, this);
    }

    private static Set<WeakReference<DefaultScheduledTask>> cleanDeadRefs(Set<WeakReference<DefaultScheduledTask>> tasks) {
        Set<WeakReference<DefaultScheduledTask>> newSet = new HashSet<WeakReference<DefaultScheduledTask>>();

        for (WeakReference<DefaultScheduledTask> task : tasks) {
            if (task.get() != null) {
                newSet.add(task);
            }
        }

        return newSet;
    }

    private int notifyReceivers(final long timeStamp, Set<WeakReference<DefaultScheduledTask>> myReceivers) {
        int deadRefCounter = 0;

        for (WeakReference<DefaultScheduledTask> rcvRef : myReceivers) {
            final DefaultScheduledTask rcv = rcvRef.get();

            if (rcv == null) {
                deadRefCounter++;

                continue;
            }
            if (rcv.isSuspended()) {
                continue;
            }

            dispatcher.submit(new Runnable() {
                public void run() {
                    rcv.onSchedule(timeStamp);
                }
            });
        }

        return deadRefCounter;
    }

    private void remove(final DefaultScheduledTask task, Quantum interval) {
        task.removePropertyChangeListener(ScheduledTask.INTERVAL_PROPERTY, this);

        synchronized (interval2recevier) {
            Set<WeakReference<DefaultScheduledTask>> receivers = interval2recevier.get(interval);

            if (receivers != null) {
                int deadRefCounter = 0;

                for (WeakReference<DefaultScheduledTask> rcvRef : receivers) {
                    DefaultScheduledTask rcv = rcvRef.get();

                    if (rcv == null) {
                        deadRefCounter++;

                        continue;
                    }

                    if (rcv.equals(task)) {
                        boolean taskAlive = !rcv.isSuspended();
                        if (taskAlive) {
                            rcv.suspend();
                        }
                        receivers.remove(rcvRef);
                        if (taskAlive) {
                            rcv.resume();
                        }
                        break;
                    }
                }

                if (deadRefCounter > 0) {
                    receivers = cleanDeadRefs(receivers);
                    interval2recevier.remove(interval);

                    if (!receivers.isEmpty()) {
                        interval2recevier.put(interval, receivers);
                    }
                } else {
                    if (receivers.isEmpty()) {
                        interval2recevier.remove(interval);
                    }
                }
            }
        }
    }

    private void reschedule(final DefaultScheduledTask task, Quantum oldInterval, Quantum newInterval) {
        remove(task, oldInterval);
        // when rescehduling from suspended state execute the task out of order
        if (oldInterval.equals(Quantum.SUSPENDED)) {
            intermediateTaskService.submit(new Runnable() {
                public void run() {
                    task.onSchedule(System.currentTimeMillis());
                }
            });
        }
        add(task, newInterval);
    }
}
