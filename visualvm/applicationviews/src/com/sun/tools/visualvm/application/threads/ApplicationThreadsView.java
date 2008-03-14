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

package com.sun.tools.visualvm.application.threads;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.core.datasupport.DataRemovedListener;
import com.sun.tools.visualvm.application.JVM;
import com.sun.tools.visualvm.application.JVMFactory;
import com.sun.tools.visualvm.application.views.ApplicationViewsSupport;
import com.sun.tools.visualvm.core.options.GlobalPreferences;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.management.ThreadMXBean;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.Timer;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import org.netbeans.lib.profiler.ui.threads.ThreadsDetailsPanel;
import org.netbeans.lib.profiler.ui.threads.ThreadsPanel;
import org.netbeans.modules.profiler.ui.NBSwingWorker;
import org.openide.util.Utilities;

/**
 *
 * @author Jiri Sedlacek
 */
class ApplicationThreadsView extends DataSourceView implements DataRemovedListener<Application> {
    
    private static final String IMAGE_PATH = "com/sun/tools/visualvm/application/views/resources/threads.png";
    private static final int DEFAULT_REFRESH = 1000;

    private DataViewComponent view;
    private Application application;
    private JVM jvm;
    private ThreadMXBean threadBean;
    private ThreadMXBeanDataManager threadsManager;
    private Timer timer;
    

    public ApplicationThreadsView(Application application, ThreadMXBean threadBean) {
        super("Threads", new ImageIcon(Utilities.loadImage(IMAGE_PATH, true)).getImage(), 30);
        this.application = application;
        this.threadBean = threadBean;
    }
    
    protected void willBeAdded() {
        jvm = JVMFactory.getJVMFor(application);
        threadsManager = new ThreadMXBeanDataManager(application, threadBean);
    }
        
    public DataViewComponent getView() {
        if (view == null) {
            view = createViewComponent();
            ApplicationThreadsPluggableView pluggableView = (ApplicationThreadsPluggableView)ApplicationViewsSupport.sharedInstance().getThreadsView();
            pluggableView.makeCustomizations(view, application);
        }
        
        return view;
    }
    
    protected void removed() {
        timer.stop();
    }
    
    public void dataRemoved(Application dataSource) {
        timer.stop();
    }
    
    
    private DataViewComponent createViewComponent() {
        timer = new Timer(GlobalPreferences.sharedInstance().getThreadsPoll() * 1000, new ActionListener() {
            public void actionPerformed(ActionEvent e) { threadsManager.refreshThreads(); }
        });
        timer.setInitialDelay(0);
        timer.start();
        application.notifyWhenRemoved(this);
                
        final DataViewComponent dvc = new DataViewComponent(
                new MasterViewSupport(application, jvm, threadsManager, timer).getMasterView(),
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
    
    private static class MasterViewSupport extends JPanel implements DataRemovedListener<Application> {
        
        private HTMLTextArea area;
        private JButton threadDumpButton;
        
        
        public MasterViewSupport(Application application, JVM jvm, ThreadMXBeanDataManager threadsManager, Timer timer) {
            initComponents(application, jvm, threadsManager, timer);
        }
        
        
        public DataViewComponent.MasterView getMasterView() {
            return new DataViewComponent.MasterView("Threads", null, this);
        }
        
        public void dataRemoved(Application dataSource) {
            threadDumpButton.setEnabled(false);
        }
        
        
        private void initComponents(final Application application, JVM jvm, final ThreadMXBeanDataManager threadsManager, Timer timer) {
            setLayout(new BorderLayout());
            
            area = new HTMLTextArea();
            area.setBorder(BorderFactory.createEmptyBorder(14, 8, 14, 8));
            updateThreadsCounts(threadsManager);
            setBackground(area.getBackground());
            
            timer.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    updateThreadsCounts(threadsManager);
                }
            });
            
            add(area, BorderLayout.CENTER);
            
//            threadDumpButton = new JButton(new AbstractAction("Thread Dump") {
//                public void actionPerformed(ActionEvent e) {
//                    ThreadDumpSupport.getInstance().takeThreadDump(application, (e.getModifiers() & InputEvent.CTRL_MASK) == 0);
//                }
//            });
//            threadDumpButton.setEnabled(jvm.isTakeThreadDumpSupported());
            
            JPanel buttonsArea = new JPanel(new BorderLayout());
            buttonsArea.setBackground(area.getBackground());
            JPanel buttonsContainer = new JPanel(new BorderLayout(3, 0));
            buttonsContainer.setBackground(area.getBackground());
            buttonsContainer.setBorder(BorderFactory.createEmptyBorder(14, 8, 14, 8));
//            buttonsContainer.add(threadDumpButton, BorderLayout.EAST);
            buttonsArea.add(buttonsContainer, BorderLayout.NORTH);
            
            add(buttonsArea, BorderLayout.AFTER_LINE_ENDS);
            
            application.notifyWhenRemoved(this);
        }
        
        private void updateThreadsCounts(final ThreadMXBeanDataManager threadsManager) {
            
            final int[] threads = new int[2];
            
            new NBSwingWorker() {
                protected void doInBackground() {
                    try {
                        threads[0] = threadsManager.getThreadCount();
                        threads[1] = threadsManager.getDaemonThreadCount();
                    } catch (Exception ex) {
                        threads[0] = 0;
                        threads[1] = 0;
                    }
                }
                protected void done() {
                    StringBuilder data = new StringBuilder();
          
                    data.append("<b>Live threads:</b> " + threads[0] + "<br>");
                    data.append("<b>Daemon threads:</b> " + threads[1] + "<br>");

                    int selStart = area.getSelectionStart();
                    int selEnd   = area.getSelectionEnd();
                    area.setText(data.toString());
                    area.select(selStart, selEnd);
                }
            }.execute();
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
