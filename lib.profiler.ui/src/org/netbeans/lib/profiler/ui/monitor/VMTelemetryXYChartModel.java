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

package org.netbeans.lib.profiler.ui.monitor;

import org.netbeans.lib.profiler.results.DataManagerListener;
import org.netbeans.lib.profiler.results.monitor.VMTelemetryDataManager;
import org.netbeans.lib.profiler.ui.charts.*;
import java.awt.Color;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


/**
 *
 * @author Jiri Sedlacek
 */
public abstract class VMTelemetryXYChartModel extends AbstractSynchronousXYChartModel implements DataManagerListener {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected VMTelemetryDataManager vmTelemetryDataManager;
    protected long[] maxYValues;
    protected long[] minYValues;
    protected Color[] seriesColors;
    protected String[] seriesNames;
    protected int itemCount = 0;
    protected int seriesCount = 0;
    protected long maxXValue = 0;
    protected long minXValue = 0;
    private final Set dataResetListeners = new HashSet();
    private long lastXValue = Long.MIN_VALUE;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of VMTelemetryXYChartModel */
    public VMTelemetryXYChartModel(VMTelemetryDataManager vmTelemetryDataManager) {
        this.vmTelemetryDataManager = vmTelemetryDataManager;
        vmTelemetryDataManager.addDataListener(this);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public int getItemCount() {
        return itemCount;
    }

    // TODO: will be moved to chart axis definition
    public long getMaxDisplayYValue(int seriesIndex) {
        return getMaxYValue(seriesIndex);
    }

    public long getMaxXValue() {
        return maxXValue;
    }

    public long getMaxYValue(int seriesIndex) {
        return maxYValues[seriesIndex];
    }

    // TODO: will be moved to chart axis definition
    public long getMinDisplayYValue(int seriesIndex) {
        return 0;
    }

    public long getMinXValue() {
        return minXValue;
    }

    public long getMinYValue(int seriesIndex) {
        return minYValues[seriesIndex];
    }

    public Color getSeriesColor(int seriesIndex) {
        return seriesColors[seriesIndex];
    }

    // --- Abstract SynchronousXYChartModel --------------------------------------
    public int getSeriesCount() {
        return seriesCount;
    }

    public String getSeriesName(int seriesIndex) {
        return seriesNames[seriesIndex];
    }

    public long getXValue(int itemIndex) {
        return vmTelemetryDataManager.timeStamps[itemIndex];
    }

    public long getYValue(int itemIndex, int seriesIndex) {
        return getYValues(seriesIndex)[itemIndex];
    }

    // --- DataReset Listeners ---------------------------------------------------
    public void addDataResetListener(VMTelemetryXYChartModelDataResetListener listener) {
        dataResetListeners.add(listener);
    }

    // --- DataManagerListener ---------------------------------------------------

    /** Called when the data managed by the manager change */
    public void dataChanged() {
        int newItemCount = vmTelemetryDataManager.getItemCount();

        if (itemCount == 0) {
            if (newItemCount > 0) {
                minXValue = vmTelemetryDataManager.timeStamps[0];
                maxXValue = vmTelemetryDataManager.timeStamps[0];

                for (int seriesIndex = 0; seriesIndex < seriesCount; seriesIndex++) {
                    minYValues[seriesIndex] = getYValues(seriesIndex)[0];
                    maxYValues[seriesIndex] = getYValues(seriesIndex)[0];
                }

                if (newItemCount > 1) {
                    processNewData(1, newItemCount - 1);
                }
            }
        } else {
            if (newItemCount > itemCount) {
                processNewData(itemCount, newItemCount - 1);
            }
        }

        itemCount = newItemCount;

        fireChartDataChanged();
    }

    /** Called when the data managed by the manager reset (e.g. when a new session is started) */
    public void dataReset() {
        itemCount = 0;

        minXValue = 0;
        maxXValue = 0;

        minYValues = new long[seriesCount];
        maxYValues = new long[seriesCount];

        lastXValue = Long.MIN_VALUE;

        fireChartDataReset();
    }

    /**
     * Removes ChartModel listener.
     * @param listener ChartModel listener to remove
     */
    public void removeDataResetListener(VMTelemetryXYChartModelDataResetListener listener) {
        dataResetListeners.remove(listener);
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

        minXValue = 0;
        maxXValue = 0;

        minYValues = new long[seriesCount];
        maxYValues = new long[seriesCount];
    }

    // --- Protected methods to be overriden by concrete implementation ----------
    protected abstract long[] getYValues(int seriesIndex);

    /**
     * Notifies all listeners about the data change.
     */
    protected void fireChartDataReset() {
        if (dataResetListeners == null) {
            return;
        }

        Set toNotify;

        synchronized (dataResetListeners) {
            toNotify = new HashSet(dataResetListeners);
        }

        Iterator iterator = toNotify.iterator();

        while (iterator.hasNext()) {
            ((VMTelemetryXYChartModelDataResetListener) iterator.next()).chartDataReset();
        }
    }

    private void processNewData(int startIndex, int endIndex) {
        long newXValue;
        long newYValue;

        for (int itemIndex = startIndex; itemIndex <= endIndex; itemIndex++) {
            newXValue = vmTelemetryDataManager.timeStamps[itemIndex];

            if (lastXValue >= newXValue) {
                //Profiler.getDefault().log(ErrorManager.WARNING, "New x-value not greater than previous x-value. Graphs may not be displayed correctly."); // ErrorManager not accessible
                System.err.println("Profiler Graphs Warning: New x-value not greater than previous x-value. Graphs may not be displayed correctly."); // NOI18N
            } else {
                maxXValue = newXValue;
            }

            lastXValue = newXValue;

            for (int seriesIndex = 0; seriesIndex < seriesCount; seriesIndex++) {
                newYValue = getYValues(seriesIndex)[itemIndex];

                minYValues[seriesIndex] = Math.min(minYValues[seriesIndex], newYValue);
                maxYValues[seriesIndex] = Math.max(maxYValues[seriesIndex], newYValue);
            }
        }
    }
}
