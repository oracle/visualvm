/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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

package org.netbeans.modules.profiler.ppoints.ui;

import org.netbeans.api.project.Project;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.ui.UIConstants;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.JExtendedTable;
import org.netbeans.lib.profiler.ui.components.table.EnhancedTableCellRenderer;
import org.netbeans.lib.profiler.ui.components.table.ExtendedTableModel;
import org.netbeans.lib.profiler.ui.components.table.JExtendedTablePanel;
import org.netbeans.lib.profiler.ui.components.table.SortableTableModel;
import org.netbeans.modules.profiler.NetBeansProfiler;
import org.netbeans.modules.profiler.ProfilerIDESettings;
import org.netbeans.modules.profiler.ppoints.CodeProfilingPoint;
import org.netbeans.modules.profiler.ppoints.ProfilingPoint;
import org.netbeans.modules.profiler.ppoints.ProfilingPointsManager;
import org.netbeans.modules.profiler.ppoints.Utils;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.util.actions.SystemAction;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import org.netbeans.modules.profiler.projectsupport.utilities.ProjectUtilities;


/**
 *
 * @author Jiri Sedlacek
 */
public class ProfilingPointsWindowUI extends JPanel implements ActionListener, ListSelectionListener, PropertyChangeListener,
                                                               MouseListener, MouseMotionListener, KeyListener {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String ALL_PROJECTS_STRING = NbBundle.getMessage(ProfilingPointsWindowUI.class,
                                                                          "ProfilingPointsWindowUI_AllProjectsString"); // NOI18N
    private static final String PROJECT_LABEL_TEXT = NbBundle.getMessage(ProfilingPointsWindowUI.class,
                                                                         "ProfilingPointsWindowUI_ProjectLabelText"); // NOI18N
    private static final String INCL_SUBPROJ_CHECKBOX_TEXT = NbBundle.getMessage(ProfilingPointsWindowUI.class,
                                                                                 "ProfilingPointsWindowUI_InclSubprojCheckboxText"); // NOI18N
    private static final String ADD_BUTTON_TOOLTIP = NbBundle.getMessage(ProfilingPointsWindowUI.class,
                                                                         "ProfilingPointsWindowUI_AddButtonToolTip"); // NOI18N
    private static final String REMOVE_BUTTON_TOOLTIP = NbBundle.getMessage(ProfilingPointsWindowUI.class,
                                                                            "ProfilingPointsWindowUI_RemoveButtonToolTip"); // NOI18N
    private static final String EDIT_BUTTON_TOOLTIP = NbBundle.getMessage(ProfilingPointsWindowUI.class,
                                                                          "ProfilingPointsWindowUI_EditButtonToolTip"); // NOI18N
    private static final String DISABLE_BUTTON_TOOLTIP = NbBundle.getMessage(ProfilingPointsWindowUI.class,
                                                                             "ProfilingPointsWindowUI_DisableButtonToolTip"); // NOI18N
    private static final String SHOW_SOURCE_ITEM_TEXT = NbBundle.getMessage(ProfilingPointsWindowUI.class,
                                                                            "ProfilingPointsWindowUI_ShowSourceItemText"); // NOI18N
    private static final String SHOW_START_ITEM_TEXT = NbBundle.getMessage(ProfilingPointsWindowUI.class,
                                                                           "ProfilingPointsWindowUI_ShowStartItemText"); // NOI18N
    private static final String SHOW_END_ITEM_TEXT = NbBundle.getMessage(ProfilingPointsWindowUI.class,
                                                                         "ProfilingPointsWindowUI_ShowEndItemText"); // NOI18N
    private static final String SHOW_REPORT_ITEM_TEXT = NbBundle.getMessage(ProfilingPointsWindowUI.class,
                                                                            "ProfilingPointsWindowUI_ShowReportItemText"); // NOI18N
    private static final String ENABLE_ITEM_TEXT = NbBundle.getMessage(ProfilingPointsWindowUI.class,
                                                                       "ProfilingPointsWindowUI_EnableItemText"); // NOI18N
    private static final String DISABLE_ITEM_TEXT = NbBundle.getMessage(ProfilingPointsWindowUI.class,
                                                                        "ProfilingPointsWindowUI_DisableItemText"); // NOI18N
    private static final String ENABLE_DISABLE_ITEM_TEXT = NbBundle.getMessage(ProfilingPointsWindowUI.class,
                                                                               "ProfilingPointsWindowUI_EnableDisableItemText"); // NOI18N
    private static final String EDIT_ITEM_TEXT = NbBundle.getMessage(ProfilingPointsWindowUI.class,
                                                                     "ProfilingPointsWindowUI_EditItemText"); // NOI18N
    private static final String REMOVE_ITEM_TEXT = NbBundle.getMessage(ProfilingPointsWindowUI.class,
                                                                       "ProfilingPointsWindowUI_RemoveItemText"); // NOI18N
    private static final String SCOPE_COLUMN_NAME = NbBundle.getMessage(ProfilingPointsWindowUI.class,
                                                                        "ProfilingPointsWindowUI_ScopeColumnName"); // NOI18N
    private static final String PROJECT_COLUMN_NAME = NbBundle.getMessage(ProfilingPointsWindowUI.class,
                                                                          "ProfilingPointsWindowUI_ProjectColumnName"); // NOI18N
    private static final String PP_COLUMN_NAME = NbBundle.getMessage(ProfilingPointsWindowUI.class,
                                                                     "ProfilingPointsWindowUI_PpColumnName"); // NOI18N
    private static final String RESULTS_COLUMN_NAME = NbBundle.getMessage(ProfilingPointsWindowUI.class,
                                                                          "ProfilingPointsWindowUI_ResultsColumnName"); // NOI18N
    private static final String SCOPE_COLUMN_TOOLTIP = NbBundle.getMessage(ProfilingPointsWindowUI.class,
                                                                           "ProfilingPointsWindowUI_ScopeColumnToolTip"); // NOI18N
    private static final String PROJECT_COLUMN_TOOLTIP = NbBundle.getMessage(ProfilingPointsWindowUI.class,
                                                                             "ProfilingPointsWindowUI_ProjectColumnToolTip"); // NOI18N
    private static final String PP_COLUMN_TOOLTIP = NbBundle.getMessage(ProfilingPointsWindowUI.class,
                                                                        "ProfilingPointsWindowUI_PpColumnToolTip"); // NOI18N
    private static final String RESULTS_COLUMN_TOOLTIP = NbBundle.getMessage(ProfilingPointsWindowUI.class,
                                                                             "ProfilingPointsWindowUI_ResultsColumnToolTip"); // NOI18N
    private static final String NO_START_DEFINED_MSG = NbBundle.getMessage(ProfilingPointsWindowUI.class,
                                                                           "ProfilingPointsWindowUI_NoStartDefinedMsg"); // NOI18N
    private static final String NO_END_DEFINED_MSG = NbBundle.getMessage(ProfilingPointsWindowUI.class,
                                                                         "ProfilingPointsWindowUI_NoEndDefinedMsg"); // NOI18N
                                                                                                                     // -----
    private static final ImageIcon PPOINT_ADD_ICON = ImageUtilities.loadImageIcon("org/netbeans/modules/profiler/ppoints/ui/resources/ppointAdd.png", false); // NOI18N
    private static final ImageIcon PPOINT_REMOVE_ICON = ImageUtilities.loadImageIcon("org/netbeans/modules/profiler/ppoints/ui/resources/ppointRemove.png", false); // NOI18N
    private static final ImageIcon PPOINT_EDIT_ICON = ImageUtilities.loadImageIcon("org/netbeans/modules/profiler/ppoints/ui/resources/ppointEdit.png", false); // NOI18N
    private static final ImageIcon PPOINT_ENABLE_DISABLE_ICON = ImageUtilities.loadImageIcon("org/netbeans/modules/profiler/ppoints/ui/resources/ppointEnableDisable.png", false); // NOI18N

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected String[] columnNames;
    protected TableCellRenderer[] columnRenderers;
    protected String[] columnToolTips;
    protected Class[] columnTypes;
    protected int[] columnWidths;
    protected boolean sortOrder; // Defines the sorting order (ascending or descending)
    protected int sortBy; // Defines sorting criteria (concrete values provided in subclasses)
    private ExtendedTableModel profilingPointsTableModel;
    private JButton addButton;
    private JButton disableButton;
    private JButton editButton;
    private JButton removeButton;
    private JCheckBox dependenciesCheckbox;
    private JComboBox projectsCombo;
    private JExtendedTable profilingPointsTable;
    private JLabel projectLabel;
    private JMenuItem disableItem;
    private JMenuItem editItem;
    private JMenuItem enableDisableItem;
    private JMenuItem enableItem;
    private JMenuItem removeItem;
    private JMenuItem showEndInSourceItem;
    private JMenuItem showInSourceItem;
    private JMenuItem showReportItem;
    private JMenuItem showStartInSourceItem;
    private JPopupMenu profilingPointsPopup;
    private JToolBar toolbar;
    private ProfilingPoint[] profilingPoints = new ProfilingPoint[0];
    private boolean profilingInProgress = false;
    private int initialSortingColumn;
    private int minProfilingPointColumnWidth; // minimal width of Profiling Point column

    private boolean internalComboChange;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public ProfilingPointsWindowUI() {
        setDefaultSorting();
        initColumnsData();
        initComponents();
        updateProjectsCombo();
        updateButtons();
        ProfilingPointsManager.getDefault().addPropertyChangeListener(this);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    // NOTE: this method only sets sortBy and sortOrder, it doesn't refresh UI!
    public void setDefaultSorting() {
        setSorting(1, SortableTableModel.SORT_ORDER_ASC);
    }

    public Project getSelectedProject() {
        return (projectsCombo.getSelectedItem() instanceof Project) ? (Project) projectsCombo.getSelectedItem() : null;
    }

    // NOTE: this method only sets sortBy and sortOrder, it doesn't refresh UI!
    public void setSorting(int sColumn, boolean sOrder) {
        if (sColumn == CommonConstants.SORTING_COLUMN_DEFAULT) {
            setDefaultSorting();
        } else {
            initialSortingColumn = sColumn;
            sortBy = getSortBy(initialSortingColumn);
            sortOrder = sOrder;
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == projectsCombo) {
            if (!internalComboChange) refreshProfilingPoints();
        } else if (e.getSource() == addButton) {
            SystemAction.get(InsertProfilingPointAction.class).performAction(getSelectedProject());
        } else if (e.getSource() == removeButton) {
            int[] selectedRows = profilingPointsTable.getSelectedRows();
            ProfilingPoint[] selectedProfilingPoints = new ProfilingPoint[selectedRows.length];

            for (int i = 0; i < selectedRows.length; i++) {
                selectedProfilingPoints[i] = getProfilingPointAt(selectedRows[i]);
            }

            ProfilingPointsManager.getDefault().removeProfilingPoints(selectedProfilingPoints);
        } else if (e.getSource() == editButton) {
            ProfilingPoint selectedProfilingPoint = getProfilingPointAt(profilingPointsTable.getSelectedRow());
            selectedProfilingPoint.customize();
        } else if (e.getSource() == disableButton) {
            int[] selectedRows = profilingPointsTable.getSelectedRows();

            for (int i : selectedRows) {
                ProfilingPoint selectedProfilingPoint = getProfilingPointAt(i);
                selectedProfilingPoint.setEnabled(!selectedProfilingPoint.isEnabled());
                repaint();
            }
        } else if (e.getSource() == showInSourceItem) {
            CodeProfilingPoint selectedProfilingPoint = (CodeProfilingPoint) getProfilingPointAt(profilingPointsTable
                                                                                                                                                                                                                                                                       .getSelectedRow());
            Utils.openLocation(selectedProfilingPoint.getLocation());
        } else if (e.getSource() == showStartInSourceItem) {
            CodeProfilingPoint.Paired selectedProfilingPoint = (CodeProfilingPoint.Paired) getProfilingPointAt(profilingPointsTable
                                                                                                               .getSelectedRow());
            CodeProfilingPoint.Location location = selectedProfilingPoint.getStartLocation();

            if (location == null) {
                NetBeansProfiler.getDefaultNB().displayWarning(NO_START_DEFINED_MSG);
            } else {
                Utils.openLocation(location);
            }
        } else if (e.getSource() == showEndInSourceItem) {
            CodeProfilingPoint.Paired selectedProfilingPoint = (CodeProfilingPoint.Paired) getProfilingPointAt(profilingPointsTable
                                                                                                               .getSelectedRow());
            CodeProfilingPoint.Location location = selectedProfilingPoint.getEndLocation();

            if (location == null) {
                NetBeansProfiler.getDefaultNB().displayWarning(NO_END_DEFINED_MSG);
            } else {
                Utils.openLocation(location);
            }
        } else if (e.getSource() == showReportItem) {
            int[] selectedRows = profilingPointsTable.getSelectedRows();

            if (selectedRows.length == 0) {
                return;
            }

            for (int selectedRow : selectedRows) {
                ProfilingPoint selectedProfilingPoint = getProfilingPointAt(selectedRow);
                selectedProfilingPoint.showResults(null);
            }
        } else if (e.getSource() == enableItem) {
            int selectedRow = profilingPointsTable.getSelectedRow();

            if (selectedRow == -1) {
                return;
            }

            ProfilingPoint selectedProfilingPoint = getProfilingPointAt(selectedRow);
            selectedProfilingPoint.setEnabled(true);
        } else if (e.getSource() == disableItem) {
            int selectedRow = profilingPointsTable.getSelectedRow();

            if (selectedRow == -1) {
                return;
            }

            ProfilingPoint selectedProfilingPoint = getProfilingPointAt(selectedRow);
            selectedProfilingPoint.setEnabled(false);
        } else if (e.getSource() == enableDisableItem) {
            int[] selectedRows = profilingPointsTable.getSelectedRows();

            if (selectedRows.length == 0) {
                return;
            }

            for (int selectedRow : selectedRows) {
                ProfilingPoint selectedProfilingPoint = getProfilingPointAt(selectedRow);
                selectedProfilingPoint.setEnabled(!selectedProfilingPoint.isEnabled());
            }
        } else if (e.getSource() == editItem) {
            int selectedRow = profilingPointsTable.getSelectedRow();

            if (selectedRow == -1) {
                return;
            }

            ProfilingPoint selectedProfilingPoint = getProfilingPointAt(selectedRow);
            selectedProfilingPoint.customize();
        } else if (e.getSource() == removeItem) {
            deletePPs();
        }
    }

    public void keyPressed(KeyEvent e) {
        if ((e.getKeyCode() == KeyEvent.VK_CONTEXT_MENU)
                || ((e.getKeyCode() == KeyEvent.VK_F10) && (e.getModifiers() == InputEvent.SHIFT_MASK))) {
            int[] selectedRows = profilingPointsTable.getSelectedRows();

            if (selectedRows.length != 0) {
                Rectangle rowBounds = profilingPointsTable.getCellRect(selectedRows[0], 1, true);
                showProfilingPointsPopup(e.getComponent(), rowBounds.x + 20,
                                         rowBounds.y + (profilingPointsTable.getRowHeight() / 2));
            }
        } else if (e.getKeyCode() == KeyEvent.VK_DELETE) {
            deletePPs();
        }
    }

    public void keyReleased(KeyEvent e) {
    }

    public void keyTyped(KeyEvent e) {
    }

    public void mouseClicked(MouseEvent e) {
        if (e.getModifiers() == InputEvent.BUTTON1_MASK) {
            int clickedRow = profilingPointsTable.rowAtPoint(e.getPoint());

            if ((clickedRow != -1) && (e.getClickCount() == 2)) {
                ProfilingPoint profilingPoint = getProfilingPointAt(clickedRow);

                if (profilingPoint instanceof CodeProfilingPoint) {
                    Utils.openLocation(((CodeProfilingPoint) profilingPoint).getLocation());
                }
            }
        } else if (e.getModifiers() == InputEvent.BUTTON3_MASK) {
            int clickedRow = profilingPointsTable.rowAtPoint(e.getPoint());

            if ((clickedRow != -1) && (profilingPointsTable.getSelectedRowCount() != 0)) {
                showProfilingPointsPopup(e.getComponent(), e.getX(), e.getY());

                return;
            }
        }

        dispatchResultsRendererEvent(e);
    }

    public void mouseDragged(MouseEvent e) {
        dispatchResultsRendererEvent(e);
    }

    public void mouseEntered(MouseEvent e) {
        dispatchResultsRendererEvent(e);
    }

    public void mouseExited(MouseEvent e) {
        dispatchResultsRendererEvent(e);
    }

    public void mouseMoved(MouseEvent e) {
        dispatchResultsRendererEvent(e);
    }

    public void mousePressed(MouseEvent e) {
        if (e.getModifiers() == InputEvent.BUTTON3_MASK) {
            int clickedRow = profilingPointsTable.rowAtPoint(e.getPoint());

            if (clickedRow != -1) {
                int[] selectedRows = profilingPointsTable.getSelectedRows();

                if (selectedRows.length == 0) {
                    profilingPointsTable.setRowSelectionInterval(clickedRow, clickedRow);
                } else {
                    boolean changeSelection = true;

                    for (int selectedRow : selectedRows) {
                        if (selectedRow == clickedRow) {
                            changeSelection = false;
                        }
                    }

                    if (changeSelection) {
                        profilingPointsTable.setRowSelectionInterval(clickedRow, clickedRow);
                    }
                }
            }
        }

        dispatchResultsRendererEvent(e);
    }

    public void mouseReleased(MouseEvent e) {
        dispatchResultsRendererEvent(e);
    }

    public void notifyProfilingStateChanged() {
        profilingInProgress = ProfilingPointsManager.getDefault().isProfilingSessionInProgress();
        updateButtons();
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName() == ProfilingPointsManager.PROPERTY_PROJECTS_CHANGED) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() { updateProjectsCombo(); } // also refreshes profiling points
            });
        } else if (evt.getPropertyName() == ProfilingPointsManager.PROPERTY_PROFILING_POINTS_CHANGED) {
            refreshProfilingPoints();
        }
    }

    public void valueChanged(ListSelectionEvent e) {
        updateButtons();
    }

    protected void initColumnsData() {
        minProfilingPointColumnWidth = getFontMetrics(getFont()).charWidth('W') * 30; // NOI18N

        EnhancedTableCellRenderer scopeRenderer = Utils.getScopeRenderer();
        EnhancedTableCellRenderer projectRenderer = Utils.getProjectRenderer();
        EnhancedTableCellRenderer profilingPointRenderer = Utils.getPresenterRenderer();

        columnNames = new String[] { SCOPE_COLUMN_NAME, PROJECT_COLUMN_NAME, PP_COLUMN_NAME, RESULTS_COLUMN_NAME };
        columnToolTips = new String[] { SCOPE_COLUMN_TOOLTIP, PROJECT_COLUMN_TOOLTIP, PP_COLUMN_TOOLTIP, RESULTS_COLUMN_TOOLTIP };
        columnTypes = new Class[] { Integer.class, Project.class, ProfilingPoint.class, ProfilingPoint.ResultsRenderer.class };
        columnRenderers = new TableCellRenderer[] { scopeRenderer, projectRenderer, profilingPointRenderer, null // dynamic
                          };
        columnWidths = new int[] { 50, 165, -1, // dynamic
            200 };
    }

    private void setColumnsData() {
        TableColumnModel colModel = profilingPointsTable.getColumnModel();
        colModel.getColumn(2).setPreferredWidth(minProfilingPointColumnWidth);

        //    colModel.getColumn(1).setPreferredWidth(minProfilingPointColumnWidth); // TODO: revert use column 2 once Scope is enabled
        int index;

        for (int i = 0; i < colModel.getColumnCount(); i++) {
            index = profilingPointsTableModel.getRealColumn(i);
            colModel.getColumn(i).setPreferredWidth((index == 2) ? minProfilingPointColumnWidth : columnWidths[index]);
            //      colModel.getColumn(i).setPreferredWidth(index == 1 ? minProfilingPointColumnWidth : columnWidths[index]); // TODO: revert use column 2 once Scope is enabled
            colModel.getColumn(i).setCellRenderer(columnRenderers[index]);
        }
    }

    private ProfilingPoint getProfilingPointAt(int row) {
        return (ProfilingPoint) profilingPointsTable.getValueAt(row, 0);
    }

    private int getSortBy(int column) {
        switch (column) {
            case 0:
                return ProfilingPointsManager.SORT_BY_SCOPE;
            case 1:
                return ProfilingPointsManager.SORT_BY_PROJECT;
            case 2:
                return ProfilingPointsManager.SORT_BY_NAME; // TODO: revert use column 2 once Scope is enabled
            case 3:
                return ProfilingPointsManager.SORT_BY_RESULTS; // TODO: revert use column 3 once Scope is enabled
            default:
                return CommonConstants.SORTING_COLUMN_DEFAULT;
        }
    }

    private void createProfilingPointsTable() {
        profilingPointsTableModel = new ExtendedTableModel(new SortableTableModel() {
                public String getColumnName(int col) {
                    return columnNames[col];
                }

                public int getRowCount() {
                    return profilingPoints.length;
                }

                public int getColumnCount() {
                    return columnNames.length;
                }

                public Class getColumnClass(int col) {
                    return columnTypes[col];
                }

                public Object getValueAt(int row, int col) {
                    return profilingPoints[row];
                }

                public String getColumnToolTipText(int col) {
                    return columnToolTips[col];
                }

                public void sortByColumn(int column, boolean order) {
                    sortBy = getSortBy(column);
                    sortOrder = order;
                    refreshProfilingPoints();
                }

                /**
                 * @param column The table column index
                 * @return Initial sorting for the specified column - if true, ascending, if false descending
                 */
                public boolean getInitialSorting(int column) {
                    return true;
                }
            });

        profilingPointsTable = new JExtendedTable(profilingPointsTableModel) {
                public TableCellRenderer getCellRenderer(int row, int column) {
                    if (getColumnClass(column) == ProfilingPoint.ResultsRenderer.class) {
                        return getProfilingPointAt(row).getResultsRenderer();
                    } else {
                        return super.getCellRenderer(row, column);
                    }
                }

                public void doLayout() {
                    int columnsWidthsSum = 0;
                    int realFirstColumn = -1;

                    int index;

                    for (int i = 0; i < profilingPointsTableModel.getColumnCount(); i++) {
                        index = profilingPointsTableModel.getRealColumn(i);

                        if (index == 2) {
                            //          if (index == 1) { // TODO: revert use column 2 once Scope is enabled
                            realFirstColumn = i;
                        } else {
                            columnsWidthsSum += getColumnModel().getColumn(i).getPreferredWidth();
                        }
                    }

                    if (realFirstColumn != -1) {
                        getColumnModel().getColumn(realFirstColumn)
                            .setPreferredWidth(Math.max(getWidth() - columnsWidthsSum, minProfilingPointColumnWidth));
                    }

                    super.doLayout();
                }
                ;
            };
        //    profilingPointsTable.getAccessibleContext().setAccessibleName(TABLE_ACCESS_NAME);
        profilingPointsTableModel.setTable(profilingPointsTable);
        profilingPointsTableModel.setInitialSorting(initialSortingColumn, sortOrder);
        profilingPointsTable.setRowSelectionAllowed(true);
        profilingPointsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        profilingPointsTable.setGridColor(UIConstants.TABLE_VERTICAL_GRID_COLOR);
        profilingPointsTable.setSelectionBackground(UIConstants.TABLE_SELECTION_BACKGROUND_COLOR);
        profilingPointsTable.setSelectionForeground(UIConstants.TABLE_SELECTION_FOREGROUND_COLOR);
        profilingPointsTable.setShowHorizontalLines(UIConstants.SHOW_TABLE_HORIZONTAL_GRID);
        profilingPointsTable.setShowVerticalLines(UIConstants.SHOW_TABLE_VERTICAL_GRID);
        profilingPointsTable.setRowMargin(UIConstants.TABLE_ROW_MARGIN);
        profilingPointsTable.setRowHeight(UIUtils.getDefaultRowHeight() + 2);
        profilingPointsTable.getSelectionModel().addListSelectionListener(this);
        profilingPointsTable.addMouseListener(this);
        profilingPointsTable.addMouseMotionListener(this);
        profilingPointsTable.addKeyListener(this);

        setColumnsData();
    }

    private void deletePPs() {
        int[] selectedRows = profilingPointsTable.getSelectedRows();

        if (selectedRows.length == 0) {
            return;
        }

        List<ProfilingPoint> pointsToRemove = new ArrayList();

        for (int selectedRow : selectedRows) {
            ProfilingPoint selectedProfilingPoint = getProfilingPointAt(selectedRow);
            pointsToRemove.add(selectedProfilingPoint);
        }

        for (ProfilingPoint pointToRemove : pointsToRemove) {
            ProfilingPointsManager.getDefault().removeProfilingPoint(pointToRemove);
        }
    }

    private void dispatchResultsRendererEvent(MouseEvent e) {
        int column = profilingPointsTable.columnAtPoint(e.getPoint());

        if (column != 3) {
            //    if (column != 2) { // TODO: revert to 3 once Scope is enabled
            profilingPointsTable.setCursor(Cursor.getDefaultCursor()); // Workaround for forgotten Hand cursor from HTML renderer, TODO: fix it!

            return;
        }

        int row = profilingPointsTable.rowAtPoint(e.getPoint());

        if (row == -1) {
            return;
        }

        ProfilingPoint profilingPoint = getProfilingPointAt(row);
        ProfilingPoint.ResultsRenderer resultsRenderer = profilingPoint.getResultsRenderer();
        Rectangle cellRect = profilingPointsTable.getCellRect(row, column, true);
        MouseEvent mouseEvent = new MouseEvent(profilingPointsTable, e.getID(), e.getWhen(), e.getModifiers(),
                                               e.getX() - cellRect.x, e.getY() - cellRect.y, e.getClickCount(),
                                               e.isPopupTrigger(), e.getButton());
        resultsRenderer.dispatchMouseEvent(mouseEvent);
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        toolbar = new JToolBar() {
                public Component add(Component comp) {
                    if (comp instanceof JButton) {
                        UIUtils.fixButtonUI((JButton) comp);
                    }

                    return super.add(comp);
                }
            };
        toolbar.setFloatable(false);
        toolbar.putClientProperty("JToolBar.isRollover", Boolean.TRUE); //NOI18N
        toolbar.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));

        projectLabel = new JLabel();
        org.openide.awt.Mnemonics.setLocalizedText(projectLabel, PROJECT_LABEL_TEXT);
        projectLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));

        projectsCombo = new JComboBox(new Object[] { ALL_PROJECTS_STRING }) {
                public Dimension getMaximumSize() {
                    return getPreferredSize();
                }

                public Dimension getPreferredSize() {
                    return new Dimension(200, super.getPreferredSize().height);
                }
                ;
                public Dimension getMinimumSize() {
                    return getPreferredSize();
                }
                ;
            };
        projectLabel.setLabelFor(projectsCombo);
        projectsCombo.addActionListener(this);
        projectsCombo.setRenderer(Utils.getProjectListRenderer());
        toolbar.add(projectLabel);
        toolbar.add(projectsCombo);

        dependenciesCheckbox = new JCheckBox();
        org.openide.awt.Mnemonics.setLocalizedText(dependenciesCheckbox, INCL_SUBPROJ_CHECKBOX_TEXT);
        dependenciesCheckbox.setSelected(ProfilerIDESettings.getInstance().getIncludeProfilingPointsDependencies());
        toolbar.add(dependenciesCheckbox);
        dependenciesCheckbox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    ProfilerIDESettings.getInstance().setIncludeProfilingPointsDependencies(dependenciesCheckbox.isSelected());
                    refreshProfilingPoints();
                }
            });

        toolbar.add(new JToolBar.Separator());

        addButton = new JButton(PPOINT_ADD_ICON);
        addButton.setToolTipText(ADD_BUTTON_TOOLTIP);
        addButton.addActionListener(this);
        toolbar.add(addButton);

        removeButton = new JButton(PPOINT_REMOVE_ICON);
        removeButton.setToolTipText(REMOVE_BUTTON_TOOLTIP);
        removeButton.addActionListener(this);
        toolbar.add(removeButton);

        toolbar.add(new JToolBar.Separator());

        editButton = new JButton(PPOINT_EDIT_ICON);
        editButton.setToolTipText(EDIT_BUTTON_TOOLTIP);
        editButton.addActionListener(this);
        toolbar.add(editButton);

        disableButton = new JButton(PPOINT_ENABLE_DISABLE_ICON);
        disableButton.setToolTipText(DISABLE_BUTTON_TOOLTIP);
        disableButton.addActionListener(this);
        toolbar.add(disableButton);

        createProfilingPointsTable();

        JPanel panel = new JPanel(new BorderLayout());
        JSeparator separator = new JSeparator() {
            public Dimension getMaximumSize() {
                return new Dimension(super.getMaximumSize().width, 1);
            }

            public Dimension getPreferredSize() {
                return new Dimension(super.getPreferredSize().width, 1);
            }
        };

        separator.setBackground(toolbar.getBackground());

        JExtendedTablePanel tablePanel = new JExtendedTablePanel(profilingPointsTable);
        tablePanel.setBorder(BorderFactory.createEmptyBorder());
        panel.add(separator, BorderLayout.NORTH);
        panel.add(tablePanel, BorderLayout.CENTER);

        showInSourceItem = new JMenuItem(SHOW_SOURCE_ITEM_TEXT);
        showInSourceItem.setFont(showInSourceItem.getFont().deriveFont(Font.BOLD));
        showInSourceItem.addActionListener(this);
        showStartInSourceItem = new JMenuItem(SHOW_START_ITEM_TEXT);
        showStartInSourceItem.setFont(showInSourceItem.getFont().deriveFont(Font.BOLD));
        showStartInSourceItem.addActionListener(this);
        showEndInSourceItem = new JMenuItem(SHOW_END_ITEM_TEXT);
        showEndInSourceItem.addActionListener(this);
        showReportItem = new JMenuItem(SHOW_REPORT_ITEM_TEXT);
        showReportItem.addActionListener(this);
        enableItem = new JMenuItem(ENABLE_ITEM_TEXT);
        enableItem.addActionListener(this);
        disableItem = new JMenuItem(DISABLE_ITEM_TEXT);
        disableItem.addActionListener(this);
        enableDisableItem = new JMenuItem(ENABLE_DISABLE_ITEM_TEXT);
        enableDisableItem.addActionListener(this);
        editItem = new JMenuItem(EDIT_ITEM_TEXT);
        editItem.addActionListener(this);
        removeItem = new JMenuItem(REMOVE_ITEM_TEXT);
        removeItem.addActionListener(this);

        profilingPointsPopup = new JPopupMenu();
        profilingPointsPopup.add(showInSourceItem);
        profilingPointsPopup.add(showStartInSourceItem);
        profilingPointsPopup.add(showEndInSourceItem);
        profilingPointsPopup.add(showReportItem);
        profilingPointsPopup.addSeparator();
        profilingPointsPopup.add(editItem);
        profilingPointsPopup.add(enableItem);
        profilingPointsPopup.add(disableItem);
        profilingPointsPopup.add(enableDisableItem);
        profilingPointsPopup.addSeparator();
        profilingPointsPopup.add(removeItem);

        add(toolbar, BorderLayout.NORTH);
        add(panel, BorderLayout.CENTER);
    }

    private void refreshProfilingPoints() {
        int[] selectedRows = profilingPointsTable.getSelectedRows();
        ProfilingPoint[] selectedProfilingPoints = new ProfilingPoint[selectedRows.length];

        for (int i = 0; i < selectedRows.length; i++) {
            selectedProfilingPoints[i] = getProfilingPointAt(selectedRows[i]);
        }

        List<ProfilingPoint> sortedProfilingPoints = ProfilingPointsManager.getDefault()
                                                                           .getSortedProfilingPoints(getSelectedProject(),
                                                                                                     sortBy, sortOrder);
        profilingPoints = sortedProfilingPoints.toArray(new ProfilingPoint[sortedProfilingPoints.size()]);
        profilingPointsTableModel.fireTableDataChanged();

        if (selectedProfilingPoints.length > 0) {
            profilingPointsTable.selectRowsByInstances(selectedProfilingPoints, 0, true);
        }

        repaint();
    }

    private void showProfilingPointsPopup(Component source, int x, int y) {
        int[] selectedRows = profilingPointsTable.getSelectedRows();

        if (selectedRows.length == 0) {
            return;
        }

        boolean singleSelection = selectedRows.length == 1;
        ProfilingPoint selectedProfilingPoint = getProfilingPointAt(selectedRows[0]);

        showInSourceItem.setVisible(!singleSelection || selectedProfilingPoint instanceof CodeProfilingPoint.Single);
        showInSourceItem.setEnabled(singleSelection);

        showStartInSourceItem.setVisible(singleSelection && selectedProfilingPoint instanceof CodeProfilingPoint.Paired);

        showEndInSourceItem.setVisible(singleSelection && selectedProfilingPoint instanceof CodeProfilingPoint.Paired);

        showReportItem.setEnabled(true);

        enableItem.setVisible(singleSelection && !selectedProfilingPoint.isEnabled());
        enableItem.setEnabled(!profilingInProgress);

        disableItem.setVisible(singleSelection && selectedProfilingPoint.isEnabled());
        disableItem.setEnabled(!profilingInProgress);

        enableDisableItem.setVisible(!singleSelection);
        enableDisableItem.setEnabled(!profilingInProgress);

        editItem.setEnabled(singleSelection && !profilingInProgress);

        removeItem.setEnabled(!profilingInProgress);

        profilingPointsPopup.show(source, x, y);
    }

    private void updateButtons() {
        int[] selectedRows = profilingPointsTable.getSelectedRows();
        addButton.setEnabled(!profilingInProgress);

        if (selectedRows.length == 0) {
            editButton.setEnabled(false);
            removeButton.setEnabled(false);
            disableButton.setEnabled(false);
        } else if (selectedRows.length == 1) {
            editButton.setEnabled(!profilingInProgress);
            removeButton.setEnabled(!profilingInProgress);
            disableButton.setEnabled(!profilingInProgress);
        } else {
            editButton.setEnabled(false);
            removeButton.setEnabled(!profilingInProgress);
            disableButton.setEnabled(!profilingInProgress);
        }
    }

    private void updateProjectsCombo() {
        Project[] projects = ProjectUtilities.getSortedProjects(ProjectUtilities.getOpenedProjects());
        Vector items = new Vector(projects.length + 1);

        for (int i = 0; i < projects.length; i++) {
            items.add(projects[i]);
        }

        items.add(0, ALL_PROJECTS_STRING);

        DefaultComboBoxModel comboModel = (DefaultComboBoxModel) projectsCombo.getModel();
        Object selectedItem = projectsCombo.getSelectedItem();

        internalComboChange = true;

        comboModel.removeAllElements();

        for (int i = 0; i < items.size(); i++) {
            comboModel.addElement(items.get(i));
        }

        if ((selectedItem != null) && (comboModel.getIndexOf(selectedItem) != -1)) {
            projectsCombo.setSelectedItem(selectedItem);
        } else {
            projectsCombo.setSelectedItem(ALL_PROJECTS_STRING);
        }

        internalComboChange = false;

        refreshProfilingPoints();
    }
}
