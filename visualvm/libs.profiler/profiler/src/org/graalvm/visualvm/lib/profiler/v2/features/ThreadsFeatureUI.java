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

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.lib.common.Profiler;
import org.graalvm.visualvm.lib.ui.components.ProfilerToolbar;
import org.graalvm.visualvm.lib.ui.swing.ActionPopupButton;
import org.graalvm.visualvm.lib.ui.swing.GrayLabel;
import org.graalvm.visualvm.lib.ui.threads.ThreadsPanel;
import org.graalvm.visualvm.lib.profiler.api.ProfilerDialogs;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "ThreadsFeatureUI_show=Show:",
    "ThreadsFeatureUI_filterAll=All threads",
    "ThreadsFeatureUI_filterLive=Live threads",
    "ThreadsFeatureUI_filterFinished=Finished threads",
    "ThreadsFeatureUI_filterSelected=Selected threads",
    "ThreadsFeatureUI_timeline=Timeline:",
    "ThreadsFeatureUI_threadsFilter=Threads filter",
    "# HTML formatted:",
    "ThreadsFeatureUI_noThreadsMsg=<html><b>No threads are currently selected.</b><br><br>Use the Selected column or invoke Select thread action to select threads.</html>"
})
abstract class ThreadsFeatureUI extends FeatureUI {

    private ProfilerToolbar toolbar;
    private ThreadsPanel threadsView;


    // --- External implementation ---------------------------------------------

    abstract Profiler getProfiler();


    // --- API implementation --------------------------------------------------

    ProfilerToolbar getToolbar() {
        if (toolbar == null) initUI();
        return toolbar;
    }

    JPanel getResultsUI() {
        if (threadsView == null) initUI();
        return threadsView;
    }


    void cleanup() {
        if (threadsView != null) threadsView.cleanup();
    }
    
    
    void sessionStateChanged(int sessionState) {
        refreshToolbar(sessionState);
        
        if (sessionState == Profiler.PROFILING_INACTIVE || sessionState == Profiler.PROFILING_IN_TRANSITION) {
            if (threadsView != null) threadsView.profilingSessionFinished();
        } else if (sessionState == Profiler.PROFILING_RUNNING) {
            if (threadsView != null) threadsView.profilingSessionStarted();
        }
    }
    
    
    // --- UI ------------------------------------------------------------------
    
    private JLabel shLabel;
    private ActionPopupButton shFilter;
    
    private JLabel tlLabel;
    private JComponent tlZoomInButton;
    private JComponent tlZoomOutButton;
    private JComponent tlFitWidthButton;
    
    
    private void initUI() {
        
        assert SwingUtilities.isEventDispatchThread();
        
        // --- Results ---------------------------------------------------------
        
        threadsView = new ThreadsPanel(getProfiler().getThreadsManager(), null) {
            protected void filterSelected(ThreadsPanel.Filter filter) {
                super.filterSelected(filter);
                shFilter.selectAction(filter.ordinal());
            }
        };
        threadsView.threadsMonitoringEnabled();
        
        threadsView.putClientProperty("HelpCtx.Key", "ProfileThreads.HelpCtx"); // NOI18N
        
        
        // --- Toolbar ---------------------------------------------------------
        
        shLabel = new GrayLabel(Bundle.ThreadsFeatureUI_show());

        Action aAll = new AbstractAction() {
            { putValue(NAME, Bundle.ThreadsFeatureUI_filterAll()); }
            public void actionPerformed(ActionEvent e) { setFilter(ThreadsPanel.Filter.ALL); }
            
        };
        Action aLive = new AbstractAction() {
            { putValue(NAME, Bundle.ThreadsFeatureUI_filterLive()); }
            public void actionPerformed(ActionEvent e) { setFilter(ThreadsPanel.Filter.LIVE); }
            
        };
        Action aFinished = new AbstractAction() {
            { putValue(NAME, Bundle.ThreadsFeatureUI_filterFinished()); }
            public void actionPerformed(ActionEvent e) { setFilter(ThreadsPanel.Filter.FINISHED); }
            
        };
        Action aSelected = new AbstractAction() {
            { putValue(NAME, Bundle.ThreadsFeatureUI_filterSelected()); }
            public void actionPerformed(ActionEvent e) { setSelectedFilter(); }
            
        };
        shFilter = new ActionPopupButton(aAll, aLive, aFinished, aSelected);
        shFilter.setToolTipText(Bundle.ThreadsFeatureUI_threadsFilter());

        tlLabel = new GrayLabel(Bundle.ThreadsFeatureUI_timeline());


        tlZoomInButton = (JComponent)threadsView.getZoomIn();
        tlZoomInButton.putClientProperty("JButton.buttonType", "segmented"); // NOI18N
        tlZoomInButton.putClientProperty("JButton.segmentPosition", "first"); // NOI18N
        tlZoomOutButton = (JComponent)threadsView.getZoomOut();
        tlZoomOutButton.putClientProperty("JButton.buttonType", "segmented"); // NOI18N
        tlZoomOutButton.putClientProperty("JButton.segmentPosition", "middle"); // NOI18N
        tlFitWidthButton = (JComponent)threadsView.getFitWidth();
        tlFitWidthButton.putClientProperty("JButton.buttonType", "segmented"); // NOI18N
        tlFitWidthButton.putClientProperty("JButton.segmentPosition", "last"); // NOI18N

        toolbar = ProfilerToolbar.create(true);

        toolbar.addSpace(2);
        toolbar.addSeparator();
        toolbar.addSpace(5);

        toolbar.add(shLabel);
        toolbar.addSpace(2);
        toolbar.add(shFilter);

        toolbar.addSpace(2);
        toolbar.addSeparator();
        toolbar.addSpace(5);

        toolbar.add(tlLabel);
        toolbar.addSpace(2);
        toolbar.add(tlZoomInButton);
        toolbar.add(tlZoomOutButton);
        toolbar.add(tlFitWidthButton);
        
        
        // --- Sync UI ---------------------------------------------------------

        setFilter(ThreadsPanel.Filter.LIVE);
        sessionStateChanged(getSessionState());
        
    }
    
    private void refreshToolbar(final int state) {
//        if (toolbar != null) SwingUtilities.invokeLater(new Runnable() {
//            public void run() {
//            }
//        });
    }
    
    private void setSelectedFilter() {
        if (threadsView.hasSelectedThreads()) {
            setFilter(ThreadsPanel.Filter.SELECTED);
        } else {
            threadsView.showSelectedColumn();
            shFilter.selectAction(threadsView.getFilter().ordinal());
            ProfilerDialogs.displayWarning(Bundle.ThreadsFeatureUI_noThreadsMsg());
        }
    }

    private void setFilter(ThreadsPanel.Filter filter) {
        threadsView.setFilter(filter);
    }
    
}
