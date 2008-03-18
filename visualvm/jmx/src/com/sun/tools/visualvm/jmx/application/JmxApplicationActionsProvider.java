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

package com.sun.tools.visualvm.jmx.application;

import com.sun.tools.visualvm.core.datasource.DataSourceRoot;
import com.sun.tools.visualvm.core.explorer.ExplorerActionDescriptor;
import com.sun.tools.visualvm.core.explorer.ExplorerActionsProvider;
import com.sun.tools.visualvm.core.explorer.ExplorerContextMenuFactory;
import com.sun.tools.visualvm.host.Host;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 * @author Luis-Miguel Alventosa
 */
class JmxApplicationActionsProvider {

    static void initialize() {
        ExplorerContextMenuFactory explorer = ExplorerContextMenuFactory.sharedInstance();
        explorer.addExplorerActionsProvider(
                new HostActionProvider(), Host.class);
        explorer.addExplorerActionsProvider(
                new DataSourceRootActionProvider(), DataSourceRoot.class);
    }

    private static class HostActionProvider implements ExplorerActionsProvider<Host> {

        public ExplorerActionDescriptor getDefaultAction(Set<Host> hosts) {
            return null;
        }

        public Set<ExplorerActionDescriptor> getActions(Set<Host> hosts) {
            Set<ExplorerActionDescriptor> actions =
                    new HashSet<ExplorerActionDescriptor>();
            
            if (hosts.size() == 1 && !Host.UNKNOWN_HOST.equals(hosts.iterator().next())) {
                actions.add(new ExplorerActionDescriptor(AddJMXConnectionAction.getInstance(), 50));
                if (!Host.LOCALHOST.equals(hosts.iterator().next())) actions.add(new ExplorerActionDescriptor(null, 55));
            }
            
            return actions;
        }
    }

    private static class DataSourceRootActionProvider
            implements ExplorerActionsProvider<DataSourceRoot> {

        public ExplorerActionDescriptor getDefaultAction(Set<DataSourceRoot> root) {
            return null;
        }

        public Set<ExplorerActionDescriptor> getActions(Set<DataSourceRoot> root) {
            Set<ExplorerActionDescriptor> actions =
                    new HashSet<ExplorerActionDescriptor>();
            actions.add(new ExplorerActionDescriptor(AddJMXConnectionAction.getInstance(), 20));
            return actions;
        }
    }
}
