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

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.RowSorterEvent;
import javax.swing.event.RowSorterListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import org.netbeans.lib.profiler.ui.UIConstants;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.swing.renderer.Movable;
import org.netbeans.lib.profiler.ui.swing.renderer.ProfilerRenderer;
import org.netbeans.modules.profiler.api.icons.GeneralIcons;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.openide.util.Lookup;

/**
 *
 * @author Jiri Sedlacek
 */
public class ProfilerTable extends JTable {
    
    private static ResourceBundle BUNDLE() {
        return ResourceBundle.getBundle("org.netbeans.lib.profiler.ui.swing.Bundle"); // NOI18N
    }
    
    public static final String PROP_NO_HOVER = "ProfilerTableHover_NoHover"; // NOI18N
    
    public ProfilerTable(TableModel model, boolean sortable,
                         boolean hideableColums, int[] scrollableColumns) {
        super(model);
        
        this.hideableColums = hideableColums;
        
        setupModels(sortable);
        setupAppearance();
        
        if (scrollableColumns != null && scrollableColumns.length > 0)
            initScrollableColumns(scrollableColumns);
        
        tweak();
    }
    
    
    // --- Initialization ------------------------------------------------------
    
    protected void setupModels(boolean sortable) {
        setAutoCreateRowSorter(false);
        if (sortable) setRowSorter(createRowSorter());
    }
    
    public void createDefaultColumnsFromModel() {
        TableModel m = getModel();
        if (m != null) {
            // Remove any current columns
            ProfilerColumnModel cm = _getColumnModel();
            while (cm.getColumnCount() > 0)
                cm.removeColumn(cm.getColumn(0));

            // Create new columns from the data model info
            for (int i = 0; i < m.getColumnCount(); i++)
                addColumn(cm.createTableColumn(i));
        }
    }
    
    // --- UI tweaks -----------------------------------------------------------
    
    private void tweak() {
        for (Tweaker tweaker : Lookup.getDefault().lookupAll(Tweaker.class))
            tweaker.tweak(this);
    }
    
    protected void setupAppearance() {
        setAutoResizeMode(AUTO_RESIZE_NEXT_COLUMN);
        setRowSelectionAllowed(true);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setGridColor(UIConstants.TABLE_VERTICAL_GRID_COLOR);
        setShowHorizontalLines(UIConstants.SHOW_TABLE_HORIZONTAL_GRID);
        setShowVerticalLines(UIConstants.SHOW_TABLE_VERTICAL_GRID);
        setRowMargin(UIConstants.TABLE_ROW_MARGIN);
        setRowHeight(UIUtils.getDefaultRowHeight() + 2);
        setBackground(UIUtils.getProfilerResultsBackground());
        
        if (UIUtils.isNimbusLookAndFeel() && Boolean.TRUE.equals(UIManager.getBoolean("nb.dark.theme"))) // NOI18N
            setForeground(UIManager.getColor("text")); // NOI18N
        
        ProfilerTableActions.install(this);
        ProfilerTableHover.install(this);
        
        getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "DEFAULT_ACTION"); // NOI18N
        getActionMap().put("DEFAULT_ACTION", new AbstractAction() { // NOI18N
                    public void actionPerformed(ActionEvent e) { performDefaultAction(); }
                });
        
        addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) { ProfilerTable.this.focusGained(); }
            public void focusLost(FocusEvent e)   { ProfilerTable.this.focusLost(); }
        });
    }
    
    public Color getBackground() {
        return isEnabled() ? super.getBackground() :
               UIManager.getColor("TextField.inactiveBackground"); // NOI18N
    }
    
    private void focusGained() {
        repaint();
    }
    
    private void focusLost() {
        repaint();
    }
    
    public void setDefaultRenderer(Class<?> columnClass, ProfilerRenderer renderer) {
        super.setDefaultRenderer(columnClass, createTableCellRenderer(renderer));
    }
    
    public void setColumnRenderer(int column, ProfilerRenderer renderer) {
        int _column = convertColumnIndexToModel(column);
        TableColumn tColumn = getColumnModel().getColumn(_column);
        tColumn.setCellRenderer(createTableCellRenderer(renderer));
    }
    
    public static TableCellRenderer createTableCellRenderer(final ProfilerRenderer renderer) {
        return new TableCellRenderer() {
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                renderer.setValue(value, table.convertRowIndexToModel(row));
                return renderer.getComponent();
            }
            public String toString() {
                return renderer.toString();
            }
        };
    }
    
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component c = super.prepareRenderer(renderer, row, column);
        
        boolean isSelected = isCellSelected(row, column);
        
        if (isSelected && isEnabled()) {
            boolean focusOwner = !shadeUnfocusedSelection || super.isFocusOwner();
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
        
        c.move(0, 0);
        
        int col = convertColumnIndexToModel(column);        
        if (!isCustomRendering() && isScrollableColumn(col)) {
            int prefWidth = getColumnPreferredWidth(col);
            return getScrollableRenderer(c, col, prefWidth);
        } else {
            return c;
        }
    }
    
    public Component prepareEditor(TableCellEditor editor, int row, int column) {
        Component c = super.prepareEditor(editor, row, column);
        
        c.setForeground(getSelectionForeground());
        c.setBackground(getSelectionBackground());
        
        return c;
    }
    
    private ScrollableRenderer _renderer;
    private ScrollableRenderer getScrollableRenderer(Component renderer, int column, int width) {
        if (_renderer == null) _renderer = new ScrollableRenderer();
        _renderer.setRenderer(renderer, getColumnOffset(column), width);
        return _renderer;
    }
    
    private class ScrollableRenderer extends Component {
        
        private Component impl;
        private Movable implM;
        
        private int offset;
        private int prefWidth;
        private int marginOffset;
        
        void setRenderer(Component c, int o, int w) {
            impl = c;
            offset = o;
            prefWidth = w;
            
            marginOffset = isLeadingAlign(impl) ? 0 : 
                           getColumnModel().getColumnMargin();
            
            implM = c instanceof Movable ? (Movable)c : null;
        }
        
        public void setBounds(int x, int y, int w, int h) {
            super.setBounds(x, y, w, h);
            if (prefWidth > w) offset += marginOffset;
            impl.setSize(Math.max(w, prefWidth), h);
        }
        
        public Dimension getPreferredSize() {
            return impl.getPreferredSize();
        }
        
        public void paint(Graphics g) {
            if (implM != null) {
                implM.move(-offset, 0);
                impl.paint(g);
            } else {
                g.translate(-offset, 0);
                impl.paint(g);
            }
        }
        
    }
    
    Component getRenderer(TableCellRenderer renderer, int row, int column, boolean sized) {
        isCustomRendering = true;
        try {
            Component comp = prepareRenderer(renderer, row, column);
            comp.setSize(comp.getPreferredSize().width, getRowHeight());
            if (sized) {
                comp.setSize(comp.getPreferredSize().width, getRowHeight());
                if (!isLeadingAlign(comp)) {
                    TableColumnModel m = getColumnModel();
                    int x = -comp.getWidth();
                    int c = m.getColumn(column).getWidth();
                    int _column = convertColumnIndexToModel(column);
                    if (isScrollableColumn(_column)) {
                        x += Math.max(c, getColumnPreferredWidth(_column));
                    } else {
                        x += c;
                    }
                    comp.move(x - m.getColumnMargin(), 0);
                }
            }
            
            return comp;
        } finally {
            isCustomRendering = false;
        }
    }
    
    public boolean isFocusOwner() {
        return !isCustomRendering() && super.isFocusOwner();
    }
    
    public void setVisibleRows(int rows) {
        Dimension size = super.getPreferredScrollableViewportSize();
        size.height = rows * getRowHeight();
        setPreferredScrollableViewportSize(size);
    }
    
    private boolean isCustomRendering;
    
    final boolean isCustomRendering() {
        return isCustomRendering;
    }
    
    // --- String value --------------------------------------------------------
    
    // row, column - view index
    public String getStringValue(int row, int column) {
        TableCellRenderer renderer = getCellRenderer(row, column);
        if (renderer instanceof ProfilerRenderer) {
            ((ProfilerRenderer)renderer).setValue(getValueAt(row, column), row);
        } else {
            prepareRenderer(renderer, row, column);
        }
        return renderer.toString();
    }
    
    // --- Main column ---------------------------------------------------------
    
    private int mainColumn = 0;
    
    public final void setMainColumn(int column) {
        mainColumn = column;
    }
    
    public final int getMainColumn() {
        return mainColumn;
    }
    
    // --- Selection -----------------------------------------------------------
    
    private boolean shadeUnfocusedSelection = false;
    
    boolean internal;
    private Object selection;
    private ListSelectionListener selectionListener;
    
    public void setSelectionModel(ListSelectionModel newModel) {
        ListSelectionModel oldModel = getSelectionModel();
        if (oldModel != null && selectionListener != null)
            oldModel.removeListSelectionListener(selectionListener);
        
        super.setSelectionModel(newModel);
        
        if (newModel != null) {
            if (selectionListener == null) selectionListener = new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) { if (!internal) saveSelection(); }
            };
            newModel.addListSelectionListener(selectionListener);
        }
    }
    
    protected void saveSelection() {
        int sel = getSelectionModel().getSelectionMode();
        selection = sel == ListSelectionModel.SINGLE_SELECTION ?
                getSelectedValue(mainColumn) : getSelectedValues(mainColumn).toArray();
    }
    
    protected void restoreSelection() {
        if (!(selection instanceof Object[])) selection = selectValue(selection, mainColumn, false);
        else selection = selectValues((Object[])selection, mainColumn, false);
    }
    
    public void selectRow(int row, boolean scrollToVisible) {
        internal = true;
        try { setRowSelectionInterval(row, row); saveSelection(); }
        finally { internal = false; }
        if (scrollToVisible) scrollRectToVisible(getCellRect(row, getSelectedColumn(), true));
    }
    
    public void selectColumn(int column, boolean scrollToVisible) {
        internal = true;
        try { setColumnSelectionInterval(column, column); }
        finally { internal = false; }
        if (scrollToVisible) scrollRectToVisible(getCellRect(getSelectedRow(), column, true));
    }
    
    public Object selectValue(Object value, int column, boolean scrollToVisible) {
        if (value == null) return null;
        
        int _column = convertColumnIndexToView(column);
        for (int row = 0; row < getRowCount(); row++) {
            Object _value = getValueAt(row, _column);
            if (value.equals(_value)) {
                selectRow(row, scrollToVisible);
                return _value;
            }
        }
        
        return null;
    }
    
    private Object[] selectValues(Object[] values, int column, boolean scrollToVisible) {
        if (values == null || values.length == 0) return null;
        
        Set<Object> toSelect = new HashSet(Arrays.asList(values));
        List<Object> selected = new ArrayList(toSelect.size());
        
        internal = true;
        try {
            int _column = convertColumnIndexToView(column);
            for (int row = 0; row < getRowCount(); row++) {
                Object _value = getValueAt(row, _column);
                if (toSelect.remove(_value)) {
                    if (selected.isEmpty()) {
                        setRowSelectionInterval(row, row);
                        if (scrollToVisible)
                            scrollRectToVisible(getCellRect(row, _column, true));
                    } else {
                        addRowSelectionInterval(row, row);
                    }
                    selected.add(_value);
                    if (toSelect.isEmpty()) break;
                }
            }
        } finally { internal = false; }
        
        return selected.isEmpty() ? null : selected.toArray();
    }
    
    public Object getSelectedValue(int column) {
        int row = getSelectedRow();
        if (row == -1) return null;
        if (row >= getModel().getRowCount()) return null; // #239936
        return getValueAt(row, convertColumnIndexToView(column));
    }
    
    public List getSelectedValues(int column) {
        List values = new ArrayList();
        int col = convertColumnIndexToView(column);
        int rowCount = getModel().getRowCount();
        for (int row : getSelectedRows())
            if (row < rowCount) // #239936
                values.add(getValueAt(row, col));
        return values;
    }
    
    public void tableChanged(TableModelEvent e) {
        internal = true;
        try { super.tableChanged(e); }
        finally { internal = false; }
        restoreSelection();
    }
    
    public final void setShadeUnfocusedSelection(boolean shade) {
        shadeUnfocusedSelection = shade;
    }
    
    public final boolean shadesUnfocusedSelection() {
        return shadeUnfocusedSelection;
    }
    
    // --- Traversing rows -----------------------------------------------------
    
    int getNextRow(int row) {
        return ++row == getRowCount() ? 0 : row;
    }
    
    int getPreviousRow(int row) {
        return --row == -1 ? getRowCount() - 1 : row;
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
    
    protected void updateColumnsPreferredWidth() {
        if (scrolling || scrollableColumns == null) return;
        
        ProfilerColumnModel cModel = _getColumnModel();
        
        if (getRowCount() == 0) {
            for (int column : scrollableColumns)
                cModel.setColumnPreferredWidth(column, 0);
            return;
        }
        
        Rectangle visible = getVisibleRect();
        if (visible.isEmpty()) return;
        
        Point visibleP = visible.getLocation();
        int first = rowAtPoint(visibleP);
        visibleP.translate(0, visible.height - 1);
        int last = rowAtPoint(visibleP);
        
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
            Component component = getRenderer(renderer, row, viewIndex, false);
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
                    // Must invoke later, JTree.getRowCount() not ready yet
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() { updateColumnsPreferredWidth(); }
                    });
                }
            }); 
        }
    }
    
    boolean isLeadingAlign(int column) {
        if (getRowCount() == 0) return true;
        int _column = convertColumnIndexToView(column);
        TableCellRenderer r = getCellRenderer(0, _column);
        return isLeadingAlign(getRenderer(r, 0, _column, false));
    }
    
    static boolean isLeadingAlign(Component component) {
        int alignment;
        
        if (component instanceof ProfilerRenderer) {
            alignment = ((ProfilerRenderer)component).getHorizontalAlignment();
        } else if (component instanceof JLabel) {
            alignment = ((JLabel)component).getHorizontalAlignment();
        } else {
            alignment = SwingConstants.LEADING;
        }
        
        return alignment == SwingConstants.LEADING ||
               alignment == SwingConstants.LEFT ||
               alignment == SwingConstants.CENTER;
    }
    
    
    // --- Columns hiding & layout ---------------------------------------------
    
    private final boolean hideableColums;
    private boolean scrolling;
    
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
                private Icon icon = Icons.getIcon(GeneralIcons.POPUP_ARROW);
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    int x = (getWidth() - icon.getIconWidth()) / 2 - 1;
                    int y = (getHeight() - icon.getIconHeight()) / 2;
                    icon.paintIcon(this, g, x, y);
                }
            };
            if (hideable) corner.setToolTipText(BUNDLE().getString("ProfilerTable_ShowHideColumns")); // NOI18N
            scrollPane.setCorner(JScrollPane.UPPER_RIGHT_CORNER, corner);
            
            if (scrollableColumns != null && !scrollableColumns.isEmpty())
                scrollPane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
                    public void adjustmentValueChanged(AdjustmentEvent e) {
                        scrolling = e.getValueIsAdjusting();
                        updateColumnsPreferredWidth();
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
                    setEnabled(c.getModelIndex() != mainColumn);
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
        TableColumn res = getTableHeader().getResizingColumn();
        if (res != null) {
            // Resizing column
            int delta = getWidth() - cModel.getTotalColumnWidth();
            TableColumn next = cModel.getNextVisibleColumn(res);
            if (res == next) {
                res.setWidth(res.getWidth() + delta);
            } else {
                next.setWidth(next.getWidth() + delta);
            }
        } else {
            // Resizing table
            int toResizeIndex = cModel.getFitWidthColumn();
            if (toResizeIndex == -1) {
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
                if (toResizeColumn != null) toResizeColumn.setWidth(getWidth() - columnsWidth);

                // instead of super.doLayout()
                layout();
            }
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
    
    public int getSortColumn() {
        return isSortable() ? _getRowSorter().getSortColumn() : -1;
    }
    
    public void setSecondarySortColumn(int column) {
        if (isSortable()) _getRowSorter().setSecondarySortColumn(column);
    }
    
    public void setDefaultSortOrder(SortOrder sortOrder) {
        if (isSortable()) _getRowSorter().setDefaultSortOrder(sortOrder);
    }
    
    public void setDefaultSortOrder(int column, SortOrder sortOrder) {
        if (isSortable()) _getRowSorter().setDefaultSortOrder(column, sortOrder);
    }
    
    // --- Row filter ----------------------------------------------------------
    
    // false = OR, true = AND
    public void setFiltersMode(boolean mode) {
        _getRowSorter().setFiltersMode(mode);
    }
    
    public boolean getFiltersMode() {
        return _getRowSorter().getFiltersMode();
    }
    
    public void addRowFilter(RowFilter filter) {
        _getRowSorter().addRowFilter(filter);
    }
    
    public void removeRowFilter(RowFilter filter) {
        _getRowSorter().removeRowFilter(filter);
    }
    
    public void setRowFilter(RowFilter filter) {
        _getRowSorter().setRowFilter(filter);
    }
    
    public RowFilter getRowFilter() {
        return _getRowSorter().getRowFilter();
    }
    
    // --- Default action ------------------------------------------------------
    
    private Action defaultAction;
    
    public void setDefaultAction(Action action) {
        this.defaultAction = action;
    }
    
    public void performDefaultAction() {
        if (defaultAction != null) defaultAction.actionPerformed(null);
    }
    
    // --- Popup menu ----------------------------------------------------------
    
    private long pressedWhen;
    private Point pressedPoint;
    private boolean providesPopupMenu;
    
    public final void providePopupMenu(boolean provide) {
        providesPopupMenu = provide;
    }
    
    public final boolean providesPopupMenu() {
        return providesPopupMenu;
    }
    
    protected void populatePopup(JPopupMenu popup, Object value, Object userValue) {
        // Implementation here
    }
    
    public final JMenuItem createCopyMenuItem() {
        final int row = getSelectedRow();
        
        JMenu copyItem = new JMenu(BUNDLE().getString("ProfilerTable_CopyMenu")); // NOI118N
        
        JMenuItem copyRowItem = new JMenuItem(BUNDLE().getString("ProfilerTable_CopyRowItem")) { // NOI118N
            protected void fireActionPerformed(ActionEvent e) {
                StringBuilder val = new StringBuilder();
                List<TableColumn> columns = Collections.list(_getColumnModel().getColumns());
                for (int col = 0; col < columns.size(); col++) if (columns.get(col).getWidth() > 0)
                    val.append("\t").append(getStringValue(row, col)); // NOI118N
                StringSelection s = new StringSelection(val.toString().trim());
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(s, s);
            }
        };
        copyItem.add(copyRowItem);
        
        copyItem.addSeparator();
        
        String genericItemName = BUNDLE().getString("ProfilerTable_CopyColumnItem"); // NOI118N
        List<TableColumn> columns = Collections.list(_getColumnModel().getColumns());
        for (int col = 0; col < columns.size(); col++) {
            final int _col = col;
            TableColumn column = columns.get(col);
            if (column.getWidth() > 0) {
                String columnName = column.getHeaderValue().toString();
                copyItem.add(new JMenuItem(MessageFormat.format(genericItemName, columnName)) {
                    protected void fireActionPerformed(ActionEvent e) {
                        StringSelection s = new StringSelection(getStringValue(row, _col));
                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(s, s);
                    }
                });
            }
        }
        
        return copyItem;
    }
    
    protected void popupShowing() {}
    
    protected void popupHidden() {}
    
    public Object getValueForRow(int row) {
        if (row == -1) return null;
        if (row >= getModel().getRowCount()) return null; // #239936
        return getValueAt(row, convertColumnIndexToView(mainColumn));
    }
    
    public Object getUserValueForRow(int row) {
        return getValueForRow(row);
    }
    
    protected void processMouseEvent(MouseEvent e) {
        // --- Resolve CellTips/MouseEvent incompatibilities -------------------
        //     TBD: doesn't work for heavyweight popups (RELEASED / CLICKED)
        MouseEvent generatedClick = null;
        if (e.getID() == MouseEvent.MOUSE_PRESSED) {
            pressedWhen = e.getWhen();
            pressedPoint = null;
        } else if (e.getID() == MouseEvent.MOUSE_RELEASED) {
            if (e.getWhen() - pressedWhen == 1) {
                // #241878 dispatch MOUSE_RELEASED after forwarding MOUSE_PRESSED
                pressedPoint = e.getPoint();
                super.processMouseEvent(e);
                return;
            } else if (e.getPoint().equals(pressedPoint)) {
                pressedPoint = null;
                generatedClick = new MouseEvent(e.getComponent(), MouseEvent.MOUSE_CLICKED,
                                                e.getWhen() + 1, e.getModifiers(),
                                                e.getX(), e.getY(), e.getClickCount(),
                                                e.isPopupTrigger(), e.getButton());
            }
            pressedWhen = 0;
        }
        // ---------------------------------------------------------------------
        
        boolean popupEvent = providesPopupMenu && SwingUtilities.isRightMouseButton(e);
        boolean clickEvent = e.getID() == MouseEvent.MOUSE_CLICKED && SwingUtilities.isLeftMouseButton(e);
        int row = rowAtPoint(e.getPoint());
        
        // Do not process doubleclick in editable cell (checkbox)
        if (clickEvent && row != -1 && e.getClickCount() > 1) {
            if (isCellEditable(row, columnAtPoint(e.getPoint())))
                e = clearClicks(e);
        }
        
        // Right-press selects row for popup
        if (popupEvent && e.getID() == MouseEvent.MOUSE_PRESSED && row != -1) {
            ListSelectionModel sel = getSelectionModel();
            if (sel.getSelectionMode() == ListSelectionModel.SINGLE_SELECTION ||
                !sel.isSelectedIndex(row)) selectRow(row, true);
        }
        
        super.processMouseEvent(e);
        
        // Right-click selects row and opens popup
        if (popupEvent && e.getID() == MouseEvent.MOUSE_CLICKED && row != -1) {
            ListSelectionModel sel = getSelectionModel();
            if (sel.getSelectionMode() == ListSelectionModel.SINGLE_SELECTION ||
                !sel.isSelectedIndex(row)) selectRow(row, true);
            final MouseEvent me = e;
            SwingUtilities.invokeLater(new Runnable() {
                public void run() { showPopupMenu(me); };
            });
        }
        
        // Only perform default action if not already processed (expand tree)
        if (!e.isConsumed() && clickEvent && e.getClickCount() == 2) performDefaultAction();
        
        if (generatedClick != null) processMouseEvent(generatedClick);
    }
    
    protected void processKeyEvent(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_CONTEXT_MENU ||
           (code == KeyEvent.VK_F10 && e.getModifiers() == InputEvent.SHIFT_MASK)) {
            e.consume();
            showPopupMenu(null);
        }
        
        super.processKeyEvent(e);
    }
    
    private void showPopupMenu(MouseEvent e) {
        JPopupMenu popup = new JPopupMenu() {
            public void setVisible(boolean visible) {
                if (visible) popupShowing();
                super.setVisible(visible);
                if (!visible) popupHidden();
            }
        };
        
        int row = getSelectedRow();
        Object value = getValueForRow(row);
        Object userValue = getUserValueForRow(row);
        populatePopup(popup, value, userValue);
        
        if (popup.getComponentCount() > 0) {
            if (e == null) {
                boolean b = row == -1;
                int c = b ? -1 : convertColumnIndexToView(mainColumn);
                Rectangle t = b ? getVisibleRect() : getCellRect(row, c, false);
                Dimension s = popup.getPreferredSize();
                int x = t.x + (t.width - s.width) / 2;
                int y = t.y + (b ? (t.height - s.height) / 2 : getRowHeight() - 4);
                popup.show(this, Math.max(x, 0), Math.max(y, 0));
            } else {
                popup.show(this, e.getX(), e.getY());
            }
        }
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
            public void setResizingColumn(TableColumn aColumn) {
                _getColumnModel().setResizingColumn(aColumn);
                super.setResizingColumn(aColumn);
            }
        };
    }
    
    protected static MouseEvent clearClicks(MouseEvent e) {
        // Clear unwanted doubleclicks
        return new MouseEvent((Component)e.getSource(), e.getID(), e.getWhen(),
                              e.getModifiers(), e.getX(), e.getY(),
                              1, e.isPopupTrigger(), e.getButton());
    }
    
    
    public static interface Tweaker {
        
        public void tweak(ProfilerTable table);
        
    } 
    
}
