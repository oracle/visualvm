/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import org.graalvm.visualvm.jfr.JFRSnapshot;
import org.graalvm.visualvm.core.ui.DataSourceView;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import java.awt.BorderLayout;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.core.ui.components.ScrollableContainer;
import org.graalvm.visualvm.jfr.model.JFRModel;
import org.graalvm.visualvm.jfr.model.JFRModelFactory;
import org.graalvm.visualvm.lib.ui.components.HTMLTextArea;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
class JFRSnapshotOverviewView extends DataSourceView {
    
    private static final Logger LOGGER = Logger.getLogger(JFRSnapshotOverviewView.class.getName());
    
    private static final String IMAGE_PATH = "org/graalvm/visualvm/jfr/resources/overview.png";  // NOI18N
    
    private JFRModel model;
    
    
    public JFRSnapshotOverviewView(JFRSnapshot snapshot) {
        super(snapshot, NbBundle.getMessage(JFRSnapshotOverviewView.class, "LBL_Overview"), new ImageIcon(ImageUtilities.loadImage(IMAGE_PATH, true)).getImage(), 0, false);    // NOI18N
    }
    
    
    @Override
    protected void willBeAdded() {
        JFRSnapshot snapshot = (JFRSnapshot)getDataSource();
        model = JFRModelFactory.getJFRModelFor(snapshot);
        
        if (model == null) LOGGER.log(Level.SEVERE, "No JFR model for " + snapshot.getFile()); // NOI18N
    }
    
    protected void removed() {
        // also called for null model - OOME etc.
        JFRModelFactory.cleanupModel__Workaround(model);
    }
    
    @Override
    protected DataViewComponent createComponent() {
        if (model == null) {
            MasterViewSupport masterView = new MasterViewSupport(model) {
                @Override void firstShown() {}
            };
            return new DataViewComponent(masterView.getMasterView(), new DataViewComponent.MasterViewConfiguration(true));
        } else {
            final OverviewViewSupport.SnapshotsViewSupport snapshotView = new OverviewViewSupport.SnapshotsViewSupport((JFRSnapshot)getDataSource());

            MasterViewSupport masterView = new MasterViewSupport(model) {
                @Override
                void firstShown() {
                    initialize(snapshotView);
                }
            };
            DataViewComponent dvc = new DataViewComponent(
                    masterView.getMasterView(),
                    new DataViewComponent.MasterViewConfiguration(false));

            Properties jvmProperties = model.getSystemProperties();
            String jvmargs = model.getJvmArgs();

            dvc.configureDetailsView(new DataViewComponent.DetailsViewConfiguration(0.25, 0, -1, -1, -1, -1));

            dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(NbBundle.getMessage(JFRSnapshotOverviewView.class, "LBL_Saved_data"), true), DataViewComponent.TOP_LEFT);  // NOI18N
            dvc.addDetailsView(snapshotView.getDetailsView(), DataViewComponent.TOP_LEFT);

            dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(NbBundle.getMessage(JFRSnapshotOverviewView.class, "LBL_Details"), true), DataViewComponent.TOP_RIGHT);    // NOI18N 
            dvc.addDetailsView(new OverviewViewSupport.JVMArgumentsViewSupport(jvmargs).getDetailsView(), DataViewComponent.TOP_RIGHT);
            dvc.addDetailsView(new OverviewViewSupport.SystemPropertiesViewSupport(jvmProperties).getDetailsView(), DataViewComponent.TOP_RIGHT);

            return dvc;
        }
    }
    
    private void initialize(final OverviewViewSupport.SnapshotsViewSupport snapshotView) {
        new RequestProcessor("JFR Overview Initializer").post(new Runnable() { // NOI18N
            public void run() { model.visitEvents(snapshotView); }
        });
    }
    
    
    // --- General data --------------------------------------------------------
    
    private static abstract class MasterViewSupport extends JPanel  {
        
        public MasterViewSupport(JFRModel model) {
            initComponents(model);
        }
        
        
        abstract void firstShown();
        
        
        public DataViewComponent.MasterView getMasterView() {
            return new DataViewComponent.MasterView(NbBundle.getMessage(JFRSnapshotOverviewView.class, "LBL_Overview"), null, this);   // NOI18N
        }
        
        
        private void initComponents(JFRModel model) {
            setLayout(new BorderLayout());
            setOpaque(false);
            
            HTMLTextArea area = new HTMLTextArea("<nobr>" + getGeneralProperties(model) + "</nobr>");    // NOI18N
            area.setBorder(BorderFactory.createEmptyBorder(14, 8, 14, 8));
            
//            add(area, BorderLayout.CENTER);
            add(model == null ? new ScrollableContainer(area) : area, BorderLayout.CENTER);
            
            if (model != null) addHierarchyListener(new HierarchyListener() {
                public void hierarchyChanged(HierarchyEvent e) {
                    if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                        if (isShowing()) {
                            removeHierarchyListener(this);
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() { firstShown(); }
                            });
                        }
                    }
                }
            });
        }
        
        private String getGeneralProperties(JFRModel model) {
            if (model != null) {
                StringBuilder data = new StringBuilder();
                
                // JFR Snapshot information
                String commandLine = model.getJavaCommand();

                if (commandLine != null) {
                    // Application information
                    int firstSpace = commandLine.indexOf(' ');
                    String mainClass;
                    String mainArgs = null;
                    if (firstSpace == -1) {
                        mainClass = commandLine;
                    } else {
                        mainClass = commandLine.substring(0,firstSpace);
                        mainArgs = commandLine.substring(firstSpace+1);
                    }
                    String mainClassLbl = NbBundle.getMessage(JFRSnapshotOverviewView.class, "LBL_Main_class");    // NOI18N
                    String argsLbl = NbBundle.getMessage(JFRSnapshotOverviewView.class, "LBL_Arguments");  // NOI18N
                    data.append("<b>"+mainClassLbl+":</b> " + mainClass + "<br>");  // NOI18N
                    data.append("<b>"+argsLbl+":</b> " + (mainArgs == null ? NbBundle.getMessage(JFRSnapshotOverviewView.class, "LBL_none") : mainArgs) + "<br>"); // NOI18N
                    
                    data.append("<br>");    // NOI18N
                }

                // JVM information
                String jvmFlags = model.getJvmFlags();
                String jvmLbl = NbBundle.getMessage(JFRSnapshotOverviewView.class, "LBL_JVM"); // NOI18N
                String jLbl = NbBundle.getMessage(JFRSnapshotOverviewView.class, "LBL_Java"); // NOI18N
                String verLbl = NbBundle.getMessage(JFRSnapshotOverviewView.class, "LBL_Java_Version"); // NOI18N
                String vendorLbl = NbBundle.getMessage(JFRSnapshotOverviewView.class, "LBL_Java_Vendor"); // NOI18N
                String jhLbl = NbBundle.getMessage(JFRSnapshotOverviewView.class, "LBL_Java_Home");    // NOI18N
                String flagsLbl = NbBundle.getMessage(JFRSnapshotOverviewView.class, "LBL_JVM_Flags"); // NOI18N
                
                String vmnS = model.getVmName();
                String vmvS = model.getVmVersion();
                String vmiS = model.getVmInfo();
                if (vmvS != null || vmiS != null) {
                    vmnS += " (";
                    if (vmvS != null) vmnS += vmvS;
                    if (vmvS != null && vmiS != null) vmnS += ", ";
                    if (vmiS != null) vmnS += vmiS;
                    vmnS += ")";
                }
                data.append("<b>"+jvmLbl+":</b> " + vmnS + "<br>");    // NOI18N
                
                Properties props = model.getSystemProperties();
                if (props != null) {
                    String javaVersion = props.getProperty("java.version"); // NOI18N
                    String javaVendor = props.getProperty("java.vendor"); // NOI18N
                    boolean spec = javaVersion == null && javaVendor == null;
                    if (spec) {
                        javaVersion = props.getProperty("java.vm.specification.version"); // NOI18N
                        javaVendor = props.getProperty("java.vm.vendor"); // NOI18N
                    }
                    if (javaVersion != null || javaVendor != null) {
                        data.append("<b>"+jLbl+":</b>");
                        if (javaVersion != null) {
                            if (!spec) data.append(" "+verLbl+" " + javaVersion);   // NOI18N
                            else data.append(" "+"spec. version"+" " + javaVersion);   // NOI18N
                        }
                        if (javaVendor != null) {
                            if (javaVersion != null) data.append(",");
                            if (!spec) data.append(" "+vendorLbl+" " + javaVendor);   // NOI18N
                            else data.append(" "+"VM vendor"+" " + javaVendor);   // NOI18N
                        }
                        data.append("<br>");
                    }
                }
                String javaHome = model.getJavaHome();
                data.append("<b>"+jhLbl+":</b> " + (javaHome == null || javaHome.length() == 0 ? NbBundle.getMessage(JFRSnapshotOverviewView.class, "LBL_none") : javaHome) + "<br>"); // NOI18N
                data.append("<b>"+flagsLbl+":</b> " + (jvmFlags == null || jvmFlags.length() == 0 ? NbBundle.getMessage(JFRSnapshotOverviewView.class, "LBL_none") : jvmFlags) + "<br><br>");  // NOI18N
                
                return data.toString();
            } else {
                JFRModelFactory f = JFRModelFactory.getDefault();
                if (!f.hasProviders()) return NbBundle.getMessage(JFRSnapshotOverviewView.class, "MSG_JFR_Failed_No_Loader"); // NOI18N
                else if (!f.hasGenericProvider()) return NbBundle.getMessage(JFRSnapshotOverviewView.class, "MSG_JFR_Failed_Install_Generic"); // NOI18N
                else return NbBundle.getMessage(JFRSnapshotOverviewView.class, "MSG_JFR_Failed_General"); // NOI18N
            }
            
        }
        
    }
    
}
