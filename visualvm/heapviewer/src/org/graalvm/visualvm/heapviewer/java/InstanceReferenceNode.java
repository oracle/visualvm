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

import org.graalvm.visualvm.lib.jfluid.heap.ArrayItemValue;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.lib.jfluid.heap.ObjectFieldValue;
import org.graalvm.visualvm.lib.jfluid.heap.Value;
import org.graalvm.visualvm.heapviewer.model.DataType;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "InstanceReferenceNode_NodeNameField={0} {1}",
    "InstanceReferenceNode_NodeNameReference={0} in {1}"
})
public abstract class InstanceReferenceNode extends InstanceNode {
    
    private final Mode mode;
    private final Value value;
    private String fieldName;

    
    public InstanceReferenceNode(Value value, Instance instance, boolean incoming) {
        super(instance);
        this.value = value;
        this.mode = incoming ? Mode.INCOMING_REFERENCE : Mode.OUTGOING_REFERENCE;
    }
    
    
    public static InstanceReferenceNode outgoing(Value value) {
        return reference(value, false);
    }
    
    public static InstanceReferenceNode incoming(Value value) {
        return reference(value, true);
    }
    
    public static InstanceReferenceNode reference(Value value, boolean incoming) {
        if (value instanceof ObjectFieldValue) return new Field((ObjectFieldValue)value, incoming);
        else if (value instanceof ArrayItemValue) return new ArrayItem((ArrayItemValue)value, incoming);
        
        return null;
    }

    
    public Mode getMode() {
        return mode;
    }

    public Value getValue() {
        return value;
    }

    public String getFieldName() {
        if (fieldName == null) {
            fieldName = computeFieldName();
        }
        return fieldName;
    }

    protected abstract String computeFieldName();

    public JavaClass getJavaClass() {
        return getInstance() == null ? null : super.getJavaClass();
    }

    public String getName(Heap heap) {
        return getInstance() == null ? "null" : super.getName(heap); // NOI18N
    }

    public String getLogicalValue(Heap heap) {
        return getInstance() == null ? "" : super.getLogicalValue(heap); // NOI18N
    }

    public long getOwnSize() {
        return getInstance() == null ? DataType.OWN_SIZE.getNoValue() : super.getOwnSize();
    }

    public long getRetainedSize(Heap heap) {
        return getInstance() == null ? DataType.RETAINED_SIZE.getNoValue() : super.getRetainedSize(heap);
    }

    public boolean isLeaf() {
        return getInstance() == null ? true : super.isLeaf();
    }
    
    public String toString() {
        // TODO: should not be called directly when sorting the tree
        if (Mode.INCOMING_REFERENCE.equals(mode)) return Bundle.InstanceReferenceNode_NodeNameReference(getFieldName(), getName(null));
        else return Bundle.InstanceReferenceNode_NodeNameField(getFieldName(), getName(null));
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof InstanceReferenceNode)) return false;
        InstanceReferenceNode r = (InstanceReferenceNode)o;
        return mode == r.mode && value.equals(r.value);
    }

    public int hashCode() {
        return value.hashCode();
    }
    
    
    public static class Field extends InstanceReferenceNode {
        
        public Field(ObjectFieldValue value, boolean incoming) {
            super(value, incoming ? value.getDefiningInstance() : value.getInstance(), incoming);
        }
        
        public ObjectFieldValue getValue() {
            return (ObjectFieldValue)super.getValue();
        }
        
        protected String computeFieldName() {
            org.graalvm.visualvm.lib.jfluid.heap.Field field = getValue().getField();
            return (field.isStatic() ? "static " : "") + field.getName(); // NOI18N
        }
        
    }
    
    public static class ArrayItem extends InstanceReferenceNode {
        
        public ArrayItem(ArrayItemValue value, boolean incoming) {
            super(value, incoming ? value.getDefiningInstance() : value.getInstance(), incoming);
        } 
        
        public ArrayItemValue getValue() {
            return (ArrayItemValue)super.getValue();
        }
        
        protected String computeFieldName() {
            return "[" + getValue().getIndex() + "]"; // NOI18N
        }
        
    }
    
}
