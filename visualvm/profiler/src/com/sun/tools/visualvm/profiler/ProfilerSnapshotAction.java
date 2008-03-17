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
package com.sun.tools.visualvm.profiler;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.explorer.ExplorerSelectionListener;
import com.sun.tools.visualvm.core.explorer.ExplorerSupport;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.netbeans.modules.profiler.LoadedSnapshot;
import org.netbeans.modules.profiler.NetBeansProfiler;
import org.netbeans.modules.profiler.ResultsManager;
import org.netbeans.modules.profiler.SnapshotsListener;
import org.netbeans.modules.profiler.actions.TakeSnapshotAction;
import org.netbeans.modules.profiler.utils.IDEUtils;
import org.openide.util.actions.SystemAction;

public final class ProfilerSnapshotAction extends AbstractAction {
    
    private static ProfilerSnapshotAction instance;
    
    private final TakeSnapshotAction originalAction = SystemAction.get(TakeSnapshotAction.class);
    private boolean openNextSnapshot = true;
    
    
    public static synchronized ProfilerSnapshotAction getInstance() {
        if (instance == null) instance = new ProfilerSnapshotAction();
        return instance;
    }
    
    public synchronized void actionPerformed(final ActionEvent e) {
        final Application selectedApplication = getSelectedApplication();
        
        if (isAvailable(selectedApplication)) {
            originalAction.performAction();
            openNextSnapshot = (e.getModifiers() & InputEvent.CTRL_MASK) == 0;
        } else {
            NetBeansProfiler.getDefaultNB().displayError("Cannot take profiler snapshot");
        }
    }
    
    
    private void updateEnabled() {
        final Application selectedApplication = getSelectedApplication();
        
        IDEUtils.runInEventDispatchThreadAndWait(new Runnable() {
            public void run() {
                setEnabled(isEnabled(selectedApplication));
            }
        });
    }
    
    private boolean isEnabled(Application application) {
        return ProfilerSupport.getInstance().getProfiledApplication() == application &&
                originalAction.isEnabled();
    }
    
    // Not to be called from AWT EDT (the result reflects that the action can/cannot be invoked)
    boolean isAvailable(Application application) {
        return isEnabled(application);
    }
    
    private Application getSelectedApplication() {
        DataSource selectedDataSource = ExplorerSupport.sharedInstance().getSelectedDataSource();
        return (selectedDataSource != null && selectedDataSource instanceof Application) ? (Application)selectedDataSource : null;
    }
    
    
    private ProfilerSnapshotAction() {
        putValue(Action.NAME, "Profiler Snapshot");
        putValue(Action.SHORT_DESCRIPTION, "Profiler Snapshot");
        
        updateEnabled();
        originalAction.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if (SystemAction.PROP_ENABLED.equals(evt.getPropertyName()))
                    updateEnabled();
            }
        });
        ExplorerSupport.sharedInstance().addSelectionListener(new ExplorerSelectionListener() {
            public void selectionChanged(Set<DataSource> selected) {
                updateEnabled();
            }
        });
        ResultsManager.getDefault().addSnapshotsListener(new SnapshotsListener() {
            public void snapshotLoaded(LoadedSnapshot arg0) {}
            public void snapshotRemoved(LoadedSnapshot arg0) {}
            public void snapshotTaken(LoadedSnapshot arg0) {}
            
            public void snapshotSaved(LoadedSnapshot arg0) {
                ProfilerSupport.getInstance().getSnapshotsProvider().createSnapshot(arg0, openNextSnapshot);
                openNextSnapshot = true;
            }
        });
    }
}
