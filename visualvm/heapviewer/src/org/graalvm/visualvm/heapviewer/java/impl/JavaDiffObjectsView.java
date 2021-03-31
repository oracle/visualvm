/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.heapviewer.java.impl;

import java.awt.BorderLayout;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SortOrder;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.ui.components.ProfilerToolbar;
import org.graalvm.visualvm.heapviewer.HeapContext;
import org.graalvm.visualvm.heapviewer.HeapViewer;
import org.graalvm.visualvm.heapviewer.java.ClassNode;
import org.graalvm.visualvm.heapviewer.java.JavaHeapFragment;
import org.graalvm.visualvm.heapviewer.model.DataType;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNode;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNodeFilter;
import org.graalvm.visualvm.heapviewer.model.Progress;
import org.graalvm.visualvm.heapviewer.model.ProgressNode;
import org.graalvm.visualvm.heapviewer.model.RootNode;
import org.graalvm.visualvm.heapviewer.model.TextNode;
import org.graalvm.visualvm.heapviewer.ui.HeapView;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerActions;
import org.graalvm.visualvm.heapviewer.ui.PluggableTreeTableView;
import org.graalvm.visualvm.heapviewer.ui.TreeTableViewColumn;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.io.File;
import java.io.IOException;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.lib.ui.swing.GrayLabel;
import org.graalvm.visualvm.lib.ui.swing.renderer.ProfilerRenderer;
import org.graalvm.visualvm.lib.profiler.api.ProfilerDialogs;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.LanguageIcons;
import org.graalvm.visualvm.lib.profiler.api.icons.ProfilerIcons;
import org.graalvm.visualvm.lib.ui.swing.renderer.HideableBarRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.RelativeRenderer;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 * TODO:
 *  - gray out classes only present in the heap dump being compared (no super/subclasses information available)
 *  - use HeapDump & DataSourceDescriptor to resolve heap dump names
 *  - enable opening the heap dump being compared
 *  - allow to switch compare order heap1 | heap2
 *  - allow opening classes in either or both heap dumps / viewers
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "JavaDiffObjectsView_Name=Comparison",
    "JavaDiffObjectsView_NamePrefix=Comparison with {0}",
    "JavaDiffObjectsView_Details=Details:",
    "JavaDiffObjectsView_LoadingProgress=loading heap dump...",
    "JavaDiffObjectsView_ComparingProgress=comparing heap dumps...",
    "JavaDiffObjectsView_CompareNoJava=No comparable Java heap found",
    "JavaDiffObjectsView_CompareNoJavaStatus=<no comparable heap found>",
    "JavaDiffObjectsView_CompareFailed=Failed to load heap dump",
    "JavaDiffObjectsView_CompareFailedStatus=<failed to load heap dump>"
})
class JavaDiffObjectsView extends HeapView {
    
    private static enum Aggregation {
        PACKAGES (Bundle.JavaObjectsView_Packages(), Icons.getIcon(LanguageIcons.PACKAGE)),
        CLASSES (Bundle.JavaObjectsView_Classes(), Icons.getIcon(LanguageIcons.CLASS));
        
        private final String aggregationName;
        private final Icon aggregationIcon;
        private Aggregation(String aggregationName, Icon aggregationIcon) { this.aggregationName = aggregationName; this.aggregationIcon = aggregationIcon; }
        public String toString() { return aggregationName; }
        public Icon getIcon() { return aggregationIcon; }
    }
    
    private final File file2;
    private final String file2Name;
    private final String file2Path;
    
    private final Object statusLock = new Object();
    private HeapViewerNode status;
    private List<ClassNode> diffClasses;
    
    private int maxDiffCount = 0;
    private long maxDiffSize = 0;
    private long maxDiffRetained = 0;
    
    private final PluggableTreeTableView objectsView;
    private ProfilerToolbar toolbar;
    private JComponent component;
    
    private Aggregation aggregation;
    
    private JToggleButton tbPackages;
    private JToggleButton tbClasses;
    
        
    JavaDiffObjectsView(HeapContext context1, File file2, final boolean compareRetained, HeapViewerActions actions) {
        super(Bundle.JavaDiffObjectsView_NamePrefix(formattedName(file2)),
              Bundle.JavaDiffObjectsView_NamePrefix(file2.getAbsolutePath()),
              Icons.getIcon(ProfilerIcons.SNAPSHOTS_COMPARE));
        
        this.file2 = file2;
        file2Name = formattedName(file2);
        file2Path = file2.getAbsolutePath();
        
        final Heap heap = context1.getFragment().getHeap();
        
        final TreeTableViewColumn countC = new TreeTableViewColumn.Count(heap, true, true) {
            public int getPreferredWidth() { return ((HideableBarRenderer)getRenderer()).getOptimalWidth(); }
        };
        final TreeTableViewColumn sizeC = new TreeTableViewColumn.OwnSize(heap, true, false);
        final TreeTableViewColumn retainedC = compareRetained ? new TreeTableViewColumn.RetainedSize(heap) : null;
        
        TreeTableViewColumn[] columns = compareRetained ?
                new TreeTableViewColumn[] {
                    new TreeTableViewColumn.Name(heap),
//                    new TreeTableViewColumn.LogicalValue(heap),
                    countC,
                    sizeC,
                    retainedC
                } :
                new TreeTableViewColumn[] {
                    new TreeTableViewColumn.Name(heap),
//                    new TreeTableViewColumn.LogicalValue(heap),
                    countC,
                    sizeC
                };
        
        for (TreeTableViewColumn column : columns) {
            ProfilerRenderer renderer = column.getRenderer();
            if (renderer instanceof RelativeRenderer) ((RelativeRenderer)renderer).setDiffMode(true);
        }
        
        status = new ProgressNode(Bundle.JavaDiffObjectsView_LoadingProgress());
        new RequestProcessor("Compare Heap Dumps Worker").post(new Runnable() { // NOI18N
            public void run() {
                computeDiffClasses(heap, compareRetained);
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        ((HideableBarRenderer)countC.getRenderer()).setMaxValue(maxDiffCount);
                        ((HideableBarRenderer)sizeC.getRenderer()).setMaxValue(maxDiffSize);
                        if (compareRetained) ((HideableBarRenderer)retainedC.getRenderer()).setMaxValue(maxDiffRetained);
                        if (objectsView != null) objectsView.getComponent().repaint();
                    }
                });
            }
        });
        
        objectsView = new PluggableTreeTableView("diff_java_objects", context1, actions, columns) { // NOI18N
            protected HeapViewerNode[] computeData(RootNode root, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {
                synchronized (statusLock) {
                    if (diffClasses == null) return new HeapViewerNode[] { status };
                }
                
                switch (getAggregation()) {
                    case PACKAGES:
                        return JavaDiffClassesProvider.getDiffHeapPackages(root, heap, diffClasses, compareRetained, viewID, viewFilter, dataTypes, sortOrders, progress);
                    case CLASSES:
                        return JavaDiffClassesProvider.getDiffHeapClasses(root, heap, diffClasses, compareRetained, viewID, viewFilter, dataTypes, sortOrders, progress);
                    default:
                        return null;
                }
            }
            protected JComponent createComponent() {
                JComponent comp = super.createComponent();

                setFilterComponent(FilterUtils.createFilterPanel(this));

                return comp;
            }
            protected void populatePopupLast(HeapViewerNode node, JPopupMenu popup) {
                super.populatePopupLast(node, popup);
                JavaClass javaClass = HeapViewerNode.getValue(node, DataType.CLASS, heap);
                final String className = javaClass == null || javaClass.isArray() ? null : javaClass.getName();
                popup.add(new JMenuItem(Bundle.JavaObjectsView_FilterSubclasses()) {
                    {
                        setEnabled(className != null);
                    }
                    protected void fireActionPerformed(ActionEvent e) {
                        JComponent filterComponent = getFilterComponent();
                        filterComponent.setVisible(true);
                        FilterUtils.filterSubclasses(className, filterComponent);
                    }
                });
            }
        };
        objectsView.setViewName(Bundle.JavaDiffObjectsView_Name());
    }
    

    public JComponent getComponent() {
        if (toolbar == null) initUI();
        return component;
    }

    public ProfilerToolbar getToolbar() {
        if (toolbar == null) initUI();
        return toolbar;
    }
    
    
    @Override
    protected void closed() {
        objectsView.closed();
    }
    
    
    private synchronized void setAggregation(Aggregation aggregation) {
        this.aggregation = aggregation;
        objectsView.reloadView();
    }
    
    private synchronized Aggregation getAggregation() {
        return aggregation;
    }
    
    
    private void initUI() {        
        toolbar = ProfilerToolbar.create(false);
        
        toolbar.addSpace(3);
        JLabel refPresenter = new JLabel(file2Name, Icons.getIcon(ProfilerIcons.SNAPSHOTS_COMPARE), JLabel.LEADING);
        refPresenter.setToolTipText(file2Path);
        toolbar.add(refPresenter);
        toolbar.addSpace(5);

        toolbar.addSeparator();
        toolbar.addSpace(5);
        toolbar.add(new GrayLabel(Bundle.JavaObjectsView_Aggregation()));
        toolbar.addSpace(2);
        
        final ButtonGroup aggregationBG = new ButtonGroup();
        class AggregationButton extends JToggleButton {
            private final Aggregation aggregation;
            AggregationButton(Aggregation aggregation) {
                super(aggregation.getIcon());
                this.aggregation = aggregation;
                setToolTipText(aggregation.toString());
                aggregationBG.add(this);
            }
            protected void fireItemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) setAggregation(aggregation);
            }
        }
        
        tbPackages = new AggregationButton(Aggregation.PACKAGES);
        tbPackages.putClientProperty("JButton.buttonType", "segmented"); // NOI18N
        tbPackages.putClientProperty("JButton.segmentPosition", "first"); // NOI18N
        toolbar.add(tbPackages);
        
        tbClasses = new AggregationButton(Aggregation.CLASSES);
        tbClasses.putClientProperty("JButton.buttonType", "segmented"); // NOI18N
        tbClasses.putClientProperty("JButton.segmentPosition", "last"); // NOI18N
        toolbar.add(tbClasses);
        
        tbClasses.setSelected(true);
        aggregation = Aggregation.CLASSES;


        
        if (objectsView.hasPlugins()) {
            toolbar.addSpace(8);

            toolbar.add(new GrayLabel(Bundle.JavaDiffObjectsView_Details()));
            toolbar.addSpace(2);
            
            toolbar.add(objectsView.getToolbar());
        }
        
        component = new ViewContainer(objectsView.getComponent(), file2);
    }
    
    
    private void computeDiffClasses(Heap heap, final boolean compareRetained) {
        try {
            HeapViewer otherViewer = new HeapViewer(file2);
            
            synchronized (statusLock) { status = new ProgressNode(Bundle.JavaDiffObjectsView_ComparingProgress()); }
            objectsView.reloadView();
            
            for (HeapContext otherContext : HeapContext.allContexts(otherViewer)) {
                if (JavaHeapFragment.isJavaHeap(otherContext)) {
                    Heap diffHeap = otherContext.getFragment().getHeap();
                    synchronized (statusLock) {
                        diffClasses = JavaDiffClassesProvider.createDiffClasses(heap, diffHeap, compareRetained);
                        
                        for (ClassNode node : diffClasses) {
                            int count = Math.abs(node.getInstancesCount());
                            maxDiffCount = Math.max(maxDiffCount, count);
                            long size = Math.abs(node.getOwnSize());
                            maxDiffSize = Math.max(maxDiffSize, size);
                            if (compareRetained) {
                                long retained = Math.abs(node.getRetainedSize(heap));
                                maxDiffRetained = Math.max(maxDiffRetained, retained);
                            }
                        }
                        
                        status = null;
                    }
                    objectsView.reloadView();
                    return;
                }
            }
            
            synchronized (statusLock) { status = new TextNode(Bundle.JavaDiffObjectsView_CompareNoJavaStatus()); }
            objectsView.reloadView();
            
            ProfilerDialogs.displayError(Bundle.JavaDiffObjectsView_CompareNoJava());
        } catch (IOException e) {
            ProfilerDialogs.displayError(Bundle.JavaDiffObjectsView_CompareFailed());
            
            synchronized (statusLock) { status = new TextNode(Bundle.JavaDiffObjectsView_CompareFailedStatus()); }
            objectsView.reloadView();
            
            Exceptions.printStackTrace(e);
        }
    }
    
    
    private static String formattedName(File file) {
        String name = file.getName();
        int extIdx = name.lastIndexOf('.'); // NOI18N
        if (extIdx != -1) name = name.substring(0, extIdx);
        return JavaDiffDumpSelector.getHeapDumpDisplayName(name);
    }
    
    
    private static class ViewContainer extends JPanel {
        
        private final File file;
        
        ViewContainer(JComponent view, File file) {
            super(new BorderLayout());
            
            this.file = file;
            
            setOpaque(false);
            setFocusable(false);
            add(view, BorderLayout.CENTER);
        }
        
        public boolean requestFocusInWindow() {
            if (getComponentCount() == 0) return super.requestFocusInWindow();
            else return getComponent(0).requestFocusInWindow();
        }
        
        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof ViewContainer)) return false;
            return file.equals(((ViewContainer)o).file);
        }

        public int hashCode() {
            return file.hashCode();
        }
        
    }
    
}
