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

package org.netbeans.lib.profiler.ui.threads;

import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.results.DataManagerListener;
import org.netbeans.lib.profiler.results.threads.ThreadData;
import org.netbeans.lib.profiler.results.threads.ThreadsDataManager;
import org.netbeans.lib.profiler.ui.UIConstants;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.CellTipManager;
import org.netbeans.lib.profiler.ui.components.FlatToolBar;
import org.netbeans.lib.profiler.ui.components.JExtendedTable;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import org.netbeans.lib.profiler.ui.components.ProfilerToolbar;
import org.netbeans.modules.profiler.api.icons.GeneralIcons;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;


/**
 * A panel to display TA threads and their state.
 *
 * @author Ian Formanek
 * @author Jiri Sedlacek
 */
public class ThreadsPanel extends JPanel implements AdjustmentListener, ActionListener, TableColumnModelListener,
                                                    DataManagerListener {
    //~ Inner Interfaces ---------------------------------------------------------------------------------------------------------

    /** A callback interface - implemented by provider of additional details of a set of threads */
    public interface ThreadsDetailsCallback {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        /** Displays a panel with details about specified threads
         *
         * @param indexes array of int indexes for threads to display
         */
        public void showDetails(int[] indexes);
    }

    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    class ThreadsScrollBar extends JScrollBar {
        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public ThreadsScrollBar() {
            super(JScrollBar.HORIZONTAL);
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public Dimension getPreferredSize() {
            Dimension pref = super.getPreferredSize();

            return new Dimension(table.getTableHeader().getHeaderRect(DISPLAY_COLUMN_INDEX).width, pref.height);
        }
    }

    // ---------------------------------------------------------------------------------------
    // Model for the table
    private class ThreadsTableModel extends AbstractTableModel {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public Class getColumnClass(int column) {
            // The main purpose of this method is to make numeric values aligned properly inside table cells
            switch (column) {
                case NAME_COLUMN_INDEX:
                    return ThreadNameCellRenderer.class;
                case DISPLAY_COLUMN_INDEX:
                    return ThreadStateCellRenderer.class;
                default:
                    return String.class;
            }
        }

        public int getColumnCount() {
            return 2;
        }

        /**
         * Returns a default name for the column using spreadsheet conventions:
         * A, B, C, ... Z, AA, AB, etc.  If <code>column</code> cannot be found,
         * returns an empty string.
         *
         * @param column the column being queried
         * @return a string containing the default name of <code>column</code>
         */
        public String getColumnName(int column) {
            switch (column) {
                case NAME_COLUMN_INDEX:
                    return THREADS_COLUMN_NAME;
                case DISPLAY_COLUMN_INDEX:
                    return TIMELINE_COLUMN_NAME;
                default:
                    return null;
            }
        }

        public int getRowCount() {
            //return manager.getThreadsCount();
            return filteredDataToDataIndex.size();
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            switch (columnIndex) {
                case NAME_COLUMN_INDEX:
                    return (Integer) (filteredDataToDataIndex.get(rowIndex));
                case DISPLAY_COLUMN_INDEX:
                    return getThreadData(((Integer) (filteredDataToDataIndex.get(rowIndex))).intValue());
                default:
                    return null;
            }
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.netbeans.lib.profiler.ui.threads.Bundle"); // NOI18N
    private static final String VIEW_THREADS_ALL = messages.getString("ThreadsPanel_ViewThreadsAll"); // NOI18N
    private static final String VIEW_THREADS_LIVE = messages.getString("ThreadsPanel_ViewThreadsLive"); // NOI18N
    private static final String VIEW_THREADS_FINISHED = messages.getString("ThreadsPanel_ViewThreadsFinished"); // NOI18N
    private static final String VIEW_THREADS_SELECTION = messages.getString("ThreadsPanel_ViewThreadsSelection"); // NOI18N
    private static final String THREADS_TABLE = messages.getString("ThreadsPanel_ThreadsTable"); // NOI18N
    private static final String ENABLE_THREADS_PROFILING = messages.getString("ThreadsPanel_EnableThreadsProfiling"); // NOI18N
    private static final String ZOOM_IN_TOOLTIP = messages.getString("ThreadsPanel_ZoomInToolTip"); // NOI18N
    private static final String ZOOM_OUT_TOOLTIP = messages.getString("ThreadsPanel_ZoomOutToolTip"); // NOI18N
    private static final String FIXED_SCALE_TOOLTIP = messages.getString("ThreadsPanel_FixedScaleToolTip"); // NOI18N
    private static final String SCALE_TO_FIT_TOOLTIP = messages.getString("ThreadsPanel_ScaleToFitToolTip"); // NOI18N
    private static final String THREADS_MONITORING_DISABLED_MSG = messages.getString("ThreadsPanel_ThreadsMonitoringDisabledMsg"); // NOI18N
    private static final String THREADS_MONITORING_DISABLED_TOOLTIP = messages.getString("ThreadsPanel_ThreadsMonitoringDisabledTooltip"); // NOI18N
    private static final String NO_PROFILING_MSG = messages.getString("ThreadsPanel_NoProfilingMsg"); // NOI18N
    private static final String THREADS_COLUMN_NAME = messages.getString("ThreadsPanel_ThreadsColumnName"); // NOI18N
    private static final String TIMELINE_COLUMN_NAME = messages.getString("ThreadsPanel_TimelineColumnName"); // NOI18N
    private static final String SELECTED_THREADS_ITEM = messages.getString("ThreadsPanel_SelectedThreadsItem"); // NOI18N
    private static final String THREAD_DETAILS_ITEM = messages.getString("ThreadsPanel_ThreadDetailsItem"); // NOI18N
    private static final String TABLE_ACCESS_NAME = messages.getString("ThreadsPanel_TableAccessName"); // NOI18N
    private static final String TABLE_ACCESS_DESCR = messages.getString("ThreadsPanel_TableAccessDescr"); // NOI18N
    private static final String COMBO_ACCESS_NAME = messages.getString("ThreadsPanel_ComboAccessName"); // NOI18N
    private static final String COMBO_ACCESS_DESCR = messages.getString("ThreadsPanel_ComboAccessDescr"); // NOI18N
    private static final String ENABLE_THREADS_MONITORING_BUTTON_ACCESS_NAME = messages.getString("ThreadsPanel_EnableThreadsMonitoringAccessName"); // NOI18N
    private static final String SHOW_LABEL_TEXT = messages.getString("ThreadsPanel_ShowLabelText"); // NOI18N
                                                                                                    // -----
    private static final int NAME_COLUMN_INDEX = 0;
    private static final int DISPLAY_COLUMN_INDEX = 1;
    private static final int RIGHT_DISPLAY_MARGIN = 20; // extra space [pixels] on the right end of the threads display
    private static final int LEFT_DISPLAY_MARGIN = 20;
    private static final int NAME_COLUMN_WIDTH = 190;
    private static final int MIN_NAME_COLUMN_WIDTH = 55;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private ArrayList filteredDataToDataIndex = new ArrayList();
    private CustomTimeLineViewport viewPort;
    private DefaultComboBoxModel comboModel;
    private DefaultComboBoxModel comboModelWithSelection;
    private JButton enableThreadsMonitoringButton;
    private JButton scaleToFitButton;
    private JButton zoomInButton;
    private JButton zoomOutButton;
    private JComboBox threadsSelectionCombo;
    private JLabel enableThreadsMonitoringLabel1;
    private JLabel enableThreadsMonitoringLabel3;
    private JLabel monitorLegend;
    private JLabel runningLegend;
    private JLabel sleepingLegend;
    private JLabel waitLegend;
    private JLabel parkLegend;
    private JMenuItem showOnlySelectedThreads;
    private JMenuItem showThreadsDetails;
    private JPanel contentPanel; // panel with CardLayout containing threadsTable & enable threads profiling notification and button
    private JPanel notificationPanel;
    private JPopupMenu popupMenu;
    private JScrollBar scrollBar; // scrollbar that is displayed in zoomed mode that allows to scroll in history
    private JScrollPane tableScroll;
    private JTable table; // table that displays individual threads
    private ProfilerToolbar buttonsToolBar;
    private ThreadsDataManager manager;
    private ThreadsDetailsCallback detailsCallback;
    private boolean internalChange = false; // prevents cycles in event handling
    private boolean internalScrollbarChange = false;
    private boolean resetPerformed = true;
    private boolean scaleToFit = false;
    private boolean supportsSleepingState; // internal flag indicating that threads monitoring engine correctly reports the "sleeping" state
    private boolean threadsMonitoringEnabled = false;
    private boolean trackingEnd = true;
    private float zoomResolutionPerPixel = 50f;
    private long viewEnd;
    private long viewStart = -1;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Creates a new threads panel that displays threads timeline from data provided
     * by specified ThreadsDataManager.
     * @param manager The provider of threads data
     * @param detailsCallback A handler of displaying additional threads details or null, in which case the
     *                        popup menu action to display details will not be present
     */
    public ThreadsPanel(ThreadsDataManager manager, ThreadsDetailsCallback detailsCallback, boolean supportsSleepingState) {
        this.manager = manager;
        this.detailsCallback = detailsCallback;
        this.supportsSleepingState = supportsSleepingState;

        // create components

        // contentPanel for threadsTable and enable threads profiling notification
        contentPanel = new JPanel(new CardLayout());

        // threads table components
        table = createViewTable();
        table.setGridColor(UIConstants.TABLE_VERTICAL_GRID_COLOR);
        table.getAccessibleContext().setAccessibleName(TABLE_ACCESS_NAME);
        table.getAccessibleContext().setAccessibleDescription(TABLE_ACCESS_DESCR);
        table.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
             .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "DEFAULT_ACTION"); // NOI18N
        table.getActionMap().put("DEFAULT_ACTION",
                                 new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    performDefaultAction();
                }
            }); // NOI18N

        scrollBar = new ThreadsScrollBar();
        zoomInButton = new JButton(Icons.getIcon(GeneralIcons.ZOOM_IN));
        zoomOutButton = new JButton(Icons.getIcon(GeneralIcons.ZOOM_OUT));
        scaleToFitButton = new JButton(scaleToFit ? Icons.getIcon(GeneralIcons.ZOOM) : Icons.getIcon(GeneralIcons.SCALE_TO_FIT));
        comboModel = new DefaultComboBoxModel(new Object[] { VIEW_THREADS_ALL, VIEW_THREADS_LIVE, VIEW_THREADS_FINISHED });
        comboModelWithSelection = new DefaultComboBoxModel(new Object[] {
                                                               VIEW_THREADS_ALL, VIEW_THREADS_LIVE, VIEW_THREADS_FINISHED,
                                                               VIEW_THREADS_SELECTION
                                                           });
        threadsSelectionCombo = new JComboBox(comboModel) {
                public Dimension getMaximumSize() {
                    return getPreferredSize();
                }
                ;
            };
        threadsSelectionCombo.getAccessibleContext().setAccessibleName(COMBO_ACCESS_NAME);
        threadsSelectionCombo.getAccessibleContext().setAccessibleDescription(COMBO_ACCESS_DESCR);

        JLabel showLabel = new JLabel(SHOW_LABEL_TEXT);
        showLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        showLabel.setLabelFor(threadsSelectionCombo);

        int mnemCharIndex = 0;
        showLabel.setDisplayedMnemonic(showLabel.getText().charAt(mnemCharIndex));
        showLabel.setDisplayedMnemonicIndex(mnemCharIndex);

        buttonsToolBar = ProfilerToolbar.create(true);

        JPanel tablePanel = new JPanel();
        JPanel scrollPanel = new JPanel();
        popupMenu = initPopupMenu();

        // set properties
        zoomInButton.setEnabled(!scaleToFit);
        zoomOutButton.setEnabled(!scaleToFit);
        zoomInButton.setToolTipText(ZOOM_IN_TOOLTIP);
        zoomOutButton.setToolTipText(ZOOM_OUT_TOOLTIP);
        scaleToFitButton.setToolTipText(scaleToFit ? FIXED_SCALE_TOOLTIP : SCALE_TO_FIT_TOOLTIP);
        zoomInButton.getAccessibleContext().setAccessibleName(zoomInButton.getToolTipText());
        zoomOutButton.getAccessibleContext().setAccessibleName(zoomOutButton.getToolTipText());
        scaleToFitButton.getAccessibleContext().setAccessibleName(scaleToFitButton.getToolTipText());

        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setSelectionBackground(UIConstants.TABLE_SELECTION_BACKGROUND_COLOR);
        table.setSelectionForeground(UIConstants.TABLE_SELECTION_FOREGROUND_COLOR);
        table.setShowGrid(false);
        table.setRowMargin(0);
        table.setRowHeight(UIUtils.getDefaultRowHeight() + 4);

        DefaultTableCellRenderer defaultHeaderRenderer = new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                component.setBackground(UIUtils.getProfilerResultsBackground());
                component.setFont(table.getFont().deriveFont(Font.BOLD));

                if (component instanceof JComponent) {
                    ((JComponent) component).setBorder(new javax.swing.border.EmptyBorder(0, 3, 0, 3));
                }

                return component;
            }
        };

        table.getTableHeader().setDefaultRenderer(defaultHeaderRenderer);
        table.getTableHeader().setReorderingAllowed(false);

        // fix the first column's width, and make the display column resize
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.getColumnModel().getColumn(NAME_COLUMN_INDEX).setMinWidth(MIN_NAME_COLUMN_WIDTH);
        table.getColumnModel().getColumn(NAME_COLUMN_INDEX).setMaxWidth(1000); // this is for some reason needed for the width to actually work
        table.getColumnModel().getColumn(NAME_COLUMN_INDEX).setPreferredWidth(NAME_COLUMN_WIDTH);

        ThreadStateHeaderRenderer headerRenderer = new ThreadStateHeaderRenderer(this);
        headerRenderer.setBackground(UIUtils.getProfilerResultsBackground());
        table.getColumnModel().getColumn(DISPLAY_COLUMN_INDEX).setHeaderRenderer(headerRenderer);

        TableColumnModel columnModel = table.getColumnModel();
        columnModel.setColumnSelectionAllowed(false);
        columnModel.setColumnMargin(0);
        table.setDefaultRenderer(ThreadNameCellRenderer.class, new ThreadNameCellRenderer(this));
        table.setDefaultRenderer(ThreadStateCellRenderer.class, new ThreadStateCellRenderer(this));

        // perform layout
        tablePanel.setLayout(new BorderLayout());
        scrollPanel.setLayout(new BorderLayout());
        scrollPanel.setBackground(UIUtils.getProfilerResultsBackground());

        buttonsToolBar.add(zoomInButton);
        buttonsToolBar.add(zoomOutButton);
        buttonsToolBar.add(scaleToFitButton);
        buttonsToolBar.addSeparator();
        buttonsToolBar.add(showLabel);
        buttonsToolBar.add(threadsSelectionCombo);
        scrollPanel.add(scrollBar, BorderLayout.EAST);

        //
        ThreadStateIcon runningIcon = new ThreadStateIcon(CommonConstants.THREAD_STATUS_RUNNING, 18, 9);
        ThreadStateIcon sleepingIcon = new ThreadStateIcon(CommonConstants.THREAD_STATUS_SLEEPING, 18, 9);
        ThreadStateIcon monitorIcon = new ThreadStateIcon(CommonConstants.THREAD_STATUS_MONITOR, 18, 9);
        ThreadStateIcon waitIcon = new ThreadStateIcon(CommonConstants.THREAD_STATUS_WAIT, 18, 9);
        ThreadStateIcon parkIcon = new ThreadStateIcon(CommonConstants.THREAD_STATUS_PARK, 18, 9);
        
        runningLegend = new JLabel(CommonConstants.THREAD_STATUS_RUNNING_STRING, runningIcon, SwingConstants.LEADING);
        runningLegend.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        sleepingLegend = new JLabel(CommonConstants.THREAD_STATUS_SLEEPING_STRING, sleepingIcon, SwingConstants.LEADING);
        sleepingLegend.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        waitLegend = new JLabel(CommonConstants.THREAD_STATUS_WAIT_STRING, waitIcon, SwingConstants.LEADING);
        waitLegend.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        parkLegend = new JLabel(CommonConstants.THREAD_STATUS_PARK_STRING, parkIcon, SwingConstants.LEADING);
        parkLegend.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        monitorLegend = new JLabel(CommonConstants.THREAD_STATUS_MONITOR_STRING, monitorIcon, SwingConstants.LEADING);
        monitorLegend.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));

        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, 7, 8));
        legendPanel.setOpaque(false);
        legendPanel.add(runningLegend);
        legendPanel.add(sleepingLegend);

        if (!supportsSleepingState) {
            sleepingLegend.setVisible(false);
        }

        legendPanel.add(waitLegend);
        legendPanel.add(parkLegend);
        legendPanel.add(monitorLegend);

        //legendPanel.add(unknownLegend);
        JPanel bottomPanel = new JPanel(new BorderLayout());
        UIUtils.decorateProfilerPanel(bottomPanel);
        bottomPanel.add(UIUtils.createHorizontalLine(bottomPanel.getBackground()), BorderLayout.NORTH);
        bottomPanel.add(legendPanel, BorderLayout.EAST);

        //scrollPanel.add(bottomPanel, BorderLayout.SOUTH);
        JPanel dataPanel = new JPanel();
        dataPanel.setLayout(new BorderLayout());

        tableScroll = new JScrollPane();
        tableScroll.setCorner(JScrollPane.UPPER_RIGHT_CORNER, new JPanel());
        tableScroll.getCorner(JScrollPane.UPPER_RIGHT_CORNER).setBackground(UIUtils.getProfilerResultsBackground());
        viewPort = new CustomTimeLineViewport(this);
        viewPort.setView(table);
        viewPort.setBackground(UIUtils.getProfilerResultsBackground());
        tableScroll.setViewport(viewPort);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());
        tableScroll.setViewportBorder(BorderFactory.createEmptyBorder());
        tableScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        tableScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        dataPanel.add(tableScroll, BorderLayout.CENTER);
        dataPanel.add(scrollPanel, BorderLayout.SOUTH);
        tablePanel.add(dataPanel, BorderLayout.CENTER);
        tablePanel.add(bottomPanel, BorderLayout.SOUTH);

        // enable threads profiling components
        notificationPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 15));
        notificationPanel.setBorder(dataPanel.getBorder());
        notificationPanel.setBackground(table.getBackground());
        UIUtils.decorateProfilerPanel(notificationPanel);

        Border myRolloverBorder = new CompoundBorder(new FlatToolBar.FlatRolloverButtonBorder(Color.GRAY, Color.LIGHT_GRAY),
                                                     new FlatToolBar.FlatMarginBorder());

        enableThreadsMonitoringLabel1 = new JLabel(THREADS_MONITORING_DISABLED_MSG);
        enableThreadsMonitoringLabel1.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 3));
        enableThreadsMonitoringLabel1.setForeground(Color.DARK_GRAY);

        enableThreadsMonitoringButton = new JButton(Icons.getIcon(ProfilerIcons.VIEW_THREADS_32));
        enableThreadsMonitoringButton.setToolTipText(THREADS_MONITORING_DISABLED_TOOLTIP);
        enableThreadsMonitoringButton.setContentAreaFilled(false);
        enableThreadsMonitoringButton.setMargin(new Insets(3, 3, 3, 3));
        enableThreadsMonitoringButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        enableThreadsMonitoringButton.setHorizontalTextPosition(SwingConstants.CENTER);
        enableThreadsMonitoringButton.setRolloverEnabled(true);
        enableThreadsMonitoringButton.setBorder(myRolloverBorder);
        enableThreadsMonitoringButton.getAccessibleContext().setAccessibleName(ENABLE_THREADS_MONITORING_BUTTON_ACCESS_NAME);

        enableThreadsMonitoringLabel3 = new JLabel(NO_PROFILING_MSG);
        enableThreadsMonitoringLabel3.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 0));
        enableThreadsMonitoringLabel3.setForeground(Color.DARK_GRAY);
        enableThreadsMonitoringLabel3.setVisible(false);

        notificationPanel.add(enableThreadsMonitoringLabel1);
        notificationPanel.add(enableThreadsMonitoringButton);
        notificationPanel.add(enableThreadsMonitoringLabel3);

        setLayout(new BorderLayout());

        contentPanel.add(notificationPanel, ENABLE_THREADS_PROFILING);
        contentPanel.add(tablePanel, THREADS_TABLE);
        add(contentPanel, BorderLayout.CENTER);

        scrollBar.addAdjustmentListener(this);
        zoomInButton.addActionListener(this);
        zoomOutButton.addActionListener(this);
        scaleToFitButton.addActionListener(this);
        threadsSelectionCombo.addActionListener(this);
        showOnlySelectedThreads.addActionListener(this);

        if (detailsCallback != null) {
            showThreadsDetails.addActionListener(this);
        }

        table.getColumnModel().addColumnModelListener(this);
        table.addComponentListener(new ComponentAdapter() {
                public void componentResized(ComponentEvent e) {
                    refreshViewData();
                    updateScrollbar();
                    updateZoomButtonsEnabledState();
                    ThreadsPanel.this.revalidate();
                }
            });

        table.addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent e) {
                    if ((e.getKeyCode() == KeyEvent.VK_CONTEXT_MENU)
                            || ((e.getKeyCode() == KeyEvent.VK_F10) && (e.getModifiers() == InputEvent.SHIFT_MASK))) {
                        int selectedRow = table.getSelectedRow();

                        if (selectedRow != -1) {
                            Rectangle cellRect = table.getCellRect(selectedRow, 0, false);
                            popupMenu.show(e.getComponent(), ((cellRect.x + table.getSize().width) > 50) ? 50 : 5, cellRect.y);
                        }
                    }
                }
            });

        table.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0) {
                        int line = table.rowAtPoint(e.getPoint());

                        if ((line != -1) && (!table.isRowSelected(line))) {
                            if (e.isControlDown()) {
                                table.addRowSelectionInterval(line, line);
                            } else {
                                table.setRowSelectionInterval(line, line);
                            }
                        }
                    }
                }

                public void mouseClicked(MouseEvent e) {
                    int clickedLine = table.rowAtPoint(e.getPoint());

                    if (clickedLine != -1) {
                        if (table.getSelectedRowCount() == 1)
                            table.getSelectionModel().setSelectionInterval(clickedLine, clickedLine);
                        if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0) {
                            popupMenu.show(e.getComponent(), e.getX(), e.getY());
                        } else if ((e.getModifiers() == InputEvent.BUTTON1_MASK) && (e.getClickCount() == 2)) {
                            performDefaultAction();
                        }
                    }
                }
                
            });
        addHierarchyListener(new HierarchyListener() {
            public void hierarchyChanged(HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                    if (isShowing()) dataChanged();
                }
            }
        });

        // Disable traversing table cells using TAB and Shift+TAB
        Set keys = new HashSet(table.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS));
        keys.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0));
        table.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, keys);

        keys = new HashSet(table.getFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS));
        keys.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_MASK));
        table.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, keys);

        updateScrollbar();
        updateZoomButtonsEnabledState();
        manager.addDataListener(this);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public Component getToolbar() {
        return buttonsToolBar.getComponent();
    }
    
    public BufferedImage getCurrentViewScreenshot(boolean onlyVisibleArea) {
        if (onlyVisibleArea) {
            return UIUtils.createScreenshot(tableScroll);
        } else {
            return UIUtils.createScreenshot(table);
        }
    }

    public long getDataEnd() {
        return manager.getEndTime();
    }

    public long getDataStart() {
        return manager.getStartTime();
    }

    public int getDisplayColumnWidth() {
        return table.getTableHeader().getHeaderRect(DISPLAY_COLUMN_INDEX).width;
    }

    public String getThreadClassName(int index) {
        return manager.getThreadClassName(index);
    }

    public ThreadData getThreadData(int index) {
        return manager.getThreadData(index);
    }

    // ---------------------------------------------------------------------------------------
    // Thread data
    public String getThreadName(int index) {
        return manager.getThreadName(index);
    }

    public long getViewEnd() {
        return viewEnd;
    }

    // ---------------------------------------------------------------------------------------
    // View controller
    public long getViewStart() {
        return viewStart;
    }

    /** Invoked when one of the buttons is pressed */
    public void actionPerformed(ActionEvent e) {
        if (internalChange) {
            return;
        }

        if (e.getSource() == scaleToFitButton) {
            if (!scaleToFit) {
                scrollBar.setVisible(true);
                scaleToFitButton.setIcon(Icons.getIcon(GeneralIcons.ZOOM)); // NOI18N
                scaleToFit = true;
            } else {
                scaleToFit = false;
                scaleToFitButton.setIcon(Icons.getIcon(GeneralIcons.SCALE_TO_FIT)); // NOI18N
                scrollBar.setVisible(false);
                scrollBar.setValues(0, 0, 0, 0);
            }

            refreshViewData();
            updateScrollbar();
            updateZoomButtonsEnabledState();
            table.getTableHeader().repaint();
            viewPort.repaint();
        } else if (e.getSource() == zoomInButton) {
            zoomInButton.setEnabled(zoomResolutionPerPixel > 0.1);
            zoomResolutionPerPixel /= 2;
            refreshViewData();
            updateScrollbar();
            updateZoomButtonsEnabledState();
            table.getTableHeader().repaint();
            viewPort.repaint();
        } else if (e.getSource() == zoomOutButton) {
            zoomResolutionPerPixel *= 2;
            refreshViewData();
            updateScrollbar();
            updateZoomButtonsEnabledState();
            table.getTableHeader().repaint();
            viewPort.repaint();
        } else if (e.getSource() == threadsSelectionCombo) {
            if ((threadsSelectionCombo.getModel() == comboModelWithSelection)
                    && (threadsSelectionCombo.getSelectedItem() != VIEW_THREADS_SELECTION)) {
                internalChange = true;

                Object selectedItem = threadsSelectionCombo.getSelectedItem();
                threadsSelectionCombo.setModel(comboModel);
                threadsSelectionCombo.setSelectedItem(selectedItem);
                internalChange = false;
            }

            table.clearSelection();
            dataChanged();
        } else if (e.getSource() == showOnlySelectedThreads) {
            for (int i = filteredDataToDataIndex.size() - 1; i >= 0; i--) {
                if (!table.isRowSelected(i)) {
                    filteredDataToDataIndex.remove(i);
                }
            }

            threadsSelectionCombo.setModel(comboModelWithSelection);
            threadsSelectionCombo.setSelectedItem(VIEW_THREADS_SELECTION);
            table.clearSelection();
        } else if (e.getSource() == showThreadsDetails) {
            performDefaultAction();
        }
    }

    // --- Save Current View action support --------------------------------------
    public void addSaveViewAction(AbstractAction saveViewAction) {
        Component actionButton = buttonsToolBar.add(saveViewAction);
        buttonsToolBar.remove(actionButton);
        buttonsToolBar.add(actionButton, 0);
        buttonsToolBar.add(new JToolBar.Separator(), 1);
    }

    // ---------------------------------------------------------------------------------------
    // Handling profiling started & finished and threads monitoring enabled & disabled
    public void addThreadsMonitoringActionListener(ActionListener listener) {
        enableThreadsMonitoringButton.addActionListener(listener);
    }

    // ---------------------------------------------------------------------------------------
    // Listeners

    /**
     * Invoked when the scrollbar is moved.
     */
    public void adjustmentValueChanged(AdjustmentEvent e) {
        // we know we are in zoom mode (in scaleToFit, the scrollbar is disabled)
        if (!internalScrollbarChange) {
            if ((scrollBar.getValue() + scrollBar.getVisibleAmount()) == scrollBar.getMaximum()) {
                trackingEnd = true;
            } else {
                trackingEnd = false;
                viewStart = manager.getStartTime() + scrollBar.getValue();
                viewEnd = viewStart
                          + (long) (zoomResolutionPerPixel * table.getTableHeader().getHeaderRect(DISPLAY_COLUMN_INDEX).width);
                ThreadsPanel.this.repaint();
            }
        }
    }

    public void columnAdded(TableColumnModelEvent e) {
    } // Ignored

    /**
     * Tells listeners that a column was moved due to a margin change.
     */
    public void columnMarginChanged(ChangeEvent e) {
        refreshViewData();
        updateScrollbar();
        updateZoomButtonsEnabledState();

        if (viewPort != null) {
            viewPort.repaint();
        }

        scrollBar.invalidate();
        ThreadsPanel.this.revalidate();
    }

    public void columnMoved(TableColumnModelEvent e) {
    } // Ignored

    public void columnRemoved(TableColumnModelEvent e) {
    } // Ignored

    public void columnSelectionChanged(ListSelectionEvent e) {
    } // Ignored

    /** Called when data in manager change */
    public void dataChanged() {
        UIUtils.runInEventDispatchThread(new Runnable() {
                public void run() {
                    refreshUI();
                }
            });
    }

    public void dataReset() {
        filteredDataToDataIndex.clear();
        UIUtils.runInEventDispatchThread(new Runnable() {
                public void run() {
                    refreshUI();
                    updateSupportsSleepingState(manager.supportsSleepingStateMonitoring());
                }
            });
    }

    public boolean fitsVisibleArea() {
        return !tableScroll.getVerticalScrollBar().isVisible();
    }

    public boolean hasView() {
        return !notificationPanel.isShowing();
    }

    public void profilingSessionFinished() {
        enableThreadsMonitoringButton.setEnabled(false);
        enableThreadsMonitoringLabel1.setVisible(false);
        enableThreadsMonitoringButton.setVisible(false);
        enableThreadsMonitoringLabel3.setVisible(true);
    }

    public void profilingSessionStarted() {
        enableThreadsMonitoringButton.setEnabled(true);
        enableThreadsMonitoringLabel1.setVisible(true);
        enableThreadsMonitoringButton.setVisible(true);
        enableThreadsMonitoringLabel3.setVisible(false);
    }

    public void removeThreadsMonitoringActionListener(ActionListener listener) {
        enableThreadsMonitoringButton.removeActionListener(listener);
    }

    public void requestFocus() {
        SwingUtilities.invokeLater(new Runnable() { // must be invoked lazily to override default focus of first component
                public void run() {
                    if (table != null) {
                        table.requestFocus();
                    }
                }
            });
    }

    public void threadsMonitoringDisabled() {
        threadsMonitoringEnabled = false;
        ((CardLayout) (contentPanel.getLayout())).show(contentPanel, ENABLE_THREADS_PROFILING);
        updateZoomButtonsEnabledState();
        threadsSelectionCombo.setEnabled(false);
    }

    public void threadsMonitoringEnabled() {
        threadsMonitoringEnabled = true;
        ((CardLayout) (contentPanel.getLayout())).show(contentPanel, THREADS_TABLE);
        updateZoomButtonsEnabledState();
        threadsSelectionCombo.setEnabled(true);
    }

    boolean supportsSleepingState() {
        return supportsSleepingState;
    }

    private JTable createViewTable() {
        return new JExtendedTable(new ThreadsTableModel()) {
                public void mouseMoved(MouseEvent event) {
                    // Identify table row and column at cursor
                    int row = rowAtPoint(event.getPoint());
                    int column = columnAtPoint(event.getPoint());

                    // Only celltip for thread name is supported
                    if (getColumnClass(column) != ThreadNameCellRenderer.class) {
                        CellTipManager.sharedInstance().setEnabled(false);

                        return;
                    }

                    // Return if table cell is the same as in previous event
                    if ((row == lastRow) && (column == lastColumn)) {
                        return;
                    }

                    lastRow = row;
                    lastColumn = column;

                    if ((row < 0) || (column < 0)) {
                        CellTipManager.sharedInstance().setEnabled(false);

                        return;
                    }

                    Component cellRenderer = ((ThreadNameCellRenderer) (getCellRenderer(row, column)))
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                .getTableCellRendererComponentPersistent(this,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         getValueAt(row,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    column),
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         false,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         false,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         row,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         column);
                    Rectangle cellRect = getCellRect(row, column, false);

                    // Return if celltip is not supported for the cell
                    if (cellRenderer == null) {
                        CellTipManager.sharedInstance().setEnabled(false);

                        return;
                    }

                    int horizontalAlignment = ((ThreadNameCellRenderer) cellRenderer).getHorizontalAlignment();

                    if ((horizontalAlignment == SwingConstants.TRAILING) || (horizontalAlignment == SwingConstants.RIGHT)) {
                        rendererRect = new Rectangle((cellRect.x + cellRect.width) - cellRenderer.getPreferredSize().width,
                                                     cellRect.y, cellRenderer.getPreferredSize().width,
                                                     cellRenderer.getPreferredSize().height);
                    } else {
                        rendererRect = new Rectangle(cellRect.x, cellRect.y, cellRenderer.getPreferredSize().width,
                                                     cellRenderer.getPreferredSize().height);
                    }

                    // Return if cell contents is fully visible
                    if ((rendererRect.x >= cellRect.x)
                            && ((rendererRect.x + rendererRect.width) <= (cellRect.x + cellRect.width))) {
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
            };
    }

    private JPopupMenu initPopupMenu() {
        JPopupMenu popup = new JPopupMenu();

        showOnlySelectedThreads = new JMenuItem(SELECTED_THREADS_ITEM);

        if (detailsCallback != null) {
            Font boldfont = popup.getFont().deriveFont(Font.BOLD);
            showThreadsDetails = new JMenuItem(THREAD_DETAILS_ITEM);
            showThreadsDetails.setFont(boldfont);
            popup.add(showThreadsDetails);
            popup.add(new JSeparator());
        }

        popup.add(showOnlySelectedThreads);

        return popup;
    }

    private void performDefaultAction() {
        int[] array = table.getSelectedRows();

        for (int i = 0; i < array.length; i++) {
            array[i] = ((Integer) filteredDataToDataIndex.get(array[i])).intValue();
        }

        ThreadsPanel.this.detailsCallback.showDetails(array);
    }

    // @AWTBound
    private void refreshUI() {
        if (!isShowing()) {
            return;
        }

        updateFilteredData();
        refreshViewData();
        updateScrollbar();
        updateZoomButtonsEnabledState();
        table.invalidate();
        ThreadsPanel.this.revalidate(); // needed to reflect table height increase when new threads appear
        ThreadsPanel.this.repaint(); // needed to paint the table even if no relayout happens
    }

    /** Updates internal view-related data based on changed conditions (new data, change in layout),
     *  to maintain the view in expected condition after the change.
     */
    private void refreshViewData() {
        if (scaleToFit) {
            long dataLen = manager.getEndTime() - manager.getStartTime();
            int viewLen = table.getTableHeader().getHeaderRect(DISPLAY_COLUMN_INDEX).width;
            float currentResolution = (float) dataLen / Math.max(viewLen - RIGHT_DISPLAY_MARGIN - LEFT_DISPLAY_MARGIN, 1);
            viewStart = manager.getStartTime() - (long) (currentResolution * LEFT_DISPLAY_MARGIN);
            viewEnd = manager.getEndTime() + (long) (currentResolution * RIGHT_DISPLAY_MARGIN);
        } else {
            long rightMarginInTime = (long) (zoomResolutionPerPixel * RIGHT_DISPLAY_MARGIN);
            long leftMarginInTime = (long) (zoomResolutionPerPixel * LEFT_DISPLAY_MARGIN);
            long widthInTime = (long) (zoomResolutionPerPixel * table.getTableHeader().getHeaderRect(DISPLAY_COLUMN_INDEX).width);

            if (viewStart == -1) { // the first data came
                viewStart = manager.getStartTime() - leftMarginInTime;
                viewEnd = viewStart + widthInTime;
            }

            if (trackingEnd) {
                viewEnd = manager.getEndTime() + rightMarginInTime;
                viewStart = viewEnd - widthInTime;

                if (viewStart < (manager.getStartTime() - leftMarginInTime)) { // data do not fill display yet
                    viewStart = manager.getStartTime() - leftMarginInTime;
                    viewEnd = viewStart + widthInTime;
                }
            } else {
                if (viewStart < manager.getStartTime()) {
                    viewStart = manager.getStartTime() - rightMarginInTime;
                }

                viewEnd = viewStart + widthInTime;
            }
        }
    }

    /** Creates new filteredDataToDataIndex according to the current filter criterion */
    private void updateFilteredData() {
        if (threadsSelectionCombo.getSelectedItem() == VIEW_THREADS_SELECTION) {
            return; // do nothing, data already filtered
        }

        filteredDataToDataIndex.clear();

        for (int i = 0; i < manager.getThreadsCount(); i++) {
            // view all threads
            if (threadsSelectionCombo.getSelectedItem().equals(VIEW_THREADS_ALL)) {
                filteredDataToDataIndex.add(Integer.valueOf(i));

                continue;
            }

            // view live threads
            if (threadsSelectionCombo.getSelectedItem().equals(VIEW_THREADS_LIVE)) {
                ThreadData threadData = manager.getThreadData(i);

                if (threadData.size() > 0) {
                    byte state = threadData.getLastState();

                    if (state != CommonConstants.THREAD_STATUS_ZOMBIE) {
                        filteredDataToDataIndex.add(Integer.valueOf(i));
                    }
                }

                continue;
            }

            // view finished threads
            if (threadsSelectionCombo.getSelectedItem().equals(VIEW_THREADS_FINISHED)) {
                ThreadData threadData = manager.getThreadData(i);

                if (threadData.size() > 0) {
                    byte state = threadData.getLastState();

                    if (state == CommonConstants.THREAD_STATUS_ZOMBIE) {
                        filteredDataToDataIndex.add(Integer.valueOf(i));
                    }
                } else {
                    // No state defined -> THREAD_STATUS_ZOMBIE assumed (thread could finish when monitoring was disabled)
                    filteredDataToDataIndex.add(Integer.valueOf(i));
                }

                continue;
            }
        }
    }

    private void updateScrollbar() {
        internalScrollbarChange = true;

        if (scrollBar.isVisible() == scaleToFit) {
            scrollBar.setVisible(!scaleToFit);
        }

        if (!scaleToFit) {
            int rightMarginInTime = (int) (zoomResolutionPerPixel * RIGHT_DISPLAY_MARGIN);
            int leftMarginInTime = (int) (zoomResolutionPerPixel * RIGHT_DISPLAY_MARGIN);

            int value = (int) (viewStart - manager.getStartTime()) + leftMarginInTime;
            int extent = (int) (viewEnd - viewStart);
            int intMax = (int) (manager.getEndTime() - manager.getStartTime()) + rightMarginInTime;

            //      System.out.println("max: "+intMax);
            //      System.out.println("value: "+value);
            //      System.out.println("extent: "+extent);
            boolean shouldBeVisible = true;

            if ((value == 0) && ((intMax - (value + extent)) <= 0)) {
                shouldBeVisible = false;
            }

            if (scrollBar.isVisible() != shouldBeVisible) {
                scrollBar.setVisible(shouldBeVisible);
            }

            if (shouldBeVisible) {
                scrollBar.setValues(value, extent, -leftMarginInTime, intMax);
                scrollBar.setBlockIncrement((int) (extent * 0.95f));
                scrollBar.setUnitIncrement(Math.max((int) (zoomResolutionPerPixel * 5), 1)); // at least 1
            }
        }

        internalScrollbarChange = false;
    }

    // ---------------------------------------------------------------------------------------
    // Sleeping state support
    private void updateSupportsSleepingState(boolean supportsSleepingState) {
        if (this.supportsSleepingState != supportsSleepingState) {
            this.supportsSleepingState = supportsSleepingState;
            sleepingLegend.setVisible(supportsSleepingState);
        }
    }

    /*  public static void main(String[] args) {
       JFrame frame = new JFrame("Threads view test");
       frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
       frame.getContentPane().setLayout(new BorderLayout());
       ThreadsDataManager threadsManager = new ThreadsDataManager();
       frame.getContentPane().add(new ThreadsPanel(threadsManager, null, true), BorderLayout.CENTER);
       frame.setSize(800, 600);
       frame.show();
       }
     */

    // ---------------------------------------------------------------------------------------
    // Private methods
    private void updateZoomButtonsEnabledState() {
        if (!threadsMonitoringEnabled) {
            zoomInButton.setEnabled(false);
            zoomOutButton.setEnabled(false);
            scaleToFitButton.setEnabled(false);
        } else {
            if (scaleToFit) {
                zoomInButton.setEnabled(false);
                zoomOutButton.setEnabled(false);
            } else {
                zoomInButton.setEnabled(zoomResolutionPerPixel > 0.1);

                // zoom out is enabled up until the actual data only cover 1/4 of the display area
                int viewWidth = table.getTableHeader().getHeaderRect(DISPLAY_COLUMN_INDEX).width;
                zoomOutButton.setEnabled((zoomResolutionPerPixel * viewWidth) < (2f * (manager.getEndTime()
                                                                                      - manager.getStartTime())));
            }

            scaleToFitButton.setEnabled(true);
            scaleToFitButton.setToolTipText(scaleToFit ? FIXED_SCALE_TOOLTIP : SCALE_TO_FIT_TOOLTIP);
        }
    }
}
