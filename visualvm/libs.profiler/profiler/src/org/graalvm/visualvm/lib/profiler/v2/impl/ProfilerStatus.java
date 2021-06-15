/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.profiler.v2.impl;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.graalvm.visualvm.lib.jfluid.TargetAppRunner;
import org.graalvm.visualvm.lib.common.Profiler;
import org.graalvm.visualvm.lib.common.event.SimpleProfilingStateAdapter;
import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;
import org.graalvm.visualvm.lib.jfluid.global.ProfilingSessionStatus;
import org.graalvm.visualvm.lib.profiler.api.ProfilerIDESettings;
import org.graalvm.visualvm.lib.profiler.v2.ProfilerSession;
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
