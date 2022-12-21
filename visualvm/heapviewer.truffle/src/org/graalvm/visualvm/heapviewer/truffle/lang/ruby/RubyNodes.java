/*
 * Copyright (c) 2017, 2022, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.heapviewer.truffle.lang.ruby;

import java.util.List;
import java.util.Map;
import javax.swing.Icon;
import org.graalvm.visualvm.heapviewer.HeapContext;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNode;
import org.graalvm.visualvm.heapviewer.truffle.dynamicobject.DynamicObjectArrayItemNode;
import org.graalvm.visualvm.heapviewer.truffle.dynamicobject.DynamicObjectFieldNode;
import org.graalvm.visualvm.heapviewer.truffle.dynamicobject.DynamicObjectNode;
import org.graalvm.visualvm.heapviewer.truffle.dynamicobject.DynamicObjectReferenceNode;
import org.graalvm.visualvm.heapviewer.truffle.dynamicobject.LocalDynamicObjectNode;
import org.graalvm.visualvm.heapviewer.truffle.nodes.TruffleLocalObjectNode;
import org.graalvm.visualvm.heapviewer.truffle.nodes.TruffleObjectArrayItemNode;
import org.graalvm.visualvm.heapviewer.truffle.nodes.TruffleObjectFieldNode;
import org.graalvm.visualvm.heapviewer.truffle.nodes.TruffleObjectNode;
import org.graalvm.visualvm.heapviewer.truffle.nodes.TruffleObjectReferenceNode;
import org.graalvm.visualvm.heapviewer.truffle.nodes.TruffleOpenNodeActionProvider;
import org.graalvm.visualvm.heapviewer.truffle.nodes.TruffleTypeNode;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerNodeAction;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerRenderer;
import org.graalvm.visualvm.lib.jfluid.heap.ArrayItemValue;
import org.graalvm.visualvm.lib.jfluid.heap.FieldValue;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.ObjectFieldValue;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.LanguageIcons;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsUtils;
import org.graalvm.visualvm.lib.ui.Formatters;
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
    
    static String getLogicalValue(RubyObject object, String type) {
        String logicalValue = null;

        if ("Proc".equals(type)) { // NOI18N
            FieldValue infoField = object.getFieldValue("sharedMethodInfo (hidden)"); // NOI18N
            Instance info = infoField instanceof ObjectFieldValue ? ((ObjectFieldValue)infoField).getInstance() : null;
            if (info != null) {
                String name = DetailsUtils.getInstanceFieldString(info, "name"); // NOI18N
                String notes = DetailsUtils.getInstanceFieldString(info, "notes"); // NOI18N
                
                if (name != null && notes != null) logicalValue = name + " (" + notes + ")"; // NOI18N
                else if (name != null) logicalValue = name;
                else if (notes != null) logicalValue = notes;
            }
        } else if ("Method".equals(type) || "UnboundMethod".equals(type)) { // NOI18N
            FieldValue methodField = object.getFieldValue("method (hidden)"); // NOI18N
            Instance method = methodField instanceof ObjectFieldValue ? ((ObjectFieldValue)methodField).getInstance() : null;
            
            Object infoField = method == null ? null : method.getValueOfField("sharedMethodInfo"); // NOI18N
            Instance info = infoField instanceof Instance ? (Instance)infoField : null;
            
            if (info != null) {
                String name = DetailsUtils.getInstanceFieldString(info, "name"); // NOI18N
                String notes = DetailsUtils.getInstanceFieldString(info, "notes"); // NOI18N
                
                if (name != null && notes != null) logicalValue = name + " (" + notes + ")"; // NOI18N
                else if (name != null) logicalValue = name;
                else if (notes != null) logicalValue = notes;
            }
        } else if ("Symbol".equals(type)) { // NOI18N
            FieldValue symbolField = object.getFieldValue("string (hidden)"); // NOI18N
            Instance symbol = symbolField instanceof ObjectFieldValue ? ((ObjectFieldValue)symbolField).getInstance() : null;
            
            if (symbol != null) logicalValue = DetailsUtils.getInstanceString(symbol);
        } else if ("Class".equals(type) || "Module".equals(type)) { // NOI18N
            FieldValue fieldsField = object.getFieldValue("fields (hidden)"); // NOI18N
            Instance fields = fieldsField instanceof ObjectFieldValue ? ((ObjectFieldValue)fieldsField).getInstance() : null;
            
            Object nameField = fields == null ? null : fields.getValueOfField("name"); // NOI18N
            Instance name = nameField instanceof Instance ? (Instance)nameField : null;
            
            if (name != null) logicalValue = DetailsUtils.getInstanceString(name);
        } else if ("BasicObject".equals(type)) { // NOI18N
            String head = "fields ["; // NOI18N
            String sep = ", "; // NOI18N
            
            StringBuilder sb = new StringBuilder();
            sb.append(head);
            
            List<FieldValue> fields = object.getFieldValues();
            for (FieldValue field : fields) {
                String name = field.getField().getName();
                if (!name.contains("(hidden)")) sb.append(name).append(sep); // NOI18N
            }
            
            int length = sb.length();
            if (length > head.length()) sb.delete(length - sep.length(), length);
            sb.append("]"); // NOI18N
            
            logicalValue = sb.toString();
        } else if ("Array".equals(type)) { // NOI18N
            FieldValue sizeField = object.getFieldValue("size (hidden)"); // NOI18N
            if (sizeField != null) {
                Integer size = Integer.parseInt(sizeField.getValue());
                logicalValue = Formatters.numberFormat().format(size) + (size == 1 ? " item" : " items"); // NOI18N
            }
        } else if ("String".equals(type)) { // NOI18N
            FieldValue ropeField = object.getFieldValue("rope (hidden)"); // NOI18N
            Instance rope = ropeField instanceof ObjectFieldValue ? ((ObjectFieldValue)ropeField).getInstance() : null;
            if (rope != null) logicalValue = DetailsUtils.getInstanceString(rope);
        } else if ("Regexp".equals(type)) { // NOI18N
            FieldValue sourceField = object.getFieldValue("source (hidden)"); // NOI18N
            Instance source = sourceField instanceof ObjectFieldValue ? ((ObjectFieldValue)sourceField).getInstance() : null;
            if (source != null) logicalValue = DetailsUtils.getInstanceString(source);
        } else if ("Encoding".equals(type)) { // NOI18N
            FieldValue encodingField = object.getFieldValue("encoding (hidden)"); // NOI18N
            Instance encoding = encodingField instanceof ObjectFieldValue ? ((ObjectFieldValue)encodingField).getInstance() : null;
            if (encoding != null) logicalValue = DetailsUtils.getInstanceString(encoding);
        } else if ("Integer".equals(type)) { // NOI18N
            FieldValue valueField = object.getFieldValue("value (hidden)"); // NOI18N
            Instance value = valueField instanceof ObjectFieldValue ? ((ObjectFieldValue)valueField).getInstance() : null;
            if (value != null) logicalValue = DetailsUtils.getInstanceString(value);
        } else if ("Rational".equals(type)) { // NOI18N
            FieldValue numField = object.getFieldValue("@numerator"); // NOI18N
            Instance numerator = numField instanceof ObjectFieldValue ? ((ObjectFieldValue)numField).getInstance() : null;
            FieldValue denomField = object.getFieldValue("@denominator"); // NOI18N
            Instance denominator = denomField instanceof ObjectFieldValue ? ((ObjectFieldValue)denomField).getInstance() : null;
            if (numField != null && denomField != null) {
                String numeratorValue;
                String denominatorValue;

                if (numerator != null) {
                    numeratorValue = DetailsUtils.getInstanceString(numerator);
                } else {
                    numeratorValue = numField.getValue();
                }
                if (denominator != null) {
                    denominatorValue = DetailsUtils.getInstanceString(denominator);
                } else {
                    denominatorValue = denomField.getValue();
                }
                if (numeratorValue != null && denominatorValue != null) {
                    logicalValue = "("+numeratorValue+"/"+denominatorValue+")";
                }
            }
        } else if ("Complex".equals(type)) { // NOI18N
            FieldValue realField = object.getFieldValue("@real"); // NOI18N
            String real = realField != null ? realField.getValue() : null;
            FieldValue imagField = object.getFieldValue("@imag"); // NOI18N
            String imag = imagField != null ? imagField.getValue() : null;
            if (real != null && imag != null) logicalValue = "(" + real + (imag.startsWith("-") ? imag : "+" + imag) + "i)"; // NOI18N
        } else if ("Range".equals(type)) { // NOI18N
            FieldValue beginField = object.getFieldValue("begin (hidden)"); // NOI18N
            FieldValue endField = object.getFieldValue("end (hidden)"); // NOI18N
            FieldValue excludedField = object.getFieldValue("excludedEnd (hidden)"); // NOI18N
            if (beginField != null && endField != null && excludedField != null) {
                Instance beginInstance = beginField instanceof ObjectFieldValue ? ((ObjectFieldValue)beginField).getInstance() : null;
                String begin = beginInstance != null ? logicalValue(beginInstance) : beginField.getValue();
                
                Instance endInstance = endField instanceof ObjectFieldValue ? ((ObjectFieldValue)endField).getInstance() : null;
                String end = endInstance != null ? logicalValue(endInstance) : endField.getValue();
                
                boolean excluded = "1".equals(excludedField.getValue()); // NOI18N
                
                logicalValue = "(" + begin + (excluded ? "..." : "..") + end + ")"; // NOI18N
            }
        }
        
        if (logicalValue != null && logicalValue.length() > MAX_LOGVALUE_LENGTH)
            logicalValue = logicalValue.substring(0, MAX_LOGVALUE_LENGTH) + "..."; // NOI18N

        return logicalValue;
    }
    
    private static String logicalValue(Instance instance) {
        if (RubyObject.isRubyObject(instance)) {
            RubyObject object = new RubyObject(instance);
            return getLogicalValue(object, object.getType());
        } else {
            return DetailsUtils.getInstanceString(instance);
        }
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
//        protected String computeObjectName(Heap heap) {
//            return RubyNodes.computeObjectName(this, heap);
//        }
        
        protected String computeLogicalValue(RubyObject object, String type) {
            String logicalValue = RubyNodes.getLogicalValue(object, type);
            return logicalValue != null ? logicalValue : super.computeLogicalValue(object, type);
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
//        protected String computeObjectName(Heap heap) {
//            return RubyNodes.computeObjectName(this, heap);
//        }
        
        protected String computeLogicalValue(RubyObject object, String type) {
            String logicalValue = RubyNodes.getLogicalValue(object, type);
            return logicalValue != null ? logicalValue : super.computeLogicalValue(object, type);
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
        public HeapViewerNode createNode(RubyObject object) {
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
//        protected String computeObjectName(Heap heap) {
//            return RubyNodes.computeObjectName(this, heap);
//        }
        
        protected String computeLogicalValue(RubyObject object, String type) {
            String logicalValue = RubyNodes.getLogicalValue(object, type);
            return logicalValue != null ? logicalValue : super.computeLogicalValue(object, type);
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
//        protected String computeObjectName(Heap heap) {
//            return RubyNodes.computeObjectName(this, heap);
//        }
        
        protected String computeLogicalValue(RubyObject object, String type) {
            String logicalValue = RubyNodes.getLogicalValue(object, type);
            return logicalValue != null ? logicalValue : super.computeLogicalValue(object, type);
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
//        protected String computeObjectName(Heap heap) {
//            return RubyNodes.computeObjectName(this, heap);
//        }
        
        protected String computeLogicalValue(RubyObject object, String type) {
            String logicalValue = RubyNodes.getLogicalValue(object, type);
            return logicalValue != null ? logicalValue : super.computeLogicalValue(object, type);
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
