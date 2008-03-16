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

package com.sun.tools.visualvm.core.dataview.overview;

import com.sun.tools.visualvm.core.datasource.Application;
import com.sun.tools.visualvm.core.model.jvm.JVM;
import com.sun.tools.visualvm.core.model.jvm.JVMFactory;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import java.awt.BorderLayout;
import java.util.Properties;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import org.openide.util.Utilities;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
class ApplicationOverviewView extends DataSourceView {
    
    private static final String IMAGE_PATH = "com/sun/tools/visualvm/core/ui/resources/overview.png";

    private Application application;
    
    private DataViewComponent view;
    

    public ApplicationOverviewView(Application application) {
        super("Overview", new ImageIcon(Utilities.loadImage(IMAGE_PATH, true)).getImage(), 10);
        this.application = application;
    }
    
    
    protected void willBeAdded() {
        JVMFactory.getJVMFor(application); 
    }
    
    public DataViewComponent getView() {
        if (view == null) {
            view = createViewComponent(application);
            OverviewViewSupport.getInstance().getApplicationOverviewPluggableView().makeCustomizations(view, application);
            application = null;
        }
        return view;
    }
    
    
    private DataViewComponent createViewComponent(Application application) {
        DataViewComponent dvc = new DataViewComponent(
                new MasterViewSupport(application).getMasterView(),
                new DataViewComponent.MasterViewConfiguration(false));
        Properties jvmProperties = null;
        String jvmargs = null;
        JVM jvm = JVMFactory.getJVMFor(application);
        if (jvm.isBasicInfoSupported()) {
            jvmargs = jvm.getJvmArgs();
        }
        if (jvm.isGetSystemPropertiesSupported()) {
            jvmProperties = jvm.getSystemProperties();
        }
        dvc.configureDetailsView(new DataViewComponent.DetailsViewConfiguration(0.25, 0, -1, -1, -1, -1));
        
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("Saved data", true), DataViewComponent.TOP_LEFT);
        dvc.addDetailsView(new OverviewViewSupport.SnapshotsViewSupport(application).getDetailsView(), DataViewComponent.TOP_LEFT);
        
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("Details", true), DataViewComponent.TOP_RIGHT);
        dvc.addDetailsView(new OverviewViewSupport.JVMArgumentsViewSupport(jvmargs).getDetailsView(), DataViewComponent.TOP_RIGHT);
        dvc.addDetailsView(new OverviewViewSupport.SystemPropertiesViewSupport(jvmProperties).getDetailsView(), DataViewComponent.TOP_RIGHT);
        
        return dvc;
    }
    
    
    // --- General data --------------------------------------------------------
    
    private static class MasterViewSupport extends JPanel  {
        
        public MasterViewSupport(Application application) {
            initComponents(application);
        }
        
        
        public DataViewComponent.MasterView getMasterView() {
            return new DataViewComponent.MasterView("Overview", null, this);
        }
        
        
        private void initComponents(Application application) {
            setLayout(new BorderLayout());
            
            HTMLTextArea area = new HTMLTextArea("<nobr>" + getGeneralProperties(application) + "</nobr>");
            area.setBorder(BorderFactory.createEmptyBorder(14, 8, 14, 8));
            setBackground(area.getBackground());
            
            // TODO: implement listener for Application.oomeHeapDumpEnabled
            
            add(area, BorderLayout.CENTER);
        }
        
        private String getGeneralProperties(Application application) {
          JVM jvm = JVMFactory.getJVMFor(application);
          StringBuilder data = new StringBuilder();

          // Application information
          data.append("<b>PID:</b> " + application.getPid() + "<br>");
          data.append("<b>Host:</b> " + application.getHost().getHostName() + "<br>");
          if (jvm.isBasicInfoSupported()) {
            String mainArgs = jvm.getMainArgs();
            String mainClass = jvm.getMainClass();
            
            data.append("<b>Main class:</b> " + (mainClass == null ? "<Unknown>": mainClass) + "<br>");
            data.append("<b>Arguments:</b> " + (mainArgs == null ? "none" : mainArgs) + "<br>");
          }

          // JVM information
          if (jvm.isBasicInfoSupported()) {
            String jvmFlags = jvm.getJvmFlags();

            data.append("<br>");
            data.append("<b>JVM:</b> " + jvm.getVMName() + " (" + jvm.getVmVersion() + ", " + jvm.getVMInfo() + ")<br>");
            data.append("<b>Java Home:</b> " + jvm.getJavaHome() + "<br>");
            data.append("<b>JVM Flags:</b> " + (jvmFlags == null || jvmFlags.length() == 0 ? "none" : jvmFlags) + "<br>");
            if (jvm.isDumpOnOOMEnabledSupported()) {
              data.append("<br>");
              data.append("<b>Heap dump on OOME:</b> " + (jvm.isDumpOnOOMEnabled()?"enabled":"disabled") + "<br>");
            }
          }

          return data.toString();
        }
        
    }
    
}
