/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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

import javax.swing.Icon;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerRenderer;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.LanguageIcons;
import org.graalvm.visualvm.lib.profiler.api.icons.ProfilerIcons;
import org.graalvm.visualvm.lib.ui.swing.renderer.LabelRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.MultiRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.NormalBoldGrayRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.ProfilerRenderer;

/**
 *
 * @author Jiri Sedlacek
 */
public class PrimitiveNodeRenderer extends MultiRenderer implements HeapViewerRenderer {
    
    private final NormalBoldGrayRenderer nameRenderer;
    private final LabelRenderer equalsRenderer;
    private final NormalBoldGrayRenderer valueRenderer;
    private final ProfilerRenderer[] renderers;
    
    public PrimitiveNodeRenderer() {
        nameRenderer = new NormalBoldGrayRenderer() {
            public void setValue(Object value, int row) {
                String name = ((PrimitiveNode)value).getFieldName();
                if (name.startsWith("static ")) { // NOI18N
                    setNormalValue("static "); // NOI18N
                    setBoldValue(name.substring("static ".length())); // NOI18N
                } else {
                    setNormalValue(""); // NOI18N
                    setBoldValue(name);
                }
                setIcon(Icons.getIcon(ProfilerIcons.NODE_FORWARD));
            }
        };
        
        equalsRenderer = new LabelRenderer() {
            public String toString() {
                return " " + getText() + " "; // NOI18N
            }
        };
        equalsRenderer.setText("="); // NOI18N
        equalsRenderer.setMargin(3, 0, 3, 0);
        
        valueRenderer = new NormalBoldGrayRenderer() {
            public void setValue(Object value, int row) {
                PrimitiveNode node = (PrimitiveNode)value;

                setNormalValue(node.getType());
                setBoldValue(node.getValue());

                setIcon(Icons.getIcon(LanguageIcons.PRIMITIVE));
                setIconTextGap(1);

                ((LabelRenderer)valueRenderers()[0]).setMargin(3, 0, 3, 3);
            }
            public String toString() {
                return getNormalValue() + " " + getBoldValue(); // NOI18N
            }
        };

        renderers = new ProfilerRenderer[] { nameRenderer, equalsRenderer, valueRenderer };
    }
    
    public Icon getIcon() {
        return nameRenderer.getIcon();
    }

    public String getShortName() {
        return nameRenderer.toString();
    }
    
    protected ProfilerRenderer[] valueRenderers() { return renderers; }
        
    public void setValue(Object value, int row) {
        nameRenderer.setValue(value, row);
        valueRenderer.setValue(value, row);
    }
    
}
