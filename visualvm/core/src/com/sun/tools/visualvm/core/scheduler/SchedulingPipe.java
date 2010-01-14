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

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Scheduling pipe maintains a list of {@linkplain ScheduledTask} instances
 * for a certain scheduling interval
 * <p>It allows addition and removal of the tasks and takes care of creating
 * and releasing appropriate scheduled executors</p>
 * 
 * @author Jaroslav Bachorik <jaroslav.bachorik@sun.com>
 */
final class SchedulingPipe {
    private static final Logger LOGGER = Logger.getLogger(SchedulingPipe.class.getName());

    final private Object pipeLock = new Object();
    final private ReadWriteLock tasksLock = new ReentrantReadWriteLock();

    // @GuardedBy pipeLock
    private ScheduledFuture pipeFuture = null;

    final private static ScheduledExecutorService schedulerService = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
    final private static ExecutorService dispatcher = Executors.newCachedThreadPool();
    
    // @GuardedBy tasksLock
    final private Set<WeakReference<DefaultScheduledTask>> tasks = new HashSet<WeakReference<DefaultScheduledTask>>();

    private Quantum interval;

    SchedulingPipe(Quantum interval) {
        this.interval = interval;
    }

    void addTask(DefaultScheduledTask task) {
        try {
            tasksLock.writeLock().lock();
            if (tasks.isEmpty()) {
                startPipe();
            }
            tasks.add(new WeakReference<DefaultScheduledTask>(task));
        } finally {
            tasksLock.writeLock().unlock();
        }
    }

    private void startPipe() {
        synchronized (pipeLock) {
            pipeFuture = schedulerService.scheduleAtFixedRate(new Runnable() {

                public void run() {
                    try {
                        tasksLock.writeLock().lock();
                        final long timeStamp = System.currentTimeMillis();
                        for (Iterator<WeakReference<DefaultScheduledTask>> iter = tasks.iterator(); iter.hasNext();) {
                            WeakReference<DefaultScheduledTask> ref = iter.next();
                            final DefaultScheduledTask t = ref.get();
                            if (t != null) {
                                dispatcher.submit(new Runnable() {

                                    public void run() {
                                        try {
                                            t.onSchedule(timeStamp);
                                        } catch (Throwable e) {
                                            LOGGER.log(Level.SEVERE, null, e);
                                        }
                                    }
                                });
                            } else {
                                iter.remove();
                            }
                        }
                        if (tasks.isEmpty()) {
                            synchronized (pipeLock) {
                                pipeFuture.cancel(false);
                                pipeFuture = null;
                            }
                        }
                    } finally {
                        tasksLock.writeLock().unlock();
                    }
                }
            }, interval.interval, interval.interval, interval.unit);
        }
    }

    void removeTask(DefaultScheduledTask task) {
        try {
            tasksLock.writeLock().lock();
            for(Iterator<WeakReference<DefaultScheduledTask>> iter = tasks.iterator();iter.hasNext();) {
                WeakReference<DefaultScheduledTask> ref = iter.next();
                DefaultScheduledTask t = ref.get();
                if (t == null || t.equals(task)) {
                    iter.remove();
                }
            }
        } finally {
            tasksLock.writeLock().unlock();
        }
        if (tasks.isEmpty()) {
            synchronized(pipeLock) {
                pipeFuture.cancel(false);
                pipeFuture = null;
            }
        }
    }
}
