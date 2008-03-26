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

package com.sun.tools.visualvm.application.views.overview;

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.snapshot.Snapshot;
import com.sun.tools.visualvm.core.datasupport.DataChangeEvent;
import com.sun.tools.visualvm.core.datasupport.DataChangeListener;
import com.sun.tools.visualvm.core.snapshot.RegisteredSnapshotCategories;
import com.sun.tools.visualvm.core.snapshot.SnapshotCategory;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import com.sun.tools.visualvm.core.ui.components.NotSupportedDisplayer;
import com.sun.tools.visualvm.core.ui.components.ScrollableContainer;
import java.awt.BorderLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;

/**
 * A public entrypoint to the Overview subtab.
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
class OverviewViewSupport {

 // --- General data --------------------------------------------------------

    static class MasterViewSupport extends JPanel  {

    public MasterViewSupport(ApplicationOverviewModel model) {
        initComponents(model);
    }


    public DataViewComponent.MasterView getMasterView() {
        return new DataViewComponent.MasterView("Overview", null, this);
    }


    private void initComponents(ApplicationOverviewModel model) {
        setLayout(new BorderLayout());

        HTMLTextArea area = new HTMLTextArea("<nobr>" + getGeneralProperties(model) + "</nobr>");
        area.setBorder(BorderFactory.createEmptyBorder(14, 8, 14, 8));
        setBackground(area.getBackground());

        // TODO: implement listener for Application.oomeHeapDumpEnabled

        add(area, BorderLayout.CENTER);
    }

    private String getGeneralProperties(ApplicationOverviewModel model) {
      StringBuilder data = new StringBuilder();

      // Application information
      data.append("<b>PID:</b> " + model.getPid() + "<br>");
      data.append("<b>Host:</b> " + model.getHostName() + "<br>");

      if (model.basicInfoSupported()) {
        data.append("<b>Main class:</b> " + model.getMainClass() + "<br>");
        data.append("<b>Arguments:</b> " + model.getMainArgs() + "<br>");

        data.append("<br>");
        data.append("<b>JVM:</b> " + model.getVmId() + "<br>");
        data.append("<b>Java Home:</b> " + model.getJavaHome() + "<br>");
        data.append("<b>JVM Flags:</b> " + model.getJvmFlags() + "<br><br>");
        data.append("<b>Heap dump on OOME:</b> " + model.oomeEnabled() + "<br>");
      }

      return data.toString();
      
    }

}

 // --- Snapshots -----------------------------------------------------------
    
    static class SnapshotsViewSupport extends JPanel  {
        
        public SnapshotsViewSupport(DataSource ds) {
            initComponents(ds);
        }        
        
        public DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView("Saved data", null, this, null);
        }
        
        private void initComponents(final DataSource ds) {
            setLayout(new BorderLayout());
            
            final HTMLTextArea area = new HTMLTextArea();
            area.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            setBackground(area.getBackground());
            
            ds.getRepository().addDataChangeListener(new DataChangeListener() {
                public void dataChanged(DataChangeEvent event) {
                    area.setText("<nobr>" + getSavedData(ds) + "</nobr>");
                }                
            }, Snapshot.class);
            
            add(new ScrollableContainer(area), BorderLayout.CENTER);
        }
        
        private String getSavedData(DataSource ds) {
            StringBuilder data = new StringBuilder();
            
            List<SnapshotCategory> snapshotCategories = RegisteredSnapshotCategories.sharedInstance().getVisibleCategories();
            for (SnapshotCategory category : snapshotCategories)
                data.append("<b>" + category.getName() + ":</b> " + ds.getRepository().getDataSources(category.getType()).size() + "<br>");

            return data.toString();
        }
        
    }
    
    
    // --- JVM arguments -------------------------------------------------------
    
    static class JVMArgumentsViewSupport extends JPanel  {
        
        public JVMArgumentsViewSupport(String jvmargs) {
            initComponents(jvmargs);
        }        
        
        public DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView("JVM arguments", null, this, null);
        }
        
        private void initComponents(String jvmargs) {
            setLayout(new BorderLayout());
            
            JComponent contents;
            
            if (jvmargs != null) {
                HTMLTextArea area = new HTMLTextArea("<nobr>" + jvmargs + "</nobr>");
                area.setCaretPosition(0);
                area.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                setBackground(area.getBackground());
                contents = area;
            } else {
                contents = new NotSupportedDisplayer(NotSupportedDisplayer.JVM);
            }
            
            add(new ScrollableContainer(contents), BorderLayout.CENTER);
        }
        
                }
        
    
    // --- System properties ---------------------------------------------------
    
    static class SystemPropertiesViewSupport extends JPanel  {
        
        public SystemPropertiesViewSupport(String properties) {
            initComponents(properties);
        }        
        
        public DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView("System properties", null, this, null);
        }
        
        private void initComponents(String properties) {
            setLayout(new BorderLayout());
            
            JComponent contents;
            
            if (properties != null) {
                HTMLTextArea area = new HTMLTextArea("<nobr>" + properties + "</nobr>");
                area.setCaretPosition(0);
                area.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                setBackground(area.getBackground());
                contents = area;
            } else {
                contents = new NotSupportedDisplayer(NotSupportedDisplayer.JVM);
            }
            
            add(new ScrollableContainer(contents), BorderLayout.CENTER);
        }
        
            }
        }
