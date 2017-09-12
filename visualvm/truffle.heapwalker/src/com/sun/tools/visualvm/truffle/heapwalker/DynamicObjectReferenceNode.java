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
package com.sun.tools.visualvm.truffle.heapwalker;

import java.util.ArrayList;
import java.util.List;
import javax.swing.Icon;
import org.netbeans.lib.profiler.heap.ArrayItemValue;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.ObjectFieldValue;
import org.netbeans.lib.profiler.heap.Value;
import org.netbeans.lib.profiler.ui.swing.renderer.LabelRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.MultiRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.NormalBoldGrayRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.ProfilerRenderer;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import org.netbeans.modules.profiler.heapwalk.details.api.DetailsSupport;
import org.netbeans.modules.profiler.heapwalker.v2.ui.HeapWalkerRenderer;

/**
 *
 * @author Jiri Sedlacek
 */
public class DynamicObjectReferenceNode extends DynamicObjectNode {
    
    private final FieldValue field;
    
    private String fieldName;
    private final Heap heap;
    
    public DynamicObjectReferenceNode(DynamicObject dobject, FieldValue value, Heap heap) {
        super(dobject, heap);
        this.field = value;
        this.heap = heap;
    }
    
    
    String getFieldName() {
        if (fieldName == null) fieldName = (field.getField().isStatic() ? "static " : "") + field.getField().getName();
        return fieldName;
    }
//    String getFieldName() {
//        if (fieldName == null) fieldName = computeFieldName(getDynamicObject(), value);
//        return fieldName;
//    }
    
    FieldValue getField() {
        return field;
    }
    
    
    // TODO: should use some kind of API?
    private String computeFieldName(DynamicObject dobject, Value value) {
        if (value instanceof ArrayItemValue) return "[" + ((ArrayItemValue)value).getIndex() + "]";
        
        if (value instanceof ObjectFieldValue) {
            ObjectFieldValue ovalue = (ObjectFieldValue)value;
            Instance toSearch = ovalue.getInstance();
            
            System.err.println(">>> Computing referrer for " + DetailsSupport.getDetailsString(toSearch, heap));
//            System.err.println(">>>     VALUES " + DetailsSupport.getDetailsString(ovalue.getInstance(), heap) + " | " + DetailsSupport.getDetailsString(ovalue.getDefiningInstance(), heap));
            
            List<FieldValue> fields = new ArrayList(dobject.getFieldValues());
            fields.addAll(dobject.getStaticFieldValues());
            
//            System.err.println(">>> SEARCHING for referrer of instance " + instance);
            for (FieldValue field : fields) {
                if (!(field instanceof ObjectFieldValue)) continue;
                
                System.err.println(">>> ANALYZING FIELD " + field.getField().getName() + " value " + DetailsSupport.getDetailsString(((ObjectFieldValue)field).getInstance(), heap));
                if (toSearch.equals(((ObjectFieldValue)field).getInstance()))
                    return field.getField().getName();
            }
        }
        
        return "<unknown field>";
    }
    
    
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof DynamicObjectReferenceNode)) return false;
        return field.equals(((DynamicObjectReferenceNode)o).field);
    }

    public int hashCode() {
        return field.hashCode();
    }
    
    
    public static class Renderer extends MultiRenderer implements HeapWalkerRenderer {
        
        private final NormalBoldGrayRenderer fieldRenderer;
        private final LabelRenderer inRenderer;
        private final DynamicObjectNode.Renderer dobjectRenderer;
        private final ProfilerRenderer[] renderers;
        
        public Renderer(Heap heap, Icon icon) {
            fieldRenderer = new NormalBoldGrayRenderer() {
                public void setValue(Object value, int row) {
                    DynamicObjectReferenceNode node = (DynamicObjectReferenceNode)value;
                    String name = node.getFieldName();
                    if (name.startsWith("static ")) {
                        setNormalValue("static ");
                        setBoldValue(name.substring("static ".length()));
                    } else {
                        setNormalValue("");
                        setBoldValue(name);
                    }
                    setIcon(Icons.getIcon(ProfilerIcons.NODE_REVERSE));
                }
            };
//            fieldRenderer.setFont(fieldRenderer.getFont().deriveFont(Font.BOLD));
//            fieldRenderer.setIcon(Icons.getIcon(ProfilerIcons.NODE_REVERSE));
            
            inRenderer = new LabelRenderer();
            inRenderer.setText("in");
            inRenderer.setMargin(3, 0, 3, 0);
            
            dobjectRenderer = new DynamicObjectNode.Renderer(heap, icon);
            
            renderers = new ProfilerRenderer[] { fieldRenderer, inRenderer, dobjectRenderer };
        }

        protected ProfilerRenderer[] valueRenderers() {
            return renderers;
        }
        
        public void setValue(Object value, int row) {
            fieldRenderer.setValue(value, row);
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
