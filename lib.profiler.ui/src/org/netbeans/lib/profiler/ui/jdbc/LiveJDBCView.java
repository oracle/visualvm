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

package org.netbeans.lib.profiler.ui.jdbc;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import org.netbeans.lib.profiler.ProfilerClient;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.results.RuntimeCCTNode;
import org.netbeans.lib.profiler.results.cpu.CPUCCTProvider;
import org.netbeans.lib.profiler.results.cpu.CPUResultsSnapshot;
import org.netbeans.lib.profiler.ui.memory.LiveMemoryView;
import org.netbeans.lib.profiler.ui.results.DataView;
import org.netbeans.lib.profiler.ui.swing.FilterUtils;
import org.netbeans.lib.profiler.ui.swing.SearchUtils;
import org.netbeans.lib.profiler.utils.Wildcards;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class LiveJDBCView extends JPanel {

    private static final int MIN_UPDATE_DIFF = 900;
    private static final int MAX_UPDATE_DIFF = 1400;
    
    private final ResultsMonitor rm;
    
    private CPUResultsSnapshot snapshot;
    private CPUResultsSnapshot refSnapshot;
    
    private DataView lastFocused;
    private JDBCTreeTableView jdbcCallsView;
    
    private long lastupdate;
    private volatile boolean paused;
    private volatile boolean forceRefresh;
    private volatile boolean refreshIsRunning;
    
    @ServiceProvider(service=CPUCCTProvider.Listener.class)
    public static final class ResultsMonitor implements CPUCCTProvider.Listener {

        private LiveJDBCView view;
        
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
    
    
    public LiveJDBCView(Set<ClientUtils.SourceCodeSelection> selection) {
        initUI(selection);
        registerActions();
        
        rm = Lookup.getDefault().lookup(ResultsMonitor.class);
        rm.view = this;
    }
    
    
    public void setView(boolean forwardCalls, boolean hotSpots, boolean reverseCalls) {
        jdbcCallsView.setVisible(forwardCalls);
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
                Logger.getLogger(LiveJDBCView.class.getName()).log(Level.SEVERE, null, t);
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
            
//            final FlatProfileContainer flatData = _snapshot.getFlatProfile(selectedThreads, CPUResultsSnapshot.METHOD_LEVEL_VIEW);
//            
//            final Map<Integer, ClientUtils.SourceCodeSelection> idMap = _snapshot.getMethodIDMap(CPUResultsSnapshot.METHOD_LEVEL_VIEW);
//
//            SwingUtilities.invokeLater(new Runnable() {
//                public void run() {
//                    try {
//                        boolean diff = _snapshot instanceof CPUResultsDiff;
//                        jdbcCallsView.setData(_snapshot, idMap, CPUResultsSnapshot.METHOD_LEVEL_VIEW, selectedThreads, mergedThreads, sampled, diff);
//                    } finally {
//                        refreshIsRunning = false;
//                    }
//                }
//            });
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
        jdbcCallsView.resetData();
        snapshot = null;
        refSnapshot = null;
    }
    
    
    public void showSelectionColumn() {
        jdbcCallsView.showSelectionColumn();
    }
    
    public void refreshSelection() {
        jdbcCallsView.refreshSelection();
    }
    
    
    public void cleanup() {
        if (rm.view == this) rm.view = null;
    }
    
    
    public void profilingSessionStarted() {
        // TODO: register CPUCCTProvider.Listener
    }
    
    public void profilingSessionFinished() {
        // TODO: unregister CPUCCTProvider.Listener
    }
    
    
    protected abstract ProfilerClient getProfilerClient();
    
    
    protected boolean profileMethodSupported() { return true; }
    
    
    protected abstract boolean showSourceSupported();
    
    protected abstract void showSource(ClientUtils.SourceCodeSelection value);
    
    protected abstract void selectForProfiling(ClientUtils.SourceCodeSelection value);
    
    protected void popupShowing() {};
    
    protected void popupHidden() {};
    
    
//    protected void foundInForwardCalls() {}
//    
//    protected void foundInHotSpots() {}
//    
//    protected void foundInReverseCalls() {}
    
    
    private void profileMethod(ClientUtils.SourceCodeSelection value) {
        selectForProfiling(value);
    }
    
    private void profileClass(ClientUtils.SourceCodeSelection value) {
        selectForProfiling(new ClientUtils.SourceCodeSelection(
                           value.getClassName(), Wildcards.ALLWILDCARD, null));
    }
    
    
    private void initUI(Set<ClientUtils.SourceCodeSelection> selection) {
        setLayout(new BorderLayout(0, 0));
        
        jdbcCallsView = new JDBCTreeTableView(selection, false) {
            protected void performDefaultAction(ClientUtils.SourceCodeSelection userValue) {
                if (showSourceSupported()) showSource(userValue);
            }
            protected void populatePopup(JPopupMenu popup, Object value, ClientUtils.SourceCodeSelection userValue) {
                LiveJDBCView.this.populatePopup(jdbcCallsView, popup, value, userValue);
            }
            protected void popupShowing() { LiveJDBCView.this.popupShowing(); }
            protected void popupHidden()  { LiveJDBCView.this.popupHidden(); }
            protected boolean hasBottomFilterFindMargin() { return true; }
        };
        jdbcCallsView.notifyOnFocus(new Runnable() {
            public void run() { lastFocused = jdbcCallsView; }
        });
        
//        JSplitPane upperSplit = new JExtendedSplitPane(JSplitPane.VERTICAL_SPLIT) {
//            {
//                setBorder(null);
//                setDividerSize(5);
//
//                if (getUI() instanceof BasicSplitPaneUI) {
//                    BasicSplitPaneDivider divider = ((BasicSplitPaneUI)getUI()).getDivider();
//                    if (divider != null) {
//                        Color c = UIUtils.isNimbus() || UIUtils.isAquaLookAndFeel() ?
//                                  UIUtils.getDisabledLineColor() : new JSeparator().getForeground();
//                        divider.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, c));
//                    }
//                }
//            }
//        };
//        upperSplit.setBorder(BorderFactory.createEmptyBorder());
//        upperSplit.setTopComponent(jdbcCallsView);
////        upperSplit.setBottomComponent(hotSpotsView);
//        upperSplit.setDividerLocation(0.5d);
//        upperSplit.setResizeWeight(0.5d);
        
        add(jdbcCallsView, BorderLayout.CENTER);
        
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
            if (jdbcCallsView.isShowing()) lastFocused = jdbcCallsView;
//            else if (hotSpotsView.isShowing()) lastFocused = hotSpotsView;
        }
        
        return lastFocused;
    }
    
    private void populatePopup(final DataView invoker, JPopupMenu popup, Object value, final ClientUtils.SourceCodeSelection userValue) {
        if (showSourceSupported()) {
            popup.add(new JMenuItem(JDBCView.ACTION_GOTOSOURCE) {
                { setEnabled(userValue != null); setFont(getFont().deriveFont(Font.BOLD)); }
                protected void fireActionPerformed(ActionEvent e) { showSource(userValue); }
            });
            popup.addSeparator();
        }
        
        popup.add(new JMenuItem(JDBCView.ACTION_PROFILE_METHOD) {
//            { setEnabled(profileMethodSupported() && userValue != null && CPUTableView.isSelectable(userValue)); }
            protected void fireActionPerformed(ActionEvent e) { profileMethod(userValue); }
        });
        
        popup.add(new JMenuItem(JDBCView.ACTION_PROFILE_CLASS) {
            { setEnabled(userValue != null); }
            protected void fireActionPerformed(ActionEvent e) { profileClass(userValue); }
        });
        
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
