/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2007-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.netbeans.lib.profiler.ui.charts.xy;

import org.netbeans.lib.profiler.charts.ChartComponent;
import org.netbeans.lib.profiler.charts.ChartOverlay;
import org.netbeans.lib.profiler.charts.swing.Utils;
import org.netbeans.lib.profiler.charts.ChartConfigurationListener;
import org.netbeans.lib.profiler.charts.ChartContext;
import org.netbeans.lib.profiler.charts.ChartItem;
import org.netbeans.lib.profiler.charts.ChartSelectionListener;
import org.netbeans.lib.profiler.charts.ItemPainter;
import org.netbeans.lib.profiler.charts.ItemSelection;
import org.netbeans.lib.profiler.charts.PaintersModel;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.List;
import javax.swing.Timer;
import org.netbeans.lib.profiler.charts.xy.XYItemSelection;

/**
 *
 * @author Jiri Sedlacek
 */
public class ProfilerXYTooltipOverlay extends ChartOverlay implements ActionListener {

    private static final int TOOLTIP_OFFSET = 15;
    private static final int TOOLTIP_MARGIN = 10;
    private static final int TOOLTIP_RESPONSE = 50;
    private static final int ANIMATION_STEPS = 5;

    private ProfilerXYTooltipPainter tooltipPainter;

    private Timer timer;
    private int currentStep;
    private Point mousePosition;
    private Point targetPosition;


    public ProfilerXYTooltipOverlay(final ChartComponent chart,
                                    ProfilerXYTooltipPainter tooltipPainter) {
        if (chart.getSelectionModel() == null)
            throw new NullPointerException("No ChartSelectionModel set for " + chart); // NOI18N

        if (!Utils.forceSpeed()) {
            timer = new Timer(TOOLTIP_RESPONSE / ANIMATION_STEPS, this);
            timer.setInitialDelay(0);
        }

        setLayout(null);

        this.tooltipPainter = tooltipPainter;
        add(tooltipPainter);
        tooltipPainter.setVisible(false);

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

        chart.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e) {
                mousePosition = e.getPoint();
                updateTooltip(chart);
            }
        });
    }

    public final void setPosition(Point p) {
        if (tooltipPainter != null) {
            if (p == null) {
                if (tooltipPainter.isVisible()) tooltipPainter.setVisible(false);
                if (timer != null) timer.stop();
            } else {
                if (!tooltipPainter.isVisible() || timer == null) {
                    tooltipPainter.setVisible(true);
                    tooltipPainter.setLocation(p);
                } else {
                    currentStep = 0;
                    targetPosition = p;
                    timer.restart();
                }
            }
        }
    }

    public final Point getPosition() {
        if (tooltipPainter == null) return null;
        return tooltipPainter.getLocation();
    }

    public void actionPerformed(ActionEvent e) {
        Point currentPosition = tooltipPainter.getLocation();

        currentPosition.x += (targetPosition.x - currentPosition.x) /
                             (ANIMATION_STEPS - currentStep);
        currentPosition.y += (targetPosition.y - currentPosition.y) /
                             (ANIMATION_STEPS - currentStep);
        tooltipPainter.setLocation(currentPosition);

        if (++currentStep == ANIMATION_STEPS) timer.stop();
    }


    private void updateTooltip(ChartComponent chart) {
        if (mousePosition == null) return;

        List<ItemSelection> highlightedItems =
                chart.getSelectionModel().getHighlightedItems();

        XYItemSelection selection = highlightedItems.isEmpty() ? null :
                                    (XYItemSelection)highlightedItems.get(0);

        if (selection == null ||
            selection.getItem().getValuesCount() <= selection.getValueIndex()) {
            setPosition(null);
        } else {
            tooltipPainter.update(highlightedItems);
            tooltipPainter.setSize(tooltipPainter.getPreferredSize());
            setPosition(highlightedItems, chart.getPaintersModel(), chart.getChartContext());
        }
    }

    private void setPosition(List<ItemSelection> selectedItems, PaintersModel paintersModel, ChartContext chartContext) {
        int tooltipX = -1;
        int tooltipY = mousePosition.y;
        for (ItemSelection selection : selectedItems) {
            ChartItem item = selection.getItem();
            ItemPainter painter = paintersModel.getPainter(item);
            Rectangle bounds = Utils.checkedRectangle(
                               painter.getSelectionBounds(selection,
                               chartContext));
            if (tooltipX == -1) tooltipX += bounds.x + bounds.width / 2;
        }

        setPosition(normalizePosition(new Point(tooltipX, tooltipY)));
    }

    private Point normalizePosition(Point basePoint) {
        int w = getWidth();
        int h = getHeight();
        int cw = tooltipPainter.getWidth();
        int ch = tooltipPainter.getHeight();

        basePoint.x += TOOLTIP_OFFSET;
        if (basePoint.x + cw + TOOLTIP_MARGIN > w)
            basePoint.x -= TOOLTIP_OFFSET + cw + TOOLTIP_MARGIN;
        if (basePoint.x < TOOLTIP_OFFSET)
            basePoint.x = TOOLTIP_OFFSET;

        basePoint.y -= ch + TOOLTIP_MARGIN;
        if (basePoint.y + ch + TOOLTIP_MARGIN > h)
            basePoint.y = h - ch - TOOLTIP_MARGIN;
        if (basePoint.y < TOOLTIP_MARGIN)
            basePoint.y = TOOLTIP_MARGIN;

        return basePoint;
    }


    public void paint(Graphics g) {
        if (tooltipPainter == null) return;

        Rectangle bounds = new Rectangle(0, 0, getWidth(), getHeight());
        Rectangle clip = g.getClipBounds();
        if (clip == null) g.setClip(bounds);
        else g.setClip(clip.intersection(bounds));

        super.paint(g);
    }

}
