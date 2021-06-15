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
import java.awt.event.ItemEvent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.lib.common.Profiler;
import org.graalvm.visualvm.lib.ui.components.ProfilerToolbar;
import org.graalvm.visualvm.lib.ui.monitor.MonitorView;
import org.graalvm.visualvm.lib.ui.swing.GrayLabel;
import org.graalvm.visualvm.lib.ui.swing.MultiButtonGroup;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.ProfilerIcons;
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

    private static final String CPU_GRAPH_FLAG = "CPU_GRAPH_FLAG"; // NOI18N
    private static final String MEM_GRAPH_FLAG = "MEM_GRAPH_FLAG"; // NOI18N
    private static final String GC_GRAPH_FLAG = "GC_GRAPH_FLAG"; // NOI18N
    private static final String THCL_GRAPH_FLAG = "THCL_GRAPH_FLAG"; // NOI18N

    private ProfilerToolbar toolbar;
    private MonitorView monitorView;


    // --- External implementation ---------------------------------------------

    abstract Profiler getProfiler();

    abstract String readFlag(String flag, String defaultValue);

    abstract void storeFlag(String flag, String value);


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
            protected void fireItemStateChanged(ItemEvent event) {
                super.fireItemStateChanged(event);
                storeFlag(CPU_GRAPH_FLAG, isSelected() ? null : Boolean.FALSE.toString());
            }
        };
        cpuView.putClientProperty("JButton.buttonType", "segmented"); // NOI18N
        cpuView.putClientProperty("JButton.segmentPosition", "first"); // NOI18N
        cpuView.setToolTipText(Bundle.MonitorFeatureUI_cpuGraph());
        group.add(cpuView);
        boolean cpuGraphVisible = Boolean.parseBoolean(readFlag(CPU_GRAPH_FLAG, Boolean.TRUE.toString()));
        monitorView.setupCPUView(cpuGraphVisible);
        cpuView.setSelected(cpuGraphVisible);
        toolbar.add(cpuView);
        
        JToggleButton memoryView = new JToggleButton(Icons.getIcon(ProfilerIcons.MEMORY)) {
            protected void fireActionPerformed(ActionEvent e) {
                super.fireActionPerformed(e);
                monitorView.setupMemoryView(isSelected());
            }
            protected void fireItemStateChanged(ItemEvent event) {
                super.fireItemStateChanged(event);
                storeFlag(MEM_GRAPH_FLAG, isSelected() ? null : Boolean.FALSE.toString());
            }
        };
        memoryView.putClientProperty("JButton.buttonType", "segmented"); // NOI18N
        memoryView.putClientProperty("JButton.segmentPosition", "middle"); // NOI18N
        memoryView.setToolTipText(Bundle.MonitorFeatureUI_memoryGraph());
        group.add(memoryView);
        boolean memGraphVisible = Boolean.parseBoolean(readFlag(MEM_GRAPH_FLAG, Boolean.TRUE.toString()));
        monitorView.setupMemoryView(memGraphVisible);
        memoryView.setSelected(memGraphVisible);
        toolbar.add(memoryView);
        
        JToggleButton gcView = new JToggleButton(Icons.getIcon(ProfilerIcons.RUN_GC)) {
            protected void fireActionPerformed(ActionEvent e) {
                super.fireActionPerformed(e);
                monitorView.setupGCView(isSelected());
            }
            protected void fireItemStateChanged(ItemEvent event) {
                super.fireItemStateChanged(event);
                storeFlag(GC_GRAPH_FLAG, isSelected() ? null : Boolean.FALSE.toString());
            }
        };
        gcView.putClientProperty("JButton.buttonType", "segmented"); // NOI18N
        gcView.putClientProperty("JButton.segmentPosition", "middle"); // NOI18N
        gcView.setToolTipText(Bundle.MonitorFeatureUI_gcGraph());
        group.add(gcView);
        boolean gcGraphVisible = Boolean.parseBoolean(readFlag(GC_GRAPH_FLAG, Boolean.TRUE.toString()));
        monitorView.setupGCView(gcGraphVisible);
        gcView.setSelected(gcGraphVisible);
        toolbar.add(gcView);
        
        JToggleButton threadsView = new JToggleButton(Icons.getIcon(ProfilerIcons.WINDOW_THREADS)) {
            protected void fireActionPerformed(ActionEvent e) {
                super.fireActionPerformed(e);
                monitorView.setupThreadsView(isSelected());
            }
            protected void fireItemStateChanged(ItemEvent event) {
                super.fireItemStateChanged(event);
                storeFlag(THCL_GRAPH_FLAG, isSelected() ? null : Boolean.FALSE.toString());
            }
        };
        threadsView.putClientProperty("JButton.buttonType", "segmented"); // NOI18N
        threadsView.putClientProperty("JButton.segmentPosition", "last"); // NOI18N
        threadsView.setToolTipText(Bundle.MonitorFeatureUI_threadsGraph());
        group.add(threadsView);
        boolean thclGraphVisible = Boolean.parseBoolean(readFlag(THCL_GRAPH_FLAG, Boolean.TRUE.toString()));
        monitorView.setupThreadsView(thclGraphVisible);
        threadsView.setSelected(thclGraphVisible);
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
