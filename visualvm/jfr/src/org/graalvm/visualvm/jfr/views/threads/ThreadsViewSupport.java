/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.jfr.model.JFREvent;
import org.graalvm.visualvm.jfr.model.JFREventVisitor;
import org.graalvm.visualvm.jfr.model.JFRModel;
import org.graalvm.visualvm.jfr.model.JFRPropertyNotAvailableException;
import org.graalvm.visualvm.jfr.model.JFRThread;
import org.graalvm.visualvm.jfr.utils.ValuesConverter;
import org.graalvm.visualvm.jfr.views.components.MessageComponent;
import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;
import org.graalvm.visualvm.lib.jfluid.results.threads.ThreadData;
import org.graalvm.visualvm.lib.ui.components.HTMLTextArea;
import org.graalvm.visualvm.lib.ui.components.ProfilerToolbar;
import org.graalvm.visualvm.lib.ui.swing.ActionPopupButton;
import org.graalvm.visualvm.lib.ui.swing.GrayLabel;
import org.graalvm.visualvm.lib.ui.threads.ThreadsPanel;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
class ThreadsViewSupport {
    
    static abstract class MasterViewSupport extends JPanel {

//        private static RequestProcessor worker = null;

//        private Application application;
        private HTMLTextArea area;
        private HTMLTextArea alertArea;
//        private JButton threadDumpButton;
//        private static final String LIVE_THRADS = NbBundle.getMessage(ThreadsViewSupport.class, "LBL_Live_threads");    // NOI18N
//        private static final String DAEMON_THREADS = NbBundle.getMessage(ThreadsViewSupport.class, "LBL_Daemon_threads");   // NOI18N

        MasterViewSupport(JFRModel model/*, VisualVMThreadsDataManager threadsManager*/) {
//            if (dataSource instanceof Application) application = (Application)dataSource;
            initComponents(model);
//            updateThreadsCounts(threadsManager);
        }
        
        abstract void firstShown();

        DataViewComponent.MasterView getMasterView() {
            return new DataViewComponent.MasterView(NbBundle.getMessage(ThreadsViewSupport.class, "LBL_Threads"), null, this);  // NOI18N
        }
        
        void initialized(Collection<String> activeTypes, int threadsCount) {
            if (activeTypes.isEmpty()) {
                area.setText(threadsCount == 0 ? "No threads information recorded" : "No thread states recorded.");
            } else {
                StringBuilder sb = new StringBuilder();
                boolean first = true;
                for (String s : activeTypes) {
                    if (first) first = false;
                    else sb.append(", ");
                    sb.append(s);
                }
                area.setText("Thread states based on:&nbsp;&nbsp;<code>" + sb.toString() + "</code>");
            }
        }

        private void initComponents(JFRModel model) {
            setLayout(new BorderLayout());
            setOpaque(false);

            if (model == null) {
                add(MessageComponent.notAvailable(), BorderLayout.CENTER);
            } else {
                area = new HTMLTextArea("<nobr><b>Progress:</b> reading data...</nobr>");
                area.setBorder(BorderFactory.createEmptyBorder(14, 8, 14, 8));

                add(area, BorderLayout.WEST);

                alertArea = new HTMLTextArea();
                alertArea.setBorder(BorderFactory.createEmptyBorder(14, 8, 14, 8));
                alertArea.setForeground(Color.RED);

                add(alertArea, BorderLayout.CENTER);

//                threadDumpButton = new JButton(new AbstractAction(NbBundle.getMessage(ThreadsViewSupport.class, "LBL_Thread_Dump")) {   // NOI18N
//                    public void actionPerformed(ActionEvent e) {
//    //                    ThreadDumpSupport.getInstance().takeThreadDump(application, (e.getModifiers() &
//    //                            Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) == 0);
//                    }
//                });
//                threadDumpButton.setEnabled(false);
//
//                JPanel buttonsArea = new JPanel(new BorderLayout());
//                buttonsArea.setOpaque(false);
//                JPanel buttonsContainer = new JPanel(new BorderLayout(3, 0));
//                buttonsContainer.setBackground(area.getBackground());
//                buttonsContainer.setBorder(BorderFactory.createEmptyBorder(14, 8, 14, 8));
//                buttonsContainer.add(threadDumpButton, BorderLayout.EAST);
//                buttonsArea.add(buttonsContainer, BorderLayout.NORTH);
//
//                add(buttonsArea, BorderLayout.AFTER_LINE_ENDS);

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
    static class TimelineViewSupport extends JPanel implements JFREventVisitor {
        
        private final JFRModel jfrModel;
        
        private JFRThreadsDataManager threadsManager;
        
        private ProfilerToolbar toolbar;
        private ThreadsPanel threadsPanel;
        
        private JLabel shLabel;
        private ActionPopupButton shFilter;

        private JLabel tlLabel;
        private JComponent tlZoomInButton;
        private JComponent tlZoomOutButton;
        private JComponent tlFitWidthButton;
        

        TimelineViewSupport(JFRModel jfrModel) {
            this.jfrModel = jfrModel;
            
            initModels();
            initComponents();
        }

        
        DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView(NbBundle.getMessage(ThreadsViewSupport.class, "LBL_Timeline"), null, 10, this, null);  // NOI18N
        }
        
        
        private static final class Definition {
            final String tname; long firstTime; byte firstState;
            Definition(String tname, long firstTime, byte firstState) { this.tname = tname; this.firstTime = firstTime; this.firstState = firstState; }
        }
        
        private static final class State {
            final long time; final byte tstate;
            State(long time, byte tstate) { this.time = time; this.tstate = tstate; }
            @Override public int hashCode() { return Long.hashCode(time); }
            @Override public boolean equals(Object o) { return o instanceof State ? ((State)o).time == time : false; }

            static final Comparator<State> COMPARATOR = new Comparator<State>() {
                @Override public int compare(State r1, State r2) { return Long.compare(r1.time, r2.time); }
            };
        }
        
        
        private long firstTimestamp = Long.MAX_VALUE;
        private long lastTimestamp = Long.MIN_VALUE;
        private Map<Long, List<State>> states;
        private Map<Long, Definition> definitions;
        private Set<String> ignoredEvents;
        
        private boolean[] activeTypes = new boolean[6];
        
        @Override
        public void init() {
            states = new HashMap<>();
            definitions = new TreeMap<>();
            ignoredEvents = new HashSet<>();
        }
        
        @Override
        public boolean visit(String typeName, JFREvent event) {
            switch (typeName) {
                case "jdk.ThreadStart": // NOI18N
                    if (processEvent(event, "thread", CommonConstants.THREAD_STATUS_RUNNING, Byte.MIN_VALUE)) // NOI18N
                        activeTypes[0] = true;
                    break;
                
                case "jdk.ThreadEnd": // NOI18N
                    if (processEvent(event, "thread", CommonConstants.THREAD_STATUS_ZOMBIE, Byte.MIN_VALUE)) // NOI18N
                        activeTypes[1] = true;
                    break;
                
                case "jdk.JavaMonitorWait": // NOI18N
                    if (processEvent(event, "eventThread", CommonConstants.THREAD_STATUS_WAIT, CommonConstants.THREAD_STATUS_RUNNING)) // NOI18N
                        activeTypes[2] = true;
                    break;
                
                case "jdk.JavaMonitorEnter": // NOI18N
                    if (processEvent(event, "eventThread", CommonConstants.THREAD_STATUS_MONITOR, CommonConstants.THREAD_STATUS_RUNNING)) // NOI18N
                        activeTypes[3] = true;
                    break;
                
                case "jdk.ThreadPark": // NOI18N
                    if (processEvent(event, "eventThread", CommonConstants.THREAD_STATUS_PARK, CommonConstants.THREAD_STATUS_RUNNING)) // NOI18N
                        activeTypes[4] = true;
                    break;
                
                case "jdk.ThreadSleep": // NOI18N
                    if (processEvent(event, "eventThread", CommonConstants.THREAD_STATUS_SLEEPING, CommonConstants.THREAD_STATUS_RUNNING)) // NOI18N
                        activeTypes[5] = true;
                    break;
                
                case "jdk.Compilation": // NOI18N
                    processEvent(event, "eventThread", CommonConstants.THREAD_STATUS_RUNNING, CommonConstants.THREAD_STATUS_PARK); // ?? // NOI18N
                    break;
                
                case "jdk.ThreadAllocationStatistics": // NOI18N
                    try {
                        JFRThread thread = event.getThread("eventThread"); // NOI18N
                        if (thread != null) {
                            long allocated = event.getLong("allocated"); // NOI18N
                            byte tstate = allocated > 0 ? CommonConstants.THREAD_STATUS_RUNNING : CommonConstants.THREAD_STATUS_WAIT; // ??
                            processDefinition(thread.getId(), thread.getName(), ValuesConverter.instantToRelativeNanos(event.getInstant("eventTime"), jfrModel), tstate); // NOI18N
                        }
                    } catch (JFRPropertyNotAvailableException e) { System.err.println(">>> --- " + e); }
                    break;
                
                default:
                    try {
                        if (!ignoredEvents.contains(typeName)) {
                            JFRThread thread = event.getThread("eventThread"); // NOI18N
                            if (thread != null) processDefinition(thread.getId(), thread.getName(), ValuesConverter.instantToRelativeNanos(event.getInstant("eventTime"), jfrModel), CommonConstants.THREAD_STATUS_RUNNING); // NOI18N
                        }
                    } catch (JFRPropertyNotAvailableException e) {
                        ignoredEvents.add(typeName);
                    }
            }
            
            return false;
        }
        
        @Override
        public void done() {
            final Collection<ThreadData> tdataC = new ArrayList<>();
            
            for (Map.Entry<Long, Definition> definitionE : definitions.entrySet()) {
                long tid = definitionE.getKey();
                Definition definition = definitionE.getValue();
                
                List<State> statesL = states.get(tid);
                if (statesL == null) statesL = new ArrayList<>();
                Collections.sort(statesL, State.COMPARATOR);
                
                if (statesL.isEmpty() && definition.firstState != Byte.MIN_VALUE) {
                    statesL.add(new State(definition.firstTime, definition.firstState));
                } else {
                    long firstStateTime = statesL.get(0).time;
                    if (firstStateTime > definition.firstTime && definition.firstState != Byte.MIN_VALUE)
                        statesL.add(0, new State(definition.firstTime, definition.firstState));
                }
                
                firstTimestamp = Math.min(firstTimestamp, statesL.get(0).time);
                lastTimestamp = Math.max(lastTimestamp, statesL.get(statesL.size() - 1).time);
                
                ThreadData tdata = new ThreadData(definition.tname, "java.lang.Thread");
                byte lastState = Byte.MIN_VALUE;
                
                for (State state : statesL) {
                    long ttime = state.time;
                    byte tstate = state.tstate;
                    
                    if (lastState != tstate) {
                        tdata.add(jfrModel.nsToAbsoluteMillis(ttime), tstate);
                        lastState = tstate;
                    }
                }
                
                tdataC.add(tdata);
            }
            
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    threadsManager.setData(jfrModel.nsToAbsoluteMillis(firstTimestamp), jfrModel.nsToAbsoluteMillis(lastTimestamp), tdataC);
                }
            });
            
            states = null;
            definitions = null;
            ignoredEvents = null;
        }
        
        
        Collection<String> getActiveTypes() {
            List<String> names = new ArrayList<>();
            
            if (activeTypes[0]) names.add("jdk.ThreadStart"); // NOI18N
            if (activeTypes[1]) names.add("jdk.ThreadEnd"); // NOI18N
            if (activeTypes[2]) names.add("jdk.JavaMonitorWait"); // NOI18N
            if (activeTypes[3]) names.add("jdk.JavaMonitorEnter"); // NOI18N
            if (activeTypes[4]) names.add("jdk.ThreadPark"); // NOI18N
            if (activeTypes[5]) names.add("jdk.ThreadSleep"); // NOI18N
            
            activeTypes = null;
            
            return names;
        }
        
        int getThreadsCount() {
            return threadsManager.getThreadsCount();
        }
        
        
        private boolean processEvent(JFREvent event, String tkey, byte tstate1, byte tstate2) {
            try {
                JFRThread thread = event.getThread(tkey);
                if (thread == null) return false;
                
                long tid = thread.getId();
                List<State> tdata = states.get(tid);
                
                if (tdata == null) {
                    tdata = new ArrayList<>();
                    states.put(tid, tdata);
                }
                
                long ttime = ValuesConverter.instantToRelativeNanos(event.getInstant("eventTime"), jfrModel); // NOI18N
                tdata.add(new State(ttime, tstate1));
                
                processDefinition(tid, thread.getName(), ttime, tstate1);
                
                if (tstate2 != Byte.MIN_VALUE) {
                    ttime += ValuesConverter.durationToNanos(event.getDuration("eventDuration")); // NOI18N
                    tdata.add(new State(ttime, tstate2));
                }
                
                return true;
            } catch (JFRPropertyNotAvailableException e) {
                System.err.println(">>> " + e + " --- " + event);
                return false;
            }
        }
        
        private void processDefinition(long tid, String tname, long ttime, byte tstate) {
            Definition definition = definitions.get(tid);
            
            if (definition == null) {
                definitions.put(tid, new Definition(tname, ttime, tstate));
            } else if (definition.firstTime > ttime) {
                definition.firstTime = ttime;
                definition.firstState = tstate;
            }
            
//            firstTimestamp = Math.min(firstTimestamp, ttime);
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
            
            // Workaround to initialize the timeline in fit-width mode
            if (tlFitWidthButton instanceof AbstractButton) ((AbstractButton)tlFitWidthButton).doClick();

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
