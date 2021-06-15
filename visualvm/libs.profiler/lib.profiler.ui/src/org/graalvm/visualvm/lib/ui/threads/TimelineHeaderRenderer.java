/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.ui.threads;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import org.graalvm.visualvm.lib.charts.axis.TimeAxisUtils;
import org.graalvm.visualvm.lib.ui.UIUtils;
import org.graalvm.visualvm.lib.ui.swing.renderer.BaseRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.LabelRenderer;

/**
 *
 * @author Jiri Sedlacek
 */
public class TimelineHeaderRenderer extends BaseRenderer implements TableCellRenderer {

    private final TableCellRenderer impl;
    private final int column;

    private final ViewManager view;


    public TimelineHeaderRenderer(TableCellRenderer impl, int column, ViewManager view) {
        this.impl = impl;
        this.column = column;
        this.view = view;
    }


    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
        boolean timelineColumn = column == table.convertColumnIndexToModel(col);
        setVisible(timelineColumn);

        Component c = impl.getTableCellRendererComponent(table, timelineColumn ?
                      " " : value, isSelected, hasFocus, row, col); // NOI18N
        if (timelineColumn) {
            if (getParent() == null && c instanceof Container) ((Container)c).add(this);

            JTableHeader header = table.getTableHeader();
            if (painter == null) initStaticUI(c, header);

            TableColumn _column = header.getColumnModel().getColumn(col);

            setSize(_column.getWidth(), header.getSize().height);
        }
        
        return c;
    }
    
    public void paint(Graphics g) {        
        long time = view.getFirstTimeMark(true);
        long step = view.getTimeMarksStep();
        String format = view.getTimeMarksFormat();
        
        int w = getWidth();
        int h = getHeight();
        int x = view.getTimePosition(time, true);
        
        g.setColor(painter.getForeground());
        int oldX = x;
        while (x < w) {
            paintTimeMark(x, TimeAxisUtils.formatTime(time, format), h, g);
            time += step;
            x = view.getTimePosition(time, true);
            
            // Workaround to prevent endless loop until fixed
            if (x <= oldX) break;
            else oldX = x;
        }
    }
    
    private void paintTimeMark(int x, String time, int h, Graphics g) {
        painter.setText(time);
        
        Dimension d = painter.getPreferredSize();
        painter.setSize(d);
        painter.move(x - d.width / 2, (h - d.height) / 2 + Y_LAF_OFFSET);
        painter.paint(g);
    }
    
    private static LabelRenderer painter;
    private static int Y_LAF_OFFSET;
    private static void initStaticUI(Component c, JTableHeader header) {
        painter = new LabelRenderer(true);
        
        Color color = c.getForeground();
        if (color == null) color = header.getForeground();
        if (color == null) color = UIManager.getColor("TableHeader.foreground"); // NOI18N
        if (color != null) painter.setForeground(color);
        Font font = c.getFont();
        if (font == null) font = header.getFont();
        if (font == null) font = UIManager.getFont("TableHeader.font"); // NOI18N
        if (font != null) painter.setFont(font);
        
        if (UIUtils.isWindowsXPLookAndFeel()) Y_LAF_OFFSET = 1;
        else if (UIUtils.isNimbusLookAndFeel()) Y_LAF_OFFSET = -1;
        else Y_LAF_OFFSET = 0;
    }
    
}
