/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author Jaroslav Bachorik
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
    private final Set listeners = new HashSet();

    // @GuardedBy this
    private boolean processorLives = false;

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public boolean hasListeners() {
        synchronized(listeners) {
            return !listeners.isEmpty();
        }
    }

    public void processDataFrame(byte[] buffer) {
        synchronized(this) {
            if (!processorLives) return;
            
            try {
                fireBatchStart();
                doProcessDataFrame(buffer);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error while processing data frame", e);
            } finally {
                fireBatchStop();
            }
        }
    }

    public void removeAllListeners() {
        Set tmpListeners ;
        synchronized(listeners) {
            tmpListeners = new HashSet(listeners);
            listeners.clear();
        }

        for (Iterator iter = tmpListeners.iterator(); iter.hasNext();) {
            ((ProfilingResultListener) iter.next()).shutdown();
        }
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
        synchronized(listeners) {
            listeners.add(listener);
        }
    }

    protected abstract void doProcessDataFrame(byte[] buffer);

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
        Set tmpListeners;
        synchronized(listeners) {
            tmpListeners = new HashSet(listeners);
        }
        for (Iterator iter = tmpListeners.iterator(); iter.hasNext();) {
            functor.execute((ProfilingResultListener) iter.next());
        }
    }

    protected void removeListener(final ProfilingResultListener listener) {
        synchronized(listeners) {
            if (listeners.remove(listener)) {
                listener.shutdown();
            }
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
