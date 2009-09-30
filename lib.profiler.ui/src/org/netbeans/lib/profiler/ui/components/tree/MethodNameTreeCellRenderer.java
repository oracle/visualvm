/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.netbeans.lib.profiler.ui.components.tree;

import java.awt.Color;
import org.netbeans.lib.profiler.results.cpu.PrestimeCPUCCTNode;
import java.awt.Component;
import javax.swing.*;
import org.netbeans.lib.profiler.ui.UIUtils;


/**
 * Formats the node as follows:
 *   - if node does not contain either '.' or '(', format the node in plain font (typically if not a class or method name)
 *   - anything after '(' is formatted using gray font                           (typically method arguments)
 *   - anything between last '.' and before '(' is formatted using bold font     (typically method name)
 */
public class MethodNameTreeCellRenderer extends EnhancedTreeCellRenderer {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private ImageIcon allThreadsIcon = new ImageIcon(MethodNameTreeCellRenderer.class
                                                         .getResource("/org/netbeans/lib/profiler/ui/resources/allThreads.png")); //NOI18N
    private ImageIcon threadIcon = new ImageIcon(MethodNameTreeCellRenderer.class.getResource("/org/netbeans/lib/profiler/ui/resources/thread.png")); //NOI18N

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
            }
        }

        // not a thread node or not instance of PrestimeCPUCCTNode
        return super.getClosedIcon(value);
    }

    protected String getLabel1Text(Object node, String value) {
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
            }
        }

        // not a thread node or not instance of PrestimeCPUCCTNode
        return super.getOpenIcon(value);
    }
}
