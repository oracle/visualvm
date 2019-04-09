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
package org.graalvm.visualvm.jfr.views.threads;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.util.HashMap;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.jfr.JFRSnapshot;
import org.graalvm.visualvm.jfr.model.JFREvent;
import org.graalvm.visualvm.jfr.model.JFREventVisitor;
import org.graalvm.visualvm.jfr.model.JFRPropertyNotAvailableException;
import org.graalvm.visualvm.jfr.model.JFRThread;
import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;
import org.graalvm.visualvm.lib.jfluid.results.threads.ThreadData;
import org.graalvm.visualvm.lib.ui.components.HTMLTextArea;
import org.graalvm.visualvm.lib.ui.components.ProfilerToolbar;
import org.graalvm.visualvm.lib.ui.swing.ActionPopupButton;
import org.graalvm.visualvm.lib.ui.swing.GrayLabel;
import org.graalvm.visualvm.lib.ui.threads.ThreadsPanel;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
class ThreadsViewSupport {
    
    static abstract class MasterViewSupport extends JPanel {

        private static RequestProcessor worker = null;

//        private Application application;
        private HTMLTextArea area;
        private HTMLTextArea alertArea;
        private JButton threadDumpButton;
        private static final String LIVE_THRADS = NbBundle.getMessage(ThreadsViewSupport.class, "LBL_Live_threads");    // NOI18N
        private static final String DAEMON_THREADS = NbBundle.getMessage(ThreadsViewSupport.class, "LBL_Daemon_threads");   // NOI18N

        MasterViewSupport(JFRSnapshot snapshot/*, VisualVMThreadsDataManager threadsManager*/) {
//            if (dataSource instanceof Application) application = (Application)dataSource;
            initComponents();
//            updateThreadsCounts(threadsManager);
        }
        
        abstract void firstShown();

        DataViewComponent.MasterView getMasterView() {
            return new DataViewComponent.MasterView(NbBundle.getMessage(ThreadsViewSupport.class, "LBL_Threads"), null, this);  // NOI18N
        }
        
        void initialized() {
            area.setText("Threads & states estimation");
        }

        private void initComponents() {
            setLayout(new BorderLayout());
            setOpaque(false);

            area = new HTMLTextArea("<nobr><b>Progress:</b> reading data...</nobr>");
            area.setBorder(BorderFactory.createEmptyBorder(14, 8, 14, 8));

            add(area, BorderLayout.WEST);

            alertArea = new HTMLTextArea();
            alertArea.setBorder(BorderFactory.createEmptyBorder(14, 8, 14, 8));
            alertArea.setForeground(Color.RED);

            add(alertArea, BorderLayout.CENTER);

            threadDumpButton = new JButton(new AbstractAction(NbBundle.getMessage(ThreadsViewSupport.class, "LBL_Thread_Dump")) {   // NOI18N
                public void actionPerformed(ActionEvent e) {
//                    ThreadDumpSupport.getInstance().takeThreadDump(application, (e.getModifiers() &
//                            Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) == 0);
                }
            });
            threadDumpButton.setEnabled(false);

            JPanel buttonsArea = new JPanel(new BorderLayout());
            buttonsArea.setOpaque(false);
            JPanel buttonsContainer = new JPanel(new BorderLayout(3, 0));
            buttonsContainer.setBackground(area.getBackground());
            buttonsContainer.setBorder(BorderFactory.createEmptyBorder(14, 8, 14, 8));
            buttonsContainer.add(threadDumpButton, BorderLayout.EAST);
            buttonsArea.add(buttonsContainer, BorderLayout.NORTH);

            add(buttonsArea, BorderLayout.AFTER_LINE_ENDS);
            
            addHierarchyListener(new HierarchyListener() {
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

//        private void updateThreadsCounts(final VisualVMThreadsDataManager threadsManager) {
//
//            final int[] threads = new int[2];
//
//            getWorker().post(new Runnable() {
//                public void run() {
//                    try {
//                        threads[0] = threadsManager.getThreadCount();
//                        threads[1] = threadsManager.getDaemonThreadCount();
//                    } catch (Exception ex) {
//                        threads[0] = 0;
//                        threads[1] = 0;
//                    }
//                    SwingUtilities.invokeLater(new Runnable() {
//                        public void run() {
//                            updateThreadsCounts(threads[0], threads[1]);
//                        }
//                    });
//                }
//            });
//        }
//
//        private void setAlertText(String alert) {
//            int selStart = alertArea.getSelectionStart();
//            int selEnd = alertArea.getSelectionEnd();
//            alertArea.setText("<center>"+alert+"</center>");   // NOI18N
//            alertArea.select(selStart, selEnd);
//        }
//        
//        private void updateThreadsCounts(int liveThreads, int daemonThreads) {
//            StringBuilder data = new StringBuilder();
//
//            data.append("<b>" + LIVE_THRADS + ":</b> " + liveThreads + "<br>");  // NOI18N
//            data.append("<b>" + DAEMON_THREADS + ":</b> " + daemonThreads + "<br>");   // NOI18N
//
//            int selStart = area.getSelectionStart();
//            int selEnd = area.getSelectionEnd();
//            area.setText(data.toString());
//            area.select(selStart, selEnd);
//        }
//
//        private static synchronized RequestProcessor getWorker() {
//            if (worker == null) worker = new RequestProcessor("ThreadsWorker", 1); // NOI18N
//            return worker;
//        }

    }
    
    
//    @NbBundle.Messages({
//        "ThreadsFeatureUI_show=Show:",
//        "ThreadsFeatureUI_filterAll=All Threads",
//        "ThreadsFeatureUI_filterLive=Live Threads",
//        "ThreadsFeatureUI_filterFinished=Finished Threads",
//        "ThreadsFeatureUI_filterSelected=Selected Threads",
//        "ThreadsFeatureUI_timeline=Timeline:",
//        "ThreadsFeatureUI_threadsFilter=Threads filter",
//        "# HTML formatted:",
//        "ThreadsFeatureUI_noThreadsMsg=<html><b>No threads are currently selected.</b><br><br>Use the Selected column or invoke Select thread action to select threads.</html>"
//    })
    static abstract class TimelineViewSupport extends JPanel implements JFREventVisitor {
        
        private JFRThreadsDataManager threadsManager;
        
        private ProfilerToolbar toolbar;
        private ThreadsPanel threadsPanel;
        
        private JLabel shLabel;
        private ActionPopupButton shFilter;

        private JLabel tlLabel;
        private JComponent tlZoomInButton;
        private JComponent tlZoomOutButton;
        private JComponent tlFitWidthButton;
        

        TimelineViewSupport() {
            initModels();
            initComponents();
        }

        
        DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView(NbBundle.getMessage(ThreadsViewSupport.class, "LBL_Timeline"), null, 10, this, null);  // NOI18N
        }
        
        
        abstract long lastTimestamp();
        
        private long firstTimestamp = -1;
        private Map<Long, ThreadData> threadMap = new HashMap(); // use TreeMap to sort by TID
        
        @Override
        public boolean visit(String typeName, JFREvent event) {
            if ("jdk.ThreadAllocationStatistics".equals(typeName)) { // NOI18N
                processThreadEvent(event, "eventThread", Byte.MIN_VALUE); // NOI18N
            } else if ("jdk.ThreadStart".equals(typeName)) { // NOI18N
                processThreadEvent(event, "thread", Byte.MIN_VALUE); // NOI18N
            } else if ("jdk.ThreadEnd".equals(typeName)) { // NOI18N
                processThreadEvent(event, "thread", CommonConstants.THREAD_STATUS_ZOMBIE); // NOI18N
            } else if ("jdk.JavaMonitorWait".equals(typeName)) { // NOI18N
                processThreadEvent(event, "eventThread", CommonConstants.THREAD_STATUS_WAIT); // NOI18N
            } else if ("jdk.JavaMonitorEnter".equals(typeName)) { // NOI18N
                processThreadEvent(event, "eventThread", CommonConstants.THREAD_STATUS_MONITOR); // NOI18N
            } else if ("jdk.ThreadPark".equals(typeName)) { // NOI18N
                processThreadEvent(event, "eventThread", CommonConstants.THREAD_STATUS_PARK); // NOI18N
            } else if ("jdk.ThreadSleep".equals(typeName)) { // NOI18N
                processThreadEvent(event, "eventThread", CommonConstants.THREAD_STATUS_SLEEPING); // NOI18N
            } else if ("jdk.FileRead".equals(typeName)) { // NOI18N
                processThreadEvent(event, "eventThread", CommonConstants.THREAD_STATUS_RUNNING); // NOI18N
            } else if ("jdk.FileWrite".equals(typeName)) { // NOI18N
                processThreadEvent(event, "eventThread", CommonConstants.THREAD_STATUS_RUNNING); // NOI18N
            } else if ("jdk.SocketRead".equals(typeName)) { // NOI18N
                processThreadEvent(event, "eventThread", CommonConstants.THREAD_STATUS_RUNNING); // NOI18N
            } else if ("jdk.SocketWrite".equals(typeName)) { // NOI18N
                processThreadEvent(event, "eventThread", CommonConstants.THREAD_STATUS_RUNNING); // NOI18N
            } else if ("jdk.ClassLoad".equals(typeName)) { // NOI18N
                processThreadEvent(event, "eventThread", CommonConstants.THREAD_STATUS_RUNNING); // NOI18N
            } else if ("jdk.Compilation".equals(typeName)) { // NOI18N
                processCompilationEvent(event);
            }
            
            return false;
        }
        
        private void processThreadEvent(JFREvent event, String tprefix, byte tstate) {
//            System.err.println(">>> PROCESSING " + event);
            try {
                JFRThread thread = event.getThread(tprefix);
                long tid = thread.getId();
//                long tid = event.getLong(tprefix + ".javaThreadId"); // NOI18N
                ThreadData tdata = threadMap.get(tid);

                if (tdata == null) {
                    String tname = thread.getName(); // NOI18N
//                    String tname = event.getString(tprefix + ".javaName"); // NOI18N
                    tdata = new ThreadData(tname, "java.lang.Thread"); // NOI18N
                    threadMap.put(tid, tdata);
                }

                if (tstate != Byte.MIN_VALUE && tstate != tdata.getLastState()) {
                    long ttime = event.getInstant("eventTime").toEpochMilli();
                    if (firstTimestamp == -1) firstTimestamp = ttime;
                    tdata.add(ttime, tstate);

    //                if (tstate == CommonConstants.THREAD_STATUS_WAIT || tstate == CommonConstants.THREAD_STATUS_MONITOR ||
    //                    tstate == CommonConstants.THREAD_STATUS_PARK || tstate == CommonConstants.THREAD_STATUS_SLEEPING)
    //                    tdata.add(ttime + event.getDuration().toMillis(), CommonConstants.THREAD_STATUS_RUNNING);
                }
            } catch (JFRPropertyNotAvailableException e) {
                System.err.println(">>> ||| " + e + " --- " + event);
            }
        }
        
        private void processCompilationEvent(JFREvent event) {
//            System.err.println(">>> PROCESSING " + event);
            String tprefix = "eventThread"; // NOI18N
            
            try {
                JFRThread thread = event.getThread(tprefix);
                long tid = thread.getId();
//                long tid = event.getLong(tprefix + ".javaThreadId"); // NOI18N
                ThreadData tdata = threadMap.get(tid);

                if (tdata == null) {
                    String tname = thread.getName(); // NOI18N
//                    String tname = event.getString(tprefix + ".javaName"); // NOI18N
                    tdata = new ThreadData(tname, "java.lang.Thread"); // NOI18N
                    threadMap.put(tid, tdata);
                }

                long ttime = event.getInstant("eventTime").toEpochMilli();
                long durat = event.getDuration("eventDuration").toMillis();
                if (firstTimestamp == -1) firstTimestamp = ttime;

                tdata.add(ttime, CommonConstants.THREAD_STATUS_RUNNING);
                tdata.add(ttime + durat, CommonConstants.THREAD_STATUS_WAIT); // ??
            } catch (JFRPropertyNotAvailableException e) {
                System.err.println(">>> ### " + e + " --- " + event);
            }
        }

        @Override
        public void done() {
            for (ThreadData tdata : threadMap.values())
                tdata.add(lastTimestamp(), tdata.getLastState());
            
            threadsManager.setData(firstTimestamp, lastTimestamp(), threadMap.values());
            threadMap.clear();
            threadMap = null;
        }
        
        
        private void initModels() {
            threadsManager = new JFRThreadsDataManager();
        }

        private void initComponents() {
            setLayout(new BorderLayout());
            setOpaque(false);

            threadsPanel = new ThreadsPanel(threadsManager, null) {
                protected void filterSelected(ThreadsPanel.Filter filter) {
                    super.filterSelected(filter);
                    shFilter.selectAction(filter.ordinal());
                }
            };
            threadsPanel.threadsMonitoringEnabled();
            
//            InputMap inputMap = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
//            ActionMap actionMap = getActionMap();
//
//            final String filterKey = org.graalvm.visualvm.lib.ui.swing.FilterUtils.FILTER_ACTION_KEY;
//            Action filterAction = new AbstractAction() {
//                public void actionPerformed(ActionEvent e) {
//                    Action action = threadsPanel.getActionMap().get(filterKey);
//                    if (action != null && action.isEnabled()) action.actionPerformed(e);
//                }
//            };
//            ActionsSupport.registerAction(filterKey, filterAction, actionMap, inputMap);
//
//            final String findKey = SearchUtils.FIND_ACTION_KEY;
//            Action findAction = new AbstractAction() {
//                public void actionPerformed(ActionEvent e) {
//                    Action action = threadsPanel.getActionMap().get(findKey);
//                    if (action != null && action.isEnabled()) action.actionPerformed(e);
//                }
//            };
//            ActionsSupport.registerAction(findKey, findAction, actionMap, inputMap);
            
            // -----------------------------------------------------------------
            // --- copy-pasted timeline toolbar from org.graalvm.visualvm.lib.profiler.v2.features.ThreadsFeatureUI
            
            shLabel = new GrayLabel("Show:");

            Action aAll = new AbstractAction() {
                { putValue(NAME, "All Threads"); }
                public void actionPerformed(ActionEvent e) { setFilter(ThreadsPanel.Filter.ALL); }

            };
            Action aLive = new AbstractAction() {
                { putValue(NAME, "Live Threads"); }
                public void actionPerformed(ActionEvent e) { setFilter(ThreadsPanel.Filter.LIVE); }

            };
            Action aFinished = new AbstractAction() {
                { putValue(NAME, "Finished Threads"); }
                public void actionPerformed(ActionEvent e) { setFilter(ThreadsPanel.Filter.FINISHED); }

            };
            Action aSelected = new AbstractAction() {
                { putValue(NAME, "Selected Threads"); }
                public void actionPerformed(ActionEvent e) { setSelectedFilter(); }

            };
            shFilter = new ActionPopupButton(aAll, aLive, aFinished, aSelected);
            shFilter.setToolTipText("Threads filter");

            tlLabel = new GrayLabel("Timeline:");


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

//            setFilter(ThreadsPanel.Filter.LIVE);
            setFilter(ThreadsPanel.Filter.ALL);
            
            // -----------------------------------------------------------------

            add(threadsPanel, BorderLayout.CENTER);
        }
        
        private void setSelectedFilter() {
            if (threadsPanel.hasSelectedThreads()) {
                setFilter(ThreadsPanel.Filter.SELECTED);
            } else {
                threadsPanel.showSelectedColumn();
                shFilter.selectAction(threadsPanel.getFilter().ordinal());
//                ProfilerDialogs.displayWarning(Bundle.ThreadsFeatureUI_noThreadsMsg());
            }
        }

        private void setFilter(ThreadsPanel.Filter filter) {
            threadsPanel.setFilter(filter);
        }
        
    }
    
}
