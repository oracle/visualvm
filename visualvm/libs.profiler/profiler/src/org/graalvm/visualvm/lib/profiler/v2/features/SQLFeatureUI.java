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

package org.graalvm.visualvm.lib.profiler.v2.features;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.lib.jfluid.ProfilerClient;
import org.graalvm.visualvm.lib.jfluid.client.ClientUtils;
import org.graalvm.visualvm.lib.common.Profiler;
import org.graalvm.visualvm.lib.ui.components.HTMLTextArea;
import org.graalvm.visualvm.lib.ui.components.ProfilerToolbar;
import org.graalvm.visualvm.lib.ui.jdbc.LiveJDBCView;
import org.graalvm.visualvm.lib.ui.jdbc.LiveJDBCViewUpdater;
import org.graalvm.visualvm.lib.ui.swing.GrayLabel;
import org.graalvm.visualvm.lib.profiler.actions.ResetResultsAction;
import org.graalvm.visualvm.lib.profiler.actions.TakeSnapshotAction;
import org.graalvm.visualvm.lib.profiler.api.GoToSource;
import org.graalvm.visualvm.lib.profiler.api.icons.GeneralIcons;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.ProfilerIcons;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "LocksFeatureUI_SqlQueryCaption=SQL Query Viewer",
    "LocksFeatureUI_SqlQueryLabel=SQL Query:"
})
abstract class SQLFeatureUI extends FeatureUI {
    
    private ProfilerToolbar toolbar;
    private LiveJDBCView jdbcView;
    private LiveJDBCViewUpdater updater;
    
    
    // --- External implementation ---------------------------------------------
        
    abstract void selectForProfiling(ClientUtils.SourceCodeSelection value);
    
    abstract Lookup.Provider getProject();
    
    abstract ProfilerClient getProfilerClient();
    
    abstract void refreshResults();
    
   
    // --- API implementation --------------------------------------------------
    
    ProfilerToolbar getToolbar() {
        if (toolbar == null) initUI();
        return toolbar;
    }

    JPanel getResultsUI() {
        if (jdbcView == null) initUI();
        return jdbcView;
    }
    
    
    void sessionStateChanged(int sessionState) {
        refreshToolbar(sessionState);
        
//        if (sessionState == Profiler.PROFILING_INACTIVE || sessionState == Profiler.PROFILING_IN_TRANSITION) {
//            if (jdbcView != null) jdbcView.profilingSessionFinished();
//        } else if (sessionState == Profiler.PROFILING_RUNNING) {
//            if (jdbcView != null) jdbcView.profilingSessionStarted();
//        }
    }

    void resetPause() {
        if (lrPauseButton != null) lrPauseButton.setSelected(false);
    }
    
    void setForceRefresh() {
        if (updater != null) updater.setForceRefresh(true);
    }
    
    void refreshData() throws ClientUtils.TargetAppOrVMTerminated {
        if (updater != null) updater.update();
    }
        
    void resetData() {
        if (lrDeltasButton != null) {
            lrDeltasButton.setSelected(false);
            lrDeltasButton.setToolTipText(Bundle.MethodsFeatureUI_showDeltas());
        }
        if (jdbcView != null) {
            jdbcView.resetData();
            jdbcView.setDiffView(false);
        }
    }
    
    void cleanup() {
        if (jdbcView != null) jdbcView.cleanup();
    }
    
    
    // --- UI ------------------------------------------------------------------    
    
    private JLabel lrLabel;
    private JToggleButton lrPauseButton;
    private JButton lrRefreshButton;
    private JToggleButton lrDeltasButton;
    
    private JLabel pdLabel;
    private JButton pdSnapshotButton;
    private JButton pdResetResultsButton;
    
    private boolean popupPause;
    
    
    private void initUI() {
        
        assert SwingUtilities.isEventDispatchThread();
        
        // --- Results ---------------------------------------------------------

        jdbcView = new LiveJDBCView(null) {
            protected ProfilerClient getProfilerClient() {
                return SQLFeatureUI.this.getProfilerClient();
            }
            protected boolean showSourceSupported() {
                return GoToSource.isAvailable();
            }
            protected void showSource(ClientUtils.SourceCodeSelection value) {
                Lookup.Provider project = getProject();
                String className = value.getClassName();
                String methodName = value.getMethodName();
                String methodSig = value.getMethodSignature();
                GoToSource.openSource(project, className, methodName, methodSig);
            }
            protected void showSQLQuery(String query, String htmlQuery) {
                    HTMLTextArea area = new HTMLTextArea(htmlQuery);
                    JScrollPane areaScroll = new JScrollPane(area);
                    areaScroll.setPreferredSize(new Dimension(500, 250));
                    JLabel label = new JLabel(Bundle.LocksFeatureUI_SqlQueryLabel(), JLabel.LEADING);
                    label.setBorder(BorderFactory.createEmptyBorder(0, 0, 3, 0));
                    label.setLabelFor(area);
                    JPanel panel = new JPanel(new BorderLayout());
                    panel.add(label, BorderLayout.NORTH);
                    panel.add(areaScroll, BorderLayout.CENTER);
                    panel.setBorder(BorderFactory.createEmptyBorder(12, 10, 0, 10));
                    HelpCtx help = new HelpCtx("SqlQueryViewer.HelpCtx"); // NOI18N
                    DialogDisplayer.getDefault().notify(new DialogDescriptor(panel,
                            Bundle.LocksFeatureUI_SqlQueryCaption(), false,
                            new Object[] { DialogDescriptor.CLOSED_OPTION },
                            DialogDescriptor.CLOSED_OPTION, DialogDescriptor.BOTTOM_ALIGN, help, null));
                }
            protected void selectForProfiling(ClientUtils.SourceCodeSelection value) {
                SQLFeatureUI.this.selectForProfiling(value);
            }
            protected void popupShowing() {
                if (lrPauseButton.isEnabled() && !lrRefreshButton.isEnabled()) {
                    popupPause = true;
                    lrPauseButton.setSelected(true);
                }
            }
            protected void popupHidden() {
                if (lrPauseButton.isEnabled() && popupPause) {
                    popupPause = false;
                    lrPauseButton.setSelected(false);
                }
            }
        };
        
        jdbcView.putClientProperty("HelpCtx.Key", "ProfileSQL.HelpCtx"); // NOI18N
        
        updater = new LiveJDBCViewUpdater(jdbcView, getProfilerClient());
        
        
        // --- Toolbar ---------------------------------------------------------
        
        lrLabel = new GrayLabel(Bundle.MethodsFeatureUI_liveResults());
            
        lrPauseButton = new JToggleButton(Icons.getIcon(GeneralIcons.PAUSE)) {
            protected void fireItemStateChanged(ItemEvent event) {
                boolean paused = isSelected();
                updater.setPaused(paused);
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
        
        Icon icon = Icons.getIcon(ProfilerIcons.DELTA_RESULTS);
        lrDeltasButton = new JToggleButton(icon) {
            protected void fireActionPerformed(ActionEvent e) {
                if (!jdbcView.setDiffView(isSelected())) setSelected(false);
                setToolTipText(isSelected() ? Bundle.MethodsFeatureUI_showAbsolute() :
                                              Bundle.MethodsFeatureUI_showDeltas());
            }
        };
        lrDeltasButton.setToolTipText(Bundle.MethodsFeatureUI_showDeltas());
        
        pdLabel = new GrayLabel(Bundle.MethodsFeatureUI_profilingData());

        pdSnapshotButton = new JButton(TakeSnapshotAction.getInstance());
        pdSnapshotButton.setHideActionText(true);

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
        
        toolbar.addSpace(5);
        toolbar.add(lrDeltasButton);
        
        toolbar.addSpace(2);
        toolbar.addSeparator();
        toolbar.addSpace(5);

        toolbar.add(pdLabel);
        toolbar.addSpace(2);
        toolbar.add(pdSnapshotButton);
        toolbar.addSpace(3);
        toolbar.add(pdResetResultsButton);


        // --- Sync UI ---------------------------------------------------------
        
        sessionStateChanged(getSessionState());

    }
    
    private void refreshToolbar(final int state) {
        if (toolbar != null) SwingUtilities.invokeLater(new Runnable() {
            public void run() {
//                boolean running = isRunning(state);
                boolean running = state == Profiler.PROFILING_RUNNING;
                lrPauseButton.setEnabled(running);
                lrRefreshButton.setEnabled(!popupPause && running && lrPauseButton.isSelected());
                lrDeltasButton.setEnabled(running);
            }
        });
    }
    
}
