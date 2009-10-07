/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
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

    //@GuardedBy warmupLock
    private boolean workerRunning;
    private Runnable warmupTimer = new Runnable() {
        public void run() {
            synchronized (warmupLock) {
                try {
                    if (workerRunning) {
                        warmupLock.wait(getWarmup());
                    }

                    if (workerRunning) {
                        nonResponding();
                    }
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }
    };


    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of SwingWorker */
    public SwingWorker(boolean forceEQ) {
        sinit();
        this.useEQ = forceEQ;
    }

    public SwingWorker() {
        this(true);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * Executes the UI task. Starts the background task and handles it's execution cycle
     * If the background task blocks for more than getWarmup() milis the nonResponding() method is invoked
     */
    public void execute() {
        postRunnable(new Runnable() {
                public void run() {
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
            });
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
     * It's run in EQ
     */
    protected void done() {
        // override to provide a functionality
    }

    /**
     * Called when the background thread lasts longer than the warmup time
     * The implementor must take care of rescheduling on AWT thread if appropriate
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
