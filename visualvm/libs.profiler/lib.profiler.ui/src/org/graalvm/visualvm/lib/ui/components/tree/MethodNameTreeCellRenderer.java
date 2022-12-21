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

package org.graalvm.visualvm.lib.ui.components.tree;

import java.awt.Color;
import java.awt.Component;
import javax.swing.*;
import org.graalvm.visualvm.lib.jfluid.results.cpu.PrestimeCPUCCTNode;
import org.graalvm.visualvm.lib.jfluid.results.memory.PresoObjAllocCCTNode;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.ProfilerIcons;
import org.graalvm.visualvm.lib.ui.UIUtils;


/**
 * Formats the node as follows:
 *   - if node does not contain either '.' or '(', format the node in plain font (typically if not a class or method name)
 *   - anything after '(' is formatted using gray font                           (typically method arguments)
 *   - anything between last '.' and before '(' is formatted using bold font     (typically method name)
 */
public class MethodNameTreeCellRenderer extends EnhancedTreeCellRenderer {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private Icon allThreadsIcon = Icons.getIcon(ProfilerIcons.ALL_THREADS);
    private Icon threadIcon = Icons.getIcon(ProfilerIcons.THREAD);

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public Component getTreeCellRendererComponentPersistent(JTree tree, Object value, boolean sel, boolean expanded,
                                                            boolean leaf, int row, boolean hasFocus) {
        MethodNameTreeCellRenderer renderer = new MethodNameTreeCellRenderer();
        renderer.setLeafIcon(getLeafIcon(value));
        renderer.setClosedIcon(getClosedIcon(value));
        renderer.setOpenIcon(getOpenIcon(value));
        Color backgroundColor = UIUtils.getProfilerResultsBackground();

        if ((row & 0x1) == 0) { //even row
            renderer.setBackgroundNonSelectionColor(UIUtils.getDarker(backgroundColor));
        } else {
            renderer.setBackgroundNonSelectionColor(backgroundColor);
        }

        return renderer.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
    }

    protected Icon getClosedIcon(Object value) {
        if (value instanceof PrestimeCPUCCTNode) {
            PrestimeCPUCCTNode cct = (PrestimeCPUCCTNode) value;

            if (cct.isThreadNode()) {
                if (cct.getThreadId() == -1) {
                    return allThreadsIcon;
                } else {
                    return threadIcon;
                }
            } else if (cct.isFiltered()) {
                return UIManager.getLookAndFeel().getDisabledIcon(this, super.getClosedIcon(value));
            }
        } else if (value instanceof PresoObjAllocCCTNode) {
            if (((PresoObjAllocCCTNode)value).isFiltered()) {
                return UIManager.getLookAndFeel().getDisabledIcon(this, super.getClosedIcon(value));
            }
        }

        // not a thread node or not instance of PrestimeCPUCCTNode
        return super.getClosedIcon(value);
    }

    protected String getLabel1Text(Object node, String value) {
        if (node instanceof PrestimeCPUCCTNode) {
            if (((PrestimeCPUCCTNode)node).isThreadNode() ||
                ((PrestimeCPUCCTNode)node).isFiltered())
                return ""; //NOI18N
        } else if (node instanceof PresoObjAllocCCTNode) {
            if (((PresoObjAllocCCTNode)node).isFiltered())
                return ""; //NOI18N
        }
        
        int bracketIndex = value.indexOf('('); //NOI18N
        int dotIndex = value.lastIndexOf('.'); //NOI18N

        if ((dotIndex == -1) && (bracketIndex == -1)) {
            return value; // not a method -> we will format it in plain text
        }

        if (bracketIndex != -1) {
            value = value.substring(0, bracketIndex);
            dotIndex = value.lastIndexOf('.'); //NOI18N
        }

        return value.substring(0, dotIndex + 1);
    }

    protected String getLabel2Text(Object node, String value) {
        if (node instanceof PrestimeCPUCCTNode) {
            if (((PrestimeCPUCCTNode)node).isThreadNode())
                return value;
            else if (((PrestimeCPUCCTNode)node).isFiltered())
                return ""; // NOI18N
        } else if (node instanceof PresoObjAllocCCTNode) {
            if (((PresoObjAllocCCTNode)node).isFiltered())
                return ""; //NOI18N
        }
        
        int bracketIndex = value.indexOf('('); //NOI18N
        int dotIndex = value.lastIndexOf('.'); //NOI18N

        if ((dotIndex == -1) && (bracketIndex == -1)) {
            return ""; //NOI18N // not a method -> we will format it in plain text
        }

        if (bracketIndex != -1) {
            value = value.substring(0, bracketIndex);
            dotIndex = value.lastIndexOf('.'); //NOI18N
        }

        return value.substring(dotIndex + 1);
    }

    protected String getLabel3Text(Object node, String value) {
        if (node instanceof PrestimeCPUCCTNode) {
            if (((PrestimeCPUCCTNode)node).isThreadNode())
                return ""; //NOI18N
            else if (((PrestimeCPUCCTNode)node).isFiltered())
                return value;
        } else if (node instanceof PresoObjAllocCCTNode) {
            if (((PresoObjAllocCCTNode)node).isFiltered())
                return value;
        }
        
        int bracketIndex = value.indexOf('('); //NOI18N

        if (bracketIndex != -1) {
            return " " + value.substring(bracketIndex); //NOI18N
        } else {
            return ""; //NOI18N
        }
    }

    protected Icon getLeafIcon(Object value) {
        if (value instanceof PrestimeCPUCCTNode) {
            PrestimeCPUCCTNode cct = (PrestimeCPUCCTNode) value;

            if (cct.isThreadNode()) {
                if (cct.getThreadId() == -1) {
                    return allThreadsIcon;
                } else {
                    return threadIcon;
                }
            } else if (cct.isFiltered()) {
                return UIManager.getLookAndFeel().getDisabledIcon(this, super.getLeafIcon(value));
            }
        } else if (value instanceof PresoObjAllocCCTNode) {
            if (((PresoObjAllocCCTNode)value).isFiltered()) {
                return UIManager.getLookAndFeel().getDisabledIcon(this, super.getLeafIcon(value));
            }
        }

        // not a thread node or not instance of PrestimeCPUCCTNode
        return super.getLeafIcon(value);
    }

    protected Icon getOpenIcon(Object value) {
        if (value instanceof PrestimeCPUCCTNode) {
            PrestimeCPUCCTNode cct = (PrestimeCPUCCTNode) value;

            if (cct.isThreadNode()) {
                if (cct.getThreadId() == -1) {
                    return allThreadsIcon;
                } else {
                    return threadIcon;
                }
            } else if (cct.isFiltered()) {
                return UIManager.getLookAndFeel().getDisabledIcon(this, super.getOpenIcon(value));
            }
        } else if (value instanceof PresoObjAllocCCTNode) {
            if (((PresoObjAllocCCTNode)value).isFiltered()) {
                return UIManager.getLookAndFeel().getDisabledIcon(this, super.getOpenIcon(value));
            }
        }

        // not a thread node or not instance of PrestimeCPUCCTNode
        return super.getOpenIcon(value);
    }
}
