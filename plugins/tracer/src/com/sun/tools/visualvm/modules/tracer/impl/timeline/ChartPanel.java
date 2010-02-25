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

import com.sun.tools.visualvm.modules.tracer.impl.swing.HeaderLabel;
import com.sun.tools.visualvm.modules.tracer.impl.swing.ScrollBar;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
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
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.charts.ChartConfigurationListener;
import org.netbeans.lib.profiler.charts.ChartSelectionModel;
import org.netbeans.lib.profiler.charts.Timeline;
import org.netbeans.lib.profiler.charts.axis.TimeMarksPainter;
import org.netbeans.lib.profiler.charts.axis.TimelineMarksComputer;
import org.netbeans.lib.profiler.charts.xy.synchronous.SynchronousXYItemsModel;
import org.openide.util.ImageUtilities;

/**
 *
 * @author Jiri Sedlacek
 */
class ChartPanel extends JPanel {

    private static final String ZOOM_IN_STRING = "Zoom in"; //"Zoom In (Mouse Wheel)";
    private static final String ZOOM_OUT_STRING = "Zoom out"; //"Zoom Out (Mouse Wheel)";
    private static final String FIXED_SCALE_STRING = "Fixed scale"; //"Fixed Scale (Mouse Wheel Click)";
    private static final String SCALE_TO_FIT_STRING = "Scale to fit"; //"Scale To Fit (Mouse Wheel Click)";

    private static final Icon ZOOM_IN_ICON = new ImageIcon(ImageUtilities.loadImage(
            "com/sun/tools/visualvm/modules/tracer/impl/resources/zoomIn.png")); // NOI18N
    private static final Icon ZOOM_OUT_ICON = new ImageIcon(ImageUtilities.loadImage(
            "com/sun/tools/visualvm/modules/tracer/impl/resources/zoomOut.png")); // NOI18N
    private static final Icon FIXED_SCALE_ICON = new ImageIcon(ImageUtilities.loadImage(
            "com/sun/tools/visualvm/modules/tracer/impl/resources/zoom.png")); // NOI18N
    private static final Icon SCALE_TO_FIT_ICON = new ImageIcon(ImageUtilities.loadImage(
            "com/sun/tools/visualvm/modules/tracer/impl/resources/scaleToFit.png")); // NOI18N
    private static final Icon ZMWHEEL_ICON = new ImageIcon(ImageUtilities.loadImage(
            "com/sun/tools/visualvm/modules/tracer/impl/resources/zmwheel.png")); // NOI18N
    private static final Icon HMWHEEL_ICON = new ImageIcon(ImageUtilities.loadImage(
            "com/sun/tools/visualvm/modules/tracer/impl/resources/hmwheel.png")); // NOI18N
    private static final Icon VMWHEEL_ICON = new ImageIcon(ImageUtilities.loadImage(
            "com/sun/tools/visualvm/modules/tracer/impl/resources/vmwheel.png")); // NOI18N


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


    ChartPanel(TimelineChart chart) {
        this.chart = chart;

        chart.setBackground(Color.WHITE);
        chart.addPreDecorator(new RowBoundsDecorator(chart));
        //            chart.addPreDecorator(new RowPostDecorator(chart));
        chart.addPostDecorator(new RowGradientDecorator(chart));
        //            chart.addOverlayComponent(new RowUnitsOverlay(chart));

        TimelineSelectionOverlay selectionOverlay = new TimelineSelectionOverlay();
        chart.addOverlayComponent(selectionOverlay);
        selectionOverlay.registerChart(chart);
        enableSelection(chart);

        TimeMarksPainter marksPainter = new TimeMarksPainter() {
            public Dimension getPreferredSize() {
                Dimension size = super.getPreferredSize();
                size.height = HeaderLabel.DEFAULT_HEIGHT;
                return size;
            }
        };
        Font font = marksPainter.getFont();
        marksPainter.setFont(font.deriveFont(Font.PLAIN, font.getSize() - 2));
        Timeline timeline = ((SynchronousXYItemsModel) chart.getItemsModel()).getTimeline();
        TimelineMarksComputer marksComputer = new TimelineMarksComputer(timeline,
                chart.getChartContext(), SwingConstants.HORIZONTAL);
        TimelineAxis axis = new TimelineAxis(chart, marksComputer, marksPainter);
        
        hScrollBar = new ScrollBar(JScrollBar.HORIZONTAL);
        vScrollBar = new ScrollBar(JScrollBar.VERTICAL);
        chart.attachHorizontalScrollBar(hScrollBar);
        chart.attachVerticalScrollBar(vScrollBar);
        
        defaultWheelHandler = chart.getMouseWheelListeners()[0];
//        chart.removeMouseWheelListener(defaultWheelHandler);
        mouseZoomImpl();

        chart.addConfigurationListener(new VisibleBoundsListener());

        setOpaque(false);
        setLayout(new BorderLayout());
        add(axis, BorderLayout.NORTH);
        add(chart, BorderLayout.CENTER);
        add(vScrollBar, BorderLayout.EAST);
        add(hScrollBar, BorderLayout.SOUTH);
        reset();
    }


    // --- Internal interface --------------------------------------------------

    void reset() {
        chart.setScale(0.02, 1);
        chart.setOffset(0, 0);
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
        if (toggleViewAction == null) toggleViewAction = new ToggleViewAction();
        return toggleViewAction;
    }


    AbstractButton mouseZoom() {
        if (mouseZoom == null) {
            mouseZoom = new OneWayToggleButton(ZMWHEEL_ICON, "Mouse wheel zooms") {
                protected void performAction() { mouseZoomImpl(); }
            };
            mouseZoom.setSelected(true);
        }
        return mouseZoom;
    }

    AbstractButton mouseHScroll() {
        if (mouseHScroll == null) mouseHScroll = new OneWayToggleButton(HMWHEEL_ICON, "Mouse wheel scrolls horizontally") {
            protected void performAction() { mouseHScrollImpl(); }
        };
        return mouseHScroll;
    }

    AbstractButton mouseVScroll() {
        if (mouseVScroll == null) mouseVScroll = new OneWayToggleButton(VMWHEEL_ICON, "Mouse wheel scrolls vertically") {
            protected void performAction() { mouseVScrollImpl(); }
        };
        return mouseVScroll;
    }


    static void enableSelection(TimelineChart chart) {
        chart.getSelectionModel().setHoverMode(ChartSelectionModel.HOVER_EACH_NEAREST);
        chart.getSelectionModel().setMoveMode(ChartSelectionModel.SELECTION_LINE_V);
    }

    static void disableSelection(TimelineChart chart) {
        chart.getSelectionModel().setHoverMode(ChartSelectionModel.HOVER_NONE);
        chart.getSelectionModel().setMoveMode(ChartSelectionModel.SELECTION_NONE);
    }


    // --- Mouse wheel handling ------------------------------------------------

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
                // Change the ScrollBar value
                if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
                    int unitsToScroll = e.getUnitsToScroll();
                    int direction = unitsToScroll < 0 ? -1 : 1;
                    if (unitsToScroll != 0) {
                        int increment = scrollBar.getUnitIncrement(direction);
                        int oldValue = scrollBar.getValue();
                        int newValue = oldValue + increment * unitsToScroll;
                        newValue = Math.max(Math.min(newValue, scrollBar.getMaximum() -
                                scrollBar.getVisibleAmount()), scrollBar.getMinimum());
                        if (oldValue != newValue) scrollBar.setValue(newValue);
                    }
                }
            }
        });
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
