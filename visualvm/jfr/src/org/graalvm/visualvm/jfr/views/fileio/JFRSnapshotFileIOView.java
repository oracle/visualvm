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
package org.graalvm.visualvm.jfr.views.fileio;

import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.jfr.JFRSnapshot;
import org.graalvm.visualvm.jfr.model.JFRModel;
import org.graalvm.visualvm.jfr.view.JFRViewTab;
import org.openide.util.ImageUtilities;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
final class JFRSnapshotFileIOView extends JFRViewTab {
    
    private static final String IMAGE_PATH = "org/graalvm/visualvm/jfr/resources/fileio.png"; // NOI18N
    
    
    JFRSnapshotFileIOView(JFRSnapshot jfrSnapshot) {
        super(jfrSnapshot, "File IO", new ImageIcon(ImageUtilities.loadImage(IMAGE_PATH, true)).getImage(), 30);

    }
    
    
    private DataViewComponent dvc;
    private FileIOViewSupport.MasterViewSupport masterView;
    private FileIOViewSupport.DataViewSupport dataView;
    
    
    protected DataViewComponent createComponent() {
        JFRModel model = getModel();
        
        masterView = new FileIOViewSupport.MasterViewSupport(model) {
            @Override
            void firstShown() {
                changeAggregation(FileIOViewSupport.Aggregation.FILE, FileIOViewSupport.Aggregation.NONE);
            }
            @Override
            void changeAggregation(FileIOViewSupport.Aggregation primary, FileIOViewSupport.Aggregation secondary) {
                JFRSnapshotFileIOView.this.setAggregation(primary, secondary);
            }
        };
        
        boolean hasEvents = model != null && model.containsEvent(JFRSnapshotFileIOViewProvider.EventChecker.class);
        
        dvc = new DataViewComponent(
                masterView.getMasterView(),
                new DataViewComponent.MasterViewConfiguration(!hasEvents));
        
        if (hasEvents) {
            dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("Data", false), DataViewComponent.TOP_LEFT);

            dataView = new FileIOViewSupport.DataViewSupport();
            dvc.addDetailsView(dataView.getDetailsView(), DataViewComponent.TOP_LEFT);
        }

        return dvc;
    }
    
    
    private void setAggregation(final FileIOViewSupport.Aggregation primary, final FileIOViewSupport.Aggregation secondary) {
        masterView.showProgress();
        dataView.setData(new FileIONode.Root(), false);
        
        new RequestProcessor("JFR FileIO Initializer").post(new Runnable() { // NOI18N
            public void run() {
                final FileIONode.Root root = new FileIONode.Root(primary, secondary);
                getModel().visitEvents(root);
                
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        dataView.setData(root, !FileIOViewSupport.Aggregation.NONE.equals(secondary));
                        masterView.hideProgress();
                    }
                });
            }
        });
    }
    
}
