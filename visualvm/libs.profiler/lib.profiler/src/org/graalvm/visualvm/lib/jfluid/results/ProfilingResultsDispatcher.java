/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.jfluid.results;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import org.graalvm.visualvm.lib.jfluid.ProfilerClient;
import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;
import org.graalvm.visualvm.lib.jfluid.results.cpu.CPUDataFrameProcessor;
import org.graalvm.visualvm.lib.jfluid.results.cpu.CPUProfilingResultListener;
import org.graalvm.visualvm.lib.jfluid.results.cpu.CPUSamplingDataFrameProcessor;
import org.graalvm.visualvm.lib.jfluid.results.locks.LockDataFrameProcessor;
import org.graalvm.visualvm.lib.jfluid.results.locks.LockProfilingResultListener;
import org.graalvm.visualvm.lib.jfluid.results.memory.MemoryDataFrameProcessor;
import org.graalvm.visualvm.lib.jfluid.results.memory.MemoryProfilingResultsListener;


/**
 *
 * @author Jaroslav Bachorik
 * @author Tomas Hurka
 */
public final class ProfilingResultsDispatcher implements ProfilingResultsProvider.Dispatcher {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final Logger LOGGER = Logger.getLogger(ProfilingResultsDispatcher.class.getName());
    private static final int QLengthLowerBound = 13;
    private static final int QLengthUpperBound = 15;
    private static ProfilingResultsDispatcher instance;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final AbstractDataFrameProcessor cpuDataProcessor = new CPUDataFrameProcessor();
    private final AbstractDataFrameProcessor cpuSamplingDataProcessor = new CPUSamplingDataFrameProcessor();
    private final AbstractDataFrameProcessor memoryDataProcessor = new MemoryDataFrameProcessor();
    private final AbstractDataFrameProcessor lockDataProcessor = new LockDataFrameProcessor();
    private final Object cpuDataProcessorQLengthLock = new Object();
    private final Object memDataProcessorQLengthLock = new Object();
    private final Object lockDataProcessorQLengthLock = new Object();
    private ExecutorService queueProcessor;
    private volatile boolean pauseFlag = true;

    // @GuardedBy cpuDataProcessorQLengthLock
    private int cpuDataProcessorQLength = 0;

    // @GuardedBy memDataProcessorQLengthLock
    private int memDataProcessorQLength = 0;

    // @GuardedBy lockDataProcessorQLengthLock
    private int lockDataProcessorQLength = 0;

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static synchronized ProfilingResultsDispatcher getDefault() {
        if (instance == null) {
            instance = new ProfilingResultsDispatcher();
        }

        return instance;
    }

    public void addListener(final CPUProfilingResultListener listener) {
        cpuDataProcessor.addListener(listener);
        cpuSamplingDataProcessor.addListener(listener);
    }

    public void addListener(final MemoryProfilingResultsListener listener) {
        memoryDataProcessor.addListener(listener);
    }

    public void addListener(final LockProfilingResultListener listener) {
        lockDataProcessor.addListener(listener);
    }

    public synchronized void dataFrameReceived(final byte[] buffer, final int instrumentationType) {
        if (!cpuDataProcessor.hasListeners() && !memoryDataProcessor.hasListeners() &&
            !cpuSamplingDataProcessor.hasListeners() && !lockDataProcessor.hasListeners()) {
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
            case CommonConstants.INSTR_NONE_SAMPLING: {
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
                                    cpuSamplingDataProcessor.processDataFrame(buffer);
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
            default: {
                synchronized (lockDataProcessorQLengthLock) {
                    lockDataProcessorQLength++;

                    if (lockDataProcessorQLength > QLengthUpperBound) {
                        try {
                            lockDataProcessorQLengthLock.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }

                    getExecutor().submit(new Runnable() {
                            public void run() {
                                try {
                                    lockDataProcessor.processDataFrame(buffer);
                                } finally {
                                    synchronized (lockDataProcessorQLengthLock) {
                                        lockDataProcessorQLength--;

                                        if (lockDataProcessorQLength < QLengthLowerBound) {
                                            lockDataProcessorQLengthLock.notifyAll();
                                        }
                                    }
                                }
                            }
                        });
                }
            }
        }
    }

    public void pause(boolean flush) {
        pauseFlag = true;
    }

    public void removeAllListeners() {
        cpuDataProcessor.removeAllListeners();
        cpuSamplingDataProcessor.removeAllListeners();
        memoryDataProcessor.removeAllListeners();
        lockDataProcessor.removeAllListeners();
    }

    public void removeListener(final CPUProfilingResultListener listener) {
        cpuDataProcessor.removeListener(listener);
        cpuSamplingDataProcessor.removeListener(listener);
    }

    public void removeListener(final MemoryProfilingResultsListener listener) {
        memoryDataProcessor.removeListener(listener);
    }

    public void removeListener(final LockProfilingResultListener listener) {
        lockDataProcessor.removeListener(listener);
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
        cpuSamplingDataProcessor.reset();
        memoryDataProcessor.reset();
        lockDataProcessor.reset();
    }

    private synchronized void fireShutdown() {
        cpuDataProcessor.shutdown();
        cpuSamplingDataProcessor.shutdown();
        memoryDataProcessor.shutdown();
        lockDataProcessor.shutdown();
    }

    private synchronized void fireStartup(ProfilerClient client) {
        cpuSamplingDataProcessor.startup(client);
        cpuDataProcessor.startup(client);
        memoryDataProcessor.startup(client);
        lockDataProcessor.startup(client);
    }
}
