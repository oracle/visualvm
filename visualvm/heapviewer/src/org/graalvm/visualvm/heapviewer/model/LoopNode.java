/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.graalvm.visualvm.heapviewer.model;

import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "LoopNode_Name={0} (loop to {1})"
})
public class LoopNode extends HeapViewerNode {
    
    private final HeapViewerNode loopNode;
    private final HeapViewerNode loopOrigin;

    LoopNode(HeapViewerNode loopNode, HeapViewerNode loopOrigin) {
        this.loopNode = loopNode;
        this.loopOrigin = loopOrigin;
        setChildren(NO_NODES);
    }

    public String toString() {
        return Bundle.LoopNode_Name(loopNode.toString(), loopOrigin.toString());
    }

    protected Object getValue(DataType type, Heap heap) {
        if (type == DataType.LOOP) return loopNode;
        if (type == DataType.LOOP_ORIGIN) return loopOrigin;
        
        return loopNode.getValue(type, heap);
    }
    
}
