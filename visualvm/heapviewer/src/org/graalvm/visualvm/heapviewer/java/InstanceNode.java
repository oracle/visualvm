/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.graalvm.visualvm.lib.jfluid.heap.GCRoot;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.api.DetailsSupport;
import org.graalvm.visualvm.heapviewer.model.DataType;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNode;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "InstanceNode_GCRootFlag={0} [GC root - {1}]"
})
public class InstanceNode extends HeapViewerNode {
    
    public static enum Mode {
        NONE,
        OUTGOING_REFERENCE,
        INCOMING_REFERENCE
    }
    
    
    private final Instance instance;
    
    private String name;
    private String logicalValue;
    
    // Internal-only flag initialized during computeName()
    private boolean isGCRoot;
    
    
    public InstanceNode(Instance instance) {
        this.instance = instance;
    }
    
    
    public String toString() {
        return getName(); // TODO: should not be called directly when sorting the tree
    }
    
    
    public Mode getMode() {
        return Mode.NONE;
    }
    
    
    public Instance getInstance() {
        return instance;
    }
    
    public JavaClass getJavaClass() {
        return instance.getJavaClass();
    }
    
    public String getName() {
        if (name == null) {
            Heap heap = instance.getJavaClass().getHeap();
            Collection<GCRoot> gcRoots = heap.getGCRoots(instance);
            isGCRoot = !gcRoots.isEmpty();
            name = computeName(heap, instance, gcRoots);
        }
        return name;
    }
    
    public String getLogicalValue() {
        if (logicalValue == null) logicalValue = computeLogicalValue(instance);
        return logicalValue;
    }
    
    public long getOwnSize() {
        return instance.getSize();
    }
    
    public long getRetainedSize(Heap heap) {
        return DataType.RETAINED_SIZE.valuesAvailable(heap) ?
               instance.getRetainedSize() : DataType.RETAINED_SIZE.getNotAvailableValue();
    }
    
    
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof InstanceNode)) return false;
        return instance.equals(((InstanceNode)o).instance);
    }
    
    public int hashCode() {
        return instance.hashCode();
    }
    
    
    boolean isGCRoot() {
        return isGCRoot;
    }
    
    
    static String computeName(Instance instance) {
        Heap heap = instance.getJavaClass().getHeap();
        Collection<GCRoot> gcroots = heap == null ? Collections.EMPTY_LIST : heap.getGCRoots(instance);
        return computeName(heap, instance, gcroots);
    }
    
    private static String computeName(Heap heap, Instance instance, Collection<GCRoot> gcroots) {
        String name = null;
        String className = instance.getJavaClass().getName();
        if (heap != null && Class.class.getName().equals(className)) {
            JavaClass jcls = heap.getJavaClassByID(instance.getInstanceId());
            if (jcls != null) {
                name = "class "+jcls.getName();     // NOI18N
            }
        }
        if (name == null) {
            name = className + "#" + instance.getInstanceNumber(); // NOI18N
        }
        if (!gcroots.isEmpty()) {
            Set<String> gcKinds = new HashSet();

            for (GCRoot gcroot : gcroots) {
                gcKinds.add(gcroot.getKind());
            }
            String kind = String.join(", ", gcKinds);       // NOI18N
            name = Bundle.InstanceNode_GCRootFlag(name, kind);
        }
        return name;
    }
    
    static String computeLogicalValue(Instance instance) {
        String detail = DetailsSupport.getDetailsString(instance);
        return detail == null ? "" : detail; // NOI18N
    }
    
    
    protected Object getValue(DataType type, Heap heap) {
        if (type == DataType.NAME) return getName();
        if (type == DataType.OWN_SIZE) return getOwnSize();
        if (type == DataType.RETAINED_SIZE) return getRetainedSize(heap);
        
        if (type == DataType.INSTANCE) return getInstance();
        if (type == DataType.CLASS) return getJavaClass();
        
        if (type == DataType.LOGICAL_VALUE) return getLogicalValue();
        
        if (type == DataType.OBJECT_ID) {
            Instance i = getInstance();
            return i == null ? DataType.OBJECT_ID.getNoValue() : i.getInstanceId();
        }
        
        return super.getValue(type, heap);
    }
    
    
    public InstanceNode createCopy() {
        if (instance == null) return null;
        
        InstanceNode copy = new InstanceNode(instance);
        setupCopy(copy);
        return copy;
    }
    
    protected void setupCopy(InstanceNode copy) {
        super.setupCopy(copy);
        copy.name = name;
        copy.logicalValue = logicalValue;
    }
    
    
    public static class IncludingNull extends InstanceNode {
        
        public IncludingNull(Instance instance) {
            super(instance);
        }
        
        public JavaClass getJavaClass() {
            if (getInstance() == null) return null;
            else return super.getJavaClass();
        }
        
        public String getName() {
            if (getInstance() == null) return "null"; // NOI18N
            else return super.getName();
        }

        public String getLogicalValue() {
            if (getInstance() == null) return DataType.LOGICAL_VALUE.getNoValue();
            else return super.getLogicalValue();
        }
        
        public long getOwnSize() {
            if (getInstance() == null) return DataType.OWN_SIZE.getNoValue();
            else return super.getOwnSize();
        }

        public long getRetainedSize(Heap heap) {
            if (getInstance() == null) return DataType.RETAINED_SIZE.valuesAvailable(heap) ?
                                       DataType.RETAINED_SIZE.getNoValue() : DataType.RETAINED_SIZE.getNotAvailableValue();
            else return super.getRetainedSize(heap);
        }
        
        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof InstanceNode)) return false;
            return Objects.equals(getInstance(), ((InstanceNode)o).getInstance());
        }

        public int hashCode() {
            return getInstance() == null ? 37 : super.hashCode();
        }
        
        public boolean isLeaf() {
            return getInstance() == null ? true : super.isLeaf();
        }
        
    }
    
}
