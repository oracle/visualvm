/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
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

package org.graalvm.visualvm.lib.profiler;

import org.graalvm.visualvm.lib.jfluid.client.ClientUtils;
import org.graalvm.visualvm.lib.common.ProfilingSettings;
import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;
import org.graalvm.visualvm.lib.ui.UIUtils;
import org.graalvm.visualvm.lib.ui.components.HTMLTextArea;
import org.graalvm.visualvm.lib.jfluid.utils.StringUtils;
import org.graalvm.visualvm.lib.jfluid.utils.formatting.MethodNameFormatterFactory;
import org.openide.util.NbBundle;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import javax.swing.*;
import org.graalvm.visualvm.lib.jfluid.filters.GenericFilter;
import org.graalvm.visualvm.lib.jfluid.utils.Wildcards;
import org.graalvm.visualvm.lib.profiler.api.icons.GeneralIcons;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.Mnemonics;
import org.openide.filesystems.FileUtil;
import org.openide.util.HelpCtx;
import org.openide.util.RequestProcessor;

@NbBundle.Messages({
    "SnapshotInfoPanel_DataCollectedFromString=Data collected from:",
    "SnapshotInfoPanel_SnapshotTakenAtString=Snapshot taken at:",
    "SnapshotInfoPanel_FileString=File:",
    "SnapshotInfoPanel_NotSavedString=Not Saved",
    "SnapshotInfoPanel_FileSizeString=File Size:",
    "SnapshotInfoPanel_SettingsString=Settings:",
    "SnapshotInfoPanel_SettingsNameString=Settings Name:",
    "SnapshotInfoPanel_ProfilingTypeString=Profiling Type:",
    "SnapshotInfoPanel_CodeRegionString=Code Region",
    "SnapshotInfoPanel_ProfiledCodeRegionString=Profiled Code Region:",
    "SnapshotInfoPanel_ExcludeSleepWaitString=Exclude time spent in Thread.sleep() and Object.wait():",
    "SnapshotInfoPanel_BufferSizeString=Buffer Size:",
    "SnapshotInfoPanel_LimitProfiledThreadsString=Limit number of profiled threads:",
    "SnapshotInfoPanel_StackDepthLimitString=Stack depth limit:",
    "SnapshotInfoPanel_UnlimitedString=Unlimited",
    "SnapshotInfoPanel_CpuSamplingString=CPU Profiling (Sampling Application)",
    "SnapshotInfoPanel_CpuEntireString=CPU Profiling (Entire Application)",
    "SnapshotInfoPanel_CpuPartString=CPU Profiling (Part of Application)",
    "SnapshotInfoPanel_MemorySamplingString=Memory (Sampling)",
    "SnapshotInfoPanel_MemoryAllocString=Memory (Allocations Only)",
    "SnapshotInfoPanel_MemoryLivenessString=Memory (Liveness)",
    "SnapshotInfoPanel_TrackingAllInstancesString=Tracking All Instances",
    "SnapshotInfoPanel_TrackEveryString=Track Every:",
    "SnapshotInfoPanel_RecordStackTracesString=Record Stack Traces:",
    "SnapshotInfoPanel_LimitStackDepthString=Limit Stack Depth:",
    "SnapshotInfoPanel_RunGcString=Run Garbage Collection When Getting Results:",
    "SnapshotInfoPanel_RootMethodsString=Root Methods:",
    "SnapshotInfoPanel_CpuProfilingTypeString=CPU Profiling Type:",
    "SnapshotInfoPanel_SamplingPeriodString=Sampling Period:",
    "SnapshotInfoPanel_CpuTimerString=CPU Timer:",
    "SnapshotInfoPanel_InstrumentationFilterString=Instrumentation Filter:",
    "SnapshotInfoPanel_InstrumentationSchemeString=Instrumentation Scheme:",
    "SnapshotInfoPanel_InstrumentMethodInvokeString=Instrument Method Invoke:",
    "SnapshotInfoPanel_InstrumentNewThreadsString=Instrument New Threads:",
    "SnapshotInfoPanel_InstrumentGettersSettersString=Instrument Getters and Setters:",
    "SnapshotInfoPanel_InstrumentEmptyMethodsString=Instrument Empty Methods:",
    "SnapshotInfoPanel_OverriddenGlobalPropertiesString=Overridden Global Properties:",
    "SnapshotInfoPanel_WorkingDirectoryString=Working Directory:",
    "SnapshotInfoPanel_ProjectPlatformNameString=<project>",
    "SnapshotInfoPanel_JavaPlatformString=Java Platform:",
    "SnapshotInfoPanel_JvmArgumentsString=JVM Arguments:",
    "SnapshotInfoPanel_CommPortString=Communication Port:",
    "SnapshotInfoPanel_YesString=Yes",
    "SnapshotInfoPanel_NoString=No",
    "SnapshotInfoPanel_OnString=On",
    "SnapshotInfoPanel_OffString=Off",
    "SnapshotInfoPanel_InvalidString=<Invalid>",
    "SnapshotInfoPanel_NoMethodsString=No methods, main(String[] args) method of first loaded class becomes root method",
    "SnapshotInfoPanel_LinesDefString={0}, lines: {1} to {2}",
    "SnapshotInfoPanel_InstrumentationProfTypeString=Instrumentation",
    "SnapshotInfoPanel_SampledInstrProfTypeString=Sampled Instrumentation",
    "SnapshotInfoPanel_SampledProfTypeString=Sampled",
    "SnapshotInfoPanel_TotalProfSchemeString=Total",
    "SnapshotInfoPanel_EagerProfSchemeString=Eager",
    "SnapshotInfoPanel_LazyProfSchemeString=Lazy",
    "SnapshotInfoPanel_InstancesCountString={0} instances",
    "SnapshotInfoPanel_InfoAreaAccessName=Snapshot properties.",
    "SnapshotInfoPanel_UserCommentsLbl=&User comments:",
    "SnapshotInfoPanel_UserCommentsCaption=Edit User Comments",
    "SnapshotInfoPanel_SummaryString=Summary:",
    "SnapshotInfoPanel_CommentsString=User comments:",
    "SnapshotInfoPanel_EditCommentsLink=edit...",
    "SnapshotInfoPanel_NoCommentsString=none",
    "SnapshotInfoPanel_ProfilingMode=Profiling Mode:",
    "SnapshotInfoPanel_MethodsAllClasses=Methods - All Classes",
    "SnapshotInfoPanel_MethodsProjectClasses=Methods - Project Classes",
    "SnapshotInfoPanel_MethodsSelectedClasses=Methods - Selected Classes",
    "SnapshotInfoPanel_MethodsSelectedMethods=Methods - Selected Methods",
    "SnapshotInfoPanel_MethodsDefinedClasses=Methods - Defined Classes",
    "SnapshotInfoPanel_ObjectsAllClasses=Objects - All Classes",
    "SnapshotInfoPanel_ObjectsProjectClasses=Objects - Project Classes",
    "SnapshotInfoPanel_ObjectsSelectedClasses=Objects - Selected Classes",
    "SnapshotInfoPanel_ProfilingTypeSampling=Sampling",
    "SnapshotInfoPanel_ProfilingTypeInstrumentation=Instrumentation",
    "SnapshotInfoPanel_ProjectClasses=Project Classes:",
    "SnapshotInfoPanel_SelectedClasses=Selected Classes:",
    "SnapshotInfoPanel_SelectedMethods=Selected Methods:",
    "SnapshotInfoPanel_RecordLifecycle=Track only live objects:",
    "SnapshotInfoPanel_RecordAllocations=Record allocations:",
    "SnapshotInfoPanel_LimitAllocations=Limit allocation calls:",
    "SnapshotInfoPanel_SqlAllQueries=SQL Queries - All Queries",
    "SnapshotInfoPanel_SqlDefinedQueries=SQL Queries - Defined Queries",
    "SnapshotInfoPanel_DefinedQueries=Defined Queries:"
})
public class SnapshotInfoPanel extends JPanel {

    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private static class UserCommentsPanel extends JPanel
    {
        //~ Instance fields ----------------------------------------------------------------------------------------------------

        private JTextArea textArea;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        UserCommentsPanel() {
            initComponents();
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        String getInputText() {
            return textArea.getText();
        }

        void setInputText(final String text) {
            textArea.setText(text);
            textArea.selectAll();
        }

        private void initComponents() {
            setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
            setLayout(new BorderLayout(0, 5));

            JLabel textLabel = new JLabel();
            Mnemonics.setLocalizedText(textLabel, Bundle.SnapshotInfoPanel_UserCommentsLbl());

            textArea = new JTextArea();
            textLabel.setLabelFor(textArea);

            textArea.requestFocus();

            JScrollPane textAreaScroll = new JScrollPane(textArea);
            textAreaScroll.setPreferredSize(new Dimension(350, 150));
            add(textAreaScroll, BorderLayout.CENTER);
            add(textLabel, BorderLayout.NORTH);

            getAccessibleContext().setAccessibleDescription(
                    NbBundle.getMessage(NotifyDescriptor.class, "ACSD_InputPanel") // NOI18N
                    );
            textArea.getAccessibleContext().setAccessibleDescription(
                    NbBundle.getMessage(NotifyDescriptor.class, "ACSD_InputField") // NOI18N
                    );
        }
    };

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final HelpCtx HELP_CTX = new HelpCtx("EditUserComments.HelpCtx"); // NOI18N
    
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private HTMLTextArea infoArea;
    private JScrollPane infoAreaScrollPane;
    private LoadedSnapshot loadedSnapshot;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public SnapshotInfoPanel(LoadedSnapshot snapshot) {
        setLayout(new BorderLayout());
        infoArea = new HTMLTextArea() {
            protected void showURL(URL url) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        String userComments = loadedSnapshot.getUserComments();
                        UserCommentsPanel panel = new UserCommentsPanel();
                        DialogDescriptor dd = new DialogDescriptor(panel, Bundle.SnapshotInfoPanel_UserCommentsCaption(),
                                true, new Object[] { DialogDescriptor.OK_OPTION, DialogDescriptor.CANCEL_OPTION },
                                    DialogDescriptor.OK_OPTION,
                                    0, HELP_CTX, null);
                        panel.setInputText(userComments);
                        Dialog d = DialogDisplayer.getDefault().createDialog(dd);
                        d.setVisible(true);
                        if (dd.getValue() == DialogDescriptor.OK_OPTION) {
                            setUserComments(panel.getInputText());
                        }
                    }
                });
            }
            public void scrollRectToVisible(Rectangle aRect) {
                if (isShowing()) super.scrollRectToVisible(aRect);
            }
        };
        infoArea.getAccessibleContext().setAccessibleName(Bundle.SnapshotInfoPanel_InfoAreaAccessName());
        infoArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        infoAreaScrollPane = new JScrollPane(infoArea);
        infoAreaScrollPane.setBorder(BorderFactory.createEmptyBorder());
        infoAreaScrollPane.setViewportBorder(BorderFactory.createEmptyBorder());
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
    
    public void setUserComments(String userComments) {
        loadedSnapshot.setUserComments(userComments);
        if (!loadedSnapshot.isSaved()) {
            updateInfo();
            final File snapshotFile = loadedSnapshot.getFile();
            if (snapshotFile != null)
                RequestProcessor.getDefault().post(new Runnable() {
                    public void run() {
                        ResultsManager.getDefault().saveSnapshot(loadedSnapshot,
                                FileUtil.toFileObject(snapshotFile));
                    }
                });
        }
    }

    public void updateInfo() {
        ProfilingSettings ps = loadedSnapshot.getSettings();

        StringBuffer htmlText = new StringBuffer(1000);

        String infoRes = Icons.getResource(GeneralIcons.INFO);
        String summaryStr = Bundle.SnapshotInfoPanel_SummaryString();
        htmlText.append("<b><img border='0' align='bottom' src='nbresloc:/").append(infoRes).append("'>&nbsp;&nbsp;").append(summaryStr).append("</b><br><hr>"); // NOI18N
        htmlText.append("<div style='margin-left: 10px;'>"); // NOI18N
        htmlText.append("<strong>"); // NOI18N
        htmlText.append(Bundle.SnapshotInfoPanel_DataCollectedFromString()).append(" "); // NOI18N
        htmlText.append("</strong>"); // NOI18N
        htmlText.append(StringUtils.formatFullDate(new Date(loadedSnapshot.getSnapshot().getBeginTime())));
        htmlText.append("<br>"); // NOI18N

        htmlText.append("<strong>"); // NOI18N
        htmlText.append(Bundle.SnapshotInfoPanel_SnapshotTakenAtString()).append(" "); // NOI18N
        htmlText.append("</strong>"); // NOI18N
        htmlText.append(StringUtils.formatFullDate(new Date(loadedSnapshot.getSnapshot().getTimeTaken())));
        htmlText.append("<br>"); // NOI18N

        htmlText.append("<strong>"); // NOI18N
        htmlText.append(Bundle.SnapshotInfoPanel_FileString()).append(" "); // NOI18N
        htmlText.append("</strong>"); // NOI18N

        File f = loadedSnapshot.getFile();

        if (f == null) {
            htmlText.append(Bundle.SnapshotInfoPanel_NotSavedString());
        } else {
            htmlText.append(f.getAbsolutePath());
            htmlText.append("<br>"); // NOI18N
            htmlText.append("<strong>"); // NOI18N
            htmlText.append(Bundle.SnapshotInfoPanel_FileSizeString()).append(" "); // NOI18N
            htmlText.append("</strong>"); // NOI18N

            NumberFormat format = NumberFormat.getIntegerInstance();
            format.setGroupingUsed(true);
            htmlText.append(format.format(f.length())).append(" B"); // NOI18N
        }
        htmlText.append("</div>"); // NOI18N
        
        String commentsRes = Icons.getResource(GeneralIcons.INFO);
        String commentsStr = Bundle.SnapshotInfoPanel_CommentsString();
        String commentsLink = Bundle.SnapshotInfoPanel_EditCommentsLink();
        String noCommentsStr = Bundle.SnapshotInfoPanel_NoCommentsString();
        htmlText.append("<br>"); // NOI18N
        htmlText.append("<br>"); // NOI18N
        htmlText.append("<b><img border='0' align='bottom' src='nbresloc:/").append(commentsRes).append("'>&nbsp;&nbsp;").append(commentsStr).append("&nbsp;&nbsp;" + "</b><a href='#'>").append(commentsLink).append("</a><br><hr>"); // NOI18N
        htmlText.append("<div style='margin-left: 10px;'>"); // NOI18N
        String comments = loadedSnapshot.getUserComments();
        comments = comments.replace("<", "&lt;").replace(">", "&gt;"); // NOI18N
        htmlText.append(comments.isEmpty() ? "&lt;" + noCommentsStr + "&gt;" : comments); // NOI18N
        htmlText.append("</div>"); // NOI18N

        htmlText.append("<br>"); // NOI18N
        htmlText.append("<br>"); // NOI18N
        String settingsRes = Icons.getResource(GeneralIcons.INFO);
        htmlText.append("<b><img border='0' align='bottom' src='nbresloc:/").append(settingsRes).append("'>&nbsp;&nbsp;").append(Bundle.SnapshotInfoPanel_SettingsString()).append("</b><br><hr>"); // NOI18N
        htmlText.append("<div style='margin-left: 10px;'>"); // NOI18N
        
        switch (ps.getProfilingType()) {
            case ProfilingSettings.PROFILE_CPU_STOPWATCH:
                htmlText.append("<strong>"); // NOI18N
                htmlText.append(Bundle.SnapshotInfoPanel_SettingsNameString()).append(" "); // NOI18N
                htmlText.append("</strong>"); // NOI18N
                htmlText.append(ps.getSettingsName());
                htmlText.append("<br>"); // NOI18N
                htmlText.append("<strong>"); // NOI18N
                htmlText.append(Bundle.SnapshotInfoPanel_ProfilingTypeString()).append(" "); // NOI18N
                htmlText.append("</strong>"); // NOI18N
                htmlText.append(Bundle.SnapshotInfoPanel_CodeRegionString());
                htmlText.append("<br>"); // NOI18N
                htmlText.append("<br>"); // NOI18N
                htmlText.append("<strong>"); // NOI18N
                htmlText.append(Bundle.SnapshotInfoPanel_ProfiledCodeRegionString()).append(" "); // NOI18N
                htmlText.append("</strong>"); // NOI18N
                htmlText.append(formatRootMethod(ps.getCodeFragmentSelection()));
                htmlText.append("<br>"); // NOI18N
                htmlText.append("<strong>"); // NOI18N
                htmlText.append(Bundle.SnapshotInfoPanel_ExcludeSleepWaitString()).append(" "); // NOI18N
                htmlText.append("</strong>"); // NOI18N
                htmlText.append(getYesNo(ps.getExcludeWaitTime()));
                htmlText.append("<br>"); // NOI18N
                htmlText.append("<strong>"); // NOI18N
                htmlText.append(Bundle.SnapshotInfoPanel_BufferSizeString()).append(" "); // NOI18N
                htmlText.append("</strong>"); // NOI18N
                htmlText.append(ps.getCodeRegionCPUResBufSize());
                htmlText.append("<br>"); // NOI18N
                htmlText.append("<strong>"); // NOI18N
                htmlText.append(Bundle.SnapshotInfoPanel_LimitProfiledThreadsString()).append(" "); // NOI18N
                htmlText.append("</strong>"); // NOI18N

                if (ps.getNProfiledThreadsLimit() < 0) {
                    htmlText.append(Bundle.SnapshotInfoPanel_UnlimitedString());
                } else {
                    htmlText.append(ps.getNProfiledThreadsLimit());
                }

                htmlText.append("<br>"); // NOI18N

                break;
            case ProfilingSettings.PROFILE_CPU_SAMPLING:
                GenericFilter filter = ps.getInstrumentationFilter();
                int filterType = filter.getType();
                if (filterType == GenericFilter.TYPE_NONE && filter.getName() == null) {
                    htmlText.append("<strong>"); // NOI18N
                    htmlText.append(Bundle.SnapshotInfoPanel_ProfilingMode()).append(" "); // NOI18N
                    htmlText.append("</strong>"); // NOI18N
                    htmlText.append(Bundle.SnapshotInfoPanel_MethodsAllClasses());
                    htmlText.append("<br>"); // NOI18N
                    htmlText.append("<strong>"); // NOI18N
                    htmlText.append(Bundle.SnapshotInfoPanel_ProfilingTypeString()).append(" "); // NOI18N
                    htmlText.append("</strong>"); // NOI18N
                    htmlText.append(Bundle.SnapshotInfoPanel_ProfilingTypeSampling());
                    htmlText.append("<br>"); // NOI18N
                    htmlText.append("<br>"); // NOI18N
                    appendSampledCPUText(htmlText, false, ps);
                } else if (filterType == GenericFilter.TYPE_INCLUSIVE && filter.getName() == null) {
                    htmlText.append("<strong>"); // NOI18N
                    htmlText.append(Bundle.SnapshotInfoPanel_ProfilingMode()).append(" "); // NOI18N
                    htmlText.append("</strong>"); // NOI18N
                    htmlText.append(Bundle.SnapshotInfoPanel_MethodsProjectClasses());
                    htmlText.append("<br>"); // NOI18N
                    htmlText.append("<strong>"); // NOI18N
                    htmlText.append(Bundle.SnapshotInfoPanel_ProfilingTypeString()).append(" "); // NOI18N
                    htmlText.append("</strong>"); // NOI18N
                    htmlText.append(Bundle.SnapshotInfoPanel_ProfilingTypeSampling());
                    htmlText.append("<br>"); // NOI18N
                    htmlText.append("<br>"); // NOI18N
                    appendSampledCPUText(htmlText, true, ps);
                } else {
                    htmlText.append("<strong>"); // NOI18N
                    htmlText.append(Bundle.SnapshotInfoPanel_SettingsNameString()).append(" "); // NOI18N
                    htmlText.append("</strong>"); // NOI18N
                    htmlText.append(ps.getSettingsName());
                    htmlText.append("<br>"); // NOI18N
                    htmlText.append("<strong>"); // NOI18N
                    htmlText.append(Bundle.SnapshotInfoPanel_ProfilingTypeString()).append(" "); // NOI18N
                    htmlText.append("</strong>"); // NOI18N
                    
                    htmlText.append(Bundle.SnapshotInfoPanel_CpuSamplingString());
                    htmlText.append("<br>"); // NOI18N
                    htmlText.append("<br>"); // NOI18N
                    appendCPUText(htmlText, ps);
                }

                break;
            case ProfilingSettings.PROFILE_CPU_ENTIRE:
                    htmlText.append("<strong>"); // NOI18N
                    htmlText.append(Bundle.SnapshotInfoPanel_SettingsNameString()).append(" "); // NOI18N
                    htmlText.append("</strong>"); // NOI18N
                    htmlText.append(ps.getSettingsName());
                    htmlText.append("<br>"); // NOI18N
                    htmlText.append("<strong>"); // NOI18N
                    htmlText.append(Bundle.SnapshotInfoPanel_ProfilingTypeString()).append(" "); // NOI18N
                    htmlText.append("</strong>"); // NOI18N
                    
                    htmlText.append(Bundle.SnapshotInfoPanel_CpuEntireString());
                    htmlText.append("<br>"); // NOI18N
                    htmlText.append("<br>"); // NOI18N
                    appendCPUText(htmlText, ps);

                break;
            case ProfilingSettings.PROFILE_CPU_PART:
                ClientUtils.SourceCodeSelection[] roots = ps.getInstrumentationRootMethods();
                if (ps.getInstrumentationFilter().getName() == null && roots != null && roots.length > 0) {
                    htmlText.append("<strong>"); // NOI18N
                    htmlText.append(Bundle.SnapshotInfoPanel_ProfilingMode()).append(" "); // NOI18N
                    htmlText.append("</strong>"); // NOI18N
                    boolean classes;
                     // NOTE: currently not possible to detect Defined Classes
                    if (Wildcards.ALLWILDCARD.equals(roots[0].getMethodName())) {
                        htmlText.append(Bundle.SnapshotInfoPanel_MethodsSelectedClasses());
                        classes = true;
                    } else {
                        htmlText.append(Bundle.SnapshotInfoPanel_MethodsSelectedMethods());
                        classes = false;
                    }
                    htmlText.append("<br>"); // NOI18N
                    htmlText.append("<strong>"); // NOI18N
                    htmlText.append(Bundle.SnapshotInfoPanel_ProfilingTypeString()).append(" "); // NOI18N
                    htmlText.append("</strong>"); // NOI18N
                    htmlText.append(Bundle.SnapshotInfoPanel_ProfilingTypeInstrumentation());
                    htmlText.append("<br>"); // NOI18N
                    htmlText.append("<br>"); // NOI18N
                    appendInstrumentedCPUText(htmlText, classes, ps);
                } else {
                    htmlText.append("<strong>"); // NOI18N
                    htmlText.append(Bundle.SnapshotInfoPanel_SettingsNameString()).append(" "); // NOI18N
                    htmlText.append("</strong>"); // NOI18N
                    htmlText.append(ps.getSettingsName());
                    htmlText.append("<br>"); // NOI18N
                    htmlText.append("<strong>"); // NOI18N
                    htmlText.append(Bundle.SnapshotInfoPanel_ProfilingTypeString()).append(" "); // NOI18N
                    htmlText.append("</strong>"); // NOI18N
                    
                    htmlText.append(Bundle.SnapshotInfoPanel_CpuPartString());
                    htmlText.append("<br>"); // NOI18N
                    htmlText.append("<br>"); // NOI18N
                    appendCPUText(htmlText, ps);
                }

                break;
            case ProfilingSettings.PROFILE_CPU_JDBC:
                filter = ps.getInstrumentationFilter();
                
                htmlText.append("<strong>"); // NOI18N
                htmlText.append(Bundle.SnapshotInfoPanel_ProfilingMode()).append(" "); // NOI18N
                htmlText.append("</strong>"); // NOI18N
                htmlText.append(filter.isAll() ? Bundle.SnapshotInfoPanel_SqlAllQueries() :
                                                 Bundle.SnapshotInfoPanel_SqlDefinedQueries());
                
                if (!filter.isAll()) {
                    htmlText.append("<br>"); // NOI18N
                    htmlText.append("<br>"); // NOI18N
                    formattedRow(htmlText, Bundle.SnapshotInfoPanel_DefinedQueries(), filter.getValue());
                }
                
                break;
            case ProfilingSettings.PROFILE_MEMORY_SAMPLING:
                filter = ps.getInstrumentationFilter();
                filterType = filter.getType();
                if (filterType == GenericFilter.TYPE_NONE && filter.getName() == null) {
                    htmlText.append("<strong>"); // NOI18N
                    htmlText.append(Bundle.SnapshotInfoPanel_ProfilingMode()).append(" "); // NOI18N
                    htmlText.append("</strong>"); // NOI18N
                    htmlText.append(Bundle.SnapshotInfoPanel_ObjectsAllClasses());
                    htmlText.append("<br>"); // NOI18N
                    htmlText.append("<strong>"); // NOI18N
                    htmlText.append(Bundle.SnapshotInfoPanel_ProfilingTypeString()).append(" "); // NOI18N
                    htmlText.append("</strong>"); // NOI18N
                    htmlText.append(Bundle.SnapshotInfoPanel_ProfilingTypeSampling());
                    htmlText.append("<br>"); // NOI18N
                    htmlText.append("<br>"); // NOI18N
                    appendSampledMemoryText(htmlText, false, ps);
                } else if (filterType == GenericFilter.TYPE_INCLUSIVE && filter.getName() == null) {
                    htmlText.append("<strong>"); // NOI18N
                    htmlText.append(Bundle.SnapshotInfoPanel_ProfilingMode()).append(" "); // NOI18N
                    htmlText.append("</strong>"); // NOI18N
                    htmlText.append(Bundle.SnapshotInfoPanel_ObjectsProjectClasses());
                    htmlText.append("<br>"); // NOI18N
                    htmlText.append("<strong>"); // NOI18N
                    htmlText.append(Bundle.SnapshotInfoPanel_ProfilingTypeString()).append(" "); // NOI18N
                    htmlText.append("</strong>"); // NOI18N
                    htmlText.append(Bundle.SnapshotInfoPanel_ProfilingTypeSampling());
                    htmlText.append("<br>"); // NOI18N
                    htmlText.append("<br>"); // NOI18N
                    appendSampledMemoryText(htmlText, true, ps);
                } else {
                    htmlText.append("<strong>"); // NOI18N
                    htmlText.append(Bundle.SnapshotInfoPanel_SettingsNameString()).append(" "); // NOI18N
                    htmlText.append("</strong>"); // NOI18N
                    htmlText.append(ps.getSettingsName());
                    htmlText.append("<br>"); // NOI18N
                    htmlText.append("<strong>"); // NOI18N
                    htmlText.append(Bundle.SnapshotInfoPanel_ProfilingTypeString()).append(" "); // NOI18N
                    htmlText.append("</strong>"); // NOI18N
                    htmlText.append(Bundle.SnapshotInfoPanel_MemorySamplingString());
                    htmlText.append("<br>"); // NOI18N
                    htmlText.append("<br>"); // NOI18N
                    appendMemoryText(htmlText, ps);
                }

                break;
            case ProfilingSettings.PROFILE_MEMORY_ALLOCATIONS:
                filter = ps.getInstrumentationFilter();
                filterType = filter.getType();
                if (filterType == GenericFilter.TYPE_INCLUSIVE && filter.getName() == null) {
                    htmlText.append("<strong>"); // NOI18N
                    htmlText.append(Bundle.SnapshotInfoPanel_ProfilingMode()).append(" "); // NOI18N
                    htmlText.append("</strong>"); // NOI18N
                    htmlText.append(Bundle.SnapshotInfoPanel_ObjectsSelectedClasses()); // NOTE: currently not possible to detect Defined Classes
                    htmlText.append("<br>"); // NOI18N
                    htmlText.append("<strong>"); // NOI18N
                    htmlText.append(Bundle.SnapshotInfoPanel_ProfilingTypeString()).append(" "); // NOI18N
                    htmlText.append("</strong>"); // NOI18N
                    htmlText.append(Bundle.SnapshotInfoPanel_ProfilingTypeInstrumentation());
                    htmlText.append("<br>"); // NOI18N
                    htmlText.append("<br>"); // NOI18N
                    appendInstrumentedMemoryText(htmlText, ps);
                } else {
                    htmlText.append("<strong>"); // NOI18N
                    htmlText.append(Bundle.SnapshotInfoPanel_SettingsNameString()).append(" "); // NOI18N
                    htmlText.append("</strong>"); // NOI18N
                    htmlText.append(ps.getSettingsName());
                    htmlText.append("<br>"); // NOI18N
                    htmlText.append("<strong>"); // NOI18N
                    htmlText.append(Bundle.SnapshotInfoPanel_ProfilingTypeString()).append(" "); // NOI18N
                    htmlText.append("</strong>"); // NOI18N

                    htmlText.append(Bundle.SnapshotInfoPanel_MemoryAllocString());
                    htmlText.append("<br>"); // NOI18N
                    htmlText.append("<br>"); // NOI18N
                    appendMemoryText(htmlText, ps);
                }

                break;
            case ProfilingSettings.PROFILE_MEMORY_LIVENESS:
                filter = ps.getInstrumentationFilter();
                filterType = filter.getType();
                if (filterType == GenericFilter.TYPE_INCLUSIVE && filter.getName() == null) {
                    htmlText.append("<strong>"); // NOI18N
                    htmlText.append(Bundle.SnapshotInfoPanel_ProfilingMode()).append(" "); // NOI18N
                    htmlText.append("</strong>"); // NOI18N
                    htmlText.append(Bundle.SnapshotInfoPanel_ObjectsSelectedClasses()); // NOTE: currently not possible to detect Defined Classes
                    htmlText.append("<br>"); // NOI18N
                    htmlText.append("<strong>"); // NOI18N
                    htmlText.append(Bundle.SnapshotInfoPanel_ProfilingTypeString()).append(" "); // NOI18N
                    htmlText.append("</strong>"); // NOI18N
                    htmlText.append(Bundle.SnapshotInfoPanel_ProfilingTypeInstrumentation());
                    htmlText.append("<br>"); // NOI18N
                    htmlText.append("<br>"); // NOI18N
                    appendInstrumentedMemoryText(htmlText, ps);
                } else {
                    htmlText.append("<strong>"); // NOI18N
                    htmlText.append(Bundle.SnapshotInfoPanel_SettingsNameString()).append(" "); // NOI18N
                    htmlText.append("</strong>"); // NOI18N
                    htmlText.append(ps.getSettingsName());
                    htmlText.append("<br>"); // NOI18N
                    htmlText.append("<strong>"); // NOI18N
                    htmlText.append(Bundle.SnapshotInfoPanel_ProfilingTypeString()).append(" "); // NOI18N
                    htmlText.append("</strong>"); // NOI18N

                    htmlText.append(Bundle.SnapshotInfoPanel_MemoryLivenessString());
                    htmlText.append("<br>"); // NOI18N
                    htmlText.append("<br>"); // NOI18N
                    appendMemoryText(htmlText, ps);
                }

                break;
        }

        appendOverriddenGlobalProperties(htmlText, ps);

        htmlText.append("</div>"); // NOI18N
        htmlText.append("<br>"); // NOI18N

        infoArea.setText(htmlText.toString());
        infoArea.setCaretPosition(0);
    }

    private static String getOnOff(boolean b) {
        return b ? Bundle.SnapshotInfoPanel_OnString() : Bundle.SnapshotInfoPanel_OffString();
    }

    private static String getYesNo(boolean b) {
        return b ? Bundle.SnapshotInfoPanel_YesString() : Bundle.SnapshotInfoPanel_NoString();
    }

    private String getCPUProfilingScheme(int type) {
        switch (type) {
            case CommonConstants.INSTRSCHEME_TOTAL:
                return Bundle.SnapshotInfoPanel_TotalProfSchemeString();
            case CommonConstants.INSTRSCHEME_EAGER:
                return Bundle.SnapshotInfoPanel_EagerProfSchemeString();
            case CommonConstants.INSTRSCHEME_LAZY:
                return Bundle.SnapshotInfoPanel_LazyProfSchemeString();
            default:
                return Bundle.SnapshotInfoPanel_InvalidString();
        }
    }

    private String getCPUProfilingType(int type) {
        switch (type) {
            case CommonConstants.CPU_INSTR_FULL:
                return Bundle.SnapshotInfoPanel_InstrumentationProfTypeString();
            case CommonConstants.CPU_INSTR_SAMPLED:
                return Bundle.SnapshotInfoPanel_SampledInstrProfTypeString();
            case CommonConstants.CPU_SAMPLED:
                return Bundle.SnapshotInfoPanel_SampledProfTypeString();
            default:
                return Bundle.SnapshotInfoPanel_InvalidString();
        }
    }
    
    private static void formattedRow(StringBuffer htmlText, String caption, String text) {
        htmlText.append("<table cellspacing='0' cellpadding='0'>"); // NOI18N
        
        htmlText.append("<tr>"); // NOI18N
        
        htmlText.append("<td valign='top' align='left'><nobr><b>"); // NOI18N
        htmlText.append(caption);
        htmlText.append("</b>&nbsp;&nbsp;</nobr></td>"); // NOI18N
        
        htmlText.append("<td valign='top' align='left' width='500'>"); // NOI18N
        htmlText.append(text);
        htmlText.append("</td>"); // NOI18N
        
        htmlText.append("</tr>"); // NOI18N
        
        htmlText.append("</table>"); // NOI18N
    }
    
    private void appendSampledCPUText(StringBuffer htmlText, boolean project, ProfilingSettings ps) {
        if (project) {
            String filter = ps.getInstrumentationFilter().getValue();
            formattedRow(htmlText, Bundle.SnapshotInfoPanel_ProjectClasses(), filter);
            htmlText.append("<br>"); // NOI18N
        }
        
        htmlText.append("<strong>"); // NOI18N
        htmlText.append(Bundle.SnapshotInfoPanel_SamplingPeriodString()).append(" "); // NOI18N
        htmlText.append("</strong>"); // NOI18N
        htmlText.append(ps.getSamplingFrequency());
        htmlText.append(" ms<br>"); // NOI18N
    }
    
    private void appendInstrumentedCPUText(StringBuffer htmlText, boolean classes, ProfilingSettings ps) {
        String roots = formatRootMethods(ps.getInstrumentationRootMethods());
        formattedRow(htmlText, classes ? Bundle.SnapshotInfoPanel_SelectedClasses() :
                                         Bundle.SnapshotInfoPanel_SelectedMethods(), roots);
        htmlText.append("<br>"); // NOI18N // TODO: formatting
        
        htmlText.append("<strong>"); // NOI18N
        htmlText.append(Bundle.SnapshotInfoPanel_CpuTimerString()).append(" "); // NOI18N
        htmlText.append("</strong>"); // NOI18N
        htmlText.append(getOnOff(ps.getThreadCPUTimerOn()));
        htmlText.append("<br>"); // NOI18N
        htmlText.append("<strong>"); // NOI18N
        htmlText.append(Bundle.SnapshotInfoPanel_ExcludeSleepWaitString()).append(" "); // NOI18N
        htmlText.append("</strong>"); // NOI18N
        htmlText.append(getYesNo(ps.getExcludeWaitTime()));
        htmlText.append("<br>"); // NOI18N
        htmlText.append("<strong>"); // NOI18N
        htmlText.append(Bundle.SnapshotInfoPanel_LimitProfiledThreadsString()).append(" "); // NOI18N
        htmlText.append("</strong>"); // NOI18N

        if (ps.getNProfiledThreadsLimit() < 0) {
            htmlText.append(Bundle.SnapshotInfoPanel_UnlimitedString());
        } else {
            htmlText.append(ps.getNProfiledThreadsLimit());
        }

        htmlText.append("<br>"); // NOI18N
        htmlText.append("<strong>"); // NOI18N
        htmlText.append(Bundle.SnapshotInfoPanel_StackDepthLimitString()).append(" "); // NOI18N
        htmlText.append("</strong>"); // NOI18N
        if (ps.getStackDepthLimit() == Integer.MAX_VALUE) {
            htmlText.append(Bundle.SnapshotInfoPanel_UnlimitedString());
        } else {
            htmlText.append(ps.getStackDepthLimit());
        }

        htmlText.append("<br>"); // NOI18N
        
        htmlText.append("<strong>"); // NOI18N
        htmlText.append(Bundle.SnapshotInfoPanel_InstrumentationSchemeString()).append(" "); // NOI18N
        htmlText.append("</strong>"); // NOI18N
        htmlText.append(getCPUProfilingScheme(ps.getInstrScheme()));
        htmlText.append("<br>"); // NOI18N
        htmlText.append("<strong>"); // NOI18N
        htmlText.append(Bundle.SnapshotInfoPanel_InstrumentMethodInvokeString()).append(" "); // NOI18N
        htmlText.append("</strong>"); // NOI18N
        htmlText.append(getYesNo(ps.getInstrumentMethodInvoke()));
        htmlText.append("<br>"); //NOI18N
        htmlText.append("<strong>"); // NOI18N
        htmlText.append(Bundle.SnapshotInfoPanel_InstrumentNewThreadsString()).append(" "); // NOI18N
        htmlText.append("</strong>"); // NOI18N
        htmlText.append(getYesNo(ps.getInstrumentSpawnedThreads()));
        htmlText.append("<br>"); // NOI18N
        htmlText.append("<strong>"); // NOI18N
        htmlText.append(Bundle.SnapshotInfoPanel_InstrumentGettersSettersString()).append(" "); // NOI18N
        htmlText.append("</strong>"); // NOI18N
        htmlText.append(getYesNo(ps.getInstrumentGetterSetterMethods()));
        htmlText.append("<br>"); // NOI18N
        htmlText.append("<strong>"); // NOI18N
        htmlText.append(Bundle.SnapshotInfoPanel_InstrumentEmptyMethodsString()).append(" "); // NOI18N
        htmlText.append("</strong>"); // NOI18N
        htmlText.append(getYesNo(ps.getInstrumentEmptyMethods()));
        htmlText.append("<br>"); // NOI18N
    }

    private void appendCPUText(StringBuffer htmlText, ProfilingSettings ps) {
        boolean sampling = ps.getProfilingType() == ProfilingSettings.PROFILE_CPU_SAMPLING;
        
        if (!sampling) {
            htmlText.append("<strong>"); // NOI18N
            htmlText.append(Bundle.SnapshotInfoPanel_RootMethodsString()).append(" "); // NOI18N
            htmlText.append("</strong>"); // NOI18N
            htmlText.append(formatRootMethods(ps.getInstrumentationRootMethods()));
            htmlText.append("<br>"); // NOI18N // TODO: formatting
        }
        htmlText.append("<strong>"); // NOI18N
        htmlText.append(Bundle.SnapshotInfoPanel_CpuProfilingTypeString()).append(" "); // NOI18N
        htmlText.append("</strong>"); // NOI18N
        htmlText.append(getCPUProfilingType(ps.getCPUProfilingType()));
        htmlText.append("<br>"); // NOI18N

        if (sampling) {
            htmlText.append("<strong>"); // NOI18N
            htmlText.append(Bundle.SnapshotInfoPanel_SamplingPeriodString()).append(" "); // NOI18N
            htmlText.append("</strong>"); // NOI18N
            htmlText.append(ps.getSamplingFrequency());
            htmlText.append(" ms<br>"); // NOI18N
        } else {
            htmlText.append("<strong>"); // NOI18N
            htmlText.append(Bundle.SnapshotInfoPanel_CpuTimerString()).append(" "); // NOI18N
            htmlText.append("</strong>"); // NOI18N
            htmlText.append(getOnOff(ps.getThreadCPUTimerOn()));
            htmlText.append("<br>"); // NOI18N
            htmlText.append("<strong>"); // NOI18N
            htmlText.append(Bundle.SnapshotInfoPanel_ExcludeSleepWaitString()).append(" "); // NOI18N
            htmlText.append("</strong>"); // NOI18N
            htmlText.append(getYesNo(ps.getExcludeWaitTime()));
            htmlText.append("<br>"); // NOI18N
            htmlText.append("<strong>"); // NOI18N
            htmlText.append(Bundle.SnapshotInfoPanel_LimitProfiledThreadsString()).append(" "); // NOI18N
            htmlText.append("</strong>"); // NOI18N

            if (ps.getNProfiledThreadsLimit() < 0) {
                htmlText.append(Bundle.SnapshotInfoPanel_UnlimitedString());
            } else {
                htmlText.append(ps.getNProfiledThreadsLimit());
            }

            htmlText.append("<br>"); // NOI18N
            htmlText.append("<strong>"); // NOI18N
            htmlText.append(Bundle.SnapshotInfoPanel_StackDepthLimitString()).append(" "); // NOI18N
            htmlText.append("</strong>"); // NOI18N
            if (ps.getStackDepthLimit() == Integer.MAX_VALUE) {
                htmlText.append(Bundle.SnapshotInfoPanel_UnlimitedString());
            } else {
                htmlText.append(ps.getStackDepthLimit());
            }

            htmlText.append("<br>"); // NOI18N
        }
        htmlText.append("<strong>"); // NOI18N
        htmlText.append(Bundle.SnapshotInfoPanel_InstrumentationFilterString()).append(" "); // NOI18N
        htmlText.append("</strong>"); // NOI18N
        htmlText.append(ps.getInstrumentationFilter().getValue());
        htmlText.append("<br>"); // NOI18N // TODO: text
        if (!sampling) {
            htmlText.append("<strong>"); // NOI18N
            htmlText.append(Bundle.SnapshotInfoPanel_InstrumentationSchemeString()).append(" "); // NOI18N
            htmlText.append("</strong>"); // NOI18N
            htmlText.append(getCPUProfilingScheme(ps.getInstrScheme()));
            htmlText.append("<br>"); // NOI18N
            htmlText.append("<strong>"); // NOI18N
            htmlText.append(Bundle.SnapshotInfoPanel_InstrumentMethodInvokeString()).append(" "); // NOI18N
            htmlText.append("</strong>"); // NOI18N
            htmlText.append(getYesNo(ps.getInstrumentMethodInvoke()));
            htmlText.append("<br>"); //NOI18N
            htmlText.append("<strong>"); // NOI18N
            htmlText.append(Bundle.SnapshotInfoPanel_InstrumentNewThreadsString()).append(" "); // NOI18N
            htmlText.append("</strong>"); // NOI18N
            htmlText.append(getYesNo(ps.getInstrumentSpawnedThreads()));
            htmlText.append("<br>"); // NOI18N
            htmlText.append("<strong>"); // NOI18N
            htmlText.append(Bundle.SnapshotInfoPanel_InstrumentGettersSettersString()).append(" "); // NOI18N
            htmlText.append("</strong>"); // NOI18N
            htmlText.append(getYesNo(ps.getInstrumentGetterSetterMethods()));
            htmlText.append("<br>"); // NOI18N
            htmlText.append("<strong>"); // NOI18N
            htmlText.append(Bundle.SnapshotInfoPanel_InstrumentEmptyMethodsString()).append(" "); // NOI18N
            htmlText.append("</strong>"); // NOI18N
            htmlText.append(getYesNo(ps.getInstrumentEmptyMethods()));
            htmlText.append("<br>"); // NOI18N
        }
    }
    
    private void appendSampledMemoryText(StringBuffer htmlText, boolean project, ProfilingSettings ps) {
        if (project) {
            String filter = ps.getInstrumentationFilter().getValue();
            formattedRow(htmlText, Bundle.SnapshotInfoPanel_ProjectClasses(), filter);
            htmlText.append("<br>"); // NOI18N
        }
        
//        htmlText.append("<strong>"); // NOI18N
//        htmlText.append(Bundle.SnapshotInfoPanel_SamplingPeriodString()).append(" "); // NOI18N
//        htmlText.append("</strong>"); // NOI18N
//        htmlText.append(ps.getSamplingInterval());
//        htmlText.append(" ms<br>"); // NOI18N
    }
    
    private void appendInstrumentedMemoryText(StringBuffer htmlText, ProfilingSettings ps) {
        htmlText.append("<strong>"); // NOI18N
        htmlText.append(Bundle.SnapshotInfoPanel_RecordLifecycle()).append(" "); // NOI18N
        htmlText.append("</strong>"); // NOI18N
        htmlText.append(getYesNo(ps.getProfilingType() == ProfilingSettings.PROFILE_MEMORY_LIVENESS));
        htmlText.append("<br>"); //NOI18N
        htmlText.append("<strong>"); // NOI18N
        htmlText.append(Bundle.SnapshotInfoPanel_RecordAllocations()).append(" "); // NOI18N
        htmlText.append("</strong>"); // NOI18N
        int allocLimit = ps.getAllocStackTraceLimit();
        htmlText.append(getYesNo(allocLimit != 0));
        htmlText.append("<br>"); //NOI18N
        htmlText.append("<strong>"); // NOI18N
        htmlText.append(Bundle.SnapshotInfoPanel_LimitAllocations()).append(" "); // NOI18N
        htmlText.append("</strong>"); // NOI18N
        htmlText.append(allocLimit < 0 ? "No" : allocLimit);
        htmlText.append("<br>"); //NOI18N
        htmlText.append("<br>"); //NOI18N
        
        String filter = ps.getInstrumentationFilter().getValue();
        formattedRow(htmlText, Bundle.SnapshotInfoPanel_SelectedClasses(), filter);
        htmlText.append("<br>"); // NOI18N
        
        if (ps.getAllocTrackEvery() == 1) {
            htmlText.append("<strong>"); // NOI18N
            htmlText.append(Bundle.SnapshotInfoPanel_TrackingAllInstancesString()).append(" "); // NOI18N
            htmlText.append("</strong>"); // NOI18N
        } else {
            htmlText.append("<strong>"); // NOI18N
            htmlText.append(Bundle.SnapshotInfoPanel_TrackEveryString()).append(" "); // NOI18N
            htmlText.append("</strong>"); // NOI18N
            htmlText.append(Bundle.SnapshotInfoPanel_InstancesCountString("" + ps.getAllocTrackEvery())); // NOI18N
        }
        htmlText.append("<br>"); //NOI18N

        htmlText.append("<strong>"); // NOI18N
        htmlText.append(Bundle.SnapshotInfoPanel_RunGcString()).append(" "); // NOI18N
        htmlText.append("</strong>"); // NOI18N
        htmlText.append(getYesNo(ps.getRunGCOnGetResultsInMemoryProfiling()));
        htmlText.append("<br>"); // NOI18N
    }

    private void appendMemoryText(StringBuffer htmlText, ProfilingSettings ps) {
        if (ps.getProfilingType() != ProfilingSettings.PROFILE_MEMORY_SAMPLING) {
            if (ps.getAllocTrackEvery() == 1) {
                htmlText.append("<strong>"); // NOI18N
                htmlText.append(Bundle.SnapshotInfoPanel_TrackingAllInstancesString()).append(" "); // NOI18N
                htmlText.append("</strong>"); // NOI18N
            } else {
                htmlText.append("<strong>"); // NOI18N
                htmlText.append(Bundle.SnapshotInfoPanel_TrackEveryString()).append(" "); // NOI18N
                htmlText.append("</strong>"); // NOI18N
                htmlText.append(Bundle.SnapshotInfoPanel_InstancesCountString("" + ps.getAllocTrackEvery())); // NOI18N
            }

            htmlText.append("<br>"); // NOI18N

            htmlText.append("<strong>"); // NOI18N
            htmlText.append(Bundle.SnapshotInfoPanel_RecordStackTracesString()).append(" "); // NOI18N
            htmlText.append("</strong>"); // NOI18N
            htmlText.append(getYesNo(ps.getAllocStackTraceLimit() != 0));
            htmlText.append("<br>"); // NOI18N

            if (ps.getAllocStackTraceLimit() != 0) {
                htmlText.append("<strong>"); // NOI18N
                htmlText.append(Bundle.SnapshotInfoPanel_LimitStackDepthString()).append(" "); // NOI18N
                htmlText.append("</strong>"); // NOI18N

                if (ps.getAllocStackTraceLimit() < 0) {
                    htmlText.append(Bundle.SnapshotInfoPanel_UnlimitedString());
                } else {
                    htmlText.append(ps.getAllocStackTraceLimit());
                }

                htmlText.append("<br>"); // NOI18N
            }
        }

        htmlText.append("<strong>"); // NOI18N
        htmlText.append(Bundle.SnapshotInfoPanel_RunGcString()).append(" "); // NOI18N
        htmlText.append("</strong>"); // NOI18N
        htmlText.append(getYesNo(ps.getRunGCOnGetResultsInMemoryProfiling()));
        htmlText.append("<br>"); // NOI18N
    }

    private void appendOverriddenGlobalProperties(StringBuffer htmlText, ProfilingSettings ps) {
        // Done
        if (ps.getOverrideGlobalSettings()) {
            htmlText.append("<br>"); // NOI18N
            htmlText.append("<strong>"); // NOI18N
            htmlText.append(Bundle.SnapshotInfoPanel_OverriddenGlobalPropertiesString()).append(" "); // NOI18N
            htmlText.append("</strong>"); // NOI18N
            htmlText.append("<br>"); // NOI18N
            htmlText.append("<div style='margin-left: 10px;'>"); // NOI18N
            htmlText.append("<strong>"); // NOI18N
            htmlText.append(Bundle.SnapshotInfoPanel_WorkingDirectoryString()).append(" "); // NOI18N
            htmlText.append("</strong>"); // NOI18N
            htmlText.append(ps.getWorkingDir());
            htmlText.append("<br>"); // NOI18N

            String platformName = ps.getJavaPlatformName();

            if (platformName == null) {
                platformName = Bundle.SnapshotInfoPanel_ProjectPlatformNameString();
            }

            htmlText.append("<strong>"); // NOI18N
            htmlText.append(Bundle.SnapshotInfoPanel_JavaPlatformString()).append(" "); // NOI18N
            htmlText.append("</strong>"); // NOI18N
            htmlText.append(platformName);
            htmlText.append("<br>"); // NOI18N
            htmlText.append("<strong>"); // NOI18N
            htmlText.append(Bundle.SnapshotInfoPanel_JvmArgumentsString()).append(" "); // NOI18N
            htmlText.append("</strong>"); // NOI18N
            htmlText.append(ps.getJVMArgs());
            htmlText.append("</div>"); // NOI18N
            htmlText.append("<br>"); // NOI18N
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
            ret = Bundle.SnapshotInfoPanel_LinesDefString(
                    method.getClassName(), 
                    "" + method.getStartLine(), 
                    "" + method.getEndLine()
            );
        }

        return ret;
    }

    private String formatRootMethods(ClientUtils.SourceCodeSelection[] methods) {
        if ((methods == null) || (methods.length == 0)) {
            return Bundle.SnapshotInfoPanel_NoMethodsString();
        } else if (methods.length == 1) {
            return formatRootMethod(methods[0]);
        } else {
            StringBuilder ret = new StringBuilder();

            java.util.List<String> rootNames = new ArrayList<String>();
            for (int i = 0; i < methods.length; i++) rootNames.add(formatRootMethod(methods[i]));
            Collections.sort(rootNames);
            for (String rootName : rootNames) ret.append(rootName).append("<br>"); // NOI18N

            return ret.toString();
        }
    }
}
