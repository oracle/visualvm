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

import org.graalvm.visualvm.lib.jfluid.ProfilerClient;
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

    public void dataReady(final byte[] buf, int instrumentationType) {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("Profiling data ready "+buf.length); // NOI18N
        }
        fireProcessData(buf, instrumentationType);
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
