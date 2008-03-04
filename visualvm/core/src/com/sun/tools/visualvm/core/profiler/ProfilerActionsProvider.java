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

package com.sun.tools.visualvm.core.profiler;

import com.sun.tools.visualvm.core.datasource.Application;
import com.sun.tools.visualvm.core.explorer.ExplorerActionDescriptor;
import com.sun.tools.visualvm.core.explorer.ExplorerActionsProvider;
import com.sun.tools.visualvm.core.explorer.ExplorerContextMenuFactory;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Jiri Sedlacek
 */
final class ProfilerActionsProvider {

    private static ProfilerActionsProvider instance;
    

    public static synchronized ProfilerActionsProvider getInstance() {
        if (instance == null) instance = new ProfilerActionsProvider();
        return instance;
    }


    void initialize() {
        ExplorerContextMenuFactory.sharedInstance().addExplorerActionsProvider(new ApplicationActionsProvider(), Application.class);
    }
    
    private ProfilerActionsProvider() {
    }
    
    
    private class ApplicationActionsProvider implements ExplorerActionsProvider<Application> {
        
        public ExplorerActionDescriptor getDefaultAction(Application application) {
            return null;
        }

        public Set<ExplorerActionDescriptor> getActions(Application application) {
            Set<ExplorerActionDescriptor> actions = new HashSet();
            
            if (ProfileApplicationAction.getInstance().isEnabled())
                actions.add(new ExplorerActionDescriptor(ProfileApplicationAction.getInstance(), 30));
            
            return actions;
        }
        
    }

}
