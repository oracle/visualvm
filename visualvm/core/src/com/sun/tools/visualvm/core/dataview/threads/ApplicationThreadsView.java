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

package com.sun.tools.visualvm.core.dataview.threads;

import com.sun.tools.visualvm.core.datasource.Application;
import com.sun.tools.visualvm.core.datasupport.DataFinishedListener;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.management.ThreadMXBean;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.Timer;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import org.netbeans.lib.profiler.ui.threads.ThreadsDetailsPanel;
import org.netbeans.lib.profiler.ui.threads.ThreadsPanel;
import org.openide.util.Utilities;

/**
 *
 * @author Jiri Sedlacek
 */
class ApplicationThreadsView extends DataSourceView {
    
    private static final String IMAGE_PATH = "com/sun/tools/visualvm/core/ui/resources/monitor.png";
    private static final int DEFAULT_REFRESH = 1000;

    private DataViewComponent view;
    

    public ApplicationThreadsView(Application application, ThreadMXBean threadBean) {
        super("Threads", new ImageIcon(Utilities.loadImage(IMAGE_PATH, true)).getImage(), 30);
        view = createViewComponent(application, threadBean);
        ThreadsViewSupport.getInstance().getApplicationThreadsPluggableView().makeCustomizations(view, application);
    }
        
    public DataViewComponent getView() {
        return view;
    }
    
    
    private DataViewComponent createViewComponent(Application application, ThreadMXBean threadBean) {
        final ThreadMXBeanDataManager threadsManager = new ThreadMXBeanDataManager(application, threadBean);
        final Timer timer = new Timer(DEFAULT_REFRESH, new ActionListener() {
            public void actionPerformed(ActionEvent e) { threadsManager.refreshThreads(); }
        });
        timer.setCoalesce(true);
        timer.start();
        application.notifyWhenFinished(new DataFinishedListener() {
            public void dataFinished(Object dataSource) { timer.stop(); }
        });
                
        final DataViewComponent dvc = new DataViewComponent(
                new MasterViewSupport(application, threadsManager, timer).getMasterView(),
                new DataViewComponent.MasterViewConfiguration(false));
        
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("Threads visualization", true), DataViewComponent.TOP_LEFT);
        
        final DetailsViewSupport detailsViewSupport = new DetailsViewSupport(threadsManager);
        final DataViewComponent.DetailsView detailsView = detailsViewSupport.getDetailsView();
        ThreadsPanel.ThreadsDetailsCallback callback = new ThreadsPanel.ThreadsDetailsCallback() {
            public void showDetails(final int[] indexes) {
                detailsViewSupport.showDetails(indexes);
                dvc.selectDetailsView(detailsView);
            }
        };
            
        dvc.addDetailsView(new TimelineViewSupport(threadsManager, callback).getDetailsView(), DataViewComponent.TOP_LEFT);
        dvc.addDetailsView(detailsView, DataViewComponent.TOP_LEFT);
        
        return dvc;
    }
    
    
    // --- General data --------------------------------------------------------
    
    private static class MasterViewSupport extends JPanel  {
        
        public MasterViewSupport(Application application, ThreadMXBeanDataManager threadsManager, Timer timer) {
            initComponents(application, threadsManager, timer);
        }
        
        
        public DataViewComponent.MasterView getMasterView() {
            return new DataViewComponent.MasterView("Threads", null, this);
        }
        
        
        private void initComponents(Application application, final ThreadMXBeanDataManager threadsManager, Timer timer) {
            setLayout(new BorderLayout());
            
            final HTMLTextArea area = new HTMLTextArea("<nobr>" + getThreadsCounts(threadsManager) + "</nobr>");
            area.setBorder(BorderFactory.createEmptyBorder(14, 8, 14, 8));
            setBackground(area.getBackground());
            
            timer.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    int selStart = area.getSelectionStart();
                    int selEnd   = area.getSelectionEnd();
                    area.setText("<nobr>" + getThreadsCounts(threadsManager) + "</nobr>");
                    area.select(selStart, selEnd);
                }
            });
            
            add(area, BorderLayout.CENTER);
        }
        
        private String getThreadsCounts(ThreadMXBeanDataManager threadsManager) {
            StringBuilder data = new StringBuilder();
          
            data.append("<b>Live threads:</b> " + threadsManager.getThreadCount() + "<br>");
            data.append("<b>Daemon threads:</b> " + threadsManager.getDaemonThreadCount() + "<br>");
          
            return data.toString();
        }
        
    }
    
    
    // --- Timeline ------------------------------------------------------------
    
    private static class TimelineViewSupport extends JPanel  {
        
        public TimelineViewSupport(ThreadMXBeanDataManager threadsManager, ThreadsPanel.ThreadsDetailsCallback callback) {
            initComponents(threadsManager, callback);
        }        
        
        public DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView("Timeline", null, this, null);
        }
        
        private void initComponents(ThreadMXBeanDataManager threadsManager, ThreadsPanel.ThreadsDetailsCallback callback) {
            setLayout(new BorderLayout());
            
            ThreadsPanel threadsPanel = new ThreadsPanel(threadsManager, callback, true);
            threadsPanel.threadsMonitoringEnabled();
            
            add(threadsPanel, BorderLayout.CENTER);
        }
        
    }
    
    
    // --- Details -------------------------------------------------------------
    
    private static class DetailsViewSupport extends JPanel  {
        
        private ThreadsDetailsPanel threadsDetailsPanel;
        
        public DetailsViewSupport(ThreadMXBeanDataManager threadsManager) {
            initComponents(threadsManager);
        }        
        
        public DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView("Details", null, this, null);
        }
        
        public void showDetails(int[] indexes) {
            threadsDetailsPanel.showDetails(indexes);
        }
        
        private void initComponents(ThreadMXBeanDataManager threadsManager) {
            setLayout(new BorderLayout());
            threadsDetailsPanel = new ThreadsDetailsPanel(threadsManager, true);
            add(threadsDetailsPanel, BorderLayout.CENTER);
        }
        
    }

}
