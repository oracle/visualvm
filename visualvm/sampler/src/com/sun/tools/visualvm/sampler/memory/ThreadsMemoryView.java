/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.visualvm.sampler.memory;

import com.sun.tools.visualvm.sampler.AbstractSamplerSupport;
import com.sun.tools.visualvm.uisupport.HTMLTextArea;
import com.sun.tools.visualvm.uisupport.TransparentToolBar;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadInfo;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.ui.UIConstants;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.FilterComponent;
import org.netbeans.lib.profiler.ui.components.JExtendedTable;
import org.netbeans.lib.profiler.ui.components.table.ClassNameTableCellRenderer;
import org.netbeans.lib.profiler.ui.components.table.CustomBarCellRenderer;
import org.netbeans.lib.profiler.ui.components.table.ExtendedTableModel;
import org.netbeans.lib.profiler.ui.components.table.JExtendedTablePanel;
import org.netbeans.lib.profiler.ui.components.table.LabelBracketTableCellRenderer;
import org.netbeans.lib.profiler.ui.components.table.LabelTableCellRenderer;
import org.netbeans.lib.profiler.ui.components.table.SortableTableModel;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
final class ThreadsMemoryView extends JPanel {
    
    private final AbstractSamplerSupport.Refresher refresher;
    private boolean forceRefresh = false;
    
    private final MemoryMXBean memoryBean;
    private final MemorySamplerSupport.HeapDumper heapDumper;
    
    private HTMLTextArea area;
    private AbstractButton deltaButton;
    private AbstractButton pauseButton;
    private AbstractButton refreshButton;
    private AbstractButton gcButton;
    private AbstractButton heapdumpButton;
    private JExtendedTable resTable;
    private ExtendedTableModel resTableModel;
    private JExtendedTablePanel resTablePanel;
    private FilterComponent filterComponent;
    private CustomBarCellRenderer customBarCellRenderer;
    private LabelTableCellRenderer labelTableCellRenderer;
    private LabelBracketTableCellRenderer labelBracketTableCellRenderer;
    
    private String filterString = ""; // NOI18N
    private int filterType = CommonConstants.FILTER_CONTAINS;
    
    private List<ThreadInfo> threads;
    private List<Long> allocatedBytes;
    private List<Long> allocatedBytesPerSec;
    private ThreadsMemoryInfo currentThreadsInfo;
    private ThreadsMemoryInfo baseThreadsInfo;
    private List<Integer> filteredSortedIndexes = new ArrayList();
    private int totalThreads = -1;
    private long totalBytes, baseTotalBytes = -1;
    
    private int sortingColumn = 1;
    private boolean sortOrder = false; // Defines the sorting order (ascending or descending)
    private String[] columnNames;
    private TableCellRenderer[] columnRenderers;
    private String[] columnToolTips;
    private Class[] columnTypes;
    private int[] columnWidths;
    private int minNamesColumnWidth; // minimal width of classnames columns
    
    ThreadsMemoryView(AbstractSamplerSupport.Refresher refresher, MemoryMXBean memoryBean, MemorySamplerSupport.HeapDumper heapDumper) {    
        this.refresher = refresher;
        this.memoryBean = memoryBean;
        this.heapDumper = heapDumper;
        initColumnsData();
        initComponents();
        
        addHierarchyListener(new HierarchyListener() {
            public void hierarchyChanged(HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                    if (isShowing()) ThreadsMemoryView.this.refresher.refresh();
                }
            }
        });
    }    
    
    void initSession() {
    }
    
    boolean isPaused() {
        return pauseButton.isSelected() && !forceRefresh;
    }
    
    boolean isEmpty() {
        return resTableModel.getRowCount() == 0;
    }
    
    void refresh(ThreadsMemoryInfo info) {
        if (isPaused()) return;
        forceRefresh = false;
        threads = info.getThreads();
        allocatedBytes = info.getAllocatedBytes();
        totalBytes = info.getTotalBytes();
        if (currentThreadsInfo != null) {
            allocatedBytesPerSec = currentThreadsInfo.getAllocatedBytesPerSecond(info);
        }
        currentThreadsInfo = info;
        if (deltaButton.isSelected()) {
            if (baseThreadsInfo == null) {
                baseThreadsInfo = info;
                baseTotalBytes = totalBytes;
                
                columnRenderers[2] = labelTableCellRenderer;
                updateColumnRenderers();
            }
            totalThreads = threads.size() - baseThreadsInfo.getThreads().size();
            allocatedBytes = baseThreadsInfo.getAllocatedDiffBytes(info);
            totalBytes = baseThreadsInfo.getTotalDiffBytes();
        } else {
            if (baseThreadsInfo != null) {
                baseThreadsInfo = null;
                baseTotalBytes = -1;
                
                columnRenderers[2] = labelBracketTableCellRenderer;
                updateColumnRenderers();
            }
            allocatedBytes = info.getAllocatedBytes();
            totalBytes = info.getTotalBytes();
            totalThreads = threads.size();
            
        }
        customBarCellRenderer.setMaximum(totalBytes);
        updateData(false);
        refreshUI();
    }
    
    void terminate() {
        pauseButton.setEnabled(false);
        refreshButton.setEnabled(false);
        deltaButton.setEnabled(false);
        gcButton.setEnabled(false);
        heapdumpButton.setEnabled(false);
    }
    
    private void updateData(boolean sortOnly) {
        int selectedRow = resTable.getSelectedRow();
        String selectedRowContents = null;
        
        if (selectedRow != -1)
            selectedRowContents = (String) resTable.getValueAt(selectedRow, 0);
        
        if (!sortOnly) filterData();
        sortData();
        
        resTableModel.fireTableDataChanged();
        
        if (selectedRowContents != null)
            resTable.selectRowByContents(selectedRowContents, 0, false);
    }
    
    
    private void filterData() {
        filteredSortedIndexes.clear();
        
        String[] filterStrings = filterComponent.getFilterStrings();
        if (filterType == CommonConstants.FILTER_NONE ||
                filterStrings == null || filterStrings[0].equals("")) { // NOI18N
            for (int i = 0; i < threads.size(); i++) filteredSortedIndexes.add(i);
        } else {
            for (int i = 0; i < threads.size(); i++)
                if (passedFilters(threads.get(i).getThreadName(), filterStrings, filterType))
                    filteredSortedIndexes.add(i);
        }
    }
    
    private static boolean passedFilters(String value, String[] filters, int type) {
        for (int i = 0; i < filters.length; i++)
            if (passedFilter(value, filters[i], type)) return true;
        return false;
    }
    
    private static boolean passedFilter(String value, String filter, int type) {
        // Case insensitive comparison (except regexp):
        switch (type) {
            case CommonConstants.FILTER_STARTS_WITH:
                return value.regionMatches(true, 0, filter, 0, filter.length()); // case insensitive startsWith, optimized
            case CommonConstants.FILTER_CONTAINS:
                return value.toLowerCase().indexOf(filter.toLowerCase()) != -1; // case insensitive indexOf, NOT OPTIMIZED
            case CommonConstants.FILTER_ENDS_WITH:
                
                // case insensitive endsWith, optimized
                return value.regionMatches(true, value.length() - filter.length(), filter, 0, filter.length());
            case CommonConstants.FILTER_EQUALS:
                return value.equalsIgnoreCase(filter); // case insensitive equals
            case CommonConstants.FILTER_REGEXP:
                return value.matches(filter); // still case sensitive!
        }
        
        return false;
    }
    
    private void sortData() {
        Collections.sort(filteredSortedIndexes, new Comparator() {
            
            public int compare(Object o1, Object o2) {
                Integer index1 = (Integer)o1;
                Integer index2 = (Integer)o2;
                
                switch (sortingColumn) {
                    case 0:
                        ThreadInfo ti1 = threads.get(index1);
                        ThreadInfo ti2 = threads.get(index2);
                        return sortOrder ? Long.valueOf(ti1.getThreadId()).compareTo(ti2.getThreadId()) :
                            Long.valueOf(ti2.getThreadId()).compareTo(ti1.getThreadId());
                    case 1:
                    case 2:
                        Long alloc1 = allocatedBytes.get(index1);
                        Long alloc2 = allocatedBytes.get(index2);
                        return sortOrder ? alloc1.compareTo(alloc2) : alloc2.compareTo(alloc1);
                    case 3:
                        Long allocSec1 = allocatedBytesPerSec.get(index1);
                        Long allocSec2 = allocatedBytesPerSec.get(index2);
                        return sortOrder ? allocSec1.compareTo(allocSec2) : allocSec2.compareTo(allocSec1);
                    default:
                        return 0;
                }
            }
            
        });
    }
    
    
    private JExtendedTable initTable() {
        resTableModel = new ExtendedTableModel(new SortableTableModel() {
            public String getColumnName(int col) {
                return columnNames[col];
            }
            
            public int getRowCount() {
                return filteredSortedIndexes.size();
            }
            
            public int getColumnCount() {
                return columnNames.length;
            }
            
            public Class getColumnClass(int col) {
                return columnTypes[col];
            }
            
            public Object getValueAt(int row, int col) {
                int index = filteredSortedIndexes.get(row);
                long alloc = allocatedBytes.get(index).longValue();
                boolean deltas = baseThreadsInfo != null;
                NumberFormat formatter = NumberFormat.getInstance();
                
                switch (col) {
                    case 0:
                        ThreadInfo threadInfo = threads.get(index);
                        return threadInfo.getThreadName() ;
                    case 1:
                        return alloc;
                    case 2:
                        if (deltas) {
                            return alloc > 0 ? "+" + formatter.format(alloc) : formatter.format(alloc); // NOI18N
                        } else {
                            return alloc == 0 ? "0 (0.0%)" : formatter.format(alloc) + " (" + getPercentValue(alloc, totalBytes) + "%)"; // NOI18N
                        }
                    case 3:
                        if (allocatedBytesPerSec != null) {
                            return formatter.format(allocatedBytesPerSec.get(index).longValue());
                        }
                        return "0";
                    default:
                        return null;
                }
            }
            
            private String getPercentValue(float value, float basevalue) {
                int basis = (int) (value / basevalue * 1000f);
                int percent = basis / 10;
                int permille = basis % 10;
                
                return "" + percent + "." + permille; // NOI18N
            }
            
            public String getColumnToolTipText(int col) {
                return columnToolTips[col];
            }
            
            public void sortByColumn(int column, boolean order) {
                sortingColumn = column;
                sortOrder = order;
                updateData(true);
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
        resTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resTable.setGridColor(UIConstants.TABLE_VERTICAL_GRID_COLOR);
        resTable.setSelectionBackground(UIConstants.TABLE_SELECTION_BACKGROUND_COLOR);
        resTable.setSelectionForeground(UIConstants.TABLE_SELECTION_FOREGROUND_COLOR);
        resTable.setShowHorizontalLines(UIConstants.SHOW_TABLE_HORIZONTAL_GRID);
        resTable.setShowVerticalLines(UIConstants.SHOW_TABLE_VERTICAL_GRID);
        resTable.setRowMargin(UIConstants.TABLE_ROW_MARGIN);
        resTable.setRowHeight(UIUtils.getDefaultRowHeight() + 2);
        
        resTable.getAccessibleContext().setAccessibleName(""); // NOI18N
        resTable.getAccessibleContext().setAccessibleDescription(""); // NOI18N
        
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
        
        ClassNameTableCellRenderer classNameTableCellRenderer = new ClassNameTableCellRenderer();
        customBarCellRenderer = new CustomBarCellRenderer(0, 100);
        labelTableCellRenderer = new LabelTableCellRenderer(JLabel.TRAILING);
        labelBracketTableCellRenderer = new LabelBracketTableCellRenderer(JLabel.TRAILING);
        
        columnNames = new String[] {
            NbBundle.getMessage(MemoryView.class, "COL_Thread_name"), // NOI18N
            NbBundle.getMessage(MemoryView.class, "COL_ABytes_rel"), // NOI18N
            NbBundle.getMessage(MemoryView.class, "COL_ABytes"), // NOI18N
            NbBundle.getMessage(MemoryView.class, "COL_ABytes_Sec")}; // NOI18N
        columnToolTips = new String[] {
            NbBundle.getMessage(MemoryView.class, "COL_Thread_name"), // NOI18N
            NbBundle.getMessage(MemoryView.class, "COL_ABytes_rel"), // NOI18N
            NbBundle.getMessage(MemoryView.class, "COL_ABytes"), // NOI18N
            NbBundle.getMessage(MemoryView.class, "COL_ABytes_Sec")}; // NOI18N
        columnTypes = new Class[] { String.class, Number.class, String.class, String.class};
        columnRenderers = new TableCellRenderer[] {
            classNameTableCellRenderer, customBarCellRenderer,
            labelBracketTableCellRenderer, labelTableCellRenderer };
        columnWidths = new int[] { maxWidth, maxWidth, maxWidth, maxWidth };
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
    
    private void updateColumnRenderers() {
        TableColumnModel colModel = resTable.getColumnModel();
        for (int i = 0; i < colModel.getColumnCount(); i++)
            colModel.getColumn(i).setCellRenderer(
                    columnRenderers[resTableModel.getRealColumn(i)]);
    }
    
    private void initComponents() {
        setLayout(new BorderLayout());
        setOpaque(false);
        
        final TransparentToolBar toolBar = new TransparentToolBar();
        
        pauseButton = new JToggleButton() {
            protected void fireActionPerformed(ActionEvent event) {
                boolean selected = pauseButton.isSelected();
                refreshButton.setEnabled(selected);
                if (!selected) refresher.refresh();
            }
        };
        pauseButton.setIcon(new ImageIcon(ImageUtilities.loadImage(
                "com/sun/tools/visualvm/sampler/resources/pause.png", true))); // NOI18N
        pauseButton.setToolTipText(NbBundle.getMessage(MemoryView.class, "TOOLTIP_Pause_results")); // NOI18N
        pauseButton.setOpaque(false);
        toolBar.addItem(pauseButton);
        
        refreshButton = new JButton() {
            protected void fireActionPerformed(ActionEvent event) {
                forceRefresh = true;
                refresher.refresh();
            }
        };
        refreshButton.setIcon(new ImageIcon(ImageUtilities.loadImage(
                "com/sun/tools/visualvm/sampler/resources/update.png", true))); // NOI18N
        refreshButton.setToolTipText(NbBundle.getMessage(MemoryView.class, "TOOLTIP_Update_results")); // NOI18N
        refreshButton.setEnabled(pauseButton.isSelected());
        refreshButton.setOpaque(false);
        toolBar.addItem(refreshButton);
        
        toolBar.addSeparator();
        
        deltaButton = new JToggleButton(NbBundle.getMessage(MemoryView.class, "LBL_Deltas")) { // NOI18N
            protected void fireActionPerformed(ActionEvent event) {
                refresher.refresh();
            }
        };
        deltaButton.setToolTipText(NbBundle.getMessage(MemoryView.class, "TOOLTIP_Deltas")); // NOI18N
        deltaButton.setOpaque(false);
        toolBar.addItem(deltaButton);
        
        toolBar.addFiller();
        
        gcButton = new JButton(NbBundle.getMessage(MemoryView.class, "LBL_Gc")) { // NOI18N
            protected void fireActionPerformed(ActionEvent event) {
                RequestProcessor.getDefault().post(new Runnable() {
                    public void run() {
                        try { memoryBean.gc(); } catch (Exception e) {
                            setEnabled(false);
                            Exceptions.printStackTrace(e);
                        }
                    };
                });
            }
        };
        gcButton.setToolTipText(NbBundle.getMessage(MemoryView.class, "TOOLTIP_Gc")); // NOI18N
        gcButton.setOpaque(false);
        gcButton.setEnabled(memoryBean != null);
        toolBar.addItem(gcButton);
        
        heapdumpButton = new JButton(NbBundle.getMessage(MemoryView.class, "LBL_HeapDump")) { // NOI18N
            protected void fireActionPerformed(ActionEvent event) {
                heapDumper.takeHeapDump((event.getModifiers() &
                        Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) == 0);
            }
        };
        heapdumpButton.setToolTipText(NbBundle.getMessage(MemoryView.class, "TOOLTIP_HeapDump")); // NOI18N
        heapdumpButton.setOpaque(false);
        heapdumpButton.setEnabled(heapDumper != null);
        toolBar.addItem(heapdumpButton);
        
        int maxHeight = pauseButton.getPreferredSize().height;
        maxHeight = Math.max(maxHeight, refreshButton.getPreferredSize().height);
        maxHeight = Math.max(maxHeight, deltaButton.getPreferredSize().height);
        maxHeight = Math.max(maxHeight, gcButton.getPreferredSize().height);
        maxHeight = Math.max(maxHeight, heapdumpButton.getPreferredSize().height);
        
        int width = pauseButton.getPreferredSize().width;
        Dimension size = new Dimension(maxHeight, maxHeight);
        pauseButton.setMinimumSize(size);
        pauseButton.setPreferredSize(size);
        pauseButton.setMaximumSize(size);
        
        width = refreshButton.getPreferredSize().width;
        size = new Dimension(maxHeight, maxHeight);
        refreshButton.setMinimumSize(size);
        refreshButton.setPreferredSize(size);
        refreshButton.setMaximumSize(size);
        
        width = deltaButton.getPreferredSize().width;
        size = new Dimension(width + 5, maxHeight);
        deltaButton.setMinimumSize(size);
        deltaButton.setPreferredSize(size);
        deltaButton.setMaximumSize(size);
        
        width = gcButton.getPreferredSize().width;
        size = new Dimension(width + 5, maxHeight);
        gcButton.setMinimumSize(size);
        gcButton.setPreferredSize(size);
        gcButton.setMaximumSize(size);
        
        width = heapdumpButton.getPreferredSize().width;
        size = new Dimension(width + 5, maxHeight);
        heapdumpButton.setMinimumSize(size);
        heapdumpButton.setPreferredSize(size);
        heapdumpButton.setMaximumSize(size);
        
        toolBar.setBorder(BorderFactory.createEmptyBorder(4, 4, 3, 4));
        
        JPanel resultsPanel = new JPanel(new BorderLayout());
        resultsPanel.setOpaque(false);
        
        JPanel areaPanel = new JPanel(new BorderLayout());
        areaPanel.setOpaque(false);
        area = new HTMLTextArea();
        area.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        refreshUI();
        areaPanel.add(area, BorderLayout.NORTH);
        areaPanel.add(new JSeparator(), BorderLayout.SOUTH);
        
        resultsPanel.add(areaPanel, BorderLayout.NORTH);
        
        add(toolBar, BorderLayout.NORTH);
        
        resTable = initTable();
        resTable.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                ThreadsMemoryView.this.revalidate();
            }
        });
        
        resTablePanel = new JExtendedTablePanel(resTable);
        resultsPanel.add(resTablePanel, BorderLayout.CENTER);
        
        resultsPanel.setBorder(resTablePanel.getBorder());
        resTablePanel.setBorder(BorderFactory.createEmptyBorder());
        
        add(resultsPanel, BorderLayout.CENTER);
        
        initFilterPanel();
    }
    
    private void initFilterPanel() {
        filterComponent = new FilterComponent();
        
        filterComponent.addFilterItem(new ImageIcon(filterComponent.getClass()
                .getResource("/org/netbeans/lib/profiler/ui/resources/filterStartsWith.png")), // NOI18N
                NbBundle.getMessage(MemoryView.class, "LBL_Starts_with"), CommonConstants.FILTER_STARTS_WITH); // NOI18N
        filterComponent.addFilterItem(new ImageIcon(filterComponent.getClass()
                .getResource("/org/netbeans/lib/profiler/ui/resources/filterContains.png")), // NOI18N
                NbBundle.getMessage(MemoryView.class, "LBL_Contains"), CommonConstants.FILTER_CONTAINS); // NOI18N
        filterComponent.addFilterItem(new ImageIcon(filterComponent.getClass()
                .getResource("/org/netbeans/lib/profiler/ui/resources/filterEndsWith.png")), // NOI18N
                NbBundle.getMessage(MemoryView.class, "LBL_Ends_with"), CommonConstants.FILTER_ENDS_WITH); // NOI18N
        filterComponent.addFilterItem(new ImageIcon(filterComponent.getClass()
                .getResource("/org/netbeans/lib/profiler/ui/resources/filterRegExp.png")), // NOI18N
                NbBundle.getMessage(MemoryView.class, "LBL_Regexp"), CommonConstants.FILTER_REGEXP); // NOI18N
        
        filterComponent.setFilterValues(filterString, filterType);
        
        filterComponent.setEmptyFilterText(NbBundle.getMessage(MemoryView.class, "LBL_Thread_filter")); // NOI18N
        
        filterComponent.addFilterListener(new FilterComponent.FilterListener() {
            public void filterChanged() {
                filterString = filterComponent.getFilterString();
                filterType = filterComponent.getFilterType();
                updateData(false);
            }
        });
        
        add(filterComponent, BorderLayout.SOUTH);
    }
    
    private void refreshUI() {
        int selStart = area.getSelectionStart();
        int selEnd   = area.getSelectionEnd();
        area.setText(getBasicTelemetry());
        area.select(selStart, selEnd);
    }
    
    private String getBasicTelemetry() {
        boolean deltas = baseThreadsInfo != null;
        String sThreads = totalThreads == -1 ? "" : (deltas && totalThreads > 0 ? "+" : "") + NumberFormat.getInstance().format(totalThreads); // NOI18N
        String sBytes = totalBytes == -1 ? "" : (deltas && totalBytes > 0 ? "+" : "") + NumberFormat.getInstance().format(totalBytes); // NOI18N
        String ssThreads = NbBundle.getMessage(MemoryView.class, "LBL_Threads", sThreads); // NOI18N
        String ssBytes = NbBundle.getMessage(MemoryView.class, "LBL_ABytes", sBytes); // NOI18N
        return "<nobr>" + ssThreads + "&nbsp;&nbsp;&nbsp;&nbsp;" + ssBytes + "</nobr>"; // NOI18N
    }
    
}
