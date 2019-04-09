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
package org.graalvm.visualvm.jfr.views.locks;

import javax.swing.SwingUtilities;
import org.graalvm.visualvm.core.ui.DataSourceView;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.jfr.JFRSnapshot;
import org.graalvm.visualvm.jfr.model.JFRModel;
import org.graalvm.visualvm.jfr.model.JFRModelFactory;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.ProfilerIcons;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
final class JFRSnapshotLocksView extends DataSourceView {
    
    private JFRModel model;
    
    
    JFRSnapshotLocksView(JFRSnapshot jfrSnapshot) {
        super(jfrSnapshot, "Locks", Icons.getImage(ProfilerIcons.WINDOW_LOCKS), 32, false);

    }
    
    
    @Override
    protected void willBeAdded() {
        JFRSnapshot snapshot = (JFRSnapshot)getDataSource();
        model = JFRModelFactory.getJFRModelFor(snapshot);
    }

    
    private DataViewComponent dvc;
    private LocksViewSupport.MasterViewSupport masterView;
    private LocksViewSupport.DataViewSupport dataView;
    
    
    protected DataViewComponent createComponent() {
        masterView = new LocksViewSupport.MasterViewSupport() {
            @Override
            void firstShown() {
                changeAggregation(0, LocksViewSupport.Aggregation.CLASS, LocksViewSupport.Aggregation.NONE);
            }
            @Override
            void changeAggregation(int mode, LocksViewSupport.Aggregation primary, LocksViewSupport.Aggregation secondary) {
                JFRSnapshotLocksView.this.setAggregation(mode, primary, secondary);
            }
        };
        
        dvc = new DataViewComponent(
                masterView.getMasterView(),
                new DataViewComponent.MasterViewConfiguration(false));
        
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("Data", false), DataViewComponent.TOP_LEFT);
        
        dataView = new LocksViewSupport.DataViewSupport();
        dvc.addDetailsView(dataView.getDetailsView(), DataViewComponent.TOP_LEFT);

        return dvc;
    }
    
    
    private void setAggregation(int mode, LocksViewSupport.Aggregation primary, LocksViewSupport.Aggregation secondary) {
        masterView.showProgress();
        dataView.setData(new LocksNode.Root(), false);
        
        new RequestProcessor("JFR Locks Initializer").post(new Runnable() { // NOI18N
            public void run() {
                final LocksNode.Root root = new LocksNode.Root(mode, primary, secondary);
                model.visitEvents(root);
                
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        dataView.setData(root, !LocksViewSupport.Aggregation.NONE.equals(secondary));
                        masterView.hideProgress();
                    }
                });
            }
        });
    }
    
}
