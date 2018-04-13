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
package com.sun.tools.visualvm.heapviewer.truffle.ui;

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
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.ui.components.ProfilerToolbar;
import org.netbeans.lib.profiler.ui.swing.ActionPopupButton;
import org.netbeans.lib.profiler.ui.swing.GrayLabel;
import org.netbeans.modules.profiler.api.icons.LanguageIcons;
import com.sun.tools.visualvm.heapviewer.HeapContext;
import com.sun.tools.visualvm.heapviewer.model.DataType;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNode;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNodeFilter;
import com.sun.tools.visualvm.heapviewer.model.Progress;
import com.sun.tools.visualvm.heapviewer.model.RootNode;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleObjectsProvider;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerActions;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerFeature;
import com.sun.tools.visualvm.heapviewer.ui.PluggableTreeTableView;
import com.sun.tools.visualvm.heapviewer.ui.TreeTableViewColumn;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class TruffleObjectsView extends HeapViewerFeature {
    
    protected static enum Preset {
        ALL_OBJECTS ("All Objects"),
        DOMINATORS ("Dominators"),
        GC_ROOTS ("GC Roots");
        
        private final String presetName;
        private Preset(String presetName) { this.presetName = presetName; }
        public String toString() { return presetName; } 
    }
    
    protected static enum Aggregation {
        TYPES ("Types", LanguageIcons.PACKAGE),
        OBJECTS ("Objects", LanguageIcons.INSTANCE);
        
        private final String aggregationName;
        private final String aggregationIcon;
        private Aggregation(String aggregationName, String aggregationIcon) { this.aggregationName = aggregationName; this.aggregationIcon = aggregationIcon; }
        public String toString() { return aggregationName; }
        public String getIcon() { return aggregationIcon; }
    }
    
    private final HeapContext context;
    
    private Icon brandedIcon;
    
    private ProfilerToolbar toolbar;
    private final PluggableTreeTableView objectsView;
    
    private Preset preset = Preset.ALL_OBJECTS;
    private Aggregation aggregation = Aggregation.TYPES;
    
    private ActionPopupButton apbPreset;
    
    private JToggleButton tbType;
    private JToggleButton tbObject;
    
    
    public TruffleObjectsView(String id, HeapContext context, HeapViewerActions actions, final TruffleObjectsProvider objectsProvider) {
        super(id, "Objects", "Objects", null, 200);
        
        this.context = context;
        Heap heap = context.getFragment().getHeap();
        
        objectsView = new PluggableTreeTableView(id, context, actions, TreeTableViewColumn.classes(heap, true)) {
            protected HeapViewerNode[] computeData(RootNode root, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) {
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
                JComponent comp = super.createComponent();

                setFilterComponent(FilterUtils.createFilterPanel(this));

                return comp;
            }
        };
    }
    
    
    protected abstract Icon languageBrandedIcon(String iconKey);
    

    public Icon getIcon() {
        if (brandedIcon == null) brandedIcon = languageBrandedIcon(LanguageIcons.CLASS);
        return brandedIcon;
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
    
    synchronized void setAggregation(Aggregation aggregation) {
        this.aggregation = aggregation;
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
        
        toolbar.add(new GrayLabel("Presets:"));
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
        
        toolbar.add(new GrayLabel("Aggregation:"));
        toolbar.addSpace(2);
        
        final ButtonGroup aggregationBG = new ButtonGroup();
        class AggregationButton extends JToggleButton {
            private final Aggregation aggregation;
            AggregationButton(Aggregation aggregation, boolean selected) {
                super(languageBrandedIcon(aggregation.getIcon()), selected);
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

            toolbar.add(new GrayLabel("Details:"));
            toolbar.addSpace(2);
            
            toolbar.add(objectsView.getToolbar());
        }
    }
    
}
