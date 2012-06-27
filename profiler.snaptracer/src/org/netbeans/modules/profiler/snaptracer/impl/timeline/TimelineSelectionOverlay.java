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

import org.netbeans.lib.profiler.charts.ChartOverlay;
import org.netbeans.lib.profiler.charts.ChartSelectionListener;
import org.netbeans.lib.profiler.charts.ItemSelection;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.charts.ChartConfigurationListener;
import org.netbeans.lib.profiler.charts.ChartContext;
import org.netbeans.lib.profiler.charts.swing.Utils;
import org.netbeans.lib.profiler.charts.xy.XYItem;
import org.netbeans.lib.profiler.charts.xy.XYItemSelection;
import org.netbeans.lib.profiler.charts.xy.synchronous.SynchronousXYItemsModel;

/**
 *
 * @author Jiri Sedlacek
 */
final class TimelineSelectionOverlay extends ChartOverlay {

    private TimelineChart chart;
    private TimelineSupport support;

    private int selectionExtent;

    private ConfigurationListener configurationListener;
    private SelectionListener selectionListener;
    private TimeSelectionListener timeSelectionListener;
    private final Set<Point> highlightedValues;
    private final Set<Point> selectedValues;

    private Paint sMarkPaint;
    private Paint sOddPerfPaint;
    private Paint sEvenPerfPaint;

    private Paint hMarkPaint;
    private Paint hOddPerfPaint;
    private Paint hEvenPerfPaint;

    private Stroke markStroke;
    private Stroke oddPerfStroke;
    private Stroke evenPerfStroke;


    TimelineSelectionOverlay() {
        configurationListener = new ConfigurationListener();
        selectionListener = new SelectionListener();
        timeSelectionListener = new TimeSelectionListener();
        highlightedValues = new HashSet();
        selectedValues = new HashSet();
        initDefaultValues();
    }
    

    // --- Internal API --------------------------------------------------------

    final void registerChart(TimelineSupport support) {
        unregisterListener();
        this.support = support;
        this.chart = support.getChart();
        registerListener();
    }

    final void unregisterChart(TimelineSupport support) {
        unregisterListener();
        this.support = null;
        this.chart = null;
    }


    // --- Private implementation ----------------------------------------------

    private void registerListener() {
        if (support == null || chart == null) return;
        chart.addConfigurationListener(configurationListener);
        chart.addRowListener(configurationListener);
        chart.getSelectionModel().addSelectionListener(selectionListener);
        support.addSelectionListener(timeSelectionListener);
    }

    private void unregisterListener() {
        if (support == null || chart == null) return;
        chart.removeConfigurationListener(configurationListener);
        chart.removeRowListener(configurationListener);
        chart.getSelectionModel().removeSelectionListener(selectionListener);
        support.removeSelectionListener(timeSelectionListener);
    }

    private void initDefaultValues() {
        sMarkPaint = new Color(120, 120, 120);
        sOddPerfPaint = new Color(120, 120, 120);
        sEvenPerfPaint = Color.WHITE;

        hMarkPaint = new Color(80, 80, 80);
        hOddPerfPaint = Color.BLACK;
        hEvenPerfPaint = Color.WHITE;

        markStroke = new BasicStroke(2.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        oddPerfStroke = new BasicStroke(1f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL, 0, new float[] { 1.0f, 3.0f }, 0);
        evenPerfStroke = new BasicStroke(1f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL, 0, new float[] { 1.0f, 3.0f }, 2);

        selectionExtent = 3;
    }

    private final Set<Integer> paintedLines = new HashSet();

    public void paint(Graphics g) {
        if (highlightedValues.isEmpty() && selectedValues.isEmpty()) return;

        Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHints(chart.getRenderingHints());

        Iterator<Point> it = selectedValues.iterator();
        paintedLines.clear();
        
        int height = getHeight();
        int extentP = 1 - selectionExtent;
        int extentD = selectionExtent * 2 - 1;

        while (it.hasNext()) {
            Point p = it.next();
            int x = p.x;
            int y = p.y;

            if (y == -1) continue;

            if (!paintedLines.contains(x)) {
                g2.setPaint(sEvenPerfPaint);
                g2.setStroke(evenPerfStroke);
                g2.drawLine(x, 0, x, height);
                g2.setPaint(sOddPerfPaint);
                g2.setStroke(oddPerfStroke);
                g2.drawLine(x, 0, x, height);

                g2.setPaint(sMarkPaint);
                g2.setStroke(markStroke);

                paintedLines.add(x);
            }
            
            if (y - selectionExtent >= 0 && y + selectionExtent <= height)
                g2.fillOval(x + extentP, y + extentP, extentD, extentD);
        }

        it = highlightedValues.iterator();
        paintedLines.clear();

        while (it.hasNext()) {
            Point p = it.next();
            int x = p.x;
            int y = p.y;

            if (y == -1) continue;

            if (!paintedLines.contains(x)) {
                g2.setPaint(hEvenPerfPaint);
                g2.setStroke(evenPerfStroke);
                g2.drawLine(x, 0, x, height);
                g2.setPaint(hOddPerfPaint);
                g2.setStroke(oddPerfStroke);
                g2.drawLine(x, 0, x, height);

                g2.setPaint(hMarkPaint);
                g2.setStroke(markStroke);

                paintedLines.add(x);
            }
            
            if (y - selectionExtent >= 0 && y + selectionExtent <= height)
                g2.fillOval(x + extentP, y + extentP, extentD, extentD);
        }

    }

    private void vLineBoundsChanged(Set<Point> oldSelection, Set<Point> newSelection) {
        SortedSet<Integer> selectionBounds = new TreeSet();
        for (Point p : oldSelection) selectionBounds.add(p.x);
        int selections = selectionBounds.size();
        if (selections == 1) {
            repaint(selectionBounds.first() - selectionExtent,
                    0, selectionExtent * 2, getHeight());
            selectionBounds.clear();
        }

        for (Point p : newSelection) selectionBounds.add(p.x);
        selections = selectionBounds.size();
        if (selections == 1) {
            repaint(selectionBounds.first() - selectionExtent,
                    0, selectionExtent * 2, getHeight());
        } else if (selections > 1) {
            int firstX = selectionBounds.first() - selectionExtent;
            int lastX  = selectionBounds.last() + selectionExtent;
            repaint(firstX, 0, lastX - firstX, getHeight());
        }
    }

    private List<ItemSelection> getSelections() {
        List<ItemSelection> items = new ArrayList();

        Set<Integer> timestamps = support.getSelectedTimestamps();
        SynchronousXYItemsModel model = (SynchronousXYItemsModel)chart.getItemsModel();
        int itemsCount = model.getItemsCount();
        for (int itemIndex = 0; itemIndex < itemsCount; itemIndex++)
            for (int timestamp : timestamps)
                items.add(new XYItemSelection.Default(model.getItem(itemIndex),
                          timestamp, XYItemSelection.DISTANCE_UNKNOWN));

        return items;
    }

    private static void updateValues(Set<Point> values,
                                     List<ItemSelection> selectedItems,
                                     TimelineChart chart) {
        values.clear();
        for (ItemSelection sel : selectedItems) {
            XYItemSelection xySel = (XYItemSelection)sel;
            XYItem item = xySel.getItem();
            TimelineXYPainter painter = (TimelineXYPainter)chart.getPaintersModel().getPainter(item);
            ChartContext context = chart.getChartContext(item);
            long xValue = item.getXValue(xySel.getValueIndex());
            long yValue = item.getYValue(xySel.getValueIndex());
            int xPos = Utils.checkedInt(Math.ceil(context.getViewX(xValue)));
            int yPos = Utils.checkedInt(Math.ceil(painter.getItemView(yValue, item, context)));
            if (xPos >= 0 && xPos <= chart.getWidth()) values.add(new Point(xPos, yPos));
        }
    }


    private class ConfigurationListener extends ChartConfigurationListener.Adapter
                                        implements TimelineChart.RowListener {
        private final Runnable selectionUpdater = new Runnable() {
            public void run() {
                Set<Point> oldSelectedValues = new HashSet(selectedValues);
                updateValues(selectedValues, getSelections(), chart);
                vLineBoundsChanged(oldSelectedValues, selectedValues);
                
                Set<Point> oldValues = new HashSet(highlightedValues);
                updateValues(highlightedValues, chart.getSelectionModel().
                             getHighlightedItems(), chart);
                vLineBoundsChanged(oldValues, highlightedValues);
            }
        };
        public void contentsUpdated(long offsetX, long offsetY,
                                    double scaleX, double scaleY,
                                    long lastOffsetX, long lastOffsetY,
                                    double lastScaleX, double lastScaleY,
                                    int shiftX, int shiftY) {
            if (highlightedValues.isEmpty() && !support.isTimestampSelection(true)) return;
            if (lastOffsetX != offsetX || lastOffsetY != offsetY ||
                scaleX != lastScaleX || scaleY != lastScaleY)
                SwingUtilities.invokeLater(selectionUpdater);
        }
        public void rowsAdded(List<TimelineChart.Row> rows) { selectionUpdater.run(); };

        public void rowsRemoved(List<TimelineChart.Row> rows) { selectionUpdater.run(); };

        public void rowsResized(List<TimelineChart.Row> rows) { selectionUpdater.run(); };
    }

    private class SelectionListener implements ChartSelectionListener {

        public void selectionModeChanged(int newMode, int oldMode) {}

        public void selectionBoundsChanged(Rectangle newBounds, Rectangle oldBounds) {
        }

        public void selectedItemsChanged(List<ItemSelection> currentItems,
              List<ItemSelection> addedItems, List<ItemSelection> removedItems) {
        }

        public void highlightedItemsChanged(List<ItemSelection> currentItems,
              List<ItemSelection> addedItems, List<ItemSelection> removedItems) {
            Set<Point> oldHighlightedValues = new HashSet(highlightedValues);
            updateValues(highlightedValues, currentItems, chart);
            vLineBoundsChanged(oldHighlightedValues, highlightedValues);
        }

    }

    private class TimeSelectionListener implements TimelineSupport.SelectionListener {
        
        public void intervalsSelectionChanged() {}

        public void indexSelectionChanged() {}

        public void timeSelectionChanged(boolean timestampsSelected, boolean justHovering) {
            Set<Point> oldSelectedValues = new HashSet(selectedValues);
            updateValues(selectedValues, getSelections(), chart);
            vLineBoundsChanged(oldSelectedValues, selectedValues);
        }

    }

}
