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

import java.awt.Rectangle;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.charts.ChartContext;
import org.netbeans.lib.profiler.charts.ChartItem;
import org.netbeans.lib.profiler.charts.ChartItemChange;
import org.netbeans.lib.profiler.charts.ChartSelectionModel;
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

    static final int MIN_ROW_HEIGHT = 20;
    static final int MAX_ROW_HEIGHT = 500;
    static final int DEF_ROW_HEIGHT = 40;
    static final int ROW_RESIZE_STEP = MIN_ROW_HEIGHT;

    private static final int ROW_MARGIN_TOP = 3;
    private static final int ROW_MARGIN_BOTTOM = 3;

    private int currentRowHeight = DEF_ROW_HEIGHT;

    private final List<Row> rows;
    private final Map<ChartItem, Row> itemsToRows;

    private final Set selectedRows = new TreeSet(new RowComparator());
    private final Set selectionBlockers = new HashSet();
    private int lastHoverMode;
    private int lastMoveMode;

    private final Set<RowListener> rowListeners = new HashSet();


    // --- Constructors --------------------------------------------------------

    TimelineChart(SynchronousXYItemsModel itemsModel) {
        super(itemsModel, new PaintersModel.Default());

        rows = new ArrayList();
        itemsToRows = new HashMap();

        setBottomBased(false);

        setZoomMode(ZOOM_X);
        setMouseZoomingEnabled(false);
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
        updateChart();
        notifyRowsAdded(Collections.singletonList(row));
        return row;
    }

    Row addRow(int rowIndex) {
        Row row = new Row();
        row.setIndex(rowIndex);
        rows.add(rowIndex, row);
        row.setHeight(currentRowHeight, true);
        updateRowOffsets(rowIndex);
        updateRowIndexes(rowIndex + 1);
        updateChart();
        notifyRowsAdded(Collections.singletonList(row));
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
        updateChart();
        notifyRowsRemoved(Collections.singletonList(row));
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
        Row row = rows.get(rowIndex);
        boolean changed = row.setHeight(rowHeight, checkStep);
        updateRowOffsets(rowIndex + 1);
        if (changed) notifyRowsResized(Collections.singletonList(row));
        updateChart(); // TODO: update only affected rows!
    }

    int getRowHeight(int rowIndex) {
        return rows.get(rowIndex).getHeight();
    }

    void increaseRowHeights(boolean step) {
        if (rows.isEmpty()) return;
        int incr = step ? ROW_RESIZE_STEP : 1;
        List<Row> resized = new ArrayList(rows.size());
        for (Row row : rows)
            if (row.setHeight(row.getHeight() + incr, step))
                resized.add(row);
        updateRowOffsets(0);
        if (!resized.isEmpty()) notifyRowsResized(resized);
        updateChart(); // TODO: update only affected rows!
        currentRowHeight += incr;
    }

    void decreaseRowHeights(boolean step) {
        if (rows.isEmpty()) return;
        int decr = step ? ROW_RESIZE_STEP : 1;
        List<Row> resized = new ArrayList(rows.size());
        for (Row row : rows)
            if (row.setHeight(row.getHeight() - decr, step))
                resized.add(row);
        updateRowOffsets(0);
        if (!resized.isEmpty()) notifyRowsResized(resized);
        updateChart(); // TODO: update only affected rows!
        currentRowHeight = Math.max(currentRowHeight - decr, MIN_ROW_HEIGHT);
    }

    void resetRowHeights() {
        if (rows.isEmpty()) return;
        List<Row> resized = new ArrayList(rows.size());
        for (Row row : rows)
            if (row.setHeight(DEF_ROW_HEIGHT, true))
                resized.add(row);
        updateRowOffsets(0);
        if (!resized.isEmpty()) notifyRowsResized(new ArrayList(rows));
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


    private void notifyRowsAdded(final List<Row> rows) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                for (RowListener listener : rowListeners)
                    listener.rowsAdded(rows);
            }
        });
    }

    private void notifyRowsRemoved(final List<Row> rows) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                for (RowListener listener : rowListeners)
                    listener.rowsRemoved(rows);
            }
        });
    }

    private void notifyRowsResized(final List<Row> rows) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                for (RowListener listener : rowListeners)
                    listener.rowsResized(rows);
            }
        });
    }


    // --- Selection support ---------------------------------------------------

    boolean selectRow(Row row) {
        if (!selectedRows.add(row)) return false;
        repaintRows();
        return true;
    }

    boolean unselectRow(Row row) {
        if (!selectedRows.remove(row)) return false;
        repaintRows();
        return true;
    }

    boolean setSelectedRow(Row row) {
        if (row == null) {
            return clearRowsSelection();
        } else {
            if (selectedRows.size() == 1 && selectedRows.contains(row)) return false;
            selectedRows.clear();
            selectedRows.add(row);
            repaintRows();
            return true;
        }
    }

    boolean toggleRowSelection(Row row) {
        if (selectedRows.contains(row)) return unselectRow(row);
        else return selectRow(row);
    }

    boolean clearRowsSelection() {
        if (selectedRows.isEmpty()) return false;
        selectedRows.clear();
        repaintRows();
        return true;
    }

    boolean isRowSelected(Row row) {
        return selectedRows.contains(row);
    }

    boolean isRowSelection() {
        return !selectedRows.isEmpty();
    }

    List<Row> getSelectedRows() {
        return new ArrayList(selectedRows);
    }
    

    void updateSelection(boolean enable, Object source) {
        int blockersSize = selectionBlockers.size();
        if (enable) selectionBlockers.remove(source);
        else selectionBlockers.add(source);
        if (selectionBlockers.size() == blockersSize) return;

        ChartSelectionModel selectionModel = getSelectionModel();
        if (selectionModel == null) return;

        if (selectionBlockers.isEmpty()) {
            selectionModel.setHoverMode(lastHoverMode);
        } else {
            lastHoverMode = selectionModel.getHoverMode();
            lastMoveMode = selectionModel.getMoveMode();
            selectionModel.setHoverMode(ChartSelectionModel.HOVER_NONE);
        }
    }


    // --- Internal API to access protected methods ----------------------------

    long maxOffsetX() {
        return super.getMaxOffsetX();
    }

    double viewWidth(double d) {
        return super.getViewWidth(d);
    }

    protected void processMouseWheelEvent(MouseWheelEvent e) {
        super.processMouseWheelEvent(e);
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
        for (int i = startIndex; i < rows.size(); i++) {
            ChartContext rowContext = rows.get(i).getContext();
            invalidateImage(new Rectangle(0, Utils.checkedInt(rowContext.
                           getViewportOffsetY()), getWidth(), rowContext.
                           getViewportHeight()));
        }
        repaintDirty();
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

        private boolean setHeight(int height, boolean checkStep) {
            height = Math.max(MIN_ROW_HEIGHT, height);
            height = Math.min(MAX_ROW_HEIGHT, height);
            if (checkStep) height = height / ROW_RESIZE_STEP * ROW_RESIZE_STEP;
            boolean changed = rowHeight != height;
            rowHeight = height;
            return changed;
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

        @SuppressWarnings("element-type-mismatch")
        boolean containsItem(ChartItem item) {
            return items.contains(item);
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

            marginTop = ROW_MARGIN_TOP;
            marginBottom = ROW_MARGIN_BOTTOM;

            bounds = new LongRect();
        }


        protected void updateBounds() {
            LongRect.clear(bounds);

            PaintersModel painters = paintersModel();
            int itemsCount = row.getItemsCount();

            for (int i = 0; i < itemsCount; i++) {
                ChartItem item = row.getItem(i);
                ItemPainter painter = painters.getPainter(item);
                LongRect itemBounds = painter.getItemBounds(item);
                if (LongRect.isClear(bounds)) {
                    LongRect.set(bounds, itemBounds);
                } else if (LongRect.isEmpty(itemBounds)) { // Zero height (constant value)
                    LongRect.add(bounds, itemBounds.x, itemBounds.height);
                } else {
                    LongRect.add(bounds, itemBounds);
                }
            }

            double oldScaleY = scaleY;
            scaleY = (double)(row.getHeight() - marginTop - marginBottom) /
                     (double)(bounds.height == 0 ? 1 : bounds.height);

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
                return bounds.y - (viewY + getViewInsets().bottom - marginBottom -
                       getOffsetY() - getHeight()) / scaleY;
            } else {
                return (viewY + getOffsetY() - getViewInsets().top - marginTop) /
                       scaleY + bounds.y;
            }
        }

    }


    private static class RowComparator implements Comparator<Row> {

        public int compare(Row r1, Row r2) {
            int r1i = r1.getIndex();
            int r2i = r2.getIndex();
            return (r1i < r2i ? -1 : (r1i == r2i ? 0 : 1));
        }

    }


    public static interface RowListener {

        public void rowsAdded(List<Row> rows);

        public void rowsRemoved(List<Row> rows);

        public void rowsResized(List<Row> rows);

    }

}
