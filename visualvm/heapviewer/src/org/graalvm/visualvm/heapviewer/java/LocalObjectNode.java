/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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

import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.heapviewer.model.DataType;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "LocalObjectNode_LocalObject=local object",
    "LocalObjectNode_UnknownLocalObject=unknown local object",
    "LocalObjectNode_Unknown=<unknown>"
})
public class LocalObjectNode extends InstanceNode {
    
    private final String localObjectName;
    
    
    public LocalObjectNode(Instance instance) {
        this(instance, Bundle.LocalObjectNode_LocalObject());
    }
    
    public LocalObjectNode(Instance instance, String localObjectName) {
        super(instance);
        this.localObjectName = localObjectName;
    }
    
    
    public String getLocalObjectName() {
        return localObjectName;
    }
    
    
    public static class Unknown extends LocalObjectNode {
        
        public Unknown() {
            super(null, Bundle.LocalObjectNode_UnknownLocalObject());
        }
        
        public String getName(Heap heap) {
            return Bundle.LocalObjectNode_Unknown();
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

            return super.getValue(type, heap);
        }
        
    }
    
}
