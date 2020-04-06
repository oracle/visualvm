/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.jfr.views.browser;

import java.util.List;
import org.graalvm.visualvm.core.ui.DataSourceView;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.jfr.JFRSnapshot;
import org.graalvm.visualvm.jfr.model.JFRDataDescriptor;
import org.graalvm.visualvm.jfr.model.JFREvent;
import org.graalvm.visualvm.jfr.model.JFREventTypeVisitor;
import org.graalvm.visualvm.jfr.model.JFREventVisitor;
import org.graalvm.visualvm.jfr.model.JFRModel;
import org.graalvm.visualvm.jfr.model.JFRModelFactory;
import org.graalvm.visualvm.lib.profiler.api.icons.GeneralIcons;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
class JFRSnapshotBrowserView extends DataSourceView {
    
    private JFRModel model;
    
    
    JFRSnapshotBrowserView(JFRSnapshot snapshot) {
        super(snapshot, "Browser", Icons.getImage(GeneralIcons.FIND), 38, false);    // NOI18N
    }
    
    
    @Override
    protected void willBeAdded() {
        JFRSnapshot snapshot = (JFRSnapshot)getDataSource();
        model = JFRModelFactory.getJFRModelFor(snapshot);
    }

    @Override
    protected DataViewComponent createComponent() {
        if (model == null) {
            BrowserViewSupport.MasterViewSupport masterView = new BrowserViewSupport.MasterViewSupport(model) {
                @Override
                void firstShown() {}
                @Override
                void reloadEvents() {
                }
            };

            return new DataViewComponent(
                    masterView.getMasterView(),
                    new DataViewComponent.MasterViewConfiguration(true));
        } else {
            final BrowserViewSupport.StackTraceViewSupport stackTracePane = new BrowserViewSupport.StackTraceViewSupport() {
                @Override
                JFREvent getEvent(long id) {
                    return model.getEvent(id);
                }
            };
            
            final BrowserViewSupport.EventsTableViewSupport eventsTable = new BrowserViewSupport.EventsTableViewSupport() {
                @Override
                void idSelected(long id) {
                    stackTracePane.idSelected(id);
                }
            };

            final BrowserViewSupport.EventsTreeViewSupport eventsTree = new BrowserViewSupport.EventsTreeViewSupport(model.getEventsCount()) {
                @Override
                void reloadEvents(JFREventVisitor visitor) {
                    initialize(null, visitor);
                }
                @Override
                void eventsSelected(String eventType, long eventsCount, List<JFRDataDescriptor> dataDescriptors) {
                    initialize(null, eventsTable.getVisitor(eventType, eventsCount, dataDescriptors));
                }
            };

            BrowserViewSupport.MasterViewSupport masterView = new BrowserViewSupport.MasterViewSupport(model) {
                @Override
                void firstShown() {
                    initialize(eventsTree, this, eventsTree.getVisitor());
                }
                @Override
                void reloadEvents() {
                }
            };

            DataViewComponent dvc = new DataViewComponent(
                    masterView.getMasterView(),
                    new DataViewComponent.MasterViewConfiguration(false));

            dvc.configureDetailsView(new DataViewComponent.DetailsViewConfiguration(0.35, 0, -1, -1, 0.65, 1));

            dvc.addDetailsView(eventsTree.getDetailsView(), DataViewComponent.TOP_LEFT);
            dvc.addDetailsView(eventsTable.getDetailsView(), DataViewComponent.TOP_RIGHT);
            dvc.addDetailsView(stackTracePane.getDetailsView(), DataViewComponent.BOTTOM_RIGHT);
            
            dvc.hideDetailsArea(DataViewComponent.BOTTOM_RIGHT);

            return dvc;
        }
    }
    
    
    private void initialize(final JFREventTypeVisitor typeVisitor, final JFREventVisitor... visitors) {
        new RequestProcessor("JFR Events Browser Initializer").post(new Runnable() { // NOI18N
            public void run() {
                if (typeVisitor != null) model.visitEventTypes(typeVisitor);
                model.visitEvents(visitors);
            }
        });
    }
    
}
