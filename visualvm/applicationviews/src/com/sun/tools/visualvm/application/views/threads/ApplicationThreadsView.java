/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.visualvm.application.views.threads;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.core.datasupport.DataRemovedListener;
import com.sun.tools.visualvm.application.jvm.Jvm;
import com.sun.tools.visualvm.application.jvm.JvmFactory;
import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.datasupport.Stateful;
import com.sun.tools.visualvm.core.options.GlobalPreferences;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import com.sun.tools.visualvm.threaddump.ThreadDumpSupport;
import com.sun.tools.visualvm.tools.jmx.JmxModel;
import com.sun.tools.visualvm.tools.jmx.JmxModel.ConnectionState;
import com.sun.tools.visualvm.tools.jmx.JmxModelFactory;
import com.sun.tools.visualvm.tools.jmx.JvmMXBeans;
import com.sun.tools.visualvm.tools.jmx.JvmMXBeansFactory;
import com.sun.tools.visualvm.tools.jmx.MBeanCacheListener;
import com.sun.tools.visualvm.uisupport.HTMLTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.ui.threads.ThreadsPanel;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.WeakListeners;

/**
 *
 * @author Jiri Sedlacek
 */
class ApplicationThreadsView extends DataSourceView implements DataRemovedListener<Application> {

    private static final String IMAGE_PATH = "com/sun/tools/visualvm/application/views/resources/threads.png";  // NOI18N
    private static final String DEADLOCK_ALERT_TEXT = NbBundle.getMessage(ApplicationThreadsView.class, "Deadlock_Alert_Text"); // NOI18N
    private JvmMXBeans mxbeans;
    private VisualVMThreadsDataManager threadsManager;
    private MBeanCacheListener listener;
    private boolean takeThreadDumpSupported;
    private MasterViewSupport mvs;

    ApplicationThreadsView(DataSource dataSource) {
        super(dataSource, NbBundle.getMessage(ApplicationThreadsView.class, "LBL_Threads"), new ImageIcon(ImageUtilities.loadImage(IMAGE_PATH, true)).getImage(), 30, false);   // NOI18N
    }

    @Override
    protected void willBeAdded() {
        DataSource ds = getDataSource();
        if (ds instanceof Application) {
            Application application = (Application)ds;
            Jvm jvm = JvmFactory.getJVMFor(application);
            takeThreadDumpSupported = jvm == null ? false : jvm.isTakeThreadDumpSupported();
            threadsManager = null;
            JmxModel jmxModel = JmxModelFactory.getJmxModelFor(application);
            if (jmxModel != null && jmxModel.getConnectionState() == ConnectionState.CONNECTED) {
                mxbeans = JvmMXBeansFactory.getJvmMXBeans(jmxModel, GlobalPreferences.sharedInstance().getThreadsPoll() * 1000);
                if (mxbeans != null) {
                    ThreadMXBeanDataManager threadsMXManager = new ThreadMXBeanDataManager(mxbeans.getThreadMXBean());
                    
                    threadsMXManager.addPropertyChangeListener(new PropertyChangeListener() {
                        public void propertyChange(PropertyChangeEvent evt) {
                            handleThreadsPropertyChange(evt);
                        }
                    });
                    threadsManager = threadsMXManager;
                }
            }
        } else {
            threadsManager = PersistenceSupport.loadDataManager(ds.getStorage());
        }
    }

    @Override
    protected synchronized void removed() {
        cleanup();
    }

    VisualVMThreadsDataManager getDataManager() {
        return threadsManager;
    }

    public synchronized void dataRemoved(Application dataSource) {
        cleanup();
    }
    
    private synchronized void cleanup() {
        if (mxbeans != null) {
            mxbeans.removeMBeanCacheListener(listener);
            mxbeans = null;
        }
    }

    @Override
    protected void setAlert(Alert newAlert, String newText) {
        super.setAlert(newAlert,newText);
        mvs.setAlertText(newText);
    }

    protected DataViewComponent createComponent() {
        DataSource ds = getDataSource();
        final Application application = ds instanceof Application ?
            (Application)ds : null;
        mvs = new MasterViewSupport(ds, takeThreadDumpSupported, threadsManager);
        if (mxbeans != null) {
            listener = new MBeanCacheListener() {
                public void flushed() {
                    if (application.getState() != Stateful.STATE_AVAILABLE) {
                        cleanup();
                    } else {
                        ((ThreadMXBeanDataManager)threadsManager).refreshThreadsAsync();
                        mvs.updateThreadsCounts(threadsManager);
                    }
                }
            };
            mxbeans.addMBeanCacheListener(listener);
        }
        if (application != null) application.notifyWhenRemoved(this);

        final DataViewComponent dvc = new DataViewComponent(mvs.getMasterView(), new DataViewComponent.MasterViewConfiguration(false));

        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(NbBundle.getMessage(ApplicationThreadsView.class, "LBL_Threads_visualization"), true), DataViewComponent.TOP_LEFT); // NOI18N
        dvc.addDetailsView(new TimelineViewSupport(threadsManager).getDetailsView(), DataViewComponent.TOP_LEFT);

        return dvc;
    }
    
    private void handleThreadsPropertyChange(PropertyChangeEvent evt) {
        if (ThreadMXBeanDataManager.DEADLOCK_PROP.equals(evt.getPropertyName())) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    setAlert(Alert.ERROR,DEADLOCK_ALERT_TEXT);
                    
                }
            });
        }
    }

    // --- General data --------------------------------------------------------

    private static class MasterViewSupport extends JPanel implements DataRemovedListener<Application>, PropertyChangeListener {

        private static RequestProcessor worker = null;

        private Application application;
        private HTMLTextArea area;
        private HTMLTextArea alertArea;
        private JButton threadDumpButton;
        private static final String LIVE_THRADS = NbBundle.getMessage(ApplicationThreadsView.class, "LBL_Live_threads");    // NOI18N
        private static final String DAEMON_THREADS = NbBundle.getMessage(ApplicationThreadsView.class, "LBL_Daemon_threads");   // NOI18N

        MasterViewSupport(DataSource dataSource, boolean takeThreadDumpSupported,
                          VisualVMThreadsDataManager threadsManager) {
            if (dataSource instanceof Application) application = (Application)dataSource;
            initComponents(takeThreadDumpSupported);
            updateThreadsCounts(threadsManager);
        }

        DataViewComponent.MasterView getMasterView() {
            return new DataViewComponent.MasterView(NbBundle.getMessage(ApplicationThreadsView.class, "LBL_Threads"), null, this);  // NOI18N
        }

        public void dataRemoved(Application dataSource) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    threadDumpButton.setEnabled(false);
                }
            });
        }

        public void propertyChange(PropertyChangeEvent evt) {
            dataRemoved(application);
        }

        private void initComponents(boolean takeThreadDumpSupported) {
            setLayout(new BorderLayout());
            setOpaque(false);

            area = new HTMLTextArea();
            area.setBorder(BorderFactory.createEmptyBorder(14, 8, 14, 8));

            add(area, BorderLayout.WEST);

            alertArea = new HTMLTextArea();
            alertArea.setBorder(BorderFactory.createEmptyBorder(14, 8, 14, 8));
            alertArea.setForeground(Color.RED);

            add(alertArea, BorderLayout.CENTER);

            threadDumpButton = new JButton(new AbstractAction(NbBundle.getMessage(ApplicationThreadsView.class, "LBL_Thread_Dump")) {   // NOI18N
                public void actionPerformed(ActionEvent e) {
                    ThreadDumpSupport.getInstance().takeThreadDump(application, (e.getModifiers() &
                            Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) == 0);
                }
            });
            threadDumpButton.setEnabled(takeThreadDumpSupported);

            JPanel buttonsArea = new JPanel(new BorderLayout());
            buttonsArea.setOpaque(false);
            JPanel buttonsContainer = new JPanel(new BorderLayout(3, 0));
            buttonsContainer.setBackground(area.getBackground());
            buttonsContainer.setBorder(BorderFactory.createEmptyBorder(14, 8, 14, 8));
            buttonsContainer.add(threadDumpButton, BorderLayout.EAST);
            buttonsArea.add(buttonsContainer, BorderLayout.NORTH);

            add(buttonsArea, BorderLayout.AFTER_LINE_ENDS);

            if (application != null) {
                application.notifyWhenRemoved(this);
                application.addPropertyChangeListener(Stateful.PROPERTY_STATE,
                        WeakListeners.propertyChange(this, application));
            }
        }

        private void updateThreadsCounts(final VisualVMThreadsDataManager threadsManager) {

            final int[] threads = new int[2];

            getWorker().post(new Runnable() {
                public void run() {
                    try {
                        threads[0] = threadsManager.getThreadCount();
                        threads[1] = threadsManager.getDaemonThreadCount();
                    } catch (Exception ex) {
                        threads[0] = 0;
                        threads[1] = 0;
                    }
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            updateThreadsCounts(threads[0], threads[1]);
                        }
                    });
                }
            });
        }

        private void setAlertText(String alert) {
            int selStart = alertArea.getSelectionStart();
            int selEnd = alertArea.getSelectionEnd();
            alertArea.setText("<center>"+alert+"</center>");   // NOI18N
            alertArea.select(selStart, selEnd);
        }
        
        private void updateThreadsCounts(int liveThreads, int daemonThreads) {
            StringBuilder data = new StringBuilder();

            data.append("<b>" + LIVE_THRADS + ":</b> " + liveThreads + "<br>");  // NOI18N
            data.append("<b>" + DAEMON_THREADS + ":</b> " + daemonThreads + "<br>");   // NOI18N

            int selStart = area.getSelectionStart();
            int selEnd = area.getSelectionEnd();
            area.setText(data.toString());
            area.select(selStart, selEnd);
        }

        private static synchronized RequestProcessor getWorker() {
            if (worker == null) worker = new RequestProcessor("ThreadsWorker", 1); // NOI18N
            return worker;
        }

    }

    // --- Timeline ------------------------------------------------------------

    private static class TimelineViewSupport extends JPanel {

        TimelineViewSupport(VisualVMThreadsDataManager threadsManager) {
            initComponents(threadsManager);
        }

        DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView(NbBundle.getMessage(ApplicationThreadsView.class, "LBL_Timeline"), null, 10, this, null);  // NOI18N
        }

        private void initComponents(VisualVMThreadsDataManager threadsManager) {
            setLayout(new BorderLayout());
            setOpaque(false);

            ThreadsPanel threadsPanel = new ThreadsPanel(threadsManager, null);
            threadsPanel.threadsMonitoringEnabled();

//            JComponent toolbar = (JComponent)threadsPanel.getToolbar();

//            add(toolbar, BorderLayout.NORTH);
            add(threadsPanel, BorderLayout.CENTER);
        }
    }
}
