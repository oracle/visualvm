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

import com.sun.tools.visualvm.core.explorer.HeapDumpNode;
import com.sun.tools.visualvm.core.datasource.HeapDump;
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
class HeapDumpNodeBuilder implements ExplorerNodeBuilder<HeapDump> {

    private final Map<HeapDump, HeapDumpNode> nodesCache = Collections.synchronizedMap(new HashMap());


    public synchronized HeapDumpNode getNodeFor(HeapDump heapDump) {
        HeapDumpNode heapDumpNode = nodesCache.get(heapDump);
        if (heapDumpNode == null) heapDumpNode = createHeapDumpNode(heapDump);
        return heapDumpNode;
    }


    private HeapDumpNode createHeapDumpNode(final HeapDump heapDump) {
        HeapDumpNode node = null;
        
        if (heapDump.getOwner() != null) {
            ExplorerModelSupport support = ExplorerModelSupport.sharedInstance();
            ExplorerNode ownerNode = support.getNodeFor(heapDump.getOwner());
            if (ownerNode != null) {
                node = new HeapDumpNode(heapDump);
                support.addNode(node, ownerNode);
                nodesCache.put(heapDump, node);
                heapDump.notifyWhenFinished(new DataFinishedListener() {
                    public void dataFinished(Object dataSource) { removeHeapDumpNode(heapDump); }
                });
            }
            
        }
        
        return node;
    }
    
    private void removeHeapDumpNode(HeapDump heapDump) {
        HeapDumpNode node = nodesCache.get(heapDump);
        nodesCache.remove(heapDump);
        ExplorerModelSupport.sharedInstance().removeNode(node);         
    }
    
    
    void initialize() {
        ExplorerModelSupport.sharedInstance().addBuilder(this, HeapDump.class);
    }

}
