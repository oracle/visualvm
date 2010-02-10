/*
 *  Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
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
 *
 * @author Jiri Sedlacek
 */
public abstract class TracerProbe<X extends DataSource> {

    private final TracerProbeDescriptor descriptor;
    private final ProbeItemDescriptor[] itemDescriptors;


    public TracerProbe(TracerProbeDescriptor descriptor,
                       ProbeItemDescriptor[] itemDescriptors) {

        if (descriptor == null)
            throw new IllegalArgumentException("Invalid TracerProbeDescriptor"); // NOI18N
        if (itemDescriptors == null || itemDescriptors.length == 0)
            throw new IllegalArgumentException("Invalid ProbeItemDescriptors"); // NOI18N

        this.descriptor = descriptor;
        this.itemDescriptors = itemDescriptors;
    }


    public final TracerProbeDescriptor getDescriptor() { return descriptor; }

    /**
     * Returns descriptors of items provided by the probe.
     *
     * @return descriptors of items provided by the probe
     */
    public final ProbeItemDescriptor[] getItemDescriptors() { return itemDescriptors; };


    /**
     * Returns number of items provided by the probe.
     *
     * @return number of items provided by the probe
     */
    public int getItemsCount() { return itemDescriptors.length; }

    public abstract long[] getItemValues(long timestamp);


    public ProbeStateHandler<X> getStateHandler() { return null; }


    public static abstract class StateAware<X extends DataSource> extends TracerProbe<X> {

        private ProbeStateHandler<X> stateHandler;


        public StateAware(TracerProbeDescriptor descriptor,
                          ProbeItemDescriptor[] itemDescriptors) {
            super(descriptor, itemDescriptors);
        }


        public synchronized final ProbeStateHandler<X> getStateHandler() {
            if (stateHandler == null) stateHandler = new ProbeStateHandler<X>() {
                public void probeAdded(X dataSource) {
                    StateAware.this.probeAdded(dataSource);
                }
                public void probeRemoved(X dataSource) {
                    StateAware.this.probeRemoved(dataSource);
                }
                public TracerProgressObject sessionInitializing(X dataSource) {
                    return StateAware.this.sessionInitializing(dataSource);
                }
                public void sessionStarting(X dataSource)
                        throws SessionInitializationException {
                    StateAware.this.sessionStarting(dataSource);
                }
                public void sessionRunning(X dataSource) {
                    StateAware.this.sessionRunning(dataSource);
                }
                public void sessionStopping(X dataSource) {
                    StateAware.this.sessionStopping(dataSource);
                }
                public void sessionFinished(X dataSource) {
                    StateAware.this.sessionFinished(dataSource);
                }
            };
            return stateHandler;
        }


        // Probe added to Probes graph
        protected void probeAdded(X dataSource) {}

        // Probe removed from Probes graph
        protected void probeRemoved(X dataSource) {}


        protected TracerProgressObject sessionInitializing(X dataSource) { return null; }

        // Tracer session is starting
        // Setup probe, deploy, instrument...
        protected void sessionStarting(X dataSource)
                throws SessionInitializationException {}

        // Tracer session is running
        protected void sessionRunning(X dataSource) {}

        // Tracer session is stopping
        // Uninstrument, undeploy, disable...
        protected void sessionStopping(X dataSource) {}

        // Tracer session is stopped
        protected void sessionFinished(X dataSource) {}

    }

}
