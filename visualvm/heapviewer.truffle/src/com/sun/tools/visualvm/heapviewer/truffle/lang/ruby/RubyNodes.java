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
package com.sun.tools.visualvm.heapviewer.truffle.lang.ruby;

import com.sun.tools.visualvm.heapviewer.HeapContext;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNode;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleLocalObjectNode;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleObjectArrayItemNode;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleObjectFieldNode;
import java.util.List;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.ObjectFieldValue;
import org.netbeans.lib.profiler.ui.Formatters;
import org.netbeans.modules.profiler.heapwalk.details.spi.DetailsUtils;
import com.sun.tools.visualvm.heapviewer.truffle.dynamicobject.DynamicObjectArrayItemNode;
import com.sun.tools.visualvm.heapviewer.truffle.dynamicobject.DynamicObjectFieldNode;
import com.sun.tools.visualvm.heapviewer.truffle.dynamicobject.DynamicObjectNode;
import com.sun.tools.visualvm.heapviewer.truffle.dynamicobject.DynamicObjectReferenceNode;
import com.sun.tools.visualvm.heapviewer.truffle.dynamicobject.LocalDynamicObjectNode;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleObjectNode;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleObjectReferenceNode;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleOpenNodeActionProvider;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleTypeNode;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerNodeAction;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerRenderer;
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
public class RubyNodes extends TruffleOpenNodeActionProvider<RubyObject, RubyType, RubyHeapFragment, RubyLanguage> {
    
    @Override
    public boolean supportsView(HeapContext context, String viewID) {
        return RubyHeapFragment.isRubyHeap(context);
    }
    
    @Override
    protected boolean supportsNode(HeapViewerNode node) {
        return node instanceof RubyNodes.RubyNode;
    }

    @Override
    protected RubyLanguage getLanguage() {
        return RubyLanguage.instance();
    }
    
    
    private static final int MAX_LOGVALUE_LENGTH = 160;
    
    static String getLogicalValue(RubyObject object, String type, Heap heap) {
        String logicalValue = null;

        if ("Proc".equals(type)) {
            FieldValue infoField = object.getFieldValue("sharedMethodInfo (hidden)");
            Instance info = infoField instanceof ObjectFieldValue ? ((ObjectFieldValue)infoField).getInstance() : null;
            if (info != null) {
                String name = DetailsUtils.getInstanceFieldString(info, "name", heap);
                String notes = DetailsUtils.getInstanceFieldString(info, "notes", heap);
                
                if (name != null && notes != null) logicalValue = name + " (" + notes + ")";
                else if (name != null) logicalValue = name;
                else if (notes != null) logicalValue = notes;
            }
        } else if ("Method".equals(type) || "UnboundMethod".equals(type)) {
            FieldValue methodField = object.getFieldValue("method (hidden)");
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
        } else if ("Symbol".equals(type)) {
            FieldValue symbolField = object.getFieldValue("string (hidden)");
            Instance symbol = symbolField instanceof ObjectFieldValue ? ((ObjectFieldValue)symbolField).getInstance() : null;
            
            if (symbol != null) logicalValue = DetailsUtils.getInstanceString(symbol, heap);
        } else if ("Class".equals(type) || "Module".equals(type)) {
            FieldValue fieldsField = object.getFieldValue("fields (hidden)");
            Instance fields = fieldsField instanceof ObjectFieldValue ? ((ObjectFieldValue)fieldsField).getInstance() : null;
            
            Object nameField = fields == null ? null : fields.getValueOfField("name");
            Instance name = nameField instanceof Instance ? (Instance)nameField : null;
            
            if (name != null) logicalValue = DetailsUtils.getInstanceString(name, heap);
        } else if ("BasicObject".equals(type)) {
            String head = "fields [";
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
        } else if ("Array".equals(type)) {
            FieldValue sizeField = object.getFieldValue("size (hidden)");
            if (sizeField != null) {
                Integer size = Integer.parseInt(sizeField.getValue());
                logicalValue = Formatters.numberFormat().format(size) + (size == 1 ? " item" : " items");
            }
        } else if ("String".equals(type)) {
            FieldValue ropeField = object.getFieldValue("rope (hidden)");
            Instance rope = ropeField instanceof ObjectFieldValue ? ((ObjectFieldValue)ropeField).getInstance() : null;
            if (rope != null) logicalValue = DetailsUtils.getInstanceString(rope, heap);
        } else if ("Regexp".equals(type)) {
            FieldValue sourceField = object.getFieldValue("source (hidden)");
            Instance source = sourceField instanceof ObjectFieldValue ? ((ObjectFieldValue)sourceField).getInstance() : null;
            if (source != null) logicalValue = DetailsUtils.getInstanceString(source, heap);
        } else if ("Encoding".equals(type)) {
            FieldValue encodingField = object.getFieldValue("encoding (hidden)");
            Instance encoding = encodingField instanceof ObjectFieldValue ? ((ObjectFieldValue)encodingField).getInstance() : null;
            if (encoding != null) logicalValue = DetailsUtils.getInstanceString(encoding, heap);
        }
        
        if (logicalValue != null && logicalValue.length() > MAX_LOGVALUE_LENGTH)
            logicalValue = logicalValue.substring(0, MAX_LOGVALUE_LENGTH) + "..."; // NOI18N

        return logicalValue;
    }
    
    
    // TODO: uncomment once types caching is available for RubyHeapFragment
//    private static String computeName(TruffleObjectNode.InstanceBased<RubyDynamicObject> node, Heap heap) {
//        return node.getTruffleObject().computeDisplayType(heap) + "#" + node.getInstance().getInstanceNumber(); // NOI18N
//    }
    
    private static RubyObjectNode createCopy(TruffleObjectNode.InstanceBased<RubyObject> node) {
        return new RubyObjectNode(node.getTruffleObject(), node.getTypeName());
    }
    
    
    static interface RubyNode {}
    
    
    static class RubyObjectNode extends DynamicObjectNode<RubyObject> implements RubyNode {
        
        RubyObjectNode(RubyObject object, String type) {
            super(object, type);
        }
        
        
        // TODO: uncomment once types caching is available for RubyHeapFragment
//        @Override
//        protected String computeName(Heap heap) {
//            return RubyNodes.computeName(this, heap);
//        }
        
        protected String computeLogicalValue(RubyObject object, String type, Heap heap) {
            String logicalValue = RubyNodes.getLogicalValue(object, type, heap);
            return logicalValue != null ? logicalValue : super.computeLogicalValue(object, type, heap);
        }
        
        
        public RubyObjectNode createCopy() {
            RubyObjectNode copy = RubyNodes.createCopy(this);
            setupCopy(copy);
            return copy;
        }

        protected void setupCopy(RubyObjectNode copy) {
            super.setupCopy(copy);
        }
        
    }
    
    static class RubyLocalObjectNode extends LocalDynamicObjectNode<RubyObject> implements RubyNode {
        
        RubyLocalObjectNode(RubyObject object, String type) {
            super(object, type);
        }
        
        
        // TODO: uncomment once types caching is available for RubyHeapFragment
//        @Override
//        protected String computeName(Heap heap) {
//            return RubyNodes.computeName(this, heap);
//        }
        
        protected String computeLogicalValue(RubyObject object, String type, Heap heap) {
            String logicalValue = RubyNodes.getLogicalValue(object, type, heap);
            return logicalValue != null ? logicalValue : super.computeLogicalValue(object, type, heap);
        }
        
        
        public RubyObjectNode createCopy() {
            return RubyNodes.createCopy(this);
        }
        
    }
    
    static class RubyTypeNode extends TruffleTypeNode<RubyObject, RubyType> implements RubyNode {
        
        RubyTypeNode(RubyType type) {
            super(type);
        }

        @Override
        public HeapViewerNode createNode(RubyObject object, Heap heap) {
            String type = getType().getName();
            return new RubyObjectNode(object, type);
        }

        @Override
        public TruffleTypeNode createCopy() {
            RubyTypeNode copy = new RubyTypeNode(getType());
            setupCopy(copy);
            return copy;
        }
        
        protected void setupCopy(RubyTypeNode copy) {
            super.setupCopy(copy);
        }
        
    }
    
    
    static class RubyObjectFieldNode extends DynamicObjectFieldNode<RubyObject> implements RubyNode {
        
        RubyObjectFieldNode(RubyObject object, String type, FieldValue field) {
            super(object, type, field);
        }
        
        
        // TODO: uncomment once types caching is available for RubyHeapFragment
//        @Override
//        protected String computeName(Heap heap) {
//            return RubyNodes.computeName(this, heap);
//        }
        
        protected String computeLogicalValue(RubyObject object, String type, Heap heap) {
            String logicalValue = RubyNodes.getLogicalValue(object, type, heap);
            return logicalValue != null ? logicalValue : super.computeLogicalValue(object, type, heap);
        }
        
        
        public RubyObjectNode createCopy() {
            return RubyNodes.createCopy(this);
        }
        
    }
    
    static class RubyObjectArrayItemNode extends DynamicObjectArrayItemNode<RubyObject> implements RubyNode {
        
        RubyObjectArrayItemNode(RubyObject object, String type, ArrayItemValue item) {
            super(object, type, item);
        }
        
        
        // TODO: uncomment once types caching is available for RubyHeapFragment
//        @Override
//        protected String computeName(Heap heap) {
//            return RubyNodes.computeName(this, heap);
//        }
        
        protected String computeLogicalValue(RubyObject object, String type, Heap heap) {
            String logicalValue = RubyNodes.getLogicalValue(object, type, heap);
            return logicalValue != null ? logicalValue : super.computeLogicalValue(object, type, heap);
        }
        
        
        public RubyObjectNode createCopy() {
            return RubyNodes.createCopy(this);
        }
        
    }
    
    static class RubyObjectReferenceNode extends DynamicObjectReferenceNode<RubyObject> implements RubyNode {
        
        RubyObjectReferenceNode(RubyObject object, String type, FieldValue value) {
            super(object, type, value);
        }
        
        
        // TODO: uncomment once types caching is available for RubyHeapFragment
//        @Override
//        protected String computeName(Heap heap) {
//            return RubyNodes.computeName(this, heap);
//        }
        
        protected String computeLogicalValue(RubyObject object, String type, Heap heap) {
            String logicalValue = RubyNodes.getLogicalValue(object, type, heap);
            return logicalValue != null ? logicalValue : super.computeLogicalValue(object, type, heap);
        }
        
        
        public RubyObjectNode createCopy() {
            return RubyNodes.createCopy(this);
        }
        
    }
    
    
    @ServiceProvider(service=HeapViewerRenderer.Provider.class)
    public static class RubyNodesRendererProvider extends HeapViewerRenderer.Provider {

        public boolean supportsView(HeapContext context, String viewID) {
            return true;
        }

        public void registerRenderers(Map<Class<? extends HeapViewerNode>, HeapViewerRenderer> renderers, HeapContext context) {
            RubyLanguage language = RubyLanguage.instance();
            Icon instanceIcon = language.createLanguageIcon(Icons.getIcon(LanguageIcons.INSTANCE));
            Icon packageIcon = language.createLanguageIcon(Icons.getIcon(LanguageIcons.PACKAGE));

            Heap heap = context.getFragment().getHeap();

            renderers.put(RubyNodes.RubyObjectNode.class, new TruffleObjectNode.Renderer(heap, instanceIcon));

            renderers.put(RubyNodes.RubyTypeNode.class, new TruffleTypeNode.Renderer(packageIcon));

            renderers.put(RubyNodes.RubyObjectFieldNode.class, new TruffleObjectFieldNode.Renderer(heap, instanceIcon));

            renderers.put(RubyNodes.RubyObjectArrayItemNode.class, new TruffleObjectArrayItemNode.Renderer(heap, instanceIcon));

            renderers.put(RubyNodes.RubyObjectReferenceNode.class, new TruffleObjectReferenceNode.Renderer(heap, instanceIcon));

            renderers.put(RubyNodes.RubyLocalObjectNode.class, new TruffleLocalObjectNode.Renderer(heap, instanceIcon));
        }

    }
    
}
