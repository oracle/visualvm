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

import java.awt.Color;
import org.netbeans.lib.profiler.charts.ChartContext;
import org.netbeans.lib.profiler.charts.ChartItem;
import org.netbeans.lib.profiler.charts.ItemSelection;
import org.netbeans.lib.profiler.charts.swing.LongRect;
import org.netbeans.lib.profiler.charts.xy.XYItem;
import org.netbeans.lib.profiler.charts.xy.XYItemSelection;
import org.netbeans.lib.profiler.charts.xy.synchronous.SynchronousXYChartContext;
import org.netbeans.lib.profiler.charts.xy.synchronous.SynchronousXYItem;
import org.netbeans.lib.profiler.charts.xy.synchronous.SynchronousXYItemPainter;

/**
 *
 * @author Jiri Sedlacek
 */
public class ProfilerXYItemPainter extends SynchronousXYItemPainter {
    
    public static ProfilerXYItemPainter absolutePainter(float lineWidth,
                                                       Color lineColor,
                                                       Color fillColor) {
        
        return new ProfilerXYItemPainter(lineWidth, lineColor, fillColor,
                                         TYPE_ABSOLUTE, 0);
    }

    public static ProfilerXYItemPainter relativePainter(float lineWidth,
                                                       Color lineColor,
                                                       Color fillColor,
                                                       int maxOffset) {

        return new ProfilerXYItemPainter(lineWidth, lineColor, fillColor,
                                         TYPE_RELATIVE, maxOffset);
    }


    public ProfilerXYItemPainter(float lineWidth, Color lineColor, Color fillColor,
                          int type, int maxValueOffset) {
        super(lineWidth, lineColor, fillColor, type, maxValueOffset);
    }
    
    
    public boolean supportsHovering(ChartItem item) {
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
            return getViewBounds(item, new int[] { sel.getValueIndex() }, context);
    }
    
    public ItemSelection getClosestSelection(ChartItem item, int viewX,
                                             int viewY, ChartContext context) {
        SynchronousXYChartContext contx = (SynchronousXYChartContext)context;

        int nearestTimestampIndex = contx.getNearestTimestampIndex(viewX, viewY);
        if (nearestTimestampIndex == -1) return null; // item not visible

        SynchronousXYItem xyItem = (SynchronousXYItem)item;
        return new XYItemSelection.Default(xyItem, nearestTimestampIndex,
                                           ItemSelection.DISTANCE_UNKNOWN);
    }
    
    
    private LongRect getViewBounds(XYItem item, int[] valuesIndexes, ChartContext context) {

        LongRect dataBounds = new LongRect();

        if (valuesIndexes == null) {
            LongRect.set(dataBounds, item.getBounds());
        } else {
            boolean firstPoint = true;
            for (int valueIndex : valuesIndexes) {
                if (valueIndex == -1) continue;
                long xValue = item.getXValue(valueIndex);
                long yValue = item.getYValue(valueIndex);
                if (firstPoint) {
                    LongRect.set(dataBounds, xValue, yValue, 0, 0);
                    firstPoint = false;
                } else {
                    LongRect.add(dataBounds, xValue, yValue);
                }
            }
        }

        if (type == TYPE_RELATIVE) {
            return getViewBoundsRelative(dataBounds, item, context);
        } else {
            LongRect viewBounds = context.getViewRect(dataBounds);
            LongRect.addBorder(viewBounds, lineWidth);
            return viewBounds;
        }
    }
    
    private LongRect getViewBoundsRelative(LongRect dataBounds, XYItem item,
                                           ChartContext context) {
        LongRect itemBounds = item.getBounds();

        double itemValueFactor = getItemValueFactor(context, maxValueOffset,
                                                    itemBounds.height);

        // TODO: fix the math!!!
        double value1 = context.getDataOffsetY() + itemValueFactor *
                      (double)(dataBounds.y - itemBounds.y);
        double value2 = context.getDataOffsetY() + itemValueFactor *
                      (double)(dataBounds.y + dataBounds.height - itemBounds.y);

        long viewX = (long)Math.ceil(context.getViewX(dataBounds.x));
        long viewWidth = (long)Math.ceil(context.getViewWidth(dataBounds.width));
        if (context.isRightBased()) viewX -= viewWidth;

        long viewY1 = (long)Math.ceil(context.getViewY(value1));
        long viewY2 = (long)Math.ceil(context.getViewY(value2));
        long viewHeight = context.isBottomBased() ? viewY1 - viewY2 :
                                                    viewY2 - viewY1;
        if (!context.isBottomBased()) viewY2 -= viewHeight;

        LongRect viewBounds =  new LongRect(viewX, viewY2, viewWidth, viewHeight);
        LongRect.addBorder(viewBounds, lineWidth);

        return viewBounds;
    }
    
    private static double getItemValueFactor(ChartContext context,
                                             double maxValueOffset,
                                             double itemHeight) {
        return ((double)context.getDataHeight() -
               context.getDataHeight(maxValueOffset)) / itemHeight;
    }
    
}
