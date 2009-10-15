/*
 * Copyright 2007-2009 Sun Microsystems, Inc.  All Rights Reserved.
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

package org.netbeans.lib.profiler.ui.threads;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.results.DataManagerListener;
import org.netbeans.lib.profiler.results.threads.ThreadData;
import org.netbeans.lib.profiler.results.threads.ThreadsDataManager;
import org.netbeans.lib.profiler.ui.UIConstants;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.JExtendedTable;
import org.netbeans.lib.profiler.ui.components.table.ExtendedTableModel;
import org.netbeans.lib.profiler.ui.components.table.JExtendedTablePanel;
import org.netbeans.lib.profiler.ui.components.table.LabelBracketTableCellRenderer;
import org.netbeans.lib.profiler.ui.components.table.LabelTableCellRenderer;
import org.netbeans.lib.profiler.ui.components.table.SortableTableModel;

/**
 *
 * @author Jiri Sedlacek
 */
public class ThreadsTablePanel extends JPanel implements ActionListener, DataManagerListener {
    
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
    
    public ThreadsTablePanel(ThreadsDataManager manager, ThreadsDetailsCallback detailsCallback, boolean supportsSleepingState) {
        tdmanager = manager;
        this.detailsCallback = detailsCallback;
        
        initColumnsData();
        initComponents();
        
        tdmanager.addDataListener(this);
        
        addHierarchyListener(new HierarchyListener() {
            public void hierarchyChanged(HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                    if (isShowing()) dataChanged();
                }
            }
        });
    }
    
    
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
            }
        });
    }
    
    private void refreshUI() {
        if (!isShowing()) {
            return;
        }

        updateFilteredData();
        resTable.invalidate();
        ThreadsTablePanel.this.revalidate(); // needed to reflect table height increase when new threads appear
        ThreadsTablePanel.this.repaint(); // needed to paint the table even if no relayout happens
    }
    
    private void sortData() {
        Collections.sort(filteredDataToDataIndex, new Comparator() {

            public int compare(Object o1, Object o2) {
                Data data1 = (Data)o1;
                Data data2 = (Data)o2;

                switch (sortingColumn) {
                    case 0:
                        return sortOrder ? data1.threadName.compareTo(data2.threadName) :
                            data2.threadName.compareTo(data1.threadName);
                    case 1:
                        return sortOrder ? data1.runningTime.compareTo(data2.runningTime) :
                            data2.runningTime.compareTo(data1.runningTime);
                    case 2:
                        return sortOrder ? data1.sleepingTime.compareTo(data2.sleepingTime) :
                            data2.sleepingTime.compareTo(data1.sleepingTime);
                    case 3:
                        return sortOrder ? data1.waitTime.compareTo(data2.waitTime) :
                            data2.waitTime.compareTo(data1.waitTime);
                    case 4:
                        return sortOrder ? data1.monitorTime.compareTo(data2.monitorTime) :
                            data2.monitorTime.compareTo(data1.monitorTime);
                    case 5:
                        return sortOrder ? data1.totalTime.compareTo(data2.totalTime) :
                            data2.totalTime.compareTo(data1.totalTime);
                    default:
                        return 0;
                }
            }

        });
    }
    
    private void updateFilteredData() {
        if (threadsSelectionCombo.getSelectedItem() == VIEW_THREADS_SELECTION) {
            return; // do nothing, data already filtered
        }

        filteredDataToDataIndex.clear();

        for (int i = 0; i < tdmanager.getThreadsCount(); i++) {
            // view all threads
            if (threadsSelectionCombo.getSelectedItem().equals(VIEW_THREADS_ALL)) {
                filteredDataToDataIndex.add(createData(i));

                continue;
            }

            // view live threads
            if (threadsSelectionCombo.getSelectedItem().equals(VIEW_THREADS_LIVE)) {
                ThreadData threadData = tdmanager.getThreadData(i);

                if (threadData.size() > 0) {
                    byte state = threadData.getLastState();

                    if (state != CommonConstants.THREAD_STATUS_ZOMBIE) {
                        filteredDataToDataIndex.add(createData(i));
                    }
                }

                continue;
            }

            // view finished threads
            if (threadsSelectionCombo.getSelectedItem().equals(VIEW_THREADS_FINISHED)) {
                ThreadData threadData = tdmanager.getThreadData(i);

                if (threadData.size() > 0) {
                    byte state = threadData.getLastState();

                    if (state == CommonConstants.THREAD_STATUS_ZOMBIE) {
                        filteredDataToDataIndex.add(createData(i));
                    }
                } else {
                    // No state defined -> THREAD_STATUS_ZOMBIE assumed (thread could finish when monitoring was disabled)
                    filteredDataToDataIndex.add(createData(i));
                }

                continue;
            }
        }
        
        sortData();
        
    }
    
    private Data createData(int threadIndex) {
        ThreadData threadData = tdmanager.getThreadData(threadIndex);
        boolean dataAvailable = threadData.size() > 0;
        
        long runningTime = dataAvailable ? 0 : -1;
        long sleepingTime = dataAvailable ? 0 : -1;
        long waitTime = dataAvailable ? 0 : -1;
        long monitorTime = dataAvailable ? 0 : -1;
        
        for (int i = 0; i < threadData.size(); i++) {
                
            byte state = threadData.getStateAt(i);
            long stateDuration = getThreadStateDuration(threadData, i);
            switch (state) {
                case CommonConstants.THREAD_STATUS_RUNNING:
                    runningTime += stateDuration;
                    break;
                case CommonConstants.THREAD_STATUS_SLEEPING:
                    sleepingTime += stateDuration;
                    break;
                case CommonConstants.THREAD_STATUS_WAIT:
                    waitTime += stateDuration;
                    break;
                case CommonConstants.THREAD_STATUS_MONITOR:
                    monitorTime += stateDuration;
                    break;
            }
        }
        
        return new Data(threadIndex, tdmanager.getThreadName(threadIndex), runningTime, sleepingTime, waitTime, monitorTime,
                runningTime + sleepingTime + waitTime + monitorTime);
    }
    
    private long getThreadStateDuration(ThreadData threadData, int index) {
        long startTime = threadData.getTimeStampAt(index);
        long endTime = index < (threadData.size() - 1) ?
            threadData.getTimeStampAt(index + 1) : tdmanager.getEndTime();
        return endTime - startTime;
    }
    
    private String getPercentValue(float value, float basevalue) {
        int basis = (int) (value / basevalue * 1000f);
        int percent = basis / 10;
        int permille = basis % 10;

        return "" + percent + "." + permille; // NOI18N
    }
    
    public void requestFocus() {
        SwingUtilities.invokeLater(new Runnable() { // must be invoked lazily to override default focus of first component
            public void run() {
                if (resTable != null) resTable.requestFocus();
            }
        });
    }
    
    public void actionPerformed(ActionEvent e) {
        if (internalChange) {
            return;
        }

        if (e.getSource() == threadsSelectionCombo) {
            if ((threadsSelectionCombo.getModel() == comboModelWithSelection)
                    && (threadsSelectionCombo.getSelectedItem() != VIEW_THREADS_SELECTION)) {
                internalChange = true;

                Object selectedItem = threadsSelectionCombo.getSelectedItem();
                threadsSelectionCombo.setModel(comboModel);
                threadsSelectionCombo.setSelectedItem(selectedItem);
                internalChange = false;
            }

            resTable.clearSelection();
            dataChanged();
        } else if (e.getSource() == showOnlySelectedThreads) {
            for (int i = filteredDataToDataIndex.size() - 1; i >= 0; i--) {
                if (!resTable.isRowSelected(i)) {
                    filteredDataToDataIndex.remove(i);
                }
            }

            threadsSelectionCombo.setModel(comboModelWithSelection);
            threadsSelectionCombo.setSelectedItem(VIEW_THREADS_SELECTION);
            resTable.clearSelection();
        } else if (e.getSource() == showThreadsDetails) {
            performDefaultAction();
        }
    }
    
    
    // --- Save Current View action support --------------------------------------
    public void addSaveViewAction(AbstractAction saveViewAction) {
        JButton actionButton = buttonsToolBar.add(saveViewAction);
        buttonsToolBar.remove(actionButton);

        buttonsToolBar.add(actionButton, 0);
        buttonsToolBar.add(new JToolBar.Separator(), 1);
    }
    
    public boolean fitsVisibleArea() {
        return !resTablePanel.getScrollPane().getVerticalScrollBar().isEnabled();
    }
    
    public boolean hasView() {
        return !filteredDataToDataIndex.isEmpty();
    }
    
    public BufferedImage getCurrentViewScreenshot(boolean onlyVisibleArea) {
        if (onlyVisibleArea) {
            return UIUtils.createScreenshot(resTablePanel.getScrollPane());
        } else {
            return UIUtils.createScreenshot(resTable);
        }
    }
    
    
    private JExtendedTable initTable() {
        resTableModel = new ExtendedTableModel(new SortableTableModel() {
            public String getColumnName(int col) {
                return columnNames[col];
            }

            public int getRowCount() {
                return filteredDataToDataIndex.size();
            }

            public int getColumnCount() {
                return columnNames.length;
            }

            public Class getColumnClass(int col) {
                return columnTypes[col];
            }

            public Object getValueAt(int row, int col) {
                Data data = filteredDataToDataIndex.get(row);
                
                if (data.runningTime == -1) {
                    return col == 0 ? data.threadName : "- (-%)";
                } else switch (col) {
                    case 0:
                        return data.threadName;
                    case 1:
                        return data.runningTime == 0 ? "0.0 (0.0%)" : TimeLineUtils.getMillisValue2(data.runningTime) + " (" + getPercentValue(data.runningTime, data.totalTime) + "%)"; // NOI18N
                    case 2:
                        return data.sleepingTime == 0 ? "0.0 (0.0%)" : TimeLineUtils.getMillisValue2(data.sleepingTime) + " (" + getPercentValue(data.sleepingTime, data.totalTime) + "%)"; // NOI18N
                    case 3:
                        return data.waitTime == 0 ? "0.0 (0.0%)" : TimeLineUtils.getMillisValue2(data.waitTime) + " (" + getPercentValue(data.waitTime, data.totalTime) + "%)"; // NOI18N
                    case 4:
                        return data.monitorTime == 0 ? "0.0 (0.0%)" : TimeLineUtils.getMillisValue2(data.monitorTime) + " (" + getPercentValue(data.monitorTime, data.totalTime) + "%)"; // NOI18N
                    case 5:
                        return data.totalTime == 0 ? "0.0" : TimeLineUtils.getMillisValue2(data.totalTime); // NOI18N
                    default:
                        return null;
                }
            }

            public String getColumnToolTipText(int col) {
                return columnToolTips[col];
            }

            public void sortByColumn(int column, boolean order) {
                sortingColumn = column;
                sortOrder = order;

                int selectedRow = resTable.getSelectedRow();
                String selectedRowContents = null;

                if (selectedRow != -1) {
                    selectedRowContents = (String) resTable.getValueAt(selectedRow, 0);
                }

                sortData();
                refreshUI();

                if (selectedRowContents != null) {
                    resTable.selectRowByContents(selectedRowContents, 0, true);
                }
            }

            /**
             * @param column The table column index
             * @return Initial sorting for the specified column - if true, ascending, if false descending
             */
            public boolean getInitialSorting(int column) {
                switch (column) {
                    case 0:
                        return true;
                    default:
                        return false;
                }
            }
        });

        resTable = new JExtendedTable(resTableModel) {
            public void doLayout() {
                int columnsWidthsSum = 0;
                int realFirstColumn = -1;

                int index;

                for (int i = 0; i < resTableModel.getColumnCount(); i++) {
                    index = resTableModel.getRealColumn(i);

                    if (index == 0) {
                        realFirstColumn = i;
                    } else {
                        columnsWidthsSum += getColumnModel().getColumn(i).getPreferredWidth();
                    }
                }

                if (realFirstColumn != -1) {
                    getColumnModel().getColumn(realFirstColumn)
                        .setPreferredWidth(Math.max(getWidth() - columnsWidthsSum, minNamesColumnWidth));
                }

                super.doLayout();
            }
            ;
        };

        resTableModel.setTable(resTable);
        resTableModel.setInitialSorting(sortingColumn, sortOrder);
        resTable.setRowSelectionAllowed(true);
        resTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        resTable.setGridColor(UIConstants.TABLE_VERTICAL_GRID_COLOR);
        resTable.setSelectionBackground(UIConstants.TABLE_SELECTION_BACKGROUND_COLOR);
        resTable.setSelectionForeground(UIConstants.TABLE_SELECTION_FOREGROUND_COLOR);
        resTable.setShowHorizontalLines(UIConstants.SHOW_TABLE_HORIZONTAL_GRID);
        resTable.setShowVerticalLines(UIConstants.SHOW_TABLE_VERTICAL_GRID);
        resTable.setRowMargin(UIConstants.TABLE_ROW_MARGIN);
        resTable.setRowHeight(UIUtils.getDefaultRowHeight() + 2);
        
        resTable.getAccessibleContext().setAccessibleName(TABLE_ACCESS_NAME);
        resTable.getAccessibleContext().setAccessibleDescription(TABLE_ACCESS_DESCR);
        resTable.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
             .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "DEFAULT_ACTION"); // NOI18N
        resTable.getActionMap().put("DEFAULT_ACTION", new AbstractAction() { // NOI18N
            public void actionPerformed(ActionEvent e) {
                performDefaultAction();
            }
        });

        // Disable traversing table cells using TAB and Shift+TAB
        Set keys = new HashSet(resTable.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS));
        keys.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0));
        resTable.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, keys);

        keys = new HashSet(resTable.getFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS));
        keys.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_MASK));
        resTable.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, keys);

        setColumnsData();

        return resTable;
    }
    
    protected void initColumnsData() {
        int maxWidth = getFontMetrics(getFont()).charWidth('W') * 13; // NOI18N // initial width of data columns
        minNamesColumnWidth = getFontMetrics(getFont()).charWidth('W') * 30; // NOI18N

        LabelTableCellRenderer labelTableCellRenderer1 = new LabelTableCellRenderer(JLabel.LEADING);
        LabelTableCellRenderer labelTableCellRenderer2 = new LabelTableCellRenderer(JLabel.TRAILING);
        LabelBracketTableCellRenderer labelBracketTableCellRenderer = new LabelBracketTableCellRenderer(JLabel.TRAILING);

        columnNames = new String[] { THREADS_COLUMN_NAME, RUNNING_COLUMN_NAME, SLEEPING_COLUMN_NAME, WAIT_COLUMN_NAME, MONITOR_COLUMN_NAME, TOTAL_COLUMN_NAME };
        columnToolTips = new String[] { THREADS_COLUMN_DESCR, RUNNING_COLUMN_DESCR, SLEEPING_COLUMN_DESCR, WAIT_COLUMN_DESCR, MONITOR_COLUMN_DESCR, TOTAL_COLUMN_DESCR };
        columnTypes = new Class[] { String.class, String.class, String.class, String.class, String.class, String.class };
        columnRenderers = new TableCellRenderer[] {
                              labelTableCellRenderer1, labelBracketTableCellRenderer, labelBracketTableCellRenderer, labelBracketTableCellRenderer, labelBracketTableCellRenderer, labelTableCellRenderer2
                          };
        columnWidths = new int[] { maxWidth, maxWidth, maxWidth, maxWidth, maxWidth, maxWidth };
    }
    
    private void setColumnsData() {
        TableColumnModel colModel = resTable.getColumnModel();
        colModel.getColumn(0).setPreferredWidth(minNamesColumnWidth);
        
        int index;
        for (int i = 0; i < colModel.getColumnCount(); i++) {
            index = resTableModel.getRealColumn(i);

            if (index == 0) {
                colModel.getColumn(i).setPreferredWidth(minNamesColumnWidth);
            } else {
                colModel.getColumn(i).setPreferredWidth(columnWidths[index - 1]);
            }
            
            colModel.getColumn(i).setCellRenderer(columnRenderers[index]);
        }
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
        int[] array = resTable.getSelectedRows();

        for (int i = 0; i < array.length; i++)
            array[i] = filteredDataToDataIndex.get(array[i]).threadIndex;

        ThreadsTablePanel.this.detailsCallback.showDetails(array);
    }
    
    private void initComponents() {
        setLayout(new BorderLayout());
        
        resTable = initTable();
        resTablePanel = new JExtendedTablePanel(resTable);
        resTablePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0, 5, 5, 5),
                resTablePanel.getBorder()));
        
        comboModel = new DefaultComboBoxModel(new Object[] { VIEW_THREADS_ALL, VIEW_THREADS_LIVE, VIEW_THREADS_FINISHED });
        comboModelWithSelection = new DefaultComboBoxModel(new Object[] {
                                                               VIEW_THREADS_ALL, VIEW_THREADS_LIVE, VIEW_THREADS_FINISHED,
                                                               VIEW_THREADS_SELECTION
                                                           });
        threadsSelectionCombo = new JComboBox(comboModel) {
                public Dimension getMaximumSize() {
                    return new Dimension(250, getPreferredSize().height);
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

        buttonsToolBar = new JToolBar(JToolBar.HORIZONTAL) {
            public Component add(Component comp) {
                if (comp instanceof JButton) {
                    UIUtils.fixButtonUI((JButton) comp);
                }

                return super.add(comp);
            }
        };
        buttonsToolBar.setFloatable(false);
        buttonsToolBar.putClientProperty("JToolBar.isRollover", Boolean.TRUE); // NOI18N
        buttonsToolBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(5, 5, 0, 5),
                buttonsToolBar.getBorder()));
        
        buttonsToolBar.add(showLabel);
        buttonsToolBar.add(threadsSelectionCombo);
        
        add(buttonsToolBar, BorderLayout.NORTH);
        add(resTablePanel, BorderLayout.CENTER);
        
        threadsSelectionCombo.addActionListener(this);
        
        popupMenu = initPopupMenu();
        showOnlySelectedThreads.addActionListener(this);

        if (detailsCallback != null) {
            showThreadsDetails.addActionListener(this);
        }
        
        resTable.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                ThreadsTablePanel.this.revalidate();
            }
        });

        resTable.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if ((e.getKeyCode() == KeyEvent.VK_CONTEXT_MENU)
                        || ((e.getKeyCode() == KeyEvent.VK_F10) && (e.getModifiers() == InputEvent.SHIFT_MASK))) {
                    int selectedRow = resTable.getSelectedRow();

                    if (selectedRow != -1) {
                        Rectangle cellRect = resTable.getCellRect(selectedRow, 0, false);
                        popupMenu.show(e.getComponent(), ((cellRect.x + resTable.getSize().width) > 50) ? 50 : 5, cellRect.y);
                    }
                }
            }
        });

        resTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0) {
                    int line = resTable.rowAtPoint(e.getPoint());

                    if ((line != -1) && (!resTable.isRowSelected(line))) {
                        if (e.isControlDown()) {
                            resTable.addRowSelectionInterval(line, line);
                        } else {
                            resTable.setRowSelectionInterval(line, line);
                        }
                    }
                }
            }

            public void mouseClicked(MouseEvent e) {
                int clickedLine = resTable.rowAtPoint(e.getPoint());

                if (clickedLine != -1) {
                    if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0) {
                        popupMenu.show(e.getComponent(), e.getX(), e.getY());
                    } else if ((e.getModifiers() == InputEvent.BUTTON1_MASK) && (e.getClickCount() == 2)) {
                        performDefaultAction();
                    }
                }
            }

        });
    }
    
    private ThreadsDataManager tdmanager;
    private ThreadsDetailsCallback detailsCallback;
    private JExtendedTable resTable;
    private ExtendedTableModel resTableModel;
    private JExtendedTablePanel resTablePanel;
    private JComboBox threadsSelectionCombo;
    private JToolBar buttonsToolBar;
    
    private JMenuItem showOnlySelectedThreads;
    private JMenuItem showThreadsDetails;
    private JPopupMenu popupMenu;
    
    private DefaultComboBoxModel comboModel;
    private DefaultComboBoxModel comboModelWithSelection;
    
    private ArrayList<Data> filteredDataToDataIndex = new ArrayList();
    
    private int sortingColumn = 1;
    protected boolean sortOrder = false; // Defines the sorting order (ascending or descending)
    protected String[] columnNames;
    protected TableCellRenderer[] columnRenderers;
    protected String[] columnToolTips;
    protected Class[] columnTypes;
    protected int[] columnWidths;
    private int minNamesColumnWidth; // minimal width of classnames columns
    private boolean internalChange = false; // prevents cycles in event handling
    
    
    private static class Data {
        Integer threadIndex;
        String threadName;
        Long runningTime;
        Long sleepingTime;
        Long waitTime;
        Long monitorTime;
        Long totalTime;
        
        public Data(Integer threadIndex, String threadName, Long runningTime, Long sleepingTime, Long waitTime, Long monitorTime, Long totalTime) {
            this.threadIndex = threadIndex;
            this.threadName = threadName;
            this.runningTime = runningTime;
            this.sleepingTime = sleepingTime;
            this.waitTime = waitTime;
            this.monitorTime = monitorTime;
            this.totalTime = totalTime;
        }
    }
    
    
    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.netbeans.lib.profiler.ui.threads.Bundle"); // NOI18N
    private static final String VIEW_THREADS_ALL = messages.getString("ThreadsPanel_ViewThreadsAll"); // NOI18N
    private static final String VIEW_THREADS_LIVE = messages.getString("ThreadsPanel_ViewThreadsLive"); // NOI18N
    private static final String VIEW_THREADS_FINISHED = messages.getString("ThreadsPanel_ViewThreadsFinished"); // NOI18N
    private static final String VIEW_THREADS_SELECTION = messages.getString("ThreadsPanel_ViewThreadsSelection"); // NOI18N
    private static final String THREADS_COLUMN_NAME = messages.getString("ThreadsTablePanel_ThreadsColumnName"); // NOI18N
    private static final String RUNNING_COLUMN_NAME = messages.getString("ThreadsTablePanel_RunningColumnName"); // NOI18N
    private static final String SLEEPING_COLUMN_NAME = messages.getString("ThreadsTablePanel_SleepingColumnName"); // NOI18N
    private static final String WAIT_COLUMN_NAME = messages.getString("ThreadsTablePanel_WaitColumnName"); // NOI18N
    private static final String MONITOR_COLUMN_NAME = messages.getString("ThreadsTablePanel_MonitorColumnName"); // NOI18N
    private static final String TOTAL_COLUMN_NAME = messages.getString("ThreadsTablePanel_TotalColumnName"); // NOI18N
    private static final String THREADS_COLUMN_DESCR = messages.getString("ThreadsTablePanel_ThreadsColumnDescr"); // NOI18N
    private static final String RUNNING_COLUMN_DESCR = messages.getString("ThreadsTablePanel_RunningColumnDescr"); // NOI18N
    private static final String SLEEPING_COLUMN_DESCR = messages.getString("ThreadsTablePanel_SleepingColumnDescr"); // NOI18N
    private static final String WAIT_COLUMN_DESCR = messages.getString("ThreadsTablePanel_WaitColumnDescr"); // NOI18N
    private static final String MONITOR_COLUMN_DESCR = messages.getString("ThreadsTablePanel_MonitorColumnDescr"); // NOI18N
    private static final String TOTAL_COLUMN_DESCR = messages.getString("ThreadsTablePanel_TotalColumnDescr"); // NOI18N
    private static final String SELECTED_THREADS_ITEM = messages.getString("ThreadsPanel_SelectedThreadsItem"); // NOI18N
    private static final String THREAD_DETAILS_ITEM = messages.getString("ThreadsPanel_ThreadDetailsItem"); // NOI18N
    private static final String TABLE_ACCESS_NAME = messages.getString("ThreadsPanel_TableAccessName"); // NOI18N
    private static final String TABLE_ACCESS_DESCR = messages.getString("ThreadsPanel_TableAccessDescr"); // NOI18N
    private static final String COMBO_ACCESS_NAME = messages.getString("ThreadsPanel_ComboAccessName"); // NOI18N
    private static final String COMBO_ACCESS_DESCR = messages.getString("ThreadsPanel_ComboAccessDescr"); // NOI18N
    private static final String SHOW_LABEL_TEXT = messages.getString("ThreadsPanel_ShowLabelText"); // NOI18N
    // -----
    

}
