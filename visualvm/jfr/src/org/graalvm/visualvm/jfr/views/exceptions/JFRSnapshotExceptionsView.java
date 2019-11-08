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
package org.graalvm.visualvm.jfr.views.exceptions;

import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.core.ui.DataSourceView;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.jfr.JFRSnapshot;
import org.graalvm.visualvm.jfr.model.JFRModel;
import org.graalvm.visualvm.jfr.model.JFRModelFactory;
import org.openide.util.ImageUtilities;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
final class JFRSnapshotExceptionsView extends DataSourceView {
    
    private static final String IMAGE_PATH = "org/graalvm/visualvm/jfr/resources/exception.png"; // NOI18N
    
    private JFRModel model;
    
    
    JFRSnapshotExceptionsView(JFRSnapshot jfrSnapshot) {
        super(jfrSnapshot, "Exceptions", new ImageIcon(ImageUtilities.loadImage(IMAGE_PATH, true)).getImage(), 35, false);

    }
    
    
    @Override
    protected void willBeAdded() {
        JFRSnapshot snapshot = (JFRSnapshot)getDataSource();
        model = JFRModelFactory.getJFRModelFor(snapshot);
    }

    
    private DataViewComponent dvc;
    private ExceptionsViewSupport.MasterViewSupport masterView;
    private ExceptionsViewSupport.DataViewSupport dataView;
    
    
    protected DataViewComponent createComponent() {
        masterView = new ExceptionsViewSupport.MasterViewSupport(model) {
            @Override
            void firstShown() {
                changeAggregation(0, ExceptionsViewSupport.Aggregation.CLASS, ExceptionsViewSupport.Aggregation.NONE);
            }
            @Override
            void changeAggregation(int mode, ExceptionsViewSupport.Aggregation primary, ExceptionsViewSupport.Aggregation secondary) {
                JFRSnapshotExceptionsView.this.setAggregation(mode, primary, secondary);
            }
        };
        
        boolean hasEvents = model != null && model.containsEvent(JFRSnapshotExceptionsViewProvider.EventChecker.class);
        
        dvc = new DataViewComponent(
                masterView.getMasterView(),
                new DataViewComponent.MasterViewConfiguration(!hasEvents));
        
        if (hasEvents) {
            dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("Data", false), DataViewComponent.TOP_LEFT);

            dataView = new ExceptionsViewSupport.DataViewSupport();
            dvc.addDetailsView(dataView.getDetailsView(), DataViewComponent.TOP_LEFT);
        }

        return dvc;
    }
    
    
    private void setAggregation(final int mode, final ExceptionsViewSupport.Aggregation primary, final ExceptionsViewSupport.Aggregation secondary) {
        masterView.showProgress();
        dataView.setData(new ExceptionsNode.Root(), false);
        
        new RequestProcessor("JFR Exceptions Initializer").post(new Runnable() { // NOI18N
            public void run() {
                final ExceptionsNode.Root root = new ExceptionsNode.Root(mode, primary, secondary);
                model.visitEvents(root);
                
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        if (root.getNChildren() == 0) root.addChild(ExceptionsNode.Label.createNoData(root));
                        dataView.setData(root, !ExceptionsViewSupport.Aggregation.NONE.equals(secondary));
                        masterView.hideProgress();
                    }
                });
            }
        });
    }
    
}
