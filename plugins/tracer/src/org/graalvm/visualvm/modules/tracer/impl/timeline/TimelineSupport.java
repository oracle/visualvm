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

package org.graalvm.visualvm.modules.tracer.impl.timeline;

import org.graalvm.visualvm.modules.tracer.ItemValueFormatter;
import org.graalvm.visualvm.modules.tracer.ProbeItemDescriptor;
import org.graalvm.visualvm.modules.tracer.TracerProbe;
import org.graalvm.visualvm.modules.tracer.TracerProbeDescriptor;
import org.graalvm.visualvm.modules.tracer.impl.options.TracerOptions;
import org.graalvm.visualvm.modules.tracer.impl.details.DetailsPanel;
import org.graalvm.visualvm.modules.tracer.impl.details.DetailsTableModel;
import org.graalvm.visualvm.modules.tracer.impl.export.DataExport;
import org.graalvm.visualvm.modules.tracer.impl.timeline.TimelineChart.Row;
import org.graalvm.visualvm.modules.tracer.impl.timeline.items.ValueItemDescriptor;
import java.awt.Color;
import java.text.Format;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import org.graalvm.visualvm.lib.charts.ChartContext;
import org.graalvm.visualvm.lib.charts.ChartSelectionModel;
import org.graalvm.visualvm.lib.charts.ItemSelection;
import org.graalvm.visualvm.lib.charts.PaintersModel;
import org.graalvm.visualvm.lib.charts.Timeline;
import org.graalvm.visualvm.lib.charts.axis.TimeAxisUtils;
import org.graalvm.visualvm.lib.charts.xy.XYItemPainter;
import org.graalvm.visualvm.lib.charts.xy.XYItemSelection;
import org.graalvm.visualvm.lib.charts.xy.synchronous.SynchronousXYItem;
import org.graalvm.visualvm.lib.charts.xy.synchronous.SynchronousXYItemsModel;

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

    private final PointsComputer pointsComputer;

    private final TimelineTooltipOverlay tooltips;
    private final TimelineLegendOverlay legend;
    private final TimelineUnitsOverlay units;

    private final List<TracerProbe> probes = new ArrayList();
    private final List<TimelineChart.Row> rows = new ArrayList();
    private final DescriptorResolver descriptorResolver;

    private final Set<ValuesListener> valuesListeners = new HashSet();

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

        pointsComputer = new PointsComputer();

        legend = new TimelineLegendOverlay(chart);
        legend.setVisible(TracerOptions.getInstance().isShowLegendEnabled());
        chart.addOverlayComponent(legend);

        units = new TimelineUnitsOverlay(chart);
        units.setVisible(TracerOptions.getInstance().isShowValuesEnabled());
        chart.addOverlayComponent(units);
    }


    // --- Chart access --------------------------------------------------------

    TimelineChart getChart() {
        return chart;
    }

    public ChartSelectionModel getChartSelectionModel() {
        return chart.getSelectionModel();
    }


    // --- Indexes computer access ---------------------------------------------

    PointsComputer getPointsComputer() {
        return pointsComputer;
    }


    // --- Overlays access -----------------------------------------------------

    public void setShowValuesEnabled(boolean enabled) {
        units.setVisible(enabled);
    }

    public boolean isShowValuesEnabled() {
        return units.isVisible();
    }

    public void setShowLegendEnabled(boolean enabled) {
        legend.setVisible(enabled);
    }

    public boolean isShowLegendEnabled() {
        return legend.isVisible();
    }


    // --- Probes management ---------------------------------------------------

    public void addProbe(final TracerProbe probe) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                resetValues();

                TimelineChart.Row row = chart.addRow();

                probes.add(probe);
                rows.add(row);

                ProbeItemDescriptor[] itemDescriptors = probe.getItemDescriptors();
                TimelineXYItem[] items = model.createItems(itemDescriptors);
                XYItemPainter[] painters  = new XYItemPainter[items.length];
                for (int i = 0; i < painters.length; i++)
                    painters[i] = TimelinePaintersFactory.createPainter(
                            itemDescriptors[i], i, pointsComputer);
                
                row.addItems(items, painters);

                setupOverlays();
            }
        });
    }

    public void removeProbe(final TracerProbe probe) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                resetValues();
                
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

    public boolean hasData() {
        return model.getTimestampsCount() > 0;
    }


    // --- Tooltips support ----------------------------------------------------

    private void setupOverlays() {
        final int rowsCount = chart.getRowsCount();

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

        units.setupModel(new TimelineUnitsOverlay.Model() {

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
                            visibleRowItemColors.add(painter.getDefiningColor());

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
        int newRow = detailsModel == null ? -1 : detailsModel.getRowCount();
        model.addValues(timestamp, newValues);
        itemsModel.valuesAdded();
        if (newRow != -1) detailsModel.fireTableRowsInserted(newRow, newRow);
        fireValuesAdded();
    }

    public void resetValues() {
        model.reset();
        itemsModel.valuesReset();
        resetSelectedTimestamps();
        pointsComputer.reset();
        if (detailsModel != null) detailsModel.fireTableStructureChanged();
        fireValuesReset();
    }

    public void exportAllValues(String title) {
        final int rowsCount = model.getTimestampsCount();
        final int columnsCount = model.getItemsCount();
        
        final Format timeFormatter = new SimpleDateFormat(MessageFormat.format(
                                     TimeAxisUtils.TIME_DATE_FORMAT, new Object[] {
                                     TimeAxisUtils.TIME_MSEC, TimeAxisUtils.DATE_YEAR}));

        final List<ProbeItemDescriptor> probeDescriptors = new ArrayList(columnsCount);
        for (TracerProbe probe : probes)
            probeDescriptors.addAll(Arrays.asList(probe.getItemDescriptors()));
        final ValueItemDescriptor[] descriptors = new ValueItemDescriptor[columnsCount];
        for (int i = 0; i < columnsCount; i++)
            descriptors[i] = (ValueItemDescriptor)probeDescriptors.get(i);

        TableModel exportModel = new AbstractTableModel() {
            public int getRowCount() {
                return rowsCount;
            }

            public int getColumnCount() {
                return columnsCount + 1;
            }

            public String getColumnName(int columnIndex) {
                if (columnIndex == 0) return "Time [ms]";

                String unitsString = descriptors[columnIndex - 1].getUnitsString(
                                     ItemValueFormatter.FORMAT_EXPORT);
                unitsString = unitsString == null ? "" : " [" + unitsString + "]";
                return itemsModel.getItem(columnIndex - 1).getName() + unitsString;
            }

            public Object getValueAt(int rowIndex, int columnIndex) {
                if (columnIndex == 0) return timeFormatter.format(model.
                                             getTimestamp(rowIndex));

                long value = itemsModel.getItem(columnIndex - 1).getYValue(rowIndex);
                return descriptors[columnIndex - 1].getValueString(value,
                                                    ItemValueFormatter.FORMAT_EXPORT);
            }
        };
        DataExport.exportData(exportModel, title);
    }

    public void exportDetailsValues(String title) {
        if (detailsModel == null) return;

        final int rowsCount = detailsModel.getRowCount();
        final int columnsCount = detailsModel.getColumnCount();

        final Format timeFormatter = new SimpleDateFormat(MessageFormat.format(
                                     TimeAxisUtils.TIME_DATE_FORMAT, new Object[] {
                                     TimeAxisUtils.TIME_MSEC, TimeAxisUtils.DATE_YEAR}));
        
        TableModel exportModel = new AbstractTableModel() {
            public int getRowCount() {
                return rowsCount;
            }

            public int getColumnCount() {
                return columnsCount - 1;
            }

            public String getColumnName(int columnIndex) {
                return detailsModel.getColumnName(columnIndex + 1);
            }

            public Object getValueAt(int rowIndex, int columnIndex) {
                Object value = detailsModel.getValueAt(rowIndex, columnIndex + 1);

                if (columnIndex == 0) return timeFormatter.format(value);

                return detailsModel.getDescriptor(columnIndex + 1).getValueString(
                                    (Long)value, ItemValueFormatter.FORMAT_EXPORT);
            }
        };
        DataExport.exportData(exportModel, title);
    }

    public void addValuesListener(ValuesListener listener) {
        valuesListeners.add(listener);
    }

    public void removeValuesListener(ValuesListener listener) {
        valuesListeners.remove(listener);
    }
    
    private void fireValuesAdded() {
        for (ValuesListener listener : valuesListeners)
            listener.valuesAdded();
    }
    
    private void fireValuesReset() {
        for (ValuesListener listener : valuesListeners)
            listener.valuesReset();
    }


    public static interface ValuesListener {

        public void valuesAdded();

        public void valuesReset();

    }

    // --- Row selection management --------------------------------------------

    private DetailsTableModel detailsModel;

    void rowSelectionChanged() {
        updateSelectedItems();
        notifyRowSelectionChanged();
    }

    public boolean isRowSelection() {
        return chart.isRowSelection();
    }

    public TableModel getDetailsModel() {
        if (!chart.isRowSelection()) detailsModel = null;
        else detailsModel = createSelectionModel();
        return detailsModel;
    }

    private DetailsTableModel createSelectionModel() {
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
            unitsString = unitsString == null ? "" : " [" + unitsString + "]";
            columnNames[i] = itemName + unitsString;
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
                if (Boolean.TRUE.equals(aValue)) selectTimestamp(rowIndex, true, false);
                else unselectTimestamp(rowIndex, false);
            }

        };
    }


    // --- Time selection management -------------------------------------------

    private static final int SCROLL_MARGIN_LEFT = 10;
    private static final int SCROLL_MARGIN_RIGHT = 50;

    private boolean hovering;
    private boolean hoveredSelected;

    
    void setTimestampHovering(boolean hovering, boolean hoveredSelected) {
        this.hovering = hovering;
        this.hoveredSelected = hoveredSelected;
        notifyTimeSelectionChanged();
    }

    public void selectTimestamp(int index, boolean scrollToVisible) {
        selectTimestamp(index, scrollToVisible, true);
    }

    private void selectTimestamp(int index, boolean scrollToVisible, boolean notifyTable) {
        boolean change = selectedTimestamps.add(index);
        if (notifyTable && detailsModel != null)
            detailsModel.fireTableCellUpdated(index, 0);
        if (change) {
            updateSelectedItems();
            notifyTimeSelectionChanged();
            if (scrollToVisible) highlightTimestamp(index);
        }
    }

    public void unselectTimestamp(int index) {
        unselectTimestamp(index, true);
    }

    public void toggleTimestampSelection(int index) {
        if (!selectedTimestamps.contains(index)) selectTimestamp(index, false);
        else unselectTimestamp(index);
    }

    public boolean isTimestampSelected(int index) {
        return selectedTimestamps.contains(index);
    }

    public boolean isTimestampSelection(boolean includeHover) {
        int selectedTimestampsCount = selectedTimestamps.size();
        if (selectedTimestampsCount == 0) return false;
        if (selectedTimestampsCount > 1)  return true;
        return (includeHover || !hovering || hoveredSelected);
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
        if (detailsModel != null) detailsModel.fireTableDataChanged();
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

    private void highlightTimestamp(int selectedIndex) {
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


    public void scrollChartToIndex(int index) {
        scrollChartToSelection(-1, index);
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

        if (oldIndex == -1) {
            chart.setOffset(newOffsetX - context.getViewportWidth() / 2, chart.getOffsetY());
        } else if (oldOffsetX > newOffsetX) {
            chart.setOffset(newOffsetX - SCROLL_MARGIN_LEFT, chart.getOffsetY());
        } else {
            chart.setOffset(newOffsetX - context.getViewportWidth() + SCROLL_MARGIN_RIGHT, chart.getOffsetY());
        }

        chart.repaintDirty();
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
        boolean rowsSelected = chart.isRowSelection();
        for (SelectionListener selectionListener: selectionListeners)
            selectionListener.rowSelectionChanged(rowsSelected);
    }

    private void notifyTimeSelectionChanged() {
        boolean sel = isTimestampSelection(true);
        boolean hov = sel && !isTimestampSelection(false);
        for (SelectionListener selectionListener: selectionListeners)
            selectionListener.timeSelectionChanged(sel, hov);
    }


    public static interface SelectionListener {

        public void rowSelectionChanged(boolean rowsSelected);

        public void timeSelectionChanged(boolean timestampsSelected,
                                         boolean justHovering);

    }

    public static interface DescriptorResolver {

        public TracerProbeDescriptor getDescriptor(TracerProbe p);

    }

}
