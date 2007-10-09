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

package org.netbeans.modules.profiler.ui.stats.drilldown.hierarchical;

import org.netbeans.lib.profiler.ProfilerClient;
import org.netbeans.lib.profiler.results.cpu.cct.CCTResultsFilter;
import org.netbeans.lib.profiler.results.cpu.cct.nodes.CategoryCPUCCTNode;
import org.netbeans.lib.profiler.results.cpu.marking.HierarchicalMark;
import org.netbeans.lib.profiler.results.cpu.marking.Mark;
import org.netbeans.lib.profiler.results.cpu.marking.MarkingEngine;
import org.netbeans.lib.profiler.ui.cpu.statistics.drilldown.DrillDownListener;
import org.netbeans.lib.profiler.ui.cpu.statistics.drilldown.IDrillDown;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 *
 * @author Jaroslav Bachorik
 */
public class DrillDown implements IDrillDown, CCTResultsFilter.Evaluator {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private static class TimeTouple {
        //~ Static fields/initializers -------------------------------------------------------------------------------------------

        public static final TimeTouple ZERO = new TimeTouple(0, 0);

        //~ Instance fields ------------------------------------------------------------------------------------------------------

        final long time0;
        final long time1;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public TimeTouple(final long time0, final long time1) {
            this.time0 = time0;
            this.time1 = time1;
        }
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final List ddPath = new ArrayList(5);
    private final Map netTimeMap = new HashMap();
    private final Map timeMap = new HashMap();
    private final ProfilerClient client;
    private final Set listeners = Collections.synchronizedSet(new HashSet());
    private final boolean secondTime;
    private final boolean validFlag;
    private HierarchicalMark currentMark;
    private HierarchicalMark root;
    private boolean isSelf = false;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public DrillDown(final ProfilerClient client, final HierarchicalMark markRoot, final boolean secondTimeStamp) {
        this.root = markRoot;
        this.secondTime = secondTimeStamp;
        this.client = client;
        reset();

        this.validFlag = !markRoot.getChildren().isEmpty();
    }

    public DrillDown(final ProfilerClient client, final HierarchicalMark markRoot) {
        this(client, markRoot, false);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public boolean isCurrent(final Mark mark) {
        return getCurrentMark().equals(mark);
    }

    public Mark getCurrentMark() {
        return (currentMark != null) ? currentMark : Mark.DEFAULT;
    }

    public long getCurrentTime(final boolean net) {
        return getMarkTime(currentMark, net);
    }

    public List getDrillDownPath() {
        List rslt = new ArrayList(ddPath);

        if (isSelf) {
            rslt.add(new Mark("SELF", "Self")); // NOI18N
        }

        return rslt;
    }

    public boolean isInSelf() {
        return isSelf;
    }

    public long getMarkTime(final Mark mark, final boolean net) {
        TimeTouple time = (TimeTouple) (net ? netTimeMap : timeMap).get(mark);

        return (time != null) ? (secondTime ? time.time1 : time.time0) : 0;
    }

    public List getSubmarks() {
        if (isSelf) {
            return Arrays.asList(new HierarchicalMark[] { currentMark });
        } else {
            List rslt = new ArrayList(currentMark.getChildren());
            rslt.add(currentMark);

            return rslt;
        }
    }

    public Mark getTopMark() {
        return root;
    }

    public long getTopTime(final boolean net) {
        return getMarkTime(root, net);
    }

    public boolean isValid() {
        return validFlag;
    }

    public void addListener(DrillDownListener drillDownListener) {
        listeners.add(drillDownListener);
    }

    public boolean canDrilldown(Mark mark) {
        if (mark == null) {
            return false;
        }

        if (isSelf) {
            return false;
        }

        if (isCurrent(mark)) { // special "SELF" category

            if (mark instanceof HierarchicalMark) {
                return ((HierarchicalMark) mark).getChildren().size() > 1;
            }
        }

        return true;
    }

    public void drilldown(Mark mark) {
        if (!canDrilldown(mark)) {
            return;
        }

        if (mark.equals(currentMark)) {
            isSelf = true;
            fireDrillDownChange();
        } else {
            isSelf = false;

            for (Iterator iter = currentMark.getChildren().iterator(); iter.hasNext();) {
                HierarchicalMark child = (HierarchicalMark) iter.next();

                if (child.equals(mark)) {
                    if (canDrilldown(mark)) {
                        currentMark = child;
                        ddPath.add(currentMark);
                        fireDrillDownChange();
                    }

                    break;
                }
            }
        }
    }

    public void drillup() {
        if (ddPath.size() == 1) {
            return;
        }

        ddPath.remove(ddPath.size() - 1);
        currentMark = (HierarchicalMark) ddPath.get(ddPath.size() - 1);
        fireDrillDownChange();
    }

    public void drillup(Mark mark) {
        if (!ddPath.contains(mark)) {
            return;
        }

        isSelf = false;

        for (int i = ddPath.size() - 1; i >= 0; i--) {
            if (ddPath.get(i).equals(mark)) {
                currentMark = (HierarchicalMark) ddPath.get(i);
                fireDrillDownChange();

                break;
            }

            ddPath.remove(ddPath.size() - 1);
        }
    }

    public boolean evaluate(Mark categoryMark) {
        if ((currentMark == null) || (currentMark.isRoot() && !isSelf)) {
            return true;
        }

        if (currentMark.isDefault && categoryMark.isDefault) {
            return true;
        }

        return isSelf ? categoryMark.equals(currentMark) : categoryMark.getLabels().contains(currentMark.getLabel());
    }

    public void refresh() {
        if (client.getTimeCollector() == null) {
            return;
        }

        clearTimeMaps();

        client.getTimeCollector().beginTrans(false);

        try {
            getTime(root);
        } finally {
            client.getTimeCollector().endTrans();
        }

        fireDataChange();
    }

    public void removeListener(DrillDownListener drillDownListener) {
        listeners.remove(drillDownListener);
    }

    public void reset() {
        ddPath.clear();
        ddPath.add(root);
        currentMark = root;
        isSelf = false;
        fireDrillDownChange();
    }

    private TimeTouple getTime(HierarchicalMark mark) {
        int markId = MarkingEngine.getDefault().getMarkId(mark);

        if (markId == -1) {
            netTimeMap.put(mark, TimeTouple.ZERO);
            timeMap.put(mark, TimeTouple.ZERO);

            return TimeTouple.ZERO;
        }

        long tmpTime0 = client.getTimeCollector().getNetTime0(mark);
        long tmpTime1 = client.getTimeCollector().getNetTime1(mark);
        long netTime0 = tmpTime0;
        long netTime1 = tmpTime1;

        for (Iterator iter = mark.getChildren().iterator(); iter.hasNext();) {
            TimeTouple subtime = getTime((HierarchicalMark) iter.next());
            tmpTime0 += subtime.time0;
            tmpTime1 += subtime.time1;
        }

        TimeTouple netTime = new TimeTouple(netTime0, netTime1);
        TimeTouple accTime = new TimeTouple(tmpTime0, tmpTime1);
        netTimeMap.put(mark, netTime);
        timeMap.put(mark, accTime);

        return accTime;
    }

    private void clearTimeMaps() {
        netTimeMap.clear();
        timeMap.clear();
    }

    private void fireDataChange() {
        Set tmpListeners = new HashSet(listeners);

        for (Iterator iter = tmpListeners.iterator(); iter.hasNext();) {
            ((DrillDownListener) iter.next()).dataChanged();
        }
    }

    private void fireDrillDownChange() {
        Set tmpListeners = new HashSet(listeners);

        for (Iterator iter = tmpListeners.iterator(); iter.hasNext();) {
            ((DrillDownListener) iter.next()).drillDownPathChanged(getDrillDownPath());
        }
    }
}
