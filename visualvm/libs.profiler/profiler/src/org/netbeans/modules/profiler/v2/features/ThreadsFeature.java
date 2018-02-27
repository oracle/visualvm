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

import javax.swing.JPanel;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.lib.profiler.ui.components.ProfilerToolbar;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import org.netbeans.modules.profiler.v2.ProfilerFeature;
import org.netbeans.modules.profiler.v2.ProfilerSession;
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
