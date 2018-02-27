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

import java.awt.Color;
import org.netbeans.lib.profiler.charts.ItemSelection;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;
import org.netbeans.lib.profiler.charts.ChartContext;
import org.netbeans.lib.profiler.charts.ChartItem;
import org.netbeans.lib.profiler.charts.ChartItemChange;
import org.netbeans.lib.profiler.charts.swing.LongRect;
import org.netbeans.lib.profiler.charts.xy.XYItem;
import org.netbeans.lib.profiler.charts.xy.XYItemChange;
import org.netbeans.lib.profiler.charts.xy.XYItemPainter;
import org.netbeans.lib.profiler.charts.xy.XYItemSelection;
import org.netbeans.lib.profiler.charts.xy.synchronous.SynchronousXYChartContext;
import org.netbeans.lib.profiler.charts.xy.synchronous.SynchronousXYItem;

/**
 *
 * @author Jiri Sedlacek
 */
abstract class TimelineXYPainter extends XYItemPainter.Abstract {

    private final int viewExtent;
    private final boolean bottomBased;

    private boolean painting;

    protected final double dataFactor;


    // --- Constructor ---------------------------------------------------------

    TimelineXYPainter(int viewExtent, boolean bottomBased, double dataFactor) {
        this.viewExtent = viewExtent;
        this.bottomBased = bottomBased;
        this.dataFactor = dataFactor;
        painting = true;
    }


    // --- Abstract interface --------------------------------------------------

    protected abstract void paint(XYItem item, List<ItemSelection> highlighted,
                                  List<ItemSelection> selected, Graphics2D g,
                                  Rectangle dirtyArea, SynchronousXYChartContext
                                  context);

    protected abstract Color getDefiningColor();


    // --- Public interface ----------------------------------------------------

    void setPainting(boolean painting) {
        this.painting = painting;
    }

    boolean isPainting() {
        return painting;
    }

    // --- ItemPainter implementation ------------------------------------------

    public LongRect getItemBounds(ChartItem item) {
        XYItem xyItem = (XYItem)item;
        return getDataBounds(xyItem.getBounds());
    }

    public LongRect getItemBounds(ChartItem item, ChartContext context) {
        XYItem xyItem = (XYItem)item;
        return getViewBounds(xyItem.getBounds(), context);
    }


    public boolean isBoundsChange(ChartItemChange itemChange) {
        XYItemChange change = (XYItemChange)itemChange;
        return !LongRect.equals(change.getOldValuesBounds(),
                                change.getNewValuesBounds());
    }

    public boolean isAppearanceChange(ChartItemChange itemChange) {
        XYItemChange change = (XYItemChange)itemChange;
        LongRect dirtyBounds = change.getDirtyValuesBounds();
        return dirtyBounds.width != 0 || dirtyBounds.height != 0;
    }

    public LongRect getDirtyBounds(ChartItemChange itemChange, ChartContext context) {
        XYItemChange change = (XYItemChange)itemChange;
        return getViewBounds(change.getDirtyValuesBounds(), context);
    }


    public boolean supportsHovering(ChartItem item) {
        return true;
    }

    public boolean supportsSelecting(ChartItem item) {
        return true;
    }

    public LongRect getSelectionBounds(ItemSelection selection, ChartContext context) {

        XYItemSelection sel = (XYItemSelection)selection;
        XYItem item  = sel.getItem();
        int selectedValueIndex = sel.getValueIndex();

        if (selectedValueIndex == -1 ||
            selectedValueIndex >= item.getValuesCount())
            // This happens on reset - bounds of the selection are unknown, let's clear whole area
            return new LongRect(0, 0, context.getViewportWidth(),
                                context.getViewportHeight());
        else
            return getViewBounds(item, selectedValueIndex, context);
    }

    public XYItemSelection getClosestSelection(ChartItem item, int viewX,
                                               int viewY, ChartContext context) {

        SynchronousXYChartContext contx = (SynchronousXYChartContext)context;

        int nearestTimestampIndex = contx.getNearestTimestampIndex(viewX, viewY);
        if (nearestTimestampIndex == -1) return null; // item not visible

        SynchronousXYItem xyItem = (SynchronousXYItem)item;
        return new XYItemSelection.Default(xyItem, nearestTimestampIndex,
                                           ItemSelection.DISTANCE_UNKNOWN);
    }

    public final void paintItem(ChartItem item, List<ItemSelection> highlighted,
                          List<ItemSelection> selected, Graphics2D g,
                          Rectangle dirtyArea, ChartContext context) {

        if (!painting) return;
        
        XYItem it = (XYItem)item;
        if (it.getValuesCount() < 1) return;
        if (context.getViewWidth() == 0 || context.getViewHeight() == 0) return;

        SynchronousXYChartContext ctx = (SynchronousXYChartContext)context;
        paint((XYItem)item, highlighted, selected, g, dirtyArea, ctx);
    }


    // --- XYItemPainter implementation ----------------------------------------

    public double getItemView(double dataY, XYItem item, ChartContext context) {
        return context.getViewY(dataY * dataFactor);
    }

    public double getItemValue(double viewY, XYItem item, ChartContext context) {
        return context.getDataY(viewY / dataFactor);
    }

    public double getItemValueScale(XYItem item, ChartContext context) {
        double scale = context.getViewHeight(dataFactor);
        if (scale <= 0) scale = -1;
        return scale;
    }


    // --- Private implementation ----------------------------------------------

    private LongRect getDataBounds(LongRect itemBounds) {
        LongRect bounds = new LongRect(itemBounds);
        bounds.y *= dataFactor;
        bounds.height *= dataFactor;

        if (bottomBased) {
            bounds.height += bounds.y;
            bounds.y = 0;
        }

        return bounds;
    }

    private LongRect getViewBounds(LongRect itemBounds, ChartContext context) {
        LongRect dataBounds = getDataBounds(itemBounds);

        LongRect viewBounds = context.getViewRect(dataBounds);
        LongRect.addBorder(viewBounds, viewExtent);

        return viewBounds;
    }

    private LongRect getViewBounds(XYItem item, int valueIndex, ChartContext context) {
        long xValue = item.getXValue(valueIndex);
        long yValue = (long)(item.getYValue(valueIndex) * dataFactor);
        return context.getViewRect(new LongRect(xValue, yValue, 0, 0));
    }

}
