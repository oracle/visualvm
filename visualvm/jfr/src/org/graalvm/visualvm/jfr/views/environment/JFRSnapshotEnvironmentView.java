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
package org.graalvm.visualvm.jfr.views.environment;

import javax.swing.ImageIcon;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.jfr.JFRSnapshot;
import org.graalvm.visualvm.jfr.model.JFREventVisitor;
import org.graalvm.visualvm.jfr.model.JFRModel;
import org.graalvm.visualvm.jfr.view.JFRViewTab;
import org.openide.util.ImageUtilities;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
final class JFRSnapshotEnvironmentView extends JFRViewTab {
    
    private static final String IMAGE_PATH = "org/graalvm/visualvm/jfr/resources/environment.png";  // NOI18N
    
    
    JFRSnapshotEnvironmentView(JFRSnapshot dataSource) {
        super(dataSource, "Environment", new ImageIcon(ImageUtilities.loadImage(IMAGE_PATH, true)).getImage(), 90);   // NOI18N
    }

    
    @Override
    protected DataViewComponent createComponent() {
        JFRModel model = getModel();
        
        boolean hasEvents = model != null && model.containsEvent(JFRSnapshotEnvironmentViewProvider.EventChecker.class);
        
        if (!hasEvents) {
            EnvironmentViewSupport.MasterViewSupport masterView = new EnvironmentViewSupport.MasterViewSupport(model) {
                @Override
                void firstShown() {}
            };
            return new DataViewComponent(masterView.getMasterView(), new DataViewComponent.MasterViewConfiguration(true));
        } else {
            EnvironmentViewSupport.CPUUtilizationSupport cpuUtilizationView = new EnvironmentViewSupport.CPUUtilizationSupport(model);
            EnvironmentViewSupport.NetworkUtilizationSupport networkUtilizationView = new EnvironmentViewSupport.NetworkUtilizationSupport(model);
            EnvironmentViewSupport.MemoryUsageSupport memoryUsageView = new EnvironmentViewSupport.MemoryUsageSupport(model);
            EnvironmentViewSupport.CPUDetailsSupport cpuDetailsView = new EnvironmentViewSupport.CPUDetailsSupport();
            EnvironmentViewSupport.OSDetailsSupport osDetailsView = new EnvironmentViewSupport.OSDetailsSupport();
            EnvironmentViewSupport.NetworkDetailsSupport networkDetailsView = new EnvironmentViewSupport.NetworkDetailsSupport();
            EnvironmentViewSupport.EnvVarSupport envVarView = new EnvironmentViewSupport.EnvVarSupport();
            EnvironmentViewSupport.ProcessesSupport processesView = new EnvironmentViewSupport.ProcessesSupport();

            EnvironmentViewSupport.MasterViewSupport masterView = new EnvironmentViewSupport.MasterViewSupport(model) {
                @Override
                void firstShown() { initialize(this, cpuUtilizationView, networkUtilizationView, memoryUsageView, cpuDetailsView, osDetailsView, networkDetailsView, envVarView, processesView); }
            };
            DataViewComponent dvc = new DataViewComponent(masterView.getMasterView(), new DataViewComponent.MasterViewConfiguration(false));

            dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("CPU & Network", true), DataViewComponent.TOP_LEFT);
            dvc.addDetailsView(cpuUtilizationView.getDetailsView(), DataViewComponent.TOP_LEFT);
            dvc.addDetailsView(networkUtilizationView.getDetailsView(), DataViewComponent.TOP_LEFT);

            dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("Memory", true), DataViewComponent.TOP_RIGHT);
            dvc.addDetailsView(memoryUsageView.getDetailsView(), DataViewComponent.TOP_RIGHT);

            dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("Details", true), DataViewComponent.BOTTOM_LEFT);
            dvc.addDetailsView(cpuDetailsView.getDetailsView(), DataViewComponent.BOTTOM_LEFT);
            dvc.addDetailsView(osDetailsView.getDetailsView(), DataViewComponent.BOTTOM_LEFT);
            dvc.addDetailsView(networkDetailsView.getDetailsView(), DataViewComponent.BOTTOM_LEFT);
            dvc.addDetailsView(envVarView.getDetailsView(), DataViewComponent.BOTTOM_LEFT);
            dvc.addDetailsView(processesView.getDetailsView(), DataViewComponent.BOTTOM_LEFT);

            return dvc;
        }
    }
    
    
    private void initialize(final JFREventVisitor... visitors) {
        new RequestProcessor("JFR Environment Initializer").post(new Runnable() { // NOI18N
            public void run() { getModel().visitEvents(visitors); }
        });
    }
    
}
