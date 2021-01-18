/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.ui.components;

import org.graalvm.visualvm.lib.jfluid.results.CCTNode;
import org.graalvm.visualvm.lib.ui.UIUtils;
import org.graalvm.visualvm.lib.ui.components.table.*;
import org.graalvm.visualvm.lib.ui.components.tree.EnhancedTreeCellRenderer;
import org.graalvm.visualvm.lib.ui.components.tree.TreeCellRendererPersistent;
import org.graalvm.visualvm.lib.ui.components.treetable.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.util.List;
import org.graalvm.visualvm.lib.profiler.api.icons.GeneralIcons;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;


/**
 * JTreeTable component implementation
 *
 * @author Jiri Sedlacek
 * @author Ian Formanek
 */
public class JTreeTable extends JTable implements CellTipAware, MouseListener, MouseMotionListener, MouseWheelListener,
                                                  KeyListener {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    /**
     * ListToTreeSelectionModelWrapper extends DefaultTreeSelectionModel to
     * listen for changes in the ListSelectionModel it maintains. Once a change
     * in the ListSelectionModel happens, the paths are updated in the
     * DefaultTreeSelectionModel.
     */
    class ListToTreeSelectionModelWrapper extends DefaultTreeSelectionModel {
        //~ Inner Classes --------------------------------------------------------------------------------------------------------

        /**
         * Class responsible for calling updateSelectedPathsFromSelectedRows
         * when the selection of the list changes.
         */
        class ListSelectionHandler implements ListSelectionListener {
            //~ Methods ----------------------------------------------------------------------------------------------------------

            public void valueChanged(ListSelectionEvent e) {
                if (treeTableModelAdapter.isFiringChange()) return;
                updateSelectedPathsFromSelectedRows();
            }
        }

        //~ Instance fields ------------------------------------------------------------------------------------------------------

        /**
         * Set to true when we are updating the ListSelectionModel.
         */
        protected boolean updatingListSelectionModel;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public ListToTreeSelectionModelWrapper() {
            super();
            getListSelectionModel().addListSelectionListener(createListSelectionListener());
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        /**
         * This is overridden to set <code>updatingListSelectionModel</code>
         * and message super. This is the only place DefaultTreeSelectionModel
         * alters the ListSelectionModel.
         */
        public void resetRowSelection() {
            if (!updatingListSelectionModel) {
                updatingListSelectionModel = true;

                try {
                    super.resetRowSelection();
                } finally {
                    updatingListSelectionModel = false;
                }
            }

            // Notice how we don't message super if
            // updatingListSelectionModel is true. If
            // updatingListSelectionModel is true, it implies the
            // ListSelectionModel has already been updated and the
            // paths are the only thing that needs to be updated.
        }

        /**
         * Creates and returns an instance of ListSelectionHandler.
         */
        protected ListSelectionListener createListSelectionListener() {
            return new ListSelectionHandler();
        }

        /**
         * If <code>updatingListSelectionModel</code> is false, this will
         * reset the selected paths from the selected rows in the list
         * selection model.
         */
        protected void updateSelectedPathsFromSelectedRows() {
            if (!updatingListSelectionModel) {
                updatingListSelectionModel = true;

                try {
                    // This is way expensive, ListSelectionModel needs an
                    // enumerator for iterating.
                    int min = listSelectionModel.getMinSelectionIndex();
                    int max = listSelectionModel.getMaxSelectionIndex();

                    clearSelection();

                    if ((min != -1) && (max != -1)) {
                        for (int counter = min; counter <= max; counter++) {
                            if (listSelectionModel.isSelectedIndex(counter)) {
                                TreePath selPath = tree.getPathForRow(counter);

                                if (selPath != null) {
                                    addSelectionPath(selPath);
                                }
                            }
                        }
                    }
                } finally {
                    updatingListSelectionModel = false;
                }
            }
        }

        /**
         * Returns the list selection model. ListToTreeSelectionModelWrapper
         * listens for changes to this model and updates the selected paths
         * accordingly.
         */
        ListSelectionModel getListSelectionModel() {
            return listSelectionModel;
        }
    }

    //------------------------------------

    /**
     * This class is used for listening to the table header mouse events.
     */
    private class TableHeaderListener extends MouseAdapter implements MouseMotionListener {
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

                    if (treeTableModel.getInitialSorting(column)) {
                        headerRenderer.setSortingOrder(SORT_ORDER_ASC); // Default sort order for strings is Ascending
                    } else {
                        headerRenderer.setSortingOrder(SORT_ORDER_DESC); // Default sort order for numbers is Descending
                    }
                }

                tableHeader.repaint();

                treeTableModel.sortByColumn(column, headerRenderer.getSortingOrder());
                updateTreeTable();
            }
        }

        public void mouseDragged(MouseEvent e) {
        }

        public void mouseMoved(MouseEvent e) {
            int focusedColumn = tableHeader.columnAtPoint(e.getPoint());

            if (focusedColumn != lastFocusedColumn) {
                if (focusedColumn != -1) {
                    tableHeader.setToolTipText(treeTableModel.getColumnToolTipText(focusedColumn));
                } else {
                    tableHeader.setToolTipText(null);
                }

                lastFocusedColumn = focusedColumn;
            }
        }

        /*
         * Here the active header button is programmatically pressed
         */
        public void mousePressed(MouseEvent e) {
            if ((e.getModifiers() == InputEvent.BUTTON1_MASK) && (tableHeader.getResizingColumn() == null)) {
                headerRenderer.setPressedColumn(tableHeader.columnAtPoint(e.getPoint()));
                tableHeader.repaint();
            }
        }

        /*
         * Here the active header button is programmatically released
         */
        public void mouseReleased(MouseEvent e) {
            if (e.getModifiers() == InputEvent.BUTTON1_MASK) {
                headerRenderer.setPressedColumn(-1);
                tableHeader.repaint();
            }
        }
    }

    private class TreeTableCellRenderer extends JTree implements TableCellRenderer {

        //~ Instance fields ------------------------------------------------------------------------------------------------------

        protected int currentlyPaintedRow;
        private Color darkerUnselectedBackground;
        private Color unselectedBackground;
        private Color unselectedForeground;
        private EnhancedTreeCellRenderer treeCellRenderer;
        private int offsetX; // x-offset used for scrolling the TreeTable cell

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public TreeTableCellRenderer(TreeModel model) {
            super(model);

            offsetX = 0;
            setOpaque(false);
            treeCellRenderer = new EnhancedTreeCellRenderer();
            setCellRenderer(treeCellRenderer);
            unselectedBackground = UIUtils.getProfilerResultsBackground();
            darkerUnselectedBackground = UIUtils.getDarker(unselectedBackground);
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        // Overridden for performance reasons.
        public void validate() {}

        // Overridden for performance reasons.
        public void revalidate() {}

        // Overridden for performance reasons.
        public Insets getInsets() { return ZERO_INSETS; }

        public void setBounds(int x, int y, int w, int h) {
            super.setBounds(x, 0, w, JTreeTable.this.getHeight());
        }

        public void setOffsetX(int offsetX) {
            this.offsetX = offsetX;
        }

        public int getOffsetX() {
            return offsetX;
        }

        public void setRowHeight(int rowHeight) {
            if (rowHeight > 0) {
                super.setRowHeight(rowHeight);

                if ((JTreeTable.this != null) && (JTreeTable.this.getRowHeight() != rowHeight)) {
                    JTreeTable.this.setRowHeight(getRowHeight());
                }
            }
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row,
                                                       int column) {
            if (isSelected) {
                setRowForeground(table.isFocusOwner() ? table.getSelectionForeground() : UIUtils.getUnfocusedSelectionForeground());
                setRowBackground(table.isFocusOwner() ? table.getSelectionBackground() : UIUtils.getUnfocusedSelectionBackground());
            } else {
                if ((row & 0x1) == 0) { //even row
                    setRowForeground((unselectedForeground != null) ? unselectedForeground : table.getForeground());
                    setRowBackground((darkerUnselectedBackground != null) ? darkerUnselectedBackground
                                                                          : UIUtils.getDarker(table.getBackground()));
                } else {
                    setRowForeground((unselectedForeground != null) ? unselectedForeground : table.getForeground());
                    setRowBackground((unselectedBackground != null) ? unselectedBackground : table.getBackground());
                }
            }

            currentlyPaintedRow = row;

            return this;
        }

        public void setTreeCellRenderer(EnhancedTreeCellRenderer renderer) {
            treeCellRenderer = renderer;
            setCellRenderer(treeCellRenderer);
        }

        public EnhancedTreeCellRenderer getTreeCellRenderer() {
            return treeCellRenderer;
        }

        public void customProcessKeyEvent(KeyEvent e) {
            processKeyEvent(e);
        }

        public void paint(Graphics g) {
            boolean selected;
            boolean focused;
            int xpos;

            selected = isRowSelected(currentlyPaintedRow);
            focused = JTreeTable.this.isFocusOwner();

            int rHeight = getRowHeight();

            // move tree according to offsetX
            g.translate(-offsetX, -currentlyPaintedRow * rHeight);

            if (isGTK) { // Optimized for GTK but doesn't paint selection on the left side of renderer
                // paint tree row, according to current Clip only one row is painted
                super.paint(g);

                // draw row background
                Rectangle rowBounds = getRowBounds(currentlyPaintedRow);
                xpos = rowBounds.x + rowBounds.width;
                g.setColor(getRowColor(currentlyPaintedRow, selected, focused));
                g.fillRect(xpos, currentlyPaintedRow * rHeight, getWidth() + offsetX - xpos, rHeight);
            } else {
                // draw row background
                xpos = selected ? 0 : getRowBounds(currentlyPaintedRow).x;
                g.setColor(getRowColor(currentlyPaintedRow, selected, focused));
                    g.fillRect(xpos, currentlyPaintedRow * rHeight, getWidth() + offsetX, rHeight);

                // paint tree row, according to current Clip only one row is painted
                super.paint(g);
            }
            
        }

        protected void setRowBackground(Color c) {
            //setBackground(c);
            treeCellRenderer.setBackground(c);
            treeCellRenderer.setBackgroundNonSelectionColor(c);
            treeCellRenderer.setBackgroundSelectionColor(c);
        }

        protected void setRowForeground(Color c) {
            //setForeground(c);
            treeCellRenderer.setForeground(c);
            treeCellRenderer.setTextNonSelectionColor(c);
            treeCellRenderer.setTextSelectionColor(c);
        }

        private Color getRowColor(int row, boolean selected, boolean focused) {
            if (selected) {
                return focused ? JTreeTable.this.getSelectionBackground() : UIUtils.getUnfocusedSelectionBackground();
            } else {
                Color backgroundColor = UIUtils.getProfilerResultsBackground();
                if ((row & 0x1) == 0) { //even row
                    return UIUtils.getDarker(backgroundColor);
                } else {
                    return backgroundColor;
                }
            }
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final Insets ZERO_INSETS = new Insets(0, 0, 0, 0);

    public static final boolean SORT_ORDER_DESC = false;
    public static final boolean SORT_ORDER_ASC = true;

    private static final boolean isGTK = UIUtils.isGTKLookAndFeel();

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    // --- CellTip support declarations -------
    protected JToolTip cellTip;
    protected Rectangle rendererRect;

    /**
     * A subclass of JTree.
     */
    TreeTableCellRenderer tree;
    protected int lastColumn = -1;
    protected int lastRow = -1;
    private AbstractTreeTableModel treeTableModel;
    private CustomSortableHeaderRenderer headerRenderer;
    private ImageIcon sortAscIcon = Icons.getImageIcon(GeneralIcons.SORT_ASCENDING);
    private ImageIcon sortDescIcon = Icons.getImageIcon(GeneralIcons.SORT_DESCENDING);
    private JTableHeader tableHeader;
    private String internalFindString;

    //------------------------------------
    // Find functionality stuff
    private String userFindString;
    private TableHeaderListener headerListener;
    private TreeTableModelAdapter treeTableModelAdapter;
    private int lastFocusedColumn = -1;
    private int treeSignExtent; // width/2 of the tree "+"/"-" sign
    private int treeSignRightMargin; // value of BasicTreeUI.getRightChildIndent()
    private int userFindColumn;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public JTreeTable(AbstractTreeTableModel treeTableModel) {
        super();
        this.treeTableModel = treeTableModel;
        
        setBackground(UIUtils.getProfilerResultsBackground());

        int initialSortingColumn = treeTableModel.getInitialSortingColumn();
        boolean initialSortingOrder = treeTableModel.getInitialSortingOrder();

        if (treeTableModel.supportsSorting()) {
            treeTableModel.sortByColumn(initialSortingColumn, initialSortingOrder);
        }

        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);

        // Required for correct updating of focused/unfocused selection
        addFocusListener(new FocusListener() {
                public void focusGained(FocusEvent e) {
                    if (getSelectedRows().length > 0) {
                        repaint();
                    }
                }

                public void focusLost(FocusEvent e) {
                    if (getSelectedRows().length > 0) {
                        repaint();
                    }
                }
            });

        // Create the tree. It will be used as a renderer and editor.
        tree = new TreeTableCellRenderer(treeTableModel);
        setTreeUIVariables();
        tree.setTransferHandler(new TransferHandler() {
            public void exportToClipboard(JComponent comp, Clipboard clip, int action)
                                                  throws IllegalStateException {
                JTreeTable.this.getTransferHandler().exportToClipboard(
                        JTreeTable.this, clip, action);
            }
        });

        // Install a tableModel representing the visible rows in the tree.
        treeTableModelAdapter = new TreeTableModelAdapter(treeTableModel, this);
        setModel(treeTableModelAdapter);

        if (treeTableModel.supportsSorting()) {
            headerListener = new TableHeaderListener();

            headerRenderer = new CustomSortableHeaderRenderer(sortAscIcon, sortDescIcon);
            headerRenderer.setSortingColumn(initialSortingColumn);
            headerRenderer.setSortingOrder(initialSortingOrder);

            updateTreeTableHeader();
        }

        getTableHeader().setReorderingAllowed(false);

        // Force the JTable and JTree to share their row selection models.
        ListToTreeSelectionModelWrapper selectionWrapper = new ListToTreeSelectionModelWrapper();
        tree.setSelectionModel(selectionWrapper);
        setSelectionModel(selectionWrapper.getListSelectionModel());

        // Install the tree editor renderer and editor.
        setDefaultRenderer(TreeTableModel.class, tree);

        // --- CellTip support ------------------
        cellTip = createCellTip();
        cellTip.setBorder(BorderFactory.createLineBorder(getGridColor()));
        cellTip.setLayout(new BorderLayout());

        CellTipManager.sharedInstance().registerComponent(this);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public JToolTip getCellTip() {
        return cellTip;
    }

    public Point getCellTipLocation() {
        if (rendererRect == null) {
            return null;
        }

        return new Point(rendererRect.getLocation().x - 1, rendererRect.getLocation().y - 1);
    }

    public int getFindColumn() {
        return userFindColumn;
    }

    public boolean isFindColumnValid() {
        return ((userFindColumn >= 0) && (userFindColumn < getColumnCount()));
    }

    public void setFindParameters(String findString, int findColumn) {
        userFindString = findString;
        userFindColumn = findColumn;
        internalFindString = getInternalFindString(userFindString);
    }

    public String getFindString() {
        return userFindString;
    }

    public boolean isFindStringDefined() {
        return ((userFindString != null) && (userFindString.trim().length() > 0));
    }

    //------------------------------------
    // CellTip support
    public void setGridColor(Color gridColor) {
        super.setGridColor(gridColor);

        if ((gridColor == null) || (cellTip == null)) {
            return;
        }

        cellTip.setBorder(BorderFactory.createLineBorder(gridColor));
    }

    /**
     * Overridden to pass the new rowHeight to the tree.
     */
    public void setRowHeight(int rowHeight) {
        super.setRowHeight(rowHeight);

        if ((tree != null) && (tree.getRowHeight() != rowHeight)) {
            tree.setRowHeight(getRowHeight());
        }
    }

    public void setSortingColumn(int column) {
        headerRenderer.setSortingColumn(column);
    }

    public int getSortingColumn() {
        return headerRenderer.getSortingColumn();
    }

    public void setSortingOrder(boolean order) {
        headerRenderer.setSortingOrder(order);
    }

    public boolean getSortingOrder() {
        return headerRenderer.getSortingOrder();
    }

    /**
     * Returns the tree that is being shared between the model.
     */
    public JTree getTree() {
        return tree;
    }

    /** Sets the x-offset used for scrolling the TreeTable cell */
    public void setTreeCellOffsetX(int offsetX) {
        if (getTreeCellOffsetX() != offsetX) {
            tree.setOffsetX(offsetX);
            repaint(0, 0, getColumnModel().getColumn(0).getWidth(), getHeight());
        }
    }

    /** Gets the x-offset used for scrolling the TreeTable cell */
    public int getTreeCellOffsetX() {
        return tree.getOffsetX();
    }

    public void setTreeCellRenderer(EnhancedTreeCellRenderer renderer) {
        tree.setTreeCellRenderer(renderer);
    }

    public EnhancedTreeCellRenderer getTreeCellRenderer() {
        return tree.getTreeCellRenderer();
    }

    public boolean canFindBePerformed() {
        return (tree != null) && (treeTableModel.getRoot() != null) && isFindColumnValid() && isFindStringDefined();
    }

    public boolean findFirst() {
        return findFirst(true);
    }

    public boolean findNext() {
        if (!canFindBePerformed()) {
            return false;
        }

        CCTNode searchRoot = getSearchRoot();

        // check current search root's subtree
        if (doFindNext(searchRoot, 0, true)) {
            return true;
        }

        CCTNode searchRootParent = searchRoot.getParent();

        // nothing found, process next siblings
        while (searchRootParent != null) {
            if (doFindNext(searchRootParent, searchRootParent.getIndexOfChild(searchRoot) + 1, true)) {
                return true;
            }

            searchRoot = searchRootParent;
            searchRootParent = searchRoot.getParent();
        }

        return false;
    }

    public boolean findPrevious() {
        if (!canFindBePerformed()) {
            return false;
        }

        // selected/last found node
        CCTNode searchRoot = getSearchRoot();

        if (!isAnyRowSelected()) {
            return findFirst();
        }

        // parent of this node than could contain previous node
        CCTNode searchRootParent = searchRoot.getParent();

        while (searchRootParent != null) {
            // if nothing found in previous siblings
            if (doFindPrevious(searchRootParent, searchRootParent.getIndexOfChild(searchRoot) - 1, true)) {
                return true;
            }

            // swith one level up
            searchRoot = searchRootParent;
            searchRootParent = searchRoot.getParent();
        }

        return false;
    }

    //------------------------------------
    // Keyboard processing
    public void keyPressed(KeyEvent e) {
        if (shouldBeForwarded(e)) {
            dispatchKeyboardEvent(e);
        }
    }

    public void keyReleased(KeyEvent e) {
        if (shouldBeForwarded(e)) {
            dispatchKeyboardEvent(e);
        }
    }

    public void keyTyped(KeyEvent e) {
        if (shouldBeForwarded(e)) {
            dispatchKeyboardEvent(e);
        }
    }

    //------------------------------------
    // Mouse processing
    public void mouseClicked(MouseEvent e) {
        dispatchMouseEvent(e);
    }

    public void mouseDragged(MouseEvent e) {
        dispatchMouseEvent(e);
    }

    public void mouseEntered(MouseEvent e) {
        //dispatchMouseEvent(e);

        // --- CellTip support ------------------
        CellTipManager.sharedInstance().setEnabled(false);
    }

    public void mouseExited(MouseEvent e) {
        //dispatchMouseEvent(e);

        // --- CellTip support ------------------
        // Return if mouseExit occurred because of showing heavyweight celltip
        if (contains(e.getPoint()) && cellTip.isShowing()) {
            return;
        }

        CellTipManager.sharedInstance().setEnabled(false);
        lastRow = -1;
        lastColumn = -1;
    }

    public void mouseMoved(MouseEvent e) {
        //dispatchMouseEvent(e);

        // --- CellTip support ------------------

        // Identify treetable row and column at cursor
        int row = rowAtPoint(e.getPoint());
        int column = columnAtPoint(e.getPoint());

        boolean isForTreeCell = (getColumnClass(column) == TreeTableModel.class);

        // Return if treetable cell is the same as in previous event
        if (!isForTreeCell && (row == lastRow) && (column == lastColumn)) {
            return;
        }

        lastRow = row;
        lastColumn = column;

        // Return if cursor isn't at any cell
        if ((row < 0) || (column < 0)) {
            CellTipManager.sharedInstance().setEnabled(false);

            return;
        }

        Component cellRenderer;
        Rectangle cellRect = getCellRect(row, column, false);

        if (isForTreeCell) {
            // Cursor at tree cell
            TreeCellRenderer treeCellRenderer = tree.getTreeCellRenderer();
            cellRenderer = ((TreeCellRendererPersistent) treeCellRenderer).getTreeCellRendererComponentPersistent(tree,
                                                                                                                  treeTableModel
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     .getValueAt(tree.getPathForRow(row)
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     .getLastPathComponent(),
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 0),
                                                                                                                  false,
                                                                                                                  tree.isExpanded(row),
                                                                                                                  treeTableModel
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           .isLeaf(tree.getPathForRow(row)
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       .getLastPathComponent()),
                                                                                                                  row, false);

            // Return if celltip is not supported for the cell
            if (cellRenderer == null) {
                CellTipManager.sharedInstance().setEnabled(false);

                return;
            }

            Point treeCellStart = tree.getPathBounds(tree.getPathForRow(row)).getLocation();
            rendererRect = new Rectangle((cellRect.x + treeCellStart.x) - tree.getOffsetX(), treeCellStart.y,
                                         cellRenderer.getPreferredSize().width, cellRenderer.getPreferredSize().height + 2);
        } else {
            // Cursor at table cell
            TableCellRenderer tableCellRenderer = getCellRenderer(row, column);

            if (!(tableCellRenderer instanceof TableCellRendererPersistent)) {
                return;
            }

            cellRenderer = ((TableCellRendererPersistent) tableCellRenderer).getTableCellRendererComponentPersistent(this,
                                                                                                                     getValueAt(row,
                                                                                                                                column),
                                                                                                                     false,
                                                                                                                     false, row,
                                                                                                                     column);

            // Return if celltip is not supported for the cell
            if (cellRenderer == null) {
                CellTipManager.sharedInstance().setEnabled(false);

                return;
            }

            int horizontalAlignment = ((EnhancedTableCellRenderer) cellRenderer).getHorizontalAlignment();

            if ((horizontalAlignment == SwingConstants.TRAILING) || (horizontalAlignment == SwingConstants.RIGHT)) {
                rendererRect = new Rectangle((cellRect.x + cellRect.width) - cellRenderer.getPreferredSize().width, cellRect.y,
                                             cellRenderer.getPreferredSize().width, cellRenderer.getPreferredSize().height);
            } else {
                rendererRect = new Rectangle(cellRect.x, cellRect.y, cellRenderer.getPreferredSize().width,
                                             cellRenderer.getPreferredSize().height);
            }
        }

        if (isForTreeCell && !rendererRect.contains(e.getPoint())) {
            CellTipManager.sharedInstance().setEnabled(false);

            return;
        }

        // Return if cell contents is fully visible
        if ((rendererRect.x >= cellRect.x) && ((rendererRect.x + rendererRect.width) <= (cellRect.x + cellRect.width))) {
            CellTipManager.sharedInstance().setEnabled(false);

            return;
        }

        while (cellTip.getComponentCount() > 0) {
            cellTip.remove(0);
        }

        cellTip.add(cellRenderer, BorderLayout.CENTER);
        cellTip.setPreferredSize(new Dimension(rendererRect.width + 2, getRowHeight(row) + 2));

        CellTipManager.sharedInstance().setEnabled(true);
    }

    public void mousePressed(MouseEvent e) {
        dispatchMouseEvent(e);
    }

    public void mouseReleased(MouseEvent e) {
        dispatchMouseEvent(e);
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
        mouseMoved(e);
        CellTipManager.sharedInstance().setEnabled(false);
    }

    public void processMouseEvent(MouseEvent e) {
        if (e instanceof MouseWheelEvent) {
            Component target = JTreeTable.this.getParent();
            if (target == null || !(target instanceof JViewport))
                target = JTreeTable.this;
            MouseEvent mwe = SwingUtilities.convertMouseEvent(
                    JTreeTable.this, (MouseWheelEvent)e, target);
            target.dispatchEvent((MouseWheelEvent)mwe);
        } else {
            super.processMouseEvent((MouseEvent)e);
        }
    }

    public void resetTreeCellOffsetX() {
        setTreeCellOffsetX(0);
    }

    public CCTNode[] getPathToRoot(CCTNode node) {
        return treeTableModel.getPathToRoot(node);
    }

    //------------------------------------
    public void selectNode(CCTNode node, boolean setVisible) {
        TreePath path = new TreePath(treeTableModel.getPathToRoot(node));
        getTree().setSelectionPath(path);

        if (setVisible) {
            scrollRectToVisible(getCellRect(getSelectedRow(), 0, true));
        }
    }
    
    public void selectRowByContents(String rowString, int columnIndex, boolean setVisible) {
        for (int i = 0; i < getRowCount(); i++) {
            if (getValueAt(i, columnIndex).toString().equals(rowString)) {
                getSelectionModel().setSelectionInterval(i, i);

                if (setVisible) {
                    scrollRectToVisible(getCellRect(i, columnIndex, true));
                }

                return;
            }
        }

        getSelectionModel().clearSelection();
    }

    public boolean silentlyFindFirst() {
        return findFirst(false);
    }

    public void updateTreeTable() {
        treeTableModelAdapter.updateTreeTable();
    }
    
    public void changeRoot(CCTNode newRoot) {
        treeTableModelAdapter.changeRoot(newRoot);
    }
    
    public void setup(List expanded, TreePath selected) {
        treeTableModelAdapter.setup(expanded, selected);
    }
    
    public List getExpandedPaths() {
        return treeTableModelAdapter.getExpandedPaths();
    }

    public void updateTreeTableHeader() {
        TableColumnModel tableColumnModel = getColumnModel();
        int n = tableColumnModel.getColumnCount();

        for (int i = 0; i < n; i++) {
            tableColumnModel.getColumn(i).setHeaderRenderer(headerRenderer);
        }

        if (tableHeader != getTableHeader()) {
            if (tableHeader != null) {
                tableHeader.removeMouseListener(headerListener);
            }

            if (tableHeader != null) {
                tableHeader.removeMouseMotionListener(headerListener);
            }

            tableHeader = getTableHeader();
            tableHeader.addMouseListener(headerListener);
            tableHeader.addMouseMotionListener(headerListener);
            updateTreeTable();
        }
    }

    /**
     * Overridden to message super and forward the method to the tree. Since
     * the tree is not actually in the component hierarchy it will never receive
     * this unless we forward it in this manner.
     */
    public void updateUI() {
        super.updateUI();

        if (tree != null) {
            tree.updateUI();
            setTreeUIVariables();
        }
    }

    protected JToolTip createCellTip() {
        return new JToolTip();
    }

    private boolean isAnyRowSelected() {
        TreePath treeSelectionPath = tree.getSelectionPath();

        return ((treeSelectionPath != null) && (treeSelectionPath.getPathCount() > 0));
    }

    private String getInternalFindString(String findString) {
        if (findString == null) {
            return null;
        }

        return findString.toLowerCase();
    }

    private CCTNode getSearchRoot() {
        if (!isAnyRowSelected()) {
            return (CCTNode) treeTableModel.getRoot();
        } else {
            return (CCTNode) tree.getSelectionPath().getLastPathComponent();
        }
    }

    private void setTreeUIVariables() {
        if (tree.getUI() instanceof BasicTreeUI) {
            BasicTreeUI treeUI = (BasicTreeUI) tree.getUI();
            treeSignExtent = treeUI.getExpandedIcon().getIconWidth() / 2;
            treeSignRightMargin = treeUI.getRightChildIndent();
        }
    }

    private void dispatchKeyboardEvent(KeyEvent e) {
        JTreeTable.this.tree.customProcessKeyEvent(e);
        
        if (!isModifierKey(e)) {
            int selectedRow = getSelectedRow();
            if (selectedRow > -1)
                scrollRectToVisible(getCellRect(selectedRow, 0, false));
        }
    }
    
    private static boolean isModifierKey(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_ALT:
            case KeyEvent.VK_ALT_GRAPH:
            case KeyEvent.VK_SHIFT:
            case KeyEvent.VK_CONTROL:
                return true;
            default:
                return false;
        }
    }

    private void dispatchMouseEvent(MouseEvent e) {
        if (e != null) {
            int row = rowAtPoint(e.getPoint());
            int column = columnAtPoint(e.getPoint());

            Rectangle tableCellRect = getCellRect(row, column, true);
            Rectangle treeCellRect = tree.getRowBounds(row);

            if (treeCellRect != null) {
                // x-coordinate of the mouseclick must be mapped to the tree coordinate system
                int xClick;
                Class columnClass = getColumnClass(column);

                if (columnClass == TreeTableModel.class) {
                    // Clicked inside tree cell
                    xClick = e.getX() - tableCellRect.x;
                    xClick += tree.getOffsetX();

                    if ((xClick < (treeCellRect.x - treeSignExtent - treeSignRightMargin))
                            || (xClick > ((treeCellRect.x + treeSignExtent) - treeSignRightMargin + 1))) {
                        // Clicked on "+"/"-" sign
                        xClick = (treeCellRect.x + treeCellRect.width) - 1;
                    }
                } else {
                    // Clicked outside tree cell
                    xClick = (treeCellRect.x + treeCellRect.width) - 1;
                }

                int clickCount = 2 - (e.getClickCount() % 2);

                MouseEvent newEvent = new MouseEvent(JTreeTable.this.tree, e.getID(), e.getWhen(), e.getModifiers(), xClick,
                                                     e.getY(), clickCount, e.isPopupTrigger());
                JTreeTable.this.tree.dispatchEvent(newEvent);
            }
        }
    }

    private boolean doFindNext(CCTNode rootForSearch, int childToSearchIndex, boolean requestFocus) {
        int nChildren = rootForSearch.getNChildren();

        // for all not processed children
        while (childToSearchIndex < nChildren) {
            CCTNode childToSearch = rootForSearch.getChild(childToSearchIndex);

            // check the child itself
            if (matchesFindCriterion(childToSearch)) {
                return selectFoundNode(childToSearch, requestFocus);
            }
            // and then its subtree
            else if ((childToSearch.getNChildren() > 0) && doFindNext(childToSearch, 0, requestFocus)) {
                return true;
            }

            childToSearchIndex++;
        }

        // nothing found
        return false;
    }

    private boolean doFindPrevious(CCTNode rootForSearch, int childToSearchIndex, boolean requestFocus) {
        // check all not processed children
        while (childToSearchIndex >= 0) {
            CCTNode childToSearch = rootForSearch.getChild(childToSearchIndex);

            if (doFindPrevious(childToSearch, childToSearch.getNChildren() - 1, requestFocus)) {
                return true;
            }

            childToSearchIndex--;
        }

        // check itself
        if (matchesFindCriterion(rootForSearch)) {
            return selectFoundNode(rootForSearch, requestFocus);
        }

        // nothing found
        return false;
    }

    private boolean findFirst(boolean requestFocus) {
        if (!canFindBePerformed()) {
            return false;
        }

        CCTNode searchRoot = (CCTNode) treeTableModel.getRoot();

        if (matchesFindCriterion(searchRoot)) {
            return selectFoundNode(searchRoot, requestFocus);
        } else {
            return doFindNext(searchRoot, 0, requestFocus);
        }
    }

    private boolean matchesFindCriterion(Object node) {
        // find is always performed on values of the first column
        // first column is always visible and has always index=0
        Object o = treeTableModel.getValueAt(node, 0);
        if (o == null) return false; // #207622, probably caused by updating the table while searching
        String s = o.toString();
        if (s == null) return false; // #207622, likely won't happen but just to be sure
        return s.toLowerCase().indexOf(internalFindString) != -1;
    }

    private boolean selectFoundNode(CCTNode nodeToSelect, boolean requestFocus) {
        TreePath nodeToSelectPath = new TreePath(treeTableModel.getPathToRoot(nodeToSelect));
        tree.expandPath(nodeToSelectPath);
        tree.setSelectionPath(nodeToSelectPath);

        if (requestFocus) {
            requestFocusInWindow();
        }

        Rectangle rect = tree.getPathBounds(nodeToSelectPath);

        if (rect != null) {
            scrollRectToVisible(rect);

            return true;
        } else {
            return false;
        }
    }

    // Filters-out actions handled by the JTable itself,
    // see http://www.netbeans.org/issues/show_bug.cgi?id=112848
    private boolean shouldBeForwarded(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_PAGE_UP:
            case KeyEvent.VK_PAGE_DOWN:
                return false;
            default:
                return true;
        }
    }
}
