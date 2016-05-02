/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2014 Oracle and/or its affiliates. All rights reserved.
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

package org.netbeans.modules.profiler.v2.features;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.PreparedStatement;
import java.sql.Statement;
import javax.swing.JPanel;
import org.netbeans.lib.profiler.ProfilerClient;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.lib.profiler.common.filters.SimpleFilter;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.results.jdbc.JdbcCCTProvider;
import org.netbeans.lib.profiler.ui.components.ProfilerToolbar;
import org.netbeans.lib.profiler.utils.Wildcards;
import org.netbeans.modules.profiler.ResultsListener;
import org.netbeans.modules.profiler.ResultsManager;
import org.netbeans.modules.profiler.api.ProjectUtilities;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import org.netbeans.modules.profiler.v2.ProfilerFeature;
import org.netbeans.modules.profiler.v2.ProfilerSession;
import org.netbeans.modules.profiler.v2.impl.WeakProcessor;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "SQLFeature_name=SQL Queries",
    "SQLFeature_description=Display executed SQL queries, their duration and invocation paths"
})
final class SQLFeature extends ProfilerFeature.Basic {
    
    private static final String[] jdbcMarkerClasses = {
        JdbcCCTProvider.DRIVER_INTERFACE,
        JdbcCCTProvider.CONNECTION_INTERFACE,
        JdbcCCTProvider.STATEMENT_INTERFACE,
        JdbcCCTProvider.PREPARED_STATEMENT_INTERFACE,
        JdbcCCTProvider.CALLABLE_STATEMENT_INTERFACE
    };

    private final WeakProcessor processor;
    
    
    private SQLFeature(ProfilerSession session) {
        super(Icons.getIcon(ProfilerIcons.WINDOW_SQL), Bundle.SQLFeature_name(),
              Bundle.SQLFeature_description(), 20, session);
        
        Lookup.Provider project = session.getProject();
        String projectName = project == null ? "External Process" : // NOI18N
                             ProjectUtilities.getDisplayName(project);
        processor = new WeakProcessor("SQLFeature Processor for " + projectName); // NOI18N
    }
    
    
    // --- Settings ------------------------------------------------------------
    
    public boolean supportsSettings(ProfilingSettings psettings) {
        return !ProfilingSettings.isMemorySettings(psettings);
    }
    
    public void configureSettings(ProfilingSettings settings) {
        settings.setProfilingType(ProfilingSettings.PROFILE_CPU_JDBC);
        settings.setCPUProfilingType(CommonConstants.CPU_INSTR_FULL);

        ClientUtils.SourceCodeSelection[] roots = new ClientUtils.SourceCodeSelection[jdbcMarkerClasses.length];
        for (int i = 0; i < jdbcMarkerClasses.length; i++) {
            roots[i] = new ClientUtils.SourceCodeSelection(jdbcMarkerClasses[i], "*", null); // NOI18N
            roots[i].setMarkerMethod(true);
        }
        settings.addRootMethods(roots);
        settings.setSelectedInstrumentationFilter(SimpleFilter.NO_FILTER);
}
    
    
    // --- Toolbar & Results UI ------------------------------------------------
    
    private SQLFeatureUI ui;
    
    public JPanel getResultsUI() {
        return getUI().getResultsUI();
    }
    
    public ProfilerToolbar getToolbar() {
        return getUI().getToolbar();
    }
    
    private SQLFeatureUI getUI() {
        if (ui == null) ui = new SQLFeatureUI() {
            void selectForProfiling(final ClientUtils.SourceCodeSelection value) {
                RequestProcessor.getDefault().post(new Runnable() {
                    public void run() {
                        String name = Wildcards.ALLWILDCARD.equals(value.getMethodName()) ?
                                      "Profile Class" :
                                      "Profile Method";
                        ProfilerSession.findAndConfigure(Lookups.fixed(value), getProject(), name);
                    }
                });
            }
            Lookup.Provider getProject() {
                return SQLFeature.this.getSession().getProject();
            }
            ProfilerClient getProfilerClient() {
                Profiler profiler = SQLFeature.this.getSession().getProfiler();
                return profiler.getTargetAppRunner().getProfilerClient();
            }
            int getSessionState() {
                return SQLFeature.this.getSessionState();
            }
            void refreshResults() {
                SQLFeature.this.refreshResults();
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
        if (ui != null && ResultsManager.getDefault().resultsAvailable()) {
            try {
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
    
    private SQLResetter resetter;
    
    public void notifyActivated() {
        resetResults();
        
        resetter = Lookup.getDefault().lookup(SQLResetter.class);
        resetter.controller = this;
    }
    
    public void notifyDeactivated() {
        resetResults();
        
        if (resetter != null) {
            resetter.controller = null;
            resetter = null;
        }
        
        if (ui != null) {
            ui.cleanup();
            ui = null;
        }
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
    
    
    @ServiceProvider(service=ResultsListener.class)
    public static final class SQLResetter implements ResultsListener {
        private SQLFeature controller;
        public void resultsAvailable() { /*if (controller != null) controller.refreshView();*/ }
        public void resultsReset() { if (controller != null && controller.ui != null) controller.ui.resetData(); }
    }
    
    
    // --- Provider ------------------------------------------------------------
    
    @ServiceProvider(service=ProfilerFeature.Provider.class)
    public static final class Provider extends ProfilerFeature.Provider {
        public ProfilerFeature getFeature(ProfilerSession session) {
            //return Boolean.getBoolean("org.netbeans.modules.profiler.features.enableSQL") ? new SQLFeature(session) : null; // NOI18N
            return new SQLFeature(session);
        }
    }
    
}
