/*
 *  Copyright 2007-2010 Sun Microsystems, Inc.  All Rights Reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Sun designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Sun in the LICENSE file that accompanied this code.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *  Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 *  CA 95054 USA or visit www.sun.com if you need additional information or
 *  have any questions.
 */

package com.sun.tools.visualvm.modules.tracer;

import com.sun.tools.visualvm.core.datasource.DataSource;

/**
 * PackageStateHandler interface allows an implementing TracerPackage to be
 * notified about Tracer session state. See TracerPackage.getStateHandler().
 *
 * @author Jiri Sedlacek
 * @param <X> any DataSource type
 */
public interface PackageStateHandler<X extends DataSource> {

    /**
     * Invoked when a probe is added into the Timeline view.
     * 
     * @param probe added probe
     * @param dataSource monitored DataSource
     */
    public void probeAdded(TracerProbe<X> probe, X dataSource);

    /**
     * Invoked when a probe is removed from the Timeline view.
     *
     * @param probe removed probe
     * @param dataSource monitored DataSource
     */
    public void probeRemoved(TracerProbe<X> probe, X dataSource);


    /**
     * Invoked when setting up a new Tracer session. This method allows a
     * Package to notify the user about initialization progress. The actual
     * initialization (and updating the TracerProgressObject) should be
     * performed in the sessionStarting() method. Useful for example for
     * messaging a delay during instrumention of classes in target application.
     *
     * @param probes probes defined for the Tracer session
     * @param dataSource monitored DataSource
     * @return TracerProgressObject to track initialization progress
     */
    public TracerProgressObject sessionInitializing(TracerProbe<X>[] probes,
            X dataSource);

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
     * @param dataSource monitored DataSource
     * @throws SessionInitializationException in case of initialization failure
     */
    public void sessionStarting(TracerProbe<X>[] probes, X dataSource)
            throws SessionInitializationException;

    /**
     * Invoked when all packages/probes have been started and the Tracer session
     * is running and collecting data.
     *
     * @param probes probes defined for the Tracer session
     * @param dataSource monitored DataSource
     */
    public void sessionRunning(TracerProbe<X>[] probes, X dataSource);

    /**
     * Invoked when stopping the Tracer session. Any package/probes cleanup
     * should be performed in this method. Any long-running cleanup code should
     * preferably be invoked in a separate worker thread to allow the Tracer
     * session to finish as fast as possible. Be sure to check/wait for the
     * cleanup thread when starting a new Tracer session in sessionStarting().
     * 
     * @param probes probes defined for the Tracer session
     * @param dataSource monitored DataSource
     */
    public void sessionStopping(TracerProbe<X>[] probes, X dataSource);

    /**
     * Invoked when the Tracer session has finished.
     *
     * @param probes probes defined for the Tracer session
     * @param dataSource monitored DataSource
     */
    public void sessionFinished(TracerProbe<X>[] probes, X dataSource);


    /**
     * An abstract adapter class for receiving Tracer session state notifications.
     *
     * @param <X> any DataSource type
     */
    public abstract class Adapter<X extends DataSource> implements PackageStateHandler<X> {

        public void probeAdded(TracerProbe<X> probe, X dataSource) {}

        public void probeRemoved(TracerProbe<X> probe, X dataSource) {}

        /**
         * Invoked when setting up a new Tracer session. This method allows a
         * Package to notify the user about initialization progress. The actual
         * initialization (and updating the TracerProgressObject) should be
         * performed in the sessionStarting() method. Useful for example for
         * messaging a delay during instrumention of classes in target application.
         *
         * @param probes probes defined for the Tracer session
         * @param dataSource monitored DataSource
         * @return TracerProgressObject null in default implementation
         */
        public TracerProgressObject sessionInitializing(TracerProbe<X>[] probes,
                X dataSource) { return null; }

        public void sessionStarting(TracerProbe<X>[] probes, X dataSource)
                throws SessionInitializationException {}

        public void sessionRunning(TracerProbe<X>[] probes, X dataSource) {}

        public void sessionStopping(TracerProbe<X>[] probes, X dataSource) {}

        public void sessionFinished(TracerProbe<X>[] probes, X dataSource) {}

    }

}
