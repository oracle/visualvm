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

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.datasource.Snapshot;
import com.sun.tools.visualvm.core.datasupport.DataChangeEvent;
import com.sun.tools.visualvm.core.datasupport.DataChangeListener;
import com.sun.tools.visualvm.core.snapshot.RegisteredSnapshotCategories;
import com.sun.tools.visualvm.core.snapshot.SnapshotCategory;
import com.sun.tools.visualvm.core.ui.PluggableView;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import com.sun.tools.visualvm.core.ui.components.NotSupportedDisplayer;
import com.sun.tools.visualvm.core.ui.components.ScrollableContainer;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
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
public final class OverviewViewSupport {

    private static OverviewViewSupport instance;

    private HostOverviewPluggableView hostPluggableView;
    private ApplicationOverviewPluggableView applicationPluggableView;
    private CoreDumpOverviewPluggableView coreDumpPluggableView;


    /**
     * Returns singleton instance of OverviewViewSupport.
     * 
     * @return singleton instance of OverviewViewSupport.
     */
    public static synchronized OverviewViewSupport getInstance() {
        if (instance == null) instance = new OverviewViewSupport();
        return instance;
    }

    
    /**
     * Returns PluggableView instance to be used to customize the Overview view of a host.
     * 
     * @return PluggableView instance to be used to customize the Overview view of a host.
     */
    public PluggableView getHostPluggableView() {
        return getHostOverviewPluggableView();
    }
    
    HostOverviewPluggableView getHostOverviewPluggableView() {
        return hostPluggableView;
    }
    
    /**
     * Returns PluggableView instance to be used to customize the Overview view of an application.
     * 
     * @return PluggableView instance to be used to customize the Overview view of an application.
     */
    public PluggableView getApplicationPluggableView() {
        return getApplicationOverviewPluggableView();
    }

    ApplicationOverviewPluggableView getApplicationOverviewPluggableView() {
        return applicationPluggableView;
    }
    
    /**
     * Returns PluggableView instance to be used to customize the Overview view of a core dump.
     * 
     * @return PluggableView instance to be used to customize the Overview view of a core dump.
     */
    public PluggableView getCoreDumpPluggableView() {
        return getCoreDumpOverviewPluggableView();
    }
    
    CoreDumpOverviewPluggableView getCoreDumpOverviewPluggableView() {
        return coreDumpPluggableView;
    }
    
    private OverviewViewSupport() {
        hostPluggableView = new HostOverviewPluggableView();
        new HostOverviewViewProvider().initialize();
        
        applicationPluggableView = new ApplicationOverviewPluggableView();
        coreDumpPluggableView = new CoreDumpOverviewPluggableView();
        new ApplicationOverviewViewProvider().initialize();
        CoreDumpOverviewViewProvider.register();
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
            
            List<SnapshotCategory> snapshotCategories = RegisteredSnapshotCategories.sharedInstance().getCategories();
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
                HTMLTextArea area = new HTMLTextArea("<nobr>" + formatJVMArgs(jvmargs) + "</nobr>");
                area.setCaretPosition(0);
                area.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                setBackground(area.getBackground());
                contents = area;
            } else {
                contents = new NotSupportedDisplayer(NotSupportedDisplayer.JVM);
            }
            
            add(new ScrollableContainer(contents), BorderLayout.CENTER);
        }
        
        private String formatJVMArgs(String jvmargs) {
            String mangledString = " ".concat(jvmargs).replace(" -","\n");
            StringTokenizer tok = new StringTokenizer(mangledString,"\n");
            StringBuffer text = new StringBuffer(100);

            while(tok.hasMoreTokens()) {
                String arg = tok.nextToken().replace(" ","&nbsp;");
                int equalsSign = arg.indexOf('=');

                text.append("<b>");
                text.append("-");
                if (equalsSign != -1) {
                text.append(arg.substring(0,equalsSign));
                text.append("</b>");
                text.append(arg.substring(equalsSign));
                } else {
                text.append(arg);
                text.append("</b>");
                }
                text.append("<br>");
            }
            return text.toString();
        }
        
    }
    
    
    // --- System properties ---------------------------------------------------
    
    static class SystemPropertiesViewSupport extends JPanel  {
        
        public SystemPropertiesViewSupport(Properties properties) {
            initComponents(properties);
        }        
        
        public DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView("System properties", null, this, null);
        }
        
        private void initComponents(Properties properties) {
            setLayout(new BorderLayout());
            
            JComponent contents;
            
            if (properties != null) {
                HTMLTextArea area = new HTMLTextArea("<nobr>" + formatSystemProperties(properties) + "</nobr>");
                area.setCaretPosition(0);
                area.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                setBackground(area.getBackground());
                contents = area;
            } else {
                contents = new NotSupportedDisplayer(NotSupportedDisplayer.JVM);
            }
            
            add(new ScrollableContainer(contents), BorderLayout.CENTER);
        }
        
        private String formatSystemProperties(Properties properties) {
            StringBuffer text = new StringBuffer(200);
            List keys = new ArrayList(properties.keySet());
            Iterator keyIt;

            Collections.sort(keys);
            keyIt = keys.iterator();
            while(keyIt.hasNext()) {
                String key = (String) keyIt.next();
                String val = properties.getProperty(key);

                text.append("<b>");
                text.append(key);
                text.append("</b>=");
                text.append(val);
                text.append("<br>");
            }
            return text.toString();
        }
        
    }
}
