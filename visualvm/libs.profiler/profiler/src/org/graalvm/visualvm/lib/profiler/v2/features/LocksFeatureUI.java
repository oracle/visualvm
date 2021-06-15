/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.lib.jfluid.ProfilerClient;
import org.graalvm.visualvm.lib.jfluid.client.ClientUtils;
import org.graalvm.visualvm.lib.ui.components.ProfilerToolbar;
import org.graalvm.visualvm.lib.ui.locks.LiveLocksViewUpdater;
import org.graalvm.visualvm.lib.ui.locks.LockContentionPanel;
import org.graalvm.visualvm.lib.ui.swing.ActionPopupButton;
import org.graalvm.visualvm.lib.ui.swing.GrayLabel;
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
    private LiveLocksViewUpdater updater;

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
    }

    void resetPause() {
//        if (lrPauseButton != null) lrPauseButton.setSelected(false);
    }

    void setForceRefresh() {
        if (updater != null) updater.setForceRefresh(true);
    }
    
    void refreshData() throws ClientUtils.TargetAppOrVMTerminated {
        if (updater != null) updater.update();
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
