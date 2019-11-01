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
package org.graalvm.visualvm.jfr.views.recording;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.core.datasupport.Positionable;
import org.graalvm.visualvm.core.ui.DataSourceView;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.jfr.JFRSnapshot;
import org.graalvm.visualvm.jfr.model.JFREventVisitor;
import org.graalvm.visualvm.jfr.model.JFRModel;
import org.graalvm.visualvm.jfr.model.JFRModelFactory;
import org.graalvm.visualvm.jfr.utils.ValuesConverter;
import org.openide.util.ImageUtilities;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
class JFRSnapshotRecordingView extends DataSourceView {
    
    private static final String IMAGE_PATH = "org/graalvm/visualvm/jfr/resources/jfrSnapshot.png";  // NOI18N
    
    private JFRModel model;
    
    
    JFRSnapshotRecordingView(JFRSnapshot dataSource) {
        super(dataSource, "Recording", new ImageIcon(ImageUtilities.loadImage(IMAGE_PATH, true)).getImage(), Positionable.POSITION_LAST, false);   // NOI18N
    }

    
    @Override
    protected void willBeAdded() {
        JFRSnapshot snapshot = (JFRSnapshot)getDataSource();
        model = JFRModelFactory.getJFRModelFor(snapshot);
    }
    
    @Override
    protected DataViewComponent createComponent() {
        if (model == null) {
            RecordingViewSupport.MasterViewSupport masterView = new RecordingViewSupport.MasterViewSupport((JFRSnapshot)getDataSource(), model) {
                @Override
                void firstShown() {}
            };
            return new DataViewComponent(masterView.getMasterView(), new DataViewComponent.MasterViewConfiguration(true));
        } else {
            final RecordingViewSupport.SettingsSupport settingsView = new RecordingViewSupport.SettingsSupport(model);
            final RecordingViewSupport.RecordingsSupport recordingsView = new RecordingViewSupport.RecordingsSupport(model);

            RecordingViewSupport.MasterViewSupport masterView = new RecordingViewSupport.MasterViewSupport((JFRSnapshot)getDataSource(), model) {
                @Override
                void firstShown() { initialize(settingsView, this, recordingsView); }
            };
            DataViewComponent dvc = new DataViewComponent(masterView.getMasterView(), new DataViewComponent.MasterViewConfiguration(false));
            dvc.configureDetailsView(new DataViewComponent.DetailsViewConfiguration(-1, -1, -1, -1, 0.75, 0.75));

            dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("Settings", true), DataViewComponent.TOP_LEFT);
            dvc.addDetailsView(settingsView.getDetailsView(), DataViewComponent.TOP_LEFT);

            dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("Concurrent recordings", true), DataViewComponent.BOTTOM_LEFT);
            dvc.addDetailsView(recordingsView.getDetailsView(), DataViewComponent.BOTTOM_LEFT);

            return dvc;
        }
    }
    
    
    private void initialize(final RecordingViewSupport.SettingsSupport settingsView, final JFREventVisitor... visitors) {
        new RequestProcessor("JFR Recording Initializer").post(new Runnable() { // NOI18N
            public void run() {
                final RecordingNode.Root settingsRoot = new RecordingNode.Root() {
                    @Override void visitEventTypes() { model.visitEventTypes(this); }
                    @Override long toRelativeNanos(Instant time) { return ValuesConverter.instantToRelativeNanos(time, model); }
                };
                
                List<JFREventVisitor> allVisitors = new ArrayList(Arrays.asList(visitors));
                allVisitors.add(settingsRoot);
                model.visitEvents(allVisitors.toArray(new JFREventVisitor[0]));
                
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        settingsView.setData(settingsRoot.getChildCount() > 0 ? settingsRoot : new RecordingNode.Root("<no settings>") {});
                    }
                });
            }
        });
    }
    
}
