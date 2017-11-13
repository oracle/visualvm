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

import javax.swing.Icon;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.ui.swing.renderer.LabelRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.MultiRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.NormalBoldGrayRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.ProfilerRenderer;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.LanguageIcons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import com.sun.tools.visualvm.heapviewer.model.DataType;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNode;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerRenderer;

/**
 *
 * @author Jiri Sedlacek
 */
public class InstanceReferenceNodeRenderer extends MultiRenderer implements HeapViewerRenderer {
    
    protected static final Icon ICON_PRIMITIVE = Icons.getIcon(LanguageIcons.PRIMITIVE);
    protected static final Icon ICON_INSTANCE = Icons.getIcon(LanguageIcons.INSTANCE);
    protected static final Icon ICON_ARRAY = Icons.getIcon(LanguageIcons.ARRAY);
    
    protected final NormalBoldGrayRenderer nameRenderer;
    protected final LabelRenderer equalsRenderer;
    
    private final InstanceNodeRenderer instanceRenderer;
    private final ProfilerRenderer[] renderers;
    
    protected final Heap heap;

    
    public InstanceReferenceNodeRenderer(Heap heap) {
        this.heap = heap;
        nameRenderer = new NormalBoldGrayRenderer() {
            public void setValue(Object value, int row) {
                InstanceReferenceNode node = (InstanceReferenceNode) value;
                String name = node.getFieldName();
                if (name.startsWith("static ")) {
                    setNormalValue("static ");
                    setBoldValue(name.substring("static ".length()));
                } else {
                    setNormalValue("");
                    setBoldValue(name);
                }
                setIcon(Icons.getIcon(InstanceNode.Mode.INCOMING_REFERENCE.equals(node.getMode()) ? ProfilerIcons.NODE_REVERSE : ProfilerIcons.NODE_FORWARD));
            }
        };
        equalsRenderer = new LabelRenderer() {
            public void setValue(Object value, int row) {
                InstanceReferenceNode node = (InstanceReferenceNode) value;
                if (InstanceNode.Mode.INCOMING_REFERENCE.equals(node.getMode())) {
                    setText("in");
                    equalsRenderer.setMargin(3, 2, 3, 0);
                } else {
                    setText("=");
                    equalsRenderer.setMargin(3, 0, 3, 0);
                }
            }
        };
        instanceRenderer = new InstanceNodeRenderer(heap);
        renderers = new ProfilerRenderer[]{nameRenderer, equalsRenderer, instanceRenderer};
    }

    public Icon getIcon() {
        return nameRenderer.getIcon();
    }

    public String getShortName() {
        return nameRenderer.toString();
    }

    protected ProfilerRenderer[] valueRenderers() {
        return renderers;
    }

    public void setValue(Object value, int row) {
        HeapViewerNode node = (HeapViewerNode) value;
        HeapViewerNode loop = HeapViewerNode.getValue(node, DataType.LOOP, heap);
        if (loop != null) {
            node = loop;
        }
        nameRenderer.setValue(node, row);
        equalsRenderer.setValue(node, row);
        instanceRenderer.setValue(value, row);
    }
    
}
