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
package com.sun.tools.visualvm.heapviewer.truffle.r;

import javax.swing.Icon;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.ui.swing.renderer.LabelRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.MultiRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.NormalBoldGrayRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.ProfilerRenderer;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import com.sun.tools.visualvm.heapviewer.model.DataType;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNode;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerRenderer;

/**
 *
 * @author Jiri Sedlacek
 */
public class RObjectFieldNode extends RObjectNode {
    
    private final FieldValue field;
    
    private String fieldName;
    
    
    public RObjectFieldNode(RObject robject, FieldValue field) {
        super(robject);
        this.field = field;
    }
    
    
    String getFieldName() {
        if (fieldName == null) fieldName = (field.getField().isStatic() ? "static " : "") + field.getField().getName();
        return fieldName;
    }
    
    FieldValue getField() {
        return field;
    }
    
    
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof RObjectFieldNode)) return false;
        return field.equals(((RObjectFieldNode)o).field);
    }

    public int hashCode() {
        return field.hashCode();
    }
    
    
    public RObjectNode createCopy() {
        return new RObjectNode(getRObject(), getType());
    }
    
    
    public static class Renderer extends MultiRenderer implements HeapViewerRenderer {
        
        private final NormalBoldGrayRenderer fieldRenderer;
        private final LabelRenderer equalsRenderer;
        private final RObjectNode.Renderer robjectRenderer;
        private final ProfilerRenderer[] renderers;
        
        private final Heap heap;
        
        public Renderer(Heap heap) {
            this.heap = heap;
            
            fieldRenderer = new NormalBoldGrayRenderer() {
                public void setValue(Object value, int row) {
                    RObjectFieldNode node = (RObjectFieldNode)value;
                    String name = node.getFieldName();
                    if (name.startsWith("static ")) {
                        setNormalValue("static ");
                        setBoldValue(name.substring("static ".length()));
                    } else {
                        setNormalValue("");
                        setBoldValue(name);
                    }
                    setIcon(Icons.getIcon(ProfilerIcons.NODE_FORWARD));
                }
            };
            
            equalsRenderer = new LabelRenderer();
            equalsRenderer.setText("=");
            equalsRenderer.setMargin(3, 0, 3, 0);
            
            robjectRenderer = new RObjectNode.Renderer(heap);
            
            renderers = new ProfilerRenderer[] { fieldRenderer, equalsRenderer, robjectRenderer };
        }

        protected ProfilerRenderer[] valueRenderers() {
            return renderers;
        }
        
        public void setValue(Object value, int row) {
            HeapViewerNode node = (HeapViewerNode)value;
            HeapViewerNode loop = HeapViewerNode.getValue(node, DataType.LOOP, heap);
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
