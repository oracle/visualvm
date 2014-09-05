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

import org.netbeans.modules.profiler.v2.ProfilerFeature;
import java.awt.event.ActionEvent;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.lib.profiler.ui.components.ProfilerToolbar;
import org.netbeans.lib.profiler.ui.locks.LockContentionPanel;
import org.netbeans.modules.profiler.NetBeansProfiler;
import org.netbeans.modules.profiler.actions.HeapDumpAction;
import org.netbeans.modules.profiler.actions.RunGCAction;
import org.netbeans.modules.profiler.actions.TakeThreadDumpAction;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import org.netbeans.modules.profiler.v2.ui.GrayLabel;
import org.netbeans.modules.profiler.v2.ui.PopupButton;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "LocksFeature_name=Locks",
    "LocksFeature_description=Collect lock contention data",
    "LocksFeature_show=View by:",
    "LocksFeature_aggregationByThreads=Threads",
    "LocksFeature_aggregationByMonitors=Monitors",
    "LocksFeature_application=Application:",
    "LocksFeature_threadDump=Thread Dump"
})
final class LocksFeature extends ProfilerFeature.Basic {
    
    private JLabel shLabel;
    private PopupButton shAggregation;
    
    private JLabel apLabel;
    private JButton apThreadDumpButton;
    private JButton apHeapDumpButton;
    private JButton apGCButton;
    
    private ProfilerToolbar toolbar;
    
    private LockContentionPanel locksPanel;
    
    
    LocksFeature() {
        super(Icons.getIcon(ProfilerIcons.WINDOW_LOCKS), Bundle.LocksFeature_name(),
              Bundle.LocksFeature_description(), 16);
    }
    

    public JPanel getResultsUI() {
        if (locksPanel == null) initResultsUI();
        return locksPanel;
    }
    
    public ProfilerToolbar getToolbar() {
        if (toolbar == null) {
            getResultsUI(); // locksPanel must be ready for toolbar actions
            
            shLabel = new GrayLabel(Bundle.LocksFeature_show());
            
            shAggregation = new PopupButton() {
                protected void populatePopup(JPopupMenu popup) { populateFilters(popup); }
            };
            
            apLabel = new GrayLabel(Bundle.LocksFeature_application());
            
            apThreadDumpButton = new JButton(TakeThreadDumpAction.getInstance());
            apThreadDumpButton.setHideActionText(true);
            apThreadDumpButton.setText(Bundle.LocksFeature_threadDump());
            
            apHeapDumpButton = new JButton(HeapDumpAction.getInstance());
            apHeapDumpButton.setHideActionText(true);
            apHeapDumpButton.setText(Bundle.MemoryFeature_heapDump());
            
            apGCButton = new JButton(RunGCAction.getInstance());
            apGCButton.setHideActionText(true);
            apGCButton.setText(Bundle.MemoryFeature_gc());
            
            toolbar = ProfilerToolbar.create(true);
            
            toolbar.addSpace(2);
            toolbar.addSeparator();
            toolbar.addSpace(5);
            
            toolbar.add(shLabel);
            toolbar.addSpace(2);
            toolbar.add(shAggregation);
            
            toolbar.addSpace(2);
            toolbar.addSeparator();
            toolbar.addSpace(5);
            
            toolbar.add(apLabel);
            toolbar.addSpace(2);
            toolbar.add(apThreadDumpButton);
            toolbar.add(apHeapDumpButton);
            toolbar.add(apGCButton);
            
            setAggregation(LockContentionPanel.Aggregation.BY_THREADS);
            
            refreshToolbar(getSessionState());
        }
        
        return toolbar;
    }
    
    public boolean supportsSettings(ProfilingSettings settings) {
        return true;
    }
    
    public void configureSettings(ProfilingSettings settings) {
        settings.setLockContentionMonitoringEnabled(true);
    }
    
    private void populateFilters(JPopupMenu popup) {
        LockContentionPanel.Aggregation a = locksPanel.getAggregation();
        
        popup.add(new JRadioButtonMenuItem(Bundle.LocksFeature_aggregationByThreads(), a == LockContentionPanel.Aggregation.BY_THREADS) {
            protected void fireActionPerformed(ActionEvent e) { setAggregation(LockContentionPanel.Aggregation.BY_THREADS); }
        });
        
        popup.add(new JRadioButtonMenuItem(Bundle.LocksFeature_aggregationByMonitors(), a == LockContentionPanel.Aggregation.BY_MONITORS) {
            protected void fireActionPerformed(ActionEvent e) { setAggregation(LockContentionPanel.Aggregation.BY_MONITORS); }
        });
    }

    private void setAggregation(LockContentionPanel.Aggregation aggregation) {
        locksPanel.setAggregation(aggregation);
        
        switch (aggregation) {
            case BY_THREADS:
                shAggregation.setText(Bundle.LocksFeature_aggregationByThreads());
                break;
            case BY_MONITORS:
                shAggregation.setText(Bundle.LocksFeature_aggregationByMonitors());
                break;
        }
    }
    
    private void initResultsUI() {
        locksPanel = new LockContentionPanel();
        locksPanel.lockContentionEnabled();
        profilingStateChanged(-1, getSessionState());
    }
    
//    private void refreshToolbar() {
//        ProjectSession session = getSession();
//        refreshToolbar(session == null ? null : session.getState());
//    }
    
    private void refreshToolbar(int state) {
        final boolean inactive = state == NetBeansProfiler.PROFILING_INACTIVE;
        if (toolbar != null) SwingUtilities.invokeLater(new Runnable() {
            public void run() {
//                boolean running = state == ProjectSession.State.RUNNING;
//                lrPauseButton.setEnabled(running);
//                lrRefreshButton.setEnabled(running && lrPauseButton.isSelected());
                
                shLabel.setEnabled(!inactive);
                apLabel.setEnabled(!inactive);
            }
        });
    }
    
    protected void profilingStateChanged(int oldState, int newState) {
        if (newState == NetBeansProfiler.PROFILING_INACTIVE || newState == NetBeansProfiler.PROFILING_IN_TRANSITION) {
            if (locksPanel != null) locksPanel.profilingSessionFinished();
        } else if (newState == NetBeansProfiler.PROFILING_RUNNING) {
            if (locksPanel != null) locksPanel.profilingSessionStarted();
        }
        refreshToolbar(newState);
    }
    
}
