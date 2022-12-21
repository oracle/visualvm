/*
 * Copyright (c) 2017, 2022, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.heapviewer.java;

import java.awt.Image;
import javax.swing.ImageIcon;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerRenderer;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.LanguageIcons;
import org.graalvm.visualvm.lib.profiler.heapwalk.ui.icons.HeapWalkerIcons;
import org.graalvm.visualvm.lib.ui.swing.renderer.JavaNameRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.LabelRenderer;
import org.openide.util.ImageUtilities;

/**
 *
 * @author Jiri Sedlacek
 */
public class InstanceNodeRenderer extends JavaNameRenderer implements HeapViewerRenderer {
    
    private static final ImageIcon ICON_INSTANCE = Icons.getImageIcon(LanguageIcons.INSTANCE);
    private static final ImageIcon ICON_ARRAY = Icons.getImageIcon(LanguageIcons.ARRAY);
    
    private static final Image IMAGE_LOOP = Icons.getImage(HeapWalkerIcons.LOOP);
    
    public InstanceNodeRenderer(Heap heap) {
    }
    
    public void setValue(Object value, int row) {
        if (value == null) {
            super.setValue(null, row);
            return;
        }
        
        InstanceNode node = (InstanceNode)value;
        
        String name = node.getName();
        if (name != null && !"null".equals(name)) { // NOI18N
            super.setValue(name, row);
        } else {
            super.setValue(null, row);
            super.setNormalValue("null"); // NOI18N
        }
        
        String log = node.getLogicalValue();
        if (log != null && !log.isEmpty()) setGrayValue(" : " + log); // NOI18N
        
        ImageIcon icon = getIcon(node.getInstance(), node.isGCRoot());
        setIcon(icon);
        setIconTextGap(1);
        
        ((LabelRenderer)valueRenderers()[0]).setMargin(3, 0, 3, 0);
    }
    
    public void flagLoopTo() {
        ImageIcon icon = (ImageIcon)getIcon();
        icon = new ImageIcon(ImageUtilities.mergeImages(icon.getImage(), IMAGE_LOOP, 0, 0));
        setIcon(icon);
        setIconTextGap(4);
        
        ((LabelRenderer)valueRenderers()[0]).setMargin(3, 1, 3, 0);
    }
    
    public String getShortName() {
        return getBoldValue();
    }
    
    
    protected boolean supportsCustomGrayForeground() {
        return false;
    }
    
    
    protected ImageIcon getIcon(Instance instance, boolean isGCRoot) {
        return instance == null || !instance.getJavaClass().isArray() ? ICON_INSTANCE : ICON_ARRAY;
    }
    
}
