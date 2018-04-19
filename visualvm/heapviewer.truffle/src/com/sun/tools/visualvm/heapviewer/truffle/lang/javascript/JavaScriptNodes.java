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
package com.sun.tools.visualvm.heapviewer.truffle.lang.javascript;

import com.sun.tools.visualvm.heapviewer.HeapContext;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNode;
import com.sun.tools.visualvm.heapviewer.truffle.nodes.TruffleLocalObjectNode;
import com.sun.tools.visualvm.heapviewer.truffle.nodes.TruffleObjectArrayItemNode;
import com.sun.tools.visualvm.heapviewer.truffle.nodes.TruffleObjectFieldNode;
import java.util.List;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.ObjectFieldValue;
import org.netbeans.lib.profiler.ui.Formatters;
import org.netbeans.modules.profiler.heapwalk.details.api.DetailsSupport;
import com.sun.tools.visualvm.heapviewer.truffle.dynamicobject.DynamicObjectArrayItemNode;
import com.sun.tools.visualvm.heapviewer.truffle.dynamicobject.DynamicObjectFieldNode;
import com.sun.tools.visualvm.heapviewer.truffle.dynamicobject.DynamicObjectNode;
import com.sun.tools.visualvm.heapviewer.truffle.dynamicobject.DynamicObjectReferenceNode;
import com.sun.tools.visualvm.heapviewer.truffle.dynamicobject.LocalDynamicObjectNode;
import com.sun.tools.visualvm.heapviewer.truffle.nodes.TruffleObjectNode;
import com.sun.tools.visualvm.heapviewer.truffle.nodes.TruffleObjectReferenceNode;
import com.sun.tools.visualvm.heapviewer.truffle.nodes.TruffleOpenNodeActionProvider;
import com.sun.tools.visualvm.heapviewer.truffle.nodes.TruffleTypeNode;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerNodeAction;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerRenderer;
import java.util.Date;
import java.util.Map;
import javax.swing.Icon;
import org.netbeans.lib.profiler.heap.ArrayItemValue;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.LanguageIcons;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@ServiceProvider(service=HeapViewerNodeAction.Provider.class)
public class JavaScriptNodes extends TruffleOpenNodeActionProvider<JavaScriptObject, JavaScriptType, JavaScriptHeapFragment, JavaScriptLanguage> {
    
    @Override
    public boolean supportsView(HeapContext context, String viewID) {
        return JavaScriptHeapFragment.isJavaScriptHeap(context);
    }

    @Override
    protected boolean supportsNode(HeapViewerNode node) {
        return node instanceof JavaScriptNodes.JavaScriptNode;
    }

    @Override
    protected JavaScriptLanguage getLanguage() {
        return JavaScriptLanguage.instance();
    }
    
    
    private static final int MAX_LOGVALUE_LENGTH = 160;
        
    static String getLogicalValue(JavaScriptObject object, String type, Heap heap) {
        String logicalValue = null;
        
        if ("Function".equals(type) || "JSFunction".equals(type)) {
            FieldValue dataField = object.getFieldValue("functionData (hidden)");
            Instance data = dataField instanceof ObjectFieldValue ? ((ObjectFieldValue)dataField).getInstance() : null;
//                Instance data = (Instance)getInstance().getValueOfField("object2");
            logicalValue = data == null ? null : DetailsSupport.getDetailsString(data, heap);
            if (logicalValue != null) logicalValue += "()";
        } else if ("JavaPackage".equals(type)) {
            FieldValue nameField = object.getFieldValue("packageName (hidden)");
            Instance name = nameField instanceof ObjectFieldValue ? ((ObjectFieldValue)nameField).getInstance() : null;
//                Instance name = (Instance)getInstance().getValueOfField("object2");
            logicalValue = name == null ? null : DetailsSupport.getDetailsString(name, heap);
        } else if ("Object".equals(type) || "JSObject".equals(type)) {
            String head = "properties [";
            String sep = ", ";
            
            StringBuilder sb = new StringBuilder();
            sb.append(head);
            
            List<FieldValue> fields = object.getFieldValues();
            for (FieldValue field : fields) {
                String name = field.getField().getName();
                if (!name.contains("(hidden)")) sb.append(name).append(sep);
            }
            
            int length = sb.length();
            if (length > head.length()) sb.delete(length - sep.length(), length);
            sb.append("]");
            
            logicalValue = sb.toString();
        } else if ("Array".equals(type) || "JSArray".equals(type)) {
            FieldValue lengthField = object.getFieldValue("length (hidden)");
            if (lengthField == null) {
                lengthField = object.getFieldValue("usedLength (hidden)");
            }
            if (lengthField != null) {
                Integer length = Integer.parseInt(lengthField.getValue());
                logicalValue = Formatters.numberFormat().format(length) + (length == 1 ? " item" : " items");
            }
        } else if ("Null$NullClass".equals(type)) {
            logicalValue = DetailsSupport.getDetailsString(object.getInstance(), heap);
        } else if ("Date".equals(type) || "JSDate".equals(type)) {
            FieldValue timeField = object.getFieldValue("timeMillis (hidden)");
            if (timeField != null) {
                double time = Double.parseDouble(timeField.getValue());
                logicalValue = new Date((long)time).toString();
            }
        } else if ("JSBoolean".equals(type) || "JSNumber".equals(type)) {
            FieldValue valueField = object.getFieldValue("value (hidden)");

            if (valueField != null) {
                if (valueField instanceof ObjectFieldValue) {
                    Instance val = ((ObjectFieldValue)valueField).getInstance();
                    logicalValue = DetailsSupport.getDetailsString(val, heap);
                } else {
                    logicalValue = valueField.getValue();
                }
            }
        }
        
        if (logicalValue != null && logicalValue.length() > MAX_LOGVALUE_LENGTH)
            logicalValue = logicalValue.substring(0, MAX_LOGVALUE_LENGTH) + "..."; // NOI18N

        return logicalValue;
    }
    
    
    private static String computeName(TruffleObjectNode.InstanceBased<JavaScriptObject> node, Heap heap) {
        return node.getTruffleObject().computeDisplayType(heap) + "#" + node.getInstance().getInstanceNumber(); // NOI18N
    }
    
    private static JavaScriptObjectNode createCopy(TruffleObjectNode.InstanceBased<JavaScriptObject> node) {
        return new JavaScriptObjectNode(node.getTruffleObject(), node.getTypeName());
    }
    
    
    static interface JavaScriptNode {}
    
    
    static class JavaScriptObjectNode extends DynamicObjectNode<JavaScriptObject> implements JavaScriptNode {
        
        JavaScriptObjectNode(JavaScriptObject object, String type) {
            super(object, type);
        }
        
        
        @Override
        protected String computeName(Heap heap) {
            return JavaScriptNodes.computeName(this, heap);
        }
        
        protected String computeLogicalValue(JavaScriptObject object, String type, Heap heap) {
            String logicalValue = JavaScriptNodes.getLogicalValue(object, type, heap);
            return logicalValue != null ? logicalValue : super.computeLogicalValue(object, type, heap);
        }
        
        
        public JavaScriptObjectNode createCopy() {
            JavaScriptObjectNode copy = JavaScriptNodes.createCopy(this);
            setupCopy(copy);
            return copy;
        }

        protected void setupCopy(JavaScriptObjectNode copy) {
            super.setupCopy(copy);
        }
        
    }
    
    static class JavaScriptTypeNode extends TruffleTypeNode<JavaScriptObject, JavaScriptType> implements JavaScriptNode {
        
        JavaScriptTypeNode(JavaScriptType type) {
            super(type);
        }

        @Override
        public HeapViewerNode createNode(JavaScriptObject object, Heap heap) {
            String type = getType().getName();
            return !type.startsWith("<") ? new JavaScriptObjectNode(object, type) :
                    new JavaScriptObjectNode(object, object.getType(heap));
        }

        @Override
        public TruffleTypeNode createCopy() {
            JavaScriptTypeNode copy = new JavaScriptTypeNode(getType());
            setupCopy(copy);
            return copy;
        }
        
        protected void setupCopy(JavaScriptTypeNode copy) {
            super.setupCopy(copy);
        }
        
    }
    
    static class JavaScriptObjectFieldNode extends DynamicObjectFieldNode<JavaScriptObject> implements JavaScriptNode {
        
        JavaScriptObjectFieldNode(JavaScriptObject object, String type, FieldValue field) {
            super(object, type, field);
        }
        
        @Override
        protected String computeName(Heap heap) {
            return JavaScriptNodes.computeName(this, heap);
        }
        
        protected String computeLogicalValue(JavaScriptObject object, String type, Heap heap) {
            String logicalValue = JavaScriptNodes.getLogicalValue(object, type, heap);
            return logicalValue != null ? logicalValue : super.computeLogicalValue(object, type, heap);
        }
        
        
        public JavaScriptObjectNode createCopy() {
            return JavaScriptNodes.createCopy(this);
        }
        
    }
    
    static class JavaScriptObjectArrayItemNode extends DynamicObjectArrayItemNode<JavaScriptObject> implements JavaScriptNode {
        
        JavaScriptObjectArrayItemNode(JavaScriptObject object, String type, ArrayItemValue item) {
            super(object, type, item);
        }
        
        @Override
        protected String computeName(Heap heap) {
            return JavaScriptNodes.computeName(this, heap);
        }
        
        protected String computeLogicalValue(JavaScriptObject object, String type, Heap heap) {
            String logicalValue = JavaScriptNodes.getLogicalValue(object, type, heap);
            return logicalValue != null ? logicalValue : super.computeLogicalValue(object, type, heap);
        }
        
        
        public JavaScriptObjectNode createCopy() {
            return JavaScriptNodes.createCopy(this);
        }
        
    }
        
    
    static class JavaScriptObjectReferenceNode extends DynamicObjectReferenceNode<JavaScriptObject> implements JavaScriptNode {
        
        JavaScriptObjectReferenceNode(JavaScriptObject object, String type, FieldValue value) {
            super(object, type, value);
        }
        
        @Override
        protected String computeName(Heap heap) {
            return JavaScriptNodes.computeName(this, heap);
        }
        
        protected String computeLogicalValue(JavaScriptObject object, String type, Heap heap) {
            String logicalValue = JavaScriptNodes.getLogicalValue(object, type, heap);
            return logicalValue != null ? logicalValue : super.computeLogicalValue(object, type, heap);
        }
        
        
        public JavaScriptObjectNode createCopy() {
            return JavaScriptNodes.createCopy(this);
        }
        
    }
    
    static class JavaScriptLocalObjectNode extends LocalDynamicObjectNode<JavaScriptObject> implements JavaScriptNode {
        
        JavaScriptLocalObjectNode(JavaScriptObject object, String type) {
            super(object, type);
        }
        
        @Override
        protected String computeName(Heap heap) {
            return JavaScriptNodes.computeName(this, heap);
        }
        
        protected String computeLogicalValue(JavaScriptObject object, String type, Heap heap) {
            String logicalValue = JavaScriptNodes.getLogicalValue(object, type, heap);
            return logicalValue != null ? logicalValue : super.computeLogicalValue(object, type, heap);
        }
        
        
        public JavaScriptObjectNode createCopy() {
            return JavaScriptNodes.createCopy(this);
        }
        
    }
    
    
    @ServiceProvider(service=HeapViewerRenderer.Provider.class)
    public static class JavaScriptNodesRendererProvider extends HeapViewerRenderer.Provider {

        public boolean supportsView(HeapContext context, String viewID) {
            return true;
        }

        public void registerRenderers(Map<Class<? extends HeapViewerNode>, HeapViewerRenderer> renderers, HeapContext context) {
            JavaScriptLanguage language = JavaScriptLanguage.instance();
            Icon instanceIcon = language.createLanguageIcon(Icons.getIcon(LanguageIcons.INSTANCE));
            Icon packageIcon = language.createLanguageIcon(Icons.getIcon(LanguageIcons.PACKAGE));

            Heap heap = context.getFragment().getHeap();

            renderers.put(JavaScriptNodes.JavaScriptObjectNode.class, new TruffleObjectNode.Renderer(heap, instanceIcon));

            renderers.put(JavaScriptNodes.JavaScriptTypeNode.class, new TruffleTypeNode.Renderer(packageIcon));

            renderers.put(JavaScriptNodes.JavaScriptObjectFieldNode.class, new TruffleObjectFieldNode.Renderer(heap, instanceIcon));

            renderers.put(JavaScriptNodes.JavaScriptObjectArrayItemNode.class, new TruffleObjectArrayItemNode.Renderer(heap, instanceIcon));

            renderers.put(JavaScriptNodes.JavaScriptObjectReferenceNode.class, new TruffleObjectReferenceNode.Renderer(heap, instanceIcon));

            renderers.put(JavaScriptNodes.JavaScriptLocalObjectNode.class, new TruffleLocalObjectNode.Renderer(heap, instanceIcon));
        }

    }
    
}
