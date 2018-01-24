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
package com.sun.tools.visualvm.heapviewer.truffle.javascript;

import java.util.List;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.ObjectFieldValue;
import org.netbeans.lib.profiler.ui.Formatters;
import org.netbeans.modules.profiler.heapwalk.details.api.DetailsSupport;
import com.sun.tools.visualvm.heapviewer.truffle.DynamicObject;
import com.sun.tools.visualvm.heapviewer.truffle.DynamicObjectArrayItemNode;
import com.sun.tools.visualvm.heapviewer.truffle.DynamicObjectFieldNode;
import com.sun.tools.visualvm.heapviewer.truffle.DynamicObjectNode;
import com.sun.tools.visualvm.heapviewer.truffle.DynamicObjectReferenceNode;
import com.sun.tools.visualvm.heapviewer.truffle.DynamicObjectsContainer;
import com.sun.tools.visualvm.heapviewer.truffle.LocalDynamicObjectNode;
import java.util.Date;
import org.netbeans.lib.profiler.heap.ArrayItemValue;

/**
 *
 * @author Jiri Sedlacek
 */
class JavaScriptNodes {
    
    
    static String getLogicalValue(DynamicObject dobject, String type, Heap heap) {
        String logicalValue = null;

        if ("JSFunction".equals(type)) {
            FieldValue dataField = dobject.getFieldValue("functionData (hidden)");
            Instance data = dataField instanceof ObjectFieldValue ? ((ObjectFieldValue)dataField).getInstance() : null;
//                Instance data = (Instance)getInstance().getValueOfField("object2");
            logicalValue = data == null ? null : DetailsSupport.getDetailsString(data, heap);
            if (logicalValue != null) logicalValue += "()";
        } else if ("JavaPackage".equals(type)) {
            FieldValue nameField = dobject.getFieldValue("packageName (hidden)");
            Instance name = nameField instanceof ObjectFieldValue ? ((ObjectFieldValue)nameField).getInstance() : null;
//                Instance name = (Instance)getInstance().getValueOfField("object2");
            logicalValue = name == null ? null : DetailsSupport.getDetailsString(name, heap);
        } else if ("JSUserObject".equals(type)) {
            String head = "properties [";
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
        } else if ("JSArray".equals(type)) {
            FieldValue lengthField = dobject.getFieldValue("length (hidden)");
            if (lengthField == null) {
                lengthField = dobject.getFieldValue("usedLength (hidden)");
            }
            if (lengthField != null) {
                Integer length = Integer.parseInt(lengthField.getValue());
                logicalValue = Formatters.numberFormat().format(length) + (length == 1 ? " item" : " items");
            }
        } else if ("Null$NullClass".equals(type)) {
            return DetailsSupport.getDetailsString(dobject.getInstance(), heap);
        } else if ("JSDate".equals(type)) {
            FieldValue timeField = dobject.getFieldValue("timeMillis (hidden)");
            if (timeField != null) {
                double time = Double.parseDouble(timeField.getValue());

                return new Date((long)time).toString();
            }
        }

        return logicalValue;
    }
    
    
    static class JavaScriptDynamicObjectNode extends DynamicObjectNode implements JavaScriptNode {
        
        JavaScriptDynamicObjectNode(DynamicObject dobject, String type) {
            super(dobject, type);
        }
        
        
        protected String computeLogicalValue(DynamicObject dobject, String type, Heap heap) {
            String logicalValue = JavaScriptNodes.getLogicalValue(dobject, type, heap);
            return logicalValue != null ? logicalValue : super.computeLogicalValue(dobject, type, heap);
        }
        
        
        public JavaScriptDynamicObjectNode createCopy() {
            JavaScriptDynamicObjectNode copy = new JavaScriptDynamicObjectNode(getDynamicObject(), getType());
            setupCopy(copy);
            return copy;
        }

        protected void setupCopy(JavaScriptDynamicObjectNode copy) {
            super.setupCopy(copy);
        }
        
    }
    
    static class JavaScriptDynamicObjectsContainer extends DynamicObjectsContainer implements JavaScriptNode {
        
        JavaScriptDynamicObjectsContainer(String name) {
            super(name);
        }

        JavaScriptDynamicObjectsContainer(String name, int maxObjects) {
            super(name, maxObjects);
        }
        
        protected JavaScriptDynamicObjectNode createNode(Instance instance) {
            return new JavaScriptDynamicObjectNode(new DynamicObject(instance), name);
        }
        
        
        public JavaScriptDynamicObjectsContainer createCopy() {
            JavaScriptDynamicObjectsContainer copy = new JavaScriptDynamicObjectsContainer(name, maxNodes);
            setupCopy(copy);
            return copy;
        }

        protected void setupCopy(JavaScriptDynamicObjectsContainer copy) {
            super.setupCopy(copy);
        }

    }
    
    static class JavaScriptDynamicObjectFieldNode extends DynamicObjectFieldNode implements JavaScriptNode {
        
        JavaScriptDynamicObjectFieldNode(DynamicObject dobject, String type, FieldValue field) {
            super(dobject, type, field);
        }
        
        protected String computeLogicalValue(DynamicObject dobject, String type, Heap heap) {
            String logicalValue = JavaScriptNodes.getLogicalValue(dobject, type, heap);
            return logicalValue != null ? logicalValue : super.computeLogicalValue(dobject, type, heap);
        }
        
        
        public JavaScriptDynamicObjectNode createCopy() {
            return new JavaScriptDynamicObjectNode(getDynamicObject(), getType());
        }
        
    }
    
    static class JavaScriptDynamicObjectArrayItemNode extends DynamicObjectArrayItemNode implements JavaScriptNode {
        
        JavaScriptDynamicObjectArrayItemNode(DynamicObject dobject, String type, ArrayItemValue item) {
            super(dobject, type, item);
        }
        
        protected String computeLogicalValue(DynamicObject dobject, String type, Heap heap) {
            String logicalValue = JavaScriptNodes.getLogicalValue(dobject, type, heap);
            return logicalValue != null ? logicalValue : super.computeLogicalValue(dobject, type, heap);
        }
        
        
        public JavaScriptDynamicObjectNode createCopy() {
            return new JavaScriptDynamicObjectNode(getDynamicObject(), getType());
        }
        
    }
        
    
    static class JavaScriptDynamicObjectReferenceNode extends DynamicObjectReferenceNode implements JavaScriptNode {
        
        JavaScriptDynamicObjectReferenceNode(DynamicObject dobject, String type, FieldValue value) {
            super(dobject, type, value);
        }
        
        protected String computeLogicalValue(DynamicObject dobject, String type, Heap heap) {
            String logicalValue = JavaScriptNodes.getLogicalValue(dobject, type, heap);
            return logicalValue != null ? logicalValue : super.computeLogicalValue(dobject, type, heap);
        }
        
        
        public JavaScriptDynamicObjectNode createCopy() {
            return new JavaScriptDynamicObjectNode(getDynamicObject(), getType()); 
        }
        
    }
    
    static class JavaScriptLocalDynamicObjectNode extends LocalDynamicObjectNode implements JavaScriptNode {
        
        JavaScriptLocalDynamicObjectNode(DynamicObject dobject, String type) {
            super(dobject, type);
        }
        
        protected String computeLogicalValue(DynamicObject dobject, String type, Heap heap) {
            String logicalValue = JavaScriptNodes.getLogicalValue(dobject, type, heap);
            return logicalValue != null ? logicalValue : super.computeLogicalValue(dobject, type, heap);
        }
        
        
        public JavaScriptDynamicObjectNode createCopy() {
            return new JavaScriptDynamicObjectNode(getDynamicObject(), getType()); 
        }
        
    }
    
    
    static interface JavaScriptNode {}
    
}
