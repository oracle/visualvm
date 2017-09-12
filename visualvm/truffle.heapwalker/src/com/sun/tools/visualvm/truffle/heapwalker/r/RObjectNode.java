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

import javax.swing.Icon;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.ui.swing.renderer.NormalBoldGrayRenderer;
import org.netbeans.modules.profiler.api.icons.LanguageIcons;
import org.netbeans.modules.profiler.heapwalk.details.api.DetailsSupport;
import org.netbeans.modules.profiler.heapwalker.v2.java.InstanceNode;
import org.netbeans.modules.profiler.heapwalker.v2.model.DataType;
import org.netbeans.modules.profiler.heapwalker.v2.ui.HeapWalkerRenderer;

/**
 *
 * @author Jiri Sedlacek
 */
class RObjectNode extends InstanceNode {
    
    private final RObject robject;
    private final String type;
    
    private String nameString;
//    private String logicalValue;
    
    
    public RObjectNode(RObject robject) {
        this(robject, robject.getType());
    }
    
    public RObjectNode(RObject robject, String type) {
        super(robject.getInstance());
        this.robject = robject;
        this.type = type;
    }
    
    
    public RObject getRObject() {
        return robject;
    }
    
    public String getName(Heap heap) {
        if (nameString == null) {
            nameString = getType();
            nameString = nameString.substring(nameString.lastIndexOf('.') + 1) + "#" + getInstance().getInstanceNumber();
        }
        return nameString;
    }
    
    public String getType() {
        return type;
    }
    
//    public String getLogicalValue(Heap heap) {
//        if (logicalValue == null) logicalValue = computeLogicalValue(robject, type, heap);
//        return logicalValue.isEmpty() ? null : logicalValue;
//    }
    
//    // TODO: make this an internal API similar to DetailsSupport.getDetailsString
//    protected String computeLogicalValue(RObject robject, String type, Heap heap) {
//        return null;
//    }
    
    
    protected Object getValue(DataType type, Heap heap) {
        if (type == DataType.OWN_SIZE) return robject.getSize();
        
        if (type == RObject.DATA_TYPE) return getRObject();
        
        return super.getValue(type, heap);
    }
    
    
    public static class Renderer extends NormalBoldGrayRenderer implements HeapWalkerRenderer {
        
        private static final Icon ICON = RSupport.createBadgedIcon(LanguageIcons.INSTANCE);
        
        private final Heap heap;
        
        public Renderer(Heap heap) {
            this.heap = heap;
        }
        
        public void setValue(Object value, int row) {
            RObjectNode node = (RObjectNode)value;
            
            setNormalValue("");
            setBoldValue(node.getName(heap));
            
            String logValue = DetailsSupport.getDetailsString(node.getInstance(), heap);
            setGrayValue(logValue == null ? "" : " : " + logValue);
            
            setIcon(ICON);
        }
        
        public String getShortName() {
            return getBoldValue();
        }
        
    }
    
}
