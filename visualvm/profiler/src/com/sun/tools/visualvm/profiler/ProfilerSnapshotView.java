/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tools.visualvm.profiler;

import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import com.sun.tools.visualvm.core.datasupport.Positionable;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import java.awt.Dimension;
import javax.swing.JPanel;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.modules.profiler.LoadedSnapshot;
import org.netbeans.modules.profiler.SnapshotResultsWindow;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
class ProfilerSnapshotView extends DataSourceView {
    
    private LoadedSnapshot loadedSnapshot;
    

    public ProfilerSnapshotView(ProfilerSnapshot snapshot) {
        this(snapshot, DataSourceDescriptorFactory.getDescriptor(snapshot));
    }
    
    private ProfilerSnapshotView(ProfilerSnapshot snapshot, DataSourceDescriptor descriptor) {
        super(snapshot, descriptor.getName(), descriptor.getIcon(), Positionable.POSITION_AT_THE_END, true);
        loadedSnapshot = snapshot.getLoadedSnapshot();
    }
    
        
    protected void removed() {
        loadedSnapshot = null;
    }
    
    protected DataViewComponent createComponent() {
        DataViewComponent dvc = new DataViewComponent(
                new MasterViewSupport().getMasterView(),
                new DataViewComponent.MasterViewConfiguration(true));
        
        return dvc;
    }
    
    
    // --- General data --------------------------------------------------------
    
    private class MasterViewSupport extends JPanel  {
        
        public DataViewComponent.MasterView getMasterView() {
            SnapshotResultsWindow srw = SnapshotResultsWindow.get(loadedSnapshot, CommonConstants.SORTING_COLUMN_DEFAULT, false);
            srw.setPreferredSize(new Dimension(1, 1));
            return new DataViewComponent.MasterView(NbBundle.getMessage(ProfilerSnapshotView.class, "MSG_Profiler_Snapshot"), null, srw);   // NOI18N
        }
        
    }
    
}
