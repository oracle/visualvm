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
import com.sun.tools.visualvm.core.datasupport.Positionable;
import javax.swing.Icon;

/**
 * TracerPackage is a container for a set of TracerProbes distributed as a single
 * package (plugin). The probes in a package are typically designed to monitor
 * the same functional unit on the target - for example disk I/O, network I/O,
 * memory subsystem etc.
 *
 * @author Jiri Sedlacek
 * @param <X> any DataSource type
 */
public abstract class TracerPackage<X extends DataSource> implements Positionable {

    private final String name;
    private final String description;
    private final Icon icon;
    private final int preferredPosition;


    /**
     * Creates new instance of TracerPackage.
     *
     * @param name name of the package
     * @param description description of the package
     * @param icon icon of the package
     * @param preferredPosition preferred position of the package in UI
     */
    public TracerPackage(String name, String description, Icon icon,
                         int preferredPosition) {
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.preferredPosition = preferredPosition;
    }


    /**
     * Returns name of the package.
     *
     * @return name of the package
     */
    public final String getName() { return name; }

    /**
     * Returns description of the package.
     *
     * @return description of the package
     */
    public final String getDescription() { return description; }

    /**
     * Returns icon of the package.
     *
     * @return icon of the package
     */
    public final Icon getIcon() { return icon; }

    /**
     * Returns preferred position of the package in UI.
     *
     * @return preferred position of the package in UI
     */
    public final int getPreferredPosition() { return preferredPosition; }


    /**
     * Returns array of TracerProbeDescriptors to present the package probes in UI.
     * Should always return descriptors for all probes provided by the provider
     * for each DataSource. If a probe is not available for the DataSource its
     * descriptor should be disabled - TracerProbeDescriptor.isProbeAvailable()
     * returns false.
     *
     * @return array of TracerProbeDescriptors to present the package probes in UI
     */
    public abstract TracerProbeDescriptor[] getProbeDescriptors();

    /**
     * Returns the probe to be used in Tracer session. The probe to return
     * is defined by its TracerProbeDescriptor created by getProbeDescriptors()
     * method and selected by the user.
     *
     * @param descriptor TracerProbeDescriptor selecting the probe
     * @return the probe to be used in Tracer session
     */
    public abstract TracerProbe<X> getProbe(TracerProbeDescriptor descriptor);

    
    /**
     * Optionally returns PackageStateHandler instance which obtains notifications
     * about the Tracer session status in context of TracerProbes provided by this
     * TracerPackage. Default implementation returns null. You may use StateAware
     * subclass instead of implementing this method to obtain the notifications.
     *
     * @return PackageStateHandler instance which obtains status notifications
     */
    public PackageStateHandler<X> getStateHandler() { return null; }


    /**
     * An abstract adapter class for receiving Tracer session state notifications.
     * See PackageStateHandler for details.
     *
     * @param <X> any DataSource type
     */
    public static abstract class SessionAware<X extends DataSource> extends TracerPackage<X> {

        private PackageStateHandler<X> stateHandler;


        /**
         * Creates new instance of TracerPackage.SessionAware.
         *
         * @param name name of the package
         * @param description description of the package
         * @param icon icon of the package
         * @param preferredPosition preferred position of the package in UI
         */
        public SessionAware(String name, String description,
                          Icon icon, int preferredPosition) {
            super(name, description, icon, preferredPosition);
        }


        public synchronized final PackageStateHandler<X> getStateHandler() {
            if (stateHandler == null) stateHandler = new PackageStateHandler<X>() {
                public void probeAdded(TracerProbe<X> probe, X dataSource) {
                    SessionAware.this.probeAdded(probe, dataSource);
                }
                public void probeRemoved(TracerProbe<X> probe, X dataSource) {
                    SessionAware.this.probeRemoved(probe, dataSource);
                }
                public TracerProgressObject sessionInitializing(TracerProbe<X>[] probes,
                    X dataSource) {
                    return SessionAware.this.sessionInitializing(probes, dataSource);
                }
                public void sessionStarting(TracerProbe<X>[] probes, X dataSource)
                        throws SessionInitializationException {
                    SessionAware.this.sessionStarting(probes, dataSource);
                }
                public void sessionRunning(TracerProbe<X>[] probes, X dataSource) {
                    SessionAware.this.sessionRunning(probes, dataSource);
                }
                public void sessionStopping(TracerProbe<X>[] probes, X dataSource) {
                    SessionAware.this.sessionStopping(probes, dataSource);
                }
                public void sessionFinished(TracerProbe<X>[] probes, X dataSource) {
                    SessionAware.this.sessionFinished(probes, dataSource);
                }
            };
            return stateHandler;
        }


        /**
         * Invoked when a probe is added into the Timeline view.
         *
         * @param probe added probe
         * @param dataSource monitored DataSource
         */
        protected void probeAdded(TracerProbe<X> probe, X dataSource) {}

        /**
         * Invoked when a probe is removed from the Timeline view.
         *
         * @param probe removed probe
         * @param dataSource monitored DataSource
         */
        protected void probeRemoved(TracerProbe<X> probe, X dataSource) {}


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
        protected TracerProgressObject sessionInitializing(TracerProbe<X>[] probes,
                X dataSource) { return null; }

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
        protected void sessionStarting(TracerProbe<X>[] probes, X dataSource)
                throws SessionInitializationException {}

        /**
         * Invoked when all packages/probes have been started and the Tracer session
         * is running and collecting data.
         *
         * @param probes probes defined for the Tracer session
         * @param dataSource monitored DataSource
         */
        protected void sessionRunning(TracerProbe<X>[] probes, X dataSource) {}

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
        protected void sessionStopping(TracerProbe<X>[] probes, X dataSource) {}

        /**
         * Invoked when the Tracer session has finished.
         *
         * @param probes probes defined for the Tracer session
         * @param dataSource monitored DataSource
         */
        protected void sessionFinished(TracerProbe<X>[] probes, X dataSource) {}

    }

}
