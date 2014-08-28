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
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.lib.profiler.ui.components.ProfilerToolbar;
import org.netbeans.lib.profiler.ui.monitor.MonitorView;
import org.netbeans.modules.profiler.NetBeansProfiler;
import org.netbeans.modules.profiler.actions.HeapDumpAction;
import org.netbeans.modules.profiler.actions.RunGCAction;
import org.netbeans.modules.profiler.actions.TakeThreadDumpAction;
import org.netbeans.modules.profiler.api.icons.GeneralIcons;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import org.netbeans.modules.profiler.v2.ProfilerSession;
import org.netbeans.modules.profiler.v2.ui.GrayLabel;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "MonitorFeature_name=Telemetry",
    "MonitorFeature_description=Monitor CPU and Memory usage, number of threads and loaded classes",
    "MonitorFeature_graphs=Graphs:",
    "MonitorFeature_application=Application:",
    "MonitorFeature_threadDump=Thread Dump",
    "MonitorFeature_heapDump=Heap Dump",
    "MonitorFeature_gc=GC"
})
final class MonitorFeature extends ProfilerFeature.Basic {
    
    private JLabel grLabel;
    private JButton grZoomInButton;
    private JButton grZoomOutButton;
    private JToggleButton grFitWidthButton;
    
    private JLabel apLabel;
    private JButton apThreadDumpButton;
    private JButton apHeapDumpButton;
    private JButton apGCButton;
    
    private ProfilerToolbar toolbar;
    
    private MonitorView monitorView;
    
    
    MonitorFeature() {
        super(Icons.getIcon(ProfilerIcons.MONITORING), Bundle.MonitorFeature_name(),
              Bundle.MonitorFeature_description(), 10);
    }

    
    public JPanel getResultsUI() {
        if (monitorView == null) initResultsUI();
        return monitorView;
    }
    
    public ProfilerToolbar getToolbar() {
        if (toolbar == null) {
            grLabel = new GrayLabel(Bundle.MonitorFeature_graphs());
            
            grZoomInButton = new JButton(Icons.getIcon(GeneralIcons.ZOOM_IN));
            grZoomInButton.setEnabled(false);
            
            grZoomOutButton = new JButton(Icons.getIcon(GeneralIcons.ZOOM_OUT));
            grZoomOutButton.setEnabled(false);
            
            grFitWidthButton = new JToggleButton(Icons.getIcon(GeneralIcons.SCALE_TO_FIT));
            grFitWidthButton.setEnabled(false);
            
            apLabel = new GrayLabel(Bundle.MonitorFeature_application());
            
            apThreadDumpButton = new JButton(TakeThreadDumpAction.getInstance());
            apThreadDumpButton.setHideActionText(true);
            apThreadDumpButton.setText(Bundle.MonitorFeature_threadDump());
            
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
            
            toolbar.add(grLabel);
            toolbar.addSpace(2);
            toolbar.add(grZoomInButton);
            toolbar.add(grZoomOutButton);
            toolbar.add(grFitWidthButton);
            
            toolbar.addSpace(2);
            toolbar.addSeparator();
            toolbar.addSpace(5);
            
            toolbar.add(apLabel);
            toolbar.addSpace(2);
            toolbar.add(apThreadDumpButton);
            toolbar.add(apHeapDumpButton);
            toolbar.add(apGCButton);
            
            refreshToolbar(getSessionState());
        }
        
        return toolbar;
    }
    
    public boolean supportsSettings(ProfilingSettings settings) {
        return true;
    }
    
    public void configureSettings(ProfilingSettings settings) {
    }
    
    private void initResultsUI() {
        monitorView = new MonitorView(getSession().getProfiler().getVMTelemetryManager());
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
                
                grLabel.setEnabled(!inactive);
                apLabel.setEnabled(!inactive);
            }
        });
    }
    
    protected void profilingStateChanged(int oldState, int newState) {
        refreshToolbar(newState);
    }
    
    public void attachedToSession(final ProfilerSession session) {
        super.attachedToSession(session);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                session.getProfiler().getVMTelemetryManager().reset();
            }
        });
    }
    
    public void detachedFromSession(final ProfilerSession session) {
        super.detachedFromSession(session);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                session.getProfiler().getVMTelemetryManager().reset();
            }
        });
    }
    
}
