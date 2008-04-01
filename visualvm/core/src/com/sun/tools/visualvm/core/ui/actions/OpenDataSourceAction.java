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
import com.sun.tools.visualvm.core.ui.DataSourceWindowManager;
import java.awt.event.ActionEvent;
import java.util.Set;


/**
 *
 * @author Jiri Sedlacek
 */
class OpenDataSourceAction extends MultiDataSourceAction<DataSource> {
    
    private static OpenDataSourceAction instance;
    
    public static synchronized OpenDataSourceAction instance() {
        if (instance == null) 
            instance = new OpenDataSourceAction();
        return instance;
    }
    
    
    protected void actionPerformed(Set<DataSource> dataSources, ActionEvent actionEvent) {
        for (DataSource dataSource : dataSources)
            DataSourceWindowManager.sharedInstance().openDataSource(dataSource);
    }

    protected boolean isEnabled(Set<DataSource> dataSources) {
        for (DataSource dataSource : dataSources)
            if (!DataSourceWindowManager.sharedInstance().canOpenDataSource(dataSource)) return false;
        return true;
    }
    
    
    private OpenDataSourceAction() {
        super(DataSource.class);
        putValue(NAME, "Open");
        putValue(SHORT_DESCRIPTION, "Open");
    }
    
}
