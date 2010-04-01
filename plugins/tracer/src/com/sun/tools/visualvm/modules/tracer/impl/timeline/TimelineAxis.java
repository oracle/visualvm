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

import com.sun.tools.visualvm.modules.tracer.impl.swing.EnhancedLabelRenderer;
import com.sun.tools.visualvm.modules.tracer.impl.swing.HeaderLabel;
import com.sun.tools.visualvm.modules.tracer.impl.swing.HeaderPanel;
import com.sun.tools.visualvm.modules.tracer.impl.swing.LegendFont;
import com.sun.tools.visualvm.modules.tracer.impl.swing.TimelineMarksPainter;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.netbeans.lib.profiler.charts.ChartConfigurationListener;
import org.netbeans.lib.profiler.charts.ChartContext;
import org.netbeans.lib.profiler.charts.ChartSelectionListener;
import org.netbeans.lib.profiler.charts.ItemSelection;
import org.netbeans.lib.profiler.charts.Timeline;
import org.netbeans.lib.profiler.charts.axis.AxisComponent;
import org.netbeans.lib.profiler.charts.axis.AxisMark;
import org.netbeans.lib.profiler.charts.axis.TimeAxisUtils;
import org.netbeans.lib.profiler.charts.axis.TimelineMarksComputer;
import org.netbeans.lib.profiler.charts.swing.Utils;
import org.netbeans.lib.profiler.charts.xy.synchronous.SynchronousXYItemsModel;
import org.openide.util.ImageUtilities;

/**
 *
 * @author Jiri Sedlacek
 */
final class TimelineAxis extends JPanel {

    private final HeaderRenderer painter;
    private final AxisComponent axis;
    private final MarksComponent marks;

    private int preferredHeight;


    TimelineAxis(TimelineChart chart, TimelineSupport support) {

        super(null);

        painter = new HeaderRenderer();
        
        Timeline timeline = ((SynchronousXYItemsModel)chart.getItemsModel()).getTimeline();
        axis = new Axis(chart, new MarksComputer(timeline, chart.getChartContext()));

        marks = new MarksComponent(support);

        preferredHeight = HeaderLabel.DEFAULT_HEIGHT;

        add(marks);
        add(axis);
        add(painter);

        chart.addConfigurationListener(new ChartConfigurationListener.Adapter() {

            public void contentsUpdated(long offsetX, long offsetY,
                                        double scaleX, double scaleY,
                                        long lastOffsetX, long lastOffsetY,
                                        double lastScaleX, double lastScaleY,
                                        int shiftX, int shiftY) {
                
                if (lastOffsetX != offsetX || lastScaleX != scaleX) marks.refresh();
            }

        });

        chart.getSelectionModel().addSelectionListener(new ChartSelectionListener() {

            public void selectionModeChanged(int i, int i1) {}

            public void selectionBoundsChanged(Rectangle rctngl, Rectangle rctngl1) {}

            public void highlightedItemsChanged(List<ItemSelection> list,
                                                List<ItemSelection> list1,
                                                List<ItemSelection> list2) {}

            public void selectedItemsChanged(List<ItemSelection> currentItems,
              List<ItemSelection> addedItems, List<ItemSelection> removedItems) {
                
                marks.refresh();
                marks.repaint();
            }
            
        });

    }


    public boolean isOptimizedDrawingEnabled() {
        return false;
    }

    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        size.height = preferredHeight;
        return size;
    }


    public void validate() {}
    public void doLayout() {}

    public void reshape(int x, int y, int width, int height) {
        super.reshape(x, y, width, height);
        painter.reshape(0, 0, width, height);
        axis.reshape(1, 1, width - 2, height - 2);
        marks.reshape(0, 0, width, height);
    }


    private static class HeaderRenderer extends HeaderPanel {

        private Image offscreen;

        public void reshape(int x, int y, int width, int height) {
            if (getWidth() != width || getHeight() != height) offscreen = null;
            super.reshape(x, y, width, height);
        }

        long total = 0;
        int count = 0;

        public void validate() {}

        public void paint(Graphics g) {
            if (offscreen == null) {
                offscreen = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
                super.paint(offscreen.getGraphics());
            }
            g.drawImage(offscreen, 0, 0, null);
        }

    }


    private static class MarksComponent extends JComponent {

        private static final Image MARK = ImageUtilities.loadImage(
                "com/sun/tools/visualvm/modules/tracer/impl/resources/timeMark.png");  // NOI18N
        private static final int MARK_EXTENT = MARK.getWidth(null) / 2;
        private static final int MARK_HEIGHT = MARK.getHeight(null);

        private final TimelineSupport support;

        private final EnhancedLabelRenderer timeRenderer;
        private final int timeRendererHeight;
        private final Format timeFormat;

        private final List<Integer> selections = new ArrayList();
        private final List<Long> times = new ArrayList();
        private final int markExtent = 2;


        MarksComponent(TimelineSupport support) {
            this.support = support;

            timeRenderer = new EnhancedLabelRenderer();
            timeRenderer.setFont(new LegendFont());
            timeRenderer.setBackground(Color.WHITE);
            timeRenderer.setMargin(new Insets(1, 2, 1, 2));
            timeRenderer.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            timeRendererHeight = timeRenderer.getPreferredSize().height;
            timeFormat = new SimpleDateFormat(TimeAxisUtils.getFormatString(1, 1, 1));

            setOpaque(false);
        }
        
        public void validate() {}
        public void doLayout() {}


        void refresh() {
            int[] selectedIndexes = support.getSelectedTimestamps();
            if (selectedIndexes.length == 0 && selections.isEmpty()) return;

            TimelineChart chart = support.getChart();
            SynchronousXYItemsModel model = (SynchronousXYItemsModel)chart.getItemsModel();
            Timeline timeline = model.getTimeline();
            ChartContext context = chart.getChartContext();
            selections.clear();
            times.clear();

            for (int selectedIndex : selectedIndexes) {
                long time = timeline.getTimestamp(selectedIndex);
                int x = Utils.checkedInt(context.getViewX(time));
                if (x > -markExtent && x < getWidth() + markExtent) {
                    selections.add(x);
                    times.add(time);
                }
            }
        }


        public void paint(Graphics g) {
            if (selections == null || selections.isEmpty()) return;

            int h = getHeight();
            int my = h - 5 - MARK_HEIGHT;
            int py = (h - timeRendererHeight) / 2;
            int selectionsCount = selections.size();
            
            for (int i = 0; i < selectionsCount; i++)
                paintMark(g, selections.get(i), my, py, times.get(i));
        }

        private void paintMark(Graphics g, int x, int my, int py, long time) {
            g.drawImage(MARK, x - MARK_EXTENT + 1, my, null);
            timeRenderer.setText(timeFormat.format(time));
            timeRenderer.setLocation(x + MARK_EXTENT + TimelineTooltipOverlay.TOOLTIP_OFFSET + 1, py);
            timeRenderer.paint(g);
        }

    }


    private static class MarksComputer extends TimelineMarksComputer {

        MarksComputer(Timeline timeline, ChartContext context) {
            super(timeline, context, SwingConstants.HORIZONTAL);
        }

        void refresh() {
            refreshConfiguration();
        }

    }


    private static class Axis extends AxisComponent {

        private static final int LAF_OFFSET = resolveOffset();

        private final Paint meshPaint = Utils.checkedColor(new Color(180, 180, 180, 50));
        private final Stroke meshStroke = new BasicStroke(1);

        private final TimelineChart chart;
        private final MarksComputer marksComputer;
        private final TimelineMarksPainter marksPainter;
        private boolean hadTicks = false;

        private final Runnable repainter;


        Axis(TimelineChart chart, MarksComputer marksComputer) {

            super(chart, marksComputer, null, SwingConstants.NORTH,
                  AxisComponent.MESH_FOREGROUND);

            this.chart = chart;
            this.marksComputer = marksComputer;
            this.marksPainter = new TimelineMarksPainter();

            repainter = new Runnable() {
                public void run() { Axis.this.chart.invalidateRepaint(); }
            };
        }


        public void validate() {}
        public void doLayout() {}


        public void paint(Graphics g) {
            Rectangle clip = g.getClipBounds();
            if (clip == null) clip = new Rectangle(0, 0, getWidth(), getHeight());

            marksComputer.refresh();

            paintHorizontalAxis(g, clip);
        }

        protected void paintHorizontalMesh(Graphics2D g, Rectangle clip, Rectangle chartMask) {
            Iterator<AxisMark> marks =
                    marksComputer.marksIterator(chartMask.x, chartMask.x + chartMask.width);

            boolean hasTicks = false;

            while (marks.hasNext()) {
                hasTicks = true;

                AxisMark mark = marks.next();
                int x = mark.getPosition();

                g.setPaint(meshPaint);
                g.setStroke(meshStroke);
                g.drawLine(x, chartMask.y, x, chartMask.y + chartMask.height);
            }

            if (!hadTicks && hasTicks) SwingUtilities.invokeLater(repainter);
            hadTicks = hasTicks;
        }

        protected void paintHorizontalAxis(Graphics g, Rectangle clip) {
            int viewStart = -1; // -1: extra 1px for axis
            int viewEnd = viewStart + chart.getWidth() + 2; // +2 extra 1px + 1px for axis

            Iterator<AxisMark> marks = marksComputer.marksIterator(viewStart, viewEnd);

            int lZeroOffset = chart.isRightBased() ? 0 : 1;
            int rZeroOffset = chart.isRightBased() ? 1 : 0;

            while (marks.hasNext()) {
                AxisMark mark = marks.next();

                int x = mark.getPosition() - 1;

                if (x < -1 - lZeroOffset ||
                    x >= -1 + chart.getWidth() + rZeroOffset) continue;

                TimelineMarksPainter painter =
                        (TimelineMarksPainter)marksPainter.getPainter(mark);
                Dimension painterSize = painter.getPreferredSize();
                int markOffsetX = painterSize.width / 2;

                if (x + markOffsetX < clip.x ||
                    x - markOffsetX >= clip.x + clip.width) continue;

                g.setColor(getForeground());
                g.drawLine(x, 1, x, 3);
                
                int markOffsetY = (getHeight() - painterSize.height) / 2 + LAF_OFFSET;
                painter.setLocation(x - markOffsetX, markOffsetY);
                painter.paint(g);
            }
        }

        private static int resolveOffset() {
            String uiId = UIManager.getLookAndFeel().getID();
            if ("Windows".equals(uiId) || // NOI18N
                "Metal".equals(uiId) || // NOI18N
                "GTK".equals(uiId)) return 1; // NOI18N
            return 0;
        }

    }

}
