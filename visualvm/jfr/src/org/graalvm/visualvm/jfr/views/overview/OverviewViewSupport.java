/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.jfr.views.overview;

import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.core.ui.components.ScrollableContainer;
import java.awt.BorderLayout;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.core.datasource.DataSource;
import org.graalvm.visualvm.core.snapshot.SnapshotsSupport;
import org.graalvm.visualvm.core.ui.DataSourceWindowManager;
import org.graalvm.visualvm.jfr.JFRSnapshot;
import org.graalvm.visualvm.jfr.model.JFREvent;
import org.graalvm.visualvm.jfr.model.JFREventVisitor;
import org.graalvm.visualvm.jfr.model.JFRModelFactory;
import org.graalvm.visualvm.jfr.model.JFRPropertyNotAvailableException;
import org.graalvm.visualvm.jfr.utils.ValuesConverter;
import org.graalvm.visualvm.lib.ui.components.HTMLTextArea;
import org.graalvm.visualvm.lib.ui.components.HTMLTextAreaSearchUtils;
import org.graalvm.visualvm.threaddump.ThreadDump;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 * 
 * @author Jiri Sedlacek
 */
final class OverviewViewSupport {

 // --- Snapshots -----------------------------------------------------------
    
    static class SnapshotsViewSupport extends JPanel implements JFREventVisitor {
        
        private static final Logger LOGGER = Logger.getLogger(SnapshotsViewSupport.class.getName());
        
        private static final String LINK_TOGGLE_CATEGORY = "file:/toggle_category/"; // NOI18N
        private static final String LINK_OPEN_SNAPSHOT = "file:/open_snapshot/"; // NOI18N
        
        private static final String CATEGORY_THREAD_DUMPS = "thread_dumps"; // NOI18N
        
        
        private final JFRSnapshot snapshot;
        
        private final Map<String, Boolean> expansionMap = new HashMap<>();
        
        private List<Long> tdumpsTimestamps;
        
        private HTMLTextArea area;
        
        
        SnapshotsViewSupport(JFRSnapshot snapshot) {
            this.snapshot = snapshot;
            
            initComponents();
        }        
        
        
        public DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView(NbBundle.getMessage(OverviewViewSupport.class, "LBL_Saved_data"), null, 10, this, null);   // NOI18N
        }
        
        
        @Override
        public void init() {
            tdumpsTimestamps = new ArrayList<>();
        }

        @Override
        public boolean visit(String typeName, JFREvent event) {
            if ("jdk.ThreadDump".equals(typeName)) { // NOI18N
                try {
                    tdumpsTimestamps.add(ValuesConverter.instantToNanos(event.getInstant("eventTime"))); // NOI18N
                } catch (JFRPropertyNotAvailableException e) {}
            }
            return false;
        }

        @Override
        public void done() {
            Collections.sort(tdumpsTimestamps);
            
            SwingUtilities.invokeLater(new Runnable() {
                public void run() { area.setText(getSavedData()); }
            });
        }
        
        
        private void initComponents() {
            setLayout(new BorderLayout());
            setOpaque(false);
            
            area = new HTMLTextArea(getSavedData()) {
                protected void showURL(URL url) {
                    String link = url.toString();
                    if (link.startsWith(LINK_TOGGLE_CATEGORY)) {
                        link = link.substring(LINK_TOGGLE_CATEGORY.length());
                        toggleExpanded(link); 
                        setText(getSavedData());
                    } else if (link.startsWith(LINK_OPEN_SNAPSHOT)) {
                        link = link.substring(LINK_OPEN_SNAPSHOT.length());
                        openThreadDump(Long.parseLong(link));
                    }
                }
            };
            area.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            
            add(new ScrollableContainer(area), BorderLayout.CENTER);
        }
        
        private String getSavedData() {
            StringBuilder data = new StringBuilder();
            
            int tdumpsCount = tdumpsTimestamps == null ? -1 : tdumpsTimestamps.size();
            if (tdumpsCount < 0) {
                data.append("<b>Progress:</b> reading data...");
            } else {
                data.append("<b>Thread Dumps:</b> ");
                
                if (tdumpsCount == 0) {
                    data.append(tdumpsCount);
                } else {
                    data.append("<a href='" + (LINK_TOGGLE_CATEGORY + CATEGORY_THREAD_DUMPS) + "'>" + tdumpsCount + "</a><br>");
                    
                    if (isExpanded(CATEGORY_THREAD_DUMPS)) {
                        for (long timestamp : tdumpsTimestamps) {
                            data.append("&nbsp;&nbsp;&nbsp;<a href='" + LINK_OPEN_SNAPSHOT + timestamp + "'>" + "[threaddump] " + SnapshotsSupport.getInstance().getTimeStamp(ValuesConverter.nanosToMillis(timestamp)) + "</a><br>"); // NOI18N
                        }
                        data.append("<br>"); // NOI18N
                    }
                }
            }
            
            return "<nobr>" + data.toString() + "</nobr>";   // NOI18N
        }
        
        private boolean isExpanded(String categoryName) {
            Boolean expanded = expansionMap.get(categoryName);
            return expanded == null ? false : expanded.booleanValue();
        }
        
        private void toggleExpanded(String categoryName) {
            expansionMap.put(categoryName, !isExpanded(categoryName));
        }
        
        
        private void openThreadDump(long timestamp) {
            final String name = "threaddump-" + Long.toString(ValuesConverter.nanosToMillis(timestamp)); // NOI18N
            DataSource.EVENT_QUEUE.post(new Runnable() {
                public void run() {
                    for (ThreadDump tdump : snapshot.getRepository().getDataSources(ThreadDump.class)) {
                        File tdumpF = tdump.getFile();
                        if (tdumpF != null && tdumpF.getName().startsWith(name)) {
                            DataSourceWindowManager.sharedInstance().openDataSource(tdump, true);
                            return;
                        }
                    }
                    openThreadDumpImpl(timestamp);
                }
            });
        }
        
        private void openThreadDumpImpl(long timestamp) {
            new RequestProcessor("JFR Thread Dump Loader").post(new Runnable() { // NOI18N
                public void run() {
                    JFRModelFactory.getJFRModelFor(snapshot).visitEvents(new JFREventVisitor() {
                        @Override
                        public boolean visit(String typeName, JFREvent event) {
                            try {
                                if ("jdk.ThreadDump".equals(typeName) && // NOI18N
                                    ValuesConverter.instantToNanos(event.getInstant("eventTime")) == timestamp) { // NOI18N
                                    ThreadDump tdump = createThreadDump(ValuesConverter.nanosToMillis(timestamp), event.getString("result")); // NOI18N
                                    if (tdump != null) DataSourceWindowManager.sharedInstance().openDataSource(tdump, true);
                                    return true;
                                }
                            } catch (JFRPropertyNotAvailableException e) {}
                            return false;
                        }
                    });
                }
            });
        }
        
        private ThreadDump createThreadDump(long timestamp, String result) {
            if (result == null) return null;
            
            String name = "threaddump-" + timestamp + ".tdump"; // NOI18N
            File file = new File(snapshot.getStorage().getDirectory(), name);
            
            // TODO: created directly in <userdir>\repository\jfrsnapshots!
            //       should be saved in a directory private to the snapshot
            
            try (PrintWriter out = new PrintWriter(file)) { out.println(result); file.deleteOnExit(); }
            catch (FileNotFoundException ex) { LOGGER.log(Level.SEVERE, "Error saving thread dump", ex); } // NOI18N
            
            ThreadDump tdump = new ThreadDump(file, snapshot) {
                { getStorage().setCustomProperty(PROPERTY_VIEW_CLOSABLE, Boolean.TRUE.toString()); }
            };
            snapshot.getRepository().addDataSource(tdump);
            
            return tdump;
        }
        
    }
    
    
    // --- JVM arguments -------------------------------------------------------
    
    static class JVMArgumentsViewSupport extends JPanel  {
        
        JVMArgumentsViewSupport(String jvmargs) {
            initComponents(jvmargs);
        }        
        
        public DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView(NbBundle.getMessage(OverviewViewSupport.class, "LBL_JVM_arguments"), null, 10, this, null);    // NOI18N
        }
        
        private void initComponents(String jvmargs) {
            setLayout(new BorderLayout());
            setOpaque(false);
            
            HTMLTextArea area = new HTMLTextArea("<nobr>" + formatJVMArgs(jvmargs) + "</nobr>");   // NOI18N
            area.setCaretPosition(0);
            area.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            
            add(new ScrollableContainer(area), BorderLayout.CENTER);
            add(HTMLTextAreaSearchUtils.createSearchPanel(area), BorderLayout.SOUTH);
        }
        
        private String formatJVMArgs(String jvmargs) {
            if (jvmargs == null || jvmargs.isEmpty()) return NbBundle.getMessage(OverviewViewSupport.class, "LBL_none"); // NOI18N
            
            String mangledString = " ".concat(jvmargs).replace(" -","\n");  // NOI18N
            StringTokenizer tok = new StringTokenizer(mangledString,"\n");  // NOI18N
            StringBuilder text = new StringBuilder(100);

            while(tok.hasMoreTokens()) {
                String arg = tok.nextToken().replace(" ","&nbsp;"); // NOI18N
                int equalsSign = arg.indexOf('=');

                text.append("<b>"); // NOI18N
                text.append("-");   // NOI18N
                if (equalsSign != -1) {
                text.append(arg.substring(0,equalsSign));
                text.append("</b>");    // NOI18N
                text.append(arg.substring(equalsSign));
                } else {
                text.append(arg);
                text.append("</b>");    // NOI18N
                }
                text.append("<br>");    // NOI18N
            }
            return text.toString();
        }
        
    }
    
    
    // --- System properties ---------------------------------------------------
    
    static class SystemPropertiesViewSupport extends JPanel  {
        
        SystemPropertiesViewSupport(Properties properties) {
            initComponents(properties);
        }        
        
        public DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView(NbBundle.getMessage(OverviewViewSupport.class, "LBL_System_properties"), null, 20, this, null);    // NOI18N
        }
        
        private void initComponents(Properties properties) {
            setLayout(new BorderLayout());
            setOpaque(false);
            
            HTMLTextArea area = area = new HTMLTextArea("<nobr>" + formatSystemProperties(properties) + "</nobr>");    // NOI18N
            area.setCaretPosition(0);
            area.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            
            add(new ScrollableContainer(area), BorderLayout.CENTER);
            add(HTMLTextAreaSearchUtils.createSearchPanel(area), BorderLayout.SOUTH);
        }
        
        private String formatSystemProperties(Properties properties) {
            if (properties == null || properties.isEmpty()) return NbBundle.getMessage(OverviewViewSupport.class, "LBL_Unknown"); // NOI18N
            
            StringBuilder text = new StringBuilder(200);
            List<Object> keys = new ArrayList<>();
            Enumeration<?> en = properties.propertyNames();
            Iterator<?> keyIt;

            while (en.hasMoreElements()) {
                keys.add(en.nextElement());
            }

            keys.sort(null);
            keyIt = keys.iterator();
            while (keyIt.hasNext()) {
                String key = (String) keyIt.next();
                String val = properties.getProperty(key);

                if ("line.separator".equals(key) && val != null) {  // NOI18N
                    val = val.replace("\n", "\\n"); // NOI18N
                    val = val.replace("\r", "\\r"); // NOI18N
                }

                text.append("<b>"); // NOI18N
                text.append(key);
                text.append("</b>=");   // NOI18N
                text.append(val);
                text.append("<br>");    // NOI18N
            }
            return text.toString();
        }
        
    }
}
