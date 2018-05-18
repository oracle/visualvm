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
import org.graalvm.visualvm.heapviewer.truffle.TruffleObject;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerRenderer;
import javax.swing.Icon;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.ui.swing.renderer.LabelRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.MultiRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.ProfilerRenderer;

/**
 *
 * @author Jiri Sedlacek
 */
public interface TruffleLocalObjectNode<O extends TruffleObject> extends TruffleObjectNode<O> {
    
    
    public class InstanceBased<O extends TruffleObject.InstanceBased> extends TruffleObjectNode.InstanceBased<O> implements TruffleLocalObjectNode<O> {
        
        public InstanceBased(O object, String type) {
            super(object, type);
        }


        public InstanceNode createCopy() {
            return null;
        }
        
        protected void setupCopy(TruffleLocalObjectNode.InstanceBased copy) {
            super.setupCopy(copy);
        }
        
    }
    
    
    public static class Renderer extends MultiRenderer implements HeapViewerRenderer {
        
        private final LabelRenderer lvRenderer;
        private final TruffleObjectNode.Renderer dobjectRenderer;
        
        private final ProfilerRenderer[] renderers;
        
        public Renderer(Heap heap, Icon icon) {
            lvRenderer = new LabelRenderer() {
                public String toString() {
                    return getText() + " "; // NOI18N
                }
            };
            lvRenderer.setText("local object"); // NOI18N
            lvRenderer.setMargin(3, 3, 3, 1);
            
            dobjectRenderer = new TruffleObjectNode.Renderer(heap, icon);
            
            renderers = new ProfilerRenderer[] { lvRenderer, dobjectRenderer };
        }
        
        protected ProfilerRenderer[] valueRenderers() { return renderers; }
        
        public void setValue(Object value, int row) {
            dobjectRenderer.setValue(value, row);
        }
        
        public Icon getIcon() {
            return dobjectRenderer.getIcon();
        }

        public String getShortName() {
            String name = dobjectRenderer.getShortName();
            int nameIdx = name.lastIndexOf('['); // NOI18N
            if (nameIdx != -1) name = name.substring(0, nameIdx).trim();
            return /*lvRenderer + " " +*/ name;
        }
        
    }
    
}
