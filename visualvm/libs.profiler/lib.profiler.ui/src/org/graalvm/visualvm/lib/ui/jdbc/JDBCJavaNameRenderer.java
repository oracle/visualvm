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
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.ProfilerIcons;
import org.graalvm.visualvm.lib.ui.swing.renderer.JavaNameRenderer;

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
            sqlRenderersKeys = new ArrayList<>(CACHE_SIZE);
            sqlRenderersCache = new HashMap<>(CACHE_SIZE);
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
