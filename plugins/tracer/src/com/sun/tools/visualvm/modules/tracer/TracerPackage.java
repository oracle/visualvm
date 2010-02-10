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
import com.sun.tools.visualvm.core.datasupport.Positionable;
import java.util.Set;
import javax.swing.Icon;

/**
 * Set of TracerProbes distributed as a single package. The probes are typically
 * designed to monitor the same functional unit on the target - for example
 * disk I/O, network I/O, memory subsystem etc.
 *
 * @author Jiri Sedlacek
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


    public static abstract class StateAware<X extends DataSource> extends TracerPackage<X> {

        private PackageStateHandler<X> stateHandler;


        public StateAware(String name, String description,
                          Icon icon, int preferredPosition) {
            super(name, description, icon, preferredPosition);
        }


        public synchronized final PackageStateHandler<X> getStateHandler() {
            if (stateHandler == null) stateHandler = new PackageStateHandler<X>() {
                public void probeAdded(TracerProbe<X> probe, X dataSource) {
                    StateAware.this.probeAdded(probe, dataSource);
                }
                public void probeRemoved(TracerProbe<X> probe, X dataSource) {
                    StateAware.this.probeRemoved(probe, dataSource);
                }
                public TracerProgressObject sessionInitializing(Set<TracerProbe<X>> probes,
                    X dataSource) {
                    return StateAware.this.sessionInitializing(probes, dataSource);
                }
                public void sessionStarting(Set<TracerProbe<X>> probes, X dataSource)
                        throws SessionInitializationException {
                    StateAware.this.sessionStarting(probes, dataSource);
                }
                public void sessionRunning(Set<TracerProbe<X>> probes, X dataSource) {
                    StateAware.this.sessionRunning(probes, dataSource);
                }
                public void sessionStopping(Set<TracerProbe<X>> probes, X dataSource) {
                    StateAware.this.sessionStopping(probes, dataSource);
                }
                public void sessionFinished(Set<TracerProbe<X>> probes, X dataSource) {
                    StateAware.this.sessionFinished(probes, dataSource);
                }
            };
            return stateHandler;
        }


        // Probe added to Probes graph
        protected void probeAdded(TracerProbe<X> probe, X dataSource) {}

        // Probe removed from Probes graph
        protected void probeRemoved(TracerProbe<X> probe, X dataSource) {}


        protected TracerProgressObject sessionInitializing(Set<TracerProbe<X>> probes,
                X dataSource) { return null; }

        // Tracer session is starting
        // Setup probe, deploy, instrument...
        protected void sessionStarting(Set<TracerProbe<X>> probes, X dataSource)
                throws SessionInitializationException {}

        // Tracer session is running
        protected void sessionRunning(Set<TracerProbe<X>> probes, X dataSource) {}

        // Tracer session is stopping
        // Uninstrument, undeploy, disable...
        protected void sessionStopping(Set<TracerProbe<X>> probes, X dataSource) {}

        // Tracer session is stopped
        protected void sessionFinished(Set<TracerProbe<X>> probes, X dataSource) {}

    }

}
