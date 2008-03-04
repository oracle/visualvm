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
package com.sun.tools.visualvm.core.snapshot.application;

import com.sun.tools.visualvm.core.datasource.Application;
import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.datasource.Snapshot;
import com.sun.tools.visualvm.core.datasupport.DataChangeEvent;
import com.sun.tools.visualvm.core.datasupport.DataChangeListener;
import com.sun.tools.visualvm.core.explorer.ExplorerSelectionListener;
import com.sun.tools.visualvm.core.explorer.ExplorerSupport;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.SwingUtilities;

public final class ApplicationSnapshotAction extends AbstractAction implements DataChangeListener {
    
    private static ApplicationSnapshotAction instance;
    
    private DataSource lastSelectedApplication;
    
    
    public static synchronized ApplicationSnapshotAction getInstance() {
        if (instance == null) instance = new ApplicationSnapshotAction();
        return instance;
    }
    
    public void actionPerformed(ActionEvent e) {
        Application selectedApplication = getSelectedApplication();
        if (selectedApplication != null && !selectedApplication.getSnapshots().isEmpty())
            ApplicationSnapshotsSupport.getInstance().getSnapshotProvider().createSnapshot(selectedApplication, (e.getModifiers() & InputEvent.CTRL_MASK) == 0);
    }
    
    void updateEnabled() {
        final Application selectedApplication = getSelectedApplication();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                setEnabled(selectedApplication != null && !selectedApplication.getSnapshots().isEmpty());
            }
        });
    }
    
    private Application getSelectedApplication() {
        DataSource selectedApplication = ExplorerSupport.sharedInstance().getSelectedDataSource();
        if (selectedApplication == null) return null;
        return selectedApplication instanceof Application ? (Application)selectedApplication : null;
    }
    
    
    private ApplicationSnapshotAction() {
        putValue(Action.NAME, "Application Snapshot");
        putValue(Action.SHORT_DESCRIPTION, "Application Snapshot");
        
        updateEnabled();
        ExplorerSupport.sharedInstance().addSelectionListener(new ExplorerSelectionListener() {
            public void selectionChanged(DataSource selected) {
                updateEnabled();
                if (lastSelectedApplication != null) {
                    lastSelectedApplication.getRepository().removeDataChangeListener(ApplicationSnapshotAction.this);
                    lastSelectedApplication = null;
                }
                if (selected instanceof Application) {
                    lastSelectedApplication = selected;
                    lastSelectedApplication.getRepository().addDataChangeListener(ApplicationSnapshotAction.this, Snapshot.class);
                }
            }
        });
    }

    
    public void dataChanged(DataChangeEvent event) {
        updateEnabled();
    }
}
