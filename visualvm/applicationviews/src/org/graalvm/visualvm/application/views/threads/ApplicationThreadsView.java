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

package org.graalvm.visualvm.application.views.threads;

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.core.datasupport.DataRemovedListener;
import org.graalvm.visualvm.application.jvm.Jvm;
import org.graalvm.visualvm.application.jvm.JvmFactory;
import org.graalvm.visualvm.core.datasource.DataSource;
import org.graalvm.visualvm.core.datasupport.Stateful;
import org.graalvm.visualvm.core.options.GlobalPreferences;
import org.graalvm.visualvm.core.ui.DataSourceView;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.threaddump.ThreadDumpSupport;
import org.graalvm.visualvm.tools.jmx.JmxModel;
import org.graalvm.visualvm.tools.jmx.JmxModel.ConnectionState;
import org.graalvm.visualvm.tools.jmx.JmxModelFactory;
import org.graalvm.visualvm.tools.jmx.JvmMXBeans;
import org.graalvm.visualvm.tools.jmx.JvmMXBeansFactory;
import org.graalvm.visualvm.tools.jmx.MBeanCacheListener;
import org.graalvm.visualvm.uisupport.HTMLTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.lib.ui.components.ProfilerToolbar;
import org.graalvm.visualvm.lib.ui.swing.ActionPopupButton;
import org.graalvm.visualvm.lib.ui.swing.GrayLabel;
import org.graalvm.visualvm.lib.ui.swing.SearchUtils;
import org.graalvm.visualvm.lib.ui.threads.ThreadsPanel;
import org.graalvm.visualvm.lib.profiler.api.ActionsSupport;
import org.graalvm.visualvm.lib.profiler.api.ProfilerDialogs;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.WeakListeners;

/**
 *
 * @author Jiri Sedlacek
 */
class ApplicationThreadsView extends DataSourceView implements DataRemovedListener<Application> {

    private static final String IMAGE_PATH = "org/graalvm/visualvm/application/views/resources/threads.png";  // NOI18N
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

    @NbBundle.Messages({
        "ThreadsFeatureUI_show=Show:",
        "ThreadsFeatureUI_filterAll=All Threads",
        "ThreadsFeatureUI_filterLive=Live Threads",
        "ThreadsFeatureUI_filterFinished=Finished Threads",
        "ThreadsFeatureUI_filterSelected=Selected Threads",
        "ThreadsFeatureUI_timeline=Timeline:",
        "ThreadsFeatureUI_threadsFilter=Threads filter",
        "# HTML formatted:",
        "ThreadsFeatureUI_noThreadsMsg=<html><b>No threads are currently selected.</b><br><br>Use the Selected column or invoke Select thread action to select threads.</html>"
    })
    private static class TimelineViewSupport extends JPanel {
        private ProfilerToolbar toolbar;
        private ThreadsPanel threadsPanel;
        
        private JLabel shLabel;
        private ActionPopupButton shFilter;

        private JLabel tlLabel;
        private JComponent tlZoomInButton;
        private JComponent tlZoomOutButton;
        private JComponent tlFitWidthButton;
        

        TimelineViewSupport(VisualVMThreadsDataManager threadsManager) {
            initComponents(threadsManager);
        }

        DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView(NbBundle.getMessage(ApplicationThreadsView.class, "LBL_Timeline"), null, 10, this, null);  // NOI18N
        }

        private void initComponents(VisualVMThreadsDataManager threadsManager) {
            setLayout(new BorderLayout());
            setOpaque(false);

            threadsPanel = new ThreadsPanel(threadsManager, null) {
                protected void filterSelected(ThreadsPanel.Filter filter) {
                    super.filterSelected(filter);
                    shFilter.selectAction(filter.ordinal());
                }
            };
            threadsPanel.threadsMonitoringEnabled();
            
            InputMap inputMap = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
            ActionMap actionMap = getActionMap();

            final String filterKey = org.graalvm.visualvm.lib.ui.swing.FilterUtils.FILTER_ACTION_KEY;
            Action filterAction = new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    Action action = threadsPanel.getActionMap().get(filterKey);
                    if (action != null && action.isEnabled()) action.actionPerformed(e);
                }
            };
            ActionsSupport.registerAction(filterKey, filterAction, actionMap, inputMap);

            final String findKey = SearchUtils.FIND_ACTION_KEY;
            Action findAction = new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    Action action = threadsPanel.getActionMap().get(findKey);
                    if (action != null && action.isEnabled()) action.actionPerformed(e);
                }
            };
            ActionsSupport.registerAction(findKey, findAction, actionMap, inputMap);
            
            // -----------------------------------------------------------------
            // --- copy-pasted timeline toolbar from org.graalvm.visualvm.lib.profiler.v2.features.ThreadsFeatureUI
            
            shLabel = new GrayLabel(Bundle.ThreadsFeatureUI_show());

            Action aAll = new AbstractAction() {
                { putValue(NAME, Bundle.ThreadsFeatureUI_filterAll()); }
                public void actionPerformed(ActionEvent e) { setFilter(ThreadsPanel.Filter.ALL); }

            };
            Action aLive = new AbstractAction() {
                { putValue(NAME, Bundle.ThreadsFeatureUI_filterLive()); }
                public void actionPerformed(ActionEvent e) { setFilter(ThreadsPanel.Filter.LIVE); }

            };
            Action aFinished = new AbstractAction() {
                { putValue(NAME, Bundle.ThreadsFeatureUI_filterFinished()); }
                public void actionPerformed(ActionEvent e) { setFilter(ThreadsPanel.Filter.FINISHED); }

            };
            Action aSelected = new AbstractAction() {
                { putValue(NAME, Bundle.ThreadsFeatureUI_filterSelected()); }
                public void actionPerformed(ActionEvent e) { setSelectedFilter(); }

            };
            shFilter = new ActionPopupButton(aAll, aLive, aFinished, aSelected);
            shFilter.setToolTipText(Bundle.ThreadsFeatureUI_threadsFilter());

            tlLabel = new GrayLabel(Bundle.ThreadsFeatureUI_timeline());


            tlZoomInButton = (JComponent)threadsPanel.getZoomIn();
            tlZoomInButton.putClientProperty("JButton.buttonType", "segmented"); // NOI18N
            tlZoomInButton.putClientProperty("JButton.segmentPosition", "first"); // NOI18N
            tlZoomOutButton = (JComponent)threadsPanel.getZoomOut();
            tlZoomOutButton.putClientProperty("JButton.buttonType", "segmented"); // NOI18N
            tlZoomOutButton.putClientProperty("JButton.segmentPosition", "middle"); // NOI18N
            tlFitWidthButton = (JComponent)threadsPanel.getFitWidth();
            tlFitWidthButton.putClientProperty("JButton.buttonType", "segmented"); // NOI18N
            tlFitWidthButton.putClientProperty("JButton.segmentPosition", "last"); // NOI18N

            toolbar = ProfilerToolbar.create(true);

//            toolbar.addSpace(2);
//            toolbar.addSeparator();
            toolbar.addSpace(5);

            toolbar.add(shLabel);
            toolbar.addSpace(2);
            toolbar.add(shFilter);

            toolbar.addSpace(2);
            toolbar.addSeparator();
            toolbar.addSpace(5);

            toolbar.add(tlLabel);
            toolbar.addSpace(2);
            toolbar.add(tlZoomInButton);
            toolbar.add(tlZoomOutButton);
            toolbar.add(tlFitWidthButton);
            
            add(toolbar.getComponent(), BorderLayout.NORTH);

            setFilter(ThreadsPanel.Filter.LIVE);
            
            // -----------------------------------------------------------------

            add(threadsPanel, BorderLayout.CENTER);
        }
        
        private void setSelectedFilter() {
            if (threadsPanel.hasSelectedThreads()) {
                setFilter(ThreadsPanel.Filter.SELECTED);
            } else {
                threadsPanel.showSelectedColumn();
                shFilter.selectAction(threadsPanel.getFilter().ordinal());
                ProfilerDialogs.displayWarning(Bundle.ThreadsFeatureUI_noThreadsMsg());
            }
        }

        private void setFilter(ThreadsPanel.Filter filter) {
            threadsPanel.setFilter(filter);
        }
    }
}
