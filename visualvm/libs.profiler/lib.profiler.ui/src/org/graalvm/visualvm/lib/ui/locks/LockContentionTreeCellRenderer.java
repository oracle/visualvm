/*
 * Copyright (c) 1997, 2022, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.ui.locks;

import java.awt.Component;
import javax.swing.Icon;
import javax.swing.JTree;
import org.graalvm.visualvm.lib.jfluid.results.locks.LockCCTNode;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.ProfilerIcons;
import org.graalvm.visualvm.lib.ui.components.tree.EnhancedTreeCellRenderer;

/**
 *
 * @author Jiri Sedlacek
 */
public class LockContentionTreeCellRenderer extends EnhancedTreeCellRenderer {

    protected String getLabel1Text(Object node, String value) {
        LockCCTNode n = (LockCCTNode)node;
        String name = n.getNodeName();
        if (n.isThreadLockNode()) return n.getParent().getParent() == null ? "" : name; // NOI18N

        int bracketIndex = name.indexOf('('); // NOI18N
        int dotIndex = name.lastIndexOf('.'); // NOI18N

        if ((dotIndex == -1) && (bracketIndex == -1)) return name;

        if (bracketIndex != -1) {
            name = name.substring(0, bracketIndex);
            dotIndex = name.lastIndexOf('.'); // NOI18N
        }

        return name.substring(0, dotIndex + 1);
    }

    protected String getLabel2Text(Object node, String value) {
        LockCCTNode n = (LockCCTNode)node;
        String name = n.getNodeName();
        if (n.isThreadLockNode()) return n.getParent().getParent() == null ? name : ""; // NOI18N

        int bracketIndex = name.indexOf('('); // NOI18N
        int dotIndex = name.lastIndexOf('.'); // NOI18N

        if ((dotIndex == -1) && (bracketIndex == -1)) return ""; // NOI18N

        if (bracketIndex != -1) {
            name = name.substring(0, bracketIndex);
            dotIndex = name.lastIndexOf('.'); // NOI18N
        }

        return name.substring(dotIndex + 1);
    }

    protected String getLabel3Text(Object node, String value) {
        LockCCTNode n = (LockCCTNode)node;
        if (n.isThreadLockNode()) return ""; // NOI18N
        
        String name = n.getNodeName();
        int bracketIndex = name.indexOf('('); // NOI18N
        return bracketIndex != -1 ? " " + name.substring(bracketIndex) : ""; // NOI18N
    }
    
    private Icon getIcon(Object node) {
        LockCCTNode n = (LockCCTNode)node;
        if (n.isThreadLockNode()) return Icons.getIcon(ProfilerIcons.THREAD);
        else if (n.isMonitorNode()) return Icons.getIcon(ProfilerIcons.WINDOW_LOCKS);
        return null;
    }
    
    protected Icon getLeafIcon(Object value) {
        return getIcon(value);
    }

    protected Icon getOpenIcon(Object value) {
        return getIcon(value);
    }
    
    protected Icon getClosedIcon(Object value) {
        return getIcon(value);
    }
    
    
    public Component getTreeCellRendererComponentPersistent(JTree tree, Object value, boolean sel, boolean expanded,
                                                            boolean leaf, int row, boolean hasFocus) {
        LockContentionTreeCellRenderer renderer = new LockContentionTreeCellRenderer();
//        renderer.setLeafIcon(leafIcon);
//        renderer.setClosedIcon(closedIcon);
//        renderer.setOpenIcon(openIcon);

        return renderer.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
    }
    
}
