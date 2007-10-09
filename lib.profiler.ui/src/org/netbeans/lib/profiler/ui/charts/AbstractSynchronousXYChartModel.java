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

package org.netbeans.lib.profiler.ui.charts;

import java.awt.Color;
import java.util.Iterator;
import java.util.Vector;


/**
 *
 * @author Jiri Sedlacek
 */
public abstract class AbstractSynchronousXYChartModel implements SynchronousXYChartModel {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private Vector listeners; // Data change listeners

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public abstract int getItemCount(); // number of collected items

    public abstract long getMaxXValue(); // maximum (last) value on the horizontal axis (valid for all series)

    public abstract long getMaxYValue(int seriesIndex); // maximum values on the vertical axis for one series

    public abstract long getMinXValue(); // minimum (first) value on the horizontal axis (valid for all series)

    public abstract long getMinYValue(int seriesIndex); // minimum values on the vertical axis for one series

    public abstract Color getSeriesColor(int seriesIndex); // color of data series

    // --- Abstract XYChartModel -------------------------------------------------
    public abstract int getSeriesCount(); // number of tracked data series

    public abstract String getSeriesName(int seriesIndex); // name of data series

    public abstract long getXValue(int itemIndex); // x value of the item (valid for all series)

    public abstract long getYValue(int itemIndex, int seriesIndex); // y values of the item for one series

    public Color getLimitYColor() {
        return Color.WHITE;
    }

    public long getLimitYValue() {
        return Long.MAX_VALUE;
    }

    // TODO: will be moved to chart axis definition
    public long getMaxDisplayYValue(int seriesIndex) {
        return getMaxYValue(seriesIndex);
    } // value to be displayed at the top of the chart

    // TODO: will be moved to chart axis definition
    public long getMinDisplayYValue(int seriesIndex) {
        return getMinYValue(seriesIndex);
    } // value to be displayed at the bottom of the chart

    // --- Listeners -------------------------------------------------------------

    /**
     * Adds new ChartModel listener.
     * @param listener ChartModel listener to add
     */
    public synchronized void addChartModelListener(ChartModelListener listener) {
        if (listeners == null) {
            listeners = new Vector();
        }

        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Removes ChartModel listener.
     * @param listener ChartModel listener to remove
     */
    public synchronized void removeChartModelListener(ChartModelListener listener) {
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    /**
     * Notifies all listeners about the data change.
     */
    protected void fireChartDataChanged() {
        if (listeners == null) {
            return;
        }

        Vector toNotify;

        synchronized (this) {
            toNotify = ((Vector) listeners.clone());
        }

        Iterator iterator = toNotify.iterator();

        while (iterator.hasNext()) {
            ((ChartModelListener) iterator.next()).chartDataChanged();
        }
    }
}
