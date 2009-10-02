/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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

package org.netbeans.lib.profiler.ui.cpu;

import org.netbeans.lib.profiler.ProfilerClient;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.results.coderegion.CodeRegionResultsSnapshot;
import org.netbeans.lib.profiler.ui.LiveResultsPanel;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import org.netbeans.lib.profiler.utils.StringUtils;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import javax.swing.*;


/**
 * A display for live code region profiling results
 *
 * @author Ian Formanek
 */
public class CodeRegionLivePanel extends JPanel implements LiveResultsPanel {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.netbeans.lib.profiler.ui.cpu.Bundle"); // NOI18N
    private static final String PANEL_NAME = messages.getString("CodeRegionLivePanel_PanelName"); // NOI18N
    private static final String NO_RESULTS_TERMINATED_MSG = messages.getString("CodeRegionLivePanel_NoResultsTerminatedMsg"); // NOI18N
    private static final String NO_RESULTS_REGION_MSG = messages.getString("CodeRegionLivePanel_NoResultsRegionMsg"); // NOI18N
    private static final String INDIVIDUAL_TIMES_MSG = messages.getString("CodeRegionLivePanel_IndividualTimesMsg"); // NOI18N
    private static final String SUMMARY_TIMES_MSG = messages.getString("CodeRegionLivePanel_SummaryTimesMsg"); // NOI18N
    private static final String TOTAL_INVOCATIONS_MSG = messages.getString("CodeRegionLivePanel_TotalInvocationsMsg"); // NOI18N
    private static final String ALL_REMEMBERED_MSG = messages.getString("CodeRegionLivePanel_AllRememberedMsg"); // NOI18N
    private static final String LAST_REMEMBERED_MSG = messages.getString("CodeRegionLivePanel_LastRememberedMsg"); // NOI18N
    private static final String INVOCATIONS_LISTED_MSG = messages.getString("CodeRegionLivePanel_InvocationsListedMsg"); // NOI18N
    private static final String AREA_ACCESS_NAME = messages.getString("CodeRegionLivePanel_AreaAccessName"); // NOI18N
                                                                                                             // -----

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private HTMLTextArea resArea;
    private ProfilerClient profilerClient;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public CodeRegionLivePanel(ProfilerClient client) {
        this.profilerClient = client;
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setLayout(new BorderLayout());
        resArea = new HTMLTextArea();
        resArea.getAccessibleContext().setAccessibleName(AREA_ACCESS_NAME);
        add(new JScrollPane(resArea), BorderLayout.CENTER);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public int getSortingColumn() {
        return CommonConstants.SORTING_COLUMN_DEFAULT; // Not used for code region
    }

    public boolean getSortingOrder() {
        return false; // Not used for code region
    }

    public String getTitle() {
        return PANEL_NAME;
    }

    public BufferedImage getViewImage(boolean onlyVisibleArea) {
        return null;
    }

    public String getViewName() {
        return null;
    }

    public boolean fitsVisibleArea() {
        return true;
    }

    public void handleRemove() {
    }

    /**
     * Called when auto refresh is on and profiling session will finish
     * to give the panel chance to do some cleanup before asynchrounous
     * call to updateLiveResults() will happen.
     */
    public void handleShutdown() {
        // Nothing to do for Code Region live results, updateLiveResults will be called explicitely from outside
    }

    // --- Save current View action support --------------------------------------
    // Code Region not used in 6.0
    public boolean hasView() {
        return false;
    }

    public void reset() {
        updateLiveResults();
    }

    public boolean supports(int instrumentationType) {
        return instrumentationType == CommonConstants.INSTR_CODE_REGION;
    }

    public void updateLiveResults() {
        try {
            CodeRegionResultsSnapshot sn = profilerClient.getCodeRegionProfilingResultsSnapshot();
            resArea.setText(getResultsText(sn.getTimes(), sn.getTimerCountsInSecond()));
        } catch (ClientUtils.TargetAppOrVMTerminated e) {
            resArea.setText("<i>" + NO_RESULTS_TERMINATED_MSG + "</i>"); // NOI18N
        }
    }

    private String getResultsText(long[] results, long timerCountsInSecond) {
        StringBuffer resultText = new StringBuffer(results.length * 10);
        StringBuffer summaryOfTimes = new StringBuffer();
        long sum = 0;
        long min;
        long max;

        if (results.length < 2) {
            resultText.append("<i>" + NO_RESULTS_REGION_MSG + "</i>"); // NOI18N
        } else {
            min = max = results[1];

            int nRes = results.length - 1;

            StringBuffer individualTimes = new StringBuffer();

            for (int i = 1; i < results.length; i++) {
                long time = results[i];
                sum += time;

                if (time > max) {
                    max = time;
                } else if (time < min) {
                    min = time;
                }

                individualTimes.append(MessageFormat.format(INDIVIDUAL_TIMES_MSG,
                                                            new Object[] {
                                                                StringUtils.mcsTimeToString((time * 1000000) / timerCountsInSecond)
                                                            }));
                individualTimes.append("<br>"); // NOI18N
            }

            summaryOfTimes.append(MessageFormat.format(SUMMARY_TIMES_MSG,
                                                       new Object[] {
                                                           StringUtils.mcsTimeToString((sum * 1000000) / timerCountsInSecond), // total
            StringUtils.mcsTimeToString((long) (((double) sum * 1000000) / nRes / timerCountsInSecond)), // average
            StringUtils.mcsTimeToString((min * 1000000) / timerCountsInSecond), // minimum
            StringUtils.mcsTimeToString((max * 1000000) / timerCountsInSecond) // maximum
                                                       }));

            resultText.append(MessageFormat.format(TOTAL_INVOCATIONS_MSG, new Object[] { "" + results[0] })); // NOI18N
            resultText.append(", "); // NOI18N

            if (results[0] <= nRes) {
                resultText.append(ALL_REMEMBERED_MSG);
            } else {
                resultText.append(MessageFormat.format(LAST_REMEMBERED_MSG, new Object[] { "" + nRes })); // NOI18N
            }

            resultText.append("<br>"); // NOI18N
            resultText.append(summaryOfTimes);
            resultText.append("<br><br><hr><br>"); // NOI18N
            resultText.append(individualTimes);
            resultText.append("<br><hr><br>"); // NOI18N
            resultText.append(MessageFormat.format(INVOCATIONS_LISTED_MSG, new Object[] { "" + nRes })); // NOI18N
            resultText.append(", "); // NOI18N
            resultText.append(summaryOfTimes);
        }

        return resultText.toString();
    }
}
