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
 * Tracer probe represents one monitored unit in the Timeline chart. The probe
 * may provide one or several values displayed in the chart, for example several
 * numeric values of an MBean.
 *
 * @author Jiri Sedlacek
 */
public abstract class TracerProbe<X extends DataSource> {

    private final TracerProbeDescriptor descriptor;
    private final ProbeItemDescriptor[] itemDescriptors;


    /**
     * Creates new instance of TracerProbe.
     *
     * @param descriptor descriptor of UI appearance of the probe
     * @param itemDescriptors descriptors of UI appearance of items provided by the probe
     */
    public TracerProbe(TracerProbeDescriptor descriptor,
                       ProbeItemDescriptor[] itemDescriptors) {

        if (descriptor == null)
            throw new IllegalArgumentException("Invalid TracerProbeDescriptor"); // NOI18N
        if (itemDescriptors == null || itemDescriptors.length == 0)
            throw new IllegalArgumentException("Invalid ProbeItemDescriptors"); // NOI18N

        this.descriptor = descriptor;
        this.itemDescriptors = itemDescriptors;
    }


    /**
     * Returns descriptor of UI appearance of the probe.
     *
     * @return descriptor of UI appearance of the probedescriptor of UI appearance of the probe
     */
    public final TracerProbeDescriptor getDescriptor() { return descriptor; }

    /**
     * Returns descriptors of UI appearance of items provided by the probe.
     *
     * @return descriptors of UI appearance of items provided by the probe
     */
    public final ProbeItemDescriptor[] getItemDescriptors() { return itemDescriptors; };


    /**
     * Returns number of items provided by the probe.
     *
     * @return number of items provided by the probe
     */
    public int getItemsCount() { return itemDescriptors.length; }

    /**
     * Returns values of items provided by the probe at the defined time. It's up
     * to the probe implementation whether the values will be read in this method
     * or retrieved from a private data model prefetching the values asynchronously.
     * The timestamp is the same for a single data-read event for all selected
     * probes of the actual Tracer session.
     *
     * @param timestamp timestamp of the data-read event
     * @return values of items provided by the probe
     */
    public abstract long[] getItemValues(long timestamp);


    /**
     * Optionally returns ProbeStateHandler instance which obtains notifications
     * about the Tracer session status. Default implementation returns null.
     * You may use StateAware subclass instead of implementing this method to
     * obtain the notifications.
     *
     * @return ProbeStateHandler instance which obtains status notifications,
     * default implementation returns null
     */
    public ProbeStateHandler<X> getStateHandler() { return null; }


    /**
     * An abstract adapter class for receiving Tracer session state notifications.
     * See ProbeStateHandler for details.
     *
     * @param <X> any DataSource type
     */
    public static abstract class SessionAware<X extends DataSource> extends TracerProbe<X> {

        private ProbeStateHandler<X> stateHandler;

        /**
         * Creates new instance of TracerProbe.SessionAware.
         *
         * @param descriptor descriptor of UI appearance of the probe
         * @param itemDescriptors descriptors of UI appearance of items provided by the probe
         */
        public SessionAware(TracerProbeDescriptor descriptor,
                          ProbeItemDescriptor[] itemDescriptors) {
            super(descriptor, itemDescriptors);
        }


        /**
         * Returns a predefined ProbeStateHandler which obtains notifications
         * about the Tracer session state.
         *
         * @return predefined ProbeStateHandler which obtains status notifications
         */
        public synchronized final ProbeStateHandler<X> getStateHandler() {
            if (stateHandler == null) stateHandler = new ProbeStateHandler<X>() {
                public void probeAdded(X dataSource) {
                    SessionAware.this.probeAdded(dataSource);
                }
                public void probeRemoved(X dataSource) {
                    SessionAware.this.probeRemoved(dataSource);
                }
                public TracerProgressObject sessionInitializing(X dataSource) {
                    return SessionAware.this.sessionInitializing(dataSource);
                }
                public void sessionStarting(X dataSource)
                        throws SessionInitializationException {
                    SessionAware.this.sessionStarting(dataSource);
                }
                public void sessionRunning(X dataSource) {
                    SessionAware.this.sessionRunning(dataSource);
                }
                public void sessionStopping(X dataSource) {
                    SessionAware.this.sessionStopping(dataSource);
                }
                public void sessionFinished(X dataSource) {
                    SessionAware.this.sessionFinished(dataSource);
                }
            };
            return stateHandler;
        }


        /**
         * Invoked when the is added into the Timeline view.
         *
         * @param dataSource monitored DataSource
         */
        protected void probeAdded(X dataSource) {}

        /**
         * Invoked when the probe is removed from the Timeline view.
         *
         * @param dataSource monitored DataSource
         */
        protected void probeRemoved(X dataSource) {}


        /**
         * Invoked when setting up a new Tracer session. This method allows a
         * Probe to notify the user about initialization progress. The actual
         * initialization (and updating the TracerProgressObject) should be
         * performed in the sessionStarting() method. Useful for example for
         * messaging a delay during instrumention of classes in target application.
         *
         * @param dataSource monitored DataSource
         * @return TracerProgressObject to track initialization progress
         */
        protected TracerProgressObject sessionInitializing(X dataSource) { return null; }

        /**
         * Invoked when starting a new Tracer session. Any probe initialization
         * should be performed in this method. If provided by the
         * sessionInitializing method, a TracerProgressObject should be updated to
         * reflect the initialization progress. This method may throw a
         * SessionInitializationException in case of initialization failure. Any
         * packages/probes initialized so far will be correctly finished, however the
         * probe throwing the SessionInitializationException is responsible for
         * cleaning up any used resources and restoring its state without any
         * following events.
         *
         * @param dataSource monitored DataSource
         * @throws SessionInitializationException in case of initialization failure
         */
        protected void sessionStarting(X dataSource)
                throws SessionInitializationException {}

        /**
         * Invoked when all packages/probes have been started and the Tracer session
         * is running and collecting data.
         *
         * @param dataSource monitored DataSource
         */
        protected void sessionRunning(X dataSource) {}

        /**
         * Invoked when stopping the Tracer session. Any probe cleanup should be
         * performed in this method. Any long-running cleanup code should preferably
         * be invoked in a separate worker thread to allow the Tracer session to
         * finish as fast as possible. Be sure to check/wait for the cleanup thread
         * when starting a new Tracer session in sessionStarting().
         *
         * @param dataSource monitored DataSource
         */
        protected void sessionStopping(X dataSource) {}

        /**
         * Invoked when the Tracer session has finished.
         *
         * @param dataSource monitored DataSource
         */
        protected void sessionFinished(X dataSource) {}

    }

}
