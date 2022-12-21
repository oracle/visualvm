/*
 * Copyright (c) 2017, 2022, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.heapviewer.java;

import org.graalvm.visualvm.heapviewer.model.DataType;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.results.CCTNode;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "ThreadNode_UnknownThread=unknown thread"
})
public class ThreadNode extends InstanceNode implements CCTNode.DoNotSortChildren {
    
    private final String name;
    private final boolean isOOME;
    
    
    public ThreadNode(String name, Instance instance) {
        this(name, false, instance);
    }
    
    public ThreadNode(String name, boolean isOOME, Instance instance) {
        super(instance);
        this.name = name;
        this.isOOME = isOOME;
    }
    
    
    public String getName(Heap heap) {
        return name;
    }
    
    public boolean isOOMEThread() {
        return isOOME;
    }
    
    
    public static class Unknown extends ThreadNode {
        
        public Unknown() {
            super(Bundle.ThreadNode_UnknownThread(), null);
        }
        
        public boolean equals(Object o) {
            return o == this;
        }

        public int hashCode() {
            return System.identityHashCode(this);
        }
        
        protected Object getValue(DataType type, Heap heap) {
            if (type == DataType.OWN_SIZE) return DataType.OWN_SIZE.getNoValue();
            if (type == DataType.RETAINED_SIZE) return DataType.RETAINED_SIZE.getNoValue();

            if (type == DataType.INSTANCE) return DataType.INSTANCE.getNoValue();
            if (type == DataType.CLASS) return DataType.CLASS.getNoValue();

            if (type == DataType.LOGICAL_VALUE) return DataType.LOGICAL_VALUE.getNoValue();
            if (type == DataType.NAME) return DataType.NAME.getNoValue();

            return super.getValue(type, heap);
        }
        
    }
    
}
