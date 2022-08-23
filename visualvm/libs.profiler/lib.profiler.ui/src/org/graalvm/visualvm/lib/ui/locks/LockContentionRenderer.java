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

package org.graalvm.visualvm.lib.ui.locks;

import javax.swing.Icon;
import org.graalvm.visualvm.lib.jfluid.results.locks.LockCCTNode;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.ProfilerIcons;
import org.graalvm.visualvm.lib.ui.results.PackageColorer;
import org.graalvm.visualvm.lib.ui.swing.renderer.NormalBoldGrayRenderer;

/**
 *
 * @author Jiri Sedlacek
 */
public class LockContentionRenderer extends NormalBoldGrayRenderer {

    private static final Icon THREAD_ICON = Icons.getIcon(ProfilerIcons.THREAD);
    private static final Icon LOCK_ICON = Icons.getIcon(ProfilerIcons.WINDOW_LOCKS);

    public void setValue(Object value, int row) {
        if (value == null) {
            setNormalValue(""); // NOI18N
            setBoldValue(""); // NOI18N
            setGrayValue(""); // NOI18N
            setIcon(null);
        } else {
            LockCCTNode node = (LockCCTNode)value;

            boolean threadNode = node.isThreadLockNode();
            boolean monitorNode = node.isMonitorNode();

            String nodeName = node.getNodeName();
            int bracketIndex = nodeName.indexOf('('); // NOI18N
            int dotIndex = nodeName.lastIndexOf('.'); // NOI18N

            String normalValue = getNormalValue(node, nodeName, bracketIndex, dotIndex, threadNode);
            String boldValue = getBoldValue(node, nodeName, bracketIndex, dotIndex, threadNode);
            String grayValue = getGrayValue(node, nodeName, bracketIndex, dotIndex, threadNode);

            setNormalValue(normalValue);
            setBoldValue(boldValue);
            setGrayValue(grayValue);

            Icon icon = null;
            if (threadNode) icon = THREAD_ICON;
            else if (monitorNode) icon = LOCK_ICON;

            setIcon(icon);
            
            // TODO: optimize to not slow down sort/search/filter by resolving color!
            setCustomForeground(monitorNode ? PackageColorer.getForeground(normalValue) : null);
        }
    }
    
    private String getNormalValue(LockCCTNode node, String nodeName, int bracketIndex,
                                  int dotIndex, boolean threadNode) {
        
        if (threadNode) return node.getParent().getParent() == null ? "" : nodeName; // NOI18N
        
        if (dotIndex == -1 && bracketIndex == -1) return nodeName;

        if (bracketIndex != -1) nodeName = nodeName.substring(0, bracketIndex);
        return nodeName.substring(0, dotIndex + 1);
    }
    
    private String getBoldValue(LockCCTNode node, String nodeName, int bracketIndex,
                                int dotIndex, boolean threadNode) {
        
        if (threadNode) return node.getParent().getParent() == null ? nodeName : ""; // NOI18N
        
        if (dotIndex == -1 && bracketIndex == -1) return ""; // NOI18N

        if (bracketIndex != -1) nodeName = nodeName.substring(0, bracketIndex);
        return nodeName.substring(dotIndex + 1);
    }
    
    private String getGrayValue(LockCCTNode node, String nodeName, int bracketIndex,
                                int dotIndex, boolean threadNode) {
        
        if (threadNode) return ""; // NOI18N
        
        return bracketIndex != -1 ? " " + nodeName.substring(bracketIndex) : ""; // NOI18N
    }
    
}
