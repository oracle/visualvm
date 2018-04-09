/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2007-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.netbeans.modules.profiler.snaptracer;

import org.netbeans.modules.profiler.snaptracer.impl.IdeSnapshot;


/**
 * ProbeStateHandler interface allows an implementing TracerProbe to be
 * notified about Tracer session state. See TracerProbe.getStateHandler().
 *
 * @author Jiri Sedlacek
 */
public interface ProbeStateHandler {

    /**
     * Invoked when the is added into the Timeline view.
     *
     * @param snapshot profiler snapshot
     */
    public void probeAdded(IdeSnapshot snapshot);

    /**
     * Invoked when the probe is removed from the Timeline view.
     *
     * @param snapshot profiler snapshot
     */
    public void probeRemoved(IdeSnapshot snapshot);


    /**
     * Invoked when setting up a new Tracer session. This method allows a
     * Probe to notify the user about initialization progress. The actual
     * initialization (and updating the TracerProgressObject) should be
     * performed in the sessionStarting() method. Useful for example for
     * messaging a delay during instrumention of classes in target application.
     *
     * @param snapshot profiler snapshot
     * @param refresh session refresh rate in miliseconds
     * @return TracerProgressObject to track initialization progress
     */
    public TracerProgressObject sessionInitializing(IdeSnapshot snapshot, int refresh);

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
     * @param snapshot profiler snapshot
     * @throws SessionInitializationException in case of initialization failure
     */
    public void sessionStarting(IdeSnapshot snapshot)
            throws SessionInitializationException;

    /**
     * Invoked when all packages/probes have been started and the Tracer session
     * is running and collecting data.
     *
     * @param snapshot profiler snapshot
     */
    public void sessionRunning(IdeSnapshot snapshot);

    /**
     * Invoked when stopping the Tracer session. Any probe cleanup should be
     * performed in this method. Any long-running cleanup code should preferably
     * be invoked in a separate worker thread to allow the Tracer session to
     * finish as fast as possible. Be sure to check/wait for the cleanup thread
     * when starting a new Tracer session in sessionStarting().
     *
     * @param snapshot profiler snapshot
     */
    public void sessionStopping(IdeSnapshot snapshot);

    /**
     * Invoked when the Tracer session has finished.
     *
     * @param snapshot profiler snapshot
     */
    public void sessionFinished(IdeSnapshot snapshot);

    /**
     * Invoked when refresh rate of the Tracer session has been changed.
     *
     * @param snapshot profiler snapshot
     * @param refresh session refresh rate in miliseconds
     */
    public void refreshRateChanged(IdeSnapshot snapshot, int refresh);


    /**
     * An abstract adapter class for receiving Tracer session state notifications.
     */
    public abstract class Adapter implements ProbeStateHandler {

        public void probeAdded(IdeSnapshot snapshot) {}

        public void probeRemoved(IdeSnapshot snapshot) {}

        /**
         * Invoked when setting up a new Tracer session. This method allows a
         * Probe to notify the user about initialization progress. The actual
         * initialization (and updating the TracerProgressObject) should be
         * performed in the sessionStarting() method. Useful for example for
         * messaging a delay during instrumention of classes in target application.
         *
         * @param snapshot profiler snapshot
         * @param refresh session refresh rate in miliseconds
         * @return TracerProgressObject null in default implementation
         */
        public TracerProgressObject sessionInitializing(IdeSnapshot snapshot, int refresh) {
            return null;
        }

        public void sessionStarting(IdeSnapshot snapshot)
                throws SessionInitializationException {}

        public void sessionRunning(IdeSnapshot snapshot) {}

        public void sessionStopping(IdeSnapshot snapshot) {}

        public void sessionFinished(IdeSnapshot snapshot) {}

        public void refreshRateChanged(IdeSnapshot snapshot, int refresh) {}

    }

}
