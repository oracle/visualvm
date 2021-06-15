/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.profiler.snaptracer.impl.details;

import org.graalvm.visualvm.lib.profiler.snaptracer.impl.swing.HeaderLabel;
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
import org.graalvm.visualvm.lib.ui.UIUtils;

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
