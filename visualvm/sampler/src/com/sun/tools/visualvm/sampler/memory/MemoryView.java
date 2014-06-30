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

import com.sun.tools.visualvm.application.jvm.HeapHistogram;
import com.sun.tools.visualvm.application.jvm.HeapHistogram.ClassInfo;
import com.sun.tools.visualvm.sampler.AbstractSamplerSupport;
import com.sun.tools.visualvm.uisupport.HTMLTextArea;
import com.sun.tools.visualvm.uisupport.SeparatorLine;
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
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.ui.UIConstants;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.FilterComponent;
import org.netbeans.lib.profiler.ui.components.JExtendedTable;
import org.netbeans.lib.profiler.ui.components.table.ClassNameTableCellRenderer;
import org.netbeans.lib.profiler.ui.components.table.CustomBarCellRenderer;
import org.netbeans.lib.profiler.ui.components.table.DiffBarCellRenderer;
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
final class MemoryView extends JPanel {

    static final int MODE_HEAP = 1;
    static final int MODE_PERMGEN = 2;
    static final int MODE_THREADALLOC = 3;
    
    private final int mode;

    private final AbstractSamplerSupport.Refresher refresher;
    private boolean forceRefresh = false;
    
    private final MemoryMXBean memoryBean;
    private final MemorySamplerSupport.HeapDumper heapDumper;
    private final MemorySamplerSupport.SnapshotDumper snapshotDumper;


    MemoryView(AbstractSamplerSupport.Refresher refresher, int mode,
               MemoryMXBean memoryBean, MemorySamplerSupport.SnapshotDumper snapshotDumper,
               MemorySamplerSupport.HeapDumper heapDumper) {

        this.refresher = refresher;
        this.mode = mode;

        this.memoryBean = memoryBean;
        this.snapshotDumper = snapshotDumper;
        this.heapDumper = heapDumper;
        initColumnsData();
        initComponents();

        addHierarchyListener(new HierarchyListener() {
            public void hierarchyChanged(HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                    if (isShowing()) MemoryView.this.refresher.refresh();
                }
            }
        });
    }


    void initSession() {
        if (snapshotButton != null) snapshotButton.setEnabled(false);
    }

    boolean isPaused() {
        return pauseButton.isSelected() && !forceRefresh;
    }

    boolean isEmpty() {
        return resTableModel.getRowCount() == 0;
    }

    void refresh(HeapHistogram histogram) {
        if (histogram == null || isPaused()) return;
        forceRefresh = false;

        if (deltaButton.isSelected()) {
            if (baseClasses == null) {
                baseClasses = new ArrayList(classes);
                baseTotalBytes = totalBytes;
                baseTotalInstances = totalInstances;

                columnRenderers[1] = diffBarCellRenderer;
                columnRenderers[2] = labelTableCellRenderer;
                columnRenderers[3] = labelTableCellRenderer;
                updateColumnRenderers();
            }

            Collection<ClassInfo> newClasses = getHistogram(histogram);
            classes = computeDeltaClasses(baseClasses, newClasses);

            totalClasses = baseClasses.size() - newClasses.size();
            totalBytes = getTotalBytes(histogram) - baseTotalBytes;
            totalInstances = getTotalInstances(histogram) - baseTotalInstances;

            long maxAbsDiffBytes = 0;
            for (ClassInfo cInfo : classes)
                maxAbsDiffBytes = Math.max(maxAbsDiffBytes, Math.abs(cInfo.getBytes()));

            diffBarCellRenderer.setMaximum(maxAbsDiffBytes);
            diffBarCellRenderer.setMinimum(-maxAbsDiffBytes);
            
        } else {
            if (baseClasses != null) {
                baseClasses = null;
                baseTotalBytes = -1;
                baseTotalInstances = -1;

                columnRenderers[1] = customBarCellRenderer;
                columnRenderers[2] = labelBracketTableCellRenderer;
                columnRenderers[3] = labelBracketTableCellRenderer;
                updateColumnRenderers();
            }
            classes.clear();
            classes.addAll(getHistogram(histogram));

            totalClasses = classes.size();
            totalBytes = getTotalBytes(histogram);
            totalInstances = getTotalInstances(histogram);
            customBarCellRenderer.setMaximum(totalBytes);
        }

        updateData(false);
        refreshUI();

        if (snapshotButton != null) snapshotButton.setEnabled(true);
    }

    void terminate() {
//        refreshRateLabel.setEnabled(false);
//        refreshCombo.setEnabled(false);
//        refreshUnitsLabel.setEnabled(false);
        pauseButton.setEnabled(false);
        refreshButton.setEnabled(false);
        deltaButton.setEnabled(false);
        gcButton.setEnabled(false);
        heapdumpButton.setEnabled(false);
    }

    private Collection getHistogram(HeapHistogram histogram) {
        if (mode == MODE_HEAP) return histogram.getHeapHistogram();
        if (mode == MODE_PERMGEN) return histogram.getPermGenHistogram();
        return null;
    }

    private long getTotalBytes(HeapHistogram histogram) {
        if (mode == MODE_HEAP) return histogram.getTotalHeapBytes();
        if (mode == MODE_PERMGEN) return histogram.getTotalPermGenHeapBytes();
        return -1;
    }

    private long getTotalInstances(HeapHistogram histogram) {
        if (mode == MODE_HEAP) return histogram.getTotalHeapInstances();
        if (mode == MODE_PERMGEN) return histogram.getTotalPerGenInstances();
        return -1;
    }

    private static List<ClassInfo> computeDeltaClasses(Collection<ClassInfo> basis, Collection<ClassInfo> changed) {

        Map<String, DeltaClassInfo> deltaMap = new HashMap((int)(basis.size() * 1.3));

        for (ClassInfo cInfo : basis)
            deltaMap.put(cInfo.getName(), new DeltaClassInfo(cInfo, true));

        for (ClassInfo cInfo : changed) {
            DeltaClassInfo bInfo = deltaMap.get(cInfo.getName());
            if (bInfo != null) bInfo.add(cInfo);
            else deltaMap.put(cInfo.getName(), new DeltaClassInfo(cInfo, false));
        }

        return new ArrayList(deltaMap.values());
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

        String[] filterStrings = FilterComponent.getFilterValues(filterString);
        if (filterType == CommonConstants.FILTER_NONE ||
            filterStrings == null || filterStrings[0].equals("")) { // NOI18N
            for (int i = 0; i < classes.size(); i++) filteredSortedIndexes.add(i);
        } else {
            for (int i = 0; i < classes.size(); i++)
                if (passedFilters(classes.get(i).getName(), filterStrings, filterType))
                    filteredSortedIndexes.add(i);
        }
    }

    private static boolean passedFilters(String value, String[] filters, int type) {
        for (int i = 0; i < filters.length; i++)
            if (passedFilter(value, filters[i], type)) return true;
        return false;
    }

    private static boolean passedFilter(String value, String filter, int type) {
        // Case sensitive comparison:
        /*switch (type) {
           case CommonConstants.FILTER_STARTS_WITH:
             return value.startsWith(filter);
           case CommonConstants.FILTER_CONTAINS:
             return value.indexOf(filter) != -1;
           case CommonConstants.FILTER_ENDS_WITH:
             return value.endsWith(filter);
           case CommonConstants.FILTER_EQUALS:
             return value.equals(filter);
           case CommonConstants.FILTER_REGEXP:
             return value.matches(filter);
           }*/

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
                ClassInfo class1 = classes.get(index1);
                ClassInfo class2 = classes.get(index2);

                switch (sortingColumn) {
                    case 0:
                        return sortOrder ? class1.getName().compareTo(class2.getName()) :
                            class2.getName().compareTo(class1.getName());
                    case 1:
                    case 2:
                        return sortOrder ? ((Long)class1.getBytes()).compareTo(class2.getBytes()) :
                            ((Long)class2.getBytes()).compareTo(class1.getBytes());
                    case 3:
                        return sortOrder ? ((Long)class1.getInstancesCount()).compareTo(class2.getInstancesCount()) :
                            ((Long)class2.getInstancesCount()).compareTo(class1.getInstancesCount());
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
                ClassInfo classs = classes.get(filteredSortedIndexes.get(row));
                boolean deltas = baseClasses != null;
                long bytes = classs.getBytes();
                long instances = classs.getInstancesCount();
                NumberFormat formatter = NumberFormat.getInstance();

                switch (col) {
                    case 0:
                        return classs.getName();
                    case 1:
                        return bytes;
                    case 2:
                        if (deltas) {
                            return bytes > 0 ? "+" + formatter.format(bytes) : formatter.format(bytes); // NOI18N
                        } else {
                            return bytes == 0 ? "0 (0.0%)" : formatter.format(bytes) + " (" + getPercentValue(bytes, totalBytes) + "%)"; // NOI18N
                        }
                    case 3:
                        if (deltas) {
                            return instances > 0 ? "+" + formatter.format(instances) : formatter.format(instances); // NOI18N
                        } else {
                            return instances == 0 ? "0 (0.0%)" : formatter.format(instances) + " (" + getPercentValue(instances, totalInstances) + "%)"; // NOI18N
                        }
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
//        resTable.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
//             .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "DEFAULT_ACTION"); // NOI18N
//        resTable.getActionMap().put("DEFAULT_ACTION", new AbstractAction() { // NOI18N
//            public void actionPerformed(ActionEvent e) {
//                performDefaultAction();
//            }
//        });

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
        diffBarCellRenderer = new DiffBarCellRenderer(0, 100);
        labelTableCellRenderer = new LabelTableCellRenderer(JLabel.TRAILING);
        labelBracketTableCellRenderer = new LabelBracketTableCellRenderer(JLabel.TRAILING);

        columnNames = new String[] { 
            NbBundle.getMessage(MemoryView.class, "COL_Class_name"), // NOI18N
            NbBundle.getMessage(MemoryView.class, "COL_Bytes_rel"), // NOI18N
            NbBundle.getMessage(MemoryView.class, "COL_Bytes"), // NOI18N
            NbBundle.getMessage(MemoryView.class, "COL_Instances") }; // NOI18N
        columnToolTips = new String[] {
            NbBundle.getMessage(MemoryView.class, "COL_Class_name"), // NOI18N
            NbBundle.getMessage(MemoryView.class, "COL_Bytes_rel"), // NOI18N
            NbBundle.getMessage(MemoryView.class, "COL_Bytes"), // NOI18N
            NbBundle.getMessage(MemoryView.class, "COL_Instances") }; // NOI18N
        columnTypes = new Class[] { String.class, Number.class, String.class, String.class };
        columnRenderers = new TableCellRenderer[] {
            classNameTableCellRenderer, customBarCellRenderer, 
            labelBracketTableCellRenderer, labelBracketTableCellRenderer };
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

//    private void performDefaultAction() {
//        int[] array = resTable.getSelectedRows();
//
//        for (int i = 0; i < array.length; i++)
//            array[i] = filteredDataToDataIndex.get(array[i]).threadIndex;
//
//        ThreadsTablePanel.this.detailsCallback.showDetails(array);
//    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setOpaque(false);

        final TransparentToolBar toolBar = new TransparentToolBar();

//        refreshRateLabel = new JLabel("Refresh: ");
//        refreshRateLabel.setToolTipText("Live results refresh rate [ms]");
//        toolBar.add(refreshRateLabel);
//
//        Integer[] refreshRates = new Integer[] { 100, 200, 500, 1000, 2000, 5000, 10000 };
//        refreshCombo = new JComboBox(refreshRates) {
//            public Dimension getMinimumSize() { return getPreferredSize(); }
//            public Dimension getMaximumSize() { return getPreferredSize(); }
//        };
//        refreshCombo.setToolTipText("Live results refresh rate [ms]");
//        refreshCombo.setEditable(false);
//        refreshCombo.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                refresher.setRefreshRate((Integer)refreshCombo.getSelectedItem());
//            }
//        });
//        refreshCombo.setSelectedItem(refresher.getRefreshRate());
//        refreshCombo.setRenderer(new ComboRenderer(refreshCombo));
//        toolBar.add(refreshCombo);
//
//        refreshUnitsLabel = new JLabel(" ms.  ");
//        refreshUnitsLabel.setToolTipText("Live results refresh rate [ms]");
//        toolBar.add(refreshUnitsLabel);

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

        if (snapshotDumper != null) {
            toolBar.addSeparator();
            
            snapshotButton = new JButton(NbBundle.getMessage(MemoryView.class,
                    "LBL_Snapshot"), new ImageIcon(ImageUtilities.loadImage( // NOI18N
                    "com/sun/tools/visualvm/sampler/resources/snapshot.png", true))) { // NOI18N
                protected void fireActionPerformed(ActionEvent event) {
                    snapshotDumper.takeSnapshot((event.getModifiers() & Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) == 0);
                }
            };
            snapshotButton.setToolTipText(NbBundle.getMessage(MemoryView.class, "TOOLTIP_Snapshot")); // NOI18N
            snapshotButton.setOpaque(false);
            snapshotButton.setEnabled(false);
            toolBar.addItem(snapshotButton);
        }

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
        if (snapshotButton != null) maxHeight = Math.max(maxHeight, snapshotButton.getPreferredSize().height);
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

        if (snapshotButton != null) {
            width = snapshotButton.getPreferredSize().width;
            size = new Dimension(width + 5, maxHeight);
            snapshotButton.setMinimumSize(size);
            snapshotButton.setPreferredSize(size);
            snapshotButton.setMaximumSize(size);
        }

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

//        controlPanel.add(toolBar, BorderLayout.NORTH);
//        controlPanel.add(new SeparatorLine(true), BorderLayout.CENTER);

        JPanel resultsPanel = new JPanel(new BorderLayout());
        resultsPanel.setOpaque(false);

        JPanel areaPanel = new JPanel(new BorderLayout());
        areaPanel.setOpaque(false);
        area = new HTMLTextArea();
        area.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        refreshUI();
        areaPanel.add(area, BorderLayout.NORTH);
        areaPanel.add(new SeparatorLine(true), BorderLayout.SOUTH);

        resultsPanel.add(areaPanel, BorderLayout.NORTH);

        add(TransparentToolBar.withSeparator(toolBar), BorderLayout.NORTH);

        resTable = initTable();
        resTable.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                MemoryView.this.revalidate();
            }
        });

        resTablePanel = new JExtendedTablePanel(resTable);
        resTablePanel.clearBorders();
        resultsPanel.add(resTablePanel, BorderLayout.CENTER);

        add(resultsPanel, BorderLayout.CENTER);

        initFilterPanel();

//        resTable.addKeyListener(new KeyAdapter() {
//            public void keyPressed(KeyEvent e) {
//                if ((e.getKeyCode() == KeyEvent.VK_CONTEXT_MENU)
//                        || ((e.getKeyCode() == KeyEvent.VK_F10) && (e.getModifiers() == InputEvent.SHIFT_MASK))) {
//                    int selectedRow = resTable.getSelectedRow();
//
//                    if (selectedRow != -1) {
//                        Rectangle cellRect = resTable.getCellRect(selectedRow, 0, false);
//                        popupMenu.show(e.getComponent(), ((cellRect.x + resTable.getSize().width) > 50) ? 50 : 5, cellRect.y);
//                    }
//                }
//            }
//        });
//
//        resTable.addMouseListener(new MouseAdapter() {
//            public void mousePressed(MouseEvent e) {
//                if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0) {
//                    int line = resTable.rowAtPoint(e.getPoint());
//
//                    if ((line != -1) && (!resTable.isRowSelected(line))) {
//                        if (e.isControlDown()) {
//                            resTable.addRowSelectionInterval(line, line);
//                        } else {
//                            resTable.setRowSelectionInterval(line, line);
//                        }
//                    }
//                }
//            }
//
//            public void mouseClicked(MouseEvent e) {
//                int clickedLine = resTable.rowAtPoint(e.getPoint());
//
//                if (clickedLine != -1) {
//                    if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0) {
//                        popupMenu.show(e.getComponent(), e.getX(), e.getY());
//                    } else if ((e.getModifiers() == InputEvent.BUTTON1_MASK) && (e.getClickCount() == 2)) {
//                        performDefaultAction();
//                    }
//                }
//            }
//
//        });
    }

    private void initFilterPanel() {
        filterComponent = FilterComponent.create(true, true);

        filterComponent.setFilter(filterString, filterType);

        filterComponent.setHint(NbBundle.getMessage(MemoryView.class, "LBL_Class_filter")); // NOI18N

        filterComponent.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                filterString = filterComponent.getFilterValue();
                filterType = filterComponent.getFilterType();
                updateData(false);
            }
        });

        add(filterComponent.getComponent(), BorderLayout.SOUTH);
    }

    private void refreshUI() {
        int selStart = area.getSelectionStart();
        int selEnd   = area.getSelectionEnd();
        area.setText(getBasicTelemetry());
        area.select(selStart, selEnd);
    }

    private String getBasicTelemetry() {
        boolean deltas = baseClasses != null;
        String sClasses = totalClasses == -1 ? "" : (deltas && totalClasses > 0 ? "+" : "") + NumberFormat.getInstance().format(totalClasses); // NOI18N
        String sInstances = totalInstances == -1 ? "" : (deltas && totalInstances > 0 ? "+" : "") + NumberFormat.getInstance().format(totalInstances); // NOI18N
        String sBytes = totalBytes == -1 ? "" : (deltas && totalBytes > 0 ? "+" : "") + NumberFormat.getInstance().format(totalBytes); // NOI18N
        String ssClasses = NbBundle.getMessage(MemoryView.class, "LBL_Classes", sClasses); // NOI18N
        String ssInstances = NbBundle.getMessage(MemoryView.class, "LBL_Instances", sInstances); // NOI18N
        String ssBytes = NbBundle.getMessage(MemoryView.class, "LBL_Bytes", sBytes); // NOI18N
        return "<nobr>" + ssClasses + "&nbsp;&nbsp;&nbsp;&nbsp;" + // NOI18N
               ssInstances + "&nbsp;&nbsp;&nbsp;&nbsp;" + ssBytes + "</nobr>"; // NOI18N
    }


    private HTMLTextArea area;
//    private JLabel refreshRateLabel;
//    private JLabel refreshUnitsLabel;
//    private JComboBox refreshCombo;
    private AbstractButton snapshotButton;
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
    private DiffBarCellRenderer diffBarCellRenderer;
    private LabelTableCellRenderer labelTableCellRenderer;
    private LabelBracketTableCellRenderer labelBracketTableCellRenderer;

    private String filterString = ""; // NOI18N
    private int filterType = CommonConstants.FILTER_CONTAINS;

    private List<ClassInfo> classes = new ArrayList();
    private List<ClassInfo> baseClasses = new ArrayList(); // Needed to correctly setup table renderers
    private List<Integer> filteredSortedIndexes = new ArrayList();
    private int totalClasses = -1;
    private long totalBytes, baseTotalBytes = -1;
    private long totalInstances, baseTotalInstances = -1;

    private int sortingColumn = 1;
    private boolean sortOrder = false; // Defines the sorting order (ascending or descending)
    private String[] columnNames;
    private TableCellRenderer[] columnRenderers;
    private String[] columnToolTips;
    private Class[] columnTypes;
    private int[] columnWidths;
    private int minNamesColumnWidth; // minimal width of classnames columns

    
//    private static class ComboRenderer implements ListCellRenderer {
//
//        private ListCellRenderer renderer;
//
//        ComboRenderer(JComboBox combo) {
//            renderer = combo.getRenderer();
//            if (renderer instanceof JLabel)
//                ((JLabel)renderer).setHorizontalAlignment(JLabel.TRAILING);
//        }
//
//        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
//            return renderer.getListCellRendererComponent(list, NumberFormat.getInstance().format(value), index, isSelected, cellHasFocus);
//        }
//
//    }

    private static class DeltaClassInfo extends ClassInfo {

        String name;
        long instancesCount;
        long bytes;

        DeltaClassInfo(ClassInfo cInfo, boolean negative) {
            name = cInfo.getName();
            instancesCount = negative ? -cInfo.getInstancesCount() : cInfo.getInstancesCount();
            bytes = negative ? -cInfo.getBytes() : cInfo.getBytes();
        }

        void add(ClassInfo cInfo) {
            instancesCount += cInfo.getInstancesCount();
            bytes += cInfo.getBytes();
        }

        public String getName() { return name; }
        public long getInstancesCount() { return instancesCount; }
        public long getBytes() { return bytes; }

    }

}
