/*
 * Copyright (c) 2007, 2022, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.coredump.impl;

import org.graalvm.visualvm.core.datasource.DataSource;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import org.graalvm.visualvm.core.snapshot.Snapshot;
import org.graalvm.visualvm.core.datasupport.DataChangeEvent;
import org.graalvm.visualvm.core.datasupport.DataChangeListener;
import org.graalvm.visualvm.core.datasupport.Positionable;
import org.graalvm.visualvm.core.snapshot.RegisteredSnapshotCategories;
import org.graalvm.visualvm.core.snapshot.SnapshotCategory;
import org.graalvm.visualvm.core.ui.DataSourceWindowManager;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.core.ui.components.NotSupportedDisplayer;
import org.graalvm.visualvm.core.ui.components.ScrollableContainer;
import org.graalvm.visualvm.uisupport.HTMLTextArea;
import java.awt.BorderLayout;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.openide.util.NbBundle;

/**
 * A public entrypoint to the Overview subtab.
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
public final class OverviewViewSupport {

 // --- Snapshots -----------------------------------------------------------
    
    static class SnapshotsViewSupport extends JPanel  {
        
        private static final String LINK_TOGGLE_CATEGORY = "file:/toggle_category"; // NOI18N
        private static final String LINK_OPEN_SNAPSHOT = "file:/open_snapshot"; // NOI18N
        
        private final Map<Integer, Snapshot> snapshotsMap = new HashMap<>();
        private final Map<String, Boolean> expansionMap = new HashMap<>();
        
        SnapshotsViewSupport(DataSource ds) {
            initComponents(ds);
        }        
        
        public DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView(NbBundle.getMessage(OverviewViewSupport.class, "LBL_Saved_data"), null, 10, this, null);   // NOI18N
        }
        
        private void initComponents(final DataSource ds) {
            setLayout(new BorderLayout());
            setOpaque(false);
            
            final HTMLTextArea area = new HTMLTextArea() {
                protected void showURL(URL url) {
                    String link = url.toString();
                    if (link.startsWith(LINK_TOGGLE_CATEGORY)) {
                        link = link.substring(LINK_TOGGLE_CATEGORY.length());
                        toggleExpanded(link); 
                        setText(getSavedData(ds));
                    } else if (link.startsWith(LINK_OPEN_SNAPSHOT)) {
                        link = link.substring(LINK_OPEN_SNAPSHOT.length());
                        Snapshot s = snapshotsMap.get(Integer.parseInt(link));
                        if (s != null) DataSourceWindowManager.sharedInstance().openDataSource(s);
                    }
                }
            };
            area.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            
            ds.getRepository().addDataChangeListener(new DataChangeListener<Snapshot>() {
                public void dataChanged(DataChangeEvent<Snapshot> event) {
                    area.setText(getSavedData(ds));
                }                
            }, Snapshot.class);
            
            add(new ScrollableContainer(area), BorderLayout.CENTER);
        }
        
        private String getSavedData(DataSource dataSource) {
            snapshotsMap.clear();
            StringBuilder data = new StringBuilder();
            
            List<SnapshotCategory<? extends Snapshot>> snapshotCategories = RegisteredSnapshotCategories.sharedInstance().getVisibleCategories();
            for (SnapshotCategory<? extends Snapshot> category : snapshotCategories) {
                Set<? extends Snapshot> snapshots = dataSource.getRepository().getDataSources(category.getType());
                if (snapshots.isEmpty()) {
                    data.append("<b>" + category.getName() + ":</b> " + snapshots.size() + "<br>"); // NOI18N
                } else {
                    String categoryName = category.getName();
                    data.append("<b>" + categoryName + ":</b> <a href='" + (LINK_TOGGLE_CATEGORY + categoryName) + "'>" + snapshots.size() + "</a><br>"); // NOI18N
                    
                    if (isExpanded(categoryName)) {
                        List<DataSourceDescriptor<? extends Snapshot>> descriptors = new ArrayList<>();
                        Map<DataSourceDescriptor<? extends Snapshot>, Snapshot> dataSources = new HashMap<>();

                        for (Snapshot s : snapshots) {
                            DataSourceDescriptor<? extends Snapshot> dsd = DataSourceDescriptorFactory.getDescriptor(s);
                            descriptors.add(dsd);
                            dataSources.put(dsd, s);
                        }
                        descriptors.sort(Positionable.STRONG_COMPARATOR);

                        int size = snapshotsMap.size();
                        for (int i = 0; i < descriptors.size(); i++) {
                            DataSourceDescriptor<? extends Snapshot> dsd = descriptors.get(i);
                            Snapshot s = dataSources.get(dsd);
                            snapshotsMap.put(i + size, s);
                            data.append("&nbsp;&nbsp;&nbsp;<a href='" + LINK_OPEN_SNAPSHOT + (i + size) + "'>" + dsd.getName() + "</a><br>"); // NOI18N
                        }
                        data.append("<br>"); // NOI18N
                    }
                }
            }            
            
            return "<nobr>" + data.toString() + "</nobr>";   // NOI18N
        }
        
        private boolean isExpanded(String categoryName) {
            Boolean expanded = expansionMap.computeIfAbsent(categoryName, k -> false);
            return expanded.booleanValue();
        }
        
        private void toggleExpanded(String categoryName) {
            expansionMap.put(categoryName, !isExpanded(categoryName));
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
            
            JComponent contents;
            
            if (jvmargs != null) {
                HTMLTextArea area = new HTMLTextArea("<nobr>" + formatJVMArgs(jvmargs) + "</nobr>");    // NOI18N
                area.setCaretPosition(0);
                area.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                contents = area;
            } else {
                contents = new NotSupportedDisplayer(NotSupportedDisplayer.JVM);
            }
            
            add(new ScrollableContainer(contents), BorderLayout.CENTER);
        }
        
        private String formatJVMArgs(String jvmargs) {
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
            
            JComponent contents;
            
            if (properties != null) {
                HTMLTextArea area = new HTMLTextArea("<nobr>" + formatSystemProperties(properties) + "</nobr>");    // NOI18N
                area.setCaretPosition(0);
                area.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                contents = area;
            } else {
                contents = new NotSupportedDisplayer(NotSupportedDisplayer.JVM);
            }
            
            add(new ScrollableContainer(contents), BorderLayout.CENTER);
        }
        
        private String formatSystemProperties(Properties properties) {
            StringBuilder text = new StringBuilder(200);
            List<Object> keys = new ArrayList<>();
            Enumeration<?> en = properties.propertyNames();
            Iterator<Object> keyIt;

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
