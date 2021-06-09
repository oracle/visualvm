/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.ui.jdbc;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.lib.jfluid.ProfilerClient;
import org.graalvm.visualvm.lib.jfluid.client.ClientUtils;
import org.graalvm.visualvm.lib.jfluid.results.jdbc.JdbcResultsDiff;
import org.graalvm.visualvm.lib.jfluid.results.jdbc.JdbcResultsSnapshot;
import org.graalvm.visualvm.lib.jfluid.results.memory.PresoObjAllocCCTNode;
import org.graalvm.visualvm.lib.ui.UIUtils;
import org.graalvm.visualvm.lib.ui.results.DataView;
import org.graalvm.visualvm.lib.ui.swing.FilterUtils;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTable;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTreeTable;
import org.graalvm.visualvm.lib.ui.swing.SearchUtils;
import org.graalvm.visualvm.lib.jfluid.utils.Wildcards;
import org.graalvm.visualvm.lib.ui.swing.renderer.HideableBarRenderer;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class LiveJDBCView extends JPanel {
    
    private JdbcResultsSnapshot snapshot;
    private JdbcResultsSnapshot refSnapshot;
    
    private DataView lastFocused;
    private JDBCTreeTableView jdbcCallsView;
    
    private long lastupdate;
    private volatile boolean refreshIsRunning;
    
    private ExecutorService executor;
    
    
    public LiveJDBCView(Set<ClientUtils.SourceCodeSelection> selection) {
        initUI(selection);
        registerActions();        
    }
    
    
    public void setView(boolean forwardCalls, boolean hotSpots, boolean reverseCalls) {
        jdbcCallsView.setVisible(forwardCalls);
    }
    
    public boolean isRefreshRunning() {
        return refreshIsRunning;
    }
    
    public long getLastUpdate() {
        return lastupdate;
    }
    
    public void setData(final JdbcResultsSnapshot snapshotData) {
        if (refreshIsRunning) return;
        refreshIsRunning = true;
        
        UIUtils.runInEventDispatchThread(new Runnable() {
            public void run() {
                snapshot = snapshotData;
                
                setData();
            }
        });
    }
    
    private void setData() {
        if (snapshot == null) {
            resetData();
            refreshIsRunning = false;
        } else {
            getExecutor().submit(new Runnable() {
                public void run() {
                    final JdbcResultsSnapshot _snapshot = refSnapshot == null ? snapshot :
                                                         refSnapshot.createDiff(snapshot);

                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            try {
                                boolean diff = _snapshot instanceof JdbcResultsDiff;
                                jdbcCallsView.setData(_snapshot, null, -1, null, false, false, diff);
                            } finally {
                                refreshIsRunning = false;
                                lastupdate = System.currentTimeMillis();
                            }
                        }
                    });
                }
            });
        }
    }
    
    public boolean setDiffView(final boolean diff) {
        if (snapshot == null) return false;
        UIUtils.runInEventDispatchThread(new Runnable() {
            public void run() {
                refSnapshot = diff ? snapshot : null;
                setData();
            }
        });
        return true;
    }
    
    public void resetData() {
        UIUtils.runInEventDispatchThread(new Runnable() {
            public void run() {
                jdbcCallsView.resetData();
                snapshot = null;
                refSnapshot = null;
            }
        });
    }
    
    
    public void showSelectionColumn() {
        jdbcCallsView.showSelectionColumn();
    }
    
    public void refreshSelection() {
        jdbcCallsView.refreshSelection();
    }
    
    
    public void cleanup() {
    }
    
    
    protected abstract ProfilerClient getProfilerClient();
    
    
    protected boolean profileMethodSupported() { return true; }
    
    protected boolean profileClassSupported() { return true; }
    
    
    protected abstract boolean showSourceSupported();
    
    protected abstract void showSource(ClientUtils.SourceCodeSelection value);
    
    protected abstract void showSQLQuery(String query, String htmlQuery);
    
    protected abstract void selectForProfiling(ClientUtils.SourceCodeSelection value);
    
    protected void popupShowing() {};
    
    protected void popupHidden() {};
    
    
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
            protected void installDefaultAction() {
                getResultsComponent().setDefaultAction(new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        ProfilerTable t = getResultsComponent();
                        int row = t.getSelectedRow();
                        PresoObjAllocCCTNode node = (PresoObjAllocCCTNode)t.getValueForRow(row);
                        if (isSQL(node)) {
                            showQueryImpl(node);
                        } else {
                            ClientUtils.SourceCodeSelection userValue = getUserValueForRow(row);
                            if (userValue != null) performDefaultAction(userValue);
                        }
                    }
                });
            }
            protected void performDefaultAction(ClientUtils.SourceCodeSelection userValue) {
                if (showSourceSupported()) showSource(userValue);
            }
            protected void populatePopup(JPopupMenu popup, Object value, ClientUtils.SourceCodeSelection userValue) {
                LiveJDBCView.this.populatePopup(jdbcCallsView, popup, value, userValue);
            }
            protected void popupShowing() { LiveJDBCView.this.popupShowing(); }
            protected void popupHidden()  { LiveJDBCView.this.popupHidden(); }
            protected boolean hasBottomFilterFindMargin() { return true; }
            HideableBarRenderer.BarDiffMode barDiffMode() { return HideableBarRenderer.BarDiffMode.MODE_BAR_NORMAL; }
        };
        jdbcCallsView.notifyOnFocus(new Runnable() {
            public void run() { lastFocused = jdbcCallsView; }
        });
        
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
        }
        
        return lastFocused;
    }
    
    private void showQueryImpl(PresoObjAllocCCTNode node) {
        showSQLQuery(node.getNodeName(), ((JDBCTreeTableView.SQLQueryNode)node).htmlName);
    }
    
    private void populatePopup(final DataView invoker, JPopupMenu popup, final Object value, final ClientUtils.SourceCodeSelection userValue) {
        final PresoObjAllocCCTNode node = (PresoObjAllocCCTNode)value;
        if (JDBCTreeTableView.isSQL(node)) {
            popup.add(new JMenuItem(JDBCView.ACTION_VIEWSQLQUERY) {
                { setFont(getFont().deriveFont(Font.BOLD)); }
                protected void fireActionPerformed(ActionEvent e) { showQueryImpl((JDBCTreeTableView.SQLQueryNode)value); }
            });
            popup.addSeparator();
        } else if (showSourceSupported()) {
            popup.add(new JMenuItem(JDBCView.ACTION_GOTOSOURCE) {
                { setEnabled(userValue != null); setFont(getFont().deriveFont(Font.BOLD)); }
                protected void fireActionPerformed(ActionEvent e) { showSource(userValue); }
            });
            popup.addSeparator();
        }
        
        if (profileMethodSupported()) popup.add(new JMenuItem(JDBCView.ACTION_PROFILE_METHOD) {
            { setEnabled(userValue != null && JDBCTreeTableView.isSelectable(node)); }
            protected void fireActionPerformed(ActionEvent e) { profileMethod(userValue); }
        });
        
        if (profileClassSupported()) popup.add(new JMenuItem(JDBCView.ACTION_PROFILE_CLASS) {
            { setEnabled(userValue != null); }
            protected void fireActionPerformed(ActionEvent e) { profileClass(userValue); }
        });
        
        if (profileMethodSupported() || profileClassSupported()) popup.addSeparator();
        
        JMenuItem[] customItems = invoker.createCustomMenuItems(this, value, userValue);
        if (customItems != null) {
            for (JMenuItem customItem : customItems) popup.add(customItem);
            popup.addSeparator();
        }
        
        customizeNodePopup(invoker, popup, value, userValue);
        
        final ProfilerTreeTable ttable = (ProfilerTreeTable)jdbcCallsView.getResultsComponent();
        JMenu expand = new JMenu(JDBCView.EXPAND_MENU);
        popup.add(expand);

        expand.add(new JMenuItem(JDBCView.EXPAND_PLAIN_ITEM) {
            protected void fireActionPerformed(ActionEvent e) {
                ttable.expandPlainPath(ttable.getSelectedRow(), 1);
            }
        });

        expand.add(new JMenuItem(JDBCView.EXPAND_TOPMOST_ITEM) {
            protected void fireActionPerformed(ActionEvent e) {
                ttable.expandFirstPath(ttable.getSelectedRow());
            }
        });
        
        expand.addSeparator();
            
        expand.add(new JMenuItem(JDBCView.COLLAPSE_CHILDREN_ITEM) {
            protected void fireActionPerformed(ActionEvent e) {
                ttable.collapseChildren(ttable.getSelectedRow());
            }
        });

        expand.add(new JMenuItem(JDBCView.COLLAPSE_ALL_ITEM) {
            protected void fireActionPerformed(ActionEvent e) {
                ttable.collapseAll();
            }
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
    
    protected void customizeNodePopup(DataView invoker, JPopupMenu popup, Object value, ClientUtils.SourceCodeSelection userValue) {}
    
    
    private synchronized ExecutorService getExecutor() {
        if (executor == null) executor = Executors.newSingleThreadExecutor();
        return executor;
    }
    
}
