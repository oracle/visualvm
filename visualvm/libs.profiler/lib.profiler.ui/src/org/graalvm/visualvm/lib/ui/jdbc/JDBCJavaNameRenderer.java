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

package org.graalvm.visualvm.lib.ui.jdbc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import org.graalvm.visualvm.lib.jfluid.results.memory.PresoObjAllocCCTNode;
import org.graalvm.visualvm.lib.ui.swing.renderer.JavaNameRenderer;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.ProfilerIcons;

/**
 *
 * @author Jiri Sedlacek
 */
final class JDBCJavaNameRenderer extends JavaNameRenderer {
    
    private static final Icon SQL_ICON = Icons.getIcon(ProfilerIcons.SQL_QUERY);
    private static final Icon SQL_ICON_DISABLED = UIManager.getLookAndFeel().getDisabledIcon(null, SQL_ICON);
    private static final Icon LEAF_ICON = Icons.getIcon(ProfilerIcons.NODE_LEAF);
    private static final Icon LEAF_ICON_DISABLED = UIManager.getLookAndFeel().getDisabledIcon(null, LEAF_ICON);
    
    private final Icon icon;
    private final Icon iconDisabled;
    
    private String currentValue;
    private JLabel currentSQLRenderer;
    
    JDBCJavaNameRenderer() {
        this(ProfilerIcons.NODE_REVERSE);
    }
    
    JDBCJavaNameRenderer(String iconKey) {
        this.icon = Icons.getIcon(iconKey);
        this.iconDisabled = UIManager.getLookAndFeel().getDisabledIcon(null, icon);
    }
    
    public void setValue(Object value, int row) {
        if (value instanceof PresoObjAllocCCTNode) {
            PresoObjAllocCCTNode node = (PresoObjAllocCCTNode)value;
            
            boolean filtered = node.isFiltered();
            currentValue = node.getNodeName();
            
            if (JDBCTreeTableView.isSQL(node)) {
                JDBCTreeTableView.SQLQueryNode sqlNode = (JDBCTreeTableView.SQLQueryNode)node;
                String htmlName = sqlNode.htmlName;
                if (htmlName == null) {
                    htmlName = SQLFormatter.format(currentValue);
                    sqlNode.htmlName = htmlName;
                }
                currentSQLRenderer = sqlRenderer(htmlName, filtered ? SQL_ICON_DISABLED : SQL_ICON);
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
                currentSQLRenderer = null;
            }
        } else {
            super.setValue(value, row);
        }
    }
    
    public JComponent getComponent() {
        return currentSQLRenderer != null ? currentSQLRenderer : super.getComponent();
    }
    
    public String toString() {
        return currentValue;
    }
    
    
    private static int CACHE_SIZE = 100;
    private List<String> sqlRenderersKeys;
    private Map<String, JLabel> sqlRenderersCache;
    
    private JLabel sqlRenderer(String text, Icon icon) {
        if (sqlRenderersCache == null) {
            sqlRenderersKeys = new ArrayList(CACHE_SIZE);
            sqlRenderersCache = new HashMap(CACHE_SIZE);
        }
        
        JLabel sqlRenderer = sqlRenderersCache.get(text);
        
        if (sqlRenderer == null) {
            if (sqlRenderersKeys.size() < CACHE_SIZE) {
                sqlRenderer = new DefaultTableCellRenderer();
            } else {
                String key = sqlRenderersKeys.remove(0);
                sqlRenderer = sqlRenderersCache.remove(key);
            }
            
            sqlRenderersKeys.add(text);
            sqlRenderersCache.put(text, sqlRenderer);
            
            sqlRenderer.setText(text);
        }
        
        sqlRenderer.setIcon(icon);
        
        return sqlRenderer;
    }
    
}
