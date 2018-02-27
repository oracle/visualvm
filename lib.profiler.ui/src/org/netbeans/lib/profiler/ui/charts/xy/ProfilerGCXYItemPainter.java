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

package org.netbeans.lib.profiler.ui.charts.xy;

import org.netbeans.lib.profiler.charts.ChartContext;
import org.netbeans.lib.profiler.charts.ChartItem;
import org.netbeans.lib.profiler.charts.ChartItemChange;
import org.netbeans.lib.profiler.charts.ItemSelection;
import org.netbeans.lib.profiler.charts.swing.LongRect;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;
import org.netbeans.lib.profiler.charts.swing.Utils;
import org.netbeans.lib.profiler.charts.xy.XYItem;
import org.netbeans.lib.profiler.charts.xy.XYItemChange;
import org.netbeans.lib.profiler.charts.xy.synchronous.SynchronousXYChartContext;
import org.netbeans.lib.profiler.charts.xy.synchronous.SynchronousXYItemPainter;

/**
 *
 * @author Jiri Sedlacek
 */
public class ProfilerGCXYItemPainter extends SynchronousXYItemPainter {

    // --- Constructor ---------------------------------------------------------

    public static ProfilerGCXYItemPainter painter(Color fillColor) {
        
        return new ProfilerGCXYItemPainter(fillColor);
    }


    ProfilerGCXYItemPainter(Color fillColor) {
        super(0, null, fillColor, TYPE_ABSOLUTE, 0);
    }


    // --- ItemPainter implementation ------------------------------------------

    public LongRect getItemBounds(ChartItem item, ChartContext context) {
//        if (!(item instanceof ProfilerGCXYItem))
//            throw new UnsupportedOperationException("Unsupported item: " + item); // NOI18N

        // TODO: should return real bounds (== empty bounds for no gc)

        LongRect viewBounds = super.getItemBounds(item, context);
        viewBounds.y = 0;
        viewBounds.height = context.getViewportHeight();
        return viewBounds;
    }

    public LongRect getDirtyBounds(ChartItemChange itemChange, ChartContext context) {
//        if (!(itemChange instanceof XYItemChange))
//            throw new UnsupportedOperationException("Unsupported itemChange: " + itemChange);
        
        XYItemChange change = (XYItemChange)itemChange;
        ProfilerGCXYItem item = (ProfilerGCXYItem)change.getItem();
        
        int[] indexes = change.getValuesIndexes();

        if (indexes.length == 1 && indexes[0] == -1) {
            // Data reset
            LongRect dirtyBounds = change.getDirtyValuesBounds();
            
            long x = (long)context.getViewX(dirtyBounds.x);
            long y = (long)context.getViewY(context.getDataOffsetY() +
                                            context.getDataHeight());
            long width = (long)context.getViewWidth(dirtyBounds.width);
            long height = context.getViewportHeight();

            return new LongRect(x, y, width, height);
        } else {
            // New data
            int index = indexes[0];
            int lastIndex = indexes[indexes.length - 1];

            long dataStart = -1;
            long dataEnd   = -1;

            while (index <= lastIndex) {
                long[] gcEnds = item.getGCEnds(index);
                if (gcEnds.length > 0) {
                    dataEnd = gcEnds[gcEnds.length - 1];
                    if (dataStart == -1) {
                        long[] gcStarts = item.getGCStarts(index);
                        dataStart = gcStarts[0];
                    }
                }
                index++;
            }

            if (dataStart == -1) return new LongRect();
            if (dataEnd == -1) dataEnd = item.getXValue(item.getValuesCount() - 1);

            long x = (long)context.getViewX(dataStart);
            long y = (long)context.getViewY(context.getDataOffsetY() +
                                            context.getDataHeight());
            long width = (long)context.getViewWidth(dataEnd - dataStart);
            width = Math.max(width, 1);
            long height = context.getViewportHeight();

            return new LongRect(x, y, width, height);

        }
    }


    public double getItemView(double dataY, XYItem item, ChartContext context) {
        return 0;
    }

    public double getItemValue(double viewY, XYItem item, ChartContext context) {
        return 0;
    }

    public double getItemValueScale(XYItem item, ChartContext context) {
        return -1;
    }


    // --- Private implementation ----------------------------------------------

    
    protected void paint(XYItem item, List<ItemSelection> highlighted,
                       List<ItemSelection> selected, Graphics2D g,
                       Rectangle dirtyArea, SynchronousXYChartContext context) {
//        if (!(item instanceof ProfilerGCXYItem))
//            throw new UnsupportedOperationException("Unsupported item: " + item); // NOI18N

        int valuesCount = item.getValuesCount();
        if (valuesCount < 2) return;
        if (context.getViewWidth() == 0 || context.getViewHeight() == 0) return;

        int[][] visibleBounds = context.getVisibleBounds(dirtyArea);

        int firstFirst = visibleBounds[0][0];
        int index = firstFirst;
        if (index == -1) index = visibleBounds[0][1];
        if (index == -1) return;

        int lastFirst = visibleBounds[1][0];
        int lastIndex = lastFirst;
        if (lastIndex == -1) lastIndex = visibleBounds[1][1];
        if (lastIndex == -1) lastIndex = valuesCount - 1;
        if (lastFirst != -1 && lastIndex < valuesCount - 1) lastIndex += 1;

        int itemsStep = (int)Math.ceil((double)valuesCount / (double)context.getViewWidth());
        if (itemsStep == 0) itemsStep = 1;

        int visibleCount = lastIndex - index + 1;

        if (itemsStep > 1) {
            int firstMod = index % itemsStep;
            index -= firstMod;
            int lastMod = lastIndex % itemsStep;
            lastIndex = lastIndex - lastMod + itemsStep;
            visibleCount = (lastIndex - index) / itemsStep + 1;
            lastIndex = Math.min(lastIndex, valuesCount - 1);
        }

        ProfilerGCXYItem xyItem = (ProfilerGCXYItem)item;

        g.setColor(fillColor);

        int startY = (int)context.getViewY(context.getDataOffsetY() +
                                           context.getDataHeight());
        int height = context.getViewportHeight();

        for (int iter = 0; iter < visibleCount; iter++) {
            long[] gcStarts = xyItem.getGCStarts(index);
            if (gcStarts.length > 0) {
                long[] gcEnds = xyItem.getGCEnds(index);
                for (int i = 0; i < gcStarts.length; i++) {
                    int itemStart = Utils.checkedInt(
                                          context.getViewX(gcStarts[i]));
                    long gcEnd = gcEnds[i];
                    if (gcEnd == -1)
                        gcEnd =  item.getXValue(item.getValuesCount() - 1);
                    int itemLength = Utils.checkedInt(
                                           context.getViewWidth(
                                           gcEnd - gcStarts[i]));

                    g.fillRect(itemStart, startY, Math.max(itemLength, 1), height);
                }
            }
            
            index = Math.min(index + itemsStep, lastIndex);
        }
        
    }

}
