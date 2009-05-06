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
import org.netbeans.lib.profiler.client.RuntimeProfilingPoint.HitEvent;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import org.netbeans.modules.profiler.ppoints.ui.LoadGeneratorCustomizer;
import org.netbeans.modules.profiler.ppoints.ui.ValidityAwarePanel;
import org.netbeans.modules.profiler.spi.LoadGenPlugin;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import java.awt.BorderLayout;
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import org.netbeans.lib.profiler.ui.UIUtils;


/**
 *
 * @author Jaroslav Bachorik
 */
public class LoadGenProfilingPoint extends CodeProfilingPoint.Paired implements PropertyChangeListener {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private class Annotation extends CodeProfilingPoint.Annotation {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private boolean isStartAnnotation;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public Annotation(boolean isStartAnnotation) {
            this.isStartAnnotation = isStartAnnotation;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public String getAnnotationType() {
            return LoadGenProfilingPoint.this.isEnabled() ? ANNOTATION_ENABLED : ANNOTATION_DISABLED;
        }

        public String getShortDescription() {
            if (!usesEndLocation()) {
                return getName();
            }

            return isStartAnnotation ? MessageFormat.format(ANNOTATION_START_STRING, new Object[] { getName() })
                                     : MessageFormat.format(ANNOTATION_END_STRING, new Object[] { getName() });
        }
    }

    private static class Result {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private final int threadId;
        private final long timestamp;
        private boolean success = false;
        private long endTimestamp = -1;
        private long startTime;
        private long stopTime;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public Result(long timestamp, int threadId, boolean success) {
            this.timestamp = timestamp;
            this.threadId = threadId;
            this.success = success;
            this.startTime = System.currentTimeMillis();
            this.stopTime = 0L;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public long getDuration() {
            return (stopTime > startTime) ? (stopTime - startTime) : (-1L);
        }

        public void setEndTimestamp(long endTimestamp) {
            this.endTimestamp = endTimestamp;
        }

        public long getEndTimestamp() {
            return endTimestamp;
        }

        public void setStopTime() {
            this.stopTime = System.currentTimeMillis();
        }

        public boolean isSuccess() {
            return success;
        }

        public int getThreadID() {
            return threadId;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

    private class Report extends TopComponent {
        //~ Static fields/initializers -------------------------------------------------------------------------------------------

        private static final String START_LOCATION_URLMASK = "file:/1"; // NOI18N
        private static final String END_LOCATION_URLMASK = "file:/2"; // NOI18N

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
            headerAreaTextBuilder.append("&nbsp;&nbsp;&nbsp;&nbsp;"); // NOI18N
            headerAreaTextBuilder.append(getHeaderType());
            headerAreaTextBuilder.append("<br>"); // NOI18N
            headerAreaTextBuilder.append("&nbsp;&nbsp;&nbsp;&nbsp;"); // NOI18N
            headerAreaTextBuilder.append(getHeaderEnabled());
            headerAreaTextBuilder.append("<br>"); // NOI18N
            headerAreaTextBuilder.append("&nbsp;&nbsp;&nbsp;&nbsp;"); // NOI18N
            headerAreaTextBuilder.append(getHeaderProject());
            headerAreaTextBuilder.append("<br>"); // NOI18N
            headerAreaTextBuilder.append("&nbsp;&nbsp;&nbsp;&nbsp;"); // NOI18N
            headerAreaTextBuilder.append(getHeaderStartLocation());
            headerAreaTextBuilder.append("<br>"); // NOI18N

            if (LoadGenProfilingPoint.this.usesEndLocation()) {
                headerAreaTextBuilder.append("&nbsp;&nbsp;&nbsp;&nbsp;"); // NOI18N
                headerAreaTextBuilder.append(getHeaderEndLocation());
                headerAreaTextBuilder.append("<br>"); // NOI18N
            }

            headerAreaTextBuilder.append("&nbsp;&nbsp;&nbsp;&nbsp;"); // NOI18N
            headerAreaTextBuilder.append(getHeaderHitsCount());

            headerArea.setText(headerAreaTextBuilder.toString());

            StringBuilder dataAreaTextBuilder = new StringBuilder();

            if (results.size() == 0) {
                dataAreaTextBuilder.append("&nbsp;&nbsp;&lt;" + NO_HITS_STRING + "&gt;"); // NOI18N
            } else {
                if (results.size() > 1) {
                    Collections.sort(results,
                                     new Comparator<LoadGenProfilingPoint.Result>() {
                            public int compare(LoadGenProfilingPoint.Result o1, LoadGenProfilingPoint.Result o2) {
                                return Long.valueOf(o1.getTimestamp()).compareTo(Long.valueOf(o2.getTimestamp()));
                            }
                        });
                }

                for (int i = 0; i < results.size(); i++) {
                    dataAreaTextBuilder.append("&nbsp;&nbsp;"); // NOI18N
                    dataAreaTextBuilder.append(getDataResultItem(i));
                    dataAreaTextBuilder.append("<br>"); // NOI18N
                }
            }

            dataArea.setText(dataAreaTextBuilder.toString());
        }

        void refreshProperties() {
            setName(LoadGenProfilingPoint.this.getName());
            setIcon(((ImageIcon) LoadGenProfilingPoint.this.getFactory().getIcon()).getImage());
            getAccessibleContext().setAccessibleDescription(MessageFormat.format(REPORT_ACCESS_DESCR, new Object[] { getName() }));
        }

        private String getDataResultItem(int index) {
            Result result = results.get(index);

            // TODO: enable once thread name by id is available
            //String threadName = Utils.getThreadName(result.getThreadID());
            //String threadClassName = Utils.getThreadClassName(result.getThreadID());
            //String threadInformation = (threadName == null ? "&lt;unknown thread&gt;" : (threadClassName == null ? threadName : threadName + " (" + threadClassName + ")"));
            String hitTime = Utils.formatProfilingPointTimeHiRes(result.getTimestamp());

            //      if (!LoadGenProfilingPoint.this.usesEndLocation()) {
            //return "<b>" + (index + 1) + ".</b> hit at <b>" + hitTime + "</b> by " + threadInformation;
            if (result.isSuccess()) {
                return MessageFormat.format(HIT_SUCCESS_STRING, new Object[] { (index + 1), hitTime });
            } else {
                return MessageFormat.format(HIT_FAILED_STRING, new Object[] { (index + 1), hitTime });
            }

            //      } else if (result.getEndTimestamp() == -1) {
            //        //return "<b>" + (index + 1) + ".</b> hit at <b>" + hitTime + "</b>, duration pending..., thread " + threadInformation;
            //        return "<b>" + (index + 1) + ".</b> hit at <b>" + hitTime + "</b>, duration <b>" + (result.getDuration() / 1000d) + "s</b>(pending...)";
            //      } else {
            //        //return "<b>" + (index + 1) + ".</b> hit at <b>" + hitTime + "</b>, duration <b>" + Utils.getDurationInMicroSec(result.getTimestamp(),result.getEndTimestamp()) + " &micro;s</b>, thread " + threadInformation;
            //        return "<b>" + (index + 1) + ".</b> hit at <b>" + hitTime + "</b>, duration <b>" + (result.getDuration() / 1000d) + " s</b>";
            //      }
        }

        private String getHeaderEnabled() {
            return MessageFormat.format(HEADER_ENABLED_STRING, new Object[] { LoadGenProfilingPoint.this.isEnabled() });
        }

        private String getHeaderEndLocation() {
            CodeProfilingPoint.Location location = LoadGenProfilingPoint.this.getEndLocation();
            File file = new File(location.getFile());
            String shortFileName = file.getName();
            int lineNumber = location.getLine();
            String locationUrl = "<a href='" + END_LOCATION_URLMASK + "'>"; // NOI18N

            return MessageFormat.format(HEADER_END_LOCATION_STRING, new Object[] { locationUrl + shortFileName, lineNumber })
                   + "</a>"; // NOI18N
        }

        private String getHeaderHitsCount() {
            return MessageFormat.format(HEADER_HITS_STRING, new Object[] { results.size() });
        }

        private String getHeaderName() {
            return "<h2><b>" + LoadGenProfilingPoint.this.getName() + "</b></h2>"; // NOI18N
        }

        private String getHeaderProject() {
            return MessageFormat.format(HEADER_PROJECT_STRING,
                                        new Object[] {
                                            ProjectUtils.getInformation(LoadGenProfilingPoint.this.getProject()).getDisplayName()
                                        });
        }

        private String getHeaderStartLocation() {
            CodeProfilingPoint.Location location = LoadGenProfilingPoint.this.getStartLocation();
            File file = new File(location.getFile());
            String shortFileName = file.getName();
            int lineNumber = location.getLine();
            String locationUrl = "<a href='" + START_LOCATION_URLMASK + "'>"; // NOI18N

            return LoadGenProfilingPoint.this.usesEndLocation()
                   ? (MessageFormat.format(HEADER_START_LOCATION_STRING, new Object[] { locationUrl + shortFileName, lineNumber })
                   + "</a>")
                   : (MessageFormat.format(HEADER_LOCATION_STRING, new Object[] { locationUrl + shortFileName, lineNumber })
                   + "</a>"); // NOI18N
        }

        private String getHeaderType() {
            return MessageFormat.format(HEADER_TYPE_STRING, new Object[] { LoadGenProfilingPoint.this.getFactory().getType() });
        }

        private void initComponents() {
            setLayout(new BorderLayout());

            JPanel contentsPanel = new JPanel(new BorderLayout());
            contentsPanel.setBackground(UIUtils.getProfilerResultsBackground());
            contentsPanel.setOpaque(true);
            contentsPanel.setBorder(BorderFactory.createMatteBorder(0, 15, 15, 15, UIUtils.getProfilerResultsBackground()));

            headerArea = new HTMLTextArea() {
                    protected void showURL(URL url) {
                        String urlString = url.toString();

                        if (START_LOCATION_URLMASK.equals(urlString)) {
                            Utils.openLocation(LoadGenProfilingPoint.this.getStartLocation());
                        } else if (LoadGenProfilingPoint.this.usesEndLocation()) {
                            Utils.openLocation(LoadGenProfilingPoint.this.getEndLocation());
                        }
                    }
                };

            JScrollPane headerAreaScrollPane = new JScrollPane(headerArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                               JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            headerAreaScrollPane.setBorder(BorderFactory.createMatteBorder(0, 0, 15, 0, UIUtils.getProfilerResultsBackground()));
            headerAreaScrollPane.setViewportBorder(BorderFactory.createEmptyBorder());
            contentsPanel.add(headerAreaScrollPane, BorderLayout.NORTH);

            dataArea = new HTMLTextArea();

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

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String ONE_HIT_STRING = NbBundle.getMessage(LoadGenProfilingPoint.class,
                                                                     "LoadGenProfilingPoint_OneHitString"); // NOI18N
    private static final String N_HITS_STRING = NbBundle.getMessage(LoadGenProfilingPoint.class,
                                                                    "LoadGenProfilingPoint_NHitsString"); // NOI18N
    private static final String NO_RESULTS_STRING = NbBundle.getMessage(LoadGenProfilingPoint.class,
                                                                        "LoadGenProfilingPoint_NoResultsString"); // NOI18N
    private static final String ANNOTATION_START_STRING = NbBundle.getMessage(LoadGenProfilingPoint.class,
                                                                              "LoadGenProfilingPoint_AnnotationStartString"); // NOI18N
    private static final String ANNOTATION_END_STRING = NbBundle.getMessage(LoadGenProfilingPoint.class,
                                                                            "LoadGenProfilingPoint_AnnotationEndString"); // NOI18N
    private static final String REPORT_ACCESS_DESCR = NbBundle.getMessage(LoadGenProfilingPoint.class,
                                                                          "LoadGenProfilingPoint_ReportAccessDescr"); // NOI18N
    private static final String NO_HITS_STRING = NbBundle.getMessage(LoadGenProfilingPoint.class,
                                                                     "LoadGenProfilingPoint_NoHitsString"); // NOI18N
    private static final String HEADER_TYPE_STRING = NbBundle.getMessage(LoadGenProfilingPoint.class,
                                                                         "LoadGenProfilingPoint_HeaderTypeString"); // NOI18N
    private static final String HEADER_ENABLED_STRING = NbBundle.getMessage(LoadGenProfilingPoint.class,
                                                                            "LoadGenProfilingPoint_HeaderEnabledString"); // NOI18N
    private static final String HEADER_PROJECT_STRING = NbBundle.getMessage(LoadGenProfilingPoint.class,
                                                                            "LoadGenProfilingPoint_HeaderProjectString"); // NOI18N
    private static final String HEADER_LOCATION_STRING = NbBundle.getMessage(LoadGenProfilingPoint.class,
                                                                             "LoadGenProfilingPoint_HeaderLocationString"); // NOI18N
    private static final String HEADER_START_LOCATION_STRING = NbBundle.getMessage(LoadGenProfilingPoint.class,
                                                                                   "LoadGenProfilingPoint_HeaderStartLocationString"); // NOI18N
    private static final String HEADER_END_LOCATION_STRING = NbBundle.getMessage(LoadGenProfilingPoint.class,
                                                                                 "LoadGenProfilingPoint_HeaderEndLocationString"); // NOI18N
    private static final String HEADER_HITS_STRING = NbBundle.getMessage(LoadGenProfilingPoint.class,
                                                                         "LoadGenProfilingPoint_HeaderHitsString"); // NOI18N
    private static final String HIT_SUCCESS_STRING = NbBundle.getMessage(LoadGenProfilingPoint.class,
                                                                         "LoadGenProfilingPoint_HitSuccessString"); // NOI18N
    private static final String HIT_FAILED_STRING = NbBundle.getMessage(LoadGenProfilingPoint.class,
                                                                        "LoadGenProfilingPoint_HitFailedString"); // NOI18N
    private static final String DATA_STRING = NbBundle.getMessage(LoadGenProfilingPoint.class, "LoadGenProfilingPoint_DataString"); // NOI18N
                                                                                                                                    // -----
    private static final Logger LOGGER = Logger.getLogger(LoadGenProfilingPoint.class.getName());
    public static final String PROPERTY_SCRIPTNAME = "p_ScriptName"; // NOI18N
    private static final String ANNOTATION_ENABLED = "loadgenProfilingPoint"; // NOI18N
    private static final String ANNOTATION_DISABLED = "loadgenProfilingPointD"; // NOI18N

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private Annotation endAnnotation;
    private Annotation startAnnotation;
    private final List<Result> results = new ArrayList<LoadGenProfilingPoint.Result>();
    private String scriptFileName;
    private WeakReference<Report> reportReference;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of LoadGenProfilingPoint */
    public LoadGenProfilingPoint(String name, Location startLocation, Location endLocation, Project project, ProfilingPointFactory factory) {
        super(name, startLocation, endLocation, project, factory);
        getChangeSupport().addPropertyChangeListener(this);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    @Override
    public void setEnabled(boolean value) {
        LoadGenPlugin lg = Lookup.getDefault().lookup(LoadGenPlugin.class);

        if (lg != null) {
            super.setEnabled(value);
        } else {
            LOGGER.warning("Can not enable the Load Generator profiling point. The appropriate modules are not installed."); // NOI18N
        }
    }

    @Override
    public boolean isEnabled() {
        boolean retValue;

        retValue = super.isEnabled();

        if (retValue) {
            LoadGenPlugin lg = Lookup.getDefault().lookup(LoadGenPlugin.class);
            retValue &= (lg != null);
        }

        return retValue;
    }

    public String getScriptFileName() {
        return (scriptFileName != null) ? scriptFileName : ""; // NOI18N
    }

    public void setSriptFileName(String fileName) {
        scriptFileName = fileName;
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

    protected CodeProfilingPoint.Annotation getEndAnnotation() {
        if (!usesEndLocation()) {
            endAnnotation = null;
        } else if (endAnnotation == null) {
            endAnnotation = new Annotation(false);
        }

        return endAnnotation;
    }

    protected String getResultsText() {
        if (hasResults()) {
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

    protected CodeProfilingPoint.Annotation getStartAnnotation() {
        if (startAnnotation == null) {
            startAnnotation = new Annotation(true);
        }

        return startAnnotation;
    }

    protected void updateCustomizer(ValidityAwarePanel c) {
        LoadGeneratorCustomizer customizer = (LoadGeneratorCustomizer) c;
        customizer.setPPName(getName());
        customizer.setPPStartLocation(getStartLocation());
        customizer.setPPEndLocation(getEndLocation());
        customizer.setScriptFile(getScriptFileName());
        customizer.setProject(getProject());
    }

    protected boolean usesEndLocation() {
        return getEndLocation() != null;
    }

    void setValues(ValidityAwarePanel c) {
        LoadGeneratorCustomizer customizer = (LoadGeneratorCustomizer) c;
        setName(customizer.getPPName());
        setStartLocation(customizer.getPPStartLocation());
        setEndLocation(customizer.getPPEndLocation());
        setSriptFileName(customizer.getScriptFile());
        
        Utils.checkLocation(this);
    }

    void hit(final HitEvent hitEvent, int index) {
        LoadGenPlugin lg = Lookup.getDefault().lookup(LoadGenPlugin.class);

        if (usesEndLocation() && (index == 1)) {
            if (lg != null) {
                lg.stop(getScriptFileName());

                for (Result result : results) {
                    if ((result.getEndTimestamp() == -1) && (result.getThreadID() == hitEvent.getThreadId())) {
                        result.setEndTimestamp(hitEvent.getTimestamp());

                        break;
                    }
                }
            }
        } else {
            if (lg != null) {
                lg.start(getScriptFileName(),
                         new LoadGenPlugin.Callback() {
                        private long correlationId = hitEvent.getTimestamp();

                        public void afterStart(LoadGenPlugin.Result result) {
                            Result rslt = new Result(hitEvent.getTimestamp(), hitEvent.getThreadId(),
                                                     result == LoadGenPlugin.Result.SUCCESS);
                            results.add(rslt);
                            correlationId = hitEvent.getTimestamp();
                            getChangeSupport().firePropertyChange(PROPERTY_RESULTS, false, true);
                        }

                        public void afterStop(LoadGenPlugin.Result result) {
                            for (Result rslt : results) {
                                if (rslt.getTimestamp() == correlationId) {
                                    rslt.setEndTimestamp(correlationId);
                                    rslt.setStopTime();

                                    break;
                                }
                            }

                            getChangeSupport().firePropertyChange(PROPERTY_RESULTS, false, true);
                        }
                    });
            }
        }
    }

    void reset() {
        boolean change = results.size() > 0;
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
        reportReference = new WeakReference<Report>(report);

        return report;
    }

    private boolean hasReport() {
        return (reportReference != null) && (reportReference.get() != null);
    }
}
