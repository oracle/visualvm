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

package com.sun.tools.visualvm.core.threaddump;

import com.sun.tools.visualvm.core.explorer.ThreadDumpNode;
import com.sun.tools.visualvm.core.datasource.ThreadDump;
import com.sun.tools.visualvm.core.datasupport.DataFinishedListener;
import com.sun.tools.visualvm.core.explorer.ExplorerModelSupport;
import com.sun.tools.visualvm.core.explorer.ExplorerNode;
import com.sun.tools.visualvm.core.explorer.ExplorerNodeBuilder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Jiri Sedlacek
 */
class ThreadDumpNodeBuilder implements ExplorerNodeBuilder<ThreadDump> {

    private final Map<ThreadDump, ThreadDumpNode> nodesCache = Collections.synchronizedMap(new HashMap());


    public synchronized ThreadDumpNode getNodeFor(ThreadDump threadDump) {
        ThreadDumpNode threadDumpNode = nodesCache.get(threadDump);
        if (threadDumpNode == null) threadDumpNode = createThreadDumpNode(threadDump);
        return threadDumpNode;
    }


    private ThreadDumpNode createThreadDumpNode(final ThreadDump threadDump) {
        ThreadDumpNode node = null;
        
        if (threadDump.getOwner() != null) {
            ExplorerModelSupport support = ExplorerModelSupport.sharedInstance();
            ExplorerNode ownerNode = support.getNodeFor(threadDump.getOwner());
            if (ownerNode != null) {
                node = new ThreadDumpNode(threadDump);
                support.addNode(node, ownerNode);
                nodesCache.put(threadDump, node);
                threadDump.notifyWhenFinished(new DataFinishedListener() {
                    public void dataFinished(Object dataSource) { removeThreadDumpNode(threadDump); }
                });
            }
            
        }
        
        return node;
    }
    
    private void removeThreadDumpNode(ThreadDump threadDump) {
        ThreadDumpNode node = nodesCache.get(threadDump);
        nodesCache.remove(threadDump);
        ExplorerModelSupport.sharedInstance().removeNode(node);         
    }
    
    
    void initialize() {
        ExplorerModelSupport.sharedInstance().addBuilder(this, ThreadDump.class);
    }

}
