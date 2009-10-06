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

import org.netbeans.lib.profiler.results.ResultsSnapshot;
import org.netbeans.lib.profiler.results.coderegion.CodeRegionResultsSnapshot;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import org.netbeans.lib.profiler.utils.StringUtils;
import java.awt.*;
import java.text.MessageFormat;
import java.util.Date;
import java.util.ResourceBundle;
import javax.swing.*;


/**
 * A display for snapshot of code region profiling results
 *
 * @author Ian Formanek
 */
public class CodeRegionSnapshotPanel extends JPanel {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.netbeans.lib.profiler.ui.cpu.Bundle"); // NOI18N
    private static final String PANEL_NAME = messages.getString("CodeRegionSnapshotPanel_PanelName"); // NOI18N
    private static final String NO_RESULTS_REGION_MSG = messages.getString("CodeRegionSnapshotPanel_NoResultsRegionMsg"); // NOI18N
    private static final String INDIVIDUAL_TIMES_MSG = messages.getString("CodeRegionSnapshotPanel_IndividualTimesMsg"); // NOI18N
    private static final String SUMMARY_TIMES_MSG = messages.getString("CodeRegionSnapshotPanel_SummaryTimesMsg"); // NOI18N
    private static final String TOTAL_INVOCATIONS_MSG = messages.getString("CodeRegionSnapshotPanel_TotalInvocationsMsg"); // NOI18N
    private static final String ALL_REMEMBERED_MSG = messages.getString("CodeRegionSnapshotPanel_AllRememberedMsg"); // NOI18N
    private static final String LAST_REMEMBERED_MSG = messages.getString("CodeRegionSnapshotPanel_LastRememberedMsg"); // NOI18N
    private static final String INVOCATIONS_LISTED_MSG = messages.getString("CodeRegionSnapshotPanel_InvocationsListedMsg"); // NOI18N
    private static final String AREA_ACCESS_NAME = messages.getString("CodeRegionSnapshotPanel_AreaAccessName"); // NOI18N
                                                                                                                 // -----

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private CodeRegionResultsSnapshot snapshot;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public CodeRegionSnapshotPanel(CodeRegionResultsSnapshot snapshot) {
        this.snapshot = snapshot;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        long[] results = snapshot.getTimes();
        long timerCountsInSecond = snapshot.getTimerCountsInSecond();
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

        HTMLTextArea resArea = new HTMLTextArea(resultText.toString());
        resArea.getAccessibleContext().setAccessibleName(AREA_ACCESS_NAME);
        add(new JScrollPane(resArea), BorderLayout.CENTER);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public ResultsSnapshot getSnapshot() {
        return snapshot;
    }

    public String getTitle() {
        return MessageFormat.format(PANEL_NAME, new Object[] { StringUtils.formatUserDate(new Date(snapshot.getTimeTaken())) });
    }
}
