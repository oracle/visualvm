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

import javax.swing.Icon;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.ui.swing.renderer.NormalBoldGrayRenderer;
import org.netbeans.modules.profiler.heapwalker.v2.java.InstanceNode;
import org.netbeans.modules.profiler.heapwalker.v2.model.DataType;
import org.netbeans.modules.profiler.heapwalker.v2.ui.HeapWalkerRenderer;

/**
 *
 * @author Jiri Sedlacek
 */
public class DynamicObjectNode extends InstanceNode {
    
    private final DynamicObject dobject;
    private final String type;
    
    private String nameString;
    private String logicalValue;
    
    
    public DynamicObjectNode(DynamicObject dobject, Heap heap) {
        this(dobject, dobject.getType(heap));
    }
    
    public DynamicObjectNode(DynamicObject dobject, String type) {
        super(dobject.getInstance());
        this.dobject = dobject;
        this.type = type;
    }
    
    
    public DynamicObject getDynamicObject() {
        return dobject;
    }
    
    public String getName(Heap heap) {
        if (nameString == null) nameString = getType() + "#" + getInstance().getInstanceNumber();
        return nameString;
    }
    
    public String getType() {
        return type;
    }
    
    public String getLogicalValue(Heap heap) {
        if (logicalValue == null) logicalValue = computeLogicalValue(dobject, type, heap);
        return logicalValue.isEmpty() ? null : logicalValue;
    }
    
    // TODO: make this an internal API similar to DetailsSupport.getDetailsString
    protected String computeLogicalValue(DynamicObject dobject, String type, Heap heap) {
        return "shape #" + dobject.getShape().getInstanceNumber();
    }
    
    
    protected Object getValue(DataType type, Heap heap) {
        if (type == DynamicObject.DATA_TYPE) return getDynamicObject();
        
        return super.getValue(type, heap);
    }
    
    
    public static class Renderer extends NormalBoldGrayRenderer implements HeapWalkerRenderer {
        
        private final Icon icon;       
        private final Heap heap;
        
        public Renderer(Heap heap, Icon icon) {
            this.heap = heap;
            this.icon = icon;
            
//            ((LabelRenderer)valueRenderers()[0]).setMargin(3, 3, 3, 0);
        }
        
        public void setValue(Object value, int row) {
            DynamicObjectNode node = (DynamicObjectNode)value;
            
            setBoldValue(node.getName(heap));
            
//            setBoldValue(" " + node.getLogicalValue(heap));
//            
//            setGrayValue(" (" + node.getShapeString() + ")");

            setGrayValue(" : " + node.getLogicalValue(heap));
            
            setIcon(icon);
            setIconTextGap(0);
        }
        
        public String getShortName() {
            return getBoldValue();
        }
        
    }
    
}
