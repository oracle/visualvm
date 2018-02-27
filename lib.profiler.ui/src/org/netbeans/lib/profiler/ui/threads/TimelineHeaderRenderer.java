/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.netbeans.lib.profiler.ui.threads;

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
import org.netbeans.lib.profiler.charts.axis.TimeAxisUtils;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.swing.renderer.BaseRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.LabelRenderer;

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
