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

package org.graalvm.visualvm.heapviewer.java;

import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.heapviewer.model.DataType;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNode;
import java.util.Iterator;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;

/**
 *
 * @author Jiri Sedlacek
 */
public class ClassNode extends HeapViewerNode {
    
    private final JavaClass jclass;
    
    private String name;
    
    
    public ClassNode(JavaClass jclass) {
        this.jclass = jclass;
    }
    
    
    public JavaClass getJavaClass() {
        return jclass;
    }
    
    public Iterator<Instance> getInstancesIterator() {
        return jclass.getInstancesIterator();
    }
    
    public String getName() {
        if (name == null) name = jclass.getName();
        return name;
    }
    
    public int getInstancesCount() {
        return jclass.getInstancesCount();
    }
    
    public long getOwnSize() {
        return jclass.getAllInstancesSize();
    }
    
    public long getRetainedSize(Heap heap) {
        return DataType.RETAINED_SIZE.valuesAvailable(heap) ?
               jclass.getRetainedSizeByClass() : DataType.RETAINED_SIZE.getNotAvailableValue();
    }
    
    
    public boolean isLeaf() {
        return jclass.getInstancesCount() == 0 ? true : super.isLeaf();
    }
    
    public String toString() {
        return getName();
    }
    
    
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof ClassNode)) return false;
        return jclass.equals(((ClassNode)o).jclass);
    }
    
    public int hashCode() {
        return jclass.hashCode();
    }
    
    
    protected Object getValue(DataType type, Heap heap) {
        if (type == DataType.NAME) return getName();
        if (type == DataType.COUNT) return getInstancesCount();
        if (type == DataType.OWN_SIZE) return getOwnSize();
        if (type == DataType.RETAINED_SIZE) return getRetainedSize(heap);
        
        if (type == DataType.CLASS) return getJavaClass();
        
        if (type == DataType.OBJECT_ID) return getJavaClass().getJavaClassId();
        
        if (type == DataType.INSTANCES_WRAPPER) return new InstancesWrapper.Simple(getJavaClass(), getInstancesCount()) {
            @Override
            public Iterator<Instance> getInstancesIterator() {
                return ClassNode.this.getInstancesIterator();
            }
        };
        
        return super.getValue(type, heap);
    }
    
    
    public ClassNode createCopy() {
        ClassNode copy = new ClassNode(getJavaClass());
        setupCopy(copy);
        return copy;
    }
    
    protected void setupCopy(ClassNode copy) {
        super.setupCopy(copy);
        copy.name = name;
    }
    
}
