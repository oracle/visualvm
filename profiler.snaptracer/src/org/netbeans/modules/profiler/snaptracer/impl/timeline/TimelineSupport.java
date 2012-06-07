/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2007-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.netbeans.modules.profiler.snaptracer.impl.timeline;

import org.netbeans.modules.profiler.snaptracer.impl.timeline.TimelineChart.Row;
import org.netbeans.modules.profiler.snaptracer.impl.timeline.items.ValueItemDescriptor;
import java.awt.Color;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
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
import org.netbeans.lib.profiler.charts.ChartContext;
import org.netbeans.lib.profiler.charts.ChartSelectionModel;
import org.netbeans.lib.profiler.charts.ItemSelection;
import org.netbeans.lib.profiler.charts.PaintersModel;
import org.netbeans.lib.profiler.charts.Timeline;
import org.netbeans.lib.profiler.charts.axis.TimeAxisUtils;
import org.netbeans.lib.profiler.charts.xy.XYItemPainter;
import org.netbeans.lib.profiler.charts.xy.XYItemSelection;
import org.netbeans.lib.profiler.charts.xy.synchronous.SynchronousXYItem;
import org.netbeans.lib.profiler.charts.xy.synchronous.SynchronousXYItemsModel;
import org.netbeans.modules.profiler.snaptracer.ItemValueFormatter;
import org.netbeans.modules.profiler.snaptracer.ProbeItemDescriptor;
import org.netbeans.modules.profiler.snaptracer.TracerProbe;
import org.netbeans.modules.profiler.snaptracer.TracerProbeDescriptor;
import org.netbeans.modules.profiler.snaptracer.impl.IdeSnapshot;
import org.netbeans.modules.profiler.snaptracer.impl.details.DetailsPanel;
import org.netbeans.modules.profiler.snaptracer.impl.details.DetailsTableModel;
import org.netbeans.modules.profiler.snaptracer.impl.export.DataExport;
import org.netbeans.modules.profiler.snaptracer.impl.options.TracerOptions;

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
    private final List<Integer> selectedIntervals = new ArrayList();
    private final Set<SelectionListener> selectionListeners = new HashSet();

    private final IdeSnapshot snapshot;


    // --- Constructor ---------------------------------------------------------

    public TimelineSupport(DescriptorResolver descriptorResolver, IdeSnapshot snapshot) {
        this.descriptorResolver = descriptorResolver;
        this.snapshot = snapshot;
        
        // TODO: must be called in EDT!
        model = new TimelineModel();
        itemsModel = new SynchronousXYItemsModel(model);
        chart = new TimelineChart(itemsModel);

        TimelineSelectionManager selectionManager = new TimelineSelectionManager();
        chart.setSelectionModel(selectionManager);
        selectionManager.registerChart(chart);

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


    // --- Indexes computer access ---------------------------------------------

    PointsComputer getPointsComputer() {
        return pointsComputer;
    }


    // --- Chart setup ---------------------------------------------------------

    private ComponentListener chartResizeHandler;

    public void dataLoadingStarted(final long range) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                cleanResizeHandler();
                chartResizeHandler = new ComponentAdapter() {
                    public void componentResized(ComponentEvent e) {
                        chart.setScale(chart.getWidth() / (double)range, 1);
                    }
                };
                chart.addComponentListener(chartResizeHandler);
                chart.setFitsWidth(false);
                chartResizeHandler.componentResized(null);
            }
        });
    }

    public void dataLoadingFinished() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                cleanResizeHandler();
                chart.setFitsWidth(true);
                chart.invalidateRepaint();
            }
        });
    }

    private void cleanResizeHandler() {
        if (chartResizeHandler != null) {
            chart.removeComponentListener(chartResizeHandler);
            chartResizeHandler = null;
        }
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
                            itemDescriptors[i], i, pointsComputer, snapshot);
                
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

    public long getTimestamp(int index) {
        return model.getTimestamp(index);
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
    
    public void selectInterval(int index1, int index2) {
        selectedIntervals.add(index1);
        selectedIntervals.add(index2);
    }
    
    public List<Integer> getSelectedIntervals() {
        return selectedIntervals;
    }
    
    public void resetSelectedIntervals() {
        selectedIntervals.clear();
    }
    
    public void selectedIntervalsChanged() {
        notifyIntervalsSelectionChanged();
    }

    private void highlightTimestamp(int selectedIndex) {
        ChartSelectionModel selectionModel = chart.getSelectionModel();
        List<ItemSelection> oldSelection = selectionModel.getHighlightedItems();
        int oldSelectedIndex = -1;
        if (!oldSelection.isEmpty()) {
            XYItemSelection sel = (XYItemSelection)oldSelection.get(0);
            oldSelectedIndex = sel.getValueIndex();
        }

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


    // --- Bounds selection management -----------------------------------------

    private int startIndex = -1;
    private int endIndex = -1;

    public void selectAll() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                TimelineSelectionManager selection = (TimelineSelectionManager)chart.getSelectionModel();
                selection.selectAll();
                startIndex = selection.getStartIndex();
                endIndex = selection.getEndIndex();
                notifyIndexSelectionChanged();
            }
        });
    }

    public boolean isSelectAll() {
        return endIndex - startIndex == model.getTimestampsCount() - 1;
    }

    public int getStartIndex() { return startIndex; }

    public int getEndIndex() { return endIndex; }


    void indexSelectionChanged(int startIndex, int endIndex) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        notifyIndexSelectionChanged();
    }


    // --- General selection support -------------------------------------------

    public void addSelectionListener(SelectionListener listener) {
        selectionListeners.add(listener);
    }

    public void removeSelectionListener(SelectionListener listener) {
        selectionListeners.remove(listener);
    }
    
    private void notifyIntervalsSelectionChanged() {
        for (SelectionListener selectionListener : selectionListeners)
            selectionListener.intervalsSelectionChanged();
    }

    private void notifyIndexSelectionChanged() {
        for (SelectionListener selectionListener : selectionListeners)
            selectionListener.indexSelectionChanged();
    }

    private void notifyTimeSelectionChanged() {
        boolean sel = isTimestampSelection(true);
        boolean hov = sel && !isTimestampSelection(false);
        for (SelectionListener selectionListener : selectionListeners)
            selectionListener.timeSelectionChanged(sel, hov);
    }


    public static interface SelectionListener {
        
        public void intervalsSelectionChanged();

        public void indexSelectionChanged();

        public void timeSelectionChanged(boolean timestampsSelected,
                                         boolean justHovering);

    }

    public static interface DescriptorResolver {

        public TracerProbeDescriptor getDescriptor(TracerProbe p);

    }

}
