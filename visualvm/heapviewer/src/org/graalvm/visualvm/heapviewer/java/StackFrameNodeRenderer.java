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

package org.graalvm.visualvm.heapviewer.java;

import org.graalvm.visualvm.lib.ui.swing.renderer.JavaNameRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.LabelRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.MultiRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.ProfilerRenderer;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerRenderer;

/**
 *
 * @author Jiri Sedlacek
 */
public class StackFrameNodeRenderer extends MultiRenderer implements HeapViewerRenderer {
    
    private final LabelRenderer atRenderer;
    private final JavaNameRenderer methodRenderer;
    private final ProfilerRenderer[] renderers;

    
    public StackFrameNodeRenderer() {
        atRenderer = new LabelRenderer() {
            public String toString() {
                return getText() + " "; // NOI18N
            }
        };
        atRenderer.setText("at"); // NOI18N
        atRenderer.setMargin(3, 3, 3, 0);
        methodRenderer = new JavaNameRenderer();
        renderers = new ProfilerRenderer[] { atRenderer, methodRenderer };
    }

    
    protected ProfilerRenderer[] valueRenderers() {
        return renderers;
    }

    
    public void setValue(Object value, int row) {
        methodRenderer.setValue(value, row);
    }
    
    public String getShortName() {
        String name = methodRenderer.toString();
        int nameIdx = name.indexOf('('); // NOI18N
        if (nameIdx == -1) return atRenderer + " " + name; // NOI18N
        
        String method = name.substring(0, nameIdx);
        int dotIdx = method.lastIndexOf('.'); // NOI18N
        if (dotIdx == -1) return atRenderer + " " + name; // NOI18N
        
        String cls = method.substring(0, dotIdx);
        dotIdx = cls.lastIndexOf('.'); // NOI18N
        return atRenderer + " " + method.substring(dotIdx + 1) + name.substring(nameIdx); // NOI18N
    }
    
}
