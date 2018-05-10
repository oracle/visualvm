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
package com.sun.tools.visualvm.heapviewer.truffle.nodes;

import org.netbeans.lib.profiler.heap.ArrayItemValue;
import org.netbeans.lib.profiler.heap.ObjectFieldValue;
import org.netbeans.lib.profiler.heap.Value;
import com.sun.tools.visualvm.heapviewer.java.InstanceNode;
import com.sun.tools.visualvm.heapviewer.java.InstanceReferenceNode;

/**
 *
 * @author Jiri Sedlacek
 */
public final class TerminalJavaNodes {
    
    public static class Instance extends InstanceNode {
        
        public Instance(org.netbeans.lib.profiler.heap.Instance instance) {
            super(instance);
            setChildren(NO_NODES);
        }
        
        public boolean isLeaf() {
            return true;
        }
        
    }
    
    public static class Field extends InstanceReferenceNode.Field {
        
        private final boolean isArray;
        
        public Field(ObjectFieldValue value, boolean incoming) {
            super(value, incoming);
            
            org.netbeans.lib.profiler.heap.Instance instance = getInstance();
            isArray = instance != null && instance.getJavaClass().isArray();
            
            if (!isArray) setChildren(NO_NODES);
        }
        
        public boolean isLeaf() {
            return isArray ? super.isLeaf() : true;
        }
        
    }
    
    public static class ArrayItem extends InstanceReferenceNode.ArrayItem {

        public ArrayItem(ArrayItemValue value, boolean incoming) {
            super(value, incoming);
            setChildren(NO_NODES);
        }
        
        public boolean isLeaf() {
            return true;
        }
        
    }
    
    
    public static InstanceReferenceNode outgoingReference(Value value) {
        return reference(value, false);
    }
    
    public static InstanceReferenceNode incomingReference(Value value) {
        return reference(value, true);
    }
    
    public static InstanceReferenceNode reference(Value value, boolean incoming) {
        if (value instanceof ObjectFieldValue) return new Field((ObjectFieldValue)value, incoming);
        else if (value instanceof ArrayItemValue) return new ArrayItem((ArrayItemValue)value, incoming);
        
        return null;
    }
    
    
    private TerminalJavaNodes() {}
    
}
