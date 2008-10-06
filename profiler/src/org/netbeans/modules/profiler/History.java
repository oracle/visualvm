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

package org.netbeans.modules.profiler;

import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.common.event.ProfilingStateEvent;
import org.netbeans.lib.profiler.common.event.ProfilingStateListener;
import org.netbeans.lib.profiler.ui.charts.ChartModelListener;
import org.netbeans.lib.profiler.ui.charts.SynchronousXYChartModel;
import org.netbeans.lib.profiler.ui.memory.*;
import org.netbeans.lib.profiler.utils.StringUtils;
import org.netbeans.modules.profiler.ui.ProfilerDialogs;
import org.openide.util.NbBundle;
import java.awt.Color;
import java.text.MessageFormat;
import java.util.ArrayList;


/**
 *
 * @author Emanuel Hucka
 */
public class History implements SynchronousXYChartModel, ActionsHandler, ProfilingStateListener {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final int HISTORY_LENGTH = 1024;
    private static final String LIVE_OBJECS = NbBundle.getMessage(History.class, "History_LiveObjects"); //NOI18N
    private static final String ALLOCATED_OBJECS = NbBundle.getMessage(History.class, "History_AllocatedObjects"); //NOI18N
    private static final String ALLOCATED_SIZE = NbBundle.getMessage(History.class, "History_AllocatedSize"); //NOI18N
    private static final String LOGGING_CONFIRMATION_CAPTION = NbBundle.getMessage(History.class,
                                                                                   "History_LoggingConfirmationCaption"); //NOI18N
    private static final String LOGGING_RESET_MSG = NbBundle.getMessage(History.class, "History_LoggingResetMsg"); //NOI18N
    private static final String LOGGING_STOP_MSG = NbBundle.getMessage(History.class, "History_LoggingStopMsg"); //NOI18N
    private static History instance = null;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected Color[] seriesColors = new Color[] { new Color(255, 127, 127), new Color(127, 63, 191) };
    protected String[] seriesNames1 = new String[] { ALLOCATED_OBJECS, ALLOCATED_SIZE };
    protected String[] seriesNames2 = new String[] { LIVE_OBJECS, ALLOCATED_OBJECS };
    protected int itemCount = 0;
    protected int seriesCount = 2;
    protected long maxXValue = 0;
    protected long maxYDisplayValue = 0;
    protected long minXValue = 0;
    protected long[] maxYValues = new long[seriesCount];
    protected long[] minYValues = new long[seriesCount];
    ArrayList historyListeners = new ArrayList(4);
    ArrayList listeners = new ArrayList(4);
    int[] history1;
    long[] history2;
    long[] time;
    boolean firstLine = true;
    int current = 0;
    private String className;
    private boolean enabled = false;
    private boolean liveness = false;
    private int historyClassID = -1;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of History */
    public History() {
        Profiler.getDefault().addProfilingStateListener(this);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static synchronized History getInstance() {
        if (instance == null) {
            instance = new History();
        }

        return instance;
    }

    public String getClassName() {
        return className;
    }

    public int getCurrent() {
        return current;
    }

    public int[] getData() {
        return history1;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setHistoryClass(int historyClassID, String className) {
        if (!enabled) {
            enabled = true;
            history1 = new int[HISTORY_LENGTH];
            history2 = new long[HISTORY_LENGTH];
            time = new long[HISTORY_LENGTH];
        }

        this.historyClassID = historyClassID;
        this.className = StringUtils.userFormClassName(className);
        current = 0;
        firstLine = true;

        for (int i = 0; i < history1.length; i++) {
            history1[i] = 0;
            time[i] = 0;
        }

        fireHistoryLogging();
    }

    public int getHistoryClassID() {
        return historyClassID;
    }

    public int getItemCount() {
        return itemCount;
    }

    public Color getLimitYColor() {
        return Color.WHITE;
    }

    public long getLimitYValue() {
        return Long.MAX_VALUE;
    }

    public void setLiveness(boolean liveness) {
        this.liveness = liveness;
    }

    public boolean isLiveness() {
        return liveness;
    }

    public long getMaxDisplayYValue(int seriesIndex) {
        return (seriesIndex == 0) ? maxYValues[seriesIndex] : maxYDisplayValue;
    }

    public long getMaxXValue() {
        return maxXValue;
    }

    public long getMaxYValue(int seriesIndex) {
        return maxYValues[seriesIndex];
    }

    public long getMinDisplayYValue(int seriesIndex) {
        return 0;
    }

    public long getMinXValue() {
        return minXValue;
    }

    public long getMinYValue(int seriesIndex) {
        return 0;
    }

    public Color getSeriesColor(int seriesIndex) {
        return seriesColors[seriesIndex];
    }

    public int getSeriesCount() {
        return seriesCount;
    }

    public String getSeriesName(int seriesIndex) {
        if (!isLiveness()) {
            return seriesNames1[seriesIndex];
        } else {
            return seriesNames2[seriesIndex];
        }
    }

    public long getXValue(int itemIndex) {
        if (itemIndex < 0) {
            return 0;
        }

        int index = (!firstLine) ? (itemIndex + current) : itemIndex;

        if (index >= time.length) {
            index = index - time.length;
        }

        return time[index];
    }

    public long getYValue(int itemIndex, int seriesIndex) {
        if (itemIndex < 0) {
            return 0;
        }

        int index = (!firstLine) ? (itemIndex + current) : itemIndex;

        if (index >= history1.length) {
            index = index - history1.length;
        }

        if (seriesIndex == 0) {
            return history1[index];
        } else {
            return history2[index];
        }
    }

    public void addChartModelListener(ChartModelListener listener) {
        listeners.add(listener);
    }

    public void addHistoryListener(HistoryListener listener) {
        historyListeners.add(listener);
    }

    public void instrumentationChanged(int i, int i0) {
    }

    public void performAction(String actionName, Object[] arguments) {
        if ("history logging".equals(actionName)) { //NOI18N

            int newHistoryClassID = ((Integer) (arguments[0])).intValue();

            if (historyClassID != -1) {
                if (newHistoryClassID == historyClassID) {
                    ProfilerDialogs.DNSAConfirmationChecked dnsa = new ProfilerDialogs.DNSAConfirmationChecked("History.historylogging.reset", //NOI18N
                                                                                                               MessageFormat
                                                                                                                                                                                                                                                                            .format(LOGGING_RESET_MSG,
                                                                                                                                                                                                                                                                                    new Object[] {
                                                                                                                                                                                                                                                                                        className
                                                                                                                                                                                                                                                                                    }),
                                                                                                               LOGGING_CONFIRMATION_CAPTION,
                                                                                                               ProfilerDialogs.DNSAConfirmationChecked.YES_NO_OPTION);

                    if (!ProfilerDialogs.notify(dnsa).equals(ProfilerDialogs.DNSAConfirmationChecked.YES_OPTION)) {
                        return;
                    }
                } else {
                    ProfilerDialogs.DNSAConfirmationChecked dnsa = new ProfilerDialogs.DNSAConfirmationChecked("History.historylogging.stop", //NOI18N
                                                                                                               MessageFormat
                                                                                                                                                                                                                                                                                         .format(LOGGING_STOP_MSG,
                                                                                                                                                                                                                                                                                                 new Object[] {
                                                                                                                                                                                                                                                                                                     className
                                                                                                                                                                                                                                                                                                 }),
                                                                                                               LOGGING_CONFIRMATION_CAPTION,
                                                                                                               ProfilerDialogs.DNSAConfirmationChecked.YES_NO_OPTION);

                    if (!ProfilerDialogs.notify(dnsa).equals(ProfilerDialogs.DNSAConfirmationChecked.YES_OPTION)) {
                        return;
                    }
                }
            }

            setLiveness(((Boolean) arguments[2]).booleanValue());
            setHistoryClass(newHistoryClassID, (String) arguments[1]);
        } else if ("history update".equals(actionName)) { //NOI18N
            update(arguments[0], arguments[1]);
        }
    }

    public void profilingStateChanged(ProfilingStateEvent profilingStateEvent) {
        if (profilingStateEvent.getNewState() == Profiler.PROFILING_INACTIVE) {
            enabled = false;
        } else if (profilingStateEvent.getNewState() == Profiler.PROFILING_STARTED) {
            historyClassID = -1;
        }
    }

    public void removeChartModelListener(ChartModelListener listener) {
        listeners.remove(listener);
    }

    public void removeHistoryListener(HistoryListener listener) {
        historyListeners.remove(listener);
    }

    public void threadsMonitoringChanged() {
    }

    public void update(Object hist1, Object hist2) {
        if ((historyClassID > -1) && enabled) {
            if (hist1 instanceof int[]) {
                history1[current] = ((int[]) hist1)[historyClassID];
            }

            if (hist2 != null) {
                if (hist2 instanceof int[]) {
                    history2[current] = ((int[]) hist2)[historyClassID];
                } else if (hist2 instanceof long[]) {
                    history2[current] = ((long[]) hist2)[historyClassID];
                }
            }

            time[current] = System.currentTimeMillis();
            current++;

            if (current >= history1.length) {
                current = 0;
                firstLine = false;
            }

            itemCount = ((firstLine) ? current : history1.length);
            minXValue = (firstLine) ? time[0] : ((current < (time.length - 1)) ? time[current] : time[0]);
            maxXValue = (current > 0) ? time[current - 1] : time[time.length - 1];
            setMinMax();
            fireChartChanged();
        }
    }

    protected long getDisplay(long min, long max) {
        long n = max - min;
        long num = 1;

        while ((10 * n) > num) {
            if (num > n) {
                return num;
            }

            if ((2 * num) > n) {
                return 2 * num;
            }

            if ((5 * num) > n) {
                return 5 * num;
            }

            num *= 10;
        }

        return n;
    }

    protected void setMinMax() {
        int a = (firstLine) ? current : history1.length;
        int m = 0;
        long m2 = 0;

        for (int i = 0; i < a; i++) {
            if (history1[i] > m) {
                m = history1[i];
            }

            if (history2[i] > m2) {
                m2 = history2[i];
            }
        }

        maxYValues[0] = m;
        maxYValues[1] = m2;
        maxYDisplayValue = getDisplay(getMinYValue(1), m2);
    }

    protected void fireChartChanged() {
        for (int i = 0; i < listeners.size(); i++) {
            ((ChartModelListener) (listeners.get(i))).chartDataChanged();
        }
    }

    protected void fireHistoryLogging() {
        for (int i = 0; i < historyListeners.size(); i++) {
            ((HistoryListener) (historyListeners.get(i))).historyLogging();
        }
    }
}
