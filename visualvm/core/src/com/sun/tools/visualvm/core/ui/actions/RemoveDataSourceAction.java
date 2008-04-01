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
package com.sun.tools.visualvm.core.ui.actions;

import com.sun.tools.visualvm.core.datasource.DataSource;
import java.awt.event.ActionEvent;
import java.util.Set;


/**
 *
 * @author Jiri Sedlacek
 */
class RemoveDataSourceAction extends MultiDataSourceAction<DataSource> {
    
    private static RemoveDataSourceAction instance;
    
    public static synchronized RemoveDataSourceAction instance() {
        if (instance == null) 
            instance = new RemoveDataSourceAction();
        return instance;
    }
    
    
    protected void actionPerformed(Set<DataSource> dataSources, ActionEvent actionEvent) {
        for (DataSource dataSource : dataSources)
            if (checkRemove(dataSource))
                dataSource.getOwner().getRepository().removeDataSource(dataSource);
    }

    protected boolean isEnabled(Set<DataSource> dataSources) {
        for (DataSource dataSource : dataSources)
            if (!dataSource.supportsUserRemove()) return false;
        return true;
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
        super(DataSource.class);
        putValue(NAME, "Remove");
        putValue(SHORT_DESCRIPTION, "Remove");
    }
}
