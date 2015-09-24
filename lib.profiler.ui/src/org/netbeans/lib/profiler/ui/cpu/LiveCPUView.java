/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2014 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.netbeans.lib.profiler.ui.cpu;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.tree.TreeNode;
import org.netbeans.lib.profiler.ProfilerClient;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.results.RuntimeCCTNode;
import org.netbeans.lib.profiler.results.cpu.CPUCCTProvider;
import org.netbeans.lib.profiler.results.cpu.CPUResultsDiff;
import org.netbeans.lib.profiler.results.cpu.CPUResultsSnapshot;
import org.netbeans.lib.profiler.results.cpu.FlatProfileContainer;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.JExtendedSplitPane;
import org.netbeans.lib.profiler.ui.memory.LiveMemoryView;
import org.netbeans.lib.profiler.ui.results.DataView;
import org.netbeans.lib.profiler.ui.swing.FilterUtils;
import org.netbeans.lib.profiler.ui.swing.ProfilerTable;
import org.netbeans.lib.profiler.ui.swing.ProfilerTreeTable;
import org.netbeans.lib.profiler.ui.swing.SearchUtils;
import org.netbeans.lib.profiler.utils.Wildcards;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class LiveCPUView extends JPanel {

    private static final int MIN_UPDATE_DIFF = 900;
    private static final int MAX_UPDATE_DIFF = 1400;
    
    private final ResultsMonitor rm;
    
    private CPUResultsSnapshot snapshot;
    private CPUResultsSnapshot refSnapshot;
    private boolean sampled;
    private boolean mergedThreads;
    private Collection<Integer> selectedThreads;
    
    private DataView lastFocused;
    private CPUTableView hotSpotsView;
    private CPUTreeTableView forwardCallsView;
    private CPUTreeTableView reverseCallsView;
    
    private ThreadsSelector threadsSelector;
    
    private long lastupdate;
    private volatile boolean paused;
    private volatile boolean forceRefresh;
    private volatile boolean refreshIsRunning;
    
    @ServiceProvider(service=CPUCCTProvider.Listener.class)
    public static final class ResultsMonitor implements CPUCCTProvider.Listener {

        private LiveCPUView view;
        
        @Override
        public void cctEstablished(RuntimeCCTNode appRootNode, boolean empty) {
            if (view != null && !empty) {
                try {
                    view.refreshData(appRootNode);
                } catch (ClientUtils.TargetAppOrVMTerminated ex) {
                    Logger.getLogger(LiveMemoryView.class.getName()).log(Level.FINE, null, ex);
                }
            }
        }

        @Override
        public void cctReset() {
            if (view != null) {
                view.resetData();
            }
        }
    }
    
    public LiveCPUView(Set<ClientUtils.SourceCodeSelection> selection) {
        initUI(selection);
        registerActions();
        
        rm = Lookup.getDefault().lookup(ResultsMonitor.class);
        rm.view = this;
    }
    
    
    public void setView(boolean forwardCalls, boolean hotSpots, boolean reverseCalls) {
        forwardCallsView.setVisible(forwardCalls);
        hotSpotsView.setVisible(hotSpots);
        reverseCallsView.setVisible(reverseCalls);
    }
    
    public ThreadsSelector createThreadSelector() {
        threadsSelector = new ThreadsSelector() {
            protected CPUResultsSnapshot getSnapshot() { return snapshot; }
            protected void selectionChanged(Collection<Integer> selected, boolean mergeThreads) {
                mergedThreads = mergeThreads;
                selectedThreads = selected;
                setData();
            }
            void reset() {
                super.reset();
                mergedThreads = false;
                selectedThreads = null;
            }
        };
        return threadsSelector;
    }
    
    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public void setForceRefresh(boolean forceRefresh) {
        this.forceRefresh = forceRefresh;
    }

    private void refreshData(RuntimeCCTNode appRootNode) throws ClientUtils.TargetAppOrVMTerminated {
        if ((lastupdate + MIN_UPDATE_DIFF > System.currentTimeMillis() || paused) && !forceRefresh) return;
        if (refreshIsRunning) return;
        refreshIsRunning = true;
        try {
            ProfilerClient client = getProfilerClient();
            final CPUResultsSnapshot snapshotData =
                    client.getStatus().getInstrMethodClasses() == null ?
                    null : client.getCPUProfilingResultsSnapshot(false);
            snapshot = snapshotData;
            sampled = snapshotData == null ? true :
                      client.getCurrentInstrType() == ProfilerClient.INSTR_NONE_SAMPLING;
            setData();
            lastupdate = System.currentTimeMillis();
            forceRefresh = false;
        } catch (CPUResultsSnapshot.NoDataAvailableException e) {
            refreshIsRunning = false;
        } catch (Throwable t) {
            refreshIsRunning = false;
            if (t instanceof ClientUtils.TargetAppOrVMTerminated) {
                throw ((ClientUtils.TargetAppOrVMTerminated)t);
            } else {
                Logger.getLogger(LiveCPUView.class.getName()).log(Level.SEVERE, null, t);
            }
        }
    }
    
    private void setData() {
        if (snapshot == null) {
            resetData();
            refreshIsRunning = false;
        } else {
            final CPUResultsSnapshot _snapshot = refSnapshot == null ? snapshot :
                                                 refSnapshot.createDiff(snapshot);
            
            final FlatProfileContainer flatData = _snapshot.getFlatProfile(selectedThreads, CPUResultsSnapshot.METHOD_LEVEL_VIEW);
            
            final Map<Integer, ClientUtils.SourceCodeSelection> idMap = _snapshot.getMethodIDMap(CPUResultsSnapshot.METHOD_LEVEL_VIEW);

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    try {
                        boolean diff = _snapshot instanceof CPUResultsDiff;
                        forwardCallsView.setData(_snapshot, idMap, CPUResultsSnapshot.METHOD_LEVEL_VIEW, selectedThreads, mergedThreads, sampled, diff);
                        hotSpotsView.setData(flatData, idMap, sampled, diff);
                        reverseCallsView.setData(_snapshot, idMap, CPUResultsSnapshot.METHOD_LEVEL_VIEW, selectedThreads, mergedThreads, sampled, diff);
                    } finally {
                        refreshIsRunning = false;
                    }
                }
            });
        }
    }
    
    public boolean setDiffView(boolean diff) {
        if (snapshot == null) return false;
        refSnapshot = diff ? snapshot : null;
        setData();
        return true;
    }
    
    public void refreshData() throws ClientUtils.TargetAppOrVMTerminated {
        if ((lastupdate + MAX_UPDATE_DIFF < System.currentTimeMillis() && !paused) || forceRefresh) {
            getProfilerClient().forceObtainedResultsDump(true);
        }        
    }
    
    public void resetData() {
        forwardCallsView.resetData();
        hotSpotsView.resetData();
        reverseCallsView.resetData();
        snapshot = null;
        refSnapshot = null;
        sampled = true;
        if (threadsSelector != null) threadsSelector.reset();
    }
    
    
    public void showSelectionColumn() {
        forwardCallsView.showSelectionColumn();
        hotSpotsView.showSelectionColumn();
        reverseCallsView.showSelectionColumn();
    }
    
    public void refreshSelection() {
        forwardCallsView.refreshSelection();
        hotSpotsView.refreshSelection();
        reverseCallsView.refreshSelection();
    }
    
    
    public void cleanup() {
        if (rm.view == this) rm.view = null;
    }
    
    
    protected abstract ProfilerClient getProfilerClient();
    
    
    protected boolean profileMethodSupported() { return true; }
    
    
    protected abstract boolean showSourceSupported();
    
    protected abstract void showSource(ClientUtils.SourceCodeSelection value);
    
    protected abstract void selectForProfiling(ClientUtils.SourceCodeSelection value);
    
    protected void popupShowing() {};
    
    protected void popupHidden() {};
    
    
    protected void foundInForwardCalls() {}
    
    protected void foundInHotSpots() {}
    
    protected void foundInReverseCalls() {}
    
    
    private void profileMethod(ClientUtils.SourceCodeSelection value) {
        selectForProfiling(value);
    }
    
    private void profileClass(ClientUtils.SourceCodeSelection value) {
        selectForProfiling(new ClientUtils.SourceCodeSelection(
                           value.getClassName(), Wildcards.ALLWILDCARD, null));
    }
    
    
    private void initUI(Set<ClientUtils.SourceCodeSelection> selection) {
        setLayout(new BorderLayout(0, 0));
        
        forwardCallsView = new CPUTreeTableView(selection, false) {
            protected void performDefaultAction(ClientUtils.SourceCodeSelection userValue) {
                if (showSourceSupported()) showSource(userValue);
            }
            protected void populatePopup(JPopupMenu popup, Object value, ClientUtils.SourceCodeSelection userValue) {
                LiveCPUView.this.populatePopup(forwardCallsView, popup, value, userValue);
            }
            protected void popupShowing() { LiveCPUView.this.popupShowing(); }
            protected void popupHidden()  { LiveCPUView.this.popupHidden(); }
            protected boolean hasBottomFilterFindMargin() { return true; }
        };
        forwardCallsView.notifyOnFocus(new Runnable() {
            public void run() { lastFocused = forwardCallsView; }
        });
        
        hotSpotsView = new CPUTableView(selection) {
            protected void performDefaultAction(ClientUtils.SourceCodeSelection userValue) {
                if (showSourceSupported()) showSource(userValue);
            }
            protected void populatePopup(JPopupMenu popup, Object value, ClientUtils.SourceCodeSelection userValue) {
                LiveCPUView.this.populatePopup(hotSpotsView, popup, value, userValue);
            }
            protected void popupShowing() { LiveCPUView.this.popupShowing(); }
            protected void popupHidden()  { LiveCPUView.this.popupHidden(); }
            protected boolean hasBottomFilterFindMargin() { return true; }
        };
        hotSpotsView.notifyOnFocus(new Runnable() {
            public void run() { lastFocused = hotSpotsView; }
        });
        
        reverseCallsView = new CPUTreeTableView(selection, true) {
            protected void performDefaultAction(ClientUtils.SourceCodeSelection userValue) {
                if (showSourceSupported()) showSource(userValue);
            }
            protected void populatePopup(JPopupMenu popup, Object value, ClientUtils.SourceCodeSelection userValue) {
                LiveCPUView.this.populatePopup(reverseCallsView, popup, value, userValue);
            }
            protected void popupShowing() { LiveCPUView.this.popupShowing(); }
            protected void popupHidden()  { LiveCPUView.this.popupHidden(); }
            protected boolean hasBottomFilterFindMargin() { return true; }
        };
        reverseCallsView.notifyOnFocus(new Runnable() {
            public void run() { lastFocused = reverseCallsView; }
        });
        
        JSplitPane upperSplit = new JExtendedSplitPane(JSplitPane.VERTICAL_SPLIT) {
            {
                setBorder(null);
                setDividerSize(5);

                if (getUI() instanceof BasicSplitPaneUI) {
                    BasicSplitPaneDivider divider = ((BasicSplitPaneUI)getUI()).getDivider();
                    if (divider != null) {
                        Color c = UIUtils.isNimbus() || UIUtils.isAquaLookAndFeel() ?
                                  UIUtils.getDisabledLineColor() : new JSeparator().getForeground();
                        divider.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, c));
                    }
                }
            }
        };
        upperSplit.setBorder(BorderFactory.createEmptyBorder());
        upperSplit.setTopComponent(forwardCallsView);
        upperSplit.setBottomComponent(hotSpotsView);
        upperSplit.setDividerLocation(0.5d);
        upperSplit.setResizeWeight(0.5d);
        
        JSplitPane lowerSplit = new JExtendedSplitPane(JSplitPane.VERTICAL_SPLIT) {
            {
                setBorder(null);
                setDividerSize(5);

                if (getUI() instanceof BasicSplitPaneUI) {
                    BasicSplitPaneDivider divider = ((BasicSplitPaneUI)getUI()).getDivider();
                    if (divider != null) {
                        Color c = UIUtils.isNimbus() || UIUtils.isAquaLookAndFeel() ?
                                  UIUtils.getDisabledLineColor() : new JSeparator().getForeground();
                        divider.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, c));
                    }
                }
            }
        };
        lowerSplit.setBorder(BorderFactory.createEmptyBorder());
        lowerSplit.setTopComponent(upperSplit);
        lowerSplit.setBottomComponent(reverseCallsView);
        lowerSplit.setDividerLocation(0.66d);
        lowerSplit.setResizeWeight(0.66d);
        
        add(lowerSplit, BorderLayout.CENTER);
        
//        // TODO: read last state?
//        setView(true, false);
    }
    
    private void registerActions() {
        ActionMap map = getActionMap();
        
        map.put(FilterUtils.FILTER_ACTION_KEY, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                DataView active = getLastFocused();
                if (active != null) active.activateFilter();
            }
        });
        
        map.put(SearchUtils.FIND_ACTION_KEY, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                DataView active = getLastFocused();
                if (active != null) active.activateSearch();
            }
        });
    }
    
    private DataView getLastFocused() {
        if (lastFocused != null && !lastFocused.isShowing()) lastFocused = null;
        
        if (lastFocused == null) {
            if (forwardCallsView.isShowing()) lastFocused = forwardCallsView;
            else if (hotSpotsView.isShowing()) lastFocused = hotSpotsView;
            else if (reverseCallsView.isShowing()) lastFocused = reverseCallsView;
        }
        
        return lastFocused;
    }
    
    private void populatePopup(final DataView invoker, JPopupMenu popup, Object value, final ClientUtils.SourceCodeSelection userValue) {
        if (showSourceSupported()) {
            popup.add(new JMenuItem(CPUView.ACTION_GOTOSOURCE) {
                { setEnabled(userValue != null); setFont(getFont().deriveFont(Font.BOLD)); }
                protected void fireActionPerformed(ActionEvent e) { showSource(userValue); }
            });
            popup.addSeparator();
        }
        
        popup.add(new JMenuItem(CPUView.ACTION_PROFILE_METHOD) {
            { setEnabled(profileMethodSupported() && userValue != null && CPUTableView.isSelectable(userValue)); }
            protected void fireActionPerformed(ActionEvent e) { profileMethod(userValue); }
        });
        
        popup.add(new JMenuItem(CPUView.ACTION_PROFILE_CLASS) {
            { setEnabled(userValue != null); }
            protected void fireActionPerformed(ActionEvent e) { profileClass(userValue); }
        });
        
        popup.addSeparator();
        if (invoker == forwardCallsView) {
            ProfilerTreeTable ttable = (ProfilerTreeTable)forwardCallsView.getResultsComponent();
            int column = ttable.convertColumnIndexToView(ttable.getMainColumn());
            final String searchString = ttable.getStringValue((TreeNode)value, column);
            
            popup.add(new JMenuItem(CPUView.FIND_IN_HOTSPOTS) {
                { setEnabled(userValue != null); }
                protected void fireActionPerformed(ActionEvent e) {
                    ProfilerTable table = hotSpotsView.getResultsComponent();
                    if (SearchUtils.findString(table, searchString)) {
                        hotSpotsView.setVisible(true);
                        foundInHotSpots();
                        table.requestFocusInWindow();
                    }
                }
            });
            
            popup.add(new JMenuItem(CPUView.FIND_IN_REVERSECALLS) {
                { setEnabled(userValue != null); }
                protected void fireActionPerformed(ActionEvent e) {
                    ProfilerTable table = reverseCallsView.getResultsComponent();
                    if (SearchUtils.findString(table, searchString)) {
                        reverseCallsView.setVisible(true);
                        foundInReverseCalls();
                        table.requestFocusInWindow();
                    }
                }
            });
        } else if (invoker == hotSpotsView) {
            // Ugly hack - there's a space between method name and parameters
            final String searchString = value.toString().replace("(", " ("); // NOI18N
            
            popup.add(new JMenuItem(CPUView.FIND_IN_FORWARDCALLS) {
                { setEnabled(userValue != null); }
                protected void fireActionPerformed(ActionEvent e) {
                    ProfilerTable table = forwardCallsView.getResultsComponent();
                    if (SearchUtils.findString(table, searchString)) {
                        forwardCallsView.setVisible(true);
                        foundInForwardCalls();
                        table.requestFocusInWindow();
                    }
                }
            });
            
            popup.add(new JMenuItem(CPUView.FIND_IN_REVERSECALLS) {
                { setEnabled(userValue != null); }
                protected void fireActionPerformed(ActionEvent e) {
                    ProfilerTable table = reverseCallsView.getResultsComponent();
                    if (SearchUtils.findString(table, searchString)) {
                        reverseCallsView.setVisible(true);
                        foundInReverseCalls();
                        table.requestFocusInWindow();
                    }
                }
            });
        } else if (invoker == reverseCallsView) {
            ProfilerTreeTable ttable = (ProfilerTreeTable)reverseCallsView.getResultsComponent();
            int column = ttable.convertColumnIndexToView(ttable.getMainColumn());
            final String searchString = ttable.getStringValue((TreeNode)value, column);
            
            popup.add(new JMenuItem(CPUView.FIND_IN_FORWARDCALLS) {
                { setEnabled(userValue != null); }
                protected void fireActionPerformed(ActionEvent e) {
                    ProfilerTable table = forwardCallsView.getResultsComponent();
                    if (SearchUtils.findString(table, searchString)) {
                        forwardCallsView.setVisible(true);
                        foundInForwardCalls();
                        table.requestFocusInWindow();
                    }
                }
            });
            
            popup.add(new JMenuItem(CPUView.FIND_IN_HOTSPOTS) {
                { setEnabled(userValue != null); }
                protected void fireActionPerformed(ActionEvent e) {
                    ProfilerTable table = hotSpotsView.getResultsComponent();
                    if (SearchUtils.findString(table, searchString)) {
                        hotSpotsView.setVisible(true);
                        foundInHotSpots();
                        table.requestFocusInWindow();
                    }
                }
            });
        }
        
        popup.addSeparator();
        popup.add(invoker.createCopyMenuItem());
        
        popup.addSeparator();
        popup.add(new JMenuItem(FilterUtils.ACTION_FILTER) {
            protected void fireActionPerformed(ActionEvent e) { invoker.activateFilter(); }
        });
        popup.add(new JMenuItem(SearchUtils.ACTION_FIND) {
            protected void fireActionPerformed(ActionEvent e) { invoker.activateSearch(); }
        });
    }
    
}
