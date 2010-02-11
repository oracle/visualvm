/*
 *  Copyright 2007-2010 Sun Microsystems, Inc.  All Rights Reserved.
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
final class TimelineChart extends SynchronousXYChart {

    static final int MIN_ROW_HEIGHT = 25;
    static final int MAX_ROW_HEIGHT = 500;
    static final int DEF_ROW_HEIGHT = 75;
    static final int ROW_RESIZE_STEP = MIN_ROW_HEIGHT;

    private int currentRowHeight = DEF_ROW_HEIGHT;

    private final List<Row> rows;
    private final Map<ChartItem, Row> itemsToRows;

    private int selectedRow = -1;

    private final Set<RowListener> rowListeners = new HashSet();


    // --- Constructors --------------------------------------------------------

    TimelineChart(SynchronousXYItemsModel itemsModel) {
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

    Row addRow() {
        Row row = new Row();
        int rowIndex = rows.size();
        row.setIndex(rowIndex);
        rows.add(row);
        row.setHeight(currentRowHeight, true);
        row.updateOffset();
//        repaintRows(row.getIndex());
        updateChart();
        notifyRowAdded(row);
        return row;
    }

    Row addRow(int rowIndex) {
        Row row = new Row();
        row.setIndex(rowIndex);
        rows.add(rowIndex, row);
        row.setHeight(currentRowHeight, true);
        updateRowOffsets(rowIndex);
        updateRowIndexes(rowIndex + 1);
//        repaintRows(rowIndex);
        updateChart();
        notifyRowAdded(row);
        return row;
    }

    Row removeRow(int rowIndex) {
        return removeRow(rows.get(rowIndex));
    }

    Row removeRow(Row row) {
        row.clearItems();
        rows.remove(row);
        int rowIndex = row.getIndex();
        updateRowIndexes(rowIndex);
        updateRowOffsets(rowIndex);
//        repaintRows(row.getIndex());
        updateChart();
        notifyRowRemoved(row);
        return row;
    }


    // --- Rows access ---------------------------------------------------------

    boolean hasRows() {
        return !rows.isEmpty();
    }

    int getRowsCount() {
        return rows.size();
    }

    Row getRow(int rowIndex) {
        return rows.get(rowIndex);
    }

    Row getRow(ChartItem item) {
        return itemsToRows.get(item);
    }


    // --- Row appearance ------------------------------------------------------

    void setRowHeight(int rowIndex, int rowHeight) {
        setRowHeight(rowIndex, rowHeight, true);
    }

    void setRowHeight(int rowIndex, int rowHeight, boolean checkStep) {
        rows.get(rowIndex).setHeight(rowHeight, checkStep);
        updateRowOffsets(rowIndex + 1);
        updateChart(); // TODO: update only affected rows!
    }

    int getRowHeight(int rowIndex) {
        return rows.get(rowIndex).getHeight();
    }

    void increaseRowHeights(boolean step) {
        if (rows.isEmpty()) return;
        int incr = step ? ROW_RESIZE_STEP : 1;
        for (Row row : rows) row.setHeight(row.getHeight() + incr, step);
        updateRowOffsets(0);
        updateChart(); // TODO: update only affected rows!
        currentRowHeight += incr;
    }

    void decreaseRowHeights(boolean step) {
        if (rows.isEmpty()) return;
        int decr = step ? ROW_RESIZE_STEP : 1;
        for (Row row : rows) row.setHeight(row.getHeight() - decr, step);
        updateRowOffsets(0);
        updateChart(); // TODO: update only affected rows!
        currentRowHeight = Math.max(currentRowHeight - decr, MIN_ROW_HEIGHT);
    }

    void resetRowHeights() {
        if (rows.isEmpty()) return;
        for (Row row : rows) row.setHeight(DEF_ROW_HEIGHT, true);
        updateRowOffsets(0);
        updateChart(); // TODO: update only affected rows!
        currentRowHeight = DEF_ROW_HEIGHT;
    }

    Row getRowAt(int ypos) {
        ypos += getOffsetY();
        for (Row row : rows) {
            int pos = row.getOffset();
            if (ypos < pos) return null;
            pos += row.getHeight();
            if (ypos <= pos) return row;
        }
        return null;
    }

    Row getNearestRow(int ypos, int range, boolean noFirst) {
        if (rows.size() == 0) return null;
        
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

    void addRowListener(RowListener listener) {
        rowListeners.add(listener);
    }

    void removeRowListener(RowListener listener) {
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

    void setSelectedRow(int rowIndex) {
        if (selectedRow == rowIndex) return;
        selectedRow = rowIndex;
        repaintRows();
    }

    int getSelectedRow() {
        return selectedRow;
    }

    void clearSelection() {
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


    void invalidateRepaint() {
        invalidateImage();
        repaintDirty();
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

    class Row {

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

        int getIndex() {
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

        int getOffset() {
            return rowOffset;
        }

        private void setHeight(int height, boolean checkStep) {
            height = Math.max(MIN_ROW_HEIGHT, height);
            height = Math.min(MAX_ROW_HEIGHT, height);
            if (checkStep) height = height / ROW_RESIZE_STEP * ROW_RESIZE_STEP;
            rowHeight = height;
        }

        int getHeight() {
            return rowHeight;
        }


        // --- Items management ------------------------------------------------

        void addItems(SynchronousXYItemsModel addedItems, PaintersModel addedPainters) {
            int itemsCount = addedItems.getItemsCount();

            SynchronousXYItem[] addedItemsArr = new SynchronousXYItem[itemsCount];
            for (int i = 0; i < itemsCount; i++)
                addedItemsArr[i] = addedItems.getItem(i);

            ItemPainter[] addedPaintersArr = new ItemPainter[itemsCount];
            for (int i = 0; i < itemsCount; i++)
                addedPaintersArr[i] = addedPainters.getPainter(addedItemsArr[i]);

            addItems(addedItemsArr, addedPaintersArr);
        }

        void addItems(SynchronousXYItem[] addedItems, ItemPainter[] addedPainters) {
            for (SynchronousXYItem item : addedItems) items.add(item);
            addItemsImpl(addedItems, addedPainters, this);
        }

        void removeItems(SynchronousXYItemsModel removedItems) {
            int itemsCount = removedItems.getItemsCount();

            SynchronousXYItem[] removedItemsArr = new SynchronousXYItem[itemsCount];
            for (int i = 0; i < itemsCount; i++)
                removedItemsArr[i] = removedItems.getItem(i);

            removeItems(removedItemsArr);
        }

        void removeItems(SynchronousXYItem[] removedItems) {
            removeItemsImpl(removedItems);
            for (SynchronousXYItem item : removedItems) items.remove(item);
        }


        // --- Items access ----------------------------------------------------

        int getItemsCount() {
            return items.size();
        }

        ChartItem getItem(int itemIndex) {
            return items.get(itemIndex);
        }

        SynchronousXYItem[] getItems() {
            return items.toArray(new SynchronousXYItem[items.size()]);
        }


        // --- Row context -----------------------------------------------------

        ChartContext getContext() {
            return context;
        }


        // --- Internal interface ----------------------------------------------

        private void setIndex(int rowIndex) {
            this.rowIndex = rowIndex;
        }

        private void clearItems() {
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
