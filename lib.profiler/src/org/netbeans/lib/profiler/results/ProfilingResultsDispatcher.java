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

package org.netbeans.lib.profiler.results;

import org.netbeans.lib.profiler.ProfilerClient;
import org.netbeans.lib.profiler.ProfilerLogger;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.results.cpu.*;
import org.netbeans.lib.profiler.results.memory.MemoryDataFrameProcessor;
import org.netbeans.lib.profiler.results.memory.MemoryProfilingResultsListener;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;


/**
 *
 * @author Jaroslav Bachorik
 */
public final class ProfilingResultsDispatcher implements ProfilingResultsProvider.Dispatcher {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final Logger LOGGER = Logger.getLogger(ProfilingResultsDispatcher.class.getName()); // NOI18N
    private static final int QLengthLowerBound = 13;
    private static final int QLengthUpperBound = 15;
    private static ProfilingResultsDispatcher instance;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final CPUDataFrameProcessor cpuDataProcessor = new CPUDataFrameProcessor();
    private final MemoryDataFrameProcessor memoryDataProcessor = new MemoryDataFrameProcessor();
    private final Object cpuDataProcessorQLengthLock = new Object();
    private final Object memDataProcessorQLengthLock = new Object();
    private ExecutorService queueProcessor;
    private volatile boolean pauseFlag = true;

    // @GuardedBy cpuDataProcessorQLengthLock
    private int cpuDataProcessorQLength = 0;

    // @GuardedBy memDataProcessorQLengthLock
    private int memDataProcessorQLength = 0;

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static synchronized ProfilingResultsDispatcher getDefault() {
        if (instance == null) {
            instance = new ProfilingResultsDispatcher();
        }

        return instance;
    }

    public void addListener(final CPUProfilingResultListener listener) {
        if (cpuDataProcessor != null) {
            cpuDataProcessor.addListener(listener);
        }
    }

    public void addListener(final MemoryProfilingResultsListener listener) {
        if (memoryDataProcessor != null) {
            memoryDataProcessor.addListener(listener);
        }
    }

    public synchronized void dataFrameReceived(final byte[] buffer, final int instrumentationType) {
        if (!cpuDataProcessor.hasListeners() && !memoryDataProcessor.hasListeners()) {
            return; // no consumers
        }

        switch (instrumentationType) {
            case CommonConstants.INSTR_RECURSIVE_FULL:
            case CommonConstants.INSTR_RECURSIVE_SAMPLED: {
                synchronized (cpuDataProcessorQLengthLock) {
                    cpuDataProcessorQLength++;

                    if (cpuDataProcessorQLength > QLengthUpperBound) {
                        try {
                            cpuDataProcessorQLengthLock.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }

                    getExecutor().submit(new Runnable() {
                            public void run() {
                                try {
                                    cpuDataProcessor.processDataFrame(buffer);
                                } finally {
                                    synchronized (cpuDataProcessorQLengthLock) {
                                        cpuDataProcessorQLength--;

                                        if (cpuDataProcessorQLength < QLengthLowerBound) {
                                            cpuDataProcessorQLengthLock.notifyAll();
                                        }
                                    }
                                }
                            }
                        });
                }

                break;
            }
            case CommonConstants.INSTR_OBJECT_ALLOCATIONS:
            case CommonConstants.INSTR_OBJECT_LIVENESS: {
                synchronized (memDataProcessorQLengthLock) {
                    memDataProcessorQLength++;

                    if (memDataProcessorQLength > QLengthUpperBound) {
                        try {
                            memDataProcessorQLengthLock.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }

                    getExecutor().submit(new Runnable() {
                            public void run() {
                                try {
                                    memoryDataProcessor.processDataFrame(buffer);
                                } finally {
                                    synchronized (memDataProcessorQLengthLock) {
                                        memDataProcessorQLength--;

                                        if (memDataProcessorQLength < QLengthLowerBound) {
                                            memDataProcessorQLengthLock.notifyAll();
                                        }
                                    }
                                }
                            }
                        });
                }

                break;
            }
            default:ProfilerLogger.warning("Unknown instrumentation type (" + instrumentationType + ") in dataframe"); // NOI18N
        }
    }

    public void pause(boolean flush) {
        pauseFlag = true;
    }

    public void removeAllListeners() {
        if (cpuDataProcessor != null) {
            cpuDataProcessor.removeAllListeners();
        }

        if (memoryDataProcessor != null) {
            memoryDataProcessor.removeAllListeners();
        }
    }

    public void removeListener(final CPUProfilingResultListener listener) {
        if (cpuDataProcessor != null) {
            cpuDataProcessor.removeListener(listener);
        }
    }

    public void removeListener(final MemoryProfilingResultsListener listener) {
        if (memoryDataProcessor != null) {
            memoryDataProcessor.removeListener(listener);
        }
    }

    public void reset() {
        fireReset();
    }

    public void resume() {
        pauseFlag = false;
    }

    public synchronized void shutdown() {
        //    queueProcessor.shutdownNow();
        fireShutdown(); // signalize shutdown
        removeAllListeners();
    }

    public synchronized void startup(ProfilerClient client) {
        fireStartup(client);
        resume();
    }

    private synchronized ExecutorService getExecutor() {
        if (queueProcessor == null) {
            queueProcessor = Executors.newSingleThreadExecutor();
        }

        return queueProcessor;
    }

    private synchronized void fireReset() {
        cpuDataProcessor.reset();
        memoryDataProcessor.reset();
    }

    private synchronized void fireShutdown() {
        if (cpuDataProcessor != null) {
            cpuDataProcessor.shutdown();
        }

        if (memoryDataProcessor != null) {
            memoryDataProcessor.shutdown();
        }
    }

    private synchronized void fireStartup(ProfilerClient client) {
        cpuDataProcessor.startup(client);
        memoryDataProcessor.startup(client);
    }
}
