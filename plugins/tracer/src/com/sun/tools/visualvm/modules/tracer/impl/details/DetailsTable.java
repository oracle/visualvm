/*
 * Copyright 2007-2010 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tools.visualvm.modules.tracer.impl.details;

import com.sun.tools.visualvm.modules.tracer.impl.swing.HeaderLabel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

/**
 *
 * @author Jiri Sedlacek
 */
final class DetailsTable extends JTable {

    private static final int DEFAULT_ROW_HEIGHT = defaultRowHeight();
    static final Color DEFAULT_GRID_COLOR = new Color(240, 240, 240);

    private TimestampRenderer timestampRenderer;


    DetailsTable() {
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
        if (aColumn.getModelIndex() == 0) updateTimestamps(true);
    }

    public void tableChanged(TableModelEvent e) {
        super.tableChanged(e);
        if (e.getType() == TableModelEvent.INSERT) updateTimestamps(false);
    }


    private void initRenderers() {
        TableCellRenderer dateRenderer = getDefaultRenderer(String.class);
        timestampRenderer = new TimestampRenderer(dateRenderer);
        TableCellRenderer numberRenderer = getDefaultRenderer(Long.class);
        setDefaultRenderer(DetailsPanel.class, timestampRenderer);
        setDefaultRenderer(Long.class, new ItemValueRenderer(numberRenderer));
    }

    private void updateTimestamps(boolean initialUpdate) {
        if (timestampRenderer == null) return;
        if (!timestampRenderer.updateFormat(dataModel) && !initialUpdate) return;
        Component renderer = timestampRenderer.getTableCellRendererComponent(
                             DetailsTable.this, TimestampRenderer.REFERENCE_TIMESTAMP,
                             false, false, 0, 0);
        TableColumn column = columnModel.getColumn(0);
        int width = renderer.getPreferredSize().width + 5;
        if (initialUpdate || column.getMaxWidth() < width) {
            column.setPreferredWidth(width);
            column.setMaxWidth(width);
            if (!initialUpdate) repaint();
        }
    }

    private static int defaultRowHeight() {
        return new JLabel("X").getPreferredSize().height + 2; // NOI18N
    }

}
