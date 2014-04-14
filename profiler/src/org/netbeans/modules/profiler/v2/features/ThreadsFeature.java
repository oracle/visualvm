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

import java.awt.Component;
import java.awt.event.ActionEvent;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.lib.profiler.ui.components.ProfilerToolbar;
import org.netbeans.lib.profiler.ui.threads.ThreadsPanel;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import org.netbeans.modules.profiler.v2.session.ProjectSession;
import org.netbeans.modules.profiler.v2.ui.components.GrayLabel;
import org.netbeans.modules.profiler.v2.ui.components.PopupButton;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "ThreadsFeature_name=Threads",
    "ThreadsFeature_show=Show:",
    "ThreadsFeature_filterAll=All threads",
    "ThreadsFeature_filterLive=Live threads",
    "ThreadsFeature_filterFinished=Finished threads",
    "ThreadsFeature_timeline=Timeline:",
    "ThreadsFeature_application=Application:",
    "ThreadsFeature_threadDump=Thread Dump",
    "ThreadsFeature_heapDump=Heap Dump",
    "ThreadsFeature_gc=GC"
})
final class ThreadsFeature extends ProfilerFeature.Basic {
    
    private JLabel shLabel;
    private PopupButton shFilter;
    
    private JLabel tlLabel;
    private Component tlZoomInButton;
    private Component tlZoomOutButton;
    private Component tlFitWidthButton;
    
    private JLabel apLabel;
    private JButton apThreadDumpButton;
    
    private ProfilerToolbar toolbar;
    
    private ThreadsPanel threadsPanel;
    
    
    ThreadsFeature() {
        super(Bundle.ThreadsFeature_name(), Icons.getIcon(ProfilerIcons.WINDOW_THREADS), 15);
    }
    
    
    public JPanel getResultsUI() {
        if (threadsPanel == null) initResultsUI();
        return threadsPanel;
    }
    
    public ProfilerToolbar getToolbar() {
        if (toolbar == null) {
            getResultsUI(); // threadsPanel must be ready for toolbar actions
            
            shLabel = new GrayLabel(Bundle.ThreadsFeature_show());
            
            shFilter = new PopupButton() {
                protected void populatePopup(JPopupMenu popup) { populateFilters(popup); }
            };
            
            tlLabel = new GrayLabel(Bundle.ThreadsFeature_timeline());
            
            
            tlZoomInButton = threadsPanel.getZoomIn();
            tlZoomOutButton = threadsPanel.getZoomOut();
            tlFitWidthButton = threadsPanel.getFitWidth();
            
            apLabel = new GrayLabel(Bundle.ThreadsFeature_application());
            
            apThreadDumpButton = new JButton(Bundle.ThreadsFeature_threadDump(), Icons.getIcon(ProfilerIcons.WINDOW_THREADS));
            apThreadDumpButton.setEnabled(false);
            
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
            
            toolbar.addSpace(2);
            toolbar.addSeparator();
            toolbar.addSpace(5);
            
            toolbar.add(apLabel);
            toolbar.addSpace(2);
            toolbar.add(apThreadDumpButton);
            
            setFilter(ThreadsPanel.Filter.ALL);
        }
        
        return toolbar;
    }
    
    public boolean supportsSettings(ProfilingSettings settings) {
        return true;
    }
    
    public void configureSettings(ProfilingSettings settings) {
        settings.setThreadsMonitoringEnabled(true);
        settings.setThreadsSamplingEnabled(false);
    }
    
    private void populateFilters(JPopupMenu popup) {
        ThreadsPanel.Filter f = threadsPanel.getFilter();
        
        popup.add(new JRadioButtonMenuItem(Bundle.ThreadsFeature_filterAll(), f == ThreadsPanel.Filter.ALL) {
            protected void fireActionPerformed(ActionEvent e) { setFilter(ThreadsPanel.Filter.ALL); }
        });
        
        popup.add(new JRadioButtonMenuItem(Bundle.ThreadsFeature_filterLive(), f == ThreadsPanel.Filter.LIVE) {
            protected void fireActionPerformed(ActionEvent e) { setFilter(ThreadsPanel.Filter.LIVE); }
        });
        
        popup.add(new JRadioButtonMenuItem(Bundle.ThreadsFeature_filterFinished(), f == ThreadsPanel.Filter.FINISHED) {
            protected void fireActionPerformed(ActionEvent e) { setFilter(ThreadsPanel.Filter.FINISHED); }
        });
    }

    private void setFilter(ThreadsPanel.Filter filter) {
        threadsPanel.setFilter(filter);
        
        switch (filter) {
            case ALL:
                shFilter.setText(Bundle.ThreadsFeature_filterAll());
                break;
            case LIVE:
                shFilter.setText(Bundle.ThreadsFeature_filterLive());
                break;
            case FINISHED:
                shFilter.setText(Bundle.ThreadsFeature_filterFinished());
                break;
        }
    }
    
    private void initResultsUI() {
        threadsPanel = new ThreadsPanel(Profiler.getDefault().getThreadsManager(), null);
        threadsPanel.threadsMonitoringEnabled();
        stateChanged(null, getSessionState());
    }
    
//    private void refreshToolbar() {
//        ProjectSession session = getSession();
//        refreshToolbar(session == null ? null : session.getState());
//    }
    
    private void refreshToolbar(final ProjectSession.State state) {
        if (toolbar != null) SwingUtilities.invokeLater(new Runnable() {
            public void run() {
//                boolean running = state == ProjectSession.State.RUNNING;
//                lrPauseButton.setEnabled(running);
//                lrRefreshButton.setEnabled(running && lrPauseButton.isSelected());
                
                boolean inactive = state == ProjectSession.State.INACTIVE;
                shLabel.setEnabled(!inactive);
                tlLabel.setEnabled(!inactive);
                apLabel.setEnabled(!inactive);
            }
        });
    }
    
    public void stateChanged(ProjectSession.State oldState, ProjectSession.State newState) {
        if (newState == null || newState == ProjectSession.State.INACTIVE) {
            if (threadsPanel != null) threadsPanel.profilingSessionFinished();
        } else if (newState == ProjectSession.State.RUNNING) {
            if (threadsPanel != null) threadsPanel.profilingSessionStarted();
        }
        refreshToolbar(newState);
    }
    
    public void attachedToSession(ProjectSession session) {
        super.attachedToSession(session);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Profiler.getDefault().getThreadsManager().resetStates();
            }
        });
    }
    
    public void detachedFromSession(ProjectSession session) {
        super.detachedFromSession(session);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Profiler.getDefault().getThreadsManager().resetStates();
            }
        });
    }
    
}
