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
package org.graalvm.visualvm.jfr.views.socketio;

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
final class JFRSnapshotSocketIOView extends DataSourceView {
    
    private static final String IMAGE_PATH = "org/graalvm/visualvm/jfr/resources/socketio.png"; // NOI18N
    
    private JFRModel model;
    
    
    JFRSnapshotSocketIOView(JFRSnapshot jfrSnapshot) {
        super(jfrSnapshot, "Socket IO", new ImageIcon(ImageUtilities.loadImage(IMAGE_PATH, true)).getImage(), 34, false);

    }
    
    
    @Override
    protected void willBeAdded() {
        JFRSnapshot snapshot = (JFRSnapshot)getDataSource();
        model = JFRModelFactory.getJFRModelFor(snapshot);
    }

    
    private DataViewComponent dvc;
    private SocketIOViewSupport.MasterViewSupport masterView;
    private SocketIOViewSupport.DataViewSupport dataView;
    
    
    protected DataViewComponent createComponent() {
        masterView = new SocketIOViewSupport.MasterViewSupport(model) {
            @Override
            void firstShown() {
                changeAggregation(SocketIOViewSupport.Aggregation.ADDRESS_PORT, SocketIOViewSupport.Aggregation.NONE);
            }
            @Override
            void changeAggregation(SocketIOViewSupport.Aggregation primary, SocketIOViewSupport.Aggregation secondary) {
                JFRSnapshotSocketIOView.this.setAggregation(primary, secondary);
            }
        };
        
        boolean hasEvents = model != null && model.containsEvent(JFRSnapshotSocketIOViewProvider.EventChecker.class);
        
        dvc = new DataViewComponent(
                masterView.getMasterView(),
                new DataViewComponent.MasterViewConfiguration(!hasEvents));
        
        if (hasEvents) {
            dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("Data", false), DataViewComponent.TOP_LEFT);

            dataView = new SocketIOViewSupport.DataViewSupport();
            dvc.addDetailsView(dataView.getDetailsView(), DataViewComponent.TOP_LEFT);
        }

        return dvc;
    }
    
    
    private void setAggregation(SocketIOViewSupport.Aggregation primary, SocketIOViewSupport.Aggregation secondary) {
        masterView.showProgress();
        dataView.setData(new SocketIONode.Root(), false);
        
        new RequestProcessor("JFR SocketIO Initializer").post(new Runnable() { // NOI18N
            public void run() {
                final SocketIONode.Root root = new SocketIONode.Root(primary, secondary);
                model.visitEvents(root);
                
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        dataView.setData(root, !SocketIOViewSupport.Aggregation.NONE.equals(secondary));
                        masterView.hideProgress();
                    }
                });
            }
        });
    }
    
}
