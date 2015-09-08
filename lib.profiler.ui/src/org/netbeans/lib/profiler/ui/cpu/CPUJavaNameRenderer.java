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

package org.netbeans.lib.profiler.ui.cpu;

import javax.swing.Icon;
import javax.swing.UIManager;
import org.netbeans.lib.profiler.results.cpu.PrestimeCPUCCTNode;
import org.netbeans.lib.profiler.ui.swing.renderer.JavaNameRenderer;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;

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
                setNormalValue(""); // NOI18N
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
    
}
