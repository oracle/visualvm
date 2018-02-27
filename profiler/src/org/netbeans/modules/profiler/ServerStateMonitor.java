/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Oracle and/or its affiliates. All rights reserved.
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
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2012 Sun Microsystems, Inc.
 */
package org.netbeans.modules.profiler;

import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.common.event.SimpleProfilingStateAdapter;
import org.netbeans.lib.profiler.global.CommonConstants;
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
