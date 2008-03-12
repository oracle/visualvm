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

package com.sun.tools.visualvm.threaddump.impl;

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.explorer.ExplorerActionDescriptor;
import com.sun.tools.visualvm.core.explorer.ExplorerActionsProvider;
import com.sun.tools.visualvm.core.explorer.ExplorerContextMenuFactory;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Jiri Sedlacek
 */
public class ThreadDumpActionsProvider {
    
    public void initialize() {
        ExplorerContextMenuFactory.sharedInstance().addExplorerActionsProvider(new ThreadDumpActionProvider(), DataSource.class);
//        ExplorerContextMenuFactory.sharedInstance().addExplorerActionsProvider(new ApplicationNodeActionProvider(), Application.class);
//        ExplorerContextMenuFactory.sharedInstance().addExplorerActionsProvider(new CoreDumpNodeActionProvider(), CoreDump.class);
    }
    
    
    private class ThreadDumpActionProvider implements ExplorerActionsProvider<DataSource> {

        public ExplorerActionDescriptor getDefaultAction(DataSource dataSource) { return null; }

        public Set<ExplorerActionDescriptor> getActions(DataSource dataSource) {
            Set<ExplorerActionDescriptor> actions = new HashSet();
            
            if (ThreadDumpAction.isAvailable(dataSource))
                actions.add(new ExplorerActionDescriptor(ThreadDumpAction.getInstance(), 10));
            
            return actions;
        }
        
    }
    
    
//    private class ApplicationNodeActionProvider implements ExplorerActionsProvider<Application> {
//
//        public ExplorerActionDescriptor getDefaultAction(Application application) { return null; }
//
//        public Set<ExplorerActionDescriptor> getActions(Application application) {
//            Set<ExplorerActionDescriptor> actions = new HashSet();
//            
//            if (ThreadDumpAction.getInstance().isEnabled())
//                actions.add(new ExplorerActionDescriptor(ThreadDumpAction.getInstance(), 10));
//            
//            return actions;
//        }
//        
//    }
//    
//    private class CoreDumpNodeActionProvider implements ExplorerActionsProvider<CoreDump> {
//
//        public ExplorerActionDescriptor getDefaultAction(CoreDump coreDump) { return null; }
//
//        public Set<ExplorerActionDescriptor> getActions(CoreDump coreDump) {
//            Set<ExplorerActionDescriptor> actions = new HashSet();
//            
//            if (ThreadDumpAction.getInstance().isEnabled())
//                actions.add(new ExplorerActionDescriptor(ThreadDumpAction.getInstance(), 10));
//            
//            return actions;
//        }
//        
//    }

}
