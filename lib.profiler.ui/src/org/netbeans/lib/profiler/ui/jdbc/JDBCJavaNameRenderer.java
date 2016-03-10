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

package org.netbeans.lib.profiler.ui.jdbc;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import org.netbeans.lib.profiler.results.memory.PresoObjAllocCCTNode;
import org.netbeans.lib.profiler.ui.swing.renderer.JavaNameRenderer;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;

/**
 *
 * @author Jiri Sedlacek
 */
public class JDBCJavaNameRenderer extends JavaNameRenderer {
    
    private static final Icon SQL_ICON = Icons.getIcon(ProfilerIcons.SQL_QUERY);
    private static final Icon SQL_ICON_DISABLED = UIManager.getLookAndFeel().getDisabledIcon(null, SQL_ICON);
    private static final Icon LEAF_ICON = Icons.getIcon(ProfilerIcons.NODE_LEAF);
    private static final Icon LEAF_ICON_DISABLED = UIManager.getLookAndFeel().getDisabledIcon(null, LEAF_ICON);
    
    private final Icon icon;
    private final Icon iconDisabled;
    
    private final JLabel sqlRenderer = new DefaultTableCellRenderer();
    
    private String currentValue;
    
    private boolean isSQL;
    
    public JDBCJavaNameRenderer() {
        this(ProfilerIcons.NODE_REVERSE);
    }
    
    public JDBCJavaNameRenderer(String iconKey) {
        this.icon = Icons.getIcon(iconKey);
        this.iconDisabled = UIManager.getLookAndFeel().getDisabledIcon(null, icon);
    }
    
    public void setValue(Object value, int row) {
        if (value instanceof PresoObjAllocCCTNode) {
            PresoObjAllocCCTNode node = (PresoObjAllocCCTNode)value;
            
            boolean filtered = node.isFiltered();
            currentValue = node.getNodeName();
            
            isSQL = JDBCTreeTableView.isSQL(node);
            if (isSQL) {
                sqlRenderer.setText(formatSQLCommand(currentValue));
                sqlRenderer.setIcon(filtered ? SQL_ICON_DISABLED : SQL_ICON);
            } else {
                if (filtered) {
                    setNormalValue(""); // NOI18N
                    setBoldValue(""); // NOI18N
                    setGrayValue(currentValue);
                } else {
                    super.setValue(currentValue, row);
                }

                if (node.isLeaf()) {
                    setIcon(filtered ? LEAF_ICON_DISABLED : LEAF_ICON);
                } else {
                    setIcon(filtered ? iconDisabled : icon);
                }
            }
        } else {
            super.setValue(value, row);
        }
    }
    
    public JComponent getComponent() {
        return isSQL ? sqlRenderer : super.getComponent();
    }
    
    public String toString() {
        return currentValue;
    }
    
    
    private static String formatSQLCommand(String command) {
        StringBuilder s = new StringBuilder();
        s.append("<html>"); // NOI18N
        
        command = command.replace("CREATE TABLE ", "<b>CREATE TABLE </b>"); // NOI18N
        command = command.replace("ALTER TABLE ", "<b>ALTER TABLE </b>"); // NOI18N
        command = command.replace("TRUNCATE TABLE ", "<b>TRUNCATE TABLE </b>"); // NOI18N
        command = command.replace("INSERT INTO ", "<b>INSERT INTO </b>"); // NOI18N
//        command = command.replace("DELETE FROM ", "<b>DELETE FROM </b>"); // NOI18N
        command = command.replace("SELECT ", "<b>SELECT </b>"); // NOI18N
        command = command.replace("DELETE ", "<b>DELETE </b>"); // NOI18N 
        command = command.replace("FROM ", "<b>FROM </b>"); // NOI18N
        command = command.replace("WHERE ", "<b>WHERE </b>"); // NOI18N
        command = command.replace("UPDATE ", "<b>UPDATE </b>"); // NOI18N
        command = command.replace("VALUES ", "<b>VALUES </b>"); // NOI18N
        command = command.replace("DISTINCT ", "<b>DISTINCT </b>"); // NOI18N
        command = command.replace("ORDER BY ", "<b>ORDER BY </b>"); // NOI18N
        command = command.replace("GROUP BY ", "<b>GROUP BY </b>"); // NOI18N
        s.append(command);
        
        s.append("</html>"); // NOI18N
        return s.toString();
    }
    
}
