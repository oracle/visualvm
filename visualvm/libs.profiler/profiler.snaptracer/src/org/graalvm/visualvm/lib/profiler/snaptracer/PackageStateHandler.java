/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.profiler.snaptracer;

import org.graalvm.visualvm.lib.profiler.snaptracer.impl.IdeSnapshot;


/**
 * PackageStateHandler interface allows an implementing TracerPackage to be
 * notified about Tracer session state. See TracerPackage.getStateHandler().
 *
 * @author Jiri Sedlacek
 */
public interface PackageStateHandler {

    /**
     * Invoked when a probe is added into the Timeline view.
     *
     * @param probe added probe
     * @param snapshot profiler snapshot
     */
    public void probeAdded(TracerProbe probe, IdeSnapshot snapshot);

    /**
     * Invoked when a probe is removed from the Timeline view.
     *
     * @param probe removed probe
     * @param snapshot profiler snapshot
     */
    public void probeRemoved(TracerProbe probe, IdeSnapshot snapshot);


    /**
     * Invoked when setting up a new Tracer session. This method allows a
     * Package to notify the user about initialization progress. The actual
     * initialization (and updating the TracerProgressObject) should be
     * performed in the sessionStarting() method. Useful for example for
     * messaging a delay during instrumentation of classes in target application.
     *
     * @param probes probes defined for the Tracer session
     * @param snapshot profiler snapshot
     * @param refresh session refresh rate in milliseconds
     * @return TracerProgressObject to track initialization progress
     */
    public TracerProgressObject sessionInitializing(TracerProbe[] probes,
            IdeSnapshot snapshot, int refresh);

    /**
     * Invoked when starting a new Tracer session. Any package/probes
     * initialization should be performed in this method. If provided by the
     * sessionInitializing method, a TracerProgressObject should be updated to
     * reflect the initialization progress. This method may throw a
     * SessionInitializationException in case of initialization failure. Any
     * packages/probes initialized so far will be correctly finished, however the
     * package throwing the SessionInitializationException is responsible for
     * cleaning up any used resources and restoring its state without any
     * following events.
     *
     * @param probes probes defined for the Tracer session
     * @param snapshot profiler snapshot
     * @throws SessionInitializationException in case of initialization failure
     */
    public void sessionStarting(TracerProbe[] probes, IdeSnapshot snapshot)
            throws SessionInitializationException;

    /**
     * Invoked when all packages/probes have been started and the Tracer session
     * is running and collecting data.
     *
     * @param probes probes defined for the Tracer session
     * @param snapshot profiler snapshot
     */
    public void sessionRunning(TracerProbe[] probes, IdeSnapshot snapshot);

    /**
     * Invoked when stopping the Tracer session. Any package/probes cleanup
     * should be performed in this method. Any long-running cleanup code should
     * preferably be invoked in a separate worker thread to allow the Tracer
     * session to finish as fast as possible. Be sure to check/wait for the
     * cleanup thread when starting a new Tracer session in sessionStarting().
     * 
     * @param probes probes defined for the Tracer session
     * @param snapshot profiler snapshot
     */
    public void sessionStopping(TracerProbe[] probes, IdeSnapshot snapshot);

    /**
     * Invoked when the Tracer session has finished.
     *
     * @param probes probes defined for the Tracer session
     * @param snapshot profiler snapshot
     */
    public void sessionFinished(TracerProbe[] probes, IdeSnapshot snapshot);

    /**
     * Invoked when refresh rate of the Tracer session has been changed.
     *
     * @param probes probes defined for the Tracer session
     * @param snapshot profiler snapshot
     * @param refresh session refresh rate in milliseconds
     */
    public void refreshRateChanged(TracerProbe[] probes, IdeSnapshot snapshot, int refresh);


    /**
     * An abstract adapter class for receiving Tracer session state notifications.
     */
    public abstract class Adapter implements PackageStateHandler {

        public void probeAdded(TracerProbe probe, IdeSnapshot snapshot) {}

        public void probeRemoved(TracerProbe probe, IdeSnapshot snapshot) {}

        /**
         * Invoked when setting up a new Tracer session. This method allows a
         * Package to notify the user about initialization progress. The actual
         * initialization (and updating the TracerProgressObject) should be
         * performed in the sessionStarting() method. Useful for example for
         * messaging a delay during instrumentation of classes in target application.
         *
         * @param probes probes defined for the Tracer session
         * @param snapshot profiler snapshot
         * @param refresh session refresh rate in milliseconds
         * @return TracerProgressObject null in default implementation
         */
        public TracerProgressObject sessionInitializing(TracerProbe[] probes,
                IdeSnapshot snapshot, int refresh) { return null; }

        public void sessionStarting(TracerProbe[] probes, IdeSnapshot snapshot)
                throws SessionInitializationException {}

        public void sessionRunning(TracerProbe[] probes, IdeSnapshot snapshot) {}

        public void sessionStopping(TracerProbe[] probes, IdeSnapshot snapshot) {}

        public void sessionFinished(TracerProbe[] probes, IdeSnapshot snapshot) {}

        public void refreshRateChanged(TracerProbe[] probes, IdeSnapshot snapshot,
                int refresh) {}

    }

}
