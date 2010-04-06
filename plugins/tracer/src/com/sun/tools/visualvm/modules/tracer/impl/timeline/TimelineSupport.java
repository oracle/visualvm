/*
 * Copyright 2007-2010 Sun Microsystems, Inc.  All Rights Reserved.
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

package com.sun.tools.visualvm.modules.tracer.impl.timeline;

import com.sun.tools.visualvm.modules.tracer.ItemValueFormatter;
import com.sun.tools.visualvm.modules.tracer.ProbeItemDescriptor;
import com.sun.tools.visualvm.modules.tracer.TracerProbe;
import com.sun.tools.visualvm.modules.tracer.TracerProbeDescriptor;
import com.sun.tools.visualvm.modules.tracer.impl.options.TracerOptions;
import com.sun.tools.visualvm.modules.tracer.impl.details.DetailsPanel;
import com.sun.tools.visualvm.modules.tracer.impl.details.DetailsTableModel;
import com.sun.tools.visualvm.modules.tracer.impl.timeline.TimelineChart.Row;
import com.sun.tools.visualvm.modules.tracer.impl.timeline.items.ValueItemDescriptor;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import org.netbeans.lib.profiler.charts.ChartContext;
import org.netbeans.lib.profiler.charts.ChartSelectionModel;
import org.netbeans.lib.profiler.charts.ItemSelection;
import org.netbeans.lib.profiler.charts.PaintersModel;
import org.netbeans.lib.profiler.charts.Timeline;
import org.netbeans.lib.profiler.charts.xy.XYItemPainter;
import org.netbeans.lib.profiler.charts.xy.XYItemSelection;
import org.netbeans.lib.profiler.charts.xy.synchronous.SynchronousXYItem;
import org.netbeans.lib.profiler.charts.xy.synchronous.SynchronousXYItemsModel;

/**
 * All methods must be invoked from the EDT.
 *
 * @author Jiri Sedlacek
 */
public final class TimelineSupport {

    public static final int[] EMPTY_TIMESTAMPS = new int[0];

    private final TimelineChart chart;
    private final TimelineModel model;
    private final SynchronousXYItemsModel itemsModel;

    private final TimelineTooltipOverlay tooltips;
    private final TimelineUnitsOverlay units;

    private final List<TracerProbe> probes = new ArrayList();
    private final List<TimelineChart.Row> rows = new ArrayList();
    private final DescriptorResolver descriptorResolver;

    private final Set<Integer> selectedTimestamps = new HashSet();
    private final Set<SelectionListener> selectionListeners = new HashSet();


    // --- Constructor ---------------------------------------------------------

    public TimelineSupport(DescriptorResolver descriptorResolver) {
        this.descriptorResolver = descriptorResolver;
        
        // TODO: must be called in EDT!
        model = new TimelineModel();
        itemsModel = new SynchronousXYItemsModel(model);
        chart = new TimelineChart(itemsModel);
        tooltips = new TimelineTooltipOverlay(this);
        chart.addOverlayComponent(tooltips);

        if (TracerOptions.getInstance().isShowValuesEnabled()) {
            units = new TimelineUnitsOverlay(chart);
            chart.addOverlayComponent(units);
        } else {
            units = null;
        }
    }


    // --- Chart access --------------------------------------------------------

    TimelineChart getChart() {
        return chart;
    }

    public ChartSelectionModel getChartSelectionModel() {
        return chart.getSelectionModel();
    }


    // --- Probes management ---------------------------------------------------

    public void addProbe(final TracerProbe probe) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                TimelineChart.Row row = chart.addRow();

                probes.add(probe);
                rows.add(row);

                ProbeItemDescriptor[] itemDescriptors = probe.getItemDescriptors();
                TimelineXYItem[] items = model.createItems(itemDescriptors);
                XYItemPainter[] painters  = new XYItemPainter[items.length];
                for (int i = 0; i < painters.length; i++)
                    painters[i] = TimelinePaintersFactory.createPainter(
                            itemDescriptors[i], i);
                
                row.addItems(items, painters);

                setupOverlays();
            }
        });
    }

    public void removeProbe(final TracerProbe probe) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                TimelineChart.Row row = getRow(probe);

                chart.removeRow(row);
                
                model.removeItems(row.getItems());

                rows.remove(row);
                probes.remove(probe);

                setupOverlays();
            }
        });
    }

    public List<TracerProbe> getProbes() {
        return probes;
    }

    public int getItemsCount() {
        return model.getItemsCount();
    }


    // --- Tooltips support ----------------------------------------------------

    private void setupOverlays() {
        final int rowsCount = chart.getRowsCount();

//        TimelineTooltipPainter[] ttPainters = new TimelineTooltipPainter[rowsCount];
        TimelineTooltipPainter.Model[] rowModels = new TimelineTooltipPainter.Model[rowsCount];
        
        for (int rowIndex = 0; rowIndex < rowModels.length; rowIndex++) {
            final TimelineChart.Row row = chart.getRow(rowIndex);
            final TracerProbe probe = getProbe(row);

            final int itemsCount = row.getItemsCount();
            final String[] rowNames = new String[itemsCount];
            final ValueItemDescriptor[] viDescriptors = new ValueItemDescriptor[itemsCount];
            final String[] unitsStrings = new String[itemsCount];
            for (int itemIndex = 0; itemIndex < itemsCount; itemIndex++) {
                rowNames[itemIndex] = ((TimelineXYItem)row.getItem(itemIndex)).getName();
                viDescriptors[itemIndex] = (ValueItemDescriptor)probe.getItemDescriptors()[itemIndex];
                unitsStrings[itemIndex] = viDescriptors[itemIndex].getUnitsString(ItemValueFormatter.FORMAT_TOOLTIP);
            }

            rowModels[rowIndex] = new TimelineTooltipPainter.Model() {

                public int getRowsCount() {
                    return itemsCount;
                }

                public String getRowName(int index) {
                    return rowNames[index];
                }

                public String getRowValue(int index, long itemValue) {
                    return viDescriptors[index].getValueString(itemValue,
                            ItemValueFormatter.FORMAT_TOOLTIP);
                }

                public String getRowUnits(int index) {
                    return unitsStrings[index];
                }

            };
        }
        tooltips.setupModel(rowModels);

        if (units != null) units.setupModel(new TimelineUnitsOverlay.Model() {

            private final String LAST_UNITS_STRING = "lastUnitsString"; // NOI18N

            private Color[][] rowColors = new Color[rowsCount][];
            private String[][] rowMinValues = new String[rowsCount][];
            private String[][] rowMaxValues = new String[rowsCount][];

            private List<Color> visibleRowItemColors;
            private List<String> visibleRowItemMinValues;
            private List<String> visibleRowItemMaxValues;

            public void prefetch() {
                PaintersModel paintersModel = chart.getPaintersModel();
                for (int rowIndex = 0; rowIndex < rowsCount; rowIndex++) {
                    
                    Row row = chart.getRow(rowIndex);
                    TracerProbe probe = getProbe(row);
                    int rowItemsCount = row.getItemsCount();

                    ChartContext rowContext = row.getContext();
                    long commonMinY = rowContext.getDataOffsetY();
                    long commonMaxY = commonMinY + rowContext.getDataHeight();

                    if (visibleRowItemColors != null) {
                        visibleRowItemColors.clear();
                        visibleRowItemMinValues.clear();
                        visibleRowItemMaxValues.clear();
                    } else {
                        visibleRowItemColors = new ArrayList(rowItemsCount);
                        visibleRowItemMinValues = new ArrayList(rowItemsCount);
                        visibleRowItemMaxValues = new ArrayList(rowItemsCount);
                    }
                    
                    boolean sameFactorUnits = true;
                    double lastDataFactor = -1;
                    String lastUnitsString = LAST_UNITS_STRING;

                    for (int itemIndex = 0; itemIndex < rowItemsCount; itemIndex++) {
                        TimelineXYItem item = (TimelineXYItem)row.getItem(itemIndex);
                        TimelineXYPainter painter =
                                (TimelineXYPainter)paintersModel.getPainter(item);

                        if (painter.isPainting()) {
                            visibleRowItemColors.add(itemColor(painter));

                            ValueItemDescriptor descriptor = (ValueItemDescriptor)
                                    probe.getItemDescriptors()[itemIndex];

                            double dataFactor = descriptor.getDataFactor();
                            String unitsString = descriptor.getUnitsString(
                                    ItemValueFormatter.FORMAT_UNITS);
                            
                            if (sameFactorUnits) {
                                if (lastDataFactor == -1)
                                    lastDataFactor = dataFactor;
                                else if (lastDataFactor != dataFactor)
                                    sameFactorUnits = false;
                                lastDataFactor = dataFactor;
                                
                                if (lastUnitsString == LAST_UNITS_STRING)
                                    lastUnitsString = unitsString;
                                else if (!equals(lastUnitsString, unitsString))
                                    sameFactorUnits = false;
                                lastUnitsString = unitsString;
                            }

                            String minValueString = descriptor.getValueString(
                                    (long)(commonMinY / painter.dataFactor),
                                    ItemValueFormatter.FORMAT_UNITS);
                            visibleRowItemMinValues.add(unitsString == null ?
                                minValueString : minValueString + " " + unitsString);
                            
                            String maxValueString = descriptor.getValueString(
                                    (long)(commonMaxY / painter.dataFactor),
                                    ItemValueFormatter.FORMAT_UNITS);
                            visibleRowItemMaxValues.add(unitsString == null ?
                                maxValueString : maxValueString + " " + unitsString);
                        }
                    }

                    if (sameFactorUnits) {
                        rowColors[rowIndex] = new Color[] { null };
                        rowMinValues[rowIndex] =
                                new String[] { visibleRowItemMinValues.get(0) };
                        rowMaxValues[rowIndex] =
                                new String[] { visibleRowItemMaxValues.get(0) };
                    } else {
                        rowColors[rowIndex] = visibleRowItemColors.toArray(
                                new Color[visibleRowItemColors.size()]);
                        rowMinValues[rowIndex] = visibleRowItemMinValues.toArray(
                                new String[visibleRowItemMinValues.size()]);
                        rowMaxValues[rowIndex] = visibleRowItemMaxValues.toArray(
                                new String[visibleRowItemMaxValues.size()]);
                    }
                }
            }

            public Color[] getColors(Row row) {
                return rowColors[row.getIndex()];
            }

            public String[] getMinUnits(TimelineChart.Row row) {
                return rowMinValues[row.getIndex()];
            }

            public String[] getMaxUnits(TimelineChart.Row row) {
                return rowMaxValues[row.getIndex()];
            }

            private Color itemColor(TimelineXYPainter painter) {
                Color color = painter.lineColor;
                if (color == null) color = painter.fillColor;
                return color;
            }

            private boolean equals(String s1, String s2) {
                if (s1 == null) {
                    if (s2 == null) return true;
                    else return false;
                } else {
                    return s1.equals(s2);
                }
            }
            
        });
    }


    // --- Rows <-> Probes mapping ---------------------------------------------

    TimelineChart.Row getRow(TracerProbe probe) {
        return rows.get(probes.indexOf(probe));
    }

    TracerProbe getProbe(TimelineChart.Row row) {
        return probes.get(rows.indexOf(row));
    }


    // --- Probe -> Descriptor mapping -----------------------------------------

    TracerProbeDescriptor getDescriptor(TracerProbe p) {
        return descriptorResolver.getDescriptor(p);
    }


    // --- Values management ---------------------------------------------------

    public void addValues(final long timestamp, final long[] newValues) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                int newRow = detailsModel == null ? -1 : detailsModel.getRowCount();
                model.addValues(timestamp, newValues);
                itemsModel.valuesAdded();
                if (newRow != -1) detailsModel.fireTableRowsInserted(newRow, newRow);
            }
        });
    }

    public void resetValues() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                int lastRow = detailsModel == null ? -1 : detailsModel.getRowCount() - 1;
                model.reset();
                itemsModel.valuesReset();
                resetSelectedTimestamps();
                if (lastRow != -1) detailsModel.fireTableRowsDeleted(0, lastRow);
            }
        });
    }

    // --- Row selection management --------------------------------------------

    private AbstractTableModel detailsModel;

    void rowSelectionChanged() {
        updateSelectedItems();
        notifyRowSelectionChanged();
    }

    public TableModel getDetailsModel() {
        if (!chart.isRowSelection()) detailsModel = null;
        else detailsModel = createSelectionModel();
        return detailsModel;
    }

    private AbstractTableModel createSelectionModel() {
        final List<SynchronousXYItem> selectedItems = getSelectedItems();
        final List<ValueItemDescriptor> selectedDescriptors = getSelectedDescriptors();
        int selectedItemsCount = selectedItems.size();
        
        final int columnCount = selectedItemsCount + 2;
        final SynchronousXYItem[] selectedItemsArr =
                selectedItems.toArray(new SynchronousXYItem[selectedItemsCount]);
        final String[] columnNames = new String[columnCount];
        columnNames[0] = "Mark";
        columnNames[1] = "Time [ms]";
        final String[] columnTooltips = new String[columnCount];
        columnTooltips[0] = "Mark a timestamp in Timeline view";
        columnTooltips[1] = "Timestamp of the data";
        for (int i = 2; i < columnCount; i++) {
            String itemName = selectedItemsArr[i - 2].getName();
            String unitsString = selectedDescriptors.get(i - 2).
                                 getUnitsString(ItemValueFormatter.FORMAT_DETAILS);
            String unitsExt = unitsString == null ? "" : " [" + unitsString + "]";
            columnNames[i] = itemName + unitsExt;
            columnTooltips[i] = selectedDescriptors.get(i - 2).getDescription();
        }

        return new DetailsTableModel() {

            public int getRowCount() {
                return model.getTimestampsCount();
            }

            public int getColumnCount() {
                return columnCount;
            }

            public String getColumnName(int columnIndex) {
                return columnNames[columnIndex];
            }

            public String getColumnTooltip(int columnIndex) {
                return columnTooltips[columnIndex];
            }

            public Class getColumnClass(int columnIndex) {
                if (columnIndex == 0) return Boolean.class;
                if (columnIndex == 1) return DetailsPanel.class;
                return Long.class;
            }

            public ValueItemDescriptor getDescriptor(int columnIndex) {
                if (columnIndex == 0) return null;
                if (columnIndex == 1) return null;
                return selectedDescriptors.get(columnIndex - 2);
            }

            public Object getValueAt(int rowIndex, int columnIndex) {
                if (columnIndex == 0) return selectedTimestamps.contains(rowIndex);
                if (columnIndex == 1) return model.getTimestamp(rowIndex);
                return selectedItemsArr[columnIndex - 2].getYValue(rowIndex);
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return columnIndex == 0;
            }

            public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
                if (Boolean.TRUE.equals(aValue)) selectTimestamp(rowIndex, false);
                else unselectTimestamp(rowIndex, false);
            }

        };
    }


    // --- Time selection management -------------------------------------------

    private static final int SCROLL_MARGIN_LEFT = 10;
    private static final int SCROLL_MARGIN_RIGHT = 50;


    public void selectTimestamp(int index) {
        selectTimestamp(index, true);
    }

    private void selectTimestamp(int index, boolean notifyTable) {
        boolean change = selectedTimestamps.add(index);
        if (notifyTable && detailsModel != null)
            detailsModel.fireTableCellUpdated(index, 0);
        if (change) {
            updateSelectedItems();
            notifyTimeSelectionChanged();
        }
    }

    public void unselectTimestamp(int index) {
        unselectTimestamp(index, true);
    }

    private void unselectTimestamp(int index, boolean notifyTable) {
        boolean change = selectedTimestamps.remove(index);
        if (notifyTable && detailsModel != null)
            detailsModel.fireTableCellUpdated(index, 0);
        if (change) {
            updateSelectedItems();
            notifyTimeSelectionChanged();
        }
    }

    public void resetSelectedTimestamps() {
        if (selectedTimestamps.isEmpty()) return;
        selectedTimestamps.clear();
        detailsModel.fireTableDataChanged();
        updateSelectedItems();
        notifyTimeSelectionChanged();
    }

    private void updateSelectedItems() {
        List<SynchronousXYItem> selectedItems = getSelectedItems();
        List<ItemSelection> selections =
                new ArrayList(selectedItems.size() * selectedTimestamps.size());

        for (int selectedIndex : selectedTimestamps)
            for (SynchronousXYItem selectedItem : selectedItems)
                selections.add(new XYItemSelection.Default(selectedItem,
                               selectedIndex, XYItemSelection.DISTANCE_UNKNOWN));

        chart.getSelectionModel().setSelectedItems(selections);
    }

    public Set<Integer> getSelectedTimestamps() {
        return selectedTimestamps;
    }

    public void highlightTimestamp(int selectedIndex) {
//        List<SynchronousXYItem> selectedItems = new ArrayList();
//        if (selectedIndex != -1) {
//            int rowsCount = chart.getRowsCount();
//            for (int i = 0; i < rowsCount; i++)
//                selectedItems.addAll(Arrays.asList(chart.getRow(i).getItems()));
//        }
//        List<ItemSelection> selections = new ArrayList(selectedItems.size());
//        if (selectedIndex != -1) {
//            for (SynchronousXYItem selectedItem : selectedItems)
//                selections.add(new XYItemSelection.Default(selectedItem,
//                               selectedIndex, XYItemSelection.DISTANCE_UNKNOWN));
//        }
//
        ChartSelectionModel selectionModel = chart.getSelectionModel();
        List<ItemSelection> oldSelection = selectionModel.getHighlightedItems();
        int oldSelectedIndex = -1;
        if (!oldSelection.isEmpty()) {
            XYItemSelection sel = (XYItemSelection)oldSelection.get(0);
            oldSelectedIndex = sel.getValueIndex();
        }
//        selectionModel.setHighlightedItems(selections);

        if (selectedIndex != -1)
            scrollChartToSelection(oldSelectedIndex, selectedIndex);
    }


    private void scrollChartToSelection(int oldIndex, int newIndex) {
        Timeline timeline = itemsModel.getTimeline();
        ChartContext context = chart.getChartContext();
        long dataOffsetX = context.getDataOffsetX();
        long newDataX = timeline.getTimestamp(newIndex);
        long newOffsetX = (long)context.getViewWidth(newDataX - dataOffsetX);

        long offsetX = chart.getOffsetX();
        long viewWidth = context.getViewportWidth();
        if (newOffsetX >= offsetX + SCROLL_MARGIN_LEFT &&
            newOffsetX <= offsetX + viewWidth - SCROLL_MARGIN_RIGHT) return;

        long oldDataX = oldIndex == -1 ? -1 : timeline.getTimestamp(oldIndex);
        long oldOffsetX = oldIndex == -1 ? -1 : (long)context.getViewWidth(oldDataX - dataOffsetX);

        if (oldOffsetX > newOffsetX) {
            chart.setOffset(newOffsetX - SCROLL_MARGIN_LEFT, chart.getOffsetY());
        } else {
            chart.setOffset(newOffsetX - context.getViewportWidth() + SCROLL_MARGIN_RIGHT, chart.getOffsetY());
        }

        chart.repaintDirtyAccel();
    }


    private List<SynchronousXYItem> getSelectedItems() {
        List<TimelineChart.Row> selectedRows = chart.getSelectedRows();
        List<SynchronousXYItem> selectedItems = new ArrayList();
        for (TimelineChart.Row selectedRow : selectedRows)
            selectedItems.addAll(Arrays.asList(selectedRow.getItems()));
        return selectedItems;
    }

    private List<ValueItemDescriptor> getSelectedDescriptors() {
        List<TimelineChart.Row> selectedRows = chart.getSelectedRows();
        List selectedDescriptors = new ArrayList();
        for (TimelineChart.Row selectedRow : selectedRows)
            selectedDescriptors.addAll(Arrays.asList(getProbe(selectedRow).getItemDescriptors()));
        return selectedDescriptors;
    }


    // --- General selection support -------------------------------------------

    public void addSelectionListener(SelectionListener listener) {
        selectionListeners.add(listener);
    }

    public void removeSelectionListener(SelectionListener listener) {
        selectionListeners.remove(listener);
    }

    private void notifyRowSelectionChanged() {
        for (SelectionListener selectionListener: selectionListeners)
            selectionListener.rowSelectionChanged();
    }

    private void notifyTimeSelectionChanged() {
        for (SelectionListener selectionListener: selectionListeners)
            selectionListener.timeSelectionChanged();
    }


    public static interface SelectionListener {

        public void rowSelectionChanged();

        public void timeSelectionChanged();

    }

    public static interface DescriptorResolver {

        public TracerProbeDescriptor getDescriptor(TracerProbe p);

    }

}
