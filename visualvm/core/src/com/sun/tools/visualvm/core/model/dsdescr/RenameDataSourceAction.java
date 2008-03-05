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
package com.sun.tools.visualvm.core.model.dsdescr;

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.explorer.ExplorerSelectionListener;
import com.sun.tools.visualvm.core.explorer.ExplorerSupport;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.netbeans.modules.profiler.utils.IDEUtils;

public final class RenameDataSourceAction extends AbstractAction {
    
    private static RenameDataSourceAction instance;
    
    
    public static synchronized RenameDataSourceAction getInstance() {
        if (instance == null) instance = new RenameDataSourceAction();
        return instance;
    }
    
    public void actionPerformed(ActionEvent e) {
        DataSource selectedDataSource = getSelectedDataSource();
        DataSourceDescriptor descriptor = selectedDataSource != null ? DataSourceDescriptorFactory.getDescriptor(selectedDataSource) : null;
        if (descriptor != null && descriptor.supportsRename()) {
            RenameConfigurator configurator = RenameConfigurator.defineName(selectedDataSource);
            if (configurator != null) descriptor.setName(configurator.getDisplayName());
        } else {
            System.err.println("Cannot rename DataSource " + selectedDataSource);
        }
    }
    
    private void updateEnabled() {
        DataSource selectedDataSource = getSelectedDataSource();
        DataSourceDescriptor descriptor = selectedDataSource != null ? DataSourceDescriptorFactory.getDescriptor(selectedDataSource) : null;
        final boolean isEnabled = descriptor != null && descriptor.supportsRename();
        
        IDEUtils.runInEventDispatchThreadAndWait(new Runnable() {
            public void run() { setEnabled(isEnabled); }
        });
    }
    
    private DataSource getSelectedDataSource() {
        return ExplorerSupport.sharedInstance().getSelectedDataSource();
    }
    
    
    private RenameDataSourceAction() {
        putValue(Action.NAME, "Rename...");
        putValue(Action.SHORT_DESCRIPTION, "Rename");
        
        updateEnabled();
        ExplorerSupport.sharedInstance().addSelectionListener(new ExplorerSelectionListener() {
            public void selectionChanged(DataSource selected) {
                updateEnabled();
            }
        });
    }
}
