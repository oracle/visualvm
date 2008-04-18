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

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.core.snapshot.Snapshot;
import com.sun.tools.visualvm.core.datasupport.DataChangeEvent;
import com.sun.tools.visualvm.core.datasupport.DataChangeListener;
import com.sun.tools.visualvm.core.ui.DataSourceViewsManager;
import com.sun.tools.visualvm.core.ui.actions.ActionUtils;
import com.sun.tools.visualvm.core.ui.actions.MultiDataSourceAction;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.util.HashSet;
import java.util.Set;

    
/**
 *
 * @author Jiri Sedlacek
 */
class ApplicationSnapshotAction extends MultiDataSourceAction<Application> {
    
    private Set<Application> lastSelectedApplications = new HashSet();
    
    private static ApplicationSnapshotAction instance;
    
    public static synchronized ApplicationSnapshotAction instance() {
        if (instance == null) 
            instance = new ApplicationSnapshotAction();
        return instance;
    }
    
    
    private final DataChangeListener changeListener = new DataChangeListener() {
        public void dataChanged(DataChangeEvent event) {
            ApplicationSnapshotAction.super.updateState(ActionUtils.getSelectedDataSources(Application.class));
        }
    };
    
        
    protected void actionPerformed(Set<Application> applications, ActionEvent actionEvent) {
        for (Application application : applications)
            ApplicationSnapshotsSupport.getInstance().getSnapshotProvider().createSnapshot(application,
            (actionEvent.getModifiers() & InputEvent.CTRL_MASK) == 0);
    }
    
    protected boolean isEnabled(Set<Application> applications) {
        for (Application application : applications) {
            if (DataSourceViewsManager.sharedInstance().canSaveViewsFor(application, ApplicationSnapshot.class))
                return true;
            if (application.getRepository().getDataSources(Snapshot.class).isEmpty())
                return false;
        }
        return true;
    }
    
    protected void updateState(Set<Application> selectedApplications) {
        super.updateState(selectedApplications);
    
        if (!lastSelectedApplications.isEmpty())
            for (Application application : lastSelectedApplications)
                application.getRepository().removeDataChangeListener(changeListener);
        lastSelectedApplications.clear();
        
        if (!selectedApplications.isEmpty()) {
            lastSelectedApplications.addAll(selectedApplications);
            for (Application application : lastSelectedApplications)
                application.getRepository().addDataChangeListener(changeListener, Snapshot.class);
        }
    }

    
    private ApplicationSnapshotAction() {
        super(Application.class);
        putValue(NAME, "Application Snapshot");
        putValue(SHORT_DESCRIPTION, "Application Snapshot");
    }
}
