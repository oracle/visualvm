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
import org.graalvm.visualvm.lib.common.Profiler;
import org.graalvm.visualvm.lib.common.ProfilingSettings;
import org.graalvm.visualvm.lib.ui.components.ProfilerToolbar;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.ProfilerIcons;
import org.graalvm.visualvm.lib.profiler.v2.ProfilerFeature;
import org.graalvm.visualvm.lib.profiler.v2.ProfilerSession;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "ThreadsFeature_name=Threads",
    "ThreadsFeature_description=Monitor thread states and times"
})
final class ThreadsFeature extends ProfilerFeature.Basic {

    private ThreadsFeature(ProfilerSession session) {
        super(Icons.getIcon(ProfilerIcons.WINDOW_THREADS), Bundle.ThreadsFeature_name(),
              Bundle.ThreadsFeature_description(), 15, session);
    }


    // --- Settings ------------------------------------------------------------

    public void configureSettings(ProfilingSettings settings) {
        settings.setThreadsMonitoringEnabled(true);
    }


    // --- Toolbar & Results UI ------------------------------------------------

    private ThreadsFeatureUI ui;

    public JPanel getResultsUI() {
        return getUI().getResultsUI();
    }

    public ProfilerToolbar getToolbar() {
        return getUI().getToolbar();
    }

    private ThreadsFeatureUI getUI() {
        if (ui == null) ui = new ThreadsFeatureUI() {
            int getSessionState() {
                return ThreadsFeature.this.getSessionState();
            }
            Profiler getProfiler() {
                return ThreadsFeature.this.getSession().getProfiler();
            }
        };
        return ui;
    }
    
    
    // --- Session lifecycle ---------------------------------------------------
    
    public void notifyActivated() {
        getSession().getProfiler().getThreadsManager().resetStates();
    }
    
    public void notifyDeactivated() {
        getSession().getProfiler().getThreadsManager().resetStates();
        
        if (ui != null) {
            ui.cleanup();
            ui = null;
        }
    }
    
    
    protected void profilingStateChanged(int oldState, int newState) {
        if (newState == Profiler.PROFILING_STARTED)
            getSession().getProfiler().getThreadsManager().reset();
        
        if (ui != null) ui.sessionStateChanged(getSessionState());
    }
    
    
    // --- Provider ------------------------------------------------------------
    
    @ServiceProvider(service=ProfilerFeature.Provider.class)
    public static final class Provider extends ProfilerFeature.Provider {
        public ProfilerFeature getFeature(ProfilerSession session) {
            return new ThreadsFeature(session);
        }
    }
    
}
