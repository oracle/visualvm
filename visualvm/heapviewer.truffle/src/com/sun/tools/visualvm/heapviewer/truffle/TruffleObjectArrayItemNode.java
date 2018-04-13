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
package com.sun.tools.visualvm.heapviewer.truffle;

import com.sun.tools.visualvm.heapviewer.java.InstanceNode;
import com.sun.tools.visualvm.heapviewer.model.DataType;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNode;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerRenderer;
import javax.swing.Icon;
import org.netbeans.lib.profiler.heap.ArrayItemValue;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.ui.swing.renderer.LabelRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.MultiRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.NormalBoldGrayRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.ProfilerRenderer;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;

/**
 *
 * @author Jiri Sedlacek
 */
public interface TruffleObjectArrayItemNode<O extends TruffleObject> {
    
    public ArrayItemValue getItem();
    
    public String getItemName();
    
    
    public static abstract class InstanceBased<O extends TruffleObject.InstanceBased> extends TruffleObjectNode.InstanceBased<O> implements TruffleObjectArrayItemNode<O> {
        
        private ArrayItemValue item;
        
        
        public InstanceBased(O object, String type, ArrayItemValue item) {
            super(object, type);
            this.item = item;
        }
        
        
        public String getItemName() {
            return "[" + item.getIndex() + "]";
        }

        public ArrayItemValue getItem() {
            return item;
        }


        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof TruffleObjectArrayItemNode.InstanceBased)) return false;
            return item.equals(((TruffleObjectArrayItemNode.InstanceBased)o).item);
        }

        public int hashCode() {
            return item.hashCode();
        }


        public InstanceNode createCopy() {
            return null;
        }
        
        protected void setupCopy(TruffleObjectArrayItemNode.InstanceBased copy) {
            super.setupCopy(copy);
            copy.item = item;
        }
        
    }
    
    
    public static class Renderer extends MultiRenderer implements HeapViewerRenderer {
        
        private final NormalBoldGrayRenderer fieldRenderer;
        private final LabelRenderer equalsRenderer;
        private final TruffleObjectNode.Renderer dobjectRenderer;
        private final ProfilerRenderer[] renderers;
        
        private final Heap heap;
        
        public Renderer(Heap heap, Icon icon) {
            this(heap, icon, "=");
        }
        
        public Renderer(Heap heap, Icon icon, String divider) {
            this.heap = heap;
            
            fieldRenderer = new NormalBoldGrayRenderer() {
                public void setValue(Object value, int row) {
                    TruffleObjectArrayItemNode node = (TruffleObjectArrayItemNode)value;
                    String name = node.getItemName();
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
            equalsRenderer.setText(divider);
            equalsRenderer.setMargin(3, 0, 3, 0);
            
            dobjectRenderer = new TruffleObjectNode.Renderer(heap, icon);
            
            renderers = new ProfilerRenderer[] { fieldRenderer, equalsRenderer, dobjectRenderer };
        }

        protected ProfilerRenderer[] valueRenderers() {
            return renderers;
        }
        
        public void setValue(Object value, int row) {
            HeapViewerNode node = (HeapViewerNode)value;
            HeapViewerNode loop = HeapViewerNode.getValue(node, DataType.LOOP, heap);
            if (loop != null) node = loop;
            
            fieldRenderer.setValue(node, row);
            dobjectRenderer.setValue(value, row);
        }
        
        
        public Icon getIcon() {
            return fieldRenderer.getIcon();
        }
        
        public String getShortName() {
            return fieldRenderer.toString();
        }
        
    }
    
}
