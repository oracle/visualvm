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
import java.text.MessageFormat;
import java.util.ResourceBundle;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.Icon;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.results.jdbc.JdbcResultsDiff;
import org.netbeans.lib.profiler.results.jdbc.JdbcResultsSnapshot;
import org.netbeans.lib.profiler.results.memory.PresoObjAllocCCTNode;
import org.netbeans.lib.profiler.ui.components.ProfilerToolbar;
import org.netbeans.lib.profiler.ui.results.DataView;
import org.netbeans.lib.profiler.ui.swing.ExportUtils;
import org.netbeans.lib.profiler.ui.swing.FilterUtils;
import org.netbeans.lib.profiler.ui.swing.ProfilerTable;
import org.netbeans.lib.profiler.ui.swing.SearchUtils;
import org.netbeans.lib.profiler.utils.Wildcards;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class SnapshotJDBCView extends JPanel {
    
    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.netbeans.lib.profiler.ui.cpu.Bundle"); // NOI18N
//    private static final String TOOLBAR_VIEW = messages.getString("SnapshotCPUView_ToolbarView"); // NOI18N
//    private static final String VIEW_FORWARD = messages.getString("SnapshotCPUView_ViewForward"); // NOI18N
//    private static final String VIEW_HOTSPOTS = messages.getString("SnapshotCPUView_ViewHotSpots"); // NOI18N
//    private static final String VIEW_REVERSE = messages.getString("SnapshotCPUView_ViewReverse"); // NOI18N
//    private static final String TOOLBAR_AGGREGATION = messages.getString("SnapshotCPUView_ToolbarAggregation"); // NOI18N
//    private static final String AGGREGATION_METHODS = messages.getString("SnapshotCPUView_AggregationMethods"); // NOI18N
//    private static final String AGGREGATION_CLASSES = messages.getString("SnapshotCPUView_AggregationClasses"); // NOI18N
//    private static final String AGGREGATION_PACKAGES = messages.getString("SnapshotCPUView_AggregationPackages"); // NOI18N
    private static final String COMPARE_SNAPSHOTS = messages.getString("SnapshotCPUView_CompareSnapshots"); // NOI18N
    private static final String RESET_COMPARE_SNAPSHOTS = messages.getString("SnapshotCPUView_ResetCompareSnapshots"); // NOI18N
    // -----
    
//    private boolean sampled;
    private JdbcResultsSnapshot snapshot;
    private JdbcResultsSnapshot refSnapshot;
    
//    private int aggregation;
//    private boolean mergedThreads;
//    private Collection<Integer> selectedThreads;
    
    private DataView lastFocused;
//    private CPUTableView hotSpotsView;
    private JDBCTreeTableView forwardCallsView;
//    private CPUTreeTableView reverseCallsView;
    
//    private JToggleButton[] toggles;
    private JToggleButton compareButton;
    
    
    public SnapshotJDBCView(JdbcResultsSnapshot snapshot, Action saveAction, Action compareAction, Action infoAction, ExportUtils.Exportable exportProvider) {
        initUI(saveAction, compareAction, infoAction, exportProvider);
        registerActions();
        
//        aggregation = CPUResultsSnapshot.METHOD_LEVEL_VIEW;
        setSnapshot(snapshot);
    }
    
    
    public void setRefSnapshot(JdbcResultsSnapshot snapshot) {
        refSnapshot = snapshot;
        
        if (compareButton != null && snapshot != null) {
            compareButton.setSelected(true);
            compareButton.setToolTipText(RESET_COMPARE_SNAPSHOTS);
        }
        
        setData();
    }
    
    
    protected abstract boolean showSourceSupported();
    
    protected abstract void showSource(ClientUtils.SourceCodeSelection value);
    
    protected abstract void showSQLQuery(String query, String htmlQuery);
    
    protected abstract void selectForProfiling(ClientUtils.SourceCodeSelection value);
    
    
//    protected void foundInForwardCalls() {
//        toggles[0].setSelected(true);
//    }
//    protected void foundInHotSpots() {
//        toggles[1].setSelected(true);
//    }
//    protected void foundInReverseCalls() {
//        toggles[2].setSelected(true);
//    }
    
    
    private void profileMethod(ClientUtils.SourceCodeSelection value) {
        selectForProfiling(value);
    }
    
    private void profileClass(ClientUtils.SourceCodeSelection value) {
        selectForProfiling(new ClientUtils.SourceCodeSelection(
                           value.getClassName(), Wildcards.ALLWILDCARD, null));
    }
    
    
    private void initUI(Action saveAction, final Action compareAction, Action infoAction, ExportUtils.Exportable exportProvider) {
        setLayout(new BorderLayout(0, 0));
        
        forwardCallsView = new JDBCTreeTableView(null, false) {
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
            protected void performDefaultAction(ClientUtils.SourceCodeSelection value) {
                if (showSourceSupported()) showSource(value);
            }
            protected void populatePopup(JPopupMenu popup, Object value, ClientUtils.SourceCodeSelection userValue) {
                SnapshotJDBCView.this.populatePopup(forwardCallsView, popup, value, userValue);
            }
        };
        forwardCallsView.notifyOnFocus(new Runnable() {
            public void run() { lastFocused = forwardCallsView; }
        });
        
//        hotSpotsView = new CPUTableView(null) {
//            protected void performDefaultAction(ClientUtils.SourceCodeSelection userValue) {
//                if (showSourceSupported()) showSource(userValue);
//            }
//            protected void populatePopup(JPopupMenu popup, Object value, ClientUtils.SourceCodeSelection userValue) {
//                SnapshotCPUView.this.populatePopup(hotSpotsView, popup, value, userValue);
//            }
//        };
//        hotSpotsView.notifyOnFocus(new Runnable() {
//            public void run() { lastFocused = hotSpotsView; }
//        });
//        
//        reverseCallsView = new CPUTreeTableView(null, true) {
//            protected void performDefaultAction(ClientUtils.SourceCodeSelection value) {
//                if (showSourceSupported()) showSource(value);
//            }
//            protected void populatePopup(JPopupMenu popup, Object value, ClientUtils.SourceCodeSelection userValue) {
//                SnapshotCPUView.this.populatePopup(reverseCallsView, popup, value, userValue);
//            }
//        };
//        reverseCallsView.notifyOnFocus(new Runnable() {
//            public void run() { lastFocused = reverseCallsView; }
//        });
        
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
//                        divider.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, c));
//                    }
//                }
//            }
//        };
//        upperSplit.setBorder(BorderFactory.createEmptyBorder());
//        upperSplit.setTopComponent(forwardCallsView);
//        upperSplit.setBottomComponent(hotSpotsView);
//        upperSplit.setDividerLocation(0.5d);
//        upperSplit.setResizeWeight(0.5d);
        
        add(forwardCallsView, BorderLayout.CENTER);
        
        ProfilerToolbar toolbar = ProfilerToolbar.create(true);
        
        if (saveAction != null) toolbar.add(saveAction);
        
        toolbar.add(ExportUtils.exportButton(this, JDBCView.EXPORT_TOOLTIP, getExportables(exportProvider)));
        
        if (compareAction != null) {
            toolbar.addSpace(2);
            toolbar.addSeparator();
            toolbar.addSpace(2);
        
            Icon icon = (Icon)compareAction.getValue(Action.SMALL_ICON);
            compareButton = new JToggleButton(icon) {
                protected void fireActionPerformed(ActionEvent e) {
                    boolean sel = isSelected();
                    if (sel) {
                        compareAction.actionPerformed(e);
                        if (refSnapshot == null) setSelected(false);
                    } else {
                        setRefSnapshot(null);
                    }
                    setToolTipText(isSelected() ? RESET_COMPARE_SNAPSHOTS :
                                                  COMPARE_SNAPSHOTS);
                }
            };
            compareButton.setToolTipText(COMPARE_SNAPSHOTS);
            toolbar.add(compareButton);
        }
        
//        toolbar.addSpace(2);
//        toolbar.addSeparator();
//        toolbar.addSpace(5);
//        
//        GrayLabel viewL = new GrayLabel(TOOLBAR_VIEW);
//        toolbar.add(viewL);
        
//        toolbar.addSpace(5);
//        
//        MultiButtonGroup group = new MultiButtonGroup();
//        toggles = new JToggleButton[3];
//        
//        toggles[0] = new JToggleButton(Icons.getIcon(ProfilerIcons.NODE_FORWARD)) {
//            protected void fireActionPerformed(ActionEvent e) {
//                super.fireActionPerformed(e);
//                setView(isSelected(), hotSpotsView.isVisible(), reverseCallsView.isVisible());
//            }
//        };
//        toggles[0].setToolTipText(VIEW_FORWARD);
//        group.add(toggles[0]);
//        toolbar.add(toggles[0]);
//        forwardCallsView.setVisible(true);
//        toggles[0].setSelected(true);
//        
//        toggles[1] = new JToggleButton(Icons.getIcon(ProfilerIcons.TAB_HOTSPOTS)) {
//            protected void fireActionPerformed(ActionEvent e) {
//                super.fireActionPerformed(e);
//                setView(forwardCallsView.isVisible(), isSelected(), reverseCallsView.isVisible());
//            }
//        };
//        toggles[1].setToolTipText(VIEW_HOTSPOTS);
//        group.add(toggles[1]);
//        toolbar.add(toggles[1]);
//        hotSpotsView.setVisible(false);
//        toggles[1].setSelected(false);
//        
//        toggles[2] = new JToggleButton(Icons.getIcon(ProfilerIcons.NODE_REVERSE)) {
//            protected void fireActionPerformed(ActionEvent e) {
//                super.fireActionPerformed(e);
//                setView(forwardCallsView.isVisible(), hotSpotsView.isVisible(), isSelected());
//            }
//        };
//        toggles[2].setToolTipText(VIEW_REVERSE);
//        group.add(toggles[2]);
//        toolbar.add(toggles[2]);
//        reverseCallsView.setVisible(false);
//        toggles[2].setSelected(false);
        
//        Action aCallTree = new AbstractAction() {
//            { putValue(NAME, VIEW_CALLTREE); }
//            public void actionPerformed(ActionEvent e) { setView(true, false); }
//            
//        };
//        Action aHotSpots = new AbstractAction() {
//            { putValue(NAME, VIEW_HOTSPOTS); }
//            public void actionPerformed(ActionEvent e) { setView(false, true); }
//            
//        };
//        Action aCombined = new AbstractAction() {
//            { putValue(NAME, VIEW_COMBINED); }
//            public void actionPerformed(ActionEvent e) { setView(true, true); }
//            
//        };
//        toolbar.add(new ActionPopupButton(2, aCallTree, aHotSpots, aCombined));
        
//        toolbar.addSpace(5);
//        ThreadsSelector threadsPopup = new ThreadsSelector() {
//            protected CPUResultsSnapshot getSnapshot() { return snapshot; }
//            protected void selectionChanged(Collection<Integer> selected, boolean mergeThreads) {
//                mergedThreads = mergeThreads;
//                selectedThreads = selected;
//                setAggregation(aggregation);
//            }
//            
//        };
//        toolbar.add(threadsPopup);
        
//        toolbar.addSpace(2);
//        toolbar.addSeparator();
//        toolbar.addSpace(5);
        
//        GrayLabel threadsL = new GrayLabel("Threads:");
//        toolbar.add(threadsL);
//        
//        toolbar.addSpace(2);
//        
//        PopupButton threads = new PopupButton("All threads") {
//            protected void populatePopup(JPopupMenu popup) {
//                popup.add(new JRadioButtonMenuItem("All threads"));
//                popup.add(new JRadioButtonMenuItem("main"));
//                popup.add(new JRadioButtonMenuItem("AWT-EventQueue-0"));
//            }
//        };
//        toolbar.add(threads);
//        
//        toolbar.addSpace(2);
//        toolbar.addSeparator();
//        toolbar.addSpace(5);
        
//        GrayLabel aggregationL = new GrayLabel(TOOLBAR_AGGREGATION);
//        toolbar.add(aggregationL);
//        
//        toolbar.addSpace(2);
        
//        Action aMethods = new AbstractAction() {
//            { putValue(NAME, AGGREGATION_METHODS); }
//            public void actionPerformed(ActionEvent e) { setAggregation(CPUResultsSnapshot.METHOD_LEVEL_VIEW); }
//            
//        };
//        Action aClasses = new AbstractAction() {
//            { putValue(NAME, AGGREGATION_CLASSES); }
//            public void actionPerformed(ActionEvent e) { setAggregation(CPUResultsSnapshot.CLASS_LEVEL_VIEW); }
//            
//        };
//        Action aPackages = new AbstractAction() {
//            { putValue(NAME, AGGREGATION_PACKAGES); }
//            public void actionPerformed(ActionEvent e) { setAggregation(CPUResultsSnapshot.PACKAGE_LEVEL_VIEW); }
//            
//        };
//        
//        ActionPopupButton aggregation = new ActionPopupButton(aMethods, aClasses, aPackages);
//        toolbar.add(aggregation);
//        
        if (infoAction != null) {
            toolbar.addFiller();
            toolbar.add(infoAction);
        }
//        
        add(toolbar.getComponent(), BorderLayout.NORTH);
        
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
        
        popup.add(new JMenuItem(JDBCView.ACTION_PROFILE_METHOD) {
            { setEnabled(userValue != null && JDBCTreeTableView.isSelectable(node)); }
            protected void fireActionPerformed(ActionEvent e) { profileMethod(userValue); }
        });
        
        popup.add(new JMenuItem(JDBCView.ACTION_PROFILE_CLASS) {
            { setEnabled(userValue != null); }
            protected void fireActionPerformed(ActionEvent e) { profileClass(userValue); }
        });
        
        customizeNodePopup(invoker, popup, value, userValue);
        
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
    
    private void setData() {
        JdbcResultsSnapshot _snapshot = refSnapshot == null ? snapshot :
                                       snapshot.createDiff(refSnapshot);
        
//        final FlatProfileContainer flatData = _snapshot.getFlatProfile(selectedThreads, aggregation);
//        
//        final Map<Integer, ClientUtils.SourceCodeSelection> idMap = _snapshot.getMethodIDMap(aggregation);

        boolean diff = _snapshot instanceof JdbcResultsDiff;
//        forwardCallsView.setData(_snapshot, idMap, aggregation, selectedThreads, mergedThreads, sampled, diff);
        forwardCallsView.setData(_snapshot, null, -1, null, false, false, diff);
    }
    
    protected final void setSnapshot(JdbcResultsSnapshot snapshot) {
        this.snapshot = snapshot;
        setData();
    }
    
    private ExportUtils.Exportable[] getExportables(final ExportUtils.Exportable snapshotExporter) {
        return new ExportUtils.Exportable[] {
            new ExportUtils.Exportable() {
                public boolean isEnabled() {
                    return refSnapshot == null && snapshotExporter.isEnabled();
                }
                public String getName() {
                    return snapshotExporter.getName();
                }
                public ExportUtils.ExportProvider[] getProviders() {
                    return snapshotExporter.getProviders();
                }
            },
            new ExportUtils.Exportable() {
                public boolean isEnabled() {
                    return forwardCallsView.isVisible();
                }
                public String getName() {
                    return MessageFormat.format(JDBCView.EXPORT_METHODS, JDBCView.EXPORT_FORWARD_CALLS);
                }
                public ExportUtils.ExportProvider[] getProviders() {
                    return forwardCallsView.getExportProviders();
                }
//            },
//            new ExportUtils.Exportable() {
//                public boolean isEnabled() {
//                    return hotSpotsView.isVisible();
//                }
//                public String getName() {
//                    return MessageFormat.format(CPUView.EXPORT_METHODS, CPUView.EXPORT_HOTSPOTS);
//                }
//                public ExportUtils.ExportProvider[] getProviders() {
//                    return hotSpotsView.getExportProviders();
//                }
//            },
//            new ExportUtils.Exportable() {
//                public boolean isEnabled() {
//                    return reverseCallsView.isVisible();
//                }
//                public String getName() {
//                    return MessageFormat.format(CPUView.EXPORT_METHODS, CPUView.EXPORT_REVERSE_CALLS);
//                }
//                public ExportUtils.ExportProvider[] getProviders() {
//                    return reverseCallsView.getExportProviders();
//                }
            }
        };
    }
    
}
