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

package com.sun.tools.visualvm.coredump.overview;

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
import org.openide.util.Utilities;

/**
 *
 * @author Tomas Hurka
 */
class CoreDumpOverviewView extends DataSourceView {
    
    private static final String IMAGE_PATH = "com/sun/tools/visualvm/coredump/resources/overview.png";
    
    private DataViewComponent view;
    
    
    public CoreDumpOverviewView(CoreDump coreDump) {
        super(coreDump, "Overview", new ImageIcon(Utilities.loadImage(IMAGE_PATH, true)).getImage(), 0, false);
        view = createViewComponent(coreDump);
        OverviewViewSupport.getInstance().getCoreDumpOverviewPluggableView().makeCustomizations(view, coreDump);
    }
    
    public DataViewComponent getView() {
        return view;
    }
    
    
    private DataViewComponent createViewComponent(CoreDump coreDump) {
        DataViewComponent dvc = new DataViewComponent(
                new MasterViewSupport(coreDump).getMasterView(),
                new DataViewComponent.MasterViewConfiguration(false));
        
        SaModel saAgent = SaModelFactory.getSAAgentFor(coreDump);
        Properties jvmProperties = saAgent.getSystemProperties();
        String jvmargs = saAgent.getJVMArgs();
        
        dvc.configureDetailsView(new DataViewComponent.DetailsViewConfiguration(0.25, 0, -1, -1, -1, -1));
        
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("Saved data", true), DataViewComponent.TOP_LEFT);
        dvc.addDetailsView(new OverviewViewSupport.SnapshotsViewSupport(coreDump).getDetailsView(), DataViewComponent.TOP_LEFT);
        
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("Details", true), DataViewComponent.TOP_RIGHT);
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
            return new DataViewComponent.MasterView("Overview", null, this);
        }
        
        
        private void initComponents(CoreDump coreDump) {
            setLayout(new BorderLayout());
            
            HTMLTextArea area = new HTMLTextArea("<nobr>" + getGeneralProperties(coreDump) + "</nobr>");
            area.setBorder(BorderFactory.createEmptyBorder(14, 8, 14, 8));
            setBackground(area.getBackground());
            
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
                data.append("<b>Main class:</b> " + mainClass + "<br>");
                data.append("<b>Arguments:</b> " + (mainArgs == null ? "none" : mainArgs) + "<br>");
            }
            
            // JVM information
            String jvmFlags = saAgent.getJVMFlags();
            
            data.append("<br>");
            data.append("<b>JVM:</b> " + saAgent.getVMName() + " (" + saAgent.getVmVersion() + ", " + saAgent.getVMInfo() + ")<br>");
            data.append("<b>Java Home:</b> " + saAgent.getJavaHome() + "<br>");
            data.append("<b>JVM Flags:</b> " + (jvmFlags == null || jvmFlags.length() == 0 ? "none" : jvmFlags) + "<br><br>");
            
            return data.toString();
            
        }
        
    }
    
}
