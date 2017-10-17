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
package com.sun.tools.visualvm.truffle.heapwalker.r;

import java.util.List;
import javax.swing.Icon;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.Value;
import org.netbeans.lib.profiler.ui.swing.renderer.LabelRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.MultiRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.NormalBoldGrayRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.ProfilerRenderer;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import org.netbeans.modules.profiler.heapwalker.v2.model.DataType;
import org.netbeans.modules.profiler.heapwalker.v2.model.HeapWalkerNode;
import org.netbeans.modules.profiler.heapwalker.v2.ui.HeapWalkerRenderer;

/**
 *
 * @author Jiri Sedlacek
 */
public class RObjectReferenceNode extends RObjectNode {
    
    private final FieldValue field;
    private final boolean rfield;
    
    private String fieldName;
    
    
    public RObjectReferenceNode(FieldValue field) {
        super(getRObject(field));
        this.field = field;
        rfield = field.getDefiningInstance().equals(getRObject().getInstance());
    }
    
    
    String getFieldName() {
        if (fieldName == null) fieldName = (field.getField().isStatic() ? "static " : "") + field.getField().getName();
        return fieldName;
    }
    
    FieldValue getField() {
        return field;
    }
    
    boolean isRField() {
        return rfield;
    }
    
    
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof RObjectReferenceNode)) return false;
        return field.equals(((RObjectReferenceNode)o).field);
    }

    public int hashCode() {
        return field.hashCode();
    }
    
    
    public RObjectNode createCopy() {
        return new RObjectNode(getRObject(), getType());
    }
    
    
    private static RObject getRObject(FieldValue field) {
        Instance instance = field.getDefiningInstance();
        
        if (RObject.isRObject(instance)) return new RObject(instance);
        
        List<Value> references = (List<Value>)instance.getReferences();
        for (Value reference : references) {
            instance = reference.getDefiningInstance();
            if (RObject.isRObject(instance)) return new RObject(instance);
        }
        
        throw new IllegalArgumentException("Illegal reference " + field);
    }
    
    
    public static class Renderer extends MultiRenderer implements HeapWalkerRenderer {
        
        private final NormalBoldGrayRenderer fieldRenderer;
        private final LabelRenderer inRenderer;
        private final RObjectNode.Renderer robjectRenderer;
        private final ProfilerRenderer[] renderers;
        
        private final Heap heap;
        
        public Renderer(Heap heap) {
            this.heap = heap;
            
            fieldRenderer = new NormalBoldGrayRenderer() {
                public void setValue(Object value, int row) {
                    RObjectReferenceNode node = (RObjectReferenceNode)value;
                    String name = node.getFieldName();
                    if (name.startsWith("static ")) {
                        setNormalValue("static ");
                        setBoldValue(name.substring("static ".length()));
                    } else {
                        setNormalValue("");
                        setBoldValue(name);
                    }
                    inRenderer.setText(node.isRField() ? "in" : "attribute in");
                    setIcon(Icons.getIcon(ProfilerIcons.NODE_REVERSE));
                }
            };
            
            inRenderer = new LabelRenderer();
//            inRenderer.setText("in");
            inRenderer.setMargin(3, 0, 3, 0);
            
            robjectRenderer = new RObjectNode.Renderer(heap);
            
            renderers = new ProfilerRenderer[] { fieldRenderer, inRenderer, robjectRenderer };
        }

        protected ProfilerRenderer[] valueRenderers() {
            return renderers;
        }
        
        public void setValue(Object value, int row) {
            HeapWalkerNode node = (HeapWalkerNode)value;
            HeapWalkerNode loop = HeapWalkerNode.getValue(node, DataType.LOOP, heap);
            if (loop != null) node = loop;
            
            fieldRenderer.setValue(node, row);
            robjectRenderer.setValue(value, row);
        }
        
        
        public Icon getIcon() {
            return fieldRenderer.getIcon();
        }
        
        public String getShortName() {
            return fieldRenderer.toString();
        }
        
    }
    
}
