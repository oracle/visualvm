/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.profiler;

import org.netbeans.api.progress.ProgressHandle;
import org.graalvm.visualvm.lib.common.Profiler;
import org.graalvm.visualvm.lib.common.event.SimpleProfilingStateAdapter;
import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;
import org.openide.util.NbBundle;

/**
 * Monitor of the profiler state which displays status of the server in the main window status bar.
 *
 * @author Jan Taus
 */
@NbBundle.Messages({
    "ServerStateMonitor_ProfilerBusy=Profiler Busy",
    "ServerStateMonitor_ServerInitializing=Initializing...",
    "ServerStateMonitor_ServerPreparing=Preparing data...",
    "ServerStateMonitor_ServerInstrumenting=Instrumenting..."
})
class ServerStateMonitor {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final Profiler profiler;
    private ProgressHandle progressHandle = null;
    private int activeServerState = -1;
    private int activeServerProgress = -1;
    private int activeServerProgressValue = -1;

    //~ Constructors ---------------------------------------------------------------------------------------------------------

    ServerStateMonitor(Profiler profiler) {
        this.profiler = profiler;
        updateProgress();
        profiler.addProfilingStateListener(new SimpleProfilingStateAdapter() {
            @Override
            protected void update() {
                updateProgress();
            }
        });
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    private void updateProgress() {
        boolean display = profiler.getProfilingState() != Profiler.PROFILING_INACTIVE &&
                          profiler.getServerState() != CommonConstants.SERVER_RUNNING;
        if (display) {
            int serverProgress = profiler.getServerProgress();
            int serverState = profiler.getServerState();
            if (progressHandle == null) {
                progressHandle = ProgressHandle.createHandle(Bundle.ServerStateMonitor_ProfilerBusy());
                if (serverProgress == CommonConstants.SERVER_PROGRESS_INDETERMINATE) {
                    progressHandle.start();
                } else {
                    progressHandle.start(CommonConstants.SERVER_PROGRESS_WORKUNITS);
                }
                activeServerState = -1;
                activeServerProgress = serverProgress;
            }
            if (serverProgress != activeServerProgress) {
                if (activeServerProgress == CommonConstants.SERVER_PROGRESS_INDETERMINATE) {
                    progressHandle.switchToDeterminate(CommonConstants.SERVER_PROGRESS_WORKUNITS);
                    progressHandle.progress(serverProgress);
                    activeServerProgressValue = serverProgress;
                } else if (serverProgress == CommonConstants.SERVER_PROGRESS_INDETERMINATE) {
                    progressHandle.switchToIndeterminate();
                } else {
                    if (serverProgress > activeServerProgressValue) {
                        progressHandle.progress(serverProgress);
                        activeServerProgressValue = serverProgress;
                    }
                }
                activeServerProgress = serverProgress;
            }

            if (serverState != activeServerState) {
                activeServerState = serverState;
                switch (activeServerState) {
                    case CommonConstants.SERVER_INITIALIZING:
                        progressHandle.progress(Bundle.ServerStateMonitor_ServerInitializing());
                        break;
                    case CommonConstants.SERVER_INSTRUMENTING:
                        progressHandle.progress(Bundle.ServerStateMonitor_ServerInstrumenting());
                        break;
                    case CommonConstants.SERVER_PREPARING:
                        progressHandle.progress(Bundle.ServerStateMonitor_ServerPreparing());
                        break;
                    default:
                        progressHandle.progress(""); // NOI18N
                        break;
                }
            }
        } else {
            closeProgress();
        }
    }

    private void closeProgress() {
        if (progressHandle != null) {
            progressHandle.finish();
            progressHandle = null;
        }
    }
}
