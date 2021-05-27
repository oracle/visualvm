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

package org.graalvm.visualvm.jfr.views.sampler;

import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import javax.swing.ImageIcon;
import org.graalvm.visualvm.jfr.JFRSnapshot;
import org.graalvm.visualvm.jfr.model.JFREventVisitor;
import org.graalvm.visualvm.jfr.model.JFRModel;
import org.graalvm.visualvm.jfr.view.JFRViewTab;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
final class JFRSnapshotSamplerView extends JFRViewTab {
    
    private static final String IMAGE_PATH = "org/graalvm/visualvm/jfr/resources/sampler.png"; // NOI18N
    
    
    JFRSnapshotSamplerView(JFRSnapshot jfrSnapshot) {
        super(jfrSnapshot, NbBundle.getMessage(JFRSnapshotSamplerView.class, "LBL_Sampler"), // NOI18N
              new ImageIcon(ImageUtilities.loadImage(IMAGE_PATH, true)).getImage(), 70);

    }
    
    
    private DataViewComponent dvc;
    private SamplerViewSupport.MasterViewSupport masterView;
    private DataViewComponent.DetailsView[] currentDetails;
    
    protected DataViewComponent createComponent() {
        final JFRModel model = getModel();
        
        masterView = new SamplerViewSupport.MasterViewSupport(model) {
            @Override void showCPU() { JFRSnapshotSamplerView.this.showCPU(model); }
            @Override void showMemory() { JFRSnapshotSamplerView.this.showMemory(model); }
        };
        
        dvc = new DataViewComponent(
                masterView.getMasterView(),
                new DataViewComponent.MasterViewConfiguration(model == null));
        
        if (model != null) {
            dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(
                        NbBundle.getMessage(JFRSnapshotSamplerView.class, "LBL_Summary"), // NOI18N
                        false), DataViewComponent.TOP_LEFT);
            DataViewComponent.DetailsView summaryView = new SamplerViewSupport.SummaryViewSupport(model).getDetailsView();
            dvc.addDetailsView(summaryView, DataViewComponent.TOP_LEFT);
            currentDetails = new DataViewComponent.DetailsView[] { summaryView };
        }

        return dvc;
    }
    
    
    private void showCPU(JFRModel model) {
        for (DataViewComponent.DetailsView detail : currentDetails)
            dvc.removeDetailsView(detail);
        
        CPUSamplerViewSupport.CPUViewSupport cpuView = new CPUSamplerViewSupport.CPUViewSupport(model);
        DataViewComponent.DetailsView cpuViewW = cpuView.getDetailsView();
        dvc.addDetailsView(cpuViewW, DataViewComponent.TOP_LEFT);
        
        CPUSamplerViewSupport.ThreadsCPUViewSupport threadCpuView = new CPUSamplerViewSupport.ThreadsCPUViewSupport(model);
        DataViewComponent.DetailsView threadsCpuViewW = threadCpuView.getDetailsView();
        dvc.addDetailsView(threadsCpuViewW, DataViewComponent.TOP_LEFT);
        
        currentDetails = new DataViewComponent.DetailsView[] { cpuViewW, threadsCpuViewW };
        
        initialize(cpuView, threadCpuView);
    }
    
    private void showMemory(JFRModel model) {
        for (DataViewComponent.DetailsView detail : currentDetails)
            dvc.removeDetailsView(detail);
        
        MemorySamplerViewSupport.HeapViewSupport heapView = new MemorySamplerViewSupport.HeapViewSupport(model);
        DataViewComponent.DetailsView heapViewW = heapView.getDetailsView();
        dvc.addDetailsView(heapViewW, DataViewComponent.TOP_LEFT);
        
        MemorySamplerViewSupport.ThreadsMemoryViewSupport threadsMemoryView = new MemorySamplerViewSupport.ThreadsMemoryViewSupport(model);
        DataViewComponent.DetailsView threadsMemoryViewW = threadsMemoryView.getDetailsView();
        dvc.addDetailsView(threadsMemoryViewW, DataViewComponent.TOP_LEFT);
        
        currentDetails = new DataViewComponent.DetailsView[] { heapViewW, threadsMemoryViewW };
        
        initialize(heapView, threadsMemoryView);
    }
    
    
    private void initialize(JFREventVisitor... visitors) {
        new RequestProcessor("JFR Sampler Initializer").post(new Runnable() { // NOI18N
            public void run() {
                masterView.showProgress();
                getModel().visitEvents(visitors);
                masterView.hideProgress();
            }
        });
    }

}
