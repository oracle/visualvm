/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.jfr.views.gc;

import javax.swing.SwingUtilities;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.jfr.JFRSnapshot;
import org.graalvm.visualvm.jfr.model.JFREventVisitor;
import org.graalvm.visualvm.jfr.model.JFRModel;
import org.graalvm.visualvm.jfr.view.JFRViewTab;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.ProfilerIcons;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
final class JFRSnapshotGcView extends JFRViewTab {
    
    JFRSnapshotGcView(JFRSnapshot jfrSnapshot) {
        super(jfrSnapshot, "GC", Icons.getImage(ProfilerIcons.RUN_GC), 60);

    }
    
    
    private GcViewSupport.MasterViewSupport masterView;
    private GcViewSupport.DataViewSupport dataView;
    
    
    protected DataViewComponent createComponent() {
        JFRModel model = getModel();
        
        boolean hasEvents = model != null && model.containsEvent(JFRSnapshotGcViewProvider.EventChecker.class);
        
        if (!hasEvents) {
            masterView = new GcViewSupport.MasterViewSupport(model) {
                @Override void firstShown() {}
                @Override void changeAggregation(GcViewSupport.Aggregation primary, GcViewSupport.Aggregation secondary) {}
            };
            return new DataViewComponent(masterView.getMasterView(), new DataViewComponent.MasterViewConfiguration(true));
        } else {
            final GcViewSupport.GcConfigurationSupport gcConfigurationView = new GcViewSupport.GcConfigurationSupport();
            final GcViewSupport.GcHeapConfigurationSupport gcHeapConfigurationView = new GcViewSupport.GcHeapConfigurationSupport();
            final GcViewSupport.GcYoungGenConfigurationSupport gcYoungGenConfigurationView = new GcViewSupport.GcYoungGenConfigurationSupport();
            final GcViewSupport.GcSurvivorConfigurationSupport gcSurvivorConfigurationView = new GcViewSupport.GcSurvivorConfigurationSupport();
            final GcViewSupport.GcTlabConfigurationSupport gcTlabConfigurationView = new GcViewSupport.GcTlabConfigurationSupport();
        
            masterView = new GcViewSupport.MasterViewSupport(model) {
                @Override
                void firstShown() {
                    changeAggregation(GcViewSupport.Aggregation.NONE, GcViewSupport.Aggregation.NONE);
                    initialize(gcConfigurationView, gcHeapConfigurationView, gcYoungGenConfigurationView, gcSurvivorConfigurationView, gcTlabConfigurationView);
                }
                @Override
                void changeAggregation(GcViewSupport.Aggregation primary, GcViewSupport.Aggregation secondary) {
                    JFRSnapshotGcView.this.setAggregation(primary, secondary);
                }
            };
            
            DataViewComponent dvc = new DataViewComponent(masterView.getMasterView(), new DataViewComponent.MasterViewConfiguration(false));
            
            dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("Data", false), DataViewComponent.TOP_LEFT);

            dataView = new GcViewSupport.DataViewSupport();
            dvc.addDetailsView(dataView.getDetailsView(), DataViewComponent.TOP_LEFT);
            
            dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("Configuration", true), DataViewComponent.BOTTOM_LEFT);
            dvc.addDetailsView(gcConfigurationView.getDetailsView(), DataViewComponent.BOTTOM_LEFT);
            dvc.addDetailsView(gcHeapConfigurationView.getDetailsView(), DataViewComponent.BOTTOM_LEFT);
            dvc.addDetailsView(gcYoungGenConfigurationView.getDetailsView(), DataViewComponent.BOTTOM_LEFT);
            dvc.addDetailsView(gcSurvivorConfigurationView.getDetailsView(), DataViewComponent.BOTTOM_LEFT);
            dvc.addDetailsView(gcTlabConfigurationView.getDetailsView(), DataViewComponent.BOTTOM_LEFT);
            
            return dvc;
        }
    }
    
    
    private void setAggregation(GcViewSupport.Aggregation primary, GcViewSupport.Aggregation secondary) {
        masterView.showProgress();
        dataView.setData(new GcNode.Root(), false, false);
        
        new RequestProcessor("JFR GC Initializer").post(new Runnable() { // NOI18N
            public void run() {
                final GcNode.Root root = new GcNode.Root(primary, secondary);
                getModel().visitEvents(root);
                
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        if (root.getNChildren() == 0) root.addChild(GcNode.Label.createNoData(root));
                        dataView.setData(root, !GcViewSupport.Aggregation.NONE.equals(primary), GcViewSupport.Aggregation.PHASE.equals(secondary));
                        masterView.hideProgress();
                    }
                });
            }
        });
    }
    
    private void initialize(final JFREventVisitor... visitors) {
        new RequestProcessor("JFR GC Initializer").post(new Runnable() { // NOI18N
            public void run() { getModel().visitEvents(visitors); }
        });
    }
    
}
