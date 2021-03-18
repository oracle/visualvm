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
package org.graalvm.visualvm.heapviewer.truffle.nodes;

import org.graalvm.visualvm.heapviewer.java.InstanceNode;
import org.graalvm.visualvm.heapviewer.model.DataType;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNode;
import org.graalvm.visualvm.heapviewer.truffle.TruffleObject;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerRenderer;
import javax.swing.Icon;
import org.graalvm.visualvm.lib.jfluid.heap.FieldValue;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.ui.swing.renderer.LabelRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.MultiRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.NormalBoldGrayRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.ProfilerRenderer;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.ProfilerIcons;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
public interface TruffleObjectFieldNode<O extends TruffleObject> {
    
    public FieldValue getField();
    
    public String getFieldName();
    
    
    public static abstract class InstanceBased<O extends TruffleObject.InstanceBased> extends TruffleObjectNode.InstanceBased<O> implements TruffleObjectFieldNode<O> {
        
        private FieldValue field;
    
        private String fieldName;
        
        
        public InstanceBased(O object, String type, FieldValue field) {
            super(object, type);
            this.field = field;
        }
        
        
        @Override
        public String getName() {
            return getFieldName() + " = " + getObjectName(); // NOI18N
        }
        
        
        public FieldValue getField() {
            return field;
        }
        
        public String getFieldName() {
            if (fieldName == null) fieldName = computeFieldName(field);
            return fieldName;
        }
        
        protected String computeFieldName(FieldValue field) {
            return (field.getField().isStatic() ? "static " : "") + field.getField().getName(); // NOI18N
        }


        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof TruffleObjectFieldNode.InstanceBased)) return false;
            return field.equals(((TruffleObjectFieldNode.InstanceBased)o).field);
        }

        public int hashCode() {
            return field.hashCode();
        }


        public InstanceNode createCopy() {
            return null;
        }
        
        protected void setupCopy(TruffleObjectFieldNode.InstanceBased copy) {
            super.setupCopy(copy);
            copy.field = field;
            copy.fieldName = fieldName;
        }
        
    }
    
    
    @NbBundle.Messages({
        "TruffleObjectFieldNodeRenderer_LoopTo=loop to"
    })
    public static class Renderer extends MultiRenderer implements HeapViewerRenderer {
        
        private final NormalBoldGrayRenderer fieldRenderer;
        private final LabelRenderer equalsRenderer;
        private final LabelRenderer loopToRenderer;
        private final TruffleObjectNode.Renderer dobjectRenderer;
        private final ProfilerRenderer[] renderers;
        
        private final Heap heap;
        
        public Renderer(Heap heap, Icon icon) {
            this(heap, icon, "="); // NOI18N
        }
        
        public Renderer(Heap heap, Icon icon, String divider) {
            this.heap = heap;
            
            fieldRenderer = new NormalBoldGrayRenderer() {
                public void setValue(Object value, int row) {
                    TruffleObjectFieldNode node = (TruffleObjectFieldNode)value;
                    String name = node.getFieldName();
                    if (name.startsWith("static ")) { // NOI18N
                        setNormalValue("static "); // NOI18N
                        setBoldValue(name.substring("static ".length())); // NOI18N
                    } else {
                        setNormalValue(""); // NOI18N
                        setBoldValue(name);
                    }
                    setIcon(Icons.getIcon(ProfilerIcons.NODE_FORWARD));
                }
            };
            
            equalsRenderer = new LabelRenderer() {
                public String toString() {
                    return " " + getText() + " "; // NOI18N
                }
            };
            equalsRenderer.setText(divider);
            equalsRenderer.setMargin(3, 0, 3, 0);
            
            loopToRenderer = new LabelRenderer() {
                public void setValue(Object value, int row) {
                    setVisible(value != null);
                }
                public String toString() {
                    return getText() + " "; // NOI18N
                }
            };
            loopToRenderer.setText(Bundle.TruffleObjectFieldNodeRenderer_LoopTo());
            
            dobjectRenderer = new TruffleObjectNode.Renderer(heap, icon);
            
            renderers = new ProfilerRenderer[] { fieldRenderer, equalsRenderer, loopToRenderer, dobjectRenderer };
        }

        protected ProfilerRenderer[] valueRenderers() {
            return renderers;
        }
        
        public void setValue(Object value, int row) {
            HeapViewerNode node = (HeapViewerNode)value;
            HeapViewerNode loop = HeapViewerNode.getValue(node, DataType.LOOP, heap);
            if (loop != null) node = loop;
            
            fieldRenderer.setValue(node, row);
            loopToRenderer.setValue(loop, row);
            dobjectRenderer.setValue(node, row);
            
            if (loopToRenderer.isVisible()) dobjectRenderer.flagLoopTo();
        }
        
        
        public Icon getIcon() {
            return fieldRenderer.getIcon();
        }
        
        public String getShortName() {
            return fieldRenderer.toString();
        }
        
    }
    
}
