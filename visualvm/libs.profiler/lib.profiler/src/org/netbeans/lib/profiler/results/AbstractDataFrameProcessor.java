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

package org.netbeans.lib.profiler.results;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.lib.profiler.ProfilerClient;


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
