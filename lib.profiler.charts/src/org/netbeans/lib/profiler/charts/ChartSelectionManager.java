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

import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.SwingUtilities;

/**
 *
 * @author Jiri Sedlacek
 */
class ChartSelectionManager implements ChartSelectionModel {

    private ChartComponent chart;

    private ChartListener chartListener;
    private MouseListener mouseListener;

    private int selectionMode;
    private int moveMode;
    private int dragMode;

    private int hoverMode;
    private int hoverDistanceLimit;

    private int mouseX;
    private int mouseY;
    private boolean inChart;

    private Rectangle selectionBounds;

    private List<ItemSelection> highlightedSelection;
    private List<ItemSelection> selectedSelection;

    private boolean mousePanningBackup;

    private List<ChartSelectionListener> selectionListeners;


    public ChartSelectionManager() {
        mouseX = -1;
        mouseY = -1;
        inChart = false;

        chartListener = new ChartListener();
        mouseListener = new MouseListener();

        setMoveMode(SELECTION_NONE);
        setDragMode(SELECTION_NONE);
        setSelectionMode(moveMode);

        setHoverMode(HOVER_NONE);
        setHoverDistanceLimit(HOVER_DISTANCE_LIMIT_NONE);
    }


    // --- Internal API --------------------------------------------------------

    void registerChart(ChartComponent chart) {
        unregisterListener();
        this.chart = chart;
        registerListener();
    }

    void unregisterChart(ChartComponent chart) {
        unregisterListener();
        this.chart = null;
    }


    // --- Selection mode ------------------------------------------------------

    public final void setMoveMode(int mode) {
        moveMode = mode;
        if (selectionMode != moveMode) setSelectionMode(moveMode);
    }

    public final int getMoveMode() {
        return moveMode;
    }

    public final void setDragMode(int mode) {
        dragMode = mode;
    }

    public final int getDragMode() {
        return dragMode;
    }

    public final int getSelectionMode() {
        return selectionMode;
    }

    public final void setHoverMode(int mode) {
        hoverMode = mode;
        updateHighlightedItems();
    }

    public final int getHoverMode() {
        return hoverMode;
    }

    public final void setHoverDistanceLimit(int limit) {
        hoverDistanceLimit = limit;
    }

    public final int getHoverDistanceLimit() {
        return hoverDistanceLimit;
    }


    // --- Selection bounds ----------------------------------------------------

    public final void setSelectionBounds(int x, int y, int w, int h) {
        setSelectionBounds(new Rectangle(x, y, w, h));
    }

    public final void setSelectionBounds(Rectangle selectionBounds) {
        if (selectionBounds == null && this.selectionBounds == null) return;
        if (this.selectionBounds != null && this.selectionBounds.equals(selectionBounds) ||
            selectionBounds != null && selectionBounds.equals(this.selectionBounds)) return;

        Rectangle oldSelectionBounds = this.selectionBounds == null ? null :
                                       new Rectangle(this.selectionBounds);

        if (selectionBounds == null) this.selectionBounds = null;
        else if (this.selectionBounds == null) this.selectionBounds = new Rectangle(selectionBounds);
        else this.selectionBounds.setBounds(selectionBounds);

        fireSelectionBoundsChanged(this.selectionBounds, oldSelectionBounds);
    }

    public final Rectangle getSelectionBounds() {
        return new Rectangle(selectionBounds);
    }


    // --- Highlighted items ---------------------------------------------------

    public final void setHighlightedItems(List<ItemSelection> items) {
        if (highlightedSelection == null) {
            if (items.isEmpty()) return;
            highlightedSelection = new ArrayList(items);
            fireHighlightedItemsChanged(items, items, Collections.EMPTY_LIST);
        } else {
            List<ItemSelection> addedItems = new ArrayList();
            List<ItemSelection> removedItems = new ArrayList();

            for (ItemSelection item : items)
                if (!highlightedSelection.contains(item)) addedItems.add(item);

            for (ItemSelection item : highlightedSelection)
                if (!items.contains(item)) removedItems.add(item);

            if (addedItems.isEmpty() && removedItems.isEmpty()) return;

            highlightedSelection = new ArrayList(items);
            fireHighlightedItemsChanged(items, addedItems, removedItems);
        }
    }

    public final List<ItemSelection> getHighlightedItems() {
        return highlightedSelection == null ? Collections.EMPTY_LIST :
                                              new ArrayList(highlightedSelection);
    }


    // --- Selected items ------------------------------------------------------

    public final void setSelectedItems(List<ItemSelection> items) {
        if (selectedSelection == null) {
            if (items.isEmpty()) return;
            selectedSelection = new ArrayList(items);
            fireHighlightedItemsChanged(items, items, Collections.EMPTY_LIST);
        } else {
            List<ItemSelection> addedItems = new ArrayList();
            List<ItemSelection> removedItems = new ArrayList();

            for (ItemSelection item : items)
                if (!selectedSelection.contains(item)) addedItems.add(item);

            for (ItemSelection item : selectedSelection)
                if (!items.contains(item)) removedItems.add(item);

            if (addedItems.isEmpty() && removedItems.isEmpty()) return;

            selectedSelection = new ArrayList(items);
            fireSelectedItemsChanged(items, addedItems, removedItems);
        }
    }

    public final List<ItemSelection> getSelectedItems() {
        return selectedSelection == null ? Collections.EMPTY_LIST :
                                           new ArrayList(selectedSelection);
    }


    // --- Selection listeners -------------------------------------------------

    public final void addSelectionListener(ChartSelectionListener listener) {
        if (selectionListeners == null) selectionListeners = new ArrayList();
        selectionListeners.add(listener);
    }

    public final void removeSelectionListener(ChartSelectionListener listener) {
        if (selectionListeners == null) return;
        selectionListeners.remove(listener);
    }


    // --- Private implementation ----------------------------------------------

    private void setSelectionMode(int selectionMode) {
        if (this.selectionMode == selectionMode) return;
        int oldSelectionMode = this.selectionMode;
        this.selectionMode = selectionMode;
        fireSelectionModeChanged(this.selectionMode, oldSelectionMode);
    }


    private void registerListener() {
        if (chart == null) return;
        chart.addMouseListener(mouseListener);
        chart.addMouseMotionListener(mouseListener);
        chart.addConfigurationListener(chartListener);
    }

    private void unregisterListener() {
        if (chart == null) return;
        chart.removeMouseListener(mouseListener);
        chart.removeMouseMotionListener(mouseListener);
        chart.removeConfigurationListener(chartListener);
    }


    private void updateHighlightedItems() {
        if (hoverMode == HOVER_NONE || !inChart) {
            setHighlightedItems(Collections.EMPTY_LIST);
            return;
        }

        final int x = mouseX;
        final int y = mouseY;

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                ItemsModel itemsModel = chart.getItemsModel();
                PaintersModel paintersModel = chart.getPaintersModel();

                int itemsCount = itemsModel.getItemsCount();
                List<ItemSelection> closestSelection = new ArrayList(itemsCount);

                for (int i = 0; i < itemsCount; i++) {
                    ChartItem item = itemsModel.getItem(i);
                    ItemPainter painter = paintersModel.getPainter(item);
                    ItemSelection selection = painter.getClosestSelection(item, x, y,
                                                            chart.getChartContext());

                    if (selection != null) {
                        int distance = selection.getDistance();
                        if (hoverMode == HOVER_EACH_NEAREST || closestSelection.isEmpty()) {
                            if (hoverDistanceLimit == HOVER_DISTANCE_LIMIT_NONE ||
                                distance <= hoverDistanceLimit)
                                closestSelection.add(selection);
                        } else {
                            if (closestSelection.get(0).getDistance() > distance) {
                                if (hoverDistanceLimit == HOVER_DISTANCE_LIMIT_NONE ||
                                    distance <= hoverDistanceLimit)
                                    closestSelection.set(0, selection);
                            }
                        }
                    }
                }

                setHighlightedItems(closestSelection);
            }
        });
    }


    private void fireSelectionModeChanged(int newMode, int oldMode) {
        if (selectionListeners == null) return;
        for (ChartSelectionListener listener : selectionListeners)
            listener.selectionModeChanged(newMode, oldMode);
    }

    private void fireSelectionBoundsChanged(Rectangle newBounds, Rectangle oldBounds) {
        if (selectionListeners == null) return;
        for (ChartSelectionListener listener : selectionListeners)
            listener.selectionBoundsChanged(newBounds, oldBounds);
    }

    private void fireHighlightedItemsChanged(List<ItemSelection> currentItems,
            List<ItemSelection> addedItems, List<ItemSelection> removedItems) {
        if (selectionListeners == null) return;
        for (ChartSelectionListener listener : selectionListeners)
            listener.highlightedItemsChanged(currentItems, addedItems, removedItems);
    }

    private void fireSelectedItemsChanged(List<ItemSelection> currentItems,
            List<ItemSelection> addedItems, List<ItemSelection> removedItems) {
        if (selectionListeners == null) return;
        for (ChartSelectionListener listener : selectionListeners)
            listener.selectedItemsChanged(currentItems, addedItems, removedItems);
    }


    private class ChartListener extends ChartConfigurationListener.Adapter {

        public void contentsWillBeUpdated(long offsetX, long offsetY,
                                double scaleX, double scaleY,
                                long lastOffsetX, long lastOffsetY,
                                double lastScaleX, double lastScaleY) {
            updateHighlightedItems();
        }

    }


    private class MouseListener extends MouseAdapter implements MouseMotionListener {

        public void mousePressed(MouseEvent e) {
            mousePanningBackup = chart.isMousePanningEnabled();

            if (selectionMode != SELECTION_NONE) {
                chart.disableMousePanning();
                setSelectionMode(dragMode);
                setSelectionBounds(e.getX(), e.getY(), 0, 0);
            }
        }

        public void mouseReleased(MouseEvent e) {
            // Clear previous selection
            setSelectionBounds(null);

            if (selectionMode == SELECTION_NONE)
                chart.setMousePanningEnabled(mousePanningBackup);

            setSelectionMode(moveMode);

            // Refresh selection if needed
            if (selectionMode != SELECTION_NONE)
                setSelectionBounds(e.getX(), e.getY(), 0, 0);
        }

        public void mouseEntered(MouseEvent e) {
            inChart = true;
            mouseX = e.getX();
            mouseY = e.getY();
        }

        public void mouseExited(MouseEvent e) {
            inChart = false;

            if (selectionMode == SELECTION_LINE_V ||
                selectionMode == SELECTION_LINE_H ||
                selectionMode == SELECTION_CROSS) {
                setSelectionBounds(null);
            }

            updateHighlightedItems();
        }

        public void mouseDragged(MouseEvent e) {
            if (selectionMode == SELECTION_RECT) {
                setSelectionBounds(selectionBounds.x, selectionBounds.y,
                        e.getX() - selectionBounds.x, e.getY() - selectionBounds.y);
            }
        }

        public void mouseMoved(MouseEvent e) {
            mouseX = e.getX();
            mouseY = e.getY();

            if (selectionMode == SELECTION_NONE) setSelectionBounds(null);
            else setSelectionBounds(mouseX, mouseY, 0, 0);

            updateHighlightedItems();
        }

    }

}
