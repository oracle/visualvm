/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.profiler.snaptracer.impl.timeline;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.List;
import java.util.Objects;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.lib.charts.ChartComponent;
import org.graalvm.visualvm.lib.charts.ChartConfigurationListener;
import org.graalvm.visualvm.lib.charts.ChartContext;
import org.graalvm.visualvm.lib.charts.ChartOverlay;
import org.graalvm.visualvm.lib.charts.ChartSelectionListener;
import org.graalvm.visualvm.lib.charts.ChartSelectionModel;
import org.graalvm.visualvm.lib.charts.ItemSelection;
import org.graalvm.visualvm.lib.charts.swing.Utils;

/**
 *
 * @author Jiri Sedlacek
 */
public class XChartSelectionOverlay extends ChartOverlay {

    private static final boolean FORCE_SPEED = Utils.forceSpeed();

    private ChartComponent chart;
    private int selectionMode;
    private Rectangle selectionBounds;
    private Rectangle oldSelectionBounds;

    private SelectionListener selectionListener;
    private ConfigurationListener configurationListener;

    private boolean renderingOptimized;

    private Paint linePaint;
    private Stroke lineStroke;
    private int lineWidth = -1;

    private Paint fillPaint;

    private Stroke oddPerfStroke;
    private Stroke evenPerfStroke;

    private boolean drawTop;
    private boolean drawBottom;
    private boolean drawLeft;
    private boolean drawRight;

    private final TimelineSupport support;


    public XChartSelectionOverlay(TimelineSupport support) {
        this.support = support;
        selectionListener = new SelectionListener();
        configurationListener = new ConfigurationListener();
        initDefaultValues();
    }
    

    // --- Public API ----------------------------------------------------------

    public final void registerChart(ChartComponent chart) {
        unregisterListener();
        this.chart = chart;
        selectionMode = chart.getSelectionModel().getSelectionMode();
        registerListener();
    }

    public final void unregisterChart(ChartComponent chart) {
        unregisterListener();
        this.chart = null;
    }


    public final void setRenderingOptimized(boolean renderingOptimized) {
        this.renderingOptimized = renderingOptimized;
    }

    public final boolean isRenderingOptimized() {
        return renderingOptimized;
    }


    public final void setLineStroke(Stroke lineStroke) {
        this.lineStroke = lineStroke;
        lineWidth = -1;
    }

    public final Stroke getLineStroke() {
        return lineStroke;
    }

    public final void setLinePaint(Paint linePaint) {
        this.linePaint = linePaint;
    }

    public final Paint getLinePaint() {
        return linePaint;
    }

    public final void setFillPaint(Paint fillPaint) {
        this.fillPaint = fillPaint;
    }

    public final Paint getFillPaint() {
        return fillPaint;
    }

    public final void setLineMode(boolean drawTop, boolean drawLeft,
                                  boolean drawBottom, boolean drawRight) {
        this.drawTop = drawTop;
        this.drawLeft = drawLeft;
        this.drawBottom = drawBottom;
        this.drawRight = drawRight;
    }


    // --- Private implementation ----------------------------------------------

    private void registerListener() {
        if (chart == null) return;
        chart.getSelectionModel().addSelectionListener(selectionListener);
        chart.addConfigurationListener(configurationListener);
    }

    private void unregisterListener() {
        if (chart == null) return;
        chart.getSelectionModel().removeSelectionListener(selectionListener);
        chart.removeConfigurationListener(configurationListener);
    }

    private void initDefaultValues() {
        setRenderingOptimized(true);

        Color systemSelection = Utils.getSystemSelection();

        setLineStroke(new BasicStroke(1));
        setLinePaint(systemSelection);

        setFillPaint(new Color(systemSelection.getRed(),
                               systemSelection.getGreen(),
                               systemSelection.getBlue(), 80));

        oddPerfStroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] { 6, 6 }, 6);
        evenPerfStroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] { 6, 6 }, 0);

        setLineMode(true, true, true, true);
    }


    private int getLineWidth() {
        if (lineWidth == -1)
            lineWidth = FORCE_SPEED ? 1 :
                        (int)Math.ceil(Utils.getStrokeWidth(lineStroke));
        return lineWidth;
    }

    private Rectangle normalizeRect(Rectangle rect, int border) {
        /*if (chart.fitsWidth()) {
            rect.x = 0;
            rect.width = getWidth();
        } else*/ if (rect.width < 0) {
            rect.x += rect.width;
            rect.width = 0 - rect.width;
        }

        rect.y = 0;
        rect.height = getHeight();

        rect.grow(border, border);

        return rect;
    }


    public void paint(Graphics g) {
        if (selectionBounds == null) return;
        
        Graphics2D g2 = (Graphics2D)g;

        Rectangle bounds = normalizeRect(new Rectangle(selectionBounds), 0);

        if (selectionBounds.width != 0 /*|| selectionBounds.height != 0*/) {

            if (bounds.width == 0 || bounds.height == 0 ||
                chart.fitsWidth() && chart.fitsHeight()) return;

            if (fillPaint != null && !FORCE_SPEED) {
                Rectangle clip = g.getClipBounds();
                if (clip == null) clip = new Rectangle(0, 0, getWidth(), getHeight());

                g2.setPaint(fillPaint);
                g2.fill(clip.intersection(bounds));
            }

            if (!FORCE_SPEED && linePaint != null && lineStroke != null) {
                g2.setPaint(linePaint);
                g2.setStroke(lineStroke);
                drawRect(g2, bounds.x, bounds.y, bounds.width, bounds.height);
            } else if (FORCE_SPEED) {
                g2.setPaint(Color.BLACK);
                g2.setStroke(evenPerfStroke);
                drawRect(g2, bounds.x, bounds.y, bounds.width, bounds.height);
                g2.setPaint(Color.WHITE);
                g2.setStroke(oddPerfStroke);
                drawRect(g2, bounds.x, bounds.y, bounds.width, bounds.height);
            }
        } else {
            if (!FORCE_SPEED) {
                g2.setPaint(selectionMode == ChartSelectionModel.SELECTION_RECT ?
                    fillPaint : linePaint);
                g2.setStroke(lineStroke);
                g.drawLine(bounds.x, bounds.y, bounds.x, bounds.y + bounds.height - 1);
            } else if (FORCE_SPEED) {
                g2.setPaint(Color.BLACK);
                g2.setStroke(evenPerfStroke);
                g.drawLine(bounds.x, bounds.y, bounds.x, bounds.y + bounds.height - 1);
                g2.setPaint(Color.WHITE);
                g2.setStroke(oddPerfStroke);
                g.drawLine(bounds.x, bounds.y, bounds.x, bounds.y + bounds.height - 1);
            }
        }

    }

    private void drawRect(Graphics g, int x, int y, int w, int h) {
        if (drawTop) g.drawLine(x, y, x + w - 1, y);
        if (drawLeft) g.drawLine(x, y, x, y + h - 1);
        if (drawRight) g.drawLine(x + w - 1, y + h - 1, x + w - 1, y);
        if (drawBottom) g.drawLine(x + w - 1, y + h - 1, x, y + h - 1);
    }


    private void updateSelection() {
        oldSelectionBounds = selectionBounds;
        selectionBounds = getCurrentBounds();
        if (selectionMode == ChartSelectionModel.SELECTION_RECT)
            rectBoundsChanged(selectionBounds, oldSelectionBounds, getLineWidth());
    }

    private Rectangle getCurrentBounds() {
        TimelineSelectionManager manager = (TimelineSelectionManager)chart.getSelectionModel();
        int startIndex = manager.getStartIndex();
        int endIndex   = manager.getEndIndex();

        if (startIndex > endIndex) {
            endIndex = startIndex;
            startIndex = manager.getEndIndex();
        }

        if (startIndex == -1) {
            return null;
        } else if (startIndex == endIndex) {
            ChartContext context = chart.getChartContext();
            long timestamp = support.getTimestamp(startIndex);
            int x = Utils.checkedInt(context.getViewX(timestamp));
            return new Rectangle(x, 0, 0, chart.getHeight());
        } else {
            ChartContext context = chart.getChartContext();
            long startTimestamp = support.getTimestamp(startIndex);
            long endTimestamp = support.getTimestamp(endIndex);
            int startX = Utils.checkedInt(context.getViewX(startTimestamp));
            int endX = Utils.checkedInt(context.getViewX(endTimestamp));
            return new Rectangle(startX, 0, endX - startX, chart.getHeight());
        }
    }

    private void rectBoundsChanged(Rectangle newBounds, Rectangle oldBounds, int lineW) {
        if (Objects.equals(newBounds, oldBounds)) return; // No change, return
        
        if (newBounds != null && oldBounds != null) { // Updating changed selection
            if (renderingOptimized) { // Painting just selection changes
                if (newBounds.x == oldBounds.x) {
                    int x1 = Math.min(newBounds.x + newBounds.width, oldBounds.x + oldBounds.width);
                    int x2 = Math.max(newBounds.x + newBounds.width, oldBounds.x + oldBounds.width);
                    paintRect(x1, 0, x2 - x1, getHeight(), lineW);
                } else if (newBounds.x + newBounds.width == oldBounds.x + oldBounds.width) {
                    int x1 = Math.min(newBounds.x, oldBounds.x);
                    int x2 = Math.max(newBounds.x, oldBounds.x);
                    paintRect(x1, 0, x2 - x1, getHeight(), lineW);
                } else {
                    int x1 = Math.min(newBounds.x, oldBounds.x);
                    int x2 = Math.max(newBounds.x + newBounds.width, oldBounds.x + oldBounds.width);
                    paintRect(x1, 0, x2 - x1, getHeight(), lineW);
                }
            } else { // Painting whole selection area
                int x1 = Math.min(newBounds.x, oldBounds.x);
                int x2 = Math.max(newBounds.x + newBounds.width, oldBounds.x + oldBounds.width);
                paintRect(x1, 0, x2 - x1, getHeight(), lineW);
            }
        } else if (oldBounds != null) { // Clearing old selection
            paintRect(oldBounds.x, oldBounds.y, oldBounds.width, oldBounds.height, lineW);
        } else if (newBounds != null) { // Painting new selection
            paintRect(newBounds.x, newBounds.y, newBounds.width, newBounds.height, lineW);
        }
    }

    private void paintRect(int x, int y, int w, int h, int t) {
        if (w != 0 && h != 0) {
            Rectangle rect = new Rectangle(x, y, w, h);
            rect.grow(t, t);
            paintImmediately(rect);
        }
    }


    private class SelectionListener implements ChartSelectionListener {

        private boolean modeChanged = false;

        public void selectionModeChanged(int newMode, int oldMode) {
            selectionMode = newMode;
            modeChanged = true;
        }

        public void selectionBoundsChanged(Rectangle newBounds, Rectangle oldBounds) {
            if (modeChanged) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        modeChanged = false;
                        
                        oldSelectionBounds = selectionBounds;
                        selectionBounds = getCurrentBounds();

                        int lineWidth = getLineWidth();

                        Rectangle rect = normalizeRect(new Rectangle(oldSelectionBounds == null ?
                            getBounds() : oldSelectionBounds), lineWidth);
                        paintImmediately(rect);

                        if (selectionBounds != null) {
                            rect = normalizeRect(new Rectangle(selectionBounds), lineWidth);
                            paintImmediately(rect);
                        }
                    }
                });
            } else {
                updateSelection();
            }
            
        }

        public void highlightedItemsChanged(List<ItemSelection> currentItems,
              List<ItemSelection> addedItems, List<ItemSelection> removedItems) {}

        public void selectedItemsChanged(List<ItemSelection> currentItems,
              List<ItemSelection> addedItems, List<ItemSelection> removedItems) {}

    }


    private class ConfigurationListener extends ChartConfigurationListener.Adapter {
        public void contentsUpdated(long offsetX, long offsetY, double scaleX, double scaleY, long lastOffsetX, long lastOffsetY, double lastScaleX, double lastScaleY, int shiftX, int shiftY) {
            if (lastOffsetX != offsetX || lastScaleX != scaleX || lastScaleY != scaleY)
                updateSelection();
        }
    }

}
