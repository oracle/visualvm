/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.application.views.overview;

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.application.jvm.Jvm;
import org.graalvm.visualvm.application.jvm.JvmFactory;
import org.graalvm.visualvm.application.snapshot.ApplicationSnapshot;
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
import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.lib.ui.components.HTMLTextArea;
import org.graalvm.visualvm.lib.ui.components.HTMLTextAreaSearchUtils;
import org.openide.util.NbBundle;
import org.openide.util.WeakListeners;

/**
 * A public entrypoint to the Overview subtab.
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
class OverviewViewSupport {

    // --- General data --------------------------------------------------------
    
    static class MasterViewSupport extends JPanel  {
        private PropertyChangeListener oomeListener;
        
        MasterViewSupport(ApplicationOverviewModel model) {
            initComponents(model);
        }
        
        
        public DataViewComponent.MasterView getMasterView() {
            return new DataViewComponent.MasterView(NbBundle.getMessage(OverviewViewSupport.class, "LBL_Overview"), null, this);    // NOI18N
        }
        
        
        private void initComponents(final ApplicationOverviewModel model) {
            setLayout(new BorderLayout());
            setOpaque(false);
            
            final HTMLTextArea area = new HTMLTextArea("<nobr>" + getGeneralProperties(model) + "</nobr>"); // NOI18N
            area.setBorder(BorderFactory.createEmptyBorder(14, 8, 14, 8));
            
            DataSource source = model.getSource();
            if (source instanceof Application) {
                oomeListener = new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent evt) {
                        if (Jvm.PROPERTY_DUMP_OOME_ENABLED.equals(evt.getPropertyName())) {
                            int selStart = area.getSelectionStart();
                            int selEnd   = area.getSelectionEnd();
                            area.setText("<nobr>" + getGeneralProperties(model) + "</nobr>");   // NOI18N
                            area.select(selStart, selEnd);
                        }
                    }
                };
                Jvm jvm = JvmFactory.getJVMFor((Application)source);
                jvm.addPropertyChangeListener(WeakListeners.propertyChange(oomeListener,jvm));
            }
            add(area, BorderLayout.CENTER);
        }
        
        private String getGeneralProperties(ApplicationOverviewModel model) {
            StringBuilder data = new StringBuilder();
            
            // Application information
            String PID = NbBundle.getMessage(OverviewViewSupport.class, "LBL_PID"); // NOI18N
            String HOST = NbBundle.getMessage(OverviewViewSupport.class, "LBL_Host");   // NOI18N
            data.append("<b>"+PID+":</b> " + model.getPid() + "<br>");  // NOI18N
            data.append("<b>"+HOST+":</b> " + model.getHostName() + "<br>");    // NOI18N
            
            if (model.basicInfoSupported()) {
                String MAIN_CLASS = NbBundle.getMessage(OverviewViewSupport.class, "LBL_Main_class");   // NOI18N
                String ARGS = NbBundle.getMessage(OverviewViewSupport.class, "LBL_Arguments");  // NOI18N
                String JVM = NbBundle.getMessage(OverviewViewSupport.class, "LBL_JVM"); // NOI18N
                String JAVA_HOME = NbBundle.getMessage(OverviewViewSupport.class, "LBL_Java_Home"); // NOI18N
                String JAVA = NbBundle.getMessage(OverviewViewSupport.class, "LBL_Java"); // NOI18N
                String JAVA_VERSION = NbBundle.getMessage(OverviewViewSupport.class, "LBL_Java_Version"); // NOI18N
                String JAVA_VENDOR = NbBundle.getMessage(OverviewViewSupport.class, "LBL_Java_Vendor"); // NOI18N
                String JVM_FLAGS = NbBundle.getMessage(OverviewViewSupport.class, "LBL_JVM_Flags"); // NOI18N
                String HEAP_DUMP_OOME = NbBundle.getMessage(OverviewViewSupport.class, "LBL_Heap_dump_on_OOME");    // NOI18N
                data.append("<b>"+MAIN_CLASS+":</b> " + model.getMainClass() + "<br>"); // NOI18N
                data.append("<b>"+ARGS+":</b> " + model.getMainArgs() + "<br>");    // NOI18N
                
                data.append("<br>");    // NOI18N
                data.append("<b>"+JVM+":</b> " + model.getVmId() + "<br>"); // NOI18N
                String javaVersion = model.getJavaVersion();
                String javaVendor = model.getJavaVendor();
                if (javaVersion != null || javaVendor != null) {
                    data.append("<b>"+JAVA+":</b>");
                    if (javaVersion != null) {
                        data.append(" "+JAVA_VERSION+" " + javaVersion);   // NOI18N
                    }
                    if (javaVendor != null) {
                        if (javaVersion != null) data.append(",");
                        data.append(" "+JAVA_VENDOR+" " + javaVendor);   // NOI18N
                    }
                    data.append("<br>");
                }
                data.append("<b>"+JAVA_HOME+":</b> " + model.getJavaHome() + "<br>");   // NOI18N
                data.append("<b>"+JVM_FLAGS+":</b> " + model.getJvmFlags() + "<br><br>");   // NOI18N
                data.append("<b>"+HEAP_DUMP_OOME+":</b> " + model.oomeEnabled() + "<br>");  // NOI18N
            }
            
            return data.toString();
            
        }
        
    }
    
    // --- Snapshots -----------------------------------------------------------
    
    static class SnapshotsViewSupport extends JPanel implements DataChangeListener<Snapshot> {
        
        private static final String LINK_TOGGLE_CATEGORY = "file:/toggle_category"; // NOI18N
        private static final String LINK_OPEN_SNAPSHOT = "file:/open_snapshot"; // NOI18N
        
        private DataSource dataSource;
        private HTMLTextArea area;
        
        private final Map<Integer, Snapshot> snapshotsMap = new HashMap();
        private final Map<String, Boolean> expansionMap = new HashMap();

        private boolean standaloneAppSnapshot;
        
        
        SnapshotsViewSupport(DataSource dataSource) {
            this.dataSource = dataSource;
            initComponents();
            dataSource.getRepository().addDataChangeListener(this, Snapshot.class);

            standaloneAppSnapshot = dataSource.getOwner() == null &&
                                    dataSource instanceof ApplicationSnapshot;
            if (standaloneAppSnapshot) {
                dataSource.setVisible(false);
                DataSource.ROOT.getRepository().addDataSource(dataSource);
            }
        }
        
        public DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView(NbBundle.getMessage(OverviewViewSupport.class, "LBL_Saved_data"), null, 10, this, null);   // NOI18N
        }
        
        private void initComponents() {
            setLayout(new BorderLayout());
            setOpaque(false);
            
            area = new HTMLTextArea() {
                protected void showURL(URL url) {
                    String link = url.toString();
                    if (link.startsWith(LINK_TOGGLE_CATEGORY)) {
                        link = link.substring(LINK_TOGGLE_CATEGORY.length());
                        toggleExpanded(link); 
                        updateSavedData();
                    } else if (link.startsWith(LINK_OPEN_SNAPSHOT)) {
                        link = link.substring(LINK_OPEN_SNAPSHOT.length());
                        Snapshot s = snapshotsMap.get(Integer.parseInt(link));
                        if (s != null) DataSourceWindowManager.sharedInstance().openDataSource(s);
                    }
                }
            };
            updateSavedData();
            area.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            
            add(new ScrollableContainer(area), BorderLayout.CENTER);
        }
        
        public void dataChanged(DataChangeEvent<Snapshot> event) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() { updateSavedData(); }
            });
        }
            
        void removed() {
            dataSource.getRepository().removeDataChangeListener(this);
            if (standaloneAppSnapshot)
                DataSource.ROOT.getRepository().removeDataSource(dataSource);
        }
        
        private void updateSavedData() {
            snapshotsMap.clear();
            StringBuilder data = new StringBuilder();
            
            List<SnapshotCategory> snapshotCategories = RegisteredSnapshotCategories.sharedInstance().getVisibleCategories();
            for (SnapshotCategory category : snapshotCategories) {
                Set<Snapshot> snapshots = dataSource.getRepository().getDataSources(category.getType());
                if (snapshots.isEmpty()) {
                    data.append("<b>" + category.getName() + ":</b> " + snapshots.size() + "<br>"); // NOI18N
                } else {
                    String categoryName = category.getName();
                    data.append("<b>" + categoryName + ":</b> <a href='" + (LINK_TOGGLE_CATEGORY + categoryName) + "'>" + snapshots.size() + "</a><br>"); // NOI18N
                    
                    if (isExpanded(categoryName)) {
                        List<DataSourceDescriptor> descriptors = new ArrayList();
                        Map<DataSourceDescriptor, Snapshot> dataSources = new HashMap();

                        for (Snapshot s : snapshots) {
                            DataSourceDescriptor dsd = DataSourceDescriptorFactory.getDescriptor(s);
                            descriptors.add(dsd);
                            dataSources.put(dsd, s);
                        }
                        Collections.sort(descriptors, Positionable.STRONG_COMPARATOR);

                        int size = snapshotsMap.size();
                        for (int i = 0; i < descriptors.size(); i++) {
                            DataSourceDescriptor dsd = descriptors.get(i);
                            Snapshot s = dataSources.get(dsd);
                            snapshotsMap.put(i + size, s);
                            data.append("&nbsp;&nbsp;&nbsp;<a href='" + LINK_OPEN_SNAPSHOT + (i + size) + "'>" + dsd.getName() + "</a><br>"); // NOI18N
                        }
                        data.append("<br>"); // NOI18N
                    }
                }
            }            
            
            area.setText("<nobr>" + data.toString() + "</nobr>");   // NOI18N
        }
        
        private boolean isExpanded(String categoryName) {
            Boolean expanded = expansionMap.get(categoryName);
            if (expanded == null) {
                expanded = standaloneAppSnapshot;
                expansionMap.put(categoryName, expanded);
            }
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
            HTMLTextArea area = null;
            
            if (jvmargs != null) {
                area = new HTMLTextArea("<nobr>" + jvmargs + "</nobr>");   // NOI18N
                area.setCaretPosition(0);
                area.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                contents = area;
            } else {
                contents = new NotSupportedDisplayer(NotSupportedDisplayer.JVM);
            }
            
            add(new ScrollableContainer(contents), BorderLayout.CENTER);
            if (area != null) add(HTMLTextAreaSearchUtils.createSearchPanel(area), BorderLayout.SOUTH);
        }
        
    }
        
    
    // --- System properties ---------------------------------------------------
    
    static class SystemPropertiesViewSupport extends JPanel  {
        
        SystemPropertiesViewSupport(String properties) {
            initComponents(properties);
        }
        
        public DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView(NbBundle.getMessage(OverviewViewSupport.class, "LBL_System_properties"), null, 20, this, null);    // NOI18N
        }
        
        private void initComponents(String properties) {
            setLayout(new BorderLayout());
            setOpaque(false);
            
            JComponent contents;
            HTMLTextArea area = null;
            
            if (properties != null) {
                area = new HTMLTextArea("<nobr>" + properties + "</nobr>");    // NOI18N
                area.setCaretPosition(0);
                area.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                contents = area;
            } else {
                contents = new NotSupportedDisplayer(NotSupportedDisplayer.JVM);
            }
            
            add(new ScrollableContainer(contents), BorderLayout.CENTER);
            if (area != null) add(HTMLTextAreaSearchUtils.createSearchPanel(area), BorderLayout.SOUTH);
        }
        
    }
}
