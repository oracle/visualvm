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

package com.sun.tools.visualvm.coredump.impl;

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.datasource.DataSourceRoot;
import com.sun.tools.visualvm.core.explorer.ExplorerActionDescriptor;
import com.sun.tools.visualvm.core.explorer.ExplorerActionsProvider;
import com.sun.tools.visualvm.core.explorer.ExplorerContextMenuFactory;
import com.sun.tools.visualvm.coredump.CoreDumpsContainer;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.openide.util.Utilities;

/**
 *
 * @author Tomas Hurka
 */
public class CoreDumpActionsProvider {
    
    
    public static void register() {
        if (Utilities.isWindows()) return;
        
        ExplorerContextMenuFactory explorer = ExplorerContextMenuFactory.sharedInstance();
        explorer.addExplorerActionsProvider(new CoreDumpsContainerActionProvider(), CoreDumpsContainer.class);
        explorer.addExplorerActionsProvider(new DataSourceRootActionProvider(), DataSourceRoot.class);
    }
    
    
    private static class CoreDumpsContainerActionProvider implements ExplorerActionsProvider<CoreDumpsContainer> {
        
        public ExplorerActionDescriptor getDefaultAction(Set<CoreDumpsContainer> container) {
            return new ExplorerActionDescriptor(AddVMCoredumpAction.getInstance(), 0);
        }
        
        public Set<ExplorerActionDescriptor> getActions(Set<CoreDumpsContainer> container) {
            return Collections.EMPTY_SET;
        }
        
    }
    
    private static class DataSourceRootActionProvider implements ExplorerActionsProvider<DataSource> {
        
        public ExplorerActionDescriptor getDefaultAction(Set<DataSource> dataSource) {
            return null;
        }
        
        public Set<ExplorerActionDescriptor> getActions(Set<DataSource> root) {
            Set<ExplorerActionDescriptor> actions = new HashSet();
            
            actions.add(new ExplorerActionDescriptor(AddVMCoredumpAction.getInstance(), 30));
            
            return actions;
        }
        
    }
    
}
