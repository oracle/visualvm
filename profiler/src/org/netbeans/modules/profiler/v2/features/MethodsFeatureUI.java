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
import java.awt.event.ItemEvent;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.ui.components.ProfilerToolbar;
import org.netbeans.lib.profiler.ui.cpu.CPUView;
import org.netbeans.modules.profiler.actions.ResetResultsAction;
import org.netbeans.modules.profiler.actions.TakeSnapshotAction;
import org.netbeans.modules.profiler.api.GoToSource;
import org.netbeans.modules.profiler.api.icons.GeneralIcons;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.v2.ui.GrayLabel;
import org.netbeans.modules.profiler.v2.ui.PopupButton;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "MethodsFeatureUI_viewHotSpots=Hot Spots",
    "MethodsFeatureUI_viewCallTree=Call Tree",
    "MethodsFeatureUI_viewCombined=Combined",
    "MethodsFeatureUI_selectedMethods=Selected methods",
    "MethodsFeatureUI_liveResults=Live results:",
    "MethodsFeatureUI_pauseResults=Pause live results",
    "MethodsFeatureUI_updateResults=Update live results",
    "MethodsFeatureUI_resultsMode=Results mode",
    "MethodsFeatureUI_profilingData=Profiling data:",
    "MethodsFeatureUI_snapshot=Snapshot"
})
abstract class MethodsFeatureUI extends FeatureUI {
    
    private ProfilerToolbar toolbar;
    private CPUView cpuView;

    
    // --- External implementation ---------------------------------------------
    
    abstract Set<ClientUtils.SourceCodeSelection> getClassesSelection();
    
    abstract Set<ClientUtils.SourceCodeSelection> getMethodsSelection();
    
    abstract void selectForProfiling(ClientUtils.SourceCodeSelection value);
    
    abstract Lookup.Provider getProject();
    
    abstract Profiler getProfiler();
    
    abstract void refreshResults();
    
    
    // --- API implementation --------------------------------------------------
    
    ProfilerToolbar getToolbar() {
        if (toolbar == null) initUI();
        return toolbar;
    }

    JPanel getResultsUI() {
        if (cpuView == null) initUI();
        return cpuView;
    }
    
    void sessionStateChanged(int sessionState) {
        refreshToolbar(sessionState);
    }
    
    
    void setForceRefresh() {
        if (cpuView != null) cpuView.setForceRefresh(true);
    }
    
    void refreshData() throws ClientUtils.TargetAppOrVMTerminated {
        if (cpuView != null) cpuView.refreshData();
    }
    
    void resetData() {
        if (cpuView != null) cpuView.resetData();
    }
    
    
    // --- UI ------------------------------------------------------------------
    
    private static enum View { HOT_SPOTS, CALL_TREE, COMBINED }
    
    private JLabel lrLabel;
    private JToggleButton lrPauseButton;
    private JButton lrRefreshButton;
    private PopupButton lrView;
    
    private JLabel pdLabel;
    private JButton pdSnapshotButton;
    private JButton pdResetResultsButton;
    
    private boolean popupPause;
    
    private View view;
    
    
    private void initUI() {
        
        assert SwingUtilities.isEventDispatchThread();
        
        // --- Results ---------------------------------------------------------
        
        cpuView = new CPUView(getProfiler().getTargetAppRunner().getProfilerClient(), getMethodsSelection(), GoToSource.isAvailable()) {
            public void showSource(ClientUtils.SourceCodeSelection value) {
                Lookup.Provider project = getProject();
                String className = value.getClassName();
                String methodName = value.getMethodName();
                String methodSig = value.getMethodSignature();
                GoToSource.openSource(project, className, methodName, methodSig);
            }
            public void selectForProfiling(ClientUtils.SourceCodeSelection value) {
                MethodsFeatureUI.this.selectForProfiling(value);
            }
            public void popupShowing() {
                if (lrPauseButton.isEnabled() && !lrRefreshButton.isEnabled()) {
                    popupPause = true;
                    lrPauseButton.setSelected(true);
                }
            }
            public void popupHidden() {
                if (lrPauseButton.isEnabled() && popupPause) {
                    popupPause = false;
                    lrPauseButton.setSelected(false);
                }
            }
        };
        
        
        // --- Toolbar ---------------------------------------------------------
        
        lrLabel = new GrayLabel(Bundle.MethodsFeatureUI_liveResults());
            
        lrPauseButton = new JToggleButton(Icons.getIcon(GeneralIcons.PAUSE)) {
            protected void fireItemStateChanged(ItemEvent event) {
                boolean paused = isSelected();
                cpuView.setPaused(paused);
                if (!paused) refreshResults();
                refreshToolbar(getSessionState());
            }
        };
        lrPauseButton.setToolTipText(Bundle.MethodsFeatureUI_pauseResults());
        lrPauseButton.setEnabled(false);

        lrRefreshButton = new JButton(Icons.getIcon(GeneralIcons.UPDATE_NOW)) {
            protected void fireActionPerformed(ActionEvent e) {
                refreshResults();
            }
        };
        lrRefreshButton.setToolTipText(Bundle.MethodsFeatureUI_updateResults());

        lrView = new PopupButton(Bundle.MethodsFeatureUI_viewHotSpots()) {
            protected void populatePopup(JPopupMenu popup) { populateViews(popup); }
        };
        lrView.setToolTipText(Bundle.MethodsFeatureUI_resultsMode());

        pdLabel = new GrayLabel(Bundle.MethodsFeatureUI_profilingData());

        pdSnapshotButton = new JButton(TakeSnapshotAction.getInstance());
        pdSnapshotButton.setHideActionText(true);
        pdSnapshotButton.setText(Bundle.MethodsFeatureUI_snapshot());

        pdResetResultsButton = new JButton(ResetResultsAction.getInstance());
        pdResetResultsButton.setHideActionText(true);

        toolbar = ProfilerToolbar.create(true);

        toolbar.addSpace(2);
        toolbar.addSeparator();
        toolbar.addSpace(5);

        toolbar.add(lrLabel);
        toolbar.addSpace(2);
        toolbar.add(lrPauseButton);
        toolbar.add(lrRefreshButton);
        toolbar.add(lrView);

        toolbar.addSpace(2);
        toolbar.addSeparator();
        toolbar.addSpace(5);

        toolbar.add(pdLabel);
        toolbar.addSpace(2);
        toolbar.add(pdSnapshotButton);
        toolbar.add(pdResetResultsButton);
        
        
        // --- Sync UI ---------------------------------------------------------
        
        setView(View.HOT_SPOTS);
        sessionStateChanged(getSessionState());
        
    }
    
    private void refreshToolbar(final int state) {
        if (toolbar != null) SwingUtilities.invokeLater(new Runnable() {
            public void run() {
//                boolean running = isRunning(state);
                boolean running = state == Profiler.PROFILING_RUNNING;
                lrPauseButton.setEnabled(running);
                lrRefreshButton.setEnabled(!popupPause && running && lrPauseButton.isSelected());
            }
        });
    }
    
    private void populateViews(JPopupMenu popup) {
        popup.add(new JRadioButtonMenuItem(Bundle.MethodsFeatureUI_viewHotSpots(), getView() == View.HOT_SPOTS) {
            protected void fireActionPerformed(ActionEvent e) { setView(View.HOT_SPOTS); }
        });
        
        popup.add(new JRadioButtonMenuItem(Bundle.MethodsFeatureUI_viewCallTree(), getView() == View.CALL_TREE) {
            protected void fireActionPerformed(ActionEvent e) { setView(View.CALL_TREE); }
        });
        
        popup.add(new JRadioButtonMenuItem(Bundle.MethodsFeatureUI_viewCombined(), getView() == View.COMBINED) {
            protected void fireActionPerformed(ActionEvent e) { setView(View.COMBINED); }
        });
    }

    private void setView(View view) {
        if (view == this.view) return;
        
        this.view = view;
        
        switch (view) {
            case HOT_SPOTS:
                cpuView.setView(false, true);
                lrView.setText(Bundle.MethodsFeatureUI_viewHotSpots());
                break;
            case CALL_TREE:
                cpuView.setView(true, false);
                lrView.setText(Bundle.MethodsFeatureUI_viewCallTree());
                break;
            case COMBINED:
                cpuView.setView(true, true);
                lrView.setText(Bundle.MethodsFeatureUI_viewCombined());
                break;
        }
        
        refreshResults();
    }
    
    private View getView() {
        return view;
    }
    
}
