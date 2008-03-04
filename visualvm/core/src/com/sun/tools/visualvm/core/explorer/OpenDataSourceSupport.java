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
import java.util.Collections;
import java.util.Set;

/**
 *
 * @author Jiri Sedlacek
 */
final class OpenDataSourceSupport implements ExplorerActionsProvider<DataSource> {

    private static OpenDataSourceSupport instance;
    
    
    public static synchronized OpenDataSourceSupport getInstance() {
        if (instance == null) instance = new OpenDataSourceSupport();
        return instance;
    }


    void initialize() {
        ExplorerContextMenuFactory.sharedInstance().addExplorerActionsProvider(this, DataSource.class);
    }
    
    private OpenDataSourceSupport() {
    }
    

    public ExplorerActionDescriptor getDefaultAction(DataSource dataSource) {
        if (OpenDataSourceAction.getInstance().isEnabled())
            return new ExplorerActionDescriptor(OpenDataSourceAction.getInstance(), 0);
        else return null;
    }

    public Set<ExplorerActionDescriptor> getActions(DataSource dataSource) {
        return Collections.EMPTY_SET;
    }

}
