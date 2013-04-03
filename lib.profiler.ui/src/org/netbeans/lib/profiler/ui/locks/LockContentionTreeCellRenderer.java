/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
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
package org.netbeans.lib.profiler.ui.locks;

import java.awt.Component;
import javax.swing.Icon;
import javax.swing.JTree;
import org.netbeans.lib.profiler.results.locks.LockCCTNode;
import org.netbeans.lib.profiler.ui.components.tree.EnhancedTreeCellRenderer;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;

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
