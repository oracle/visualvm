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
import com.sun.tools.visualvm.core.ui.actions.ActionUtils;
import com.sun.tools.visualvm.core.ui.actions.SingleDataSourceAction;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import org.netbeans.modules.profiler.LoadedSnapshot;
import org.netbeans.modules.profiler.ResultsManager;
import org.netbeans.modules.profiler.SnapshotsListener;
import org.netbeans.modules.profiler.actions.TakeSnapshotAction;
import org.openide.util.actions.SystemAction;

    
/**
 *
 * @author Jiri Sedlacek
 */
class ProfilerSnapshotAction extends SingleDataSourceAction<Application> {
    
    private final TakeSnapshotAction originalAction = SystemAction.get(TakeSnapshotAction.class);
    private boolean openNextSnapshot = true;
    
    
    public static ProfilerSnapshotAction create() {
        ProfilerSnapshotAction action = new ProfilerSnapshotAction();
        action.initialize();
        return action;
    }
    
        
    protected void actionPerformed(Application dataSource, ActionEvent actionEvent) {
        openNextSnapshot = (actionEvent.getModifiers() & InputEvent.CTRL_MASK) == 0;
            originalAction.performAction();
        }
    
    protected boolean isEnabled(Application application) {
        return ProfilerSupport.getInstance().getProfiledApplication() == application && originalAction.isEnabled();
            }
    
    protected void initialize() {
        super.initialize();
    
        originalAction.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if (SystemAction.PROP_ENABLED.equals(evt.getPropertyName()))
                    ProfilerSnapshotAction.this.updateState(ActionUtils.getSelectedDataSources(Application.class));
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
    
    
    private ProfilerSnapshotAction() {
        super(Application.class);
        putValue(NAME, "Profiler Snapshot");
        putValue(SHORT_DESCRIPTION, "Profiler Snapshot");
}
}
