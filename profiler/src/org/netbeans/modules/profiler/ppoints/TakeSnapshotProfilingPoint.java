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
import org.netbeans.lib.profiler.TargetAppRunner;
import org.netbeans.lib.profiler.client.RuntimeProfilingPoint;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.results.cpu.CPUResultsSnapshot;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import org.netbeans.modules.profiler.LoadedSnapshot;
import org.netbeans.modules.profiler.NetBeansProfiler;
import org.netbeans.modules.profiler.ProfilerControlPanel2;
import org.netbeans.modules.profiler.ResultsManager;
import org.netbeans.modules.profiler.actions.HeapDumpAction;
import org.netbeans.modules.profiler.heapwalk.HeapWalkerManager;
import org.netbeans.modules.profiler.ppoints.ui.TakeSnapshotCustomizer;
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
import org.openide.cookies.EditorCookie;
import org.openide.loaders.DataObject;


/**
 *
 * @author Tomas Hurka
 * @author Jiri Sedlacek
 */
public final class TakeSnapshotProfilingPoint extends CodeProfilingPoint.Single implements PropertyChangeListener {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private class Annotation extends CodeProfilingPoint.Annotation {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public String getAnnotationType() {
            return TakeSnapshotProfilingPoint.this.isEnabled() ? ANNOTATION_ENABLED : ANNOTATION_DISABLED;
        }

        public String getShortDescription() {
            return getName();
        }
    }

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
            headerAreaTextBuilder.append(getHeaderLocation());
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
            setName(TakeSnapshotProfilingPoint.this.getName());
            setIcon(((ImageIcon) TakeSnapshotProfilingPoint.this.getFactory().getIcon()).getImage());
            getAccessibleContext().setAccessibleDescription(MessageFormat.format(REPORT_ACCESS_DESCR, new Object[] { getName() }));
        }

        private String getDataResultItem(int index) {
            Result result = results.get(index);

            // TODO: enable once thread name by id is available
            //String threadName = Utils.getThreadName(result.getThreadID());
            //String threadClassName = Utils.getThreadClassName(result.getThreadID());
            //String threadInformation = (threadName == null ? "&lt;unknown thread&gt;" : (threadClassName == null ? threadName : threadName + " (" + threadClassName + ")"));
            String resultString = result.getResultString();
            String snapshotInformation = resultString.startsWith(SNAPSHOT_LOCATION_URLMASK)
                                         ? ("<a href='" + resultString + "'>" + OPEN_SNAPSHOT_STRING + "</a>") : resultString; // NOI18N
                                                                                                                               //return "<b>" + (index + 1) + ".</b> hit at <b>" + Utils.formatProfilingPointTimeHiRes(result.getTimestamp()) + "</b> by " + threadInformation + ", " + snapshotInformation;

            return MessageFormat.format(HIT_STRING,
                                        new Object[] {
                                            (index + 1), Utils.formatProfilingPointTimeHiRes(result.getTimestamp()),
                                            snapshotInformation
                                        });
        }

        private String getHeaderEnabled() {
            return MessageFormat.format(HEADER_ENABLED_STRING, new Object[] { TakeSnapshotProfilingPoint.this.isEnabled() });
        }

        private String getHeaderHitsCount() {
            return MessageFormat.format(HEADER_HITS_STRING, new Object[] { results.size() });
        }

        private String getHeaderLocation() {
            CodeProfilingPoint.Location location = TakeSnapshotProfilingPoint.this.getLocation();
            String shortFileName = new File(location.getFile()).getName();
            int lineNumber = location.getLine();

            return MessageFormat.format(HEADER_LOCATION_STRING, new Object[] { shortFileName, lineNumber });
        }

        private String getHeaderMode() {
            return TakeSnapshotProfilingPoint.this.getSnapshotType().equals(TYPE_PROFDATA_KEY) ? HEADER_MODE_DATA_STRING
                                                                                               : HEADER_MODE_DUMP_STRING;
        }

        private String getHeaderName() {
            return "<h2><b>" + TakeSnapshotProfilingPoint.this.getName() + "</b></h2>"; // NOI18N
        }

        private String getHeaderProject() {
            return MessageFormat.format(HEADER_PROJECT_STRING,
                                        new Object[] {
                                            ProjectUtils.getInformation(TakeSnapshotProfilingPoint.this.getProject())
                                                        .getDisplayName()
                                        });
        }

        private String getHeaderResetResults() {
            return MessageFormat.format(HEADER_RESET_RESULTS_STRING,
                                        new Object[] { TakeSnapshotProfilingPoint.this.getResetResults() });
        }

        private String getHeaderTarget() {
            return TakeSnapshotProfilingPoint.this.getSnapshotTarget().equals(TARGET_PROJECT_KEY) ? HEADER_TARGET_PROJECT_STRING
                                                                                                  : MessageFormat.format(HEADER_TARGET_PROJECT_STRING,
                                                                                                                         new Object[] {
                                                                                                                             TakeSnapshotProfilingPoint.this
                                                                                                                             .getSnapshotFile()
                                                                                                                         });
        }

        private String getHeaderType() {
            return MessageFormat.format(HEADER_TYPE_STRING,
                                        new Object[] { TakeSnapshotProfilingPoint.this.getFactory().getType() });
        }

        private void initComponents() {
            setLayout(new BorderLayout());

            JPanel contentsPanel = new JPanel(new BorderLayout());
            contentsPanel.setBackground(UIUtils.getProfilerResultsBackground());
            contentsPanel.setOpaque(true);
            contentsPanel.setBorder(BorderFactory.createMatteBorder(0, 15, 15, 15, UIUtils.getProfilerResultsBackground()));

            headerArea = new HTMLTextArea() {
                    protected void showURL(URL url) {
                        Utils.openLocation(TakeSnapshotProfilingPoint.this.getLocation());
                    }
                };

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
                            if (TakeSnapshotProfilingPoint.this.getSnapshotType().equals(TYPE_PROFDATA_KEY)) {
                                File sf = FileUtil.normalizeFile(snapshotFile);
                                LoadedSnapshot snapshot = ResultsManager.getDefault()
                                                                        .loadSnapshot(FileUtil.toFileObject(sf));
                                ResultsManager.getDefault().openSnapshot(snapshot);
                            } else if (TakeSnapshotProfilingPoint.this.getSnapshotType().equals(TYPE_HEAPDUMP_KEY)) {
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
        private final int threadId;
        private final long timestamp;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public Result(long timestamp, int threadId, String resultString) {
            this.timestamp = timestamp;
            this.threadId = threadId;
            this.resultString = resultString;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public String getResultString() {
            return resultString;
        }

        public int getThreadID() {
            return threadId;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String NO_DATA_AVAILABLE_MSG = NbBundle.getMessage(TakeSnapshotProfilingPoint.class,
                                                                            "TakeSnapshotProfilingPoint_NoDataAvailableMsg"); // NOI18N
    private static final String NO_DATA_REMOTE_MSG = NbBundle.getMessage(TriggeredTakeSnapshotProfilingPoint.class,
                                                                         "TakeSnapshotProfilingPoint_RemoteUnsupportedMsg"); // NOI18N
    private static final String NO_DATA_JDK_MSG = NbBundle.getMessage(TriggeredTakeSnapshotProfilingPoint.class,
                                                                      "TakeSnapshotProfilingPoint_NoDataJdkMsg"); // NOI18N
    private static final String ONE_HIT_STRING = NbBundle.getMessage(TakeSnapshotProfilingPoint.class,
                                                                     "TakeSnapshotProfilingPoint_OneHitString"); // NOI18N
    private static final String N_HITS_STRING = NbBundle.getMessage(TakeSnapshotProfilingPoint.class,
                                                                    "TakeSnapshotProfilingPoint_NHitsString"); // NOI18N
    private static final String NO_RESULTS_STRING = NbBundle.getMessage(TakeSnapshotProfilingPoint.class,
                                                                        "TakeSnapshotProfilingPoint_NoResultsString"); // NOI18N
    private static final String REPORT_ACCESS_DESCR = NbBundle.getMessage(TakeSnapshotProfilingPoint.class,
                                                                          "TakeSnapshotProfilingPoint_ReportAccessDescr"); // NOI18N
    private static final String NO_HITS_STRING = NbBundle.getMessage(TakeSnapshotProfilingPoint.class,
                                                                     "TakeSnapshotProfilingPoint_NoHitsString"); // NOI18N
    private static final String HEADER_TYPE_STRING = NbBundle.getMessage(TakeSnapshotProfilingPoint.class,
                                                                         "TakeSnapshotProfilingPoint_HeaderTypeString"); // NOI18N
    private static final String HEADER_ENABLED_STRING = NbBundle.getMessage(TakeSnapshotProfilingPoint.class,
                                                                            "TakeSnapshotProfilingPoint_HeaderEnabledString"); // NOI18N
    private static final String HEADER_PROJECT_STRING = NbBundle.getMessage(TakeSnapshotProfilingPoint.class,
                                                                            "TakeSnapshotProfilingPoint_HeaderProjectString"); // NOI18N
    private static final String HEADER_LOCATION_STRING = NbBundle.getMessage(TakeSnapshotProfilingPoint.class,
                                                                             "TakeSnapshotProfilingPoint_HeaderLocationString"); // NOI18N
    private static final String HEADER_MODE_DATA_STRING = NbBundle.getMessage(TakeSnapshotProfilingPoint.class,
                                                                              "TakeSnapshotProfilingPoint_HeaderModeDataString"); // NOI18N
    private static final String HEADER_MODE_DUMP_STRING = NbBundle.getMessage(TakeSnapshotProfilingPoint.class,
                                                                              "TakeSnapshotProfilingPoint_HeaderModeDumpString"); // NOI18N
    private static final String HEADER_TARGET_PROJECT_STRING = NbBundle.getMessage(TakeSnapshotProfilingPoint.class,
                                                                                   "TakeSnapshotProfilingPoint_HeaderTargetProjectString"); // NOI18N
    private static final String HEADER_TARGET_CUSTOM_STRING = NbBundle.getMessage(TakeSnapshotProfilingPoint.class,
                                                                                  "TakeSnapshotProfilingPoint_HeaderTargetCustomString"); // NOI18N
    private static final String HEADER_RESET_RESULTS_STRING = NbBundle.getMessage(TakeSnapshotProfilingPoint.class,
                                                                                  "TakeSnapshotProfilingPoint_HeaderResetResultsString"); // NOI18N
    private static final String HEADER_HITS_STRING = NbBundle.getMessage(TakeSnapshotProfilingPoint.class,
                                                                         "TakeSnapshotProfilingPoint_HeaderHitsString"); // NOI18N
    private static final String OPEN_SNAPSHOT_STRING = NbBundle.getMessage(TakeSnapshotProfilingPoint.class,
                                                                           "TakeSnapshotProfilingPoint_OpenSnapshotString"); // NOI18N
    private static final String HIT_STRING = NbBundle.getMessage(TakeSnapshotProfilingPoint.class,
                                                                 "TakeSnapshotProfilingPoint_HitString"); // NOI18N
    private static final String SNAPSHOT_NOT_AVAILABLE_MSG = NbBundle.getMessage(TakeSnapshotProfilingPoint.class,
                                                                                 "TakeSnapshotProfilingPoint_SnapshotNotAvailableMsg"); // NOI18N
    private static final String DATA_STRING = NbBundle.getMessage(TakeSnapshotProfilingPoint.class,
                                                                  "TakeSnapshotProfilingPoint_DataString"); // NOI18N
                                                                                                            // -----
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
    private static final String ANNOTATION_ENABLED = "takeSnapshotProfilingPoint"; // NOI18N
    private static final String ANNOTATION_DISABLED = "takeSnapshotProfilingPointD"; // NOI18N

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private Annotation annotation;
    private List<Result> results = new ArrayList();
    private String snapshotFile = System.getProperty("java.io.tmpdir"); // NOI18N
    private String snapshotTarget = TARGET_PROJECT_KEY;
    private String snapshotType = TYPE_PROFDATA_KEY;
    private WeakReference<Report> reportReference;
    private boolean resetResults = false;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public TakeSnapshotProfilingPoint(String name, Location location, Project project) {
        super(name, location, project);
        getChangeSupport().addPropertyChangeListener(this);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public ProfilingPointFactory getFactory() {
        return TakeSnapshotProfilingPointFactory.getDefault();
    }

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

    protected CodeProfilingPoint.Annotation getAnnotation() {
        if (annotation == null) {
            annotation = new Annotation();
        }

        return annotation;
    }

    protected String getResultsText() {
        if (hasResults()) {
            int size = results.size();

            return (results.size() == 1)
                   ? MessageFormat.format(ONE_HIT_STRING,
                                          new Object[] {
                                              Utils.formatProfilingPointTime(results.get(results.size() - 1).getTimestamp())
                                          })
                   : MessageFormat.format(N_HITS_STRING,
                                          new Object[] {
                                              results.size(),
                                              Utils.formatProfilingPointTime(results.get(results.size() - 1).getTimestamp())
                                          });
        } else {
            return NO_RESULTS_STRING;
        }
    }

    protected void updateCustomizer(ValidityAwarePanel c) {
        TakeSnapshotCustomizer customizer = (TakeSnapshotCustomizer) c;
        customizer.setPPName(getName());
        customizer.setPPLocation(getLocation());
        customizer.setPPType(TYPE_PROFDATA_KEY.equals(getSnapshotType()));
        customizer.setPPTarget(TARGET_PROJECT_KEY.equals(getSnapshotTarget()));
        customizer.setPPFile(getSnapshotFile());
        customizer.setPPResetResults(getResetResults());
    }

    String getServerHandlerClassName() {
        if (getSnapshotType().equals(TYPE_HEAPDUMP_KEY)) {
            return "org.netbeans.lib.profiler.server.TakeHeapdumpProfilingPointHandler"; // NOI18N
        }

        if (getResetResults()) {
            return "org.netbeans.lib.profiler.server.TakeSnapshotWithResetProfilingPointHandler"; // NOI18N
        }

        return "org.netbeans.lib.profiler.server.TakeSnapshotProfilingPointHandler"; // NOI18N
    }

    String getServerInfo() {
        if (getSnapshotType().equals(TYPE_HEAPDUMP_KEY)) {
            try {
                return FileUtil.toFile(getSnapshotDirectory()).getAbsolutePath();
            } catch (IOException ex) {
                ErrorManager.getDefault().notify(ErrorManager.ERROR, ex);
            }
        }

        return null;
    }

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
        TakeSnapshotCustomizer customizer = (TakeSnapshotCustomizer) c;
        setName(customizer.getPPName());
        setLocation(customizer.getPPLocation());
        setSnapshotType(customizer.getPPType() ? TYPE_PROFDATA_KEY : TYPE_HEAPDUMP_KEY);
        setSnapshotTarget(customizer.getPPTarget() ? TARGET_PROJECT_KEY : TARGET_CUSTOM_KEY);
        setSnapshotFile(customizer.getPPFile());
        setResetResults(customizer.getPPResetResults());
        
        Utils.checkLocation(this);
    }

    void hit(RuntimeProfilingPoint.HitEvent hitEvent, int index) {
        String snapshotFilename;

        if (snapshotType.equals(TYPE_HEAPDUMP_KEY)) {
            TargetAppRunner runner = Profiler.getDefault().getTargetAppRunner();

            if (runner.getProfilingSessionStatus().remoteProfiling) {
                snapshotFilename = NO_DATA_REMOTE_MSG;
            } else if (!runner.hasSupportedJDKForHeapDump()) {
                snapshotFilename = NO_DATA_JDK_MSG;
            } else {
                snapshotFilename = takeHeapdumpHit(hitEvent.getTimestamp());
            }
        } else {
            snapshotFilename = takeSnapshotHit();
        }

        results.add(new Result(hitEvent.getTimestamp(), hitEvent.getThreadId(), snapshotFilename));
        getChangeSupport().firePropertyChange(PROPERTY_RESULTS, false, true);
    }

    void reset() {
        boolean change = hasResults();
        results.clear();

        if (change) {
            getChangeSupport().firePropertyChange(PROPERTY_RESULTS, false, true);
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

    private File constructHeapDumpFile(long time) throws IOException {
        String dir = FileUtil.toFile(getSnapshotDirectory()).getAbsolutePath();
        String heapDumpFileName = dir + File.separatorChar + HeapDumpAction.TAKEN_HEAPDUMP_PREFIX + time + "."
                                  + ResultsManager.HEAPDUMP_EXTENSION; // NOI18N

        return new File(heapDumpFileName);
    }

    private boolean hasReport() {
        return (reportReference != null) && (reportReference.get() != null);
    }

    private String takeHeapdumpHit(long time) {
        try {
            File heapdumpFile = constructHeapDumpFile(time);

            if (heapdumpFile.exists()) {
                File fixedHeapdumpFile = constructHeapDumpFile(Utils.getTimeInMillis(time));
                heapdumpFile.renameTo(fixedHeapdumpFile);
                ProfilerControlPanel2.getDefault().refreshSnapshotsList();

                return fixedHeapdumpFile.toURI().toURL().toExternalForm();
            }
        } catch (IOException ex) {
            ErrorManager.getDefault().notify(ErrorManager.ERROR, ex);

            return NO_DATA_AVAILABLE_MSG;
        }

        return NO_DATA_AVAILABLE_MSG;
    }

    private static LoadedSnapshot takeSnapshot() throws CPUResultsSnapshot.NoDataAvailableException {
        return ResultsManager.getDefault().prepareSnapshot();
    }

    private String takeSnapshotHit() {
        LoadedSnapshot loadedSnapshot = null;
        String snapshotFilename = null;

        try {
            loadedSnapshot = takeSnapshot();
        } catch (CPUResultsSnapshot.NoDataAvailableException e) {
            //ErrorManager.getDefault().notify(ErrorManager.ERROR, e);
            // NOTE: this is actually a supported state - taking snapshots when no data are available, resultString remains null
        }

        if (loadedSnapshot != null) {
            try {
                FileObject snapshotDirectory = getSnapshotDirectory();
                FileObject profFile = snapshotDirectory.createData(ResultsManager.getDefault()
                                                                                 .getDefaultSnapshotFileName(loadedSnapshot),
                                                                   ResultsManager.SNAPSHOT_EXTENSION);
                ResultsManager.getDefault().saveSnapshot(loadedSnapshot, profFile);
                snapshotFilename = FileUtil.toFile(profFile).toURI().toURL().toExternalForm();
            } catch (IOException e) {
                ErrorManager.getDefault().notify(ErrorManager.ERROR, e);
            }
        }

        return (snapshotFilename == null) ? NO_DATA_AVAILABLE_MSG : snapshotFilename;
    }
}
