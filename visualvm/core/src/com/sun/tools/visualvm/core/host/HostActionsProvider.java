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

package com.sun.tools.visualvm.core.host;

import com.sun.tools.visualvm.core.explorer.HostNode;
import com.sun.tools.visualvm.core.datasource.Host;
import com.sun.tools.visualvm.core.explorer.ExplorerActionDescriptor;
import com.sun.tools.visualvm.core.explorer.ExplorerActionsProvider;
import com.sun.tools.visualvm.core.explorer.ExplorerContextMenuFactory;
import com.sun.tools.visualvm.core.explorer.ExplorerRoot;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.swing.AbstractAction;

/**
 *
 * @author Jiri Sedlacek
 */
class HostActionsProvider {

    private final AddNewHostAction addNewHostAction = new AddNewHostAction();


    void initialize() {
        ExplorerContextMenuFactory.sharedInstance().addExplorerActionsProvider(new HostNodeActionProvider(), HostNode.class);
        ExplorerContextMenuFactory.sharedInstance().addExplorerActionsProvider(new HostsNodeActionProvider(), RemoteHostsNode.class);
        ExplorerContextMenuFactory.sharedInstance().addExplorerActionsProvider(new RootNodeActionProvider(), ExplorerRoot.class);
    }    
    
    
    private class AddNewHostAction extends AbstractAction {
        
        public AddNewHostAction() {
            super("Add Remote Host...");
        }
        
        public void actionPerformed(ActionEvent e) {
            HostDescriptor hostDescriptor = HostUtils.defineHost();
            if (hostDescriptor != null) HostsSupport.getInstance().getHostProvider().createHost(hostDescriptor, true);
        }
        
    }
    
    private class RenameHostAction extends AbstractAction {
        
        private Host host;
        
        public RenameHostAction(Host host) {
            super("Rename...");
            this.host = host;
        }
        
        public void actionPerformed(ActionEvent e) {
            HostDescriptor hostDescriptor = HostUtils.renameHost(host);
            if (hostDescriptor != null) host.setDisplayName(hostDescriptor.getDisplayName());
        }
        
    }
    
    private class RemoveHostAction extends AbstractAction {
        
        private HostImpl host;
        
        public RemoveHostAction(HostImpl host) {
            super("Remove");
            this.host = host;
        }
        
        public void actionPerformed(ActionEvent e) {
            HostsSupport.getInstance().getHostProvider().removeHost(host, true);
        }
        
    }
    
    
    private class HostNodeActionProvider implements ExplorerActionsProvider<HostNode> {
        
        public ExplorerActionDescriptor getDefaultAction(HostNode hostNode) {
            return null;
        }

        public List<ExplorerActionDescriptor> getActions(HostNode hostNode) {
            Host host = hostNode.getHost();
            if (host == null || host == Host.LOCALHOST) return Collections.EMPTY_LIST;
            
            List<ExplorerActionDescriptor> actions = new LinkedList();

            actions.add(new ExplorerActionDescriptor(new RenameHostAction(host), 10));
            
            if (host instanceof HostImpl) actions.add(new ExplorerActionDescriptor(new RemoveHostAction((HostImpl)host), 20));

            return actions;
        }
    }
    
    private class HostsNodeActionProvider implements ExplorerActionsProvider<RemoteHostsNode> {

        public ExplorerActionDescriptor getDefaultAction(RemoteHostsNode node) { return null; }

        public List<ExplorerActionDescriptor> getActions(RemoteHostsNode node) {
            List<ExplorerActionDescriptor> actions = new LinkedList();
            
            actions.add(new ExplorerActionDescriptor(addNewHostAction, 0));
            
            return actions;
        }
        
    }
    
    private class RootNodeActionProvider implements ExplorerActionsProvider<ExplorerRoot> {

        public ExplorerActionDescriptor getDefaultAction(ExplorerRoot node) { return null; }

        public List<ExplorerActionDescriptor> getActions(ExplorerRoot node) {
            List<ExplorerActionDescriptor> actions = new LinkedList();
            
            actions.add(new ExplorerActionDescriptor(addNewHostAction, 10));
            
            return actions;
        }
        
    }

}
