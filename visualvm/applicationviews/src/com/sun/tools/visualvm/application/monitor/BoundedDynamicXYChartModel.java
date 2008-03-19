/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
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

package com.sun.tools.visualvm.application.monitor;

import java.awt.Color;
import org.netbeans.lib.profiler.ui.charts.DynamicSynchronousXYChartModel;


/**
 *
 * @author Jiri Sedlacek
 */
public class BoundedDynamicXYChartModel extends DynamicSynchronousXYChartModel {    
    
    private int maxItemsCount;
    
    
    public BoundedDynamicXYChartModel(int maxItemsCount) {
        this.maxItemsCount = maxItemsCount;
    }
    
    public void setupModel(String[] seriesNames, Color[] seriesColors) {
        this.seriesNames = seriesNames;
        this.seriesColors = seriesColors;

        if (seriesNames.length != seriesColors.length) {
            seriesCount = 0;
            throw new RuntimeException("Counts of series names and series colors don't match."); // NOI18N
        } else {
            seriesCount = seriesNames.length;
        }

        itemCount = 0;

        xValues = new long[maxItemsCount];
        yValues = new long[maxItemsCount][];

        minXValue = 0;
        maxXValue = 0;

        minYValues = new long[seriesCount];
        maxYValues = new long[seriesCount];
    }

    public void addItemValues(long xValue, long[] yValues) {
        // first data arrived, initialize min/max values
        if (itemCount == 0) {
            for (int i = 0; i < seriesCount; i++) {
                minYValues[i] = yValues[i];
                maxYValues[i] = yValues[i];
            }
        } else {
            // check "timeline" consistency
            if (xValues[itemCount - 1] >= xValue) {
                throw new RuntimeException("New x-value not greater than previous x-value."); // NOI18N
            }            

            // check min/max for y values
            for (int i = 0; i < seriesCount; i++) {
                minYValues[i] = Math.min(minYValues[i], yValues[i]);
                maxYValues[i] = Math.max(maxYValues[i], yValues[i]);
            }
        }
        
        if (itemCount == maxItemsCount) {
            System.arraycopy(xValues, 1, xValues, 0, xValues.length - 1);
            System.arraycopy(this.yValues, 1, this.yValues, 0, this.yValues.length - 1);
            
            // add new x value
            xValues[itemCount - 1] = xValue;

            // add new y values
            this.yValues[itemCount - 1] = yValues;
        } else {
            // add new x value
            xValues[itemCount] = xValue;

            // add new y values
            this.yValues[itemCount] = yValues;
        }

        // increment item counter
        if (itemCount < maxItemsCount) itemCount++;
        
        minXValue = xValues[0];
        maxXValue = xValue; // new values are always greater

        fireChartDataChanged();
    }
}
