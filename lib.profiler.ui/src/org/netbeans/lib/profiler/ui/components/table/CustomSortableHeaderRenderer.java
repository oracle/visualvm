/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
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

package org.netbeans.lib.profiler.ui.components.table;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.TableCellRenderer;


/**
 * This class is used for rendering the JTable header. It also holds information about current sorting column and sorting order.
 * The column header is rendered by the JButton using the appropriate icon.
 * @author Jiri Sedlacek
 */
public class CustomSortableHeaderRenderer implements TableCellRenderer {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private ImageIcon ascIcon;
    private ImageIcon descIcon;
    private boolean sortOrder = SortableTableModel.SORT_ORDER_DESC;

    /**
     * The column which is currently being pressed.
     * The button has to be pressed programatically because the mouse events are not delivered to the JButton from the table header.
     */
    private int pressedColumn = -1;

    /**
     * The column which currently defines the sorting order. Only this column header contains the appropriate icon.
     */
    private int sortingColumn = -1;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Creates a new instance of CustomSortableHeaderRenderer
     * @param asc The icon representing ascending sort order.
     * @param desc The icon representing descending sort order.
     */
    public CustomSortableHeaderRenderer(ImageIcon asc, ImageIcon desc) {
        ascIcon = asc;
        descIcon = desc;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setPressedColumn(int column) {
        pressedColumn = column;
    }

    public int getPressedColumn() {
        return pressedColumn;
    }

    public void setSortingColumn(int column) {
        sortingColumn = column;
    }

    public int getSortingColumn() {
        return sortingColumn;
    }

    public void setSortingOrder(boolean order) {
        sortOrder = order;
    }

    public boolean getSortingOrder() {
        return sortOrder;
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row,
                                                   int column) {
        TableCellRenderer tableCellRenderer = table.getTableHeader().getDefaultRenderer();
        Component c = tableCellRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        if (c instanceof JLabel) {
            JLabel l = (JLabel) c;

            if (column == sortingColumn) { // only for sorting column the icon is displayed
                l.setIcon((sortOrder == SortableTableModel.SORT_ORDER_ASC) ? ascIcon : descIcon);
                l.setFont(l.getFont().deriveFont(Font.BOLD));
            } else {
                l.setIcon(null);
            }

            l.setHorizontalTextPosition(JLabel.LEFT);
        }

        return c;
    }

    public void reverseSortingOrder() {
        sortOrder = !sortOrder;
    }
}
