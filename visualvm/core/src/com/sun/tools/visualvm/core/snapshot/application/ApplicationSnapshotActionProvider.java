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

package com.sun.tools.visualvm.core.snapshot.application;

import com.sun.tools.visualvm.core.datasource.Application;
import com.sun.tools.visualvm.core.datasource.DataSourceRoot;
import com.sun.tools.visualvm.core.explorer.ExplorerActionDescriptor;
import com.sun.tools.visualvm.core.explorer.ExplorerActionsProvider;
import com.sun.tools.visualvm.core.explorer.ExplorerContextMenuFactory;
import com.sun.tools.visualvm.core.snapshot.SnapshotsContainer;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.swing.AbstractAction;

/**
 *
 * @author Jiri Sedlacek
 */
final class ApplicationSnapshotActionProvider {

    private static ApplicationSnapshotActionProvider instance;
    
    private final AddApplicationSnapshotAction addApplicationSnapshotAction = new AddApplicationSnapshotAction();
    private final SaveApplicationAction saveApplicationAction = new SaveApplicationAction();
    private final SaveApplicationSnapshotAction saveApplicationSnapshotAction = new SaveApplicationSnapshotAction();
    private final DeleteApplicationSnapshotAction deleteApplicationSnapshotAction = new DeleteApplicationSnapshotAction();


    public static synchronized ApplicationSnapshotActionProvider getInstance() {
        if (instance == null) instance = new ApplicationSnapshotActionProvider();
        return instance;
    }


    void initialize() {
        ExplorerContextMenuFactory.sharedInstance().addExplorerActionsProvider(new AddApplicationSnapshotActionProvider(), SnapshotsContainer.class);
        ExplorerContextMenuFactory.sharedInstance().addExplorerActionsProvider(new SaveApplicationActionProvider(), Application.class);
        ExplorerContextMenuFactory.sharedInstance().addExplorerActionsProvider(new ApplicationSnapshotActionsProvider(), ApplicationSnapshot.class);
        ExplorerContextMenuFactory.sharedInstance().addExplorerActionsProvider(new AddApplicationSnapshotRootActionProvider(), DataSourceRoot.class);
    }
    
    private ApplicationSnapshotActionProvider() {
    }
    
    
    private class AddApplicationSnapshotAction extends AbstractAction {
        
        public AddApplicationSnapshotAction() {
            super("Add Application Snapshot...");
        }
        
        public void actionPerformed(ActionEvent e) {
            ApplicationSnapshotConfigurator newSnapshotConfiguration = ApplicationSnapshotConfigurator.defineSnapshot();
            if (newSnapshotConfiguration != null) {
                ApplicationSnapshotProvider provider = ApplicationSnapshotsSupport.getInstance().getSnapshotProvider();
                provider.addSnapshotArchive(newSnapshotConfiguration.getSnapshotFile(), newSnapshotConfiguration.deleteSourceFile());
            }
        }
        
    }
    
    private class SaveApplicationAction extends AbstractAction {
        
        public SaveApplicationAction() {
            super("Save Snapshot");
        }
        
        public void actionPerformed(ActionEvent e) {
            Application dataSource = (Application)e.getSource();
            ApplicationSnapshotsSupport.getInstance().getSnapshotProvider().createSnapshot(dataSource, (e.getModifiers() & InputEvent.CTRL_MASK) == 0);
        }
        
    }
    
    private class SaveApplicationSnapshotAction extends AbstractAction {
        
        public SaveApplicationSnapshotAction() {
            super("Save As...");
        }
        
        public void actionPerformed(ActionEvent e) {
            ApplicationSnapshot snapshot = (ApplicationSnapshot)e.getSource();
            snapshot.saveAs();
        }
        
    }
    
    private class DeleteApplicationSnapshotAction extends AbstractAction {
        
        public DeleteApplicationSnapshotAction() {
            super("Delete");
        }
        
        public void actionPerformed(ActionEvent e) {
            ApplicationSnapshot snapshot = (ApplicationSnapshot)e.getSource();
            ApplicationSnapshotsSupport.getInstance().getSnapshotProvider().deleteSnapshot(snapshot, (e.getModifiers() & InputEvent.CTRL_MASK) == 0);
        }
        
    }
    
    
    private class AddApplicationSnapshotActionProvider implements ExplorerActionsProvider<SnapshotsContainer> {
        
        public ExplorerActionDescriptor getDefaultAction(SnapshotsContainer container) {
            return new ExplorerActionDescriptor(addApplicationSnapshotAction, 0);
        }

        public Set<ExplorerActionDescriptor> getActions(SnapshotsContainer container) {
            return Collections.EMPTY_SET;
        }
        
    }
    
    private class AddApplicationSnapshotRootActionProvider implements ExplorerActionsProvider<DataSourceRoot> {
        
        public ExplorerActionDescriptor getDefaultAction(DataSourceRoot root) {
            return null;
        }

        public Set<ExplorerActionDescriptor> getActions(DataSourceRoot root) {
            return Collections.singleton(new ExplorerActionDescriptor(addApplicationSnapshotAction, 30));
        }
        
    }
    
    private class SaveApplicationActionProvider implements ExplorerActionsProvider<Application> {
        
        public ExplorerActionDescriptor getDefaultAction(Application application) {
            if (!application.getSnapshots().isEmpty())
                return new ExplorerActionDescriptor(saveApplicationAction, 10);
            else return null;
        }

        public Set<ExplorerActionDescriptor> getActions(Application application) {
            return Collections.EMPTY_SET;
        }
        
    }
    
    private class ApplicationSnapshotActionsProvider implements ExplorerActionsProvider<ApplicationSnapshot> {
        
        public ExplorerActionDescriptor getDefaultAction(ApplicationSnapshot snapshot) {
            return null;
        }

        public Set<ExplorerActionDescriptor> getActions(ApplicationSnapshot snapshot) {
            Set<ExplorerActionDescriptor> actions = new HashSet();
    
            actions.add(new ExplorerActionDescriptor(saveApplicationSnapshotAction, 10));
            actions.add(new ExplorerActionDescriptor(deleteApplicationSnapshotAction, 20));
            
            return actions;
        }
        
    }

}
