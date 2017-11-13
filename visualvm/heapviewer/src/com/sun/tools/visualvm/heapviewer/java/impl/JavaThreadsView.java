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

package com.sun.tools.visualvm.heapviewer.java.impl;

import java.awt.CardLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.net.URL;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.SortOrder;
import org.netbeans.lib.profiler.heap.GCRoot;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaFrameGCRoot;
import org.netbeans.lib.profiler.results.CCTNode;
import org.netbeans.lib.profiler.ui.components.ProfilerToolbar;
import org.netbeans.lib.profiler.ui.swing.GrayLabel;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import org.netbeans.modules.profiler.heapwalk.ui.icons.HeapWalkerIcons;
import com.sun.tools.visualvm.heapviewer.HeapContext;
import com.sun.tools.visualvm.heapviewer.java.JavaHeapFragment;
import com.sun.tools.visualvm.heapviewer.model.DataType;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNode;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNodeFilter;
import com.sun.tools.visualvm.heapviewer.model.Progress;
import com.sun.tools.visualvm.heapviewer.model.RootNode;
import com.sun.tools.visualvm.heapviewer.ui.HTMLView;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerActions;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerFeature;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerNodeAction;
import com.sun.tools.visualvm.heapviewer.ui.PluggableTreeTableView;
import com.sun.tools.visualvm.heapviewer.ui.TreeTableViewColumn;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
public class JavaThreadsView extends HeapViewerFeature {
    
    private static final String FEATURE_ID = "java_threads"; // NOI18N
    private static final String VIEW_OBJECTS_ID = FEATURE_ID + "_objects"; // NOI18N
    private static final String VIEW_HTML_ID = FEATURE_ID + "_html"; // NOI18N
    
//    private final HeapContext context;
//    private final HeapViewerActions actions;
    
    private JComponent component;
    private ProfilerToolbar toolbar;
    private ProfilerToolbar pluginsToolbar;
    
    private final HTMLView htmlView;
    private final PluggableTreeTableView objectsView;
    
    private JToggleButton rObjects;
    private JToggleButton rHTML;
    
    
    public JavaThreadsView(HeapContext context, HeapViewerActions actions) {
        super(FEATURE_ID, "Threads", "Threads", Icons.getIcon(ProfilerIcons.WINDOW_THREADS), 300);
        
//        this.context = context;
//        this.actions = actions;
        
        Heap heap = context.getFragment().getHeap();
        
        objectsView = new PluggableTreeTableView(VIEW_OBJECTS_ID, context, actions, TreeTableViewColumn.instances(heap, false)) {
            protected HeapViewerNode[] computeData(RootNode root, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) {
                return JavaThreadsObjects.getThreads(root, heap);
            }
            protected void childrenChanged() {
                CCTNode[] children = getRoot().getChildren();
                for (CCTNode child : children) expandNode((HeapViewerNode)child);
            }
        };
        objectsView.setViewName("Threads");
        
        htmlView = new HTMLView(VIEW_HTML_ID, context, actions, "<br>&nbsp;&nbsp;computing threads...") {
            protected String computeData(HeapContext context, String viewID) {
                return JavaThreadsHTML.getThreads(context);
            }
            protected HeapViewerNode nodeForURL(URL url, HeapContext context) {
                return JavaThreadsHTML.getNode(url, context);
            }
        };
    }
    
    
    public JComponent getComponent() {
        if (component == null) init();
        return component;
    }

    public ProfilerToolbar getToolbar() {
        if (toolbar == null) init();
        return toolbar;
    }
    
    
    void selectInstance(long instanceID, String viewID) {
        if (rHTML == null) init();
        
        rHTML.setSelected(true);
        htmlView.selectReference(Long.toString(instanceID));
    }
    
    
    private void init() {
        toolbar = ProfilerToolbar.create(false);
        
        toolbar.addSpace(2);
        toolbar.addSeparator();
        toolbar.addSpace(5);
        
        toolbar.add(new GrayLabel("Results:"));
        toolbar.addSpace(3);
        
        ButtonGroup resultsBG = new ButtonGroup();
        
        rObjects = new JToggleButton(Icons.getIcon(ProfilerIcons.TAB_HOTSPOTS), true) {
            protected void fireItemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    if (component != null) ((CardLayout)component.getLayout()).first(component);
                    if (pluginsToolbar != null) pluginsToolbar.getComponent().setVisible(true);
                }
            }
        };
        rObjects.setToolTipText("Objects");
        resultsBG.add(rObjects);
        toolbar.add(rObjects);
        rHTML = new JToggleButton(Icons.getIcon(HeapWalkerIcons.PROPERTIES)) {
            protected void fireItemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    if (component != null) ((CardLayout)component.getLayout()).last(component);
                    if (pluginsToolbar != null) pluginsToolbar.getComponent().setVisible(false);
                }
            }
        };
        rHTML.setToolTipText("HTML");
        resultsBG.add(rHTML);
        toolbar.add(rHTML);
        
        if (objectsView.hasPlugins()) {
            pluginsToolbar = ProfilerToolbar.create(false);
//            detailsToolbar.addSpace(2);
//            detailsToolbar.addSeparator();
            pluginsToolbar.addSpace(8);

            pluginsToolbar.add(new GrayLabel("Details:"));
            pluginsToolbar.addSpace(2);
            
            pluginsToolbar.add(objectsView.getToolbar());
            
            toolbar.add(pluginsToolbar);
        }

        JScrollPane htmlViewScroll = new JScrollPane(htmlView.getComponent(),
                                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        htmlViewScroll.setBorder(BorderFactory.createEmptyBorder());
        htmlViewScroll.setViewportBorder(BorderFactory.createEmptyBorder());
        htmlViewScroll.getVerticalScrollBar().setUnitIncrement(10);
        htmlViewScroll.getHorizontalScrollBar().setUnitIncrement(10);

        component = new JPanel(new CardLayout());
        component.add(objectsView.getComponent());
        component.add(htmlViewScroll);
    }
    
    
    @ServiceProvider(service=HeapViewerFeature.Provider.class)
    public static class Provider extends HeapViewerFeature.Provider {

        public HeapViewerFeature getFeature(HeapContext context, HeapViewerActions actions) {
            if (JavaHeapFragment.isJavaHeap(context))
                return new JavaThreadsView(context, actions);
            
            return null;
        }

    }
    
    
    private static class SelectInstanceAction extends HeapViewerNodeAction {
        
        private final long id;
        private final HeapViewerActions actions;
        
        SelectInstanceAction(long id, HeapViewerActions actions) {
            super("Select in Threads", 205);
            this.id = id;
            this.actions = actions;
        }

        public void actionPerformed(ActionEvent e) {
            HeapViewerFeature feature = actions.findFeature(FEATURE_ID);
            if (feature instanceof JavaThreadsView) {
                actions.selectFeature(feature);
                ((JavaThreadsView)feature).selectInstance(id, VIEW_HTML_ID);
            }
        }
        
    }
    
    @ServiceProvider(service=HeapViewerNodeAction.Provider.class)
    public static class SelectInstanceActionProvider extends HeapViewerNodeAction.Provider {
        
        public boolean supportsView(HeapContext context, String viewID) {
            return !VIEW_HTML_ID.equals(viewID) && JavaHeapFragment.isJavaHeap(context);
        }

        public HeapViewerNodeAction[] getActions(HeapViewerNode node, HeapContext context, HeapViewerActions actions) {
            Heap heap = context.getFragment().getHeap();
            
            Instance instance = HeapViewerNode.getValue(node, DataType.INSTANCE, heap);
            if (instance == null) return null;
            
            GCRoot gcRoot = heap.getGCRoot(instance);
            if (gcRoot == null || !GCRoot.JAVA_FRAME.equals(gcRoot.getKind())) return null;
            
            JavaFrameGCRoot frameVar = (JavaFrameGCRoot)gcRoot;
            if (frameVar.getFrameNumber() == -1) return null;
            
            return new HeapViewerNodeAction[] { new SelectInstanceAction(instance.getInstanceId(), actions) };
        }
        
    }
    
}
