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
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.results.cpu.CPUResultsSnapshot;
import org.netbeans.lib.profiler.results.memory.AllocMemoryResultsSnapshot;
import org.netbeans.lib.profiler.results.memory.LivenessMemoryResultsSnapshot;
import org.netbeans.lib.profiler.results.memory.MemoryResultsSnapshot;
import org.netbeans.lib.profiler.results.memory.PresoObjAllocCCTNode;
import org.netbeans.lib.profiler.results.memory.SampledMemoryResultsSnapshot;
import org.netbeans.lib.profiler.ui.components.ProfilerToolbar;
import org.netbeans.lib.profiler.ui.results.DataView;
import org.netbeans.lib.profiler.ui.swing.ActionPopupButton;
import org.netbeans.lib.profiler.ui.swing.ExportUtils;
import org.netbeans.lib.profiler.ui.swing.GrayLabel;
import org.netbeans.lib.profiler.utils.Wildcards;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class SnapshotMemoryView extends JPanel {
    
    private final DataView dataView;
    
    private final DataSetter dataSetter;
    private final ExporterGetter exporterGetter;
    
    private int aggregation;
    
    public SnapshotMemoryView(final MemoryResultsSnapshot snapshot, final Collection filter, Action... actions) {
        setLayout(new BorderLayout());
        
        boolean supportsPackageAggregation = true;
        
        if (snapshot instanceof SampledMemoryResultsSnapshot) {
            final SampledTableView view = new SampledTableView(null) {
                protected void performDefaultAction(ClientUtils.SourceCodeSelection value) {
                    if (showSourceSupported()) showSource(value);
                }
                protected void populatePopup(JPopupMenu popup, ClientUtils.SourceCodeSelection value) {
                    SnapshotMemoryView.this.populatePopup(this, popup, value);
                }
            };
            add(view, BorderLayout.CENTER);
            
            dataView = view;
            dataSetter = new DataSetter() {
                public void setData(int aggregation) { view.setData((SampledMemoryResultsSnapshot)snapshot, aggregation); }
            };
            exporterGetter = new ExporterGetter() {
                public ExportUtils.ExportProvider[] getProviders() { return view.getExportProviders(); }
            };
        } else if (snapshot instanceof AllocMemoryResultsSnapshot) {
            final AllocMemoryResultsSnapshot _snapshot = (AllocMemoryResultsSnapshot)snapshot;
            if (snapshot.containsStacks()) {
                final AllocTreeTableView view = new AllocTreeTableView() {
                    protected void performDefaultAction(ClientUtils.SourceCodeSelection value) {
                        if (showSourceSupported()) showSource(value);
                    }
                    protected void populatePopup(JPopupMenu popup, ClientUtils.SourceCodeSelection value) {
                        SnapshotMemoryView.this.populatePopup(this, popup, value);
                    }
                };
                add(view, BorderLayout.CENTER);
                
                dataView = view;
                dataSetter = new DataSetter() {
                    public void setData(int aggregation) { view.setData(_snapshot, filter, aggregation); }
                };
                exporterGetter = new ExporterGetter() {
                    public ExportUtils.ExportProvider[] getProviders() { return view.getExportProviders(); }
                };
                supportsPackageAggregation = false;
            } else {
                final AllocTableView view = new AllocTableView(null) {
                    protected void performDefaultAction(ClientUtils.SourceCodeSelection value) {
                        if (showSourceSupported()) showSource(value);
                    }
                    protected void populatePopup(JPopupMenu popup, ClientUtils.SourceCodeSelection value) {
                        SnapshotMemoryView.this.populatePopup(this, popup, value);
                    }
                };
                add(view, BorderLayout.CENTER);
                
                dataView = view;
                dataSetter = new DataSetter() {
                    public void setData(int aggregation) { view.setData(_snapshot, filter, aggregation); }
                };
                exporterGetter = new ExporterGetter() {
                    public ExportUtils.ExportProvider[] getProviders() { return view.getExportProviders(); }
                };
            }
        } else if (snapshot instanceof LivenessMemoryResultsSnapshot) {
            final LivenessTableView view = new LivenessTableView(null) {
                protected void performDefaultAction(ClientUtils.SourceCodeSelection value) {
                    if (showSourceSupported()) showSource(value);
                }
                protected void populatePopup(JPopupMenu popup, ClientUtils.SourceCodeSelection value) {
                    SnapshotMemoryView.this.populatePopup(this, popup, value);
                }
            };
            add(view, BorderLayout.CENTER);
            
            dataView = view;
            dataSetter = new DataSetter() {
                public void setData(int aggregation) { view.setData((LivenessMemoryResultsSnapshot)snapshot, filter, aggregation); }
            };
            exporterGetter = new ExporterGetter() {
                public ExportUtils.ExportProvider[] getProviders() { return view.getExportProviders(); }
            };
        } else {
            dataView = null;
            dataSetter = null;
            exporterGetter = null;
        }
        
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
        
        Action aClasses = new AbstractAction() {
            { putValue(NAME, "Classes"); }
            public void actionPerformed(ActionEvent e) { setAggregation(CPUResultsSnapshot.CLASS_LEVEL_VIEW); }
            
        };
        Action aPackages = new AbstractAction() {
            { putValue(NAME, "Packages"); }
            public void actionPerformed(ActionEvent e) { setAggregation(CPUResultsSnapshot.PACKAGE_LEVEL_VIEW); }
            
        };
        
        ActionPopupButton aggregation = new ActionPopupButton(0, aClasses, aPackages);
        aggregation.setEnabled(supportsPackageAggregation);
        toolbar.add(aggregation);
        
        Action aInfo = actions.length > 0 ? actions[actions.length - 1] : null;
        if (aInfo != null) {
            toolbar.addFiller();
            toolbar.add(aInfo);
        }
        
        add(toolbar.getComponent(), BorderLayout.NORTH);
        
        setAggregation(CPUResultsSnapshot.CLASS_LEVEL_VIEW);
    }
    
    
    public void activateFilter() {
        dataView.activateFilter();
    }
    
    public void activateSearch() {
        dataView.activateSearch();
    }
    
    
    public ExportUtils.Exportable getExportable(final File sourceFile) {
        return new ExportUtils.Exportable() {
            public String getName() {
                return "Export Objects";
            }
            public ExportUtils.ExportProvider[] getProviders() {
                ExportUtils.ExportProvider npsProvider = sourceFile == null ? null :
                    new ExportUtils.NPSExportProvider(sourceFile);
                ExportUtils.ExportProvider[] providers = exporterGetter.getProviders();
                
                List<ExportUtils.ExportProvider> _providers = new ArrayList();
                if (npsProvider != null) _providers.add(npsProvider);
                if (providers != null) _providers.addAll(Arrays.asList(providers));
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
    
    
    // TODO: implement isSelectable()
//    // Check if primitive type/array
//    if ((methodName == null && methodSig == null) && (VMUtils.isVMPrimitiveType(className) ||
//         VMUtils.isPrimitiveType(className))) ProfilerDialogs.displayWarning(CANNOT_SHOW_PRIMITIVE_SRC_MSG);
    static boolean isSelectable(ClientUtils.SourceCodeSelection value, boolean method) {
        String className = value.getClassName();
        String methodName = value.getMethodName();
        
        if (method && methodName.endsWith("[native]")) return false;
        
        if (PresoObjAllocCCTNode.VM_ALLOC_CLASS.equals(className) && PresoObjAllocCCTNode.VM_ALLOC_METHOD.equals(methodName)) return false;
        
        return true;
    }
    
    private void populatePopup(final DataView invoker, JPopupMenu popup, final ClientUtils.SourceCodeSelection value) {
        if (showSourceSupported()) {
            popup.add(new JMenuItem("Go to Source") {
                { setEnabled(value != null && aggregation != CPUResultsSnapshot.PACKAGE_LEVEL_VIEW); setFont(getFont().deriveFont(Font.BOLD)); }
                protected void fireActionPerformed(ActionEvent e) { showSource(value); }
            });
            popup.addSeparator();
        }
        
        if (value != null && !Wildcards.ALLWILDCARD.equals(value.getMethodName())) {
            popup.add(new JMenuItem("Profile Method") {
                { setEnabled(isSelectable(value, true)); }
                protected void fireActionPerformed(ActionEvent e) { profileMethod(value); }
            });
        }
        
        popup.add(new JMenuItem("Profile Class") {
            { setEnabled(value != null && aggregation != CPUResultsSnapshot.PACKAGE_LEVEL_VIEW && isSelectable(value, false)); }
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
    
    private void setAggregation(int _aggregation) {
        aggregation = _aggregation;
        
        dataSetter.setData(aggregation);
    }
    
    
    private static interface DataSetter { void setData(int aggregation); }
    
    private static interface ExporterGetter { ExportUtils.ExportProvider[] getProviders(); }
    
}
