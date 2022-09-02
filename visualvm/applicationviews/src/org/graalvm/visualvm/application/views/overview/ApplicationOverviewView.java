/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.application.views.overview;

import javax.swing.ImageIcon;
import org.graalvm.visualvm.core.datasource.DataSource;
import org.graalvm.visualvm.core.ui.DataSourceView;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
class ApplicationOverviewView extends DataSourceView {
    
    private static final String IMAGE_PATH = "org/graalvm/visualvm/application/views/resources/overview.png"; // NOI18N

    private ApplicationOverviewModel model;
    
    private OverviewViewSupport.SnapshotsViewSupport snapshotsView;
    

    ApplicationOverviewView(DataSource dataSource, ApplicationOverviewModel model) {
        super(dataSource, NbBundle.getMessage(ApplicationOverviewView.class, "LBL_Overview"), new ImageIcon(ImageUtilities.loadImage(IMAGE_PATH, true)).getImage(), 10, false);  // NOI18N
        this.model = model;
    }
    
    
    protected void willBeAdded() {
        model.initialize();
    }
    
    protected void removed() {
        snapshotsView.removed();
    }
    
    
    ApplicationOverviewModel getModel() {
        return model;
    }
    
    
    protected DataViewComponent createComponent() {
        DataViewComponent dvc = new DataViewComponent(
                new OverviewViewSupport.MasterViewSupport(model).getMasterView(),
                new DataViewComponent.MasterViewConfiguration(false));
        
        dvc.configureDetailsView(new DataViewComponent.DetailsViewConfiguration(0.25, 0, -1, -1, -1, -1));
        
        snapshotsView = new OverviewViewSupport.SnapshotsViewSupport(model.getSource());
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(NbBundle.getMessage(ApplicationOverviewView.class, "LBL_Saved_data"), true), DataViewComponent.TOP_LEFT);   // NOI18N
        dvc.addDetailsView(snapshotsView.getDetailsView(), DataViewComponent.TOP_LEFT);
        
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(NbBundle.getMessage(ApplicationOverviewView.class, "LBL_Details"), true), DataViewComponent.TOP_RIGHT); // NOI18N
        dvc.addDetailsView(new OverviewViewSupport.JVMArgumentsViewSupport(model.getJvmArgs()).getDetailsView(), DataViewComponent.TOP_RIGHT);
        dvc.addDetailsView(new OverviewViewSupport.SystemPropertiesViewSupport(model.getSystemProperties()).getDetailsView(), DataViewComponent.TOP_RIGHT);
        
        return dvc;
    }
    
}
