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
package com.sun.tools.visualvm.truffle.heapwalker.ruby;

import java.util.List;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.ObjectFieldValue;
import org.netbeans.lib.profiler.ui.Formatters;
import org.netbeans.modules.profiler.heapwalk.details.spi.DetailsUtils;
import com.sun.tools.visualvm.truffle.heapwalker.DynamicObject;
import com.sun.tools.visualvm.truffle.heapwalker.DynamicObjectArrayItemNode;
import com.sun.tools.visualvm.truffle.heapwalker.DynamicObjectFieldNode;
import com.sun.tools.visualvm.truffle.heapwalker.DynamicObjectNode;
import com.sun.tools.visualvm.truffle.heapwalker.DynamicObjectReferenceNode;
import com.sun.tools.visualvm.truffle.heapwalker.DynamicObjectsContainer;
import com.sun.tools.visualvm.truffle.heapwalker.LocalDynamicObjectNode;
import org.netbeans.lib.profiler.heap.ArrayItemValue;

/**
 *
 * @author Jiri Sedlacek
 */
class RubyNodes {
    
    
    static String getLogicalValue(DynamicObject dobject, String type, Heap heap) {
        String logicalValue = null;

        if ("ProcType".equals(type)) {
            FieldValue infoField = dobject.getFieldValue("sharedMethodInfo (hidden)");
            Instance info = infoField instanceof ObjectFieldValue ? ((ObjectFieldValue)infoField).getInstance() : null;
            if (info != null) {
                String name = DetailsUtils.getInstanceFieldString(info, "name", heap);
                String notes = DetailsUtils.getInstanceFieldString(info, "notes", heap);
                
                if (name != null && notes != null) logicalValue = name + " (" + notes + ")";
                else if (name != null) logicalValue = name;
                else if (notes != null) logicalValue = notes;
            }
        } else if ("MethodType".equals(type) || "UnboundMethodType".equals(type)) {
            FieldValue methodField = dobject.getFieldValue("method (hidden)");
            Instance method = methodField instanceof ObjectFieldValue ? ((ObjectFieldValue)methodField).getInstance() : null;
            
            Object infoField = method == null ? null : method.getValueOfField("sharedMethodInfo");
            Instance info = infoField instanceof Instance ? (Instance)infoField : null;
            
            if (info != null) {
                String name = DetailsUtils.getInstanceFieldString(info, "name", heap);
                String notes = DetailsUtils.getInstanceFieldString(info, "notes", heap);
                
                if (name != null && notes != null) logicalValue = name + " (" + notes + ")";
                else if (name != null) logicalValue = name;
                else if (notes != null) logicalValue = notes;
            }
        } else if ("SymbolType".equals(type)) {
            FieldValue symbolField = dobject.getFieldValue("string (hidden)");
            Instance symbol = symbolField instanceof ObjectFieldValue ? ((ObjectFieldValue)symbolField).getInstance() : null;
            
            if (symbol != null) logicalValue = DetailsUtils.getInstanceString(symbol, heap);
        } else if ("ClassType".equals(type) || "ModuleType".equals(type)) {
            FieldValue fieldsField = dobject.getFieldValue("fields (hidden)");
            Instance fields = fieldsField instanceof ObjectFieldValue ? ((ObjectFieldValue)fieldsField).getInstance() : null;
            
            Object nameField = fields == null ? null : fields.getValueOfField("name");
            Instance name = nameField instanceof Instance ? (Instance)nameField : null;
            
            if (name != null) logicalValue = DetailsUtils.getInstanceString(name, heap);
        } else if ("BasicObjectType".equals(type)) {
            String head = "fields [";
            String sep = ", ";
            
            StringBuilder sb = new StringBuilder();
            sb.append(head);
            
            List<FieldValue> fields = dobject.getFieldValues();
            for (FieldValue field : fields) {
                String name = field.getField().getName();
                if (!name.contains("(hidden)")) sb.append(name).append(sep);
            }
            
            int length = sb.length();
            if (length > head.length()) sb.delete(length - sep.length(), length);
            sb.append("]");
            
            logicalValue = sb.toString();
        } else if ("ArrayType".equals(type)) {
            FieldValue sizeField = dobject.getFieldValue("size (hidden)");
            if (sizeField != null) {
                Integer size = Integer.parseInt(sizeField.getValue());
                logicalValue = Formatters.numberFormat().format(size) + (size == 1 ? " item" : " items");
            }
        }

        return logicalValue;
    }
    
    
    static class RubyDynamicObjectNode extends DynamicObjectNode implements RubyNode {
        
        RubyDynamicObjectNode(DynamicObject dobject, String type) {
            super(dobject, type);
        }
        
        
        protected String computeLogicalValue(DynamicObject dobject, String type, Heap heap) {
            String logicalValue = RubyNodes.getLogicalValue(dobject, type, heap);
            return logicalValue != null ? logicalValue : super.computeLogicalValue(dobject, type, heap);
        }
        
        
        public RubyDynamicObjectNode createCopy() {
            RubyDynamicObjectNode copy = new RubyDynamicObjectNode(getDynamicObject(), getType());
            setupCopy(copy);
            return copy;
        }

        protected void setupCopy(RubyDynamicObjectNode copy) {
            super.setupCopy(copy);
        }
        
    }
    
    static class RubyDynamicObjectsContainer extends DynamicObjectsContainer implements RubyNode {
        
        RubyDynamicObjectsContainer(String name) {
            super(name);
        }

        RubyDynamicObjectsContainer(String name, int maxObjects) {
            super(name, maxObjects);
        }
        
        
        protected RubyDynamicObjectNode createNode(DynamicObject dobject) {
            return new RubyDynamicObjectNode(dobject, name);
        }
        
        
        public RubyDynamicObjectsContainer createCopy() {
            RubyDynamicObjectsContainer copy = new RubyDynamicObjectsContainer(name, maxNodes);
            setupCopy(copy);
            return copy;
        }

        protected void setupCopy(RubyDynamicObjectNode copy) {
            super.setupCopy(copy);
        }
        
    }
    
    static class RubyDynamicObjectFieldNode extends DynamicObjectFieldNode implements RubyNode {
        
        RubyDynamicObjectFieldNode(DynamicObject dobject, String type, FieldValue field) {
            super(dobject, type, field);
        }
        
        protected String computeLogicalValue(DynamicObject dobject, String type, Heap heap) {
            String logicalValue = RubyNodes.getLogicalValue(dobject, type, heap);
            return logicalValue != null ? logicalValue : super.computeLogicalValue(dobject, type, heap);
        }
        
        
        public RubyDynamicObjectNode createCopy() {
            return new RubyDynamicObjectNode(getDynamicObject(), getType());
        }
        
    }
    
    static class RubyDynamicObjectArrayItemNode extends DynamicObjectArrayItemNode implements RubyNode {
        
        RubyDynamicObjectArrayItemNode(DynamicObject dobject, String type, ArrayItemValue item) {
            super(dobject, type, item);
        }
        
        protected String computeLogicalValue(DynamicObject dobject, String type, Heap heap) {
            String logicalValue = RubyNodes.getLogicalValue(dobject, type, heap);
            return logicalValue != null ? logicalValue : super.computeLogicalValue(dobject, type, heap);
        }
        
        
        public RubyDynamicObjectNode createCopy() {
            return new RubyDynamicObjectNode(getDynamicObject(), getType());
        }
        
    }
    
    static class RubyDynamicObjectReferenceNode extends DynamicObjectReferenceNode implements RubyNode {
        
        RubyDynamicObjectReferenceNode(DynamicObject dobject, String type, FieldValue value) {
            super(dobject, type, value);
        }
        
        protected String computeLogicalValue(DynamicObject dobject, String type, Heap heap) {
            String logicalValue = RubyNodes.getLogicalValue(dobject, type, heap);
            return logicalValue != null ? logicalValue : super.computeLogicalValue(dobject, type, heap);
        }
        
        
        public RubyDynamicObjectNode createCopy() {
            return new RubyDynamicObjectNode(getDynamicObject(), getType());
        }
        
    }
    
    static class RubyLocalDynamicObjectNode extends LocalDynamicObjectNode implements RubyNode {
        
        RubyLocalDynamicObjectNode(DynamicObject dobject, String type) {
            super(dobject, type);
        }
        
        protected String computeLogicalValue(DynamicObject dobject, String type, Heap heap) {
            String logicalValue = RubyNodes.getLogicalValue(dobject, type, heap);
            return logicalValue != null ? logicalValue : super.computeLogicalValue(dobject, type, heap);
        }
        
        
        public RubyDynamicObjectNode createCopy() {
            return new RubyDynamicObjectNode(getDynamicObject(), getType());
        }
        
    }
    
    
    static interface RubyNode {}
    
}
