/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.charts.xy;

import java.util.Iterator;
import org.graalvm.visualvm.lib.charts.ChartContext;
import org.graalvm.visualvm.lib.charts.axis.AxisMark;
import org.graalvm.visualvm.lib.charts.axis.AxisMarksComputer;
import org.graalvm.visualvm.lib.charts.axis.DecimalAxisUtils;
import org.graalvm.visualvm.lib.charts.axis.LongMark;
import org.graalvm.visualvm.lib.charts.swing.Utils;

/**
 *
 * @author Jiri Sedlacek
 */
public class DecimalXYItemMarksComputer extends XYItemMarksComputer {

    private double scale;
    private long step;


    public DecimalXYItemMarksComputer(XYItem item,
                                      XYItemPainter painter,
                                      ChartContext context,
                                      int orientation) {

        super(item, painter, context, orientation);

        scale = -1;
        step = -1;

    }


    protected boolean refreshConfiguration() {
        double oldScale = scale;

        if (context.getViewWidth() == 0) {
            scale = -1;
//        } else if (item.getValuesCount() == 0) {
//            // Initial scale
//            scale = -1;
        } else {
            scale = painter.getItemValueScale(item, context);
        }
        
        if (oldScale != scale) {

            if (scale == -1) {
                step = -1;
            } else {
                step = DecimalAxisUtils.getDecimalUnits(scale, getMinMarksDistance());
            }

            oldScale = scale;
            return true;
        } else {
            return false;
        }
    }


    public Iterator<AxisMark> marksIterator(int start, int end) {
            if (step == -1) return EMPTY_ITERATOR;

            final long dataStart = ((long)painter.getItemValue(start, item,
                                          context) / step) * step;
            final long dataEnd = ((long)painter.getItemValue(end, item,
                                          context) / step) * step;
            final long iterCount = Math.abs(dataEnd - dataStart) / step + 2;
            final long[] iterIndex = new long[] { 0 };


            return new AxisMarksComputer.AbstractIterator() {

                public boolean hasNext() {
                    return iterIndex[0] < iterCount;
                }

                public AxisMark next() {
                    long value = reverse ? dataStart - iterIndex[0] * step :
                                           dataStart + iterIndex[0] * step;

                    iterIndex[0]++;
                    int position = Utils.checkedInt(
                                         painter.getItemView(value, item, context));
                    return new LongMark(value, position);
                }

            };

        }

}
