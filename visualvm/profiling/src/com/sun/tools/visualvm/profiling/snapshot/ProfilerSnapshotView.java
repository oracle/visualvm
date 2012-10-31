/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.visualvm.profiling.snapshot;

import com.sun.tools.visualvm.application.snapshot.ApplicationSnapshot;
import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import com.sun.tools.visualvm.core.datasupport.Positionable;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
final class ProfilerSnapshotView extends DataSourceView {
    
    private ProfilerSnapshot loadedSnapshot = null;

    public ProfilerSnapshotView(ProfilerSnapshot snapshot) {
        this(snapshot, DataSourceDescriptorFactory.getDescriptor(snapshot));
    }
    
    private ProfilerSnapshotView(ProfilerSnapshot snapshot, DataSourceDescriptor descriptor) {
        super(snapshot, descriptor.getName(), descriptor.getIcon(),
              Positionable.POSITION_AT_THE_END, isClosableView(snapshot));
        loadedSnapshot = snapshot;
    }
    
        
    protected void removed() {
        loadedSnapshot.closeComponent();
        loadedSnapshot = null;
    }
    
    protected DataViewComponent createComponent() {
        DataViewComponent dvc = new DataViewComponent(
                new MasterViewSupport().getMasterView(),
                new DataViewComponent.MasterViewConfiguration(true));
        
        return dvc;
    }
    
    private static boolean isClosableView(ProfilerSnapshot snapshot) {
        // ProfilerSnapshot invisible
        if (!snapshot.isVisible()) return false;
        
        // ProfilerSnapshot not in DataSources tree
        DataSource owner = snapshot.getOwner();
        if (owner == null) return false;
        
        while (owner != null && owner != DataSource.ROOT) {
            // Application snapshot provides link to open the ProfilerSnapshot
            if (owner instanceof ApplicationSnapshot) return true;
            // Subtree containing ProfilerSnapshot invisible
            if (!owner.isVisible()) return false;
            owner = owner.getOwner();
        }
        
        // ProfilerSnapshot visible in DataSources tree
        if (owner == DataSource.ROOT) return true;
        
        // ProfilerSnapshot not in DataSources tree
        return false;
    }
    
    
    // --- General data --------------------------------------------------------
    
    private class MasterViewSupport extends JPanel  {
        
        public DataViewComponent.MasterView getMasterView() {
            JComponent srw = loadedSnapshot.getUIComponent();
            return new DataViewComponent.MasterView(NbBundle.getMessage(
                    ProfilerSnapshotView.class, "DESCR_Profiler_Snapshot"), null, srw);   // NOI18N
        }
        
    }
    
}
