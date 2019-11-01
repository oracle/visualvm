/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.jfr.views.monitor;

import javax.swing.ImageIcon;
import org.graalvm.visualvm.core.ui.DataSourceView;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.jfr.JFRSnapshot;
import org.graalvm.visualvm.jfr.model.JFREvent;
import org.graalvm.visualvm.jfr.model.JFREventVisitor;
import org.graalvm.visualvm.jfr.model.JFRModel;
import org.graalvm.visualvm.jfr.model.JFRModelFactory;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
class JFRSnapshotMonitorView extends DataSourceView {
    
    private static final String IMAGE_PATH = "org/graalvm/visualvm/jfr/resources/monitor.png";  // NOI18N
    
    private JFRModel model;
    
    
    public JFRSnapshotMonitorView(JFRSnapshot snapshot) {
        super(snapshot, NbBundle.getMessage(JFRSnapshotMonitorView.class, "LBL_Monitor"), new ImageIcon(ImageUtilities.loadImage(IMAGE_PATH, true)).getImage(), 20, false);
    }
    
    
    @Override
    protected void willBeAdded() {
        JFRSnapshot snapshot = (JFRSnapshot)getDataSource();
        model = JFRModelFactory.getJFRModelFor(snapshot);
    }
    
    @Override
    protected DataViewComponent createComponent() {

        boolean hasEvents = model != null && model.containsEvent(JFRSnapshotMonitorViewProvider.EventChecker.class);
        boolean hasPermGen = hasEvents && model.containsEvent(JFRSnapshotMonitorViewProvider.PermGenChecker.class);
        boolean hasMetaSpace = hasEvents && model.containsEvent(JFRSnapshotMonitorViewProvider.MetaspaceChecker.class);
        
        final MonitorViewSupport.CPUViewSupport cpuView = hasEvents ? new MonitorViewSupport.CPUViewSupport(model) : null;
        final MonitorViewSupport.HeapViewSupport heapView = hasEvents ? new MonitorViewSupport.HeapViewSupport(model) : null;
        final MonitorViewSupport.PermGenViewSupport permgenView = hasPermGen ? new MonitorViewSupport.PermGenViewSupport(model) : null;
        final MonitorViewSupport.MetaspaceViewSupport metaspaceView = hasMetaSpace ? new MonitorViewSupport.MetaspaceViewSupport(model) : null;
        final MonitorViewSupport.ClassesViewSupport classesView = hasEvents ? new MonitorViewSupport.ClassesViewSupport(model) : null;
        final MonitorViewSupport.ThreadsViewSupport threadsView = hasEvents ? new MonitorViewSupport.ThreadsViewSupport(model) : null;
        
        MonitorViewSupport.MasterViewSupport masterView = new MonitorViewSupport.MasterViewSupport(model) {
            @Override
            void firstShown() { initialize(this, cpuView, heapView, permgenView, metaspaceView, classesView, threadsView); }
        };
        
        
        
        DataViewComponent dvc = new DataViewComponent(
                masterView.getMasterView(),
                new DataViewComponent.MasterViewConfiguration(!hasEvents));
        
        if (hasEvents) {
            dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(NbBundle.
                    getMessage(JFRSnapshotMonitorView.class, "LBL_Cpu"), true), DataViewComponent.TOP_LEFT);  // NOI18N
            dvc.addDetailsView(cpuView.getDetailsView(), DataViewComponent.TOP_LEFT);

            dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(NbBundle.
                    getMessage(JFRSnapshotMonitorView.class, "LBL_Memory"), true), DataViewComponent.TOP_RIGHT);  // NOI18N
            dvc.addDetailsView(heapView.getDetailsView(), DataViewComponent.TOP_RIGHT);
            if (metaspaceView != null) dvc.addDetailsView(metaspaceView.getDetailsView(), DataViewComponent.TOP_RIGHT);
            else if (permgenView != null) dvc.addDetailsView(permgenView.getDetailsView(), DataViewComponent.TOP_RIGHT);

            dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(NbBundle.
                    getMessage(JFRSnapshotMonitorView.class, "LBL_Classes"), true), DataViewComponent.BOTTOM_LEFT);    // NOI18N
            dvc.addDetailsView(classesView.getDetailsView(), DataViewComponent.BOTTOM_LEFT);

            dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(NbBundle.
                    getMessage(JFRSnapshotMonitorView.class, "LBL_Threads"), true), DataViewComponent.BOTTOM_RIGHT);   // NOI18N
            dvc.addDetailsView(threadsView.getDetailsView(), DataViewComponent.BOTTOM_RIGHT);
        }
        
        return dvc;
    }
    
    
    private void initialize(final MonitorViewSupport.MasterViewSupport masterView,
                            final MonitorViewSupport.CPUViewSupport cpuView,
                            final MonitorViewSupport.HeapViewSupport heapView,
                            final MonitorViewSupport.PermGenViewSupport permgenView,
                            final MonitorViewSupport.MetaspaceViewSupport metaspaceView,
                            final MonitorViewSupport.ClassesViewSupport classesView,
                            final MonitorViewSupport.ThreadsViewSupport threadsView) {
        final JFREventVisitor doneHandler = new JFREventVisitor() {
            @Override
            public boolean visit(String typeName, JFREvent event) { return true; }
            @Override
            public void done() { masterView.dataComputed(); }
        };
        new RequestProcessor("JFR Monitor Initializer").post(new Runnable() { // NOI18N
            public void run() {
                if (permgenView == null && metaspaceView == null) {
                    model.visitEvents(cpuView, heapView, classesView, threadsView, doneHandler);
                } else {
                    model.visitEvents(cpuView, heapView, metaspaceView != null ? metaspaceView : permgenView, classesView, threadsView, doneHandler);
                }
            }
        });
    }
    
}
