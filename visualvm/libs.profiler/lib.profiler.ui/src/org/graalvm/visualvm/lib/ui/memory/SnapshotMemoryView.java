/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.ui.memory;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.ResourceBundle;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import org.graalvm.visualvm.lib.jfluid.client.ClientUtils;
import org.graalvm.visualvm.lib.jfluid.filters.GenericFilter;
import org.graalvm.visualvm.lib.jfluid.results.cpu.CPUResultsSnapshot;
import org.graalvm.visualvm.lib.jfluid.results.memory.AllocMemoryResultsSnapshot;
import org.graalvm.visualvm.lib.jfluid.results.memory.LivenessMemoryResultsSnapshot;
import org.graalvm.visualvm.lib.jfluid.results.memory.MemoryResultsSnapshot;
import org.graalvm.visualvm.lib.jfluid.results.memory.PresoObjAllocCCTNode;
import org.graalvm.visualvm.lib.jfluid.results.memory.SampledMemoryResultsSnapshot;
import org.graalvm.visualvm.lib.jfluid.utils.Wildcards;
import org.graalvm.visualvm.lib.ui.components.ProfilerToolbar;
import org.graalvm.visualvm.lib.ui.results.DataView;
import org.graalvm.visualvm.lib.ui.swing.ExportUtils;
import org.graalvm.visualvm.lib.ui.swing.FilterUtils;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTreeTable;
import org.graalvm.visualvm.lib.ui.swing.SearchUtils;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class SnapshotMemoryView extends JPanel {

    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.graalvm.visualvm.lib.ui.memory.Bundle"); // NOI18N
    private static final String COMPARE_SNAPSHOTS = messages.getString("SnapshotMemoryView_CompareSnapshots"); // NOI18N
    private static final String RESET_COMPARE_SNAPSHOTS = messages.getString("SnapshotMemoryView_ResetCompareSnapshots"); // NOI18N
//    private static final String TOOLBAR_AGGREGATION = messages.getString("SnapshotMemoryView_ToolbarAggregation"); // NOI18N
//    private static final String AGGREGATION_CLASSES = messages.getString("SnapshotMemoryView_AggregationClasses"); // NOI18N
//    private static final String AGGREGATION_PACKAGES = messages.getString("SnapshotMemoryView_AggregationPackages"); // NOI18N
    // -----
    
    private final MemoryView dataView;
    
    private int aggregation;
    private final GenericFilter filter;
    private final MemoryResultsSnapshot snapshot;
    private MemoryResultsSnapshot refSnapshot;
    
    private JToggleButton compareButton;
    
    
    public SnapshotMemoryView(MemoryResultsSnapshot snapshot, GenericFilter filter, Action saveAction, final Action compareAction, Action infoAction, ExportUtils.Exportable exportProvider) {
        this.filter = filter;
        this.snapshot = snapshot;
        
        // class names in VM format
        MemoryView.userFormClassNames(snapshot);
        
        setOpaque(false);
        setLayout(new BorderLayout());
        
//        boolean supportsPackageAggregation = true;
        
        if (snapshot instanceof SampledMemoryResultsSnapshot) {
            dataView = new SampledTableView(null) {
                protected void performDefaultAction(ClientUtils.SourceCodeSelection userValue) {
                    if (showSourceSupported()) showSource(userValue);
                }
                protected void populatePopup(JPopupMenu popup, Object value, ClientUtils.SourceCodeSelection userValue) {
                    SnapshotMemoryView.this.populatePopup(this, popup, value, userValue);
                }
            };
        } else if (snapshot instanceof AllocMemoryResultsSnapshot) {
            if (snapshot.containsStacks()) {
                dataView = new AllocTreeTableView(null) {
                    protected void performDefaultAction(ClientUtils.SourceCodeSelection userValue) {
                        if (showSourceSupported()) showSource(userValue);
                    }
                    protected void populatePopup(JPopupMenu popup, Object value, ClientUtils.SourceCodeSelection userValue) {
                        SnapshotMemoryView.this.populatePopup(this, popup, value, userValue);
                    }
                };
//                supportsPackageAggregation = false;
            } else {
                dataView = new AllocTableView(null) {
                    protected void performDefaultAction(ClientUtils.SourceCodeSelection userValue) {
                        if (showSourceSupported()) showSource(userValue);
                    }
                    protected void populatePopup(JPopupMenu popup, Object value, ClientUtils.SourceCodeSelection userValue) {
                        SnapshotMemoryView.this.populatePopup(this, popup, value, userValue);
                    }
                };
            }
        } else if (snapshot instanceof LivenessMemoryResultsSnapshot) {
            if (snapshot.containsStacks()) {
                dataView = new LivenessTreeTableView(null, filter == null) {
                    protected void performDefaultAction(ClientUtils.SourceCodeSelection userValue) {
                        if (showSourceSupported()) showSource(userValue);
                    }
                    protected void populatePopup(JPopupMenu popup, Object value, ClientUtils.SourceCodeSelection userValue) {
                        SnapshotMemoryView.this.populatePopup(this, popup, value, userValue);
                    }
                };
//                supportsPackageAggregation = false;
            } else {
                dataView = new LivenessTableView(null, filter == null) {
                    protected void performDefaultAction(ClientUtils.SourceCodeSelection userValue) {
                        if (showSourceSupported()) showSource(userValue);
                    }
                    protected void populatePopup(JPopupMenu popup, Object value, ClientUtils.SourceCodeSelection userValue) {
                        SnapshotMemoryView.this.populatePopup(this, popup, value, userValue);
                    }
                };
            }
        } else {
            dataView = null;
        }
        
        ProfilerToolbar toolbar = ProfilerToolbar.create(true);
        
        if (saveAction != null) toolbar.add(saveAction);
        
        toolbar.add(ExportUtils.exportButton(this, MemoryView.EXPORT_TOOLTIP, getExportables(exportProvider)));
        
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
        
//        GrayLabel aggregationL = new GrayLabel(TOOLBAR_AGGREGATION);
//        toolbar.add(aggregationL);
//        
//        toolbar.addSpace(2);
//        
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
//        ActionPopupButton aggregation = new ActionPopupButton(0, aClasses, aPackages);
//        aggregation.setEnabled(supportsPackageAggregation);
//        toolbar.add(aggregation);
        
        if (infoAction != null) {
            toolbar.addFiller();
            toolbar.add(infoAction);
        }
        
        if (dataView != null) add(dataView, BorderLayout.CENTER);
        add(toolbar.getComponent(), BorderLayout.NORTH);
        
        setAggregation(CPUResultsSnapshot.CLASS_LEVEL_VIEW);
        
        registerActions();
    }
    
    private void registerActions() {
        ActionMap map = getActionMap();
        
        map.put(FilterUtils.FILTER_ACTION_KEY, new AbstractAction() {
            public void actionPerformed(ActionEvent e) { dataView.activateFilter(); }
        });
        
        map.put(SearchUtils.FIND_ACTION_KEY, new AbstractAction() {
            public void actionPerformed(ActionEvent e) { dataView.activateSearch(); }
        });
    }
    
    
    public void setRefSnapshot(MemoryResultsSnapshot snapshot) {
        // class names in VM format
        MemoryView.userFormClassNames(snapshot);
        
        refSnapshot = snapshot;
        if (compareButton != null && snapshot != null) {
            compareButton.setSelected(true);
            compareButton.setToolTipText(RESET_COMPARE_SNAPSHOTS);
        }
        
        setAggregation(aggregation);
    }
    
    
    protected boolean profileMethodSupported() { return true; }
    
    protected boolean profileClassSupported() { return true; }
    
    
    protected abstract boolean showSourceSupported();
    
    protected abstract void showSource(ClientUtils.SourceCodeSelection value);
    
    protected abstract void selectForProfiling(ClientUtils.SourceCodeSelection value);
    
    
    private void profileMethod(ClientUtils.SourceCodeSelection value) {
        selectForProfiling(value);
    }
    
    private void profileClass(ClientUtils.SourceCodeSelection value) {
        selectForProfiling(new ClientUtils.SourceCodeSelection(
                           value.getClassName(), Wildcards.ALLWILDCARD, null));
    }
    
    
    // TODO: implement isSelectable()
//    // Check if primitive type/array
//    if ((methodName == null && methodSig == null) && (VMUtils.isVMPrimitiveType(className) ||
//         VMUtils.isPrimitiveType(className))) ProfilerDialogs.displayWarning(CANNOT_SHOW_PRIMITIVE_SRC_MSG);
    static boolean isSelectable(ClientUtils.SourceCodeSelection value, boolean method) {
        String className = value.getClassName();
        String methodName = value.getMethodName();
        
        if (method && methodName.endsWith("[native]")) return false; // NOI18N
        
        if (PresoObjAllocCCTNode.VM_ALLOC_CLASS.equals(className) && PresoObjAllocCCTNode.VM_ALLOC_METHOD.equals(methodName)) return false;
        
        return true;
    }
    
    private void populatePopup(final DataView invoker, JPopupMenu popup, Object value, final ClientUtils.SourceCodeSelection userValue) {
        if (showSourceSupported()) {
            popup.add(new JMenuItem(MemoryView.ACTION_GOTOSOURCE) {
                { setEnabled(userValue != null && aggregation != CPUResultsSnapshot.PACKAGE_LEVEL_VIEW); setFont(getFont().deriveFont(Font.BOLD)); }
                protected void fireActionPerformed(ActionEvent e) { showSource(userValue); }
            });
            popup.addSeparator();
        }
        
        if (profileMethodSupported()) {
            if (userValue == null || !Wildcards.ALLWILDCARD.equals(userValue.getMethodName())) {
                popup.add(new JMenuItem(MemoryView.ACTION_PROFILE_METHOD) {
                    { setEnabled(userValue != null && isSelectable(userValue, true)); }
                    protected void fireActionPerformed(ActionEvent e) { profileMethod(userValue); }
                });
            }
        }
        
        if (profileClassSupported()) popup.add(new JMenuItem(MemoryView.ACTION_PROFILE_CLASS) {
            { setEnabled(userValue != null && aggregation != CPUResultsSnapshot.PACKAGE_LEVEL_VIEW && isSelectable(userValue, false)); }
            protected void fireActionPerformed(ActionEvent e) { profileClass(userValue); }
        });
        
        if (profileMethodSupported() || profileClassSupported()) popup.addSeparator();
        
        JMenuItem[] customItems = invoker.createCustomMenuItems(this, value, userValue);
        if (customItems != null) {
            for (JMenuItem customItem : customItems) popup.add(customItem);
            popup.addSeparator();
        }
        
        customizeNodePopup(invoker, popup, value, userValue);
        
        if (snapshot.containsStacks()) {
            final ProfilerTreeTable ttable = (ProfilerTreeTable)dataView.getResultsComponent();
            JMenu expand = new JMenu(MemoryView.EXPAND_MENU);
            popup.add(expand);

            expand.add(new JMenuItem(MemoryView.EXPAND_PLAIN_ITEM) {
                protected void fireActionPerformed(ActionEvent e) {
                    ttable.expandPlainPath(ttable.getSelectedRow(), 1);
                }
            });

            expand.add(new JMenuItem(MemoryView.EXPAND_TOPMOST_ITEM) {
                protected void fireActionPerformed(ActionEvent e) {
                    ttable.expandFirstPath(ttable.getSelectedRow());
                }
            });
            
            expand.addSeparator();
            
            expand.add(new JMenuItem(MemoryView.COLLAPSE_CHILDREN_ITEM) {
                protected void fireActionPerformed(ActionEvent e) {
                    ttable.collapseChildren(ttable.getSelectedRow());
                }
            });
            
            expand.add(new JMenuItem(MemoryView.COLLAPSE_ALL_ITEM) {
                protected void fireActionPerformed(ActionEvent e) {
                    ttable.collapseAll();
                }
            });
            
            popup.addSeparator();
        }
        
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
    
    private void setAggregation(int aggregation) {
        this.aggregation = aggregation;
        if (dataView != null) {
            if (refSnapshot == null) dataView.setData(snapshot, filter, aggregation);
            else dataView.setData(snapshot.createDiff(refSnapshot), filter, aggregation);
        }
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
                    return true;
                }
                public String getName() {
                    return MemoryView.EXPORT_OBJECTS;
                }
                public ExportUtils.ExportProvider[] getProviders() {
                    return dataView.getExportProviders();
                }
            }
        };
    }
    
}
