/*
 *  Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
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
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 */

package org.graalvm.visualvm.modules.tracer.impl.timeline;

import org.graalvm.visualvm.modules.tracer.impl.options.TracerOptions;
import org.graalvm.visualvm.modules.tracer.impl.swing.ScrollBar;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.lib.charts.ChartConfigurationListener;
import org.graalvm.visualvm.lib.charts.ChartSelectionModel;
import org.graalvm.visualvm.lib.charts.Timeline;
import org.graalvm.visualvm.lib.charts.swing.Utils;
import org.graalvm.visualvm.lib.charts.xy.synchronous.SynchronousXYItemsModel;
import org.openide.util.ImageUtilities;

/**
 *
 * @author Jiri Sedlacek
 */
final class ChartPanel extends JPanel {

    private static final String ZOOM_IN_STRING = "Zoom in"; //"Zoom In (Mouse Wheel)";
    private static final String ZOOM_OUT_STRING = "Zoom out"; //"Zoom Out (Mouse Wheel)";
    private static final String FIXED_SCALE_STRING = "Fixed scale"; //"Fixed Scale (Mouse Wheel Click)";
    private static final String SCALE_TO_FIT_STRING = "Scale to fit"; //"Scale To Fit (Mouse Wheel Click)";

    private static final Icon ZOOM_IN_ICON = new ImageIcon(ImageUtilities.loadImage(
            "org/graalvm/visualvm/modules/tracer/impl/resources/zoomIn.png")); // NOI18N
    private static final Icon ZOOM_OUT_ICON = new ImageIcon(ImageUtilities.loadImage(
            "org/graalvm/visualvm/modules/tracer/impl/resources/zoomOut.png")); // NOI18N
    private static final Icon FIXED_SCALE_ICON = new ImageIcon(ImageUtilities.loadImage(
            "org/graalvm/visualvm/modules/tracer/impl/resources/zoom.png")); // NOI18N
    private static final Icon SCALE_TO_FIT_ICON = new ImageIcon(ImageUtilities.loadImage(
            "org/graalvm/visualvm/modules/tracer/impl/resources/scaleToFit.png")); // NOI18N
    private static final Icon ZMWHEEL_ICON = new ImageIcon(ImageUtilities.loadImage(
            "org/graalvm/visualvm/modules/tracer/impl/resources/zmwheel.png")); // NOI18N
    private static final Icon HMWHEEL_ICON = new ImageIcon(ImageUtilities.loadImage(
            "org/graalvm/visualvm/modules/tracer/impl/resources/hmwheel.png")); // NOI18N
    private static final Icon VMWHEEL_ICON = new ImageIcon(ImageUtilities.loadImage(
            "org/graalvm/visualvm/modules/tracer/impl/resources/vmwheel.png")); // NOI18N


    private final TimelineChart chart;

    private ZoomInAction zoomInAction;
    private ZoomOutAction zoomOutAction;
    private ToggleViewAction toggleViewAction;

    private final JScrollBar hScrollBar;
    private final JScrollBar vScrollBar;
    private final MouseWheelListener defaultWheelHandler;
    private AbstractButton mouseZoom;
    private AbstractButton mouseHScroll;
    private AbstractButton mouseVScroll;


    ChartPanel(TimelineChart chart, TimelineSupport support) {
        this.chart = chart;

        boolean speed = Utils.forceSpeed();

        chart.setBackground(Color.WHITE);
        if (speed && TracerOptions.getInstance().isRowsSelectionEnabled())
            chart.addPreDecorator(new RowBackgroundDecorator(chart));
        chart.addPreDecorator(new RowBoundsDecorator(chart));
        chart.addPostDecorator(new RowForegroundDecorator(chart,
                TracerOptions.getInstance().isRowsDecorationEnabled(),
                speed ? false : TracerOptions.getInstance().isRowsSelectionEnabled()));

        TimelineSelectionOverlay selectionOverlay = new TimelineSelectionOverlay();
        chart.addOverlayComponent(selectionOverlay);
        selectionOverlay.registerChart(support);

        ChartSelectionModel selectionModel = chart.getSelectionModel();
        if (selectionModel != null) {
            selectionModel.setHoverMode(ChartSelectionModel.HOVER_EACH_NEAREST);
            selectionModel.setMoveMode(ChartSelectionModel.SELECTION_LINE_V);
        }
        
        TimelineAxis axis = new TimelineAxis(chart, support);
        
        hScrollBar = new ScrollBar(JScrollBar.HORIZONTAL);
        hScrollBar.addAdjustmentListener(new AdjustmentListener() {
            public void adjustmentValueChanged(AdjustmentEvent e) {
                if (hScrollBar.getValueIsAdjusting())
                    ChartPanel.this.chart.updateSelection(false, hScrollBar);
                else
                    ChartPanel.this.chart.updateSelection(true, hScrollBar);
            }
        });
        chart.attachHorizontalScrollBar(hScrollBar);

        vScrollBar = new ScrollBar(JScrollBar.VERTICAL);
        vScrollBar.addAdjustmentListener(new AdjustmentListener() {
            public void adjustmentValueChanged(AdjustmentEvent e) {
                if (vScrollBar.getValueIsAdjusting())
                    ChartPanel.this.chart.updateSelection(false, vScrollBar);
                else
                    ChartPanel.this.chart.updateSelection(true, vScrollBar);
            }
        });
        chart.attachVerticalScrollBar(vScrollBar);
        
        defaultWheelHandler = chart.getMouseWheelListeners()[0];

        chart.addConfigurationListener(new VisibleBoundsListener());

        setOpaque(false);
        setLayout(new BorderLayout());
        add(axis, BorderLayout.NORTH);
        add(chart, BorderLayout.CENTER);
        add(vScrollBar, BorderLayout.EAST);
        add(hScrollBar, BorderLayout.SOUTH);

        resetChart();
    }


    // --- Internal interface --------------------------------------------------

    void resetChart() {
        chart.setScale(0.02, 1);
        chart.setOffset(0, 0);
    }

    boolean resetSelection() {
        if (chart.isRowSelection()) {
            chart.clearRowsSelection();
            return true;
        } else {
            return false;
        }
    }


    Action zoomInAction() {
        if (zoomInAction == null) zoomInAction = new ZoomInAction();
        return zoomInAction;
    }

    Action zoomOutAction() {
        if (zoomOutAction == null) zoomOutAction = new ZoomOutAction();
        return zoomOutAction;
    }

    Action toggleViewAction() {
        if (toggleViewAction == null) {
            toggleViewAction = new ToggleViewAction();
            if (TracerOptions.getInstance().getZoomMode().equals(TracerOptions.
                    SCALE_TO_FIT)) toggleViewAction.actionPerformed(null);
        }
        return toggleViewAction;
    }


    AbstractButton mouseZoom() {
        if (mouseZoom == null) {
            mouseZoom = new OneWayToggleButton(ZMWHEEL_ICON, "Mouse wheel zooms") {
                protected void performAction() { mouseZoomImpl(); }
            };
            if (TracerOptions.getInstance().getMouseWheelAction().equals(
                    TracerOptions.MOUSE_WHEEL_ZOOMS)) {
                mouseZoom.setSelected(true);
                mouseZoomImpl();
            }
        }
        return mouseZoom;
    }

    AbstractButton mouseHScroll() {
        if (mouseHScroll == null) {
            mouseHScroll = new OneWayToggleButton(HMWHEEL_ICON, "Mouse wheel scrolls horizontally") {
                protected void performAction() { mouseHScrollImpl(); }
            };
            if (TracerOptions.getInstance().getMouseWheelAction().equals(
                    TracerOptions.MOUSE_WHEEL_HSCROLLS)) {
                mouseHScroll.setSelected(true);
                mouseHScrollImpl();
            }
        }
        return mouseHScroll;
    }

    AbstractButton mouseVScroll() {
        if (mouseVScroll == null) {
            mouseVScroll = new OneWayToggleButton(VMWHEEL_ICON, "Mouse wheel scrolls vertically") {
                protected void performAction() { mouseVScrollImpl(); }
            };
            if (TracerOptions.getInstance().getMouseWheelAction().equals(
                    TracerOptions.MOUSE_WHEEL_VSCROLLS)) {
                mouseVScroll.setSelected(true);
                mouseVScrollImpl();
            }
        }
        return mouseVScroll;
    }


    // --- Mouse wheel handling ------------------------------------------------

    void vScroll(MouseWheelEvent e) {
        scroll(vScrollBar, e);
    }

    private void mouseZoomImpl() {
        clearWheelHandlers();
        chart.setMouseZoomingEnabled(true);
        chart.addMouseWheelListener(defaultWheelHandler);
    }

    private void mouseHScrollImpl() {
        chart.setMouseZoomingEnabled(false);
        clearWheelHandlers();
        setWheelScrollHandler(hScrollBar);
    }

    private void mouseVScrollImpl() {
        chart.setMouseZoomingEnabled(false);
        clearWheelHandlers();
        setWheelScrollHandler(vScrollBar);
    }

    private void clearWheelHandlers() {
        MouseWheelListener[] handlers = chart.getMouseWheelListeners();
        for (MouseWheelListener handler : handlers)
            chart.removeMouseWheelListener(handler);
    }

    private void setWheelScrollHandler(final JScrollBar scrollBar) {
        chart.addMouseWheelListener(new MouseWheelListener() {
            public void mouseWheelMoved(MouseWheelEvent e) {
                scroll(scrollBar, e);
            }
        });
    }

    private static void scroll(JScrollBar scrollBar, MouseWheelEvent e) {
        if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
            int unitsToScroll = e.getUnitsToScroll();
            if (unitsToScroll != 0) {
                int direction = unitsToScroll < 0 ? -1 : 1;
                int increment = scrollBar.getUnitIncrement(direction);
                int oldValue = scrollBar.getValue();
                int newValue = oldValue + increment * unitsToScroll;
                newValue = Math.max(Math.min(newValue, scrollBar.getMaximum() -
                        scrollBar.getVisibleAmount()), scrollBar.getMinimum());
                if (oldValue != newValue) scrollBar.setValue(newValue);
            }
        }
    }


    // --- Actions support -----------------------------------------------------

    private class ZoomInAction extends AbstractAction {

        private static final int ONE_SECOND_WIDTH_THRESHOLD = 200;

        public ZoomInAction() {
            super();

            putValue(SHORT_DESCRIPTION, ZOOM_IN_STRING);
            putValue(SMALL_ICON, ZOOM_IN_ICON);

            updateAction();
        }

        public void actionPerformed(ActionEvent e) {
            boolean followsWidth = chart.currentlyFollowingDataWidth();
            chart.zoom(getWidth() / 2, getHeight() / 2, 2d);
            if (followsWidth) chart.setOffset(chart.maxOffsetX(), chart.getOffsetY());

            chart.repaintDirty();
        }

        private void updateAction() {
            Timeline timeline = ((SynchronousXYItemsModel)chart.getItemsModel()).getTimeline();
            setEnabled(timeline.getTimestampsCount() > 1 && !chart.fitsWidth() &&
                       chart.viewWidth(1000) < ONE_SECOND_WIDTH_THRESHOLD);
        }

    }

    private class ZoomOutAction extends AbstractAction {

        private static final float USED_CHART_WIDTH_THRESHOLD = 0.33f;

        public ZoomOutAction() {
            super();

            putValue(SHORT_DESCRIPTION, ZOOM_OUT_STRING);
            putValue(SMALL_ICON, ZOOM_OUT_ICON);

            updateAction();
        }

        public void actionPerformed(ActionEvent e) {
            boolean followsWidth = chart.currentlyFollowingDataWidth();
            chart.zoom(getWidth() / 2, getHeight() / 2, 0.5d);
            if (followsWidth) chart.setOffset(chart.maxOffsetX(), chart.getOffsetY());

            chart.repaintDirty();
        }

        private void updateAction() {
            Timeline timeline = ((SynchronousXYItemsModel)chart.getItemsModel()).getTimeline();
            setEnabled(timeline.getTimestampsCount() > 0 && !chart.fitsWidth() &&
                       chart.getContentsWidth() > getWidth() * USED_CHART_WIDTH_THRESHOLD);
        }

    }

    private class ToggleViewAction extends AbstractAction {

        private long origOffsetX  = -1;
        private double origScaleX = -1;

        public ToggleViewAction() {
            super();
            updateAction();
            addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isMiddleMouseButton(e))
                        actionPerformed(null);
                }
            });
        }

        public void actionPerformed(ActionEvent e) {
            boolean fitsWidth = chart.fitsWidth();

            if (!fitsWidth) {
                origOffsetX = chart.getOffsetX();
                if (chart.tracksDataWidth() && origOffsetX == chart.maxOffsetX())
                    origOffsetX = Long.MAX_VALUE;
                origScaleX  = chart.getScaleX();
            }

            chart.setFitsWidth(!fitsWidth);

            if (fitsWidth && origOffsetX != -1 && origScaleX != -1) {
                chart.setScale(origScaleX, chart.getScaleY());
                chart.setOffset(origOffsetX, chart.getOffsetY());
            }

            updateAction();
            if (zoomInAction != null) zoomInAction.updateAction();
            if (zoomOutAction != null) zoomOutAction.updateAction();

            chart.repaintDirty();

        }

        private void updateAction() {
            boolean fitsWidth = chart.fitsWidth();
            Icon icon = fitsWidth ? FIXED_SCALE_ICON : SCALE_TO_FIT_ICON;
            String name = fitsWidth ? FIXED_SCALE_STRING : SCALE_TO_FIT_STRING;
            putValue(SHORT_DESCRIPTION, name);
            putValue(SMALL_ICON, icon);
        }

    }


    private static abstract class OneWayToggleButton extends JToggleButton {

        private boolean action;


        public OneWayToggleButton(Icon icon, String toolTip) {
            super(icon);
            setToolTipText(toolTip);
        }

        protected void processMouseEvent(MouseEvent e) {
            if (isSelected()) {
                e.consume();
                action = false;
            } else {
                action = true;
            }
            super.processMouseEvent(e);
        }

        protected void processKeyEvent(KeyEvent e) {
            if (isSelected()) {
                e.consume();
                action = false;
            } else {
                action = true;
            }
            super.processKeyEvent(e);
        }

        protected final void fireActionPerformed(ActionEvent e) {
            if (action) performAction();
        }

        protected abstract void performAction();

    }


    // --- ChartConfigurationListener implementation ---------------------------

    private class VisibleBoundsListener extends ChartConfigurationListener.Adapter {

        public void dataBoundsChanged(long dataOffsetX, long dataOffsetY,
                                      long dataWidth, long dataHeight,
                                      long oldDataOffsetX, long oldDataOffsetY,
                                      long oldDataWidth, long oldDataHeight) {

            if (zoomInAction != null) zoomInAction.updateAction();
            if (zoomOutAction != null) zoomOutAction.updateAction();
        }

        public void scaleChanged(double oldScaleX, double oldScaleY,
                                 double newScaleX, double newScaleY) {

            if (zoomInAction != null) zoomInAction.updateAction();
            if (zoomOutAction != null) zoomOutAction.updateAction();
        }
    }

}
