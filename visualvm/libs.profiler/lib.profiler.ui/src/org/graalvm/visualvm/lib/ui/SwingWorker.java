/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Mimics the functionality of the SwingWorker from JDK6+
 * @author Jaroslav Bachorik
 */
public abstract class SwingWorker {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static ExecutorService warmupService;
    private static ExecutorService taskService;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final Object warmupLock = new Object();
    private boolean useEQ;
    final private Semaphore throughputSemaphore;
    final private AtomicBoolean cancelFlag = new AtomicBoolean(false);
    final private AtomicBoolean primed= new AtomicBoolean(true);

    //@GuardedBy warmupLock
    private boolean workerRunning;
    private Runnable warmupTimer = new Runnable() {
        public void run() {
            synchronized (warmupLock) {
                try {
                    if (workerRunning) {
                        warmupLock.wait(getWarmup());
                    }

                    if (workerRunning && !isCancelled()) {
                        nonResponding();
                    }
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }
    };


    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Creates a new instance of SwingWorker
     * @param forceEQ When set the corresponding {@linkplain SwingWorker#done() } method is executed on EDT
     */
    public SwingWorker(boolean forceEQ) {
        this(forceEQ, null);
    }

    /**
     * Creates a new instance of SwingWorker with <b>forceEQ=true</b>
     */
    public SwingWorker() {
        this(true, null);
    }
    
    /**
     * Creates a new instance of SwingWorker with <b>forceEQ=true</b>. Allows to control the throughput by a given {@linkplain Semaphore} instance
     * @param throughputSemaphore A semaphore instance used to control the worker throughput
     */
    public SwingWorker(Semaphore throughputSemaphore) {
        this(true, throughputSemaphore);
    }
    /**
     * Creates a new instance of SwingWorker. Allows to control the throughput by a given {@linkplain Semaphore} instance
     * @param forceEQ When set the corresponding {@linkplain SwingWorker#done() } method is executed on EDT
     * @param throughputSemaphore A semaphore instance used to control the worker throughput
     */
    public SwingWorker(boolean forceEQ, Semaphore throughputSemaphore) {
        sinit();
        this.useEQ = forceEQ;
        this.throughputSemaphore = throughputSemaphore;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * Executes the UI task. Starts the background task and handles it's execution cycle.
     * If the background task blocks for more than getWarmup() millis the nonResponding() method is invoked
     * 
     * The background task should check for {@linkplain SwingWorker#isCancelled()} to cancel its execution properly.
     * 
     * <b>Each swing worker instance may be used at most once.</b>
     * @throws IllegalStateException In case of attempted reuse of an instance
     */
    public void execute() {
        if (!primed.compareAndSet(true, false)) {
            throw new IllegalStateException("SwingWorker instance may be used only once");
        }
        postRunnable(new Runnable() {
            public void run() {
                try {
                    if (throughputSemaphore != null) {
                        throughputSemaphore.acquire();
                    }
                    if (!isCancelled()) {
                        synchronized (warmupLock) {
                            workerRunning = true;
                        }

                        warmupService.submit(warmupTimer);

                        try {
                            doInBackground();
                        } catch (Throwable ex) {
                            Logger.getLogger(SwingWorker.class.getName()).log(Level.SEVERE, "SwingWorker", ex);
                        } finally {
                            synchronized (warmupLock) {
                                workerRunning = false;
                                warmupLock.notify();
                            }
                            if (!isCancelled()) {
                                if (useEQ) {
                                    UIUtils.runInEventDispatchThread(new Runnable() {
                                        public void run() {
                                            done();
                                        }
                                    });
                                } else {
                                    done();
                                }
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    if (throughputSemaphore != null) {
                        throughputSemaphore.release();
                    }
                }
            }
        });
    }
    
    /**
     * Cancels the background task.
     * Sets a flag which can be checked by calling {@linkplain SwingWorker#isCancelled()} from the subclass.
     * Does not handle the background task interruption.
     * 
     * @since 1.18
     */
    final public void cancel() {
         if (cancelFlag.compareAndSet(false, true)) {
             cancelled();
             if (throughputSemaphore != null) {
                 throughputSemaphore.release(); // release the semaphore
             }
         }
    }

    /**
     * Used to check for the cancellation status
     * @return Returns the cancellation status
     * 
     * @since 1.18
     */
    final protected boolean isCancelled() {
        return cancelFlag.get();
    }
    
    /**
     * @return Returns a warmup time - time in ms before a "non responding"  message is shown; default is 500ms
     */
    protected int getWarmup() {
        return 500;
    }

    /**
     * Implementors will implement this method to provide the background task logic
     */
    protected abstract void doInBackground();

    /**
     * Executed after the background task had finished
     * It's run in EQ if specified in the constructor
     * It is not called if the task has been cancelled
     */
    protected void done() {
        // override to provide a functionality
    }
    
    /**
     * Called upon task cancellation.
     * Can be used in cases when checking for {@linkplain SwingWorker#isCancelled()} is unwieldy for any reason
     * 
     * @since 1.18
     */
    protected void cancelled() {
        // override to provide a functionality
    }

    /**
     * Called when the background thread lasts longer than the warmup time
     * The implementor must take care of rescheduling on AWT thread if appropriate
     * 
     * It is not called if the task has been cancelled previously
     */
    protected void nonResponding() {
        // override to provide functionality
    }

    protected void postRunnable(Runnable runnable) {
        taskService.submit(runnable);
    }

    static synchronized void sinit() {
        if (warmupService == null) {
            UIUtils.runInEventDispatchThreadAndWait(new Runnable() {
                public void run() {
                    warmupService = Executors.newCachedThreadPool();
                    taskService = Executors.newCachedThreadPool();
                }
            });
        }
    }

}
