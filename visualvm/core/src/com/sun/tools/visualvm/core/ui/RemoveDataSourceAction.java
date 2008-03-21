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
package com.sun.tools.visualvm.core.ui;

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.explorer.ExplorerActionDescriptor;
import com.sun.tools.visualvm.core.explorer.ExplorerActionsProvider;
import com.sun.tools.visualvm.core.explorer.ExplorerContextMenuFactory;
import com.sun.tools.visualvm.core.explorer.ExplorerSelectionListener;
import com.sun.tools.visualvm.core.explorer.ExplorerSupport;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.netbeans.modules.profiler.utils.IDEUtils;

class RemoveDataSourceAction extends AbstractAction {
    
    private static RemoveDataSourceAction instance;
    
    
    public static synchronized RemoveDataSourceAction getInstance() {
        if (instance == null) instance = new RemoveDataSourceAction();
        return instance;
    }
    
    public void actionPerformed(ActionEvent e) {
        Set<DataSource> removableDataSources = getRemovableDataSources();
        for (DataSource dataSource : removableDataSources)
            if (checkRemove(dataSource))
                dataSource.getOwner().getRepository().removeDataSource(dataSource);
    }
    
    private void updateEnabled() {
        final boolean isEnabled = !getRemovableDataSources().isEmpty();
        
        IDEUtils.runInEventDispatchThreadAndWait(new Runnable() {
            public void run() { setEnabled(isEnabled); }
        });
    }
    
    private Set<DataSource> getRemovableDataSources() {
        Set<DataSource> selectedDataSources = getSelectedDataSources();
        Set<DataSource> removableDataSources = new HashSet();
        for (DataSource dataSource : selectedDataSources)
            if (!dataSource.supportsUserRemove()) return Collections.EMPTY_SET;
            else removableDataSources.add(dataSource);
        return removableDataSources;
    }
    
    private Set<DataSource> getSelectedDataSources() {
        return ExplorerSupport.sharedInstance().getSelectedDataSources();
    }
    
    
    private static boolean checkRemove(DataSource dataSource) {
        // Check if the DataSource can be removed
        if (!dataSource.checkRemove(dataSource)) return false;
        
        // Check if all repository DataSources can be removed
        Set<? extends DataSource> repositoryDataSources = dataSource.getRepository().getDataSources();
        for (DataSource repositoryDataSource : repositoryDataSources)
            if (!repositoryDataSource.checkRemove(dataSource)) return false;
        return true;
    }
    
    
    private RemoveDataSourceAction() {
        putValue(Action.NAME, "Remove");
        putValue(Action.SHORT_DESCRIPTION, "Remove");
        
        ExplorerContextMenuFactory.sharedInstance().addExplorerActionsProvider(new ExplorerActionsProvider<DataSource>() {

            public ExplorerActionDescriptor getDefaultAction(Set<DataSource> dataSources) { return null; }

            public Set<ExplorerActionDescriptor> getActions(Set<DataSource> dataSources) {
                Set<ExplorerActionDescriptor> actions = new HashSet();

                if (RemoveDataSourceAction.this.isEnabled())
                    actions.add(new ExplorerActionDescriptor(RemoveDataSourceAction.this, 100));

                return actions;
            }

        }, DataSource.class);
        
        updateEnabled();
        ExplorerSupport.sharedInstance().addSelectionListener(new ExplorerSelectionListener() {
            public void selectionChanged(Set<DataSource> selected) {
                updateEnabled();
            }
        });
    }
}
