/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2007-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.netbeans.modules.profiler.snaptracer.impl.details;

import org.netbeans.modules.profiler.snaptracer.impl.swing.HeaderLabel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import org.netbeans.lib.profiler.ui.UIUtils;

/**
 *
 * @author Jiri Sedlacek
 */
final class DetailsTable extends JTable {

    private static final int DEFAULT_ROW_HEIGHT = defaultRowHeight();
    static final Color DEFAULT_GRID_COLOR = new Color(240, 240, 240);

    private TableCellRenderer markRenderer;
    private TimestampRenderer timestampRenderer;


    DetailsTable() {
        setOpaque(true);
        setBackground(UIUtils.getProfilerResultsBackground());
        setRowHeight(DEFAULT_ROW_HEIGHT);
        setRowMargin(0);
        setAutoCreateRowSorter(true);
        setShowHorizontalLines(false);
        setShowVerticalLines(true);
        setGridColor(DEFAULT_GRID_COLOR);
        getTableHeader().setPreferredSize(new Dimension(1, HeaderLabel.DEFAULT_HEIGHT));
        getColumnModel().setColumnMargin(1);
        initRenderers();
    }
    

    public void addColumn(TableColumn aColumn) {
        super.addColumn(aColumn);
        if (aColumn.getModelIndex() == 0 || aColumn.getModelIndex() == 1)
            updateColumns(true);
    }

    public void tableChanged(TableModelEvent e) {
        super.tableChanged(e);
        if (e.getType() == TableModelEvent.INSERT) updateColumns(false);
    }

    protected JTableHeader createDefaultTableHeader() {
        return new JTableHeader(columnModel) {
            public String getToolTipText(MouseEvent e) {
                int index = columnModel.getColumnIndexAtX(e.getPoint().x);
                int realIndex = columnModel.getColumn(index).getModelIndex();
                return ((DetailsTableModel)dataModel).getColumnTooltip(realIndex);
            }
        };
    }


    private void initRenderers() {
        markRenderer = getDefaultRenderer(Boolean.class);
        TableCellRenderer dateRenderer = getDefaultRenderer(String.class);
        timestampRenderer = new TimestampRenderer(dateRenderer);
        TableCellRenderer numberRenderer = getDefaultRenderer(Long.class);
        setDefaultRenderer(Boolean.class, new MarkRenderer(markRenderer));
        setDefaultRenderer(DetailsPanel.class, timestampRenderer);
        setDefaultRenderer(Long.class, new ItemValueRenderer(numberRenderer));
    }

    private void updateColumns(boolean initialUpdate) {
        if (timestampRenderer == null) return;

        if (initialUpdate) {
            Component boolRenderer = markRenderer.getTableCellRendererComponent(
                                     DetailsTable.this, Boolean.FALSE, false,
                                     false, 0, 0);
            int width = boolRenderer.getPreferredSize().width;
            TableColumn column = columnModel.getColumn(0);
            TableCellRenderer headerRenderer = getTableHeader().getDefaultRenderer();
            Component renderer = headerRenderer.getTableCellRendererComponent(
                                 DetailsTable.this, column.getHeaderValue(), false,
                                 false, 0, 0);
            width = Math.max(width, renderer.getPreferredSize().width);
            width += 16;
            column.setPreferredWidth(width);
            column.setMaxWidth(width);
        }

        if (columnModel.getColumnCount() > 1) {
            if (!timestampRenderer.updateFormat(dataModel) && !initialUpdate) return;
            Component renderer = timestampRenderer.getTableCellRendererComponent(
                                 DetailsTable.this, TimestampRenderer.
                                 REFERENCE_TIMESTAMP, false, false, 0, 1);
            TableColumn column = columnModel.getColumn(1);
            int width = renderer.getPreferredSize().width + 5;
            if (initialUpdate || column.getMaxWidth() < width) {
                column.setPreferredWidth(width);
                column.setMaxWidth(width);
                if (!initialUpdate) repaint();
            }
        }
    }

    private static int defaultRowHeight() {
        return new JLabel("X").getPreferredSize().height + 4; // NOI18N
    }

}
