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

package org.netbeans.lib.profiler.charts.axis;

import java.util.Iterator;
import org.netbeans.lib.profiler.charts.ChartContext;
import org.netbeans.lib.profiler.charts.Timeline;
import org.netbeans.lib.profiler.charts.swing.Utils;

/**
 *
 * @author Jiri Sedlacek
 */
public class TimelineMarksComputer extends AxisMarksComputer.Abstract {

    private final Timeline timeline;

    private double scale;
    private long step;

    private long firstTimestamp;
    private long lastTimestamp;


    public TimelineMarksComputer(Timeline timeline,
                                 ChartContext context,
                                 int orientation) {

        super(context, orientation);
        this.timeline = timeline;

        scale = -1;
        step = -1;
    }


    protected int getMinMarksDistance() {
            return 120;
        }

    protected boolean refreshConfiguration() {
        double oldScale = scale;
        long oldFirstTimestamp = firstTimestamp;
        long oldLastTimestamp = lastTimestamp;
        
        if ((horizontal && context.getViewWidth() == 0) ||
            (!horizontal && context.getViewHeight() == 0)) {
            scale = -1;
//        } else if (timeline.getTimestampsCount() == 0) {
//            // Initial scale
//            scale = -1;
        } else {
            scale = horizontal ? context.getViewWidth(1d) :
                                 context.getViewHeight(1d);
        }

        int timestampsCount = timeline.getTimestampsCount();
        if (horizontal) {
            firstTimestamp = timestampsCount == 0 ? (long)context.getDataX(0) :
                                                     timeline.getTimestamp(0);
            lastTimestamp = timestampsCount == 0 ? (long)context.getDataX(
                                                    context.getViewportWidth()):
                                                    Math.max(timeline.getTimestamp
                                                    (timestampsCount - 1),
                                                    (long)context.getDataX(
                                                    context.getViewportWidth()));
        } else {
            firstTimestamp = timestampsCount == 0 ? (long)context.getDataY(0) :
                                                     timeline.getTimestamp(0);
            lastTimestamp = timestampsCount == 0 ? (long)context.getDataY(
                                                    context.getViewportWidth()):
                                                    Math.max(timeline.getTimestamp
                                                    (timestampsCount - 1),
                                                    (long)context.getDataY(
                                                    context.getViewportWidth()));
        }
        
        if (oldScale != scale) {

            if (scale == -1) {
                step = -1;
            } else {
                step = TimeAxisUtils.getTimeUnits(scale, getMinMarksDistance());
            }

            oldScale = scale;
            return true;
        } else {
            return oldFirstTimestamp != firstTimestamp ||
                   oldLastTimestamp != lastTimestamp;
        }
    }


    public Iterator<AxisMark> marksIterator(int start, int end) {
        if (step == -1) return EMPTY_ITERATOR;

        final long dataStart = horizontal ?
                               ((long)context.getDataX(start) / step) * step :
                               ((long)context.getDataY(start) / step) * step;
        final long dataEnd = horizontal ?
                               ((long)context.getDataX(end) / step) * step :
                               ((long)context.getDataY(end) / step) * step;
        final long iterCount = Math.abs(dataEnd - dataStart) / step + 2;
        final long[] iterIndex = new long[] { 0 };

        final String format = TimeAxisUtils.getFormatString(step, firstTimestamp,
                                                            lastTimestamp);


        return new AxisMarksComputer.AbstractIterator() {

            public boolean hasNext() {
                return iterIndex[0] < iterCount;
            }

            public AxisMark next() {
                long value = reverse ? dataStart - iterIndex[0] * step :
                                       dataStart + iterIndex[0] * step;
                iterIndex[0]++;
                int position = horizontal ?
                               Utils.checkedInt(Math.ceil(context.getViewX(value))) :
                               Utils.checkedInt(Math.ceil(context.getViewY(value)));
                return new TimeMark(value, position, format);
            }

        };
    }

}
