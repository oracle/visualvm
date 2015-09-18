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
package org.netbeans.lib.profiler.ui.memory;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.Collection;
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
import org.netbeans.lib.profiler.results.cpu.CPUResultsSnapshot;
import org.netbeans.lib.profiler.results.memory.AllocMemoryResultsSnapshot;
import org.netbeans.lib.profiler.results.memory.LivenessMemoryResultsSnapshot;
import org.netbeans.lib.profiler.results.memory.MemoryResultsSnapshot;
import org.netbeans.lib.profiler.results.memory.PresoObjAllocCCTNode;
import org.netbeans.lib.profiler.results.memory.SampledMemoryResultsSnapshot;
import org.netbeans.lib.profiler.ui.components.ProfilerToolbar;
import org.netbeans.lib.profiler.ui.results.DataView;
import org.netbeans.lib.profiler.ui.swing.ExportUtils;
import org.netbeans.lib.profiler.ui.swing.FilterUtils;
import org.netbeans.lib.profiler.ui.swing.SearchUtils;
import org.netbeans.lib.profiler.utils.Wildcards;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class SnapshotMemoryView extends JPanel {
    
    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.netbeans.lib.profiler.ui.memory.Bundle"); // NOI18N
    private static final String COMPARE_SNAPSHOTS = messages.getString("SnapshotMemoryView_CompareSnapshots"); // NOI18N
    private static final String RESET_COMPARE_SNAPSHOTS = messages.getString("SnapshotMemoryView_ResetCompareSnapshots"); // NOI18N
//    private static final String TOOLBAR_AGGREGATION = messages.getString("SnapshotMemoryView_ToolbarAggregation"); // NOI18N
//    private static final String AGGREGATION_CLASSES = messages.getString("SnapshotMemoryView_AggregationClasses"); // NOI18N
//    private static final String AGGREGATION_PACKAGES = messages.getString("SnapshotMemoryView_AggregationPackages"); // NOI18N
    // -----
    
    private final MemoryView dataView;
    
    private int aggregation;
    private final Collection<String> filter;
    private final MemoryResultsSnapshot snapshot;
    private MemoryResultsSnapshot refSnapshot;
    
    private JToggleButton compareButton;
    
    
    public SnapshotMemoryView(MemoryResultsSnapshot snapshot, Collection<String> filter, Action saveAction, final Action compareAction, Action infoAction, ExportUtils.Exportable exportProvider) {
        this.filter = filter;
        this.snapshot = snapshot;
        
        // class names in VM format
        MemoryView.userFormClassNames(snapshot);
        
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
        
        if (userValue != null && !Wildcards.ALLWILDCARD.equals(userValue.getMethodName())) {
            popup.add(new JMenuItem(MemoryView.ACTION_PROFILE_METHOD) {
                { setEnabled(isSelectable(userValue, true)); }
                protected void fireActionPerformed(ActionEvent e) { profileMethod(userValue); }
            });
        }
        
        popup.add(new JMenuItem(MemoryView.ACTION_PROFILE_CLASS) {
            { setEnabled(userValue != null && aggregation != CPUResultsSnapshot.PACKAGE_LEVEL_VIEW && isSelectable(userValue, false)); }
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
