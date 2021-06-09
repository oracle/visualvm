/*
 *  Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
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
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 */
package org.graalvm.visualvm.application.snapshot;

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.core.snapshot.Snapshot;
import org.graalvm.visualvm.core.datasupport.DataChangeEvent;
import org.graalvm.visualvm.core.datasupport.DataChangeListener;
import org.graalvm.visualvm.core.ui.DataSourceViewsManager;
import org.graalvm.visualvm.core.ui.actions.ActionUtils;
import org.graalvm.visualvm.core.ui.actions.MultiDataSourceAction;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Set;
import org.openide.util.NbBundle;

    
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
            (actionEvent.getModifiers() & Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) == 0);
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
        putValue(NAME, NbBundle.getMessage(ApplicationSnapshotAction.class, "LBL_Application_Snapshot"));   // NOI18N
        putValue(SHORT_DESCRIPTION, NbBundle.getMessage(ApplicationSnapshotAction.class, "DESCR_Application_Snapshot"));  // NOI18N
    }
}
