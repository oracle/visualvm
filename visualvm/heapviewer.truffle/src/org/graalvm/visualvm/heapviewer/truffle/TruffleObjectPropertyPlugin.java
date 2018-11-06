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
package org.graalvm.visualvm.heapviewer.truffle;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Objects;
import javax.swing.JComponent;
import javax.swing.SortOrder;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.heapviewer.HeapContext;
import org.graalvm.visualvm.heapviewer.model.DataType;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNode;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNodeFilter;
import org.graalvm.visualvm.heapviewer.model.Progress;
import org.graalvm.visualvm.heapviewer.model.RootNode;
import org.graalvm.visualvm.heapviewer.model.TextNode;
import org.graalvm.visualvm.heapviewer.ui.HeapViewPlugin;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerActions;
import org.graalvm.visualvm.heapviewer.ui.TreeTableView;
import org.graalvm.visualvm.heapviewer.ui.TreeTableViewColumn;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerRenderer;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "TruffleObjectPropertyPlugin_NoSelection=<no object selected>",
    "TruffleObjectPropertyPlugin_NoItems=<no {0}>", // <no items>
    "TruffleObjectPropertyPlugin_ShowMergedSwitch=Show merged {0}"
})
public class TruffleObjectPropertyPlugin<O extends TruffleObject, T extends TruffleType<O>, F extends TruffleLanguageHeapFragment<O, T>, L extends TruffleLanguage<O, T, F>> extends HeapViewPlugin {
    
    private static final String KEY_TYPE_PROPERTIES_HISTOGRAM = "tPropertiesHisto"; // NOI18N
    
    private volatile boolean tPropertiesHisto = readItem(KEY_TYPE_PROPERTIES_HISTOGRAM, true);
    
    private static final TreeTableView.ColumnConfiguration CCONF_TYPE = new TreeTableView.ColumnConfiguration(DataType.COUNT, null, DataType.COUNT, SortOrder.DESCENDING, Boolean.FALSE);
    private static final TreeTableView.ColumnConfiguration CCONF_OBJECT = new TreeTableView.ColumnConfiguration(null, DataType.COUNT, DataType.NAME, SortOrder.UNSORTED, null);
    
    private final TruffleObjectPropertyProvider<O, T, F, L, ? extends Object> provider;
    
    private final Heap heap;
    
    private HeapViewerNode selected;
    
    private final TreeTableView objectsView;
    
    
    // TODO: temporary workaround, delete ASAP!
    private static TreeTableView SHARED_VIEW;
    // TODO: temporary workaround, delete ASAP!
    static HeapViewerRenderer resolveRenderer(HeapViewerNode node) {
        return SHARED_VIEW == null ? null : SHARED_VIEW.getNodeRenderer(node);
    }
    

    public TruffleObjectPropertyPlugin(String name, String description, Icon icon, String viewID, HeapContext context, HeapViewerActions actions, TruffleObjectPropertyProvider<O, T, F, L, ? extends Object> provider) {
        super(name, description, icon);
        
        this.provider = provider;
        
        if (!provider.supportsAggregation()) tPropertiesHisto = false;
        
        heap = context.getFragment().getHeap();
        
        TreeTableViewColumn[] columns = provider.supportsAggregation() ?
            new TreeTableViewColumn[] {
                new TreeTableViewColumn.Name(heap),
                new TreeTableViewColumn.LogicalValue(heap),
                new TreeTableViewColumn.Count(heap, true, true),
                new TreeTableViewColumn.OwnSize(heap, false, false),
                new TreeTableViewColumn.RetainedSize(heap, false, false),
                new TreeTableViewColumn.ObjectID(heap)
            } : new TreeTableViewColumn[] {
                new TreeTableViewColumn.Name(heap),
                new TreeTableViewColumn.LogicalValue(heap),
                new TreeTableViewColumn.OwnSize(heap, false, false),
                new TreeTableViewColumn.RetainedSize(heap, false, false),
                new TreeTableViewColumn.ObjectID(heap)
            };
        objectsView = new TreeTableView(viewID, context, actions, columns) {
            protected HeapViewerNode[] computeData(RootNode root, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) {
                HeapViewerNode _selected;
                synchronized (objectsView) { _selected = selected; }
                
                if (_selected == null) return new HeapViewerNode[] { new TextNode(Bundle.TruffleObjectPropertyPlugin_NoSelection()) };
                
                HeapViewerNode[] nodes;
                TruffleObjectsWrapper wrapper = !tPropertiesHisto ? null : HeapViewerNode.getValue(_selected, TruffleObjectsWrapper.DATA_TYPE, heap);
                if (wrapper != null) {
                    nodes = provider.getNodes(wrapper, _selected, heap, viewID, viewFilter, dataTypes, sortOrders, progress);
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            if (!CCONF_TYPE.equals(objectsView.getCurrentColumnConfiguration()))
                                objectsView.configureColumns(CCONF_TYPE);
                        }
                    });
                } else {
                    O selectedO = provider.getObject(_selected, heap);
                    if (selectedO != null) nodes = provider.getNodes(selectedO, root, heap, viewID, viewFilter, dataTypes, sortOrders, progress);
                    else nodes = new HeapViewerNode[] { new TextNode(Bundle.TruffleObjectPropertyPlugin_NoSelection()) };
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            if (!CCONF_OBJECT.equals(objectsView.getCurrentColumnConfiguration()))
                                objectsView.configureColumns(CCONF_OBJECT);
                        }
                    });
                }
                    
                return nodes == null || nodes.length == 0 ? new HeapViewerNode[] { new TextNode(Bundle.TruffleObjectPropertyPlugin_NoItems(provider.getName())) } : nodes;
            }
            @Override
            protected void populatePopup(HeapViewerNode node, JPopupMenu popup) {
                if (!provider.supportsAggregation()) return;
                
                if (popup.getComponentCount() > 0) popup.addSeparator();
                
                popup.add(new JCheckBoxMenuItem(Bundle.TruffleObjectPropertyPlugin_ShowMergedSwitch(provider.getName()), tPropertiesHisto) {
                    @Override
                    protected void fireActionPerformed(ActionEvent event) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                tPropertiesHisto = isSelected();
                                storeItem(KEY_TYPE_PROPERTIES_HISTOGRAM, tPropertiesHisto);
                                reloadView();
                            }
                        });
                    }
                });
            }
        };
        if (SHARED_VIEW == null) SHARED_VIEW = objectsView;
    }

    protected JComponent createComponent() {
        return objectsView.getComponent();
    }
    
    
    protected final boolean supportsAggregation() {
        return provider.supportsAggregation();
    }
    
    
    protected void nodeSelected(HeapViewerNode node, boolean adjusting) {
        synchronized (objectsView) {
            if (Objects.equals(selected, node)) return;
            
            selected = node;
        }
        
        objectsView.reloadView();
    }
    
    
    private static boolean readItem(String itemName, boolean initial) {
        return NbPreferences.forModule(TruffleObjectPropertyPlugin.class).getBoolean("TruffleObjectPropertyPlugin." + itemName, initial); // NOI18N
    }

    private static void storeItem(String itemName, boolean value) {
        NbPreferences.forModule(TruffleObjectPropertyPlugin.class).putBoolean("TruffleObjectPropertyPlugin." + itemName, value); // NOI18N
    }
    
}
