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

package org.netbeans.modules.profiler;

import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import org.netbeans.lib.profiler.utils.StringUtils;
import org.netbeans.lib.profiler.utils.formatting.MethodNameFormatterFactory;
import org.openide.util.NbBundle;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import javax.swing.*;


public class SnapshotInfoPanel extends JPanel {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String DATA_COLLECTED_FROM_STRING = NbBundle.getMessage(SnapshotInfoPanel.class,
                                                                                 "SnapshotInfoPanel_DataCollectedFromString"); // NOI18N
    private static final String SNAPSHOT_TAKEN_AT_STRING = NbBundle.getMessage(SnapshotInfoPanel.class,
                                                                               "SnapshotInfoPanel_SnapshotTakenAtString"); // NOI18N
    private static final String FILE_STRING = NbBundle.getMessage(SnapshotInfoPanel.class, "SnapshotInfoPanel_FileString"); // NOI18N
    private static final String NOT_SAVED_STRING = NbBundle.getMessage(SnapshotInfoPanel.class, "SnapshotInfoPanel_NotSavedString"); // NOI18N
    private static final String FILE_SIZE_STRING = NbBundle.getMessage(SnapshotInfoPanel.class, "SnapshotInfoPanel_FileSizeString"); // NOI18N
    private static final String SETTINGS_STRING = NbBundle.getMessage(SnapshotInfoPanel.class, "SnapshotInfoPanel_SettingsString"); // NOI18N
    private static final String SETTINGS_NAME_STRING = NbBundle.getMessage(SnapshotInfoPanel.class,
                                                                           "SnapshotInfoPanel_SettingsNameString"); // NOI18N
    private static final String PROFILING_TYPE_STRING = NbBundle.getMessage(SnapshotInfoPanel.class,
                                                                            "SnapshotInfoPanel_ProfilingTypeString"); // NOI18N
    private static final String CODE_REGION_STRING = NbBundle.getMessage(SnapshotInfoPanel.class,
                                                                         "SnapshotInfoPanel_CodeRegionString"); // NOI18N
    private static final String PROFILED_CODE_REGION_STRING = NbBundle.getMessage(SnapshotInfoPanel.class,
                                                                                  "SnapshotInfoPanel_ProfiledCodeRegionString"); // NOI18N
    private static final String EXCLUDE_SLEEP_WAIT_STRING = NbBundle.getMessage(SnapshotInfoPanel.class,
                                                                                "SnapshotInfoPanel_ExcludeSleepWaitString"); // NOI18N
    private static final String BUFFER_SIZE_STRING = NbBundle.getMessage(SnapshotInfoPanel.class,
                                                                         "SnapshotInfoPanel_BufferSizeString"); // NOI18N
    private static final String LIMIT_PROFILED_THREADS_STRING = NbBundle.getMessage(SnapshotInfoPanel.class,
                                                                                    "SnapshotInfoPanel_LimitProfiledThreadsString"); // NOI18N
    private static final String UNLIMITED_STRING = NbBundle.getMessage(SnapshotInfoPanel.class,
                                                                       "SnapshotInfoPanel_UnlimitedString"); // NOI18N
    private static final String CPU_ENTIRE_STRING = NbBundle.getMessage(SnapshotInfoPanel.class,
                                                                        "SnapshotInfoPanel_CpuEntireString"); // NOI18N
    private static final String CPU_PART_STRING = NbBundle.getMessage(SnapshotInfoPanel.class, "SnapshotInfoPanel_CpuPartString"); // NOI18N
    private static final String MEMORY_ALLOC_STRING = NbBundle.getMessage(SnapshotInfoPanel.class,
                                                                          "SnapshotInfoPanel_MemoryAllocString"); // NOI18N
    private static final String MEMORY_LIVENESS_STRING = NbBundle.getMessage(SnapshotInfoPanel.class,
                                                                             "SnapshotInfoPanel_MemoryLivenessString"); // NOI18N
    private static final String TRACKING_ALL_INSTANCES_STRING = NbBundle.getMessage(SnapshotInfoPanel.class,
                                                                                    "SnapshotInfoPanel_TrackingAllInstancesString"); // NOI18N
    private static final String TRACK_EVERY_STRING = NbBundle.getMessage(SnapshotInfoPanel.class,
                                                                         "SnapshotInfoPanel_TrackEveryString"); // NOI18N
    private static final String RECORD_STACK_TRACES_STRING = NbBundle.getMessage(SnapshotInfoPanel.class,
                                                                                 "SnapshotInfoPanel_RecordStackTracesString"); // NOI18N
    private static final String LIMIT_STACK_DEPTH_STRING = NbBundle.getMessage(SnapshotInfoPanel.class,
                                                                               "SnapshotInfoPanel_LimitStackDepthString"); // NOI18N
    private static final String RUN_GC_STRING = NbBundle.getMessage(SnapshotInfoPanel.class, "SnapshotInfoPanel_RunGcString"); // NOI18N
    private static final String ROOT_METHODS_STRING = NbBundle.getMessage(SnapshotInfoPanel.class,
                                                                          "SnapshotInfoPanel_RootMethodsString"); // NOI18N
    private static final String CPU_PROFILING_TYPE_STRING = NbBundle.getMessage(SnapshotInfoPanel.class,
                                                                                "SnapshotInfoPanel_CpuProfilingTypeString"); // NOI18N
    private static final String SAMPLING_PERIOD_STRING = NbBundle.getMessage(SnapshotInfoPanel.class,
                                                                             "SnapshotInfoPanel_SamplingPeriodString"); // NOI18N
    private static final String CPU_TIMER_STRING = NbBundle.getMessage(SnapshotInfoPanel.class, "SnapshotInfoPanel_CpuTimerString"); // NOI18N
    private static final String INSTRUMENTATION_FILTER_STRING = NbBundle.getMessage(SnapshotInfoPanel.class,
                                                                                    "SnapshotInfoPanel_InstrumentationFilterString"); // NOI18N
    private static final String INSTRUMENTATION_SCHEME_STRING = NbBundle.getMessage(SnapshotInfoPanel.class,
                                                                                    "SnapshotInfoPanel_InstrumentationSchemeString"); // NOI18N
    private static final String INSTRUMENT_METHOD_INVOKE_STRING = NbBundle.getMessage(SnapshotInfoPanel.class,
                                                                                      "SnapshotInfoPanel_InstrumentMethodInvokeString"); // NOI18N
    private static final String INSTRUMENT_NEW_THREADS_STRING = NbBundle.getMessage(SnapshotInfoPanel.class,
                                                                                    "SnapshotInfoPanel_InstrumentNewThreadsString"); // NOI18N
    private static final String INSTRUMENT_GETTERS_SETTERS_STRING = NbBundle.getMessage(SnapshotInfoPanel.class,
                                                                                        "SnapshotInfoPanel_InstrumentGettersSettersString"); // NOI18N
    private static final String INSTRUMENT_EMPTY_METHODS_STRING = NbBundle.getMessage(SnapshotInfoPanel.class,
                                                                                      "SnapshotInfoPanel_InstrumentEmptyMethodsString"); // NOI18N
    private static final String OVERRIDEN_GLOBAL_PROPERTIES_STRING = NbBundle.getMessage(SnapshotInfoPanel.class,
                                                                                         "SnapshotInfoPanel_OverridenGlobalPropertiesString"); // NOI18N
    private static final String WORKING_DIRECTORY_STRING = NbBundle.getMessage(SnapshotInfoPanel.class,
                                                                               "SnapshotInfoPanel_WorkingDirectoryString"); // NOI18N
    private static final String PROJECT_PLATFORM_NAME_STRING = NbBundle.getMessage(SnapshotInfoPanel.class,
                                                                                   "SnapshotInfoPanel_ProjectPlatformNameString"); // NOI18N
    private static final String JAVA_PLATFORM_STRING = NbBundle.getMessage(SnapshotInfoPanel.class,
                                                                           "SnapshotInfoPanel_JavaPlatformString"); // NOI18N
    private static final String JVM_ARGUMENTS_STRING = NbBundle.getMessage(SnapshotInfoPanel.class,
                                                                           "SnapshotInfoPanel_JvmArgumentsString"); // NOI18N
    private static final String COMM_PORT_STRING = NbBundle.getMessage(SnapshotInfoPanel.class, "SnapshotInfoPanel_CommPortString"); // NOI18N
    private static final String YES_STRING = NbBundle.getMessage(SnapshotInfoPanel.class, "SnapshotInfoPanel_YesString"); // NOI18N
    private static final String NO_STRING = NbBundle.getMessage(SnapshotInfoPanel.class, "SnapshotInfoPanel_NoString"); // NOI18N
    private static final String ON_STRING = NbBundle.getMessage(SnapshotInfoPanel.class, "SnapshotInfoPanel_OnString"); // NOI18N
    private static final String OFF_STRING = NbBundle.getMessage(SnapshotInfoPanel.class, "SnapshotInfoPanel_OffString"); // NOI18N
    private static final String INVALID_STRING = NbBundle.getMessage(SnapshotInfoPanel.class, "SnapshotInfoPanel_InvalidString"); // NOI18N
    private static final String NO_METHODS_STRING = NbBundle.getMessage(SnapshotInfoPanel.class,
                                                                        "SnapshotInfoPanel_NoMethodsString"); // NOI18N
    private static final String METHODS_COUNT_STRING = NbBundle.getMessage(SnapshotInfoPanel.class,
                                                                           "SnapshotInfoPanel_MethodsCountString"); // NOI18N
    private static final String LINES_DEF_STRING = NbBundle.getMessage(SnapshotInfoPanel.class, "SnapshotInfoPanel_LinesDefString"); // NOI18N
    private static final String INSTRUMENTATION_PROF_TYPE_STRING = NbBundle.getMessage(SnapshotInfoPanel.class,
                                                                                       "SnapshotInfoPanel_InstrumentationProfTypeString"); // NOI18N
    private static final String SAMPLED_PROF_TYPE_STRING = NbBundle.getMessage(SnapshotInfoPanel.class,
                                                                               "SnapshotInfoPanel_SampledProfTypeString"); // NOI18N
    private static final String TOTAL_PROF_SCHEME_STRING = NbBundle.getMessage(SnapshotInfoPanel.class,
                                                                               "SnapshotInfoPanel_TotalProfSchemeString"); // NOI18N
    private static final String EAGER_PROF_SCHEME_STRING = NbBundle.getMessage(SnapshotInfoPanel.class,
                                                                               "SnapshotInfoPanel_EagerProfSchemeString"); // NOI18N
    private static final String LAZY_PROF_SCHEME_STRING = NbBundle.getMessage(SnapshotInfoPanel.class,
                                                                              "SnapshotInfoPanel_LazyProfSchemeString"); // NOI18N
    private static final String INSTANCES_COUNT_STRING = NbBundle.getMessage(SnapshotInfoPanel.class,
                                                                             "SnapshotInfoPanel_InstancesCountString"); // NOI18N
    private static final String INFO_AREA_ACCESS_NAME = NbBundle.getMessage(SnapshotInfoPanel.class,
                                                                            "SnapshotInfoPanel_InfoAreaAccessName"); // NOI18N
                                                                                                                     // -----

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private HTMLTextArea infoArea;
    private JScrollPane infoAreaScrollPane;
    private LoadedSnapshot loadedSnapshot;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public SnapshotInfoPanel(LoadedSnapshot snapshot) {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        infoArea = new HTMLTextArea();
        infoArea.getAccessibleContext().setAccessibleName(INFO_AREA_ACCESS_NAME);
        infoAreaScrollPane = new JScrollPane(infoArea);
        add(infoAreaScrollPane, BorderLayout.CENTER);
        this.loadedSnapshot = snapshot;
        updateInfo();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public BufferedImage getCurrentViewScreenshot(boolean onlyVisibleArea) {
        if (onlyVisibleArea) {
            return UIUtils.createScreenshot(infoAreaScrollPane);
        } else {
            return UIUtils.createScreenshot(infoArea);
        }
    }

    public boolean fitsVisibleArea() {
        return !infoAreaScrollPane.getVerticalScrollBar().isVisible();
    }

    public void updateInfo() {
        ProfilingSettings ps = loadedSnapshot.getSettings();

        StringBuffer htmlText = new StringBuffer(1000);

        htmlText.append("<strong>"); // NOI18N
        htmlText.append(DATA_COLLECTED_FROM_STRING + " "); // NOI18N
        htmlText.append("</strong>"); // NOI18N
        htmlText.append(StringUtils.formatFullDate(new Date(loadedSnapshot.getSnapshot().getBeginTime())));
        htmlText.append("<br>"); // NOI18N

        htmlText.append("<strong>"); // NOI18N
        htmlText.append(SNAPSHOT_TAKEN_AT_STRING + " "); // NOI18N
        htmlText.append("</strong>"); // NOI18N
        htmlText.append(StringUtils.formatFullDate(new Date(loadedSnapshot.getSnapshot().getTimeTaken())));
        htmlText.append("<br>"); // NOI18N

        htmlText.append("<strong>"); // NOI18N
        htmlText.append(FILE_STRING + " "); // NOI18N
        htmlText.append("</strong>"); // NOI18N

        File f = loadedSnapshot.getFile();

        if (f == null) {
            htmlText.append(NOT_SAVED_STRING);
        } else {
            htmlText.append(f.getAbsolutePath());
            htmlText.append("<br>"); // NOI18N
            htmlText.append("<strong>"); // NOI18N
            htmlText.append(FILE_SIZE_STRING + " "); // NOI18N
            htmlText.append("</strong>"); // NOI18N

            NumberFormat format = NumberFormat.getIntegerInstance();
            format.setGroupingUsed(true);
            htmlText.append(format.format(f.length()) + " B"); // NOI18N
        }

        htmlText.append("<br>"); // NOI18N
        htmlText.append("<br>"); // NOI18N
        htmlText.append("<strong>"); // NOI18N
        htmlText.append(SETTINGS_STRING);
        htmlText.append("</strong>"); // NOI18N
        htmlText.append("<br>"); // NOI18N
        htmlText.append("<br>"); // NOI18N
        htmlText.append("<blockquote>"); // NOI18N
        htmlText.append("<strong>"); // NOI18N
        htmlText.append(" " + SETTINGS_NAME_STRING + " "); // NOI18N
        htmlText.append("</strong>"); // NOI18N
        htmlText.append(ps.getSettingsName());
        htmlText.append("<br>"); // NOI18N
        htmlText.append("<strong>"); // NOI18N
        htmlText.append(" " + PROFILING_TYPE_STRING + " "); // NOI18N
        htmlText.append("</strong>"); // NOI18N

        switch (ps.getProfilingType()) {
            case ProfilingSettings.PROFILE_CPU_STOPWATCH:
                htmlText.append(CODE_REGION_STRING);
                htmlText.append("<br>"); // NOI18N
                htmlText.append("<br>"); // NOI18N
                htmlText.append("<strong>"); // NOI18N
                htmlText.append(PROFILED_CODE_REGION_STRING + " "); // NOI18N
                htmlText.append("</strong>"); // NOI18N
                htmlText.append(formatRootMethod(ps.getCodeFragmentSelection()));
                htmlText.append("<br>"); // NOI18N
                htmlText.append("<strong>"); // NOI18N
                htmlText.append(EXCLUDE_SLEEP_WAIT_STRING + " "); // NOI18N
                htmlText.append("</strong>"); // NOI18N
                htmlText.append(getYesNo(ps.getExcludeWaitTime()));
                htmlText.append("<br>"); // NOI18N
                htmlText.append("<strong>"); // NOI18N
                htmlText.append(BUFFER_SIZE_STRING + " "); // NOI18N
                htmlText.append("</strong>"); // NOI18N
                htmlText.append(ps.getCodeRegionCPUResBufSize());
                htmlText.append("<br>"); // NOI18N
                htmlText.append("<strong>"); // NOI18N
                htmlText.append(LIMIT_PROFILED_THREADS_STRING + " "); // NOI18N
                htmlText.append("</strong>"); // NOI18N

                if (ps.getNProfiledThreadsLimit() < 0) {
                    htmlText.append(UNLIMITED_STRING);
                } else {
                    htmlText.append("" + ps.getNProfiledThreadsLimit()); // NOI18N
                }

                htmlText.append("<br>"); // NOI18N

                break;
            case ProfilingSettings.PROFILE_CPU_ENTIRE:
                htmlText.append(CPU_ENTIRE_STRING);
                htmlText.append("<br>"); // NOI18N
                htmlText.append("<br>"); // NOI18N
                appendCPUText(htmlText, ps);

                break;
            case ProfilingSettings.PROFILE_CPU_PART:
                htmlText.append(CPU_PART_STRING);
                htmlText.append("<br>"); // NOI18N
                htmlText.append("<br>"); // NOI18N
                appendCPUText(htmlText, ps);

                break;
            case ProfilingSettings.PROFILE_MEMORY_ALLOCATIONS:
                htmlText.append(MEMORY_ALLOC_STRING);
                htmlText.append("<br>"); // NOI18N
                htmlText.append("<br>"); // NOI18N
                appendMemoryText(htmlText, ps);

                break;
            case ProfilingSettings.PROFILE_MEMORY_LIVENESS:
                htmlText.append(MEMORY_LIVENESS_STRING);
                htmlText.append("<br>"); // NOI18N
                htmlText.append("<br>"); // NOI18N
                appendMemoryText(htmlText, ps);

                break;
        }

        appendOverridenGlobalProperties(htmlText, ps);

        htmlText.append("</blockquote>"); // NOI18N
        htmlText.append("<br>"); // NOI18N

        infoArea.setText(htmlText.toString());
    }

    private static String getOnOff(boolean b) {
        return b ? ON_STRING : OFF_STRING;
    }

    private static String getYesNo(boolean b) {
        return b ? YES_STRING : NO_STRING;
    }

    private String getCPUProfilingScheme(int type) {
        switch (type) {
            case CommonConstants.INSTRSCHEME_TOTAL:
                return TOTAL_PROF_SCHEME_STRING;
            case CommonConstants.INSTRSCHEME_EAGER:
                return EAGER_PROF_SCHEME_STRING;
            case CommonConstants.INSTRSCHEME_LAZY:
                return LAZY_PROF_SCHEME_STRING;
            default:
                return INVALID_STRING;
        }
    }

    private String getCPUProfilingType(int type) {
        switch (type) {
            case CommonConstants.CPU_INSTR_FULL:
                return INSTRUMENTATION_PROF_TYPE_STRING;
            case CommonConstants.CPU_INSTR_SAMPLED:
                return SAMPLED_PROF_TYPE_STRING;
            default:
                return INVALID_STRING;
        }
    }

    private void appendCPUText(StringBuffer htmlText, ProfilingSettings ps) {
        // Done
        htmlText.append("<strong>"); // NOI18N
        htmlText.append(ROOT_METHODS_STRING + " "); // NOI18N
        htmlText.append("</strong>"); // NOI18N
        htmlText.append(formatRootMethods(ps.getInstrumentationRootMethods()));
        htmlText.append("<br>"); // NOI18N // TODO: formatting
        htmlText.append("<strong>"); // NOI18N
        htmlText.append(CPU_PROFILING_TYPE_STRING + " "); // NOI18N
        htmlText.append("</strong>"); // NOI18N
        htmlText.append(getCPUProfilingType(ps.getCPUProfilingType()));
        htmlText.append("<br>"); // NOI18N

        if (ps.getCPUProfilingType() == CommonConstants.CPU_INSTR_SAMPLED) {
            htmlText.append("<strong>"); // NOI18N
            htmlText.append(SAMPLING_PERIOD_STRING + " "); // NOI18N
            htmlText.append("</strong>"); // NOI18N
            htmlText.append(ps.getSamplingInterval());
            htmlText.append(" ms<br>"); // NOI18N
        }

        htmlText.append("<strong>"); // NOI18N
        htmlText.append(CPU_TIMER_STRING + " "); // NOI18N
        htmlText.append("</strong>"); // NOI18N
        htmlText.append(getOnOff(ps.getThreadCPUTimerOn()));
        htmlText.append("<br>"); // NOI18N
        htmlText.append("<strong>"); // NOI18N
        htmlText.append(EXCLUDE_SLEEP_WAIT_STRING + " "); // NOI18N
        htmlText.append("</strong>"); // NOI18N
        htmlText.append(getYesNo(ps.getExcludeWaitTime()));
        htmlText.append("<br>"); // NOI18N
        htmlText.append("<strong>"); // NOI18N
        htmlText.append(LIMIT_PROFILED_THREADS_STRING + " "); // NOI18N
        htmlText.append("</strong>"); // NOI18N

        if (ps.getNProfiledThreadsLimit() < 0) {
            htmlText.append(UNLIMITED_STRING);
        } else {
            htmlText.append("" + ps.getNProfiledThreadsLimit()); // NOI18N
        }

        htmlText.append("<br>"); // NOI18N
        htmlText.append("<strong>"); // NOI18N
        htmlText.append(INSTRUMENTATION_FILTER_STRING + " "); // NOI18N
        htmlText.append("</strong>"); // NOI18N
        htmlText.append(ps.getSelectedInstrumentationFilter().toString());
        htmlText.append("<br>"); // NOI18N // TODO: text
        htmlText.append("<strong>"); // NOI18N
        htmlText.append(INSTRUMENTATION_SCHEME_STRING + " "); // NOI18N
        htmlText.append("</strong>"); // NOI18N
        htmlText.append(getCPUProfilingScheme(ps.getInstrScheme()));
        htmlText.append("<br>"); // NOI18N
        htmlText.append("<strong>"); // NOI18N
        htmlText.append(INSTRUMENT_METHOD_INVOKE_STRING + " "); // NOI18N
        htmlText.append("</strong>"); // NOI18N
        htmlText.append(getYesNo(ps.getInstrumentMethodInvoke()));
        htmlText.append("<br>"); //NOI18N
        htmlText.append("<strong>"); // NOI18N
        htmlText.append(INSTRUMENT_NEW_THREADS_STRING + " "); // NOI18N
        htmlText.append("</strong>"); // NOI18N
        htmlText.append(getYesNo(ps.getInstrumentSpawnedThreads()));
        htmlText.append("<br>"); // NOI18N
        htmlText.append("<strong>"); // NOI18N
        htmlText.append(INSTRUMENT_GETTERS_SETTERS_STRING + " "); // NOI18N
        htmlText.append("</strong>"); // NOI18N
        htmlText.append(getYesNo(ps.getInstrumentGetterSetterMethods()));
        htmlText.append("<br>"); // NOI18N
        htmlText.append("<strong>"); // NOI18N
        htmlText.append(INSTRUMENT_EMPTY_METHODS_STRING + " "); // NOI18N
        htmlText.append("</strong>"); // NOI18N
        htmlText.append(getYesNo(ps.getInstrumentEmptyMethods()));
        htmlText.append("<br>"); // NOI18N
    }

    private void appendMemoryText(StringBuffer htmlText, ProfilingSettings ps) {
        // Done
        if (ps.getAllocTrackEvery() == 1) {
            htmlText.append("<strong>"); // NOI18N
            htmlText.append(TRACKING_ALL_INSTANCES_STRING + " "); // NOI18N
            htmlText.append("</strong>"); // NOI18N
        } else {
            htmlText.append("<strong>"); // NOI18N
            htmlText.append(TRACK_EVERY_STRING + " "); // NOI18N
            htmlText.append("</strong>"); // NOI18N
            htmlText.append(MessageFormat.format(INSTANCES_COUNT_STRING, new Object[] { "" + ps.getAllocTrackEvery() })); // NOI18N
        }

        htmlText.append("<br>"); // NOI18N

        htmlText.append("<strong>"); // NOI18N
        htmlText.append(RECORD_STACK_TRACES_STRING + " "); // NOI18N
        htmlText.append("</strong>"); // NOI18N
        htmlText.append(getYesNo(ps.getAllocStackTraceLimit() != 0));
        htmlText.append("<br>"); // NOI18N

        if (ps.getAllocStackTraceLimit() != 0) {
            htmlText.append("<strong>"); // NOI18N
            htmlText.append(LIMIT_STACK_DEPTH_STRING + " "); // NOI18N
            htmlText.append("</strong>"); // NOI18N

            if (ps.getAllocStackTraceLimit() < 0) {
                htmlText.append(UNLIMITED_STRING);
            } else {
                htmlText.append(ps.getAllocStackTraceLimit());
            }

            htmlText.append("<br>"); // NOI18N
        }

        htmlText.append("<strong>"); // NOI18N
        htmlText.append(RUN_GC_STRING + " "); // NOI18N
        htmlText.append("</strong>"); // NOI18N
        htmlText.append(getYesNo(ps.getRunGCOnGetResultsInMemoryProfiling()));
        htmlText.append("<br>"); // NOI18N
    }

    private void appendOverridenGlobalProperties(StringBuffer htmlText, ProfilingSettings ps) {
        // Done
        if (ps.getOverrideGlobalSettings()) {
            htmlText.append("<br>"); // NOI18N
            htmlText.append("<strong>"); // NOI18N
            htmlText.append(OVERRIDEN_GLOBAL_PROPERTIES_STRING + " "); // NOI18N
            htmlText.append("</strong>"); // NOI18N
            htmlText.append("<br>"); // NOI18N
            htmlText.append("<blockquote>"); //NOI18N
            htmlText.append("<strong>"); // NOI18N
            htmlText.append(WORKING_DIRECTORY_STRING + " "); // NOI18N
            htmlText.append("</strong>"); // NOI18N
            htmlText.append(ps.getWorkingDir());
            htmlText.append("<br>"); // NOI18N

            String platformName = ps.getJavaPlatformName();

            if (platformName == null) {
                platformName = PROJECT_PLATFORM_NAME_STRING;
            }

            htmlText.append("<strong>"); // NOI18N
            htmlText.append(JAVA_PLATFORM_STRING + " "); // NOI18N
            htmlText.append("</strong>"); // NOI18N
            htmlText.append(platformName);
            htmlText.append("<br>"); // NOI18N
            htmlText.append("<strong>"); // NOI18N
            htmlText.append(JVM_ARGUMENTS_STRING + " "); // NOI18N
            htmlText.append("</strong>"); // NOI18N
            htmlText.append(ps.getJVMArgs());
            htmlText.append("<br>"); // NOI18N
            htmlText.append("</blockquote>"); // NOI18N
        }
    }

    private String formatRootMethod(ClientUtils.SourceCodeSelection method) {
        String ret;

        if (method.definedViaMethodName()) {
            //      ret =
            //      new MethodNameFormatter(
            //          method.getClassName(), method.getMethodName(), method.getMethodSignature()
            //      ).getFormattedClassAndMethod();
            ret = MethodNameFormatterFactory.getDefault().getFormatter().formatMethodName(method).toFormatted();
            ret = ret.replace("<", "&lt;"); // NOI18N
            ret = ret.replace(">", "&gt;"); // NOI18N
        } else {
            ret = MessageFormat.format(LINES_DEF_STRING,
                                       new String[] {
                                           method.getClassName(), "" + method.getStartLine(), "" + method.getEndLine()
                                       } // NOI18N
            );
        }

        return ret;
    }

    private String formatRootMethods(ClientUtils.SourceCodeSelection[] methods) {
        if ((methods == null) || (methods.length == 0)) {
            return NO_METHODS_STRING;
        } else if (methods.length == 1) {
            return formatRootMethod(methods[0]);
        } else {
            StringBuffer ret = new StringBuffer();

            ret.append(MessageFormat.format(METHODS_COUNT_STRING, new Object[] { "" + methods.length })); // NOI18N
            ret.append("<br>"); // NOI18N

            ret.append("<blockquote>"); // NOI18N

            java.util.List<String> rootNames = new ArrayList<String>();

            for (int i = 0; i < methods.length; i++) {
                String frm = formatRootMethod(methods[i]);
                rootNames.add(frm);
            }

            Collections.sort(rootNames);

            for (String rootName : rootNames) {
                ret.append(rootName);
                ret.append("<br>"); // NOI18N
            }

            ret.append("</blockquote>"); // NOI18N

            return ret.toString();
        }
    }
}
