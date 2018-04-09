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

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.ProfilerClient;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.ui.components.ProfilerToolbar;
import org.netbeans.lib.profiler.ui.locks.LockContentionPanel;
import org.netbeans.lib.profiler.ui.swing.ActionPopupButton;
import org.netbeans.lib.profiler.ui.swing.GrayLabel;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "LocksFeatureUI_show=View by:",
    "LocksFeatureUI_aggregationByThreads=Threads",
    "LocksFeatureUI_aggregationByMonitors=Monitors",
    "LocksFeatureUI_aggregationHint=Results aggregation"
})
abstract class LocksFeatureUI extends FeatureUI {
    
    private ProfilerToolbar toolbar;
    private LockContentionPanel locksView;
    
    // --- External implementation ---------------------------------------------
        
    abstract ProfilerClient getProfilerClient();
    
    abstract void refreshResults();
    
   
    // --- API implementation --------------------------------------------------
    
    ProfilerToolbar getToolbar() {
        if (toolbar == null) initUI();
        return toolbar;
    }

    JPanel getResultsUI() {
        if (locksView == null) initUI();
        return locksView;
    }
    
    
    void sessionStateChanged(int sessionState) {
        refreshToolbar(sessionState);
        
        if (sessionState == Profiler.PROFILING_INACTIVE || sessionState == Profiler.PROFILING_IN_TRANSITION) {
            if (locksView != null) locksView.profilingSessionFinished();
        } else if (sessionState == Profiler.PROFILING_RUNNING) {
            if (locksView != null) locksView.profilingSessionStarted();
        }
    }

    void resetPause() {
//        if (lrPauseButton != null) lrPauseButton.setSelected(false);
    }
    
    void setForceRefresh() {
        if (locksView != null) locksView.setForceRefresh(true);
    }
    
    void refreshData() throws ClientUtils.TargetAppOrVMTerminated {
        if (locksView != null) locksView.refreshData();
    }
        
    void resetData() {
        if (locksView != null) locksView.resetData();
    }
    
    
    // --- UI ------------------------------------------------------------------
    
    private JLabel shLabel;
    private ActionPopupButton shAggregation;
    
    
    private void initUI() {
        
        assert SwingUtilities.isEventDispatchThread();
        
        // --- Results ---------------------------------------------------------

        locksView = new LockContentionPanel() {
            protected ProfilerClient getProfilerClient() {
                return LocksFeatureUI.this.getProfilerClient();
            }
        };
        locksView.lockContentionEnabled();
        
        locksView.putClientProperty("HelpCtx.Key", "ProfileLocks.HelpCtx"); // NOI18N
        
        
        // --- Toolbar ---------------------------------------------------------
        
        shLabel = new GrayLabel(Bundle.LocksFeatureUI_show());
        
        Action aThreads = new AbstractAction() {
            { putValue(NAME, Bundle.LocksFeatureUI_aggregationByThreads()); }
            public void actionPerformed(ActionEvent e) { setAggregation(LockContentionPanel.Aggregation.BY_THREADS); }
            
        };
        Action aMonitors = new AbstractAction() {
            { putValue(NAME, Bundle.LocksFeatureUI_aggregationByMonitors()); }
            public void actionPerformed(ActionEvent e) { setAggregation(LockContentionPanel.Aggregation.BY_MONITORS); }
            
        };
        shAggregation = new ActionPopupButton(aThreads, aMonitors);
        shAggregation.setToolTipText(Bundle.LocksFeatureUI_aggregationHint());

        toolbar = ProfilerToolbar.create(true);

        toolbar.addSpace(2);
        toolbar.addSeparator();
        toolbar.addSpace(5);

        toolbar.add(shLabel);
        toolbar.addSpace(2);
        toolbar.add(shAggregation);


        // --- Sync UI ---------------------------------------------------------
        
        setAggregation(LockContentionPanel.Aggregation.BY_THREADS);
        sessionStateChanged(getSessionState());

    }
    
    private void refreshToolbar(final int state) {
//        if (toolbar != null) SwingUtilities.invokeLater(new Runnable() {
//            public void run() {
//            }
//        });
    }
    
    private void setAggregation(LockContentionPanel.Aggregation aggregation) {
        locksView.setAggregation(aggregation);
        shAggregation.selectAction(aggregation.ordinal());
    }
    
}
