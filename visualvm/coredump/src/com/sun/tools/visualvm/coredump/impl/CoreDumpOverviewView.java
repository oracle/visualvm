/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tools.visualvm.coredump.impl;

import com.sun.tools.visualvm.coredump.CoreDump;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import com.sun.tools.visualvm.tools.sa.SaModel;
import com.sun.tools.visualvm.tools.sa.SaModelFactory;
import java.awt.BorderLayout;
import java.util.Properties;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;

/**
 *
 * @author Tomas Hurka
 */
class CoreDumpOverviewView extends DataSourceView {
    
    private static final String IMAGE_PATH = "com/sun/tools/visualvm/coredump/resources/overview.png";  // NOI18N
    
    
    public CoreDumpOverviewView(CoreDump coreDump) {
        super(coreDump, NbBundle.getMessage(CoreDumpOverviewView.class, "LBL_Overview"), new ImageIcon(Utilities.loadImage(IMAGE_PATH, true)).getImage(), 0, false);    // NOI18N
    }
    
    protected DataViewComponent createComponent() {
        CoreDump coreDump = (CoreDump)getDataSource();
        DataViewComponent dvc = new DataViewComponent(
                new MasterViewSupport(coreDump).getMasterView(),
                new DataViewComponent.MasterViewConfiguration(false));
        
        SaModel saAgent = SaModelFactory.getSAAgentFor(coreDump);
        Properties jvmProperties = saAgent.getSystemProperties();
        String jvmargs = saAgent.getJvmArgs();
        
        dvc.configureDetailsView(new DataViewComponent.DetailsViewConfiguration(0.25, 0, -1, -1, -1, -1));
        
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(NbBundle.getMessage(CoreDumpOverviewView.class, "LBL_Saved_data"), true), DataViewComponent.TOP_LEFT);  // NOI18N
        dvc.addDetailsView(new OverviewViewSupport.SnapshotsViewSupport(coreDump).getDetailsView(), DataViewComponent.TOP_LEFT);
        
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(NbBundle.getMessage(CoreDumpOverviewView.class, "LBL_Details"), true), DataViewComponent.TOP_RIGHT);    // NOI18N 
        dvc.addDetailsView(new OverviewViewSupport.JVMArgumentsViewSupport(jvmargs).getDetailsView(), DataViewComponent.TOP_RIGHT);
        dvc.addDetailsView(new OverviewViewSupport.SystemPropertiesViewSupport(jvmProperties).getDetailsView(), DataViewComponent.TOP_RIGHT);
        
        return dvc;
    }
    
    
    // --- General data --------------------------------------------------------
    
    private static class MasterViewSupport extends JPanel  {
        
        public MasterViewSupport(CoreDump coreDump) {
            initComponents(coreDump);
        }
        
        
        public DataViewComponent.MasterView getMasterView() {
            return new DataViewComponent.MasterView(NbBundle.getMessage(CoreDumpOverviewView.class, "LBL_Overview"), null, this);   // NOI18N
        }
        
        
        private void initComponents(CoreDump coreDump) {
            setLayout(new BorderLayout());
            setOpaque(false);
            
            HTMLTextArea area = new HTMLTextArea("<nobr>" + getGeneralProperties(coreDump) + "</nobr>");    // NOI18N
            area.setBorder(BorderFactory.createEmptyBorder(14, 8, 14, 8));
            
            // TODO: implement listener for CoreDump.oomeHeapDumpEnabled
            
            add(area, BorderLayout.CENTER);
        }
        
        private String getGeneralProperties(CoreDump coreDump) {
            SaModel saAgent = SaModelFactory.getSAAgentFor(coreDump);
            StringBuilder data = new StringBuilder();
            
            // CoreDump information
            String commandLine = saAgent.getJavaCommand();
            
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
                String mainClassLbl = NbBundle.getMessage(CoreDumpOverviewView.class, "LBL_Main_class");    // NOI18N
                String argsLbl = NbBundle.getMessage(CoreDumpOverviewView.class, "LBL_Arguments");  // NOI18N
                data.append("<b>"+mainClassLbl+":</b> " + mainClass + "<br>");  // NOI18N
                data.append("<b>"+argsLbl+":</b> " + (mainArgs == null ? NbBundle.getMessage(CoreDumpOverviewView.class, "LBL_none") : mainArgs) + "<br>"); // NOI18N
            }
            
            // JVM information
            String jvmFlags = saAgent.getJvmFlags();
            String jvmLbl = NbBundle.getMessage(CoreDumpOverviewView.class, "LBL_JVM"); // NOI18N
            String jhLbl = NbBundle.getMessage(CoreDumpOverviewView.class, "LBL_Java_Home");    // NOI18N
            String flagsLbl = NbBundle.getMessage(CoreDumpOverviewView.class, "LBL_JVM_Flags"); // NOI18N
            data.append("<br>");    // NOI18N
            data.append("<b>"+jvmLbl+":</b> " + saAgent.getVmName() + " (" + saAgent.getVmVersion() + ", " + saAgent.getVmInfo() + ")<br>");    // NOI18N
            data.append("<b>"+jhLbl+":</b> " + saAgent.getJavaHome() + "<br>"); // NOI18N
            data.append("<b>"+flagsLbl+":</b> " + (jvmFlags == null || jvmFlags.length() == 0 ? NbBundle.getMessage(CoreDumpOverviewView.class, "LBL_none") : jvmFlags) + "<br><br>");  // NOI18N
            
            return data.toString();
            
        }
        
    }
    
}
