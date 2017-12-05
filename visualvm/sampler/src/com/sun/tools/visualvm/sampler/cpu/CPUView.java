/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.visualvm.sampler.cpu;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.profiling.actions.ProfilerResultsAction;
import com.sun.tools.visualvm.sampler.AbstractSamplerSupport;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.ItemEvent;
import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.results.cpu.CPUResultsSnapshot;
import org.netbeans.lib.profiler.results.cpu.StackTraceSnapshotBuilder;
import org.netbeans.lib.profiler.ui.components.ProfilerToolbar;
import org.netbeans.lib.profiler.ui.cpu.LiveCPUView;
import org.netbeans.lib.profiler.ui.swing.GrayLabel;
import org.netbeans.lib.profiler.ui.swing.MultiButtonGroup;
import org.netbeans.modules.profiler.actions.ResetResultsAction;
import org.netbeans.modules.profiler.actions.TakeSnapshotAction;
import org.netbeans.modules.profiler.api.GoToSource;
import org.netbeans.modules.profiler.api.icons.GeneralIcons;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
//    "MethodsFeatureUI_viewHotSpots=Hot spots",
//    "MethodsFeatureUI_viewCallTree=Call tree",
//    "MethodsFeatureUI_viewCombined=Combined",
    "MethodsFeatureUI_selectedMethods=Selected methods",
    "MethodsFeatureUI_liveResults=Results:",
    "MethodsFeatureUI_pauseResults=Pause live results",
    "MethodsFeatureUI_updateResults=Update live results",
    "MethodsFeatureUI_view=View:",
    "MethodsFeatureUI_viewForward=Forward calls",
    "MethodsFeatureUI_viewHotSpots=Hot spots",
    "MethodsFeatureUI_viewReverse=Reverse calls",
    "MethodsFeatureUI_resultsMode=Results mode",
    "MethodsFeatureUI_profilingData=Collected data:",
    "MethodsFeatureUI_snapshot=Snapshot",
    "MethodsFeatureUI_showAbsolute=Show absolute values",
    "MethodsFeatureUI_showDeltas=Show delta values"
})
final class CPUView extends JPanel {

    private final AbstractSamplerSupport.Refresher refresher;
    private boolean forceRefresh = false;
    
    private final CPUSamplerSupport.SnapshotDumper snapshotDumper;
    private final CPUSamplerSupport.ThreadDumper threadDumper;
    
    private StackTraceSnapshotBuilder builder;

    private ProfilerToolbar toolbar;
    private LiveCPUView cpuView;


    CPUView(AbstractSamplerSupport.Refresher refresher, CPUSamplerSupport.SnapshotDumper
            snapshotDumper, CPUSamplerSupport.ThreadDumper threadDumper, Application application) {
        this.refresher = refresher;
        this.snapshotDumper = snapshotDumper;
        this.threadDumper = threadDumper;
        
        initComponents(application);

        addHierarchyListener(new HierarchyListener() {
            public void hierarchyChanged(HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                    if (isShowing()) CPUView.this.refresher.refresh();
                }
            }
        });
    }

    
    void setBuilder(StackTraceSnapshotBuilder builder) {
        this.builder = builder;
    }

    void initSession() {
        pdSnapshotButton.setEnabled(false);
    }

    void refresh() {
        if (!isShowing() || (lrPauseButton.isSelected() && !forceRefresh)) return;
        forceRefresh = false;
        
        try {
            // TODO: perform out of the EDT!
            CPUResultsSnapshot snapshot = builder.createSnapshot(System.currentTimeMillis());
            cpuView.setData(snapshot, true);
        } catch (CPUResultsSnapshot.NoDataAvailableException ex) {
            Exceptions.printStackTrace(ex);
        }

        pdSnapshotButton.setEnabled(snapshotDumper != null);
    }

    void terminate() {
        lrPauseButton.setEnabled(false);
        lrRefreshButton.setEnabled(false);
        threaddumpButton.setEnabled(false);
    }

    
    private JLabel lrLabel;
    private JToggleButton lrPauseButton;
    private JButton lrRefreshButton;
    private JToggleButton lrDeltasButton;
    
    private JLabel pdLabel;
    private JButton pdSnapshotButton;
    private JButton pdResetResultsButton;
    
    private boolean popupPause;
    private JToggleButton[] toggles;
    
    private AbstractButton threaddumpButton;

    private void initComponents(Application application) {
        setLayout(new BorderLayout());
        setOpaque(false);
        
        
        cpuView = new LiveCPUView(null) {
            protected boolean showSourceSupported() {
                return GoToSource.isAvailable();
            }
            protected boolean profileMethodSupported() {
                return false;
            }
            protected boolean profileClassSupported() {
                return false;
            }
            protected void showSource(ClientUtils.SourceCodeSelection value) {
//                Lookup.Provider project = getProject();
                Lookup.Provider project = null;
                String className = value.getClassName();
                String methodName = value.getMethodName();
                String methodSig = value.getMethodSignature();
                GoToSource.openSource(project, className, methodName, methodSig);
            }
            protected void selectForProfiling(ClientUtils.SourceCodeSelection value) {
//                MethodsFeatureUI.this.selectForProfiling(value);
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
            protected void foundInForwardCalls() {
                super.foundInForwardCalls();
                toggles[0].setSelected(true);
            }
            protected void foundInHotSpots() {
                super.foundInHotSpots();
                toggles[1].setSelected(true);
            }
            protected void foundInReverseCalls() {
                super.foundInReverseCalls();
                toggles[2].setSelected(true);
            }
        };
        cpuView.putClientProperty(ProfilerResultsAction.PROP_APPLICATION, application);
        
        // --- Toolbar ---------------------------------------------------------
        
        lrLabel = new GrayLabel(Bundle.MethodsFeatureUI_liveResults());
            
        lrPauseButton = new JToggleButton(Icons.getIcon(GeneralIcons.PAUSE)) {
            protected void fireItemStateChanged(ItemEvent event) {
                boolean paused = lrPauseButton.isSelected();
                lrRefreshButton.setEnabled(paused && !popupPause);
                if (!paused) refresher.refresh();
            }
        };
        lrPauseButton.setToolTipText(Bundle.MethodsFeatureUI_pauseResults());
//        lrPauseButton.setEnabled(false);

        lrRefreshButton = new JButton(Icons.getIcon(GeneralIcons.UPDATE_NOW)) {
            protected void fireActionPerformed(ActionEvent e) {
                forceRefresh = true;
                refresher.refresh();
            }
        };
        lrRefreshButton.setToolTipText(Bundle.MethodsFeatureUI_updateResults());
        lrRefreshButton.setEnabled(false);
        
        Icon icon = Icons.getIcon(ProfilerIcons.DELTA_RESULTS);
        lrDeltasButton = new JToggleButton(icon) {
            protected void fireActionPerformed(ActionEvent e) {
                if (!cpuView.setDiffView(isSelected())) setSelected(false);
                setToolTipText(isSelected() ? Bundle.MethodsFeatureUI_showAbsolute() :
                                              Bundle.MethodsFeatureUI_showDeltas());
            }
        };
        lrDeltasButton.setToolTipText(Bundle.MethodsFeatureUI_showDeltas());
        
        MultiButtonGroup group = new MultiButtonGroup();
        toggles = new JToggleButton[3];
        
        JToggleButton forwardCalls = new JToggleButton(Icons.getIcon(ProfilerIcons.NODE_FORWARD)) {
            protected void fireActionPerformed(ActionEvent e) {
                super.fireActionPerformed(e);
                cpuView.setView(isSelected(), toggles[1].isSelected(), toggles[2].isSelected());
                refresh();
            }
        };
        forwardCalls.putClientProperty("JButton.buttonType", "segmented"); // NOI18N
        forwardCalls.putClientProperty("JButton.segmentPosition", "first"); // NOI18N
        forwardCalls.setToolTipText(Bundle.MethodsFeatureUI_viewForward());
        group.add(forwardCalls);
        toggles[0] = forwardCalls;
        forwardCalls.setSelected(true);
        
        JToggleButton hotSpots = new JToggleButton(Icons.getIcon(ProfilerIcons.TAB_HOTSPOTS)) {
            protected void fireActionPerformed(ActionEvent e) {
                super.fireActionPerformed(e);
                cpuView.setView(toggles[0].isSelected(), isSelected(), toggles[2].isSelected());
                refresh();
            }
        };
        hotSpots.putClientProperty("JButton.buttonType", "segmented"); // NOI18N
        hotSpots.putClientProperty("JButton.segmentPosition", "middle"); // NOI18N
        hotSpots.setToolTipText(Bundle.MethodsFeatureUI_viewHotSpots());
        group.add(hotSpots);
        toggles[1] = hotSpots;
        hotSpots.setSelected(false);
        
        JToggleButton reverseCalls = new JToggleButton(Icons.getIcon(ProfilerIcons.NODE_REVERSE)) {
            protected void fireActionPerformed(ActionEvent e) {
                super.fireActionPerformed(e);
                cpuView.setView(toggles[0].isSelected(), toggles[1].isSelected(), isSelected());
                refresh();
            }
        };
        reverseCalls.putClientProperty("JButton.buttonType", "segmented"); // NOI18N
        reverseCalls.putClientProperty("JButton.segmentPosition", "last"); // NOI18N
        reverseCalls.setToolTipText(Bundle.MethodsFeatureUI_viewReverse());
        group.add(reverseCalls);
        toggles[2] = reverseCalls;
        reverseCalls.setSelected(false);

        pdLabel = new GrayLabel(Bundle.MethodsFeatureUI_profilingData());

        pdSnapshotButton = new JButton(TakeSnapshotAction.getInstance()) {
            protected void fireActionPerformed(ActionEvent event) {
                snapshotDumper.takeSnapshot((event.getModifiers() & Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) == 0);
            }
        };
//        pdSnapshotButton.setHideActionText(true);
        pdSnapshotButton.setText(Bundle.MethodsFeatureUI_snapshot());

        pdResetResultsButton = new JButton(ResetResultsAction.getInstance());
        pdResetResultsButton.setHideActionText(true);

        toolbar = ProfilerToolbar.create(true);

//        toolbar.addSpace(2);
//        toolbar.addSeparator();
        toolbar.addSpace(5);

        toolbar.add(lrLabel);
        toolbar.addSpace(2);
        toolbar.add(lrPauseButton);
        toolbar.add(lrRefreshButton);
        
        toolbar.addSpace(5);
        toolbar.add(lrDeltasButton);
        
        toolbar.addSpace(2);
//        toolbar.addSeparator();
        toolbar.addSpace(5);
        
        toolbar.add(new GrayLabel(Bundle.MethodsFeatureUI_view()));
        toolbar.addSpace(2);
        toolbar.add(forwardCalls);
        toolbar.add(hotSpots);
        toolbar.add(reverseCalls);
        
        toolbar.addSpace(5);
        toolbar.add(cpuView.createThreadSelector());

        toolbar.addSpace(2);
        toolbar.addSeparator();
        toolbar.addSpace(5);

        toolbar.add(pdLabel);
        toolbar.addSpace(2);
        toolbar.add(pdSnapshotButton);
        toolbar.addSpace(3);
        toolbar.add(pdResetResultsButton);
        
        toolbar.addFiller();
        
        threaddumpButton = new JButton(NbBundle.getMessage(CPUView.class, "LBL_Thread_dump")) { // NOI18N
            protected void fireActionPerformed(ActionEvent event) {
                threadDumper.takeThreadDump((event.getModifiers() & Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) == 0);
            }
            public Dimension getPreferredSize() {
                Dimension dim = super.getPreferredSize();
                dim.width += 5;
                return dim;
            }
        };
        threaddumpButton.setToolTipText(NbBundle.getMessage(CPUView.class, "TOOLTIP_Thread_dump")); // NOI18N
        threaddumpButton.setOpaque(false);
        threaddumpButton.setEnabled(threadDumper != null);
        toolbar.add(threaddumpButton);
        
        
        cpuView.setView(true, false, false);
        
        
        add(toolbar.getComponent(), BorderLayout.NORTH);
        add(cpuView, BorderLayout.CENTER);
        
    }

}
