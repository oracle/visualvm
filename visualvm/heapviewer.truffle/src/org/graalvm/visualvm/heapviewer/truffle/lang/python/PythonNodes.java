/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.heapviewer.truffle.lang.python;

import java.util.List;
import org.graalvm.visualvm.heapviewer.HeapContext;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNode;
import org.graalvm.visualvm.heapviewer.truffle.nodes.TruffleLocalObjectNode;
import org.graalvm.visualvm.heapviewer.truffle.nodes.TruffleObjectFieldNode;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.api.DetailsSupport;
import org.graalvm.visualvm.heapviewer.truffle.nodes.TruffleObjectNode;
import org.graalvm.visualvm.heapviewer.truffle.nodes.TruffleObjectReferenceNode;
import org.graalvm.visualvm.heapviewer.truffle.nodes.TruffleOpenNodeActionProvider;
import org.graalvm.visualvm.heapviewer.truffle.nodes.TruffleTypeNode;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerNodeAction;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerRenderer;
import java.util.Map;
import javax.swing.Icon;
import org.graalvm.visualvm.lib.jfluid.heap.FieldValue;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.ObjectFieldValue;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.LanguageIcons;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsUtils;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@ServiceProvider(service=HeapViewerNodeAction.Provider.class)
public class PythonNodes extends TruffleOpenNodeActionProvider<PythonObject, PythonType, PythonHeapFragment, PythonLanguage> {
    
    @Override
    public boolean supportsView(HeapContext context, String viewID) {
        return PythonHeapFragment.isPythonHeap(context);
    }
    
    @Override
    protected boolean supportsNode(HeapViewerNode node) {
        return node instanceof PythonNodes.PythonNode;
    }

    @Override
    protected PythonLanguage getLanguage() {
        return PythonLanguage.instance();
    }
    
    
    private static final int MAX_LOGVALUE_LENGTH = 160;
    
    static String getLogicalValue(PythonObject object, String type) {
        String logicalValue = null;
        
        if ("ModuleSpec".equals(type)) { // NOI18N
            List<FieldValue> attributes = object.getAttributes();
            for (FieldValue attribute : attributes) {
                if ("name".equals(attribute.getField().getName()) && attribute instanceof ObjectFieldValue) { // NOI18N
                    Instance attributeI = ((ObjectFieldValue)attribute).getInstance();
                    logicalValue = DetailsSupport.getDetailsString(attributeI);
                    break;
                }
            }
        } else if ("SourceFileLoader".equals(type)) { // NOI18N
            List<FieldValue> attributes = object.getAttributes();
            for (FieldValue attribute : attributes) {
                if ("path".equals(attribute.getField().getName()) && attribute instanceof ObjectFieldValue) { // NOI18N
                    Instance attributeI = ((ObjectFieldValue)attribute).getInstance();
                    logicalValue = DetailsSupport.getDetailsString(attributeI);
                    break;
                }
            }
        } else if ("mappingproxy".equals(type)) { // NOI18N
            List<FieldValue> attributes = object.getAttributes();
            for (FieldValue attribute : attributes) {
                if ("__name__".equals(attribute.getField().getName()) && attribute instanceof ObjectFieldValue) { // NOI18N
                    Instance attributeI = ((ObjectFieldValue)attribute).getInstance();
                    logicalValue = DetailsSupport.getDetailsString(attributeI);
                    break;
                }
            }
        } else if ("generator".equals(type)) { // NOI18N
            logicalValue = DetailsUtils.getInstanceFieldString(object.getInstance(), "name"); // NOI18N
        } else if ("FileIO".equals(type)) { // NOI18N
            List<FieldValue> attributes = object.getAttributes();
            for (FieldValue attribute : attributes) {
                if ("name".equals(attribute.getField().getName()) && attribute instanceof ObjectFieldValue) { // NOI18N
                    Instance attributeI = ((ObjectFieldValue)attribute).getInstance();
                    logicalValue = DetailsSupport.getDetailsString(attributeI);
                    break;
                }
            }
        } else if ("super".equals(type)) { // NOI18N
            Object moduleO = object.getInstance().getValueOfField("type"); // NOI18N
            if (!(moduleO instanceof Instance)) moduleO = null;
            else if (!((Instance)moduleO).getJavaClass().getName().equals("com.oracle.graal.python.builtins.objects.type.PythonClass")) moduleO = null; // NOI18N
            logicalValue = moduleO == null ? null : DetailsUtils.getInstanceString((Instance)moduleO);
        } else if ("code".equals(type)) { // NOI18N
            Object callTarget = object.getInstance().getValueOfField("callTarget"); // NOI18N
            if (callTarget instanceof Instance) {
                Object rootNode = ((Instance)callTarget).getValueOfField("rootNode"); // NOI18N
                if (rootNode instanceof Instance) {
                    logicalValue = DetailsUtils.getInstanceFieldString((Instance)rootNode, "functionName"); // NOI18N
                }
            }
        } else if ("FileFinder".equals(type)) { // NOI18N
            List<FieldValue> attributes = object.getAttributes();
            for (FieldValue attribute : attributes) {
                if ("path".equals(attribute.getField().getName()) && attribute instanceof ObjectFieldValue) { // NOI18N
                    Instance attributeI = ((ObjectFieldValue)attribute).getInstance();
                    logicalValue = DetailsSupport.getDetailsString(attributeI);
                    break;
                }
            }
        } else if ("BufferedReader".equals(type) || "BufferedWriter".equals(type)) { // NOI18N
            List<FieldValue> attributes = object.getAttributes();
            for (FieldValue attribute : attributes) {
                if ("_raw".equals(attribute.getField().getName()) && attribute instanceof ObjectFieldValue) { // NOI18N
                    Instance attributeI = ((ObjectFieldValue)attribute).getInstance();
                    if (PythonObject.isPythonObject(attributeI)) {
                        logicalValue = getLogicalValue(new PythonObject(attributeI), "FileIO"); // NOI18N
                        break;
                    }
                }
            }
        } else if ("TextIOWrapper".equals(type)) { // NOI18N
            List<FieldValue> attributes = object.getAttributes();
            for (FieldValue attribute : attributes) {
                if ("_buffer".equals(attribute.getField().getName()) && attribute instanceof ObjectFieldValue) { // NOI18N
                    Instance attributeI = ((ObjectFieldValue)attribute).getInstance();
                    if (PythonObject.isPythonObject(attributeI)) {
                        logicalValue = getLogicalValue(new PythonObject(attributeI), "BufferedWriter"); // NOI18N
                        break;
                    }
                }
            }
        } else if ("TemplateFormatter".equals(type)) { // NOI18N
            List<FieldValue> attributes = object.getAttributes();
            for (FieldValue attribute : attributes) {
                if ("template".equals(attribute.getField().getName()) && attribute instanceof ObjectFieldValue) { // NOI18N
                    Instance attributeI = ((ObjectFieldValue)attribute).getInstance();
                    logicalValue = DetailsSupport.getDetailsString(attributeI);
                    break;
                }
            }
        } else if ("_Printer".equals(type)) { // NOI18N
            List<FieldValue> attributes = object.getAttributes();
            for (FieldValue attribute : attributes) {
                if ("__name".equals(attribute.getField().getName()) && attribute instanceof ObjectFieldValue) { // NOI18N
                    Instance attributeI = ((ObjectFieldValue)attribute).getInstance();
                    logicalValue = DetailsSupport.getDetailsString(attributeI);
                    break;
                }
            }
        } else if ("Quitter".equals(type)) { // NOI18N
            List<FieldValue> attributes = object.getAttributes();
            for (FieldValue attribute : attributes) {
                if ("name".equals(attribute.getField().getName()) && attribute instanceof ObjectFieldValue) { // NOI18N
                    Instance attributeI = ((ObjectFieldValue)attribute).getInstance();
                    logicalValue = DetailsSupport.getDetailsString(attributeI);
                    break;
                }
            }
        } else if ("CodecInfo".equals(type)) { // NOI18N
            List<FieldValue> attributes = object.getAttributes();
            for (FieldValue attribute : attributes) {
                if ("name".equals(attribute.getField().getName()) && attribute instanceof ObjectFieldValue) { // NOI18N
                    Instance attributeI = ((ObjectFieldValue)attribute).getInstance();
                    logicalValue = DetailsSupport.getDetailsString(attributeI);
                    break;
                }
            }
        } else if ("dict".equals(type)) { // NOI18N
            List<FieldValue> attributes = object.getAttributes();
            logicalValue = attributes.size() + " pairs"; // NOI18N
        }
        
        if (logicalValue != null && logicalValue.length() > MAX_LOGVALUE_LENGTH)
            logicalValue = logicalValue.substring(0, MAX_LOGVALUE_LENGTH) + "..."; // NOI18N
        
        return logicalValue != null ? logicalValue :
               DetailsSupport.getDetailsString(object.getInstance());
    }
    
    
    private static String computeObjectName(TruffleObjectNode.InstanceBased<PythonObject> node) {
        String typeString = node.getTypeName();
        return typeString.substring(typeString.lastIndexOf('.') + 1) + "#" + node.getInstance().getInstanceNumber(); // NOI18N
    }
    
    private static PythonObjectNode createCopy(TruffleObjectNode.InstanceBased<PythonObject> node) {
        return new PythonObjectNode(node.getTruffleObject(), node.getTypeName());
    }
    
    
    static interface PythonNode {}
    
    
    static class PythonObjectNode extends TruffleObjectNode.InstanceBased<PythonObject> implements PythonNode {
        
        PythonObjectNode(PythonObject object) {
            this(object, object.getType());
        }

        PythonObjectNode(PythonObject robject, String type) {
            super(robject, type);
        }
        
        
        @Override
        protected String computeObjectName() {
            return PythonNodes.computeObjectName(this);
        }
        
        protected String computeLogicalValue(PythonObject object, String type) {
            String logicalValue = PythonNodes.getLogicalValue(object, type);
            return logicalValue != null ? logicalValue : super.computeLogicalValue(object, type);
        }
        
        
        public PythonObjectNode createCopy() {
            PythonObjectNode copy = PythonNodes.createCopy(this);
            setupCopy(copy);
            return copy;
        }

        protected void setupCopy(PythonObjectNode copy) {
            super.setupCopy(copy);
        }
        
    }
    
    static class PythonLocalObjectNode extends TruffleLocalObjectNode.InstanceBased<PythonObject> implements PythonNode {
        
        PythonLocalObjectNode(PythonObject object, String type) {
            super(object, type);
        }
        
        @Override
        protected String computeObjectName() {
            return PythonNodes.computeObjectName(this);
        }
        
        protected String computeLogicalValue(PythonObject object, String type) {
            String logicalValue = PythonNodes.getLogicalValue(object, type);
            return logicalValue != null ? logicalValue : super.computeLogicalValue(object, type);
        }
        
        
        public PythonObjectNode createCopy() {
            return PythonNodes.createCopy(this);
        }
        
    }
    
    static class PythonTypeNode extends TruffleTypeNode<PythonObject, PythonType> implements PythonNode {
        
        PythonTypeNode(PythonType type) {
            super(type);
        }

        @Override
        public HeapViewerNode createNode(PythonObject object) {
            String type = getType().getName();
            return new PythonNodes.PythonObjectNode(object, type);
        }

        @Override
        public TruffleTypeNode createCopy() {
            PythonTypeNode copy = new PythonTypeNode(getType());
            setupCopy(copy);
            return copy;
        }
        
        protected void setupCopy(PythonTypeNode copy) {
            super.setupCopy(copy);
        }
        
    }
    
    
    static class PythonObjectFieldNode extends TruffleObjectFieldNode.InstanceBased<PythonObject> implements PythonNode {
        
        PythonObjectFieldNode(PythonObject object, String type, FieldValue field) {
            super(object, type, field);
        }
        
        @Override
        protected String computeObjectName() {
            return PythonNodes.computeObjectName(this); // NOI18N
        }
        
        protected String computeLogicalValue(PythonObject object, String type) {
            String logicalValue = PythonNodes.getLogicalValue(object, type);
            return logicalValue != null ? logicalValue : super.computeLogicalValue(object, type);
        }
        
        
        public PythonObjectNode createCopy() {
            return PythonNodes.createCopy(this);
        }
        
    }
    
    static class PythonObjectReferenceNode extends TruffleObjectReferenceNode.InstanceBased<PythonObject> implements PythonNode {
        
        PythonObjectReferenceNode(PythonObject object, String type, FieldValue value) {
            super(object, type, value);
        }
        
        @Override
        protected String computeObjectName() {
            return PythonNodes.computeObjectName(this);
        }
        
        protected String computeLogicalValue(PythonObject object, String type) {
            String logicalValue = PythonNodes.getLogicalValue(object, type);
            return logicalValue != null ? logicalValue : super.computeLogicalValue(object, type);
        }
        
        
        public PythonObjectNode createCopy() {
            return PythonNodes.createCopy(this);
        }
        
    }
    
    static class PythonObjectAttributeReferenceNode extends TruffleObjectReferenceNode.InstanceBased<PythonObject> implements PythonNode {
        
        PythonObjectAttributeReferenceNode(PythonObject object, String type, FieldValue value) {
            super(object, type, value);
        }
        
        @Override
        protected String computeObjectName() {
            return PythonNodes.computeObjectName(this);
        }
        
        protected String computeLogicalValue(PythonObject object, String type) {
            String logicalValue = PythonNodes.getLogicalValue(object, type);
            return logicalValue != null ? logicalValue : super.computeLogicalValue(object, type);
        }
        
        
        public PythonObjectNode createCopy() {
            return PythonNodes.createCopy(this);
        }
        
    }
    
    
    @ServiceProvider(service=HeapViewerRenderer.Provider.class)
    public static class PythonNodesRendererProvider extends HeapViewerRenderer.Provider {

        public boolean supportsView(HeapContext context, String viewID) {
            return true;
        }

        public void registerRenderers(Map<Class<? extends HeapViewerNode>, HeapViewerRenderer> renderers, HeapContext context) {
            PythonLanguage language = PythonLanguage.instance();
            Icon instanceIcon = language.createLanguageIcon(Icons.getIcon(LanguageIcons.INSTANCE));
            Icon packageIcon = language.createLanguageIcon(Icons.getIcon(LanguageIcons.PACKAGE));

            Heap heap = context.getFragment().getHeap();

            renderers.put(PythonNodes.PythonObjectNode.class, new TruffleObjectNode.Renderer(heap, instanceIcon));

            renderers.put(PythonNodes.PythonTypeNode.class, new TruffleTypeNode.Renderer(packageIcon));

            renderers.put(PythonNodes.PythonObjectFieldNode.class, new TruffleObjectFieldNode.Renderer(heap, instanceIcon));

            renderers.put(PythonNodes.PythonObjectReferenceNode.class, new TruffleObjectReferenceNode.Renderer(heap, instanceIcon));

            renderers.put(PythonNodes.PythonObjectAttributeReferenceNode.class, new TruffleObjectReferenceNode.Renderer(heap, instanceIcon, "attribute in")); // NOI18N
        }

    }
    
}
