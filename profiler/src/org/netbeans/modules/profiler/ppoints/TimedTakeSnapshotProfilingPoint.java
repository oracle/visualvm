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

package org.netbeans.modules.profiler.ppoints;

import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.lib.profiler.ProfilerLogger;
import org.netbeans.lib.profiler.TargetAppRunner;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import org.netbeans.modules.profiler.LoadedSnapshot;
import org.netbeans.modules.profiler.NetBeansProfiler;
import org.netbeans.modules.profiler.ProfilerControlPanel2;
import org.netbeans.modules.profiler.ResultsManager;
import org.netbeans.modules.profiler.heapwalk.HeapWalkerManager;
import org.netbeans.modules.profiler.ppoints.ui.TimedTakeSnapshotCustomizer;
import org.netbeans.modules.profiler.ppoints.ui.ValidityAwarePanel;
import org.netbeans.modules.profiler.utils.IDEUtils;
import org.openide.ErrorManager;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.windows.TopComponent;
import java.awt.BorderLayout;
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import org.netbeans.lib.profiler.ui.UIUtils;


/**
 *
 * @author Jiri Sedlacek
 */
public final class TimedTakeSnapshotProfilingPoint extends TimedGlobalProfilingPoint implements PropertyChangeListener {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private class Report extends TopComponent {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private HTMLTextArea dataArea;
        private HTMLTextArea headerArea;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public Report() {
            initDefaults();
            initComponents();
            refreshData();
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public int getPersistenceType() {
            return TopComponent.PERSISTENCE_NEVER;
        }

        protected String preferredID() {
            return this.getClass().getName();
        }

        void refreshData() {
            StringBuilder headerAreaTextBuilder = new StringBuilder();

            headerAreaTextBuilder.append(getHeaderName());
            headerAreaTextBuilder.append("<br>"); // NOI18N
            headerAreaTextBuilder.append("&nbsp;&nbsp;&nbsp;&nbsp;");
            headerAreaTextBuilder.append(getHeaderType());
            headerAreaTextBuilder.append("<br>"); // NOI18N
            headerAreaTextBuilder.append("&nbsp;&nbsp;&nbsp;&nbsp;");
            headerAreaTextBuilder.append(getHeaderEnabled());
            headerAreaTextBuilder.append("<br>"); // NOI18N
            headerAreaTextBuilder.append("&nbsp;&nbsp;&nbsp;&nbsp;");
            headerAreaTextBuilder.append(getHeaderProject());
            headerAreaTextBuilder.append("<br>"); // NOI18N
            headerAreaTextBuilder.append("&nbsp;&nbsp;&nbsp;&nbsp;");
            headerAreaTextBuilder.append(getHeaderMode());
            headerAreaTextBuilder.append("<br>"); // NOI18N
            headerAreaTextBuilder.append("&nbsp;&nbsp;&nbsp;&nbsp;");
            headerAreaTextBuilder.append(getHeaderTarget());
            headerAreaTextBuilder.append("<br>"); // NOI18N
            headerAreaTextBuilder.append("&nbsp;&nbsp;&nbsp;&nbsp;");
            headerAreaTextBuilder.append(getHeaderResetResults());
            headerAreaTextBuilder.append("<br>"); // NOI18N
            headerAreaTextBuilder.append("&nbsp;&nbsp;&nbsp;&nbsp;");
            headerAreaTextBuilder.append(getHeaderHitsCount()); // NOI18N

            headerArea.setText(headerAreaTextBuilder.toString());

            StringBuilder dataAreaTextBuilder = new StringBuilder();

            if (!hasResults()) {
                dataAreaTextBuilder.append("&nbsp;&nbsp;&lt;" + NO_HITS_STRING + "&gt;"); // NOI18N
            } else {
                for (int i = 0; i < results.size(); i++) {
                    dataAreaTextBuilder.append("&nbsp;&nbsp;");
                    dataAreaTextBuilder.append(getDataResultItem(i));
                    dataAreaTextBuilder.append("<br>"); // NOI18N
                }
            }

            dataArea.setText(dataAreaTextBuilder.toString());
        }

        void refreshProperties() {
            setName(TimedTakeSnapshotProfilingPoint.this.getName());
            setIcon(((ImageIcon) TimedTakeSnapshotProfilingPoint.this.getFactory().getIcon()).getImage());
            getAccessibleContext().setAccessibleDescription(MessageFormat.format(REPORT_ACCESS_DESCR, new Object[] { getName() }));
        }

        private String getDataResultItem(int index) {
            Result result = results.get(index);
            String resultString = result.getResultString();
            String snapshotInformation = resultString.startsWith(SNAPSHOT_LOCATION_URLMASK)
                                         ? ("<a href='" + resultString + "'>" + OPEN_SNAPSHOT_STRING + "</a>") : resultString; // NOI18N

            return MessageFormat.format(HIT_STRING,
                                        new Object[] {
                                            (index + 1), Utils.formatLocalProfilingPointTime(result.getTimestamp()),
                                            snapshotInformation
                                        });
        }

        private String getHeaderEnabled() {
            return MessageFormat.format(HEADER_ENABLED_STRING, new Object[] { TimedTakeSnapshotProfilingPoint.this.isEnabled() });
        }

        private String getHeaderHitsCount() {
            return MessageFormat.format(HEADER_HITS_STRING, new Object[] { results.size() });
        }

        private String getHeaderMode() {
            return TimedTakeSnapshotProfilingPoint.this.getSnapshotType().equals(TYPE_PROFDATA_KEY) ? HEADER_MODE_DATA_STRING
                                                                                                    : HEADER_MODE_DUMP_STRING;
        }

        private String getHeaderName() {
            return "<h2><b>" + TimedTakeSnapshotProfilingPoint.this.getName() + "</b></h2>"; // NOI18N
        }

        private String getHeaderProject() {
            return MessageFormat.format(HEADER_PROJECT_STRING,
                                        new Object[] {
                                            ProjectUtils.getInformation(TimedTakeSnapshotProfilingPoint.this.getProject())
                                                        .getDisplayName()
                                        });
        }

        private String getHeaderResetResults() {
            return MessageFormat.format(HEADER_RESET_RESULTS_STRING,
                                        new Object[] { TimedTakeSnapshotProfilingPoint.this.getResetResults() });
        }

        private String getHeaderTarget() {
            return TimedTakeSnapshotProfilingPoint.this.getSnapshotTarget().equals(TARGET_PROJECT_KEY)
                   ? HEADER_TARGET_PROJECT_STRING
                   : MessageFormat.format(HEADER_TARGET_PROJECT_STRING,
                                          new Object[] { TimedTakeSnapshotProfilingPoint.this.getSnapshotFile() });
        }

        private String getHeaderType() {
            return MessageFormat.format(HEADER_TYPE_STRING,
                                        new Object[] { TimedTakeSnapshotProfilingPoint.this.getFactory().getType() });
        }

        private void initComponents() {
            setLayout(new BorderLayout());

            JPanel contentsPanel = new JPanel(new BorderLayout());
            contentsPanel.setBackground(UIUtils.getProfilerResultsBackground());
            contentsPanel.setOpaque(true);
            contentsPanel.setBorder(BorderFactory.createMatteBorder(0, 15, 15, 15, UIUtils.getProfilerResultsBackground()));

            headerArea = new HTMLTextArea();

            JScrollPane headerAreaScrollPane = new JScrollPane(headerArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                               JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            headerAreaScrollPane.setBorder(BorderFactory.createMatteBorder(0, 0, 15, 0, UIUtils.getProfilerResultsBackground()));
            headerAreaScrollPane.setViewportBorder(BorderFactory.createEmptyBorder());
            contentsPanel.add(headerAreaScrollPane, BorderLayout.NORTH);

            dataArea = new HTMLTextArea() {
                    protected void showURL(URL url) {
                        File resolvedFile = null;

                        try {
                            resolvedFile = new File(url.toURI());
                        } catch (URISyntaxException ex) {
                            ex.printStackTrace();
                        }

                        final File snapshotFile = resolvedFile;

                        if ((snapshotFile != null) && snapshotFile.exists()) {
                            if (TimedTakeSnapshotProfilingPoint.this.getSnapshotType().equals(TYPE_PROFDATA_KEY)) {
                                File sf = FileUtil.normalizeFile(snapshotFile);
                                LoadedSnapshot snapshot = ResultsManager.getDefault()
                                                                        .loadSnapshot(FileUtil.toFileObject(sf));
                                ResultsManager.getDefault().openSnapshot(snapshot);
                            } else if (TimedTakeSnapshotProfilingPoint.this.getSnapshotType().equals(TYPE_HEAPDUMP_KEY)) {
                                RequestProcessor.getDefault().post(new Runnable() {
                                        public void run() {
                                            HeapWalkerManager.getDefault().openHeapWalker(snapshotFile);
                                        }
                                    });
                            }
                        } else {
                            NetBeansProfiler.getDefaultNB().displayWarning(SNAPSHOT_NOT_AVAILABLE_MSG);
                        }
                    }
                };

            JScrollPane dataAreaScrollPane = new JScrollPane(dataArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                             JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            TitledBorder tb = new TitledBorder(DATA_STRING);
            tb.setTitleFont(tb.getTitleFont().deriveFont(Font.BOLD));
            tb.setTitleColor(javax.swing.UIManager.getColor("Label.foreground")); // NOI18N
            dataAreaScrollPane.setBorder(tb);
            dataAreaScrollPane.setViewportBorder(BorderFactory.createEmptyBorder());
            dataAreaScrollPane.setBackground(UIUtils.getProfilerResultsBackground());
            contentsPanel.add(dataAreaScrollPane, BorderLayout.CENTER);

            add(contentsPanel, BorderLayout.CENTER);
        }

        private void initDefaults() {
            refreshProperties();
            setFocusable(true);
        }
    }

    private static class Result {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private final String resultString;
        private final long timestamp;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public Result(long timestamp, String resultString) {
            this.timestamp = timestamp;
            this.resultString = resultString;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public String getResultString() {
            return resultString;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String NO_DATA_AVAILABLE_MSG = NbBundle.getMessage(TimedTakeSnapshotProfilingPoint.class,
                                                                            "TimedTakeSnapshotProfilingPoint_NoDataAvailableMsg"); // NOI18N
    private static final String NO_DATA_REMOTE_MSG = NbBundle.getMessage(TimedTakeSnapshotProfilingPoint.class,
                                                                         "TimedTakeSnapshotProfilingPoint_NoDataRemoteMsg"); // NOI18N
    private static final String NO_DATA_JDK_MSG = NbBundle.getMessage(TimedTakeSnapshotProfilingPoint.class,
                                                                      "TimedTakeSnapshotProfilingPoint_NoDataJdkMsg"); // NOI18N
    private static final String ONE_HIT_STRING = NbBundle.getMessage(TimedTakeSnapshotProfilingPoint.class,
                                                                     "TimedTakeSnapshotProfilingPoint_OneHitString"); // NOI18N
    private static final String N_HITS_STRING = NbBundle.getMessage(TimedTakeSnapshotProfilingPoint.class,
                                                                    "TimedTakeSnapshotProfilingPoint_NHitsString"); // NOI18N
    private static final String NO_RESULTS_STRING = NbBundle.getMessage(TimedTakeSnapshotProfilingPoint.class,
                                                                        "TimedTakeSnapshotProfilingPoint_NoResultsString"); // NOI18N
    private static final String REPORT_ACCESS_DESCR = NbBundle.getMessage(TimedTakeSnapshotProfilingPoint.class,
                                                                          "TimedTakeSnapshotProfilingPoint_ReportAccessDescr"); // NOI18N
    private static final String NO_HITS_STRING = NbBundle.getMessage(TimedTakeSnapshotProfilingPoint.class,
                                                                     "TimedTakeSnapshotProfilingPoint_NoHitsString"); // NOI18N
    private static final String HEADER_TYPE_STRING = NbBundle.getMessage(TimedTakeSnapshotProfilingPoint.class,
                                                                         "TimedTakeSnapshotProfilingPoint_HeaderTypeString"); // NOI18N
    private static final String HEADER_ENABLED_STRING = NbBundle.getMessage(TimedTakeSnapshotProfilingPoint.class,
                                                                            "TimedTakeSnapshotProfilingPoint_HeaderEnabledString"); // NOI18N
    private static final String HEADER_PROJECT_STRING = NbBundle.getMessage(TimedTakeSnapshotProfilingPoint.class,
                                                                            "TimedTakeSnapshotProfilingPoint_HeaderProjectString"); // NOI18N
    private static final String HEADER_MODE_DATA_STRING = NbBundle.getMessage(TimedTakeSnapshotProfilingPoint.class,
                                                                              "TimedTakeSnapshotProfilingPoint_HeaderModeDataString"); // NOI18N
    private static final String HEADER_MODE_DUMP_STRING = NbBundle.getMessage(TimedTakeSnapshotProfilingPoint.class,
                                                                              "TimedTakeSnapshotProfilingPoint_HeaderModeDumpString"); // NOI18N
    private static final String HEADER_TARGET_PROJECT_STRING = NbBundle.getMessage(TimedTakeSnapshotProfilingPoint.class,
                                                                                   "TimedTakeSnapshotProfilingPoint_HeaderTargetProjectString"); // NOI18N
    private static final String HEADER_TARGET_CUSTOM_STRING = NbBundle.getMessage(TimedTakeSnapshotProfilingPoint.class,
                                                                                  "TimedTakeSnapshotProfilingPoint_HeaderTargetCustomString"); // NOI18N
    private static final String HEADER_RESET_RESULTS_STRING = NbBundle.getMessage(TimedTakeSnapshotProfilingPoint.class,
                                                                                  "TimedTakeSnapshotProfilingPoint_HeaderResetResultsString"); // NOI18N
    private static final String HEADER_HITS_STRING = NbBundle.getMessage(TimedTakeSnapshotProfilingPoint.class,
                                                                         "TimedTakeSnapshotProfilingPoint_HeaderHitsString"); // NOI18N
    private static final String OPEN_SNAPSHOT_STRING = NbBundle.getMessage(TimedTakeSnapshotProfilingPoint.class,
                                                                           "TimedTakeSnapshotProfilingPoint_OpenSnapshotString"); // NOI18N
    private static final String HIT_STRING = NbBundle.getMessage(TimedTakeSnapshotProfilingPoint.class,
                                                                 "TimedTakeSnapshotProfilingPoint_HitString"); // NOI18N
    private static final String SNAPSHOT_NOT_AVAILABLE_MSG = NbBundle.getMessage(TimedTakeSnapshotProfilingPoint.class,
                                                                                 "TimedTakeSnapshotProfilingPoint_SnapshotNotAvailableMsg"); // NOI18N
    private static final String DATA_STRING = NbBundle.getMessage(TimedTakeSnapshotProfilingPoint.class,
                                                                  "TimedTakeSnapshotProfilingPoint_DataString"); // NOI18N
                                                                                                                 // -----
    public static final String TAKEN_HEAPDUMP_PREFIX = "heapdump-"; // NOI18N // should differ from generated OOME heapdumps not to be detected as OOME
    static final String PROPERTY_TYPE = "p_snapshot"; // NOI18N
    public static final String TYPE_PROFDATA_KEY = "profdata"; // NOI18N
    public static final String TYPE_HEAPDUMP_KEY = "heapdump"; // NOI18N
    static final String PROPERTY_TARGET = "p_target"; // NOI18N
    public static final String TARGET_PROJECT_KEY = "project"; // NOI18N
    public static final String TARGET_CUSTOM_KEY = "custom"; // NOI18N
    static final String PROPERTY_CUSTOM_FILE = "p_file"; // NOI18N
    static final String PROPERTY_RESET_RESULTS = "p_reset_results"; // NOI18N

    // --- Implementation --------------------------------------------------------
    private static final String SNAPSHOT_LOCATION_URLMASK = "file:"; // NOI18N
    private static final String NO_DATA_AVAILABLE_MESSAGE = NO_DATA_AVAILABLE_MSG;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private List<Result> results = new ArrayList();
    private String snapshotFile = System.getProperty("java.io.tmpdir"); // NOI18N
    private String snapshotTarget = TARGET_PROJECT_KEY;
    private String snapshotType = TYPE_PROFDATA_KEY;
    private WeakReference<Report> reportReference;
    private boolean resetResults = false;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public TimedTakeSnapshotProfilingPoint(String name, Project project, ProfilingPointFactory factory) {
        super(name, project, factory);
        getChangeSupport().addPropertyChangeListener(this);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setResetResults(boolean resetResults) {
        if (this.resetResults == resetResults) {
            return;
        }

        this.resetResults = resetResults;
        getChangeSupport().firePropertyChange(PROPERTY_RESET_RESULTS, !this.resetResults, this.resetResults);
    }

    public boolean getResetResults() {
        return resetResults;
    }

    public void setSnapshotFile(String snapshotFile) {
        if ((snapshotFile == null) || !new File(snapshotFile).exists()) {
            return;
        }

        if ((this.snapshotFile != null) && new File(this.snapshotFile).equals(new File(snapshotFile))) {
            return;
        }

        String oldSnapshotFile = this.snapshotFile;
        this.snapshotFile = snapshotFile;
        getChangeSupport().firePropertyChange(PROPERTY_CUSTOM_FILE, oldSnapshotFile, snapshotFile);
    }

    public String getSnapshotFile() {
        return snapshotFile;
    }

    public void setSnapshotTarget(String snapshotTarget) {
        if (!snapshotTarget.equals(TARGET_PROJECT_KEY) && !snapshotTarget.equals(TARGET_CUSTOM_KEY)) {
            throw new IllegalArgumentException("Invalid snapshot target category: " + snapshotTarget); // NOI18N
        }

        if (this.snapshotTarget.equals(snapshotTarget)) {
            return;
        }

        String oldSnapshotTarget = this.snapshotTarget;
        this.snapshotTarget = snapshotTarget;
        getChangeSupport().firePropertyChange(PROPERTY_TARGET, oldSnapshotTarget, snapshotTarget);
    }

    public String getSnapshotTarget() {
        return snapshotTarget;
    }

    public void setSnapshotType(String snapshotType) {
        if ((snapshotType == null) || !(snapshotType.equals(TYPE_PROFDATA_KEY) || snapshotType.equals(TYPE_HEAPDUMP_KEY))) {
            throw new IllegalArgumentException("Invalid snapshot type: " + snapshotType); // NOI18N
        }

        if (this.snapshotType.equals(snapshotType)) {
            return;
        }

        String oldSnapshotType = this.snapshotType;
        this.snapshotType = snapshotType;
        getChangeSupport().firePropertyChange(PROPERTY_TYPE, oldSnapshotType, snapshotType);
    }

    public String getSnapshotType() {
        return snapshotType;
    }

    public boolean hasResults() {
        return !results.isEmpty();
    }

    public void hideResults() {
        SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    if (hasReport()) {
                        getReport().close();
                    }
                }
            });
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (hasReport()) {
            if (evt.getPropertyName() == PROPERTY_NAME) {
                getReport().refreshProperties();
            }

            getReport().refreshData();
        }
    }

    public void showResults(URL url) {
        TopComponent topComponent = getReport();
        topComponent.open();
        topComponent.requestActive();
    }

    protected String getResultsText() {
        if (hasResults()) {
            int size = results.size();

            return (size == 1)
                   ? MessageFormat.format(ONE_HIT_STRING,
                                          new Object[] { Utils.formatLocalProfilingPointTime(results.get(size - 1).getTimestamp()) })
                   : MessageFormat.format(N_HITS_STRING,
                                          new Object[] {
                                              size, Utils.formatLocalProfilingPointTime(results.get(size - 1).getTimestamp())
                                          });
        } else {
            return NO_RESULTS_STRING;
        }
    }

    protected void updateCustomizer(ValidityAwarePanel c) {
        TimedTakeSnapshotCustomizer customizer = (TimedTakeSnapshotCustomizer) c;
        customizer.setPPName(getName());
        customizer.setPPType(TYPE_PROFDATA_KEY.equals(getSnapshotType()));
        customizer.setPPTarget(TARGET_PROJECT_KEY.equals(getSnapshotTarget()));
        customizer.setPPFile(getSnapshotFile());
        customizer.setPPResetResults(getResetResults());
        customizer.setTimeCondition(getCondition());
    }

    // ---
    FileObject getSnapshotDirectory() throws IOException {
        if (snapshotTarget.equals(TARGET_PROJECT_KEY)) {
            return IDEUtils.getProjectSettingsFolder(getProject(), true);
        } else {
            File f = new File(snapshotFile);
            f.mkdirs();

            return FileUtil.toFileObject(FileUtil.normalizeFile(f));
        }
    }

    void setValues(ValidityAwarePanel c) {
        TimedTakeSnapshotCustomizer customizer = (TimedTakeSnapshotCustomizer) c;
        setName(customizer.getPPName());
        setSnapshotType(customizer.getPPType() ? TYPE_PROFDATA_KEY : TYPE_HEAPDUMP_KEY);
        setSnapshotTarget(customizer.getPPTarget() ? TARGET_PROJECT_KEY : TARGET_CUSTOM_KEY);
        setSnapshotFile(customizer.getPPFile());
        setResetResults(customizer.getPPResetResults());
        setCondition(customizer.getTimeCondition());
    }

    void hit(long hitValue) {
        String snapshotFilename;
        long currentTime = System.currentTimeMillis();

        if (snapshotType.equals(TYPE_HEAPDUMP_KEY)) {
            snapshotFilename = takeHeapdumpHit();
        } else {
            snapshotFilename = takeSnapshotHit();

            if (getResetResults()) {
                try {
                    ResultsManager.getDefault().reset();
                    
                    TargetAppRunner runner = Profiler.getDefault().getTargetAppRunner();

                    if (runner.targetJVMIsAlive()) {
                        runner.resetTimers();
                    }
                } catch (ClientUtils.TargetAppOrVMTerminated targetAppOrVMTerminated) {
                } // ignore
            }
        }

        results.add(new Result(currentTime, snapshotFilename));
        getChangeSupport().firePropertyChange(PROPERTY_RESULTS, false, true);
    }

    void reset() {
        boolean change = hasResults();
        results.clear();

        if (change) {
            getChangeSupport().firePropertyChange(PROPERTY_RESULTS, false, true);
        }
    }

    private String getCurrentHeapDumpFilename() {
        try {
            String fileName = TAKEN_HEAPDUMP_PREFIX + System.currentTimeMillis();
            FileObject folder = getSnapshotDirectory();

            //      FileObject folder = targetFolder == null ? IDEUtils.getProjectSettingsFolder(NetBeansProfiler.getDefaultNB().getProfiledProject()) : FileUtil.toFileObject(new File(targetFolder));
            return FileUtil.toFile(folder).getAbsolutePath() + File.separator
                   + FileUtil.findFreeFileName(folder, fileName, ResultsManager.HEAPDUMP_EXTENSION) + "."
                   + ResultsManager.HEAPDUMP_EXTENSION; // NOI18N
        } catch (IOException e) {
            return null;
        }
    }

    private Report getReport() {
        if (hasReport()) {
            return reportReference.get();
        }

        Report report = new Report();
        reportReference = new WeakReference(report);

        return report;
    }

    private boolean hasReport() {
        return (reportReference != null) && (reportReference.get() != null);
    }

    private String takeHeapdumpHit() {
        TargetAppRunner runner = Profiler.getDefault().getTargetAppRunner();

        if (runner.getProfilingSessionStatus().remoteProfiling) {
            return NO_DATA_REMOTE_MSG;
        }

        if (!runner.hasSupportedJDKForHeapDump()) {
            return NO_DATA_JDK_MSG;
        }

        String dumpFileName = getCurrentHeapDumpFilename();

        if (dumpFileName == null) {
            return NO_DATA_AVAILABLE_MESSAGE;
        }

        boolean heapdumpTaken = false;

        try {
            heapdumpTaken = runner.getProfilerClient().takeHeapDump(dumpFileName);
        } catch (Exception ex) {
            ProfilerLogger.log(ex);
        }

        if (heapdumpTaken) {
            ProfilerControlPanel2.getDefault().refreshSnapshotsList();

            try {
                return new File(dumpFileName).toURI().toURL().toExternalForm();
            } catch (MalformedURLException ex) {
                ProfilerLogger.log(ex);

                return NO_DATA_AVAILABLE_MESSAGE;
            }
        } else {
            return NO_DATA_AVAILABLE_MESSAGE;
        }
    }

    private static LoadedSnapshot takeSnapshot() {
        return ResultsManager.getDefault().prepareSnapshot();
    }

    private String takeSnapshotHit() {
        LoadedSnapshot loadedSnapshot = null;
        String snapshotFilename = null;
        loadedSnapshot = takeSnapshot();

        if (loadedSnapshot != null) {
            try {
                FileObject snapshotDirectory = getSnapshotDirectory();
                FileObject profFile = snapshotDirectory.createData(ResultsManager.getDefault()
                                                                                 .getDefaultSnapshotFileName(loadedSnapshot),
                                                                   ResultsManager.SNAPSHOT_EXTENSION);
                ResultsManager.getDefault().saveSnapshot(loadedSnapshot, profFile); // Also updates list of snapshots in ProfilerControlPanel2
                snapshotFilename = FileUtil.toFile(profFile).toURI().toURL().toExternalForm();
            } catch (IOException e) {
                ErrorManager.getDefault().notify(ErrorManager.ERROR, e);
            }
        }

        return (snapshotFilename == null) ? NO_DATA_AVAILABLE_MESSAGE : snapshotFilename;
    }
}
