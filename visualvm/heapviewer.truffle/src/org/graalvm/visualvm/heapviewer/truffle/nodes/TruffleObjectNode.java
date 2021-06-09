/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Image;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.ui.swing.renderer.LabelRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.NormalBoldGrayRenderer;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.heapwalk.ui.icons.HeapWalkerIcons;
import org.graalvm.visualvm.heapviewer.java.InstanceNode;
import org.graalvm.visualvm.heapviewer.model.DataType;
import org.graalvm.visualvm.heapviewer.truffle.TruffleObject;
import org.graalvm.visualvm.heapviewer.truffle.TruffleType;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerRenderer;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.api.DetailsSupport;
import org.graalvm.visualvm.lib.ui.results.PackageColorer;
import org.openide.util.ImageUtilities;

/**
 *
 * @author Jiri Sedlacek
 */
public interface TruffleObjectNode<O extends TruffleObject> {
    
    public O getTruffleObject();
    
    public String getName();
    
    public String getObjectName();
    
    public String getTypeName();
    
    public String getLogicalValue();
        
        
    public static abstract class InstanceBased<O extends TruffleObject.InstanceBased> extends InstanceNode implements TruffleObjectNode<O> {
    
        private final O object;
        private final String typeName;

        private String objectNameString;
        private String logicalValue;


        public InstanceBased(O object, String typeName) {
            super(object.getInstance());
            this.object = object;
            this.typeName = typeName;
        }


        public O getTruffleObject() {
            return object;
        }
        
        public String getName() {
            return getObjectName();
        }

        public String getObjectName() {
            if (objectNameString == null) objectNameString = computeObjectName();
            return objectNameString;
        }

        protected String computeObjectName() {
            return getTypeName() + "#" + getInstance().getInstanceNumber(); // NOI18N
        }

        public String getTypeName() {
            return typeName;
        }

        public String getLogicalValue() {
            if (logicalValue == null) {
                logicalValue = computeLogicalValue(object, typeName);
                if (logicalValue == null) logicalValue = ""; // NOI18N
            }
            return logicalValue.isEmpty() ? null : logicalValue;
        }

        // TODO: make this an internal API similar to DetailsSupport.getDetailsString
        protected String computeLogicalValue(O object, String type) {
            return DetailsSupport.getDetailsString(object.getInstance());
        }

        public long getOwnSize() {
            return object.getSize();
        }
        
        public long getRetainedSize(Heap heap) {
            return DataType.RETAINED_SIZE.valuesAvailable(heap) ?
               object.getRetainedSize() : DataType.RETAINED_SIZE.getNotAvailableValue();
        }


        protected Object getValue(DataType type, Heap heap) {
            if (type == TruffleType.TYPE_NAME) return getTypeName();
            if (type == TruffleObject.DATA_TYPE) return getTruffleObject();

            return super.getValue(type, heap);
        }


//        public InstanceNode createCopy() {
//            TruffleObjectNode copy = new TruffleObjectNode(object, type);
//            setupCopy(copy);
//            return copy;
//        }

        protected void setupCopy(TruffleObjectNode.InstanceBased copy) {
            super.setupCopy(copy);
            copy.objectNameString = objectNameString;
            copy.logicalValue = logicalValue;
        }
    
    }
    
    
    public static class Renderer extends NormalBoldGrayRenderer implements HeapViewerRenderer {
        
        private static final Image IMAGE_LOOP = Icons.getImage(HeapWalkerIcons.LOOP);
        
        private final Icon icon;
        
        private final Heap heap;
        
        public Renderer(Heap heap, Icon icon) {
            this.heap = heap;
            this.icon = icon;
        }
        
        public void setValue(Object value, int row) {
            TruffleObjectNode node = (TruffleObjectNode)value;
            
            String name = node == null ? "" : node.getObjectName(); // NOI18N
            if (name != null && !"null".equals(name)) { // NOI18N
                super.setNormalValue(""); // NOI18N
                super.setBoldValue(name);
            } else {
                super.setNormalValue("null"); // NOI18N
                super.setBoldValue(null);
            }
            
            String logValue = node == null ? null : node.getLogicalValue();
            setGrayValue(logValue == null ? "" : " : " + logValue); // NOI18N
            
            setIcon(icon);   
            
            setIconTextGap(0);
            ((LabelRenderer)valueRenderers()[0]).setMargin(3, 0, 3, 0);
            
            setCustomForeground(PackageColorer.getForeground(getBoldValue()));
        }
        
        public void flagLoopTo() {
            Image loopImage = ImageUtilities.icon2Image(getIcon());
            setIcon(new ImageIcon(ImageUtilities.mergeImages(loopImage, IMAGE_LOOP, 0, 0)));
            setIconTextGap(4);

            ((LabelRenderer)valueRenderers()[0]).setMargin(3, 1, 3, 0);
        }
        
        public String getShortName() {
            return getBoldValue();
        }
        
        
        protected boolean supportsCustomGrayForeground() {
            return false;
        }
        
    }
    
}
