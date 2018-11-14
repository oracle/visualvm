/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.lib.ui.components.ProfilerToolbar;
import org.graalvm.visualvm.lib.ui.swing.ActionPopupButton;
import org.graalvm.visualvm.lib.ui.swing.GrayLabel;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.LanguageIcons;
import org.graalvm.visualvm.lib.profiler.api.icons.ProfilerIcons;
import org.graalvm.visualvm.heapviewer.HeapContext;
import org.graalvm.visualvm.heapviewer.java.JavaHeapFragment;
import org.graalvm.visualvm.heapviewer.model.DataType;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNode;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNodeFilter;
import org.graalvm.visualvm.heapviewer.model.Progress;
import org.graalvm.visualvm.heapviewer.model.RootNode;
import org.graalvm.visualvm.heapviewer.ui.HeapView;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerActions;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerFeature;
import org.graalvm.visualvm.heapviewer.ui.PluggableTreeTableView;
import org.graalvm.visualvm.heapviewer.ui.TreeTableViewColumn;
import javax.swing.JButton;
import org.graalvm.visualvm.heapviewer.ui.TreeTableView;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "JavaObjectsView_Name=Objects",
    "JavaObjectsView_Description=Objects",
    "JavaObjectsView_Compare=Compare with another heap dump...",
    "JavaObjectsView_AllObjects=All Objects",
    "JavaObjectsView_Dominators=Dominators",
    "JavaObjectsView_GcRoots=GC Roots",
    "JavaObjectsView_Types=Types",
    "JavaObjectsView_Packages=Packages",
    "JavaObjectsView_Classes=Classes",
    "JavaObjectsView_Instances=Instances",
    "JavaObjectsView_FilterSubclasses=Filter Subclasses",
    "JavaObjectsView_Preset=Preset:",
    "JavaObjectsView_Aggregation=Aggregation:",
    "JavaObjectsView_Details=Details:"
})
public class JavaObjectsView extends HeapViewerFeature {
    
    private static final TreeTableView.ColumnConfiguration CCONF_CLASS = new TreeTableView.ColumnConfiguration(DataType.COUNT, null, DataType.COUNT, SortOrder.DESCENDING, Boolean.FALSE);
    private static final TreeTableView.ColumnConfiguration CCONF_INSTANCE = new TreeTableView.ColumnConfiguration(null, DataType.COUNT, DataType.OWN_SIZE, SortOrder.DESCENDING, null);
    
    private static final TreeTableView.ColumnConfiguration CCONF_PRES1 = new TreeTableView.ColumnConfiguration(DataType.COUNT, null, DataType.COUNT, SortOrder.DESCENDING, Boolean.TRUE);
    private static final TreeTableView.ColumnConfiguration CCONF_PRES2 = new TreeTableView.ColumnConfiguration(DataType.OWN_SIZE, null, DataType.OWN_SIZE, SortOrder.DESCENDING, Boolean.TRUE); // TODO: COUNT should also be visible!
    private static final TreeTableView.ColumnConfiguration CCONF_PRES3 = new TreeTableView.ColumnConfiguration(DataType.OWN_SIZE, DataType.COUNT, DataType.OWN_SIZE, SortOrder.DESCENDING, Boolean.TRUE);
    private static final TreeTableView.ColumnConfiguration CCONF_PRES4 = new TreeTableView.ColumnConfiguration(DataType.RETAINED_SIZE, DataType.COUNT, DataType.RETAINED_SIZE, SortOrder.DESCENDING, Boolean.TRUE);
    
    private static enum Preset {
        ALL_OBJECTS (Bundle.JavaObjectsView_AllObjects()),
        DOMINATORS (Bundle.JavaObjectsView_Dominators()),
        GC_ROOTS (Bundle.JavaObjectsView_GcRoots());
        
        private final String presetName;
        private Preset(String presetName) { this.presetName = presetName; }
        public String toString() { return presetName; } 
    }
    
    private static enum Aggregation {
        TYPES (Bundle.JavaObjectsView_Types(), Icons.getIcon(ProfilerIcons.RUN_GC)),
        PACKAGES (Bundle.JavaObjectsView_Packages(), Icons.getIcon(LanguageIcons.PACKAGE)),
        CLASSES (Bundle.JavaObjectsView_Classes(), Icons.getIcon(LanguageIcons.CLASS)),
        INSTANCES (Bundle.JavaObjectsView_Instances(), Icons.getIcon(LanguageIcons.INSTANCE));
        
        private final String aggregationName;
        private final Icon aggregationIcon;
        private Aggregation(String aggregationName, Icon aggregationIcon) { this.aggregationName = aggregationName; this.aggregationIcon = aggregationIcon; }
        public String toString() { return aggregationName; }
        public Icon getIcon() { return aggregationIcon; }
    }
    
    private static final String FEATURE_ID = "java_objects"; // NOI18N
    
    private final HeapContext context;
    private final HeapViewerActions actions;
    
    private final PluggableTreeTableView objectsView;
    private ProfilerToolbar toolbar;
    
    private Preset preset = Preset.ALL_OBJECTS;
    private Aggregation aggregation = Aggregation.CLASSES;
    
    private ActionPopupButton apbPreset;
    
    private JToggleButton tbType;
    private JToggleButton tbPackages;
    private JToggleButton tbClasses;
    private JToggleButton tbInstances;
    
    
    public JavaObjectsView(HeapContext context, HeapViewerActions actions) {
        super(FEATURE_ID, Bundle.JavaObjectsView_Name(), Bundle.JavaObjectsView_Description(), Icons.getIcon(LanguageIcons.CLASS), 200);
        
        this.context = context;
        this.actions = actions;
        
        Heap heap = context.getFragment().getHeap();
        
        objectsView = new PluggableTreeTableView(FEATURE_ID, context, actions, TreeTableViewColumn.classes(heap, true)) {
            protected HeapViewerNode[] computeData(RootNode root, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {
                switch (getPreset()) {
                    case ALL_OBJECTS:
                        switch (getAggregation()) {
                            case PACKAGES:
                                return JavaClassesProvider.getHeapPackages(root, heap, viewID, viewFilter, dataTypes, sortOrders, progress);
                            case CLASSES:
                                return JavaClassesProvider.getHeapClasses(root, heap, viewID, viewFilter, dataTypes, sortOrders, progress);
                            default:
                                return JavaInstancesProvider.getHeapInstances(root, heap, viewID, viewFilter, dataTypes, sortOrders, progress);
                        }
                    case DOMINATORS:
                        switch (getAggregation()) {
                            case PACKAGES:
                                return JavaClassesProvider.getHeapDominators(root, heap, viewID, viewFilter, dataTypes, sortOrders, progress, 2);
                            case CLASSES:
                                return JavaClassesProvider.getHeapDominators(root, heap, viewID, viewFilter, dataTypes, sortOrders, progress, 1);
                            default:
                                return JavaClassesProvider.getHeapDominators(root, heap, viewID, viewFilter, dataTypes, sortOrders, progress, 0);
                        }
                    case GC_ROOTS:
                        switch (getAggregation()) {
                            case TYPES:
                                return JavaClassesProvider.getHeapGCRoots(root, heap, viewID, viewFilter, dataTypes, sortOrders, progress, 3);
                            case PACKAGES:
                                return JavaClassesProvider.getHeapGCRoots(root, heap, viewID, viewFilter, dataTypes, sortOrders, progress, 2);
                            case CLASSES:
                                return JavaClassesProvider.getHeapGCRoots(root, heap, viewID, viewFilter, dataTypes, sortOrders, progress, 1);
                            default:
                                return JavaClassesProvider.getHeapGCRoots(root, heap, viewID, viewFilter, dataTypes, sortOrders, progress, 0);
                        }
                    default:
                        return HeapViewerNode.NO_NODES;
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
    }
    

    public JComponent getComponent() {
        if (toolbar == null) init();
        return objectsView.getComponent();
    }

    public ProfilerToolbar getToolbar() {
        if (toolbar == null) init();
        return toolbar;
    }
    
    
    private volatile boolean skipReload = false;
    
    void configureClassesByInstancesCount() {
        try {
            objectsView.configureColumns(CCONF_PRES1);
            
            if (apbPreset == null) {
                preset = Preset.ALL_OBJECTS;
            } else if (preset != Preset.ALL_OBJECTS) {
                skipReload = true;
                setPreset(Preset.ALL_OBJECTS);
                apbPreset.selectAction(preset.ordinal());
            }

            if (tbClasses == null) {
                skipReload = true;
                setAggregation(Aggregation.CLASSES, null);
            } else if (!tbClasses.isSelected()) {
                skipReload = true;
                tbClasses.setSelected(true);
            }
            
            if (skipReload) objectsView.reloadView();
        } finally {
            skipReload = false;
        }
    }
    
    void configureClassesByInstancesSize() {
        try {
            objectsView.configureColumns(CCONF_PRES2);
            
            if (apbPreset == null) {
                preset = Preset.ALL_OBJECTS;
            } else if (preset != Preset.ALL_OBJECTS) {
                skipReload = true;
                setPreset(Preset.ALL_OBJECTS);
                apbPreset.selectAction(preset.ordinal());
            }

            if (tbClasses == null) {
                skipReload = true;
                setAggregation(Aggregation.CLASSES, null);
            } else if (!tbClasses.isSelected()) {
                skipReload = true;
                tbClasses.setSelected(true);
            }
            
            if (skipReload) objectsView.reloadView();
        } finally {
            skipReload = false;
        }
    }
    
    void configureInstancesBySize() {
        try {
            objectsView.configureColumns(CCONF_PRES3);
            
            if (apbPreset == null) {
                preset = Preset.ALL_OBJECTS;
            } else if (preset != Preset.ALL_OBJECTS) {
                skipReload = true;
                setPreset(Preset.ALL_OBJECTS);
                apbPreset.selectAction(preset.ordinal());
            }

            if (tbInstances == null) {
                skipReload = true;
                setAggregation(Aggregation.INSTANCES, null);
            } else if (!tbInstances.isSelected()) {
                objectsView.configureColumns(CCONF_PRES3);
                skipReload = true;
                tbInstances.setSelected(true);
            }
            
            if (skipReload) objectsView.reloadView();
        } finally {
            skipReload = false;
        }
    }
    
    void configureDominatorsByRetainedSize() {
        try {
            objectsView.configureColumns(CCONF_PRES4);
            
            if (apbPreset == null) {
                preset = Preset.DOMINATORS;
            } else if (preset != Preset.DOMINATORS) {
                skipReload = true;
                setPreset(Preset.DOMINATORS);
                apbPreset.selectAction(preset.ordinal());
            }

            if (tbInstances == null) {
                skipReload = true;
                setAggregation(Aggregation.INSTANCES, null);
            } else if (!tbInstances.isSelected()) {
                skipReload = true;
                tbInstances.setSelected(true);
            }
            
            if (skipReload) objectsView.reloadView();
        } finally {
            skipReload = false;
        }
    }
    
    
    private Runnable dominatorsRefresher;
    
    private synchronized void setPreset(Preset preset) {
        if (preset == Preset.DOMINATORS) {
            final Heap heap = context.getFragment().getHeap();
            if (!DataType.RETAINED_SIZE.valuesAvailable(heap)) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        dominatorsRefresher = new Runnable() {
                            public void run() {
                                if (getPreset() == Preset.DOMINATORS) objectsView.reloadView();
                                dominatorsRefresher = null;
                            }
                        };
                        DataType.RETAINED_SIZE.notifyWhenAvailable(heap, dominatorsRefresher);
                        DataType.RETAINED_SIZE.computeValues(heap, null);
                    }
                });
            }
        }
        
        this.preset = preset;
        objectsView.setViewName(preset.toString());
        tbType.setVisible(preset == Preset.GC_ROOTS);
        if (tbType.isSelected() && !tbType.isVisible()) tbClasses.setSelected(true);
        else if (!skipReload) objectsView.reloadView();
    }
    
    private synchronized Preset getPreset() {
        return preset;
    }
    
    private synchronized void setAggregation(Aggregation aggregation, TreeTableView.ColumnConfiguration cconfig) {
        this.aggregation = aggregation;
        
        if (cconfig != null && !cconfig.equals(objectsView.getCurrentColumnConfiguration()))
            objectsView.configureColumns(cconfig);
        
        if (!skipReload) objectsView.reloadView();
    }
    
    private synchronized Aggregation getAggregation() {
        return aggregation;
    }
        
    
    private void init() {
        toolbar = ProfilerToolbar.create(false);
        
        toolbar.addSpace(2);
        toolbar.addSeparator();
        toolbar.addSpace(2);
        
        JButton compareButton = new JButton(Icons.getIcon(ProfilerIcons.SNAPSHOTS_COMPARE)) {
            protected void fireActionPerformed(ActionEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        final JavaDiffDumpSelector.Result r = JavaDiffDumpSelector.selectSnapshot(context, false);
                        if (r != null) new RequestProcessor().post(new Runnable() {
                            public void run() {
                                HeapView v = new JavaDiffObjectsView(context, r.getFile(), r.compareRetained(), actions);
                                actions.addView(v, true);
                            }
                        });
                    }
                });
            }
        };
        compareButton.setToolTipText(Bundle.JavaObjectsView_Compare());
        toolbar.add(compareButton);
        
        toolbar.addSpace(2);
        toolbar.addSeparator();
        toolbar.addSpace(5);
        
        toolbar.add(new GrayLabel(Bundle.JavaObjectsView_Preset()));
        toolbar.addSpace(2);
        
        class PresetAction extends AbstractAction {
            final Preset preset;
            PresetAction(Preset preset) {
                this.preset = preset;
                putValue(NAME, preset.toString());
            }
            public void actionPerformed(ActionEvent e) {
                setPreset(preset);
            }
        }
        Preset[] presetItems = Preset.values();
        Action[] presetActions = new PresetAction[presetItems.length];
        for (int i = 0; i < presetItems.length; i++) presetActions[i] = new PresetAction(presetItems[i]);
        apbPreset = new ActionPopupButton(0, presetActions);
        apbPreset.selectAction(preset.ordinal());
        toolbar.add(apbPreset);
        
        toolbar.addSpace(8);
        
        toolbar.add(new GrayLabel(Bundle.JavaObjectsView_Aggregation()));
        toolbar.addSpace(2);
        
        final ButtonGroup aggregationBG = new ButtonGroup();
        class AggregationButton extends JToggleButton {
            private final Aggregation aggregation;
            AggregationButton(Aggregation aggregation, boolean selected) {
                super(aggregation.getIcon(), selected);
                this.aggregation = aggregation;
                setToolTipText(aggregation.toString());
                aggregationBG.add(this);
            }
            protected void fireItemStateChanged(ItemEvent e) {
                // invoked also from constructor: super(aggregation.getIcon(), selected)
                // in this case aggregation is still null, ignore the event...
                if (e.getStateChange() == ItemEvent.SELECTED && aggregation != null) {
                    TreeTableView.ColumnConfiguration cconf = Aggregation.INSTANCES.equals(aggregation) ?
                                                              CCONF_INSTANCE : CCONF_CLASS;
                    setAggregation(aggregation, cconf);
                }
            }
        }
        
        tbType = new AggregationButton(Aggregation.TYPES, Aggregation.TYPES.equals(aggregation)) {
            public void setVisible(boolean b) {
                super.setVisible(b);
                if (tbPackages != null) tbPackages.putClientProperty("JButton.segmentPosition", // NOI18N
                                        b ? "middle" : "first"); // NOI18N
            }
        };
        tbType.putClientProperty("JButton.buttonType", "segmented"); // NOI18N
        tbType.putClientProperty("JButton.segmentPosition", "first"); // NOI18N
        toolbar.add(tbType);
        
        tbPackages = new AggregationButton(Aggregation.PACKAGES, Aggregation.PACKAGES.equals(aggregation));
        tbPackages.putClientProperty("JButton.buttonType", "segmented"); // NOI18N
        tbPackages.putClientProperty("JButton.segmentPosition", "first"); // NOI18N
        toolbar.add(tbPackages);
        
        tbClasses = new AggregationButton(Aggregation.CLASSES, Aggregation.CLASSES.equals(aggregation));
        tbClasses.putClientProperty("JButton.buttonType", "segmented"); // NOI18N
        tbClasses.putClientProperty("JButton.segmentPosition", "middle"); // NOI18N
        toolbar.add(tbClasses);
        
        tbInstances = new AggregationButton(Aggregation.INSTANCES, Aggregation.INSTANCES.equals(aggregation));
        tbInstances.putClientProperty("JButton.buttonType", "segmented"); // NOI18N
        tbInstances.putClientProperty("JButton.segmentPosition", "last"); // NOI18N
        toolbar.add(tbInstances);

        setPreset(preset); // updates tbType visibility and sets objectsView name
        
        if (objectsView.hasPlugins()) {
            toolbar.addSpace(8);

            toolbar.add(new GrayLabel(Bundle.JavaObjectsView_Details()));
            toolbar.addSpace(2);
            
            toolbar.add(objectsView.getToolbar());
        }
    }
    
    
    @ServiceProvider(service=HeapViewerFeature.Provider.class)
    public static class Provider extends HeapViewerFeature.Provider {

        public HeapViewerFeature getFeature(HeapContext context, HeapViewerActions actions) {
            if (JavaHeapFragment.isJavaHeap(context))
                return new JavaObjectsView(context, actions);

            return null;
        }

    }
    
}
