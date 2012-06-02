/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
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
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
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
 */

package org.netbeans.lib.profiler.ui;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.SwingUtilities;


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
                        } finally {
                            synchronized (warmupLock) {
                                workerRunning = false;
                                warmupLock.notify();
                            }
                            if (!isCancelled()) {
                                if (useEQ) {
                                    runInEventDispatchThread(new Runnable() {
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

    private static void runInEventDispatchThread(final Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeLater(r);
        }
    }

    private static void runInEventDispatchThreadAndWait(final Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(r);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // don't swallow the interrupted exception!
            } catch (InvocationTargetException e) {
            }
        }
    }
}
