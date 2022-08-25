/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * A default implementation of the <code>ScheduledTask</code>
 * @author Jaroslav Bachorik
 */
class DefaultScheduledTask implements ScheduledTask, SchedulerTask {
    static private final Map<Quantum, WeakReference<SchedulingPipe>> pipeMap = new HashMap<>();

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private final ReadWriteLock intervalLock = new ReentrantReadWriteLock();

    // @GuardedBy intervalLock
    private Quantum interval;
    private SchedulerTask delegateTask;
    private Quantum suspendedFrom = Quantum.SUSPENDED;

    private SchedulingPipe pipe = null;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    DefaultScheduledTask(Quantum interval, SchedulerTask task) {
        delegateTask = task;
        setInterval(interval);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------
    /**
     * @see org.graalvm.visualvm.core.scheduler.ScheduledTask#setInterval(Quantum)
     */
    public void setInterval(Quantum interval) {
        intervalLock.writeLock().lock();

        Quantum oldInterval = interval;

        try {
            oldInterval = this.interval;

            synchronized(pipeMap) {
                WeakReference<SchedulingPipe> oldPipeRef = pipeMap.get(oldInterval);
                WeakReference<SchedulingPipe> newPipeRef = pipeMap.get(interval);

                SchedulingPipe oldPipe = oldPipeRef != null ? oldPipeRef.get() : null;
                SchedulingPipe newPipe = newPipeRef != null ? newPipeRef.get() : null;
                if (oldPipe != null) {
                    oldPipe.removeTask(this);
                }
                if (newPipe == null && interval != Quantum.SUSPENDED) {
                    newPipe = new SchedulingPipe(interval);
                    pipeMap.put(interval, new WeakReference<>(newPipe));
                }
                if (newPipe != null) {
                    newPipe.addTask(this);
                }
            }
            this.interval = interval;
        } finally {
            intervalLock.writeLock().unlock();
        }

        pcs.firePropertyChange(INTERVAL_PROPERTY, oldInterval, interval);
    }

    /**
     * @see org.graalvm.visualvm.core.scheduler.ScheduledTask#getInterval()
     */
    public Quantum getInterval() {
        intervalLock.readLock().lock();

        try {
            return interval;
        } finally {
            intervalLock.readLock().unlock();
        }
    }

    /**
     * @see PropertyChangeSupport#addPropertyChangeListener(java.lang.String, java.beans.PropertyChangeListener)
     */
    public synchronized void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(propertyName, listener);
    }

    /**
     * @see PropertyChangeSupport#addPropertyChangeListener(java.beans.PropertyChangeListener)
     */
    public synchronized void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    /**
     * @see PropertyChangeSupport#hasListeners(java.lang.String)
     */
    public synchronized boolean hasListeners(String propertyName) {
        return pcs.hasListeners(propertyName);
    }

    /**
     * @see PropertyChangeSupport#removePropertyChangeListener(java.lang.String, java.beans.PropertyChangeListener)
     */
    public synchronized void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(propertyName, listener);
    }

    /**
     * @see PropertyChangeSupport#removePropertyChangeListener(java.beans.PropertyChangeListener)
     */
    public synchronized void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    /**
     * @see org.graalvm.visualvm.core.scheduler.ScheduledTask#suspend()
     */
    public void suspend() {
        if (suspendedFrom.equals(Quantum.SUSPENDED)) suspendedFrom = getInterval();
        setInterval(Quantum.SUSPENDED);
    }

    /**
     * @see ScheduledTask#resume()
     */
    public void resume() {
        setInterval(suspendedFrom);
        suspendedFrom = Quantum.SUSPENDED;
    }

    /**
     * @see ScheduledTask#isSuspended()
     */
    public boolean isSuspended() {
        return interval.equals(Quantum.SUSPENDED);
    }
    
    /**
     * @see org.graalvm.visualvm.core.scheduler.SchedulerTask#onSchedule(long)
     */
    public void onSchedule(long timeStamp) {
        delegateTask.onSchedule(timeStamp);
    }

    void setPipe(SchedulingPipe pipe) {
        this.pipe = pipe;
    }

    SchedulingPipe getPipe() {
        return pipe;
    }
}
