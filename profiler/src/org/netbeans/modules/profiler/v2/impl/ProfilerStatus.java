/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.netbeans.modules.profiler.v2.impl;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.netbeans.lib.profiler.TargetAppRunner;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.common.event.SimpleProfilingStateAdapter;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.global.ProfilingSessionStatus;
import org.netbeans.modules.profiler.api.ProfilerIDESettings;
import org.netbeans.modules.profiler.v2.ProfilerSession;
import org.openide.awt.StatusDisplayer;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "ProfilerStatus_profilerStopped=Profiler stopped",
    "ProfilerStatus_profilerStarting=Profiler starting",
    "ProfilerStatus_profilerRunning=Profiler running",
    "ProfilerStatus_profilerRunningMethods=Profiler running, {0} methods instrumented",
    "ProfilerStatus_profilerRunningClasses=Profiler running, {0} classes instrumented"
})
public final class ProfilerStatus {
    
    private static final int STATUS_TIMEOUT = Integer.getInteger("org.openide.awt.StatusDisplayer.DISPLAY_TIME", 5000); // NOI18N
    private static final int STATUS_REFRESH = Math.min(STATUS_TIMEOUT - 250, 1500); // NOI18N
    
    private final ProfilerSession session;
    
    private volatile boolean logging;
    private final Timer refreshTimer;
    
    private int progressDots;
    private boolean wasRunning;
    
    private Reference<StatusDisplayer.Message> lastMessage;
    
    
    public static ProfilerStatus forSession(ProfilerSession session) {
        return new ProfilerStatus(session);
    }
    
    
    public void startSessionLogging() {
//        if (logging == false) {
            logging = true;
            updateStatus();
//        }
    }
    
    public void stopSessionLogging() {
        logging = false;
        clearStatus();
    }
    
    public void log(final String text) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                StatusDisplayer.Message message = StatusDisplayer.getDefault().setStatusText(text, 1000);
                message.clear(STATUS_TIMEOUT);
                lastMessage = new WeakReference(message);
            }
        });
    }
    
    
    private ProfilerStatus(ProfilerSession session) {
        this.session = session;
        
        refreshTimer = new Timer(STATUS_REFRESH, new ActionListener() {
            public void actionPerformed(ActionEvent e) { updateStatus(); }
        });
        refreshTimer.setRepeats(false);
        
        session.addListener(new SimpleProfilingStateAdapter() {
            public void update() { updateStatus(); }
        });
    }
    
    
    private void updateStatus() {
        if (!logging) return;
        if (!ProfilerIDESettings.getInstance().getLogProfilerStatus()) {
            clearStatus();
            return;
        }
        
        switch (session.getState()) {
            case Profiler.PROFILING_INACTIVE:
                if (wasRunning) {
                    log(Bundle.ProfilerStatus_profilerStopped());
                    wasRunning = false;
                }
                break;
            case Profiler.PROFILING_PAUSED:
//                log("Profiler paused");
                break;
            case Profiler.PROFILING_STARTED:
                log(Bundle.ProfilerStatus_profilerStarting());
                progressDots = 0;
                wasRunning = true;
                break;
            case Profiler.PROFILING_STOPPED:
//                log("Profiler stopped");
                break;
            case Profiler.PROFILING_IN_TRANSITION:
//                log("Profiler changing state");
                progressDots = 0;
                break;
            case Profiler.PROFILING_RUNNING:
                TargetAppRunner taRunner = session.getProfiler().getTargetAppRunner();
                ProfilingSessionStatus pss = taRunner.getProfilingSessionStatus();
                StringBuilder sb = new StringBuilder();
                switch (pss.currentInstrType) {
                    case CommonConstants.INSTR_RECURSIVE_FULL:
                    case CommonConstants.INSTR_RECURSIVE_SAMPLED:
                        sb.append(Bundle.ProfilerStatus_profilerRunningMethods(pss.getNInstrMethods()));
                        break;
                    case CommonConstants.INSTR_OBJECT_ALLOCATIONS:
                    case CommonConstants.INSTR_OBJECT_LIVENESS:
                        sb.append(Bundle.ProfilerStatus_profilerRunningClasses(pss.getNInstrClasses()));
                        break;
                    default:
                        sb.append(Bundle.ProfilerStatus_profilerRunning());
                }
                
                for (int i = 0; i < progressDots; i++) sb.append('.'); // NOI18N
                log(sb.toString());
                if (++progressDots > 3) progressDots = 0;
                
                refreshTimer.start();
                
                break;
        }
    }
    
    private void clearStatus() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                StatusDisplayer.Message message = lastMessage == null ? null :
                                                  lastMessage.get();
                if (message != null) message.clear(0);
            }
        });
    }
    
}
