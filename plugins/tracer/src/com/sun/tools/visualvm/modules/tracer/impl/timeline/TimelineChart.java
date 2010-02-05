/*
 *  Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Sun designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Sun in the LICENSE file that accompanied this code.
 * 
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 * 
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 *  Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 *  CA 95054 USA or visit www.sun.com if you need additional information or
 *  have any questions.
 */

package com.sun.tools.visualvm.modules.tracer.impl.timeline;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.charts.ChartContext;
import org.netbeans.lib.profiler.charts.ChartItem;
import org.netbeans.lib.profiler.charts.ChartItemChange;
import org.netbeans.lib.profiler.charts.ItemPainter;
import org.netbeans.lib.profiler.charts.PaintersModel;
import org.netbeans.lib.profiler.charts.swing.LongRect;
import org.netbeans.lib.profiler.charts.swing.Utils;
import org.netbeans.lib.profiler.charts.xy.synchronous.SynchronousXYChart;
import org.netbeans.lib.profiler.charts.xy.synchronous.SynchronousXYItem;
import org.netbeans.lib.profiler.charts.xy.synchronous.SynchronousXYItemsModel;

/**
 *
 * @author Jiri Sedlacek
 */
public class TimelineChart extends SynchronousXYChart {

    public static final int MIN_ROW_HEIGHT = 25;
    public static final int MAX_ROW_HEIGHT = 500;
    public static final int DEF_ROW_HEIGHT = 75;
    public static final int ROW_RESIZE_STEP = MIN_ROW_HEIGHT;

    private final List<Row> rows;
    private final Map<ChartItem, Row> itemsToRows;

    private int selectedRow = -1;

    private final Set<RowListener> rowListeners = new HashSet();


    // --- Constructors --------------------------------------------------------

    public TimelineChart(SynchronousXYItemsModel itemsModel) {
        super(itemsModel, new PaintersModel.Default());

        rows = new ArrayList();
        itemsToRows = new HashMap();

        setBottomBased(false);

        setZoomMode(ZOOM_X);
        setMouseZoomingEnabled(true);
        setMousePanningEnabled(false);

        setAccelerationPriority(1f);
    }


    // --- Rows management -----------------------------------------------------

    public Row addRow() {
        Row row = new Row();
        int rowIndex = rows.size();
        row.setIndex(rowIndex);
        rows.add(row);
        row.setHeight(DEF_ROW_HEIGHT, true);
        row.updateOffset();
//        repaintRows(row.getIndex());
        updateChart();
        notifyRowAdded(row);
        return row;
    }

    public Row addRow(int rowIndex) {
        Row row = new Row();
        row.setIndex(rowIndex);
        rows.add(rowIndex, row);
        row.setHeight(DEF_ROW_HEIGHT, true);
        updateRowOffsets(rowIndex);
        updateRowIndexes(rowIndex + 1);
//        repaintRows(rowIndex);
        updateChart();
        notifyRowAdded(row);
        return row;
    }

    public Row removeRow(int rowIndex) {
        return removeRow(rows.get(rowIndex));
    }

    public Row removeRow(Row row) {
//        System.err.println(">>> Removing row, current bounds: " + dataBounds + ", current rows: " + rows.size());
        row.clearItems();
        rows.remove(row);
        int rowIndex = row.getIndex();
        updateRowIndexes(rowIndex);
        updateRowOffsets(rowIndex);
//        repaintRows(row.getIndex());
        updateChart();
        notifyRowRemoved(row);
//        System.err.println(">>> Removed row, current bounds: " + dataBounds + ", current rows: " + rows.size());
        return row;
    }


    // --- Rows access ---------------------------------------------------------

    public int getRowsCount() {
        return rows.size();
    }

    public Row getRow(int rowIndex) {
        return rows.get(rowIndex);
    }

    public Row getRow(ChartItem item) {
        return itemsToRows.get(item);
    }


    // --- Row appearance ------------------------------------------------------

    public void setRowHeight(int rowIndex, int rowHeight) {
        setRowHeight(rowIndex, rowHeight, true);
    }

    public void setRowHeight(int rowIndex, int rowHeight, boolean checkStep) {
        rows.get(rowIndex).setHeight(rowHeight, checkStep);
        updateRowOffsets(rowIndex + 1);
        updateChart(); // TODO: update only affected rows!
    }

    public int getRowHeight(int rowIndex) {
        return rows.get(rowIndex).getHeight();
    }

    public Row getRowAt(int ypos) {
        ypos += getOffsetY();
        for (Row row : rows) {
            int pos = row.getOffset();
            if (ypos < pos) return null;
            pos += row.getHeight();
            if (ypos <= pos) return row;
        }
        return null;
    }

    public Row getNearestRow(int ypos, int range, boolean noFirst) {
        ypos += getOffsetY();

        if (noFirst) {
            Row row = rows.get(0);
            int pos = row.getOffset() + row.getHeight();
            if (ypos < pos - range) return null;
        }

        for (Row row : rows) {
            int pos = row.getOffset();
            if (ypos < pos - range) return null;
            if (ypos <= pos + range) return row;
            pos += row.getHeight();
            if (ypos < pos - range) return null;
            if (ypos <= pos + range) return row;
        }
        return null;
    }

    private void updateRowOffsets(int rowIndex) {
        int rowsCount = rows.size();
        if (rowIndex >= rowsCount) return;
        for (int i = rowIndex; i < rowsCount; i++)
            rows.get(i).updateOffset();
    }


    // --- Row events ----------------------------------------------------------

    public void addRowListener(RowListener listener) {
        rowListeners.add(listener);
    }

    public void removeRowListener(RowListener listener) {
        rowListeners.remove(listener);
    }


    private void notifyRowAdded(final Row row) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                for (RowListener listener : rowListeners)
                    listener.rowAdded(row);
            }
        });
    }

    private void notifyRowRemoved(final Row row) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                for (RowListener listener : rowListeners)
                    listener.rowRemoved(row);
            }
        });
    }


    // --- Selection support ---------------------------------------------------

    public void setSelectedRow(int rowIndex) {
        if (selectedRow == rowIndex) return;
        selectedRow = rowIndex;
        repaintRows();
    }

    public int getSelectedRow() {
        return selectedRow;
    }

    public void clearSelection() {
        setSelectedRow(-1);
    }


    // --- Protected implementation --------------------------------------------

    protected ChartContext getChartContext(ChartItem item) {
        if (item == null) return super.getChartContext(null);
        else return itemsToRows.get(item).getContext();
    }

    protected void computeDataBounds() {
        LongRect.clear(dataBounds);

        if (rows == null) return;

        for (Row row : rows) {
            RowContext context = (RowContext)row.getContext();
            if (LongRect.isClear(dataBounds)) LongRect.set(dataBounds, context.bounds);
            else LongRect.add(dataBounds, context.bounds);
        }

        dataBounds.y = 0;
        Row lastRow = rows.size() > 0 ? rows.get(rows.size() - 1) : null;
        dataBounds.height = lastRow != null ? lastRow.getOffset() + lastRow.getHeight() : 0;
    }

    protected void updateChart() {
        updateRowBounds();
        super.updateChart();
    }


    protected void itemsAdded(List<ChartItem> addedItems) {
        updateRowBounds();
        super.itemsAdded(addedItems);
    }

    protected void itemsRemoved(List<ChartItem> removedItems) {
        updateRowBounds();
        super.itemsRemoved(removedItems);
    }

    protected void itemsChanged(List<ChartItemChange> itemChanges) {
        updateRowBounds(); // NOTE: should be computed from itemChanges!!!
        super.itemsChanged(itemChanges);
    }

    protected void paintersChanged(List<ItemPainter> changedPainters) {
        updateRowBounds();
        super.paintersChanged(changedPainters);
    }


    // --- Internal implementation ---------------------------------------------

    void addItemsImpl(SynchronousXYItem[] addedItems, ItemPainter[] addedPainters, Row row) {
        for (SynchronousXYItem item : addedItems) itemsToRows.put(item, row);
        paintersModel().addPainters(addedItems, addedPainters);
        itemsModel().addItems(addedItems);
    }

    void removeItemsImpl(SynchronousXYItem[] removedItems) {
        itemsModel().removeItems(removedItems);
        paintersModel().removePainters(removedItems);
        for (SynchronousXYItem item : removedItems) itemsToRows.remove(item);
    }


    // --- Private implementation ----------------------------------------------

    private SynchronousXYItemsModel itemsModel() {
        return (SynchronousXYItemsModel)getItemsModel();
    }

    private PaintersModel.Default paintersModel() {
        return (PaintersModel.Default)getPaintersModel();
    }

    private void updateRowIndexes(int startIndex) {
        for (int i = startIndex; i < rows.size(); i++)
            rows.get(i).setIndex(i);
    }

    private void repaintRows() {
        invalidateImage();
        repaintDirty();
    }

    private void repaintRows(final int startIndex) {
//        SwingUtilities.invokeLater(new Runnable() {
//            public void run() {
                for (int i = startIndex; i < rows.size(); i++) {
                    ChartContext rowContext = rows.get(i).getContext();
                    invalidateImage(new Rectangle(0, Utils.checkedInt(rowContext.
                                   getViewportOffsetY()), getWidth(), rowContext.
                                   getViewportHeight()));
                    System.err.println(">>> Invalidating: " + new Rectangle(0, Utils.checkedInt(rowContext.
                                   getViewportOffsetY()), getWidth(), rowContext.
                                   getViewportHeight()));
                }
                repaintDirty();
//            }
//        });
    }

    private void updateRowBounds() {
        if (rows == null) return; // Happens when called from constructor
        for (Row row : rows) ((RowContext)row.getContext()).updateBounds();
    }


    // --- Row definition ------------------------------------------------------

    public class Row {

        private int rowIndex;
        private int rowOffset;
        private int rowHeight;
        private final List<SynchronousXYItem> items;
        private final RowContext context;


        // --- Constructors ----------------------------------------------------

        Row() {
            items = new ArrayList();
            context = new RowContext(this);
        }


        // --- Row telemetry ---------------------------------------------------

        public int getIndex() {
            return rowIndex;
        }

        private void updateOffset() {
            if (rowIndex != 0) {
                Row previousRow = rows.get(rowIndex - 1);
                rowOffset = previousRow.rowOffset + previousRow.rowHeight;
            } else {
                rowOffset = 0;
            }
        }

        public int getOffset() {
            return rowOffset;
        }

        private void setHeight(int height, boolean checkStep) {
            height = Math.max(MIN_ROW_HEIGHT, height);
            height = Math.min(MAX_ROW_HEIGHT, height);
            if (checkStep) height = height / ROW_RESIZE_STEP * ROW_RESIZE_STEP;
            rowHeight = height;
        }

        public int getHeight() {
            return rowHeight;
        }


        // --- Items management ------------------------------------------------

        public void addItems(SynchronousXYItemsModel addedItems, PaintersModel addedPainters) {
            int itemsCount = addedItems.getItemsCount();

            SynchronousXYItem[] addedItemsArr = new SynchronousXYItem[itemsCount];
            for (int i = 0; i < itemsCount; i++)
                addedItemsArr[i] = addedItems.getItem(i);

            ItemPainter[] addedPaintersArr = new ItemPainter[itemsCount];
            for (int i = 0; i < itemsCount; i++)
                addedPaintersArr[i] = addedPainters.getPainter(addedItemsArr[i]);

            addItems(addedItemsArr, addedPaintersArr);
        }

        public void addItems(SynchronousXYItem[] addedItems, ItemPainter[] addedPainters) {
            for (SynchronousXYItem item : addedItems) items.add(item);
            addItemsImpl(addedItems, addedPainters, this);
        }

        public void removeItems(SynchronousXYItemsModel removedItems) {
            int itemsCount = removedItems.getItemsCount();

            SynchronousXYItem[] removedItemsArr = new SynchronousXYItem[itemsCount];
            for (int i = 0; i < itemsCount; i++)
                removedItemsArr[i] = removedItems.getItem(i);

            removeItems(removedItemsArr);
        }

        public void removeItems(SynchronousXYItem[] removedItems) {
            removeItemsImpl(removedItems);
            for (SynchronousXYItem item : removedItems) items.remove(item);
        }


        // --- Items access ----------------------------------------------------

        public int getItemsCount() {
            return items.size();
        }

        public ChartItem getItem(int itemIndex) {
            return items.get(itemIndex);
        }

        public SynchronousXYItem[] getItems() {
            return items.toArray(new SynchronousXYItem[items.size()]);
        }


        // --- Row context -----------------------------------------------------

        public ChartContext getContext() {
            return context;
        }


        // --- Internal interface ----------------------------------------------

        void setIndex(int rowIndex) {
            this.rowIndex = rowIndex;
        }

        void clearItems() {
            if (items.size() == 0) return;
            removeItemsImpl(getItems());
        }

    }


    // --- RowContext implementation -------------------------------------------

    private class RowContext extends SynchronousXYChart.Context {

        private final Row row;

        private final LongRect bounds;
        private double scaleY;

        private int marginTop;
        private int marginBottom;


        RowContext(Row row) {
            super(TimelineChart.this);
            this.row = row;

            marginTop = 3;
            marginBottom = 3;

            bounds = new LongRect();
        }


        protected void updateBounds() {
            LongRect.clear(bounds);

            PaintersModel painters = paintersModel();
            int itemsCount = row.getItemsCount();

            for (int i = 0; i < itemsCount; i++) {
                ChartItem item = row.getItem(i);
                ItemPainter painter = painters.getPainter(item);
                if (LongRect.isClear(bounds))
                    LongRect.set(bounds, painter.getItemBounds(item));
                else
                    LongRect.add(bounds, painter.getItemBounds(item));
            }

            double oldScaleY = scaleY;
            scaleY = (double)(row.getHeight() - marginTop - marginBottom) /
                     (double)bounds.height;
            if (scaleY != oldScaleY) invalidateImage(Utils.checkedRectangle(
                                                     getViewRect(bounds)));
        }


        public boolean isBottomBased() { return true; }

        public boolean fitsHeight() { return true; }

        public long getDataOffsetY() { return bounds.y; }

        public long getDataHeight() { return bounds.height; }

        public long getViewHeight() { return row.getHeight(); }

        public long getViewportOffsetY() { return row.getOffset() - getOffsetY(); }

        public int getViewportHeight() { return row.getHeight(); }

        public double getViewY(double dataY) { return getViewY(dataY, false); }

        public double getReversedViewY(double dataY) { return getViewY(dataY, true); }

        public double getViewHeight(double dataHeight) { return dataHeight * scaleY; }

        public double getDataY(double viewY) { return getDataY(viewY, false); }

        public double getReversedDataY(double viewY) { return getDataY(viewY, true); }

        public double getDataHeight(double viewHeight) { return viewHeight / scaleY; }


        private double getViewY(double dataY, boolean reverse) {
            if (isBottomBased() && !reverse || !isBottomBased() && reverse) {
                return row.getHeight() - (dataY - bounds.y) * scaleY - getOffsetY() +
                       getViewInsets().top - marginBottom + row.getOffset();
            } else {
                return (dataY - bounds.y) * scaleY - getOffsetY() +
                       getViewInsets().top + marginTop + row.getOffset();
            }
        }

        private double getDataY(double viewY, boolean reverse) {
            if ((isBottomBased() && !reverse) || (!isBottomBased() && reverse)) {
                return bounds.y - (viewY + getViewInsets().bottom -
                       getOffsetY() - getHeight()) / scaleY;
            } else {
                return (viewY + getOffsetY() - getViewInsets().top) /
                       scaleY + bounds.y;
            }
        }

    }


    public static interface RowListener {

        public void rowAdded(Row row);

        public void rowRemoved(Row row);

    }

}
