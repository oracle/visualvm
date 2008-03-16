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
import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.snapshot.Snapshot;
import com.sun.tools.visualvm.core.datasupport.DataChangeEvent;
import com.sun.tools.visualvm.core.datasupport.DataChangeListener;
import com.sun.tools.visualvm.core.explorer.ExplorerSelectionListener;
import com.sun.tools.visualvm.core.explorer.ExplorerSupport;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.netbeans.modules.profiler.utils.IDEUtils;

public final class ApplicationSnapshotAction extends AbstractAction implements DataChangeListener {
    
    private static ApplicationSnapshotAction instance;
    
    private Set<Application> lastSelectedApplications;
    
    
    public static synchronized ApplicationSnapshotAction getInstance() {
        if (instance == null) instance = new ApplicationSnapshotAction();
        return instance;
    }
    
    public void actionPerformed(ActionEvent e) {
        Set<Application> saveableApplications = getSaveableApplications();
        for (Application application : saveableApplications)
            ApplicationSnapshotsSupport.getInstance().getSnapshotProvider().createSnapshot(application, (e.getModifiers() & InputEvent.CTRL_MASK) == 0);
    }
    
    void updateEnabled() {
        Set<Application> saveableApplications = getSaveableApplications();
        final boolean isEnabled = !saveableApplications.isEmpty();
        
        IDEUtils.runInEventDispatchThread(new Runnable() {
            public void run() { setEnabled(isEnabled); }
        });
    }
    
    private Set<Application> getSaveableApplications() {
        Set<Application> selectedApplications = getSelectedApplications();
        Set<Application> saveableApplications = new HashSet();
        for (Application application : selectedApplications)
            if (!application.getRepository().getDataSources(Snapshot.class).isEmpty())
                saveableApplications.add(application);
            else return Collections.EMPTY_SET;
        return saveableApplications;
    }
    
    private Set<Application> getSelectedApplications() {
        Set<DataSource> selectedDataSources = ExplorerSupport.sharedInstance().getSelectedDataSources();
        Set<Application> selectedApplications = new HashSet();
        for (DataSource dataSource : selectedDataSources)
            if (dataSource instanceof Application)
                selectedApplications.add((Application)dataSource);
            else return Collections.EMPTY_SET;
        return selectedApplications;
    }
    
    
    private ApplicationSnapshotAction() {
        putValue(Action.NAME, "Application Snapshot");
        putValue(Action.SHORT_DESCRIPTION, "Application Snapshot");
        
        updateEnabled();
        ExplorerSupport.sharedInstance().addSelectionListener(new ExplorerSelectionListener() {
            public void selectionChanged(Set<DataSource> selected) {
                Set<Application> selectedApplications = getSelectedApplications();
                if (lastSelectedApplications != null) {
                    for (Application application : lastSelectedApplications)
                        application.getRepository().removeDataChangeListener(ApplicationSnapshotAction.this);
                    lastSelectedApplications = null;
                }
                if (!selectedApplications.isEmpty()) {
                    lastSelectedApplications = selectedApplications;
                    for (Application application : lastSelectedApplications)
                        application.getRepository().addDataChangeListener(ApplicationSnapshotAction.this, Snapshot.class);
                }
            }
        });
    }

    
    public void dataChanged(DataChangeEvent event) {
        updateEnabled();
    }
}
