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

import org.netbeans.lib.profiler.ui.components.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;


/**
 * This class provides a superclass, from which Table Models can be derived, that will support
 * sorting by a column on which the user clicks. A subclass should call setTable(table),
 * and should provide an implementation of the sortByColumn(int column) method.
 *
 * @author Misha Dmitriev
 * @author Jiri Sedlacek
 */
public abstract class SortableTableModel extends AbstractTableModel {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    /**
     * This class is used for listening to the table header mouse events.
     */
    private class HeaderListener extends MouseAdapter implements MouseMotionListener {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        /*
         * If the user clicks to the sorting column (column defining the sort criterium and order), the sorting order is reversed.
         * If new sorting column is selected, the appropriate sorting order for column's datatype is set.
         */
        public void mouseClicked(MouseEvent e) {
            if (e.getModifiers() == InputEvent.BUTTON1_MASK) {
                int column = tableHeader.columnAtPoint(e.getPoint());
                int sortingColumn = headerRenderer.getSortingColumn();

                if (column == sortingColumn) {
                    headerRenderer.reverseSortingOrder();
                } else {
                    headerRenderer.setSortingColumn(column);

                    if (getInitialSorting(column)) {
                        headerRenderer.setSortingOrder(SORT_ORDER_ASC); // Default sort order for strings is Ascending
                    } else {
                        headerRenderer.setSortingOrder(SORT_ORDER_DESC); // Default sort order for numbers is Descending
                    }
                }

                tableHeader.repaint();

                sortByColumn(column, headerRenderer.getSortingOrder());
            }
        }

        public void mouseDragged(MouseEvent e) {
        }

        public void mouseMoved(MouseEvent e) {
            int focusedColumn = tableHeader.columnAtPoint(e.getPoint());

            if ((focusedColumn != lastFocusedColumn) && (focusedColumn != -1)) {
                tableHeader.setToolTipText(SortableTableModel.this.getColumnToolTipText(focusedColumn));
                lastFocusedColumn = focusedColumn;
            }
        }

        /*
         * Here the active header button is programatically pressed
         */
        public void mousePressed(MouseEvent e) {
            if ((e.getModifiers() == InputEvent.BUTTON1_MASK) && (tableHeader.getResizingColumn() == null)) {
                headerRenderer.setPressedColumn(tableHeader.columnAtPoint(e.getPoint()));
                tableHeader.repaint();
            }
        }

        /*
         * Here the active header button is programatically released
         */
        public void mouseReleased(MouseEvent e) {
            if (e.getModifiers() == InputEvent.BUTTON1_MASK) {
                headerRenderer.setPressedColumn(-1);
                tableHeader.repaint();
            }
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    public static final boolean SORT_ORDER_DESC = false;
    public static final boolean SORT_ORDER_ASC = true;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private CustomSortableHeaderRenderer headerRenderer;
    private HeaderListener headerListener;
    private ImageIcon sortAscIcon = new ImageIcon(SortableTableModel.class.getResource("/org/netbeans/lib/profiler/ui/resources/sortAsc.png")); //NOI18N
    private ImageIcon sortDescIcon = new ImageIcon(SortableTableModel.class.getResource("/org/netbeans/lib/profiler/ui/resources/sortDesc.png")); //NOI18N
    private JTableHeader tableHeader;
    private int lastFocusedColumn = -1;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public SortableTableModel() {
        headerListener = new HeaderListener();
        headerRenderer = new CustomSortableHeaderRenderer(sortAscIcon, sortDescIcon);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * After the table to which this model belongs has been set, this method allows to set the initial sorting column and sorting order.
     * @param sortingColumn The initial sorting column
     * @param sortingOrder The initial sorting order
     */
    public void setInitialSorting(int sortingColumn, boolean sortingOrder) {
        if (headerRenderer != null) {
            headerRenderer.setSortingColumn(sortingColumn);
            headerRenderer.setSortingOrder(sortingOrder);
        }
    }

    /**
     * @param column The table column index
     * @return Initial sorting for the specified column - if true, ascending, if false descending
     */
    public abstract boolean getInitialSorting(int column); /* {
       return (getColumnClass(column).equals(String.class));
       }*/

    public int getSortingColumn() {
        return headerRenderer.getSortingColumn();
    }

    public boolean getSortingOrder() {
        return headerRenderer.getSortingOrder();
    }

    /**
     * Assigns this SortableTableModel to the JTable and sets the custom renderer for the selectable table header.
     * @param table The JTable to set this table model to
     */
    public void setTable(JTable table) {
        TableColumnModel tableModel = table.getColumnModel();
        int n = tableModel.getColumnCount();

        for (int i = 0; i < n; i++) {
            tableModel.getColumn(i).setHeaderRenderer(headerRenderer);
        }

        if (tableHeader != table.getTableHeader()) {
            if (tableHeader != null) {
                tableHeader.removeMouseListener(headerListener);
                tableHeader.removeMouseMotionListener(headerListener);
                lastFocusedColumn = -1;
            }

            tableHeader = table.getTableHeader();
            tableHeader.setReorderingAllowed(false);
            tableHeader.addMouseListener(headerListener);
            tableHeader.addMouseMotionListener(headerListener);
        }
    }

    public abstract void sortByColumn(int column, boolean order);

    public String getColumnToolTipText(int column) {
        return null;
    }
}
