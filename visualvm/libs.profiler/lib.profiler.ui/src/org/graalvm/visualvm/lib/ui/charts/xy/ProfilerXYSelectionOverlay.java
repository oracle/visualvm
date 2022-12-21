/*
 * Copyright (c) 2015, 2022, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.ui.charts.xy;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.lib.charts.ChartComponent;
import org.graalvm.visualvm.lib.charts.ChartConfigurationListener;
import org.graalvm.visualvm.lib.charts.ChartOverlay;
import org.graalvm.visualvm.lib.charts.ChartSelectionListener;
import org.graalvm.visualvm.lib.charts.ItemSelection;
import org.graalvm.visualvm.lib.charts.swing.LongRect;
import org.graalvm.visualvm.lib.charts.swing.Utils;

/**
 *
 * @author Jiri Sedlacek
 */
public class ProfilerXYSelectionOverlay extends ChartOverlay {

    private ChartComponent chart;

    private int selectionExtent;

    private final ConfigurationListener configurationListener;
    private final SelectionListener selectionListener;
    private final Set<Point> selectedValues;

    private Paint markPaint;
    private Paint oddPerfPaint;
    private Paint evenPerfPaint;

    private Stroke markStroke;
    private Stroke oddPerfStroke;
    private Stroke evenPerfStroke;


    public ProfilerXYSelectionOverlay() {
        configurationListener = new ConfigurationListener();
        selectionListener = new SelectionListener();
        selectedValues = new HashSet();
        initDefaultValues();
    }


    // --- Public API ----------------------------------------------------------

    public final void registerChart(ChartComponent chart) {
        unregisterListener();
        this.chart = chart;
        registerListener();
    }

    public final void unregisterChart(ChartComponent chart) {
        unregisterListener();
        this.chart = null;
    }


    // --- Private implementation ----------------------------------------------

    private void registerListener() {
        if (chart == null) return;
        chart.addConfigurationListener(configurationListener);
        chart.getSelectionModel().addSelectionListener(selectionListener);
    }

    private void unregisterListener() {
        if (chart == null) return;
        chart.removeConfigurationListener(configurationListener);
        chart.getSelectionModel().removeSelectionListener(selectionListener);
    }

    private void initDefaultValues() {
        markPaint = new Color(80, 80, 80);
        oddPerfPaint = Color.BLACK;
        evenPerfPaint = Color.WHITE;

        markStroke = new BasicStroke(2.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        oddPerfStroke = new BasicStroke(1f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL, 0, new float[] { 1.0f, 3.0f }, 0);
        evenPerfStroke = new BasicStroke(1f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL, 0, new float[] { 1.0f, 3.0f }, 2);

        selectionExtent = 3;
    }


    public void paint(Graphics g) {
        if (selectedValues.isEmpty()) return;

        Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHints(chart.getRenderingHints());

        Iterator<Point> it = selectedValues.iterator();
        boolean linePainted = false;

        while (it.hasNext()) {
            Point p = it.next();

            if (!linePainted) {
                g2.setPaint(evenPerfPaint);
                g2.setStroke(evenPerfStroke);
                g2.drawLine(p.x, 0, p.x, getHeight());
                g2.setPaint(oddPerfPaint);
                g2.setStroke(oddPerfStroke);
                g2.drawLine(p.x, 0, p.x, getHeight());

                g2.setPaint(markPaint);
                g2.setStroke(markStroke);

                linePainted = true;
            }

            g2.fillOval(p.x - selectionExtent + 1, p.y - selectionExtent + 1,
                        selectionExtent * 2 - 1, selectionExtent * 2 - 1);
        }

    }

    private void vLineBoundsChanged(Set<Point> oldSelection, Set<Point> newSelection) {
        Point oldSel = oldSelection.isEmpty() ? null : oldSelection.iterator().next();
        Point newSel = newSelection.isEmpty() ? null : newSelection.iterator().next();

        if (oldSel != null) repaint(oldSel.x - selectionExtent, 0,
                                             selectionExtent * 2, getHeight());
        if (newSel != null) repaint(newSel.x - selectionExtent, 0,
                                             selectionExtent * 2, getHeight());
    }

    private static void updateSelectedValues(Set<Point> selectedValues,
                                             List<ItemSelection> selectedItems,
                                             ChartComponent chart) {
        selectedValues.clear();
        for (ItemSelection sel : selectedItems) {
            ProfilerXYItemPainter painter = (ProfilerXYItemPainter)chart.getPaintersModel().getPainter(sel.getItem());
            LongRect bounds = painter.getSelectionBounds(sel, chart.getChartContext());
            selectedValues.add(new Point(Utils.checkedInt(bounds.x + (bounds.width >> 2) + 1),
                                         Utils.checkedInt(bounds.y + (bounds.height >> 2) + 1)));
        }
    }


    private class ConfigurationListener extends ChartConfigurationListener.Adapter {
        public void contentsUpdated(long offsetX, long offsetY,
                                    double scaleX, double scaleY,
                                    long lastOffsetX, long lastOffsetY,
                                    double lastScaleX, double lastScaleY,
                                    int shiftX, int shiftY) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        Set<Point> oldSelectedValues = new HashSet(selectedValues);
                        updateSelectedValues(selectedValues, chart.getSelectionModel().getHighlightedItems(), chart);
                        vLineBoundsChanged(oldSelectedValues, selectedValues);
                    }
                });
        }
    }

    private class SelectionListener implements ChartSelectionListener {

        public void selectionModeChanged(int newMode, int oldMode) {}

        public void selectionBoundsChanged(Rectangle newBounds, Rectangle oldBounds) {}

        public void selectedItemsChanged(List<ItemSelection> currentItems,
              List<ItemSelection> addedItems, List<ItemSelection> removedItems) {}

        public void highlightedItemsChanged(List<ItemSelection> currentItems,
              List<ItemSelection> addedItems, List<ItemSelection> removedItems) {
            Set<Point> oldSelectedValues = new HashSet(selectedValues);
            updateSelectedValues(selectedValues, currentItems, chart);
            vLineBoundsChanged(oldSelectedValues, selectedValues);
        }

    }

}
