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

import java.awt.Image;
import javax.swing.ImageIcon;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.ui.swing.renderer.LabelRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.NormalBoldGrayRenderer;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.LanguageIcons;
import org.netbeans.modules.profiler.heapwalk.details.api.DetailsSupport;
import org.netbeans.modules.profiler.heapwalk.ui.icons.HeapWalkerIcons;
import org.netbeans.modules.profiler.heapwalker.v2.java.InstanceNode;
import org.netbeans.modules.profiler.heapwalker.v2.model.DataType;
import org.netbeans.modules.profiler.heapwalker.v2.model.HeapWalkerNode;
import org.netbeans.modules.profiler.heapwalker.v2.ui.HeapWalkerRenderer;
import org.openide.util.ImageUtilities;

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
    
    
    public RObjectNode createCopy() {
        RObjectNode copy = new RObjectNode(robject, type);
        setupCopy(copy);
        return copy;
    }
    
    protected void setupCopy(RObjectNode copy) {
        super.setupCopy(copy);
    }
    
    
    public static class Renderer extends NormalBoldGrayRenderer implements HeapWalkerRenderer {
        
        private static final ImageIcon ICON = RSupport.createBadgedIcon(LanguageIcons.INSTANCE);
        private static final Image IMAGE_LOOP = Icons.getImage(HeapWalkerIcons.LOOP);
        
        private final Heap heap;
        
        public Renderer(Heap heap) {
            this.heap = heap;
        }
        
        public void setValue(Object value, int row) {
            HeapWalkerNode loop = HeapWalkerNode.getValue((HeapWalkerNode)value, DataType.LOOP, heap);
            boolean isLoop = loop != null;
            RObjectNode node = isLoop ? (RObjectNode)loop : (RObjectNode)value;
            
            String name = node.getName(heap);
            if (name != null && !"null".equals(name)) {
                super.setNormalValue(isLoop ? "loop to " : "");
                super.setBoldValue(name);
            } else {
                super.setNormalValue("null");
                super.setBoldValue(null);
            }
            
            String logValue = DetailsSupport.getDetailsString(node.getInstance(), heap);
            setGrayValue(logValue == null ? "" : " : " + logValue);
            
            setIcon(isLoop ? new ImageIcon(ImageUtilities.mergeImages(ICON.getImage(), IMAGE_LOOP, 0, 0)) : ICON);   
            
            setIconTextGap(isLoop ? 4 : 1);
            ((LabelRenderer)valueRenderers()[0]).setMargin(3, isLoop ? 3 : 0, 3, 0);
        }
        
        public String getShortName() {
            return getBoldValue();
        }
        
    }
    
}
