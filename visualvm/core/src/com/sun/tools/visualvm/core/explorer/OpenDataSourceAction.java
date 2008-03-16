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
import com.sun.tools.visualvm.core.ui.DataSourceViewsFactory;
import com.sun.tools.visualvm.core.ui.DataSourceWindowManager;
import java.awt.event.ActionEvent;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.netbeans.modules.profiler.NetBeansProfiler;
import org.netbeans.modules.profiler.utils.IDEUtils;
import org.openide.util.RequestProcessor;

public final class OpenDataSourceAction extends AbstractAction {
    
    private static OpenDataSourceAction instance;
    
    
    public static synchronized OpenDataSourceAction getInstance() {
        if (instance == null) instance = new OpenDataSourceAction();
        return instance;
    }
    
    public void actionPerformed(ActionEvent e) {
        final Set<DataSource> selectedDataSources = getSelectedDataSources();
        
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                if (isAvailable(selectedDataSources)) {
                    for (DataSource dataSource : selectedDataSources)
                        DataSourceWindowManager.sharedInstance().openDataSource(dataSource);
                } else {
                    NetBeansProfiler.getDefaultNB().displayError("Cannot open selected item(s)");
                }
            }
        });
    }
    
    private void updateEnabled() {
        final boolean isEnabled = !getSelectedDataSources().isEmpty();
        
        IDEUtils.runInEventDispatchThreadAndWait(new Runnable() {
            public void run() {
                setEnabled(isEnabled);
            }
        });
    }
    
    // Not to be called from AWT EDT (the result reflects that the action can/cannot be invoked)
    boolean isAvailable(Set<DataSource> selectedDataSources) {
        if (!isEnabled()) return false;
        
        for (DataSource dataSource : selectedDataSources)
            if (!DataSourceViewsFactory.sharedInstance().canCreateWindowFor(dataSource)) return false;
        return true;
    }
    
    private Set<DataSource> getSelectedDataSources() {
        return ExplorerSupport.sharedInstance().getSelectedDataSources();
    }
    
    
    private OpenDataSourceAction() {
        putValue(Action.NAME, "Open");
        putValue(Action.SHORT_DESCRIPTION, "Open");
        
        updateEnabled();
        ExplorerSupport.sharedInstance().addSelectionListener(new ExplorerSelectionListener() {
            public void selectionChanged(Set<DataSource> selected) {
                updateEnabled();
            }
        });
    }
}
