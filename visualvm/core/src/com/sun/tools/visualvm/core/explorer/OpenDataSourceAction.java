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
package com.sun.tools.visualvm.core.explorer;

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.ui.DataSourceWindowFactory;
import com.sun.tools.visualvm.core.ui.DataSourceWindowManager;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.netbeans.modules.profiler.utils.IDEUtils;

public final class OpenDataSourceAction extends AbstractAction {
    
    private static OpenDataSourceAction instance;
    
    
    public static synchronized OpenDataSourceAction getInstance() {
        if (instance == null) instance = new OpenDataSourceAction();
        return instance;
    }
    
    public void actionPerformed(ActionEvent e) {
        DataSource selectedDataSource = getSelectedDataSource();
        if (selectedDataSource != null &&
                DataSourceWindowFactory.sharedInstance().canCreateWindowFor(selectedDataSource)) {
            DataSource viewMaster = selectedDataSource.getMaster();
            if (viewMaster != null) DataSourceWindowManager.sharedInstance().addViews(viewMaster, selectedDataSource);
            else DataSourceWindowManager.sharedInstance().openWindow(selectedDataSource);
        } else {
            System.err.println("Cannot open DataSource " + selectedDataSource);
        }
    }
    
    private void updateEnabled() {
        DataSource selectedDataSource = getSelectedDataSource();
        final boolean isEnabled = selectedDataSource != null &&
                        DataSourceWindowFactory.sharedInstance().canCreateWindowFor(selectedDataSource);
        
        IDEUtils.runInEventDispatchThreadAndWait(new Runnable() {
            public void run() { setEnabled(isEnabled); }
        });
    }
    
    private DataSource getSelectedDataSource() {
        return ExplorerSupport.sharedInstance().getSelectedDataSource();
    }
    
    
    private OpenDataSourceAction() {
        putValue(Action.NAME, "Open");
        putValue(Action.SHORT_DESCRIPTION, "Open");
        
        updateEnabled();
        ExplorerSupport.sharedInstance().addSelectionListener(new ExplorerSelectionListener() {
            public void selectionChanged(DataSource selected) {
                updateEnabled();
            }
        });
    }
}
