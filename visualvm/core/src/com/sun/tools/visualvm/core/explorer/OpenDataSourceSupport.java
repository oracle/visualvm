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

package com.sun.tools.visualvm.core.explorer;

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.ui.DataSourceUIFactory;
import com.sun.tools.visualvm.core.ui.DataSourceUIManager;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.List;
import javax.swing.AbstractAction;

/**
 *
 * @author Jiri Sedlacek
 */
final class OpenDataSourceSupport implements ExplorerActionsProvider<DataSourceExplorerNode> {

    private static OpenDataSourceSupport instance;


    public static synchronized OpenDataSourceSupport getInstance() {
        if (instance == null) instance = new OpenDataSourceSupport();
        return instance;
    }


    void initialize() {
        ExplorerContextMenuFactory.sharedInstance().addExplorerActionsProvider(this, DataSourceExplorerNode.class);
    }
    
    private OpenDataSourceSupport() {
    }
    

    public ExplorerActionDescriptor getDefaultAction(DataSourceExplorerNode node) {
        DataSource dataSource = node.getDataSource();
        if (DataSourceUIFactory.sharedInstance().canCreateWindowFor(dataSource))
            return new ExplorerActionDescriptor(new OpenDataSourceAction(dataSource), 0);
        else return null;
    }

    public List<ExplorerActionDescriptor> getActions(DataSourceExplorerNode node) {
        return Collections.EMPTY_LIST;
    }
    
    
    private class OpenDataSourceAction extends AbstractAction {
        
        private DataSource dataSource;
        
        public OpenDataSourceAction(DataSource dataSource) {
            super("Open");
            this.dataSource = dataSource;
        }
        
        public void actionPerformed(ActionEvent e) {
            DataSourceUIManager uiManager = DataSourceUIManager.sharedInstance();
            
            // Open DataSource in separate TopComponent
            if (dataSource.getMaster() == null) uiManager.openWindow(dataSource);
            
            // Open DataSource as a view in its ViewMaster's TopComponent
            DataSource viewMaster = dataSource.getMaster();
            if (viewMaster != null) uiManager.addView(viewMaster, dataSource);
            else uiManager.openWindow(dataSource);
            
        }
        
    }

}
