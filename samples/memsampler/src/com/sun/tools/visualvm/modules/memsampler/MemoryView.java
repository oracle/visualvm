/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
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

package com.sun.tools.visualvm.modules.memsampler;

import com.sun.tools.visualvm.attach.HeapHistogramImpl;
import com.sun.tools.visualvm.attach.HeapHistogramImpl.ClassInfoImpl;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
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
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.ui.UIConstants;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.FilterComponent;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import org.netbeans.lib.profiler.ui.components.JExtendedTable;
import org.netbeans.lib.profiler.ui.components.table.ClassNameTableCellRenderer;
import org.netbeans.lib.profiler.ui.components.table.CustomBarCellRenderer;
import org.netbeans.lib.profiler.ui.components.table.DiffBarCellRenderer;
import org.netbeans.lib.profiler.ui.components.table.ExtendedTableModel;
import org.netbeans.lib.profiler.ui.components.table.JExtendedTablePanel;
import org.netbeans.lib.profiler.ui.components.table.LabelBracketTableCellRenderer;
import org.netbeans.lib.profiler.ui.components.table.LabelTableCellRenderer;
import org.netbeans.lib.profiler.ui.components.table.SortableTableModel;

/**
 *
 * @author Jiri Sedlacek
 */
public class MemoryView extends JPanel {

    static final int MODE_HEAP = 1;
    static final int MODE_PERMGEN = 2;


    private MemorySamplerView.Refresher refresher;
    private int mode;


    public MemoryView(MemorySamplerView.Refresher refresher, int mode) {

        this.refresher = refresher;
        this.mode = mode;

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


    public void refresh(HeapHistogramImpl histogram) {
        if (!isShowing() || histogram == null || (pauseButton.isSelected() && !forceRefresh)) return;
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

            Collection<ClassInfoImpl> newClasses = getHistogram(histogram);
            classes = computeDeltaClasses(baseClasses, newClasses);

            totalClasses = baseClasses.size() - newClasses.size();
            totalBytes = getTotalBytes(histogram) - baseTotalBytes;
            totalInstances = getTotalInstances(histogram) - baseTotalInstances;

            long maxAbsDiffBytes = 0;
            for (ClassInfoImpl cInfo : classes)
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
    }

    private Collection<ClassInfoImpl> getHistogram(HeapHistogramImpl histogram) {
        if (mode == MODE_HEAP) return histogram.getHeapHistogram();
        if (mode == MODE_PERMGEN) return histogram.getPermGenHistogram();
        return null;
    }

    private long getTotalBytes(HeapHistogramImpl histogram) {
        if (mode == MODE_HEAP) return histogram.getTotalHeapBytes();
        if (mode == MODE_PERMGEN) return histogram.getTotalPermGenHeapBytes();
        return -1;
    }

    private long getTotalInstances(HeapHistogramImpl histogram) {
        if (mode == MODE_HEAP) return histogram.getTotalHeapInstances();
        if (mode == MODE_PERMGEN) return histogram.getTotalPerGenInstances();
        return -1;
    }

    private static List<ClassInfoImpl> computeDeltaClasses(Collection<ClassInfoImpl> basis, Collection<ClassInfoImpl> changed) {

        Map<String, DeltaClassInfo> deltaMap = new HashMap((int)(basis.size() * 1.3));

        for (ClassInfoImpl cInfo : basis)
            deltaMap.put(cInfo.getName(), new DeltaClassInfo(cInfo, true));

        for (ClassInfoImpl cInfo : changed) {
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

        String[] filterStrings = filterComponent.getFilterStrings();
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
                ClassInfoImpl class1 = classes.get(index1);
                ClassInfoImpl class2 = classes.get(index2);

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
                ClassInfoImpl classs = classes.get(filteredSortedIndexes.get(row));
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

        resTable.getAccessibleContext().setAccessibleName("");
        resTable.getAccessibleContext().setAccessibleDescription("");
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

        columnNames = new String[] { "Class Name", "Bytes [%]", "Bytes", "Instances" };
        columnToolTips = new String[] { "Class Name", "Bytes [%]", "Bytes", "Instances" };
        columnTypes = new Class[] { String.class, Number.class, String.class, String.class };
        columnRenderers = new TableCellRenderer[] { classNameTableCellRenderer, customBarCellRenderer, labelBracketTableCellRenderer, labelBracketTableCellRenderer };
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

        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.setOpaque(false);
        controlPanel.setBorder(BorderFactory.createEmptyBorder(14, 8, 14, 3));

        area = new HTMLTextArea(getBasicTelemetry());
        controlPanel.add(area, BorderLayout.CENTER);

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 0));
        buttonsPanel.setOpaque(false);

        deltaButton = new JToggleButton("Deltas") {
            protected void fireActionPerformed(ActionEvent event) {
                refresher.refresh();
            }
        };
        buttonsPanel.add(deltaButton);

        pauseButton = new JToggleButton("Pause") {
            protected void fireActionPerformed(ActionEvent event) {
                refreshButton.setEnabled(pauseButton.isSelected());
            }
        };
        buttonsPanel.add(pauseButton);

        refreshButton = new JButton("Update!") {
            protected void fireActionPerformed(ActionEvent event) {
                forceRefresh = true;
                refresher.refresh();
            }
        };
        refreshButton.setEnabled(pauseButton.isSelected());
        buttonsPanel.add(refreshButton);

        controlPanel.add(buttonsPanel, BorderLayout.EAST);

        add(controlPanel, BorderLayout.NORTH);

        resTable = initTable();
        resTable.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                MemoryView.this.revalidate();
            }
        });

        resTablePanel = new JExtendedTablePanel(resTable);
        add(resTablePanel, BorderLayout.CENTER);

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
        filterComponent = new FilterComponent();

        filterComponent.addFilterItem(new ImageIcon(filterComponent.getClass()
                                                                   .getResource("/org/netbeans/lib/profiler/ui/resources/filterStartsWith.png") // NOI18N
        ), "Starts with", CommonConstants.FILTER_STARTS_WITH);
        filterComponent.addFilterItem(new ImageIcon(filterComponent.getClass()
                                                                   .getResource("/org/netbeans/lib/profiler/ui/resources/filterContains.png") // NOI18N
        ), "Contains", CommonConstants.FILTER_CONTAINS);
        filterComponent.addFilterItem(new ImageIcon(filterComponent.getClass()
                                                                   .getResource("/org/netbeans/lib/profiler/ui/resources/filterEndsWith.png") // NOI18N
        ), "Ends with", CommonConstants.FILTER_ENDS_WITH);
        filterComponent.addFilterItem(new ImageIcon(filterComponent.getClass()
                                                                   .getResource("/org/netbeans/lib/profiler/ui/resources/filterRegExp.png")), // NOI18N
                                      "Regular expression", CommonConstants.FILTER_REGEXP);

        filterComponent.setFilterValues(filterString, filterType);

        filterComponent.setEmptyFilterText("[Class Name Filter]");

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
        boolean deltas = baseClasses != null;
        String sClasses = totalClasses == -1 ? "" : (deltas && totalClasses > 0 ? "+" : "") + NumberFormat.getInstance().format(totalClasses);
        String sInstances = totalInstances == -1 ? "" : (deltas && totalInstances > 0 ? "+" : "") + NumberFormat.getInstance().format(totalInstances);
        String sBytes = totalBytes == -1 ? "" : (deltas && totalBytes > 0 ? "+" : "") + NumberFormat.getInstance().format(totalBytes);
        return "<nobr><b>Classes: </b>" + sClasses + "&nbsp;&nbsp;&nbsp;&nbsp;<b>Instances: </b>" + sInstances + "&nbsp;&nbsp;&nbsp;&nbsp;<b>Bytes: </b>" + sBytes + "</nobr>";
    }


    private boolean forceRefresh = false;

    private HTMLTextArea area;
    private AbstractButton deltaButton;
    private AbstractButton pauseButton;
    private AbstractButton refreshButton;
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

    private List<ClassInfoImpl> classes = new ArrayList();
    private List<ClassInfoImpl> baseClasses = new ArrayList(); // Needed to correctly setup table renderers
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


    private static class DeltaClassInfo extends ClassInfoImpl {

        String name;
        long instancesCount;
        long bytes;

        DeltaClassInfo(ClassInfoImpl cInfo, boolean negative) {
            name = cInfo.getName();
            instancesCount = negative ? -cInfo.getInstancesCount() : cInfo.getInstancesCount();
            bytes = negative ? -cInfo.getBytes() : cInfo.getBytes();
        }

        void add(ClassInfoImpl cInfo) {
            instancesCount += cInfo.getInstancesCount();
            bytes += cInfo.getBytes();
        }

        public String getName() { return name; }
        public long getInstancesCount() { return instancesCount; }
        public long getBytes() { return bytes; }

    }

}
