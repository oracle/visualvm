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

package com.sun.tools.visualvm.application.snapshot;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.core.datasource.DataSourceRoot;
import com.sun.tools.visualvm.core.explorer.ExplorerActionDescriptor;
import com.sun.tools.visualvm.core.explorer.ExplorerActionsProvider;
import com.sun.tools.visualvm.core.explorer.ExplorerContextMenuFactory;
import com.sun.tools.visualvm.core.snapshot.SnapshotsContainer;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Jiri Sedlacek
 */
final class ApplicationSnapshotActionProvider {

    private static ApplicationSnapshotActionProvider instance;


    public static synchronized ApplicationSnapshotActionProvider getInstance() {
        if (instance == null) instance = new ApplicationSnapshotActionProvider();
        return instance;
    }


    void initialize() {
        ExplorerContextMenuFactory.sharedInstance().addExplorerActionsProvider(new AddApplicationSnapshotActionProvider(), SnapshotsContainer.class);
        ExplorerContextMenuFactory.sharedInstance().addExplorerActionsProvider(new SaveApplicationActionProvider(), Application.class);
        ExplorerContextMenuFactory.sharedInstance().addExplorerActionsProvider(new AddApplicationSnapshotRootActionProvider(), DataSourceRoot.class);
    }
    
    private ApplicationSnapshotActionProvider() {
    }
    
    
    private class AddApplicationSnapshotActionProvider implements ExplorerActionsProvider<SnapshotsContainer> {
        
        public ExplorerActionDescriptor getDefaultAction(Set<SnapshotsContainer> container) {
            return new ExplorerActionDescriptor(AddApplicationSnapshotAction.getInstance(), 0);
        }

        public Set<ExplorerActionDescriptor> getActions(Set<SnapshotsContainer> container) {
            return Collections.EMPTY_SET;
        }
        
    }
    
    private class AddApplicationSnapshotRootActionProvider implements ExplorerActionsProvider<DataSourceRoot> {
        
        public ExplorerActionDescriptor getDefaultAction(Set<DataSourceRoot> root) {
            return null;
        }

        public Set<ExplorerActionDescriptor> getActions(Set<DataSourceRoot> root) {
            return Collections.singleton(new ExplorerActionDescriptor(AddApplicationSnapshotAction.getInstance(), 40));
        }
        
    }
    
    private class SaveApplicationActionProvider implements ExplorerActionsProvider<Application> {
        
        public ExplorerActionDescriptor getDefaultAction(Set<Application> applications) {
            return null;
        }

        public Set<ExplorerActionDescriptor> getActions(Set<Application> applications) {
            Set<ExplorerActionDescriptor> actions = new HashSet();
            
            if (ApplicationSnapshotAction.getInstance().isEnabled())
                actions.add(new ExplorerActionDescriptor(ApplicationSnapshotAction.getInstance(), 40));
            
            return actions;
        }
        
    }

}
