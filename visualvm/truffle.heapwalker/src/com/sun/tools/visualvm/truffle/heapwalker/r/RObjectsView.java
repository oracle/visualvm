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
package com.sun.tools.visualvm.truffle.heapwalker.r;

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
import org.netbeans.modules.profiler.heapwalker.v2.HeapContext;
import org.netbeans.modules.profiler.heapwalker.v2.model.DataType;
import org.netbeans.modules.profiler.heapwalker.v2.model.HeapWalkerNode;
import org.netbeans.modules.profiler.heapwalker.v2.model.HeapWalkerNodeFilter;
import org.netbeans.modules.profiler.heapwalker.v2.model.Progress;
import org.netbeans.modules.profiler.heapwalker.v2.model.RootNode;
import org.netbeans.modules.profiler.heapwalker.v2.ui.HeapWalkerActions;
import org.netbeans.modules.profiler.heapwalker.v2.ui.HeapWalkerFeature;
import org.netbeans.modules.profiler.heapwalker.v2.ui.PluggableTreeTableView;
import org.netbeans.modules.profiler.heapwalker.v2.ui.TreeTableViewColumn;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
class RObjectsView extends HeapWalkerFeature {
    
    private static enum Preset {
        ALL_OBJECTS ("All Objects"),
        DOMINATORS ("Dominators"),
        GC_ROOTS ("GC Roots");
        
        private final String presetName;
        private Preset(String presetName) { this.presetName = presetName; }
        public String toString() { return presetName; } 
    }
    
    private static enum Aggregation {
        TYPES ("Types", RSupport.createBadgedIcon(LanguageIcons.PACKAGE)),
        OBJECTS ("Objects", RSupport.createBadgedIcon(LanguageIcons.INSTANCE));
        
        private final String aggregationName;
        private final Icon aggregationIcon;
        private Aggregation(String aggregationName, Icon aggregationIcon) { this.aggregationName = aggregationName; this.aggregationIcon = aggregationIcon; }
        public String toString() { return aggregationName; }
        public Icon getIcon() { return aggregationIcon; }
    }
    
    private final HeapContext context;
    
    private ProfilerToolbar toolbar;
    private final PluggableTreeTableView objectsView;
    
    private Preset preset;
    private Aggregation aggregation;
    
    private JToggleButton tbType;
    private JToggleButton tbObject;
    
    
    public RObjectsView(HeapContext context, HeapWalkerActions actions) {
        super("r_objects", "Objects", "Objects", RSupport.createBadgedIcon(LanguageIcons.CLASS), 200);
        
        this.context = context;
        Heap heap = context.getFragment().getHeap();
        
        objectsView = new PluggableTreeTableView("r_objects", context, actions, TreeTableViewColumn.classes(heap, true)) {
            protected HeapWalkerNode[] computeData(RootNode root, Heap heap, String viewID, HeapWalkerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) {
                switch (getPreset()) {
                    case ALL_OBJECTS:
                        switch (getAggregation()) {
                            case TYPES:
                                return RObjectsProvider.getAllObjects(root, RObjectsView.this.context, viewID, viewFilter, dataTypes, sortOrders, progress, 1);
                            default:
                                return RObjectsProvider.getAllObjects(root, RObjectsView.this.context, viewID, viewFilter, dataTypes, sortOrders, progress, 0);
                        }
                    case DOMINATORS:
                        switch (getAggregation()) {
                            case TYPES:
                                return RObjectsProvider.getDominators(root, RObjectsView.this.context, viewID, viewFilter, dataTypes, sortOrders, progress, 1);
                            default:
                                return RObjectsProvider.getDominators(root, RObjectsView.this.context, viewID, viewFilter, dataTypes, sortOrders, progress, 0);
                        }
                    case GC_ROOTS:
                        switch (getAggregation()) {
                            case TYPES:
                                return RObjectsProvider.getGCRoots(root, RObjectsView.this.context, viewID, viewFilter, dataTypes, sortOrders, progress, 1);
                            default:
                                return RObjectsProvider.getGCRoots(root, RObjectsView.this.context, viewID, viewFilter, dataTypes, sortOrders, progress, 0);
                        }
                    default:
                        return HeapWalkerNode.NO_NODES;
                }
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
        objectsView.reloadView();
    }
    
    private synchronized Preset getPreset() {
        return preset;
    }
    
    private synchronized void setAggregation(Aggregation aggregation) {
        this.aggregation = aggregation;
        objectsView.reloadView();
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
        preset = Preset.ALL_OBJECTS;
        objectsView.setViewName(Preset.ALL_OBJECTS.toString());
        toolbar.add(new ActionPopupButton(0, presetActions));
        
        toolbar.addSpace(8);
        
        toolbar.add(new GrayLabel("Aggregation:"));
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
        
        tbType = new AggregationButton(Aggregation.TYPES);
        toolbar.add(tbType);
        
        tbObject = new AggregationButton(Aggregation.OBJECTS);
        toolbar.add(tbObject);
        
        if (objectsView.hasPlugins()) {
            toolbar.addSpace(8);

            toolbar.add(new GrayLabel("Details:"));
            toolbar.addSpace(2);
            
            toolbar.add(objectsView.getToolbar());
        }
        
        tbType.setSelected(true);
        aggregation = Aggregation.TYPES;
    }
    
    
    @ServiceProvider(service=HeapWalkerFeature.Provider.class)
    public static class Provider extends HeapWalkerFeature.Provider {

        public HeapWalkerFeature getFeature(HeapContext context, HeapWalkerActions actions) {
            if (RHeapFragment.isRHeap(context))
                return new RObjectsView(context, actions);
            
            return null;
        }

    }
    
}
