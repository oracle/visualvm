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

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.heapviewer.truffle.swing.LinkButton;
import org.graalvm.visualvm.lib.ui.UIUtils;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "TruffleObjectPropertyPlugin_NoSelection=<no object selected>",
    "TruffleObjectPropertyPlugin_NoSelectionEx=<no object or type selected>",
    "TruffleObjectPropertyPlugin_NoItems=<no {0}>", // <no items>
    "TruffleObjectPropertyPlugin_AutoShowMergedSwitch=Compute merged {0} automatically",
    "TruffleObjectPropertyPlugin_ComputeMergedReferencesLbl=Compute Merged {0}",
    "TruffleObjectPropertyPlugin_ComputeMergedReferencesTtp=Compute merged {0} for the selected type",
    "TruffleObjectPropertyPlugin_AutoComputeMergedReferencesLbl=Compute Merged {0} Automatically",
    "TruffleObjectPropertyPlugin_AutoComputeMergedReferencesTtp=Compute merged {0} automatically for each selected type"
})
public class TruffleObjectPropertyPlugin<O extends TruffleObject, T extends TruffleType<O>, F extends TruffleLanguageHeapFragment<O, T>, L extends TruffleLanguage<O, T, F>> extends HeapViewPlugin {
    
    private static final TreeTableView.ColumnConfiguration CCONF_TYPE = new TreeTableView.ColumnConfiguration(DataType.COUNT, null, DataType.COUNT, SortOrder.DESCENDING, Boolean.FALSE);
    private static final TreeTableView.ColumnConfiguration CCONF_OBJECT = new TreeTableView.ColumnConfiguration(null, DataType.COUNT, DataType.NAME, SortOrder.UNSORTED, null);
    
    private final TruffleObjectPropertyProvider<O, T, F, L, ? extends Object> provider;
    
    private final Heap heap;
    
    private HeapViewerNode selected;
    
    private volatile boolean mergedRequest;
    
    private final TreeTableView objectsView;
    
    
    public TruffleObjectPropertyPlugin(String name, String description, Icon icon, String viewID, HeapContext context, HeapViewerActions actions, TruffleObjectPropertyProvider<O, T, F, L, ? extends Object> provider) {
        super(name, description, icon);
        
        this.provider = provider;
        
        final String mergedPropertiesKey = provider.getMergedPropertiesKey();
        
        heap = context.getFragment().getHeap();
        
        TreeTableViewColumn[] columns = mergedPropertiesKey == null ?
            new TreeTableViewColumn[] {
                new TreeTableViewColumn.Name(heap),
                new TreeTableViewColumn.LogicalValue(heap),
                new TreeTableViewColumn.OwnSize(heap, false, false),
                new TreeTableViewColumn.RetainedSize(heap, false, false),
                new TreeTableViewColumn.ObjectID(heap)
            } : new TreeTableViewColumn[] {
                new TreeTableViewColumn.Name(heap),
                new TreeTableViewColumn.LogicalValue(heap),
                new TreeTableViewColumn.Count(heap, true, true),
                new TreeTableViewColumn.OwnSize(heap, false, false),
                new TreeTableViewColumn.RetainedSize(heap, false, false),
                new TreeTableViewColumn.ObjectID(heap)
            };
        objectsView = new TreeTableView(viewID, context, actions, columns) {
            protected HeapViewerNode[] computeData(RootNode root, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {
                if (mergedRequest) return HeapViewerNode.NO_NODES;
                
                HeapViewerNode _selected;
                synchronized (objectsView) { _selected = selected; }
                
                if (_selected == null) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            if (!CCONF_OBJECT.equals(objectsView.getCurrentColumnConfiguration()))
                                objectsView.configureColumns(CCONF_OBJECT);
                        }
                    });
                    
                    return new HeapViewerNode[] { new TextNode(noSelectionString()) };
                }
                
                HeapViewerNode[] nodes;
                TruffleObjectsWrapper wrapper = mergedPropertiesKey == null ? null : HeapViewerNode.getValue(_selected, TruffleObjectsWrapper.DATA_TYPE, heap);
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
                    else nodes = new HeapViewerNode[] { new TextNode(noSelectionString()) };
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
                if (provider.getMergedPropertiesKey() == null) return;
                
                if (popup.getComponentCount() > 0) popup.addSeparator();
                
                popup.add(new JCheckBoxMenuItem(Bundle.TruffleObjectPropertyPlugin_AutoShowMergedSwitch(provider.getName()), isAutoMerge()) {
                    @Override
                    protected void fireActionPerformed(ActionEvent event) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                setAutoMerge(isSelected());
                            }
                        });
                    }
                });
            }
        };
    }
    
    private String noSelectionString() {
        return provider.getMergedPropertiesKey() == null ? Bundle.TruffleObjectPropertyPlugin_NoSelection() :
                                                           Bundle.TruffleObjectPropertyPlugin_NoSelectionEx();
    }
    
    private JComponent component;
    
    private void showObjectsView() {
        JComponent c = objectsView.getComponent();
        if (c.isVisible()) return;
        
        c.setVisible(true);
        
        component.removeAll();
        component.add(c, BorderLayout.CENTER);
        
        mergedRequest = false;
        
        component.invalidate();
        component.revalidate();
        component.repaint();
    }
    
    private void showMergedView() {
        JComponent c = objectsView.getComponent();
        if (!c.isVisible()) return;
        
        c.setVisible(false);
        
        component.removeAll();
        
        String name = provider.getName();
        String _name = name.substring(0, 1).toUpperCase() + name.substring(1);
        
        JButton jb = new JButton(Bundle.TruffleObjectPropertyPlugin_ComputeMergedReferencesLbl(_name), getIcon()) {
            protected void fireActionPerformed(ActionEvent e) {
                showObjectsView();
                objectsView.reloadView();
            }
        };
        jb.setIconTextGap(jb.getIconTextGap() + 2);
        jb.setToolTipText(Bundle.TruffleObjectPropertyPlugin_ComputeMergedReferencesTtp(name));
        Insets margin = jb.getMargin();
        if (margin != null) jb.setMargin(new Insets(margin.top + 3, margin.left + 3, margin.bottom + 3, margin.right + 3));
        
        
        LinkButton lb = new LinkButton(Bundle.TruffleObjectPropertyPlugin_AutoComputeMergedReferencesLbl(_name)) {
            protected void fireActionPerformed(ActionEvent e) {
                setAutoMerge(true);
                showObjectsView();
                objectsView.reloadView();
            }
        };
        lb.setToolTipText(Bundle.TruffleObjectPropertyPlugin_AutoComputeMergedReferencesTtp(name));
                
        
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        GridBagConstraints g;
        
        g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridy = 0;
        p.add(jb, g);
        
        g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridy = 1;
        g.insets = new Insets(10, 0, 0, 0);
        p.add(lb, g);
        
        component.add(p);
        
        mergedRequest = true;

        component.invalidate();
        component.revalidate();
        component.repaint();
    }

    protected JComponent createComponent() {
        component = new JPanel(new BorderLayout());
        component.setOpaque(true);
        component.setBackground(UIUtils.getProfilerResultsBackground());
        
        objectsView.getComponent().setVisible(false); // force init in showObjectsView()
        showObjectsView();
        
        return component;
    }
    
    
    @Override
    protected void closed() {
        objectsView.closed();
    }
    
    
    protected void nodeSelected(HeapViewerNode node, boolean adjusting) {
        synchronized (objectsView) {
            if (Objects.equals(selected, node)) return;
            selected = node;
        }
        
        if (selected != null && provider.getMergedPropertiesKey() != null && !isAutoMerge() && HeapViewerNode.getValue(selected, TruffleObjectsWrapper.DATA_TYPE, heap) != null) showMergedView();
        else showObjectsView();
        
        objectsView.reloadView();
    }
    
    
    private boolean isAutoMerge() {
        return NbPreferences.root().getBoolean(provider.getMergedPropertiesKey(), false);
    }

    private void setAutoMerge(boolean value) {
        NbPreferences.root().putBoolean(provider.getMergedPropertiesKey(), value);
    }
    
}
