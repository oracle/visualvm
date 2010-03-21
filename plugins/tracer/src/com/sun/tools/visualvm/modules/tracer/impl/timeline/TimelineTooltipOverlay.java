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

import org.netbeans.lib.profiler.charts.ChartOverlay;
import org.netbeans.lib.profiler.charts.swing.Utils;
import org.netbeans.lib.profiler.charts.ChartConfigurationListener;
import org.netbeans.lib.profiler.charts.ChartContext;
import org.netbeans.lib.profiler.charts.ChartSelectionListener;
import org.netbeans.lib.profiler.charts.ItemPainter;
import org.netbeans.lib.profiler.charts.ItemSelection;
import org.netbeans.lib.profiler.charts.PaintersModel;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.Timer;
import org.netbeans.lib.profiler.charts.ChartSelectionModel;
import org.netbeans.lib.profiler.charts.swing.LongRect;
import org.netbeans.lib.profiler.charts.xy.XYItemSelection;
import org.netbeans.lib.profiler.charts.xy.synchronous.SynchronousXYItem;

/**
 *
 * @author Jiri Sedlacek
 */
final class TimelineTooltipOverlay extends ChartOverlay implements ActionListener {

    private static final int TOOLTIP_OFFSET = 15;
    private static final int TOOLTIP_MARGIN = 10;
    private static final int TOOLTIP_RESPONSE = 50;
    private static final int ANIMATION_STEPS = 5;

    private TimelineTooltipPainter[] tooltipPainters;

    private Timer timer;
    private int currentStep;
//    private Point mousePosition;
    private Point[] targetPositions;


    TimelineTooltipOverlay(final TimelineChart chart) {
        if (chart.getSelectionModel() == null)
            throw new NullPointerException("No ChartSelectionModel set for " + chart); // NOI18N

        if (!Utils.forceSpeed()) {
            timer = new Timer(TOOLTIP_RESPONSE / ANIMATION_STEPS, this);
            timer.setInitialDelay(0);
        }

        setLayout(null);

        chart.getSelectionModel().addSelectionListener(new ChartSelectionListener() {

            public void selectionModeChanged(int newMode, int oldMode) {}

            public void selectionBoundsChanged(Rectangle newBounds, Rectangle oldBounds) {}

            public void highlightedItemsChanged(List<ItemSelection> currentItems,
                List<ItemSelection> addedItems, List<ItemSelection> removedItems) {
                updateTooltip(chart);
            }

            public void selectedItemsChanged(List<ItemSelection> currentItems,
                List<ItemSelection> addedItems, List<ItemSelection> removedItems) {}

        });

        chart.addConfigurationListener(new ChartConfigurationListener.Adapter() {

            public void contentsUpdated(long offsetX, long offsetY,
                                    double scaleX, double scaleY,
                                    long lastOffsetX, long lastOffsetY,
                                    double lastScaleX, double lastScaleY,
                                    int shiftX, int shiftY) {
                updateTooltip(chart);
            }

        });
    }

    void setupPainters(TimelineTooltipPainter[] tooltipPainters) {
        removeAll();
        
        this.tooltipPainters = tooltipPainters;
        for (TimelineTooltipPainter tooltipPainter : tooltipPainters) {
            add(tooltipPainter);
            tooltipPainter.setVisible(false);
        }

        targetPositions = new Point[tooltipPainters.length];
    }

    private void setPosition(Point p, TimelineTooltipPainter tooltipPainter, int index) {
        if (tooltipPainters != null) {
            if (p == null) {
                if (tooltipPainter.isVisible()) tooltipPainter.setVisible(false);
                if (timer != null) timer.stop();
            } else {
                if (!tooltipPainter.isVisible() || timer == null) {
                    tooltipPainter.setVisible(true);
                    tooltipPainter.setLocation(p);
                } else {
                    currentStep = 0;
                    targetPositions[index] = p;
                    timer.restart();
                }
            }
        }
    }

    public void actionPerformed(ActionEvent e) {
        for (int i = 0; i < tooltipPainters.length; i++) {
            TimelineTooltipPainter tooltipPainter = tooltipPainters[i];
            Point targetPosition = targetPositions[i];
            Point currentPosition = tooltipPainter.getLocation();

            currentPosition.x += (targetPosition.x - currentPosition.x) /
                                 (ANIMATION_STEPS - currentStep);
            currentPosition.y += (targetPosition.y - currentPosition.y) /
                                 (ANIMATION_STEPS - currentStep);
            tooltipPainter.setLocation(currentPosition);
        }
        if (++currentStep == ANIMATION_STEPS) timer.stop();
    }


    @SuppressWarnings("element-type-mismatch")
    private void updateTooltip(TimelineChart chart) {
        ChartSelectionModel selectionModel = chart.getSelectionModel();
        if (selectionModel == null) return;

        List<ItemSelection> highlightedItems =
                selectionModel.getHighlightedItems();

        boolean noSelection = highlightedItems.isEmpty();
        if (!noSelection) {
            XYItemSelection sel = (XYItemSelection)highlightedItems.get(0);
            noSelection = sel.getItem().getValuesCount() <= sel.getValueIndex();
        }

        int rowsCount = chart.getRowsCount();
        for (int i = 0; i < rowsCount; i++) {
            TimelineTooltipPainter tooltipPainter = tooltipPainters[i];
            if (noSelection) {
                setPosition(null, tooltipPainter, i);
            } else {
                TimelineChart.Row row = chart.getRow(i);
                List<ItemSelection> highlightedRowItems = new ArrayList();
                List<SynchronousXYItem> rowItems = Arrays.asList(row.getItems());

                for (ItemSelection sel : highlightedItems)
                    if (rowItems.contains(sel.getItem()))
                        highlightedRowItems.add(sel);

                tooltipPainter.update(highlightedRowItems);
                tooltipPainter.setSize(tooltipPainter.getPreferredSize());
                setPosition(highlightedRowItems, chart.getPaintersModel(), row.getContext(), tooltipPainter, i);
            }
        }
    }

    private void setPosition(List<ItemSelection> selectedItems, PaintersModel paintersModel,
                             ChartContext chartContext, TimelineTooltipPainter tooltipPainter, int index) {
        LongRect bounds = null;

        for (ItemSelection selection : selectedItems) {
            ItemPainter painter = paintersModel.getPainter(selection.getItem());
            LongRect selBounds = painter.getSelectionBounds(selection, chartContext);
            if (bounds == null) bounds = selBounds; else LongRect.add(bounds, selBounds);
        }

        setPosition(normalizePosition(Utils.checkedRectangle(bounds), tooltipPainter, chartContext), tooltipPainter, index);
    }

    private Point normalizePosition(Rectangle bounds, TimelineTooltipPainter tooltipPainter, ChartContext chartContext) {
        Point p = new Point();

        p.x = bounds.x + bounds.width + TOOLTIP_OFFSET;
        if (p.x > chartContext.getViewportWidth() - tooltipPainter.getWidth() - TOOLTIP_MARGIN)
            p.x = bounds.x - tooltipPainter.getWidth() - TOOLTIP_OFFSET;

        int rowY = Utils.checkedInt(chartContext.getViewportOffsetY());
        int rowHeight = chartContext.getViewportHeight();
        p.y = rowY + (rowHeight - tooltipPainter.getHeight()) / 2;

        return p;
    }


    public void paint(Graphics g) {
        if (tooltipPainters == null) return;

        Rectangle bounds = new Rectangle(0, 0, getWidth(), getHeight());
        Rectangle clip = g.getClipBounds();
        if (clip == null) g.setClip(bounds);
        else g.setClip(clip.intersection(bounds));

        super.paint(g);
    }

}
