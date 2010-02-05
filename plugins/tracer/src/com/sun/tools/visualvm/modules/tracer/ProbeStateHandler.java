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
public interface ProbeStateHandler<X extends DataSource> {

    // Probe added to Probes graph
    public void probeAdded(X dataSource);

    // Probe removed from Probes graph
    public void probeRemoved(X dataSource);


    // Tracer session is starting
    // Setup probe, deploy, instrument...
    public void sessionStarting(X dataSource)
            throws SessionInitializationException;

    // Tracer session is running
    public void sessionRunning(X dataSource);

    // Tracer session is stopping
    // Uninstrument, undeploy, disable...
    public void sessionStopping(X dataSource);

    // Tracer session is stopped
    public void sessionFinished(X dataSource);


//    // Tracer UI is closed or target has finished
//    public void sessionImpossible(X dataSource);


    public class Adapter<X extends DataSource> implements ProbeStateHandler<X> {

        public void probeAdded(X dataSource) {}

        public void probeRemoved(X dataSource) {}

        public void sessionStarting(X dataSource)
                throws SessionInitializationException {}

        public void sessionRunning(X dataSource) {}

        public void sessionStopping(X dataSource) {}

        public void sessionFinished(X dataSource) {}

//        public void sessionImpossible(X dataSource) {}

    }

}
