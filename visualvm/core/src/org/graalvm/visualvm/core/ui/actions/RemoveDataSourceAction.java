/*
 *  Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
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
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 */
package org.graalvm.visualvm.core.ui.actions;

import org.graalvm.visualvm.core.datasource.DataSource;
import org.graalvm.visualvm.core.datasupport.Utils;
import java.awt.event.ActionEvent;
import java.util.Set;
import org.openide.util.NbBundle;


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
        return Utils.areDataSourcesIndependent(dataSources);
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
        putValue(NAME, NbBundle.getMessage(RemoveDataSourceAction.class, "LBL_Remove"));    // NOI18N
        putValue(SHORT_DESCRIPTION, NbBundle.getMessage(RemoveDataSourceAction.class, "DESCR_Remove"));   // NOI18N
    }
}
