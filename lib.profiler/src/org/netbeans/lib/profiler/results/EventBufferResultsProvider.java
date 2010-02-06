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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author Jaroslav Bachorik
 */
public class EventBufferResultsProvider implements ProfilingResultsProvider {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final Logger LOGGER = Logger.getLogger(EventBufferResultsProvider.class.getName());
    private static final EventBufferResultsProvider instance = new EventBufferResultsProvider();

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final Set listeners = Collections.synchronizedSet(new HashSet());

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of RawProfilingResultsCollector */
    private EventBufferResultsProvider() {
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static EventBufferResultsProvider getDefault() {
        return instance;
    }

    public void addDispatcher(ProfilingResultsProvider.Dispatcher dispatcher) {
        listeners.add(dispatcher);
    }

    public void dataReady(int buffsize, int instrumentationType) {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("Profiling data ready "+buffsize); // NOI18N
        }

        byte[] data = new byte[buffsize];
        System.arraycopy(EventBufferProcessor.buf, 0, data, 0, buffsize);
        fireProcessData(data, instrumentationType);
    }

    public void removeDispatcher(ProfilingResultsProvider.Dispatcher dispatcher) {
        listeners.remove(dispatcher);
    }

    public void shutdown() {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("Shutting down profiler"); // NOI18N
        }

        fireShutdown();
    }

    public void startup(ProfilerClient client) {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("Starting up profiler"); // NOI18N
        }

        fireStartup(client);
    }

    private void fireProcessData(final byte[] data, final int instrumentationType) {
        for (Iterator iter = listeners.iterator(); iter.hasNext();) {
            ProfilingResultsProvider.Dispatcher dispatcher = (ProfilingResultsProvider.Dispatcher) iter.next();
            dispatcher.dataFrameReceived(data, instrumentationType);
        }
    }

    private void fireShutdown() {
        for (Iterator iter = listeners.iterator(); iter.hasNext();) {
            ProfilingResultsProvider.Dispatcher dispatcher = (ProfilingResultsProvider.Dispatcher) iter.next();
            dispatcher.shutdown();
        }
    }

    private void fireStartup(ProfilerClient client) {
        for (Iterator iter = listeners.iterator(); iter.hasNext();) {
            ProfilingResultsProvider.Dispatcher dispatcher = (ProfilingResultsProvider.Dispatcher) iter.next();
            dispatcher.startup(client);
        }
    }
}
