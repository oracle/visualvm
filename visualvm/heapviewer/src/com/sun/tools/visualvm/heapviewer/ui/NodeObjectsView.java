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

package com.sun.tools.visualvm.heapviewer.ui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Objects;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SortOrder;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.results.CCTNode;
import org.netbeans.lib.profiler.ui.components.ProfilerToolbar;
import org.netbeans.lib.profiler.ui.swing.GrayLabel;
import com.sun.tools.visualvm.heapviewer.HeapContext;
import com.sun.tools.visualvm.heapviewer.model.DataType;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNode;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNodeFilter;
import com.sun.tools.visualvm.heapviewer.model.Progress;
import com.sun.tools.visualvm.heapviewer.model.RootNode;
import javax.swing.SwingUtilities;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
public class NodeObjectsView extends HeapView {
    
    private final HeapViewerNode viewNode;
    
    private String name;
    private String description;
    private Icon icon;
    
    private final PluggableTreeTableView objectsView;
    private ProfilerToolbar toolbar;
    private JComponent component;
    
        
    public NodeObjectsView(String viewID, HeapViewerNode node, HeapContext context, HeapViewerActions actions) {
        super(null, null);
        
        viewNode = node;
        
        Heap heap = context.getFragment().getHeap();
        
        Integer count = HeapViewerNode.getValue(node, DataType.COUNT, heap);
        boolean hasCount = !Objects.equals(count, DataType.COUNT.getUnsupportedValue());
        TreeTableViewColumn[] columns = hasCount ? TreeTableViewColumn.classes(heap, true) :
                                                   TreeTableViewColumn.instances(heap, true);
        
        objectsView = new PluggableTreeTableView(viewID, context, actions, columns) {
            protected HeapViewerNode[] computeData(RootNode root, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) {
                return new HeapViewerNode[] { viewNode };
            }
            protected void childrenChanged() {
                CCTNode[] children = getRoot().getChildren();
                for (CCTNode child : children) expandNode((HeapViewerNode)child);
            }
        };
    }
    
    
    public String getName() {
        if (toolbar == null) initUI();
        return name;
    }

    public String getDescription() {
        if (toolbar == null) initUI();
        return description;
    }

    public Icon getIcon() {
        if (toolbar == null) initUI();
        return icon;
    }
    

    public JComponent getComponent() {
        if (toolbar == null) initUI();
        return component;
    }

    public ProfilerToolbar getToolbar() {
        if (toolbar == null) initUI();
        return toolbar;
    }
    
    
    private void initUI() {
        HeapViewerRenderer renderer = objectsView.getNodeRenderer(viewNode);
        name = renderer.getShortName();
        description = renderer.toString();
        icon = renderer.getIcon();
        
        toolbar = ProfilerToolbar.create(false);
        
        toolbar.addSpace(3);
        JLabel nodePresenter = new JLabel(name, icon, JLabel.LEADING);
        nodePresenter.setToolTipText(description);
        toolbar.add(nodePresenter);
        toolbar.addSpace(5);
        
        if (objectsView.hasPlugins()) {
            toolbar.addSeparator();
            toolbar.addSpace(5);

            toolbar.add(new GrayLabel("Details:"));
            toolbar.addSpace(2);
            
            toolbar.add(objectsView.getToolbar());
        }
        
        component = new ViewContainer(objectsView.getComponent(), viewNode);
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                objectsView.selectNode(viewNode);
                objectsView.getComponent().requestFocusInWindow();
            }
        });
    }
    
    
    private static class ViewContainer extends JPanel {
        
        private final HeapViewerNode node;
        
        ViewContainer(JComponent view, HeapViewerNode viewNode) {
            super(new BorderLayout());
            node = viewNode;
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
            return node.equals(((ViewContainer)o).node);
        }

        public int hashCode() {
            return node.hashCode();
        }
        
    }
    
    
    public static abstract class OpenAction extends HeapViewerNodeAction {
        
        private final HeapViewerNode node;
        private final HeapContext context;
        private final HeapViewerActions actions;
        
        public OpenAction(String name, int position, HeapViewerNode node, HeapContext context, HeapViewerActions actions) {
            super(name, position);
            this.node = node;
            this.context = context;
            this.actions = actions;
            setEnabled(node != null);
        }
        
        
        public abstract NodeObjectsView createView(HeapViewerNode node, HeapContext context, HeapViewerActions actions);
        
        
        public void actionPerformed(ActionEvent e) {
            new RequestProcessor().post(new Runnable() {
                public void run() {
                    NodeObjectsView view = createView(node, context, actions);
                    actions.addView(view, (e.getModifiers() & ActionEvent.SHIFT_MASK) == 0);
                }
            });
        }
        
    }
    
    public static abstract class DefaultOpenAction extends OpenAction {
        
        public DefaultOpenAction(HeapViewerNode node, HeapContext context, HeapViewerActions actions) {
            super("Open in New Tab", 0, node, context, actions);
        }
        
        public boolean isDefault() {
            return true;
        }
        
        public boolean isMiddleButtonDefault(ActionEvent e) {
            return (e.getModifiers() & ActionEvent.CTRL_MASK) != ActionEvent.CTRL_MASK;
        }
        
    }
    
}
