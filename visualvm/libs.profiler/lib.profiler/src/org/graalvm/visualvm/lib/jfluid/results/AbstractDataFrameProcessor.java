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

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.graalvm.visualvm.lib.jfluid.ProfilerClient;


/**
 *
 * @author Jaroslav Bachorik
 * @author Tomas Hurka
 */
public abstract class AbstractDataFrameProcessor implements DataFrameProcessor {
    //~ Inner Interfaces ---------------------------------------------------------------------------------------------------------

    protected static interface ListenerFunctor {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        void execute(ProfilingResultListener listener);
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    protected static final Logger LOGGER = Logger.getLogger(DataFrameProcessor.class.getName());

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected volatile ProfilerClient client = null;
    protected volatile boolean collectingTwoTimeStamps;
    private final Set listeners = new CopyOnWriteArraySet();

    // @GuardedBy this
    private boolean processorLives = false;

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public boolean hasListeners() {
        return !listeners.isEmpty();
    }

    public void processDataFrame(byte[] buffer) {
        synchronized(client) {
            synchronized (this) {
                if (!processorLives) return;
                try {
                    fireBatchStart();
                    if (LOGGER.isLoggable(Level.FINEST)) {
                        LOGGER.finest("Frame start, size="+buffer.length); // NOI18N
                    }
                    collectingTwoTimeStamps = (client != null) ? client.getStatus().collectingTwoTimeStamps() : false;
                    doProcessDataFrame(ByteBuffer.wrap(buffer));
                } catch (Throwable e) {
                    LOGGER.log(Level.SEVERE, "Error while processing data frame", e); // NOI18N
                } finally {
                    if (LOGGER.isLoggable(Level.FINEST)) {
                        LOGGER.finest("Frame stop"); // NOI18N
                    }
                    fireBatchStop();
                }
            }
        }
    }

    public void removeAllListeners() {
        for (Iterator iter = listeners.iterator(); iter.hasNext();) {
            ((ProfilingResultListener) iter.next()).shutdown();
        }
        listeners.clear();
    }

    public void reset() {
        fireReset();
    }

    public void shutdown() {
        // finalize the batch
        synchronized(this) {
            processorLives = false;
            fireShutdown();
        }
    }

    public void startup(ProfilerClient client) {
        synchronized(this) {
            processorLives = true;
            this.client = client;
        }
    }

    protected void addListener(final ProfilingResultListener listener) {
        listeners.add(listener);
    }

    protected abstract void doProcessDataFrame(ByteBuffer buffer);

    protected static long getTimeStamp(ByteBuffer buffer) {
        long timestamp = (((long) buffer.get() & 0xFF) << 48) | (((long) buffer.get() & 0xFF) << 40)
                         | (((long) buffer.get() & 0xFF) << 32) | (((long) buffer.get() & 0xFF) << 24)
                         | (((long) buffer.get() & 0xFF) << 16) | (((long) buffer.get() & 0xFF) << 8)
                         | ((long) buffer.get() & 0xFF);
        return timestamp;
    }

    protected static String getString(final ByteBuffer buffer) {
        int strLen = buffer.getChar();
        byte[] str = new byte[strLen];
        
        buffer.get(str);
        return new String(str);
    }

    protected void fireProfilingPoint(final int threadId, final int ppId, final long timeStamp) {
        foreachListener(new ListenerFunctor() {
                public void execute(ProfilingResultListener listener) {
                    listener.profilingPoint(threadId, ppId, timeStamp);
                }
            });
    }

    protected void fireReset() {
        foreachListener(new ListenerFunctor() {
                public void execute(ProfilingResultListener listener) {
                    listener.reset();
                }
            });
    }

    protected void foreachListener(ListenerFunctor functor) {
        for (Iterator iter = listeners.iterator(); iter.hasNext();) {
            functor.execute((ProfilingResultListener) iter.next());
        }
    }

    protected void removeListener(final ProfilingResultListener listener) {
        if (listeners.remove(listener)) {
            listener.shutdown();
        }
    }

    private void fireBatchStart() {
        foreachListener(new ListenerFunctor() {
                public void execute(ProfilingResultListener listener) {
                    listener.onBatchStart();
                }
            });
    }

    private void fireBatchStop() {
        foreachListener(new ListenerFunctor() {
                public void execute(ProfilingResultListener listener) {
                    listener.onBatchStop();
                }
            });
    }

    private void fireShutdown() {
        foreachListener(new ListenerFunctor() {
                public void execute(ProfilingResultListener listener) {
                    listener.shutdown();
                }
            });
    }
}
