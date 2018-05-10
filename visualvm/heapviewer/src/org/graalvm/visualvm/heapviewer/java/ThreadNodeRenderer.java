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

import java.awt.Font;
import javax.swing.Icon;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.ui.swing.renderer.LabelRenderer;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.ProfilerIcons;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerRenderer;
import java.awt.Color;
import java.util.Objects;
import javax.swing.JTable;

/**
 *
 * @author Jiri Sedlacek
 */
public class ThreadNodeRenderer extends LabelRenderer implements HeapViewerRenderer {
    
    private static final Icon ICON = Icons.getIcon(ProfilerIcons.THREAD);
    
    private static final Color REPLACEABLE_FOREGROUND = new JTable().getForeground();
    
    protected final Heap heap;

    private Color customForeground;
    
    
    public ThreadNodeRenderer(Heap heap) {
        this.heap = heap;
        
        setIcon(ICON);
        setFont(getFont().deriveFont(Font.BOLD));
    }
    
    
    public void setValue(Object value, int row) {
        ThreadNode node = (ThreadNode)value;
        setText(node.getName(heap));
        setCustomForeground(node.isOOMEThread() ? Color.RED : null);
    }
    
    public String getShortName() {
        String name = getText();
        int nameIdx = name.indexOf('"') + 1; // NOI18N
        if (nameIdx > 0) name = name.substring(nameIdx, name.indexOf('"', nameIdx)); // NOI18N
        return name;
    }
    
    
    public void setForeground(Color foreground) {
        if (customForeground != null && Objects.equals(foreground, REPLACEABLE_FOREGROUND)) {
            super.setForeground(customForeground);
        } else {
            super.setForeground(foreground);
        }
    }
    
    private void setCustomForeground(Color foreground) {
        customForeground = foreground;
    }
    
}
