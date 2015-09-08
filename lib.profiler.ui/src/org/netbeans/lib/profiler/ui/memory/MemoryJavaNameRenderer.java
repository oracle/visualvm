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
package org.netbeans.lib.profiler.ui.memory;

import javax.swing.Icon;
import javax.swing.UIManager;
import org.netbeans.lib.profiler.results.memory.PresoObjAllocCCTNode;
import org.netbeans.lib.profiler.ui.swing.renderer.JavaNameRenderer;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.LanguageIcons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;

/**
 *
 * @author Jiri Sedlacek
 */
public class MemoryJavaNameRenderer extends JavaNameRenderer {
    
    private static final Icon CLASS_ICON = Icons.getIcon(LanguageIcons.CLASS);
    private static final Icon REVERSE_ICON = Icons.getIcon(ProfilerIcons.NODE_REVERSE);
    private static final Icon REVERSE_ICON_DISABLED = UIManager.getLookAndFeel().getDisabledIcon(null, REVERSE_ICON);
    
    public void setValue(Object value, int row) {
        if (value instanceof PresoObjAllocCCTNode) {
            PresoObjAllocCCTNode node = (PresoObjAllocCCTNode)value;
            
            if (node.isFiltered()) {
                setNormalValue(""); // NOI18N
                setBoldValue("");
                setGrayValue(node.getNodeName()); // NOI18N
            } else {
                super.setValue(value, row);
            }
            
            if (node.isFiltered()) setIcon(REVERSE_ICON_DISABLED);
            else if (node.getMethodClassNameAndSig()[2] == null) setIcon(CLASS_ICON); // class name
            else setIcon(REVERSE_ICON); // method name
        } else {
            super.setValue(value, row);
        }
        
//        // TODO: <clinit> methods should be displayed with "()" similar to <init>
//        // PlainFormattableMethodName.getFullFormattedMethod()
//        
//        if (getGrayValue().isEmpty()) System.err.println(">> value: " + ((PresoObjAllocCCTNode)value).getMethodClassNameAndSig()[2]);
//        // TODO: also "Objects allocated by reflection" should be excluded to display icon
//        if (getGrayValue().isEmpty()) setIcon(null); // class name
//        else setIcon(Icons.getIcon(ProfilerIcons.NODE_REVERSE)); // method name
    }
    
}
