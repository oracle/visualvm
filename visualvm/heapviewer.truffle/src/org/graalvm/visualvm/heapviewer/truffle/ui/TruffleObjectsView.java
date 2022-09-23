/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.heapviewer.truffle.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JToggleButton;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.heapviewer.HeapContext;
import org.graalvm.visualvm.heapviewer.model.DataType;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNode;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNodeFilter;
import org.graalvm.visualvm.heapviewer.model.Progress;
import org.graalvm.visualvm.heapviewer.model.RootNode;
import org.graalvm.visualvm.heapviewer.truffle.TruffleLanguage;
import org.graalvm.visualvm.heapviewer.truffle.TruffleObjectsProvider;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerActions;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerFeature;
import org.graalvm.visualvm.heapviewer.ui.PluggableTreeTableView;
import org.graalvm.visualvm.heapviewer.ui.TreeTableViewColumn;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.LanguageIcons;
import org.graalvm.visualvm.lib.ui.components.ProfilerToolbar;
import org.graalvm.visualvm.lib.ui.swing.ActionPopupButton;
import org.graalvm.visualvm.lib.ui.swing.GrayLabel;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "TruffleObjectsView_Name=Objects",
    "TruffleObjectsView_Description=Objects",
    "TruffleObjectsView_Compare=Compare with another heap dump...",
    "TruffleObjectsView_AllObjects=All Objects",
    "TruffleObjectsView_Dominators=Dominators",
    "TruffleObjectsView_GcRoots=GC Roots",
    "TruffleObjectsView_Types=Types",
    "TruffleObjectsView_Objects=Objects",
    "TruffleObjectsView_FilterSubclasses=Filter Subclasses",
    "TruffleObjectsView_Preset=Preset:",
    "TruffleObjectsView_Aggregation=Aggregation:",
    "TruffleObjectsView_Details=Details:"
})
public class TruffleObjectsView extends HeapViewerFeature {
    
    private static final String FEATURE_ID = "objects"; // NOI18N
    
    protected static enum Preset {
        ALL_OBJECTS (Bundle.TruffleObjectsView_AllObjects()),
        DOMINATORS (Bundle.TruffleObjectsView_Dominators()),
        GC_ROOTS (Bundle.TruffleObjectsView_GcRoots());
        
        private final String presetName;
        private Preset(String presetName) { this.presetName = presetName; }
        public String toString() { return presetName; } 
    }
    
    protected static enum Aggregation {
        TYPES (Bundle.TruffleObjectsView_Types(), Icons.getIcon(LanguageIcons.PACKAGE)),
        OBJECTS (Bundle.TruffleObjectsView_Objects(), Icons.getIcon(LanguageIcons.INSTANCE));
        
        private final String aggregationName;
        private final Icon aggregationIcon;
        private Aggregation(String aggregationName, Icon aggregationIcon) { this.aggregationName = aggregationName; this.aggregationIcon = aggregationIcon; }
        public String toString() { return aggregationName; }
        public Icon getIcon() { return aggregationIcon; }
    }
    
    
    private final TruffleLanguage language;
    
    private final HeapContext context;
    
    private ProfilerToolbar toolbar;
    private final PluggableTreeTableView objectsView;
    private JComponent component;
    
    private Preset preset = Preset.ALL_OBJECTS;
    private Aggregation aggregation = Aggregation.TYPES;
    
    private ActionPopupButton apbPreset;
    
    private JToggleButton tbType;
    private JToggleButton tbObject;
    
    
    public TruffleObjectsView(TruffleLanguage language, HeapContext context, HeapViewerActions actions) {
        super(idFromLanguage(language), Bundle.TruffleObjectsView_Name(), Bundle.TruffleObjectsView_Description(), iconFromLanguage(language), 200);
        
        this.language = language;
        this.context = context;
        Heap heap = context.getFragment().getHeap();
        
        final TruffleObjectsProvider objectsProvider = new TruffleObjectsProvider(language);
        
        objectsView = new PluggableTreeTableView(getID(), context, actions, TreeTableViewColumn.classes(heap, true)) {
            protected HeapViewerNode[] computeData(RootNode root, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {
                switch (getPreset()) {
                    case ALL_OBJECTS:
                        switch (getAggregation()) {
                            case TYPES:
                                return objectsProvider.getAllObjects(root, context, viewID, viewFilter, dataTypes, sortOrders, progress, 1);
                            default:
                                return objectsProvider.getAllObjects(root, context, viewID, viewFilter, dataTypes, sortOrders, progress, 0);
                        }
                    case DOMINATORS:
                        switch (getAggregation()) {
                            case TYPES:
                                return objectsProvider.getDominators(root, context, viewID, viewFilter, dataTypes, sortOrders, progress, 1);
                            default:
                                return objectsProvider.getDominators(root, context, viewID, viewFilter, dataTypes, sortOrders, progress, 0);
                        }
                    case GC_ROOTS:
                        switch (getAggregation()) {
                            case TYPES:
                                return objectsProvider.getGCRoots(root, context, viewID, viewFilter, dataTypes, sortOrders, progress, 1);
                            default:
                                return objectsProvider.getGCRoots(root, context, viewID, viewFilter, dataTypes, sortOrders, progress, 0);
                        }
                    default:
                        return HeapViewerNode.NO_NODES;
                }
            }
            protected JComponent createComponent() {
                if (component == null) {
                    component = super.createComponent();
                    setFilterComponent(FilterUtils.createFilterPanel(this));
                }

                return component;
            }
        };
    }
    
    
    static String idFromLanguage(TruffleLanguage language) {
        return language.getID() + "_" + FEATURE_ID; // NOI18N
    }
    
    static Icon iconFromLanguage(TruffleLanguage language) {
        return language.createLanguageIcon(Icons.getIcon(LanguageIcons.CLASS));
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
    
    public void configureTypesByObjectsCount() {
        try {
            objectsView.setSortColumn(DataType.COUNT, SortOrder.DESCENDING);
            
            if (apbPreset == null) {
                preset = Preset.ALL_OBJECTS;
            } else if (preset != Preset.ALL_OBJECTS) {
                skipReload = true;
                setPreset(Preset.ALL_OBJECTS);
                apbPreset.selectAction(preset.ordinal());
            }

            if (tbType == null) {
                skipReload = true;
                setAggregation(Aggregation.TYPES);
            } else if (!tbType.isSelected()) {
                skipReload = true;
                tbType.setSelected(true);
            }
            
            if (skipReload) objectsView.reloadView();
        } finally {
            skipReload = false;
        }
    }
    
    public void configureTypesByObjectsSize() {
        try {
            objectsView.setSortColumn(DataType.OWN_SIZE, SortOrder.DESCENDING);
            
            if (apbPreset == null) {
                preset = Preset.ALL_OBJECTS;
            } else if (preset != Preset.ALL_OBJECTS) {
                skipReload = true;
                setPreset(Preset.ALL_OBJECTS);
                apbPreset.selectAction(preset.ordinal());
            }

            if (tbType == null) {
                skipReload = true;
                setAggregation(Aggregation.TYPES);
            } else if (!tbType.isSelected()) {
                skipReload = true;
                tbType.setSelected(true);
            }
            
            if (skipReload) objectsView.reloadView();
        } finally {
            skipReload = false;
        }
    }
    
    public void configureObjectsBySize() {
        try {
            objectsView.setSortColumn(DataType.OWN_SIZE, SortOrder.DESCENDING);
            
            if (apbPreset == null) {
                preset = Preset.ALL_OBJECTS;
            } else if (preset != Preset.ALL_OBJECTS) {
                skipReload = true;
                setPreset(Preset.ALL_OBJECTS);
                apbPreset.selectAction(preset.ordinal());
            }

            if (tbObject == null) {
                skipReload = true;
                setAggregation(Aggregation.OBJECTS);
            } else if (!tbObject.isSelected()) {
                skipReload = true;
                tbObject.setSelected(true);
            }
            
            if (skipReload) objectsView.reloadView();
        } finally {
            skipReload = false;
        }
    }
    
    public void configureDominatorsByRetainedSize() {
        try {
            objectsView.setSortColumn(DataType.RETAINED_SIZE, SortOrder.DESCENDING);
            
            if (apbPreset == null) {
                preset = Preset.DOMINATORS;
            } else if (preset != Preset.DOMINATORS) {
                skipReload = true;
                setPreset(Preset.DOMINATORS);
                apbPreset.selectAction(preset.ordinal());
            }

            if (tbObject == null) {
                skipReload = true;
                setAggregation(Aggregation.OBJECTS);
            } else if (!tbObject.isSelected()) {
                skipReload = true;
                tbObject.setSelected(true);
            }
            
            if (skipReload) objectsView.reloadView();
        } finally {
            skipReload = false;
        }
    }
    
    
    private Runnable dominatorsRefresher;
    
    synchronized void setPreset(Preset preset) {
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
        if (!skipReload) objectsView.reloadView();
    }
    
    private synchronized Preset getPreset() {
        return preset;
    }
    
    private volatile boolean countVisible1 = true;
    private volatile boolean countVisible2 = false;
    
    synchronized void setAggregation(Aggregation aggregation) {
        boolean objectsInvolved = Aggregation.OBJECTS.equals(aggregation) ||
                                  Aggregation.OBJECTS.equals(this.aggregation);
        
        this.aggregation = aggregation;
        
        if (objectsInvolved) {
            // TODO: having Count visible for Instances aggregation resets the column width!
            objectsView.getComponent(); // Make sure objectsView is initialized before accessing its columns
            boolean countVisible = objectsView.isColumnVisible(DataType.COUNT);
            if (Aggregation.OBJECTS.equals(aggregation)) {
                countVisible1 = countVisible;
                objectsView.setColumnVisible(DataType.COUNT, countVisible2);
            } else {
                countVisible2 = countVisible;
                objectsView.setColumnVisible(DataType.COUNT, countVisible1);
            }
        }
        
        if (!skipReload) objectsView.reloadView();
    }
    
    private synchronized Aggregation getAggregation() {
        return aggregation;
    }
        
    
    private void init() {
        toolbar = ProfilerToolbar.create(false);
        
        toolbar.addSpace(2);
        toolbar.addSeparator();
        toolbar.addSpace(5);
        
        toolbar.add(new GrayLabel(Bundle.TruffleObjectsView_Preset()));
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
        
        toolbar.add(new GrayLabel(Bundle.TruffleObjectsView_Aggregation()));
        toolbar.addSpace(2);
        
        final ButtonGroup aggregationBG = new ButtonGroup();
        class AggregationButton extends JToggleButton {
            private final Aggregation aggregation;
            AggregationButton(Aggregation aggregation, boolean selected) {
                super(language.createLanguageIcon(aggregation.getIcon()), selected);
                this.aggregation = aggregation;
                setToolTipText(aggregation.toString());
                aggregationBG.add(this);
            }
            protected void fireItemStateChanged(ItemEvent e) {
                // invoked also from constructor: super(aggregation.getIcon(), selected)
                // in this case aggregation is still null, ignore the event...
                if (e.getStateChange() == ItemEvent.SELECTED && aggregation != null) setAggregation(aggregation);
            }
        }
        
        tbType = new AggregationButton(Aggregation.TYPES, Aggregation.TYPES.equals(aggregation));
        tbType.putClientProperty("JButton.buttonType", "segmented"); // NOI18N
        tbType.putClientProperty("JButton.segmentPosition", "first"); // NOI18N
        toolbar.add(tbType);
        
        tbObject = new AggregationButton(Aggregation.OBJECTS, Aggregation.OBJECTS.equals(aggregation));
        tbObject.putClientProperty("JButton.buttonType", "segmented"); // NOI18N
        tbObject.putClientProperty("JButton.segmentPosition", "last"); // NOI18N
        toolbar.add(tbObject);
        
        setPreset(preset);
        
        if (objectsView.hasPlugins()) {
            toolbar.addSpace(8);

            toolbar.add(new GrayLabel(Bundle.TruffleObjectsView_Details()));
            toolbar.addSpace(2);
            
            toolbar.add(objectsView.getToolbar());
        }
    }
    
}
