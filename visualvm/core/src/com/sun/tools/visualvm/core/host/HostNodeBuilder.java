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
import com.sun.tools.visualvm.core.datasupport.DataFinishedListener;
import com.sun.tools.visualvm.core.explorer.ExplorerModelSupport;
import com.sun.tools.visualvm.core.explorer.ExplorerNodeBuilder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Jiri Sedlacek
 */
class HostNodeBuilder implements ExplorerNodeBuilder<Host> {

    private final Map<Host, HostNode> nodesCache = Collections.synchronizedMap(new HashMap());


    public synchronized HostNode getNodeFor(Host host) {
        HostNode hostNode = nodesCache.get(host);
        if (hostNode == null) hostNode = createHostNode(host);
        return hostNode;
    }


    private HostNode createHostNode(final Host host) {
        boolean isLocalhost = host == Host.LOCALHOST;
        final HostNode hostNode = isLocalhost ? new LocalHostNode() : new HostNode(host);
        final ExplorerModelSupport support = ExplorerModelSupport.sharedInstance();
        if (isLocalhost) {
            support.addRootNode(hostNode);
        } else {
            RemoteHostsNode.NODE.addNode(hostNode);
            support.updateNodeStructure(RemoteHostsNode.NODE);
        }
        nodesCache.put(host, hostNode);
        
        host.notifyWhenFinished(new DataFinishedListener() {
            public void dataFinished(Object dataSource) { removeHostNode(host); }
        });
        
        if (isLocalhost) support.addRootNode(RemoteHostsNode.NODE);
        
        return hostNode;
    }
    
    private void removeHostNode(Host host) {
        HostNode node = nodesCache.get(host);
        nodesCache.remove(host);
        ExplorerModelSupport.sharedInstance().removeNode(node);         
    }
    
    
    void initialize() {
        ExplorerModelSupport.sharedInstance().addBuilder(this, Host.class);
    }

}
