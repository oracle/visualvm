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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.ui.components.ProfilerToolbar;
import org.netbeans.lib.profiler.ui.monitor.MonitorView;
import org.netbeans.lib.profiler.ui.swing.GrayLabel;
import org.netbeans.lib.profiler.ui.swing.MultiButtonGroup;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "MonitorFeatureUI_graphs=View:",
    "MonitorFeatureUI_cpuGraph=CPU and GC",
    "MonitorFeatureUI_memoryGraph=Memory",
    "MonitorFeatureUI_gcGraph=Garbage Collection",
    "MonitorFeatureUI_threadsGraph=Threads and Classes"
})
abstract class MonitorFeatureUI extends FeatureUI {
    
    private ProfilerToolbar toolbar;
    private MonitorView monitorView;
    
    
    // --- External implementation ---------------------------------------------
    
    abstract Profiler getProfiler();
    
    
    // --- API implementation --------------------------------------------------
    
    ProfilerToolbar getToolbar() {
        if (toolbar == null) initUI();
        return toolbar;
    }

    JPanel getResultsUI() {
        if (monitorView == null) initUI();
        return monitorView;
    }
    
    
    void cleanup() {
        if (monitorView != null) monitorView.cleanup();
    }
    
    
    void sessionStateChanged(int sessionState) {
        refreshToolbar(sessionState);
    }
    
    
    // --- UI ------------------------------------------------------------------
    
    private JLabel grLabel;
    
    
    private void initUI() {
        
        assert SwingUtilities.isEventDispatchThread();
        
        // --- Results ---------------------------------------------------------
        
        monitorView = new MonitorView(getProfiler().getVMTelemetryManager());
        
        monitorView.putClientProperty("HelpCtx.Key", "ProfileTelemetry.HelpCtx"); // NOI18N
        
        
        // --- Toolbar ---------------------------------------------------------
        
        MultiButtonGroup group = new MultiButtonGroup();

        toolbar = ProfilerToolbar.create(true);

        toolbar.addSpace(2);
        toolbar.addSeparator();
        toolbar.addSpace(5);

        grLabel = new GrayLabel(Bundle.MonitorFeatureUI_graphs());
        toolbar.add(grLabel);
        
        toolbar.addSpace(2);
        
        JToggleButton cpuView = new JToggleButton(Icons.getIcon(ProfilerIcons.CPU)) {
            protected void fireActionPerformed(ActionEvent e) {
                super.fireActionPerformed(e);
                monitorView.setupCPUView(isSelected());
            }
        };
        cpuView.putClientProperty("JButton.buttonType", "segmented"); // NOI18N
        cpuView.putClientProperty("JButton.segmentPosition", "first"); // NOI18N
        cpuView.setToolTipText(Bundle.MonitorFeatureUI_cpuGraph());
        group.add(cpuView);
        monitorView.setupCPUView(true);
        cpuView.setSelected(true);
        toolbar.add(cpuView);
        
        JToggleButton memoryView = new JToggleButton(Icons.getIcon(ProfilerIcons.MEMORY)) {
            protected void fireActionPerformed(ActionEvent e) {
                super.fireActionPerformed(e);
                monitorView.setupMemoryView(isSelected());
            }
        };
        memoryView.putClientProperty("JButton.buttonType", "segmented"); // NOI18N
        memoryView.putClientProperty("JButton.segmentPosition", "middle"); // NOI18N
        memoryView.setToolTipText(Bundle.MonitorFeatureUI_memoryGraph());
        group.add(memoryView);
        monitorView.setupMemoryView(true);
        memoryView.setSelected(true);
        toolbar.add(memoryView);
        
        JToggleButton gcView = new JToggleButton(Icons.getIcon(ProfilerIcons.RUN_GC)) {
            protected void fireActionPerformed(ActionEvent e) {
                super.fireActionPerformed(e);
                monitorView.setupGCView(isSelected());
            }
        };
        gcView.putClientProperty("JButton.buttonType", "segmented"); // NOI18N
        gcView.putClientProperty("JButton.segmentPosition", "middle"); // NOI18N
        gcView.setToolTipText(Bundle.MonitorFeatureUI_gcGraph());
        group.add(gcView);
        monitorView.setupGCView(true);
        gcView.setSelected(true);
        toolbar.add(gcView);
        
        JToggleButton threadsView = new JToggleButton(Icons.getIcon(ProfilerIcons.WINDOW_THREADS)) {
            protected void fireActionPerformed(ActionEvent e) {
                super.fireActionPerformed(e);
                monitorView.setupThreadsView(isSelected());
            }
        };
        threadsView.putClientProperty("JButton.buttonType", "segmented"); // NOI18N
        threadsView.putClientProperty("JButton.segmentPosition", "last"); // NOI18N
        threadsView.setToolTipText(Bundle.MonitorFeatureUI_threadsGraph());
        group.add(threadsView);
        monitorView.setupThreadsView(true);
        threadsView.setSelected(true);
        toolbar.add(threadsView);
        
        
        // --- Sync UI ---------------------------------------------------------
        
        sessionStateChanged(getSessionState());
        
    }
    
    private void refreshToolbar(final int state) {
//        if (toolbar != null) SwingUtilities.invokeLater(new Runnable() {
//            public void run() {
//            }
//        });
    }
    
}
