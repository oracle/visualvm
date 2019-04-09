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
package org.graalvm.visualvm.jfr.views.threads;

import javax.swing.ImageIcon;
import org.graalvm.visualvm.core.ui.DataSourceView;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.jfr.JFRSnapshot;
import org.graalvm.visualvm.jfr.model.JFRModel;
import org.graalvm.visualvm.jfr.model.JFRModelFactory;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
class JFRSnapshotThreadsView extends DataSourceView {
    
    private static final String IMAGE_PATH = "org/graalvm/visualvm/jfr/resources/threads.png";  // NOI18N
    
    private JFRModel model;
    
    
    JFRSnapshotThreadsView(JFRSnapshot dataSource) {
        super(dataSource, NbBundle.getMessage(JFRSnapshotThreadsView.class, "LBL_Threads"), new ImageIcon(ImageUtilities.loadImage(IMAGE_PATH, true)).getImage(), 30, false);   // NOI18N
    }

    
    @Override
    protected void willBeAdded() {
        JFRSnapshot snapshot = (JFRSnapshot)getDataSource();
        model = JFRModelFactory.getJFRModelFor(snapshot);
    }
    
    @Override
    protected DataViewComponent createComponent() {
        ThreadsViewSupport.TimelineViewSupport timelineView = new ThreadsViewSupport.TimelineViewSupport() {
            @Override
            long lastTimestamp() { return model.getLastEventTime(); }
        };
        
        ThreadsViewSupport.MasterViewSupport masterView = new ThreadsViewSupport.MasterViewSupport((JFRSnapshot)getDataSource()) {
            @Override
            void firstShown() { initialize(this, timelineView); }
        };
        DataViewComponent dvc = new DataViewComponent(masterView.getMasterView(), new DataViewComponent.MasterViewConfiguration(false));

        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(NbBundle.getMessage(JFRSnapshotThreadsView.class, "LBL_Threads_visualization"), true), DataViewComponent.TOP_LEFT); // NOI18N
        dvc.addDetailsView(timelineView.getDetailsView(), DataViewComponent.TOP_LEFT);

        return dvc;
    }
    
    
    private void initialize(ThreadsViewSupport.MasterViewSupport masterView, ThreadsViewSupport.TimelineViewSupport timelineView) {
        new RequestProcessor("JFR Threads Initializer").post(new Runnable() { // NOI18N
            public void run() { model.visitEvents(timelineView); masterView.initialized(); }
        });
    }
    
}
