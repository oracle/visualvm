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

package org.netbeans.lib.profiler.charts;

import org.netbeans.lib.profiler.charts.swing.LongRect;
import org.netbeans.lib.profiler.charts.swing.Utils;
import org.netbeans.lib.profiler.charts.canvas.InteractiveCanvasComponent;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Jiri Sedlacek
 */
public class ChartComponent extends InteractiveCanvasComponent {

    private RenderingHints renderingHints;

    private List<ChartDecorator> preDecorators;
    private List<ChartDecorator> postDecorators;

    private ItemsModel itemsModel;
    private PaintersModel paintersModel;

    protected LongRect dataBounds;
    private LongRect initialDataBounds;
    private ChartContext chartContext;

    private ItemsModelListener itemsListener;
    private PaintersModelListener paintersListener;

    private List<ChartOverlay> overlays;

    private ChartSelectionModel selectionModel;
    private SelectionListener selectionListener;

    private List<ChartConfigurationListener> configurationListeners;


    public ChartComponent() {
        initRenderingHints();

        itemsListener = new ItemsModelListener();
        paintersListener = new PaintersModelListener();

        dataBounds = new LongRect();
        initialDataBounds = new LongRect();

        setLayout(null);

        setSelectionModel(new ChartSelectionManager());
    }


    // --- Models --------------------------------------------------------------

    public final void setItemsModel(ItemsModel itemsModel) {
        if (itemsModel == null) throw new IllegalArgumentException("ItemsModel cannot be null");
        if (itemsModel == this.itemsModel) return;

        if (this.itemsModel != null) this.itemsModel.removeItemsListener(itemsListener);

        this.itemsModel = itemsModel;
        this.itemsModel.addItemsListener(itemsListener);

        updateChart();
    }

    public final ItemsModel getItemsModel() {
        return itemsModel;
    }

    public final void setPaintersModel(PaintersModel paintersModel) {
        if (paintersModel == null) throw new IllegalArgumentException("PaintersModel cannot be null");
        if (paintersModel == this.paintersModel) return;

        if (this.paintersModel != null) this.paintersModel.removePaintersListener(paintersListener);

        this.paintersModel = paintersModel;
        this.paintersModel.addPaintersListener(paintersListener);

        updateChart();
    }

    public final PaintersModel getPaintersModel() {
        return paintersModel;
    }

    public final void setSelectionModel(ChartSelectionModel selectionModel) {
        // Cleanup previous model
        if (this.selectionModel != null) {
            this.selectionModel.removeSelectionListener(selectionListener);
            if (this.selectionModel instanceof ChartSelectionManager)
                ((ChartSelectionManager)this.selectionModel).unregisterChart(this);
        }

        // Assign new model
        this.selectionModel = selectionModel;

        // Setup new model
        if (selectionModel != null) {
            if (selectionListener == null) selectionListener = new SelectionListener();
            selectionModel.addSelectionListener(selectionListener);
            if (selectionModel instanceof ChartSelectionManager)
                ((ChartSelectionManager)selectionModel).registerChart(this);
        } else {
            selectionListener = null;
        }
    }

    public final ChartSelectionModel getSelectionModel() {
        return selectionModel;
    }


    // --- Initial data bounds -------------------------------------------------

    public final void setInitialDataBounds(LongRect bounds) {
        if (LongRect.equals(bounds, initialDataBounds)) return;
        LongRect.set(initialDataBounds, bounds);
        if (LongRect.isEmpty(dataBounds)) {
            resizeChart();
            invalidateImage();
            repaintDirty();
        }
    }

    public final LongRect getInitialDataBounds() {
        return initialDataBounds;
    }

    // --- Customizable RenderingHints -----------------------------------------

    public final void setRenderingHints(RenderingHints renderingHints) {
        this.renderingHints = Utils.checkedRenderingHints(renderingHints);
    }

    public final RenderingHints getRenderingHints() {
        return (RenderingHints)renderingHints.clone();
    }

    private void applyRenderingHints(Graphics2D g) {
        if (renderingHints != null) g.setRenderingHints(renderingHints);
    }

    private void initRenderingHints() {
        RenderingHints hints = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        setRenderingHints(hints);
    }


    // --- ChartContext --------------------------------------------------------

    public final ChartContext getChartContext() {
        return getChartContext(null);
    }

    protected ChartContext getChartContext(ChartItem item) {
        if (chartContext == null) chartContext = createChartContext();
        return chartContext;
    }

    protected ChartContext createChartContext() {
        return new Context(this);
    }


    // --- Configuration Listeners ---------------------------------------------

    public final void addConfigurationListener(ChartConfigurationListener listener) {
        if (configurationListeners == null) configurationListeners = new ArrayList();
        configurationListeners.add(listener);
    }

    public final void removeConfigurationListener(ChartConfigurationListener listener) {
        if (configurationListeners == null) return;
        configurationListeners.remove(listener);
    }


    protected final void offsetChanged(long oldOffsetX, long oldOffsetY,
                              long newOffsetX, long newOffsetY) {
        super.offsetChanged(oldOffsetX, oldOffsetY, newOffsetX, newOffsetY);
        fireOffsetChanged(oldOffsetX, oldOffsetY, newOffsetX, newOffsetY);
    }

    protected final void scaleChanged(double oldScaleX, double oldScaleY,
                             double newScaleX, double newScaleY) {
        super.scaleChanged(oldScaleX, oldScaleY, newScaleX, newScaleY);
        fireScaleChanged(oldScaleX, oldScaleY, newScaleX, newScaleY);
    }

    protected final void dataBoundsChanged(long dataOffsetX, long dataOffsetY,
                                  long dataWidth, long dataHeight,
                                  long oldDataOffsetX, long oldDataOffsetY,
                                  long oldDataWidth, long oldDataHeight) {
        super.dataBoundsChanged(dataOffsetX, dataOffsetY, dataWidth, dataHeight,
                                oldDataOffsetX, oldDataOffsetY, oldDataWidth, oldDataHeight);
        fireDataBoundsChanged(dataOffsetX, dataOffsetY, dataWidth, dataHeight,
                              oldDataOffsetX, oldDataOffsetY, oldDataWidth, oldDataHeight);
    }

    protected final void contentsWillBeUpdated(long offsetX, long offsetY,
                               double scaleX, double scaleY,
                               long lastOffsetX, long lastOffsetY,
                               double lastScaleX, double lastScaleY) {
        super.contentsWillBeUpdated(offsetX, offsetY, scaleX, scaleY, lastOffsetX,
                          lastOffsetY, lastScaleX, lastScaleY);
        fireContentsWillBeUpdated(offsetX, offsetY, scaleX, scaleY,
                                  lastOffsetX, lastOffsetY, lastScaleX, lastScaleY);
    }

    protected final void contentsUpdated(long offsetX, long offsetY,
                            double scaleX, double scaleY,
                            long lastOffsetX, long lastOffsetY,
                            double lastScaleX, double lastScaleY,
                            int shiftX, int shiftY) {
        super.contentsUpdated(offsetX, offsetY, scaleX, scaleY, lastOffsetX,
                          lastOffsetY, lastScaleX, lastScaleY, shiftX, shiftY);
        fireContentsUpdated(offsetX, offsetY, scaleX, scaleY, lastOffsetX, lastOffsetY,
                        lastScaleX, lastScaleY, shiftX, shiftY);
    }


    private void fireOffsetChanged(long oldOffsetX, long oldOffsetY,
                                   long newOffsetX, long newOffsetY) {
        if (configurationListeners == null) return;
        for (ChartConfigurationListener listener : configurationListeners)
            listener.offsetChanged(oldOffsetX, oldOffsetY, newOffsetX, newOffsetY);
    }

    private void fireScaleChanged(double oldScaleX, double oldScaleY,
                                  double newScaleX, double newScaleY) {
        if (configurationListeners == null) return;
        for (ChartConfigurationListener listener : configurationListeners)
            listener.scaleChanged(oldScaleX, oldScaleY, newScaleX, newScaleY);
    }

    private void fireDataBoundsChanged(long dataOffsetX, long dataOffsetY,
                                       long dataWidth, long dataHeight,
                                       long oldDataOffsetX, long oldDataOffsetY,
                                       long oldDataWidth, long oldDataHeight) {
        if (configurationListeners == null) return;
        for (ChartConfigurationListener listener : configurationListeners)
            listener.dataBoundsChanged(dataOffsetX, dataOffsetY, dataWidth,
                                       dataHeight, oldDataOffsetX, oldDataOffsetY,
                                       oldDataWidth, oldDataHeight);
    }

    private void fireContentsWillBeUpdated(long offsetX, long offsetY,
                            double scaleX, double scaleY,
                            long lastOffsetX, long lastOffsetY,
                            double lastScaleX, double lastScaleY) {
        if (configurationListeners == null) return;
        for (ChartConfigurationListener listener : configurationListeners)
            listener.contentsWillBeUpdated(offsetX, offsetY, scaleX, scaleY,
                              lastOffsetX, lastOffsetY, lastScaleX, lastScaleY);
    }

    private void fireContentsUpdated(long offsetX, long offsetY,
                            double scaleX, double scaleY,
                            long lastOffsetX, long lastOffsetY,
                            double lastScaleX, double lastScaleY,
                            int shiftX, int shiftY) {
        if (configurationListeners == null) return;
        for (ChartConfigurationListener listener : configurationListeners)
            listener.contentsUpdated(offsetX, offsetY, scaleX, scaleY, lastOffsetX,
                                 lastOffsetY, lastScaleX, lastScaleY, shiftX, shiftY);
    }


    // --- Pre & post painters support -----------------------------------------

    public final void addPreDecorator(ChartDecorator decorator) {
        if (preDecorators == null) preDecorators = new ArrayList(2);
        preDecorators.add(decorator);
    }

    public final void removePreDecorator(ChartDecorator decorator) {
        if (preDecorators != null) preDecorators.remove(decorator);
    }

    final List<ChartDecorator> getPreDecorators() {
        return preDecorators;
    }

    public final void addPostDecorator(ChartDecorator decorator) {
        if (postDecorators == null) postDecorators = new ArrayList(2);
        postDecorators.add(decorator);
    }

    public final void removePostDecorator(ChartDecorator decorator) {
        if (postDecorators != null) postDecorators.remove(decorator);
    }

    final List<ChartDecorator> getPostDecorators() {
        return postDecorators;
    }


    // --- Overlays ------------------------------------------------------------

    public final void addOverlayComponent(ChartOverlay overlay) {
        if (overlays == null) overlays = new ArrayList();

        overlay.setChartContext(getChartContext());

        overlays.add(overlay);
        add(overlay);
    }

    public final void removeOverlayComponent(ChartOverlay overlay) {
        remove(overlay);
        overlays.remove(overlay);

        overlay.setChartContext(null);
    }


    protected void reshaped(Rectangle oldBounds, Rectangle newBounds) {
        super.reshaped(oldBounds, newBounds);

        if (overlays == null) return;

        for (ChartOverlay overlay : overlays)
            overlay.setBounds(0, 0, newBounds.width, newBounds.height);
    }


    // --- Paint implementation ------------------------------------------------

    protected void paintContents(Graphics g, Rectangle invalidArea) {
        Graphics2D g2 = (Graphics2D)g;

        // Set clip
        g2.setClip(invalidArea);

        // Set rendering hints
        applyRenderingHints(g2);

        // Paint background if opaque
        if (isOpaque()) {
            g2.setColor(getBackground());
            g2.fillRect(invalidArea.x, invalidArea.y,
                       invalidArea.width, invalidArea.height);
        }

        // Paint registered prepainters
        if (preDecorators != null)
            for (ChartDecorator decorator : preDecorators)
                decorator.paint(g2, invalidArea, getChartContext());

        // Paint chart items
        if (itemsModel != null && paintersModel != null) {
            int itemsCount = itemsModel.getItemsCount();
            
            if (itemsCount != 0) {
                boolean sel = selectionModel != null;
                
                List<ItemSelection> highlightedSelection = sel ? selectionModel.getHighlightedItems() : null;
                List<ItemSelection> selectedSelection = sel ? selectionModel.getSelectedItems() : null;
                List<ItemSelection> filteredHighlighted = sel ? new ArrayList() : Collections.EMPTY_LIST;
                List<ItemSelection> filteredSelected = sel ? new ArrayList() : Collections.EMPTY_LIST;

                for (int i = 0; i < itemsCount; i++) {
                    ChartItem item = itemsModel.getItem(i);
                    ItemPainter painter = paintersModel.getPainter(item);

                    if (sel) {
                        filteredHighlighted.clear();
                        if (painter.supportsHovering(item))
                            filterSelection(highlightedSelection, filteredHighlighted, item);
                        filteredSelected.clear();
                        if (painter.supportsSelecting(item))
                            filterSelection(selectedSelection, filteredSelected, item);
                    }
                    
                    painter.paintItem(item, filteredHighlighted, filteredSelected,
                                      g2, invalidArea, getChartContext(item));
                }
            }
        }

        // Paint registered postpainters
        if (postDecorators != null)
            for (ChartDecorator decorator : postDecorators)
                decorator.paint(g2, invalidArea, getChartContext());
    }


    private static void filterSelection(List<ItemSelection> selection, List<ItemSelection> result, ChartItem filter) {
        if (filter == null) return;
        for (ItemSelection sel : selection)
            if (sel.getItem().equals(filter)) result.add(sel);
    }


    // --- UI tweaks -----------------------------------------------------------

    public void setBackground(Color bg) {
        super.setBackground(Utils.checkedColor(bg));
    }


    // --- Protected implementation ----------------------------------------------

    protected void computeDataBounds() {
        LongRect.clear(dataBounds);
        if (itemsModel == null || paintersModel == null) return;

        int itemsCount = itemsModel.getItemsCount();
        for (int i = 0; i < itemsCount; i++) {
            ChartItem item = itemsModel.getItem(i);
            ItemPainter painter = paintersModel.getPainter(item);
            if (i == 0)
                LongRect.set(dataBounds, painter.getItemBounds(item));
            else
                LongRect.add(dataBounds, painter.getItemBounds(item));
        }
    }

    protected void resizeChart() {
        if (LongRect.isEmpty(dataBounds)) {
            LongRect bounds = new LongRect(dataBounds);
            if (bounds.width == 0) {
                bounds.width = initialDataBounds.width;
                if (bounds.x == 0) bounds.x = initialDataBounds.x;
            }
            if (bounds.height == 0) {
                bounds.height = initialDataBounds.height;
                if (bounds.y == 0) bounds.y = initialDataBounds.y;
            }
            setDataBounds(bounds.x, bounds.y, bounds.width, bounds.height);
        } else {
            setDataBounds(dataBounds.x, dataBounds.y, dataBounds.width, dataBounds.height);
        }
    }

    protected void updateChart() {
        computeDataBounds();
        resizeChart();
        invalidateImage();
        repaintDirty();
    }

    protected void itemsAdded(List<ChartItem> addedItems) {
        // Update chart size
        LongRect oldBounds = new LongRect(dataBounds);
        for (ChartItem item : addedItems) {
            ItemPainter painter = paintersModel.getPainter(item);
            LongRect.add(dataBounds, painter.getItemBounds(item));
        }
        if (!LongRect.equals(oldBounds, dataBounds)) resizeChart();

        // Update chart appearance
        LongRect uiBounds = null;
        for (ChartItem item : addedItems) {
            ItemPainter painter = paintersModel.getPainter(item);
            if (uiBounds == null) uiBounds =
                new LongRect(painter.getItemBounds(item, getChartContext(item)));
            else
                LongRect.add(uiBounds, painter.getItemBounds(item, getChartContext(item)));
        }
        invalidateImage(Utils.getCheckedRectangle(uiBounds));
        repaintDirty();
    }

    protected void itemsRemoved(List<ChartItem> removedItems) {
        List<ItemPainter> painters = new ArrayList(removedItems.size());

        // Try to resolve painters for all removed items
        for (ChartItem item : removedItems) {
            ItemPainter painter = paintersModel.getPainter(item);
            if (painter == null) {
                painters = null;
                break;
            }
            painters.add(painter);
        }

        if (painters == null) {
            // Some or all painters for removed items not available
            updateChart();
        } else {
            // All painters for removed items available

            // Update chart size
            LongRect oldBounds = new LongRect(dataBounds);
            computeDataBounds();
            if (!LongRect.equals(oldBounds, dataBounds)) resizeChart();

            // Update chart appearance
            LongRect uiBounds = null;
            for (int i = 0; i < removedItems.size(); i++) {
                ChartItem item = removedItems.get(i);
                ItemPainter painter = painters.get(i);
                if (uiBounds == null) uiBounds =
                    new LongRect(painter.getItemBounds(item, getChartContext(item)));
                else
                    LongRect.add(uiBounds, painter.getItemBounds(item, getChartContext(item)));
            }
            invalidateImage(Utils.getCheckedRectangle(uiBounds));
            repaintDirty();
        }
    }

    protected void itemsChanged(List<ChartItemChange> itemChanges) {
        // Resolve painters for changedItems
        List<ItemPainter> painters = new ArrayList(itemChanges.size());
        for (ChartItemChange change : itemChanges)
            painters.add(paintersModel.getPainter(change.getItem()));

        // Check if items bounds changed
        boolean boundsChange = false;
        for (int i = 0; i < itemChanges.size(); i++) {
            ChartItemChange change = itemChanges.get(i);
            ItemPainter painter = painters.get(i);
            boundsChange = painter.isBoundsChange(change);
            if (boundsChange) break;
        }

        // Update chart size
        if (boundsChange) {
            LongRect oldBounds = new LongRect(dataBounds);
            computeDataBounds();
            if (!LongRect.equals(oldBounds, dataBounds)) resizeChart();
        }

        // Check if items appearance changed
        boolean appearanceChange = false;
        for (int i = 0; i < itemChanges.size(); i++) {
            ChartItemChange change = itemChanges.get(i);
            ItemPainter painter = painters.get(i);
            appearanceChange = painter.isAppearanceChange(change);
            if (appearanceChange) break;
        }

        // Update chart appearance
        if (appearanceChange) {
            LongRect uiBounds = null;
            for (int i = 0; i < itemChanges.size(); i++) {
                ChartItemChange change = itemChanges.get(i);
                ChartItem item = change.getItem();
                ItemPainter painter = paintersModel.getPainter(item);
                if (painter.isAppearanceChange(change)) {
                    if (uiBounds == null) uiBounds =
                        new LongRect(painter.getDirtyBounds(change, getChartContext(item)));
                    else
                        LongRect.add(uiBounds, painter.getDirtyBounds(change, getChartContext(item)));
                }
            }
            invalidateImage(Utils.getCheckedRectangle(uiBounds));
            repaintDirtyAccel();
        } else {
            repaintDirty();
        }
    }

    protected void paintersChanged() {
        updateChart();
    }

    protected void paintersChanged(List<ItemPainter> changedPainters) {
        Set<ChartItem> changedItems = new HashSet();

        // Update chart size
        LongRect oldBounds = new LongRect(dataBounds);
        computeDataBounds();
        if (!LongRect.equals(oldBounds, dataBounds)) resizeChart();

        // Resolve changed items
        for (int i = 0; i < itemsModel.getItemsCount(); i++) {
            ChartItem item = itemsModel.getItem(i);
            if (changedPainters.contains(paintersModel.getPainter(item)))
                changedItems.add(item);
        }

        // Update chart appearance
        LongRect uiBounds = null;
        for (ChartItem item : changedItems) {
            ItemPainter painter = paintersModel.getPainter(item);
            if (uiBounds == null) uiBounds =
                new LongRect(painter.getItemBounds(item, getChartContext(item)));
            else
                LongRect.add(uiBounds, painter.getItemBounds(item, getChartContext(item)));
        }
        invalidateImage(Utils.getCheckedRectangle(uiBounds));
        repaintDirty();
    }


    // --- ItemsModel change support -------------------------------------------

    private class ItemsModelListener implements ItemsListener {

        public void itemsAdded(List<ChartItem> addedItems) {
            ChartComponent.this.itemsAdded(addedItems);
        }

        public void itemsRemoved(List<ChartItem> removedItems) {
            ChartComponent.this.itemsRemoved(removedItems);
        }

        public void itemsChanged(List<ChartItemChange> itemChanges) {
            ChartComponent.this.itemsChanged(itemChanges);
        }

    }


    // --- PaintersModel change support ----------------------------------------

    private class PaintersModelListener implements PaintersListener {

        public void paintersChanged() {
            ChartComponent.this.paintersChanged();
        }

        public void paintersChanged(List<ItemPainter> changedPainters) {
            ChartComponent.this.paintersChanged(changedPainters);
        }

    }


    // --- Selection listener --------------------------------------------------

    private class SelectionListener implements ChartSelectionListener {

        public void selectionModeChanged(int newMode, int oldMode) {}

        public void selectionBoundsChanged(Rectangle newBounds, Rectangle oldBounds) {}

        public void highlightedItemsChanged(List<ItemSelection> currentItems,
              List<ItemSelection> addedItems, List<ItemSelection> removedItems) {
            refreshSelection(addedItems, removedItems);
        }

        public void selectedItemsChanged(List<ItemSelection> currentItems,
              List<ItemSelection> addedItems, List<ItemSelection> removedItems) {
            refreshSelection(addedItems, removedItems);
        }


        private void refreshSelection(List<ItemSelection> addedItems, List<ItemSelection> removedItems) {
            // TODO: should be optimized!
            Rectangle dirtyArea = new Rectangle();

            if (!removedItems.isEmpty() && removedItems.get(0) != null)
                for (ItemSelection sel : removedItems) {
                    ChartItem item = sel.getItem();
                    ItemPainter painter = paintersModel.getPainter(item);
                    if (painter.supportsHovering(item)) {
                        if (dirtyArea.isEmpty())
                            dirtyArea.setBounds(Utils.getCheckedRectangle(
                            painter.getSelectionBounds(sel, getChartContext(item))));
                        else
                            dirtyArea.add(Utils.getCheckedRectangle(
                            painter.getSelectionBounds(sel, getChartContext(item))));
                    }
                }

            if (!dirtyArea.isEmpty()) {
                invalidateImage(dirtyArea);
                paintImmediately(dirtyArea);
//                repaintDirty();
                dirtyArea = new Rectangle();
            }

            if (!addedItems.isEmpty() && addedItems.get(0) != null)
                for (ItemSelection sel : addedItems) {
                    ChartItem item = sel.getItem();
                    ItemPainter painter = paintersModel.getPainter(item);
                    if (painter.supportsHovering(item)) {
                        if (dirtyArea.isEmpty())
                            dirtyArea.setBounds(Utils.getCheckedRectangle(
                            painter.getSelectionBounds(sel, getChartContext(item))));
                        else
                            dirtyArea.add(Utils.getCheckedRectangle(
                            painter.getSelectionBounds(sel, getChartContext(item))));
                    }
                }

            if (!dirtyArea.isEmpty()) {
                invalidateImage(dirtyArea);
                paintImmediately(dirtyArea);
//                repaintDirty();
            }
                
//            invalidateImage(dirtyArea);
//            immediatelyRepaintDirty();
//            repaintDirty(dirtyArea);
        }

    }


    // --- ChartContext implementation -----------------------------------------

    protected static class Context implements ChartContext {

        private ChartComponent chart;


        public Context(ChartComponent chart) { this.chart = chart; }

        protected ChartComponent getChartComponent() { return chart; }


        public boolean isRightBased() { return chart.isRightBased(); }

        public boolean isBottomBased() { return chart.isBottomBased(); }

        public boolean fitsWidth() { return chart.fitsWidth(); }

        public boolean fitsHeight() { return chart.fitsHeight(); }

        public long getDataOffsetX() { return chart.getDataOffsetX(); }

        public long getDataOffsetY() { return chart.getDataOffsetY(); }

        public long getDataWidth() { return chart.getDataWidth(); }

        public long getDataHeight() { return chart.getDataHeight(); }

        public long getViewWidth() { return chart.getContentsWidth(); }

        public long getViewHeight() { return chart.getContentsHeight(); }

        public long getViewportOffsetX() { return 0; }

        public long getViewportOffsetY() { return 0; }

        public int getViewportWidth() { return chart.getWidth(); }

        public int getViewportHeight() { return chart.getHeight(); }

        public double getViewX(double dataX) { return chart.getViewX(dataX); }

        public double getReversedViewX(double dataX) { return chart.getReversedViewX(dataX); }

        public double getViewY(double dataY) { return chart.getViewY(dataY); }

        public double getReversedViewY(double dataY) { return chart.getReversedViewY(dataY); }

        public double getViewWidth(double dataWidth) { return chart.getViewWidth(dataWidth); }

        public double getViewHeight(double dataHeight) { return chart.getViewHeight(dataHeight); }

        public LongRect getViewRect(LongRect dataRect) { return getViewRectImpl(dataRect); }

        public double getDataX(double viewX) { return chart.getDataX(viewX); }

        public double getReversedDataX(double viewX) { return chart.getReversedDataX(viewX); }

        public double getDataY(double viewY) { return chart.getDataY(viewY); }

        public double getReversedDataY(double viewY) { return chart.getReversedDataY(viewY); }

        public double getDataWidth(double viewWidth) { return chart.getDataWidth(viewWidth); }

        public double getDataHeight(double viewHeight) { return chart.getDataHeight(viewHeight); }


        private LongRect getViewRectImpl(LongRect dataRect) {
            LongRect viewRect = new LongRect();

            viewRect.x = (long)Math.ceil(getViewX(dataRect.x));
            viewRect.width = (long)Math.ceil(getViewWidth(dataRect.width));
            if (isRightBased()) viewRect.x -= viewRect.width;

            viewRect.y = (long)Math.ceil(getViewY(dataRect.y));
            viewRect.height = (long)Math.ceil(getViewHeight(dataRect.height));
            if (isBottomBased()) viewRect.y -= viewRect.height;

            return viewRect;
        }

    }

}
