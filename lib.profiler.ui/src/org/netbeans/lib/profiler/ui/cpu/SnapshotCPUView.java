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
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.results.cpu.CPUResultsSnapshot;
import org.netbeans.lib.profiler.results.cpu.FlatProfileContainer;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.JExtendedSplitPane;
import org.netbeans.lib.profiler.ui.components.ProfilerToolbar;
import org.netbeans.lib.profiler.ui.results.DataView;
import org.netbeans.lib.profiler.ui.swing.ActionPopupButton;
import org.netbeans.lib.profiler.ui.swing.ExportUtils;
import org.netbeans.lib.profiler.ui.swing.ExportUtils.ExportProvider;
import org.netbeans.lib.profiler.ui.swing.FilterUtils;
import org.netbeans.lib.profiler.ui.swing.GrayLabel;
import org.netbeans.lib.profiler.ui.swing.SearchUtils;
import org.netbeans.lib.profiler.utils.Wildcards;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class SnapshotCPUView extends JPanel {
    
    private final boolean sampled;
    private final CPUResultsSnapshot snapshot;
    
    private int aggregation;
    
    private DataView lastFocused;
    private CPUTableView tableView;
    private CPUTreeTableView treeTableView;
    
    private Component viewContainer;
    
    public SnapshotCPUView(CPUResultsSnapshot snapshot, boolean sampled, Action... actions) {
        this.sampled = sampled;
        this.snapshot = snapshot;
        
        initUI(actions);
        registerActions();
        
        setAggregation(CPUResultsSnapshot.METHOD_LEVEL_VIEW);
    }
    
    
    public ExportUtils.Exportable getExportable(final File sourceFile) {
        return new ExportUtils.Exportable() {
            public String getName() {
                return "Export Methods";
            }
            public ExportUtils.ExportProvider[] getProviders() {
                ExportUtils.ExportProvider[] providers = null;
                ExportProvider npsProvider = sourceFile == null ? null :
                    new ExportUtils.NPSExportProvider(sourceFile);
                
                if (tableView.isVisible() && !treeTableView.isVisible()) {
                    providers = tableView.getExportProviders();
                } else if (!tableView.isVisible() && treeTableView.isVisible()) {
                    providers = treeTableView.getExportProviders();
                }
                
                List<ExportUtils.ExportProvider> _providers = new ArrayList();
                if (npsProvider != null) _providers.add(npsProvider);
                if (providers != null) _providers.addAll(Arrays.asList(providers));
                _providers.add(new ExportUtils.PNGExportProvider(viewContainer));
                return _providers.toArray(new ExportUtils.ExportProvider[_providers.size()]);
            }
        };
    }
    
    
    public abstract boolean showSourceSupported();
    
    public abstract void showSource(ClientUtils.SourceCodeSelection value);
    
    public abstract void selectForProfiling(ClientUtils.SourceCodeSelection value);
    
    
    private void profileMethod(ClientUtils.SourceCodeSelection value) {
        selectForProfiling(value);
    }
    
    private void profileClass(ClientUtils.SourceCodeSelection value) {
        selectForProfiling(new ClientUtils.SourceCodeSelection(
                           value.getClassName(), Wildcards.ALLWILDCARD, null));
    }
    
    
    private void initUI(Action... actions) {
        setLayout(new BorderLayout(0, 0));
        
        treeTableView = new CPUTreeTableView(null) {
            protected void performDefaultAction(ClientUtils.SourceCodeSelection value) {
                if (showSourceSupported()) showSource(value);
            }
            protected void populatePopup(JPopupMenu popup, ClientUtils.SourceCodeSelection value) {
                SnapshotCPUView.this.populatePopup(treeTableView, popup, value);
            }
        };
        treeTableView.notifyOnFocus(new Runnable() {
            public void run() { lastFocused = treeTableView; }
        });
        
        tableView = new CPUTableView(null) {
            protected void performDefaultAction(ClientUtils.SourceCodeSelection value) {
                if (showSourceSupported()) showSource(value);
            }
            protected void populatePopup(JPopupMenu popup, ClientUtils.SourceCodeSelection value) {
                SnapshotCPUView.this.populatePopup(tableView, popup, value);
            }
        };
        tableView.notifyOnFocus(new Runnable() {
            public void run() { lastFocused = tableView; }
        });
        
        JSplitPane split = new JExtendedSplitPane(JSplitPane.VERTICAL_SPLIT) {
            {
                setBorder(null);
                setDividerSize(5);

                if (getUI() instanceof BasicSplitPaneUI) {
                    BasicSplitPaneDivider divider = ((BasicSplitPaneUI)getUI()).getDivider();
                    if (divider != null) {
                        Color c = UIUtils.isNimbus() ? UIUtils.getDisabledLineColor() :
                                new JSeparator().getForeground();
                        divider.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, c));
                    }
                }
            }
        };
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setTopComponent(treeTableView);
        split.setBottomComponent(tableView);
        split.setDividerLocation(0.5d);
        split.setResizeWeight(0.5d);
        
        add(split, BorderLayout.CENTER);
        viewContainer = split;
        
        ProfilerToolbar toolbar = ProfilerToolbar.create(true);
        
        for (int i = 0; i < actions.length - 1; i++) {
            Action action = actions[i];
            if (action != null) {
                toolbar.add(action);
            } else {
                toolbar.addSpace(2);
                toolbar.addSeparator();
                toolbar.addSpace(2);
            }
        }
        
        if (actions.length > 0) {
            toolbar.addSpace(2);
            toolbar.addSeparator();
            toolbar.addSpace(5);
        }
        
        GrayLabel viewL = new GrayLabel("View:");
        toolbar.add(viewL);
        
        toolbar.addSpace(2);
        
        Action aCallTree = new AbstractAction() {
            { putValue(NAME, "Call tree"); }
            public void actionPerformed(ActionEvent e) { setView(true, false); }
            
        };
        Action aHotSpots = new AbstractAction() {
            { putValue(NAME, "Hot spots"); }
            public void actionPerformed(ActionEvent e) { setView(false, true); }
            
        };
        Action aCombined = new AbstractAction() {
            { putValue(NAME, "Combined"); }
            public void actionPerformed(ActionEvent e) { setView(true, true); }
            
        };
        toolbar.add(new ActionPopupButton(2, aCallTree, aHotSpots, aCombined));
        
        toolbar.addSpace(2);
        toolbar.addSeparator();
        toolbar.addSpace(5);
        
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
        
        GrayLabel aggregationL = new GrayLabel("Aggregation:");
        toolbar.add(aggregationL);
        
        toolbar.addSpace(2);
        
        Action aMethods = new AbstractAction() {
            { putValue(NAME, "Methods"); }
            public void actionPerformed(ActionEvent e) { setAggregation(CPUResultsSnapshot.METHOD_LEVEL_VIEW); }
            
        };
        Action aClasses = new AbstractAction() {
            { putValue(NAME, "Classes"); }
            public void actionPerformed(ActionEvent e) { setAggregation(CPUResultsSnapshot.CLASS_LEVEL_VIEW); }
            
        };
        Action aPackages = new AbstractAction() {
            { putValue(NAME, "Packages"); }
            public void actionPerformed(ActionEvent e) { setAggregation(CPUResultsSnapshot.PACKAGE_LEVEL_VIEW); }
            
        };
        
        ActionPopupButton aggregation = new ActionPopupButton(aMethods, aClasses, aPackages);
        toolbar.add(aggregation);
        
        Action aInfo = actions.length > 0 ? actions[actions.length - 1] : null;
        if (aInfo != null) {
            toolbar.addFiller();
            toolbar.add(aInfo);
        }
        
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
            if (treeTableView.isShowing()) lastFocused = treeTableView;
            else if (tableView.isShowing()) lastFocused = tableView;
        }
        
        return lastFocused;
    }
    
    private void populatePopup(final DataView invoker, JPopupMenu popup, final ClientUtils.SourceCodeSelection value) {
        if (showSourceSupported()) {
            popup.add(new JMenuItem("Go to Source") {
                { setEnabled(value != null && aggregation != CPUResultsSnapshot.PACKAGE_LEVEL_VIEW); setFont(getFont().deriveFont(Font.BOLD)); }
                protected void fireActionPerformed(ActionEvent e) { showSource(value); }
            });
            popup.addSeparator();
        }
        
        popup.add(new JMenuItem("Profile Method") {
            { setEnabled(value != null && aggregation == CPUResultsSnapshot.METHOD_LEVEL_VIEW && CPUTableView.isSelectable(value)); }
            protected void fireActionPerformed(ActionEvent e) { profileMethod(value); }
        });
        
        popup.add(new JMenuItem("Profile Class") {
            { setEnabled(value != null && aggregation != CPUResultsSnapshot.PACKAGE_LEVEL_VIEW); }
            protected void fireActionPerformed(ActionEvent e) { profileClass(value); }
        });
        
        popup.addSeparator();
        popup.add(new JMenuItem("Filter") {
            protected void fireActionPerformed(ActionEvent e) { invoker.activateFilter(); }
        });
        popup.add(new JMenuItem("Find") {
            protected void fireActionPerformed(ActionEvent e) { invoker.activateSearch(); }
        });
        
    }
    
    private void setView(boolean callTree, boolean hotSpots) {
        treeTableView.setVisible(callTree);
        tableView.setVisible(hotSpots);
    }
    
    private void setAggregation(int _aggregation) {
        aggregation = _aggregation;
        
        final FlatProfileContainer flatData = snapshot.getFlatProfile(-1, aggregation);

        final Map<Integer, ClientUtils.SourceCodeSelection> idMap = new HashMap();
        for (int i = 0; i < flatData.getNRows(); i++) // TODO: getNRows is filtered, may not work for tree data!
            idMap.put(flatData.getMethodIdAtRow(i), flatData.getSourceCodeSelectionAtRow(i));
//        SwingUtilities.invokeLater(new Runnable() {
//
//            @Override
//            public void run() {
//        treeTableView.setData(snapshot, idMap, aggregation, sampled);
//        tableView.setData(flatData, idMap, sampled);
//            }
//        });
        
        treeTableView.setData(snapshot, idMap, aggregation, sampled);
        tableView.setData(flatData, idMap, sampled);
    }
    
}
