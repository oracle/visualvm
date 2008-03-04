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

package com.sun.tools.visualvm.core.heapdump;

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
class HeapDumpActionsProvider {
    

    void initialize() {
//        ExplorerContextMenuFactory.sharedInstance().addExplorerActionsProvider(new ApplicationActionProvider(), Application.class);
//        ExplorerContextMenuFactory.sharedInstance().addExplorerActionsProvider(new CoreDumpActionProvider(), CoreDump.class);
        ExplorerContextMenuFactory.sharedInstance().addExplorerActionsProvider(new HeapDumpActionProvider(), DataSource.class);
    }
    
    
    private class HeapDumpActionProvider implements ExplorerActionsProvider<DataSource> {

        public ExplorerActionDescriptor getDefaultAction(DataSource dataSource) { return null; }

        public Set<ExplorerActionDescriptor> getActions(DataSource dataSource) {
            Set<ExplorerActionDescriptor> actions = new HashSet();
            
            if (HeapDumpAction.getInstance().isEnabled())
                actions.add(new ExplorerActionDescriptor(HeapDumpAction.getInstance(), 20));
            
            return actions;
        }
        
    }
    
    
//    private class ApplicationActionProvider implements ExplorerActionsProvider<Application> {
//
//        public ExplorerActionDescriptor getDefaultAction(Application application) { return null; }
//
//        public Set<ExplorerActionDescriptor> getActions(Application application) {
//            Set<ExplorerActionDescriptor> actions = new HashSet();
//            
//            JVM jvm = JVMFactory.getJVMFor(application);
//            if (jvm.isTakeHeapDumpSupported())
//                actions.add(new ExplorerActionDescriptor(takeApplicationHeapDumpAction, 20));
//            
//            return actions;
//        }
//        
//    }
//    
//    private class CoreDumpActionProvider implements ExplorerActionsProvider<CoreDump> {
//
//        public ExplorerActionDescriptor getDefaultAction(CoreDump coreDump) { return null; }
//
//        public Set<ExplorerActionDescriptor> getActions(CoreDump coreDump) {
//            Set<ExplorerActionDescriptor> actions = new HashSet();
//            
//            actions.add(new ExplorerActionDescriptor(takeCoreDumpHeapDumpAction, 20));
//            
//            return actions;
//        }
//        
//    }

}
