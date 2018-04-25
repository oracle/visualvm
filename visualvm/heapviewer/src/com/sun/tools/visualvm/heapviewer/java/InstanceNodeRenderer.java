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

package com.sun.tools.visualvm.heapviewer.java;

import java.awt.Image;
import javax.swing.ImageIcon;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.ui.swing.renderer.JavaNameRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.LabelRenderer;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.LanguageIcons;
import org.netbeans.modules.profiler.heapwalk.ui.icons.HeapWalkerIcons;
import com.sun.tools.visualvm.heapviewer.model.DataType;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNode;
import org.openide.util.ImageUtilities;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerRenderer;
import org.netbeans.lib.profiler.heap.Instance;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "InstanceNodeRenderer_LoopTo=loop to {0}"
})
public class InstanceNodeRenderer extends JavaNameRenderer implements HeapViewerRenderer {
    
    private static final ImageIcon ICON_INSTANCE = Icons.getImageIcon(LanguageIcons.INSTANCE);
    private static final ImageIcon ICON_ARRAY = Icons.getImageIcon(LanguageIcons.ARRAY);
    
    private static final Image IMAGE_LOOP = Icons.getImage(HeapWalkerIcons.LOOP);
    
    private final Heap heap;
    
    public InstanceNodeRenderer(Heap heap) {
        this.heap = heap;
    }
    
    public void setValue(Object value, int row) {
        if (value == null) {
            super.setValue(null, row);
            return;
        }
        
        HeapViewerNode loop = HeapViewerNode.getValue((HeapViewerNode)value, DataType.LOOP, heap);
        boolean isLoop = loop != null;
        InstanceNode node = isLoop ? (InstanceNode)loop : (InstanceNode)value;
        
        String name = node.getName(heap);
        if (name != null && !"null".equals(name)) { // NOI18N
            super.setValue(name, row);
            if (isLoop) super.setNormalValue(Bundle.InstanceNodeRenderer_LoopTo(super.getNormalValue()));
        } else {
            super.setValue(null, row);
            super.setNormalValue("null"); // NOI18N
        }
        
        String log = node.getLogicalValue(heap);
        if (log != null && !log.isEmpty()) setGrayValue(" : " + log); // NOI18N
        
        ImageIcon icon = getIcon(node.getInstance(), node.isGCRoot());
        if (isLoop) icon = new ImageIcon(ImageUtilities.mergeImages(icon.getImage(), IMAGE_LOOP, 0, 0));
        setIcon(icon);     
        
        setIconTextGap(isLoop ? 4 : 1);
        ((LabelRenderer)valueRenderers()[0]).setMargin(3, isLoop ? 3 : 0, 3, 0);
    }
    
    public String getShortName() {
        return getBoldValue();
    }
    
    
    protected ImageIcon getIcon(Instance instance, boolean isGCRoot) {
        return instance == null || !instance.getJavaClass().isArray() ? ICON_INSTANCE : ICON_ARRAY;
    }
    
}
