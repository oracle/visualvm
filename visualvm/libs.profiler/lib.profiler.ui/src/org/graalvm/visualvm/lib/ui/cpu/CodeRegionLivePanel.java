/*
 * Copyright (c) 1997, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.graalvm.visualvm.lib.ui.cpu;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import javax.swing.*;
import org.graalvm.visualvm.lib.jfluid.ProfilerClient;
import org.graalvm.visualvm.lib.jfluid.client.ClientUtils;
import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;
import org.graalvm.visualvm.lib.jfluid.results.coderegion.CodeRegionResultsSnapshot;
import org.graalvm.visualvm.lib.jfluid.utils.StringUtils;
import org.graalvm.visualvm.lib.ui.LiveResultsPanel;
import org.graalvm.visualvm.lib.ui.components.HTMLTextArea;


/**
 * A display for live code region profiling results
 *
 * @author Ian Formanek
 */
public class CodeRegionLivePanel extends JPanel implements LiveResultsPanel {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.graalvm.visualvm.lib.ui.cpu.Bundle"); // NOI18N
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
        StringBuilder resultText = new StringBuilder(results.length * 10);
        StringBuilder summaryOfTimes = new StringBuilder();
        long sum = 0;
        long min;
        long max;

        if (results.length < 2) {
            resultText.append("<i>").append(NO_RESULTS_REGION_MSG).append("</i>"); // NOI18N
        } else {
            min = max = results[1];

            int nRes = results.length - 1;

            StringBuilder individualTimes = new StringBuilder();

            for (int i = 1; i < results.length; i++) {
                long time = results[i];
                sum += time;

                if (time > max) {
                    max = time;
                } else if (time < min) {
                    min = time;
                }

                individualTimes.append(MessageFormat.format(INDIVIDUAL_TIMES_MSG,
                        StringUtils.mcsTimeToString((time * 1000000) / timerCountsInSecond)));
                individualTimes.append("<br>"); // NOI18N
            }

            summaryOfTimes.append(MessageFormat.format(SUMMARY_TIMES_MSG,
                    StringUtils.mcsTimeToString((sum * 1000000) / timerCountsInSecond), // total
                    StringUtils.mcsTimeToString((long) (((double) sum * 1000000) / nRes / timerCountsInSecond)), // average
                    StringUtils.mcsTimeToString((min * 1000000) / timerCountsInSecond), // minimum
                    StringUtils.mcsTimeToString((max * 1000000) / timerCountsInSecond) // maximum
            ));

            resultText.append(MessageFormat.format(TOTAL_INVOCATIONS_MSG, "" + results[0])); // NOI18N
            resultText.append(", "); // NOI18N

            if (results[0] <= nRes) {
                resultText.append(ALL_REMEMBERED_MSG);
            } else {
                resultText.append(MessageFormat.format(LAST_REMEMBERED_MSG, "" + nRes)); // NOI18N
            }

            resultText.append("<br>"); // NOI18N
            resultText.append(summaryOfTimes);
            resultText.append("<br><br><hr><br>"); // NOI18N
            resultText.append(individualTimes);
            resultText.append("<br><hr><br>"); // NOI18N
            resultText.append(MessageFormat.format(INVOCATIONS_LISTED_MSG, "" + nRes)); // NOI18N
            resultText.append(", "); // NOI18N
            resultText.append(summaryOfTimes);
        }

        return resultText.toString();
    }
}
