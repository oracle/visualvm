/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2014 Oracle and/or its affiliates. All rights reserved.
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

import javax.swing.Icon;
import org.netbeans.lib.profiler.results.locks.LockCCTNode;
import org.netbeans.lib.profiler.ui.swing.renderer.NormalBoldGrayRenderer;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;

/**
 *
 * @author Jiri Sedlacek
 */
public class LockContentionRenderer extends NormalBoldGrayRenderer {
    
    private static final Icon THREAD_ICON = Icons.getIcon(ProfilerIcons.THREAD);
    private static final Icon LOCK_ICON = Icons.getIcon(ProfilerIcons.WINDOW_LOCKS);
    
    public void setValue(Object value, int row) {
        LockCCTNode node = (LockCCTNode)value;
        
        boolean lockNode = node.isThreadLockNode();
        boolean monitorNode = node.isMonitorNode();
        
        String nodeName = node.getNodeName();
        int bracketIndex = nodeName.indexOf('('); // NOI18N
        int dotIndex = nodeName.lastIndexOf('.'); // NOI18N
        
        String normalValue = getNormalValue(node, nodeName, bracketIndex, dotIndex, lockNode);
        String boldValue = getBoldValue(node, nodeName, bracketIndex, dotIndex, lockNode);
        String grayValue = getGrayValue(node, nodeName, bracketIndex, dotIndex, lockNode);
        
        setNormalValue(normalValue);
        setBoldValue(boldValue);
        setGrayValue(grayValue);
        
        Icon icon = null;
        if (lockNode) icon = THREAD_ICON;
        else if (monitorNode) icon = LOCK_ICON;
        
        setIcon(icon);
    }
    
    private String getNormalValue(LockCCTNode node, String nodeName, int bracketIndex,
                                  int dotIndex, boolean lockNode) {
        
        if (lockNode) return node.getParent().getParent() == null ? "" : nodeName; // NOI18N
        
        if (dotIndex == -1 && bracketIndex == -1) return nodeName;

        if (bracketIndex != -1) nodeName = nodeName.substring(0, bracketIndex);
        return nodeName.substring(0, dotIndex + 1);
    }
    
    private String getBoldValue(LockCCTNode node, String nodeName, int bracketIndex,
                                int dotIndex, boolean lockNode) {
        
        if (lockNode) return node.getParent().getParent() == null ? nodeName : ""; // NOI18N
        
        if (dotIndex == -1 && bracketIndex == -1) return ""; // NOI18N

        if (bracketIndex != -1) nodeName = nodeName.substring(0, bracketIndex);
        return nodeName.substring(dotIndex + 1);
    }
    
    private String getGrayValue(LockCCTNode node, String nodeName, int bracketIndex,
                                int dotIndex, boolean lockNode) {
        
        if (lockNode) return ""; // NOI18N
        
        return bracketIndex != -1 ? " " + nodeName.substring(bracketIndex) : ""; // NOI18N
    }
    
}
