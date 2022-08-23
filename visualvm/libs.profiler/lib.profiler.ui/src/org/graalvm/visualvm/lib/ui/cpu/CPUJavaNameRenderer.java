/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.ui.cpu;

import javax.swing.Icon;
import javax.swing.UIManager;
import org.graalvm.visualvm.lib.jfluid.results.cpu.PrestimeCPUCCTNode;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.ProfilerIcons;
import org.graalvm.visualvm.lib.ui.swing.renderer.JavaNameRenderer;

/**
 *
 * @author Jiri Sedlacek
 */
public class CPUJavaNameRenderer extends JavaNameRenderer {

    private static final Icon THREAD_ICON = Icons.getIcon(ProfilerIcons.THREAD);
    private static final Icon THREAD_ICON_DISABLED = UIManager.getLookAndFeel().getDisabledIcon(null, THREAD_ICON);
    private static final Icon LEAF_ICON = Icons.getIcon(ProfilerIcons.NODE_LEAF);
    private static final Icon LEAF_ICON_DISABLED = UIManager.getLookAndFeel().getDisabledIcon(null, LEAF_ICON);

    private final Icon icon;
    private final Icon iconDisabled;

    public CPUJavaNameRenderer() {
        this(ProfilerIcons.NODE_FORWARD);
    }

    public CPUJavaNameRenderer(String iconKey) {
        this.icon = Icons.getIcon(iconKey);
        this.iconDisabled = UIManager.getLookAndFeel().getDisabledIcon(null, icon);
    }

    public void setValue(Object value, int row) {
        if (value instanceof PrestimeCPUCCTNode) {
            PrestimeCPUCCTNode node = (PrestimeCPUCCTNode)value;

            if (node.isSelfTimeNode()) {
                setNormalValue(node.getNodeName());
                setBoldValue(""); // NOI18N
                setGrayValue(""); // NOI18N
            } else if (node.isThreadNode()) {
                setNormalValueEx(""); // NOI18N
                setBoldValue(node.getNodeName());
                setGrayValue(""); // NOI18N
            } else if (node.isFiltered()) {
                setNormalValue(""); // NOI18N
                setBoldValue("");
                setGrayValue(node.getNodeName()); // NOI18N
            } else {
                super.setValue(node.getNodeName(), row);
            }
            
            if (node.isThreadNode()) {
                setIcon(node.isFiltered() ? THREAD_ICON_DISABLED : THREAD_ICON);
            } else if (node.isLeaf()) {
                setIcon(node.isFiltered() ? LEAF_ICON_DISABLED : LEAF_ICON);
            } else {
                setIcon(node.isFiltered() ? iconDisabled : icon);
            }
        } else {
            super.setValue(value, row);
        }
    }
    
    
    // TODO: optimize to not slow down sort/search/filter by resolving color!
    private void setNormalValueEx(String value) {
        super.setNormalValue(value);
        setCustomForeground(null);
    }
    
}
