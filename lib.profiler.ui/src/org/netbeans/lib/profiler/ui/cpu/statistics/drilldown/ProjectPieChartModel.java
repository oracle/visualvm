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

package org.netbeans.lib.profiler.ui.cpu.statistics.drilldown;

import org.netbeans.lib.profiler.results.cpu.marking.Mark;
import java.text.MessageFormat;
import java.util.ResourceBundle;


/**
 *
 * @author Jaroslav Bachorik
 */
public class ProjectPieChartModel extends DrillDownPieChartModel {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.netbeans.lib.profiler.ui.cpu.statistics.drilldown.Bundle"); // NOI18N
    private static final String SELF_BADGE_TEXT = messages.getString("ProjectPieChartModel_SelfBadgeText"); // NOI18N
                                                                                                            // -----

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Creates a new instance of ProjectPieChartModel
     */
    public ProjectPieChartModel(IDrillDown model) {
        super(model);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    @Override
    public String getItemName(int index) {
        return getItemNameAt(getMappedIndex(index));
    }

    @Override
    public double getItemValue(int index) {
        double ret = getItemValueAt(getMappedIndex(index));
        System.out.println("Getting value for category n." + index + "  : " + ret); // NOI18N

        return ret;
    }

    @Override
    public double getItemValueRel(int index) {
        long allTime = drillDown.getCurrentTime(false);

        //    long netSelfTime = drillDown.getCurrentTime(true);
        long allTimeCalc = 0;

        for (int i = 0; i < drillDown.getSubmarks().size(); i++) {
            allTimeCalc += getItemValueAt(i);
        }

        //    allTimeCalc = allTimeCalc - allTime + netSelfTime; // compensation for gross time of the current category; it gets its way in as one of the submark times (self submark time)
        if (allTimeCalc != allTime) {
            System.err.println("time mismatch: " + allTime + " != " + allTimeCalc); // NOI18N
        }

        if (allTime == 0) {
            return 1;
        }

        //    System.out.println("Ratio for mark " + index + " = " + getItemValueAt(getMappedIndex(index)) + "/" + allTime);
        return getItemValueAt(getMappedIndex(index)) / (double) allTime;
    }

    public boolean isSelectable(int index) {
        if (drillDown.getSubmarks().size() <= index) {
            return false;
        }

        if (index != -1) {
            return drillDown.canDrilldown((Mark) drillDown.getSubmarks().get(index));
        }

        return false;
    }

    private String getItemNameAt(int index) {
        if (drillDown.getSubmarks().size() <= index) {
            return ""; // NOI18N
        }

        if (((index == -1) || drillDown.isCurrent((Mark) drillDown.getSubmarks().get(index))) && !drillDown.isInSelf()) {
            return MessageFormat.format(SELF_BADGE_TEXT, new Object[] { ((Mark) drillDown.getCurrentMark()).description });
        } else {
            return ((Mark) drillDown.getSubmarks().get(index)).description;
        }
    }

    private double getItemValueAt(int index) {
        if (drillDown.getSubmarks().size() <= index) {
            return 0d;
        }

        if (((index == -1) || drillDown.isCurrent((Mark) drillDown.getSubmarks().get(index))) && !drillDown.isInSelf()) {
            return (double) drillDown.getCurrentTime(true);
        } else {
            return (double) drillDown.getMarkTime((Mark) drillDown.getSubmarks().get(index), false);
        }
    }
}
