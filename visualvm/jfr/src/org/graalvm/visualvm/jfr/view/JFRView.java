/*
 * Copyright (c) 2021, 2022, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.jfr.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import org.graalvm.visualvm.core.datasupport.Positionable;
import org.graalvm.visualvm.core.snapshot.SnapshotView;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.jfr.JFRSnapshot;
import org.graalvm.visualvm.jfr.model.JFRModel;
import org.graalvm.visualvm.jfr.model.JFRModelFactory;
import org.graalvm.visualvm.uisupport.ProfilerTabbedView;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
public class JFRView extends SnapshotView {
    
    private static final Logger LOGGER = Logger.getLogger(JFRView.class.getName());
    
    
    private final List<JFRViewTab> tabViews = new ArrayList<>();
    
    private JFRModel model;
    
    
    JFRView(JFRSnapshot jfrSnapshot, Collection<? extends JFRViewTabProvider> tabProviders) {
        this(jfrSnapshot, DataSourceDescriptorFactory.getDescriptor(jfrSnapshot), tabProviders);
    }
    
    private JFRView(JFRSnapshot jfrSnapshot, DataSourceDescriptor descriptor, Collection<? extends JFRViewTabProvider> tabProviders) {
        super(jfrSnapshot, descriptor.getName(), descriptor.getIcon(), 0);
        
        for (JFRViewTabProvider tabProvider : tabProviders) {
            tabViews.add(tabProvider.createView(jfrSnapshot));
        }
        tabViews.sort(Positionable.COMPARATOR);
    }
    

    @Override
    protected DataViewComponent createComponent() {
        JFRSnapshot jfrSnapshot = (JFRSnapshot)getDataSource();
        DataViewComponent dvc = new DataViewComponent(
                new MasterViewSupport(jfrSnapshot, tabViews).getMasterView(),
                new DataViewComponent.MasterViewConfiguration(true));
        
        return dvc;
    }
    
    
    @Override
    protected void willBeAdded() {
        JFRSnapshot snapshot = (JFRSnapshot)getDataSource();
        model = JFRModelFactory.getJFRModelFor(snapshot);
        
        if (model == null) LOGGER.log(Level.SEVERE, "No JFR model for " + snapshot.getFile()); // NOI18N
        
        for (JFRViewTab tabView : tabViews) {
            tabView.setModel(model);
        }
    }
    
    @Override
    protected void removed() {
        // also called for null model - OOME etc.
        JFRModelFactory.cleanupModel__Workaround(model);
    }
    
    
    private static class MasterViewSupport extends JPanel {
        
        private ProfilerTabbedView views;
        
        MasterViewSupport(JFRSnapshot jfrSnapshot, List<JFRViewTab> tabViews) {
            initComponents(tabViews);
        }
        
        public DataViewComponent.MasterView getMasterView() {
            return new DataViewComponent.MasterView(NbBundle.getMessage(JFRView.class, "LBL_JFR_Snapshot"), null, this);  // NOI18N
        }
        
        
        private void initComponents(List<JFRViewTab> tabViews) {
            setLayout(new BorderLayout());
            setOpaque(false);
            
            views = ProfilerTabbedView.createBottom(true, true, null);
            views.setShowsTabPopup(false);
            views.setFocusMaster(this);
            add(views.getComponent(), BorderLayout.CENTER);
            
            for (JFRViewTab tabView : tabViews) {
                views.addView(tabView.getName(), new ImageIcon(tabView.getImage()), null, tabView.createComponent(), false);
            }
        }
        
        
        public void addNotify() {
            super.addNotify();
            tweakUI();
        }
        
        private void tweakUI() {
            try {
                // Ugly hack to hide the "JFR Snapshot" caption added by the toplevel DisplayArea
                Container DisplayArea_ViewArea = getParent();
                Container DisplayArea = DisplayArea_ViewArea.getParent();
                Component DisplayArea_captionArea = DisplayArea.getComponent(0);
                if (DisplayArea_captionArea instanceof JPanel) DisplayArea_captionArea.setVisible(false);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to tweak UI for JFRView", e); // NOI18N
            }
        }
        
    }
    
}
