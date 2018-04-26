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

package com.sun.tools.visualvm.heapviewer.java;

import org.netbeans.lib.profiler.heap.GCRoot;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.modules.profiler.heapwalk.details.api.DetailsSupport;
import com.sun.tools.visualvm.heapviewer.model.DataType;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNode;
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
        return getName(null); // TODO: should not be called directly when sorting the tree
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
    
    public String getName(Heap heap) {
        if (name == null) {
            if (heap == null) {
                return computeName(instance, (GCRoot)null);
            } else {
                GCRoot gcRoot = heap.getGCRoot(instance);
                isGCRoot = gcRoot != null;
                name = computeName(instance, gcRoot);
            }
        }
        return name;
    }
    
    public String getLogicalValue(Heap heap) {
        if (logicalValue == null) logicalValue = computeLogicalValue(instance, heap);
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
    
    
    static String computeName(Instance instance, Heap heap) {
        GCRoot gcroot = heap == null ? null : heap.getGCRoot(instance);
        return computeName(instance, gcroot);
    }
    
    private static String computeName(Instance instance, GCRoot gcroot) {
        String name = instance.getJavaClass().getName() + "#" + instance.getInstanceNumber(); // NOI18N
        if (gcroot != null) name = Bundle.InstanceNode_GCRootFlag(name, gcroot.getKind());
        return name;
    }
    
    static String computeLogicalValue(Instance instance, Heap heap) {
        String detail = DetailsSupport.getDetailsString(instance, heap);
        return detail == null ? "" : detail; // NOI18N
    }
    
    
    protected Object getValue(DataType type, Heap heap) {
        if (type == DataType.NAME) return getName(heap);
        if (type == DataType.OWN_SIZE) return getOwnSize();
        if (type == DataType.RETAINED_SIZE) return getRetainedSize(heap);
        
        if (type == DataType.INSTANCE) return getInstance();
        if (type == DataType.CLASS) return getJavaClass();
        
        if (type == DataType.LOGICAL_VALUE) return getLogicalValue(heap);
        
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
    
}
