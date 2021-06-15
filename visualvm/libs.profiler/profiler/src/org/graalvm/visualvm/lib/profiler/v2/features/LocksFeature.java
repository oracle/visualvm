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

package org.graalvm.visualvm.lib.profiler.v2.features;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.lib.jfluid.ProfilerClient;
import org.graalvm.visualvm.lib.jfluid.client.ClientUtils;
import org.graalvm.visualvm.lib.common.Profiler;
import org.graalvm.visualvm.lib.common.ProfilingSettings;
import org.graalvm.visualvm.lib.ui.components.ProfilerToolbar;
import org.graalvm.visualvm.lib.profiler.api.ProjectUtilities;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.ProfilerIcons;
import org.graalvm.visualvm.lib.profiler.v2.ProfilerFeature;
import org.graalvm.visualvm.lib.profiler.v2.ProfilerSession;
import org.graalvm.visualvm.lib.profiler.v2.impl.WeakProcessor;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "LocksFeature_name=Locks",
    "LocksFeature_description=Collect lock contention statistics",
    "LocksFeature_show=View by:",
    "LocksFeature_aggregationByThreads=Threads",
    "LocksFeature_aggregationByMonitors=Monitors",
    "LocksFeature_application=Application:",
    "LocksFeature_threadDump=Thread Dump"
})
final class LocksFeature extends ProfilerFeature.Basic {

    private final WeakProcessor processor;

    private LocksFeature(ProfilerSession session) {
        super(Icons.getIcon(ProfilerIcons.WINDOW_LOCKS), Bundle.LocksFeature_name(),
              Bundle.LocksFeature_description(), 16, session);

        assert !SwingUtilities.isEventDispatchThread();

        Lookup.Provider project = session.getProject();
        String projectName = project == null ? "External Process" : // NOI18N
                             ProjectUtilities.getDisplayName(project);
        processor = new WeakProcessor("MethodsFeature Processor for " + projectName); // NOI18N
    }
    
    
    // --- Settings ------------------------------------------------------------
    
    public void configureSettings(ProfilingSettings settings) {
        settings.setLockContentionMonitoringEnabled(true);
    }
    
    
    // --- Toolbar & Results UI ------------------------------------------------
    
    private LocksFeatureUI ui;
    
    public JPanel getResultsUI() {
        return getUI().getResultsUI();
    }
    
    public ProfilerToolbar getToolbar() {
        return getUI().getToolbar();
    }
    
    private LocksFeatureUI getUI() {
        if (ui == null) ui = new LocksFeatureUI() {
            int getSessionState() {
                return LocksFeature.this.getSessionState();
            }
            ProfilerClient getProfilerClient() {
                Profiler profiler = LocksFeature.this.getSession().getProfiler();
                return profiler.getTargetAppRunner().getProfilerClient();
            }
            void refreshResults() {
                LocksFeature.this.refreshResults();
            }
        };
        return ui;
    }

    // --- Live results --------------------------------------------------------
    
    private Runnable refresher;
    private volatile boolean running;
    
    
    private void startResults() {
        if (running) return;
        running = true;
        
        refresher = new Runnable() {
            public void run() {
                if (running) {
                    refreshView();
                    refreshResults(1500);
                }
            }
        };
        
        refreshResults(1000);
    }

    private void refreshView() {
        if (ui != null /* && ResultsManager.getDefault().resultsAvailable()*/) {
            try {
                ProfilingSettings settings = getSession().getProfilingSettings();
                if (ProfilingSettings.isCPUSettings(settings)
                   || ProfilingSettings.isJDBCSettings(settings)
                   || ProfilingSettings.isMemorySettings(settings)) {
                    // CPU or memory profiling will do refresh for us,
                    // it will call ProfilerClient.forceObtainedResultsDump()
                    return;
                }
                ui.refreshData();
            } catch (ClientUtils.TargetAppOrVMTerminated ex) {
                stopResults();
            }
        }
    }
    
    private void refreshResults() {
        if (running) processor.post(new Runnable() {
            public void run() {
                if (ui != null) ui.setForceRefresh();
                refreshView();
            }
        });
    }
    
    private void refreshResults(int delay) {
        if (running && refresher != null) processor.post(refresher, delay);
    }
    
    private void resetResults() {
        if (ui != null) ui.resetData();
    }

    private void stopResults() {
        if (refresher != null) {
            running = false;
            refresher = null;
        }
    }
    
    private void unpauseResults() {
        if (ui != null) ui.resetPause();
    }
     
    // --- Session lifecycle ---------------------------------------------------
    
    public void notifyActivated() {
        resetResults();
    }
    
    public void notifyDeactivated() {
        resetResults();
    }
    
    
    protected void profilingStateChanged(int oldState, int newState) {
        if (newState == Profiler.PROFILING_INACTIVE || newState == Profiler.PROFILING_IN_TRANSITION) {
            stopResults();
        } else if (isActivated() && newState == Profiler.PROFILING_RUNNING) {
            startResults();
        } else if (newState == Profiler.PROFILING_STARTED) {
            resetResults();
            unpauseResults();
        }
        
        if (ui != null) ui.sessionStateChanged(getSessionState());
    }
    
    
    // --- Provider ------------------------------------------------------------
    
    @ServiceProvider(service=ProfilerFeature.Provider.class)
    public static final class Provider extends ProfilerFeature.Provider {
        public ProfilerFeature getFeature(ProfilerSession session) {
            return new LocksFeature(session);
        }
    }
    
}
