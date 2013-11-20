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
package org.netbeans.lib.profiler.ui.swing;

import org.netbeans.lib.profiler.ui.swing.renderer.Translatable;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.RowSorterEvent;
import javax.swing.event.RowSorterListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import org.netbeans.lib.profiler.ui.UIConstants;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.modules.profiler.api.icons.GeneralIcons;
import org.netbeans.modules.profiler.api.icons.Icons;

/**
 *
 * @author Jiri Sedlacek
 */
public class ProfilerTable extends JTable {
    
    public static final String PROP_NO_HOVER = "ProfilerTableHover_NoHover"; // NOI18N
    
    public ProfilerTable(TableModel model, boolean sortable, boolean hideableColums,
                         int[] scrollableColumns, boolean keepUnfocusedSelection) {
        super(model);
        
        this.hideableColums = hideableColums;
        this.keepUnfocusedSelection = keepUnfocusedSelection;
        
        setupModels(sortable);
        setupAppearance();
        
        if (scrollableColumns != null && scrollableColumns.length > 0)
            initScrollableColumns(scrollableColumns);
    }
    
    
    // --- Initialization ------------------------------------------------------
    
    protected void setupModels(boolean sortable) {
        setAutoCreateRowSorter(false);
        if (sortable) setRowSorter(createRowSorter());
    }
    
    // --- UI tweaks -----------------------------------------------------------
    
    protected void setupAppearance() {
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        setRowSelectionAllowed(true);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setGridColor(UIConstants.TABLE_VERTICAL_GRID_COLOR);
//        setSelectionBackground(UIConstants.TABLE_SELECTION_BACKGROUND_COLOR);
//        setSelectionForeground(UIConstants.TABLE_SELECTION_FOREGROUND_COLOR);
        setShowHorizontalLines(UIConstants.SHOW_TABLE_HORIZONTAL_GRID);
        setShowVerticalLines(UIConstants.SHOW_TABLE_VERTICAL_GRID);
        setRowMargin(UIConstants.TABLE_ROW_MARGIN);
        setRowHeight(UIUtils.getDefaultRowHeight() + 2);
        setBackground(UIUtils.getProfilerResultsBackground());
        
        ProfilerTableActions.install(this);
        ProfilerTableHover.install(this);
        
        getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "DEFAULT_ACTION"); // NOI18N
        getActionMap().put("DEFAULT_ACTION", new AbstractAction() { // NOI18N
                    public void actionPerformed(ActionEvent e) { performDefaultAction(); }
                });
        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2)
                    performDefaultAction();
            }
        });
        
        addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) { ProfilerTable.this.focusGained(); }
            public void focusLost(FocusEvent e)   { ProfilerTable.this.focusLost(); }
        });
    }
    
    private void focusGained() {
        repaint();
    }
    
    private void focusLost() {
        repaint();
    }
    
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component c = super.prepareRenderer(renderer, row, column);
        
        boolean isSelected = isCellSelected(row, column);
        
        if (isSelected && isEnabled()) {
            boolean focusOwner = keepUnfocusedSelection || super.isFocusOwner();
            c.setForeground(focusOwner ? getSelectionForeground() : UIUtils.getUnfocusedSelectionForeground());
            c.setBackground(focusOwner ? getSelectionBackground() : UIUtils.getUnfocusedSelectionBackground());
        } else if (!isEnabled()) {
            c.setForeground(UIManager.getColor("TextField.inactiveForeground")); // NOI18N
            c.setBackground(UIManager.getColor("TextField.inactiveBackground")); // NOI18N
        } else {
            c.setForeground(getForeground());
            c.setBackground((row & 0x1) == 0 ? getBackground() :
                            UIUtils.getDarker(getBackground()));
        }
        
        int col = convertColumnIndexToModel(column);
        
        // TODO: will be removed once custom renderers are implemented
        if (col > 0 && c instanceof JLabel) 
            ((JLabel)c).setHorizontalAlignment(JLabel.TRAILING);
//        else
//            ((JLabel)c).setHorizontalAlignment(JLabel.LEADING);
        
        if (!isCustomRendering && isScrollableColumn(col)) {
            int prefWidth = getColumnPreferredWidth(col);
            return getScrollableRenderer(c, col, prefWidth);
        } else {
            return c;
        }
    }
    
    private ScrollableRenderer _renderer;
    private ScrollableRenderer getScrollableRenderer(Component renderer, int column, int width) {
        if (_renderer == null) _renderer = new ScrollableRenderer();
        _renderer.setRenderer(renderer, getColumnOffset(column), width);
        return _renderer;
    }
    
    private class ScrollableRenderer extends Component {
        
        private Component impl;
        private Translatable implT;
        
        private int offset;
        private int prefWidth;
        
        void setRenderer(Component c, int o, int w) {
            impl = c;
            offset = o;
            prefWidth = w;
            
            implT = c instanceof Translatable ? (Translatable)c : null;
        }
        
        public void setBounds(int x, int y, int w, int h) {
            impl.setSize(Math.max(w, prefWidth), h);
        }
        
        public Dimension getPreferredSize() {
            return impl.getPreferredSize();
        }
        
        public void paint(Graphics g) {
            if (implT != null) {
                implT.translate(-offset, 0);
                impl.paint(g);
            } else {
                g.translate(-offset, 0);
                impl.paint(g);
                g.translate(offset, 0);
            }
        }
        
    }
    
    Component getRenderer(TableCellRenderer renderer, int row, int column) {
        isCustomRendering = true;
        try {
            return prepareRenderer(renderer, row, column);
        } finally {
            isCustomRendering = false;
        }
    }
    
    private boolean isCustomRendering;
    public boolean isFocusOwner() {
        return !isCustomRendering && super.isFocusOwner();
    }
    
    public boolean isCellEditable(int row, int column) {
        return false;
    }
    
    public void setVisibleRows(int rows) {
        Dimension size = super.getPreferredScrollableViewportSize();
        size.height = rows * getRowHeight();
        setPreferredScrollableViewportSize(size);
    }
    
    // --- Selection -----------------------------------------------------------
    
    private final boolean keepUnfocusedSelection;
    
    public void selectRow(int row, boolean scrollToVisible) {
        setRowSelectionInterval(row, row);
        if (scrollToVisible) scrollRectToVisible(getCellRect(row, getSelectedColumn(), true));
    }
    
    public void selectColumn(int column, boolean scrollToVisible) {
        setColumnSelectionInterval(column, column);
        if (scrollToVisible) scrollRectToVisible(getCellRect(getSelectedRow(), column, true));
    }
    
    public void selectValue(Object value, int column) {
        if (value == null) return;
        
        for (int row = 0; row < getRowCount(); row++)
            if (value.equals(getValueAt(row, column))) {
                selectRow(row, true);
                break;
            }
    }
    
    // --- Column model --------------------------------------------------------
    
    private boolean columnWidthsValid;
    private Set<Integer> scrollableColumns;
    
    ProfilerColumnModel _getColumnModel() {
        return (ProfilerColumnModel)getColumnModel();
    }
    
    protected TableColumnModel createDefaultColumnModel() {
        return new ProfilerColumnModel();
    }
    
    public void setFitWidthColumn(int column) {
        _getColumnModel().setFitWidthColumn(column);
    }
    
    public void setDefaultColumnWidth(int width) {
        _getColumnModel().setDefaultColumnWidth(width);
    }
    
    public void setDefaultColumnWidth(int column, int width) {
        _getColumnModel().setDefaultColumnWidth(column, width);
    }
    
    public void setColumnToolTips(String[] toolTips) {
        _getColumnModel().setColumnToolTips(toolTips);
    }
    
    public void setColumnVisibility(int column, boolean visible) {
        ProfilerColumnModel cModel = _getColumnModel();
        TableColumn col = cModel.getColumn(convertColumnIndexToView(column));
        cModel.setColumnVisibility(col, visible, this);
    }
    
    public boolean isColumnVisible(int column) {
        int _column = convertColumnIndexToView(column);
        return _getColumnModel().isColumnVisible(_column);
    }
    
    public void setColumnOffset(int column, int offset) {
        if (_getColumnModel().setColumnOffset(column, offset)) {
            column = convertColumnIndexToView(column);
            Rectangle rect = getCellRect(0, column, true);
            repaint(rect.x, 0, rect.width, getHeight());
        }
    }
    
    public Set<Integer> getScrollableColumns() {
        return scrollableColumns;
    }
    
    public boolean isScrollableColumn(int column) {
        return scrollableColumns != null && scrollableColumns.contains(column);
    }
    
    public int getColumnOffset(int column) {
        return _getColumnModel().getColumnOffset(column);
    }
    
    private void updateColumnsPreferredWidth() {
        Rectangle visible = getVisibleRect();
        if (visible.isEmpty()) return;
        
        Point visibleP = visible.getLocation();
        int first = rowAtPoint(visible.getLocation());
        visibleP.translate(0, visible.height - 1);
        int last = rowAtPoint(visibleP);
        
        ProfilerColumnModel cModel = _getColumnModel();
        
        for (int column : scrollableColumns) {
            int _column = convertColumnIndexToView(column);
            int width = computeColumnPreferredWidth(column, _column, first, last);
            cModel.setColumnPreferredWidth(column, width);
        }
        
        columnWidthsValid = true;
    }
    
    public void updateColumnPreferredWidth(int column) {
        Rectangle visible = getVisibleRect();
        if (visible.isEmpty()) return;
        
        Point visibleP = visible.getLocation();
        int first = rowAtPoint(visible.getLocation());
        visibleP.translate(0, visible.height - 1);
        int last = rowAtPoint(visibleP);
        
        int _column = convertColumnIndexToView(column);
        
        int width = computeColumnPreferredWidth(column, _column, first, last);
        _getColumnModel().setColumnPreferredWidth(column, width);
    }
    
    protected int computeColumnPreferredWidth(int modelIndex, int viewIndex, int firstRow, int lastRow) {
        int width = 0;
        for (int row = firstRow; row <= lastRow; row++) {
            TableCellRenderer renderer = getCellRenderer(row, viewIndex);
            Component component = getRenderer(renderer, row, viewIndex);
            width = Math.max(component.getPreferredSize().width, width);
        }
        return width;
    }
    
    public int getColumnPreferredWidth(int column) {
        if (!columnWidthsValid) updateColumnsPreferredWidth();
        return _getColumnModel().getColumnPreferredWidth(column);
    }
    
    private void initScrollableColumns(int[] columns) {
        scrollableColumns = new HashSet(columns.length);
        for (final int column : columns) scrollableColumns.add(column);
        
        columnWidthsValid = false;
        
        if (isSortable()) {
            getRowSorter().addRowSorterListener(new RowSorterListener() {
                public void sorterChanged(RowSorterEvent e) {
                    if (RowSorterEvent.Type.SORTED.equals(e.getType()))
                        updateColumnsPreferredWidth();
                }
            });
        } else {
            getModel().addTableModelListener(new TableModelListener() {
                public void tableChanged(TableModelEvent e) {
                    updateColumnsPreferredWidth();
                }
            }); 
        }
    }
    
    
    // --- Columns hiding & layout ---------------------------------------------
    
    private final boolean hideableColums;
    
    protected void configureEnclosingScrollPane() {
        super.configureEnclosingScrollPane();

        JScrollPane scrollPane = getEnclosingScrollPane();
        if (scrollPane != null) {
            boolean hideable = hideableColums && !UIUtils.isAquaLookAndFeel();
            final ActionListener chooser = !hideable ? null : new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    chooseColumns((Component)e.getSource(), null);
                }
            };
            HeaderComponent corner = !hideable ? new HeaderComponent(chooser) :
                                                 new HeaderComponent(chooser) {
                private Icon icon = Icons.getIcon(GeneralIcons.SORT_DESCENDING);
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    int x = (getWidth() - icon.getIconWidth()) / 2 - 1;
                    int y = (getHeight() - icon.getIconHeight()) / 2;
                    icon.paintIcon(this, g, x, y);
                }
            };
            scrollPane.setCorner(JScrollPane.UPPER_RIGHT_CORNER, corner);
            
            if (scrollableColumns != null && !scrollableColumns.isEmpty())
                scrollPane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
                    public void adjustmentValueChanged(AdjustmentEvent e) {
                        if (!e.getValueIsAdjusting()) updateColumnsPreferredWidth();
                    }
                });
        }
    }
    
    private JScrollPane getEnclosingScrollPane() {
        Container parent = SwingUtilities.getUnwrappedParent(this);
        if (!(parent instanceof JViewport)) return null;
        Container scroll = ((JViewport)parent).getParent();
        return scroll instanceof JScrollPane ? (JScrollPane)scroll : null;
    }
    
    private void chooseColumns(Component source, Point p) {
        final ProfilerColumnModel cModel = _getColumnModel();
        List<TableColumn> columns = Collections.list(cModel.getColumns());
        
// --- NOTE: uncomment to sort column checkboxes by model index ----------------
//        Collections.sort(columns, new Comparator<TableColumn>() {
//            public int compare(TableColumn c1, TableColumn c2) {
//                return Integer.compare(c1.getModelIndex(), c2.getModelIndex());
//            }
//        });
        
        JPopupMenu popup = new JPopupMenu();
        for (final TableColumn c : columns)
            popup.add(new JCheckBoxMenuItem(c.getHeaderValue().toString(), c.getWidth() > 0) {
                {
                    setEnabled(c.getModelIndex() != 0);
//                    setToolTipText(cModel.getColumnToolTip(c.getModelIndex()));
                }
                protected void fireActionPerformed(ActionEvent e) {
                    cModel.setColumnVisibility(c, isSelected(), ProfilerTable.this);
                }
            });
        
        if (p == null) p = new Point(source.getSize().width -
                                     popup.getPreferredSize().width - 1,
                                     source.getHeight() - 1);
        popup.show(source, p.x, p.y);
    }
    
    public void doLayout() {
        ProfilerColumnModel cModel = _getColumnModel();
        int toResizeIndex = cModel.getFitWidthColumn();
        JTableHeader header = toResizeIndex != -1 ? getTableHeader() : null;
        boolean resizing = header == null ? false : header.getResizingColumn() != null;
        if (resizing || toResizeIndex == -1) {
            super.doLayout();
        } else {
            Enumeration<TableColumn> columns = cModel.getColumns();
            TableColumn toResizeColumn = null;
            int columnsWidth = 0;
            while (columns.hasMoreElements()) {
                TableColumn column = columns.nextElement();
                if (column.getModelIndex() == toResizeIndex) {
                    if (!cModel.isColumnVisible(column)) {
                        super.doLayout();
                        return;
                    }
                    toResizeColumn = column;
                } else {
                    columnsWidth += column.getWidth();
                }
            }
            toResizeColumn.setWidth(getWidth() - columnsWidth);
        }
    }
    
    // --- Row sorter ----------------------------------------------------------
    
    boolean isSortable() {
        return getRowSorter() != null;
    }
    
    ProfilerRowSorter _getRowSorter() {
        return (ProfilerRowSorter)getRowSorter();
    }
    
    protected TableRowSorter createRowSorter() {
        ProfilerRowSorter s = new ProfilerRowSorter(getModel());
        s.setDefaultSortOrder(SortOrder.DESCENDING);
        s.setDefaultSortOrder(0, SortOrder.ASCENDING);
        s.setSortColumn(0);
        return s;
    }
    
    public void disableColumnSorting(int column) {
        ProfilerRowSorter sorter = _getRowSorter();
        if (sorter == null) return;
        int _column = convertColumnIndexToView(column);
        sorter.setSortable(_column, false);
    }
    
    public void setSortColumn(int column) {
        if (isSortable()) _getRowSorter().setSortColumn(column);
    }
    
    public void setDefaultSortOrder(int column, SortOrder sortOrder) {
        if (isSortable()) _getRowSorter().setDefaultSortOrder(column, sortOrder);
    }
    
    // --- Default action ------------------------------------------------------
    
    private Action defaultAction;
    
    public void setDefaultAction(Action action) {
        this.defaultAction = action;
    }
    
    public void performDefaultAction() {
        if (defaultAction != null) defaultAction.actionPerformed(null);
    }
    
    // --- Persistence ---------------------------------------------------------
    
    public void loadColumns(Properties properties) {
        _getColumnModel().loadFromStorage(properties, this);
        if (isSortable()) _getRowSorter().loadFromStorage(properties, this);
    }
    
    public void saveColumns(Properties properties) {
        _getColumnModel().saveToStorage(properties, this);
        if (isSortable()) _getRowSorter().saveToStorage(properties, this);
    }
    
    // --- Header tweaks -------------------------------------------------------
    
    protected JTableHeader createDefaultTableHeader() {
        return new JTableHeader(columnModel) {
            public String getToolTipText(MouseEvent e) {
                int index = columnAtPoint(e.getPoint());
                if (index == -1) return null;
                ProfilerColumnModel cModel = _getColumnModel();
                TableColumn column = cModel.getColumn(index);
                String toolTip = cModel.getColumnToolTip(column.getModelIndex());
                return toolTip;
            }
            protected void processMouseEvent(MouseEvent e) {
                if (hideableColums && UIUtils.isAquaLookAndFeel() && e.isPopupTrigger())
                    chooseColumns((Component)e.getSource(), e.getPoint());
                super.processMouseEvent(e.getClickCount() > 1 ? clearClicks(e) : e);
            }
        };
    }
    
    private MouseEvent clearClicks(MouseEvent e) {
        // Clears doubleclicks to prevent misses when switching sort order
        MouseEvent ee = new MouseEvent((Component)e.getSource(), e.getID(), e.getWhen(),
                                       e.getModifiers(), e.getX(), e.getY(),
                                       1, e.isPopupTrigger(), e.getButton());
        return ee;
    }
    
}
