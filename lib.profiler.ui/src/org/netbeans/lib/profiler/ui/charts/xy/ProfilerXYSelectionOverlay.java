/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2015 Oracle and/or its affiliates. All rights reserved.
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
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2015 Sun Microsystems, Inc.
 */
package org.netbeans.lib.profiler.ui.charts.xy;

import org.netbeans.lib.profiler.charts.ChartComponent;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.charts.ChartConfigurationListener;
import org.netbeans.lib.profiler.charts.swing.LongRect;
import org.netbeans.lib.profiler.charts.swing.Utils;

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
