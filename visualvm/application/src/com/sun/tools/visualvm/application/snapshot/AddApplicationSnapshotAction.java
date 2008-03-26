/*
 *  Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Sun designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Sun in the LICENSE file that accompanied this code.
 * 
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 * 
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 *  Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 *  CA 95054 USA or visit www.sun.com if you need additional information or
 *  have any questions.
 */
package com.sun.tools.visualvm.application.snapshot;

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.snapshot.SnapshotsContainer;
import com.sun.tools.visualvm.core.ui.actions.ActionUtils;
import com.sun.tools.visualvm.core.ui.actions.SingleDataSourceAction;
import java.awt.event.ActionEvent;
import java.util.Set;
import javax.swing.Action;

    
/**
 *
 * @author Jiri Sedlacek
 */
class AddApplicationSnapshotAction extends SingleDataSourceAction<DataSource> {
    
    private boolean tracksSelection = false;
    
    
    public static AddApplicationSnapshotAction alwaysEnabled() {
        AddApplicationSnapshotAction action = new AddApplicationSnapshotAction();
        action.initialize();
        return action;
    }
    
    public static AddApplicationSnapshotAction selectionAware() {
        AddApplicationSnapshotAction action = new AddApplicationSnapshotAction().trackSelection();
        action.initialize();
        return action;
    }
    
    
    protected void actionPerformed(DataSource dataSource, ActionEvent actionEvent) {
        ApplicationSnapshotConfigurator newSnapshotConfiguration = ApplicationSnapshotConfigurator.defineSnapshot();
        if (newSnapshotConfiguration != null) {
            ApplicationSnapshotProvider provider = ApplicationSnapshotsSupport.getInstance().getSnapshotProvider();
            provider.addSnapshotArchive(newSnapshotConfiguration.getSnapshotFile(), newSnapshotConfiguration.deleteSourceFile());
        }
    }
    
    protected boolean isEnabled(DataSource dataSource) {
        return dataSource == DataSource.ROOT || dataSource instanceof SnapshotsContainer;
    }
    
    protected void updateState(Set<DataSource> selectedDataSources) {
        if (tracksSelection) super.updateState(selectedDataSources);
    }
    
    private AddApplicationSnapshotAction trackSelection() {
        tracksSelection = true;
        updateState(ActionUtils.getSelectedDataSources());
        return this;
    }
    
    
    private AddApplicationSnapshotAction() {
        super(DataSource.class);
        putValue(Action.NAME, "Add Application Snapshot...");
        putValue(Action.SHORT_DESCRIPTION, "Add Application Snapshot");
    }
}
