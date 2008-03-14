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
import java.util.HashSet;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.netbeans.modules.profiler.utils.IDEUtils;

public final class RemoveDataSourceAction extends AbstractAction {
    
    private static RemoveDataSourceAction instance;
    
    
    public static synchronized RemoveDataSourceAction getInstance() {
        if (instance == null) instance = new RemoveDataSourceAction();
        return instance;
    }
    
    public void actionPerformed(ActionEvent e) {
        final DataSource selectedDataSource = getSelectedDataSource();
        selectedDataSource.getOwner().getRepository().removeDataSource(selectedDataSource);
    }
    
    private void updateEnabled() {
        final DataSource selectedDataSource = getSelectedDataSource();
        
        IDEUtils.runInEventDispatchThreadAndWait(new Runnable() {
            public void run() {
                setEnabled(isEnabled(selectedDataSource));
            }
        });
    }
    
    // Safe to be called from AWT EDT (the result doesn't mean the action is really available)
    private static boolean isEnabled(DataSource dataSource) {
        return dataSource != null && dataSource.supportsUserRemove();
    }
    
    // Not to be called from AWT EDT (the result reflects that the action can/cannot be invoked)
    static boolean isAvailable(DataSource dataSource) {
        return isEnabled(dataSource);
    }
    
    private DataSource getSelectedDataSource() {
        return ExplorerSupport.sharedInstance().getSelectedDataSource();
    }
    
    
    private RemoveDataSourceAction() {
        putValue(Action.NAME, "Remove");
        putValue(Action.SHORT_DESCRIPTION, "Remove");
        
        ExplorerContextMenuFactory.sharedInstance().addExplorerActionsProvider(new ExplorerActionsProvider<DataSource>() {

            public ExplorerActionDescriptor getDefaultAction(DataSource dataSource) { return null; }

            public Set<ExplorerActionDescriptor> getActions(DataSource dataSource) {
                Set<ExplorerActionDescriptor> actions = new HashSet();

                if (RemoveDataSourceAction.this.isEnabled())
                actions.add(new ExplorerActionDescriptor(RemoveDataSourceAction.this, 100));

                return actions;
            }

        }, DataSource.class);
        
        updateEnabled();
        ExplorerSupport.sharedInstance().addSelectionListener(new ExplorerSelectionListener() {
            public void selectionChanged(DataSource selected) {
                updateEnabled();
            }
        });
    }
}
